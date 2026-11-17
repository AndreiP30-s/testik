# Tech Store — ЛР2, PostgreSQL в Docker

REST-сервис магазина техники на Java 17, Spring Boot, Maven и Spring Data JPA.
Упрощённая версия проекта: один набор JPA-моделей, без отдельных моделей хранения,
мапперов и консольного приложения.

## Команды Makefile

В терминале из корня проекта:

| Команда | Действие |
|---|---|
| `make` | Список команд |
| `make build` | Чистая сборка JAR с проверками |
| `make test` | Тесты без PostgreSQL |
| `make run` | Запустить Docker-базу и REST-сервер |
| `make up` | Запустить только базу |
| `make down` | Остановить базу, сохранив данные |
| `make logs` | Журнал PostgreSQL |
| `make status` | Состояние базы |
| `make clean` | Очистить сборку |

Docker Desktop должен быть запущен для команд работы с базой и `make run`.
`make build` и `make test` работают без базы. Сервер при `make run` работает в
текущем терминале: сначала остановить его **Ctrl+C**, затем выполнить `make down`.
`make down` останавливает только PostgreSQL и не удаляет volume.
Команды `./mvnw` и `./scripts/run.sh` также остаются доступными без Make.

## Быстрый запуск

Нужны JDK 17+, Docker Desktop и IntelliJ IDEA. PostgreSQL отдельно устанавливать
не требуется. Maven Wrapper уже включён.

1. Запустить Docker Desktop.
2. Открыть **эту папку** в IntelliJ как Maven-проект.
3. В терминале проекта выполнить:

```sh
docker compose up -d --wait
```

4. В IntelliJ выбрать конфигурацию **TechStore Docker** и нажать Run.

Альтернативный запуск из терминала, одной командой:

```sh
./scripts/run.sh
```

Скрипт поднимает базу, ждёт готовности и запускает Spring Boot через Maven Wrapper.
При первом запуске нужны интернет для образа PostgreSQL и Maven-зависимостей.

Проверить сервер:

- http://localhost:8081/api/categories
- http://localhost:8081/api/products

Это REST-сервер: ответы в JSON, консольного меню и HTML-магазина нет.
Начальный каталог пустой, справочники уже заполнены. Создание смартфона:

```sh
curl -i -X POST http://localhost:8081/api/products \
  -H 'Content-Type: application/json' \
  --data-binary @examples/smartphone.json
```

В `examples/requests.http` есть запросы для IntelliJ HTTP Client. JSON-файлы
смартфона, планшета, ноутбука, наушников и пользователя можно использовать в Postman.
Артикул берётся из ответа POST, а не предполагается равным фиксированному числу.

## Swagger UI — простые формы без JSON

После запуска открыть **http://localhost:8081/swagger-ui.html** и обновить страницу,
если она была открыта до обновления приложения. Дизайн Swagger остался стандартным.

**Добавить товар:**

1. Открыть первую группу **«1. Товары»**.
2. Выбрать **«Добавить смартфон — заполнить поля»** (либо планшет, ноутбук, наушники).
3. Нажать **Try it out**.
4. Заполнить название (`name`) и цену (`price`). Проверить бренд и характеристики:
   они уже имеют начальные значения, которые можно изменить.
5. Нажать **Execute**. Код **201** означает, что товар добавлен. Артикул — в ответе.

Новый бренд в этой форме создаётся автоматически. Сначала создавать его отдельным
запросом не требуется. Категория выбирается самой операцией. Форма смартфона/планшета
создаёт одну основную заднюю камеру; дополнительные группы камер доступны через JSON.

**Посмотреть товары:** GET `/api/products` → Try it out → Execute. Фильтры можно
оставить пустыми. `page=0`, `size=20` менять необязательно.

**Изменить цену:** PATCH `/api/products/{article}/details` → Try it out → ввести
артикул и новую цену → Execute. Пустые поля оставляют прежние значения. Характеристики
и камеры сохраняются. Здесь также меняются название, бренд и доступность.

**Добавить пользователя:** «2. Пользователи» → POST `/api/users/form` → Try it out →
заполнить email, имя и пароль → Execute. По умолчанию роль CUSTOMER. Пароль отправляется
в теле формы, а не в строке URL, хешируется BCrypt и не возвращается в ответе.
PUT `/api/users/{email}/form` изменяет имя и роль обычными полями.

Группы «3. Справочники» и «4. Скидки» содержат дополнительные операции.
Полные JSON-запросы создания/обновления находятся в последней группе
**«5. Расширенные JSON-запросы»**; прежние маршруты работают как раньше.

Swagger отправляет настоящие запросы: POST/PUT/PATCH/DELETE изменяют базу.
Спецификация OpenAPI: http://localhost:8081/v3/api-docs.
Используется springdoc-openapi 3.2.0 для Spring Boot 4.1.

## База в Docker

Compose запускает только PostgreSQL; Java-приложение запускается в IntelliJ или
через Maven. Это позволяет отлаживать его обычным способом.

