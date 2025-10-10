package org.techishthoughts.payload.service;

/**
 * Standardized exception for serialization operations.
 * Provides better error handling and debugging information.
 */
public class SerializationException extends Exception {

    private final String frameworkName;
    private final String operation;
    private final long dataSize;

    public SerializationException(String frameworkName, String operation, String message) {
        super(String.format("[%s:%s] %s", frameworkName, operation, message));
        this.frameworkName = frameworkName;
        this.operation = operation;
        this.dataSize = -1;
    }

    public SerializationException(String frameworkName, String operation, String message, Throwable cause) {
        super(String.format("[%s:%s] %s", frameworkName, operation, message), cause);
        this.frameworkName = frameworkName;
        this.operation = operation;
        this.dataSize = -1;
    }

    public SerializationException(String frameworkName, String operation, String message, long dataSize) {
        super(String.format("[%s:%s] %s (data size: %d bytes)", frameworkName, operation, message, dataSize));
        this.frameworkName = frameworkName;
        this.operation = operation;
        this.dataSize = dataSize;
    }

    public SerializationException(String frameworkName, String operation, String message, long dataSize, Throwable cause) {
        super(String.format("[%s:%s] %s (data size: %d bytes)", frameworkName, operation, message, dataSize), cause);
        this.frameworkName = frameworkName;
        this.operation = operation;
        this.dataSize = dataSize;
        this.initCause(cause);
    }

    public String getFrameworkName() {
        return frameworkName;
    }

    public String getOperation() {
        return operation;
    }

    public long getDataSize() {
        return dataSize;
    }

    /**
     * Create a serialization exception
     */
    public static SerializationException serialization(String framework, String message) {
        return new SerializationException(framework, "SERIALIZE", message);
    }

    public static SerializationException serialization(String framework, String message, Throwable cause) {
        return new SerializationException(framework, "SERIALIZE", message, cause);
    }

    /**
     * Create a deserialization exception
     */
    public static SerializationException deserialization(String framework, String message) {
        return new SerializationException(framework, "DESERIALIZE", message);
    }

    public static SerializationException deserialization(String framework, String message, Throwable cause) {
        return new SerializationException(framework, "DESERIALIZE", message, cause);
    }

    /**
     * Create a compression exception
     */
    public static SerializationException compression(String framework, String message, long dataSize) {
        return new SerializationException(framework, "COMPRESS", message, dataSize);
    }

    public static SerializationException compression(String framework, String message, long dataSize, Throwable cause) {
        return new SerializationException(framework, "COMPRESS", message, dataSize, cause);
    }

    /**
     * Create a decompression exception
     */
    public static SerializationException decompression(String framework, String message, long dataSize) {
        return new SerializationException(framework, "DECOMPRESS", message, dataSize);
    }

    public static SerializationException decompression(String framework, String message, long dataSize, Throwable cause) {
        return new SerializationException(framework, "DECOMPRESS", message, dataSize, cause);
    }
}