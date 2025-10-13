#!/bin/bash
##
# Complete Benchmark Automation Script
#
# Runs the complete benchmark workflow with automatic report generation.
# This script coordinates JMH benchmarks, integration tests, and report generation.
#
# Usage:
#   ./run_complete_benchmark.sh [options]
#
# Options:
#   --quick              Run quick benchmarks (fewer iterations)
#   --skip-jmh           Skip JMH benchmarks (use cached results)
#   --skip-integration   Skip integration tests (use cached results)
#   --format FORMAT      Report format: pdf, html, json, csv, all (default: all)
#   --output DIR         Output directory (default: ./reports)
#   --help               Show this help message
#
# Examples:
#   # Run complete workflow
#   ./run_complete_benchmark.sh
#
#   # Quick benchmark with HTML reports
#   ./run_complete_benchmark.sh --quick --format html
#
#   # Use cached JMH results, regenerate reports
#   ./run_complete_benchmark.sh --skip-jmh --skip-integration --format all
##

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"

    # Check Python 3
    if ! command -v python3 &> /dev/null; then
        print_error "Python 3 is required but not installed"
        exit 1
    fi
    print_success "Python 3 found: $(python3 --version)"

    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is required but not installed"
        exit 1
    fi
    print_success "Java found: $(java -version 2>&1 | head -n 1)"

    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is required but not installed"
        exit 1
    fi
    print_success "Maven found: $(mvn -version | head -n 1)"

    # Check Python dependencies
    print_header "Checking Python Dependencies"

    if python3 -c "import requests" 2>/dev/null; then
        print_success "requests module found"
    else
        print_warning "requests module not found - installing..."
        pip3 install requests
    fi

    # Optional dependencies
    if python3 -c "import matplotlib" 2>/dev/null; then
        print_success "matplotlib found (charts enabled)"
    else
        print_warning "matplotlib not found (charts disabled)"
        echo "  Install with: pip3 install matplotlib"
    fi

    if python3 -c "import pandas" 2>/dev/null; then
        print_success "pandas found (CSV export enabled)"
    else
        print_warning "pandas not found (CSV export disabled)"
        echo "  Install with: pip3 install pandas"
    fi

    if python3 -c "import reportlab" 2>/dev/null; then
        print_success "reportlab found (PDF export enabled)"
    else
        print_warning "reportlab not found (PDF export disabled)"
        echo "  Install with: pip3 install reportlab"
    fi

    echo ""
}

# Show help
show_help() {
    head -n 30 "$0" | grep "^#" | sed 's/^# \?//'
    exit 0
}

# Parse arguments
ARGS="$@"
if [[ "$*" == *"--help"* ]]; then
    show_help
fi

# Main execution
print_header "🚀 Complete Benchmark Automation"
echo "Starting comprehensive benchmark workflow..."
echo "Output directory: ${OUTPUT_DIR:-./reports}"
echo ""

# Check prerequisites
check_prerequisites

# Run Python orchestration script
print_header "Running Benchmark Workflow"
echo "This may take 20-60 minutes depending on options"
echo "Progress will be shown below..."
echo ""

if python3 run_complete_benchmark_with_reports.py $ARGS; then
    print_success "Benchmark workflow completed successfully!"
    echo ""
    echo "📄 Check the ./reports directory for generated files"
    echo "📊 Open benchmark_report_*.html in your browser for interactive results"
    exit 0
else
    print_error "Benchmark workflow failed"
    exit 1
fi
