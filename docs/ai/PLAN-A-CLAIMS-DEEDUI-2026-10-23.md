# Plan a-claims-deedui: Claims-Umbau Branch 2 – Urkunden-UI und Chunk-Bilder (SP22-8, 23.10.2026)

Basis: `a-claims-credits` (Branch 1, Credits/Dimensionen fertig) + `claude-wave1`. Konzept
`docs/ai/KONZEPT-CLAIMS-UMBAU-2026-10-22.md` §3.3, §4, §8 Zeile 2. Gilt nur MC 26.3 Fabric (Regel 22.10.).
**Nicht** in diesem Branch: Kartentisch (Branch 3 `a-claims-carto`), Maps-Ebene/`suggests` (Branch 4 `a-claims-maps`),
Entfernen des Claim-Tisches (Branch 3).

**C1–C13 vorläufig nach Empfehlung** (Besitzer kann ändern). Hier relevant: C1 Rechte-Code bleibt hinter Knopf „Rechte“;
C4 eigener Knopf „Kaufen“; C5 Overworld/Nether/End immer, weitere Reiter nur mit eigenem Land; C7 Land in fremder
Urkunde nur sichtbar mit irgendeinem Recht; C10 Kartenfarben-Code eigen (kein Import aus Simple Maps); C12 Hinweis
„Simple Maps empfohlen“ mit ×, lokal gemerkt; C13 gestauchte Abstände mit Beschriftung.

## Ist (erhoben)
- `deed/DeedService.snapshot` → `DeedOpenPayload` (Modus CLAIM/EDIT/READ); `DeedScreen` = Code-Editor bzw. Lesesicht bzw.
  Bestätigungsblatt. Netz-Haken `DeedNet` (Fabric: `TweaksFabric.registerDeedNetwork`, `TweaksFabricClient`).
- `Claims` (Branch 1): `credits`, `budget`, `buy/expand`, `buyAndClaim`, `unclaim(owner,key,tick)`, `nextTier`.
- Server-Tick in Tweaks über `play/mixin/PlayServerMixin` (`MinecraftServer.tickServer` RETURN).
- Kartenfarben-Muster in Simple Maps (`web/WebExport`, `Reveal.brightness`) – nur als Vorbild, kein Import.
- UI-Bausteine simplelib: `UiBoxes.box/insetColored/raised`, `UiPalette.derived`, `UiContrast`.

## Änderungen
1. **Chunk-Bilder** (`deed/ClaimPictures.java`, neu): je beanspruchtem Chunk 16×16 Vanilla-Kartenfarben (gepackte
   `MapColor`-ID inkl. Helligkeit, 1 Byte/Pixel) + Aufnahmezeit (Overworld-Spielzeit). Eigene Datei
   `<welt>/simpletweaks-claim-maps.dat` (komprimiertes NBT, atomar über `.tmp`), getrennt vom Ledger; kaputte Datei →
   `.broken-<ms>`, leer weiter. Höchstens `ClaimConfig.MAX_GLOBAL` Bilder.
   - `scan(server, claims, now)`: nur Chunks mit `getChunkNow != null` (nie laden/generieren), höchstens 4 je Durchlauf,
     ohne Bild zuerst, sonst älteste zuerst, frühestens 6000 Ticks nach dem letzten Bild; Bilder aufgegebener Claims fallen weg.
   - `tick`: alle 100 Ticks `scan`, alle 1200 Ticks speichern (nur wenn geändert); `stop` speichert (aus `Claims.stop`).
   - Sofort-Bild nach erfolgreichem Beanspruchen über die Urkunde (Blatt und „Hier beanspruchen“).
   - Abtastung `sample(level, chunk)`: Oberfläche (`WORLD_SURFACE`, erste Kartenfarbe ≠ NONE), Wassertiefe, Schattierung
     wie Vanilla-Karte (Nachbar nördlich); Decken-Dimensionen: ab Meereshöhe+32 abwärts zum ersten Boden unter Luft.
2. **Netz** (`deed/net`):
   - `DeedOpenPayload` + `DeedMap`-Teil: Kartenansicht erlaubt, Besitzer?, Credits, Standort (frei/eigen/nicht),
     Kaufstufe (Zustand, +Chunks, Level, ≤ 8 Kosten), `mapsPresent`, Reiter (≤ 32: Dimension + Chunk-Anzahl), Ergebnis der
     letzten Aktion. Alter Konstruktor bleibt (Tests).
   - `DeedChunksRequest(hand, dimension)` C→S; Antwort `DeedChunksPayload(dimension, page, pages, tiles ≤ 64)` mit
     `Tile(x, z, alter, bild 0|256 B)`; nur Chunks des Urkunden-Besitzers; nur Besitzer oder Halter mit irgendeinem Recht
     (C7); Drosselung wie `DeedService.REQUEST_TICKS`.
   - `DeedActionPayload(hand, action, dimension, chunk)` C→S: `CLAIM_HERE` (`buyAndClaim`), `BUY` (`expand`),
     `UNCLAIM` (`unclaim(owner,key)`); nur der Besitzer selbst (Urkunde in der Hand, an ihn gebunden). Danach neuer
     Schnappschuss (Ergebnis-Schlüssel) + Ton.
   - Fabric-Adapter: Registrierung + Empfänger. NeoForge/Forge nicht angefasst (Regel) → dort bleibt die alte Ansicht
     (Pakete fehlen, `DeedNet.send` liefert false) – als Offen notiert.
