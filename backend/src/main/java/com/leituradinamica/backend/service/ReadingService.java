package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.Book;
import com.leituradinamica.backend.domain.entity.Bookmark;
import com.leituradinamica.backend.domain.entity.ReadingSession;
import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.domain.enums.Role;
import com.leituradinamica.backend.domain.enums.ReadingSessionStatus;
import com.leituradinamica.backend.domain.enums.SubscriptionPlanType;
import com.leituradinamica.backend.dto.ReadingDtos;
import com.leituradinamica.backend.exception.BusinessException;
import com.leituradinamica.backend.repository.BookmarkRepository;
import com.leituradinamica.backend.repository.ReadingSessionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingService {

    private final ReadingSessionRepository readingSessionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final BookService bookService;
    private final AchievementService achievementService;

    public ReadingService(
            ReadingSessionRepository readingSessionRepository,
            BookmarkRepository bookmarkRepository,
            BookService bookService,
            AchievementService achievementService
    ) {
        this.readingSessionRepository = readingSessionRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.bookService = bookService;
        this.achievementService = achievementService;
    }

    @Transactional
    public ReadingDtos.ReadingHistoryItem start(ReadingDtos.ReadingCommand request, User user) {
        validateSpeedLimit(user, request.wpm());
        Book book = bookService.findOwnedBook(request.bookId(), user);
        ReadingSession session = saveSession(request, user, book, ReadingSessionStatus.STARTED);
        saveBookmark(request, user, book);
        return toHistoryItem(session, request.wordPosition());
    }

    @Transactional
    public ReadingDtos.ReadingHistoryItem pause(ReadingDtos.ReadingCommand request, User user) {
        validateSpeedLimit(user, request.wpm());
        Book book = bookService.findOwnedBook(request.bookId(), user);
        ReadingSession session = saveSession(request, user, book, ReadingSessionStatus.PAUSED);
        saveBookmark(request, user, book);
        return toHistoryItem(session, request.wordPosition());
    }

    @Transactional
    public ReadingDtos.ReadingHistoryItem finish(ReadingDtos.ReadingCommand request, User user) {
        validateSpeedLimit(user, request.wpm());
        Book book = bookService.findOwnedBook(request.bookId(), user);
        ReadingSession session = saveSession(request, user, book, ReadingSessionStatus.FINISHED);
        saveBookmark(request, user, book);
        achievementService.evaluate(user, request.wpm(), request.progress(), request.durationSeconds());
        return toHistoryItem(session, request.wordPosition());
    }

    public ReadingDtos.ReadingHistoryResponse history(User user) {
        List<ReadingDtos.ReadingHistoryItem> items = readingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(session -> {
                    Integer wordPosition = bookmarkRepository.findTopByUserIdAndBookIdOrderByCreatedAtDesc(user.getId(), session.getBook().getId())
                            .map(Bookmark::getWordPosition)
                            .orElse(0);
                    return toHistoryItem(session, wordPosition);
                })
                .toList();
        return new ReadingDtos.ReadingHistoryResponse(items);
    }

    private ReadingSession saveSession(ReadingDtos.ReadingCommand request, User user, Book book, ReadingSessionStatus status) {
        return readingSessionRepository.save(ReadingSession.builder()
                .user(user)
                .book(book)
                .wpm(request.wpm())
                .wordsRead(request.wordsRead())
                .durationSeconds(request.durationSeconds())
                .progress(request.progress())
                .status(status)
                .build());
    }

    private void saveBookmark(ReadingDtos.ReadingCommand request, User user, Book book) {
        bookmarkRepository.save(Bookmark.builder()
                .user(user)
                .book(book)
                .wordPosition(request.wordPosition())
                .lastWpm(request.wpm())
                .build());
    }

    private ReadingDtos.ReadingHistoryItem toHistoryItem(ReadingSession session, Integer wordPosition) {
        return new ReadingDtos.ReadingHistoryItem(
                session.getId(),
                session.getBook().getId(),
                session.getBook().getTitle(),
                session.getWpm(),
                session.getWordsRead(),
                session.getDurationSeconds(),
                session.getProgress(),
                session.getStatus(),
                session.getCreatedAt(),
                wordPosition
        );
    }

    private void validateSpeedLimit(User user, Integer wpm) {
        if (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_EDITOR) {
            return;
        }

        if (user.getPlanType() == SubscriptionPlanType.FREE && wpm > 500) {
            throw new BusinessException("Plano FREE permite até 500 WPM.");
        }
    }
}