# Plan b-modicons (21.10.2026)

Queue „(B, b-modicons, 21.10.)“, Besitzer 21.10.: „die ganzen simple mods haben die meißten kein image. bitte immer ein
16x16 als image nehmen also pixelart.“ (Bild 14: Mod-Liste mit Fragezeichen). Prinzip `docs/ai/PRINZIPIEN.md` A8 (c)+(b),
Audit-Punkt ST-10 in `docs/ai/AUDIT-STIL-MODULE-2026-10-17.md`.

## Ist-Zustand

- 44 Einträge in `modules/modules.json` (Root SimpleBuilding, 42 Module, Beispiel `wiringexample`); vier Super-Mods
  (Quality of Life, Aesthetics, Optimization) sind normale Module. `simpleopt*` nur Fabric.
- Icons vorhanden: SimpleBuilding (Kelle, 1080×1080 hochskaliert, Raster 67,5 px – kein exaktes 16×16), Simple Money
  (800×800, 50 px/Pixel), Simple Riding (1080×1080), Simple Visuals (16×16). Nur Fabric trägt `icon`; Module ohne
  `iconFile`/`logoFile`. Root NeoForge nutzt `iconFile`, Root Forge `logoFile`.
- Loader-Verhalten (Quellen auf sb-test gelesen):
  - Fabric/Mod Menu: 16×16 wird scharf gezeigt (Bild 14, Simple Visuals).
  - NeoForge 26.3.0.16: Mod-Liste nimmt `iconFile`; `iconBlur` Standard **false** → 16×16 scharf. `logoFile` ist
    veraltet (Warnbildschirm, Kommentar im Root-`neoforge.mods.toml`) und dient nur noch als Banner → nicht setzen.
  - Forge 26.3-66.0.8: nur `logoFile`, wird in ein Rechteck 50 px hoch eingepasst (`blitInscribed`); der
    `logoBlur`-Upload ist auskommentiert, Filterung also nicht steuerbar. Ein 16×16 würde ~3,1-fach gestreckt und
    bei linearer Filterung weich. → Forge bekommt eine **8-fach Nearest-Neighbor-Variante 128×128** (`icon_128.png`),
    aus derselben Ebenen-Quelle abgeleitet (verkleinern bleibt scharf, vergrößern nicht), dazu `logoBlur=false`
    (dokumentierter Schlüssel, schadet nicht).

## Umsetzung

1. `tools/textures/mod_icons.py`: je Mod ein Motiv als ASCII-Zeichnung mit Palette; Zeichen werden benannten Ebenen
   zugeordnet (z. B. Kontur, Grundform, Schattierung, Glanz, Details). Schreibt `assets/<modid>/icon.layers.json`,
   `icon.png` (16×16, Zusammensetzung laut `layers.py`) und `icon_128.png`. `--check` prüft, dass die Dateien dem
   Generator entsprechen. Stil: Vanilla-nah (dunkle Kontur, Licht von oben links), eigene Pixel, 1 px Rand frei (AGENTS 7),
   transparenter Hintergrund (liest sich auf hellem und dunklem Listen-Hintergrund).
2. Bestehende Besitzer-Bilder (SimpleBuilding-Kelle, Money, Riding, Visuals) respektieren (3.7): auf exaktes 16×16
   zurückgerechnet (Zellmitten abtasten) und als Ebenen-Quelle übernommen – inhaltlich unverändert.
3. Ablage: Module `modules/<id>/shared/resources/assets/<modid>/` (landet in allen Loader-Jars), Root
   `src/main/resources/assets/simplebuilding/`. Einträge: Fabric `"icon"`, NeoForge `iconFile`, Forge
   `logoFile=…icon_128.png` + `logoBlur=false`. Nur diese Zeilen in fremd-berührten Deskriptoren. Linie 1.21.11 bleibt
   (Ports erst auf Anforderung).
4. `tools/check_mod_icons.py` in Gradle-`check` (Map der Text-Prüfungen in `build.gradle`): jedes Modul aus
   `modules.json` hat je Loader den Eintrag, Datei vorhanden, `icon.png` 16×16 mit `icon.layers.json`, `icon_128.png` ==
   8-fach-Vergrößerung. Unit-Test `tools/tests/test_check_mod_icons.py`.
5. Abnahme-Seite (Scratchpad `ic21.html`, nicht veröffentlicht): Raster aller Icons (16× und 4×), Mock Mod-Liste
   hell/dunkel, je Icon „ok / neu“, Auswahl-Code `IC21` mit Liste der „neu“-Icons, Kopier-Knopf.
6. Wiki: Icons in der Mod-Übersicht, falls `wiki/modules.py` das einfach erlaubt; sonst offen.

## Risiken

- Merge mit parallelen Agenten (b-wither21, b-ui21) in Deskriptoren: nur Icon-Zeilen → kleine Konflikte.
- Forge-Darstellung ohne Client-Lauf nicht sichtbar prüfbar (keine Client-Läufe erlaubt) → offen für Abnahme.

## Verifikation

`python3 tools/check_mod_icons.py`, `tools/textures/layers.py check`, `mod_icons.py --check`, Unit-Tests; auf sb-test
Compile aller Loader, `data_integrity`, Jar eines Moduls auf `icon.png`/`icon_128.png` prüfen, Guides/Wiki/Discover
erzeugen (-gen), `./gradlew check -q`.
