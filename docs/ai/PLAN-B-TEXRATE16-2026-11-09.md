# Plan b-texrate16: A8d-Überarbeitung der roten/orangen Texturen nach Problemgruppen, Verankerung in den Generatoren (09.11., N13d Fortsetzung)

Auftrag B (N13d Fortsetzung), Basis hub/b-bundle166 (enthält b-texrate15, b-pal15, b-fun14, b-metals14). Antwortcode TEXRATE16.

## Ist-Stand
- b-texrate15: A8d + `tools/textures/texrate.py`, Bewertung `texrate15_2026_11_09.py` (1986 Texturen), Rework
  `texrate15_rework_2026_11_09.py` (R3-Flächenbrecher) für 24 Metallformen + 10 End-Paletten; Rest offen: Pads (hart
  30–48 %), Lampen, Esche/Dämmerholz, Licht-Scheiben, Vitrinen, Froschlichter. Generator-Neuläufe (generate_textures,
  metals14) schrieben die alten Pixel zurück (gemessen: generate_textures `--check` meldete die 10 End-Paletten veraltet).
- generate_textures `--check` ist auf der Basis rot (fehlende Handvorlage hand/enderite_carrot.png, 78 fremde veraltete
  Dateien), pad_fade `--check` ebenfalls (Pads ins Modul simplepads umgezogen) – beides nicht von dieser Runde.
- Pads: Besitzer-Originale (tools/textures/hand/pad_res), am 02.10. von generate_textures auf Vanilla-Rampen quantisiert.

## Vorgehen
1. Bewertung aufgefrischt (`texrate15_2026_11_09.py --base hub/b-bundle166`): 2032 Texturen, 547 rot / 517 orange /
   619 grün / 349 grau (pal15-Meißel und Simple Fun jetzt ohne Sperre).
2. Vanilla-Analyse je Gruppe (`texrate.py`-Messung der Jar-Texturen): glass/white_stained_glass rot (191 px), Froschlicht
   rot (132 px), redstone_lamp rot (88 px, hart 18/48 %), glowstone/bedrock/furnace_front_on hart 39–47 %,
   end_portal_frame_top hart 43 %, respawn_anchor_top hart 16 %, birch/cherry Türen orange/grün, copper_bulb_lit orange.
   Folgerung: Kanten-Motive (Pads, Streifen, Funken) bekommen gestufte Extreme statt Brecher; leuchtende Flächen werden
   in echte Stufen gesetzt; Holz bekommt Maserungsstriche; Glas/Froschlicht-Mitte bleiben Vanilla-ruhig.
3. Werkzeuge: `texrate16_2026_11_09.py` (Auswahl, Gruppe+Methode, nur Ziele mit Ampel-Gewinn, Vorschau),
   `texrate16_rework_2026_11_09.py` (Methoden quant/soft/areas/fine/grain/stripe/lamp/pad, Runden R1–R3, `--apply`
   idempotent aus der Ebenen-Basis, `--check`), Ziel-Liste `texrate16_targets.json` (inkl. texrate15-Ziele, Methode r15).
4. Verankerung: `layers.save()`/`write_layers_for()`, generate_textures.py (Schreiben + `--check`), light_panes,
   copper_light_panes, pad_fade rufen `finish(img, pfad)`. Generatoren, die die Baum-PNG selbst lesen (hand_drawn), werden
   erkannt (PNG = Datei mit Rework-Ebene → nicht doppelt).

## Runden (A8c) – Bewertung
- **R1** (erster Wurf, grobe Grenzen): Pads mit linearer Kontraststauchung → Wirbel verschwindet (elytra_pad R2 fast
  einfarbig); Lampen/Froschlichter mit Medianschnitt → graue Flecken in hellen Mitten; Vitrinen mit Flächenbrecher → die
  Schrägstreifen zerfielen in Krümel.
- **R2** (Korrektur): Pads-Stauchung nur jenseits ±22 Helligkeit vom Mittelton (Wirbel bleibt lesbar), Ziel hart ≤ 20 %
  statt 12 % (Kante ist Motiv, wie Vanilla-Endportalrahmen); Quantisierung auf echte Töne der Textur (Bucket-Mitglied
  statt Mittelwert); Streifen: Kontraststufe + Tonwechsel nur diagonal längs; Holz: nur 2–3-px-Striche.
- **R3** (gebaut): R2 + Hauptton ≤ 38 %. Blick 1x/8x neben Vanilla (birch_trapdoor, cherry_door_top, stripped_birch_log,
  copper_bulb_lit, respawn_anchor_top, glass_pane_top, redstone_lamp): Holz liest sich als Maserung, Scheiben als
  gestufte Leuchtfläche, Pads behalten Wirbel bei weniger Flimmern, Streifen bleiben Streifen.
- Animationsstreifen: Farbschritte über den ganzen Streifen, Brecher auf Bild 0 und in alle Bilder übernommen, wo das
  Pixel gleich Bild 0 ist (kein Flackern; geprüft an potion_pad_cooling, pocket_jukebox_playing).
- Nach dem Blick zusätzlich ausgeschlossen: lit Öfen, Verzauberer-Oberseiten, Lampen-an, Dimensionsschrott, Farbkasten,
  glitched_bedrock (Schein/Funken gedämpft), Froschlichter (Vanilla-ruhige Mitte).

## Ergebnis
565 Ziele (533 neu überarbeitet + 32 texrate15 verankert): 101 rot / 678 orange / 904 grün / 349 grau gesamt (vorher
547/517/619/349). Zählung je Mod und Gruppe: docs/ai/TEXTUR-BEWERTUNG-2026-11-09.md „Runde 16“.

## Entscheidungen / Abweichungen
- Orange nur überarbeitet, wo die Ampel besser wird (orange → grün); orange → orange hätte abgenommene Texturen nur gedämpft.
- Vom Besitzer abgenommene rote Texturen wurden überarbeitet, wenn eindeutig rot (Messwert über der roten Grenze) und die
  Ampel besser wird; Besitzer-Zeichnungen (OWNER-Liste in texrate16_2026_11_09.py) nie.
- **Rückfrage Besitzer:** Pads beruhen auf deinen Originalen (02.10. quantisiert); Runde 16 senkt nur die Extreme
  (Wirbel/Funken gleich). `pads alt` → Gruppe aus texrate16_targets.json nehmen, `texrate16_rework --apply` + Checkout.
- pad_fade bleibt auf der Basis rot (Pads im Modul simplepads, Overlay-Fades veraltet) – nicht Teil dieser Runde.
- mc1_21_11-Kopien behalten alte Pixel (Port-Run); Wiki-Kopien erneuert das Bündel.

## Verifikation
`python3 tools/textures/texrate.py --selftest` ok; `layers.py check` 0 Probleme (3148); `check_missing_textures` grün;
`texrate16_rework_2026_11_09.py --check` 565 aktuell; light_panes/copper_light_panes `--check` OK; metals14 und pal15 `--own`
neu → keine Änderung; generate_textures `--check` mit Stub für die fehlende enderite_carrot-Vorlage → 0 veraltete Ziele
(78 fremde wie auf der Basis). Kein sbt (nur PNG/.layers.json/Python/Doku, keine Namen/Java).
