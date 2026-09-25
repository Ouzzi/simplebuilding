# Multiloader – Test- & To-do-Status

_Stand: **2026-08-31**. Zwei Minecraft-Linien (**26.2** und **1.21.11**), je zwei aktive Loader
(**Fabric** + **NeoForge**) = vier Ziele. Forge liegt bewusst still (siehe unten)._

## 1. Build- & Testergebnis (tatsächlich ausgeführt, nicht abgeleitet)

| Ziel | In-Game-Tests | Renderer-Nachweis (Client) |
|------|---------------|----------------------------|
| Fabric 26.2 | ✅ 45/45 (`:runGametest`) | ✅ `:runClientGameTest` |
| Fabric 1.21.11 | ✅ 45/45 (`:mc1_21_11:fabric:runGametest`) | ✅ `:mc1_21_11:fabric:runClientGameTest` |
| NeoForge 26.2 | ✅ 44/44 (`:neoforge:runGameTest`) | ✅ `:neoforge:runClientGameTest` |
| NeoForge 1.21.11 | ✅ 44/44 (`:mc1_21_11:neoforge:runGameTest`) | ✅ `:mc1_21_11:neoforge:runClientGameTest` |

44 Testkörper liegen in **einem** gemeinsamen Katalog (`SimpleBuildingGameTests`) je Minecraft-Linie;
die Loader registrieren dieselben Körper über dünne Adapter. Fabric zählt einen Test mehr, weil dort
ein zusätzlicher loaderspezifischer Fall registriert ist.

Der Renderer-Nachweis vergleicht Screenshots gegen einen gemessenen Rauschboden (0/409920 Pixel) mit
Gegenprobe; die Abbau-Risse werden stattdessen über die Render-States zurückgelesen, weil Abbaupartikel
zufällig sind. **Nicht** bewiesen: dass die Pixel *richtig aussehen* – es gibt kein Referenzbild.

## 2. Was funktioniert (NeoForge-Parität bestätigt – compile/analyse)

Auf NeoForge funktional vollständig verdrahtet (kein echtes Feature-Loch):
- **Core-Registry** (Blocks, Items, Block-Entities, Data-Components, Screen-Handler, Recipes, Item-Gruppen) – via `RegisterEvent` + `DeferredRegister`.
- **Gameplay-Events** (Sledgehammer, StripMiner, VeinMiner, Versatility, DynamicLight, Spatula-Migration) – via `@EventBusSubscriber`.
- **Networking** (alle 10 C2S + 3 S2C Payloads, PlatformServices, ClientNetworking).
- **Loot / Trades / Worldgen / Commands** – via NeoForge-Events + `biome_modifier`-JSONs (2 Erze = 2 JSONs).
- **Client** (Keybinds, HUD-Overlays, Menü-Screen, Tooltip, Item-Model-Property, Block-Highlight, S2C-Receiver, **Config-Screen**).

## 3. Offene Punkte – nach Priorität

### ✅ Erledigt in dieser Session
- **B1** – Dedicated-Server-Crash-Mixin (Client-Mixins ins side-gated `client`-Array).
- **M1** – NeoForge In-Game-Config-Screen (`IConfigScreenFactory` + cloth AutoConfig-GUI).
- **M2** – Versatility nur beim initialen Klick (`Action.START`), nicht pro Mining-Tick.
- **M3** – Versionsangleichung (READMEs auf MC 26.1.2 / Java 25 / Loader 0.19.3 / Mod 1.3.1).
- **M4** – Obsolete Version-Profile + Matrix-Tasks entfernt (`profiles/`, `matrix*`/`listVersionProfiles` in `build.gradle`) — die Mechanik war wirkungslos.
- **M6** – Alle Quellen committet, Working-Tree sauber.
- **L1** – Deprecation-Warnung (`KeyMapping.Category`) unterdrückt.
- **L2** – Repo-Müll entfernt (~4,5 MB javap-Dumps), `.gitignore` ergänzt.
- **L5** – `ModEnchantmentEffects`-No-Op geprüft → kein Bug (bewusster Platzhalter).
- **L6** – „tote" Mixins geprüft → behalten (geplantes Render-Scaffolding, kein Müll).

