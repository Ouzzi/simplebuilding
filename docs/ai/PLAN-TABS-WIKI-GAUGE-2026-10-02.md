# Plan: Vanilla-Tabs, Tab-Aufteilung, Wiki-Grundmaterialien, Messuhr-Autowalk (2026-10-02)

Branch `claude-tabswiki`, nur committen.

## Ist-Zustand
- `SearchTabPlacement` setzt schon jedes Item der Hauptmod (inkl. tweaks) mit `PARENT_AND_SEARCH_TABS` in
  Vanilla-Tabs (Fabric `ModItemGroups`, NeoForge `NeoForgeSearchTabPlacement`, Forge `Forge263Events`).
  Lücken: Mod-Werkzeuge stehen als ein Block hinter der Netherit-Hacke statt je Stufe; Enderit-Axt fehlt im
  Kampf-Tab (Vanilla führt Äxte in Werkzeuge UND Kampf); Redstone-Tab ohne Mod-Platten/Truhen/Öfen/Lampen
  (Vanilla führt Druckplatten, Truhe, Kupfertruhe, Ofen, Redstone-Lampe auch dort).
- Module: simplemoney (Zutaten hinter Papier) und simplefun (Kampf/Gebrauchsblöcke) platzieren schon;
  **simpleriding** (Hufeisen + Hufeisen-Vorlage) nicht. simpledimensions/simpletweaks haben nur Alt-Items,
  wiringexample ist ein Beispiel - bleiben ohne Platzierung.
- Mod-Tabs: tools (Werkzeuge+Waffen+Rüstung+Geräte+Bücher), building_blocks, materials (inkl. Nahrung),
  functional, pads, arrows.
- Wiki: Rezeptkarten zeigen nur direkte Zutaten; Modul-Items haben kein `craftedBy`. Geldschein ohne Wert.
- Messuhr-Autowalk (`client/GaugeAutowalk`): bei jedem offenen Screen wird die Vorwärtstaste losgelassen;
  NeoForge/Forge melden `keyUp.isDown()` bei offenem Screen ohnehin falsch (KeyConflictContext IN_GAME);
  `getMainHandItem() != held` (Identität) bricht nach jedem Container-Sync ab.

## Umsetzung
1. `SearchTabPlacement`: Werkzeuge je Stufe (Meißel/Hammer/Baustab der Stufe hinter der Vanilla-Hacke der
   Stufe; Netherit dann Enderit-Werkzeuge), Enderit-Axt zusätzlich im Kampf hinter der Netherit-Axt,
   Redstone-Zweitplatzierungen (Platten, Truhen, Öfen, Lampen). Neues Record-Feld `secondary` = bewusstes
   zweites Vorkommen in einem weiteren Vanilla-Tab (Suchtab nimmt das erste). Test passt Doppel-Logik an.
2. simpleriding: `Horseshoes.vanillaTabPlacement` - Hufeisen im Kampf vor der Wolfsrüstung (also hinter allen
   Pferderüstungen inkl. Enderit), Vorlage in Zutaten vor der Erfahrungsflasche; Fabric/NeoForge/Forge-Hooks;
   Modultest in RidingTests.
3. Mod-Tabs aufteilen: neu `combat` (Schwerter, Speere, Rüstung, Reittier-Rüstung; Icon Enderit-Schwert) und
   `food` (Nahrung; Icon verz. Enderit-Apfel). Reihenfolge tools, combat, building_blocks, materials, food,
   functional, pads, arrows. Lang EN/DE beidseitig (`itemgroup.*`, Testzentrale `section.tab_*`).
   Tests: DataIntegrity-Tab-Tests (Heimat-Tabs, Pins, Layout-Tests tools/combat/materials/food), TcContext.
4. Wiki `generate.py`: `base_materials()` löst je Rezept rekursiv bis zu Grundmaterialien auf
   (Barren, Stämme/Stängel und Items ohne Rezept: Zuckerrohr, Honigwabe, Bruchstein, Diamant …), über
   Mod-Rezepte + committete Vanilla-Rezepte der Linie (deterministisch, kein Jar nötig). Zyklen über
   Pfad-Erkennung, Rückweg-Rezepte (Block→Barren) verworfen, Mehrfach-Rezepte nach fester Priorität
   (Werkbank > Ofen > Steinsäge > Schmied; dann Id), Ausbeute als Bruch, Tags über einen Vertreter.
   Ergebnis `recipe.baseMaterials` (ignoriert beim Linienvergleich), Anzeige in `recipeCard`.
   Module: `craftedBy/usedIn` + `baseMaterials`; Item-`value` aus den Handels-JSONs des Moduls
   (Geldschein: Smaragde je Schein, belegt durch `villager_trade/*`). Python-Tests.
5. Autowalk: Mixin `KeyboardInputMixin` (TAIL von `KeyboardInput#tick`) setzt Vorwärts, solange
   `GaugeAutowalk.forcesForward()`; offen bleiben nicht pausierende Screens, Pause-Screens lassen los.
   Gauge-Prüfung per `ItemStack.isSameItem` + Slot. Reine Logik in `VelocityGaugeItem` (server-testbar).

## Annahmen
- "Waffen und Rüstung getrennt wie Vanilla" = Vanilla-Tab "Kampf" (Waffen + Rüstung zusammen); Äxte bleiben
  im Mod-Tab bei den Werkzeugen (jedes Mod-Item genau ein Mod-Tab). Verzauberte Bücher bleiben in SimpleTools.
- Grundmaterial-Mengen sind exakte Anteile je Craft (gerundet auf 2 Stellen), keine Batch-Rundung.
- Geldschein-Wert = was Dorfbewohner im Code für einen Schein geben (Smaragd-Spanne) - keine erfundene Zahl.

## Verifikation
- Server-GameTests `fabric-263`, `neoforge-263`, Modul `module-simpleriding-fabric-263`,
  `module-simplemoney-fabric-263`; Compile 26.2 `:compileJava :neoforge:compileJava`, Forge 26.3.
- Wiki: venv `--all`, uv `--all --check`, `python -m unittest` in `wiki/tests`.
- Nicht getestet: echter Client (Autowalk im Inventar nur per Logiktest, kein Client-Start erlaubt).
