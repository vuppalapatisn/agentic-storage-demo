package com.example.agenticstorage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

/**
 * Sandbox Configuration — Safety Layer 2: Sandboxing
 *
 * Each agent is bound to a named sandbox and is only allowed to
 * access paths within allowed prefixes. This prevents the
 * "confused deputy" problem where an agent with broad permissions
 * gets tricked into acting outside its intended scope.
 *
 * Example:
 *   - log-agent  → can only access /logs/
 *   - code-agent → can only access /code/ and /artifacts/
 */
@Configuration
public class SandboxConfig {

    /**
     * Maps agentId → sandbox name.
     * In production this would come from a database or config server.
     */
    @Bean
    public Map<String, String> agentSandboxMap() {
        return Map.of(
            "log-agent-001",  "log-sandbox",
            "code-agent-001", "code-sandbox",
            "ops-agent-001",  "ops-sandbox"
        );
    }

    /**
     * Maps sandbox name → set of allowed path prefixes.
     */
    @Bean
    public Map<String, Set<String>> sandboxAllowedPaths() {
        return Map.of(
            "log-sandbox",  Set.of("/logs/"),
            "code-sandbox", Set.of("/code/", "/artifacts/"),
            "ops-sandbox",  Set.of("/playbooks/", "/incidents/")
        );
    }

    /**
     * Operations that require intent validation before executing.
     * Safety Layer 3: Intent Validation for high-impact ops.
     */
    @Bean
    public Set<String> highImpactOperations() {
        return Set.of("ARCHIVE");
    }
}
