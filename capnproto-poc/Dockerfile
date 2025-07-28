FROM openjdk:21-jdk-slim

# Install Cap'n Proto compiler
RUN apt-get update && apt-get install -y \
    wget \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install Cap'n Proto
RUN wget https://capnproto.org/capnproto-c++-1.0.2.tar.gz \
    && tar -xzf capnproto-c++-1.0.2.tar.gz \
    && cd capnproto-c++-1.0.2 \
    && ./configure \
    && make -j$(nproc) \
    && make install \
    && cd .. \
    && rm -rf capnproto-c++-1.0.2*

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY capnproto-poc/pom.xml capnproto-poc/

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY capnproto-poc/src capnproto-poc/src

# Build the application
RUN ./mvnw clean package -pl capnproto-poc -am -DskipTests

# Create runtime image
FROM openjdk:21-jre-slim

# Install Cap'n Proto runtime
RUN apt-get update && apt-get install -y \
    libcapnp-0.10.2 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR
COPY --from=0 /app/capnproto-poc/target/*.jar app.jar

# Expose port
EXPOSE 8087

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8087/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
