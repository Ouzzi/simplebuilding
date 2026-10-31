# Release-Audit Familie QoL-B (b-rel32-qolb, 31.10.2026)

Mods: Simple Tools, Simple Movement, Simple Farming, Simple Mob Manners, Simple Loot, Simple Weather (alle Sub-Mods von Simple Quality of Life). Nicht Teil: Super-Mod `simplequalityoflife` und `simpleinterfaces` (Agent b-rel32-qola). Basis b-rel32 d9cda1e21, nur MC 26.3 Fabric. Je Mod ein `modules/<mod>/AUDIT.md` mit Dateiliste, C1–C9, Befunden und Urteil.

## 1. Plan (Kopf)

- Reihenfolge: simpleweather, simpleloot (klein) -> simplemobmanners -> simplefarming -> simplemovement -> simpletools (gross, 19 Mixins).
- Je Mod gelesen: alle `.java` unter `shared/java`, `fabric/src`, `clienttest` (Tests nur auf Aufbau und Fehlerfaelle), `fabric.mod.json`, `*.mixins.json`, Lang EN/DE, Daten (Rezepte, Tags, Verzauberungen, Loot, Biome-Brush-Dateien), Modelle/Items-Definitionen; Texturen ueber die Pruefskripte. Dazu die genutzten Kern-/SimpleLib-Teile (`ConfigFiles`, `SubMods`, `TransformHints`, `IdMoves`, `SafeCodecs`, `ConfigCommands` – nur Aufrufstellen, keine Aenderung).
- Vorab billig geprueft ohne Gradle: `check_lang_keys.py` (6 Mods gruen), `check_mod_icons.py` (52 Mods gruen), je `modules/<mod>/tools/check_data.py` (gruen), `check_config_commands.py` (0 Probleme), `check_module_style.py` (OK), `check_gametest_adapters.py` (0 Luecken), `check_missing_textures.py` (0 Fehler).
- Test einmal am Ende in einem sbt-Lauf (Compile, 12 Test-Targets, Mixin-Pruefer, Lang-Pruefer); Ergebnis siehe Abschnitt 5.

## 2. Befund-Uebersicht

| Mod | P1 | P2 | P3 | Davon gefixt | Urteil |
|---|---|---|---|---|---|
| simpleweather | 0 | 0 | 1 (Besitzerfrage) | 0 | shipbar |
| simpleloot | 0 | 1 | 1 | 1 | shipbar |
| simplemobmanners | 0 | 0 | 3 | 0 | shipbar |
| simplefarming | 0 | 2 | 2 | 3 | shipbar |
| simplemovement | 0 | 1 | 2 | 2 | shipbar |
| simpletools | 0 | 0 | 7 | 2 | shipbar |
| **Summe** | **0** | **4** | **16** | **8** | |

Fix-Commits: `bb1592f6c` (kopierte Beschreibung loot/movement/farming), `e4a1919c3` (Farming-Tooltip, Gras-Schnitt-Reihenfolge, Soulbound-Schalter in Loot und Amboss), `ddd4d457c` (Tastenkategorie Movement).

## 3. Rückfragen an den Besitzer (echte Entscheidungen)

- **Q-B1 Cloth Config als harte Abhaengigkeit.** Alle sechs Mods verlangen `cloth-config` in `depends`, nur fuer den Einstellungs-Bildschirm (Client). Ein Dedicated Server muss damit Cloth Config installieren. Vorschlag: auf `recommends` herabstufen und den Screen nur laden, wenn Cloth da ist (betrifft alle Simple-Mods einheitlich, daher nicht einzeln geaendert).
- **Q-B2 Simple Weather: Standard-Regendichte 20 %.** Frisch installiert faellt Regen fuer alle Spieler nur noch mit einem Fuenftel Dichte (Darstellung, nicht Serverwetter). Vorschlag: Standard 100 und die Option als Wunsch beschreiben, oder 20 beibehalten und deutlich im Beschreibungstext nennen.
- **Q-B3 Simple Movement: Leiter-Standard doppelt so schnell wie Vanilla** (0.4 gegen 0.2, Rutschen 0.8). Gewollt als Standard, oder erst per Option?
- **Q-B4 Biome-Brush-Preis:** Rezept ohne Simple Mobs braucht 3 Nether-Sterne + Netherit-Nugget + Pinsel. Beabsichtigt?
- **Q-B5 Death-Vault-Rueckkauf (`deathVaultRedeem`)** ist ein halb fertiges „Future Feature“, standardmaessig aus. Im Release als „experimentell“ nennen, oder aus der Beschreibung weglassen?

