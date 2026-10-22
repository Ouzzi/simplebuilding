# Plan b-icons21b (21.10.): Mod-Icons mit Motiv + Hintergrund (IC21), kleiner Strong-Upgrade-Splitter (WI21)

Queue: „(B, b-icons21b, 21.10.)“. Grundlage: b-modicons (`tools/textures/mod_icons.py`), b-wither21
(`tools/textures/wither21_textures.py`). Prinzip A8 (vanilla-nah, Ebenen-Quellen).

## Ist-Zustand
- 44 Mods, je `assets/<modid>/icon.png` (16×16) + `icon_128.png` (Forge) + `icon.layers.json`; 40 gezeichnet
  (ASCII/prozedural, freistehend ohne Hintergrund), 4 Besitzer-Bilder (SB, Money, Riding, Visuals) importiert.
- Die 4 Besitzer-Bilder sind bereits randlos deckend (16×16 voll, eigener Hintergrund: hellgrau, grau, Funkeln, blau).
- `checkModIcons` (`tools/check_mod_icons.py`) prüft Deskriptoren + `mod_icons.py --check`.
- Strong-Upgrade-Splitter: Variante A eingebaut (Keil ≈12×13 px), B (schlanker Span ≈9×13 px).

## IC21 – Umsetzung
1. **Hintergrund (Familienstil):** volle 16×16-Kachel wie die Besitzer-Bilder: 1-px-Rahmen (dunkler Ton der
   Mod-Farbe), innen Fase wie Vanilla-Knöpfe (oben/links hell, unten/rechts dunkel), Fläche zweistufig diagonal
   (hell oben links), drei helle Funkelpunkte wie bei Riding/Visuals (nur wo das Motiv frei lässt). Je Mod eine
   eigene Grundfarbe (`BG`), Optimization-Familie in Indigo-Abstufungen.
2. **Motiv = Charakteristik des Mods:** wo das Modul ein typisches eigenes Item hat, dessen echte Item-Textur
   (z. B. Hammers → Eisen-Vorschlaghammer, Music → Platte, Storage → Rucksack, Mobs → Netherherz, Beacons →
   Strong-Upgrade-Vorlage, Tools → Biom-Pinsel, Maps → Wegfinder-Karte, Tweaks → Claim-Urkunde, Lib → Enderit-Barren,
   Crucibles → Keramik-Lavaeimer, End → Driftfrucht, Drachenkampf → Drachenchitin, Mob Manners → Froscheimer,
   Trims → Leucht-Vorlage). Mods ohne eigene Items (Sounds, Weather, Ovens, Opt*, …) behalten ihr gezeichnetes
   Motiv (es zeigt das Typische bereits) – jetzt auf Hintergrund.
3. **Ebenen:** `Hintergrund`, `Rahmen`, dann Motiv-Ebenen (gezeichnete Gruppen bzw. eine Ebene `Motiv`).
   Die Item-Motiv-Ebene wird beim Schreiben aus der Item-Textur (1. Animationsbild) übernommen; danach ist die
   Ebenen-Quelle maßgeblich (`--check` vergleicht Hintergrund mit dem Skript und die Datei mit ihrer Komposition),
   damit fremde Textur-Änderungen `check` nicht brechen. Fabric `icon` / NeoForge `iconFile` / Forge `logoFile`
   unverändert.
4. **Abweichung Regel 7 (Rand frei):** der Hintergrund läuft bis an den Rand – vom Besitzer ausdrücklich verlangt
   und wie seine vier Bilder; das Motiv selbst hält den Rand weiter frei (Prüfung bleibt für gezeichnete Motive).
5. **Besitzer-Bilder SB/Money/Riding/Visuals:** unverändert – sie haben bereits einen eigenen, deckenden
   Hintergrund; ein zweiter Rahmen würde ihr Motiv beschneiden. Sie sind die Stilvorlage der Kacheln.

## WI21 – Splitter
- Zwei neue, deutlich kleinere Splitter (≈8×10 px statt 12×13, Größe wie Vanilla-Amethyst-/Echo-Splitter):
  **SA** aus Variante A (Keil mit Goldkreuz), **SB** aus Variante B (schlanker Span, Gold an der Kante).
- Eingebaut: **SB** (Besitzer nannte „b aber kleiner“ zuerst). Wahl per Code `SP21 A|B`.
- Netherherz/Juwel: nur prüfen, dass b-rarity21 Variante A animiert (keine Änderung).

## Vorschau
`scratchpad/icons21b-preview.html`, ohne externe Skripte: alle Icons 16-fach + 1×/2× auf hell/dunkel, alt neben
neu; Splitter alt A/B neben neu SA/SB; Code-Feld „IC21B <mod>=ok|neu …“, „SP21 A|B“. Als Artifact veröffentlicht.

## Verifikation
`mod_icons.py --check`, `wither21_textures.py --check`, `layers.py check`, data_integrity, Guides/Wiki --all
(textures.js)/Discover auf sb-test, `./gradlew check -q`. Kein Java → kein Compile. Client-Sicht nicht getestet.
