#!/usr/bin/env python3
"""User-run CloudShell bootstrap. Create-only; never print or rotate secrets.

Default: read-only plan. --apply requires an interactive confirmation.
Does not create Firebase credentials, grant IAM access, or start services.
"""
import argparse
import base64
import secrets

REGION = "ap-northeast-2"
ACCOUNT = "889384901776"
KEY_ID = "prod-v1"
TARGETS = {
    "tosunsaeng/prod/identity/reissue-encryption-keyring": "reissue",
    "tosunsaeng/prod/identity/phone-fingerprint": "phone",
    "tosunsaeng/prod/identity/phone-eligibility-binding": "phone",
    "tosunsaeng/prod/identity/account-recovery-key-ring": "recovery",
    "tosunsaeng/prod/challenge/outbound": "token",
    "tosunsaeng/prod/challenge/callback": "token",
}


def material(kind):
    if kind == "token":
        return secrets.token_urlsafe(32)
    key = base64.b64encode(secrets.token_bytes(32)).decode("ascii")
    if kind == "reissue":
        return f"{KEY_ID}={key}\n"
    if kind == "phone":
        return f"{KEY_ID},ACTIVE_WRITE,{key}"
    if kind == "recovery":
        return f"{KEY_ID}:{key}"
    raise ValueError("Unsupported material kind")


def inventory(client):
    missing = []
    for name in TARGETS:
        try:
            metadata = client.describe_secret(SecretId=name)
        except client.exceptions.ResourceNotFoundException:
            missing.append(name)
            print("MISSING", name)
            continue
        if metadata.get("DeletedDate"):
            raise RuntimeError("Target is pending deletion; resolve manually")
        print("EXISTS_UNVALIDATED", name)
    return missing


def create_missing(client, missing):
    for name in missing:
        try:
            client.create_secret(
                Name=name,
                Description="New production bootstrap; do not rotate without migration review",
                SecretString=material(TARGETS[name]),
                Tags=[{"Key": "Environment", "Value": "prod"}],
            )
            print("CREATED", name)
        except client.exceptions.ResourceExistsException:
            print("EXISTS_UNVALIDATED", name)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    import boto3  # Available in AWS CloudShell; no dependency for offline tests.
    session = boto3.Session(region_name=REGION)
    if session.client("sts").get_caller_identity()["Account"] != ACCOUNT:
        raise RuntimeError("Unexpected AWS account; no writes performed")
    client = session.client("secretsmanager")
    missing = inventory(client)
    if not args.apply or not missing:
        print("No writes. Existing values require separate validation.")
        return
    print(f"Create {len(missing)} Secrets Manager secrets in {ACCOUNT}/{REGION}.")
    print("Standard AWS storage/API charges apply. No existing values or IAM will change.")
    if input("Type CREATE-PROD-SECRETS to continue: ") != "CREATE-PROD-SECRETS":
        print("Cancelled; no writes.")
        return
    create_missing(client, missing)
    print("Creation completed. Firebase, IAM, ECS and network setup remain pending.")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Do not print SDK/parser exception text or local variables containing values.
        raise SystemExit("Stopped. No existing secrets were overwritten; rerun the read-only plan to inspect partial creation.") from None
