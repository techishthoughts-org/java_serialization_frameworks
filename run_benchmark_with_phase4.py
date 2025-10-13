#!/usr/bin/env python3
"""
Complete Benchmark Workflow with Phase 4 Integration

Wraps the Phase 3 benchmark system with Phase 4 features:
- Automatic data cleanup (prevents GB-scale bloat)
- History tracking (SQLite-based time-series)
- Regression detection
- Storage monitoring

Usage:
    python run_benchmark_with_phase4.py [phase3_options]

All Phase 3 options are passed through:
    --skip-jmh, --skip-integration, --format, --output, --quick
"""

import argparse
import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path


def print_header(title):
    """Print formatted header"""
    print("\n" + "=" * 80)
    print(f"🚀 {title}")
    print("=" * 80)


def load_config():
    """Load benchmark configuration"""
    config_file = Path("benchmark_config.json")
    if config_file.exists():
        with open(config_file, 'r') as f:
            return json.load(f)
    return None


def run_cleanup(aggressive=False):
    """Run data cleanup before benchmark"""
    print_header("Phase 4: Pre-Benchmark Cleanup")

    config = load_config()
    if not config or not config.get('data_retention', {}).get('cleanup_on_startup', True):
        print("ℹ️  Automatic cleanup disabled in config")
        return

    print("🧹 Running automatic cleanup...")
    try:
        result = subprocess.run(
            [sys.executable, "cleanup_benchmark_data.py", "--auto"],
            timeout=60,
            capture_output=True,
            text=True
        )

        if result.returncode == 0:
            print("✅ Cleanup complete")
            # Print summary lines from output
            for line in result.stdout.split('\n')[-10:]:
                if line.strip():
                    print(f"   {line}")
        else:
            print(f"⚠️  Cleanup had issues: {result.stderr[:200]}")

    except subprocess.TimeoutExpired:
        print("⚠️  Cleanup timeout")
    except FileNotFoundError:
        print("⚠️  Cleanup script not found - skipping")
    except Exception as e:
        print(f"⚠️  Cleanup error: {e}")


def monitor_storage():
    """Monitor storage usage"""
    config = load_config()
    if not config or not config.get('monitoring', {}).get('enable_size_monitoring', True):
        return

    reports_dir = Path(config.get('storage_paths', {}).get('reports_dir', './reports'))
    if not reports_dir.exists():
        return

    # Calculate total size
    total_size = sum(f.stat().st_size for f in reports_dir.rglob('*') if f.is_file())
    size_mb = total_size / (1024 * 1024)

    max_size_mb = config.get('data_retention', {}).get('max_total_size_mb', 500)
    warning_size_mb = config.get('monitoring', {}).get('warning_size_mb', 400)

    print(f"\n📊 Storage Status:")
    print(f"   Current: {size_mb:.1f}MB")
    print(f"   Limit: {max_size_mb}MB")

    if size_mb > max_size_mb:
        print(f"   🔴 CRITICAL: Over limit! Run cleanup with --aggressive")
    elif size_mb > warning_size_mb:
        print(f"   🟡 WARNING: Approaching limit")
    else:
        print(f"   🟢 OK: {(size_mb/max_size_mb)*100:.1f}% used")


def run_benchmark(args):
    """Run the Phase 3 benchmark workflow"""
    print_header("Phases 1-3: JMH + Integration + Reports")

    # Build command
    cmd = [sys.executable, "run_complete_benchmark_with_reports.py"]

    # Pass through all arguments
    if args.skip_jmh:
        cmd.append("--skip-jmh")
    if args.skip_integration:
        cmd.append("--skip-integration")
    if args.format:
        cmd.extend(["--format", args.format])
    if args.output:
        cmd.extend(["--output", args.output])
    if args.quick:
        cmd.append("--quick")

    try:
        result = subprocess.run(cmd, check=False)
        return result.returncode == 0

    except KeyboardInterrupt:
        print("\n🛑 Benchmark interrupted")
        return False
    except Exception as e:
        print(f"❌ Benchmark error: {e}")
        return False


def record_to_history(output_dir):
    """Record benchmark results to history database"""
    print_header("Phase 4: History Tracking")

    config = load_config()
    if not config or not config.get('monitoring', {}).get('enable_history_tracking', True):
        print("ℹ️  History tracking disabled")
        return

    print("📝 Recording results to history...")

    # Find most recent results file
    output_path = Path(output_dir)
    if not output_path.exists():
        print("⚠️  Output directory not found")
        return

    # Look for JSON reports
    json_reports = list(output_path.glob("benchmark_report_*.json"))
    if not json_reports:
        print("⚠️  No benchmark reports found")
        return

    latest_report = max(json_reports, key=lambda p: p.stat().st_mtime)

    try:
        result = subprocess.run(
            [sys.executable, "benchmark_history_tracker.py", "record", str(latest_report)],
            timeout=30,
            capture_output=True,
            text=True
        )

        if result.returncode == 0:
            print("✅ Results recorded to history database")
            # Show any alerts from output
            for line in result.stdout.split('\n'):
                if '⚠️' in line or '✅' in line:
                    print(f"   {line}")
        else:
            print(f"⚠️  History recording failed: {result.stderr[:200]}")

    except subprocess.TimeoutExpired:
        print("⚠️  History recording timeout")
    except FileNotFoundError:
        print("⚠️  History tracker not found - skipping")
    except Exception as e:
        print(f"⚠️  History recording error: {e}")


