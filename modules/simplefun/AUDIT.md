# Audit simplefun (a-audsb31e1, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplepads/AUDIT.md`.
Umfang: 55 Java-Dateien / 3,5k Zeilen (shared + fabric, ohne Tests; Tests zusaetzlich 5 Dateien / 1,3k Zeilen). Risikogeleitet geprueft
(P1-Klassen zuerst: Projektile/Schutz, Statik, Mixins). Zusatzeingabe BEW39 (`/root/sb-tools-a/bewertung39/simplefun.json`) wurde
gegen den Code geprueft.

## Vollstaendig gelesen
- Mixins: `YeetMixin`, `KnockbackCapMixin`, `ItemMixin`, `NoDamageMixin`, `PiggyFoodMixin`, `PlayerInteractMixin`, `DelightTickMixin`,
  `EnchantabilityMixin`, `fabric/.../PlayerEntityMixin`, Mixin-Konfiguration `simplefun.mixins.json` (Client-Mixins nur im Abschnitt `client`).
- Projektile/Gegenstaende/Bloecke: `BrickProjectileEntity`, `BrickSnowballItem`, `WaterBalloonEntity`, `SkippingStoneEntity`,
  `PaperPlaneEntity`, `ToyThrowItem`, `BubbleWandItem`, `PartyPopperItem`, `RollItem`, `PaperPlaneItem`, `RubberDuckItem`, `RubberDuckBlock`.
- Logik: `toys/FunToys` (ganz), `FunDelights`, `SimplefunConfig`, `SimplefunCommon`, `SimplefunCommands`, Registrierung (`ModToys`, `FunContent`,
  `ModEffects`, `ModEnchantments`, `SimplefunRegistry`, `ModEntities`, `ModItems`), `SimplefunFabric`, `FunFabricData`, `ModMenuIntegration`.
- Daten: `data/minecraft/enchantment/knockback.json`, `data/simplefun/enchantment/no_damage.json`, Tag `knockback_allowed`, Loot `rubber_duck`,
  `fabric.mod.json`.
