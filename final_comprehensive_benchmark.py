#!/usr/bin/env python3
"""
Final Comprehensive Serialization Framework Benchmark (2025)
Tests all 12 frameworks across multiple scenarios with detailed reporting
"""

import json
import statistics
import time
from datetime import datetime

import requests

# All 12 frameworks with their complete endpoint mapping
FRAMEWORKS = {
    'jackson': {
        'port': 8081,
        'name': 'Jackson JSON',
        'endpoints': {
            'serialization': '/api/jackson/benchmark/serialization',
            'compression': '/api/jackson/benchmark/compression',
            'performance': '/api/jackson/benchmark/performance'
        }
    },
    'protobuf': {
        'port': 8082,
        'name': 'Protocol Buffers',
        'endpoints': {
            'serialization': '/api/protobuf/benchmark/serialization',
            'compression': '/api/protobuf/benchmark/compression',
            'performance': '/api/protobuf/benchmark/performance'
        }
    },
    'avro': {
        'port': 8083,
        'name': 'Apache Avro',
        'endpoints': {
            'serialization': '/api/avro/benchmark/serialization',
            'compression': '/api/avro/benchmark/compression',
            'performance': '/api/avro/benchmark/performance'
        }
    },
    'kryo': {
        'port': 8084,
        'name': 'Kryo',
        'endpoints': {
            'test': '/api/kryo/test-kryo-debug',
            'benchmark': '/api/kryo/benchmark'
        }
    },

    'msgpack': {
        'port': 8086,
        'name': 'MessagePack',
        'endpoints': {
            'serialization': '/api/msgpack/benchmark/serialization',
            'compression': '/api/msgpack/benchmark/compression',
            'performance': '/api/msgpack/benchmark/performance'
        }
    },
    'thrift': {
        'port': 8087,
        'name': 'Apache Thrift',
        'endpoints': {
            'serialization': '/api/thrift/benchmark/serialization',
            'compression': '/api/thrift/benchmark/compression',
            'performance': '/api/thrift/benchmark/performance'
        }
    },
    'capnproto': {
        'port': 8088,
        'name': 'Cap\'n Proto',
        'endpoints': {
            'serialization': '/api/capnproto/benchmark/serialization',
            'compression': '/api/capnproto/benchmark/compression',
            'performance': '/api/capnproto/benchmark/performance'
        }
    },

    'fst': {
        'port': 8090,
        'name': 'FST (Fast Serialization)',
        'endpoints': {
            'serialization': '/api/fst/benchmark/serialization',
            'compression': '/api/fst/benchmark/compression',
            'performance': '/api/fst/benchmark/performance'
        }
    },
    'flatbuffers': {
        'port': 8091,
        'name': 'FlatBuffers',
        'endpoints': {
            'serialization': '/api/flatbuffers/benchmark/serialization',
            'compression': '/api/flatbuffers/benchmark/compression',
            'performance': '/api/flatbuffers/benchmark/performance'
        }
    },
    'grpc': {
        'port': 8092,
        'name': 'gRPC',
        'endpoints': {
            'serialization': '/api/grpc/benchmark/serialization',
            'compression': '/api/grpc/benchmark/compression',
            'performance': '/api/grpc/benchmark/performance'
        }
    }
}

# Test scenarios
TEST_SCENARIOS = [
    {'complexity': 'SMALL', 'iterations': 100,
     'description': 'Small payload'},
    {'complexity': 'MEDIUM', 'iterations': 50,
     'description': 'Medium payload'},
    {'complexity': 'LARGE', 'iterations': 10,
     'description': 'Large payload'},
    {'complexity': 'HUGE', 'iterations': 5,
     'description': 'Huge payload'}
]


