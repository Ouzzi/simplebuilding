# Plan A-DINOREST3: DINO-REST Schritt 3 – Parasaurolophus („Leittier“)

Stand 12.11.2026, Agent a-dinorest3, Branch `a-dinorest3` (Basis `hub/a-dinorest2` 8e9289610 = b-bundle190 + Triceratops + Stegosaurus). Nur MC 26.3 Fabric.
Auftrag: `/root/sb-tools-a/agenten-1530.md` §19. Konzept: `docs/ai/KONZEPT-A-DINO36-2026-11-12.md` §4.4 (DINO36 angenommen, Empfehlungen gelten).
Muster: `docs/ai/PLAN-A-DINOREST1-2026-11-12.md` (Triceratops), `docs/ai/PLAN-A-DINOREST2-2026-11-12.md` (Stegosaurus), Teil-Hitboxen
`docs/ai/PLAN-A-DINO36B-2026-11-12.md`. Vorschau `/root/sb-tools-a/previews/a-dinorest3-preview.html`, Antwortcode **DINOREST3**
(TEXTUR-REGEL: vorher/nachher je Textur, ok/Einwand je Textur).

## 1. Ist-Stand (gelesen)

- Rahmen aus a-dinorest1/2: `Dino#kind()`, `usesJumpAbility()` + synchronisiertes `ABILITY_COOLDOWN` (Server zählt, Sprungbalken zeigt;
  der Client sendet keinen Sprung, solange `getJumpCooldown() > 0`, `LocalPlayer#aiStep`), `onPlayerJump` (Reiter-Seite) + `handleStartJump`
  (Server, nur wenn `canJump()` und Stärke > 0, `ServerGamePacketListenerImpl` START_RIDING_JUMP), `isFood`/`isFavoriteFood`/`healFor`,
  `DinoKind.entity()`, Eierschalen (`EGGSHELLS`; Para-Schale samt Textur/Knochenmehl-Rezept existiert), Rüstung über `armor(material,key,tag)`,
  `PartRig` (Kopf/Schwanz-Teilboxen, `solidHead`, `tailExtra`), Generator `tools/gen_models.py` (`<art>_spec` → Textur, `<Art>Layers`, `<Art>Bones`),
  Item-Sprites `tools/dino_items.py`, Fundort-Gewichtung (Sumpf/Mangrove Para ×2) wirkt, sobald die Art schlüpft. Ei/Schlüpfen generisch über
  `DinoKind.entity()` (`DinoEggBlockEntity`), keine artspezifische Stelle.
- Tests heute: `registered` erwartet 3 schlüpffähige Arten (Para/Raptor nicht), `dino_kind_site_weights` rechnet Badlands mit T. rex/Tri/Stego
  (Anteile ändern sich, sobald Para schlüpft) → anpassen. `egg_other_kinds_wait_for_their_dinosaur` nutzt schon das Utahraptor-Ei.
- Vanilla 26.3 (Quelle `/root/mcsrc263np`):
  - **Schwimmen mit Reiter:** `LivingEntity#travelInFluid` ruft `floatInLiquidWhileRidden(ENTITY_FLOATABLE)`: ein Fahrzeug im Entity-Tag
    `minecraft:can_float_while_ridden` (Pferd, Esel, Maultier, Kamel …) steigt +0,04/Tick, solange es tiefer als `getFluidJumpThreshold()` im
    Wasser steckt. Abgeworfen wird der Reiter nur, wenn seine Augen unter Wasser sind **und** das Fahrzeug im Tag `minecraft:dismounts_underwater`
    steht (`LivingEntity#baseTick`, Pferd/Kamel stehen darin). → Para in `can_float_while_ridden`, **nicht** in `dismounts_underwater`, eigene
    Schwimmtiefe über `getFluidJumpThreshold()` (der Reiter bleibt trocken), keine eigene Physik.
  - **Wassertempo:** `travelInWater` mischt mit dem Attribut `WATER_MOVEMENT_EFFICIENCY` (wie Wasserläufer) Bodenreibung und Reittempo ein
    (schwimmend halbiert) → Attribut 1,0 + Reittempo im Wasser × 0,8 ergibt rechnerisch ≈ 0,6 × Landtempo (Konzept).
  - **Folgen ohne Mixin:** `PathNavigation#moveTo(Entity, reachRange, speed)` (Teilpfade, wenn das Ziel weiter als FOLLOW_RANGE ist),
    `Animal#isFood` (Vanilla-Locken), `Leashable#isLeashed`, `OwnableEntity#getOwnerReference`, Vanilla-Tiere haben `FloatGoal`
    (Navigation darf schwimmen) → die Herde folgt auch durchs Wasser.
  - **Klang:** `SoundEvents.GOAT_HORN_SOUND_VARIANTS` (8 Hörner; 5 „call“, 6 „yearn“, 0 „ponder“), Lautstärke 4 = 64 Blöcke Hörweite.
  - Flucht: Vanilla `PanicGoal` (Schadensart-Tag `panic_causes`), Ziele laufen nur ohne Reiter (`isEffectiveAi`).

