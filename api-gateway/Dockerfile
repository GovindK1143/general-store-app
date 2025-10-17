# Use OpenJDK 17 base image
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy built JAR into container
COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Expose the gateway port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
