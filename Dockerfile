## Use an official OpenJDK runtime as a parent image
#FROM openjdk:17-alpine
#
## Set the working directory to /app
#WORKDIR /app
#
## Copy the application JAR file to the container
#COPY target/demo-0.0.1-SNAPSHOT.jar /app
#
## Run the application when the container starts
#CMD ["java", "-jar", "demo-0.0.1-SNAPSHOT.jar"]

# Use a Maven 3.8.3 parent image with JDK 17
FROM maven:3.8.3-openjdk-17-slim AS build

# Set the working directory to /app
WORKDIR /app

# Copy the pom.xml and project files to the container
COPY pom.xml .
COPY src/ ./src/

# Build the application, skipping tests
RUN mvn clean package -DskipTests

# Create a new container based on a lightweight Alpine Linux image with JRE 17
FROM openjdk:17.0.1-jdk-slim

# Set the working directory to /app
WORKDIR /app

# Copy the JAR file from the build container to the new container
COPY --from=build /app/target/*.jar .

# Expose port 8080 for the application
#EXPOSE 8080

# Start the application
CMD ["java",  "-Dkafka.host=${KAFKA_HOST}", "-Dkafka.port=${KAFKA_PORT}", "-jar", "demo-0.0.1-SNAPSHOT.jar"]
