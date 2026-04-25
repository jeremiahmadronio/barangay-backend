package com.barangay.barangay.blotter.controller;

import com.barangay.barangay.audit.service.IpAddressUtils;
import com.barangay.barangay.blotter.dto.Records.FtrSummaryStatsDTO;
import com.barangay.barangay.blotter.dto.Records.UpdateStatusDTO;
import com.barangay.barangay.blotter.dto.complaint.ArchiveCaseDTO;
import com.barangay.barangay.blotter.dto.complaint.EvidenceOptionDTO;
import com.barangay.barangay.blotter.dto.complaint.NatureOptionDTO;
import com.barangay.barangay.blotter.dto.hearing.BusySlotDTO;
import com.barangay.barangay.blotter.dto.hearing.CalendarMarkerDTO;
import com.barangay.barangay.blotter.dto.hearing.HearingViewDTO;
import com.barangay.barangay.blotter.dto.hearing.MediationHearingViewDTO;
import com.barangay.barangay.blotter.dto.notes.AddCaseNoteRequest;
import com.barangay.barangay.blotter.dto.notes.CaseNoteViewDTO;
import com.barangay.barangay.blotter.dto.reports_and_display.*;
import com.barangay.barangay.blotter.service.BlotterService;
import com.barangay.barangay.blotter.service.BlotterServiceViewOnly;
import com.barangay.barangay.enumerated.CaseType;
import com.barangay.barangay.security.CustomUserDetails;
import com.barangay.barangay.vawc.dto.AssignOfficerOptionDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/blotter")
@RequiredArgsConstructor
public class BlotterController {

  private final BlotterService  blotterService;
  private final BlotterServiceViewOnly  blotterServiceViewOnly;


    @PostMapping("/add-note")
    public ResponseEntity<?> addCaseNotes(
            @Valid @RequestBody AddCaseNoteRequest dto,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ){
        String ipAddress = IpAddressUtils.getClientIp(request);
          blotterService.addNoteToCase(dto, userDetails.user(), ipAddress);

        return ResponseEntity.ok("Successfully added note");
    }


    @GetMapping("/record-table")
    public ResponseEntity<Page<BlotterSummaryDTO>> getPagedBlotters(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long natureId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @PageableDefault(size = 10, sort = "dateFiled", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                blotterServiceViewOnly.getPagedBlotters(
                        userDetails.user(),
                        search,
                        status,
                        natureId,
                        start,
                        end,
                        pageable
                )
        );
    }

    @GetMapping("/docket-table")
    public ResponseEntity<Page<BlotterSummaryDTO>> getDocketTable(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long natureId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @PageableDefault(size = 5, sort = "dateFiled", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                blotterServiceViewOnly.docketTable(
                        userDetails.user(),
                        search,
                        status,
                        natureId,
                        start,
                        end,
                        pageable
                )
        );
    }


    @GetMapping("/view-all/{blotterNumber}")
    public ResponseEntity<BlotterRecordViewDTO> getFullBlotterRecord(
            @PathVariable String blotterNumber) {

        return ResponseEntity.ok(
                blotterServiceViewOnly.getFullBlotterRecord(blotterNumber)
        );
    }


    @GetMapping("/view-all-docket/{blotterNumber}")
    public ResponseEntity<BlotterDocketViewDTO> getFullBlotterDocket(
            @PathVariable String blotterNumber) {

        return ResponseEntity.ok(
                blotterServiceViewOnly.getDocketFullView(blotterNumber)
        );
    }


   @GetMapping("/mediation-process/{blotterNumber}")
    public ResponseEntity<MediationProcessDTO> getMediationProcess(
           @PathVariable String blotterNumber){

        return ResponseEntity.ok(blotterServiceViewOnly.getMediationProcess(blotterNumber));
    }

    @GetMapping("/hearing-view/{blotterNumber}")
  public ResponseEntity<List<HearingViewDTO>> getHearingView(
           @PathVariable String blotterNumber
  ){
        return ResponseEntity.ok(blotterServiceViewOnly.getCaseHearings(blotterNumber));
  }



