package org.techishthoughts.payload.generator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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
import org.techishthoughts.payload.model.TrackingEvent.TrackingEventType;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.model.UserProfile;

/**
 * Generates massive payload data for extreme stress-testing of serialization frameworks.
 * Supports configurable complexity levels and dataset sizes.
 */
public class HugePayloadGenerator {

    // Complexity levels for different testing scenarios
    public enum ComplexityLevel {
        SMALL(10, 2, 3, 5, 2, 1),      // 10 users, 2 addresses, 3 orders, 5 skills, 2 languages, 1 social
        MEDIUM(100, 3, 5, 8, 3, 2),    // 100 users, 3 addresses, 5 orders, 8 skills, 3 languages, 2 social
        LARGE(1000, 5, 10, 12, 4, 3),  // 1000 users, 5 addresses, 10 orders, 12 skills, 4 languages, 3 social
        HUGE(10000, 8, 20, 15, 5, 5),  // 10000 users, 8 addresses, 20 orders, 15 skills, 5 languages, 5 social
        MASSIVE(50000, 10, 30, 20, 6, 8); // 50000 users, 10 addresses, 30 orders, 20 skills, 6 languages, 8 social

        private final int userCount;
        private final int addressCount;
        private final int orderCount;
        private final int skillCount;
        private final int languageCount;
        private final int socialCount;

        ComplexityLevel(int userCount, int addressCount, int orderCount, int skillCount, int languageCount, int socialCount) {
            this.userCount = userCount;
            this.addressCount = addressCount;
            this.orderCount = orderCount;
            this.skillCount = skillCount;
            this.languageCount = languageCount;
            this.socialCount = socialCount;
        }

        public int getUserCount() { return userCount; }
        public int getAddressCount() { return addressCount; }
        public int getOrderCount() { return orderCount; }
        public int getSkillCount() { return skillCount; }
        public int getLanguageCount() { return languageCount; }
        public int getSocialCount() { return socialCount; }
    }

    private static final String[] FIRST_NAMES = {
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Christopher", "Karen", "Charles", "Nancy", "Daniel", "Lisa",
        "Matthew", "Betty", "Anthony", "Dorothy", "Mark", "Sandra", "Donald", "Donna",
        "Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Isabella", "Lucas", "Sophia", "Mason",
        "Mia", "Oliver", "Charlotte", "Elijah", "Amelia", "James", "Harper", "Benjamin", "Evelyn", "Sebastian"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White",
        "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young",
        "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores", "Green", "Adams",
        "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts"
    };

    private static final String[] CITIES = {
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia",
        "San Antonio", "San Diego", "Dallas", "San Jose", "Austin", "Jacksonville",
        "Fort Worth", "Columbus", "Charlotte", "San Francisco", "Indianapolis", "Seattle",
        "Denver", "Washington", "Boston", "El Paso", "Nashville", "Detroit", "Oklahoma City",
        "Portland", "Las Vegas", "Memphis", "Louisville", "Baltimore", "Milwaukee", "Albuquerque",
        "Tucson", "Fresno", "Sacramento", "Atlanta", "Kansas City", "Long Beach", "Colorado Springs",
        "Raleigh", "Miami", "Virginia Beach", "Omaha", "Oakland", "Minneapolis", "Tampa", "Tulsa"
    };

    private static final String[] COMPANIES = {
        "Apple Inc.", "Microsoft Corporation", "Google LLC", "Amazon.com Inc.", "Meta Platforms Inc.",
        "Tesla Inc.", "NVIDIA Corporation", "Berkshire Hathaway", "JPMorgan Chase & Co.", "Johnson & Johnson",
        "UnitedHealth Group Inc.", "Exxon Mobil Corporation", "Visa Inc.", "Procter & Gamble Co.",
        "Mastercard Inc.", "Bank of America Corp", "The Home Depot Inc.", "Pfizer Inc.",
        "Chevron Corporation", "AbbVie Inc.", "Coca-Cola Company", "PepsiCo Inc.", "Walmart Inc.",
        "Walt Disney Company", "Comcast Corporation", "Verizon Communications", "AT&T Inc.",
        "Netflix Inc.", "Adobe Inc.", "Intel Corporation", "Cisco Systems Inc.", "Oracle Corporation",
        "Salesforce Inc.", "PayPal Holdings Inc.", "Broadcom Inc.", "Qualcomm Inc.", "IBM Corporation",
        "Caterpillar Inc.", "Merck & Co. Inc.", "Amgen Inc.", "Boeing Company", "Goldman Sachs Group Inc."
    };

