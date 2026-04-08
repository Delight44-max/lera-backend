package com.lera.controller;

import com.lera.dto.request.*;
import com.lera.dto.response.*;
import com.lera.model.User;
import com.lera.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        AuthResponse result = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result));
    }

    // POST /api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        AuthResponse result = authService.login(req);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // POST /api/v1/auth/refresh  — exchange refresh token for new access token
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest req) {
        AuthResponse result = authService.refresh(req);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // POST /api/v1/auth/logout  — revoke refresh token(s)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody LogoutRequest req) {
        authService.logout(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .status("success").message("Logged out successfully").build());
    }

    // GET /api/v1/auth/me
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> getMe(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
            ApiResponse.success(java.util.Map.of("user", authService.getMe(user.getId()))));
    }

    // PATCH /api/v1/auth/me
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Object>> updateMe(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateMeRequest req) {
        return ResponseEntity.ok(
            ApiResponse.success(java.util.Map.of("user", authService.updateMe(user.getId(), req))));
    }

    // PATCH /api/v1/auth/me/fcm-token
    @PatchMapping("/me/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FcmTokenRequest req) {
        authService.updateFcmToken(user.getId(), req.getFcmToken());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .status("success").message("FCM token updated").build());
    }
}
