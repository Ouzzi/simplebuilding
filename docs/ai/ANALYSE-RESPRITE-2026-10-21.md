# Analyse Resprite (21.10.2026) – Vorbild für den Wiki-Pixeleditor

Auftrag Besitzer 21.10. (Queue „(B, b-pixeleditor, 21.10.)“): Resprite recherchieren, Funktionen und UI/UX
analysieren, daraus einen Pixeleditor mit Ebenen im Wiki-Dev-Bereich bauen. **Nur Funktions-/UX-Nachbau, keine
Code- oder Asset-Kopie.**

## Quellen und Grenzen

- App-Store-Seite „Resprite: pixel art studio“ (Jiaxing Fengeon Software, iPhone/iPad, ab iPadOS 16, Metal-Renderer,
  Version 1.62 vom 11.10.2026): https://apps.apple.com/us/app/resprite-pixel-art-studio/id1662335989
- Android/Desktop-Paket `com.fengeon.resprite_desktop`: https://apkcombo.com/resprite-pixel-art-studio/com.fengeon.resprite_desktop/
- Vorstellung durch den Entwickler (2023, sspai): https://sspai.com/post/81164
- Store-Listing Spielekonsole/Steam-Text (dekudeals): https://www.dekudeals.com/items/resprite

Ein offizielles Handbuch wurde nicht gefunden; die App selbst wurde **nicht** bedient (iOS-App, kein Gerät im CT).
UI-Aufbau und Gesten unten stammen aus Store-Texten und Release-Notes, die Werkzeug-Kürzel ergänzen wir nach
Aseprite-Konvention (dem Desktop-Standard, an dem sich auch Resprites Tastatur-Unterstützung orientiert).

## Funktionsliste Resprite

| Bereich | Resprite | Übernehmen? |
|---|---|---|
| Stift / Pinsel | Pinsel, eigene Pinsel (Drehen/Spiegeln), Pixel Perfect (keine L-Doppelpixel) | **ja**: 1-px-Stift + Pixel-Perfect-Schalter; eigene Pinsel nein |
| Radierer | als Pinsel-Modus | **ja** |
| Füllen | Farbeimer (zusammenhängend) | **ja**, plus „gleiche Farbe überall“ (Umschalt) |
| Pipette | eigenes Werkzeug, auch aus Referenzbild/UI | **ja** (Werkzeug + Alt/Langdruck), liest die Zusammensetzung |
| Auswahl / Verschieben | 3 Auswahlwerkzeuge (Rechteck, Ellipse, Lasso/Zauberstab), Füllen/Kontur, Transformieren, Kopieren/Ausschneiden/Einfügen | **ja**: Rechteck-Auswahl, Verschieben, Kopieren/Einfügen, Löschen, Spiegeln der Auswahl; Ellipse/Lasso/Zauberstab nein |
| Linie / Form | Linie (isometrische Winkel), Kurve, 3 Formwerkzeuge, „aus der Mitte“ | **ja**: Linie, Rechteck (Kontur/gefüllt); Ellipse, Kurve nein |
| Spiegeln / Symmetrie | Symmetrie waagrecht/senkrecht/45°, Mitte zurücksetzen/sperren | **ja**: senkrechte + waagrechte Achse (Texturmitte) |
| Bild spiegeln/drehen | Flip, Rotate, Scale, Free Transform, Trim | **ja**: Ebene waagrecht/senkrecht spiegeln, 90° drehen (nur quadratisch) |
| Zoom / Raster | Zoom per Pinch/Trackpad, 1×1-Raster, iso/Perspektiv-Raster | **ja**: Zoom (Rad, Pinch, Tasten), Pixelraster, 16er-Raster für Blätter; Iso/Perspektive nein |
| Rückgängig / Wiederholen | Knöpfe, 2-/3-Finger-Tipp | **ja**: Knöpfe, Strg+Z/Strg+Y/Strg+Umschalt+Z, 2-/3-Finger-Tipp |
| Paletten | frei anordnen, aus Bild sammeln, Zwischenfarben, GPL/RPL, Lospec-Import, Farbrad/HSL | **ja**: Palette aus der Textur, Vanilla-nahe Grundpalette (eigene Werte), Farbwähler mit Hex + Deckkraft; GPL-Export; Interpolation nein |
| Ebenen | Kopieren, Zusammenführen, Reduzieren, Gruppen, Farbmarken, Deckkraft, Mischmodi, Stil (Kontur/Schatten), Alpha-Sperre | **ja**: hinzufügen, duplizieren, löschen, umbenennen, ein/aus, Deckkraft, Reihenfolge, nach unten zusammenführen, Alpha-Sperre; Mischmodus nur „normal“; Gruppen/Stile nein |
| Referenzbild | mehrere, verschiebbar, zoombar, als Palettenquelle | **teilweise**: ein Referenzbild (Datei) neben der Arbeitsfläche, Pipette darauf; nicht im Repo gespeichert |
| Animation | Zeitleiste, Zwiebelhaut, Clips, Tweening | **nein** (Minecraft-Animationen = Streifen im PNG; der Editor bearbeitet das ganze Blatt) |
| Tilemap / Text / Filter | Tilemaps, Text, HSV/Blur/Schärfen/Outline | **nein** (Folgepunkt möglich: Farbton-Verschiebung der Ebene) |
| Vorschau | Vorschaufenster | **ja**: 1:1, 2:1 und 3×3-Kachel |
| Export | GIF/APNG/Spritesheet/PSD/Paket, Skalierung | **ja**: PNG (1:1, verlustfrei) + Ebenen-JSON; kein PSD/GIF |
| Oberfläche | frei schwebende Paneele, Arbeitsbereiche speichern, Themes | **nein** (feste, responsive Anordnung); hell/dunkel nach Systemvorgabe |

