# RamsAI Kitchen — Claude context

## Project overview
Real-time restaurant management system. Spring Boot 3.2 backend serving static HTML/JS/CSS pages. No active React frontend (the `frontend/` directory is an unused Vite stub).

## Stack
- **Backend:** Java 17, Spring Boot 3.2, Spring Security (JWT + refresh tokens), Spring Data JPA (Hibernate), MapStruct, Lombok
- **Database:** PostgreSQL, schema-managed by Flyway migrations in `src/main/resources/db/migration/`
- **Frontend:** Vanilla JS + HTML + CSS under `src/main/resources/static/`. `auth.js` is loaded on every page and handles auth state, nav rewriting, and `authenticatedFetch`.
- **Build:** Maven (`pom.xml`). No `mvnw` wrapper — use system `mvn`.
- **Docker:** `docker-compose.yml` for local dev (app + Postgres).

## Package layout (`com.ramsai.kitchen`)
```
controllers/     REST endpoints
services/        Business logic
repositories/    Spring Data JPA interfaces
models/
  entities/      JPA entities
  dtos/          Java records used as request/response bodies
mappers/         MapStruct interfaces
enums/           OrderStatus, ItemStatus, TableStatus, UserRole, ...
config/          SecurityConfig, JwtAuthenticationFilter, ApplicationConfig, WebConfig
exceptions/      GlobalExceptionHandler, ResourceNotFoundException, ...
```

## Auth & roles
Four roles: `CUSTOMER`, `WAITER`, `CHEF`, `MANAGER`.  
JWT stored in `localStorage` or `sessionStorage`. Refresh token flow is in `auth.js` (`authenticatedFetch`).  
`SecurityConfig` explicitly permits static files and some GET endpoints; everything else requires authentication. Role gates: `MANAGER`-only, `CHEF|MANAGER` for kitchen, `WAITER|MANAGER` for table management.

## Key domain concepts
- **Order** — belongs to a `RestaurantTable` and a `customerId`. Status flow: `DRAFT → RECEIVED → COOKING → READY → SERVED` (or `CANCELLED`). A DRAFT is the customer's live cart.
- **OrderItem** — item inside an order. Status flow: `PENDING → COOKING → READY → SERVED` (or `CANCELLED`).
- **RestaurantTable** — grid cell on the floor plan. Multiple cells share a `tableNumber` to form a group. `occupiedByUserId` tracks which customer is seated.
- **CartService** — manages the single DRAFT order per customer. Checkout transitions it to `RECEIVED` and marks the table `OCCUPIED`.
- **KitchenService** — kitchen staff update `ItemStatus`; moving to `COOKING` triggers inventory deduction.

## Frontend conventions
- Every page includes `auth.js` (manages nav + cart count) then the page-specific JS.
- `authenticatedFetch(url, options)` — use this everywhere instead of bare `fetch` so token refresh happens transparently.
- Nav is **rewritten by `auth.js`** when the user is logged in; static nav in HTML is only shown to unauthenticated visitors.
- `sessionStorage.selectedTableId` / `selectedTableNumber` — set when a customer picks a table on the floor plan. Cleared on checkout. Don't rely on it for post-checkout state; use `occupiedByUserId` from the API instead.
- CSS variables: `--primary-color: #f39c12`, `--secondary-color: #2c3e50`, `--bg-light: #fafafa`, `--shadow`.

## API conventions
- Base path: `/api/v1/`
- Responses wrap data: `{ "data": ..., "message": "..." }`
- `@AuthenticationPrincipal User user` — inject the current user in controllers.
- DTOs are Java records.

## Coding style
- No comments unless the WHY is non-obvious.
- No docstrings or multi-line comment blocks.
- No error handling for impossible scenarios; validate only at system boundaries.
- No speculative abstractions — solve the task at hand, no more.
- Prefer editing existing files over creating new ones.
- When adding a new static page, add it to the `permitAll` list in `SecurityConfig`.
- Schema changes go in a new Flyway migration file (`V{N}__description.sql`).

## Running locally
```bash
docker-compose up        # starts Postgres + app
# or
mvn spring-boot:run      # after Postgres is up
```
App listens on `http://localhost:8080`.
