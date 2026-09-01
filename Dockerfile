# Multi-stage build for Java 26 Spring Boot Application

# Stage 1: Build stage
FROM eclipse-temurin:26-jdk AS builder
WORKDIR /app

# Copy build configuration files
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Grant execution permissions on Gradle Wrapper
RUN chmod +x gradlew

# Download dependencies (cached layer unless build.gradle/settings.gradle changes)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build production executable JAR skipping unit tests (tests can be executed during CI)
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:26-jre
WORKDIR /app

# Create non-root system user for runtime security
RUN groupadd -r appgroup && useradd -r -g appgroup -s /bin/false appuser

# Copy built application artifact from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Adjust file ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication -Xms256m -Xmx1024m"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]