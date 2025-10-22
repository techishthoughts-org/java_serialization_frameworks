# Screenshot Guide for Documentation

This guide shows what screenshots to capture for the project documentation.

## Required Screenshots

### 1. **Project Structure** (`screenshots/01-project-structure.png`)

**Command to run:**
```bash
tree -L 2 -I 'target|node_modules|.git' --dirsfirst
```

**What to capture:**
- Clean project directory structure
- All 13 framework POCs
- Common payload module
- Management scripts
- Dashboards directory
- Results directory

---

### 2. **Management Script Help** (`screenshots/02-manage-help.png`)

**Command to run:**
```bash
./manage.sh help
```

**What to capture:**
- Full help output showing all commands
- Framework list
- Usage examples
- Color-coded output

---

### 3. **Starting All Services** (`screenshots/03-services-starting.png`)

**Command to run:**
```bash
./manage.sh start
```

**What to capture:**
- Services starting up
- Health checks being performed
- Progress indicators
- Success/failure messages with colors

---

### 4. **Service Status Check** (`screenshots/04-services-status.png`)

**Command to run:**
```bash
./manage.sh status
```

**What to capture:**
- All 13 frameworks listed
- Port numbers
- Categories
- Health status (RUNNING/STOPPED)
- Running services count

---

### 5. **Enhanced Benchmark Execution** (`screenshots/05-benchmark-running.png`)

**Command to run:**
```bash
python3 enhanced_benchmark.py
```

**What to capture:**
- Benchmark header with metrics phases
- Health check phase
- Testing progress for multiple frameworks
- Metrics being collected (time, size, CPU, memory)
- Real-time updates

---

### 6. **Benchmark Results Summary** (`screenshots/06-benchmark-complete.png`)

**What to capture:**
- Benchmark completion message
- Total tests run
- Success/failure counts
- Output file paths
- Performance summary by framework

---

### 7. **JSON Results File** (`screenshots/07-results-json.png`)

**Command to run:**
```bash
cat results/enhanced_benchmark_*.json | jq '.' | head -50
```

**What to capture:**
- JSON structure with comprehensive metrics
- Network metrics (DNS, TCP, handshake)
- Serialization metrics (times, throughput)
- Resource metrics (CPU, memory)
- Transport metrics (size, compression)

---

### 8. **Prometheus Metrics Export** (`screenshots/08-prometheus-metrics.png`)

**Command to run:**
```bash
cat results/metrics_*.prom | head -30
```

**What to capture:**
- Prometheus format metrics
- Different metric types
- Labels (framework, scenario, config)
- Actual metric values

---

### 9. **Grafana Dashboard - Performance Overview** (`screenshots/09-grafana-performance.png`)

**What to capture (in Grafana UI):**
- Performance Overview dashboard
- Serialization time comparison chart
- Throughput chart
- Payload size bar gauge
- Compression ratio stats
- Memory/CPU usage panels
- Performance heatmap

**URL:** `http://localhost:3000/d/serialization-performance`

---

### 10. **Grafana Dashboard - Resource Utilization** (`screenshots/10-grafana-resources.png`)

**What to capture (in Grafana UI):**
- Resource Utilization dashboard
- Memory usage overview
- CPU usage chart
- Memory efficiency metrics
- CPU efficiency metrics
- Resource heatmap
- Top consumers table

**URL:** `http://localhost:3000/d/resource-utilization`

---

### 11. **Framework API Response** (`screenshots/11-api-response.png`)

**Command to run:**
```bash
curl -X POST http://localhost:8081/api/jackson/v2/benchmark \
  -H "Content-Type: application/json" \
  -d '{
    "complexity": "SMALL",
    "iterations": 10,
    "enableWarmup": true,
    "enableCompression": true,
    "enableRoundtrip": true,
    "enableMemoryMonitoring": true
  }' | jq '.'
```

**What to capture:**
- Full JSON response
- Success status
- All metrics fields
- Memory metrics
- Roundtrip results

---

### 12. **Service Logs** (`screenshots/12-service-logs.png`)

**Command to run:**
```bash
./manage.sh logs jackson
```

**What to capture:**
- Real-time log streaming
- Spring Boot startup messages
- Benchmark execution logs
- Request/response logging
- Performance metrics in logs

---

### 13. **Prometheus Targets** (`screenshots/13-prometheus-targets.png`)

**What to capture (in Prometheus UI):**
- Targets page showing file_sd configuration
- Service discovery for benchmark results
- Scrape status
- Last scrape time

**URL:** `http://localhost:9090/targets`

---

### 14. **Prometheus Query** (`screenshots/14-prometheus-query.png`)

**What to capture (in Prometheus UI):**
- Query interface
- Example query: `avg(serialization_time_ms) by (framework)`
- Graph visualization
- Table view with data

**URL:** `http://localhost:9090/graph`

