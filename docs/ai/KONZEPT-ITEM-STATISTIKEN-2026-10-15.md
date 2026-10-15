# Konzept Item-Statistiken (Queue Nachtrag 62, 2026-10-15)

Nur Konzept, kein Code. Quelle: Queue Nachtrag 62, Abschnitt „Item-Statistiken“ (Vorbild Mod „Item Stat Tracker“).
Belege: Modrinth-API/Projektseite von „Item Stat Tracker“ (Stand 15.10.2026) und Repo per grep. Nichts davon im Spiel geprüft.
Haltung: Tooltip-Regel (Nachtrag 38), Server-Autorität (AGENTS 3.8), Modul-Unabhängigkeit, keine Texturen (reine Text-/Komponentenfunktion).

## 1. Item Stat Tracker (Fremdmod) – was öffentlich belegbar ist
Autor Potota, Modrinth-Slug `itemstats` (CurseForge: „Item Stat Tracker“), ~21.800 Downloads, Fabric + NeoForge, MC 1.21–26.2, erste Version 04.10.2025,
letzte 1.1.1 (16.06.2026). Lizenz **All Rights Reserved** (Quelltext auf tangled.org; **nicht gelesen, nichts übernehmbar** – wir übernehmen nur Ideen).
Umgebung laut Modrinth: Client **und** Server erforderlich (Fabric-„server-side edition“ steht unter „Coming soon“).

**Funktionen (Projektseite + Changelogs):**
- Jedes Ausrüstungsstück zählt selbst, Anzeige im Tooltip: Blöcke abgebaut (Spitzhacke/Axt/Schaufel/Hacke/Schere), Mobs getötet (Schwert, Axt, Dreizack,
  Streitkolben, Speer, Bogen, Armbrust), Schaden absorbiert (Helm/Brust/Hose/Stiefel/Schild, auch bei Mobs getragene Rüstung, Fallschaden), Flugstrecke (Elytra),
  Pfeile verschossen, Stämme entrindet, Schafe geschoren. „Many more to come“.
- Seit 1.0.0 (04.03.2026) **detailliert**: „Blocks Mined“ listet genau welche Blöcke; Stats alter Versionen werden migriert; neuer **Stat-Viewer-Bildschirm**; Taste „Show Stats“.
- Kompatibilität: Elytra im Trinkets-Slot, Oritech-Railgun/-Elytra, Übersetzungen für Create-Schadensarten; Warnung statt Absturz, wenn ein Mod einen ItemStack zu früh baut.
- Befehl: der Fehlerbehebungseintrag zu 1.0.1 (NPE bei „einem Befehl mit ungültiger stats-Komponente“) zeigt: die Daten liegen in einer **Item-Komponente** namens „stats“,
  per `/give` setzbar.
**Speicherformat:** nur diese Aussage ist öffentlich (Komponente, ab 1.0.0 neues, reicheres Schema mit Migration). Feldaufbau, Obergrenzen, Namensraum: **nicht öffentlich belegt**.

**Stärken:** deckt fast alle Gear-Arten ab; Datenträger ist das Item selbst (wandert mit Truhe/Handel/Tod); Stat-Viewer; Mod-Kompat-Haken; läuft auf Server+Client ohne Zusatzdaten.
**Schwächen (aus Beleg bzw. Schluss daraus, als solche markiert):**
1. Closed Source/ARR – nicht erweiter-, nicht forkbar.
2. Keine dokumentierte Größenbegrenzung: „genau welche Blöcke“ ist eine offene Liste (Schluss: unbegrenzt wachsend); keine Doku zu Top-N.
3. Kein dokumentiertes Zurücksetzen, keine Config, keine Datenschutz-/Sichtbarkeitsoption (nichts in Beschreibung/Changelog).
4. Keine Mob-Variante (nur „Mobs killed“), keine Rekorde (längster Schuss/Flug, größter Treffer), keine Stiefel-Wegstrecke.
5. Anfälligkeit durch globalen Eingriff (Warnung „ItemStack zu früh gebaut“ deutet auf Hook beim Item-Bau; Schluss, nicht verifiziert).
6. Schema wurde einmal inkompatibel umgebaut (Migration nötig) – Lehre: Versionsfeld von Anfang an.

