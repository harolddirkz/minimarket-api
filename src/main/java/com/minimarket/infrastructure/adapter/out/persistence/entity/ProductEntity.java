package com.minimarket.infrastructure.adapter.out.persistence.entity;

import com.minimarket.domain.model.UnitOfMeasure;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 64)
    private String barcode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(length = 30)
    private String presentation;

    @Column(name = "packaging_material", length = 30)
    private String packagingMaterial;

    @Column(name = "content_quantity", precision = 12, scale = 3)
    private BigDecimal contentQuantity;

    @Column(name = "content_unit", length = 10)
    private String contentUnit;

    @Column(length = 60)
    private String variant;

    @Column(name = "units_per_package")
    private Integer unitsPerPackage;

    @Column(name = "unit_content_quantity", precision = 12, scale = 3)
    private BigDecimal unitContentQuantity;

    @Column(name = "unit_content_unit", length = 10)
    private String unitContentUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false, length = 10)
    private UnitOfMeasure unitOfMeasure;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductEntity() {
    }

    public ProductEntity(Long id, String name, String description, String barcode, BigDecimal price,
            BigDecimal purchasePrice, String presentation, String packagingMaterial, BigDecimal contentQuantity, String contentUnit,
            String variant, Integer unitsPerPackage, BigDecimal unitContentQuantity, String unitContentUnit,
            CategoryEntity category, UnitOfMeasure unitOfMeasure, boolean active,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.barcode = barcode;
        this.price = price;
        this.purchasePrice = purchasePrice;
        this.presentation = presentation;
        this.packagingMaterial = packagingMaterial;
        this.contentQuantity = contentQuantity;
        this.contentUnit = contentUnit;
        this.variant = variant;
        this.unitsPerPackage = unitsPerPackage;
        this.unitContentQuantity = unitContentQuantity;
        this.unitContentUnit = unitContentUnit;
        this.category = category;
        this.unitOfMeasure = unitOfMeasure;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    void initializeAuditDates() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getBarcode() { return barcode; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public String getPresentation() { return presentation; }
    public String getPackagingMaterial() { return packagingMaterial; }
    public BigDecimal getContentQuantity() { return contentQuantity; }
    public String getContentUnit() { return contentUnit; }
    public String getVariant() { return variant; }
    public Integer getUnitsPerPackage() { return unitsPerPackage; }
    public BigDecimal getUnitContentQuantity() { return unitContentQuantity; }
    public String getUnitContentUnit() { return unitContentUnit; }
    public CategoryEntity getCategory() { return category; }
    public UnitOfMeasure getUnitOfMeasure() { return unitOfMeasure; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
