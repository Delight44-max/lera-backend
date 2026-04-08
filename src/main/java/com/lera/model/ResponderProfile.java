package com.lera.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "responder_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResponderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResponderType type;

    @Column(nullable = false)
    private String certificationId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Availability availability = Availability.offline;

    private Double currentLat;
    private Double currentLng;
    private Instant lastSeenAt;
}
