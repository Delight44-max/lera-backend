package com.lera.dto.response;

import com.lera.model.*;
import lombok.*;
import java.time.Instant;

@Data @Builder
public class ResponderProfileDto {
    private String id;
    private String userId;
    private ResponderType type;
    private String certificationId;
    private Availability availability;
    private Double currentLat;
    private Double currentLng;
    private Instant lastSeenAt;

    public static ResponderProfileDto from(ResponderProfile p) {
        return ResponderProfileDto.builder()
            .id(p.getId())
            .userId(p.getUser().getId())
            .type(p.getType())
            .certificationId(p.getCertificationId())
            .availability(p.getAvailability())
            .currentLat(p.getCurrentLat())
            .currentLng(p.getCurrentLng())
            .lastSeenAt(p.getLastSeenAt())
            .build();
    }
}