## 2. Umfang

### 2a. Parasaurolophus (Konzept §4.4, Tabelle §4)
- Entity `simpledinos:parasaurolophus` (`Parasaurolophus extends Dino`): 60 LP, Tempo 0,24, kein Sprung (Sprungtaste = Ruf), Stufe 1,0,
  Rückstoßfest 0,3, Fallschaden 0,5, `WATER_MOVEMENT_EFFICIENCY` 1,0; Modell in 75 % wie die anderen; Kern-Hitbox am Modell bestimmt
  (Konzept 2,2 × 3,6 – Höhe dort mit Kopf).
- Teil-Hitboxen: Hals + Kopf (Schädel, Entenschnabel, Unterkiefer, Kamm 3 Würfel) und Schwanz (3 Glieder). Langer Hals → Kopf wird wie beim
  T. rex **eingezogen** (Hals zurück/hoch, Kopf nickt), Ruf-Pose = Kopf hoch + Maul offen. Ob der Kopf fest sein muss, entscheidet die
  Skelett-Rechnung (bleibt der eingezogene Kopf hinter der Kernbox-Front → nicht fest wie T. rex, sonst fest wie Tri/Stego).
- Futter `#simpledinos:parasaurolophus_food`: Süßbeeren (Liebling), Weizen, Heuballen, Rote Bete.
- **Kamm-Ruf (Sprungtaste, nur erwachsen, mit Sattel und Reiter):** Server wählt bis `callMaxAnimals` (24) Tiere im Umkreis `callRadius` (24),
  nächste zuerst: nur Tiere aus `#simpledinos:herd_followers` (Kuh, Mooshroom, Schaf, Schwein, Huhn, Ziege, Kaninchen, Pferd, Esel, Maultier,
  Lama, Kamel, Gürteltier, Schnüffler; optional `simpleanimals:deer|zebra|zorse`); Haupthand leer → alle davon, sonst nur, wer das Item als
  Futter mag (Vanilla `Animal#isFood`: Weizen → Kühe, Schafe, Ziegen …; Karotte → Schweine, Kaninchen; Samen → Hühner). Ausgenommen: von
  anderen gezähmt (Besitzer ≠ Reiter), angeleint, geritten oder selbst Fahrgast, mit Namensschild (`callIgnoreNamed`), Standort in fremdem
  Claim (SimpleLib-Probe `InteractionGuard.permission` am Block des Tiers, je Chunk und Ruf einmal). Folgen `callSeconds` (120) lang.
  Abklingzeit `callCooldownSeconds` (20) im Sprungbalken; in den ersten 5 s nach dem Ruf ist der Balken frei: ein zweiter Druck **entlässt**
  die Herde (Konzept), danach läuft die restliche Abklingzeit. Neuer Ruf ersetzt die alte Herde.
