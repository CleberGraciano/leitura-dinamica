package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.RefreshToken;
import com.leituradinamica.backend.domain.entity.Subscription;
import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.domain.enums.Role;
import com.leituradinamica.backend.domain.enums.SubscriptionPlanType;
import com.leituradinamica.backend.domain.enums.SubscriptionStatus;
import com.leituradinamica.backend.dto.AuthDtos;
import com.leituradinamica.backend.exception.BusinessException;
import com.leituradinamica.backend.exception.ResourceNotFoundException;
import com.leituradinamica.backend.mapper.UserMapper;
import com.leituradinamica.backend.repository.RefreshTokenRepository;
import com.leituradinamica.backend.repository.SubscriptionRepository;
import com.leituradinamica.backend.repository.UserRepository;
import com.leituradinamica.backend.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            SubscriptionRepository subscriptionRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email já cadastrado.", HttpStatus.CONFLICT);
        }

        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .photo(request.photo())
                .role(request.role() == Role.ROLE_ADMIN ? Role.ROLE_USER : Role.ROLE_USER)
                .planType(SubscriptionPlanType.FREE)
                .active(true)
                .build());

        subscriptionRepository.save(Subscription.builder()
                .user(user)
                .planType(SubscriptionPlanType.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .paymentProvider("LOCAL")
                .startDate(Instant.now())
                .expirationDate(Instant.now().plus(3650, ChronoUnit.DAYS))
                .build());

        return buildTokenResponse(user);
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        return buildTokenResponse(user);
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token inválido.", HttpStatus.UNAUTHORIZED));
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Refresh token expirado.", HttpStatus.UNAUTHORIZED);
        }
        return buildTokenResponse(refreshToken.getUser());
    }

    private AuthDtos.TokenResponse buildTokenResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(jwtService.extractExpiration(refreshTokenValue))
                .build());
        return new AuthDtos.TokenResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                60L * 60,
                userMapper.toResponse(user)
        );
    }
}