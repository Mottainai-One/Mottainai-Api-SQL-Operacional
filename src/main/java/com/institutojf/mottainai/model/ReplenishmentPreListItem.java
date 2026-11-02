package com.institutojf.mottainai.model;

import com.institutojf.mottainai.model.enums.PriorityLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "replenishment_pre_list_item", schema = "mottainai")
@Getter
@Setter
public class ReplenishmentPreListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_list_item_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_list_id", nullable = false)
    private ReplenishmentPreList preList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "suggested_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal suggestedQuantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "priority_level")
    private PriorityLevel priority = PriorityLevel.MEDIUM;

}
