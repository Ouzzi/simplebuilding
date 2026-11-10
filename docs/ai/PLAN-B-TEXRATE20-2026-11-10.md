# PLAN b-texrate20 (N20b, 10.11.2026) – neue Texturen für die TEXRATE16-Einwände

Branch `b-texrate20`, Basis `hub/b-texrate17` (enthält b-bundle171-gen + Runde 17). Auftrag /root/b-auftrag-20261109.md N20b,
Wortlaut Besitzer /root/sb-tools-a/abnahme-a/antworten.md (10.11. ~09:00): „Fast alle mit Einwand sollen eine neue Textur
bekommen, da sie mir nicht gefällt.“ → nicht die Vor-TEXRATE16-Fassung zurück, sondern neu entwerfen (A8a/A8c/A8d).
Alle übrigen TEXRATE16-Texturen sind abgenommen und bleiben unberührt.

## Ist-Stand
- Die 49 Einwand-Dateien (47 genannt + die zwei Grund-Vitrinenrahmen `display_case_frame`/`glow_display_case_frame`, Teil
  der „17 Farben“) standen in `tools/textures/texrate16_targets.json` und wurden über `texrate16_rework.finish()` aus der
  Generator-Ausgabe weichgerechnet. Anmerkungen je Textur gab der Besitzer nicht (nur die Liste) – Leitlinie daher: neues
  Motiv, Vanilla-Grammatik, klare Pixel.
- Generatoren: claim_table_2026_10_15 (ohne finish), light_panes/copper_light_panes (finish), simpleanimals gen_items
  (layers.save), simple_shops_2026_10_15 (Grundrahmen) → simple_shops_tint_2026_10_24 (16 Farben: 4 Eisentöne → Farbrampe),
  shops31_2026_10_30 (Einlass, ohne finish), chess_2026_10_06 (ohne finish), generate_textures (Bündel/Köcher aus
  tools/textures/hand/ + Glimmer). Ohne Generator: infernite/terranite/zenithite_block, cracked_astralit_bricks.
- Bündel/Köcher: die Items liegen seit dem Umzug in Simple Storage; deren Kopien (`modules/simplestorage/.../item/`) sind
  die im Spiel benutzten, `src/main/.../item/` ist die bewertete Altkopie → beide bekommen Design A.

## Umsetzung
- Neu `tools/textures/texrate20_2026_11_10.py`: alle Entwürfe (A eingebaut, B für die Vorschau), reines Python, Ebenen je
  Material (A8 b). `--apply` (Ziele ohne Familie schreiben + .layers.json), `--check`, `--variant B --out DIR`.
- Verankerung: `texrate16_rework.finish()` fragt zuerst `texrate20.override_img()` → alle finish-/layers.save-Generatoren
  schreiben Design A. Zusätzlich eingehängt: claim_table (override + Ebenen), chess (finish für alle Felder, `--check` jetzt
  pixelweise statt byteweise), shops31 (`build_all` nimmt die texrate20-Ebenen), simple_shops_2026_10_15.case_frame()
  zeichnet texrate20.case_frame(trim) + schreibt Ebenen, simple_shops_tint kennt zwei Halbstufen (IRON_MID → Rampenmitte).
  simpleanimals gen_items `--check` vergleicht jetzt nach finish (vorher meldete es jedes Rework-Ziel als veraltet).
- `texrate16_targets.json`: die 49 Ziele entfernt (399 bleiben, `--check` OK).

## Entwürfe (A = eingebaut, B = Alternative)
| Textur | A | B |
|---|---|---|
| claim_table_top | Karte im Holzrahmen: Land, Wasser, Claim-Grenze, Fahne | Urkunde auf Brettern, Textzeilen, Wachssiegel |
| claim_table_side | Tischkante, Beine, Fach mit zwei Urkundenrollen | Paneel mit geschnitzter Fahne |
| claim_table_bottom | 4 waagrechte Bretter, versetzte Fugen | senkrechte Bretter + Querleiste |
| redstone_light_pane / copper_… (Item) | 16 Signalstärken = 16 Farbfelder (Pixel der Familien-Scheibe je Farbe), Rahmen in 3-px-Abschnitten | 4 große Farbfelder hinter einfarbigem Kreuz |
| truffle_soup | Vanilla-Schale, cremige Suppe, Trüffelstücke | tiefere Schale, Trüffelscheibe |
| sales_storage_intake | Trichtermund mit Eisenkragen auf den Seitenbrettern, Münzen | Eisenplatte mit Münzschlitz |
| display_case_frame (17) + glow (17) | gebürstete Eisenplatte, Fase in Abschnitten, Nieten; Leucht: Bernstein-Nieten auf Säule/Deckel + Leiste | senkrecht gebürstet (Pfosten) |
| chess/purpur | 2×2 Purpurkacheln (Vanilla-Grammatik) | Kacheln mit eingelassenem Quadrat |
| chess/quartz_accent | Rauchquarz (dunkel, warm) | Gold |
| cracked_astralit_bricks | auf den abgenommenen astralit_bricks: Haarrisse + Abplatzer | gespaltene Ziegel, Bruchstück dunkler |
| enderite_bundle | Vanilla-Bündel-Umriss, violett-schwarzes Leder, Enderit-Halsring, Leuchtschließe | Kordel + 2 genietete Platten |
| enderite_quiver | Umriss/Riemen/Pfeile der Köcher-Stufen, Leder quer schattiert, 3 Enderitbänder | Enderitplatte + Naht |
| infernite/terranite/zenithite_block | Metallblock (Gold-/Diamantblock-Grammatik): Fase, zwei Glanzdiagonalen, Akzentader des Barrens | vier Barrenplatten |

