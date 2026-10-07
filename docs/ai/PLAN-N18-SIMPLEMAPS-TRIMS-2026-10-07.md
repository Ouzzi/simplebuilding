# Nachtrag 18 – Plan (2026-10-07, Besitzer)

Status: **nur geplant**, nichts umgesetzt. Grundlagen: KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md, PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md, UI-Stil refs-n12 Bild 3/4 (Rahmenmaße PLAN-CRUCIBLE-N12B).

## Grundsatz Konsistenz (verbindlich, alle Simple-Mods)
- Was mehrere Mods betrifft, gehört in **simplelib**: UI-Bausteine (Rahmen, Slots, Lesezeichen-Tabs, **Kontextmenü**, Knöpfe, Tooltips), In-World-Umwandlung, Stapelgrößen-Logik, Config-Gerüst („Enable Simple XY“, Kreativtab-Schalter), Kreativtab-Einsortierung.
- Neue UI-Konzepte werden als wiederverwendbare Bausteine mit Doku festgehalten (docs/ai/UI-BAUSTEINE.md, anzulegen): Rahmen, Slot, Lesezeichen-Tab, Kontextmenü, Bestätigung durch zweiten Klick (rot), Ausgrauen.

## A. Simple Trims (Sub-Mod von SimpleBuilding)
- Enthält alle Schmiedevorlagen (Templates) inkl. Platzierbarkeit wie heute.
- Werkzeug für die Vorlagen-Interaktion: mit SimpleBuilding der Vorschlaghammer, ohne SimpleBuilding die Axt (Prinzip „Vanilla-Fallback Axt“).
- Standalone-Target, SB requires Simple Trims (Super-/Sub-Konzept).
- Risiko: Umzug vieler Registrierungen → IDs bleiben `simplebuilding:*` oder Migration nötig (Frage F1).

## B. Simple Maps (Sub-Mod von SimpleBuilding) – Wegfinder-Karte
**Item „Wayfinder Map“**
- Unendliche Karte, folgt dem Spieler: Spieler immer in der Mitte, Terrain verschiebt sich.
- Aufdeck-Radius wie Vanilla-Karte, per Config einstellbar (harte Grenzen).
- Erweitern: normale (gefüllte) Karte + Wegfinder-Karte → Bereich der normalen Karte wird übernommen (freigeschaltet).
- Kopieren (wie Vanilla-Karten mit leerer Karte) und Kombinieren (zwei Wegfinder-Karten → Vereinigung der Bereiche), Kombinieren kostet etwas (Trade-off).
- Preis: relativ hoch, erreichbar im (frühen) Midgame; im Early Game nur mit gezielter Suche (Vorschlag in F3).
- In Haupt-/Nebenhand: funktioniert wie Karte, zusätzlich erscheinen Wegpunkte in der Locator-Bar neben den Spieler-Indikatoren.

**GUI (Rechtsklick)** – Größe etwa Doppeltruhe + Inventar, UI-Stil Bild 3/4
- Kartenfläche breiter als hoch (≈ 4:3), so bemessen, dass das Raster immer ganz passt.
- Schrittweise zoombar (Mausrad bzw. +/−), verschiebbar per Ziehen.
- **Lesezeichen links:** Snap an/aus (beim Verschieben am Raster einrasten), Raster an/aus, Zoom-Stufe.
- **Lesezeichen rechts:** „Spieler“ (oben) + Wegpunkt 1–8. Klick zentriert. Bei Snap an zentriert „Spieler“ das Rasterfeld/Chunk des Spielers, nicht den Spieler selbst.
- **Kontextmenü (Rechtsklick auf Karte):** „Wegpunkt erstellen“ → alles ausgegraut außer den 8 Wegpunkt-Lesezeichen; freies anklicken = sofort gespeichert; belegtes: 1. Klick färbt rot, 2. Klick ersetzt.
- **Kontextmenü (Rechtsklick auf Wegpunkt-Lesezeichen):** „Wegpunkt konfigurieren“ (Name; Farbe für die Locator-Bar ODER Mob-Kopf als Icon), „Wegpunkt löschen“.
- Daten: Freigeschaltete Bereiche und Wegpunkte gehören zur Karte (Item-Component bzw. Kartendaten), nicht zum Spieler (Frage F2).

