#!/usr/bin/env python3
"""
Complete Benchmark Automation with Report Generation - Phase 3

Orchestrates the complete benchmarking workflow:
1. Runs JMH microbenchmarks (pure performance)
2. Runs integration tests (real-world performance)
3. Analyzes performance delta (HTTP overhead)
4. Generates comprehensive reports (PDF, HTML, JSON, CSV)
5. Creates visualizations automatically

Usage:
    python run_complete_benchmark_with_reports.py [options]

Options:
    --skip-jmh          Skip JMH benchmarks (use cached results)
    --skip-integration  Skip integration tests (use cached results)
    --format FORMAT     Report format: pdf, html, json, csv, all (default: all)
    --output DIR        Output directory for reports (default: ./reports)
    --quick             Run quick benchmarks (fewer iterations)
"""

import argparse
import json
import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path
import time

try:
    import pandas as pd
    HAS_PANDAS = True
except ImportError:
    HAS_PANDAS = False
    print("⚠️  Pandas not available - CSV export will be limited")

try:
    import matplotlib.pyplot as plt
    import matplotlib
    matplotlib.use('Agg')  # Non-interactive backend
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False
    print("⚠️  Matplotlib not available - chart generation disabled")

try:
    from reportlab.lib.pagesizes import letter
    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.lib.units import inch
    from reportlab.lib import colors
    HAS_REPORTLAB = True
except ImportError:
    HAS_REPORTLAB = False
    print("⚠️  ReportLab not available - PDF generation disabled")


