package org.techishthoughts.payload.service;

import java.util.List;

import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.service.result.BenchmarkResult;
import org.techishthoughts.payload.service.result.CompressionResult;
import org.techishthoughts.payload.service.result.SerializationResult;

/**
 * Common interface for all serialization framework services.
 * Provides standardized methods for serialization, compression, and benchmarking.
 */
public interface SerializationService {

    /**
     * Get the name of this serialization framework
     */
    String getFrameworkName();

    /**
     * Serialize a list of users to the framework's native format
     */
    SerializationResult serialize(List<User> users) throws SerializationException;

    /**
     * Deserialize data back to a list of users
     */
    List<User> deserialize(byte[] data) throws SerializationException;

    /**
     * Compress serialized data using the best compression algorithm for this framework
     */
    CompressionResult compress(byte[] data) throws SerializationException;

    /**
     * Decompress data
     */
    byte[] decompress(byte[] compressedData) throws SerializationException;

    /**
     * Run a comprehensive benchmark with the given configuration
     */
    BenchmarkResult runBenchmark(BenchmarkConfig config) throws SerializationException;

    /**
     * Test roundtrip serialization/deserialization
     */
    default boolean testRoundtrip(List<User> users) {
        try {
            SerializationResult serialized = serialize(users);
            List<User> deserialized = deserialize(serialized.getData());
            return users.size() == deserialized.size();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get supported compression algorithms for this framework
     */
    List<String> getSupportedCompressionAlgorithms();

    /**
     * Check if this framework supports schema evolution
     */
    boolean supportsSchemaEvolution();

    /**
     * Get the typical use case for this framework
     */
    String getTypicalUseCase();
}