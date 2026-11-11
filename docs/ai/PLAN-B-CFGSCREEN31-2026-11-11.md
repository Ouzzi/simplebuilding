# Plan b-cfgscreen31 (11.11.2026): L13 / RELSB31 f2=A – Config-Bildschirm für jede Mod aus SimpleLib

Basis hub/b-bundle182-gen (+ Merge hub/b-relsb31 für docs/ai/RELEASE-SIMPLEBUILDING.md). Auftrag N31b
(/root/b-auftrag-20261109.md), Besitzer 11.11.: f1=A (Welle 1 = SB + 20 Pflicht-Mods), f2=A (gemeinsamer SimpleLib-Baustein,
automatisch aus der Server-/Client-JSON, über Mod Menu für jede Mod, Cloth optional).

## Ist-Stand (Retrieval)
- **Schema ist schon da:** `com.simplelib.api.config.ConfigCommands` (CMD24). Fast jede Mod meldet ihre Config einmal an
  (`ConfigCommands.server(MOD_ID).json(...)/.gson(...)`, 55 Mods; Client-Configs per `client(...)`). Ein `Spec` kennt alle Optionen
  (Blätter der JSON, `gruppe.feld`), Art (BOOL/INT/DECIMAL/TEXT/LIST/OBJECT) aus dem Standardwert, `choices`, `restart`, `secret`,
  und schreibt per `set`/`reset` (Klemmen durch die Config-Klasse, Speichern atomar, `after`-Hook).
- **Mod Menu:** 17 Mods + SB haben eigene Cloth-Bildschirme (b-compat26-Befund). Ohne Cloth geben 16 davon
  `ModMenuApi.super.getModConfigScreenFactory()` (= `NullScreenFactory`) zurück, SimpleLib eine Fabrik, die `null` liefert.
- **Mod Menu 21 (Bytecode 20.0.1 geprüft):** `getProvidedConfigScreenFactories()` jeder Mod-Menu-Implementierung wird bei jedem
  Nachschlagen gelesen und per `putIfAbsent` eingetragen; eigene Fabriken (außer `NullScreenFactory`) haben Vorrang.
- **Vanilla-Bildschirm ohne Cloth:** `gradle/native-config/ConfigBuilder.java` (Cloth-förmige API aus Vanilla-Widgets, nur für
  Forge-Builds per Relocation). Wird als Vorlage übernommen.
- Lang-Schema `text.autoconfig.<mod>.option.<pfad>[.@Tooltip]`, `.category.<gruppe>`, `.title` (b-cfglang26); Tooltips nennen
  „Default:“/„Standard:“ meist schon selbst.

## Entscheidungen (selbst, begründet)
1. **Ein Mod-Menu-Eintrag für alle (dünnster Weg):** SimpleLibs `ModMenuApi` liefert über `getProvidedConfigScreenFactories()` für
   jede geladene Mod mit angemeldetem Spec den generischen Bildschirm. Keine Klasse je Mod nötig („Entrypoint je Mod nur dünn“ →
   hier sogar null). Eigene Bildschirme bleiben vorne (Mod Menu `putIfAbsent`); ohne Cloth fallen die 16 eigenen automatisch auf den
   generischen zurück (sie geben `NullScreenFactory`). SimpleLib selbst: ohne Cloth generisch statt `null`.
2. **Modell getrennt vom Bild:** `ConfigScreenModel` (shared, ohne Client-Klassen, testbar): Spec → Kategorien (erste Pfad-Ebene,
   flache Optionen → „general“; Server vor Client) → Felder (Widget TOGGLE/INTEGER/DECIMAL/TEXT/CHOICE/LIST/JSON, Wert, Standard,
   Bereich, Neustart, geheim). Speichern = nur geänderte Werte über `Spec.set` (gleiche Klemm-/Speicherlogik wie der Befehl).
3. **Wertebereiche „soweit vorhanden“:** neues optionales `Builder.range(pfad, min, max)`; exemplarisch bei Hammers + Pads gesetzt.
   Kein Raten aus der Klemmlogik (würde die Live-Config verändern).
