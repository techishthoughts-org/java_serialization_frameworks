#!/usr/bin/env bash
# ============================================================================
# Java Serialization Frameworks - Unified Management Script
# ============================================================================
# Manages 13 serialization frameworks with comprehensive operations
# Author: Tech.ish Thoughts
# Version: 2.0.0
# Date: 2025-10-22
# ============================================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${BASE_DIR}/logs"
RESULTS_DIR="${BASE_DIR}/results"

# Framework configuration (13 working frameworks)
declare -A FRAMEWORKS=(
    [jackson]="8081:Jackson JSON:Text-based"
    [avro]="8083:Apache Avro:Binary Schema"
    [kryo]="8084:Kryo:Binary Schema-less"
    [msgpack]="8086:MessagePack:Binary Schema-less"
    [thrift]="8087:Apache Thrift:Binary Schema"
    [capnproto]="8088:Cap'n Proto:Binary Zero-copy"
    [fst]="8090:FST:Binary Schema-less"
    [grpc]="8092:gRPC:RPC Framework"
    [cbor]="8093:CBOR:Binary Schema-less"
    [bson]="8094:BSON:Binary Schema-less"
    [arrow]="8095:Apache Arrow:Columnar"
    [sbe]="8096:SBE:Binary Schema"
    [parquet]="8097:Apache Parquet:Columnar"
)

# ============================================================================
# Utility Functions
# ============================================================================

log_info() {
    echo -e "${BLUE}ℹ${NC}  $1"
}

log_success() {
    echo -e "${GREEN}✓${NC}  $1"
}

log_warn() {
    echo -e "${YELLOW}⚠${NC}  $1"
}

log_error() {
    echo -e "${RED}✗${NC}  $1"
}

print_header() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}  $1"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# ============================================================================
# Service Management Functions
# ============================================================================

check_service_health() {
    local port=$1
    local status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${port}/actuator/health --max-time 2 2>/dev/null || echo "down")
    if [ "$status" = "200" ]; then
        return 0
    else
        return 1
    fi
}

start_service() {
    local key=$1
    local config="${FRAMEWORKS[$key]}"
    IFS=':' read -r port name category <<< "$config"
    local dir="${BASE_DIR}/${key}-poc"

    if check_service_health "$port"; then
        log_warn "$name already running on port $port"
        return 0
    fi

    log_info "Starting $name on port $port..."
    mkdir -p "$LOG_DIR"

    cd "$dir"
    nohup mvn spring-boot:run > "${LOG_DIR}/${key}.log" 2>&1 &
    echo $! > "${LOG_DIR}/${key}.pid"

    # Wait for service to start
    local retries=0
    while [ $retries -lt 30 ]; do
        if check_service_health "$port"; then
            log_success "$name started successfully"
            return 0
        fi
        sleep 2
        ((retries++))
    done

    log_error "$name failed to start"
    return 1
}

stop_service() {
    local key=$1
    local config="${FRAMEWORKS[$key]}"
    IFS=':' read -r port name category <<< "$config"
    local pid_file="${LOG_DIR}/${key}.pid"

    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            log_info "Stopping $name (PID: $pid)..."
            kill "$pid"
            rm "$pid_file"
            log_success "$name stopped"
        else
            log_warn "$name not running (stale PID file)"
            rm "$pid_file"
        fi
    else
        log_warn "$name not running"
    fi
}

# ============================================================================
# Bulk Operations
# ============================================================================

start_all() {
    print_header "Starting All Services"
    local count=0
    for key in "${!FRAMEWORKS[@]}"; do
        start_service "$key" && ((count++)) || true
    done
    echo ""
    log_success "Started $count/${#FRAMEWORKS[@]} services"
}

stop_all() {
    print_header "Stopping All Services"
    for key in "${!FRAMEWORKS[@]}"; do
        stop_service "$key"
    done
    log_success "All services stopped"
}

