# Use an official OpenJDK image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy jar from target folder (make sure to build jar first)
COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8085

# Environment variables (optional but useful)
ENV SPRING_PROFILES_ACTIVE=default

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
