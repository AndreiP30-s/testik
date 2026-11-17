# Схема хранения

Основные таблицы и ключи оставлены из предоставленного SQL.

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
    sales_order ||--|{ order_item : order_number
    product ||--o{ order_item : product_article
    product ||--o| discount_backup : product_article
```

`discount_state` — одна строка (`id=1`), хранит флаг скидки и используется для
блокировки конкурирующих изменений каталога.
`product_article_seq` — последовательность выдачи артикулов.

`product_specification` имеет составной PK `(product_article, specification_name)`.
`category_specification`: `(category_code, specification_name)`.
`rear_camera`: `(product_article, camera_type, resolution_mp)`.
`order_item`: `(order_number, product_article)`.

Картинка показывает предметные связи. Исходные внешние ключи не гарантируют наличие
хотя бы одной позиции в каждом заказе; заказы пока не обслуживаются REST API.
Типы значений характеристик и их принадлежность категории проверяются Java-сервисом.
Прямые SQL-записи должны соблюдать те же правила самостоятельно.