status_all() {
    print_header "Service Status"
    printf "%-20s %-25s %-20s %-10s\n" "KEY" "NAME" "CATEGORY" "STATUS"
    echo "────────────────────────────────────────────────────────────────────────────"

    local running=0
    for key in $(echo "${!FRAMEWORKS[@]}" | tr ' ' '\n' | sort); do
        local config="${FRAMEWORKS[$key]}"
        IFS=':' read -r port name category <<< "$config"

        if check_service_health "$port"; then
            printf "%-20s %-25s %-20s ${GREEN}%-10s${NC}\n" "$key" "$name" "$category" "RUNNING"
            ((running++))
        else
            printf "%-20s %-25s %-20s ${RED}%-10s${NC}\n" "$key" "$name" "$category" "STOPPED"
        fi
    done
    echo ""
    log_info "Running: $running/${#FRAMEWORKS[@]} services"
}

# ============================================================================
# Benchmark Operations
# ============================================================================

run_benchmark() {
    print_header "Running Comprehensive Benchmark"

    # Check if services are running
    local running_count=0
    for key in "${!FRAMEWORKS[@]}"; do
        local config="${FRAMEWORKS[$key]}"
        IFS=':' read -r port name category <<< "$config"
        check_service_health "$port" && ((running_count++)) || true
    done

    if [ $running_count -eq 0 ]; then
        log_error "No services running! Start services first with: ./manage.sh start"
        exit 1
    fi

    log_info "Found $running_count/${#FRAMEWORKS[@]} services running"
    mkdir -p "$RESULTS_DIR"

    log_info "Executing enhanced benchmark..."
    python3 enhanced_benchmark.py

    if [ $? -eq 0 ]; then
        log_success "Benchmark completed! Results in $RESULTS_DIR"
    else
        log_error "Benchmark failed"
        exit 1
    fi
}

analyze_results() {
    print_header "Analyzing Results"

    local latest_result=$(ls -t "$RESULTS_DIR"/benchmark_*.json 2>/dev/null | head -1)
    if [ -z "$latest_result" ]; then
        log_error "No benchmark results found"
        exit 1
    fi

    log_info "Analyzing: $(basename $latest_result)"
    python3 analyze_metrics.py "$latest_result"
}

# ============================================================================
# Main Command Handler
# ============================================================================

show_usage() {
    cat << EOF
╔════════════════════════════════════════════════════════════════╗
║  Java Serialization Frameworks - Management Script v2.0.0     ║
╚════════════════════════════════════════════════════════════════╝

USAGE:
    ./manage.sh <command> [options]

COMMANDS:
    start               Start all 13 framework services
    stop                Stop all running services
    restart             Restart all services
    status              Show status of all services

    benchmark           Run comprehensive performance benchmark
    analyze             Analyze latest benchmark results

    logs <framework>    Show logs for specific framework
    clean               Clean all logs and temporary files

    help                Show this help message

EXAMPLES:
    ./manage.sh start           # Start all services
    ./manage.sh status          # Check which services are running
    ./manage.sh benchmark       # Run benchmark (services must be running)
    ./manage.sh analyze         # Analyze latest results
    ./manage.sh logs jackson    # View Jackson service logs
    ./manage.sh stop            # Stop all services

FRAMEWORKS (13):
    jackson, avro, kryo, msgpack, thrift, capnproto, fst,
    grpc, cbor, bson, arrow, sbe, parquet

EOF
}

# Main command dispatcher
case "${1:-help}" in
    start)
        start_all
        ;;
    stop)
        stop_all
        ;;
    restart)
        stop_all
        sleep 2
        start_all
        ;;
    status)
        status_all
        ;;
    benchmark)
        run_benchmark
        ;;
    analyze)
        analyze_results
        ;;
    logs)
        if [ -z "${2:-}" ]; then
            log_error "Please specify a framework: ./manage.sh logs <framework>"
            exit 1
        fi
        tail -f "${LOG_DIR}/${2}.log"
        ;;
    clean)
        log_info "Cleaning logs and temporary files..."
        rm -rf "$LOG_DIR" "$RESULTS_DIR"/*.log
        log_success "Cleanup complete"
        ;;
    help|--help|-h)
        show_usage
        ;;
    *)
        log_error "Unknown command: $1"
        show_usage
        exit 1
        ;;
esac
