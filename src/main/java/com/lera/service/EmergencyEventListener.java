package com.lera.service;

import com.lera.dto.response.EmergencyDto;
import com.lera.dto.response.NotificationDto;
import com.lera.event.*;
import com.lera.model.*;
import com.lera.repository.ResponderProfileRepository;
import com.lera.socket.SocketIOService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmergencyEventListener {

    private final SocketIOService socketIOService;
    private final NotificationService notificationService;
    private final ResponderProfileRepository responderProfileRepo;

    @Async
    @EventListener
    public void onEmergencyCreated(EmergencyCreatedEvent event) {

        Emergency emergency = event.getEmergency();
        EmergencyDto dto = EmergencyDto.from(emergency);

        log.info("[EVENT] EmergencyCreated → {}", emergency.getId());

        socketIOService.broadcastNewEmergency(dto);

        socketIOService.sendEmergencyUpdate(
                emergency.getCitizen().getId(),
                dto,
                NotificationDto.builder().build()
        );

        notificationService.createAndSend(
                emergency.getCitizen(),
                emergency,
                "Emergency Reported",
                "Your " + emergency.getType() + " emergency has been reported. Help is on the way."
        );

        List<ResponderProfile> online = responderProfileRepo.findByAvailability(Availability.online);

        for (ResponderProfile profile : online) {
            notificationService.createAndSend(
                    profile.getUser(),
                    emergency,
                    "🚨 New Emergency Alert",
                    capitalize(emergency.getType().name()) +
                            " reported" +
                            (emergency.getDistrict() != null ? " in " + emergency.getDistrict() : "") +
                            ". Tap to respond."
            );
        }

        log.info("[EVENT] Notified {} online responders", online.size());
    }

    @Async
    @EventListener
    public void onEmergencyStatusChanged(EmergencyStatusChangedEvent event) {

        Emergency emergency = event.getEmergency();
        EmergencyDto dto = EmergencyDto.from(emergency);

        log.info("[EVENT] EmergencyStatusChanged → {}", emergency.getId());

        // ONLY react when dispatched (ignore other statuses for now)
        if (emergency.getStatus() == EmergencyStatus.dispatched) {

            // 🚨 send update to citizen
            socketIOService.sendEmergencyUpdate(
                    emergency.getCitizen().getId(),
                    dto,
                    NotificationDto.builder().build()
            );

            // 🚨 send update to responder
            if (emergency.getResponder() != null) {
                socketIOService.sendEmergencyUpdate(
                        emergency.getResponder().getId(),
                        dto,
                        NotificationDto.builder().build()
                );
            }

            // 📩 notifications
            assert emergency.getResponder() != null;
            notificationService.createAndSend(
                    emergency.getCitizen(),
                    emergency,
                    "Responder Dispatched ✅",
                    emergency.getResponder().getFullName() + " is on the way to your location."
            );

            notificationService.createAndSend(
                    emergency.getResponder(),
                    emergency,
                    "Emergency Accepted",
                    "You accepted a " + emergency.getType() + " emergency."
            );
        }
    }

    @Async
    @EventListener
    public void onEmergencyDeclined(EmergencyDeclinedEvent event) {

        Emergency emergency = event.getEmergency();
        EmergencyDto dto = EmergencyDto.from(emergency);

        log.info("[EVENT] EmergencyDeclined → {}", emergency.getId());

        socketIOService.broadcastNewEmergency(dto);

        notificationService.createAndSend(
                emergency.getCitizen(),
                emergency,
                "Still Searching for Responder",
                "A responder declined. We are finding another available responder."
        );
    }

    @Async
    @EventListener
    public void onEmergencyResolved(EmergencyResolvedEvent event) {

        Emergency emergency = event.getEmergency();
        EmergencyDto dto = EmergencyDto.from(emergency);

        log.info("[EVENT] EmergencyResolved → {}", emergency.getId());

        socketIOService.sendEmergencyUpdate(
                emergency.getCitizen().getId(),
                dto,
                NotificationDto.builder().build()
        );

        if (emergency.getResponder() != null) {
            socketIOService.sendEmergencyUpdate(
                    emergency.getResponder().getId(),
                    dto,
                    NotificationDto.builder().build()
            );
        }

        notificationService.createAndSend(
                emergency.getCitizen(),
                emergency,
                "Emergency Resolved ✅",
                "Your emergency has been marked as resolved. Stay safe."
        );

        if (emergency.getResponder() != null) {
            notificationService.createAndSend(
                    emergency.getResponder(),
                    emergency,
                    "Emergency Closed",
                    "You have resolved the " + emergency.getType() + " emergency. Good work."
            );
        }
    }

    @Async
    @EventListener
    public void onEmergencyCancelled(EmergencyCancelledEvent event) {

        Emergency emergency = event.getEmergency();
        EmergencyDto dto = EmergencyDto.from(emergency);

        log.info("[EVENT] EmergencyCancelled → {}", emergency.getId());

        socketIOService.broadcastEmergencyUpdate(dto);

        if (emergency.getResponder() != null) {
            notificationService.createAndSend(
                    emergency.getResponder(),
                    emergency,
                    "Emergency Cancelled",
                    "The citizen has cancelled this " + emergency.getType() + " emergency."
            );
        }

        notificationService.createAndSend(
                emergency.getCitizen(),
                emergency,
                "Emergency Cancelled",
                "Your emergency report has been cancelled."
        );
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}