### 🟡 MEDIUM – offen
- [ ] **M5 – NeoForge-Datagen erzeugt nichts.** `data`-Run existiert, aber kein `GatherDataEvent`/Provider registriert → No-Op. Unkritisch, weil NeoForge die Fabric-generierten Assets aus `src/main/generated` mitnutzt. `neoforge/build.gradle`

- [ ] **Manueller Spieldurchlauf.** Kein automatisierter Test kann beurteilen, ob sich die Mod
  *richtig anfühlt*. Steht auf allen vier Zielen aus und kann nur der Mensch erledigen.
- [ ] **Nicht abgedeckte Bereiche der Testsuite** (bewusste Grenzen, keine Fehler):
  Renderer-*Korrektheit*, Config-Kombinationen, echtes Mehrspieler-Setup, Zusammenspiel mit
  fremden Mods, Leistung.

### 🟢 LOW – offen (kosmetisch / minimal)
- [ ] **Fabric-Loader auf 0.19.3 festgenagelt** statt 0.19.4 — 0.19.4 brachte eine MixinExtras-
  Unverträglichkeit. Bei der nächsten MixinExtras-Aktualisierung erneut versuchen. `gradle.properties`
- [ ] **`makeMockServerPlayerInLevel` ist `@Deprecated(forRemoval)`** und trägt die halbe Testsuite.
  Wenn Mojang es entfernt, brauchen die Tests einen eigenen Mock-Spieler.
- [ ] **cloth_config nutzt das veraltete `logoFile`** → NeoForge zeigt beim Start einen Warnbildschirm.
  Fremdbibliothek, von uns nicht behebbar; die Testläufe unterdrücken den Bildschirm über
  `showLoadWarnings = false`. Unsere eigene Warnung ist behoben (`iconFile`).
- [x] **L3 – `loom_version=1.16-SNAPSHOT`** ✅ **BEHOBEN (2026-08-20)** — auf Release `1.16.3` gepinnt (die Version, auf die der SNAPSHOT zuletzt auflöste; per maven.fabricmc.net verifiziert), Build weiterhin grün.
- [x] **L4 – `neoforge.mods.toml` Metadaten** ✅ **BEHOBEN** — `logoFile="assets/simplebuilding/icon.png"` (im JAR vorhanden, verifiziert) + `displayURL` ergänzt → Icon/Link in der NeoForge-Mod-Liste.
- [x] **L7 – Fragile Registrierung** ✅ **BEWERTET – keine Aktion.** Static-Init + implizite Lifecycle-Reihenfolge funktionieren in der aktuellen NeoForge-Lifecycle (RegisterEvent vor FMLCommonSetup). Strukturelle Anmerkung, kein Fehler; ein Guard wäre Nice-to-have, kein Muss.
- [x] **L8 – Payloads „required"** ✅ **BEWERTET – by design.** SimpleBuilding ist ein Inhalts-Mod, der ohnehin auf beiden Seiten installiert sein muss; `required` (kein `.optional()`) ist hier korrekt, nicht ein Bug.
- [x] **L9 – Ore-Biome-Targeting** ✅ **BEWERTET – akzeptabel.** `foundInTheEnd()` vs. `#minecraft:is_end` decken sich für das Vanilla-End; Abweichung nur bei modded End-Biomen ohne `is_end`-Tag → vernachlässigbar.

## 4. Laufzeit

✅ **NeoForge-Client startet sauber** (`gradlew :neoforge:runClient`, 2026-06-16): FML erkennt den Mod, alle Registrierungen + `common initialized` laufen, Textur-Atlanten/Sound/Resource-Reload (inkl. `mod/cloth_config`) OK, **0 Errors/Exceptions/Mixin-Fehler**, Hauptmenü erreicht.

