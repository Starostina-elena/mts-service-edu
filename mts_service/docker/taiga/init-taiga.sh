#!/bin/bash
set -e

echo "Waiting for Taiga to initialize database..."
sleep 30

# Проверяем, существует ли таблица auth_user
for i in {1..60}; do
    if psql -v ON_ERROR_STOP=1 --username "taiga" --dbname "taiga" -c "SELECT 1 FROM auth_user LIMIT 1" > /dev/null 2>&1; then
        echo "Database is ready"
        break
    fi
    echo "Waiting... attempt $i/60"
    sleep 1
done

# Создаем пользователя
echo "Creating manager user..."
psql -v ON_ERROR_STOP=1 --username "taiga" --dbname "taiga" <<-EOSQL
    DELETE FROM auth_user WHERE username='manager';

    INSERT INTO auth_user (
        username,
        email,
        password,
        first_name,
        last_name,
        is_staff,
        is_active,
        is_superuser,
        date_joined
    ) VALUES (
        'manager',
        'manager@example.com',
        'pbkdf2_sha256\$260000\$VWqW6yy0sZrr0tMkfCd5Wg\$NX8AwMqpPmKFhCbM3vFp3dMhKXJ1mZ7sJq8vXqXqJ5M=',
        'Manager',
        'User',
        true,
        true,
        true,
        NOW()
    );
EOSQL

echo "Taiga initialization complete"
