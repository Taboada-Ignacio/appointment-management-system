# Multi-stage Dockerfile para api-turnos con Java 25

# 1. Etapa de compilación
FROM maven:3.9.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# 2. Etapa de ejecución
FROM eclipse-temurin:25-jre
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser:appgroup

COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

