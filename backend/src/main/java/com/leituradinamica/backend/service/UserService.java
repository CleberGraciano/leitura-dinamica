package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.dto.UserDtos;
import com.leituradinamica.backend.exception.BusinessException;
import com.leituradinamica.backend.exception.ResourceNotFoundException;
import com.leituradinamica.backend.mapper.UserMapper;
import com.leituradinamica.backend.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDtos.UserResponse> findAll() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    public UserDtos.UserResponse toResponse(User user) {
        return userMapper.toResponse(user);
    }

    public UserDtos.UserResponse findById(Long id) {
        return userMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public UserDtos.UserResponse update(Long id, UserDtos.UpdateUserRequest request) {
        User user = findEntityById(id);
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email já utilizado por outro usuário.", HttpStatus.CONFLICT);
        }
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhoto(request.photo());
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.planType() != null) {
            user.setPlanType(request.planType());
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(findEntityById(id));
    }

    @Transactional
    public void changePassword(User user, UserDtos.ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Senha atual inválida.");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    public User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }
}