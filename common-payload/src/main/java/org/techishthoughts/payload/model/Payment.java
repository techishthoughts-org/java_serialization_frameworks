package org.techishthoughts.payload.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import org.techishthoughts.payload.model.Payment.PaymentMethod;
import org.techishthoughts.payload.model.Payment.PaymentStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment information with various methods and status tracking.
 */
public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("method")
    private PaymentMethod method;

    @JsonProperty("status")
    private PaymentStatus status;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("cardLastFour")
    private String cardLastFour;

    @JsonProperty("cardBrand")
    private String cardBrand;

    @JsonProperty("processedAt")
    private LocalDateTime processedAt;

    @JsonProperty("gatewayResponse")
    private String gatewayResponse;

    public Payment() {}

    public Payment(Long id, PaymentMethod method, PaymentStatus status, BigDecimal amount,
                  String currency, String transactionId, String cardLastFour, String cardBrand,
                  LocalDateTime processedAt, String gatewayResponse) {
        this.id = id;
        this.method = method;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.transactionId = transactionId;
        this.cardLastFour = cardLastFour;
        this.cardBrand = cardBrand;
        this.processedAt = processedAt;
        this.gatewayResponse = gatewayResponse;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }

    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum PaymentMethod implements Serializable {
        CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, CRYPTOCURRENCY, CASH_ON_DELIVERY
    }

    public enum PaymentStatus implements Serializable {
        PENDING, AUTHORIZED, CAPTURED, DECLINED, REFUNDED, CANCELLED
    }
}
