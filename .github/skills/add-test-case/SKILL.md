---
name: add-test-case
description: "Use when: adding a unit test, regression test, integration-style fixture test, or extending coverage in owlcms4. Keywords: add test case, add unit test, add regression test, write test, JUnit, ChampionshipTest, test coverage. Critical rule: do not create Vaadin UI objects in tests; extract non-UI logic first if coverage is needed."
---

# Add Test Case

This skill adds or updates tests for the `owlcms4` repository.

## Scope

Use this skill for test work in `owlcms4`, especially when extending existing JUnit coverage around competition, ranking, exports, team results, and fixture-backed behavior.

## Core Rule

Tests must not create Vaadin UI objects.

In this repository, Vaadin objects can latch onto database initialization. Server startup is allowed to return early to avoid 502 errors, while page rendering waits until the database is fully loaded. Tests that instantiate Vaadin-backed classes can therefore hang or never terminate while waiting on UI-side initialization behavior.

This includes, but is not limited to:

- Vaadin views and content classes
- `UI`
- grids, dialogs, layouts, and components
- tests that rely on constructing page/controller classes only to reach internal logic

If the behavior to test currently lives inside a Vaadin class, extract the non-UI logic into a helper, domain class, or other non-Vaadin seam first, then test that seam directly.

## Goal

Add coverage that validates behavior through stable non-UI logic with minimal setup and no UI framework construction.

## Workflow

1. Read the surrounding test file and nearby production code first.
2. Reuse existing fixture setup, helper methods, and assertion style where possible.
3. Identify the narrowest non-UI seam that expresses the behavior under test.
4. If the logic is trapped in a Vaadin class, move that logic to a non-UI helper or domain class.
5. Add or update tests against the non-UI logic.
6. Keep the test focused on observable behavior, not implementation trivia.
7. Avoid broad refactors unrelated to making the behavior testable.

## Test Design Rules

- Prefer extending an existing test class over creating a new one when the fixture and subject area already match.
- Prefer deterministic fixture-backed tests over mocked UI behavior.
- Use domain objects, repositories, exported beans, and helper classes directly.
- Keep assertions specific and behavior-oriented.
- Preserve repository test style, naming, and helper conventions.
- If a regression comes from page logic, test the extracted non-UI rules rather than the page object.

## Vaadin Prohibition

Do not do any of the following in tests:

- instantiate `TeamResultsContent`, `ResultsContent`, dialogs, views, or other Vaadin-backed classes just to call private helpers
- create `UI`, mock `UI`, or depend on attached Vaadin state
- use reflection as a substitute for extracting non-UI logic from a Vaadin class when the behavior can be moved cleanly

Allowed alternative:

- extract a helper such as a display-rules class, formatter, or domain calculator and test that helper directly

## Repository-Specific Notes

- `ChampionshipTest` already contains fixture-backed competition tests and is the preferred place for related team-results and championship-rule regressions.
- Changes to tests should respect the repository rule against unnecessary Maven/build execution without explicit user consent.
- When a test needs scoring behavior, use the fixture and existing helper methods rather than inventing parallel setup.

## Expected Output

When using this skill:

- add or update the smallest useful test coverage
- avoid Vaadin object creation in tests entirely
- extract non-UI logic first when needed
- summarize what behavior is now covered and where the non-UI seam lives

## Do Not

- do not create Vaadin UI objects in tests
- do not add UI-thread dependencies to tests
- do not add reflection-based tests against Vaadin page internals when a small extraction is viable
- do not run Maven or full builds without explicit user approval
- do not stage, commit, or push unless explicitly requested