## 2. Ist-Zustand im Projekt (grep)
- Keine Item-Statistik vorhanden. Items tragen Daten als `DataComponentType` mit `persistent` + `networkSynchronized` (Muster `TweaksComponents`, `BlueprintItem`, `BackpackItem`).
- Tooltip-Einhängung: `@Mixin(ItemStack)` auf `addDetailsToTooltip` (Muster `ItemStackSoulboundTooltipMixin`, Simple Tools) – eine Stelle, alle Loader, kein Loader-Event nötig.
- Abbau: auch der Hammer-Flächenabbau läuft über `ServerPlayerGameMode#destroyBlock` (`SledgehammerUsageEvent:54`), das `ItemStack#mineBlock` aufruft (`SledgehammerItem:173`
  überschreibt und ruft `super`). **Ein** Hook auf `ItemStack#mineBlock` zählt also Vanilla, Hammer, Vein-/Strip-Miner ohne Kenntnis von SimpleBuilding; Kreativ ruft `mineBlock` nicht auf (kein Zählen im Kreativmodus ohne Zusatzcode).
- Es gibt Schutz-Probes (`BuildGuard`) für Abbau-Events je Loader – für Stats nicht nötig (zählt nur erfolgreiche Abbauten).
- Soulbound/Todesgewölbe (Simple Tools) lassen Items beim Tod unverändert → Komponente bleibt erhalten.

## 3. Welche Stats je Item-Art
Zuordnung **nach Tag/Komponente, nicht nach Klasse** (damit Mod-Gear mit Vanilla-Verhalten gratis mitläuft): `#minecraft:pickaxes/axes/shovels/hoes/swords/spears`, Komponenten
`TOOL`, `WEAPON`, `EQUIPPABLE` (Slot), `GLIDER` (Elytra), `BLOCKS_ATTACKS` (Schild), Items vom Typ Bogen/Armbrust/Dreizack/Streitkolben/Angel/Schere/Bürste.
Jeder Stat = Schlüssel `<modul:stat>` → Gesamtzähler; „Detail-Stats“ haben zusätzlich eine Top-N-Liste.

| Item-Art | Gesamtzähler | Detail-Liste (Top-N) | Rekord |
|---|---|---|---|
| Spitzhacke, Schaufel, Hacke, Schere, Hammer (alle `TOOL`) | `blocks_mined` | je Blockart | – |
| Axt | `blocks_mined`, `logs_stripped`, Kills | Blockart; Kills je Mob | – |
| Hacke | zusätzlich `tilled` | – | – |
| Schere | zusätzlich `sheared` (Mobs) | je Mob/Farbe | – |
| Schwert, Axt, Speer, Streitkolben, Dreizack | `kills`, `damage_dealt` | **je Mob** und **je Mob-Variante** (siehe unten) | größter Einzeltreffer; Streitkolben: höchster Schmetterschaden |
| Bogen, Armbrust | `arrows_fired`, `hits`, `kills` | Kills je Mob/Variante | längster Treffer (Blöcke) |
| Dreizack (geworfen) | `throws`, `kills` | Kills je Mob | – |
| Helm, Brust, Hose, Stiefel, Reittier-Rüstung | `damage_absorbed`, `hits_taken`, `worn_time` | absorbiert je Schadensart (Fall, Explosion, Feuer …) | größter abgefangener Treffer |
| Schild | `damage_blocked`, `attacks_blocked` | je Angreifer-Mobart | – |
| Elytra | `distance_flown`, `flights`, `rockets_used` | – | längster Flug |
| Stiefel (zusätzlich) | `distance_walked`, `distance_sprinted`, `distance_sneaked`, `distance_swum` | – | – |
| Angel | `casts`, `catches` | Fänge je Beute-Item; Kategorien Fisch/Schatz/Müll | – |
| Bürste, Feuerzeug (optional) | `brushed`, `ignited` | – | – |
| Mod-Gear (Baustab, Hammer, Oktant …) | über Tags wie Vanilla; eigene Zähler (z. B. „platziert“) später, nur per öffentlichem Tag/ID-Vertrag | – | – |
Bewusst **nicht**: Haltbarkeit/Reparaturen (Balken zeigt es), Zeit seit Erstellung, Geld/Wert, irgendeine Spielwirkung (Stats sind rein kosmetisch → keine Balance-Folgen).

