package com.leituradinamica.backend.dto;

import com.leituradinamica.backend.domain.enums.BookFileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public final class BookDtos {

    private BookDtos() {
    }

    public record BookRequest(
            @NotBlank @Size(max = 180) String title,
            String author,
            @Size(max = 2000) String description,
            String cover,
            String filePath,
            @NotNull BookFileType fileType,
            Long categoryId,
            Boolean favorite,
            Boolean archived,
            Boolean publicBook,
            String contentText
    ) {
    }

    public record BookResponse(
            Long id,
            String title,
            String author,
            String description,
            String cover,
            String filePath,
            BookFileType fileType,
            Long categoryId,
            String categoryName,
            Long userId,
            Instant createdAt,
            boolean favorite,
            boolean archived,
            boolean publicBook,
            int wordCount,
            String contentText
    ) {
    }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class BookUploadRequest {
                @NotBlank
                @Size(max = 180)
                private String title;

                private String author;

                @Size(max = 2000)
                private String description;

                private String cover;

                private String filePath;

                private BookFileType fileType;

                private Long categoryId;

                private Boolean favorite;

                private Boolean archived;

                private Boolean publicBook;

                private String contentText;

                private MultipartFile file;
        }
}