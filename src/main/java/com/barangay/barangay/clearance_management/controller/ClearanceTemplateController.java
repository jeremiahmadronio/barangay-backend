package com.barangay.barangay.clearance_management.controller;

import com.barangay.barangay.audit.service.IpAddressUtils;
import com.barangay.barangay.clearance_management.dto.*;
import com.barangay.barangay.clearance_management.service.ClearanceTemplateService;
import com.barangay.barangay.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clearance/template")
@RequiredArgsConstructor
public class ClearanceTemplateController {

    private final ClearanceTemplateService clearanceTemplateService;


    @PostMapping("/create-template")
    public ResponseEntity<?> createClearanceTemplate(
            @RequestBody @Valid TemplateRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest request
            ){
        String ipAddress = IpAddressUtils.getClientIp(request);
        clearanceTemplateService.createTemplate(dto,actor.user(),ipAddress);
        return ResponseEntity.ok("Successfully created clearance template");
    }


    @GetMapping("/archive-templates")
    public ResponseEntity<List<ArchiveTemplateDTO>> getAllArchiveTemplates() {
        List<ArchiveTemplateDTO> templates = clearanceTemplateService.getAllTemplatesList();

        if (templates.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/view-template")
    public ResponseEntity<List<TemplateResponseDTO>> getAllTemplates() {
        return ResponseEntity.ok(clearanceTemplateService.getAllTemplates());
    }

    @PostMapping("/issue-clearance")
    public ResponseEntity<?> issueClearance(
            @RequestBody @Valid IssuanceRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest request
            ){
        String ipAddress = IpAddressUtils.getClientIp(request);
        clearanceTemplateService.issueCertificate(dto,actor.user(),ipAddress);
        return ResponseEntity.ok("Successfully issued clearance template");
    }

    @GetMapping("/summary-table")
    public ResponseEntity<List<SummaryResponseDTO>> getSummaryTable(){
        List<SummaryResponseDTO> summaries = clearanceTemplateService.getAllCertificatesSummary();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/stats")
    public ResponseEntity<CertStatsResponseDTO> getStats() {
        return ResponseEntity.ok(clearanceTemplateService.getCertStats());
    }



    @PutMapping("archive-issued/{issuedId}")
    public ResponseEntity<?> archiveIssued(
         @PathVariable   Long issuedId,
          @RequestBody @Valid  VoidRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest request
    ){
        String ipAddress = IpAddressUtils.getClientIp(request);
        clearanceTemplateService.voidCertificate(issuedId,dto,actor.user(),ipAddress);
        return ResponseEntity.ok("Successfully archived clearance template");
    }


    @PatchMapping("/toggle-archive/{id}")
    public ResponseEntity<Void> toggleTemplate(
            @PathVariable Long id,
            @RequestBody @Valid ArchiveTemplateReponseDTO dto,
            @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest request) {
        String ipAddress = IpAddressUtils.getClientIp(request);
        clearanceTemplateService.toggleTemplateArchive(id,dto,actor.user(),ipAddress);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/restore/{id}")
    public ResponseEntity<Void> restoreVoid(
            @PathVariable Long id,
            @RequestBody @Valid ArchiveTemplateReponseDTO dto
            , @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest request) {
        String ipAddress = IpAddressUtils.getClientIp(request);
        clearanceTemplateService.toggleTemplateArchive(id, dto , actor.user(), ipAddress );
        return ResponseEntity.ok().build();
    }


    @GetMapping("/archive-table")
    public ResponseEntity<List<ArchiveSummaryResponseDTO>> getAllArchives() {
        return ResponseEntity.ok(clearanceTemplateService.getAllArchived());
    }

    @GetMapping("/archive-stats")
    public ResponseEntity<ArchiveStatsResponseDTO> getArchiveStats() {
        return ResponseEntity.ok(clearanceTemplateService.getArchiveStats());
    }
}
