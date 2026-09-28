# Baukerne: wie selten sind sie? (Stand 2026-09-28)

Analyse fuer den Besitzer. **Keine Chance wurde geaendert** - die Vorschlaege am Ende sind nur
Vorschlaege. Herstellung ueber den Netherstern (Wither) ist wie gewuenscht ausgeklammert.

## 1. Quellen im Code

Truhen und Tresore (`common/src/shared/java/com/simplebuilding/loot/ModLootTableModifications.java`,
Konstanten `*_CORE_CHANCE`; jeder Kern hat einen eigenen Pool mit genau einem Wurf `binomial(1, p)`,
also hoechstens ein Kern je Kiste):

| Kern | Beutetabelle | Chance je Kiste bzw. Oeffnung |
|---|---|---|
| Kupferkern | - (keine Kiste) | 0 |
| Eisenkern | `chests/woodland_mansion` | 0,8 % |
| Goldkern | `chests/bastion_other` und `chests/bastion_treasure` | 0,6 % |
| Goldkern | `chests/nether_bridge` (Netherfestung) | 0,8 % |
| Diamantkern | `chests/trial_chambers/reward_ominous` (unheilvoller Tresor) | 0,8 % je Oeffnung |
| Diamantkern | `chests/trial_chambers/reward_rare` | 0,8 % je Wurf; der normale Tresor wuerfelt `reward_rare` mit 80 % (Vanilla `reward.json`: rare 8 zu common 2), also **0,64 % je normaler Oeffnung** |
| Netheritkern | `chests/bastion_treasure` (eine Kiste je Schatz-Bastion) | 4 % |
| Enderit-Kern | `chests/end_city_treasure` (Stadt und Schiff) | 0,25 % |

Die Brueckenkisten (`bastion_bridge`) und die Hoglin-Staelle (`bastion_hoglin_stable`) der Bastion
bekommen keinen Kern - nur `bastion_other` und der Schatzraum.

Haendler (nicht Teil der Frage, aber fuer die Einordnung wichtig; `src/main/resources/data/simplebuilding/villager_trade/`,
1.21.11: `ModTradeDefinitions`):

| Kern | Haendler | Preis | Nutzungen | Wie oft im Angebot |
|---|---|---|---|---|
| Kupferkern | Steinmetz Stufe 2 | 25 Smaragde | 2, frischt auf | 2 aus 4 Eintraegen: 50 % je Steinmetz |
| Kupferkern | fahrender Haendler (haeufig) | 23 Smaragde (seit 2026-09-28 je 1 statt 2 fuer 46) | 8 (vorher 4 x 2) | 5 aus 79: ~6,3 % je Haendler |
| Eisenkern | fahrender Haendler (haeufig) | 28 Smaragde (vorher 2 fuer 56) | 8 (vorher 4 x 2) | ~6,3 % je Haendler |
| Goldkern | fahrender Haendler (selten) | 30 Smaragde | 1 | 2 aus 19: ~10,5 % je Haendler |
| Diamantkern | Steinmetz Stufe 2 | 3 Netheritbarren | 2, frischt auf | 50 % je Steinmetz |

Die Tausche des fahrenden Haendlers gaben vorher 2 Kerne auf einmal. Weil Kerne jetzt nicht mehr
stapeln (Stapelgroesse 1), gibt jeder Tausch einen Kern zum halben Preis, mit doppelt so vielen
Nutzungen - Stueckpreis und Gesamtmenge je Haendler bleiben gleich.

## 2. Modell: wie oft oeffnet ein Spieler solche Kisten?

Angenommen ist ein Spieler, der die jeweilige Struktur **gezielt** sucht (Mittel- bis Endspiel,
ausgeruestet, weiss wohin). Die Raten sind Schaetzungen; wer andere annimmt, rechnet mit derselben
Formel neu: Kerne je Stunde `λ = p x Kisten je Stunde`, erster Kern im Mittel nach `1/λ`
(Median `0,69/λ`, 90 % der Spieler nach `2,3/λ`), zehn Kerne im Mittel nach `10/λ`.

| Struktur | Annahme | Kisten bzw. Oeffnungen je Stunde |
|---|---|---|
| Waldanwesen | 1 Anwesen je 2,5 h (Erkunderkarte, weite Reise), ~20 Truhen | 8 |
| Bastion | 1,5 Bastionen je Stunde, ~4 `bastion_other`-Truhen, jede 4. hat den Schatzraum | 6 + 0,375 Schatz |
| Schatz-Bastion gezielt | 1 Schatz-Bastion je 1,5 h | 0,67 |
| Netherfestung | 1 Festung je 45 min, ~3 Truhen | 4 |
| Pruefungskammer | 1 Kammer je 1,5 h, ~6 normale Tresore; ~2 unheilvolle, wenn man unheilvolle Pruefungen macht | 4 normale + 1,33 unheilvolle |
| Endsiedlung | mit Elytren 1 Stadt je 20 min, ~5 Truhen (Stadt + Schiff) | 15 |

