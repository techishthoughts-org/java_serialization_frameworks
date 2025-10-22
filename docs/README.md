# Documentation Index

Complete documentation for the Java Serialization Frameworks Benchmark project.

## 1. Documentation Structure

### 1.1 Main Documentation
- [Project README](../README.md) - Complete project documentation, setup, and usage

### 1.2 Framework Deep Dives
Each framework has comprehensive documentation covering architecture, implementation, performance characteristics, and best practices.

#### 1.2.1 Text-Based Frameworks
- [Jackson JSON](frameworks/jackson.md) - The ubiquitous JSON serialization library

#### 1.2.2 Binary Schema Frameworks
- [Apache Avro](frameworks/avro.md) - Schema evolution with compact binary format
- [Apache Thrift](frameworks/thrift.md) - Cross-language RPC with IDL
- [SBE (Simple Binary Encoding)](frameworks/sbe.md) - Ultra-low latency financial trading

#### 1.2.3 Binary Schema-less Frameworks
- [Kryo](frameworks/kryo.md) - Fast Java-only serialization
- [FST (Fast Serialization)](frameworks/fst.md) - Drop-in Java serialization replacement
- [MessagePack](frameworks/msgpack.md) - Efficient binary JSON alternative
- [CBOR](frameworks/cbor.md) - Concise Binary Object Representation for IoT
- [BSON](frameworks/bson.md) - Binary JSON for MongoDB

