package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.model.CustomerAuth;
import com.institutojf.mottainai.security.ProtectedDataToken;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against the supplied complete installer, never Hibernate-generated DDL or migrations. */
@EnabledIfEnvironmentVariable(named = "PROTECTED_SCHEMA_TEST_URL", matches = ".+")
class ProtectedCustomerSchemaTest {
    private static SessionFactory factory;

    @BeforeAll
    static void validateInstalledSchema() {
        String url = System.getenv("PROTECTED_SCHEMA_TEST_URL");
        assertTrue(url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/protected_contract"),
            "Use the isolated local protected_contract database, never the application database");
        factory = new Configuration()
            .addAnnotatedClass(Customer.class)
            .addAnnotatedClass(CustomerAuth.class)
            .setProperty("hibernate.connection.url", url)
            .setProperty("hibernate.connection.username", System.getenv().getOrDefault("PROTECTED_SCHEMA_TEST_USER", "postgres"))
            .setProperty("hibernate.connection.password", System.getenv().getOrDefault("PROTECTED_SCHEMA_TEST_PASSWORD", ""))
            .setProperty("hibernate.hbm2ddl.auto", "validate")
            .buildSessionFactory();
    }

    @AfterAll
    static void closeFactory() {
        if (factory != null) factory.close();
    }

    @Test
    void persistsAndReadsProtectedCustomerAndBcryptRecoveryAgainstInstaller() {
        try (var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            try {
                var customer = protectedCustomer();
                session.persist(customer);
                var auth = new CustomerAuth();
                var encoder = new BCryptPasswordEncoder();
                auth.setCustomer(customer);
                auth.setLoginEmail(customer.getEmail());
                auth.setPasswordHash(encoder.encode("Strong1!"));
                auth.setRecoveryTokenHash(encoder.encode("test-only-reset-token"));
                auth.setRecoveryExpiresAt(LocalDateTime.now().plusMinutes(15));
                auth.setPasswordChangedAt(LocalDateTime.now());
                auth.setCreatedAt(LocalDateTime.now());
                auth.setUpdatedAt(LocalDateTime.now());
                session.persist(auth);
                session.flush();
                Integer id = customer.getId();
                session.clear();

                var loaded = session.find(Customer.class, id);
                assertEquals(customer.getEmail(), loaded.getEmail());
                assertEquals((short) 1990, loaded.getBirthYear());
                assertEquals(customer.getAddressToken(), loaded.getAddressToken());
                assertNull(session.createNativeQuery("select address_id from mottainai.customer where customer_id = :id", Integer.class)
                    .setParameter("id", id).getSingleResult());
                var loadedAuth = session.createQuery("from CustomerAuth where loginEmail = :email", CustomerAuth.class)
                    .setParameter("email", customer.getEmail()).getSingleResult();
                assertTrue(encoder.matches("test-only-reset-token", loadedAuth.getRecoveryTokenHash()));

                loaded.setFullName(ProtectedDataToken.customerName("Updated name"));
                loaded.setPhone(null);
                loaded.setBirthYear(null);
                session.flush();
                session.clear();
                assertNull(session.find(Customer.class, id).getBirthYear());
            }
            finally {
                transaction.rollback();
            }
        }
    }

    @Test
    void rejectsPlainPersonalDataAndInvalidBirthYear() {
        assertRejected(customer -> customer.setFullName("Plain name"));
        assertRejected(customer -> customer.setEmail("plain@example.com"));
        assertRejected(customer -> customer.setCpf("12345678909"));
        assertRejected(customer -> customer.setPhone("11999999999"));
        assertRejected(customer -> customer.setBirthYear((short) 1899));
    }

    @Test
    void matchesPostgresqlNormalizationAndAddressConcatenation() {
        try (var session = factory.openSession()) {
            assertEquals(ProtectedDataToken.email(" Ana@Example.com "),
                session.createNativeQuery("select 'EMAIL_SHA256_' || upper(encode(public.digest(lower(btrim(:value)), 'sha256'), 'hex'))", String.class)
                    .setParameter("value", " Ana@Example.com ").getSingleResult());
            assertEquals(ProtectedDataToken.cpf("123.456.789-09"),
                session.createNativeQuery("select 'CPF_SHA256_' || upper(encode(public.digest(regexp_replace(:value, '\\D', '', 'g'), 'sha256'), 'hex'))", String.class)
                    .setParameter("value", "123.456.789-09").getSingleResult());
            assertEquals(ProtectedDataToken.address("01001000", " Rua A ", "1", "", "Centro", "São Paulo", "SP"),
                session.createNativeQuery("select 'ADDR_SHA256_' || upper(encode(public.digest(concat_ws('|', '01001000', ' Rua A ', '1', '', 'Centro', 'São Paulo', 'SP'), 'sha256'), 'hex'))", String.class)
                    .getSingleResult());
        }
    }

    private void assertRejected(java.util.function.Consumer<Customer> invalidValue) {
        try (var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            try {
                var customer = protectedCustomer();
                invalidValue.accept(customer);
                assertThrows(jakarta.persistence.PersistenceException.class, () -> {
                    session.persist(customer);
                    session.flush();
                });
            }
            finally {
                transaction.rollback();
            }
        }
    }

    private Customer protectedCustomer() {
        var customer = new Customer();
        customer.setFullName(ProtectedDataToken.customerName("Ana"));
        customer.setCpf(ProtectedDataToken.cpf("12345678909"));
        customer.setEmail(ProtectedDataToken.email(UUID.randomUUID() + "@example.com"));
        customer.setPhone(ProtectedDataToken.phone("11999999999"));
        customer.setExternalAuthUid(ProtectedDataToken.externalAuthUid(UUID.randomUUID().toString()));
        customer.setBirthYear((short) 1990);
        customer.setAddressToken(ProtectedDataToken.address("01001000", "Rua A", "1", null, "Centro", "São Paulo", "SP"));
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        return customer;
    }
}
