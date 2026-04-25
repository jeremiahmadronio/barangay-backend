package com.barangay.barangay.vawc.service;

import com.barangay.barangay.admin_management.model.User;
import com.barangay.barangay.audit.service.AuditLogService;
import com.barangay.barangay.blotter.dto.complaint.WitnessDTO;
import com.barangay.barangay.blotter.dto.notes.AddCaseNoteRequest;
import com.barangay.barangay.blotter.dto.notes.CaseNoteViewDTO;
import com.barangay.barangay.blotter.dto.reports_and_display.CaseTimeLineDTO;
import com.barangay.barangay.blotter.model.BlotterCase;
import com.barangay.barangay.blotter.model.CaseNote;
import com.barangay.barangay.blotter.model.CaseTimeline;
import com.barangay.barangay.blotter.model.IncidentDetail;
import com.barangay.barangay.blotter.repository.CaseNoteRepository;
import com.barangay.barangay.blotter.repository.CasteTimeLineRepository;
import com.barangay.barangay.department.model.Department;
import com.barangay.barangay.employee.model.Employee;
import com.barangay.barangay.employee.repository.EmployeeRepository;
import com.barangay.barangay.enumerated.*;
import com.barangay.barangay.lupon.model.PangkatCFA;
import com.barangay.barangay.lupon.repository.PangkatCFARepository;
import com.barangay.barangay.person.model.Person;
import com.barangay.barangay.person.model.Respondent;
import com.barangay.barangay.person.repository.WitnessRepository;
import com.barangay.barangay.user_management.repository.UserManagementRepository;
import com.barangay.barangay.vawc.dto.*;
import com.barangay.barangay.vawc.model.BaranggayProtectionOrder;
import com.barangay.barangay.vawc.model.Intervention;
import com.barangay.barangay.vawc.model.InterventionFollowUp;
import com.barangay.barangay.vawc.model.InterventionPerformedBy;
import com.barangay.barangay.vawc.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VawcService {


    private final VawcCaseRepository caseRepository;
    private final BarangayProtectionOrderRepository barangayProtectionOrderRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final InterventionRepository interventionRepository;
    private final InterventionPerfomedByRepository interventionPerfomedByRepository;
    private final EmployeeRepository employeeRepository;
    private final InterventionFollowUpRepository interventionFollowUpRepository;
    private final CasteTimeLineRepository caseTimeLineRepository;
    private final UserManagementRepository userManagementRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final PangkatCFARepository pangkatCFARepository;
    private final PangkatCFARepository cfaRepository;

    @Transactional(readOnly = true)
    public Page<CaseSummaryDTO> getVAWCSummary(
            User officer,
            String search, String status, String violenceType,
            LocalDate start, LocalDate end, Pageable pageable) {

        List<Long> deptIds = officer.getAllowedDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        if (deptIds.isEmpty()) {
            throw new RuntimeException("Unauthorized: No department assigned.");
        }

        Specification<BlotterCase> spec = VawcFilteringSpecs.buildVawcFilter(
                search, status, violenceType, start, end, deptIds
        );

        return caseRepository.findAll(spec, pageable).map(this::toListResponse);
    }


    public class VawcFilteringSpecs {
        public static Specification<BlotterCase> buildVawcFilter(
                String search, String status, String violenceType,
                LocalDate start, LocalDate end, List<Long> deptIds) {

            return (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                predicates.add(root.get("department").get("id").in(deptIds));

                if (search != null && !search.isBlank()) {
                    String pattern = "%" + search.toLowerCase() + "%";
                    Predicate caseNum = cb.like(cb.lower(root.get("blotterNumber")), pattern);
                    Predicate firstName = cb.like(cb.lower(root.get("complainant").get("person").get("firstName")), pattern);
                    Predicate lastName = cb.like(cb.lower(root.get("complainant").get("person").get("lastName")), pattern);
                    predicates.add(cb.or(caseNum, firstName, lastName));
                }

                if (status != null && !status.isBlank()) {
                    try {
                        CaseStatus caseStatus = CaseStatus.valueOf(status.toUpperCase());
                        predicates.add(cb.equal(root.get("status"), caseStatus));
                    } catch (IllegalArgumentException ignored) {}
                }

                if (violenceType != null && !violenceType.isBlank()) {
                    String pattern = "%" + violenceType.toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("incidentDetail").get("natureOfComplaint")), pattern));
                }

                if (start != null && end != null) {
                    LocalDateTime from = start.atStartOfDay();
                    LocalDateTime to = end.atTime(23, 59, 59);
                    predicates.add(cb.between(root.get("dateFiled"), from, to));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };
        }
    }





    private CaseSummaryDTO toListResponse(BlotterCase bc) {
        String victim = "Unknown Victim";
        if (bc.getComplainant() != null && bc.getComplainant().getPerson() != null) {
            Person p = bc.getComplainant().getPerson();
            String prefix = getPrefix(p.getGender(), p.getCivilStatus());
            victim = (prefix.isEmpty() ? "" : prefix + " ")
                    + p.getFirstName() + " " + p.getLastName();
        }

        String violenceTypes = "None";
        if (bc.getIncidentDetail() != null && bc.getIncidentDetail().getNatureOfComplaint() != null) {
            violenceTypes = bc.getIncidentDetail().getNatureOfComplaint();
        }

        String assignedOfficer = "Unassigned";
        if (bc.getEmployee() != null && bc.getEmployee().getPerson() != null) {
            Person ep = bc.getEmployee().getPerson();
            assignedOfficer = ep.getFirstName() + " " + ep.getLastName();
        }

        return new CaseSummaryDTO(
                bc.getId(),
                bc.getBlotterNumber(),
                victim,
                violenceTypes,
                bc.getStatus().name(),
                bc.getDateFiled(),
                assignedOfficer
        );
    }

    private String getPrefix(String gender, String civilStatus) {
        if (gender == null) return "";
        if (gender.equalsIgnoreCase("Male")) return "Mr.";
        if (civilStatus != null && civilStatus.equalsIgnoreCase("Married")) return "Mrs.";
        return "Ms.";
    }



    @Transactional(readOnly = true)
    public CaseStatsDTO getVawcStats() {
        long total = caseRepository.countTotalVawc();
        long pending = caseRepository.countByStatus(CaseStatus.PENDING);

        long active = total - caseRepository.countByStatus(CaseStatus.CLOSED)
                - caseRepository.countByStatus(CaseStatus.SETTLED);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fifteenDaysAgo = now.minusDays(15);
        LocalDateTime twelveDaysAgo = now.minusDays(12);

        long expiringSoon = caseRepository.countExpiringBPOs(fifteenDaysAgo, twelveDaysAgo);

        return new CaseStatsDTO(total, active, expiringSoon, pending);
    }




    @Transactional(readOnly = true)
    public CaseViewDTO getVawcCaseDetails(Long id) {
        BlotterCase bc = caseRepository.findByIdWithFullDetails(id)
                .orElseThrow(() -> new RuntimeException("Case with ID " + id + " not found."));

        BaranggayProtectionOrder bpo = caseRepository.findBpoWithViolenceTypes(id).orElse(null);
        List<String> evidenceNames = caseRepository.findEvidenceNamesByCaseId(id);

        String remainingTime;
        LocalDate bpoDeadline;

        if (bc.getStatus() == CaseStatus.PENDING) {
            LocalDateTime deadline24h = bc.getDateFiled().plusHours(24);
            long hours = java.time.Duration.between(LocalDateTime.now(), deadline24h).toHours();
            remainingTime = Math.max(0, hours) + "h";
            bpoDeadline = deadline24h.toLocalDate();
        }
        else {
            bpoDeadline = (bpo != null && bpo.getExpiredAt() != null)
                    ? bpo.getExpiredAt()
                    : bc.getDateFiled().toLocalDate().plusDays(15);
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), bpoDeadline);
            remainingTime = Math.max(0, days) + "d";
        }

        Person vP = bc.getComplainant().getPerson();
        Respondent res = bc.getRespondent();
        Person rP = res.getPerson();
        IncidentDetail idet = bc.getIncidentDetail();

        return new CaseViewDTO(
                bpoDeadline,
                remainingTime,
                bc.getBlotterNumber(),
                bc.getStatus(),
                bc.getStatusRemarks(),
                bc.getDateFiled(),
                bc.getEmployee() != null ? bc.getEmployee().getPerson().getFirstName() + " " + bc.getEmployee().getPerson().getLastName() : "Unassigned",
                bc.getCreatedBy() != null ? bc.getCreatedBy().getPerson().getFirstName() + " " + bc.getCreatedBy().getPerson().getLastName() : "System",

                vP.getFirstName(), vP.getLastName(), vP.getMiddleName(),
                vP.getContactNumber(), vP.getAge(), vP.getGender(),
                vP.getCivilStatus(), vP.getEmail(), vP.getCompleteAddress(),

                rP.getFirstName(), rP.getLastName(), rP.getMiddleName(),
                res.getAlias(), rP.getContactNumber(), (int) rP.getAge(),
                rP.getGender(), rP.getCivilStatus(), rP.getOccupation(),
                res.getRelationshipToComplainant(), rP.getCompleteAddress(),
                res.getLivingWithComplainant(),

                idet != null ? idet.getNatureOfComplaint() : "N/A",
                idet != null ? idet.getDateOfIncident() : null,
                idet != null ? idet.getTimeOfIncident() : null,
                idet != null ? idet.getPlaceOfIncident() : "N/A",
                idet != null ? idet.getFrequency() : "N/A",
                idet != null ? idet.getInjuriesDamagesDescription() : "N/A",

                bc.getNarrativeStatement() != null ? bc.getNarrativeStatement().getStatement() : "No narrative recorded.",
                evidenceNames,

                bc.getWitnesses().stream()
                        .filter(w -> w.getPerson() != null)
                        .map(w -> new WitnessDTO(
                                w.getPerson().getId(),
                                w.getPerson().getFirstName() + " " + w.getPerson().getLastName(),
                                w.getPerson().getContactNumber(),
                                w.getPerson().getCompleteAddress(),
                                w.getTestimony()
                        )).toList(),

                bpo != null ? bpo.getViolenceTypes().stream()
                        .map(v -> new ViolenceTypeDTO(
                                v.getId(),
                                v.getName(),
                                v.getDescription()
                        )).toList() : List.of()
        );
    }


    @Transactional
    public String activateBpo(Long caseId, User officer, String ipAddress) {
        BaranggayProtectionOrder bpo = barangayProtectionOrderRepository.findByBlotterCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("BPO Record not found for this case."));

        if (bpo.getStatus() != BpoStatus.PENDING) {
            throw new RuntimeException("BPO is already " + bpo.getStatus() + ". Cannot re-activate.");
        }


        String generatedBpoNumber;
        boolean isExisting;
        String currentYear = String.valueOf(LocalDate.now().getYear());

        do {
            String randomSuffix = generateRandomAlphanumeric(5);
            generatedBpoNumber = currentYear + "-BPO-" + randomSuffix;

            isExisting = barangayProtectionOrderRepository.existsByBpoControlNumber(generatedBpoNumber);

        } while (isExisting);

        LocalDate today = LocalDate.now();
        bpo.setBpoControlNumber(generatedBpoNumber);
        bpo.setExpiredAt(today.plusDays(15));
        bpo.setStatus(BpoStatus.ISSUED);
        bpo.setCreatedBy(officer);
        bpo.setActivatedAt(LocalDateTime.now());

        BlotterCase bc = bpo.getBlotterCase();
        bc.setStatus(CaseStatus.UNDER_MEDIATION);
        bc.setStatusRemarks("BPO has been officially issued. Valid until: " + bpo.getExpiredAt());

        BaranggayProtectionOrder savedBpo = barangayProtectionOrderRepository.save(bpo);


        CaseTimeline timeline = new CaseTimeline();
        timeline.setBlotterCase(bc);
        timeline.setEventType(TimelineEventType.BPO_ISSUED);
        timeline.setTitle("BPO Issued: " + generatedBpoNumber);
        timeline.setDescription("Barangay Protection Order has been officially issued by " +
                officer.getPerson().getFirstName() + " " + officer.getPerson().getLastName() +
                ". Protection is valid for 15 days (until " + bpo.getExpiredAt() + ").");
        timeline.setPerformedBy(officer);
        caseTimeLineRepository.save(timeline);

        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("caseNumber", bc.getBlotterNumber());
            details.put("bpoControlNumber", savedBpo.getBpoControlNumber());
            details.put("expiryDate", savedBpo.getExpiredAt());
            details.put("issuedBy", officer.getPerson().getFirstName() + " " + officer.getPerson().getLastName());

            String logDetails = objectMapper.writeValueAsString(details);

            auditLogService.log(
                    officer,
                    Departments.VAWC,
                    "VAWC_MODULE",
                    Severity.INFO,
                    "ACTIVATE_BPO",
                    ipAddress,
                    "Issued BPO for Case: " + bc.getBlotterNumber(),
                    null,
                    logDetails
            );
        } catch (Exception e) {
            auditLogService.log(officer, Departments.VAWC, "ERROR", Severity.INFO, "AUDIT_LOG_FAIL", ipAddress, e.getMessage(), null, null);
        }

        return "BPO successfully activated. Expiration set to " + bpo.getExpiredAt();
    }


    private String generateRandomAlphanumeric(int length) {
        String chars = "0123456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }


    @Transactional(readOnly = true)
    public BpoDetails getActivatedBpoDetails(Long caseId) {
        BaranggayProtectionOrder bpo = barangayProtectionOrderRepository.findBpoDetailsByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("BPO Record not found for this case."));

        if (bpo.getExpiredAt() == null) {
            throw new RuntimeException("BPO is not yet activated.");
        }
        BlotterCase bc = bpo.getBlotterCase();
        Person victim = bc.getComplainant().getPerson();
        Person resp = bc.getRespondent().getPerson();

        String officerName = (bc.getEmployee() != null && bc.getEmployee().getPerson() != null)
                ? bc.getEmployee().getPerson().getFirstName() + " " + bc.getEmployee().getPerson().getLastName()
                : "Unassigned";
        return new BpoDetails(
                bpo.getId(),
                bc.getBlotterNumber(),
                victim.getFirstName() + " " + victim.getLastName(),
                resp.getFirstName() + " " + resp.getLastName(),
                officerName,
                bpo.getBpoControlNumber(),
                bpo.getCreatedAt(),
                bpo.getExpiredAt()
        );
    }


    @Transactional
    public String addIntervention(InterventionRequestDTO dto, User officer, String ipAddress) {
        BaranggayProtectionOrder bpo = barangayProtectionOrderRepository.findById(dto.bpoId())
                .orElseThrow(() -> new RuntimeException("BPO Record not found."));
        BlotterCase bc = bpo.getBlotterCase();

        Intervention log = new Intervention();
        log.setBaranggayProtectionOrder(bpo);
        log.setActivityType(dto.activityType());
        log.setInterventionDetails(dto.interventionDetails());
        log.setInterventionDate(dto.interventionDate());
        log.setInterventionDuration(dto.interventionDuration());
        log.setCreatedBy(officer);
        Intervention savedLog = interventionRepository.save(log);

        List<String> performerNames = new ArrayList<>();
        if (dto.performedByEmployeeIds() != null && !dto.performedByEmployeeIds().isEmpty()) {
            List<InterventionPerformedBy> performers = dto.performedByEmployeeIds().stream().map(empId -> {
                Employee emp = employeeRepository.findById(empId)
                        .orElseThrow(() -> new RuntimeException("Employee ID " + empId + " not found."));

                performerNames.add(emp.getPerson().getFirstName() + " " + emp.getPerson().getLastName());

                InterventionPerformedBy pb = new InterventionPerformedBy();
                pb.setIntervention(savedLog);
                pb.setEmployee(emp);
                return pb;
            }).toList();

            interventionPerfomedByRepository.saveAll(performers);
        }

        CaseTimeline timeline = new CaseTimeline();
        timeline.setBlotterCase(bc);
        timeline.setEventType(TimelineEventType.INTERVENTION_RECORDED);
        timeline.setTitle("Intervention Recorded: " + dto.activityType());

        String performerList = String.join(", ", performerNames);
        timeline.setDescription("An intervention activity ('" + dto.activityType() + "') was conducted. " +
                "Details: " + dto.interventionDetails() + ". " +
                "Personnel involved: " + (performerList.isEmpty() ? "None specified" : performerList));

        timeline.setPerformedBy(officer);
        caseTimeLineRepository.save(timeline);

        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("BPO_Control_No", bpo.getBpoControlNumber());
            snapshot.put("Activity", dto.activityType());
            snapshot.put("Personnel", performerNames);
            snapshot.put("Duration", dto.interventionDuration() + " mins");

            String jsonSnapshot = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);

            auditLogService.log(
                    officer,
                    Departments.VAWC,
                    "VAWC_INTERVENTION",
                    Severity.INFO,
                    "CREATE_INTERVENTION",
                    ipAddress,
                    "Intervention logged for BPO: " + bpo.getBpoControlNumber(),
                    null,
                    jsonSnapshot
            );
        } catch (Exception e) {
            auditLogService.log(officer, Departments.VAWC, "ERROR", Severity.CRITICAL, "AUDIT_FAIL", ipAddress, e.getMessage(), null, null);
        }

        return "Intervention recorded successfully.";
    }

    @Transactional
    public String addFollowUp(FollowUpDTO dto, User officer, String ipAddress) {
        Intervention intervention = interventionRepository.findById(dto.interventionId())
                .orElseThrow(() -> new RuntimeException("Intervention Log not found."));

        InterventionFollowUp followUp = new InterventionFollowUp();
        followUp.setIntervention(intervention);
        followUp.setNotes(dto.notes());
        followUp.setCreatedBy(officer);

        interventionFollowUpRepository.save(followUp);
        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("interventionId", intervention.getId());
            details.put("activityType", intervention.getActivityType());
            details.put("notes", dto.notes());
            details.put("officer", officer.getPerson().getLastName());

            auditLogService.log(
                    officer,
                    Departments.VAWC,
                    "INTERVENTION_FOLLOW_UP",
                    Severity.INFO,
                    "ADD_FOLLOW_UP",
                    ipAddress,
                    "Follow-up added for intervention ID: " + intervention.getId(),
                    null,
                    objectMapper.writeValueAsString(details)
            );
        } catch (Exception e) {
            auditLogService.log(officer, Departments.VAWC, "ERROR", Severity.CRITICAL, "LOG_FAIL", ipAddress, e.getMessage(), null, null);
        }

        return "Follow-up notes added successfully.";
    }



    @Transactional(readOnly = true)
    public InterventionViewDTO getInterventionFullDetails(Long interventionId) {
        Intervention intervention = interventionRepository.findByIdWithDetails(interventionId)
                .orElseThrow(() -> new RuntimeException("Intervention not found."));

        List<InterventionFollowUp> followUps = interventionFollowUpRepository
                .findAllByInterventionIdOrderByCreatedAtDesc(interventionId);

        List<String> performers = intervention.getPerformedBy().stream()
                .map(emp -> emp.getPerson().getFirstName() + " " + emp.getPerson().getLastName())
                .toList();

        List<FollowUpViewDTO> followUpDTOs = followUps.stream()
                .map(f -> new FollowUpViewDTO(
                        f.getId(),
                        f.getNotes(),
                        f.getCreatedAt(),
                        f.getCreatedBy().getPerson().getFirstName() + " " + f.getCreatedBy().getPerson().getLastName()
                )).toList();

        return new InterventionViewDTO(
                intervention.getId(),
                intervention.getActivityType(),
                intervention.getInterventionDetails(),
                intervention.getInterventionDate(),
                intervention.getInterventionDuration(),
                intervention.getCreatedBy().getPerson().getFirstName() + " " + intervention.getCreatedBy().getPerson().getLastName(),
                performers,
                followUpDTOs
        );
    }



    @Transactional(readOnly = true)
    public List<AssignOfficerOptionDTO> getVawcInterventionDropdown() {
        return employeeRepository.findAssignOfficerOptionDTO();
    }




    @Transactional(readOnly = true)
    public List<AssignOfficerOptionDTO> getVawcComplaintOfficer() {
        return employeeRepository.findAssignOfficerOptioNComplaint();
    }



    @Transactional
    public void addNoteToCase(AddCaseNoteRequest dto, User officer, String ipAddress) {
        User managedOfficer = userManagementRepository.findByIdWithDepartments(officer.getId())
                .orElseThrow(() -> new RuntimeException("Officer not found."));

        BlotterCase blotterCase = caseRepository.findByBlotterNumber(dto.blotterNumber())
                .orElseThrow(() -> new RuntimeException("Case not found: " + dto.blotterNumber()));

        CaseNote caseNote = new CaseNote();
        caseNote.setBlotterCase(blotterCase);
        caseNote.setNote(dto.note());
        caseNote.setCreatedBy(managedOfficer);

        caseNoteRepository.save(caseNote);


        CaseTimeline timeline = new CaseTimeline();
        timeline.setBlotterCase(blotterCase);

        timeline.setEventType(TimelineEventType.NOTE_ADDED);
        timeline.setTitle("Note Added");

        String noteSnippet = dto.note().length() > 100
                ? dto.note().substring(0, 97) + "..."
                : dto.note();
        timeline.setDescription(noteSnippet);

        timeline.setPerformedBy(managedOfficer);
        caseTimeLineRepository.save(timeline);


        logNoteActivity(managedOfficer, blotterCase, dto.note(), ipAddress);

    }

    private void logNoteActivity(User officer, BlotterCase bc, String note, String ip) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("caseNumber", bc.getBlotterNumber());
            snapshot.put("noteSnippet", note.substring(0, Math.min(note.length(), 100)));
            snapshot.put("officer", officer.getPerson().getFirstName() + " " + officer.getPerson().getLastName());

            String jsonState = objectMapper.writeValueAsString(snapshot);

            auditLogService.log(
                    officer,
                    Departments.BLOTTER,
                    "CASE_NOTE_ADDED",
                    Severity.INFO,
                    "ADD_NOTE",
                    ip,
                    "Added follow-up note to Case: " + bc.getBlotterNumber(),
                    null,
                    jsonState
            );
        } catch (Exception e) {
            auditLogService.log(officer, null, "ERROR", Severity.CRITICAL, "LOG_FAIL", ip, e.getMessage(), null, null);
        }
    }


    @Transactional(readOnly = true)
    public List<CaseNoteViewDTO> getCaseNotesById(Long caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new RuntimeException("Case ID " + caseId + " not found.");
        }

        List<CaseNote> notes = caseNoteRepository.findByBlotterCaseIdOrderByCreatedAtDesc(caseId);

        return notes.stream()
                .map(note -> new CaseNoteViewDTO(
                        note.getId(),
                        note.getNote(),
                        note.getCreatedBy().getPerson().getFirstName() + " " + note.getCreatedBy().getPerson().getLastName(),
                        note.getCreatedAt()
                ))
                .toList();
    }


    public List<CaseTimeLineDTO> getTimelineByCase(String caseId) {
        return caseTimeLineRepository.findByBlotterCase_BlotterNumberOrderByEventDateDesc(caseId)
                .stream()
                .map(t -> new CaseTimeLineDTO(
                        t.getId(),
                        t.getEventType(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getPerformedBy() != null ?
                                t.getPerformedBy().getPerson().getFirstName() + " " + t.getPerformedBy().getPerson().getLastName() : "System",
                        t.getEventDate()
                ))
                .collect(Collectors.toList());
    }



    @Transactional
    public String withdrawVawcCase(Long caseId, UpdateCaseStatusDTO request) {
        BlotterCase bc = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("The requested case record could not be found in the system."));

        if (bc.getStatus() == CaseStatus.SETTLED || bc.getStatus() == CaseStatus.CERTIFIED_TO_FILE_ACTION || bc.getStatus() == CaseStatus.CLOSED) {
            throw new RuntimeException("Illegal Action: This case has already reached a final status and cannot be withdrawn.");
        }

        bc.setStatus(CaseStatus.WITHDRAWN);
        bc.setStatusRemarks(request.reason());
        bc.setSettledAt(LocalDateTime.now());
        caseRepository.save(bc);

        barangayProtectionOrderRepository.findByBlotterCase(bc).ifPresent(bpo -> {
            if (bpo.getStatus() != BpoStatus.EXPIRED) {
                bpo.setStatus(BpoStatus.REVOKED);
                barangayProtectionOrderRepository.save(bpo);
            }
        });

        return "Case withdrawal processed successfully.";
    }



    @Transactional
    public String issueVawcReferral(CreateReferralDTO dto, User currentUser) {
        BlotterCase bc = caseRepository.findById(dto.caseId())
                .orElseThrow(() -> new RuntimeException("Operational Error: The specified case record was not found."));

        if (bc.getStatus() == CaseStatus.CERTIFIED_TO_FILE_ACTION || bc.getStatus() == CaseStatus.SETTLED) {
            throw new RuntimeException("Validation Failed: A referral or settlement has already been processed for this case.");
        }

        PangkatCFA referral = new PangkatCFA();
        referral.setBlotterCase(bc);
        referral.setGrounds(dto.grounds());
        referral.setSubjectOfLitigation(dto.subjectOfLitigation());
        referral.setControlNumber(generateControlNumber());
        referral.setIssuedBy(currentUser);
        pangkatCFARepository.save(referral);

        bc.setStatus(CaseStatus.CERTIFIED_TO_FILE_ACTION);
        bc.setStatusRemarks("CASE REFERRED TO PROPER AUTHORITIES. Referral Control #: " + generateControlNumber());
        bc.setSettledAt(LocalDateTime.now());
        caseRepository.save(bc);

        return "Referral letter issued and case status updated to REFERRED.";
    }

    private String generateControlNumber() {
        int year = LocalDate.now().getYear();
        SecureRandom random = new SecureRandom();
        int randomDigits = 10000 + random.nextInt(90000);
        return year + "-RF-" + randomDigits;
    }



    @Transactional(readOnly = true)
    public DisplayCFADTO getSingleCfaByCaseId(Long caseId) {
        PangkatCFA p = cfaRepository.findByBlotterCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Erroe: No cfa in this case  " + caseId));

        var bc = p.getBlotterCase();
        var comp = bc.getComplainant();
        var resp = bc.getRespondent();
        var emp = bc.getEmployee();

        String cPrefix = (comp != null && comp.getPerson() != null && "MALE".equalsIgnoreCase(comp.getPerson().getGender())) ? "Mr. " : "Ms. ";
        String rPrefix = (resp != null && resp.getPerson() != null && "MALE".equalsIgnoreCase(resp.getPerson().getGender())) ? "Mr. " : "Ms. ";

        return new DisplayCFADTO(
                bc.getBlotterNumber(),
                p.getSubjectOfLitigation(),
                cPrefix + comp.getPerson().getFirstName() + " " + comp.getPerson().getLastName(),
                comp.getPerson().getCompleteAddress(),
                rPrefix + resp.getPerson().getFirstName() + " " + resp.getPerson().getLastName(),
                resp.getPerson().getCompleteAddress(),
                p.getGrounds(),
                p.getControlNumber(),
                p.getIssuedAt(),
                emp != null ? emp.getPerson().getFirstName() + " " + emp.getPerson().getLastName() : "Unassigned",
                emp != null ? emp.getPosition() : "N/A"
        );
    }

}
