# Plan b-stylefix3 (17.10.): Reste des Stil-Audits (ST-16, ST-22 bis ST-24, Fehlalarm mixed_octet)

Ist-Zustand: Der gemeinsame Schalter existiert bereits als `framework/.../CreativeTabSettings` (`config/simple-creative-tabs.properties`,
`<modid>.addItemsToVanillaTabs`, Standard true). Von den 11 Modulen aus ST-22 haben beacons, crucibles, fun, mobheads, ovens, riding,
shops, trims ihn auf allen 3 Loadern schon; es fehlen nur maps, mobs, tweaks. Framework ist in allen 11 gebuendelt (kein neues Bundling).

1. ST-22: Schalter in maps (`MapsConfig.vanillaTabs()`), mobs (`MobsConfig.vanillaTabs()`), tweaks (inline, Config-Pfad je Loader)
   an jede Vanilla-Tab-Einfuegung auf Fabric/NeoForge/Forge. Handbuecher bleiben immer in Werkzeuge (wie SB). Eigener Tab bleibt.
   Standard = bisheriges Verhalten (true). Wiki: Feature `creative_tab_switch` (EN/DE) in den 10 Modul-manual.json (mobheads hat es).
2. ST-16: riding NeoForge `iconFile`; Fabric-Beschreibung = modules.json (= NeoForge/Forge).
3. ST-23/24: `tools/audit_data.py`: Model-only-Definitionen (placed_/standing_/held_, riding Buecher, sandwiches board.bread_half) und
   `jukebox_song.*` als Ausnahme.
4. Fehlalarm: `wiki/generate.py` zog `TECHNICAL_BLOCKS` nur in der Vorwaertsrichtung ab, nicht in der Gegenrichtung "registriert ohne Key".
Verifikation: Compile 3 Loader, Modul-Targets maps/mobs/tweaks/riding (fabric+neoforge), DataIntegrity/ConfigOption, wiki check, check_data.
