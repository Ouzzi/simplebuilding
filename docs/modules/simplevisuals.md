# Simple Visuals – Portinventar und Vertrag (26.3)

Quelle nur gelesen: `C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/simplevisuals`,
HEAD `099a45af84ff6c3478ebcd6a8e8f05278f5fa835`, Fabric 1.21.11, Version 1.0.7.
Die Quelle hatte bereits eine uncommittete Änderung an `gradle.properties`; sie blieb erhalten.
35 Java-Dateien, kein eigener Testkatalog. README, Modrinth-README, TODO und Implementierung
wurden verglichen. Quellcode gewinnt bei abweichenden README-Aussagen.

## Vollständiges Funktionsinventar

| Quellfunktion | 26.3-Umsetzung und Grenzen |
|---|---|
| Geschwindigkeitslinien | Farbe, Deckkraft, Dichte, Innenradius, Breite, Tempo, Länge, Schwelle; Egoansicht, höchstens 60 Linien. Tickbasierte Animation statt FPS-abhängiger Alterung. |
| Aufnahme-HUD | Item/XP, Position, Skalierung, Dauer, Seite, vier Layouts, Symbol/Name/Anzahl, Seltenheitsfarbe und Hintergrundstil; acht Zeilen, 64 Cacheeinträge. Nur bestätigte Vanilla-Aufnahmeereignisse, keine erfundenen Inventarzuwächse. |
| Elytra-Neigung | Zwei Winkel (-40/+40), Toleranz und Empfindlichkeit; reine Anzeige, keinerlei Flug-/Geschwindigkeitsbonus. |
| Effekt-Zeitleisten | HUD und Inventar, größte beobachtete aktive Dauer, unendliche Effekte ohne Leiste, Verlauf beim Weltwechsel zurücksetzen. |
| Spieler-Locator | Köpfe statt Vanilla-Symbolen, ausschließlich bereits vom Server übermittelte Wegpunkte; Winkel, Pfeile, Unsichtbarkeit und Leistenpriorität bleiben Vanilla. Kein eigener Positionsscan. |
| Chat-Köpfe | Erkannte Spielernachrichten und vorhandene Vanilla-Skins; kein eigener Downloaddienst. |
| Todesnachrichten | Configschlüssel/Enums erhalten; aus, lokal anhängen oder lokale separate Zeile. Datenschutzkorrektur: nur eigene Todeskoordinaten, nie fremde Positionen oder öffentliche Chatpakete. |
| Karten-Tooltip | 128-Pixel-Vorschau sowie Mittelpunkt und Dimension aus bereits synchronisierten Kartendaten. Nur echte MapItems, kein Überschreiben fremder Blueprint-/Container-Tooltips. |
| Gehaltenes Item | Haltbarkeit und begrenzte Verzauberungsliste über der Hotbar; Komponentenänderung erneuert die Vanilla-Namensanzeige. Kein Gadget-Aktionsleisten-/Chattext. |
| Schadenszahlen | Beobachtete Lebensverluste naher sichtbarer Lebewesen; 32 Anzeigen, 40 Ticks, Reichweite 16 Blöcke, maximal 128 inspizierte/32 verfolgte Entities. Farbe über acht Schaden ist eine Heuristik, kein Beweis für Bann/Kritisch. Feste Blöcke verdecken Zahlen. |
| Biomhinweis | Standard aus; Wechsel, Anzeigezeit, Versatz, Wiedereintrittssperre; begrenzter Verlauf. |
| Ambossformatierung | Gültige `&`-/`§`-Codes serverseitig validiert; abschaltbar; 50-Zeichen-Grenze, Steuerzeichenfilter und Vanilla-Kosten. Vorherige Nutzungen aus Reparaturkosten, wenn kein Ergebnis vorliegt. |
| CIT-Kompatibilität | JSONs unter `assets/<namespace>/cit/*.json`: `item`, `name`, `model`, optional `id`, `tags`, `weight`; exakte Namen, deterministische höchste Gewichtung. Alte Modellpfade werden in aktuelle Vanilla-Itemdefinitionen übersetzt. Keine echte Itemkomponente wird geändert. |
| Lose PNGs | `config/simplevisuals/textures/*.png`, Vanilla-Stick; `stick_<name>` wird zu `<name>`. Maximal 128 Regeln, 64 KiB pro JSON, 1 MiB pro PNG, 256×256 Pixel; Headerprüfung vor nativer Dekompression, keine Symlink-Dateien. |
| Modellbrowser | Amboss-Schaltfläche, Suchfeld nach Namen/Tags, fünf Spalten, 15 Vorschauen je Seite, benannte Treffer und Amboss-Umbenennungsanleitung. Keine eigene C2S-Modellauswahl. |
| Config | Alter Pfad `config/simplevisuals.json` und alle 49 ursprünglichen aktiven Skalarfelder bleiben; zusätzlich globale Partikelstufe und zwölf Overrides. Cloth-Tabs, EN/DE, Default in jedem Tooltip, finite Zahlen und feste Grenzen. Mod Menu auf Fabric, Mods-Config auf NeoForge. |
| Befehle | Lokale kosmetische Optionen: `/simplevisuals config <alter Feldpfad als Wörter> [Wert]`. Alte Aliases (Locator/Statusleisten/Chat/Elytra/Speed/Pickup) einschliesslich `elytraHelper angles <up> <down>` und `visuals pickupNotifier offset <x> <y>` ebenfalls vorhanden. Serverregel: `/simplevisuals config visuals enableAnvilFormatting [true|false]`, nur Admin. Quelle hatte entgegen README keinen generischen Config-Unterbaum und keinen M-Keybind. |

