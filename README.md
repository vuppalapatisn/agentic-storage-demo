# Agentic Storage Demo — Spring Boot

A Spring Boot application demonstrating **Agentic Storage** concepts from the IBM Technology video, implemented as an MCP-style REST API.

## What This Demonstrates

| Concept | Implementation |
|---|---|
| **MCP Server** | REST API at `/mcp/tools/*` mimicking MCP tool primitives |
| **Immutable Versioning** | Every write creates a new DB row; files can only be archived, never deleted |
| **Sandboxing** | Agents are mapped to sandboxes with allowed path prefixes |
| **Intent Validation** | Archive operations require a policy-backed reason |
| **Audit Trail** | Every operation logged to `audit_logs` table |
| **Rollback** | Restore any previous version as a new version |

---

## Architecture

```
AI Agent
   │
   ▼  (HTTP / JSON-RPC)
McpStorageController          ← MCP Server (this app)
   │
   ▼
AgenticStorageService
   ├── Safety Layer 1: Immutable Versioning
   ├── Safety Layer 2: Sandboxing
   └── Safety Layer 3: Intent Validation
   │
   ▼
H2 Database (file_versions + audit_logs)
```

## Agent → Sandbox Mapping

| Agent ID | Sandbox | Allowed Paths |
|---|---|---|
| `log-agent-001` | log-sandbox | `/logs/` |
| `code-agent-001` | code-sandbox | `/code/`, `/artifacts/` |
| `ops-agent-001` | ops-sandbox | `/playbooks/`, `/incidents/` |

---

## Running the App

```bash
./mvnw spring-boot:run
```

App starts at: **http://localhost:8080**  
H2 Console: **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:agenticstorage`)

---

## API Reference & curl Examples

### ✅ Write a File (MCP Tool: write_file)
```bash
curl -X POST http://localhost:8080/mcp/tools/write_file \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "log-agent-001",
    "filePath": "/logs/app.log",
    "content": "2024-01-15 INFO Service restarted",
    "intentReason": "Logging restart event"
  }'
```

### ✅ Read Latest Version (MCP Tool: read_file)
```bash
curl -X POST http://localhost:8080/mcp/tools/read_file \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "log-agent-001",
    "filePath": "/logs/app.log"
  }'
```

### ✅ Read a Specific Version
```bash
curl -X POST http://localhost:8080/mcp/tools/read_file \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "log-agent-001",
    "filePath": "/logs/app.log",
    "version": 1
  }'
```

### ✅ List Active Files in Sandbox
```bash
curl http://localhost:8080/mcp/tools/list_directory/log-agent-001
```

### ✅ View Full Version History
```bash
curl "http://localhost:8080/mcp/tools/file_history/log-agent-001?filePath=/logs/app.log"
```

### ✅ Archive a File (requires valid intent)
```bash
curl -X POST http://localhost:8080/mcp/tools/archive_file \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "log-agent-001",
    "filePath": "/logs/app.log",
    "intentReason": "Files older than 90 days per retention policy"
  }'
```

### ❌ Archive with vague reason → Intent Validation Failure
```bash
curl -X POST http://localhost:8080/mcp/tools/archive_file \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "log-agent-001",
    "filePath": "/logs/app.log",
    "intentReason": "I want to clean up"
  }'
# Returns 400: INTENT VALIDATION FAILED
```

### ❌ Sandbox escape attempt → Security Denial
```bash
curl -X POST http://localhost:8080/mcp/tools/write_file \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "log-agent-001",
    "filePath": "/system/passwd",
    "content": "hacked",
    "intentReason": "trying to escape"
  }'
# Returns 403: SANDBOX VIOLATION
```

### ✅ Rollback to Previous Version
```bash
curl -X POST http://localhost:8080/mcp/tools/rollback \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "log-agent-001",
    "filePath": "/logs/app.log",
    "targetVersion": 1,
    "intentReason": "Reverting bad deployment"
  }'
```

### ✅ View Audit Trail
```bash
curl http://localhost:8080/mcp/audit/log-agent-001
```

---

## Running Tests

```bash
./mvnw test
```

Tests cover all 3 safety layers:
- Immutable versioning (write, archive, rollback)
- Sandboxing (path enforcement, unknown agent)
- Intent validation (missing reason, vague reason, valid reason)

---

## Mapping to Video Concepts

| Video Concept | Code Location |
|---|---|
| "Context window = RAM (volatile)" | `DataLoader` seeds state that survives between requests |
| "RAG is read-only" | `readFile()` = resource access; write ops add persistence |
| "MCP uniform interface" | `McpStorageController` exposes tool primitives |
| "Immutable versioning" | `FileVersionRepository` + `version` field, never UPDATE |
| "Sandboxing / confused deputy" | `assertPathAllowed()` in `AgenticStorageService` |
| "Intent validation" | `validateIntent()` checks reasoning chain |
| "Audit trail (Jeff's requirement)" | `AuditLog` entity + `auditLogRepo` |
