# syntax=docker/dockerfile:1.6

# Stage 1: build the Spring Boot jar with Maven.
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

ARG MAVEN_CLI_OPTS="-B -ntp -Dmaven.wagon.http.retryHandler.count=10 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 -Dmaven.wagon.rto=120000"

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn ${MAVEN_CLI_OPTS} dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn ${MAVEN_CLI_OPTS} package -DskipTests

# Stage 2: run the built jar.
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
