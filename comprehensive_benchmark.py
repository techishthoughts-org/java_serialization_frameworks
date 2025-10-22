#!/usr/bin/env python3
"""
Comprehensive Benchmark Script for All 15 Java Serialization Frameworks
Collects deep metrics for comparison analysis
"""
import json
import subprocess
import sys
import time
from datetime import datetime
from typing import Dict, List, Any

# All 15 frameworks configuration
FRAMEWORKS = {
    'jackson': {
        'port': 8081,
        'name': 'Jackson JSON',
        'v2_endpoint': '/api/jackson/v2/benchmark',
        'category': 'Text-based'
    },
    'protobuf': {
        'port': 8082,
        'name': 'Protocol Buffers',
        'v2_endpoint': '/api/protobuf/v2/benchmark',
        'category': 'Binary Schema'
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
    'flatbuffers': {
        'port': 8091,
        'name': 'FlatBuffers',
        'v2_endpoint': '/api/flatbuffers/v2/benchmark',
        'category': 'Binary Zero-copy'
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

# V2 Benchmark configuration options
BENCHMARK_CONFIGS = [
    {
        'name': 'baseline',
        'warmup': 3,
        'compression': False,
        'roundtrip': True,
        'memoryMonitoring': True
    },
    {
        'name': 'with_compression',
        'warmup': 3,
        'compression': True,
        'roundtrip': True,
        'memoryMonitoring': True
    }
]


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


def run_v2_benchmark(framework_key: str, config: Dict, scenario: Dict, bench_config: Dict) -> Dict[str, Any]:
    """Run V2 benchmark API call"""
    port = config['port']
    endpoint = config['v2_endpoint']
    url = f"http://localhost:{port}{endpoint}"

    payload = {
        'complexity': scenario['complexity'],
        'iterations': scenario['iterations'],
        'config': bench_config
    }

    try:
        start_time = time.time()
        result = subprocess.run(
            ["curl", "-s", "-X", "POST", url,
             "-H", "Content-Type: application/json",
             "-d", json.dumps(payload)],
            capture_output=True,
            text=True,
            timeout=120
        )
        end_time = time.time()

        if result.returncode == 0:
            try:
                data = json.loads(result.stdout)
                data['wall_clock_time_ms'] = (end_time - start_time) * 1000
                return {'success': True, 'data': data}
            except json.JSONDecodeError as e:
                return {'success': False, 'error': f'JSON parse error: {str(e)}', 'raw': result.stdout[:200]}
        else:
            return {'success': False, 'error': f'curl failed: {result.stderr[:200]}'}
    except subprocess.TimeoutExpired:
        return {'success': False, 'error': 'Timeout (120s)'}
    except Exception as e:
        return {'success': False, 'error': str(e)[:200]}


def main():
    print("=" * 80)
    print("🚀 COMPREHENSIVE SERIALIZATION FRAMEWORK BENCHMARK")
    print("=" * 80)
    print(f"⏰ Start time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"📊 Frameworks: {len(FRAMEWORKS)}")
    print(f"🎯 Scenarios: {len(SCENARIOS)} payload sizes")
    print(f"🔧 Configurations: {len(BENCHMARK_CONFIGS)} test configs")
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
    print("Phase 2: Comprehensive Benchmarking")
    print("=" * 80)
    print()

    results = {
        'timestamp': datetime.now().isoformat(),
        'total_frameworks': len(FRAMEWORKS),
        'healthy_frameworks': len(healthy_frameworks),
        'unhealthy_frameworks': unhealthy_frameworks,
        'scenarios': SCENARIOS,
        'benchmark_configs': BENCHMARK_CONFIGS,
        'results': {}
    }

    total_tests = len(healthy_frameworks) * len(SCENARIOS) * len(BENCHMARK_CONFIGS)
    current_test = 0

    for fw_key, fw_config in healthy_frameworks.items():
        print(f"\n🧪 Testing: {fw_config['name']} ({fw_config['category']})")
        print("-" * 80)

        fw_results = {
            'name': fw_config['name'],
            'port': fw_config['port'],
            'category': fw_config['category'],
            'tests': {}
        }

        for scenario in SCENARIOS:
            for bench_config in BENCHMARK_CONFIGS:
                current_test += 1
                test_id = f"{scenario['complexity']}_{bench_config['name']}"

                print(f"  [{current_test}/{total_tests}] {scenario['complexity']:6s} | "
                      f"{bench_config['name']:20s} ... ", end="", flush=True)

                result = run_v2_benchmark(fw_key, fw_config, scenario, bench_config)

                if result['success']:
                    data = result['data']
                    print(f"✅ {data.get('wall_clock_time_ms', 0):.1f}ms")
                    fw_results['tests'][test_id] = {
                        'scenario': scenario,
                        'config': bench_config,
                        'result': data,
                        'success': True
                    }
                else:
                    print(f"❌ {result['error'][:50]}")
                    fw_results['tests'][test_id] = {
                        'scenario': scenario,
                        'config': bench_config,
                        'error': result['error'],
                        'success': False
                    }

                # Small delay to prevent overwhelming services
                time.sleep(0.5)

        results['results'][fw_key] = fw_results

    # Save results
    timestamp_str = datetime.now().strftime('%Y%m%d_%H%M%S')
    output_file = f"comprehensive_benchmark_{timestamp_str}.json"

    with open(output_file, 'w') as f:
        json.dump(results, f, indent=2)

    print()
    print("=" * 80)
    print("✅ BENCHMARK COMPLETE!")
    print("=" * 80)
    print(f"📁 Results saved to: {output_file}")
    print(f"📊 Total tests run: {current_test}")
    print(f"⏱️  Duration: {(time.time() - time.time()) / 60:.1f} minutes")
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
        sys.exit(1)
