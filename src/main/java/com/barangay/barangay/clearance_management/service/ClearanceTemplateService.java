package com.barangay.barangay.clearance_management.service;

import com.barangay.barangay.admin_management.model.User;
import com.barangay.barangay.audit.service.AuditLogService;
import com.barangay.barangay.clearance_management.dto.*;
import com.barangay.barangay.clearance_management.model.CertificateTemplate;
import com.barangay.barangay.clearance_management.model.IssuedCertificate;
import com.barangay.barangay.clearance_management.model.RevenueRecord;
import com.barangay.barangay.clearance_management.model.TemplateSignatory;
import com.barangay.barangay.clearance_management.repository.CertificateTemplateRepository;
import com.barangay.barangay.clearance_management.repository.IssuedCertificateRepository;
import com.barangay.barangay.clearance_management.repository.RevenueRecordRepository;
import com.barangay.barangay.clearance_management.repository.TemplateSignatoryRepository;
import com.barangay.barangay.enumerated.ClearanceStatus;
import com.barangay.barangay.enumerated.Departments;
import com.barangay.barangay.enumerated.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClearanceTemplateService {

    private final CertificateTemplateRepository certificateTemplateRepository;
    private final AuditLogService auditLogService;
    private final TemplateSignatoryRepository templateSignatoryRepository;
    private final RevenueRecordRepository  revenueRecordRepository;
    private final IssuedCertificateRepository  issuedCertificateRepository;


    @Transactional
    public void createTemplate(TemplateRequestDTO dto , User actor, String ipAddress){

        if(certificateTemplateRepository.existsByCertTitle(dto.certTitle()))
            throw  new RuntimeException("Template already exists");

        CertificateTemplate template = new CertificateTemplate();

        template.setCertTitle(dto.certTitle());
        template.setLayoutStyle(dto.layoutStyle());
        template.setCertTagline(dto.certTagline());
        template.setBodySections(dto.bodySections());
        template.setIssueFields(dto.issueFields());
        template.setRequiresPhoto(dto.requiresPhoto());
        template.setRequiresThumbmark(dto.requiresThumbmark());
        template.setHasFee(dto.hasFee());
        template.setHasCtn(dto.hasCtn());
        template.setCertFee(dto.certFee());
        template.setValidityMonths(dto.validityMonths());
        template.setFooterText(dto.footerText());
        template.setCreatedBy(actor);

        if(dto.signatories() != null){
            List< TemplateSignatory> signatories = dto.signatories().stream()
                    .map(sDto -> {
                        TemplateSignatory s = new TemplateSignatory();
                        s.setSignatoryName(sDto.signatoryName());
                        s.setSignatoryTitle(sDto.signatoryTitle());
                        s.setTemplate(template);
                        return s;
                    }).toList();
            template.setSignatories(signatories);
        }

        certificateTemplateRepository.save(template);

        auditLogService.log(
                actor,
                Departments.CLEARANCE,
                "Clearance Management",
                Severity.INFO,
                actor.getPerson().getFirstName() + " " + actor.getPerson().getLastName() + "Created Clearance Template",
                ipAddress,
                null,
                null,
                template.getId()
        );

    }


    @Transactional(readOnly = true)
    public List<TemplateResponseDTO> getAllTemplates() {
        return certificateTemplateRepository.findAllByHasArchiveFalse().stream()
                .map(template -> new TemplateResponseDTO(
                        template.getId(),
                        template.getCertTitle(),
                        template.getLayoutStyle(),
                        template.getCertTagline(),
                        template.getBodySections(),
                        template.getIssueFields(),
                        template.isHasFee(),
                        template.getCertFee(),
                        template.isHasCtn(),
                        template.getValidityMonths(),
                        template.getSignatories().stream()
                                .map(s -> new SignatoryDTO(s.getSignatoryName(), s.getSignatoryTitle()))
                                .toList()
                ))
                .toList();
    }



    @Transactional
    public void issueCertificate(IssuanceRequestDTO dto, User actor, String ipAddress) {

        CertificateTemplate template = certificateTemplateRepository.findById(dto.templateId())
                .orElseThrow(() -> new RuntimeException("Template not found with ID: " + dto.templateId()));

        if (template.isHasFee() && !dto.isFree()) {
            if (dto.orNumber() == null || dto.orNumber().isBlank()) {
                throw new RuntimeException("Official Receipt (OR) Number is required for paid certificate: " + template.getCertTitle());            }
        }

        if (template.isHasCtn()) {
            if (dto.ctnNumber() == null || dto.ctnNumber().isBlank()) {
                throw new RuntimeException("Community Tax Number (CTN/Cedula) is required for this certificate type.");            }
        }

        IssuedCertificate issuedCert = new IssuedCertificate();
        issuedCert.setTemplate(template);
        issuedCert.setCertNumber(generateClearanceNumber());
        issuedCert.setRequestorName(dto.requestorName());
        issuedCert.setFieldValues(dto.fieldValues());
        issuedCert.setOrNumber(dto.orNumber());
        issuedCert.setCtnNumber(dto.ctnNumber());
        issuedCert.setFree(dto.isFree());
        issuedCert.setStatus(ClearanceStatus.RELEASED);
        issuedCert.setIssuedById(actor);

        issuedCert.setExpiryDate(LocalDate.now().plusMonths(template.getValidityMonths()));

        IssuedCertificate savedCert = issuedCertificateRepository.save(issuedCert);

        if (template.isHasFee() && !dto.isFree()) {
            RevenueRecord revenue = new RevenueRecord();
            revenue.setIssuedCertificate(savedCert);
            revenue.setOrNumber(dto.orNumber());
            revenue.setAmount(template.getCertFee());
            revenue.setCollectedById(actor);
            revenueRecordRepository.save(revenue);
        }

        auditLogService.log(
                actor,
                Departments.CLEARANCE,
                "Clearance Management",
                Severity.INFO,
                "Issued " + template.getCertTitle() + " to " + dto.requestorName(),
                ipAddress,
                null, null, "Cert ID: " + savedCert.getId()
        );

    }


    @Transactional(readOnly = true)
    public List<SummaryResponseDTO> getAllCertificatesSummary() {
        return issuedCertificateRepository.findAll().stream()
                .map(cert -> new SummaryResponseDTO(
                        cert.getId(),
                        cert.getOrNumber() != null ? cert.getOrNumber() : generateClearanceNumber(),
                        cert.getRequestorName(),
                        cert.getTemplate().getCertTitle(),
                        cert.isFree() ? java.math.BigDecimal.ZERO : cert.getTemplate().getCertFee(),
                       cert.getStatus(),
                        cert.getIssuedAt()
                ))
                .collect(Collectors.toList());
    }




    @Transactional(readOnly = true)
    public CertStatsResponseDTO getCertStats() {
        long totalCert = issuedCertificateRepository.count();
        long paid = issuedCertificateRepository.countByIsFreeFalse();
        long free = issuedCertificateRepository.countByIsFreeTrue();
        long activeTemplates = certificateTemplateRepository.countByHasArchiveFalse();


        return new CertStatsResponseDTO(
                totalCert,
                paid,
                free,
                activeTemplates
        );
    }




    @Transactional
    public void voidCertificate(Long id, VoidRequestDTO request,User actor, String ipAddress) {
        IssuedCertificate cert = issuedCertificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));

        if (cert.getStatus() == ClearanceStatus.VOID) {
            throw new IllegalStateException("Certificate is already voided.");
        }

        cert.setStatus(ClearanceStatus.VOID);
        cert.setArchiveRemarks(request.remarks());
        cert.setIssuedById(actor);
        cert.setArchive(true);

        if (cert.getRevenueRecord() != null) {
            cert.getRevenueRecord().setAmount(BigDecimal.ZERO);
        }

        issuedCertificateRepository.save(cert);

        auditLogService.log(
                actor,
                Departments.CLEARANCE,
                "Clearance Management",
                Severity.INFO,
                "Update Status by " + actor.getPerson().getFirstName() + " " + actor.getPerson().getLastName(),
                ipAddress,
                request.remarks(), "Released", "Cert ID: " + cert.getId()
        );
    }


    // Sa loob ng ClearanceTemplateService.java

    @Transactional(readOnly = true)
    public List<ArchiveTemplateDTO> getAllTemplatesList() {
        return certificateTemplateRepository.findAll().stream()
                .map(this::mapToArchiveDTO)
                .toList();
    }

    private ArchiveTemplateDTO mapToArchiveDTO(CertificateTemplate t) {
        return new ArchiveTemplateDTO(
                t.getId(),
                t.getCertTitle(), // templateName
                t.getLayoutStyle(),
                t.isHasArchive(), // isArchived
                t.getArchiveReason() // archiveRemarks
        );
    }


    @Transactional
    public void toggleTemplateArchive(Long id, ArchiveTemplateReponseDTO dto, User actor, String ipAddress) {
        CertificateTemplate template = certificateTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        boolean newStatus = !template.isHasArchive();
        template.setHasArchive(newStatus);
        template.setArchiveReason(dto.remarks());
        certificateTemplateRepository.save(template);

        String action = newStatus ? "Archive Template" : "Restore Template";

        auditLogService.log(actor,
                Departments.CLEARANCE,
                "Clearance Management",
                Severity.INFO,
                action,
                ipAddress,
                dto.remarks(),
                null,
        null);
    }


    @Transactional(readOnly = true)
    public List<ArchiveSummaryResponseDTO> getAllArchived() {
        return issuedCertificateRepository.findAllArchived();
    }



    @Transactional(readOnly = true)
    public ArchiveStatsResponseDTO getArchiveStats() {
        long totalIssued = issuedCertificateRepository.countTotalArchiveIssued();
        BigDecimal lostRev = issuedCertificateRepository.sumLostRevenue();
        long totalTemplate = certificateTemplateRepository.countTotalArchiveTemplate();
        String mostArchived = issuedCertificateRepository.findMostArchivedTemplateName();

        return new ArchiveStatsResponseDTO(
                totalIssued,
                lostRev != null ? lostRev : BigDecimal.ZERO,
                totalTemplate,
                mostArchived != null ? mostArchived : "N/A"
        );
    }





    private String generateClearanceNumber() {
        return LocalDateTime.now().getYear() + "-CL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
