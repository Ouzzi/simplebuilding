# Konzept: Claims-Umbau (Queue SP22-8, 22.10.2026)

Branch `a-claims22` (Basis `claude-wave1` f3e9c5546) · **nur Konzept, kein Spielcode** · gilt nur MC 26.3 Fabric
(Regel „NUR 26.3 FABRIC“ vom 22.10.; andere Loader/Versionen erst auf Anforderung).
Mockup: `/root/sb-tools-a/previews/claims22-preview.html` (eigenständige HTML-Seite, nicht im Repo).

## 0. Besitzer-Vorgabe (Wortlaut Queue SP22-8, Besitzer 22.10.)

> SP22-8 (Claims, Umbau; erst Konzept, dann Umsetzung; Abnahme im Spiel) Claim-Tisch „erscheint mir gerade nicht gut“. Er wird ersetzt:
> - **Anzeige:** Claims sieht man in erster Linie über die Wayfinder-Karte (Simple Maps), eigene und fremde, wenn die Ebene an ist. Fehlt Simple Maps, zeigt ein Hinweis, dass es für Claims empfohlen ist.
> - **Claim-Urkunde wie eine Karte:** Sie zeigt den geclaimten Bereich.
> - **Wiederherstellung am Kartentisch:** Leere Urkunde rein, persönliche Urkunde raus. Kopieren geht auch. Fremde oder schon personalisierte Urkunden sind für andere nicht änderbar, aber es gibt immer einen Weg zur eigenen Urkunde.
> - **Urkunden-UI:**
>   - Zeigt die eigenen Chunks und ihre Abstände zueinander.
>   - Reiter Overworld/Nether/End plus einer je weiterer Dimension.
>   - Bei 1 Chunk die Karte dieses Chunks. Bei mehreren Chunks klickbare Kacheln (Größe 2×2 Slots); ein Klick zeigt die Karte des Chunks, zurück führt zur Übersicht.
>   - Aktualisiert wird nur, wenn der Chunk geladen ist; es wird nichts extra geladen.
> - **Dimensionen und Kosten:** Claims gelten in allen Dimensionen. Jeder weitere Chunk kostet mehr, wie bisher.
> - **Aufgeben:** Wer einen Chunk aufgibt, bekommt nichts zurückgezahlt, aber einen Chunk-Credit für einen neuen Claim.

Frühere Besitzer-Entscheidungen, die weiter gelten (`PLAN-CLAIM-URKUNDE-GUI-2026-10-13.md` §„Besitzer-Entscheidungen“,
`ClaimConfig.DEFAULT_TIERS`): Rechte-Code-Form, Sichtbarkeit Standard `all` (Mitgliederlisten nie gesendet), Kopie der
Kopie verboten, Erweitern nur Besitzer, keine Rückerstattung bei `unclaim`, Kostenkurve 21.10. (Stufe n kostet n Diamanten,
Level 10/15/20/30/35/40/50/60, Land 4 → 6 → 8 → 12 → 16 → 24 → 32 → 48 → 64).

## 1. Ist-Stand (erhoben auf f3e9c5546)

