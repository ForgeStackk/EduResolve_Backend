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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassLookupService {

    private final TeacherRepository teacherRepo;
    private final TeacherSubjectMappingRepository mappingRepo;
    private final ClassRoomRepository classRoomRepo;
    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final SubjectRepository subjectRepo;

    public List<ClassResponse> getMyClasses(UUID teacherId) {
        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new NoSuchElementException("Teacher not found: " + teacherId));

        List<UUID> classIds = mappingRepo.findByTeacherId(teacherId).stream()
                .map(m -> m.getClassId())
                .distinct().toList();

        return classIds.stream()
                .map(classRoomRepo::findById)
                .filter(opt -> opt.isPresent())
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

    public List<StudentWithParentResponse> getStudentsWithParents(UUID classId) {
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
