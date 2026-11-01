# Apex Retail Inventory Management: Complete Source Code

Single-file snapshot of the whole project: Spring Boot backend (145 files) and React frontend (88 files).

Left out: lock files, images/fonts, `.mvn`/`mvnw` wrapper scripts, the generated `routeTree.gen.ts`, and `.env`
files with real secrets (only `.env.example` is included).

# Part 1: Backend (apex-backend)

## `.env.example`

```
DB_TYPE=postgresql
DB_HOST=localhost
DB_PORT=5432
DB_NAME=apex
DB_USER=postgres
DB_PASSWORD=change-me
# HS256 key: at least 32 characters
JWT_SECRET=change-me-to-a-long-random-string-of-32+chars
JWT_TTL_SECONDS=3600
# Optional: comma separated list of frontend origins allowed by CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
# Optional: first admin user, created on startup when the email is not yet registered
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-me-too
```

## `.gitignore`

```
HELP.md
target/
.env
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/

.DS_Store
**/.DS_Store
```

## `README.md`

````markdown
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
````

## `docs/AGILE_BACKLOG.md`

```markdown
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
| 12 | E6 | Customers and sales orders with stock reservation | Done |
| 13 | E6 | Fulfilment (pick/pack/ship), cancellation, returns | Done |
| 14 | E7 | Reorder rules, low-stock alerts, notifications | Done |
| 15 | E8 | Reports, dashboard, audit log, final documentation | Done |

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
```

## `docs/API.md`

```markdown
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
```

## `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath /> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.apex</groupId>
	<artifactId>app</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>app</name>
	<description />
	<url />
	<licenses>
		<license>
			<name>MIT License</name>
			<url>https://opensource.org/licenses/MIT</url>
		</license>
	</licenses>
	<developers>
		<developer />
	</developers>
	<scm>
		<connection />
		<developerConnection />
		<tag />
		<url />
	</scm>
	<properties>
		<java.version>21</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-configuration-processor</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<executions>
					<execution>
						<id>default-compile</id>
						<phase>compile</phase>
						<goals>
							<goal>compile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
					<execution>
						<id>default-testCompile</id>
						<phase>test-compile</phase>
						<goals>
							<goal>testCompile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
```

## `src/main/java/com/apex/Application.java`

```java
package com.apex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
```

## `src/main/java/com/apex/config/AuditInterceptor.java`

```java
package com.apex.config;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.apex.model.AuditLog;
import com.apex.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Writes an audit entry for every successful POST/PUT/PATCH/DELETE made by an authenticated user. */
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);
  private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");
  private static final String PREFIX = "/api/v1/";

  private final AuditLogRepository auditLogRepository;

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {
      if (!MUTATING.contains(request.getMethod()) || response.getStatus() >= 400
          || !request.getRequestURI().startsWith(PREFIX)) {
        return;
      }
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
        return;
      }
      Jwt jwt = jwtAuth.getToken();
      String path = request.getRequestURI();
      String rest = path.substring(PREFIX.length());
      int slash = rest.indexOf('/');
      auditLogRepository.save(AuditLog.builder()
          .userId(parse(jwt.getSubject()))
          .userEmail(jwt.getClaimAsString("email"))
          .method(request.getMethod())
          .path(path.length() > 500 ? path.substring(0, 500) : path)
          .resource(slash < 0 ? rest : rest.substring(0, slash))
          .statusCode(response.getStatus())
          .ipAddress(request.getRemoteAddr())
          .build());
    } catch (Exception e) {
      // auditing must never break the request that already succeeded
      log.error("Failed to write audit log", e);
    }
  }

  private UUID parse(String value) {
    try {
      return UUID.fromString(value);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
```

## `src/main/java/com/apex/config/DataSeeder.java`

```java
package com.apex.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.apex.model.Role;
import com.apex.model.User;
import com.apex.repository.RoleRepository;
import com.apex.security.Permissions;
import com.apex.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.bootstrap.admin.email:}")
  private String adminEmail;

  @Value("${app.bootstrap.admin.password:}")
  private String adminPassword;

  @Override
  @Transactional
  public void run(String... args) {
    seedRole("ADMIN", Permissions.admin());
    seedRole("MANAGER", Permissions.manager());
    seedRole("WAREHOUSE_STAFF", Permissions.warehouseStaff());
    seedRole("CASHIER", Permissions.cashier());

    seedAdminUser();
  }

  private void seedAdminUser() {
    if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
      return;
    }
    String email = adminEmail.trim().toLowerCase(Locale.ROOT);
    if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
      return;
    }
    Role admin = roleRepository.findByName("ADMIN").orElseThrow();
    userRepository.save(User.builder()
        .firstName("System")
        .lastName("Admin")
        .email(email)
        .passwordHash(passwordEncoder.encode(adminPassword))
        .role(admin)
        .build());
    log.info("Seeded bootstrap admin user '{}'", email);
  }

  private void seedRole(String name, List<String> permissions) {
    var existing = roleRepository.findByName(name);
    if (existing.isPresent()) {
      // Additive sync so permissions introduced by later sprints reach built-in roles.
      Role role = existing.get();
      Set<String> merged = new LinkedHashSet<>(role.getPermissions() != null ? role.getPermissions() : List.of());
      if (merged.addAll(permissions)) {
        role.setPermissions(new ArrayList<>(merged));
        roleRepository.save(role);
        log.info("Updated permissions of role '{}'", name);
      }
      return;
    }
    roleRepository.save(Role.builder().name(name).permissions(permissions).build());
    log.info("Seeded role '{}' with {} permissions", name, permissions.size());
  }
}
```

## `src/main/java/com/apex/config/SecurityConfig.java`

```java
package com.apex.config;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final int MIN_SECRET_BYTES = 32;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
            .requestMatchers("/api/v1/health").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(authenticationEntryPoint())
            .accessDeniedHandler(accessDeniedHandler()))
        .oauth2ResourceServer(oauth2 -> oauth2
            .authenticationEntryPoint(authenticationEntryPoint())
            .accessDeniedHandler(accessDeniedHandler())
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${app.jwt.secret}") String jwtSecret,
      @Value("${app.jwt.issuer:apex-backend}") String issuer) {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_BYTES + " characters long");
    }
    SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuer)));
    return decoder;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Extracts authorities from the JWT "permissions" claim so they can be used
   * with @PreAuthorize("hasAuthority('...')").
   */
  @Bean
  Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    return jwt -> {
      List<String> permissions = jwt.getClaimAsStringList("permissions");
      Collection<GrantedAuthority> authorities = permissions == null
          ? List.of()
          : permissions.stream().map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p)).toList();
      return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    };
  }

  private AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, ex) -> writeError(response, HttpStatus.UNAUTHORIZED,
        "Authentication required: missing or invalid access token");
  }

  private AccessDeniedHandler accessDeniedHandler() {
    return (request, response, ex) -> writeError(response, HttpStatus.FORBIDDEN,
        "Access denied: insufficient permissions");
  }

  private void writeError(HttpServletResponse response, HttpStatus status, String message) throws java.io.IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    String body = "{\"status\":" + status.value()
        + ",\"message\":\"" + message.replace("\"", "'")
        + "\",\"details\":null,\"timestamp\":\"" + LocalDateTime.now() + "\"}";
    response.getWriter().write(body);
  }
}
```

## `src/main/java/com/apex/config/WebConfig.java`

```java
package com.apex.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

  private final AuditInterceptor auditInterceptor;

  public WebConfig(AuditInterceptor auditInterceptor) {
    this.auditInterceptor = auditInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(auditInterceptor).addPathPatterns("/api/**");
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") String allowedOrigins) {
    List<String> origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(o -> !o.isEmpty())
        .toList();

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    config.setExposedHeaders(List.of("Content-Disposition"));
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
```

## `src/main/java/com/apex/controller/AlertController.java`

```java
package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.AlertRequests.ReorderRuleRequest;
import com.apex.dto.response.AlertResponses.AlertResponse;
import com.apex.dto.response.AlertResponses.AlertSummary;
import com.apex.dto.response.AlertResponses.ReorderRuleResponse;
import com.apex.dto.response.AlertResponses.ReplenishmentSuggestion;
import com.apex.model.StockAlert.Status;
import com.apex.service.AlertService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AlertController {

  private final AlertService alertService;

  @PostMapping("/reorder-rules")
  @PreAuthorize("hasAuthority('alert:manage')")
  public ResponseEntity<ReorderRuleResponse> createRule(@Valid @RequestBody ReorderRuleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(alertService.createRule(request));
  }

  @GetMapping("/reorder-rules")
  @PreAuthorize("hasAuthority('alert:read')")
  public ResponseEntity<List<ReorderRuleResponse>> listRules(@RequestParam(required = false) UUID warehouseId) {
    return ResponseEntity.ok(alertService.listRules(warehouseId));
  }

  @PutMapping("/reorder-rules/{id}")
  @PreAuthorize("hasAuthority('alert:manage')")
  public ResponseEntity<ReorderRuleResponse> updateRule(
      @PathVariable UUID id,
      @Valid @RequestBody ReorderRuleRequest request) {
    return ResponseEntity.ok(alertService.updateRule(id, request));
  }

  @DeleteMapping("/reorder-rules/{id}")
  @PreAuthorize("hasAuthority('alert:manage')")
  public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
    alertService.deleteRule(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/reorder-rules/suggestions")
  @PreAuthorize("hasAuthority('alert:read')")
  public ResponseEntity<List<ReplenishmentSuggestion>> suggestions(@RequestParam(required = false) UUID warehouseId) {
    return ResponseEntity.ok(alertService.suggestions(warehouseId));
  }

  /** Without a status filter, returns alerts that still need attention (OPEN and ACKNOWLEDGED). */
  @GetMapping("/alerts")
  @PreAuthorize("hasAuthority('alert:read')")
  public ResponseEntity<Page<AlertResponse>> listAlerts(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) UUID warehouseId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(alertService.listAlerts(status, warehouseId, pageable));
  }

  @GetMapping("/alerts/summary")
  @PreAuthorize("hasAuthority('alert:read')")
  public ResponseEntity<AlertSummary> summary() {
    return ResponseEntity.ok(alertService.summary());
  }

  @PostMapping("/alerts/{id}/acknowledge")
  @PreAuthorize("hasAuthority('alert:manage')")
  public ResponseEntity<AlertResponse> acknowledge(@PathVariable UUID id) {
    return ResponseEntity.ok(alertService.acknowledge(id));
  }
}
```

## `src/main/java/com/apex/controller/AuthController.java`

```java
package com.apex.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.AuthRequests.LoginRequest;
import com.apex.dto.request.AuthRequests.RefreshTokenRequest;
import com.apex.dto.request.AuthRequests.RegisterRequest;
import com.apex.dto.response.AuthResponses.AuthResponse;
import com.apex.dto.response.AuthResponses.UserResponse;
import com.apex.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @PreAuthorize("hasAuthority('user:create')")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(authService.refresh(request));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me(Authentication authentication) {
    return ResponseEntity.ok(authService.getUserById(UUID.fromString(authentication.getName())));
  }

  @GetMapping("/users/{id}")
  @PreAuthorize("hasAuthority('user:read') or #id.toString() == authentication.name")
  public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
    return ResponseEntity.ok(authService.getUserById(id));
  }
}
```

## `src/main/java/com/apex/controller/CategoryController.java`

```java
package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.CategoryRequests.CreateCategoryRequest;
import com.apex.dto.request.CategoryRequests.MoveCategoryRequest;
import com.apex.dto.request.CategoryRequests.UpdateCategoryRequest;
import com.apex.dto.response.CategoryResponse;
import com.apex.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  @PostMapping
  @PreAuthorize("hasAuthority('category:create')")
  public ResponseEntity<CategoryResponse> createCategory(
      @Valid @RequestBody CreateCategoryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('category:read')")
  public ResponseEntity<List<CategoryResponse>> getCategoryTree() {
    return ResponseEntity.ok(categoryService.getCategoryTree());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('category:read')")
  public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id) {
    return ResponseEntity.ok(categoryService.getCategory(id));
  }

  @GetMapping("/slug/{slug}")
  @PreAuthorize("hasAuthority('category:read')")
  public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
    return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
  }

  @GetMapping("/{parentId}/subcategories")
  @PreAuthorize("hasAuthority('category:read')")
  public ResponseEntity<List<CategoryResponse>> getSubcategories(@PathVariable UUID parentId) {
    return ResponseEntity.ok(categoryService.getSubcategories(parentId));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('category:update')")
  public ResponseEntity<CategoryResponse> updateCategory(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCategoryRequest request) {
    return ResponseEntity.ok(categoryService.updateCategory(id, request));
  }

  @PatchMapping("/{id}/parent")
  @PreAuthorize("hasAuthority('category:update')")
  public ResponseEntity<CategoryResponse> moveCategory(
      @PathVariable UUID id,
      @RequestBody MoveCategoryRequest request) {
    return ResponseEntity.ok(categoryService.moveCategory(id, request.parentId()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('category:delete')")
  public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.noContent().build();
  }
}
```

## `src/main/java/com/apex/controller/CustomerController.java`

```java
package com.apex.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.CustomerRequests.CustomerRequest;
import com.apex.dto.response.OrderResponses.CustomerResponse;
import com.apex.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @PostMapping
  @PreAuthorize("hasAuthority('customer:create')")
  public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('customer:read')")
  public ResponseEntity<Page<CustomerResponse>> list(
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return ResponseEntity.ok(customerService.list(keyword, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('customer:read')")
  public ResponseEntity<CustomerResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(customerService.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('customer:update')")
  public ResponseEntity<CustomerResponse> update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
    return ResponseEntity.ok(customerService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('customer:delete')")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    customerService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
```

## `src/main/java/com/apex/controller/HealthController.java`

```java
package com.apex.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  @GetMapping("/api/v1/health")
  public ResponseEntity<Map<String, String>> getHealth() {
    Map<String, String> healthStatus = new HashMap<>();
    healthStatus.put("status", "200 OK");
    healthStatus.put("message", "Application is running smoothly.");
    return new ResponseEntity<>(healthStatus, HttpStatus.OK);
  }
}
```

## `src/main/java/com/apex/controller/InventoryController.java`

```java
package com.apex.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.InventoryRequests.AdjustStockRequest;
import com.apex.dto.request.InventoryRequests.AssignLocationRequest;
import com.apex.dto.request.InventoryRequests.StockCountRequest;
import com.apex.dto.response.InventoryResponses.MovementResponse;
import com.apex.dto.response.InventoryResponses.StockLevelResponse;
import com.apex.dto.response.InventoryResponses.VariantStockSummary;
import com.apex.model.StockMovement.MovementType;
import com.apex.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

  private final InventoryService inventoryService;

  @GetMapping
  @PreAuthorize("hasAuthority('inventory:read')")
  public ResponseEntity<Page<StockLevelResponse>> listStock(
      @RequestParam(required = false) UUID warehouseId,
      @RequestParam(required = false) UUID variantId,
      @RequestParam(defaultValue = "false") boolean inStockOnly,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(inventoryService.listStock(warehouseId, variantId, inStockOnly, pageable));
  }

  @GetMapping("/variants/{variantId}")
  @PreAuthorize("hasAuthority('inventory:read')")
  public ResponseEntity<VariantStockSummary> variantSummary(@PathVariable UUID variantId) {
    return ResponseEntity.ok(inventoryService.getVariantSummary(variantId));
  }

  @GetMapping("/movements")
  @PreAuthorize("hasAuthority('inventory:read')")
  public ResponseEntity<Page<MovementResponse>> listMovements(
      @RequestParam(required = false) UUID variantId,
      @RequestParam(required = false) UUID warehouseId,
      @RequestParam(required = false) MovementType type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(inventoryService.listMovements(variantId, warehouseId, type, from, to, pageable));
  }

  @PostMapping("/adjustments")
  @PreAuthorize("hasAuthority('inventory:adjust')")
  public ResponseEntity<StockLevelResponse> adjust(@Valid @RequestBody AdjustStockRequest request) {
    return ResponseEntity.ok(inventoryService.adjust(request));
  }

  @PostMapping("/stock-counts")
  @PreAuthorize("hasAuthority('inventory:adjust')")
  public ResponseEntity<StockLevelResponse> stockCount(@Valid @RequestBody StockCountRequest request) {
    return ResponseEntity.ok(inventoryService.stockCount(request));
  }

  @PutMapping("/variants/{variantId}/warehouses/{warehouseId}/location")
  @PreAuthorize("hasAuthority('inventory:adjust')")
  public ResponseEntity<StockLevelResponse> assignLocation(
      @PathVariable UUID variantId,
      @PathVariable UUID warehouseId,
      @RequestBody AssignLocationRequest request) {
    return ResponseEntity.ok(inventoryService.assignLocation(variantId, warehouseId, request.locationId()));
  }
}
```

## `src/main/java/com/apex/controller/OrderController.java`

```java
package com.apex.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.OrderRequests.CancelOrderRequest;
import com.apex.dto.request.OrderRequests.CreateOrderRequest;
import com.apex.dto.request.OrderRequests.CreateReturnRequest;
import com.apex.dto.request.OrderRequests.PickRequest;
import com.apex.dto.request.OrderRequests.ShipOrderRequest;
import com.apex.dto.response.OrderResponses.ReturnResponse;
import com.apex.service.FulfilmentService;
import java.util.List;
import java.util.stream.Collectors;
import com.apex.dto.response.OrderResponses.OrderResponse;
import com.apex.model.SalesOrder.Status;
import com.apex.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;
  private final FulfilmentService fulfilmentService;

  @PostMapping
  @PreAuthorize("hasAuthority('order:create')")
  public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('order:read')")
  public ResponseEntity<Page<OrderResponse>> list(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) UUID customerId,
      @RequestParam(required = false) UUID warehouseId,
      @RequestParam(required = false) String orderNumber,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(orderService.list(status, customerId, warehouseId, orderNumber, from, to, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('order:read')")
  public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(orderService.get(id));
  }

  // ---------- Fulfilment ----------

  @PostMapping("/{id}/pick")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<OrderResponse> pick(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) PickRequest request) {
    java.util.Map<UUID, Integer> picks = request == null || request.lines() == null ? null
        : request.lines().stream().collect(Collectors.toMap(
            PickRequest.PickLine::lineId, PickRequest.PickLine::quantity, Integer::sum));
    return ResponseEntity.ok(fulfilmentService.pick(id, picks));
  }

  @PostMapping("/{id}/pack")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<OrderResponse> pack(@PathVariable UUID id) {
    return ResponseEntity.ok(fulfilmentService.pack(id));
  }

  @PostMapping("/{id}/ship")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<OrderResponse> ship(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) ShipOrderRequest request) {
    return ResponseEntity.ok(fulfilmentService.ship(id, request));
  }

  @PostMapping("/{id}/deliver")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<OrderResponse> deliver(@PathVariable UUID id) {
    return ResponseEntity.ok(fulfilmentService.deliver(id));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('order:cancel')")
  public ResponseEntity<OrderResponse> cancel(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) CancelOrderRequest request) {
    return ResponseEntity.ok(fulfilmentService.cancel(id, request == null ? null : request.reason()));
  }

  // ---------- Returns ----------

  @PostMapping("/{id}/returns")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<ReturnResponse> createReturn(
      @PathVariable UUID id,
      @Valid @RequestBody CreateReturnRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(fulfilmentService.createReturn(id, request));
  }

  @GetMapping("/{id}/returns")
  @PreAuthorize("hasAuthority('order:read')")
  public ResponseEntity<List<ReturnResponse>> listReturns(@PathVariable UUID id) {
    return ResponseEntity.ok(fulfilmentService.listReturns(id));
  }
}
```

## `src/main/java/com/apex/controller/ProductController.java`

```java
package com.apex.controller;

import com.apex.dto.request.ProductRequests.CreateBarcodeRequest;
import com.apex.dto.request.ProductRequests.CreateProductRequest;
import com.apex.dto.request.ProductRequests.CreateMediaRequest;
import com.apex.dto.request.ProductRequests.CreateVariantRequest;
import com.apex.dto.request.ProductRequests.UpdateProductRequest;
import com.apex.dto.request.ProductRequests.UpdateVariantRequest;
import com.apex.dto.response.ProductResponses.BarcodeResponse;
import com.apex.dto.response.ProductResponses.MediaResponse;
import com.apex.dto.response.ProductResponses.ProductResponse;
import com.apex.dto.response.ProductResponses.VariantResponse;
import com.apex.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  // ---------- Products ----------

  @PostMapping
  @PreAuthorize("hasAuthority('product:create')")
  public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<Page<ProductResponse>> listProducts(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(productService.listProducts(categoryId, keyword, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
    return ResponseEntity.ok(productService.getProduct(id));
  }

  @GetMapping("/slug/{slug}")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
    return ResponseEntity.ok(productService.getProductBySlug(slug));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<ProductResponse> updateProduct(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateProductRequest request) {
    return ResponseEntity.ok(productService.updateProduct(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('product:delete')")
  public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }

  // ---------- Variants ----------

  @PostMapping("/{productId}/variants")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<VariantResponse> addVariant(
      @PathVariable UUID productId,
      @Valid @RequestBody CreateVariantRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.addVariant(productId, request));
  }

  @GetMapping("/{productId}/variants")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<Page<VariantResponse>> listVariants(
      @PathVariable UUID productId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(productService.listVariants(productId, pageable));
  }

  @GetMapping("/variants/sku/{sku}")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<VariantResponse> getVariantBySku(@PathVariable String sku) {
    return ResponseEntity.ok(productService.getVariantBySku(sku));
  }

  @PutMapping("/variants/{variantId}")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<VariantResponse> updateVariant(
      @PathVariable UUID variantId,
      @Valid @RequestBody UpdateVariantRequest request) {
    return ResponseEntity.ok(productService.updateVariant(variantId, request));
  }

  @DeleteMapping("/variants/{variantId}")
  @PreAuthorize("hasAuthority('product:delete')")
  public ResponseEntity<Void> deactivateVariant(@PathVariable UUID variantId) {
    productService.deactivateVariant(variantId);
    return ResponseEntity.noContent().build();
  }

  // ---------- Media ----------

  @PostMapping("/{productId}/media")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<MediaResponse> addMedia(
      @PathVariable UUID productId,
      @Valid @RequestBody CreateMediaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.addMedia(productId, request));
  }

  @GetMapping("/{productId}/media")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<List<MediaResponse>> listMedia(@PathVariable UUID productId) {
    return ResponseEntity.ok(productService.listMedia(productId));
  }

  @DeleteMapping("/media/{mediaId}")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<Void> deleteMedia(@PathVariable UUID mediaId) {
    productService.deleteMedia(mediaId);
    return ResponseEntity.noContent().build();
  }

  // ---------- Barcodes & Hardware Scanners ----------

  @DeleteMapping("/barcodes/id/{barcodeId}")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<Void> removeBarcode(@PathVariable UUID barcodeId) {
    productService.removeBarcode(barcodeId);
    return ResponseEntity.noContent().build();
  }


  @PostMapping("/variants/{variantId}/barcodes")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<BarcodeResponse> addBarcode(
      @PathVariable UUID variantId,
      @Valid @RequestBody CreateBarcodeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.addBarcode(variantId, request));
  }

  @GetMapping("/barcodes/{barcode}")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<VariantResponse> lookupByBarcode(@PathVariable String barcode) {
    return ResponseEntity.ok(productService.lookupByBarcode(barcode));
  }
}
```

## `src/main/java/com/apex/controller/PurchaseOrderController.java`

```java
package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.PurchaseOrderRequests.CreatePurchaseOrderRequest;
import com.apex.dto.request.PurchaseOrderRequests.ReceivePurchaseOrderRequest;
import com.apex.dto.request.PurchaseOrderRequests.UpdatePurchaseOrderRequest;
import com.apex.dto.response.PurchaseOrderResponses.GoodsReceiptResponse;
import com.apex.dto.response.PurchaseOrderResponses.PurchaseOrderResponse;
import com.apex.model.PurchaseOrder.Status;
import com.apex.service.PurchaseOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

  private final PurchaseOrderService service;

  @PostMapping
  @PreAuthorize("hasAuthority('purchase:create')")
  public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('purchase:read')")
  public ResponseEntity<Page<PurchaseOrderResponse>> list(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) UUID supplierId,
      @RequestParam(required = false) UUID warehouseId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(service.list(status, supplierId, warehouseId, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase:read')")
  public ResponseEntity<PurchaseOrderResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase:update')")
  public ResponseEntity<PurchaseOrderResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdatePurchaseOrderRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @PostMapping("/{id}/submit")
  @PreAuthorize("hasAuthority('purchase:update')")
  public ResponseEntity<PurchaseOrderResponse> submit(@PathVariable UUID id) {
    return ResponseEntity.ok(service.submit(id));
  }

  @PostMapping("/{id}/receive")
  @PreAuthorize("hasAuthority('purchase:receive')")
  public ResponseEntity<GoodsReceiptResponse> receive(
      @PathVariable UUID id,
      @Valid @RequestBody ReceivePurchaseOrderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.receive(id, request));
  }

  @GetMapping("/{id}/receipts")
  @PreAuthorize("hasAuthority('purchase:read')")
  public ResponseEntity<List<GoodsReceiptResponse>> receipts(@PathVariable UUID id) {
    return ResponseEntity.ok(service.listReceipts(id));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('purchase:cancel')")
  public ResponseEntity<PurchaseOrderResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(service.cancel(id));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('purchase:update')")
  public ResponseEntity<PurchaseOrderResponse> close(@PathVariable UUID id) {
    return ResponseEntity.ok(service.close(id));
  }
}
```

## `src/main/java/com/apex/controller/ReportController.java`

```java
package com.apex.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.response.ReportResponses.AuditLogResponse;
import com.apex.dto.response.ReportResponses.Dashboard;
import com.apex.dto.response.ReportResponses.LowStockRow;
import com.apex.dto.response.ReportResponses.SalesReport;
import com.apex.dto.response.ReportResponses.StockValuationReport;
import com.apex.dto.response.ReportResponses.TopProductRow;
import com.apex.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  @GetMapping("/dashboard")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<Dashboard> dashboard() {
    return ResponseEntity.ok(reportService.dashboard());
  }

  @GetMapping("/reports/stock-valuation")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<StockValuationReport> stockValuation(@RequestParam(required = false) UUID warehouseId) {
    return ResponseEntity.ok(reportService.stockValuation(warehouseId));
  }

  @GetMapping("/reports/low-stock")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<List<LowStockRow>> lowStock(@RequestParam(defaultValue = "10") int threshold) {
    return ResponseEntity.ok(reportService.lowStock(threshold));
  }

  /** Revenue is recognised when an order ships; defaults to the last 30 days. */
  @GetMapping("/reports/sales")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<SalesReport> sales(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ResponseEntity.ok(reportService.sales(from, to));
  }

  @GetMapping("/reports/top-products")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<List<TopProductRow>> topProducts(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "10") int limit) {
    return ResponseEntity.ok(reportService.topProducts(from, to, limit));
  }

  @GetMapping("/audit-logs")
  @PreAuthorize("hasAuthority('audit:read')")
  public ResponseEntity<Page<AuditLogResponse>> auditLogs(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String method,
      @RequestParam(required = false) String resource,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(reportService.auditLogs(userId, method, resource, from, to, pageable));
  }
}
```

## `src/main/java/com/apex/controller/RoleController.java`

```java
package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.RoleRequests.CreateRoleRequest;
import com.apex.dto.request.RoleRequests.UpdateRoleRequest;
import com.apex.dto.response.RoleResponse;
import com.apex.service.RoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;

  @GetMapping
  @PreAuthorize("hasAuthority('role:read')")
  public ResponseEntity<List<RoleResponse>> listRoles() {
    return ResponseEntity.ok(roleService.listRoles());
  }

  @GetMapping("/permissions")
  @PreAuthorize("hasAuthority('role:read')")
  public ResponseEntity<List<String>> listPermissions() {
    return ResponseEntity.ok(roleService.listPermissions());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('role:read')")
  public ResponseEntity<RoleResponse> getRole(@PathVariable UUID id) {
    return ResponseEntity.ok(roleService.getRole(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('role:manage')")
  public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('role:manage')")
  public ResponseEntity<RoleResponse> updateRole(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateRoleRequest request) {
    return ResponseEntity.ok(roleService.updateRole(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('role:manage')")
  public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
    roleService.deleteRole(id);
    return ResponseEntity.noContent().build();
  }
}
```

## `src/main/java/com/apex/controller/SupplierController.java`

```java
package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.SupplierRequests.CreateSupplierRequest;
import com.apex.dto.request.SupplierRequests.SupplierProductRequest;
import com.apex.dto.request.SupplierRequests.UpdateSupplierRequest;
import com.apex.dto.response.SupplierResponses.SupplierProductResponse;
import com.apex.dto.response.SupplierResponses.SupplierResponse;
import com.apex.service.SupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

  private final SupplierService supplierService;

  @PostMapping
  @PreAuthorize("hasAuthority('supplier:create')")
  public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('supplier:read')")
  public ResponseEntity<Page<SupplierResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return ResponseEntity.ok(supplierService.list(keyword, active, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('supplier:read')")
  public ResponseEntity<SupplierResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(supplierService.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('supplier:update')")
  public ResponseEntity<SupplierResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateSupplierRequest request) {
    return ResponseEntity.ok(supplierService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('supplier:delete')")
  public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
    supplierService.deactivate(id);
    return ResponseEntity.noContent().build();
  }

  // ---------- Supplier catalogue ----------

  @PostMapping("/{id}/products")
  @PreAuthorize("hasAuthority('supplier:update')")
  public ResponseEntity<SupplierProductResponse> addProduct(
      @PathVariable UUID id,
      @Valid @RequestBody SupplierProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.addProduct(id, request));
  }

  @GetMapping("/{id}/products")
  @PreAuthorize("hasAuthority('supplier:read')")
  public ResponseEntity<List<SupplierProductResponse>> listProducts(@PathVariable UUID id) {
    return ResponseEntity.ok(supplierService.listProducts(id));
  }

  @PutMapping("/{id}/products/{variantId}")
  @PreAuthorize("hasAuthority('supplier:update')")
  public ResponseEntity<SupplierProductResponse> updateProduct(
      @PathVariable UUID id,
      @PathVariable UUID variantId,
      @Valid @RequestBody SupplierProductRequest request) {
    return ResponseEntity.ok(supplierService.updateProduct(id, variantId, request));
  }

  @DeleteMapping("/{id}/products/{variantId}")
  @PreAuthorize("hasAuthority('supplier:update')")
  public ResponseEntity<Void> removeProduct(@PathVariable UUID id, @PathVariable UUID variantId) {
    supplierService.removeProduct(id, variantId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/by-variant/{variantId}")
  @PreAuthorize("hasAuthority('supplier:read')")
  public ResponseEntity<List<SupplierProductResponse>> suppliersForVariant(@PathVariable UUID variantId) {
    return ResponseEntity.ok(supplierService.listSuppliersForVariant(variantId));
  }
}
```

## `src/main/java/com/apex/controller/TransferController.java`

```java
package com.apex.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.TransferRequests.CreateTransferRequest;
import com.apex.dto.request.TransferRequests.ReceiveTransferRequest;
import com.apex.dto.response.TransferResponses.TransferResponse;
import com.apex.model.StockTransfer.Status;
import com.apex.service.TransferService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

  private final TransferService transferService;

  @PostMapping
  @PreAuthorize("hasAuthority('transfer:create')")
  public ResponseEntity<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(transferService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('transfer:read')")
  public ResponseEntity<Page<TransferResponse>> list(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) UUID warehouseId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(transferService.list(status, warehouseId, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('transfer:read')")
  public ResponseEntity<TransferResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(transferService.get(id));
  }

  @PostMapping("/{id}/dispatch")
  @PreAuthorize("hasAuthority('transfer:update')")
  public ResponseEntity<TransferResponse> dispatch(@PathVariable UUID id) {
    return ResponseEntity.ok(transferService.dispatch(id));
  }

  @PostMapping("/{id}/receive")
  @PreAuthorize("hasAuthority('transfer:update')")
  public ResponseEntity<TransferResponse> receive(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) ReceiveTransferRequest request) {
    return ResponseEntity.ok(transferService.receive(id, request));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('transfer:update')")
  public ResponseEntity<TransferResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(transferService.cancel(id));
  }
}
```

## `src/main/java/com/apex/controller/UserController.java`

```java
package com.apex.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.UserRequests.ChangePasswordRequest;
import com.apex.dto.request.UserRequests.ResetPasswordRequest;
import com.apex.dto.request.UserRequests.UpdateUserRequest;
import com.apex.dto.response.AuthResponses.UserResponse;
import com.apex.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/users")
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<Page<UserResponse>> listUsers(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(userService.listUsers(pageable));
  }

  @GetMapping("/users/{id}")
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.getUser(id));
  }

  @PutMapping("/users/{id}")
  @PreAuthorize("hasAuthority('user:update')")
  public ResponseEntity<UserResponse> updateUser(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateUserRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(userService.updateUser(id, request, UUID.fromString(authentication.getName())));
  }

  @DeleteMapping("/users/{id}")
  @PreAuthorize("hasAuthority('user:delete')")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id, Authentication authentication) {
    userService.deleteUser(id, UUID.fromString(authentication.getName()));
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/users/{id}/password")
  @PreAuthorize("hasAuthority('user:update')")
  public ResponseEntity<Void> resetPassword(
      @PathVariable UUID id,
      @Valid @RequestBody ResetPasswordRequest request) {
    userService.resetPassword(id, request);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/auth/password")
  public ResponseEntity<Void> changeOwnPassword(
      @Valid @RequestBody ChangePasswordRequest request,
      Authentication authentication) {
    userService.changePassword(UUID.fromString(authentication.getName()), request);
    return ResponseEntity.noContent().build();
  }
}
```

## `src/main/java/com/apex/controller/WarehouseController.java`

```java
package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.WarehouseRequests.CreateWarehouseRequest;
import com.apex.dto.request.WarehouseRequests.LocationRequest;
import com.apex.dto.request.WarehouseRequests.UpdateWarehouseRequest;
import com.apex.dto.response.WarehouseResponses.LocationResponse;
import com.apex.dto.response.WarehouseResponses.WarehouseResponse;
import com.apex.service.WarehouseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

  private final WarehouseService warehouseService;

  @PostMapping
  @PreAuthorize("hasAuthority('warehouse:create')")
  public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('warehouse:read')")
  public ResponseEntity<Page<WarehouseResponse>> list(
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 20, sort = "code") Pageable pageable) {
    return ResponseEntity.ok(warehouseService.listWarehouses(active, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('warehouse:read')")
  public ResponseEntity<WarehouseResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(warehouseService.getWarehouse(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('warehouse:update')")
  public ResponseEntity<WarehouseResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateWarehouseRequest request) {
    return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('warehouse:delete')")
  public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
    warehouseService.deactivateWarehouse(id);
    return ResponseEntity.noContent().build();
  }

  // ---------- Locations ----------

  @PostMapping("/{id}/locations")
  @PreAuthorize("hasAuthority('warehouse:update')")
  public ResponseEntity<LocationResponse> addLocation(
      @PathVariable UUID id,
      @Valid @RequestBody LocationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.addLocation(id, request));
  }

  @GetMapping("/{id}/locations")
  @PreAuthorize("hasAuthority('warehouse:read')")
  public ResponseEntity<List<LocationResponse>> listLocations(@PathVariable UUID id) {
    return ResponseEntity.ok(warehouseService.listLocations(id));
  }

  @PutMapping("/locations/{locationId}")
  @PreAuthorize("hasAuthority('warehouse:update')")
  public ResponseEntity<LocationResponse> updateLocation(
      @PathVariable UUID locationId,
      @Valid @RequestBody LocationRequest request) {
    return ResponseEntity.ok(warehouseService.updateLocation(locationId, request));
  }

  @DeleteMapping("/locations/{locationId}")
  @PreAuthorize("hasAuthority('warehouse:update')")
  public ResponseEntity<Void> deactivateLocation(@PathVariable UUID locationId) {
    warehouseService.deactivateLocation(locationId);
    return ResponseEntity.noContent().build();
  }
}
```

## `src/main/java/com/apex/dto/request/AlertRequests.java`

```java
package com.apex.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AlertRequests {

  public record ReorderRuleRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull(message = "Warehouse ID is required") UUID warehouseId,
      @NotNull @Min(value = 0, message = "Reorder point cannot be negative") Integer reorderPoint,
      @NotNull @Min(value = 1, message = "Reorder quantity must be at least 1") Integer reorderQuantity,
      Boolean isActive) {
  }
}
```

## `src/main/java/com/apex/dto/request/AuthRequests.java`

```java
package com.apex.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AuthRequests {

  public record LoginRequest(
      @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
      @NotBlank(message = "Password is required") String password) {
  }

  public record RegisterRequest(
      @NotBlank(message = "First name is required") String firstName,
      @NotBlank(message = "Last name is required") String lastName,
      @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
      @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters long") String password,
      @NotNull(message = "Role ID is required") UUID roleId) {
  }

  public record RefreshTokenRequest(
      @NotBlank(message = "Refresh token is required") String refreshToken) {
  }
}
```

## `src/main/java/com/apex/dto/request/CategoryRequests.java`

```java
package com.apex.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class CategoryRequests {

  public record CreateCategoryRequest(
      @NotBlank(message = "Category name is required") String name,
      String description,
      UUID parentId) {
  }

  /** A null parentId moves the category to the root level. */
  public record MoveCategoryRequest(UUID parentId) {
  }

  public record UpdateCategoryRequest(
      @NotBlank(message = "Category name is required") String name,
      String description,
      Boolean isActive) {
  }
}
```

## `src/main/java/com/apex/dto/request/CustomerRequests.java`

```java
package com.apex.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CustomerRequests {

  public record CustomerRequest(
      @NotBlank(message = "Customer name is required") @Size(max = 200) String name,
      @Email(message = "Invalid email format") String email,
      String phone,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      @Size(max = 1000) String notes,
      Boolean isActive) {
  }
}
```

## `src/main/java/com/apex/dto/request/InventoryRequests.java`

```java
package com.apex.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InventoryRequests {

  /** Positive delta adds stock, negative removes it. */
  public record AdjustStockRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull(message = "Warehouse ID is required") UUID warehouseId,
      @NotNull(message = "Quantity change is required") Integer quantityChange,
      @NotBlank(message = "A reason is required") @Size(max = 500) String reason,
      Boolean damaged) {
  }

  /** Physical count: sets quantity on hand to the counted figure. */
  public record StockCountRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull(message = "Warehouse ID is required") UUID warehouseId,
      @NotNull(message = "Counted quantity is required") @Min(value = 0, message = "Counted quantity cannot be negative") Integer countedQuantity,
      @Size(max = 500) String reason) {
  }

  public record AssignLocationRequest(UUID locationId) {
  }
}
```

## `src/main/java/com/apex/dto/request/OrderRequests.java`

```java
package com.apex.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.apex.model.SalesOrder.Channel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OrderRequests {

  public record CreateOrderRequest(
      UUID customerId,
      @NotNull(message = "Fulfilment warehouse is required") UUID warehouseId,
      Channel channel,
      @Size(max = 500) String shippingAddress,
      @Size(max = 1000) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid OrderLineRequest> lines) {
  }

  /** unitPrice defaults to the variant's retail price. */
  public record OrderLineRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
      @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0") BigDecimal unitPrice,
      @DecimalMin(value = "0.0") @DecimalMax(value = "100.0") BigDecimal discountPercent) {
  }

  /** Lines left out keep their current picked quantity; an empty body picks the whole order. */
  public record PickRequest(List<@Valid PickLine> lines) {
    public record PickLine(
        @NotNull UUID lineId,
        @NotNull @Min(value = 0, message = "Quantity cannot be negative") Integer quantity) {
    }
  }

  public record ShipOrderRequest(
      @Size(max = 100) String carrier,
      @Size(max = 100) String trackingNumber) {
  }

  public record CancelOrderRequest(@Size(max = 500) String reason) {
  }

  public record CreateReturnRequest(
      @Size(max = 500) String reason,
      Boolean restock,
      @NotEmpty(message = "At least one line is required") List<@Valid ReturnLineRequest> lines) {
  }

  public record ReturnLineRequest(
      @NotNull UUID orderLineId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
  }
}
```

## `src/main/java/com/apex/dto/request/ProductRequests.java`

```java
package com.apex.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductRequests {

  public record CreateProductRequest(
      @NotNull(message = "Category ID is required") UUID categoryId,
      @NotBlank(message = "Product name is required") String name,
      String description,
      String brand,
      @NotBlank(message = "Unit of measure is required") String unitOfMeasure,
      Boolean hasVariants,
      List<CreateVariantRequest> variants) {
  }

  public record UpdateProductRequest(
      @NotNull(message = "Category ID is required") UUID categoryId,
      @NotBlank(message = "Product name is required") String name,
      String description,
      String brand,
      @NotBlank(message = "Unit of measure is required") String unitOfMeasure,
      Boolean isActive) {
  }

  public record UpdateVariantRequest(
      Map<String, Object> variantAttributes,
      @NotNull(message = "Cost price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than 0") BigDecimal costPrice,
      @NotNull(message = "Retail price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Retail price must be greater than 0") BigDecimal retailPrice,
      Map<String, Object> dimensions,
      Map<String, Object> weight,
      Boolean isActive) {
  }

  public record CreateMediaRequest(
      UUID variantId,
      @NotBlank(message = "URL is required") String url,
      @NotBlank(message = "File name is required") String fileName,
      @NotBlank(message = "MIME type is required") String mimeType,
      Boolean isPrimary,
      Integer sortOrder) {
  }

  public record CreateVariantRequest(
      @NotBlank(message = "SKU is required") String sku,
      Map<String, Object> variantAttributes,
      @NotNull(message = "Cost price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than 0") BigDecimal costPrice,
      @NotNull(message = "Retail price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Retail price must be greater than 0") BigDecimal retailPrice,
      Map<String, Object> dimensions,
      Map<String, Object> weight,
      List<CreateBarcodeRequest> barcodes) {
  }

  public record CreateBarcodeRequest(
      @NotBlank(message = "Barcode string is required") String barcode,
      @NotBlank(message = "Barcode type is required") String type,
      Boolean isPrimary) {
  }
}
```

## `src/main/java/com/apex/dto/request/PurchaseOrderRequests.java`

```java
package com.apex.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PurchaseOrderRequests {

  public record CreatePurchaseOrderRequest(
      @NotNull(message = "Supplier is required") UUID supplierId,
      @NotNull(message = "Destination warehouse is required") UUID warehouseId,
      LocalDate expectedDate,
      @Size(max = 1000) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid PoLineRequest> lines) {
  }

  public record UpdatePurchaseOrderRequest(
      @NotNull(message = "Destination warehouse is required") UUID warehouseId,
      LocalDate expectedDate,
      @Size(max = 1000) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid PoLineRequest> lines) {
  }

  /** When unitCost is omitted the supplier's catalogue price (or the variant cost price) is used. */
  public record PoLineRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
      @DecimalMin(value = "0.0", inclusive = false, message = "Unit cost must be greater than 0") BigDecimal unitCost) {
  }

  public record ReceivePurchaseOrderRequest(
      @Size(max = 500) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid ReceiveLine> lines) {
  }

  public record ReceiveLine(
      @NotNull UUID lineId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
  }
}
```

## `src/main/java/com/apex/dto/request/RoleRequests.java`

```java
package com.apex.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RoleRequests {

  public record CreateRoleRequest(
      @NotBlank(message = "Role name is required") @Size(max = 100) String name,
      @NotNull(message = "Permissions are required") List<String> permissions) {
  }

  public record UpdateRoleRequest(
      @NotBlank(message = "Role name is required") @Size(max = 100) String name,
      @NotNull(message = "Permissions are required") List<String> permissions) {
  }
}
```

## `src/main/java/com/apex/dto/request/SupplierRequests.java`

```java
package com.apex.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SupplierRequests {

  public record CreateSupplierRequest(
      @NotBlank(message = "Supplier code is required") @Size(max = 30) String code,
      @NotBlank(message = "Supplier name is required") @Size(max = 200) String name,
      String contactName,
      @Email(message = "Invalid email format") String email,
      String phone,
      String addressLine,
      String city,
      String country,
      String paymentTerms,
      @Min(value = 0, message = "Lead time cannot be negative") Integer leadTimeDays,
      @Size(max = 1000) String notes) {
  }

  public record UpdateSupplierRequest(
      @NotBlank(message = "Supplier name is required") @Size(max = 200) String name,
      String contactName,
      @Email(message = "Invalid email format") String email,
      String phone,
      String addressLine,
      String city,
      String country,
      String paymentTerms,
      @Min(value = 0, message = "Lead time cannot be negative") Integer leadTimeDays,
      @Size(max = 1000) String notes,
      Boolean isActive) {
  }

  public record SupplierProductRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      String supplierSku,
      @NotNull(message = "Unit cost is required") @DecimalMin(value = "0.0", inclusive = false, message = "Unit cost must be greater than 0") BigDecimal unitCost,
      @Min(value = 1, message = "Minimum order quantity must be at least 1") Integer minOrderQuantity,
      Boolean isPreferred) {
  }
}
```

## `src/main/java/com/apex/dto/request/TransferRequests.java`

```java
package com.apex.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TransferRequests {

  public record CreateTransferRequest(
      @NotNull(message = "Source warehouse is required") UUID fromWarehouseId,
      @NotNull(message = "Destination warehouse is required") UUID toWarehouseId,
      @Size(max = 500) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid TransferLineRequest> lines) {
  }

  public record TransferLineRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
  }

  /** Optional per-line override; lines left out are received in full. */
  public record ReceiveTransferRequest(List<@Valid ReceivedLine> lines) {
  }

  public record ReceivedLine(
      @NotNull UUID lineId,
      @NotNull @Min(value = 0, message = "Received quantity cannot be negative") Integer receivedQuantity) {
  }
}
```

## `src/main/java/com/apex/dto/request/UserRequests.java`

```java
package com.apex.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserRequests {

  public record UpdateUserRequest(
      @NotBlank(message = "First name is required") String firstName,
      @NotBlank(message = "Last name is required") String lastName,
      @NotNull(message = "Role ID is required") UUID roleId,
      Boolean isActive) {
  }

  public record ChangePasswordRequest(
      @NotBlank(message = "Current password is required") String currentPassword,
      @NotBlank(message = "New password is required") @Size(min = 8, message = "Password must be at least 8 characters long") String newPassword) {
  }

  public record ResetPasswordRequest(
      @NotBlank(message = "New password is required") @Size(min = 8, message = "Password must be at least 8 characters long") String newPassword) {
  }
}
```

## `src/main/java/com/apex/dto/request/WarehouseRequests.java`

```java
package com.apex.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WarehouseRequests {

  public record CreateWarehouseRequest(
      @NotBlank(message = "Warehouse code is required") @Size(max = 30) String code,
      @NotBlank(message = "Warehouse name is required") @Size(max = 150) String name,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      String contactPhone) {
  }

  public record UpdateWarehouseRequest(
      @NotBlank(message = "Warehouse name is required") @Size(max = 150) String name,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      String contactPhone,
      Boolean isActive) {
  }

  public record LocationRequest(
      @NotBlank(message = "Location code is required") @Size(max = 50) String code,
      @Size(max = 255) String description,
      Boolean isActive) {
  }
}
```

## `src/main/java/com/apex/dto/response/AlertResponses.java`

```java
package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AlertResponses {

  public record ReorderRuleResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      int reorderPoint,
      int reorderQuantity,
      boolean isActive) {
  }

  public record AlertResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      String type,
      String status,
      int availableQuantity,
      int reorderPoint,
      LocalDateTime createdAt,
      LocalDateTime acknowledgedAt,
      LocalDateTime resolvedAt) {
  }

  public record AlertSummary(long open, long acknowledged, long resolved) {
  }

  public record ReplenishmentSuggestion(
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      int availableQuantity,
      int reorderPoint,
      int suggestedQuantity,
      UUID preferredSupplierId,
      String preferredSupplierName,
      BigDecimal estimatedUnitCost) {
  }
}
```

## `src/main/java/com/apex/dto/response/AuthResponses.java`

```java
package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AuthResponses {

  public record AuthResponse(
      String accessToken,
      String refreshToken,
      String tokenType,
      long expiresIn,
      UserResponse user) {
    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
      this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
  }

  public record UserResponse(
      UUID id,
      String firstName,
      String lastName,
      String email,
      String roleName,
      List<String> permissions,
      Boolean isActive,
      Boolean mfaEnabled,
      LocalDateTime createdAt) {
  }
}
```

## `src/main/java/com/apex/dto/response/CategoryResponse.java`

```java
package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    UUID parentId,
    String name,
    String slug,
    String description,
    Boolean isActive,
    List<CategoryResponse> subcategories,
    LocalDateTime createdAt) {
}
```

## `src/main/java/com/apex/dto/response/InventoryResponses.java`

```java
package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InventoryResponses {

  public record StockLevelResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      UUID locationId,
      String locationCode,
      int quantityOnHand,
      int quantityReserved,
      int quantityAvailable,
      LocalDateTime updatedAt) {
  }

  public record VariantStockSummary(
      UUID variantId,
      String sku,
      String productName,
      long totalOnHand,
      long totalReserved,
      long totalAvailable,
      List<StockLevelResponse> warehouses) {
  }

  public record MovementResponse(
      UUID id,
      UUID variantId,
      String sku,
      UUID warehouseId,
      String warehouseCode,
      String type,
      int quantityChange,
      int quantityAfter,
      String referenceType,
      UUID referenceId,
      String reason,
      UUID performedBy,
      LocalDateTime createdAt) {
  }
}
```

## `src/main/java/com/apex/dto/response/OrderResponses.java`

```java
package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderResponses {

  public record CustomerResponse(
      UUID id,
      String name,
      String email,
      String phone,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      String notes,
      Boolean isActive,
      LocalDateTime createdAt) {
  }

  public record OrderResponse(
      UUID id,
      String orderNumber,
      UUID customerId,
      String customerName,
      UUID warehouseId,
      String warehouseCode,
      String status,
      String channel,
      String shippingAddress,
      String notes,
      BigDecimal subtotal,
      BigDecimal taxAmount,
      BigDecimal total,
      String carrier,
      String trackingNumber,
      List<OrderLineResponse> lines,
      LocalDateTime shippedAt,
      LocalDateTime deliveredAt,
      LocalDateTime createdAt) {
  }

  public record OrderLineResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      int quantity,
      int quantityPicked,
      int quantityReturned,
      BigDecimal unitPrice,
      BigDecimal discountPercent,
      BigDecimal lineTotal) {
  }

  public record ReturnResponse(
      UUID id,
      String returnNumber,
      UUID orderId,
      String orderNumber,
      String reason,
      boolean restocked,
      BigDecimal refundAmount,
      List<ReturnLineResponse> lines,
      LocalDateTime createdAt) {
  }

  public record ReturnLineResponse(UUID orderLineId, String sku, int quantity, BigDecimal refundAmount) {
  }
}
```

## `src/main/java/com/apex/dto/response/ProductResponses.java`

```java
package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductResponses {

  public record ProductResponse(
      UUID id,
      UUID categoryId,
      String categoryName,
      String name,
      String slug,
      String description,
      String brand,
      String unitOfMeasure,
      Boolean hasVariants,
      Boolean isActive,
      List<VariantResponse> variants,
      List<String> mediaUrls,
      LocalDateTime createdAt) {
  }

  public record VariantResponse(
      UUID id,
      UUID productId,
      String sku,
      Map<String, Object> variantAttributes,
      BigDecimal costPrice,
      BigDecimal retailPrice,
      Map<String, Object> dimensions,
      Map<String, Object> weight,
      Boolean isActive,
      List<BarcodeResponse> barcodes,
      LocalDateTime createdAt) {
  }

  public record MediaResponse(
      UUID id,
      UUID productId,
      UUID variantId,
      String url,
      String fileName,
      String mimeType,
      Boolean isPrimary,
      Integer sortOrder) {
  }

  public record BarcodeResponse(
      UUID id,
      String barcode,
      String type,
      Boolean isPrimary) {
  }
}
```

## `src/main/java/com/apex/dto/response/PurchaseOrderResponses.java`

```java
package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PurchaseOrderResponses {

  public record PurchaseOrderResponse(
      UUID id,
      String poNumber,
      UUID supplierId,
      String supplierName,
      UUID warehouseId,
      String warehouseCode,
      String status,
      LocalDate expectedDate,
      String notes,
      BigDecimal totalAmount,
      List<PoLineResponse> lines,
      LocalDateTime orderedAt,
      LocalDateTime createdAt) {
  }

  public record PoLineResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      int quantityOrdered,
      int quantityReceived,
      int quantityOutstanding,
      BigDecimal unitCost,
      BigDecimal lineTotal) {
  }

  public record GoodsReceiptResponse(
      UUID id,
      String receiptNumber,
      String notes,
      List<ReceiptLineResponse> lines,
      UUID receivedBy,
      LocalDateTime receivedAt) {
  }

  public record ReceiptLineResponse(UUID poLineId, String sku, int quantity) {
  }
}
```

## `src/main/java/com/apex/dto/response/ReportResponses.java`

```java
package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReportResponses {

  public record StockValuationReport(
      long totalUnits,
      BigDecimal totalCostValue,
      BigDecimal totalRetailValue,
      List<StockValuationRow> rows) {
  }

  public record StockValuationRow(
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      int quantityOnHand,
      BigDecimal unitCost,
      BigDecimal unitRetailPrice,
      BigDecimal costValue,
      BigDecimal retailValue) {
  }

  public record LowStockRow(
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      int quantityOnHand,
      int quantityAvailable) {
  }

  public record SalesReport(
      LocalDate from,
      LocalDate to,
      long orders,
      long unitsSold,
      BigDecimal revenue,
      BigDecimal cost,
      BigDecimal grossProfit,
      BigDecimal refunds,
      BigDecimal netRevenue,
      List<SalesDay> days) {
  }

  public record SalesDay(LocalDate date, long orders, long units, BigDecimal revenue, BigDecimal grossProfit) {
  }

  public record TopProductRow(
      UUID variantId,
      String sku,
      String productName,
      long unitsSold,
      BigDecimal revenue) {
  }

  public record Dashboard(
      long activeProducts,
      long activeWarehouses,
      long totalUnitsOnHand,
      BigDecimal stockCostValue,
      long openAlerts,
      long pendingOrders,
      long ordersToShip,
      long openPurchaseOrders,
      long ordersToday,
      BigDecimal revenueToday,
      LocalDateTime generatedAt) {
  }

  public record AuditLogResponse(
      UUID id,
      UUID userId,
      String userEmail,
      String method,
      String path,
      String resource,
      int statusCode,
      String ipAddress,
      LocalDateTime createdAt) {
  }
}
```

## `src/main/java/com/apex/dto/response/RoleResponse.java`

```java
package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RoleResponse(
    UUID id,
    String name,
    List<String> permissions,
    boolean builtIn,
    long userCount,
    LocalDateTime createdAt) {
}
```

## `src/main/java/com/apex/dto/response/SupplierResponses.java`

```java
package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class SupplierResponses {

  public record SupplierResponse(
      UUID id,
      String code,
      String name,
      String contactName,
      String email,
      String phone,
      String addressLine,
      String city,
      String country,
      String paymentTerms,
      Integer leadTimeDays,
      String notes,
      Boolean isActive,
      LocalDateTime createdAt) {
  }

  public record SupplierProductResponse(
      UUID id,
      UUID supplierId,
      String supplierName,
      UUID variantId,
      String sku,
      String productName,
      String supplierSku,
      BigDecimal unitCost,
      int minOrderQuantity,
      boolean isPreferred) {
  }
}
```

## `src/main/java/com/apex/dto/response/TransferResponses.java`

```java
package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TransferResponses {

  public record TransferResponse(
      UUID id,
      String referenceNumber,
      UUID fromWarehouseId,
      String fromWarehouseCode,
      UUID toWarehouseId,
      String toWarehouseCode,
      String status,
      String notes,
      List<TransferLineResponse> lines,
      LocalDateTime dispatchedAt,
      LocalDateTime receivedAt,
      LocalDateTime createdAt) {
  }

  public record TransferLineResponse(
      UUID id,
      UUID variantId,
      String sku,
      int quantity,
      Integer receivedQuantity) {
  }
}
```

## `src/main/java/com/apex/dto/response/WarehouseResponses.java`

```java
package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class WarehouseResponses {

  public record WarehouseResponse(
      UUID id,
      String code,
      String name,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      String contactPhone,
      Boolean isActive,
      LocalDateTime createdAt) {
  }

  public record LocationResponse(
      UUID id,
      UUID warehouseId,
      String code,
      String description,
      Boolean isActive) {
  }
}
```

## `src/main/java/com/apex/exception/BusinessRuleException.java`

```java
package com.apex.exception;

/** A request that is well-formed but violates a business rule (maps to 422). */
public class BusinessRuleException extends RuntimeException {
  public BusinessRuleException(String message) {
    super(message);
  }
}
```

## `src/main/java/com/apex/exception/DuplicateResourceException.java`

```java
package com.apex.exception;

public class DuplicateResourceException extends RuntimeException {
  public DuplicateResourceException(String message) {
    super(message);
  }
}
```

## `src/main/java/com/apex/exception/GlobalExceptionHandler.java`

```java
package com.apex.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  public record ApiError(
      int status,
      String message,
      List<String> details,
      LocalDateTime timestamp) {
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
    return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
    return build(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions", null);
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  @ExceptionHandler({ IllegalArgumentException.class, PropertyReferenceException.class })
  public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getField() + ": " + f.getDefaultMessage())
        .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
    List<String> details = ex.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
        .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
    return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", null);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", null);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
    return build(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'", null);
  }

  @ExceptionHandler({ DataIntegrityViolationException.class, OptimisticLockingFailureException.class })
  public ResponseEntity<ApiError> handleConflict(Exception ex) {
    log.warn("Data conflict: {}", ex.getMessage());
    return build(HttpStatus.CONFLICT,
        "The request conflicts with the current state of the data. Reload and retry.", null);
  }

  /** Framework exceptions that already carry a status (404 for unknown routes, 405, 415...). */
  @ExceptionHandler({ ErrorResponseException.class, NoResourceFoundException.class,
      HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class })
  public ResponseEntity<ApiError> handleErrorResponse(Exception ex) {
    ErrorResponse response = (ErrorResponse) ex;
    HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
    return build(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR,
        response.getBody().getDetail(), null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneralException(Exception ex) {
    log.error("Unhandled exception", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
  }

  private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
    return ResponseEntity.status(status)
        .body(new ApiError(status.value(), message, details, LocalDateTime.now()));
  }
}
```

## `src/main/java/com/apex/exception/ResourceNotFoundException.java`

```java
package com.apex.exception;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
```

## `src/main/java/com/apex/exception/UnauthorizedException.java`

```java
package com.apex.exception;

public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) {
    super(message);
  }
}
```

## `src/main/java/com/apex/mapper/CategoryMapper.java`

```java
package com.apex.mapper;

import com.apex.dto.response.CategoryResponse;
import com.apex.model.Category;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CategoryMapper {

  public CategoryResponse toResponse(Category category) {
    if (category == null)
      return null;

    List<CategoryResponse> subcategories = category.getChildren() != null
        ? category.getChildren().stream().map(this::toResponse).toList()
        : Collections.emptyList();

    return new CategoryResponse(
        category.getId(),
        category.getParent() != null ? category.getParent().getId() : null,
        category.getName(),
        category.getSlug(),
        category.getDescription(),
        category.getIsActive(),
        subcategories,
        category.getCreatedAt());
  }
}
```

## `src/main/java/com/apex/mapper/ProductMapper.java`

```java
package com.apex.mapper;

import com.apex.dto.response.ProductResponses.BarcodeResponse;
import com.apex.dto.response.ProductResponses.MediaResponse;
import com.apex.dto.response.ProductResponses.ProductResponse;
import com.apex.dto.response.ProductResponses.VariantResponse;
import com.apex.model.MediaAsset;
import com.apex.model.Product;
import com.apex.model.ProductBarcode;
import com.apex.model.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductMapper {

  public ProductResponse toProductResponse(Product product, List<MediaAsset> mediaAssets) {
    if (product == null)
      return null;

    List<VariantResponse> variants = product.getVariants() != null
        ? product.getVariants().stream().map(this::toVariantResponse).toList()
        : Collections.emptyList();

    List<String> mediaUrls = mediaAssets != null
        ? mediaAssets.stream().map(asset -> asset.getUrl()).toList()
        : Collections.emptyList();

    return new ProductResponse(
        product.getId(),
        product.getCategory() != null ? product.getCategory().getId() : null,
        product.getCategory() != null ? product.getCategory().getName() : null,
        product.getName(),
        product.getSlug(),
        product.getDescription(),
        product.getBrand(),
        product.getUnitOfMeasure(),
        product.getHasVariants(),
        product.getIsActive(),
        variants,
        mediaUrls,
        product.getCreatedAt());
  }

  public VariantResponse toVariantResponse(ProductVariant variant) {
    if (variant == null)
      return null;

    List<BarcodeResponse> barcodes = variant.getBarcodes() != null
        ? variant.getBarcodes().stream().map(this::toBarcodeResponse).toList()
        : Collections.emptyList();

    return new VariantResponse(
        variant.getId(),
        variant.getProduct() != null ? variant.getProduct().getId() : null,
        variant.getSku(),
        variant.getVariantAttributes(),
        variant.getCostPrice(),
        variant.getRetailPrice(),
        variant.getDimensions(),
        variant.getWeight(),
        variant.getIsActive(),
        barcodes,
        variant.getCreatedAt());
  }

  public MediaResponse toMediaResponse(MediaAsset asset) {
    return new MediaResponse(
        asset.getId(),
        asset.getProduct() != null ? asset.getProduct().getId() : null,
        asset.getVariant() != null ? asset.getVariant().getId() : null,
        asset.getUrl(),
        asset.getFileName(),
        asset.getMimeType(),
        asset.getIsPrimary(),
        asset.getSortOrder());
  }

  public BarcodeResponse toBarcodeResponse(ProductBarcode barcode) {
    if (barcode == null)
      return null;

    return new BarcodeResponse(
        barcode.getId(),
        barcode.getBarcode(),
        barcode.getType() != null ? barcode.getType().name() : null,
        barcode.getIsPrimary());
  }
}
```

## `src/main/java/com/apex/mapper/UserMapper.java`

```java
package com.apex.mapper;

import com.apex.dto.response.AuthResponses.UserResponse;
import com.apex.model.User;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class UserMapper {

  public UserResponse toResponse(User user) {
    if (user == null)
      return null;

    return new UserResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getRole() != null ? user.getRole().getName() : null,
        user.getRole() != null && user.getRole().getPermissions() != null
            ? user.getRole().getPermissions()
            : Collections.emptyList(),
        user.getIsActive(),
        user.getMfaEnabled(),
        user.getCreatedAt());
  }
}
```

## `src/main/java/com/apex/model/AuditLog.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Append-only trail of every successful state-changing API call. */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_created", columnList = "created_at"),
    @Index(name = "idx_audit_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "user_id", columnDefinition = "UUID")
  private UUID userId;

  @Column(name = "user_email", length = 255)
  private String userEmail;

  @Column(nullable = false, length = 10)
  private String method;

  @Column(nullable = false, length = 500)
  private String path;

  /** First path segment after /api/v1, e.g. "orders". */
  @Column(length = 60)
  private String resource;

  @Column(name = "status_code", nullable = false)
  private Integer statusCode;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/Category.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_categories_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Category parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
  private List<Category> children;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(unique = true, nullable = false, length = 180)
  private String slug;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/Customer.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 255)
  private String email;

  @Column(length = 40)
  private String phone;

  @Column(name = "address_line", length = 255)
  private String addressLine;

  @Column(length = 100)
  private String city;

  @Column(length = 100)
  private String state;

  @Column(name = "postal_code", length = 20)
  private String postalCode;

  @Column(length = 100)
  private String country;

  @Column(length = 1000)
  private String notes;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/GoodsReceipt.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One delivery against a purchase order (goods received note). */
@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipt {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "receipt_number", unique = true, nullable = false, length = 40)
  private String receiptNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @Column(length = 500)
  private String notes;

  @Column(name = "received_by", columnDefinition = "UUID")
  private UUID receivedBy;

  @Builder.Default
  @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GoodsReceiptLine> lines = new ArrayList<>();

  @Column(name = "received_at", nullable = false, updatable = false)
  private LocalDateTime receivedAt;

  public void addLine(GoodsReceiptLine line) {
    lines.add(line);
    line.setReceipt(this);
  }

  @PrePersist
  protected void onCreate() {
    receivedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/GoodsReceiptLine.java`

```java
package com.apex.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "goods_receipt_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptLine {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "receipt_id", nullable = false)
  private GoodsReceipt receipt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "po_line_id", nullable = false)
  private PurchaseOrderLine poLine;

  @Column(nullable = false)
  private Integer quantity;
}
```

## `src/main/java/com/apex/model/MediaAsset.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id")
  private ProductVariant variant;

  @Column(nullable = false, length = 512)
  private String url;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Builder.Default
  @Column(name = "is_primary", nullable = false)
  private Boolean isPrimary = false;

  @Builder.Default
  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/Product.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(unique = true, nullable = false, length = 220)
  private String slug;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String description;

  @Column(length = 100)
  private String brand;

  @Column(name = "unit_of_measure", nullable = false, length = 50)
  private String unitOfMeasure;

  @Builder.Default
  @Column(name = "has_variants", nullable = false)
  private Boolean hasVariants = false;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProductVariant> variants;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/ProductBarcode.java`

```java
package com.apex.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_barcodes", indexes = {
    @Index(name = "idx_product_barcodes_barcode", columnList = "barcode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBarcode {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(unique = true, nullable = false, length = 128)
  private String barcode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private BarcodeType type;

  @Builder.Default
  @Column(name = "is_primary", nullable = false)
  private Boolean isPrimary = true;

  public enum BarcodeType {
    UPC_A, EAN_13, CODE_128, QR_CODE
  }
}
```

## `src/main/java/com/apex/model/ProductVariant.java`

```java
package com.apex.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product_variants", indexes = {
    @Index(name = "idx_product_variants_sku", columnList = "sku")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(unique = true, nullable = false, length = 100)
  private String sku;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "variant_attributes")
  private Map<String, Object> variantAttributes;

  @Column(name = "cost_price", precision = 12, scale = 2, nullable = false)
  private BigDecimal costPrice;

  @Column(name = "retail_price", precision = 12, scale = 2, nullable = false)
  private BigDecimal retailPrice;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column
  private Map<String, Object> dimensions;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column
  private Map<String, Object> weight;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Builder.Default
  @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProductBarcode> barcodes = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // --- Helper Methods for Child Collection Management ---
  public void addBarcode(ProductBarcode barcode) {
    barcodes.add(barcode);
    barcode.setVariant(this);
  }

  public void removeBarcode(ProductBarcode barcode) {
    barcodes.remove(barcode);
    barcode.setVariant(null);
  }
}
```

## `src/main/java/com/apex/model/PurchaseOrder.java`

```java
package com.apex.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

  public enum Status {
    DRAFT, ORDERED, PARTIALLY_RECEIVED, RECEIVED, CLOSED, CANCELLED
  }

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "po_number", unique = true, nullable = false, length = 40)
  private String poNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "supplier_id", nullable = false)
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 25)
  private Status status = Status.DRAFT;

  @Column(name = "expected_date")
  private LocalDate expectedDate;

  @Column(length = 1000)
  private String notes;

  @Builder.Default
  @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PurchaseOrderLine> lines = new ArrayList<>();

  @Column(name = "created_by", columnDefinition = "UUID")
  private UUID createdBy;

  @Column(name = "ordered_at")
  private LocalDateTime orderedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public void addLine(PurchaseOrderLine line) {
    lines.add(line);
    line.setPurchaseOrder(this);
  }

  public BigDecimal getTotalAmount() {
    return lines.stream().map(PurchaseOrderLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/PurchaseOrderLine.java`

```java
package com.apex.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_order_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderLine {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(name = "quantity_ordered", nullable = false)
  private Integer quantityOrdered;

  @Builder.Default
  @Column(name = "quantity_received", nullable = false)
  private Integer quantityReceived = 0;

  @Column(name = "unit_cost", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitCost;

  public int getOutstanding() {
    return quantityOrdered - quantityReceived;
  }

  public BigDecimal getLineTotal() {
    return unitCost.multiply(BigDecimal.valueOf(quantityOrdered));
  }
}
```

## `src/main/java/com/apex/model/RefreshToken.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_hash", columnList = "token_hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, length = 512)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Builder.Default
  @Column(nullable = false)
  private Boolean revoked = false;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/ReorderRule.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Raise an alert when available stock of a variant in a warehouse falls to the reorder point. */
@Entity
@Table(name = "reorder_rules", uniqueConstraints = {
    @UniqueConstraint(name = "uk_reorder_variant_warehouse", columnNames = { "variant_id", "warehouse_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderRule {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Column(name = "reorder_point", nullable = false)
  private Integer reorderPoint;

  @Column(name = "reorder_quantity", nullable = false)
  private Integer reorderQuantity;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/Role.java`

```java
package com.apex.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(unique = true, nullable = false, length = 100)
  private String name;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column
  private List<String> permissions;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/SalesOrder.java`

```java
package com.apex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sales_orders", indexes = {
    @Index(name = "idx_sales_orders_status", columnList = "status"),
    @Index(name = "idx_sales_orders_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrder {

  public enum Status {
    PENDING, PICKING, PACKED, SHIPPED, DELIVERED, CANCELLED
  }

  public enum Channel {
    IN_STORE, ONLINE, PHONE
  }

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "order_number", unique = true, nullable = false, length = 40)
  private String orderNumber;

  /** Null for anonymous walk-in sales. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private Customer customer;

  /** Warehouse that fulfils (and reserves stock for) the order. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Channel channel = Channel.IN_STORE;

  @Column(name = "shipping_address", length = 500)
  private String shippingAddress;

  @Column(length = 1000)
  private String notes;

  @Builder.Default
  @Column(name = "tax_rate", precision = 6, scale = 4, nullable = false)
  private BigDecimal taxRate = BigDecimal.ZERO;

  @Builder.Default
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SalesOrderLine> lines = new ArrayList<>();

  @Column(name = "tracking_number", length = 100)
  private String trackingNumber;

  @Column(length = 100)
  private String carrier;

  @Column(name = "created_by", columnDefinition = "UUID")
  private UUID createdBy;

  @Column(name = "shipped_at")
  private LocalDateTime shippedAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public void addLine(SalesOrderLine line) {
    lines.add(line);
    line.setOrder(this);
  }

  public BigDecimal getSubtotal() {
    return lines.stream().map(SalesOrderLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public BigDecimal getTaxAmount() {
    return getSubtotal().multiply(taxRate).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  public BigDecimal getTotal() {
    return getSubtotal().add(getTaxAmount());
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/SalesOrderLine.java`

```java
package com.apex.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sales_order_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderLine {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private SalesOrder order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitPrice;

  /** Cost price at the time of sale, for margin reporting. */
  @Column(name = "unit_cost", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitCost;

  @Builder.Default
  @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
  private BigDecimal discountPercent = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "quantity_picked", nullable = false)
  private Integer quantityPicked = 0;

  @Builder.Default
  @Column(name = "quantity_returned", nullable = false)
  private Integer quantityReturned = 0;

  public BigDecimal getLineTotal() {
    BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
    BigDecimal factor = BigDecimal.ONE.subtract(discountPercent.divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP));
    return gross.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
  }
}
```

## `src/main/java/com/apex/model/SalesReturn.java`

```java
package com.apex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sales_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturn {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "return_number", unique = true, nullable = false, length = 40)
  private String returnNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private SalesOrder order;

  @Column(length = 500)
  private String reason;

  /** Whether returned goods went back into sellable stock (false = written off). */
  @Column(nullable = false)
  private boolean restocked;

  @Column(name = "refund_amount", precision = 12, scale = 2, nullable = false)
  private BigDecimal refundAmount;

  @Builder.Default
  @OneToMany(mappedBy = "salesReturn", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SalesReturnLine> lines = new ArrayList<>();

  @Column(name = "created_by", columnDefinition = "UUID")
  private UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public void addLine(SalesReturnLine line) {
    lines.add(line);
    line.setSalesReturn(this);
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/SalesReturnLine.java`

```java
package com.apex.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sales_return_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnLine {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "return_id", nullable = false)
  private SalesReturn salesReturn;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_line_id", nullable = false)
  private SalesOrderLine orderLine;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "refund_amount", precision = 12, scale = 2, nullable = false)
  private BigDecimal refundAmount;
}
```

## `src/main/java/com/apex/model/StockAlert.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_alerts", indexes = {
    @Index(name = "idx_alerts_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlert {

  public enum Type {
    LOW_STOCK, OUT_OF_STOCK
  }

  public enum Status {
    OPEN, ACKNOWLEDGED, RESOLVED
  }

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Type type;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.OPEN;

  @Column(name = "available_quantity", nullable = false)
  private Integer availableQuantity;

  @Column(name = "reorder_point", nullable = false)
  private Integer reorderPoint;

  @Column(name = "acknowledged_by", columnDefinition = "UUID")
  private UUID acknowledgedBy;

  @Column(name = "acknowledged_at")
  private LocalDateTime acknowledgedAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/StockLevel.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Current stock of one variant in one warehouse. */
@Entity
@Table(name = "stock_levels", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stock_variant_warehouse", columnNames = { "variant_id", "warehouse_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLevel {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_id")
  private StorageLocation location;

  @Builder.Default
  @Column(name = "quantity_on_hand", nullable = false)
  private Integer quantityOnHand = 0;

  /** Quantity promised to open orders / outgoing transfers; still physically in the warehouse. */
  @Builder.Default
  @Column(name = "quantity_reserved", nullable = false)
  private Integer quantityReserved = 0;

  @Version
  private Long version;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public int getAvailable() {
    return quantityOnHand - quantityReserved;
  }

  @PrePersist
  @PreUpdate
  protected void touch() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/StockMovement.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable ledger entry: every change of quantity on hand is recorded here. */
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_movements_variant", columnList = "variant_id"),
    @Index(name = "idx_movements_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_movements_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

  public enum MovementType {
    INITIAL, PURCHASE_RECEIPT, ADJUSTMENT, STOCK_COUNT, DAMAGE, TRANSFER_OUT, TRANSFER_IN, SALE, RETURN
  }

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MovementType type;

  /** Signed change of quantity on hand. */
  @Column(name = "quantity_change", nullable = false)
  private Integer quantityChange;

  @Column(name = "quantity_after", nullable = false)
  private Integer quantityAfter;

  @Column(name = "reference_type", length = 40)
  private String referenceType;

  @Column(name = "reference_id", columnDefinition = "UUID")
  private UUID referenceId;

  @Column(length = 500)
  private String reason;

  @Column(name = "performed_by", columnDefinition = "UUID")
  private UUID performedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/StockTransfer.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

  public enum Status {
    REQUESTED, IN_TRANSIT, RECEIVED, CANCELLED
  }

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "reference_number", unique = true, nullable = false, length = 40)
  private String referenceNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "from_warehouse_id", nullable = false)
  private Warehouse fromWarehouse;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "to_warehouse_id", nullable = false)
  private Warehouse toWarehouse;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.REQUESTED;

  @Column(length = 500)
  private String notes;

  @Builder.Default
  @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<StockTransferLine> lines = new ArrayList<>();

  @Column(name = "requested_by", columnDefinition = "UUID")
  private UUID requestedBy;

  @Column(name = "dispatched_at")
  private LocalDateTime dispatchedAt;

  @Column(name = "received_at")
  private LocalDateTime receivedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public void addLine(StockTransferLine line) {
    lines.add(line);
    line.setTransfer(this);
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/StockTransferLine.java`

```java
package com.apex.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_transfer_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferLine {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transfer_id", nullable = false)
  private StockTransfer transfer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "received_quantity")
  private Integer receivedQuantity;
}
```

## `src/main/java/com/apex/model/StorageLocation.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A bin / shelf / zone inside a warehouse. */
@Entity
@Table(name = "storage_locations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_location_warehouse_code", columnNames = { "warehouse_id", "code" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageLocation {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Column(nullable = false, length = 50)
  private String code;

  @Column(length = 255)
  private String description;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/Supplier.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(unique = true, nullable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "contact_name", length = 150)
  private String contactName;

  @Column(length = 255)
  private String email;

  @Column(length = 40)
  private String phone;

  @Column(name = "address_line", length = 255)
  private String addressLine;

  @Column(length = 100)
  private String city;

  @Column(length = 100)
  private String country;

  @Column(name = "payment_terms", length = 100)
  private String paymentTerms;

  @Column(name = "lead_time_days")
  private Integer leadTimeDays;

  @Column(length = 1000)
  private String notes;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/SupplierProduct.java`

```java
package com.apex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A variant that a supplier can deliver, with the agreed unit cost. */
@Entity
@Table(name = "supplier_products", uniqueConstraints = {
    @UniqueConstraint(name = "uk_supplier_variant", columnNames = { "supplier_id", "variant_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierProduct {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "supplier_id", nullable = false)
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(name = "supplier_sku", length = 100)
  private String supplierSku;

  @Column(name = "unit_cost", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitCost;

  @Builder.Default
  @Column(name = "min_order_quantity", nullable = false)
  private Integer minOrderQuantity = 1;

  @Builder.Default
  @Column(name = "is_preferred", nullable = false)
  private Boolean isPreferred = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/User.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(unique = true, nullable = false, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Builder.Default
  @Column(name = "mfa_enabled", nullable = false)
  private Boolean mfaEnabled = false;

  @Column(name = "mfa_secret")
  private String mfaSecret;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/model/Warehouse.java`

```java
package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouses", indexes = {
    @Index(name = "idx_warehouses_code", columnList = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(unique = true, nullable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "address_line", length = 255)
  private String addressLine;

  @Column(length = 100)
  private String city;

  @Column(length = 100)
  private String state;

  @Column(name = "postal_code", length = 20)
  private String postalCode;

  @Column(length = 100)
  private String country;

  @Column(name = "contact_phone", length = 40)
  private String contactPhone;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

## `src/main/java/com/apex/repository/AuditLogRepository.java`

```java
package com.apex.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.apex.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
}
```

## `src/main/java/com/apex/repository/CategoryRepository.java`

```java
package com.apex.repository;

import com.apex.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

  Optional<Category> findBySlug(String slug);

  boolean existsBySlug(String slug);

  List<Category> findByParentId(UUID parentId);

  @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true")
  List<Category> findAllRootCategories();
}
```

## `src/main/java/com/apex/repository/CustomerRepository.java`

```java
package com.apex.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.apex.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

  boolean existsByEmailIgnoreCase(String email);
}
```

## `src/main/java/com/apex/repository/GoodsReceiptRepository.java`

```java
package com.apex.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.GoodsReceipt;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

  List<GoodsReceipt> findByPurchaseOrderIdOrderByReceivedAtAsc(UUID purchaseOrderId);
}
```

## `src/main/java/com/apex/repository/MediaAssetRepository.java`

```java
package com.apex.repository;

import com.apex.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

  List<MediaAsset> findByProductIdOrderBySortOrderAsc(UUID productId);

  List<MediaAsset> findByVariantIdOrderBySortOrderAsc(UUID variantId);
}
```

## `src/main/java/com/apex/repository/ProductBarcodeRepository.java`

```java
package com.apex.repository;

import com.apex.model.ProductBarcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, UUID> {

  Optional<ProductBarcode> findByBarcode(String barcode);

  boolean existsByBarcode(String barcode);

  List<ProductBarcode> findByVariantId(UUID variantId);

  @Query("SELECT b FROM ProductBarcode b JOIN FETCH b.variant v JOIN FETCH v.product WHERE b.barcode = :barcode")
  Optional<ProductBarcode> findBarcodeWithProductDetails(@Param("barcode") String barcode);
}
```

## `src/main/java/com/apex/repository/ProductRepository.java`

```java
package com.apex.repository;

import com.apex.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

  boolean existsBySlug(String slug);

  long countByDeletedAtIsNullAndIsActiveTrue();

  /** Includes soft-deleted products, which still reference the category through a foreign key. */
  boolean existsByCategoryId(UUID categoryId);

  Optional<Product> findBySlugAndDeletedAtIsNull(String slug);

  boolean existsBySlugAndDeletedAtIsNull(String slug);

  Page<Product> findByCategoryIdAndDeletedAtIsNull(UUID categoryId, Pageable pageable);

}
```

## `src/main/java/com/apex/repository/ProductVariantRepository.java`

```java
package com.apex.repository;

import com.apex.model.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

  Optional<ProductVariant> findBySku(String sku);

  boolean existsBySku(String sku);

  List<ProductVariant> findByProductIdAndIsActiveTrue(UUID productId);

  Page<ProductVariant> findByProductId(UUID productId, Pageable pageable);
}
```

## `src/main/java/com/apex/repository/PurchaseOrderRepository.java`

```java
package com.apex.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.apex.model.PurchaseOrder;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {
}
```

## `src/main/java/com/apex/repository/RefreshTokenRepository.java`

```java
package com.apex.repository;

import com.apex.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
  void revokeAllByUserId(@Param("userId") UUID userId);

  @Modifying
  @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revoked = true")
  void deleteAllExpiredOrRevoked(@Param("now") LocalDateTime now);
}
```

## `src/main/java/com/apex/repository/ReorderRuleRepository.java`

```java
package com.apex.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.ReorderRule;

public interface ReorderRuleRepository extends JpaRepository<ReorderRule, UUID> {

  Optional<ReorderRule> findByVariantIdAndWarehouseId(UUID variantId, UUID warehouseId);

  List<ReorderRule> findByIsActiveTrue();

  List<ReorderRule> findByWarehouseId(UUID warehouseId);
}
```

## `src/main/java/com/apex/repository/RoleRepository.java`

```java
package com.apex.repository;

import com.apex.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

  Optional<Role> findByName(String name);

  boolean existsByName(String name);
}
```

## `src/main/java/com/apex/repository/SalesOrderRepository.java`

```java
package com.apex.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.apex.model.SalesOrder;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID>, JpaSpecificationExecutor<SalesOrder> {

  long countByCustomerId(UUID customerId);
}
```

## `src/main/java/com/apex/repository/SalesReturnRepository.java`

```java
package com.apex.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.SalesReturn;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, UUID> {

  List<SalesReturn> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
```

## `src/main/java/com/apex/repository/StockAlertRepository.java`

```java
package com.apex.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.StockAlert;
import com.apex.model.StockAlert.Status;

public interface StockAlertRepository extends JpaRepository<StockAlert, UUID> {

  List<StockAlert> findByVariantIdAndWarehouseIdAndStatusIn(UUID variantId, UUID warehouseId, Collection<Status> statuses);

  Page<StockAlert> findByStatusIn(Collection<Status> statuses, Pageable pageable);

  Page<StockAlert> findByStatusInAndWarehouseId(Collection<Status> statuses, UUID warehouseId, Pageable pageable);

  long countByStatus(Status status);
}
```

## `src/main/java/com/apex/repository/StockLevelRepository.java`

```java
package com.apex.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apex.model.StockLevel;

import jakarta.persistence.LockModeType;

public interface StockLevelRepository extends JpaRepository<StockLevel, UUID>, JpaSpecificationExecutor<StockLevel> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM StockLevel s WHERE s.variant.id = :variantId AND s.warehouse.id = :warehouseId")
  Optional<StockLevel> findForUpdate(@Param("variantId") UUID variantId, @Param("warehouseId") UUID warehouseId);

  Optional<StockLevel> findByVariantIdAndWarehouseId(UUID variantId, UUID warehouseId);

  @Query("SELECT s FROM StockLevel s JOIN FETCH s.warehouse WHERE s.variant.id = :variantId ORDER BY s.warehouse.code")
  List<StockLevel> findByVariant(@Param("variantId") UUID variantId);

  boolean existsByWarehouseIdAndQuantityOnHandGreaterThan(UUID warehouseId, int quantity);

  boolean existsByVariantIdAndQuantityOnHandGreaterThan(UUID variantId, int quantity);

  @Query("SELECT COALESCE(SUM(s.quantityOnHand), 0) FROM StockLevel s WHERE s.variant.id = :variantId")
  long totalOnHand(@Param("variantId") UUID variantId);

  @Query("SELECT COALESCE(SUM(s.quantityOnHand - s.quantityReserved), 0) FROM StockLevel s WHERE s.variant.id = :variantId")
  long totalAvailable(@Param("variantId") UUID variantId);

  Page<StockLevel> findByWarehouseId(UUID warehouseId, Pageable pageable);

  @Query("SELECT s FROM StockLevel s JOIN FETCH s.variant v JOIN FETCH v.product JOIN FETCH s.warehouse "
      + "WHERE s.quantityOnHand > 0 ORDER BY v.sku")
  List<StockLevel> findAllInStock();

  @Query("SELECT s FROM StockLevel s JOIN FETCH s.variant v JOIN FETCH v.product JOIN FETCH s.warehouse w "
      + "WHERE s.quantityOnHand > 0 AND w.id = :warehouseId ORDER BY v.sku")
  List<StockLevel> findInStockByWarehouse(@Param("warehouseId") UUID warehouseId);

  @Query("SELECT s FROM StockLevel s JOIN FETCH s.variant v JOIN FETCH v.product JOIN FETCH s.warehouse w "
      + "WHERE (s.quantityOnHand - s.quantityReserved) <= :threshold AND v.isActive = true ORDER BY (s.quantityOnHand - s.quantityReserved), v.sku")
  List<StockLevel> findAtOrBelowAvailable(@Param("threshold") int threshold);

  @Query("SELECT COALESCE(SUM(s.quantityOnHand), 0) FROM StockLevel s")
  long totalUnitsOnHand();

  @Query("SELECT COALESCE(SUM(s.quantityOnHand * v.costPrice), 0) FROM StockLevel s JOIN s.variant v")
  java.math.BigDecimal totalCostValue();
}
```

## `src/main/java/com/apex/repository/StockMovementRepository.java`

```java
package com.apex.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.apex.model.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>, JpaSpecificationExecutor<StockMovement> {
}
```

## `src/main/java/com/apex/repository/StockTransferRepository.java`

```java
package com.apex.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.apex.model.StockTransfer;

public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID>, JpaSpecificationExecutor<StockTransfer> {
}
```

## `src/main/java/com/apex/repository/StorageLocationRepository.java`

```java
package com.apex.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.StorageLocation;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, UUID> {

  boolean existsByWarehouseIdAndCode(UUID warehouseId, String code);

  List<StorageLocation> findByWarehouseIdOrderByCodeAsc(UUID warehouseId);
}
```

## `src/main/java/com/apex/repository/SupplierProductRepository.java`

```java
package com.apex.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apex.model.SupplierProduct;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, UUID> {

  boolean existsBySupplierIdAndVariantId(UUID supplierId, UUID variantId);

  Optional<SupplierProduct> findBySupplierIdAndVariantId(UUID supplierId, UUID variantId);

  @Query("SELECT sp FROM SupplierProduct sp JOIN FETCH sp.variant v JOIN FETCH v.product WHERE sp.supplier.id = :supplierId ORDER BY v.sku")
  List<SupplierProduct> findBySupplier(@Param("supplierId") UUID supplierId);

  @Query("SELECT sp FROM SupplierProduct sp JOIN FETCH sp.supplier s WHERE sp.variant.id = :variantId AND s.isActive = true ORDER BY sp.isPreferred DESC, sp.unitCost ASC")
  List<SupplierProduct> findActiveByVariant(@Param("variantId") UUID variantId);
}
```

## `src/main/java/com/apex/repository/SupplierRepository.java`

```java
package com.apex.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.apex.model.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, UUID>, JpaSpecificationExecutor<Supplier> {

  boolean existsByCode(String code);
}
```

## `src/main/java/com/apex/repository/UserRepository.java`

```java
package com.apex.repository;

import com.apex.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

  long countByRoleIdAndDeletedAtIsNull(UUID roleId);

  @Query(value = "SELECT u FROM User u JOIN FETCH u.role WHERE u.deletedAt IS NULL", countQuery = "SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
  Page<User> findAllActiveWithRole(Pageable pageable);

  Optional<User> findByEmailAndDeletedAtIsNull(String email);

  boolean existsByEmailAndDeletedAtIsNull(String email);

  Page<User> findAllByDeletedAtIsNull(Pageable pageable);

  @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id = :id AND u.deletedAt IS NULL")
  Optional<User> findByIdWithRole(@Param("id") UUID id);
}
```

## `src/main/java/com/apex/repository/WarehouseRepository.java`

```java
package com.apex.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.Warehouse;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

  boolean existsByCode(String code);

  long countByIsActiveTrue();

  Optional<Warehouse> findByCode(String code);

  Page<Warehouse> findByIsActive(Boolean isActive, Pageable pageable);
}
```

## `src/main/java/com/apex/security/CurrentUser.java`

```java
package com.apex.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

  private CurrentUser() {
  }

  /** Id of the authenticated user (the JWT subject), if the request is authenticated. */
  public static Optional<UUID> id() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(auth.getName()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
```

## `src/main/java/com/apex/security/Permissions.java`

```java
package com.apex.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Catalogue of every permission the API understands, and the built-in role bundles. */
public final class Permissions {

  private Permissions() {
  }

  private static List<String> crud(String resource) {
    return List.of(resource + ":create", resource + ":read", resource + ":update", resource + ":delete");
  }

  private static final List<String> ALL;

  static {
    List<String> all = new ArrayList<>();
    all.addAll(crud("user"));
    all.addAll(List.of("role:read", "role:manage"));
    all.addAll(crud("category"));
    all.addAll(crud("product"));
    all.addAll(crud("warehouse"));
    all.addAll(List.of("inventory:read", "inventory:adjust"));
    all.addAll(List.of("transfer:create", "transfer:read", "transfer:update"));
    all.addAll(crud("supplier"));
    all.addAll(List.of("purchase:create", "purchase:read", "purchase:update", "purchase:receive", "purchase:cancel"));
    all.addAll(crud("customer"));
    all.addAll(List.of("order:create", "order:read", "order:update", "order:cancel"));
    all.addAll(List.of("alert:read", "alert:manage"));
    all.addAll(List.of("report:read", "audit:read"));
    ALL = List.copyOf(all);
  }

  public static List<String> all() {
    return ALL;
  }

  public static boolean isValid(String permission) {
    return ALL.contains(permission);
  }

  /** Returns the permissions that are not part of the catalogue. */
  public static Set<String> unknown(Iterable<String> permissions) {
    Set<String> unknown = new TreeSet<>();
    for (String p : permissions) {
      if (!isValid(p)) {
        unknown.add(p);
      }
    }
    return unknown;
  }

  public static List<String> admin() {
    return ALL;
  }

  public static List<String> manager() {
    return List.of(
        "category:create", "category:read", "category:update",
        "product:create", "product:read", "product:update",
        "warehouse:read",
        "inventory:read", "inventory:adjust",
        "transfer:create", "transfer:read", "transfer:update",
        "supplier:create", "supplier:read", "supplier:update",
        "purchase:create", "purchase:read", "purchase:update", "purchase:receive", "purchase:cancel",
        "customer:create", "customer:read", "customer:update",
        "order:create", "order:read", "order:update", "order:cancel",
        "alert:read", "alert:manage",
        "report:read");
  }

  public static List<String> warehouseStaff() {
    return List.of(
        "category:read", "product:read",
        "warehouse:read",
        "inventory:read", "inventory:adjust",
        "transfer:create", "transfer:read", "transfer:update",
        "purchase:read", "purchase:receive",
        "order:read", "order:update",
        "alert:read");
  }

  public static List<String> cashier() {
    return List.of(
        "category:read", "product:read",
        "inventory:read",
        "customer:create", "customer:read",
        "order:create", "order:read");
  }
}
```

## `src/main/java/com/apex/service/AlertService.java`

```java
package com.apex.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.AlertRequests.ReorderRuleRequest;
import com.apex.dto.response.AlertResponses.AlertResponse;
import com.apex.dto.response.AlertResponses.AlertSummary;
import com.apex.dto.response.AlertResponses.ReorderRuleResponse;
import com.apex.dto.response.AlertResponses.ReplenishmentSuggestion;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.ProductVariant;
import com.apex.model.ReorderRule;
import com.apex.model.StockAlert;
import com.apex.model.StockAlert.Status;
import com.apex.model.StockAlert.Type;
import com.apex.model.StockLevel;
import com.apex.model.Warehouse;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.ReorderRuleRepository;
import com.apex.repository.StockAlertRepository;
import com.apex.repository.StockLevelRepository;
import com.apex.repository.SupplierProductRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Reorder rules and low-stock alerts. {@link #evaluate} is called by the inventory service after every
 * change of stock, inside the same transaction, so alerts always reflect committed quantities.
 */
@Service
@RequiredArgsConstructor
public class AlertService {

  private static final Logger log = LoggerFactory.getLogger(AlertService.class);
  private static final EnumSet<Status> ACTIVE = EnumSet.of(Status.OPEN, Status.ACKNOWLEDGED);

  private final ReorderRuleRepository ruleRepository;
  private final StockAlertRepository alertRepository;
  private final StockLevelRepository stockLevelRepository;
  private final ProductVariantRepository variantRepository;
  private final WarehouseRepository warehouseRepository;
  private final SupplierProductRepository supplierProductRepository;

  // ---------- Evaluation ----------

  @Transactional
  public void evaluate(UUID variantId, UUID warehouseId) {
    ReorderRule rule = ruleRepository.findByVariantIdAndWarehouseId(variantId, warehouseId).orElse(null);
    List<StockAlert> active = alertRepository.findByVariantIdAndWarehouseIdAndStatusIn(variantId, warehouseId, ACTIVE);

    if (rule == null || !Boolean.TRUE.equals(rule.getIsActive())) {
      resolve(active);
      return;
    }
    int available = stockLevelRepository.findByVariantIdAndWarehouseId(variantId, warehouseId)
        .map(StockLevel::getAvailable).orElse(0);

    if (available > rule.getReorderPoint()) {
      resolve(active);
      return;
    }
    Type type = available <= 0 ? Type.OUT_OF_STOCK : Type.LOW_STOCK;
    if (active.isEmpty()) {
      alertRepository.save(StockAlert.builder()
          .variant(rule.getVariant()).warehouse(rule.getWarehouse()).type(type)
          .availableQuantity(available).reorderPoint(rule.getReorderPoint()).build());
      log.warn("Stock alert {}: SKU {} in {} has {} available (reorder point {})", type,
          rule.getVariant().getSku(), rule.getWarehouse().getCode(), available, rule.getReorderPoint());
    } else {
      active.forEach(a -> {
        a.setType(type);
        a.setAvailableQuantity(available);
        a.setReorderPoint(rule.getReorderPoint());
      });
      alertRepository.saveAll(active);
    }
  }

  private void resolve(List<StockAlert> active) {
    if (active.isEmpty()) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    active.forEach(a -> {
      a.setStatus(Status.RESOLVED);
      a.setResolvedAt(now);
    });
    alertRepository.saveAll(active);
  }

  // ---------- Rules ----------

  @Transactional
  public ReorderRuleResponse createRule(ReorderRuleRequest r) {
    if (ruleRepository.findByVariantIdAndWarehouseId(r.variantId(), r.warehouseId()).isPresent()) {
      throw new DuplicateResourceException("A reorder rule already exists for this SKU and warehouse");
    }
    ProductVariant variant = variantRepository.findById(r.variantId())
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + r.variantId()));
    Warehouse warehouse = warehouseRepository.findById(r.warehouseId())
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + r.warehouseId()));
    ReorderRule rule = ruleRepository.save(ReorderRule.builder()
        .variant(variant).warehouse(warehouse).reorderPoint(r.reorderPoint())
        .reorderQuantity(r.reorderQuantity()).isActive(r.isActive() == null || r.isActive()).build());
    ruleRepository.flush();
    evaluate(variant.getId(), warehouse.getId());
    return toResponse(rule);
  }

  @Transactional
  public ReorderRuleResponse updateRule(UUID id, ReorderRuleRequest r) {
    ReorderRule rule = findRule(id);
    rule.setReorderPoint(r.reorderPoint());
    rule.setReorderQuantity(r.reorderQuantity());
    if (r.isActive() != null) {
      rule.setIsActive(r.isActive());
    }
    ruleRepository.saveAndFlush(rule);
    evaluate(rule.getVariant().getId(), rule.getWarehouse().getId());
    return toResponse(rule);
  }

  @Transactional
  public void deleteRule(UUID id) {
    ReorderRule rule = findRule(id);
    ruleRepository.delete(rule);
    ruleRepository.flush();
    evaluate(rule.getVariant().getId(), rule.getWarehouse().getId());
  }

  @Transactional(readOnly = true)
  public List<ReorderRuleResponse> listRules(UUID warehouseId) {
    List<ReorderRule> rules = warehouseId == null ? ruleRepository.findAll() : ruleRepository.findByWarehouseId(warehouseId);
    return rules.stream().map(this::toResponse).toList();
  }

  // ---------- Alerts ----------

  @Transactional(readOnly = true)
  public Page<AlertResponse> listAlerts(Status status, UUID warehouseId, Pageable pageable) {
    var statuses = status == null ? ACTIVE : EnumSet.of(status);
    Page<StockAlert> page = warehouseId == null
        ? alertRepository.findByStatusIn(statuses, pageable)
        : alertRepository.findByStatusInAndWarehouseId(statuses, warehouseId, pageable);
    return page.map(this::toResponse);
  }

  @Transactional
  public AlertResponse acknowledge(UUID id) {
    StockAlert alert = alertRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));
    if (alert.getStatus() == Status.RESOLVED) {
      throw new IllegalStateException("Alert is already resolved");
    }
    alert.setStatus(Status.ACKNOWLEDGED);
    alert.setAcknowledgedAt(LocalDateTime.now());
    alert.setAcknowledgedBy(CurrentUser.id().orElse(null));
    return toResponse(alertRepository.save(alert));
  }

  @Transactional(readOnly = true)
  public AlertSummary summary() {
    return new AlertSummary(alertRepository.countByStatus(Status.OPEN),
        alertRepository.countByStatus(Status.ACKNOWLEDGED), alertRepository.countByStatus(Status.RESOLVED));
  }

  /** Items at or below their reorder point, with the quantity to order and the cheapest preferred supplier. */
  @Transactional(readOnly = true)
  public List<ReplenishmentSuggestion> suggestions(UUID warehouseId) {
    return ruleRepository.findByIsActiveTrue().stream()
        .filter(r -> warehouseId == null || r.getWarehouse().getId().equals(warehouseId))
        .map(r -> {
          int available = stockLevelRepository
              .findByVariantIdAndWarehouseId(r.getVariant().getId(), r.getWarehouse().getId())
              .map(StockLevel::getAvailable).orElse(0);
          return new Object[] { r, available };
        })
        .filter(pair -> (int) pair[1] <= ((ReorderRule) pair[0]).getReorderPoint())
        .map(pair -> {
          ReorderRule r = (ReorderRule) pair[0];
          var supplier = supplierProductRepository.findActiveByVariant(r.getVariant().getId()).stream().findFirst();
          return new ReplenishmentSuggestion(r.getVariant().getId(), r.getVariant().getSku(),
              r.getVariant().getProduct().getName(), r.getWarehouse().getId(), r.getWarehouse().getCode(),
              (int) pair[1], r.getReorderPoint(), r.getReorderQuantity(),
              supplier.map(s -> s.getSupplier().getId()).orElse(null),
              supplier.map(s -> s.getSupplier().getName()).orElse(null),
              supplier.map(s -> s.getUnitCost()).orElse(r.getVariant().getCostPrice()));
        })
        .toList();
  }

  private ReorderRule findRule(UUID id) {
    return ruleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Reorder rule not found: " + id));
  }

  private ReorderRuleResponse toResponse(ReorderRule r) {
    return new ReorderRuleResponse(r.getId(), r.getVariant().getId(), r.getVariant().getSku(),
        r.getVariant().getProduct().getName(), r.getWarehouse().getId(), r.getWarehouse().getCode(),
        r.getReorderPoint(), r.getReorderQuantity(), r.getIsActive());
  }

  private AlertResponse toResponse(StockAlert a) {
    return new AlertResponse(a.getId(), a.getVariant().getId(), a.getVariant().getSku(),
        a.getVariant().getProduct().getName(), a.getWarehouse().getId(), a.getWarehouse().getCode(),
        a.getType().name(), a.getStatus().name(), a.getAvailableQuantity(), a.getReorderPoint(),
        a.getCreatedAt(), a.getAcknowledgedAt(), a.getResolvedAt());
  }
}
```

## `src/main/java/com/apex/service/AuthService.java`

```java
package com.apex.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.AuthRequests.LoginRequest;
import com.apex.dto.request.AuthRequests.RefreshTokenRequest;
import com.apex.dto.request.AuthRequests.RegisterRequest;
import com.apex.dto.response.AuthResponses.AuthResponse;
import com.apex.dto.response.AuthResponses.UserResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.exception.UnauthorizedException;
import com.apex.mapper.UserMapper;
import com.apex.model.RefreshToken;
import com.apex.model.Role;
import com.apex.model.User;
import com.apex.repository.RefreshTokenRepository;
import com.apex.repository.RoleRepository;
import com.apex.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final long REFRESH_TOKEN_TTL_DAYS = 7;

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserMapper userMapper;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
      throw new DuplicateResourceException("A user with this email already exists");
    }

    Role role = roleRepository.findById(request.roleId())
        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.roleId()));

    User user = User.builder()
        .firstName(request.firstName())
        .lastName(request.lastName())
        .email(email)
        .passwordHash(passwordEncoder.encode(request.password()))
        .role(role)
        .build();

    user = userRepository.save(user);
    return issueTokens(user);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(request.email()))
        .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

    if (!Boolean.TRUE.equals(user.getIsActive())) {
      throw new UnauthorizedException("Account is deactivated");
    }

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new UnauthorizedException("Invalid email or password");
    }

    return issueTokens(user);
  }

  @Transactional
  public AuthResponse refresh(RefreshTokenRequest request) {
    RefreshToken stored = refreshTokenRepository
        .findByTokenHashAndRevokedFalse(sha256(request.refreshToken()))
        .orElseThrow(() -> new UnauthorizedException("Invalid or revoked refresh token"));

    if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new UnauthorizedException("Refresh token has expired");
    }

    // Rotate the refresh token
    stored.setRevoked(true);
    refreshTokenRepository.save(stored);

    User user = userRepository.findByIdWithRole(stored.getUser().getId())
        .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

    if (!Boolean.TRUE.equals(user.getIsActive())) {
      throw new UnauthorizedException("Account is deactivated");
    }

    return issueTokens(user);
  }

  @Transactional
  public void logout(RefreshTokenRequest request) {
    refreshTokenRepository.findByTokenHashAndRevokedFalse(sha256(request.refreshToken()))
        .ifPresent(token -> {
          token.setRevoked(true);
          refreshTokenRepository.save(token);
        });
  }

  public UserResponse getUserById(UUID id) {
    User user = userRepository.findByIdWithRole(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    return userMapper.toResponse(user);
  }

  private AuthResponse issueTokens(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = generateOpaqueToken();

    RefreshToken token = RefreshToken.builder()
        .user(user)
        .tokenHash(sha256(refreshToken))
        .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS))
        .build();
    refreshTokenRepository.save(token);

    return new AuthResponse(accessToken, refreshToken, jwtService.getTtlSeconds(),
        userMapper.toResponse(user));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(java.util.Locale.ROOT);
  }

  private String generateOpaqueToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
```

## `src/main/java/com/apex/service/CategoryService.java`

```java
package com.apex.service;

import com.apex.dto.request.CategoryRequests.CreateCategoryRequest;
import com.apex.dto.request.CategoryRequests.UpdateCategoryRequest;
import com.apex.dto.response.CategoryResponse;
import com.apex.exception.ResourceNotFoundException;
import com.apex.mapper.CategoryMapper;
import com.apex.model.Category;
import com.apex.repository.CategoryRepository;
import com.apex.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;
  private final ProductRepository productRepository;

  @Transactional
  public CategoryResponse createCategory(CreateCategoryRequest request) {
    String slug = generateUniqueSlug(request.name());

    Category parent = null;
    if (request.parentId() != null) {
      parent = categoryRepository.findById(request.parentId())
          .orElseThrow(
              () -> new ResourceNotFoundException("Parent category not found: " + request.parentId()));
    }

    Category category = Category.builder()
        .name(request.name())
        .slug(slug)
        .description(request.description())
        .parent(parent)
        .isActive(true)
        .build();

    return categoryMapper.toResponse(categoryRepository.save(category));
  }

  @Transactional
  public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

    category.setName(request.name());
    if (request.description() != null) {
      category.setDescription(request.description());
    }
    if (request.isActive() != null) {
      category.setIsActive(request.isActive());
    }

    return categoryMapper.toResponse(categoryRepository.save(category));
  }

  @Transactional
  public CategoryResponse moveCategory(UUID id, UUID newParentId) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

    Category newParent = null;
    if (newParentId != null) {
      newParent = categoryRepository.findById(newParentId)
          .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + newParentId));
      for (Category node = newParent; node != null; node = node.getParent()) {
        if (node.getId().equals(id)) {
          throw new IllegalArgumentException("A category cannot be moved under itself or one of its descendants");
        }
      }
    }
    category.setParent(newParent);
    return categoryMapper.toResponse(categoryRepository.save(category));
  }

  @Transactional(readOnly = true)
  public CategoryResponse getCategory(UUID id) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    return categoryMapper.toResponse(category);
  }

  @Transactional(readOnly = true)
  public CategoryResponse getCategoryBySlug(String slug) {
    Category category = categoryRepository.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
    return categoryMapper.toResponse(category);
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> getCategoryTree() {
    return categoryRepository.findAllRootCategories().stream()
        .map(categoryMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> getSubcategories(UUID parentId) {
    return categoryRepository.findByParentId(parentId).stream()
        .map(categoryMapper::toResponse)
        .toList();
  }

  @Transactional
  public void deleteCategory(UUID id) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

    if (category.getChildren() != null && !category.getChildren().isEmpty()) {
      throw new IllegalStateException(
          "Cannot delete category with active subcategories. Delete or reassign subcategories first.");
    }
    if (productRepository.existsByCategoryId(id)) {
      throw new IllegalStateException("Cannot delete a category that has products (including archived ones). Deactivate it instead.");
    }
    categoryRepository.delete(category);
  }

  private String generateUniqueSlug(String name) {
    String base = slugify(name);
    String slug = base;
    int counter = 1;
    while (categoryRepository.existsBySlug(slug)) {
      slug = base + "-" + counter++;
    }
    return slug;
  }

  private String slugify(String input) {
    if (input == null)
      return "";
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return normalized.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\s-]", "")
        .trim()
        .replaceAll("[\\s-]+", "-");
  }
}
```

## `src/main/java/com/apex/service/CustomerService.java`

```java
package com.apex.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.CustomerRequests.CustomerRequest;
import com.apex.dto.response.OrderResponses.CustomerResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.Customer;
import com.apex.repository.CustomerRepository;
import com.apex.repository.SalesOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final SalesOrderRepository orderRepository;

  @Transactional
  public CustomerResponse create(CustomerRequest r) {
    if (r.email() != null && !r.email().isBlank() && customerRepository.existsByEmailIgnoreCase(r.email())) {
      throw new DuplicateResourceException("A customer with this email already exists");
    }
    Customer customer = Customer.builder().build();
    apply(customer, r);
    return toResponse(customerRepository.save(customer));
  }

  @Transactional(readOnly = true)
  public Page<CustomerResponse> list(String keyword, Pageable pageable) {
    Specification<Customer> spec = (root, query, cb) -> cb.conjunction();
    if (keyword != null && !keyword.isBlank()) {
      String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("name")), pattern),
          cb.like(cb.lower(root.get("email")), pattern),
          cb.like(cb.lower(root.get("phone")), pattern)));
    }
    return customerRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public CustomerResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public CustomerResponse update(UUID id, CustomerRequest r) {
    Customer customer = find(id);
    if (r.email() != null && !r.email().isBlank() && !r.email().equalsIgnoreCase(customer.getEmail())
        && customerRepository.existsByEmailIgnoreCase(r.email())) {
      throw new DuplicateResourceException("A customer with this email already exists");
    }
    apply(customer, r);
    return toResponse(customerRepository.save(customer));
  }

  /** Customers with order history are deactivated; the rest are removed. */
  @Transactional
  public void delete(UUID id) {
    Customer customer = find(id);
    if (orderRepository.countByCustomerId(id) > 0) {
      customer.setIsActive(false);
      customerRepository.save(customer);
    } else {
      customerRepository.delete(customer);
    }
  }

  private void apply(Customer c, CustomerRequest r) {
    c.setName(r.name());
    c.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim());
    c.setPhone(r.phone());
    c.setAddressLine(r.addressLine());
    c.setCity(r.city());
    c.setState(r.state());
    c.setPostalCode(r.postalCode());
    c.setCountry(r.country());
    c.setNotes(r.notes());
    if (r.isActive() != null) {
      c.setIsActive(r.isActive());
    }
  }

  private Customer find(UUID id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
  }

  private CustomerResponse toResponse(Customer c) {
    return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getAddressLine(),
        c.getCity(), c.getState(), c.getPostalCode(), c.getCountry(), c.getNotes(), c.getIsActive(),
        c.getCreatedAt());
  }
}
```

## `src/main/java/com/apex/service/FulfilmentService.java`

```java
package com.apex.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.OrderRequests.CreateReturnRequest;
import com.apex.dto.request.OrderRequests.ReturnLineRequest;
import com.apex.dto.request.OrderRequests.ShipOrderRequest;
import com.apex.dto.response.OrderResponses.OrderResponse;
import com.apex.dto.response.OrderResponses.ReturnLineResponse;
import com.apex.dto.response.OrderResponses.ReturnResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.model.SalesOrder;
import com.apex.model.SalesOrder.Status;
import com.apex.model.SalesOrderLine;
import com.apex.model.SalesReturn;
import com.apex.model.SalesReturnLine;
import com.apex.model.StockMovement.MovementType;
import com.apex.repository.SalesOrderRepository;
import com.apex.repository.SalesReturnRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Order fulfilment: PENDING -> PICKING -> PACKED -> SHIPPED -> DELIVERED.
 * Stock stays reserved until the order ships, then leaves the warehouse. Orders can be
 * cancelled until they ship; after that goods come back through a return.
 */
@Service
@RequiredArgsConstructor
public class FulfilmentService {

  private static final String REF = "SALES_ORDER";

  private final OrderService orderService;
  private final SalesOrderRepository orderRepository;
  private final SalesReturnRepository returnRepository;
  private final InventoryService inventoryService;

  /** Records picked quantities; with no lines given everything on the order is picked. */
  @Transactional
  public OrderResponse pick(UUID orderId, Map<UUID, Integer> picks) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "picked", Status.PENDING, Status.PICKING);

    if (picks == null || picks.isEmpty()) {
      order.getLines().forEach(l -> l.setQuantityPicked(l.getQuantity()));
    } else {
      picks.forEach((lineId, qty) -> {
        SalesOrderLine line = order.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Line does not belong to this order: " + lineId));
        if (qty < 0 || qty > line.getQuantity()) {
          throw new BusinessRuleException("Picked quantity for SKU " + line.getVariant().getSku()
              + " must be between 0 and " + line.getQuantity());
        }
        line.setQuantityPicked(qty);
      });
    }
    order.setStatus(Status.PICKING);
    return orderService.toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse pack(UUID orderId) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "packed", Status.PICKING);
    for (SalesOrderLine line : order.getLines()) {
      if (line.getQuantityPicked() < line.getQuantity()) {
        throw new BusinessRuleException("SKU " + line.getVariant().getSku() + " is not fully picked ("
            + line.getQuantityPicked() + "/" + line.getQuantity() + ")");
      }
    }
    order.setStatus(Status.PACKED);
    return orderService.toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse ship(UUID orderId, ShipOrderRequest request) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "shipped", Status.PACKED);
    for (SalesOrderLine line : order.getLines()) {
      inventoryService.consumeReserved(line.getVariant().getId(), order.getWarehouse().getId(),
          line.getQuantity(), MovementType.SALE, REF, order.getId(), order.getOrderNumber());
    }
    order.setStatus(Status.SHIPPED);
    order.setShippedAt(LocalDateTime.now());
    if (request != null) {
      order.setCarrier(request.carrier());
      order.setTrackingNumber(request.trackingNumber());
    }
    return orderService.toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse deliver(UUID orderId) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "marked delivered", Status.SHIPPED);
    order.setStatus(Status.DELIVERED);
    order.setDeliveredAt(LocalDateTime.now());
    return orderService.toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse cancel(UUID orderId, String reason) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "cancelled", Status.PENDING, Status.PICKING, Status.PACKED);
    for (SalesOrderLine line : order.getLines()) {
      inventoryService.release(line.getVariant().getId(), order.getWarehouse().getId(), line.getQuantity());
    }
    order.setStatus(Status.CANCELLED);
    order.setCancelledAt(LocalDateTime.now());
    if (reason != null && !reason.isBlank()) {
      String existing = order.getNotes() == null ? "" : order.getNotes() + "\n";
      order.setNotes(existing + "Cancelled: " + reason);
    }
    return orderService.toResponse(orderRepository.save(order));
  }

  // ---------- Returns ----------

  @Transactional
  public ReturnResponse createReturn(UUID orderId, CreateReturnRequest request) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "returned", Status.SHIPPED, Status.DELIVERED);
    boolean restock = request.restock() == null || request.restock();

    Map<UUID, Integer> quantities = new HashMap<>();
    for (ReturnLineRequest l : request.lines()) {
      quantities.merge(l.orderLineId(), l.quantity(), Integer::sum);
    }

    SalesReturn salesReturn = SalesReturn.builder()
        .returnNumber(ReferenceNumbers.next("RET"))
        .order(order)
        .reason(request.reason())
        .restocked(restock)
        .refundAmount(BigDecimal.ZERO)
        .createdBy(CurrentUser.id().orElse(null))
        .build();

    BigDecimal total = BigDecimal.ZERO;
    for (Map.Entry<UUID, Integer> e : quantities.entrySet()) {
      SalesOrderLine line = order.getLines().stream().filter(l -> l.getId().equals(e.getKey())).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Line does not belong to this order: " + e.getKey()));
      int qty = e.getValue();
      int returnable = line.getQuantity() - line.getQuantityReturned();
      if (qty > returnable) {
        throw new BusinessRuleException("Cannot return " + qty + " of SKU " + line.getVariant().getSku()
            + ": only " + returnable + " returnable");
      }
      BigDecimal net = line.getLineTotal()
          .multiply(BigDecimal.valueOf(qty))
          .divide(BigDecimal.valueOf(line.getQuantity()), 2, RoundingMode.HALF_UP);
      BigDecimal refund = net.add(net.multiply(order.getTaxRate())).setScale(2, RoundingMode.HALF_UP);
      line.setQuantityReturned(line.getQuantityReturned() + qty);
      salesReturn.addLine(SalesReturnLine.builder().orderLine(line).quantity(qty).refundAmount(refund).build());
      total = total.add(refund);

      if (restock) {
        inventoryService.apply(line.getVariant().getId(), order.getWarehouse().getId(), qty,
            MovementType.RETURN, "SALES_RETURN", order.getId(), request.reason());
      }
    }
    salesReturn.setRefundAmount(total);
    orderRepository.save(order);
    return toResponse(returnRepository.save(salesReturn));
  }

  @Transactional(readOnly = true)
  public List<ReturnResponse> listReturns(UUID orderId) {
    orderService.find(orderId);
    return returnRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream().map(this::toResponse).toList();
  }

  // ---------- helpers ----------

  private void requireStatus(SalesOrder order, String action, Status... allowed) {
    for (Status s : allowed) {
      if (order.getStatus() == s) {
        return;
      }
    }
    throw new IllegalStateException("Order " + order.getOrderNumber() + " is " + order.getStatus()
        + " and cannot be " + action);
  }

  private ReturnResponse toResponse(SalesReturn r) {
    return new ReturnResponse(r.getId(), r.getReturnNumber(), r.getOrder().getId(), r.getOrder().getOrderNumber(),
        r.getReason(), r.isRestocked(), r.getRefundAmount(),
        r.getLines().stream().map(l -> new ReturnLineResponse(l.getOrderLine().getId(),
            l.getOrderLine().getVariant().getSku(), l.getQuantity(), l.getRefundAmount())).toList(),
        r.getCreatedAt());
  }
}
```

## `src/main/java/com/apex/service/InventoryService.java`

```java
package com.apex.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.InventoryRequests.AdjustStockRequest;
import com.apex.dto.request.InventoryRequests.StockCountRequest;
import com.apex.dto.response.InventoryResponses.MovementResponse;
import com.apex.dto.response.InventoryResponses.StockLevelResponse;
import com.apex.dto.response.InventoryResponses.VariantStockSummary;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.ProductVariant;
import com.apex.model.StockLevel;
import com.apex.model.StockMovement;
import com.apex.model.StockMovement.MovementType;
import com.apex.model.StorageLocation;
import com.apex.model.Warehouse;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.StockLevelRepository;
import com.apex.repository.StockMovementRepository;
import com.apex.repository.StorageLocationRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Single entry point for every change of stock. Other modules (transfers, purchase receipts, orders)
 * call the primitives below so that stock levels and the movement ledger can never drift apart.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

  private final StockLevelRepository stockLevelRepository;
  private final StockMovementRepository movementRepository;
  private final ProductVariantRepository variantRepository;
  private final WarehouseRepository warehouseRepository;
  private final StorageLocationRepository locationRepository;
  private final AlertService alertService;

  // ---------- Queries ----------

  @Transactional(readOnly = true)
  public Page<StockLevelResponse> listStock(UUID warehouseId, UUID variantId, boolean inStockOnly, Pageable pageable) {
    Specification<StockLevel> spec = (root, query, cb) -> cb.conjunction();
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
    }
    if (variantId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("variant").get("id"), variantId));
    }
    if (inStockOnly) {
      spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("quantityOnHand"), 0));
    }
    return stockLevelRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public VariantStockSummary getVariantSummary(UUID variantId) {
    ProductVariant variant = findVariant(variantId);
    List<StockLevelResponse> levels = stockLevelRepository.findByVariant(variantId).stream()
        .map(this::toResponse).toList();
    long onHand = levels.stream().mapToLong(StockLevelResponse::quantityOnHand).sum();
    long reserved = levels.stream().mapToLong(StockLevelResponse::quantityReserved).sum();
    return new VariantStockSummary(variantId, variant.getSku(), variant.getProduct().getName(),
        onHand, reserved, onHand - reserved, levels);
  }

  @Transactional(readOnly = true)
  public Page<MovementResponse> listMovements(UUID variantId, UUID warehouseId, MovementType type,
      LocalDate from, LocalDate to, Pageable pageable) {
    Specification<StockMovement> spec = (root, query, cb) -> cb.conjunction();
    if (variantId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("variant").get("id"), variantId));
    }
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
    }
    if (type != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
    }
    if (from != null) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
    }
    if (to != null) {
      spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
    }
    return movementRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public long availableQuantity(UUID variantId, UUID warehouseId) {
    return stockLevelRepository.findByVariantIdAndWarehouseId(variantId, warehouseId)
        .map(StockLevel::getAvailable).orElse(0);
  }

  // ---------- Manual operations ----------

  @Transactional
  public StockLevelResponse adjust(AdjustStockRequest request) {
    if (request.quantityChange() == 0) {
      throw new IllegalArgumentException("Quantity change must not be zero");
    }
    MovementType type = request.quantityChange() < 0 && Boolean.TRUE.equals(request.damaged())
        ? MovementType.DAMAGE
        : MovementType.ADJUSTMENT;
    StockLevel level = apply(request.variantId(), request.warehouseId(), request.quantityChange(), type,
        "MANUAL", null, request.reason());
    return toResponse(level);
  }

  @Transactional
  public StockLevelResponse stockCount(StockCountRequest request) {
    StockLevel level = lockOrCreate(request.variantId(), request.warehouseId());
    int delta = request.countedQuantity() - level.getQuantityOnHand();
    if (request.countedQuantity() < level.getQuantityReserved()) {
      throw new BusinessRuleException("Counted quantity (" + request.countedQuantity()
          + ") is lower than the quantity reserved for open orders (" + level.getQuantityReserved() + ")");
    }
    if (delta == 0) {
      return toResponse(level);
    }
    return toResponse(apply(request.variantId(), request.warehouseId(), delta, MovementType.STOCK_COUNT,
        "STOCK_COUNT", null, request.reason() != null ? request.reason() : "Physical stock count"));
  }

  @Transactional
  public StockLevelResponse assignLocation(UUID variantId, UUID warehouseId, UUID locationId) {
    StockLevel level = lockOrCreate(variantId, warehouseId);
    StorageLocation location = null;
    if (locationId != null) {
      location = locationRepository.findById(locationId)
          .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationId));
      if (!location.getWarehouse().getId().equals(warehouseId)) {
        throw new IllegalArgumentException("Location does not belong to this warehouse");
      }
    }
    level.setLocation(location);
    return toResponse(stockLevelRepository.save(level));
  }

  // ---------- Primitives used by other modules ----------

  /**
   * Changes quantity on hand by {@code delta} (positive or negative) and writes a ledger entry.
   * Outgoing quantity may only come from stock that is not reserved.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public StockLevel apply(UUID variantId, UUID warehouseId, int delta, MovementType type,
      String referenceType, UUID referenceId, String reason) {
    StockLevel level = lockOrCreate(variantId, warehouseId);
    if (delta > 0) {
      requireActiveWarehouse(level.getWarehouse());
    }
    int newOnHand = level.getQuantityOnHand() + delta;
    if (newOnHand < level.getQuantityReserved()) {
      throw new BusinessRuleException("Insufficient available stock for SKU " + level.getVariant().getSku()
          + " in warehouse " + level.getWarehouse().getCode() + ": available " + level.getAvailable()
          + ", requested " + (-delta));
    }
    level.setQuantityOnHand(newOnHand);
    record(level, type, delta, referenceType, referenceId, reason);
    level = stockLevelRepository.saveAndFlush(level);
    alertService.evaluate(variantId, warehouseId);
    return level;
  }

  /** Reserves stock for an order or outgoing transfer. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void reserve(UUID variantId, UUID warehouseId, int quantity) {
    requirePositive(quantity);
    StockLevel level = lockOrCreate(variantId, warehouseId);
    if (level.getAvailable() < quantity) {
      throw new BusinessRuleException("Insufficient available stock for SKU " + level.getVariant().getSku()
          + " in warehouse " + level.getWarehouse().getCode() + ": available " + level.getAvailable()
          + ", requested " + quantity);
    }
    level.setQuantityReserved(level.getQuantityReserved() + quantity);
    stockLevelRepository.saveAndFlush(level);
    alertService.evaluate(variantId, warehouseId);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void release(UUID variantId, UUID warehouseId, int quantity) {
    requirePositive(quantity);
    StockLevel level = lockOrCreate(variantId, warehouseId);
    if (level.getQuantityReserved() < quantity) {
      throw new IllegalStateException("Cannot release " + quantity + " units: only "
          + level.getQuantityReserved() + " reserved");
    }
    level.setQuantityReserved(level.getQuantityReserved() - quantity);
    stockLevelRepository.saveAndFlush(level);
    alertService.evaluate(variantId, warehouseId);
  }

  /** Removes previously reserved stock from the warehouse (shipment / transfer dispatch). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void consumeReserved(UUID variantId, UUID warehouseId, int quantity, MovementType type,
      String referenceType, UUID referenceId, String reason) {
    requirePositive(quantity);
    StockLevel level = lockOrCreate(variantId, warehouseId);
    if (level.getQuantityReserved() < quantity) {
      throw new IllegalStateException("Cannot consume " + quantity + " units: only "
          + level.getQuantityReserved() + " reserved");
    }
    level.setQuantityReserved(level.getQuantityReserved() - quantity);
    level.setQuantityOnHand(level.getQuantityOnHand() - quantity);
    record(level, type, -quantity, referenceType, referenceId, reason);
    stockLevelRepository.saveAndFlush(level);
    alertService.evaluate(variantId, warehouseId);
  }

  // ---------- Helpers ----------

  private StockLevel lockOrCreate(UUID variantId, UUID warehouseId) {
    return stockLevelRepository.findForUpdate(variantId, warehouseId).orElseGet(() -> {
      ProductVariant variant = findVariant(variantId);
      Warehouse warehouse = warehouseRepository.findById(warehouseId)
          .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
      if (!Boolean.TRUE.equals(variant.getIsActive())) {
        throw new IllegalStateException("Variant is inactive: " + variant.getSku());
      }
      return stockLevelRepository.saveAndFlush(StockLevel.builder().variant(variant).warehouse(warehouse).build());
    });
  }

  private void record(StockLevel level, MovementType type, int delta, String referenceType,
      UUID referenceId, String reason) {
    movementRepository.save(StockMovement.builder()
        .variant(level.getVariant())
        .warehouse(level.getWarehouse())
        .type(type)
        .quantityChange(delta)
        .quantityAfter(level.getQuantityOnHand())
        .referenceType(referenceType)
        .referenceId(referenceId)
        .reason(reason)
        .performedBy(CurrentUser.id().orElse(null))
        .build());
  }

  private void requireActiveWarehouse(Warehouse warehouse) {
    if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + warehouse.getCode());
    }
  }

  private void requirePositive(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
  }

  private ProductVariant findVariant(UUID id) {
    return variantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + id));
  }

  private StockLevelResponse toResponse(StockLevel s) {
    return new StockLevelResponse(s.getId(), s.getVariant().getId(), s.getVariant().getSku(),
        s.getVariant().getProduct().getName(), s.getWarehouse().getId(), s.getWarehouse().getCode(),
        s.getLocation() != null ? s.getLocation().getId() : null,
        s.getLocation() != null ? s.getLocation().getCode() : null,
        s.getQuantityOnHand(), s.getQuantityReserved(), s.getAvailable(), s.getUpdatedAt());
  }

  private MovementResponse toResponse(StockMovement m) {
    return new MovementResponse(m.getId(), m.getVariant().getId(), m.getVariant().getSku(),
        m.getWarehouse().getId(), m.getWarehouse().getCode(), m.getType().name(), m.getQuantityChange(),
        m.getQuantityAfter(), m.getReferenceType(), m.getReferenceId(), m.getReason(), m.getPerformedBy(),
        m.getCreatedAt());
  }
}
```

## `src/main/java/com/apex/service/JwtService.java`

```java
package com.apex.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.apex.model.User;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class JwtService {

  private final String secret;
  private final long ttlSeconds;
  private final String issuer;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.ttl-seconds:3600}") long ttlSeconds,
      @Value("${app.jwt.issuer:apex-backend}") String issuer) {
    this.secret = secret;
    this.ttlSeconds = ttlSeconds;
    this.issuer = issuer;
  }

  public long getTtlSeconds() {
    return ttlSeconds;
  }

  public String generateAccessToken(User user) {
    Instant now = Instant.now();

    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole() != null ? user.getRole().getName() : null)
        .claim("permissions",
            user.getRole() != null && user.getRole().getPermissions() != null
                ? user.getRole().getPermissions()
                : Collections.emptyList())
        .issueTime(java.util.Date.from(now))
        .expirationTime(java.util.Date.from(now.plusSeconds(ttlSeconds)))
        .build();

    try {
      JWSSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(signer);
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Failed to sign JWT", e);
    }
  }
}
```

## `src/main/java/com/apex/service/OrderService.java`

```java
package com.apex.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.OrderRequests.CreateOrderRequest;
import com.apex.dto.request.OrderRequests.OrderLineRequest;
import com.apex.dto.response.OrderResponses.OrderLineResponse;
import com.apex.dto.response.OrderResponses.OrderResponse;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.Customer;
import com.apex.model.ProductVariant;
import com.apex.model.SalesOrder;
import com.apex.model.SalesOrder.Channel;
import com.apex.model.SalesOrder.Status;
import com.apex.model.SalesOrderLine;
import com.apex.model.Warehouse;
import com.apex.repository.CustomerRepository;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.SalesOrderRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/** Order capture. Fulfilment steps live in {@link FulfilmentService}. */
@Service
@RequiredArgsConstructor
public class OrderService {

  private final SalesOrderRepository orderRepository;
  private final CustomerRepository customerRepository;
  private final WarehouseRepository warehouseRepository;
  private final ProductVariantRepository variantRepository;
  private final InventoryService inventoryService;

  @Value("${app.sales.tax-rate:0}")
  private BigDecimal taxRate;

  /** Creates the order and reserves stock for every line; fails as a whole if any line cannot be covered. */
  @Transactional
  public OrderResponse create(CreateOrderRequest request) {
    Customer customer = null;
    if (request.customerId() != null) {
      customer = customerRepository.findById(request.customerId())
          .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
      if (!Boolean.TRUE.equals(customer.getIsActive())) {
        throw new IllegalStateException("Customer is inactive");
      }
    }
    Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.warehouseId()));
    if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + warehouse.getCode());
    }

    SalesOrder order = SalesOrder.builder()
        .orderNumber(ReferenceNumbers.next("SO"))
        .customer(customer)
        .warehouse(warehouse)
        .channel(request.channel() != null ? request.channel() : Channel.IN_STORE)
        .shippingAddress(request.shippingAddress())
        .notes(request.notes())
        .taxRate(taxRate)
        .createdBy(CurrentUser.id().orElse(null))
        .build();

    Map<UUID, OrderLineRequest> merged = new LinkedHashMap<>();
    for (OrderLineRequest l : request.lines()) {
      merged.merge(l.variantId(), l, (a, b) -> new OrderLineRequest(a.variantId(), a.quantity() + b.quantity(),
          a.unitPrice(), a.discountPercent()));
    }

    merged.values().forEach(l -> {
      ProductVariant variant = variantRepository.findById(l.variantId())
          .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + l.variantId()));
      if (!Boolean.TRUE.equals(variant.getIsActive()) || variant.getProduct().getDeletedAt() != null) {
        throw new IllegalStateException("SKU is not available for sale: " + variant.getSku());
      }
      inventoryService.reserve(variant.getId(), warehouse.getId(), l.quantity());
      order.addLine(SalesOrderLine.builder()
          .variant(variant)
          .quantity(l.quantity())
          .unitPrice(l.unitPrice() != null ? l.unitPrice() : variant.getRetailPrice())
          .unitCost(variant.getCostPrice())
          .discountPercent(l.discountPercent() != null ? l.discountPercent() : BigDecimal.ZERO)
          .build());
    });

    return toResponse(orderRepository.save(order));
  }

  @Transactional(readOnly = true)
  public OrderResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional(readOnly = true)
  public Page<OrderResponse> list(Status status, UUID customerId, UUID warehouseId, String orderNumber,
      java.time.LocalDate from, java.time.LocalDate to, Pageable pageable) {
    Specification<SalesOrder> spec = (root, query, cb) -> cb.conjunction();
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (customerId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
    }
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
    }
    if (orderNumber != null && !orderNumber.isBlank()) {
      String pattern = "%" + orderNumber.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("orderNumber")), pattern));
    }
    if (from != null) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
    }
    if (to != null) {
      spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
    }
    return orderRepository.findAll(spec, pageable).map(this::toResponse);
  }

  SalesOrder find(UUID id) {
    return orderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
  }

  OrderResponse toResponse(SalesOrder o) {
    return new OrderResponse(o.getId(), o.getOrderNumber(),
        o.getCustomer() != null ? o.getCustomer().getId() : null,
        o.getCustomer() != null ? o.getCustomer().getName() : null,
        o.getWarehouse().getId(), o.getWarehouse().getCode(), o.getStatus().name(), o.getChannel().name(),
        o.getShippingAddress(), o.getNotes(), o.getSubtotal(), o.getTaxAmount(), o.getTotal(),
        o.getCarrier(), o.getTrackingNumber(),
        o.getLines().stream().map(l -> new OrderLineResponse(l.getId(), l.getVariant().getId(),
            l.getVariant().getSku(), l.getVariant().getProduct().getName(), l.getQuantity(),
            l.getQuantityPicked(), l.getQuantityReturned(), l.getUnitPrice(), l.getDiscountPercent(),
            l.getLineTotal())).toList(),
        o.getShippedAt(), o.getDeliveredAt(), o.getCreatedAt());
  }
}
```

## `src/main/java/com/apex/service/ProductService.java`

```java
package com.apex.service;

import com.apex.dto.request.ProductRequests.CreateBarcodeRequest;
import com.apex.dto.request.ProductRequests.CreateProductRequest;
import com.apex.dto.request.ProductRequests.CreateMediaRequest;
import com.apex.dto.request.ProductRequests.CreateVariantRequest;
import com.apex.dto.request.ProductRequests.UpdateProductRequest;
import com.apex.dto.request.ProductRequests.UpdateVariantRequest;
import com.apex.dto.response.ProductResponses.BarcodeResponse;
import com.apex.dto.response.ProductResponses.MediaResponse;
import com.apex.dto.response.ProductResponses.ProductResponse;
import com.apex.dto.response.ProductResponses.VariantResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.mapper.ProductMapper;
import com.apex.model.Category;
import com.apex.model.MediaAsset;
import com.apex.model.Product;
import com.apex.model.ProductBarcode;
import com.apex.model.ProductBarcode.BarcodeType;
import com.apex.model.ProductVariant;
import com.apex.repository.CategoryRepository;
import com.apex.repository.MediaAssetRepository;
import com.apex.repository.ProductBarcodeRepository;
import com.apex.repository.ProductRepository;
import com.apex.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductVariantRepository variantRepository;
  private final ProductBarcodeRepository barcodeRepository;
  private final CategoryRepository categoryRepository;
  private final MediaAssetRepository mediaAssetRepository;
  private final ProductMapper productMapper;

  // ---------- Products ----------

  @Transactional
  public ProductResponse createProduct(CreateProductRequest request) {
    Category category = categoryRepository.findById(request.categoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));

    Product product = Product.builder()
        .category(category)
        .name(request.name())
        .slug(generateUniqueSlug(request.name()))
        .description(request.description() != null ? request.description() : "")
        .brand(request.brand())
        .unitOfMeasure(request.unitOfMeasure())
        .hasVariants(Boolean.TRUE.equals(request.hasVariants()))
        .isActive(true)
        .build();

    product = productRepository.save(product);

    if (request.variants() != null && !request.variants().isEmpty()) {
      final Product savedProduct = product;
      List<ProductVariant> variants = request.variants().stream()
          .map(variantRequest -> buildVariant(savedProduct, variantRequest))
          .collect(Collectors.toCollection(ArrayList::new));
      product.setVariants(variants);
      product = productRepository.save(product);
    }

    return toFullResponse(product);
  }

  @Transactional(readOnly = true)
  public ProductResponse getProduct(UUID id) {
    Product product = findActiveProduct(id);
    return toFullResponse(product);
  }

  @Transactional(readOnly = true)
  public ProductResponse getProductBySlug(String slug) {
    Product product = productRepository.findBySlugAndDeletedAtIsNull(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
    return toFullResponse(product);
  }

  @Transactional(readOnly = true)
  public Page<ProductResponse> listProducts(UUID categoryId, String keyword, Pageable pageable) {
    Specification<Product> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    if (categoryId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
    }
    if (keyword != null && !keyword.isBlank()) {
      String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("name")), pattern),
          cb.like(cb.lower(root.get("brand")), pattern)));
    }
    return productRepository.findAll(spec, pageable).map(this::toFullResponse);
  }

  @Transactional
  public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
    Product product = findActiveProduct(id);
    if (!product.getCategory().getId().equals(request.categoryId())) {
      product.setCategory(categoryRepository.findById(request.categoryId())
          .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId())));
    }
    product.setName(request.name());
    product.setDescription(request.description() != null ? request.description() : product.getDescription());
    product.setBrand(request.brand());
    product.setUnitOfMeasure(request.unitOfMeasure());
    if (request.isActive() != null) {
      product.setIsActive(request.isActive());
    }
    return toFullResponse(productRepository.save(product));
  }

  @Transactional
  public void deleteProduct(UUID id) {
    Product product = findActiveProduct(id);
    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
  }

  // ---------- Variants ----------

  @Transactional
  public VariantResponse addVariant(UUID productId, CreateVariantRequest request) {
    Product product = findActiveProduct(productId);
    ProductVariant variant = variantRepository.save(buildVariant(product, request));
    return productMapper.toVariantResponse(variant);
  }

  @Transactional(readOnly = true)
  public Page<VariantResponse> listVariants(UUID productId, Pageable pageable) {
    findActiveProduct(productId);
    return variantRepository.findByProductId(productId, pageable)
        .map(productMapper::toVariantResponse);
  }

  @Transactional(readOnly = true)
  public VariantResponse getVariantBySku(String sku) {
    ProductVariant variant = variantRepository.findBySku(sku)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + sku));
    return productMapper.toVariantResponse(variant);
  }

  @Transactional
  public VariantResponse updateVariant(UUID variantId, UpdateVariantRequest request) {
    ProductVariant variant = findVariant(variantId);
    variant.setVariantAttributes(request.variantAttributes());
    variant.setCostPrice(request.costPrice());
    variant.setRetailPrice(request.retailPrice());
    variant.setDimensions(request.dimensions());
    variant.setWeight(request.weight());
    if (request.isActive() != null) {
      variant.setIsActive(request.isActive());
    }
    return productMapper.toVariantResponse(variantRepository.save(variant));
  }

  /** Variants are deactivated rather than deleted, since stock and order history reference them. */
  @Transactional
  public void deactivateVariant(UUID variantId) {
    ProductVariant variant = findVariant(variantId);
    variant.setIsActive(false);
    variantRepository.save(variant);
  }

  // ---------- Media ----------

  @Transactional
  public MediaResponse addMedia(UUID productId, CreateMediaRequest request) {
    Product product = findActiveProduct(productId);
    ProductVariant variant = null;
    if (request.variantId() != null) {
      variant = findVariant(request.variantId());
      if (!variant.getProduct().getId().equals(productId)) {
        throw new IllegalArgumentException("Variant does not belong to this product");
      }
    }
    boolean primary = Boolean.TRUE.equals(request.isPrimary());
    if (primary) {
      mediaAssetRepository.findByProductIdOrderBySortOrderAsc(productId)
          .forEach(existing -> existing.setIsPrimary(false));
    }
    MediaAsset asset = MediaAsset.builder()
        .product(product)
        .variant(variant)
        .url(request.url())
        .fileName(request.fileName())
        .mimeType(request.mimeType())
        .isPrimary(primary)
        .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
        .build();
    return productMapper.toMediaResponse(mediaAssetRepository.save(asset));
  }

  @Transactional(readOnly = true)
  public List<MediaResponse> listMedia(UUID productId) {
    findActiveProduct(productId);
    return mediaAssetRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
        .map(productMapper::toMediaResponse)
        .toList();
  }

  @Transactional
  public void deleteMedia(UUID mediaId) {
    MediaAsset asset = mediaAssetRepository.findById(mediaId)
        .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + mediaId));
    mediaAssetRepository.delete(asset);
  }

  // ---------- Barcodes ----------

  @Transactional
  public BarcodeResponse addBarcode(UUID variantId, CreateBarcodeRequest request) {
    ProductVariant variant = variantRepository.findById(variantId)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

    validateBarcodeUniqueness(request.barcode());

    ProductBarcode barcode = buildBarcode(request);
    if (request.isPrimary() == null) {
      barcode.setIsPrimary(variant.getBarcodes().isEmpty());
    }
    if (Boolean.TRUE.equals(barcode.getIsPrimary())) {
      variant.getBarcodes().forEach(existing -> existing.setIsPrimary(false));
    }
    variant.addBarcode(barcode);
    barcode = barcodeRepository.save(barcode);
    return productMapper.toBarcodeResponse(barcode);
  }

  @Transactional
  public void removeBarcode(UUID barcodeId) {
    ProductBarcode barcode = barcodeRepository.findById(barcodeId)
        .orElseThrow(() -> new ResourceNotFoundException("Barcode not found: " + barcodeId));
    ProductVariant variant = barcode.getVariant();
    variant.removeBarcode(barcode);
    if (Boolean.TRUE.equals(barcode.getIsPrimary()) && !variant.getBarcodes().isEmpty()) {
      variant.getBarcodes().get(0).setIsPrimary(true);
    }
    variantRepository.save(variant);
  }

  @Transactional(readOnly = true)
  public VariantResponse lookupByBarcode(String barcode) {
    ProductBarcode found = barcodeRepository.findBarcodeWithProductDetails(barcode)
        .orElseThrow(() -> new ResourceNotFoundException("Barcode not found: " + barcode));
    return productMapper.toVariantResponse(found.getVariant());
  }

  // ---------- Private Helpers ----------

  private ProductVariant findVariant(UUID id) {
    return variantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + id));
  }

  private Product findActiveProduct(UUID id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    if (product.getDeletedAt() != null) {
      throw new ResourceNotFoundException("Product not found: " + id);
    }
    return product;
  }

  private ProductVariant buildVariant(Product product, CreateVariantRequest request) {
    if (variantRepository.existsBySku(request.sku())) {
      throw new DuplicateResourceException("SKU already exists: " + request.sku());
    }

    ProductVariant variant = ProductVariant.builder()
        .product(product)
        .sku(request.sku())
        .variantAttributes(request.variantAttributes())
        .costPrice(request.costPrice())
        .retailPrice(request.retailPrice())
        .dimensions(request.dimensions())
        .weight(request.weight())
        .isActive(true)
        .build();

    if (request.barcodes() != null) {
      for (CreateBarcodeRequest barcodeRequest : request.barcodes()) {
        validateBarcodeUniqueness(barcodeRequest.barcode());
        variant.addBarcode(buildBarcode(barcodeRequest));
      }
    }
    return variant;
  }

  private ProductBarcode buildBarcode(CreateBarcodeRequest request) {
    BarcodeType type;
    try {
      type = BarcodeType.valueOf(request.type().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid barcode type: " + request.type()
          + ". Valid types: UPC_A, EAN_13, CODE_128, QR_CODE");
    }

    return ProductBarcode.builder()
        .barcode(request.barcode())
        .type(type)
        .isPrimary(request.isPrimary() != null ? request.isPrimary() : true)
        .build();
  }

  private void validateBarcodeUniqueness(String barcode) {
    if (barcodeRepository.existsByBarcode(barcode)) {
      throw new DuplicateResourceException("Barcode already exists: " + barcode);
    }
  }

  private ProductResponse toFullResponse(Product product) {
    var mediaAssets = mediaAssetRepository.findByProductIdOrderBySortOrderAsc(product.getId());
    return productMapper.toProductResponse(product, mediaAssets);
  }

  private String generateUniqueSlug(String name) {
    String base = slugify(name);
    String slug = base;
    int counter = 1;
    while (productRepository.existsBySlug(slug)) {
      slug = base + "-" + counter++;
    }
    return slug;
  }

  private String slugify(String input) {
    if (input == null)
      return "";
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return normalized.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\s-]", "")
        .trim()
        .replaceAll("[\\s-]+", "-");
  }
}
```

## `src/main/java/com/apex/service/PurchaseOrderService.java`

```java
package com.apex.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.PurchaseOrderRequests.CreatePurchaseOrderRequest;
import com.apex.dto.request.PurchaseOrderRequests.PoLineRequest;
import com.apex.dto.request.PurchaseOrderRequests.ReceiveLine;
import com.apex.dto.request.PurchaseOrderRequests.ReceivePurchaseOrderRequest;
import com.apex.dto.request.PurchaseOrderRequests.UpdatePurchaseOrderRequest;
import com.apex.dto.response.PurchaseOrderResponses.GoodsReceiptResponse;
import com.apex.dto.response.PurchaseOrderResponses.PoLineResponse;
import com.apex.dto.response.PurchaseOrderResponses.PurchaseOrderResponse;
import com.apex.dto.response.PurchaseOrderResponses.ReceiptLineResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.GoodsReceipt;
import com.apex.model.GoodsReceiptLine;
import com.apex.model.ProductVariant;
import com.apex.model.PurchaseOrder;
import com.apex.model.PurchaseOrder.Status;
import com.apex.model.PurchaseOrderLine;
import com.apex.model.StockMovement.MovementType;
import com.apex.model.Supplier;
import com.apex.model.Warehouse;
import com.apex.repository.GoodsReceiptRepository;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.PurchaseOrderRepository;
import com.apex.repository.SupplierProductRepository;
import com.apex.repository.SupplierRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Purchase order lifecycle: DRAFT -> ORDERED -> PARTIALLY_RECEIVED -> RECEIVED.
 * An order with no deliveries can be CANCELLED; a partially delivered one can be CLOSED.
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final GoodsReceiptRepository receiptRepository;
  private final SupplierRepository supplierRepository;
  private final SupplierProductRepository supplierProductRepository;
  private final WarehouseRepository warehouseRepository;
  private final ProductVariantRepository variantRepository;
  private final InventoryService inventoryService;

  @Transactional
  public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {
    Supplier supplier = supplierRepository.findById(request.supplierId())
        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));
    if (!Boolean.TRUE.equals(supplier.getIsActive())) {
      throw new IllegalStateException("Supplier is inactive: " + supplier.getCode());
    }
    PurchaseOrder po = PurchaseOrder.builder()
        .poNumber(ReferenceNumbers.next("PO"))
        .supplier(supplier)
        .warehouse(activeWarehouse(request.warehouseId()))
        .expectedDate(request.expectedDate())
        .notes(request.notes())
        .createdBy(CurrentUser.id().orElse(null))
        .build();
    fillLines(po, request.lines());
    return toResponse(purchaseOrderRepository.save(po));
  }

  @Transactional
  public PurchaseOrderResponse update(UUID id, UpdatePurchaseOrderRequest request) {
    PurchaseOrder po = find(id);
    requireStatus(po, "edited", Status.DRAFT);
    po.setWarehouse(activeWarehouse(request.warehouseId()));
    po.setExpectedDate(request.expectedDate());
    po.setNotes(request.notes());
    po.getLines().clear();
    purchaseOrderRepository.flush();
    fillLines(po, request.lines());
    return toResponse(purchaseOrderRepository.save(po));
  }

  @Transactional
  public PurchaseOrderResponse submit(UUID id) {
    PurchaseOrder po = find(id);
    requireStatus(po, "submitted", Status.DRAFT);
    po.setStatus(Status.ORDERED);
    po.setOrderedAt(LocalDateTime.now());
    return toResponse(purchaseOrderRepository.save(po));
  }

  @Transactional
  public GoodsReceiptResponse receive(UUID id, ReceivePurchaseOrderRequest request) {
    PurchaseOrder po = find(id);
    requireStatus(po, "received against", Status.ORDERED, Status.PARTIALLY_RECEIVED);

    Map<UUID, Integer> quantities = new LinkedHashMap<>();
    for (ReceiveLine l : request.lines()) {
      quantities.merge(l.lineId(), l.quantity(), Integer::sum);
    }

    GoodsReceipt receipt = GoodsReceipt.builder()
        .receiptNumber(ReferenceNumbers.next("GRN"))
        .purchaseOrder(po)
        .notes(request.notes())
        .receivedBy(CurrentUser.id().orElse(null))
        .build();

    quantities.forEach((lineId, qty) -> {
      PurchaseOrderLine line = po.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Line does not belong to this purchase order: " + lineId));
      if (qty > line.getOutstanding()) {
        throw new BusinessRuleException("Cannot receive " + qty + " of SKU " + line.getVariant().getSku()
            + ": only " + line.getOutstanding() + " outstanding");
      }
      line.setQuantityReceived(line.getQuantityReceived() + qty);
      inventoryService.apply(line.getVariant().getId(), po.getWarehouse().getId(), qty,
          MovementType.PURCHASE_RECEIPT, "PURCHASE_ORDER", po.getId(), po.getPoNumber());
      receipt.addLine(GoodsReceiptLine.builder().poLine(line).quantity(qty).build());
    });

    boolean complete = po.getLines().stream().allMatch(l -> l.getOutstanding() == 0);
    po.setStatus(complete ? Status.RECEIVED : Status.PARTIALLY_RECEIVED);
    purchaseOrderRepository.save(po);
    return toResponse(receiptRepository.save(receipt));
  }

  @Transactional
  public PurchaseOrderResponse cancel(UUID id) {
    PurchaseOrder po = find(id);
    requireStatus(po, "cancelled", Status.DRAFT, Status.ORDERED);
    po.setStatus(Status.CANCELLED);
    return toResponse(purchaseOrderRepository.save(po));
  }

  /** Stops waiting for the rest of a partially delivered order. */
  @Transactional
  public PurchaseOrderResponse close(UUID id) {
    PurchaseOrder po = find(id);
    requireStatus(po, "closed", Status.PARTIALLY_RECEIVED);
    po.setStatus(Status.CLOSED);
    return toResponse(purchaseOrderRepository.save(po));
  }

  @Transactional(readOnly = true)
  public PurchaseOrderResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional(readOnly = true)
  public Page<PurchaseOrderResponse> list(Status status, UUID supplierId, UUID warehouseId, Pageable pageable) {
    Specification<PurchaseOrder> spec = (root, query, cb) -> cb.conjunction();
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (supplierId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("supplier").get("id"), supplierId));
    }
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
    }
    return purchaseOrderRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public List<GoodsReceiptResponse> listReceipts(UUID id) {
    find(id);
    return receiptRepository.findByPurchaseOrderIdOrderByReceivedAtAsc(id).stream().map(this::toResponse).toList();
  }

  // ---------- helpers ----------

  private void fillLines(PurchaseOrder po, List<PoLineRequest> requests) {
    Map<UUID, PoLineRequest> merged = new LinkedHashMap<>();
    for (PoLineRequest r : requests) {
      merged.merge(r.variantId(), r, (a, b) -> new PoLineRequest(a.variantId(), a.quantity() + b.quantity(),
          a.unitCost() != null ? a.unitCost() : b.unitCost()));
    }
    merged.values().forEach(r -> {
      ProductVariant variant = variantRepository.findById(r.variantId())
          .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + r.variantId()));
      var cost = r.unitCost() != null ? r.unitCost()
          : supplierProductRepository.findBySupplierIdAndVariantId(po.getSupplier().getId(), variant.getId())
              .map(sp -> sp.getUnitCost()).orElse(variant.getCostPrice());
      po.addLine(PurchaseOrderLine.builder().variant(variant).quantityOrdered(r.quantity()).unitCost(cost).build());
    });
  }

  private void requireStatus(PurchaseOrder po, String action, Status... allowed) {
    for (Status s : allowed) {
      if (po.getStatus() == s) {
        return;
      }
    }
    throw new IllegalStateException("Purchase order " + po.getPoNumber() + " is " + po.getStatus()
        + " and cannot be " + action);
  }

  private Warehouse activeWarehouse(UUID id) {
    Warehouse w = warehouseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
    if (!Boolean.TRUE.equals(w.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + w.getCode());
    }
    return w;
  }

  private PurchaseOrder find(UUID id) {
    return purchaseOrderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));
  }

  private PurchaseOrderResponse toResponse(PurchaseOrder po) {
    return new PurchaseOrderResponse(po.getId(), po.getPoNumber(), po.getSupplier().getId(),
        po.getSupplier().getName(), po.getWarehouse().getId(), po.getWarehouse().getCode(),
        po.getStatus().name(), po.getExpectedDate(), po.getNotes(), po.getTotalAmount(),
        po.getLines().stream().map(l -> new PoLineResponse(l.getId(), l.getVariant().getId(),
            l.getVariant().getSku(), l.getVariant().getProduct().getName(), l.getQuantityOrdered(),
            l.getQuantityReceived(), l.getOutstanding(), l.getUnitCost(), l.getLineTotal())).toList(),
        po.getOrderedAt(), po.getCreatedAt());
  }

  private GoodsReceiptResponse toResponse(GoodsReceipt r) {
    return new GoodsReceiptResponse(r.getId(), r.getReceiptNumber(), r.getNotes(),
        r.getLines().stream().map(l -> new ReceiptLineResponse(l.getPoLine().getId(),
            l.getPoLine().getVariant().getSku(), l.getQuantity())).toList(),
        r.getReceivedBy(), r.getReceivedAt());
  }
}
```

## `src/main/java/com/apex/service/ReferenceNumbers.java`

```java
package com.apex.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Human readable document numbers such as {@code PO-20261031-K3F9QZ}. */
public final class ReferenceNumbers {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  private ReferenceNumbers() {
  }

  public static String next(String prefix) {
    StringBuilder suffix = new StringBuilder();
    for (int i = 0; i < 6; i++) {
      suffix.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return prefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
  }
}
```

## `src/main/java/com/apex/service/ReportService.java`

```java
package com.apex.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.response.ReportResponses.AuditLogResponse;
import com.apex.dto.response.ReportResponses.Dashboard;
import com.apex.dto.response.ReportResponses.LowStockRow;
import com.apex.dto.response.ReportResponses.SalesDay;
import com.apex.dto.response.ReportResponses.SalesReport;
import com.apex.dto.response.ReportResponses.StockValuationReport;
import com.apex.dto.response.ReportResponses.StockValuationRow;
import com.apex.dto.response.ReportResponses.TopProductRow;
import com.apex.model.AuditLog;
import com.apex.model.PurchaseOrder;
import com.apex.model.SalesOrder;
import com.apex.model.SalesOrderLine;
import com.apex.model.SalesReturn;
import com.apex.model.StockAlert;
import com.apex.model.StockLevel;
import com.apex.repository.AuditLogRepository;
import com.apex.repository.ProductRepository;
import com.apex.repository.PurchaseOrderRepository;
import com.apex.repository.SalesOrderRepository;
import com.apex.repository.SalesReturnRepository;
import com.apex.repository.StockAlertRepository;
import com.apex.repository.StockLevelRepository;
import com.apex.repository.WarehouseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

  /** Orders that count as sales: they have left (or are about to leave) the warehouse and were not cancelled. */
  private static final EnumSet<SalesOrder.Status> REVENUE_STATUSES = EnumSet.of(
      SalesOrder.Status.SHIPPED, SalesOrder.Status.DELIVERED);

  private final StockLevelRepository stockLevelRepository;
  private final SalesOrderRepository orderRepository;
  private final SalesReturnRepository returnRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final StockAlertRepository alertRepository;
  private final ProductRepository productRepository;
  private final WarehouseRepository warehouseRepository;
  private final AuditLogRepository auditLogRepository;

  public StockValuationReport stockValuation(UUID warehouseId) {
    List<StockLevel> levels = warehouseId == null
        ? stockLevelRepository.findAllInStock()
        : stockLevelRepository.findInStockByWarehouse(warehouseId);
    List<StockValuationRow> rows = levels.stream().map(s -> {
      BigDecimal qty = BigDecimal.valueOf(s.getQuantityOnHand());
      return new StockValuationRow(s.getVariant().getId(), s.getVariant().getSku(),
          s.getVariant().getProduct().getName(), s.getWarehouse().getId(), s.getWarehouse().getCode(),
          s.getQuantityOnHand(), s.getVariant().getCostPrice(), s.getVariant().getRetailPrice(),
          s.getVariant().getCostPrice().multiply(qty), s.getVariant().getRetailPrice().multiply(qty));
    }).toList();
    return new StockValuationReport(
        rows.stream().mapToLong(StockValuationRow::quantityOnHand).sum(),
        rows.stream().map(StockValuationRow::costValue).reduce(BigDecimal.ZERO, BigDecimal::add),
        rows.stream().map(StockValuationRow::retailValue).reduce(BigDecimal.ZERO, BigDecimal::add),
        rows);
  }

  public List<LowStockRow> lowStock(int threshold) {
    return stockLevelRepository.findAtOrBelowAvailable(threshold).stream()
        .map(s -> new LowStockRow(s.getVariant().getId(), s.getVariant().getSku(),
            s.getVariant().getProduct().getName(), s.getWarehouse().getId(), s.getWarehouse().getCode(),
            s.getQuantityOnHand(), s.getAvailable()))
        .toList();
  }

  public SalesReport sales(LocalDate from, LocalDate to) {
    List<SalesOrder> orders = salesOrders(from, to);
    Map<LocalDate, long[]> counters = new TreeMap<>();      // orders, units
    Map<LocalDate, BigDecimal[]> amounts = new TreeMap<>(); // revenue, profit

    long units = 0;
    BigDecimal revenue = BigDecimal.ZERO;
    BigDecimal cost = BigDecimal.ZERO;
    for (SalesOrder o : orders) {
      LocalDate day = o.getShippedAt() != null ? o.getShippedAt().toLocalDate() : o.getCreatedAt().toLocalDate();
      long orderUnits = o.getLines().stream().mapToLong(SalesOrderLine::getQuantity).sum();
      BigDecimal orderRevenue = o.getSubtotal();
      BigDecimal orderCost = o.getLines().stream()
          .map(l -> l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);

      long[] c = counters.computeIfAbsent(day, d -> new long[2]);
      c[0]++;
      c[1] += orderUnits;
      BigDecimal[] a = amounts.computeIfAbsent(day, d -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
      a[0] = a[0].add(orderRevenue);
      a[1] = a[1].add(orderRevenue.subtract(orderCost));

      units += orderUnits;
      revenue = revenue.add(orderRevenue);
      cost = cost.add(orderCost);
    }

    BigDecimal refunds = returnsBetween(from, to).stream().map(SalesReturn::getRefundAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    List<SalesDay> days = counters.entrySet().stream()
        .map(e -> new SalesDay(e.getKey(), e.getValue()[0], e.getValue()[1],
            amounts.get(e.getKey())[0], amounts.get(e.getKey())[1]))
        .toList();
    return new SalesReport(from, to, orders.size(), units, revenue, cost, revenue.subtract(cost), refunds,
        revenue.subtract(refunds), days);
  }

  public List<TopProductRow> topProducts(LocalDate from, LocalDate to, int limit) {
    Map<UUID, long[]> units = new HashMap<>();
    Map<UUID, BigDecimal> revenue = new HashMap<>();
    Map<UUID, SalesOrderLine> sample = new HashMap<>();
    for (SalesOrder o : salesOrders(from, to)) {
      for (SalesOrderLine l : o.getLines()) {
        UUID id = l.getVariant().getId();
        units.computeIfAbsent(id, k -> new long[1])[0] += l.getQuantity();
        revenue.merge(id, l.getLineTotal(), BigDecimal::add);
        sample.putIfAbsent(id, l);
      }
    }
    return units.entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
        .limit(Math.max(1, limit))
        .map(e -> {
          SalesOrderLine l = sample.get(e.getKey());
          return new TopProductRow(e.getKey(), l.getVariant().getSku(), l.getVariant().getProduct().getName(),
              e.getValue()[0], revenue.get(e.getKey()));
        })
        .collect(Collectors.toList());
  }

  public Dashboard dashboard() {
    LocalDate today = LocalDate.now();
    List<SalesOrder> todays = salesOrders(today, today);
    long pending = orderRepository.count((root, q, cb) -> cb.equal(root.get("status"), SalesOrder.Status.PENDING));
    long toShip = orderRepository.count((root, q, cb) -> root.get("status").in(
        SalesOrder.Status.PENDING, SalesOrder.Status.PICKING, SalesOrder.Status.PACKED));
    long openPos = purchaseOrderRepository.count((root, q, cb) -> root.get("status").in(
        PurchaseOrder.Status.ORDERED, PurchaseOrder.Status.PARTIALLY_RECEIVED));
    return new Dashboard(
        productRepository.countByDeletedAtIsNullAndIsActiveTrue(),
        warehouseRepository.countByIsActiveTrue(),
        stockLevelRepository.totalUnitsOnHand(),
        stockLevelRepository.totalCostValue(),
        alertRepository.countByStatus(StockAlert.Status.OPEN),
        pending,
        toShip,
        openPos,
        todays.size(),
        todays.stream().map(SalesOrder::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
        LocalDateTime.now());
  }

  public Page<AuditLogResponse> auditLogs(UUID userId, String method, String resource, LocalDate from, LocalDate to,
      Pageable pageable) {
    Specification<AuditLog> spec = (root, q, cb) -> cb.conjunction();
    if (userId != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("userId"), userId));
    }
    if (method != null && !method.isBlank()) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("method"), method.toUpperCase()));
    }
    if (resource != null && !resource.isBlank()) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("resource"), resource));
    }
    if (from != null) {
      spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
    }
    if (to != null) {
      spec = spec.and((root, q, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
    }
    return auditLogRepository.findAll(spec, pageable).map(a -> new AuditLogResponse(a.getId(), a.getUserId(),
        a.getUserEmail(), a.getMethod(), a.getPath(), a.getResource(), a.getStatusCode(), a.getIpAddress(),
        a.getCreatedAt()));
  }

  // ---------- helpers ----------

  private List<SalesOrder> salesOrders(LocalDate from, LocalDate to) {
    LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
    LocalDate end = to != null ? to : LocalDate.now();
    Specification<SalesOrder> spec = (root, q, cb) -> root.get("status").in(REVENUE_STATUSES);
    spec = spec.and((root, q, cb) -> cb.and(
        cb.greaterThanOrEqualTo(root.get("shippedAt"), start.atStartOfDay()),
        cb.lessThan(root.get("shippedAt"), end.plusDays(1).atStartOfDay())));
    return orderRepository.findAll(spec);
  }

  private List<SalesReturn> returnsBetween(LocalDate from, LocalDate to) {
    LocalDateTime start = (from != null ? from : LocalDate.now().minusDays(30)).atStartOfDay();
    LocalDateTime end = (to != null ? to : LocalDate.now()).plusDays(1).atStartOfDay();
    return returnRepository.findAll().stream()
        .filter(r -> !r.getCreatedAt().isBefore(start) && r.getCreatedAt().isBefore(end))
        .toList();
  }
}
```

## `src/main/java/com/apex/service/RoleService.java`

```java
package com.apex.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.RoleRequests.CreateRoleRequest;
import com.apex.dto.request.RoleRequests.UpdateRoleRequest;
import com.apex.dto.response.RoleResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.Role;
import com.apex.repository.RoleRepository;
import com.apex.repository.UserRepository;
import com.apex.security.Permissions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

  public static final Set<String> BUILT_IN = Set.of("ADMIN", "MANAGER", "WAREHOUSE_STAFF", "CASHIER");

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<RoleResponse> listRoles() {
    return roleRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public RoleResponse getRole(UUID id) {
    return toResponse(find(id));
  }

  public List<String> listPermissions() {
    return Permissions.all();
  }

  @Transactional
  public RoleResponse createRole(CreateRoleRequest request) {
    String name = normalizeName(request.name());
    if (roleRepository.existsByName(name)) {
      throw new DuplicateResourceException("Role already exists: " + name);
    }
    List<String> permissions = validate(request.permissions());
    return toResponse(roleRepository.save(Role.builder().name(name).permissions(permissions).build()));
  }

  @Transactional
  public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
    Role role = find(id);
    String name = normalizeName(request.name());
    List<String> permissions = validate(request.permissions());

    if (BUILT_IN.contains(role.getName()) && !role.getName().equals(name)) {
      throw new BusinessRuleException("Built-in roles cannot be renamed");
    }
    if ("ADMIN".equals(role.getName()) && !permissions.containsAll(Permissions.admin())) {
      throw new BusinessRuleException("The ADMIN role must keep every permission");
    }
    roleRepository.findByName(name).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
      throw new DuplicateResourceException("Role already exists: " + name);
    });

    role.setName(name);
    role.setPermissions(permissions);
    return toResponse(roleRepository.save(role));
  }

  @Transactional
  public void deleteRole(UUID id) {
    Role role = find(id);
    if (BUILT_IN.contains(role.getName())) {
      throw new BusinessRuleException("Built-in roles cannot be deleted");
    }
    if (userRepository.countByRoleIdAndDeletedAtIsNull(id) > 0) {
      throw new IllegalStateException("Role is still assigned to users. Reassign them first.");
    }
    roleRepository.delete(role);
  }

  private Role find(UUID id) {
    return roleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
  }

  private String normalizeName(String name) {
    return name.trim().toUpperCase().replaceAll("\\s+", "_");
  }

  private List<String> validate(List<String> permissions) {
    Set<String> unknown = Permissions.unknown(permissions);
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException("Unknown permissions: " + String.join(", ", unknown));
    }
    return new ArrayList<>(new LinkedHashSet<>(permissions));
  }

  private RoleResponse toResponse(Role role) {
    return new RoleResponse(role.getId(), role.getName(), role.getPermissions(),
        BUILT_IN.contains(role.getName()),
        userRepository.countByRoleIdAndDeletedAtIsNull(role.getId()), role.getCreatedAt());
  }
}
```

## `src/main/java/com/apex/service/SupplierService.java`

```java
package com.apex.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.SupplierRequests.CreateSupplierRequest;
import com.apex.dto.request.SupplierRequests.SupplierProductRequest;
import com.apex.dto.request.SupplierRequests.UpdateSupplierRequest;
import com.apex.dto.response.SupplierResponses.SupplierProductResponse;
import com.apex.dto.response.SupplierResponses.SupplierResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.ProductVariant;
import com.apex.model.Supplier;
import com.apex.model.SupplierProduct;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.SupplierProductRepository;
import com.apex.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierService {

  private final SupplierRepository supplierRepository;
  private final SupplierProductRepository supplierProductRepository;
  private final ProductVariantRepository variantRepository;

  @Transactional
  public SupplierResponse create(CreateSupplierRequest r) {
    String code = r.code().trim().toUpperCase(Locale.ROOT);
    if (supplierRepository.existsByCode(code)) {
      throw new DuplicateResourceException("Supplier code already exists: " + code);
    }
    Supplier supplier = Supplier.builder()
        .code(code).name(r.name()).contactName(r.contactName()).email(r.email()).phone(r.phone())
        .addressLine(r.addressLine()).city(r.city()).country(r.country()).paymentTerms(r.paymentTerms())
        .leadTimeDays(r.leadTimeDays()).notes(r.notes())
        .build();
    return toResponse(supplierRepository.save(supplier));
  }

  @Transactional(readOnly = true)
  public Page<SupplierResponse> list(String keyword, Boolean active, Pageable pageable) {
    Specification<Supplier> spec = (root, query, cb) -> cb.conjunction();
    if (active != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), active));
    }
    if (keyword != null && !keyword.isBlank()) {
      String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("name")), pattern),
          cb.like(cb.lower(root.get("code")), pattern)));
    }
    return supplierRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public SupplierResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public SupplierResponse update(UUID id, UpdateSupplierRequest r) {
    Supplier s = find(id);
    s.setName(r.name());
    s.setContactName(r.contactName());
    s.setEmail(r.email());
    s.setPhone(r.phone());
    s.setAddressLine(r.addressLine());
    s.setCity(r.city());
    s.setCountry(r.country());
    s.setPaymentTerms(r.paymentTerms());
    s.setLeadTimeDays(r.leadTimeDays());
    s.setNotes(r.notes());
    if (r.isActive() != null) {
      s.setIsActive(r.isActive());
    }
    return toResponse(supplierRepository.save(s));
  }

  @Transactional
  public void deactivate(UUID id) {
    Supplier s = find(id);
    s.setIsActive(false);
    supplierRepository.save(s);
  }

  // ---------- Supplier catalogue ----------

  @Transactional
  public SupplierProductResponse addProduct(UUID supplierId, SupplierProductRequest r) {
    Supplier supplier = find(supplierId);
    ProductVariant variant = variantRepository.findById(r.variantId())
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + r.variantId()));
    if (supplierProductRepository.existsBySupplierIdAndVariantId(supplierId, r.variantId())) {
      throw new DuplicateResourceException("Supplier already supplies SKU " + variant.getSku());
    }
    SupplierProduct sp = SupplierProduct.builder()
        .supplier(supplier).variant(variant).supplierSku(r.supplierSku()).unitCost(r.unitCost())
        .minOrderQuantity(r.minOrderQuantity() != null ? r.minOrderQuantity() : 1)
        .isPreferred(Boolean.TRUE.equals(r.isPreferred()))
        .build();
    return toResponse(supplierProductRepository.save(sp));
  }

  @Transactional(readOnly = true)
  public List<SupplierProductResponse> listProducts(UUID supplierId) {
    find(supplierId);
    return supplierProductRepository.findBySupplier(supplierId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<SupplierProductResponse> listSuppliersForVariant(UUID variantId) {
    return supplierProductRepository.findActiveByVariant(variantId).stream().map(this::toResponse).toList();
  }

  @Transactional
  public SupplierProductResponse updateProduct(UUID supplierId, UUID variantId, SupplierProductRequest r) {
    SupplierProduct sp = supplierProductRepository.findBySupplierIdAndVariantId(supplierId, variantId)
        .orElseThrow(() -> new ResourceNotFoundException("Supplier does not supply variant " + variantId));
    sp.setSupplierSku(r.supplierSku());
    sp.setUnitCost(r.unitCost());
    if (r.minOrderQuantity() != null) {
      sp.setMinOrderQuantity(r.minOrderQuantity());
    }
    if (r.isPreferred() != null) {
      sp.setIsPreferred(r.isPreferred());
    }
    return toResponse(supplierProductRepository.save(sp));
  }

  @Transactional
  public void removeProduct(UUID supplierId, UUID variantId) {
    SupplierProduct sp = supplierProductRepository.findBySupplierIdAndVariantId(supplierId, variantId)
        .orElseThrow(() -> new ResourceNotFoundException("Supplier does not supply variant " + variantId));
    supplierProductRepository.delete(sp);
  }

  private Supplier find(UUID id) {
    return supplierRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
  }

  private SupplierResponse toResponse(Supplier s) {
    return new SupplierResponse(s.getId(), s.getCode(), s.getName(), s.getContactName(), s.getEmail(),
        s.getPhone(), s.getAddressLine(), s.getCity(), s.getCountry(), s.getPaymentTerms(),
        s.getLeadTimeDays(), s.getNotes(), s.getIsActive(), s.getCreatedAt());
  }

  private SupplierProductResponse toResponse(SupplierProduct sp) {
    return new SupplierProductResponse(sp.getId(), sp.getSupplier().getId(), sp.getSupplier().getName(),
        sp.getVariant().getId(), sp.getVariant().getSku(), sp.getVariant().getProduct().getName(),
        sp.getSupplierSku(), sp.getUnitCost(), sp.getMinOrderQuantity(), sp.getIsPreferred());
  }
}
```

## `src/main/java/com/apex/service/TransferService.java`

```java
package com.apex.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.TransferRequests.CreateTransferRequest;
import com.apex.dto.request.TransferRequests.ReceiveTransferRequest;
import com.apex.dto.request.TransferRequests.ReceivedLine;
import com.apex.dto.request.TransferRequests.TransferLineRequest;
import com.apex.dto.response.TransferResponses.TransferLineResponse;
import com.apex.dto.response.TransferResponses.TransferResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.ProductVariant;
import com.apex.model.StockMovement.MovementType;
import com.apex.model.StockTransfer;
import com.apex.model.StockTransfer.Status;
import com.apex.model.StockTransferLine;
import com.apex.model.Warehouse;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.StockTransferRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Transfer lifecycle: REQUESTED (stock reserved at source) -> IN_TRANSIT (stock leaves source)
 * -> RECEIVED (stock lands at destination). A REQUESTED transfer can be CANCELLED.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

  private static final String REF = "TRANSFER";

  private final StockTransferRepository transferRepository;
  private final WarehouseRepository warehouseRepository;
  private final ProductVariantRepository variantRepository;
  private final InventoryService inventoryService;

  @Transactional
  public TransferResponse create(CreateTransferRequest request) {
    if (request.fromWarehouseId().equals(request.toWarehouseId())) {
      throw new IllegalArgumentException("Source and destination warehouse must differ");
    }
    Warehouse from = activeWarehouse(request.fromWarehouseId());
    Warehouse to = activeWarehouse(request.toWarehouseId());

    // merge duplicate variants so that reservation is done once per variant
    Map<UUID, Integer> quantities = new LinkedHashMap<>();
    for (TransferLineRequest line : request.lines()) {
      quantities.merge(line.variantId(), line.quantity(), Integer::sum);
    }

    StockTransfer transfer = StockTransfer.builder()
        .referenceNumber(ReferenceNumbers.next("TRF"))
        .fromWarehouse(from)
        .toWarehouse(to)
        .notes(request.notes())
        .requestedBy(CurrentUser.id().orElse(null))
        .build();

    quantities.forEach((variantId, qty) -> {
      ProductVariant variant = variantRepository.findById(variantId)
          .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
      inventoryService.reserve(variantId, from.getId(), qty);
      transfer.addLine(StockTransferLine.builder().variant(variant).quantity(qty).build());
    });

    return toResponse(transferRepository.save(transfer));
  }

  @Transactional
  public TransferResponse dispatch(UUID id) {
    StockTransfer transfer = find(id);
    requireStatus(transfer, Status.REQUESTED, "dispatched");
    for (StockTransferLine line : transfer.getLines()) {
      inventoryService.consumeReserved(line.getVariant().getId(), transfer.getFromWarehouse().getId(),
          line.getQuantity(), MovementType.TRANSFER_OUT, REF, transfer.getId(), transfer.getReferenceNumber());
    }
    transfer.setStatus(Status.IN_TRANSIT);
    transfer.setDispatchedAt(LocalDateTime.now());
    return toResponse(transferRepository.save(transfer));
  }

  @Transactional
  public TransferResponse receive(UUID id, ReceiveTransferRequest request) {
    StockTransfer transfer = find(id);
    requireStatus(transfer, Status.IN_TRANSIT, "received");

    Map<UUID, Integer> overrides = new HashMap<>();
    if (request != null && request.lines() != null) {
      for (ReceivedLine rl : request.lines()) {
        overrides.put(rl.lineId(), rl.receivedQuantity());
      }
      for (UUID lineId : overrides.keySet()) {
        if (transfer.getLines().stream().noneMatch(l -> l.getId().equals(lineId))) {
          throw new IllegalArgumentException("Line does not belong to this transfer: " + lineId);
        }
      }
    }

    for (StockTransferLine line : transfer.getLines()) {
      int received = overrides.getOrDefault(line.getId(), line.getQuantity());
      if (received > line.getQuantity()) {
        throw new BusinessRuleException("Received quantity exceeds shipped quantity for SKU "
            + line.getVariant().getSku());
      }
      line.setReceivedQuantity(received);
      if (received > 0) {
        inventoryService.apply(line.getVariant().getId(), transfer.getToWarehouse().getId(), received,
            MovementType.TRANSFER_IN, REF, transfer.getId(), transfer.getReferenceNumber());
      }
    }
    transfer.setStatus(Status.RECEIVED);
    transfer.setReceivedAt(LocalDateTime.now());
    return toResponse(transferRepository.save(transfer));
  }

  @Transactional
  public TransferResponse cancel(UUID id) {
    StockTransfer transfer = find(id);
    requireStatus(transfer, Status.REQUESTED, "cancelled");
    for (StockTransferLine line : transfer.getLines()) {
      inventoryService.release(line.getVariant().getId(), transfer.getFromWarehouse().getId(), line.getQuantity());
    }
    transfer.setStatus(Status.CANCELLED);
    return toResponse(transferRepository.save(transfer));
  }

  @Transactional(readOnly = true)
  public TransferResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional(readOnly = true)
  public Page<TransferResponse> list(Status status, UUID warehouseId, Pageable pageable) {
    Specification<StockTransfer> spec = (root, query, cb) -> cb.conjunction();
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.or(
          cb.equal(root.get("fromWarehouse").get("id"), warehouseId),
          cb.equal(root.get("toWarehouse").get("id"), warehouseId)));
    }
    return transferRepository.findAll(spec, pageable).map(this::toResponse);
  }

  private void requireStatus(StockTransfer transfer, Status expected, String action) {
    if (transfer.getStatus() != expected) {
      throw new IllegalStateException("Transfer " + transfer.getReferenceNumber() + " is "
          + transfer.getStatus() + " and cannot be " + action);
    }
  }

  private Warehouse activeWarehouse(UUID id) {
    Warehouse w = warehouseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
    if (!Boolean.TRUE.equals(w.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + w.getCode());
    }
    return w;
  }

  private StockTransfer find(UUID id) {
    return transferRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + id));
  }

  private TransferResponse toResponse(StockTransfer t) {
    return new TransferResponse(t.getId(), t.getReferenceNumber(),
        t.getFromWarehouse().getId(), t.getFromWarehouse().getCode(),
        t.getToWarehouse().getId(), t.getToWarehouse().getCode(),
        t.getStatus().name(), t.getNotes(),
        t.getLines().stream().map(l -> new TransferLineResponse(l.getId(), l.getVariant().getId(),
            l.getVariant().getSku(), l.getQuantity(), l.getReceivedQuantity())).toList(),
        t.getDispatchedAt(), t.getReceivedAt(), t.getCreatedAt());
  }
}
```

## `src/main/java/com/apex/service/UserService.java`

```java
package com.apex.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.UserRequests.ChangePasswordRequest;
import com.apex.dto.request.UserRequests.ResetPasswordRequest;
import com.apex.dto.request.UserRequests.UpdateUserRequest;
import com.apex.dto.response.AuthResponses.UserResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.exception.UnauthorizedException;
import com.apex.mapper.UserMapper;
import com.apex.model.Role;
import com.apex.model.User;
import com.apex.repository.RefreshTokenRepository;
import com.apex.repository.RoleRepository;
import com.apex.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  public Page<UserResponse> listUsers(Pageable pageable) {
    return userRepository.findAllActiveWithRole(pageable).map(userMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public UserResponse getUser(UUID id) {
    return userMapper.toResponse(find(id));
  }

  @Transactional
  public UserResponse updateUser(UUID id, UpdateUserRequest request, UUID actingUserId) {
    User user = find(id);
    Role role = roleRepository.findById(request.roleId())
        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.roleId()));

    boolean deactivating = Boolean.FALSE.equals(request.isActive());
    if (id.equals(actingUserId) && (deactivating || !role.getId().equals(user.getRole().getId()))) {
      throw new BusinessRuleException("You cannot deactivate yourself or change your own role");
    }

    boolean roleChanged = !role.getId().equals(user.getRole().getId());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setRole(role);
    if (request.isActive() != null) {
      user.setIsActive(request.isActive());
    }
    user = userRepository.save(user);

    if (deactivating || roleChanged) {
      refreshTokenRepository.revokeAllByUserId(id);
    }
    return userMapper.toResponse(user);
  }

  @Transactional
  public void deleteUser(UUID id, UUID actingUserId) {
    if (id.equals(actingUserId)) {
      throw new BusinessRuleException("You cannot delete your own account");
    }
    User user = find(id);
    user.setDeletedAt(LocalDateTime.now());
    user.setIsActive(false);
    // Free the unique e-mail so it can be registered again.
    user.setEmail(user.getEmail() + ".deleted." + user.getId());
    userRepository.save(user);
    refreshTokenRepository.revokeAllByUserId(id);
  }

  @Transactional
  public void resetPassword(UUID id, ResetPasswordRequest request) {
    User user = find(id);
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    refreshTokenRepository.revokeAllByUserId(id);
  }

  @Transactional
  public void changePassword(UUID id, ChangePasswordRequest request) {
    User user = find(id);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Current password is incorrect");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    refreshTokenRepository.revokeAllByUserId(id);
  }

  private User find(UUID id) {
    return userRepository.findByIdWithRole(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
  }
}
```

## `src/main/java/com/apex/service/WarehouseService.java`

```java
package com.apex.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.WarehouseRequests.CreateWarehouseRequest;
import com.apex.dto.request.WarehouseRequests.LocationRequest;
import com.apex.dto.request.WarehouseRequests.UpdateWarehouseRequest;
import com.apex.dto.response.WarehouseResponses.LocationResponse;
import com.apex.dto.response.WarehouseResponses.WarehouseResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.StorageLocation;
import com.apex.model.Warehouse;
import com.apex.repository.StockLevelRepository;
import com.apex.repository.StorageLocationRepository;
import com.apex.repository.WarehouseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WarehouseService {

  private final WarehouseRepository warehouseRepository;
  private final StorageLocationRepository locationRepository;
  private final StockLevelRepository stockLevelRepository;

  @Transactional
  public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
    String code = normalizeCode(request.code());
    if (warehouseRepository.existsByCode(code)) {
      throw new DuplicateResourceException("Warehouse code already exists: " + code);
    }
    Warehouse warehouse = Warehouse.builder()
        .code(code)
        .name(request.name())
        .addressLine(request.addressLine())
        .city(request.city())
        .state(request.state())
        .postalCode(request.postalCode())
        .country(request.country())
        .contactPhone(request.contactPhone())
        .build();
    return toResponse(warehouseRepository.save(warehouse));
  }

  @Transactional(readOnly = true)
  public Page<WarehouseResponse> listWarehouses(Boolean active, Pageable pageable) {
    Page<Warehouse> page = active == null
        ? warehouseRepository.findAll(pageable)
        : warehouseRepository.findByIsActive(active, pageable);
    return page.map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public WarehouseResponse getWarehouse(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public WarehouseResponse updateWarehouse(UUID id, UpdateWarehouseRequest request) {
    Warehouse warehouse = find(id);
    warehouse.setName(request.name());
    warehouse.setAddressLine(request.addressLine());
    warehouse.setCity(request.city());
    warehouse.setState(request.state());
    warehouse.setPostalCode(request.postalCode());
    warehouse.setCountry(request.country());
    warehouse.setContactPhone(request.contactPhone());
    if (request.isActive() != null) {
      if (!request.isActive()) {
        requireNoStock(id);
      }
      warehouse.setIsActive(request.isActive());
    }
    return toResponse(warehouseRepository.save(warehouse));
  }

  /** Warehouses are deactivated, never removed: stock history references them. */
  @Transactional
  public void deactivateWarehouse(UUID id) {
    Warehouse warehouse = find(id);
    requireNoStock(id);
    warehouse.setIsActive(false);
    warehouseRepository.save(warehouse);
  }

  // ---------- Locations ----------

  @Transactional
  public LocationResponse addLocation(UUID warehouseId, LocationRequest request) {
    Warehouse warehouse = find(warehouseId);
    String code = normalizeCode(request.code());
    if (locationRepository.existsByWarehouseIdAndCode(warehouseId, code)) {
      throw new DuplicateResourceException("Location already exists in this warehouse: " + code);
    }
    StorageLocation location = StorageLocation.builder()
        .warehouse(warehouse)
        .code(code)
        .description(request.description())
        .build();
    return toResponse(locationRepository.save(location));
  }

  @Transactional(readOnly = true)
  public List<LocationResponse> listLocations(UUID warehouseId) {
    find(warehouseId);
    return locationRepository.findByWarehouseIdOrderByCodeAsc(warehouseId).stream().map(this::toResponse).toList();
  }

  @Transactional
  public LocationResponse updateLocation(UUID locationId, LocationRequest request) {
    StorageLocation location = findLocation(locationId);
    String code = normalizeCode(request.code());
    if (!code.equals(location.getCode())
        && locationRepository.existsByWarehouseIdAndCode(location.getWarehouse().getId(), code)) {
      throw new DuplicateResourceException("Location already exists in this warehouse: " + code);
    }
    location.setCode(code);
    location.setDescription(request.description());
    if (request.isActive() != null) {
      location.setIsActive(request.isActive());
    }
    return toResponse(locationRepository.save(location));
  }

  @Transactional
  public void deactivateLocation(UUID locationId) {
    StorageLocation location = findLocation(locationId);
    location.setIsActive(false);
    locationRepository.save(location);
  }

  // ---------- Helpers used by other services ----------

  /** Returns an active warehouse or fails; used when stock is moved into or out of it. */
  @Transactional(readOnly = true)
  public Warehouse requireActive(UUID id) {
    Warehouse warehouse = find(id);
    if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + warehouse.getCode());
    }
    return warehouse;
  }

  private void requireNoStock(UUID warehouseId) {
    if (stockLevelRepository.existsByWarehouseIdAndQuantityOnHandGreaterThan(warehouseId, 0)) {
      throw new IllegalStateException("Warehouse still holds stock. Transfer or write it off before deactivating.");
    }
  }

  private Warehouse find(UUID id) {
    return warehouseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
  }

  private StorageLocation findLocation(UUID id) {
    return locationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
  }

  private String normalizeCode(String code) {
    return code.trim().toUpperCase(Locale.ROOT);
  }

  private WarehouseResponse toResponse(Warehouse w) {
    return new WarehouseResponse(w.getId(), w.getCode(), w.getName(), w.getAddressLine(), w.getCity(),
        w.getState(), w.getPostalCode(), w.getCountry(), w.getContactPhone(), w.getIsActive(), w.getCreatedAt());
  }

  private LocationResponse toResponse(StorageLocation l) {
    return new LocationResponse(l.getId(), l.getWarehouse().getId(), l.getCode(), l.getDescription(), l.getIsActive());
  }
}
```

## `src/main/resources/application.properties`

```properties
spring.config.import=optional:file:.env[.properties]

spring.application.name=app

# JWT Configuration
app.jwt.secret=${JWT_SECRET}
app.jwt.ttl-seconds=${JWT_TTL_SECONDS:3600}
app.jwt.issuer=apex-backend

# CORS (frontend lives in a separate project)
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173,http://localhost:4200}

# Optional bootstrap admin (skipped when email is blank)
app.bootstrap.admin.email=${ADMIN_EMAIL:}
app.bootstrap.admin.password=${ADMIN_PASSWORD:}

# PostgreSQL Database Connection Settings
spring.datasource.url=jdbc:${DB_TYPE}://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# JPA / Hibernate Configurations
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# Sales: tax rate applied to new orders (0.10 = 10%)
app.sales.tax-rate=${SALES_TAX_RATE:0}
```

## `src/test/java/com/apex/AlertTests.java`

```java
package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class AlertTests extends IntegrationTest {

  private String rule(String variant, String wh, int point, int qty) {
    return "{\"variantId\":\"%s\",\"warehouseId\":\"%s\",\"reorderPoint\":%d,\"reorderQuantity\":%d}"
        .formatted(variant, wh, point, qty);
  }

  private String alertsFor(String admin, String wh) {
    return "/api/v1/alerts?warehouseId=" + wh;
  }

  @Test
  void alertRaisedWhenStockFallsToReorderPointAndResolvedOnRestock() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), wh = newWarehouse(admin);
    addStock(admin, variant, wh, 20);
    postAs(admin, "/api/v1/reorder-rules", rule(variant, wh, 5, 50)).andExpect(status().isCreated());
    postAs(admin, "/api/v1/reorder-rules", rule(variant, wh, 5, 50)).andExpect(status().isConflict());
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(0));

    // selling stock through an order (reservation) already counts against availability
    postAs(admin, "/api/v1/orders",
        "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":16}]}".formatted(wh, variant))
        .andExpect(status().isCreated());
    var alerts = getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value("LOW_STOCK"))
        .andExpect(jsonPath("$.content[0].availableQuantity").value(4));
    String alertId = read(alerts, "$.content[0].id");

    // dropping to zero escalates the same alert instead of opening a second one
    addStockDelta(admin, variant, wh, -4);
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value("OUT_OF_STOCK"));

    postAs(admin, "/api/v1/alerts/" + alertId + "/acknowledge", "")
        .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    getAs(admin, "/api/v1/alerts/summary").andExpect(status().isOk());

    // restocking above the reorder point resolves it
    addStock(admin, variant, wh, 30);
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(0));
    getAs(admin, "/api/v1/alerts?status=RESOLVED&warehouseId=" + wh).andExpect(jsonPath("$.content.length()").value(1));
    postAs(admin, "/api/v1/alerts/" + alertId + "/acknowledge", "").andExpect(status().isConflict());
  }

  private void addStockDelta(String admin, String variant, String wh, int delta) throws Exception {
    postAs(admin, "/api/v1/inventory/adjustments",
        "{\"variantId\":\"%s\",\"warehouseId\":\"%s\",\"quantityChange\":%d,\"reason\":\"t\"}".formatted(variant, wh, delta))
        .andExpect(status().isOk());
  }

  @Test
  void creatingRuleOnAlreadyLowStockRaisesAlertImmediately() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), wh = newWarehouse(admin);
    addStock(admin, variant, wh, 2);
    String ruleId = read(postAs(admin, "/api/v1/reorder-rules", rule(variant, wh, 5, 10)), "$.id");
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(1));

    // raising or lowering the threshold re-evaluates; deleting the rule clears the alert
    putAs(admin, "/api/v1/reorder-rules/" + ruleId, rule(variant, wh, 1, 10)).andExpect(status().isOk());
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(0));
    putAs(admin, "/api/v1/reorder-rules/" + ruleId, rule(variant, wh, 5, 10));
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(1));
    deleteAs(admin, "/api/v1/reorder-rules/" + ruleId).andExpect(status().isNoContent());
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void suggestionsListLowItemsWithPreferredSupplier() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), wh = newWarehouse(admin);
    String supplier = read(postAs(admin, "/api/v1/suppliers", "{\"code\":\"" + unique("s") + "\",\"name\":\"Prime Supply\"}"), "$.id");
    postAs(admin, "/api/v1/suppliers/" + supplier + "/products", "{\"variantId\":\"" + variant + "\",\"unitCost\":1.75}");
    addStock(admin, variant, wh, 1);
    postAs(admin, "/api/v1/reorder-rules", rule(variant, wh, 3, 40));

    getAs(admin, "/api/v1/reorder-rules/suggestions?warehouseId=" + wh)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].suggestedQuantity").value(40))
        .andExpect(jsonPath("$[0].preferredSupplierName").value("Prime Supply"))
        .andExpect(jsonPath("$[0].estimatedUnitCost").value(1.75));
  }

  @Test
  void permissions() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), wh = newWarehouse(admin);
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/reorder-rules", rule(variant, wh, 1, 1)).andExpect(status().isForbidden());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/alerts").andExpect(status().isOk());
    getAs(tokenFor("CASHIER"), "/api/v1/alerts").andExpect(status().isForbidden());
  }
}
```

## `src/test/java/com/apex/ApplicationTests.java`

```java
package com.apex;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class ApplicationTests extends IntegrationTest {

  @Test
  void contextLoads() {
  }
}
```

## `src/test/java/com/apex/CatalogCompletionTests.java`

```java
package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class CatalogCompletionTests extends IntegrationTest {

  private String category(String admin) throws Exception {
    return read(postAs(admin, "/api/v1/categories", "{\"name\":\"" + unique("Cat") + "\"}"), "$.id");
  }

  @Test
  void productAndVariantCanBeUpdatedAndVariantDeactivated() throws Exception {
    String admin = adminToken();
    String cat = category(admin);
    String cat2 = category(admin);
    var created = postAs(admin, "/api/v1/products", """
        {"categoryId":"%s","name":"Shirt","unitOfMeasure":"piece",
         "variants":[{"sku":"%s","costPrice":5,"retailPrice":10}]}""".formatted(cat, unique("SH")));
    String productId = read(created, "$.id");
    String variantId = read(created, "$.variants[0].id");

    putAs(admin, "/api/v1/products/" + productId, """
        {"categoryId":"%s","name":"Polo Shirt","unitOfMeasure":"piece","brand":"Apex","isActive":true}""".formatted(cat2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Polo Shirt"))
        .andExpect(jsonPath("$.categoryId").value(cat2));

    putAs(admin, "/api/v1/products/variants/" + variantId, """
        {"costPrice":6,"retailPrice":12.5,"weight":{"value":0.2,"unit":"kg"},"variantAttributes":{"size":"M"}}""")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.retailPrice").value(12.5))
        .andExpect(jsonPath("$.weight.unit").value("kg"));

    deleteAs(admin, "/api/v1/products/variants/" + variantId).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/products/variants/sku/" + read(created, "$.variants[0].sku"))
        .andExpect(jsonPath("$.isActive").value(false));
  }

  @Test
  void mediaCanBeAttachedAndOnlyOnePrimaryRemains() throws Exception {
    String admin = adminToken();
    String productId = read(postAs(admin, "/api/v1/products", """
        {"categoryId":"%s","name":"Lamp","unitOfMeasure":"piece"}""".formatted(category(admin))), "$.id");

    String m1 = read(postAs(admin, "/api/v1/products/" + productId + "/media",
        "{\"url\":\"https://cdn/a.jpg\",\"fileName\":\"a.jpg\",\"mimeType\":\"image/jpeg\",\"isPrimary\":true}")
        .andExpect(status().isCreated()), "$.id");
    postAs(admin, "/api/v1/products/" + productId + "/media",
        "{\"url\":\"https://cdn/b.jpg\",\"fileName\":\"b.jpg\",\"mimeType\":\"image/jpeg\",\"isPrimary\":true,\"sortOrder\":1}")
        .andExpect(status().isCreated());

    getAs(admin, "/api/v1/products/" + productId + "/media")
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].isPrimary").value(false))
        .andExpect(jsonPath("$[1].isPrimary").value(true));
    getAs(admin, "/api/v1/products/" + productId).andExpect(jsonPath("$.mediaUrls.length()").value(2));

    deleteAs(admin, "/api/v1/products/media/" + m1).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/products/" + productId + "/media").andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void barcodesKeepASinglePrimaryAndCanBeRemoved() throws Exception {
    String admin = adminToken();
    var created = postAs(admin, "/api/v1/products", """
        {"categoryId":"%s","name":"Gum","unitOfMeasure":"pack",
         "variants":[{"sku":"%s","costPrice":1,"retailPrice":2,"barcodes":[{"barcode":"%s","type":"EAN_13"}]}]}"""
        .formatted(category(admin), unique("GM"), unique("111")));
    String variantId = read(created, "$.variants[0].id");

    var second = postAs(admin, "/api/v1/products/variants/" + variantId + "/barcodes",
        "{\"barcode\":\"" + unique("222") + "\",\"type\":\"UPC_A\",\"isPrimary\":true}").andExpect(status().isCreated());
    getAs(admin, "/api/v1/products/variants/sku/" + read(created, "$.variants[0].sku"))
        .andExpect(jsonPath("$.barcodes[?(@.isPrimary==true)]", org.hamcrest.Matchers.hasSize(1)));

    deleteAs(admin, "/api/v1/products/barcodes/id/" + read(second, "$.id")).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/products/variants/sku/" + read(created, "$.variants[0].sku"))
        .andExpect(jsonPath("$.barcodes.length()").value(1))
        .andExpect(jsonPath("$.barcodes[0].isPrimary").value(true));
  }

  @Test
  void categoriesCanBeMovedButNotIntoTheirOwnSubtree() throws Exception {
    String admin = adminToken();
    String root = category(admin);
    String child = read(postAs(admin, "/api/v1/categories",
        "{\"name\":\"" + unique("Child") + "\",\"parentId\":\"" + root + "\"}"), "$.id");
    String other = category(admin);

    patchAs(admin, "/api/v1/categories/" + root + "/parent", "{\"parentId\":\"" + child + "\"}")
        .andExpect(status().isBadRequest());
    patchAs(admin, "/api/v1/categories/" + child + "/parent", "{\"parentId\":\"" + other + "\"}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.parentId").value(other));
    patchAs(admin, "/api/v1/categories/" + child + "/parent", "{}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.parentId").doesNotExist());
  }
}
```

## `src/test/java/com/apex/FulfilmentTests.java`

```java
package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class FulfilmentTests extends IntegrationTest {

  private String newOrder(String admin, String wh, String variant, int qty) throws Exception {
    return read(postAs(admin, "/api/v1/orders",
        "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":%d}]}".formatted(wh, variant, qty))
        .andExpect(status().isCreated()), "$.id");
  }

  @Test
  void happyPathPickPackShipDeliver() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "4", "10");
    addStock(admin, variant, wh, 10);
    String id = newOrder(admin, wh, variant, 4);

    // cannot pack or ship before picking
    postAs(admin, "/api/v1/orders/" + id + "/pack", "").andExpect(status().isConflict());
    postAs(admin, "/api/v1/orders/" + id + "/ship", "").andExpect(status().isConflict());

    postAs(admin, "/api/v1/orders/" + id + "/pick", "").andExpect(jsonPath("$.status").value("PICKING"))
        .andExpect(jsonPath("$.lines[0].quantityPicked").value(4));
    postAs(admin, "/api/v1/orders/" + id + "/pack", "").andExpect(jsonPath("$.status").value("PACKED"));
    assertEquals(10, onHand(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/ship", "{\"carrier\":\"DHL\",\"trackingNumber\":\"TRK1\"}")
        .andExpect(jsonPath("$.status").value("SHIPPED")).andExpect(jsonPath("$.trackingNumber").value("TRK1"));
    assertEquals(6, onHand(admin, variant, wh));
    assertEquals(0, reserved(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/cancel", "").andExpect(status().isConflict());
    postAs(admin, "/api/v1/orders/" + id + "/deliver", "").andExpect(jsonPath("$.status").value("DELIVERED"));
    getAs(admin, "/api/v1/inventory/movements?type=SALE&variantId=" + variant)
        .andExpect(jsonPath("$.content[0].quantityChange").value(-4));
  }

  @Test
  void partialPickBlocksPacking() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 10);
    var created = postAs(admin, "/api/v1/orders",
        "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":5}]}".formatted(wh, variant));
    String id = read(created, "$.id"), lineId = read(created, "$.lines[0].id");

    postAs(admin, "/api/v1/orders/" + id + "/pick", "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"quantity\":3}]}");
    postAs(admin, "/api/v1/orders/" + id + "/pack", "").andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/orders/" + id + "/pick", "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"quantity\":6}]}")
        .andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/orders/" + id + "/pick", "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"quantity\":5}]}");
    postAs(admin, "/api/v1/orders/" + id + "/pack", "").andExpect(status().isOk());
  }

  @Test
  void cancellingReleasesReservedStock() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 5);
    String id = newOrder(admin, wh, variant, 5);
    assertEquals(5, reserved(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/cancel", "{\"reason\":\"customer changed mind\"}")
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.notes").value(org.hamcrest.Matchers.containsString("customer changed mind")));
    assertEquals(0, reserved(admin, variant, wh));
    assertEquals(5, onHand(admin, variant, wh));
    newOrder(admin, wh, variant, 5); // stock is sellable again
    postAs(admin, "/api/v1/orders/" + id + "/pick", "").andExpect(status().isConflict());
  }

  @Test
  void returnsRestockOrWriteOffAndRefundIncludesDiscount() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "4", "10");
    addStock(admin, variant, wh, 10);
    var created = postAs(admin, "/api/v1/orders", """
        {"warehouseId":"%s","lines":[{"variantId":"%s","quantity":4,"discountPercent":50}]}""".formatted(wh, variant));
    String id = read(created, "$.id"), lineId = read(created, "$.lines[0].id");

    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":1}]}").andExpect(status().isConflict()); // not shipped

    postAs(admin, "/api/v1/orders/" + id + "/pick", "");
    postAs(admin, "/api/v1/orders/" + id + "/pack", "");
    postAs(admin, "/api/v1/orders/" + id + "/ship", "");
    assertEquals(6, onHand(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"reason\":\"faulty\",\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":1}]}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.refundAmount").value(5.0))   // 10 * 50% off
        .andExpect(jsonPath("$.restocked").value(true));
    assertEquals(7, onHand(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"restock\":false,\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":2}]}")
        .andExpect(jsonPath("$.refundAmount").value(10.0));
    assertEquals(7, onHand(admin, variant, wh)); // written off, not restocked

    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":2}]}").andExpect(status().isUnprocessableEntity());
    getAs(admin, "/api/v1/orders/" + id + "/returns").andExpect(jsonPath("$.length()").value(2));
    getAs(admin, "/api/v1/orders/" + id).andExpect(jsonPath("$.lines[0].quantityReturned").value(3));
  }

  @Test
  void warehouseStaffCanFulfilButNotCancel() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 2);
    String id = newOrder(admin, wh, variant, 1);
    String staff = tokenFor("WAREHOUSE_STAFF");
    postAs(staff, "/api/v1/orders/" + id + "/pick", "").andExpect(status().isOk());
    postAs(staff, "/api/v1/orders/" + id + "/cancel", "").andExpect(status().isForbidden());
  }
}
```

## `src/test/java/com/apex/IdentityTests.java`

```java
package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class IdentityTests extends IntegrationTest {

  private String roleId(String name) {
    return roleRepository.findByName(name).orElseThrow().getId().toString();
  }

  private String newUser(String admin, String email, String role) throws Exception {
    return read(postAs(admin, "/api/v1/auth/register", """
        {"firstName":"U","lastName":"X","email":"%s","password":"longenough1","roleId":"%s"}"""
        .formatted(email, roleId(role))).andExpect(status().isCreated()), "$.user.id");
  }

  @Test
  void adminManagesUsers() throws Exception {
    String admin = adminToken();
    String email = unique("emp") + "@apex.test";
    String id = newUser(admin, email, "CASHIER");

    getAs(admin, "/api/v1/users").andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    putAs(admin, "/api/v1/users/" + id, """
        {"firstName":"New","lastName":"Name","roleId":"%s","isActive":true}""".formatted(roleId("MANAGER")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.roleName").value("MANAGER"));

    // deactivated users cannot log in
    putAs(admin, "/api/v1/users/" + id, """
        {"firstName":"New","lastName":"Name","roleId":"%s","isActive":false}""".formatted(roleId("MANAGER")))
        .andExpect(status().isOk());
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
        .contentType("application/json").content("{\"email\":\"" + email + "\",\"password\":\"longenough1\"}"))
        .andExpect(status().isUnauthorized());

    deleteAs(admin, "/api/v1/users/" + id).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/users/" + id).andExpect(status().isNotFound());
    newUser(admin, email, "CASHIER"); // e-mail is free again
  }

  @Test
  void adminCannotLockThemselvesOut() throws Exception {
    String admin = adminToken();
    String selfId = userRepository.findByEmailAndDeletedAtIsNull(ADMIN_EMAIL).orElseThrow().getId().toString();
    putAs(admin, "/api/v1/users/" + selfId, """
        {"firstName":"S","lastName":"A","roleId":"%s","isActive":false}""".formatted(roleId("ADMIN")))
        .andExpect(status().isUnprocessableEntity());
    deleteAs(admin, "/api/v1/users/" + selfId).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void passwordsCanBeChangedAndReset() throws Exception {
    String admin = adminToken();
    String email = unique("pw") + "@apex.test";
    String id = newUser(admin, email, "CASHIER");
    String token = login(email, "longenough1");

    patchAs(token, "/api/v1/auth/password", "{\"currentPassword\":\"nope\",\"newPassword\":\"brandnew123\"}")
        .andExpect(status().isUnauthorized());
    patchAs(token, "/api/v1/auth/password", "{\"currentPassword\":\"longenough1\",\"newPassword\":\"brandnew123\"}")
        .andExpect(status().isNoContent());
    login(email, "brandnew123");

    patchAs(token, "/api/v1/users/" + id + "/password", "{\"newPassword\":\"sneaky12345\"}")
        .andExpect(status().isForbidden());
    patchAs(admin, "/api/v1/users/" + id + "/password", "{\"newPassword\":\"resetbyadmin1\"}")
        .andExpect(status().isNoContent());
    login(email, "resetbyadmin1");
  }

  @Test
  void rolesCanBeManagedButBuiltInsAreProtected() throws Exception {
    String admin = adminToken();
    getAs(admin, "/api/v1/roles/permissions").andExpect(status().isOk());

    String name = unique("auditor").replace("-", "_").toUpperCase();
    var created = postAs(admin, "/api/v1/roles",
        "{\"name\":\"" + name + "\",\"permissions\":[\"product:read\",\"report:read\"]}")
        .andExpect(status().isCreated());
    String id = read(created, "$.id");

    postAs(admin, "/api/v1/roles", "{\"name\":\"bad\",\"permissions\":[\"made:up\"]}")
        .andExpect(status().isBadRequest());
    putAs(admin, "/api/v1/roles/" + id, "{\"name\":\"" + name + "\",\"permissions\":[\"product:read\"]}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.permissions.length()").value(1));

    putAs(admin, "/api/v1/roles/" + roleId("CASHIER"), "{\"name\":\"RENAMED\",\"permissions\":[\"product:read\"]}")
        .andExpect(status().isUnprocessableEntity());
    putAs(admin, "/api/v1/roles/" + roleId("ADMIN"), "{\"name\":\"ADMIN\",\"permissions\":[\"product:read\"]}")
        .andExpect(status().isUnprocessableEntity());
    deleteAs(admin, "/api/v1/roles/" + roleId("MANAGER")).andExpect(status().isUnprocessableEntity());

    newUser(admin, unique("aud") + "@apex.test", name);
    deleteAs(admin, "/api/v1/roles/" + id).andExpect(status().isConflict());
    getAs(tokenFor("CASHIER"), "/api/v1/roles").andExpect(status().isForbidden());
  }
}
```

## `src/test/java/com/apex/InventoryTests.java`

```java
package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.apex.model.StockMovement.MovementType;
import com.apex.service.InventoryService;
import com.apex.support.IntegrationTest;

class InventoryTests extends IntegrationTest {

  @Autowired InventoryService inventoryService;
  @Autowired TransactionTemplate tx;

  @Test
  void adjustmentsUpdateStockAndWriteLedger() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);

    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, 50, "opening stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantityOnHand").value(50))
        .andExpect(jsonPath("$.quantityAvailable").value(50));
    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, -8, "broken in store"))
        .andExpect(jsonPath("$.quantityOnHand").value(42));

    getAs(admin, "/api/v1/inventory/movements?variantId=" + variant)
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].quantityAfter").value(42))
        .andExpect(jsonPath("$.content[0].quantityChange").value(-8));

    getAs(admin, "/api/v1/inventory/variants/" + variant)
        .andExpect(jsonPath("$.totalOnHand").value(42))
        .andExpect(jsonPath("$.warehouses.length()").value(1));
  }

  @Test
  void stockCannotGoNegativeAndReasonIsMandatory() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 5);

    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, -6, "too many"))
        .andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, 0, "nothing"))
        .andExpect(status().isBadRequest());
    postAs(admin, "/api/v1/inventory/adjustments",
        "{\"variantId\":\"" + variant + "\",\"warehouseId\":\"" + wh + "\",\"quantityChange\":1}")
        .andExpect(status().isBadRequest());
    assertEquals(5, onHand(admin, variant, wh));
  }

  @Test
  void stockCountSetsQuantityAndRecordsDifference() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 10);

    postAs(admin, "/api/v1/inventory/stock-counts",
        "{\"variantId\":\"" + variant + "\",\"warehouseId\":\"" + wh + "\",\"countedQuantity\":7}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.quantityOnHand").value(7));
    getAs(admin, "/api/v1/inventory/movements?variantId=" + variant + "&type=STOCK_COUNT")
        .andExpect(jsonPath("$.content[0].quantityChange").value(-3));
  }

  @Test
  void reservationsReduceAvailabilityButNotOnHand() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 10);
    java.util.UUID v = java.util.UUID.fromString(variant);
    java.util.UUID w = java.util.UUID.fromString(wh);

    tx.executeWithoutResult(s -> inventoryService.reserve(v, w, 6));
    getAs(admin, "/api/v1/inventory?variantId=" + variant)
        .andExpect(jsonPath("$.content[0].quantityOnHand").value(10))
        .andExpect(jsonPath("$.content[0].quantityReserved").value(6))
        .andExpect(jsonPath("$.content[0].quantityAvailable").value(4));

    // reserved units are protected from manual removal and further reservation
    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, -5, "oops"))
        .andExpect(status().isUnprocessableEntity());
    org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
        () -> tx.executeWithoutResult(s -> inventoryService.reserve(v, w, 5)));

    tx.executeWithoutResult(s -> inventoryService.consumeReserved(v, w, 6, MovementType.SALE, "ORDER", null, "shipped"));
    assertEquals(4, onHand(admin, variant, wh));
    assertEquals(0, reserved(admin, variant, wh));
  }

  @Test
  void concurrentReservationsNeverOversell() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 10);
    java.util.UUID v = java.util.UUID.fromString(variant);
    java.util.UUID w = java.util.UUID.fromString(wh);

    ExecutorService pool = Executors.newFixedThreadPool(8);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      Callable<Boolean> task = () -> {
        try {
          tx.executeWithoutResult(s -> inventoryService.reserve(v, w, 1));
          return true;
        } catch (Exception e) {
          return false;
        }
      };
      results.add(pool.submit(task));
    }
    long ok = 0;
    for (Future<Boolean> f : results) {
      if (f.get()) {
        ok++;
      }
    }
    pool.shutdown();
    assertEquals(10, ok);
    assertEquals(10, reserved(admin, variant, wh));
  }

  @Test
  void warehouseWithStockCannotBeDeactivated() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 3);
    deleteAs(admin, "/api/v1/warehouses/" + wh).andExpect(status().isConflict());

    addStock(admin, variant, wh, -3);
    deleteAs(admin, "/api/v1/warehouses/" + wh).andExpect(status().isNoContent());
    // stock cannot be added to an inactive warehouse
    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, 1, "late"))
        .andExpect(status().isConflict());
  }

  @Test
  void inventoryPermissionsAreEnforced() throws Exception {
    String variant = newVariant(adminToken());
    String wh = newWarehouse(adminToken());
    postAs(tokenFor("CASHIER"), "/api/v1/inventory/adjustments", adj(variant, wh, 1, "x")).andExpect(status().isForbidden());
    getAs(tokenFor("CASHIER"), "/api/v1/inventory").andExpect(status().isOk());
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/inventory/adjustments", adj(variant, wh, 1, "x")).andExpect(status().isOk());
  }

  private String adj(String variant, String wh, int delta, String reason) {
    return "{\"variantId\":\"%s\",\"warehouseId\":\"%s\",\"quantityChange\":%d,\"reason\":\"%s\"}"
        .formatted(variant, wh, delta, reason);
  }
}
```

## `src/test/java/com/apex/OrderTests.java`

```java
package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class OrderTests extends IntegrationTest {

  private String order(String wh, String variant, int qty) {
    return "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":%d}]}".formatted(wh, variant, qty);
  }

  @Test
  void customerCrud() throws Exception {
    String admin = adminToken();
    String email = unique("c") + "@shop.test";
    var created = postAs(admin, "/api/v1/customers", "{\"name\":\"Jane Doe\",\"email\":\"" + email + "\",\"phone\":\"0400\"}")
        .andExpect(status().isCreated());
    String id = read(created, "$.id");
    postAs(admin, "/api/v1/customers", "{\"name\":\"Other\",\"email\":\"" + email.toUpperCase() + "\"}")
        .andExpect(status().isConflict());
    putAs(admin, "/api/v1/customers/" + id, "{\"name\":\"Jane Smith\",\"email\":\"" + email + "\"}")
        .andExpect(jsonPath("$.name").value("Jane Smith"));
    getAs(admin, "/api/v1/customers?keyword=smith").andExpect(jsonPath("$.content.length()").value(1));
    deleteAs(admin, "/api/v1/customers/" + id).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/customers/" + id).andExpect(status().isNotFound());
  }

  @Test
  void orderReservesStockAndComputesTotals() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "4.00", "10.00");
    addStock(admin, variant, wh, 10);

    postAs(admin, "/api/v1/orders", """
        {"warehouseId":"%s","lines":[{"variantId":"%s","quantity":3,"discountPercent":10}]}""".formatted(wh, variant))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.subtotal").value(27.0))
        .andExpect(jsonPath("$.total").value(27.0))
        .andExpect(jsonPath("$.lines[0].unitPrice").value(10.0));
    assertEquals(10, onHand(admin, variant, wh));
    assertEquals(3, reserved(admin, variant, wh));
  }

  @Test
  void orderFailsAtomicallyWhenAnyLineLacksStock() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), v1 = newVariant(admin), v2 = newVariant(admin);
    addStock(admin, v1, wh, 5);
    addStock(admin, v2, wh, 1);

    postAs(admin, "/api/v1/orders", """
        {"warehouseId":"%s","lines":[{"variantId":"%s","quantity":2},{"variantId":"%s","quantity":2}]}"""
        .formatted(wh, v1, v2)).andExpect(status().isUnprocessableEntity());
    assertEquals(0, reserved(admin, v1, wh)); // first line's reservation rolled back
  }

  @Test
  void cashierCanSellButNotSeeSuppliers() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 4);
    String cashier = tokenFor("CASHIER");
    postAs(cashier, "/api/v1/orders", order(wh, variant, 1)).andExpect(status().isCreated());
    getAs(cashier, "/api/v1/orders").andExpect(status().isOk());
    getAs(cashier, "/api/v1/suppliers").andExpect(status().isForbidden());
  }

  @Test
  void filtersAndValidation() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 4);
    String id = read(postAs(admin, "/api/v1/orders", order(wh, variant, 1)), "$.id");
    getAs(admin, "/api/v1/orders?status=PENDING&warehouseId=" + wh).andExpect(jsonPath("$.content[0].id").value(id));
    getAs(admin, "/api/v1/orders?status=SHIPPED&warehouseId=" + wh).andExpect(jsonPath("$.content.length()").value(0));
    postAs(admin, "/api/v1/orders", "{\"warehouseId\":\"" + wh + "\",\"lines\":[]}").andExpect(status().isBadRequest());
    postAs(admin, "/api/v1/orders", order(wh, variant, 0)).andExpect(status().isBadRequest());
  }
}
```

## `src/test/java/com/apex/PurchaseOrderTests.java`

```java
package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class PurchaseOrderTests extends IntegrationTest {

  private String newSupplier(String admin) throws Exception {
    return read(postAs(admin, "/api/v1/suppliers", "{\"code\":\"" + unique("s") + "\",\"name\":\"Acme\"}"), "$.id");
  }

  private String po(String supplier, String wh, String variant, int qty, String cost) {
    return "{\"supplierId\":\"%s\",\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":%d%s}]}"
        .formatted(supplier, wh, variant, qty, cost == null ? "" : ",\"unitCost\":" + cost);
  }

  @Test
  void purchaseOrderFlowFromDraftToFullyReceived() throws Exception {
    String admin = adminToken();
    String supplier = newSupplier(admin), wh = newWarehouse(admin), variant = newVariant(admin, "3.00", "6.00");
    postAs(admin, "/api/v1/suppliers/" + supplier + "/products", "{\"variantId\":\"" + variant + "\",\"unitCost\":2.50}");

    var created = postAs(admin, "/api/v1/purchase-orders", po(supplier, wh, variant, 100, null))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.lines[0].unitCost").value(2.5))   // supplier catalogue price
        .andExpect(jsonPath("$.totalAmount").value(250.0));
    String id = read(created, "$.id");
    String lineId = read(created, "$.lines[0].id");

    // draft is editable, submitted is not
    putAs(admin, "/api/v1/purchase-orders/" + id,
        "{\"warehouseId\":\"" + wh + "\",\"lines\":[{\"variantId\":\"" + variant + "\",\"quantity\":60,\"unitCost\":2.00}]}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(120.0));
    postAs(admin, "/api/v1/purchase-orders/" + id + "/receive", receive(lineId, 1)).andExpect(status().isConflict());
    postAs(admin, "/api/v1/purchase-orders/" + id + "/submit", "").andExpect(jsonPath("$.status").value("ORDERED"));
    putAs(admin, "/api/v1/purchase-orders/" + id, "{\"warehouseId\":\"" + wh + "\",\"lines\":[{\"variantId\":\"" + variant + "\",\"quantity\":1}]}")
        .andExpect(status().isConflict());

    String newLineId = read(getAs(admin, "/api/v1/purchase-orders/" + id), "$.lines[0].id");
    postAs(admin, "/api/v1/purchase-orders/" + id + "/receive", receive(newLineId, 25))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.receiptNumber").exists());
    assertEquals(25, onHand(admin, variant, wh));
    getAs(admin, "/api/v1/purchase-orders/" + id)
        .andExpect(jsonPath("$.status").value("PARTIALLY_RECEIVED"))
        .andExpect(jsonPath("$.lines[0].quantityOutstanding").value(35));

    postAs(admin, "/api/v1/purchase-orders/" + id + "/receive", receive(newLineId, 36)).andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/purchase-orders/" + id + "/cancel", "").andExpect(status().isConflict());
    postAs(admin, "/api/v1/purchase-orders/" + id + "/receive", receive(newLineId, 35));
    getAs(admin, "/api/v1/purchase-orders/" + id).andExpect(jsonPath("$.status").value("RECEIVED"));
    assertEquals(60, onHand(admin, variant, wh));
    getAs(admin, "/api/v1/purchase-orders/" + id + "/receipts").andExpect(jsonPath("$.length()").value(2));
    getAs(admin, "/api/v1/inventory/movements?type=PURCHASE_RECEIPT&variantId=" + variant)
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  void cancelAndCloseRules() throws Exception {
    String admin = adminToken();
    String supplier = newSupplier(admin), wh = newWarehouse(admin), variant = newVariant(admin);

    String a = read(postAs(admin, "/api/v1/purchase-orders", po(supplier, wh, variant, 5, "1.00")), "$.id");
    postAs(admin, "/api/v1/purchase-orders/" + a + "/cancel", "").andExpect(jsonPath("$.status").value("CANCELLED"));
    postAs(admin, "/api/v1/purchase-orders/" + a + "/submit", "").andExpect(status().isConflict());

    var b = postAs(admin, "/api/v1/purchase-orders", po(supplier, wh, variant, 5, "1.00"));
    String bId = read(b, "$.id");
    postAs(admin, "/api/v1/purchase-orders/" + bId + "/submit", "");
    postAs(admin, "/api/v1/purchase-orders/" + bId + "/receive", receive(read(b, "$.lines[0].id"), 2));
    postAs(admin, "/api/v1/purchase-orders/" + bId + "/close", "").andExpect(jsonPath("$.status").value("CLOSED"));
    getAs(admin, "/api/v1/purchase-orders?status=CLOSED&supplierId=" + supplier).andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test
  void roleChecks() throws Exception {
    String admin = adminToken();
    String supplier = newSupplier(admin), wh = newWarehouse(admin), variant = newVariant(admin);
    String id = read(postAs(admin, "/api/v1/purchase-orders", po(supplier, wh, variant, 5, "1.00")), "$.id");
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/purchase-orders", po(supplier, wh, variant, 5, "1.00")).andExpect(status().isForbidden());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/purchase-orders/" + id).andExpect(status().isOk());
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/purchase-orders/" + id + "/cancel", "").andExpect(status().isForbidden());
  }

  private String receive(String lineId, int qty) {
    return "{\"lines\":[{\"lineId\":\"%s\",\"quantity\":%d}]}".formatted(lineId, qty);
  }
}
```

## `src/test/java/com/apex/ReportTests.java`

```java
package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class ReportTests extends IntegrationTest {

  @Test
  void stockValuationUsesCostAndRetailPrices() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "3.00", "5.00");
    addStock(admin, variant, wh, 10);

    getAs(admin, "/api/v1/reports/stock-valuation?warehouseId=" + wh)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUnits").value(10))
        .andExpect(jsonPath("$.totalCostValue").value(30.0))
        .andExpect(jsonPath("$.totalRetailValue").value(50.0))
        .andExpect(jsonPath("$.rows.length()").value(1));
  }

  @Test
  void lowStockReportListsItemsBelowThreshold() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), low = newVariant(admin), plenty = newVariant(admin);
    addStock(admin, low, wh, 2);
    addStock(admin, plenty, wh, 500);
    getAs(admin, "/api/v1/reports/low-stock?threshold=5")
        .andExpect(jsonPath("$[?(@.variantId=='" + low + "')]", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[?(@.variantId=='" + plenty + "')]", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  void salesReportRecognisesRevenueOnShipmentAndSubtractsRefunds() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "4.00", "10.00");
    addStock(admin, variant, wh, 20);
    var order = postAs(admin, "/api/v1/orders",
        "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":5}]}".formatted(wh, variant));
    String id = read(order, "$.id"), lineId = read(order, "$.lines[0].id");

    String today = java.time.LocalDate.now().toString();
    // pending orders are not revenue
    getAs(admin, "/api/v1/reports/top-products?from=" + today + "&to=" + today + "&limit=100")
        .andExpect(jsonPath("$[?(@.variantId=='" + variant + "')]", org.hamcrest.Matchers.hasSize(0)));

    postAs(admin, "/api/v1/orders/" + id + "/pick", "");
    postAs(admin, "/api/v1/orders/" + id + "/pack", "");
    postAs(admin, "/api/v1/orders/" + id + "/ship", "");
    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":1}]}").andExpect(status().isCreated());

    var report = getAs(admin, "/api/v1/reports/sales?from=" + today + "&to=" + today).andExpect(status().isOk());
    report.andExpect(jsonPath("$.unitsSold").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)))
        .andExpect(jsonPath("$.days.length()").value(1));

    getAs(admin, "/api/v1/reports/top-products?from=" + today + "&to=" + today + "&limit=100")
        .andExpect(jsonPath("$[?(@.variantId=='" + variant + "')].unitsSold", org.hamcrest.Matchers.contains(5)))
        .andExpect(jsonPath("$[?(@.variantId=='" + variant + "')].revenue", org.hamcrest.Matchers.contains(50.0)));
  }

  @Test
  void dashboardSummarisesTheBusiness() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 7);
    getAs(admin, "/api/v1/dashboard").andExpect(status().isOk())
        .andExpect(jsonPath("$.activeProducts").isNumber())
        .andExpect(jsonPath("$.totalUnitsOnHand").value(org.hamcrest.Matchers.greaterThanOrEqualTo(7)))
        .andExpect(jsonPath("$.generatedAt").exists());
  }

  @Test
  void auditTrailRecordsMutationsButNotReads() throws Exception {
    String admin = adminToken();
    String name = unique("Audited");
    postAs(admin, "/api/v1/categories", "{\"name\":\"" + name + "\"}").andExpect(status().isCreated());
    getAs(admin, "/api/v1/categories").andExpect(status().isOk());
    postAs(admin, "/api/v1/categories", "{}").andExpect(status().isBadRequest()); // failures are not audited

    getAs(admin, "/api/v1/audit-logs?resource=categories&method=POST&size=200")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].userEmail").value(ADMIN_EMAIL))
        .andExpect(jsonPath("$.content[0].statusCode").value(201))
        .andExpect(jsonPath("$.content[?(@.statusCode==400)]", org.hamcrest.Matchers.hasSize(0)));
    getAs(admin, "/api/v1/audit-logs?method=GET").andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void reportsAndAuditAreRestricted() throws Exception {
    getAs(tokenFor("CASHIER"), "/api/v1/dashboard").andExpect(status().isForbidden());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/reports/stock-valuation").andExpect(status().isForbidden());
    getAs(tokenFor("MANAGER"), "/api/v1/dashboard").andExpect(status().isOk());
    getAs(tokenFor("MANAGER"), "/api/v1/audit-logs").andExpect(status().isForbidden());
  }
}
```

## `src/test/java/com/apex/SecurityAndCatalogTests.java`

```java
package com.apex;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.apex.support.IntegrationTest;

class SecurityAndCatalogTests extends IntegrationTest {

  @Test
  void healthIsPublicButEverythingElseNeedsToken() throws Exception {
    mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
    mvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void loginRejectsBadCredentials() throws Exception {
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registerIsNotOpenToAnonymousOrUnprivilegedUsers() throws Exception {
    String body = "{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"x@y.com\",\"password\":\"longenough1\",\"roleId\":\""
        + roleRepository.findByName("ADMIN").orElseThrow().getId() + "\"}";
    postAs(null, "/api/v1/auth/register", body).andExpect(status().isUnauthorized());
    postAs(tokenFor("CASHIER"), "/api/v1/auth/register", body).andExpect(status().isForbidden());
  }

  @Test
  void adminCanRegisterUsersAndEmailsAreCaseInsensitive() throws Exception {
    String email = unique("Staff") + "@Apex.Test";
    String roleId = roleRepository.findByName("CASHIER").orElseThrow().getId().toString();
    postAs(adminToken(), "/api/v1/auth/register",
        "{\"firstName\":\"S\",\"lastName\":\"T\",\"email\":\"" + email + "\",\"password\":\"longenough1\",\"roleId\":\""
            + roleId + "\"}")
        .andExpect(status().isCreated());
    login(email.toLowerCase(), "longenough1");
    postAs(adminToken(), "/api/v1/auth/register",
        "{\"firstName\":\"S\",\"lastName\":\"T\",\"email\":\"" + email.toUpperCase() + "\",\"password\":\"longenough1\",\"roleId\":\""
            + roleId + "\"}")
        .andExpect(status().isConflict());
  }

  @Test
  void cashierCanReadCatalogButNotModifyIt() throws Exception {
    String cashier = tokenFor("CASHIER");
    getAs(cashier, "/api/v1/categories").andExpect(status().isOk());
    postAs(cashier, "/api/v1/categories", "{\"name\":\"Nope\"}").andExpect(status().isForbidden());
  }

  @Test
  void productLifecycleHonoursSoftDeleteAndCategoryRules() throws Exception {
    String admin = adminToken();
    var category = postAs(admin, "/api/v1/categories", "{\"name\":\"" + unique("Drinks") + "\"}")
        .andExpect(status().isCreated());
    String categoryId = read(category, "$.id");

    String sku = unique("SKU");
    var product = postAs(admin, "/api/v1/products", """
        {"categoryId":"%s","name":"Cola","unitOfMeasure":"bottle",
         "variants":[{"sku":"%s","costPrice":1.00,"retailPrice":2.50,
                      "barcodes":[{"barcode":"%s","type":"EAN_13"}]}]}
        """.formatted(categoryId, sku, unique("890")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.description").value(""));
    String productId = read(product, "$.id");

    // duplicate SKU -> 409
    postAs(admin, "/api/v1/products/" + productId + "/variants",
        "{\"sku\":\"" + sku + "\",\"costPrice\":1,\"retailPrice\":2}").andExpect(status().isConflict());

    // category in use cannot be removed
    deleteAs(admin, "/api/v1/categories/" + categoryId).andExpect(status().isConflict());

    deleteAs(admin, "/api/v1/products/" + productId).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/products/" + productId).andExpect(status().isNotFound());
    getAs(admin, "/api/v1/products?categoryId=" + categoryId).andExpect(jsonPath("$.content.length()").value(0));
    getAs(admin, "/api/v1/products?categoryId=" + categoryId).andExpect(jsonPath("$.page.totalElements").value(0));
    // archived products still reference the category, so it can only be deactivated
    deleteAs(admin, "/api/v1/categories/" + categoryId).andExpect(status().isConflict());
    putAs(admin, "/api/v1/categories/" + categoryId, "{\"name\":\"Retired\",\"isActive\":false}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.isActive").value(false));
  }

  @Test
  void errorsAreReportedWithProperStatusCodes() throws Exception {
    String admin = adminToken();
    getAs(admin, "/api/v1/does-not-exist").andExpect(status().isNotFound());
    postAs(admin, "/api/v1/categories", "{not json").andExpect(status().isBadRequest());
    getAs(admin, "/api/v1/products?sort=nonsense,asc").andExpect(status().isBadRequest());
    getAs(admin, "/api/v1/products/not-a-uuid").andExpect(status().isBadRequest());
  }
}
```

## `src/test/java/com/apex/SupplierTests.java`

```java
package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class SupplierTests extends IntegrationTest {

  private String newSupplier(String admin) throws Exception {
    return read(postAs(admin, "/api/v1/suppliers",
        "{\"code\":\"" + unique("s") + "\",\"name\":\"Acme Wholesale\",\"email\":\"sales@acme.test\",\"leadTimeDays\":7}")
        .andExpect(status().isCreated()), "$.id");
  }

  @Test
  void supplierCrudAndValidation() throws Exception {
    String admin = adminToken();
    String code = unique("sup");
    var created = postAs(admin, "/api/v1/suppliers", "{\"code\":\"" + code + "\",\"name\":\"Globex\"}")
        .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(code.toUpperCase()));
    String id = read(created, "$.id");
    postAs(admin, "/api/v1/suppliers", "{\"code\":\"" + code + "\",\"name\":\"Again\"}").andExpect(status().isConflict());
    postAs(admin, "/api/v1/suppliers", "{\"code\":\"x\",\"name\":\"Bad\",\"email\":\"nope\"}").andExpect(status().isBadRequest());

    putAs(admin, "/api/v1/suppliers/" + id, "{\"name\":\"Globex Intl\",\"leadTimeDays\":3,\"isActive\":true}")
        .andExpect(jsonPath("$.name").value("Globex Intl"));
    getAs(admin, "/api/v1/suppliers?keyword=globex").andExpect(jsonPath("$.content.length()").value(1));
    deleteAs(admin, "/api/v1/suppliers/" + id).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/suppliers/" + id).andExpect(jsonPath("$.isActive").value(false));
  }

  @Test
  void supplierCatalogueTracksCostsAndPreferredSupplier() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String s1 = newSupplier(admin), s2 = newSupplier(admin);

    postAs(admin, "/api/v1/suppliers/" + s1 + "/products",
        "{\"variantId\":\"" + variant + "\",\"unitCost\":4.50,\"minOrderQuantity\":10}").andExpect(status().isCreated());
    postAs(admin, "/api/v1/suppliers/" + s1 + "/products",
        "{\"variantId\":\"" + variant + "\",\"unitCost\":4.50}").andExpect(status().isConflict());
    postAs(admin, "/api/v1/suppliers/" + s2 + "/products",
        "{\"variantId\":\"" + variant + "\",\"unitCost\":4.00,\"isPreferred\":true}").andExpect(status().isCreated());

    getAs(admin, "/api/v1/suppliers/by-variant/" + variant)
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].supplierId").value(s2));

    putAs(admin, "/api/v1/suppliers/" + s1 + "/products/" + variant, "{\"variantId\":\"" + variant + "\",\"unitCost\":3.00}")
        .andExpect(jsonPath("$.unitCost").value(3.0));
    deleteAs(admin, "/api/v1/suppliers/" + s1 + "/products/" + variant).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/suppliers/" + s1 + "/products").andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void warehouseStaffCannotManageSuppliers() throws Exception {
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/suppliers", "{\"code\":\"zz\",\"name\":\"Z\"}").andExpect(status().isForbidden());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/suppliers").andExpect(status().isForbidden());
  }
}
```

## `src/test/java/com/apex/TransferTests.java`

```java
package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class TransferTests extends IntegrationTest {

  private String body(String from, String to, String variant, int qty) {
    return "{\"fromWarehouseId\":\"%s\",\"toWarehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":%d}]}"
        .formatted(from, to, variant, qty);
  }

  @Test
  void fullTransferMovesStockBetweenWarehouses() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), a = newWarehouse(admin), b = newWarehouse(admin);
    addStock(admin, variant, a, 20);

    var created = postAs(admin, "/api/v1/transfers", body(a, b, variant, 8))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REQUESTED"));
    String id = read(created, "$.id");
    assertEquals(20, onHand(admin, variant, a));
    assertEquals(8, reserved(admin, variant, a));

    postAs(admin, "/api/v1/transfers/" + id + "/dispatch", "").andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    assertEquals(12, onHand(admin, variant, a));
    assertEquals(0, reserved(admin, variant, a));
    assertEquals(0, onHand(admin, variant, b));

    postAs(admin, "/api/v1/transfers/" + id + "/receive", "").andExpect(jsonPath("$.status").value("RECEIVED"));
    assertEquals(8, onHand(admin, variant, b));

    getAs(admin, "/api/v1/inventory/movements?variantId=" + variant + "&type=TRANSFER_IN")
        .andExpect(jsonPath("$.content.length()").value(1));
    postAs(admin, "/api/v1/transfers/" + id + "/receive", "").andExpect(status().isConflict());
  }

  @Test
  void partialReceiptOnlyAddsReceivedQuantity() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), a = newWarehouse(admin), b = newWarehouse(admin);
    addStock(admin, variant, a, 10);
    var created = postAs(admin, "/api/v1/transfers", body(a, b, variant, 10));
    String id = read(created, "$.id");
    String lineId = read(created, "$.lines[0].id");
    postAs(admin, "/api/v1/transfers/" + id + "/dispatch", "");

    postAs(admin, "/api/v1/transfers/" + id + "/receive",
        "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"receivedQuantity\":11}]}").andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/transfers/" + id + "/receive",
        "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"receivedQuantity\":9}]}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.lines[0].receivedQuantity").value(9));
    assertEquals(9, onHand(admin, variant, b));
  }

  @Test
  void cancelReleasesReservationAndInsufficientStockIsRejected() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), a = newWarehouse(admin), b = newWarehouse(admin);
    addStock(admin, variant, a, 5);

    postAs(admin, "/api/v1/transfers", body(a, b, variant, 6)).andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/transfers", body(a, a, variant, 1)).andExpect(status().isBadRequest());

    String id = read(postAs(admin, "/api/v1/transfers", body(a, b, variant, 5)), "$.id");
    postAs(admin, "/api/v1/transfers", body(a, b, variant, 1)).andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/transfers/" + id + "/cancel", "").andExpect(jsonPath("$.status").value("CANCELLED"));
    assertEquals(0, reserved(admin, variant, a));
    postAs(admin, "/api/v1/transfers/" + id + "/dispatch", "").andExpect(status().isConflict());
    getAs(admin, "/api/v1/transfers?status=CANCELLED").andExpect(status().isOk());
  }

  @Test
  void cashierCannotTransfer() throws Exception {
    String id = java.util.UUID.randomUUID().toString();
    postAs(tokenFor("CASHIER"), "/api/v1/transfers", body(id, java.util.UUID.randomUUID().toString(), id, 1)).andExpect(status().isForbidden());
  }
}
```

## `src/test/java/com/apex/WarehouseTests.java`

```java
package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class WarehouseTests extends IntegrationTest {

  @Test
  void warehouseLifecycle() throws Exception {
    String admin = adminToken();
    String code = unique("wh");
    var created = postAs(admin, "/api/v1/warehouses",
        "{\"code\":\"" + code + "\",\"name\":\"Main DC\",\"city\":\"Sydney\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(code.toUpperCase()));
    String id = read(created, "$.id");

    postAs(admin, "/api/v1/warehouses", "{\"code\":\"" + code.toLowerCase() + "\",\"name\":\"Dup\"}")
        .andExpect(status().isConflict());

    putAs(admin, "/api/v1/warehouses/" + id, "{\"name\":\"Main DC 2\",\"city\":\"Melbourne\",\"isActive\":true}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.city").value("Melbourne"));

    deleteAs(admin, "/api/v1/warehouses/" + id).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/warehouses?active=false").andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]", org.hamcrest.Matchers.hasSize(1)));
  }

  @Test
  void locationsAreUniquePerWarehouse() throws Exception {
    String admin = adminToken();
    String w1 = read(postAs(admin, "/api/v1/warehouses", "{\"code\":\"" + unique("a") + "\",\"name\":\"A\"}"), "$.id");
    String w2 = read(postAs(admin, "/api/v1/warehouses", "{\"code\":\"" + unique("b") + "\",\"name\":\"B\"}"), "$.id");

    var loc = postAs(admin, "/api/v1/warehouses/" + w1 + "/locations", "{\"code\":\"a-01-01\"}")
        .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("A-01-01"));
    postAs(admin, "/api/v1/warehouses/" + w1 + "/locations", "{\"code\":\"A-01-01\"}").andExpect(status().isConflict());
    postAs(admin, "/api/v1/warehouses/" + w2 + "/locations", "{\"code\":\"A-01-01\"}").andExpect(status().isCreated());

    getAs(admin, "/api/v1/warehouses/" + w1 + "/locations").andExpect(jsonPath("$.length()").value(1));
    putAs(admin, "/api/v1/warehouses/locations/" + read(loc, "$.id"), "{\"code\":\"A-01-02\",\"description\":\"top shelf\"}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("A-01-02"));
  }

  @Test
  void permissionsAreEnforced() throws Exception {
    String body = "{\"code\":\"" + unique("x") + "\",\"name\":\"X\"}";
    postAs(tokenFor("MANAGER"), "/api/v1/warehouses", body).andExpect(status().isForbidden());
    getAs(tokenFor("MANAGER"), "/api/v1/warehouses").andExpect(status().isOk());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/warehouses").andExpect(status().isOk());
    getAs(tokenFor("CASHIER"), "/api/v1/warehouses").andExpect(status().isForbidden());
  }
}
```

## `src/test/java/com/apex/support/IntegrationTest.java`

```java
package com.apex.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.apex.model.Role;
import com.apex.model.User;
import com.apex.repository.RoleRepository;
import com.apex.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Shared helpers for MockMvc based integration tests (H2 in-memory database). */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

  public static final String ADMIN_EMAIL = "admin@apex.test";
  public static final String ADMIN_PASSWORD = "Admin#12345";

  @Autowired protected MockMvc mvc;
  @Autowired protected RoleRepository roleRepository;
  @Autowired protected UserRepository userRepository;
  @Autowired protected PasswordEncoder passwordEncoder;

  protected String adminToken() throws Exception {
    return login(ADMIN_EMAIL, ADMIN_PASSWORD);
  }

  /** Creates (or reuses) a user with a built-in role and returns an access token for it. */
  protected String tokenFor(String roleName) throws Exception {
    String email = roleName.toLowerCase() + "@apex.test";
    if (!userRepository.existsByEmailAndDeletedAtIsNull(email)) {
      Role role = roleRepository.findByName(roleName).orElseThrow();
      userRepository.save(User.builder().firstName(roleName).lastName("User").email(email)
          .passwordHash(passwordEncoder.encode("Passw0rd!x")).role(role).build());
    }
    return login(email, "Passw0rd!x");
  }

  protected String login(String email, String password) throws Exception {
    String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  protected ResultActions getAs(String token, String url) throws Exception {
    return mvc.perform(auth(get(url), token));
  }

  protected ResultActions postAs(String token, String url, String json) throws Exception {
    return mvc.perform(auth(post(url), token).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  protected ResultActions putAs(String token, String url, String json) throws Exception {
    return mvc.perform(auth(put(url), token).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  protected ResultActions patchAs(String token, String url, String json) throws Exception {
    return mvc.perform(auth(patch(url), token).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  protected ResultActions deleteAs(String token, String url) throws Exception {
    return mvc.perform(auth(delete(url), token));
  }

  private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
    return token == null ? b : b.header("Authorization", "Bearer " + token);
  }

  protected static String read(ResultActions result, String path) throws Exception {
    Object v = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    return String.valueOf(v);
  }

  protected static String unique(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  // ---------- fixtures ----------

  protected String newCategory(String token) throws Exception {
    return read(postAs(token, "/api/v1/categories", "{\"name\":\"" + unique("Cat") + "\"}"), "$.id");
  }

  /** Creates a product with one variant and returns the variant id. */
  protected String newVariant(String token) throws Exception {
    return newVariant(token, "1.00", "2.00");
  }

  protected String newVariant(String token, String cost, String retail) throws Exception {
    var created = postAs(token, "/api/v1/products", """
        {"categoryId":"%s","name":"Item %s","unitOfMeasure":"piece",
         "variants":[{"sku":"%s","costPrice":%s,"retailPrice":%s}]}"""
        .formatted(newCategory(token), unique("p"), unique("SKU"), cost, retail));
    return read(created, "$.variants[0].id");
  }

  protected String newWarehouse(String token) throws Exception {
    return read(postAs(token, "/api/v1/warehouses", "{\"code\":\"" + unique("wh") + "\",\"name\":\"WH\"}"), "$.id");
  }

  protected void addStock(String token, String variantId, String warehouseId, int qty) throws Exception {
    postAs(token, "/api/v1/inventory/adjustments", """
        {"variantId":"%s","warehouseId":"%s","quantityChange":%d,"reason":"test stock"}"""
        .formatted(variantId, warehouseId, qty));
  }

  protected int onHand(String token, String variantId, String warehouseId) throws Exception {
    var r = getAs(token, "/api/v1/inventory?variantId=" + variantId + "&warehouseId=" + warehouseId);
    String body = r.andReturn().getResponse().getContentAsString();
    java.util.List<Integer> v = JsonPath.read(body, "$.content[*].quantityOnHand");
    return v.isEmpty() ? 0 : v.get(0);
  }

  protected int reserved(String token, String variantId, String warehouseId) throws Exception {
    var r = getAs(token, "/api/v1/inventory?variantId=" + variantId + "&warehouseId=" + warehouseId);
    java.util.List<Integer> v = JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.content[*].quantityReserved");
    return v.isEmpty() ? 0 : v.get(0);
  }
}
```

## `src/test/resources/application.properties`

```properties
spring.config.import=
spring.application.name=app-test

app.jwt.secret=test-secret-test-secret-test-secret-test-secret
app.jwt.ttl-seconds=3600
app.jwt.issuer=apex-backend
app.bootstrap.admin.email=admin@apex.test
app.bootstrap.admin.password=Admin#12345
app.cors.allowed-origins=http://localhost:3000

spring.datasource.url=jdbc:h2:mem:apex;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.open-in-view=false
```

# Part 2: Frontend (apex-frontend)

## `.claude/launch.json`

```json
{
  "version": "0.0.1",
  "configurations": [
    { "name": "apex-frontend", "runtimeExecutable": "bun", "runtimeArgs": ["run", "dev"], "port": 3000 }
  ]
}
```

## `.cta.json`

```json
{
  "bundler": "vite",
  "projectName": "apex-frontend",
  "mode": "file-router",
  "typescript": true,
  "packageManager": "bun",
  "includeExamples": false,
  "tailwind": true,
  "projectPreset": "default",
  "routerOnly": true,
  "addOnOptions": {},
  "git": true,
  "install": true,
  "intent": true,
  "version": 1,
  "framework": "react",
  "chosenAddOns": []
}
```

## `.env.example`

```
# Base URL of the Apex backend API
VITE_API_URL=http://localhost:8080/api/v1
```

## `.gitignore`

```
node_modules
.DS_Store
dist
dist-ssr
*.local
.env
.nitro
.tanstack
.wrangler
.output
.vinxi
__unconfig*
todos.json
```

## `.vscode/settings.json`

```json
{
  "files.watcherExclude": {
    "**/routeTree.gen.ts": true
  },
  "search.exclude": {
    "**/routeTree.gen.ts": true
  },
  "files.readonlyInclude": {
    "**/routeTree.gen.ts": true
  }
}
```

## `AGENTS.md`

```markdown
<!-- intent-skills:start -->
## Skill Loading

Use the repository’s installed Intent. If it is unavailable, report the missing dependency instead of downloading a replacement.
Before editing files for a substantial task:
- Run `bunx --no-install --package @tanstack/intent intent list` from the workspace root to see available local skills.
- If a listed skill matches the task, run `bunx --no-install --package @tanstack/intent intent load <package>#<skill>` before changing files.
- Use the loaded `SKILL.md` guidance while making the change.
- Monorepos: when working across packages, run the skill check from the workspace root and prefer the local skill for the package being changed.
- Multiple matches: prefer the most specific local skill for the package or concern you are changing; load additional skills only when the task spans multiple packages or concerns.
<!-- intent-skills:end -->
```

## `README.md`

````markdown
# Apex Frontend

Responsive web client for the Apex retail inventory backend (`apex-backend`, Spring Boot, `/api/v1`, JWT).

Stack: React 19, Vite, TanStack Router (file routes) + Query, Tailwind CSS 4, shadcn/ui (Radix, lucide icons),
fonts via `@fontsource-variable/inter` and `@fontsource-variable/jetbrains-mono`.

## Run

```bash
cp .env.example .env   # VITE_API_URL, default http://localhost:8080/api/v1
bun install
bun run dev            # http://localhost:3000
bun run build
```

The backend must allow `http://localhost:3000` in `CORS_ALLOWED_ORIGINS`.

## Structure

- `src/routes`: file routes. `_authenticated/` holds every page behind the login guard.
- `src/auth.tsx`: session state, `can(...permissions)`; tokens persist in localStorage and refresh on 401 (`src/lib/api.ts`).
- `src/lib/queries.ts`: TanStack Query options per endpoint; `src/lib/types.ts` mirrors backend DTOs.
- `src/components/ui`: shadcn components (add more with `bunx shadcn@latest add <name>`).
- `src/components/layout/nav.ts`: navigation; items are hidden without the listed permissions.

## Delivery

Agile, sprint by sprint: see [docs/AGILE_BACKLOG.md](docs/AGILE_BACKLOG.md). Sprints 0 to 19 are done (foundation, auth,
app shell, dashboard, products, inventory, alerts, orders, procurement, transfers, customers, warehouses, users/roles, audit log). Product, supplier, user and sales-order forms are included; see the backlog for what remains.

## Licence

MIT, see [LICENSE](LICENSE).
````

## `components.json`

```json
{
  "$schema": "https://ui.shadcn.com/schema.json",
  "style": "new-york",
  "rsc": false,
  "tsx": true,
  "tailwind": {
    "config": "",
    "css": "src/styles.css",
    "baseColor": "neutral",
    "cssVariables": true,
    "prefix": ""
  },
  "iconLibrary": "lucide",
  "aliases": {
    "components": "@/components",
    "utils": "@/lib/utils",
    "ui": "@/components/ui",
    "lib": "@/lib",
    "hooks": "@/hooks"
  }
}
```

## `docs/AGILE_BACKLOG.md`

```markdown
# Apex Frontend: Agile Backlog

React 19 + TanStack Router + Tailwind CSS 4 + shadcn/ui, talking to the Apex backend (`/api/v1`, JWT bearer).
Backend API reference: `apex-backend/docs/API.md`. Delivered in short sprints; each sprint is one or more commits
named `Sprint N: ...`. Mobile-first and responsive throughout; permission-aware navigation (RBAC from the JWT user).

## Epics

| # | Epic | Backend area |
|---|------|--------------|
| F1 | Foundation & design system | Tailwind, shadcn, fontsource, theme, API client |
| F2 | Identity & access | Login, session refresh, route guards, users, roles, profile |
| F3 | Dashboard & reports | Dashboard KPIs, stock valuation, sales, top products |
| F4 | Catalogue | Categories, products, variants, barcodes |
| F5 | Inventory & warehouses | Stock levels, movements, adjustments, transfers, warehouses |
| F6 | Procurement | Suppliers, purchase orders, goods receipt |
| F7 | Sales & fulfilment | Customers, orders, pick/pack/ship, returns |
| F8 | Alerts & audit | Alerts, reorder rules, suggestions, audit log |

## Definition of done
- Typechecks and `bun run build` passes.
- Works at 375px and desktop widths; keyboard accessible; light and dark themes.
- Respects permissions (hidden nav, guarded routes, disabled actions).
- Committed with a sprint-scoped message.

## Sprints

| Sprint | Epic | Goal | Status |
|--------|------|------|--------|
| 0 | F1 | Project scaffold (Vite, TanStack Router, Tailwind) | Done |
| 1 | F1 | Tailwind theme, shadcn/ui, fontsource fonts, typed API client, query client | Done |
| 2 | F2 | Login, token storage and refresh, route guards, permission helpers | Done |
| 3 | F1, F2 | Responsive app shell: sidebar, mobile sheet nav, header, theme toggle, user menu | Done |
| 4 | F3 | Dashboard KPIs, sales trend, top products, open alerts | Done |
| 5 | F4 | Products list with search/filter/paging, product detail with variants | Done |
| 6 | F5 | Stock levels, movement ledger, adjustments, warehouses | Done |
| 7 | F8 | Alerts and replenishment suggestions | Done |
| 8 | F7 | Sales orders list/detail and fulfilment actions | Done |
| 9 | F6 | Suppliers and purchase orders | Done |
| 10 | F2, F8 | Users, roles, audit log (read-only views) | Done |
| 11 | F4 | Reusable form dialog; create/edit product, add/edit variant | Done |
| 12 | F6 | Create/edit supplier | Done |
| 13 | F2 | Create user, edit user (role, status), reset password | Done |
| 14 | F7 | New sales order (customer, warehouse, lines) and customer creation | Done |
| 15 | F1, F2 | Smoke test against backend; replace next-themes (React 19 script warning); permission route guards; layout fixes | Done |
| 16 | F7, F5 | Customers and warehouses pages (create/edit) | Done |
| 17 | F6 | Create purchase order; shared variant picker and line editor | Done |
| 18 | F5, F8 | Stock transfers (create, dispatch, receive, cancel) and reorder rules | Done |
| 19 | F7, F2 | Returns against orders; role management | Done |

## Backlog (stories)

- F1: As a developer I have a typed API client that attaches the token, refreshes on 401, and surfaces `{status,message}` errors.
- F2: As a user I can sign in, stay signed in across reloads, and be redirected to login when my session ends.
- F2: As a user I only see navigation and actions I have permission for.
- F3: As a manager I see today's orders and revenue, stock value and open alerts at a glance.
- F4: As staff I can search products by keyword/category and open a product to see variants, prices and barcodes.
- F5: As warehouse staff I can see stock per warehouse, review the movement ledger and record an adjustment.
- F8: As a manager I can see open alerts and replenishment suggestions.
- F7: As staff I can follow an order through pick, pack, ship and deliver.
- F6: As a buyer I can see purchase orders and their receipt progress.
- F2: As an admin I can manage users and roles; F8: review the audit trail.

Smoke test (backend on in-memory H2, admin and a warehouse-staff user): login/logout, restricted dashboard and nav,
product create, stock and alerts, new order with tax, pick/pack/ship, purchase order submit and partial receipt,
user create, audit log, mobile drawer and no horizontal overflow at 375px.

Smoke test 2 (sprints 16-19, H2 backend): return recorded with refund incl. tax and restock; transfer
create (insufficient-stock error shown, then success), dispatch, receive; reorder rule (unknown SKU error, then
create); custom role with chosen permissions. Pages render for customers, warehouses and new purchase order; the
purchase order submit, customer and warehouse dialogs were not exercised.

## Next (unscheduled)

- Remaining write flows: categories, storage locations, supplier catalogue (supplier products), product media and barcodes, partial picking and partial transfer receipt, change own password.
- Reports pages (stock valuation, sales).
- Tests (Vitest + Testing Library) and CI.
```

## `index.html`

```html

<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Apex</title>
    <script>
      try {
        var t = localStorage.getItem('apex.theme') || 'system'
        if (t === 'dark' || (t === 'system' && matchMedia('(prefers-color-scheme: dark)').matches)) document.documentElement.classList.add('dark')
      } catch (e) {}
    </script>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

## `package.json`

```json
{
  "name": "apex-frontend",
  "private": true,
  "license": "MIT",
  "type": "module",
  "imports": {
    "#/*": "./src/*"
  },
  "scripts": {
    "dev": "vite dev --port 3000",
    "generate-routes": "tsr generate",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@fontsource-variable/inter": "^5.3.0",
    "@fontsource-variable/jetbrains-mono": "^5.3.0",
    "@tailwindcss/vite": "^4.1.18",
    "@tanstack/react-devtools": "0.10.14",
    "@tanstack/react-query": "^5.105.0",
    "@tanstack/react-router": "1.170.46",
    "@tanstack/react-router-devtools": "1.167.2",
    "class-variance-authority": "^0.7.1",
    "clsx": "^2.1.1",
    "lucide-react": "^1.66.0",
    "radix-ui": "^1.8.1",
    "react": "^19.2.0",
    "react-dom": "^19.2.0",
    "sonner": "^2.0.8",
    "tailwind-merge": "^3.7.0",
    "tailwindcss": "^4.1.18",
    "tw-animate-css": "^1.4.0",
    "zod": "^4.6.7"
  },
  "devDependencies": {
    "@tanstack/devtools-vite": "0.8.6",
    "@tanstack/router-cli": "^1.132.0",
    "@tanstack/router-plugin": "1.168.48",
    "@types/node": "^22.10.2",
    "@types/react": "^19.2.0",
    "@types/react-dom": "^19.2.0",
    "@vitejs/plugin-react": "^6.0.1",
    "typescript": "^6.0.2",
    "vite": "^8.0.0"
  },
  "pnpm": {
    "onlyBuiltDependencies": [
      "esbuild",
      "lightningcss"
    ]
  },
  "intent": {
    "skills": [
      "*"
    ]
  }
}
```

## `src/auth.tsx`

```tsx
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, loadSession, saveSession, setSessionExpiredHandler } from '#/lib/api'
import type { AuthResponse, User } from '#/lib/types'

export interface AuthState {
  status: 'loading' | 'authenticated' | 'anonymous'
  isAuthenticated: boolean
  user: User | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  can: (...permissions: string[]) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [status, setStatus] = useState<AuthState['status']>(() => (loadSession() ? 'loading' : 'anonymous'))

  useEffect(() => {
    setSessionExpiredHandler(() => {
      setUser(null)
      setStatus('anonymous')
    })
    if (!loadSession()) return
    api
      .get<User>('/auth/me')
      .then((me) => {
        setUser(me)
        setStatus('authenticated')
      })
      .catch(() => {
        saveSession(null)
        setStatus('anonymous')
      })
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.public.post<AuthResponse>('/auth/login', { email, password })
    saveSession({ accessToken: res.accessToken, refreshToken: res.refreshToken })
    setUser(res.user)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(async () => {
    const session = loadSession()
    if (session) {
      await api.public.post('/auth/logout', { refreshToken: session.refreshToken }).catch(() => {})
    }
    saveSession(null)
    setUser(null)
    setStatus('anonymous')
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      status,
      isAuthenticated: status === 'authenticated',
      user,
      login,
      logout,
      can: (...permissions) => !!user && permissions.every((p) => user.permissions.includes(p)),
    }),
    [status, user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
```

## `src/components/admin/role-dialog.tsx`

```tsx
import { useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { api } from '#/lib/api'
import { permissionsQuery } from '#/lib/queries'
import type { Role } from '#/lib/types'

export function RoleDialog({ open, onOpenChange, role }: { open: boolean; onOpenChange: (o: boolean) => void; role?: Role }) {
  const queryClient = useQueryClient()
  const { data: catalogue } = useQuery({ ...permissionsQuery, enabled: open })
  const [name, setName] = useState('')
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  useEffect(() => {
    if (open) {
      setName(role?.name ?? '')
      setSelected(new Set(role?.permissions ?? []))
      setError(null)
    }
  }, [open, role])

  // Group "resource:action" permissions by resource.
  const groups = new Map<string, string[]>()
  for (const p of catalogue ?? []) {
    const [resource] = p.split(':')
    groups.set(resource, [...(groups.get(resource) ?? []), p])
  }

  const toggle = (p: string) =>
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(p)) next.delete(p)
      else next.add(p)
      return next
    })

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setPending(true)
    setError(null)
    try {
      const body = { name: name.trim(), permissions: [...selected] }
      if (role) await api.put(`/roles/${role.id}`, body)
      else await api.post('/roles', body)
      toast.success(role ? 'Role updated' : 'Role created')
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      onOpenChange(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
    } finally {
      setPending(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-xl">
        <form onSubmit={onSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{role ? `Edit ${role.name.replaceAll('_', ' ')}` : 'New role'}</DialogTitle>
            <DialogDescription>Choose what people with this role may do.</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="role-name">Name</Label>
            <Input id="role-name" value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} disabled={role?.builtIn} />
            {role?.builtIn && <p className="text-xs text-muted-foreground">Built-in role names cannot be changed.</p>}
          </div>
          <div className="space-y-3">
            {[...groups.entries()].map(([resource, perms]) => (
              <fieldset key={resource} className="rounded-md border p-3">
                <legend className="px-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">{resource}</legend>
                <div className="flex flex-wrap gap-x-4 gap-y-2">
                  {perms.map((p) => (
                    <label key={p} className="flex items-center gap-2 text-sm">
                      <input type="checkbox" className="size-4 accent-primary" aria-label={p} checked={selected.has(p)} onChange={() => toggle(p)} />
                      {p.split(':')[1]}
                    </label>
                  ))}
                </div>
              </fieldset>
            ))}
          </div>
          {error && (
            <p role="alert" className="text-sm text-destructive">
              {error}
            </p>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending}>
              {pending && <Loader2 className="animate-spin" />}
              {role ? 'Save changes' : 'Create role'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
```

## `src/components/admin/user-dialogs.tsx`

```tsx
import { useMemo } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { FormDialog, type FieldDef, type FormValues } from '#/components/common/form-dialog'
import { api } from '#/lib/api'
import { rolesQuery } from '#/lib/queries'
import type { User } from '#/lib/types'

const NEW_USER: FormValues = {}

export function UserDialog({ open, onOpenChange, user }: { open: boolean; onOpenChange: (o: boolean) => void; user?: User }) {
  const queryClient = useQueryClient()
  const { data: roles } = useQuery(rolesQuery)
  const editing = !!user
  const roleOptions = (roles ?? []).map((r) => ({ value: r.id, label: r.name.replaceAll('_', ' ').toLowerCase() }))

  const fields: FieldDef[] = [
    { name: 'firstName', label: 'First name', required: true },
    { name: 'lastName', label: 'Last name', required: true },
    ...(editing
      ? []
      : [
          { name: 'email', label: 'Email', type: 'email' as const, required: true },
          { name: 'password', label: 'Temporary password', type: 'password' as const, required: true, hint: 'At least 8 characters.' },
        ]),
    { name: 'roleId', label: 'Role', type: 'select', required: true, options: roleOptions },
    ...(editing
      ? [{ name: 'isActive', label: 'Status', type: 'select' as const, options: [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }] }]
      : []),
  ]

  const initial = useMemo<FormValues>(
    () =>
      user
        ? {
            firstName: user.firstName,
            lastName: user.lastName,
            roleId: roles?.find((r) => r.name === user.roleName)?.id ?? '',
            isActive: String(user.isActive),
          }
        : NEW_USER,
    [user, roles],
  )

  async function onSubmit(v: FormValues) {
    if (!v.roleId) throw new Error('Choose a role.')
    if (user) {
      await api.put(`/users/${user.id}`, { firstName: v.firstName, lastName: v.lastName, roleId: v.roleId, isActive: v.isActive !== 'false' })
      toast.success('User updated')
    } else {
      await api.post('/auth/register', { firstName: v.firstName, lastName: v.lastName, email: v.email, password: v.password, roleId: v.roleId })
      toast.success('User created')
    }
    queryClient.invalidateQueries({ queryKey: ['users'] })
    queryClient.invalidateQueries({ queryKey: ['roles'] })
  }

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={editing ? 'Edit user' : 'New user'}
      fields={fields}
      initial={initial}
      submitLabel={editing ? 'Save changes' : 'Create user'}
      onSubmit={onSubmit}
    />
  )
}

const RESET_FIELDS: FieldDef[] = [{ name: 'newPassword', label: 'New password', type: 'password', required: true, hint: 'At least 8 characters. Existing sessions are signed out.' }]

export function ResetPasswordDialog({ user, onOpenChange }: { user: User | null; onOpenChange: (o: boolean) => void }) {
  return (
    <FormDialog
      open={!!user}
      onOpenChange={onOpenChange}
      title="Reset password"
      description={user ? `Set a new password for ${user.firstName} ${user.lastName}.` : undefined}
      fields={RESET_FIELDS}
      submitLabel="Reset password"
      onSubmit={async (v) => {
        if (v.newPassword.length < 8) throw new Error('Password must be at least 8 characters long.')
        await api.patch(`/users/${user!.id}/password`, { newPassword: v.newPassword })
        toast.success('Password reset')
      }}
    />
  )
}
```

## `src/components/catalog/product-dialogs.tsx`

```tsx
import { useMemo } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { FormDialog, optional, type FieldDef, type FormValues } from '#/components/common/form-dialog'
import { api } from '#/lib/api'
import { categoriesQuery } from '#/lib/queries'
import type { Category, Product, Variant } from '#/lib/types'

const flatten = (cats: Category[]): Category[] => cats.flatMap((c) => [c, ...flatten(c.subcategories ?? [])])

const NEW_PRODUCT: FormValues = { unitOfMeasure: 'each' }
const NEW_VARIANT: FormValues = {}

export function ProductDialog({ open, onOpenChange, product }: { open: boolean; onOpenChange: (o: boolean) => void; product?: Product }) {
  const queryClient = useQueryClient()
  const { data: categories } = useQuery(categoriesQuery)
  const editing = !!product

  const fields: FieldDef[] = useMemo(
    () => [
      { name: 'name', label: 'Name', required: true },
      {
        name: 'categoryId',
        label: 'Category',
        type: 'select',
        required: true,
        options: flatten(categories ?? []).map((c) => ({ value: c.id, label: c.name })),
      },
      { name: 'brand', label: 'Brand' },
      { name: 'description', label: 'Description' },
      { name: 'unitOfMeasure', label: 'Unit of measure', required: true, placeholder: 'each, kg, box…' },
      ...(editing
        ? [{ name: 'isActive', label: 'Status', type: 'select' as const, options: [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }] }]
        : [
            { name: 'sku', label: 'First variant SKU', required: true, hint: 'A default variant is created with the product.' },
            { name: 'costPrice', label: 'Cost price', type: 'number' as const, step: '0.01', min: '0.01', required: true },
            { name: 'retailPrice', label: 'Retail price', type: 'number' as const, step: '0.01', min: '0.01', required: true },
          ]),
    ],
    [categories, editing],
  )

  const initial: FormValues = useMemo(
    () =>
      product
        ? {
            name: product.name,
            categoryId: product.categoryId,
            brand: product.brand ?? '',
            description: product.description ?? '',
            unitOfMeasure: product.unitOfMeasure,
            isActive: String(product.isActive),
          }
        : NEW_PRODUCT,
    [product],
  )

  async function onSubmit(v: FormValues) {
    if (!v.categoryId) throw new Error('Choose a category.')
    const base = {
      categoryId: v.categoryId,
      name: v.name,
      description: optional(v.description),
      brand: optional(v.brand),
      unitOfMeasure: v.unitOfMeasure,
    }
    if (product) {
      await api.put(`/products/${product.id}`, { ...base, isActive: v.isActive !== 'false' })
      toast.success('Product updated')
    } else {
      await api.post('/products', {
        ...base,
        hasVariants: false,
        variants: [{ sku: v.sku, costPrice: Number(v.costPrice), retailPrice: Number(v.retailPrice) }],
      })
      toast.success('Product created')
    }
    queryClient.invalidateQueries({ queryKey: ['products'] })
  }

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={editing ? 'Edit product' : 'New product'}
      fields={fields}
      initial={initial}
      submitLabel={editing ? 'Save changes' : 'Create product'}
      onSubmit={onSubmit}
    />
  )
}

export function VariantDialog({
  open,
  onOpenChange,
  productId,
  variant,
}: {
  open: boolean
  onOpenChange: (o: boolean) => void
  productId: string
  variant?: Variant
}) {
  const queryClient = useQueryClient()
  const editing = !!variant

  const fields: FieldDef[] = [
    ...(editing ? [] : [{ name: 'sku', label: 'SKU', required: true }]),
    { name: 'costPrice', label: 'Cost price', type: 'number', step: '0.01', min: '0.01', required: true },
    { name: 'retailPrice', label: 'Retail price', type: 'number', step: '0.01', min: '0.01', required: true },
    ...(editing
      ? [{ name: 'isActive', label: 'Status', type: 'select' as const, options: [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }] }]
      : [{ name: 'barcode', label: 'Barcode', hint: 'Optional. Added as an EAN-13 primary barcode.' }]),
  ]

  const initial = useMemo<FormValues>(
    () => (variant ? { costPrice: String(variant.costPrice), retailPrice: String(variant.retailPrice), isActive: String(variant.isActive) } : NEW_VARIANT),
    [variant],
  )

  async function onSubmit(v: FormValues) {
    if (variant) {
      await api.put(`/products/variants/${variant.id}`, {
        variantAttributes: variant.variantAttributes ?? undefined,
        costPrice: Number(v.costPrice),
        retailPrice: Number(v.retailPrice),
        isActive: v.isActive !== 'false',
      })
      toast.success('Variant updated')
    } else {
      await api.post(`/products/${productId}/variants`, {
        sku: v.sku,
        costPrice: Number(v.costPrice),
        retailPrice: Number(v.retailPrice),
        barcodes: optional(v.barcode) ? [{ barcode: v.barcode.trim(), type: 'EAN_13', isPrimary: true }] : undefined,
      })
      toast.success('Variant added')
    }
    queryClient.invalidateQueries({ queryKey: ['products'] })
  }

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={editing ? `Edit ${variant.sku}` : 'Add variant'}
      fields={fields}
      initial={initial}
      submitLabel={editing ? 'Save changes' : 'Add variant'}
      onSubmit={onSubmit}
    />
  )
}
```

## `src/components/common/cart-lines.tsx`

```tsx
import type { ReactNode } from 'react'
import { Minus, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'

export interface CartLine {
  variantId: string
  sku: string
  productName: string
  quantity: number
}

/** Editable list of document lines with quantity steppers; `extra` renders per-line fields. */
export function CartLines<T extends CartLine>({
  lines,
  onChange,
  empty = 'Search for a product and add a variant.',
  extra,
}: {
  lines: T[]
  onChange: (lines: T[]) => void
  empty?: string
  extra?: (line: T, update: (patch: Partial<T>) => void) => ReactNode
}) {
  if (lines.length === 0) return <p className="text-sm text-muted-foreground">{empty}</p>
  const update = (variantId: string, patch: Partial<T>) => onChange(lines.map((l) => (l.variantId === variantId ? { ...l, ...patch } : l)))
  return (
    <ul className="divide-y">
      {lines.map((l) => (
        <li key={l.variantId} className="flex flex-wrap items-center gap-2 py-2.5">
          <div className="min-w-0 flex-1 basis-40">
            <p className="truncate text-sm font-medium">{l.productName}</p>
            <p className="font-mono text-xs text-muted-foreground">{l.sku}</p>
          </div>
          {extra?.(l, (patch) => update(l.variantId, patch))}
          <div className="flex items-center gap-1">
            <Button type="button" variant="outline" size="icon" className="size-8" aria-label="Decrease quantity" onClick={() => update(l.variantId, { quantity: Math.max(1, l.quantity - 1) } as Partial<T>)}>
              <Minus />
            </Button>
            <span className="w-7 text-center text-sm tabular-nums">{l.quantity}</span>
            <Button type="button" variant="outline" size="icon" className="size-8" aria-label="Increase quantity" onClick={() => update(l.variantId, { quantity: l.quantity + 1 } as Partial<T>)}>
              <Plus />
            </Button>
            <Button type="button" variant="ghost" size="icon" className="size-8" aria-label={`Remove ${l.sku}`} onClick={() => onChange(lines.filter((x) => x.variantId !== l.variantId))}>
              <Trash2 />
            </Button>
          </div>
        </li>
      ))}
    </ul>
  )
}

export function addLine<T extends CartLine>(lines: T[], line: T): T[] {
  return lines.some((l) => l.variantId === line.variantId)
    ? lines.map((l) => (l.variantId === line.variantId ? { ...l, quantity: l.quantity + 1 } : l))
    : [...lines, line]
}
```

## `src/components/common/empty-state.tsx`

```tsx
export function EmptyState({ title, description }: { title: string; description?: string }) {
  return (
    <div className="grid place-items-center rounded-lg border border-dashed px-6 py-12 text-center">
      <p className="font-medium">{title}</p>
      {description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}
    </div>
  )
}
```

## `src/components/common/form-dialog.tsx`

```tsx
import { useEffect, useState } from 'react'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

export interface FieldDef {
  name: string
  label: string
  type?: 'text' | 'email' | 'password' | 'number' | 'select'
  required?: boolean
  options?: { value: string; label: string }[]
  placeholder?: string
  hint?: string
  step?: string
  min?: string
}

export type FormValues = Record<string, string>

export function FormDialog({
  open,
  onOpenChange,
  title,
  description,
  fields,
  initial,
  submitLabel = 'Save',
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  fields: FieldDef[]
  initial?: FormValues
  submitLabel?: string
  onSubmit: (values: FormValues) => Promise<unknown>
}) {
  const [values, setValues] = useState<FormValues>({})
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  useEffect(() => {
    if (open) {
      setValues(initial ?? {})
      setError(null)
    }
  }, [open, initial])

  const set = (name: string, value: string) => setValues((v) => ({ ...v, [name]: value }))

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setPending(true)
    setError(null)
    try {
      await onSubmit(values)
      onOpenChange(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
    } finally {
      setPending(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            {description && <DialogDescription>{description}</DialogDescription>}
          </DialogHeader>
          {fields.map((f) => (
            <div key={f.name} className="space-y-2">
              <Label htmlFor={`f-${f.name}`}>
                {f.label}
                {f.required && <span className="text-destructive"> *</span>}
              </Label>
              {f.type === 'select' ? (
                <Select value={values[f.name] ?? ''} onValueChange={(v) => set(f.name, v)}>
                  <SelectTrigger id={`f-${f.name}`} className="w-full">
                    <SelectValue placeholder={f.placeholder ?? 'Select…'} />
                  </SelectTrigger>
                  <SelectContent>
                    {f.options?.map((o) => (
                      <SelectItem key={o.value} value={o.value}>
                        {o.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              ) : (
                <Input
                  id={`f-${f.name}`}
                  type={f.type ?? 'text'}
                  step={f.step}
                  min={f.min}
                  required={f.required}
                  placeholder={f.placeholder}
                  value={values[f.name] ?? ''}
                  onChange={(e) => set(f.name, e.target.value)}
                />
              )}
              {f.hint && <p className="text-xs text-muted-foreground">{f.hint}</p>}
            </div>
          ))}
          {error && (
            <p role="alert" className="text-sm text-destructive">
              {error}
            </p>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending}>
              {pending && <Loader2 className="animate-spin" />}
              {submitLabel}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/** Empty strings become undefined so optional backend fields are omitted. */
export const optional = (v: string | undefined) => (v && v.trim() !== '' ? v.trim() : undefined)
```

## `src/components/common/page-header.tsx`

```tsx
import type { ReactNode } from 'react'

export function PageHeader({ title, description, actions }: { title: string; description?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}
```

## `src/components/common/pagination.tsx`

```tsx
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { Page } from '#/lib/types'

export function Pagination({ page, onChange }: { page: Page<unknown>['page'] | undefined; onChange: (n: number) => void }) {
  if (!page || page.totalPages <= 1) return null
  const from = page.number * page.size + 1
  const to = Math.min((page.number + 1) * page.size, page.totalElements)
  return (
    <div className="flex items-center justify-between gap-3 pt-4 text-sm text-muted-foreground">
      <p>
        {from}–{to} of {page.totalElements}
      </p>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={page.number === 0} onClick={() => onChange(page.number - 1)}>
          <ChevronLeft /> Prev
        </Button>
        <Button variant="outline" size="sm" disabled={page.number + 1 >= page.totalPages} onClick={() => onChange(page.number + 1)}>
          Next <ChevronRight />
        </Button>
      </div>
    </div>
  )
}
```

## `src/components/common/stat-card.tsx`

```tsx
import type { LucideIcon } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'

export function StatCard({
  label,
  value,
  icon: Icon,
  hint,
  loading,
}: {
  label: string
  value: string
  icon: LucideIcon
  hint?: string
  loading?: boolean
}) {
  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm text-muted-foreground">{label}</p>
          {loading ? (
            <Skeleton className="mt-2 h-8 w-24" />
          ) : (
            <p className="mt-1 truncate text-2xl font-semibold tabular-nums">{value}</p>
          )}
          {hint && !loading && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
        </div>
        <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-accent text-accent-foreground">
          <Icon className="size-4" />
        </span>
      </CardContent>
    </Card>
  )
}
```

## `src/components/common/status-badge.tsx`

```tsx
import { Badge } from '@/components/ui/badge'

const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  CANCELLED: 'destructive',
  DELIVERED: 'default',
  RECEIVED: 'default',
  SHIPPED: 'default',
  PENDING: 'outline',
  DRAFT: 'outline',
}

export function StatusBadge({ status }: { status: string }) {
  return <Badge variant={variants[status] ?? 'secondary'}>{status.replaceAll('_', ' ').toLowerCase()}</Badge>
}
```

## `src/components/common/variant-picker.tsx`

```tsx
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Plus, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { productsQuery } from '#/lib/queries'
import { formatCurrency } from '#/lib/format'

export interface PickedVariant {
  variantId: string
  sku: string
  productName: string
  retailPrice: number
  costPrice: number
}

/** Search products and add one of their active variants to a document. */
export function VariantPicker({ onAdd, price = 'retail' }: { onAdd: (v: PickedVariant) => void; price?: 'retail' | 'cost' }) {
  const [keyword, setKeyword] = useState('')
  const { data, isFetching } = useQuery(productsQuery({ page: 0, keyword: keyword || undefined }))

  return (
    <div className="space-y-3">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Search products by name…" className="pl-9" aria-label="Search products" />
      </div>
      <ul className="divide-y rounded-md border">
        {data?.content
          .filter((p) => p.isActive)
          .flatMap((p) => p.variants.filter((v) => v.isActive).map((v) => ({ p, v })))
          .map(({ p, v }) => (
            <li key={v.id} className="flex items-center gap-3 p-3">
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium">{p.name}</p>
                <p className="font-mono text-xs text-muted-foreground">{v.sku}</p>
              </div>
              <span className="text-sm tabular-nums">{formatCurrency(price === 'cost' ? v.costPrice : v.retailPrice)}</span>
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => onAdd({ variantId: v.id, sku: v.sku, productName: p.name, retailPrice: v.retailPrice, costPrice: v.costPrice })}
              >
                <Plus /> Add
              </Button>
            </li>
          ))}
        {data && data.content.length === 0 && <li className="p-4 text-sm text-muted-foreground">No products match.</li>}
        {!data && isFetching && <li className="p-4 text-sm text-muted-foreground">Loading…</li>}
      </ul>
    </div>
  )
}
```

## `src/components/inventory/adjust-stock-dialog.tsx`

```tsx
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { api } from '#/lib/api'
import type { StockLevel } from '#/lib/types'

export function AdjustStockDialog({ stock, onClose }: { stock: StockLevel | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (body: { variantId: string; warehouseId: string; quantityChange: number; reason: string }) =>
      api.post('/inventory/adjustments', body),
    onSuccess: () => {
      toast.success('Stock adjusted')
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
    onError: (e: Error) => setError(e.message),
  })

  return (
    <Dialog open={!!stock} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        {stock && (
          <form
            onSubmit={(e) => {
              e.preventDefault()
              const form = new FormData(e.currentTarget)
              const quantityChange = Number(form.get('quantityChange'))
              if (!Number.isInteger(quantityChange) || quantityChange === 0) {
                setError('Enter a whole number other than zero.')
                return
              }
              setError(null)
              mutation.mutate({
                variantId: stock.variantId,
                warehouseId: stock.warehouseId,
                quantityChange,
                reason: String(form.get('reason')),
              })
            }}
            className="space-y-4"
          >
            <DialogHeader>
              <DialogTitle>Adjust stock</DialogTitle>
              <DialogDescription>
                {stock.productName} · <span className="font-mono">{stock.sku}</span> at {stock.warehouseCode}. Currently {stock.quantityOnHand} on hand.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-2">
              <Label htmlFor="quantityChange">Quantity change</Label>
              <Input id="quantityChange" name="quantityChange" type="number" step={1} placeholder="e.g. 5 or -2" required />
              <p className="text-xs text-muted-foreground">Positive adds stock, negative removes it.</p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="reason">Reason</Label>
              <Input id="reason" name="reason" maxLength={500} required />
            </div>
            {error && (
              <p role="alert" className="text-sm text-destructive">
                {error}
              </p>
            )}
            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending && <Loader2 className="animate-spin" />}
                Save adjustment
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  )
}
```

## `src/components/inventory/rule-dialog.tsx`

```tsx
import { useMemo } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { FormDialog, type FieldDef, type FormValues } from '#/components/common/form-dialog'
import { api } from '#/lib/api'
import { warehousesQuery } from '#/lib/queries'
import type { ReorderRule, Variant } from '#/lib/types'

const NEW_RULE: FormValues = {}

export function RuleDialog({ open, onOpenChange, rule }: { open: boolean; onOpenChange: (o: boolean) => void; rule?: ReorderRule }) {
  const queryClient = useQueryClient()
  const { data: warehouses } = useQuery(warehousesQuery)

  const fields: FieldDef[] = [
    ...(rule
      ? []
      : [
          { name: 'sku', label: 'Variant SKU', required: true, hint: 'Exact SKU of the product variant.' },
          {
            name: 'warehouseId',
            label: 'Warehouse',
            type: 'select' as const,
            required: true,
            options: (warehouses?.content ?? []).filter((w) => w.isActive).map((w) => ({ value: w.id, label: `${w.code} · ${w.name}` })),
          },
        ]),
    { name: 'reorderPoint', label: 'Reorder point', type: 'number', min: '0', step: '1', required: true, hint: 'An alert is raised when available stock falls to this level.' },
    { name: 'reorderQuantity', label: 'Reorder quantity', type: 'number', min: '1', step: '1', required: true },
    ...(rule ? [{ name: 'isActive', label: 'Status', type: 'select' as const, options: [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }] }] : []),
  ]

  const initial = useMemo<FormValues>(
    () => (rule ? { reorderPoint: String(rule.reorderPoint), reorderQuantity: String(rule.reorderQuantity), isActive: String(rule.isActive) } : NEW_RULE),
    [rule],
  )

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={rule ? `Edit rule for ${rule.sku}` : 'New reorder rule'}
      description={rule ? `Warehouse ${rule.warehouseCode}` : undefined}
      fields={fields}
      initial={initial}
      submitLabel={rule ? 'Save changes' : 'Create rule'}
      onSubmit={async (v) => {
        const numbers = { reorderPoint: Number(v.reorderPoint), reorderQuantity: Number(v.reorderQuantity) }
        if (rule) {
          await api.put(`/reorder-rules/${rule.id}`, { variantId: rule.variantId, warehouseId: rule.warehouseId, ...numbers, isActive: v.isActive !== 'false' })
          toast.success('Rule updated')
        } else {
          if (!v.warehouseId) throw new Error('Choose a warehouse.')
          const variant = await api.get<Variant>(`/products/variants/sku/${encodeURIComponent(v.sku.trim())}`).catch(() => {
            throw new Error(`No variant with SKU "${v.sku.trim()}".`)
          })
          await api.post('/reorder-rules', { variantId: variant.id, warehouseId: v.warehouseId, ...numbers })
          toast.success('Rule created')
        }
        queryClient.invalidateQueries({ queryKey: ['reorder-rules'] })
        queryClient.invalidateQueries({ queryKey: ['alerts'] })
      }}
    />
  )
}
```

## `src/components/inventory/warehouse-dialog.tsx`

```tsx
import { useMemo } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { FormDialog, optional, type FieldDef, type FormValues } from '#/components/common/form-dialog'
import { api } from '#/lib/api'
import type { Warehouse } from '#/lib/types'

const NEW_WAREHOUSE: FormValues = {}

export function WarehouseDialog({ open, onOpenChange, warehouse }: { open: boolean; onOpenChange: (o: boolean) => void; warehouse?: Warehouse }) {
  const queryClient = useQueryClient()
  const fields: FieldDef[] = [
    ...(warehouse ? [] : [{ name: 'code', label: 'Code', required: true, hint: 'Short unique identifier, e.g. MAIN.' }]),
    { name: 'name', label: 'Name', required: true },
    { name: 'city', label: 'City' },
    { name: 'country', label: 'Country' },
    { name: 'contactPhone', label: 'Contact phone' },
    ...(warehouse
      ? [{ name: 'isActive', label: 'Status', type: 'select' as const, options: [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }], hint: 'Deactivating is refused while stock remains.' }]
      : []),
  ]

  const initial = useMemo<FormValues>(
    () =>
      warehouse
        ? { name: warehouse.name, city: warehouse.city ?? '', country: warehouse.country ?? '', contactPhone: warehouse.contactPhone ?? '', isActive: String(warehouse.isActive) }
        : NEW_WAREHOUSE,
    [warehouse],
  )

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={warehouse ? 'Edit warehouse' : 'New warehouse'}
      fields={fields}
      initial={initial}
      submitLabel={warehouse ? 'Save changes' : 'Create warehouse'}
      onSubmit={async (v) => {
        const body = { name: v.name, city: optional(v.city), country: optional(v.country), contactPhone: optional(v.contactPhone) }
        if (warehouse) {
          await api.put(`/warehouses/${warehouse.id}`, { ...body, isActive: v.isActive !== 'false' })
          toast.success('Warehouse updated')
        } else {
          await api.post('/warehouses', { ...body, code: v.code })
          toast.success('Warehouse created')
        }
        queryClient.invalidateQueries({ queryKey: ['warehouses'] })
      }}
    />
  )
}
```

## `src/components/layout/app-shell.tsx`

```tsx
import { Outlet } from '@tanstack/react-router'
import { Separator } from '@/components/ui/separator'
import { SidebarInset, SidebarProvider, SidebarTrigger } from '@/components/ui/sidebar'
import { AppSidebar } from './app-sidebar'
import { ThemeToggle } from './theme-toggle'
import { UserMenu } from './user-menu'

export function AppShell() {
  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset className="min-w-0">
        <header className="sticky top-0 z-10 flex h-14 items-center gap-2 border-b bg-background/80 px-4 backdrop-blur">
          <SidebarTrigger aria-label="Toggle navigation" />
          <Separator orientation="vertical" className="mx-1 h-5" />
          <div className="ml-auto flex items-center gap-1">
            <ThemeToggle />
            <UserMenu />
          </div>
        </header>
        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
```

## `src/components/layout/app-sidebar.tsx`

```tsx
import { Link } from '@tanstack/react-router'
import { Boxes } from 'lucide-react'
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
  useSidebar,
} from '@/components/ui/sidebar'
import { useAuth } from '#/auth'
import { navGroups } from './nav'

export function AppSidebar() {
  const { user } = useAuth()
  const { setOpenMobile } = useSidebar()
  const granted = new Set(user?.permissions ?? [])

  const groups = navGroups
    .map((g) => ({
      ...g,
      items: g.items.filter((i) => !i.permissions || i.permissions.some((p) => granted.has(p))),
    }))
    .filter((g) => g.items.length > 0)

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" asChild>
              <Link to="/" onClick={() => setOpenMobile(false)}>
                <span className="grid size-8 place-items-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                  <Boxes className="size-4" />
                </span>
                <span className="grid text-left leading-tight">
                  <span className="font-semibold">Apex</span>
                  <span className="text-xs text-sidebar-foreground/60">Inventory</span>
                </span>
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        {groups.map((group) => (
          <SidebarGroup key={group.label}>
            <SidebarGroupLabel>{group.label}</SidebarGroupLabel>
            <SidebarMenu>
              {group.items.map((item) => (
                <SidebarMenuItem key={item.label}>
                  <SidebarMenuButton asChild tooltip={item.label}>
                    <Link
                      to={item.to}
                      activeOptions={{ exact: item.to === '/' }}
                      activeProps={{ 'data-active': true }}
                      onClick={() => setOpenMobile(false)}
                    >
                      <item.icon />
                      <span>{item.label}</span>
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroup>
        ))}
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
  )
}
```

## `src/components/layout/nav.ts`

```ts
import { LayoutDashboard, Package, Warehouse, Bell, ShoppingCart, Truck, Factory, Users, ScrollText, Contact, Building2, ArrowLeftRight, type LucideIcon } from 'lucide-react'
import type { LinkProps } from '@tanstack/react-router'

export interface NavItem {
  label: string
  to: LinkProps['to']
  icon: LucideIcon
  /** Any one of these permissions makes the item visible. */
  permissions?: string[]
}

export interface NavGroup {
  label: string
  items: NavItem[]
}

export const navGroups: NavGroup[] = [
  {
    label: 'Overview',
    items: [{ label: 'Dashboard', to: '/', icon: LayoutDashboard }],
  },
  {
    label: 'Catalogue',
    items: [{ label: 'Products', to: '/products', icon: Package, permissions: ['product:read'] }],
  },
  {
    label: 'Procurement',
    items: [
      { label: 'Purchase orders', to: '/purchase-orders', icon: Truck, permissions: ['purchase:read'] },
      { label: 'Suppliers', to: '/suppliers', icon: Factory, permissions: ['supplier:read'] },
    ],
  },
  {
    label: 'Operations',
    items: [
      { label: 'Orders', to: '/orders', icon: ShoppingCart, permissions: ['order:read'] },
      { label: 'Customers', to: '/customers', icon: Contact, permissions: ['customer:read'] },
      { label: 'Inventory', to: '/inventory', icon: Warehouse, permissions: ['inventory:read'] },
      { label: 'Transfers', to: '/transfers', icon: ArrowLeftRight, permissions: ['transfer:read'] },
      { label: 'Warehouses', to: '/warehouses', icon: Building2, permissions: ['warehouse:read'] },
      { label: 'Alerts', to: '/alerts', icon: Bell, permissions: ['alert:read'] },
    ],
  },
  {
    label: 'Administration',
    items: [
      { label: 'Users & roles', to: '/users', icon: Users, permissions: ['user:read'] },
      { label: 'Audit log', to: '/audit', icon: ScrollText, permissions: ['audit:read'] },
    ],
  },
]
```

## `src/components/layout/theme-toggle.tsx`

```tsx
import { Monitor, Moon, Sun } from 'lucide-react'
import { useTheme } from '#/lib/theme'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

export function ThemeToggle() {
  const { setTheme } = useTheme()
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" aria-label="Change theme">
          <Sun className="size-4 dark:hidden" />
          <Moon className="hidden size-4 dark:block" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={() => setTheme('light')}>
          <Sun /> Light
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => setTheme('dark')}>
          <Moon /> Dark
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => setTheme('system')}>
          <Monitor /> System
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
```

## `src/components/layout/user-menu.tsx`

```tsx
import { useRouter } from '@tanstack/react-router'
import { LogOut } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuth } from '#/auth'

export function UserMenu() {
  const { user, logout } = useAuth()
  const router = useRouter()
  if (!user) return null
  const initials = `${user.firstName[0] ?? ''}${user.lastName[0] ?? ''}`.toUpperCase()

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="h-9 gap-2 px-1.5" aria-label="Account menu">
          <Avatar className="size-7">
            <AvatarFallback className="text-xs">{initials}</AvatarFallback>
          </Avatar>
          <span className="hidden text-sm font-medium sm:inline">{user.firstName}</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel className="font-normal">
          <p className="text-sm font-medium">
            {user.firstName} {user.lastName}
          </p>
          <p className="truncate text-xs text-muted-foreground">{user.email}</p>
          <p className="mt-1 text-xs text-muted-foreground">{user.roleName.replaceAll('_', ' ')}</p>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          onClick={async () => {
            await logout()
            await router.invalidate()
            await router.navigate({ to: '/login', search: {} })
          }}
        >
          <LogOut /> Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
```

## `src/components/procurement/supplier-dialog.tsx`

```tsx
import { useMemo } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { FormDialog, optional, type FieldDef, type FormValues } from '#/components/common/form-dialog'
import { api } from '#/lib/api'
import type { Supplier } from '#/lib/types'

const NEW_SUPPLIER: FormValues = {}

export function SupplierDialog({ open, onOpenChange, supplier }: { open: boolean; onOpenChange: (o: boolean) => void; supplier?: Supplier }) {
  const queryClient = useQueryClient()
  const editing = !!supplier

  const fields: FieldDef[] = [
    ...(editing ? [] : [{ name: 'code', label: 'Code', required: true, hint: 'Short unique identifier, e.g. ACME.' }]),
    { name: 'name', label: 'Name', required: true },
    { name: 'contactName', label: 'Contact name' },
    { name: 'email', label: 'Email', type: 'email' },
    { name: 'phone', label: 'Phone' },
    { name: 'city', label: 'City' },
    { name: 'country', label: 'Country' },
    { name: 'paymentTerms', label: 'Payment terms', placeholder: 'e.g. Net 30' },
    { name: 'leadTimeDays', label: 'Lead time (days)', type: 'number', min: '0', step: '1' },
    ...(editing
      ? [{ name: 'isActive', label: 'Status', type: 'select' as const, options: [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }] }]
      : []),
  ]

  const initial = useMemo<FormValues>(
    () =>
      supplier
        ? {
            name: supplier.name,
            contactName: supplier.contactName ?? '',
            email: supplier.email ?? '',
            phone: supplier.phone ?? '',
            city: supplier.city ?? '',
            country: supplier.country ?? '',
            paymentTerms: supplier.paymentTerms ?? '',
            leadTimeDays: supplier.leadTimeDays != null ? String(supplier.leadTimeDays) : '',
            isActive: String(supplier.isActive),
          }
        : NEW_SUPPLIER,
    [supplier],
  )

  async function onSubmit(v: FormValues) {
    const body = {
      name: v.name,
      contactName: optional(v.contactName),
      email: optional(v.email),
      phone: optional(v.phone),
      city: optional(v.city),
      country: optional(v.country),
      paymentTerms: optional(v.paymentTerms),
      leadTimeDays: optional(v.leadTimeDays) ? Number(v.leadTimeDays) : undefined,
    }
    if (supplier) {
      await api.put(`/suppliers/${supplier.id}`, { ...body, isActive: v.isActive !== 'false' })
      toast.success('Supplier updated')
    } else {
      await api.post('/suppliers', { ...body, code: v.code })
      toast.success('Supplier created')
    }
    queryClient.invalidateQueries({ queryKey: ['suppliers'] })
  }

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={editing ? 'Edit supplier' : 'New supplier'}
      fields={fields}
      initial={initial}
      submitLabel={editing ? 'Save changes' : 'Create supplier'}
      onSubmit={onSubmit}
    />
  )
}
```

## `src/components/sales/customer-dialog.tsx`

```tsx
import { useMemo } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { FormDialog, optional, type FieldDef, type FormValues } from '#/components/common/form-dialog'
import { api } from '#/lib/api'
import type { Customer } from '#/lib/types'

const NEW_CUSTOMER: FormValues = {}

const FIELDS: FieldDef[] = [
  { name: 'name', label: 'Name', required: true },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'phone', label: 'Phone' },
  { name: 'city', label: 'City' },
  { name: 'country', label: 'Country' },
  { name: 'notes', label: 'Notes' },
]

export function CustomerDialog({ open, onOpenChange, customer }: { open: boolean; onOpenChange: (o: boolean) => void; customer?: Customer }) {
  const queryClient = useQueryClient()
  const fields: FieldDef[] = customer
    ? [...FIELDS, { name: 'isActive', label: 'Status', type: 'select', options: [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }] }]
    : FIELDS

  const initial = useMemo<FormValues>(
    () =>
      customer
        ? {
            name: customer.name,
            email: customer.email ?? '',
            phone: customer.phone ?? '',
            city: customer.city ?? '',
            country: customer.country ?? '',
            notes: customer.notes ?? '',
            isActive: String(customer.isActive),
          }
        : NEW_CUSTOMER,
    [customer],
  )

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={customer ? 'Edit customer' : 'New customer'}
      fields={fields}
      initial={initial}
      submitLabel={customer ? 'Save changes' : 'Create customer'}
      onSubmit={async (v) => {
        const body = { name: v.name, email: optional(v.email), phone: optional(v.phone), city: optional(v.city), country: optional(v.country), notes: optional(v.notes) }
        if (customer) {
          await api.put(`/customers/${customer.id}`, { ...body, isActive: v.isActive !== 'false' })
          toast.success('Customer updated')
        } else {
          await api.post('/customers', body)
          toast.success('Customer created')
        }
        queryClient.invalidateQueries({ queryKey: ['customers'] })
      }}
    />
  )
}
```

## `src/components/sales/return-dialog.tsx`

```tsx
import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { api } from '#/lib/api'
import type { Order } from '#/lib/types'

export function ReturnDialog({ order, open, onOpenChange }: { order: Order; open: boolean; onOpenChange: (o: boolean) => void }) {
  const queryClient = useQueryClient()
  const [quantities, setQuantities] = useState<Record<string, string>>({})
  const [reason, setReason] = useState('')
  const [restock, setRestock] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  useEffect(() => {
    if (open) {
      setQuantities({})
      setReason('')
      setRestock(true)
      setError(null)
    }
  }, [open])

  const returnable = (l: Order['lines'][number]) => Math.max(0, l.quantityPicked - l.quantityReturned)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    const lines = order.lines
      .map((l) => ({ orderLineId: l.id, quantity: Number(quantities[l.id] ?? 0), max: returnable(l) }))
      .filter((l) => l.quantity > 0)
    if (!lines.length) return setError('Enter a quantity for at least one line.')
    if (lines.some((l) => !Number.isInteger(l.quantity) || l.quantity > l.max)) return setError('Quantities must be whole numbers within what can be returned.')
    setPending(true)
    setError(null)
    try {
      await api.post(`/orders/${order.id}/returns`, {
        reason: reason.trim() || undefined,
        restock,
        lines: lines.map(({ orderLineId, quantity }) => ({ orderLineId, quantity })),
      })
      toast.success('Return recorded')
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      onOpenChange(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
    } finally {
      setPending(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto">
        <form onSubmit={onSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Return items</DialogTitle>
            <DialogDescription>Order {order.orderNumber}. Enter how many of each item came back.</DialogDescription>
          </DialogHeader>
          <ul className="divide-y">
            {order.lines.map((l) => (
              <li key={l.id} className="flex items-center gap-3 py-2">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{l.productName}</p>
                  <p className="text-xs text-muted-foreground">
                    <span className="font-mono">{l.sku}</span> · up to {returnable(l)}
                  </p>
                </div>
                <Input
                  type="number"
                  min={0}
                  max={returnable(l)}
                  disabled={returnable(l) === 0}
                  value={quantities[l.id] ?? ''}
                  onChange={(e) => setQuantities((q) => ({ ...q, [l.id]: e.target.value }))}
                  aria-label={`Quantity returned for ${l.sku}`}
                  className="w-20 text-right"
                />
              </li>
            ))}
          </ul>
          <div className="space-y-2">
            <Label htmlFor="return-reason">Reason</Label>
            <Input id="return-reason" value={reason} onChange={(e) => setReason(e.target.value)} maxLength={500} />
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="size-4 accent-primary" checked={restock} onChange={(e) => setRestock(e.target.checked)} />
            Return items to stock (uncheck to write off)
          </label>
          {error && (
            <p role="alert" className="text-sm text-destructive">
              {error}
            </p>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending}>
              {pending && <Loader2 className="animate-spin" />}
              Record return
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
```

## `src/components/ui/avatar.tsx`

```tsx
import * as React from "react"
import { cn } from "@/lib/utils"
import { Avatar as AvatarPrimitive } from "radix-ui"

function Avatar({
  className,
  size = "default",
  ...props
}: React.ComponentProps<typeof AvatarPrimitive.Root> & {
  size?: "default" | "sm" | "lg"
}) {
  return (
    <AvatarPrimitive.Root
      data-slot="avatar"
      data-size={size}
      className={cn(
        "group/avatar relative flex size-8 shrink-0 overflow-hidden rounded-full select-none data-[size=lg]:size-10 data-[size=sm]:size-6",
        className
      )}
      {...props}
    />
  )
}

function AvatarImage({
  className,
  ...props
}: React.ComponentProps<typeof AvatarPrimitive.Image>) {
  return (
    <AvatarPrimitive.Image
      data-slot="avatar-image"
      className={cn("aspect-square size-full", className)}
      {...props}
    />
  )
}

function AvatarFallback({
  className,
  ...props
}: React.ComponentProps<typeof AvatarPrimitive.Fallback>) {
  return (
    <AvatarPrimitive.Fallback
      data-slot="avatar-fallback"
      className={cn(
        "flex size-full items-center justify-center rounded-full bg-muted text-sm text-muted-foreground group-data-[size=sm]/avatar:text-xs",
        className
      )}
      {...props}
    />
  )
}

function AvatarBadge({ className, ...props }: React.ComponentProps<"span">) {
  return (
    <span
      data-slot="avatar-badge"
      className={cn(
        "absolute right-0 bottom-0 z-10 inline-flex items-center justify-center rounded-full bg-primary text-primary-foreground ring-2 ring-background select-none",
        "group-data-[size=sm]/avatar:size-2 group-data-[size=sm]/avatar:[&>svg]:hidden",
        "group-data-[size=default]/avatar:size-2.5 group-data-[size=default]/avatar:[&>svg]:size-2",
        "group-data-[size=lg]/avatar:size-3 group-data-[size=lg]/avatar:[&>svg]:size-2",
        className
      )}
      {...props}
    />
  )
}

function AvatarGroup({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="avatar-group"
      className={cn(
        "group/avatar-group flex -space-x-2 *:data-[slot=avatar]:ring-2 *:data-[slot=avatar]:ring-background",
        className
      )}
      {...props}
    />
  )
}

function AvatarGroupCount({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="avatar-group-count"
      className={cn(
        "relative flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-sm text-muted-foreground ring-2 ring-background group-has-data-[size=lg]/avatar-group:size-10 group-has-data-[size=sm]/avatar-group:size-6 [&>svg]:size-4 group-has-data-[size=lg]/avatar-group:[&>svg]:size-5 group-has-data-[size=sm]/avatar-group:[&>svg]:size-3",
        className
      )}
      {...props}
    />
  )
}

export {
  Avatar,
  AvatarImage,
  AvatarFallback,
  AvatarBadge,
  AvatarGroup,
  AvatarGroupCount,
}
```

## `src/components/ui/badge.tsx`

```tsx
import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import { Slot } from "radix-ui"

const badgeVariants = cva(
  "inline-flex w-fit shrink-0 items-center justify-center gap-1 overflow-hidden rounded-full border border-transparent px-2 py-0.5 text-xs font-medium whitespace-nowrap transition-[color,box-shadow] focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 [&>svg]:pointer-events-none [&>svg]:size-3",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground [a&]:hover:bg-primary/90",
        secondary:
          "bg-secondary text-secondary-foreground [a&]:hover:bg-secondary/90",
        destructive:
          "bg-destructive text-white focus-visible:ring-destructive/20 dark:bg-destructive/60 dark:focus-visible:ring-destructive/40 [a&]:hover:bg-destructive/90",
        outline:
          "border-border text-foreground [a&]:hover:bg-accent [a&]:hover:text-accent-foreground",
        ghost: "[a&]:hover:bg-accent [a&]:hover:text-accent-foreground",
        link: "text-primary underline-offset-4 [a&]:hover:underline",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

function Badge({
  className,
  variant = "default",
  asChild = false,
  ...props
}: React.ComponentProps<"span"> &
  VariantProps<typeof badgeVariants> & { asChild?: boolean }) {
  const Comp = asChild ? Slot.Root : "span"

  return (
    <Comp
      data-slot="badge"
      data-variant={variant}
      className={cn(badgeVariants({ variant }), className)}
      {...props}
    />
  )
}

export { Badge, badgeVariants }
```

## `src/components/ui/button.tsx`

```tsx
import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import { Slot } from "radix-ui"

const buttonVariants = cva(
  "inline-flex shrink-0 items-center justify-center gap-2 rounded-md text-sm font-medium whitespace-nowrap transition-all outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-primary/90",
        destructive:
          "bg-destructive text-white hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:bg-destructive/60 dark:focus-visible:ring-destructive/40",
        outline:
          "border bg-background shadow-xs hover:bg-accent hover:text-accent-foreground dark:border-input dark:bg-input/30 dark:hover:bg-input/50",
        secondary:
          "bg-secondary text-secondary-foreground hover:bg-secondary/80",
        ghost:
          "hover:bg-accent hover:text-accent-foreground dark:hover:bg-accent/50",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        default: "h-9 px-4 py-2 has-[>svg]:px-3",
        xs: "h-6 gap-1 rounded-md px-2 text-xs has-[>svg]:px-1.5 [&_svg:not([class*='size-'])]:size-3",
        sm: "h-8 gap-1.5 rounded-md px-3 has-[>svg]:px-2.5",
        lg: "h-10 rounded-md px-6 has-[>svg]:px-4",
        icon: "size-9",
        "icon-xs": "size-6 rounded-md [&_svg:not([class*='size-'])]:size-3",
        "icon-sm": "size-8",
        "icon-lg": "size-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant = "default",
  size = "default",
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
```

## `src/components/ui/card.tsx`

```tsx
import * as React from "react"
import { cn } from "@/lib/utils"

function Card({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card"
      className={cn(
        "flex flex-col gap-6 rounded-xl border bg-card py-6 text-card-foreground shadow-sm",
        className
      )}
      {...props}
    />
  )
}

function CardHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-header"
      className={cn(
        "@container/card-header grid auto-rows-min grid-rows-[auto_auto] items-start gap-2 px-6 has-data-[slot=card-action]:grid-cols-[1fr_auto] [.border-b]:pb-6",
        className
      )}
      {...props}
    />
  )
}

function CardTitle({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-title"
      className={cn("leading-none font-semibold", className)}
      {...props}
    />
  )
}

function CardDescription({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-description"
      className={cn("text-sm text-muted-foreground", className)}
      {...props}
    />
  )
}

function CardAction({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-action"
      className={cn(
        "col-start-2 row-span-2 row-start-1 self-start justify-self-end",
        className
      )}
      {...props}
    />
  )
}

function CardContent({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-content"
      className={cn("px-6", className)}
      {...props}
    />
  )
}

function CardFooter({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-footer"
      className={cn("flex items-center px-6 [.border-t]:pt-6", className)}
      {...props}
    />
  )
}

export {
  Card,
  CardHeader,
  CardFooter,
  CardTitle,
  CardAction,
  CardDescription,
  CardContent,
}
```

## `src/components/ui/dialog.tsx`

```tsx
"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { XIcon } from "lucide-react"
import { Dialog as DialogPrimitive } from "radix-ui"

import { Button } from "@/components/ui/button"

function Dialog({
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Root>) {
  return <DialogPrimitive.Root data-slot="dialog" {...props} />
}

function DialogTrigger({
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Trigger>) {
  return <DialogPrimitive.Trigger data-slot="dialog-trigger" {...props} />
}

function DialogPortal({
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Portal>) {
  return <DialogPrimitive.Portal data-slot="dialog-portal" {...props} />
}

function DialogClose({
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Close>) {
  return <DialogPrimitive.Close data-slot="dialog-close" {...props} />
}

function DialogOverlay({
  className,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Overlay>) {
  return (
    <DialogPrimitive.Overlay
      data-slot="dialog-overlay"
      className={cn(
        "fixed inset-0 z-50 bg-black/50 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0",
        className
      )}
      {...props}
    />
  )
}

function DialogContent({
  className,
  children,
  showCloseButton = true,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Content> & {
  showCloseButton?: boolean
}) {
  return (
    <DialogPortal data-slot="dialog-portal">
      <DialogOverlay />
      <DialogPrimitive.Content
        data-slot="dialog-content"
        className={cn(
          "fixed top-[50%] left-[50%] z-50 grid w-full max-w-[calc(100%-2rem)] translate-x-[-50%] translate-y-[-50%] gap-4 rounded-lg border bg-background p-6 shadow-lg duration-200 outline-none data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95 sm:max-w-lg",
          className
        )}
        {...props}
      >
        {children}
        {showCloseButton && (
          <DialogPrimitive.Close
            data-slot="dialog-close"
            className="absolute top-4 right-4 rounded-xs opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:ring-2 focus:ring-ring focus:ring-offset-2 focus:outline-hidden disabled:pointer-events-none data-[state=open]:bg-accent data-[state=open]:text-muted-foreground [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4"
          >
            <XIcon />
            <span className="sr-only">Close</span>
          </DialogPrimitive.Close>
        )}
      </DialogPrimitive.Content>
    </DialogPortal>
  )
}

function DialogHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="dialog-header"
      className={cn("flex flex-col gap-2 text-center sm:text-left", className)}
      {...props}
    />
  )
}

function DialogFooter({
  className,
  showCloseButton = false,
  children,
  ...props
}: React.ComponentProps<"div"> & {
  showCloseButton?: boolean
}) {
  return (
    <div
      data-slot="dialog-footer"
      className={cn(
        "flex flex-col-reverse gap-2 sm:flex-row sm:justify-end",
        className
      )}
      {...props}
    >
      {children}
      {showCloseButton && (
        <DialogPrimitive.Close asChild>
          <Button variant="outline">Close</Button>
        </DialogPrimitive.Close>
      )}
    </div>
  )
}

function DialogTitle({
  className,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Title>) {
  return (
    <DialogPrimitive.Title
      data-slot="dialog-title"
      className={cn("text-lg leading-none font-semibold", className)}
      {...props}
    />
  )
}

function DialogDescription({
  className,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Description>) {
  return (
    <DialogPrimitive.Description
      data-slot="dialog-description"
      className={cn("text-sm text-muted-foreground", className)}
      {...props}
    />
  )
}

export {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
  DialogTrigger,
}
```

## `src/components/ui/dropdown-menu.tsx`

```tsx
"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { CheckIcon, ChevronRightIcon, CircleIcon } from "lucide-react"
import { DropdownMenu as DropdownMenuPrimitive } from "radix-ui"

function DropdownMenu({
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Root>) {
  return <DropdownMenuPrimitive.Root data-slot="dropdown-menu" {...props} />
}

function DropdownMenuPortal({
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Portal>) {
  return (
    <DropdownMenuPrimitive.Portal data-slot="dropdown-menu-portal" {...props} />
  )
}

function DropdownMenuTrigger({
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Trigger>) {
  return (
    <DropdownMenuPrimitive.Trigger
      data-slot="dropdown-menu-trigger"
      {...props}
    />
  )
}

function DropdownMenuContent({
  className,
  sideOffset = 4,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Content>) {
  return (
    <DropdownMenuPrimitive.Portal>
      <DropdownMenuPrimitive.Content
        data-slot="dropdown-menu-content"
        sideOffset={sideOffset}
        className={cn(
          "z-50 max-h-(--radix-dropdown-menu-content-available-height) min-w-[8rem] origin-(--radix-dropdown-menu-content-transform-origin) overflow-x-hidden overflow-y-auto rounded-md border bg-popover p-1 text-popover-foreground shadow-md data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95",
          className
        )}
        {...props}
      />
    </DropdownMenuPrimitive.Portal>
  )
}

function DropdownMenuGroup({
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Group>) {
  return (
    <DropdownMenuPrimitive.Group data-slot="dropdown-menu-group" {...props} />
  )
}

function DropdownMenuItem({
  className,
  inset,
  variant = "default",
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Item> & {
  inset?: boolean
  variant?: "default" | "destructive"
}) {
  return (
    <DropdownMenuPrimitive.Item
      data-slot="dropdown-menu-item"
      data-inset={inset}
      data-variant={variant}
      className={cn(
        "relative flex cursor-default items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-hidden select-none focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50 data-[inset]:pl-8 data-[variant=destructive]:text-destructive data-[variant=destructive]:focus:bg-destructive/10 data-[variant=destructive]:focus:text-destructive dark:data-[variant=destructive]:focus:bg-destructive/20 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4 [&_svg:not([class*='text-'])]:text-muted-foreground data-[variant=destructive]:*:[svg]:text-destructive!",
        className
      )}
      {...props}
    />
  )
}

function DropdownMenuCheckboxItem({
  className,
  children,
  checked,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.CheckboxItem>) {
  return (
    <DropdownMenuPrimitive.CheckboxItem
      data-slot="dropdown-menu-checkbox-item"
      className={cn(
        "relative flex cursor-default items-center gap-2 rounded-sm py-1.5 pr-2 pl-8 text-sm outline-hidden select-none focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        className
      )}
      checked={checked}
      {...props}
    >
      <span className="pointer-events-none absolute left-2 flex size-3.5 items-center justify-center">
        <DropdownMenuPrimitive.ItemIndicator>
          <CheckIcon className="size-4" />
        </DropdownMenuPrimitive.ItemIndicator>
      </span>
      {children}
    </DropdownMenuPrimitive.CheckboxItem>
  )
}

function DropdownMenuRadioGroup({
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.RadioGroup>) {
  return (
    <DropdownMenuPrimitive.RadioGroup
      data-slot="dropdown-menu-radio-group"
      {...props}
    />
  )
}

function DropdownMenuRadioItem({
  className,
  children,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.RadioItem>) {
  return (
    <DropdownMenuPrimitive.RadioItem
      data-slot="dropdown-menu-radio-item"
      className={cn(
        "relative flex cursor-default items-center gap-2 rounded-sm py-1.5 pr-2 pl-8 text-sm outline-hidden select-none focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        className
      )}
      {...props}
    >
      <span className="pointer-events-none absolute left-2 flex size-3.5 items-center justify-center">
        <DropdownMenuPrimitive.ItemIndicator>
          <CircleIcon className="size-2 fill-current" />
        </DropdownMenuPrimitive.ItemIndicator>
      </span>
      {children}
    </DropdownMenuPrimitive.RadioItem>
  )
}

function DropdownMenuLabel({
  className,
  inset,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Label> & {
  inset?: boolean
}) {
  return (
    <DropdownMenuPrimitive.Label
      data-slot="dropdown-menu-label"
      data-inset={inset}
      className={cn(
        "px-2 py-1.5 text-sm font-medium data-[inset]:pl-8",
        className
      )}
      {...props}
    />
  )
}

function DropdownMenuSeparator({
  className,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Separator>) {
  return (
    <DropdownMenuPrimitive.Separator
      data-slot="dropdown-menu-separator"
      className={cn("-mx-1 my-1 h-px bg-border", className)}
      {...props}
    />
  )
}

function DropdownMenuShortcut({
  className,
  ...props
}: React.ComponentProps<"span">) {
  return (
    <span
      data-slot="dropdown-menu-shortcut"
      className={cn(
        "ml-auto text-xs tracking-widest text-muted-foreground",
        className
      )}
      {...props}
    />
  )
}

function DropdownMenuSub({
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Sub>) {
  return <DropdownMenuPrimitive.Sub data-slot="dropdown-menu-sub" {...props} />
}

function DropdownMenuSubTrigger({
  className,
  inset,
  children,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.SubTrigger> & {
  inset?: boolean
}) {
  return (
    <DropdownMenuPrimitive.SubTrigger
      data-slot="dropdown-menu-sub-trigger"
      data-inset={inset}
      className={cn(
        "flex cursor-default items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-hidden select-none focus:bg-accent focus:text-accent-foreground data-[inset]:pl-8 data-[state=open]:bg-accent data-[state=open]:text-accent-foreground [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4 [&_svg:not([class*='text-'])]:text-muted-foreground",
        className
      )}
      {...props}
    >
      {children}
      <ChevronRightIcon className="ml-auto size-4" />
    </DropdownMenuPrimitive.SubTrigger>
  )
}

function DropdownMenuSubContent({
  className,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.SubContent>) {
  return (
    <DropdownMenuPrimitive.SubContent
      data-slot="dropdown-menu-sub-content"
      className={cn(
        "z-50 min-w-[8rem] origin-(--radix-dropdown-menu-content-transform-origin) overflow-hidden rounded-md border bg-popover p-1 text-popover-foreground shadow-lg data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95",
        className
      )}
      {...props}
    />
  )
}

export {
  DropdownMenu,
  DropdownMenuPortal,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuItem,
  DropdownMenuCheckboxItem,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuSub,
  DropdownMenuSubTrigger,
  DropdownMenuSubContent,
}
```

## `src/components/ui/input.tsx`

```tsx
import * as React from "react"
import { cn } from "@/lib/utils"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-xs transition-[color,box-shadow] outline-none selection:bg-primary selection:text-primary-foreground file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm dark:bg-input/30",
        "focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50",
        "aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40",
        className
      )}
      {...props}
    />
  )
}

export { Input }
```

## `src/components/ui/label.tsx`

```tsx
import * as React from "react"
import { cn } from "@/lib/utils"
import { Label as LabelPrimitive } from "radix-ui"

function Label({
  className,
  ...props
}: React.ComponentProps<typeof LabelPrimitive.Root>) {
  return (
    <LabelPrimitive.Root
      data-slot="label"
      className={cn(
        "flex items-center gap-2 text-sm leading-none font-medium select-none group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:opacity-50 peer-disabled:cursor-not-allowed peer-disabled:opacity-50",
        className
      )}
      {...props}
    />
  )
}

export { Label }
```

## `src/components/ui/scroll-area.tsx`

```tsx
"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { ScrollArea as ScrollAreaPrimitive } from "radix-ui"

function ScrollArea({
  className,
  children,
  ...props
}: React.ComponentProps<typeof ScrollAreaPrimitive.Root>) {
  return (
    <ScrollAreaPrimitive.Root
      data-slot="scroll-area"
      className={cn("relative", className)}
      {...props}
    >
      <ScrollAreaPrimitive.Viewport
        data-slot="scroll-area-viewport"
        className="size-full rounded-[inherit] transition-[color,box-shadow] outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-1"
      >
        {children}
      </ScrollAreaPrimitive.Viewport>
      <ScrollBar />
      <ScrollAreaPrimitive.Corner />
    </ScrollAreaPrimitive.Root>
  )
}

function ScrollBar({
  className,
  orientation = "vertical",
  ...props
}: React.ComponentProps<typeof ScrollAreaPrimitive.ScrollAreaScrollbar>) {
  return (
    <ScrollAreaPrimitive.ScrollAreaScrollbar
      data-slot="scroll-area-scrollbar"
      orientation={orientation}
      className={cn(
        "flex touch-none p-px transition-colors select-none",
        orientation === "vertical" &&
          "h-full w-2.5 border-l border-l-transparent",
        orientation === "horizontal" &&
          "h-2.5 flex-col border-t border-t-transparent",
        className
      )}
      {...props}
    >
      <ScrollAreaPrimitive.ScrollAreaThumb
        data-slot="scroll-area-thumb"
        className="relative flex-1 rounded-full bg-border"
      />
    </ScrollAreaPrimitive.ScrollAreaScrollbar>
  )
}

export { ScrollArea, ScrollBar }
```

## `src/components/ui/select.tsx`

```tsx
"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { CheckIcon, ChevronDownIcon, ChevronUpIcon } from "lucide-react"
import { Select as SelectPrimitive } from "radix-ui"

function Select({
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Root>) {
  return <SelectPrimitive.Root data-slot="select" {...props} />
}

function SelectGroup({
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Group>) {
  return <SelectPrimitive.Group data-slot="select-group" {...props} />
}

function SelectValue({
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Value>) {
  return <SelectPrimitive.Value data-slot="select-value" {...props} />
}

function SelectTrigger({
  className,
  size = "default",
  children,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Trigger> & {
  size?: "sm" | "default"
}) {
  return (
    <SelectPrimitive.Trigger
      data-slot="select-trigger"
      data-size={size}
      className={cn(
        "flex w-fit items-center justify-between gap-2 rounded-md border border-input bg-transparent px-3 py-2 text-sm whitespace-nowrap shadow-xs transition-[color,box-shadow] outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 data-[placeholder]:text-muted-foreground data-[size=default]:h-9 data-[size=sm]:h-8 *:data-[slot=select-value]:line-clamp-1 *:data-[slot=select-value]:flex *:data-[slot=select-value]:items-center *:data-[slot=select-value]:gap-2 dark:bg-input/30 dark:hover:bg-input/50 dark:aria-invalid:ring-destructive/40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4 [&_svg:not([class*='text-'])]:text-muted-foreground",
        className
      )}
      {...props}
    >
      {children}
      <SelectPrimitive.Icon asChild>
        <ChevronDownIcon className="size-4 opacity-50" />
      </SelectPrimitive.Icon>
    </SelectPrimitive.Trigger>
  )
}

function SelectContent({
  className,
  children,
  position = "item-aligned",
  align = "center",
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Content>) {
  return (
    <SelectPrimitive.Portal>
      <SelectPrimitive.Content
        data-slot="select-content"
        className={cn(
          "relative z-50 max-h-(--radix-select-content-available-height) min-w-[8rem] origin-(--radix-select-content-transform-origin) overflow-x-hidden overflow-y-auto rounded-md border bg-popover text-popover-foreground shadow-md data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95",
          position === "popper" &&
            "data-[side=bottom]:translate-y-1 data-[side=left]:-translate-x-1 data-[side=right]:translate-x-1 data-[side=top]:-translate-y-1",
          className
        )}
        position={position}
        align={align}
        {...props}
      >
        <SelectScrollUpButton />
        <SelectPrimitive.Viewport
          className={cn(
            "p-1",
            position === "popper" &&
              "h-[var(--radix-select-trigger-height)] w-full min-w-[var(--radix-select-trigger-width)] scroll-my-1"
          )}
        >
          {children}
        </SelectPrimitive.Viewport>
        <SelectScrollDownButton />
      </SelectPrimitive.Content>
    </SelectPrimitive.Portal>
  )
}

function SelectLabel({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Label>) {
  return (
    <SelectPrimitive.Label
      data-slot="select-label"
      className={cn("px-2 py-1.5 text-xs text-muted-foreground", className)}
      {...props}
    />
  )
}

function SelectItem({
  className,
  children,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Item>) {
  return (
    <SelectPrimitive.Item
      data-slot="select-item"
      className={cn(
        "relative flex w-full cursor-default items-center gap-2 rounded-sm py-1.5 pr-8 pl-2 text-sm outline-hidden select-none focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4 [&_svg:not([class*='text-'])]:text-muted-foreground *:[span]:last:flex *:[span]:last:items-center *:[span]:last:gap-2",
        className
      )}
      {...props}
    >
      <span
        data-slot="select-item-indicator"
        className="absolute right-2 flex size-3.5 items-center justify-center"
      >
        <SelectPrimitive.ItemIndicator>
          <CheckIcon className="size-4" />
        </SelectPrimitive.ItemIndicator>
      </span>
      <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
    </SelectPrimitive.Item>
  )
}

function SelectSeparator({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Separator>) {
  return (
    <SelectPrimitive.Separator
      data-slot="select-separator"
      className={cn("pointer-events-none -mx-1 my-1 h-px bg-border", className)}
      {...props}
    />
  )
}

function SelectScrollUpButton({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.ScrollUpButton>) {
  return (
    <SelectPrimitive.ScrollUpButton
      data-slot="select-scroll-up-button"
      className={cn(
        "flex cursor-default items-center justify-center py-1",
        className
      )}
      {...props}
    >
      <ChevronUpIcon className="size-4" />
    </SelectPrimitive.ScrollUpButton>
  )
}

function SelectScrollDownButton({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.ScrollDownButton>) {
  return (
    <SelectPrimitive.ScrollDownButton
      data-slot="select-scroll-down-button"
      className={cn(
        "flex cursor-default items-center justify-center py-1",
        className
      )}
      {...props}
    >
      <ChevronDownIcon className="size-4" />
    </SelectPrimitive.ScrollDownButton>
  )
}

export {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectScrollDownButton,
  SelectScrollUpButton,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
}
```

## `src/components/ui/separator.tsx`

```tsx
"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { Separator as SeparatorPrimitive } from "radix-ui"

function Separator({
  className,
  orientation = "horizontal",
  decorative = true,
  ...props
}: React.ComponentProps<typeof SeparatorPrimitive.Root>) {
  return (
    <SeparatorPrimitive.Root
      data-slot="separator"
      decorative={decorative}
      orientation={orientation}
      className={cn(
        "shrink-0 bg-border data-[orientation=horizontal]:h-px data-[orientation=horizontal]:w-full data-[orientation=vertical]:h-full data-[orientation=vertical]:w-px",
        className
      )}
      {...props}
    />
  )
}

export { Separator }
```

## `src/components/ui/sheet.tsx`

```tsx
import * as React from "react"
import { cn } from "@/lib/utils"
import { XIcon } from "lucide-react"
import { Dialog as SheetPrimitive } from "radix-ui"

function Sheet({ ...props }: React.ComponentProps<typeof SheetPrimitive.Root>) {
  return <SheetPrimitive.Root data-slot="sheet" {...props} />
}

function SheetTrigger({
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Trigger>) {
  return <SheetPrimitive.Trigger data-slot="sheet-trigger" {...props} />
}

function SheetClose({
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Close>) {
  return <SheetPrimitive.Close data-slot="sheet-close" {...props} />
}

function SheetPortal({
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Portal>) {
  return <SheetPrimitive.Portal data-slot="sheet-portal" {...props} />
}

function SheetOverlay({
  className,
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Overlay>) {
  return (
    <SheetPrimitive.Overlay
      data-slot="sheet-overlay"
      className={cn(
        "fixed inset-0 z-50 bg-black/50 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0",
        className
      )}
      {...props}
    />
  )
}

function SheetContent({
  className,
  children,
  side = "right",
  showCloseButton = true,
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Content> & {
  side?: "top" | "right" | "bottom" | "left"
  showCloseButton?: boolean
}) {
  return (
    <SheetPortal>
      <SheetOverlay />
      <SheetPrimitive.Content
        data-slot="sheet-content"
        className={cn(
          "fixed z-50 flex flex-col gap-4 bg-background shadow-lg transition ease-in-out data-[state=closed]:animate-out data-[state=closed]:duration-300 data-[state=open]:animate-in data-[state=open]:duration-500",
          side === "right" &&
            "inset-y-0 right-0 h-full w-3/4 border-l data-[state=closed]:slide-out-to-right data-[state=open]:slide-in-from-right sm:max-w-sm",
          side === "left" &&
            "inset-y-0 left-0 h-full w-3/4 border-r data-[state=closed]:slide-out-to-left data-[state=open]:slide-in-from-left sm:max-w-sm",
          side === "top" &&
            "inset-x-0 top-0 h-auto border-b data-[state=closed]:slide-out-to-top data-[state=open]:slide-in-from-top",
          side === "bottom" &&
            "inset-x-0 bottom-0 h-auto border-t data-[state=closed]:slide-out-to-bottom data-[state=open]:slide-in-from-bottom",
          className
        )}
        {...props}
      >
        {children}
        {showCloseButton && (
          <SheetPrimitive.Close className="absolute top-4 right-4 rounded-xs opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:ring-2 focus:ring-ring focus:ring-offset-2 focus:outline-hidden disabled:pointer-events-none data-[state=open]:bg-secondary">
            <XIcon className="size-4" />
            <span className="sr-only">Close</span>
          </SheetPrimitive.Close>
        )}
      </SheetPrimitive.Content>
    </SheetPortal>
  )
}

function SheetHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sheet-header"
      className={cn("flex flex-col gap-1.5 p-4", className)}
      {...props}
    />
  )
}

function SheetFooter({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sheet-footer"
      className={cn("mt-auto flex flex-col gap-2 p-4", className)}
      {...props}
    />
  )
}

function SheetTitle({
  className,
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Title>) {
  return (
    <SheetPrimitive.Title
      data-slot="sheet-title"
      className={cn("font-semibold text-foreground", className)}
      {...props}
    />
  )
}

function SheetDescription({
  className,
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Description>) {
  return (
    <SheetPrimitive.Description
      data-slot="sheet-description"
      className={cn("text-sm text-muted-foreground", className)}
      {...props}
    />
  )
}

export {
  Sheet,
  SheetTrigger,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetFooter,
  SheetTitle,
  SheetDescription,
}
```

## `src/components/ui/sidebar.tsx`

```tsx
import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import { PanelLeftIcon } from "lucide-react"
import { Slot } from "radix-ui"

import { useIsMobile } from "@/hooks/use-mobile"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Separator } from "@/components/ui/separator"
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"

const SIDEBAR_COOKIE_NAME = "sidebar_state"
const SIDEBAR_COOKIE_MAX_AGE = 60 * 60 * 24 * 7
const SIDEBAR_WIDTH = "16rem"
const SIDEBAR_WIDTH_MOBILE = "18rem"
const SIDEBAR_WIDTH_ICON = "3rem"
const SIDEBAR_KEYBOARD_SHORTCUT = "b"

type SidebarContextProps = {
  state: "expanded" | "collapsed"
  open: boolean
  setOpen: (open: boolean) => void
  openMobile: boolean
  setOpenMobile: (open: boolean) => void
  isMobile: boolean
  toggleSidebar: () => void
}

const SidebarContext = React.createContext<SidebarContextProps | null>(null)

function useSidebar() {
  const context = React.useContext(SidebarContext)
  if (!context) {
    throw new Error("useSidebar must be used within a SidebarProvider.")
  }

  return context
}

function SidebarProvider({
  defaultOpen = true,
  open: openProp,
  onOpenChange: setOpenProp,
  className,
  style,
  children,
  ...props
}: React.ComponentProps<"div"> & {
  defaultOpen?: boolean
  open?: boolean
  onOpenChange?: (open: boolean) => void
}) {
  const isMobile = useIsMobile()
  const [openMobile, setOpenMobile] = React.useState(false)

  // This is the internal state of the sidebar.
  // We use openProp and setOpenProp for control from outside the component.
  const [_open, _setOpen] = React.useState(defaultOpen)
  const open = openProp ?? _open
  const setOpen = React.useCallback(
    (value: boolean | ((value: boolean) => boolean)) => {
      const openState = typeof value === "function" ? value(open) : value
      if (setOpenProp) {
        setOpenProp(openState)
      } else {
        _setOpen(openState)
      }

      // This sets the cookie to keep the sidebar state.
      document.cookie = `${SIDEBAR_COOKIE_NAME}=${openState}; path=/; max-age=${SIDEBAR_COOKIE_MAX_AGE}`
    },
    [setOpenProp, open]
  )

  // Helper to toggle the sidebar.
  const toggleSidebar = React.useCallback(() => {
    return isMobile ? setOpenMobile((open) => !open) : setOpen((open) => !open)
  }, [isMobile, setOpen, setOpenMobile])

  // Adds a keyboard shortcut to toggle the sidebar.
  React.useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (
        event.key === SIDEBAR_KEYBOARD_SHORTCUT &&
        (event.metaKey || event.ctrlKey)
      ) {
        event.preventDefault()
        toggleSidebar()
      }
    }

    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [toggleSidebar])

  // We add a state so that we can do data-state="expanded" or "collapsed".
  // This makes it easier to style the sidebar with Tailwind classes.
  const state = open ? "expanded" : "collapsed"

  const contextValue = React.useMemo<SidebarContextProps>(
    () => ({
      state,
      open,
      setOpen,
      isMobile,
      openMobile,
      setOpenMobile,
      toggleSidebar,
    }),
    [state, open, setOpen, isMobile, openMobile, setOpenMobile, toggleSidebar]
  )

  return (
    <SidebarContext.Provider value={contextValue}>
      <TooltipProvider delayDuration={0}>
        <div
          data-slot="sidebar-wrapper"
          style={
            {
              "--sidebar-width": SIDEBAR_WIDTH,
              "--sidebar-width-icon": SIDEBAR_WIDTH_ICON,
              ...style,
            } as React.CSSProperties
          }
          className={cn(
            "group/sidebar-wrapper flex min-h-svh w-full has-data-[variant=inset]:bg-sidebar",
            className
          )}
          {...props}
        >
          {children}
        </div>
      </TooltipProvider>
    </SidebarContext.Provider>
  )
}

function Sidebar({
  side = "left",
  variant = "sidebar",
  collapsible = "offcanvas",
  className,
  children,
  ...props
}: React.ComponentProps<"div"> & {
  side?: "left" | "right"
  variant?: "sidebar" | "floating" | "inset"
  collapsible?: "offcanvas" | "icon" | "none"
}) {
  const { isMobile, state, openMobile, setOpenMobile } = useSidebar()

  if (collapsible === "none") {
    return (
      <div
        data-slot="sidebar"
        className={cn(
          "flex h-full w-(--sidebar-width) flex-col bg-sidebar text-sidebar-foreground",
          className
        )}
        {...props}
      >
        {children}
      </div>
    )
  }

  if (isMobile) {
    return (
      <Sheet open={openMobile} onOpenChange={setOpenMobile} {...props}>
        <SheetContent
          data-sidebar="sidebar"
          data-slot="sidebar"
          data-mobile="true"
          className="w-(--sidebar-width) bg-sidebar p-0 text-sidebar-foreground [&>button]:hidden"
          style={
            {
              "--sidebar-width": SIDEBAR_WIDTH_MOBILE,
            } as React.CSSProperties
          }
          side={side}
        >
          <SheetHeader className="sr-only">
            <SheetTitle>Sidebar</SheetTitle>
            <SheetDescription>Displays the mobile sidebar.</SheetDescription>
          </SheetHeader>
          <div className="flex h-full w-full flex-col">{children}</div>
        </SheetContent>
      </Sheet>
    )
  }

  return (
    <div
      className="group peer hidden text-sidebar-foreground md:block"
      data-state={state}
      data-collapsible={state === "collapsed" ? collapsible : ""}
      data-variant={variant}
      data-side={side}
      data-slot="sidebar"
    >
      {/* This is what handles the sidebar gap on desktop */}
      <div
        data-slot="sidebar-gap"
        className={cn(
          "relative w-(--sidebar-width) bg-transparent transition-[width] duration-200 ease-linear",
          "group-data-[collapsible=offcanvas]:w-0",
          "group-data-[side=right]:rotate-180",
          variant === "floating" || variant === "inset"
            ? "group-data-[collapsible=icon]:w-[calc(var(--sidebar-width-icon)+(--spacing(4)))]"
            : "group-data-[collapsible=icon]:w-(--sidebar-width-icon)"
        )}
      />
      <div
        data-slot="sidebar-container"
        className={cn(
          "fixed inset-y-0 z-10 hidden h-svh w-(--sidebar-width) transition-[left,right,width] duration-200 ease-linear md:flex",
          side === "left"
            ? "left-0 group-data-[collapsible=offcanvas]:left-[calc(var(--sidebar-width)*-1)]"
            : "right-0 group-data-[collapsible=offcanvas]:right-[calc(var(--sidebar-width)*-1)]",
          // Adjust the padding for floating and inset variants.
          variant === "floating" || variant === "inset"
            ? "p-2 group-data-[collapsible=icon]:w-[calc(var(--sidebar-width-icon)+(--spacing(4))+2px)]"
            : "group-data-[collapsible=icon]:w-(--sidebar-width-icon) group-data-[side=left]:border-r group-data-[side=right]:border-l",
          className
        )}
        {...props}
      >
        <div
          data-sidebar="sidebar"
          data-slot="sidebar-inner"
          className="flex h-full w-full flex-col bg-sidebar group-data-[variant=floating]:rounded-lg group-data-[variant=floating]:border group-data-[variant=floating]:border-sidebar-border group-data-[variant=floating]:shadow-sm"
        >
          {children}
        </div>
      </div>
    </div>
  )
}

function SidebarTrigger({
  className,
  onClick,
  ...props
}: React.ComponentProps<typeof Button>) {
  const { toggleSidebar } = useSidebar()

  return (
    <Button
      data-sidebar="trigger"
      data-slot="sidebar-trigger"
      variant="ghost"
      size="icon"
      className={cn("size-7", className)}
      onClick={(event) => {
        onClick?.(event)
        toggleSidebar()
      }}
      {...props}
    >
      <PanelLeftIcon />
      <span className="sr-only">Toggle Sidebar</span>
    </Button>
  )
}

function SidebarRail({ className, ...props }: React.ComponentProps<"button">) {
  const { toggleSidebar } = useSidebar()

  return (
    <button
      data-sidebar="rail"
      data-slot="sidebar-rail"
      aria-label="Toggle Sidebar"
      tabIndex={-1}
      onClick={toggleSidebar}
      title="Toggle Sidebar"
      className={cn(
        "absolute inset-y-0 z-20 hidden w-4 -translate-x-1/2 transition-all ease-linear group-data-[side=left]:-right-4 group-data-[side=right]:left-0 after:absolute after:inset-y-0 after:left-1/2 after:w-[2px] hover:after:bg-sidebar-border sm:flex",
        "in-data-[side=left]:cursor-w-resize in-data-[side=right]:cursor-e-resize",
        "[[data-side=left][data-state=collapsed]_&]:cursor-e-resize [[data-side=right][data-state=collapsed]_&]:cursor-w-resize",
        "group-data-[collapsible=offcanvas]:translate-x-0 group-data-[collapsible=offcanvas]:after:left-full hover:group-data-[collapsible=offcanvas]:bg-sidebar",
        "[[data-side=left][data-collapsible=offcanvas]_&]:-right-2",
        "[[data-side=right][data-collapsible=offcanvas]_&]:-left-2",
        className
      )}
      {...props}
    />
  )
}

function SidebarInset({ className, ...props }: React.ComponentProps<"main">) {
  return (
    <main
      data-slot="sidebar-inset"
      className={cn(
        "relative flex w-full flex-1 flex-col bg-background",
        "md:peer-data-[variant=inset]:m-2 md:peer-data-[variant=inset]:ml-0 md:peer-data-[variant=inset]:rounded-xl md:peer-data-[variant=inset]:shadow-sm md:peer-data-[variant=inset]:peer-data-[state=collapsed]:ml-2",
        className
      )}
      {...props}
    />
  )
}

function SidebarInput({
  className,
  ...props
}: React.ComponentProps<typeof Input>) {
  return (
    <Input
      data-slot="sidebar-input"
      data-sidebar="input"
      className={cn("h-8 w-full bg-background shadow-none", className)}
      {...props}
    />
  )
}

function SidebarHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sidebar-header"
      data-sidebar="header"
      className={cn("flex flex-col gap-2 p-2", className)}
      {...props}
    />
  )
}

function SidebarFooter({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sidebar-footer"
      data-sidebar="footer"
      className={cn("flex flex-col gap-2 p-2", className)}
      {...props}
    />
  )
}

function SidebarSeparator({
  className,
  ...props
}: React.ComponentProps<typeof Separator>) {
  return (
    <Separator
      data-slot="sidebar-separator"
      data-sidebar="separator"
      className={cn("mx-2 w-auto bg-sidebar-border", className)}
      {...props}
    />
  )
}

function SidebarContent({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sidebar-content"
      data-sidebar="content"
      className={cn(
        "flex min-h-0 flex-1 flex-col gap-2 overflow-auto group-data-[collapsible=icon]:overflow-hidden",
        className
      )}
      {...props}
    />
  )
}

function SidebarGroup({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sidebar-group"
      data-sidebar="group"
      className={cn("relative flex w-full min-w-0 flex-col p-2", className)}
      {...props}
    />
  )
}

function SidebarGroupLabel({
  className,
  asChild = false,
  ...props
}: React.ComponentProps<"div"> & { asChild?: boolean }) {
  const Comp = asChild ? Slot.Root : "div"

  return (
    <Comp
      data-slot="sidebar-group-label"
      data-sidebar="group-label"
      className={cn(
        "flex h-8 shrink-0 items-center rounded-md px-2 text-xs font-medium text-sidebar-foreground/70 ring-sidebar-ring outline-hidden transition-[margin,opacity] duration-200 ease-linear focus-visible:ring-2 [&>svg]:size-4 [&>svg]:shrink-0",
        "group-data-[collapsible=icon]:-mt-8 group-data-[collapsible=icon]:opacity-0",
        className
      )}
      {...props}
    />
  )
}

function SidebarGroupAction({
  className,
  asChild = false,
  ...props
}: React.ComponentProps<"button"> & { asChild?: boolean }) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="sidebar-group-action"
      data-sidebar="group-action"
      className={cn(
        "absolute top-3.5 right-3 flex aspect-square w-5 items-center justify-center rounded-md p-0 text-sidebar-foreground ring-sidebar-ring outline-hidden transition-transform hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 [&>svg]:size-4 [&>svg]:shrink-0",
        // Increases the hit area of the button on mobile.
        "after:absolute after:-inset-2 md:after:hidden",
        "group-data-[collapsible=icon]:hidden",
        className
      )}
      {...props}
    />
  )
}

function SidebarGroupContent({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sidebar-group-content"
      data-sidebar="group-content"
      className={cn("w-full text-sm", className)}
      {...props}
    />
  )
}

function SidebarMenu({ className, ...props }: React.ComponentProps<"ul">) {
  return (
    <ul
      data-slot="sidebar-menu"
      data-sidebar="menu"
      className={cn("flex w-full min-w-0 flex-col gap-1", className)}
      {...props}
    />
  )
}

function SidebarMenuItem({ className, ...props }: React.ComponentProps<"li">) {
  return (
    <li
      data-slot="sidebar-menu-item"
      data-sidebar="menu-item"
      className={cn("group/menu-item relative", className)}
      {...props}
    />
  )
}

const sidebarMenuButtonVariants = cva(
  "peer/menu-button flex w-full items-center gap-2 overflow-hidden rounded-md p-2 text-left text-sm ring-sidebar-ring outline-hidden transition-[width,height,padding] group-has-data-[sidebar=menu-action]/menu-item:pr-8 group-data-[collapsible=icon]:size-8! group-data-[collapsible=icon]:p-2! hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 active:bg-sidebar-accent active:text-sidebar-accent-foreground disabled:pointer-events-none disabled:opacity-50 aria-disabled:pointer-events-none aria-disabled:opacity-50 data-[active=true]:bg-sidebar-accent data-[active=true]:font-medium data-[active=true]:text-sidebar-accent-foreground data-[state=open]:hover:bg-sidebar-accent data-[state=open]:hover:text-sidebar-accent-foreground [&>span:last-child]:truncate [&>svg]:size-4 [&>svg]:shrink-0",
  {
    variants: {
      variant: {
        default: "hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
        outline:
          "bg-background shadow-[0_0_0_1px_var(--sidebar-border)] hover:bg-sidebar-accent hover:text-sidebar-accent-foreground hover:shadow-[0_0_0_1px_var(--sidebar-accent)]",
      },
      size: {
        default: "h-8 text-sm",
        sm: "h-7 text-xs",
        lg: "h-12 text-sm group-data-[collapsible=icon]:p-0!",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function SidebarMenuButton({
  asChild = false,
  isActive = false,
  variant = "default",
  size = "default",
  tooltip,
  className,
  ...props
}: React.ComponentProps<"button"> & {
  asChild?: boolean
  isActive?: boolean
  tooltip?: string | React.ComponentProps<typeof TooltipContent>
} & VariantProps<typeof sidebarMenuButtonVariants>) {
  const Comp = asChild ? Slot.Root : "button"
  const { isMobile, state } = useSidebar()

  const button = (
    <Comp
      data-slot="sidebar-menu-button"
      data-sidebar="menu-button"
      data-size={size}
      data-active={isActive}
      className={cn(sidebarMenuButtonVariants({ variant, size }), className)}
      {...props}
    />
  )

  if (!tooltip) {
    return button
  }

  if (typeof tooltip === "string") {
    tooltip = {
      children: tooltip,
    }
  }

  return (
    <Tooltip>
      <TooltipTrigger asChild>{button}</TooltipTrigger>
      <TooltipContent
        side="right"
        align="center"
        hidden={state !== "collapsed" || isMobile}
        {...tooltip}
      />
    </Tooltip>
  )
}

function SidebarMenuAction({
  className,
  asChild = false,
  showOnHover = false,
  ...props
}: React.ComponentProps<"button"> & {
  asChild?: boolean
  showOnHover?: boolean
}) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="sidebar-menu-action"
      data-sidebar="menu-action"
      className={cn(
        "absolute top-1.5 right-1 flex aspect-square w-5 items-center justify-center rounded-md p-0 text-sidebar-foreground ring-sidebar-ring outline-hidden transition-transform peer-hover/menu-button:text-sidebar-accent-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 [&>svg]:size-4 [&>svg]:shrink-0",
        // Increases the hit area of the button on mobile.
        "after:absolute after:-inset-2 md:after:hidden",
        "peer-data-[size=sm]/menu-button:top-1",
        "peer-data-[size=default]/menu-button:top-1.5",
        "peer-data-[size=lg]/menu-button:top-2.5",
        "group-data-[collapsible=icon]:hidden",
        showOnHover &&
          "group-focus-within/menu-item:opacity-100 group-hover/menu-item:opacity-100 peer-data-[active=true]/menu-button:text-sidebar-accent-foreground data-[state=open]:opacity-100 md:opacity-0",
        className
      )}
      {...props}
    />
  )
}

function SidebarMenuBadge({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sidebar-menu-badge"
      data-sidebar="menu-badge"
      className={cn(
        "pointer-events-none absolute right-1 flex h-5 min-w-5 items-center justify-center rounded-md px-1 text-xs font-medium text-sidebar-foreground tabular-nums select-none",
        "peer-hover/menu-button:text-sidebar-accent-foreground peer-data-[active=true]/menu-button:text-sidebar-accent-foreground",
        "peer-data-[size=sm]/menu-button:top-1",
        "peer-data-[size=default]/menu-button:top-1.5",
        "peer-data-[size=lg]/menu-button:top-2.5",
        "group-data-[collapsible=icon]:hidden",
        className
      )}
      {...props}
    />
  )
}

function SidebarMenuSkeleton({
  className,
  showIcon = false,
  ...props
}: React.ComponentProps<"div"> & {
  showIcon?: boolean
}) {
  // Random width between 50 to 90%.
  const width = React.useMemo(() => {
    return `${Math.floor(Math.random() * 40) + 50}%`
  }, [])

  return (
    <div
      data-slot="sidebar-menu-skeleton"
      data-sidebar="menu-skeleton"
      className={cn("flex h-8 items-center gap-2 rounded-md px-2", className)}
      {...props}
    >
      {showIcon && (
        <Skeleton
          className="size-4 rounded-md"
          data-sidebar="menu-skeleton-icon"
        />
      )}
      <Skeleton
        className="h-4 max-w-(--skeleton-width) flex-1"
        data-sidebar="menu-skeleton-text"
        style={
          {
            "--skeleton-width": width,
          } as React.CSSProperties
        }
      />
    </div>
  )
}

function SidebarMenuSub({ className, ...props }: React.ComponentProps<"ul">) {
  return (
    <ul
      data-slot="sidebar-menu-sub"
      data-sidebar="menu-sub"
      className={cn(
        "mx-3.5 flex min-w-0 translate-x-px flex-col gap-1 border-l border-sidebar-border px-2.5 py-0.5",
        "group-data-[collapsible=icon]:hidden",
        className
      )}
      {...props}
    />
  )
}

function SidebarMenuSubItem({
  className,
  ...props
}: React.ComponentProps<"li">) {
  return (
    <li
      data-slot="sidebar-menu-sub-item"
      data-sidebar="menu-sub-item"
      className={cn("group/menu-sub-item relative", className)}
      {...props}
    />
  )
}

function SidebarMenuSubButton({
  asChild = false,
  size = "md",
  isActive = false,
  className,
  ...props
}: React.ComponentProps<"a"> & {
  asChild?: boolean
  size?: "sm" | "md"
  isActive?: boolean
}) {
  const Comp = asChild ? Slot.Root : "a"

  return (
    <Comp
      data-slot="sidebar-menu-sub-button"
      data-sidebar="menu-sub-button"
      data-size={size}
      data-active={isActive}
      className={cn(
        "flex h-7 min-w-0 -translate-x-px items-center gap-2 overflow-hidden rounded-md px-2 text-sidebar-foreground ring-sidebar-ring outline-hidden hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 active:bg-sidebar-accent active:text-sidebar-accent-foreground disabled:pointer-events-none disabled:opacity-50 aria-disabled:pointer-events-none aria-disabled:opacity-50 [&>span:last-child]:truncate [&>svg]:size-4 [&>svg]:shrink-0 [&>svg]:text-sidebar-accent-foreground",
        "data-[active=true]:bg-sidebar-accent data-[active=true]:text-sidebar-accent-foreground",
        size === "sm" && "text-xs",
        size === "md" && "text-sm",
        "group-data-[collapsible=icon]:hidden",
        className
      )}
      {...props}
    />
  )
}

export {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupAction,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInput,
  SidebarInset,
  SidebarMenu,
  SidebarMenuAction,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSkeleton,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarProvider,
  SidebarRail,
  SidebarSeparator,
  SidebarTrigger,
  useSidebar,
}
```

## `src/components/ui/skeleton.tsx`

```tsx
import { cn } from "@/lib/utils"

function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn("animate-pulse rounded-md bg-accent", className)}
      {...props}
    />
  )
}

export { Skeleton }
```

## `src/components/ui/sonner.tsx`

```tsx
import {
  CircleCheckIcon,
  InfoIcon,
  Loader2Icon,
  OctagonXIcon,
  TriangleAlertIcon,
} from "lucide-react"
import { useTheme } from "@/lib/theme"
import { Toaster as Sonner, type ToasterProps } from "sonner"

const Toaster = ({ ...props }: ToasterProps) => {
  const { theme = "system" } = useTheme()

  return (
    <Sonner
      theme={theme as ToasterProps["theme"]}
      className="toaster group"
      icons={{
        success: <CircleCheckIcon className="size-4" />,
        info: <InfoIcon className="size-4" />,
        warning: <TriangleAlertIcon className="size-4" />,
        error: <OctagonXIcon className="size-4" />,
        loading: <Loader2Icon className="size-4 animate-spin" />,
      }}
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "var(--border)",
          "--border-radius": "var(--radius)",
        } as React.CSSProperties
      }
      {...props}
    />
  )
}

export { Toaster }
```

## `src/components/ui/table.tsx`

```tsx
"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

function Table({ className, ...props }: React.ComponentProps<"table">) {
  return (
    <div
      data-slot="table-container"
      className="relative w-full overflow-x-auto"
    >
      <table
        data-slot="table"
        className={cn("w-full caption-bottom text-sm", className)}
        {...props}
      />
    </div>
  )
}

function TableHeader({ className, ...props }: React.ComponentProps<"thead">) {
  return (
    <thead
      data-slot="table-header"
      className={cn("[&_tr]:border-b", className)}
      {...props}
    />
  )
}

function TableBody({ className, ...props }: React.ComponentProps<"tbody">) {
  return (
    <tbody
      data-slot="table-body"
      className={cn("[&_tr:last-child]:border-0", className)}
      {...props}
    />
  )
}

function TableFooter({ className, ...props }: React.ComponentProps<"tfoot">) {
  return (
    <tfoot
      data-slot="table-footer"
      className={cn(
        "border-t bg-muted/50 font-medium [&>tr]:last:border-b-0",
        className
      )}
      {...props}
    />
  )
}

function TableRow({ className, ...props }: React.ComponentProps<"tr">) {
  return (
    <tr
      data-slot="table-row"
      className={cn(
        "border-b transition-colors hover:bg-muted/50 has-aria-expanded:bg-muted/50 data-[state=selected]:bg-muted",
        className
      )}
      {...props}
    />
  )
}

function TableHead({ className, ...props }: React.ComponentProps<"th">) {
  return (
    <th
      data-slot="table-head"
      className={cn(
        "h-10 px-2 text-left align-middle font-medium whitespace-nowrap text-foreground [&:has([role=checkbox])]:pr-0 [&>[role=checkbox]]:translate-y-[2px]",
        className
      )}
      {...props}
    />
  )
}

function TableCell({ className, ...props }: React.ComponentProps<"td">) {
  return (
    <td
      data-slot="table-cell"
      className={cn(
        "p-2 align-middle whitespace-nowrap [&:has([role=checkbox])]:pr-0 [&>[role=checkbox]]:translate-y-[2px]",
        className
      )}
      {...props}
    />
  )
}

function TableCaption({
  className,
  ...props
}: React.ComponentProps<"caption">) {
  return (
    <caption
      data-slot="table-caption"
      className={cn("mt-4 text-sm text-muted-foreground", className)}
      {...props}
    />
  )
}

export {
  Table,
  TableHeader,
  TableBody,
  TableFooter,
  TableHead,
  TableRow,
  TableCell,
  TableCaption,
}
```

## `src/components/ui/tabs.tsx`

```tsx
import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import { Tabs as TabsPrimitive } from "radix-ui"

function Tabs({
  className,
  orientation = "horizontal",
  ...props
}: React.ComponentProps<typeof TabsPrimitive.Root>) {
  return (
    <TabsPrimitive.Root
      data-slot="tabs"
      data-orientation={orientation}
      orientation={orientation}
      className={cn(
        "group/tabs flex gap-2 data-[orientation=horizontal]:flex-col",
        className
      )}
      {...props}
    />
  )
}

const tabsListVariants = cva(
  "group/tabs-list inline-flex w-fit items-center justify-center rounded-lg p-[3px] text-muted-foreground group-data-[orientation=horizontal]/tabs:h-9 group-data-[orientation=vertical]/tabs:h-fit group-data-[orientation=vertical]/tabs:flex-col data-[variant=line]:rounded-none",
  {
    variants: {
      variant: {
        default: "bg-muted",
        line: "gap-1 bg-transparent",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

function TabsList({
  className,
  variant = "default",
  ...props
}: React.ComponentProps<typeof TabsPrimitive.List> &
  VariantProps<typeof tabsListVariants>) {
  return (
    <TabsPrimitive.List
      data-slot="tabs-list"
      data-variant={variant}
      className={cn(tabsListVariants({ variant }), className)}
      {...props}
    />
  )
}

function TabsTrigger({
  className,
  ...props
}: React.ComponentProps<typeof TabsPrimitive.Trigger>) {
  return (
    <TabsPrimitive.Trigger
      data-slot="tabs-trigger"
      className={cn(
        "relative inline-flex h-[calc(100%-1px)] flex-1 items-center justify-center gap-1.5 rounded-md border border-transparent px-2 py-1 text-sm font-medium whitespace-nowrap text-foreground/60 transition-all group-data-[orientation=vertical]/tabs:w-full group-data-[orientation=vertical]/tabs:justify-start hover:text-foreground focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-1 focus-visible:outline-ring disabled:pointer-events-none disabled:opacity-50 group-data-[variant=default]/tabs-list:data-[state=active]:shadow-sm group-data-[variant=line]/tabs-list:data-[state=active]:shadow-none dark:text-muted-foreground dark:hover:text-foreground [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        "group-data-[variant=line]/tabs-list:bg-transparent group-data-[variant=line]/tabs-list:data-[state=active]:bg-transparent dark:group-data-[variant=line]/tabs-list:data-[state=active]:border-transparent dark:group-data-[variant=line]/tabs-list:data-[state=active]:bg-transparent",
        "data-[state=active]:bg-background data-[state=active]:text-foreground dark:data-[state=active]:border-input dark:data-[state=active]:bg-input/30 dark:data-[state=active]:text-foreground",
        "after:absolute after:bg-foreground after:opacity-0 after:transition-opacity group-data-[orientation=horizontal]/tabs:after:inset-x-0 group-data-[orientation=horizontal]/tabs:after:bottom-[-5px] group-data-[orientation=horizontal]/tabs:after:h-0.5 group-data-[orientation=vertical]/tabs:after:inset-y-0 group-data-[orientation=vertical]/tabs:after:-right-1 group-data-[orientation=vertical]/tabs:after:w-0.5 group-data-[variant=line]/tabs-list:data-[state=active]:after:opacity-100",
        className
      )}
      {...props}
    />
  )
}

function TabsContent({
  className,
  ...props
}: React.ComponentProps<typeof TabsPrimitive.Content>) {
  return (
    <TabsPrimitive.Content
      data-slot="tabs-content"
      className={cn("flex-1 outline-none", className)}
      {...props}
    />
  )
}

export { Tabs, TabsList, TabsTrigger, TabsContent, tabsListVariants }
```

## `src/components/ui/tooltip.tsx`

```tsx
import * as React from "react"
import { cn } from "@/lib/utils"
import { Tooltip as TooltipPrimitive } from "radix-ui"

function TooltipProvider({
  delayDuration = 0,
  ...props
}: React.ComponentProps<typeof TooltipPrimitive.Provider>) {
  return (
    <TooltipPrimitive.Provider
      data-slot="tooltip-provider"
      delayDuration={delayDuration}
      {...props}
    />
  )
}

function Tooltip({
  ...props
}: React.ComponentProps<typeof TooltipPrimitive.Root>) {
  return <TooltipPrimitive.Root data-slot="tooltip" {...props} />
}

function TooltipTrigger({
  ...props
}: React.ComponentProps<typeof TooltipPrimitive.Trigger>) {
  return <TooltipPrimitive.Trigger data-slot="tooltip-trigger" {...props} />
}

function TooltipContent({
  className,
  sideOffset = 0,
  children,
  ...props
}: React.ComponentProps<typeof TooltipPrimitive.Content>) {
  return (
    <TooltipPrimitive.Portal>
      <TooltipPrimitive.Content
        data-slot="tooltip-content"
        sideOffset={sideOffset}
        className={cn(
          "z-50 w-fit origin-(--radix-tooltip-content-transform-origin) animate-in rounded-md bg-foreground px-3 py-1.5 text-xs text-balance text-background fade-in-0 zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95",
          className
        )}
        {...props}
      >
        {children}
        <TooltipPrimitive.Arrow className="z-50 size-2.5 translate-y-[calc(-50%_-_2px)] rotate-45 rounded-[2px] bg-foreground fill-foreground" />
      </TooltipPrimitive.Content>
    </TooltipPrimitive.Portal>
  )
}

export { Tooltip, TooltipTrigger, TooltipContent, TooltipProvider }
```

## `src/hooks/use-mobile.ts`

```ts
import * as React from "react"

const MOBILE_BREAKPOINT = 768

export function useIsMobile() {
  const [isMobile, setIsMobile] = React.useState<boolean | undefined>(undefined)

  React.useEffect(() => {
    const mql = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`)
    const onChange = () => {
      setIsMobile(window.innerWidth < MOBILE_BREAKPOINT)
    }
    mql.addEventListener("change", onChange)
    setIsMobile(window.innerWidth < MOBILE_BREAKPOINT)
    return () => mql.removeEventListener("change", onChange)
  }, [])

  return !!isMobile
}
```

## `src/lib/api.ts`

```ts
import type { AuthResponse } from './types'

const BASE_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080/api/v1'
const STORAGE_KEY = 'apex.session'

export interface Session {
  accessToken: string
  refreshToken: string
}

export class ApiError extends Error {
  status: number
  details: unknown
  constructor(status: number, message: string, details?: unknown) {
    super(message)
    this.status = status
    this.details = details
  }
}

export function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}

export function saveSession(session: Session | null) {
  try {
    if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // storage unavailable; session lives for this page only
  }
}

let onSessionExpired: () => void = () => {}
export function setSessionExpiredHandler(fn: () => void) {
  onSessionExpired = fn
}

let refreshing: Promise<Session | null> | null = null

function refreshSession(): Promise<Session | null> {
  refreshing ??= (async () => {
    const current = loadSession()
    if (!current) return null
    const res = await fetch(`${BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: current.refreshToken }),
    })
    if (!res.ok) return null
    const data = (await res.json()) as AuthResponse
    const next = { accessToken: data.accessToken, refreshToken: data.refreshToken }
    saveSession(next)
    return next
  })().finally(() => {
    refreshing = null
  })
  return refreshing
}

type Params = Record<string, string | number | boolean | undefined | null>

function buildUrl(path: string, params?: Params) {
  const url = new URL(`${BASE_URL}${path}`)
  for (const [k, v] of Object.entries(params ?? {})) {
    if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, String(v))
  }
  return url.toString()
}

async function request<T>(method: string, path: string, opts: { params?: Params; body?: unknown; auth?: boolean } = {}): Promise<T> {
  const { params, body, auth = true } = opts
  const send = (token?: string) =>
    fetch(buildUrl(path, params), {
      method,
      headers: {
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })

  let res = await send(auth ? loadSession()?.accessToken : undefined)

  if (res.status === 401 && auth) {
    const next = await refreshSession()
    if (next) {
      res = await send(next.accessToken)
    } else {
      saveSession(null)
      onSessionExpired()
    }
  }

  if (!res.ok) {
    let message = res.statusText || 'Request failed'
    let details: unknown
    try {
      const err = (await res.json()) as { message?: string; details?: unknown }
      message = err.message ?? message
      details = err.details
    } catch {
      // non-JSON error body
    }
    throw new ApiError(res.status, message, details)
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export const api = {
  get: <T>(path: string, params?: Params) => request<T>('GET', path, { params }),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, { body }),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, { body }),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, { body }),
  delete: <T>(path: string) => request<T>('DELETE', path),
  public: {
    post: <T>(path: string, body?: unknown) => request<T>('POST', path, { body, auth: false }),
  },
}
```

## `src/lib/format.ts`

```ts
const currency = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' })
const compact = new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 })
const integer = new Intl.NumberFormat()

export const formatCurrency = (n: number | null | undefined) => currency.format(n ?? 0)
export const formatCompact = (n: number | null | undefined) => compact.format(n ?? 0)
export const formatNumber = (n: number | null | undefined) => integer.format(n ?? 0)
export const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
```

## `src/lib/guards.ts`

```ts
import { redirect } from '@tanstack/react-router'
import type { AuthState } from '#/auth'

/** Route guard: send users without every listed permission back to the dashboard. */
export const requires =
  (...permissions: string[]) =>
  ({ context }: { context: { auth: AuthState } }) => {
    if (!context.auth.can(...permissions)) throw redirect({ to: '/' })
  }
```

## `src/lib/queries.ts`

```ts
import { keepPreviousData, queryOptions } from '@tanstack/react-query'
import { api } from './api'
import type { Alert, AuditLog, Category, Customer, Dashboard, Movement, Order, OrderReturn, Page, Product, PurchaseOrder, ReorderRule, ReplenishmentSuggestion, Role, SalesReport, StockLevel, Supplier, TopProductRow, Transfer, User, Warehouse } from './types'

const isoDate = (d: Date) => d.toISOString().slice(0, 10)

export const daysAgo = (n: number) => {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return isoDate(d)
}

export const dashboardQuery = queryOptions({
  queryKey: ['dashboard'],
  queryFn: () => api.get<Dashboard>('/dashboard'),
})

export const salesReportQuery = (from: string, to: string) =>
  queryOptions({
    queryKey: ['reports', 'sales', from, to],
    queryFn: () => api.get<SalesReport>('/reports/sales', { from, to }),
  })

export const topProductsQuery = (from: string, to: string, limit = 5) =>
  queryOptions({
    queryKey: ['reports', 'top-products', from, to, limit],
    queryFn: () => api.get<TopProductRow[]>('/reports/top-products', { from, to, limit }),
  })

export const openAlertsQuery = (size = 5) =>
  queryOptions({
    queryKey: ['alerts', 'open', size],
    queryFn: () => api.get<Page<Alert>>('/alerts', { status: 'OPEN', size }),
  })

export const categoriesQuery = queryOptions({
  queryKey: ['categories'],
  queryFn: () => api.get<Category[]>('/categories'),
  staleTime: 5 * 60_000,
})

export const productsQuery = (params: { page: number; keyword?: string; categoryId?: string }) =>
  queryOptions({
    queryKey: ['products', params],
    queryFn: () =>
      api.get<Page<Product>>('/products', {
        page: params.page,
        size: 20,
        keyword: params.keyword,
        categoryId: params.categoryId,
      }),
    placeholderData: keepPreviousData,
  })

export const productQuery = (id: string) =>
  queryOptions({
    queryKey: ['products', id],
    queryFn: () => api.get<Product>(`/products/${id}`),
  })

export const warehousesQuery = queryOptions({
  queryKey: ['warehouses'],
  queryFn: () => api.get<Page<Warehouse>>('/warehouses', { size: 100 }),
  staleTime: 5 * 60_000,
})

export const stockQuery = (params: { page: number; warehouseId?: string; inStockOnly?: boolean }) =>
  queryOptions({
    queryKey: ['inventory', 'stock', params],
    queryFn: () => api.get<Page<StockLevel>>('/inventory', { size: 20, ...params }),
    placeholderData: keepPreviousData,
  })

export const movementsQuery = (params: { page: number; warehouseId?: string }) =>
  queryOptions({
    queryKey: ['inventory', 'movements', params],
    queryFn: () => api.get<Page<Movement>>('/inventory/movements', { size: 20, sort: 'createdAt,desc', ...params }),
    placeholderData: keepPreviousData,
  })

export const alertsQuery = (params: { page: number; status?: string }) =>
  queryOptions({
    queryKey: ['alerts', 'list', params],
    queryFn: () => api.get<Page<Alert>>('/alerts', { size: 20, ...params }),
    placeholderData: keepPreviousData,
  })

export const suggestionsQuery = queryOptions({
  queryKey: ['alerts', 'suggestions'],
  queryFn: () => api.get<ReplenishmentSuggestion[]>('/reorder-rules/suggestions'),
})

export const ordersQuery = (params: { page: number; status?: string; orderNumber?: string }) =>
  queryOptions({
    queryKey: ['orders', 'list', params],
    queryFn: () => api.get<Page<Order>>('/orders', { size: 20, sort: 'createdAt,desc', ...params }),
    placeholderData: keepPreviousData,
  })

export const orderQuery = (id: string) =>
  queryOptions({ queryKey: ['orders', id], queryFn: () => api.get<Order>(`/orders/${id}`) })

export const suppliersQuery = (params: { page: number; keyword?: string }) =>
  queryOptions({
    queryKey: ['suppliers', params],
    queryFn: () => api.get<Page<Supplier>>('/suppliers', { size: 20, ...params }),
    placeholderData: keepPreviousData,
  })

export const purchaseOrdersQuery = (params: { page: number; status?: string }) =>
  queryOptions({
    queryKey: ['purchase-orders', 'list', params],
    queryFn: () => api.get<Page<PurchaseOrder>>('/purchase-orders', { size: 20, sort: 'createdAt,desc', ...params }),
    placeholderData: keepPreviousData,
  })

export const purchaseOrderQuery = (id: string) =>
  queryOptions({ queryKey: ['purchase-orders', id], queryFn: () => api.get<PurchaseOrder>(`/purchase-orders/${id}`) })

export const usersQuery = (page: number) =>
  queryOptions({
    queryKey: ['users', page],
    queryFn: () => api.get<Page<User>>('/users', { size: 20, page }),
    placeholderData: keepPreviousData,
  })

export const rolesQuery = queryOptions({
  queryKey: ['roles'],
  queryFn: () => api.get<Role[]>('/roles'),
})

export const auditLogsQuery = (params: { page: number; method?: string }) =>
  queryOptions({
    queryKey: ['audit-logs', params],
    queryFn: () => api.get<Page<AuditLog>>('/audit-logs', { size: 25, sort: 'createdAt,desc', ...params }),
    placeholderData: keepPreviousData,
  })

export const customersListQuery = (params: { page: number; keyword?: string }) =>
  queryOptions({
    queryKey: ['customers', 'list', params],
    queryFn: () => api.get<Page<Customer>>('/customers', { size: 20, sort: 'name,asc', ...params }),
    placeholderData: keepPreviousData,
  })

export const customersQuery = queryOptions({
  queryKey: ['customers'],
  queryFn: () => api.get<Page<Customer>>('/customers', { size: 100, sort: 'name,asc' }),
})

export const supplierOptionsQuery = queryOptions({
  queryKey: ['suppliers', 'options'],
  queryFn: () => api.get<Page<Supplier>>('/suppliers', { size: 100, active: true, sort: 'name,asc' }),
})

export const transfersQuery = (params: { page: number; status?: string }) =>
  queryOptions({
    queryKey: ['transfers', 'list', params],
    queryFn: () => api.get<Page<Transfer>>('/transfers', { size: 20, sort: 'createdAt,desc', ...params }),
    placeholderData: keepPreviousData,
  })

export const transferQuery = (id: string) =>
  queryOptions({ queryKey: ['transfers', id], queryFn: () => api.get<Transfer>(`/transfers/${id}`) })

export const reorderRulesQuery = queryOptions({
  queryKey: ['reorder-rules'],
  queryFn: () => api.get<ReorderRule[]>('/reorder-rules'),
})

export const permissionsQuery = queryOptions({
  queryKey: ['roles', 'permissions'],
  queryFn: () => api.get<string[]>('/roles/permissions'),
  staleTime: 10 * 60_000,
})

export const orderReturnsQuery = (orderId: string) =>
  queryOptions({
    queryKey: ['orders', orderId, 'returns'],
    queryFn: () => api.get<OrderReturn[]>(`/orders/${orderId}/returns`),
  })
```

## `src/lib/theme.tsx`

```tsx
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

export type Theme = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'apex.theme'
const query = () => window.matchMedia('(prefers-color-scheme: dark)')

function readStored(): Theme {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    if (v === 'light' || v === 'dark' || v === 'system') return v
  } catch {
    // storage unavailable
  }
  return 'system'
}

const isDark = (theme: Theme) => theme === 'dark' || (theme === 'system' && query().matches)

const ThemeContext = createContext<{ theme: Theme; setTheme: (t: Theme) => void } | null>(null)

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(readStored)

  useEffect(() => {
    const apply = () => document.documentElement.classList.toggle('dark', isDark(theme))
    apply()
    if (theme !== 'system') return
    const mq = query()
    mq.addEventListener('change', apply)
    return () => mq.removeEventListener('change', apply)
  }, [theme])

  const setTheme = useCallback((t: Theme) => {
    try {
      localStorage.setItem(STORAGE_KEY, t)
    } catch {
      // ignore
    }
    setThemeState(t)
  }, [])

  const value = useMemo(() => ({ theme, setTheme }), [theme, setTheme])
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider')
  return ctx
}
```

## `src/lib/types.ts`

```ts
export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface User {
  id: string
  firstName: string
  lastName: string
  email: string
  roleName: string
  permissions: string[]
  isActive: boolean
  mfaEnabled: boolean
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface Dashboard {
  activeProducts: number
  activeWarehouses: number
  totalUnitsOnHand: number
  stockCostValue: number
  openAlerts: number
  pendingOrders: number
  ordersToShip: number
  openPurchaseOrders: number
  ordersToday: number
  revenueToday: number
  generatedAt: string
}

export interface SalesDay {
  date: string
  orders: number
  units: number
  revenue: number
  grossProfit: number
}

export interface SalesReport {
  from: string
  to: string
  orders: number
  unitsSold: number
  revenue: number
  cost: number
  grossProfit: number
  refunds: number
  netRevenue: number
  days: SalesDay[]
}

export interface TopProductRow {
  variantId: string
  sku: string
  productName: string
  unitsSold: number
  revenue: number
}

export interface Alert {
  id: string
  variantId: string
  sku: string
  productName: string
  warehouseId: string
  warehouseCode: string
  type: 'LOW_STOCK' | 'OUT_OF_STOCK' | string
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED' | string
  availableQuantity: number
  reorderPoint: number
  createdAt: string
}

export interface Barcode {
  id: string
  barcode: string
  type: string
  isPrimary: boolean
}

export interface Variant {
  id: string
  productId: string
  sku: string
  variantAttributes: Record<string, unknown> | null
  costPrice: number
  retailPrice: number
  isActive: boolean
  barcodes: Barcode[]
}

export interface Product {
  id: string
  categoryId: string
  categoryName: string
  name: string
  slug: string
  description: string | null
  brand: string | null
  unitOfMeasure: string
  hasVariants: boolean
  isActive: boolean
  variants: Variant[]
  mediaUrls: string[]
  createdAt: string
}

export interface Category {
  id: string
  parentId: string | null
  name: string
  slug: string
  description: string | null
  isActive: boolean
  subcategories: Category[]
}

export interface StockLevel {
  id: string
  variantId: string
  sku: string
  productName: string
  warehouseId: string
  warehouseCode: string
  locationCode: string | null
  quantityOnHand: number
  quantityReserved: number
  quantityAvailable: number
  updatedAt: string
}

export interface Warehouse {
  id: string
  code: string
  name: string
  city: string | null
  country: string | null
  contactPhone: string | null
  isActive: boolean
}

export interface Movement {
  id: string
  variantId: string
  sku: string
  warehouseId: string
  warehouseCode: string
  type: string
  quantityChange: number
  quantityAfter: number
  referenceType: string | null
  reason: string | null
  createdAt: string
}

export interface ReplenishmentSuggestion {
  variantId: string
  sku: string
  productName: string
  warehouseId: string
  warehouseCode: string
  availableQuantity: number
  reorderPoint: number
  suggestedQuantity: number
  preferredSupplierName: string | null
  estimatedUnitCost: number | null
}

export type OrderStatus = 'PENDING' | 'PICKING' | 'PACKED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

export interface OrderLine {
  id: string
  variantId: string
  sku: string
  productName: string
  quantity: number
  quantityPicked: number
  quantityReturned: number
  unitPrice: number
  discountPercent: number | null
  lineTotal: number
}

export interface Order {
  id: string
  orderNumber: string
  customerName: string | null
  warehouseCode: string
  status: OrderStatus
  channel: string | null
  shippingAddress: string | null
  notes: string | null
  subtotal: number
  taxAmount: number
  total: number
  carrier: string | null
  trackingNumber: string | null
  lines: OrderLine[]
  shippedAt: string | null
  deliveredAt: string | null
  createdAt: string
}

export interface Supplier {
  id: string
  code: string
  name: string
  contactName: string | null
  email: string | null
  phone: string | null
  city: string | null
  country: string | null
  paymentTerms: string | null
  leadTimeDays: number | null
  isActive: boolean
}

export interface PoLine {
  id: string
  variantId: string
  sku: string
  productName: string
  quantityOrdered: number
  quantityReceived: number
  quantityOutstanding: number
  unitCost: number
  lineTotal: number
}

export interface PurchaseOrder {
  id: string
  poNumber: string
  supplierName: string
  warehouseCode: string
  status: 'DRAFT' | 'ORDERED' | 'PARTIALLY_RECEIVED' | 'RECEIVED' | 'CANCELLED' | 'CLOSED'
  expectedDate: string | null
  notes: string | null
  totalAmount: number
  lines: PoLine[]
  orderedAt: string | null
  createdAt: string
}

export interface Role {
  id: string
  name: string
  permissions: string[]
  builtIn: boolean
  userCount: number
  createdAt: string
}

export interface AuditLog {
  id: string
  userEmail: string | null
  method: string
  path: string
  resource: string | null
  statusCode: number
  ipAddress: string | null
  createdAt: string
}

export interface Customer {
  id: string
  name: string
  email: string | null
  phone: string | null
  city: string | null
  country: string | null
  notes: string | null
  isActive: boolean
}

export type TransferStatus = 'REQUESTED' | 'IN_TRANSIT' | 'RECEIVED' | 'CANCELLED'

export interface TransferLine {
  id: string
  variantId: string
  sku: string
  quantity: number
  receivedQuantity: number | null
}

export interface Transfer {
  id: string
  referenceNumber: string
  fromWarehouseCode: string
  toWarehouseCode: string
  status: TransferStatus
  notes: string | null
  lines: TransferLine[]
  dispatchedAt: string | null
  receivedAt: string | null
  createdAt: string
}

export interface ReorderRule {
  id: string
  variantId: string
  sku: string
  productName: string
  warehouseId: string
  warehouseCode: string
  reorderPoint: number
  reorderQuantity: number
  isActive: boolean
}

export interface OrderReturn {
  id: string
  returnNumber: string
  reason: string | null
  restocked: boolean
  refundAmount: number
  lines: { orderLineId: string; sku: string; quantity: number; refundAmount: number }[]
  createdAt: string
}
```

## `src/lib/utils.ts`

```ts
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

## `src/main.tsx`

```tsx
import ReactDOM from 'react-dom/client'
import { RouterProvider } from '@tanstack/react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '#/lib/theme'
import { AuthProvider, useAuth } from './auth'
import { router } from './router'
import { Toaster } from '#/components/ui/sonner'
import { TooltipProvider } from '#/components/ui/tooltip'
import { ApiError } from '#/lib/api'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 2,
    },
  },
})

function InnerApp() {
  const auth = useAuth()
  if (auth.status === 'loading') {
    return (
      <div className="grid min-h-svh place-items-center text-sm text-muted-foreground" role="status">
        Loading…
      </div>
    )
  }
  return <RouterProvider router={router} context={{ auth }} />
}

const rootElement = document.getElementById('app')!

if (!rootElement.innerHTML) {
  ReactDOM.createRoot(rootElement).render(
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <TooltipProvider>
            <InnerApp />
          </TooltipProvider>
          <Toaster richColors position="top-right" />
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>,
  )
}
```

## `src/router.tsx`

```tsx
import { createRouter } from '@tanstack/react-router'
import { routeTree } from './routeTree.gen'
import type { AuthState } from './auth'

export const router = createRouter({
  routeTree,
  context: { auth: undefined! as AuthState },
  defaultPreload: 'intent',
  scrollRestoration: true,
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
```

## `src/routes/__root.tsx`

```tsx
import { Outlet, createRootRouteWithContext } from '@tanstack/react-router'
import type { AuthState } from '#/auth'
import '../styles.css'

export const Route = createRootRouteWithContext<{ auth: AuthState }>()({
  component: Outlet,
  notFoundComponent: () => (
    <div className="grid min-h-svh place-items-center p-8 text-center">
      <div>
        <p className="text-5xl font-semibold">404</p>
        <p className="mt-2 text-muted-foreground">This page does not exist.</p>
      </div>
    </div>
  ),
})
```

## `src/routes/_authenticated.tsx`

```tsx
import { AppShell } from "#/components/layout/app-shell"
import { createFileRoute, redirect } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated')({
  beforeLoad: ({ context, location }) => {
    if (!context.auth.isAuthenticated) {
      throw redirect({ to: '/login', search: { redirect: location.href } })
    }
  },
  component: AppShell,
})
```

## `src/routes/_authenticated/alerts/index.tsx`

```tsx
import { requires } from '#/lib/guards'
import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { RuleDialog } from '#/components/inventory/rule-dialog'
import type { ReorderRule } from '#/lib/types'
import { z } from 'zod'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAuth } from '#/auth'
import { api } from '#/lib/api'
import { alertsQuery, reorderRulesQuery, suggestionsQuery } from '#/lib/queries'
import { formatCurrency } from '#/lib/format'

export const Route = createFileRoute('/_authenticated/alerts/')({
  beforeLoad: requires('alert:read'),
  validateSearch: z.object({
    view: z.enum(['OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'suggestions', 'rules']).catch('OPEN').default('OPEN'),
    page: z.number().int().min(0).catch(0).default(0),
  }),
  component: AlertsPage,
})

function AlertsPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  return (
    <>
      <PageHeader title="Alerts" description="Low-stock alerts and suggested replenishment." />
      <Tabs className="mb-4" value={search.view} onValueChange={(v) => navigate({ search: { view: v as typeof search.view, page: 0 } })}>
        <TabsList>
          <TabsTrigger value="OPEN">Open</TabsTrigger>
          <TabsTrigger value="ACKNOWLEDGED">Acknowledged</TabsTrigger>
          <TabsTrigger value="RESOLVED">Resolved</TabsTrigger>
          <TabsTrigger value="suggestions">Replenishment</TabsTrigger>
          <TabsTrigger value="rules">Rules</TabsTrigger>
        </TabsList>
      </Tabs>
      {search.view === 'suggestions' ? <Suggestions /> : search.view === 'rules' ? <Rules /> : <AlertList status={search.view} />}
    </>
  )
}

const Loading = () => <Skeleton className="h-48 w-full" />

function AlertList({ status }: { status: string }) {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { can } = useAuth()
  const queryClient = useQueryClient()
  const { data, isPending, isError } = useQuery(alertsQuery({ page: search.page, status }))

  const acknowledge = useMutation({
    mutationFn: (id: string) => api.post(`/alerts/${id}/acknowledge`),
    onSuccess: () => {
      toast.success('Alert acknowledged')
      queryClient.invalidateQueries({ queryKey: ['alerts'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (e: Error) => toast.error(e.message),
  })

  if (isError) return <EmptyState title="Could not load alerts" />
  if (isPending) return <Loading />
  if (!data.content.length) return <EmptyState title="Nothing here" description="No alerts with this status." />

  return (
    <>
      <Card className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Type</TableHead>
              <TableHead>Product</TableHead>
              <TableHead>Warehouse</TableHead>
              <TableHead className="text-right">Available</TableHead>
              <TableHead className="text-right">Reorder point</TableHead>
              <TableHead>Raised</TableHead>
              {status === 'OPEN' && can('alert:manage') && <TableHead className="w-32" />}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.map((a) => (
              <TableRow key={a.id}>
                <TableCell>
                  <Badge variant={a.type === 'OUT_OF_STOCK' ? 'destructive' : 'secondary'}>
                    {a.type === 'OUT_OF_STOCK' ? 'Out of stock' : 'Low stock'}
                  </Badge>
                </TableCell>
                <TableCell>
                  <p className="font-medium">{a.productName}</p>
                  <p className="font-mono text-xs text-muted-foreground">{a.sku}</p>
                </TableCell>
                <TableCell>{a.warehouseCode}</TableCell>
                <TableCell className="text-right tabular-nums">{a.availableQuantity}</TableCell>
                <TableCell className="text-right tabular-nums">{a.reorderPoint}</TableCell>
                <TableCell className="whitespace-nowrap text-muted-foreground">{new Date(a.createdAt).toLocaleDateString()}</TableCell>
                {status === 'OPEN' && can('alert:manage') && (
                  <TableCell className="text-right">
                    <Button variant="outline" size="sm" disabled={acknowledge.isPending} onClick={() => acknowledge.mutate(a.id)}>
                      Acknowledge
                    </Button>
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>
      <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
    </>
  )
}

function Suggestions() {
  const { data, isPending, isError } = useQuery(suggestionsQuery)
  if (isError) return <EmptyState title="Could not load suggestions" />
  if (isPending) return <Loading />
  if (!data.length) return <EmptyState title="No replenishment needed" description="Nothing is at or below its reorder point." />
  return (
    <Card className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Product</TableHead>
            <TableHead>Warehouse</TableHead>
            <TableHead className="text-right">Available</TableHead>
            <TableHead className="text-right">Order qty</TableHead>
            <TableHead>Preferred supplier</TableHead>
            <TableHead className="text-right">Est. unit cost</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((s) => (
            <TableRow key={`${s.variantId}-${s.warehouseId}`}>
              <TableCell>
                <p className="font-medium">{s.productName}</p>
                <p className="font-mono text-xs text-muted-foreground">{s.sku}</p>
              </TableCell>
              <TableCell>{s.warehouseCode}</TableCell>
              <TableCell className="text-right tabular-nums">
                {s.availableQuantity} / {s.reorderPoint}
              </TableCell>
              <TableCell className="text-right font-medium tabular-nums">{s.suggestedQuantity}</TableCell>
              <TableCell>{s.preferredSupplierName ?? '—'}</TableCell>
              <TableCell className="text-right tabular-nums">{s.estimatedUnitCost != null ? formatCurrency(s.estimatedUnitCost) : '—'}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  )
}

function Rules() {
  const { can } = useAuth()
  const queryClient = useQueryClient()
  const { data, isPending, isError } = useQuery(reorderRulesQuery)
  const [dialog, setDialog] = useState<{ open: boolean; rule?: ReorderRule }>({ open: false })
  const manage = can('alert:manage')

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/reorder-rules/${id}`),
    onSuccess: () => {
      toast.success('Rule deleted')
      queryClient.invalidateQueries({ queryKey: ['reorder-rules'] })
    },
    onError: (e: Error) => toast.error(e.message),
  })

  if (isError) return <EmptyState title="Could not load rules" />
  if (isPending) return <Loading />

  return (
    <>
      {manage && (
        <div className="mb-4">
          <Button onClick={() => setDialog({ open: true })}>
            <Plus /> New rule
          </Button>
        </div>
      )}
      <RuleDialog open={dialog.open} onOpenChange={(o) => setDialog((d) => ({ ...d, open: o }))} rule={dialog.rule} />
      {!data.length ? (
        <EmptyState title="No reorder rules" description="Rules raise an alert when stock falls to the reorder point." />
      ) : (
        <Card className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Product</TableHead>
                <TableHead>Warehouse</TableHead>
                <TableHead className="text-right">Reorder point</TableHead>
                <TableHead className="text-right">Reorder qty</TableHead>
                <TableHead>Status</TableHead>
                {manage && <TableHead className="w-24" />}
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.map((r) => (
                <TableRow key={r.id}>
                  <TableCell>
                    <p className="font-medium">{r.productName}</p>
                    <p className="font-mono text-xs text-muted-foreground">{r.sku}</p>
                  </TableCell>
                  <TableCell>{r.warehouseCode}</TableCell>
                  <TableCell className="text-right tabular-nums">{r.reorderPoint}</TableCell>
                  <TableCell className="text-right tabular-nums">{r.reorderQuantity}</TableCell>
                  <TableCell>
                    <Badge variant={r.isActive ? 'default' : 'secondary'}>{r.isActive ? 'Active' : 'Inactive'}</Badge>
                  </TableCell>
                  {manage && (
                    <TableCell className="whitespace-nowrap text-right">
                      <Button variant="ghost" size="icon" aria-label={`Edit rule for ${r.sku}`} onClick={() => setDialog({ open: true, rule: r })}>
                        <Pencil />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Delete rule for ${r.sku}`}
                        disabled={remove.isPending}
                        onClick={() => confirm(`Delete the rule for ${r.sku} at ${r.warehouseCode}?`) && remove.mutate(r.id)}
                      >
                        <Trash2 />
                      </Button>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/audit/index.tsx`

```tsx
import { requires } from '#/lib/guards'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { z } from 'zod'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { Badge } from '@/components/ui/badge'
import { Card } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { auditLogsQuery } from '#/lib/queries'

export const Route = createFileRoute('/_authenticated/audit/')({
  beforeLoad: requires('audit:read'),
  validateSearch: z.object({
    page: z.number().int().min(0).catch(0).default(0),
    method: z.string().optional(),
  }),
  component: AuditPage,
})

function AuditPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { data, isPending, isError } = useQuery(auditLogsQuery(search))

  return (
    <>
      <PageHeader title="Audit log" description="Every change made through the API." />
      <div className="mb-4">
        <Select value={search.method ?? 'all'} onValueChange={(v) => navigate({ search: { method: v === 'all' ? undefined : v, page: 0 } })}>
          <SelectTrigger className="w-full sm:w-48" aria-label="Filter by method">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All methods</SelectItem>
            {['POST', 'PUT', 'PATCH', 'DELETE'].map((m) => (
              <SelectItem key={m} value={m}>
                {m}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {isError ? (
        <EmptyState title="Could not load audit log" />
      ) : isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : !data.content.length ? (
        <EmptyState title="No entries" />
      ) : (
        <>
          <Card className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>When</TableHead>
                  <TableHead>User</TableHead>
                  <TableHead>Action</TableHead>
                  <TableHead>Result</TableHead>
                  <TableHead>IP</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((l) => (
                  <TableRow key={l.id}>
                    <TableCell className="whitespace-nowrap text-muted-foreground">{new Date(l.createdAt).toLocaleString()}</TableCell>
                    <TableCell>{l.userEmail ?? '—'}</TableCell>
                    <TableCell className="font-mono text-xs">
                      <Badge variant="secondary" className="mr-2">{l.method}</Badge>
                      {l.path}
                    </TableCell>
                    <TableCell>
                      <Badge variant={l.statusCode >= 400 ? 'destructive' : 'outline'}>{l.statusCode}</Badge>
                    </TableCell>
                    <TableCell className="font-mono text-xs text-muted-foreground">{l.ipAddress ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
          <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
        </>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/customers/index.tsx`

```tsx
import { useEffect, useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Pencil, Plus, Search } from 'lucide-react'
import { z } from 'zod'
import { CustomerDialog } from '#/components/sales/customer-dialog'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '#/auth'
import { requires } from '#/lib/guards'
import { customersListQuery } from '#/lib/queries'
import type { Customer } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/customers/')({
  beforeLoad: requires('customer:read'),
  validateSearch: z.object({
    page: z.number().int().min(0).catch(0).default(0),
    keyword: z.string().optional(),
  }),
  component: CustomersPage,
})

function CustomersPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { can } = useAuth()
  const [keyword, setKeyword] = useState(search.keyword ?? '')
  const [dialog, setDialog] = useState<{ open: boolean; customer?: Customer }>({ open: false })

  useEffect(() => {
    const t = setTimeout(() => {
      if ((search.keyword ?? '') !== keyword) navigate({ search: { keyword: keyword || undefined, page: 0 } })
    }, 300)
    return () => clearTimeout(t)
  }, [keyword, navigate, search.keyword])

  const { data, isPending, isError } = useQuery(customersListQuery(search))

  return (
    <>
      <PageHeader
        title="Customers"
        description="People and businesses you sell to."
        actions={
          can('customer:create') && (
            <Button onClick={() => setDialog({ open: true })}>
              <Plus /> New customer
            </Button>
          )
        }
      />
      <CustomerDialog open={dialog.open} onOpenChange={(o) => setDialog((d) => ({ ...d, open: o }))} customer={dialog.customer} />
      <div className="relative mb-4 max-w-md">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Search customers…" className="pl-9" aria-label="Search customers" />
      </div>
      {isError ? (
        <EmptyState title="Could not load customers" />
      ) : isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : !data.content.length ? (
        <EmptyState title="No customers found" />
      ) : (
        <>
          <Card className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Phone</TableHead>
                  <TableHead>Location</TableHead>
                  <TableHead>Status</TableHead>
                  {can('customer:update') && <TableHead className="w-16" />}
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((c) => (
                  <TableRow key={c.id}>
                    <TableCell className="font-medium">{c.name}</TableCell>
                    <TableCell>{c.email ?? '—'}</TableCell>
                    <TableCell>{c.phone ?? '—'}</TableCell>
                    <TableCell>{[c.city, c.country].filter(Boolean).join(', ') || '—'}</TableCell>
                    <TableCell>
                      <Badge variant={c.isActive ? 'default' : 'secondary'}>{c.isActive ? 'Active' : 'Inactive'}</Badge>
                    </TableCell>
                    {can('customer:update') && (
                      <TableCell className="text-right">
                        <Button variant="ghost" size="icon" aria-label={`Edit ${c.name}`} onClick={() => setDialog({ open: true, customer: c })}>
                          <Pencil />
                        </Button>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
          <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
        </>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/index.tsx`

```tsx
import { createFileRoute } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { AlertTriangle, Banknote, Boxes, ClipboardList, Package, ShoppingCart, Truck, Warehouse } from 'lucide-react'
import { PageHeader } from '#/components/common/page-header'
import { StatCard } from '#/components/common/stat-card'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuth } from '#/auth'
import { dashboardQuery, daysAgo, openAlertsQuery, salesReportQuery, topProductsQuery } from '#/lib/queries'
import { formatCompact, formatCurrency, formatNumber } from '#/lib/format'

export const Route = createFileRoute('/_authenticated/')({ component: Dashboard })

function Dashboard() {
  const { user, can } = useAuth()
  const canReport = can('report:read')
  const canAlerts = can('alert:read')

  if (!canReport) {
    return (
      <>
        <PageHeader title={`Welcome, ${user?.firstName}`} description="Pick a section from the navigation to get started." />
      </>
    )
  }

  return (
    <>
      <PageHeader title="Dashboard" description={`Welcome back, ${user?.firstName}. Here is how the business looks right now.`} />
      <Kpis />
      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <SalesTrend />
        <TopProducts />
      </div>
      {canAlerts && <OpenAlerts />}
    </>
  )
}

function Kpis() {
  const { data, isPending } = useQuery(dashboardQuery)
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard loading={isPending} label="Revenue today" value={formatCurrency(data?.revenueToday)} hint={`${formatNumber(data?.ordersToday)} orders`} icon={Banknote} />
      <StatCard loading={isPending} label="Stock value (cost)" value={formatCurrency(data?.stockCostValue)} hint={`${formatCompact(data?.totalUnitsOnHand)} units on hand`} icon={Boxes} />
      <StatCard loading={isPending} label="Orders to ship" value={formatNumber(data?.ordersToShip)} hint={`${formatNumber(data?.pendingOrders)} pending`} icon={ShoppingCart} />
      <StatCard loading={isPending} label="Open alerts" value={formatNumber(data?.openAlerts)} icon={AlertTriangle} />
      <StatCard loading={isPending} label="Active products" value={formatNumber(data?.activeProducts)} icon={Package} />
      <StatCard loading={isPending} label="Warehouses" value={formatNumber(data?.activeWarehouses)} icon={Warehouse} />
      <StatCard loading={isPending} label="Open purchase orders" value={formatNumber(data?.openPurchaseOrders)} icon={Truck} />
      <StatCard loading={isPending} label="Pending orders" value={formatNumber(data?.pendingOrders)} icon={ClipboardList} />
    </div>
  )
}

function SalesTrend() {
  const from = daysAgo(13)
  const to = daysAgo(0)
  const { data, isPending } = useQuery(salesReportQuery(from, to))
  const days = data?.days ?? []
  const max = Math.max(1, ...days.map((d) => d.revenue))

  return (
    <Card className="lg:col-span-2">
      <CardHeader>
        <CardTitle>Revenue, last 14 days</CardTitle>
        <CardDescription>
          {data ? `${formatCurrency(data.netRevenue)} net across ${formatNumber(data.orders)} orders` : 'Loading…'}
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isPending ? (
          <Skeleton className="h-48 w-full" />
        ) : days.length === 0 ? (
          <p className="grid h-48 place-items-center text-sm text-muted-foreground">No sales in this period.</p>
        ) : (
          <div className="flex h-48 items-end gap-1.5" role="img" aria-label="Daily revenue bar chart">
            {days.map((d) => (
              <div key={d.date} className="group relative flex h-full min-w-0 flex-1 flex-col justify-end">
                <div
                  className="w-full rounded-t bg-primary/80 transition-colors group-hover:bg-primary"
                  style={{ height: `${Math.max(2, (d.revenue / max) * 100)}%` }}
                  title={`${d.date}: ${formatCurrency(d.revenue)}`}
                />
                <span className="mt-1 hidden text-center text-[10px] text-muted-foreground sm:block">{d.date.slice(8)}</span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function TopProducts() {
  const { data, isPending } = useQuery(topProductsQuery(daysAgo(29), daysAgo(0)))
  return (
    <Card>
      <CardHeader>
        <CardTitle>Top products</CardTitle>
        <CardDescription>Last 30 days by units sold</CardDescription>
      </CardHeader>
      <CardContent>
        {isPending ? (
          <Skeleton className="h-40 w-full" />
        ) : !data?.length ? (
          <p className="text-sm text-muted-foreground">No sales yet.</p>
        ) : (
          <ol className="space-y-3">
            {data.map((p, i) => (
              <li key={p.variantId} className="flex items-center gap-3">
                <span className="grid size-6 place-items-center rounded-full bg-muted text-xs font-medium">{i + 1}</span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{p.productName}</p>
                  <p className="truncate font-mono text-xs text-muted-foreground">{p.sku}</p>
                </div>
                <div className="text-right text-sm tabular-nums">
                  <p className="font-medium">{formatNumber(p.unitsSold)}</p>
                  <p className="text-xs text-muted-foreground">{formatCurrency(p.revenue)}</p>
                </div>
              </li>
            ))}
          </ol>
        )}
      </CardContent>
    </Card>
  )
}

function OpenAlerts() {
  const { data, isPending } = useQuery(openAlertsQuery(5))
  return (
    <Card className="mt-6">
      <CardHeader>
        <CardTitle>Open alerts</CardTitle>
        <CardDescription>Stock that needs attention</CardDescription>
      </CardHeader>
      <CardContent>
        {isPending ? (
          <Skeleton className="h-24 w-full" />
        ) : !data?.content.length ? (
          <p className="text-sm text-muted-foreground">All clear, no open alerts.</p>
        ) : (
          <ul className="divide-y">
            {data.content.map((a) => (
              <li key={a.id} className="flex items-center gap-3 py-2.5">
                <Badge variant={a.type === 'OUT_OF_STOCK' ? 'destructive' : 'secondary'}>
                  {a.type === 'OUT_OF_STOCK' ? 'Out of stock' : 'Low stock'}
                </Badge>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{a.productName}</p>
                  <p className="truncate font-mono text-xs text-muted-foreground">
                    {a.sku} · {a.warehouseCode}
                  </p>
                </div>
                <p className="text-sm tabular-nums text-muted-foreground">
                  {a.availableQuantity} / {a.reorderPoint}
                </p>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
```

## `src/routes/_authenticated/inventory/index.tsx`

```tsx
import { useState } from 'react'
import { requires } from '#/lib/guards'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { z } from 'zod'
import { AdjustStockDialog } from '#/components/inventory/adjust-stock-dialog'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAuth } from '#/auth'
import { movementsQuery, stockQuery, warehousesQuery } from '#/lib/queries'
import { formatNumber } from '#/lib/format'
import type { StockLevel } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/inventory/')({
  beforeLoad: requires('inventory:read'),
  validateSearch: z.object({
    view: z.enum(['stock', 'movements']).catch('stock').default('stock'),
    page: z.number().int().min(0).catch(0).default(0),
    warehouseId: z.string().optional(),
  }),
  component: InventoryPage,
})

function InventoryPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { data: warehouses } = useQuery(warehousesQuery)

  return (
    <>
      <PageHeader title="Inventory" description="Stock on hand across warehouses and the movement ledger." />
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Tabs value={search.view} onValueChange={(v) => navigate({ search: (s) => ({ ...s, view: v as 'stock' | 'movements', page: 0 }) })}>
          <TabsList>
            <TabsTrigger value="stock">Stock levels</TabsTrigger>
            <TabsTrigger value="movements">Movements</TabsTrigger>
          </TabsList>
        </Tabs>
        <Select
          value={search.warehouseId ?? 'all'}
          onValueChange={(v) => navigate({ search: (s) => ({ ...s, warehouseId: v === 'all' ? undefined : v, page: 0 }) })}
        >
          <SelectTrigger className="w-full sm:w-56" aria-label="Filter by warehouse">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All warehouses</SelectItem>
            {warehouses?.content.map((w) => (
              <SelectItem key={w.id} value={w.id}>
                {w.code} · {w.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {search.view === 'stock' ? <StockTable /> : <MovementsTable />}
    </>
  )
}

function Loading() {
  return (
    <div className="space-y-2">
      {Array.from({ length: 6 }, (_, i) => (
        <Skeleton key={i} className="h-12 w-full" />
      ))}
    </div>
  )
}

function StockTable() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { can } = useAuth()
  const [adjusting, setAdjusting] = useState<StockLevel | null>(null)
  const { data, isPending, isError } = useQuery(stockQuery({ page: search.page, warehouseId: search.warehouseId }))

  if (isError) return <EmptyState title="Could not load stock" />
  if (isPending) return <Loading />
  if (!data.content.length) return <EmptyState title="No stock records" description="Receive a purchase order or adjust stock to get started." />

  return (
    <>
      <Card className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Product</TableHead>
              <TableHead>SKU</TableHead>
              <TableHead>Warehouse</TableHead>
              <TableHead className="text-right">On hand</TableHead>
              <TableHead className="text-right">Reserved</TableHead>
              <TableHead className="text-right">Available</TableHead>
              {can('inventory:adjust') && <TableHead className="w-24" />}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.map((s) => (
              <TableRow key={s.id}>
                <TableCell className="font-medium">{s.productName}</TableCell>
                <TableCell className="font-mono text-xs">{s.sku}</TableCell>
                <TableCell>
                  {s.warehouseCode}
                  {s.locationCode && <span className="text-muted-foreground"> · {s.locationCode}</span>}
                </TableCell>
                <TableCell className="text-right tabular-nums">{formatNumber(s.quantityOnHand)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatNumber(s.quantityReserved)}</TableCell>
                <TableCell className="text-right tabular-nums">
                  {s.quantityAvailable <= 0 ? <Badge variant="destructive">{s.quantityAvailable}</Badge> : formatNumber(s.quantityAvailable)}
                </TableCell>
                {can('inventory:adjust') && (
                  <TableCell className="text-right">
                    <Button variant="outline" size="sm" onClick={() => setAdjusting(s)}>
                      Adjust
                    </Button>
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>
      <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
      <AdjustStockDialog stock={adjusting} onClose={() => setAdjusting(null)} />
    </>
  )
}

function MovementsTable() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { data, isPending, isError } = useQuery(movementsQuery({ page: search.page, warehouseId: search.warehouseId }))

  if (isError) return <EmptyState title="Could not load movements" />
  if (isPending) return <Loading />
  if (!data.content.length) return <EmptyState title="No movements yet" />

  return (
    <>
      <Card className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>When</TableHead>
              <TableHead>SKU</TableHead>
              <TableHead>Warehouse</TableHead>
              <TableHead>Type</TableHead>
              <TableHead className="text-right">Change</TableHead>
              <TableHead className="text-right">After</TableHead>
              <TableHead>Reason</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.map((m) => (
              <TableRow key={m.id}>
                <TableCell className="whitespace-nowrap text-muted-foreground">{new Date(m.createdAt).toLocaleString()}</TableCell>
                <TableCell className="font-mono text-xs">{m.sku}</TableCell>
                <TableCell>{m.warehouseCode}</TableCell>
                <TableCell>
                  <Badge variant="secondary">{m.type.replaceAll('_', ' ').toLowerCase()}</Badge>
                </TableCell>
                <TableCell className={`text-right tabular-nums ${m.quantityChange < 0 ? 'text-destructive' : 'text-success'}`}>
                  {m.quantityChange > 0 ? '+' : ''}
                  {m.quantityChange}
                </TableCell>
                <TableCell className="text-right tabular-nums">{m.quantityAfter}</TableCell>
                <TableCell className="max-w-64 truncate text-muted-foreground">{m.reason ?? '—'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>
      <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
    </>
  )
}
```

## `src/routes/_authenticated/orders/$orderId.tsx`

```tsx
import { useState } from 'react'
import { requires } from '#/lib/guards'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { StatusBadge } from '#/components/common/status-badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '#/auth'
import { api } from '#/lib/api'
import { orderQuery, orderReturnsQuery } from '#/lib/queries'
import { ReturnDialog } from '#/components/sales/return-dialog'
import { formatCurrency } from '#/lib/format'
import type { Order } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/orders/$orderId')({
  beforeLoad: requires('order:read'),
 component: OrderPage })

function OrderPage() {
  const { orderId } = Route.useParams()
  const { data: order, isPending, isError } = useQuery(orderQuery(orderId))

  return (
    <>
      <Link to="/orders" className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Orders
      </Link>
      {isPending ? (
        <Skeleton className="h-48 w-full" />
      ) : isError ? (
        <EmptyState title="Order not found" />
      ) : (
        <OrderDetail order={order} />
      )}
    </>
  )
}

function OrderDetail({ order }: { order: Order }) {
  const { can } = useAuth()
  const queryClient = useQueryClient()
  const [carrier, setCarrier] = useState('')
  const [tracking, setTracking] = useState('')
  const [returning, setReturning] = useState(false)
  const isShipped = ['SHIPPED', 'DELIVERED'].includes(order.status)
  const { data: returns } = useQuery({ ...orderReturnsQuery(order.id), enabled: isShipped })

  const act = useMutation({
    mutationFn: ({ action, body }: { action: string; body?: unknown }) => api.post<Order>(`/orders/${order.id}/${action}`, body),
    onSuccess: (updated, { action }) => {
      toast.success(`Order ${action === 'pick' ? 'picked' : action === 'pack' ? 'packed' : action === 'ship' ? 'shipped' : action === 'deliver' ? 'delivered' : 'cancelled'}`)
      queryClient.setQueryData(['orders', order.id], updated)
      queryClient.invalidateQueries({ queryKey: ['orders', 'list'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const canUpdate = can('order:update')
  const canCancel = can('order:cancel') && ['PENDING', 'PICKING', 'PACKED'].includes(order.status)
  const run = (action: string, body?: unknown) => act.mutate({ action, body })

  return (
    <>
      <PageHeader
        title={order.orderNumber}
        description={`${order.customerName ?? 'Walk-in customer'} · ${order.warehouseCode} · ${new Date(order.createdAt).toLocaleString()}`}
        actions={<StatusBadge status={order.status} />}
      />

      {(canUpdate || canCancel) && !['DELIVERED', 'CANCELLED'].includes(order.status) && (
        <Card className="mb-6">
          <CardContent className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end">
            {canUpdate && order.status === 'PENDING' && <Button disabled={act.isPending} onClick={() => run('pick')}>Pick all lines</Button>}
            {canUpdate && order.status === 'PICKING' && <Button disabled={act.isPending} onClick={() => run('pack')}>Mark packed</Button>}
            {canUpdate && order.status === 'PACKED' && (
              <>
                <Input value={carrier} onChange={(e) => setCarrier(e.target.value)} placeholder="Carrier" aria-label="Carrier" className="sm:w-44" />
                <Input value={tracking} onChange={(e) => setTracking(e.target.value)} placeholder="Tracking number" aria-label="Tracking number" className="sm:w-56" />
                <Button disabled={act.isPending} onClick={() => run('ship', { carrier: carrier || undefined, trackingNumber: tracking || undefined })}>
                  Ship
                </Button>
              </>
            )}
            {canUpdate && order.status === 'SHIPPED' && <Button disabled={act.isPending} onClick={() => run('deliver')}>Mark delivered</Button>}
            {canCancel && (
              <Button variant="outline" className="text-destructive sm:ml-auto" disabled={act.isPending} onClick={() => confirm('Cancel this order?') && run('cancel')}>
                Cancel order
              </Button>
            )}
            {act.isPending && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
          </CardContent>
        </Card>
      )}

      {isShipped && canUpdate && (
        <div className="mb-6">
          <Button variant="outline" onClick={() => setReturning(true)}>
            Record a return
          </Button>
          <ReturnDialog order={order} open={returning} onOpenChange={setReturning} />
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Items</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Product</TableHead>
                <TableHead className="text-right">Qty</TableHead>
                <TableHead className="text-right">Picked</TableHead>
                <TableHead className="text-right">Unit price</TableHead>
                <TableHead className="text-right">Total</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {order.lines.map((l) => (
                <TableRow key={l.id}>
                  <TableCell>
                    <p className="font-medium">{l.productName}</p>
                    <p className="font-mono text-xs text-muted-foreground">{l.sku}</p>
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{l.quantity}</TableCell>
                  <TableCell className="text-right tabular-nums">{l.quantityPicked}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatCurrency(l.unitPrice)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatCurrency(l.lineTotal)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <dl className="ml-auto mt-4 w-full max-w-xs space-y-1 text-sm">
            <div className="flex justify-between"><dt className="text-muted-foreground">Subtotal</dt><dd className="tabular-nums">{formatCurrency(order.subtotal)}</dd></div>
            <div className="flex justify-between"><dt className="text-muted-foreground">Tax</dt><dd className="tabular-nums">{formatCurrency(order.taxAmount)}</dd></div>
            <div className="flex justify-between border-t pt-1 font-semibold"><dt>Total</dt><dd className="tabular-nums">{formatCurrency(order.total)}</dd></div>
          </dl>
          {(order.trackingNumber || order.carrier) && (
            <p className="mt-4 text-sm text-muted-foreground">
              Shipped via {order.carrier ?? 'carrier'}
              {order.trackingNumber && (
                <>
                  {' '}· tracking <span className="font-mono">{order.trackingNumber}</span>
                </>
              )}
            </p>
          )}
        </CardContent>
      </Card>
      {returns && returns.length > 0 && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle>Returns</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="divide-y text-sm">
              {returns.map((r) => (
                <li key={r.id} className="flex flex-wrap items-center justify-between gap-2 py-2.5">
                  <div>
                    <p className="font-mono text-xs font-medium">{r.returnNumber}</p>
                    <p className="text-muted-foreground">
                      {r.lines.map((l) => `${l.quantity} × ${l.sku}`).join(', ')} · {r.restocked ? 'restocked' : 'written off'}
                      {r.reason ? ` · ${r.reason}` : ''}
                    </p>
                  </div>
                  <span className="tabular-nums">Refund {formatCurrency(r.refundAmount)}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/orders/index.tsx`

```tsx
import { requires } from '#/lib/guards'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { useAuth } from '#/auth'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { StatusBadge } from '#/components/common/status-badge'
import { Card } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ordersQuery } from '#/lib/queries'
import { formatCurrency } from '#/lib/format'

const STATUSES = ['PENDING', 'PICKING', 'PACKED', 'SHIPPED', 'DELIVERED', 'CANCELLED']

export const Route = createFileRoute('/_authenticated/orders/')({
  beforeLoad: requires('order:read'),
  validateSearch: z.object({
    page: z.number().int().min(0).catch(0).default(0),
    status: z.string().optional(),
  }),
  component: OrdersPage,
})

function OrdersPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { can } = useAuth()
  const { data, isPending, isError } = useQuery(ordersQuery(search))

  return (
    <>
      <PageHeader
        title="Orders"
        description="Sales orders and their fulfilment progress."
        actions={
          can('order:create') && (
            <Button asChild>
              <Link to="/orders/new">
                <Plus /> New order
              </Link>
            </Button>
          )
        }
      />
      <div className="mb-4">
        <Select value={search.status ?? 'all'} onValueChange={(v) => navigate({ search: { status: v === 'all' ? undefined : v, page: 0 } })}>
          <SelectTrigger className="w-full sm:w-56" aria-label="Filter by status">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {s.toLowerCase()}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {isError ? (
        <EmptyState title="Could not load orders" />
      ) : isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : !data.content.length ? (
        <EmptyState title="No orders found" />
      ) : (
        <>
          <Card className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Order</TableHead>
                  <TableHead>Customer</TableHead>
                  <TableHead>Warehouse</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                  <TableHead>Placed</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((o) => (
                  <TableRow key={o.id}>
                    <TableCell className="font-mono text-xs font-medium">
                      <Link to="/orders/$orderId" params={{ orderId: o.id }} className="hover:underline">
                        {o.orderNumber}
                      </Link>
                    </TableCell>
                    <TableCell>{o.customerName ?? 'Walk-in'}</TableCell>
                    <TableCell>{o.warehouseCode}</TableCell>
                    <TableCell>
                      <StatusBadge status={o.status} />
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{formatCurrency(o.total)}</TableCell>
                    <TableCell className="whitespace-nowrap text-muted-foreground">{new Date(o.createdAt).toLocaleDateString()}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
          <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
        </>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/orders/new.tsx`

```tsx
import { useState } from 'react'
import { createFileRoute, Link, redirect, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Loader2, UserPlus } from 'lucide-react'
import { toast } from 'sonner'
import { FormDialog, optional } from '#/components/common/form-dialog'
import { addLine, CartLines, type CartLine } from '#/components/common/cart-lines'
import { PageHeader } from '#/components/common/page-header'
import { VariantPicker } from '#/components/common/variant-picker'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { api } from '#/lib/api'
import { customersQuery, warehousesQuery } from '#/lib/queries'
import { formatCurrency } from '#/lib/format'
import type { Order } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/orders/new')({
  beforeLoad: ({ context }) => {
    if (!context.auth.can('order:create')) throw redirect({ to: '/orders' })
  },
  component: NewOrderPage,
})

interface OrderLine extends CartLine {
  unitPrice: number
}

const CHANNELS = [
  { value: 'IN_STORE', label: 'In store' },
  { value: 'ONLINE', label: 'Online' },
  { value: 'PHONE', label: 'Phone' },
]

function NewOrderPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: warehouses, isError: warehousesFailed } = useQuery(warehousesQuery)
  const { data: customers } = useQuery(customersQuery)
  const [warehouseId, setWarehouseId] = useState('')
  const [customerId, setCustomerId] = useState('walk-in')
  const [channel, setChannel] = useState('IN_STORE')
  const [notes, setNotes] = useState('')
  const [lines, setLines] = useState<OrderLine[]>([])
  const [newCustomer, setNewCustomer] = useState(false)

  const subtotal = lines.reduce((sum, l) => sum + l.unitPrice * l.quantity, 0)

  const create = useMutation({
    mutationFn: () =>
      api.post<Order>('/orders', {
        customerId: customerId === 'walk-in' ? undefined : customerId,
        warehouseId,
        channel,
        notes: optional(notes),
        lines: lines.map((l) => ({ variantId: l.variantId, quantity: l.quantity })),
      }),
    onSuccess: (order) => {
      toast.success(`Order ${order.orderNumber} created`)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      navigate({ to: '/orders/$orderId', params: { orderId: order.id } })
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const ready = !!warehouseId && lines.length > 0

  return (
    <>
      <Link to="/orders" className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Orders
      </Link>
      <PageHeader title="New order" description="Stock is reserved in the chosen warehouse when the order is placed." />

      <div className="grid gap-6 xl:grid-cols-5">
        <div className="space-y-6 xl:col-span-3">
          <Card>
            <CardHeader>
              <CardTitle>Add products</CardTitle>
            </CardHeader>
            <CardContent>
              <VariantPicker onAdd={(v) => setLines((ls) => addLine(ls, { variantId: v.variantId, sku: v.sku, productName: v.productName, unitPrice: v.retailPrice, quantity: 1 }))} />
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6 xl:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="warehouse">Fulfilment warehouse</Label>
                <Select value={warehouseId} onValueChange={setWarehouseId}>
                  <SelectTrigger id="warehouse" className="w-full">
                    <SelectValue placeholder="Select warehouse" />
                  </SelectTrigger>
                  <SelectContent>
                    {warehouses?.content
                      .filter((w) => w.isActive)
                      .map((w) => (
                        <SelectItem key={w.id} value={w.id}>
                          {w.code} · {w.name}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
              </div>
              {warehousesFailed && (
                <p role="alert" className="-mt-2 text-xs text-destructive">
                  Your role cannot list warehouses (needs warehouse:read), so an order cannot be placed from this account.
                </p>
              )}
              <div className="space-y-2">
                <Label htmlFor="customer">Customer</Label>
                <div className="flex gap-2">
                  <Select value={customerId} onValueChange={setCustomerId}>
                    <SelectTrigger id="customer" className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="walk-in">Walk-in customer</SelectItem>
                      {customers?.content.map((c) => (
                        <SelectItem key={c.id} value={c.id}>
                          {c.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button type="button" variant="outline" size="icon" aria-label="New customer" onClick={() => setNewCustomer(true)}>
                    <UserPlus />
                  </Button>
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="channel">Channel</Label>
                <Select value={channel} onValueChange={setChannel}>
                  <SelectTrigger id="channel" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {CHANNELS.map((c) => (
                      <SelectItem key={c.value} value={c.value}>
                        {c.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="notes">Notes</Label>
                <Input id="notes" value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={1000} />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Order lines</CardTitle>
            </CardHeader>
            <CardContent>
              <CartLines lines={lines} onChange={setLines} extra={(l) => <span className="text-sm tabular-nums text-muted-foreground">{formatCurrency(l.unitPrice)}</span>} />
              <div className="mt-4 flex items-center justify-between border-t pt-4">
                <span className="text-sm text-muted-foreground">Subtotal (before tax)</span>
                <span className="font-semibold tabular-nums">{formatCurrency(subtotal)}</span>
              </div>
              <Button className="mt-4 w-full" disabled={!ready || create.isPending} onClick={() => create.mutate()}>
                {create.isPending && <Loader2 className="animate-spin" />}
                Place order
              </Button>
              {!warehouseId && lines.length > 0 && <p className="mt-2 text-xs text-muted-foreground">Choose a warehouse to continue.</p>}
            </CardContent>
          </Card>
        </div>
      </div>

      <FormDialog
        open={newCustomer}
        onOpenChange={setNewCustomer}
        title="New customer"
        fields={[
          { name: 'name', label: 'Name', required: true },
          { name: 'email', label: 'Email', type: 'email' },
          { name: 'phone', label: 'Phone' },
        ]}
        submitLabel="Create customer"
        onSubmit={async (v) => {
          const created = await api.post<{ id: string }>('/customers', { name: v.name, email: optional(v.email), phone: optional(v.phone) })
          await queryClient.invalidateQueries({ queryKey: ['customers'] })
          setCustomerId(created.id)
          toast.success('Customer created')
        }}
      />
    </>
  )
}
```

## `src/routes/_authenticated/products/$productId.tsx`

```tsx
import { useState } from 'react'
import { requires } from '#/lib/guards'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Pencil, Plus } from 'lucide-react'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { ProductDialog, VariantDialog } from '#/components/catalog/product-dialogs'
import { useAuth } from '#/auth'
import { Button } from '@/components/ui/button'
import type { Variant } from '#/lib/types'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { productQuery } from '#/lib/queries'
import { formatCurrency } from '#/lib/format'

export const Route = createFileRoute('/_authenticated/products/$productId')({
  beforeLoad: requires('product:read'),
 component: ProductPage })

function ProductPage() {
  const { productId } = Route.useParams()
  const { data: product, isPending, isError } = useQuery(productQuery(productId))
  const { can } = useAuth()
  const [editingProduct, setEditingProduct] = useState(false)
  const [variantDialog, setVariantDialog] = useState<{ open: boolean; variant?: Variant }>({ open: false })
  const canUpdate = can('product:update')

  return (
    <>
      <Link to="/products" className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Products
      </Link>
      {isPending ? (
        <Skeleton className="h-40 w-full" />
      ) : isError ? (
        <EmptyState title="Product not found" />
      ) : (
        <>
          <PageHeader
            title={product.name}
            description={[product.categoryName, product.brand].filter(Boolean).join(' · ')}
            actions={
              <>
                <Badge variant={product.isActive ? 'default' : 'secondary'}>{product.isActive ? 'Active' : 'Inactive'}</Badge>
                {canUpdate && (
                  <Button variant="outline" size="sm" onClick={() => setEditingProduct(true)}>
                    <Pencil /> Edit
                  </Button>
                )}
              </>
            }
          />
          {product.description && <p className="mb-6 max-w-prose text-sm text-muted-foreground">{product.description}</p>}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Variants</CardTitle>
              {canUpdate && (
                <Button size="sm" onClick={() => setVariantDialog({ open: true })}>
                  <Plus /> Add variant
                </Button>
              )}
            </CardHeader>
            <CardContent className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>SKU</TableHead>
                    <TableHead>Attributes</TableHead>
                    <TableHead>Barcodes</TableHead>
                    <TableHead className="text-right">Cost</TableHead>
                    <TableHead className="text-right">Retail</TableHead>
                    {canUpdate && <TableHead className="w-16" />}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {product.variants.map((v) => (
                    <TableRow key={v.id} className={v.isActive ? '' : 'opacity-60'}>
                      <TableCell className="font-mono text-xs">{v.sku}</TableCell>
                      <TableCell>
                        {Object.entries(v.variantAttributes ?? {})
                          .map(([k, val]) => `${k}: ${String(val)}`)
                          .join(', ') || '—'}
                      </TableCell>
                      <TableCell className="font-mono text-xs">{v.barcodes.map((b) => b.barcode).join(', ') || '—'}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatCurrency(v.costPrice)}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatCurrency(v.retailPrice)}</TableCell>
                      {canUpdate && (
                        <TableCell className="text-right">
                          <Button variant="ghost" size="icon" aria-label={`Edit ${v.sku}`} onClick={() => setVariantDialog({ open: true, variant: v })}>
                            <Pencil />
                          </Button>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
          <ProductDialog open={editingProduct} onOpenChange={setEditingProduct} product={product} />
          <VariantDialog
            open={variantDialog.open}
            onOpenChange={(o) => setVariantDialog((d) => ({ ...d, open: o }))}
            productId={product.id}
            variant={variantDialog.variant}
          />
        </>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/products/index.tsx`

```tsx
import { useEffect, useState } from 'react'
import { requires } from '#/lib/guards'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Plus, Search } from 'lucide-react'
import { z } from 'zod'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { ProductDialog } from '#/components/catalog/product-dialogs'
import { useAuth } from '#/auth'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { categoriesQuery, productsQuery } from '#/lib/queries'
import { formatCurrency } from '#/lib/format'
import type { Category, Product } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/products/')({
  beforeLoad: requires('product:read'),
  validateSearch: z.object({
    page: z.number().int().min(0).catch(0).default(0),
    keyword: z.string().optional(),
    categoryId: z.string().optional(),
  }),
  component: ProductsPage,
})

const flatten = (cats: Category[]): Category[] => cats.flatMap((c) => [c, ...flatten(c.subcategories ?? [])])

function priceRange(p: Product) {
  const prices = p.variants.filter((v) => v.isActive).map((v) => v.retailPrice)
  if (!prices.length) return '—'
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  return min === max ? formatCurrency(min) : `${formatCurrency(min)} – ${formatCurrency(max)}`
}

function ProductsPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [keyword, setKeyword] = useState(search.keyword ?? '')
  const { can } = useAuth()
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    const t = setTimeout(() => {
      if ((search.keyword ?? '') !== keyword) {
        navigate({ search: (s) => ({ ...s, keyword: keyword || undefined, page: 0 }) })
      }
    }, 300)
    return () => clearTimeout(t)
  }, [keyword, navigate, search.keyword])

  const { data, isPending, isError } = useQuery(productsQuery(search))
  const { data: categories } = useQuery(categoriesQuery)
  const options = flatten(categories ?? [])

  return (
    <>
      <PageHeader
        title="Products"
        description="Browse the catalogue, variants and prices."
        actions={
          can('product:create') && (
            <Button onClick={() => setCreating(true)}>
              <Plus /> New product
            </Button>
          )
        }
      />
      <ProductDialog open={creating} onOpenChange={setCreating} />
      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Search products…"
            className="pl-9"
            aria-label="Search products"
          />
        </div>
        <Select
          value={search.categoryId ?? 'all'}
          onValueChange={(v) => navigate({ search: (s) => ({ ...s, categoryId: v === 'all' ? undefined : v, page: 0 }) })}
        >
          <SelectTrigger className="w-full sm:w-56" aria-label="Filter by category">
            <SelectValue placeholder="All categories" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All categories</SelectItem>
            {options.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isError ? (
        <EmptyState title="Could not load products" description="Check your connection and permissions, then try again." />
      ) : isPending ? (
        <div className="space-y-2">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : data.content.length === 0 ? (
        <EmptyState title="No products found" description="Try a different search or category." />
      ) : (
        <>
          {/* Cards on small screens */}
          <ul className="space-y-3 md:hidden">
            {data.content.map((p) => (
              <li key={p.id}>
                <Link to="/products/$productId" params={{ productId: p.id }}>
                  <Card className="transition-colors hover:bg-accent/40">
                    <CardContent className="space-y-1">
                      <div className="flex items-start justify-between gap-2">
                        <p className="font-medium">{p.name}</p>
                        {!p.isActive && <Badge variant="secondary">Inactive</Badge>}
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {p.categoryName}
                        {p.brand ? ` · ${p.brand}` : ''}
                      </p>
                      <p className="text-sm tabular-nums">
                        {priceRange(p)} · {p.variants.length} variant{p.variants.length === 1 ? '' : 's'}
                      </p>
                    </CardContent>
                  </Card>
                </Link>
              </li>
            ))}
          </ul>
          {/* Table from md up */}
          <Card className="hidden md:block">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Product</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Brand</TableHead>
                  <TableHead className="text-right">Variants</TableHead>
                  <TableHead className="text-right">Price</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell className="font-medium">
                      <Link to="/products/$productId" params={{ productId: p.id }} className="hover:underline">
                        {p.name}
                      </Link>
                    </TableCell>
                    <TableCell>{p.categoryName}</TableCell>
                    <TableCell>{p.brand ?? '—'}</TableCell>
                    <TableCell className="text-right tabular-nums">{p.variants.length}</TableCell>
                    <TableCell className="text-right tabular-nums">{priceRange(p)}</TableCell>
                    <TableCell>
                      <Badge variant={p.isActive ? 'default' : 'secondary'}>{p.isActive ? 'Active' : 'Inactive'}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
          <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
        </>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/purchase-orders/$poId.tsx`

```tsx
import { useState } from 'react'
import { requires } from '#/lib/guards'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { StatusBadge } from '#/components/common/status-badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '#/auth'
import { api } from '#/lib/api'
import { purchaseOrderQuery } from '#/lib/queries'
import { formatCurrency } from '#/lib/format'
import type { PurchaseOrder } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/purchase-orders/$poId')({
  beforeLoad: requires('purchase:read'),
 component: PurchaseOrderPage })

function PurchaseOrderPage() {
  const { poId } = Route.useParams()
  const { data: po, isPending, isError } = useQuery(purchaseOrderQuery(poId))
  return (
    <>
      <Link to="/purchase-orders" className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Purchase orders
      </Link>
      {isPending ? <Skeleton className="h-48 w-full" /> : isError ? <EmptyState title="Purchase order not found" /> : <Detail po={po} />}
    </>
  )
}

function Detail({ po }: { po: PurchaseOrder }) {
  const { can } = useAuth()
  const queryClient = useQueryClient()
  const [quantities, setQuantities] = useState<Record<string, string>>({})

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['purchase-orders'] })
    queryClient.invalidateQueries({ queryKey: ['inventory'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }
  const onError = (e: Error) => toast.error(e.message)

  const act = useMutation({
    mutationFn: (action: 'submit' | 'cancel' | 'close') => api.post(`/purchase-orders/${po.id}/${action}`),
    onSuccess: (_d, action) => {
      toast.success(`Purchase order ${action === 'submit' ? 'submitted' : action === 'cancel' ? 'cancelled' : 'closed'}`)
      refresh()
    },
    onError,
  })

  const receive = useMutation({
    mutationFn: (lines: { lineId: string; quantity: number }[]) => api.post(`/purchase-orders/${po.id}/receive`, { lines }),
    onSuccess: () => {
      toast.success('Goods received')
      setQuantities({})
      refresh()
    },
    onError,
  })

  const receivable = ['ORDERED', 'PARTIALLY_RECEIVED'].includes(po.status) && can('purchase:receive')
  const pending = act.isPending || receive.isPending

  function submitReceipt() {
    const lines = po.lines
      .map((l) => ({ lineId: l.id, quantity: Number(quantities[l.id] ?? 0), max: l.quantityOutstanding }))
      .filter((l) => l.quantity > 0)
    if (!lines.length) return toast.error('Enter a quantity to receive.')
    if (lines.some((l) => !Number.isInteger(l.quantity) || l.quantity > l.max)) return toast.error('Quantities must be whole numbers within the outstanding amount.')
    receive.mutate(lines.map(({ lineId, quantity }) => ({ lineId, quantity })))
  }

  return (
    <>
      <PageHeader
        title={po.poNumber}
        description={`${po.supplierName} → ${po.warehouseCode}${po.expectedDate ? ` · expected ${po.expectedDate}` : ''}`}
        actions={<StatusBadge status={po.status} />}
      />
      <div className="mb-6 flex flex-wrap gap-2">
        {po.status === 'DRAFT' && can('purchase:update') && <Button disabled={pending} onClick={() => act.mutate('submit')}>Submit to supplier</Button>}
        {receivable && <Button disabled={pending} onClick={submitReceipt}>Receive entered quantities</Button>}
        {po.status === 'PARTIALLY_RECEIVED' && can('purchase:update') && (
          <Button variant="outline" disabled={pending} onClick={() => confirm('Close with outstanding quantities?') && act.mutate('close')}>Close</Button>
        )}
        {['DRAFT', 'ORDERED'].includes(po.status) && can('purchase:cancel') && (
          <Button variant="outline" className="text-destructive" disabled={pending} onClick={() => confirm('Cancel this purchase order?') && act.mutate('cancel')}>Cancel</Button>
        )}
        {pending && <Loader2 className="size-4 animate-spin self-center text-muted-foreground" />}
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Lines</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Product</TableHead>
                <TableHead className="text-right">Ordered</TableHead>
                <TableHead className="text-right">Received</TableHead>
                <TableHead className="text-right">Unit cost</TableHead>
                <TableHead className="text-right">Total</TableHead>
                {receivable && <TableHead className="w-28 text-right">Receive now</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {po.lines.map((l) => (
                <TableRow key={l.id}>
                  <TableCell>
                    <p className="font-medium">{l.productName}</p>
                    <p className="font-mono text-xs text-muted-foreground">{l.sku}</p>
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{l.quantityOrdered}</TableCell>
                  <TableCell className="text-right tabular-nums">{l.quantityReceived}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatCurrency(l.unitCost)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatCurrency(l.lineTotal)}</TableCell>
                  {receivable && (
                    <TableCell className="text-right">
                      <Input
                        type="number"
                        min={0}
                        max={l.quantityOutstanding}
                        disabled={l.quantityOutstanding === 0}
                        value={quantities[l.id] ?? ''}
                        onChange={(e) => setQuantities((q) => ({ ...q, [l.id]: e.target.value }))}
                        aria-label={`Quantity to receive for ${l.sku}`}
                        className="ml-auto w-24 text-right"
                      />
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <p className="mt-4 text-right text-sm font-semibold tabular-nums">Total {formatCurrency(po.totalAmount)}</p>
        </CardContent>
      </Card>
    </>
  )
}
```

## `src/routes/_authenticated/purchase-orders/index.tsx`

```tsx
import { requires } from '#/lib/guards'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { useAuth } from '#/auth'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { StatusBadge } from '#/components/common/status-badge'
import { Card } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { purchaseOrdersQuery } from '#/lib/queries'
import { formatCurrency } from '#/lib/format'

const STATUSES = ['DRAFT', 'ORDERED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED', 'CLOSED']

export const Route = createFileRoute('/_authenticated/purchase-orders/')({
  beforeLoad: requires('purchase:read'),
  validateSearch: z.object({
    page: z.number().int().min(0).catch(0).default(0),
    status: z.string().optional(),
  }),
  component: PurchaseOrdersPage,
})

function PurchaseOrdersPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { can } = useAuth()
  const { data, isPending, isError } = useQuery(purchaseOrdersQuery(search))

  return (
    <>
      <PageHeader
        title="Purchase orders"
        description="Stock on order from suppliers."
        actions={
          can('purchase:create') && (
            <Button asChild>
              <Link to="/purchase-orders/new">
                <Plus /> New purchase order
              </Link>
            </Button>
          )
        }
      />
      <div className="mb-4">
        <Select value={search.status ?? 'all'} onValueChange={(v) => navigate({ search: { status: v === 'all' ? undefined : v, page: 0 } })}>
          <SelectTrigger className="w-full sm:w-56" aria-label="Filter by status">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {s.replaceAll('_', ' ').toLowerCase()}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {isError ? (
        <EmptyState title="Could not load purchase orders" />
      ) : isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : !data.content.length ? (
        <EmptyState title="No purchase orders found" />
      ) : (
        <>
          <Card className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>PO</TableHead>
                  <TableHead>Supplier</TableHead>
                  <TableHead>Warehouse</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Expected</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((po) => (
                  <TableRow key={po.id}>
                    <TableCell className="font-mono text-xs font-medium">
                      <Link to="/purchase-orders/$poId" params={{ poId: po.id }} className="hover:underline">
                        {po.poNumber}
                      </Link>
                    </TableCell>
                    <TableCell>{po.supplierName}</TableCell>
                    <TableCell>{po.warehouseCode}</TableCell>
                    <TableCell>
                      <StatusBadge status={po.status} />
                    </TableCell>
                    <TableCell className="whitespace-nowrap text-muted-foreground">{po.expectedDate ?? '—'}</TableCell>
                    <TableCell className="text-right tabular-nums">{formatCurrency(po.totalAmount)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
          <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
        </>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/purchase-orders/new.tsx`

```tsx
import { useState } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { addLine, CartLines, type CartLine } from '#/components/common/cart-lines'
import { PageHeader } from '#/components/common/page-header'
import { VariantPicker } from '#/components/common/variant-picker'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { api } from '#/lib/api'
import { requires } from '#/lib/guards'
import { supplierOptionsQuery, warehousesQuery } from '#/lib/queries'
import { formatCurrency } from '#/lib/format'
import type { PurchaseOrder } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/purchase-orders/new')({
  beforeLoad: requires('purchase:create'),
  component: NewPurchaseOrderPage,
})

interface PoDraftLine extends CartLine {
  unitCost: string
}

function NewPurchaseOrderPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: suppliers } = useQuery(supplierOptionsQuery)
  const { data: warehouses } = useQuery(warehousesQuery)
  const [supplierId, setSupplierId] = useState('')
  const [warehouseId, setWarehouseId] = useState('')
  const [expectedDate, setExpectedDate] = useState('')
  const [notes, setNotes] = useState('')
  const [lines, setLines] = useState<PoDraftLine[]>([])

  const total = lines.reduce((sum, l) => sum + (Number(l.unitCost) || 0) * l.quantity, 0)
  const ready = !!supplierId && !!warehouseId && lines.length > 0 && lines.every((l) => Number(l.unitCost) > 0)

  const create = useMutation({
    mutationFn: () =>
      api.post<PurchaseOrder>('/purchase-orders', {
        supplierId,
        warehouseId,
        expectedDate: expectedDate || undefined,
        notes: notes.trim() || undefined,
        lines: lines.map((l) => ({ variantId: l.variantId, quantity: l.quantity, unitCost: Number(l.unitCost) })),
      }),
    onSuccess: (po) => {
      toast.success(`${po.poNumber} saved as draft`)
      queryClient.invalidateQueries({ queryKey: ['purchase-orders'] })
      navigate({ to: '/purchase-orders/$poId', params: { poId: po.id } })
    },
    onError: (e: Error) => toast.error(e.message),
  })

  return (
    <>
      <Link to="/purchase-orders" className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Purchase orders
      </Link>
      <PageHeader title="New purchase order" description="Saved as a draft. Submit it to the supplier from its detail page." />

      <div className="grid gap-6 xl:grid-cols-5">
        <Card className="xl:col-span-3">
          <CardHeader>
            <CardTitle>Add products</CardTitle>
          </CardHeader>
          <CardContent>
            <VariantPicker
              price="cost"
              onAdd={(v) => setLines((ls) => addLine(ls, { variantId: v.variantId, sku: v.sku, productName: v.productName, quantity: 1, unitCost: String(v.costPrice) }))}
            />
          </CardContent>
        </Card>

        <div className="space-y-6 xl:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="supplier">Supplier</Label>
                <Select value={supplierId} onValueChange={setSupplierId}>
                  <SelectTrigger id="supplier" className="w-full">
                    <SelectValue placeholder="Select supplier" />
                  </SelectTrigger>
                  <SelectContent>
                    {suppliers?.content.map((s) => (
                      <SelectItem key={s.id} value={s.id}>
                        {s.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="warehouse">Deliver to</Label>
                <Select value={warehouseId} onValueChange={setWarehouseId}>
                  <SelectTrigger id="warehouse" className="w-full">
                    <SelectValue placeholder="Select warehouse" />
                  </SelectTrigger>
                  <SelectContent>
                    {warehouses?.content
                      .filter((w) => w.isActive)
                      .map((w) => (
                        <SelectItem key={w.id} value={w.id}>
                          {w.code} · {w.name}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="expected">Expected date</Label>
                <Input id="expected" type="date" value={expectedDate} onChange={(e) => setExpectedDate(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="notes">Notes</Label>
                <Input id="notes" value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={1000} />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Lines</CardTitle>
            </CardHeader>
            <CardContent>
              <CartLines
                lines={lines}
                onChange={setLines}
                extra={(l, update) => (
                  <Input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={l.unitCost}
                    onChange={(e) => update({ unitCost: e.target.value })}
                    aria-label={`Unit cost for ${l.sku}`}
                    className="h-8 w-20 text-right"
                  />
                )}
              />
              <div className="mt-4 flex items-center justify-between border-t pt-4">
                <span className="text-sm text-muted-foreground">Total</span>
                <span className="font-semibold tabular-nums">{formatCurrency(total)}</span>
              </div>
              <Button className="mt-4 w-full" disabled={!ready || create.isPending} onClick={() => create.mutate()}>
                {create.isPending && <Loader2 className="animate-spin" />}
                Save draft
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </>
  )
}
```

## `src/routes/_authenticated/suppliers/index.tsx`

```tsx
import { useEffect, useState } from 'react'
import { requires } from '#/lib/guards'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Pencil, Plus, Search } from 'lucide-react'
import { z } from 'zod'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { SupplierDialog } from '#/components/procurement/supplier-dialog'
import { useAuth } from '#/auth'
import type { Supplier } from '#/lib/types'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { suppliersQuery } from '#/lib/queries'

export const Route = createFileRoute('/_authenticated/suppliers/')({
  beforeLoad: requires('supplier:read'),
  validateSearch: z.object({
    page: z.number().int().min(0).catch(0).default(0),
    keyword: z.string().optional(),
  }),
  component: SuppliersPage,
})

function SuppliersPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [keyword, setKeyword] = useState(search.keyword ?? '')
  const { can } = useAuth()
  const [dialog, setDialog] = useState<{ open: boolean; supplier?: Supplier }>({ open: false })

  useEffect(() => {
    const t = setTimeout(() => {
      if ((search.keyword ?? '') !== keyword) navigate({ search: { keyword: keyword || undefined, page: 0 } })
    }, 300)
    return () => clearTimeout(t)
  }, [keyword, navigate, search.keyword])

  const { data, isPending, isError } = useQuery(suppliersQuery(search))

  return (
    <>
      <PageHeader
        title="Suppliers"
        description="Vendors you purchase stock from."
        actions={
          can('supplier:create') && (
            <Button onClick={() => setDialog({ open: true })}>
              <Plus /> New supplier
            </Button>
          )
        }
      />
      <SupplierDialog open={dialog.open} onOpenChange={(o) => setDialog((d) => ({ ...d, open: o }))} supplier={dialog.supplier} />
      <div className="relative mb-4 max-w-md">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Search suppliers…" className="pl-9" aria-label="Search suppliers" />
      </div>
      {isError ? (
        <EmptyState title="Could not load suppliers" />
      ) : isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : !data.content.length ? (
        <EmptyState title="No suppliers found" />
      ) : (
        <>
          <Card className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Supplier</TableHead>
                  <TableHead>Contact</TableHead>
                  <TableHead>Location</TableHead>
                  <TableHead>Terms</TableHead>
                  <TableHead className="text-right">Lead time</TableHead>
                  <TableHead>Status</TableHead>
                  {can('supplier:update') && <TableHead className="w-16" />}
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((s) => (
                  <TableRow key={s.id}>
                    <TableCell>
                      <p className="font-medium">{s.name}</p>
                      <p className="font-mono text-xs text-muted-foreground">{s.code}</p>
                    </TableCell>
                    <TableCell>
                      <p>{s.contactName ?? '—'}</p>
                      <p className="text-xs text-muted-foreground">{s.email}</p>
                    </TableCell>
                    <TableCell>{[s.city, s.country].filter(Boolean).join(', ') || '—'}</TableCell>
                    <TableCell>{s.paymentTerms ?? '—'}</TableCell>
                    <TableCell className="text-right tabular-nums">{s.leadTimeDays != null ? `${s.leadTimeDays} d` : '—'}</TableCell>
                    <TableCell>
                      <Badge variant={s.isActive ? 'default' : 'secondary'}>{s.isActive ? 'Active' : 'Inactive'}</Badge>
                    </TableCell>
                    {can('supplier:update') && (
                      <TableCell className="text-right">
                        <Button variant="ghost" size="icon" aria-label={`Edit ${s.name}`} onClick={() => setDialog({ open: true, supplier: s })}>
                          <Pencil />
                        </Button>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
          <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
        </>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/transfers/$transferId.tsx`

```tsx
import { createFileRoute, Link } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { StatusBadge } from '#/components/common/status-badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '#/auth'
import { api } from '#/lib/api'
import { requires } from '#/lib/guards'
import { transferQuery } from '#/lib/queries'
import type { Transfer } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/transfers/$transferId')({
  beforeLoad: requires('transfer:read'),
  component: TransferPage,
})

function TransferPage() {
  const { transferId } = Route.useParams()
  const { data, isPending, isError } = useQuery(transferQuery(transferId))
  return (
    <>
      <Link to="/transfers" className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Transfers
      </Link>
      {isPending ? <Skeleton className="h-48 w-full" /> : isError ? <EmptyState title="Transfer not found" /> : <Detail transfer={data} />}
    </>
  )
}

function Detail({ transfer }: { transfer: Transfer }) {
  const { can } = useAuth()
  const queryClient = useQueryClient()

  const act = useMutation({
    mutationFn: (action: 'dispatch' | 'receive' | 'cancel') => api.post<Transfer>(`/transfers/${transfer.id}/${action}`, action === 'receive' ? {} : undefined),
    onSuccess: (updated, action) => {
      toast.success(action === 'dispatch' ? 'Transfer dispatched' : action === 'receive' ? 'Transfer received' : 'Transfer cancelled')
      queryClient.setQueryData(['transfers', transfer.id], updated)
      queryClient.invalidateQueries({ queryKey: ['transfers', 'list'] })
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const canUpdate = can('transfer:update')

  return (
    <>
      <PageHeader
        title={transfer.referenceNumber}
        description={`${transfer.fromWarehouseCode} → ${transfer.toWarehouseCode} · ${new Date(transfer.createdAt).toLocaleString()}`}
        actions={<StatusBadge status={transfer.status} />}
      />
      {canUpdate && ['REQUESTED', 'IN_TRANSIT'].includes(transfer.status) && (
        <div className="mb-6 flex flex-wrap items-center gap-2">
          {transfer.status === 'REQUESTED' && <Button disabled={act.isPending} onClick={() => act.mutate('dispatch')}>Dispatch</Button>}
          {transfer.status === 'IN_TRANSIT' && <Button disabled={act.isPending} onClick={() => act.mutate('receive')}>Receive in full</Button>}
          {transfer.status === 'REQUESTED' && (
            <Button variant="outline" className="text-destructive" disabled={act.isPending} onClick={() => confirm('Cancel this transfer?') && act.mutate('cancel')}>
              Cancel
            </Button>
          )}
          {act.isPending && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
        </div>
      )}
      {transfer.notes && <p className="mb-4 text-sm text-muted-foreground">{transfer.notes}</p>}
      <Card>
        <CardHeader>
          <CardTitle>Lines</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>SKU</TableHead>
                <TableHead className="text-right">Quantity</TableHead>
                <TableHead className="text-right">Received</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {transfer.lines.map((l) => (
                <TableRow key={l.id}>
                  <TableCell className="font-mono text-xs">{l.sku}</TableCell>
                  <TableCell className="text-right tabular-nums">{l.quantity}</TableCell>
                  <TableCell className="text-right tabular-nums">{l.receivedQuantity ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </>
  )
}
```

## `src/routes/_authenticated/transfers/index.tsx`

```tsx
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { z } from 'zod'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { StatusBadge } from '#/components/common/status-badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '#/auth'
import { requires } from '#/lib/guards'
import { transfersQuery } from '#/lib/queries'

const STATUSES = ['REQUESTED', 'IN_TRANSIT', 'RECEIVED', 'CANCELLED']

export const Route = createFileRoute('/_authenticated/transfers/')({
  beforeLoad: requires('transfer:read'),
  validateSearch: z.object({
    page: z.number().int().min(0).catch(0).default(0),
    status: z.string().optional(),
  }),
  component: TransfersPage,
})

function TransfersPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { can } = useAuth()
  const { data, isPending, isError } = useQuery(transfersQuery(search))

  return (
    <>
      <PageHeader
        title="Transfers"
        description="Stock moving between warehouses."
        actions={
          can('transfer:create') && (
            <Button asChild>
              <Link to="/transfers/new">
                <Plus /> New transfer
              </Link>
            </Button>
          )
        }
      />
      <div className="mb-4">
        <Select value={search.status ?? 'all'} onValueChange={(v) => navigate({ search: { status: v === 'all' ? undefined : v, page: 0 } })}>
          <SelectTrigger className="w-full sm:w-56" aria-label="Filter by status">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {s.replaceAll('_', ' ').toLowerCase()}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {isError ? (
        <EmptyState title="Could not load transfers" />
      ) : isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : !data.content.length ? (
        <EmptyState title="No transfers found" />
      ) : (
        <>
          <Card className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Reference</TableHead>
                  <TableHead>From</TableHead>
                  <TableHead>To</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Lines</TableHead>
                  <TableHead>Created</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((t) => (
                  <TableRow key={t.id}>
                    <TableCell className="font-mono text-xs font-medium">
                      <Link to="/transfers/$transferId" params={{ transferId: t.id }} className="hover:underline">
                        {t.referenceNumber}
                      </Link>
                    </TableCell>
                    <TableCell>{t.fromWarehouseCode}</TableCell>
                    <TableCell>{t.toWarehouseCode}</TableCell>
                    <TableCell>
                      <StatusBadge status={t.status} />
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{t.lines.length}</TableCell>
                    <TableCell className="whitespace-nowrap text-muted-foreground">{new Date(t.createdAt).toLocaleDateString()}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
          <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
        </>
      )}
    </>
  )
}
```

## `src/routes/_authenticated/transfers/new.tsx`

```tsx
import { useState } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { addLine, CartLines, type CartLine } from '#/components/common/cart-lines'
import { PageHeader } from '#/components/common/page-header'
import { VariantPicker } from '#/components/common/variant-picker'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { api } from '#/lib/api'
import { requires } from '#/lib/guards'
import { warehousesQuery } from '#/lib/queries'
import type { Transfer } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/transfers/new')({
  beforeLoad: requires('transfer:create'),
  component: NewTransferPage,
})

function NewTransferPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: warehouses } = useQuery(warehousesQuery)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [notes, setNotes] = useState('')
  const [lines, setLines] = useState<CartLine[]>([])

  const active = warehouses?.content.filter((w) => w.isActive) ?? []
  const ready = !!from && !!to && from !== to && lines.length > 0

  const create = useMutation({
    mutationFn: () =>
      api.post<Transfer>('/transfers', {
        fromWarehouseId: from,
        toWarehouseId: to,
        notes: notes.trim() || undefined,
        lines: lines.map((l) => ({ variantId: l.variantId, quantity: l.quantity })),
      }),
    onSuccess: (t) => {
      toast.success(`${t.referenceNumber} requested`)
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      navigate({ to: '/transfers/$transferId', params: { transferId: t.id } })
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const warehouseSelect = (id: string, label: string, value: string, set: (v: string) => void) => (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Select value={value} onValueChange={set}>
        <SelectTrigger id={id} className="w-full">
          <SelectValue placeholder="Select warehouse" />
        </SelectTrigger>
        <SelectContent>
          {active.map((w) => (
            <SelectItem key={w.id} value={w.id}>
              {w.code} · {w.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )

  return (
    <>
      <Link to="/transfers" className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Transfers
      </Link>
      <PageHeader title="New transfer" description="Stock is reserved at the source warehouse until the transfer is dispatched." />
      <div className="grid gap-6 xl:grid-cols-5">
        <Card className="xl:col-span-3">
          <CardHeader>
            <CardTitle>Add products</CardTitle>
          </CardHeader>
          <CardContent>
            <VariantPicker onAdd={(v) => setLines((ls) => addLine(ls, { variantId: v.variantId, sku: v.sku, productName: v.productName, quantity: 1 }))} />
          </CardContent>
        </Card>
        <div className="space-y-6 xl:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {warehouseSelect('from', 'From warehouse', from, setFrom)}
              {warehouseSelect('to', 'To warehouse', to, setTo)}
              {from && from === to && <p role="alert" className="text-xs text-destructive">Source and destination must differ.</p>}
              <div className="space-y-2">
                <Label htmlFor="notes">Notes</Label>
                <Input id="notes" value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={500} />
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Lines</CardTitle>
            </CardHeader>
            <CardContent>
              <CartLines lines={lines} onChange={setLines} />
              <Button className="mt-4 w-full" disabled={!ready || create.isPending} onClick={() => create.mutate()}>
                {create.isPending && <Loader2 className="animate-spin" />}
                Request transfer
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </>
  )
}
```

## `src/routes/_authenticated/users/index.tsx`

```tsx
import { useState } from 'react'
import { requires } from '#/lib/guards'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { RoleDialog } from '#/components/admin/role-dialog'
import { api } from '#/lib/api'
import { z } from 'zod'
import { ResetPasswordDialog, UserDialog } from '#/components/admin/user-dialogs'
import { Button } from '@/components/ui/button'
import type { Role, User } from '#/lib/types'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Pagination } from '#/components/common/pagination'
import { Badge } from '@/components/ui/badge'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAuth } from '#/auth'
import { rolesQuery, usersQuery } from '#/lib/queries'

export const Route = createFileRoute('/_authenticated/users/')({
  beforeLoad: requires('user:read'),
  validateSearch: z.object({
    view: z.enum(['users', 'roles']).catch('users').default('users'),
    page: z.number().int().min(0).catch(0).default(0),
  }),
  component: UsersPage,
})

function UsersPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { can } = useAuth()
  const [creating, setCreating] = useState(false)
  const [creatingRole, setCreatingRole] = useState(false)
  return (
    <>
      <PageHeader
        title="Users & roles"
        description="Who can sign in and what they are allowed to do."
        actions={
          search.view === 'users' ? (
            can('user:create') && (
              <Button onClick={() => setCreating(true)}>
                <Plus /> New user
              </Button>
            )
          ) : (
            can('role:manage') && (
              <Button onClick={() => setCreatingRole(true)}>
                <Plus /> New role
              </Button>
            )
          )
        }
      />
      <UserDialog open={creating} onOpenChange={setCreating} />
      <RoleDialog open={creatingRole} onOpenChange={setCreatingRole} />
      <Tabs className="mb-4" value={search.view} onValueChange={(v) => navigate({ search: { view: v as 'users' | 'roles', page: 0 } })}>
        <TabsList>
          <TabsTrigger value="users">Users</TabsTrigger>
          {can('role:read') && <TabsTrigger value="roles">Roles</TabsTrigger>}
        </TabsList>
      </Tabs>
      {search.view === 'roles' ? <Roles /> : <Users />}
    </>
  )
}

function Users() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { data, isPending, isError } = useQuery(usersQuery(page))
  const { can } = useAuth()
  const [editing, setEditing] = useState<User | null>(null)
  const [resetting, setResetting] = useState<User | null>(null)
  if (isError) return <EmptyState title="Could not load users" />
  if (isPending) return <Skeleton className="h-64 w-full" />
  if (!data.content.length) return <EmptyState title="No users" />
  return (
    <>
      <Card className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Joined</TableHead>
              {can('user:update') && <TableHead className="w-24" />}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.map((u) => (
              <TableRow key={u.id}>
                <TableCell className="font-medium">
                  {u.firstName} {u.lastName}
                </TableCell>
                <TableCell>{u.email}</TableCell>
                <TableCell>
                  <Badge variant="secondary">{u.roleName.replaceAll('_', ' ').toLowerCase()}</Badge>
                </TableCell>
                <TableCell>
                  <Badge variant={u.isActive ? 'default' : 'secondary'}>{u.isActive ? 'Active' : 'Inactive'}</Badge>
                </TableCell>
                <TableCell className="text-muted-foreground">{new Date(u.createdAt).toLocaleDateString()}</TableCell>
                {can('user:update') && (
                  <TableCell className="whitespace-nowrap text-right">
                    <Button variant="ghost" size="icon" aria-label={`Edit ${u.email}`} onClick={() => setEditing(u)}>
                      <Pencil />
                    </Button>
                    <Button variant="ghost" size="icon" aria-label={`Reset password for ${u.email}`} onClick={() => setResetting(u)}>
                      <KeyRound />
                    </Button>
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>
      <Pagination page={data.page} onChange={(n) => navigate({ search: (s) => ({ ...s, page: n }) })} />
      <UserDialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)} user={editing ?? undefined} />
      <ResetPasswordDialog user={resetting} onOpenChange={(o) => !o && setResetting(null)} />
    </>
  )
}

function Roles() {
  const { can } = useAuth()
  const queryClient = useQueryClient()
  const { data, isPending, isError } = useQuery(rolesQuery)
  const [editing, setEditing] = useState<Role | null>(null)
  const manage = can('role:manage')

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/roles/${id}`),
    onSuccess: () => {
      toast.success('Role deleted')
      queryClient.invalidateQueries({ queryKey: ['roles'] })
    },
    onError: (e: Error) => toast.error(e.message),
  })

  if (isError) return <EmptyState title="Could not load roles" />
  if (isPending) return <Skeleton className="h-64 w-full" />
  return (
    <>
      <div className="grid gap-4 md:grid-cols-2">
        {data.map((r) => (
          <Card key={r.id} className="p-5">
            <div className="flex items-center justify-between gap-2">
              <h2 className="font-semibold">{r.name.replaceAll('_', ' ')}</h2>
              <div className="flex items-center gap-1">
                <span className="mr-1 text-xs text-muted-foreground">
                  {r.userCount} user{r.userCount === 1 ? '' : 's'}
                  {r.builtIn ? ' · built-in' : ''}
                </span>
                {manage && (
                  <Button variant="ghost" size="icon" className="size-8" aria-label={`Edit ${r.name}`} onClick={() => setEditing(r)}>
                    <Pencil />
                  </Button>
                )}
                {manage && !r.builtIn && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="size-8"
                    aria-label={`Delete ${r.name}`}
                    disabled={remove.isPending}
                    onClick={() => confirm(`Delete the role ${r.name}?`) && remove.mutate(r.id)}
                  >
                    <Trash2 />
                  </Button>
                )}
              </div>
            </div>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {r.permissions.map((p) => (
                <Badge key={p} variant="outline" className="font-mono text-[11px] font-normal">
                  {p}
                </Badge>
              ))}
            </div>
          </Card>
        ))}
      </div>
      <RoleDialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)} role={editing ?? undefined} />
    </>
  )
}
```

## `src/routes/_authenticated/warehouses/index.tsx`

```tsx
import { useState } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Pencil, Plus } from 'lucide-react'
import { WarehouseDialog } from '#/components/inventory/warehouse-dialog'
import { EmptyState } from '#/components/common/empty-state'
import { PageHeader } from '#/components/common/page-header'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '#/auth'
import { requires } from '#/lib/guards'
import { warehousesQuery } from '#/lib/queries'
import type { Warehouse } from '#/lib/types'

export const Route = createFileRoute('/_authenticated/warehouses/')({
  beforeLoad: requires('warehouse:read'),
  component: WarehousesPage,
})

function WarehousesPage() {
  const { can } = useAuth()
  const { data, isPending, isError } = useQuery(warehousesQuery)
  const [dialog, setDialog] = useState<{ open: boolean; warehouse?: Warehouse }>({ open: false })

  return (
    <>
      <PageHeader
        title="Warehouses"
        description="Locations where stock is held."
        actions={
          can('warehouse:create') && (
            <Button onClick={() => setDialog({ open: true })}>
              <Plus /> New warehouse
            </Button>
          )
        }
      />
      <WarehouseDialog open={dialog.open} onOpenChange={(o) => setDialog((d) => ({ ...d, open: o }))} warehouse={dialog.warehouse} />
      {isError ? (
        <EmptyState title="Could not load warehouses" />
      ) : isPending ? (
        <Skeleton className="h-48 w-full" />
      ) : !data.content.length ? (
        <EmptyState title="No warehouses yet" description="Create one before receiving stock or taking orders." />
      ) : (
        <Card className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Warehouse</TableHead>
                <TableHead>Location</TableHead>
                <TableHead>Phone</TableHead>
                <TableHead>Status</TableHead>
                {can('warehouse:update') && <TableHead className="w-16" />}
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((w) => (
                <TableRow key={w.id}>
                  <TableCell>
                    <p className="font-medium">{w.name}</p>
                    <p className="font-mono text-xs text-muted-foreground">{w.code}</p>
                  </TableCell>
                  <TableCell>{[w.city, w.country].filter(Boolean).join(', ') || '—'}</TableCell>
                  <TableCell>{w.contactPhone ?? '—'}</TableCell>
                  <TableCell>
                    <Badge variant={w.isActive ? 'default' : 'secondary'}>{w.isActive ? 'Active' : 'Inactive'}</Badge>
                  </TableCell>
                  {can('warehouse:update') && (
                    <TableCell className="text-right">
                      <Button variant="ghost" size="icon" aria-label={`Edit ${w.name}`} onClick={() => setDialog({ open: true, warehouse: w })}>
                        <Pencil />
                      </Button>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </>
  )
}
```

## `src/routes/login.tsx`

```tsx
import { useState } from 'react'
import { createFileRoute, redirect, useRouter } from '@tanstack/react-router'
import { Boxes, Loader2 } from 'lucide-react'
import { z } from 'zod'
import { Button } from '#/components/ui/button'
import { Input } from '#/components/ui/input'
import { Label } from '#/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '#/components/ui/card'
import { useAuth } from '#/auth'
import { ApiError } from '#/lib/api'

export const Route = createFileRoute('/login')({
  validateSearch: z.object({ redirect: z.string().optional() }),
  beforeLoad: ({ context, search }) => {
    if (context.auth.isAuthenticated) throw redirect({ to: search.redirect ?? '/' })
  },
  component: LoginPage,
})

function LoginPage() {
  const { login } = useAuth()
  const router = useRouter()
  const search = Route.useSearch()
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    setPending(true)
    setError(null)
    try {
      await login(String(form.get('email')), String(form.get('password')))
      await router.invalidate()
      await router.navigate({ to: search.redirect ?? '/' })
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401 ? 'Invalid email or password.' : 'Unable to sign in. Please try again.')
    } finally {
      setPending(false)
    }
  }

  return (
    <main className="grid min-h-svh place-items-center bg-muted/40 p-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="flex flex-col items-center gap-2">
          <span className="grid size-11 place-items-center rounded-xl bg-primary text-primary-foreground">
            <Boxes className="size-6" />
          </span>
          <h1 className="text-2xl font-semibold tracking-tight">Apex</h1>
          <p className="text-sm text-muted-foreground">Retail inventory management</p>
        </div>
        <Card>
          <CardHeader>
            <CardTitle>Sign in</CardTitle>
            <CardDescription>Use your work email and password.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={onSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" name="email" type="email" autoComplete="username" required autoFocus />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input id="password" name="password" type="password" autoComplete="current-password" required />
              </div>
              {error && (
                <p role="alert" className="text-sm text-destructive">
                  {error}
                </p>
              )}
              <Button type="submit" className="w-full" disabled={pending}>
                {pending && <Loader2 className="animate-spin" />}
                Sign in
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </main>
  )
}
```

## `src/styles.css`

```css
@import "tailwindcss";
@import "tw-animate-css";
@import "@fontsource-variable/inter";
@import "@fontsource-variable/jetbrains-mono";

@custom-variant dark (&:is(.dark *));

:root {
  --radius: 0.625rem;
  --background: oklch(0.985 0.002 250);
  --foreground: oklch(0.21 0.02 255);
  --card: oklch(1 0 0);
  --card-foreground: oklch(0.21 0.02 255);
  --popover: oklch(1 0 0);
  --popover-foreground: oklch(0.21 0.02 255);
  --primary: oklch(0.52 0.19 262);
  --primary-foreground: oklch(0.985 0 0);
  --secondary: oklch(0.96 0.006 255);
  --secondary-foreground: oklch(0.28 0.03 255);
  --muted: oklch(0.96 0.006 255);
  --muted-foreground: oklch(0.52 0.025 255);
  --accent: oklch(0.95 0.015 262);
  --accent-foreground: oklch(0.3 0.08 262);
  --destructive: oklch(0.577 0.22 27);
  --border: oklch(0.91 0.008 255);
  --input: oklch(0.91 0.008 255);
  --ring: oklch(0.52 0.19 262);
  --success: oklch(0.6 0.15 155);
  --warning: oklch(0.75 0.16 75);
  --chart-1: oklch(0.52 0.19 262);
  --chart-2: oklch(0.6 0.15 155);
  --chart-3: oklch(0.75 0.16 75);
  --chart-4: oklch(0.62 0.2 320);
  --chart-5: oklch(0.577 0.22 27);
  --sidebar: oklch(0.2 0.03 262);
  --sidebar-foreground: oklch(0.92 0.01 255);
  --sidebar-primary: oklch(0.62 0.19 262);
  --sidebar-primary-foreground: oklch(0.985 0 0);
  --sidebar-accent: oklch(0.27 0.04 262);
  --sidebar-accent-foreground: oklch(0.985 0 0);
  --sidebar-border: oklch(0.3 0.03 262);
  --sidebar-ring: oklch(0.62 0.19 262);
}

.dark {
  --background: oklch(0.16 0.015 262);
  --foreground: oklch(0.96 0.005 255);
  --card: oklch(0.2 0.02 262);
  --card-foreground: oklch(0.96 0.005 255);
  --popover: oklch(0.2 0.02 262);
  --popover-foreground: oklch(0.96 0.005 255);
  --primary: oklch(0.65 0.18 262);
  --primary-foreground: oklch(0.16 0.015 262);
  --secondary: oklch(0.26 0.02 262);
  --secondary-foreground: oklch(0.96 0.005 255);
  --muted: oklch(0.26 0.02 262);
  --muted-foreground: oklch(0.7 0.02 255);
  --accent: oklch(0.28 0.04 262);
  --accent-foreground: oklch(0.96 0.005 255);
  --destructive: oklch(0.7 0.19 22);
  --border: oklch(1 0 0 / 11%);
  --input: oklch(1 0 0 / 15%);
  --ring: oklch(0.65 0.18 262);
  --success: oklch(0.72 0.15 155);
  --warning: oklch(0.8 0.15 80);
  --sidebar: oklch(0.14 0.02 262);
  --sidebar-accent: oklch(0.24 0.03 262);
  --sidebar-border: oklch(1 0 0 / 10%);
}

@theme inline {
  --font-sans: "Inter Variable", ui-sans-serif, system-ui, sans-serif;
  --font-mono: "JetBrains Mono Variable", ui-monospace, monospace;
  --radius-sm: calc(var(--radius) - 4px);
  --radius-md: calc(var(--radius) - 2px);
  --radius-lg: var(--radius);
  --radius-xl: calc(var(--radius) + 4px);
  --color-background: var(--background);
  --color-foreground: var(--foreground);
  --color-card: var(--card);
  --color-card-foreground: var(--card-foreground);
  --color-popover: var(--popover);
  --color-popover-foreground: var(--popover-foreground);
  --color-primary: var(--primary);
  --color-primary-foreground: var(--primary-foreground);
  --color-secondary: var(--secondary);
  --color-secondary-foreground: var(--secondary-foreground);
  --color-muted: var(--muted);
  --color-muted-foreground: var(--muted-foreground);
  --color-accent: var(--accent);
  --color-accent-foreground: var(--accent-foreground);
  --color-destructive: var(--destructive);
  --color-border: var(--border);
  --color-input: var(--input);
  --color-ring: var(--ring);
  --color-success: var(--success);
  --color-warning: var(--warning);
  --color-chart-1: var(--chart-1);
  --color-chart-2: var(--chart-2);
  --color-chart-3: var(--chart-3);
  --color-chart-4: var(--chart-4);
  --color-chart-5: var(--chart-5);
  --color-sidebar: var(--sidebar);
  --color-sidebar-foreground: var(--sidebar-foreground);
  --color-sidebar-primary: var(--sidebar-primary);
  --color-sidebar-primary-foreground: var(--sidebar-primary-foreground);
  --color-sidebar-accent: var(--sidebar-accent);
  --color-sidebar-accent-foreground: var(--sidebar-accent-foreground);
  --color-sidebar-border: var(--sidebar-border);
  --color-sidebar-ring: var(--sidebar-ring);
}

@layer base {
  * {
    @apply border-border outline-ring/50;
  }
  html,
  body,
  #app {
    min-height: 100%;
  }
  body {
    @apply bg-background text-foreground font-sans antialiased;
    margin: 0;
  }
}
```

## `tsconfig.json`

```json
{
  "include": ["**/*.ts", "**/*.tsx"],
  "compilerOptions": {
    "target": "ES2022",
    "jsx": "react-jsx",
    "module": "ESNext",
    "paths": {
      "#/*": ["./src/*"],
      "@/*": ["./src/*"]
    },
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "types": ["vite/client"],

    /* Bundler mode */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "verbatimModuleSyntax": true,
    "noEmit": true,

    /* Linting */
    "skipLibCheck": true,
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedSideEffectImports": true
  }
}
```

## `tsr.config.json`

```json
{
  "target": "react"
}
```

## `vite.config.ts`

```ts
import { defineConfig } from 'vite'
import { devtools } from '@tanstack/devtools-vite'

import { tanstackRouter } from '@tanstack/router-plugin/vite'

import viteReact from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const config = defineConfig({
  resolve: { tsconfigPaths: true },
  plugins: [
    devtools(),
    tailwindcss(),
    tanstackRouter({ target: 'react', autoCodeSplitting: true }),
    viteReact(),
  ],
})

export default config
```
