# Plan A-DINOREST2: DINO-REST Schritt 2 – Stegosaurus + Dino-Pflug

Stand 12.11.2026, Agent a-dinorest2, Branch `a-dinorest2` (Basis `hub/b-bundle190` d91f28407, enthält a-dinorest1). Nur MC 26.3 Fabric.
Auftrag: `/root/sb-tools-a/agenten-1530.md` §18. Konzept: `docs/ai/KONZEPT-A-DINO36-2026-11-12.md` §4.2 (DINO36 angenommen, Empfehlungen gelten),
Muster: `docs/ai/PLAN-A-DINOREST1-2026-11-12.md` (Triceratops), Teil-Hitboxen `docs/ai/PLAN-A-DINO36B-2026-11-12.md`. Vorschau
`/root/sb-tools-a/previews/a-dinorest2-preview.html`, Antwortcode **DINOREST2** (TEXTUR-REGEL: vorher/nachher je Textur, ok/Einwand je Textur).

## 1. Ist-Stand (gelesen)

- a-dinorest1 liefert den Rahmen: `Dino#kind()`, `usesJumpAbility()` + `ABILITY_COOLDOWN` (Server zählt, Sprungbalken zeigt), `onPlayerJump`/
  `executeRidersJump` (Reiter-Seite) + `handleStartJump` (Server), `isFood`/`isFavoriteFood`/`healFor`, `DinoKind.entity()`, Eierschalen
  (`EGGSHELLS`, Stego-Schale existiert schon samt Textur/Rezept), Rüstung je Art über `armor(material,key,tag)`, `PartRig(..., solidHead)`,
  `Dino#move`/`clipHead`/`pushEntities` (fester Kopf), Generator `tools/gen_models.py` (`<art>_spec` → Textur, `<Art>Layers`, `<Art>Bones`),
  Item-Sprites `tools/dino_items.py` (optik32_engine), Fundort-Gewichtung (Badlands Stego ×1,5) wirkt, sobald die Art schlüpft.
- Tests prüfen heute „nur T. rex + Triceratops schlüpfen“ (`registered`, `egg_other_kinds_wait_for_their_dinosaur` nutzt das Stego-Ei,
  `dino_kind_site_weights` rechnet 25:10) → beim Bau anpassen.
- Vanilla 26.3 (Quelle `/root/mcsrc263np`, Signaturen per `javap` gegen `minecraft-client.jar`): Hacken gibt es nur noch als Daten-Transformer
  `BlockTransformers.HOE` (Registry `Registries.BLOCK_TRANSFORMER`; Regeln: `#turns_into_farmland` + Luft darüber → Ackerboden, grobe Erde + Luft →
  Erde, Wurzelerde → Erde + Beute `till_rooted_dirt`, Klang `HOE_TILL`); `BlockStateProvider#getOptionalState(level, random, pos)` wertet die
  Regeln aus. `FarmlandBlock#fallOn` zertrampelt (Zufall < Fallhöhe − 0,5, große Wesen) → wird über `checkFallDamage` abgefangen.
  `ItemStack#hurtAndBreak(int, ServerLevel, ServerPlayer|null, Consumer<ItemStack>)` (Haltbarkeit inkl. Haltbarkeit-Verzauberung).
- Rechte: SimpleLib `InteractionGuard.permission` (Claim-Probe) setzen heute nur Simple Tools/Farming/Interfaces; allein gespielt bliebe sie „immer
  erlaubt“ → Simple Dinos installiert dieselbe Probe (Fabric `PlayerBlockBreakEvents.BEFORE`, NeoForge `BreakBlockEvent`), gleiche Semantik.

## 2. Umfang

### 2a. Stegosaurus (Konzept §4.2, Tabelle §4)
- Entity `simpledinos:stegosaurus` (`Stegosaurus extends Dino`): 90 LP, Tempo 0,17 (Pflug gesenkt 0,12), kein Sprung, Stufe 1,0, Rückstoßfest 0,7,
  Fallschaden 0,6; Modell in 75 % wie T. rex/Triceratops, Kern-Hitbox am Modell bestimmt (Konzept 2,4 × 3,5).
