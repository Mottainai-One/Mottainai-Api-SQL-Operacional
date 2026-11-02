package com.institutojf.mottainai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_rule", schema = "mottainai")
@Getter
@Setter
@NoArgsConstructor
public class SystemRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer id;

    @Column(name = "rule_category", nullable = false, length = 30)
    private String category;

    @Column(name = "rule_key", nullable = false, length = 60)
    private String key;

    @Column(name = "rule_name", length = 120)
    private String name;

    @Column(name = "rule_value")
    private String value;

    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;

    @Column
    private String description;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
