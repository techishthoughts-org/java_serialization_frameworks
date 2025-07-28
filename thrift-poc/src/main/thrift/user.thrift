namespace java org.techishthoughts.thrift.model

enum AddressType {
    HOME = 0,
    WORK = 1,
    BILLING = 2,
    SHIPPING = 3,
    OTHER = 4
}

enum OrderStatus {
    PENDING = 0,
    CONFIRMED = 1,
    PROCESSING = 2,
    SHIPPED = 3,
    DELIVERED = 4,
    CANCELLED = 5,
    RETURNED = 6
}

enum TrackingEventType {
    ORDER_PLACED = 0,
    ORDER_CONFIRMED = 1,
    ORDER_PROCESSING = 2,
    ORDER_SHIPPED = 3,
    ORDER_DELIVERED = 4,
    ORDER_CANCELLED = 5,
    ORDER_RETURNED = 6
}

enum PaymentMethod {
    CREDIT_CARD = 0,
    DEBIT_CARD = 1,
    PAYPAL = 2,
    BANK_TRANSFER = 3,
    CRYPTOCURRENCY = 4,
    CASH = 5
}

enum PaymentStatus {
    PENDING = 0,
    PROCESSING = 1,
    COMPLETED = 2,
    FAILED = 3,
    REFUNDED = 4,
    CANCELLED = 5
}

enum SocialPlatform {
    FACEBOOK = 0,
    TWITTER = 1,
    INSTAGRAM = 2,
    LINKEDIN = 3,
    GITHUB = 4,
    YOUTUBE = 5,
    TIKTOK = 6,
    DISCORD = 7
}

enum SkillLevel {
    BEGINNER = 0,
    INTERMEDIATE = 1,
    ADVANCED = 2,
    EXPERT = 3
}

enum LanguageProficiency {
    BASIC = 0,
    CONVERSATIONAL = 1,
    FLUENT = 2,
    NATIVE = 3
}

struct Address {
    1: required i64 id,
    2: required AddressType type,
    3: required string street1,
    4: optional string street2,
    5: required string city,
    6: required string state,
    7: required string postalCode,
    8: required string country,
    9: required double latitude,
    10: required double longitude,
    11: required bool isDefault
}

struct TrackingEvent {
    1: required i64 id,
    2: required TrackingEventType eventType,
    3: required string description,
    4: required string location,
    5: required i64 timestamp,
    6: required string status
}

struct OrderItem {
    1: required i64 id,
    2: required i64 productId,
    3: required string productName,
    4: required i32 quantity,
    5: required double unitPrice,
    6: required double totalPrice,
    7: required list<TrackingEvent> trackingEvents,
    8: optional string notes
}

struct Payment {
    1: required i64 id,
    2: required PaymentMethod method,
    3: required double amount,
    4: required string currency,
    5: required PaymentStatus status,
    6: required string transactionId,
    7: required i64 processedAt,
    8: optional string lastFourDigits,
    9: optional string expiryDate
}

struct Order {
    1: required i64 id,
    2: required string orderNumber,
    3: required OrderStatus status,
    4: required double totalAmount,
    5: required string currency,
    6: required list<OrderItem> items,
    7: required Address shippingAddress,
    8: required Address billingAddress,
    9: required Payment payment,
    10: required i64 createdAt,
    11: required i64 updatedAt,
    12: optional i64 estimatedDelivery,
    13: optional string trackingNumber,
    14: optional string notes
}

struct SocialConnection {
    1: required i64 id,
    2: required SocialPlatform platform,
    3: required string username,
    4: required string profileUrl,
    5: required bool isVerified,
    6: required i64 followerCount,
    7: required i64 connectedAt,
    8: required i64 lastSyncAt,
    9: required map<string, string> additionalData
}

struct Skill {
    1: required i64 id,
    2: required string name,
    3: required SkillLevel level,
    4: required i32 yearsOfExperience,
    5: optional string certifications
}

struct Education {
    1: required i64 id,
    2: required string institution,
    3: required string degree,
    4: required string fieldOfStudy,
    5: required i64 startDate,
    6: optional i64 endDate,
    7: required double gpa,
    8: optional string honors
}

struct Language {
    1: required i64 id,
    2: required string name,
    3: required string code,
    4: required LanguageProficiency proficiency,
    5: required bool isNative
}

struct UserProfile {
    1: optional string bio,
    2: optional string avatar,
    3: optional i64 dateOfBirth,
    4: optional string gender,
    5: optional string phoneNumber,
    6: optional string nationality,
    7: optional string occupation,
    8: optional string company,
    9: required list<string> interests,
    10: required list<Skill> skills,
    11: required list<Education> education,
    12: required list<Language> languages
}

struct User {
    1: required i64 id,
    2: required string username,
    3: required string email,
    4: required string firstName,
    5: required string lastName,
    6: required UserProfile profile,
    7: required list<Address> addresses,
    8: required list<Order> orders,
    9: required map<string, string> preferences,
    10: required map<string, string> metadata,
    11: required list<string> tags,
    12: required i64 createdAt,
    13: optional i64 lastLoginAt,
    14: required bool isActive,
    15: required double loyaltyPoints,
    16: required list<SocialConnection> socialConnections
}

struct UserList {
    1: required list<User> users
}

service UserService {
    User getUser(1: i64 userId),
    list<User> getUsers(1: list<i64> userIds, 2: i32 limit, 3: i32 offset),
    User createUser(1: User user),
    list<User> bulkCreateUsers(1: list<User> users),
    UserList getUserList(1: i32 count)
}
