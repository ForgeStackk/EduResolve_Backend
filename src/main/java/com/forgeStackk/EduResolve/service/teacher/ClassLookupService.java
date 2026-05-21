package com.forgeStackk.EduResolve.service.teacher;

import com.forgeStackk.EduResolve.dto.teacher.ClassResponse;
import com.forgeStackk.EduResolve.dto.teacher.StudentWithParentResponse;
import com.forgeStackk.EduResolve.dto.teacher.SubjectResponse;
import com.forgeStackk.EduResolve.entity.Subject;
import com.forgeStackk.EduResolve.entity.teacher.ClassRoom;
import com.forgeStackk.EduResolve.entity.teacher.Parent;
import com.forgeStackk.EduResolve.entity.teacher.Student;
import com.forgeStackk.EduResolve.entity.teacher.Teacher;
import com.forgeStackk.EduResolve.enums.StudentStatus;
import com.forgeStackk.EduResolve.repository.SubjectRepository;
import com.forgeStackk.EduResolve.repository.teacher.ClassRoomRepository;
import com.forgeStackk.EduResolve.repository.teacher.ParentRepository;
import com.forgeStackk.EduResolve.repository.teacher.StudentRepository;
import com.forgeStackk.EduResolve.repository.teacher.TeacherRepository;
import com.forgeStackk.EduResolve.repository.teacher.TeacherSubjectMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassLookupService {

    private final TeacherRepository teacherRepo;
    private final TeacherSubjectMappingRepository mappingRepo;
    private final ClassRoomRepository classRoomRepo;
    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final SubjectRepository subjectRepo;

    /** Returns all classrooms for the requesting teacher's school, sorted by grade then section. */
    public List<ClassResponse> getAllClasses(String schoolName) {
        List<ClassRoom> rooms = (schoolName != null && !schoolName.isBlank())
                ? classRoomRepo.findBySchoolName(schoolName)
                : classRoomRepo.findAll();
        return rooms.stream()
                .sorted(java.util.Comparator
                        .comparing(ClassRoom::getClassName)
                        .thenComparing(cr -> cr.getSection() == null ? "" : cr.getSection()))
                .map(cr -> new ClassResponse(cr.getClassId(), cr.getClassName(), cr.getSection(), false))
                .toList();
    }

    public List<ClassResponse> getMyClasses(UUID teacherId) {
        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new NoSuchElementException("Teacher not found: " + teacherId));

        // Start with subject-mapping classes, preserving insertion order
        Set<UUID> classIds = mappingRepo.findByTeacherId(teacherId).stream()
                .map(m -> m.getClassId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Always include the CT class even when no explicit subject mapping exists
        if (teacher.getClassTeacherOf() != null) {
            classIds.add(teacher.getClassTeacherOf());
        }

        return classIds.stream()
                .map(classRoomRepo::findById)
                .filter(Optional::isPresent)
                .map(opt -> {
                    ClassRoom cr = opt.get();
                    boolean isCT = cr.getClassId().equals(teacher.getClassTeacherOf());
                    return new ClassResponse(cr.getClassId(), cr.getClassName(), cr.getSection(), isCT);
                }).toList();
    }

    public List<SubjectResponse> getMySubjects(UUID teacherId, UUID classId) {
        return mappingRepo.findByTeacherIdAndClassId(teacherId, classId).stream()
                .map(m -> subjectRepo.findById(m.getSubjectId()))
                .filter(opt -> opt.isPresent())
                .map(opt -> {
                    Subject s = opt.get();
                    return new SubjectResponse(s.getId(), s.getName());
                }).toList();
    }

    public List<StudentWithParentResponse> getStudentsWithParents(UUID classId, String requestingSchool) {
        // Verify the classroom belongs to the requesting teacher's school
        classRoomRepo.findById(classId).ifPresent(cr -> {
            if (requestingSchool != null && cr.getSchoolName() != null
                    && !requestingSchool.equalsIgnoreCase(cr.getSchoolName())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Cannot view students of a class from a different school");
            }
        });

        List<Student> students = studentRepo.findByClassIdAndStatus(classId, StudentStatus.ACTIVE);
        return students.stream().map(s -> {
            String parentName = null;
            String parentPhone = null;
            if (s.getParentId() != null) {
                Parent p = parentRepo.findById(s.getParentId()).orElse(null);
                if (p != null) {
                    parentName = p.getFullName();
                    parentPhone = p.getPhone();
                }
            }
            return new StudentWithParentResponse(s.getStudentId(), s.getFullName(), s.getRollNumber(), parentName, parentPhone);
        }).toList();
    }
}
