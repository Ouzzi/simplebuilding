# Plan B: Bild-Bauplan (b-picture, 19.10.2026)

Grundlage: `docs/ai/KONZEPT-BILD-BAUPLAN-2026-10-19.md`, Empfehlungen des Konzepts gelten (Besitzer).
Stufe 1 + 2 des Konzepts (simplelib-Umwandlung, Blaupausen-Import „Bild“). Karten (Simple Maps) NICHT.

## Ist-Zustand (erhoben)
- Blaupause: Bau-Sprache `BlueprintCode` (common/src/shared, 26.2 + 26.3), Grenze 32000 Zeichen,
  Aliase `$a = block`, Import-/Export-Fenster `BlueprintIoScreen` (Auto/Code/Befehle), Speichern über
  `BlueprintEditPayload` → `ModMessageHandlers.editedBlueprint` (Server prüft Länge, Signieren).
- simplelib gibt es nur auf 26.3; geteilter Code darf es nicht importieren. Muster dafür: Zwilling
  `util/LibServices` (common/src/mc26_2 ↔ mc26_3/overlay), `checkOverlays` wacht darüber.
- Server-Config `ServerTuningConfig.Blueprint` (wird an den Client gespiegelt, `ServerTuning.get()`).
- T10-Block-Protokoll (Simple Tweaks) loggt nur `BlockItem.place` (Mixin); Blaupausen-Bauten fehlen
  dort heute ganz. Es gibt keinen vorhandenen Hook für Werkzeug-Bauten.
- Farbwerte: `MapColor` gibt Wolle und Beton je Farbstoff dieselbe Farbe (16 Kollisionen) → ungeeignet.

## Entscheidungen
1. **Bild-Code = Bau-Code.** Neue Anweisung `pixels x,y,z <Zeile>`: eine Bildzeile entlang +x,
   Lauflängen-kodiert (`3a2b.c` = 3×$a, 2×$b, 1 frei, 1×$c); Buchstaben a–z, A–Z sind 1-Zeichen-Aliase
   (`$a = white_wool`), `.` = frei. 128×128 mit Dithering ≈ 17–20 K Zeichen → passt in 32000.
   Kein neues Paket, kein Upload-Kanal: der Code geht wie jeder Code durch den Editor.
2. **simplelib `com.simplelib.api.image`** (rein, ohne MC-Client): `PixelPalette` (sRGB→Lab,
   nächste Farbe ΔE76), `PixelArt` (Skalieren mit Flächenmittel, Quantisieren: aus / Floyd-Steinberg /
   Bayer 4×4, Alpha < 128 = frei), `PixelRle` (Kodieren/Dekodieren einer Zeile).
3. **Brücke** über den vorhandenen Zwilling `LibServices`: 26.3 ruft simplelib, 26.2 meldet
   „nicht verfügbar“ (Parser-Fehler `pixels_unsupported`, Bild-Knopf unsichtbar). Port später.
4. **Palette** `BlueprintPicture.PALETTE`: 16 Wolle + 16 Beton + 16 Terrakotta + Terrakotta = 49
   Vollblöcke, Farbe = sRGB-Mittel der opaken Texturpixel des Standard-Packs 26.3 (so sieht es der
   Spieler an der Wand); Werkzeug `tools/picture/block_colors.py <client.jar>` erzeugt die Tabelle.
   Option „nur Blöcke im Inventar“ (Client, Palette auf vorhandene Items begrenzt).
5. **Client-Fenster** `BlueprintPictureScreen` (vom Import-Fenster, Knopf „Bild …“): PNG aus
   `config/simplebuilding/images/`, Größe (16–128, Seitenverhältnis bleibt), Dithering
   (Floyd-Steinberg Standard / Bayer / aus), Wand/Boden, Palette (alle / Inventar), Live-Vorschau,
   Status (Größe, Blöcke, Farben, Codelänge). „Übernehmen“ setzt den Code in den Editor.
   PNG-Lesen mit `NativeImage` (kein AWT).
