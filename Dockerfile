# Stage 1: Build the application JAR using Maven inside Docker.
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first so Docker can cache the dependency download layer.
# If pom.xml does not change between builds, Maven skips re-downloading all dependencies.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source code and build the JAR, skipping tests to speed up the build.
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Create the final runtime image using only the JAR.
# This image has no Maven, no source code — just the JRE and the JAR file.
# Result is a much smaller and more secure image than including the full build environment.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user to run the application.
# Running as root inside a container is a security risk — this prevents it.
RUN addgroup -S jokester && adduser -S jokester -G jokester

# Copy only the built JAR from Stage 1 into this minimal runtime image.
COPY --from=builder /app/target/*.jar app.jar

RUN chown jokester:jokester app.jar
USER jokester

EXPOSE 8080

# Start the application with JVM flags tuned for running inside a Docker container.
# UseContainerSupport: JVM reads Docker memory limits instead of the host machine RAM.
# MaxRAMPercentage=75: Use 75 percent of the container memory for the Java heap.
# UseG1GC: Garbage collector optimized for low latency and predictable pause times.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-jar", "app.jar"]