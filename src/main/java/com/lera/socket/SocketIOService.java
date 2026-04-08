package com.lera.socket;

import com.corundumstudio.socketio.SocketIOServer;
import com.lera.dto.response.EmergencyDto;
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

    @PostConstruct
    public void start() {
        server.addConnectListener(client -> {
            String sessionId = client.getSessionId().toString();
            log.info("[Socket.IO] Client connected: {}", sessionId);
        });

        server.addDisconnectListener(client -> {
            String sessionId = client.getSessionId().toString();
            log.info("[Socket.IO] Client disconnected: {}", sessionId);
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
        log.info("[Socket.IO] Broadcast emergency:new → {}", emergency.getId());
    }


    public void sendEmergencyUpdate(String userId, EmergencyDto emergency) {
        server.getRoomOperations(userId).sendEvent("emergency:updated", emergency);
        log.info("[Socket.IO] emergency:updated → room:{} status:{}", userId, emergency.getStatus());
    }


    public void broadcastEmergencyUpdate(EmergencyDto emergency) {
        server.getBroadcastOperations().sendEvent("emergency:updated", emergency);
        log.info("[Socket.IO] Broadcast emergency:updated → {}", emergency.getId());
    }


    public void sendResponderLocation(String citizenId, String responderId, double lat, double lng) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("responderId", responderId);
        payload.put("lat", lat);
        payload.put("lng", lng);
        server.getRoomOperations(citizenId).sendEvent("responder:location", payload);
    }
}
