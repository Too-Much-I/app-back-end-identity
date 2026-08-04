#!/bin/sh
set -eu

KEY_DIR="/app/runtime/keys"
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

mkdir -p "$KEY_DIR"

umask 077
printf '%s\n' "$JWT_PRIVATE_KEY_PEM" > "$PRIVATE_KEY_FILE"
chmod 600 "$PRIVATE_KEY_FILE"

printf '%s\n' "$JWT_PUBLIC_KEY_PEM" > "$PUBLIC_KEY_FILE"
chmod 600 "$PUBLIC_KEY_FILE"

export JWT_PRIVATE_KEY_LOCATION="file:${PRIVATE_KEY_FILE}"
export JWT_PUBLIC_KEY_LOCATION="file:${PUBLIC_KEY_FILE}"

unset JWT_PRIVATE_KEY_PEM
unset JWT_PUBLIC_KEY_PEM

exec java -jar /app/app.jar
