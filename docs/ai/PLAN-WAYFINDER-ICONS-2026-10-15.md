# Plan: Wayfinder-Karte Icons, Configure Waypoint, Anzeigemodus (N65, claude-mapicons)

Ist: Wegpunkt = Slot, Position, Name, Farbe, optional Mob-Kopf (`head`); Leisten rechts mit Zahl, links Vanilla-Buttons;
Konfig-Dialog mit Farbpalette; Höhenlinien nur als Sitzungs-Schalter im Screen, Handkarte immer 1:1 ohne Höhenlinien.

## Daten (serverseitig validiert, Komponente `simplemaps:waypoints` wie bisher)
- `Waypoint` bekommt `icon` (beliebiges Item, ersetzt `head`; Codec liest altes Feld `head` weiter) und `face`
  (0 = Blockmodell 3D, 1..6 = eine Seite: N(Vorderseite, Standard), O, S, W, oben, unten).
- Migration: Alt-Wegpunkt ohne Icon -> Wolle der naechsten Farbe (`WaypointIcons.forColor`), Mob-Kopf bleibt Icon.
- Server (`MapsNetwork.valid`): Item registriert und nicht Luft; `face` 0..6; Blockmodell (3D) nur bei BlockItem, sonst auf Vorderseite normalisiert.
- Neue Wegpunkte: Client waehlt zufaelliges Icon aus festem Pool (`WaypointIcons.POOL`).
- Anzeigemodus pro Karte: neue Komponente `simplemaps:display` (Hoehenlinien an/aus, Massstab der Handkarte) + Payload `DisplayPayload`
  (Hand 0/1, validiert: nur Wayfinder in der Hand, Zoom aus `ZOOMS`). Menue und Handkarte lesen dieselbe Komponente.
- Zoom synchronisieren: Knopf links. Klick = Handkarte uebernimmt den Massstab des Menues (Menue zentriert auf Spieler, beide zeigen
  dasselbe Gebiet); Shift+Klick = Menue uebernimmt Massstab der Handkarte. Zoom wird nie automatisch uebernommen.

## UI
- `WayfinderLayout`: quadratische Leisten, Groesse aus verfuegbarer Hoehe (20..32 px), beide Seiten als Lesezeichen (Bronze `UiPalette.derived(0xFF6F5F4B)`),
  Icons zentriert, keine Zahlen (leere Slots: dunkles Plus). Reine Mathe, JUnit `WayfinderLayoutTest`.
- Dialog `WaypointDialog` + reine Geometrie `IconPickerLayout`/`CubeNet` (JUnit): Name, Suche + Raster (Kreativ-Suchtab, sonst Registry),
  Modus: 3D-Knopf (nur bei Blockmodell) und Wuerfelnetz (Kreuz aus 6 Seiten mit echten Texturen) zum Anklicken; Items ohne Blockmodell: nur Icon.
- Karte/Locator/Leiste zeichnen ueber `client/WaypointArt` (3D-Item, eine Blockseite als Sprite, oder Item-Sprite).
- Handkarte (`ViewTexture`) bekommt Modus (Hoehenlinien) und Massstab; Wegpunkte dort bleiben farbige Quadrate (Pixelbild).

## Verifikation
Compile 3 Loader, `:modules:simplemaps:fabric:test` (JUnit), Modul-Gametests (`simplebuilding:*maps*`), Client-Screenshots Karte + Configure.
