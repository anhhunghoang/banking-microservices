# Java Coding and Testing Rules

## 1. General Coding Standards

### 1.1 Naming Conventions
- **Classes**: PascalCase (e.g., `TransactionController`).
- **Methods**: camelCase (e.g., `processPayment`).
- **Variables**: camelCase (e.g., `accountBalance`).
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_ATTEMPTS`).
- **Packages**: lowercase (e.g., `com.banking.account`).
- **Interfaces**: PascalCase. Avoid `I` prefix (e.g., `AccountRepository`, not `IAccountRepository`).
- **Test Classes**: Append `Test` to the class name (e.g., `UserServiceTest`).

### 1.2 Formatting
- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
- Use 4 spaces for indentation.
- Maximum line length: 120 characters.
- Always use braces `{}` for control structures (if, else, for, do, while), even for single-line statements.
- Organize imports: Standard Java -> Third-party -> Project specific. Remove unused imports.

## 2. Java Language Features (Java 21+)

- **Records**: Use `record` for data carriers, DTOs, and config properties where immutability is improved.
  ```java
  public record UserDto(String username, String email) {}
  ```
- **Pattern Matching**: Utilize `instanceof` pattern matching to reduce boilerplate.
- **Switch Expressions**: Use enhanced switch expressions for cleaner logic.
- **Optional**: Use `Optional` for return types that might be empty. avoid `null` returns. Do not use `Optional` in field declarations or method parameters.
- **Stream API**: Use Streams for collections processing where readability is enhanced, but prefer simple loops for performance-critical hot paths if necessary.
- **Immutability**: Prefer `final` fields and immutable collections (`List.of()`, `Map.of()`).

## 3. Spring Boot & Microservices Best Practices

- **Dependency Injection**: Always use **Constructor Injection**. Avoid `@Autowired` on fields.
  ```java
  @Service
  @RequiredArgsConstructor // Lombok
  public class AccountService {
      private final AccountRepository accountRepository;
  }
  ```
- **Layered Architecture**:
  - `Controller`: Handle HTTP requests, validation, and serialization. Keep logic minimal.
  - `Service`: Business logic, transaction boundaries (`@Transactional`).
  - `Repository`: Data access logic only.
- **Entities & DTOs**: Never expose Entity classes directly in the API. Map Entities to DTOs in the Service or Controller layer (tools like MapStruct are recommended).
- **Configuration**: Externalize configuration. Use `@ConfigurationProperties` over `@Value`.
- **Exception Handling**: Use `@ControllerAdvice` / `@RestControllerAdvice` for global exception handling. Return standard error responses (RFC 7807 Problem Details).

## 4. Testing Guidelines

### 4.1 General
- **Frameworks**: JUnit 5, Mockito, AssertJ.
- **Naming**: Method names should describe the behavior and expected result.
  - Format: `should[ExpectedBehavior]_when[StateUnderThreshold]` or `methodName_condition_expectedResult`.
  - Example: `shouldReturnActiveUser_whenUserIdExists()`
- **Structure**: Follow AAA (Arrange, Act, Assert).

### 4.2 Unit Tests
- Test logic in isolation.
- Mock all external dependencies.
- Coverage: Aim for >80% code coverage.
- Avoid using Spring context (`@SpringBootTest`) for unit tests; use `@ExtendWith(MockitoExtension.class)`.

### 4.3 Integration Tests
- Use `@SpringBootTest` with `@ActiveProfiles("test")`.
- Use **Testcontainers** for database, message brokers (Kafka/RabbitMQ), and external services. H2 database usage is discouraged if production uses PostgreSQL/MySQL.
- Verify end-to-end flows for critical paths.
- Ensure tests are independent and clean up data after execution (or use `@Transactional` to rollback).

## 5. Documentation & Comments
- **Javadoc**: Required for public APIs, Service interfaces, and complex algorithms.
- **Self-Documenting Code**: Code should be readable without comments. Use descriptive method/variable names.
- **comments**: Use comments to explain *why* something is done, not *what* is done.

## 6. Commit Message Guidelines
- Follow Conventional Commits format: `<type>[optional scope]: <description>`
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
- Example: `feat(payment): add retry logic for failed transactions`
