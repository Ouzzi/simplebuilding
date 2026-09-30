---
name: testabdeckung-2026-09
description: Stand der Testabdeckung — alle Arbeitspakete P1–P8 am 2026-09-10 abgeschlossen; was noch offen bleibt und warum
metadata:
  node_type: memory
  type: project
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-11T01:00:00.000Z
---

**Aktueller Stand (Audit vom 2026-09-08, Commit `0f2738a`, Bericht in `testing/AUDIT-2026-09-08.md`):**
1000 Verhaltensweisen untersucht, 702 gedeckt (70 %), 75 teilweise, 223 ungedeckt. 22 Prüfer,
jede „ist abgedeckt"-Behauptung adversarisch gegengelesen; diesmal zählen Server- **und**
Client-Tests als Deckung.

Offen: **127 serverseitig schreibbar, 74 clientseitig**, dazu 30 strukturell blockiert,
24 harness-blockiert, 41 nicht lohnend (die letzten drei Kategorien sind Vorschläge der Prüfer,
kein Urteil — sie stehen als P4 in `testing/PLAN.md` und sind einzeln gegenzulesen).

**Der wichtigere Fund: 104 falsche Grün-Meldungen** (90 als „sicher" eingestuft). Rohdaten liegen
diesmal im Repo: `testing/audit_offen.json` (298 Einträge) und `testing/audit_falsegreens.json`
(104 Einträge, jeder mit einer konkreten Mutation, nach der die Suite grün bleibt).

Eine davon selbst nachgestellt und bestätigt: In `RotatorItem#getRimDirection` die z-Prüfungen der
Y-Fläche vor die x-Prüfungen gezogen → **269 von 269 Tests bleiben grün**, obwohl ein Eckklick den
Stamm danach in die falsche Achse legt. Kein Test im Repo klickt je eine Ecke.

**Stand 2026-09-24 abends (Feature-Paket, Commit `f5288a9`):** Server 316/316/314/314 = 1260, Client 4 × 104, P10 (47 Server-Gegenproben) rot auf beiden Linien, Gate GO 1676/1676.

**Stand 2026-09-24 früh:** Server 271/271/269/269 = 1080 (Constructor's-Touch-Test läuft jetzt auch auf 1.21.11), Client 4 × 102; P9-Katalog (18 Gegenproben für die Fixes der 27 P4-Einträge + isDestroying) rot auf beiden Linien und allen vier Client-Zielen, Commits `d63a419`, `3bfc8d3`.

**Endstand 2026-09-10 (alle Pakete P1–P8 abgeschlossen, `testing/PLAN.md` Abschnitt 6):**
Server 271 / 271 / 268 / 268 = 1078, alle grün. Client: **alle vier Ziele 102 Prüfpunkte** aus
demselben geteilten Baum, alle grün, `CLIENT_PARITY_DEBT` leer. Reproduzierbar mit
`python tools/testrunner/run.py --release-gate --targets everything`.

- **P4** (97 Einträge): 27 Entscheidungen des Besitzers, 47 begründet offen, 10 harness-blockiert,
  3 inzwischen gedeckt, **10 schreibbare — alle geschrieben** (drei Werkzeug-Töne über den
  `SoundRecorder`, Oktant-Formen/-Orientierung als Bildvergleich, Vein-Miner-Vorschau per Mining
  Fatigue II, Erzdetektor-Haltbarkeit mit echtem Überlebensspieler, Trim-Knopf-Symbol und
  -Tooltip aus dem GUI-Renderzustand). Stand je Eintrag in `audit_offen.json` unter `p4.stand`.
- **P7 clientseitig**: 21 Schärfungen, alle 22 Mutationen (`mutations.py --run`) rot mit der
  erwarteten Meldung. Zwei Schärfungen sahen ihre Mutation erst nach Nachbesserung (äquivalenter
  Mutant am Strip-Miner-Zweig → Vein-Miner-Fall; Schwerpunkt auf einer aus dem Bild ragenden
  Ebene → 3×3-Ebene; Sofort-Feedback erst im selben Aufruf messbar).
- **P6**: 27 Server-Mutationen in Verzauberungen/Werkzeugen/Schwerkraft/Besatz
  (`mutations.py --p6 --run`), 27 rot. Befund: `VeinMinerUsageEvent` trug eine eigene Kopie von
  Budget und Suche — behoben, fragt jetzt `MiningUtils.getVeinMinerBlocks` wie die Vorschau.
- **P6b (2026-09-11)**: 48 Mutationen in den übrigen Bereichen (`--p6b`), per Workflow entworfen
  (8 Entwerfer, 48 Skeptiker), 48 rot im ersten Lauf, kein neuer Befund. **Beide Runden auch auf
  der 1.21.11-Kopie rot** (`--line 1.21.11`, `ON_1_21_11` für die vier Linien-Anker/-Tests) — das
  ist der Beweis, den `--drift` nicht geben kann: die übersetzten Körper *beißen*. (`ON_1_21_11`
  hat sechs Einträge: vier Server-Anker/-Tests, zwei Client-Anker.)
- **Client auf allen vier Zielen (2026-09-13)**: 31 clientseitige Gegenproben (20 aus P7 + 11 für
  die zehn neuen Client-Tests) je Ziel adressiert (`for_target`, `ON_NEOFORGE`, `ON_1_21_11`) —
  31/31 rot auf Fabric 26.2, NeoForge 26.2, Fabric 1.21.11, NeoForge 1.21.11; die zwei
  serverseitigen P7-Mutationen je Linie. Belegt durch Datensätze + `--reread`-Neulesungen (Feld
  `rereadOf`) + wache Wiederholungen der vom Ruhezustand getroffenen Runden. Der Runner liest
  Verdikte nur aus dem eigenen `run.py --json`-Datensatz und dessen archiviertem Log (nie aus
  `latest.log`/`junit.xml`). Release-Gate prüft alle Anker (`mutations.py --check --all-catalogues`).
- Nebenbefunde am Mod, behoben: die drei HUD-Overlays ignorierten F1 auf NeoForge (Fabric erbt den
  Schalter der Vanilla-Ebene, NeoForge nicht); `mc1_21_11/neoforge/build.gradle` las noch die
  RESULT.txt des abgelösten RendererProofRun — jeder Lauf „scheiterte", die Screenshot-Zählung des
  Runners hat es kaschiert.

**P8 (2026-09-10): die 1.21.11-Testkörper hingen eine Woche hinter 26.2.** Vier Commits vom
2026-09-03/04 (`284ee2d`, `37c41f4`, `e6b0546`, `73159b7` — „Stapel 4", 94 geschärfte Tests) haben
nur 26.2-Dateien angefasst; das Paritätstor vergleicht Ids, Schärfungen fügen keine hinzu. Rund
2 400 Zeilen in 15 Klassen. Nachgezogen per übersetztem Commit-Replay bzw. Frischport;
`tools/port_tests_to_1_21_11.py --drift` vergleicht seitdem die Körper und hängt im Tor.
Lehre: **Id-Parität ist keine Körper-Parität** — jede Änderung an einem Test gehört auf beide
Linien, und nur ein Körpervergleich merkt, wenn nicht.

**P7 serverseitig:** 80 von 81 falschen Grün geschärft, jede durch ihre Mutation belegt
(70 Mutationen, 70-mal rot). Die eine Ablehnung ist begründet. Clientseitig siehe oben.

**Was die Parität sichert:** `check_parity()` in `tools/testrunner/run.py` hängt im Release-Tor und
wird rot, wenn eine Test-Id nur auf einer MC-Linie existiert (Ausnahmen als *Gegenstücke* in
`LINE_DIFFERENCES`), wenn eine solche Erklärung veraltet, oder wenn der Client-Rückstand wächst.
Gleich beim ersten Lauf fand es eine echte Lücke: `wandering_trader_can_roll_amod_trade` gab es nur
auf 1.21.11 — inzwischen portiert.

**Vorgeschichte:** Das Audit vom 2026-09-03 kam auf 36 % und 406 schreibbare Lücken; die sind alle
geschlossen und die damaligen 138 falschen Grün-Meldungen repariert (Commits `8699e49`, `37c41f4`,
`e6b0546`, `73159b7`, `0380be1`). Das jetzige Audit untersucht mehr Verhalten (1000 statt 765) und
zählt strenger, deshalb sind die Zahlen nicht eins zu eins vergleichbar.

Das Feature-Inventar kommt aus `wiki/manual.json`, der Testkatalog aus `SimpleBuildingGameTests.java`.

Siehe auch [[gametest-harness-fallen]], [[client-test-geruest]] und [[offene-mod-befunde-2026-09]].
