FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY fory-poc/pom.xml fory-poc/

# Copy source code
COPY fory-poc/src fory-poc/src
COPY common-payload/src common-payload/src

# Install Maven and build
RUN apt-get update && apt-get install -y maven curl
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8085

# Run the application
CMD ["java", "-jar", "fory-poc/target/fory-poc-1.0.0.jar"]
