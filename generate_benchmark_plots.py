#!/usr/bin/env python3
"""
Benchmark Results Plot Generator (2025)
Creates comprehensive visualizations from benchmark results
"""

import json
import os
from datetime import datetime

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

# Set style for professional plots
plt.style.use('seaborn-v0_8')
sns.set_palette("husl")

def load_benchmark_results(filename):
    """Load benchmark results from JSON file"""
    with open(filename, 'r') as f:
        return json.load(f)

def create_performance_ranking_plot(results, output_dir):
    """Create performance ranking plot by response time"""
    frameworks = []
    response_times = []
    success_rates = []

    for framework_key, data in results['results'].items():
        if data['summary']['successful_tests'] > 0:
            frameworks.append(data['name'])
            response_times.append(data['summary']['avg_response_time_ms'])
            success_rates.append(data['summary']['overall_success_rate'])

    # Sort by response time (ascending)
    sorted_data = sorted(zip(frameworks, response_times, success_rates),
                        key=lambda x: x[1])
    frameworks, response_times, success_rates = zip(*sorted_data)

    # Create the plot
    fig, ax = plt.subplots(figsize=(14, 10))

    # Create bars with color based on success rate
    colors = ['#2E8B57' if rate >= 80 else '#FFD700' if rate >= 50 else '#DC143C'
              for rate in success_rates]

    bars = ax.barh(frameworks, response_times, color=colors, alpha=0.8)

    # Add value labels on bars
    for i, (bar, time, rate) in enumerate(zip(bars, response_times, success_rates)):
        ax.text(bar.get_width() + max(response_times) * 0.01,
                bar.get_y() + bar.get_height()/2,
                f'{time:.1f}ms\n({rate:.1f}%)',
                va='center', ha='left', fontsize=10, fontweight='bold')

    ax.set_xlabel('Average Response Time (ms)', fontsize=14, fontweight='bold')
    ax.set_title('Framework Performance Ranking by Response Time\n(2025 Benchmark Results)',
                 fontsize=16, fontweight='bold', pad=20)
    ax.grid(axis='x', alpha=0.3)

    # Add legend
    legend_elements = [
        plt.Rectangle((0,0),1,1, facecolor='#2E8B57', alpha=0.8, label='Success Rate ≥80%'),
        plt.Rectangle((0,0),1,1, facecolor='#FFD700', alpha=0.8, label='Success Rate 50-79%'),
        plt.Rectangle((0,0),1,1, facecolor='#DC143C', alpha=0.8, label='Success Rate <50%')
    ]
    ax.legend(handles=legend_elements, loc='lower right', fontsize=12)

    plt.tight_layout()
    plt.savefig(f'{output_dir}/performance_ranking.png', dpi=300, bbox_inches='tight')
    plt.close()

    return 'performance_ranking.png'

def create_success_rate_plot(results, output_dir):
    """Create success rate comparison plot"""
    frameworks = []
    success_rates = []
    response_times = []

    for framework_key, data in results['results'].items():
        frameworks.append(data['name'])
        success_rates.append(data['summary']['overall_success_rate'])
        response_times.append(data['summary']['avg_response_time_ms'])

    # Sort by success rate (descending)
    sorted_data = sorted(zip(frameworks, success_rates, response_times),
                        key=lambda x: x[1], reverse=True)
    frameworks, success_rates, response_times = zip(*sorted_data)

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(20, 8))

    # Success Rate Plot
    colors = ['#2E8B57' if rate >= 80 else '#FFD700' if rate >= 50 else '#DC143C'
              for rate in success_rates]

    bars1 = ax1.bar(frameworks, success_rates, color=colors, alpha=0.8)
    ax1.set_ylabel('Success Rate (%)', fontsize=14, fontweight='bold')
    ax1.set_title('Framework Success Rate Comparison', fontsize=16, fontweight='bold')
    ax1.set_ylim(0, 105)
    ax1.grid(axis='y', alpha=0.3)

    # Add value labels
    for bar, rate in zip(bars1, success_rates):
        height = bar.get_height()
        ax1.text(bar.get_x() + bar.get_width()/2., height + 1,
                f'{rate:.1f}%', ha='center', va='bottom', fontweight='bold')

    # Response Time Plot
    bars2 = ax2.bar(frameworks, response_times, color='#4682B4', alpha=0.8)
    ax2.set_ylabel('Average Response Time (ms)', fontsize=14, fontweight='bold')
    ax2.set_title('Framework Response Time Comparison', fontsize=16, fontweight='bold')
    ax2.grid(axis='y', alpha=0.3)

    # Add value labels
    for bar, time in zip(bars2, response_times):
        height = bar.get_height()
        ax2.text(bar.get_x() + bar.get_width()/2., height + max(response_times) * 0.01,
                f'{time:.1f}ms', ha='center', va='bottom', fontweight='bold')

    # Rotate x-axis labels
    for ax in [ax1, ax2]:
        ax.tick_params(axis='x', rotation=45, labelsize=10)

    plt.tight_layout()
    plt.savefig(f'{output_dir}/success_rate_comparison.png', dpi=300, bbox_inches='tight')
    plt.close()

    return 'success_rate_comparison.png'

