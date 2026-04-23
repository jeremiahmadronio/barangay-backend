package com.barangay.barangay.admin_management.dto.report;

public record ArchiveSummaryDTO(
        long archivedResidents,
        long archivedUsers,
        long archivedOfficers,
        long archivedAdmins
) {}