### 1.1 Simple Tweaks – Claims (`modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/`)
| Teil | Stand | Dateien |
|---|---|---|
| Ledger v2 | Chunk-Zeilen `Key(dimension, chunk) → Claim(owner)`, Länder mit Rechte-Code, `budgets[]` je Besitzer; JSON atomar, fail closed (`locked`) | `claims/ClaimStore.java`, `Claims.java` |
| Rechte-Code | Buchstaben B/D/U/C/R/A/M, Rollen, Abschnitte `[x,z]`; Parser serverseitig | `claims/ClaimCode.java`, `ClaimRights.java` |
| Budget/Kosten | `budgetOf(stored, held) = min(maxClaimsPerPlayer, max(stored ?: start, start, held))`; `expand()` kauft die nächste Stufe (Diamanten + Level), braucht eigene/leere Urkunde im Inventar, wird **am Claim-Tisch** ausgelöst | `ClaimConfig.java` (`DEFAULT_TIERS`), `Claims.expand` |
| Dimensionen | Allowlist `dimensions` (Standard Overworld/Nether/End, ≤ 32); weitere Dimensionen (Simple Dimensions, Datapacks) sind **nicht** beanspruchbar | `ClaimConfig.dimensions`, `Claims.create` |
| Urkunde | `simpletweaks:claim_deed`, Stapel 1, Komponente `simpletweaks:deed {owner, owner_name, generation}` (nur Anzeige, nie Berechtigung). Rechtsklick: leer → Bestätigungsblatt „Hier beanspruchen“ (CLAIM), gebunden → Rechte-Code-Editor (EDIT) oder Lesesicht (READ) | `deed/ClaimDeedItem.java`, `DeedService.java`, `deed/client/DeedScreen.java`, `deed/net/*` |
| Kopieren | Werkbank formlos: gebundene + leere Urkunde → Kopie (generation 1), Kopie der Kopie nicht möglich | `deed/DeedCloningRecipe.java`, `recipe/claim_deed_copy.json` |
| Claim-Tisch | Block ohne Block-Entity/Container; Karte (Chunk-Raster ohne Gelände, Farbe je Besitzer) + Liste + Knopf „Land erweitern“; Rezept Papier+Urkunde/4 Bretter; Sichtbarkeit `claimVisibility` | `table/*` (ClaimTableBlock, TableService, TableScreen, TableLayout, ClaimGrid, TableVisibility, 4 Pakete) |
| Maps-Ebene | `table/ClaimMapLayer` registriert Quelle `claims` bei `framework.api.MapLayerSources` (ViewerCollector, gleiche Sichtbarkeit wie Tisch, Streifen `area:RRGGBB:M:W`, ≤ 256 Marker). Simple Maps kennt Tweaks nicht | `table/ClaimMapLayer.java` |
| Tests | 11 Testklassen (~2000 Zeilen), u. a. `TableTests`, `ExpansionTests`, `ClaimDeedTests`, `Stage4ClaimTests`; JUnit `DeedLayoutTest`, `DeedTextTest` | `claims/*Tests.java` |

### 1.2 Simple Maps (`modules/simplemaps/shared/java/com/simplemaps/`)
- Wayfinder-Karte mit Ebenen (`client/MapLayers`, Zustand je Client in `config`, Ebenen **standardmäßig aus**, Umschalter je
  Quelle); `net/LayerService` antwortet nur für **bereits erkundete** Kacheln, Rate-begrenzt.
- Kartentisch: `mixin/CartographyTableMenuMixin` (Priorität 1000) ersetzt Slot 0/1 durch breitere Slots
  (`Cartography.mapSlot/additionalSlot`) und umhüllt den Ergebnis-Slot. SimpleBuilding (Blaupause, 1100) und Simple Music
  (Notenblatt, 1200, `SheetCopy.wrapSlots`) hängen sich mit höherer Priorität als **Hüllen** davor – erprobtes Muster
  für mehrere Mods am selben Kartentisch.
- `modules.json`: simplemaps `requires [simplelib]`, `optional [simplemoney]`; simpletweaks `requires [simplelib]`,
  `optional [simpledimensions, simplebuilding, simplemoney, simpleweather]` – **simplemaps fehlt in `optional`/`suggests`**.

### 1.3 Gefundene Lücke (wichtig für „Chunk-Credit“)
`budgetOf` nimmt `max(..., held)`: Wer Land aus der Zeit vor dem Budget hält (kein gespeicherter Wert, z. B. 10 Chunks bei
Start 4), verliert beim Aufgeben den Platz wieder (Budget fällt mit `held`). Das widerspricht „Aufgeben gibt einen
Credit“. → Migration in §6.1 schreibt das Budget einmal fest.

## 2. Ist → Soll (Übersicht)

