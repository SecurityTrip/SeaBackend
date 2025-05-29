# ----------------------
# 1. Build Stage
# ----------------------
FROM gradle:8.13.0-jdk21 AS build

# Set working directory inside container
WORKDIR /home/gradle/project

# Copy Gradle configuration files
COPY build.gradle ./
COPY gradle ./gradle

# Copy application source code
COPY src ./src

# Build the application (skip tests for speed; remove -x test to run them)
RUN gradle clean build --no-daemon -x test


# ----------------------
# 2. Runtime Stage
# ----------------------
FROM eclipse-temurin:21-jre-jammy

# Define application directory
WORKDIR /app

# Copy built JAR from the build stage
ARG JAR_FILE=build/libs/*.jar
COPY --from=build /home/gradle/project/${JAR_FILE} app.jar

# Expose default Spring Boot port
EXPOSE 80

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]