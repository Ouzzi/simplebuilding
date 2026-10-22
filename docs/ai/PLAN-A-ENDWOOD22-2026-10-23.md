# Plan a-endwood22 (TX22-3 + TX22-4, Simple End) – 23.10.

Basis claude-wave1 e93433ccf, Branch `a-endwood22`. Nur MC 26.3 Fabric (Regel 22.10.).

## Ist-Stand
- `EndLeavesBlock` (modules/simpleend/shared/.../block): Zustand `inner` (bool) = alle 6 Nachbarn Laub/Stamm/Stöcke;
  `updateShape` hält ihn aktuell; Weltgen setzt ihn nach (EndTrees, EndFlora). Bremse = Vanilla-Spinnennetz
  (0,25 / 0,05 / 0,25; Weben 0,5 / 0,25 / 0,5). Härte 0,2, Abbau wie Vanilla-Laub (Tag `minecraft:leaves` → Hacke).
- Datagen `modules/simpleend/tools/gen_data.py` `leaves()`: Blockstate multipart inner=false → `W_leaves`, true → `W_leaves_inner`.
- Texturen `modules/simpleend/tools/gen_textures.py` (Ebenen per `layers.snap`): `leaf_clumps` (Rand 78–80 %, innen 100 %).
- Tests `EndTests.java`: end_leaves_inner_state, end_leaves_act_like_cobweb (Schwert/Schere ≥ Spinnennetz), Fabric-Adapter `ModuleGameTest.java`.

## TX22-4 (voll)
1. `inner` → `density` (IntegerProperty 0–2: LOOSE/MEDIUM/DENSE). `density(level,pos)` zählt Nachbarn, die keine Luft sind
   (egal welche): 0–3 → LOOSE, 4–5 → MEDIUM, 6 → DENSE. Platzierung + `updateShape` + Weltgen-Nachlauf wie bisher.
   Standardzustand MEDIUM (= heutige Optik/Bremse für alles, was ohne Update gesetzt wird).
   Alte Welten: Eigenschaft `inner` verschwindet → Standard MEDIUM, beim nächsten Nachbarwechsel korrekt (kein Datafixer nötig).
2. Bremse je Dichte (`stuckSpeed`): LOOSE 0,5/0,15/0,5 · MEDIUM 0,25/0,05/0,25 (Spinnennetz) · DENSE 0,15/0,03/0,15;
   Weben verdoppelt seitlich, ×5 senkrecht (MEDIUM mit Weben = Vanilla 0,5/0,25), höchstens 1.
3. Abbau: Hacke in der Haupthand → Laub-Abbau (Härte 0,2, Hacke-Tempo). Sonst `getDestroyProgress` = der eines
   Spinnennetzes mit demselben Werkzeug (Hand 20 s wie Spinnennetz; Schwert/Schere so schnell wie am Spinnennetz).
   Entscheidung: „ohne Hacke ≈ Spinnennetz“ wörtlich je Werkzeug, damit Schwert/Schere wie bisher am Netz bleiben.
4. Datagen: Blockstate density 0 → `W_leaves_loose`, 1 → `W_leaves`, 2 → `W_leaves_inner`; neues Modell `W_leaves_loose`.
5. Textur `W_leaves_loose` (beide Hölzer) aus `leaf_clumps` (≈ 50 % deckend, kleinere Blätter, Pollen) – Kandidatin,
   Optik-Abnahme mit TX22-3.
6. Tests: `end_leaves_density_state` (Nachbarn 0…6 inkl. Stein, Rückweg), `end_leaves_density_brake` (Weg je Dichte
   streng fallend, MEDIUM = Spinnennetz), `end_leaves_hoe_tool` (Hand = Spinnennetz-Fortschritt, Hacke ≥ 10× schneller);
   alter inner-Test ersetzt; Fabric-Adapter; Client-Smoke auf density umgestellt (nicht ausgeführt, keine Client-Läufe).
7. Wiki (manual.json EN/DE), docs/MODS-UND-FEATURES.md, Wiki/Guides/Discover neu erzeugen.

## TX22-3 (nur exemplarisch, nichts eingebaut)
- Neu in gen_textures.py: Varianten A/B/C für Aschholz-Stamm Seite + Oberseite, entrindeter Stamm Seite + Oberseite,
  Blätter (locker/mittel/dicht), je in Ebenen (`snap`), 8–10 Töne, Vanilla-Struktur (Rindenfurchen, Ringe mit Rinde,
  Maserung), grobe Ideen bleiben (graue Asche-Rinde, Lila-Kristall-Einschlüsse, Lila-Kern im Querschnitt).
- `modules/simpleend/tools/tx22_preview.py` erzeugt `/root/sb-tools-a/previews/endwood22-preview.html`
  (16×, Kachel 3×3 im Block-Verbund, alter Stand, Vanilla Eiche/Fichte/Kirsche nur in der Vorschau aus dem Jar).
- Umstellplan nach Besitzer-Wahl: gewählte Variante in `main()` statt `log_side/rings/stripped_side/leaves*` einsetzen,
  gleiche Formensprache auf Dämmerholz (eigene Palette/Motiv: Meander/Spirale), danach Holz, Bretter (Treppe/Stufe/
  Zaun/Tür/Falltür/Schilder/Boote nutzen Bretter-Palette), Regal, Stöcke; Vorschau je Schritt; layers check + textures.js.

## Verifikation (über /root/sbt a-endwood22)
- Compile `:mc26_3:fabric:compileJava` + `:modules:simpleend:fabric:compileJava`.
- Tests `module-simpleend-fabric-263`, `module-simpleend-standalone-fabric-263`.
- Generatoren auf sb-test (gen_textures, gen_data, layers check, wiki --all, guides, discover, textures.js) → `-gen`-Branch,
  dann `./gradlew check -q`.
- Nicht: Client-Läufe, andere Loader/Versionen.