| Thema | Ist | Soll |
|---|---|---|
| Claims ansehen | Claim-Tisch (Raster ohne Gelände) | **Wayfinder-Karte** Ebene „Claims“ (eigene + fremde); **Urkunde** zeigt das eigene Land mit Gelände |
| Ohne Simple Maps | Tisch zeigt alles | Urkunde zeigt eigenes Land; Hinweis „Simple Maps empfohlen“ in Urkunde, Wiki, Tooltip-freie Erstnutzungs-Tipps |
| Urkunde | Code-Editor bzw. Bestätigungsblatt | **Karten-Ansicht** (Reiter je Dimension, Kacheln, Detailkarte); Rechte-Code bleibt über Knopf „Rechte“ |
| Kopieren | Werkbank | **Kartentisch** (Hauptweg) + Werkbank bleibt (wie Vanilla-Karten) |
| Wiederherstellen | – (leere Urkunde beansprucht nur, wenn man kein Land hat) | **Kartentisch**: leere Urkunde → eigene persönliche Urkunde, immer |
| Erweitern (kaufen) | Knopf am Tisch | In der Urkunde (Fußzeile „Kaufen“ bzw. „Hier beanspruchen“ kauft mit) |
| Dimensionen | Allowlist 3 | **alle** Dimensionen (`"*"`), Allowlist bleibt als Admin-Einschränkung |
| Kosten | Stufen global | unverändert global über alle Dimensionen („wie bisher“) |
| Aufgeben | Budget bleibt (bei Altland Lücke §1.3) | Budget bleibt = **Chunk-Credit**, sichtbar als Zahl; Lücke geschlossen |
| Claim-Tisch | Block mit Rezept | entfällt; vorhandene Tische werden zum **Kartentisch + Urkunde** (§6.3) |

## 3. Datenmodell

### 3.1 Credits (keine neue Währung, Budget sichtbar gemacht)
- Gespeichert bleibt je Besitzer **ein** Wert `budgets[owner]` = gekaufte Landgröße (umbenannt im Text zu „bezahlte Chunks“).
- **Chunk-Credits = bezahlte Chunks − gehaltene Chunks** (über alle Dimensionen). Es gibt keinen zweiten Zähler, der
  auseinanderlaufen kann.
- Beanspruchen verbraucht einen Credit; Aufgeben gibt einen Credit zurück (Budget bleibt, `held` sinkt). Material und Level
  werden **nie** erstattet („kein Geld zurück“).
- Ist kein Credit frei, kostet der nächste Chunk die **nächste Stufe** (Kurve 21.10.); eine Stufe bringt so viele Credits
  wie sie Land hinzufügt (+2, +2, +4, +4, +8, +8, +16, +16). Jeder Kauf kostet mehr als der vorige; **je Chunk** bleibt
  der Preis der Kurve 21.10. aber fast gleich (½–1 Diamant und 3–7,5 Level je Chunk, schwankend) – siehe C3.
- Neue Spieler: `startChunks` (4) Credits gratis.
- `budgetOf` neu: `min(maxClaimsPerPlayer, max(stored ?: startChunks, startChunks))` – **ohne** `held`. Kein Spieler
  verliert Land, weil die Migration (§6.1) `stored = max(start, held)` einmal schreibt.
- Admin: `/claim admin budget <Spieler> <n>` bleibt; neu `/claim credits` (eigene: bezahlt, gehalten, frei, nächste Stufe).

### 3.2 Dimensionen
- `ClaimConfig.dimensions` erlaubt `"*"` = alle geladenen Dimensionen (Standard neu `["*"]`). Eine konkrete Liste bleibt
  möglich (Admin will z. B. eine Farmwelt ausnehmen); `"!simpledimension:farm"` als Ausschluss ist **nicht** nötig (C6).
- Reihenfolge der Reiter: Overworld, Nether, End, danach weitere Dimensionen alphabetisch nach ID, **nur wenn** der
  Besitzer dort Land hat (C5). Reitertitel = übersetzter Dimensionsname (`dimension.<ns>.<path>`, Fallback Pfad).
- Spawnpuffer gilt wie bisher nur in der Spawn-Dimension; Weltgrenze je Dimension wie bisher.

### 3.3 Chunk-Bilder (neu, rein kosmetisch)
- Je beanspruchtem Chunk ein Bild **16×16 Vanilla-Kartenfarben** (`MapColor` + Helligkeitsstufe wie Vanilla-Karte,
  1 Byte je Pixel = 256 Byte) und der Spielzeitpunkt der Aufnahme.
- Eigene Datei `<welt>/simpletweaks-claim-maps.dat` (komprimiertes NBT), **getrennt vom Ledger**: ein defektes Bild sperrt
  nie Claims; kaputte Datei → umbenennen `.broken-<Zeit>`, leer weiter (wie `framework.api.ConfigFiles`).
  Grenze 10000 Claims × 256 B ≈ 2,5 MiB.