**Mob-Variante:** Schlüssel `entity_type` + optional `#variant`, aus Entity-Variant-Komponenten (Katze, Wolf, Frosch, Axolotl, Pferdefarbe, Schaf-Farbe, Dorfbewohner-Typ/-Beruf,
Kaninchen, Papagei, Pilzkuh …) sowie `baby`, `charged` (Creeper), Slime-Größe. Unbekannte Mod-Mobs zählen unter ihrer Entity-ID ohne Variante (Mod-Varianten über einen späteren
Variant-Extraktor in `framework.api`, nicht in Phase 1).
**Absorbiert-Zuordnung (Näherung, im Wiki so sagen):** abgefangener Schaden = Rohschaden minus Schaden nach Rüstung/Verzauberung; Verteilung auf getragene Teile **proportional zu
Rüstungspunkten**; Schild: tatsächlich geblockter Betrag; Fallschaden: Stiefel. Spieler-Kills zählen nur als Anzahl (kein Name).
**Zählregeln:** Blöcke mit Härte 0 zählen nur, wenn das Werkzeug dafür „correct for drops“ ist (Hacke auf Feldfrüchten ja, Spitzhacke auf Gras nein); Kreativmodus und Fake-Spieler
(Maschinen) zählen nicht (Config); Kills gehören dem Item in `DamageSource#getWeaponItem` (so zählt ein Pfeil für den Bogen, auch wenn die Hand wechselt – Fehler, den auch Item Stat Tracker erst in 0.2.1 behoben hat).

## 4. Speicherung als Item-Komponente (mit Größenbegrenzung)
Komponente `<modul>:item_stats`, `persistent` + `networkSynchronized` (Tooltip rendert clientseitig aus der Komponente):
```
{ v:1, since:<Spielzeit/Datum>, totals:{ "blocks_mined":5472, "kills":312 },
  detail:{ "blocks_mined":{ top:[["minecraft:stone",3104],["minecraft:deepslate",911],…], other:1457 },
           "kills":{ top:[["minecraft:zombie",120],["minecraft:cat#tabby",3],…], other:42 } },
  records:{ "longest_flight":8120 } }
```
- **Top-N je Liste, Standard N=12, Server-Obergrenze 32.** Überlauf: in der Liste dürfen 4 Reserveplätze bis N+4 wachsen, damit neue Schlüssel sich hocharbeiten können; danach fällt der
  kleinste Eintrag in `other` (Summe bleibt exakt: Σtop + other = Gesamtzähler – Invariante, per Test gesichert). Kein Space-Saving-Überzählen.
- **Harte Größenbremse:** Codec prüft beim Dekodieren Listenlängen (> N_max → abschneiden in `other`), Zähler `long`, Schlüssel nur gültige IDs. Rechenbudget: ≤ 6 Detail-Listen × 36 Einträge × ~25 B ≈ 5,4 KB
  im ungünstigsten Fall, typisch < 1 KB. Item ohne Aktivität bekommt **keine** Komponente (kein Default für alle Items → kein Hook im ItemStack-Bau, vermeidet die Fehlerklasse „zu früh gebaut“).
- Versionsfeld `v` ab Tag 1; unbekannte höhere Version: nicht überschreiben, nur lesen/ignorieren (Rückwärts-Sicherheit bei Rollback).
- **Vererbung:** Schmiedeupgrade (Diamant → Netherit) kopiert Komponenten → Stats bleiben (gewollt, Wiki nennt es). Amboss: linkes Item gewinnt (Vanilla-Verhalten), kein Zusammenführen.
  Reparatur im Crafting-Raster erzeugt ein neues Item → Stats weg (Vanilla-Verhalten, im Wiki erwähnen). Soulbound/Tod/Truhen/Handel: unverändert.
- Gleiche Items mit/ohne Stats stapeln nicht (Werkzeuge stapeln ohnehin nicht; Rüstung/Elytra ebenso).

## 5. Anzeige: Kurzvorschau + Shift-Details
- **Normal (Tooltip-Regel Nachtrag 38):** Zahl ist sonst nirgends sichtbar → erlaubt (Fall b). **Höchstens 2 Zeilen, ≤ 40 Zeichen:**
  Z1 Hauptwert mit dem häufigsten Eintrag, z. B. „5.472 Blöcke · meist Stein“ / „312 Kills · meist Zombie“ / „8,1 km geflogen“; Z2 grau „Shift: Statistik“.
  Zahlen lokalisiert, ab 10.000 kompakt („12,4k“). Items ohne Komponente zeigen **nichts** (kein „0 Blöcke“-Rauschen).
