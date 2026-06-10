package com.forgeStackk.EduResolve.controller.teacher;

import com.forgeStackk.EduResolve.dto.teacher.ClassResponse;
import com.forgeStackk.EduResolve.dto.teacher.StudentWithParentResponse;
import com.forgeStackk.EduResolve.dto.teacher.SubjectResponse;
import com.forgeStackk.EduResolve.security.TeacherPortalAuthHelper;
import com.forgeStackk.EduResolve.service.teacher.ClassLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher-portal")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TeacherClassLookupController {

    private final ClassLookupService classLookupService;
    private final TeacherPortalAuthHelper authHelper;

    // GET /me — returns the resolved teacher UUID (used by frontend for WebSocket topic)
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me() {
        UUID teacherId = authHelper.resolveTeacherId();
        return ResponseEntity.ok(Map.of("teacherId", teacherId.toString()));
    }

    // GET /all-classes — classrooms scoped to the requesting teacher's school
    @GetMapping("/all-classes")
    public ResponseEntity<List<ClassResponse>> allClasses() {
        String school = authHelper.resolveSchoolName();
        return ResponseEntity.ok(classLookupService.getAllClasses(school));
    }

    // GET /my-classes
    @GetMapping("/my-classes")
    public ResponseEntity<?> myClasses() {
        UUID teacherId = authHelper.resolveTeacherId();
        try {
            List<ClassResponse> result = classLookupService.getMyClasses(teacherId);
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /my-subjects?classId=
    @GetMapping("/my-subjects")
    public ResponseEntity<List<SubjectResponse>> mySubjects(@RequestParam UUID classId) {
        UUID teacherId = authHelper.resolveTeacherId();
        return ResponseEntity.ok(classLookupService.getMySubjects(teacherId, classId));
    }

    // GET /class/{classId}/students
    @GetMapping("/class/{classId}/students")
    public ResponseEntity<List<StudentWithParentResponse>> studentsWithParents(
            @PathVariable UUID classId) {
        String school = authHelper.resolveSchoolName();
        return ResponseEntity.ok(classLookupService.getStudentsWithParents(classId, school));
    }
}
