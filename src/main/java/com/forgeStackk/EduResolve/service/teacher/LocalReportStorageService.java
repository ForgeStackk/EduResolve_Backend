package com.forgeStackk.EduResolve.service.teacher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class LocalReportStorageService implements ReportStorageService {

    @Value("${reports.storage-dir:uploads/attendance-reports}")
    private String storageDir;

    @Override
    public String store(UUID classId, int year, int month, UUID studentId, byte[] pdfBytes) {
        Path dir = Paths.get(storageDir,
                classId.toString(),
                String.format("%04d-%02d", year, month));
        try {
            Files.createDirectories(dir);
            Path dest = dir.resolve(studentId + ".pdf");
            Files.write(dest, pdfBytes);
            log.debug("Stored attendance PDF: {}", dest);
            return dest.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store attendance PDF", e);
        }
    }
}