## UI-Aufbau (nachgebaut, nicht kopiert)

Resprite: Arbeitsfläche in der Mitte, Werkzeugleiste und Aktionsleiste frei platzierbar, Paneele (Farben, Ebenen,
Vorschau, Zeitleiste) schwebend/andockbar. Für eine Web-Seite ohne Fensterverwaltung übernehmen wir die **Rollen**,
nicht die Fensterlogik:

- **Kopfleiste**: Name/ID, Rückgängig, Wiederholen, Raster, Symmetrie, Export, Schließen.
- **Werkzeugleiste**: links (Desktop) bzw. unten (Handy, eine scrollbare Reihe, große Ziele ≥ 40 px).
- **Arbeitsfläche**: Mitte, Schachbrett-Hintergrund, Pixelraster ab Zoom 4.
- **Seitenleiste** (Desktop rechts, Handy darunter): aktuelle Farbe + Tausch-Farbe, Palette, Ebenen-Liste
  (oben = oberste Ebene, wie Resprite/Aseprite), Vorschau 1:1/Kachel, Referenzbild.

## UX-Muster

- Ein Finger/Maus zeichnet; zwei Finger zoomen/verschieben (Pinch); 2-Finger-Tipp = Rückgängig, 3-Finger-Tipp =
  Wiederholen (wie Resprite, abschaltbar nicht nötig, da nur bei kurzer Berührung ohne Bewegung).
- Langdruck (≥ 450 ms ohne Bewegung) mit dem Stift = Pipette (Resprite: Langdruck-Aktionen).
- Jeder Strich ist **ein** Rückgängig-Schritt (nicht jeder Pixel).
- Ebenen: Antippen wählt, Auge schaltet sichtbar, Doppelklick/Knopf benennt um.
- Tastatur (Aseprite-üblich): B Stift, E Radierer, G Füllen, I Pipette (Alt gedrückt = Pipette), L Linie,
  U Rechteck, M Auswahl, V Verschieben, X Farben tauschen, Strg+Z / Strg+Y / Strg+Umschalt+Z,
  + / − bzw. Mausrad Zoom, Strg+' Raster, Umschalt+N neue Ebene, Entf löscht Auswahl, Strg+C/V/D (Kopieren,
  Einfügen, Auswahl aufheben), Pfeiltasten verschieben die Auswahl, Esc schließt.

## Weglassen (begründet)

Animation/Zeitleiste, Tilemaps, Text, Filter, PSD, Gruppen, Ebenen-Stile, frei schwebende Fenster: für 16×16/32×32
Minecraft-Texturen ohne Nutzen und viel Code. Mischmodi außer „normal“: Minecraft kennt sie nicht, die Prüfung im Repo
müsste sie sonst exakt nachrechnen.
