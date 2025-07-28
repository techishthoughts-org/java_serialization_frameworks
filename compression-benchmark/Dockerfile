FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY compression-benchmark/pom.xml compression-benchmark/

# Copy source code
COPY compression-benchmark/src compression-benchmark/src
COPY common-payload/src common-payload/src

# Install Maven and build
RUN apt-get update && apt-get install -y maven curl
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8086

# Run the application
CMD ["java", "-jar", "compression-benchmark/target/compression-benchmark-1.0.0.jar"]