## 4. Abnahmeplan (In-Game, Aktion -> erwartetes Ergebnis)

Voraussetzung: Fabric 26.3, Fabric API, Cloth Config; jede Mod einzeln (nur diese Mod + Abhaengigkeiten) UND einmal alle zusammen mit Simple Quality of Life. Befehle brauchen Operatorrechte (Stufe 2).

### Simple Weather (QOLB-WEA)

1. **QOLB-WEA-1** `/weather rain` in der Oberwelt -> Regen faellt, aber duenn (Standard 20 %): wenige Tropfen, kaum Bodenpartikel, leises Rauschen.
2. **QOLB-WEA-2** Mod-Menue -> Simple Weather -> Regendichte 100 speichern -> Regen sofort wie Vanilla.
3. **QOLB-WEA-3** Regendichte 0 -> kein Regen sichtbar/hoerbar, Himmel wird trotzdem dunkler (Server-Wetter laeuft).
4. **QOLB-WEA-4** `/simpleweather config set disableWeather true`, dann `/weather thunder` -> Regen und Gewitter verschwinden binnen eines Ticks, auch nach `/weather rain` wiederholt.
5. **QOLB-WEA-5** disableWeather wieder aus -> `/weather rain` wirkt wieder normal, Wetterzyklus laeuft weiter.
6. **QOLB-WEA-6** Mit Simple Quality of Life: Schalter „Enable Simple Weather“ aus -> Wetter und Regen wie Vanilla trotz Einstellungen.
7. **QOLB-WEA-7** Dedicated Server starten mit der Mod allein -> kein Absturz, Log ohne Client-Klassenfehler.

### Simple Loot (QOLB-LOO)

1. **QOLB-LOO-1** Prueftresor (`/give @s minecraft:vault`, platzieren, Ominous/Trial-Schluessel) einmal oeffnen -> Belohnung wie Vanilla.
2. **QOLB-LOO-2** `/time add 100d` (oder `/simpleloot vaults cooldown 1` und `/time add 2d`) -> der Tresor ist wieder „bereit“ (Partikel/Anzeige) und belohnt den Spieler erneut.
3. **QOLB-LOO-3** Vor Ablauf der Zeit denselben Schluessel einsetzen -> Tresor lehnt ab (Vanilla-Verhalten).
4. **QOLB-LOO-4** Welt speichern, neu laden -> Wartezeit bleibt erhalten (zweiter Versuch vor Ablauf wird weiter abgelehnt).
5. **QOLB-LOO-5** `/simpleloot vaults repeat false` -> Tresor belohnt nur einmal pro Spieler (Vanilla).
6. **QOLB-LOO-6** `/simpleloot config get vaultCooldownDays` -> Wert, Standard 100, Typ int; `set 0` wird abgelehnt oder auf 1 geklemmt.
7. **QOLB-LOO-7** Zweiter Spieler: eigener Zeitstempel, beeinflusst den ersten nicht.

### Simple Mob Manners (QOLB-MOB)

1. **QOLB-MOB-1** Kuh mit Namensschild „Berta_mute“ -> macht keine Geraeusche mehr (auch fuer Mitspieler).
2. **QOLB-MOB-2** `/simplemobmanners mutedEntities add minecraft:pig` -> alle Schweine stumm; `remove` -> wieder laut.
3. **QOLB-MOB-3** Ferkel/Kalb mit Namensschild „Fritz_baby“ -> bleibt Baby (auch nach Spielzeit/Fuettern); erwachsene Tiere mit diesem Suffix werden zum Baby (gewollt).
4. **QOLB-MOB-4** Piglin-Bastion mit goldener Hacke in der Hand betreten -> Piglins greifen nicht an; ohne Gold angreifbar.
5. **QOLB-MOB-5** Ruestung mit Gold-Rüstungsbesatz (Smithing) -> Piglins bleiben friedlich; `piglinsIgnoreGoldTrims false` -> sie greifen an.
6. **QOLB-MOB-6** Wassereimer auf einen Frosch (Rechtsklick) -> Frosch verschwindet, „Bucket of Frog“ im Inventar mit Variante; Rechtsklick auf Boden -> Frosch kommt zurueck (Variante, Name, Alter erhalten).
7. **QOLB-MOB-7** Leerer Eimer auf Frosch -> nichts passiert. Froscheimer im Kreativ-Menue (Werkzeuge, nach Kaulquappeneimer) und in JEI (Info-Seite) sichtbar.
8. **QOLB-MOB-8** `/simplecreatures muteSuffixes list` -> Hinweis „/simplecreatures heisst jetzt /simplemobmanners“ plus Liste.

