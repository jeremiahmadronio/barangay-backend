package com.example.barangay.audit;

import com.example.barangay.auth.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String ipAddress;
    @Column(nullable = false)
    private String module;
    @Column(nullable = false)
    private String severity;
    @Column(nullable = false)
    private String actionTaken;
    @Column(nullable = false)
    private String reason;

    @Column(columnDefinition = "jsonb")
    private String old_value;
    @Column(columnDefinition = "jsonb")
    private String new_value;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;



}