- **Folgen (kein Mixin, keine KI-Ziele der Tiere):** der Para lenkt seine Folger selbst: je Folger alle 20 Ticks (gestaffelt nach Index)
  `navigation.moveTo(para, 2, 1.2)`, steht er näher als ~3 Blöcke vor dem Para, hält er an; Noten-Partikel über dem Folger alle 40 Ticks.
  Folger fällt heraus, wenn er tot/entladen, angeleint, geritten oder weiter als 64 Blöcke weg ist. Liste ≤ 64 UUIDs, nicht gespeichert:
  endet beim Entladen/Tod des Para.
- **Schwimmen:** siehe §1; Reittempo im Wasser × 0,8 (≈ 0,6 × Land), Schwimmtiefe ~1,3 Blöcke × Größe; der Reiter wird nie abgeworfen.
- **Abwehr:** greift nie an; wird er (ohne Reiter) verletzt, flieht er (Vanilla `PanicGoal` auch für Erwachsene) und stößt einen Warnruf aus
  (nur Ton, höchstens alle 3 s).
- Klänge bis zur Hörabnahme (R14): Ruf = Ziegenhorn „call/yearn/ponder“ tief (0,55–0,65), Warnruf = Ziegenhorn „seek“ höher, sonst Kamel tief.

### A12 (Konzept §4.4)
- **Nutzen:** Tiere umsiedeln, Herden aus fernen Biomen zur Farm holen und über Flüsse führen (in Vanilla mühsam: Leinen, Futter in der Hand,
  Boote). **Änderung:** zeitweises Folgen wie beim Locken mit Futter; ein Reittier, das mit Reiter schwimmt. **Eingriff:** gering – Zucht und KI
  der Tiere bleiben Vanilla (nur Navigation, kein Mixin), fremd gezähmte/angeleinte/benannte/in fremden Claims stehende Tiere bleiben, Obergrenze
  und Dauer per Config; Schwimmen ist der Vanilla-Mechanismus der Pferde ohne Abwurf.

### A14
Ruf 64 Blöcke hörbar, Ruf-Pose (Kopf hoch, Maul offen) + Noten-Ring am Kamm, Noten über jedem Folger alle 2 s, Abklingzeit im Sprungbalken
(5 s frei = Entlassen möglich).

### 2b. Items, Daten, Wiring
- Parasaurolophus-Rüstung Leder/Eisen/Diamant (8 Material um 1 Para-Eierschale, Tag `can_wear_parasaurolophus_armor`; Form Kammhaube +
  Rückendecke), Großer Sattel: Tag um `parasaurolophus`, `DinoKind.PARASAUROLOPHUS.entity()` → schlüpffähig.
- Tags: `simpledinos:herd_followers` (entity_type), `simpledinos:parasaurolophus_food` (item), `minecraft:can_float_while_ridden` (+para).
- Config: `callRadius` 24 (8–48), `callSeconds` 120 (30–600), `callMaxAnimals` 24 (1–64), `callCooldownSeconds` 20 (5–300), `callIgnoreNamed` true.
- Lang EN/DE (Entity, Items, Config, Handbuch), Wiki `manual.json` (Feature „parasaurolophus“, Notizen neue IDs, Eier/Ausrüstung/Config),
  Handbuch/Guide-Intro, Questionary, Discover, Kopf-Ausnahme `mobs.json` + `HeadTable.EXCLUDED`, Preise (ausgenommen), Testzentrale-Liste,
  Kreativtabs (Fabric + NeoForge), NeoForge/Forge-Registrierung analog, `check_data.py`, MODS-UND-FEATURES, Landing-Discover.

## 3. Texturen (A8c Runden, TEXTUR-REGEL)
- Parasaurolophus: 5 Felle × 4 Stufen (generiert, `gen_models.py`): heller Bauch, dunkler Rücken, senkrechte Schattenstreifen an den Flanken,
  Kamm mit kontrastierendem Band; **Kamm-Varianten A „dunkles Band, helle Kante“ (eingebaut), B „heller Kamm, dunkle Spitze“, C „Querringe“**
  (UI erst exemplarisch), Schnabel hornfarben, Rüstung (Kammhaube, Rückendecke) im selben Atlas.
- Items (`dino_items.py`): Parasaurolophus-Rüstung ×3. texrate grün; Runden in §6 und Vorschau.

