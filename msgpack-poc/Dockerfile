FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the JAR file
COPY target/msgpack-poc-1.0.0.jar app.jar

# Expose the application port
EXPOSE 8090

# Set JVM options for optimal performance
ENV JVM_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"

# Run the application
CMD ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
