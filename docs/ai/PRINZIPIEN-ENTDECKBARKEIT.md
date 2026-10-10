# Prinzip Entdeckbarkeit (N21, 2026-10-10)

Queue N21: Jedes herstellbare oder umwandelbare Item soll **zuerst intuitiv** sein und **zusätzlich im Spiel gehintet** werden. Nur Konzept; Umsetzung erst nach Besitzer-Entscheidung.

## Regeln
1. **Intuitiv vor Hinweis:** Aussehen, Name und Material verraten die Nutzung (Tiegel sieht aus wie Kessel über Feuer). Ein Hinweis ersetzt nie eine unklare Form.
2. **Jeder Weg hat mindestens zwei Hinweise** aus den Ebenen unten, **einen davon im Spiel-Alltag**, nicht nur im Wiki (Spieler lesen das Wiki nicht).
3. **Hinweise zeigen, nicht erklären:** Beispiel vor Text; kurze Tooltips (max. 2 Zeilen, Config-Umbruch-Regel).
4. **Modulunabhängig:** jedes Modul bringt seine eigenen Hinweise mit (`PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`); Verweise auf fremde Module nur bedingt.
5. **Spoiler-Stufen:** Basisfunktion früh (Tooltip, Rezeptbuch), Tiefes (Upgrade-Ketten, Geheimnisse) erst über Fortschritt/Guide.

## Ebenen (Werkzeugkasten)
| Ebene | Wirkung | Aufwand | Anmerkung |
|---|---|---|---|
| A Tooltip am Item | sagt Nutzung in 1–2 Zeilen, Shift = mehr | gering | Registry-Tooltips existieren in ca. 29 Java-Klassen (Stichprobe `appendHoverText`) |
| B Rezeptbuch/JEI/REI | zeigt Rezept und Umwandlung | mittel | Vanilla-Rezeptbuch schaltet per Rezept-Advancement frei; JEI nur in simplefun, simplemoney, Tiegel |
| C Advancements | Meilensteine, schalten Rezepte frei, geben „Was nun?“ | mittel | Module haben je 0–7 Fortschritte |
| D Guide-Buch / Handbuch | vollständige Erklärung, Kapitel je Modul | hoch | SimpleBuilding-Mega-Handbuch; Modulbücher geplant (`PLAN-MODUL-GUIDES-2026-10-05.md`) |
| E Weltelemente (Vorführung) | Struktur zeigt die Nutzung: Dorf-Lagerfeuer mit Tiegel, Werkstätten mit Amboss/Hammer, Bilderrahmen mit Rezept | hoch | größter Mehrwert, braucht Strukturen/Datapack-Jigsaw |
| F Loot-Hinweise | Bücher/Zettel in Struktur-Truhen, Gemälde mit Rezept | mittel | gut für Geheimnisse |
| G Bedien-Hinweise | Wackeln, Partikel, Aktionsleistentext („Schleichen + Rechtsklick“) | gering | z. B. Hammer-Wackeln (N24) |
| H Fortschritts-Hinweis | erster Kontakt (Item aufgenommen) → Toast/Chat-Tipp einmalig | gering | `Toast` pro Item, einmalig, abschaltbar |

## Rezept pro Item-Gruppe (Vorschlag)
- **Alle herstellbaren Items:** A (Tooltip) + B (Rezept sichtbar) Pflicht. Prüfbar als Test (Registry-Lauf: Item ohne Tooltip-Key oder ohne Rezept-Advancement → Warnung).
- **Maschinen/Blöcke (Tiegel, Trichter, Crafter):** zusätzlich E (Vorführung im Dorf/Struktur) oder H (Toast beim ersten Platzieren).
- **Umwandlungen (Hammer-Teilen, Shulker→Shellker, Sackeln):** G (Hinweis in der Aktionsleiste) + D.
- **Geheimnisse (Boss-Drops, Biom-Pinsel):** F (Buch in Truhe) + C (Fortschritt mit Hinweis), bewusst kein JEI-Spoiler vor Fortschritt.

## Inventar der aktuellen Lücken (Stichproben, vor Umsetzung per Registry-Test genau zählen)
- Crafting/Umwandlungen sind **nirgends im Spiel gehintet** (Queue N21): keine Welt-Vorführung, keine Struktur-Bücher.
- Advancements: simplecontainers, simplemaps, simplesounds, simplevisuals, simplelib **0**; übrige Module je 1–7; unklar, wie viele Rezept-Advancements Rezepte freischalten.
- JEI/REI: Plugin nur in simplefun (Rezepte), simplemoney (Links) und Tiegel (JEI/Jade); Tiegel-Bauweise ohne Rezept-Ansicht, Hammer-/Amboss-Umwandlungen nicht in JEI.
- Tooltips: vorhanden an vielen Items; Lücke bei Umwandlungs-Items (Hammer-Modi), Trichter-Filter, Blaupausen-Rechtsklick (erst per Hilfe-Buch).
- Guide: Modul-Bücher sind geplant (Schritt 3/4), noch nicht alle gemergt; FTB-Startquest ebenso.
- Geheimnisse (Nihil-Gewölbe, Schallplatten-Tracks) haben keine Spur in der Oberwelt.

## Umsetzungsvorschlag (in dieser Reihenfolge)
1. **Registry-Prüfer** (Testlauf): listet herstellbare Items ohne Tooltip, ohne Rezept-Advancement, ohne Wiki-/Guide-Eintrag. Gibt die exakte Lückenliste.
2. **Tooltips + Rezept-Advancements** für alle Lücken (Shift-Tooltip, Config zum Abschalten).
3. **Toast beim ersten Aufnehmen** für Maschinen/Umwandlungs-Items (einmal pro Welt, Config).
4. **Dorf-Vorführung Tiegel:** Datapack-Jigsaw-Haus mit erloschenem Lagerfeuer + Tiegel (Beispiel für E), danach Werkstatt-Varianten.
5. **Struktur-Bücher:** Loot-Tabelle mit „Notiz“-Buch (Rezept eines Geheimnis-Items), Bilderrahmen mit Item in Strukturen.
6. **Guide-Seiten** je Item-Gruppe als Pflicht ergänzen (`tools/guide_book_pages.py` 0 Probleme).

## Offene Besitzer-Fragen (Empfehlung zuerst)
1. Toast beim ersten Item-Kontakt ok (Empfehlung, abschaltbar) oder lieber nur Tooltips?
2. Dorf-Vorführung Tiegel als erstes Beispiel (Empfehlung) – welche weiteren Maschinen zuerst?
3. Geheimnisse (Biom-Pinsel, Shellker) bewusst ohne JEI-Rezept bis zum Fortschritt (Empfehlung) oder sofort sichtbar?
4. Registry-Prüfer als verpflichtendes Gate (Empfehlung: Warnung zuerst, später Pflicht)?
