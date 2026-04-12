package com.lera.controller;

import com.lera.dto.request.CreateEmergencyRequest;
import com.lera.dto.response.*;
import com.lera.exception.AppException;
import com.lera.model.User;
import com.lera.service.EmergencyService;
import com.lera.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/emergencies")
@RequiredArgsConstructor
public class EmergencyController {

    private final EmergencyService emergencyService;
    private final NotificationService notificationService;

    // POST /api/v1/emergencies  — citizen creates emergency
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, EmergencyDto>>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateEmergencyRequest req) {

        if (user.getRole().name().equals("responder")) {
            throw AppException.forbidden("Only citizens can report emergencies");
        }
        EmergencyDto emergency = emergencyService.createEmergency(user.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(Map.of("emergency", emergency)));
    }

    // GET /api/v1/emergencies/active
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActive(
            @AuthenticationPrincipal User user) {

        Optional<EmergencyDto> active = emergencyService.getActive(
                user.getId(), user.getRole().name());
        // Return null emergency if none — frontend handles null gracefully
        Map<String, Object> data = new HashMap<>();
        data.put("emergency", active.orElse(null));
        return ResponseEntity.ok(ApiResponse.success(data));
    }


    // GET /api/v1/emergencies/history
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, List<EmergencyDto>>>> getHistory(
            @AuthenticationPrincipal User user) {

        List<EmergencyDto> emergencies = emergencyService.getHistory(
            user.getId(), user.getRole().name());
        return ResponseEntity.ok(
            ApiResponse.<Map<String, List<EmergencyDto>>>builder()
                .status("success")
                .data(Map.of("emergencies", emergencies))
                .results(emergencies.size())
                .build());
    }

    // GET /api/v1/emergencies/:id
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, EmergencyDto>>> getById(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        EmergencyDto emergency = emergencyService.getById(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("emergency", emergency)));
    }

    // PATCH /api/v1/emergencies/:id/accept  — responder accepts
    @PatchMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Map<String, EmergencyDto>>> accept(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        if (!user.getRole().name().equals("responder")) {
            throw AppException.forbidden("Only responders can accept emergencies");
        }
        EmergencyDto emergency = emergencyService.acceptEmergency(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("emergency", emergency)));
    }

    // PATCH /api/v1/emergencies/:id/decline  — responder declines
    @PatchMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<Object>> decline(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        if (!user.getRole().name().equals("responder")) {
            throw AppException.forbidden("Only responders can decline emergencies");
        }
        EmergencyDto emergency = emergencyService.declineEmergency(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("emergency", emergency)));
    }

    // PATCH /api/v1/emergencies/:id/resolve  — responder resolves
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<Map<String, EmergencyDto>>> resolve(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        if (!user.getRole().name().equals("responder")) {
            throw AppException.forbidden("Only responders can resolve emergencies");
        }
        EmergencyDto emergency = emergencyService.resolveEmergency(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("emergency", emergency)));
    }

    // PATCH /api/v1/emergencies/:id/cancel  — citizen cancels
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Map<String, EmergencyDto>>> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        if (!user.getRole().name().equals("citizen")) {
            throw AppException.forbidden("Only citizens can cancel emergencies");
        }
        EmergencyDto emergency = emergencyService.cancelEmergency(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("emergency", emergency)));
    }

}