- Nicht gelesen: `FunGuide`, `FunDiscover`, `FunJeiPlugin`, `FunConfigScreen`, Client-Renderer/Partikel (`PigHeadFeatureRenderer`,
  `SoapBubbleParticle`, `WornPigHeadMixin`, Render-State-Mixins), Lang/Texturen/Modelle, Forge/NeoForge, Tests (nur Muster gelesen).

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigenen Payloads. Alle Spielzeuge laufen ueber Vanilla-Pakete (Benutzen, Entitaetsklick, Fallenlassen), Entscheidung nur auf dem Server; Wuerfel-/Muenzwurf rechnet der Server, Chat an Spieler im Umkreis 16 Bloecke, Abklingzeit 1 s. |
| C2 Server-Autoritaet | Alle Wirkungen serverseitig (`ServerLevel`/`ServerPlayer`-Pruefungen); Client-Klassen nur in `client/` und im `client`-Abschnitt der Mixin-Konfiguration. Luftklick-Platzierung der Ente prueft nun selbst die Bauerlaubnis (**P2-2**). |
| C3 Dupes | Kein Dupe gefunden: Wurf-Gegenstaende verbrauchen vor dem Spawn genau einen (`stack.consume`), Papierflieger landet genau einmal (`land()` prueft `isRemoved`), Ziegel `shrink(1)` nur auf dem Server, Bubble-Wand kostet je Blase 1 Haltbarkeit. Kreativ verbraucht nichts (wie Vanilla). |
| C4 NBT/Komponenten | Kein eigener Speicher. Papierflieger-Notiz: Lore, hoechstens 200 Zeichen (`PLANE_NOTE_MAX`), nur Klartext (kein Klickereignis). Config: `normalize()` begrenzt alle Fliesskommawerte (NaN/unendlich/Bereich), kaputte Datei wird in Quarantaene gelegt. |
| C5 Tick-Kosten | `DelightTickMixin` ruft je Spieler und Tick `FunDelights.tick` + `FunToys.tick`; `FunDelights.enabled` liest den Schalter per Reflection (**P3-4**). Schneeengel: ~40 Partikelpakete alle 10 Ticks nur fuer den Spieler mit lebendem Engel (hoechstens ein Engel je Spieler). Konfetti 40 Einzelpartikel je 3 s. |
| C6 Leaks | Statische Zustandstabellen sind alle `WeakHashMap` mit dem Spieler/der Entitaet als Schluessel (`FunToys` Z. 203-205, 357, 396-397, `FunDelights.NEXT`, `BrickProjectileEntity.NEXT_THROW`). Der Wert `Angel` haelt den `ServerLevel` (Z. 394); der Schluessel wird nach dem Abmelden unerreichbar (der Level verliert den Spieler), kein Zyklus ueber die Spielzeit hinaus (**P3-6**). |
| C7 Sicherheit/Rechte | Befehl `/simplefun` nur `LEVEL_GAMEMASTERS`. Weltaenderungen: Ziegel-Glasbruch geschuetzt (`mayInteract`, Weltgrenze, Standard aus, nur Einzelspieler); **Wasserbombe und Ente hatten keine Pruefung (P2-1, P2-2, gefixt)**. Mixins: `KnockbackCapMixin` ohne Super-Mod-Schalter (**P3-1**), `EnchantabilityMixin` nicht abschaltbar (laeuft beim Item-Aufbau). |
| C8 Unabhaengigkeit | Allein spielbar (Standalone-Ziel `modules/simplefun/fabric/build.gradle`, SimpleLib per Jar-in-Jar, `fabric.mod.json` `depends simplelib`). Cloth Config nur ueber `isModLoaded`-Schutz im Mod-Menu-Einstieg. Metadaten-Abweichung `modules.json` (**P3-7**). |
| C9 Doku/Pflege | Keine TODO/FIXME. 7 Kern-Dubletten-Paare in `tools/dup_pairs.txt` (Registry-Listen, `PlayerEntityMixin`; das Paar `mixin/client/ItemMixin` des Kerns ist ein anderer Mixin gleichen Namens - Fehlpaarung, **P3-8**). Lang EN=DE (`check_data.py` gruen). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplefun-P2-1 | **P2 (Schutz/Griefing)** | `WaterBalloonEntity#splash`/`#douse` (WaterBalloonEntity.java:84-146): Die Wasserbombe loescht Feuer, brennende Kerzen und Lagerfeuer im 3x3x3-Bereich und fuellt einen getroffenen Kessel (`setBlock`/`setBlockAndUpdate`) ohne jede Bauerlaubnis. Wer aus dem Spawnschutz, von ausserhalb eines Claims oder nach dem Abmelden (Besitzer weg) wirft, veraendert fremde Bloecke; die Ziegel-Glasbruch-Logik (BrickProjectileEntity.java:136-141) prueft dagegen `mayInteract`. | **gefixt**: `FunToys#mayChange` (FunToys.java:82): nur ein echter Spieler als Besitzer, `ServerPlayer#mayInteract` (Spawnschutz, Weltgrenze) und alle Claim-Anbieter (`Protection`, wie `PlacedTemplates` in Simple Trims); ohne Besitzer aendert die Bombe keinen Block (Dampf/Plaetschern bleibt). Tests: `toy_water_balloon_protection` (neu, ohne Fix rot), `toy_water_balloon` (Besitzer statt `null`). |
| simplefun-P2-2 | **P2 (Schutz)** | `RubberDuckItem#use` (RubberDuckItem.java:20-30): Der Luftklick auf eine Wasserflaeche legt den Entenblock per `BlockItem#useOn` direkt, ohne Spawnschutz-/Claim-Pruefung. Der Paket-Handler prueft `isUnderSpawnProtection`/`mayInteract` nur bei Blockklicks (`handleUseItemOn`), nicht bei `handleUseItem` (Luftklick). | **gefixt**: derselbe `FunToys#mayChange` fuer den Zielblock (`hit.getBlockPos().above()`), Ergebnis `FAIL`. Test `toy_rubber_duck_protection` (neu, ohne Fix rot). |
| simplefun-P2-3 | **P2 (Logik/Rueckfrage)** | Knockback-Ueberschreibung `data/minecraft/enchantment/knockback.json:24,32` (BEW39 ROT): Vanilla-Knockback wird ersetzt (`max_level` 5 statt 2, `supported_items` `#simplefun:knockback_allowed` = Federn, Staebe, Schwerter, Aexte, Streitkolben, Dreizack, Bogen, Armbrust, Speere) und laedt **ohne Bedingung**. Der Schalter `enableHigherKnockback` (SimplefunConfig.java:25) begrenzt nur die Wirkung (`KnockbackCapMixin.java:12`: 2 statt `maxKnockback`), nicht die Daten; Stufe III-V bleibt am Zaubertisch/Amboss erhaeltlich. Der Deckel `maxKnockback` ist hart auf 4 begrenzt (SimplefunConfig.java:80), also wirkt Stufe V wie IV. Auch bei abgeschaltetem Super-Mod bleibt die Ueberschreibung (Daten sind nicht schaltbar). | **Rueckfrage** (Besitzer), nicht geaendert: siehe Besitzer-Fragen F1. |
| simplefun-P3-1 | P3 | `KnockbackCapMixin` (KnockbackCapMixin.java:9-15) prueft `SimplefunCommon.on()` nicht: bei abgeschaltetem Super-Mod gilt der Deckel weiter (4 bzw. 2), auch fuer fremde Angreifer mit grosser `attack_knockback`. Auf `EnchantmentHelper#modifyKnockback` fuer alle Entitaeten. | Hinweis (mit F1 mitloesen) |
| simplefun-P3-2 | P3 (A12) | Standard-an-Eingriffe in Vanilla-Verhalten (BEW39 orange): Wurf beim Schleichen+Q mit Staerke 3 (`YeetMixin.java:25-40`, `SimplefunConfig.java:18-19`), Rechtsklick mit Ziegel/Netherziegel/Harzziegel wirft (`ItemMixin.java:25-31`, Standard an), Schweinefleisch loest 5 Minuten Schweinekopf aus (`PiggyFoodMixin.java:25-31`), Federn/Staebe sind verzauberbar und Feder-Nahkampf macht 0 Schaden (`EnchantabilityMixin`, `NoDamageMixin`), Streicheln/Abklatschen belegen Schleichen+leere Hand auf eigene Tiere/Spieler (`PlayerInteractMixin`, ersetzt das Sitzen-Umschalten beim Schleichen). Dazu kommt `pickUpDelay` 20 statt 40 bei geworfenen Gegenstaenden (YeetMixin.java:39). | **Rueckfrage** F2 (kein Fehler) |
| simplefun-P3-3 | P3 | Papierflieger-Notiz (`FunToys#readNote`, FunToys.java:336-345) geht beim Lesen als Systemnachricht in den Chat (`readNote`) und umgeht damit Chat-Stummschaltung/-Meldung; der Text kommt aus Buechern beliebiger Spieler (bis 200 Zeichen). | Hinweis |
| simplefun-P3-4 | P3 (Tick) | `FunDelights.enabled` (FunDelights.java:24-35) nutzt `getField(...)` je Aufruf; `FunToys.on("snowAngel")` laeuft in `FunToys.tick` fuer jeden Spieler jeden Tick (FunToys.java:416). Bei 100 Spielern 2000 Reflection-Aufrufe/s - messbar klein, aber unnoetig. | Liste: `Field` je Schluessel zwischenspeichern |
| simplefun-P3-5 | P3 | `ItemMixin` gibt `SUCCESS` zurueck, auch wenn `throwFrom` wegen Abklingzeit/Schalter nichts tat (Arm schwingt, kein Wurf); `BrickSnowballItem` ebenso. Nur Optik. | Hinweis |
| simplefun-P3-6 | P3 | `FunToys.Angel` haelt den `ServerLevel` als Wert einer `WeakHashMap` (Schluessel Spieler). Loest sich nach dem Abmelden (Level haelt den Spieler nicht mehr); bei Weltwechsel im Einzelspieler bleibt der alte Level so lange, bis der Spieler eingesammelt ist. | Hinweis |
| simplefun-P3-7 | P3 (Metadaten) | `modules/modules.json` `requires: ["cloth_config"]` (BEW39), `fabric.mod.json` verlangt dagegen `simplelib` und kein Cloth (Q-B1); MODSTRUKTUR-Text/Store-Seite nachziehen. | Hauptagent (`modules.json` nicht in meinem Umfang) |
| simplefun-P3-8 | P3 | 7 Kern-Dubletten (`tools/dup_pairs.txt`, Registry-Listen, `PlayerEntityMixin`); Eintrag `mixin/client/ItemMixin.java` -> `simplefun/mixin/ItemMixin.java` meint zwei verschiedene Mixins (Tooltip vs. Ziegelwurf). Die Fixes P2-1/P2-2 betreffen nur Modul-Klassen ohne Kern-Paar. | Liste (Paar bereinigen) |

