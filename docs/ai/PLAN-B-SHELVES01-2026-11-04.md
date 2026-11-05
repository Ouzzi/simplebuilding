# PLAN b-shelves01 (04.11.2026): End-Regale, gemeißelte Regale, Nihilit-Buch, Mod-Bücher in gemeißelten Regalen

Besitzer 04.11. (/root/b-antworten-20261104c.md Punkt 3, Queue DW04C-3): „die ashwood und duskwood regale und chisseled
bookshelve sind noch nicht implementiert … gehören dann in simple end und simple enchanting. werapped varianten gehören in simple
blocks und in simple enchanting. zusätzlich fehlen die nihilit books und auch die kompartibilität der nihilit und blaze books in die
chissled bookshelves. dann auch die variation der books in der chissled bockshelve wie in vanilla.“ Basis beb03c24e (Bündel 107).
Nur 26.3 Fabric.

## Ist-Stand
- SB (26.3, `McVersion.ASTRAL_ENCHANTING`): `crimson/warped_blazewood_planks`, `crimson/warped_blazewood_bookshelf` (2 Punkte am
  Enchanter, Tag `simpleenchanting:blaze_shelves`), `chiseled_crimson/warped_blazewood_bookshelf` (Vanilla-Block + Vanilla-BE per
  `SkullBlockEntityTypeMixin`), `blaze_book` (nur Rezeptzutat der Lohen-Regale, kein eigener Verzauberungs-Bonus). Zeile
  `astral_enchanting` im SB-Tab „Funktional“.
- Gemeißelte Lohen-Regale: Vorderseite „besetzt“ = alle sechs Bücher in Lohenbuch-Orange (Variante A von a-tex29) – keine
  Variation wie Vanilla. Blaze-/Nihilit-Bücher sind NICHT in `minecraft:bookshelf_books` → passen in kein gemeißeltes Regal.
- Simple Enchanting: Zwillinge (nur ohne SB) der Lohen-Bretter/-Regale, keine gemeißelten (PLAN-B-ENCH02 Rest).
- Simple End: Ascheholz/Dämmerholz mit vollem Vanilla-Holzsatz inkl. Regal (`*_shelf`), KEIN Bücherregal, KEIN gemeißeltes.
- Kein Nihilit-Buch.

## Entscheidungen (selbst entschieden, begründet)
1. **„werapped“ = warped** (Wirr-Lohenholz; Crimson gehört mit dazu). Die Ids bleiben `simplebuilding:` (ENCH01, Besitzer 03.11.:
   „simple building soll alle baublöcke besitzen“; Simple Blocks ist der Baublock-Tab der SB-Familie, SB hängt seine Baublock-Zeilen
   dort an). Umsetzung: Lohen-Bretter, -Regale, gemeißelte Regale und beide Bücher wandern aus der SB-Funktional-Zeile in eine
   eigene Baublock-Zeile `blazewood` (steht damit im Tab „Simple Blocks“) und stehen zusätzlich im Simple-Enchanting-Tab.
   Simple Enchanting allein bekommt die fehlenden gemeißelten Zwillinge (`simpleenchanting:chiseled_*_blazewood_bookshelf`, Vanilla-BE
   per Modul-Mixin). Verworfen: Id-Umzug nach `simpleblocks:` (Welt-Migration, gleicher sichtbarer Effekt, widerspricht ENCH01).
2. **End-Regale in Simple End** (Holz-Heimat): `simpleend:{ashwood,duskwood}_bookshelf` und `simpleend:chiseled_{ashwood,duskwood}_bookshelf`,
   Vanilla-Regeln (Rezept 6 Bretter + 3 Bücher bzw. 6 Bretter + 3 Holzstufen, Abbau 3 Bücher/Behutsamkeit, Axt, brennbar,
   `minecraft:enchantment_power_provider` → am Vanilla-Tisch wie ein Bücherregal). Der Verzauberungs-Teil gehört Simple Enchanting:
   neuer Tag `simpleenchanting:end_shelves` (beide Namensräume `required:false`) → 2 Punkte wie Lohen-Regale an Dark und Astral
   Enchanter (keine Neubalance: Lohen-Regale wirken weiter voll; „Stoffe“-Frage KONZ-ENCH01 damit beantwortet).
