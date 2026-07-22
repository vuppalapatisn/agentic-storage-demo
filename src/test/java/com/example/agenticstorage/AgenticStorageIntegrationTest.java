package com.example.agenticstorage;

import com.example.agenticstorage.model.FileVersion;
import com.example.agenticstorage.service.AgenticStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class AgenticStorageIntegrationTest {

    @Autowired
    AgenticStorageService storageService;

    // ─── Safety Layer 1: Immutable Versioning ────────────────────────────────

    @Test
    void writeFile_createsNewVersionEachTime() {
        storageService.writeFile("log-agent-001", "/logs/test.log", "v1 content", "initial");
        storageService.writeFile("log-agent-001", "/logs/test.log", "v2 content", "update");

        List<FileVersion> history = storageService.getFileHistory("log-agent-001", "/logs/test.log");
        assertThat(history).hasSizeGreaterThanOrEqualTo(2);
        assertThat(history.get(0).getVersion()).isLessThan(history.get(1).getVersion());
    }

    @Test
    void archiveFile_doesNotDestroyHistory() {
        storageService.writeFile("log-agent-001", "/logs/archive-test.log",
                "content to archive", "initial");
        storageService.archiveFile("log-agent-001", "/logs/archive-test.log",
                "File expired per 90-day retention policy");

        // Latest is archived but history still exists
        Optional<FileVersion> latest = storageService
                .readFile("log-agent-001", "/logs/archive-test.log", null);
        assertThat(latest).isEmpty(); // archived = invisible as active

        List<FileVersion> history = storageService
                .getFileHistory("log-agent-001", "/logs/archive-test.log");
        assertThat(history).isNotEmpty(); // history still preserved!
    }

    @Test
    void rollback_restoresPreviousVersion() {
        storageService.writeFile("log-agent-001", "/logs/rollback-test.log",
                "good content", "initial");
        storageService.writeFile("log-agent-001", "/logs/rollback-test.log",
                "bad content", "mistake");
        storageService.rollback("log-agent-001", "/logs/rollback-test.log",
                1, "Rolling back bad deployment");

        Optional<FileVersion> latest = storageService
                .readFile("log-agent-001", "/logs/rollback-test.log", null);
        assertThat(latest).isPresent();
        assertThat(latest.get().getContent()).isEqualTo("good content");
    }

    // ─── Safety Layer 2: Sandboxing ──────────────────────────────────────────

    @Test
    void writeFile_blocksAccessOutsideSandbox() {
        // log-agent-001 is in log-sandbox → can only write to /logs/
        assertThatThrownBy(() ->
                storageService.writeFile("log-agent-001", "/system/passwd",
                        "malicious content", "trying to escape sandbox"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("sandbox");
    }

    @Test
    void writeFile_blocksUnknownAgent() {
        assertThatThrownBy(() ->
                storageService.writeFile("unknown-agent-999", "/logs/anything.log",
                        "content", "reason"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not registered");
    }

    // ─── Safety Layer 3: Intent Validation ───────────────────────────────────

    @Test
    void archiveFile_requiresIntent() {
        storageService.writeFile("log-agent-001", "/logs/intent-test.log",
                "content", "initial");

        assertThatThrownBy(() ->
                storageService.archiveFile("log-agent-001", "/logs/intent-test.log", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Intent reason is required");
    }

    @Test
    void archiveFile_rejectsVagueIntent() {
        storageService.writeFile("log-agent-001", "/logs/vague-intent.log",
                "content", "initial");

        assertThatThrownBy(() ->
                storageService.archiveFile("log-agent-001", "/logs/vague-intent.log",
                        "I want to delete this"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Intent validation failed");
    }

    @Test
    void archiveFile_acceptsValidRetentionReason() {
        storageService.writeFile("log-agent-001", "/logs/valid-intent.log",
                "content", "initial");

        FileVersion archived = storageService.archiveFile(
                "log-agent-001", "/logs/valid-intent.log",
                "Files older than 90 days per retention policy");

        assertThat(archived.isArchived()).isTrue();
    }

    // ─── Audit Trail ─────────────────────────────────────────────────────────

    @Test
    void auditLog_recordsAllOperations() {
        storageService.writeFile("log-agent-001", "/logs/audit-check.log",
                "data", "testing audit");
        var logs = storageService.getAuditLogs("log-agent-001");
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getOperation()).isNotBlank();
    }
}
