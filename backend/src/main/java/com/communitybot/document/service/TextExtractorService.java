package com.communitybot.document.service;

import com.communitybot.attachment.domain.AttachmentKind;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Extracts plain text from supported document types for FAQ ingestion.
 */
@Service
@Slf4j
public class TextExtractorService {

    public String extract(InputStream stream, AttachmentKind kind) throws IOException {
        return switch (kind) {
            case PDF  -> extractPdf(stream);
            case DOCX -> extractDocx(stream);
            case TXT, MD -> extractPlainText(stream);
            case JPEG -> throw new IOException("Image files cannot be ingested as FAQ text");
        };
    }

    private String extractPlainText(InputStream stream) throws IOException {
        String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        log.debug("Plain text extracted: {} chars", text.length());
        return text;
    }

    private String extractPdf(InputStream stream) throws IOException {
        // PDFBox 3.x requires reading into bytes first; Loader.loadPDF(byte[]) is the entry point
        byte[] bytes = stream.readAllBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.debug("PDF extracted: {} chars", text.length());
            return text;
        }
    }

    private String extractDocx(InputStream stream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(stream)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (!text.isBlank()) {
                    sb.append(text).append('\n');
                }
            }
            log.debug("DOCX extracted: {} chars", sb.length());
            return sb.toString();
        }
    }
}
