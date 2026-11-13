# PLAN a-bundlequiver – Enderit-Bündel und Enderit-Köcher neu (13.11.2026)

Auftrag: /root/sb-tools-a/agenten-1530.md §20. Besitzer 13.11. ~00:30 UTC: „A-W1B=ok außer bundle und quivers“
(T12 Enderit-Bündel gefärbt, T13 Enderit-Köcher gefärbt → neue Texturen); schon TEXRATE16 (10.11.) hatte Einwand zu
`enderite_bundle`/`enderite_quiver`. Basis `hub/b-bundle191` 4c7a3078b, Branch `a-bundlequiver`, Ziel Bündel 192.
Nur 26.3 Fabric. Regeln: PRINZIPIEN A8 (a, b), A8b, A8c, A8d; UI/Optik erst exemplarisch (3 Varianten je Item,
beste vorausgewählt eingebaut); keine Mojang-Pixel.

## 1. Ist-Stand

Auf 26.3 liegen Bündel und Köcher in Simple Storage (`McVersion.STORAGE_IN_SB = false`), die Texturen unter
`modules/simplestorage/shared/resources/assets/simplestorage/textures/item/`:

| Datei | Herkunft heute | Problem |
|---|---|---|
| `enderite_bundle.png` | `texrate20_2026_11_10.bundle('A')` | Umriss = Vanilla-Bündel-Silhouette, dumpfes Pflaumenleder, Besitzer-Einwand |
| `enderite_bundle_dyed{,_overlay}.png` | `storage_w1b_2026_11_12.dyed_pair` aus TEXRATE20-A | dunkelstes Leder bleibt violett im Overlay → schmutzig, T12 Einwand |
| `enderite_bundle_open_back/front.png` | **Vanilla-Umfärbung** (`recolour08_2026_11_08.json`, `vanilla_derived.MODULE_OUTPUTS`, `.gitignore`) | Mojang-Pixel (nur beim Build), passt nicht zum neuen Bündel |
| `enderite_bundle_open_{back,front}_dyed{,_overlay}.png` | `generate_textures.bundle_open_textures` (alte Form) | alte Palette |
| `enderite_quiver.png` | `texrate20.quiver('A')` aus `tools/textures/hand/enderite_quiver.png` | Familien-Silhouette, Einwand |
| `enderite_quiver_dyed{,_overlay}.png` | `storage_w1b` aus TEXRATE20-A | Bänder hell/dunkel gemischt gefärbt, T13 Einwand |

Item-Modelle: `items/enderite_bundle.json` (geschlossen; GUI mit ausgewähltem Eintrag: open_back + Gegenstand +
open_front; gefärbt: layer0 = `*_dyed` mit `minecraft:dye`-Tönung, layer1 = `*_dyed_overlay` ungetönt).
Köcher: nur geschlossen/gefärbt (kein Füllzustand am Item; die Füllstufen 0–5 gibt es nur beim abgestellten Köcher).

Nicht im Umfang (begründet):
- `block/placed_enderite_{bundle,quiver}*.png` (32×32-Blatt des abgestellten 3D-Modells, `placed_bundle_textures.py`):
  kein Besitzer-Einwand; „UI/Optik erst exemplarisch“ → nach Abnahme der Item-Form an die neue Palette angleichen (TODO).
- 26.2-Kopien im SimpleBuilding-Namensraum (`src/main/resources/.../simplebuilding/textures/item/enderite_*`,
  `mc26_3/overlay/...simplebuilding...`): nur 26.2/Port-Linie → Port-Run (TODO).

## 2. Plan

1. **Neuer Generator** `tools/textures/bundlequiver_2026_11_13.py` (reines Python, `layers`/`texrate20.Tex` wie
   `storage_w1b`): je Item drei Varianten A/B/C als 16×16-Zeichenkarten mit Ebenen (Leder, Enderit-Beschlag,
   Kordel/Pfeile, Glimmer), `BUILT = {'bundle': X, 'quiver': Y}` = vorausgewählte Variante.
   Ziele (12 PNG + `.layers.json`): `enderite_bundle`, `_dyed`, `_dyed_overlay`, `_open_back`, `_open_front`,
   `_open_back_dyed{,_overlay}`, `_open_front_dyed{,_overlay}`, `enderite_quiver`, `_dyed`, `_dyed_overlay`.
   `--write`, `--check` (Pixel + Ebenen + Färberegeln), `--out DIR`, `--variants DIR` (alle Varianten für die Vorschau).
2. **Färbung über Overlay sauber:** Leder (alle Lederstufen) → neutrales Grau im `*_dyed` (Helligkeit aus der
   Lederrampe, hellster Ton ~240, damit Weiß weiß wirkt), Umriss, Enderit-Beschlag, Kordel, Pfeile, Glimmer →
   `*_dyed_overlay`. Regeln wie `storage_w1b.dyed_problems` (grau, keine Überlappung, Vereinigung = Umriss des
   ungefärbten Items) im `--check`.
3. **Offen-Zustand ohne Mojang-Pixel:** eigene `open_back/open_front` je Variante (Enderit-Rand als Öffnung,
   Vorder-Beutel wie das geschlossene Bündel), Einträge aus `recolour08_2026_11_08.json`,
   `vanilla_derived.MODULE_OUTPUTS['simplestorage']` und `.gitignore` (Modul + Wiki-Blätter) entfernen.