class CompleteBenchmarkOrchestrator:
    """Orchestrates complete benchmark workflow with automated reporting"""

    def __init__(self, args):
        self.args = args
        self.timestamp = datetime.now()
        self.output_dir = Path(args.output)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        # Results storage
        self.jmh_results = None
        self.integration_results = None
        self.comparison_results = None

        # File paths
        self.jmh_results_file = None
        self.integration_results_file = None

    def print_header(self, title):
        """Print a formatted header"""
        print("\n" + "=" * 80)
        print(f"🚀 {title}")
        print("=" * 80)

    def run_jmh_benchmarks(self):
        """Run JMH microbenchmarks and parse results"""
        if self.args.skip_jmh:
            print("⏭️  Skipping JMH benchmarks (using cached results)")
            return self.load_cached_jmh_results()

        self.print_header("PHASE 1: JMH Microbenchmarks (Pure Performance)")

        # Check if JMH module is built
        jmh_jar = Path("benchmark-jmh/target/benchmarks.jar")
        if not jmh_jar.exists():
            print("📦 Building JMH benchmarks module...")
            try:
                subprocess.run(
                    ["mvn", "clean", "package", "-DskipTests"],
                    cwd="benchmark-jmh",
                    check=True,
                    timeout=600  # 10 minutes
                )
            except subprocess.TimeoutExpired:
                print("❌ JMH build timeout - this may take longer on first run")
                print("💡 Try running manually: cd benchmark-jmh && mvn clean package")
                return None
            except subprocess.CalledProcessError as e:
                print(f"❌ JMH build failed: {e}")
                return None

        # Run JMH benchmarks
        print("🏃 Running JMH benchmarks...")
        print("⏱️  This will take 15-30 minutes for all frameworks")

        jmh_output_file = self.output_dir / f"jmh_results_{self.timestamp.strftime('%Y%m%d_%H%M%S')}.json"

        jmh_args = [
            "java", "-jar", str(jmh_jar),
            "-rf", "json",
            "-rff", str(jmh_output_file)
        ]

        if self.args.quick:
            jmh_args.extend(["-wi", "2", "-i", "3", "-f", "1"])  # Quick mode
            print("⚡ Quick mode: 2 warmup, 3 iterations, 1 fork")

        try:
            result = subprocess.run(
                jmh_args,
                cwd="benchmark-jmh",
                timeout=3600  # 1 hour max
            )

            if result.returncode == 0:
                print(f"✅ JMH benchmarks complete")
                print(f"📄 Results saved to: {jmh_output_file}")
                self.jmh_results_file = jmh_output_file
                return self.parse_jmh_results(jmh_output_file)
            else:
                print(f"❌ JMH benchmarks failed with exit code {result.returncode}")
                return None

        except subprocess.TimeoutExpired:
            print("❌ JMH benchmarks timeout")
            return None
        except Exception as e:
            print(f"❌ JMH execution error: {e}")
            return None

    def parse_jmh_results(self, results_file):
        """Parse JMH JSON results into structured format"""
        try:
            with open(results_file, 'r') as f:
                jmh_data = json.load(f)

            parsed = {}
            for benchmark in jmh_data:
                benchmark_name = benchmark.get('benchmark', '')
                # Extract framework name from benchmark (e.g., "JacksonBenchmark.serialize")
                if '.' in benchmark_name:
                    parts = benchmark_name.split('.')
                    framework = parts[0].replace('Benchmark', '')
                    operation = parts[1] if len(parts) > 1 else 'unknown'

                    if framework not in parsed:
                        parsed[framework] = {}

                    # Extract performance metrics
                    primary_metric = benchmark.get('primaryMetric', {})
                    score = primary_metric.get('score', 0)
                    score_error = primary_metric.get('scoreError', 0)
                    unit = primary_metric.get('scoreUnit', 'ops/s')

                    # Convert throughput (ops/s) to latency (ms/op) if needed
                    if unit == 'ops/s' and score > 0:
                        latency_ms = (1000.0 / score)
                        latency_error_ms = (1000.0 * score_error / (score * score)) if score > 0 else 0
                    else:
                        latency_ms = score
                        latency_error_ms = score_error

                    parsed[framework][operation] = {
                        'throughput_ops_per_sec': score if unit == 'ops/s' else (1000.0 / score if score > 0 else 0),
                        'latency_ms': latency_ms,
                        'latency_error_ms': latency_error_ms,
                        'unit': unit,
                        'raw_score': score,
                        'raw_error': score_error
                    }

            self.jmh_results = parsed
            return parsed

        except Exception as e:
            print(f"❌ Failed to parse JMH results: {e}")
            return None

    def load_cached_jmh_results(self):
        """Load most recent JMH results from output directory"""
        jmh_files = list(self.output_dir.glob("jmh_results_*.json"))
        if not jmh_files:
            print("❌ No cached JMH results found")
            return None

        latest_file = max(jmh_files, key=lambda p: p.stat().st_mtime)
        print(f"📂 Loading cached JMH results from: {latest_file}")
        return self.parse_jmh_results(latest_file)

    def run_integration_tests(self):
        """Run integration tests using existing benchmark script"""
        if self.args.skip_integration:
            print("⏭️  Skipping integration tests (using cached results)")
            return self.load_cached_integration_results()

        self.print_header("PHASE 2: Integration Tests (Real-World Performance)")

        print("🏃 Running integration tests...")
        print("⏱️  This will take 5-10 minutes depending on available frameworks")

        try:
            # Run the existing comprehensive benchmark script
            result = subprocess.run(
                [sys.executable, "final_comprehensive_benchmark.py"],
                timeout=1800,  # 30 minutes max
                capture_output=True,
                text=True
            )

            if result.returncode == 0:
                print("✅ Integration tests complete")

                # Find the most recent results file
                results_files = list(Path('.').glob("final_comprehensive_benchmark_*.json"))
                if results_files:
                    latest_file = max(results_files, key=lambda p: p.stat().st_mtime)
                    print(f"📄 Results saved to: {latest_file}")
                    self.integration_results_file = latest_file
                    return self.parse_integration_results(latest_file)
                else:
                    print("❌ Integration results file not found")
                    return None
            else:
                print(f"❌ Integration tests failed: {result.stderr[:200]}")
                return None

        except subprocess.TimeoutExpired:
            print("❌ Integration tests timeout")
            return None
        except Exception as e:
            print(f"❌ Integration execution error: {e}")
            return None

    def parse_integration_results(self, results_file):
        """Parse integration test results"""
        try:
            with open(results_file, 'r') as f:
                data = json.load(f)

            self.integration_results = data
            return data

        except Exception as e:
            print(f"❌ Failed to parse integration results: {e}")
            return None

    def load_cached_integration_results(self):
        """Load most recent integration results"""
        results_files = list(Path('.').glob("final_comprehensive_benchmark_*.json"))
        if not results_files:
            print("❌ No cached integration results found")
            return None

        latest_file = max(results_files, key=lambda p: p.stat().st_mtime)
        print(f"📂 Loading cached integration results from: {latest_file}")
        return self.parse_integration_results(latest_file)

    def analyze_performance_delta(self):
        """Analyze the difference between JMH and integration test results"""
        if not self.jmh_results or not self.integration_results:
            print("⚠️  Cannot analyze performance delta - missing results")
            return None

        self.print_header("PHASE 3: Performance Delta Analysis (HTTP Overhead)")

        comparison = {}

        # Extract integration test performance data
        integration_data = {}
        for framework_key, framework_data in self.integration_results.get('results', {}).items():
            framework_name = framework_data.get('name', framework_key)
            scenarios = framework_data.get('scenarios', {})

            # Use MEDIUM complexity as baseline for comparison
            if 'MEDIUM' in scenarios:
                medium_scenario = scenarios['MEDIUM']
                summary = medium_scenario.get('summary', {})
                avg_time = summary.get('avg_response_time_ms', 0)
                percentiles = summary.get('percentiles', {})

                integration_data[framework_key] = {
                    'name': framework_name,
                    'avg_time_ms': avg_time,
                    'p50_ms': percentiles.get('p50_ms', avg_time),
                    'p95_ms': percentiles.get('p95_ms', 0),
                    'p99_ms': percentiles.get('p99_ms', 0)
                }

        # Compare with JMH results
        for framework_key, jmh_data in self.jmh_results.items():
            # Find matching framework in integration results
            integration_match = None
            for int_key, int_data in integration_data.items():
                if framework_key.lower() in int_key.lower() or int_key.lower() in framework_key.lower():
                    integration_match = int_data
                    break

            if not integration_match:
                continue

            # Use roundtrip operation for fairest comparison
            roundtrip_data = jmh_data.get('roundtrip', jmh_data.get('serialize', {}))
            jmh_latency_ms = roundtrip_data.get('latency_ms', 0)

            integration_latency_ms = integration_match['p50_ms']

            # Calculate HTTP overhead
            http_overhead_ms = integration_latency_ms - jmh_latency_ms
            overhead_percent = ((http_overhead_ms / jmh_latency_ms) * 100) if jmh_latency_ms > 0 else 0

            comparison[framework_key] = {
                'framework': integration_match['name'],
                'jmh_latency_ms': round(jmh_latency_ms, 2),
                'integration_latency_ms': round(integration_latency_ms, 2),
                'http_overhead_ms': round(http_overhead_ms, 2),
                'overhead_percent': round(overhead_percent, 1),
                'jmh_throughput_ops_sec': roundtrip_data.get('throughput_ops_per_sec', 0)
            }

        self.comparison_results = comparison

        # Print comparison table
        print("\n📊 JMH vs Integration Performance Comparison")
        print("-" * 100)
        print(f"{'Framework':<20} {'JMH (Pure)':<15} {'Integration':<15} {'HTTP Overhead':<20} {'% Overhead'}")
        print("-" * 100)

        # Sort by overhead percentage (highest first)
        sorted_comparison = sorted(
            comparison.items(),
            key=lambda x: x[1]['overhead_percent'],
            reverse=True
        )

        for _, data in sorted_comparison:
            framework = data['framework']
            jmh_ms = data['jmh_latency_ms']
            int_ms = data['integration_latency_ms']
            overhead_ms = data['http_overhead_ms']
            overhead_pct = data['overhead_percent']

            print(f"{framework:<20} {jmh_ms:>10.2f} ms   {int_ms:>10.2f} ms   {overhead_ms:>10.2f} ms         {overhead_pct:>6.1f}%")

        print("\n💡 Key Insights:")
        if comparison:
            avg_overhead = sum(d['overhead_percent'] for d in comparison.values()) / len(comparison)
            print(f"   Average HTTP overhead: {avg_overhead:.1f}%")
            print(f"   Overhead range: {min(d['overhead_percent'] for d in comparison.values()):.1f}% - {max(d['overhead_percent'] for d in comparison.values()):.1f}%")

        return comparison

    def generate_charts(self):
        """Generate visualization charts"""
        if not HAS_MATPLOTLIB:
            print("⚠️  Skipping chart generation - matplotlib not available")
            return []

        self.print_header("PHASE 4: Chart Generation")

        charts_generated = []

        # Chart 1: JMH vs Integration comparison
        if self.comparison_results:
            chart_file = self.output_dir / f"performance_comparison_{self.timestamp.strftime('%Y%m%d_%H%M%S')}.png"
            self.create_comparison_chart(chart_file)
            charts_generated.append(chart_file)
            print(f"✅ Generated: {chart_file.name}")

        # Chart 2: HTTP Overhead percentage
        if self.comparison_results:
            chart_file = self.output_dir / f"http_overhead_{self.timestamp.strftime('%Y%m%d_%H%M%S')}.png"
            self.create_overhead_chart(chart_file)
            charts_generated.append(chart_file)
            print(f"✅ Generated: {chart_file.name}")

        # Chart 3: Throughput comparison
        if self.jmh_results:
            chart_file = self.output_dir / f"throughput_comparison_{self.timestamp.strftime('%Y%m%d_%H%M%S')}.png"
            self.create_throughput_chart(chart_file)
            charts_generated.append(chart_file)
            print(f"✅ Generated: {chart_file.name}")

        return charts_generated

    def create_comparison_chart(self, output_file):
        """Create stacked bar chart comparing JMH and HTTP overhead"""
        frameworks = []
        jmh_times = []
        overhead_times = []

        for _, data in sorted(self.comparison_results.items(), key=lambda x: x[1]['jmh_latency_ms']):
            frameworks.append(data['framework'][:15])  # Truncate long names
            jmh_times.append(data['jmh_latency_ms'])
            overhead_times.append(data['http_overhead_ms'])

        fig, ax = plt.subplots(figsize=(12, 6))
        x = range(len(frameworks))

        ax.bar(x, jmh_times, label='JMH (Pure Serialization)', color='#2ecc71')
        ax.bar(x, overhead_times, bottom=jmh_times, label='HTTP Overhead', color='#e74c3c')

        ax.set_xticks(x)
        ax.set_xticklabels(frameworks, rotation=45, ha='right')
        ax.set_ylabel('Time (ms)')
        ax.set_title('Pure Performance vs Real-World Performance')
        ax.legend()
        ax.grid(axis='y', alpha=0.3)

        plt.tight_layout()
        plt.savefig(output_file, dpi=300)
        plt.close()

    def create_overhead_chart(self, output_file):
        """Create bar chart showing HTTP overhead percentage"""
        frameworks = []
        overhead_percentages = []

        for _, data in sorted(self.comparison_results.items(), key=lambda x: x[1]['overhead_percent'], reverse=True):
            frameworks.append(data['framework'][:15])
            overhead_percentages.append(data['overhead_percent'])

        fig, ax = plt.subplots(figsize=(12, 6))
        x = range(len(frameworks))

        colors_list = ['#e74c3c' if pct > 200 else '#f39c12' if pct > 150 else '#2ecc71' for pct in overhead_percentages]
        ax.barh(x, overhead_percentages, color=colors_list)

        ax.set_yticks(x)
        ax.set_yticklabels(frameworks)
        ax.set_xlabel('HTTP Overhead (%)')
        ax.set_title('HTTP/Network Overhead by Framework')
        ax.grid(axis='x', alpha=0.3)

        # Add percentage labels
        for i, pct in enumerate(overhead_percentages):
            ax.text(pct + 5, i, f'{pct:.1f}%', va='center')

        plt.tight_layout()
        plt.savefig(output_file, dpi=300)
        plt.close()

    def create_throughput_chart(self, output_file):
        """Create bar chart showing JMH throughput"""
        frameworks = []
        throughput_values = []

        for framework, data in sorted(self.jmh_results.items()):
            roundtrip_data = data.get('roundtrip', data.get('serialize', {}))
            throughput = roundtrip_data.get('throughput_ops_per_sec', 0)
            if throughput > 0:
                frameworks.append(framework)
                throughput_values.append(throughput)

        # Sort by throughput
        sorted_data = sorted(zip(frameworks, throughput_values), key=lambda x: x[1], reverse=True)
        frameworks, throughput_values = zip(*sorted_data) if sorted_data else ([], [])

        fig, ax = plt.subplots(figsize=(12, 6))
        x = range(len(frameworks))

        ax.barh(x, throughput_values, color='#3498db')

        ax.set_yticks(x)
        ax.set_yticklabels(frameworks)
        ax.set_xlabel('Throughput (operations/second)')
        ax.set_title('Pure Serialization Throughput (JMH)')
        ax.grid(axis='x', alpha=0.3)

        # Add throughput labels
        for i, tput in enumerate(throughput_values):
            ax.text(tput + max(throughput_values) * 0.02, i, f'{tput:.0f}', va='center')

        plt.tight_layout()
        plt.savefig(output_file, dpi=300)
        plt.close()

    def generate_reports(self, charts):
        """Generate reports in requested formats"""
        self.print_header("PHASE 5: Report Generation")

        formats = []
        if self.args.format == 'all':
            formats = ['json', 'csv', 'html', 'pdf']
        else:
            formats = [self.args.format]

        generated_reports = []

        for fmt in formats:
            if fmt == 'json':
                report = self.generate_json_report()
                if report:
                    generated_reports.append(report)
            elif fmt == 'csv' and HAS_PANDAS:
                report = self.generate_csv_report()
                if report:
                    generated_reports.append(report)
            elif fmt == 'html':
                report = self.generate_html_report(charts)
                if report:
                    generated_reports.append(report)
            elif fmt == 'pdf' and HAS_REPORTLAB:
                report = self.generate_pdf_report(charts)
                if report:
                    generated_reports.append(report)

        return generated_reports

    def generate_json_report(self):
        """Generate comprehensive JSON report"""
        report_file = self.output_dir / f"benchmark_report_{self.timestamp.strftime('%Y%m%d_%H%M%S')}.json"

        report_data = {
            'metadata': {
                'timestamp': self.timestamp.isoformat(),
                'quick_mode': self.args.quick,
                'jmh_results_file': str(self.jmh_results_file) if self.jmh_results_file else None,
                'integration_results_file': str(self.integration_results_file) if self.integration_results_file else None
            },
            'jmh_results': self.jmh_results,
            'integration_results': self.integration_results,
            'performance_comparison': self.comparison_results,
            'executive_summary': self.generate_executive_summary()
        }

        with open(report_file, 'w') as f:
            json.dump(report_data, f, indent=2, default=str)

        print(f"✅ Generated: {report_file.name}")
        return report_file

    def generate_csv_report(self):
        """Generate CSV report with comparison data"""
        if not self.comparison_results:
            return None

        report_file = self.output_dir / f"benchmark_comparison_{self.timestamp.strftime('%Y%m%d_%H%M%S')}.csv"

        df = pd.DataFrame.from_dict(self.comparison_results, orient='index')
        df.to_csv(report_file, index=False)

        print(f"✅ Generated: {report_file.name}")
        return report_file

    def generate_html_report(self, charts):
        """Generate interactive HTML report"""
        report_file = self.output_dir / f"benchmark_report_{self.timestamp.strftime('%Y%m%d_%H%M%S')}.html"

        html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Benchmark Report - {self.timestamp.strftime('%Y-%m-%d %H:%M:%S')}</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background: #f5f5f5;
        }}
        .header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
        }}
        .section {{
            background: white;
            padding: 20px;
            margin-bottom: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }}
        th, td {{
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }}
        th {{
            background-color: #667eea;
            color: white;
        }}
        tr:hover {{
            background-color: #f5f5f5;
        }}
        .chart-container {{
            margin: 20px 0;
            text-align: center;
        }}
        .chart-container img {{
            max-width: 100%;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }}
        .metric {{
            display: inline-block;
            background: #667eea;
            color: white;
            padding: 15px 30px;
            margin: 10px;
            border-radius: 8px;
        }}
        .metric-value {{
            font-size: 24px;
            font-weight: bold;
        }}
        .metric-label {{
            font-size: 12px;
            opacity: 0.9;
        }}
    </style>
