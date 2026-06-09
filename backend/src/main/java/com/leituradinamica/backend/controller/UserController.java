package com.leituradinamica.backend.controller;

import com.leituradinamica.backend.dto.UserDtos;
import com.leituradinamica.backend.service.AuthenticatedUserService;
import com.leituradinamica.backend.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthenticatedUserService authenticatedUserService;

    public UserController(UserService userService, AuthenticatedUserService authenticatedUserService) {
        this.userService = userService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDtos.UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDtos.UserResponse> me() {
        return ResponseEntity.ok(userService.toResponse(authenticatedUserService.getCurrentUser()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDtos.UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDtos.UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody UserDtos.ChangePasswordRequest request) {
        userService.changePassword(authenticatedUserService.getCurrentUser(), request);
        return ResponseEntity.noContent().build();
    }
}