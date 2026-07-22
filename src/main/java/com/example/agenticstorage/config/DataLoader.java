package com.example.agenticstorage.config;

import com.example.agenticstorage.service.AgenticStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds demo data so you can immediately test the API after startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final AgenticStorageService storageService;

    @Override
    public void run(String... args) {
        log.info("=== Seeding demo data ===");

        // log-agent writes and rewrites a log file (immutable versioning demo)
        storageService.writeFile("log-agent-001", "/logs/app.log",
                "2024-01-01 INFO Application started", "Initial log entry");
        storageService.writeFile("log-agent-001", "/logs/app.log",
                "2024-01-01 INFO Application started\n2024-01-01 ERROR NullPointerException",
                "Appending error entry");
        storageService.writeFile("log-agent-001", "/logs/app.log",
                "2024-01-01 INFO Application started\n2024-01-01 INFO Recovered",
                "Corrected log after recovery");

        // code-agent writes a Python script
        storageService.writeFile("code-agent-001", "/code/remediation.py",
                "# Auto-generated remediation script\ndef remediate(): pass",
                "Initial scaffold");
        storageService.writeFile("code-agent-001", "/code/remediation.py",
                "# Auto-generated remediation script\ndef remediate():\n    restart_service('app')",
                "Added restart logic");

        // ops-agent writes a playbook
        storageService.writeFile("ops-agent-001", "/playbooks/incident-001.md",
                "# Incident 001\n## Steps\n1. Check logs\n2. Restart service",
                "Initial playbook");

        log.info("=== Demo data seeded. API ready at http://localhost:8080 ===");
        log.info("=== H2 Console: http://localhost:8080/h2-console ===");
    }
}