✅ **Fabric-Client startet sauber** (`gradlew :runClient`, 2026-08-20): Fabric Loader 0.19.3 + MC 26.1.2, alle Registrierungen (`Simplebuilding common initialized for fabric`), Welt geladen, **0 Errors/Exceptions/Mixin-Fehler**.

Noch nicht systematisch in-game durchgespielt (optional, am echten Client): eine Welt erstellen und Gameplay prüfen — Items/Blöcke im Creative-Tab, Building-Wand + Highlight, Hopper-Menü, Doublejump, Config-Screen-Button, Trim-Boni. Sowie optional `:neoforge:runServer` (Dedicated-Server, prüft den B1-Fix real).

### Paritäts-Audit 1.21.11 <-> Multiloader (2026-08-20/25, Fabric-Fokus)
Vollständiges Feature-Audit (10 Domänen, verifiziert): Kern-Gameplay, Blöcke, Netzwerk, Daten und Config sind vollständig portiert. **6 verifizierte Regressionen** — alle behoben und auf `claude/integration-check` zusammengeführt (`gradlew build` grün für beide Loader, Fabric-`runClient` sauber gebootet):
- [x] Trim-Glow auf getragener Rüstung — `EquipmentRendererMixin` neu implementiert + registriert (f854b8d); Mixin-Anwendung im Fabric-Log belegt.
- [x] Villager-/Wandering-Trades — 20 datengetriebene Trade-JSONs + 11 Tag-Merges, Loot-Funktion `simplebuilding:weighted_enchant`, Config-Gate via Resource-Condition (6f658db). **Fabric vollständig**; NeoForge siehe offene Punkte.
- [x] Fabric-Outline invertiert — Negation ergänzt (0e3421a).
- [x] Building-Wand-Ghost-Vorschau — `BuildingWandPreviewRenderer` (29e3ed6).
- [x] Multi-Block-Abbau-Risse — `MultiBlockBreakingSupport`, leerer `WorldRendererMixin` entfernt (29e3ed6).
- [x] Statusmeldungen Actionbar statt Chat — 13 Stellen auf `sendOverlayMessage` (1ce1d0d). **Fabric vollständig**; NeoForge siehe offene Punkte.
- [x] Nachgelagert gefunden: Vanilla-Outline wurde auf dem anvisierten Block ganz unterdrückt, obwohl beide Mod-Renderer ihn bewusst auslassen — 1.21.11-Verhalten wiederhergestellt (3f0a827).

Aufgeräumt: `backup 1/` nach `checker-backup/` außerhalb der Ressourcen verschoben + 3 verirrte Fremd-JARs entfernt (e253ac3) — **Mod-JAR 15,3 MB auf 1,2 MB**, Ressourcen-Fehlerspam beim Start weg.

### Aus dem Audit (NeoForge-seitig) — ERLEDIGT 2026-08-27
- [x] **Config-Gate der Trades wirkt jetzt auch auf NeoForge** — `villager_trade` ist eine
  Datapack-Registry aus `RegistryDataLoader.WORLDGEN_REGISTRIES`, und NeoForge patcht genau
  diesen Ladeweg auf `ConditionalOps`: eine nicht erfüllte Bedingung lässt den Eintrag ganz
  weg. Neue `ConfigLoadCondition` (ICondition) unter demselben Namen `simplebuilding:config`
  wie die Fabric-Condition; alle 20 Trade-JSONs tragen jetzt beide Bedingungsblöcke mit
  identischem Flag.
- [x] **Statusmeldungen** — die drei bekannten Chat-statt-Actionbar-Stellen behoben; zusätzlich
  eine vierte gefunden (Rotator-Meldung in `ModRegistriesNeoForge`). NeoForge hat jetzt null
  `sendSystemMessage`-Aufrufe. Ursache angegangen: Toggle-Key- und Constructor's-Touch-Logik
  liegen im gemeinsamen Baum, beide Loader lesen aus einer Quelle.
- [x] **`ConfigResourceCondition`** aus dem Paket `datagen` nach `com.simplebuilding.condition`
  verschoben — sie ist eine Laufzeitklasse (läuft beim Laden der Datenpakete).

