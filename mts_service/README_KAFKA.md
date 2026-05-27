Как поднять Kafka и проверить асинхронную обработку заявок

Коротко
- Я переместил сервисы Zookeeper и Kafka в общий `docker-compose.yml` в папке `mts_service`.
- Приложение (`app`) получает адрес брокера через переменную окружения `KAFKA_BOOTSTRAP` (значение `kafka:9092` в compose). Spring сопоставляет это с проперти `kafka.bootstrap`.
- При создании заявки (`POST /applications`) приложение сохраняет запись в БД и публикует событие в топик `applications.created`.
- В приложении запущены два consumer'а (внутри контейнера `app`): `TaigaConsumerWorker` и `NotificationConsumerWorker`.

Что изменено (файлы)
- `docker-compose.yml` — добавлены сервисы `zookeeper` и `kafka`, а также `KAFKA_BOOTSTRAP` в окружение `app`.
- `docker-compose.kafka.yml` — помечен как устаревший (deprecated).
- Java: добавлены классы в `ru.aigul.mts_service.kafka`: `ApplicationCreatedEvent`, `KafkaProducerService`, `TaigaConsumerWorker`, `NotificationConsumerWorker`.
- `ApplicationService.create(...)` теперь публикует событие в Kafka вместо синхронного создания issue в Taiga.

Как поднять локально
1) Откройте терминал в папке проекта `mts_service`:

```bash
cd /Users/elena/Desktop/studies/6_semestr/blps/lab1/mts-service-edu/mts_service
```

2) Поднять стек (в фоне):

```bash
docker compose up -d
```

(Если у вас старый docker-compose: `docker-compose up -d`.)

3) (опционально) создать топик вручную, чтобы иметь контроль над partition/replication:

```bash
# создаст topic applications.created с 3 партициями
docker compose exec kafka bash -lc "kafka-topics --create --topic applications.created --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1"
```

Примечание: в некоторых образах путь к утилитам Kafka может отличаться, но команда `kafka-topics` обычно доступна в контейнере `confluentinc/cp-kafka`.

Проверка и отладка
- Посмотреть логи приложения (в них вы увидите отправку events и логи консьюмеров):

```bash
docker compose logs -f app
```

- Посмотреть логи Kafka/Zookeeper:

```bash
docker compose logs -f kafka
docker compose logs -f zookeeper
```

- Потреблять сообщения из топика (например, чтобы увидеть payload):

```bash
docker compose exec kafka bash -lc "kafka-console-consumer --bootstrap-server localhost:9092 --topic applications.created --from-beginning --max-messages 10"
```

Тестовый запрос (создание заявки)
- Пример curl, отправляет заявку от пользователя с id=1 (заголовок `X-User-Id` используется контроллером):

```bash
curl -v -X POST http://localhost:8080/applications \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"tariffId":1,"address":"ул. Ленина, 1","cityId":null,"additionalServiceIds":[]} '
```

Ожидаемое поведение
- Контроллер сразу вернёт созданную заявку (201). В фоне приложение отправит JSON-сообщение в `applications.created`.
- `NotificationConsumerWorker` прочитает сообщение и отправит email (если вы настроили `spring.mail.*`), иначе просто залогирует уведомление в логах `app`.
- `TaigaConsumerWorker` прочитает сообщение и попробует создать issue через JCA `TaigaConnectionFactory` (если Taiga доступен), иначе залогирует ошибку.

Запуск двух узлов (распределённая обработка)
- Чтобы имитировать два независимых узла приложения, можно масштабировать сервис `app`:

```bash
# Запустит два экземпляра контейнера приложения (каждый — отдельный consumer instance)
docker compose up -d --scale app=2
```

Kafka распределит партиции между экземплярами в рамках consumer groups (`taiga-integration` и `notifications`), таким образом они будут обрабатывать сообщения параллельно на двух узлах.

Дальше
- Если хотите, я могу: (A) запустить у себя docker-compose (если разрешите), (B) добавить автоматическое создание топика при старте приложения, (C) реализовать outbox-паттерн для надёжной доставки, (D) настроить retry/DLQ логику для консьюмеров.

Если нужно — запускаем стек и прогоняем тестовый POST, посмотрим логи и убедимся, что сообщения доходят до консьюмеров.
