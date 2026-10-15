# Multi-stage build of the NexusMarket API.
#   docker build -t nexusmarket-api .
# Configuration comes only from environment variables at run time (see docker-compose.yml); no secret is baked in.

# --- Build stage: compile and package the Spring Boot jar --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Dependencies first, so they are cached while only the sources change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# The tests run in the normal build (mvn test); the image build only packages.
RUN mvn -B -q package -DskipTests

# --- Runtime stage: JRE only, non-root user ------------------------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 10001 --no-create-home nexus
COPY --from=build /workspace/target/*.jar /app/app.jar
USER nexus

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