def show_regression_summary():
    """Show recent regression alerts"""
    print_header("Phase 4: Regression Summary")

    try:
        result = subprocess.run(
            [sys.executable, "benchmark_history_tracker.py", "alerts", "--limit", "5"],
            timeout=10,
            capture_output=True,
            text=True
        )

        if result.returncode == 0:
            print(result.stdout)
        else:
            print("ℹ️  No regression data available yet")

    except:
        print("ℹ️  Regression detection not available")


def post_benchmark_cleanup():
    """Optional cleanup after benchmark if storage is critical"""
    config = load_config()
    if not config:
        return

    reports_dir = Path(config.get('storage_paths', {}).get('reports_dir', './reports'))
    if not reports_dir.exists():
        return

    # Check if we're over limit
    total_size = sum(f.stat().st_size for f in reports_dir.rglob('*') if f.is_file())
    size_mb = total_size / (1024 * 1024)
    critical_size_mb = config.get('monitoring', {}).get('critical_size_mb', 500)

    if size_mb > critical_size_mb:
        print(f"\n🔴 CRITICAL: Storage ({size_mb:.1f}MB) exceeds limit ({critical_size_mb}MB)")
        print("   Running aggressive cleanup...")

        try:
            subprocess.run(
                [sys.executable, "cleanup_benchmark_data.py", "--aggressive", "--auto"],
                timeout=60
            )
        except:
            print("   ⚠️  Automatic cleanup failed - please run manually")


def main():
    parser = argparse.ArgumentParser(
        description='Complete benchmark with Phase 4 features (cleanup + history)',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Complete workflow with all phases
  python run_benchmark_with_phase4.py

  # Quick mode with automatic cleanup
  python run_benchmark_with_phase4.py --quick

  # Skip benchmarks, just generate reports (no cleanup)
  python run_benchmark_with_phase4.py --skip-jmh --skip-integration

Phase 4 Features:
  - Automatic cleanup before running (configurable)
  - History tracking in SQLite database
  - Regression detection
  - Storage monitoring
  - Post-benchmark cleanup if needed
"""
    )

    # Phase 3 arguments (passed through)
    parser.add_argument('--skip-jmh', action='store_true',
                       help='Skip JMH benchmarks (use cached results)')
    parser.add_argument('--skip-integration', action='store_true',
                       help='Skip integration tests (use cached results)')
    parser.add_argument('--format', choices=['pdf', 'html', 'json', 'csv', 'all'],
                       default='all', help='Report format (default: all)')
    parser.add_argument('--output', default='./reports',
                       help='Output directory (default: ./reports)')
    parser.add_argument('--quick', action='store_true',
                       help='Quick mode (fewer iterations)')

    # Phase 4 options
    parser.add_argument('--skip-cleanup', action='store_true',
                       help='Skip pre-benchmark cleanup')
    parser.add_argument('--skip-history', action='store_true',
                       help='Skip history tracking')
    parser.add_argument('--aggressive-cleanup', action='store_true',
                       help='More aggressive cleanup (keep fewer files)')

    args = parser.parse_args()

    start_time = datetime.now()

    print("=" * 80)
    print("🚀 Complete Benchmark Workflow (Phases 1-4)")
    print("=" * 80)
    print(f"⏰ Started: {start_time.strftime('%Y-%m-%d %H:%M:%S')}")
    print()

    # Pre-benchmark cleanup
    if not args.skip_cleanup:
        run_cleanup(args.aggressive_cleanup)
        monitor_storage()

    # Run main benchmark workflow
    success = run_benchmark(args)

    if success:
        # Post-benchmark Phase 4 tasks
        if not args.skip_history:
            record_to_history(args.output)
            show_regression_summary()

        # Check if we need post-cleanup
        post_benchmark_cleanup()

        # Final status
        duration = (datetime.now() - start_time).total_seconds()

        print("\n" + "=" * 80)
        print("✅ Complete Workflow Finished Successfully")
        print("=" * 80)
        print(f"⏱️  Total duration: {duration/60:.1f} minutes")

        monitor_storage()

        print(f"\n💡 Phase 4 Commands:")
        print(f"   View history:    python benchmark_history_tracker.py show --framework jackson")
        print(f"   Check alerts:    python benchmark_history_tracker.py alerts")
        print(f"   Analyze trends:  python benchmark_history_tracker.py trend jackson")
        print(f"   Manual cleanup:  python cleanup_benchmark_data.py")

        sys.exit(0)
    else:
        print("\n❌ Workflow failed")
        sys.exit(1)


if __name__ == "__main__":
    main()