## Besitzer-Fragen (fuer die Vorschau AUDSB31E1)
**F1 Knockback bis Stufe V (simplefun-P2-3).** Kontext: Die Daten-Ueberschreibung von `minecraft:enchantment/knockback` laedt immer; der Schalter `enableHigherKnockback` und der Super-Mod-Schalter aendern sie nicht, und der harte Deckel 4 macht Stufe V gleich IV.
- A: Ladebedingung wie die vorhandene `simplefun:trades_enabled` (`FunFabricData`): Ueberschreibung nur laden, wenn Simple Fun an **und** `enableHigherKnockback` an ist; `max_level` auf 4 (passend zum Deckel) oder Deckel auf 5.
- B: wie A, aber `enableHigherKnockback` Standard **aus** (Besitzer schaltet bewusst ein; Vanilla bleibt Standard).
- C: lassen wie jetzt, A12-Steckbrief (starker PvP-Eingriff, kann mit Datenpaketen kollidieren) im Handbuch/Store-Text, Stufe V = IV offen sagen.
- Empfehlung: **A mit `max_level` 4** - der Schalter muss auch die Daten steuern (sonst ist er irrefuehrend), keine neue Mechanik; ob Standard an oder aus bleibt dem Besitzer (B).

**F2 Standard-an-Eingriffe (simplefun-P3-2, A12).** Kontext: Fuenf Eingriffe aendern Vanilla-Verhalten ohne Opt-in; am stoerendsten fuer Bauende ist der Ziegelwurf, weil Rechtsklick mit Baumaterial wirft (Schaden 2, Abklingzeit 0,5 s).
- A: alle Standard **aus** (Opt-in ueber Config/Befehl).
- B: nur den Ziegelwurf Standard aus (oder nur mit Schleichen+Rechtsklick), Yeet/Schwein/Gesten an, A12-Satz im Store-Text.
- C: alles an lassen, A12-Saetze im Store-Text ergaenzen.
- Empfehlung: **B** - der Ziegelwurf kollidiert mit dem Bauen (Rechtsklick mit Ziegeln), die uebrigen sind kosmetisch oder haben Gegenwirkung (Yeet-Deckel 1,5 Bloecke/Tick, Gesten nur mit leerer Hand).

