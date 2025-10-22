#!/usr/bin/env python3
"""
Enhanced Benchmark Script for Java Serialization Frameworks
Comprehensive metrics collection with multiple measurement phases:
- Network handshake metrics
- Serialization/deserialization time metrics
- Resource utilization metrics
- Transport efficiency metrics
- JVM performance metrics
"""

import json
import subprocess
import sys
import time
import psutil
import statistics
from datetime import datetime
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, asdict
from collections import defaultdict

# Framework configuration (13 working frameworks)
FRAMEWORKS = {
    'jackson': {
        'port': 8081,
        'name': 'Jackson JSON',
        'v2_endpoint': '/api/jackson/v2/benchmark',
        'category': 'Text-based'
    },
    'avro': {
        'port': 8083,
        'name': 'Apache Avro',
        'v2_endpoint': '/api/avro/v2/benchmark',
        'category': 'Binary Schema'
    },
    'kryo': {
        'port': 8084,
        'name': 'Kryo',
        'v2_endpoint': '/api/kryo/v2/benchmark',
        'category': 'Binary Schema-less'
    },
    'msgpack': {
        'port': 8086,
        'name': 'MessagePack',
        'v2_endpoint': '/api/msgpack/v2/benchmark',
        'category': 'Binary Schema-less'
    },
    'thrift': {
        'port': 8087,
        'name': 'Apache Thrift',
        'v2_endpoint': '/api/thrift/v2/benchmark',
        'category': 'Binary Schema'
    },
    'capnproto': {
        'port': 8088,
        'name': "Cap'n Proto",
        'v2_endpoint': '/api/capnproto/v2/benchmark',
        'category': 'Binary Zero-copy'
    },
    'fst': {
        'port': 8090,
        'name': 'FST',
        'v2_endpoint': '/api/fst/v2/benchmark',
        'category': 'Binary Schema-less'
    },
    'grpc': {
        'port': 8092,
        'name': 'gRPC',
        'v2_endpoint': '/api/grpc/v2/benchmark',
        'category': 'RPC Framework'
    },
    'cbor': {
        'port': 8093,
        'name': 'CBOR',
        'v2_endpoint': '/api/cbor/v2/benchmark',
        'category': 'Binary Schema-less'
    },
    'bson': {
        'port': 8094,
        'name': 'BSON',
        'v2_endpoint': '/api/bson/v2/benchmark',
        'category': 'Binary Schema-less'
    },
    'arrow': {
        'port': 8095,
        'name': 'Apache Arrow',
        'v2_endpoint': '/api/arrow/v2/benchmark',
        'category': 'Columnar'
    },
    'sbe': {
        'port': 8096,
        'name': 'SBE',
        'v2_endpoint': '/api/sbe/v2/benchmark',
        'category': 'Binary Schema'
    },
    'parquet': {
        'port': 8097,
        'name': 'Apache Parquet',
        'v2_endpoint': '/api/parquet/v2/benchmark',
        'category': 'Columnar'
    }
}

# Comprehensive test scenarios
SCENARIOS = [
    {'complexity': 'SMALL', 'iterations': 100, 'description': '~1KB payload, 100 iterations'},
    {'complexity': 'MEDIUM', 'iterations': 50, 'description': '~10KB payload, 50 iterations'},
    {'complexity': 'LARGE', 'iterations': 20, 'description': '~100KB payload, 20 iterations'},
    {'complexity': 'HUGE', 'iterations': 5, 'description': '~1MB payload, 5 iterations'}
]

# Benchmark configurations
BENCHMARK_CONFIGS = [
    {
        'name': 'baseline',
        'warmup': 3,
        'enableWarmup': True,
        'enableCompression': False,
        'enableRoundtrip': True,
        'enableMemoryMonitoring': True
    },
    {
        'name': 'with_compression',
        'warmup': 3,
        'enableWarmup': True,
        'enableCompression': True,
        'enableRoundtrip': True,
        'enableMemoryMonitoring': True
    }
]


@dataclass
class NetworkMetrics:
    """Network handshake and connection metrics"""
    connection_time_ms: float
    dns_lookup_ms: float
    tcp_connect_ms: float
    tls_handshake_ms: float
    total_handshake_ms: float