Im **normalen Spiel** (man kommt an der Struktur vorbei, sucht sie nicht) liegen die Raten grob bei
einem Viertel bis Fuenftel; alle Zeiten unten dann mal 4 bis 5.

## 3. Ergebnis

| Kern | beste Truhenquelle (gezielt) | Kerne je Stunde | erster Kern: Mittel (Median / 90 %) | ~10 Kerne: Mittel | Kisten je Kern | Chance je Struktur | braucht der Kern (Rezepte) |
|---|---|---|---|---|---|---|---|
| Kupferkern | keine | 0 | nie aus Truhen (Steinmetz sofort) | - | - | - | **2**: Kupfer-Baustab, Geschwindigkeitsmesser |
| Eisenkern | Waldanwesen | 0,064 | **16 h** (11 h / 36 h) | 156 h | 125 | ~15 % je Anwesen | **5**: Eisen-Baustab, Aufwertung Kupfer->Eisen-Baustab, Amethystlinse (Laserpointer), Magnet, Rotator |
| Goldkern | Bastionen (Festungen: 31 h) | 0,038 | **26 h** (18 h / 60 h) | 261 h | 167 | ~2,5 % je Bastion | **3**: Gold-Baustab, Aufwertung Eisen->Gold-Baustab, Erzdetektor |
| Diamantkern | normale + unheilvolle Tresore (nur normale: 39 h) | 0,036 | **28 h** (19 h / 64 h) | 276 h | 156 | ~3,8 % je Kammer | **3**: Diamant-Baustab, Aufwertung Gold->Diamant-Baustab, Netheritkern (Schmiedetisch) |
| Netheritkern | nur Schatz-Bastionen (allgemein: 67 h) | 0,027 | **37 h** (26 h / 86 h) | 373 h | 25 | 4 % je Schatz-Bastion | **1**: Enderit-Kern (Schmiedetisch) |
| Enderit-Kern | Endsiedlungen mit Elytren | 0,0375 | **27 h** (18 h / 61 h) | 267 h | 400 | ~1,2 % je Stadt | **3**: Echolot (Echo Sounder), Flugpad I, Durchtraenktes Trank-Pad III (dazu 2 Oster-Varianten derselben Ergebnisse) |

Alle Rezepte brauchen genau **einen** Kern je Herstellung (Test
`BuildingCoreTests#everyCoreRecipeCraftsWithOneCorePerSlot` prueft alle 17). Der Rezeptzaehler
enthaelt die Kette: der Netheritkern entsteht aus einem Diamantkern (+ Netheritbarren), der
Enderit-Kern aus einem Netheritkern (+ Enderitbarren, Enderit-Vorlage). Angekuendigte weitere
Rezepte mit Kupfer-/Eisenkern (Spawn-Teleporter u. a., Queue-Punkt H) sind hier noch nicht gezaehlt.

Zum Vergleich die Haendler (Annahme: ein fahrender Haendler je Spielstunde in der Naehe):
Kupfer- und Eisenkern je ~0,5 Stueck je Stunde (6,3 % x 8, begrenzt durch Smaragde), Goldkern
~0,1 je Stunde; ein Steinmetz mit dem Angebot liefert 2 Kupfer- bzw. 2 Diamantkerne je Auffrischung
(bis zu zweimal am Tag, also mehrere je Stunde).

## 4. Einordnung und Vorschlaege (Entscheidung beim Besitzer)

- **Truhen sind fuer keinen Kern die Hauptquelle, ausser fuer Netherit und Enderit.** Kupfer, Eisen,
  Gold und Diamant kommen realistisch vom Haendler (bzw. vom Netherstern); die Truhenchancen sind ein
  seltener Bonus - so war es 2026-09-27 gewollt ("sehr selten").
- **Diamantkern:** Der Steinmetz verkauft ihn fuer 3 Netheritbarren, 2 Nutzungen, mit Auffrischen.
  Das untergraebt die Seltenheit viel staerker als jede Truhenchance. Wenn Diamantkerne selten sein
  sollen, waere dort anzusetzen (z. B. 1 Nutzung, oder erst Stufe 4/5), nicht an der Truhe.
- **Eisenkern:** der meistgebrauchte Kern (5 Rezepte, bald mehr) hat nur eine Truhenquelle, die weit
  entfernte Waldanwesen. Fuer zehn Kerne aus Truhen braucht man ~150 h gezielte Suche - aus Truhen
  praktisch unerreichbar, vom Haendler leicht. Passt, wenn die Truhe nur Bonus sein soll; sonst
  koennte eine zweite, haeufigere Quelle (Plaenderer-Aussenposten oder Grabungskisten) mit ~0,5 % helfen.
- **Netheritkern:** 4 % in der Schatzkiste ist fuer einen Kern, den genau ein Rezept braucht und den
  man aus Diamantkern + Netheritbarren schmieden kann, angemessen. Keine Aenderung noetig.