Die Quelle registriert **keine Items, Blöcke, Blockentities, Mobs, Enchantments, Rezepte,
Beutetabellen, Tags, Advancements, Kreativtabs oder Keybinds**. Alte Sprachreste für
Spawn-Teleporter, Launchpad, Brick Snowball, Geld und Gameplay-Config sind tote Kopierreste;
sie werden nicht als doppelte Inhalte registriert. Die generierte Scaffold-Marke wurde entfernt.
Darum sind JEI-Rezepte/Jade-Blockprovider/Advancement-Hinweise hier nicht anwendbar.
Alle Visuals haben stattdessen zweisprachige Config-Hinweise und Wiki-Kapitel.

Quell-TODO nennt unzuverlässige Aufnahmehinweise und nicht sichtbare Schadenszahlen.
Der Port verwendet begrenzte Vanilla-Ereigniscaches und echte Client-Lebenswertbeobachtung
statt des fehleranfälligen `setHealth`-Hooks und unbeschränkter Listen/FPS-Alterung.

## Kollisionen und Zuständigkeit

- SimpleBuilding-Pads/Gadgetfeedback/XP-Orb-Skalierung bleiben unverändert. Kein Nachbau
  der toten Sprachreste; keine Registry-/Rezept-/Tag-/Keybind-Kollision.
- Beide Mods erweitern HUD/Itemdarstellung. Simple Visuals hängt eigenen HUD-Inhalt an,
  ergänzt Itemnamen und Statussymbole und ersetzt nur das Locator-Symbol; SimpleBuildings
  Spawn-Elytra/Luftsprung/XP-Kontextpriorität entscheidet weiterhin über die Leiste.
- Karten-Vorschau bleibt auf Vanilla-MapItems begrenzt; Blueprint-/Rucksack-Tooltips behalten
  ihre Daten. Kein Import einer fremden Implementierungsklasse, nur öffentliche Registry-IDs
  im Cross-Mod-Test.
- Geplantes Simple Models übernimmt Modellindividualisierung. Sobald `simplemodels` geladen
  ist, deaktiviert Simple Visuals seinen Legacy-CIT-Handler. Alte Configschlüssel bleiben lesbar;
  JSON-/PNG-Pfade sind für die spätere Migration dokumentiert. Keine alten Welt-IDs betroffen,
  denn CIT hat nie Gameplay-Items registriert. Die Bibliothek soll nicht doppelt gepflegt werden.
- Death APPEND war in der Quelle ebenfalls nur lokale Darstellung, kein öffentliches
  Serverfeature. Fremde Todeskoordinaten werden hier absichtlich nicht aus Clientpositionen ergänzt.
- Die einzige gemeinsame Gameplay-Änderung ist die serverseitig abschaltbare Ambossformatierung.
  Eine eigene Serverpolicy ist auch im integrierten Server von der Clientconfig getrennt;
  die GUI zeigt diese Serveroption schreibgeschuetzt.
  Alle HUD-/Partikelwerte bleiben lokal; kein Clientpaket beeinflusst Tempo, Reichweite, Inventare,
  Claims, Weltgrenzen, Chunks, Entities oder Handel. Kein Duplikations-/Arbitrageweg vorhanden.

## Partikelvertrag für Simple Sounds

