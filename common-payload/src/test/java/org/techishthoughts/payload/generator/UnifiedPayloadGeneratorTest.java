package org.techishthoughts.payload.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techishthoughts.payload.config.BenchmarkProperties;
import org.techishthoughts.payload.generator.HugePayloadGenerator.ComplexityLevel;
import org.techishthoughts.payload.model.User;

/**
 * Unit tests for UnifiedPayloadGenerator.
 * Tests data generation consistency, memory efficiency, and performance.
 */
@ExtendWith(MockitoExtension.class)
class UnifiedPayloadGeneratorTest {

    private UnifiedPayloadGenerator generator;
    private BenchmarkProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BenchmarkProperties();
        generator = new UnifiedPayloadGenerator(properties);
    }

    @Test
    void testGenerateDatasetWithSmallComplexity() {
        // Given
        ComplexityLevel complexity = ComplexityLevel.SMALL;

        // When
        List<User> users = generator.generateDataset(complexity);

        // Then
        assertNotNull(users);
        assertEquals(complexity.getUserCount(), users.size());

        // Verify first user has expected nested data
        if (!users.isEmpty()) {
            User firstUser = users.get(0);
            assertNotNull(firstUser.getId());
            assertNotNull(firstUser.getUsername());
            assertNotNull(firstUser.getEmail());
            assertNotNull(firstUser.getProfile());
            assertNotNull(firstUser.getAddresses());
            assertNotNull(firstUser.getOrders());

            // Check complexity constraints
            assertTrue(firstUser.getAddresses().size() <= complexity.getAddressCount());
            assertTrue(firstUser.getOrders().size() <= complexity.getOrderCount());
            assertTrue(firstUser.getProfile().getSkills().size() <= complexity.getSkillCount());
        }
    }

    @Test
    void testGenerateDatasetWithMediumComplexity() {
        // Given
        ComplexityLevel complexity = ComplexityLevel.MEDIUM;

        // When
        List<User> users = generator.generateDataset(complexity);

        // Then
        assertNotNull(users);
        assertEquals(complexity.getUserCount(), users.size());

        // Verify complexity scaling
        if (!users.isEmpty()) {
            User user = users.get(0);
            assertTrue(user.getAddresses().size() <= complexity.getAddressCount());
            assertTrue(user.getProfile().getSkills().size() <= complexity.getSkillCount());
            assertTrue(user.getProfile().getLanguages().size() <= complexity.getLanguageCount());
        }
    }

    @Test
    void testGenerateDatasetWithCustomUserCount() {
        // Given
        int customUserCount = 50;

        // When
        List<User> users = generator.generateDataset(customUserCount);

        // Then
        assertNotNull(users);
        assertEquals(customUserCount, users.size());
    }

    @Test
    void testGeneratedUsersHaveUniqueIds() {
        // Given
        List<User> users = generator.generateDataset(ComplexityLevel.SMALL);

        // When & Then
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            assertEquals(Long.valueOf(i + 1), user.getId());
        }
    }

    @Test
    void testGeneratedUsersHaveValidEmails() {
        // Given
        List<User> users = generator.generateDataset(ComplexityLevel.SMALL);

        // When & Then
        for (User user : users) {
            String email = user.getEmail();
            assertNotNull(email);
            assertTrue(email.contains("@"));
            assertTrue(email.contains("."));
        }
    }

    @Test
    void testGeneratedUsersHaveCompleteProfiles() {
        // Given
        List<User> users = generator.generateDataset(ComplexityLevel.SMALL);

        // When & Then
        for (User user : users) {
            assertNotNull(user.getProfile());
            assertNotNull(user.getProfile().getBio());
            assertNotNull(user.getProfile().getOccupation());
            assertNotNull(user.getProfile().getSkills());
            assertNotNull(user.getProfile().getLanguages());
            assertFalse(user.getProfile().getSkills().isEmpty());
            assertFalse(user.getProfile().getLanguages().isEmpty());
        }
    }

    @Test
    void testGeneratedAddressesHaveValidCoordinates() {
        // Given
        List<User> users = generator.generateDataset(ComplexityLevel.SMALL);

        // When & Then
        for (User user : users) {
            user.getAddresses().forEach(address -> {
                assertNotNull(address.getLatitude());
                assertNotNull(address.getLongitude());
                // US latitude range: 25-50
                assertTrue(address.getLatitude() >= 25.0 && address.getLatitude() <= 50.0);
                // US longitude range: -125 to -60
                assertTrue(address.getLongitude() >= -125.0 && address.getLongitude() <= -60.0);
            });
        }
    }

    @Test
    void testGeneratedOrdersHaveValidTotals() {
        // Given
        List<User> users = generator.generateDataset(ComplexityLevel.MEDIUM);

        // When & Then
        for (User user : users) {
            user.getOrders().forEach(order -> {
                assertNotNull(order.getTotalAmount());
                assertNotNull(order.getSubtotal());
                assertNotNull(order.getItems());
                assertFalse(order.getItems().isEmpty());

                // Verify order total makes sense
                assertTrue(order.getTotalAmount().doubleValue() > 0);
                assertTrue(order.getSubtotal().doubleValue() > 0);
            });
        }
    }

    @Test
    void testMemoryUsageIsReasonable() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Clean up before test
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // When
        List<User> users = generator.generateDataset(ComplexityLevel.MEDIUM);

        // Then
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;
        double memoryUsedMB = memoryUsed / (1024.0 * 1024.0);

        // Verify reasonable memory usage (should be less than 100MB for MEDIUM complexity)
        assertTrue(memoryUsedMB < 100.0, "Memory usage too high: " + memoryUsedMB + " MB");

        // Verify we actually generated data
        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    void testPerformanceIsAcceptable() {
        // Given
        long startTime = System.currentTimeMillis();

        // When
        List<User> users = generator.generateDataset(ComplexityLevel.SMALL);

        // Then
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Should generate SMALL dataset (10 users) in less than 1 second
        assertTrue(duration < 1000, "Generation took too long: " + duration + " ms");
        assertEquals(ComplexityLevel.SMALL.getUserCount(), users.size());
    }

    @Test
    void testComplexityLevelMappingForCustomUserCount() {
        // Test boundary conditions for complexity level mapping

        // Small range (≤ 10)
        List<User> small = generator.generateDataset(5);
        assertEquals(5, small.size());

        // Medium range (≤ 100)
        List<User> medium = generator.generateDataset(50);
        assertEquals(50, medium.size());

        // Large range (≤ 1000)
        List<User> large = generator.generateDataset(500);
        assertEquals(500, large.size());
    }
}