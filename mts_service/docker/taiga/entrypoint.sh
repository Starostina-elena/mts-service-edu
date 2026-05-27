#!/bin/bash
set -e

# Этот скрипт будет запущен как entrypoint в taiga-back контейнере
# после инициализации Taiga

# Ждем, пока migrации завершатся
echo "Waiting for migrations to complete..."
sleep 10

# Создаем пользователя через Django shell
python manage.py shell << 'EOF'
from django.contrib.auth.models import User
from projects.models import Project

# Создаем пользователя manager
try:
    user = User.objects.get(username='manager')
    print("User 'manager' already exists")
except User.DoesNotExist:
    user = User.objects.create_user(
        username='manager',
        email='manager@example.com',
        password='managerpass'
    )
    user.is_staff = True
    user.is_superuser = True
    user.save()
    print(f"User 'manager' created successfully")

# Создаем проект
try:
    project = Project.objects.get(name='MTS Applications')
    print("Project 'MTS Applications' already exists")
except Project.DoesNotExist:
    project = Project.objects.create(
        name='MTS Applications',
        slug='mts-applications',
        description='Project for MTS service applications',
        owner=user
    )
    print(f"Project 'MTS Applications' created successfully")
EOF

echo "Taiga initialization complete!"

