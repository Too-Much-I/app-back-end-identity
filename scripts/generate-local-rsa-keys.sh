#!/usr/bin/env bash

set -euo pipefail

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
key_directory="${project_root}/.local/keys"
private_key="${key_directory}/private.pem"
public_key="${key_directory}/public.pem"
force=false
first_argument="${1:-}"

if [[ $# -gt 1 || (${#} -eq 1 && "${first_argument}" != "--force") ]]; then
	echo "Usage: $0 [--force]" >&2
	exit 2
fi

if [[ ${#} -eq 1 ]]; then
	force=true
fi

if ! command -v openssl >/dev/null 2>&1; then
	echo "OpenSSL is required to generate local RSA keys." >&2
	exit 1
fi

if [[ "${force}" == false && (-e "${private_key}" || -e "${public_key}") ]]; then
	echo "Local RSA keys already exist. Use --force to replace them." >&2
	exit 1
fi

mkdir -p -- "${key_directory}"
temporary_directory="$(mktemp -d "${key_directory}/.generate.XXXXXX")"

cleanup() {
	rm -f -- "${temporary_directory}/private.pem" "${temporary_directory}/public.pem"
	rmdir -- "${temporary_directory}" 2>/dev/null || true
}
trap cleanup EXIT

openssl genpkey \
	-algorithm RSA \
	-pkeyopt rsa_keygen_bits:2048 \
	-out "${temporary_directory}/private.pem" >/dev/null 2>&1

openssl pkey \
	-in "${temporary_directory}/private.pem" \
	-pubout \
	-out "${temporary_directory}/public.pem" >/dev/null 2>&1

chmod 600 "${temporary_directory}/private.pem"
chmod 644 "${temporary_directory}/public.pem"
mv -f -- "${temporary_directory}/private.pem" "${private_key}"
mv -f -- "${temporary_directory}/public.pem" "${public_key}"

echo "Local RSA key pair generated under .local/keys."
