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
@Table(name = "property_images")
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PropertyImage() {
    }

    public PropertyImage(Property property, String imageUrl, boolean primary) {
        this.property = property;
        this.imageUrl = imageUrl;
        this.primary = primary;
    }

    public Long getId() {
        return id;
    }

    public Property getProperty() {
        return property;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void markPrimary() {
        this.primary = true;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
