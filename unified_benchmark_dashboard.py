#!/usr/bin/env python3
"""
Unified Java Serialization Framework Benchmark Dashboard 2025
Multi-page dashboard consolidating all analysis views
"""

import json

import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
import streamlit as st

# Page config
st.set_page_config(
    page_title="Java Serialization Framework Benchmark 2025",
    page_icon="🚀",
    layout="wide",
    initial_sidebar_state="expanded"
)

@st.cache_data
def load_benchmark_data():
    """Load benchmark data"""
    try:
        with open('comprehensive_benchmark_data_20250728_211911.json', 'r') as f:
            data = json.load(f)
        return data
    except FileNotFoundError:
        st.error("Benchmark data file not found.")
        return None

def create_performance_dataframe(data):
    """Create DataFrame for performance analysis"""
    rows = []

    for framework_name, framework_data in data['frameworks'].items():
        for payload_size, payload_data in framework_data['payload_sizes'].items():
            row = {
                'Framework': framework_name,
                'Payload_Size': payload_size,
                'Size_MB': payload_data['size_kb'] / 1024,
                'Users_Count': payload_data['users'],
                'Iterations': payload_data['iterations'],
                'Response_Time_ms': payload_data['response_time_ms'],
                'Serialization_Time_ms': payload_data['serialization_time_ms'],
                'Deserialization_Time_ms': payload_data['deserialization_time_ms'],
                'Success_Rate': payload_data['success_rate'],
                'Total_Operations': payload_data['users'] * payload_data['iterations'],
                'Throughput_ops_per_sec': (payload_data['users'] * payload_data['iterations']) / (payload_data['response_time_ms'] / 1000),
                'Serialization_Throughput': (payload_data['users'] * payload_data['iterations']) / (payload_data['serialization_time_ms'] / 1000),
                'Deserialization_Throughput': (payload_data['users'] * payload_data['iterations']) / (payload_data['deserialization_time_ms'] / 1000),
                'Performance_Score': 1000 / payload_data['response_time_ms'] * (payload_data['success_rate'] / 100)
            }
            rows.append(row)

    return pd.DataFrame(rows)

def create_resource_dataframe(data):
    """Create DataFrame for resource analysis"""
    rows = []

    for framework_name, framework_data in data['frameworks'].items():
        for payload_size, payload_data in framework_data['payload_sizes'].items():
            row = {
                'Framework': framework_name,
                'Payload_Size': payload_size,
                'Size_MB': payload_data['size_kb'] / 1024,
                'Response_Time_ms': payload_data['response_time_ms'],
                'Success_Rate': payload_data['success_rate'],
                'Memory_Usage_MB': payload_data['memory_usage_mb'],
                'CPU_Usage_Percent': payload_data['cpu_usage_percent'],
                'Compression_Ratio': payload_data['compression_ratio'],
                'Throughput_ops_per_sec': (payload_data['users'] * payload_data['iterations']) / (payload_data['response_time_ms'] / 1000),
                'Memory_Efficiency': payload_data['size_kb'] / payload_data['memory_usage_mb'],
                'Performance_Score': 1000 / payload_data['response_time_ms'] * (payload_data['success_rate'] / 100),
                'Efficiency_Score': (payload_data['success_rate'] / 100) * (payload_data['size_kb'] / payload_data['memory_usage_mb']) * payload_data['compression_ratio']
            }
            rows.append(row)

    return pd.DataFrame(rows)

def create_decision_dataframe(data):
    """Create DataFrame for decision analysis"""
    rows = []

    for framework_name, framework_data in data['frameworks'].items():
        for payload_size, payload_data in framework_data['payload_sizes'].items():
            row = {
                'Framework': framework_name,
                'Payload_Size': payload_size,
                'Size_MB': payload_data['size_kb'] / 1024,
                'Response_Time_ms': payload_data['response_time_ms'],
                'Success_Rate': payload_data['success_rate'],
                'Memory_Usage_MB': payload_data['memory_usage_mb'],
                'CPU_Usage_Percent': payload_data['cpu_usage_percent'],
                'Compression_Ratio': payload_data['compression_ratio'],
                'Throughput_ops_per_sec': (payload_data['users'] * payload_data['iterations']) / (payload_data['response_time_ms'] / 1000),
                'Memory_Efficiency': payload_data['size_kb'] / payload_data['memory_usage_mb'],
                'Performance_Score': 1000 / payload_data['response_time_ms'] * (payload_data['success_rate'] / 100),
                'Efficiency_Score': (payload_data['success_rate'] / 100) * (payload_data['size_kb'] / payload_data['memory_usage_mb']) * payload_data['compression_ratio']
            }
            rows.append(row)

    return pd.DataFrame(rows)

# Load data
data = load_benchmark_data()
if data is None:
    st.stop()

# Create dataframes
performance_df = create_performance_dataframe(data)
resource_df = create_resource_dataframe(data)
decision_df = create_decision_dataframe(data)

# Sidebar navigation
st.sidebar.title("🚀 Java Serialization Benchmark 2025")
st.sidebar.markdown("---")

# Page selection
page = st.sidebar.selectbox(
    "Select Dashboard:",
    [
        "🏠 Home & Overview",
        "🎯 Decision Dashboard",
        "🚀 Performance Analysis",
        "💾 Resource Analysis",
        "🏗️ Infrastructure Analysis",
        "📊 Comprehensive Analysis",
        "🔍 Detailed Metrics"
    ]
)

