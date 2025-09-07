# =============================
# DMS Backend - Spring Boot
# Multi-stage build: Build with Maven, run with JRE
# =============================

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom first for caching deps
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy source
COPY src ./src

# Build jar
RUN mvn -q -e -DskipTests package

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built jar
COPY --from=builder /app/target/*.jar /app/app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Allow passing JVM options, e.g. -Xms256m -Xmx512m
ENV JAVA_OPTS=""

# Spring Boot reads env to override properties, e.g.:
#   SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD
#   SPRING_DATA_REDIS_HOST, SPRING_DATA_REDIS_PORT, SPRING_KAFKA_BOOTSTRAP_SERVERS
#   APPLICATION_EMAIL_* etc.

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