### Simple Farming (QOLB-FRM)

1. **QOLB-FRM-1** Reifer Weizen, Hacke in der Hand, Rechtsklick -> Ernte faellt, Pflanze steht sofort als Setzling (kostet 1 Samen), Hacke -1 Haltbarkeit.
2. **QOLB-FRM-2** Unreife Pflanze/Fremdblock Rechtsklick -> nichts passiert (Hacke tilled wie Vanilla).
3. **QOLB-FRM-3** Karotten, Kartoffeln, Rote Bete, Netherwarze, Kakao ernten -> jeweils reif ernten und neu setzen.
4. **QOLB-FRM-4** Mit Mod `simplebuilding`: Hacke neigt sich leicht, wenn der Rechtsklick ernten wuerde (Hand-Hinweis).
5. **QOLB-FRM-5** Stiefel mit Feder-Fall auf Ackerboden springen -> Ackerboden bleibt; ohne Verzauberung wird er zertrampelt.
6. **QOLB-FRM-6** Schwert mit Schaerfe I–V auf hohes Gras schlagen/Mob dahinter: Pflanzenumriss verschwindet teilweise, Treffer geht durch (I ca. 10 %, V ca. 90 %), Gras im 3x3-Feld faellt als Drop.
7. **QOLB-FRM-7** Schleichen -> kein Gras-Schnitt, Umriss bleibt.
8. **QOLB-FRM-8** Schwung-Verzauberung am Schwert -> groesserer Schnittbereich (+1 Ring pro Stufe).
9. **QOLB-FRM-9** Schutzbereich (Claim-Mod): fremdes Grundstueck -> weder Ernte noch Schnitt wirken.
10. **QOLB-FRM-10** `/simplefarming hoeHarvest false` -> Hacke verhaelt sich wie Vanilla.

### Simple Movement (QOLB-MOV)

1. **QOLB-MOV-1** `/crawl` (oder gebundene Taste, Standard unbelegt) -> Spieler kriecht in Schwimmpose, ein zweites Mal -> steht wieder auf; Sprung beendet das Kriechen.
2. **QOLB-MOV-2** Zweiter Spieler sieht den Kriechenden (Pose synchron).
3. **QOLB-MOV-3** Autowalk: Standard aus -> Taste R tut nichts; `/simplemovement autowalk true` -> R schaltet Dauerlauf an/aus, Menue/Chat stoppt ihn.
4. **QOLB-MOV-4** Leiter hochlaufen -> doppelt so schnell wie Vanilla (0.4); `/simplemovement ladderSpeed 0.2` -> Vanilla.
5. **QOLB-MOV-5** Leiter runter: nach unten schauen (>45 Grad) + Schleichen -> schnelles Rutschen (Modus CAMERA); ohne Schleichen normal. Modus ALWAYS: umgekehrt.
6. **QOLB-MOV-6** Pulverschnee mit Frostlaeufer-Stiefeln -> man sinkt nicht ein; Schalter aus -> sinkt wie Vanilla.
7. **QOLB-MOV-7** Sprungkraft-Trank: Block hoch ohne zu springen (Stufe 1 Block); mit Stufenhoehe-Stiefeln (Kreativ/`/enchant`) -> hoechster Wert, nicht addiert.
8. **QOLB-MOV-8** Verzauberungen: `/enchant @s simplemovement:step_height 2` auf Stiefel (+Stufe), `simplemovement:sprint 3` auf Hose -> nur beim Sprinten schneller (+10/20/30 %).
9. **QOLB-MOV-9** Server-Anti-Cheat: Mit manipuliertem Client sehr schnell klettern -> wird zurueckgesetzt (Spieler wird auf Leiter zurueckgeteleportiert), legales Klettern nicht.
10. **QOLB-MOV-10** Tasten-Einstellungen: Kategorie heisst „Simple Movement“ (nicht „Simple Quality of Life“), Eintraege Auto Walk / Crawl.