4. **Server-Autorität:** `access(...)`: Client-Config immer bearbeitbar; Server-Config bearbeitbar ohne Welt (eigene Datei,
   Titelbildschirm) und im Einzelspieler/LAN-Host nur mit Operator-Rechten (Stufe 2, wie `/<mod> config`); auf einem fremden Server
   nur lesbar + Hinweis auf `/<mod> config` (die lokale Datei ist dort nicht die des Servers). Speichern im Einzelspieler auf dem
   Server-Thread (`IntegratedServer.execute`). Geheime Optionen: `***`, nur lesbar.
5. **Cloth optional:** mit Cloth → Cloth-Builder (wie die vorhandenen Bildschirme), ohne → `VanillaConfigBuilder` in SimpleLib
   (Kopie der Forge-Vorlage, Beschriftung als weißer Text statt Knopf → Kontrast; Standard/Bereich nicht doppelt). Zwei kleine
   Adapter hinter einer `Form`-Schnittstelle, Bildschirmlogik nur einmal.
6. **Tooltips:** Lang-Tooltip, sonst keiner; Zeile „Standard: x“/„Default: x“ und „Bereich: a–b“ nur, wenn der Lang-Text sie nicht
   schon nennt; „Neustart nötig“ bei `restart`. Beschriftung: Lang-Schlüssel, sonst lesbarer Pfadname (camelCase → Wörter).
7. **Exemplarisch (Regel UI erst exemplarisch):** Hammers (flach, ohne Lang → Lang EN/DE ergänzt + Bereiche), Ores (flach, Lang da),
   Pads (Gruppen = Kategorien, Lang da, Bereiche). Ausgerollt ist der Baustein automatisch für alle Mods mit Spec (Auftrag: danach
   ausrollen) – es gibt keinen Code je Mod, der zurückgehalten werden könnte; Besitzer-Varianten nur zur Darstellung (Vorschau).
8. **compat26:** `Compat26ClientTest` öffnet zusätzlich die gelieferten Fabriken (`getProvidedConfigScreenFactories`).

## Dateien
- modules/simplelib/shared/java/com/simplelib/api/config/ConfigCommands.java (range, secret-Abfrage)
- modules/simplelib/shared/java/com/simplelib/api/config/ConfigScreenModel.java (neu)
- modules/simplelib/shared/java/com/simplelib/test/ConfigScreenTests.java (neu) + LibTests + fabric ModuleGameTest
- modules/simplelib/fabric/src/main/java/com/simplelib/config/screen/{GenericConfigScreen,Form,ClothForm,VanillaForm,VanillaConfigBuilder}.java (neu)
- modules/simplelib/fabric/.../SimpleLibModMenu.java; Lang simplelib EN/DE
- modules/simplehammers (Lang + range), modules/simplepads (range)
- integration/.../Compat26ClientTest.java
- docs/ai/RELEASE-SIMPLEBUILDING.md (L13 + f1–f6)

## Tests
Gametests (simplelib): `config_screen_model` (Spec → Kategorien/Widgets/Standard/Bereich/Auswahl), `config_screen_save` (nur
geänderte Werte, Klemmen, Datei, ungültige Eingabe), `config_screen_access` (Rechte-Tabelle), `config_screen_every_mod` (jede
geladene Mod mit Spec ergibt ein nicht-leeres Modell ohne Ausnahme). Lokal: javac gegen MC-26.3 (b-guide01-javac.sh),
check_lang_keys, check_tooltip_text, check_client_mixins, precheck --fast. EIN sbt: Compile + module-simplelib-fabric-263 +
module-simplehammers/-pads-fabric-263 + check_client_mixins. Client-Screenshot nur bei leerem sb-test (Regel 16: keine Client-Läufe
bis auf Weiteres) → sonst Mockup.

## Stand / Abweichungen
(wird beim Abschluss ergänzt)
