# Konzept: Referenz-Mod-Bloecke + eigene Deep-Dark-Bloecke (Nachtrag 69, 2026-10-16)

Status: NUR KONZEPT + Vorschau. Nichts im Spiel. Freigabe des Besitzers steht aus.
Vorschau: `docs/ai/previews/claude-modref/modref_board.png` (8x, isometrisch). Generator: `tools/textures/modref_concept_2026_10_16.py`.

## 1. Mod
Video "I'm Adding Every Missing Block... Because Mojang Won't" (CinchPlaysMC, youtube NwnPH9DraFA).
Mod: **Cinch's Missing Blocks** (Autor cinchtail), https://modrinth.com/mod/cinchs-missing-blocks, CurseForge cinchs-missing-blocks.
Fabric/Forge/NeoForge/Quilt, aktuelle Builds fuer 26.2 und 1.21.x. Lizenz **All Rights Reserved** -> nur ansehen, nichts kopieren/uebernehmen, auch keine Texturen.
Ehrlicher Vorbehalt: Videoseite/Texturen waren hier nicht abrufbar (nur Titel/Beschreibung der Modseiten). Stilbeschreibung unten
ist daher aus Beschreibung + Vanilla-Konvention abgeleitet; Besitzer kann im Video Szenen nennen, die genauer interessieren.

## 2. Inhalt und Stil der Mod
Vervollstaendigt Vanilla-Familien (Stein, Ziegel) um fehlende Slab/Stair/Wall und neue Ziegelsets:
- Calcit: poliert, Ziegel, rissig, bemoost, gemeisselt, Ziegelsaeule; normal mit Slab/Stair/Wall.
- Netherrack, glatter Basalt: Slab/Stair/Wall. Rote Netherziegel: rissig, gemeisselt, Zaun.
- Geschnittener (roter) Sandstein: Stair/Wall; glatter Sandstein: Wall.
- Tropfstein, Tuff, Andesit, Granit, Diorit, Quarz, Lehmziegel, Endstein, Tiefenschiefer, Blackstone, Prismarin, Terrakotta/Beton (konfigurierbar): poliert/Ziegel/rissig/bemoost/gemeisselt-Varianten.
- Schwefel/Zinnober (aus Vanilla 26.x): rissig, bemoost, Saeule.
Stil (Vanilla-nah, Texturpaket optional): Familien-Palette bleibt erhalten, Ziegel = 2x2-Raster mit dunkler Fuge und hellem
Oberkanten-Pixel; "rissig" = dunkle Diagonalrisse; "bemoost" = gruene Flecken in den Fugen; "gemeisselt" = eingedellte Mitte mit Rahmen;
Saeule = Rillen an den Seiten, Ringmuster oben. Kontrast niedrig bis mittel, keine Outlines. Stairs/Slabs/Walls erben die Vollblock-Textur.

## 3. Abgleich mit SimpleBuilding
Ist: wir haben fuer fast alle Vanilla-Steine **Achtel (Octet)** und eigene Familien (Astralit, Nihilith, Ender Quartz mit Slab/Stair/Wall/Checker),
Cinnabar/Sulfur/Resin nur als Octet. Fehlt bei uns: die klassischen Slab/Stair/Wall-Luecken und die Zustandsvarianten.
Modulzuordnung nach PRINZIPIEN-MODUL-UNABHAENGIGKEIT: Bausteine in das Baumodul (`simplebuilding`/`simpleaesthetics`), Stone-Cutter-Rezepte
nur aus Vanilla-Zutaten, keine Abhaengigkeit zu anderen Modulen.
| Block (Familie) | Zweck | Modul | Rezeptidee |
|---|---|---|---|
| Calcite Stair/Slab/Wall | Luecke in Vanilla | simplebuilding | Standard 6/6/6 + Steinsaege |
| Calcite Ziegel + rissig/bemoost/gemeisselt/Saeule | Baupalette hell | simplebuilding | 4 Calcit; rissig = Ziegel+Spitzhacke-Schmieden/Ofen; bemoost = +Moos |
| Netherrack Stair/Slab/Wall | Nether-Bau | simplebuilding | Standard |
| Glatter Basalt Stair/Slab/Wall | dunkler Bau | simplebuilding | Standard |
| Geschn. Sandstein Stair/Wall (rot+normal), glatter Wall | Vanilla-Luecke | simplebuilding | Standard |
| Rote Netherziegel rissig/gemeisselt/Zaun | Nether-Palette | simplebuilding | Ofen / 2 Slabs / Zaun-Muster |
| Tuff-/Schlamm-/Endstein-/Deepslate-Zustaende (rissig, bemoost) | Alterungsvarianten | simpleaesthetics | Ziegel + Moos bzw. Ofen |
| Cinnabar/Sulfur: rissig, bemoost, Saeule | 26.x-Steine komplettieren | simpleaesthetics | wie oben |
| Tropfstein-/Andesit-/Granit-/Diorit-/Prismarin-Ziegelsets | neue Paletten | simplebuilding | 4 Stein = 4 Ziegel |
Geschaetzt **ca. 60-80 fehlende Einzelbloecke** in 9 Familiengruppen (die Mod nennt keine exakte Zahl; Zaehlung nach Freigabe aus dem Mod-Jar).
Wichtig: Regel 7 -> alle Texturen eigene Pixel; unsere Octet-Texturen als Grundlage, Zustaende per Generator.
Empfehlung: nur Stair/Slab/Wall + Ziegel/rissig/bemoost (Gruppen 1-5) bauen; Octet-Varianten nur fuer die neuen Ziegel.