    @GetMapping("/markers")
    public ResponseEntity<List<CalendarMarkerDTO>> getMarkers(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(blotterServiceViewOnly.getMonthMarkers(year, month));


    }

    @GetMapping("/busy-slots")
    public ResponseEntity<List<BusySlotDTO>> getBusySlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(blotterServiceViewOnly.getBusySlots(date));
    }


    @GetMapping("/hearing-minutes-view/{hearingId}")
    public ResponseEntity<MediationHearingViewDTO> getMediationHearingView(
            @PathVariable Long hearingId){
        return ResponseEntity.ok(blotterServiceViewOnly.getHearingView(hearingId));
    }



    @GetMapping("/{blotterNumber}/notes")
    public ResponseEntity<List<CaseNoteViewDTO>> getCaseNotes(@PathVariable String blotterNumber) {
        return ResponseEntity.ok(blotterServiceViewOnly.getCaseNotesByNumber(blotterNumber));
    }


    @GetMapping("/evidence-type-options")
    public ResponseEntity<List<EvidenceOptionDTO>> getEvidenceOptions() {
        return ResponseEntity.ok(blotterService.getEvidenceOptions());
    }





    @PutMapping("/update-case-status")
    public ResponseEntity<?> updateStatus(
            @RequestBody @Valid UpdateStatusDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ){
        String ipAddress = IpAddressUtils.getClientIp(request);
        blotterService.updateStatus(dto,userDetails.user(),ipAddress);
        return ResponseEntity.ok("Status has been successfully updated");
    }

    @GetMapping("/docket-stats")
    public ResponseEntity<DocketStatsDTO> getDocketStats(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return ResponseEntity.ok(blotterServiceViewOnly.getFormalStatsForUser(userDetails.user()));
    }



    @GetMapping("/records-stats")
    public ResponseEntity<FtrSummaryStatsDTO> getRecordsSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return ResponseEntity.ok(blotterService.getFtrDashboardStats(userDetails.user()));
    }


    @GetMapping("/timeline/{caseId}")
    public ResponseEntity<List<CaseTimeLineDTO>> getCaseTimeline(@PathVariable String caseId) {
        return ResponseEntity.ok(blotterService.getTimelineByCase(caseId));
    }

    @GetMapping("/assign-officer-option")
    public ResponseEntity<List<AssignOfficerOptionDTO>> getAssignOfficerOption(){
        return ResponseEntity.ok(blotterService.getBlotterLuponDropdown());
    }


    @PatchMapping("/archived/{caseId}")
    public ResponseEntity<?> archiveCase (
            @PathVariable Long caseId,
            @RequestBody @Valid ArchiveCaseDTO dto,
            @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest request
            ){
        String ipAddress = IpAddressUtils.getClientIp(request);
        blotterService.archiveCase(caseId,dto,actor.user(),ipAddress);
        return ResponseEntity.ok("Case has been successfully archived");
    }

    @PatchMapping("/restore/{caseId}")
    public  ResponseEntity<?> restoreCase (
            @PathVariable Long caseId,
            @RequestBody @Valid ArchiveCaseDTO dto,
            @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest request
    ) {
        String ipAddress = IpAddressUtils.getClientIp(request);
        blotterService.restoreCase(caseId, dto, actor.user(), ipAddress);
        return ResponseEntity.ok("Case has been successfully restore");

    }

    @GetMapping("/archive-table")
    public ResponseEntity<Page<ArchiveTableDTO>> getArchivedCases(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CaseType caseType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @PageableDefault(size = 10, sort = "dateFiled", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                blotterServiceViewOnly.getArchivedCases(search, caseType, dateFrom, dateTo, pageable)
        );
    }


    @GetMapping("/archive/stats")
    public ResponseEntity<ArchiveStatsDTO> getArchiveStats() {
        return ResponseEntity.ok(blotterServiceViewOnly.getArchiveStats());
    }
}