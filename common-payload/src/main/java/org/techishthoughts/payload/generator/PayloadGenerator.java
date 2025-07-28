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
 * Generates large, complex payload data for stress-testing serialization frameworks.
 * Creates realistic user data with deep object graphs and collections.
 */
public class PayloadGenerator {

    private static final String[] FIRST_NAMES = {
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Christopher", "Karen", "Charles", "Nancy", "Daniel", "Lisa",
        "Matthew", "Betty", "Anthony", "Dorothy", "Mark", "Sandra", "Donald", "Donna"
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
        "Mastercard Inc.", "Bank of America Corp", "The Home Depot Inc.", "Pfizer Inc.",
        "Chevron Corporation", "AbbVie Inc.", "Coca-Cola Company", "PepsiCo Inc.", "Walmart Inc.",
        "Walt Disney Company", "Comcast Corporation", "Verizon Communications", "AT&T Inc.",
        "Netflix Inc.", "Adobe Inc.", "Intel Corporation", "Cisco Systems Inc.", "Oracle Corporation"
    };

    private static final String[] SKILLS = {
        "Java", "Python", "JavaScript", "C++", "C#", "Go", "Rust", "Kotlin", "Swift", "TypeScript",
        "React", "Angular", "Vue.js", "Spring Boot", "Django", "Flask", "Node.js", "Express.js",
        "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Jenkins", "GitLab CI", "GitHub Actions",
        "PostgreSQL", "MongoDB", "Redis", "Elasticsearch", "Apache Kafka", "RabbitMQ",
        "Machine Learning", "Deep Learning", "Data Science", "DevOps", "Microservices", "GraphQL"
    };

    private static final String[] BIO_TEMPLATES = {
        "Passionate software engineer with %d years of experience in building scalable applications. Loves working with cutting-edge technologies and solving complex problems.",
        "Full-stack developer specializing in modern web technologies. Experienced in agile methodologies and team leadership with %d years in the industry.",
        "Senior backend engineer with expertise in distributed systems and cloud architecture. %d years of experience in enterprise software development.",
        "Data scientist and machine learning engineer with %d years of experience in building AI-powered solutions for various industries.",
        "DevOps engineer focused on automation and infrastructure as code. %d years of experience in cloud platforms and CI/CD pipelines."
    };

    public static List<User> generateUsers(int count) {
        List<User> users = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            users.add(generateUser((long) i + 1, random));
        }

