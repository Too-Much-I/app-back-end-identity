#!/usr/bin/env python3
"""Read-only plan by default. --apply adds the explicitly approved prod resources.

Does not attach roles/SGs to services, start tasks, read objects, or alter buckets.
"""
import argparse
import json

ACCOUNT = "889384901776"
REGION = "ap-northeast-2"
VPC = "vpc-04068339482839ff1"
ALB = "sg-0e5462fa65a22ded8"
REDIS = "sg-0f8528393d749b3a9"
MARKER = "tosunsaeng-prod-runtime-network"
POLICY = "ProductionRuntimeS3"
GROUPS = {k: f"tosunsaeng-{n}-prod-sg" for k, n in {
    "identity": "identity", "learningCore": "learning-core",
    "examAi": "ai", "challengeAi": "ai-learning"}.items()}
ROLES = {
    "tosunsaeng-learning-core-prod-task-role": (["s3:GetObject", "s3:PutObject"], "arn:aws:s3:::to-teacher-exam/*"),
    "tosunsaeng-ai-learning-prod-task-role": (["s3:GetObject", "s3:PutObject", "s3:DeleteObject"], "arn:aws:s3:::to-teacher-exam/challenge/*"),
}


def trust():
    return {"Version": "2012-10-17", "Statement": [{"Effect": "Allow",
        "Principal": {"Service": "ecs-tasks.amazonaws.com"}, "Action": "sts:AssumeRole",
        "Condition": {"StringEquals": {"aws:SourceAccount": ACCOUNT},
                      "ArnLike": {"aws:SourceArn": f"arn:aws:ecs:{REGION}:{ACCOUNT}:*"}}}]}


