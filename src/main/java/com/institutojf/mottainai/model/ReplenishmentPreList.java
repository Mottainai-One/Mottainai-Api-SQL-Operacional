package com.institutojf.mottainai.model;

import com.institutojf.mottainai.model.enums.PreListStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "replenishment_pre_list", schema = "mottainai")
@Getter
@Setter
public class ReplenishmentPreList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_list_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private RetailStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "pre_list_status")
    private PreListStatus status = PreListStatus.GENERATED;

    @OneToMany(mappedBy = "preList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReplenishmentPreListItem> items = new ArrayList<>();

    public void addItem(ReplenishmentPreListItem item) {
        item.setPreList(this);
        items.add(item);
    }

}
