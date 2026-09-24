# Loot-Balance (Stand 2026-09-24)

Gilt für beide MC-Linien (26.2 und 1.21.11) und alle Loader; Quelle ist
`loot/ModLootTableModifications.java` (auf beiden Linien identisch), Handel in
`src/main/resources/data/simplebuilding/villager_trade` (26.2) bzw.
`trade/ModTradeDefinitions.java` (1.21.11). Schalter: `enableLootTableChanges`,
`enableVillagerTrades`, `enableWanderingTrades` – unverändert.

## Leitlinien

- **Menge pro Kiste**: Strukturen mit vielen Kisten (Mine, Mansion, Ancient City,
  gewöhnliche Bastion-Kisten, Verlies) geben im Schnitt ~0,5 Mod-Stapel pro Kiste.
  Einzelkisten (Bastion-Schatzraum, Stronghold-Bibliothek, Buried Treasure, End City)
  dürfen mehr geben. Vorher: Mansion ~2,3, Ancient City ~1,4, Bastion ~1,2 pro Kiste.
- **Kerne** (Netherstern im Rezept) sind überall Gewicht 1–2 und nie in Massen-Kisten
  mit hoher Wurfzahl; der Netheritkern liegt nur noch im Bastion-Schatzraum.
- **Vielfalt**: Diamantkiesel (9 → Rissiger Diamant → Diamant) als häufiger, kleiner
  Füller; End-Rohstoffe in der End City; neue Quellen Portalruine und Angeln.
- **Vergleich Vanilla**: Netherite-Template 100 % im Bastion-Schatz, 10 % sonst →
  Enderit-Template 30 % pro End-City-Kiste (vorher 50 %). Verzauberter Goldapfel
  ~1/60 pro Wurf in Ancient City → verzauberte Netherit-/Enderit-Äpfel Gewicht 1 von 58–84.
  Angel-Schatz: Vanilla-Buch ~1/6 der Schatzfänge → Mod-Bücher ~9/33.
- **Brücke** bleibt ohne Truhenquelle (Verzauberung hat derzeit keine Wirkung).

## Kisten (Würfe; Einträge mit Gewicht, Anzahl; Ø Mod-Stapel/Kiste)

