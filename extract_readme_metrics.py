#!/usr/bin/env python3
"""
README Metrics Extractor
Automatically extracts and formats benchmark results for README.md

Usage:
    python extract_readme_metrics.py [options]

Options:
    --output FORMAT     Output format: markdown, json, csv (default: markdown)
    --source FILE       Specific benchmark file to use (default: latest)
    --update-readme     Automatically update README.md with results
"""

import argparse
import json
import glob
import os
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Optional


class READMEMetricsExtractor:
    """Extract and format benchmark metrics for README"""

    def __init__(self, benchmark_file: Optional[str] = None):
        self.benchmark_file = benchmark_file or self._find_latest_results()
        self.data = self._load_data()
        self.jmh_data = self._load_jmh_data()

    def _find_latest_results(self) -> str:
        """Find the most recent benchmark results file"""
        patterns = [
            'final_comprehensive_benchmark_*.json',
            'reports/benchmark_report_*.json'
        ]

        all_files = []
        for pattern in patterns:
            all_files.extend(glob.glob(pattern))

        if not all_files:
            raise FileNotFoundError(
                "No benchmark results found. Please run benchmarks first:\n"
                "  python run_benchmark_with_phase4.py\n"
                "or:\n"
                "  ./run_complete_benchmark.sh"
            )

        # Return most recent file
        return max(all_files, key=os.path.getmtime)

    def _load_data(self) -> Dict:
        """Load benchmark data"""
        try:
            with open(self.benchmark_file, 'r') as f:
                return json.load(f)
        except Exception as e:
            raise RuntimeError(f"Failed to load benchmark data: {e}")

    def _load_jmh_data(self) -> Optional[Dict]:
        """Load JMH benchmark data if available"""
        jmh_files = glob.glob('reports/jmh_results_*.json')
        if not jmh_files:
            return None

        latest_jmh = max(jmh_files, key=os.path.getmtime)
        try:
            with open(latest_jmh, 'r') as f:
                raw_data = json.load(f)

            # Parse JMH results
            parsed = {}
            for benchmark in raw_data:
                benchmark_name = benchmark.get('benchmark', '')
                if '.' in benchmark_name:
                    parts = benchmark_name.split('.')
                    framework = parts[0].replace('Benchmark', '')

                    if framework not in parsed:
                        parsed[framework] = {}

                    primary_metric = benchmark.get('primaryMetric', {})
                    score = primary_metric.get('score', 0)
                    unit = primary_metric.get('scoreUnit', 'ops/s')

                    # Convert to ms if needed
                    if unit == 'ops/s' and score > 0:
                        latency_ms = 1000.0 / score
                    else:
                        latency_ms = score

                    parsed[framework]['latency_ms'] = latency_ms

            return parsed
        except Exception as e:
            print(f"Warning: Could not load JMH data: {e}", file=sys.stderr)
            return None

    def extract_test_environment(self) -> Dict[str, Any]:
        """Extract test environment information"""
        metadata = self.data.get('metadata', {})
        summary = self.data.get('summary', {})

        return {
            'jvm_version': self._get_jvm_version(),
            'total_frameworks': summary.get('total_frameworks', 0),
            'total_tests': summary.get('total_tests', 0),
            'overall_success_rate': summary.get('overall_success_rate', 0),
            'test_duration_seconds': summary.get('total_duration_seconds', 0),
            'timestamp': metadata.get('timestamp', datetime.now().isoformat())
        }

    def _get_jvm_version(self) -> str:
        """Get JVM version"""
        try:
            import subprocess
            result = subprocess.run(
                ['java', '-version'],
                capture_output=True,
                text=True
            )
            # Parse version from stderr (java prints version there)
            version_line = result.stderr.split('\n')[0]
            # Extract version number
            if 'version' in version_line:
                version = version_line.split('"')[1]
                return f"OpenJDK {version}"
        except:
            pass
        return "Unknown"

    def extract_framework_rankings(self) -> List[Dict[str, Any]]:
        """Extract framework rankings by performance"""
        results = self.data.get('results', {})
        rankings = []

        for framework_key, framework_data in results.items():
            name = framework_data.get('name', framework_key)
            scenarios = framework_data.get('scenarios', {})

            # Calculate average response time across all scenarios
            times = []
            success_rates = []

            for scenario_name, scenario_data in scenarios.items():
                summary = scenario_data.get('summary', {})
                times.append(summary.get('avg_response_time_ms', 0))
                success_rates.append(summary.get('success_rate', 0))

            avg_time = sum(times) / len(times) if times else 0
            avg_success = sum(success_rates) / len(success_rates) if success_rates else 0

            # Get JMH data if available
            jmh_time = None
            http_overhead = None
            if self.jmh_data and framework_key in self.jmh_data:
                jmh_time = self.jmh_data[framework_key].get('latency_ms', 0)
                if jmh_time > 0 and avg_time > 0:
                    http_overhead = ((avg_time - jmh_time) / jmh_time) * 100

            rankings.append({
                'framework': name,
                'key': framework_key,
                'avg_response_time_ms': round(avg_time, 1),
                'success_rate': round(avg_success, 1),
                'jmh_time_ms': round(jmh_time, 2) if jmh_time else None,
                'http_overhead_percent': round(http_overhead, 1) if http_overhead else None,
                'status': '🟢' if avg_success >= 100 else '🟡' if avg_success >= 80 else '🔴'
            })

        # Sort by response time (fastest first)
        rankings.sort(key=lambda x: x['avg_response_time_ms'])

        return rankings

    def extract_scenario_analysis(self) -> List[Dict[str, Any]]:
        """Extract performance by scenario (payload size)"""
        results = self.data.get('results', {})
        scenarios_data = {}

        # Collect all scenarios
        for framework_data in results.values():
            scenarios = framework_data.get('scenarios', {})
            for scenario_name, scenario_data in scenarios.items():
                if scenario_name not in scenarios_data:
                    scenarios_data[scenario_name] = {
                        'times': [],
                        'success_rates': [],
                        'best_framework': None,
                        'best_time': float('inf')
                    }

                summary = scenario_data.get('summary', {})
                avg_time = summary.get('avg_response_time_ms', 0)
                success_rate = summary.get('success_rate', 0)

                scenarios_data[scenario_name]['times'].append(avg_time)
                scenarios_data[scenario_name]['success_rates'].append(success_rate)

                # Track best framework for this scenario
                if avg_time < scenarios_data[scenario_name]['best_time'] and success_rate >= 80:
                    scenarios_data[scenario_name]['best_time'] = avg_time
                    scenarios_data[scenario_name]['best_framework'] = framework_data.get('name')

        # Calculate averages
        analysis = []
        for scenario_name, data in scenarios_data.items():
            avg_success = sum(data['success_rates']) / len(data['success_rates']) if data['success_rates'] else 0
            avg_time = sum(data['times']) / len(data['times']) if data['times'] else 0

            analysis.append({
                'scenario': scenario_name,
                'avg_success_rate': round(avg_success, 1),
                'avg_response_time_ms': round(avg_time, 1),
                'best_framework': data['best_framework'] or 'N/A'
            })

        # Sort by scenario complexity
        scenario_order = {'SMALL': 0, 'MEDIUM': 1, 'LARGE': 2, 'HUGE': 3, 'MASSIVE': 4}
        analysis.sort(key=lambda x: scenario_order.get(x['scenario'], 999))

        return analysis

    def format_markdown(self) -> str:
        """Format results as Markdown for README"""
        env = self.extract_test_environment()
        rankings = self.extract_framework_rankings()
        scenarios = self.extract_scenario_analysis()

        md = []

        # Test Environment
        md.append("### 🏆 Latest Benchmark Results\n")
        md.append("**Test Environment:**\n")
        md.append(f"- **JVM**: {env['jvm_version']}")
        md.append(f"- **Total Frameworks**: {env['total_frameworks']}")
        md.append(f"- **Total Tests**: {env['total_tests']}")
        md.append(f"- **Overall Success Rate**: {env['overall_success_rate']:.1f}%")
        md.append(f"- **Test Duration**: {env['test_duration_seconds']:.0f} seconds")
        md.append("")

        # Framework Rankings by Success Rate
        md.append("### 🥇 Framework Ranking by Success Rate\n")
        md.append("| Rank | Framework | Success Rate | Avg Response Time | Status |")
        md.append("|------|-----------|-------------|-------------------|--------|")

        sorted_by_success = sorted(rankings, key=lambda x: (-x['success_rate'], x['avg_response_time_ms']))
        for i, r in enumerate(sorted_by_success, 1):
            status_text = "Ultra-fast & reliable" if r['success_rate'] >= 100 and r['avg_response_time_ms'] < 10 else \
                         "Fast & reliable" if r['success_rate'] >= 100 else \
                         "Reliable" if r['success_rate'] >= 95 else \
                         "Good" if r['success_rate'] >= 80 else "Needs attention"

            md.append(f"| {i} | {r['status']} {r['framework']} | {r['success_rate']:.1f}% | "
                     f"{r['avg_response_time_ms']:.1f}ms | {status_text} |")

        md.append("")

        # Performance Rankings
        md.append("### ⚡ Performance Ranking (Response Time)\n")
        md.append("| Rank | Framework | Avg Response Time | Success Rate | Best Use Case |")
        md.append("|------|-----------|------------------|-------------|---------------|")

        use_cases = {
            'Jackson': 'Web APIs, REST services',
            'Protobuf': 'gRPC, microservices',
            'Avro': 'Big data, Kafka',
            'Kryo': 'High-performance Java',
            'MessagePack': 'IoT, compact data',
            'Thrift': 'Cross-language RPC',
            'CapnProto': 'Zero-copy scenarios',
            'FST': 'Fast Java serialization',
            'FlatBuffers': 'Game dev, mobile',
            'gRPC': 'Microservices, HTTP/2',
            'CBOR': 'IoT, constrained networks',
            'BSON': 'MongoDB, document DBs',
            'Arrow': 'Analytics, big data',
            'SBE': 'Ultra-low latency trading',
            'Parquet': 'Data warehousing'
        }

        for i, r in enumerate(rankings, 1):
            use_case = use_cases.get(r['framework'], 'General purpose')
            md.append(f"| {i} | ⚡ {r['framework']} | {r['avg_response_time_ms']:.1f}ms | "
                     f"{r['success_rate']:.1f}% | {use_case} |")

        md.append("")

        # Scenario Analysis
        md.append("### 📋 Scenario Analysis\n")
        md.append("| Scenario | Avg Success Rate | Avg Response Time | Best Framework |")
        md.append("|----------|-----------------|-------------------|----------------|")

        for s in scenarios:
            md.append(f"| {s['scenario']} | {s['avg_success_rate']:.1f}% | "
                     f"{s['avg_response_time_ms']:.1f}ms | {s['best_framework']} |")

        md.append("")

        # JMH Comparison (if available)
        if self.jmh_data:
            md.append("### 🔬 JMH vs Integration Performance\n")
            md.append("| Framework | JMH (Pure) | Integration (Real) | HTTP Overhead |")
            md.append("|-----------|-----------|-------------------|---------------|")

            for r in rankings:
                if r['jmh_time_ms'] and r['http_overhead_percent']:
                    md.append(f"| {r['framework']} | {r['jmh_time_ms']:.2f}ms | "
                             f"{r['avg_response_time_ms']:.1f}ms | "
                             f"{r['http_overhead_percent']:.1f}% |")

            md.append("")

        return '\n'.join(md)

    def format_json(self) -> str:
        """Format results as JSON"""
        return json.dumps({
            'test_environment': self.extract_test_environment(),
            'framework_rankings': self.extract_framework_rankings(),
            'scenario_analysis': self.extract_scenario_analysis()
        }, indent=2)

    def format_csv(self) -> str:
        """Format results as CSV"""
        rankings = self.extract_framework_rankings()

        csv = ['Framework,Avg Response Time (ms),Success Rate (%),JMH Time (ms),HTTP Overhead (%)']
        for r in rankings:
            jmh = r['jmh_time_ms'] if r['jmh_time_ms'] else ''
            overhead = r['http_overhead_percent'] if r['http_overhead_percent'] else ''
            csv.append(f"{r['framework']},{r['avg_response_time_ms']},{r['success_rate']},{jmh},{overhead}")

        return '\n'.join(csv)

    def update_readme(self, dry_run: bool = False):
        """Update README.md with latest benchmark results"""
        readme_path = Path('README.md')
        if not readme_path.exists():
            raise FileNotFoundError("README.md not found")

        with open(readme_path, 'r') as f:
            content = f.read()

        # Find the results section
        start_marker = '### 🏆 Latest Benchmark Results'
        end_marker = '## 🚀 Interactive Dashboard'

        start_idx = content.find(start_marker)
        end_idx = content.find(end_marker)

        if start_idx == -1 or end_idx == -1:
            raise ValueError("Could not find benchmark results section in README.md")

        # Generate new content
        new_section = self.format_markdown()

        # Replace section
        new_content = content[:start_idx] + new_section + '\n' + content[end_idx:]

        if dry_run:
            print("=== DRY RUN: Would update README.md with: ===\n")
            print(new_section)
            return

        # Backup old README
        backup_path = readme_path.with_suffix('.md.backup')
        with open(backup_path, 'w') as f:
            f.write(content)

        # Write new README
        with open(readme_path, 'w') as f:
            f.write(new_content)

        print(f"✅ README.md updated successfully")
        print(f"📄 Backup saved to: {backup_path}")