- **Aktualisierung nur geladener Chunks:** Server-Tick-Dienst prüft alle 100 Ticks die beanspruchten Chunks, die
  `level.getChunkSource().getChunkNow(x,z) != null` liefern (kein Laden, kein Generieren), und nimmt höchstens 4 Bilder
  je Durchlauf auf, älteste zuerst, frühestens 6000 Ticks (5 min) nach dem letzten Bild. Zusätzlich sofort beim
  Beanspruchen (der Spieler steht dort, Chunk ist geladen).
- Gelöschte Claims → Bild wird mitgelöscht. Ohne Bild zeigt die Urkunde ein Papier-Raster „noch nicht erkundet“.

### 3.4 Urkunde (Komponente unverändert)
`simpletweaks:deed {owner, owner_name, generation}` bleibt (nur Anzeige). Kein Kartenbild im Item (keine Datenflut im
Inventar, keine fremden Daten in Item-NBT); die Bilder kommen beim Öffnen vom Server.

## 4. Urkunden-UI

### 4.1 Wer sieht was
| Halter | Ansicht |
|---|---|
| leere Urkunde | Blatt „Hier beanspruchen“ (wie heute CLAIM) mit Credit-/Kostenzeile; danach gebunden |
| eigene Urkunde | volle Karten-Ansicht, Kaufen, Beanspruchen, Aufgeben, Knopf „Rechte“ (Code-Editor wie heute) |
| fremde Urkunde, Halter hat Recht M | Karten-Ansicht ohne Kaufen/Aufgeben, Knopf „Rechte“ (Editor mit M-Grenzen wie heute) |
| fremde Urkunde, sonstige Rechte | Karten-Ansicht nur lesen (C7) + „Du darfst hier: U C“ |
| fremde Urkunde, keine Rechte | nur „Urkunde von X“ (heutige READ-Ansicht, keine Chunk-Lagen) |

### 4.2 Übersicht (mehrere Chunks)
```
┌ Claim-Urkunde von Ouzzi ───────────────────────── 6 Chunks · 2 Credits ┐
│ [Overworld][Nether][End][Farmwelt]                                    │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │  ┌──┬──┐                                     ┌──┐                 │ │
│ │  │▓▓│▓▓│  ·········· 312 Blöcke NO ········· │▓▓│                 │ │
│ │  ├──┼──┤                                     └──┘                 │ │
│ │  │▓▓│▓▓│ ◄ du                                                     │ │
│ │  └──┴──┘                                                          │ │
│ │        ⋮ 1 840 Blöcke S                                           │ │
│ │       ┌──┐                                                        │ │
│ │       │▓▓│                                                        │ │
│ │       └──┘                                                        │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ Tipp: Mit Simple Maps siehst du alle Claims auf der Wayfinder-Karte.  │
│ [Rechte]  Nächster Chunk: Credit frei      [Hier beanspruchen] [Fertig]│
└───────────────────────────────────────────────────────────────────────┘
```
- **Kachel = 2×2 Slots** (36×36 px inkl. Rand, Bild 32×32 = 16×16 Pixel ×2) im Vanilla-Slot-Stil (`UiBoxes.slot`).
- **Gruppen:** benachbarte Chunks (4er-Nachbarschaft) bilden eine Gruppe und liegen maßstäblich aneinander.
- **Abstände:** Gruppen werden in ihrer echten Himmelsrichtung zueinander angeordnet, die Lücke aber gestaucht
  (eine Kachel Abstand + gepunktete Linie mit Beschriftung „312 Blöcke NO“, Abstand Rand zu Rand in Blöcken, Richtung in
  8 Himmelsrichtungen). Gemessen wird zur jeweils nächsten Gruppe (minimaler Spannbaum), damit die Linien nicht kreuzen.
- Passt die Übersicht nicht ins Fenster: ziehen (Maus) und Mausrad-Scrollen; Startansicht auf die Gruppe, in der man
  steht, sonst die größte. Eigener Standort als Pfeil ◄ (nur gleiche Dimension).
- Hover über Kachel: „Chunk 12, -4 · Blöcke 192..207 / -64..-49 · Stand: vor 3 min“ (Zeit seit Bild).
- Reiter ohne eigene Chunks (Overworld/Nether/End immer sichtbar): leeres Blatt „Hier hast du kein Land“.

