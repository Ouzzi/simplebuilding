# Plan: Blaupausen-UI-Umbau + Rechtsklick auf liegende Blaupause (Queue N23)

Stand 2026-10-09 · Branch `claude-q-blueprint` (von `claude-wave1`) · 26.3, Fabric zuerst, NeoForge/Forge im selben Zug.
Wortlaut: `.claude/QUEUE.md` Nachtrag 23 („Blaupause am Boden“, „Blaupausen-UI Umbau“), Roadmap P1 „3D-Vorschau-Drag kaputt“.

## Ist-Zustand
- `BlueprintScreen` (Kartenblatt, 3 Spalten): Liste mit Icon+Menge+Name, Maße/Blockzahl in **einer** Zeile (überlappen bei
  großen Zahlen), Stab-Icon + Stufenname + Leiste darunter; Zeichenzahl oben rechts im Kopf; Einfüge-Leiste unter dem Code;
  Buch- und Reset-Knopf (Kompass) über der Vorschau; Hilfe mit Reitern Guide|Blocks.
- Drag/Strg-Pan/Reset kamen mit `e2e17854a` (opencode); Besitzer-Bericht ließ sich dort nicht nachstellen.
  Restschwäche: der Drag startet nur, wenn `super.mouseClicked` nichts trifft.
- Liegende Blaupause = `placed_blueprint` (`PlacedTemplateBlock`, Block-Entity hält den Stapel); Rechtsklick tut nichts.
- simplebuilding hängt **nicht** von simplelib ab (Modul-Unabhängigkeit) → Papier-Stil bleibt (Queue: „Stil ungefähr gleich“),
  eigene kleine Pixel-Symbole im selben Stil statt `UiSymbols`.

## Umsetzung
1. **Reine Layout-Klasse** `blueprint/BlueprintEditorLayout` (ohne MC): alle Rechtecke aus Panelgröße + Breite der
   längsten Materials-Zeile; Hilfen `fitScale` (Text auf Breite verkleinern) und `compactCount` (12k/1.2M).
   JUnit `BlueprintEditorLayoutTest`: alles im Panel, keine Überlappung (Spalten, Fußzeilen, Elemente in der Vorschau je Modus).
2. **Screen-Umbau** nach Queue-Text:
   - Kopf: Überschrift (Titel bzw. signierter Titel + Autor).
   - Materials: Liste schmaler, Raster aus Icon + Zahl (Name im Tooltip); darunter „X×Y×Z“ / „= n Blöcke“ je eigene Zeile,
     darunter Stab-Icon + Fortschrittsbalken (Stufe, genutzt/frei im Tooltip).
   - Code: Codeblock; darunter „Code OK“ / „n/32000 Zeichen“; rechtsbündig daneben Buch-Knopf.
   - Preview breiter; Reset-Icon (gezeichneter Kreispfeil) **im** Vorschaufenster oben rechts; darunter Signieren | Fertig.
   - Hilfe-Buch (statt Vorschau): Reiter **Blocks** zuerst + Standard: Textfeld + „Einfügen“ daneben, Trefferliste darunter
     (Klick wählt, Doppelklick/Enter fügt ein); beim Öffnen ist das Textfeld fokussiert. Reiter **Guide**: vollständige,
     leicht verständliche Anleitung (Abschnitte), unten fester „Text kopieren“-Knopf (Zwischenablage).
   - Drag: Klick in die freie Vorschau startet den Drag **vor** der Widget-Verteilung (nur wenn kein Widget unter der Maus).
3. **Liegende Blaupause**: `PlacedTemplateBlock#useWithoutItem` öffnet den Editor, wenn keine andere Aktion greift:
   Haupthand leer und Nebenhand kein Block (sonst PASS → Item/Block der Hand wirkt wie bisher), Feature-Schalter wie beim Item.
   Client öffnet `BlueprintScreen` für den Stapel aus der Block-Entity; Speichern über `BlueprintEditPayload` mit optionaler
   Blockposition (Server prüft Block, Reichweite, `WorldPermissions.mayChange`, Signatur – gleiche Regeln wie im Slot).
4. Lang EN/DE (Guide neu, neue Schlüssel additiv), Wiki (`manual.json` Editor-Absatz + Rechtsklick), `docs/BLUEPRINT.md` §2.
5. Tests: JUnit Layout; GameTests Rechtsklick-Fall (leere Hand öffnet, Block in Hand/Nebenhand nicht) und Payload an Position
   (speichert, signiert, signierte/ferne abgelehnt); Client-Tests angepasst (Insert im Buch fokussiert, Reset im Fenster),
   Screenshots vorher/nachher nach `/root/previews/blueprint/`.

## Risiken
- Reflexions-Feldnamen im View-Client-Test (`viewX`, `viewW`, `bodyY`, `bodyH`, `draggingView`) bleiben erhalten.
- `useWithoutItem` greift auch bei Nebenhand-Items, die keine Blöcke sind (z. B. Fackel = Block → PASS; Schwert → öffnet).
