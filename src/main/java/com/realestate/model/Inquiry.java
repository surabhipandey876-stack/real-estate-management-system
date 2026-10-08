package com.realestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 30)
    private String status = "OPEN";

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Inquiry() {
    }

    public Inquiry(User customer, Property property, String message) {
        this.customer = customer;
        this.property = property;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public User getCustomer() {
        return customer;
    }

    public Property getProperty() {
        return property;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void reply(String responseMessage) {
        this.responseMessage = responseMessage;
        this.respondedAt = LocalDateTime.now();
        this.status = "ANSWERED";
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
