package com.lera.controller;

import com.lera.dto.request.*;
import com.lera.dto.response.*;
import com.lera.exception.AppException;
import com.lera.model.User;
import com.lera.service.ResponderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/responders")
@RequiredArgsConstructor
public class ResponderController {

    private final ResponderService responderService;

    private void requireResponder(User user) {
        if (!user.getRole().name().equals("responder")) {
            throw AppException.forbidden("Access restricted to responders");
        }
    }

    // GET /api/v1/responders/profile
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(
            @AuthenticationPrincipal User user) {
        requireResponder(user);
        return ResponseEntity.ok(
            ApiResponse.success(Map.of("profile", responderService.getProfile(user.getId()))));
    }

    // PATCH /api/v1/responders/availability
    @PatchMapping("/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setAvailability(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AvailabilityRequest req) {
        requireResponder(user);
        return ResponseEntity.ok(
            ApiResponse.success(Map.of("profile",
                responderService.setAvailability(user.getId(), req))));
    }

    // PATCH /api/v1/responders/location
    @PatchMapping("/location")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateLocation(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody LocationRequest req) {
        requireResponder(user);
        responderService.updateLocation(user.getId(), req);
        return ResponseEntity.ok(
            ApiResponse.success(Map.of("message", "Location updated")));
    }
}