**Eine maßgebliche Registry:** `modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json`.
`EffectRegistry` lädt diese Datei; Simple Sounds soll ihre IDs und Kategorien spiegeln.
IDs sind persistent und werden nicht aus Übersetzungen abgeleitet. Vanilla-Typen, keine neuen
Partikeltexturen. Stufen: **Off, Subtle, Normal, Strong, Maximum** / **Aus, Dezent, Normal, Stark,
Maximum**. Globalstandard Subtle; je Effekt entweder globale Stufe oder ausdrückliches Override,
auch Off. Pro Auslösemöglichkeit 0/1/2/3/4; gemeinsame Grenzen können die tatsächliche Zahl senken.
Über `framework` `CosmeticIntensity` veröffentlicht Visuals die globale Stufe und (ab 0.1.2) die
Effekt-Overrides (`registerEffects`); Simple Sounds folgt damit je Effekt. Die Stufen-Enums werden
über ausdrückliche `switch`-Zuordnungen (`Intensity#shared`) umgesetzt, nicht über Konstantennamen.

| Stabile ID | Kategorie | Vanilla-Typ | Intervall (Ticks) |
|---|---|---|---|
| `footstep_dust` | `movement` | `minecraft:poof` | 4 |
| `cold_breath` | `weather` | `minecraft:white_smoke` | 40 |
| `fireflies` | `nature` | `minecraft:firefly` | 40 |
| `pollen` | `nature` | `minecraft:wax_on` | 40 |
| `fire_sparks` | `environment` | `minecraft:small_flame` | 20 |
| `water_ripples` | `water` | `minecraft:splash` | 8 |
| `water_droplets` | `water` | `minecraft:dripping_water` | 12 |
| `leaf_fall` | `nature` | `minecraft:cherry_leaves` | 30 |
| `enchanted_items` | `magic` | `minecraft:enchant` | 20 |
| `beacon_aura` | `magic` | `minecraft:end_rod` | 20 |
| `damage_feedback` | `reactive` | `minecraft:damage_indicator` | 1 |
| `healing_feedback` | `reactive` | `minecraft:heart` | 1 |

Harte Caps: 24 zusätzliche Partikel je Clienttick, vier je Spieler, acht berücksichtigte Spieler,
16 Blöcke Entfernung. Reaktionen verbrauchen das lokale Spielerbudget. Vanilla Decreased senkt
das globale Budget auf acht; Minimal schaltet alle zusätzlichen Partikel aus. Kein Erz-/Entityscan,
keine Chunkloads; Umgebungsbedingungen prüfen je Abfrage sechs bereits geladene Nachbarblöcke.
Effektpriorität rotiert; verfolgte Gesundheit und UI-Verläufe verfallen bei Weltwechsel.
`particles.globalLevel` und `particles.overrides.<effect_id>` bleiben der Datenvertrag.

## Configfelder (Originalnamen und Defaults)

| Feld | Default | Tab |
|---|---|---|
| `particles.globalLevel` | `SUBTLE` | particles |
| `visuals.enablePlayerLocator` | `true` | general |
| `visuals.enableChatHeads` | `true` | general |
| `visuals.enableStatusEffectBars` | `true` | general |
| `visuals.enableElytraPitchHelper` | `true` | general |
| `visuals.elytraTargetAngleUp` | `-40.0` | general |
| `visuals.elytraTargetAngleDown` | `40.0` | general |
| `visuals.elytraPitchTolerance` | `10.0` | general |
| `visuals.elytraSensitivity` | `4.0` | general |
| `visuals.enableRenamedItemTextures` | `false` | general |
| `visuals.enableAnvilFormatting` | `true` | general |
| `visuals.enhanceDeathMessages` | `true` | general |
| `visuals.deathCoordsMode` | `SEPARATE` | general |
| `visuals.enableMapTooltips` | `true` | general |
| `visuals.damageIndicators.enable` | `true` | damage |
| `visuals.damageIndicators.scale` | `1.0` | damage |
| `visuals.damageIndicators.colorNormal` | `16777215` | damage |
| `visuals.damageIndicators.colorSpecial` | `16766720` | damage |
| `visuals.damageIndicators.showBorder` | `true` | damage |
| `visuals.biomeInfo.enable` | `false` | biome |
| `visuals.biomeInfo.displayDuration` | `60` | biome |
| `visuals.biomeInfo.cooldownSeconds` | `60` | biome |
| `visuals.biomeInfo.yOffset` | `50` | biome |
| `visuals.speedLines.enableSpeedLines` | `true` | speed |
| `visuals.speedLines.speedLinesColor` | `16777215` | speed |
| `visuals.speedLines.speedLinesAlpha` | `0.7` | speed |
| `visuals.speedLines.speedLinesAmount` | `1.0` | speed |
| `visuals.speedLines.speedLinesRadius` | `0.7` | speed |
| `visuals.speedLines.speedLinesWidth` | `8.0` | speed |
| `visuals.speedLines.speedLinesSpeed` | `1.0` | speed |
| `visuals.speedLines.speedLinesScale` | `4.0` | speed |
| `visuals.speedLines.speedThreshold` | `0.6` | speed |
| `visuals.heldItemTooltips.enable` | `true` | tooltips |
| `visuals.heldItemTooltips.showDurability` | `true` | tooltips |
| `visuals.heldItemTooltips.showEnchantments` | `true` | tooltips |
| `visuals.heldItemTooltips.maxEnchantments` | `3` | tooltips |
| `visuals.pickupNotifier.enablePickupNotifier` | `true` | pickup |
| `visuals.pickupNotifier.pickupNotifierOffsetX` | `10` | pickup |
| `visuals.pickupNotifier.pickupNotifierOffsetY` | `10` | pickup |
| `visuals.pickupNotifier.pickupNotifierScale` | `1.0` | pickup |
| `visuals.pickupNotifier.pickupNotifierDuration` | `120` | pickup |
| `visuals.pickupNotifier.pickupNotifierShowXp` | `true` | pickup |
| `visuals.pickupNotifier.pickupNotifierSide` | `RIGHT` | pickup |
| `visuals.pickupNotifier.pickupNotifierLayout` | `COUNT_ICON_NAME` | pickup |
| `visuals.pickupNotifier.pickupShowItem` | `true` | pickup |
| `visuals.pickupNotifier.pickupShowName` | `true` | pickup |
| `visuals.pickupNotifier.pickupShowCount` | `true` | pickup |
| `visuals.pickupNotifier.pickupUseRarityColor` | `true` | pickup |
| `visuals.pickupNotifier.pickupVanillaStyle` | `true` | pickup |
| `visuals.pickupNotifier.pickupBackgroundOpacity` | `1.0` | pickup |

