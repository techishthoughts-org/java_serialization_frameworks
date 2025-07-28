FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY kryo-poc/pom.xml kryo-poc/

# Copy source code
COPY kryo-poc/src kryo-poc/src
COPY common-payload/src common-payload/src

# Install Maven and build
RUN apt-get update && apt-get install -y maven curl
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8084

# Run the application
CMD ["java", "-jar", "kryo-poc/target/kryo-poc-1.0.0.jar"]