## 4. Risiken
- Reiten rechnet der Client des Reiters (Schwimmen über Vanilla-Tag + Schwelle, Tempo = synchronisierte Werte); Ruf/Folgen nur der Server.
- Navigation fremder Tiere kann mit deren eigenen Zielen (Umherlaufen, Locken) wechseln → alle 20 Ticks neu gesetzt; kein Eingriff in ihre KI.
- Claim-Probe je Tier kann Meldungen der Claim-Mods auslösen → je Chunk und Ruf nur einmal, Ruf höchstens alle 20 s.
- Gametests nur als sbt-Befehl (sb-test nicht erreichbar); Client-Sicht nur per Software-Render.

## 5. Prüfung
Lokal: `precheck.py --fast`, alle `modules/*/tools/check_data.py`, `check_dup_impl.py`, `check_tooltip_text.py`, `wiki/generate.py --all --check`,
`check_missing_textures.py`, `layers.py check`, texrate je Item, `gen_models.py --check`, `gen_blocks.py --check`, Guides/Discover-Checks, javac gegen 26.3.
Gametests (neu): Para registriert/Werte/Hitbox, Teil-Boxen (Kopf an Wand/Spieler, Schwanz am Pfeiler), Zähmung (Schlüpfen → zahm + Eierschalen),
Futter/Heilen, Sattel/Rüstung/Reiten, Ruf nach Futter/leerer Hand/Nicht-Herdentier, Ausschlüsse (fremd gezähmt, Leine, Name + Config, Claim),
Obergrenze 24 (+ Config) nächste zuerst, Abklingzeit/Entlassen-Fenster/Hatchling, Folgen + Ende nach Zeit, Schwimmen mit Reiter (Tags, Tempo,
Steigen an die Oberfläche, Reiter über Wasser und bleibt sitzen), Flucht/Warnruf ohne Angriff. Angepasst: registered, config_caps,
dino_kind_site_weights. sbt-Befehl `/root/sb-tools-a/a-dinorest3-sbt.cmd`.

## 6. Runden (A8c)

| Runde | Was | Bewertung |
|---|---|---|
| Modell R1 | Rumpf leicht nach vorn geneigt, Hüftkamm, Hals 10 px schräg (-0,75), kleiner Kopf, Kamm 3 dünne Würfel, 3-gliedriger Schwanz | Hadrosaurier erkennbar, aber Hals dünn und flach (Kopf kaum über dem Rücken), Kopf klein, Kamm ein Stab, Flankenstreifen hart (Zebra-Wirkung) |
| Modell R2 | Hals 12 px + Halsansatz, steiler (-1,0), Kopf größer mit breitem Entenschnabel, Kamm 5/4,5/4 px dick, tiefer Schwanz, Streifen weicher (alle 6 px, schwächer) | liest sich als Parasaurolophus; Kamm-Spitze kommt beim Ruf an den Sattel (Reiter vorn) |
| Modell R3 (eingebaut) | Hüftkamm nach hinten, Sattel in die Rückenmitte (Reiter 0,05 hinter der Mitte), Schnabel mit eigener Hornrampe (Quantisierung machte ihn rotbraun), Einzieh- und Ruf-Pose so gewählt, dass der Kamm den Reiter nie schneidet | Felle hart 5,8–7,5 % (A8b ~7 %), texrate Atlas grün (2,7–4,5 %) |
| Kamm A/B/C | A dunkles Band + helle Kante (eingebaut), B heller Kamm mit dunkler Spitze, C Querringe (R1 von C: Ringe nur seitlich → auf allen Flächen, 2 px je 6 px) | alle 6,6–7,0 % hart; Wahl in der Vorschau |
| Items R1 | Kammhaube als schräge Stange über einer Decke | liest sich wie Schlitten/Rechen |
| Items R2 | Kopfform mit Kamm und Schnabel, Decke direkt darunter | wirkt wie eine sitzende Ente |
| Items R3 | schlanker langer Kamm, Decke getrennt, Schnabel in eigener Hautfarbe | Parasaurolophus-Kopf erkennbar; hart 19–27 % (Goldbesatz gegen Leder) |
| Items R4 (eingebaut) | Besatzton 35 % zum Grundton gezogen, Form von R3 | texrate grün, hart 6,7–10,1 % (Stego-Rüstung 4,5–11,3 %) |