Numerische Grenzen stehen zentral in `ConfigOptions.bounds`; UI und Dateileser verwenden dieselbe
Policy. Farben 0..0xFFFFFF, Winkel -90..90, Toleranz 0..45, Empfindlichkeit 0..10, Deckkraft 0..1,
Liniendichte 0..2, Radius 0,2..1, Breite 0,25..8, Linientempo/-länge 0..4, Temposchwelle 0,05..4,
Skalierung 0,5..2, Verzauberungen 0..8, Anzeigezeit 20..600, Sperre 0..3600 s, Versatz 0..4096.
NaN/Infinity erhalten den Default, unbekannte Effekt-IDs und null-Overrides werden verworfen.

## Produzentendaten, Launch Hub und Tests

Manifest enthält vollständige Pfade/Abhängigkeiten, Loaderversion 1.0.7, eigene Kataloge,
Datenhook und Client-Entrypoint. Keine modulspezifische Änderung an Rootbuild, Runner,
Launch Hub oder gemeinsamer Clientmetadatei. `newmod.py` aktualisierte nur zusätzlich die
Integration-Auswahl. Die Hub-Mods-Seite und Testziele werden daraus automatisch entdeckt.
Tunables: benannte Caps in `ParticleBudget`/`AssetBounds`, Intervalle/Typen in der Registry,
Configdefaults und Grenzen in Java. Balanceablage `balance/simplevisuals`; keine bestehende
SimpleBuilding-Historie migriert. Eigenes Manual mit 76 Kapiteln, eigene EN/DE-Dateien,
eigener Generated-Pfad (keine Gameplay-Datagen-Inhalte nötig).

Serverziele: `module-simplevisuals-fabric-263`, `module-simplevisuals-neoforge-263`.
18 Fälle je Loader: Launch/Registries/Command, alle Configgrenzen und alte JSONs,
Namen/Tooltips/Tabs/Defaults/Sprachen, Gesamt-/Spieler-/Vanilla-Partikelcaps, echtes Amboss-
Cross-Mod-Ergebnis/Kosten/Server-Off, Dekompressionsgrenzen und zwölf Effekt-Stufenpolicies.
Integration lädt SimpleBuilding mit. Eigener Fabric-Clienttest prüft Titel→Welt,
vier Aufnahmelayouts, Queuecaps, echte Haltbarkeit/Effektprogress/Kartenbild,
alle Vanilla-Partikeltypen, minimale Einstellung, Speed-Linien, lokale Commands,
CIT/Renderkopie/Gewicht, Modellbrowser, Amboss und Config; fünf Screenshotpunkte.
Kein Server-GameTest behauptet, einen Client gerendert zu haben.

## Bekannte Grenzen und zurückgestellte Ports

