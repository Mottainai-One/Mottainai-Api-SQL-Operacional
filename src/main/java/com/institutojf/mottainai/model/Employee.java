package com.institutojf.mottainai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee", schema = "mottainai")
@Getter
@Setter
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private RetailStore store;

    @jakarta.persistence.OneToOne(mappedBy = "employee", fetch = FetchType.LAZY)
    private EmployeePrivateData privateData;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private EmployeeRole role;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