### Simple Tools (QOLB-TLS)

1. **QOLB-TLS-1** Buch + Amboss (Handwerk, ungeformt) -> Handbuch (34 Seiten, EN/DE nach Spielsprache).
2. **QOLB-TLS-2** Ofen: Lavaeimer rechtsklicken (nicht schleichen) -> Lava landet im Brennstoff-Slot, Eimer verbraucht; mit leerem Eimer im Slot: Tausch; schleichen -> normales Lava-Platzieren.
3. **QOLB-TLS-3** Amboss: Reparatur mit Material/Zusammenlegen ohne neue Verzauberung -> Arbeitskosten steigen nicht weiter; Verzaubern -> steigen.
4. **QOLB-TLS-4** Sparsamkeit (Thrift, `simplequalityoflife:thrift`) am Enchanting Table/Haendler -> max. Haltbarkeit +33 % pro Stufe (Stufe III doppelt); nicht mit Reparatur kombinierbar.
5. **QOLB-TLS-5** Soulbound-Buch (`/give @s enchanted_book[stored_enchantments={"simpletools:soulbound":1}]`) im Amboss auf Werkzeug: Kosten 35 Level; Item nach Tod im selben Slot, Haltbarkeit hoechstens 50 (Option `deathDurabilityCap`).
6. **QOLB-TLS-6** Hardcore-Welt: Soulbound wirkt nicht, Tooltip erklaert es.
7. **QOLB-TLS-7** Todesgrab-Block (Creative > Funktional; Rezept: Obsidian, Enderauge, Seelensand/-erde, Truhe): Rechtsklick -> Fenster mit Drops des letzten Todes, farbige Rahmen (liegt / aufgehoben / verbrannt / verfallen / Void), Legende rechts, Timer-Balken.
8. **QOLB-TLS-8** Nach Tod: Items aufheben -> Rahmen wechselt auf „von dir aufgehoben“; Items in Lava -> „verbrannt“; 5 Minuten warten -> „verfallen“.
9. **QOLB-TLS-9** Biome-Pinsel (Rezept ohne Simple Mobs: 3 Nether-Sterne + Netherit-Nugget + Pinsel; sonst Nether-Herz): Pinsel in der Haupthand, Sand in der Nebenhand, Rechtsklick auf Boden -> Biom um den Klick wechselt zur Wueste (Partikel, Sand -1, Pinsel -1); Schleichen -> nur eine 4x4x4-Zelle.
10. **QOLB-TLS-10** Pinsel in der Nether-/End-Welt mit Oberwelt-Material -> Rauch, keine Aenderung; Tiefen-Dunkel/Tropfstein-Hoehlen werden nie ueberstrichen.
11. **QOLB-TLS-11** Item-Rahmen mit Item, schleichen: Schere -> Rahmen unsichtbar/sichtbar; Wabe oder Glasscheibe -> gesperrt (Item nicht drehbar/entnehmbar); Axt -> entsperrt; leerer Hand/anderes Item -> Item rueckwaerts drehen.
12. **QOLB-TLS-12** Voll-Haltbarkeits-Bonus: Standard-Multiplikator 1.0 (kein Effekt); `/simpletools config set fullDurabilityBonusMultiplier 1.5` -> neues Werkzeug (>= 80 % Haltbarkeit) baut 50 % schneller, Waffe macht 50 % mehr Schaden.
13. **QOLB-TLS-13** `/simpletools soulbound false` -> keine Soulbound-Wirkung, Kosten am Amboss normal; Haendler/End-City liefern kein Buch mehr.

## 5. Testergebnis (ein sbt-Lauf)

Befehl (ein sbt-Lauf, `/root/sbt b-rel32-qolb`): `./gradlew :mc26_3:fabric:compileJava :modules:<6 Mods>:fabric:compileJava -q` -> **C=0**; `tools/testrunner/run.py --targets module-<mod>-fabric-263,module-<mod>-standalone-fabric-263` (12 Targets) -> **T=1 wegen einer Infrastruktur-Stoerung, sonst 201/201 gruen**; `check_client_mixins.py` -> **M=0** (660 Mixins, 0 Probleme); `check_lang_keys.py` -> **L=0**.

