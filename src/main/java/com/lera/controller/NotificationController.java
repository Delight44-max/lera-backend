package com.lera.controller;

import com.lera.dto.response.*;
import com.lera.model.User;
import com.lera.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/v1/notifications
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<NotificationDto>>>> getAll(
            @AuthenticationPrincipal User user) {
        List<NotificationDto> notifications = notificationService.getNotifications(user.getId());
        return ResponseEntity.ok(
            ApiResponse.<Map<String, List<NotificationDto>>>builder()
                .status("success")
                .data(Map.of("notifications", notifications))
                .results(notifications.size())
                .build());
    }

    // PATCH /api/v1/notifications/read-all  — MUST be before /{id}/read
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .status("success").message("All notifications marked as read").build());
    }

    // PATCH /api/v1/notifications/:id/read
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .status("success").message("Notification marked as read").build());
    }
}
