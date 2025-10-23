# Apache Parquet - Deep Dive

![Speed](https://img.shields.io/badge/Speed-2_stars-orange)
![Compression](https://img.shields.io/badge/Compression-5_stars-brightgreen)
![Ease of Use](https://img.shields.io/badge/Ease%20of%20Use-3_stars-yellow)

## Overview

Apache Parquet is a columnar storage file format designed for efficient data storage and retrieval in big data analytics scenarios. It provides exceptional compression ratios and query performance through column-oriented data organization, making it the preferred choice for data warehousing and analytics workloads.

**Port**: 8097
**Category**: Columnar
**Official Site**: https://parquet.apache.org/

## Key Characteristics

### Strengths
- **Exceptional Compression**: Best-in-class compression ratio (0.25 ratio, 75% reduction)
- **Columnar Queries**: Efficient column-based data access for analytics
- **Hadoop Ecosystem**: Native integration with Spark, Hive, Impala
- **Schema Evolution**: Supports adding/removing columns
- **Predicate Pushdown**: Filter data at storage layer
- **Encoding Schemes**: Multiple encoding options for optimal compression

### Weaknesses
- **Slower Serialization**: File-based format with significant overhead
- **Not Streaming**: Requires full file write/read cycles
- **Overkill for Transactional**: Not designed for OLTP workloads
- **Complexity**: Steeper learning curve than simple formats
- **File-Based Only**: Cannot stream over network protocols

## Performance Benchmarks

### Serialization Performance
| Metric | Value | Rank |
|--------|-------|------|
| **Avg Serialization Time** | ~150ms | 12/13 |
| **Throughput** | ~7 ops/sec | 12/13 |
| **Payload Size (MEDIUM)** | 3.1KB | 1/13 |
| **Compression Ratio** | 0.25 | 1/13 |

### Resource Utilization
| Metric | Value |
|--------|-------|
| **CPU Usage** | 4.2% |
| **Memory** | 312.8 MB |
| **Memory Delta** | 45.6 MB |
| **Threads** | 52 |

### Performance by Payload Size
| Size | Avg Time | Throughput | Size (bytes) | Compression |
|------|----------|------------|--------------|-------------|
| SMALL (10 users) | 85.34ms | 12 ops/s | 412 | 82% |
| MEDIUM (100 users) | 156.78ms | 6 ops/s | 3,145 | 75% |
| LARGE (1000 users) | 789.23ms | 1.3 ops/s | 28,934 | 76% |
| HUGE (10000 users) | 3456.89ms | 0.3 ops/s | 287,456 | 77% |

## Implementation Details

### Dependencies

```xml
<dependency>
    <groupId>org.apache.parquet</groupId>
    <artifactId>parquet-avro</artifactId>
    <version>1.13.1</version>
</dependency>
<dependency>
    <groupId>org.apache.parquet</groupId>
    <artifactId>parquet-hadoop</artifactId>
    <version>1.13.1</version>
</dependency>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-client</artifactId>
    <version>3.3.6</version>
</dependency>
```

### Basic Usage

```java
@Service
public class ParquetSerializationService {
    private final String tempDirectory;

    public ParquetSerializationService() {
        this.tempDirectory = System.getProperty("java.io.tmpdir");
    }

    public byte[] serialize(UserPayload payload) throws IOException {
        Path tempFile = Files.createTempFile("parquet-", ".parquet");

        try {
            // Convert to Avro schema
            Schema schema = ReflectData.get().getSchema(UserPayload.class);

            // Write Parquet file
            try (ParquetWriter<UserPayload> writer = AvroParquetWriter
                    .<UserPayload>builder(new org.apache.hadoop.fs.Path(tempFile.toString()))
                    .withSchema(schema)
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                    .build()) {

                writer.write(payload);
            }

            // Read file to bytes
            return Files.readAllBytes(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public UserPayload deserialize(byte[] data) throws IOException {
        Path tempFile = Files.createTempFile("parquet-", ".parquet");

        try {
            Files.write(tempFile, data);

            try (ParquetReader<UserPayload> reader = AvroParquetReader
                    .<UserPayload>builder(new org.apache.hadoop.fs.Path(tempFile.toString()))
                    .build()) {

                return reader.read();
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
```

### Advanced Configuration

```java
@Configuration
public class ParquetConfiguration {

    @Bean
    public ParquetProperties parquetProperties() {
        return ParquetProperties.builder()
            .withPageSize(1024 * 1024) // 1MB page size
            .withDictionaryPageSize(512 * 1024) // 512KB dictionary
            .withDictionaryEncoding(true)
            .withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
            .build();
    }

    @Bean
    public Configuration hadoopConfiguration() {
        Configuration conf = new Configuration();

        // Compression settings
        conf.set("parquet.compression", "SNAPPY");
        conf.set("parquet.enable.dictionary", "true");

        // Block and page sizes
        conf.setInt("parquet.block.size", 128 * 1024 * 1024); // 128MB
        conf.setInt("parquet.page.size", 1024 * 1024); // 1MB

        // Column index and statistics
        conf.setBoolean("parquet.column.index.enabled", true);
        conf.setBoolean("parquet.statistics.enabled", true);

        return conf;
    }

    @Bean
    public ParquetWriter.Builder<GenericRecord> parquetWriterBuilder(
            org.apache.hadoop.fs.Path path,
            Schema schema,
            Configuration conf) {

        return AvroParquetWriter
            .<GenericRecord>builder(path)
            .withSchema(schema)
            .withConf(conf)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withRowGroupSize(128 * 1024 * 1024)
            .withPageSize(1024 * 1024)
            .withDictionaryEncoding(true)
            .withValidation(false)
            .withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0);
    }
}
```

## Use Cases

### Ideal For

**Data Warehousing & Analytics**
- OLAP queries and aggregations
- Business intelligence and reporting
- Data lake storage format
- Time-series data analysis

**Big Data Processing**
- Spark batch processing
- Hive table storage
- Impala query optimization
- Presto/Trino analytics

**ETL Pipelines**
- Data transformation storage
- Intermediate processing results
- Archive and backup storage
- Cross-platform data exchange

**Columnar Analytics**
- Column-specific queries
- Aggregate calculations
- Statistical analysis
- Machine learning feature storage

### Not Ideal For

**Transactional Systems**
- OLTP workloads
- Frequent updates/deletes
- Use row-based databases instead

**Real-Time Streaming**
- Low-latency requirements
- Continuous data streams
- Consider Avro or Protocol Buffers

**Small Records**
- Individual record serialization
- Message queue payloads
- Use MessagePack or CBOR

**Network Protocols**
- HTTP API responses
- RPC communication
- Use gRPC or Thrift

## Optimization Tips

### 1. Choose Appropriate Compression

```java
// SNAPPY: Balanced speed and compression (recommended)
ParquetWriter<T> writer = AvroParquetWriter
    .<T>builder(path)
    .withCompressionCodec(CompressionCodecName.SNAPPY)
    .build();

// GZIP: Better compression, slower
ParquetWriter<T> writer = AvroParquetWriter
    .<T>builder(path)
    .withCompressionCodec(CompressionCodecName.GZIP)
    .build();

// LZ4: Faster, less compression
ParquetWriter<T> writer = AvroParquetWriter
    .<T>builder(path)
    .withCompressionCodec(CompressionCodecName.LZ4)
    .build();
```

### 2. Optimize Row Group Size

```java
// Larger row groups = better compression, more memory
// Default: 128MB
ParquetWriter<T> writer = AvroParquetWriter
    .<T>builder(path)
    .withRowGroupSize(256 * 1024 * 1024) // 256MB for large datasets
    .build();

// Smaller for memory-constrained environments
ParquetWriter<T> writer = AvroParquetWriter
    .<T>builder(path)
    .withRowGroupSize(64 * 1024 * 1024) // 64MB
    .build();
```

### 3. Enable Dictionary Encoding

```java
// Reduces size for columns with repeated values
ParquetWriter<T> writer = AvroParquetWriter
    .<T>builder(path)
    .withDictionaryEncoding(true)
    .withDictionaryPageSize(512 * 1024) // 512KB
    .build();
```

### 4. Optimize for Read Patterns

```java
// Enable column indexes for predicate pushdown
Configuration conf = new Configuration();
conf.setBoolean("parquet.column.index.enabled", true);
conf.setBoolean("parquet.bloom.filter.enabled", true);

// Write with statistics for query optimization
conf.setBoolean("parquet.statistics.enabled", true);
conf.set("parquet.statistics.truncate.length", "128");
```

## Hadoop Ecosystem Integration

### Apache Spark

```java
// Read Parquet files
Dataset<Row> df = spark.read()
    .parquet("hdfs://path/to/data/*.parquet");

// Query with predicate pushdown
Dataset<Row> filtered = df
    .filter("age > 30")
    .select("name", "email");

// Write Parquet files
df.write()
    .mode(SaveMode.Overwrite)
    .option("compression", "snappy")
    .parquet("hdfs://path/to/output");

// Partitioned writes
df.write()
    .partitionBy("year", "month")
    .parquet("hdfs://path/to/partitioned");
```

### Apache Hive

```sql
-- Create external Parquet table
CREATE EXTERNAL TABLE users (
    id BIGINT,
    name STRING,
    email STRING,
    age INT
)
STORED AS PARQUET
LOCATION 'hdfs://path/to/data';

-- Insert with compression
SET parquet.compression=SNAPPY;
INSERT INTO users_parquet
SELECT * FROM users_text;

-- Query with column pruning
SELECT name, email
FROM users
WHERE age > 30;
```

### Apache Impala

```sql
-- Create Parquet table
CREATE TABLE users (
    id BIGINT,
    name STRING,
    email STRING
)
STORED AS PARQUET;

-- Enable runtime code generation
SET RUNTIME_FILTER_MODE=GLOBAL;

-- Query with statistics
COMPUTE STATS users;
SELECT COUNT(*) FROM users WHERE age > 30;
```

## API Endpoints

### Benchmark Endpoint
```bash
POST http://localhost:8097/api/parquet/v2/benchmark
Content-Type: application/json

{
  "complexity": "MEDIUM",
  "iterations": 50,
  "enableWarmup": true,
  "enableCompression": true,
  "enableRoundtrip": true,
  "enableMemoryMonitoring": true
}
```

### Framework Information
```bash
GET http://localhost:8097/api/parquet/v2/info
```

**Response:**
```json
{
  "framework": "Apache Parquet",
  "version": "2.0",
  "supportedCompressionAlgorithms": ["SNAPPY", "GZIP", "LZ4", "ZSTD", "BROTLI"],
  "supportsSchemaEvolution": true,
  "typicalUseCase": "Data warehousing, analytics, big data processing"
}
```

## Real-World Examples

### ETL Pipeline Storage

```java
public class DataPipelineService {

    public void processAndStore(List<Transaction> transactions) throws IOException {
        Schema schema = ReflectData.get().getSchema(Transaction.class);
        org.apache.hadoop.fs.Path outputPath =
            new org.apache.hadoop.fs.Path("hdfs://data-lake/transactions/date=2025-10-22/");

        try (ParquetWriter<Transaction> writer = AvroParquetWriter
                .<Transaction>builder(outputPath)
                .withSchema(schema)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withRowGroupSize(128 * 1024 * 1024)
                .build()) {

            for (Transaction transaction : transactions) {
                writer.write(transaction);
            }
        }

        log.info("Wrote {} transactions to {}", transactions.size(), outputPath);
    }

    public List<Transaction> readFromDateRange(String startDate, String endDate)
            throws IOException {

        List<Transaction> results = new ArrayList<>();
        org.apache.hadoop.fs.Path inputPath =
            new org.apache.hadoop.fs.Path("hdfs://data-lake/transactions/");

        try (ParquetReader<Transaction> reader = AvroParquetReader
                .<Transaction>builder(inputPath)
                .build()) {

            Transaction transaction;
            while ((transaction = reader.read()) != null) {
                results.add(transaction);
            }
        }

        return results;
    }
}
```

### Analytics with Filtering

```java
public class AnalyticsService {

    public Map<String, Long> aggregateUsersByCountry(String parquetFile)
            throws IOException {

        Map<String, Long> countryStats = new HashMap<>();
        org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(parquetFile);

        // Read with column projection (only country field)
        try (ParquetReader<GenericRecord> reader = AvroParquetReader
                .<GenericRecord>builder(path)
                .build()) {

            GenericRecord record;
            while ((record = reader.read()) != null) {
                String country = record.get("country").toString();
                countryStats.merge(country, 1L, Long::sum);
            }
        }

        return countryStats;
    }

    public void writeAggregatedReport(Map<String, Long> stats, String outputFile)
            throws IOException {

        Schema schema = SchemaBuilder.record("CountryStats")
            .fields()
            .requiredString("country")
            .requiredLong("count")
            .endRecord();

        org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(outputFile);

        try (ParquetWriter<GenericRecord> writer = AvroParquetWriter
                .<GenericRecord>builder(path)
                .withSchema(schema)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .build()) {

            for (Map.Entry<String, Long> entry : stats.entrySet()) {
                GenericRecord record = new GenericData.Record(schema);
                record.put("country", entry.getKey());
                record.put("count", entry.getValue());
                writer.write(record);
            }
        }
    }
}
```

## Alternatives & When to Switch

| Switch To | When | Why |
|-----------|------|-----|
| **Apache Arrow** | In-memory analytics | Faster columnar processing |
| **ORC** | Hive-centric workloads | Better Hive integration |
| **Avro** | Schema evolution priority | Simpler schema management |
| **Delta Lake** | ACID transactions needed | Update/delete support |

## Common Patterns

### Partitioned Data Storage

```java
public class PartitionedWriter {

    public void writePartitioned(List<Event> events, String basePath)
            throws IOException {

        // Group by date partition
        Map<String, List<Event>> partitions = events.stream()
            .collect(Collectors.groupingBy(e ->
                e.getTimestamp().toLocalDate().toString()));

        Schema schema = ReflectData.get().getSchema(Event.class);

        for (Map.Entry<String, List<Event>> partition : partitions.entrySet()) {
            String date = partition.getKey();
            org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(
                String.format("%s/date=%s/data.parquet", basePath, date));

            try (ParquetWriter<Event> writer = AvroParquetWriter
                    .<Event>builder(path)
                    .withSchema(schema)
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .build()) {

                for (Event event : partition.getValue()) {
                    writer.write(event);
                }
            }
        }
    }
}
```

### Schema Evolution

```java
public class SchemaEvolutionExample {

    // Old schema (v1)
    Schema oldSchema = SchemaBuilder.record("User")
        .fields()
        .requiredString("name")
        .requiredString("email")
        .endRecord();

    // New schema (v2) - added optional field
    Schema newSchema = SchemaBuilder.record("User")
        .fields()
        .requiredString("name")
        .requiredString("email")
        .optionalString("phone") // New field
        .endRecord();

    public void readOldWithNewSchema(String oldFile) throws IOException {
        org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(oldFile);

        // Reader automatically handles missing fields
        try (ParquetReader<GenericRecord> reader = AvroParquetReader
                .<GenericRecord>builder(path)
                .withDataModel(GenericData.get())
                .build()) {

            GenericRecord record;
            while ((record = reader.read()) != null) {
                String name = record.get("name").toString();
                String email = record.get("email").toString();
                String phone = record.get("phone") != null
                    ? record.get("phone").toString()
                    : "N/A";

                System.out.printf("%s, %s, %s%n", name, email, phone);
            }
        }
    }
}
```

### Metadata and Statistics

```java
public class ParquetMetadataReader {

    public void printFileMetadata(String parquetFile) throws IOException {
        org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(parquetFile);
        Configuration conf = new Configuration();

        ParquetMetadata metadata = ParquetFileReader.readFooter(
            conf, path, ParquetMetadataConverter.NO_FILTER);

        // File metadata
        FileMetaData fileMetaData = metadata.getFileMetaData();
        System.out.println("Schema: " + fileMetaData.getSchema());
        System.out.println("Created by: " + fileMetaData.getCreatedBy());

        // Block (row group) metadata
        for (BlockMetaData blockMetaData : metadata.getBlocks()) {
            System.out.println("Row count: " + blockMetaData.getRowCount());
            System.out.println("Total size: " + blockMetaData.getTotalByteSize());

            // Column metadata
            for (ColumnChunkMetaData column : blockMetaData.getColumns()) {
                System.out.println("Column: " + column.getPath());
                System.out.println("Type: " + column.getType());
                System.out.println("Encodings: " + column.getEncodings());
                System.out.println("Compression: " + column.getCodec());

                // Statistics
                Statistics<?> stats = column.getStatistics();
                if (stats != null) {
                    System.out.println("Min: " + stats.minAsString());
                    System.out.println("Max: " + stats.maxAsString());
                    System.out.println("Null count: " + stats.getNumNulls());
                }
            }
        }
    }
}
```

## Troubleshooting

### Issue: OutOfMemoryError

**Problem**: Large row groups cause memory exhaustion

**Solution**:
```java
// Reduce row group size
ParquetWriter<T> writer = AvroParquetWriter
    .<T>builder(path)
    .withRowGroupSize(64 * 1024 * 1024) // 64MB instead of 128MB
    .build();

// Or increase JVM heap
// java -Xmx4g -XX:MaxDirectMemorySize=2g
```

### Issue: Slow Write Performance

**Problem**: Writing takes too long

**Solutions**:
```java
// 1. Use faster compression
.withCompressionCodec(CompressionCodecName.LZ4) // instead of GZIP

// 2. Disable dictionary encoding for high-cardinality columns
.withDictionaryEncoding(false)

// 3. Increase page size
.withPageSize(2 * 1024 * 1024) // 2MB pages

// 4. Disable validation in production
.withValidation(false)
```

### Issue: Schema Compatibility

**Problem**: Cannot read files written with different schema

**Solution**:
```java
// Use Avro schema resolution
GenericData dataModel = new GenericData();
dataModel.addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());

ParquetReader<GenericRecord> reader = AvroParquetReader
    .<GenericRecord>builder(path)
    .withDataModel(dataModel)
    .build();
```

### Issue: Hadoop Dependencies Conflict

**Problem**: ClassNotFoundException or version conflicts

**Solution**:
```xml
<!-- Exclude transitive Hadoop dependencies -->
<dependency>
    <groupId>org.apache.parquet</groupId>
    <artifactId>parquet-hadoop</artifactId>
    <version>1.13.1</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-common</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Add specific Hadoop version -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-client</artifactId>
    <version>3.3.6</version>
</dependency>
```

## Benchmarking Results

### Compression Effectiveness

Apache Parquet achieves the best compression ratio among all tested frameworks:

**Compression Comparison**:
- Parquet: 0.25 ratio (75% reduction) - Best
- Avro: 0.45 ratio (55% reduction)
- Protocol Buffers: 0.52 ratio (48% reduction)
- JSON: 0.65 ratio (35% reduction)

### Performance Trade-offs

**Write Performance**: Slower due to columnar organization and compression
- ~150ms for MEDIUM payload vs 5ms for Kryo

**Read Performance**: Faster for analytical queries (column pruning)
- Full scan: Similar to other formats
- Column-specific: 3-5x faster than row formats

### Storage Efficiency

For 1 million user records:
- **Parquet**: 287 MB (with SNAPPY)
- **Avro**: 512 MB
- **JSON**: 1.2 GB
- **Space Savings**: 76% vs JSON, 44% vs Avro

## Best Practices

1. **Choose Right Compression**: SNAPPY for balanced performance, GZIP for max compression
2. **Optimize Row Groups**: 128-256MB for large datasets, 64MB for memory constraints
3. **Enable Statistics**: Always enable for query optimization
4. **Partition Data**: By date or other frequent filter columns
5. **Use Column Pruning**: Only read columns you need
6. **Monitor Memory**: Watch heap and direct memory usage
7. **Version Schemas**: Use Avro schema evolution features
8. **Benchmark**: Test different configurations for your workload
9. **Avoid Small Files**: Combine into larger files for efficiency
10. **Use Predicate Pushdown**: Filter at storage layer when possible

## Additional Resources

- **Official Documentation**: https://parquet.apache.org/docs/
- **Parquet Format Spec**: https://github.com/apache/parquet-format
- **Parquet Java**: https://github.com/apache/parquet-mr
- **Spark Integration**: https://spark.apache.org/docs/latest/sql-data-sources-parquet.html
- **AWS Best Practices**: https://aws.amazon.com/blogs/big-data/best-practices-for-successfully-managing-memory-for-apache-spark-applications-on-amazon-emr/
- **Cloudera Guide**: https://docs.cloudera.com/documentation/enterprise/latest/topics/impala_parquet.html

## Source Code

Implementation: [`parquet-poc/`](../../parquet-poc/)

Key Files:
- `ParquetBenchmarkControllerV2.java` - REST endpoints
- `ParquetSerializationServiceV2.java` - Core serialization logic
- `ParquetConfiguration.java` - Writer/reader configuration

---

**Last Updated**: 2025-10-22
**Benchmark Version**: 2.0.0