- Gesundheitszahlen beruhen auf dem tatsächlich synchronisierten Clientwert; unbekannte
  Mobgesundheit kann nicht angezeigt werden. Maximal 32 nahe beobachtete Entities, kein X-ray.
- Pollen/Blätter/Feuer/Beacons verwenden absichtlich wenige angrenzende Blockproben,
  keinen großen Radius. Die Partikeltypen bleiben Vanilla; Maximal ist weiterhin begrenzt.
- Die lokale CIT-Bridge ist Übergangskompatibilität; beliebige alte Custom-Modelle können
  Vanilla-Modellformatkorrekturen benötigen. Gleichnamige Regeln gewinnen deterministisch
  nach Gewicht und ID. Keine versteckten UUIDs oder kostenlose Umbenennung.
- Forge 26.3: Loader-Metadaten, Cloth/Config-Bindung, Clientbefehle und Testadapter sowie
  echte Clientdarstellung ergänzen; deaktiviertes Scaffold ist kein veröffentlichter Forgeport.
- 26.2, 1.21.11 und 26.4: ausdrücklich eigener Port-Run nach Besitzerfreigabe; keine Dateien
  dieser Linien verändert. Gemeinsame SimpleBuilding-26.2-Kompilierung bleibt Gateanforderung.
- Besitzerwelt, echte Upgrade-Welt, vollständige SimpleBuilding-Clientsuite und beliebige
  Modpackkombinationen benötigen separate Abnahme. Keine neue Pixelkunst; Quell-Icon erhalten.

## Verifikation

- Voller 26.3-Serverlauf `2026-09-30T15-55-16Z-1186`: **1599/1599 gruen**,
  Fabric und NeoForge je 781 Bestandsfaelle, Integration 1, Modul je 18.
  Testzentrale aufgebaut, Plan und komplette Item-/Blockabdeckung auf beiden Loadern gruen.
- Nach Trennung der Serverpolicy: `2026-09-30T16-04-11Z-4d2f`, **41/41 gruen**
  (36 Modul-Serverfaelle und 5 Fabric-Client-Screenshotpunkte).
- Strengere PNG-IHDR-Pruefung: `2026-09-30T16-07-59Z-af75`, **2/2 gruen**
  mit einem Filtermuster `simplevisuals:*asset*`, beide Loader.
- Letzter Client-Smoke `2026-09-30T16-09-15Z-48f0`: **5/5 gruen**.
  Screenshots nach Resourcepack-Reload visuell kontrolliert; Ambossknopf ueberlagert JEI nicht.
  Vorschauen: `modules/simplevisuals/previews/` (Titel, Welt, Modelle, Amboss, Config).
- `gradlew.bat check -q --no-daemon`: **Exit 0**, auch nach den letzten Codekorrekturen.
  Wiki generiert und geprueft mit Standardaufruf sowie `--all` / `--all --check`.
  Datencheck: 50 vollstaendige Skalaroptionen, 12 stabile Effekte, 76 bilinguale Kapitel.
- Keine Serverfaelle des Moduls ausgelassen. Der Client-Smoke beweist gezielte Assertions
  und die fuenf Oberflaechen; er ersetzt keine visuelle Mehrspielerabnahme von Chatkoepfen,
  Locatorpfeilen oder jedem Umgebungseffekt. NeoForge-Clientdarstellung, Simple-Models-
  Zusammenspiel, echte Upgrade-/Besitzerwelt und Lastverhalten auf schwacher Hardware bleiben
  ungetestet. Forge/deferierte Linien sind kein Bestandteil dieses Ports.
- Keine gemeinsame Implementierung oder Testinfrastruktur erweitert. Eigener Manifesteintrag
  liefert alle Ziele; Integration-Auswahl durch Scaffold, Wiki-Katalog automatisch generiert.

## Experimental Forge 26.3
Opt-in `-Pforge263=true` adapter: shared cosmetic mixins, local commands, server formatting policy, JSON configuration, and server tests. Same catalogue IDs and shared bodies; module-owned loader hooks and isolated test world. No speculative optional Forge dependencies. Cloth GUI unavailable; existing server JSON settings retained. Forge client, real multiplayer and optional integrations remain unverified.

Forge catalogue: **18/18, alles gruen**, `2026-09-30T17-07-50Z-5834`. Launch, configuration/legacy/language, particle budgets, decompression bounds, server anvil policy and twelve effect contracts passed. Forge client commands and Simple Models detection are client-only; no Cloth GUI or Fabric screenshot harness is compiled into Forge. Rendered effects/HUD and real mod combination still require acceptance.
