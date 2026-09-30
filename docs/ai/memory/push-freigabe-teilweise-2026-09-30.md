---
name: push-freigabe-teilweise-2026-09-30
description: Besitzer erlaubt Teil-Pushes auf master nach gruenem Gate; der Berechtigungsfilter blockt Pushes ohne ausdrueckliche Chat-Freigabe
metadata:
  node_type: memory
  type: feedback
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-30T18:26:35.785Z
---

Nach gruenem Gate (`python tools/ai/aitool.py gate --integration`, "VERDICT: GREEN" gelesen) wird genau der gegatete Commit gepusht: `git push origin <sha>:master`, nie ein wandernder HEAD. Der Besitzer will Teilstaende sofort auf GitHub haben ("push schonmal einen Teil auf master", 2026-09-30).

**Why:** Der erste Push-Versuch am 2026-09-30 wurde vom Berechtigungsfilter abgelehnt ("Blind Apply"), weil im Chat keine ausdrueckliche Freigabe stand. Nach der Freigabe des Besitzers liefen alle weiteren Pushes nach gruenem Gate durch. Ungegatete Commits (z. B. ein roter Merge) bleiben lokal oder auf einem Nebenzweig.
**How to apply:** Gate lesen, dann nur die gategte SHA pushen. Bei Ablehnung nicht auf anderem Weg versuchen, sondern den Besitzer fragen. Rote Merges vom master nehmen (Nebenzweig `merge-<name>`, `git reset --hard <gruene SHA>` lokal) statt sie mitzuschleppen. Siehe [[push-nur-nach-gruenem-ergebnis]].
