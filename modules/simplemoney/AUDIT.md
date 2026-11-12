# Audit simplemoney (a-audsb31e1, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric; Forge/NeoForge nicht Teil des Releases. Umfang: 18 Java-Dateien / 1488 Zeilen
ohne Tests (shared + Fabric), dazu Daten (8 Rezepte, 47 Handelsangebote, 380 Money-Link-Preise in 20 Tabellen). Risikogeleitet geprueft
(P1-Klassen zuerst: Money-Links-Mixins, Handels-Ergebnis-Slot, Budget); Aufbau wie `modules/simplestorage/AUDIT.md`.
Zusatzeingabe BEW39 (`/root/sb-tools-a/bewertung39/simplemoney.json`): rot (1) Preistabellen im Handbuch veraltet -> gefixt (P3-1),
rot (2) Papierraketen-Rezept -> Besitzer-Frage 1; orange Balance-Punkte -> Besitzer-Frage 2.

## Vollstaendig gelesen
- Mixins: `mixin/LinkedOfferMixin.java` (getCostA), `mixin/LinkedTradeMixin.java` (VillagerTrade#getOffer), `mixin/LinkedResultMixin.java`
  (mayPickup, onTake), `mixin/LinkedMenuMixin.java` (quickMoveStack), `mixin/WanderingTraderLinksMixin.java`, `simplemoney.links.mixins.json`.
- Logik: `MoneyLinks.java` (Preise, `allowed`), `LinkBudget.java` (SavedData, Budget/Abklingzeit), `SimpleMoney.java` (Config, Schalter,
  Flags), `MoneyItems.java`, `MoneyLoot.java`, `WeightedEnchantFunction.java`.
- Fabric: `MoneyFabric.java`, `MoneyCondition.java`, `MoneyGameTest.java`, `MoneyModMenu.java`, `client/MoneyLinksJei.java`, `fabric.mod.json`.
- Daten/Werkzeuge: `tools/generate_links.py`, `tools/check_data.py`, Preistabellen `shared/.../money/*/prices.json`, erzeugte Angebote
  `generated/.../villager_trade/` (Stichproben armorer/toolsmith/wandering_trader), `recipe/rocket_from_paper.json`, `money_bill_from_blasting.json`,
  `wiki/manual.json` (Money-Links-, Konfigurations- und Raketen-Eintraege).
- Stichproben: `testing/LinkTests.java#menu` (Schnellverschieben-Schutz), `client/MoneyConfigScreen.java` (nur lokaler Entwurf).
- Nicht gelesen: `guide/MoneyGuide.java`, `discover/MoneyDiscover.java`, uebrige Tests, `clienttest/`.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigenen Payloads; nur Vanilla-Handel. Config-Befehl `/simplemoney config` ueber SimpleLib, Stufe 2. |
| C2 Server-Autoritaet | Preis, Budget, Abklingzeit, Schalter entscheidet nur der Server (`LinkedResultMixin`/`LinkedMenuMixin` nur fuer `ServerPlayer`); der Client-Config-Bildschirm schreibt nur einen lokalen Entwurf. |
| C3 Dupes / Arbitrage | Link-Angebote werden beim Erzeugen normiert (1 Item, Preis aus Tabelle, Vorrat <= 4, XP 0, Preisfaktor 0; `LinkedTradeMixin`); `getCostA` liefert den Grundpreis (kein Ruf-/Heldenrabatt, keine Nachfrage; `LinkedOfferMixin`); Ergebnis-Slot: `mayPickup` prueft Budget/Schalter, `quickMoveStack` (umgeht Vanilla `mayPickup`) ebenso, die Vanilla-Schleife endet an der Abklingzeit (>= 20 Ticks); Budget wird erst nach bezahltem Handel gebucht (`onTake` an `notifyTrade`). Keine Rueckkauf-Kanten (Generator verbietet `sell`), `check_data.py` prueft Salvage-Gewinn ueber Rezeptketten. Handels-Ergebnis-Slot sonst Vanilla. |
| C4 NBT | `LinkBudget`: Eintraege begrenzt (100k, Zaehler `intRange`), volle Tabelle raeumt Eintraege > 1 Tag ab. |
| C5 Tick-Kosten | keine Tick-Methoden; `SimpleMoney.enabled` liest bei jedem Aufruf das Datei-Datum (P3-5). |
| C6 Leaks | keine statischen Spieler-Maps (`byItem` nur Item -> Preis). |
| C7 Sicherheit / Mixins | 5 Mixins, alle `@Inject` (kein `@Overwrite`), wirken nur auf Angebote mit Geldschein-Preis und Tabellen-Item; bei abgeschaltetem Super-Mod-Schalter tun sie nichts (P3-4). Ladebedingungen `simplemoney:config` fuer jedes Angebot. |
| C8 Unabhaengigkeit | Pflicht nur SimpleLib (`fabric.mod.json`); Links nur fuer geladene Mods (`fabric:all_mods_loaded`, Enderit braucht Simple Ores); Standalone-Ziel `runSimplemoneyStandaloneGameTest` vorhanden. `modules.json` nennt noch `requires: [cloth_config]` (P3-8, Hauptagent). |
| C9 Doku / Pflege | Lang EN=DE (76 Keys); Handbuch-Preistabellen veraltet (P3-1, gefixt), Konfig-Eintrag "keine Befehle" falsch (P3-2, gefixt); 24 verwaiste erzeugte Angebote (P3-3); Kern-Dublette `WeightedEnchantFunction` (nur Paket-Kommentar verschieden, `check_dup_impl` 0 Probleme). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplemoney-P3-1 | P3 (Doku, BEW39 rot) | `wiki/manual.json` listete 18 Preistabellen: 6 leere ("0 item prices": simplemodels, simplequalityoflife, simpleriding, simplesounds, simpletweaks, simplevisuals), 8 echte fehlten (simpleblocks, simpleend, simpleenddragonfight, simplegadgets, simplelib, simpleores, simplesandwiches, simplestorage), simplebuilding nannte 148 statt 126 Preise (63 alte Eintraege, u. a. laengst verschobene Items, 35 neue fehlten), simplemachines 4 statt 7, "moved on 04.11./07.11." veraltet. Einen Generator fuer diese Eintraege gibt es nicht (`tools/generate_links.py` schreibt nur Daten). | **gefixt**: die 20 Eintraege `money_links_<mod>` EN+DE neu aus `shared/.../money/<mod>/prices.json` + den erzeugten Angeboten (Preis = `wants.count` der geladenen Datei), leere Tabellen ohne Eintrag (Satz im Eintrag `money_links` angepasst); 380 Preise, Quellen nur noch existierende Dateien. `tools/check_data.py` und `generate_links.py --check` gruen. Kein Gametest (reine Doku). |
| simplemoney-P3-2 | P3 (Doku) | Eintrag `configuration` behauptete "keine Befehle", es gibt `/simplemoney config` (Stufe 2, eigener Handbuch-Eintrag). | **gefixt** EN+DE |
| simplemoney-P3-3 | P3 (Pflege) | 24 erzeugte Angebote unter `generated/.../villager_trade/links/simplebuilding/` gehoeren zu Items, die laengst in Sub-Mods liegen (z. B. `amethyst_lens` -> `simplegadgets`, Rucksaecke/Truhen -> `simplestorage`); kein Tag nennt sie, `generate_links.py` loescht alte Ausgaben nicht und `--check` erkennt ueberzaehlige Dateien nicht. BEW39s "19 Scheine fuer die Amethystlinse" stammt aus so einer Leiche (lebendes Angebot: `links/simplegadgets/amethyst_lens.json`). | Hinweis: Generator soll nicht erzeugte Dateien im `links/`-Ordner loeschen bzw. in `--check` melden, dann die 24 Dateien weg |
| simplemoney-P3-4 | P3 | Super-Mod-Schalter aus: `MoneyLinks.price` liefert `null` (`MoneyLinks.java:52-55`), `allowed` dann `true` (`:78-80`) - schon vorhandene Link-Angebote an Dorfbewohnern sind ohne Budget/Abklingzeit kaufbar (nur durch ihren Vorrat begrenzt). Der Config-Schalter `links.enabled` sperrt dagegen korrekt. | Hinweis (bei aus: Link-Angebot = Geldschein-Preis + Tabellen-Item ablehnen) |
| simplemoney-P3-5 | P3 (Last) | `SimpleMoney.enabled` (synchronized) fragt bei jedem Aufruf das Aenderungsdatum der Config-Datei ab (`SimpleMoney.java:45-56`), auch bei jedem `mayPickup` eines Link-Ergebnisses (`MoneyLinks.java:82`). | Hinweis (Datei nur bei Reload/Befehl lesen) |
| simplemoney-P3-6 | P3 (Regel 14) | Alle 7 Items tragen eine reine Stimmungs-Tooltipzeile (+1 Zusatzzeile Spezialfaser, `MoneyItems.java:34-37`, `tooltip.simplemoney.*`), die Tooltip-Regel erlaubt nur Bedienung/Zustand; `check_data.py` verlangt die Keys. | Hinweis (mit dem Tooltip-Aufraeumen der Familie) |
| simplemoney-P3-7 | P3 | `WeightedEnchantFunction`: `level`/`weight` ungebunden (`Codec.INT`), Pool ohne Laenge (`WeightedEnchantFunction.java:26-36`); negative Gewichte verfaelschen die Auswahl. Nur Datapacks betroffen. Kern-Dublette `common/.../loot/WeightedEnchantFunction.java`. | Hinweis (`ExtraCodecs.POSITIVE_INT`, beim 26.2-Port mitziehen) |
| simplemoney-P3-8 | P3 (Metadaten) | `modules.json` `requires: [cloth_config]`, `fabric.mod.json` verlangt nur SimpleLib (Cloth optional seit b-cloth26); `fabric.mod.json` `suggests` (`:42-54`) nennt 6 Mods mit leerer Preistabelle und 15 Mods mit Tabelle nicht. | Hinweis an Hauptagent (`modules.json` + `modtree.py`) |

BEW39 "missing" nicht gebaut (Antwort des Besitzers steht aus): Client-Bild Haendlerfenster, eigener Gametest der Buch-Gewichtung, JEI mit
Preis/Haendler, Abnahme CL-5.

## Besitzer-Fragen (Vorschau AUDSB31E1)
1. **Papierraketen-Rezept.** `recipe/rocket_from_paper.json`: 7 Papier + 2 Schwarzpulver -> 1 Rakete; Vanilla macht aus 1 Papier + 1 Schwarzpulver
   3 Raketen. Das Rezept ist immer schlechter und passt nicht zu einer Geld-Mod. Optionen: **A** entfernen (Rezept, Rezept-Advancement,
   Handbuch-Eintrag `rocket_recipe`, Erwartung in `MoneyTests.recipeOutputs`); **B** so lassen; **C** aendern (z. B. 7 Papier + 2 Pulver -> 3 Raketen
   Flugdauer 3). Empfehlung **A**: kein Spielnutzen, Vanilla deckt Raketen ab, weniger Pflege.
2. **Balance gesammelt (BEW39 orange).** (a) Ankauf: Ruestungsschmied 15 Diamanten -> 2 Scheine (`villager_trade/armorer/25.json`), Werkzeugschmied
   7 -> 2 (`toolsmith/39.json`); (b) fahrender Haendler verkauft Knospenden Amethyst (14-18 Scheine, 3x) und 4 Tiefenschiefer-Diamanterz (5-6 Scheine,
   7x) - starker Eingriff in Vanilla-Knappheit (A12), nur mit allen Haendler-Angeboten zusammen abschaltbar (`enableWanderingTrades`); (c) Roh-Schein
   brennt 24000 Ticks (20 min) im Schmelzofen. Optionen: **A** alles lassen (gewollte Senke/Seltenheit); **B** angleichen: einheitlicher Ankauf
   (z. B. beide 10 -> 2), (b) aus dem Standard nehmen (eigener Schalter, Standard aus), Brennzeit 6000 Ticks; **C** nur (b) entschaerfen, Kurse und
   Brennzeit lassen. Empfehlung **C**: (b) ist der einzige starke Vanilla-Eingriff (PRINZIPIEN A12), Kurse und Brennzeit sind Geschmack und mit
   SB-Oefen ertraeglich.

## Urteil
**shipbar** (0 P1, 0 P2, 8 P3 - davon P3-1 (BEW39 rot) und P3-2 gefixt; 2 Besitzer-Fragen). Die Money-Links sind gegen Arbitrage,
Rabatt-Umgehung und Schnellverschieben-Umgehung abgesichert und getestet (`linkMenu`, `linkNoArbitrage`, `linkBudgets`).

## Testergebnis
Lokal: keine Java-Aenderung (kein javac noetig). `modules/simplemoney/tools/check_data.py` gruen (7 Items, 8 Rezepte, 47 Angebote, 380 Preise,
Salvage-Audit), `tools/generate_links.py --check` gruen, `tools/check_tooltip_text.py` gruen. Das Handbuch wurde mit einem einmaligen Skript
(`regen_money_links_manual.py`, nur im Scratchpad des Laufs) aus Tabellen + erzeugten Angeboten neu geschrieben; `wiki/generate.py` muss der
Hauptagent laufen lassen (wiki/data/simplemoney.json nennt noch die alten Eintraege). sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: Gametests (kein Gradle), Client/JEI, Haendlerfenster im Spiel, NeoForge.
