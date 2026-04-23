package com.barangay.barangay.admin_management.dto.report;

import java.util.List;

public record GrowthTrendResponse(
        List<String> labels,
        List<Long> adminCounts,
        List<Long> residentCounts,
        List<Long> officerCounts
) {}