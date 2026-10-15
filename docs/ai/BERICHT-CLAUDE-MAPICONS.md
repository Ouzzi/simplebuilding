# Bericht claude-mapicons (N65 Wayfinder-Karte: Icons, Configure, Anzeigemodus)

Branch `claude-mapicons` (Plan: docs/ai/PLAN-WAYFINDER-ICONS-2026-10-15.md), ungemergt, nicht auf master.
- Wegpunkt: `icon` (beliebiges Item, altes `head` wird gelesen) + `face` (0 = 3D, 1..6 Seite, Standard Vorderseite); Alt-Farbe -> Wolle der Farbe. Server `MapsNetwork.clean`: Item registriert/nicht Luft, 3D nur bei BlockItem (sonst Vorderseite). Neue Wegpunkte: Zufalls-Icon (`WaypointIcons.POOL`).
- Leisten: beide Seiten Lesezeichen in Bronze (#6F5F4B, gesamter Screen), quadratisch, 20-32 px je nach freier Hoehe, keine Zahlen (freie Slots: Plus). Wegpunkt-Icon zeichnet `WaypointArt` auch auf Karte und Locator.
- Configure: Name, Suche + scrollendes 9-Spalten-Raster (Kreativ-Suchtab, sonst Registry), 3D-Knopf (nur bei Blockmodell), Wuerfelnetz mit echten Seitentexturen; Raster bleibt im Dialog (Besitzer-Befund Kopfreihe).
- Anzeigemodus: Komponente `simplemaps:display` (Hoehenlinien, Handkarten-Massstab) + `DisplayPayload`; Hand-/Nebenhandkarte (`ViewTexture`) zeigt Hoehenlinien und den Massstab.
- Zoom synchronisieren (6. Knopf links): Klick = Handkarte uebernimmt Menue-Massstab und Menue zentriert auf Spieler (beide zeigen dasselbe Gebiet, nie automatisch); Shift+Klick = Menue uebernimmt Handkarten-Massstab. Begruendung: ein Knopf, zwei Richtungen, kein Zustand der Zoom ungefragt mitzieht.
- Tests: Compile 3 Loader gruen; JUnit `WayfinderLayoutTest/IconPickerLayoutTest/CubeNetTest/WaypointIconsTest` (modules/simplemaps/fabric/src/test) ohne Fehler; Modul-Gametests "Simple Maps fabric integration" 18/18 (inkl. Migration, Validierung, Display-Payload). 
- NICHT getestet: Client-Screenshots (sb-test: Xvfb ohne GLX, `Couldn't find matching GLX visual`; Smoke-Test um Configure/Hand-Hoehenlinien erweitert, ungelaufen), NeoForge/Forge-Laufzeit.
- Abweichungen/Hinweise: Handkarte zeichnet Wegpunkte weiter als Farbquadrate (Pixelbild); Seitenbild ohne Biom-Tint; mapfix-Branch aendert dieselbe Datei WayfinderScreen (LEFT_TABS 6, Marks-Schleife) -> Merge-Konflikte klein erwartet.
