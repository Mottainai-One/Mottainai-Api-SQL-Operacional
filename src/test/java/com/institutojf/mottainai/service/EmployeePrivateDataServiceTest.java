package com.institutojf.mottainai.service;

import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeePrivateData;
import com.institutojf.mottainai.repository.EmployeePrivateDataRepository;
import com.institutojf.mottainai.repository.EmployeeRepository;
import com.institutojf.mottainai.security.StaffDataCryptography;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeePrivateDataServiceTest {

    private final EmployeePrivateDataRepository repository = mock(EmployeePrivateDataRepository.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final StaffDataCryptography cryptography = new StaffDataCryptography(
            Base64.getEncoder().encodeToString(new byte[32]), 3);
    private final EmployeePrivateDataService service = new EmployeePrivateDataService(repository, cryptography, employeeRepository);

    @Test
    void storesAllEmployeePiiAsBinaryAesGcmAndPersistsMasksAndKeyMetadata() {
        Employee employee = new Employee();
        employee.setId(44);
        when(repository.save(any(EmployeePrivateData.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeePrivateData saved = service.save(employee, "Ana Silva", "123.456.789-09", "ANA@example.com", "+55 11 99999-1242");

        assertEquals(44, saved.getEmployeeId());
        assertNotNull(saved.getNameCiphertext());
        assertNotNull(saved.getNameNonce());
        assertNotNull(saved.getCpfCiphertext());
        assertNotNull(saved.getCpfNonce());
        assertNotNull(saved.getEmailCiphertext());
        assertNotNull(saved.getEmailNonce());
        assertNotNull(saved.getPhoneCiphertext());
        assertNotNull(saved.getPhoneNonce());
        assertEquals(Short.valueOf((short) 3), saved.getKeyVersion());
        assertEquals("AES-256-GCM", saved.getAlgorithm());
        assertEquals("ENCRYPTED", saved.getProtectionState());
        assertNotNull(saved.getEncryptedAt());
        assertEquals("***", saved.getNameMasked());
        assertEquals("***.***.***-**", saved.getCpfMasked());
        assertEquals("***@***", saved.getEmailMasked());
        assertNotEquals("Ana Silva", new String(saved.getNameCiphertext(), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("Ana Silva", cryptography.decrypt("employee.name", saved.getKeyVersion(), saved.getNameCiphertext(), saved.getNameNonce()));
        assertEquals("12345678909", cryptography.decrypt("staff.cpf", saved.getKeyVersion(), saved.getCpfCiphertext(), saved.getCpfNonce()));
        assertEquals("ana@example.com", cryptography.decrypt("employee.email", saved.getKeyVersion(), saved.getEmailCiphertext(), saved.getEmailNonce()));
        assertEquals("+55 11 99999-1242", cryptography.decrypt("employee.phone", saved.getKeyVersion(), saved.getPhoneCiphertext(), saved.getPhoneNonce()));
        verify(repository).save(saved);
    }

    @Test
    void normalizesBlankOptionalEmailAndPhoneToNullAndRejectsMissingRequiredPii() {
        Employee employee = new Employee();
        employee.setId(45);
        when(repository.save(any(EmployeePrivateData.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeePrivateData saved = service.save(employee, "  Ana Silva  ", "12345678909", "  ", "   ");

        assertNull(saved.getEmailCiphertext());
        assertNull(saved.getEmailNonce());
        assertNull(saved.getEmailMasked());
        assertNull(saved.getPhoneCiphertext());
        assertNull(saved.getPhoneNonce());
        assertNull(saved.getPhoneMasked());
        assertEquals("Ana Silva", cryptography.decrypt("employee.name", saved.getKeyVersion(),
                saved.getNameCiphertext(), saved.getNameNonce()));
        assertThrows(IllegalArgumentException.class,
                () -> service.save(new Employee(), "   ", "12345678909", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.save(new Employee(), "Ana Silva", "   ", null, null));
    }
}
