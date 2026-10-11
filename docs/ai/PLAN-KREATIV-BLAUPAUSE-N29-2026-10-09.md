# Plan: Kreativ-Blaupause, Kreativ-Baustab, Hilfe-Icons (Queue Nachtrag 29)

Stand 2026-10-09 · Branch `claude-q-creative` (von `claude-wave1` 9c7929ff4) · 26.3, Fabric zuerst, NeoForge/Forge im selben Zug.

## Ist-Zustand: alle Reichweiten-Grenzen
| Grenze | Wo | Wert |
|---|---|---|
| Ziel-Reichweite Bauen (Stab-Klick, Blaupause bauen) | Vanilla `useOn` nur bei `client.hitResult` = Block (Attribut `block_interaction_range`, 4,5) | 4,5 Bloecke |
| Vorschau-Reichweite (Geister) | `BuildingWandPreviewRenderer` nutzt `client.hitResult` | 4,5 Bloecke |
| Groessen-Stufe Blaupause | `BlueprintTiers.edgeFor` (Kante 16 … 256) | Enderit = 256 = ganzes Raster |
| Bearbeiten abgelegte Blaupause | `ModMessageHandlers.PLACED_BLUEPRINT_EDIT_RANGE` | 8 Bloecke |
| Stab-Flaeche/Linie/Bruecke | Radius aus `maxDiameter` (Enderit 13) | bleibt (Form, keine Reichweite) |
| Geister-Zahl | `BlueprintBuilder.MAX_PREVIEW` 4096 | bleibt (Leistung, keine Reichweite) |

Signierte Blaupausen sind schon schreibgeschuetzt (Editor `readOnly`, Server `editedBlueprint` -> `null`); die
einzige Tuer zurueck ist die **Kopie am Kartentisch** (signiert oben + leer unten -> unsignierte Kopie).

## Entscheidungen (autonom, im Bericht genannt)
- **Unbegrenzte Reichweite** = Ziel per eigenem Strahl bis `CreativeReach.FAR_REACH` = 1024 Bloecke (mehr als jede
  Sichtweite, 32 Chunks = 512), sobald die Vanilla-Reichweite nichts trifft. Gilt fuer Kreativ-Baustab in der
  Haupthand **oder** Baustab + Kreativ-Blaupause in der Nebenhand. Klick: Vanilla schickt bei Luft `use`; der Stab
  wirft dort (Client und Server, je mit eigener Blickrichtung) den Strahl und ruft `useOn` mit dem Treffer.
  Vorschau: derselbe Strahl im Renderer. Kein Mixin, keine Attribut-Grenze (Vanilla deckelt das Attribut bei 64).
- **Kreativ-Baustab**: wie Enderit (Radius 13, Kante 256, alle Baustab-Verzauberungen per Tag), **ohne
  Haltbarkeit und ohne Materialverbrauch** – er baut wie im Kreativmodus (`BuildingWandItem.freeBuild`), auch in
  der Blaupause und der Oktant-Fuellung. Begruendung: Kreativ-Item ohne Rezept; Material verbrauchen und
  zerbrechen waere im Kreativmodus ohnehin abgeschaltet; ein Stab, der nur in Kreativ erhaeltlich ist, soll sich
  ueberall gleich verhalten. Im Ueberleben braucht die Flaeche weiterhin einen Block im Inventar als Vorlage.
- **Kreativ-Blaupause**: `BlueprintItem` mit Kreativ-Flag. Alles wie die normale; signiert ist sie endgueltig:
  Editor nur lesend, Server lehnt ab (wie bisher), **Kartentisch-Kopie bleibt signiert** (exakte Kopie statt
  bearbeitbarer). Groessen-Stufe des Stabs gilt weiter.
- Beide nur im Kreativ-Tab (Mod-Tab neben ihren Vorbildern + Suchtab), kein Rezept, Seltenheit EPIC, eigene
  Texturen (Blaupause violett + goldener Stern, Stab mit Quarzschaft und Goldstern-Kopf), Lang EN/DE, Wiki.
- **Hilfe-Icons**: Reiter Blocks/Guide und „Text kopieren“ als 16×16-`IconButton` mit Pixel-Symbol (Wuerfel, Buch,
  zwei Blaetter), Name im Tooltip; der gewaehlte Reiter wird hell hinterlegt. Kopieren sitzt rechts in der
  Reiterzeile, der Anleitungstext reicht bis unten.

## Dateien
`items/custom/CreativeReach.java` (neu), `BuildingWandItem`, `BlueprintItem`, `ModItems`, `BlueprintBuilder`,
`BlueprintCartography`, `BuildingWandPreviewRenderer`, `BlueprintScreen`, `BlueprintEditorLayout` (+Test),
`ModItemGroupsContent`, `SearchTabPlacement`, `DataIntegrityTests` (Tab-Kopie), Datagen (Modelle, Tag),
Lang (beide Baeume), `tools/textures/creative_items_2026_10_09.py`, `wiki/manual.json`, `docs/BLUEPRINT.md`.

## Tests
- GameTests `CreativeItemTests`: Kreativ-Stab baut an einem Ziel 40 Bloecke entfernt (Luftklick, Strahl), normaler
  Stab dort nicht; Kreativ-Blaupause baut fern mit Enderit-Stab; signierte Kreativ-Blaupause: Edit-Payload aendert
  nichts, Kartentisch-Kopie bleibt signiert; Kreativ-Stab ohne Verbrauch/Haltbarkeit im Ueberleben.
- JUnit Layout (Icons in der Reiterzeile, keine Ueberlappung).
- Client: Reiter/Kopieren sind Icons (schmal, ohne Text) mit Tooltip; Screenshots nach `<preview-dir>/creative/`.
