package com.barangay.barangay.security.system_health;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system")
@RequiredArgsConstructor
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    @GetMapping("/health")
    public ResponseEntity<SystemHealthDTO> getHealth() {
        return ResponseEntity.ok(systemHealthService.getSystemMetrics());
    }
}