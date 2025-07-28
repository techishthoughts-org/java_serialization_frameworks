package org.techishthoughts.payload.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import org.techishthoughts.payload.model.TrackingEvent.TrackingEventType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Order tracking event with type and metadata.
 */
public class TrackingEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("description")
    private String description;

    @JsonProperty("location")
    private String location;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("carrier")
    private String carrier;

    @JsonProperty("trackingNumber")
    private String trackingNumber;

    @JsonProperty("eventType")
    private TrackingEventType eventType;

    public enum TrackingEventType implements Serializable {
        ORDER_PLACED,
        ORDER_CONFIRMED,
        ORDER_PROCESSING,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_CANCELLED,
        ORDER_RETURNED
    }

    public TrackingEvent() {}

    public TrackingEvent(Long id, String status, String description, String location,
                        LocalDateTime timestamp, String carrier, String trackingNumber, TrackingEventType eventType) {
        this.id = id;
        this.status = status;
        this.description = description;
        this.location = location;
        this.timestamp = timestamp;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.eventType = eventType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public TrackingEventType getEventType() { return eventType; }
    public void setEventType(TrackingEventType eventType) { this.eventType = eventType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingEvent that = (TrackingEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
