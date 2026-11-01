package com.institutojf.mottainai.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "damage_report", schema = "mottainai")
@Getter @Setter @NoArgsConstructor
public class DamageReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "damage_report_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "store_id") private RetailStore store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employee_id") private Employee employee;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id") private Product product;
    @Column(nullable = false, length = 100) private String reason;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(length = 500) private String note;
    @Column(name = "photo_url", length = 2048) private String photoUrl;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
}