- Teil-Hitboxen: Hals/Kopf (klein, tief) und Schwanz (3 Glieder + Thagomizer-Stacheln, die mitbiegen, aber selbst nicht gebogen werden →
  `PartRig` bekommt optionale „Schwanz-Anhänge“). Fester Kopf nur, wenn das Einziehen nicht reicht (am Modell gerechnet).
- Futter `#simpledinos:stegosaurus_food`: Farn, Großer Farn (Liebling), Weizen, Heuballen, Süßbeeren.
- Abwehr „Stachelschwanz“: trifft ein Feind (`Enemy`, nie Spieler) den erwachsenen Stego im Nahkampf, bekommt er 2 Schaden (Dornen-Quelle), Schwanzschlag
  sichtbar + Ton. Keine Angriffsziele.
- Zertrampelt nie Ackerboden (`checkFallDamage` auf Ackerboden: Fallschaden wie sonst, aber kein `fallOn`).
- Klänge bis zur Hörabnahme (R14): Schnüffler tief.

### 2b. Dino-Pflug (Konzept §4.2/§6)
- Item `simpledinos:dino_plow`: Rezept `I K I` / `H H H` (Eisenbarren, Eisenkette, Eisenhacke), Haltbarkeit 750, Reparatur Eisenbarren (Amboss),
  verzauberbar (Tag `minecraft:enchantable/durability` → Haltbarkeit, Reparatur). Tooltip 1 Zeile (Bedienung nicht offensichtlich).
- Anlegen: Rechtsklick mit dem Pflug auf den eigenen erwachsenen Stegosaurus (wie die Truhe am Esel); Abnehmen: Schere (Vanilla-Regel wie
  Sattel/Rüstung, Pflug zuerst); fällt beim Tod. Gespeichert als ItemStack der Entität, Sichtbarkeit synchronisiert.
- Sprungtaste (`usesJumpAbility`): senkt/hebt den Pflug (Server entscheidet, 10 Ticks Sperre im Sprungbalken), Stegosaurus springt nie.
- Pflügen (Server, nur gesenkt, nur erwachsen, nur mit Reiter): beim Betreten einer neuen Blockposition 3 Felder quer hinter den Hüften
  (`stegosaurusPlowWidth` 1–3) über den Vanilla-Hacken-Transformer; nur wenn der Reiter dort bauen darf: nicht Zuschauer, `mayBuild`,
  `mayInteract` (Spawnschutz, Weltgrenze), `mayUseItemAt`, `InteractionGuard.permission` (Claims; nach Ablehnung 1 s Pause gegen Meldungs-Spam).
  −1 Haltbarkeit je Feld; bricht er, ist er weg (Bruch-Ton). Erd-Partikel je Feld, Hacken-Ton gedrosselt.
- Säen (`stegosaurusSow`): Saat in der Nebenhand des Reiters (`#simpledinos:plow_seeds`: Weizen-, Rote-Bete-, Melonen-, Kürbis-, Fackellilien-Samen,
  Kannenpflanzen-Kapsel, Karotte, Kartoffel) → je Ackerfeld des Streifens mit Luft darüber 1 Saat (nur wenn die Pflanze dort stehen kann;
  Kreativ verbraucht nichts).
- Tempo gesenkt 0,12 (×0,12/0,17 auf das Reittempo, Reiter-Seite).

### A12 (Konzept §4.2)
- **Nutzen:** große Felder schnell anlegen und besäen (Ackerbau ist Vanilla-Kern). **Änderung:** die Hacke in 3er-Breite vom Reittier aus, Säen im
  Vorbeigehen. **Eingriff:** gering – nur die Vanilla-Hackenregel, nur mit Reiter, Pflug und Baurecht, Haltbarkeit wie ein Werkzeug; abschaltbar
  über `stegosaurusPlowWidth` 1 (eine Bahn) und `stegosaurusSow`. Stachelschwanz: Abwehr wie Dornen, nur gegen Monster.