| Tabelle | Würfe | Inhalt | Leer | Ø |
|---|---|---|---|---|
| Stronghold-Bibliothek | 0–2 | Reichweite II 4, Baumeister 3, Vielseitigkeit I 4 / II 2 | 12 | 0,52 |
| End City | 15 % / 30 % / 1 / 0–3 | Schrott; Template; Roh-Enderit 4 (1–2), Enderit-Nugget 6 (2–5), Astralitstaub 6 (2–6), Nihilith-Splitter 6 (1–4) [leer 14]; Reichweite III 4, Baumeister 3, Übersteuerung II 5, Luftsprung II 5, Vielseitigkeit I 6 / II 3, Diamant-Baustab* 6, Diamant-Vorschlaghammer* 8, Enderit-Apfel 3, verz. Enderit-Apfel 1 | 40 | 1,88 |
| Ancient City | 0–2 | Tiefe Taschen II 5, Radius 4, Oktant* 5, Diamant-Vorschlaghammer 3, Köcher* 3, Netherit-Apfel 2, verz. Netherit-Apfel 1, Netherit-Nugget 4 (1–3), Diamantkiesel 6 (2–5) | 25 | 0,57 |
| Bastion (alle Kisten) | 0–2 | Trichter I 5, Durchbruch I 5, Gold-Vorschlaghammer 6, Goldkern 1, Netherit-Nugget 12 (1–4), Netherit-Karotte 6 (1–2) | 25 | 0,58 |
| + nur Bastion-Schatz | 1 | Netheritkern 2, Netherit-Apfel 4, verz. Netherit-Apfel 2, Durchbruch II 3 | 7 | +0,61 |
| Netherfestung | 0–2 | Tunnelgräber I 6 / II 3, Trichter 2, Durchbruch 2, Goldkern 1, Oktant* 3, Netherit-Nugget 6 (1–3), Netherit-Karotte 3 (1–3) | 14 | 0,65 |
| Plünderer-Außenposten | 0–2 | Farbpalette 6, Abdeckung 8, Linear 8, Oktant 5, Köcher 5, Kupfermeißel 4 | 20 | 0,64 |
| Waldanwesen | 0–2 | Farbpalette 3, Abdeckung 5, Linear 5, Aderabbau V 1 / IV 3, Eisen-Baustab 4, Eisenkern 1, Köcher 3 | 30 | 0,45 |
| Buried Treasure | 0–2 | Berührung d. K. 3, Schnelles Meißeln II 2, Goldmeißel 10, Diamantmeißel 6, Diamantkiesel 10 (2–6) | 30 | 0,51 |
| Verlies | 0–2 | Schnelles Meißeln I 5, Trichter 8, Durchbruch 8, Aderabbau IV 3 / III 8 / II 12, Verst. Bündel 8, Basis-Template 2, Diamantkiesel 6 (1–3) | 40 | 0,60 |
| Schiffswrack-Schatz | 0–1 | Schnelles Meißeln I 10, Verst. Bündel 8, Diamantkiesel 10 (1–4) | 20 | 0,29 |
| Iglu | 0–1 | Berührung d. K. 3, Schnelles Meißeln I 3, Diamantmeißel 6 | 8 | 0,30 |
| Verlassene Mine | 0–2 | Schnelles Meißeln I 2, Tunnelgräber I 8 / III 3, Aderabbau III 4 / IV 3, Verst. Bündel* 6, Diamantkiesel 8 (1–3) | 30 | 0,53 |
| Vault normal (+selten) | 0–1 | Berührung d. K. 3, Schnelles Meißeln II 2, Diamantkiesel 3 (2–4) | 12 | 0,20 |
| Vault unheilvoll (+selten) | 0–1 | Baumeister 10, Luftsprung I 7, Diamantkern 2, Netherit-Apfel 2, verz. Netherit-Apfel 1 | 35 | 0,19 |
| Portalruine (neu) | 0–1 | Netherit-Nugget 3 (1–2), Goldmeißel 3, Netherit-Karotte 2 | 12 | 0,20 |
| Angeln, Schatz (neu) | 1 | Schnelles Meißeln I 3, Berührung d. K. 2, Tiefe Taschen I 2, Linear I 2, Diamantkiesel 4 (1–3) | 20 | 0,39 |

`*` = zufällig verzaubert (EnchantRandomlyFunction). Der seltene Vault bekommt beide Vault-Pools.

## Handel

| Änderung | Vorher | Nachher | Grund |
|---|---|---|---|
| Rabattfaktor Bibliothekar 3/4/5 | 0,3 / 0,5 / 1,0 | 0,2 | 1,0 senkte den Preis nach wenigen Käufen/einer Heilung auf 1 Smaragd |
| Rabattfaktor Hämmer (Schmied 4), Spitzhacke (Schmied 5) | 0,5 / 0,8 | 0,2 | Vanilla-Wert für verzauberte Werkzeuge |
| Diamantkern (Steinmetz 2) | 6 Netheritbarren | 3 Netheritbarren | 6 Barren lohnten sich gegenüber Rezept (4 Diamanten + Netherstern) nie |
| Radius-Buch (fahrender Händler) | 60 Smaragde | 40 Smaragde | einzige Handelsquelle für Radius, sonst nur Ancient City |
| neu: 3 Diamantkiesel (fahrender Händler, häufig) | – | 5 Smaragde, 4×, Rabatt 0,05 | ~15 Smaragde pro Diamant, nicht zurückverkaufbar |

Arbitrage: Jedes Item, das ein Händler ankauft (Oktant 8, Verst. Bündel 12), kostet beim
Kauf mehr (10 bzw. 16). Test: `TradeAndMigrationTests.modTradesStayWorthItWithoutBeingExploitable`.
Kisten-Budget: `ConfigOptionTests.lootBalanceKeepsEveryChestWithinItsBudget` (Ø-Bänder pro
Tabelle, Schatzraum-Items nicht in gewöhnlichen Bastion-Kisten).