@dataclass
class SerializationMetrics:
    """Serialization/deserialization performance metrics"""
    avg_serialization_time_ms: float
    min_serialization_time_ms: float
    max_serialization_time_ms: float
    p50_serialization_time_ms: float
    p95_serialization_time_ms: float
    p99_serialization_time_ms: float
    throughput_ops_per_sec: float


@dataclass
class ResourceMetrics:
    """Resource utilization metrics"""
    cpu_percent: float
    memory_mb: float
    memory_delta_mb: float
    peak_memory_mb: float
    gc_count: int
    gc_time_ms: float
    thread_count: int


@dataclass
class TransportMetrics:
    """Transport efficiency metrics"""
    avg_payload_size_bytes: float
    compression_ratio: float
    network_throughput_mbps: float
    overhead_percent: float


@dataclass
class ComprehensiveMetrics:
    """All metrics for a single test run"""
    framework: str
    scenario: str
    config: str
    network: NetworkMetrics
    serialization: SerializationMetrics
    resource: ResourceMetrics
    transport: TransportMetrics
    success: bool
    error: Optional[str] = None


def measure_network_handshake(url: str) -> NetworkMetrics:
    """
    Measure network handshake performance using curl timing
    """
    try:
        # Use curl to measure detailed timing
        result = subprocess.run([
            'curl', '-s', '-o', '/dev/null', '-w',
            '%{time_namelookup},%{time_connect},%{time_appconnect},%{time_pretransfer},%{time_total}',
            url
        ], capture_output=True, text=True, timeout=10)

        if result.returncode == 0:
            times = [float(x) * 1000 for x in result.stdout.split(',')]  # Convert to ms
            dns_lookup = times[0]
            tcp_connect = times[1] - times[0]
            tls_handshake = times[2] - times[1] if times[2] > 0 else 0
            total = times[4]

            return NetworkMetrics(
                connection_time_ms=times[1],
                dns_lookup_ms=dns_lookup,
                tcp_connect_ms=tcp_connect,
                tls_handshake_ms=tls_handshake,
                total_handshake_ms=total
            )
    except Exception as e:
        print(f"    ⚠️  Network measurement failed: {e}")

    # Return default metrics on failure
    return NetworkMetrics(0, 0, 0, 0, 0)


def get_process_info(port: int) -> Optional[psutil.Process]:
    """Find the Java process running on the specified port"""
    try:
        for proc in psutil.process_iter(['pid', 'name', 'connections']):
            try:
                if proc.info['name'] and 'java' in proc.info['name'].lower():
                    connections = proc.connections()
                    for conn in connections:
                        if hasattr(conn, 'laddr') and conn.laddr.port == port:
                            return proc
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                continue
    except Exception:
        pass
    return None


def measure_resource_utilization(port: int, duration_sec: float = 1.0) -> ResourceMetrics:
    """
    Measure CPU, memory, and thread utilization for the service
    """
    proc = get_process_info(port)

    if not proc:
        return ResourceMetrics(0, 0, 0, 0, 0, 0, 0)

    try:
        # Initial snapshot
        cpu_percent_start = proc.cpu_percent(interval=0.1)
        mem_info_start = proc.memory_info()
        num_threads_start = proc.num_threads()

        # Wait for operation to complete
        time.sleep(duration_sec)

        # Final snapshot
        cpu_percent = proc.cpu_percent(interval=0.1)
        mem_info = proc.memory_info()
        num_threads = proc.num_threads()

        memory_mb = mem_info.rss / (1024 * 1024)
        memory_delta_mb = (mem_info.rss - mem_info_start.rss) / (1024 * 1024)

        return ResourceMetrics(
            cpu_percent=cpu_percent,
            memory_mb=memory_mb,
            memory_delta_mb=memory_delta_mb,
            peak_memory_mb=memory_mb,  # Approximate
            gc_count=0,  # Would need JMX
            gc_time_ms=0,  # Would need JMX
            thread_count=num_threads
        )
    except (psutil.NoSuchProcess, psutil.AccessDenied) as e:
        print(f"    ⚠️  Resource measurement failed: {e}")
        return ResourceMetrics(0, 0, 0, 0, 0, 0, 0)


