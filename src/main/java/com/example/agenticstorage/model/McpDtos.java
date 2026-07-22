package com.example.agenticstorage.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTOs used by the MCP-style REST API.
 */
public class McpDtos {

    // ─── Write File Request ───────────────────────────────────────────────────

    @Data
    public static class WriteFileRequest {
        @NotBlank(message = "agentId is required")
        private String agentId;

        @NotBlank(message = "filePath is required")
        private String filePath;

        @NotBlank(message = "content is required")
        private String content;

        /** Intent reason — required for high-impact operations */
        private String intentReason;
    }

    // ─── Archive (Logical Delete) Request ────────────────────────────────────

    @Data
    public static class ArchiveFileRequest {
        @NotBlank(message = "agentId is required")
        private String agentId;

        @NotBlank(message = "filePath is required")
        private String filePath;

        /** REQUIRED for archive operations (intent validation) */
        @NotBlank(message = "intentReason is required for archive operations")
        private String intentReason;
    }

    // ─── Read File Request ────────────────────────────────────────────────────

    @Data
    public static class ReadFileRequest {
        @NotBlank(message = "agentId is required")
        private String agentId;

        @NotBlank(message = "filePath is required")
        private String filePath;

        /** Optional: read a specific version; null = latest */
        private Integer version;
    }

    // ─── Generic Response ─────────────────────────────────────────────────────

    @Data
    public static class McpResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> McpResponse<T> ok(String message, T data) {
            McpResponse<T> r = new McpResponse<>();
            r.success = true;
            r.message = message;
            r.data = data;
            return r;
        }

        public static <T> McpResponse<T> error(String message) {
            McpResponse<T> r = new McpResponse<>();
            r.success = false;
            r.message = message;
            return r;
        }
    }
}