4. **Alte Generatoren entkoppeln:** `texrate20.DESIGNS` ohne die zwei STORAGE-Ziele (CORE = 26.2 bleibt),
   `storage_w1b.DESIGNS` ohne die Enderit-Bündel/-Köcher-Färbeziele (Barrels bleiben),
   `generate_textures` schreibt nur den SimpleBuilding-Baum (unberührt).
5. **A8c-Runden** (mind. 3): erstellen → texrate + Blick 1×/8×/Slot/gefärbt (Weiß, Rot, Blau, Grün, Schwarz) gegen
   Vanilla `bundle`, `white_bundle`, `bundle_open_*` und die Enderit-Familie (Barren, Rucksack, Rüstung) → neu.
   Bewertung je Runde unten.
6. **Vorschau** `/root/sb-tools-a/previews/a-bundlequiver-preview.html` (Bilder inline): vorher/nachher je Textur
   (groß, Spielgröße, im Slot, gefärbt 4 Farben), Varianten A/B/C je Item, ok/Einwand je Textur, Antwortcode
   BUNDLEQUIVER.

## 3. Prüfung

`bundlequiver --check`, `texrate20 --check`, `storage_w1b --check`, `layers.py check`, `texrate.py` (neue Texturen grün),
`check_missing_textures.py`, `modules/simplestorage/tools/check_data.py`, `vanilla_derived.py --list` (Konsistenz),
`recolour08 --verify` falls Jar da, `precheck.py --fast`. sb-test: `timeout 10 ssh sb-test true`; nur bei
Erreichbarkeit EIN gezielter Lauf (DyedStorageTests), sonst sbt-Befehl als `/root/sb-tools-a/a-bundlequiver-sbt.cmd` + TODO.

## 4. Runden (A8c)

Bewertet je Runde in 1×, Slot (18 px) und 6–20× (Raster), gefärbt Weiß/Rot/Blau/Grün/Schwarz (StorageDyes), gegen
Vanilla `bundle`, `white_bundle`, `bundle_open_back/front` (nur Vorschau, aus dem Jar) und die Enderit-Familie
(Barren, Rucksack, Brustpanzer). Renders der Runden: `/root/sb-tools-a/bq/r1.png` … `r5.png`.

| Runde | Stand | Bewertung → nächster Schritt |
|---|---|---|
| R1 | Bündel A kastig (Topf), Köcher A mit 3 Bändern | Bauch zu eckig, Offen-Zustand riesig; Köcher fast nur Metall → Färbung kaum sichtbar → Bauch runden, Leder statt Bänder |
| R2 | 3 Varianten je Item; Köcher prozedural (Rohr entlang der Diagonale) | Bündel C wirkt wie ein Trank (Flaschenhals + Metallboden); Köcher-Rohr gleichförmig gestreift, Pfeile ein weißer Klecks → C umbauen, Köcher von Hand |
| R3 | Bündel B Kordel + Perlen, C Eckkappen; Köcher Mündung verbreitert | Bündel-Oberteil mit zwei Lappen liest sich wie Katzenohren; Köcher-Mündung unsauber, Riemen-Buckel isoliert → Oberteil asymmetrisch gefaltet, Köcher pixelweise neu |
| R4 | Köcher handgezeichnet (Familien-Layout, Rohr 5 px, Riemen mit Enderit-Schnalle, 3 Federn) | alle grün; Offen-Rückseiten orange (Umriss-/Innenflächen in einem Ton, wie Vanilla `bundle_open_back` 50,7 %), C-Vorderseite orange → Innenraum/Umriss in 2–3-px-Gruppen brechen |
| R5 | Offen-Rückseiten/C-Vorderseite gebrochen | alle 12 Varianten-Texturen grün (texrate), Spielgröße lesbar, Färbung sauber (Leder voll getönt, Beschlag/Glimmer/Pfeile violett bzw. grau) → vorlegen |

texrate eingebaut (vorher → nachher): `enderite_bundle` orange (big 19, ≥6 45,2 %) → grün (big 8, 27,4 %);
`enderite_bundle_open_back` orange (Vanilla-Umfärbung, 50,7 %) → grün (big 7, 11,3 %); `enderite_bundle_open_front`
grün → grün (27,3 %); `enderite_quiver` grün → grün (big 6, 5,6 %).

**Vorausgewählt eingebaut:** Bündel **A „Ring“** (stärkste Enderit-Kennung, Ring wird im Offen-Zustand zum Rand,
Leder voll färbbar), Köcher **B „Kappe“** (größte färbbare Lederfläche, ruhige Silhouette, Kappe + Mündungsring
wiederholen den Ring des Bündels). Andere Wahl: nur `BUILT` im Generator ändern, `--write`.

## 5. Abweichungen

- Ziele 12 statt 14 (Zählfehler im Plan; alle Item-Texturen beider Items sind erfasst).
- Wiki-Kopien/`wiki/data/textures.js`/`tools/landing/discover.html` (Zähler eigene Texturen 3232 → 3234) im selben
  Branch erneuert (`wiki/generate.py --all`), weil `--check` sonst rot wäre; dazu die zwei Wiki-Blätter der 26.2-Kopie
  (`sheets/simplebuilding/simplebuilding/item/enderite_{bundle,quiver}.png`), die nötig wurden, weil Storage- und
  Kern-Bild nicht mehr gleich sind.
- `storage_w1b_2026_11_12.py`: die nun toten Färbe-Helfer (`dyed_pair`, `dyed`, `dyed_problems`) entfernt.