        return users;
    }

    private static User generateUser(Long id, Random random) {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String username = (firstName + lastName + random.nextInt(1000)).toLowerCase();
        String email = username + "@" + generateRandomEmail(random);

        // Generate complex user profile
        UserProfile profile = generateUserProfile(random);

        // Generate multiple addresses
        List<Address> addresses = generateAddresses(random);

        // Generate order history (0-10 orders per user)
        List<Order> orders = generateOrders(random, addresses);

        // Generate preferences map
        Map<String, Object> preferences = generatePreferences(random);

        // Generate metadata
        Map<String, String> metadata = generateMetadata(random);

        // Generate tags
        List<String> tags = generateTags(random);

        // Generate social connections
        List<SocialConnection> socialConnections = generateSocialConnections(random);

        LocalDateTime createdAt = generateRandomPastDate(random, 730); // Up to 2 years ago
        LocalDateTime lastLoginAt = generateRandomPastDate(random, 30); // Up to 30 days ago

        return new User(
            id, username, email, firstName, lastName, profile, addresses, orders,
            preferences, metadata, tags, createdAt, lastLoginAt,
            random.nextBoolean(), random.nextDouble() * 10000, socialConnections
        );
    }

    private static UserProfile generateUserProfile(Random random) {
        String bio = String.format(BIO_TEMPLATES[random.nextInt(BIO_TEMPLATES.length)],
                                 random.nextInt(15) + 1);

        LocalDate dateOfBirth = LocalDate.now().minusYears(random.nextInt(50) + 18);
        String[] genders = {"Male", "Female", "Non-binary", "Prefer not to say"};
        String[] nationalities = {"American", "Canadian", "British", "German", "French", "Japanese", "Indian", "Australian"};

        List<String> interests = generateInterests(random);
        List<Skill> skills = generateSkills(random);
        List<Education> education = generateEducation(random);
        List<Language> languages = generateLanguages(random);

        return new UserProfile(
            bio,
            "https://api.dicebear.com/7.x/avataaars/svg?seed=" + random.nextInt(10000),
            dateOfBirth,
            genders[random.nextInt(genders.length)],
            generatePhoneNumber(random),
            nationalities[random.nextInt(nationalities.length)],
            generateOccupation(random),
            COMPANIES[random.nextInt(COMPANIES.length)],
            interests,
            skills,
            education,
            languages
        );
    }

    private static List<Address> generateAddresses(Random random) {
        int addressCount = random.nextInt(3) + 1; // 1-3 addresses
        List<Address> addresses = new ArrayList<>();
        AddressType[] types = AddressType.values();

        for (int i = 0; i < addressCount; i++) {
            addresses.add(new Address(
                (long) i + 1,
                types[random.nextInt(types.length)],
                generateStreetAddress(random),
                random.nextBoolean() ? generateStreetAddress2(random) : null,
                CITIES[random.nextInt(CITIES.length)],
                generateState(random),
                generatePostalCode(random),
                "United States",
                generateLatitude(random),
                generateLongitude(random),
                i == 0 // First address is default
            ));
        }

        return addresses;
    }

    private static List<Order> generateOrders(Random random, List<Address> addresses) {
        int orderCount = random.nextInt(11); // 0-10 orders
        List<Order> orders = new ArrayList<>();
        OrderStatus[] statuses = OrderStatus.values();

        for (int i = 0; i < orderCount; i++) {
            List<OrderItem> items = generateOrderItems(random);
            BigDecimal subtotal = calculateSubtotal(items);
            BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(0.08)); // 8% tax
            BigDecimal shippingAmount = BigDecimal.valueOf(random.nextDouble() * 20 + 5); // $5-25
            BigDecimal discountAmount = random.nextBoolean() ?
                subtotal.multiply(BigDecimal.valueOf(0.1)) : BigDecimal.ZERO; // 10% discount or none
            BigDecimal totalAmount = subtotal.add(taxAmount).add(shippingAmount).subtract(discountAmount);

            Address shippingAddress = addresses.get(random.nextInt(addresses.size()));
            Address billingAddress = addresses.get(random.nextInt(addresses.size()));

            Payment payment = generatePayment(random, totalAmount);
            // List<TrackingEvent> tracking = generateTrackingEvents(random);
            List<TrackingEvent> tracking = new ArrayList<>(); // Temporarily disable tracking events

            LocalDateTime orderDate = generateRandomPastDate(random, 365);
            LocalDateTime shippedDate = orderDate.plusDays(random.nextInt(3) + 1);
            LocalDateTime deliveredDate = shippedDate.plusDays(random.nextInt(7) + 1);

            orders.add(new Order(
                (long) i + 1,
                "ORD" + String.format("%08d", random.nextInt(100000000)),
                statuses[random.nextInt(statuses.length)],
                items,
                totalAmount,
                subtotal,
                taxAmount,
                shippingAmount,
                discountAmount,
                "USD",
                shippingAddress,
                billingAddress,
                payment,
                tracking,
                orderDate,
                shippedDate,
                deliveredDate,
                generateOrderNotes(random),
                generateOrderCustomFields(random)
            ));
        }

        return orders;
    }

    private static List<OrderItem> generateOrderItems(Random random) {
        int itemCount = random.nextInt(5) + 1; // 1-5 items per order
        List<OrderItem> items = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            int quantity = random.nextInt(3) + 1;
            BigDecimal unitPrice = BigDecimal.valueOf(random.nextDouble() * 200 + 10); // $10-210
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal discount = random.nextBoolean() ?
                totalPrice.multiply(BigDecimal.valueOf(0.05)) : BigDecimal.ZERO; // 5% discount or none

            Map<String, String> attributes = new HashMap<>();
            attributes.put("color", generateRandomColor(random));
            attributes.put("size", generateRandomSize(random));
            attributes.put("material", generateRandomMaterial(random));

            items.add(new OrderItem(
                (long) i + 1,
                (long) random.nextInt(10000) + 1,
                generateProductName(random),
                "SKU" + String.format("%08d", random.nextInt(100000000)),
                quantity,
                unitPrice,
                totalPrice,
                discount,
                attributes
            ));
        }

        return items;
    }

    private static Payment generatePayment(Random random, BigDecimal amount) {
        PaymentMethod[] methods = PaymentMethod.values();
        PaymentStatus[] statuses = PaymentStatus.values();

        return new Payment(
            1L,
            methods[random.nextInt(methods.length)],
            statuses[random.nextInt(statuses.length)],
            amount,
            "USD",
            "txn_" + UUID.randomUUID().toString().substring(0, 8),
            String.format("%04d", random.nextInt(10000)),
            generateCardBrand(random),
            LocalDateTime.now().minusHours(random.nextInt(24)),
            "SUCCESS"
        );
    }

    private static List<TrackingEvent> generateTrackingEvents(Random random) {
        // Temporarily return empty list to avoid enum issues
        return new ArrayList<>();
    }

    private static List<SocialConnection> generateSocialConnections(Random random) {
        int connectionCount = random.nextInt(4); // 0-3 connections
        List<SocialConnection> connections = new ArrayList<>();
        SocialPlatform[] platforms = SocialPlatform.values();

        for (int i = 0; i < connectionCount; i++) {
            SocialPlatform platform = platforms[random.nextInt(platforms.length)];
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("posts", random.nextInt(1000));
            additionalData.put("likes", random.nextInt(10000));
            additionalData.put("engagement_rate", random.nextDouble() * 10);

            connections.add(new SocialConnection(
                (long) i + 1,
                platform,
                generateUsername(random),
                generateProfileUrl(platform, generateUsername(random)),
                random.nextBoolean(),
                (long) random.nextInt(100000),
                generateRandomPastDate(random, 1095), // Up to 3 years ago
                generateRandomPastDate(random, 7), // Up to 7 days ago
                additionalData
            ));
        }

        return connections;
    }

    private static List<String> generateInterests(Random random) {
        String[] allInterests = {
            "Technology", "Travel", "Photography", "Music", "Reading", "Cooking", "Fitness",
            "Gaming", "Art", "Sports", "Movies", "Nature", "Science", "History", "Fashion",
            "Food", "Cars", "Books", "Hiking", "Swimming", "Cycling", "Yoga", "Meditation"
        };

        int count = random.nextInt(8) + 3; // 3-10 interests
        Set<String> selected = new HashSet<>();
        while (selected.size() < count) {
            selected.add(allInterests[random.nextInt(allInterests.length)]);
        }

        return new ArrayList<>(selected);
    }

    private static List<Skill> generateSkills(Random random) {
        int skillCount = random.nextInt(10) + 5; // 5-14 skills
        List<Skill> skills = new ArrayList<>();
        SkillLevel[] levels = SkillLevel.values();

        Set<String> selectedSkills = new HashSet<>();
        while (selectedSkills.size() < skillCount) {
            selectedSkills.add(SKILLS[random.nextInt(SKILLS.length)]);
        }

        int i = 1;
        for (String skillName : selectedSkills) {
            skills.add(new Skill(
                (long) i++,
                skillName,
                levels[random.nextInt(levels.length)],
                random.nextInt(15) + 1,
                random.nextBoolean() ? generateCertification(skillName, random) : null
            ));
        }

        return skills;
    }

    private static List<Education> generateEducation(Random random) {
        int educationCount = random.nextInt(3) + 1; // 1-3 education entries
        List<Education> educationList = new ArrayList<>();

        String[] institutions = {
            "Harvard University", "MIT", "Stanford University", "University of California Berkeley",
            "Carnegie Mellon University", "Georgia Tech", "University of Washington",
            "Princeton University", "Yale University", "Columbia University"
        };

        String[] degrees = {"Bachelor's", "Master's", "PhD", "Associate"};
        String[] fields = {
            "Computer Science", "Software Engineering", "Data Science", "Information Technology",
            "Electrical Engineering", "Mathematics", "Business Administration", "Economics"
        };

        for (int i = 0; i < educationCount; i++) {
            LocalDate startDate = LocalDate.now().minusYears(random.nextInt(20) + 4);
            LocalDate endDate = startDate.plusYears(random.nextInt(6) + 2);

            educationList.add(new Education(
                (long) i + 1,
                institutions[random.nextInt(institutions.length)],
                degrees[random.nextInt(degrees.length)],
                fields[random.nextInt(fields.length)],
                startDate,
                endDate,
                2.0 + random.nextDouble() * 2.0, // GPA between 2.0 and 4.0
                random.nextBoolean() ? generateHonors(random) : null
            ));
        }

        return educationList;
    }

    private static List<Language> generateLanguages(Random random) {
        String[][] languages = {
            {"English", "en"}, {"Spanish", "es"}, {"French", "fr"}, {"German", "de"},
            {"Italian", "it"}, {"Portuguese", "pt"}, {"Russian", "ru"}, {"Chinese", "zh"},
            {"Japanese", "ja"}, {"Korean", "ko"}, {"Arabic", "ar"}, {"Hindi", "hi"}
        };

        int languageCount = random.nextInt(4) + 1; // 1-4 languages
        List<Language> languageList = new ArrayList<>();
        LanguageProficiency[] proficiencies = LanguageProficiency.values();

        Set<Integer> selectedIndices = new HashSet<>();
        while (selectedIndices.size() < languageCount) {
            selectedIndices.add(random.nextInt(languages.length));
        }

        int i = 1;
        for (Integer index : selectedIndices) {
            String[] lang = languages[index];
            languageList.add(new Language(
                (long) i++,
                lang[0],
                lang[1],
                proficiencies[random.nextInt(proficiencies.length)],
                i == 1 && lang[0].equals("English") // First language and English = native
            ));
        }

        return languageList;
    }

    // Helper methods for generating realistic data
    private static String generateRandomEmail(Random random) {
        String[] domains = {"gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "company.com"};
        return domains[random.nextInt(domains.length)];
    }

    private static String generatePhoneNumber(Random random) {
        return String.format("+1-%03d-%03d-%04d",
            random.nextInt(900) + 100,
            random.nextInt(900) + 100,
            random.nextInt(9000) + 1000);
    }

    private static String generateOccupation(Random random) {
        String[] occupations = {
            "Software Engineer", "Data Scientist", "Product Manager", "DevOps Engineer",
            "UX Designer", "Backend Developer", "Frontend Developer", "Full Stack Developer",
            "Machine Learning Engineer", "Cloud Architect", "Security Engineer", "QA Engineer"
        };
        return occupations[random.nextInt(occupations.length)];
    }

    private static String generateStreetAddress(Random random) {
        return (random.nextInt(9999) + 1) + " " +
               LAST_NAMES[random.nextInt(LAST_NAMES.length)] + " " +
               (random.nextBoolean() ? "Street" : "Avenue");
    }

    private static String generateStreetAddress2(Random random) {
        String[] types = {"Apt", "Suite", "Unit", "Floor"};
        return types[random.nextInt(types.length)] + " " + (random.nextInt(999) + 1);
    }

    private static String generateState(Random random) {
        String[] states = {"CA", "NY", "TX", "FL", "IL", "PA", "OH", "GA", "NC", "MI"};
        return states[random.nextInt(states.length)];
    }

    private static String generatePostalCode(Random random) {
        return String.format("%05d", random.nextInt(100000));
    }

    private static Double generateLatitude(Random random) {
        return (random.nextDouble() - 0.5) * 180; // -90 to 90
    }

    private static Double generateLongitude(Random random) {
        return (random.nextDouble() - 0.5) * 360; // -180 to 180
    }

    private static LocalDateTime generateRandomPastDate(Random random, int maxDaysAgo) {
        return LocalDateTime.now().minusDays(random.nextInt(maxDaysAgo));
    }

    private static Map<String, Object> generatePreferences(Random random) {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("theme", random.nextBoolean() ? "dark" : "light");
        preferences.put("notifications", random.nextBoolean());
        preferences.put("language", "en");
        preferences.put("timezone", "UTC-" + (random.nextInt(12) + 1));
        preferences.put("currency", "USD");
        preferences.put("newsletter", random.nextBoolean());
        preferences.put("privacy_level", random.nextInt(3) + 1);
        return preferences;
    }

    private static Map<String, String> generateMetadata(Random random) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "web");
        metadata.put("campaign", "campaign_" + random.nextInt(10));
        metadata.put("referrer", "google.com");
        metadata.put("user_agent", "Mozilla/5.0 (compatible)");
        metadata.put("ip_address", generateIpAddress(random));
        return metadata;
    }

    private static List<String> generateTags(Random random) {
        String[] allTags = {
            "premium", "beta_user", "early_adopter", "power_user", "enterprise",
            "developer", "designer", "manager", "student", "freelancer"
        };

        int tagCount = random.nextInt(5) + 1; // 1-5 tags
        List<String> tags = new ArrayList<>();
        Set<String> selected = new HashSet<>();

        while (selected.size() < tagCount) {
            selected.add(allTags[random.nextInt(allTags.length)]);
        }

        tags.addAll(selected);
        return tags;
    }

    private static String generateIpAddress(Random random) {
        return String.format("%d.%d.%d.%d",
            random.nextInt(256), random.nextInt(256),
            random.nextInt(256), random.nextInt(256));
    }

    private static BigDecimal calculateSubtotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String generateOrderNotes(Random random) {
        String[] notes = {
            "Please leave at front door",
            "Call upon arrival",
            "Handle with care - fragile items",
            "Delivery between 9 AM - 5 PM only",
            "Ring doorbell twice"
        };
        return random.nextBoolean() ? notes[random.nextInt(notes.length)] : null;
    }

    private static Map<String, Object> generateOrderCustomFields(Random random) {
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("gift_wrapping", random.nextBoolean());
        customFields.put("express_shipping", random.nextBoolean());
        customFields.put("insurance", random.nextBoolean());
        customFields.put("priority", random.nextInt(3) + 1);
        return customFields;
    }

    private static String generateProductName(Random random) {
        String[] adjectives = {"Premium", "Deluxe", "Ultra", "Pro", "Advanced", "Classic", "Modern"};
        String[] products = {"Laptop", "Smartphone", "Headphones", "Tablet", "Watch", "Camera", "Speaker"};
        return adjectives[random.nextInt(adjectives.length)] + " " +
               products[random.nextInt(products.length)];
    }

    private static String generateRandomColor(Random random) {
        String[] colors = {"Black", "White", "Red", "Blue", "Green", "Yellow", "Purple", "Orange"};
        return colors[random.nextInt(colors.length)];
    }

    private static String generateRandomSize(Random random) {
        String[] sizes = {"XS", "S", "M", "L", "XL", "XXL"};
        return sizes[random.nextInt(sizes.length)];
    }

    private static String generateRandomMaterial(Random random) {
        String[] materials = {"Cotton", "Polyester", "Leather", "Metal", "Plastic", "Wood", "Glass"};
        return materials[random.nextInt(materials.length)];
    }

    private static String generateCardBrand(Random random) {
        String[] brands = {"Visa", "Mastercard", "American Express", "Discover"};
        return brands[random.nextInt(brands.length)];
    }

    private static String generateTrackingDescription(String status) {
        return switch (status) {
            case "ORDER_PLACED" -> "Order has been placed and is being processed";
            case "PROCESSING" -> "Order is being prepared for shipment";
            case "SHIPPED" -> "Package has been shipped and is in transit";
            case "IN_TRANSIT" -> "Package is on its way to destination";
            case "OUT_FOR_DELIVERY" -> "Package is out for delivery";
            case "DELIVERED" -> "Package has been delivered successfully";
            default -> "Status update";
        };
    }

    private static String generateUsername(Random random) {
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)].toLowerCase() +
               random.nextInt(1000);
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

    private static String generateCertification(String skillName, Random random) {
        return skillName + " Certified Professional " + (2020 + random.nextInt(5));
    }

    private static String generateHonors(Random random) {
        String[] honors = {"Summa Cum Laude", "Magna Cum Laude", "Cum Laude", "Dean's List"};
        return honors[random.nextInt(honors.length)];
    }

    /**
     * Generate a massive dataset for performance testing
     * @param userCount Number of users to generate
     * @return Generated user list
     */
    public static List<User> generateMassiveDataset(int userCount) {
        System.out.println("Generating " + userCount + " users with complex nested data...");
        long startTime = System.currentTimeMillis();

        List<User> users = generateUsers(userCount);

        long endTime = System.currentTimeMillis();
        System.out.println("Generated " + userCount + " users in " + (endTime - startTime) + "ms");

        // Calculate approximate size
        User sampleUser = users.get(0);
        System.out.println("Sample user: " + sampleUser.getUsername());
        System.out.println("  - " + sampleUser.getAddresses().size() + " addresses");
        System.out.println("  - " + sampleUser.getOrders().size() + " orders");
        System.out.println("  - " + sampleUser.getProfile().getSkills().size() + " skills");
        System.out.println("  - " + sampleUser.getSocialConnections().size() + " social connections");

        return users;
    }
}
