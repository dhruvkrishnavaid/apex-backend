# API reference

Base path `/api/v1`. Send `Authorization: Bearer <accessToken>`. Lists are paged
(`?page=0&size=20&sort=field,asc`) and return `{content, page: {...}}`. Errors look like
`{"status":409,"message":"...","details":null,"timestamp":"..."}`.

Status codes: 400 validation/malformed, 401 missing or invalid token, 403 missing permission, 404 not found,
409 conflict (duplicate, wrong state, still referenced), 422 business rule (e.g. insufficient stock).

## Auth and users
| Method | Path | Permission |
|--------|------|-----------|
| POST | `/auth/login`, `/auth/refresh`, `/auth/logout` | public |
| POST | `/auth/register` | `user:create` |
| GET | `/auth/me` | any |
| PATCH | `/auth/password` (own password) | any |
| GET | `/auth/users/{id}` | `user:read` or self |
| GET | `/users`, `/users/{id}` | `user:read` |
| PUT | `/users/{id}` | `user:update` |
| PATCH | `/users/{id}/password` (admin reset) | `user:update` |
| DELETE | `/users/{id}` | `user:delete` |
| GET | `/roles`, `/roles/{id}`, `/roles/permissions` | `role:read` |
| POST/PUT/DELETE | `/roles`, `/roles/{id}` | `role:manage` |

## Catalogue
| Method | Path | Permission |
|--------|------|-----------|
| GET | `/categories`, `/categories/{id}`, `/categories/slug/{slug}`, `/categories/{id}/subcategories` | `category:read` |
| POST / PUT / DELETE | `/categories`, `/categories/{id}` | `category:create` / `update` / `delete` |
| PATCH | `/categories/{id}/parent` | `category:update` |
| GET | `/products` (`categoryId`, `keyword`), `/products/{id}`, `/products/slug/{slug}` | `product:read` |
| POST / PUT / DELETE | `/products`, `/products/{id}` | `product:create` / `update` / `delete` |
| POST / GET | `/products/{id}/variants` | `product:update` / `read` |
| GET | `/products/variants/sku/{sku}` | `product:read` |
| PUT / DELETE | `/products/variants/{variantId}` (delete deactivates) | `product:update` / `delete` |
| POST / GET | `/products/{id}/media`; DELETE `/products/media/{mediaId}` | `product:update` / `read` |
| POST | `/products/variants/{variantId}/barcodes` | `product:update` |
| DELETE | `/products/barcodes/id/{barcodeId}` | `product:update` |
| GET | `/products/barcodes/{barcode}` (scanner lookup) | `product:read` |

## Warehouses and inventory
| Method | Path | Permission |
|--------|------|-----------|
| POST / GET / PUT / DELETE | `/warehouses`, `/warehouses/{id}` (delete deactivates; refused while stock remains) | `warehouse:*` |
| POST / GET | `/warehouses/{id}/locations`; PUT/DELETE `/warehouses/locations/{id}` | `warehouse:update` / `read` |
| GET | `/inventory` (`warehouseId`, `variantId`, `inStockOnly`), `/inventory/variants/{id}` | `inventory:read` |
| GET | `/inventory/movements` (`variantId`, `warehouseId`, `type`, `from`, `to`) | `inventory:read` |
| POST | `/inventory/adjustments` (`quantityChange`, `reason`, `damaged`) | `inventory:adjust` |
| POST | `/inventory/stock-counts` | `inventory:adjust` |
| PUT | `/inventory/variants/{v}/warehouses/{w}/location` | `inventory:adjust` |
| POST | `/transfers`, `/transfers/{id}/dispatch`, `/receive`, `/cancel` | `transfer:create` / `update` |
| GET | `/transfers`, `/transfers/{id}` | `transfer:read` |

## Procurement
| Method | Path | Permission |
|--------|------|-----------|
| POST / GET / PUT / DELETE | `/suppliers`, `/suppliers/{id}` | `supplier:*` |
| POST / GET / PUT / DELETE | `/suppliers/{id}/products[/{variantId}]`; GET `/suppliers/by-variant/{variantId}` | `supplier:update` / `read` |
| POST | `/purchase-orders` | `purchase:create` |
| GET | `/purchase-orders`, `/{id}`, `/{id}/receipts` | `purchase:read` |
| PUT | `/purchase-orders/{id}` (DRAFT only), POST `/{id}/submit`, `/{id}/close` | `purchase:update` |
| POST | `/purchase-orders/{id}/receive` | `purchase:receive` |
| POST | `/purchase-orders/{id}/cancel` | `purchase:cancel` |

## Sales and fulfilment
| Method | Path | Permission |
|--------|------|-----------|
| CRUD | `/customers` | `customer:*` |
| POST | `/orders` (reserves stock) | `order:create` |
| GET | `/orders` (`status`, `customerId`, `warehouseId`, `orderNumber`, `from`, `to`), `/orders/{id}` | `order:read` |
| POST | `/orders/{id}/pick`, `/pack`, `/ship`, `/deliver` | `order:update` |
| POST | `/orders/{id}/cancel` | `order:cancel` |
| POST / GET | `/orders/{id}/returns` | `order:update` / `read` |

## Alerts, reports, audit
| Method | Path | Permission |
|--------|------|-----------|
| GET | `/alerts`, `/alerts/summary`, `/reorder-rules`, `/reorder-rules/suggestions` | `alert:read` |
| POST / PUT / DELETE | `/reorder-rules[/{id}]`, POST `/alerts/{id}/acknowledge` | `alert:manage` |
| GET | `/dashboard`, `/reports/stock-valuation`, `/reports/low-stock`, `/reports/sales`, `/reports/top-products` | `report:read` |
| GET | `/audit-logs` (`userId`, `method`, `resource`, `from`, `to`) | `audit:read` |
