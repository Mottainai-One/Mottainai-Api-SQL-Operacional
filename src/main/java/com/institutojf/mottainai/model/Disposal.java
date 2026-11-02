package com.institutojf.mottainai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "disposal", schema = "mottainai")
@Getter
@Setter
public class Disposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disposal_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private RetailStore store;

    @Column(name = "suggested_action_id")
    private Integer suggestedActionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(name = "disposal_date", nullable = false)
    private LocalDateTime disposalDate;

    private String observation;

    @Version
    private Integer version = 1;

    @OneToMany(mappedBy = "disposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DisposalItem> items = new ArrayList<>();

    public void addItem(DisposalItem item) {
        item.setDisposal(this);
        items.add(item);
    }

}
