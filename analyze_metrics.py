#!/usr/bin/env python3
"""
Comprehensive Metrics Analysis and Comparison Tool
Analyzes benchmark results and generates deep insights
"""
import json
import sys
from collections import defaultdict
from typing import Dict, List, Any


def load_benchmark_results(filename: str) -> Dict:
    """Load benchmark results from JSON file"""
    try:
        with open(filename, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"❌ Error: File '{filename}' not found")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"❌ Error: Invalid JSON in '{filename}': {e}")
        sys.exit(1)


def calculate_aggregates(results: Dict) -> Dict[str, Any]:
    """Calculate aggregate statistics across all frameworks"""
    aggregates = {
        'by_framework': {},
        'by_complexity': defaultdict(lambda: {'frameworks': [], 'avg_time': 0, 'success_rate': 0}),
        'by_category': defaultdict(lambda: {'frameworks': [], 'avg_time': 0, 'success_rate': 0}),
        'overall_stats': {}
    }

    # Analyze each framework
    for fw_key, fw_data in results['results'].items():
        fw_name = fw_data['name']
        fw_category = fw_data['category']
        tests = fw_data['tests']

        successful_tests = [t for t in tests.values() if t['success']]
        failed_tests = [t for t in tests.values() if not t['success']]

        if successful_tests:
            avg_wall_time = sum(t['result'].get('wall_clock_time_ms', 0) for t in successful_tests) / len(successful_tests)
            avg_ser_time = sum(t['result'].get('serializationTimeMs', 0) for t in successful_tests) / len(successful_tests)
            avg_deser_time = sum(t['result'].get('deserializationTimeMs', 0) for t in successful_tests) / len(successful_tests)
            avg_payload = sum(t['result'].get('serializedSizeBytes', 0) for t in successful_tests) / len(successful_tests)
        else:
            avg_wall_time = avg_ser_time = avg_deser_time = avg_payload = 0

        aggregates['by_framework'][fw_key] = {
            'name': fw_name,
            'category': fw_category,
            'total_tests': len(tests),
            'successful_tests': len(successful_tests),
            'failed_tests': len(failed_tests),
            'success_rate': (len(successful_tests) / len(tests) * 100) if tests else 0,
            'avg_wall_clock_ms': avg_wall_time,
            'avg_serialization_ms': avg_ser_time,
            'avg_deserialization_ms': avg_deser_time,
            'avg_payload_bytes': avg_payload
        }

        # Group by complexity
        for test_id, test_data in tests.items():
            if test_data['success']:
                complexity = test_data['scenario']['complexity']
                aggregates['by_complexity'][complexity]['frameworks'].append(fw_name)
                if 'wall_clock_time_ms' in test_data['result']:
                    aggregates['by_complexity'][complexity]['avg_time'] += test_data['result']['wall_clock_time_ms']

        # Group by category
        if successful_tests:
            aggregates['by_category'][fw_category]['frameworks'].append(fw_name)
            aggregates['by_category'][fw_category]['avg_time'] += avg_wall_time

    # Calculate complexity averages
    for complexity_data in aggregates['by_complexity'].values():
        if complexity_data['frameworks']:
            complexity_data['avg_time'] /= len(complexity_data['frameworks'])
            complexity_data['success_rate'] = 100.0

    # Calculate category averages
    for category_data in aggregates['by_category'].values():
        if category_data['frameworks']:
            category_data['avg_time'] /= len(category_data['frameworks'])

    return aggregates