### 2c. Items, Daten, Wiring
- Stegosaurus-Rüstung Leder/Eisen/Diamant (8 Material um 1 Stegosaurus-Eierschale, Tag `can_wear_stegosaurus_armor`; Form Hals- und Flankenplatten,
  Rückenplatten frei); Großer Sattel: Tag um `stegosaurus` erweitert. `DinoKind.STEGOSAURUS.entity()` → schlüpffähig.
- Config: `stegosaurusPlowWidth` 3 (1–3), `stegosaurusSow` true. Lang EN/DE (Entity, Items, Config, Handbuch), Wiki `manual.json` (Feature
  „stegosaurus“, Notizen neue IDs, Config), Handbuch/Guide-Intro, Questionary, Discover, Kopf-Ausnahme `mobs.json`, Preise (ausgenommen),
  Testzentrale-Liste, Kreativtabs (Fabric + NeoForge), NeoForge/Forge-Registrierung analog, `check_data.py`, MODS-UND-FEATURES.

## 3. Texturen (A8c Runden, TEXTUR-REGEL)
- Stegosaurus: 5 Felle × 4 Stufen (generiert, `gen_models.py`), Artmuster: dunkler Rücken, helle Längsflecken an den Flanken, Platten mit Verlauf;
  **Platten-Varianten A „Verlauf ocker-rot zur Spitze“ (eingebaut), B „dunkler Rand“, C „Querbänder“** (UI erst exemplarisch), Stacheln hornfarben,
  Pflug Holz + Eisen im selben Atlas.
- Items (`dino_items.py`): Dino-Pflug, Stegosaurus-Rüstung ×3. texrate grün; Runden in §6 und Vorschau.

## 4. Risiken
- Reiten rechnet der Client (Tempo, Pflug gesenkt = synchronisierter Wert), Wirkung (Pflügen/Säen/Haltbarkeit) nur der Server.
- Claim-Probe auf jedem Feld kann bei Claim-Mods Meldungen auslösen → Probe nur für umwandelbare Felder, nach Ablehnung 1 s Pause.
- Gametests nur als sbt-Befehl (sb-test nicht erreichbar); Client-Sicht nur per Software-Render.

## 5. Prüfung
Lokal: `precheck.py --fast`, alle `modules/*/tools/check_data.py`, `check_dup_impl.py`, `check_tooltip_text.py`, `wiki/generate.py --all --check`,
`check_missing_textures.py`, `layers.py check`, texrate je Item, `gen_models.py --check`, `gen_blocks.py --check`, Guides/Discover-Checks, javac gegen 26.3.
Gametests (neu): Stego registriert/Werte/Hitbox, Teil-Boxen (Kopf an Wand, Schwanz/Stacheln am Pfeiler), Zähmung (Schlüpfen → zahm + Eierschalen),
Futter/Heilen, Sattel/Rüstung/Reiten, Pflug anlegen/abnehmen/Tod, Sprungtaste senkt/hebt (Server, Sperre), Pflügen 3 breit nur Umwandelbares mit Luft,
grobe Erde/Wurzelerde, Baurecht (Abenteuer-Modus, Claim-Veto) → nichts, Breite aus Config, Haltbarkeit, Säen aus der Nebenhand/aus, kein Zertrampeln,
Stachelschwanz (Monster 2 Schaden, Spieler nie). sbt-Befehl `/root/sb-tools-a/a-dinorest2-sbt.cmd`.

## 6. Runden (A8c)