# Global filters
st.sidebar.markdown("---")
st.sidebar.markdown("### 🔍 Global Filters")

frameworks = sorted(performance_df['Framework'].unique())
selected_frameworks = st.sidebar.multiselect(
    "Select Frameworks:",
    frameworks,
    default=frameworks
)

payload_sizes = sorted(performance_df['Payload_Size'].unique())
selected_payload_sizes = st.sidebar.multiselect(
    "Select Payload Sizes:",
    payload_sizes,
    default=payload_sizes
)

# Filter data based on selections
filtered_performance = performance_df[
    (performance_df['Framework'].isin(selected_frameworks)) &
    (performance_df['Payload_Size'].isin(selected_payload_sizes))
]

filtered_resource = resource_df[
    (resource_df['Framework'].isin(selected_frameworks)) &
    (resource_df['Payload_Size'].isin(selected_payload_sizes))
]

filtered_decision = decision_df[
    (decision_df['Framework'].isin(selected_frameworks)) &
    (decision_df['Payload_Size'].isin(selected_payload_sizes))
]

def show_home_page(perf_df, res_df):
    """Home page with overview"""
    st.title("🏠 Java Serialization Framework Benchmark 2025")
    st.markdown("**Comprehensive Analysis of 10 Serialization Frameworks**")

    # Key metrics
    col1, col2, col3, col4 = st.columns(4)

    with col1:
        fastest_framework = perf_df.loc[perf_df['Response_Time_ms'].idxmin(), 'Framework']
        fastest_time = perf_df['Response_Time_ms'].min()
        st.metric("🏆 Fastest Framework", fastest_framework, f"{fastest_time:.2f}ms")

    with col2:
        most_reliable = res_df.loc[res_df['Success_Rate'].idxmax(), 'Framework']
        max_success = res_df['Success_Rate'].max()
        st.metric("🛡️ Most Reliable", most_reliable, f"{max_success:.1f}%")

    with col3:
        most_efficient = res_df.loc[res_df['Memory_Efficiency'].idxmax(), 'Framework']
        max_efficiency = res_df['Memory_Efficiency'].max()
        st.metric("💡 Most Efficient", most_efficient, f"{max_efficiency:.2f} KB/MB")

    with col4:
        total_tests = len(perf_df)
        st.metric("🧪 Total Tests", f"{total_tests:,}")

    # Framework overview
    st.header("📊 Framework Overview")

    col1, col2 = st.columns(2)

    with col1:
        st.markdown("### 🏆 Performance Rankings")
        performance_ranking = perf_df.groupby('Framework')['Response_Time_ms'].mean().sort_values()
        fig_perf = px.bar(
            performance_ranking.reset_index(),
            x='Framework',
            y='Response_Time_ms',
            title="Average Response Time by Framework",
            color='Response_Time_ms',
            color_continuous_scale='RdYlGn_r',
            template='plotly_white'
        )
        fig_perf.update_layout(xaxis_tickangle=-45)
        st.plotly_chart(fig_perf, use_container_width=True)

    with col2:
        st.markdown("### 🛡️ Reliability Rankings")
        reliability_ranking = res_df.groupby('Framework')['Success_Rate'].mean().sort_values(ascending=False)
        fig_rel = px.bar(
            reliability_ranking.reset_index(),
            x='Framework',
            y='Success_Rate',
            title="Average Success Rate by Framework",
            color='Success_Rate',
            color_continuous_scale='RdYlGn',
            template='plotly_white'
        )
        fig_rel.update_layout(xaxis_tickangle=-45)
        st.plotly_chart(fig_rel, use_container_width=True)

    # Quick insights
    st.header("💡 Quick Insights")

    col1, col2, col3 = st.columns(3)

    with col1:
        st.markdown("### 🚀 Performance Champions")
        top_perf = performance_ranking.head(3)
        for framework, time in top_perf.items():
            st.markdown(f"• **{framework}**: {time:.2f}ms")

    with col2:
        st.markdown("### 🛡️ Reliability Leaders")
        top_rel = reliability_ranking.head(3)
        for framework, rate in top_rel.items():
            st.markdown(f"• **{framework}**: {rate:.1f}%")

    with col3:
        st.markdown("### 💡 Efficiency Winners")
        efficiency_ranking = res_df.groupby('Framework')['Memory_Efficiency'].mean().sort_values(ascending=False)
        top_eff = efficiency_ranking.head(3)
        for framework, efficiency in top_eff.items():
            st.markdown(f"• **{framework}**: {efficiency:.2f} KB/MB")

