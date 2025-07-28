FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY fst-poc/pom.xml fst-poc/

# Copy source code
COPY fst-poc/src fst-poc/src
COPY common-payload/src common-payload/src

# Install Maven and curl
RUN apt-get update && apt-get install -y maven curl

# Build the application
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8090

# Run the application with JVM arguments for FST
CMD ["java", \
     "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
     "--add-opens", "java.base/java.util=ALL-UNNAMED", \
     "--add-opens", "java.base/java.io=ALL-UNNAMED", \
     "--add-opens", "java.base/java.math=ALL-UNNAMED", \
     "--add-opens", "java.base/java.time=ALL-UNNAMED", \
     "--add-opens", "java.base/java.nio=ALL-UNNAMED", \
     "-Xmx4g", "-Xms2g", \
     "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", \
     "-jar", "fst-poc/target/fst-poc-1.0.0.jar"]