| Mod | Integration | Standalone |
|---|---|---|
| Simple Weather | 5/5 | 5/5 |
| Simple Loot | 7/7 | 7/7 |
| Simple Mob Manners | 12/12 | 12/12 |
| Simple Farming | 13/13 | 13/13 (Wiederholung, s. u.) |
| Simple Movement | 19/19 | 19/19 |
| Simple Tools | 51/51 | 51/51 |

Das Target `module-simplefarming-standalone-fabric-263` endete im ersten Lauf mit 0 Tests: „Gradle build daemon disappeared unexpectedly“ (Daemon auf sb-test verschwunden; zeitgleich war sb-test kurz nicht erreichbar). Kein Testfehler; nur dieses Target wurde auf Commit ff9fdd1fa wiederholt: **13/13 gruen** (`testing/runs/2026-10-31T02-00-05Z-3971.json`). Erster Lauf: `testing/runs/2026-10-31T01-38-47Z-dc4c.json` (auf sb-test unter `/root/wt/t-b-rel32-qolb`).

Nicht getestet: echter Client-Start (`mixinaudit-client-263`, `xvfb-run`), Forge/NeoForge (Auftrag: nur Fabric), Zusammenspiel mit Sodium/Iris/Lithium/JEI/Mod Menu im Spiel, Mehrspieler/Dedicated-Server von Hand (nur die Gametest-Server), die In-Game-Schritte aus Abschnitt 4. Der Mixin-Pruefer (`check_client_mixins`) deckt die Ziele aller Client-Mixins gegen das 26.3-Spiel-Jar ab (Weather-Client-Mixins eingeschlossen).

## 6. Release-Punkte

Vorschlag Version fuer alle sechs: **1.0.0** (aktuell `0.1.0` in `modules.json`/`moduleVersion`; SimpleLib 0.3.0 ist Jar-in-Jar und erfuellt `>=0.3.0`). Lizenz CC0-1.0 (fabric.mod.json). Abhaengigkeiten je Mod: Fabric Loader >= 0.19.5, Minecraft ~26.3, Java >= 25, Fabric API, Cloth Config (siehe Q-B1); Mod Menu optional; Simple Quality of Life optional (suggests). Jar-in-Jar: Framework (SubMods) und SimpleLib.

fabric.mod.json-Befunde: (a) Beschreibungstexte von loot/movement/farming waren kopiert, gefixt; (b) `simpletools` ohne `simplelib`-Zeile in `depends` (nur Konsistenz); (c) `simpleweather` ohne `fabric-api`-Zeile (kommt ueber SimpleLib); (d) Icon `assets/<id>/icon.png` 16x16 vorhanden bei allen sechs.

### Changelog-Entwurf 1.0.0 (je Mod)

- **Simple Weather 1.0.0:** Wetter serverseitig abschaltbar; Regendichte (Darstellung) einstellbar 0–100 %; `/simpleweather config`; uebernimmt alte Simple-QoL-Einstellungen einmalig.
- **Simple Loot 1.0.0:** Tresore belohnen nach einer einstellbaren Wartezeit (Standard 100 Minecraft-Tage) erneut; pro Spieler; bleibt beim Neuladen erhalten; `/simpleloot vaults …`.
- **Simple Mob Manners 1.0.0:** Mobs per Typ oder Namensschild stumm; Babys mit Namensschild-Suffix bleiben klein; Piglins akzeptieren Gold-Besatz und Goldwerkzeug; neu: Eimer-Frosch. Aus „Simple Creatures“ umbenannt (Alias bleibt).
- **Simple Farming 1.0.0:** Hacke erntet und pflanzt neu; Schaerfe-Schwerter schneiden Gras; Feder-Fall schuetzt Ackerboden.
- **Simple Movement 1.0.0:** Kriechen auf Befehl/Taste; Autowalk (serverseitig erlaubt); schnellere Leitern und Rutschen; Frostlaeufer auf Pulverschnee; Sprungkraft = 1 Block Stufenhoehe; Verzauberungen Stufenhoehe und Sprint.
- **Simple Tools 1.0.0:** Voll-Haltbarkeits-Bonus, Verzauberungen Sparsamkeit und Seelengebunden, Todesgrab, Biome-Pinsel, Amboss-Reparatur ohne Mehrkosten, Lavaeimer in Oefen, bessere Item-Rahmen, Handbuch.

