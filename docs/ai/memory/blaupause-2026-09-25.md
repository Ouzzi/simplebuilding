---
name: blaupause-2026-09-25
description: "Blaupause (Bau-Code, Editor, Kartentisch-Scan, Baumodus) - Aufbau, Entscheidungen, Fallen"
metadata:
  node_type: memory
  type: project
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-25T00:13:36.711Z
---

Branch worktree-agent-a6358c289c46d09da (5 Commits ab 53de6aa), Spezifikation docs/BLUEPRINT.md.

- Code in `blueprint/` (beide Linien): BlueprintCode (Parser+Serialisierer+Einfaerbung, Raster 0..127,
  32000 Zeichen), BlueprintBuilder (Baumodus + Geistervorschau, in BuildingWandPreviewRenderer eingehaengt),
  BlueprintScanner/BlueprintCartography (+ CartographyTableMenuMixin: Slot-Huellen, setupResultSlot).
- 3D in GUI ohne PIP-Renderer: CPU-Projektion, ein GuiElementRenderState ueber Accessor
  (26.2 GuiGraphicsExtractor.guiRenderState/addGuiElement, 1.21.11 GuiGraphics/submitGuiElement).
- MultilineTextField.StringView ist protected -> nicht benutzbar; Editor bricht Zeilen selbst mit
  font.getSplitter().splitLines um, Auswahlanker per MultilineTextFieldAccessor.
- Besitzer-Entscheid Runde 2: Kanten 16/32/48/64/128/256, Raster 0..255, max. 4 194 304 belegte Stellen;
  Scan folgt der Oktant-Figur (util/OctantShape, auch vom BlockHighlightRenderer benutzt); Scan (262 144 Stellen/Tick,
  via broadcastChanges-Override im CartographyTableMenuMixin) und Bau (4096 Bloecke/Tick, via Baustab-inventoryTick)
  laufen als Auftraege ueber mehrere Ticks.
- Falle: statische Test-Budgets rennen gegeneinander (Spieltests laufen parallel) -> Test-Haken per Spieler-Tag
  (BlueprintScanner.SMALL_BUDGET_TAG). Mock-Spieler werden vom Server echt getickt.
- Falle: GameTestSequence.thenExecute-Fehler stoppen die Sequenz nicht; gemeldet wird evtl. eine spaetere Meldung.
- Falle: Python-Heredocs im Bash-Tool verwandeln "\\n"/"\\r" in echte Zeilenumbrueche - Skripte mit dem
  Write-Tool als Datei anlegen.
- Falle: Mutation `while (false)` kompiliert nicht (unreachable) -> `while (x < 0 && ...)`.
- Runde 3 (Besitzer): keine Beispiel-Automatik; Knopf "Beispiel einfuegen" bei leerem Code (Biom der Spielerposition,
  16 Vorlagen in data/simplebuilding/blueprint_examples/*.sbp, per Klassenpfad geladen); Bauen nur signiert;
  Kopie am Kartentisch (signiert oben + leer unten); Autospeichern 1,5 s entprellt + onClose + removed().
- Falle: Python-Heredocs mit Dreifach-Anfuehrungszeichen im Bash-Tool brechen ab - Skripte per Write-Tool.
- Falle: Testnamen mit Einzelbuchstaben-Wort (CopiesASigned) ergeben haessliche Ids (copies_asigned).
- Runde 3 G: Bau waechst sichtbar (Schicht, Mitte nach aussen, 1-9 s via BlueprintBuilder.ticksFor), rote Fehlstellen,
  Zwei-Klick-Regel (WARNED je Spieler, 60 Ticks), Abbruch auch ausserhalb der Haupthand (tick(..., inMainHand)).
  Spieltests: build() = Klick + completeJob, click() = nur erste Scheibe.
- Falle: der Mock-Spieler aus makeMockServerPlayerInLevel tickt sein Inventar NICHT selbst - Staffeltests treiben
  inventoryTick je Tick von Hand.
- 2026-09-27 Formen + Variablen (docs/BLUEPRINT.md 1.8, BlueprintCode beide Linien identisch): box/sphere/dome/cylinder/
  pyramid/line, Zahlenform (Mitte+Radius / Ecke+Groesse) oder Boxform = OctantShape.predicate (Ausrichtung UP);
  hollow_ = 6-Nachbar-Huelle; $name = Ausdruck (+ - * / % abgerundet, Klammern, 6 Ziffern, Tiefe 32). Budgets:
  MAX_SHAPE_VOLUME (Summe Box-Volumen <= 256^3, Fehler shape_volume) + MAX_EXPANDED_CELLS wie bisher.
  Falle: in Regionen leitet das erste '*' ausserhalb von Klammern die Wiederholung ein -> Multiplikation in Klammern.
  Rueckwaertsvertraeglichkeit per Fingerabdruck-Test (Werte mit dem ALTEN Parser aufgenommen) - bei Aenderungen
  am Parser nie die Erwartung neu aufnehmen, ohne zu verstehen, warum sie abweicht.
- Falle: `sed -i` im Bash-Tool entfernt CR aus CRLF-Dateien -> danach per Python auf CRLF zurueck (oder Edit-Tool).
