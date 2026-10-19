package com.institutojf.mottainai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.dto.response.TokenResponse;
import com.institutojf.mottainai.dto.response.UserResponse;
import com.institutojf.mottainai.service.AuthenticationService;
import com.institutojf.mottainai.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
    }

    @Test
    @DisplayName("Does not expose staff password recovery")
    void doesNotExposeStaffPasswordRecovery() throws Exception {
        mockMvc.perform(put("/api/v1/auth/password-recovery").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should accept a valid password reset")
    void shouldAcceptAValidPasswordReset() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "token": "valid-reset-token",
                  "firebaseIdToken": "verified-firebase-id-token",
                  "newPassword": "New-Password@42"
                }
                """)).andExpect(status().isNoContent());
        verify(authenticationService).resetPassword(any());
    }

    @Test
    @DisplayName("Should return access and refresh tokens for Firebase login")
    void shouldLoginWithFirebaseIdentity() throws Exception {
        when(authenticationService.login(any()))
            .thenReturn(new TokenResponse("access", "refresh", "Bearer", 3600, 604800));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"firebaseIdToken":"verified-firebase-id-token","password":"Strong@Password42"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access"))
            .andExpect(jsonPath("$.refreshToken").value("refresh"));
        verify(authenticationService).login(any());
    }

    @Test
    @DisplayName("Should rotate tokens through refresh route")
    void shouldRefreshTokens() throws Exception {
        when(authenticationService.refresh("old-refresh"))
            .thenReturn(new TokenResponse("new-access", "new-refresh", "Bearer", 3600, 604800));
        mockMvc
            .perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"old-refresh\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
        verify(authenticationService).refresh("old-refresh");
    }

    @Test
    @DisplayName("Should use the access token session when logging out")
    void shouldLogoutMatchingSession() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Jwt jwt = new Jwt("access", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"),
                Map.of("sub", "firebase-staff-uid", "sid", sessionId.toString()));
        mockMvc
            .perform(post("/api/v1/auth/logout").principal(new JwtAuthenticationToken(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh\"}"))
            .andExpect(status().isNoContent());
        verify(authenticationService).logout("refresh", "firebase-staff-uid", sessionId);
    }

    @Test
    @DisplayName("Should return the current profile")
    void shouldReturnProfile() throws Exception {
        when(userProfileService.me("firebase-staff-uid")).thenReturn(
                new UserResponse(1, "Admin", "***456789**", "admin@example.com", null, "ADMINISTRATOR", true, 1));
        mockMvc
            .perform(get("/api/v1/auth/profile")
                .principal(new UsernamePasswordAuthenticationToken("firebase-staff-uid", null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"))
            .andExpect(jsonPath("$.cpf").value("***456789**"));
        verify(userProfileService).me("firebase-staff-uid");
    }

    @Test
    @DisplayName("Should send a password change for the authenticated user")
    void shouldChangePassword() throws Exception {
        mockMvc
            .perform(put("/api/v1/auth/password")
                .principal(new UsernamePasswordAuthenticationToken("firebase-staff-uid", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"Old@Password42","newPassword":"New@Password42"}
                        """))
            .andExpect(status().isNoContent());
        verify(authenticationService).changePassword(any(), eq("firebase-staff-uid"));
    }

}
