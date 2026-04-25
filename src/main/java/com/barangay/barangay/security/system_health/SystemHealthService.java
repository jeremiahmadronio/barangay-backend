package com.barangay.barangay.security.system_health;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemHealthService {

    private final MeterRegistry meterRegistry;


    public SystemHealthDTO getSystemMetrics() {
        // 1. CPU - Keep your current logic, it's fine.
        double cpu = 0;
        try {
            cpu = meterRegistry.get("system.cpu.usage").gauge().value() * 100;
        } catch (Exception e) {
            cpu = 0; // Fallback if metric is not ready
        }

        // 2. JVM Memory (RAM) - THE FIX
        long totalMemory = Runtime.getRuntime().totalMemory(); // Current allocated
        long freeMemory = Runtime.getRuntime().freeMemory();   // Unused in current allocation
        long maxMemory = Runtime.getRuntime().maxMemory();     // THE REAL LIMIT (-Xmx)

        double memUsedMB = (totalMemory - freeMemory) / (1024.0 * 1024.0);
        double memMaxMB = maxMemory / (1024.0 * 1024.0);

        double memPercent = (memMaxMB > 0) ? (memUsedMB / memMaxMB) * 100 : 0;

        File root = new File("/");
        double diskTotal = root.getTotalSpace() / (1024.0 * 1024 * 1024);
        double diskFree = root.getFreeSpace() / (1024.0 * 1024 * 1024);
        double diskUsedPercent = (diskTotal > 0) ? ((diskTotal - diskFree) / diskTotal) * 100 : 0;

        String status = (memPercent > 90 || cpu > 90) ? "CRITICAL" : "HEALTHY";

        return new SystemHealthDTO(
                Math.round(cpu * 100.0) / 100.0,
                Math.round(memUsedMB),
                Math.round(memMaxMB),
                Math.round(memPercent * 100.0) / 100.0,
                Math.round(diskFree * 100.0) / 100.0,
                Math.round(diskTotal * 100.0) / 100.0,
                Math.round(diskUsedPercent * 100.0) / 100.0,
                status
        );
    }
}