# syntax=docker/dockerfile:1
# E-Commerce Shop (Spring MVC + Thymeleaf) - multi-stage build
#   docker build -t nhacg1 .
#   docker run -d --name nhacg1 -p 8080:8080 nhacg1
#   docker compose up -d --build        (web app + MySQL, see docker-compose.yml)

# ---------- 1. Build ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Maven wrapper and pom first, so dependencies are cached until pom.xml changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Strip Windows line endings in case the wrapper was checked out with CRLF
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests run in CI / locally with ./mvnw test; skip them here to keep image builds fast
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------- 2. Runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as an unprivileged user
RUN addgroup -S spring && adduser -S spring -G spring

# Layers from least to most frequently changed, for better image caching
COPY --from=build --chown=spring:spring /workspace/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/application/ ./

USER spring

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