- **Enderit-Kern:** 0,25 % je Kiste heisst ~400 Truhen je Kern; der erste kommt selbst mit Elytren
  erst nach ~27 h gezielter Endsuche, im normalen Spiel nach 100 h und mehr. Da er auch aus
  Netheritkern + Enderitbarren + Vorlage geschmiedet wird, ist die Truhe nur ein Glueckstreffer -
  gewollt "besonders selten". Soll ein Spieler ohne Schmieden realistisch einen finden, waeren
  0,5 % (erster Kern ~13 h gezielt) ein moderater Schritt.
- **Goldkern:** Bastion und Festung liegen mit 26 bis 31 h fuer den ersten Kern zwischen Eisen und
  Diamant; stimmig mit der Stufenfolge. Keine Aenderung noetig.
- **Kupferkern:** keine Truhe, zwei billige Haendler - passt zum Einstiegskern.

Modellrechnung: `python` mit den Werten aus Abschnitt 2 (Skript im Scratchpad der Sitzung vom
2026-09-28, Formel oben).

## 5. Median je Kernanzahl, Zeitalter und Vorschlag (2026-09-28, Besitzer-Anfrage)

Median in Stunden **gezielter** Suche bis zum k-ten Kern (Gamma-Verteilung: k Treffer eines
Poisson-Prozesses mit Rate λ aus Abschnitt 2; normales Spiel x4-5). Rezeptzahlen neu gezaehlt nach
Welle 22 (ohne Oster-Rezepte und ohne die Rezepte, die den Kern selbst herstellen): Kupfer 3
(Kupfer-Baustab, Geschwindigkeitsmesser, Chunk-Loader I), Eisen 6 (+ Launchpad I), Gold 3, Diamant 3,
Netherit 1, Enderit 3. Kupfer: nur Haendler (fahrender Haendler ~0,5/h; Steinmetz sofort).

| Kern | 1 | 2 | 3 | 4 | 5 | 6 | Rezepte | alle Rezepte | Zeitalter A (~Spielzeit) | Zeitalter B (~Spielzeit) |
|---|---|---|---|---|---|---|---|---|---|---|
| Kupfer | 1,4 | 3,4 | 5,3 | 7,3 | 9,3 | 11,3 | 3 | 5,3 | Early Game, erste Basis (~2 h) | Dorf & Handel (~4 h) |
| Eisen | 10,8 | 26,2 | 41,8 | 57,4 | 73,0 | 88,6 | 6 | 88,6 | Dorf & volle Eisenruestung (~4 h) | Diamantzeit, Zaubertisch (~8 h) |
| Gold | 18,1 | 43,7 | 69,6 | 95,6 | 121,6 | 147,7 | 3 | 69,6 | Nether-Einstieg (~12 h) | Braustand & Traenke (~15 h) |
| Diamant | 19,1 | 46,3 | 73,8 | 101,3 | 128,9 | 156,5 | 3 | 73,8 | Diamantruestung, Pruefungskammern (~15 h) | Late Game, Netherit (~25 h) |
| Netherit | 25,9 | 62,6 | 99,8 | 137,0 | 174,3 | 211,6 | 1 | 25,9 | Late Game, Netherit (~25 h) | Enderdrache besiegt (~30 h) |
| Enderit | 18,5 | 44,8 | 71,3 | 97,9 | 124,6 | 151,2 | 3 | 71,3 | End-Staedte & Elytren (~35 h) | Wither, Beacon, Grossbauten (~45 h) |

Zeitalter-Zeiten sind Richtwerte fuer einen durchschnittlichen Spieler (Gesamtspielzeit), keine Messung.

Vorschlag "etwas spaeter, Enderit 35 h" (erster Kern Median 14/20/25/30/35 h):

| Kern | Chance heute -> neu | 1 | 2 | 3 | 4 | 5 | 6 | alle Rezepte |
|---|---|---|---|---|---|---|---|---|
| Eisen | 0,8 % -> 0,62 % | 14,0 | 33,9 | 54,0 | 74,2 | 94,3 | 114,5 | 114,5 |
| Gold | 0,6 % -> 0,54 % (Festung 0,8 -> 0,72 %) | 20,0 | 48,4 | 77,2 | 106,0 | 134,8 | 163,6 | 77,2 |
| Diamant | 0,8 % -> 0,61 % | 25,0 | 60,5 | 96,4 | 132,4 | 168,5 | 204,5 | 96,4 |
| Netherit | 4 % -> 3,45 % | 30,0 | 72,6 | 115,7 | 158,9 | 202,2 | 245,4 | 30,0 |
| Enderit | 0,25 % -> 0,13 % (30-40 h: 0,15-0,11 %) | 35,0 | 84,7 | 135,0 | 185,4 | 235,9 | 286,3 | 135,0 |

Noch nichts geaendert - Entscheidung beim Besitzer. Skript: scratchpad `cores_tab.py` (2026-09-28).
