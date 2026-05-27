Инструкция по развёртыванию Taiga (использует уже клонированный `taiga-docker`)

Кратко: все нужные файлы уже находятся в `./taiga-docker`. Следуйте шагам ниже — они соответствуют официальному репозиторию `taiga-docker` и статье по установке.

Шаги (выполняйте в терминале, zsh):

1) Перейдите в каталог с `taiga-docker`:

```bash
cd $(pwd)/taiga-docker
```

2) Скопируйте пример `.env` в рабочий `.env` и отредактируйте значения (особенно `SECRET_KEY`, `POSTGRES_PASSWORD` и `TAIGA_DOMAIN`):

```bash
cp .env .env.local  # создаём копию, чтобы не потерять оригинал
# либо
cp .env .env
# затем откройте .env в редакторе и поправьте переменные
```

Обязательные переменные для проверки (минимум):
- POSTGRES_USER
- POSTGRES_PASSWORD
- SECRET_KEY
- TAIGA_DOMAIN (например: localhost:9000)
- при необходимости RABBITMQ_*

3) Дать права на скрипты (если ещё не заданы) и запустить Taiga:

```bash
chmod +x launch-taiga.sh taiga-manage.sh
./launch-taiga.sh
```

Этот скрипт выполнит `docker compose -f docker-compose.yml up -d` и поднимет контейнеры.

4) Инициализация (создание суперпользователя и миграции):

Запустите команды управления Django через контейнер `taiga-manage` из `docker-compose-inits.yml`:

```bash
./taiga-manage.sh migrate
./taiga-manage.sh createsuperuser
```

Если нужно импортировать начальные данные (опционально, см. README в taiga-docker):

```bash
./taiga-manage.sh loaddata initial_data.json
```

5) Проверка состояния и логов:

```bash
# показать контейнеры
docker compose ps
# смотреть логи back
docker compose logs -f taiga-back
# смотреть логи gateway (фронтенд)
docker compose logs -f taiga-gateway
```

6) Откройте в браузере:

http://localhost:9000

Если Docker не установлен на машине, установите его по инструкции с сайта docker.com.

Примечание: если хотите запускать через верхний `docker-compose.yml` в этом каталоге, можно адаптировать его для использования сервисов из `taiga-docker`, но проще использовать предоставленные скрипты `launch-taiga.sh` и `taiga-manage.sh`.

Если нужно — могу автоматически:
- скопировать `.env` в `.env.local` и подставить значения по умолчанию;
- настроить привязку volumes в корне `mts_service/docker/taiga`;
- или адаптировать верхний `docker-compose.yml` чтобы он ссылался на `taiga-docker` сервисы.

Скажите, делаем дальше: автоматически заполнить `.env` и запустить проверку `docker compose config` (если Docker доступен) или оставить инструкции и помочь с запуском у вас локально?
