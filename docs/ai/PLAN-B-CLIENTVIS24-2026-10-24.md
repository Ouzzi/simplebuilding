# Plan B: Client-Sichtprüfung 24.10. (VIS24, Branch b-clientvis24)

Besitzer-Auftrag: visuelle Abnahme von allem, vom User-Interface bis zur User-Interaktion, im echten Client.
Nur 26.3 Fabric. Ausnahme „keine Client-Läufe“: höchstens EIN Client-Lauf zur Zeit über `/root/sbt`.

## Ist-Zustand
- Kern-Client-Suite (`client-fabric-263`, `common/src/shared/clientgametest`, ~183 Screenshots): Oktanten-/Stab-Screen,
  Trichter, Schmiedetisch, Trim-Referenz, Mod-Config, Kreativ, Rucksäcke, Truhen, Auto-Smither, Fletching, Astral-
  Verzauberer, Bauplan-Editor, Mega-Guide, HUD/Tooltips. Nur Kern + 12 per `implementation` eingebundene Module.
- Integrations-Client (`:integration:runClientGameTest`, alle Module als Jars) mit je einem `ModuleClientSmoke` je Modul
  (Shops, Tweaks/Urkunde/Invsee, Stats, Respawn, Models, Maps, Questionary, Crucible, Storage-Tisch, Death Vault …).
- Nirgends abgedeckt: Vanilla-Stationen mit Mod-Rahmen (Werkbank, Ofen, Amboss, Schleifstein, Webstuhl, Steinsäge,
  Kartentisch, Braustand, Verzauberung, Crafter, Leuchtfeuer, Händler, Spender, Shulker), Formsäge, Disc-Presse,
  Notenblatt-Editor, Handbücher aller Module, Pausenmenü, Steuerung (Tastenbelegungen), Config-Screens aller Mods per ModMenu,
  Tooltip-Hover im Inventar mit Mod-Items. Simple Earth ist nicht in wave1 → entfällt.

## Umsetzung
1. `integration/src/gametest/.../clienttest/Vis24ClientTest.java` (Fabric-Client-Gametest, nur Vanilla-/Fabric-API +
   Reflexion, damit keine Modul-Kompilierkopplung): Welt erzeugen, Plattform, dann je Eintrag Block setzen und per
   echtem Rechtsklick (`MultiPlayerGameMode.useItemOn`) bzw. Item-Benutzung (`useItem`) öffnen, auf Screen warten,
   Screenshot. Unbekannte IDs werden übersprungen, jeder Schritt einzeln abgesichert (ein Fehler bricht den Lauf nicht ab),
   Log-Zeilen `[vis24] OK|SKIP|FAIL <name> -> <Screen-Klasse>` + Zusammenfassung `[vis24] screens shot: N`.
   Interaktionen: Slot klicken/ziehen (Truhe), Mausrad (Kreativ-Liste), Knopf klicken (Pausenmenü → Optionen),
   Tooltip-Hover (Inventar).
   Zusätzlich: Prüfung jedes offenen Screens auf rohe Übersetzungsschlüssel in Widget-Texten/Titel (Log `[vis24] RAWKEY`).
2. `integration/build.gradle`: Modus `-PmoduleClientTest=vis24` (nur Vis24) und `vis24all` (alle Modul-Smokes + Vis24).
3. `tools/testrunner/run.py`: Ziele `vis24-client-263` / `vis24all-client-263` (Log-Audit wie audit-models, Marker).
4. Läufe (nacheinander, je vorher `df`/`free`): (a) `vis24all-client-263`, (b) `client-fabric-263` (Kern-Screens),
   (c) nach Fixes `vis24-client-263` als Beleg.
5. Auswertung per Bild: Überlauf, Kontrast < 4,5:1, lila-schwarz, rohe Schlüssel, Ausrichtung, Vanilla-Nähe.
   Kleine klare Fehler direkt beheben; Größeres als Queue-Abschnitt „Client-Sicht 24.10. (VIS24)“.
6. Vorschauseite `/root/sb-tools-b/previews/vis24-client.html` (verkleinerte Bilder als data:-URI, je Bild Befund, < 15 MB).
7. Generatoren + `./gradlew check`, Push `git push /root/sb.git HEAD:b-clientvis24`.

## Risiken
- Laufzeit/RAM: ein Client-Lauf belegt sb-test lange → nur einer zur Zeit, nie kill.
- API-Namen 26.3 (gameMode/useItem) → Compile vorab per `:integration:compileGametestJava`.
- Manche Screens öffnen nur mit Zustand (Shops mit Besitzer, Urkunde) → diese bleiben bei den Modul-Smokes.

## Verifikation
Compile, Läufe (a)–(c) mit Zahlen, Bildsichtung, `gradlew check`. Nicht getestet: NeoForge/Forge/andere MC-Linien.
