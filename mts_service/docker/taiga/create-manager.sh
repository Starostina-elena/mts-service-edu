#!/bin/bash
set -e
python3 manage.py shell <<'PY'
from django.contrib.auth import get_user_model
User=get_user_model()
if User.objects.filter(username='manager').exists():
    print('manager_exists')
else:
    User.objects.create_superuser('manager','manager@example.com','managerpass')
    print('manager_created')
PY

