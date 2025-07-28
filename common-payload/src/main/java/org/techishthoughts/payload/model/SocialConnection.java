package org.techishthoughts.payload.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import org.techishthoughts.payload.model.SocialConnection.SocialPlatform;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Social media connection information.
 */
public class SocialConnection implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("platform")
    private SocialPlatform platform;

    @JsonProperty("username")
    private String username;

    @JsonProperty("profileUrl")
    private String profileUrl;

    @JsonProperty("isVerified")
    private Boolean isVerified;

    @JsonProperty("followerCount")
    private Long followerCount;

    @JsonProperty("connectedAt")
    private LocalDateTime connectedAt;

    @JsonProperty("lastSyncAt")
    private LocalDateTime lastSyncAt;

    @JsonProperty("additionalData")
    private Map<String, Object> additionalData;

    public SocialConnection() {}

    public SocialConnection(Long id, SocialPlatform platform, String username, String profileUrl,
                           Boolean isVerified, Long followerCount, LocalDateTime connectedAt,
                           LocalDateTime lastSyncAt, Map<String, Object> additionalData) {
        this.id = id;
        this.platform = platform;
        this.username = username;
        this.profileUrl = profileUrl;
        this.isVerified = isVerified;
        this.followerCount = followerCount;
        this.connectedAt = connectedAt;
        this.lastSyncAt = lastSyncAt;
        this.additionalData = additionalData;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SocialPlatform getPlatform() { return platform; }
    public void setPlatform(SocialPlatform platform) { this.platform = platform; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public Long getFollowerCount() { return followerCount; }
    public void setFollowerCount(Long followerCount) { this.followerCount = followerCount; }

    public LocalDateTime getConnectedAt() { return connectedAt; }
    public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }

    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public Map<String, Object> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SocialConnection that = (SocialConnection) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum SocialPlatform implements Serializable {
        FACEBOOK, TWITTER, LINKEDIN, INSTAGRAM, GITHUB, YOUTUBE, TIKTOK, DISCORD
    }
}
