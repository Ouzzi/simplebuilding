# Plan b-status01: Wunsch-Status-Audit (01.11.2026)

## 1. Plan
**Ist-Zustand.** Basis `hub/b-queue01` = Production `a5a4c513c` (Bündel 87) plus Queue-Abgleich. Queue hat 778 Zeilen, viele `[~]`/`[ ]` veraltet; die Besitzerlisten aus `/root/sb-tools-b/wd25.md` (WD25-WI26) stehen größtenteils gar nicht in der Queue.
**Vorgehen.** (1) `/root/sb-tools-b/build_wunsch_status_json.py` liest die Queue-Abschnitte NA24, W24 (WA/WB/WC), WD25-WG26, E29-E31, BS25-BS29b, MOB27, ABN29/29b/30, RG30, SN30, IG31, TR31-R, OW24, VIS24, MIXCHECK31, WP-01..34 und prüft die genannten Branches gegen Production (`git merge-base --is-ancestor`, sonst `git log a5a4c513c --grep=<Branch>`); Branches ohne Merge-Spur zählen nicht als Production. (2) Status-Korrekturen per Overrides gegen den Queue-Text und Code-Greps auf `a5a4c513c`. (3) Punkte aus wd25.md und `b-antworten-20261101a.md`, die nicht in der Queue standen, werden als Zeilen ergänzt (Beleg „kein Treffer in Code und Queue“ = offen). (4) `/root/sb-tools-b/gen_wunsch_status.py` erzeugt `/root/sb-tools-b/previews/wunsch-status.html` aus `wunsch-status.json`.
**Dateien.** `.claude/QUEUE.md` (Checkboxen + Klammer „(Status 01.11.: …)“, Abschnitt „Status-Audit 01.11.“, Priorität 0 = IG31-39) und diese Plan-Datei. Keine Code-Änderung, kein Gradle.
**Statusregeln.** prod = in Production belegt, nichts offen; abnahme = gebaut und gemergt, wartet auf Besitzer-/Client-Abnahme oder -Wahl; arbeit = laufender Branch (b-rest01, b-flaky01, b-heads39); offen = nicht gebaut oder nur Konzept/Vorschau/Rückfrage; verworfen = nur mit Besitzer-Zitat (VW-1, VW-3; Quelle wd25.md „VW-1 und VW-3 verwerfen“).
**Abweichungen.** Die Queue-Nummer `IG31-37` war doppelt vergeben (Linksklick-Fehler b-click33); die alte Zeile heißt jetzt `CLICK33-1`, `IG31-37` ist der Besitzer-Punkt Kisten-Netzwerk. Zeilen mit Hinweis „offen: Client-/Besitzer-Abnahme“ zählen auch bei `[x]` als `abnahme`.
**Risiken.** Viele Punkte stützen sich auf Queue-Text plus Branch-Merge, nicht auf Einzeltests. „Offen, kein Treffer“ bei WD25-WI26 stammt aus Schlüsselwort-Greps und kann Umsetzungen unter anderem Namen übersehen. b-heads39 war beim Audit nicht auf dem Hub.
**Verifikation.** Stichproben auf `a5a4c513c`: vorhanden sind frost_grass, snowlogged, GuiHeadFit, mighty/evil_conduit, rose_bronze_pressling, double_piston, signal_dampener, oak_door_1_5x2, hedgehogThorns, cauldronStand, devVariants, TrimAbilities, AstralEnchanting, wither/void_wings, DinoVariants, AnimalVariants, ItemStatsScreen, KeyConflicts sowie die Module simplefoundry, simplelivemap, simpleores, simpleresonance, simplegadgets; kein Modul für Chisels oder Building Tools. Die HTML-Seite wurde mit headless Chromium geladen (Zahlen und Filter laufen).

## 2. Ergebnis (290 Punkte)
prod 69 · abnahme 143 · arbeit 4 · offen 72 · verworfen 2
Seite: `/root/sb-tools-b/previews/wunsch-status.html`. Neubau: `python3 /root/sb-tools-b/build_wunsch_status_json.py <repo>` (mit `WRITE_QUEUE=1` auch Queue-Annotationen, nur auf frischer Queue), dann `python3 /root/sb-tools-b/gen_wunsch_status.py`.
Die meisten abnahme-Punkte warten auf Client-Sicht; die Seite trennt Production (Merge belegt) von Besitzer-Abnahme.

