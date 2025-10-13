# Quick Benchmark Guide

**Goal:** Run benchmarks and extract numbers for README in under 30 minutes

## 🚀 Fastest Path (Recommended)

### Option 1: Quick Benchmarks Only (~5 minutes)

```bash
# 1. Start all frameworks
python start_all_frameworks_comprehensive.py

# 2. Wait for all services to be ready
sleep 30

# 3. Run quick integration benchmarks
python final_comprehensive_benchmark.py

# 4. Extract and format results
python extract_readme_metrics.py

# 5. Copy the output to README.md
```

### Option 2: Complete with Reports (~30 minutes)

```bash
# All-in-one command
python run_benchmark_with_phase4.py --quick

# When done, extract metrics
python extract_readme_metrics.py --update-readme
```

### Option 3: Full Suite with JMH (~60 minutes)

```bash
# Complete workflow
python run_benchmark_with_phase4.py

# Extract and update README
python extract_readme_metrics.py --update-readme
```

---

## 📊 Extract Metrics Commands

### Preview Markdown for README
```bash
python extract_readme_metrics.py
```

### Get JSON output
```bash
python extract_readme_metrics.py --output json
```

### Get CSV for spreadsheet
```bash
python extract_readme_metrics.py --output csv > results.csv
```

### Automatically update README
```bash
# Preview first (dry run)
python extract_readme_metrics.py --update-readme --dry-run

# Actually update
python extract_readme_metrics.py --update-readme
```

---

## 🎯 What Gets Measured

### Integration Tests (Default)
- **Measures:** Full stack (HTTP + serialization + Spring Boot)
- **Time:** ~5-10 minutes
- **Output:** `final_comprehensive_benchmark_*.json`
- **Use for:** Real-world performance comparison

### JMH Microbenchmarks (Optional)
- **Measures:** Pure serialization performance
- **Time:** ~15-30 minutes
- **Output:** `reports/jmh_results_*.json`
- **Use for:** Algorithm comparison, HTTP overhead analysis

---

## 📈 Understanding Results

### Key Metrics

**Success Rate:**
- 100% = Production ready
- 95-99% = Good, minor issues
- <95% = Needs investigation

**Response Time:**
- <10ms = Excellent
- 10-50ms = Good
- 50-100ms = Acceptable
- >100ms = Slow (check for issues)

**HTTP Overhead:**
- 100-200% = Normal (HTTP adds 1-2x serialization time)
- >300% = High (framework is very fast, HTTP dominates)

### Interpreting Results

```
Framework: Kryo
- JMH: 1.8ms (pure serialization)
- Integration: 5.6ms (with HTTP)
- Overhead: 211% (HTTP adds 3.8ms)
```

**Meaning:** Kryo is extremely fast at serialization (1.8ms), but HTTP adds 3.8ms overhead. For internal services (no HTTP), Kryo is ideal. For REST APIs, the difference is less important.

---

## 🔧 Troubleshooting

### No benchmark results found

```bash
# Check if benchmark ran
ls -la final_comprehensive_benchmark_*.json

# If empty, run benchmark
python final_comprehensive_benchmark.py
```

### Services not responding

```bash
# Check service health
for port in {8081..8097}; do
  curl -s http://localhost:$port/actuator/health | jq -r ".status" 2>/dev/null || echo "Port $port: DOWN"
done

# Restart if needed
pkill -f "spring-boot:run"
python start_all_frameworks_comprehensive.py
```

### Results seem wrong

```bash
# 1. Check if warmup happened
# First few requests are always slower

# 2. Check for GC interference
# Run multiple times, compare results

# 3. Check system load
top
# If CPU > 80%, results may be skewed
```

---

## 📋 Checklist

Before running benchmarks:

- [ ] Java 21 installed (`java -version`)
- [ ] Maven 3.8+ installed (`mvn -version`)
- [ ] Python 3.8+ installed (`python3 --version`)
- [ ] Dependencies installed (`pip install -r requirements.txt`)
- [ ] System not under heavy load (`top`)
- [ ] Enough memory (~8GB free recommended)

---

## 🎨 Generate Visualizations

```bash
# After running benchmarks
python generate_benchmark_plots.py

# Output: PNG files in current directory
ls -la *.png
```

