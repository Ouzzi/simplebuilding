# Konzept: Shops-Umbau – Enderperle, Fernzugriff-Block, nur Transaktionen (2026-10-19)

Queue „Besitzer 19.10. abends – Nachtrag“, Zeile Shops-Umbau (B, b-konz20). Nur Konzept, kein Code.
„(Vorschlag)“ = eigene Festlegung, der Besitzer kann sie ändern. Entscheidungen: §8 (Seite `konzepte20.html`, Code `KONZ20 S…`).

## 1. Ist-Zustand (per grep erhoben)
- Modul **`modules/simpleshops`** (nicht Simple Money, nicht SB), `requires: simplelib`, optional Simple Money/Tweaks/Maps. Grundkonzept
  `docs/ai/KONZEPT-SHOP-VITRINE-2026-10-14.md`, letzter Umbau `docs/ai/BERICHT-CLAUDE-SHOPS2.md` (N66).
- Blöcke: `display_case` / `glow_display_case` (Vitrine, **5 Waren-Fächer**, Besitzer legt Ware hinein), `sales_block`
  (Verkaufsblock: Besitzer, Preis-Vorlagen `PriceRow` ≤ 5, **27 Einnahmen-Slots**), `sales_storage` (Verkaufslager, 27 Slots, zieht alle
  8 Ticks aus dem Block darüber, Kette ≤ 16, nur gleicher Besitzer).
- „Nichts hineinlegen“ ist **schon weitgehend so**: Einnahmen-Slots in `SalesOwnerMenu` und `StorageMenu` haben `mayPlace = false`;
  `ListContainer` = kein Welt-Container (kein Trichter, keine Lore, keine Mod-Rohre). Ungeprüft sind Nebenwege (Zifferntaste-Tausch,
  Doppelklick-Sammeln, Drag) – das wird in Stufe 1 per GameTest belegt.
- Rezepte: Verkaufsblock `PPP/ICI/PPP` (Bretter, Eisen, Truhe), Lager `PPP/ICI/PHP` (+ Trichter), Vitrine `NNN/GGG/PPP`. Keine Enderperle.
- Daten: `ShopLedger` (SavedData: Besitzer, Dimension, Position je Shop-Block, für `maxShopsPerPlayer` 16, max. 64) – **Grundstock für eine
  Shop-Liste**. `ServerShops` (Admin-Shops `s001…`, Kasse). `ShopMapLayer` zeigt aktive Shops in **geladenen** Chunks (b-maps20 setzt
  Geldschein-Symbol). Tausch atomar in `ShopTrade` (prüfen → simulieren → buchen), Sperre ein Spieler, `revision`.
- Es gibt **keinen** Fernzugriff, keine Suche über Shops, keine Daten außerhalb der Block-Entities (Ware/Einnahmen liegen nur im Chunk).

## 2. Kernproblem Fernzugriff
Ware und Einnahmen liegen in Block-Entities. Ein Fernzugriff auf einen entladenen Chunk müsste ihn laden (Chunk-Loader-Missbrauch, Lag)
oder mit veralteten Kopien arbeiten (Dupe-Gefahr). Deshalb entscheidet die Datenhaltung über alles andere:

| Variante | Idee | + | − |
|---|---|---|---|
| **A · Ender-Shops (Empfehlung)** | Shop-Inhalt (Ware, Preis, Einnahmen) liegt in Weltdaten `simpleshops:ender_shops` (SavedData, je Shop-Id), der Block ist nur das „Fenster“ – wie die Endertruhe. Daher die **Enderperle** im Rezept. | Fernzugriff ohne Chunk-Laden, ein Tauschpfad für vor Ort und fern, Suche trivial, Karte zeigt auch entladene Shops | Umbau der Datenhaltung + Migration alter Shops |
| B · Spiegel + kurzes Laden | Block-Entity bleibt Wahrheit; Katalog = Kopie in SavedData (bei jeder `revision` aktualisiert); Kauf lädt den Chunk 1 Tick | kleiner Umbau | Synchrones Chunk-Laden je Fernkauf (Lag/Missbrauch, Rate-Limit nötig), Kopie kann veralten |
| C · Fernzugriff nur geladene Chunks | wie heute die Karte | kein Umbau | Fernzugriff fast nutzlos (fremde Shops sind fast immer entladen) |

