# Apex Backend: Retail Inventory Management System

Spring Boot 4 / Java 21 REST API for a retail inventory platform: product catalogue, multi-warehouse stock,
purchasing, order fulfilment, alerts and reporting. The frontend lives in a separate project and talks to this
API over `/api/v1` using JWT bearer tokens.

Delivery followed Scrum: 8 epics over 15 sprints (see [docs/AGILE_BACKLOG.md](docs/AGILE_BACKLOG.md)). The full
endpoint list is in [docs/API.md](docs/API.md).

## Running it

```bash
cp .env.example .env      # then edit: database, JWT secret (32+ chars), first admin
./mvnw spring-boot:run
```

On start-up the app seeds the built-in roles and, if `ADMIN_EMAIL` / `ADMIN_PASSWORD` are set, the first admin
user. Schema is created and updated by Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

| Variable | Purpose |
|----------|---------|
| `DB_TYPE`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` | HS256 signing key, at least 32 characters |
| `JWT_TTL_SECONDS` | Access token lifetime (default 3600) |
| `CORS_ALLOWED_ORIGINS` | Comma separated frontend origins |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Optional bootstrap admin |
| `SALES_TAX_RATE` | Tax applied to new orders, e.g. `0.10` (default 0) |

## Tests

```bash
./mvnw test
```

Integration tests run against an in-memory H2 database (`src/test/resources/application.properties`); they never
touch the configured PostgreSQL database.

## Architecture

Layered: `controller` (REST + `@PreAuthorize`) -> `service` (business rules, transactions) -> `repository`
(Spring Data JPA) -> `model` (entities). DTOs live in `dto/request` and `dto/response`; `GlobalExceptionHandler`
maps domain exceptions to a uniform `{status, message, details, timestamp}` error body.

### Security
- Stateless JWT (HS256). The token carries the user's role permissions as authorities.
- Every endpoint except login, refresh, logout and health requires a token and a specific permission, for
  example `product:update`. Registering users needs `user:create`.
- Refresh tokens are opaque, stored hashed and rotated on use. Changing roles, deactivating a user or
  changing a password revokes their refresh tokens (an access token stays valid until it expires).
- Built-in roles: `ADMIN`, `MANAGER`, `WAREHOUSE_STAFF`, `CASHIER`. Custom roles can be created through the roles API
  using the permission catalogue in `com.apex.security.Permissions`.

### Inventory integrity
- `InventoryService` is the only code that changes stock. Every change of quantity on hand writes an immutable
  `stock_movements` ledger row.
- Rows are locked pessimistically per (variant, warehouse), so concurrent orders cannot oversell.
- Stock has `onHand`, `reserved` and `available = onHand - reserved`. Orders and outgoing transfers reserve stock;
  it leaves the warehouse on shipment or dispatch.

### Document flows

| Document | Flow |
|----------|------|
| Stock transfer | REQUESTED -> IN_TRANSIT -> RECEIVED (or CANCELLED before dispatch) |
| Purchase order | DRAFT -> ORDERED -> PARTIALLY_RECEIVED -> RECEIVED (or CANCELLED / CLOSED) |
| Sales order | PENDING -> PICKING -> PACKED -> SHIPPED -> DELIVERED (or CANCELLED before shipping) |
| Return | Created against shipped/delivered orders; restocks or writes off |

### Alerts and audit
- Reorder rules per (variant, warehouse) raise `LOW_STOCK` / `OUT_OF_STOCK` alerts when available stock falls to the
  reorder point, and resolve them automatically when stock recovers.
- Every successful POST/PUT/PATCH/DELETE by an authenticated user is written to `audit_logs`.

## Licence

MIT, see [LICENSE](LICENSE).
