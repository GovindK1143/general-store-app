# Use OpenJDK 17 base image
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy built JAR file into container
COPY target/configserver-0.0.1-SNAPSHOT.jar app.jar

# Expose Config Server port
EXPOSE 8888

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
