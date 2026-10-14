package com.minimarket.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_items")
public class SaleItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private SaleEntity sale;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 160)
    private String productName;

    @Column(name = "product_presentation", length = 30)
    private String productPresentation;

    @Column(name = "product_packaging_material", length = 30)
    private String productPackagingMaterial;

    @Column(name = "product_content_quantity", precision = 12, scale = 3)
    private BigDecimal productContentQuantity;

    @Column(name = "product_content_unit", length = 10)
    private String productContentUnit;

    @Column(name = "product_variant", length = 60)
    private String productVariant;
    @Column(name = "product_units_per_package")
    private Integer productUnitsPerPackage;
    @Column(name = "product_unit_content_quantity", precision = 12, scale = 3)
    private BigDecimal productUnitContentQuantity;
    @Column(name = "product_unit_content_unit", length = 10)
    private String productUnitContentUnit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected SaleItemEntity() {
    }

    public SaleItemEntity(Long id, Long productId, String productName, String productPresentation, String productPackagingMaterial,
            BigDecimal productContentQuantity, String productContentUnit, BigDecimal quantity,
            String productVariant, Integer productUnitsPerPackage,
            BigDecimal productUnitContentQuantity, String productUnitContentUnit,
            BigDecimal unitPrice, BigDecimal subtotal) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productPresentation = productPresentation;
        this.productPackagingMaterial = productPackagingMaterial;
        this.productContentQuantity = productContentQuantity;
        this.productContentUnit = productContentUnit;
        this.productVariant = productVariant;
        this.productUnitsPerPackage = productUnitsPerPackage;
        this.productUnitContentQuantity = productUnitContentQuantity;
        this.productUnitContentUnit = productUnitContentUnit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    void attachTo(SaleEntity sale) {
        this.sale = sale;
    }

    public Long getId() { return id; }
    public SaleEntity getSale() { return sale; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductPresentation() { return productPresentation; }
    public String getProductPackagingMaterial() { return productPackagingMaterial; }
    public BigDecimal getProductContentQuantity() { return productContentQuantity; }
    public String getProductContentUnit() { return productContentUnit; }
    public String getProductVariant() { return productVariant; }
    public Integer getProductUnitsPerPackage() { return productUnitsPerPackage; }
    public BigDecimal getProductUnitContentQuantity() { return productUnitContentQuantity; }
    public String getProductUnitContentUnit() { return productUnitContentUnit; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
}
