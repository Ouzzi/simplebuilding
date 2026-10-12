# Plan UI-Konzepte in echte UIs (Queue N16 „UI-Konzepte“ + N43, Branch `claude-ui-concepts`, 2026-10-12)

Stil streng nach `refs-n12` Bild 3/4, Rahmen aus PLAN-CRUCIBLE-N12B (FRAME 5, unten 7 mit 2 px Schatten, Ecken-Treppe
2 px, Titel links bei x 8 / y 6 wie Vanilla-Container), Bausteine aus SimpleLib (`docs/ai/UI-BAUSTEINE.md`), Vorgehen
wie PLAN-SIMPLECONTAINERS-2026-10-08 (dort Welle W2 = diese Arbeit). Tiegel-GUI nicht angefasst (anderer Agent).

## Inventur (Stand claude-wave1 eb1818c6e)
| Modul | UI | Stil vorher | Konzept / Umsetzung |
|---|---|---|---|
| simplelib | CrucibleScreen | N12 (Referenz) | unverändert (anderer Agent) |
| simpleinterfaces | alle Vanilla-Container (22 Stile) | Container-Stil | fertig (W1 G1–G3) |
| SB | TieredChest, Rucksack, Auto-Schmied, Autonomer Crafter, Lager-Werkbank, Befiederung, Mod-Trichter, Astral-Verzauberung | Container-Stil | fertig (W1 G4 + Nachträge) |
| riding / QoL | Hufeisen-Panel, LinkedPanel | Container-Stil | fertig (G4) |
| simplemaps | WayfinderScreen | Container-Stil (UiBookmarks, Kontextmenü) | fertig |
| simplequestionary | QuestionaryScreen | Container-Stil | fertig |
| SB | **OctantScreen** | Vanilla-Popup (nine-slice) | **neu:** Kasten Messing `#A0642E` (Oktant-Rahmen), helle Labels, Zusammenfassung im hellen eingelassenen Feld (dunkle Schrift wie Inventar) |
| SB | **BuildingWandScreen** | lose Knöpfe ohne Fläche, Titel zentriert | **neu:** kompakter Kasten 216x94 in Material-Farbe des Stabs (Kupfer/Eisen/Gold/Diamant/Netherit/Enderit/Kreativ), Titel oben links, Knöpfe 200 breit bei x 8 (Vanilla-Rand), Schließen mittig |
| SB | **TrimReferenceScreen** | Vollbild-Text ohne Fläche, gelbe Überschriften | **neu:** Kasten in Schmiedetisch-Farbe (`StationStyles.SMITHING`), Breite nach längster Zeile, Liste im hellen Feld (geschnitten, Scrollleiste im Feld), Werte dunkelblau, „Fertig“ unten im Rahmen |
| simplemodels | **ModelBrowser** | dunkles Code-Theme (#272B32 + Kopfbalken) | **neu:** Kasten in Amboss-Farbe (`StationStyles.ANVIL`, von dort geöffnet), Titel oben links, Label-Farbe der Palette, untere Knopfreihe 3 px über dem 7-px-Rahmen |
| simplevisuals | **ModelBrowser** (Legacy-CIT) | ohne Fläche | **neu:** ein Amboss-Kasten 220 breit um Suche/Raster/Knöpfe, Titel oben links |
| SB | BlueprintScreen | Papier-Optik | bleibt (Besitzer N23: „Stil ungefähr gleich“) |
| SB | GuideBookScreen | Buch | bleibt (Entscheidung 5 simplecontainers) |
| SB | TrimStatsPanel, HUDs, Tooltips | Vanilla-Sprites | bleibt (HUD-Stil, Entscheidung 1) |
| alle | Config-Screens (Cloth/AutoConfig), Kreativinventar | Vanilla | bleibt (Entscheidung 1) |

## Entscheidungen (Claude)
1. Widgets (Knöpfe, Eingabefelder) bleiben Vanilla – wie in allen schon gestylten Screens; nur Fläche, Rahmen, Titel,
   Felder ändern sich. Abstände: Inhalt 8 px vom Kastenrand (= Vanilla-Slotrand), Titel x 8 / y 6.
2. SB-Screens liegen in `common/src/shared` (auch 26.2) → Zeichnen über `ModScreenStyle.panel/field/label`
   (26.3-Overlay mit SimpleLib, 26.2-Zwilling = `false`/Fallback → alte Optik). Abschaltbar über den Vergleichsschalter
   `UiStyleToggle` wie G4. Geometrie + Farben als reine Zahlen in `ModScreenLayout` (server-prüfbar).
3. simplemodels/simplevisuals bündeln SimpleLib (Prinzip 6a, Muster simpleriding: Fabric `include`, NeoForge `jarJar`,
   Forge `forgeBundleSimplelib`), Modul bleibt allein spielbar.
4. Kein PNG, keine Motive in diesen Kästen (Textflächen, Motive würden mit Text kollidieren).

## Verifikation
- Compile SB 26.3 Fabric/NeoForge/Forge + 26.2-Wurzel, simplemodels/simplevisuals je Loader.
- GameTest `mod_screen_style_game_test_slotless_panels_hold_their_widgets` (Knöpfe/Felder im Kasten, alle Größen).
- Client: `client-fabric-263` (`SIMPLEBUILDING_CLIENT_ONLY=mod-screens`: Oktant, Bauzauberstab, Trim-Referenz),
  `module-simplemodels-client-263`, `module-simplevisuals-client-263`; Vorher/Nachher nach `~/previews/ui-concepts/`.
- `./gradlew test` (JUnit, Layout-Tests).
