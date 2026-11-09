# Plan B-LAYERS12 (09.11.2026) – Simple Layers: Texturen im Vanilla-Stil

Branch `b-layers12`, Basis `a-layers10` (af285a18e, in Bündel 159). Nur 26.3 Fabric.
Auftrag: Besitzer-Einwand zu LAYERS10 – Texturen „mehr im Vanilla-Stil“. Arbeitsweise A8c (mehrere Runden).

## Ist-Stand
- Generator `tools/textures/layers10_2026_11_08.py` zeichnet alle 22 Simple-Layers-Texturen (19 Block, 2 Item + 3 animiert) aus
  bilinearem Value-Noise mit kontinuierlicher Helligkeit → 30–120 Farbtöne je Textur, weiche Flecken, wirkt verschwommen
  („Photoshop-Wolke“). Vanilla (Messung an Jar-Texturen 26.3): 3–8 Töne in klaren Pixel-Clustern (Schnee 3, Sandstein 5–7,
  Wolle ~40 durch Webmuster, Grundgestein 5), Items mit dunkler Kontur und 3–4-stufiger Schattierung (Licht oben links).
- `guide_book.png` (module_guide_books) und `icon*.png` (mod_icons) gehören zu anderen Generatoren → nicht Teil dieses Punkts.

## Vorgehen
1. Generator in place umbauen (gleiche Dateinamen, gleiche Modelle, gleiche .mcmeta-Struktur → keine Asset-Umbenennung,
   kein data_integrity-Bedarf). Grundsatz: feste Paletten (4–6 Töne je Material), Pixel-Cluster statt Verlauf, Vanilla-Raster
   (Kanten-Fasen hell oben/links, dunkel unten/rechts), Items mit Kontur.
2. Je Runde: alle Texturen erzeugen → Kontaktbogen 1x + 8x nearest neben Vanilla-Referenzen (Jar) + Blockkontext (3×3-Kachel,
   Raum-Ansicht Wand/Boden/Decke) → Bewertung (Vanilla-Nähe, Lesbarkeit, Härte A8b ~7 %, Tonanzahl, Kacheln) → nächste Runde.
   Runden-Stände als Kopie im Scratchpad `/root/sb-tools-b/layers12/r<N>/`.
3. Vorschau `/root/sb-tools-b/previews/b-layers12-preview.html`: alt → R1 → R2 → R3 je Textur, Bewertung je Runde,
   Raumkontext, Antwortcode LAYERS12.
4. Ebenen-JSON per `layers.save` (unverändert), `layers.py check`, `check_missing_textures`; Wiki-`textures.js` auf sb-test neu.
5. Test: ein sbt (Compile + module-simplelayers-fabric-263 + standalone).

## Entscheidungen (selbst)
- Keine Varianten A/B mehr (LAYERS10-Wahl wolke/tapete ist durch den Einwand überholt): eine Endfassung je Textur;
  Antwortcode erlaubt „ok“ oder Nachbesserung je Textur.
- Wolke: Vanilla hat keinen Wolkenblock; Maßstab Schnee/Puderschnee (fast flach, 3–4 Töne) + Wolle-Körnung.

## Runden-Bewertung
Messung je Runde: Härte (A8b, Lumen-Sprung > 40) und Tonanzahl; Render 1x + 8x nearest neben Vanilla, 3×3-Kachel, Raumkontext.

- **alt (a-layers10):** 30–120 Töne je Textur aus Value-Noise, wirkt verschwommen; Härte 0–3 % nur weil alles weich ist.
- **Runde 1:** feste Paletten + Pixel-Cluster (Radius 1). Klare Pixel, aber Cluster zu groß: Wolke/Tapete/Grundgestein
  marmoriert, Flecken wiederholen sich in der Kachel. Teppich-Webmuster zu regelmäßig (Sackleinen). Wasserfleck harter Ring
  (17 %). Streifen kaum lesbar, Riss nur Punkte. Flasche/Schild gut.
- **Runde 2:** anisotrope Cluster: Wolke/Wolkenstein flach mit waagrechten Flocken (wie Schnee/Wolle), Tapete senkrechte
  Fasern, Streifen klar, Wasserfleck unregelmäßig 2 Töne (11,7 %), Grundgestein waagrechte Cluster wie Vanilla, Riss als
  gezackte Linie, Schlüsselbart lesbar. Neu falsch: Teppich wie Eichenholz-Maserung; Tür/Schild-Deckel fleckig.
- **Runde 3 (eingebaut):** Teppich als Ein-Pixel-Flor mit dunklen Feuchteflecken, Tür gebürsteter Stahl, Schild-Deckel fein,
  Riss mit gestrichelter Aura. Raumkontext stimmig, keine sichtbare Wiederholung. Härte: Flächen 0–3 % (Vanilla Schnee/
  Sandstein 0 %), Funktionsblöcke 9–13 %, Schild/Riss/Grundgestein/Items 20–32 % (Vanilla-Pendants: Grundgestein 47 %,
  Redstone-Lampe 48 %, Trank 23 %). 3–8 Töne je Textur.

## Abweichungen
- Generator-Vorschau `--preview` ohne A/B-Wahl (überholt); die Runden-Vorschau entsteht im Scratchpad
  (`/root/sb-tools-b/layers12/scr/preview.py`), Vanilla-Bilder nur dort.
- Wiki-Sheets/`wiki/data/textures.js` werden auf sb-test mit `wiki/generate.py --all` erzeugt (lokaler Teil-Sync hätte
  fremde Vanilla-abgeleitete Sheets gelöscht) und per `-gen` zurückgeholt.

## Ergebnis
Vorschau `/root/sb-tools-b/previews/b-layers12-preview.html`, Antwortcode LAYERS12. Offen: Besitzer-Abnahme, Client-Sicht.
