# AUDIT Simple Mob Manners (`simplemobmanners`) – Release-Audit b-rel32-qolb, 31.10.2026

Basis: b-rel32 d9cda1e21, Branch `b-rel32-qolb`. Umfang: 930 Zeilen Java (davon ca. 73 Tests), 4 Mixins. Nur MC 26.3 Fabric. Pruefliste C1–C9 aus `docs/ai/RELEASE-QOL-OPT-AES.md`.

## Durchgesehene Dateien (jede Datei vollstaendig gelesen)

Java (Fabric 26.3 + shared; neoforge/forge-Loader-Klassen nicht Teil des Auftrags, nur NUR-Fabric):
- `clienttest/java/com/simplebuilding/modules/simplemobmanners/ModuleClientSmoke.java` (9 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplemobmanners/MobMannersFabric.java` (23 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplemobmanners/MobMannersModMenu.java` (9 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplemobmanners/MobMannersViewer.java` (24 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplemobmanners/ModuleGameTest.java` (19 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/FrogBucket.java` (87 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/FrogBucketItem.java` (27 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/MobMannersCommands.java` (99 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/MobMannersConfig.java` (36 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/MobMannersScreen.java` (41 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/MobMannersTests.java` (73 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/SimpleMobManners.java` (111 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/mixin/EntityMuteMixin.java` (83 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/mixin/FrogBucketMixin.java` (23 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/mixin/PassiveEntityMixin.java` (56 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemobmanners/mixin/PiglinGoldMixin.java` (62 Zeilen)

Ressourcen/Daten (Zeilen):
- `fabric/src/main/resources/fabric.mod.json` (44)
- `shared/resources/assets/simplemobmanners/icon.layers.json` (64)
- `shared/resources/assets/simplemobmanners/items/frog_bucket.json` (6)
- `shared/resources/assets/simplemobmanners/lang/de_de.json` (24)
- `shared/resources/assets/simplemobmanners/lang/en_us.json` (24)
- `shared/resources/assets/simplemobmanners/models/item/frog_bucket.json` (6)
- `shared/resources/assets/simplemobmanners/questionary/de_de.json` (13)
- `shared/resources/assets/simplemobmanners/questionary/en_us.json` (13)
- `shared/resources/assets/simplemobmanners/textures/item/frog_bucket.layers.json` (28)
- `shared/resources/simplemobmanners.mixins.json` (14)
- 3 PNG-Dateien (Texturen/Icons, aus `*.layers.json` erzeugt; Pixelgroesse geprueft durch check_mod_icons / check_missing_textures)
- `fabric/build.gradle`, `wiki/manual.json`, `tools/check_data.py`


## Ergebnis C1–C9

- **C1** Alle Klassen gelesen (Config/Migration von `simplecreatures.json` und QoL, Befehle mit Alias `/simplecreatures`, `FrogBucket`+`FrogBucketItem`, Viewer, 4 Mixins). Registrierung des einzigen Items (`frog_bucket`) im Fabric-Entry. Befunde mobmanners-P3-1/-2.
- **C2** EntityMute (`Entity.isSilent` HEAD; Typ-Set zwischengespeichert, synchronisiert); FrogBucket (`Animal.mobInteract` HEAD, nur Frosch + Wassereimer); PassiveEntity (`AgeableMob.aiStep` HEAD, alle 100 Ticks, nur Server und nur mit Namen); PiglinGold (`PiglinAi.isWearingSafeArmor` HEAD, statisch). `defaultRequire=1`. Konfliktrisiko: `Entity.isSilent` ist ein Allerwelts-Ziel (HEAD, nur `true` wird gesetzt, sonst faellt es auf Vanilla/andere Mods durch).
- **C3** Defaults: Suffixe `_mute/_shhh`, `_baby/_small`, Piglin-Schalter an. Listen auf 64 Eintraege/64 Zeichen begrenzt. Cloth-Screen, `/simplemobmanners` (Listen+Schalter) und `/simplemobmanners config`. Lang 24 Schluessel EN=DE.
- **C4** Item `frog_bucket` (Item-Definition, Modell, Textur, Lang, JEI-Info-Seite), kein Rezept (nur Fang per Wassereimer), Creative-Tab Werkzeuge. Textur aus Pixel-Ebenen. Kein Loot.
- **C5** Allein spielbar (Standalone-Target). Keine Fremdzugriffe. Cloth Config harte Abhaengigkeit (Q-B1). Alias `simplecreatures` (Umbenennung) bleibt bewusst.
- **C6** A12: Stummschalten per Namensschild ist schwach, Baby-Halten stark (Erwachsene mit Suffix werden zu Babys – gewollt, Kommentar im Code), Piglin-Gold als Schalter; Froscheimer neues Item (kein Vanilla-Pendant). Jeder Eingriff einzeln abschaltbar bzw. ueber Sub-Mod-Schalter.
- **C7** Kein eigenes Netzwerk. Stummschaltung wirkt serverseitig (kein Ton-Paket). Mit Vanilla-Client: Froscheimer-Item braucht die Mod am Client (Fabric-ueblich).
- **C8** Eigene Pixel-Textur, keine Mojang-Pixel; keine Pfade/Schluessel; CC0.
- **C9** Keine TODO/FIXME. Queue REV2-B-2 erledigt.

## Befunde

| ID | Befund | Fix / Restpunkt |
|---|---|---|
| simplemobmanners-P3-1 | `/simplemobmanners … add <suffix>` meldet „Added“, obwohl `normalize` Eintraege >64 Zeichen oder leere stillschweigend verwirft. | Restpunkt (Kleinkram). |
| simplemobmanners-P3-2 | `EntityMuteMixin.mutedTypes` liest die Config-Liste ohne Sperre, waehrend ein Befehl sie aendern kann (Integrierter Server: Client-Thread liest). Fenster minimal (ArrayList-Modifikation). | Restpunkt (theoretisch; `normalize` ersetzt die Liste nach jeder Aenderung). |
| simplemobmanners-P3-3 | Befehls-Rueckmeldungen als `Component.translatable` – Vanilla-Client sieht Schluessel (nur Operatoren). | Restpunkt (wie simpletools-P3-6). |

## Test (ein sbt-Lauf fuer alle sechs Mods)

TESTERGEBNIS-PLATZHALTER

## Urteil

**shipbar** (nach gruenem Testlauf). Keine P1/P2; drei P3 als Restpunkte ohne Release-Einfluss. Rückfragen: Q-B1.
