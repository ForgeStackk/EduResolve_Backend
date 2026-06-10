package com.forgeStackk.EduResolve.service.teacher;

import com.forgeStackk.EduResolve.entity.teacher.Attendance;
import com.forgeStackk.EduResolve.enums.AttendanceStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AttendancePdfService {

    private static final float PAGE_W   = PDRectangle.A4.getWidth();   // 595
    private static final float PAGE_H   = PDRectangle.A4.getHeight();  // 842
    private static final float MARGIN   = 50f;
    private static final float COL_W    = PAGE_W - 2 * MARGIN;         // 495

    private static final float COL_DATE   = MARGIN;
    private static final float COL_DAY    = MARGIN + 120f;
    private static final float COL_STATUS = MARGIN + 250f;

    private static final float FONT_TITLE  = 13f;
    private static final float FONT_LABEL  = 10f;
    private static final float FONT_TABLE  = 9.5f;
    private static final float ROW_HEIGHT  = 13f;
    private static final float LINE_GAP    = 5f;

    @Value("${reports.school-name:EduResolve Academy}")
    private String schoolName;

    /**
     * Generates a per-student attendance PDF matching the spec layout.
     *
     * @param className    e.g. "10A"
     * @param section      e.g. "A"
     * @param month        1-12
     * @param year         e.g. 2026
     * @param studentName  full name
     * @param rollNumber   roll number string
     * @param teacherName  class teacher full name
     * @param records      all Attendance rows for this student in the month
     * @param totalWorkingDays computed working days for the class
     * @return PDF bytes ready to write to disk / S3
     */
    public byte[] generate(
            String className,
            String section,
            int month,
            int year,
            String studentName,
            String rollNumber,
            String teacherName,
            List<Attendance> records,
            int totalWorkingDays) throws IOException {

        Map<LocalDate, AttendanceStatus> byDate = records.stream()
                .collect(Collectors.toMap(Attendance::getDate, Attendance::getStatus));

        YearMonth ym = YearMonth.of(year, month);
        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        // Count per-status for summary
        int present = count(records, AttendanceStatus.PRESENT);
        int absent  = count(records, AttendanceStatus.ABSENT);
        int late    = count(records, AttendanceStatus.LATE);
        int halfDay = count(records, AttendanceStatus.HALF_DAY);
        double pct  = totalWorkingDays > 0
                ? (present * 100.0) / totalWorkingDays
                : 0.0;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_H - MARGIN;

                // ── Header ────────────────────────────────────────────────────
                y = drawHRule(cs, y);
                y -= 18f;
                y = drawCentred(cs, bold, FONT_TITLE,
                        schoolName + " — Attendance Report", y);
                y -= 14f;
                y = drawLine(cs, regular, FONT_LABEL,
                        "Class: " + className + "-" + section
                        + "   |   Month: " + monthName + " " + year, y);
                y -= 12f;
                y = drawLine(cs, regular, FONT_LABEL,
                        "Student: " + studentName
                        + "   |   Roll No: " + rollNumber, y);
                y -= 8f;
                y = drawHRule(cs, y);

                // ── Table header ──────────────────────────────────────────────
                y -= 14f;
                drawCell(cs, bold, FONT_TABLE, "Date",   COL_DATE,   y);
                drawCell(cs, bold, FONT_TABLE, "Day",    COL_DAY,    y);
                drawCell(cs, bold, FONT_TABLE, "Status", COL_STATUS, y);
                y -= (ROW_HEIGHT - LINE_GAP);
                y = drawHRule(cs, y);

                // ── Attendance rows ───────────────────────────────────────────
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH);
                for (int d = 1; d <= ym.lengthOfMonth(); d++) {
                    LocalDate date = ym.atDay(d);
                    boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                                    || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                    String statusLabel;
                    if (isWeekend) {
                        statusLabel = "Weekend";
                    } else {
                        AttendanceStatus s = byDate.get(date);
                        statusLabel = (s != null) ? formatStatus(s) : "Not Marked";
                    }

                    y -= ROW_HEIGHT;
                    drawCell(cs, regular, FONT_TABLE, date.format(dateFmt),         COL_DATE,   y);
                    drawCell(cs, regular, FONT_TABLE,
                            date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            COL_DAY, y);
                    drawCell(cs, regular, FONT_TABLE, statusLabel,                  COL_STATUS, y);
                }

                // ── Summary ───────────────────────────────────────────────────
                y -= 10f;
                y = drawHRule(cs, y);
                y -= 16f;
                y = drawLine(cs, bold,    FONT_LABEL, "Summary:", y);
                y -= 13f;
                y = drawLine(cs, regular, FONT_LABEL,
                        String.format("Total Working Days : %d", totalWorkingDays), y);
                y -= 13f;
                y = drawLine(cs, regular, FONT_LABEL,
                        String.format("Days Present       : %d", present), y);
                y -= 13f;
                y = drawLine(cs, regular, FONT_LABEL,
                        String.format("Days Absent        : %d", absent), y);
                if (late > 0) {
                    y -= 13f;
                    y = drawLine(cs, regular, FONT_LABEL,
                            String.format("Days Late          : %d", late), y);
                }
                if (halfDay > 0) {
                    y -= 13f;
                    y = drawLine(cs, regular, FONT_LABEL,
                            String.format("Half Days          : %d", halfDay), y);
                }
                y -= 13f;
                y = drawLine(cs, regular, FONT_LABEL,
                        String.format("Attendance %%       : %.1f%%", pct), y);

                // ── Footer ────────────────────────────────────────────────────
                y -= 8f;
                y = drawHRule(cs, y);
                y -= 14f;
                y = drawLine(cs, regular, FONT_LABEL, "Class Teacher: " + teacherName, y);
                y -= 13f;
                drawLine(cs, regular, FONT_LABEL,
                        "Generated on: " + java.time.Instant.now()
                                .atZone(java.time.ZoneId.of("Asia/Kolkata"))
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)),
                        y);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private float drawHRule(PDPageContentStream cs, float y) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(MARGIN + COL_W, y);
        cs.stroke();
        return y;
    }

    private float drawCentred(PDPageContentStream cs, PDFont font, float size, String text, float y)
            throws IOException {
        float tw = (font.getStringWidth(text) / 1000f) * size;
        float x  = (PAGE_W - tw) / 2f;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y;
    }

    private float drawLine(PDPageContentStream cs, PDFont font, float size, String text, float y)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(text);
        cs.endText();
        return y;
    }

    private void drawCell(PDPageContentStream cs, PDFont font, float size, String text, float x, float y)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private int count(List<Attendance> records, AttendanceStatus status) {
        return (int) records.stream().filter(a -> a.getStatus() == status).count();
    }

    private String formatStatus(AttendanceStatus s) {
        return switch (s) {
            case PRESENT  -> "Present";
            case ABSENT   -> "Absent";
            case LATE     -> "Late";
            case HALF_DAY -> "Half Day";
            case HOLIDAY  -> "Holiday";
        };
    }
}
