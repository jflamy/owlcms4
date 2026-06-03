---
name: vaadin-skills
description: "Use when: writing, styling, fixing, or reviewing Vaadin UI code in owlcms_67 (Vaadin Flow, Java server-side) — layouts, forms/Binder, Grid/ComboBox data providers, theming, routing/navigation, security, or Vaadin tests. Keywords: Vaadin, Flow, Vaadin 25, HorizontalLayout, VerticalLayout, FlexLayout, AppLayout, Binder, GridField, Grid, ComboBox, data provider, Lumo, Aura theme, route, TestBench, Browserless. This skill points to the official Vaadin agent-skills and the Vaadin MCP server."
---

# Vaadin Skills (pointer)

owlcms is a **Vaadin Flow** application (server-side Java, package `app.owlcms.nui.*`).
For Vaadin-specific work, prefer the authoritative Vaadin guidance over guessing.

## 1. Vaadin MCP server (already available in this workspace)

This workspace already exposes the Vaadin MCP tools. Use them first for version-accurate
component APIs, styling, and docs (load them with `tool_search` if they are deferred):

- `mcp_vaadin_get_component_java_api` — Flow (Java) component API
- `mcp_vaadin_get_component_web_component_api` — underlying web component API
- `mcp_vaadin_get_component_styling` / `mcp_vaadin_get_theme_css_properties` — styling/theme tokens
- `mcp_vaadin_search_vaadin_docs` / `mcp_vaadin_get_full_document` — docs search/fetch
- `mcp_vaadin_get_vaadin_primer` — high-level primer
- `mcp_vaadin_get_components_by_version` / `mcp_vaadin_get_latest_vaadin_version` — version info

Because owlcms is **Flow / Java**, prefer the *Java* API tools. Ignore React/Hilla
client-side guidance unless a task is explicitly about a client-side view.

## 2. Official Vaadin agent-skills

Source of truth: https://github.com/vaadin/agent-skills

These are designed for Vaadin 25 apps and installed with the `vercel-labs/skills` CLI.
To pull the full skill content locally (creates/updates a skills folder):

```bash
# all skills
npx skills add vaadin/agent-skills

# only the ones relevant to a Flow/server-side app like owlcms
npx skills add vaadin/agent-skills \
  --skill forms-and-validation \
  --skill data-providers \
  --skill vaadin-layouts \
  --skill responsive-layouts \
  --skill reusable-components \
  --skill views-and-navigation \
  --skill theming \
  --skill frontend-design \
  --skill security \
  --skill third-party-components \
  --skill ui-unit-testing \
  --skill testbench-testing
```

### Available skills and when to use them

Relevant to owlcms (Vaadin Flow):

- `vaadin-layouts` — HorizontalLayout, VerticalLayout, FlexLayout, AppLayout, spacing, sizing, alignment. Note: be careful with `setFlexGrow` / `setFlexShrink`.
- `responsive-layouts` — responsive desktop/mobile layouts.
- `forms-and-validation` — Binder-based forms, validation, converters, submission.
- `data-providers` — Grid/ComboBox data providers, lazy loading, filtering, sorting, pagination.
- `reusable-components` — splitting large Flow views into focused reusable components.
- `views-and-navigation` — views, routes, router layouts, navigation menus, URL parameters.
- `theming` / `aura-theme` — Aura and Lumo theme customization.
- `frontend-design` — polished UI beyond default theme styling.
- `security` — Spring Security, route access control, login/logout, roles, OAuth2.
- `third-party-components` — integrating third-party web/React components into Flow.
- `ui-unit-testing` — fast browser-free Browserless view tests.
- `testbench-testing` — end-to-end browser tests with TestBench.
- `signals` — Vaadin Signals reactive state (mostly client-side; rarely needed in owlcms Flow code).

Mostly NOT relevant to owlcms (client-side React/Hilla):

- `client-side-views` — React/Hilla views, file-based routes (owlcms uses Flow, not Hilla).

## 3. owlcms-specific Vaadin guidance

- Do not create Vaadin UI objects in unit tests — see the `add-test-case` skill.
- `OwlcmsSession.getLocale()` / `OwlcmsSession.withFop()` rely on `UI.getCurrent()`; capture
  values on the UI thread before entering background threads/callbacks (see copilot-instructions).
- After editing Java, validate with the `verify-java-fix` skill (`get_errors`), do not run Maven
  without explicit consent.
