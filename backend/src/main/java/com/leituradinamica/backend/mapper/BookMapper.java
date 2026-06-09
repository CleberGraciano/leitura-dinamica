package com.leituradinamica.backend.mapper;

import com.leituradinamica.backend.domain.entity.Book;
import com.leituradinamica.backend.dto.BookDtos;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "wordCount", expression = "java(book.getContentText() == null || book.getContentText().isBlank() ? 0 : book.getContentText().trim().split(\"\\\\s+\").length)")
    BookDtos.BookResponse toResponse(Book book);
}