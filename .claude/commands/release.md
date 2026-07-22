---
description: Verify on Java 21, then commit the source changes and push to main.
argument-hint: [optional commit message]
---

# /release

Ship the current working-tree changes to `main` for the agentic-storage-demo,
safely. Run these steps in order and stop if any step fails.

## 1. Verify first (never push a red build)

Run the **verify-java21** skill (`mvn clean verify` on Java 21). If tests fail
or the build fails, STOP and report the failure — do not commit or push.

## 2. Review what will be committed

```bash
git status --short
```

- Stage source and config only. **Never stage `target/`** (build output) or
  `.claude/settings.local.json` (machine-local). Both are gitignored, but
  double-check with:
  ```bash
  git diff --cached --name-only | grep -E '^target/|settings\.local\.json' && echo "STOP: local/build files staged"
  ```
- If there are unexpected files, ask before proceeding.

## 3. Commit

Use `$ARGUMENTS` as the commit message if provided; otherwise write a concise
message summarizing the change. End the message with:

```
Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
```

## 4. Push to main

This repo has been using a push-straight-to-`main` flow from the worktree
branch:

```bash
git push origin HEAD:main
```

Confirm the push output shows the ref update (e.g. `abc123..def456  HEAD -> main`).

## 5. Report

Summarize: what was verified (JDK + test tally), the commit hash, and the
`origin/main` ref update. Remind the user to `git pull origin main` in their
main project directory to pick up the changes locally.
