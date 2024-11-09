FROM bellsoft/liberica-openjdk-alpine:17.0.12

# Set working directory
WORKDIR /app

# Copy the pre-built JAR into the container
COPY build/libs/api-server-0.0.1-SNAPSHOT.jar /app/app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