def create_scenario_analysis_plot(results, output_dir):
    """Create scenario analysis plot"""
    scenarios = ['SMALL', 'MEDIUM', 'LARGE', 'HUGE']
    scenario_names = ['Small Payload', 'Medium Payload', 'Large Payload', 'Huge Payload']

    # Collect data for each scenario
    scenario_data = {scenario: {'success_rates': [], 'response_times': []}
                    for scenario in scenarios}

    for framework_key, data in results['results'].items():
        for scenario in scenarios:
            if scenario in data['scenarios']:
                scenario_summary = data['scenarios'][scenario]['summary']
                scenario_data[scenario]['success_rates'].append(scenario_summary['success_rate'])
                if scenario_summary['avg_response_time_ms'] > 0:
                    scenario_data[scenario]['response_times'].append(
                        scenario_summary['avg_response_time_ms'])

    # Calculate averages
    avg_success_rates = []
    avg_response_times = []

    for scenario in scenarios:
        success_rates = scenario_data[scenario]['success_rates']
        response_times = scenario_data[scenario]['response_times']

        avg_success_rates.append(np.mean(success_rates) if success_rates else 0)
        avg_response_times.append(np.mean(response_times) if response_times else 0)

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(20, 8))

    # Success Rate by Scenario
    bars1 = ax1.bar(scenario_names, avg_success_rates,
                   color=['#2E8B57', '#4682B4', '#FFD700', '#DC143C'], alpha=0.8)
    ax1.set_ylabel('Average Success Rate (%)', fontsize=14, fontweight='bold')
    ax1.set_title('Success Rate by Payload Size', fontsize=16, fontweight='bold')
    ax1.set_ylim(0, 105)
    ax1.grid(axis='y', alpha=0.3)

    for bar, rate in zip(bars1, avg_success_rates):
        height = bar.get_height()
        ax1.text(bar.get_x() + bar.get_width()/2., height + 1,
                f'{rate:.1f}%', ha='center', va='bottom', fontweight='bold')

    # Response Time by Scenario
    bars2 = ax2.bar(scenario_names, avg_response_times,
                   color=['#2E8B57', '#4682B4', '#FFD700', '#DC143C'], alpha=0.8)
    ax2.set_ylabel('Average Response Time (ms)', fontsize=14, fontweight='bold')
    ax2.set_title('Response Time by Payload Size', fontsize=16, fontweight='bold')
    ax2.grid(axis='y', alpha=0.3)

    for bar, time in zip(bars2, avg_response_times):
        height = bar.get_height()
        ax2.text(bar.get_x() + bar.get_width()/2., height + max(avg_response_times) * 0.01,
                f'{time:.1f}ms', ha='center', va='bottom', fontweight='bold')

    plt.tight_layout()
    plt.savefig(f'{output_dir}/scenario_analysis.png', dpi=300, bbox_inches='tight')
    plt.close()

    return 'scenario_analysis.png'

def create_comprehensive_summary_plot(results, output_dir):
    """Create comprehensive summary plot"""
    frameworks = []
    success_rates = []
    response_times = []
    total_tests = []

    for framework_key, data in results['results'].items():
        frameworks.append(data['name'])
        success_rates.append(data['summary']['overall_success_rate'])
        response_times.append(data['summary']['avg_response_time_ms'])
        total_tests.append(data['summary']['total_tests'])

    # Create bubble chart
    fig, ax = plt.subplots(figsize=(16, 12))

    # Color coding based on success rate
    colors = ['#2E8B57' if rate >= 80 else '#FFD700' if rate >= 50 else '#DC143C'
              for rate in success_rates]

    # Bubble size based on total tests
    sizes = [max(100, test * 20) for test in total_tests]

    scatter = ax.scatter(response_times, success_rates, s=sizes, c=colors,
                        alpha=0.7, edgecolors='black', linewidth=1)

    # Add framework labels
    for i, framework in enumerate(frameworks):
        ax.annotate(framework, (response_times[i], success_rates[i]),
                   xytext=(5, 5), textcoords='offset points',
                   fontsize=10, fontweight='bold')

    ax.set_xlabel('Average Response Time (ms)', fontsize=14, fontweight='bold')
    ax.set_ylabel('Success Rate (%)', fontsize=14, fontweight='bold')
    ax.set_title('Comprehensive Framework Performance Summary\n(2025 Benchmark Results)',
                 fontsize=16, fontweight='bold', pad=20)

    # Add grid
    ax.grid(True, alpha=0.3)

    # Add legend
    legend_elements = [
        plt.scatter([], [], c='#2E8B57', s=100, alpha=0.7, edgecolors='black',
                   label='Success Rate ≥80%'),
        plt.scatter([], [], c='#FFD700', s=100, alpha=0.7, edgecolors='black',
                   label='Success Rate 50-79%'),
        plt.scatter([], [], c='#DC143C', s=100, alpha=0.7, edgecolors='black',
                   label='Success Rate <50%')
    ]
    ax.legend(handles=legend_elements, loc='upper right', fontsize=12)

    plt.tight_layout()
    plt.savefig(f'{output_dir}/comprehensive_summary.png', dpi=300, bbox_inches='tight')
    plt.close()

    return 'comprehensive_summary.png'