### 4.3 Detailkarte (1 Chunk, oder Klick auf Kachel)
```
┌ Claim-Urkunde von Ouzzi ─────────────────── Overworld · Chunk 12, -4 ┐
│ ┌────────────────────────┐  Blöcke 192..207 / -64..-49                │
│ │ 16×16 Kartenpixel ×8   │  Stand: vor 3 min (geladen)                 │
│ │ (128×128 px, wie       │  Nachbarn: ▲ eigen  ▶ frei  ▼ eigen  ◀ frei │
│ │  Vanilla-Karte)        │                                            │
│ └────────────────────────┘  [Chunk aufgeben …]                         │
│ [◄ Übersicht]                                              [Fertig]   │
└───────────────────────────────────────────────────────────────────────┘
```
- Bei genau 1 Chunk in der Dimension öffnet direkt die Detailkarte, ohne Knopf „Übersicht“.
- „Chunk aufgeben …“ fragt nach („Kein Geld zurück, du erhältst 1 Credit.“, `UiBookmarks.confirm`), dann Server.
- Rahmen wie Vanilla-Karte (Papierrand), Darstellung folgt `UiBrightness`; Text ≥ 4,5:1 Kontrast (A7).

### 4.4 Beanspruchen und Kaufen
- „Hier beanspruchen“ nur aktiv, wenn man in einem freien, erlaubten Chunk steht. Mit Credit: sofort. Ohne Credit zeigt der
  Knopf die Stufe: „Kaufen + beanspruchen: 2 Diamanten, 15 Level (+2 Chunks)“; Server bucht erst das Budget, dann den
  Claim, nimmt dann Material/Level (bestehende Reihenfolge aus `expand`: scheitert das Speichern, kostet es nichts).
- Separater Knopf „Kaufen“ (ohne Claim) bleibt in der Fußzeile, damit man vorab Credits erwerben kann (C4).
- `/claim create` und `/claim unclaim` bleiben und nutzen dieselben Server-Methoden.

### 4.5 Netz (Grenzen, Server entscheidet)
| Paket | Richtung | Inhalt / Grenzen |
|---|---|---|
| `DeedOpenPayload` (erweitert) | S→C | + `credits`, `paid`, `nextTier` (Land, ≤ 8 Kosten, Level), `mapsPresent`, Reiterliste (≤ 32 Dimensionen, Chunk-Anzahl je Reiter) |
| `DeedChunksRequest(hand, dimension)` | C→S | ≥ 5 Ticks Abstand (wie `DeedService.REQUEST_TICKS`), nur eigene Urkunde bzw. Recht ≠ 0 |
| `DeedChunksPayload` | S→C | ≤ 256 Chunks (`maxClaimsPerPlayer` ≤ 256) × (x, z, Alter, Bild 256 B) ≈ 66 KiB Spitze, in Seiten à 64 |
| `DeedActionPayload(hand, action, dimension, chunk)` | C→S | `CLAIM_HERE`, `BUY`, `UNCLAIM(chunk)`; Server prüft Urkunde in der Hand, Besitzer (Aufgeben/Kaufen nur Besitzer), Abklingzeit, `locked` |
Fremde Besitzer, Mitglieder oder Codes werden nie gesendet; Bilder nur von Chunks des Urkunden-Besitzers.

## 5. Kartentisch (Wiederherstellen, Kopieren)

| Oben (Slot 0) | Unten (Slot 1) | Ergebnis | Wer |
|---|---|---|---|
| leere Urkunde | – (leer) | **persönliche Urkunde des Benutzers** (generation 0) | jeder, immer („Weg zur eigenen“) |
| eigene Urkunde | leere Urkunde | Kopie (generation 1), Original bleibt | Besitzer (und M, C8) |
| fremde Urkunde | beliebig | **kein Ergebnis**, Fehlerkreuz | – (nicht änderbar) |
| Kopie (generation 1) | leere Urkunde | kein Ergebnis (Kopie der Kopie verboten) | – |
- Das Ergebnis wird erst bei `onTake` (Server, Spieler bekannt) an den **nehmenden** Spieler gebunden; die Anzeige im
  Ergebnis-Slot zeigt den Namen des Menü-Spielers. Wer die Urkunde später weitergibt, gibt eine fremde Urkunde weiter.