    private static final String[] SKILLS = {
        "Java", "Python", "JavaScript", "C++", "C#", "Go", "Rust", "Kotlin", "Swift", "TypeScript",
        "React", "Angular", "Vue.js", "Spring Boot", "Django", "Flask", "Node.js", "Express.js",
        "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Jenkins", "GitLab CI", "GitHub Actions",
        "PostgreSQL", "MongoDB", "Redis", "Elasticsearch", "Apache Kafka", "RabbitMQ",
        "Machine Learning", "Deep Learning", "Data Science", "DevOps", "Microservices", "GraphQL",
        "TensorFlow", "PyTorch", "Scikit-learn", "Pandas", "NumPy", "Matplotlib", "Seaborn",
        "Apache Spark", "Hadoop", "Hive", "Pig", "HBase", "Cassandra", "Neo4j", "InfluxDB",
        "Prometheus", "Grafana", "Kibana", "Logstash", "Beats", "Ansible", "Terraform", "Puppet",
        "Chef", "Vagrant", "VirtualBox", "VMware", "Hyper-V", "OpenStack", "CloudFoundry"
    };

    private static final String[] LANGUAGES = {
        "English", "Spanish", "French", "German", "Italian", "Portuguese", "Russian", "Japanese",
        "Chinese", "Korean", "Arabic", "Hindi", "Bengali", "Turkish", "Dutch", "Swedish",
        "Norwegian", "Danish", "Finnish", "Polish", "Czech", "Hungarian", "Romanian", "Bulgarian",
        "Greek", "Hebrew", "Thai", "Vietnamese", "Indonesian", "Malay", "Filipino", "Ukrainian"
    };

    private static final String[] BIO_TEMPLATES = {
        "Passionate software engineer with %d years of experience in building scalable applications. Loves working with cutting-edge technologies and solving complex problems. Specializes in %s and %s.",
        "Full-stack developer specializing in modern web technologies. Experienced in agile methodologies and team leadership with %d years in the industry. Expert in %s and %s.",
        "Senior backend engineer with expertise in distributed systems and cloud architecture. %d years of experience in enterprise software development. Proficient in %s and %s.",
        "Data scientist and machine learning engineer with %d years of experience in building AI-powered solutions for various industries. Skilled in %s and %s.",
        "DevOps engineer focused on automation and infrastructure as code. %d years of experience in cloud platforms and CI/CD pipelines. Expert in %s and %s.",
        "Frontend architect with %d years of experience in building responsive and accessible web applications. Specializes in %s and %s frameworks.",
        "Database administrator and data engineer with %d years of experience in managing large-scale data systems. Expert in %s and %s technologies.",
        "Security engineer with %d years of experience in cybersecurity and secure software development. Specializes in %s and %s security practices."
    };

    /**
     * Generate a massive dataset based on complexity level
     * @param level Complexity level for the dataset
     * @return Generated user list
     */
    public static List<User> generateHugeDataset(ComplexityLevel level) {
        System.out.println("🚀 Generating HUGE dataset with complexity level: " + level.name());
        System.out.println("📊 Configuration:");
        System.out.println("  - Users: " + level.getUserCount());
        System.out.println("  - Addresses per user: " + level.getAddressCount());
        System.out.println("  - Orders per user: " + level.getOrderCount());
        System.out.println("  - Skills per user: " + level.getSkillCount());
        System.out.println("  - Languages per user: " + level.getLanguageCount());
        System.out.println("  - Social connections per user: " + level.getSocialCount());

        long startTime = System.currentTimeMillis();
        List<User> users = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < level.getUserCount(); i++) {
            if (i % 1000 == 0 && i > 0) {
                System.out.println("  ✅ Generated " + i + " users...");
            }
            users.add(generateHugeUser((long) i + 1, random, level));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("✅ Generated " + level.getUserCount() + " users in " + duration + "ms");
        System.out.println("📈 Average generation time per user: " + (duration / level.getUserCount()) + "ms");

        // Calculate approximate size
        if (!users.isEmpty()) {
            User sampleUser = users.get(0);
            System.out.println("📋 Sample user statistics:");
            System.out.println("  - Username: " + sampleUser.getUsername());
            System.out.println("  - Addresses: " + sampleUser.getAddresses().size());
            System.out.println("  - Orders: " + sampleUser.getOrders().size());
            System.out.println("  - Skills: " + sampleUser.getProfile().getSkills().size());
            System.out.println("  - Languages: " + sampleUser.getProfile().getLanguages().size());
            System.out.println("  - Social connections: " + sampleUser.getSocialConnections().size());
        }

        return users;
    }

