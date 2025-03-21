
FROM openjdk:17-jdk-alpine

# Set a working directory inside the container
WORKDIR /app

# Copy the JAR file from the target directory
COPY target/*.jar app.jar

# Expose the application port (if needed)
EXPOSE 8080 port

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