def print_comparison_report(results: Dict, aggregates: Dict):
    """Print comprehensive comparison report"""
    print("=" * 100)
    print("📊 COMPREHENSIVE METRICS ANALYSIS & COMPARISON REPORT")
    print("=" * 100)
    print()

    # Overall statistics
    print("🎯 OVERALL STATISTICS")
    print("-" * 100)
    print(f"Total Frameworks Tested: {results['healthy_frameworks']}/{results['total_frameworks']}")
    print(f"Unhealthy Frameworks: {', '.join(results['unhealthy_frameworks']) if results['unhealthy_frameworks'] else 'None'}")
    print(f"Test Scenarios: {len(results['scenarios'])}")
    print(f"Benchmark Configurations: {len(results['benchmark_configs'])}")
    print()

    # Framework rankings by performance
    print("🏆 FRAMEWORK RANKINGS (by Average Response Time)")
    print("-" * 100)
    sorted_frameworks = sorted(
        aggregates['by_framework'].items(),
        key=lambda x: x[1]['avg_wall_clock_ms'] if x[1]['successful_tests'] > 0 else float('inf')
    )

    print(f"{'Rank':<6} {'Framework':<25} {'Category':<20} {'Avg Time':<12} {'Success Rate':<12} {'Payload Size'}")
    print("-" * 100)

    for rank, (fw_key, fw_stats) in enumerate(sorted_frameworks, 1):
        if fw_stats['successful_tests'] > 0:
            print(f"{rank:<6} {fw_stats['name']:<25} {fw_stats['category']:<20} "
                  f"{fw_stats['avg_wall_clock_ms']:>10.1f}ms {fw_stats['success_rate']:>10.1f}% "
                  f"{fw_stats['avg_payload_bytes']:>10.0f}B")

    print()

    # Performance by complexity
    print("📏 PERFORMANCE BY PAYLOAD COMPLEXITY")
    print("-" * 100)
    print(f"{'Complexity':<12} {'Avg Time':<15} {'Tested Frameworks'}")
    print("-" * 100)

    for complexity in ['SMALL', 'MEDIUM', 'LARGE', 'HUGE']:
        if complexity in aggregates['by_complexity']:
            data = aggregates['by_complexity'][complexity]
            print(f"{complexity:<12} {data['avg_time']:>12.1f}ms {len(data['frameworks']):<3} frameworks")

    print()

    # Performance by category
    print("🎨 PERFORMANCE BY FRAMEWORK CATEGORY")
    print("-" * 100)
    sorted_categories = sorted(
        aggregates['by_category'].items(),
        key=lambda x: x[1]['avg_time']
    )

    print(f"{'Category':<25} {'Avg Time':<15} {'Frameworks'}")
    print("-" * 100)

    for category, data in sorted_categories:
        print(f"{category:<25} {data['avg_time']:>12.1f}ms {', '.join(data['frameworks'][:3])}")
        if len(data['frameworks']) > 3:
            print(f"{'':25} {'':<15} {', '.join(data['frameworks'][3:])}")

    print()

    # Detailed metrics per framework
    print("📋 DETAILED FRAMEWORK METRICS")
    print("=" * 100)

    for fw_key, fw_stats in sorted_frameworks:
        if fw_stats['successful_tests'] > 0:
            print(f"\n{fw_stats['name']} ({fw_stats['category']})")
            print("-" * 100)
            print(f"  Success Rate:           {fw_stats['success_rate']:.1f}% ({fw_stats['successful_tests']}/{fw_stats['total_tests']} tests)")
            print(f"  Avg Wall Clock Time:    {fw_stats['avg_wall_clock_ms']:.2f}ms")
            print(f"  Avg Serialization:      {fw_stats['avg_serialization_ms']:.2f}ms")
            print(f"  Avg Deserialization:    {fw_stats['avg_deserialization_ms']:.2f}ms")
            print(f"  Avg Payload Size:       {fw_stats['avg_payload_bytes']:.0f} bytes")

    print()
    print("=" * 100)
    print("✅ ANALYSIS COMPLETE")
    print("=" * 100)


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 analyze_metrics.py <benchmark_results.json>")
        print("\nExample:")
        print("  python3 analyze_metrics.py comprehensive_benchmark_20251022_123456.json")
        sys.exit(1)

    results_file = sys.argv[1]
    print(f"📂 Loading results from: {results_file}")
    print()

    results = load_benchmark_results(results_file)
    aggregates = calculate_aggregates(results)
    print_comparison_report(results, aggregates)


if __name__ == '__main__':
    main()
