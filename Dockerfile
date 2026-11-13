# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY gradle-9.8.2 /opt/gradle
ENV PATH="/opt/gradle:${PATH}"

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew
RUN gradle bootJar --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]