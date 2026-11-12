# Plan a-audsb31e (A, 12.11.2026) – Code-Audit Gruppe E (optionale SB-Sub-Mods ohne AUDIT.md)

Quelle: `/root/sb-tools-a/agenten-1530.md` Abschnitt 11. Zwei Branches: **e1** (dieser Abschnitt, Branch `a-audsb31e1`)
und e2 (`a-audsb31e2`, eigener Abschnitt). Basis `hub/b-bundle188` 9dfb3f643 (enthaelt Shop-Musik, Bogenportale,
Chunk-Loader). Nur MC 26.3 Fabric. Vorbild: `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`, `docs/ai/PLAN-B-AUDSB31A-2026-11-11.md`,
`modules/{simplepads,simplegadgets,simplestorage}/AUDIT.md`.

## e1 – Ist-Stand
- Mods (keine `AUDIT.md`): simplemoney (1,5k Zeilen ohne Tests), simpleriding (2,0k), simplefun (3,5k), simpletweaks (13,9k),
  simplesandwiches (3,2k), simplemaps (8,2k), simpletrims (3,1k), simplequestionary (2,0k), simpleshops (6,9k inkl. Shop-Musik
  aus a-shopmusic37), simplewanderers (1,5k) – zusammen ~46k Zeilen ohne Tests/Forge/NeoForge.
- Zusatzeingabe BEW39 (`/root/sb-tools-a/bewertung39/<mod>.json`), rote Eintraege und Einordnung:
  | Mod | BEW39 rot | Einordnung e1 |
  |---|---|---|
  | simplewanderers | Vanilla-Handelslisten des fahrenden Haendlers ohne Bedingung ueberschrieben (auch wenn Mod/Schalter aus) | **echter Fehler → fixen** |
  | simplesandwiches | Handbuch/Wiki beschreiben die alte Naehrwert-Formel (ohne Deckel) | **Doku-Fehler → fixen** |
  | simplemoney | Preistabellen im Handbuch veraltet (6 mit 0 Preisen, 8 fehlen) | **Doku-Fehler → fixen, wenn Generator vorhanden** |
  | simplemoney | Papierraketen-Rezept 7 Papier + 2 Pulver → 1 Rakete | Geschmack/Balance → Besitzer-Frage |
  | simplefun | Knockback bis V ueberschreibt `minecraft:enchantment/knockback` ohne Bedingung | A12-Eingriff → Besitzer-Frage |
  | simplemaps | Kontextmenue/Slot-Wahl im Client unbelegt | Client-Probe noetig → TODO (sb-test) |
  | simplequestionary | Text-Abdeckung (Inhalt fehlt) | neuer Inhalt = „missing“ → nicht bauen, nur notieren |
  | simpleshops | Shop-Musik fehlte | in Basis b-bundle188 enthalten → mit auditieren |
- Orange Metadaten-Befund (mehrere Mods): `modules.json` `requires: [cloth_config]`, obwohl `fabric.mod.json` seit b-cloth26
  nur SimpleLib verlangt → pruefen, ggf. korrigieren + `modtree.py`.

## e1 – Vorgehen
1. Risikogeleitet je Mod wie Gruppen A–D (C1–C9-Liste aus `/root/sb-tools-a/audsb31/brief.md`): C2S-Payloads + Handler,
   Menues (quickMoveStack/clickMenuButton/stillValid/Slots), BlockEntities, Items mit Inhalt, Befehle, Mixins, Schutz,
   statische Maps, Tick-Kosten; dazu die BEW39-Eintraege oben.
2. Lesen parallel durch 4 Unteragenten (max. 4 gleichzeitig, kein Gradle, javac nur seriell unter `flock`):
   E1a simpletweaks · E1b simplemaps + simplequestionary · E1c simpleshops + simplemoney ·
   E1d simplefun + simplesandwiches + simpletrims + simpleriding. simplewanderers + Metadaten + Querschnitt mache ich selbst.
   Jeder P1/P2-Befund wird von mir an der Code-Stelle nachgeprueft, bevor er als gefixt gilt.
3. Befunde je Mod in `modules/<mod>/AUDIT.md` (gelesen, C1–C9, Befunde, Urteil, Test).
4. Kleine sichere Fixes mit Gametest (Katalog + `@GameTest` im Fabric-Adapter); Mechanik/Balance/Geschmack = Besitzer-Frage
   (Vorschau `/root/sb-tools-a/previews/a-audsb31e1-preview.html`, Antwortcode **AUDSB31E1**, je Frage Empfehlung + Optionen).
   Neue Inhalte aus BEW39 „missing“ werden NICHT gebaut (BEW39-Antwort steht aus).
