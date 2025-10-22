# Grafana Dashboards for Java Serialization Frameworks

This directory contains pre-configured Grafana dashboards for visualizing serialization framework performance metrics.

## Available Dashboards

### 1. Performance Overview (`serialization-performance.json`)
Comprehensive performance analysis dashboard with:
- **Serialization Time Comparison**: Line charts showing serialization time across frameworks and payload sizes
- **Throughput Analysis**: Operations per second by framework
- **Payload Size Comparison**: Bar gauge showing serialized size efficiency
- **Compression Ratio**: Statistics on compression effectiveness
- **Memory Usage**: Time series of memory consumption
- **CPU Usage**: CPU utilization during benchmarks
- **Performance Heatmap**: Visual representation of performance by payload size
- **Best Framework Table**: Sortable table with aggregate statistics

**Key Features:**
- Filter by payload size (SMALL, MEDIUM, LARGE, HUGE)
- Filter by framework (all 13 frameworks)
- Compare baseline vs compressed configurations
- Real-time updates every 30 seconds

### 2. Resource Utilization (`resource-utilization.json`)
Deep dive into resource consumption with:
- **Memory Usage Overview**: Detailed memory consumption tracking
- **CPU Usage by Framework**: CPU utilization patterns
- **Memory Efficiency**: MB per 1000 operations
- **CPU Efficiency**: CPU percentage per 1000 operations
- **Resource Utilization Heatmap**: Combined CPU+Memory visualization
- **Top Resource Consumers**: Table ranking frameworks by resource usage
- **Alert Statistics**: Count of high-resource-usage instances
- **Average Usage Stats**: Overall resource consumption metrics

**Key Features:**
- Multi-framework comparison
- Efficiency metrics (resource per operation)
- Alert thresholds for high usage
- Sortable tables with aggregations

## Installation & Setup

### Prerequisites
1. **Prometheus** - Time series database for metrics storage
2. **Grafana** - Visualization platform (v9.0 or later)
3. **Python 3** with `psutil` - For enhanced benchmark script

### Setup Instructions

#### 1. Install Dependencies

```bash
# Install Prometheus (macOS)
brew install prometheus

# Install Grafana (macOS)
brew install grafana

# Start services
brew services start prometheus
brew services start grafana

# Install Python dependencies
pip3 install psutil
```

#### 2. Configure Prometheus

Create or update `/opt/homebrew/etc/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'serialization-benchmarks'
    static_configs:
      - targets: ['localhost:9090']

    # File-based service discovery for benchmark results
    file_sd_configs:
      - files:
          - '/Users/arthuralvesdacosta/dev/java_serialization_frameworks/results/*.prom'
        refresh_interval: 30s
```

Restart Prometheus:
```bash
brew services restart prometheus
```

#### 3. Configure Grafana Data Source

1. Open Grafana: http://localhost:3000 (default credentials: admin/admin)
2. Go to **Configuration > Data Sources**
3. Click **Add data source**
4. Select **Prometheus**
5. Configure:
   - **Name**: `Serialization Benchmarks`
   - **URL**: `http://localhost:9090`
   - **Scrape interval**: `15s`
6. Click **Save & Test**

#### 4. Import Dashboards

Two methods:

**Method A: UI Import**
1. Go to **Dashboards > Import**
2. Click **Upload JSON file**
3. Select `serialization-performance.json` or `resource-utilization.json`
4. Select the Prometheus data source
5. Click **Import**

**Method B: API Import**
```bash
# Import Performance Dashboard
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @dashboards/serialization-performance.json

# Import Resource Dashboard
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @dashboards/resource-utilization.json
```

## Running Benchmarks

### Using the Enhanced Benchmark Script

```bash
# 1. Start all framework services
./manage.sh start

# 2. Wait for services to be healthy
./manage.sh status

# 3. Run enhanced benchmark with comprehensive metrics
python3 enhanced_benchmark.py

# Output files:
# - results/enhanced_benchmark_YYYYMMDD_HHMMSS.json
# - results/metrics_YYYYMMDD_HHMMSS.prom
```

### Metrics Collection

The enhanced benchmark collects:

**Network Metrics:**
- DNS lookup time
- TCP connection time
- TLS handshake time
- Total connection time

**Serialization Metrics:**
- Average serialization time
- Min/Max serialization time
- P50, P95, P99 latencies
- Throughput (ops/sec)

**Resource Metrics:**
- CPU usage percentage
- Memory usage (MB)
- Memory delta
- Peak memory
- Thread count

