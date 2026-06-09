package com.leituradinamica.backend.controller;

import com.leituradinamica.backend.dto.ReadingDtos;
import com.leituradinamica.backend.service.AuthenticatedUserService;
import com.leituradinamica.backend.service.ReadingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reading")
public class ReadingController {

    private final ReadingService readingService;
    private final AuthenticatedUserService authenticatedUserService;

    public ReadingController(ReadingService readingService, AuthenticatedUserService authenticatedUserService) {
        this.readingService = readingService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/start")
    public ResponseEntity<ReadingDtos.ReadingHistoryItem> start(@Valid @RequestBody ReadingDtos.ReadingCommand request) {
        return ResponseEntity.ok(readingService.start(request, authenticatedUserService.getCurrentUser()));
    }

    @PostMapping("/pause")
    public ResponseEntity<ReadingDtos.ReadingHistoryItem> pause(@Valid @RequestBody ReadingDtos.ReadingCommand request) {
        return ResponseEntity.ok(readingService.pause(request, authenticatedUserService.getCurrentUser()));
    }

    @PostMapping("/finish")
    public ResponseEntity<ReadingDtos.ReadingHistoryItem> finish(@Valid @RequestBody ReadingDtos.ReadingCommand request) {
        return ResponseEntity.ok(readingService.finish(request, authenticatedUserService.getCurrentUser()));
    }

    @GetMapping("/history")
    public ResponseEntity<ReadingDtos.ReadingHistoryResponse> history() {
        return ResponseEntity.ok(readingService.history(authenticatedUserService.getCurrentUser()));
    }
}