- Umsetzung als eigener Mixin in Simple Tweaks, Priorität **1300**, nach dem Muster `SheetCopy.wrapSlots` (Simple Music):
  hüllt Slot 0/1 (lässt zusätzlich `claim_deed` zu) und den Ergebnis-Slot ein, `setupResultSlot` HEAD nur wenn eine Urkunde
  beteiligt ist. Funktioniert mit und ohne Simple Maps/SB/Music (jede Hülle reicht Unbekanntes weiter).
- Werkbank-Kopie (`DeedCloningRecipe`) bleibt wie Vanilla-Karten (beide Wege) – C9.
- Hinweise (A6): Tooltip der leeren Urkunde „Kartentisch: eigene Urkunde“ (≤ 2 Zeilen), Erstnutzungs-Tipp
  (`framework.api.FirstUseTips`) beim ersten Öffnen eines Kartentischs mit Urkunde im Inventar, Guide-Seite.

## 6. Migration

### 6.1 Ledger
- Beim ersten Start mit dem neuen Stand: für jeden Besitzer ohne gespeichertes Budget `budgets[owner] = max(startChunks,
  held)`, für alle mit Budget `max(stored, held)`. Einmalig, im selben atomaren Schreibvorgang; vorher Kopie
  `simpletweaks-claims.v2.json`. Marker `creditsMigrated: true` im Ledger (additiv, ältere Builds ignorieren das Feld nicht
  schädlich – sie lesen `budgets` gleich).
- Kein Chunk, kein Recht, kein Code ändert sich.

### 6.2 Config (`config/simpletweaks-claims.json`, `configVersion` 2 → 3)
- Steht in der Datei genau die alte Standardliste `[overworld, the_nether, the_end]`, wird sie einmal zu `["*"]` (war nie
  eine Wahl – gleiche Logik wie die `enabled`-Migration in `ClaimConfig.migrate`). Jede andere Liste bleibt (Admin-Wahl).
- `claimVisibility` bleibt; sie gilt nun für Maps-Ebene (Hauptanzeige). Wert `off` = keine Claims auf der Karte.

### 6.3 Vorhandene Claim-Tische (Block + Item)
- Registry-IDs `simpletweaks:claim_table` (Block, Item) bleiben registriert, damit Welten nicht „unbekannter Block“ melden.
- **Block in der Welt:** Rechtsklick oder Abbau ersetzt ihn durch einen **Vanilla-Kartentisch** und wirft eine **leere
  Urkunde** aus (Rezept war Papier + Urkunde + 4 Bretter; Kartentisch = 2 Papier + 4 Bretter → kein Gewinn, kein Verlust
  der Urkunde). Kein Tick-Scan nach Tischen (nichts extra laden).
- **Item im Inventar:** Setzen setzt einen Kartentisch und gibt eine leere Urkunde; Tooltip „Veraltet: wird zum
  Kartentisch“. Nicht mehr im Kreativ-Tab, Rezept + Rezept-Fortschritt gelöscht, JEI/Wiki/Discover ohne Eintrag.
- Pakete `Table*`, `TableScreen`, `TableService` entfallen; `TableVisibility` und `ClaimGrid.color` ziehen nach
  `claims/` (von Maps-Ebene und Urkunde genutzt).
- Lang-Schlüssel des Tisches bleiben nur für Name/Tooltip des Alt-Items.

### 6.4 Urkunden
Vorhandene Urkunden (generation 0/1) gelten weiter, öffnen die neue Ansicht. Keine Änderung an Komponente/Codec.

## 7. Modulgrenzen

| Aufbau | Verhalten |
|---|---|
| **Simple Tweaks allein** | Urkunde mit Karten-Ansicht (eigene Bilder aus §3.3), Kartentisch-Wege, Credits, alle Dimensionen. Fremde Claims sieht man nur über `/claim` am Standort und den Schutz selbst. Hinweis „Simple Maps empfohlen“ in der Urkunde (Fußzeile, abschaltbar per Klick ×, lokal gemerkt) und im Wiki. |
| **Tweaks + Simple Maps** | zusätzlich Ebene „Claims“ auf der Wayfinder-Karte (vorhanden, `ClaimMapLayer`), eigene hervorgehoben, fremde mit Name je `claimVisibility`; Hinweis entfällt. |
| **Simple Maps allein** | keine Claims-Ebene, kein Knopf (Quelle nicht registriert) – wie heute. |
- **Kein Import** zwischen Tweaks und Maps (Regel 3). Kopplung bleibt `framework.api.MapLayerSources`.
- „Maps vorhanden?“: Schalter beim Start aus `FabricLoader.isModLoaded("simplemaps")` im Loader-Adapter, `shared` liest
  (Regel 4) und zusätzlich `SubMods.enabled("simplemaps")`. Der **Server** entscheidet und schickt `mapsPresent` mit
  (die Ebene kommt vom Server).
