Цель

Минимально и аккуратно вынести таблицу `balances` во вторую PostgreSQL базу и настроить распределённые транзакции (XA) через Narayana так, чтобы:
- при одобрении заявки админом списывались деньги с аккаунта пользователя (если хватает) и операция откатывалась, если денег недостаточно;
- пополнение баланса у пользователя работало.

Общее решение (кратко)

1) Использовать JTA (Narayana) и два XA DataSource (primary и balance).
2) Оставить основную БД как есть для всех сущностей кроме `Balance`.
3) Перенести сущность `Balance` и её репозиторий в отдельный пакет/модуль, настроить для них отдельный EntityManagerFactory, который использует второй (XA) DataSource.
4) Использовать общую JTA-транзакцию (Narayana) для операций, затрагивающих обе БД (в частности: при одобрении заявки сначала проверить и списать баланс во второй БД, затем пометить заявку в первой БД). Транзакция должна быть атомарной.

Чеклист (что сделать)

- [ ] добавить зависимость `spring-boot-starter-jta-narayana` в `build.gradle`
- [ ] добавить конфигурацию второго XA DataSource (например, `balanceDataSource`) и настроить `LocalContainerEntityManagerFactoryBean` для пакета с сущностью `Balance`
- [ ] конфиг `@EnableJpaRepositories` для основного репозитория и отдельный `@EnableJpaRepositories` для `balance`-репозиториев (указать `entityManagerFactoryRef` и `transactionManagerRef`)
- [ ] убедиться, что обе EMF используют JTA (свойство `jakarta.persistence.transactionType=JTA` и `hibernate.transaction.jta.platform` = Narayana)
- [ ] перенести `Balance` в отдельный пакет (например, `ru.aigul.mts_service.balance.model`) и `BalanceRepository` в `ru.aigul.mts_service.balance.repository`
- [ ] изменить `ApplicationService.approve(...)` так, чтобы внутри JTA-транзакции проверять и списывать баланс через `BalanceRepository`, и только после успешного списания помечать `Application` как APPROVED; если денег недостаточно — бросать `InsufficientFundsException` и откатить всю транзакцию
- [ ] реализовать пополнение баланса: добавить в `BalanceService` метод, который в JTA-транзакции создает/обновляет запись в `balances` во второй БД; для упрощённого тестирования добавить тестовый callback-эндпоинт или временный endpoint, который напрямую применяет пополнение
- [ ] добавить отдельную Flyway конфигурацию (или DDL-скрипт) для создания таблицы `balances` во второй БД (включая внешний ключ на users, либо можно хранить user_id как plain long — минимально: оставить FK, если cross-db FK невозможен, убрать FK и хранить user_id)
- [ ] добавить в `docker-compose.yml` второй service `balance_db` (Postgres на отдельном порту, напр. 5433) и передать переменные окружения в `app`
- [ ] обновить `application.properties` (или `application-local.properties`) — добавить переменные подключения для второй БД: `spring.datasource.balance.url`, `...username`, `...password` (имена любые, затем использовать их в конфиге)
- [ ] покрыть изменения простыми интеграционными проверками (см. раздел "Тестирование")

Файлы/места для правок (минимально и аккуратно)

- `build.gradle` — добавить `implementation 'org.springframework.boot:spring-boot-starter-jta-narayana'`
- `src/main/java/.../model/Balance.java` — переместить пакет в `ru.aigul.mts_service.balance.model` и, при необходимости, убрать cross-DB foreign key (заменить `User` на `Long userId`) если проще
- `src/main/java/.../repository/BalanceRepository.java` — пакет `ru.aigul.mts_service.balance.repository`
- добавить конфиг `src/main/java/.../config/BalanceDataSourceConfig.java` с:
  - бин`DataSource` (XA — `PGXADataSource` / `org.postgresql.xa.PGXADataSource` или использовать Spring's XA wrapper)
  - `LocalContainerEntityManagerFactoryBean` для `ru.aigul.mts_service.balance.model`
  - `@EnableJpaRepositories(basePackages = "ru.aigul.mts_service.balance.repository", entityManagerFactoryRef = "balanceEntityManager", transactionManagerRef = "transactionManager")`
- основной конфиг (если потребуется) `PrimaryDataSourceConfig.java` — убедиться, что основной EMF использует JTA datasource и помечен `@Primary`;
- `ApplicationService` — в методе `approve` обернуть операции списания баланса и изменения статуса заявки в одну `@Transactional` JTA-транзакцию; логика:
  - получить тариф/цены и суммарную цену
  - получить баланс через `BalanceRepository.findByUserId(userId)` (это second EMF/repo)
  - если хватает, уменьшить и сохранить баланс
  - затем пометить `Application` как APPROVED и сохранить
- `BalanceService` — добавить метод `applyTopUp(userEmail or userId, amount)` который в JTA-транзакции либо создаст запись баланса (если отсутствует), либо добавит к существующей и сохранит
- `docker-compose.yml` — добавить сервис `balance_db` (postgres:15) с отдельным volume и порт `5433:5432`; обновить `app` environment и добавить переменные `SPRING_DATASOURCE_BALANCE_URL`, `SPRING_DATASOURCE_BALANCE_USERNAME`, `SPRING_DATASOURCE_BALANCE_PASSWORD`

Замечания по FK и миграциям

Так как сейчас `balances` ссылается на `users` через foreign key, а `users` будет в другой БД, в реальной схеме cross-DB FK невозможен. Для минимального вмешательства есть два варианта:
- вариант A (рекомендую для простоты): убрать JPA-уровневую связь `@ManyToOne User user` в сущности `Balance` и заменить на поле `Long userId`. Так вы оставите ссылку семантически, но без DB FK. Это самый простой путь.
- вариант B: оставить `@ManyToOne` и в новой БД создать таблицу `users`-подмножество (seed) — но это сложнее и дублирует данные. Не рекомендуется.

Flyway: добавить отдельные миграции в `src/main/resources/db/migration/balance` и настроить Flyway для второго DataSource (или выполнять миграции вручную). Минимально: создать миграцию `V1__create_balances.sql` для второй БД, где `user_id` — просто bigint (без FK).

Примерный список конкретных правок (файлы)

- `build.gradle` — добавить Narayana (1 строка).
- `src/main/java/ru/aigul/mts_service/balance/model/Balance.java` — новая сущность (или обновлённый пакет, поле `Long userId`).
- `src/main/java/ru/aigul/mts_service/balance/repository/BalanceRepository.java` — пакет и интерфейс.
- `src/main/java/ru/aigul/mts_service/config/BalanceDataSourceConfig.java` — конфиг XA DataSource + EMF + EnableJpaRepositories для баланса.
- (опционально) `src/main/java/ru/aigul/mts_service/config/PrimaryXaConfig.java` — привести основной DataSource к JTA/XA EMF.
- `src/main/java/ru/aigul/mts_service/service/ApplicationService.java` — обновить метод `approve(...)`.
- `src/main/java/ru/aigul/mts_service/service/BalanceService.java` — добавить метод пополнения, и endpoint/callback для теста.
- `src/main/resources/application.properties` — добавить настройки для второй БД (названия переменных: `spring.datasource.balance.*` или `spring.datasource.balance.url`), конфигурация Narayana (при необходимости).
- `docker-compose.yml` — добавить `balance_db` и новые env-переменные для `app`.

Команды сборки и запуска (локально с докером)

1) Сборка проекта

