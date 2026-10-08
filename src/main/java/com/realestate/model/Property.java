package com.realestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "property_type", nullable = false, length = 50)
    private String propertyType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    private Integer bedrooms;

    private Integer bathrooms;

    @Column(name = "area_sqft")
    private Integer areaSqft;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE";

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private PropertyApprovalStatus approvalStatus = PropertyApprovalStatus.APPROVED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Property() {
    }

    public Property(
            String title,
            String description,
            String propertyType,
            BigDecimal price,
            Integer bedrooms,
            Integer bathrooms,
            Integer areaSqft,
            String city,
            String address,
            String status,
            User agent
    ) {
        this(
                title,
                description,
                propertyType,
                price,
                bedrooms,
                bathrooms,
                areaSqft,
                city,
                address,
                status,
                agent,
                PropertyApprovalStatus.APPROVED
        );
    }

    public Property(
            String title,
            String description,
            String propertyType,
            BigDecimal price,
            Integer bedrooms,
            Integer bathrooms,
            Integer areaSqft,
            String city,
            String address,
            String status,
            User agent,
            PropertyApprovalStatus approvalStatus
    ) {
        this.title = title;
        this.description = description;
        this.propertyType = propertyType;
        this.price = price;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.areaSqft = areaSqft;
        this.city = city;
        this.address = address;
        this.status = status;
        this.agent = agent;
        this.approvalStatus = approvalStatus;
    }

    public void updateDetails(
            String title,
            String description,
            String propertyType,
            BigDecimal price,
            Integer bedrooms,
            Integer bathrooms,
            Integer areaSqft,
            String city,
            String address,
            String status
    ) {
        this.title = title;
        this.description = description;
        this.propertyType = propertyType;
        this.price = price;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.areaSqft = areaSqft;
        this.city = city;
        this.address = address;
        this.status = status;
    }

    public void assignAgent(User agent) {
        this.agent = agent;
    }

    public void setApprovalStatus(PropertyApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public Integer getAreaSqft() {
        return areaSqft;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return status;
    }

    public PropertyApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public User getAgent() {
        return agent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
