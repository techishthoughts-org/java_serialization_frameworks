#!/usr/bin/env python3
"""
Final Comprehensive Serialization Framework Benchmark (2025) - V2 Enhanced

Tests all frameworks across multiple scenarios with detailed reporting.
Supports both V1 (legacy) and V2 (unified) API endpoints.

V2 API Features:
- Unified benchmark endpoint with comprehensive BenchmarkResult response
- Memory usage monitoring and metrics
- Roundtrip serialization/deserialization testing
- Enhanced compression analysis
- Standardized error handling and response format

Compatibility:
- Maintains full backward compatibility with V1 endpoints
- Automatically detects and prefers V2 endpoints when available
- Falls back gracefully to V1 endpoints for frameworks without V2 support
- Enhanced reporting shows V2 vs V1 performance and feature comparison
"""

import json
import statistics
import time
from datetime import datetime

import requests

try:
    import numpy as np
    HAS_NUMPY = True
except ImportError:
    HAS_NUMPY = False
    print("⚠️  NumPy not available - percentile analysis will use basic statistics")

# All 15 frameworks with their complete endpoint mapping (V2 + V1 support)
FRAMEWORKS = {
    'jackson': {
        'port': 8081,
        'name': 'Jackson JSON',
        'endpoints': {
            'serialization': '/api/jackson/benchmark/serialization',
            'compression': '/api/jackson/benchmark/compression',
            'performance': '/api/jackson/benchmark/performance'
        },
        'endpoints_v2': {
            'unified': '/api/jackson/v2/benchmark',
            'info': '/api/jackson/v2/info',
            'health': '/api/jackson/v2/health'
        },
        'supports_v2': True
    },
    'protobuf': {
        'port': 8082,
        'name': 'Protocol Buffers',
        'endpoints': {
            'serialization': '/api/protobuf/benchmark/serialization',
            'compression': '/api/protobuf/benchmark/compression',
            'performance': '/api/protobuf/benchmark/performance'
        },
        'endpoints_v2': {
            'unified': '/api/protobuf/v2/benchmark',
            'info': '/api/protobuf/v2/info',
            'health': '/api/protobuf/v2/health'
        },
        'supports_v2': False  # V2 not implemented yet
    },
    'avro': {
        'port': 8083,
        'name': 'Apache Avro',
        'endpoints': {
            'serialization': '/api/avro/benchmark/serialization',
            'compression': '/api/avro/benchmark/compression',
            'performance': '/api/avro/benchmark/performance'
        },
        'endpoints_v2': {
            'unified': '/api/avro/v2/benchmark',
            'info': '/api/avro/v2/info',
            'health': '/api/avro/v2/health'
        },
        'supports_v2': False  # V2 not implemented yet
    },
    'kryo': {
        'port': 8084,
        'name': 'Kryo',
        'endpoints': {
            'test': '/api/kryo/test-kryo-debug',
            'benchmark': '/api/kryo/benchmark'
        },
        'endpoints_v2': {
            'unified': '/api/kryo/v2/benchmark',
            'info': '/api/kryo/v2/info',
            'health': '/api/kryo/v2/health'
        },
        'supports_v2': False  # V2 not implemented yet
    },
    'msgpack': {
        'port': 8086,
        'name': 'MessagePack',
        'endpoints': {
            'serialization': '/api/msgpack/benchmark/serialization',
            'compression': '/api/msgpack/benchmark/compression',
            'performance': '/api/msgpack/benchmark/performance'
        },
        'endpoints_v2': {
            'unified': '/api/msgpack/v2/benchmark',
            'info': '/api/msgpack/v2/info',
            'health': '/api/msgpack/v2/health'
        },
        'supports_v2': False  # V2 not implemented yet
    },
    'thrift': {
        'port': 8087,
        'name': 'Apache Thrift',
        'endpoints': {
            'serialization': '/api/thrift/benchmark/serialization',
            'compression': '/api/thrift/benchmark/compression',
            'performance': '/api/thrift/benchmark/performance'
        },
        'endpoints_v2': {
            'unified': '/api/thrift/v2/benchmark',
            'info': '/api/thrift/v2/info',
            'health': '/api/thrift/v2/health'
        },
        'supports_v2': False  # V2 not implemented yet
    },
    'capnproto': {
        'port': 8088,
        'name': 'Cap\'n Proto',
        'endpoints': {
            'serialization': '/api/capnproto/benchmark/serialization',
            'compression': '/api/capnproto/benchmark/compression',
            'performance': '/api/capnproto/benchmark/performance'
        },
        'endpoints_v2': {
            'unified': '/api/capnproto/v2/benchmark',
            'info': '/api/capnproto/v2/info',
            'health': '/api/capnproto/v2/health'
        },
        'supports_v2': False  # V2 not implemented yet
    },
    'fst': {
        'port': 8090,
        'name': 'FST (Fast Serialization)',
        'endpoints': {
            'serialization': '/api/fst/benchmark/serialization',
            'compression': '/api/fst/benchmark/compression',
            'performance': '/api/fst/benchmark/performance'
        },
        'endpoints_v2': {
            'unified': '/api/fst/v2/benchmark',
            'info': '/api/fst/v2/info',
            'health': '/api/fst/v2/health'
        },
        'supports_v2': False  # V2 not implemented yet
    },
    'flatbuffers': {
        'port': 8091,
        'name': 'FlatBuffers',
        'endpoints': {
            'serialization': '/api/flatbuffers/benchmark/serialization',
            'compression': '/api/flatbuffers/benchmark/compression',
            'performance': '/api/flatbuffers/benchmark/performance'
        },
        'endpoints_v2': {
            'unified': '/api/flatbuffers/v2/benchmark',
            'info': '/api/flatbuffers/v2/info',
            'health': '/api/flatbuffers/v2/health'
        },
        'supports_v2': False  # V2 not implemented yet
    },
    'grpc': {
        'port': 8092,
        'name': 'gRPC',
        'endpoints': {
            'serialization': '/api/grpc/benchmark/serialization',
            'compression': '/api/grpc/benchmark/compression',
            'performance': '/api/grpc/benchmark/performance'
        },
        'endpoints_v2': {
            'unified': '/api/grpc/v2/benchmark',
            'info': '/api/grpc/v2/info',
            'health': '/api/grpc/v2/health'
        },
        'supports_v2': False  # V2 not implemented yet
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
        self.v2_endpoints_available = {}  # Track which frameworks support V2

    def check_framework_availability(self):
        """Check which frameworks are available and detect V2 endpoint support"""
        available = {}

        print("🔍 Checking framework availability...")
        for framework_key, framework_info in FRAMEWORKS.items():
            port = framework_info['port']
            name = framework_info['name']

            try:
                # Check basic health endpoint first
                response = requests.get(
                    f"http://localhost:{port}/actuator/health", timeout=3)
                if response.status_code == 200:
                    available[framework_key] = framework_info

                    # Check if V2 endpoints are available
                    v2_available = self.check_v2_endpoint_availability(
                        framework_key, framework_info)
                    self.v2_endpoints_available[framework_key] = v2_available

                    v2_status = "V2 Available" if v2_available else "V1 Only"
                    print(f"✅ {name} (Port {port}): Available ({v2_status})")
                else:
                    print(f"❌ {name} (Port {port}): "
                          f"HTTP {response.status_code}")
            except requests.exceptions.ConnectionError:
                print(f"❌ {name} (Port {port}): Not running")
            except Exception as e:
                print(f"❌ {name} (Port {port}): Error - {str(e)[:30]}")

        print(f"\n📊 Available: {len(available)}/{len(FRAMEWORKS)} "
              f"frameworks")
        v2_count = sum(1 for v in self.v2_endpoints_available.values() if v)
        print(f"🚀 V2 Endpoints: {v2_count}/{len(available)} frameworks")
        return available

    def check_v2_endpoint_availability(self, framework_key, framework_info):
        """Check if V2 endpoints are available for a framework"""
        if not framework_info.get('supports_v2', False):
            return False

        port = framework_info['port']
        v2_endpoints = framework_info.get('endpoints_v2', {})

        if not v2_endpoints:
            return False

        # Try to access the V2 info endpoint
        info_endpoint = v2_endpoints.get('info')
        if info_endpoint:
            try:
                response = requests.get(
                    f"http://localhost:{port}{info_endpoint}", timeout=3)
                return response.status_code == 200
            except:
                return False

        return False

    def test_v2_unified_endpoint(self, framework_key, framework_info, scenario):
        """Test the new V2 unified benchmark endpoint"""
        port = framework_info['port']
        v2_endpoints = framework_info.get('endpoints_v2', {})
        unified_endpoint = v2_endpoints.get('unified')

        if not unified_endpoint:
            return {
                'success': False,
                'error': 'V2 unified endpoint not configured'
            }

        url = f"http://localhost:{port}{unified_endpoint}"

        # Prepare V2 payload with enhanced options
        payload = {
            'complexity': scenario['complexity'],
            'iterations': scenario['iterations'],
            'enableWarmup': True,
            'enableCompression': True,
            'enableRoundtrip': True,
            'enableMemoryMonitoring': True
        }

        try:
            start_time = time.time()
            response = requests.post(url, json=payload, timeout=60)  # Longer timeout for comprehensive tests
            end_time = time.time()
            response_time = (end_time - start_time) * 1000  # ms

            if response.status_code == 200:
                try:
                    data = response.json()
                    return {
                        'success': True,
                        'response_time_ms': response_time,
                        'data': data,
                        'status_code': response.status_code,
                        'endpoint_type': 'v2_unified'
                    }
                except json.JSONDecodeError:
                    return {
                        'success': True,
                        'response_time_ms': response_time,
                        'data': {'raw_response': response.text[:200]},
                        'status_code': response.status_code,
                        'endpoint_type': 'v2_unified'
                    }
            else:
                return {
                    'success': False,
                    'response_time_ms': response_time,
                    'error': f"HTTP {response.status_code}",
                    'status_code': response.status_code,
                    'endpoint_type': 'v2_unified'
                }

        except requests.exceptions.Timeout:
            return {
                'success': False,
                'error': 'Timeout (60s)',
                'response_time_ms': 60000,
                'endpoint_type': 'v2_unified'
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e)[:100],
                'response_time_ms': 0,
                'endpoint_type': 'v2_unified'
            }

    def test_endpoint(self, framework_key, framework_info, endpoint_name,
                      endpoint_path, scenario):
        """Test a specific endpoint with a scenario (V1 legacy endpoints)"""
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
                        'status_code': response.status_code,
                        'endpoint_type': 'v1_legacy'
                    }
                except json.JSONDecodeError:
                    return {
                        'success': True,
                        'response_time_ms': response_time,
                        'data': {'raw_response': response.text[:200]},
                        'status_code': response.status_code,
                        'endpoint_type': 'v1_legacy'
                    }
            else:
                return {
                    'success': False,
                    'response_time_ms': response_time,
                    'error': f"HTTP {response.status_code}: {response.text[:100] if response.text else 'No response body'}",
                    'status_code': response.status_code,
                    'endpoint_type': 'v1_legacy'
                }

        except requests.exceptions.Timeout:
            return {
                'success': False,
                'error': 'Timeout (30s)',
                'response_time_ms': 30000,
                'endpoint_type': 'v1_legacy'
            }
        except requests.exceptions.ConnectionError:
            return {
                'success': False,
                'error': 'Connection error - framework may have stopped',
                'response_time_ms': 0,
                'endpoint_type': 'v1_legacy'
            }
        except Exception as e:
            return {
                'success': False,
                'error': f"Unexpected error: {str(e)[:100]}",
                'response_time_ms': 0,
                'endpoint_type': 'v1_legacy'
            }

    def parse_v2_result(self, result_data):
        """Parse V2 unified benchmark result into standardized format"""
        if not result_data.get('success', False):
            return {
                'parsed_success': False,
                'error': result_data.get('error', 'V2 benchmark failed'),
                'framework': result_data.get('framework', 'Unknown')
            }

        # Extract key metrics from V2 BenchmarkResult format
        parsed = {
            'parsed_success': True,
            'framework': result_data.get('framework', 'Unknown'),
            'complexity': result_data.get('complexity', 'Unknown'),
            'iterations': result_data.get('iterations', 0),
            'total_duration_ms': result_data.get('totalDurationMs', 0),
            'success_rate_percent': result_data.get('successRate', 0),
            'successful_serializations': result_data.get('successfulSerializations', 0),
            'successful_compressions': result_data.get('successfulCompressions', 0),
            'roundtrip_success': result_data.get('roundtripSuccess', False),

            # Performance metrics
            'avg_serialization_time_ms': result_data.get('averageSerializationTimeMs', 0.0),
            'avg_compression_ratio': result_data.get('averageCompressionRatio', 0.0),
            'avg_serialized_size_bytes': result_data.get('averageSerializedSizeBytes', 0.0),

            # Memory metrics (if available)
            'memory_metrics': result_data.get('memoryMetrics'),

            # Legacy compatibility fields
            'serialization_time_ms': result_data.get('serializationTimeMs', result_data.get('averageSerializationTimeMs', 0.0)),
            'deserialization_time_ms': result_data.get('deserializationTimeMs', 0.0),
            'total_size_bytes': result_data.get('totalSizeBytes', result_data.get('averageSerializedSizeBytes', 0.0)),
            'user_count': result_data.get('userCount', 0)
        }

        return parsed

    def run_framework_benchmark(self, framework_key, framework_info):
        """Run comprehensive benchmark for a single framework (V2 + V1 support)"""
        name = framework_info['name']
        has_v2 = self.v2_endpoints_available.get(framework_key, False)
        v2_status = "V2" if has_v2 else "V1"
        print(f"\n🧪 Testing {name} ({v2_status})...")

        framework_results = {
            'name': name,
            'port': framework_info['port'],
            'supports_v2': has_v2,
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
                'v2_unified_result': None,
                'summary': {}
            }

            scenario_success_count = 0
            scenario_response_times = []

            # First, try V2 unified endpoint if available
            if has_v2:
                print(f"   Testing V2 unified endpoint with {scenario_key} payload...")
                v2_result = self.test_v2_unified_endpoint(framework_key, framework_info, scenario)
                scenario_results['v2_unified_result'] = v2_result
                total_tests += 1

                if v2_result['success']:
                    successful_tests += 1
                    scenario_success_count += 1
                    scenario_response_times.append(v2_result['response_time_ms'])
                    total_response_time += v2_result['response_time_ms']

                    # Parse V2 result for enhanced metrics
                    parsed_v2 = self.parse_v2_result(v2_result['data'])
                    scenario_results['v2_parsed'] = parsed_v2
                else:
                    print(f"     V2 unified endpoint failed: {v2_result.get('error', 'Unknown error')}")

            # Test V1 legacy endpoints for backward compatibility and comparison
            endpoints = framework_info['endpoints'].items()
            for endpoint_name, endpoint_path in endpoints:
                print(f"   Testing V1 {endpoint_name} with {scenario_key} payload...")

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

            # Calculate scenario summary with percentile analysis
            total_scenario_endpoints = (1 if has_v2 else 0) + len(framework_info['endpoints'])

            # Calculate percentiles if we have response times
            percentile_stats = {}
            if scenario_response_times:
                if HAS_NUMPY:
                    percentile_stats = {
                        'p50_ms': float(np.percentile(scenario_response_times, 50)),  # Median
                        'p95_ms': float(np.percentile(scenario_response_times, 95)),
                        'p99_ms': float(np.percentile(scenario_response_times, 99)),
                        'min_ms': float(np.min(scenario_response_times)),
                        'max_ms': float(np.max(scenario_response_times)),
                        'std_dev_ms': float(np.std(scenario_response_times))
                    }
                else:
                    sorted_times = sorted(scenario_response_times)
                    percentile_stats = {
                        'p50_ms': statistics.median(sorted_times),
                        'p95_ms': sorted_times[int(len(sorted_times) * 0.95)] if len(sorted_times) > 1 else sorted_times[0],
                        'p99_ms': sorted_times[int(len(sorted_times) * 0.99)] if len(sorted_times) > 1 else sorted_times[0],
                        'min_ms': min(sorted_times),
                        'max_ms': max(sorted_times),
                        'std_dev_ms': statistics.stdev(sorted_times) if len(sorted_times) > 1 else 0
                    }

            scenario_results['summary'] = {
                'success_rate': (scenario_success_count / total_scenario_endpoints) * 100 if total_scenario_endpoints > 0 else 0,
                'avg_response_time_ms': (
                    statistics.mean(scenario_response_times)
                    if scenario_response_times else 0),
                'percentiles': percentile_stats,  # Add percentile statistics
                'total_endpoints': total_scenario_endpoints,
                'successful_endpoints': scenario_success_count,
                'v2_available': has_v2,
                'v2_successful': has_v2 and scenario_results.get('v2_unified_result', {}).get('success', False)
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
            'v2_available': has_v2,
            'total_endpoints': len(framework_info['endpoints']) + (1 if has_v2 else 0),
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
            try:
                result = self.run_framework_benchmark(framework_key, framework_info)
                if result and isinstance(result, dict) and 'summary' in result:
                    self.results[framework_key] = result
                else:
                    print(f"⚠️  {framework_info['name']}: Invalid result, skipping")
            except Exception as e:
                print(f"❌ {framework_info['name']}: Error during benchmark - {str(e)[:100]}")

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
        total_tests = sum(r.get('summary', {}).get('total_tests', 0)
                         for r in self.results.values())
        total_successful = sum(r.get('summary', {}).get('successful_tests', 0)
                             for r in self.results.values())

        # Enhanced statistics with V2 information
        v2_frameworks = sum(1 for r in self.results.values() if r.get('summary', {}).get('v2_available', False))
        total_v2_tests = sum(sum(1 for s in r.get('scenarios', {}).values() if s.get('v2_unified_result'))
                           for r in self.results.values())
        successful_v2_tests = sum(sum(1 for s in r.get('scenarios', {}).values()
                                    if s.get('v2_unified_result', {}).get('success', False))
                                for r in self.results.values())

        print(f"🏆 Total Frameworks Tested: {total_frameworks}")
        print(f"🚀 V2 API Support: {v2_frameworks}/{total_frameworks} frameworks ({(v2_frameworks/total_frameworks)*100:.1f}%)")
        print(f"🧪 Total Tests Executed: {total_tests}")
        print(f"✅ Total Successful Tests: {total_successful}")
        success_rate = (total_successful/total_tests)*100 if total_tests > 0 else 0
        print(f"📈 Overall Success Rate: {success_rate:.1f}%")
        if total_v2_tests > 0:
            v2_success_rate = (successful_v2_tests/total_v2_tests)*100
            print(f"🔥 V2 API Success Rate: {v2_success_rate:.1f}% ({successful_v2_tests}/{total_v2_tests} tests)")

        # Framework ranking by success rate
        print(f"\n🥇 FRAMEWORK RANKING BY SUCCESS RATE:")
        print("-" * 80)
        print(f"{'Rank':<4} {'Framework':<25} {'API':<5} {'Success':<9} {'Avg Time':<12} {'Status'}")
        print("-" * 80)

        sorted_frameworks = sorted(
            self.results.items(),
            key=lambda x: x[1].get('summary', {}).get('overall_success_rate', 0),
            reverse=True
        )

        for i, (framework_key, result) in enumerate(sorted_frameworks, 1):
            success_rate = result.get('summary', {}).get('overall_success_rate', 0)
            avg_response = result.get('summary', {}).get('avg_response_time_ms', 0)
            has_v2 = result.get('summary', {}).get('v2_available', False)
            api_version = "V2" if has_v2 else "V1"

            status = ("🟢" if success_rate >= 80 else
                     "🟡" if success_rate >= 50 else "🔴")

            print(f"{i:2}. {result['name']:25} {api_version:<5} {success_rate:5.1f}%    {avg_response:8.1f}ms    {status}")

        # Performance ranking
        print(f"\n⚡ FRAMEWORK RANKING BY PERFORMANCE (Response Time):")
        print("-" * 80)
        print(f"{'Rank':<4} {'Framework':<25} {'API':<5} {'Avg Time':<12} {'Success':<9} {'V2 Features'}")
        print("-" * 80)

        # Filter only successful frameworks for performance ranking
        successful_frameworks = [
            (k, v) for k, v in self.results.items()
            if v.get('summary', {}).get('successful_tests', 0) > 0
        ]

        sorted_by_performance = sorted(
            successful_frameworks,
            key=lambda x: x[1].get('summary', {}).get('avg_response_time_ms', float('inf'))
        )

        for i, (framework_key, result) in enumerate(sorted_by_performance, 1):
            avg_response = result.get('summary', {}).get('avg_response_time_ms', 0)
            success_rate = result.get('summary', {}).get('overall_success_rate', 0)
            has_v2 = result.get('summary', {}).get('v2_available', False)
            api_version = "V2" if has_v2 else "V1"

            # Check for V2-specific features in results
            v2_features = []
            if has_v2:
                for scenario in result['scenarios'].values():
                    v2_result = scenario.get('v2_unified_result')
                    if v2_result and v2_result.get('success') and v2_result.get('data'):
                        data = v2_result['data']
                        if data.get('memoryMetrics'):
                            v2_features.append('Memory')
                        if data.get('roundtripSuccess') is not None:
                            v2_features.append('Roundtrip')
                        if data.get('averageCompressionRatio'):
                            v2_features.append('Compression')
                        break

            v2_features_str = ', '.join(set(v2_features)) if v2_features else 'N/A'

            print(f"{i:2}. {result['name']:25} {api_version:<5} {avg_response:8.1f}ms    {success_rate:5.1f}%    {v2_features_str}")

        # V2 API Analysis
        print(f"\n🚀 V2 API ANALYSIS:")
        print("-" * 80)

        v2_frameworks = [(k, v) for k, v in self.results.items() if v.get('summary', {}).get('v2_available', False)]

        if v2_frameworks:
            print(f"{'Framework':<25} {'V2 Success':<12} {'Memory':<8} {'Roundtrip':<10} {'Compression'}")
            print("-" * 80)

            for framework_key, result in v2_frameworks:
                v2_success_count = 0
                total_v2_tests = 0
                has_memory = False
                has_roundtrip = False
                has_compression = False

                for scenario in result['scenarios'].values():
                    v2_result = scenario.get('v2_unified_result')
                    if v2_result:
                        total_v2_tests += 1
                        if v2_result.get('success'):
                            v2_success_count += 1
                            data = v2_result.get('data', {})
                            if data.get('memoryMetrics'):
                                has_memory = True
                            if data.get('roundtripSuccess') is not None:
                                has_roundtrip = True
                            if data.get('averageCompressionRatio'):
                                has_compression = True

                v2_success_rate = (v2_success_count / total_v2_tests) * 100 if total_v2_tests > 0 else 0
                memory_str = "✓" if has_memory else "-"
                roundtrip_str = "✓" if has_roundtrip else "-"
                compression_str = "✓" if has_compression else "-"

                print(f"{result['name']:25} {v2_success_rate:8.1f}%    {memory_str:^8} {roundtrip_str:^10} {compression_str:^11}")
        else:
            print("No V2 endpoints available yet.")

        # Scenario analysis with percentiles
        print(f"\n📋 SCENARIO ANALYSIS (with Percentile Statistics):")
        print("-" * 80)
        print(f"{'Scenario':<15} {'Overall':<10} {'Avg Time':<12} {'p50':<10} {'p95':<10} {'p99'}")
        print("-" * 80)

        for scenario in TEST_SCENARIOS:
            scenario_key = scenario['complexity']
            scenario_name = scenario['description']

            scenario_success_rates = []
            scenario_response_times = []
            scenario_p50_times = []
            scenario_p95_times = []
            scenario_p99_times = []

            for result in self.results.values():
                scenarios = result.get('scenarios', {})
                if scenario_key in scenarios:
                    scenario_data = scenarios[scenario_key].get('summary', {})
                    scenario_success_rates.append(scenario_data.get('success_rate', 0))
                    if scenario_data.get('avg_response_time_ms', 0) > 0:
                        scenario_response_times.append(scenario_data['avg_response_time_ms'])

                    # Collect percentile data if available
                    percentiles = scenario_data.get('percentiles', {})
                    if percentiles.get('p50_ms', 0) > 0:
                        scenario_p50_times.append(percentiles['p50_ms'])
                    if percentiles.get('p95_ms', 0) > 0:
                        scenario_p95_times.append(percentiles['p95_ms'])
                    if percentiles.get('p99_ms', 0) > 0:
                        scenario_p99_times.append(percentiles['p99_ms'])

            avg_success = (statistics.mean(scenario_success_rates)
                          if scenario_success_rates else 0)
            avg_response = (statistics.mean(scenario_response_times)
                           if scenario_response_times else 0)
            avg_p50 = (statistics.mean(scenario_p50_times)
                      if scenario_p50_times else 0)
            avg_p95 = (statistics.mean(scenario_p95_times)
                      if scenario_p95_times else 0)
            avg_p99 = (statistics.mean(scenario_p99_times)
                      if scenario_p99_times else 0)

            print(f"{scenario_name:15} {avg_success:6.1f}%    {avg_response:8.1f}ms    {avg_p50:6.1f}ms   {avg_p95:6.1f}ms   {avg_p99:6.1f}ms")

    def save_detailed_results(self):
        """Save detailed results to JSON file with V2 metadata"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"final_comprehensive_benchmark_{timestamp}.json"

        # Calculate V2 statistics
        v2_frameworks = sum(1 for r in self.results.values() if r.get('summary', {}).get('v2_available', False))
        total_v2_tests = sum(sum(1 for s in r.get('scenarios', {}).values() if s.get('v2_unified_result'))
                           for r in self.results.values())
        successful_v2_tests = sum(sum(1 for s in r.get('scenarios', {}).values()
                                    if s.get('v2_unified_result', {}).get('success', False))
                                for r in self.results.values())

        detailed_results = {
            'metadata': {
                'timestamp': self.start_time.isoformat(),
                'total_frameworks': len(self.results),
                'v2_frameworks': v2_frameworks,
                'v2_adoption_percent': (v2_frameworks / len(self.results)) * 100 if self.results else 0,
                'total_v2_tests': total_v2_tests,
                'successful_v2_tests': successful_v2_tests,
                'v2_success_rate': (successful_v2_tests / total_v2_tests) * 100 if total_v2_tests > 0 else 0,
                'test_scenarios': TEST_SCENARIOS,
                'duration_seconds': ((datetime.now() - self.start_time)
                                   .total_seconds()),
                'api_versions_tested': ['V1 (Legacy)', 'V2 (Unified)'],
                'v2_features_tested': ['Memory Monitoring', 'Roundtrip Testing', 'Compression Analysis']
            },
            'results': self.results,
            'v2_endpoints_availability': self.v2_endpoints_available
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