def policy(role):
    actions, resource = ROLES[role]
    return {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": actions, "Resource": resource}]}


def tcp(port, kind, target):
    return ("tcp", port, port, kind, target)


def rules(ids):
    https = tcp(443, "cidr", "0.0.0.0/0")
    mongo = tcp(27017, "cidr", "0.0.0.0/0")
    redis = tcp(6379, "group", REDIS)
    return {
        "identity": ({tcp(8081, "group", ALB)}, {https, mongo}),
        "learningCore": ({tcp(8080, "group", ALB)}, {https, mongo, redis, tcp(8000, "group", ids["examAi"])}),
        "examAi": ({tcp(8000, "group", ids["learningCore"])}, {https, redis}),
        "challengeAi": ({tcp(8000, "group", ALB)}, {https, redis}),
    }


def atoms(permissions):
    result = set()
    for p in permissions:
        base = (p["IpProtocol"], p.get("FromPort", -1), p.get("ToPort", -1))
        for field, value, kind in [("IpRanges", "CidrIp", "cidr"), ("Ipv6Ranges", "CidrIpv6", "ipv6"),
                                   ("UserIdGroupPairs", "GroupId", "group"), ("PrefixListIds", "PrefixListId", "prefix")]:
            result.update(base + (kind, x[value]) for x in p.get(field, []))
    return result


def permission(atom):
    proto, start, end, kind, target = atom
    p = {"IpProtocol": proto, "FromPort": start, "ToPort": end}
    if kind == "group":
        p["UserIdGroupPairs"] = [{"GroupId": target}]
    elif kind == "cidr":
        p["IpRanges"] = [{"CidrIp": target}]
    else:
        raise ValueError("Unsupported planned target")
    return p


def get_group(ec2, group_id):
    return ec2.describe_security_groups(GroupIds=[group_id])["SecurityGroups"][0]


def preflight(iam, ec2):
    shared = {n: get_group(ec2, n) for n in (ALB, REDIS)}
    if any(g["VpcId"] != VPC or g["OwnerId"] != ACCOUNT for g in shared.values()):
        raise ValueError("Shared SG account/VPC mismatch")
    existing = {}
    for key, name in GROUPS.items():
        found = ec2.describe_security_groups(Filters=[{"Name": "vpc-id", "Values": [VPC]}, {"Name": "group-name", "Values": [name]}])["SecurityGroups"]
        if len(found) > 1:
            raise ValueError("Ambiguous group")
        existing[key] = found[0] if found else None
    ids = {k: g["GroupId"] if g else "MISSING:" + k for k, g in existing.items()}
    planned = rules(ids)
    for k, g in existing.items():
        if not g:
            continue
        tags = {x["Key"]: x["Value"] for x in g.get("Tags", [])}
        if g["OwnerId"] != ACCOUNT or tags.get("ManagedBy") != MARKER:
            raise ValueError("Existing group not managed by this operation")
        for index, field in enumerate(("IpPermissions", "IpPermissionsEgress")):
            if not atoms(g[field]).issubset(planned[k][index]):
                raise ValueError("Unexpected existing group rules; no automatic overwrite")
    role_exists = {}
    for role in ROLES:
        try:
            current = iam.get_role(RoleName=role)["Role"]
        except iam.exceptions.NoSuchEntityException:
            role_exists[role] = False
            continue
        if current["AssumeRolePolicyDocument"] != trust() or current.get("PermissionsBoundary"):
            raise ValueError("Existing runtime role trust/boundary mismatch")
        if iam.list_attached_role_policies(RoleName=role)["AttachedPolicies"]:
            raise ValueError("Unexpected managed policies")
        names = set(iam.list_role_policies(RoleName=role)["PolicyNames"])
        if names - {POLICY}:
            raise ValueError("Unexpected runtime inline policies")
        if POLICY in names and iam.get_role_policy(RoleName=role, PolicyName=POLICY)["PolicyDocument"] != policy(role):
            raise ValueError("Runtime policy drift")
        role_exists[role] = True
    return existing, role_exists, shared


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    import boto3
    session = boto3.Session(region_name=REGION)
    if session.client("sts").get_caller_identity()["Account"] != ACCOUNT:
        raise ValueError("Wrong AWS account")
    iam, ec2, s3 = [session.client(n) for n in ("iam", "ec2", "s3")]
    s3.head_bucket(Bucket="to-teacher-exam", ExpectedBucketOwner=ACCOUNT)
    existing, role_exists, shared = preflight(iam, ec2)
    print("PREFLIGHT_OK", json.dumps({"roles": role_exists, "groups": {k: g["GroupId"] if g else None for k, g in existing.items()}}))
    if not args.apply:
        return
    tags = [{"Key": "Environment", "Value": "prod"}, {"Key": "ManagedBy", "Value": MARKER}]
    for role, exists in role_exists.items():
        if not exists:
            iam.create_role(RoleName=role, AssumeRolePolicyDocument=json.dumps(trust()), Tags=tags)
        iam.put_role_policy(RoleName=role, PolicyName=POLICY, PolicyDocument=json.dumps(policy(role)))
        if iam.get_role_policy(RoleName=role, PolicyName=POLICY)["PolicyDocument"] != policy(role):
            raise ValueError("Role readback mismatch")
        print("ROLE_VERIFIED", role)
    ids = {}
    for key, g in existing.items():
        if g:
            ids[key] = g["GroupId"]
            continue
        ids[key] = ec2.create_security_group(GroupName=GROUPS[key], Description="Production service scoped traffic", VpcId=VPC,
            TagSpecifications=[{"ResourceType": "security-group", "Tags": tags}])["GroupId"]
        default = get_group(ec2, ids[key])["IpPermissionsEgress"]
        if atoms(default) != {("-1", -1, -1, "cidr", "0.0.0.0/0")}:
            raise ValueError("Unexpected new SG defaults; inspect before continuing")
        ec2.revoke_security_group_egress(GroupId=ids[key], IpPermissions=default)
    for key, (ingress, egress) in rules(ids).items():
        for wanted, field, action in [(ingress, "IpPermissions", ec2.authorize_security_group_ingress),
                                      (egress, "IpPermissionsEgress", ec2.authorize_security_group_egress)]:
            missing = wanted - atoms(get_group(ec2, ids[key])[field])
            if missing:
                action(GroupId=ids[key], IpPermissions=[permission(a) for a in sorted(missing)])
            if atoms(get_group(ec2, ids[key])[field]) != wanted:
                raise ValueError("SG readback mismatch")
        print("GROUP_VERIFIED", key, ids[key])
    expected = {tcp(6379, "group", ids[k]) for k in ("learningCore", "examAi", "challengeAi")}
    before = atoms(shared[REDIS]["IpPermissions"])
    current = atoms(get_group(ec2, REDIS)["IpPermissions"])
    if current != before:
        raise ValueError("Redis changed concurrently; inspect before adding")
    missing = expected - current
    if missing:
        ec2.authorize_security_group_ingress(GroupId=REDIS, IpPermissions=[permission(a) for a in sorted(missing)])
    after = get_group(ec2, REDIS)
    if atoms(after["IpPermissions"]) != before | expected or atoms(after["IpPermissionsEgress"]) != atoms(shared[REDIS]["IpPermissionsEgress"]):
        raise ValueError("Redis readback mismatch")
    if atoms(get_group(ec2, ALB)["IpPermissions"]) != atoms(shared[ALB]["IpPermissions"]):
        raise ValueError("ALB changed concurrently")
    print("REDIS_ADDITIONS_VERIFIED", len(missing))
    preflight(iam, ec2)
    print("COMPLETE_NO_BUCKET_POLICY_CHANGE_NO_ECS_START")


if __name__ == "__main__":
    main()