### Kosmetik (optional)
- [x] Ressourcen-Warnungen: Ordner `backup 1/` unter `src/main/resources/assets/simplebuilding/textures/block/` hat ein Leerzeichen im Pfad → ungültige ResourceLocation, wird ignoriert (harmlos, geteilt mit Fabric). Erledigt (2026-08-20): PNGs unterscheiden sich von den aktiven Texturen → nach `art/dev-textures/checker-backup/` verschoben; außerdem 3 versehentlich committete JARs (~15 MB) aus `textures/item/` entfernt.

## Forge: ZURUECKGESTELLT (Entscheidung 2026-08-27)

**Forge wird vorerst NICHT weiterverfolgt.** Das Modul bleibt im Repo und baut gruen
(`gradlew :forge:build`, MC 26.2 / Forge 26.2-65.1.3 / ForgeGradle 7.0.36), damit der
Wiedereinstieg jederzeit moeglich ist — es wird aber nicht auf Feature-Paritaet
gebracht und nicht ausgeliefert. Aktive Loader sind **Fabric und NeoForge**.

Was bei einem spaeteren Wiedereinstieg offen ist (belegt durch Vergleich mit dem
NeoForge-Modul):
- [ ] **Keiner der drei In-Welt-Renderer ist verdrahtet** — `BlockHighlightRenderer`,
  `BuildingWandPreviewRenderer` und `MultiBlockBreakingSupport` kommen im Forge-Modul
  nicht vor. Auf Forge fehlen damit Sledgehammer-/Octant-Highlights, die
  Building-Wand-Ghost-Vorschau und die Abbau-Risse auf Mehrfachbloecken.
  Erschwerend: MC 26.2 reicht Geometrie ueber `SubmitNodeCollector` ein; Forge 65.1.3
  hat kein offensichtliches Pendant zu NeoForges `SubmitCustomGeometryEvent`
  (Kandidat waere `AddFramePassEvent`, sonst ein eigener Mixin).
- [ ] **Kein Config-Screen** — NeoForge nutzt `IConfigScreenFactory` + cloth AutoConfig.
  `cloth-config` gibt es fuer Fabric und NeoForge, fuer Forge vermutlich nicht; dann
  waere `ForgeConfigSpec` + eigener Screen noetig. Achtung: `SimplebuildingConfig` und
  `HeldItemRendererMixin` liegen im gemeinsamen Baum und nutzen `me.shedaniel`-Klassen.
- [ ] **Weniger HUD-Verdrahtung als NeoForge** (`AddGuiOverlayLayersEvent`): Oktant-HUD
  (`RangefinderHudOverlay`), Tacho (`SpeedometerHudOverlay`) und Luftsprung-Balken
  (`DoubleJumpHudOverlay`) fehlen; ebenso die `enchant_type`-Property der Buchtexturen.
- [ ] **Nie zur Laufzeit gestartet** — auch nicht vor der Entfernung in dbdffdf.
  Ein `runClient`/`runServer`-Durchlauf steht komplett aus.
- [ ] **Kein Gametest-Adapter** — die gemeinsamen Testkoerper werden mitkompiliert
  (`forge/build.gradle` nimmt `common/src/shared/java`), aber nichts registriert sie, es gibt
  keinen `gameTestServer`-Lauf und kein Forge-Ziel in `tools/testrunner/run.py`. Alle
  Forge-eigenen Daten sind damit ungeprueft, etwa die Biom-Modifier
  `forge/src/main/resources/data/simplebuilding/forge/biome_modifier/*.json`. Belegt ist nur
  der Quellstand: `ConstructorsTouchSingleSourceTest` (JUnit, Teil von `gradlew check`) prueft,
  dass Forge die gemeinsame Constructor's-Touch-Logik aufruft.