- **Mit Shift:** alle Gesamtzähler, je Detail-Liste die Top-5 samt „… und 7 weitere (n)“ (aus `other`), Rekorde, „Seit <Datum>“. Max. ~10 Zeilen.
  Das überschreitet die 2-Zeilen-Regel bewusst → **Ausnahme-Entscheidung des Besitzers nötig** (Frage 3); Alternative: nur Taste + Stat-Viewer-Bildschirm (Phase 2).
- Kein Text auf dem Bildschirm außerhalb des Tooltips (AGENTS 3.4 betrifft Gadgets; wir fügen keine Aktionsleiste/Chat hinzu).
- Neue Lang-Keys EN (amerikanisch) + DE, `tools/check_tooltip_text.py` grün; Einstellungs-Tooltips der Config sind ausgenommen.
- Kompatibilität: Mod-Tooltip-Zeilen (Soulbound-Hinweis, Rucksack-Füllstand) bleiben vor den Stats; Stats am Ende (TAIL-Inject), außer mit F3+H.

## 6. Performance (Ereignisse, kein Tick-Polling)
- Alle Hooks sind **Ereignisse**, kein Item-Tick und kein Inventar-Scan: `ItemStack#mineBlock` (Abbau), `LivingEntity#die`/`DamageSource#getWeaponItem` (Kills, Pfeile über
  `AbstractArrow#getWeaponItem`), Rüstungsschaden im vorhandenen Schadenspfad (`LivingEntity#actuallyHurt`, einmal je Treffer, Teile der getragenen Slots), `FishingHook#retrieve`,
  `Player#awardStat` (Vanille zählt Laufen/Fliegen/Schwimmen schon – wir hängen uns an dessen Inkremente, kein eigenes Messen), Equipment-Wechsel für `worn_time` (Start-/Endstempel, Differenz bei Ablegen).
- **Gebündeltes Schreiben:** Änderungen sammeln sich je Spieler in einem kleinen Puffer (Map Item→Delta) und werden **höchstens einmal je Spielerzeit-Takt** (z. B. alle 100 Ticks bzw. am Ende
  des Server-Ticks bei Burst) plus bei Ablegen/Logout/Tod in die Komponente geschrieben; Hammer-Flächenabbau mit 25 Blöcken = **1** Schreibvorgang. Der Flush läuft nur für Spieler mit
  „dirty“-Flag (Server-Tick-Ende-Hook mit leerem Normalfall).
- **Netz:** jeder Komponentenschreibvorgang löst ein Slot-Update aus → deshalb gedrosselt (≤ 1 pro Item und 5 s). Risiko: Komponentenänderung in der Haupthand kann die Aufnahme-/Re-Equip-Animation
  auslösen (`Item#shouldCauseReequipAnimation`) → per Mixin Stats-Komponente aus dem Vergleich nehmen; im Client-Test prüfen.
- Rechenkosten je Ereignis: eine Hash-Map-Erhöhung; Top-N-Pflege O(N) mit N ≤ 36. Zielwert für Test: 10.000 Abbauten < 50 ms Zusatzzeit, ≤ 10 Komponentenschreibvorgänge.

## 7. Multiplayer und Sichtbarkeit
- **Server-autoritativ:** nur der Server zählt; der Client rendert nur. Config (Server, mit Obergrenzen): `enabled`, Kategorien einzeln (Abbau/Kills/Rüstung/Strecken/Angel), `topN` 1–32,
  `countCreative=false`, `countFakePlayers=false`, `trackPlayerKills=true` (nur Anzahl), `resetRecipe`, `showToOthers`.
- **Sichtbarkeit:** Items sind Trophäen – Stats sind für jeden sichtbar, der das Item sieht (Inventar, Truhe, Rahmen, Händler). Der Tooltip entsteht auf dem Client aus der Komponente;
  **echte Verbergung vor anderen Spielern ist daher nicht möglich** (nur kosmetisch per `showToOthers` + gespeicherter Besitzer-UUID). Empfehlung: keine Besitzer-UUID speichern, keine
  Namen von getöteten Spielern; Datenschutz = nur Anzahl. Wer sein Item teilt, teilt die Stats (im Wiki sagen).
