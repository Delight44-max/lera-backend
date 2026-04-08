package com.lera.service;

import com.lera.dto.request.*;
import com.lera.dto.response.ResponderProfileDto;
import com.lera.exception.AppException;
import com.lera.model.*;
import com.lera.repository.*;
import com.lera.socket.SocketIOService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponderService {

    private final ResponderProfileRepository responderProfileRepo;
    private final SocketIOService socketIOService;

    @Transactional(readOnly = true)
    public ResponderProfileDto getProfile(String userId) {
        ResponderProfile profile = responderProfileRepo.findByUserId(userId)
            .orElseThrow(() -> AppException.notFound("Responder profile not found"));
        return ResponderProfileDto.from(profile);
    }

    @Transactional
    public ResponderProfileDto setAvailability(String userId, AvailabilityRequest req) {
        ResponderProfile profile = responderProfileRepo.findByUserId(userId)
            .orElseThrow(() -> AppException.notFound("Responder profile not found"));
        profile.setAvailability(req.getAvailability());
        profile.setLastSeenAt(Instant.now());
        responderProfileRepo.save(profile);
        log.info("Responder {} availability → {}", userId, req.getAvailability());
        return ResponderProfileDto.from(profile);
    }

    @Transactional
    public void updateLocation(String userId, LocationRequest req) {
        ResponderProfile profile = responderProfileRepo.findByUserId(userId)
            .orElseThrow(() -> AppException.notFound("Responder profile not found"));
        profile.setCurrentLat(req.getLat());
        profile.setCurrentLng(req.getLng());
        profile.setLastSeenAt(Instant.now());
        responderProfileRepo.save(profile);


        log.debug("Responder {} location updated: {},{}", userId, req.getLat(), req.getLng());
    }
}
