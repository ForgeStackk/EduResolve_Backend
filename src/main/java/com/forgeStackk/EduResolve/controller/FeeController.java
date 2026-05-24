package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.Fee;
import com.forgeStackk.EduResolve.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fees")
@CrossOrigin(origins = "*")
public class FeeController {

    @Autowired
    private FeeRepository repo;

    @GetMapping
    public List<Fee> list(@RequestParam(required = false) Fee.Status status,
                          @RequestParam(required = false) Long studentId) {
        if (status != null) return repo.findByStatus(status);
        if (studentId != null) return repo.findByStudentId(studentId);
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<Fee> create(@RequestBody Fee fee) {
        fee.setId(null);
        return ResponseEntity.ok(repo.save(fee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Fee> update(@PathVariable Long id, @RequestBody Fee fee) {
        return repo.findById(id).map(existing -> {
            existing.setAmount(fee.getAmount());
            existing.setStatus(fee.getStatus());
            existing.setDueDate(fee.getDueDate());
            existing.setStudentName(fee.getStudentName());
            existing.setClassName(fee.getClassName());
            existing.setPhone(fee.getPhone());
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/remind")
    public ResponseEntity<Map<String, Object>> sendReminder(@PathVariable Long id) {
        return repo.findById(id).map(fee -> {
            fee.setLastReminderAt(Instant.now());
            repo.save(fee);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "success", true,
                    "message", "Reminder sent to " + (fee.getPhone() != null ? fee.getPhone() : "guardian"),
                    "sentAt", fee.getLastReminderAt()
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/payment-link")
    public ResponseEntity<Map<String, Object>> getPaymentLink(@PathVariable Long id) {
        return repo.findById(id).map(fee -> {
            BigDecimal paid    = fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balance = fee.getAmount() != null ? fee.getAmount().subtract(paid) : BigDecimal.ZERO;
            balance = balance.max(BigDecimal.ZERO);

            String upiVpa    = System.getenv().getOrDefault("UPI_VPA",    "school@okaxis");
            String schoolName = System.getenv().getOrDefault("SCHOOL_NAME", "EduResolve");
            String raw       = "Fee-" + (fee.getStudentName() != null ? fee.getStudentName() : "Student");
            String txnNote   = raw.replaceAll("[^A-Za-z0-9\\-]", "");
            txnNote = txnNote.substring(0, Math.min(50, txnNote.length()));

            String upiLink = String.format(
                    "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s",
                    upiVpa, schoolName, balance, txnNote);

            String waMsg = String.format(
                    "Dear parent of %s, your school fee of ₹%.0f is due. Pay via UPI: %s",
                    fee.getStudentName(), balance, upiLink);
            String phone  = fee.getPhone() != null ? fee.getPhone().replaceAll("[^0-9]", "") : "";
            String waLink = "https://wa.me/" + (phone.startsWith("91") ? phone : "91" + phone)
                    + "?text=" + java.net.URLEncoder.encode(waMsg, java.nio.charset.StandardCharsets.UTF_8);

            Map<String, Object> res = new HashMap<>();
            res.put("upiLink",     upiLink);
            res.put("waLink",      waLink);
            res.put("studentName", fee.getStudentName());
            res.put("balance",     balance);
            res.put("phone",       fee.getPhone());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<Fee> all = repo.findAll();
        double revenue = all.stream()
                .filter(f -> f.getStatus() == Fee.Status.Paid)
                .mapToDouble(f -> f.getAmount() == null ? 0.0 : f.getAmount().doubleValue())
                .sum();
        long unpaidCount = all.stream().filter(f -> f.getStatus() == Fee.Status.Unpaid).count();
        return Map.of(
                "revenue", revenue,
                "unpaidCount", unpaidCount,
                "totalRecords", all.size()
        );
    }
}
