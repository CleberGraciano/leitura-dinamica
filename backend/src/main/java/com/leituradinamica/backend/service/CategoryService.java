package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.Category;
import com.leituradinamica.backend.dto.CategoryDtos;
import com.leituradinamica.backend.exception.ResourceNotFoundException;
import com.leituradinamica.backend.mapper.CategoryMapper;
import com.leituradinamica.backend.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryDtos.CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CategoryRequest request) {
        Category category = categoryRepository.save(Category.builder()
                .name(request.name())
                .description(request.description())
                .build());
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.CategoryRequest request) {
        Category category = findEntityById(id);
        category.setName(request.name());
        category.setDescription(request.description());
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.delete(findEntityById(id));
    }

    public Category findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
    }
}