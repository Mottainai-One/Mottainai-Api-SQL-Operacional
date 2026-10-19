package com.institutojf.mottainai.service;

import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeePrivateData;
import com.institutojf.mottainai.repository.EmployeePrivateDataRepository;
import com.institutojf.mottainai.security.StaffDataCryptography;
import com.institutojf.mottainai.security.StaffDataCryptography.EncryptedValue;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists and reads employee PII exclusively through employee_private_data. */
@Service
public class EmployeePrivateDataService {

    private final EmployeePrivateDataRepository repository;

    private final StaffDataCryptography cryptography;

    public EmployeePrivateDataService(EmployeePrivateDataRepository repository, StaffDataCryptography cryptography) {
        this.repository = repository;
        this.cryptography = cryptography;
    }

    @Transactional
    public EmployeePrivateData save(Employee employee, String name, String cpf, String email, String phone) {
        String normalizedName = StaffContactData.name(name);
        String normalizedCpf = StaffContactData.cpf(cpf);
        if (normalizedName == null) {
            throw new IllegalArgumentException("Employee name is required");
        }
        if (normalizedCpf == null) {
            throw new IllegalArgumentException("Employee CPF is required");
        }
        EmployeePrivateData data = employee.getPrivateData();
        if (data == null) {
            data = new EmployeePrivateData();
            data.setEmployee(employee);
            data.setEmployeeId(employee.getId());
            employee.setPrivateData(data);
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (data.getCreatedAt() == null) {
            data.setCreatedAt(now);
        }
        data.setUpdatedAt(now);
        data.setAlgorithm("AES-256-GCM");
        data.setProtectionState("ENCRYPTED");
        data.setKeyVersion(cryptography.keyVersion());
        data.setEncryptedAt(now);
        employee.setCpfLookupHmac(cryptography.cpfLookupHmac(normalizedCpf));
        setName(data, normalizedName);
        setCpf(data, normalizedCpf);
        setEmail(data, email);
        setPhone(data, phone);
        return repository.save(data);
    }

    @Transactional(readOnly = true)
    public boolean cpfExists(String cpf, Integer excludedEmployeeId) {
        String normalized = StaffContactData.cpf(cpf);
        if (normalized == null) {
            return false;
        }
        byte[] lookup = cryptography.cpfLookupHmac(normalized);
        return excludedEmployeeId == null
                ? repository.existsByEmployee_CpfLookupHmac(lookup)
                : repository.existsByEmployee_CpfLookupHmacAndEmployee_IdNot(lookup, excludedEmployeeId);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email, Integer excludedEmployeeId) {
        String normalized = StaffContactData.email(email);
        if (normalized == null) {
            return false;
        }
        return repository.findAll().stream()
                .filter(data -> !Objects.equals(data.getEmployeeId(), excludedEmployeeId))
                .map(this::decryptEmail)
                .anyMatch(value -> value != null && value.equalsIgnoreCase(normalized));
    }

    public String name(Employee employee) {
        EmployeePrivateData data = data(employee);
        return cryptography.decrypt("employee.name", data.getKeyVersion(), data.getNameCiphertext(), data.getNameNonce());
    }

    public String cpf(Employee employee) {
        return decryptCpf(data(employee));
    }

    public String cpfMasked(Employee employee) {
        return data(employee).getCpfMasked();
    }

    public String email(Employee employee) {
        return decryptEmail(data(employee));
    }

    public String phone(Employee employee) {
        EmployeePrivateData data = data(employee);
        return cryptography.decrypt("employee.phone", data.getKeyVersion(), data.getPhoneCiphertext(), data.getPhoneNonce());
    }

    private void setName(EmployeePrivateData data, String value) {
        EncryptedValue encrypted = cryptography.encrypt("employee.name", value);
        data.setNameCiphertext(ciphertext(encrypted));
        data.setNameNonce(nonce(encrypted));
        data.setNameMasked(maskName(value));
    }

    private void setCpf(EmployeePrivateData data, String value) {
        String normalized = StaffContactData.cpf(value);
        EncryptedValue encrypted = cryptography.encrypt("staff.cpf", normalized);
        data.setCpfCiphertext(ciphertext(encrypted));
        data.setCpfNonce(nonce(encrypted));
        data.setCpfMasked(maskCpf(normalized));
    }

    private void setEmail(EmployeePrivateData data, String value) {
        String normalized = StaffContactData.email(value);
        EncryptedValue encrypted = cryptography.encrypt("employee.email", normalized);
        data.setEmailCiphertext(ciphertext(encrypted));
        data.setEmailNonce(nonce(encrypted));
        data.setEmailMasked(maskEmail(normalized));
    }

    private void setPhone(EmployeePrivateData data, String value) {
        String normalized = StaffContactData.phone(value);
        EncryptedValue encrypted = cryptography.encrypt("employee.phone", normalized);
        data.setPhoneCiphertext(ciphertext(encrypted));
        data.setPhoneNonce(nonce(encrypted));
        data.setPhoneMasked(maskPhone(normalized));
    }

    private String decryptCpf(EmployeePrivateData data) {
        return cryptography.decrypt("staff.cpf", data.getKeyVersion(), data.getCpfCiphertext(), data.getCpfNonce());
    }

    private String decryptEmail(EmployeePrivateData data) {
        return cryptography.decrypt("employee.email", data.getKeyVersion(), data.getEmailCiphertext(), data.getEmailNonce());
    }

    private EmployeePrivateData data(Employee employee) {
        if (employee == null || employee.getPrivateData() == null
                || !"ENCRYPTED".equals(employee.getPrivateData().getProtectionState())
                || employee.getPrivateData().getKeyVersion() == null) {
            throw new IllegalStateException("Employee personal data is not encrypted in employee_private_data");
        }
        return employee.getPrivateData();
    }

    private static byte[] ciphertext(EncryptedValue encrypted) {
        return encrypted == null ? null : encrypted.ciphertext();
    }

    private static byte[] nonce(EncryptedValue encrypted) {
        return encrypted == null ? null : encrypted.nonce();
    }

    private static String maskName(String value) {
        return value == null || value.isBlank() ? null : "***";
    }

    private static String maskCpf(String value) {
        return value == null ? null : "***.***.***-**";
    }

    private static String maskEmail(String value) {
        return value == null ? null : "***@***";
    }

    private static String maskPhone(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.length() <= 2 ? "**" : "*".repeat(Math.max(0, digits.length() - 2)) + digits.substring(digits.length() - 2);
    }
}
