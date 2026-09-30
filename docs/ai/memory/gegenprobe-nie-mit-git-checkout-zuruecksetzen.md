---
name: gegenprobe-nie-mit-git-checkout-zuruecksetzen
description: "Eine Gegenprobe an einer Datei mit uncommitteten Aenderungen nie per git checkout zuruecknehmen; mutations.py setzt so zurueck, darum vorher committen"
metadata:
  node_type: memory
  type: feedback
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-23T22:44:53.126Z
---

Eine schnelle Gegenprobe (Datei kurz verfaelschen, Test rot sehen, zuruecknehmen) darf nicht mit
`git checkout -- <datei>` zurueckgenommen werden, wenn die Datei uncommittete Arbeit enthaelt: das
setzt auf HEAD zurueck und die Arbeit ist weg. Am 2026-09-24 hat genau das den frischen Forge-Fix
in `ForgeGameplayEvents.java` geloescht; gerettet nur, weil vorher eine Kopie nach /tmp lag.

**Why:** `git checkout` kennt nur den committeten Stand. `tools/testrunner/mutations.py` stellt
seine Dateien ebenfalls per `git checkout` wieder her und verlangt deshalb einen sauberen Baum fuer
die beruehrten Dateien.

**How to apply:** Vor einer Hand-Gegenprobe die Datei kopieren und aus der Kopie zuruecklegen. Vor
jedem `mutations.py --run` (auch `--p9`) erst committen. Siehe [[gametest-harness-fallen]].
