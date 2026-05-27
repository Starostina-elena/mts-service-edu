#!/bin/bash

echo "Waiting for taiga-back to be ready..."
for i in {1..120}; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://taiga-back:8000/api/v1/)
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "404" ]; then
        echo "Taiga is ready!"
        break
    fi
    echo "Attempt $i/120, HTTP Code: $HTTP_CODE"
    sleep 1
done

sleep 3

# Получаем токен или логиним
echo "Trying to authenticate as manager..."
AUTH_RESPONSE=$(curl -s -X POST http://taiga-back:8000/api/v1/auth \
  -H "Content-Type: application/json" \
  -d '{
    "username": "manager",
    "password": "managerpass"
  }')

echo "Auth response: $AUTH_RESPONSE"

TOKEN=$(echo "$AUTH_RESPONSE" | grep -o '"auth_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "Failed to get token, user might not exist or credentials are wrong"
  echo "Full response: $AUTH_RESPONSE"
else
  echo "Got token: $TOKEN"

  # Получаем список проектов
  echo "Checking projects..."
  PROJECTS=$(curl -s -H "X-TAIGA-TOKEN: $TOKEN" http://taiga-back:8000/api/v1/projects/)

  if echo "$PROJECTS" | grep -q "MTS Applications"; then
    echo "Project already exists"
  else
    echo "Creating project..."
    curl -s -X POST http://taiga-back:8000/api/v1/projects \
      -H "Content-Type: application/json" \
      -H "X-TAIGA-TOKEN: $TOKEN" \
      -d '{
        "name": "MTS Applications",
        "description": "Project for MTS service applications"
      }'
  fi
fi

echo "Initialization complete!"
