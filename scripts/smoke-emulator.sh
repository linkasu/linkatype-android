#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

: "${SMOKE_EMAIL:?SMOKE_EMAIL is required in .env}"
: "${SMOKE_PASSWORD:?SMOKE_PASSWORD is required in .env}"

if ! adb get-state >/dev/null 2>&1; then
  echo "No emulator/device found. Start an emulator before running this script." >&2
  exit 1
fi

"${ROOT_DIR}/gradlew" connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.email="${SMOKE_EMAIL}" \
  -Pandroid.testInstrumentationRunnerArguments.password="${SMOKE_PASSWORD}"