5. Je Mod ein Commit (AUDIT.md + Fixes + Tests).
6. Pruefen: javac der geaenderten Dateien (`/root/sb-tools-a/audsb31e1/javac.sh`), `precheck.py --fast`, alle `check_data.py`,
   `check_dup_impl`, `check_tooltip_text`, `wiki/generate.py --all --check`, `check_missing_textures`, `layers.py check`.
   sb-test nicht erreichbar → sbt-Befehl als Datei `/root/sb-tools-a/a-audsb31e1-sbt.cmd` + TODO; antwortet sb-test wieder
   (`timeout 10 ssh sb-test true`), EIN gezielter Lauf ueber `/root/sbt`.
7. Push nur `git push /root/sb.git a-audsb31e1`.

## e1 – Risiken
- Vanilla-Overrides mit Fabric-Ladebedingung: faellt die Bedingung, verschwindet bei dynamischen Registern evtl. der ganze
  Eintrag (kein Rueckfall auf Vanilla) → Wanderer-Fix nur mit belegtem Rueckfall-Weg.
- Kern-Dubletten (`check_dup_impl`): Fix nur im Modul, Drift notieren.
- Wenig RAM auf sb-agent (4 GB, Swap voll): javac nur seriell, keine Repo-Kopien.

## e1 – Ergebnis
| Mod | P1 | P2 | P3 | Fixes (Gametest) |
|---|---|---|---|---|
| Wanderers | 0 | 1 (gefixt) | 5 (1 gefixt) | Vanilla-Haendler-Sets nicht mehr ueberschrieben, Haelfte nur bei `on()` per Mixin (`trader_split_follows_the_switch`) |
| Tweaks | 0 | 5 (2 gefixt, F2–F4) | 12 | Backup-Aufraeumen trifft keine fremde Welt (`backup_prune_keeps_other_worlds`), `/lag` nur Server-Thread (`lag_finder_ignores_other_threads`); F1 Standardwerte |
| Shops | 0 | 2 (1 gefixt, F5) | 7 (1 gefixt) | Vitrine faerben ueber den echten Klickweg (`display_tint_through_game_mode`), Beschreibung war Simple Beacons |
| Money | 0 | 0 | 8 (2 gefixt) | Handbuch-Preistabellen (BEW39 rot) und Config-Eintrag; F6 Raketenrezept, F7 Balance |
| Maps | **1 (gefixt)** | 4 (2 gefixt, F8, F9) | 11 (2 gefixt) | `Integer.MIN_VALUE`-Rahmen liess Clients abstuerzen (`coords_min_value_refused`), Token bei Live-Freigabe (`web_http_public_bind_token`), URL-kodiertes Token (`web_http_token_url_encoded`), `layers_unaligned_area`; F10 |
| Questionary | 0 | 0 | 6 (1 gefixt) | Schreibweise; Text-Abdeckung = BEW39 missing, nicht gebaut |
| Fun | 0 | 3 (2 gefixt, F11) | 8 | Wasserbombe/Ente achten auf Schutz (`toy_water_balloon_protection`, `toy_rubber_duck_protection`); F12 |
| Sandwiches | 0 | 2 (gefixt) | 7 | Handbuch-Formel (BEW39 rot), `depends simplelib`; F13, F14 |
| Trims | 0 | 2 (gefixt) | 7 | Licht nur in reinem Wasser (`radiance_light_spares_waterlogged_blocks`), Vorlage prueft Zielblock (`placing_checks_the_placed_block`); F15 |
| Riding | 0 | 1 (gefixt) | 6 | `depends simplelib`; F16, F17 |

Summe: 1 P1 (gefixt), 20 P2 (13 gefixt, 7 als Frage), 77 P3. Besitzer-Fragen F1–F17: Vorschau
`/root/sb-tools-a/previews/a-audsb31e1-preview.html`, Antwortcode **AUDSB31E1** (die AUDIT.md-Dateien nummerieren ihre Fragen je Mod).
Querschnitt: `modules.json` `requires` = simplelib fuer Money/Riding/Fun/Sandwiches/Wanderers (statt `cloth_config` bzw. leer), Modtree neu.

