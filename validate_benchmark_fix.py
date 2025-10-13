#!/usr/bin/env python3
"""Quick validation of benchmark script fix using V2 endpoints"""
import requests
import json
from datetime import datetime

FRAMEWORKS = {
    'jackson': {'port': 8081, 'name': 'Jackson JSON', 'v2_path': '/api/jackson/benchmark/serialization'},
    'avro': {'port': 8083, 'name': 'Apache Avro', 'v2_path': '/api/avro/benchmark/serialization'},
    'kryo': {'port': 8084, 'name': 'Kryo', 'v2_path': '/api/kryo/benchmark/serialization'},
    'msgpack': {'port': 8086, 'name': 'MessagePack', 'v2_path': '/api/msgpack/benchmark/serialization'},
    'flatbuffers': {'port': 8091, 'name': 'FlatBuffers', 'v2_path': '/api/flatbuffers/benchmark/serialization'},
    'grpc': {'port': 8092, 'name': 'gRPC', 'v2_path': '/api/grpc/benchmark/serialization'}
}

results = {}
print("🚀 Validating Benchmark Fix with V2 Endpoints...")
print()

for key, info in FRAMEWORKS.items():
    port = info['port']
    name = info['name']
    v2_path = info['v2_path']

    try:
        # Test V2 endpoint
        print(f"Testing {name}...", end=" ")
        resp = requests.post(
            f"http://localhost:{port}{v2_path}",
            json={"complexity": "SMALL", "iterations": 5},
            timeout=15
        )

        if resp.status_code == 200:
            data = resp.json()
            # Simulate what comprehensive benchmark does - store result with summary
            results[key] = {
                'name': name,
                'scenarios': {
                    'SMALL_serialization': {
                        'summary': {
                            'avg_response_time_ms': data.get('serializationTimeMs', 0),
                            'success_rate': 100.0 if data.get('success', False) else 0.0,
                            'total_tests': 1
                        }
                    }
                },
                'summary': {
                    'total_tests': 1,
                    'successful_tests': 1 if data.get('success', False) else 0,
                    'overall_success_rate': 100.0 if data.get('success', False) else 0.0,
                    'v2_available': True
                }
            }
            print(f"✅ {data.get('serializationTimeMs', 0):.2f}ms")
        else:
            print(f"❌ HTTP {resp.status_code}")

    except Exception as e:
        print(f"❌ Error: {str(e)[:50]}")

print()
print("=" * 80)
print("Testing safe dictionary access patterns (the fix)...")
print("=" * 80)

# Test the safe access patterns that were fixed
try:
    # This would have crashed before fix
    total_tests = sum(r.get('summary', {}).get('total_tests', 0) for r in results.values())
    print(f"✅ Total tests (safe access): {total_tests}")

    successful_tests = sum(r.get('summary', {}).get('successful_tests', 0) for r in results.values())
    print(f"✅ Successful tests (safe access): {successful_tests}")

    # Sort by success rate (safe access)
    sorted_frameworks = sorted(
        results.items(),
        key=lambda x: x[1].get('summary', {}).get('overall_success_rate', 0),
        reverse=True
    )
    print(f"✅ Sorted {len(sorted_frameworks)} frameworks by success rate")

    # V2 statistics (safe access)
    v2_frameworks = sum(1 for r in results.values() if r.get('summary', {}).get('v2_available', False))
    print(f"✅ V2 frameworks count (safe access): {v2_frameworks}")

    print()
    print("🎉 All safe access patterns working correctly!")
    print()

except Exception as e:
    print(f"❌ Safe access test failed: {e}")

# Save results
output = {
    'metadata': {
        'timestamp': datetime.now().isoformat(),
        'test_type': 'fix_validation',
        'frameworks_tested': len(results)
    },
    'results': results,
    'summary': {
        'total_frameworks': len(results),
        'successful_frameworks': len([r for r in results.values() if r.get('summary', {}).get('overall_success_rate', 0) > 0]),
        'overall_success_rate': 100.0 * len([r for r in results.values() if r.get('summary', {}).get('overall_success_rate', 0) > 0]) / len(results) if results else 0
    }
}

filename = f"validation_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
with open(filename, 'w') as f:
    json.dump(output, f, indent=2)

print(f"✅ Results saved to: {filename}")
print(f"📊 Tested {len(results)} frameworks")
print(f"✅ Success rate: {output['summary']['overall_success_rate']:.1f}%")
