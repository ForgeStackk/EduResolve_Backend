package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.teacher.StudentOptionDto;
import com.forgeStackk.EduResolve.entity.teacher.ClassRoom;
import com.forgeStackk.EduResolve.enums.StudentStatus;
import com.forgeStackk.EduResolve.repository.teacher.ClassRoomRepository;
import com.forgeStackk.EduResolve.repository.teacher.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public (no-auth) lookup used by the signup form to populate the
 * parent "child's name" dropdown.  Sits under /api/auth/** which is
 * already permit-all in WebSecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PublicStudentLookupController {

    private final ClassRoomRepository classRoomRepository;
    private final StudentRepository   studentRepository;

    /**
     * Resolves the classId UUID for a given grade + section.
     * Used by the teacher message composer to target any class.
     * Returns {"classId":"<uuid>"} or 404 if the classroom doesn't exist.
     */
    @GetMapping("/classroom-id")
    public ResponseEntity<Map<String, String>> getClassroomId(
            @RequestParam String grade,
            @RequestParam(required = false) String section) {

        String className = "Class " + grade;
        java.util.Optional<ClassRoom> room;
        if (section != null && !section.isBlank() && !"none".equalsIgnoreCase(section)) {
            room = classRoomRepository.findByClassNameAndSection(className, section.toUpperCase());
        } else {
            List<ClassRoom> rooms = classRoomRepository.findByClassName(className);
            room = rooms.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(rooms.get(0));
        }
        return room
                .map(cr -> ResponseEntity.ok(Map.of("classId", cr.getClassId().toString())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/students-by-grade/{grade}")
    public ResponseEntity<List<StudentOptionDto>> getByGrade(
            @PathVariable String grade,
            @RequestParam(required = false) String section) {

        List<ClassRoom> rooms;
        if (section != null && !section.isBlank() && !"none".equalsIgnoreCase(section)) {
            rooms = classRoomRepository
                    .findByClassNameAndSection("Class " + grade, section.toUpperCase())
                    .map(java.util.List::of)
                    .orElse(java.util.List.of());
        } else {
            rooms = classRoomRepository.findByClassName("Class " + grade);
        }

        List<StudentOptionDto> result = rooms.stream()
                .flatMap(room -> studentRepository
                        .findByClassIdAndStatus(room.getClassId(), StudentStatus.ACTIVE)
                        .stream())
                .map(s -> new StudentOptionDto(
                        s.getStudentId().toString(),
                        s.getFullName(),
                        s.getRollNumber()))
                .toList();
        return ResponseEntity.ok(result);
    }
}