## e1 – Abweichungen
- Wie Gruppen A–D risikogeleitet (4 Unteragenten, Wanderers selbst); nicht Gelesenes steht je AUDIT.md.
- Wanderers-Fix nicht ueber eine Fabric-Ladebedingung (wuerde den Vanilla-Eintrag ganz verwerfen), sondern eigenes Set + Mixin;
  Mixin-Konfig auch in NeoForge-TOML und Forge-`forgeMixinConfigs` eingetragen, dort aber weder kompiliert noch getestet.
- `fabric.mod.json` von Riding/Sandwiches: `depends simplelib` ergaenzt (Absturz ohne SimpleLib); NeoForge/Forge-Metadaten nur TODO.
- Zusatz ausserhalb der Mods: `docs/ai/RELEASE-SIMPLEBUILDING.md` 4.4 (Maps-Probe nie gelaufen).
- Wiki: nur die Daten der geaenderten Handbuecher committet; `simpleblocks`/`simpleoctets`/`textures.js`/`simplebuilding.json`
  sind schon auf der Basis veraltet (pal35 bzw. lokale Pfade) und bleiben fuer das Buendel-gen.

## e1 – Test
Lokal: javac aller geaenderten Dateien (nur bekannte Access-Widener-Meldungen in nicht geaenderten Dateien), alle `check_data.py` gruen,
`check_dup_impl` 0 Probleme, `check_tooltip_text` gruen, `multimod.py` gueltig, `check_missing_textures` 0 Fehler, `layers.py check` 0 Probleme,
`precheck --fast` gruen, `wiki/generate.py --all --check` rot nur durch Basis-Altlasten (siehe Abweichungen).
**sbt offen** (sb-test „No route to host“): Befehl `/root/sb-tools-a/a-audsb31e1-sbt.cmd` – Compile der 8 Mods mit Code-/Metadaten-Aenderung,
`module-<mod>-fabric-263` + standalone fuer Wanderers, Tweaks, Shops, Maps, Fun, Trims, standalone fuer Riding, Sandwiches, Money, dazu data_integrity.
Nicht getestet: Gametests selbst (kein Lauf), Client (Kartenrahmen, Vitrine), NeoForge/Forge.

---

## Plan a-audsb31e2 (A, 12.11.2026) – Code-Audit Gruppe E, Liste e2 (optionale SB-Sub-Mods ohne AUDIT.md)

Quelle: `/root/sb-tools-a/agenten-1530.md` Abschnitt 11. Basis `hub/b-bundle188` 9dfb3f643 (enthaelt Shop-Musik, Bogenportale,
Chunk-Loader). Nur MC 26.3 Fabric. Parallel arbeitet a-audsb31e1 (Liste e1) in einer eigenen Kopie dieser Datei – den
Konflikt beim Buendeln loest A (Abschnitte e1/e2 nebeneinander behalten).
Vorlage: `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`, `docs/ai/PLAN-B-AUDSB31A-2026-11-11.md`, `modules/{simplemobheads,simplestorage}/AUDIT.md`.

## e2 – Ist-Stand
- Keine `AUDIT.md` in den 11 Mods: simpledimensions, simpleend, simplerespawn, simplebeacons, simplemusic, simpleearth,
  simplechallenges, simpleanimals, simpledinos, simplelayers, simplevault.
- Umfang ohne Tests/Forge/NeoForge (Java-Zeilen): Music 8,3k · Layers 6,0k · Challenges 5,8k · Earth 5,3k · End 3,7k ·
  Dimensions 3,6k · Vault 3,3k · Beacons 3,3k · Animals 2,8k · Dinos 2,2k · Respawn 1,8k = **46k** (+ ~13k Tests).
- Zusatz-Eingabe BEW39 (`/root/sb-tools-a/bewertung39/<mod>.json`, Auszug rot/orange `/root/sb-tools-a/audsb31e2/bew39-e2.md`).
  Darin als echte Fehler-Kandidaten: Respawn `otherPlayers()` zaehlt Spieler anderer Dimensionen als Helfer; Dimensions
  Fremd-Claim-Mods sperren alle Portale (fail-closed, `suggests` trotzdem); Metadaten-Widersprueche `fabric.mod.json` vs.
  `modules.json` (Dimensions, End); veraltete Texte (MODSTRUKTUR Beacons „6 pyramid levels“, Earth „activation height“,
  Layers „below the bedrock“); Vault ohne `configCommand`-Gametest. Platzhalter-Inhalte (Beacons Maechtig, Dinos-Eier,
  Layers-StubBands) und alles aus „missing“ sind **Inhalte/Features → nicht bauen** (Besitzer-Antwort BEW39 offen).
