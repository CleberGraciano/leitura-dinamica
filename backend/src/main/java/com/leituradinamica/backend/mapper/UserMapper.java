package com.leituradinamica.backend.mapper;

import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.dto.UserDtos;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDtos.UserResponse toResponse(User user);
}