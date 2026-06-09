package com.leituradinamica.backend.mapper;

import com.leituradinamica.backend.domain.entity.Category;
import com.leituradinamica.backend.dto.CategoryDtos;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDtos.CategoryResponse toResponse(Category category);
}