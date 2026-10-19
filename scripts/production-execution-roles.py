#!/usr/bin/env python3
"""Metadata-only plan by default; --apply provisions approved prod execution roles.

Never retrieves secret values, changes existing service roles, or starts ECS tasks.
"""
import argparse
import json

ACCOUNT = "889384901776"
REGION = "ap-northeast-2"
POLICY_NAME = "ProductionExecutionResources"
PLANS = {
    "identity": {
        "role": "tosunsaeng-identity-prod-execution-role",
        "repository": "tosunsaeng-identity",
        "logGroup": "/ecs/tosunsaeng-identity-prod",
        "secretNames": ["tosunsaeng/staging/identity"] + [
            "tosunsaeng/prod/identity/" + name for name in (
                "firebase", "reissue-encryption-keyring", "phone-fingerprint",
                "phone-eligibility-binding", "account-recovery-key-ring")],
    },
    "learningCore": {
        "role": "tosunsaeng-learning-core-prod-execution-role",
        "repository": "tosunsaeng-learning-core",
        "logGroup": "/ecs/tosunsaeng-learning-core-prod",
        "secretNames": ["tosunsaeng/staging/learning-core", "tosunsaeng/prod/challenge/outbound", "tosunsaeng/prod/challenge/callback"],
    },
    "examAi": {
        "role": "tosunsaeng-ai-prod-execution-role",
        "repository": "tosunsaeng-ai",
        "logGroup": "/ecs/tosunsaeng-ai-prod",
        "secretNames": ["tosunsaeng/staging/ai"],
    },
    "challengeAi": {
        "role": "tosunsaeng-ai-learning-prod-execution-role",
        "repository": "tosunsaeng-ai-learning-worker",
        "logGroup": "/ecs/tosunsaeng-ai-learning-prod",
        "secretNames": ["tosunsaeng/staging/ai", "tosunsaeng/prod/challenge/outbound", "tosunsaeng/prod/challenge/callback"],
    },
}


def trust_policy():
    return {"Version": "2012-10-17", "Statement": [{
        "Effect": "Allow", "Principal": {"Service": "ecs-tasks.amazonaws.com"},
        "Action": "sts:AssumeRole", "Condition": {
            "StringEquals": {"aws:SourceAccount": ACCOUNT},
            "ArnLike": {"aws:SourceArn": f"arn:aws:ecs:{REGION}:{ACCOUNT}:*"},
        },
    }]}


def permission_policy(plan, arns):
    if set(arns) != set(plan["secretNames"]):
        raise ValueError("Secret metadata set mismatch")
    prefix = f"arn:aws:secretsmanager:{REGION}:{ACCOUNT}:secret:"
    for name, arn in arns.items():
        if not arn.startswith(prefix + name + "-") or ":" in arn[len(prefix):] or "*" in arn:
            raise ValueError("Expected exact same-account Secret ARN")
    return {"Version": "2012-10-17", "Statement": [
        {"Sid": "ReadServiceSecrets", "Effect": "Allow", "Action": ["secretsmanager:GetSecretValue"], "Resource": sorted(arns.values())},
        {"Sid": "RegistryAuthorization", "Effect": "Allow", "Action": ["ecr:GetAuthorizationToken"], "Resource": "*"},
        {"Sid": "PullServiceImage", "Effect": "Allow", "Action": ["ecr:BatchCheckLayerAvailability", "ecr:GetDownloadUrlForLayer", "ecr:BatchGetImage"], "Resource": f"arn:aws:ecr:{REGION}:{ACCOUNT}:repository/{plan['repository']}"},
        {"Sid": "WriteServiceLogs", "Effect": "Allow", "Action": ["logs:CreateLogStream", "logs:PutLogEvents"], "Resource": f"arn:aws:logs:{REGION}:{ACCOUNT}:log-group:{plan['logGroup']}:log-stream:*"},
    ]}


