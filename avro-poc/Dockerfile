FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY avro-poc/pom.xml avro-poc/

# Copy source code
COPY avro-poc/src avro-poc/src
COPY common-payload/src common-payload/src

# Install Maven and build
RUN apt-get update && apt-get install -y maven curl
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8083

# Run the application
CMD ["java", "-jar", "avro-poc/target/avro-poc-1.0.0.jar"]
