#!/bin/bash

# Start 6 framework services for benchmarking

BASE_DIR="/Users/arthurcosta/dev/personal/java_serialization_frameworks"

echo "Starting 6 framework services..."

cd "$BASE_DIR/jackson-poc" && mvn spring-boot:run -DskipTests > /tmp/jackson.log 2>&1 &
echo "Started Jackson (port 8081)"
sleep 2

cd "$BASE_DIR/avro-poc" && mvn spring-boot:run -DskipTests > /tmp/avro.log 2>&1 &
echo "Started Avro (port 8083)"
sleep 2

cd "$BASE_DIR/kryo-poc" && mvn spring-boot:run -DskipTests > /tmp/kryo.log 2>&1 &
echo "Started Kryo (port 8084)"
sleep 2

cd "$BASE_DIR/msgpack-poc" && mvn spring-boot:run -DskipTests > /tmp/msgpack.log 2>&1 &
echo "Started MessagePack (port 8086)"
sleep 2

cd "$BASE_DIR/flatbuffers-poc" && mvn spring-boot:run -DskipTests > /tmp/flatbuffers.log 2>&1 &
echo "Started FlatBuffers (port 8091)"
sleep 2

cd "$BASE_DIR/grpc-poc" && mvn spring-boot:run -DskipTests > /tmp/grpc.log 2>&1 &
echo "Started gRPC (port 8092)"

echo ""
echo "All services starting..."
echo "Wait 60 seconds for initialization..."
sleep 60

echo ""
echo "Checking service status..."
for port in 8081 8083 8084 8086 8091 8092; do
    status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health --max-time 2 2>/dev/null || echo "down")
    if [ "$status" = "200" ]; then
        echo "✅ Port $port: UP"
    else
        echo "❌ Port $port: DOWN"
    fi
done

echo ""
echo "Services ready for benchmarking!"