**Transport Metrics:**
- Average payload size
- Compression ratio
- Network throughput (Mbps)
- Overhead percentage

## Dashboard Usage Tips

### Performance Dashboard

1. **Compare Frameworks**: Use the framework filter to compare specific serialization libraries
2. **Analyze by Payload Size**: Filter by scenario (SMALL/MEDIUM/LARGE/HUGE) to see how frameworks scale
3. **Compression Impact**: Switch between baseline and with_compression configs
4. **Identify Bottlenecks**: Look at the heatmap to find performance issues across payload sizes

### Resource Utilization Dashboard

1. **Memory Efficiency**: Sort the table by "Memory (MB)" to find most efficient frameworks
2. **CPU Efficiency**: Check CPU per 1000 operations for processor-intensive frameworks
3. **Alert Monitoring**: Watch the alert stats panels for frameworks exceeding thresholds
4. **Trend Analysis**: Use time series panels to identify memory leaks or CPU spikes

## Metrics Reference

### Available Prometheus Metrics

```promql
# Serialization performance
serialization_time_ms{framework="Jackson JSON",scenario="MEDIUM",config="baseline"}
serialization_throughput_ops{framework="Jackson JSON",scenario="MEDIUM",config="baseline"}

# Payload and compression
payload_size_bytes{framework="Jackson JSON",scenario="MEDIUM",config="baseline"}
compression_ratio{framework="Jackson JSON",scenario="MEDIUM",config="with_compression"}

# Resource utilization
memory_usage_mb{framework="Jackson JSON",scenario="MEDIUM",config="baseline"}
cpu_usage_percent{framework="Jackson JSON",scenario="MEDIUM",config="baseline"}
```

### Useful PromQL Queries

```promql
# Average serialization time across all frameworks
avg(serialization_time_ms) by (framework)

# Top 5 fastest frameworks
topk(5, avg by (framework) (serialization_time_ms))

# Memory efficiency (MB per 1000 ops)
memory_usage_mb / (serialization_throughput_ops / 1000)

# Compression effectiveness
1 - compression_ratio

# Frameworks using > 500MB memory
count(memory_usage_mb > 500) by (framework)
```

## Customization

### Adding Custom Panels

1. Click **Add panel** in any dashboard
2. Select visualization type
3. Write PromQL query
4. Configure display options
5. Save dashboard

### Alert Rules

Configure alerts in Grafana for:
- High memory usage (> 1000 MB)
- Slow serialization (> 100 ms)
- Low throughput (< 100 ops/sec)
- High CPU usage (> 80%)

Example alert configuration:
```yaml
alert: HighMemoryUsage
expr: memory_usage_mb > 1000
for: 5m
labels:
  severity: warning
annotations:
  summary: "High memory usage detected"
  description: "Framework {{ $labels.framework }} using {{ $value }}MB"
```

## Troubleshooting

### No Data in Dashboards

1. **Check Prometheus**: http://localhost:9090/targets
2. **Verify metrics file**: `ls -l results/*.prom`
3. **Check data source**: Grafana > Configuration > Data Sources
4. **Run benchmark**: `python3 enhanced_benchmark.py`

### Metrics Not Updating

1. **Check scrape interval**: Should be 15-30s
2. **Verify file permissions**: Prometheus needs read access to `results/` directory
3. **Check Prometheus logs**: `tail -f /opt/homebrew/var/log/prometheus.log`
4. **Restart Prometheus**: `brew services restart prometheus`

### Dashboard Import Fails

1. **Check Grafana version**: Must be v9.0+
2. **Verify JSON syntax**: Use `jq . dashboard.json` to validate
3. **Check data source**: Ensure Prometheus data source exists
4. **Manual import**: Copy JSON content and paste in Grafana UI

## Best Practices

1. **Regular Benchmarks**: Run benchmarks daily/weekly to track performance trends
2. **Baseline Comparison**: Always run baseline config before testing optimizations
3. **Resource Monitoring**: Watch memory and CPU to prevent resource exhaustion
4. **Alert Configuration**: Set up alerts for critical thresholds
5. **Dashboard Organization**: Use folders to organize dashboards by category
6. **Retention Policy**: Configure Prometheus retention based on storage capacity

## References

- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Best Practices](https://grafana.com/docs/grafana/latest/best-practices/)
- [Dashboard Design Guide](https://grafana.com/docs/grafana/latest/dashboards/)
