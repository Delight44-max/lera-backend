package com.lera.socket;

import com.corundumstudio.socketio.SocketIOServer;
import com.lera.dto.response.EmergencyDto;
import com.lera.dto.response.NotificationDto;
import com.lera.service.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketIOService {

    private final SocketIOServer server;
    private final NotificationService notificationService;

    @PostConstruct
    public void start() {

        server.addConnectListener(client -> {
            String sessionId = client.getSessionId().toString();

            String userId = client.getHandshakeData().getSingleUrlParam("userId");

            if (userId != null) {
                client.joinRoom(userId);
                log.info("[Socket.IO] User {} joined room", userId);
            }

            log.info("[Socket.IO] Client connected: {}", sessionId);
        });

        server.addDisconnectListener(client -> {
            log.info("[Socket.IO] Client disconnected: {}", client.getSessionId());
        });

        server.start();
        log.info("[Socket.IO] Server started on port {}", server.getConfiguration().getPort());
    }
    @PreDestroy
    public void stop() {
        server.stop();
        log.info("[Socket.IO] Server stopped");
    }




    public void broadcastNewEmergency(EmergencyDto emergency) {
        server.getBroadcastOperations().sendEvent("emergency:new", emergency);
        log.info("[Socket.IO]  emergency:new → {}", emergency.getId());
    }

    public void sendNotificationToUser(String userId, NotificationDto notificationDto) {
        server.getRoomOperations(userId).sendEvent("notification:new", notificationDto);
    }


    public void sendEmergencyUpdate(String userId, EmergencyDto emergency, NotificationDto notificationDto) {
        server.getRoomOperations(userId).sendEvent("emergency:statusChanged", emergency);
        if (notificationDto != null) {
            server.getRoomOperations(userId)
                    .sendEvent("notification:new", notificationDto);
        }
        log.info("[Socket.IO] emergency:statusChanged → {}", userId);
    }



    public void broadcastEmergencyUpdate(EmergencyDto emergency) {
        server.getBroadcastOperations().sendEvent("emergency:statusChanged", emergency);
        log.info("[Socket.IO] broadcast emergency:statusChanged → {}", emergency.getId());
    }

    public void sendResponderLocation(String citizenId, String responderId, double lat, double lng) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("responderId", responderId);
        payload.put("lat", lat);
        payload.put("lng", lng);
        server.getRoomOperations(citizenId).sendEvent("responder:location", payload);
    }
}