</head>
<body>
    <div class="header">
        <h1>🚀 Complete Benchmark Report</h1>
        <p>Generated: {self.timestamp.strftime('%Y-%m-%d %H:%M:%S')}</p>
    </div>

    {self._generate_html_executive_summary()}

    {self._generate_html_comparison_table()}

    {self._generate_html_charts(charts)}

    <div class="section">
        <h2>📚 Methodology</h2>
        <ul>
            <li><strong>JMH Benchmarks:</strong> Measure pure serialization performance without HTTP overhead</li>
            <li><strong>Integration Tests:</strong> Measure real-world performance with HTTP and Spring Boot overhead</li>
            <li><strong>Performance Delta:</strong> Calculated as Integration Time - JMH Time = HTTP Overhead</li>
        </ul>
    </div>

    <div class="section" style="text-align: center; color: #666;">
        <p>🤖 Generated with Claude Code - Java Serialization Frameworks Benchmark Suite</p>
    </div>
</body>
</html>
"""

        with open(report_file, 'w') as f:
            f.write(html_content)

        print(f"✅ Generated: {report_file.name}")
        return report_file

    def _generate_html_executive_summary(self):
        """Generate HTML for executive summary section"""
        summary = self.generate_executive_summary()
        if not summary:
            return ""

        return f"""
    <div class="section">
        <h2>📊 Executive Summary</h2>
        <div style="text-align: center;">
            <div class="metric">
                <div class="metric-label">Frameworks Tested</div>
                <div class="metric-value">{summary.get('frameworks_tested', 0)}</div>
            </div>
            <div class="metric">
                <div class="metric-label">Average HTTP Overhead</div>
                <div class="metric-value">{summary.get('avg_http_overhead_percent', 0):.1f}%</div>
            </div>
            <div class="metric">
                <div class="metric-label">Fastest (JMH)</div>
                <div class="metric-value">{summary.get('fastest_jmh', 'N/A')}</div>
            </div>
            <div class="metric">
                <div class="metric-label">Fastest (Integration)</div>
                <div class="metric-value">{summary.get('fastest_integration', 'N/A')}</div>
            </div>
        </div>
    </div>
