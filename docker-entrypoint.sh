#!/bin/sh
set +x
set -eu
umask 077

fail() {
  echo "$1" >&2
  exit 1
}

# The directory and its parents must be controlled by the deployment, never users.
SECRET_ROOT="${IDENTITY_RUNTIME_SECRET_DIR:-/app/runtime/keys}"
case "$SECRET_ROOT" in /*) ;; *) fail "Runtime secret directory must be absolute" ;; esac
[ ! -L "$SECRET_ROOT" ] || fail "Runtime secret directory must not be a symlink"
mkdir -p "$SECRET_ROOT"
chmod 700 "$SECRET_ROOT"
# A fresh private directory avoids overwriting existing files or following file symlinks.
KEY_DIR=$(mktemp -d "${SECRET_ROOT}/boot.XXXXXXXXXX")
cleanup_failed_start() {
  rm -f "$KEY_DIR/private.pem" "$KEY_DIR/public.pem" "$KEY_DIR/firebase.json" "$KEY_DIR/reissue.properties"
  rmdir "$KEY_DIR"
}
trap cleanup_failed_start EXIT
trap 'exit 1' HUP INT TERM
PRIVATE_KEY_FILE="${KEY_DIR}/private.pem"
PUBLIC_KEY_FILE="${KEY_DIR}/public.pem"

if [ -z "${JWT_PRIVATE_KEY_PEM:-}" ]; then
  echo "JWT_PRIVATE_KEY_PEM is required" >&2
  exit 1
fi

if [ -z "${JWT_PUBLIC_KEY_PEM:-}" ]; then
  echo "JWT_PUBLIC_KEY_PEM is required" >&2
  exit 1
fi

printf '%s\n' "$JWT_PRIVATE_KEY_PEM" > "$PRIVATE_KEY_FILE"
chmod 600 "$PRIVATE_KEY_FILE"

printf '%s\n' "$JWT_PUBLIC_KEY_PEM" > "$PUBLIC_KEY_FILE"
chmod 600 "$PUBLIC_KEY_FILE"

export JWT_PRIVATE_KEY_LOCATION="file:${PRIVATE_KEY_FILE}"
export JWT_PUBLIC_KEY_LOCATION="file:${PUBLIC_KEY_FILE}"

unset JWT_PRIVATE_KEY_PEM
unset JWT_PUBLIC_KEY_PEM

# Only shell builtins receive the raw value. Do not echo it or pass it to tools.
# Result is an absolute filesystem path (not a Spring file: resource URI).
prepare_file() {
  material="$1"
  supplied_path="$2"
  generated_path="$3"
  label="$4"
  [ -z "$material" ] || [ -z "$supplied_path" ] || fail "$label: choose raw content OR an existing file"
  prepared_path="$supplied_path"
  if [ -n "$material" ]; then
    printf '%s' "$material" > "$generated_path"
    chmod 600 "$generated_path"
    prepared_path="$generated_path"
  fi
  if [ -n "$prepared_path" ]; then
    case "$prepared_path" in /*) ;; *) fail "$label: file path must be absolute" ;; esac
    [ -f "$prepared_path" ] && [ -r "$prepared_path" ] && [ -s "$prepared_path" ] \
      || fail "$label: file must be readable and nonempty"
  fi
  unset material supplied_path generated_path label
}

prepare_file "${FIREBASE_SERVICE_ACCOUNT_JSON:-}" "${GOOGLE_APPLICATION_CREDENTIALS:-}" "$KEY_DIR/firebase.json" "Firebase credentials"
if [ -n "$prepared_path" ]; then
  export GOOGLE_APPLICATION_CREDENTIALS="$prepared_path"
fi
case "${FIREBASE_AUTH_ENABLED:-false}" in
  [Tt][Rr][Uu][Ee])
    [ -n "$prepared_path" ] || fail "Firebase credentials file is required when Firebase auth is enabled"
    [ -n "${FIREBASE_PROJECT_ID:-}" ] || fail "FIREBASE_PROJECT_ID is required"
    ;;
esac
unset FIREBASE_SERVICE_ACCOUNT_JSON

prepare_file "${AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT:-}" "${AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION:-}" "$KEY_DIR/reissue.properties" "Reissue keyring"
if [ -n "$prepared_path" ]; then
  export AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION="$prepared_path"
fi
case "${AUTH_REISSUE_RECOVERY_ENABLED:-false}" in
  [Tt][Rr][Uu][Ee])
    [ -n "$prepared_path" ] || fail "Reissue keyring file is required when recovery is enabled"
    [ -n "${AUTH_REISSUE_ENCRYPTION_ACTIVE_KEY_ID:-}" ] || fail "AUTH_REISSUE_ENCRYPTION_ACTIVE_KEY_ID is required"
    [ -n "${AUTH_REISSUE_ENCRYPTION_ENVIRONMENT:-}" ] || fail "AUTH_REISSUE_ENCRYPTION_ENVIRONMENT is required"
    ;;
esac
unset AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT prepared_path

# JVM startup performs JSON/PEM/keyring semantic validation. Files remain available
# for its lifetime; use task-local ephemeral storage, never a shared persistent volume.
exec java -jar /app/app.jar
