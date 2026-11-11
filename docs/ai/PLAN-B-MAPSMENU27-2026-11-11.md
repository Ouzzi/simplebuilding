# Plan b-mapsmenu27 (11.11.) – Simple Maps: Kontextmenü/Wegpunkt-Platz mit echter Maus (prio27 #7)

Quelle: /root/sb-tools-b/prio27.md #7, docs/ai/RELEASE-ABNAHME-A-2026-11-09.md Z.69 („context-menu“/„pick-slot“-Bilder zeigen kein Menü). Basis hub/b-bundle180-gen 7def212d6.

## Ist-Stand
- `WayfinderScreen.mouseClicked`: Rechtsklick auf die Karte (`InputConstants.MOUSE_BUTTON_RIGHT`) öffnet `UiContextMenu` „Wegpunkt erstellen / Hierher zentrieren“;
  „erstellen“ setzt `picking`, ein Linksklick auf ein Lesezeichen rechts legt den Wegpunkt an.
- `ModuleClientSmoke` (clienttest) ruft `mouseClicked` mit **rohen Zahlen** `MouseButtonInfo(1, 0)` / `(0, 0)` auf. 26.3 zählt Maustasten nach SDL
  (links 1, Mitte 2, rechts 3; docs/ai/memory/mc-26.3-overlay-linie.md Z.42). „1“ ist also LINKS (= Karte ziehen), „0“ gar keine Taste →
  kein Menü, kein Platz-Wählen. Altes Bild rel10/pipe/112 bestätigt: Karte ohne Menü.
- Vermutung: Smoke-Test-Fehler, kein Mod-Fehler. Beweis nur mit echter Maus (Memory ui-test-echte-maus).

## Umsetzung
1. Smoke: alle vier rohen Tastennummern → `InputConstants.MOUSE_BUTTON_RIGHT/LEFT`.
2. Echte-Maus-Probe `modules/simplemaps/tools/maps_menu_probe.py` (XTest-Muster aus simpleearth/menu_probe.py, Welt/Befehle/Screenshots über den
   vorhandenen SortProbe-Kanal von Simple Interfaces im Integrations-Client – kein neuer Mod-Code):
   Karte in die Hand, echter Rechtsklick in die Welt öffnet den Wayfinder, echter Rechtsklick auf die Karte → Menü (Pixelvergleich),
   Linksklick „Wegpunkt erstellen“ → Platz-Wählen (Panel grau), Linksklick Lesezeichen 1 → Wegpunkt angelegt, Rechtsklick auf das Lesezeichen → Menü
   „Konfigurieren/Löschen“. Geometrie aus `WayfinderLayout` (Python-Nachbau).
3. Falls die Probe ein fehlendes Menü zeigt → Fehler in WayfinderScreen/UiContextMenu fixen (dann Abweichung hier notieren).

## Test
Ein sb-test-Lauf (nur bei leerem sb-test, Regel 16 + Auftrag): Probe + `module-simplemaps-client-263` (Smoke-Bilder context-menu/pick-slot neu) nacheinander.
Ergebnis + Bilder: /root/sb-tools-b/previews/b-mapsmenu27-preview.html.

## Teststand (11.11. 14:07 UTC)
- Ursache gefunden (Code + altes Bild rel10/pipe/112): Smoke klickte mit rohen Zahlen 1/0 = 26.3-SDL links/keine Taste → kein Menü. Mod-Code korrekt
  (`MOUSE_BUTTON_RIGHT`), Smoke auf Konstanten umgestellt (ebenso die beiden „structures-off“-Klicks, die bisher ins Leere gingen).
- Probe-/Client-Lauf **nicht gefahren**: sb-test 11:07–14:07 UTC durchgehend belegt (Gate 6e6faa936, A-Jobs, bprep b-bundle182), 3-h-Grenze erreicht.
- Nachholen bei leerem sb-test: `/root/sb-tools-b/mapsmenu27/run.sh` (Probe + `module-simplemaps-client-263`, Ergebnis /var/tmp/sb/mapsmenu27 auf sb-test).
