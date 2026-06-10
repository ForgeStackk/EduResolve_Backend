package com.forgeStackk.EduResolve.notes.service;

import com.forgeStackk.EduResolve.notes.entity.StudentNote;
import com.forgeStackk.EduResolve.notes.repository.StudentNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotesArchiveJob {

    private final StudentNoteRepository noteRepo;

    @Value("${notes.trash-retention-days:30}")
    private int trashRetentionDays;

    @Scheduled(cron = "${notes.archive-cron:0 0 1 * * ?}")
    public void archiveOldTrash() {
        Instant cutoff = Instant.now().minus(trashRetentionDays, ChronoUnit.DAYS);
        List<StudentNote> eligible = noteRepo.findTrashReadyToArchive(cutoff);

        if (eligible.isEmpty()) {
            log.debug("Notes archive job: nothing to archive.");
            return;
        }

        List<Long> ids = eligible.stream().map(StudentNote::getId).toList();
        noteRepo.archiveByIds(ids);
        log.info("Notes archive job: archived {} notes with deletedAt < {}", ids.size(), cutoff);
    }
}
