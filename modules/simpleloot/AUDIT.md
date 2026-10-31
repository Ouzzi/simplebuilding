# AUDIT Simple Loot (`simpleloot`) – Release-Audit b-rel32-qolb, 31.10.2026

Basis: b-rel32 d9cda1e21, Branch `b-rel32-qolb`. Umfang: 787 Zeilen Java (davon ca. 121 Tests), 5 Mixin-Klassen (2 Accessoren). Nur MC 26.3 Fabric. Pruefliste C1–C9 aus `docs/ai/RELEASE-QOL-OPT-AES.md`.

## Durchgesehene Dateien (jede Datei vollstaendig gelesen)

Java (Fabric 26.3 + shared; neoforge/forge-Loader-Klassen nicht Teil des Auftrags, nur NUR-Fabric):
- `clienttest/java/com/simplebuilding/modules/simpleloot/ModuleClientSmoke.java` (9 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpleloot/LootFabric.java` (15 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpleloot/LootModMenu.java` (9 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpleloot/ModuleGameTest.java` (15 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/IVaultCooldown.java` (12 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/LootCommands.java` (37 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/LootConfig.java` (18 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/LootScreen.java` (35 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/LootTests.java` (121 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/SimpleLoot.java` (96 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/mixin/VaultBlockEntityMixin.java` (64 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/mixin/VaultBlockEntityServerMixin.java` (115 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/mixin/VaultServerDataAccessor.java` (18 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/mixin/VaultServerDataMixin.java` (71 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleloot/mixin/VaultSharedDataAccessor.java` (18 Zeilen)

Ressourcen/Daten (Zeilen):
- `fabric/src/main/resources/fabric.mod.json` (44)
- `shared/resources/assets/simpleloot/icon.layers.json` (100)
- `shared/resources/assets/simpleloot/lang/de_de.json` (10)
- `shared/resources/assets/simpleloot/lang/en_us.json` (10)
- `shared/resources/simpleloot.mixins.json` (15)
- 2 PNG-Dateien (Texturen/Icons, aus `*.layers.json` erzeugt; Pixelgroesse geprueft durch check_mod_icons / check_missing_textures)
- `fabric/build.gradle`, `wiki/manual.json`, `tools/check_data.py`


## Ergebnis C1–C9

- **C1** Alle Klassen gelesen. Tresor-Abklingzeit pro Spieler (UUID -> Weltzeit) in `VaultServerData`, Speicherung ueber `VaultBlockEntity` (`SimpleBuildingLootTimes`, Codec mit String-Schluesseln, Kommentar erklaert die Falle). Tick-Pfad nur alle 40 Ticks und nur bei vorhandenen belohnten Spielern. Statisches `simpleloot$wasRewardedBeforeUnlock` nur Server-Thread (HEAD→TAIL im selben Aufruf). Befund loot-P3-1.
- **C2** VaultBlockEntity (`saveAdditional`/`loadAdditional` TAIL, `@Shadow serverData`); VaultBlockEntity.Server (`tick` HEAD, `tryInsertKey` HEAD+TAIL); VaultServerData (Interface `IVaultCooldown`); zwei Accessoren (`rewardedPlayers`, `connectedPlayers`, `isDirty`). `defaultRequire=1`. TAIL bei `tryInsertKey` feuert nur am Methodenende; fruehe Returns stempeln bewusst nicht. Konfliktrisiko: gering (Tresor-Block ist sonst wenig angefasst).
- **C3** Defaults: Wiederholung an, 100 Tage; Grenzen 1–36500. Cloth-Screen, `/simpleloot vaults cooldown|repeat` und `/simpleloot config`. Lang 10 Schluessel EN=DE.
- **C4** Keine Daten ausser Lang/Icon. Persistenz getestet (`vault_persistence`: Speichern/Laden des Block-Entity).
- **C5** Allein spielbar (Standalone-Target). Keine Fremdzugriffe. Cloth Config harte Abhaengigkeit (Q-B1).
- **C6** A12: eine Mechanik (Tresor-Belohnung nach Wartezeit erneut), mittlere Staerke, per `enableVaultCooldown` komplett abschaltbar (dann Vanilla einmalig pro Spieler).
- **C7** Kein eigenes Netzwerk; Clients sehen die Tresor-Partikel/-Zustaende ueber vanilla `sendBlockUpdated`. Vanilla-Client funktioniert (rein servergetrieben).
- **C8** Keine Assets ausser Icon; keine Pfade/Schluessel; CC0.
- **C9** Keine TODO/FIXME.

## Befunde

| ID | Befund | Fix / Restpunkt |
|---|---|---|
| simpleloot-P2-1 | `fabric.mod.json`/`mods.toml`: kopierter Weather-Text („Loot switch … rain density“). | gefixt: Commit bb1592f6c. |
| simpleloot-P3-1 | Pro Tresor werden max. 128 Zeitstempel gehalten (aeltester wird verdraengt). Bei >128 belohnten Spielern pro Tresor setzt der Tick fuer verdraengte Spieler laufend neue Zeitstempel (Cooldown-Neustart + Block-Update alle 2 s). | Restpunkt (nur Oeffentliche Server mit >128 Spielern pro Tresor); Abhilfe: Verdraengung auch aus `rewardedPlayers` entfernen oder Grenze groesser. |

## Test (ein sbt-Lauf fuer alle sechs Mods)

Lauf 31.10.2026 auf sb-test, Branch `b-rel32-qolb` (Commit e4a1919c3; Docs/Lang-Kategorie ff9fdd1fa): `:mc26_3:fabric:compileJava` + Modul-Compile C=0; `module-simpleloot-fabric-263` **7/7 gruen**, `module-simpleloot-standalone-fabric-263` **7/7 gruen**; `check_client_mixins.py` M=0 (660 Mixins, 0 Probleme, alle Mods); `check_lang_keys.py` L=0 (52 Module).

## Urteil

**shipbar** (nach gruenem Testlauf). Ein P2 (Beschreibung) gefixt, ein P3 Restpunkt. Rückfragen: Q-B1.
