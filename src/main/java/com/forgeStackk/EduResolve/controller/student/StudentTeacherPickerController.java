package com.forgeStackk.EduResolve.controller.student;

import com.forgeStackk.EduResolve.dto.student.submission.TeacherPickerDto;
import com.forgeStackk.EduResolve.entity.teacher.ClassRoom;
import com.forgeStackk.EduResolve.entity.teacher.Teacher;
import com.forgeStackk.EduResolve.entity.teacher.TeacherSubjectMapping;
import com.forgeStackk.EduResolve.repository.SubjectRepository;
import com.forgeStackk.EduResolve.repository.teacher.StudentRepository;
import com.forgeStackk.EduResolve.repository.teacher.ClassRoomRepository;
import com.forgeStackk.EduResolve.repository.teacher.TeacherRepository;
import com.forgeStackk.EduResolve.repository.teacher.TeacherSubjectMappingRepository;
import com.forgeStackk.EduResolve.security.StudentPortalAuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/student-portal/teachers")
@RequiredArgsConstructor
public class StudentTeacherPickerController {

    private final StudentPortalAuthHelper        authHelper;
    private final TeacherRepository              teacherRepo;
    private final TeacherSubjectMappingRepository mappingRepo;
    private final ClassRoomRepository            classRoomRepo;
    private final SubjectRepository              subjectRepo;
    private final StudentRepository              studentRepo;

    /**
     * Returns all active teachers in the student's school as a safe picker projection.
     * Never exposes teacher email, phone, or internal UUIDs.
     */
    @GetMapping
    public ResponseEntity<List<TeacherPickerDto>> listForPicker() {
        Long studentUserId = authHelper.resolveUserLoginId();
        String schoolName  = authHelper.resolveSchoolName();

        if (schoolName == null || schoolName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your school information is not set — please complete your profile.");
        }

        // Resolve student's classId for "teaches my class" flag
        UUID studentClassId = studentRepo.findByUserId(studentUserId)
                .map(s -> s.getClassId())
                .orElse(null);

        List<Teacher> teachers = teacherRepo.findActiveBySchool(schoolName);

        // Pre-load all classrooms in this school keyed by UUID
        Map<UUID, ClassRoom> classRoomMap = classRoomRepo.findBySchoolName(schoolName)
                .stream().collect(Collectors.toMap(ClassRoom::getClassId, cr -> cr));

        // Pre-load all subjects once
        Map<Long, String> subjectNames = new HashMap<>();
        subjectRepo.findAll().forEach(s -> subjectNames.put(s.getId(), s.getName()));

        List<TeacherPickerDto> dtos = new ArrayList<>();
        for (Teacher t : teachers) {
            List<TeacherSubjectMapping> mappings = mappingRepo.findByTeacherId(t.getTeacherId());

            List<String> subjects = mappings.stream()
                    .map(m -> subjectNames.getOrDefault(m.getSubjectId(), ""))
                    .filter(n -> !n.isBlank())
                    .distinct()
                    .sorted()
                    .toList();

            List<String> classNames = mappings.stream()
                    .map(m -> classRoomMap.get(m.getClassId()))
                    .filter(Objects::nonNull)
                    .map(cr -> cr.getClassName() + "-" + cr.getSection())
                    .distinct()
                    .sorted()
                    .toList();

            boolean teachesMyClass = studentClassId != null && mappings.stream()
                    .anyMatch(m -> studentClassId.equals(m.getClassId()));

            dtos.add(new TeacherPickerDto(t.getUserId(), t.getFullName(),
                    subjects, classNames, teachesMyClass));
        }

        // Sort: "teaches my class" first, then alphabetical
        dtos.sort(Comparator.comparing(TeacherPickerDto::teachesMyClass).reversed()
                .thenComparing(TeacherPickerDto::name));

        return ResponseEntity.ok(dtos);
    }
}