- simplelayers und simplevault sind pausiert: nur Fehler-Audit.
- sb-test zurzeit nicht erreichbar.

## e2 – Vorgehen
1. Risikogeleitet wie Gruppen A–D (C1–C9, P1–P3), je Mod die Risiko-Dateien vollstaendig lesen.
2. Lesen parallel durch 5 Unteragenten (sonnet, Brief `/root/sb-tools-a/audsb31e2/brief.md`, kein Gradle/git, javac nur seriell
   per `flock` ueber `/root/sb-tools-a/audsb31e2/javac.sh`):
   E2a Dimensions + Respawn · E2b Beacons + Animals + Dinos · E2c Music · E2d End + Challenges · E2e Earth + Layers + Vault.
   Jeden P1/P2-Befund pruefe ich selbst an der Code-Stelle nach, bevor er als gefixt gilt.
3. Befunde je Mod in `modules/<mod>/AUDIT.md`; kleine sichere Fixes direkt mit Gametest (Katalog `<Mod>Tests.ALL` +
   `@GameTest` im Fabric-Adapter); Spielmechanik/Balance = Besitzer-Frage (Vorschau
   `/root/sb-tools-a/previews/a-audsb31e2-preview.html`, Antwortcode **AUDSB31E2**, je Frage Empfehlung + Optionen).
4. Metadaten/Doku ausserhalb der Module (modules.json, MODSTRUKTUR, PRINZIPIEN-Texte) mache ich selbst, nur Korrektur von
   Falschem, keine neuen Regeln.
5. Je Mod ein Commit (AUDIT.md + Fixes + Tests), dann ein Commit fuer Plan/Querschnitt.
6. Pruefen lokal: javac der geaenderten Dateien, `precheck.py --fast`, alle `modules/*/tools/check_data.py`, `check_dup_impl`,
   `check_tooltip_text`, `check_gametest_adapters`, `wiki/generate.py --all --check`, `check_missing_textures`, `layers.py check`.
   sbt-Befehl als Datei `/root/sb-tools-a/a-audsb31e2-sbt.cmd` + TODO; wenn `timeout 10 ssh sb-test true` klappt, EIN Lauf.

## e2 – Risiken
- Unteragenten-Befunde koennen falsch sein → P1/P2 nur nach eigener Nachpruefung uebernehmen.
- Kern-Dubletten (`tools/dup_pairs.txt`): Fix nur im Modul, Drift vermerken.
- Viele parallele A-Agenten (e1, portal, livemap …): javac nur unter der gemeinsamen Sperre, sb-test nur einmal am Ende.
- Ohne sbt sind neue Gametests nur typgeprueft, nicht gelaufen (ehrlich so berichten).

## e2 – Ergebnis
| Mod | P1 | P2 | P3 | Fixes (Gametest) |
|---|---|---|---|---|
| Music | **1 (gefixt)** | 2 (1 gefixt, F8) | 14 (F9) | Notencode-Verschachtelung max. 16 (`note_code_nesting_limit`; vorher StackOverflow im Server-Thread = Absturz, auch beim Neustart ueber die Plattenpresse), .nbs-Import passt ins Paketlimit (`nbs_import_fits_limit`) |
| Dimensions | 0 | 3 (2 gefixt, F3) | 10 (F4, F5) | Portal-Ledger vergisst verschwundene Portale (`portal_records_follow_portals`), defekte `server.json` -> Quarantaene statt Absturz (`settings_broken_file`) |
| Respawn | 0 | 2 (1 gefixt, F1) | 8 (F2) | Helfer nur in derselben Dimension (`helper_in_other_dimension_does_not_count`) |
| Challenges | 0 | 4 (3 gefixt, F6) | 9 | gesperrte Zellen: kein Lucky-Wurf (`lucky_in_locked_cell_gives_nothing`), keine Drops (`locked_cells_drop_nothing`, neuer Mixin `LockedDropsMixin`), Kolben (`pistons_leave_locked_cells_alone`, neuer Mixin `PistonStructureResolverMixin`) |
| Beacons | 0 | 1 (gefixt) | 7 (F7) | Aufwertung fragt Claims/Partner-Rechte (`upgrade_respects_claims`) |
| Dinos | 0 | 2 (gefixt) | 7 (F12) | Biss folgt Config (`trex_bite_follows_config`), verteidigt nur gegen jagende Monster (`trex_defends_only_hunted`) |
| Earth | 0 | 1 (gefixt) | 6 | Sampler-Stopp gibt Plaetze zurueck (`tile_service_stop_releases_slots`) |
| End | 0 | 1 (gefixt) | 11 | Dragon-Fight-Schalter folgt `/simpleend config` sofort (`nestedSwitchFollowsConfigCommand`, nur Fabric-Adapter) |
| Animals | 0 | 0 | 5 | – |
| Layers (pausiert) | 0 | 2 (1 gefixt, F10) | 8 | Waende halten serverseitig auch ohne Effekt (`backroom_walls_hold_without_effect`), `config_command` |
| Vault (pausiert) | 0 | 2 (1 gefixt, F11) | 6 | Fries haelt serverseitig auch ohne Effekt (`vault_frieze_holds_without_effect`), `config_command` |