    /**
     * Generate a single user with huge complexity
     */
    private static User generateHugeUser(Long id, Random random, ComplexityLevel level) {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String email = generateRandomEmail(firstName, lastName, random);
        String username = generateUsername(firstName, random);

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setIsActive(random.nextBoolean());
        user.setCreatedAt(generateRandomPastDate(random, 365 * 5));
        user.setLastLoginAt(generateRandomPastDate(random, 30));
        user.setLoyaltyPoints(random.nextDouble() * 10000);

        // Generate complex nested data
        user.setAddresses(generateHugeAddresses(random, level.getAddressCount()));
        user.setOrders(generateHugeOrders(random, user.getAddresses(), level.getOrderCount()));
        user.setSocialConnections(generateHugeSocialConnections(random, level.getSocialCount()));
        user.setProfile(generateHugeUserProfile(random, level));
        user.setPreferences(generateHugePreferences(random));
        user.setMetadata(generateHugeMetadata(random));
        user.setTags(generateHugeTags(random));

        return user;
    }

    private static UserProfile generateHugeUserProfile(Random random, ComplexityLevel level) {
        UserProfile profile = new UserProfile();
        profile.setBio(generateHugeBio(random));
        profile.setAvatarUrl("https://avatars.githubusercontent.com/u/" + random.nextInt(1000000));
        profile.setDateOfBirth(generateRandomPastDate(random, 365 * 50).toLocalDate());
        profile.setGender(random.nextBoolean() ? "Male" : "Female");
        profile.setPhoneNumber("+1-" + String.format("%03d", random.nextInt(1000)) + "-" + String.format("%03d", random.nextInt(1000)) + "-" + String.format("%04d", random.nextInt(10000)));
        profile.setNationality("US");
        profile.setOccupation(generateOccupation(random));
        profile.setCompany(COMPANIES[random.nextInt(COMPANIES.length)]);
        profile.setInterests(generateHugeInterests(random));
        profile.setSkills(generateHugeSkills(random, level.getSkillCount()));
        profile.setEducation(generateHugeEducation(random));
        profile.setLanguages(generateHugeLanguages(random, level.getLanguageCount()));
        return profile;
    }

    private static List<Address> generateHugeAddresses(Random random, int count) {
        List<Address> addresses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Address address = new Address();
            address.setId(random.nextLong());
            address.setType(AddressType.values()[random.nextInt(AddressType.values().length)]);
            address.setStreet1(generateStreetAddress(random));
            address.setStreet2(generateStreetAddress2(random));
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

    private static List<Order> generateHugeOrders(Random random, List<Address> addresses, int count) {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setId(random.nextLong());
            order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order.setOrderDate(generateRandomPastDate(random, 365));
            order.setStatus(OrderStatus.values()[random.nextInt(OrderStatus.values().length)]);
            order.setTotalAmount(BigDecimal.valueOf(random.nextDouble() * 10000 + 100).setScale(2, BigDecimal.ROUND_HALF_UP));
            order.setSubtotal(BigDecimal.valueOf(random.nextDouble() * 8000 + 80).setScale(2, BigDecimal.ROUND_HALF_UP));
            order.setTaxAmount(BigDecimal.valueOf(random.nextDouble() * 800 + 8).setScale(2, BigDecimal.ROUND_HALF_UP));
            order.setShippingAmount(BigDecimal.valueOf(random.nextDouble() * 200 + 10).setScale(2, BigDecimal.ROUND_HALF_UP));
            order.setDiscountAmount(BigDecimal.valueOf(random.nextDouble() * 500).setScale(2, BigDecimal.ROUND_HALF_UP));
            order.setCurrency("USD");
            order.setShippingAddress(addresses.get(random.nextInt(addresses.size())));
            order.setBillingAddress(addresses.get(random.nextInt(addresses.size())));
            order.setItems(generateHugeOrderItems(random, random.nextInt(10) + 1));
            order.setPayment(generateHugePayment(random, order.getTotalAmount()));
            order.setTracking(generateHugeTrackingEvents(random));
            order.setShippedDate(generateRandomPastDate(random, 30));
            order.setDeliveredDate(generateRandomPastDate(random, 7));
            order.setNotes(generateOrderNotes(random));
            order.setCustomFields(generateHugeOrderCustomFields(random));
            orders.add(order);
        }
        return orders;
    }