**Weitere Feature-Vorschläge (zur Auswahl)**
1. Wegpunkt teilen: Karte kopieren überträgt Wegpunkte (optional ohne).
2. Todespunkt automatisch als temporärer Wegpunkt (verfällt nach Abholung).
3. Biom-/Struktur-Markierungen, die beim Erkunden automatisch erscheinen (Dörfer, Portale) – abschaltbar.
4. Kartenrahmen-Anzeige im Item-Frame: zeigt den Bereich um den Rahmen.
5. Höhenschattierung / Höhenlinien-Modus als drittes Lesezeichen.
6. Dimensionen getrennt: je Dimension eigene Bereiche, Umschalt-Lesezeichen.
7. Kompass-Fusion: Wegfinder-Karte + Bergungskompass zeigt den letzten Todespunkt.
8. Server-Config: Kombinieren/Kopieren abschaltbar, maximaler Bereich (harte Grenzen).

**Fragebogen Simple Maps**
- F2: Wegpunkte pro Karte (wandern mit der Karte, teilbar) oder pro Spieler? (Empfehlung: pro Karte)
- F3: Rezept-Vorschlag: 8 Karten + Kompass um Bergungskompass-Fragment? Oder Karte + Kompass + 4 Gold + 1 Echo-Splitter? Early-Game-Weg: selten in Kartografen-Truhen/Schiffswracks?
- F4: Kosten fürs Kombinieren: z. B. 1 Echo-Splitter / Erfahrungsstufen am Kartentisch? Oder Material-Kosten?
- F5: Wo wird erweitert/kombiniert/kopiert: Kartentisch (vanilla-nah) oder Werkbank?
- F6: Unendlich = wirklich unbegrenzt oder mit Config-Maximum (Speicher pro Karte)?
- F7: Wegpunkte auch ohne Karte in der Hand in der Locator-Bar (z. B. im Inventar)? (Besitzer sagte: Haupt-/Nebenhand)
- F8: Mehrere Dimensionen auf einer Karte (Vorschlag 6) oder eine Karte je Dimension?
- F9: Zoom-Stufen: wie Vanilla 1:1 bis 1:16, oder feiner?
- F10: Mob-Kopf-Icons: alle Vanilla-Köpfe frei wählbar oder nur solche, die man besitzt?

## C. Kleinere Punkte
- C1 Config (Super- und Sub-Mods): Schalter „Items in Kreativ-Tabs hinzufügen“ (an/aus) je Mod – in simplelib als Teil des Config-Gerüsts.
- C2 Guides: farbiger Strich an freigeschalteten Lesezeichen-Tabs entfernen.
- C3 Dev-Kreativtabs immer ganz ans Ende der Tab-Reihenfolge (Production und Dev bleiben nah beieinander).
- C4 Sandwiches appetitlicher (Texturen: sattere Zutatenfarben, mehr Kontrast/Glanz, dickere Füllung – Vorschau mit Varianten).
- C5 Maximale Stapelgröße aller 0,125er-Blöcke auf 128 (über simplelib-Stapelgrößen-Logik, gemeinsam mit Tiegel-Stapelgröße aus Nachtrag 15).

## Fragen an den Besitzer (übergreifend)
- F1: Simple Trims: sollen Item-IDs `simplebuilding:*` bleiben (keine Welt-Migration) oder auf `simpletrims:*` wechseln (mit Migration alter Welten)?

## Antworten Besitzer (2026-10-07)
- F1: Vorlagen-IDs in simpletrims (`simpletrims:*`, mit Migration alter Welten).
- F2: Wegpunkte gehören immer der Karte, die gerade genutzt wird.
- F3: offen – Besitzer möchte Rezept-Vorschläge.
- F4: Normale Karte + Wegfinder-Karte → nur die normale Karte wird verbraucht. Zwei Wegfinder-Karten → die zweite wird verbraucht, Wegpunkte der ersten bleiben immer erhalten.
- F5: Kartentisch.
- F6: Unbegrenzt, aber optimiert gespeichert (nur erkundete Bereiche, z. B. regionsweise/komprimiert); aufgedeckt wird, wo man hinreist; Navigation per Ziehen und Zoom.
- F7: Wegpunkte in der Locator-Bar nur mit Karte in Haupt- oder Nebenhand.
- F8: Je Dimension eine eigene Karte; Nether-Karte deutlich teurer, End-Karte noch teurer. Kompatibilität für weitere Dimensionen vorsehen (Erweiterung in dieser Mod oder per Config/Datapack anderer Mods).
- F9: Zoom: größte Stufe 1 Block = 1 Kartenpixel, dann 4, 8, 16, 64 … Blöcke je Pixel (zunächst zum Testen, Faktor ×2/×4).
- F10: Jeder Mob-Kopf wählbar.
- Feature 5 (Höhenlinien-Modus): ja.
- Feature 7 neu gefasst: Bergungskompass + Wegfinder-Karte gleichzeitig in den Händen → Todespunkt auf der Karte sichtbar, auch in unerkundetem Gebiet (Terrain bleibt dort verborgen, nur der Punkt erscheint).
