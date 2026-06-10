package com.forgeStackk.EduResolve.config;

import com.forgeStackk.EduResolve.service.NcertBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes NCERT book data on application startup.
 * Syncs books from the local NCERT folder to the database if empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NcertDataInitializer implements CommandLineRunner {

    private final NcertBookService ncertBookService;

    @Override
    public void run(String... args) {
        log.info("Starting NCERT data initialization...");
        try {
            ncertBookService.syncBooksFromGitHub();
            log.info("NCERT data initialization completed successfully");
        } catch (Exception e) {
            log.error("Error during NCERT data initialization", e);
        }
    }
}
