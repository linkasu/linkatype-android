#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

exec "${ROOT_DIR}/scripts/e2e-emulator.sh" \
  --profiles phone_base_p \
  --classes ru.ibakaidov.distypepro.SmokeLoginTest