- **Mehrere Benutzer eines Items:** zählt, wer es nutzt (Gemeinschaftswerkzeug); kein Erstbesitzer-Feld.
- Voraussetzung wie Item Stat Tracker: Mod auf Server **und** Client (eigener Komponententyp; Vanilla-Client kann die Welt nicht betreten – wie alle Mods des Projekts mit eigenen Items).

## 8. Zurücksetzen
- **Befehl** `/itemstats reset` (Haupthand, Stufe 2) und `/itemstats show` (Rohwerte, Debug) – Server-Op.
- **Im Spiel (Vorschlag):** Werkbank, formlos: Item + Tintenbeutel → gleiches Item ohne Stats (Verzauberungen, Schaden, Name bleiben; kleine Rezept-Sonderklasse, dreimal registrieren).
  Config `resetRecipe`. Nicht Schleifstein (Entzaubern würde sonst Stats löschen).
- Kein automatisches Zurücksetzen (Tod, Reparatur, Upgrade).

## 9. Modulwahl
Optionen: (A) Simple Tools erweitern; (B) SimpleBuilding-Kern; (C) **neues Sub-Mod `simplestats`** („Simple Stats“); (D) neues Sub-Mod und Pflicht-Sub-Mod der QoL-Super-Mod.
**Empfehlung C, nicht in QoL-`requires` (zunächst optional/suggests).** Gründe: Funktion ist cross-cutting (jedes Werkzeug), aber unabhängig von Bau/Maschinen → nicht Kern (der soll nicht weiter wachsen);
Simple Tools hat bereits Soulbound/Vault/Thrift, ein zweites Datenmodell dort würde Schalter und Tests vermischen; eigener Jar lässt Spieler, die keine Tooltip-Zeilen wollen, die Funktion weglassen
(QoL-Super-Mod hat „Enable Simple XY“-Schalter je Sub-Mod, Konzept Super/Sub-Mod).
**Unabhängigkeit (Regel 1–8):** allein spielbar mit Vanilla + Cloth Config; zählt Vanilla-Gear über Tags/Komponenten, **keine Klassenimporte** aus SimpleBuilding/anderen Modulen; Mod-Gear mit eigenem
Verhalten nur über `framework.api`-Vertrag oder Registry-IDs (`required:false`-Tags `simplestats:track/...`); eigenes Standalone-Testziel, eigene Wiki-Seite (`modules/simplestats/wiki/manual.json`,
Hinweis „mit SimpleBuilding: Hammer-Flächenabbau zählt jeden Block“), Balancing-Ablage `balance/simplestats/`, Eintrag in `docs/MODS-UND-FEATURES.md` + `modules.json` (alle drei Loader, nur 26.3).
Anlage per `python tools/newmod.py simplestats "Simple Stats"`. Hooks per Mixin in `shared` → ein Code für alle Loader (nur Registrierung/Befehl loaderspezifisch).

## 10. Vergleich mit Item Stat Tracker
| Thema | Item Stat Tracker | Hier |
|---|---|---|
| Datenträger | Item-Komponente „stats“ | übernommen (Komponente), mit Versionsfeld |
| Gear-Abdeckung | Abbau, Kills, Absorbiert, Flug, Pfeile, Entrinden, Scheren | übernommen + Wegstrecke (Stiefel), Angel, Schild/Schmetter-/Treffer-Rekorde, Tragezeit |
| Detail je Block | ja, Umfang unbegrenzt | ja, **Top-N + „Sonstige“, harte Obergrenze** |
| Mob-Variante | nein (nur Gesamt) | **ja** (Wunsch Besitzer) |
| Anzeige | Tooltip + Stat-Viewer + Taste | Tooltip (2 Zeilen) + Shift-Details; Viewer optional Phase 2 |
| Mod-Kompat | Handarbeit je Mod | **Tags/Komponenten statt Klassen**, Fremd-Gear mit Vanilla-Verhalten gratis |
| Reset / Config / Sichtbarkeit | nicht dokumentiert | Befehl + Rezept, Server-Config mit Obergrenzen, Datenschutz = nur Anzahl |
| Lizenz | ARR | eigener Code, nichts kopiert (Quelltext nicht eingesehen) |
| Server | Client+Server nötig | gleiche Anforderung; Server-autoritativ |
| Schema-Stabilität | einmal gebrochen | `v`-Feld, defensives Dekodieren |

