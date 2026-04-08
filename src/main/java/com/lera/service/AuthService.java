package com.lera.service;

import com.lera.dto.request.*;
import com.lera.dto.response.*;
import com.lera.exception.AppException;
import com.lera.model.*;
import com.lera.repository.*;
import com.lera.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepo;
    private final ResponderProfileRepository responderProfileRepo;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;


    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw AppException.conflict("Email already in use");
        }
        if (userRepo.existsByPhoneNumber(req.getPhoneNumber())) {
            throw AppException.conflict("Phone number already in use");
        }
        if (req.getRole() == Role.responder) {
            if (req.getCertificationId() == null || req.getCertificationId().isBlank()) {
                throw AppException.badRequest("Certification ID is required for responders");
            }
            if (req.getResponderType() == null) {
                throw AppException.badRequest("Responder type is required for responders");
            }
        }

        User user = User.builder()
            .fullName(req.getFullName())
            .email(req.getEmail())
            .phoneNumber(req.getPhoneNumber())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .role(req.getRole())
            .build();
        userRepo.save(user);

        if (req.getRole() == Role.responder) {
            ResponderProfile profile = ResponderProfile.builder()
                .user(user)
                .type(req.getResponderType())
                .certificationId(req.getCertificationId())
                .availability(Availability.offline)
                .build();
            responderProfileRepo.save(profile);
        }

        log.info("User registered: {} ({})", user.getEmail(), user.getRole());
        return buildAuthResponse(user);
    }


    public AuthResponse login(LoginRequest req) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (BadCredentialsException e) {
            throw AppException.unauthorized("Invalid email or password");
        }
        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> AppException.unauthorized("Invalid email or password"));
        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }


    @Transactional
    public AuthResponse refresh(RefreshTokenRequest req) {
        RefreshToken rotated = refreshTokenService.rotate(req.getRefreshToken());
        User user = rotated.getUser();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        log.info("Token refreshed for user:{}", user.getId());
        return AuthResponse.builder()
            .token(accessToken)
            .refreshToken(rotated.getToken())
            .expiresIn(jwtService.getAccessExpirationSeconds())
            .user(UserDto.from(user))
            .build();
    }


    @Transactional
    public void logout(String userId, LogoutRequest req) {

        refreshTokenService.revokeAll(userId);
        log.info("User logged out: {}", userId);
    }


    public UserDto getMe(String userId) {
        return UserDto.from(userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("User not found")));
    }

    @Transactional
    public UserDto updateMe(String userId, UpdateMeRequest req) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("User not found"));
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName());
        }
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank()) {
            if (!user.getPhoneNumber().equals(req.getPhoneNumber()) &&
                userRepo.existsByPhoneNumber(req.getPhoneNumber())) {
                throw AppException.conflict("Phone number already in use");
            }
            user.setPhoneNumber(req.getPhoneNumber());
        }
        return UserDto.from(userRepo.save(user));
    }

    @Transactional
    public void updateFcmToken(String userId, String fcmToken) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("User not found"));
        user.setFcmToken(fcmToken);
        userRepo.save(user);
    }

    // ── Helper ─────────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        RefreshToken refresh = refreshTokenService.create(user);
        return AuthResponse.builder()
            .token(accessToken)
            .refreshToken(refresh.getToken())
            .expiresIn(jwtService.getAccessExpirationSeconds())
            .user(UserDto.from(user))
            .build();
    }
}