"""

    def _generate_html_comparison_table(self):
        """Generate HTML comparison table"""
        if not self.comparison_results:
            return ""

        rows = ""
        for _, data in sorted(self.comparison_results.items(), key=lambda x: x[1]['jmh_latency_ms']):
            rows += f"""
            <tr>
                <td>{data['framework']}</td>
                <td>{data['jmh_latency_ms']:.2f} ms</td>
                <td>{data['integration_latency_ms']:.2f} ms</td>
                <td>{data['http_overhead_ms']:.2f} ms</td>
                <td>{data['overhead_percent']:.1f}%</td>
                <td>{data['jmh_throughput_ops_sec']:.0f} ops/s</td>
            </tr>
"""

        return f"""
    <div class="section">
        <h2>🔍 Performance Comparison</h2>
        <table>
            <thead>
                <tr>
                    <th>Framework</th>
                    <th>JMH (Pure)</th>
                    <th>Integration (Real)</th>
                    <th>HTTP Overhead</th>
                    <th>Overhead %</th>
                    <th>Throughput</th>
                </tr>
            </thead>
            <tbody>
                {rows}
            </tbody>
        </table>
    </div>
"""

    def _generate_html_charts(self, charts):
        """Generate HTML for charts section"""
        if not charts:
            return ""

        chart_html = '<div class="section"><h2>📈 Visualizations</h2>'

        for chart_file in charts:
            chart_html += f"""
        <div class="chart-container">
            <h3>{chart_file.stem.replace('_', ' ').title()}</h3>
            <img src="{chart_file.name}" alt="{chart_file.stem}">
        </div>
