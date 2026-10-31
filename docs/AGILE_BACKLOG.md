# Apex Retail Inventory Management: Agile Backlog

Backend only (Spring Boot 4 / Java 21 / PostgreSQL). The frontend lives in a separate project.
Delivered as 8 epics over 15 sprints; each sprint is committed to git as `Sprint N: ...`.

## Epics

| # | Epic | Scope |
|---|------|-------|
| E1 | Product Catalog | Categories, products, variants, barcodes, media |
| E2 | Identity & Access | Auth, JWT, refresh tokens, RBAC, user and role management |
| E3 | Warehouse & Inventory | Multi-warehouse stock levels, adjustments, movement ledger |
| E4 | Stock Transfers | Inter-warehouse transfers, reservations |
| E5 | Supplier & Procurement | Suppliers, purchase orders, goods receipt |
| E6 | Order Fulfilment | Customers, sales orders, pick/pack/ship, cancellations, returns |
| E7 | Replenishment & Alerts | Reorder rules, low-stock alerts, notifications |
| E8 | Reporting & Audit | Stock valuation, sales and movement reports, dashboard, audit trail |

## Sprints

| Sprint | Epic | Goal | Status |
|--------|------|------|--------|
| 1 | E2 | Project setup, configuration, PostgreSQL, health check | Done |
| 2 | E1, E2 | Auth (JWT + refresh), roles, categories, products, variants, barcodes | Done |
| 3 | E1 | Catalog refinement, exception handling | Done |
| 4 | E1, E2 | **Stabilisation**: security config fix, RBAC enforcement, error handling, CORS, test infra | Done |
| 5 | E1 | Catalog completion: update product/variant, media, barcode removal, category move | Done |
| 6 | E2 | User and role management, password change | Done |
| 7 | E3 | Warehouses and storage locations | Done |
| 8 | E3 | Stock levels, adjustments, movement ledger | Done |
| 9 | E4 | Stock transfers and reservations | Done |
| 10 | E5 | Suppliers and supplier catalog | Done |
| 11 | E5 | Purchase orders and goods receipt | Done |
| 12 | E6 | Customers and sales orders with stock reservation | Planned |
| 13 | E6 | Fulfilment (pick/pack/ship), cancellation, returns | Planned |
| 14 | E7 | Reorder rules, low-stock alerts, notifications | Planned |
| 15 | E8 | Reports, dashboard, audit log, final documentation | Planned |

## Sprint 4 report (stabilisation)

Defects found in the existing build and fixed:

- `SecurityConfig` was an inner class of `WebConfig`, so the security chain was not registered reliably. Now a top-level class.
- No endpoint enforced permissions. All controllers now use `@PreAuthorize` with the permission claims.
- `POST /auth/register` was public and accepted any `roleId` (anyone could create an ADMIN). It now requires `user:create`; a first admin is seeded from `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
- `GET /auth/users/{id}` was readable by any user; now `user:read` or the user themselves. Added `GET /auth/me`.
- Refresh tokens were accepted for deactivated accounts; emails are now case-insensitive.
- `RefreshTokenRepository.deleteAllExpiredOrRevoked` compared a `LocalDateTime` column with an `Instant`.
- Product creation failed (500) when `description` was omitted. Soft-deleted products appeared in unfiltered listings and their slugs collided with the unique constraint.
- The category/keyword filters on product listing were mutually exclusive; they now combine.
- Deleting a category that has products raised a 500 / FK error; now a 409.
- Global handler leaked exception messages as 500s and turned 404/405/400 framework errors into 500s.
- Primary barcode handling allowed several primary barcodes per variant.
- CORS was missing for the separate frontend. The JWT secret length is validated on start-up and the issuer is verified.
- Seed data: CASHIER and WAREHOUSE_STAFF lacked `category:read`.
- Tests ran only against the remote Supabase database. They now use in-memory H2.
