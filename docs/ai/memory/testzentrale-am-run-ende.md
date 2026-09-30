---
name: testzentrale-am-run-ende
description: Am Ende eines Runs (Gruppe von Wellen) die Testzentrale neu bauen und durchgehen; der Abdeckungstest faengt fehlende Inhalte automatisch
metadata:
  node_type: memory
  type: feedback
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-25T20:09:36.524Z
---

Am Ende jedes Feature-Runs (einer Gruppe von Wellen, nicht nach jedem Schritt) die Testzentrale oeffnen
bzw. neu bauen (`/sbtestcentre build`, Welt "SB-Testzentrale" baut sich im Dev-Client beim ersten Betreten
selbst) und die neuen Features dort sichtbar pruefen. Fehlende Inhalte muss man nicht suchen: der
Server-Gametest `test_centre_game_test_every_mod_item_and_block_has_its_place_in_the_test_centre` wird rot,
sobald ein Mod-Item keinem Abschnitt zufaellt (es steht dann unter "unsorted"); neue Tab-Zeilen landen von
selbst im Abschnitt "devices". Doku: docs/TESTZENTRALE.md, Code: com.simplebuilding.dev.testcentre
(26.x in common/src/shared, 1.21.11-Spiegel ohne McVersion-Shims).

**Why:** Besitzer-Anweisung 2026-09-25 (Welle 15): reproduzierbare Testwelt aus Code statt der von Hand
gebauten; Pruefen am Run-Ende spart Zeit gegenueber Pruefen nach jedem kleinen Schritt.
**How to apply:** Beim Abschluss eines Runs Zentrale neu bauen, Abschnitte durchgehen; ein neues Feature,
das mehr als Anschauen braucht, bekommt eine eigene Station in TestCentreSections. Siehe [[testlaeufe-sparsam]].
