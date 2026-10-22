# Plan B-FRAMEMAPS22 – Wayfinder-Karte im Rahmen speichern, Rahmenbereich, Codefeld, Stapelgröße 1 (22.10.2026)

Branch `b-framemaps22`, Queue-Zeile 102 (Besitzer 21.10., Simple Maps). Nur `modules/simplemaps` (+ Queue, Plan, Doku).

## Ist-Zustand (erhoben)
- `ItemFrameMixin`: Rechtsklick (ohne Schleichen) auf eine gerahmte Wegfinder-Karte öffnet `WayfinderScreen.forFrame` (Feature 4).
- `WayfinderScreen.removed()` schickt beim Schließen **immer** `FrameViewPayload` → der Rahmen übernimmt still die
  zuletzt gescrollte Ansicht (Besitzer will das Gegenteil). Dabei wird `mag()` vergessen (8:1-Stufen falsch).
- `MapsComponents.View(x, z, zoom)` = Mitte in Blöcken + Blöcke je Pixel (1..64); keine Vergrößerung (8:1/4:1/2:1).
- `ViewTexture` malt 128×128 Pixel um die Mitte; der Rahmen zeigt also 128·zoom Blöcke.
- `MapsNetwork.handleFrameView` prüft Distanz (8 Blöcke), Rahmen, Wegfinder-Karte, Koordinaten – **keine Rechte**
  (Claims, fester Rahmen, Abenteuer, Spawnschutz).
- Bildschirm-Stufen: `8:1, 4:1, 2:1` (Vergrößerung, 1 Block = 8/4/2 Pixel) und `1:1 … 1:64` (Blöcke je Pixel); der
  Zoom-Knopf zeigt genau diese Beschriftung (`ratio()`).
- Stapelgröße: Standard 64. Kartentisch-Kopie liefert `copyWithCount(2)` und verbraucht beide Eingaben.
- Rechte im Projekt: framework `Protection` (Claims von Simple Tweaks: Rahmen brauchen dort das Recht C =
  `Action.CONTAINER`), Vorbild `BiomeBrush.mayEdit` / `HammerProtection` (mayBuild, mayInteract, Protection).

## Festlegung Codefeld (Format `X,Z,Stufe`)
- `X,Z` = **Chunk-Koordinaten der Nordwest-Ecke (oben links)** des Rahmenbildes, wie F3 „Chunk“ sie zeigt.
- `Stufe` = Beschriftung des Zoom-Knopfs: `8:1, 4:1, 2:1, 1:1, 1:2, 1:4, 1:8, 1:16, 1:32, 1:64`. Fehlt sie (`5,6`),
  bleibt die aktuelle Stufe.
- Ein Rahmen ist `8 · Blöcke-je-Pixel / Vergrößerung` Chunks breit und hoch: 8:1 = 1, 4:1 = 2, 2:1 = 4, 1:1 = 8,
  1:2 = 16 … 1:64 = 512. Nachbar rechts (Osten) = `X + Breite`, darunter (Süden) = `Z + Breite`.
  Beispiele: `1,1,8:1` = genau Chunk (1,1); `-1,-100,1:64` = 512×512 Chunks ab Chunk (-1,-100).
- Gespeichert wird weiter die Mitte: `x = X·16 + Breite_in_Blöcken/2` (Breite = 128·zoom/mag). Rückweg
  `X = floorDiv(x − Breite/2, 16)` (alte Ansichten ohne `mag` lesen sich als Stufe zoom, mag 1).
- Fehler (sichtbar im Feld rot + Zeile im Fuß): falsches Format, unbekannte Stufe, außerhalb der Welt
  (|Mitte| > 30 000 000). Leerzeichen sind erlaubt.

## Umsetzung
1. `FrameCode` (shared, ohne Client-Klassen): Stufen-Tabellen (vom Bildschirm mitbenutzt), `parse`, `format`,
   `view(cx,cz,stufe)`, `of(View)`, `spanChunks`. 
2. `MapsComponents.View` + `mag` (optional im Codec, Standard 1; ungültig → 1; zoom ≠ 1 → mag 1).
3. `ViewTexture.compose` mit `mag`; `MapsClient.fillFrameState` reicht `mag` durch.
4. Server: `MapsNetwork.saveFrameView(player, entityId, view)` → `SAVED / DENIED / INVALID`;
   INVALID: aus, kein Rahmen, zu weit, keine Wegfinder-Karte, außerhalb der Welt; DENIED: Zuschauer, fester Rahmen
   (`Fixed`), kein Baurecht (Abenteuer), Spawnschutz (`mayInteract`), framework-`Protection` mit `CONTAINER`
   (Claims: Rahmen = Recht C). Antwort `FrameSavedPayload(entityId, result)` an den Spieler. Sync an alle über die
   Entity-Daten wie bisher.
