#!/usr/bin/env python3
"""
Benchmark Data Cleanup Script - Phase 4

Automatically purges old benchmark data to prevent GB-scale bloat.
Enforces retention policies defined in benchmark_config.json.

Usage:
    python cleanup_benchmark_data.py                  # Interactive cleanup
    python cleanup_benchmark_data.py --auto           # Automatic cleanup
    python cleanup_benchmark_data.py --dry-run        # Preview only
    python cleanup_benchmark_data.py --aggressive     # Maximum cleanup
"""

import argparse
import json
import os
import shutil
import sqlite3
import sys
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Tuple


class BenchmarkDataCleaner:
    """Manages cleanup of benchmark data with retention policies"""

    def __init__(self, config_path: str = "benchmark_config.json"):
        self.config = self.load_config(config_path)
        self.retention = self.config.get('data_retention', {})
        self.storage = self.config.get('storage_paths', {})
        self.dry_run = False
        self.files_deleted = 0
        self.bytes_freed = 0

    def load_config(self, config_path: str) -> Dict:
        """Load configuration from JSON file"""
        try:
            with open(config_path, 'r') as f:
                return json.load(f)
        except FileNotFoundError:
            print(f"⚠️  Config file not found: {config_path}")
            print("    Using default retention policies")
            return self.get_default_config()

    def get_default_config(self) -> Dict:
        """Return default configuration"""
        return {
            'data_retention': {
                'max_benchmark_results': 10,
                'max_jmh_results': 10,
                'max_reports_per_format': 5,
                'max_charts': 15,
                'max_history_days': 30,
                'max_total_size_mb': 500,
                'auto_cleanup_enabled': True
            },
            'storage_paths': {
                'reports_dir': './reports',
                'jmh_results_dir': './reports',
                'history_db': './benchmark_history.db',
                'charts_dir': './reports',
                'logs_dir': './logs'
            }
        }

    def get_directory_size(self, path: Path) -> int:
        """Calculate total size of directory in bytes"""
        if not path.exists():
            return 0

        total = 0
        for entry in path.rglob('*'):
            if entry.is_file():
                try:
                    total += entry.stat().st_size
                except (OSError, PermissionError):
                    pass
        return total

    def format_size(self, bytes_size: int) -> str:
        """Format bytes to human-readable string"""
        for unit in ['B', 'KB', 'MB', 'GB']:
            if bytes_size < 1024.0:
                return f"{bytes_size:.2f} {unit}"
            bytes_size /= 1024.0
        return f"{bytes_size:.2f} TB"

    def get_files_by_pattern(self, directory: Path, pattern: str) -> List[Tuple[Path, float]]:
        """Get files matching pattern with their modification times"""
        if not directory.exists():
            return []

        files = []
        for file_path in directory.glob(pattern):
            if file_path.is_file():
                try:
                    mtime = file_path.stat().st_mtime
                    files.append((file_path, mtime))
                except (OSError, PermissionError):
                    pass

        # Sort by modification time (newest first)
        files.sort(key=lambda x: x[1], reverse=True)
        return files

    def delete_file(self, file_path: Path) -> int:
        """Delete file and return bytes freed"""
        try:
            size = file_path.stat().st_size
            if not self.dry_run:
                file_path.unlink()
            self.files_deleted += 1
            self.bytes_freed += size
            return size
        except (OSError, PermissionError) as e:
            print(f"    ⚠️  Failed to delete {file_path.name}: {e}")
            return 0

    def cleanup_by_count(self, directory: Path, pattern: str, max_files: int, description: str):
        """Keep only the N most recent files matching pattern"""
        print(f"\n📁 Cleaning {description}...")

        files = self.get_files_by_pattern(directory, pattern)

        if not files:
            print(f"    ℹ️  No files found")
            return

        total_files = len(files)
        files_to_delete = files[max_files:] if len(files) > max_files else []

        print(f"    Found: {total_files} files")
        print(f"    Keeping: {min(total_files, max_files)} most recent")
        print(f"    Deleting: {len(files_to_delete)} old files")

        if files_to_delete:
            total_size = 0
            for file_path, _ in files_to_delete:
                size = self.delete_file(file_path)
                total_size += size
                if self.dry_run:
                    print(f"    [DRY-RUN] Would delete: {file_path.name} ({self.format_size(size)})")

            if not self.dry_run:
                print(f"    ✅ Freed: {self.format_size(total_size)}")

    def cleanup_by_age(self, directory: Path, pattern: str, max_days: int, description: str):
        """Delete files older than max_days"""
        print(f"\n📅 Cleaning old {description} (>{max_days} days)...")

        files = self.get_files_by_pattern(directory, pattern)

        if not files:
            print(f"    ℹ️  No files found")
            return

        cutoff_time = (datetime.now() - timedelta(days=max_days)).timestamp()
        old_files = [(f, t) for f, t in files if t < cutoff_time]

        print(f"    Found: {len(files)} files")
        print(f"    Deleting: {len(old_files)} files older than {max_days} days")

        if old_files:
            total_size = 0
            for file_path, mtime in old_files:
                age_days = (datetime.now().timestamp() - mtime) / 86400
                size = self.delete_file(file_path)
                total_size += size
                if self.dry_run:
                    print(f"    [DRY-RUN] Would delete: {file_path.name} (age: {age_days:.1f} days, size: {self.format_size(size)})")

            if not self.dry_run:
                print(f"    ✅ Freed: {self.format_size(total_size)}")

    def cleanup_empty_directories(self, base_path: Path):
        """Remove empty directories"""
        print(f"\n📂 Cleaning empty directories...")

        if not base_path.exists():
            return

        empty_dirs = []
        for dir_path in base_path.rglob('*'):
            if dir_path.is_dir():
                try:
                    if not any(dir_path.iterdir()):
                        empty_dirs.append(dir_path)
                except (OSError, PermissionError):
                    pass

        if empty_dirs:
            print(f"    Found: {len(empty_dirs)} empty directories")
            for dir_path in empty_dirs:
                try:
                    if not self.dry_run:
                        dir_path.rmdir()
                    else:
                        print(f"    [DRY-RUN] Would remove: {dir_path}")
                except (OSError, PermissionError) as e:
                    print(f"    ⚠️  Failed to remove {dir_path}: {e}")

            if not self.dry_run:
                print(f"    ✅ Removed {len(empty_dirs)} directories")
        else:
            print(f"    ℹ️  No empty directories found")

    def cleanup_large_files(self, directory: Path, max_size_mb: int):
        """Identify and optionally delete large files"""
        print(f"\n📊 Scanning for large files (>{max_size_mb}MB)...")

        if not directory.exists():
            return

        large_files = []
        for file_path in directory.rglob('*'):
            if file_path.is_file():
                try:
                    size_mb = file_path.stat().st_size / (1024 * 1024)
                    if size_mb > max_size_mb:
                        large_files.append((file_path, size_mb))
                except (OSError, PermissionError):
                    pass

        if large_files:
            large_files.sort(key=lambda x: x[1], reverse=True)
            print(f"    Found {len(large_files)} large files:")
            for file_path, size_mb in large_files:
                print(f"    ⚠️  {file_path.name}: {size_mb:.2f}MB")
            print(f"    💡 Consider reviewing these files manually")
        else:
            print(f"    ✅ No large files found")

    def vacuum_database(self, db_path: Path):
        """Vacuum SQLite database to reclaim space"""
        if not db_path.exists():
            return

        print(f"\n🗄️  Vacuuming database: {db_path.name}...")

        try:
            size_before = db_path.stat().st_size

            if not self.dry_run:
                conn = sqlite3.connect(str(db_path))
                conn.execute("VACUUM")
                conn.close()

            size_after = db_path.stat().st_size if not self.dry_run else size_before
            saved = size_before - size_after

            if saved > 0:
                print(f"    ✅ Reclaimed: {self.format_size(saved)}")
            else:
                print(f"    ℹ️  No space reclaimed")

        except Exception as e:
            print(f"    ⚠️  Failed to vacuum database: {e}")

    def run_cleanup(self, aggressive: bool = False):
        """Execute complete cleanup workflow"""
        print("=" * 60)
        print("🧹 Benchmark Data Cleanup")
        print("=" * 60)

        if self.dry_run:
            print("\n🔍 DRY-RUN MODE - No files will be deleted\n")

        # Get initial sizes
        reports_dir = Path(self.storage.get('reports_dir', './reports'))

        if reports_dir.exists():
            initial_size = self.get_directory_size(reports_dir)
            print(f"\n📊 Current storage usage: {self.format_size(initial_size)}")

        # Cleanup benchmark results
        max_benchmark = self.retention.get('max_benchmark_results', 10)
        if aggressive:
            max_benchmark = max(3, max_benchmark // 2)

        self.cleanup_by_count(
            reports_dir,
            'final_comprehensive_benchmark_*.json',
            max_benchmark,
            'integration test results'
        )

        # Cleanup JMH results
        max_jmh = self.retention.get('max_jmh_results', 10)
        if aggressive:
            max_jmh = max(3, max_jmh // 2)

        self.cleanup_by_count(
            reports_dir,
            'jmh_results_*.json',
            max_jmh,
            'JMH results'
        )

        # Cleanup reports by format
        max_reports = self.retention.get('max_reports_per_format', 5)
        if aggressive:
            max_reports = max(2, max_reports // 2)

        self.cleanup_by_count(reports_dir, 'benchmark_report_*.html', max_reports, 'HTML reports')
        self.cleanup_by_count(reports_dir, 'benchmark_report_*.pdf', max_reports, 'PDF reports')
        self.cleanup_by_count(reports_dir, 'benchmark_report_*.json', max_reports, 'JSON reports')
        self.cleanup_by_count(reports_dir, 'benchmark_comparison_*.csv', max_reports, 'CSV reports')

        # Cleanup charts
        max_charts = self.retention.get('max_charts', 15)
        if aggressive:
            max_charts = max(5, max_charts // 2)

        self.cleanup_by_count(reports_dir, '*.png', max_charts, 'charts')

        # Cleanup by age
        max_days = self.retention.get('max_history_days', 30)
        if aggressive:
            max_days = max(7, max_days // 2)

        self.cleanup_by_age(reports_dir, '*_*.json', max_days, 'old results')
        self.cleanup_by_age(reports_dir, '*.png', max_days, 'old charts')

        # Cleanup empty directories
        self.cleanup_empty_directories(reports_dir)

        # Check for large files
        self.cleanup_large_files(reports_dir, 50)  # Flag files > 50MB

        # Vacuum database
        db_path = Path(self.storage.get('history_db', './benchmark_history.db'))
        if db_path.exists():
            self.vacuum_database(db_path)

        # Final summary
        print("\n" + "=" * 60)
        print("📋 Cleanup Summary")
        print("=" * 60)

        if self.dry_run:
            print("🔍 DRY-RUN MODE - No changes were made")

        print(f"\n📁 Files processed: {self.files_deleted}")
        print(f"💾 Space freed: {self.format_size(self.bytes_freed)}")

        if reports_dir.exists():
            final_size = self.get_directory_size(reports_dir)
            print(f"📊 Current storage: {self.format_size(final_size)}")

            max_size_mb = self.retention.get('max_total_size_mb', 500)
            max_size_bytes = max_size_mb * 1024 * 1024
            usage_percent = (final_size / max_size_bytes) * 100 if max_size_bytes > 0 else 0

            print(f"🎯 Storage limit: {max_size_mb}MB")
            print(f"📈 Usage: {usage_percent:.1f}%")

            if usage_percent > 80:
                print(f"\n⚠️  WARNING: Storage usage is high!")
                print(f"    Consider running with --aggressive flag")

        print()


def main():
    parser = argparse.ArgumentParser(
        description='Clean up old benchmark data to prevent bloat',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive cleanup (preview + confirm)
  python cleanup_benchmark_data.py

  # Automatic cleanup
  python cleanup_benchmark_data.py --auto

  # Preview only (no deletion)
  python cleanup_benchmark_data.py --dry-run

  # Aggressive cleanup (keep fewer files)
  python cleanup_benchmark_data.py --aggressive --auto
"""
    )

    parser.add_argument('--auto', action='store_true',
                       help='Run cleanup automatically without confirmation')
    parser.add_argument('--dry-run', action='store_true',
                       help='Preview cleanup without deleting files')
    parser.add_argument('--aggressive', action='store_true',
                       help='More aggressive cleanup (keep fewer files)')
    parser.add_argument('--config', default='benchmark_config.json',
                       help='Path to configuration file')

    args = parser.parse_args()

    cleaner = BenchmarkDataCleaner(args.config)
    cleaner.dry_run = args.dry_run

    # Run dry-run first if not in auto mode
    if not args.auto and not args.dry_run:
        print("Running preview first...\n")
        preview_cleaner = BenchmarkDataCleaner(args.config)
        preview_cleaner.dry_run = True
        preview_cleaner.run_cleanup(args.aggressive)

        print("\n" + "=" * 60)
        response = input("\nProceed with cleanup? [y/N]: ")
        if response.lower() != 'y':
            print("❌ Cleanup cancelled")
            return
        print()

    # Run actual cleanup
    cleaner.run_cleanup(args.aggressive)

    print("✅ Cleanup complete!")


if __name__ == "__main__":
    main()