## Runden (A8c) – Bewertung
- **R1** Erstentwurf. Befund: Claim oben zu unruhig (rot, hart 38 %), Seite A wirkt wie Fenster; Suppe grau; Lichtscheiben-
  Rahmen eine Fläche (60 px), Kreuz in B kariert; Einlass innen flach; Risse zu hart (25 %); Metallblöcke große Grundfläche
  (96 px); Vitrinenrahmen flach (122 px) bzw. B hart 37 %.
- **R2** Grenze durchgehend, Urkundenrollen, Suppe wärmer, Rahmen in 3-px-Abschnitten, Regenbogen-Reihenfolge, Kreuz
  einfarbig, Einlass gebrochen, Risse mit 2. dunkelstem Ton, mehr Kachel-Haufen. Rest: Metall rot (66 px), Rahmen rot.
- **R3** Metall A per Regel (zwei volle Glanzdiagonalen, gebrochene Schattenlinie) → orange (29); Vitrinenrahmen aus 6
  Eisenstufen (zwei Halbstufen, Tönung mitgeführt), keine harten 3/2-Sprünge → orange; Claim-Farben gedämpft (hart 27 %).
- **R4** Claim-Seite A Fach gebrochen (43 → 26); Vitrinenrahmen dunkler (innen nur Halbstufen + Grundton) – mit den hellen
  Tönen wurden die 16 Farben pastellig; RNG-Fehler im Rahmen-Zufall behoben (nur 3 von 10 Tönen wurden gezogen).
- Geprüft je Runde in 1× und 6–10× nearest-neighbour neben Vanilla (cartography/fletching table, mushroom_stew, bundle,
  hopper, purpur/quartz/gold/iron/diamond block, cracked_stone_bricks), Blöcke als 3×3-Kachel, Vitrine als Modellstreifen
  (Spalte 0/Zeile 0). Kein Client-Lauf (sb-test belegt).
- Ampel A (texrate.py): grün Suppe, Köcher; kein Rot; orange (Bündel A: Lederfläche wie Vanilla bundle bewusst ruhig; (begründet: Detail-/Karten-Motive hart wie Vanilla
  cartography_table_top 24,6 %, Metall/Eisen flach wie Vanilla iron_block) Claim-Flächen, Lichtscheiben (Farbanzahl kommt
  aus der Familien-Körnung), Einlass (Holz der Seite), Purpur/Quarz, Risse, Metalle, Rahmen.

## Prüfung
texrate20 --check OK; texrate16_rework --check OK (399); texrate17 --check OK; simple_shops/tint/shops31/claim_table/
light_panes/copper_light_panes/gen_items --check OK; layers check 0 Probleme; texrate --selftest ok; check_missing_textures
0 Fehler; check_data simpletweaks/simplelights/simpleanimals/simpleshops/simpleores/simplestorage grün; unittest
tools/textures/tests 64 OK. Kein sbt (keine Java-/Namensänderung).
Nicht prüfbar/Vorbestand: generate_textures --check auf der Basis nicht lauffähig (enderite_carrot-Vorlage); chess --check
meldet 10 fremde Altabweichungen (purpur_accent, nihilith*, polished_*, knight.json) – die zwei neuen sind aktuell.

## Offen
- Besitzer TEXRATE20 (Vorschau /root/sb-tools-b/previews/b-texrate20-preview.html, Generator
  /root/sb-tools-b/b-texrate20-review.py). Wahl B = in texrate20 `BUILT`/DESIGNS die Variante je Ziel umstellen, dann
  `--apply` (Rahmen: simple_shops_2026_10_15 + simple_shops_tint laufen lassen).
- Gefärbte Bündel/Köcher (`*_dyed*`) kommen weiter aus der alten Handvorlage (tools/textures/hand/); erst nach Abnahme die
  Vorlage auf Design A umstellen (Riemen-/Lederfarben-Mengen in generate_textures BUNDLE_STRAP/QUIVER_LEATHER prüfen).
- chess/purpur_accent ist aus dem alten Purpurfeld abgeleitet (abgenommen, nicht angefasst).
- Wiki-Kopien/textures.js im Bündel (-gen); Client-Sicht im Bündel. mc1_21_11-Kopien alt (Port-Run).
- TODO development → Production über Bündel/Gate durch A nach Freigabe.
