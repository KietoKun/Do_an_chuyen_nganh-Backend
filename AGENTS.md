# Repository Guidelines

## Project Structure & Module Organization
This is a Java 17 Spring Boot backend for a pizza store API. Main code lives in `src/main/java/com/pizzastore`, organized by responsibility:

- `controller/` exposes HTTP endpoints and request/response classes.
- `service/` contains business logic.
- `repository/` contains Spring Data JPA interfaces.
- `entity/`, `dto/`, `enums/`, `security/`, `config/`, `scheduler/`, and `util/` hold domain models, transfer objects, shared configuration, and helpers.

Runtime configuration is in `src/main/resources/Application.yml`. Root-level deployment files include `Dockerfile`, `docker-compose.yml`, `.env.example`, and `pom.xml`. Build output is generated under `target/` and should not be edited.

## Build, Test, and Development Commands
Use the Maven wrapper when available:

- `./mvnw spring-boot:run` or `mvn spring-boot:run`: run the API locally on port `8080`.
- `./mvnw test` or `mvn test`: run the test suite.
- `./mvnw clean package` or `mvn clean package`: compile, test, and build the JAR in `target/`.
- `docker compose up --build`: start PostgreSQL and the backend using root Docker files.

Copy `.env.example` to `.env` for local secrets before running Docker or the app.

## Coding Style & Naming Conventions
Follow standard Spring Boot conventions with 4-space indentation. Keep classes in the existing `com.pizzastore` package tree and name types by role, such as `OrderController`, `OrderService`, `OrderRepository`, `OrderRequest`, and `OrderStatus`. Prefer constructor injection for new dependencies when possible. Use Lombok consistently with existing entities and DTOs, but avoid adding annotations that hide important validation or business logic.

## Testing Guidelines
The project includes `spring-boot-starter-test` and `spring-security-test`. Place tests under `src/test/java/com/pizzastore` using names like `OrderServiceTest` or `AuthControllerTest`. Add service tests for business rules and controller tests for request validation, security behavior, and response status codes. Run `mvn test` before opening a pull request.

## Commit & Pull Request Guidelines
Recent commits use short, imperative summaries, for example `Fix Order,Add Coupon` and `Update Comment`. Keep new commit subjects concise and action-oriented, and mention the affected area when useful.

Pull requests should include a brief description, test results, linked issues or tasks, and API examples or screenshots when endpoint behavior changes. Note any required environment variables or database migration effects.

## Security & Configuration Tips
Do not commit real secrets. Required values include `DB_PASSWORD`, `JWT_SECRET`, Cloudinary credentials, VNPay settings, `GOONG_API_KEY`, and `VNPAY_RETURN_URL`. Keep `.env` local and update `.env.example` when adding new configuration keys.
