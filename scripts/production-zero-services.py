#!/usr/bin/env python3
"""Create approved prod ECS/ALB skeleton, desiredCount=0. DNS is separate.

Default is read-only. Never starts tasks or overwrites existing services/rules.
Production secret references are resolved by metadata; no secret values fetched.
"""
import argparse
import copy
import json
from pathlib import Path

ACCOUNT = "889384901776"
REGION = "ap-northeast-2"
NAMESPACE = "arn:aws:servicediscovery:ap-northeast-2:889384901776:namespace/ns-wisnr6tgrfy7prhf"
TASK_FIELDS = ("networkMode", "containerDefinitions", "volumes", "placementConstraints", "requiresCompatibilities", "runtimePlatform", "ephemeralStorage")
IDENTITY_KEYS = {
    "AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT": "reissue-encryption-keyring",
    "FIREBASE_SERVICE_ACCOUNT_JSON": "firebase",
    "PHONE_IDENTITY_FINGERPRINT_KEY_RING": "phone-fingerprint",
    "PHONE_ELIGIBILITY_BINDING_KEY_RING": "phone-eligibility-binding",
    "ACCOUNT_RECOVERY_KEY_RING": "account-recovery-key-ring",
}


def env(container):
    return {x["name"]: x["value"] for x in container.get("environment", [])}


def build_task(key, plan, settings, source, operating, secrets):
    task = {k: copy.deepcopy(source[k]) for k in TASK_FIELDS if k in source}
    task.update(family=plan["family"], cpu=plan["cpu"], memory=plan["memory"],
                executionRoleArn=f"arn:aws:iam::{ACCOUNT}:role/{plan['executionRole']}")
    if plan["runtimeRole"]:
        task["taskRoleArn"] = f"arn:aws:iam::{ACCOUNT}:role/{plan['runtimeRole']}"
    for container in task["containerDefinitions"]:
        container["image"] = plan["image"]
        container.pop("environmentFiles", None)
        container.pop("repositoryCredentials", None)
        values = env(operating["containerDefinitions"][0]) if key in ("identity", "learningCore") else env(container)
        if key in ("identity", "learningCore"):
            values.update(settings["environment"])
            # This revision is not a release-ready launch configuration.
            values.update({k: "false" for k in settings["launchTargetFlags"]})
            values.update(settings["holdOffFlags"])
            values["SPRING_PROFILES_ACTIVE"] = "prod"
            values["SENTRY_ENVIRONMENT"] = "prod"
            values["SWAGGER_ENABLED"] = "false"
            values["SENTRY_SMOKE_TRIGGER_ENABLED"] = "false"
            if key == "identity":
                values["AUTH_REISSUE_ENCRYPTION_ACTIVE_KEY_ID"] = "prod-v1"
                for platform in ("ANDROID", "IOS"):
                    for version in ("MINIMUM", "LATEST"):
                        values[f"APP_{platform}_{version}_VERSION"] = ""
            else:
                overrides = dict(settings["springPropertyOverrides"])
                overrides.update({"app.notifications.api-enabled": False, "app.notifications.tracking-enabled": False})
                values["SPRING_APPLICATION_JSON"] = json.dumps(overrides)
        elif key == "examAi":
            values.update(settings["environmentForApiAndAllWorkers"])
        else:
            values.update(settings["environmentForApiAndWorker"])
            if container["name"] == "learning-worker":
                values.update(settings["workerEnvironment"])
        if key in ("examAi", "challengeAi") and "LANGSMITH_PROJECT" in values:
            values["LANGSMITH_PROJECT"] = "tosunsaeng-" + key + "-prod"
        # Drop any plaintext value shadowing injected secrets.
        for name in secrets[container["name"]]:
            values.pop(name, None)
        container["environment"] = [{"name": n, "value": v} for n, v in sorted(values.items())]
        container["secrets"] = [{"name": n, "valueFrom": v} for n, v in sorted(secrets[container["name"]].items())]
        group = "/ecs/" + plan["family"]
        container["logConfiguration"] = {"logDriver": "awslogs", "options": {
            "awslogs-group": group, "awslogs-region": REGION, "awslogs-stream-prefix": "ecs"}}
    validate_task(key, task)
    return task


