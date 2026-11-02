package com.institutojf.mottainai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/** AES-GCM ciphertext, nonce and display masks for employee personal data. */
@Entity
@Table(name = "employee_private_data", schema = "mottainai")
@Getter
@Setter
public class EmployeePrivateData {

    @Id
    @Column(name = "employee_id")
    private Integer employeeId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "name_ciphertext", columnDefinition = "bytea")
    private byte[] nameCiphertext;

    @Column(name = "name_nonce", columnDefinition = "bytea")
    private byte[] nameNonce;

    @Column(name = "name_masked", length = 150)
    private String nameMasked;

    @Column(name = "cpf_ciphertext", columnDefinition = "bytea")
    private byte[] cpfCiphertext;

    @Column(name = "cpf_nonce", columnDefinition = "bytea")
    private byte[] cpfNonce;

    @Column(name = "cpf_masked", length = 20)
    private String cpfMasked;

    @Column(name = "email_ciphertext", columnDefinition = "bytea")
    private byte[] emailCiphertext;

    @Column(name = "email_nonce", columnDefinition = "bytea")
    private byte[] emailNonce;

    @Column(name = "email_masked", length = 150)
    private String emailMasked;

    @Column(name = "phone_ciphertext", columnDefinition = "bytea")
    private byte[] phoneCiphertext;

    @Column(name = "phone_nonce", columnDefinition = "bytea")
    private byte[] phoneNonce;

    @Column(name = "phone_masked", length = 80)
    private String phoneMasked;

    @Column(name = "key_version")
    private Short keyVersion;

    @Column(nullable = false, length = 20)
    private String algorithm = "AES-256-GCM";

    @Column(name = "protection_state", nullable = false, length = 20)
    private String protectionState = "MIGRATION_PENDING";

    @Column(name = "encrypted_at")
    private OffsetDateTime encryptedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
