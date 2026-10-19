package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.AuthenticationControllerApi;
import com.institutojf.mottainai.dto.request.ChangePasswordRequest;
import com.institutojf.mottainai.dto.request.LoginRequest;
import com.institutojf.mottainai.dto.request.RefreshTokenRequest;
import com.institutojf.mottainai.dto.request.ResetPasswordRequest;
import com.institutojf.mottainai.dto.response.TokenResponse;
import com.institutojf.mottainai.dto.response.TokenValidationResponse;
import com.institutojf.mottainai.dto.response.UserResponse;
import com.institutojf.mottainai.service.AuthenticationService;
import com.institutojf.mottainai.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController implements AuthenticationControllerApi {

    private final AuthenticationService authenticationService;

    private final UserProfileService userProfileService;

    @Override
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> profile(Authentication authentication) {
        return ResponseEntity.ok(userProfileService.me(authentication.getName()));
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @Override
    @GetMapping("/password-reset/validate")
    public ResponseEntity<TokenValidationResponse> validateResetToken(@RequestParam String token) {
        return ResponseEntity.ok(new TokenValidationResponse(authenticationService.validateResetToken(token)));
    }

    // Recebe o convite e a nova senha para ativar a conta operacional.
    @Override
    @PostMapping("/password-reset")
    public ResponseEntity<Void> passwordReset(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refresh(request.refreshToken()));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request, Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        UUID sessionId = UUID.fromString(token.getToken().getClaimAsString("sid"));
        authenticationService.logout(request.refreshToken(), authentication.getName(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        authenticationService.changePassword(request, authentication.getName());
        return ResponseEntity.noContent().build();
    }

}