## 4. Drei eigene Bloecke (Entwurf im Board)
1. **Sculk Deepslate** (`sculk_deepslate`). Tiefenschiefer mit tuerkisen Sculk-Adern. Fundort: Ancient City, als Randschicht um Sculk-Flecken;
   nicht herstellbar ohne Silk Touch. Verhalten: leuchtet nicht, gibt 1-2 XP beim Abbau ohne Silk Touch, droppt Cobbled Deepslate.
   Rezept: 1 Deepslate + 1 Sculk (formlos) -> 2 Sculk Deepslate. Stairs/Slab/Wall spaeter optional.
2. **Deepmoss Block** (`deepmoss_block`). Dunkelgruenes Moos auf Tiefenschiefer. Fundort: Deep-Dark-Hoehlen am Rand der Ancient City, auch
   Moos-Variante fuer Sculk-Zonen. Verhalten: Knochenmehl breitet es auf Tiefenschiefer aus (nur Dunkelheit, Lichtlevel <= 4); kein Sculk-Spread;
   Bruchgeraeusch wie Moos. Rezept: Moos Block + Deepslate -> 2 Deepmoss.
3. **Hushed Deepslate Tiles** (`hushed_deepslate_tiles`). Vertiefte Tiefenschiefer-Fliesen mit Sculk-Punkt. Verhalten: wie Wolle blockiert
   es Vibrationen (Sculk-Sensor/Warden hoeren Schritte darauf nicht, auch Laufen darauf macht keine Vibration). Rezept: 4 Deepslate Tiles + 1 Sculk
   -> 4 Hushed Tiles. Zweck: Schleichbasen und Redstone-Stille.
Alle drei brauchen nur Vanilla (Modul simpleaesthetics bzw. simplebuilding), keine Fremdabhaengigkeit.

## 5. Fragen an den Besitzer (mit Empfehlung)
1. Umfang der Referenzliste: nur Stair/Slab/Wall + rissig/bemoost (Empfehlung) oder alles (Tropfstein/Prismarin-Ziegel usw.)?
2. Welche der drei Eigenbloecke bauen (Empfehlung: alle drei; Hushed Tiles zuerst, da klarer Nutzen)?
3. Wunsch-Szenen aus dem Video, die genau getroffen werden sollen (Texturen waren hier nicht einsehbar)?
4. Modul: simpleaesthetics fuer Zustaende, simplebuilding fuer Stair/Slab/Wall ok?
5. Sculk Deepslate: darf es natuerlich generieren (Welt-Gen-Aenderung) oder nur herstellbar (Empfehlung: nur herstellbar + Ancient-City-Loot)?

## 6. Antworten Besitzer (2026-10-16, verbindlich)
M1 nur Treppe/Stufe/Mauer + rissig/bemoost (keine neuen Ziegelsets). M2 nur Sculk Deepslate und Hushed Deepslate Tiles (kein Deepmoss). M3 Hushed Deepslate Tiles zuerst. M4 Module wie vorgeschlagen (Zustände → simpleaesthetics, Treppe/Stufe/Mauer → simplebuilding). M5 Sculk Deepslate nur herstellbar + Ancient-City-Loot, keine Weltgenerierung.
