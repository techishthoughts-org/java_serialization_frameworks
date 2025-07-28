#!/usr/bin/env python3
"""
Comprehensive Framework Launcher (2025)
Starts all 17 serialization frameworks simultaneously for benchmarking
"""

import os
import signal
import subprocess
import sys
import time

import requests

# All 15 frameworks with their details
FRAMEWORKS = {
    'jackson': {
        'port': 8081,
        'name': 'Jackson JSON',
        'module': 'jackson-poc',
        'health_endpoint': '/actuator/health'
    },
    'protobuf': {
        'port': 8082,
        'name': 'Protocol Buffers',
        'module': 'protobuf-poc',
        'health_endpoint': '/actuator/health'
    },
    'avro': {
        'port': 8083,
        'name': 'Apache Avro',
        'module': 'avro-poc',
        'health_endpoint': '/actuator/health'
    },
    'kryo': {
        'port': 8084,
        'name': 'Kryo',
        'module': 'kryo-poc',
        'health_endpoint': '/actuator/health'
    },

    'msgpack': {
        'port': 8086,
        'name': 'MessagePack',
        'module': 'msgpack-poc',
        'health_endpoint': '/actuator/health'
    },
    'thrift': {
        'port': 8087,
        'name': 'Apache Thrift',
        'module': 'thrift-poc',
        'health_endpoint': '/actuator/health'
    },
    'capnproto': {
        'port': 8088,
        'name': 'Cap\'n Proto',
        'module': 'capnproto-poc',
        'health_endpoint': '/actuator/health'
    },

    'fst': {
        'port': 8090,
        'name': 'FST (Fast Serialization)',
        'module': 'fst-poc',
        'health_endpoint': '/actuator/health'
    },
    'flatbuffers': {
        'port': 8091,
        'name': 'FlatBuffers',
        'module': 'flatbuffers-poc',
        'health_endpoint': '/actuator/health'
    },
    'grpc': {
        'port': 8092,
        'name': 'gRPC',
        'module': 'grpc-poc',
        'health_endpoint': '/actuator/health'
    },
    'cbor': {
        'port': 8093,
        'name': 'CBOR',
        'module': 'cbor-poc',
        'health_endpoint': '/actuator/health'
    },
    'bson': {
        'port': 8094,
        'name': 'BSON',
        'module': 'bson-poc',
        'health_endpoint': '/actuator/health'
    },
    'arrow': {
        'port': 8095,
        'name': 'Apache Arrow',
        'module': 'arrow-poc',
        'health_endpoint': '/actuator/health'
    },
    'sbe': {
        'port': 8096,
        'name': 'SBE',
        'module': 'sbe-poc',
        'health_endpoint': '/actuator/health'
    },
    'parquet': {
        'port': 8097,
        'name': 'Apache Parquet',
        'module': 'parquet-poc',
        'health_endpoint': '/actuator/health'
    }
}


