# syntax=docker/dockerfile:1

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Production
FROM eclipse-temurin:21-jre-alpine

ENV JAVA_OPTS="-XX:+UseZGC -XX:+UseStringDeduplication" \
    SPRING_PROFILES_ACTIVE=prod

WORKDIR /app

# Copy built jar from builder
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 18020

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=40s \
  CMD wget --quiet --tries=1 --spider http://localhost:18020/health || exit 1

# Run the application with virtual threads enabled
CMD ["java", "-jar", "app.jar"]