3. **Nihilit-Buch** `simplebuilding:nihilith_book` (neben dem Lohenbuch, Nihilit ist SB-Material): Rezept Buch + 4 Nihilitsplitter
   (Plus-Form; Splitter seltener als Lohenstaub). Wie das Lohenbuch die Zutat der starken Regale: mit SB brauchen die End-Bücherregale
   3 Nihilit-Bücher (eigenes Rezept `simplebuilding:{wood}_bookshelf`, Simple-End-Rezept dann per Ladebedingung aus) und geben beim
   Abbau 3 Nihilit-Bücher zurück (SB ersetzt die Simple-End-Beute per Fabric `LootTableEvents.REPLACE`, gleiche JSON mit Nihilit-Buch).
   Ohne SB: normale Bücher (wie die Simple-Enchanting-Zwillinge der Lohen-Regale). Kein eigener Verzauberungs-Bonus (das Lohenbuch hat
   auch keinen).
4. **Bücher in gemeißelten Regalen**: Lohen- und Nihilit-Buch kommen in `minecraft:bookshelf_books` (Vanilla- und alle Mod-Regale,
   Trichter, Comparator-Signal unverändert Vanilla). Gemeißelte Regale zählen nach Vanilla-Regel NICHT als Verzauberungskraft.
5. **Buch-Variation wie Vanilla**: alle gemeißelten Mod-Regale nutzen Vanillas „besetzt“-Bücher (je Fach andere Farben) mit Rahmen im
   eigenen Holz (abgeleitet, nicht committet). Steckt ein Lohen- oder Nihilit-Buch in einem Fach, zeichnet ein Block-Entity-Renderer
   genau dieses Fach mit Mod-Buchrücken (je Fach eigener Farbton aus der Buch-Rampe – Variation wie Vanilla). Dafür sendet die
   Vanilla-BE ihre Bücher an den Client (Mixin `getUpdatePacket/getUpdateTag`, wie Vanillas `ShelfBlockEntity`).
6. **Optik (A8/A8b)**: End-Bücherregal = eigene Pixel (Rahmen aus den Ascheholz-/Dämmerholz-Brettern, Bücher in Vanilla-Verteilung mit
   gedämpften End-Farben), gemeißelt = Vanilla-Struktur umgefärbt (abgeleitet), Nihilit-Buch = Vanilla-Buchform mit Nihilit-Rampe
   (abgeleitet, wie das Lohenbuch). Vorschau nur als Bild `abnahme-b/bilder/SHELVES01.jpg` (Raster + Ingame-Bild).

## Dateien
- SB: `ModItems` (NIHILITH_BOOK), Tab-Zeilen, Discover, Lang EN/DE (26.3-Overlay + src/main), Item-Modell, Rezept (generated von Hand),
  `data/minecraft/tags/item/bookshelf_books.json`, Rezepte der End-Regale mit Nihilit-Buch, Loot-REPLACE (Fabric), BE-Sync-Mixin,
  `ChiseledBooksRenderer` (Fabric-Client), Wiki-Manual, Preis.
- Simple End: `EndWood` (bookshelf, chiseledBookshelf), BE-Mixin, `gen_data.py` (Modelle/Zustände/Beute/Rezepte/Tags/Lang), Texturen,
  Tab, Discover, Wiki, Preise, Gametests.
- Simple Enchanting: `end_shelves`-Tag + Regel, gemeißelte Zwillinge + Mixin + Daten, Tab/Discover, Wiki, Preise, Gametests.
- Texturen: `tools/textures/shelves01_2026_11_04.py` (eigene + abgeleitete), `vanilla_derived.py` (Einträge).

## Verifikation
- Lokal: `precheck.py --fix` (alle Python-Prüfungen), Textur-Skript `--check`.
- sbt (ein Lauf): Compile `:mc26_3:fabric:compileJava` + Module end/enchanting; Gametests `module-simpleend-*`, `module-simpleenchanting-*`
  (fabric + standalone), SB gefiltert (`data_integrity*`, `enchanting_block*`); `check_client_mixins`; `wiki/generate.py --all --check`;
  Client-Bild (`-PmoduleClientTest=simpleend`, Szene ShelvesShotsClient) für SHELVES01.jpg.
- Gametests: Regal-Kraft (End-Regal 2 Punkte, gemeißelt 0, Vanilla-Tisch zählt End-Regal), Bücher einsetzen (Lohen/Nihilit in Vanilla-
  und Mod-gemeißeltes Regal), Comparator-Signal wie Vanilla (letztes Fach), Rezept/Beute vorhanden.
