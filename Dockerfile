FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user (Debian way)
RUN groupadd -r jokester && useradd -r -g jokester jokester

# Copy jar
COPY target/*.jar app.jar

# Set permissions
RUN chown jokester:jokester app.jar
USER jokester

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]