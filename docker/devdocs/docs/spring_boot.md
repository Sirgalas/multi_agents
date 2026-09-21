# Spring Boot Architecture & Guidelines

- **Framework**: Spring Boot 3.4+ on Java 21 LTS.
- **DTOs**: Use Java Records for request and response DTOs.
- **Dependency Injection**: Use constructor injection via Lombok @RequiredArgsConstructor (avoid field @Autowired).
- **Validation**: Strict validation on inputs with @Valid, @NotNull, @NotBlank, @Size, @Pattern.
- **Error Handling**: Standard RFC 7807 ProblemDetail via centralized @RestControllerAdvice.
- **Transactions & Persistence**: Spring Data JPA with explicit @Transactional(readOnly = true) on read queries and @Transactional on state modifications.
- **Layering**: Clean separation: Controller -> Service -> Repository -> Entity. Keep controllers focused strictly on HTTP mapping.
- **Configuration**: Type-safe @ConfigurationProperties records or classes with validation.
- **Observability**: Actuator endpoints for health checks, metrics, and Prometheus monitoring.
