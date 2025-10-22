# Documentation Index

Complete documentation for the Java Serialization Frameworks Benchmark project.

## 📚 Documentation Structure

### Main Documentation
- **[Project README](../README.md)** - Complete project documentation, setup, and usage

### Framework Deep Dives
Each framework has comprehensive documentation covering architecture, implementation, performance characteristics, and best practices.

#### Text-Based Frameworks
- **[Jackson JSON](frameworks/jackson.md)** - The ubiquitous JSON serialization library

#### Binary Schema Frameworks
- **[Apache Avro](frameworks/avro.md)** - Schema evolution with compact binary format
- **[Apache Thrift](frameworks/thrift.md)** - Cross-language RPC with IDL
- **[SBE (Simple Binary Encoding)](frameworks/sbe.md)** - Ultra-low latency financial trading

#### Binary Schema-less Frameworks
- **[Kryo](frameworks/kryo.md)** - Fast Java-only serialization
- **[FST (Fast Serialization)](frameworks/fst.md)** - Drop-in Java serialization replacement
- **[MessagePack](frameworks/msgpack.md)** - Efficient binary JSON alternative
- **[CBOR](frameworks/cbor.md)** - Concise Binary Object Representation for IoT
- **[BSON](frameworks/bson.md)** - Binary JSON for MongoDB

#### Zero-Copy Frameworks
- **[Cap'n Proto](frameworks/capnproto.md)** - Zero-copy reads with schema evolution

#### Columnar Frameworks
- **[Apache Arrow](frameworks/arrow.md)** - In-memory columnar format for analytics
- **[Apache Parquet](frameworks/parquet.md)** - Columnar file format for data warehousing

#### RPC Frameworks
- **[gRPC](frameworks/grpc.md)** - HTTP/2-based RPC for microservices

### Guides
- **[Benchmark Results](guides/BENCHMARK_SUMMARY.md)** - Complete performance analysis
- **[Screenshot Guide](guides/SCREENSHOT_GUIDE.md)** - Visual documentation capture
- **[Visual Documentation](guides/VISUAL_DOCUMENTATION.md)** - ASCII diagrams and examples

### Monitoring
- **[Grafana Dashboards](../dashboards/README.md)** - Performance and resource monitoring setup

## 🎯 Quick Navigation

### By Use Case

**Web APIs & REST Services**
→ [Jackson JSON](frameworks/jackson.md) | [CBOR](frameworks/cbor.md) | [MessagePack](frameworks/msgpack.md)

**Microservices & RPC**
→ [gRPC](frameworks/grpc.md) | [Apache Thrift](frameworks/thrift.md) | [Apache Avro](frameworks/avro.md)

**High-Performance Java**
→ [Kryo](frameworks/kryo.md) | [FST](frameworks/fst.md) | [SBE](frameworks/sbe.md)

**Ultra-Low Latency**
→ [SBE](frameworks/sbe.md) | [Cap'n Proto](frameworks/capnproto.md) | [Kryo](frameworks/kryo.md)

**Big Data & Analytics**
→ [Apache Arrow](frameworks/arrow.md) | [Apache Parquet](frameworks/parquet.md) | [Apache Avro](frameworks/avro.md)

**IoT & Embedded**
→ [CBOR](frameworks/cbor.md) | [MessagePack](frameworks/msgpack.md) | [Apache Avro](frameworks/avro.md)

**Schema Evolution**
→ [Apache Avro](frameworks/avro.md) | [Apache Thrift](frameworks/thrift.md) | [Cap'n Proto](frameworks/capnproto.md)

**Best Compression**
→ [Apache Parquet](frameworks/parquet.md) | [Apache Avro](frameworks/avro.md) | [MessagePack](frameworks/msgpack.md)

### By Performance Tier

**⚡ Ultra-Fast (< 2ms avg)**
- [SBE](frameworks/sbe.md) - 0.89ms
- [Cap'n Proto](frameworks/capnproto.md) - 1.23ms
- [FST](frameworks/fst.md) - 1.45ms
- [Kryo](frameworks/kryo.md) - 1.67ms

**🚀 Very Fast (2-4ms avg)**
- [Apache Arrow](frameworks/arrow.md) - 2.01ms
- [Apache Avro](frameworks/avro.md) - 2.34ms
- [MessagePack](frameworks/msgpack.md) - 2.89ms
- [Apache Thrift](frameworks/thrift.md) - 3.12ms
- [CBOR](frameworks/cbor.md) - 3.45ms
- [Apache Parquet](frameworks/parquet.md) - 3.78ms

**⚙️ Fast (4-6ms avg)**
- [BSON](frameworks/bson.md) - 4.23ms
- [gRPC](frameworks/grpc.md) - 4.56ms
- [Jackson JSON](frameworks/jackson.md) - 5.67ms

## 📖 Reading Guide

### For Beginners
1. Start with [Project README](../README.md)
2. Read [Jackson JSON](frameworks/jackson.md) - most familiar format
3. Explore [Benchmark Results](guides/BENCHMARK_SUMMARY.md)
4. Choose a framework based on your use case

### For Performance Optimization
1. Review [Benchmark Results](guides/BENCHMARK_SUMMARY.md)
2. Read deep-dives for top performers:
   - [SBE](frameworks/sbe.md)
   - [Cap'n Proto](frameworks/capnproto.md)
   - [Kryo](frameworks/kryo.md)
3. Check [Grafana Dashboards](../dashboards/README.md) for monitoring

### For Architecture Decisions
1. Read [Project README](../README.md) - Framework Selection Guide
2. Compare frameworks by category
3. Review deep-dives for shortlisted frameworks
4. Run benchmarks with your actual data

## 🔍 Framework Comparison Matrix

| Framework | Speed | Compression | Schema | Cross-Lang | Zero-Copy | Streaming |
|-----------|-------|-------------|--------|------------|-----------|-----------|
| Jackson | ⭐⭐⭐ | ⭐⭐ | ❌ | ✅ | ❌ | ✅ |
| Avro | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ | ✅ | ❌ | ✅ |
| Kryo | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ❌ | ❌ | ❌ | ❌ |
| MessagePack | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ❌ | ✅ | ❌ | ✅ |
| Thrift | ⭐⭐⭐⭐ | ⭐⭐⭐ | ✅ | ✅ | ❌ | ✅ |
| Cap'n Proto | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ✅ | ✅ | ✅ | ✅ |
| FST | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ❌ | ❌ | ❌ | ❌ |
| gRPC | ⭐⭐⭐⭐ | ⭐⭐⭐ | ✅ | ✅ | ❌ | ✅ |
| CBOR | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ❌ | ✅ | ❌ | ✅ |
| BSON | ⭐⭐⭐ | ⭐⭐⭐ | ❌ | ✅ | ❌ | ✅ |
| Arrow | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ | ✅ | ✅ | ✅ |
| SBE | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ | ❌ | ❌ | ❌ |
| Parquet | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ | ✅ | ❌ | ❌ |

## 📝 Contributing to Documentation

To add or improve documentation:

1. **Framework Deep-Dive**: Use existing frameworks as templates
2. **Code Examples**: Include working code snippets
3. **Performance Data**: Back claims with benchmark results
4. **Use Cases**: Provide real-world scenarios
5. **Best Practices**: Share lessons learned

## 📧 Documentation Feedback

Found an error or have suggestions?
- Open an issue on GitHub
- Submit a pull request
- Contact the maintainers

---

**Last Updated**: 2025-10-22
**Version**: 2.0.0
