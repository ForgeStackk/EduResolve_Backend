package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.Complaint;
import com.forgeStackk.EduResolve.entity.Fee;
import com.forgeStackk.EduResolve.repository.ComplaintRepository;
import com.forgeStackk.EduResolve.repository.FeeRepository;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    @Autowired private FeeRepository feeRepo;
    @Autowired private ComplaintRepository complaintRepo;
    @Autowired private UserLoginRepository userRepo;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> result = new HashMap<>();

        List<Fee> fees = feeRepo.findAll();
        double revenue = fees.stream()
                .filter(f -> f.getStatus() == Fee.Status.Paid)
                .mapToDouble(f -> f.getAmount() == null ? 0 : f.getAmount().doubleValue())
                .sum();

        long enrollment = userRepo.findAll().stream()
                .filter(u -> "student".equalsIgnoreCase(u.getRole()))
                .count();

        long activeTickets = complaintRepo.findAll().stream()
                .filter(c -> c.getStatus() != Complaint.Status.Resolved)
                .count();

        result.put("revenue", revenue);
        result.put("enrollment", enrollment);
        result.put("enrollmentTarget", 1300);
        result.put("activeTickets", activeTickets);
        result.put("recentUnpaidFees", fees.stream()
                .filter(f -> f.getStatus() == Fee.Status.Unpaid)
                .limit(5)
                .toList());
        return result;
    }
}
