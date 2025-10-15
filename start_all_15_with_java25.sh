#!/bin/bash

# Start all 15 framework services using Java 25

export JAVA_HOME="$HOME/.sdkman/candidates/java/25-tem"
export PATH="$JAVA_HOME/bin:$PATH"

echo "🔧 Using Java: $(java -version 2>&1 | head -1)"
echo ""

BASE_DIR="/Users/arthurcosta/dev/personal/java_serialization_frameworks"

echo "🚀 Starting all 15 framework services..."
echo "⏱️  This will take approximately 2-3 minutes..."
echo ""

# Start each service individually
echo "Starting Jackson (port 8081)..."
cd "$BASE_DIR/jackson-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/jackson-poc.log 2>&1 &
sleep 1

echo "Starting Protobuf (port 8082)..."
cd "$BASE_DIR/protobuf-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/protobuf-poc.log 2>&1 &
sleep 1

echo "Starting Avro (port 8083)..."
cd "$BASE_DIR/avro-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/avro-poc.log 2>&1 &
sleep 1

echo "Starting Kryo (port 8084)..."
cd "$BASE_DIR/kryo-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/kryo-poc.log 2>&1 &
sleep 1

echo "Starting MessagePack (port 8086)..."
cd "$BASE_DIR/msgpack-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/msgpack-poc.log 2>&1 &
sleep 1

echo "Starting Thrift (port 8087)..."
cd "$BASE_DIR/thrift-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/thrift-poc.log 2>&1 &
sleep 1

echo "Starting Cap'n Proto (port 8088)..."
cd "$BASE_DIR/capnproto-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/capnproto-poc.log 2>&1 &
sleep 1

echo "Starting FST (port 8090)..."
cd "$BASE_DIR/fst-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/fst-poc.log 2>&1 &
sleep 1

echo "Starting FlatBuffers (port 8091)..."
cd "$BASE_DIR/flatbuffers-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/flatbuffers-poc.log 2>&1 &
sleep 1

echo "Starting gRPC (port 8092)..."
cd "$BASE_DIR/grpc-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/grpc-poc.log 2>&1 &
sleep 1

echo "Starting CBOR (port 8093)..."
cd "$BASE_DIR/cbor-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/cbor-poc.log 2>&1 &
sleep 1

echo "Starting BSON (port 8094)..."
cd "$BASE_DIR/bson-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/bson-poc.log 2>&1 &
sleep 1

echo "Starting Arrow (port 8095)..."
cd "$BASE_DIR/arrow-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/arrow-poc.log 2>&1 &
sleep 1

echo "Starting SBE (port 8096)..."
cd "$BASE_DIR/sbe-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/sbe-poc.log 2>&1 &
sleep 1

echo "Starting Parquet (port 8097)..."
cd "$BASE_DIR/parquet-poc" && nohup mvn spring-boot:run -DskipTests > /tmp/parquet-poc.log 2>&1 &

echo ""
echo "📦 All services starting in background..."
echo "⏳ Waiting 120 seconds for initialization..."
sleep 120

echo ""
echo "🔍 Checking service status..."
echo "------------------------------------------------"

check_service() {
    local name=$1
    local port=$2
    local status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health --max-time 3 2>/dev/null || echo "down")
    if [ "$status" = "200" ]; then
        echo "✅ $name (port $port): UP"
        return 0
    else
        echo "❌ $name (port $port): DOWN"
        return 1
    fi
}

up_count=0
check_service "Jackson" 8081 && ((up_count++))
check_service "Protobuf" 8082 && ((up_count++))
check_service "Avro" 8083 && ((up_count++))
check_service "Kryo" 8084 && ((up_count++))
check_service "MessagePack" 8086 && ((up_count++))
check_service "Thrift" 8087 && ((up_count++))
check_service "Cap'n Proto" 8088 && ((up_count++))
check_service "FST" 8090 && ((up_count++))
check_service "FlatBuffers" 8091 && ((up_count++))
check_service "gRPC" 8092 && ((up_count++))
check_service "CBOR" 8093 && ((up_count++))
check_service "BSON" 8094 && ((up_count++))
check_service "Arrow" 8095 && ((up_count++))
check_service "SBE" 8096 && ((up_count++))
check_service "Parquet" 8097 && ((up_count++))

echo "------------------------------------------------"
echo "📊 Status: $up_count/15 services UP"
echo ""

if [ $up_count -eq 15 ]; then
    echo "🎉 All services running! Ready to benchmark!"
    echo "💡 Run: python3 simple_benchmark.py"
else
    echo "⚠️  Some services failed to start"
    echo "💡 Check logs: tail -f /tmp/{framework}-poc.log"
fi
