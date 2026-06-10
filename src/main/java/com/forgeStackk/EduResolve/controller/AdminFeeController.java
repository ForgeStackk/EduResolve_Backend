package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.AuditLog;
import com.forgeStackk.EduResolve.entity.Fee;
import com.forgeStackk.EduResolve.repository.AuditLogRepository;
import com.forgeStackk.EduResolve.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/fees")
@CrossOrigin(origins = "*")
public class AdminFeeController {

    @Autowired private FeeRepository    feeRepo;
    @Autowired private AuditLogRepository auditRepo;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "")   String status) {

        Page<Fee> result = feeRepo.searchFees(
                status,
                search,
                PageRequest.of(page, size));

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> data = result.getContent().stream().map(f -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id",          f.getId());
            row.put("studentId",   f.getStudentId());
            row.put("studentName", f.getStudentName());
            row.put("className",   f.getClassName());
            row.put("grade",       f.getGrade());
            row.put("phone",       f.getPhone());
            row.put("amount",      f.getAmount());
            row.put("paidAmount",  f.getPaidAmount() != null ? f.getPaidAmount() : BigDecimal.ZERO);
            row.put("balance",     f.getAmount() != null
                    ? f.getAmount().subtract(f.getPaidAmount() != null ? f.getPaidAmount() : BigDecimal.ZERO)
                    : BigDecimal.ZERO);
            row.put("status",      f.getStatus());
            row.put("dueDate",     f.getDueDate());
            row.put("daysOverdue", f.getDueDate() != null && f.getDueDate().isBefore(today)
                    ? (int) (today.toEpochDay() - f.getDueDate().toEpochDay()) : 0);
            row.put("lastReminderAt", f.getLastReminderAt());
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

    @PostMapping("/{id}/payments")
    public ResponseEntity<Map<String, Object>> recordPayment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        return feeRepo.findById(id).map(fee -> {
            double amount = Double.parseDouble(body.getOrDefault("amount", "0").toString());
            BigDecimal pay = BigDecimal.valueOf(amount);

            BigDecimal prev    = fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal newPaid = prev.add(pay);
            BigDecimal balance = fee.getAmount().subtract(newPaid);

            fee.setPaidAmount(newPaid);
            fee.setStatus(balance.compareTo(BigDecimal.ZERO) <= 0 ? Fee.Status.Paid
                       : newPaid.compareTo(BigDecimal.ZERO) > 0   ? Fee.Status.Partial
                       : fee.getStatus());
            if (balance.compareTo(BigDecimal.ZERO) <= 0) fee.setPaidAt(Instant.now());
            feeRepo.save(fee);

            AuditLog log = new AuditLog();
            log.setAction("fee.payment_recorded");
            log.setTargetType("Fee");
            log.setTargetId(String.valueOf(id));
            log.setDetail("Recorded payment of ₹" + pay + " via " + body.getOrDefault("method", "cash"));
            auditRepo.save(log);

            Map<String, Object> res = new HashMap<>();
            res.put("newBalance", balance.max(BigDecimal.ZERO));
            res.put("status",     fee.getStatus());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/remind")
    public ResponseEntity<Map<String, Object>> sendReminder(@PathVariable Long id) {
        return feeRepo.findById(id).map(fee -> {
            fee.setLastReminderAt(Instant.now());
            feeRepo.save(fee);
            Map<String, Object> res = new HashMap<>();
            res.put("message", "Reminder queued successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/payment-link")
    public ResponseEntity<Map<String, Object>> getPaymentLink(@PathVariable Long id) {
        return feeRepo.findById(id).map(fee -> {
            BigDecimal paid    = fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balance = fee.getAmount() != null ? fee.getAmount().subtract(paid) : BigDecimal.ZERO;
            balance = balance.max(BigDecimal.ZERO);

            String upiVpa    = System.getenv().getOrDefault("UPI_VPA",    "school@okaxis");
            String schoolName = System.getenv().getOrDefault("SCHOOL_NAME", "EduResolve");
            String txnNote   = ("Fee-" + fee.getStudentName()).replaceAll("[^A-Za-z0-9\\-]", "").substring(0,
                    Math.min(50, ("Fee-" + fee.getStudentName()).replaceAll("[^A-Za-z0-9\\-]", "").length()));

            String upiLink = String.format(
                    "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s",
                    upiVpa, schoolName, balance, txnNote);

            // WhatsApp deep link — sends the UPI link as a pre-filled message
            String waMsg   = String.format(
                    "Dear parent of %s, your school fee of ₹%.0f is due. " +
                    "Pay instantly via UPI: %s",
                    fee.getStudentName(), balance, upiLink);
            String phone   = fee.getPhone() != null ? fee.getPhone().replaceAll("[^0-9]", "") : "";
            String waLink  = "https://wa.me/" + (phone.startsWith("91") ? phone : "91" + phone)
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
}
