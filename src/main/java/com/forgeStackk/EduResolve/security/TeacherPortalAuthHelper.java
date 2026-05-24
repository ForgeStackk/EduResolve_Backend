package com.forgeStackk.EduResolve.security;

import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.entity.teacher.Teacher;
import com.forgeStackk.EduResolve.enums.TeacherStatus;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import com.forgeStackk.EduResolve.repository.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TeacherPortalAuthHelper {

    private final TeacherRepository teacherRepo;
    private final UserLoginRepository userLoginRepo;

    public UUID resolveTeacherId() {
        Long userId = resolveUserId();
        return teacherRepo.findByUserId(userId)
                .orElseGet(() -> bootstrapTeacher(userId))
                .getTeacherId();
    }

    public String resolveSchoolName() {
        return userLoginRepo.findById(resolveUserId())
                .map(UserLogin::getSchoolName)
                .orElse(null);
    }

    public Long resolveUserLoginId() {
        return resolveUserId();
    }

    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return (Long) auth.getPrincipal();
    }

    // Creates a minimal Teacher row on first use for accounts that pre-date the
    // teacher-portal feature (avoids requiring every teacher to re-login).
    private Teacher bootstrapTeacher(Long userId) {
        UserLogin user = userLoginRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName())
                + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        Teacher teacher = new Teacher();
        teacher.setUserId(userId);
        teacher.setFullName(fullName.isEmpty() ? user.getEmail() : fullName);
        teacher.setEmail(user.getEmail());
        if (user.getPhoneNumber() != null) teacher.setPhone(user.getPhoneNumber());
        teacher.setStatus(TeacherStatus.ACTIVE);
        return teacherRepo.save(teacher);
    }
}
