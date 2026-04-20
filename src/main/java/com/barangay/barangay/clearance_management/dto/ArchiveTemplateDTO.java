package com.barangay.barangay.clearance_management.dto;

public record ArchiveTemplateDTO(
        Long templateId,
        String templateName,
        String layoutStyle,
        boolean isArchived,
        String archiveRemarks
) {
}
