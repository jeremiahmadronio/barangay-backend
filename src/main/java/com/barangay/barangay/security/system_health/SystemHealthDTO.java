package com.barangay.barangay.security.system_health;

public record SystemHealthDTO(
        double cpuUsage,
        double memoryUsedMB,
        double memoryMaxMB,
        double memoryPercent,
        double diskFreeGB,
        double diskTotalGB,
        double diskPercent,
        String status
) {}