## 11. Tests (später, Namensraum `simplestats:`)
Server-Gametests (Fabric: Wrapper in `src/main/java/.../gametest/*GameTest.java` + Katalog, NeoForge/Forge gleiche Fälle): Abbau zählt Gesamt+Block; Härte-0-Regel (Hacke ja, Spitzhacke nein);
Hammer-25-Block-Abbau = 25 Zählungen, 1 Schreibvorgang; Kill zählt Mob+Variante (Katze tabby, Schaf rot); Pfeil-Kill zählt dem Bogen nach Handwechsel; Rüstung absorbiert (Summe der Teile = Rohschaden − Rest);
Schild geblockt; Elytra-Strecke über Vanilla-Stat; Angel Fang/Schatz; **Invariante Σtop + other = total** bei 1.000 verschiedenen Schlüsseln; **Größenbudget** (serialisiert < 6 KB); Codec-Roundtrip,
Fremdversion bleibt unangetastet; Kreativ/Fake-Spieler zählen nicht; Config aus ⇒ keine Komponente entsteht; Schmiedeupgrade behält Stats; Reset-Rezept behält Verzauberungen/Schaden/Name;
Befehl nur mit Rechten; Performance (10.000 Abbauten). Client-Test (nur seriell, ohne Besitzer-Client): Tooltip ≤ 2 Zeilen/≤ 40 Zeichen, Shift-Zeilen, keine Re-Equip-Animation.
Standalone-Ziel `module-simplestats-standalone-{fabric,neoforge}-263`; Checks: `wiki/generate.py --all --check --strict`, `module_guides.py --check`, `discover.py --check`, `check_tooltip_text.py`.

## 12. Phasen
0 Entscheidungen (Abschnitt 13). 1 Komponente + Codec + Top-N-Kern (reine Unit-Tests) + Abbau/Kills/Rüstung + Tooltip. 2 Strecken (Stiefel, Elytra), Angel, Schild, Rekorde, Shift-Details, Befehl, Reset-Rezept.
3 Wiki/Guide/Testzentrale, Fabric-Welle abnehmen, dann NeoForge/Forge im selben Zug (Hooks sind gemeinsamer Mixin-Code). Port-Run 26.2 erst nach Abnahme; Rückport-Prüfung (Vanilla hat keine Item-Statistik).

## 13. Offene Besitzer-Fragen (jeweils mit Empfehlung)
1. Modul: A Simple Tools, B Kern, **C neues Sub-Mod `simplestats`** (empfohlen), D C + Pflicht-Sub-Mod von QoL → Empfehlung C, QoL nur optional.
2. Umfang Phase 1: **Abbau, Kills (je Mob/Variante), Rüstung absorbiert** zuerst; Strecken/Angel/Schild/Rekorde in Phase 2 → Empfehlung ja, oder alles auf einmal?
3. Shift-Details dürfen die 2-Zeilen-Regel überschreiten (~10 Zeilen)? **Ja, nur mit Shift** (empfohlen); sonst Stat-Viewer-Bildschirm statt Shift.
4. Top-N Standard: **12, Obergrenze 32** (empfohlen) – oder kleiner (8) für kürzere Dateien?
5. Zurücksetzen im Spiel: **Werkbank Item + Tintenbeutel** (empfohlen) / nur Befehl / gar nicht?
6. Stats bei Schmiedeupgrade vererben (**ja**, empfohlen)? Amboss-Zusammenführung: **nein**, linkes Item gewinnt.
7. Sichtbarkeit: Stats für alle sichtbar, Datenschutz = nur Anzahl bei Spieler-Kills, **kein Besitzerfeld** (empfohlen) – oder Besitzer speichern („Erstbesitzer“)?
8. Kreativmodus und Maschinen (Fake-Spieler) zählen **nicht** (empfohlen)?
9. Härte-0-Blöcke (Gras, Feldfrüchte) nur mit passendem Werkzeug zählen (**ja**, empfohlen) oder alles zählen?
10. Rüstungs-Absorbiert als Näherung (proportional zu Rüstungspunkten) akzeptiert (**ja**) – oder aufwendig exakt je Teil?
11. Tragezeit (`worn_time`) und Stiefel-Wegstrecke aufnehmen (**ja**, ereignisgetrieben)?
12. Spätere Meilensteine/Titel („1.000 Kills“ als Name-Zusatz, Toast) – **nicht jetzt** (Stats bleiben kosmetisch, kein Gameplay)?