    private static List<OrderItem> generateHugeOrderItems(Random random, int count) {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            OrderItem item = new OrderItem();
            item.setId(random.nextLong());
            item.setProductId(random.nextLong());
            item.setProductName(generateProductName(random));
            item.setProductSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            item.setQuantity(random.nextInt(10) + 1);
            item.setUnitPrice(BigDecimal.valueOf(random.nextDouble() * 1000 + 10).setScale(2, BigDecimal.ROUND_HALF_UP));
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, BigDecimal.ROUND_HALF_UP));
            item.setDiscount(BigDecimal.valueOf(random.nextDouble() * 50).setScale(2, BigDecimal.ROUND_HALF_UP));
            item.setAttributes(generateProductAttributes(random));
            items.add(item);
        }
        return items;
    }

    private static Payment generateHugePayment(Random random, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setId(random.nextLong());
        payment.setMethod(PaymentMethod.values()[random.nextInt(PaymentMethod.values().length)]);
        payment.setStatus(PaymentStatus.values()[random.nextInt(PaymentStatus.values().length)]);
        payment.setAmount(amount);
        payment.setCurrency("USD");
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        payment.setCardLastFour(String.format("%04d", random.nextInt(10000)));
        payment.setCardBrand(random.nextBoolean() ? "Visa" : "Mastercard");
        payment.setProcessedAt(generateRandomPastDate(random, 30));
        payment.setGatewayResponse("SUCCESS");
        return payment;
    }

    private static List<TrackingEvent> generateHugeTrackingEvents(Random random) {
        List<TrackingEvent> events = new ArrayList<>();
        int eventCount = random.nextInt(8) + 2;
        for (int i = 0; i < eventCount; i++) {
            TrackingEvent event = new TrackingEvent();
            event.setId(random.nextLong());
            event.setStatus("TRACKING_" + (i + 1));
            event.setDescription(generateTrackingDescription("TRACKING_" + (i + 1)));
            event.setLocation(CITIES[random.nextInt(CITIES.length)]);
            event.setTimestamp(generateRandomPastDate(random, 30));
            event.setCarrier("FedEx");
            event.setTrackingNumber("TRK" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            event.setEventType(TrackingEventType.ORDER_PLACED); // Use fixed value to avoid enum issues
            events.add(event);
        }
        return events;
    }

    private static List<SocialConnection> generateHugeSocialConnections(Random random, int count) {
        List<SocialConnection> connections = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SocialConnection connection = new SocialConnection();
            connection.setId(random.nextLong());
            connection.setPlatform(SocialPlatform.values()[random.nextInt(SocialPlatform.values().length)]);
            connection.setUsername(generateUsername(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)], random));
            connection.setProfileUrl(generateProfileUrl(connection.getPlatform(), connection.getUsername()));
            connection.setIsVerified(random.nextBoolean());
            connection.setFollowerCount(random.nextLong() % 100000);
            connection.setConnectedAt(generateRandomPastDate(random, 365));
            connection.setLastSyncAt(generateRandomPastDate(random, 30));
            connection.setAdditionalData(generateSocialAdditionalData(random));
            connections.add(connection);
        }
        return connections;
    }

    private static List<String> generateHugeInterests(Random random) {
        List<String> interests = new ArrayList<>();
        String[] interestCategories = {
            "Technology", "Sports", "Music", "Travel", "Cooking", "Reading", "Gaming", "Photography",
            "Art", "Science", "History", "Politics", "Business", "Health", "Fitness", "Fashion",
            "Movies", "TV Shows", "Podcasts", "Podcasting", "Blogging", "Vlogging", "Coding", "Design"
        };

        int interestCount = random.nextInt(8) + 3;
        for (int i = 0; i < interestCount; i++) {
            interests.add(interestCategories[random.nextInt(interestCategories.length)]);
        }
        return interests;
    }

    private static List<Skill> generateHugeSkills(Random random, int count) {
        List<Skill> skills = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Skill skill = new Skill();
            skill.setId(random.nextLong());
            skill.setName(SKILLS[random.nextInt(SKILLS.length)]);
            skill.setLevel(SkillLevel.values()[random.nextInt(SkillLevel.values().length)]);
            skill.setYearsOfExperience(random.nextInt(15) + 1);
            skill.setCertifications("Certified " + SKILLS[random.nextInt(SKILLS.length)] + " Professional");
            skills.add(skill);
        }
        return skills;
    }

    private static List<Education> generateHugeEducation(Random random) {
        List<Education> education = new ArrayList<>();
        int educationCount = random.nextInt(3) + 1;
        for (int i = 0; i < educationCount; i++) {
            Education edu = new Education();
            edu.setId(random.nextLong());
            edu.setInstitution("University of " + CITIES[random.nextInt(CITIES.length)]);
            edu.setDegree("Bachelor's in Computer Science");
            edu.setFieldOfStudy("Computer Science");
            edu.setStartDate(generateRandomPastDate(random, 365 * 10).toLocalDate());
            edu.setEndDate(generateRandomPastDate(random, 365 * 5).toLocalDate());
            edu.setGpa(random.nextDouble() * 1.0 + 3.0);
            edu.setHonors(generateHonors(random));
            education.add(edu);
        }
        return education;
    }

    private static List<Language> generateHugeLanguages(Random random, int count) {
        List<Language> languages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Language language = new Language();
            language.setId(random.nextLong());
            language.setName(LANGUAGES[random.nextInt(LANGUAGES.length)]);
            language.setCode(LANGUAGES[random.nextInt(LANGUAGES.length)].substring(0, 2).toUpperCase());
            language.setProficiency(LanguageProficiency.values()[random.nextInt(LanguageProficiency.values().length)]);
            language.setIsNative(i == 0);
            languages.add(language);
        }
        return languages;
    }

    private static Map<String, Object> generateHugePreferences(Random random) {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("theme", random.nextBoolean() ? "dark" : "light");
        preferences.put("language", "en");
        preferences.put("timezone", "UTC-" + random.nextInt(12));
        preferences.put("notifications_enabled", random.nextBoolean());
        preferences.put("email_frequency", random.nextInt(3) + 1);
        preferences.put("privacy_level", random.nextInt(3) + 1);
        preferences.put("auto_save", random.nextBoolean());
        preferences.put("two_factor_auth", random.nextBoolean());
        return preferences;
    }

    private static Map<String, String> generateHugeMetadata(Random random) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "huge_payload_generator");
        metadata.put("version", "2.0");
        metadata.put("generated_at", LocalDateTime.now().toString());
        metadata.put("complexity_level", "HUGE");
        metadata.put("user_agent", "Mozilla/5.0 (compatible; HugePayloadGenerator/2.0)");
        metadata.put("ip_address", generateIpAddress(random));
        return metadata;
    }

    private static List<String> generateHugeTags(Random random) {
        List<String> tags = new ArrayList<>();
        String[] tagCategories = {
            "developer", "engineer", "architect", "lead", "senior", "junior", "fullstack", "backend", "frontend",
            "devops", "data", "ml", "ai", "cloud", "microservices", "api", "database", "security", "testing",
            "agile", "scrum", "lean", "startup", "enterprise", "fintech", "healthtech", "edtech", "ecommerce"
        };

        int tagCount = random.nextInt(6) + 2;
        for (int i = 0; i < tagCount; i++) {
            tags.add(tagCategories[random.nextInt(tagCategories.length)]);
        }
        return tags;
    }

    private static String generateHugeBio(Random random) {
        int years = random.nextInt(20) + 5;
        String skill1 = SKILLS[random.nextInt(SKILLS.length)];
        String skill2 = SKILLS[random.nextInt(SKILLS.length)];
        return String.format(BIO_TEMPLATES[random.nextInt(BIO_TEMPLATES.length)], years, skill1, skill2);
    }

    // Helper methods (reused from PayloadGenerator)
    private static String generateRandomEmail(String firstName, String lastName, Random random) {
        return firstName.toLowerCase() + "." + lastName.toLowerCase() + random.nextInt(1000) + "@example.com";
    }

    private static String generateUsername(String firstName, Random random) {
        return firstName.toLowerCase() + random.nextInt(10000);
    }

    private static LocalDateTime generateRandomPastDate(Random random, int maxDaysAgo) {
        return LocalDateTime.now().minusDays(random.nextInt(maxDaysAgo) + 1);
    }

    private static String generateStreetAddress(Random random) {
        return random.nextInt(9999) + 1 + " " + FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " St";
    }

    private static String generateStreetAddress2(Random random) {
        return random.nextBoolean() ? "Apt " + (random.nextInt(999) + 1) : null;
    }

    private static String generateState(Random random) {
        String[] states = {"CA", "TX", "FL", "NY", "PA", "IL", "OH", "GA", "NC", "MI"};
        return states[random.nextInt(states.length)];
    }

    private static String generatePostalCode(Random random) {
        return String.format("%05d", random.nextInt(90000) + 10000);
    }

    private static Double generateLatitude(Random random) {
        return 25.0 + random.nextDouble() * 25.0; // US latitude range
    }

    private static Double generateLongitude(Random random) {
        return -125.0 + random.nextDouble() * 65.0; // US longitude range
    }

    private static String generateOccupation(Random random) {
        String[] occupations = {"Software Engineer", "Senior Developer", "Tech Lead", "Architect", "DevOps Engineer", "Data Scientist", "Product Manager"};
        return occupations[random.nextInt(occupations.length)];
    }

    private static String generateProductName(Random random) {
        String[] adjectives = {"Premium", "Deluxe", "Ultra", "Pro", "Advanced", "Classic", "Modern", "Elite", "Supreme", "Master"};
        String[] products = {"Laptop", "Smartphone", "Headphones", "Tablet", "Watch", "Camera", "Speaker", "Monitor", "Keyboard", "Mouse"};
        return adjectives[random.nextInt(adjectives.length)] + " " + products[random.nextInt(products.length)];
    }

    private static String generateTrackingDescription(String status) {
        return switch (status) {
            case "TRACKING_1" -> "Order has been placed and is being processed";
            case "TRACKING_2" -> "Order is being prepared for shipment";
            case "TRACKING_3" -> "Package has been shipped and is in transit";
            case "TRACKING_4" -> "Package is on its way to destination";
            case "TRACKING_5" -> "Package is out for delivery";
            case "TRACKING_6" -> "Package has been delivered successfully";
            default -> "Status update";
        };
    }

    private static String generateProfileUrl(SocialPlatform platform, String username) {
        return switch (platform) {
            case FACEBOOK -> "https://facebook.com/" + username;
            case TWITTER -> "https://twitter.com/" + username;
            case INSTAGRAM -> "https://instagram.com/" + username;
            case LINKEDIN -> "https://linkedin.com/in/" + username;
            case GITHUB -> "https://github.com/" + username;
            case YOUTUBE -> "https://youtube.com/@" + username;
            case TIKTOK -> "https://tiktok.com/@" + username;
            case DISCORD -> "https://discord.com/users/" + username;
        };
    }

    private static String generateHonors(Random random) {
        String[] honors = {"Summa Cum Laude", "Magna Cum Laude", "Cum Laude", "Dean's List", "Honor Society", "Academic Excellence"};
        return honors[random.nextInt(honors.length)];
    }

    private static String generateOrderNotes(Random random) {
        String[] notes = {
            "Please deliver during business hours",
            "Leave package at front door if no answer",
            "Call before delivery",
            "Signature required",
            "Fragile items - handle with care",
            "Gift wrapping requested",
            "Express shipping preferred",
            "Contactless delivery preferred"
        };
        return notes[random.nextInt(notes.length)];
    }

    private static Map<String, Object> generateHugeOrderCustomFields(Random random) {
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("gift_wrapping", random.nextBoolean());
        customFields.put("express_shipping", random.nextBoolean());
        customFields.put("insurance", random.nextBoolean());
        customFields.put("priority", random.nextInt(3) + 1);
        customFields.put("special_instructions", "Handle with care");
        customFields.put("delivery_preference", random.nextBoolean() ? "morning" : "afternoon");
        return customFields;
    }

    private static String generateIpAddress(Random random) {
        return random.nextInt(255) + "." + random.nextInt(255) + "." + random.nextInt(255) + "." + random.nextInt(255);
    }

    private static Map<String, String> generateProductAttributes(Random random) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("color", random.nextBoolean() ? "Black" : "White");
        attributes.put("size", random.nextBoolean() ? "Large" : "Medium");
        attributes.put("brand", "Premium Brand");
        attributes.put("warranty", "1 Year");
        return attributes;
    }

    private static Map<String, Object> generateSocialAdditionalData(Random random) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("verified", random.nextBoolean());
        additionalData.put("followers", random.nextInt(100000));
        additionalData.put("following", random.nextInt(1000));
        additionalData.put("posts", random.nextInt(1000));
        additionalData.put("last_activity", generateRandomPastDate(random, 7).toString());
        return additionalData;
    }
}