| Runde | Was | Bewertung |
|---|---|---|
| Modell R1 | Rumpf-Kiste (leicht geneigt), 2 Plattenreihen 5–13 px, kleiner Kopf am schrägen Hals, Thagomizer, Pflug | erkennbar, aber Platten wie Ziegel (zu klein/flach), Verlauf je Teilbox (alles rot), Rumpf ohne Rückenbogen |
| Modell R2 | Platten bis 18 px, Sockel + Spitze mit durchgehendem Verlauf (`t0..t1`), Rücken stärker zur Hüfte geneigt + höherer Buckel, schlankerer Kopf/Hals, größere Stacheln | liest sich als Stegosaurus; Krallen als helle „Socken“ |
| Modell R3 (eingebaut) | Krallen gedämpft, Hüftplatten 16/15 px (Kern 3,7 statt 3,8), Pflug (Gurt, Deichseln, Balken, 3 Schare 1 Block auseinander) und Rüstung (Hals, Flanken) geprüft | Felle hart 4,5–5,7 % (A8b ~7 %), texrate Atlas grün (2,5–3,2 %) |
| Items R1 | Rüstung Seitenansicht mit Zinnen-Lücken + Gurten, Pflug Frontansicht (Ring, Balken, 3 Schare) | Rüstung wirkt wie ein Tier mit Beinen; Pflug lesbar, Balken flach |
| Items R2 | Rüstung oben offen, eigene Platten (Ockerrot) ragen durch, keine Gurte; Pflug mit Kettengliedern und dunkler Balkenkante | Stegosaurus erkennbar, Halsschutz nur ein Fleck; Pflug orange (24-px-Fläche), Eisen hart 13,3 % |
| Items R3 (eingebaut) | Halsschutz als Band, dunkle Kante unter den Platten, Plattenton heller; Balken-Maserung in 2-px-Gruppen | texrate alle grün: Pflug big 6/ge6 7 %/hart 5,8 %, Rüstung hart 4,5–11,3 % (Vanilla-Vorbilder Pferderüstung 26–39 %, Eisenhacke 65 %, Sattel 34 %) |

Platten-Muster A/B/C in der Vorschau (UI erst exemplarisch), A eingebaut (`gen_models.PLATE_CHOSEN`).

## 7. Ergebnis und Abweichungen

- Umgesetzt wie §2/§3. **Abweichungen:** (1) Kern-Hitbox **2,6 × 3,7** statt 2,4 × 3,5 (Rumpf 2,9 lang, Hüftplatten bis 3,66; Modell 75 % wie T. rex/Tri).
  (2) „Kopf einziehen“ heißt beim Stegosaurus **Kopf senken und zurückziehen** (Grasen: Hals +0,9, Kopf −0,5, Schnauze 2,55 → 1,77); der Kopf ist
  trotzdem fest (bleibt vor den Schultern). (3) `PartRig` hat jetzt optionale **Schwanz-Anhänge** (Stacheln zählen zur Schwanz-Box, biegen mit, werden
  selbst nicht gebogen). (4) Säen auch auf schon gepflügten, leeren Ackerboden im Streifen (zweiter Durchgang mit Saat), Haltbarkeit nur beim Pflügen.
  (5) Simple Dinos installiert die Claim-Probe von SimpleLib selbst (Fabric/NeoForge/Forge, gleiche Semantik wie Simple Tools), damit der Pflug auch
  allein gespielt Claims achtet; nach einer Ablehnung 1 s Pause (Meldungs-Spam). (6) Abwehr trifft `Enemy` (Monster, Schleime, Hoglins …), nie Spieler.
  (7) Pflug-Rezept schaltet sich mit der Stegosaurus-Eierschale frei (wie die Rüstung). (8) Reiter sitzt 0,95 Blöcke vor der Mitte (Sattel über den
  Schultern vor den Platten), Passenger-Punkt mit z-Versatz.
- Geprüft: javac gegen 26.3 (`-XDshould-stop.ifError=FLOW`, nur die bekannten Access-Widener-Meldungen in unveränderten Stellen), `precheck.py --fast`
  grün, alle `modules/*/tools/check_data.py`, `check_dup_impl`, `check_tooltip_text`, `check_missing_textures` (0 Fehler), `layers.py check`,
  `gen_models/gen_blocks --check`, `discover --check`, `module_guides --check`, `wiki/generate.py --all --check`, texrate; Skelett-Rechnung (Python-Spiegel)
  für Hitbox/Reichweite/Pfeiler- und Spieler-Geometrie der Gametests.
- **Nicht geprüft:** 14 neue Gametests + angepasste (sb-test nicht erreichbar, Befehl `/root/sb-tools-a/a-dinorest2-sbt.cmd`), Client-Sicht (Modell,
  Pflug gesenkt/gehoben, Sprungbalken, Tempo mit Pflug, Schwanzschlag), NeoForge/Forge (nur Registrierung/Probe analog, nicht kompiliert).
