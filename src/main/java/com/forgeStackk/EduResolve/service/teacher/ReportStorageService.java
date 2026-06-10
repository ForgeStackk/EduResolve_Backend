package com.forgeStackk.EduResolve.service.teacher;

import java.util.UUID;

/**
 * Abstracts where generated PDFs are stored.
 * Swap the implementation bean to point at S3 / MinIO for production.
 */
public interface ReportStorageService {

    /**
     * Persist {@code pdfBytes} and return the addressable URL/path.
     */
    String store(UUID classId, int year, int month, UUID studentId, byte[] pdfBytes);
}
