#!/usr/bin/env python3
"""
Benchmark History Tracker - Phase 4

Lightweight local time-series tracking using SQLite.
Tracks benchmark runs over time and detects performance regressions.

Usage:
    python benchmark_history_tracker.py record <results_file>
    python benchmark_history_tracker.py analyze
    python benchmark_history_tracker.py show --framework jackson
    python benchmark_history_tracker.py detect-regression
"""

import argparse
import json
import sqlite3
import sys
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple


class BenchmarkHistoryTracker:
    """Tracks benchmark history in local SQLite database"""

    def __init__(self, db_path: str = "benchmark_history.db"):
        self.db_path = db_path
        self.conn = None
        self.init_database()

    def init_database(self):
        """Initialize SQLite database schema"""
        self.conn = sqlite3.connect(self.db_path)
        self.conn.row_factory = sqlite3.Row

        # Create tables
        self.conn.executescript("""
            CREATE TABLE IF NOT EXISTS benchmark_runs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                run_type TEXT NOT NULL,  -- 'jmh', 'integration', 'complete'
                total_frameworks INTEGER,
                successful_frameworks INTEGER,
                duration_seconds REAL,
                notes TEXT
            );

            CREATE TABLE IF NOT EXISTS framework_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                run_id INTEGER NOT NULL,
                framework TEXT NOT NULL,
                result_type TEXT NOT NULL,  -- 'jmh', 'integration'

                -- Performance metrics
                latency_ms REAL,
                throughput_ops_sec REAL,
                success_rate REAL,

                -- Size metrics
                serialized_size_bytes INTEGER,
                compression_ratio REAL,

                -- Additional metrics
                p50_ms REAL,
                p95_ms REAL,
                p99_ms REAL,

                FOREIGN KEY (run_id) REFERENCES benchmark_runs(id)
            );

            CREATE TABLE IF NOT EXISTS performance_alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                framework TEXT NOT NULL,
                alert_type TEXT NOT NULL,  -- 'regression', 'improvement', 'anomaly'
                severity TEXT NOT NULL,     -- 'critical', 'warning', 'info'
                metric TEXT NOT NULL,
                old_value REAL,
                new_value REAL,
                change_percent REAL,
                message TEXT
            );

            -- Indexes for fast queries
            CREATE INDEX IF NOT EXISTS idx_run_timestamp ON benchmark_runs(timestamp);
            CREATE INDEX IF NOT EXISTS idx_framework_results ON framework_results(run_id, framework);
            CREATE INDEX IF NOT EXISTS idx_alerts_timestamp ON performance_alerts(timestamp);
        """)

        self.conn.commit()

    def close(self):
        """Close database connection"""
        if self.conn:
            self.conn.close()

    def record_benchmark_run(self, results_file: Path) -> int:
        """Record a benchmark run from results file"""
        print(f"📝 Recording benchmark run from: {results_file.name}")

        try:
            with open(results_file, 'r') as f:
                data = json.load(f)
        except Exception as e:
            print(f"❌ Failed to load results: {e}")
            return -1

        # Determine run type
        metadata = data.get('metadata', {})
        run_type = self._determine_run_type(data)

        # Extract run-level metrics
        timestamp = metadata.get('timestamp', datetime.now().isoformat())
        duration = metadata.get('duration_seconds', 0)

        # Count frameworks
        results = data.get('results', {})
        total_frameworks = len(results)
        successful_frameworks = sum(
            1 for r in results.values()
            if r.get('summary', {}).get('successful_tests', 0) > 0
        )

        # Insert run record
        cursor = self.conn.cursor()
        cursor.execute("""
            INSERT INTO benchmark_runs
            (timestamp, run_type, total_frameworks, successful_frameworks, duration_seconds)
            VALUES (?, ?, ?, ?, ?)
        """, (timestamp, run_type, total_frameworks, successful_frameworks, duration))

        run_id = cursor.lastrowid

        # Record framework results
        self._record_framework_results(run_id, data)

        # Detect regressions
        self._detect_regressions(run_id)

        self.conn.commit()

        print(f"✅ Recorded run #{run_id} with {total_frameworks} frameworks")
        return run_id

    def _determine_run_type(self, data: Dict) -> str:
        """Determine type of benchmark run from data"""
        if 'jmh_results' in data and 'integration_results' in data:
            return 'complete'
        elif 'jmh_results' in data:
            return 'jmh'
        elif 'results' in data:
            return 'integration'
        return 'unknown'

    def _record_framework_results(self, run_id: int, data: Dict):
        """Record individual framework results"""
        cursor = self.conn.cursor()

        # Record JMH results if present
        jmh_results = data.get('jmh_results', {})
        for framework, metrics in jmh_results.items():
            # Get roundtrip or serialize metrics
            roundtrip = metrics.get('roundtrip', metrics.get('serialize', {}))

            cursor.execute("""
                INSERT INTO framework_results
                (run_id, framework, result_type, latency_ms, throughput_ops_sec)
                VALUES (?, ?, 'jmh', ?, ?)
            """, (
                run_id,
                framework,
                roundtrip.get('latency_ms', 0),
                roundtrip.get('throughput_ops_per_sec', 0)
            ))

        # Record integration results if present
        integration_results = data.get('results', {})
        for framework_key, framework_data in integration_results.items():
            summary = framework_data.get('summary', {})

            # Get MEDIUM scenario for consistency
            scenarios = framework_data.get('scenarios', {})
            medium_scenario = scenarios.get('MEDIUM', {})
            scenario_summary = medium_scenario.get('summary', {})
            percentiles = scenario_summary.get('percentiles', {})

            cursor.execute("""
                INSERT INTO framework_results
                (run_id, framework, result_type, latency_ms, success_rate,
                 p50_ms, p95_ms, p99_ms)
                VALUES (?, ?, 'integration', ?, ?, ?, ?, ?)
            """, (
                run_id,
                framework_data.get('name', framework_key),
                summary.get('avg_response_time_ms', 0),
                summary.get('overall_success_rate', 0),
                percentiles.get('p50_ms', 0),
                percentiles.get('p95_ms', 0),
                percentiles.get('p99_ms', 0)
            ))

    def _detect_regressions(self, run_id: int):
        """Detect performance regressions by comparing with previous runs"""
        cursor = self.conn.cursor()

        # Get current run results
        current_results = cursor.execute("""
            SELECT framework, result_type, latency_ms, throughput_ops_sec
            FROM framework_results
            WHERE run_id = ?
        """, (run_id,)).fetchall()

        # Get previous run (most recent before current)
        previous_run = cursor.execute("""
            SELECT id FROM benchmark_runs
            WHERE id < ? AND run_type IN (
                SELECT run_type FROM benchmark_runs WHERE id = ?
            )
            ORDER BY id DESC LIMIT 1
        """, (run_id, run_id)).fetchone()

        if not previous_run:
            print("    ℹ️  No previous run to compare against")
            return

        prev_run_id = previous_run['id']

        # Compare each framework
        for current in current_results:
            framework = current['framework']
            result_type = current['result_type']

            # Get previous result for same framework
            previous = cursor.execute("""
                SELECT latency_ms, throughput_ops_sec
                FROM framework_results
                WHERE run_id = ? AND framework = ? AND result_type = ?
            """, (prev_run_id, framework, result_type)).fetchone()

            if not previous:
                continue

            # Check latency regression (higher is worse)
            if current['latency_ms'] > 0 and previous['latency_ms'] > 0:
                change_percent = ((current['latency_ms'] - previous['latency_ms'])
                                / previous['latency_ms'] * 100)

                if change_percent > 10:  # >10% slower
                    severity = 'critical' if change_percent > 25 else 'warning'
                    self._create_alert(
                        framework, 'regression', severity, 'latency_ms',
                        previous['latency_ms'], current['latency_ms'], change_percent,
                        f"{framework} latency increased by {change_percent:.1f}%"
                    )
                    print(f"    ⚠️  {framework}: Latency regression detected (+{change_percent:.1f}%)")

                elif change_percent < -10:  # >10% faster
                    self._create_alert(
                        framework, 'improvement', 'info', 'latency_ms',
                        previous['latency_ms'], current['latency_ms'], change_percent,
                        f"{framework} latency improved by {abs(change_percent):.1f}%"
                    )
                    print(f"    ✅ {framework}: Latency improvement detected ({abs(change_percent):.1f}%)")

            # Check throughput regression (lower is worse)
            if current['throughput_ops_sec'] > 0 and previous['throughput_ops_sec'] > 0:
                change_percent = ((current['throughput_ops_sec'] - previous['throughput_ops_sec'])
                                / previous['throughput_ops_sec'] * 100)

                if change_percent < -10:  # >10% slower
                    severity = 'critical' if change_percent < -25 else 'warning'
                    self._create_alert(
                        framework, 'regression', severity, 'throughput',
                        previous['throughput_ops_sec'], current['throughput_ops_sec'], change_percent,
                        f"{framework} throughput decreased by {abs(change_percent):.1f}%"
                    )

    def _create_alert(self, framework: str, alert_type: str, severity: str,
                     metric: str, old_value: float, new_value: float,
                     change_percent: float, message: str):
        """Create a performance alert"""
        cursor = self.conn.cursor()
        cursor.execute("""
            INSERT INTO performance_alerts
            (timestamp, framework, alert_type, severity, metric,
             old_value, new_value, change_percent, message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            datetime.now().isoformat(), framework, alert_type, severity,
            metric, old_value, new_value, change_percent, message
        ))

    def show_history(self, framework: Optional[str] = None, limit: int = 10):
        """Show benchmark history"""
        cursor = self.conn.cursor()

        if framework:
            print(f"\n📊 Benchmark History for {framework}")
        else:
            print(f"\n📊 Benchmark History (Last {limit} runs)")

        print("=" * 80)

        # Get runs
        if framework:
            query = """
                SELECT DISTINCT br.id, br.timestamp, br.run_type, br.duration_seconds
                FROM benchmark_runs br
                JOIN framework_results fr ON br.id = fr.run_id
                WHERE fr.framework = ?
                ORDER BY br.id DESC
                LIMIT ?
            """
            runs = cursor.execute(query, (framework, limit)).fetchall()
        else:
            query = """
                SELECT id, timestamp, run_type, total_frameworks,
                       successful_frameworks, duration_seconds
                FROM benchmark_runs
                ORDER BY id DESC
                LIMIT ?
            """
            runs = cursor.execute(query, (limit,)).fetchall()

        for run in runs:
            timestamp = datetime.fromisoformat(run['timestamp'])
            print(f"\n🏃 Run #{run['id']} - {timestamp.strftime('%Y-%m-%d %H:%M:%S')}")
            print(f"   Type: {run['run_type']}")

            if not framework:
                print(f"   Frameworks: {run['successful_frameworks']}/{run['total_frameworks']}")

            if run['duration_seconds']:
                print(f"   Duration: {run['duration_seconds']:.1f}s")

            # Show framework results if specific framework requested
            if framework:
                results = cursor.execute("""
                    SELECT result_type, latency_ms, throughput_ops_sec, success_rate
                    FROM framework_results
                    WHERE run_id = ? AND framework = ?
                """, (run['id'], framework)).fetchall()

                for result in results:
                    print(f"   {result['result_type'].upper()}:")
                    if result['latency_ms']:
                        print(f"      Latency: {result['latency_ms']:.2f}ms")
                    if result['throughput_ops_sec']:
                        print(f"      Throughput: {result['throughput_ops_sec']:.0f} ops/s")
                    if result['success_rate']:
                        print(f"      Success: {result['success_rate']:.1f}%")

    def show_alerts(self, severity: Optional[str] = None, limit: int = 20):
        """Show performance alerts"""
        cursor = self.conn.cursor()

        print(f"\n🚨 Performance Alerts")
        if severity:
            print(f"   Severity: {severity}")
        print("=" * 80)

        query = """
            SELECT timestamp, framework, alert_type, severity, metric,
                   old_value, new_value, change_percent, message
            FROM performance_alerts
        """

        params = []
        if severity:
            query += " WHERE severity = ?"
            params.append(severity)

        query += " ORDER BY timestamp DESC LIMIT ?"
        params.append(limit)

        alerts = cursor.execute(query, params).fetchall()

        if not alerts:
            print("✅ No alerts found")
            return

        for alert in alerts:
            timestamp = datetime.fromisoformat(alert['timestamp'])
            severity_emoji = {
                'critical': '🔴',
                'warning': '🟡',
                'info': '🔵'
            }.get(alert['severity'], '⚪')

            print(f"\n{severity_emoji} {alert['framework']} - {timestamp.strftime('%Y-%m-%d %H:%M')}")
            print(f"   {alert['message']}")
            print(f"   {alert['old_value']:.2f} → {alert['new_value']:.2f} ({alert['change_percent']:+.1f}%)")

    def analyze_trends(self, framework: str, metric: str = 'latency_ms', days: int = 30):
        """Analyze performance trends"""
        cursor = self.conn.cursor()

        cutoff = (datetime.now() - timedelta(days=days)).isoformat()

        results = cursor.execute("""
            SELECT br.timestamp, fr.latency_ms, fr.throughput_ops_sec
            FROM framework_results fr
            JOIN benchmark_runs br ON fr.run_id = br.id
            WHERE fr.framework = ? AND br.timestamp >= ?
            ORDER BY br.timestamp
        """, (framework, cutoff)).fetchall()

        if not results:
            print(f"❌ No data found for {framework} in last {days} days")
            return

        print(f"\n📈 Performance Trend: {framework} ({metric}, last {days} days)")
        print("=" * 80)

        values = []
        for result in results:
            if metric == 'latency_ms' and result['latency_ms']:
                values.append(result['latency_ms'])
            elif metric == 'throughput' and result['throughput_ops_sec']:
                values.append(result['throughput_ops_sec'])

        if not values:
            print("❌ No data available for requested metric")
            return

        # Calculate statistics
        avg = sum(values) / len(values)
        min_val = min(values)
        max_val = max(values)
        latest = values[-1]

        print(f"   Measurements: {len(values)}")
        print(f"   Average: {avg:.2f}")
        print(f"   Min: {min_val:.2f}")
        print(f"   Max: {max_val:.2f}")
        print(f"   Latest: {latest:.2f}")

        # Trend direction
        if len(values) >= 2:
            first_half_avg = sum(values[:len(values)//2]) / (len(values)//2)
            second_half_avg = sum(values[len(values)//2:]) / (len(values) - len(values)//2)
            change = ((second_half_avg - first_half_avg) / first_half_avg * 100)

            if abs(change) < 5:
                trend = "→ Stable"
            elif change > 0:
                trend = f"↗ Worsening (+{change:.1f}%)" if metric == 'latency_ms' else f"↗ Improving (+{change:.1f}%)"
            else:
                trend = f"↘ Improving ({change:.1f}%)" if metric == 'latency_ms' else f"↘ Worsening ({change:.1f}%)"

            print(f"   Trend: {trend}")

    def get_database_size(self) -> Tuple[int, int]:
        """Get database size and row counts"""
        db_path = Path(self.db_path)
        size_bytes = db_path.stat().st_size if db_path.exists() else 0

        cursor = self.conn.cursor()
        run_count = cursor.execute("SELECT COUNT(*) FROM benchmark_runs").fetchone()[0]

        return size_bytes, run_count


def main():
    parser = argparse.ArgumentParser(
        description='Track benchmark history and detect regressions',
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    subparsers = parser.add_subparsers(dest='command', help='Commands')

    # Record command
    record_parser = subparsers.add_parser('record', help='Record a benchmark run')
    record_parser.add_argument('results_file', type=str, help='Path to results JSON file')

    # Analyze command
    analyze_parser = subparsers.add_parser('analyze', help='Analyze benchmark history')

    # Show command
    show_parser = subparsers.add_parser('show', help='Show history for a framework')
    show_parser.add_argument('--framework', type=str, help='Framework name')
    show_parser.add_argument('--limit', type=int, default=10, help='Number of runs to show')

    # Alerts command
    alerts_parser = subparsers.add_parser('alerts', help='Show performance alerts')
    alerts_parser.add_argument('--severity', choices=['critical', 'warning', 'info'],
                              help='Filter by severity')
    alerts_parser.add_argument('--limit', type=int, default=20, help='Number of alerts')

    # Trend command
    trend_parser = subparsers.add_parser('trend', help='Analyze performance trends')
    trend_parser.add_argument('framework', type=str, help='Framework name')
    trend_parser.add_argument('--metric', choices=['latency_ms', 'throughput'],
                             default='latency_ms', help='Metric to analyze')
    trend_parser.add_argument('--days', type=int, default=30, help='Number of days')

    # Stats command
    stats_parser = subparsers.add_parser('stats', help='Show database statistics')

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return

    tracker = BenchmarkHistoryTracker()

    try:
        if args.command == 'record':
            results_path = Path(args.results_file)
            if not results_path.exists():
                print(f"❌ File not found: {results_path}")
                return
            tracker.record_benchmark_run(results_path)

        elif args.command == 'analyze':
            tracker.show_history(limit=20)
            print()
            tracker.show_alerts(limit=10)

        elif args.command == 'show':
            tracker.show_history(framework=args.framework, limit=args.limit)

        elif args.command == 'alerts':
            tracker.show_alerts(severity=args.severity, limit=args.limit)

        elif args.command == 'trend':
            tracker.analyze_trends(args.framework, args.metric, args.days)

        elif args.command == 'stats':
            size_bytes, run_count = tracker.get_database_size()
            size_mb = size_bytes / (1024 * 1024)
            print(f"\n📊 Database Statistics")
            print("=" * 60)
            print(f"Size: {size_mb:.2f}MB")
            print(f"Runs: {run_count}")
            print(f"Database: {tracker.db_path}")

    finally:
        tracker.close()


if __name__ == "__main__":
    main()
