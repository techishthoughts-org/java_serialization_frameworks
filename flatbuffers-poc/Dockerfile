FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY flatbuffers-poc/pom.xml flatbuffers-poc/

# Copy source code
COPY flatbuffers-poc/src flatbuffers-poc/src
COPY common-payload/src common-payload/src

# Install Maven, curl, and FlatBuffers compiler
RUN apt-get update && apt-get install -y maven curl wget unzip
RUN wget -O /tmp/flatc.zip https://github.com/google/flatbuffers/releases/download/v25.2.10/Linux.flatc.binary.clang++-12.zip && \
    unzip /tmp/flatc.zip -d /usr/local/bin && \
    chmod +x /usr/local/bin/flatc && \
    rm /tmp/flatc.zip

# Build the application
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8091

# Run the application
CMD ["java", "-jar", "flatbuffers-poc/target/flatbuffers-poc-1.0.0.jar"]
