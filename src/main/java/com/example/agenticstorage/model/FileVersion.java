package com.example.agenticstorage.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Represents an immutable version of a file in the agentic storage layer.
 *
 * Every write creates a NEW version — agents can never truly delete data,
 * only archive it. This gives a complete audit trail and rollback capability.
 */
@Entity
@Table(name = "file_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Logical file path within the agent's sandbox */
    @Column(nullable = false)
    private String filePath;

    /** Content of this version */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Version number (monotonically increasing per filePath) */
    @Column(nullable = false)
    private int version;

    /** Which agent performed this write */
    @Column(nullable = false)
    private String agentId;

    /** The sandbox this file belongs to (agent is restricted to this) */
    @Column(nullable = false)
    private String sandbox;

    /** Reason the agent gave for this write (intent validation) */
    @Column(columnDefinition = "TEXT")
    private String intentReason;

    /** Operation type: WRITE, ARCHIVE */
    @Column(nullable = false)
    private String operation;

    /** Whether this version is archived (logically "deleted") */
    @Column(nullable = false)
    private boolean archived;

    /** Timestamp of this version */
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
