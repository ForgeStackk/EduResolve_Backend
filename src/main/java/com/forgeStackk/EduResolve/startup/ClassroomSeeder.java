package com.forgeStackk.EduResolve.startup;

import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.entity.teacher.ClassRoom;
import com.forgeStackk.EduResolve.entity.teacher.Parent;
import com.forgeStackk.EduResolve.entity.teacher.Student;
import com.forgeStackk.EduResolve.entity.teacher.Teacher;
import com.forgeStackk.EduResolve.enums.TeacherStatus;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import com.forgeStackk.EduResolve.repository.teacher.ClassRoomRepository;
import com.forgeStackk.EduResolve.repository.teacher.ParentRepository;
import com.forgeStackk.EduResolve.repository.teacher.StudentRepository;
import com.forgeStackk.EduResolve.repository.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassroomSeeder implements ApplicationRunner {

    private final ClassRoomRepository classRoomRepo;
    private final UserLoginRepository userLoginRepo;
    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final ParentRepository parentRepo;

    private static final int[]    GRADES   = {9, 10, 11, 12};
    private static final String[] SECTIONS = {"A", "B", "C", "D", "E"};
    private static final String   AY       = "2025-26";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedClassrooms();
        backfillStudents();
        backfillTeachers();
        backfillParents();
    }

    // ── Seed classrooms ────────────────────────────────────────────────────────

    private void seedClassrooms() {
        int created = 0;
        for (int grade : GRADES) {
            for (String section : SECTIONS) {
                String name = "Class " + grade;
                if (classRoomRepo.findByClassNameAndSection(name, section).isEmpty()) {
                    ClassRoom cr = new ClassRoom();
                    cr.setClassName(name);
                    cr.setSection(section);
                    cr.setAcademicYear(AY);
                    classRoomRepo.save(cr);
                    created++;
                }
            }
        }
        if (created > 0) {
            log.info("ClassroomSeeder: created {} classroom(s) for {}", created, AY);
        }
    }

    // ── Back-fill tp_student for existing student users ────────────────────────

    private void backfillStudents() {
        List<UserLogin> students = userLoginRepo.findAll().stream()
                .filter(u -> "student".equalsIgnoreCase(u.getRole()))
                .filter(u -> u.getClassName() != null && !u.getClassName().isBlank())
                .toList();

        int linked = 0;
        for (UserLogin u : students) {
            if (studentRepo.findByUserId(u.getId()).isPresent()) continue;

            Optional<ClassRoom> room = resolveRoom(u.getClassName());
            if (room.isEmpty()) continue;

            Student s = new Student();
            s.setUserId(u.getId());
            s.setFullName((u.getFirstName() + " " + u.getLastName()).trim());
            s.setClassId(room.get().getClassId());
            s.setRollNumber("S" + u.getId());
            studentRepo.save(s);
            linked++;
        }
        if (linked > 0) {
            log.info("ClassroomSeeder: back-filled {} tp_student record(s)", linked);
        }
    }

    // ── Back-fill teacher.class_teacher_of for existing teacher users ──────────

    private void backfillTeachers() {
        List<UserLogin> teachers = userLoginRepo.findAll().stream()
                .filter(u -> "teacher".equalsIgnoreCase(u.getRole()))
                .filter(u -> u.getClassName() != null && !u.getClassName().isBlank())
                .toList();

        int linked = 0;
        for (UserLogin u : teachers) {
            Teacher teacher = teacherRepo.findByUserId(u.getId()).orElse(null);
            if (teacher == null) {
                teacher = new Teacher();
                teacher.setUserId(u.getId());
                teacher.setFullName((u.getFirstName() + " " + u.getLastName()).trim());
                teacher.setEmail(u.getEmail());
                if (u.getPhoneNumber() != null) teacher.setPhone(u.getPhoneNumber());
                teacher.setStatus(TeacherStatus.ACTIVE);
            }
            if (teacher.getClassTeacherOf() != null) continue;

            Optional<ClassRoom> room = resolveRoom(u.getClassName());
            if (room.isEmpty()) continue;

            teacher.setClassTeacherOf(room.get().getClassId());
            teacherRepo.save(teacher);

            // Back-link classroom → class_teacher_id + stamp school_name
            ClassRoom cr = room.get();
            boolean dirty = false;
            if (cr.getClassTeacherId() == null) {
                cr.setClassTeacherId(teacher.getTeacherId());
                dirty = true;
            }
            if (cr.getSchoolName() == null && u.getSchoolName() != null) {
                cr.setSchoolName(u.getSchoolName());
                dirty = true;
            }
            if (dirty) classRoomRepo.save(cr);
            linked++;
        }
        if (linked > 0) {
            log.info("ClassroomSeeder: back-filled {} teacher classroom link(s)", linked);
        }
    }

    // ── Back-fill tp_parent for existing parent users ──────────────────────────

    private void backfillParents() {
        List<UserLogin> parents = userLoginRepo.findAll().stream()
                .filter(u -> "parent".equalsIgnoreCase(u.getRole()))
                .toList();

        int linked = 0;
        for (UserLogin u : parents) {
            if (parentRepo.findByUserId(u.getId()).isPresent()) continue;

            Parent p = new Parent();
            p.setUserId(u.getId());
            p.setFullName((u.getFirstName() + " " + u.getLastName()).trim());
            p.setEmail(u.getEmail());
            p.setPhone(u.getPhoneNumber() != null ? u.getPhoneNumber() : "");
            parentRepo.save(p);
            linked++;
        }
        if (linked > 0) {
            log.info("ClassroomSeeder: back-filled {} tp_parent record(s)", linked);
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Optional<ClassRoom> resolveRoom(String className) {
        if (className == null || className.isBlank()) return Optional.empty();
        String cn      = className.trim();
        String grade   = cn.replaceAll("[^0-9]", "");
        String section = cn.replaceAll("[0-9]", "").toUpperCase();
        if (grade.isBlank()) return Optional.empty();
        return (!section.isEmpty())
                ? classRoomRepo.findByClassNameAndSection("Class " + grade, section)
                : classRoomRepo.findByClassName("Class " + grade).stream().findFirst();
    }
}
