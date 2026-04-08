package com.lera.service;

import com.lera.dto.request.CreateEmergencyRequest;
import com.lera.dto.response.EmergencyDto;
import com.lera.event.*;
import com.lera.exception.AppException;
import com.lera.model.*;
import com.lera.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyService {

    private final EmergencyRepository emergencyRepo;
    private final UserRepository userRepo;
    private final ApplicationEventPublisher eventPublisher;


    private static final List<EmergencyStatus> ACTIVE_STATUSES =
        List.of(EmergencyStatus.pending, EmergencyStatus.dispatched);

    @Transactional
    public EmergencyDto createEmergency(String citizenId, CreateEmergencyRequest req) {
        User citizen = userRepo.findById(citizenId)
            .orElseThrow(() -> AppException.notFound("User not found"));


        emergencyRepo.findFirstByCitizenIdAndStatusIn(citizenId, ACTIVE_STATUSES)
            .ifPresent(e -> { throw AppException.conflict("You already have an active emergency"); });

        Emergency emergency = Emergency.builder()
            .citizen(citizen)
            .type(req.getType())
            .incidentLat(req.getIncidentLat())
            .incidentLng(req.getIncidentLng())
            .district(req.getDistrict())
            .status(EmergencyStatus.pending)
            .build();

        emergency = emergencyRepo.save(emergency);
        log.info("Emergency created: {} by citizen:{}", emergency.getId(), citizenId);


        eventPublisher.publishEvent(new EmergencyCreatedEvent(this, emergency));

        return EmergencyDto.from(emergency);
    }

    @Transactional
    public EmergencyDto acceptEmergency(String emergencyId, String responderId) {
        Emergency emergency = getEmergencyOrThrow(emergencyId);
        User responder = userRepo.findById(responderId)
            .orElseThrow(() -> AppException.notFound("Responder not found"));

        if (emergency.getStatus() != EmergencyStatus.pending) {
            throw AppException.badRequest("Emergency is not in pending state");
        }


        emergencyRepo.findFirstByResponderIdAndStatusIn(responderId, ACTIVE_STATUSES)
            .ifPresent(e -> { throw AppException.conflict("You already have an active emergency"); });

        emergency.setResponder(responder);
        emergency.setStatus(EmergencyStatus.dispatched);
        emergency = emergencyRepo.save(emergency);
        log.info("Emergency {} accepted by responder:{}", emergencyId, responderId);

        eventPublisher.publishEvent(new EmergencyAcceptedEvent(this, emergency));
        return EmergencyDto.from(emergency);
    }

    @Transactional
    public EmergencyDto declineEmergency(String emergencyId, String responderId) {
        Emergency emergency = getEmergencyOrThrow(emergencyId);

        if (emergency.getStatus() != EmergencyStatus.pending) {
            throw AppException.badRequest("Emergency is not in pending state");
        }


        if (emergency.getResponder() != null &&
            emergency.getResponder().getId().equals(responderId)) {
            emergency.setResponder(null);
            emergency.setStatus(EmergencyStatus.pending);
            emergency = emergencyRepo.save(emergency);
        }

        eventPublisher.publishEvent(new EmergencyDeclinedEvent(this, emergency));
        return EmergencyDto.from(emergency);
    }

    @Transactional
    public EmergencyDto resolveEmergency(String emergencyId, String responderId) {
        Emergency emergency = getEmergencyOrThrow(emergencyId);

        if (emergency.getStatus() != EmergencyStatus.dispatched) {
            throw AppException.badRequest("Emergency is not dispatched");
        }
        if (emergency.getResponder() == null ||
            !emergency.getResponder().getId().equals(responderId)) {
            throw AppException.forbidden("You are not assigned to this emergency");
        }

        emergency.setStatus(EmergencyStatus.resolved);
        emergency.setResolvedAt(Instant.now());
        emergency = emergencyRepo.save(emergency);
        log.info("Emergency {} resolved by responder:{}", emergencyId, responderId);

        eventPublisher.publishEvent(new EmergencyResolvedEvent(this, emergency));
        return EmergencyDto.from(emergency);
    }

    @Transactional
    public EmergencyDto cancelEmergency(String emergencyId, String citizenId) {
        Emergency emergency = getEmergencyOrThrow(emergencyId);

        if (!emergency.getCitizen().getId().equals(citizenId)) {
            throw AppException.forbidden("You did not create this emergency");
        }
        if (!ACTIVE_STATUSES.contains(emergency.getStatus())) {
            throw AppException.badRequest("Emergency cannot be cancelled in its current state");
        }

        emergency.setStatus(EmergencyStatus.cancelled);
        emergency = emergencyRepo.save(emergency);
        log.info("Emergency {} cancelled by citizen:{}", emergencyId, citizenId);

        eventPublisher.publishEvent(new EmergencyCancelledEvent(this, emergency));
        return EmergencyDto.from(emergency);
    }

    @Transactional(readOnly = true)
    public EmergencyDto getById(String emergencyId, String requestingUserId) {
        Emergency emergency = emergencyRepo.findByIdWithUsers(emergencyId)
            .orElseThrow(() -> AppException.notFound("Emergency not found"));

        String citizenId  = emergency.getCitizen() != null ? emergency.getCitizen().getId() : null;
        String responderId = emergency.getResponder() != null ? emergency.getResponder().getId() : null;

        if (!requestingUserId.equals(citizenId) && !requestingUserId.equals(responderId)) {
            throw AppException.forbidden("Access denied to this emergency");
        }
        return EmergencyDto.from(emergency);
    }

    @Transactional(readOnly = true)
    public Optional<EmergencyDto> getActive(String userId, String role) {
        Optional<Emergency> active = "responder".equals(role)
            ? emergencyRepo.findFirstByResponderIdAndStatusIn(userId, ACTIVE_STATUSES)
            : emergencyRepo.findFirstByCitizenIdAndStatusIn(userId, ACTIVE_STATUSES);
        return active.map(EmergencyDto::from);
    }

    @Transactional(readOnly = true)
    public List<EmergencyDto> getHistory(String userId, String role) {
        List<Emergency> list = "responder".equals(role)
            ? emergencyRepo.findByResponderIdOrderByCreatedAtDesc(userId)
            : emergencyRepo.findByCitizenIdOrderByCreatedAtDesc(userId);
        return list.stream().map(EmergencyDto::from).toList();
    }

    private Emergency getEmergencyOrThrow(String id) {
        return emergencyRepo.findByIdWithUsers(id)
            .orElseThrow(() -> AppException.notFound("Emergency not found"));
    }
}
