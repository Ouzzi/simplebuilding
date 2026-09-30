---
name: hauptlinie-26-3-zuerst
description: "Ab 2026-09-29 ist 26.3 (neueste Version) die Hauptlinie - erst dort alles fertig implementieren und testen, erst danach auf 26.2/Forge/1.21.11/26.4 portieren"
metadata:
  node_type: memory
  type: feedback
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-29T12:05:00.278Z
---

Neue Regel des Besitzers (2026-09-29): 26.3 ist die Hauptlinie. Features erst fuer 26.3 umsetzen und nur dort testen (fabric-263, neoforge-263), bis alles so ist wie gewuenscht und der Run/die Session abgeschlossen ist. Erst dann in einem eigenen Port-Run die anderen Linien (26.2 Fabric/NeoForge/Forge, 1.21.11-Kopie, 26.4) nachziehen und testen.

**Why:** Der Besitzer will schneller iterieren; parallele Pflege aller Linien pro Feature kostet Zeit und erzeugt Konflikte.
**How to apply:** Agenten-Briefings: Tests nur auf fabric-263/neoforge-263, mc1_21_11-Kopie nicht anfassen; geteilter Code (common/src/shared) muss trotzdem fuer 26.2 kompilieren (`check` bleibt gruen). Nach Abschluss einer Welle einen Port-Run fuer die uebrigen Linien starten. Siehe [[mc-26.3-overlay-linie]], [[testlaeufe-sparsam]].
