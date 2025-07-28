FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY benchmark-comparison/pom.xml benchmark-comparison/

# Copy source code
COPY benchmark-comparison/src benchmark-comparison/src
COPY common-payload/src common-payload/src

# Install Maven and build
RUN apt-get update && apt-get install -y maven curl
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "benchmark-comparison/target/benchmark-comparison-1.0.0.jar"]
