package com.forgeStackk.EduResolve.security;

import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class AdminAuthHelper {

    private final UserLoginRepository userLoginRepo;

    public UserLogin resolve(Principal principal) {
        if (!(principal instanceof JwtAuthenticationToken jwtAuth)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String email = jwtAuth.getToken().getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email claim missing from token");
        }
        return userLoginRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
    }

    public String resolveSchoolName(Principal principal) {
        return resolve(principal).getSchoolName();
    }

    public Long resolveUserId(Principal principal) {
        return resolve(principal).getId();
    }

    public String resolveAdminName(Principal principal) {
        UserLogin u = resolve(principal);
        return (u.getFirstName() + " " + u.getLastName()).trim();
    }
}
