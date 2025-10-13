# JMH Microbenchmarks for Serialization Frameworks

## Purpose

This module provides **pure JVM microbenchmarks** using JMH (Java Microbenchmark Harness) to measure the **true serialization performance** of each framework without HTTP/network overhead.

## Why JMH?

**Integration Tests** (microservices + Python) measure **real-world** performance including:
- HTTP request/response overhead
- Network latency
- Spring Boot container overhead

**JMH Benchmarks** measure **pure algorithm** performance:
- Raw serialization speed (bytes/second)
- JVM warmup effects
- GC impact
- Nanosecond precision

## Quick Start

### Build

```bash
mvn clean package
```

This creates `target/benchmarks.jar` containing all benchmarks.

### Run All Benchmarks

```bash
java -jar target/benchmarks.jar
```

### Run Specific Framework

```bash
java -jar target/benchmarks.jar JacksonBenchmark
java -jar target/benchmarks.jar KryoBenchmark
```

### Run with Custom Options

```bash
# More iterations for statistical confidence
java -jar target/benchmarks.jar -wi 10 -i 20

# Export results to JSON
java -jar target/benchmarks.jar -rf json -rff jmh-results.json

# Run only serialization benchmarks
java -jar target/benchmarks.jar ".*serialize$"
```

## Benchmark Structure

Each framework has three benchmarks:

1. **serialize**: Measure serialization speed (objects → bytes)
2. **deserialize**: Measure deserialization speed (bytes → objects)
3. **roundtrip**: Measure full cycle (serialize + deserialize)

## Interpreting Results

### Sample Output

```
Benchmark                    Mode  Cnt    Score     Error  Units
JacksonBenchmark.serialize  thrpt   20  15234.2 ± 543.1  ops/s
JacksonBenchmark.deserialize thrpt   20  12456.7 ± 432.2  ops/s
KryoBenchmark.serialize     thrpt   20  28456.7 ± 892.3  ops/s
KryoBenchmark.deserialize   thrpt   20  25123.4 ± 765.4  ops/s
```

**Mode**: `thrpt` = Throughput (higher is better)
**Cnt**: Number of measurement iterations
**Score**: Operations per second
**Error**: 99.9% confidence interval

### Key Metrics

- **ops/s**: Operations per second (throughput)
- **ms/op**: Milliseconds per operation (latency)
- **±**: Confidence interval (measure of variance)

## Comparison: JMH vs Integration Tests

| Metric | JMH (Pure) | Integration (Real-World) | Delta (HTTP Overhead) |
|--------|-----------|--------------------------|------------------------|
| Jackson | ~3.2ms | ~9.3ms | ~6.1ms (191%) |
| Kryo | ~1.8ms | ~5.6ms | ~3.8ms (211%) |

**Key Insight**: HTTP overhead is **~2-3x** the pure serialization time.

## Advanced Usage

### Profile with JFR

```bash
java -jar target/benchmarks.jar -prof jfr
```

### GC Profiling

```bash
java -jar target/benchmarks.jar -prof gc
```

### Custom JVM Options

```bash
java -jar target/benchmarks.jar -jvmArgs "-Xms4G -Xmx4G -XX:+UseG1GC"
```

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Run JMH Benchmarks
  run: |
    cd benchmark-jmh
    mvn clean package
    java -jar target/benchmarks.jar -rf json -rff results.json
    
- name: Compare with Baseline
  run: python scripts/compare_jmh_results.py results.json baseline.json
```

## Development

### Adding New Framework Benchmark

1. Create `src/main/java/org/techishthoughts/benchmark/{Framework}Benchmark.java`
2. Follow the template from `JacksonBenchmark.java`
3. Implement `serialize()`, `deserialize()`, and `roundtrip()` methods
4. Run and verify: `mvn clean package && java -jar target/benchmarks.jar {Framework}Benchmark`

## Resources

- [JMH Documentation](https://github.com/openjdk/jmh)
- [JMH Samples](https://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
- [Avoiding Benchmarking Pitfalls](https://www.oracle.com/technical-resources/articles/java/architect-benchmarking.html)
