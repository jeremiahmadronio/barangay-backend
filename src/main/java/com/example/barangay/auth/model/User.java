package com.example.barangay.auth.model;

import com.example.barangay.enumerated.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter  @Setter @AllArgsConstructor @NoArgsConstructor
public class User {

            @Id
            @GeneratedValue(strategy = GenerationType.UUID)
            private UUID id;

            //basic column
            @Column(unique = true)
            private String username;
            @Column(nullable = false)
            private String password;
            @Column(unique = true)
            private String email;
            @Column(length = 100, nullable = false)
            private String firstName;
            @Column(length = 100, nullable = false)
            private String lastName;
            @Column(length = 15)
            private String contactNumber;
            @Column
            private Integer failedAttempts;
            @Enumerated(EnumType.STRING)
            @Column(nullable = false)
            private Status status;
            @Column
            private Boolean isLocked = false;

            //date related
            @Column
            private LocalDateTime lockUntil;
            @CreationTimestamp
            @Column(updatable = false, nullable = false)
            private LocalDateTime createdAt;
            @UpdateTimestamp
            private LocalDateTime updatedAt;
            @Column
            private LocalDateTime lastLoginAt;


            //role connection
            @ManyToOne(fetch = FetchType.EAGER)
            @JoinColumn(name = "role_id")
            private Role role;

            //department connection
            @ManyToOne(fetch = FetchType.LAZY)
            @JoinColumn(name = "department_id")
            private Department department;










}
