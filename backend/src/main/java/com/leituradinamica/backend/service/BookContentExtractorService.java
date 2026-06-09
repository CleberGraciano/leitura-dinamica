package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.enums.BookFileType;
import com.leituradinamica.backend.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BookContentExtractorService {

    public ExtractedBookContent extract(MultipartFile file, BookFileType requestedType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Envie um arquivo PDF, EPUB ou TXT.", HttpStatus.BAD_REQUEST);
        }

        BookFileType fileType = requestedType != null ? requestedType : inferFileType(file.getOriginalFilename());
        String content = switch (fileType) {
            case PDF -> extractPdf(file);
            case EPUB -> extractEpub(file);
            case TXT, TEXT -> extractPlainText(file);
        };

        if (content.isBlank()) {
            throw new BusinessException("Nao foi possivel extrair texto do arquivo enviado.", HttpStatus.BAD_REQUEST);
        }

        return new ExtractedBookContent(fileType, file.getOriginalFilename(), normalizeWhitespace(content));
    }

    private String extractPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        } catch (IOException exception) {
            throw new BusinessException("Falha ao processar o PDF enviado.", HttpStatus.BAD_REQUEST, exception);
        }
    }

    private String extractEpub(MultipartFile file) {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = file.getInputStream(); ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory() || !isReadableEpubEntry(entry.getName())) {
                    continue;
                }

                String html = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                String text = Jsoup.parse(html).text();
                if (!text.isBlank()) {
                    if (content.length() > 0) {
                        content.append('\n');
                    }
                    content.append(text);
                }
            }
        } catch (IOException exception) {
            throw new BusinessException("Falha ao processar o EPUB enviado.", HttpStatus.BAD_REQUEST, exception);
        }

        return content.toString();
    }

    private String extractPlainText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException("Falha ao ler o arquivo de texto enviado.", HttpStatus.BAD_REQUEST, exception);
        }
    }

    private BookFileType inferFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException("Nao foi possivel identificar o tipo do arquivo enviado.", HttpStatus.BAD_REQUEST);
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "pdf" -> BookFileType.PDF;
            case "epub" -> BookFileType.EPUB;
            case "txt" -> BookFileType.TXT;
            case "text" -> BookFileType.TEXT;
            default -> throw new BusinessException("Formato de arquivo nao suportado: " + extension, HttpStatus.BAD_REQUEST);
        };
    }

    private boolean isReadableEpubEntry(String entryName) {
        String normalized = entryName.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".xhtml") || normalized.endsWith(".html") || normalized.endsWith(".htm") || normalized.endsWith(".xml");
    }

    private String normalizeWhitespace(String content) {
        return content.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    public record ExtractedBookContent(BookFileType fileType, String originalFileName, String contentText) {
    }
}