def preflight(iam, sm, ecr):
    resolved = {}
    for key, plan in PLANS.items():
        arns = {}
        for name in plan["secretNames"]:
            meta = sm.describe_secret(SecretId=name)
            if meta.get("DeletedDate") or meta.get("KmsKeyId") not in (None, "alias/aws/secretsmanager"):
                raise ValueError("Deleted secret or customer KMS requires separate review")
            if not any("AWSCURRENT" in stages for stages in meta.get("VersionIdsToStages", {}).values()):
                raise ValueError("Secret has no current version")
            arns[name] = meta["ARN"]
        policy = permission_policy(plan, arns)
        ecr.describe_repositories(repositoryNames=[plan["repository"]])
        try:
            role = iam.get_role(RoleName=plan["role"])["Role"]
        except iam.exceptions.NoSuchEntityException:
            role = None
        if role:
            if role["AssumeRolePolicyDocument"] != trust_policy() or role.get("PermissionsBoundary"):
                raise ValueError("Existing role trust/boundary requires separate review")
            attached = iam.list_attached_role_policies(RoleName=plan["role"])["AttachedPolicies"]
            inline = set(iam.list_role_policies(RoleName=plan["role"])["PolicyNames"])
            expected = {POLICY_NAME} | ({"ReadProductionFirebaseOnly"} if key == "identity" else set())
            if attached or not inline.issubset(expected):
                raise ValueError("Unexpected existing role policies")
            if "ReadProductionFirebaseOnly" in inline:
                old = iam.get_role_policy(RoleName=plan["role"], PolicyName="ReadProductionFirebaseOnly")["PolicyDocument"]
                expected_old = {"Version": "2012-10-17", "Statement": [{"Sid": "ReadProductionFirebaseOnly", "Effect": "Allow", "Action": ["secretsmanager:GetSecretValue"], "Resource": [arns["tosunsaeng/prod/identity/firebase"]]}]}
                if old != expected_old:
                    raise ValueError("Firebase bootstrap policy drift")
            if POLICY_NAME in inline and iam.get_role_policy(RoleName=plan["role"], PolicyName=POLICY_NAME)["PolicyDocument"] != policy:
                raise ValueError("Existing production execution policy drift")
        resolved[key] = (policy, bool(role))
    return resolved


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    import boto3
    session = boto3.Session(region_name=REGION)
    if session.client("sts").get_caller_identity()["Account"] != ACCOUNT:
        raise ValueError("Unexpected account")
    iam, sm, ecr, logs = [session.client(name) for name in ("iam", "secretsmanager", "ecr", "logs")]
    resolved = preflight(iam, sm, ecr)
    for key, (policy, exists) in resolved.items():
        plan = PLANS[key]
        print(json.dumps({"service": key, "role": plan["role"], "roleExists": exists, "secretCount": len(plan["secretNames"]), "repository": plan["repository"], "logGroup": plan["logGroup"]}))
        if not args.apply:
            continue
        if not exists:
            iam.create_role(RoleName=plan["role"], AssumeRolePolicyDocument=json.dumps(trust_policy()), Description="Production ECS execution, service-scoped resources", Tags=[{"Key": "Environment", "Value": "prod"}])
        iam.put_role_policy(RoleName=plan["role"], PolicyName=POLICY_NAME, PolicyDocument=json.dumps(policy))
        try:
            logs.create_log_group(logGroupName=plan["logGroup"], tags={"Environment": "prod"})
        except logs.exceptions.ResourceAlreadyExistsException:
            pass  # Preserve existing retention/KMS/settings. Never delete log records.
        if iam.get_role_policy(RoleName=plan["role"], PolicyName=POLICY_NAME)["PolicyDocument"] != policy:
            raise ValueError("Policy readback mismatch")
        print("APPLIED_VERIFIED", key)
    print("ECS task registration, service creation, runtime S3 roles and service start were not performed.")


if __name__ == "__main__":
    main()
