# Use OpenJDK 17 base image
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy jar file into container
COPY target/eureka-0.0.1-SNAPSHOT.jar app.jar

# Expose Eureka Server port
EXPOSE 8761

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