6. **Server-Schutz** (`server.blueprint.*`): `pictureImport` (Standard an), `picturePermission`
   (0–4, Standard 0), `pictureMaxSide` (16–128, Standard 128). `editedBlueprint` weist Code mit
   `pixels`-Zeilen ab, wenn aus / Stufe fehlt / größer als erlaubt (Aktionsleiste sagt es).
   Ehrliche Grenze: dasselbe Bild als gewöhnliche Quader-Zeilen ist ein normaler Bau.
7. **T10**: neuer kleiner simplelib-Hook `com.simplelib.api.ToolPlacements` (Abweichung: es gab
   keinen vorhandenen). SB meldet jeden Blaupausen-Bau-Block über `LibServices.toolPlaced`
   (26.2: nichts); Simple Tweaks hört in `Admin.start` mit und schreibt `P`-Einträge. Keine
   Abhängigkeit SB ↔ Tweaks, beide nur an simplelib.
8. **Lesbarkeit**: Blaupausen-Tinten `INK_SOFT`/`ERROR`/`OK` lagen auf Papier bei 3,95/4,23/3,86:1;
   werden auf ≥ 5:1 abgedunkelt (Abweichung, betrifft auch Editor/Import-Status).

## Dateien
- neu: `modules/simplelib/shared/java/com/simplelib/api/image/{PixelPalette,PixelArt,PixelRle}.java`,
  `modules/simplelib/shared/java/com/simplelib/api/ToolPlacements.java`,
  `modules/simplelib/shared/java/com/simplelib/test/PixelArtTests.java` (+ Fabric-Adapter, Neo/Forge über LibTests.ALL),
  `common/src/shared/java/com/simplebuilding/blueprint/BlueprintPicture.java`,
  `common/src/shared/java/com/simplebuilding/client/blueprint/BlueprintPictureScreen.java`,
  `tools/picture/block_colors.py`.
- geändert: `BlueprintCode` (Anweisung `pixels`, `ParseResult.pixels`), `BlueprintIoScreen` (Knopf),
  `BlueprintScreen` (Tinten), `BlueprintBuilder` (Hook), `ModMessageHandlers` (Prüfung),
  `ServerTuning(Config)`, `LibServices` (beide Zwillinge), simpletweaks `Admin` + `AdminTests`,
  Lang EN/DE, `ConfigOptionTests`, `docs/BLUEPRINT.md`, `docs/CONFIG.md`, `wiki/manual.json`,
  `docs/MODS-UND-FEATURES.md`, `BlueprintTests` + Fabric-Adapter + Katalog.

## Tests
- simplelib (module-simplelib-fabric-263): Lab-Nächstfarbe (bekannte Farben → erwarteter Index),
  Dithering deterministisch (zweimal gleich, FS ≠ aus bei Verlauf, aus bei Palettenfarben exakt),
  RLE-Rundlauf (Zufallszeilen, Transparenz, Fehler bei kaputtem Text), Skalieren/Größenlimit.
- SB (fabric-263/neoforge-263, Filter `simplebuilding:blueprint*`): Bild → Code → Parse ergibt
  die erwarteten Blöcke (Wand/Boden, Ränder transparent), Code bleibt ≤ 32000 bei 128×128,
  Server lehnt bei Schalter aus / Stufe / Übergröße ab und nimmt an, wenn erlaubt.
- Tweaks: Block-Protokoll-Test um ToolPlacements erweitert.
- Compile: `:mc26_3:fabric/neoforge/forge:compileJava`, `:compileJava` (26.2), `checkOverlays`;
  `tools/check_ui_contrast.py`; Wiki/Guides/Discover-Generatoren; `check`.
- NICHT: Client-Läufe (Verbot heute) → Fenster/Vorschau ungetestet am Bildschirm.
