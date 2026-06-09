package com.leituradinamica.backend.controller;

import com.leituradinamica.backend.dto.TrainingDtos;
import com.leituradinamica.backend.service.AuthenticatedUserService;
import com.leituradinamica.backend.service.TrainingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
public class TrainingController {

    private final TrainingService trainingService;
    private final AuthenticatedUserService authenticatedUserService;

    public TrainingController(TrainingService trainingService, AuthenticatedUserService authenticatedUserService) {
        this.trainingService = trainingService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/overview")
    public ResponseEntity<TrainingDtos.TrainingOverviewResponse> overview() {
        return ResponseEntity.ok(trainingService.overview(authenticatedUserService.getCurrentUser()));
    }

    @PostMapping("/execute")
    public ResponseEntity<TrainingDtos.TrainingSessionItem> execute(@Valid @RequestBody TrainingDtos.TrainingExecutionRequest request) {
        return ResponseEntity.ok(trainingService.execute(request, authenticatedUserService.getCurrentUser()));
    }
}package com.leituradinamica.backend.controller;

import com.leituradinamica.backend.dto.TrainingDtos;
import com.leituradinamica.backend.service.AuthenticatedUserService;
import com.leituradinamica.backend.service.TrainingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
public class TrainingController {

    private final TrainingService trainingService;
    private final AuthenticatedUserService authenticatedUserService;

    public TrainingController(TrainingService trainingService, AuthenticatedUserService authenticatedUserService) {
        this.trainingService = trainingService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/overview")
    public ResponseEntity<TrainingDtos.TrainingOverviewResponse> overview() {
        return ResponseEntity.ok(trainingService.overview(authenticatedUserService.getCurrentUser()));
    }

    @PostMapping("/sessions")
    public ResponseEntity<TrainingDtos.TrainingOverviewResponse> execute(@Valid @RequestBody TrainingDtos.TrainingExecutionRequest request) {
        return ResponseEntity.ok(trainingService.execute(request, authenticatedUserService.getCurrentUser()));
    }
}