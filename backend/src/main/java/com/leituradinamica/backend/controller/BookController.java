package com.leituradinamica.backend.controller;

import com.leituradinamica.backend.dto.BookDtos;
import com.leituradinamica.backend.service.AuthenticatedUserService;
import com.leituradinamica.backend.service.BookService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final AuthenticatedUserService authenticatedUserService;

    public BookController(BookService bookService, AuthenticatedUserService authenticatedUserService) {
        this.bookService = bookService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<List<BookDtos.BookResponse>> findAll() {
        return ResponseEntity.ok(bookService.findAllByUser(authenticatedUserService.getCurrentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDtos.BookResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.findById(id, authenticatedUserService.getCurrentUser()));
    }

    @PostMapping
    public ResponseEntity<BookDtos.BookResponse> create(@Valid @RequestBody BookDtos.BookRequest request) {
        return ResponseEntity.ok(bookService.create(request, authenticatedUserService.getCurrentUser()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookDtos.BookResponse> createMultipart(@Valid @ModelAttribute BookDtos.BookUploadRequest request) {
        return ResponseEntity.ok(bookService.create(request, authenticatedUserService.getCurrentUser()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDtos.BookResponse> update(@PathVariable Long id, @Valid @RequestBody BookDtos.BookRequest request) {
        return ResponseEntity.ok(bookService.update(id, request, authenticatedUserService.getCurrentUser()));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookDtos.BookResponse> updateMultipart(@PathVariable Long id, @Valid @ModelAttribute BookDtos.BookUploadRequest request) {
        return ResponseEntity.ok(bookService.update(id, request, authenticatedUserService.getCurrentUser()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id, authenticatedUserService.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}