- [x] **Angeglichen am 2026-09-24** (Code-Stand, nicht gestartet): die Loot-Funktion
  `simplebuilding:weighted_enchant` wird in `ForgeRegistryBootstrap` registriert (ohne sie
  scheitert das Laden der Trade-JSONs); der Constructor's-Touch-Stock ruft
  `ConstructorsTouchInteraction` (die driftende Kopie `ModRegistriesForge` ist geloescht, ebenso
  die tote dritte Kopie `ModRegistriesNeoForge` auf 26.2); der Luftsprung laeuft ueber
  `DoubleJumpController` statt ueber eine eigene Logik ohne Abklingzeit; `/simplebuilding` kommt
  aus dem gemeinsamen `SimplebuildingCommand`.

## MC-1.21.11-Linie (Stand 2026-08-27)

Zweite unterstuetzte Minecraft-Version, aus der 26.2-Linie heruntergeportet (nicht aus dem
alten Yarn-Branch hochgezogen). Verzeichnis `mc1_21_11/` mit `shared/java` + `fabric` +
`neoforge`. Ein einziger `gradlew build` erzeugt beide Minecraft-Versionen.

- [x] Fabric 1.21.11 — baut, Client bootet bis ins Hauptmenue.
- [x] NeoForge 1.21.11 (21.11.45) — baut, Client bootet.
- [x] Villager-/Wandering-Trades — code-basiert (Fabric: TradeOfferHelper, NeoForge:
  eigene Events), alle 20 Trades der 26.2-Linie, Tabelle und Zufallslogik einmal im
  gemeinsamen Baum. Beide Config-Gates wirken hier auf BEIDEN Loadern.
- [x] **Datagen der Linie laeuft eigenstaendig** (2026-08-27) — `fabricApi.configureDataGeneration`
  aktiviert, `:mc1_21_11:fabric:runDatagen` erzeugt die Daten aus den eigenen Providern.
  663 Dateien rein, 663 raus; die einzigen 54 Abweichungen sind kosmetisch (53 Rezepte mit
  explizitem `"count": 1`, ein Tag nur in anderer Reihenfolge).
- [ ] **Kein Gameplay-Test** — bisher nur Boot-Tests auf beiden Loadern.
- [x] Kosmetik: 6 Spatula-Items waren ohne Modell registriert (fuer
  `LegacySpatulaMigration`) und erzeugten beim Start "No model loaded"-Warnungen.
  Seit 2026-09-24 haben sie auf beiden Minecraft-Versionen eine Item-Modelldefinition
  (ModModelProvider, gehalten wie die Meissel, Textur textures/item/<stufe>_spatula.png).

## MC-26.3-Linie (Stand 2026-09-25) - Overlay statt Kopie

Dritte Linie fuer **Fabric + NeoForge** (Forge bleibt 26.2-only), 26.2 bleibt die Hauptlinie.
Keine dritte Vollkopie: `:mc26_3:fabric` und `:mc26_3:neoforge` kompilieren dieselben Baeume wie die
26.2-Module (`common/src/shared/java`, `common/src/jei/java`, `src/main/java` bzw.
`neoforge/src/main/java`, die gemeinsamen Client-Test-Baeume) plus ein kleines Overlay. Was sich
zwischen 26.2 und 26.3 unterscheidet, liegt als **Zwillingspaar** vor:

| 26.2-Seite (nur 26.2-Module) | 26.3-Seite (nur mc26_3-Module) | Inhalt |
|---|---|---|
| `common/src/mc26_2/java` | `mc26_3/overlay/java` | Versions-Shim `com.simplebuilding.version.*` (McVersion, McClientVersion, LootNumbers, BlockCodecs, TexturedGuiElementState), ModWorldGen, 3 Client-Mixins (HeldItemRenderer, BundleTooltip, EquipmentRenderer), Test-Helfer OreGenChecks / LootJsonShape / CookingChecks |
| `src/mc26_2/java` | `mc26_3/fabric/src/main/java` | Fabric-Datagen: RecipeProviderCompat, ModelGenCompat |
| `common/src/mc26_2/clientgametest/java` | `mc26_3/overlay/clientgametest/java` | SpriteRecordingGraphics, ClientTestVersion |
| `neoforge/src/mc26_2/clientGameTest/java` | `mc26_3/neoforge/src/clientGameTest/java` | MouseHandlerAccessor, MouseEvents |