---

## 📊 View Interactive Dashboard

```bash
# Install Streamlit if needed
pip install streamlit

# Start dashboard
streamlit run unified_benchmark_dashboard.py --server.port 8509

# Open browser to: http://localhost:8509
```

---

## 💡 Pro Tips

### 1. Always warmup first
```bash
# Bad: Run benchmarks immediately
python final_comprehensive_benchmark.py

# Good: Let services warm up
python start_all_frameworks_comprehensive.py
sleep 60  # Wait 1 minute
python final_comprehensive_benchmark.py
```

### 2. Run multiple times for confidence
```bash
# Run 3 times, compare results
for i in {1..3}; do
  echo "Run $i"
  python final_comprehensive_benchmark.py
  sleep 10
done

# Results should be within ±10% of each other
```

### 3. Use quick mode for testing
```bash
# Quick mode: Fewer iterations, faster results
python run_benchmark_with_phase4.py --quick

# Use for:
# - Testing changes
# - Quick comparisons
# - Development

# Full mode for final results
python run_benchmark_with_phase4.py
```

### 4. Check for outliers
```bash
# View raw results
cat final_comprehensive_benchmark_*.json | jq '.results[].scenarios.MEDIUM.measurements[] | .response_time_ms'

# If you see values like: 5, 5, 5, 100, 5, 5
# The 100ms is likely GC interference - run again
```

---

## 🚦 Quick Status Check

```bash
# Are services running?
pgrep -f "spring-boot:run" | wc -l
# Should be 15 (one per framework)

# Are they healthy?
curl -s http://localhost:8081/actuator/health | jq -r .status
# Should be "UP"

# Do we have results?
ls -la final_comprehensive_benchmark_*.json | tail -1
# Should show recent file

# Can we extract metrics?
python extract_readme_metrics.py 2>&1 | head -5
# Should show "### 🏆 Latest Benchmark Results"
```

---

## 📦 Complete Workflow Example

```bash
#!/bin/bash
# complete_benchmark_workflow.sh

set -e  # Exit on error

echo "🚀 Starting complete benchmark workflow"

# 1. Clean up any existing processes
echo "🧹 Cleaning up..."
pkill -f "spring-boot:run" || true
sleep 5

# 2. Start all frameworks
echo "📦 Starting frameworks..."
python start_all_frameworks_comprehensive.py

# 3. Wait for warmup
echo "⏳ Waiting for warmup (60 seconds)..."
sleep 60

# 4. Verify all services
echo "✅ Verifying services..."
all_up=true
for port in {8081..8097}; do
  if ! curl -s http://localhost:$port/actuator/health | grep -q "UP"; then
    echo "❌ Service on port $port is DOWN"
    all_up=false
  fi
done

if [ "$all_up" = false ]; then
  echo "❌ Not all services are up. Exiting."
  exit 1
fi

echo "✅ All services are up"

# 5. Run benchmarks
echo "📊 Running benchmarks..."
python final_comprehensive_benchmark.py

# 6. Extract metrics
echo "📝 Extracting metrics..."
python extract_readme_metrics.py --update-readme

# 7. Generate visualizations
echo "🎨 Generating visualizations..."
python generate_benchmark_plots.py

echo "✅ Complete! Results in README.md and *.png files"

# 8. Cleanup
echo "🧹 Cleaning up..."
pkill -f "spring-boot:run" || true

echo "🎉 Done!"
```

Save as `complete_benchmark_workflow.sh` and run:

```bash
chmod +x complete_benchmark_workflow.sh
./complete_benchmark_workflow.sh
```

---

## 🆘 Getting Help

If things don't work:

1. **Check logs:** `tail -f benchmark_run_clean.log`
2. **Check service logs:** Framework logs in their respective directories
3. **Verify Java version:** Must be Java 21
4. **Check memory:** Need ~8GB free for all services
5. **Read methodology review:** See `BENCHMARK_METHODOLOGY_REVIEW.md` for known issues

---

**Questions?** Review the full documentation in:
- `README.md` - Project overview
- `BENCHMARK_RECOMMENDATIONS.md` - JMH vs Integration guidance
- `BENCHMARK_METHODOLOGY_REVIEW.md` - Methodology analysis
