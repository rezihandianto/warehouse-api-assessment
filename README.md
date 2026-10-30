# Warehouse API

REST API for a shop's warehouse: items, variants, pricing, stock tracking, and sales that
are rejected when stock is insufficient.

Java 17+ · Spring Boot 3.5 · Spring Data JPA · H2 (default) / PostgreSQL (Docker) · springdoc Swagger

## How to run

Requirements: JDK 17+ and Maven 3.9+ (or generate a wrapper with `mvn wrapper:wrapper`).

```bash
# Option A: in-memory H2 (zero setup)
mvn spring-boot:run

# Option B: PostgreSQL via Docker
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres

# Tests
mvn test
```

The app starts on http://localhost:8080 and seeds sample data on an empty database
(disable with `--app.seed-data=false`).

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

## Configuration

All settings live in `src/main/resources/application.properties` (H2 by default).
`application-postgres.properties` is loaded only with the `postgres` profile and can be
overridden with `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

## Project structure

```
config/       OpenAPI config, sample data loader
controller/   REST endpoints (thin: validation + response wrapping)
service/      Business rules and transactions
repository/   Spring Data JPA repositories (incl. atomic stock updates)
entity/       JPA entities
dto/request/  Validated request bodies
dto/response/ Response bodies and the BaseResponse envelope
exception/    ApiException and the global exception handler
```

## Response format

Every endpoint returns the same envelope. HTTP status codes are still meaningful
(201, 400, 404, 409, 500).

```json
{
  "responseStatus": "success",
  "responseMessage": "Item retrieved",
  "data": { "id": 1, "sku": "TEE-001", "stockQuantity": 65, "inStock": true }
}
```

Failures use `"responseStatus": "failed"`. For validation errors, `data` holds the
per-field messages; otherwise it is `null`.

```json
{ "responseStatus": "failed", "responseMessage": "Insufficient stock for CAP-001 (requested 999)", "data": null }
```

```json
{
  "responseStatus": "failed",
  "responseMessage": "Validation failed",
  "data": { "sku": "must not be blank", "price": "must be greater than or equal to 0.00" }
}
```

Lists are paginated: `data` is `{ content, page, size, totalElements, totalPages }`.

## Domain model

- **Item**: sku (unique), name, description, price, stock.
- **ItemVariant**: belongs to an item; own sku, name, attributes (size, color...),
  optional price override (null = inherit item price), own stock.
- **Sale / SaleLine**: a purchase; lines snapshot sku, name and unit price.

An item without variants holds its own stock. An item with variants holds stock only on its
variants; the item's `stockQuantity` in responses is the sum of its variants.

## Design decisions

| Decision | Why |
|---|---|
| Atomic guarded `UPDATE` for stock (`... WHERE stock + :delta >= 0`) | No read-modify-write race; overselling is impossible under concurrency. 0 rows updated means insufficient stock. |
| Sales are all-or-nothing (one transaction) | Any failing line rolls back earlier decrements. |
| Duplicate lines merged, lines sorted by id | Correct stock check; consistent lock order avoids deadlocks. |
| Stock changes only via `stock-adjustments` and sales | Stock can't be silently overwritten by a PUT. |
| `BigDecimal` for money | No floating point errors. |
| Sale lines store plain ids and snapshot name/price | History survives catalog edits and deletes. |
| `BaseResponse` envelope plus real HTTP status codes | Uniform body for clients; status codes stay usable for monitoring and retries. |
| Package-by-layer, records as DTOs, `open-in-view=false` | Familiar structure; stable contract; no lazy-loading surprises. |
| `ddl-auto=update` | Fastest for an assessment; Flyway would be used in production. |

## Assumptions

- Single warehouse, single currency, no authentication.
- "Selling" is `POST /api/sales`; no payment or customer.
- Deleting an item deletes its variants; past sales are kept.
- Adding a variant to an item that still holds its own stock is rejected (409).
- `DELETE` returns 200 with the envelope (a 204 cannot carry a body).

## API

| Method | Path | Notes |
|---|---|---|
| POST | `/api/items` | optional `initialStock`; 201 |
| GET | `/api/items?page=0&size=20&sort=name,asc` | paginated, includes variants |
| GET / PUT / DELETE | `/api/items/{id}` | |
| POST | `/api/items/{id}/stock-adjustments` | `{"delta": 10}` or negative |
| POST / GET | `/api/items/{itemId}/variants` | |
| GET / PUT / DELETE | `/api/items/{itemId}/variants/{variantId}` | |
| POST | `/api/items/{itemId}/variants/{variantId}/stock-adjustments` | |
| POST | `/api/sales` | 409 if any line lacks stock |
| GET | `/api/sales/{id}` | |

Status codes: 201 created, 400 validation, 404 not found, 409 duplicate SKU / insufficient stock.

### Examples

```bash
curl -X POST localhost:8080/api/items -H 'Content-Type: application/json' \
  -d '{"sku":"BAG-001","name":"Tote Bag","price":11.50,"initialStock":20}'

curl -X POST localhost:8080/api/items/1/variants -H 'Content-Type: application/json' \
  -d '{"sku":"TEE-001-XL-BLK","name":"XL / Black","attributes":{"size":"XL","color":"black"},"initialStock":10}'

curl -X POST localhost:8080/api/items/4/stock-adjustments -H 'Content-Type: application/json' \
  -d '{"delta":50}'

# item 1 has variants (variantId required); item 4 does not
curl -X POST localhost:8080/api/sales -H 'Content-Type: application/json' \
  -d '{"lines":[{"itemId":1,"variantId":2,"quantity":2},{"itemId":4,"quantity":3}]}'

# Selling more than available -> 409 with responseStatus "failed"
curl -i -X POST localhost:8080/api/sales -H 'Content-Type: application/json' \
  -d '{"lines":[{"itemId":6,"quantity":999}]}'
```