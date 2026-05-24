package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/students")
@CrossOrigin(origins = "*")
public class AdminStudentController {

    @Autowired private UserLoginRepository userRepo;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "")   String className) {

        Page<UserLogin> result = userRepo.findByRoleAndSearch(
                "student",
                search,
                className,
                PageRequest.of(page, size));

        List<Map<String, Object>> data = result.getContent().stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id",          u.getId());
            row.put("name",        u.getFirstName() + " " + u.getLastName());
            row.put("email",       u.getEmail());
            row.put("className",   u.getClassName());
            row.put("phoneNumber", u.getPhoneNumber());
            row.put("schoolName",  u.getSchoolName());
            return row;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("data",       data);
        response.put("total",      result.getTotalElements());
        response.put("page",       page);
        response.put("pageSize",   size);
        response.put("totalPages", result.getTotalPages());
        return response;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        return userRepo.findById(id).map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id",          u.getId());
            row.put("name",        u.getFirstName() + " " + u.getLastName());
            row.put("firstName",   u.getFirstName());
            row.put("lastName",    u.getLastName());
            row.put("email",       u.getEmail());
            row.put("className",   u.getClassName());
            row.put("phoneNumber", u.getPhoneNumber());
            row.put("schoolName",  u.getSchoolName());
            row.put("role",        u.getRole());
            return ResponseEntity.ok(row);
        }).orElse(ResponseEntity.notFound().build());
    }
}