`gradlew checkOverlays` (haengt an `check`) scheitert, wenn eine Klasse zugleich im gemeinsamen Baum
und in einem Overlay liegt oder ein Overlay-File keinen Zwilling hat.

**Ressourcen** (`mc26_3/resources.gradle`): `mc26_3/overlay/resources` (Enderit-Equipment-Asset mit trim_overrides) und
`mc26_3/generated` liegen VOR `src/main/resources` / `src/main/generated`, Duplikate = EXCLUDE (erstes
gewinnt). `mc26_3/generated` enthaelt nur die 26.3-Datagen-Dateien, die sich von 26.2 unterscheiden
(`:mc26_3:fabric:runDatagen` -> `syncGenerated263`); `removed-on-26.3.txt` blendet 26.2-Dateien aus, die
26.3 nicht mehr erzeugt (configured_feature). Handels-JSONs werden beim Bauen auf die 26.3-Schluessel
umgeschrieben, Trim-Paletten nach `textures/palettes/trim` kopiert.

Tests: `run.py --targets fabric-263,neoforge-263` (Server, gemeinsamer Katalog), Client-Ziele
`client-fabric-263` / `client-neoforge-263`.

Mod-Blasting-Rezepte: 26.3 speichert die Ofenzeit und der Hochofen halbiert sie; die 26.3-Datagen
schreibt deshalb die doppelte Zeit (RecipeProviderCompat.fastMachineTicks), real bleibt es bei der 26.2-Dauer.

Erledigt in Welle 12 (2026-09-25):
- Kalibrierte Erzdetektoren aus 26.2-Welten: getCustomBlock reicht readBlockState beide Schreibweisen
  (Name/Properties und id/properties); Test ore_detector_..._detector_calibrated_in_either_minecraft_line_keeps_its_target.
- Enderit-Trim auf Enderit-Ruestung in `enderite_darker`: 26.2 ueber den Asset-Group-Override
  (ModTrimMaterials.DARKER_ON), 26.3 ueber trim_overrides in
  `mc26_3/overlay/resources/assets/simplebuilding/equipment/enderite.json` (erste Datei in overlay/resources);
  Test trim_wiring_..._enderite_trim_turns_darker_on_enderite_armour.
- Wiki mit 26.3-Linie (generate.py baut den 26.3-Baum aus src/main/generated + mc26_3/generated, Rezepte
  mit gleicher Id und anderem Inhalt werden als Variante gezeigt, 26.3-Hochofenzeiten normalisiert).
- Jade / AppleSkin / Mouse Tweaks als 26.3-Dev-Mods (nur runClient/runServer).

Teststand 2026-09-25 (nach Merge mit master): alle 7 Server-Ziele gruen (2776/2776), client-fabric-263,
client-neoforge-263 und client-fabric-262 je 117/117. Der Glimmer-Test vergleicht Pixel nur im Inventar-Panel
(die Welt dahinter aendert sich auf 26.3 weiter); "nirgends sonst" prueft der Render-State fuer den ganzen Bildschirm.
Client-Unterschiede 26.3, die das Geruest jetzt abdeckt: SDL-Maustasten (links 1, rechts 3 -> immer
InputConstants.MOUSE_BUTTON_*), gamerule/time set/weather ohne Aenderung sind Befehlsfehler,
ItemStack#useOn setzt heldItemTransformedTo in jedes Success.

## MC-26.4-Snapshot-Linie (Stand 2026-09-25) - EXPERIMENTELL, nicht fuer Releases

