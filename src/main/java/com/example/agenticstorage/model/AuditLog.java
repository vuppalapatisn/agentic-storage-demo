package com.example.agenticstorage.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Audit log for every operation an agent attempts.
 * Jeff Crume gets his audit trail!
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String agentId;

    @Column(nullable = false)
    private String sandbox;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String intentReason;

    /** ALLOWED, DENIED, VALIDATED */
    @Column(nullable = false)
    private String outcome;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
