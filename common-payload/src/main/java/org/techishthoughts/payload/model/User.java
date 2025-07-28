package org.techishthoughts.payload.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Complex User model designed to stress-test serialization frameworks.
 * Contains nested objects, collections, and various data types.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("profile")
    private UserProfile profile;

    @JsonProperty("addresses")
    private List<Address> addresses;

    @JsonProperty("orders")
    private List<Order> orders;

    @JsonProperty("preferences")
    private Map<String, Object> preferences;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("lastLoginAt")
    private LocalDateTime lastLoginAt;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("loyaltyPoints")
    private Double loyaltyPoints;

    @JsonProperty("socialConnections")
    private List<SocialConnection> socialConnections;

    // Default constructor
    public User() {}

    // Constructor with all fields
    public User(Long id, String username, String email, String firstName, String lastName,
                UserProfile profile, List<Address> addresses, List<Order> orders,
                Map<String, Object> preferences, Map<String, String> metadata,
                List<String> tags, LocalDateTime createdAt, LocalDateTime lastLoginAt,
                Boolean isActive, Double loyaltyPoints, List<SocialConnection> socialConnections) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profile = profile;
        this.addresses = addresses;
        this.orders = orders;
        this.preferences = preferences;
        this.metadata = metadata;
        this.tags = tags;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.isActive = isActive;
        this.loyaltyPoints = loyaltyPoints;
        this.socialConnections = socialConnections;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }

    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }

    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Double getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(Double loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    public List<SocialConnection> getSocialConnections() { return socialConnections; }
    public void setSocialConnections(List<SocialConnection> socialConnections) { this.socialConnections = socialConnections; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
               Objects.equals(username, user.username) &&
               Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", isActive=" + isActive +
                ", loyaltyPoints=" + loyaltyPoints +
                '}';
    }
}
