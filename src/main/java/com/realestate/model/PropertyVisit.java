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
@Table(name = "property_visits")
public class PropertyVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PropertyVisit() {
    }

    public PropertyVisit(Property property, User customer, LocalDateTime scheduledAt) {
        this.property = property;
        this.customer = customer;
        this.agent = property.getAgent();
        this.scheduledAt = scheduledAt;
    }

    public void cancel() {
        this.status = "CANCELLED";
    }

    public void decide(PropertyVisitStatus decision) {
        if (decision != PropertyVisitStatus.ACCEPTED && decision != PropertyVisitStatus.REJECTED) {
            throw new IllegalArgumentException("A visit decision must be ACCEPTED or REJECTED.");
        }
        this.status = decision.name();
    }

    public Long getId() {
        return id;
    }

    public Property getProperty() {
        return property;
    }

    public User getCustomer() {
        return customer;
    }

    public User getAgent() {
        return agent;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