Querschnitt (Hauptagent): `modules.json` requires `simplelib` fuer Dimensions/End/Animals/Dinos/Respawn (= `fabric.mod.json` depends,
Respawn-`depends simplelib >=0.4.0` nachgetragen), Beschreibungen Beacons (5 Ebenen, Maechtig/Boese), Earth (Renderhoehe statt
„activation height“), Layers (eigene Dimension, zehn Ebenen) -> MODSTRUKTUR + Landing-Paketseite neu, PRINZIPIEN-Kurzzeile Layers,
`simplelayers/fabric.mod.json`-Beschreibung. `cloth_config` in Dimensions-`requires` bleibt (wie Fun/Money/Riding nach b-cloth26, NeoForge verlangt es).
Besitzer-Fragen F1–F12: `/root/sb-tools-a/previews/a-audsb31e2-preview.html`, Antwortcode **AUDSB31E2**.

## e2 – Abweichungen
- Risikogeleitet wie Gruppen A–D, kein Vollaudit Zeile fuer Zeile (46k Zeilen); nicht Gelesenes je AUDIT.md unter „Nicht gelesen“.
- Zusaetzliche P2-Fixes ausserhalb der BEW39-Liste (Dimensions-Ledger/`server.json`, Challenges-Grenze, Music-Parser): klein, Datenverlust/Absturz/Dupe, je mit Gametest.
- Challenges: zwei neue Mixins (Wirkung nur mit gewaehlter Challenge, `Gate`) – stellen das im Handbuch beschriebene Verhalten her; im Gate besonders pruefen (Mixin-Anwendung).
- End-Gametest nur im Fabric-Adapter (braucht SimpleLib-`ConfigCommands`), nicht im Katalog.
- Britisches Englisch in Earth/Layers/Vault/Music/End/Respawn-Texten (colour, centre, grey) nicht umgestellt (P3, eigener Pflegegang; generierte Guide-Zeilen kommen aus `tools/guides/module_guides.py`).
- Nicht angefasst: `tools/dup_pairs.txt` (Layers-Vorschlag: Paar `BlockStateBaseMixin` streichen, nur Namensgleichheit), Beacons-`optional simplemobs` (braucht NeoForge/Forge-toml).

## e2 – Test (lokal)
javac aller geaenderten Dateien je Mod (nur bekannte Access-Widener-Meldungen in ungeaenderten Dateien), `precheck --fast` gruen,
alle `check_data.py` gruen, `check_dup_impl` 0 Probleme, `check_tooltip_text`, `check_lang_keys`, `check_codec_lists` gruen,
`check_gametest_adapters` nur die bekannte Kern-Luecke der Basis, `check_missing_textures` 0 Fehler, `layers.py check` 0 Probleme,
`multimod.py` gueltig. `wiki/generate.py --all --check`: dieselben 28 Meldungen wie die Basis 9dfb3f643 (simpleblocks/simpleoctets/
textures/modules.js), keine neue. Volles `precheck`: rot nur durch Basis-Fehler (wiki-Tests simpleblocks, Adapter-Kern-Luecke).
**sbt offen** (sb-test nicht erreichbar): `/root/sb-tools-a/a-audsb31e2-sbt.cmd` – Compile + `module-<mod>-fabric-263` + Standalone
fuer die 10 Mods mit Code-Aenderung + `data_integrity`. Die 17 neuen Gametests sind nur typgeprueft, nicht gelaufen.
