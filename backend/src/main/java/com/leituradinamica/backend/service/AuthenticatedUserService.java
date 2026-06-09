package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.exception.ResourceNotFoundException;
import com.leituradinamica.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResourceNotFoundException("Usuário autenticado não encontrado.");
        }
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
    }
}