def show_decision_page(df):
    """Decision dashboard page"""
    st.title("🎯 Decision Dashboard")
    st.markdown("**Final Recommendations and Trade-offs for Choosing the Right Framework**")

    # Decision criteria
    st.sidebar.markdown("### 🎯 Decision Criteria")

    performance_weight = st.sidebar.slider(
        "Performance Importance", 0, 100, 30
    )

    reliability_weight = st.sidebar.slider(
        "Reliability Importance", 0, 100, 25
    )

    efficiency_weight = st.sidebar.slider(
        "Resource Efficiency", 0, 100, 20
    )

    # Resource constraints
    st.sidebar.markdown("### 💻 Resource Constraints")

    available_memory_mb = st.sidebar.slider(
        "Available Memory (MB):", 100, 2000, 512
    )

    available_cpu_percent = st.sidebar.slider(
        "Available CPU (%):", 10, 100, 50
    )

    # Use case selection
    use_case = st.sidebar.selectbox(
        "Select Your Use Case:",
        [
            "High-Performance Systems",
            "Enterprise Applications",
            "Resource-Constrained Systems",
            "Microservices",
            "Big Data Processing",
            "Real-time Applications",
            "IoT/Edge Computing",
            "General Purpose"
        ]
    )

    # Calculate decision scores
    frameworks = df['Framework'].unique()
    decision_scores = []

    for framework in frameworks:
        framework_data = df[df['Framework'] == framework]

        # Performance score
        avg_response_time = framework_data['Response_Time_ms'].mean()
        avg_throughput = framework_data['Throughput_ops_per_sec'].mean()
        performance_score = (1000 / avg_response_time) * (avg_throughput / 1000)

        # Reliability score
        avg_success_rate = framework_data['Success_Rate'].mean()
        reliability_score = avg_success_rate / 100

        # Efficiency score
        avg_memory_efficiency = framework_data['Memory_Efficiency'].mean()
        avg_compression = framework_data['Compression_Ratio'].mean()
        efficiency_score = (avg_memory_efficiency / 10) * avg_compression

        # Resource constraint check
        max_memory = framework_data['Memory_Usage_MB'].max()
        max_cpu = framework_data['CPU_Usage_Percent'].max()

        memory_fits = max_memory <= available_memory_mb
        cpu_fits = max_cpu <= available_cpu_percent

        resource_penalty = 0
        if not memory_fits:
            resource_penalty += 0.5
        if not cpu_fits:
            resource_penalty += 0.3

        # Weighted overall score
        total_weight = performance_weight + reliability_weight + efficiency_weight
        if total_weight > 0:
            overall_score = (
                (performance_score * performance_weight) +
                (reliability_score * reliability_weight) +
                (efficiency_score * efficiency_weight)
            ) / total_weight
        else:
            overall_score = 0

        overall_score = overall_score * (1 - resource_penalty)

        decision_scores.append({
            'Framework': framework,
            'Performance_Score': round(performance_score, 4),
            'Reliability_Score': round(reliability_score, 4),
            'Efficiency_Score': round(efficiency_score, 4),
            'Overall_Score': round(overall_score, 4),
            'Avg_Response_Time_ms': round(avg_response_time, 2),
            'Avg_Success_Rate': round(avg_success_rate, 1),
            'Max_Memory_MB': round(max_memory, 2),
            'Max_CPU_Percent': round(max_cpu, 2),
            'Fits_Constraints': memory_fits and cpu_fits,
            'Resource_Penalty': round(resource_penalty, 2)
        })

    decision_df = pd.DataFrame(decision_scores)
    decision_df = decision_df.sort_values('Overall_Score', ascending=False)

    # Display results
    col1, col2 = st.columns(2)

    with col1:
        st.markdown("### 📊 Overall Rankings")
        st.dataframe(decision_df, use_container_width=True)

    with col2:
        st.markdown("### 🏆 Top Framework Visualization")
        fig_rankings = px.bar(
            decision_df.head(5),
            x='Framework',
            y='Overall_Score',
            title="Top 5 Frameworks by Overall Score",
            color='Overall_Score',
            color_continuous_scale='RdYlGn',
            template='plotly_white'
        )
        fig_rankings.update_layout(xaxis_tickangle=-45)
        st.plotly_chart(fig_rankings, use_container_width=True)

    # Multi-dimensional comparison
    st.header("📊 Multi-Dimensional Comparison")

    # Create radar chart data
    radar_data = []
    for _, row in decision_df.iterrows():
        radar_data.extend([
            dict(Framework=row['Framework'], Metric="Performance", Score=row['Performance_Score']),
            dict(Framework=row['Framework'], Metric="Reliability", Score=row['Reliability_Score']),
            dict(Framework=row['Framework'], Metric="Efficiency", Score=row['Efficiency_Score'])
        ])

    radar_df = pd.DataFrame(radar_data)

    # Create radar chart
    fig_radar = px.line_polar(
        radar_df,
        r='Score',
        theta='Metric',
        color='Framework',
        line_close=True,
        title="Multi-Dimensional Framework Comparison",
        template='plotly_white',
        color_discrete_sequence=px.colors.qualitative.Set3
    )

    fig_radar.update_layout(
        polar=dict(
            radialaxis=dict(
                visible=True,
                range=[0, 1]
            )
        ),
        showlegend=True,
        legend=dict(
            orientation="h",
            yanchor="bottom",
            y=1.02,
            xanchor="right",
            x=1
        )
    )

    st.plotly_chart(fig_radar, use_container_width=True)

    # Decision Matrix
    st.header("📊 Decision Matrix")

    # Create decision matrix with all frameworks
    decision_matrix = decision_df[['Framework', 'Performance_Score', 'Reliability_Score', 'Efficiency_Score', 'Overall_Score', 'Fits_Constraints']].copy()
    decision_matrix['Recommendation'] = decision_matrix['Overall_Score'].rank(ascending=False).astype(int)

    # Color code the matrix
    def color_decision_matrix(val):
        if isinstance(val, bool):
            return 'background-color: lightgreen' if val else 'background-color: lightcoral'
        elif isinstance(val, str) and 'Score' in val:
            return 'background-color: lightblue'
        else:
            return ''

    st.dataframe(decision_matrix.style.applymap(color_decision_matrix), use_container_width=True)

    # Recommendations
    st.header("💡 Final Recommendations")

    feasible_frameworks = decision_df[decision_df['Fits_Constraints'] == True]
    if not feasible_frameworks.empty:
        top_framework = feasible_frameworks.iloc[0]
        st.success(f"**Best Overall Choice:** {top_framework['Framework']}")
        st.markdown(f"• Overall Score: {top_framework['Overall_Score']:.4f}")
        st.markdown(f"• Response Time: {top_framework['Avg_Response_Time_ms']}ms")
        st.markdown(f"• Success Rate: {top_framework['Avg_Success_Rate']}%")
        st.markdown(f"• Memory Usage: {top_framework['Max_Memory_MB']:.1f}MB")
        st.markdown(f"• CPU Usage: {top_framework['Max_CPU_Percent']:.1f}%")
    else:
        st.warning("⚠️ No frameworks fit within your resource constraints")
        st.markdown("**Consider these alternatives:**")

        # Show top 3 frameworks regardless of constraints
        top_alternatives = decision_df.head(3)
        for _, row in top_alternatives.iterrows():
            st.markdown(f"• **{row['Framework']}**: Score {row['Overall_Score']:.4f} (Memory: {row['Max_Memory_MB']:.1f}MB, CPU: {row['Max_CPU_Percent']:.1f}%)")

        st.markdown("**Recommendations:**")
        st.markdown("• Increase available memory or CPU")
        st.markdown("• Consider smaller payload sizes")
        st.markdown("• Use resource-optimized frameworks")

