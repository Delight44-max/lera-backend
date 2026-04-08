package com.lera.config;

import com.corundumstudio.socketio.*;
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
        config.setOrigin("*");


        config.setAuthorizationListener(data -> {
            try {
                String token = data.getSingleUrlParam("token");
                if (token == null) {
                    String authHeader = data.getHttpHeaders().get("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                    }
                }

                if (token != null && jwtService.isValid(token)) {

                    return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
                }
            } catch (Exception e) {
                log.debug("Socket auth failed: {}", e.getMessage());
            }

            return AuthorizationResult.FAILED_AUTHORIZATION;
        });


        config.setPingInterval(25_000);
        config.setPingTimeout(60_000);
        config.setMaxFramePayloadLength(1024 * 1024);

        SocketIOServer server = new SocketIOServer(config);
        log.info("Socket.IO configured on {}:{}", host, port);
        return server;
    }
}