3. **Layout** (`deed/DeedMapLayout.java`, ohne Minecraft-Klassen): Fenster 240..380 × 150..250, Titel/Info, Reiterzeile,
   Blatt, Hinweiszeile (optional), Fußzeile 4 Knöpfe. Übersicht: Gruppen = 4er-Nachbarschaft, Kachel 36 px (Bild 32×32),
   Gruppen maßstäblich innen; Gruppen untereinander per minimalem Spannbaum (Rand-zu-Rand-Abstand in Blöcken), Kind in
   seiner 8er-Himmelsrichtung eine Kachel neben dem Elternteil, bei Überlappung weiter geschoben; Linie + Beschriftung
   „312 Blöcke NO“. Detailkarte: Maßstab 8 (bei kleinem Fenster 6/4).
4. **Bildschirm** (`deed/client/DeedMapScreen.java`, neu): Reiter je Dimension (Übersetzung `dimension.<ns>.<pfad>`,
   eigene Schlüssel für die drei Vanilla-Dimensionen, sonst Pfad), Übersicht mit Ziehen/Mausrad, Hover „Chunk x, z ·
   Blöcke … · Stand: vor …“, Klick → Detail; Detail mit Nachbarn, „Chunk aufgeben …“ + Bestätigung im UI („Kein Geld
   zurück, du erhältst 1 Credit“), „◄ Übersicht“ nur bei > 1 Chunk; Fußzeile Rechte / Kaufen / Hier beanspruchen
   (bzw. „Kaufen + beanspruchen“ ohne Credit, Kosten im Tooltip) / Fertig; Hinweis „Simple Maps empfohlen“ nur ohne
   Maps, × merkt es in `config/simpletweaks-client.json`. Rechte → `DeedScreen` (Editor), Abbrechen kehrt zur Karte zurück.
   Leere Urkunde/Urkunde ohne Land: Bestätigungsblatt wie bisher, Text um Credits ergänzt. Fremde ohne Recht: Lesesicht.
5. Lang EN/DE, Wiki `modules/simpletweaks/wiki/manual.json` (Urkunde), Generatoren.

## Tests
- JUnit `DeedMapLayoutTest` (fabric/src/test): Gruppenbildung, Kachel 36 px, keine Überlappung (auch 30 verstreute
  Gruppen), gestauchte Abstände + Beschriftung (Blöcke, Richtung), alles im Blatt für 240..380 px, Detail-Maßstab.
- Gametests (`claims/DeedUiTests.java`, Fabric-Adapter `ClaimsGameTest`):
  - `claims_deedui_pictures`: Bild nur von geladenem Chunk; entfernter Chunk bleibt ohne Bild und ungeladen; höchstens 4
    je Durchlauf; kein neues Bild vor 6000 Ticks, danach ja; aufgegebener Claim → Bild weg; Speichern/Laden; kaputte
    Datei → `.broken-*`, leer weiter; Ledger unberührt.
  - `claims_deedui_tiles`: Besitzer bekommt Kacheln (nur eigene, mit Bild/Alter, Seiten à 64), Reiterliste mit
    weiterer Dimension nur bei Land; Fremder ohne Recht bekommt nichts; Halter mit Recht bekommt Kacheln; zu schnell →
    verworfen; Open-Paket enthält Credits/Kaufstufe/`mapsPresent`.
  - `claims_deedui_actions`: Aufgeben über Paket (Credit zurück, kein Material), fremde/gefälschte Urkunde → nichts,
    Kaufen über Paket, Hier beanspruchen über Paket (+ Sofort-Bild), Codec-Rundreisen und Grenzen.
- Kontrast: Werte im Bericht; `simplelib:ui_text_contrast` unverändert grün (keine neue Palette in simplelib).

## Verifikation (nur über `/root/sbt a-claims-deedui`)
Compile `:mc26_3:fabric:compileJava` + `:modules:simpletweaks:fabric:compileJava`; Testrunner
`module-simpletweaks-fabric-263`, `module-simpletweaks-standalone-fabric-263`; Generatoren + `./gradlew check -q`.
HTML-Vorschau des tatsächlichen Layouts (Python-Port von `DeedMapLayout` + simplelib-Bausteinen):
`/root/sb-tools-a/previews/deedui-preview.html`.
Nicht getestet: Client (keine Client-Läufe erlaubt), andere Loader/Versionen (Regel).