def show_performance_page(df):
    """Performance analysis page"""
    st.title("🚀 Performance Analysis")
    st.markdown("**Deep analysis of speed, scaling, and throughput**")

    # Key performance metrics
    col1, col2, col3, col4 = st.columns(4)

    with col1:
        fastest_framework = df.loc[df['Response_Time_ms'].idxmin(), 'Framework']
        fastest_time = df['Response_Time_ms'].min()
        st.metric("🏆 Fastest Framework", fastest_framework, f"{fastest_time:.2f}ms")

    with col2:
        highest_throughput = df['Throughput_ops_per_sec'].max()
        throughput_framework = df.loc[df['Throughput_ops_per_sec'].idxmax(), 'Framework']
        st.metric("📈 Highest Throughput", f"{highest_throughput:,.0f} ops/sec", throughput_framework)

    with col3:
        avg_success = df['Success_Rate'].mean()
        st.metric("🎯 Avg Success Rate", f"{avg_success:.1f}%")

    with col4:
        total_operations = df['Total_Operations'].sum()
        st.metric("⚡ Total Operations", f"{total_operations:,}")

    # Performance rankings
    st.header("🏆 Performance Rankings")

    col1, col2 = st.columns(2)

    with col1:
        st.markdown("### 📊 Performance by Framework & Payload Size")
        performance_ranking = df.groupby(['Framework', 'Payload_Size'])['Response_Time_ms'].mean().reset_index()
        fig_ranking = px.bar(
            performance_ranking,
            x='Framework',
            y='Response_Time_ms',
            color='Payload_Size',
            title="Response Time Performance by Framework and Payload Size",
            template='plotly_white'
        )
        fig_ranking.update_layout(xaxis_tickangle=-45)
        st.plotly_chart(fig_ranking, use_container_width=True)

    with col2:
        st.markdown("### 📈 Performance Scaling")
        fig_scaling = px.line(
            df.groupby(['Framework', 'Payload_Size'])['Response_Time_ms'].mean().reset_index(),
            x='Payload_Size',
            y='Response_Time_ms',
            color='Framework',
            title="Performance Scaling by Payload Size",
            template='plotly_white'
        )
        st.plotly_chart(fig_scaling, use_container_width=True)

    # Throughput analysis
    st.header("📊 Throughput Analysis")

    col1, col2 = st.columns(2)

    with col1:
        st.markdown("### 🚀 Throughput by Framework")
        fig_throughput = px.bar(
            df.groupby('Framework')['Throughput_ops_per_sec'].mean().reset_index(),
            x='Framework',
            y='Throughput_ops_per_sec',
            title="Average Throughput by Framework",
            color='Throughput_ops_per_sec',
            color_continuous_scale='RdYlGn',
            template='plotly_white'
        )
        fig_throughput.update_layout(xaxis_tickangle=-45)
        st.plotly_chart(fig_throughput, use_container_width=True)

    with col2:
        st.markdown("### 📤 Serialization vs Deserialization")
        fig_serial = px.scatter(
            df,
            x='Size_MB',
            y='Serialization_Time_ms',
            color='Framework',
            size='Success_Rate',
            hover_data=['Users_Count', 'Iterations'],
            title="Serialization Time vs Payload Size",
            template='plotly_white'
        )
        st.plotly_chart(fig_serial, use_container_width=True)

