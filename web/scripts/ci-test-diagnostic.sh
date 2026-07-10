#!/usr/bin/env bash
set +e

LOG_FILE="${RUNNER_TEMP:-/tmp}/joprelys-angular-tests.log"
npx ng test >"$LOG_FILE" 2>&1
STATUS=$?

if [ "$STATUS" -ne 0 ]; then
  echo "=== ANGULAR TEST FAILURE SUMMARY ==="
  grep -E -B 2 -A 6 "FAIL|FAILED|Error:|error TS|TS[0-9]{4}|NG[0-9]{4}|AssertionError|Expected|TypeError" "$LOG_FILE" \
    | tail -n 140 \
    || tail -n 140 "$LOG_FILE"
else
  tail -n 30 "$LOG_FILE"
fi

exit "$STATUS"
