package com.lera.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${lera.firebase.credentials-json:}")
    private String credentialsJson;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            if (credentialsJson != null && !credentialsJson.isBlank()) {
                try {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))
                    );
                    FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase initialized successfully");
                } catch (Exception e) {
                    log.warn("Firebase initialization failed: {}. Push notifications disabled.", e.getMessage());
                }
            } else {
                log.warn("FIREBASE_SERVICE_ACCOUNT_JSON not set — push notifications disabled");
            }
        }
        return FirebaseApp.getApps().isEmpty() ? null : FirebaseApp.getInstance();
    }
}
