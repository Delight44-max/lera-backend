package com.lera.config;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;
import com.lera.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SocketIOConfig {

    private final JwtService jwtService;

    @Value("${lera.socketio.host}")
    private String host;

    @Value("${lera.socketio.port}")
    private Integer port;

    @Bean
    public SocketIOServer socketIOServer() {

        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setOrigin(null);

        // --- Final, Simplified Authorization Listener ---
        config.setAuthorizationListener(data -> {
            try {
                // The only source of truth: the 'token' URL query parameter.
                String token = data.getSingleUrlParam("token");

                if (token == null) {
                    log.warn("[Socket.IO] Auth failed: Token is missing from URL query parameters.");
                    return AuthorizationResult.FAILED_AUTHORIZATION;
                }

                if (jwtService.isValid(token)) {
                    log.info("[Socket.IO] Client authorized successfully via URL token.");
                    return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
                } else {
                    log.warn("[Socket.IO] Auth failed: Provided token from URL is invalid.");
                    return AuthorizationResult.FAILED_AUTHORIZATION;
                }
            } catch (Exception e) {
                log.error("[Socket.IO] An unexpected error occurred during authorization: {}", e.getMessage(), e);
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
        });

        // Standard configuration options
        config.setPingInterval(25_000);
        config.setPingTimeout(60_000);
        config.setMaxFramePayloadLength(1024 * 1024); // 1MB

        SocketIOServer server = new SocketIOServer(config);
        log.info("Socket.IO server configured on {}:{}", host, port);
        return server;
    }
}