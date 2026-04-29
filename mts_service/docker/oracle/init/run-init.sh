#!/bin/bash
set -euo pipefail

# run-init.sh - wait for Oracle to be ready and execute SQL init scripts
# Intended to be mounted into gvenzl/oracle-xe image under /opt/oracle/scripts/setup

ORACLE_HOST=${ORACLE_HOST:-localhost}
ORACLE_PORT=${ORACLE_PORT:-1521}
SERVICE=${SERVICE:-XEPDB1}
SYS_PASSWORD=${ORACLE_PASSWORD:-oracle}

# Prefer network EZCONNECT (avoids bequeath/ORA-01034 timing issues)
SQLPLUS_NET_CMD="sqlplus -s sys/$SYS_PASSWORD@//$ORACLE_HOST:$ORACLE_PORT/$SERVICE as sysdba"
SQLPLUS_LOCAL_CMD="sqlplus -s / as sysdba"

SQLPLUS_CMD=""

echo "Waiting for Oracle listener / instance... (host=$ORACLE_HOST port=$ORACLE_PORT service=$SERVICE)"
# wait until either network or local connection works
for i in {1..60}; do
  if $SQLPLUS_NET_CMD <<<"select 1 from dual;" 2>/dev/null | grep -q 1; then
    echo "Oracle reachable over network"
    SQLPLUS_CMD="$SQLPLUS_NET_CMD"
    MODE="net"
    break
  fi

  if $SQLPLUS_LOCAL_CMD <<<"select 1 from dual;" 2>/dev/null | grep -q 1; then
    echo "Oracle local connection OK (bequeath)"
    SQLPLUS_CMD="$SQLPLUS_LOCAL_CMD"
    MODE="local"
    break
  fi

  echo "Waiting for Oracle... ($i)"
  sleep 5
done

if [ -z "$SQLPLUS_CMD" ]; then
  echo "Oracle did not become ready in time" >&2
  exit 1
fi

echo "Using sqlplus mode: ${MODE}"

# Helper to run a .sql file and fail on errors reported by sqlplus
run_sql_file() {
  local file="$1"
  echo "Executing $file"
  if [ "${MODE}" = "local" ]; then
    # run via local sysdba and switch to PDB explicitly
    out=$(sqlplus -s / as sysdba <<SQL 2>&1
SET ECHO ON
SET SERVEROUTPUT ON
ALTER SESSION SET CONTAINER = ${SERVICE};
@${file}
EXIT
SQL
) || rc=$?; rc=${rc:-$?}
  else
    out=$($SQLPLUS_CMD @$file 2>&1) || rc=$?; rc=${rc:-$?}
  fi
  echo "$out"
  # If sqlplus returned non-zero OR printed ORA- SP2- or ERROR, treat as failure
  if echo "$out" | grep -E "ORA-|SP2-|ERROR" >/dev/null 2>&1; then
    echo "Error while executing $file" >&2
    return 1
  fi
  if [ "${rc:-0}" -ne 0 ]; then
    echo "sqlplus exited with code ${rc} while executing $file" >&2
    return $rc
  fi
  return 0
}

# Execute all .sql files in this directory in lexical order
shopt -s nullglob
sql_files=(/opt/oracle/scripts/setup/*.sql)
for f in "${sql_files[@]}"; do
  run_sql_file "$f" || {
    echo "Failed to execute $f" >&2
    exit 1
  }
done

# Execute any .sh scripts (excluding this one) for additional setup
shopt -s nullglob
for s in /opt/oracle/scripts/setup/*.sh; do
  [ "$s" = "/opt/oracle/scripts/setup/run-init.sh" ] && continue
  echo "Running $s"
  bash "$s" || {
    echo "Failed to run $s" >&2
    exit 1
  }
done

echo "Init scripts executed successfully"
