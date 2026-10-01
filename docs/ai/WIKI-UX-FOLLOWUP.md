# Wiki UX follow-up — 2026-10-01

Scope: small browser-only improvements on top of `17c5c754`. No generated
data, gameplay, module selection, or Minecraft runtime changes.

## Implemented

- Remember list search text per module and route across navigation and reload.
  Blocked local storage falls back to memory for the current page session.
- Preserve recipe view search text alongside the existing filter/view options;
  store this state per module and retain legacy saved options as defaults.
- Explain empty lists and unsuccessful filters in English and German. Generic
  list filters offer a reset button that restores rows and returns input focus.
- Keep table headings visible in bounded scrolling table regions, including
  narrow screens. Regions are keyboard focusable and have translated labels.

## Already present

Sidebar section counters, saved folding, recipe view toggles, missing-entry IDs
with suggestions/search/back links, and individual config-entry links.

## Validation

- `node wiki/tests/ui_lists.cjs` using installed Playwright and Edge: passed.
  Exercises history/reload, list/module isolation, reset, recipe query persistence,
  sticky-header geometry, a 390px viewport, and unavailable local storage.
- `python -m unittest discover -s wiki/tests -q`: 19 tests passed.
- `python wiki/generate.py --all --check`: up to date, everything documented.
- `git diff --check`: clean.

The browser check uses an ephemeral loopback server and its own headless browser;
it neither starts nor closes an owner browser or Minecraft client. Playwright
must be available through the environment (for example NODE_PATH); WIKI_BROWSER
can select an installed Chromium channel instead of the default `msedge`.

## Remaining

Generic row deep links (`?f=<id>`) need a consistent row identity and router flow
across all list types; they are not included. Config bounds/scope columns require
generator and code evidence. Owner desktop/mobile acceptance remains separate.
