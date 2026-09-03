# Multi-stage Dockerfile para api-turnos con Java 21 LTS

# 1. Etapa de compilación
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# 2. Etapa de ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Instalar curl para healthcheck
RUN apk add --no-cache curl

# Crear usuario sin privilegios por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=30s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]