def show_resource_page(df):
    """Resource analysis page"""
    st.title("💾 Resource Analysis")
    st.markdown("**Memory, CPU, and infrastructure requirements analysis**")

    # Resource constraints
    st.sidebar.markdown("### 💻 Resource Constraints")

    available_memory_mb = st.sidebar.slider(
        "Available Memory (MB):", 100, 2000, 512
    )

    available_cpu_percent = st.sidebar.slider(
        "Available CPU (%):", 10, 100, 50
    )

    # Key resource metrics
    col1, col2, col3, col4 = st.columns(4)

    with col1:
        most_memory_efficient = df.loc[df['Memory_Efficiency'].idxmax(), 'Framework']
        max_efficiency = df['Memory_Efficiency'].max()
        st.metric("💾 Most Memory Efficient", most_memory_efficient, f"{max_efficiency:.2f} KB/MB")

    with col2:
        lowest_memory_usage = df.loc[df['Memory_Usage_MB'].idxmin(), 'Framework']
        min_memory = df['Memory_Usage_MB'].min()
        st.metric("📉 Lowest Memory Usage", lowest_memory_usage, f"{min_memory:.2f} MB")

    with col3:
        lowest_cpu_usage = df.loc[df['CPU_Usage_Percent'].idxmin(), 'Framework']
        min_cpu = df['CPU_Usage_Percent'].min()
        st.metric("🔋 Lowest CPU Usage", lowest_cpu_usage, f"{min_cpu:.1f}%")

    with col4:
        best_compression = df.loc[df['Compression_Ratio'].idxmax(), 'Framework']
        max_compression = df['Compression_Ratio'].max()
        st.metric("🗜️ Best Compression", best_compression, f"{max_compression:.3f}")

    # Memory analysis
    st.header("💾 Memory Analysis")

    col1, col2 = st.columns(2)

    with col1:
        st.markdown("### 📊 Memory Usage by Framework")
        fig_memory = px.bar(
            df.groupby(['Framework', 'Payload_Size'])['Memory_Usage_MB'].mean().reset_index(),
            x='Framework',
            y='Memory_Usage_MB',
            color='Payload_Size',
            title="Memory Usage by Framework and Payload Size",
            template='plotly_white'
        )
        fig_memory.update_layout(xaxis_tickangle=-45)
        st.plotly_chart(fig_memory, use_container_width=True)

    with col2:
        st.markdown("### 📈 Memory Scaling")
        fig_memory_scaling = px.line(
            df.groupby(['Framework', 'Payload_Size'])['Memory_Usage_MB'].mean().reset_index(),
            x='Payload_Size',
            y='Memory_Usage_MB',
            color='Framework',
            title="Memory Usage Scaling by Payload Size",
            template='plotly_white'
        )
        st.plotly_chart(fig_memory_scaling, use_container_width=True)

    # CPU analysis
    st.header("⚡ CPU Analysis")

    col1, col2 = st.columns(2)

    with col1:
        st.markdown("### 📊 CPU Usage by Framework")
        fig_cpu = px.bar(
            df.groupby(['Framework', 'Payload_Size'])['CPU_Usage_Percent'].mean().reset_index(),
            x='Framework',
            y='CPU_Usage_Percent',
            color='Payload_Size',
            title="CPU Usage by Framework and Payload Size",
            template='plotly_white'
        )
        fig_cpu.update_layout(xaxis_tickangle=-45)
        st.plotly_chart(fig_cpu, use_container_width=True)

    with col2:
        st.markdown("### 📈 CPU Scaling")
        fig_cpu_scaling = px.line(
            df.groupby(['Framework', 'Payload_Size'])['CPU_Usage_Percent'].mean().reset_index(),
            x='Payload_Size',
            y='CPU_Usage_Percent',
            color='Framework',
            title="CPU Usage Scaling by Payload Size",
            template='plotly_white'
        )
        st.plotly_chart(fig_cpu_scaling, use_container_width=True)

    # Resource constraint analysis
    st.header("⚠️ Resource Constraint Analysis")

    constraint_analysis = []
    for framework in df['Framework'].unique():
        framework_data = df[df['Framework'] == framework]
        max_memory = framework_data['Memory_Usage_MB'].max()
        max_cpu = framework_data['CPU_Usage_Percent'].max()

        memory_fits = max_memory <= available_memory_mb
        cpu_fits = max_cpu <= available_cpu_percent

        constraint_analysis.append({
            'Framework': framework,
            'Max_Memory_MB': max_memory,
            'Max_CPU_Percent': max_cpu,
            'Memory_Fits': memory_fits,
            'CPU_Fits': cpu_fits,
            'Fits_Constraints': memory_fits and cpu_fits
        })

    constraint_df = pd.DataFrame(constraint_analysis)
    constraint_df = constraint_df.sort_values('Fits_Constraints', ascending=False)

    col1, col2 = st.columns(2)

    with col1:
        st.markdown("### 📋 Frameworks Within Constraints")
        st.dataframe(constraint_df, use_container_width=True)

    with col2:
        st.markdown("### ✅ Constraint Compliance")
        fits_constraints = constraint_df[constraint_df['Fits_Constraints'] == True]
        if not fits_constraints.empty:
            st.success(f"✅ {len(fits_constraints)} frameworks fit within constraints")
            for _, row in fits_constraints.iterrows():
                st.markdown(f"• **{row['Framework']}**: {row['Max_Memory_MB']:.1f}MB RAM, {row['Max_CPU_Percent']:.1f}% CPU")
        else:
            st.warning("⚠️ No frameworks fit within the specified constraints")
            st.markdown("**Resource Analysis:**")

            # Show resource usage breakdown
            memory_violations = constraint_df[constraint_df['Memory_Fits'] == False]
            cpu_violations = constraint_df[constraint_df['CPU_Fits'] == False]

            if not memory_violations.empty:
                st.markdown(f"• **Memory Issues**: {len(memory_violations)} frameworks exceed {available_memory_mb}MB")
                worst_memory = memory_violations.loc[memory_violations['Max_Memory_MB'].idxmax()]
                st.markdown(f"  - Highest: {worst_memory['Framework']} ({worst_memory['Max_Memory_MB']:.1f}MB)")

            if not cpu_violations.empty:
                st.markdown(f"• **CPU Issues**: {len(cpu_violations)} frameworks exceed {available_cpu_percent}%")
                worst_cpu = cpu_violations.loc[cpu_violations['Max_CPU_Percent'].idxmax()]
                st.markdown(f"  - Highest: {worst_cpu['Framework']} ({worst_cpu['Max_CPU_Percent']:.1f}%)")

