#!/usr/bin/env python3
"""TMI-191: operator-only, dry-run by default. Never run inside the public API.

Dependencies: pymongo, google-auth, requests. Credentials are supplied through
MONGODB_URI and GOOGLE_APPLICATION_CREDENTIALS; never command arguments/output.
See docs/contracts/provider-link-recovery-operations.md before use.
"""
import argparse
import hashlib
import json
import os
import re
from datetime import datetime, timedelta, timezone


class UnsafeRecovery(Exception):
    pass


def require(condition):
    if not condition:
        raise UnsafeRecovery("RECOVERY_PRECONDITION_FAILED")


def validate(s, remote, now):
    a, c, m, b, u = (s[k] for k in ("attempt", "control", "methods", "binding", "user"))
    require(all((a, c, m, b, u)))
    require(a["state"] == "STARTED" and a["expiresAt"] <= now)
    require(u.get("accountType") == "MEMBER" and u.get("status") == "ACTIVE")
    require(a["userId"] == u["_id"] == c["_id"] == m["_id"] == b["userId"])
    require(a["bindingId"] == b["_id"] == m["bindingId"])
    require(a["bindingCreatedAt"] == b["createdAt"])
    require(a["projectId"] == b["firebaseProjectId"] and a["firebaseUid"] == b["firebaseUid"])
    require(c.get("activeLogoutId") == "link:" + a["_id"] and c["sessionEpoch"] == a["epoch"])
    require(m["revision"] == a["revision"] and m.get("blocked", {}).get(a["provider"]) == a["startedAt"])
    require(all(isinstance(x.get("version"), int) for x in (a, c, m)))
    require(not any(s[k] for k in ("otherLinks", "unlinks", "relinks", "logouts")))
    require(remote.get("localId") == b["firebaseUid"] and not remote.get("disabled", False))
    require(bool(remote.get("phoneNumber")))
    mapping = {"GOOGLE": "google.com", "APPLE": "apple.com", "KAKAO": "oidc.kakao"}
    require(a["provider"] in mapping)
    providers = {p["providerId"]: p["rawId"] for p in remote.get("providerUserInfo", [])}
    require(mapping[a["provider"]] not in providers)
    require(all(p["provider"] != a["provider"] for p in s["socials"]))
    require(bool(s["socials"]))
    for p in s["socials"]:
        require(providers.get(mapping.get(p["provider"])) == p["providerSubject"])
    require(set(providers) - {"phone"} == {mapping[p["provider"]] for p in s["socials"]})
    # Exact previous/local comparison is encoded in the review digest; unknown providers fail closed.
    require(set(providers).issubset(set(mapping.values()) | {"phone"}))


def read_snapshot(db, attempt_id, session):
    opts = {"session": session}
    a = db.provider_link_attempts.find_one({"_id": attempt_id}, **opts)
    require(a is not None)
    owner = a["userId"]
    one = lambda name, query: db[name].find_one(query, **opts)
    many = lambda name, query: list(db[name].find(query, **opts).sort("_id", 1).limit(101))
    s = {"attempt": a, "control": one("user_session_controls", {"_id": owner}),
         "methods": one("auth_method_change_controls", {"_id": owner}),
         "binding": one("firebase_identities", {"_id": a["bindingId"]}),
         "user": one("users", {"_id": owner}),
         "socials": many("social_identities", {"userId": owner}),
         "otherLinks": many("provider_link_attempts", {"userId": owner, "state": "STARTED", "_id": {"$ne": attempt_id}}),
         "unlinks": many("provider_unlink_operations", {"userId": owner, "state": {"$nin": ["COMPLETED", "SUPERSEDED"]}}),
         "relinks": many("provider_relink_attempts", {"userId": owner, "consumedAt": None}),
         "logouts": many("logout_all_operations", {"userId": owner, "status": {"$nin": ["COMPLETED", "SUPERSEDED_BY_WITHDRAWAL"]}})}
    require(len(s["socials"]) < 101)
    return s


def digest(s, remote):
    from bson.json_util import dumps, CANONICAL_JSON_OPTIONS
    data = dumps({"snapshot": s, "remote": remote}, json_options=CANONICAL_JSON_OPTIONS, sort_keys=True)
    return hashlib.sha256(data.encode()).hexdigest()


