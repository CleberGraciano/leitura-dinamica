package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.Book;
import com.leituradinamica.backend.domain.entity.Category;
import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.domain.enums.Role;
import com.leituradinamica.backend.domain.enums.SubscriptionPlanType;
import com.leituradinamica.backend.dto.BookDtos;
import com.leituradinamica.backend.exception.BusinessException;
import com.leituradinamica.backend.exception.ResourceNotFoundException;
import com.leituradinamica.backend.mapper.BookMapper;
import com.leituradinamica.backend.repository.BookRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final CategoryService categoryService;
    private final BookContentExtractorService bookContentExtractorService;

    public BookService(
            BookRepository bookRepository,
            BookMapper bookMapper,
            CategoryService categoryService,
            BookContentExtractorService bookContentExtractorService
    ) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
        this.categoryService = categoryService;
        this.bookContentExtractorService = bookContentExtractorService;
    }

    @Transactional(readOnly = true)
    public List<BookDtos.BookResponse> findAllByUser(User user) {
        return bookRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(bookMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BookDtos.BookResponse findById(Long id, User user) {
        return bookMapper.toResponse(findOwnedBook(id, user));
    }

    @Transactional
    public BookDtos.BookResponse create(BookDtos.BookRequest request, User user) {
        validateCreationLimit(user);
        Category category = request.categoryId() == null ? null : categoryService.findEntityById(request.categoryId());
        Book book = bookRepository.save(Book.builder()
                .title(request.title())
                .author(request.author())
                .description(request.description())
                .cover(request.cover())
                .filePath(request.filePath())
                .fileType(request.fileType())
                .category(category)
                .user(user)
                .favorite(Boolean.TRUE.equals(request.favorite()))
                .archived(Boolean.TRUE.equals(request.archived()))
                .publicBook(Boolean.TRUE.equals(request.publicBook()))
                .contentText(request.contentText())
                .build());
        return bookMapper.toResponse(book);
    }

            @Transactional
            public BookDtos.BookResponse create(BookDtos.BookUploadRequest request, User user) {
            validateCreationLimit(user);
            Category category = request.getCategoryId() == null ? null : categoryService.findEntityById(request.getCategoryId());
            ResolvedBookData resolvedBookData = resolveBookData(
                request.getFile(),
                request.getFileType(),
                request.getFilePath(),
                request.getContentText(),
                null,
                null,
                true
            );

            Book book = bookRepository.save(Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .description(request.getDescription())
                .cover(request.getCover())
                .filePath(resolvedBookData.filePath())
                .fileType(resolvedBookData.fileType())
                .category(category)
                .user(user)
                .favorite(Boolean.TRUE.equals(request.getFavorite()))
                .archived(Boolean.TRUE.equals(request.getArchived()))
                .publicBook(Boolean.TRUE.equals(request.getPublicBook()))
                .contentText(resolvedBookData.contentText())
                .build());
            return bookMapper.toResponse(book);
            }

    @Transactional
    public BookDtos.BookResponse update(Long id, BookDtos.BookRequest request, User user) {
        Book book = findOwnedBook(id, user);
        Category category = request.categoryId() == null ? null : categoryService.findEntityById(request.categoryId());
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setDescription(request.description());
        book.setCover(request.cover());
        book.setFilePath(request.filePath());
        book.setFileType(request.fileType());
        book.setCategory(category);
        book.setFavorite(Boolean.TRUE.equals(request.favorite()));
        book.setArchived(Boolean.TRUE.equals(request.archived()));
        book.setPublicBook(Boolean.TRUE.equals(request.publicBook()));
        book.setContentText(request.contentText());
        return bookMapper.toResponse(book);
    }

    @Transactional
    public BookDtos.BookResponse update(Long id, BookDtos.BookUploadRequest request, User user) {
        Book book = findOwnedBook(id, user);
        Category category = request.getCategoryId() == null ? null : categoryService.findEntityById(request.getCategoryId());
        ResolvedBookData resolvedBookData = resolveBookData(
                request.getFile(),
                request.getFileType(),
                request.getFilePath(),
                request.getContentText(),
                book.getFilePath(),
                book.getContentText(),
                false
        );

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setDescription(request.getDescription());
        book.setCover(request.getCover());
        book.setFilePath(resolvedBookData.filePath());
        book.setFileType(resolvedBookData.fileType());
        book.setCategory(category);
        book.setFavorite(Boolean.TRUE.equals(request.getFavorite()));
        book.setArchived(Boolean.TRUE.equals(request.getArchived()));
        book.setPublicBook(Boolean.TRUE.equals(request.getPublicBook()));
        book.setContentText(resolvedBookData.contentText());
        return bookMapper.toResponse(book);
    }

    @Transactional
    public void delete(Long id, User user) {
        bookRepository.delete(findOwnedBook(id, user));
    }

    public Book findOwnedBook(Long id, User user) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado."));
        if (!book.getUser().getId().equals(user.getId()) && !book.isPublicBook()) {
            throw new ResourceNotFoundException("Livro não encontrado para o usuário atual.");
        }
        return book;
    }

    private void validateCreationLimit(User user) {
        if (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_EDITOR) {
            return;
        }

        if (user.getPlanType() == SubscriptionPlanType.FREE && bookRepository.countByUserId(user.getId()) >= 3) {
            throw new BusinessException("Plano FREE permite no máximo 3 livros cadastrados.");
        }
    }

    private ResolvedBookData resolveBookData(
            MultipartFile file,
            com.leituradinamica.backend.domain.enums.BookFileType fileType,
            String filePath,
            String contentText,
            String currentFilePath,
            String currentContentText,
            boolean createOperation
    ) {
        if (file != null && !file.isEmpty()) {
            BookContentExtractorService.ExtractedBookContent extractedBookContent = bookContentExtractorService.extract(file, fileType);
            return new ResolvedBookData(
                    extractedBookContent.fileType(),
                    extractedBookContent.originalFileName(),
                    extractedBookContent.contentText()
            );
        }

        String resolvedContent = contentText != null ? contentText.trim() : currentContentText;
        if (resolvedContent == null || resolvedContent.isBlank()) {
            throw new BusinessException(
                    createOperation ? "Informe um conteudo manual ou envie um arquivo para o livro." : "O livro precisa manter um conteudo valido.",
                    HttpStatus.BAD_REQUEST
            );
        }

        com.leituradinamica.backend.domain.enums.BookFileType resolvedFileType = fileType != null ? fileType : inferManualFileType(filePath, currentFilePath);
        return new ResolvedBookData(resolvedFileType, firstNonBlank(filePath, currentFilePath), resolvedContent);
    }

    private com.leituradinamica.backend.domain.enums.BookFileType inferManualFileType(String filePath, String currentFilePath) {
        String candidate = firstNonBlank(filePath, currentFilePath);
        if (candidate == null || candidate.isBlank()) {
            return com.leituradinamica.backend.domain.enums.BookFileType.TEXT;
        }

        String normalized = candidate.toLowerCase();
        if (normalized.endsWith(".pdf")) {
            return com.leituradinamica.backend.domain.enums.BookFileType.PDF;
        }
        if (normalized.endsWith(".epub")) {
            return com.leituradinamica.backend.domain.enums.BookFileType.EPUB;
        }
        if (normalized.endsWith(".txt")) {
            return com.leituradinamica.backend.domain.enums.BookFileType.TXT;
        }
        return com.leituradinamica.backend.domain.enums.BookFileType.TEXT;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback != null && !fallback.isBlank() ? fallback.trim() : null;
    }

    private record ResolvedBookData(
            com.leituradinamica.backend.domain.enums.BookFileType fileType,
            String filePath,
            String contentText
    ) {
    }
}