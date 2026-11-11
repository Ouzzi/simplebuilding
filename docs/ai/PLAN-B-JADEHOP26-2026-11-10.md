# Plan b-jadehop26 (10.11.) – Jade-Trichterfilter für simplemachines:hopper

Quelle: /root/sb-tools-b/prio26.md #6 (MACH32 St.3 Rest), Queue MACH32-MOD („offen … Jade-Trichterfilter“). Basis hub/b-bundle175-gen.

## Ist-Stand
- `common/src/jade/.../SimplebuildingJadePlugin` registriert den Server-Provider HOPPER_FILTER nur für `ModHopperBlockEntity`
  (26.2-Pfad, `McVersion.HOPPERS_IN_SB`). Auf 26.3 liegen die Trichter seit Stufe 3 in Simple Machines
  (`TierHopperBlockEntity`) → Jade zeigt dort keinen Filter.
- Simple Machines hat kein Jade-Plugin; Vorbilder: Simple Pads (`PadsJadePlugin`), Simple Crucibles (`CrucibleInfo` + Jade-Klassen).
- Simple Machines baut nur Fabric (kein neoforge/forge-Ordner) → kein Forge-Ausschluss nötig.

## Umsetzung
1. `com.simplemachines.compat.HopperInfo` (Jade-frei, testbar): `serverData(be, tag)` schreibt Modus-Ordinal + Beschreibungs-IDs der Filter-Items
   (Filter-Prinzip: echtes Item im Slot = Filter); `lines(tag)` baut „Filter: <Modus>“, bei Modus ≠ Aus „Gefiltert: …“ bzw. „nimmt nichts an“.
2. `com.simplemachines.compat.jade.{MachinesJadePlugin,HopperJadeServer,HopperJadeClient}`: Daten-Provider auf `TierHopperBlockEntity`,
   Komponente auf `TierHopperBlock`, UID `simplemachines:hopper_filter` (eigener Jade-Schalter).
3. `fabric/build.gradle`: Jade `compileOnly` (nie gebündelt); `fabric.mod.json`: Entrypoint `jade`, `suggests: jade` – Modul läuft ohne Jade.
4. Lang EN/DE: `jade.simplemachines.hopper.{mode,items,nothing}`, `config.jade.plugin_simplemachines.hopper_filter` (Texte wie SB).
5. SB-Plugin bleibt unverändert (26.2-Pfad braucht ihn; auf 26.3 meldet `BlockInfo` für Modul-Trichter nichts → keine Doppelzeile).

## Test
Gametest `jade_hopper_filter_data` (HopperModuleTests + Fabric-Adapter): Aus → nur Moduszeile; Exakt ohne Items → „nimmt nichts an“;
mit Smaragd/Eisen → Items in den Daten und Items-Zeile; Truhe → keine Daten. Ein sbt: Compile Kern + Modul, module-simplemachines fabric+standalone.
Nicht getestet: Jade-Tooltip im Client (kein Client-Lauf je Branch; macht das Bündel/Client-Sicht).

## Teststand (10.11. ~20:40)
- sbt-Lauf 1 (3bd6b3f96, lief nach dem Hänger noch durch): COMPILE_RC=1, TEST_RC=1 – Ursache: Modrinth-Maven-Repo fehlte in modules/simplemachines/fabric/build.gradle (`Could not find maven.modrinth:jade:lt43vWtF`). Fix: Repo-Zeile wie Simple Pads ergänzt. Zweiter (letzter erlaubter) sbt-Lauf **offen – sb-test-STOPP**.
  Log auf sb-agent: /root/sb-tools-b/b-jadehop26-sbt.log.
- Lokal: javac der neuen/geänderten Klassen (HopperInfo, compat/jade/*, HopperModuleTests) gegen MC-26.3-Jar + Jade lt43vWtF fehlerfrei.
