package com.lera.dto.response;

import com.lera.model.Notification;
import lombok.*;
import java.time.Instant;

@Data @Builder
public class NotificationDto {
    private String id;
    private String userId;
    private String emergencyId;
    private String title;
    private String body;
    private boolean isRead;
    private Instant sentAt;

    public static NotificationDto from(Notification n) {
        return NotificationDto.builder()
            .id(n.getId())
            .userId(n.getUser().getId())
            .emergencyId(n.getEmergency() != null ? n.getEmergency().getId() : null)
            .title(n.getTitle())
            .body(n.getBody())
            .isRead(n.isRead())
            .sentAt(n.getSentAt())
            .build();
    }
}
