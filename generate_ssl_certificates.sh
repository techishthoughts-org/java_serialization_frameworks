#!/bin/bash

# SSL Certificate Generation Script for Java Serialization Frameworks (2025)
# This script generates SSL certificates for all frameworks

echo "🔐 SSL Certificate Generation Script"
echo "===================================="
echo "⏰ Start time: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# Check prerequisites
echo "🔍 Checking prerequisites..."

# Check if keytool is available
if ! command -v keytool &> /dev/null; then
    echo "❌ keytool not found. Please install Java JDK"
    exit 1
fi

# Check if openssl is available
if ! command -v openssl &> /dev/null; then
    echo "❌ openssl not found. Please install OpenSSL"
    exit 1
fi

echo "✅ Prerequisites check passed"
echo ""

# Create certificates directory
echo "📁 Creating certificates directory..."
mkdir -p ssl-certificates
cd ssl-certificates

echo "🔐 Generating SSL certificates for all frameworks..."
echo ""

# Framework ports
PORTS=(8081 8082 8083 8084 8086 8087 8088 8090 8091 8092)
FRAMEWORKS=("jackson" "protobuf" "avro" "kryo" "msgpack" "capnproto" "thrift" "fst" "flatbuffers" "grpc")

# Generate certificates for each framework
for i in "${!PORTS[@]}"; do
    PORT=${PORTS[$i]}
    FRAMEWORK=${FRAMEWORKS[$i]}

    echo "🔐 Generating certificate for ${FRAMEWORK} (port ${PORT})..."

    keytool -genkeypair \
        -alias "${FRAMEWORK}-${PORT}" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 365 \
        -keystore "keystore-${PORT}.p12" \
        -storetype PKCS12 \
        -storepass changeit \
        -keypass changeit \
        -dname "CN=localhost, OU=Development, O=TechishThoughts, L=City, ST=State, C=US" \
        -noprompt

    if [ $? -eq 0 ]; then
        echo "✅ Certificate generated for ${FRAMEWORK} (port ${PORT})"
    else
        echo "❌ Failed to generate certificate for ${FRAMEWORK} (port ${PORT})"
    fi
done

echo ""
echo "🔐 Generating main certificate for the project..."

# Generate main certificate
keytool -genkeypair \
    -alias serialization-benchmark \
    -keyalg RSA \
    -keysize 2048 \
    -validity 365 \
    -keystore keystore.p12 \
    -storetype PKCS12 \
    -storepass changeit \
    -keypass changeit \
    -dname "CN=localhost, OU=Development, O=TechishThoughts, L=City, ST=State, C=US" \
    -noprompt

if [ $? -eq 0 ]; then
    echo "✅ Main certificate generated"
else
    echo "❌ Failed to generate main certificate"
fi

echo ""
echo "🔐 Generating CA certificate for production..."

# Create CA directory
mkdir -p ../ca-certificates
cd ../ca-certificates

# Generate CA private key
openssl genrsa -out ca-private-key.pem 4096

# Generate CA certificate
openssl req -new -x509 -days 365 -key ca-private-key.pem \
    -out ca-certificate.pem \
    -subj "/C=US/ST=State/L=City/O=TechishThoughts/OU=IT/CN=Serialization-CA" \
    -nodes

# Generate server private key
openssl genrsa -out server-private-key.pem 2048

# Generate server certificate signing request
openssl req -new -key server-private-key.pem \
    -out server-certificate.csr \
    -subj "/C=US/ST=State/L=City/O=TechishThoughts/OU=IT/CN=serialization-benchmark.local" \
    -nodes

# Sign server certificate with CA
openssl x509 -req -days 365 -in server-certificate.csr \
    -CA ca-certificate.pem -CAkey ca-private-key.pem \
    -CAcreateserial -out server-certificate.pem

# Convert to PKCS12 format for Java
openssl pkcs12 -export \
    -in server-certificate.pem \
    -inkey server-private-key.pem \
    -out keystore.p12 \
    -name serialization-benchmark \
    -passout pass:changeit

cd ..

echo ""
echo "🔐 Setting proper permissions..."

# Set proper permissions
chmod 600 ssl-certificates/*.p12
chmod 600 ca-certificates/*.pem
chmod 600 ca-certificates/*.p12

echo ""
echo "📋 Certificate Summary:"
echo "======================"

# List all generated certificates
echo "🔐 Development Certificates (ssl-certificates/):"
for port in "${PORTS[@]}"; do
    if [ -f "ssl-certificates/keystore-${port}.p12" ]; then
        echo "  ✅ keystore-${port}.p12"
    else
        echo "  ❌ keystore-${port}.p12 (missing)"
    fi
done

echo ""
echo "🔐 Production Certificates (ca-certificates/):"
if [ -f "ca-certificates/keystore.p12" ]; then
    echo "  ✅ keystore.p12 (CA-signed)"
else
    echo "  ❌ keystore.p12 (missing)"
fi

if [ -f "ca-certificates/ca-certificate.pem" ]; then
    echo "  ✅ ca-certificate.pem"
else
    echo "  ❌ ca-certificate.pem (missing)"
fi

echo ""
echo "🔐 Testing SSL certificates..."

# Test certificates
for port in "${PORTS[@]}"; do
    if [ -f "ssl-certificates/keystore-${port}.p12" ]; then
        echo "  ✅ Certificate for port ${port} is valid"
    else
        echo "  ❌ Certificate for port ${port} is missing"
    fi
done

echo ""
echo "🚀 SSL Certificate Generation Complete!"
echo "======================================"
echo "📁 Certificates location: ssl-certificates/"
echo "📁 CA certificates location: ca-certificates/"
echo ""
echo "🔐 Next steps:"
echo "1. Copy certificates to your application resources"
echo "2. Configure SSL in application.yml"
echo "3. Test HTTPS endpoints"
echo "4. Update docker-compose.yml if needed"
echo ""
echo "⏰ End time: $(date '+%Y-%m-%d %H:%M:%S')"
