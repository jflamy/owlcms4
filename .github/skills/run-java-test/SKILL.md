---
name: run-java-test
description: "Use when: running a Java test class or suite (including JUnit 4 WildcardPatternSuite like `AllTests`) through the VS Code Java Test Runner from an agent. Keywords: run Java test, JUnit, AllTests, vscode test runner, java.test.editor.run, test file not found, No tests found in the files."
---

# Run a Java Test via the VS Code Test Runner

This skill documents the only reliable way to run a Java test class in this
workspace from an automated agent, without invoking Maven.

## When to use

- The user asks to run a specific Java test class or the `AllTests` suite.
- The generic VS Code test runner tool returns `No tests found in the files`
  when given a Java test file path.

## Permission

- You do NOT need to ask for permission to run the test file you are currently
  fixing or editing. Running these tests via the Java Test Runner
  (`java.test.editor.run`) is a safe, read-only verification step — just run it.
- The `mvn`/build human-consent rule still applies: never fall back to
  `mvn test` to run tests without explicit human consent.

## Why the generic runner is insufficient

The generic VS Code test runner tool (`runTests`) does not discover Java tests
in this workspace, even when given the absolute path of a `*Test.java` file
or the path of a JUnit 4 `WildcardPatternSuite` class such as
`owlcms/src/test/java/app/owlcms/tests/AllTests.java`. It returns
`No tests found in the files`.

The Java Test Runner extension exposes `java.test.editor.run`, which runs the
tests defined in the **currently active editor**. That command works for both
plain JUnit classes and the `AllTests` wildcard suite.

## Required steps

1. Open the target test file in the active VS Code window using the `code`
   CLI so it becomes the active editor:

   ```bash
   code -r /absolute/path/to/SomeTest.java
   ```

   Use `-r` (reuse window). Do not use the `vscode.open` command via
   `run_vscode_command` — it fails in the agent tool context.

2. Run the Java Test Runner command on the current file:

   - command: `java.test.editor.run`
   - name: `Java: Run Tests in Current File`

3. Read the returned JSON payload. Failures appear as items with
   `ownComputedState == 4`. The payload is usually large and gets written to a
   `chat-session-resources/.../content.txt` file — parse it with `jq`:

   ```bash
   tail -n +3 "$out" | jq -r '.items[]
     | select(.ownComputedState == 4)
     | .item.label'
   ```

4. The Java Test Runner does not include stack traces in the returned
   payload, and the underlying test task output is not retrievable by task id
   afterward. Report which test methods failed and inspect the source to
   diagnose. Do not fall back to Maven without explicit human consent.

## State code reference (Java Test Runner payload)

| ownComputedState | Meaning   |
| ---------------- | --------- |
| 0                | Unset     |
| 1                | Queued    |
| 2                | Running   |
| 3                | Passed    |
| 4                | Failed    |
| 5                | Skipped   |
| 6                | Errored   |

## Do not

- Do not call `runTests` directly on a Java test file path — it returns
  `No tests found in the files`.
- Do not run `mvn test` to work around this — Maven requires explicit human
  consent in this repository.
- Do not try to call `java.test.runTests` directly — it expects internal
  VS Code `TestRunRequest` objects that cannot be constructed from the agent.