class ComprehensiveFrameworkLauncher:
    def __init__(self):
        self.processes = {}
        self.startup_results = {}

    def cleanup_existing_processes(self):
        """Kill any existing Spring Boot processes"""
        print("🧹 Cleaning up existing processes...")
        try:
            subprocess.run(['pkill', '-f', 'spring-boot:run'], check=False)
            subprocess.run(['pkill', '-f', 'java.*target.*classes'],
                           check=False)
            time.sleep(2)
            print("✅ Cleanup complete")
        except Exception as e:
            print("⚠️  Cleanup warning: {}".format(e))

    def start_framework(self, framework_key, framework_info):
        """Start a single framework in the background"""
        module = framework_info['module']
        port = framework_info['port']
        name = framework_info['name']

        print("🚀 Starting {} on port {}...".format(name, port))

        try:
            # Start the process
            process = subprocess.Popen(
                ['mvn', 'spring-boot:run', '-pl', module],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                cwd=os.getcwd(),
                preexec_fn=os.setsid  # Create new process group
            )

            self.processes[framework_key] = {
                'process': process,
                'port': port,
                'name': name,
                'module': module
            }

            print("✅ {} process started (PID: {})".format(name, process.pid))
            return True

        except Exception as e:
            print("❌ Failed to start {}: {}".format(name, e))
            return False

    def wait_for_startup(self, max_wait_time=120):
        """Wait for all frameworks to be ready"""
        print("\n⏳ Waiting up to {} seconds for all "
              "frameworks to start...".format(max_wait_time))

        start_time = time.time()
        ready_frameworks = set()

        while time.time() - start_time < max_wait_time:
            for framework_key, framework_info in FRAMEWORKS.items():
                if framework_key in ready_frameworks:
                    continue

                port = framework_info['port']
                health_endpoint = framework_info['health_endpoint']

                try:
                    response = requests.get(
                        "http://localhost:{}{}".format(port, health_endpoint),
                        timeout=2
                    )
                    if response.status_code == 200:
                        ready_frameworks.add(framework_key)
                        print("✅ {} is ready!".format(
                            framework_info['name']))
                except Exception:
                    pass  # Still starting up

            if len(ready_frameworks) == len(FRAMEWORKS):
                print("\n🎉 ALL {} FRAMEWORKS ARE READY!".format(
                    len(FRAMEWORKS)))
                return ready_frameworks

            time.sleep(3)

        print("\n⏰ Startup timeout after {} seconds".format(max_wait_time))
        print("✅ Ready: {}/{} frameworks".format(
            len(ready_frameworks), len(FRAMEWORKS)))

        # Show which ones are ready and which are not
        for framework_key, framework_info in FRAMEWORKS.items():
            status = ("✅ READY" if framework_key in ready_frameworks
                      else "❌ NOT READY")
            print("   {} ({}): {}".format(
                framework_info['name'], framework_info['port'], status))

        return ready_frameworks

    def start_all_frameworks(self):
        """Start all frameworks simultaneously"""
        print("🚀 COMPREHENSIVE FRAMEWORK LAUNCHER")
        print("=" * 50)

        # Cleanup first
        self.cleanup_existing_processes()

        # Start all frameworks
        print("\n🔥 Starting {} frameworks simultaneously...".format(
            len(FRAMEWORKS)))

        start_count = 0
        for framework_key, framework_info in FRAMEWORKS.items():
            if self.start_framework(framework_key, framework_info):
                start_count += 1

        print("\n📊 Started {}/{} framework processes".format(
            start_count, len(FRAMEWORKS)))

        # Wait for all to be ready
        ready_frameworks = self.wait_for_startup()

        return ready_frameworks

    def show_status(self):
        """Show the status of all frameworks"""
        print("\n📋 FRAMEWORK STATUS SUMMARY")
        print("=" * 50)

        total_ready = 0
        for framework_key, framework_info in FRAMEWORKS.items():
            port = framework_info['port']
            name = framework_info['name']

            try:
                response = requests.get(
                    "http://localhost:{}/actuator/health".format(port),
                    timeout=2)
                if response.status_code == 200:
                    status = "✅ RUNNING"
                    total_ready += 1
                else:
                    status = "❌ HTTP {}".format(response.status_code)
            except requests.exceptions.ConnectionError:
                status = "❌ CONNECTION REFUSED"
            except Exception as e:
                status = "❌ ERROR: {}".format(str(e)[:30])

            print("Port {:4} | {:25} | {}".format(port, name, status))

        print("\n🏆 TOTAL READY: {}/{} frameworks".format(
            total_ready, len(FRAMEWORKS)))

        if total_ready == len(FRAMEWORKS):
            print("\n🎉 ALL FRAMEWORKS ARE READY FOR BENCHMARKING!")
            return True
        else:
            print("\n⚠️  {} frameworks need attention".format(
                len(FRAMEWORKS) - total_ready))
            return False

    def cleanup(self):
        """Clean up all processes"""
        print("\n🛑 Shutting down all frameworks...")

        for framework_key, process_info in self.processes.items():
            try:
                process = process_info['process']
                name = process_info['name']

                # Kill the process group
                os.killpg(os.getpgid(process.pid), signal.SIGTERM)
                print("✅ Stopped {}".format(name))
            except Exception as e:
                print("⚠️  Error stopping {}: {}".format(name, e))

        # Final cleanup
        self.cleanup_existing_processes()
        print("🏁 All processes cleaned up")


def signal_handler(sig, frame):
    """Handle Ctrl+C gracefully"""
    print("\n\n🛑 Received interrupt signal...")
    if 'launcher' in globals():
        launcher.cleanup()
    sys.exit(0)


def main():
    global launcher

    # Set up signal handler for graceful shutdown
    signal.signal(signal.SIGINT, signal_handler)

    launcher = ComprehensiveFrameworkLauncher()

    try:
        # Start all frameworks
        launcher.start_all_frameworks()

        # Show final status
        launcher.show_status()

        # Keep running until interrupted
        print("\n🔄 All frameworks are running. "
              "Press Ctrl+C to stop all services.")
        print("💡 You can now run benchmarks against all frameworks!")

        # Keep the script alive
        while True:
            time.sleep(10)

    except KeyboardInterrupt:
        print("\n🛑 Interrupted by user")
    except Exception as e:
        print("\n❌ Unexpected error: {}".format(e))
    finally:
        launcher.cleanup()


if __name__ == "__main__":
    main()