def validate_task(key, task):
    if not task["family"].endswith("-prod") or task["networkMode"] != "awsvpc":
        raise ValueError("Unexpected task identity/network")
    for c in task["containerDefinitions"]:
        if "@sha256:" not in c["image"]:
            raise ValueError("Unpinned image")
        for secret in c["secrets"]:
            if "/test/" in secret["valueFrom"]:
                raise ValueError("Test secret reference prohibited")
        values = env(c)
        for name, value in values.items():
            if name.endswith("_ENABLED") and value.lower() == "true" and key in ("identity", "learningCore"):
                # Inherited stable features allowed; asynchronous features are explicitly disabled above.
                if any(word in name for word in ("WORKER", "PUBLISHER", "CLEANUP", "BACKFILL")):
                    raise ValueError("Background processing enabled in prestart task")
            if "to-teacher.com" in value and "-test.to-teacher.com" in value:
                raise ValueError("Test endpoint prohibited")


def service_request(key, spec, topology, task_arn, target):
    c_name = spec["containerName"]
    request = {"cluster": topology["cluster"], "serviceName": spec["name"], "taskDefinition": task_arn,
        "desiredCount": 0, "launchType": "FARGATE", "platformVersion": "LATEST", "enableExecuteCommand": False,
        "networkConfiguration": {"awsvpcConfiguration": {"subnets": topology["subnets"],
            "securityGroups": [spec["securityGroup"]], "assignPublicIp": "DISABLED"}},
        "deploymentConfiguration": {"deploymentCircuitBreaker": {"enable": True, "rollback": True}, "minimumHealthyPercent": 100, "maximumPercent": 200}}
    if target:
        request["loadBalancers"] = [{"targetGroupArn": target, "containerName": c_name, "containerPort": spec["port"]}]
        request["healthCheckGracePeriodSeconds"] = 300
    if key in ("learningCore", "examAi"):
        request["serviceConnectConfiguration"] = {"enabled": True, "namespace": NAMESPACE}
        if key == "examAi":
            request["serviceConnectConfiguration"]["services"] = [{"portName": "ai-api", "discoveryName": "tosunsaeng-ai-prod", "clientAliases": [{"port": 8000, "dnsName": "tosunsaeng-ai-prod"}]}]
    return request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--settings", default="production-settings-2026-10-08.json")
    parser.add_argument("--topology", default="production-topology-2026-10-08.json")
    args = parser.parse_args()
    settings = json.loads(Path(args.settings).read_text())
    topology = json.loads(Path(args.topology).read_text())
    if topology["initialDesiredCount"] != 0 or topology["createAutoscaling"]:
        raise ValueError("Only zero-count preparation approved")
    import boto3
    session = boto3.Session(region_name=REGION)
    if session.client("sts").get_caller_identity()["Account"] != ACCOUNT:
        raise ValueError("Wrong account")
    ecs, elb, sm, ecr = [session.client(n) for n in ("ecs", "elbv2", "secretsmanager", "ecr")]
    def arn(name):
        return sm.describe_secret(SecretId=name)["ARN"]
    bundles = {k: arn("tosunsaeng/staging/" + v) for k, v in (("identity", "identity"), ("learningCore", "learning-core"), ("ai", "ai"))}
    prod_identity = {k: arn("tosunsaeng/prod/identity/" + v) for k, v in IDENTITY_KEYS.items()}
    challenge = {"outbound": arn("tosunsaeng/prod/challenge/outbound"), "callback": arn("tosunsaeng/prod/challenge/callback")}
    tasks = {}
    for key, spec in topology["services"].items():
        source = ecs.describe_task_definition(taskDefinition=spec["sourceTask"])["taskDefinition"]
        repo_digest = spec["image"].split("/", 1)[1]
        repo, digest = repo_digest.split("@")
        ecr.describe_images(repositoryName=repo, imageIds=[{"imageDigest": digest}])
        operating = ecs.describe_task_definition(taskDefinition="tosunsaeng-identity:23" if key == "identity" else "tosunsaeng-learning-core:23")["taskDefinition"] if key in ("identity", "learningCore") else source
        by_container = {}
        for c in source["containerDefinitions"]:
            if key in ("identity", "learningCore"):
                names = ("MONGODB_URI", "JWT_PRIVATE_KEY_PEM", "JWT_PUBLIC_KEY_PEM", "SENTRY_DSN") if key == "identity" else ("MONGODB_URI", "SENTRY_DSN")
                refs = {n: bundles[key] + ":" + n + "::" for n in names}
                if key == "identity":
                    refs.update(prod_identity)
                else:
                    refs.update(CHALLENGE_AI_OUTBOUND_CREDENTIAL=challenge["outbound"], CHALLENGE_AI_CALLBACK_CREDENTIAL=challenge["callback"])
            else:
                refs = {}
                for old in c.get("secrets", []):
                    n = old["name"]
                    if n == "LEARNING_CORE_TO_AI_CREDENTIAL":
                        refs[n] = challenge["outbound"]
                    elif n == "LEARNING_RESULT_CALLBACK_TOKEN":
                        refs[n] = challenge["callback"]
                    elif n in ("AZURE_SPEECH_KEY", "LANGSMITH_API_KEY", "LLM_API_KEY", "OPENAI_API_KEY"):
                        refs[n] = bundles["ai"] + ":" + n + "::"
                    else:
                        raise ValueError("Unreviewed AI secret name")
            by_container[c["name"]] = refs
        spec["containerName"] = next(c["name"] for c in source["containerDefinitions"] if c.get("portMappings"))
        tasks[key] = build_task(key, spec, settings[key], source, operating, by_container)
    existing_services = ecs.describe_services(cluster=topology["cluster"], services=[v["name"] for v in topology["services"].values()])["services"]
    if any(s["status"] != "INACTIVE" for s in existing_services):
        raise ValueError("Prod service exists; stop rather than overwrite")
    existing_tg = {g["TargetGroupName"] for g in elb.describe_target_groups()["TargetGroups"]}
    if any(s["targetGroupName"] in existing_tg for s in topology["services"].values() if s["targetGroupName"]):
        raise ValueError("Prod target group exists; inspect partial work")
    existing_rules = elb.describe_rules(ListenerArn=topology["listenerArn"])["Rules"]
    if {str(r["priority"]) for r in topology["listenerRules"]} & {r["Priority"] for r in existing_rules}:
        raise ValueError("Rule priority occupied")
    for r in existing_rules:
        if any(host in json.dumps(r.get("Conditions", [])) for host in topology["dnsNames"]):
            raise ValueError("Prod route already exists")
    print("VALIDATED_FOUR_PRODUCTION_TASKS; no test secrets; all desiredCount=0", flush=True)
    if not args.apply:
        return
    targets = {}
    for key, spec in topology["services"].items():
        if not spec["targetGroupName"]:
            targets[key] = None
            continue
        target = elb.create_target_group(Name=spec["targetGroupName"], Protocol="HTTP", Port=spec["port"], VpcId=topology["vpc"],
            TargetType="ip", HealthCheckProtocol="HTTP", HealthCheckPath=spec["healthPath"], Matcher={"HttpCode": "200"},
            Tags=[{"Key": "Environment", "Value": "prod"}])["TargetGroups"][0]
        targets[key] = target["TargetGroupArn"]
        print("TARGET_CREATED", key, flush=True)
    for rule in topology["listenerRules"]:
        conditions = [{"Field": "host-header", "HostHeaderConfig": {"Values": [rule["host"]]}}]
        if rule.get("path"):
            conditions.append({"Field": "path-pattern", "PathPatternConfig": {"Values": [rule["path"]]}})
        elb.create_rule(ListenerArn=topology["listenerArn"], Priority=rule["priority"], Conditions=conditions,
            Actions=[{"Type": "forward", "TargetGroupArn": targets[rule["service"]]}])
        print("RULE_CREATED", rule["priority"], flush=True)
    for key in ("examAi", "identity", "learningCore", "challengeAi"):
        spec = topology["services"][key]
        registered = ecs.register_task_definition(**tasks[key], tags=[{"key": "Environment", "value": "prod"}, {"key": "LaunchState", "value": "PRESTART_DO_NOT_SCALE"}])["taskDefinition"]
        request = service_request(key, spec, topology, registered["taskDefinitionArn"], targets[key])
        created = ecs.create_service(**request)["service"]
        if created["desiredCount"] != 0:
            raise ValueError("Unexpected nonzero service count")
        print("SERVICE_ZERO_CREATED", key, registered["family"] + ":" + str(registered["revision"]), flush=True)
    for service in ecs.describe_services(cluster=topology["cluster"], services=[v["name"] for v in topology["services"].values()])["services"]:
        if any(service[k] != 0 for k in ("desiredCount", "runningCount", "pendingCount")):
            raise ValueError("Unexpected active tasks")
        print("ZERO_VERIFIED", service["serviceName"], flush=True)
    print("COMPLETE_ZERO_SERVICES; DNS not modified; startup not authorized", flush=True)


if __name__ == "__main__":
    main()
