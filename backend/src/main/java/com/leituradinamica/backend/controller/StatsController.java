package com.leituradinamica.backend.controller;

import com.leituradinamica.backend.dto.StatsDtos;
import com.leituradinamica.backend.service.AuthenticatedUserService;
import com.leituradinamica.backend.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;
    private final AuthenticatedUserService authenticatedUserService;

    public StatsController(StatsService statsService, AuthenticatedUserService authenticatedUserService) {
        this.statsService = statsService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<StatsDtos.DashboardResponse> dashboard() {
        return ResponseEntity.ok(statsService.dashboard(authenticatedUserService.getCurrentUser()));
    }

    @GetMapping("/monthly")
    public ResponseEntity<StatsDtos.SeriesResponse> monthly() {
        return ResponseEntity.ok(statsService.monthly(authenticatedUserService.getCurrentUser()));
    }

    @GetMapping("/yearly")
    public ResponseEntity<StatsDtos.SeriesResponse> yearly() {
        return ResponseEntity.ok(statsService.yearly(authenticatedUserService.getCurrentUser()));
    }
}