def main():
    parser = argparse.ArgumentParser(
        description='Extract and format benchmark metrics for README.md',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Generate markdown for README (default)
  python extract_readme_metrics.py

  # Generate JSON output
  python extract_readme_metrics.py --output json

  # Use specific benchmark file
  python extract_readme_metrics.py --source my_results.json

  # Update README.md automatically
  python extract_readme_metrics.py --update-readme

  # Dry run (preview changes)
  python extract_readme_metrics.py --update-readme --dry-run
        """
    )

    parser.add_argument('--output', choices=['markdown', 'json', 'csv'],
                       default='markdown', help='Output format')
    parser.add_argument('--source', help='Specific benchmark file to use')
    parser.add_argument('--update-readme', action='store_true',
                       help='Automatically update README.md')
    parser.add_argument('--dry-run', action='store_true',
                       help='Preview changes without modifying files')

    args = parser.parse_args()

    try:
        extractor = READMEMetricsExtractor(args.source)

        if args.update_readme:
            extractor.update_readme(dry_run=args.dry_run)
        else:
            # Output to stdout
            if args.output == 'markdown':
                print(extractor.format_markdown())
            elif args.output == 'json':
                print(extractor.format_json())
            elif args.output == 'csv':
                print(extractor.format_csv())

    except Exception as e:
        print(f"❌ Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
