package com.example.agenticstorage.service;

import com.example.agenticstorage.model.AuditLog;
import com.example.agenticstorage.model.FileVersion;

import com.example.agenticstorage.storage.FileVersionRepository;
import com.example.agenticstorage.storage.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * AgenticStorageService — The core of the agentic storage layer.
 *
 * Implements all three safety layers described in the video:
 *
 *  1. IMMUTABLE VERSIONING — every write creates a new version; agents
 *     can never truly delete data, only archive it.
 *
 *  2. SANDBOXING — agents are confined to allowed path prefixes;
 *     prevents the "confused deputy" problem.
 *
 *  3. INTENT VALIDATION — high-impact operations (archive/delete) require
 *     the agent to supply a reasoning chain before proceeding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgenticStorageService {

    private final FileVersionRepository fileVersionRepo;
    private final AuditLogRepository auditLogRepo;

    // Injected from SandboxConfig
    private final Map<String, String> agentSandboxMap;
    private final Map<String, Set<String>> sandboxAllowedPaths;
    private final Set<String> highImpactOperations;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API — MCP Tool equivalents
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * MCP Tool: write_file
     *
     * Writes content to a path in the agent's sandbox.
     * Safety Layer 1: always creates a NEW version (never overwrites).
     * Safety Layer 2: validates the path is within sandbox boundaries.
     */
    @Transactional
    public FileVersion writeFile(String agentId, String filePath,
                                  String content, String intentReason) {

        String sandbox = resolveSandbox(agentId);
        assertPathAllowed(agentId, sandbox, filePath, "WRITE");

        int nextVersion = fileVersionRepo.findMaxVersion(sandbox, filePath) + 1;

        FileVersion fv = FileVersion.builder()
                .filePath(filePath)
                .content(content)
                .version(nextVersion)
                .agentId(agentId)
                .sandbox(sandbox)
                .intentReason(intentReason)
                .operation("WRITE")
                .archived(false)
                .createdAt(LocalDateTime.now())
                .build();

        fileVersionRepo.save(fv);

        audit(agentId, sandbox, "WRITE", filePath, intentReason, "ALLOWED",
              "Version " + nextVersion + " created");

        log.info("[WRITE] agent={} sandbox={} path={} version={}",
                 agentId, sandbox, filePath, nextVersion);
        return fv;
    }

    /**
     * MCP Tool: read_file
     *
     * Reads a file from the agent's sandbox.
     * Optionally reads a specific historical version (RAG-like resource access).
     * Safety Layer 2: path must be in sandbox.
     */
    @Transactional(readOnly = true)
    public Optional<FileVersion> readFile(String agentId, String filePath,
                                           Integer version) {
        String sandbox = resolveSandbox(agentId);
        assertPathAllowed(agentId, sandbox, filePath, "READ");

        Optional<FileVersion> result = version != null
                ? fileVersionRepo.findBySandboxAndFilePathAndVersion(sandbox, filePath, version)
                : fileVersionRepo.findLatest(sandbox, filePath);

        audit(agentId, sandbox, "READ", filePath, null,
              result.isPresent() ? "ALLOWED" : "NOT_FOUND",
              version != null ? "version=" + version : "latest");

        return result;
    }

    /**
     * MCP Tool: archive_file  (logical delete — Safety Layer 1)
     *
     * Agents CANNOT hard-delete files. Archive creates a new version
     * marked as archived, preserving history and enabling rollback.
     *
     * Safety Layer 3: requires a valid intent reason before proceeding.
     */
    @Transactional
    public FileVersion archiveFile(String agentId, String filePath,
                                    String intentReason) {

        validateIntent("ARCHIVE", intentReason);   // Safety Layer 3

        String sandbox = resolveSandbox(agentId);
        assertPathAllowed(agentId, sandbox, filePath, "ARCHIVE");

        FileVersion latest = fileVersionRepo.findLatest(sandbox, filePath)
                .orElseThrow(() -> new IllegalArgumentException(
                        "File not found or already archived: " + filePath));

        int nextVersion = fileVersionRepo.findMaxVersion(sandbox, filePath) + 1;

        FileVersion archived = FileVersion.builder()
                .filePath(filePath)
                .content(latest.getContent())   // content preserved
                .version(nextVersion)
                .agentId(agentId)
                .sandbox(sandbox)
                .intentReason(intentReason)
                .operation("ARCHIVE")
                .archived(true)
                .createdAt(LocalDateTime.now())
                .build();

        fileVersionRepo.save(archived);

        audit(agentId, sandbox, "ARCHIVE", filePath, intentReason, "ALLOWED",
              "Archived at version " + nextVersion + "; reason validated");

        log.info("[ARCHIVE] agent={} path={} reason={}", agentId, filePath, intentReason);
        return archived;
    }

    /**
     * MCP Tool: list_directory
     *
     * Lists all active (non-archived) files in the agent's sandbox.
     */
    @Transactional(readOnly = true)
    public List<String> listDirectory(String agentId) {
        String sandbox = resolveSandbox(agentId);
        List<String> files = fileVersionRepo.findActiveFilesInSandbox(sandbox);
        audit(agentId, sandbox, "LIST", "/", null, "ALLOWED",
              files.size() + " files");
        return files;
    }

    /**
     * MCP Tool: get_file_history
     *
     * Returns the full version history of a file — complete audit trail.
     */
    @Transactional(readOnly = true)
    public List<FileVersion> getFileHistory(String agentId, String filePath) {
        String sandbox = resolveSandbox(agentId);
        assertPathAllowed(agentId, sandbox, filePath, "HISTORY");
        return fileVersionRepo
                .findBySandboxAndFilePathOrderByVersionAsc(sandbox, filePath);
    }

    /**
     * Rollback to a previous version — creates a new version with old content.
     */
    @Transactional
    public FileVersion rollback(String agentId, String filePath,
                                 int targetVersion, String intentReason) {

        String sandbox = resolveSandbox(agentId);
        assertPathAllowed(agentId, sandbox, filePath, "ROLLBACK");

        FileVersion target = fileVersionRepo
                .findBySandboxAndFilePathAndVersion(sandbox, filePath, targetVersion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version " + targetVersion + " not found for " + filePath));

        int nextVersion = fileVersionRepo.findMaxVersion(sandbox, filePath) + 1;

        FileVersion rollbackVersion = FileVersion.builder()
                .filePath(filePath)
                .content(target.getContent())
                .version(nextVersion)
                .agentId(agentId)
                .sandbox(sandbox)
                .intentReason("ROLLBACK to v" + targetVersion + ": " + intentReason)
                .operation("WRITE")
                .archived(false)
                .createdAt(LocalDateTime.now())
                .build();

        fileVersionRepo.save(rollbackVersion);
        audit(agentId, sandbox, "ROLLBACK", filePath, intentReason, "ALLOWED",
              "Rolled back to v" + targetVersion + " as v" + nextVersion);

        return rollbackVersion;
    }

    /**
     * Get audit logs for an agent.
     */
    public List<AuditLog> getAuditLogs(String agentId) {
        return auditLogRepo.findByAgentIdOrderByTimestampDesc(agentId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Safety Layer Implementations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Safety Layer 2: Sandbox resolution.
     * Every agent maps to exactly one sandbox.
     */
    private String resolveSandbox(String agentId) {
        String sandbox = agentSandboxMap.get(agentId);
        if (sandbox == null) {
            throw new SecurityException("Unknown agent: " + agentId +
                    ". Agent not registered in any sandbox.");
        }
        return sandbox;
    }

    /**
     * Safety Layer 2: Path enforcement.
     * Prevents the confused deputy problem — an agent can only access
     * paths within its sandbox's allowed prefixes.
     */
    private void assertPathAllowed(String agentId, String sandbox,
                                    String filePath, String operation) {
        Set<String> allowed = sandboxAllowedPaths.get(sandbox);
        boolean permitted = allowed != null &&
                allowed.stream().anyMatch(filePath::startsWith);

        if (!permitted) {
            String detail = "Agent " + agentId + " attempted " + operation +
                    " on path '" + filePath + "' outside sandbox '" + sandbox + "'";
            audit(agentId, sandbox, operation, filePath, null, "DENIED", detail);
            log.warn("[DENIED] {}", detail);
            throw new SecurityException(detail);
        }
    }

    /**
     * Safety Layer 3: Intent Validation.
     * High-impact operations (ARCHIVE) require a non-trivial reasoning chain.
     * In a real system this could call an LLM to verify the reasoning
     * matches storage policies (e.g., retention rules).
     */
    private void validateIntent(String operation, String intentReason) {
        if (!highImpactOperations.contains(operation)) return;

        if (intentReason == null || intentReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Intent reason is required for high-impact operation: " + operation);
        }

        // Simulate policy verification: reason must mention retention or policy
        boolean validReason = intentReason.toLowerCase().contains("retention") ||
                              intentReason.toLowerCase().contains("policy") ||
                              intentReason.toLowerCase().contains("expired") ||
                              intentReason.toLowerCase().contains("days");

        if (!validReason) {
            throw new IllegalArgumentException(
                    "Intent validation failed. Reason must reference retention policy " +
                    "or expiry. Got: '" + intentReason + "'");
        }

        log.info("[INTENT VALIDATED] operation={} reason={}", operation, intentReason);
    }

    /**
     * Persist every operation to the audit log — Jeff's audit trail.
     */
    private void audit(String agentId, String sandbox, String operation,
                        String filePath, String reason, String outcome, String details) {
        auditLogRepo.save(AuditLog.builder()
                .agentId(agentId)
                .sandbox(sandbox)
                .operation(operation)
                .filePath(filePath)
                .intentReason(reason)
                .outcome(outcome)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