def show_infrastructure_page(perf_df, res_df):
    """Infrastructure analysis page"""
    st.title("🏗️ Infrastructure Analysis")
    st.markdown("**HTTP, SSL, compression, and network performance analysis**")

    # Mock infrastructure data (in real scenario, this would come from actual tests)
    infrastructure_data = []

    for framework in perf_df['Framework'].unique():
        # Mock infrastructure configurations
        infra_configs = {
            'Kryo': {'http': 'HTTP/2', 'ssl': 'TLS 1.3', 'compression': 'GZIP'},
            'Cap\'n Proto': {'http': 'HTTP/2', 'ssl': 'TLS 1.3', 'compression': 'None'},
            'FlatBuffers': {'http': 'HTTP/2', 'ssl': 'TLS 1.3', 'compression': 'None'},
            'Apache Avro': {'http': 'HTTP/1.1', 'ssl': 'TLS 1.2', 'compression': 'Snappy'},
            'gRPC': {'http': 'HTTP/2', 'ssl': 'TLS 1.3', 'compression': 'GZIP'},
            'Protocol Buffers': {'http': 'HTTP/1.1', 'ssl': 'TLS 1.2', 'compression': 'GZIP'},
            'MessagePack': {'http': 'HTTP/1.1', 'ssl': 'None', 'compression': 'LZ4'},
            'Jackson JSON': {'http': 'HTTP/1.1', 'ssl': 'TLS 1.2', 'compression': 'GZIP'},
            'FST': {'http': 'HTTP/2', 'ssl': 'TLS 1.3', 'compression': 'Zstandard'},
            'Apache Thrift': {'http': 'HTTP/1.1', 'ssl': 'TLS 1.2', 'compression': 'GZIP'}
        }

        config = infra_configs.get(framework, {'http': 'HTTP/1.1', 'ssl': 'None', 'compression': 'None'})

        framework_perf = perf_df[perf_df['Framework'] == framework]
        framework_res = res_df[res_df['Framework'] == framework]

        infrastructure_data.append({
            'Framework': framework,
            'HTTP_Version': config['http'],
            'SSL_Protocol': config['ssl'],
            'Compression_Type': config['compression'],
            'Avg_Response_Time_ms': framework_perf['Response_Time_ms'].mean(),
            'Avg_Success_Rate': framework_res['Success_Rate'].mean(),
            'Avg_Memory_MB': framework_res['Memory_Usage_MB'].mean(),
            'Avg_CPU_Percent': framework_res['CPU_Usage_Percent'].mean()
        })

    infra_df = pd.DataFrame(infrastructure_data)

    # Infrastructure overview
    st.header("🌐 Infrastructure Overview")

    col1, col2, col3 = st.columns(3)

    with col1:
        st.markdown("### HTTP Version Distribution")
        http_counts = infra_df['HTTP_Version'].value_counts()
        fig_http = px.pie(
            values=http_counts.values,
            names=http_counts.index,
            title="HTTP Version Distribution",
            template='plotly_white'
        )
        st.plotly_chart(fig_http, use_container_width=True)

    with col2:
        st.markdown("### SSL Protocol Distribution")
        ssl_counts = infra_df['SSL_Protocol'].value_counts()
        fig_ssl = px.pie(
            values=ssl_counts.values,
            names=ssl_counts.index,
            title="SSL Protocol Distribution",
            template='plotly_white'
        )
        st.plotly_chart(fig_ssl, use_container_width=True)

    with col3:
        st.markdown("### Compression Type Distribution")
        comp_counts = infra_df['Compression_Type'].value_counts()
        fig_comp = px.pie(
            values=comp_counts.values,
            names=comp_counts.index,
            title="Compression Type Distribution",
            template='plotly_white'
        )
        st.plotly_chart(fig_comp, use_container_width=True)

    # Performance by infrastructure
    st.header("📊 Performance by Infrastructure")

    col1, col2 = st.columns(2)

    with col1:
        st.markdown("### Response Time by HTTP Version")
        fig_http_perf = px.box(
            infra_df,
            x='HTTP_Version',
            y='Avg_Response_Time_ms',
            title="Response Time by HTTP Version",
            template='plotly_white'
        )
        st.plotly_chart(fig_http_perf, use_container_width=True)

    with col2:
        st.markdown("### Response Time by Compression Type")
        fig_comp_perf = px.box(
            infra_df,
            x='Compression_Type',
            y='Avg_Response_Time_ms',
            title="Response Time by Compression Type",
            template='plotly_white'
        )
        fig_comp_perf.update_layout(xaxis_tickangle=-45)
        st.plotly_chart(fig_comp_perf, use_container_width=True)

    # Infrastructure matrix
    st.header("📋 Infrastructure Configuration Matrix")
    st.dataframe(infra_df, use_container_width=True)

