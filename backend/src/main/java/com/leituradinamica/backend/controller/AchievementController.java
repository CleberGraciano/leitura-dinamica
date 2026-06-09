package com.leituradinamica.backend.controller;

import com.leituradinamica.backend.dto.AchievementDtos;
import com.leituradinamica.backend.service.AchievementService;
import com.leituradinamica.backend.service.AuthenticatedUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;
    private final AuthenticatedUserService authenticatedUserService;

    public AchievementController(AchievementService achievementService, AuthenticatedUserService authenticatedUserService) {
        this.achievementService = achievementService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<AchievementDtos.AchievementResponse> list() {
        return ResponseEntity.ok(achievementService.listForUser(authenticatedUserService.getCurrentUser()));
    }
}package com.leituradinamica.backend.controller;

import com.leituradinamica.backend.dto.AchievementDtos;
import com.leituradinamica.backend.service.AchievementService;
import com.leituradinamica.backend.service.AuthenticatedUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;
    private final AuthenticatedUserService authenticatedUserService;

    public AchievementController(AchievementService achievementService, AuthenticatedUserService authenticatedUserService) {
        this.achievementService = achievementService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<AchievementDtos.AchievementResponse> list() {
        return ResponseEntity.ok(achievementService.listByUser(authenticatedUserService.getCurrentUser()));
    }
}