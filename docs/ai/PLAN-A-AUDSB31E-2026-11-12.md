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
