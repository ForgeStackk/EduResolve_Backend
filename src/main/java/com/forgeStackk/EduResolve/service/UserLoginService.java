package com.forgeStackk.EduResolve.service;

import com.forgeStackk.EduResolve.dto.ForgotPasswordRequest;
import com.forgeStackk.EduResolve.dto.ForgotPasswordResponse;
import com.forgeStackk.EduResolve.dto.LoginRequest;
import com.forgeStackk.EduResolve.dto.LoginResponse;
import com.forgeStackk.EduResolve.dto.RegisterRequest;
import com.forgeStackk.EduResolve.dto.ResetPasswordRequest;
import com.forgeStackk.EduResolve.dto.ResetPasswordResponse;
import com.forgeStackk.EduResolve.entity.AdminProfile;
import com.forgeStackk.EduResolve.entity.ParentsProfile;
import com.forgeStackk.EduResolve.entity.StudentProfile;
import com.forgeStackk.EduResolve.entity.TeacherProfile;
import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.entity.teacher.ClassRoom;
import com.forgeStackk.EduResolve.entity.teacher.Parent;
import com.forgeStackk.EduResolve.entity.teacher.Student;
import com.forgeStackk.EduResolve.entity.teacher.Teacher;
import com.forgeStackk.EduResolve.enums.TeacherStatus;
import com.forgeStackk.EduResolve.repository.AdminProfileRepository;
import com.forgeStackk.EduResolve.repository.ParentsProfileRepository;
import com.forgeStackk.EduResolve.repository.StudentProfileRepository;
import com.forgeStackk.EduResolve.repository.TeacherProfileRepository;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import com.forgeStackk.EduResolve.repository.teacher.ClassRoomRepository;
import com.forgeStackk.EduResolve.repository.teacher.ParentRepository;
import com.forgeStackk.EduResolve.repository.teacher.StudentRepository;
import com.forgeStackk.EduResolve.repository.teacher.TeacherRepository;
import com.forgeStackk.EduResolve.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserLoginService {

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TeacherProfileRepository teacherProfileRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdminProfileRepository adminProfileRepository;

    @Autowired
    private ParentsProfileRepository parentsProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Register a new user (first-time login)
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        LoginResponse response = new LoginResponse();

        // Check if username already exists (only when an explicit username is supplied)
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && userLoginRepository.existsByUsername(request.getUsername())) {
            response.setSuccess(false);
            response.setMessage("Username already taken. Please choose another.");
            return response;
        }

        try {
            // Resolve firstName / lastName from combined name if needed
            String firstName = request.getFirstName();
            String lastName = request.getLastName();
            if ((firstName == null || firstName.isBlank()) && request.getName() != null && !request.getName().isBlank()) {
                String[] parts = request.getName().trim().split("\\s+", 2);
                firstName = parts[0];
                lastName = parts.length > 1 ? parts[1] : "";
            }
            // Use email as username when no explicit username is supplied
            String username = (request.getUsername() != null && !request.getUsername().isBlank())
                    ? request.getUsername() : request.getEmail();

            // Create new user
            UserLogin user = new UserLogin();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            if (request.getClassName() != null && !request.getClassName().trim().isEmpty()) {
                user.setClassName(request.getClassName());
            }
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            // Public registration always creates a STUDENT. Role escalation is prevented here.
            user.setRole("student");
            user.setPhoneNumber(request.getPhoneNumber());
            if (request.getSchoolName() != null && !request.getSchoolName().trim().isEmpty()) {
                user.setSchoolName(request.getSchoolName());
            }

            // Save user — if this succeeds and student profile save below fails,
            // @Transactional rolls back both together so no orphaned user_login rows.
            UserLogin savedUser = userLoginRepository.save(user);

            // Create the role-specific profile row before returning success.
            setProfileId(response, savedUser);

            // Parent: link tp_student.parent_id via Parent.students collection
            if ("parent".equalsIgnoreCase(savedUser.getRole())
                    && request.getStudentId() != null && !request.getStudentId().isBlank()) {
                try {
                    java.util.UUID studentUuid = java.util.UUID.fromString(request.getStudentId());
                    parentRepository.findByUserId(savedUser.getId()).ifPresent(parent -> {
                        studentRepository.findById(studentUuid).ifPresent(student -> {
                            if (student.getParentId() == null) {
                                parent.getStudents().add(student);
                                parentRepository.save(parent);
                            }
                        });
                    });
                } catch (Exception e) {
                    System.err.println("Warning: could not link parent to student: " + e.getMessage());
                }
            }

            response.setId(savedUser.getId());
            response.setFirstName(savedUser.getFirstName());
            response.setLastName(savedUser.getLastName());
            response.setUsername(savedUser.getUsername());
            response.setEmail(savedUser.getEmail());
            response.setRole(savedUser.getRole());
            response.setClassName(savedUser.getClassName());
            response.setPhoneNumber(savedUser.getPhoneNumber());
            response.setSchoolName(savedUser.getSchoolName());
            response.setSuccess(true);
            response.setMessage("Registration successful!");
            response.setToken(jwtUtil.generate(savedUser.getId(), savedUser.getRole()));

            return response;
        } catch (Exception e) {
            // Mark transaction for rollback so user_login is not committed without its profile
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.setSuccess(false);
            response.setMessage("Registration failed: " + e.getMessage());
            return response;
        }
    }

    /**
     * Login existing user
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        LoginResponse response = new LoginResponse();

        // Support login by username (frontend) or email (API clients)
        Optional<UserLogin> userOptional = (request.getUsername() != null && !request.getUsername().isBlank())
                ? userLoginRepository.findByUsername(request.getUsername())
                : userLoginRepository.findFirstByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Account not found. Please register first.");
            return response;
        }

        UserLogin user = userOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.setSuccess(false);
            response.setMessage("Invalid password. Please try again.");
            return response;
        }

        // Ensure the role-specific profile row exists.
        // A profile-creation failure must not block a valid login.
        try {
            setProfileId(response, user);
        } catch (Exception e) {
            // Profile upsert failed (e.g. table missing before first restart) — log and continue
            System.err.println("Warning: could not upsert profile for userId=" + user.getId() + ": " + e.getMessage());
        }

        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setClassName(user.getClassName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setSchoolName(user.getSchoolName());
        response.setSuccess(true);
        response.setMessage("Login successful!");
        response.setToken(jwtUtil.generate(user.getId(), user.getRole()));

        return response;
    }

    private void setProfileId(LoginResponse response, UserLogin user) {
        String role = user.getRole();
        if ("student".equalsIgnoreCase(role)) {
            response.setStudentId(upsertStudentProfile(user).getId().intValue());
        } else if ("teacher".equalsIgnoreCase(role)) {
            response.setTeacherId(upsertTeacherProfile(user).getId().intValue());
        } else if ("admin".equalsIgnoreCase(role)) {
            response.setAdminId(upsertAdminProfile(user).getId().intValue());
        } else if ("parent".equalsIgnoreCase(role)) {
            response.setParentId(upsertParentsProfile(user).getId().intValue());
        }
    }

    private StudentProfile upsertStudentProfile(UserLogin user) {
        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();
        String initials = buildInitials(user.getFirstName(), user.getLastName());

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseGet(StudentProfile::new);

        profile.setUserId(user.getId());
        profile.setName(fullName);
        profile.setInitials(initials);
        profile.setClassName(user.getClassName());

        StudentProfile saved = studentProfileRepository.save(profile);

        // Also sync tp_student so the teacher portal (attendance, messaging) can see this student
        if (user.getClassName() != null && !user.getClassName().trim().isEmpty()) {
            String cn      = user.getClassName().trim();
            String grade   = cn.replaceAll("[^0-9]", "");
            String section = cn.replaceAll("[0-9]", "").toUpperCase();
            java.util.Optional<ClassRoom> roomOpt = (!section.isEmpty())
                    ? classRoomRepository.findByClassNameAndSection("Class " + grade, section)
                    : classRoomRepository.findByClassName("Class " + grade).stream().findFirst();

            roomOpt.ifPresent(room -> {
                Student student = studentRepository.findByUserId(user.getId())
                        .orElseGet(Student::new);
                student.setUserId(user.getId());
                student.setFullName(fullName);
                student.setClassId(room.getClassId());
                if (student.getRollNumber() == null || student.getRollNumber().isBlank()) {
                    student.setRollNumber("S" + user.getId());
                }
                studentRepository.save(student);
            });
        }

        return saved;
    }

    private TeacherProfile upsertTeacherProfile(UserLogin user) {
        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();

        TeacherProfile profile = teacherProfileRepository.findByUserId(user.getId())
                .orElseGet(TeacherProfile::new);

        profile.setUserId(user.getId());
        profile.setName(fullName);
        profile.setInitials(buildInitials(user.getFirstName(), user.getLastName()));
        profile.setClassName(user.getClassName());
        if (profile.getStatus() == null) {
            profile.setStatus("active");
        }

        // Also keep the Teacher entity (used by teacher-portal endpoints) in sync
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseGet(Teacher::new);
        teacher.setUserId(user.getId());
        teacher.setFullName(fullName);
        teacher.setEmail(user.getEmail());
        if (user.getPhoneNumber() != null) teacher.setPhone(user.getPhoneNumber());
        if (teacher.getStatus() == null) teacher.setStatus(TeacherStatus.ACTIVE);

        // Link CT classroom from className (e.g. "9A", "10B", or legacy "9")
        if (user.getClassName() != null && !user.getClassName().trim().isEmpty()) {
            String cn      = user.getClassName().trim();
            String grade   = cn.replaceAll("[^0-9]", "");
            String section = cn.replaceAll("[0-9]", "").toUpperCase();
            java.util.Optional<ClassRoom> room = (!section.isEmpty())
                    ? classRoomRepository.findByClassNameAndSection("Class " + grade, section)
                    : classRoomRepository.findByClassName("Class " + grade).stream().findFirst();
            room.ifPresent(cr -> teacher.setClassTeacherOf(cr.getClassId()));
        }

        Teacher savedTeacher = teacherRepository.save(teacher);

        // Back-link classroom → class_teacher_id
        if (savedTeacher.getClassTeacherOf() != null) {
            classRoomRepository.findById(savedTeacher.getClassTeacherOf()).ifPresent(room -> {
                if (room.getClassTeacherId() == null) {
                    room.setClassTeacherId(savedTeacher.getTeacherId());
                    classRoomRepository.save(room);
                }
            });
        }

        return teacherProfileRepository.save(profile);
    }

    private AdminProfile upsertAdminProfile(UserLogin user) {
        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();

        AdminProfile profile = adminProfileRepository.findByUserId(user.getId())
                .orElseGet(AdminProfile::new);

        profile.setUserId(user.getId());
        profile.setName(fullName);
        profile.setInitials(buildInitials(user.getFirstName(), user.getLastName()));
        if (profile.getAccessLevel() == null) {
            profile.setAccessLevel("full");
        }
        if (profile.getStatus() == null) {
            profile.setStatus("active");
        }

        return adminProfileRepository.save(profile);
    }

    private ParentsProfile upsertParentsProfile(UserLogin user) {
        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();

        ParentsProfile profile = parentsProfileRepository.findByUserId(user.getId())
                .orElseGet(ParentsProfile::new);

        profile.setUserId(user.getId());
        profile.setName(fullName);
        profile.setInitials(buildInitials(user.getFirstName(), user.getLastName()));
        profile.setClassName(user.getClassName());
        if (profile.getStatus() == null) {
            profile.setStatus("active");
        }

        // Also keep tp_parent in sync (used by parent-portal endpoints)
        Parent parent = parentRepository.findByUserId(user.getId()).orElseGet(Parent::new);
        parent.setUserId(user.getId());
        parent.setFullName(fullName);
        parent.setEmail(user.getEmail());
        parent.setPhone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        parentRepository.save(parent);

        return parentsProfileRepository.save(profile);
    }

    private String buildInitials(String firstName, String lastName) {
        return ("" +
                (firstName != null && !firstName.isEmpty()
                        ? Character.toUpperCase(firstName.charAt(0)) : "") +
                (lastName != null && !lastName.isEmpty()
                        ? Character.toUpperCase(lastName.charAt(0)) : ""));
    }

    /**
     * Get user by email
     */
    public Optional<UserLogin> getUserByEmail(String email) {
        return userLoginRepository.findFirstByEmail(email);
    }

    /**
     * Build a full LoginResponse for a user looked up by Keycloak email claim.
     * Used by the /api/auth/profile endpoint after OIDC login.
     */
    public LoginResponse getLoginResponseByEmail(String email) {
        Optional<UserLogin> userOpt = userLoginRepository.findFirstByEmail(email);
        if (userOpt.isEmpty()) return null;
        UserLogin user = userOpt.get();
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setClassName(user.getClassName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setSchoolName(user.getSchoolName());
        response.setSuccess(true);
        response.setMessage("Profile loaded.");
        setProfileId(response, user);
        return response;
    }

    /**
     * Forgot password - generate reset token
     */
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        ForgotPasswordResponse response = new ForgotPasswordResponse();
        
        Optional<UserLogin> userOptional = userLoginRepository.findFirstByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            // For security, still return success even if email doesn't exist
            response.setSuccess(true);
            response.setMessage("If an account exists with this email, a reset token has been generated.");
            response.setToken(null);
            return response;
        }
        
        UserLogin user = userOptional.get();
        
        // Generate a simple reset token (in production, this should be stored in DB with expiration)
        String resetToken = UUID.randomUUID().toString().substring(0, 8);
        
        // In production, you would store this token in the database with an expiration time
        // and send it via email. For demo purposes, we return it directly.
        
        // For demo purposes, return the token directly
        // In production, this should be sent via email
        response.setSuccess(true);
        response.setMessage("Password reset token generated. Use this token to reset your password.");
        response.setToken(resetToken);
        
        return response;
    }

    /**
     * Reset password with token
     */
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        ResetPasswordResponse response = new ResetPasswordResponse();
        
        Optional<UserLogin> userOptional = userLoginRepository.findFirstByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Email not found.");
            return response;
        }
        
        UserLogin user = userOptional.get();
        
        // For demo purposes, accept any token
        // In production, validate the token against stored value and check expiration
        if (request.getToken() == null || request.getToken().isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Invalid reset token.");
            return response;
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userLoginRepository.save(user);
        
        response.setSuccess(true);
        response.setMessage("Password reset successfully. Please login with your new password.");
        
        return response;
    }
}
