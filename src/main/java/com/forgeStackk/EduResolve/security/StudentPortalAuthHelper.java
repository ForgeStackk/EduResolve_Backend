package com.forgeStackk.EduResolve.security;

import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.repository.StudentProfileRepository;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import com.forgeStackk.EduResolve.repository.teacher.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StudentPortalAuthHelper {

    private final StudentRepository       studentRepo;
    private final StudentProfileRepository studentProfileRepo;
    private final UserLoginRepository      userLoginRepo;

    /** Returns the student's UUID (tp_student.student_id) — used by V4 inbox/attendance APIs. */
    public UUID resolveStudentId() {
        return studentRepo.findByUserId(resolveRawUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No student profile linked to this account"))
                .getStudentId();
    }

    /** Returns the user_login.id (BIGINT) — used by V9 submission and doubt APIs. */
    public Long resolveUserLoginId() {
        Long userId = resolveRawUserId();
        studentRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No student profile linked to this account"));
        return userId;
    }

    /** Returns student_profile.id (BIGINT) — used as FK in student_notes.student_id. */
    public Long resolveStudentProfileId() {
        Long userLoginId = resolveRawUserId();
        return studentProfileRepo.findByUserId(userLoginId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No student profile linked to this account"))
                .getId();
    }

    public String resolveSchoolName() {
        return userLoginRepo.findById(resolveRawUserId())
                .map(UserLogin::getSchoolName)
                .orElse(null);
    }

    private Long resolveRawUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return (Long) auth.getPrincipal();
    }
}
