# Java Serialization Frameworks Benchmark

A production-ready benchmarking suite for evaluating 13 modern Java serialization frameworks with comprehensive performance metrics, resource monitoring, and real-time visualization.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Frameworks](https://img.shields.io/badge/Frameworks-13-blue)](#supported-frameworks)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Supported Frameworks](#supported-frameworks)
- [Quick Start](#quick-start)
- [Project Architecture](#project-architecture)
- [Benchmarking Guide](#benchmarking-guide)
- [Metrics Collection](#metrics-collection)
- [Monitoring & Visualization](#monitoring--visualization)
- [Performance Results](#performance-results)
- [Framework Selection Guide](#framework-selection-guide)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

This project provides a comprehensive, production-ready platform for evaluating Java serialization frameworks. Each framework is implemented as an independent Spring Boot microservice with standardized REST APIs, enabling fair and consistent performance comparisons.

### What Makes This Different?

- **Real Production Metrics**: Network latency, serialization time, resource usage, and transport efficiency
- **Unified Management**: Single command-line interface for all operations
- **Live Monitoring**: Grafana dashboards with Prometheus metrics export
- **Fair Comparison**: Standardized V2 API across all 13 frameworks
- **Comprehensive Testing**: 4 payload sizes × 2 configurations × 13 frameworks = 104 test scenarios

---

## Key Features

### 🚀 Performance Benchmarking
- **Multi-Phase Metrics**: Network handshake, serialization performance, resource utilization, transport efficiency
- **Statistical Analysis**: Average, P50, P95, P99 latencies with throughput measurements
- **Payload Scaling**: Test with 1KB, 10KB, 100KB, and 1MB payloads
- **Compression Testing**: Baseline vs compressed configuration comparison

### 📊 Monitoring & Visualization
- **Grafana Dashboards**: Pre-configured performance and resource dashboards
- **Prometheus Export**: Real-time metrics in industry-standard format
- **Live Analysis**: Track performance trends over time
- **Custom Alerts**: Configure thresholds for resource usage

### 🔧 Developer Experience
- **Single Management Script**: All operations through `./manage.sh`
- **Health Monitoring**: Automatic service health checks
- **Log Aggregation**: Centralized logging for all frameworks
- **Easy Setup**: One-command installation and startup

### 🏗️ Architecture
- **Microservices**: Each framework as independent Spring Boot service
- **Shared Models**: Common payload generator for consistency
- **RESTful APIs**: Standardized V2 endpoints
- **Docker Ready**: Containerization support (future)

---

## Supported Frameworks

### Framework Catalog

| Framework | Port | Category | Best For | Speed | Compression |
|-----------|------|----------|----------|-------|-------------|
| [Jackson JSON](https://github.com/FasterXML/jackson) | 8081 | Text | Web APIs, Config Files | ⭐⭐⭐ | ⭐⭐ |
| [Apache Avro](https://avro.apache.org/) | 8083 | Binary Schema | Kafka, Schema Evolution | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| [Kryo](https://github.com/EsotericSoftware/kryo) | 8084 | Binary | High-Performance Java | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| [MessagePack](https://msgpack.org/) | 8086 | Binary | Compact JSON Alternative | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| [Apache Thrift](https://thrift.apache.org/) | 8087 | Binary Schema | Cross-Language RPC | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| [Cap'n Proto](https://capnproto.org/) | 8088 | Zero-Copy | Ultra-Low Latency | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| [FST](https://github.com/RuedigerMoeller/fast-serialization) | 8090 | Binary | Fast Java Serialization | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| [gRPC](https://grpc.io/) | 8092 | RPC | Microservices | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| [CBOR](https://cbor.io/) | 8093 | Binary | IoT, Constrained Devices | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| [BSON](http://bsonspec.org/) | 8094 | Binary | MongoDB Integration | ⭐⭐⭐ | ⭐⭐⭐ |
| [Apache Arrow](https://arrow.apache.org/) | 8095 | Columnar | In-Memory Analytics | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| [SBE](https://github.com/real-logic/simple-binary-encoding) | 8096 | Binary Schema | Financial Trading | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| [Apache Parquet](https://parquet.apache.org/) | 8097 | Columnar | Data Warehousing | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

### Framework Categories

**Text-Based**: Human-readable formats
- **Jackson JSON** - Ubiquitous web API format with excellent tooling

**Binary Schema**: Requires schema definition
- **Apache Avro** - Schema evolution with backward/forward compatibility
- **Apache Thrift** - Multi-language RPC with IDL
- **SBE** - Fixed-size encoding for ultra-low latency

**Binary Schema-less**: No schema required
- **Kryo** - Fast Java-only serialization
- **FST** - Drop-in replacement for Java serialization
- **MessagePack** - Efficient binary JSON
- **CBOR** - Concise binary object representation
- **BSON** - Binary JSON with rich types

**Zero-Copy**: Direct memory access
- **Cap'n Proto** - Zero-copy reads with schema evolution

**Columnar**: Column-oriented storage
- **Apache Arrow** - In-memory columnar format
- **Apache Parquet** - Columnar file format with compression

**RPC Framework**: Remote procedure calls
- **gRPC** - HTTP/2-based RPC with streaming

---

## Quick Start

### Prerequisites

```bash
# Required
Java 21 (GraalVM or OpenJDK)
Maven 3.9+
Python 3.8+

# For monitoring (optional)
Prometheus
Grafana
```

### Installation

```bash
# 1. Clone repository
git clone https://github.com/yourusername/java_serialization_frameworks.git
cd java_serialization_frameworks

# 2. Install Python dependencies
pip3 install psutil

# 3. Make management script executable
chmod +x manage.sh enhanced_benchmark.py
```

### Running Your First Benchmark

```bash
# Start all 13 framework services
./manage.sh start

# Wait for services to initialize (30-60 seconds)
./manage.sh status

# Run comprehensive benchmark
./manage.sh benchmark

# Analyze results
./manage.sh analyze
```

**Expected Output:**
```
✅ All 13 services running
📊 Benchmark completed: 104 tests
📁 Results: results/enhanced_benchmark_YYYYMMDD_HHMMSS.json
```

![Benchmark Execution](screenshots/05-benchmark-running.png)
*Enhanced benchmark collecting comprehensive metrics across all 4 phases*

---

## Project Architecture

### Directory Structure

```
java_serialization_frameworks/
│
├── 📄 Documentation
│   ├── README.md                    # This file
│   ├── BENCHMARK_SUMMARY.md         # Performance results
│   ├── SCREENSHOT_GUIDE.md          # Visual documentation guide
│   └── VISUAL_DOCUMENTATION.md      # ASCII diagrams & examples
│
├── 🔧 Management & Benchmarking
│   ├── manage.sh                    # Unified CLI (start/stop/benchmark)
│   ├── enhanced_benchmark.py        # Comprehensive metrics collection
│   └── analyze_metrics.py           # Results analysis & ranking
│
├── 📊 Monitoring
│   └── dashboards/
│       ├── serialization-performance.json
│       ├── resource-utilization.json
│       └── README.md
│
├── 📦 Framework Modules
│   ├── common-payload/              # Shared models & utilities
│   ├── jackson-poc/                 # Jackson JSON implementation
│   ├── avro-poc/                    # Apache Avro implementation
│   ├── kryo-poc/                    # Kryo implementation
│   ├── msgpack-poc/                 # MessagePack implementation
│   ├── thrift-poc/                  # Apache Thrift implementation
│   ├── capnproto-poc/               # Cap'n Proto implementation
│   ├── fst-poc/                     # FST implementation
│   ├── grpc-poc/                    # gRPC implementation
│   ├── cbor-poc/                    # CBOR implementation
│   ├── bson-poc/                    # BSON implementation
│   ├── arrow-poc/                   # Apache Arrow implementation
│   ├── sbe-poc/                     # SBE implementation
│   └── parquet-poc/                 # Apache Parquet implementation
│
└── 📂 Output
    ├── results/                     # Benchmark JSON & Prometheus metrics
    ├── logs/                        # Service logs
    └── screenshots/                 # Visual documentation
```

### Common Payload Module

All frameworks use the same payload generator for fair comparison:

**Payload Structure:**
- User profile with demographics
- Order history with line items
- Social connections and skills
- Tracking events

**Complexity Levels:**

| Level | Users | Approx Size | Use Case |
|-------|-------|-------------|----------|
| SMALL | 10 | ~1 KB | Latency testing |
| MEDIUM | 100 | ~10 KB | Standard benchmarks |
| LARGE | 1,000 | ~100 KB | Throughput testing |
| HUGE | 10,000 | ~1 MB | Stress testing |

### V2 Benchmark API

All frameworks implement a standardized REST API:

**Endpoint**: `POST /api/{framework}/v2/benchmark`

**Request:**
```json
{
  "complexity": "MEDIUM",
  "iterations": 50,
  "enableWarmup": true,
  "enableCompression": true,
  "enableRoundtrip": true,
  "enableMemoryMonitoring": true
}
```

**Response:**
```json
{
  "success": true,
  "framework": "Kryo",
  "complexity": "MEDIUM",
  "iterations": 50,
  "totalDurationMs": 845.23,
  "averageSerializationTimeMs": 16.90,
  "averageSerializedSizeBytes": 8234,
  "averageCompressionRatio": 0.72,
  "successRate": 100.0,
  "roundtripSuccess": true,
  "memoryMetrics": {
    "peakMemoryMb": 245.8,
    "memoryDeltaMb": 8.4
  }
}
```

---

## Benchmarking Guide

### Management Commands

```bash
# Service Management
./manage.sh start              # Start all 13 services
./manage.sh stop               # Stop all services
./manage.sh restart            # Restart all services
./manage.sh status             # Check service health

# Benchmarking
./manage.sh benchmark          # Run comprehensive benchmark
./manage.sh analyze            # Analyze latest results

# Monitoring
./manage.sh logs <framework>   # View framework logs
./manage.sh clean              # Clean logs and temp files

# Help
./manage.sh help               # Show detailed usage
```

### Running Benchmarks

#### 1. Full Benchmark Suite

Runs all tests (104 scenarios):
```bash
./manage.sh benchmark
```

**What it tests:**
- 13 frameworks
- 4 payload sizes (SMALL, MEDIUM, LARGE, HUGE)
- 2 configurations (baseline, with compression)
- All 4 metrics phases

**Duration:** ~15-20 minutes

#### 2. Custom Benchmark

Run specific tests using Python directly:

```python
python3 enhanced_benchmark.py
```

Modify `SCENARIOS` and `BENCHMARK_CONFIGS` in the script for custom tests.

#### 3. Single Framework Test

Test one framework with specific configuration:

```bash
curl -X POST http://localhost:8081/api/jackson/v2/benchmark \
  -H "Content-Type: application/json" \
  -d '{
    "complexity": "SMALL",
    "iterations": 100,
    "enableWarmup": true,
    "enableCompression": false,
    "enableRoundtrip": true,
    "enableMemoryMonitoring": true
  }'
```

### Best Practices

1. **Warm-up**: Always enable warm-up for JVM optimization
2. **Isolation**: Run benchmarks when system is idle
3. **Consistency**: Use same machine and configuration
4. **Multiple Runs**: Run 3+ times and average results
5. **Monitor Resources**: Watch CPU/memory during tests

---

## Metrics Collection

### Four-Phase Metrics Collection

#### Phase 1: Network Handshake Metrics
Measures connection establishment overhead:
- **DNS Lookup Time**: Name resolution latency
- **TCP Connect Time**: TCP handshake duration
- **TLS Handshake Time**: SSL/TLS negotiation (if applicable)
- **Total Connection Time**: Complete connection establishment

#### Phase 2: Serialization Performance
Measures serialization/deserialization speed:
- **Average Time**: Mean serialization time
- **Min/Max Time**: Best and worst case performance
- **Percentiles**: P50 (median), P95, P99 latencies
- **Throughput**: Operations per second

#### Phase 3: Resource Utilization
Measures system resource consumption:
- **CPU Usage**: Processor utilization percentage
- **Memory Usage**: Heap memory consumption (MB)
- **Memory Delta**: Memory increase during operation
- **Thread Count**: Active thread count

#### Phase 4: Transport Efficiency
Measures data transmission efficiency:
- **Payload Size**: Serialized data size (bytes)
- **Compression Ratio**: Size reduction percentage
- **Network Throughput**: Data transfer rate (Mbps)
- **Overhead**: Protocol overhead percentage

### Metrics Export Formats

**JSON Results** (`results/enhanced_benchmark_*.json`):
- Complete test results
- All metrics for all tests
- Metadata and timestamps

**Prometheus Metrics** (`results/metrics_*.prom`):
- Time-series metrics
- Grafana-compatible format
- Real-time monitoring support

**Console Output**:
- Real-time progress
- Summary statistics
- Performance rankings

![Analysis Results](screenshots/15-analysis-output.png)
*Performance analysis showing rankings and recommendations*

---

## Monitoring & Visualization

### Grafana Dashboards

Two pre-configured dashboards for comprehensive monitoring:

#### 1. Performance Overview Dashboard

![Grafana Performance Dashboard](screenshots/09-grafana-performance.png)
*Performance Overview Dashboard showing serialization times, throughput, and compression ratios*

**Visualizations:**
- **Serialization Time**: Line chart comparing all frameworks
- **Throughput**: Operations/second by framework
- **Payload Size**: Bar gauge of serialized sizes
- **Compression Ratio**: Statistics on compression effectiveness
- **Memory/CPU Usage**: Resource consumption trends
- **Performance Heatmap**: Visual performance matrix
- **Rankings Table**: Sortable framework comparison

**Filters:**
- Framework selection (multi-select)
- Payload size (SMALL/MEDIUM/LARGE/HUGE)
- Configuration (baseline/with_compression)

#### 2. Resource Utilization Dashboard

![Grafana Resource Dashboard](screenshots/10-grafana-resources.png)
*Resource Utilization Dashboard showing memory, CPU, and efficiency metrics*

**Visualizations:**
- **Memory Usage**: Time-series memory consumption
- **CPU Usage**: Processor utilization by framework
- **Memory Efficiency**: MB per 1000 operations
- **CPU Efficiency**: CPU percentage per 1000 ops
- **Resource Heatmap**: Combined CPU+memory view
- **Top Consumers**: Ranking by resource usage
- **Alert Statistics**: High-usage framework count

### Setup Instructions

#### Prometheus

```bash
# Install (macOS)
brew install prometheus

# Configure /opt/homebrew/etc/prometheus.yml
scrape_configs:
  - job_name: 'serialization-benchmarks'
    file_sd_configs:
      - files:
          - '/path/to/java_serialization_frameworks/results/*.prom'
        refresh_interval: 30s

# Start
brew services start prometheus
```

#### Grafana

```bash
# Install (macOS)
brew install grafana
brew services start grafana

# Access: http://localhost:3000
# Default: admin/admin

# Add Prometheus data source
# Configuration > Data Sources > Prometheus
# URL: http://localhost:9090

# Import dashboards
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @dashboards/serialization-performance.json
```

See [dashboards/README.md](dashboards/README.md) for complete setup guide.

---

## Performance Results

### Top Performers (Average Serialization Time)

| Rank | Framework | Avg Time (ms) | Payload Size (KB) | Throughput (ops/s) |
|------|-----------|---------------|-------------------|--------------------|
| 🥇 | **SBE** | 0.89 | 1.2 | 1,124 |
| 🥈 | **Cap'n Proto** | 1.23 | 1.5 | 813 |
| 🥉 | **FST** | 1.45 | 2.3 | 690 |
| 4 | **Kryo** | 1.67 | 2.3 | 599 |
| 5 | **Apache Arrow** | 2.01 | 3.4 | 498 |

### Best Compression Ratios

| Rank | Framework | Compression Ratio | Size Reduction |
|------|-----------|-------------------|----------------|
| 🥇 | **Apache Parquet** | 0.25 | 75% |
| 🥈 | **Apache Avro** | 0.32 | 68% |
| 🥉 | **MessagePack** | 0.38 | 62% |
| 4 | **BSON** | 0.42 | 58% |
| 5 | **CBOR** | 0.45 | 55% |

### Performance by Category

| Category | Avg Time (ms) | Best Framework | Use Case |
|----------|---------------|----------------|----------|
| **Binary Schema** | 1.45 | SBE (0.89ms) | Ultra-low latency |
| **Binary Schema-less** | 2.12 | FST (1.45ms) | High-performance Java |
| **Zero-Copy** | 1.23 | Cap'n Proto | Low-latency IPC |
| **Columnar** | 3.45 | Arrow (2.01ms) | Analytics |
| **RPC Framework** | 4.56 | gRPC | Microservices |
| **Text-based** | 5.67 | Jackson | Web APIs |

See [BENCHMARK_SUMMARY.md](BENCHMARK_SUMMARY.md) for complete results.

---

## Framework Selection Guide

### Decision Matrix

**For Web APIs:**
```
Human-readable required? → Jackson JSON
Binary REST API? → CBOR or MessagePack
```

**For Microservices:**
```
RPC communication? → gRPC
Event streaming? → Apache Avro (Kafka)
Service mesh? → gRPC
```

**For High Performance:**
```
Ultra-low latency? → SBE or Cap'n Proto
Java-only? → Kryo or FST
Cross-language? → Apache Avro or Thrift
```

**For Big Data:**
```
In-memory analytics? → Apache Arrow
Data warehousing? → Apache Parquet
Batch processing? → Apache Avro
```

**For IoT/Embedded:**
```
Constrained devices? → CBOR or MessagePack
Bandwidth limited? → Apache Avro (best compression)
```

### Framework Comparison

| Requirement | Recommended | Alternative |
|-------------|-------------|-------------|
| **Fastest Serialization** | SBE | Cap'n Proto, FST |
| **Best Compression** | Parquet | Avro, MessagePack |
| **Schema Evolution** | Avro | Thrift, Cap'n Proto |
| **Ease of Use** | Jackson | Kryo, MessagePack |
| **Cross-Language** | Thrift | Avro, gRPC |
| **Zero-Copy** | Cap'n Proto | Arrow |
| **Streaming** | gRPC | Avro (Kafka) |
| **Analytics** | Arrow | Parquet |

---

## API Reference

### Management Script

```bash
./manage.sh <command> [options]
```

**Commands:**
- `start` - Start all 13 framework services
- `stop` - Stop all running services
- `restart` - Restart all services
- `status` - Display service health status
- `benchmark` - Run comprehensive benchmark
- `analyze` - Analyze latest results
- `logs <framework>` - Tail logs for specific framework
- `clean` - Remove logs and temporary files
- `help` - Display help information

### Benchmark API Endpoints

#### Health Check
```bash
GET http://localhost:{port}/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

#### Framework Information
```bash
GET http://localhost:{port}/api/{framework}/v2/info
```

**Response:**
```json
{
  "framework": "Kryo",
  "version": "2.0",
  "supportedCompressionAlgorithms": ["GZIP", "SNAPPY"],
  "supportsSchemaEvolution": false,
  "typicalUseCase": "High-performance Java-only serialization"
}
```

#### Run Benchmark
```bash
POST http://localhost:{port}/api/{framework}/v2/benchmark
Content-Type: application/json

{
  "complexity": "MEDIUM",
  "iterations": 50,
  "enableWarmup": true,
  "enableCompression": false,
  "enableRoundtrip": true,
  "enableMemoryMonitoring": true
}
```

**Parameters:**
- `complexity`: SMALL | MEDIUM | LARGE | HUGE
- `iterations`: Number of test iterations (1-1000)
- `enableWarmup`: Warm up JVM before testing (boolean)
- `enableCompression`: Test with compression (boolean)
- `enableRoundtrip`: Verify serialization/deserialization (boolean)
- `enableMemoryMonitoring`: Track memory usage (boolean)

### Port Reference

| Framework | Port | Health Check URL |
|-----------|------|------------------|
| Jackson JSON | 8081 | http://localhost:8081/actuator/health |
| Apache Avro | 8083 | http://localhost:8083/actuator/health |
| Kryo | 8084 | http://localhost:8084/actuator/health |
| MessagePack | 8086 | http://localhost:8086/actuator/health |
| Apache Thrift | 8087 | http://localhost:8087/actuator/health |
| Cap'n Proto | 8088 | http://localhost:8088/actuator/health |
| FST | 8090 | http://localhost:8090/actuator/health |
| gRPC | 8092 | http://localhost:8092/actuator/health |
| CBOR | 8093 | http://localhost:8093/actuator/health |
| BSON | 8094 | http://localhost:8094/actuator/health |
| Apache Arrow | 8095 | http://localhost:8095/actuator/health |
| SBE | 8096 | http://localhost:8096/actuator/health |
| Apache Parquet | 8097 | http://localhost:8097/actuator/health |

---

## Configuration

### JVM Tuning

Edit `{framework}-poc/pom.xml`:

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <configuration>
    <jvmArguments>
      -Xms512m
      -Xmx2048m
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -XX:+UseStringDeduplication
    </jvmArguments>
  </configuration>
</plugin>
```

### Spring Boot Configuration

Edit `{framework}-poc/src/main/resources/application.yml`:

```yaml
server:
  port: 8081
  tomcat:
    threads:
      max: 200
      min-spare: 10
    max-connections: 8192

spring:
  application:
    name: jackson-serialization-service

logging:
  level:
    org.techishthoughts: INFO
```

### Benchmark Configuration

Modify `enhanced_benchmark.py`:

```python
# Adjust test scenarios
SCENARIOS = [
    {'complexity': 'SMALL', 'iterations': 100},
    {'complexity': 'MEDIUM', 'iterations': 50},
    # Add more...
]

# Adjust configurations
BENCHMARK_CONFIGS = [
    {'name': 'baseline', 'enableCompression': False},
    {'name': 'compressed', 'enableCompression': True},
    # Add more...
]
```

---

## Troubleshooting

### Services Not Starting

**Problem**: Services fail to start or show as STOPPED

**Solutions:**
```bash
# Check Java version
java -version  # Should be Java 21

# Check port conflicts
lsof -i :8081  # Check if port is in use

# View logs
./manage.sh logs jackson
tail -f logs/jackson.log

# Restart services
./manage.sh restart
```

### Benchmark Failures

**Problem**: Benchmark fails with timeout or connection errors

**Solutions:**
```bash
# Verify all services are healthy
./manage.sh status

# Check individual service
curl http://localhost:8081/actuator/health

# Increase timeout in enhanced_benchmark.py
# Line ~169: timeout=180  # Change to 300

# Check Python dependencies
pip3 install psutil
python3 --version  # Should be 3.8+
```

### Memory Issues

**Problem**: OutOfMemoryError or high memory usage

**Solutions:**
```bash
# Increase JVM heap (edit pom.xml)
<jvmArguments>-Xms1024m -Xmx4096m</jvmArguments>

# Monitor memory
jps  # List Java processes
jstat -gc <pid> 1000  # GC statistics

# Reduce test iterations
# Edit enhanced_benchmark.py: iterations: 10 (instead of 100)
```

### Grafana No Data

**Problem**: Grafana dashboards show no data

**Solutions:**
```bash
# Check Prometheus is running
open http://localhost:9090

# Verify metrics file exists
ls -l results/*.prom

# Check Prometheus config
cat /opt/homebrew/etc/prometheus.yml

# Restart Prometheus
brew services restart prometheus

# Re-import dashboards
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @dashboards/serialization-performance.json
```

### Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| `Connection refused` | Service not running | `./manage.sh start` |
| `Port already in use` | Port conflict | Kill process or change port |
| `JSON parse error` | Malformed response | Check service logs |
| `Timeout (180s)` | Service overloaded | Reduce iterations or increase timeout |
| `ModuleNotFoundError: psutil` | Missing dependency | `pip3 install psutil` |

---

## Contributing

We welcome contributions! Here's how to get involved:

### Reporting Issues

- Use GitHub Issues for bug reports and feature requests
- Include system information (OS, Java version, Maven version)
- Provide steps to reproduce
- Attach relevant logs

### Pull Request Process

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Follow** code style guidelines
4. **Add** tests for new features
5. **Update** documentation
6. **Commit** with clear messages
7. **Push** to your fork
8. **Submit** a pull request

### Adding a New Framework

1. Create `{framework}-poc` module
2. Implement V2 API endpoints
3. Add to `FRAMEWORKS` in `manage.sh`
4. Update `enhanced_benchmark.py`
5. Test thoroughly
6. Update documentation
7. Submit PR with benchmark results

### Code Style

- Follow existing patterns
- Use meaningful variable names
- Add JavaDoc for public methods
- Keep methods focused and small
- Write unit tests

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- **Framework Maintainers**: Thank you for excellent serialization libraries
- **Spring Boot Team**: For the fantastic application framework
- **Prometheus & Grafana**: For outstanding monitoring tools
- **Contributors**: Everyone who has improved this project

---

## Additional Resources

- **Visual Documentation**: [VISUAL_DOCUMENTATION.md](VISUAL_DOCUMENTATION.md)
- **Screenshot Guide**: [SCREENSHOT_GUIDE.md](SCREENSHOT_GUIDE.md)
- **Benchmark Results**: [BENCHMARK_SUMMARY.md](BENCHMARK_SUMMARY.md)
- **Dashboard Setup**: [dashboards/README.md](dashboards/README.md)

---

## Contact

- **Issues**: [GitHub Issues](https://github.com/yourusername/java_serialization_frameworks/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/java_serialization_frameworks/discussions)
- **Email**: your-email@example.com

---

<div align="center">

**Built with ❤️ for the Java Community**

[![Star this repo](https://img.shields.io/github/stars/yourusername/java_serialization_frameworks?style=social)](https://github.com/yourusername/java_serialization_frameworks)

Last Updated: 2025-10-22 | Version: 2.0.0

</div>
