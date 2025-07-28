@0x8c5c8c5c8c5c8c5c;

struct User {
  id @0 :UInt64;
  username @1 :Text;
  email @2 :Text;
  firstName @3 :Text;
  lastName @4 :Text;
  profile @5 :UserProfile;
  addresses @6 :List(Address);
  orders @7 :List(Order);
  preferences @8 :List(Preference);
  metadata @9 :List(Metadata);
  tags @10 :List(Text);
  createdAt @11 :Data;
  lastLoginAt @12 :Data;
  isActive @13 :Bool;
  loyaltyPoints @14 :Float64;
  socialConnections @15 :List(SocialConnection);
}

struct UserProfile {
  bio @0 :Text;
  avatar @1 :Text;
  dateOfBirth @2 :Data;
  gender @3 :Text;
  phoneNumber @4 :Text;
  nationality @5 :Text;
  occupation @6 :Text;
  company @7 :Text;
  interests @8 :List(Text);
  skills @9 :List(Skill);
  education @10 :List(Education);
  languages @11 :List(Language);
}

struct Address {
  id @0 :UInt64;
  type @1 :AddressType;
  street1 @2 :Text;
  street2 @3 :Text;
  city @4 :Text;
  state @5 :Text;
  postalCode @6 :Text;
  country @7 :Text;
  latitude @8 :Float64;
  longitude @9 :Float64;
  isDefault @10 :Bool;
}

enum AddressType {
  home @0;
  work @1;
  billing @2;
  shipping @3;
  other @4;
}

struct Order {
  id @0 :UInt64;
  orderNumber @1 :Text;
  status @2 :OrderStatus;
  totalAmount @3 :Float64;
  currency @4 :Text;
  items @5 :List(OrderItem);
  shippingAddress @6 :Address;
  billingAddress @7 :Address;
  payment @8 :Payment;
  createdAt @9 :Data;
  updatedAt @10 :Data;
  estimatedDelivery @11 :Data;
  trackingNumber @12 :Text;
  notes @13 :Text;
}

enum OrderStatus {
  pending @0;
  confirmed @1;
  processing @2;
  shipped @3;
  delivered @4;
  cancelled @5;
  returned @6;
}

struct OrderItem {
  id @0 :UInt64;
  productId @1 :UInt64;
  productName @2 :Text;
  quantity @3 :UInt32;
  unitPrice @4 :Float64;
  totalPrice @5 :Float64;
  trackingEvents @6 :List(TrackingEvent);
  notes @7 :Text;
}

struct TrackingEvent {
  id @0 :UInt64;
  eventType @1 :TrackingEventType;
  description @2 :Text;
  location @3 :Text;
  timestamp @4 :Data;
  status @5 :Text;
}

enum TrackingEventType {
  orderPlaced @0;
  orderConfirmed @1;
  orderProcessing @2;
  orderShipped @3;
  orderDelivered @4;
  orderCancelled @5;
  orderReturned @6;
}

struct Payment {
  id @0 :UInt64;
  method @1 :PaymentMethod;
  amount @2 :Float64;
  currency @3 :Text;
  status @4 :PaymentStatus;
  transactionId @5 :Text;
  processedAt @6 :Data;
  lastFourDigits @7 :Text;
  expiryDate @8 :Text;
}

enum PaymentMethod {
  creditCard @0;
  debitCard @1;
  paypal @2;
  bankTransfer @3;
  cryptocurrency @4;
  cash @5;
}

enum PaymentStatus {
  pending @0;
  processing @1;
  completed @2;
  failed @3;
  refunded @4;
  cancelled @5;
}

struct SocialConnection {
  id @0 :UInt64;
  platform @1 :SocialPlatform;
  username @2 :Text;
  profileUrl @3 :Text;
  isVerified @4 :Bool;
  followerCount @5 :UInt64;
  connectedAt @6 :Data;
  lastSyncAt @7 :Data;
  additionalData @8 :List(AdditionalData);
}

enum SocialPlatform {
  facebook @0;
  twitter @1;
  instagram @2;
  linkedin @3;
  github @4;
  youtube @5;
  tiktok @6;
  discord @7;
}

struct Skill {
  id @0 :UInt64;
  name @1 :Text;
  level @2 :SkillLevel;
  yearsOfExperience @3 :UInt32;
  certifications @4 :Text;
}

enum SkillLevel {
  beginner @0;
  intermediate @1;
  advanced @2;
  expert @3;
}

struct Education {
  id @0 :UInt64;
  institution @1 :Text;
  degree @2 :Text;
  fieldOfStudy @3 :Text;
  startDate @4 :Data;
  endDate @5 :Data;
  gpa @6 :Float64;
  honors @7 :Text;
}

struct Language {
  id @0 :UInt64;
  name @1 :Text;
  code @2 :Text;
  proficiency @3 :LanguageProficiency;
  isNative @4 :Bool;
}

enum LanguageProficiency {
  basic @0;
  conversational @1;
  fluent @2;
  native @3;
}

struct Preference {
  key @0 :Text;
  value @1 :Text;
}

struct Metadata {
  key @0 :Text;
  value @1 :Text;
}

struct AdditionalData {
  key @0 :Text;
  value @1 :Text;
}

struct UserList {
  users @0 :List(User);
}