```bash
./gradlew clean build -x test
```

2) Запуск контейнеров

```bash
docker-compose up --build
```

Тестовые сценарии (ручные cURL)

1) Подготовка: создать тестового пользователя (если не создано) через `POST /api/users`.

2) Пополнение баланса (тестовый callback)
- Реализовать временный endpoint, например `POST /payments/callback` который вызывает `BalanceService.applyTopUp(userId, amount)`.
- Вызывать:

```bash
curl -X POST -H "Content-Type: application/json" -d '{"userId":123,"amount":100.00}' http://localhost:8080/payments/callback
```

- Проверить `GET /account/balance` под учётом email пользователя — баланс должен увеличиться.

3) Создать заявку (или использовать существующую) и проверить поведение approve:
- Убедиться, что сумма заявки меньше или равна балансу.
- Вызвать approve (от имени менеджера):

```bash
curl -X POST -H "X-User-Id: <managerId>" http://localhost:8080/applications/<applicationId>/approve
```

- Ожидаемый результат: ответ `200` и статус заявки `APPROVED`, баланс уменьшился на сумму заявки.
- Если средств недостаточно — ожидается HTTP 4xx с ошибкой `InsufficientFundsException` и баланс и статус заявки остаются неизменными.

Тесты автоматические (рекомендация)

- Добавить один интеграционный тест, который запускает контекст Spring с embedded PostgreSQL (или Testcontainers), настраивает две БД, создаёт пользователя и баланс, создаёт заявку и проверяет:
  - successful approve decrements balance and sets status
  - approve with insufficient funds throws and не изменяет ничего
  - top-up increments balance

Что проверить после внедрения

- билд проходит
- приложение стартует и оба DataSource подключаются
- Flyway миграции для обеих БД (если включены) проходят
- распределённые транзакции работают: операции затрагивающие обе БД либо полностью коммитятся, либо откатываются

Порядок выполнения (пошагово)

1) Добавить зависимость Narayana и собрать проект, чтобы убедиться, что dependency разрешается.
2) Создать/переместить сущность `Balance` и `BalanceRepository` в отдельный пакет `balance` и заменить `User user` на `Long userId` (если хотите избежать cross-DB FK).
3) Добавить `BalanceDataSourceConfig.java` (XA DataSource, EMF, EnableJpaRepositories).
4) Привести основной EMF к JTA (если нужно) или создать Primary EMF, пометить `@Primary`.
5) Обновить `ApplicationService.approve(...)` и `BalanceService.applyTopUp(...)` в JTA-транзакции.
6) Обновить `docker-compose.yml`, добавить `balance_db` и передать параметры в `app`.
7) Добавить миграцию для второй БД (создание таблицы `balances` без FK) и настроить Flyway для второго DataSource либо выполнить DDL вручную.
8) Запустить `docker-compose up`, выполнить ручные тесты cURL.
9) Добавить интеграционные тесты (опционально).

Риски и упрощения

- cross-DB foreign keys невозможны: рекомендую заменить связь `@ManyToOne User user` на `Long userId`.
- если проект использует старый стиль конфигурации Spring Boot auto-configured datasource, потребуется немного больше правок; план рассчитан на минимальные ручные конфиги EMF для обеих БД.

Следующие шаги (если подтвердите):
- я могу реализовать минимальную рабочую версию: добавить конфигурационные классы, переместить сущность/репозиторий, изменить `ApplicationService` и `BalanceService`, добавить миграцию и правки `docker-compose.yml`, затем запустить сборку и проверку. Хотите, чтобы я выполнил эти правки сейчас?