def create_performance_heatmap(results, output_dir):
    """Create performance heatmap"""
    frameworks = []
    scenarios = ['SMALL', 'MEDIUM', 'LARGE', 'HUGE']
    scenario_names = ['Small', 'Medium', 'Large', 'Huge']

    # Collect data
    heatmap_data = []

    for framework_key, data in results['results'].items():
        frameworks.append(data['name'])
        row_data = []
        for scenario in scenarios:
            if scenario in data['scenarios']:
                success_rate = data['scenarios'][scenario]['summary']['success_rate']
                row_data.append(success_rate)
            else:
                row_data.append(0)
        heatmap_data.append(row_data)

    # Create heatmap
    fig, ax = plt.subplots(figsize=(12, 10))

    im = ax.imshow(heatmap_data, cmap='RdYlGn', aspect='auto', vmin=0, vmax=100)

    # Add colorbar
    cbar = ax.figure.colorbar(im, ax=ax)
    cbar.ax.set_ylabel('Success Rate (%)', rotation=-90, va="bottom", fontsize=12)

    # Set ticks and labels
    ax.set_xticks(range(len(scenario_names)))
    ax.set_yticks(range(len(frameworks)))
    ax.set_xticklabels(scenario_names, fontsize=12)
    ax.set_yticklabels(frameworks, fontsize=10)

    # Rotate x-axis labels
    plt.setp(ax.get_xticklabels(), rotation=0, ha="center")

    # Add text annotations
    for i in range(len(frameworks)):
        for j in range(len(scenario_names)):
            text = ax.text(j, i, f'{heatmap_data[i][j]:.0f}%',
                          ha="center", va="center", color="black", fontweight='bold')

    ax.set_title('Framework Performance Heatmap by Scenario\n(Success Rate %)',
                 fontsize=16, fontweight='bold', pad=20)
    ax.set_xlabel('Payload Size', fontsize=14, fontweight='bold')
    ax.set_ylabel('Framework', fontsize=14, fontweight='bold')

    plt.tight_layout()
    plt.savefig(f'{output_dir}/performance_heatmap.png', dpi=300, bbox_inches='tight')
    plt.close()

    return 'performance_heatmap.png'

def main():
    """Main function to generate all plots"""
    # Find the latest benchmark results file
    import glob
    result_files = glob.glob('final_comprehensive_benchmark_*.json')
    if not result_files:
        print("❌ No benchmark results file found!")
        return

    latest_file = max(result_files, key=os.path.getctime)
    print(f"📊 Loading results from: {latest_file}")

    # Load results
    results = load_benchmark_results(latest_file)

    # Create output directory
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_dir = f"benchmark_plots_{timestamp}"
    os.makedirs(output_dir, exist_ok=True)

    print(f"📁 Creating plots in: {output_dir}")

    # Generate all plots
    plots = []

    print("📈 Generating performance ranking plot...")
    plots.append(create_performance_ranking_plot(results, output_dir))

    print("📊 Generating success rate comparison plot...")
    plots.append(create_success_rate_plot(results, output_dir))

    print("📋 Generating scenario analysis plot...")
    plots.append(create_scenario_analysis_plot(results, output_dir))

    print("🎯 Generating comprehensive summary plot...")
    plots.append(create_comprehensive_summary_plot(results, output_dir))

    print("🔥 Generating performance heatmap...")
    plots.append(create_performance_heatmap(results, output_dir))

    # Create summary
    print(f"\n✅ All plots generated successfully!")
    print(f"📁 Output directory: {output_dir}")
    print(f"📊 Generated plots:")
    for plot in plots:
        print(f"   - {plot}")

    return output_dir, plots

if __name__ == "__main__":
    main()