#### 1.2.4 Zero-Copy Frameworks
- [Cap'n Proto](frameworks/capnproto.md) - Zero-copy reads with schema evolution

#### 1.2.5 Columnar Frameworks
- [Apache Arrow](frameworks/arrow.md) - In-memory columnar format for analytics
- [Apache Parquet](frameworks/parquet.md) - Columnar file format for data warehousing

#### 1.2.6 RPC Frameworks
- [gRPC](frameworks/grpc.md) - HTTP/2-based RPC for microservices

### 1.3 Guides
- [Benchmark Results](guides/BENCHMARK_SUMMARY.md) - Complete performance analysis
- [Screenshot Guide](guides/SCREENSHOT_GUIDE.md) - Visual documentation capture
- [Visual Documentation](guides/VISUAL_DOCUMENTATION.md) - ASCII diagrams and examples

### 1.4 Monitoring
- [Grafana Dashboards](../dashboards/README.md) - Performance and resource monitoring setup

## 2. Quick Navigation

### 2.1 By Use Case

#### 2.1.1 Web APIs and REST Services
[Jackson JSON](frameworks/jackson.md) | [CBOR](frameworks/cbor.md) | [MessagePack](frameworks/msgpack.md)

#### 2.1.2 Microservices and RPC
[gRPC](frameworks/grpc.md) | [Apache Thrift](frameworks/thrift.md) | [Apache Avro](frameworks/avro.md)

#### 2.1.3 High-Performance Java
[Kryo](frameworks/kryo.md) | [FST](frameworks/fst.md) | [SBE](frameworks/sbe.md)

#### 2.1.4 Ultra-Low Latency
[SBE](frameworks/sbe.md) | [Cap'n Proto](frameworks/capnproto.md) | [Kryo](frameworks/kryo.md)

#### 2.1.5 Big Data and Analytics
[Apache Arrow](frameworks/arrow.md) | [Apache Parquet](frameworks/parquet.md) | [Apache Avro](frameworks/avro.md)

#### 2.1.6 IoT and Embedded Systems
[CBOR](frameworks/cbor.md) | [MessagePack](frameworks/msgpack.md) | [Apache Avro](frameworks/avro.md)

#### 2.1.7 Schema Evolution
[Apache Avro](frameworks/avro.md) | [Apache Thrift](frameworks/thrift.md) | [Cap'n Proto](frameworks/capnproto.md)

#### 2.1.8 Best Compression
[Apache Parquet](frameworks/parquet.md) | [Apache Avro](frameworks/avro.md) | [MessagePack](frameworks/msgpack.md)

### 2.2 By Performance Tier

#### 2.2.1 Ultra-Fast (Less than 2ms average)
1. [SBE](frameworks/sbe.md) - 0.89ms
2. [Cap'n Proto](frameworks/capnproto.md) - 1.23ms
3. [FST](frameworks/fst.md) - 1.45ms
4. [Kryo](frameworks/kryo.md) - 1.67ms

#### 2.2.2 Very Fast (2-4ms average)
1. [Apache Arrow](frameworks/arrow.md) - 2.01ms
2. [Apache Avro](frameworks/avro.md) - 2.34ms
3. [MessagePack](frameworks/msgpack.md) - 2.89ms
4. [Apache Thrift](frameworks/thrift.md) - 3.12ms
5. [CBOR](frameworks/cbor.md) - 3.45ms
6. [Apache Parquet](frameworks/parquet.md) - 3.78ms

#### 2.2.3 Fast (4-6ms average)
1. [BSON](frameworks/bson.md) - 4.23ms
2. [gRPC](frameworks/grpc.md) - 4.56ms
3. [Jackson JSON](frameworks/jackson.md) - 5.67ms

## 3. Reading Guide

### 3.1 For Beginners
1. Start with [Project README](../README.md)
2. Read [Jackson JSON](frameworks/jackson.md) - most familiar format
3. Explore [Benchmark Results](guides/BENCHMARK_SUMMARY.md)
4. Choose a framework based on your use case

### 3.2 For Performance Optimization
1. Review [Benchmark Results](guides/BENCHMARK_SUMMARY.md)
2. Read deep-dives for top performers:
   - [SBE](frameworks/sbe.md)
   - [Cap'n Proto](frameworks/capnproto.md)
   - [Kryo](frameworks/kryo.md)
3. Check [Grafana Dashboards](../dashboards/README.md) for monitoring

### 3.3 For Architecture Decisions
1. Read [Project README](../README.md) - Framework Selection Guide
2. Compare frameworks by category
3. Review deep-dives for shortlisted frameworks
4. Run benchmarks with your actual data

## 4. Framework Comparison Matrix

| Framework | Speed | Compression | Schema | Cross-Lang | Zero-Copy | Streaming |
|-----------|-------|-------------|--------|------------|-----------|-----------|
| Jackson | 3 stars | 2 stars | No | Yes | No | Yes |
| Avro | 4 stars | 5 stars | Yes | Yes | No | Yes |
| Kryo | 5 stars | 3 stars | No | No | No | No |
| MessagePack | 4 stars | 4 stars | No | Yes | No | Yes |
| Thrift | 4 stars | 3 stars | Yes | Yes | No | Yes |
| Cap'n Proto | 5 stars | 3 stars | Yes | Yes | Yes | Yes |
| FST | 5 stars | 3 stars | No | No | No | No |
| gRPC | 4 stars | 3 stars | Yes | Yes | No | Yes |
| CBOR | 4 stars | 4 stars | No | Yes | No | Yes |
| BSON | 3 stars | 3 stars | No | Yes | No | Yes |
| Arrow | 4 stars | 4 stars | Yes | Yes | Yes | Yes |
| SBE | 5 stars | 4 stars | Yes | No | No | No |
| Parquet | 4 stars | 5 stars | Yes | Yes | No | No |

## 5. Contributing to Documentation

To add or improve documentation:

### 5.1 Framework Deep-Dive
Use existing frameworks as templates

### 5.2 Code Examples
Include working code snippets

### 5.3 Performance Data
Back claims with benchmark results

### 5.4 Use Cases
Provide real-world scenarios

### 5.5 Best Practices
Share lessons learned

## 6. Documentation Feedback

Found an error or have suggestions?
- Open an issue on GitHub
- Submit a pull request
- Contact the maintainers

---

**Last Updated**: 2025-10-22
**Version**: 2.0.0
