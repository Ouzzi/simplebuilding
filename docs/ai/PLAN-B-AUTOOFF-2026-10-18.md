# Plan b-autooff (18.10.2026): Simple Tweaks "automatisch aus mit Partner-Mod"

Quelle: Queue-Punkt Plan Schutz 17.10., `PLAN-TWEAKS-PROTECT-2026-10-17.md` (Überschneidungen), Modpack `MODPACK-BESITZER-2026-10-17.md`.

## Ist-Zustand
- Drei Server-Configs (`simpletweaks-admin/-play/-protect.json`), lazy per `Admin/Play/Protect.config()` geladen, Reset bei Serverstopp.
- Loader-Abstraktion für Modliste existiert: `play.Mods.check` (Fabric `FabricLoader.isModLoaded`, NeoForge `ModList.isLoaded`, Forge `ModList.isLoaded`), gesetzt in `Play*.init`. Keine neue Loader-Arbeit nötig.

## Entwurf
- `play/AutoOff` (rein): `match(Map<Feature,List<ModId>>, Predicate loaded)` -> Feature -> erster geladener Partner; `clean` (Id-Muster, Grenzen); `log` (eine INFO-Zeile je Treffer).
- Je Config ein Feld `autoOffWith` (Map Feature -> Mod-Ids) und `withAutoOff(loaded, log)`, aufgerufen in `config()` nach dem Laden (Tests mit `configure()` bleiben unberührt).
- Zuordnung/Defaults: blockLog [ledger, minetracer]; deathBackups [invrestore, inv_restore]; invsee [invview, inv_view]; pregen [chunky]; actionChecks [litematica, axiom]; antiXray [] (Standard-Schutz bleibt an, Admin kann eintragen); freecam [freecam] nur Hinweis.
- Mod-Ids: aus `tools/devmods.json` (axiom, freecam, invview, ledger, litematica) belegt; invrestore/minetracer/chunky nicht lokal belegbar -> gängige Schreibweisen (`invrestore`+`inv_restore`, `invview`+`inv_view`).
- Explizites `enabled:true`: in der Datei nicht vom Standard unterscheidbar (Defaults werden beim ersten Start mitgeschrieben) -> autoOff gewinnt; Log nennt: Liste leeren.
- Litematica/Axiom: Plan sagt, beide platzieren/brechen legitim weit und schnell (Easy Place, Axiom-Paket) -> actionChecks (Reach/Fast-Break) würden fehlmelden -> aus. Anti-X-Ray: Standardschutz, daher leere Liste.

## Tests
- GameTest `protect_game_test_auto_off_with_partner_mods` (Fabric-Adapter `ProtectGameTest`, NeoForge/Forge über `ProtectTests.TESTS`) mit simulierten Ids; Compile Fabric/NeoForge/Forge; `check_data.py`; Wiki/Guides/Discover regenerieren, `./gradlew check`.
