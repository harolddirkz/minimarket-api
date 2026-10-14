package com.minimarket.infrastructure.adapter.out.persistence.entity;

import com.minimarket.domain.model.SaleStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class SaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_number", nullable = false, length = 20, unique = true)
    private String saleNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItemEntity> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SaleEntity() {
    }

    public SaleEntity(Long id, String saleNumber, Long userId, SaleStatus status,
            BigDecimal total, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.saleNumber = saleNumber;
        this.userId = userId;
        this.status = status;
        this.total = total;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void addItem(SaleItemEntity item) {
        items.add(item);
        item.attachTo(this);
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
    public String getSaleNumber() { return saleNumber; }
    public Long getUserId() { return userId; }
    public SaleStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public List<SaleItemEntity> getItems() { return List.copyOf(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
