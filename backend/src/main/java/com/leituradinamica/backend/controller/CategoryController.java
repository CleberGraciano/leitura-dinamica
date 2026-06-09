package com.leituradinamica.backend.controller;

import com.leituradinamica.backend.dto.CategoryDtos;
import com.leituradinamica.backend.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDtos.CategoryResponse>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<CategoryDtos.CategoryResponse> create(@Valid @RequestBody CategoryDtos.CategoryRequest request) {
        return ResponseEntity.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<CategoryDtos.CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryDtos.CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}