| Параметр | Значение по умолчанию |
|---|---|
| PostgreSQL на компьютере | `localhost:5434` |
| База | `techstore` |
| Пользователь | `techstore` |
| Пароль для локальной разработки | `techstore_local` |
| REST API | `localhost:8081` |

Это отдельная база и отдельный Docker volume `postgres_data` в Compose-проекте
`techstore-lab2-docker`. Существующий PostgreSQL на 5432 и предыдущий проект на
8080 не используются. Данные прежней версии автоматически не переносятся.

Справочники и таблицы создаются автоматически скриптами из `db` при первом запуске
с пустым volume:

1. `01-schema.sql` — исходная схема предмета по БД;
2. `02-java-support.sql` — последовательность артикулов и хранение скидки;
3. `03-reference-data.sql` — категории, характеристики, связи и `DemoBrand`.

SQL монтируется в контейнер только для чтения. При повторном запуске с тем же volume
скрипты заново не выполняются, сохранённые товары остаются.

```sh
docker compose ps          # состояние и порт
docker compose logs db     # журнал PostgreSQL
docker compose stop        # остановить базу с сохранением данных
docker compose up -d --wait # снова запустить
```

Обычный `docker compose down` тоже сохраняет named volume. Флаг `-v` удаляет
volume и данные; для обычного завершения работы он не нужен.

Для просмотра в pgAdmin зарегистрировать сервер: host `localhost`, port `5434`,
maintenance database `techstore`, user `techstore`, password `techstore_local`.
Встроенный pgAdmin в Compose не добавлен: можно использовать уже установленный.

Необязательные изменения настроек — через `.env` по образцу `.env.example`.
`./scripts/run.sh` читает этот файл и передаёт значения обоим процессам.
При запуске из IntelliJ изменённые DB_PASSWORD/PORT нужно также задать в Environment
variables; IntelliJ-конфигурация сама `.env` не читает. DB_URL можно задать явно,
например `jdbc:postgresql://localhost:5435/techstore` при изменении порта контейнера.
Изменение пароля в `.env` не меняет пароль уже созданной базы в существующем volume.

## Структура проекта

Все пакеты находятся в `com.example.techstore`.

- `model.product` — единственная модель товара `Product`, значения характеристик,
  задние камеры, ключи и перечисления;
- `model.catalog` — бренд, категория, определение характеристики, разрешённые связи;
- `model.user` — абстрактный `User`, наследники `AdminUser`, `ManagerUser`,
  `CustomerUser` и enum `UserRole`;
- `model.discount` — состояние скидки и сохранённые исходные цены;
- `repository` — интерфейсы Spring Data JPA и запрос фильтрации;
- `service` — интерфейсы ProductService, UserService, ReferenceService и DiscountService,
  а также вспомогательные проверки и расчёты;
- `service.impl` — реализации основных сервисов: ProductServiceImpl, UserServiceImpl,
  ReferenceServiceImpl и DiscountServiceImpl;
- `controller` — HTTP-запросы;
- `dto` — входной и выходной JSON;
- `exception` — обработка ошибок;
- `config` — BCrypt PasswordEncoder.

Путь запроса: Controller → Service (интерфейс) → ServiceImpl → Repository → PostgreSQL.
Контроллер получает интерфейс через конструктор; Spring подставляет реализацию.
Интерфейс перечисляет доступные операции, реализация содержит прежнюю бизнес-логику.
JPA-модели напрямую в ответы не возвращаются: DTO исключают хеши паролей и
предотвращают рекурсивную сериализацию связей.

### Товар без дублирования моделей

Отдельных `Smartphone`, `Laptop`, `Tablet`, `Headphones` больше нет.
Есть один `Product`, связанный с категорией. Все характеристики находятся в
`product_specification`, как в твоей SQL-схеме.

Например, RAM смартфона — запись с именем `ramGb` и значением `"8"`.
`ProductRules` проверяет обязательные поля, положительные числа, перечисления,
частоты и камеры. `CharacteristicValidator` проверяет разрешение характеристики
для категории и её тип по справочнику.

| Категория | Обязательные характеристики |
|---|---|
| Смартфон | processor, ramGb, storageGb, screenDiagonalInches, frontCameraMp, batteryCapacityMah, supports5g |
| Планшет | processor, ramGb, storageGb, screenDiagonalInches, frontCameraMp, batteryCapacityMah, cellular |
| Ноутбук | processor, ramGb, storageGb, screenDiagonalInches, frontCameraMp, storageType |
| Наушники | headphoneType, wireless, noiseCancelling, minFrequencyHz, maxFrequencyHz |

Значения характеристик — строки. Булевы значения: `"true"` и `"false"`.
StorageType: SSD/HDD; HeadphoneType: IN_EAR/ON_EAR/OVER_EAR.
Задние камеры нужны смартфонам и планшетам, а ноутбукам и наушникам не назначаются.
Типы камер: WIDE/ULTRA_WIDE/TELEPHOTO/MACRO. Одинаковые модули объединяются через
quantity, не более 32 модулей на товар. Дополнительные характеристики разрешены
после создания определения и привязки к категории.

