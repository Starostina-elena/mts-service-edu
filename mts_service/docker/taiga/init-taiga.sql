#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    INSERT INTO auth_user (username, password, email, first_name, last_name, is_staff, is_active, is_superuser, last_login, date_joined)
    VALUES ('manager', 'pbkdf2_sha256\$260000\$salt\$hash', 'manager@example.com', 'Manager', 'User', true, true, true, NOW(), NOW())
    ON CONFLICT (username) DO NOTHING;
EOSQL

