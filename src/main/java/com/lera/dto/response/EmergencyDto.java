package com.lera.dto.response;

import com.lera.model.*;
import lombok.*;
import java.time.Instant;

@Data @Builder
public class EmergencyDto {
    private String id;
    private String citizenId;
    private String responderId;
    private EmergencyType type;
    private EmergencyStatus status;
    private String district;
    private Double incidentLat;
    private Double incidentLng;
    private Instant createdAt;
    private Instant resolvedAt;
    private UserDto citizen;
    private UserDto responder;

    public static EmergencyDto from(Emergency e) {
        return EmergencyDto.builder()
            .id(e.getId())
            .citizenId(e.getCitizen() != null ? e.getCitizen().getId() : null)
            .responderId(e.getResponder() != null ? e.getResponder().getId() : null)
            .type(e.getType())
            .status(e.getStatus())
            .district(e.getDistrict())
            .incidentLat(e.getIncidentLat())
            .incidentLng(e.getIncidentLng())
            .createdAt(e.getCreatedAt())
            .resolvedAt(e.getResolvedAt())
            .citizen(e.getCitizen() != null ? UserDto.from(e.getCitizen()) : null)
            .responder(e.getResponder() != null ? UserDto.from(e.getResponder()) : null)
            .build();
    }
}