## 3. Offene Punkte nach Besitzer-Priorität (Abschnitt „PRIORITÄT ab 30.10.“, IG31-39 ganz oben)
0. Besitzer 01.11., hohe Priorität
- IG31-39 (arbeit): Mob-Köpfe-Kreativ-Tab kaputt: fast alle Felder leer, Köpfe verschoben (HOHE PRIORITÄT) – b-heads39 läuft, Verdacht GuiHeadFit
- IG31-37 (offen): Shulker-Kisten und Fässer ans Netzwerk anschließbar (Interface, Controller, Aggregation im – Basis ST27-1/BS29b-3
- IG31-38 (offen): Mod-Conduit-Items (Strong, Mighty, Evil, Mighty Evil) als echte 3D-Modelle wie Vanilla-Con
1. Simple Ores (ABN30-10, RG30): gebaut; offen Blei/Zink/Platin/Silberpfeil/-spiegel
- ABN30-10 (abnahme): Roségold/Simple Ores: alles in Simple Ores, Varianten in Hammers/Tools/Rüstung nur wenn Or
- RG30-2 (offen): Simple Ores: Nutzen-Liste aller Erze (Zink, Silber, Nickel, weitere) als Vorschläge zur Ab
- IG31-13 (abnahme): Materialien aufteilen: Enderit-Varianten + Rohe Enderit-Fragmente, Dimensions-Schrott, Tie – Gate, SB-Erz-Config ausblenden
2. Tastenbelegung (WP-25/UI28-1) + UI-Vereinheitlichung der Ausreißer
- UI28-1 (abnahme): Eigene Seite für Tastenbelegungen (Custom Keybinds Settings Page)
- IG31-31 (abnahme): Mod-Keypicker: einzelne Knöpfe funktionieren nicht
- IG31-32 (offen): Rohre: KONZEPT + Vorschau mit Wahl zuerst – Rohre: Besitzer-Wahl, dann Einbau
- UI-Ausreißer-Vereinheitlichung: keine eigene Zeile, nicht belegt (offen)
3. Trials + Evil/Void Omen (WP-20): gebaut
- BS27-2 (abnahme): KONZEPT „Verwitterte Prüfungen“ (Dungeon-artig mit Spawnern): eigener Trial Spawner in and
- TR31-R (abnahme): Trials-Rest (a-trials31 nicht gebaut): Wither-Treffer Stufe 2, bremsende Pfeile Stufe 3, V
4. Zonen/Resonanz/Schutz V/Enchanter (WP-22)
- MOB27-2 (abnahme): gebaut (Plan docs/ai/PLAN-A-ZONES31-2026-10-30.md, Konzept-Empfehlungen Q1-Q10, Test im Bü
- BS25-20 (offen): WARTELISTE: nur Hooks vorbereitet (`AstralEnchanting.registerEndgame`/`levelCap`/`ENDGAME_ – Dark-/End-Enchanter nicht gebaut
5. Rest
- BS25-23 (offen): KONZEPT Simple Mobs: Spawn-Eier herstellbar machen
- BS25-24 (offen): KONZEPT neuer Endgame-Boss, klein (≈ Deceiver-Größe, schlanker), inspiriert von „Clay Sold
- BS25-25 (offen): KONZEPT weiterer Boss-Mob: kann von oben nicht getroffen werden – trägt einen harten Hut w
- ABN29b-2 (offen): 
- ABN30-1 (abnahme): Dinos: Farben je Biom (Aufzucht-Biom o. ä., freie Gestaltung), realitätsnah (z. B. eher ge – Dino-Farben
- ABN30-12 (abnahme): Live-Karte: eigener Mod PARALLEL zu Simple Maps (kein Sub-Mod); „erkundet“ = von Spielern  – Live-Karte Stufen 2-4
- ABN30-13 (abnahme): Schmelzanlage = Sub-Mod von Simple Ovens „Simple Foundry“. – Foundry Stufen 2-4
- BS29b-1 (offen): PHASE 2: YouTube-Video https://www.youtube.com/watch?v=Y3iR0jDfNgc&t=346s transkribieren/z – Phase 2
- BS29-2 (offen): PHASE 3: „Simple Skilltree“, angelehnt an https://modrinth.com/mod/skills und den Craftmin – Phase 3 Skilltree
6. In Arbeit (Sitzung B)
- IG31-3 (arbeit): Kreativ-Tab „Simple Enchants“ (Dev-Tab) komplett entfernen – b-rest01
- IG31-8 (arbeit): Ledertuch (Leather Sheet) – b-rest01
- b-flaky01: Flaky-Test-Fix (ores soundproof), kein Wunsch-Punkt.
7. Nie in die Queue gelangte Besitzer-Wünsche (jetzt im Abschnitt „Status-Audit 01.11.“)
- WD25-2 (offen): Klebemasse/Kleber statt Wachs als Zustandsspeicher (End-Baum-Blätter, Eis); mit Behutsamke
- WD25-3 (offen): Inverse Trims per Herz des Meeres auf Besatz-Vorlage
- WD25-7 (offen): Bücher alle im Megaguide-Stil; Sub-Mod-Guides im Super-Mod-Guide; Config
- WE25-3 (offen): Client-Sortierung Inventar/Container (z. B. mittlere Maustaste)
- WE25-7 (offen): Untermod „Simple Chisels“ (Meißel auslagern)
- WE25-9 (offen): Befiederungstisch: optionaler Trank-Slot, Trank-Pfeile
- WF25-4 (offen): Riesen-/Klein-Mobkopf: echte Effekte (Giant/Small), beim Ausziehen entfernen
- WF25-6 (offen): Spawner-Fragment/-Brocken, behelfsmäßiger Spawner, ~30 % Fragment beim Abbau
- WF25-9 (offen): Neuer Sub-Mod „Simple Building Tools“: Baustäbe, Blaupausen, Oktanten
- WF25-10 (offen): Strong Upgrades/-Charts nach SimpleLib; Wither-Chart ohne Simple Mobs
- WF25-11 (offen): Shulker-Helm: Resistenz nur beim Schleichen mit Bodenkontakt
- WH26-10 (offen): Kompakte Blöcke Stufe 1 Compact / Stufe 2 Dense
- WI26-2 (offen): Schachfiguren: beim Abbau nur die angeschaute Figur
- WI26-3 (offen): Zwei neue Tooltip-Stufen „Corrupted“ (Glitch) und „Void/Voided“; Vorschau zuerst
- WI26-6 (offen): Enderherz ab dem 4. besiegten Drachen (1× pro Spieler, platziert um das Endportal)
- Weitere: WD25-5, WD25-18, WD25-24, WE25-5/6/8, WF25-5/7/12/13, WH26-2/3/6/7/9, WI26-1/4/5 (Seite, Filter „Offen“).
Besitzer-Rückfragen offen: IG31-4, IG31-15, IG31-14 (Wahl), IG31-32 (Wahl), WE25-8, BS25-19 (Logs).
TODO Prod-Übernahme: `b-status01` ist nur Doku/Queue; Sitzung B zieht ihn nach Freigabe in ein Bündel.
