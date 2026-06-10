package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.Complaint;
import com.forgeStackk.EduResolve.entity.Fee;
import com.forgeStackk.EduResolve.repository.ComplaintRepository;
import com.forgeStackk.EduResolve.repository.FeeRepository;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    public Map<String, Object> dashboard(@RequestParam(defaultValue = "today") String range) {
        Map<String, Object> result = new HashMap<>();

        List<Fee> fees = feeRepo.findAll();

        // Revenue within requested range
        Instant rangeFrom = switch (range) {
            case "7d"  -> Instant.now().minus(7,  ChronoUnit.DAYS);
            case "30d" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default    -> Instant.now().truncatedTo(ChronoUnit.DAYS); // today
        };
        double revenue = fees.stream()
                .filter(f -> f.getStatus() == Fee.Status.Paid
                        && f.getPaidAt() != null
                        && f.getPaidAt().isAfter(rangeFrom))
                .mapToDouble(f -> f.getAmount() == null ? 0 : f.getAmount().doubleValue())
                .sum();
        // Fall back to all-time paid sum when nothing in range (avoids zero on fresh data)
        if (revenue == 0) {
            revenue = fees.stream()
                    .filter(f -> f.getStatus() == Fee.Status.Paid)
                    .mapToDouble(f -> f.getAmount() == null ? 0 : f.getAmount().doubleValue())
                    .sum();
        }

        long enrollment = userRepo.findAll().stream()
                .filter(u -> "student".equalsIgnoreCase(u.getRole()))
                .count();

        long activeTickets = complaintRepo.findAll().stream()
                .filter(c -> c.getStatus() != Complaint.Status.Resolved
                          && c.getStatus() != Complaint.Status.Closed)
                .count();

        // Fee collection rate: paid / total * 100
        double totalFees = fees.stream()
                .mapToDouble(f -> f.getAmount() == null ? 0 : f.getAmount().doubleValue())
                .sum();
        double paidFees = fees.stream()
                .filter(f -> f.getStatus() == Fee.Status.Paid)
                .mapToDouble(f -> f.getAmount() == null ? 0 : f.getAmount().doubleValue())
                .sum();
        int feeCollectionRate = totalFees > 0 ? (int) Math.round(paidFees / totalFees * 100) : 0;

        result.put("revenue", revenue);
        result.put("enrollment", enrollment);
        result.put("enrollmentTarget", 1300);
        result.put("activeTickets", activeTickets);
        result.put("feeCollectionRate", feeCollectionRate);
        result.put("range", range);
        result.put("recentUnpaidFees", fees.stream()
                .filter(f -> f.getStatus() == Fee.Status.Unpaid || f.getStatus() == Fee.Status.Overdue)
                .limit(5)
                .toList());
        return result;
    }
}