- `modules.json` simpletweaks `optional` + `fabric.mod.json` `suggests`: `simplemaps` ergänzen (Regel 7).
- Kartentisch: eigener Mixin in Tweaks (Hüllen-Muster), keine Änderung an Simple Maps nötig.
- Vanilla-Kartenfarben: nur Vanilla-API (`BlockState.getMapColor`), kein Code aus Simple Maps (`TileCodec`) – der bleibt
  Maps-intern; doppelte 20 Zeilen sind billiger als ein simplelib-Umbau (C10).

## 8. Umsetzung in 4 Branches (Reihenfolge)

| # | Branch | Inhalt | Tests (nur 26.3 Fabric) |
|---|---|---|---|
| 1 | `a-claims-credits` | §3.1 Credits (budgetOf ohne `held`, Migration §6.1), §3.2 `"*"` + Config-Migration §6.2, `Claims.claimAt(owner, key)`/`unclaim(owner, key)`/`buyAndClaim`, `/claim credits`, Lang/Wiki | JUnit `ClaimConfig` (`"*"`, Migration alte Liste → `*`, eigene Liste bleibt); Gametests `claims_credit_*`: Aufgeben gibt Credit (auch Altland ohne Budget), kein Material zurück, Kauf + Claim atomar, Claim in Custom-Dimension (`simpletweaks:claims_*` Testumgebung), `maxClaimsPerPlayer` deckelt; bestehende `ExpansionTests` angepasst. Targets `module-simpletweaks-fabric-263`, `-standalone-` |
| 2 | `a-claims-deedui` | §3.3 Chunk-Bilder + Dienst, §4 Urkunden-UI (Reiter, Kacheln, Gruppen/Abstände, Detail, Aufgeben, Kaufen), Pakete §4.5, Rechte-Editor hinter Knopf | JUnit `DeedMapLayout` (Kacheln 36 px, Gruppen, gestauchte Abstände, keine Überlappung, Fenster 240..380 px), `ChunkPictureCodec`; Gametests: Bild nur bei geladenem Chunk (entladener bleibt alt, kein Laden), Bild beim Claim, Pakete abgelehnt bei fremder Urkunde/ohne Recht/zu schnell, Aufgeben über Paket; Kontrast `ui_text_contrast`; Vorschau-HTML für den Besitzer |
| 3 | `a-claims-carto` | §5 Kartentisch (Mixin 1300), §6.3 Alt-Tisch-Umwandlung, Tisch-Code/Rezept/Tab entfernen, Tooltips, Erstnutzungs-Tipp, Guide/Wiki/Discover | Gametests: leere Urkunde → eigene; eigene + leere → Kopie; fremde/Kopie → kein Ergebnis; Bindung an nehmenden Spieler; Alt-Tisch Rechtsklick/Abbau → Kartentisch + Urkunde; Alt-Item setzen. `integration-263` mit Simple Maps + Music + SB (Hüllen-Reihenfolge, Wayfinder-/Notenblatt-Kopie gehen weiter) |
| 4 | `a-claims-maps` | §7 `mapsPresent`, Hinweis, `optional`/`suggests`, Maps-Ebene: eigene Gruppen-Umrisse, Tooltip „dein Land · 2 Credits“ (nur eigene), Wiki beider Module | Gametests: Ebene liefert eigene + fremde nach `claimVisibility`, `mapsPresent` false im Standalone, true in `integration-263`; `module-simplemaps-standalone-fabric-263` ohne Ebene |
Abhängigkeiten: 1 → 2 → 3; 4 nach 1 (parallel zu 2/3 möglich, kleine Überschneidung nur Lang-Dateien). Je Branch eigener Plan
`docs/ai/PLAN-A-…`, Abnahme im Spiel durch den Besitzer am Ende (Client-Läufe nicht auf sb-test).
Aufwand grob: 1 ≈ 0,5, 2 ≈ 1,5, 3 ≈ 1, 4 ≈ 0,5 Agent-Tage.

