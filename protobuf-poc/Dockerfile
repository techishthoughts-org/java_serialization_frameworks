FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY protobuf-poc/pom.xml protobuf-poc/

# Copy source code
COPY protobuf-poc/src protobuf-poc/src
COPY common-payload/src common-payload/src

# Install Maven and build
RUN apt-get update && apt-get install -y maven curl
RUN mvn clean package -DskipTests

# Expose ports
EXPOSE 8082
EXPOSE 9090

# Run the application
CMD ["java", "-jar", "protobuf-poc/target/protobuf-poc-1.0.0.jar"]
