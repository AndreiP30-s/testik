# Схема базы данных

```mermaid
erDiagram
    brand ||--o{ product : brand_name
    category ||--o{ product : category_code
    category ||--o{ category_specification : category_code
    specification ||--o{ category_specification : specification_name
    product ||--o{ product_specification : product_article
    specification ||--o{ product_specification : specification_name
    product ||--o{ rear_camera : product_article
    app_user ||--o{ sales_order : user_email
    sales_order ||--o{ order_item : order_number
    product ||--o{ order_item : product_article
    product ||--o| discount_backup : product_article
```

| Таблица | Назначение |
|---|---|
| `brand` | Бренды |
| `category` | Категории товаров |
| `specification` | Определения характеристик, типы и единицы измерения |
| `category_specification` | Разрешённые характеристики категорий |
| `product` | Общие поля товаров |
| `product_specification` | Значения характеристик товаров |
| `rear_camera` | Типы, разрешение и количество модулей задних камер |
| `app_user` | Пользователи и роли |
| `sales_order` | Заказы |
| `order_item` | Позиции заказов |
| `discount_state` | Состояние скидки |
| `discount_backup` | Исходные цены товаров |

`discount_state` содержит одну строку с `id=1`. Она используется для блокировки
конкурирующих изменений каталога. Последовательность `product_article_seq`
выдаёт артикулы товаров.

## Составные ключи

- `product_specification`: `(product_article, specification_name)`;
- `category_specification`: `(category_code, specification_name)`;
- `rear_camera`: `(product_article, camera_type, resolution_mp)`;
- `order_item`: `(order_number, product_article)`.

Типы значений характеристик и их соответствие категории проверяются сервисом.
Таблицы `sales_order` и `order_item` не обслуживаются REST API.

## Инициализация

SQL находится в каталоге `db`. Скрипты выполняются в порядке имени файла при
создании PostgreSQL с пустым volume. Hibernate использует `ddl-auto=validate`
и не изменяет структуру таблиц.
