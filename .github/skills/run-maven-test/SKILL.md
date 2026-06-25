---
name: run-maven-test
description: "Use when: running owlcms Maven/Surefire tests after explicit human consent, especially single Java test classes, AllTests, reactor module dependencies, -pl owlcms -am, shared module no matching tests, surefire.failIfNoSpecifiedTests, Surefire reports, Maven output missing or failed to retrieve. Keywords: mvn test, Maven runner, Surefire, target/surefire-reports, BUILD FAILURE, test report."
---

# Run OWLCMS Maven Tests and Read Surefire Reports

This skill documents the Maven/Surefire path for owlcms when the user has explicitly allowed Maven for more detailed test output.

## When To Use

- The user explicitly authorizes Maven, asks to use the Maven runner, or needs stack traces/assertion details that the Java Test Runner did not show.
- You need to run one Java test class, a small set of Java test classes, or `AllTests` through Surefire.
- The terminal tool reports `Failed to retrieve command output` after a Maven test run.
- Maven output is too large, truncated, or less useful than Surefire reports.

## Permission

- Do not run Maven in this repository unless the user has explicitly consented in the current task context.
- Prefer the VS Code Java Test Runner first when consent has not been given. See `../run-java-test/SKILL.md`.
- Once consent is given, keep Maven runs narrow: run the specific test class or suite needed for the current diagnosis.

## Required Maven Shape

The `owlcms` test module depends on sibling modules such as `shared`. A plain module-only run can fail because dependencies are not built in the reactor. Use `-pl owlcms -am`:

```bash
mvn -pl owlcms -am -Dtest=SomeTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
```

Why each flag matters:

- `-pl owlcms`: run the `owlcms` module where most app tests live.
- `-am`: also make required upstream modules such as `shared`.
- `-Dtest=SomeTest`: keep the run focused.
- `-DfailIfNoTests=false`: tolerate modules in the reactor that have no tests for this run.
- `-Dsurefire.failIfNoSpecifiedTests=false`: tolerate upstream modules like `shared` not having a matching `SomeTest` class.

For multiple focused classes, use a comma-separated `-Dtest` value:

```bash
mvn -pl owlcms -am -Dtest=TwoMinutesRuleTest,LiftingOrderReconstructionTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
```

For `AllTests`, use the suite class name:

```bash
mvn -pl owlcms -am -Dtest=AllTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
```

## Read Surefire Reports Directly

Do not rely only on captured Maven terminal output. In this environment Maven output may be huge, truncated, written to a chat-session resource file, or the terminal tool may say `Failed to retrieve command output` even though Surefire reports were written.

After every Maven test run, inspect the direct report files under:

```text
owlcms/target/surefire-reports/
```

For a specific class, read:

```text
owlcms/target/surefire-reports/app.owlcms.tests.SomeTest.txt
owlcms/target/surefire-reports/TEST-app.owlcms.tests.SomeTest.xml
```

For test classes in subpackages, include the full package path in the filename, for example:

```text
owlcms/target/surefire-reports/app.owlcms.tests.migration.ChampionshipV65MigrationTest.txt
```

A passing `.txt` report looks like:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

Failure line numbers and stack traces are usually in the `.txt` report. XML reports are useful for exact method names, assertions, elapsed time, and stdout/stderr when the text report is sparse.

## Practical Workflow

1. Confirm Maven consent exists.
2. Run the narrow reactor command with `-pl owlcms -am` and the two no-match flags.
3. If terminal output is missing or truncated, do not rerun immediately. First read the Surefire `.txt` report for the test class.
4. If the report timestamp did not change or the report is missing, check whether Maven is still running before retrying.
5. Use Surefire failure line numbers to inspect the smallest relevant code path.
6. After editing Java, run `get_errors` on touched Java files before reporting success.
7. Rerun the same focused Maven command and read the refreshed Surefire report.

## Useful Report Commands

Check report freshness:

```bash
ls -l owlcms/target/surefire-reports/app.owlcms.tests.SomeTest.txt owlcms/target/surefire-reports/TEST-app.owlcms.tests.SomeTest.xml
```

Extract failures from a text report:

```bash
grep -n -A12 -B4 'FAILURE\|ERROR\|java.lang.AssertionError\|SomeTest.java' owlcms/target/surefire-reports/app.owlcms.tests.SomeTest.txt
```

Check whether a Maven run is still active before rerunning:

```bash
ps -ef | grep '[m]vn -pl owlcms'
```

## Do Not

- Do not interpret `Failed to retrieve command output` as a Maven/test failure by itself.
- Do not rerun Maven repeatedly before checking `target/surefire-reports`.
- Do not omit `-am` for `owlcms` tests that need upstream modules.
- Do not omit `-Dsurefire.failIfNoSpecifiedTests=false` when the reactor includes modules without the selected test class.
- Do not run broad Maven test suites when a focused class or package-level failure is enough to diagnose the current issue.
