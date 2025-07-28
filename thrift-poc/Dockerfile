FROM openjdk:21-jdk-slim

# Install Apache Thrift compiler
RUN apt-get update && apt-get install -y \
    wget \
    build-essential \
    libboost-dev \
    libevent-dev \
    libssl-dev \
    libtool \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

# Install Apache Thrift
RUN wget https://archive.apache.org/dist/thrift/0.20.0/thrift-0.20.0.tar.gz \
    && tar -xzf thrift-0.20.0.tar.gz \
    && cd thrift-0.20.0 \
    && ./configure --with-java \
    && make -j$(nproc) \
    && make install \
    && cd .. \
    && rm -rf thrift-0.20.0*

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY thrift-poc/pom.xml thrift-poc/

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY thrift-poc/src thrift-poc/src

# Build the application
RUN ./mvnw clean package -pl thrift-poc -am -DskipTests

# Create runtime image
FROM openjdk:21-jre-slim

WORKDIR /app

# Copy the built JAR
COPY --from=0 /app/thrift-poc/target/*.jar app.jar

# Expose port
EXPOSE 8088

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8088/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
