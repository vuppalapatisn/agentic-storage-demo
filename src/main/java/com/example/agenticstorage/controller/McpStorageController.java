package com.example.agenticstorage.controller;

import com.example.agenticstorage.model.AuditLog;
import com.example.agenticstorage.model.FileVersion;
import com.example.agenticstorage.model.McpDtos.*;
import com.example.agenticstorage.service.AgenticStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * McpStorageController — REST API that simulates an MCP Server.
 *
 * In the MCP architecture from the video, these endpoints represent the
 * "tools" that an AI agent can invoke:
 *
 *   POST /mcp/tools/write_file
 *   POST /mcp/tools/read_file
 *   POST /mcp/tools/archive_file
 *   GET  /mcp/tools/list_directory/{agentId}
 *   GET  /mcp/tools/file_history/{agentId}
 *   POST /mcp/tools/rollback
 *   GET  /mcp/audit/{agentId}
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpStorageController {

    private final AgenticStorageService storageService;

    // ─── MCP Tool: write_file ─────────────────────────────────────────────────

    @PostMapping("/tools/write_file")
    public ResponseEntity<McpResponse<FileVersion>> writeFile(
            @Valid @RequestBody WriteFileRequest request) {
        try {
            FileVersion fv = storageService.writeFile(
                    request.getAgentId(),
                    request.getFilePath(),
                    request.getContent(),
                    request.getIntentReason());
            return ResponseEntity.ok(
                    McpResponse.ok("File written. Version " + fv.getVersion() + " created.", fv));
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(McpResponse.error("SANDBOX VIOLATION: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(McpResponse.error(e.getMessage()));
        }
    }

    // ─── MCP Tool: read_file ──────────────────────────────────────────────────

    @PostMapping("/tools/read_file")
    public ResponseEntity<McpResponse<FileVersion>> readFile(
            @Valid @RequestBody ReadFileRequest request) {
        try {
            Optional<FileVersion> fv = storageService.readFile(
                    request.getAgentId(),
                    request.getFilePath(),
                    request.getVersion());
            return fv.map(v -> ResponseEntity.ok(McpResponse.ok("File retrieved.", v)))
                     .orElseGet(() -> ResponseEntity.status(404)
                             .body(McpResponse.error("File not found or archived.")));
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(McpResponse.error("SANDBOX VIOLATION: " + e.getMessage()));
        }
    }

    // ─── MCP Tool: archive_file ───────────────────────────────────────────────

    @PostMapping("/tools/archive_file")
    public ResponseEntity<McpResponse<FileVersion>> archiveFile(
            @Valid @RequestBody ArchiveFileRequest request) {
        try {
            FileVersion fv = storageService.archiveFile(
                    request.getAgentId(),
                    request.getFilePath(),
                    request.getIntentReason());
            return ResponseEntity.ok(
                    McpResponse.ok("File archived (not deleted). Full history preserved.", fv));
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(McpResponse.error("SANDBOX VIOLATION: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(McpResponse.error("INTENT VALIDATION FAILED: " + e.getMessage()));
        }
    }

    // ─── MCP Tool: list_directory ─────────────────────────────────────────────

    @GetMapping("/tools/list_directory/{agentId}")
    public ResponseEntity<McpResponse<List<String>>> listDirectory(
            @PathVariable String agentId) {
        try {
            List<String> files = storageService.listDirectory(agentId);
            return ResponseEntity.ok(
                    McpResponse.ok("Active files in sandbox.", files));
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(McpResponse.error(e.getMessage()));
        }
    }

    // ─── MCP Tool: file_history ───────────────────────────────────────────────

    @GetMapping("/tools/file_history/{agentId}")
    public ResponseEntity<McpResponse<List<FileVersion>>> fileHistory(
            @PathVariable String agentId,
            @RequestParam String filePath) {
        try {
            List<FileVersion> history = storageService.getFileHistory(agentId, filePath);
            return ResponseEntity.ok(
                    McpResponse.ok("Full version history.", history));
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(McpResponse.error(e.getMessage()));
        }
    }

    // ─── MCP Tool: rollback ───────────────────────────────────────────────────

    @PostMapping("/tools/rollback")
    public ResponseEntity<McpResponse<FileVersion>> rollback(
            @RequestBody RollbackRequest request) {
        try {
            FileVersion fv = storageService.rollback(
                    request.getAgentId(),
                    request.getFilePath(),
                    request.getTargetVersion(),
                    request.getIntentReason());
            return ResponseEntity.ok(
                    McpResponse.ok("Rolled back. New version " + fv.getVersion() + " created.", fv));
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(McpResponse.error("SANDBOX VIOLATION: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(McpResponse.error(e.getMessage()));
        }
    }

    // ─── Audit Trail ──────────────────────────────────────────────────────────

    @GetMapping("/audit/{agentId}")
    public ResponseEntity<McpResponse<List<AuditLog>>> auditLog(
            @PathVariable String agentId) {
        List<AuditLog> logs = storageService.getAuditLogs(agentId);
        return ResponseEntity.ok(McpResponse.ok("Audit trail for " + agentId, logs));
    }

    // ─── Inner DTO ────────────────────────────────────────────────────────────

    @lombok.Data
    public static class RollbackRequest {
        private String agentId;
        private String filePath;
        private int targetVersion;
        private String intentReason;
    }
}