### Наследование пользователей

`User` — абстрактная JPA-сущность. Наследники хранятся в одной таблице `app_user`,
а существующий столбец `role` определяет Java-класс (SINGLE_TABLE).
Новых таблиц пользователей, UUID и преобразований UserEntity → User не требуется.
Ключ пользователя — email, как в исходном SQL.

Смена роли меняет конкретный тип сущности. Для этого репозиторий выполняет явный
UPDATE столбца role с очисткой persistence context, чтобы следующий запрос загрузил
правильный подкласс. Email неизменяемый. Пароли хешируются BCrypt и не возвращаются.
Проверка прав HTTP-клиента и вход в систему в эту лабораторную не входят.

## REST API

| Метод | Маршрут | Назначение |
|---|---|---|
| POST | `/api/products/smartphones`, `/tablets`, `/laptops`, `/headphones` | Добавление через поля формы |
| PATCH | `/api/products/{article}/details` | Изменить только цену, название, бренд или доступность |
| POST | `/api/users/form` | Добавить пользователя через поля формы |
| PUT | `/api/users/{email}/form` | Изменить пользователя через поля формы |
| GET / POST | `/api/products` | Список с фильтрами / создание через JSON |
| GET / PUT / DELETE | `/api/products/{article}` | Чтение / обновление / удаление |
| GET / POST | `/api/users` | Список с необязательной ролью / создание |
| GET / PUT / DELETE | `/api/users/{email}` | Чтение / имя и роль / удаление |
| GET / POST | `/api/brands` | Список / создание |
| PUT / DELETE | `/api/brands/{name}` | Переименование / удаление неиспользуемого бренда |
| GET | `/api/categories` | Четыре фиксированные категории SQL-схемы |
| GET / POST | `/api/specifications` | Список / создание определения характеристики |
| GET / PUT / DELETE | `/api/specifications/{name}` | Чтение / обновление / удаление |
| GET | `/api/categories/{code}/specifications` | Разрешённые характеристики |
| PUT / DELETE | `/api/categories/{code}/specifications/{name}` | Назначение / снятие связи |
| GET / POST / DELETE | `/api/discount` | Статус / применение / отмена |

Фильтры товаров: name, brand, category, minPrice, maxPrice, available.
Их можно сочетать. Пагинация: page с 0, size от 1 до 100. Сортировка по артикулу.

```text
GET /api/products?category=SMARTPHONE&brand=DemoBrand&minPrice=10000&maxPrice=50000&page=0&size=10
```

PUT товара полностью заменяет его поля, характеристики и камеры в одной транзакции.
Удаление товара удаляет зависимые характеристики и камеры. Если товар используется
заказом, операция откатывается с 409; можно изменить available на false.
Таблицы заказов оставлены в общей схеме, REST API заказов пока не реализован.

Коды ответов: 201 при создании, 204 при удалении, 400 при неверных данных,
404 при отсутствии объекта, 409 при конфликте уникальности или связей.

## Многопоточность и скидки

Расчёт использует ExecutorService с потоками `price-worker-N`.
Рабочие потоки получают неизменяемые `PriceSnapshot` (артикул, имя, цена),
а не JPA-сущности. Исходные цены сохраняются в discount_backup, состояние —
в discount_state. Отмена работает после перезапуска приложения.
Пока скидка активна, изменения каталога заблокированы до её отмены.
Блокировка строки состояния в PostgreSQL защищает от повторного одновременного
применения. Скидка, округляющая цену до нуля, отклоняется.

## Тесты и CI

```sh
./mvnw clean verify
```

Docker и PostgreSQL для тестов не нужны. В тестовом профиле отключены JDBC и JPA,
репозитории заменены @MockitoBean. REST проверяется через @SpringBootTest и MockMvc.
Отдельно проверяются правила всех категорий, камеры и параллельный расчёт цен.
OpenApiTest проверяет маршруты документации, пример товара, код 201 и отсутствие
пароля в схеме ответа пользователя. Всего в актуальной проверке прошли 58 тестов, включая новые формы и частичное обновление.
Тесты старой консоли и удалённого преобразования моделей не перенесены, поскольку
такого кода в этой версии больше нет. Проверки действующего REST-функционала сохранены.

GitHub Actions и `.gitlab-ci.yml` выполняют Maven verify на Java 17.
Удалённый pipeline требует публикации проекта; Git-операции здесь не выполнялись.
Подробности локальной проверки — в `docs/verification.md`.

## Документация Docker

- [Официальный образ PostgreSQL: инициализация и хранение данных](https://github.com/docker-library/docs/blob/master/postgres/README.md)
- [Готовность сервисов и healthcheck](https://docs.docker.com/compose/how-tos/startup-order/)

- [Документация springdoc и совместимость со Spring Boot](https://springdoc.org/)

## Соответствие лабораторным

Разбор каждого пункта и оставшиеся действия перед сдачей — в
[docs/lab-requirements.md](docs/lab-requirements.md).
