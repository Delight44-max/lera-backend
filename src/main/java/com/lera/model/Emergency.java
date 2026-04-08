package com.lera.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emergencies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Emergency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_id")
    private User responder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EmergencyStatus status = EmergencyStatus.pending;

    private String district;

    @Column(nullable = false)
    private Double incidentLat;

    @Column(nullable = false)
    private Double incidentLng;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant resolvedAt;

    @OneToMany(mappedBy = "emergency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();
}