def show_comprehensive_page(perf_df, res_df):
    """Comprehensive analysis page"""
    st.title("📊 Comprehensive Analysis")
    st.markdown("**Complete overview of all metrics and comparisons**")

    # Overall rankings
    st.header("🏆 Overall Framework Rankings")

    # Calculate overall scores
    frameworks = perf_df['Framework'].unique()
    overall_scores = []

    for framework in frameworks:
        perf_data = perf_df[perf_df['Framework'] == framework]
        res_data = res_df[res_df['Framework'] == framework]

        # Performance score (normalized)
        avg_response_time = perf_data['Response_Time_ms'].mean()
        avg_throughput = perf_data['Throughput_ops_per_sec'].mean()
        performance_score = (1000 / avg_response_time) * (avg_throughput / 1000)

        # Reliability score
        avg_success_rate = res_data['Success_Rate'].mean()
        reliability_score = avg_success_rate / 100

        # Efficiency score
        avg_memory_efficiency = res_data['Memory_Efficiency'].mean()
        avg_compression = res_data['Compression_Ratio'].mean()
        efficiency_score = (avg_memory_efficiency / 10) * avg_compression

        # Overall score (weighted average)
        overall_score = (performance_score * 0.4 + reliability_score * 0.35 + efficiency_score * 0.25)

        overall_scores.append({
            'Framework': framework,
            'Performance_Score': round(performance_score, 4),
            'Reliability_Score': round(reliability_score, 4),
            'Efficiency_Score': round(efficiency_score, 4),
            'Overall_Score': round(overall_score, 4),
            'Avg_Response_Time_ms': round(avg_response_time, 2),
            'Avg_Success_Rate': round(avg_success_rate, 1),
            'Avg_Memory_MB': round(res_data['Memory_Usage_MB'].mean(), 2),
            'Avg_CPU_Percent': round(res_data['CPU_Usage_Percent'].mean(), 1)
        })

    overall_df = pd.DataFrame(overall_scores)
    overall_df = overall_df.sort_values('Overall_Score', ascending=False)

    # Overall rankings table - full width
    st.markdown("### 📊 Overall Framework Rankings")
    st.dataframe(overall_df, use_container_width=True)

    # Top frameworks visualization
    st.markdown("### 🏆 Top Frameworks Visualization")
    fig_overall = px.bar(
        overall_df.head(5),
        x='Framework',
        y='Overall_Score',
        title="Top 5 Frameworks by Overall Score",
        color='Overall_Score',
        color_continuous_scale='RdYlGn',
        template='plotly_white'
    )
    fig_overall.update_layout(xaxis_tickangle=-45)
    st.plotly_chart(fig_overall, use_container_width=True)

    # Correlation analysis
    st.header("🔍 Correlation Analysis")

    # Create correlation matrix
    correlation_data = overall_df[['Performance_Score', 'Reliability_Score', 'Efficiency_Score', 'Overall_Score']]
    correlation_matrix = correlation_data.corr()

    fig_corr = px.imshow(
        correlation_matrix,
        title="Metric Correlation Matrix",
        color_continuous_scale='RdYlBu',
        aspect='auto',
        template='plotly_white'
    )
    st.plotly_chart(fig_corr, use_container_width=True)

    # Key insights
    st.header("💡 Key Insights")

    col1, col2, col3 = st.columns(3)

    with col1:
        st.markdown("### 🚀 Performance Leaders")
        top_perf = overall_df.nlargest(3, 'Performance_Score')
        for _, row in top_perf.iterrows():
            st.markdown(f"• **{row['Framework']}**: {row['Performance_Score']:.4f}")

    with col2:
        st.markdown("### 🛡️ Reliability Leaders")
        top_rel = overall_df.nlargest(3, 'Reliability_Score')
        for _, row in top_rel.iterrows():
            st.markdown(f"• **{row['Framework']}**: {row['Reliability_Score']:.4f}")

    with col3:
        st.markdown("### 💡 Efficiency Leaders")
        top_eff = overall_df.nlargest(3, 'Efficiency_Score')
        for _, row in top_eff.iterrows():
            st.markdown(f"• **{row['Framework']}**: {row['Efficiency_Score']:.4f}")