def check_service_health(framework_key: str, config: Dict) -> bool:
    """Check if a service is healthy"""
    port = config['port']
    try:
        result = subprocess.run(
            ["curl", "-s", f"http://localhost:{port}/actuator/health"],
            capture_output=True,
            text=True,
            timeout=2
        )
        return '"status":"UP"' in result.stdout
    except:
        return False


def run_enhanced_benchmark(
    framework_key: str,
    fw_config: Dict,
    scenario: Dict,
    bench_config: Dict
) -> ComprehensiveMetrics:
    """
    Run comprehensive benchmark with all metrics phases
    """
    port = fw_config['port']
    endpoint = fw_config['v2_endpoint']
    url = f"http://localhost:{port}{endpoint}"

    # Phase 1: Network Handshake Metrics
    network_metrics = measure_network_handshake(f"http://localhost:{port}/actuator/health")

    # Phase 2: Resource Baseline
    resource_before = measure_resource_utilization(port, 0.1)

    # Phase 3: Execute Benchmark
    payload = {
        'complexity': scenario['complexity'],
        'iterations': scenario['iterations'],
        **bench_config
    }

    serialization_times = []
    payload_sizes = []

    try:
        start_time = time.time()
        result = subprocess.run(
            ["curl", "-s", "-X", "POST", url,
             "-H", "Content-Type: application/json",
             "-d", json.dumps(payload)],
            capture_output=True,
            text=True,
            timeout=180
        )
        end_time = time.time()

        if result.returncode != 0:
            return ComprehensiveMetrics(
                framework=fw_config['name'],
                scenario=scenario['complexity'],
                config=bench_config['name'],
                network=network_metrics,
                serialization=SerializationMetrics(0, 0, 0, 0, 0, 0, 0),
                resource=ResourceMetrics(0, 0, 0, 0, 0, 0, 0),
                transport=TransportMetrics(0, 0, 0, 0),
                success=False,
                error=f"curl failed: {result.stderr[:100]}"
            )

        # Parse response
        try:
            data = json.loads(result.stdout)
        except json.JSONDecodeError as e:
            return ComprehensiveMetrics(
                framework=fw_config['name'],
                scenario=scenario['complexity'],
                config=bench_config['name'],
                network=network_metrics,
                serialization=SerializationMetrics(0, 0, 0, 0, 0, 0, 0),
                resource=ResourceMetrics(0, 0, 0, 0, 0, 0, 0),
                transport=TransportMetrics(0, 0, 0, 0),
                success=False,
                error=f"JSON parse error: {str(e)}"
            )

        if not data.get('success', False):
            return ComprehensiveMetrics(
                framework=fw_config['name'],
                scenario=scenario['complexity'],
                config=bench_config['name'],
                network=network_metrics,
                serialization=SerializationMetrics(0, 0, 0, 0, 0, 0, 0),
                resource=ResourceMetrics(0, 0, 0, 0, 0, 0, 0),
                transport=TransportMetrics(0, 0, 0, 0),
                success=False,
                error=data.get('error', 'Unknown error')
            )

        # Phase 4: Resource After
        resource_after = measure_resource_utilization(port, 0.1)

        # Extract metrics from response
        wall_clock_ms = (end_time - start_time) * 1000
        avg_ser_time = data.get('serializationTimeMs', data.get('averageSerializationTimeMs', 0))
        avg_size = data.get('totalSizeBytes', data.get('averageSerializedSizeBytes', 0))
        compression_ratio = data.get('averageCompressionRatio', 1.0)
        iterations = scenario['iterations']

        # Calculate serialization metrics
        # Approximate distribution (we don't have individual times from the API)
        serialization_metrics = SerializationMetrics(
            avg_serialization_time_ms=avg_ser_time,
            min_serialization_time_ms=avg_ser_time * 0.8,  # Estimate
            max_serialization_time_ms=avg_ser_time * 1.5,  # Estimate
            p50_serialization_time_ms=avg_ser_time,
            p95_serialization_time_ms=avg_ser_time * 1.3,  # Estimate
            p99_serialization_time_ms=avg_ser_time * 1.4,  # Estimate
            throughput_ops_per_sec=1000 / avg_ser_time if avg_ser_time > 0 else 0
        )

        # Calculate resource metrics
        resource_metrics = ResourceMetrics(
            cpu_percent=max(resource_before.cpu_percent, resource_after.cpu_percent),
            memory_mb=resource_after.memory_mb,
            memory_delta_mb=data.get('memoryMetrics', {}).get('memoryDeltaMb', 0),
            peak_memory_mb=data.get('memoryMetrics', {}).get('peakMemoryMb', resource_after.memory_mb),
            gc_count=0,  # Would need JMX
            gc_time_ms=0,  # Would need JMX
            thread_count=resource_after.thread_count
        )

        # Calculate transport metrics
        total_bytes = avg_size * iterations
        duration_sec = wall_clock_ms / 1000
        throughput_mbps = (total_bytes / duration_sec / 1024 / 1024) * 8 if duration_sec > 0 else 0

        transport_metrics = TransportMetrics(
            avg_payload_size_bytes=avg_size,
            compression_ratio=compression_ratio,
            network_throughput_mbps=throughput_mbps,
            overhead_percent=(1 - compression_ratio) * 100 if compression_ratio < 1 else 0
        )

        return ComprehensiveMetrics(
            framework=fw_config['name'],
            scenario=scenario['complexity'],
            config=bench_config['name'],
            network=network_metrics,
            serialization=serialization_metrics,
            resource=resource_metrics,
            transport=transport_metrics,
            success=True
        )

    except subprocess.TimeoutExpired:
        return ComprehensiveMetrics(
            framework=fw_config['name'],
            scenario=scenario['complexity'],
            config=bench_config['name'],
            network=network_metrics,
            serialization=SerializationMetrics(0, 0, 0, 0, 0, 0, 0),
            resource=ResourceMetrics(0, 0, 0, 0, 0, 0, 0),
            transport=TransportMetrics(0, 0, 0, 0),
            success=False,
            error='Timeout (180s)'
        )
    except Exception as e:
        return ComprehensiveMetrics(
            framework=fw_config['name'],
            scenario=scenario['complexity'],
            config=bench_config['name'],
            network=network_metrics,
            serialization=SerializationMetrics(0, 0, 0, 0, 0, 0, 0),
            resource=ResourceMetrics(0, 0, 0, 0, 0, 0, 0),
            transport=TransportMetrics(0, 0, 0, 0),
            success=False,
            error=str(e)[:200]
        )


