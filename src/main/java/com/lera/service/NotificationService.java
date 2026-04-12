package com.lera.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.lera.dto.response.NotificationDto;
import com.lera.exception.AppException;
import com.lera.model.Emergency;
import com.lera.model.Notification;
import com.lera.model.User;
import com.lera.repository.NotificationRepository;
import com.lera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final FirebaseApp firebaseApp;


    @Transactional
    public void createAndSend(User user, Emergency emergency, String title, String body) {

        Notification notification = Notification.builder()
                .user(user)
                .emergency(emergency)
                .title(title)
                .body(body)
                .isRead(false)
                .build();
        notificationRepo.save(notification);
        log.debug("Notification saved for user:{} title:{}", user.getId(), title);


        if (firebaseApp != null && user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            sendFcm(user.getFcmToken(), title, body, emergency != null ? emergency.getId() : null);
        }
    }


    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String userId) {
        return notificationRepo.findByUserIdOrderBySentAtDesc(userId)
                .stream()
                .map(NotificationDto::from)
                .toList();
    }


    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> AppException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Not your notification");
        }
        n.setRead(true);
        notificationRepo.save(n);
    }


    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepo.markAllReadByUserId(userId);
    }
    @Transactional
    public void delete(String id) {
        Notification notification = notificationRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Notification not found"));
        notificationRepo.delete(notification);
    }


    private void sendFcm(String token, String title, String body, String emergencyId) {
        try {
            Message.Builder builder = Message.builder()
                    .setToken(token)
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                    )
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build());

            if (emergencyId != null) {
                builder.putData("emergencyId", emergencyId);
                builder.putData("type", "emergency");
            }

            String response = FirebaseMessaging.getInstance(firebaseApp).send(builder.build());
            log.info("FCM push sent: {}", response);

        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed (non-fatal): {}", e.getMessage());
        } catch (Exception e) {
            log.warn("FCM unexpected error (non-fatal): {}", e.getMessage());
        }
    }
}