5. Bildschirm (nur Rahmen-Modus): kein Speichern beim Schließen mehr. Untere Leiste: Codefeld (EditBox, Enter setzt
   die Ansicht, Fehler rot), Kopieren-Knopf (Zwischenablage), Speichern-Knopf zweistufig:
   „Ansicht speichern“ → „Wirklich speichern? (5)“ mit rotem Rand, erst nach 0,5 s klickbar (kein Doppelklick-
   Durchrutschen), ✕-Knopf/Esc bricht ab, Ablauf 5 s, Ändern der Ansicht bricht ab; danach „Speichert …“ bis die
   Antwort kommt, Fuß zeigt „Gespeichert“ / „Nicht erlaubt …“ / „Ungültig“. Knopf aus, wenn der Rahmen die Ansicht
   schon zeigt. Karte: durchgezogener Rahmen = neue Ansicht (Chunk-genau, mittig), gestrichelt = was der Rahmen
   gerade zeigt; Beschriftung „Neu“/„Rahmen“. Layout: `WayfinderLayout.of(w, h, frame)` mit Zusatzleiste.
6. Stapelgröße 1 für alle drei Karten; Kartentisch-Kopie: **eine** Kopie, das Original bleibt liegen, nur die leere
   Karte wird verbraucht (wie Bauplan/Notenblatt) – nötige Folge von Stapelgröße 1 (sonst Stapel 2 > Maximum).
7. Lang EN/DE, `wiki/manual.json` (Rahmen + Kartentisch), Generatoren (Guide, Wiki, Discover) auf sb-test,
   `docs/MODS-UND-FEATURES.md`.

## Risiken
- Alte gespeicherte `View` ohne `mag` → optionales Feld, Standard 1.
- Andere Mods am Kartentisch (SB-Bauplan 1100, Simple Music 1200) wickeln Slots ein und rufen das innere `onTake`
  → unser Ergebnis-Slot (1000) liegt innen und bleibt wirksam.
- Kein Client-Lauf erlaubt: Bildschirm nur per Compile + HTML-Vorschau geprüft.

## Verifikation
- Gametests (shared `MapsTests`, Fabric-Adapter `ModuleGameTest`): `frame_view` (Öffnen, Speichern, Paket-Antwort,
  Sync-Flag), `frame_save_rules` (zu weit, fremde ID, keine Wegfinder-Karte, Welt-Grenze, fest, Abenteuer,
  Zuschauer, Claims über `Protection`), `frame_code` (gültig/ungültig, negativ, Grenzen, alle Stufen, Nachbarn,
  Codec alt/neu), `stack_size` (Stapelgröße 1, Kopie behält Original), `cartography_copy` angepasst,
  `screen_layout` + JUnit `WayfinderLayoutTest` für den Rahmen-Modus.
- Ziele: `module-simplemaps-fabric-263`, `-neoforge-263`, `-standalone-fabric-263`; Compile aller Loader;
  Generatoren + `./gradlew check -q`.
- Kontrast: Texte über `UiContrast.ensure` gegen die Panel-Farbe (≥ 4,5:1), Werte im Bericht.

## Ergebnis (22.10.)
- Umgesetzt wie geplant: `FrameCode`, `View.mag`, `ViewTexture`/`fillFrameState` mit Vergrößerung, `MapsNetwork.saveFrameView`
  + `mayChange` + `FrameSavedPayload` (Fabric/NeoForge/Forge registriert), Rahmen-Leiste im `WayfinderScreen`
  (`WayfinderLayout.of(w, h, true)`, `BAR_H` 22), Stapelgröße 1, Kopie über eigenen Ergebnis-Slot (`Cartography.resultSlot`).
- Abweichungen: Hilfetexte der Knöpfe/des Codefelds heißen `*.help` statt `*.tooltip` (die Breitenregel von
  `tools/check_tooltip_text.py` gilt für Item-Tooltips; wie die vorhandenen `screen.simplemaps.tab.*`). Die Mausposition im
  Fuß wird nur noch gezeichnet, wenn sie nicht in die Zoomzeile läuft (auch im Hand-Modus). Deutsche Rahmen-Zusammenfassung
  gekürzt, damit sie auf eine Handbuchseite passt.
- Kontrast (Panel #6F5F4B hell/dämmrig/dunkel): Beschriftung 5,32/7,81/12,74; Status ok 4,58/6,72/10,96, Fehler 4,59/5,31/8,67,
  Frage 4,83/7,09/11,57 (über `UiContrast.ensure`); Codefeld 15,91, Fehlertext 7,57, Hinweis 9,04 auf Schwarz; Etiketten ≥ 10,5.
- Tests: module-simplemaps fabric/neoforge/standalone-fabric je 34/34 (neu `frame_save_rules`, `frame_code`, `stack_size`;
  `frame_view`, `cartography_copy`, `screen_layout` erweitert), JUnit `WayfinderLayoutTest` (Rahmen-Leiste), Compile aller
  drei Loader, Generatoren, `check`.
- Nicht getestet: Bildschirm im Client (kein Client-Lauf erlaubt; nur Compile + HTML-Vorschau), Forge-Gametests, echte Claims
  von Simple Tweaks (nur über einen Test-Provider der framework-`Protection`).
