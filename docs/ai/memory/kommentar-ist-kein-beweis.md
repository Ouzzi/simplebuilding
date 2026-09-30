---
name: kommentar-ist-kein-beweis
description: Bei Widerspruch zwischen Code und Kommentar ist der Kommentar oft der veraltete Teil
metadata:
  node_type: memory
  type: feedback
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-08T09:42:05.132Z
---

Wenn Code und Kommentar sich widersprechen, ist **nicht automatisch der Code der Fehler**. Oft ist
der Kommentar der veraltete Teil — jemand hat die Formel bewusst geändert und den Text darüber
vergessen.

**Why:** Der Nutzer hat das am 2026-09-08 ausdrücklich angemerkt, nachdem ich mehrere
Kommentar/Code-Abweichungen als Mod-Fehler gemeldet hatte (u. a. den Drawer-Multiplikator
`(16 + level) / 8` gegen den Kommentar `(8 + level) / 8`). Einen absichtlich stärkeren Wert auf den
Kommentar „zurückzureparieren" wäre eine echte Verschlechterung des Spiels — und zwar eine, die wie
eine Fehlerbehebung aussieht.

**How to apply:** Bei jedem Widerspruch erst Belege sammeln, bevor eine Seite als richtig gilt:
Git-Historie der Zeile (`git log -L`), Wiki/Handbuch, Balance-Werte der Nachbarstufen, und ob
irgendein Test oder Datensatz den einen Wert schon voraussetzt. Erst danach entscheiden — und wenn
die Belege für den Code sprechen, **den Kommentar** korrigieren, nicht die Formel. Bleibt es
unklar, ist es eine Design-Frage für den Nutzer, keine Reparatur.

Siehe auch [[offene-mod-befunde-2026-09]] und [[testabdeckung-2026-09]].