Vorbereitung auf den naechsten Drop. Stand: **26.4-snapshot-1** (Mojang-Manifest, 2026-09-22), Fabric
Loader 0.19.5, Fabric API 0.161.1+26.4, Loom 1.17.20, ModMenu 22.0.0-alpha.1. **Cloth Config** hat noch
keinen 26.4-Build; die 26.3-Version (26.3.159, `minecraft >=26.3-`) laeuft auf dem Snapshot. JEI, Jade,
AppleSkin, Mouse Tweaks: keine 26.4-Builds -> keine Dev-Mods (JEI-Plugin kompiliert gegen die 26.3-API).
**Forge:** kein Snapshot-Build (neuestes 26.3-66.0.3). `mc26_4/forge/build.gradle` ist ein ungetestetes
Geruest, eingeschaltet mit `-Pmc264_forge_version=<v>` (Forge-Quellen waren nie auf 26.3, also mit
eigenem Overlay-Paar rechnen). NeoForge-26.4 war nicht Teil des Auftrags.

**Nur im Build mit `-Pmc264=true`** (settings.gradle): ein normales `gradlew check` laedt und
kompiliert den Snapshot nie. `run.py`: Ziele `fabric-264` und `client-fabric-264`, nur per Id,
`--targets snapshot` oder `--targets everything-plus-snapshot` - nie in `all`/`everything`/`--release-gate`.
Launch-Knoepfe "fabric-client 26.4-snapshot" / "fabric-server 26.4-snapshot".

**Overlay-Kette** (`mc26_4/chain.gradle`): gemeinsame Baeume -> 26.3-Overlay -> 26.4-Overlay. Eine Datei
in `mc26_4/overlay/java` (bzw. `mc26_4/fabric/src/main/java`, `mc26_4/overlay/clientgametest/java`)
ERSETZT ihren Zwilling im entsprechenden 26.3-Overlay; alle anderen 26.3-Overlay-Dateien gelten
unveraendert. Der zusammengesetzte Baum entsteht in `mc26_4/fabric/build/overlay264/<name>`
(Compilerfehler zeigen dorthin - bearbeitet wird die Quelle in mc26_3/ bzw. mc26_4/). `checkOverlays`
(an `check`, auch ohne -Pmc264) verlangt: jede 26.4-Datei hat einen 26.3-Zwilling und unterscheidet sich
von ihm. Bricht 26.4 eine noch gemeinsame Datei, zuerst ins 26.2/26.3-Paar ziehen, dann den 26.4-Zwilling
anlegen. Ressourcen: `mcLayeredResources` (mc26_3/resources.gradle) stapelt `mc26_4/overlay/resources`
und `mc26_4/generated` ueber die 26.3-Schichten (`mergeResources264`/`checkResources264`).
`mc26_4/generated` = nur Datagen-Dateien, die sich vom 26.3-Stand (mc26_3/generated ueber
src/main/generated) unterscheiden; `:mc26_4:fabric:runDatagen` -> `syncGenerated264`.

**Brueche 26.3 -> 26.4-snapshot-1 (am Jar belegt):**
- `RenderPipeline` ist zurueck in `com.mojang.blaze3d.pipeline` (26.3: renderpearl) -> 26.4-Zwillinge von
  TexturedGuiElementState, BundleTooltipComponentMixin (auch das @At-Target), SpriteRecordingGraphics.
- `DyeColor` hat keine Farbwerte mehr (getTextureDiffuseColor/getMapColor/getTextColor/getFireworkColor
  weg; Farben privat in DyedItemColor.DYE_COLORS) -> Test-Shim `gametest/DyeRgb` (neues Paar
  26.2/26.3/26.4; 26.4 ueber `DyedItemColor.applyDyes(null, List.of(dye))`).
- Datagen: 0 Abweichungen zu 26.3.

Teststand 2026-09-25 (26.4-snapshot-1): Server `fabric-264` 400/400 gruen.

Bei jedem neuen Snapshot: `mc264_minecraft_version` / `mc264_fabric_version` heben, kompilieren,
Brueche als 26.4-Zwillinge, `runDatagen`, `run.py --targets snapshot`. Sobald Cloth Config 26.4
erscheint: `mc264_cloth_version` heben.
