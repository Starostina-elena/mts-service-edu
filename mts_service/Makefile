COMPOSE = docker compose -f docker-compose.yml --env-file .env

.PHONY: up migrate down logs ps build

up:
	./gradlew bootJar -x test
	$(COMPOSE) up --build -d

migrate:
	$(COMPOSE) run --rm flyway migrate

down:
	$(COMPOSE) down -v

logs:
	$(COMPOSE) logs -f

ps:
	$(COMPOSE) ps

build:
	$(COMPOSE) build app
