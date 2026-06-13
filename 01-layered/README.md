# 01 — Layered Architecture

The classic Spring Boot pattern: requests flow top-down through Controller → Service → Repository → Database.

## Structure

```
com.example.layered
├── controller/    REST endpoints + GlobalExceptionHandler
├── service/       Business logic, @Transactional boundaries
├── repository/    Spring Data JPA repositories
├── domain/        JPA entities
├── dto/           Request/response records
└── exception/     Domain exceptions
```

## Run

```bash
mvn spring-boot:run
```

Server starts on **port 8081**.

## Try it

```bash
# Create
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Widget","price":19.99,"stock":100}'

# List
curl http://localhost:8081/api/products

# Get one
curl http://localhost:8081/api/products/1
```

## What to notice

- `ProductController` is thin — it just delegates to the service and returns DTOs.
- `ProductService` owns the transaction boundary (`@Transactional`).
- The `Product` JPA entity is the *only* domain model — there's no separation between persistence and domain. This is the main weakness of layered architecture and what hexagonal/clean fix.
- DTOs (`ProductRequest`, `ProductResponse`) keep the JPA entity out of the API surface.
