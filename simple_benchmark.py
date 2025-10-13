#!/usr/bin/env python3
"""Simple benchmark using only V1 POST endpoints that are working"""
import json
import time
import statistics
from datetime import datetime
import requests

# Only the 6 frameworks that are currently running
FRAMEWORKS = {
    'jackson': {
        'port': 8081,
        'name': 'Jackson JSON',
        'endpoint': '/api/jackson/benchmark/serialization'
    },
    'avro': {
        'port': 8083,
        'name': 'Apache Avro',
        'endpoint': '/api/avro/benchmark/serialization'
    },
    'kryo': {
        'port': 8084,
        'name': 'Kryo',
        'endpoint': '/api/kryo/benchmark/serialization'
    },
    'msgpack': {
        'port': 8086,
        'name': 'MessagePack',
        'endpoint': '/api/msgpack/benchmark/serialization'
    },
    'flatbuffers': {
        'port': 8091,
        'name': 'FlatBuffers',
        'endpoint': '/api/flatbuffers/benchmark/serialization'
    },
    'grpc': {
        'port': 8092,
        'name': 'gRPC',
        'endpoint': '/api/grpc/benchmark/serialization'
    }
}

SCENARIOS = [
    {'complexity': 'SMALL', 'iterations': 50},
    {'complexity': 'MEDIUM', 'iterations': 25},
    {'complexity': 'LARGE', 'iterations': 10}
]

def test_framework(framework_key, framework_info, scenario):
    """Test a single framework with a scenario"""
    port = framework_info['port']
    endpoint = framework_info['endpoint']
    url = f"http://localhost:{port}{endpoint}"

    try:
        start_time = time.time()
        response = requests.post(url, json=scenario, timeout=30)
        end_time = time.time()
        response_time = (end_time - start_time) * 1000  # ms

        if response.status_code == 200:
            data = response.json()
            return {
                'success': True,
                'response_time_ms': response_time,
                'data': data
            }
        else:
            return {
                'success': False,
                'error': f"HTTP {response.status_code}",
                'response_time_ms': response_time
            }
    except requests.exceptions.Timeout:
        return {'success': False, 'error': 'Timeout (30s)', 'response_time_ms': 30000}
    except Exception as e:
        return {'success': False, 'error': str(e)[:100], 'response_time_ms': 0}

def main():
    start_time = datetime.now()
    print("🚀 SIMPLE BENCHMARK - V1 POST Endpoints")
    print("=" * 60)
    print(f"⏰ Start time: {start_time.strftime('%Y-%m-%d %H:%M:%S')}\n")

    results = {}

    for framework_key, framework_info in FRAMEWORKS.items():
        print(f"🧪 Testing {framework_info['name']}...")
        framework_results = {
            'name': framework_info['name'],
            'port': framework_info['port'],
            'scenarios': {}
        }

        total_response_time = 0
        successful_tests = 0
        total_tests = 0

        for scenario in SCENARIOS:
            print(f"   {scenario['complexity']} payload... ", end="", flush=True)
            result = test_framework(framework_key, framework_info, scenario)

            framework_results['scenarios'][scenario['complexity']] = result
            total_tests += 1

            if result['success']:
                successful_tests += 1
                total_response_time += result['response_time_ms']
                print(f"✅ {result['response_time_ms']:.2f}ms")
            else:
                print(f"❌ {result.get('error', 'Failed')}")

        framework_results['summary'] = {
            'total_tests': total_tests,
            'successful_tests': successful_tests,
            'overall_success_rate': (successful_tests / total_tests) * 100 if total_tests > 0 else 0,
            'avg_response_time_ms': total_response_time / successful_tests if successful_tests > 0 else 0,
            'v2_available': False
        }

        results[framework_key] = framework_results
        print()

    # Generate summary
    print("\n" + "=" * 60)
    print("📊 BENCHMARK RESULTS SUMMARY")
    print("=" * 60)

    # Rankings
    sorted_frameworks = sorted(
        results.items(),
        key=lambda x: x[1]['summary']['overall_success_rate'],
        reverse=True
    )

    print(f"\n🏆 FRAMEWORK RANKING:")
    print("-" * 60)
    print(f"{'Rank':<6} {'Framework':<20} {'Success':<10} {'Avg Time':<12} {'Status'}")
    print("-" * 60)

    for i, (framework_key, result) in enumerate(sorted_frameworks, 1):
        success_rate = result['summary']['overall_success_rate']
        avg_time = result['summary']['avg_response_time_ms']
        status = "🟢" if success_rate >= 80 else "🟡" if success_rate >= 50 else "🔴"
        print(f"{i:4}. {result['name']:20} {success_rate:6.1f}%    {avg_time:8.2f}ms    {status}")

    # Save results
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"final_comprehensive_benchmark_{timestamp}.json"

    output = {
        'metadata': {
            'timestamp': start_time.isoformat(),
            'total_frameworks': len(results),
            'duration_seconds': (datetime.now() - start_time).total_seconds(),
            'test_scenarios': SCENARIOS
        },
        'results': results,
        'summary': {
            'total_frameworks': len(results),
            'successful_frameworks': len([r for r in results.values() if r['summary']['overall_success_rate'] > 0])
        }
    }

    with open(filename, 'w') as f:
        json.dump(output, f, indent=2)

    print(f"\n💾 Results saved to: {filename}")
    print(f"⏱️  Total duration: {output['metadata']['duration_seconds']:.1f} seconds")
    print(f"\n🎉 BENCHMARK COMPLETE!")

if __name__ == "__main__":
    main()