class ComprehensiveBenchmark:
    def __init__(self):
        self.results = {}
        self.start_time = datetime.now()

    def check_framework_availability(self):
        """Check which frameworks are available"""
        available = {}

        print("🔍 Checking framework availability...")
        for framework_key, framework_info in FRAMEWORKS.items():
            port = framework_info['port']
            name = framework_info['name']

            try:
                response = requests.get(
                    f"http://localhost:{port}/actuator/health", timeout=3)
                if response.status_code == 200:
                    available[framework_key] = framework_info
                    print(f"✅ {name} (Port {port}): Available")
                else:
                    print(f"❌ {name} (Port {port}): "
                          f"HTTP {response.status_code}")
            except requests.exceptions.ConnectionError:
                print(f"❌ {name} (Port {port}): Not running")
            except Exception as e:
                print(f"❌ {name} (Port {port}): Error - {str(e)[:30]}")

        print(f"\n📊 Available: {len(available)}/{len(FRAMEWORKS)} "
              f"frameworks")
        return available

    def test_endpoint(self, framework_key, framework_info, endpoint_name,
                      endpoint_path, scenario):
        """Test a specific endpoint with a scenario"""
        port = framework_info['port']

        url = f"http://localhost:{port}{endpoint_path}"

        # Prepare payload based on endpoint type
        if endpoint_name in ['serialization', 'compression', 'performance']:
            payload = {
                'complexity': scenario['complexity'],
                'iterations': scenario['iterations']
            }
            method = 'POST'
        elif endpoint_name == 'benchmark':
            payload = {
                'iterations': scenario['iterations'],
                'userCount': scenario.get('userCount', 100)
            }
            method = 'POST'
        else:  # test endpoints
            payload = None
            method = 'GET'

        try:
            start_time = time.time()

            if method == 'POST':
                response = requests.post(url, json=payload, timeout=30)
            else:
                response = requests.get(url, timeout=30)

            end_time = time.time()
            response_time = (end_time - start_time) * 1000  # ms

            if response.status_code == 200:
                try:
                    data = response.json()
                    return {
                        'success': True,
                        'response_time_ms': response_time,
                        'data': data,
                        'status_code': response.status_code
                    }
                except json.JSONDecodeError:
                    return {
                        'success': True,
                        'response_time_ms': response_time,
                        'data': {'raw_response': response.text[:200]},
                        'status_code': response.status_code
                    }
            else:
                return {
                    'success': False,
                    'response_time_ms': response_time,
                    'error': f"HTTP {response.status_code}",
                    'status_code': response.status_code
                }

        except requests.exceptions.Timeout:
            return {
                'success': False,
                'error': 'Timeout (30s)',
                'response_time_ms': 30000
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e)[:100],
                'response_time_ms': 0
            }

    def run_framework_benchmark(self, framework_key, framework_info):
        """Run comprehensive benchmark for a single framework"""
        name = framework_info['name']
        print(f"\n🧪 Testing {name}...")

        framework_results = {
            'name': name,
            'port': framework_info['port'],
            'scenarios': {},
            'summary': {}
        }

        total_tests = 0
        successful_tests = 0
        total_response_time = 0

        # Test each scenario
        for scenario in TEST_SCENARIOS:
            scenario_key = scenario['complexity']
            scenario_results = {
                'description': scenario['description'],
                'endpoints': {},
                'summary': {}
            }

            scenario_success_count = 0
            scenario_response_times = []

            # Test each endpoint for this scenario
            endpoints = framework_info['endpoints'].items()
            for endpoint_name, endpoint_path in endpoints:
                print(f"   Testing {endpoint_name} with "
                      f"{scenario_key} payload...")

                result = self.test_endpoint(
                    framework_key, framework_info, endpoint_name,
                    endpoint_path, scenario)

                scenario_results['endpoints'][endpoint_name] = result
                total_tests += 1

                if result['success']:
                    successful_tests += 1
                    scenario_success_count += 1
                    scenario_response_times.append(result['response_time_ms'])
                    total_response_time += result['response_time_ms']

            # Calculate scenario summary
            endpoint_count = len(framework_info['endpoints'])
            scenario_results['summary'] = {
                'success_rate': (scenario_success_count /
                                endpoint_count) * 100,
                'avg_response_time_ms': (
                    statistics.mean(scenario_response_times)
                    if scenario_response_times else 0),
                'total_endpoints': endpoint_count,
                'successful_endpoints': scenario_success_count
            }

            framework_results['scenarios'][scenario_key] = scenario_results

        # Calculate framework summary
        framework_results['summary'] = {
            'overall_success_rate': ((successful_tests / total_tests) * 100
                                   if total_tests > 0 else 0),
            'avg_response_time_ms': (total_response_time / successful_tests
                                   if successful_tests > 0 else 0),
            'total_tests': total_tests,
            'successful_tests': successful_tests,
            'total_endpoints': len(framework_info['endpoints']),
            'total_scenarios': len(TEST_SCENARIOS)
        }

        return framework_results

    def run_comprehensive_benchmark(self):
        """Run the complete benchmark suite"""
        print("🚀 FINAL COMPREHENSIVE BENCHMARK")
        print("=" * 60)
        print(f"⏰ Start time: "
              f"{self.start_time.strftime('%Y-%m-%d %H:%M:%S')}")

        # Check availability
        available_frameworks = self.check_framework_availability()

        if not available_frameworks:
            print("\n❌ No frameworks are available. Please start them first!")
            print("💡 Run: python start_all_frameworks_comprehensive.py")
            return None

        print(f"\n🔥 Running comprehensive tests on "
              f"{len(available_frameworks)} frameworks...")

        # Run benchmarks
        for framework_key, framework_info in available_frameworks.items():
            self.results[framework_key] = self.run_framework_benchmark(
                framework_key, framework_info)

        return self.results

    def generate_summary_report(self):
        """Generate a comprehensive summary report"""
        if not self.results:
            return

        print("\n" + "=" * 80)
        print("📊 COMPREHENSIVE BENCHMARK RESULTS SUMMARY")
        print("=" * 80)

        # Overall statistics
        total_frameworks = len(self.results)
        total_tests = sum(r['summary']['total_tests']
                         for r in self.results.values())
        total_successful = sum(r['summary']['successful_tests']
                             for r in self.results.values())

        print(f"🏆 Total Frameworks Tested: {total_frameworks}")
        print(f"🧪 Total Tests Executed: {total_tests}")
        print(f"✅ Total Successful Tests: {total_successful}")
        success_rate = (total_successful/total_tests)*100 if total_tests > 0 else 0
        print(f"📈 Overall Success Rate: {success_rate:.1f}%")

        # Framework ranking by success rate
        print(f"\n🥇 FRAMEWORK RANKING BY SUCCESS RATE:")
        print("-" * 60)

        sorted_frameworks = sorted(
            self.results.items(),
            key=lambda x: x[1]['summary']['overall_success_rate'],
            reverse=True
        )

        for i, (framework_key, result) in enumerate(sorted_frameworks, 1):
            success_rate = result['summary']['overall_success_rate']
            avg_response = result['summary']['avg_response_time_ms']
            status = ("🟢" if success_rate >= 80 else
                     "🟡" if success_rate >= 50 else "🔴")

            print(f"{i:2}. {status} {result['name']:25} | "
                  f"Success: {success_rate:5.1f}% | "
                  f"Avg Response: {avg_response:6.1f}ms")

        # Performance ranking
        print(f"\n⚡ FRAMEWORK RANKING BY PERFORMANCE (Response Time):")
        print("-" * 60)

        # Filter only successful frameworks for performance ranking
        successful_frameworks = [
            (k, v) for k, v in self.results.items()
            if v['summary']['successful_tests'] > 0
        ]

        sorted_by_performance = sorted(
            successful_frameworks,
            key=lambda x: x[1]['summary']['avg_response_time_ms']
        )

        for i, (framework_key, result) in enumerate(sorted_by_performance, 1):
            avg_response = result['summary']['avg_response_time_ms']
            success_rate = result['summary']['overall_success_rate']

            print(f"{i:2}. ⚡ {result['name']:25} | "
                  f"Avg Response: {avg_response:6.1f}ms | "
                  f"Success: {success_rate:5.1f}%")

        # Scenario analysis
        print(f"\n📋 SCENARIO ANALYSIS:")
        print("-" * 60)

        for scenario in TEST_SCENARIOS:
            scenario_key = scenario['complexity']
            scenario_name = scenario['description']

            scenario_success_rates = []
            scenario_response_times = []

            for result in self.results.values():
                if scenario_key in result['scenarios']:
                    scenario_data = result['scenarios'][scenario_key]['summary']
                    scenario_success_rates.append(scenario_data['success_rate'])
                    if scenario_data['avg_response_time_ms'] > 0:
                        scenario_response_times.append(
                            scenario_data['avg_response_time_ms'])

            avg_success = (statistics.mean(scenario_success_rates)
                          if scenario_success_rates else 0)
            avg_response = (statistics.mean(scenario_response_times)
                           if scenario_response_times else 0)

            print(f"{scenario_name:15} | Success: {avg_success:5.1f}% | "
                  f"Avg Response: {avg_response:6.1f}ms")

    def save_detailed_results(self):
        """Save detailed results to JSON file"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"final_comprehensive_benchmark_{timestamp}.json"

        detailed_results = {
            'metadata': {
                'timestamp': self.start_time.isoformat(),
                'total_frameworks': len(self.results),
                'test_scenarios': TEST_SCENARIOS,
                'duration_seconds': ((datetime.now() - self.start_time)
                                   .total_seconds())
            },
            'results': self.results
        }

        with open(filename, 'w') as f:
            json.dump(detailed_results, f, indent=2, default=str)

        print(f"\n💾 Detailed results saved to: {filename}")
        return filename


def main():
    benchmark = ComprehensiveBenchmark()

    try:
        # Run comprehensive benchmark
        results = benchmark.run_comprehensive_benchmark()

        if results:
            # Generate and display summary
            benchmark.generate_summary_report()

            # Save detailed results
            filename = benchmark.save_detailed_results()

            print(f"\n🎉 BENCHMARK COMPLETE!")
            print(f"📄 Results saved to: {filename}")
            duration = (datetime.now() - benchmark.start_time).total_seconds()
            print(f"⏱️  Total duration: {duration:.1f} seconds")

    except KeyboardInterrupt:
        print("\n🛑 Benchmark interrupted by user")
    except Exception as e:
        print(f"\n❌ Benchmark error: {e}")


if __name__ == "__main__":
    main()
