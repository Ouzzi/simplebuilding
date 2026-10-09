# Plan: Wiki-Seite „Texturen“ (Branch claude-wikitex)

Ziel: Seite `?tab=textures` / `#/textures` im Wiki, die jede Textur aller Module automatisch zeigt
(fertig + Textur-Sheet), dazu „Oberflächen (UIs)“. Nichts von Hand gepflegt.

## Umsetzung
- `wiki/textures.py` (neu): sammelt `assets/*/textures/**/*.png` je Modul.
  - SimpleBuilding: `src/main/resources` → `mc26_3/overlay/resources` (Overlay gewinnt je Pfad, wie mergeResources263).
  - Module: `<shared>/resources`, Loader-Ressourcen, `generated` (erster Fund gewinnt, wie wiki/modules.py).
  - Je Eintrag: id, Modul, Art (= erster Ordner unter `textures/`), Maße per PNG-Kopf, Bytes, `.mcmeta`
    (Animation: Frames, Raster, frametime, Reihenfolge), zuletzt geändert (ein `git log`-Durchlauf),
    Name/Icon aus den erzeugten Wiki-Daten (Item/Block mit derselben Textur bzw. gleicher Id → 3D-Render).
  - Bild-Datei: vorhandene byte-gleiche Kopie unter `wiki/assets/textures/` wird wiederverwendet,
    sonst Kopie nach `wiki/assets/textures/sheets/<modul>/<ns>/<pfad>`; verwaiste Kopien werden entfernt.
  - Ausgabe `wiki/data/textures.js` (ein Eintrag je Zeile, konfliktarm). `--check` vergleicht ohne `modified`.
  - `--ui-shots <ordner>`: PNGs → `wiki/assets/ui/` + `wiki/data/ui-shots.js` (beides gitignored).
- `wiki/generate.py`: ruft `textures.sync` nach den Mod-Daten (bei Standardlauf und `--all`), Option `--ui-shots`.
- `wiki/index.html`: Route `textures`, Seitenleiste, Seite mit Filtern/Sortierung/Raster, Lightbox; Daten lazy nachladen.
- `tools/wiki_site.py`: `data/textures.js` und referenzierte Bilder mit veröffentlichen.
- Tests `wiki/tests/test_textures.py`; Doku `wiki/README.md`.

## Verifikation
`python3.12 -m unittest discover -s wiki/tests`, `generate.py --all`, `--all --check`, `tools/wiki_site.py --verify-only`,
Playwright-Screenshots Seite + Lightbox nach `/root/previews/wiki/textures-*.png`.

## Entscheidungen
- Kein Datum im Check (sonst schlägt `--check` nach jedem Commit an); Datum = letzter Commit der Quelldatei.
- UI-Screenshots werden nicht veröffentlicht (`tools/wiki_site.py` lässt sie weg) und nicht eingecheckt.
