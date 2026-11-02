package com.institutojf.mottainai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Encrypted, recoverable personal-data payload; requires a future DB sidecar table. */
@Entity
@Table(name = "customer_private_data", schema = "mottainai")
@Getter
@Setter
public class CustomerPrivateData {
    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "payload_ciphertext", nullable = false, columnDefinition = "bytea")
    private byte[] payloadCiphertext;

    @Column(name = "payload_nonce", nullable = false, columnDefinition = "bytea")
    private byte[] payloadNonce;

    @Column(name = "key_version", nullable = false)
    private Short keyVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
