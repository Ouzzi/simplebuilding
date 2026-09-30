---
name: push-nur-nach-gruenem-ergebnis
description: git push nie im selben Befehl nach einem Testlauf ohne Pruefung des Ergebnisses; run.py endet auch bei roten Tests mit Exit 0
metadata:
  node_type: memory
  type: feedback
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-29T02:33:29.278Z
---

Push erst, wenn das Test-/Check-Ergebnis gelesen und gruen ist; im Befehl nur mit Bedingung (`if [ $c -eq 0 ]; then git push; fi` auf den Gradle-Exit bzw. nach Lesen der "alles gruen"-Zeile).

**Why:** Am 2026-09-29 habe ich `run.py ... | grep ...; git push` verkettet und damit einen Stand mit 7 roten Tests gepusht - run.py beendet sich auch bei roten Tests mit Exit 0, und der Push hing nicht am Ergebnis.
**How to apply:** Testlauf und Push in getrennten Schritten; bei run.py die Zeile "alles gruen" pruefen. Siehe [[testlaeufe-sparsam]].
