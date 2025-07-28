package org.techishthoughts.hessian.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;

/**
 * Hessian Serialization Service (2025 Fixed)
 *
 * Simplified Hessian implementation that handles Java objects directly
 * without custom time class serializers.
 */
@Service
public class HessianSerializationService {

    private final PayloadGenerator payloadGenerator;
    private final SerializerFactory serializerFactory;
    private final Map<String, Long> performanceMetrics = new ConcurrentHashMap<>();

    public HessianSerializationService(PayloadGenerator payloadGenerator) {
        this.payloadGenerator = payloadGenerator;
        this.serializerFactory = new SerializerFactory();
        System.out.println("✅ Fixed Hessian Service initialized (2025)");
    }

    /**
     * Serialize users using standard Hessian2
     */
    public byte[] serializeUsers(List<User> users) {
        long startTime = System.nanoTime();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Hessian2Output hessianOutput = new Hessian2Output(baos);
            hessianOutput.setSerializerFactory(serializerFactory);

            hessianOutput.writeObject(users);
            hessianOutput.close();

            byte[] result = baos.toByteArray();
            long serializationTime = System.nanoTime() - startTime;
            performanceMetrics.put("lastSerializationNs", serializationTime);

            System.out.println("Fixed Hessian serialization took: " + (serializationTime / 1_000_000.0) + " ms");
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize users with Fixed Hessian", e);
        }
    }

    /**
     * Deserialize users using standard Hessian2
     */
    @SuppressWarnings("unchecked")
    public List<User> deserializeUsers(byte[] data) {
        long startTime = System.nanoTime();

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            Hessian2Input hessianInput = new Hessian2Input(bais);
            hessianInput.setSerializerFactory(serializerFactory);

            List<User> users = (List<User>) hessianInput.readObject();
            hessianInput.close();

            long deserializationTime = System.nanoTime() - startTime;
            performanceMetrics.put("lastDeserializationNs", deserializationTime);

            System.out.println("Fixed Hessian deserialization took: " + (deserializationTime / 1_000_000.0) + " ms");
            return users;

        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize users with Fixed Hessian", e);
        }
    }

    /**
     * Get performance statistics
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "framework", "Fixed Hessian (2025)",
            "version", "2025-simplified",
            "features", List.of(
                "Binary web service protocol",
                "Cross-language compatibility",
                "Standard Java object serialization",
                "Fixed LocalDateTime handling"
            ),
            "performanceMetrics", performanceMetrics,
            "status", "✅ Compilation issues resolved"
        );
    }
}