def show_detailed_page(perf_df, res_df):
    """Detailed metrics page"""
    st.title("🔍 Detailed Metrics")
    st.markdown("**Complete dataset and detailed analysis**")

    # Complete datasets
    st.header("📊 Complete Datasets")

    tab1, tab2, tab3 = st.tabs(["Performance Data", "Resource Data", "Combined Analysis"])

    with tab1:
        st.markdown("### Performance Metrics by Payload Size")

        # Group by Framework and Payload Size
        perf_by_size = perf_df.groupby(['Framework', 'Payload_Size']).agg({
            'Response_Time_ms': 'mean',
            'Throughput_ops_per_sec': 'mean',
            'Success_Rate': 'mean',
            'Serialization_Time_ms': 'mean',
            'Deserialization_Time_ms': 'mean'
        }).round(2).reset_index()

        st.dataframe(perf_by_size, use_container_width=True)

        # Performance statistics by framework
        st.markdown("### Performance Statistics by Framework")
        perf_stats = perf_df.groupby('Framework').agg({
            'Response_Time_ms': ['mean', 'std', 'min', 'max'],
            'Throughput_ops_per_sec': ['mean', 'std', 'min', 'max'],
            'Success_Rate': ['mean', 'std', 'min', 'max']
        }).round(2)
        st.dataframe(perf_stats, use_container_width=True)

        # Strategy breakdown (HTTP/SSL)
        st.markdown("### Strategy Analysis (HTTP/SSL)")

        # Mock strategy data (in real scenario, this would come from actual tests)
        strategy_data = []
        for framework in perf_df['Framework'].unique():
            strategy_configs = {
                'Kryo': {'http': 'HTTP/2', 'ssl': 'TLS 1.3'},
                'Cap\'n Proto': {'http': 'HTTP/2', 'ssl': 'TLS 1.3'},
                'FlatBuffers': {'http': 'HTTP/2', 'ssl': 'TLS 1.3'},
                'Apache Avro': {'http': 'HTTP/1.1', 'ssl': 'TLS 1.2'},
                'gRPC': {'http': 'HTTP/2', 'ssl': 'TLS 1.3'},
                'Protocol Buffers': {'http': 'HTTP/1.1', 'ssl': 'TLS 1.2'},
                'MessagePack': {'http': 'HTTP/1.1', 'ssl': 'None'},
                'Jackson JSON': {'http': 'HTTP/1.1', 'ssl': 'TLS 1.2'},
                'FST': {'http': 'HTTP/2', 'ssl': 'TLS 1.3'},
                'Apache Thrift': {'http': 'HTTP/1.1', 'ssl': 'TLS 1.2'}
            }

            config = strategy_configs.get(framework, {'http': 'HTTP/1.1', 'ssl': 'None'})
            framework_perf = perf_df[perf_df['Framework'] == framework]

            strategy_data.append({
                'Framework': framework,
                'HTTP_Version': config['http'],
                'SSL_Protocol': config['ssl'],
                'Avg_Response_Time_ms': framework_perf['Response_Time_ms'].mean(),
                'Avg_Throughput_ops_per_sec': framework_perf['Throughput_ops_per_sec'].mean(),
                'Avg_Success_Rate': framework_perf['Success_Rate'].mean()
            })

        strategy_df = pd.DataFrame(strategy_data)
        st.dataframe(strategy_df.round(2), use_container_width=True)

    with tab2:
        st.markdown("### Resource Metrics")
        st.dataframe(res_df, use_container_width=True)

        # Resource statistics
        st.markdown("### Resource Statistics")
        res_stats = res_df.groupby('Framework').agg({
            'Memory_Usage_MB': ['mean', 'std', 'min', 'max'],
            'CPU_Usage_Percent': ['mean', 'std', 'min', 'max'],
            'Compression_Ratio': ['mean', 'std', 'min', 'max']
        }).round(2)
        st.dataframe(res_stats, use_container_width=True)

    with tab3:
        # Combined analysis
        st.markdown("### Combined Framework Analysis")

        combined_data = []
        for framework in perf_df['Framework'].unique():
            perf_data = perf_df[perf_df['Framework'] == framework]
            res_data = res_df[res_df['Framework'] == framework]

            combined_data.append({
                'Framework': framework,
                'Avg_Response_Time_ms': perf_data['Response_Time_ms'].mean(),
                'Avg_Throughput_ops_per_sec': perf_data['Throughput_ops_per_sec'].mean(),
                'Avg_Success_Rate': res_data['Success_Rate'].mean(),
                'Avg_Memory_MB': res_data['Memory_Usage_MB'].mean(),
                'Avg_CPU_Percent': res_data['CPU_Usage_Percent'].mean(),
                'Avg_Compression_Ratio': res_data['Compression_Ratio'].mean(),
                'Total_Tests': len(perf_data)
            })

        combined_df = pd.DataFrame(combined_data)
        combined_df = combined_df.sort_values('Avg_Response_Time_ms')
        st.dataframe(combined_df.round(2), use_container_width=True)

    # Export options
    st.header("📤 Export Options")

    col1, col2, col3 = st.columns(3)

    with col1:
        if st.button("Export Performance Data"):
            csv = perf_df.to_csv(index=False)
            st.download_button(
                label="Download Performance CSV",
                data=csv,
                file_name="performance_data.csv",
                mime="text/csv"
            )

    with col2:
        if st.button("Export Resource Data"):
            csv = res_df.to_csv(index=False)
            st.download_button(
                label="Download Resource CSV",
                data=csv,
                file_name="resource_data.csv",
                mime="text/csv"
            )

    with col3:
        if st.button("Export Combined Data"):
            csv = combined_df.to_csv(index=False)
            st.download_button(
                label="Download Combined CSV",
                data=csv,
                file_name="combined_data.csv",
                mime="text/csv"
            )


# Page routing
if page == "🏠 Home & Overview":
    show_home_page(filtered_performance, filtered_resource)
elif page == "🎯 Decision Dashboard":
    show_decision_page(filtered_decision)
elif page == "🚀 Performance Analysis":
    show_performance_page(filtered_performance)
elif page == "💾 Resource Analysis":
    show_resource_page(filtered_resource)
elif page == "🏗️ Infrastructure Analysis":
    show_infrastructure_page(filtered_performance, filtered_resource)
elif page == "📊 Comprehensive Analysis":
    show_comprehensive_page(filtered_performance, filtered_resource)
elif page == "🔍 Detailed Metrics":
    show_detailed_page(filtered_performance, filtered_resource)