**Empfehlung A.** Regeln: Shop-Id beim Platzieren (`p000123`, nie wiederverwendet, wie `ServerShops`); Abbau durch Besitzer droppt den
Inhalt **einmal** am Block (Chunk ist dann geladen) und löscht den Eintrag; Block ohne Eintrag (z. B. `/setblock`) = leerer neuer Shop;
Eintrag ohne Block (Block per Befehl entfernt) = „verwaist“, nur `/shop` (OP) räumt auf, Inhalt bleibt erhalten (nie stiller Verlust).
**Migration:** beim ersten Laden eines alten Blocks wandert sein Inhalt in einen neuen Eintrag (Datenversion im BE, GameTest „alt → neu,
Summe aller Items gleich“).

## 3. Blöcke und Rezepte (Vorschlag)
| Block | Rezept neu | Aufgabe |
|---|---|---|
| Verkaufsblock (Shop) | `PPP / IEI / PCP` (Bretter, Eisen, **Enderperle**, Truhe) | Kasse + Preis, Shop-Id |
| Verkaufslager (Shop-Lager) | `PPP / IEI / PHP` (Trichter bleibt) | Einnahmen-Erweiterung, gehört zur Shop-Id darüber |
| Vitrine | unverändert (Glas/Nuggets) | zeigt Ware, Ware gehört zum Shop darunter |
| **Handelskontor** (Fernzugriff, neu, `simpleshops:trade_terminal`) | `GEG / BLB / PPP` (Gold, **Enderauge**, Bücherregal, Lesepult, Bretter) | zwei Tabs, s. §4 |

Alte Shops ohne Enderperle bleiben gültig (Migration §2), Rezept-Advancements wie bisher.

## 4. Handelskontor – zwei Tabs (Kastenstil simplelib `UiBoxes`, Palette BARREL, Kontrast ≥ 4,5:1)
**Tab „Meine Shops“** – Liste (Name/Position/Dimension, Ware-Icons, Bestand, Einnahmen-Stand, Status aktiv/ausverkauft/Einnahmen voll):
- Einnahmen **abholen** (in Inventar; nie hinein).
- Preise ändern (gleiche Vorlagen-Klicks wie am Verkaufsblock), Shop **öffentlich/privat** (privat = nicht im Katalog, vor Ort kaufbar).
- Ware: siehe Balancing §5 (Empfehlung: nur **entnehmen**, einlegen nur vor Ort).
- Name je Shop (Amboss-Umbenennung des Verkaufsblocks oder Feld im Kontor, ≤ 24 Zeichen, Server prüft).

**Tab „Shops durchsuchen“** – alle öffentlichen Shops aller Spieler + Server-Shops:
- Suchfeld (Item-Name, Mod-Id), Filter „hat Bestand“, Sortierung nach Preis/Entfernung/Besitzer, Seitenweise (Server schickt ≤ 50 Zeilen
  pro Anfrage, nie den ganzen Katalog).
- Kaufen: gleicher Ablauf wie die Käufer-GUI (Preis-Slots aus dem eigenen Inventar füllen, „Tauschen“, Shift = max.), Server bucht über
  denselben `ShopTrade`-Pfad gegen die Weltdaten. Optional „Ort anzeigen“ (Kompass-Partikel bzw. Wegpunkt in Simple Maps, nur wenn geladen).
- Eigene Shops sind dort sichtbar, aber nicht kaufbar (Testkauf bleibt Dev-Knopf vor Ort).

Sperre: ein Shop bleibt **ein** Bediener gleichzeitig (vor Ort oder fern, gleiche Sperre in den Weltdaten, nie gespeichert). Fern = kurzes
Halten (nur während des Kauf-Dialogs), Idle-Timeout wie Config.

## 5. Balancing „Zusatzlager“ (5 Fächer × bis zu 16 Shops = 80 Stapel)
Missbrauch: Ware in eigene Vitrinen legen, Preis absurd hoch, dann überall per Kontor herausholen → ein großes, ortloses Lager
(Endertruhe hat nur 27).

| Variante | Fernzugriff auf Ware | Missbrauch |
|---|---|---|
| **W1 · Nur ansehen** | Ware fern nur ansehen; ein- und ausräumen nur vor Ort | keiner |
| **W2 · Fern nur entnehmen (Empfehlung)** | entnehmen fern mit **Lieferzeit** (2 s je Stapel, Config 0–20 s) und nur aus öffentlichen Shops mit gültigem Preis; einlegen nur vor Ort | Einweg-Lager, das man vorher hinbringen muss; Lieferzeit macht es zum schlechten Lager |
| W3 · Voll fern | ein- und ausräumen fern; dafür Gebühr je Fernbuchung (Config, z. B. 1 Enderperle je 8 Stapel) | wirkt wie eine zweite Endertruhe, nur über Kosten gebremst |

