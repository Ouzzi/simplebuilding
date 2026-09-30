# Claims stages 5 and 6 (2026-10-01)

Base: 7422a3ab. Worktree branch only; no push, merge, agents, clients or ports.
Stages 1–3 are present. Stage 4 belongs to a separate unmerged worker; completion
is not established. Claims remain disabled by default and are not enabled-ready.

## State and files

1. Stage 5: extend ClaimCommands and Claims with owner-only UUID trust/revocation,
   bounded immutable snapshots, shared mutation cooldown, and explicit OP4 admin
   commands. Reuse ClaimStore atomic publication. Resolve names only from online
   players; accept canonical offline UUIDs without lookup. The Vanilla name cache
   can perform blocking network lookups and fabricate offline-mode identities on a
   cache miss, so offline names are deliberately rejected with UUID guidance.
   Update module tests/catalogues, EN/DE resources, wiki and balance metadata.
2. Stage 6: consume the framework Protection contract in Simple Dimensions, retain
   unsupported foreign-provider rejection, check complete frame/interior and both
   travel endpoints, generated exits and destination construction. A denied return
   tries a checked emergency landing without granting access to the denied claim.
   Preserve settings, portal shapes, presets and loader packaging. Add actual
   activation/travel tests through public hooks with owner and second player.

## Risks and verification

- Failed persistence must not grant/revoke rights in memory; retain fail-closed lock.
- Trust does not convey administration. OP4 protection bypass is separately configured.
- Bound identifiers, trust counts and mutation history; preserve command collision guard.
- A supported provider cannot mask an unsupported foreign claim mod. Disabled/absent
  Tweaks must add no restrictions. No internal cross-module imports.
- Run relevant module server targets for Fabric/NeoForge 26.3, one filter per invocation;
  read each actual `alles gruen` line. Cap Gradle workers at 2. Preserve existing tests.
- Run config/lang/module checks, wiki generation/check and final worktree Gradle check.
  Record exact runs, gaps and the two stage commits. Full merged matrix is orchestrator work.

## Stage 5 result


- Stufe-5-Verifikation: 2026-09-30T23-18-06Z-4f7d, Fabric 21/21,
  NeoForge 21/21, **alles gruen 42/42** (Filter simpletweaks:*claims*).
  Echte Brigadier-Befehle, Online-Join/Offline-UUID, Zugriff/Widerruf, OP3/OP4,
  beide Bypass-Konfigurationen, Limits, atomare Fehler und bestehende Hooks geprüft.
  Der alte Strahl-Test lädt jetzt seinen Zielchunk vor dem Entity-Spawn; vorher
  war die Zielerfassung abhängig von der zufälligen Testposition.
  Wiki default/module generate/check und Modul-/EN-DE-Datenprüfung grün.
  Keine Clients, Besitzerwelt oder vollständige Sicherheitsmatrix geprüft.