Kamm-Muster A/B/C in der Vorschau (UI erst exemplarisch), A eingebaut (`gen_models.CREST_CHOSEN`).

## 7. Ergebnis und Abweichungen

- Umgesetzt wie §2/§3. **Abweichungen:** (1) Kern-Hitbox **2,2 × 2,5** statt 2,2 × 3,6 (Konzepthöhe mit Kopf; Rumpf 2,26 lang, Hüftkamm bis 2,48;
  Kopf/Kamm sind Teilboxen bis 3,24). (2) **Fester Kopf** wie Tri/Stego: der Halsansatz steht 0,3 vor der Kernbox, darum kann der Kopf nicht ganz
  einziehen; „Einziehen“ = Hals hoch, Kinn runter (Schnabel 2,57 → 1,88). (3) Herdentiere immer nur aus `#simpledinos:herd_followers`, auch mit
  Futter in der Hand (sonst kämen Wölfe, Katzen, Füchse mit); ein Gegenstand, den kein Tier frisst, ruft niemanden. (4) Ein neuer Ruf ersetzt die
  alte Herde (verlängert damit auch die Zeit). (5) In den ersten 5 s nach dem Ruf zeigt der Sprungbalken „frei“ (sonst könnte der Client keinen
  zweiten Druck senden, `LocalPlayer` sendet nur bei Abklingzeit 0); danach die restliche Abklingzeit. (6) Schwimmen über den Vanilla-Tag
  `minecraft:can_float_while_ridden` + `getFluidJumpThreshold` 1,3 statt eigener Physik; Tempo über Attribut `water_movement_efficiency` 1 + ×0,8
  im Wasser (rechnerisch ≈ 0,6 schwimmend, 0,8 watend). (7) Flucht = Vanilla-`PanicGoal` auch für Erwachsene (ohne Reiter), Warnruf bei jedem
  lebenden Angreifer außer dem eigenen Reiter. (8) Claim-Probe je Chunk und Ruf einmal (nicht je Tier). (9) Sattel/Reiter in der Rückenmitte statt
  über den Schultern (Kamm).
- Geprüft: javac gegen 26.3 (`-XDshould-stop.ifError=FLOW`, nur die bekannten Access-Widener-Meldungen in unveränderten Stellen), `precheck.py --fast`
  grün, alle `modules/*/tools/check_data.py`, `check_dup_impl`, `check_tooltip_text`, `check_missing_textures` (0 Fehler), `layers.py check`,
  `gen_models/gen_blocks --check`, `discover --check`, `module_guides --check`, `wiki/generate.py --all --check`, `simplemobheads generate_heads --check`,
  texrate; Skelett-Rechnung (Python-Spiegel) für Hitbox/Reichweite/Wand-, Pfeiler- und Spieler-Geometrie, Schwimm-Gleichgewicht (Simulation der
  Vanilla-Wasserphysik: Tiefe pendelt nach ~50 Ticks um 1,1–1,3).
- Gametests: 12 neue (`parasaurolophus_*`) + angepasst `registered`, `config_caps`, `dino_kind_site_weights`. Die Ruf-Tests rufen mit Radius 8, damit
  keine Tiere benachbarter Test-Strukturen (5 Blöcke Abstand im GameTest-Raster) mitgezählt werden.
- **Nicht geprüft:** Gametests (sb-test nicht erreichbar, Befehl `/root/sb-tools-a/a-dinorest3-sbt.cmd`), Client-Sicht (Modell, Ruf-Pose, Noten,
  Sprungbalken im Entlass-Fenster, Schwimmen mit echtem Client, Folgen über längere Strecken/durch Flüsse), NeoForge/Forge (nur Registrierung analog,
  nicht kompiliert), Klänge (Vanilla-Platzhalter bis R14).
