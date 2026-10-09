# Plan: Werkbank mit Lager + neue Tränke/Effekte (Branch `claude-q-brew`, 2026-10-09)

Queue: N26 (Werkbank mit Lager), N24 (Dunkelheits-Trank, Übelkeits-Trank), N20 (Zittern I/II, Trugbild,
Umgekehrtes Trugbild, Verblasst). Konzept der Effekte: `KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md`.
Alles nur 26.3 (`McVersion.STORAGE_CRAFTING_TABLE`, `McVersion.BREWING_EFFECTS`), 26.2 kompiliert weiter
(Stubs in `common/src/mc26_2`).

## 1. Werkbank mit Lager (`simplebuilding:storage_crafting_table`)
- Block + Block-Entity mit 9 Feldern (kein `Container`, also **kein Trichter**: das Raster ist Arbeitsfläche,
  automatisches Craften macht schon der Autonome Crafter; Trichter würden das Raster leeren).
- Menü = Vanilla-`CraftingMenu` (Rezeptbuch, Shift-Klick, Operator-Buch-Mixin bleiben), eigener `MenuType`
  (`getType()`), `stillValid` auf den eigenen Block. Beim Öffnen wird das Raster aus der Block-Entity geladen,
  jede Änderung (pro Tick verglichen) zurückgeschrieben und an andere offene Menüs desselben Blocks verteilt
  (kein Dupe bei zwei Spielern). Beim Schließen bleibt alles liegen (statt `clearContainer`).
- Abbauen: Block droppt sich selbst + Inhalt. Rezept: formlos Werkbank + Truhe.
- Block-Entity-Renderer: die Items flach auf der Oberseite im 3x3-Raster (Zeile 1 im Norden).
- Bildschirm: wie die Werkbank (Rezeptbuch), im Mod-UI-Stil (`ModScreenStyle.storageCraftingTable`, Holz-Motiv).
- Textur: eigene Pixel-Art (Generator `tools/textures/storage_crafting_table_textures.py`): Oberseite mit
  vertieftem 3x3-Ablagefeld, Seiten mit Truhenbeschlag/Schublade.

## 2. Tränke (datengetrieben, `ModBrewingProvider`)
| Trank | Zutat | Varianten | Begründung |
|---|---|---|---|
| Dunkelheit | Seltsamer Trank + **Wärter-Fühler** (neuer Warden-Drop, 1–2 Stück) | lang, Wurf, Verweil | Auftrag N24 |
| Übelkeit | Seltsamer Trank + **Roter Pilz** | lang, Wurf, Verweil | giftiger Fliegenpilz ⇒ Übelkeit; Pilze sind sonst keine Brauzutat |
| Zittern | Seltsamer Trank + **Schneeball** | lang, II (Glowstone), Wurf, Verweil | Kälte ⇒ Zittern; Schneeball ungenutzt |
| Trugbild | Seltsamer Trank + **Amethystscherbe** | lang, Wurf, Verweil | Kristall bricht das Licht ⇒ Trugbild |
| Umgekehrtes Trugbild | Trugbild + **Fermentiertes Spinnenauge** | lang, Wurf, Verweil | wie Vanilla: Spinnenauge kehrt um |
| Verblasst | Seltsamer Trank + **Tintenbeutel** | lang, Wurf, Verweil | Tinte ⇒ schwarz-weiß |
Dauern: schädliche 1:30 / 4:00 (wie Schwäche), Zittern II 0:45, Übelkeit 0:45 / 2:00.

## 3. Effekte (Client über alle 3 Loader per gemeinsamer Client-Mixin-Konfiguration)
- **Zittern:** `Hud#extractCrosshair` wird verschoben (I ±1 px, II ±3 px, zwei überlagerte Sinus = geglättet).
- **Trugbild / Umgekehrt:** `EntityRenderDispatcher#extractEntity` zeichnet statt des Mobs ein clientseitiges
  Ersatz-Wesen (Position, Drehung, Laufanimation kopiert). Tabelle `MirageTable` (geteilt, serverseitig
  testbar): Größenklasse klein/mittel/groß, Wahl stabil aus der UUID. Bosse/Spieler/Rüstungsständer nie.
- **Verblasst:** Post-Effekt `simplebuilding:faded` (Vanilla-Shader `post/color_convolve` mit Graumatrix) auf das
  Hauptbild: bei Spielansicht/Inventaren/Chat **nach** der GUI (Welt + HUD + Inventare grau), bei Menüs
  (`isPauseScreen`, z. B. Esc, Optionen) **vor** der GUI (Welt grau, Menü farbig).
- Effekt-Symbole 18x18 eigene Pixel-Art (`tools/textures/brew_effect_icons.py`).

## 4. Prüfung
- Server-Tests: Brau-Rezepte (echte `RecipeType.BREWING`-Auflösung je Schritt), Effekte nach dem Trinken
  (Stufe, Dauer, Milch), Trugbild-Tabelle, Warden-Loot, Werkbank behält Items / teilt zwischen zwei Menüs / Drop
  beim Abbau / kein Trichterzugriff. Compile 3 Loader, Filter-Tests 3 Loader.
- Client: Screenshots Effekte + Werkbank (`BrewClientTest`), Vorschauen `/root/previews/brew/`.