Zusätzlich in jeder Variante: Fernkauf und Fernentnahme nur in **derselben Dimension** (Config `remoteCrossDimension`, Standard aus);
Config `remoteStock = none|take|full` (Standard `take`) und `remoteDelaySeconds`; harte Obergrenzen serverseitig (AGENTS 3.8).

## 6. Server-Sicherheit
- Client schickt nur Absichten (`shopId`, `revision`, Ware-Index, Anzahl-Modus); alle Mengen und Preise kennt der Server. Unbekannte/fremde
  Shop-Id, falsche `revision`, Sperre fremd → nichts passiert.
- Ein Tauschpfad (`ShopTrade` auf Weltdaten), Snapshots + Rollback wie heute. Fern-Lieferzeit: Ware wird beim **Auftrag** reserviert
  (aus dem Shop genommen), nicht erst bei Lieferung – kein Doppelverkauf; Abbruch/Disconnect legt sie zurück.
- Kein Item-Drop vor die Füße im Kontor: kein Platz = kein Kauf/keine Entnahme (wie heute).
- Katalog-Anfragen rate-limitiert (z. B. 4/s je Spieler), Antwort ≤ 50 Zeilen, Suche serverseitig, Text ≤ 32 Zeichen.
- Schutzregeln der Blöcke (Explosion, Kolben, nur Besitzer baut ab, Claims `claim_use` = U) bleiben; Kontor selbst hält nichts, darf normal
  abgebaut werden, braucht kein Claim-Recht außer U.
- Weltdaten-Grenzen: Einträge ≤ `MAX_SITES`, Codec mit Listenlimits, Speichern atomar (Vanilla SavedData).

## 7. Stufenplan
1. **Ist-Belege + Rezepte**: GameTests „nichts hineinlegen“ über alle Klickarten (Zifferntaste, Doppelklick, Drag, Shift) für Verkaufsblock
   und Lager; Rezepte mit Enderperle. Klein, sofort möglich.
2. **Ender-Datenhaltung** (§2 A) + Migration + Abbau/Drop-Regeln; Vor-Ort-UIs lesen/schreiben Weltdaten. Missbrauchs-Suite (52 Tests) muss grün bleiben.
3. **Handelskontor Tab „Meine Shops“** (Einnahmen, Preise, öffentlich/privat, Name, Ware nach §5).
4. **Tab „Shops durchsuchen“** + Fernkauf; Karte (Simple Maps, bedingt) zeigt auch entladene öffentliche Shops.
5. Wiki/Guide/Discover, Kontrast-Werte, Client-Abnahme der beiden Tabs.

## 8. Testplan
- JUnit: Layout des Kontors (wie `ShopLayoutTest`), Katalog-Sortierung/Seiten.
- GameTests (Fabric/NeoForge/Forge + standalone): Migration alt→neu (Item-Summe gleich); Fernkauf = Vor-Ort-Kauf (gleiche Bilanz);
  entladener Chunk wird **nicht** geladen (Chunk-Ticket-Zähler unverändert); gleichzeitiger Kauf vor Ort + fern (nur einer gewinnt);
  Lieferzeit + Disconnect legt Ware zurück; privat = nicht im Katalog; Dimensionsregel; Rate-Limit; fremde Shop-Id/alte revision abgelehnt;
  Abbau droppt genau einmal; Block per `/setblock` entfernt → Eintrag verwaist, nichts verloren.
- Standalone-Target `module-simpleshops-standalone-fabric-263` (ohne Maps/Money/Tweaks).
- Client-Abnahme: beide Tabs, Kontrast, Suchfeld.

## 9. Offene Besitzer-Fragen (Vorschlag fett)
- **S1 Datenhaltung:** **A Ender-Shops (Inhalt in Weltdaten)** / B Spiegel + kurzes Chunk-Laden / C nur geladene Chunks.
- **S2 Ware über Fernzugriff:** W1 nur ansehen / **W2 nur entnehmen mit Lieferzeit** / W3 voll mit Gebühr.
- **S3 Fernkauf über Dimensionen:** **nur gleiche Dimension** / überall.
- **S4 „Nichts hineinlegen“:** **Verkaufsblock + Lager nur Transaktionen (heute schon so, wird belegt), Vitrine bleibt Waren-Eingang** /
  auch die Vitrine nur über den Verkaufsblock befüllen.
- **S5 Name Fernzugriff-Block:** **Handelskontor (Trade Terminal)** / Shop-Terminal / Marktbuch.
