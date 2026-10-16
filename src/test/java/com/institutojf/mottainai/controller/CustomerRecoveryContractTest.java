package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.model.CustomerAuth;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import com.institutojf.mottainai.security.CustomerAccess;
import com.institutojf.mottainai.security.JwtProperties;
import com.institutojf.mottainai.security.JwtService;
import com.institutojf.mottainai.security.ProtectedDataToken;
import com.institutojf.mottainai.security.TokenHashService;
import com.institutojf.mottainai.service.CustomerAuthenticationService;
import com.institutojf.mottainai.service.CustomerRecoveryDeliveryService;
import com.institutojf.mottainai.service.CustomerService;
import com.institutojf.mottainai.service.PasswordResetEmailService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerRecoveryContractTest {

    @ParameterizedTest(name = "Recovery returns the same empty 204 for {0}")
    @ValueSource(strings = { "existing", "missing", "inactive-account", "inactive-customer", "deleted-customer", "smtp-failure" })
    void recoveryDoesNotExposeAccountExistence(String scenario) throws Exception {
        CustomerAuthRepository repository = mock(CustomerAuthRepository.class);
        PasswordResetEmailService mail = mock(PasswordResetEmailService.class);
        CustomerRecoveryDeliveryService delivery = new CustomerRecoveryDeliveryService(repository, mail);
        CustomerAuthenticationService service = new CustomerAuthenticationService(repository,
                mock(CustomerAccess.class), mock(CustomerService.class), new BCryptPasswordEncoder(4),
                new TokenHashService(), mock(JwtService.class), mock(JwtProperties.class), delivery);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CustomerAuthenticationController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        Customer customer = new Customer();
        customer.setActive(!scenario.equals("inactive-customer"));
        if (scenario.equals("deleted-customer")) {
            customer.setDeletedAt(LocalDateTime.now());
        }
        CustomerAuth auth = new CustomerAuth();
        auth.setId(1);
        auth.setCustomer(customer);
        auth.setActive(!scenario.equals("inactive-account"));
        auth.setLoginEmail(ProtectedDataToken.email("ana@example.com"));
        when(repository.findByLoginEmailForUpdate(auth.getLoginEmail()))
            .thenReturn(scenario.equals("missing") ? Optional.empty() : Optional.of(auth));
        boolean deliverable = scenario.equals("existing") || scenario.equals("smtp-failure");
        if (deliverable) {
            when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(auth));
        }
        if (scenario.equals("smtp-failure")) {
            doThrow(new MailSendException("SMTP unavailable")).when(mail).sendRecoveryLink(anyString(), anyString());
        }

        mvc.perform(post("/api/v1/customers/auth/password-recovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Ana@Example.com\"}"))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(repository).findByLoginEmailForUpdate(ProtectedDataToken.email("ana@example.com"));
        if (deliverable) {
            ArgumentCaptor<String> sentToken = ArgumentCaptor.forClass(String.class);
            verify(mail).sendRecoveryLink(eq("ana@example.com"), sentToken.capture());
            assertTrue(sentToken.getValue().matches("1\\.[A-Za-z0-9_-]{43}"));
            if (scenario.equals("existing")) {
                assertTrue(new BCryptPasswordEncoder(4).matches(sentToken.getValue().substring(2),
                        auth.getRecoveryTokenHash()));
            }
        }
        else {
            verifyNoInteractions(mail);
        }
        if (scenario.equals("smtp-failure")) {
            assertNull(auth.getRecoveryTokenHash());
            assertNull(auth.getRecoveryExpiresAt());
        }
    }
}
