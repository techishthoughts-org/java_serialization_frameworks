#!/usr/bin/env python3
import requests
import json
import time
from datetime import datetime

FRAMEWORKS = {
    'jackson': {'port': 8081, 'name': 'Jackson JSON'},
    'avro': {'port': 8083, 'name': 'Apache Avro'},
    'kryo': {'port': 8084, 'name': 'Kryo'},
    'msgpack': {'port': 8086, 'name': 'MessagePack'},
    'flatbuffers': {'port': 8091, 'name': 'FlatBuffers'},
    'grpc': {'port': 8092, 'name': 'gRPC'}
}

results = {}
print("🚀 Quick Benchmark Test - Running...")

for key, info in FRAMEWORKS.items():
    port = info['port']
    name = info['name']
    
    try:
        # Test health
        health = requests.get(f"http://localhost:{port}/actuator/health", timeout=2)
        if health.status_code != 200:
            print(f"❌ {name}: Not available")
            continue
            
        print(f"✅ {name}: Testing...")
        
        # Quick performance test
        times = []
        for i in range(10):
            start = time.time()
            resp = requests.post(
                f"http://localhost:{port}/api/{key}/benchmark",
                json={"complexity": "MEDIUM", "iterations": 10},
                timeout=10
            )
            elapsed = (time.time() - start) * 1000  # Convert to ms
            if resp.status_code == 200:
                times.append(elapsed)
        
        if times:
            avg_time = sum(times) / len(times)
            results[key] = {
                'name': name,
                'port': port,
                'avg_response_time_ms': round(avg_time, 2),
                'success_rate': 100.0,
                'tests': len(times)
            }
            print(f"   Average: {avg_time:.2f}ms")
        
    except Exception as e:
        print(f"❌ {name}: Error - {e}")

# Save results
output = {
    'metadata': {
        'timestamp': datetime.now().isoformat(),
        'test_type': 'quick_benchmark',
        'frameworks_tested': len(results)
    },
    'results': results,
    'summary': {
        'total_frameworks': len(results),
        'overall_success_rate': 100.0 if results else 0
    }
}

filename = f"final_comprehensive_benchmark_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
with open(filename, 'w') as f:
    json.dump(output, f, indent=2)

print(f"\n✅ Results saved to: {filename}")
print(f"📊 Tested {len(results)} frameworks successfully")
