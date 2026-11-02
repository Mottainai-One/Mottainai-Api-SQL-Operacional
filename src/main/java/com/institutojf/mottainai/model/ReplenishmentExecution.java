package com.institutojf.mottainai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "replenishment_execution", schema = "mottainai")
@Getter
@Setter
public class ReplenishmentExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_list_id", nullable = false)
    private ReplenishmentPreList preList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    private Integer rating;

    private String comment;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReplenishmentExecutionItem> items = new ArrayList<>();

    public void addItem(ReplenishmentExecutionItem item) {
        item.setExecution(this);
        items.add(item);
    }

}