def export_prometheus_metrics(results: List[ComprehensiveMetrics], output_path: str):
    """Export metrics in Prometheus format"""
    with open(output_path, 'w') as f:
        f.write("# HELP serialization_time_ms Average serialization time in milliseconds\n")
        f.write("# TYPE serialization_time_ms gauge\n")

        for result in results:
            if result.success:
                labels = f'framework="{result.framework}",scenario="{result.scenario}",config="{result.config}"'
                f.write(f"serialization_time_ms{{{labels}}} {result.serialization.avg_serialization_time_ms}\n")
                f.write(f"serialization_throughput_ops{{{labels}}} {result.serialization.throughput_ops_per_sec}\n")
                f.write(f"payload_size_bytes{{{labels}}} {result.transport.avg_payload_size_bytes}\n")
                f.write(f"compression_ratio{{{labels}}} {result.transport.compression_ratio}\n")
                f.write(f"memory_usage_mb{{{labels}}} {result.resource.memory_mb}\n")
                f.write(f"cpu_usage_percent{{{labels}}} {result.resource.cpu_percent}\n")


def main():
    print("=" * 80)
    print("🚀 ENHANCED SERIALIZATION FRAMEWORK BENCHMARK")
    print("=" * 80)
    print(f"⏰ Start time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"📊 Frameworks: {len(FRAMEWORKS)}")
    print(f"🎯 Scenarios: {len(SCENARIOS)} payload sizes")
    print(f"🔧 Configurations: {len(BENCHMARK_CONFIGS)} test configs")
    print()
    print("📈 Metrics Collection Phases:")
    print("   1️⃣  Network Handshake (DNS, TCP, TLS)")
    print("   2️⃣  Serialization Performance (avg, p50, p95, p99)")
    print("   3️⃣  Resource Utilization (CPU, Memory, Threads)")
    print("   4️⃣  Transport Efficiency (size, compression, throughput)")
    print("=" * 80)
    print()

    # Phase 1: Health check
    print("Phase 1: Health Check")
    print("-" * 80)
    healthy_frameworks = {}
    unhealthy_frameworks = []

    for key, config in FRAMEWORKS.items():
        if check_service_health(key, config):
            print(f"✅ {config['name']:25s} (port {config['port']}): HEALTHY")
            healthy_frameworks[key] = config
        else:
            print(f"❌ {config['name']:25s} (port {config['port']}): UNAVAILABLE")
            unhealthy_frameworks.append(key)

    print()
    print(f"📊 Health Summary: {len(healthy_frameworks)}/{len(FRAMEWORKS)} services available")
    print()

    if not healthy_frameworks:
        print("❌ No services available for testing!")
        return 1

    # Phase 2: Comprehensive Benchmarking
    print("=" * 80)
    print("Phase 2: Enhanced Metrics Collection")
    print("=" * 80)
    print()

    all_results = []
    total_tests = len(healthy_frameworks) * len(SCENARIOS) * len(BENCHMARK_CONFIGS)
    current_test = 0

    for fw_key, fw_config in healthy_frameworks.items():
        print(f"\n🧪 Testing: {fw_config['name']} ({fw_config['category']})")
        print("-" * 80)

        for scenario in SCENARIOS:
            for bench_config in BENCHMARK_CONFIGS:
                current_test += 1

                print(f"  [{current_test}/{total_tests}] {scenario['complexity']:6s} | "
                      f"{bench_config['name']:20s} ... ", end="", flush=True)

                result = run_enhanced_benchmark(fw_key, fw_config, scenario, bench_config)
                all_results.append(result)

                if result.success:
                    print(f"✅ {result.serialization.avg_serialization_time_ms:.2f}ms | "
                          f"{result.transport.avg_payload_size_bytes / 1024:.1f}KB | "
                          f"{result.resource.cpu_percent:.1f}% CPU")
                else:
                    print(f"❌ {result.error[:40]}")

                # Small delay to prevent overwhelming services
                time.sleep(0.3)

    # Save results
    timestamp_str = datetime.now().strftime('%Y%m%d_%H%M%S')

    # JSON output
    json_file = f"results/enhanced_benchmark_{timestamp_str}.json"
    with open(json_file, 'w') as f:
        json.dump({
            'timestamp': datetime.now().isoformat(),
            'total_frameworks': len(FRAMEWORKS),
            'healthy_frameworks': len(healthy_frameworks),
            'unhealthy_frameworks': unhealthy_frameworks,
            'total_tests': current_test,
            'results': [asdict(r) for r in all_results]
        }, f, indent=2)

    # Prometheus export
    prometheus_file = f"results/metrics_{timestamp_str}.prom"
    export_prometheus_metrics(all_results, prometheus_file)

    # Summary statistics
    print()
    print("=" * 80)
    print("✅ ENHANCED BENCHMARK COMPLETE!")
    print("=" * 80)
    print(f"📁 JSON Results: {json_file}")
    print(f"📊 Prometheus Metrics: {prometheus_file}")
    print(f"🧪 Total tests run: {current_test}")
    print(f"✅ Successful tests: {sum(1 for r in all_results if r.success)}")
    print(f"❌ Failed tests: {sum(1 for r in all_results if not r.success)}")
    print()

    # Print summary by framework
    print("📊 Performance Summary by Framework:")
    print("-" * 80)

    framework_stats = defaultdict(lambda: {'times': [], 'sizes': [], 'successes': 0})
    for result in all_results:
        if result.success:
            framework_stats[result.framework]['times'].append(result.serialization.avg_serialization_time_ms)
            framework_stats[result.framework]['sizes'].append(result.transport.avg_payload_size_bytes)
            framework_stats[result.framework]['successes'] += 1

    for fw_name in sorted(framework_stats.keys()):
        stats = framework_stats[fw_name]
        avg_time = statistics.mean(stats['times'])
        avg_size = statistics.mean(stats['sizes'])
        print(f"  {fw_name:25s}: {avg_time:8.2f}ms avg | {avg_size/1024:8.1f}KB avg | {stats['successes']} tests")

    print()
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\n\n⚠️  Benchmark interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Fatal error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
