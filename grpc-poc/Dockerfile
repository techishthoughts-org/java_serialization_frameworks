FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY grpc-poc/pom.xml grpc-poc/

# Copy source code
COPY grpc-poc/src grpc-poc/src
COPY common-payload/src common-payload/src

# Install Maven, curl, and protobuf compiler
RUN apt-get update && apt-get install -y maven curl protobuf-compiler

# Build the application
RUN mvn clean package -DskipTests

# Expose ports (HTTP and gRPC)
EXPOSE 8092 9092

# Run the application
CMD ["java", "-jar", "grpc-poc/target/grpc-poc-1.0.0.jar"]
