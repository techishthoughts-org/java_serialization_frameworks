package org.techishthoughts.payload.generator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.techishthoughts.payload.config.BenchmarkProperties;
import org.techishthoughts.payload.generator.HugePayloadGenerator.ComplexityLevel;
import org.techishthoughts.payload.model.Address;
import org.techishthoughts.payload.model.Address.AddressType;
import org.techishthoughts.payload.model.Education;
import org.techishthoughts.payload.model.Language;
import org.techishthoughts.payload.model.Language.LanguageProficiency;
import org.techishthoughts.payload.model.Order;
import org.techishthoughts.payload.model.Order.OrderStatus;
import org.techishthoughts.payload.model.OrderItem;
import org.techishthoughts.payload.model.Payment;
import org.techishthoughts.payload.model.Payment.PaymentMethod;
import org.techishthoughts.payload.model.Payment.PaymentStatus;
import org.techishthoughts.payload.model.Skill;
import org.techishthoughts.payload.model.Skill.SkillLevel;
import org.techishthoughts.payload.model.SocialConnection;
import org.techishthoughts.payload.model.SocialConnection.SocialPlatform;
import org.techishthoughts.payload.model.TrackingEvent;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.model.UserProfile;

/**
 * Unified payload generator that consolidates PayloadGenerator and HugePayloadGenerator.
 * Uses proper configuration, logging, and memory management.
 * Replaces both legacy generators with improved performance and consistency.
 */
