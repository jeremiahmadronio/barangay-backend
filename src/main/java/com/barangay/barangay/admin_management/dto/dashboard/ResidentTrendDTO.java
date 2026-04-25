package com.barangay.barangay.admin_management.dto.dashboard;

import java.util.List;

public record ResidentTrendDTO (
        List<String> labels,
        List<Long> counts
) {
}