### Beschreibungen (Entwurf fuer Modrinth/CurseForge)

| Mod | Kurz (<= 100 Zeichen) | Lang |
|---|---|---|
| Simple Weather | Wetter abschalten und Regen ausduennen. | Schaltet auf Wunsch Regen und Gewitter auf dem Server ab und laesst dich die Regenintensitaet (Partikel, Geraeusche, Regenlinien) lokal von 0 bis 100 % regeln. Allein spielbar, Teil von Simple Quality of Life. Befehl `/simpleweather config`. |
| Simple Loot | Tresore belohnen wieder – nach einer Wartezeit. | Trial-Tresore geben jedem Spieler nach einer einstellbaren Zeit (Standard 100 Minecraft-Tage) erneut ihre Belohnung. Die Zeit wird pro Spieler und Tresor gespeichert. Abschaltbar, dann gilt Vanilla. Befehl `/simpleloot`. |
| Simple Mob Manners | Stumme Mobs, ewige Babys, Piglins mit Gold-Manieren, Frosch im Eimer. | Namensschild-Suffixe (`_mute`, `_baby`) oder Typlisten stellen Mobs stumm bzw. halten Babys klein; Piglins akzeptieren Gold-Besatz und Goldwerkzeug; Frosch per Wassereimer mitnehmen (Variante, Name, Alter bleiben). |
| Simple Farming | Hacke erntet und pflanzt neu, Schaerfe schneidet Gras. | Rechtsklick mit der Hacke erntet reife Pflanzen und setzt sofort neu; Feder-Fall verhindert Ackerboden-Zertrampeln; Schwerter mit Schaerfe schlagen durch Gras und schneiden es um das Ziel. Alles serverseitig und abschaltbar. |
| Simple Movement | Kriechen, Autowalk, schnelle Leitern, Stufenhoehe und Sprint. | Kriechen per Befehl/Taste, Autowalk (Server erlaubt), schnelleres Klettern und Rutschen mit serverseitiger Pruefung gegen Cheats, Frostlaeufer auf Pulverschnee, Sprungkraft hebt Stufenhoehe, zwei neue Verzauberungen (Stufenhoehe fuer Stiefel, Sprint fuer Hosen). |
| Simple Tools | Haltbarkeits-Bonus, Todesgrab, Seelenbindung, Biome-Pinsel und mehr. | Werkzeug-Helfer fuer Fabric: Bonus bei voller Haltbarkeit, Sparsamkeit und Seelengebunden (Items bleiben im Slot beim Tod), das Todesgrab zeigt, was aus deinen Drops wurde, der Biome-Pinsel malt Biome, Amboss-Reparatur ohne Mehrkosten, Lavaeimer in Oefen, bessere Item-Rahmen (unsichtbar, sperren, rueckwaerts drehen). |

### Release-Checkliste (Anteil QoL-B)

- [x] Code-Durchsicht aller sechs Mods, AUDIT.md je Mod
- [x] Beschreibungen der Mods korrigiert (loot/movement/farming)
- [ ] Besitzer-Entscheidungen Q-B1…Q-B5
- [ ] Version 1.0.0 setzen (`moduleVersion`, `modules.json`) – erst nach Freigabe
- [ ] Gate/Bundle, echter Client-Start (`mixinaudit-client-263`, ausserhalb dieses Laufs), Abnahme Abschnitt 4
- Dev-Schalter: `DevVariants.DEFAULT_ON` kommt in diesen sechs Mods nicht vor (Grep sauber); keine Testzentralen/Dev-Menues in diesen Mods.

## 7. Restpunkte (nicht gefixt)

Siehe AUDIT.md je Mod: simpletools-P3-3/-4/-5/-6/-7, simplemovement-P3-2, simplefarming-P3-2, simplemobmanners-P3-1/-2/-3, simpleloot-P3-1, simpleweather-P3-1 (Q-B2). Keiner blockiert ein Release; die meisten betreffen seltene Randfaelle oder gemeinsame Generatoren.