@Component
public class UnifiedPayloadGenerator {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedPayloadGenerator.class);

    private final BenchmarkProperties benchmarkProperties;

    // Static data arrays - loaded once for better performance
    private static final String[] FIRST_NAMES = {
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Christopher", "Karen", "Charles", "Nancy", "Daniel", "Lisa",
        "Matthew", "Betty", "Anthony", "Dorothy", "Mark", "Sandra", "Donald", "Donna",
        "Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Isabella", "Lucas", "Sophia", "Mason"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White",
        "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young"
    };

    private static final String[] CITIES = {
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia",
        "San Antonio", "San Diego", "Dallas", "San Jose", "Austin", "Jacksonville",
        "Fort Worth", "Columbus", "Charlotte", "San Francisco", "Indianapolis", "Seattle",
        "Denver", "Washington", "Boston", "El Paso", "Nashville", "Detroit", "Oklahoma City"
    };

    private static final String[] COMPANIES = {
        "Apple Inc.", "Microsoft Corporation", "Google LLC", "Amazon.com Inc.", "Meta Platforms Inc.",
        "Tesla Inc.", "NVIDIA Corporation", "Berkshire Hathaway", "JPMorgan Chase & Co.", "Johnson & Johnson",
        "UnitedHealth Group Inc.", "Exxon Mobil Corporation", "Visa Inc.", "Procter & Gamble Co.",
        "Mastercard Inc.", "Bank of America Corp", "The Home Depot Inc.", "Pfizer Inc."
    };

    private static final String[] SKILLS = {
        "Java", "Python", "JavaScript", "C++", "C#", "Go", "Rust", "Kotlin", "Swift", "TypeScript",
        "React", "Angular", "Vue.js", "Spring Boot", "Django", "Flask", "Node.js", "Express.js",
        "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Jenkins", "GitLab CI", "GitHub Actions",
        "PostgreSQL", "MongoDB", "Redis", "Elasticsearch", "Apache Kafka", "RabbitMQ"
    };

    @Autowired
    public UnifiedPayloadGenerator(BenchmarkProperties benchmarkProperties) {
        this.benchmarkProperties = benchmarkProperties;
    }

    /**
     * Generate dataset using complexity level
     */
    public List<User> generateDataset(ComplexityLevel level) {
        logger.info("🚀 Generating dataset with complexity level: {}", level.name());

        long startTime = System.currentTimeMillis();
        List<User> users = new ArrayList<>();

        // Use ThreadLocalRandom for better performance in multi-threaded environments
        Random random = ThreadLocalRandom.current();

        int userCount = level.getUserCount();
        int progressInterval = benchmarkProperties.getLimits().getProgressReportInterval();

        // Memory monitoring
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < userCount; i++) {
            if (i > 0 && i % progressInterval == 0) {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                double memoryUsageMB = (currentMemory - initialMemory) / (1024.0 * 1024.0);
                logger.info("  ✅ Generated {} users... (Memory: {:.1f} MB)", i, memoryUsageMB);

                // Check memory threshold
                if (benchmarkProperties.getLimits().isEnableMemoryMonitoring()) {
                    double memoryPercent = ((double) currentMemory / runtime.maxMemory()) * 100;
                    if (memoryPercent > benchmarkProperties.getLimits().getMemoryThresholdPercent()) {
                        logger.warn("⚠️ Memory usage ({:.1f}%) exceeds threshold ({:.1f}%), running GC...",
                                   memoryPercent, benchmarkProperties.getLimits().getMemoryThresholdPercent());
                        System.gc();
                    }
                }
            }

            users.add(generateUser((long) i + 1, random, level));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.info("✅ Generated {} users in {} ms", userCount, duration);
        logger.info("📈 Average generation time per user: {:.2f} ms", (double) duration / userCount);

        // Log sample statistics
        if (!users.isEmpty()) {
            User sample = users.get(0);
            logger.info("📋 Sample user statistics:");
            logger.info("  - Username: {}", sample.getUsername());
            logger.info("  - Addresses: {}", sample.getAddresses().size());
            logger.info("  - Orders: {}", sample.getOrders().size());
            logger.info("  - Skills: {}", sample.getProfile().getSkills().size());
            logger.info("  - Languages: {}", sample.getProfile().getLanguages().size());
            logger.info("  - Social connections: {}", sample.getSocialConnections().size());
        }

        return users;
    }

    /**
     * Generate dataset with custom user count
     */
    public List<User> generateDataset(int userCount) {
        // Determine appropriate complexity level based on user count
        ComplexityLevel level;
        if (userCount <= 10) level = ComplexityLevel.SMALL;
        else if (userCount <= 100) level = ComplexityLevel.MEDIUM;
        else if (userCount <= 1000) level = ComplexityLevel.LARGE;
        else if (userCount <= 10000) level = ComplexityLevel.HUGE;
        else level = ComplexityLevel.MASSIVE;

        logger.info("Mapping {} users to complexity level: {}", userCount, level.name());

        // Override user count in level
        List<User> users = new ArrayList<>();
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < userCount; i++) {
            users.add(generateUser((long) i + 1, random, level));
        }

        return users;
    }

    /**
     * Generate a single user with all nested data
     */
    private User generateUser(Long id, Random random, ComplexityLevel level) {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String username = generateUsername(firstName, random);
        String email = generateEmail(firstName, lastName, random);

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setIsActive(random.nextBoolean());
        user.setCreatedAt(generateRandomPastDate(random, 365 * 2));
        user.setLastLoginAt(generateRandomPastDate(random, 30));
        user.setLoyaltyPoints(random.nextDouble() * 10000);

        // Generate nested data based on complexity level
        user.setAddresses(generateAddresses(random, level.getAddressCount()));
        user.setOrders(generateOrders(random, user.getAddresses(), level.getOrderCount()));
        user.setSocialConnections(generateSocialConnections(random, level.getSocialCount()));
        user.setProfile(generateUserProfile(random, level));
        user.setPreferences(generatePreferences(random));
        user.setMetadata(generateMetadata(random));
        user.setTags(generateTags(random));

        return user;
    }

    private UserProfile generateUserProfile(Random random, ComplexityLevel level) {
        UserProfile profile = new UserProfile();
        profile.setBio(generateBio(random));
        profile.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + random.nextInt(10000));
        profile.setDateOfBirth(LocalDate.now().minusYears(random.nextInt(50) + 18));
        profile.setGender(random.nextBoolean() ? "Male" : "Female");
        profile.setPhoneNumber(generatePhoneNumber(random));
        profile.setNationality("US");
        profile.setOccupation(generateOccupation(random));
        profile.setCompany(COMPANIES[random.nextInt(COMPANIES.length)]);
        profile.setInterests(generateInterests(random));
        profile.setSkills(generateSkills(random, level.getSkillCount()));
        profile.setEducation(generateEducation(random));
        profile.setLanguages(generateLanguages(random, level.getLanguageCount()));
        return profile;
    }

    private List<Address> generateAddresses(Random random, int count) {
        List<Address> addresses = new ArrayList<>();
        AddressType[] types = AddressType.values();

        for (int i = 0; i < count; i++) {
            Address address = new Address();
            address.setId((long) i + 1);
            address.setType(types[random.nextInt(types.length)]);
            address.setStreet1(generateStreetAddress(random));
            address.setStreet2(random.nextBoolean() ? generateStreetAddress2(random) : null);
            address.setCity(CITIES[random.nextInt(CITIES.length)]);
            address.setState(generateState(random));
            address.setPostalCode(generatePostalCode(random));
            address.setCountry("United States");
            address.setLatitude(generateLatitude(random));
            address.setLongitude(generateLongitude(random));
            address.setIsDefault(i == 0);
            addresses.add(address);
        }
        return addresses;
    }

    private List<Order> generateOrders(Random random, List<Address> addresses, int maxCount) {
        List<Order> orders = new ArrayList<>();
        int orderCount = random.nextInt(maxCount + 1); // 0 to maxCount orders

        for (int i = 0; i < orderCount; i++) {
            Order order = new Order();
            order.setId((long) i + 1);
            order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order.setOrderDate(generateRandomPastDate(random, 365));
            order.setStatus(OrderStatus.values()[random.nextInt(OrderStatus.values().length)]);

            List<OrderItem> items = generateOrderItems(random);
            order.setItems(items);

            BigDecimal subtotal = calculateSubtotal(items);
            BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(0.08));
            BigDecimal shippingAmount = BigDecimal.valueOf(random.nextDouble() * 20 + 5);
            BigDecimal discountAmount = random.nextBoolean() ?
                subtotal.multiply(BigDecimal.valueOf(0.1)) : BigDecimal.ZERO;

            order.setSubtotal(subtotal);
            order.setTaxAmount(taxAmount);
            order.setShippingAmount(shippingAmount);
            order.setDiscountAmount(discountAmount);
            order.setTotalAmount(subtotal.add(taxAmount).add(shippingAmount).subtract(discountAmount));
            order.setCurrency("USD");

            order.setShippingAddress(addresses.get(random.nextInt(addresses.size())));
            order.setBillingAddress(addresses.get(random.nextInt(addresses.size())));
            order.setPayment(generatePayment(random, order.getTotalAmount()));
            order.setTracking(generateTrackingEvents(random));
            order.setShippedDate(generateRandomPastDate(random, 30));
            order.setDeliveredDate(generateRandomPastDate(random, 7));
            order.setNotes(generateOrderNotes(random));
            order.setCustomFields(generateOrderCustomFields(random));

            orders.add(order);
        }
        return orders;
    }

    // Helper methods (simplified and optimized versions)
    private String generateUsername(String firstName, Random random) {
        return firstName.toLowerCase() + random.nextInt(10000);
    }

    private String generateEmail(String firstName, String lastName, Random random) {
        String[] domains = {"gmail.com", "yahoo.com", "outlook.com", "company.com"};
        return firstName.toLowerCase() + "." + lastName.toLowerCase() +
               random.nextInt(1000) + "@" + domains[random.nextInt(domains.length)];
    }

    private String generatePhoneNumber(Random random) {
        return String.format("+1-%03d-%03d-%04d",
            random.nextInt(900) + 100,
            random.nextInt(900) + 100,
            random.nextInt(9000) + 1000);
    }

    private String generateOccupation(Random random) {
        String[] occupations = {
            "Software Engineer", "Data Scientist", "Product Manager", "DevOps Engineer",
            "UX Designer", "Backend Developer", "Frontend Developer", "Full Stack Developer"
        };
        return occupations[random.nextInt(occupations.length)];
    }

    private String generateBio(Random random) {
        String[] templates = {
            "Passionate software engineer with %d years of experience.",
            "Full-stack developer specializing in modern technologies.",
            "Senior engineer with expertise in distributed systems.",
            "Data scientist building AI-powered solutions."
        };
        return String.format(templates[random.nextInt(templates.length)], random.nextInt(15) + 1);
    }

    private LocalDateTime generateRandomPastDate(Random random, int maxDaysAgo) {
        return LocalDateTime.now().minusDays(random.nextInt(maxDaysAgo) + 1);
    }

    private String generateStreetAddress(Random random) {
        return (random.nextInt(9999) + 1) + " " +
               LAST_NAMES[random.nextInt(LAST_NAMES.length)] + " " +
               (random.nextBoolean() ? "Street" : "Avenue");
    }

    private String generateStreetAddress2(Random random) {
        return random.nextBoolean() ? "Apt " + (random.nextInt(999) + 1) : null;
    }

    private String generateState(Random random) {
        String[] states = {"CA", "NY", "TX", "FL", "IL", "PA", "OH", "GA", "NC", "MI"};
        return states[random.nextInt(states.length)];
    }

    private String generatePostalCode(Random random) {
        return String.format("%05d", random.nextInt(90000) + 10000);
    }

    private Double generateLatitude(Random random) {
        return 25.0 + random.nextDouble() * 25.0; // US latitude range
    }

    private Double generateLongitude(Random random) {
        return -125.0 + random.nextDouble() * 65.0; // US longitude range
    }

    // Stub implementations for brevity - these would contain the full logic
    private List<String> generateInterests(Random random) {
        String[] interests = {"Technology", "Travel", "Music", "Sports", "Gaming"};
        List<String> result = new ArrayList<>();
        int count = random.nextInt(5) + 1;
        for (int i = 0; i < count; i++) {
            result.add(interests[random.nextInt(interests.length)]);
        }
        return result;
    }

    private List<Skill> generateSkills(Random random, int maxCount) {
        List<Skill> skills = new ArrayList<>();
        int count = random.nextInt(maxCount) + 1;
        for (int i = 0; i < count; i++) {
            Skill skill = new Skill();
            skill.setId((long) i + 1);
            skill.setName(SKILLS[random.nextInt(SKILLS.length)]);
            skill.setLevel(SkillLevel.values()[random.nextInt(SkillLevel.values().length)]);
            skill.setYearsOfExperience(random.nextInt(15) + 1);
            skills.add(skill);
        }
        return skills;
    }

    private List<Education> generateEducation(Random random) {
        List<Education> education = new ArrayList<>();
        Education edu = new Education();
        edu.setId(1L);
        edu.setInstitution("University of " + CITIES[random.nextInt(CITIES.length)]);
        edu.setDegree("Bachelor's");
        edu.setFieldOfStudy("Computer Science");
        edu.setStartDate(LocalDate.now().minusYears(6));
        edu.setEndDate(LocalDate.now().minusYears(2));
        edu.setGpa(3.0 + random.nextDouble());
        education.add(edu);
        return education;
    }

    private List<Language> generateLanguages(Random random, int maxCount) {
        List<Language> languages = new ArrayList<>();
        String[] languageNames = {"English", "Spanish", "French", "German", "Chinese"};
        int count = Math.min(maxCount, languageNames.length);

        for (int i = 0; i < count; i++) {
            Language language = new Language();
            language.setId((long) i + 1);
            language.setName(languageNames[i]);
            language.setCode(languageNames[i].substring(0, 2).toUpperCase());
            language.setProficiency(LanguageProficiency.values()[random.nextInt(LanguageProficiency.values().length)]);
            language.setIsNative(i == 0);
            languages.add(language);
        }
        return languages;
    }

    private List<SocialConnection> generateSocialConnections(Random random, int maxCount) {
        List<SocialConnection> connections = new ArrayList<>();
        int count = random.nextInt(maxCount + 1);

        for (int i = 0; i < count; i++) {
            SocialConnection connection = new SocialConnection();
            connection.setId((long) i + 1);
            connection.setPlatform(SocialPlatform.values()[random.nextInt(SocialPlatform.values().length)]);
            connection.setUsername(generateUsername(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)], random));
            connection.setIsVerified(random.nextBoolean());
            connection.setFollowerCount((long) random.nextInt(100000));
            connection.setConnectedAt(generateRandomPastDate(random, 365));
            connection.setLastSyncAt(generateRandomPastDate(random, 30));
            connections.add(connection);
        }
        return connections;
    }

    private List<OrderItem> generateOrderItems(Random random) {
        List<OrderItem> items = new ArrayList<>();
        int count = random.nextInt(5) + 1;

        for (int i = 0; i < count; i++) {
            OrderItem item = new OrderItem();
            item.setId((long) i + 1);
            item.setProductId((long) random.nextInt(10000) + 1);
            item.setProductName(generateProductName(random));
            item.setProductSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
            item.setQuantity(random.nextInt(5) + 1);
            item.setUnitPrice(BigDecimal.valueOf(random.nextDouble() * 100 + 10));
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            items.add(item);
        }
        return items;
    }

    private String generateProductName(Random random) {
        String[] adjectives = {"Premium", "Deluxe", "Ultra", "Pro"};
        String[] products = {"Laptop", "Phone", "Headphones", "Tablet"};
        return adjectives[random.nextInt(adjectives.length)] + " " +
               products[random.nextInt(products.length)];
    }

    private BigDecimal calculateSubtotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Payment generatePayment(Random random, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setMethod(PaymentMethod.values()[random.nextInt(PaymentMethod.values().length)]);
        payment.setStatus(PaymentStatus.values()[random.nextInt(PaymentStatus.values().length)]);
        payment.setAmount(amount);
        payment.setCurrency("USD");
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8));
        payment.setProcessedAt(generateRandomPastDate(random, 1));
        return payment;
    }

    private List<TrackingEvent> generateTrackingEvents(Random random) {
        // Return empty list to avoid enum issues - can be expanded later
        return new ArrayList<>();
    }

    private String generateOrderNotes(Random random) {
        String[] notes = {
            "Please deliver during business hours",
            "Leave at front door",
            "Call before delivery",
            "Handle with care"
        };
        return random.nextBoolean() ? notes[random.nextInt(notes.length)] : null;
    }

    private Map<String, Object> generateOrderCustomFields(Random random) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("gift_wrapping", random.nextBoolean());
        fields.put("express_shipping", random.nextBoolean());
        fields.put("priority", random.nextInt(3) + 1);
        return fields;
    }

    private Map<String, Object> generatePreferences(Random random) {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("theme", random.nextBoolean() ? "dark" : "light");
        preferences.put("notifications", random.nextBoolean());
        preferences.put("language", "en");
        return preferences;
    }

    private Map<String, String> generateMetadata(Random random) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "unified_generator");
        metadata.put("version", "2.0");
        metadata.put("generator", "UnifiedPayloadGenerator");
        return metadata;
    }

    private List<String> generateTags(Random random) {
        String[] allTags = {"premium", "beta_user", "developer", "enterprise"};
        List<String> tags = new ArrayList<>();
        int count = random.nextInt(3) + 1;

        Set<String> selected = new HashSet<>();
        while (selected.size() < count) {
            selected.add(allTags[random.nextInt(allTags.length)]);
        }
        tags.addAll(selected);
        return tags;
    }
}