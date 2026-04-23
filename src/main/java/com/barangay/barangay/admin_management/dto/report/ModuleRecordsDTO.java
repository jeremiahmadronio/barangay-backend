package com.barangay.barangay.admin_management.dto.report;

public record ModuleRecordsDTO(
        long admin,
        long resident,
        long officer,
        long auditLogs
) {}