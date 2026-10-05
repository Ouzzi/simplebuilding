# Plan Crucible-Art v2 (2026-10-05)

Branch `claude-crucibleart` (Basis cb466f86). Besitzer-Texturwahl zu den Vorschlägen A–C (Previews
`crucible-stufen-…`, `fass-stufen-…`, `seelenlava-…`, `eimer-korrektur-…`). P6-Reste (Seelen-Lava-Config, Jade, JEI-Kategorie)
laufen parallel als GPT-Auftrag auf `claude-crucibleart-gpt` und werden per cherry-pick übernommen
(Brief: `PLAN-CRUCIBLE-P6-2026-10-05.md` dort).

## Besitzerwünsche (Interpretation, Sprachdiktat)
1. Tiegel: „B oder C, mehr 3D-Modell, unten leicht schmaler, oben auch, 1–2 px“ → neues Kessel-Modell:
   Fuß y0–2 (x/z 3–13, −2 px), Boden y2–4 (2–14, −1 px), Bauch y4–11 (1–15, volle Breite), Hals y11–14 (2–14, −1 px).
   Innenraum 3–13 bleibt, Boden innen jetzt auf y=4 (vorher 3). Griffe bleiben am Bauch (y9–11).
   Texturen B (Rippen) und C (Stufenbeschlag) werden für die neue Silhouette neu gezeichnet (Zeilen = Modellzonen),
   beide in 3D-Vorschau gezeigt; die stimmigere wird für Eisen/Verstärkt/Netherit/Enderit eingebaut.
   Hitbox (`CrucibleBlock.SHAPE`) und Inhalt-Renderer (`CrucibleRenderer.FLOOR`) folgen der Form; Rohling-Modelle
   (crucible_blank_1..5) bauen dieselben Wände.
2. Fass Kupfer/Verstärkt/Enderit „analog zu den Chests“: Vanilla-Fass-Form (Dauben + 2 Reifen, Deckel mit Ring),
   Materialfarben und Beschläge aus den Truhen: Kupfer = Vanilla-Kupfertruhe (Kupfer + dunkelrote Kanten),
   Verstärkt = SB-Verstärkte Truhe (graue Stahlplatten, helle Eckbeschläge, Diamant-Türkis-Schloss),
   Enderit = SB-Enderit-Truhe (Lila Platten, helle Eckbeschläge, rosa Funken/Schloss).
3. Seelen-Lava: Variante B (Kanäle) still + fließend, Eimer-Inhalt aus derselben Palette. Nebel in Seelen-Lava türkis
   (Client-Mixin auf den Lava-Nebel, gilt für alle Loader, da gemeinsamer Client-Code).
4. Kupfer-Eimer eckiger mit Muster (eigene Silhouette, Vanilla-Eimergröße, je Oxidationsstufe; gewachst = gleiche
   Textur wie bisher), Enderit-Eimer verziert. Je 3 Varianten in der Vorschau, beste eingebaut (Wasser/Lava/Seelen-Lava
   folgen der Form).

## Dateien
- `modules/simplelib/tools/gen_resources.py` (Modell), `tools/crucible_sb_resources.py` (Enderit-Kopie, unverändert
  logisch), neu `tools/textures/crucible_art_v2_2026_10_05.py` (alle neuen Texturen + Vorschau + 3D-Renderer, `--check`).
- `modules/simplelib/shared/java/com/simplelib/crucible/CrucibleBlock.java`, `client/CrucibleRenderer.java`.
- Fog: Client-Mixin in `common/src/shared` (bzw. overlay + Zwilling, falls 26.2 abweicht).
- Texturen: simplelib `textures/block/{iron,reinforced,netherite}_crucible_*`, `{copper,reinforced}_barrel_*`,
  SB overlay `enderite_crucible_*`, `enderite_barrel_*`, `soul_lava_*`, `item/*bucket*`.
- `tools/textures/crucible_placeholders_2026_10_05.py` `--check` passt nicht mehr zu den neuen Bildern → dessen
  Ressourcenliste wird auf die Teile beschränkt, die er noch besitzt (Seelenbrand-Icon, verstärkter Kessel).

## Risiken
- Optik nur per Vorschau-Renderer geprüft (kein Client): Z-Fighting/UV-Fehler im Spiel möglich → Client-Abnahme offen.
- Boden höher → Inhalt-Renderer muss mitgehen, sonst schweben/versinken Items.
- Wiki rendert Blockmodelle selbst (`wiki/generate.py`): neue Elemente müssen darstellbar bleiben.

## Verifikation
Generator `--check`, Vorschau-PNG `previews/crucible-art-v2-vorschau.png`, GameTests Tiegel (Form/Hitbox unverändert
funktional), Gates fabric-263/neoforge-263/forge-263, Module simplelib/sandwiches je Loader, `gradlew check -q`,
Compile 26.2, Datagen 26.3, Wiki `--all` + `--all --check`.
