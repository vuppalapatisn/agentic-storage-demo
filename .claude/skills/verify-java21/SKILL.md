---
name: verify-java21
description: Build and test the agentic-storage-demo on Java 21 and report the result. Use when asked to build, verify, run the tests, or confirm the app still works after a change. Optionally boots the packaged jar to confirm startup.
---

# verify-java21

Build and test this Spring Boot project against **Java 21** (the version CI
uses) and report clearly whether it passed.

## Environment

- This is a Maven project (`pom.xml`); there is **no working `mvnw.cmd`**, so use
  the system `mvn`.
- Java 21 lives at `C:\Program Files\OpenLogic\jdk-21.0.3.9-hotspot`.
- The shell is PowerShell. Set `JAVA_HOME` for the command, do not rely on a
  global default.

## Steps

1. Run the full build + test:

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\OpenLogic\jdk-21.0.3.9-hotspot"
   mvn --batch-mode --no-transfer-progress clean verify
   ```

2. Report the outcome from the summary lines:
   - Look for `Tests run: N, Failures: F, Errors: E` and `BUILD SUCCESS` /
     `BUILD FAILURE`.
   - If anything fails, read `target/surefire-reports/*.txt` and quote the first
     real `Caused by:` — do **not** just report the top-level
     `Failed to load ApplicationContext` cascade, which hides the root cause.

3. (Optional, only if asked to "confirm it boots" / "run the app") Launch the
   packaged jar in the background, wait for startup, then stop it:

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\OpenLogic\jdk-21.0.3.9-hotspot"
   $log = "$env:TEMP\agentic-run.log"
   $p = Start-Process "$env:JAVA_HOME\bin\java.exe" `
        -ArgumentList "-jar","target\agentic-storage-demo-1.0.0.jar" `
        -RedirectStandardOutput $log -RedirectStandardError "$env:TEMP\agentic-run.err.log" `
        -PassThru -NoNewWindow
   # wait until "Started AgenticStorageApplication" or "Application run failed" appears in $log
   # then: Stop-Process -Id $p.Id -Force
   ```

   A healthy boot logs `Started AgenticStorageApplication`, the seed writes
   (`[WRITE] agent=... sandbox=...`), and `Demo data seeded. API ready`. A
   `SecurityException ... outside sandbox` means the seed data was rejected —
   that is an app bug, not a Java-version problem.

## Report format

State the JDK used, the test tally (e.g. `9/9 passed`), and whether the build
succeeded. If it failed, give the root cause and the file/line.
