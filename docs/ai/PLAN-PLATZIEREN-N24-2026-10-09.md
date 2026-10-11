# Plan: Platzieren N24/N16 (Branch claude-q-place, 2026-10-09)

Queue: N24 Ziegenhorn-Halter, Barren 3D, Trims bis 4 („Plex“), Speer im Spender; N16 senkrechte Stäbe nahtlos,
Hängematte wie Leine.

## Ist-Zustand
- Platzier-System = `PlacedSmallParts` (Häufchen bis `MAX_PARTS` = 4, Kleinteile-Tag `placeable_small`, Eier, Kerzen,
  Seegurken) + `PlacedTemplates` (einzeln: Vorlagen, Blaupause, Oktant, Attractor, Detector; Wand/Decke/Boden).
  Barren liegen schon als flache Platte im Häufchen (Tag), Vorlagen nur einzeln.
- `StandingRodBlock` (claude-rods3, gemergt): Säulen 13/14 px hoch → gestapelt 2–3 px Lücke (N16 offen).
- Hängematte: 2-Klick-Weg merkt den ersten Anker 30 s im Item (Funken), nichts Sichtbares dazwischen.
- Kein Speer-Spender-Verhalten, kein Ziegenhorn-Block, keine „Stachelfalle“ im Code (Begriff = Vorbild aus anderen Mods).

## Entscheidungen (selbst getroffen)
1. **Trims/„Plex“:** Vorlagen (jede `SmithingTemplateItem` + die zwei schlichten Aufwertungsvorlagen) werden Häufchen-Teile:
   die erste liegt weiter einzeln (Hammer-Aufwertung bleibt), Schleich-Klick mit einer weiteren Vorlage/einem Kleinteil
   macht daraus ein Häufchen bis 4. Alle anderen Platzierbaren sind schon bis 4. Funktions-Items (Blaupause, Oktant,
   Attractor, Detector) bleiben einzeln, weil sie im Block arbeiten.
2. **Barren 3D:** neue Teil-Art `INGOT` für Kupfer-, Eisen-, Gold-, Netherit-, Enderitbarren: 3D-Barren (8×4×2 + 6×2×1 px,
   Trapez), Textur aus der Item-Palette (Generator `tools/textures/placed_ingots_2026_10_09.py`, wie die Eier). Nur Barren
   im Häufchen → Barrenstapel: 2 unten nebeneinander, 3./4. quer darüber. Gemischt: jeder Barren auf seinem Platz.
   Fremde Barren ohne Modell bleiben flache Platte.
3. **Ziegenhorn:** Schleichen + Rechtsklick legt das Horn ab (Boden: Öffnung nach oben, Wand: an Halterung, Öffnung
   schräg nach oben). Rechtsklick mit Fackel (normal/Seelen/Kupfer/Redstone) oder Stab (Stock, Knochen, Lohen-, Böen-,
   Diamantstab; Tag `simplebuilding:goat_horn_holdable`) steckt eins hinein, leere Hand nimmt es heraus. Das Item steht
   als 3D-Modell (Vanilla-Fackel bzw. aufgestellter Stab) im Horn, Fackeln leuchten (Licht wie der Fackelblock) und
   flackern. Horn behält Instrument/Komponenten (Block-Entity). Abbau: Horn + Inhalt.
4. **Speer im Spender:** alle Vanilla-Speere + Enderit-Speer: der Spender wirft ihn nicht, sondern stößt zu: Schaden
   (Angriffsschaden des Speers) an allen Lebewesen in den 2 Blöcken vor der Öffnung, leichter Rückstoß, Speer-Klang,
   Partikel; 1 Haltbarkeit je Stoß mit Treffer, zerbricht wie ein Werkzeug. Ohne Ziel nur Klang.
5. **Stäbe nahtlos:** Zustand `up` (Stab darüber) → Modell `standing_<rod>_up` verlängert die Säule mit dem Schaft bis
   16 px, Form ebenso.
6. **Hängematte wie Leine:** der erste Ankerklick hängt die Hängematte wie eine Leine an: Seil aus der Woll-Textur der
   Hängematte durchhängend vom Anker zur Hand (alle Spieler sehen es, Weltrenderer), statt 30-s-Frist löst sie sich bei
   > 10 Blöcken Abstand (Leinen-Reißklang).

## Verifikation
GameTests je Punkt (Häufchen mit Vorlagen, Barrenstapel-Lage/Trefferform, Horn: ablegen/einstecken/Licht/Drop,
Speer-Spender: Schaden + Haltbarkeit, Stäbe `up`-Zustand + Form, Hängematte: Abstandsregel). Compile 3 Loader,
gefilterte Server-Tests 3 Loader, Wiki/Texturen-Checks, Client-Screenshot → <preview-dir>/place/.
