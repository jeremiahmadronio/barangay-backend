package com.barangay.barangay.blotter.model;

import com.barangay.barangay.admin_management.model.User;
import com.barangay.barangay.department.model.Department;
import com.barangay.barangay.employee.model.Employee;
import com.barangay.barangay.enumerated.CaseStatus;
import com.barangay.barangay.enumerated.CaseType;
import com.barangay.barangay.lupon.model.PangkatCFA;
import com.barangay.barangay.person.model.Complainant;
import com.barangay.barangay.person.model.Respondent;
import com.barangay.barangay.person.model.Witness;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@Table(name = "cases")
@NoArgsConstructor
@AllArgsConstructor
public class BlotterCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true, nullable = false, name = "case_number")
    private String blotterNumber;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "case_type")
    private CaseType caseType;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "case_status")
    private CaseStatus status = CaseStatus.PENDING;


    @Column(columnDefinition = "TEXT", name = "case_remarks")
    private String statusRemarks;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "archived_remarks" , columnDefinition = "TEXT")
    private String archivedRemarks;


    @Column(name = "is_attested")
    private Boolean isCertified = false;

    @Column(name = "attested_at")
    private LocalDateTime certifiedAt;


    @Column(columnDefinition = "TEXT" ,name = "settlement_terms" )
    private String settlementTerms;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;



    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_id")
    private Complainant complainant;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "respondent_id")
    private Respondent respondent;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "narrative_statemet_id")
    private Narrative narrativeStatement;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_detail_id")
    private IncidentDetail incidentDetail;

    @Enumerated(EnumType.STRING)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_filed_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_updated_by")
    private User updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_assign_to")
    private Employee employee;

    @CreationTimestamp
    @Column(name = "case_filed_at", updatable = false , nullable = false)
    private LocalDateTime dateFiled;

    @UpdateTimestamp
    @Column(name = "case_updated_at")
    private LocalDateTime updatedAt;


    @OneToOne(mappedBy = "blotterCase", cascade = CascadeType.ALL)
    private PangkatCFA pangkatCfa;

    @OneToMany(mappedBy = "blotterCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Witness> witnesses = new ArrayList<>();

    @OneToOne(mappedBy = "blotterCase", cascade = CascadeType.ALL)
    private LuponReferral luponReferral;








}