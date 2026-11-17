.DEFAULT_GOAL := help

MVNW := ./mvnw
MAVEN_FLAGS := --batch-mode --no-transfer-progress

.PHONY: help build test run up down logs status clean

help:
	@echo "make build  — собрать JAR и запустить тесты"
	@echo "make test   — запустить тесты без PostgreSQL"
	@echo "make run    — поднять базу и запустить сервер (остановка: Ctrl+C)"
	@echo "make up     — поднять только PostgreSQL"
	@echo "make down   — остановить PostgreSQL, сохранив данные"
	@echo "make logs   — показать журнал PostgreSQL"
	@echo "make status — показать состояние PostgreSQL"
	@echo "make clean  — убрать результаты сборки Maven"

build:
	$(MVNW) $(MAVEN_FLAGS) clean verify

test:
	$(MVNW) $(MAVEN_FLAGS) test

run:
	@set -eu; \
	if [ -f .env ]; then set -a; . ./.env; set +a; fi; \
	export DB_URL="$${DB_URL:-jdbc:postgresql://localhost:$${DB_PORT:-5434}/techstore}"; \
	export DB_USER="$${DB_USER:-techstore}"; \
	export DB_PASSWORD="$${DB_PASSWORD:-techstore_local}"; \
	export PORT="$${PORT:-8081}"; \
	docker compose up -d --wait; \
	exec $(MVNW) $(MAVEN_FLAGS) spring-boot:run

up:
	docker compose up -d --wait

down:
	docker compose stop

logs:
	docker compose logs --tail=100 db

status:
	docker compose ps -a

clean:
	$(MVNW) $(MAVEN_FLAGS) clean