"""

        chart_html += '</div>'
        return chart_html

    def generate_pdf_report(self, charts):
        """Generate PDF report"""
        if not HAS_REPORTLAB:
            print("⚠️  Skipping PDF generation - reportlab not available")
            return None

        report_file = self.output_dir / f"benchmark_report_{self.timestamp.strftime('%Y%m%d_%H%M%S')}.pdf"

        doc = SimpleDocTemplate(str(report_file), pagesize=letter)
        story = []
        styles = getSampleStyleSheet()

        # Title
        title_style = ParagraphStyle(
            'CustomTitle',
            parent=styles['Heading1'],
            fontSize=24,
            textColor=colors.HexColor('#667eea'),
            spaceAfter=30
        )
        story.append(Paragraph("Complete Benchmark Report", title_style))
        story.append(Paragraph(f"Generated: {self.timestamp.strftime('%Y-%m-%d %H:%M:%S')}", styles['Normal']))
        story.append(Spacer(1, 0.3*inch))

        # Executive Summary
        summary = self.generate_executive_summary()
        if summary:
            story.append(Paragraph("Executive Summary", styles['Heading2']))
            story.append(Paragraph(f"Frameworks Tested: {summary.get('frameworks_tested', 0)}", styles['Normal']))
            story.append(Paragraph(f"Average HTTP Overhead: {summary.get('avg_http_overhead_percent', 0):.1f}%", styles['Normal']))
            story.append(Spacer(1, 0.2*inch))

        # Comparison Table
        if self.comparison_results:
            story.append(Paragraph("Performance Comparison", styles['Heading2']))

            table_data = [['Framework', 'JMH (ms)', 'Integration (ms)', 'Overhead (%)']]
            for _, data in sorted(self.comparison_results.items(), key=lambda x: x[1]['jmh_latency_ms']):
                table_data.append([
                    data['framework'][:20],
                    f"{data['jmh_latency_ms']:.2f}",
                    f"{data['integration_latency_ms']:.2f}",
                    f"{data['overhead_percent']:.1f}%"
                ])

            table = Table(table_data)
            table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#667eea')),
                ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
                ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
                ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
                ('FONTSIZE', (0, 0), (-1, 0), 12),
                ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
                ('BACKGROUND', (0, 1), (-1, -1), colors.beige),
                ('GRID', (0, 0), (-1, -1), 1, colors.black)
            ]))

            story.append(table)
            story.append(PageBreak())

        # Add charts
        if charts:
            story.append(Paragraph("Visualizations", styles['Heading2']))
            for chart_file in charts:
                # Note: This requires PIL/Pillow to be installed
                try:
                    from reportlab.platypus import Image
                    img = Image(str(chart_file), width=6*inch, height=3*inch)
                    story.append(img)
                    story.append(Spacer(1, 0.2*inch))
                except Exception as e:
                    story.append(Paragraph(f"Chart: {chart_file.name} (could not embed: {e})", styles['Normal']))

        doc.build(story)
        print(f"✅ Generated: {report_file.name}")
        return report_file

    def generate_executive_summary(self):
        """Generate executive summary data"""
        if not self.comparison_results:
            return None

        frameworks_tested = len(self.comparison_results)
        avg_overhead = sum(d['overhead_percent'] for d in self.comparison_results.values()) / frameworks_tested if frameworks_tested > 0 else 0

        # Find fastest frameworks
        fastest_jmh = min(self.comparison_results.items(), key=lambda x: x[1]['jmh_latency_ms'])[1]['framework'] if self.comparison_results else 'N/A'
        fastest_integration = min(self.comparison_results.items(), key=lambda x: x[1]['integration_latency_ms'])[1]['framework'] if self.comparison_results else 'N/A'

        return {
            'frameworks_tested': frameworks_tested,
            'avg_http_overhead_percent': avg_overhead,
            'fastest_jmh': fastest_jmh,
            'fastest_integration': fastest_integration,
            'timestamp': self.timestamp.isoformat()
        }

    def run_complete_workflow(self):
        """Execute the complete benchmark workflow"""
        start_time = time.time()

        self.print_header("COMPLETE BENCHMARK WORKFLOW")
        print(f"📁 Output directory: {self.output_dir}")
        print(f"⚙️  Quick mode: {'Enabled' if self.args.quick else 'Disabled'}")
        print(f"📊 Report format: {self.args.format}")

        # Phase 1: JMH Benchmarks
        if not self.args.skip_jmh:
            self.jmh_results = self.run_jmh_benchmarks()
        else:
            self.jmh_results = self.load_cached_jmh_results()

        # Phase 2: Integration Tests
        if not self.args.skip_integration:
            self.integration_results = self.run_integration_tests()
        else:
            self.integration_results = self.load_cached_integration_results()

        # Phase 3: Performance Delta Analysis
        self.comparison_results = self.analyze_performance_delta()

        # Phase 4: Chart Generation
        charts = self.generate_charts()

        # Phase 5: Report Generation
        reports = self.generate_reports(charts)

        # Final summary
        duration = time.time() - start_time

        self.print_header("WORKFLOW COMPLETE")
        print(f"⏱️  Total duration: {duration/60:.1f} minutes")
        print(f"\n📄 Generated Reports:")
        for report in reports:
            print(f"   ✅ {report}")
        print(f"\n📈 Generated Charts:")
        for chart in charts:
            print(f"   ✅ {chart}")

        print(f"\n💡 Next Steps:")
        print(f"   1. Open HTML report: {self.output_dir}/benchmark_report_*.html")
        print(f"   2. Review charts in: {self.output_dir}/")
        print(f"   3. Share PDF report with stakeholders")

        return True


def main():
    parser = argparse.ArgumentParser(
        description='Complete benchmark automation with report generation',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Run complete workflow (JMH + Integration + Reports)
  python run_complete_benchmark_with_reports.py

  # Quick benchmarks with all reports
  python run_complete_benchmark_with_reports.py --quick --format all

  # Skip JMH, use cached results
  python run_complete_benchmark_with_reports.py --skip-jmh

  # Generate only HTML report
  python run_complete_benchmark_with_reports.py --skip-jmh --skip-integration --format html
"""
    )

    parser.add_argument('--skip-jmh', action='store_true',
                       help='Skip JMH benchmarks (use cached results)')
    parser.add_argument('--skip-integration', action='store_true',
                       help='Skip integration tests (use cached results)')
    parser.add_argument('--format', choices=['pdf', 'html', 'json', 'csv', 'all'],
                       default='all', help='Report format (default: all)')
    parser.add_argument('--output', default='./reports',
                       help='Output directory for reports (default: ./reports)')
    parser.add_argument('--quick', action='store_true',
                       help='Run quick benchmarks (fewer iterations)')

    args = parser.parse_args()

    orchestrator = CompleteBenchmarkOrchestrator(args)

    try:
        success = orchestrator.run_complete_workflow()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n🛑 Workflow interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Workflow failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
