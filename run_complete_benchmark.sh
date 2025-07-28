#!/bin/bash

# Complete Benchmark Runner for Java Serialization Frameworks (2025)
# This script compiles all projects, starts all frameworks, and runs comprehensive benchmarks

echo "🚀 Java Serialization Framework Complete Benchmark Runner"
echo "========================================================="
echo "⏰ Start time: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# Check prerequisites
echo "🔍 Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 21+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21+ required. Found Java $JAVA_VERSION"
    exit 1
fi
echo "✅ Java $JAVA_VERSION found"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven 3.8+"
    exit 1
fi
echo "✅ Maven found"

# Check Python
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 not found. Please install Python 3.8+"
    exit 1
fi
echo "✅ Python 3 found"

# Check virtual environment
if [ ! -d "venv" ]; then
    echo "📦 Creating Python virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
echo "🔧 Activating virtual environment..."
source venv/bin/activate

# Install Python dependencies
echo "📦 Installing Python dependencies..."
pip install requests -q

echo ""
echo "🏗️ STEP 1: Compiling all projects..."
echo "====================================="

# Clean and compile all projects
mvn clean install -q -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi
echo "✅ All projects compiled successfully"

echo ""
echo "🚀 STEP 2: Starting all frameworks..."
echo "====================================="

# Start all frameworks in background
python start_all_frameworks_comprehensive.py &
LAUNCHER_PID=$!

echo "⏳ Waiting 60 seconds for all frameworks to start..."
sleep 60

# Check if launcher is still running
if ! kill -0 $LAUNCHER_PID 2>/dev/null; then
    echo "❌ Framework launcher failed to start"
    exit 1
fi

echo "✅ Framework launcher is running (PID: $LAUNCHER_PID)"

echo ""
echo "🧪 STEP 3: Running comprehensive benchmark..."
echo "============================================="

# Run the comprehensive benchmark
python final_comprehensive_benchmark.py

BENCHMARK_EXIT_CODE=$?

echo ""
echo "🛑 STEP 4: Cleanup..."
echo "===================="

# Stop all frameworks
echo "🛑 Stopping all frameworks..."
kill $LAUNCHER_PID 2>/dev/null

# Kill any remaining Spring Boot processes
pkill -f "spring-boot:run" 2>/dev/null

echo "✅ Cleanup completed"

echo ""
echo "📊 BENCHMARK COMPLETE!"
echo "======================"

if [ $BENCHMARK_EXIT_CODE -eq 0 ]; then
    echo "✅ Benchmark completed successfully"
    echo "📄 Check the generated JSON file for detailed results"
    echo "📊 Review the console output above for performance rankings"
else
    echo "❌ Benchmark completed with errors (exit code: $BENCHMARK_EXIT_CODE)"
fi

echo ""
echo "🎯 Next Steps:"
echo "- Review the generated benchmark results JSON file"
echo "- Check individual framework logs if needed"
echo "- Run individual framework tests for deeper analysis"
echo ""

# Display latest results file
LATEST_RESULTS=$(ls -t final_comprehensive_benchmark_*.json 2>/dev/null | head -n 1)
if [ -n "$LATEST_RESULTS" ]; then
    echo "📄 Latest results file: $LATEST_RESULTS"
fi

echo "⏰ Total runtime: $(($(date +%s) - $(date -d "$(date '+%Y-%m-%d %H:%M:%S')" +%s))) seconds"
echo "🏁 Complete benchmark finished at: $(date '+%Y-%m-%d %H:%M:%S')"