## 9. Risiken
- **Bild-Dienst Last:** nur `getChunkNow`, ≤ 4 Bilder / 100 Ticks, kein Laden; Test misst, dass entladene Chunks nicht
  geladen werden (`ServerChunkCache` Zähler).
- **Kartentisch-Konflikte** mit Maps/SB/Music: Hüllen-Muster + Integrations-Test; ohne Urkunde reicht die Hülle alles durch.
- **Credit-Migration:** einmalig, Kopie v2 vorher; Test mit Altland > Start.
- **Datenschutz:** Urkunde zeigt nur Land des Urkunden-Besitzers und nur Haltern mit Recht; Kopie weitergeben = bewusste
  Freigabe (Wiki erwähnt das).
- **Wegfall Tisch-Liste „wer hat was“:** ersetzt durch Wayfinder-Ebene (erkundete Gebiete) und `/claim admin listall`.

## 10. Offene Fragen an den Besitzer (je mit Empfehlung)
- **C1 Rechte-Code in der Urkunde:** bleibt der Code-Editor (hinter Knopf „Rechte“) oder kommt er weg?
  *Empfehlung:* bleibt, als zweite Seite der Urkunde; die Karten-Ansicht ist die erste.
- **C2 Urkunde in der Hand wie eine Karte** (zweihändig gehalten, Bild in der Ego-Sicht, Rahmen an der Wand)?
  *Empfehlung:* zuerst nein – Item-Textur kartenähnlich (Papier mit Siegel und Chunk-Raster), Bild nur in der UI;
  Halte-Darstellung später als eigener Punkt (Client-Mixin, Abnahme im Spiel nötig).
- **C3 Kosten:** (A) Stufen-Pakete wie bisher: jeder *Kauf* teurer (1 … 8 Diamanten, 10 … 60 Level), je Chunk aber fast
  gleich (½–1 Diamant, 3–7,5 Level). (B) echter Einzelpreis, der je Chunk steigt, z. B. Chunk n kostet 1 Diamant +
  min(30, n−4) Level (bis 64 Chunks: 60 statt 36 Diamanten). *Empfehlung:* A („wie bisher“, Kurve 21.10. bleibt); B nur,
  wenn „jeder Chunk teurer“ wörtlich gemeint ist.
- **C4 Kaufen ohne Beanspruchen** (Credits vorab kaufen)? *Empfehlung:* ja, eigener Knopf.
- **C5 Reiter weiterer Dimensionen** immer oder nur mit eigenem Land? *Empfehlung:* Overworld/Nether/End immer, weitere nur mit
  eigenem Land (sonst bis zu 32 leere Reiter).
- **C6 Alle Dimensionen auch auf bestehenden Servern?** *Empfehlung:* ja, aber nur wenn die alte Standardliste unverändert in
  der Config steht (eigene Admin-Listen bleiben).
- **C7 Wer darf das Land in einer fremden Urkunde sehen?** *Empfehlung:* nur wer dort irgendein Recht hat; andere sehen nur
  „Urkunde von X“.
- **C8 Kopieren durch Verwalter (M)?** *Empfehlung:* ja (M darf heute schon den Code ändern); andere nicht.
- **C9 Werkbank-Kopie behalten?** *Empfehlung:* ja, wie Vanilla-Karten (Kartentisch und Werkbank).
- **C10 Kartenfarben-Code teilen** (simplelib) statt 20 Zeilen doppelt? *Empfehlung:* nein, erst bei drittem Nutzer.
- **C11 Alte Claim-Tische:** Umwandlung in Kartentisch + leere Urkunde (beim Benutzen/Abbauen)?
  *Empfehlung:* ja; Alternative wäre ersatzloses Löschen beim Laden.
- **C12 Simple-Maps-Hinweis abschaltbar?** *Empfehlung:* ja, kleines × in der Urkunde, lokal gemerkt; Wiki-Hinweis bleibt.
- **C13 Abstände anzeigen** als gestauchte Lage mit Beschriftung (Blöcke + Himmelsrichtung) statt maßstäblich?
  *Empfehlung:* ja (maßstäblich wären zwei Basen 2000 Blöcke auseinander nur Punkte).