def apply_resolution(db, s, review_digest, approval, resolution, now, session):
    """Called only after remote recheck/approval. No block/floor/epoch edits."""
    a, c, m = (s[k] for k in ("attempt", "control", "methods"))
    require(resolution in ("CANCELLED", "FAILED"))
    audit_id = "TMI-191:" + a["_id"]
    db.provider_link_recovery_audit.insert_one({"_id": audit_id, "approvalReference": approval,
        "reviewDigest": review_digest, "resolvedAt": now, "resolution": resolution,
        "sdkStoppedConfirmed": True, "remoteTargetAbsent": True,
        "snapshots": {k: s[k] for k in ("attempt", "control", "methods", "binding")}}, session=session)
    for collection, expected, extra, update in (
        ("user_session_controls", c, {"activeLogoutId": "link:" + a["_id"], "sessionEpoch": a["epoch"]},
         {"$set": {"activeLogoutId": None}, "$inc": {"version": 1, "revision": 1}}),
        ("auth_method_change_controls", m, {"bindingId": a["bindingId"], "revision": a["revision"]},
         {"$inc": {"version": 1, "revision": 1}}),
        ("provider_link_attempts", a, {"userId": a["userId"], "state": "STARTED", "revision": a["revision"]},
         {"$set": {"state": resolution, "resolvedAt": now, "resolutionReason": "OPERATOR_VERIFIED_REMOTE_ABSENT",
                    "cleanupAt": now + timedelta(days=7)}, "$inc": {"version": 1}})):
        query = {"_id": expected["_id"], "version": expected["version"], **extra}
        require(db[collection].update_one(query, update, session=session).modified_count == 1)


def main():
    parser = argparse.ArgumentParser(description="Restricted provider link recovery (dry-run default)")
    parser.add_argument("--database", required=True)
    parser.add_argument("--project", required=True)
    parser.add_argument("--attempt-id", required=True)
    parser.add_argument("--expected-digest")
    parser.add_argument("--approval-reference")
    parser.add_argument("--sdk-stopped-confirmed", action="store_true")
    parser.add_argument("--resolution", choices=["CANCELLED", "FAILED"], default="FAILED")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    if args.apply:
        require(args.sdk_stopped_confirmed and bool(args.expected_digest))
        require(bool(re.fullmatch(r"[A-Z][A-Z0-9]*-\d+", args.approval_reference or "")))
    from pymongo import MongoClient
    from pymongo.read_concern import ReadConcern
    from pymongo.write_concern import WriteConcern
    from pymongo.read_preferences import ReadPreference
    from google.oauth2 import service_account
    from google.auth.transport.requests import AuthorizedSession
    credentials = service_account.Credentials.from_service_account_file(
        os.environ["GOOGLE_APPLICATION_CREDENTIALS"], scopes=["https://www.googleapis.com/auth/identitytoolkit"])
    http = AuthorizedSession(credentials)
    def remote_read(uid):
        response = http.post("https://identitytoolkit.googleapis.com/v1/projects/" + args.project + "/accounts:lookup",
                             json={"localId": [uid]}, timeout=10)
        require(response.status_code == 200)
        users = response.json().get("users", [])
        require(len(users) == 1)
        # Only security-relevant stable fields; no email/display name/password hashes.
        return {k: users[0].get(k) for k in ("localId", "disabled", "createdAt", "validSince", "phoneNumber", "providerUserInfo")}
    with MongoClient(os.environ["MONGODB_URI"], tz_aware=True, serverSelectionTimeoutMS=10000) as client:
        db = client[args.database]
        # Completed/uncertain commit retries never repeat writes.
        audit = db.provider_link_recovery_audit.find_one({"_id": "TMI-191:" + args.attempt_id})
        if audit:
            require(audit.get("reviewDigest") == args.expected_digest and audit.get("approvalReference") == args.approval_reference)
            print("RECOVERY_ALREADY_RECORDED_VERIFY_STATUS"); return
        with client.start_session() as session:
            with session.start_transaction(read_concern=ReadConcern("snapshot"), write_concern=WriteConcern("majority"),
                                           read_preference=ReadPreference.PRIMARY, max_commit_time_ms=10000):
                s = read_snapshot(db, args.attempt_id, session)
                require(s["attempt"]["projectId"] == args.project)
                remote = remote_read(s["attempt"]["firebaseUid"])
                now = datetime.now(timezone.utc)
                validate(s, remote, now)
                review = digest(s, remote)
                if not args.apply:
                    print(json.dumps({"dryRunValid": True, "reviewDigest": review, "writes": 0})); return
                require(review == args.expected_digest)
                require(remote_read(s["attempt"]["firebaseUid"]) == remote)
                apply_resolution(db, s, review, args.approval_reference, args.resolution, now, session)
        print("RECOVERY_COMMITTED")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Never emit driver/provider exceptions: they can contain credentials/identifiers.
        print("RECOVERY_NOT_CONFIRMED_VERIFY_AUDIT_AND_STATUS_NO_BLIND_RETRY")
        raise SystemExit(1)
