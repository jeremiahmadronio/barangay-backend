package com.barangay.barangay.blotter.service;

import com.barangay.barangay.admin_management.model.User;
import com.barangay.barangay.blotter.dto.complaint.WitnessDTO;
import com.barangay.barangay.blotter.dto.hearing.*;
import com.barangay.barangay.blotter.dto.notes.CaseNoteViewDTO;
import com.barangay.barangay.blotter.dto.reports_and_display.*;
import com.barangay.barangay.blotter.model.BlotterCase;
import com.barangay.barangay.blotter.model.CaseNote;
import com.barangay.barangay.blotter.model.Hearing;
import com.barangay.barangay.blotter.model.HearingMinutes;
import com.barangay.barangay.blotter.repository.*;
import com.barangay.barangay.department.model.Department;
import com.barangay.barangay.department.repository.DepartmentRepository;
import com.barangay.barangay.enumerated.CaseStatus;
import com.barangay.barangay.enumerated.CaseType;
import com.barangay.barangay.enumerated.Departments;
import com.barangay.barangay.enumerated.Severity;
import com.barangay.barangay.lupon.repository.PangkatCompositionRepository;
import com.barangay.barangay.person.model.Person;
import com.barangay.barangay.person.repository.WitnessRepository;
import com.barangay.barangay.user_management.repository.UserManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlotterServiceViewOnly {

    private final BlotterCaseRepository blotterRepository;
    private final EvidenceRecordRepository evidenceRecordRepository;
    private final WitnessRepository witnessRepository;
    private final HearingRepository  hearingRepository;
    private final HearingMinutesRepository hearingMinutesRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final PangkatCompositionRepository pangkatCompositionRepository;
    private final DepartmentRepository departmentRepository;



    @Transactional(readOnly = true)
    public Page<BlotterSummaryDTO> getPagedBlotters(
            User officer,
            String search, String status, Long natureId,
            LocalDate start, LocalDate end, Pageable pageable) {

        List<Long> deptIds = officer.getAllowedDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        if (deptIds.isEmpty()) {
            throw new RuntimeException("Unauthorized: No department assigned.");
        }


        if (deptIds.contains(3L) && !deptIds.contains(4L)) {
            deptIds.add(4L);
        }

        CaseType targetType = CaseType.FOR_THE_RECORD;

        Specification<BlotterCase> spec = BlotterRecordsSpecificationsFiltering.buildFormalDocketFilter(
                search,
                status,
                natureId,
                start,
                end,
                null,
                targetType
        ).and((root, query, cb) -> cb.equal(root.get("isArchived"), false));

        return blotterRepository.findAll(spec, pageable).map(this::mapToSummaryDTO);
    }


    @Transactional(readOnly = true)
    public BlotterRecordViewDTO getFullBlotterRecord(String blotterNumber) {

        BlotterCase bc = blotterRepository.findByBlotterNumber(blotterNumber)
                .orElseThrow(() -> new RuntimeException(" Case not found: " + blotterNumber));

        List<String> evidence = evidenceRecordRepository.findAllByBlotterCase(bc)
                .stream()
                .map(er -> er.getType().getTypeName())
                .toList();

        String officerName = (bc.getCreatedBy() != null)
                ? bc.getCreatedBy().getPerson().getFirstName() + " " + bc.getCreatedBy().getPerson().getLastName()
                : "Unassigned / System Generated";


        String relationshipName = (bc.getRespondent() != null && bc.getRespondent().getRelationshipToComplainant() != null)
                ? bc.getRespondent().getRelationshipToComplainant()
                : "N/A";

        return new BlotterRecordViewDTO(
                bc.getId(),
                bc.getBlotterNumber(),
                bc.getDateFiled(),
                bc.getStatus().name(),
                officerName,
                bc.getComplainant().getPerson().getFirstName() + " " + bc.getComplainant().getPerson().getLastName(),
                bc.getComplainant().getPerson().getContactNumber(),
                bc.getComplainant().getPerson().getCompleteAddress(),
                bc.getComplainant().getPerson().getCivilStatus(),
                bc.getComplainant().getPerson().getAge() != null ? bc.getComplainant().getPerson().getAge().intValue() : 0,
                bc.getComplainant().getPerson().getGender(),
                bc.getComplainant().getPerson().getEmail(),
                bc.getRespondent().getPerson().getFirstName() + " " + bc.getRespondent().getPerson().getLastName() ,
                bc.getRespondent().getPerson().getContactNumber(),
                relationshipName,
                bc.getRespondent().getPerson().getCompleteAddress(),
                bc.getIncidentDetail().getNatureOfComplaint(),
                bc.getIncidentDetail().getDateOfIncident(),
                bc.getIncidentDetail().getTimeOfIncident(),
                bc.getIncidentDetail().getPlaceOfIncident(),
                bc.getNarrativeStatement().getStatement(),
                evidence
        );





    }



    @Transactional(readOnly = true)
    public Page<BlotterSummaryDTO> docketTable(
            User officer,
            String search, String status, Long natureId,
            LocalDate start, LocalDate end, Pageable pageable) {

        List<Long> deptIds = officer.getAllowedDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        if (deptIds.isEmpty()) {
            throw new RuntimeException("Unauthorized: No department assigned.");
        }


        if (deptIds.contains(3L)) {
            if (!deptIds.contains(4L)) {
                deptIds.add(7L);
            }
        }

        Specification<BlotterCase> spec = BlotterRecordsSpecificationsFiltering.buildFormalDocketFilter(
                search,
                status,
                natureId,
                start,
                end,
                deptIds,
                CaseType.FORMAL_COMPLAINT
        );

        return blotterRepository.findAll(spec, pageable).map(this::mapToSummaryDTO);
    }

    private BlotterSummaryDTO mapToSummaryDTO(BlotterCase bc) {
        String complainant = (bc.getComplainant() != null && bc.getComplainant().getPerson() != null)
                ? bc.getComplainant().getPerson().getFirstName() + " " + bc.getComplainant().getPerson().getLastName()
                : "N/A";

        String respondent = (bc.getRespondent() != null && bc.getRespondent().getPerson() != null)
                ? bc.getRespondent().getPerson().getFirstName() + " " + bc.getRespondent().getPerson().getLastName()
                : "N/A";

        String nature = (bc.getIncidentDetail() != null && bc.getIncidentDetail().getNatureOfComplaint() != null)
                ? bc.getIncidentDetail().getNatureOfComplaint()
                : "General Record";

        return new BlotterSummaryDTO(
                bc.getId(),
                bc.getBlotterNumber(),
                complainant,
                respondent,
                nature,
                bc.getDateFiled(),
                bc.getStatus() != null ? bc.getStatus().name() : "RECORDED"
        );
    }











    @Transactional(readOnly = true)
    public BlotterDocketViewDTO getDocketFullView(String blotterNumber) {
        BlotterCase bc = blotterRepository.findByBlotterNumber(blotterNumber)
                .orElseThrow(() -> new RuntimeException(" Docket Case not found: " + blotterNumber));



        LocalDateTime filedDateTime = bc.getDateFiled();
        LocalDate deadline = (filedDateTime != null) ? filedDateTime.toLocalDate().plusDays(15) : LocalDate.now().plusDays(15);
        long remaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        if (remaining < 0) remaining = 0;

        List<String> evidence = evidenceRecordRepository.findAllByBlotterCase(bc).stream()
                .map(er -> (er.getType() != null) ? er.getType().getTypeName() : "Unknown Evidence")
                .toList();

        List<WitnessDTO> witnesses = witnessRepository.findAllByBlotterCase(bc).stream()
                .map(w -> {
                    Person p = w.getPerson();
                    String fullName = (p != null) ? p.getFirstName() + " " + p.getLastName() : "Anonymous Witness";
                    String contact = (p != null && p.getContactNumber() != null) ? p.getContactNumber() : "N/A";
                    String address = (p != null && p.getCompleteAddress() != null) ? p.getCompleteAddress() : "N/A";

                    return new WitnessDTO(
                            p != null ? p.getId() : null,
                            fullName,
                            contact,
                            address,
                            w.getTestimony()
                    );
                }).toList();

        List<CaseHandleByDTO> luponManagement = pangkatCompositionRepository.findByBlotterCaseId(bc.getId())
                .stream()
                .map(p -> new CaseHandleByDTO(
                        p.getEmployee().getPerson().getFirstName(),
                        p.getEmployee().getPerson().getLastName(),
                        p.getPosition()
                )).toList();


        return new BlotterDocketViewDTO(
                deadline,
                remaining,
                bc.getBlotterNumber(),
                bc.getStatus(),
                bc.getStatusRemarks(),
                bc.getDateFiled(),

                // --- Complainant Info (Safe Navigations) ---
                bc.getComplainant().getPerson().getFirstName(),
                bc.getComplainant().getPerson().getLastName(),
                bc.getComplainant().getPerson().getMiddleName(),
                bc.getComplainant().getPerson().getContactNumber(),
                (bc.getComplainant().getPerson().getAge() != null)
                        ? bc.getComplainant().getPerson().getAge()
                        : (short) 0,
                bc.getComplainant().getPerson().getGender(),
                bc.getComplainant().getPerson().getCivilStatus(),
                bc.getComplainant().getPerson().getEmail(),
                bc.getComplainant().getPerson().getCompleteAddress(),

                // --- Respondent Info (HEAVILY PROTECTED) ---
                bc.getRespondent().getPerson().getFirstName(),
                bc.getRespondent().getPerson().getLastName(),
                bc.getRespondent().getPerson().getMiddleName(),
                bc.getRespondent().getAlias(),
                (bc.getRespondent().getPerson().getContactNumber() != null) ? bc.getRespondent().getPerson().getContactNumber() : "N/A",
                (bc.getRespondent().getPerson().getAge() != null) ? bc.getRespondent().getPerson().getAge().intValue() : 0,
                bc.getRespondent().getPerson().getGender(),
                bc.getRespondent().getPerson().getBirthDate(), // LocalDate can be null
                bc.getRespondent().getPerson().getCivilStatus(),
                bc.getRespondent().getPerson().getOccupation(),
                (bc.getRespondent().getRelationshipToComplainant() != null) ? bc.getRespondent().getRelationshipToComplainant() : "Others/Unknown",
                (bc.getRespondent().getPerson().getCompleteAddress() != null) ? bc.getRespondent().getPerson().getCompleteAddress() : "N/A",
                (bc.getRespondent().getLivingWithComplainant() != null) ? bc.getRespondent().getLivingWithComplainant() : false,

                (bc.getIncidentDetail().getNatureOfComplaint() != null) ? bc.getIncidentDetail().getNatureOfComplaint() : "Uncategorized",
                bc.getIncidentDetail().getDateOfIncident(),
                bc.getIncidentDetail().getTimeOfIncident(),
                bc.getIncidentDetail().getPlaceOfIncident(),
                (bc.getIncidentDetail().getFrequency() != null) ? bc.getIncidentDetail().getFrequency() : "First Time",
                (bc.getIncidentDetail().getInjuriesDamagesDescription() != null) ? bc.getIncidentDetail().getInjuriesDamagesDescription() : "None reported",

                (bc.getNarrativeStatement() != null) ? bc.getNarrativeStatement().getStatement() : "No narrative statement provided.",

                evidence,
                witnesses,
                bc.getSettlementTerms(),
                bc.getSettledAt(),
                luponManagement,
                bc.getEmployee().getPerson().getFirstName() + " " + bc.getEmployee().getPerson().getLastName()

        );
    }




    @Transactional(readOnly = true)
    public MediationProcessDTO getMediationProcess(String blotterNumber) {
        BlotterCase bc = blotterRepository.findByBlotterNumber(blotterNumber)
                .orElseThrow(() -> new RuntimeException("Case not found: " + blotterNumber));

        int hCount = (int) hearingRepository.countByBlotterCaseId(bc.getId());

        boolean s1 = true;
        String s1Date = (bc.getDateFiled() != null)
                ? bc.getDateFiled().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                : "Date not recorded";

        boolean s2 = hCount > 0;
        String s2Status = s2 ? "Mediation Issued" : "Awaiting first Mediation   ";

        boolean s3 = hCount > 0;

        boolean s4 = List.of(CaseStatus.SETTLED, CaseStatus.DISMISSED, CaseStatus.CERTIFIED_TO_FILE_ACTION)
                .contains(bc.getStatus());

        String s4Status = s4 ? bc.getStatus().name().replace("_", " ") : "Awaiting resolution";

        return new MediationProcessDTO(
                s1,         // stepCaseReceived
                s1Date,     // caseReceivedDate
                s2,         // stepSummonIssued
                s2Status,   // summonStatus
                s3,         // stepMediationOngoing
                hCount,     // hearingsConducted
                s4,         // stepResolved
                s4Status    // resolutionStatus
        );
    }



    @Transactional(readOnly = true)
    public List<HearingViewDTO> getCaseHearings(String blotterNumber) {
        BlotterCase bc = blotterRepository.findByBlotterNumber(blotterNumber)
                .orElseThrow(() -> new RuntimeException(" Case not found: " + blotterNumber));

        List<Hearing> hearings = hearingRepository.findAllByBlotterCaseIdOrderByScheduledStartAsc(bc.getId());


        return hearings.stream()
                .map(h -> {
                    LocalDate date = h.getScheduledStart().toLocalDate();
                    LocalTime startTime = h.getScheduledStart().toLocalTime();
                    LocalTime endTime = h.getScheduledEnd().toLocalTime();

                    return new HearingViewDTO(
                            h.getId(),
                            h.getSummonNumber().intValue(),
                            h.getStatus().name(),
                            date,
                            startTime,
                            endTime,
                            h.getVenue()
                    );
                }).toList();
    }



    @Transactional(readOnly = true)
    public List<BusySlotDTO> getBusySlots(LocalDate date) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        return hearingRepository.findActiveBlotterHearingsByDate(date).stream()
                .map(h -> new BusySlotDTO(
                        h.getScheduledStart().toLocalTime().format(timeFormatter),
                        h.getScheduledEnd().toLocalTime().format(timeFormatter),
                        h.getBlotterCase().getBlotterNumber(),
                        h.getBlotterCase().getIncidentDetail().getNatureOfComplaint() != null
                                ? h.getBlotterCase().getIncidentDetail().getNatureOfComplaint()
                                : "N/A"
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarMarkerDTO> getMonthMarkers(int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusNanos(1);

        List<Hearing> activeHearings = hearingRepository.findAllActiveBlotterInMonth(start, end);
        return activeHearings.stream()
                .collect(Collectors.groupingBy(h -> h.getScheduledStart().toLocalDate()))
                .entrySet().stream()
                .map(entry -> new CalendarMarkerDTO(
                        entry.getKey().toString(),
                        entry.getValue().size()
                ))
                .toList();
    }




    @Transactional(readOnly = true)
    public MediationHearingViewDTO getHearingView(Long hearingId) {
        Hearing hearing = hearingRepository.findHearingForView(hearingId)
                .orElseThrow(() -> new RuntimeException("Hearing not found."));

        HearingMinutes minutes = hearingMinutesRepository.findByHearingId(hearingId).orElse(null);

        List<HearingParticipantDTO> participants = new ArrayList<>();

        if (hearing.getBlotterCase().getComplainant() != null) {
            String status = (minutes != null && minutes.getComplainantPresent()) ? "Present" : "Absent";
            participants.add(new HearingParticipantDTO(
                    hearing.getBlotterCase().getComplainant().getPerson().getFirstName() + " " + hearing.getBlotterCase().getComplainant().getPerson().getLastName(),
                    "Complainant",
                    status
            ));
        }

        // Respondent Mapping
        if (hearing.getBlotterCase().getRespondent() != null) {
            String status = (minutes != null && minutes.getRespondentPresent()) ? "Present" : "Absent";
            participants.add(new HearingParticipantDTO(
                    hearing.getBlotterCase().getRespondent().getPerson().getFirstName() + " " + hearing.getBlotterCase().getRespondent().getPerson().getLastName(),
                    "Respondent",
                    status
            ));
        }

        return new MediationHearingViewDTO(
                "Hearing " + hearing.getSummonNumber(),
                hearing.getStatus().name(),
                hearing.getScheduledStart().toLocalDate(),
                hearing.getScheduledStart().toLocalTime() + " - " + hearing.getScheduledEnd().toLocalTime(),
                hearing.getVenue(),
                hearing.getBlotterCase().getBlotterNumber(),
                hearing.getBlotterCase().getCaseType().name(),
                "Summon " + hearing.getSummonNumber(),
                participants
        );
    }

    @Transactional(readOnly = true)
    public List<CaseNoteViewDTO> getCaseNotesByNumber(String blotterNumber) {
        if (!blotterRepository.existsByBlotterNumber(blotterNumber)) {
            throw new RuntimeException("Blotter number " + blotterNumber + " not found.");
        }

        List<CaseNote> notes = caseNoteRepository.findByBlotterCaseBlotterNumberOrderByCreatedAtDesc(blotterNumber);

        return notes.stream()
                .map(note -> new CaseNoteViewDTO(
                        note.getId(),
                        note.getNote(),
                        note.getCreatedBy().getPerson().getFirstName() + " " + note.getCreatedBy().getPerson().getLastName(),
                        note.getCreatedAt()
                ))
                .toList();
    }



    @Transactional(readOnly = true)
    public DocketStatsDTO getFormalStatsForUser(User actor) {
        CaseType formal = CaseType.FORMAL_COMPLAINT;

        Department userDept = actor.getAllowedDepartments().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unauthorized: No department assigned to your account."));

        Set<CaseStatus> activeStatuses = Set.of(
                CaseStatus.PENDING,
                CaseStatus.UNDER_MEDIATION,
                CaseStatus.UNDER_CONCILIATION,
                CaseStatus.REFERRED_TO_LUPON
        );

        Set<CaseStatus> resolvedStatuses = Set.of(
                CaseStatus.SETTLED,
                CaseStatus.CERTIFIED_TO_FILE_ACTION,
                CaseStatus.DISMISSED,
                CaseStatus.ARCHIVED,
                CaseStatus.CLOSED
        );

        return new DocketStatsDTO(
                blotterRepository.countByCaseTypeAndDepartment(formal, userDept),
                blotterRepository.countByCaseTypeAndStatusInAndDepartment(formal, activeStatuses, userDept),
                blotterRepository.countByCaseTypeAndStatusInAndDepartment(formal, resolvedStatuses, userDept),
                blotterRepository.countByCaseTypeAndStatusAndDepartment(formal, CaseStatus.UNDER_MEDIATION, userDept)
        );
    }





    @Transactional(readOnly = true)
    public Page<ArchiveTableDTO> getArchivedCases(
            String search,
            CaseType caseType,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    ) {
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return blotterRepository.findArchivedCases(trimmedSearch, caseType, dateFrom, dateTo, pageable);
    }


    @Transactional(readOnly = true)
    public ArchiveStatsDTO getArchiveStats() {
        LocalDateTime startOfMonth = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay();

        ArchiveStatsDTO stats = blotterRepository.getArchiveStatistics(startOfMonth);


        return new ArchiveStatsDTO(
                stats.totalArchive() != null ? stats.totalArchive() : 0L,
                stats.totalArchiveThisMonth() != null ? stats.totalArchiveThisMonth() : 0L,
                stats.totalArchiveFormalComplaint() != null ? stats.totalArchiveFormalComplaint() : 0L,
                stats.totalArchiveForTheRecord() != null ? stats.totalArchiveForTheRecord() : 0L
        );
    }



}