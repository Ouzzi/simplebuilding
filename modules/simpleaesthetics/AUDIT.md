# Audit simpleaesthetics (b-rel32-aes, 31.10.2026)

Basis `b-rel32` 1be1d2873, nur MC 26.3 Fabric. Gelesen: alle Fabric- und Shared-Java-Dateien des Mods vollstaendig, dazu fabric.mod.json, Mixin-JSON, Gradle, Daten, Tools-Skript und genutzte Framework-Klassen (`SubMods`, `ConfigFiles`, `CosmeticIntensity`). Gametest-Klassen (AestheticsTests) nur ueberflogen, Forge/NeoForge-Zweige nicht Teil dieses Releases (nur Dateiliste). Nichts veroeffentlicht.

Durchgesehene Dateien (20 Dateien, 558 Zeilen; PNG/Binaerdateien nur auf Herkunft geprueft). Forge/NeoForge-Teile sind nur gelesen, nicht Teil dieses Releases (nur 26.3 Fabric):

- `clienttest/java/com/simplebuilding/modules/simpleaesthetics/ModuleClientSmoke.java` (9)
- `fabric/build.gradle` (13)
- `fabric/src/main/java/com/simplebuilding/modules/simpleaesthetics/AestheticsFabric.java` (12)
- `fabric/src/main/java/com/simplebuilding/modules/simpleaesthetics/ModuleGameTest.java` (8)
- `fabric/src/main/resources/fabric.mod.json` (35)
- `forge/build.gradle` (8)
- `forge/src/main/java/com/simplebuilding/modules/simpleaesthetics/AestheticsForge.java` (10)
- `forge/src/main/resources/META-INF/mods.toml` (42)
- `neoforge/build.gradle` (33)
- `neoforge/src/main/java/com/simplebuilding/modules/simpleaesthetics/AestheticsNeoForge.java` (10)
- `neoforge/src/main/java/com/simplebuilding/modules/simpleaesthetics/ModuleNeoTests.java` (20)
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml` (33)
- `shared/java/com/simplebuilding/modules/simpleaesthetics/AestheticsTests.java` (63)
- `shared/java/com/simplebuilding/modules/simpleaesthetics/SimpleAesthetics.java` (76)
- `shared/resources/assets/simpleaesthetics/icon.layers.json` (100)
- `shared/resources/assets/simpleaesthetics/lang/de_de.json` (9)
- `shared/resources/assets/simpleaesthetics/lang/en_us.json` (9)
- `shared/resources/data/simpleaesthetics/structure/empty.nbt` (19)
- `tools/check_data.py` (18)
- `wiki/manual.json` (31)

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Code | Super-Mod ohne Inhalt: `SimpleAesthetics` liest/schreibt `config/simpleaesthetics.json` (Quarantaene bei kaputter Datei, atomar geschrieben), setzt die drei Schalter ueber `SubMods`. Die Sub-Mods fragen die Schalter erst zur Laufzeit (Hooks), daher ist die Fabric-Ladereihenfolge (Sub-Mods vor dem Super-Mod wegen `depends`) unkritisch. Kein Client-Code, keine statischen Maps ausser `config`. |
| C2 Mixins | keine |
| C3 Config | drei Schalter, Default true, Lang EN/DE mit "Default:"/"Standard:"; `/simpleaesthetics config` ueber `ConfigCommands` (Neustart-Hinweis fuer alle drei, obwohl live angewendet - konservativ, harmlos). Kein Mod-Menu-Eintrag (Konfiguration nur per Befehl/Datei) - Hinweis P3. |
| C4 Daten | nur Lang + Icon + leere Struktur; `check_data.py` gruen (lokal). |
| C5 Unabhaengigkeit | Super-Mod requires die drei Sub-Mods (Supermod-Konzept); Sub-Mods bleiben einzeln spielbar. `depends simplelib >=0.3.0` plus eingebettet. |
| C6 A12 | kein Eingriff in Vanilla. |
| C7 Netzwerk | keine Pakete. |
| C8 Lizenz/Assets | CC0-1.0 in fabric.mod.json; Icon selbstgezeichnet (`icon.layers.json`); keine Pfade/IPs/Secrets. Es liegt kein `LICENSE`-Datei im Modulordner (nur bei simplevisuals) - P3. |
| C9 Offen | kein TODO/FIXME im Code. |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simpleaesthetics-P3-1 | P3 | Kein Mod-Menu-/Cloth-Bildschirm, nur Befehl/Datei | Hinweis (Absicht, kein Gameplay) |
| simpleaesthetics-P3-2 | P3 | Kein `LICENSE` im Modulordner (fabric.mod.json nennt CC0-1.0) | Hinweis fuer Release (Datei aus simplevisuals kopieren) |
| simpleaesthetics-P3-3 | P3 | Version 0.9.0 (Sub-Mods: Visuals 0.9.0, Models/Sounds 0.1.0) uneinheitlich (ST-14) | Rueckfrage Versionsschema |

## Urteil
**shipbar** (0 P1, 0 P2, 3 P3). Sub-Mod-Urteile siehe deren AUDIT.md.

## Testergebnis
@@TEST@@
