package org.example.panhabackend.dto.report;

public record ExportedFile(String fileName, String contentType, byte[] content) {
}
