package com.forgeStackk.EduResolve.dto.admin.attendance;

import java.util.List;
import java.util.Map;

public record InsightsDto(
        Map<String, Double> dayOfWeekBreakdown,
        Map<String, Long> reasonsBreakdown,
        List<MonthlyTrendPointDto> monthlyTrend,
        Map<String, Double> comparisonToOtherClasses
) {
    public record MonthlyTrendPointDto(String month, double percentage) {}
}
