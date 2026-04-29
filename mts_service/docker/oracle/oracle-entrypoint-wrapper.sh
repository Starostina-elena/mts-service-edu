#!/bin/bash
set -euo pipefail

# Start original container entrypoint in background
container-entrypoint.sh "$@" &
MAIN_PID=$!

# Forward termination signals to child so container can stop gracefully
trap 'echo "Signal received, forwarding to pid $MAIN_PID"; kill -TERM "$MAIN_PID" 2>/dev/null || true' TERM INT

echo "Started original entrypoint (pid=$MAIN_PID), waiting for DB instance to be OPEN..."

# Wait until v$instance.status = 'OPEN' (resilient to transient errors like ORA-01034)
MAX_ATTEMPTS=120
SLEEP_SECS=5
attempt=1
while [ $attempt -le $MAX_ATTEMPTS ]; do
  out=$(sqlplus -s / as sysdba <<'SQL' 2>&1
SET HEADING OFF
SET FEEDBACK OFF
SELECT status FROM v\$instance;
EXIT
SQL
) || out="$out"

  if echo "$out" | grep -qi "OPEN"; then
    echo "Database instance status=OPEN"
    break
  fi

  echo "Waiting for DB instance to become OPEN... (attempt=$attempt/$MAX_ATTEMPTS)"
  attempt=$((attempt + 1))
  sleep $SLEEP_SECS
done

# Final check
if ! sqlplus -s / as sysdba <<<"SET HEADING OFF; SET FEEDBACK OFF; SELECT status FROM v\$instance;" 2>/dev/null | grep -qi OPEN; then
  echo "Database did not become OPEN in time" >&2
  # Let main process continue but exit non-zero to signal failure
  # Wait a short period for the main process and then exit
  sleep 2
  wait "$MAIN_PID" || true
  exit 1
fi

# Run init scripts if present (use bash to avoid relying on executable bit). Retry a few times if transient failures occur.
INIT_SCRIPT=/opt/oracle/scripts/setup/run-init.sh
if [ -f "$INIT_SCRIPT" ]; then
  MAX_INIT_ATTEMPTS=6
  init_attempt=1
  while [ $init_attempt -le $MAX_INIT_ATTEMPTS ]; do
    echo "Running init attempt $init_attempt/$MAX_INIT_ATTEMPTS"
    if bash "$INIT_SCRIPT"; then
      echo "Init scripts executed successfully"
      break
    else
      echo "run-init.sh failed on attempt $init_attempt"
      init_attempt=$((init_attempt + 1))
      sleep 5
    fi
  done
  if [ $init_attempt -gt $MAX_INIT_ATTEMPTS ]; then
    echo "run-init.sh failed after $MAX_INIT_ATTEMPTS attempts" >&2
    # decide: continue or exit non-zero. We'll exit non-zero to alert user.
    wait "$MAIN_PID" || true
    exit 1
  fi
else
  echo "No run-init.sh found at $INIT_SCRIPT"
fi

# Wait for main process to exit and forward its exit code
wait "$MAIN_PID"
EXIT_CODE=$?
exit "$EXIT_CODE"

# Oracle wrapper removed — Oracle support replaced by second Postgres instance.
# This file is intentionally left as a placeholder and should not be executed.
