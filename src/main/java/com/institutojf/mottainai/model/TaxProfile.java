package com.institutojf.mottainai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_profile", schema = "mottainai")
@Getter
@Setter
public class TaxProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tax_profile_id")
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 4)
    private String cfop;

    @Column(name = "icms_cst", length = 3)
    private String icmsCst;

    @Column(name = "icms_csosn", length = 4)
    private String icmsCsosn;

    @Column(name = "icms_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal icmsRate;

    @Column(name = "ipi_cst", length = 2)
    private String ipiCst;

    @Column(name = "ipi_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal ipiRate;

    @Column(name = "pis_cst", length = 2)
    private String pisCst;

    @Column(name = "pis_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal pisRate;

    @Column(name = "cofins_cst", length = 2)
    private String cofinsCst;

    @Column(name = "cofins_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal cofinsRate;

    @Column(nullable = false)
    private Boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}
