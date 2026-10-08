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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorites_customer_property",
                columnNames = {"customer_id", "property_id"}
        )
)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Favorite() {
    }

    public Favorite(User customer, Property property) {
        this.customer = customer;
        this.property = property;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