## Urteil
**shipbar nach Fix** (0 P1, 3 P2: 2 gefixt, 1 Rueckfrage F1; 8 P3). Das Modul ist durch Deckel, Abklingzeiten, `WeakHashMap`-Zustand und Gametests (42 Tests vor diesem Audit) solide; die zwei Schutzluecken (Wasserbombe, Ente) sind geschlossen.

## Testergebnis
Lokal: javac ueber `/root/sb-tools-a/audsb31e1/javac.sh simplefun` fuer `FunToys`, `WaterBalloonEntity`, `RubberDuckItem`, `FunToyTests`, `ModuleGameTest` - keine Fehler in geaenderten Dateien (nur die bekannten Access-Widener-Meldungen in `FunContent`, `FunTests`, `LibMaterials`); `modules/simplefun/tools/check_data.py` gruen. sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: die neuen Gametests wurden nicht ausgefuehrt (kein Gradle); "ohne Fix rot" ist aus dem Code abgeleitet (Besitzer `null` und Claim-Anbieter vor dem Fix ohne Wirkung). Nicht geprueft: echter Claim-Anbieter (nur der Framework-Hook `Protection` mit Testanbieter), Client (Spielzeug-Optik/Ton, BEW39 "ohne Client-Beleg"), Forge/NeoForge.
