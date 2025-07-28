FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY hessian-poc/pom.xml hessian-poc/

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY hessian-poc/src hessian-poc/src

# Build the application
RUN ./mvnw clean package -pl hessian-poc -am -DskipTests

# Create runtime image
FROM openjdk:21-jre-slim

WORKDIR /app

# Copy the built JAR
COPY --from=0 /app/hessian-poc/target/*.jar app.jar

# Expose port
EXPOSE 8089

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8089/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
