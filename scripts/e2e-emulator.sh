#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

DEVICE_SERIAL="${DEVICE_SERIAL:-}"
ARTIFACTS_ROOT="${ARTIFACTS_ROOT:-${ROOT_DIR}/artifacts/e2e}"
DEFAULT_TEST_CLASSES="ru.ibakaidov.distypepro.SmokeLoginTest,ru.ibakaidov.distypepro.BankPlacementSwitchTest,ru.ibakaidov.distypepro.BankHostParityTest"
SELECTED_PROFILES="${PROFILES:-phone_small_p,phone_base_p,phone_base_l,tablet_p,tablet_l}"
TEST_CLASSES="${TEST_CLASSES:-${DEFAULT_TEST_CLASSES}}"
SKIP_BUILD="${SKIP_BUILD:-0}"

ADB=(adb)
if [[ -n "${DEVICE_SERIAL}" ]]; then
  ADB+=( -s "${DEVICE_SERIAL}" )
fi

APP_APK="${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="${ROOT_DIR}/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
TIMESTAMP="$(date +"%Y%m%d-%H%M%S")"
RUN_DIR="${ARTIFACTS_ROOT}/${TIMESTAMP}"
INSTRUMENTATION=""
APP_PACKAGE="ru.ibakaidov.distypepro"
TEST_PACKAGE=""

usage() {
  cat <<'EOF'
Usage: scripts/e2e-emulator.sh [--profiles profile1,profile2] [--classes class1,class2] [--skip-build]

Profiles:
  phone_small_p
  phone_base_p
  phone_base_l
  tablet_p
  tablet_l
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profiles)
      SELECTED_PROFILES="$2"
      shift 2
      ;;
    --classes)
      TEST_CLASSES="$2"
      shift 2
      ;;
    --skip-build)
      SKIP_BUILD=1
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

ensure_device() {
  if ! "${ADB[@]}" get-state >/dev/null 2>&1; then
    echo "No emulator/device found. Start an emulator before running this script." >&2
    exit 1
  fi
}

build_apks() {
  if [[ "${SKIP_BUILD}" == "1" ]]; then
    return
  fi
  "${ROOT_DIR}/gradlew" assembleDebug assembleDebugAndroidTest
}

install_apks() {
  "${ADB[@]}" install -r "${APP_APK}" >/dev/null
  "${ADB[@]}" install -r "${TEST_APK}" >/dev/null
}

resolve_instrumentation() {
  local entry
  entry="$("${ADB[@]}" shell pm list instrumentation | tr -d '\r' | grep "target=${APP_PACKAGE}" | head -n 1 || true)"
  if [[ -z "${entry}" ]]; then
    echo "Unable to resolve instrumentation for ${APP_PACKAGE}" >&2
    exit 1
  fi
  INSTRUMENTATION="$(sed -E 's/^instrumentation:([^ ]+) .*/\1/' <<<"${entry}")"
  TEST_PACKAGE="${INSTRUMENTATION%%/*}"
}

disable_animations() {
  "${ADB[@]}" shell settings put global window_animation_scale 0 >/dev/null
  "${ADB[@]}" shell settings put global transition_animation_scale 0 >/dev/null
  "${ADB[@]}" shell settings put global animator_duration_scale 0 >/dev/null
}

enable_soft_keyboard_with_hardware() {
  "${ADB[@]}" shell settings put secure show_ime_with_hard_keyboard 1 >/dev/null || true
}

reset_window_metrics() {
  "${ADB[@]}" shell wm size reset >/dev/null || true
  "${ADB[@]}" shell wm density reset >/dev/null || true
  "${ADB[@]}" shell settings put system accelerometer_rotation 1 >/dev/null || true
}

apply_window_profile() {
  local dp_width="$1"
  local dp_height="$2"
  local density="$3"
  local rotation="$4"
  local px_width=$(((dp_width * density + 80) / 160))
  local px_height=$(((dp_height * density + 80) / 160))

  "${ADB[@]}" shell wm size "${px_width}x${px_height}" >/dev/null
  "${ADB[@]}" shell wm density "${density}" >/dev/null
  "${ADB[@]}" shell settings put system accelerometer_rotation 0 >/dev/null
  "${ADB[@]}" shell settings put system user_rotation "${rotation}" >/dev/null
  sleep 2
}

apply_profile() {
  local profile="$1"
  case "${profile}" in
    phone_small_p)
      apply_window_profile 360 640 320 0
      ;;
    phone_base_p)
      apply_window_profile 412 915 420 0
      ;;
    phone_base_l)
      apply_window_profile 915 412 420 1
      ;;
    tablet_p)
      apply_window_profile 800 1280 240 0
      ;;
    tablet_l)
      apply_window_profile 1280 800 240 1
      ;;
    *)
      echo "Unknown profile: ${profile}" >&2
      exit 1
      ;;
  esac
}

clear_app_state() {
  "${ADB[@]}" shell pm clear "${APP_PACKAGE}" >/dev/null
  if [[ -n "${TEST_PACKAGE}" ]]; then
    "${ADB[@]}" shell pm clear "${TEST_PACKAGE}" >/dev/null || true
  fi
}

collect_failure_artifacts() {
  local profile="$1"
  local profile_dir="${RUN_DIR}/${profile}"
  mkdir -p "${profile_dir}"

  "${ADB[@]}" logcat -d > "${profile_dir}/logcat.txt" || true
  "${ADB[@]}" exec-out screencap -p > "${profile_dir}/screen.png" || true
  "${ADB[@]}" shell uiautomator dump /sdcard/window_dump.xml >/dev/null || true
  "${ADB[@]}" pull /sdcard/window_dump.xml "${profile_dir}/window_dump.xml" >/dev/null || true
}

run_instrumentation() {
  local profile="$1"
  local profile_dir="${RUN_DIR}/${profile}"
  mkdir -p "${profile_dir}"

  "${ADB[@]}" logcat -c || true

  set +e
  "${ADB[@]}" shell am instrument -w -r \
    -e clearPackageData true \
    -e email "${SMOKE_EMAIL}" \
    -e password "${SMOKE_PASSWORD}" \
    -e class "${TEST_CLASSES}" \
    "${INSTRUMENTATION}" | tee "${profile_dir}/instrumentation.txt"
  local exit_code=${PIPESTATUS[0]}
  set -e

  if [[ ${exit_code} -ne 0 ]]; then
    collect_failure_artifacts "${profile}"
    return ${exit_code}
  fi
}

run_profile() {
  local profile="$1"
  echo "==> Running profile: ${profile}"
  trap reset_window_metrics RETURN
  clear_app_state
  apply_profile "${profile}"
  enable_soft_keyboard_with_hardware
  run_instrumentation "${profile}"
}

main() {
  : "${SMOKE_EMAIL:?SMOKE_EMAIL is required in .env}"
  : "${SMOKE_PASSWORD:?SMOKE_PASSWORD is required in .env}"
  ensure_device
  mkdir -p "${RUN_DIR}"
  build_apks
  install_apks
  resolve_instrumentation
  disable_animations

  IFS=',' read -r -a profiles <<< "${SELECTED_PROFILES}"
  for profile in "${profiles[@]}"; do
    run_profile "${profile}"
  done

  echo "E2E artifacts: ${RUN_DIR}"
}

main "$@"