---

### 15. **Analysis Script Output** (`screenshots/15-analysis-output.png`)

**Command to run:**
```bash
./manage.sh analyze
```

**What to capture:**
- Analysis summary
- Performance rankings
- Category comparisons
- Key insights
- Recommendations

---

## Screenshot Best Practices

### Terminal Screenshots

1. **Use a clean terminal theme**
   - Recommended: iTerm2 with Solarized Dark or Material Theme
   - Font: Menlo, Monaco, or Fira Code (14-16pt)
   - Window size: 120 columns x 40 rows minimum

2. **Before capturing:**
   ```bash
   clear  # Clear terminal
   # Run command
   ```

3. **Capture tools:**
   - macOS: `Cmd + Shift + 4` then `Space` (window capture)
   - Or use: `screencapture -w screenshots/filename.png`

### Browser Screenshots (Grafana/Prometheus)

1. **Use clean browser window**
   - Hide bookmarks bar
   - Use full screen or large window
   - Zoom to 100%

2. **Before capturing:**
   - Let dashboards load completely
   - Ensure data is visible in all panels
   - Check time range shows recent data

3. **Capture tools:**
   - macOS: `Cmd + Shift + 4` then select area
   - Browser extensions: Full Page Screenshot

---

## Screenshot Annotations

For each screenshot, add a caption in the documentation:

```markdown
![Description](screenshots/filename.png)
*Caption describing what's shown in the screenshot*
```

Example:
```markdown
![Service Status](screenshots/04-services-status.png)
*All 13 serialization framework services running and healthy, showing port numbers and categories*
```

---

## Updating Screenshots

When to update screenshots:
- After major feature additions
- After UI/output format changes
- When dashboard layouts are modified
- When adding new frameworks

**Quick update script:**
```bash
# Take all terminal screenshots
./scripts/capture-terminal-screenshots.sh

# Browse to Grafana and take dashboard screenshots manually

# Update documentation references
./scripts/update-screenshot-references.sh
```

---

## Screenshot Organization

```
screenshots/
├── 01-project-structure.png
├── 02-manage-help.png
├── 03-services-starting.png
├── 04-services-status.png
├── 05-benchmark-running.png
├── 06-benchmark-complete.png
├── 07-results-json.png
├── 08-prometheus-metrics.png
├── 09-grafana-performance.png
├── 10-grafana-resources.png
├── 11-api-response.png
├── 12-service-logs.png
├── 13-prometheus-targets.png
├── 14-prometheus-query.png
└── 15-analysis-output.png
```

---

## Quick Screenshot Capture Session

Run these commands in sequence for terminal screenshots:

```bash
# Clean terminal and start session
clear

# Screenshot 1: Project structure
tree -L 2 -I 'target|node_modules|.git' --dirsfirst
# CAPTURE NOW

# Screenshot 2: Help
clear
./manage.sh help
# CAPTURE NOW

# Screenshot 3: Start services
clear
./manage.sh start
# CAPTURE NOW (wait for completion)

# Screenshot 4: Status
clear
./manage.sh status
# CAPTURE NOW

# Screenshot 5-6: Benchmark
clear
python3 enhanced_benchmark.py
# CAPTURE DURING AND AFTER

# Screenshot 7: JSON results
clear
cat results/enhanced_benchmark_*.json | jq '.' | head -50
# CAPTURE NOW

# Screenshot 8: Prometheus metrics
clear
cat results/metrics_*.prom | head -30
# CAPTURE NOW

# Screenshot 11: API call
clear
curl -X POST http://localhost:8081/api/jackson/v2/benchmark \
  -H "Content-Type: application/json" \
  -d '{"complexity": "SMALL", "iterations": 10, "enableWarmup": true, "enableCompression": true, "enableRoundtrip": true, "enableMemoryMonitoring": true}' | jq '.'
# CAPTURE NOW

# Screenshot 12: Logs
clear
./manage.sh logs jackson
# CAPTURE NOW (Ctrl+C to stop)

# Screenshot 15: Analysis
clear
./manage.sh analyze
# CAPTURE NOW
```

For Grafana/Prometheus screenshots (9, 10, 13, 14):
1. Open browser
2. Navigate to URLs listed above
3. Wait for data to load
4. Capture full dashboard view

---

## Verification Checklist

Before finalizing screenshots:

- [ ] All screenshots are high resolution (at least 1920x1080 for dashboards)
- [ ] Terminal screenshots use readable font size (14-16pt minimum)
- [ ] No sensitive information visible (passwords, tokens, etc.)
- [ ] Color coding is visible and clear
- [ ] All text is legible
- [ ] Screenshots show successful operations (not errors unless documenting troubleshooting)
- [ ] Filenames match documentation references
- [ ] All 15 screenshots captured
- [ ] Screenshots committed to git
- [ ] Documentation updated with screenshot references
