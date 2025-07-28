FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY jackson-poc/pom.xml jackson-poc/

# Copy source code
COPY jackson-poc/src jackson-poc/src
COPY common-payload/src common-payload/src

# Install Maven and build
RUN apt-get update && apt-get install -y maven curl
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8081

# Run the application
CMD ["java", "-jar", "jackson-poc/target/jackson-poc-1.0.0.jar"]
