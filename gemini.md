# Ramsai Kitchen — gemini.md

## 1. Project Context
Ramsai Kitchen is a real-time digital restaurant management system built with Java and Spring Boot.

### Business Requirements
- For all business rules, user stories, and feature details, ALWAYS read the local file: `docs/user_stories.md`.

### Core Features
- Customer ordering & live tracking
- Kitchen Display System (KDS)
- Waitstaff floor management
- Inventory & management dashboard

## 2. Tech Stack
- Backend: Java 17+, Spring Boot 3+
- Database: PostgreSQL
- Migrations: Flyway or Liquibase
- Real-Time: WebSockets (STOMP)
- Security: Spring Security + JWT
- Mapping: MapStruct
- Boilerplate: Lombok

## 3. Architecture & Package Structure
Use a layered architecture or feature-based structure.

### Mandatory packages
- controllers
- services
- repositories
- models.entities
- models.dtos
- mappers
- exceptions
- config

### Base package
com.ramsai.kitchen.<feature>

## 4. Coding Standards

### Naming
- Classes: PascalCase
- Methods/variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Endpoints: /api/v1/<plural-kebab-case>

### General Rules
- Use `jakarta.*` packages instead of `javax.*` (Spring Boot 3 standard).
- Use constructor injection only
- Use Lombok @RequiredArgsConstructor
- Use final wherever possible
- Use @Slf4j for logging
- NEVER use System.out.println

## 5. Entity & DTO Rules
- NEVER expose entities in controllers
- Always use DTOs (Java record)
- Use MapStruct for mapping
- Apply validation annotations (from `jakarta.validation.constraints`):
    - @NotNull
    - @NotBlank
    - @Positive

## 6. Database & Performance
- Avoid N+1 queries
- Use:
    - JOIN FETCH
    - @EntityGraph
- All schema changes must go through Flyway/Liquibase

### Migration Naming (Flyway example)
V1__init_schema.sql
V2__add_orders_table.sql

## 7. Transactions
- Use @Transactional at service layer only
- Default: readOnly = true for queries
- Explicit write transactions for mutations
- Rollback on business exceptions

## 8. Exception Handling
- Create custom exceptions:
    - ResourceNotFoundException
    - InsufficientStockException
- Use global handler with @RestControllerAdvice

### Standard Error Response
{
"timestamp": "...",
"status": 400,
"error": "Bad Request",
"message": "...",
"path": "/api/v1/..."
}

## 9. API Response Format

### Success Response
{
"data": {},
"message": "Success"
}

### Error Response
Handled globally via exception handler.

## 10. Security Rules
- Roles:
    - CUSTOMER
    - WAITER
    - CHEF
    - MANAGER

- Use JWT authentication
- Protect endpoints with role-based access
- Never expose sensitive data

## 11. Testing
- Use:
    - JUnit 5
    - Mockito
    - Spring Boot Test

### Requirements
- Unit tests for services
- Integration tests for controllers
- Cover business logic and edge cases

## 12. AI Code Generation Rules
When generating code, ALWAYS follow this order:

1. Entity
2. Repository
3. DTO
4. Mapper
5. Service
6. Controller

### Mandatory
- Include package declaration
- Include all imports
- Provide production-ready code
- Use English for code
- Use Romanian for explanations

## 13. Assumptions Rule
If requirements are unclear:
- DO NOT invent fields or logic
- Either:
    - Ask for clarification
    - OR clearly state assumptions

## 14. Forbidden Practices
- Field injection (@Autowired on fields)
- Returning entities in API
- Business logic in controllers
- Generic 500 errors
- Skipping DTO layer
- Missing validation
- Hardcoded values
- Ignoring transactions
- Using `javax.*` packages for validation or persistence.

## 15. Logging
Use structured logs:
log.info("Order {} created for table {}", orderId, tableId);

### Levels
- info → normal flow
- warn → recoverable issues
- error → failures