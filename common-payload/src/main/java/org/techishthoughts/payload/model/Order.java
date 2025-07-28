package org.techishthoughts.payload.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.techishthoughts.payload.model.Order.OrderStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Order information with complex nested data and financial calculations.
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("status")
    private OrderStatus status;

    @JsonProperty("items")
    private List<OrderItem> items;

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    @JsonProperty("taxAmount")
    private BigDecimal taxAmount;

    @JsonProperty("shippingAmount")
    private BigDecimal shippingAmount;

    @JsonProperty("discountAmount")
    private BigDecimal discountAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("shippingAddress")
    private Address shippingAddress;

    @JsonProperty("billingAddress")
    private Address billingAddress;

    @JsonProperty("payment")
    private Payment payment;

    @JsonProperty("tracking")
    private List<TrackingEvent> tracking;

    @JsonProperty("orderDate")
    private LocalDateTime orderDate;

    @JsonProperty("shippedDate")
    private LocalDateTime shippedDate;

    @JsonProperty("deliveredDate")
    private LocalDateTime deliveredDate;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("customFields")
    private Map<String, Object> customFields;

    public Order() {}

    public Order(Long id, String orderNumber, OrderStatus status, List<OrderItem> items,
                BigDecimal totalAmount, BigDecimal subtotal, BigDecimal taxAmount,
                BigDecimal shippingAmount, BigDecimal discountAmount, String currency,
                Address shippingAddress, Address billingAddress, Payment payment,
                List<TrackingEvent> tracking, LocalDateTime orderDate, LocalDateTime shippedDate,
                LocalDateTime deliveredDate, String notes, Map<String, Object> customFields) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.items = items;
        this.totalAmount = totalAmount;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.shippingAmount = shippingAmount;
        this.discountAmount = discountAmount;
        this.currency = currency;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.payment = payment;
        this.tracking = tracking;
        this.orderDate = orderDate;
        this.shippedDate = shippedDate;
        this.deliveredDate = deliveredDate;
        this.notes = notes;
        this.customFields = customFields;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }

    public Address getBillingAddress() { return billingAddress; }
    public void setBillingAddress(Address billingAddress) { this.billingAddress = billingAddress; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public List<TrackingEvent> getTracking() { return tracking; }
    public void setTracking(List<TrackingEvent> tracking) { this.tracking = tracking; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public LocalDateTime getShippedDate() { return shippedDate; }
    public void setShippedDate(LocalDateTime shippedDate) { this.shippedDate = shippedDate; }

    public LocalDateTime getDeliveredDate() { return deliveredDate; }
    public void setDeliveredDate(LocalDateTime deliveredDate) { this.deliveredDate = deliveredDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Map<String, Object> getCustomFields() { return customFields; }
    public void setCustomFields(Map<String, Object> customFields) { this.customFields = customFields; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id) && Objects.equals(orderNumber, order.orderNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderNumber);
    }

    public enum OrderStatus implements Serializable {
        PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED
    }
}
