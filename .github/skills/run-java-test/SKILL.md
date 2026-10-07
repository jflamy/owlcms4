---
name: run-java-test
description: "Use when: running a Java test class or suite through Maven/Surefire, including JUnit 4 WildcardPatternSuite like AllTests. Focused Maven test runs have standing maintainer authorization. Keywords: run Java test, JUnit, AllTests, Maven, Surefire, test file not found, No tests found in the files."
---

# Run a Java Test via Maven/Surefire

Use Maven/Surefire as the default Java test runner. VS Code test discovery,
an active editor and the Java Test Runner command tool are not prerequisites.

## When to use

- The user asks to run a specific Java test class or the `AllTests` suite.
- Validate a Java test file or production code currently being changed.
- The generic VS Code test runner does not discover the requested Java tests.

## Permission

- The maintainer has given standing authorization for focused Maven test
  runs and their prerequisite compilation. Do not ask again for each run.
- This authorization covers testing, not packaging, installation, deployment,
  application launch or unrelated builds. Respect any current user instruction
  that limits or forbids test execution.
- Keep selectors narrow; run a full suite only when requested or when targeted
  results establish that broader verification is needed.

## Required steps

1. Load `run-maven-test` for reactor selection and Surefire report handling.
2. Run the relevant test class from the repository root:

   ```bash
   mvn -pl owlcms -am -Dtest=SomeTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
   ```

3. Read the fresh report under `owlcms/target/surefire-reports/`, using the
   test's full package name. Report tests run, failures, errors and skips.
4. Diagnose failures from the report before rerunning. Check workspace Java
   diagnostics after source edits.

## Do not

- Do not call `runTests` directly on a Java test file path — it returns
  `No tests found in the files`.
- Do not require VS Code editor activation or extension commands to run tests.
- Do not invoke packaging or deployment goals under test authorization.
- Do not rerun a command merely because captured output is missing; inspect
  Surefire reports and the original process first.
