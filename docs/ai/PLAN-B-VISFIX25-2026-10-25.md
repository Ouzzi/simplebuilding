# Plan B: Client-Sicht-Befunde beheben (VIS24-2…11, Branch b-visfix25)

Basis b-clientvis24 (Vis24ClientTest, Ziel `vis24-client-263`). Nur 26.3 Fabric. Befunde und Bilder:
`.claude/QUEUE.md` Abschnitt „Client-Sicht 24.10. (VIS24)“, Vorschau /root/sb-tools-b/previews/vis24-client.html.
Bestehende Look-Entscheidungen bleiben; echte Gestaltungsfragen nur als Vorschlag.

## Ist-Zustand und Änderung je Befund
- **VIS24-2 Starkes Leuchtfeuer** (`simplebeacons/.../client/StrongBeaconScreen.java`): Bild 269 px hoch (Besitzer T12:
  Vanilla-Layout + 2 Reihen), Fenster bei 854×480/Skala 2 nur 240 → `topPos` −14, oben und unten abgeschnitten.
  Layout bleibt (Besitzer-Entscheidung). Fix: passt das Bild nicht ins Fenster, steht es oben bündig (2 px Rand) und
  lässt sich mit dem Mausrad verschieben (`scroll`, Widgets werden mitgeschoben, Slots folgen `topPos`); schmale
  Scroll-Leiste rechts neben dem Bild als Hinweis. Große Fenster: unverändert (zentriert, keine Leiste).
- **VIS24-3 Handbücher** (`tools/guides/module_guides.py` `split_to`): die kürzere Sprache wird zur Seitenzahl-Angleichung
  stur in der Wortmitte geteilt. Fix: Teilungspunkt im mittleren Bereich (¼…¾) an einem Satzende (`. ! ? :`), sonst an
  `, ;`, sonst wie bisher die Mitte. Seiten neu erzeugen (Generator auf sb-test).
- **VIS24-5 Pferde-Inventar** (`simpleriding/.../mixin/client/HorseshoeScreenMixin.java`): Huf-Lasche fest IRON (grau),
  der gestylte Reittier-Kasten (simpleinterfaces, StorageStyles.HORSE #8B5E3C) braun. Fix: Lasche nimmt das Sattel-
  leder des Kastens, wenn der Screen gestylt ist (`ContainerStyleHints.isRestyled`, Framework-API, keine Kopplung an
  simpleinterfaces); ohne Stil (Vanilla-PNG grau) bleibt IRON – so passt die Lasche in beiden Fällen.
- **VIS24-6 Formsäge** (`simpleblocks/.../saw/client/ShapecutterScreen.java`): eigener Screen mit Vanillas
  stonecutter.png, darum ohne den Steinsägen-Stil. Fix nach Vorbild der Steinsäge: simpleinterfaces stylt ihn als
  „stonecutter“ (gleiche Geometrie/Slots, gleicher Config-Schalter): `StationStyles.LOOKALIKES` (Klassenname →
  Stil-ID, eigener Menütyp) + `@Pseudo`-Mixin `ShapecutterStationMixin` (Hintergrund-Blit → `StationScreens.draw`,
  Scroller/Kacheln → `thumb`/`tile`). Ohne simpleblocks passiert nichts (Pseudo, require 0). „Inventar“-Label fällt
  wie bei allen gestylten Screens weg (ContainerLabelsMixin).
- **VIS24-7 Verknüpfte Truhe** (`simpleinterfaces/.../client/LinkedPanel.java`): im Inventar passt bei 37 px Platz
  über dem GUI nicht einmal eine Reihe (42 px) → Panel bei y −7. Fix: oben auf 1 px geklemmt; es liegt dann über dem
  oberen Rahmen des Inventars (dessen Titel/Slots bleiben frei). Große Fenster unverändert.
- **VIS24-8 Kartentisch** (`StyledScreens.drawLabels`): alle gestylten Titel bei y 6; die Kartenvorschau beginnt bei
  y 13, „Table“ berührt sie. Vanilla hebt genau diesen Titel auf y 4. Fix: nur beim Kartentisch Vanillas Titelhöhe.
- **VIS24-9 Hinweise:** (a) Simple-Models-Hilfe/Handbuch: Entwicklerpfad `docs/modules/simplemodels.md` → Verweis aufs
  Wiki (Lang + manual.json); (b) Simple-Visuals-Modelle: Leerhinweis bei leerer Liste (EN+DE);
  (c) Pausenmenü-Iconreihe ist Vanilla (kein Mod-Mixin) → keine Änderung; (d) Fletching-Freiraum = Gestaltungsfrage →
  nur Vorschlag.
- **VIS24-10** (simpletweaks-Smoke im Sammellauf): braucht den schweren Sammellauf zur Diagnose → offen lassen, Hinweis.
- **VIS24-11:** Kisten (simplestorage) haben absichtlich keine GUI (Rechtsklick nimmt/legt), Shops ohne Besitzer
  ebenso → erwartet, [x].

## Risiken
- Pseudo-Mixin auf eine Klasse eines anderen Jars: Ziel muss zur Mixin-Zeit ladbar sein; `require = 0` je Injektion.
- Leuchtfeuer-Scroll: Vanilla-Knöpfe hängen an `topPos` beim init → nach dem Verschieben alle Widgets mitschieben.
- Handbuch-Generator ändert viele Lang-Seiten (nur Teilungspunkte); Diff prüfen.

## Verifikation
Compile `:modules:{simplebeacons,simpleriding,simpleblocks,simpleinterfaces,simplevisuals,simplemodels}:fabric:compileJava`
+ `:mc26_3:fabric:compileJava`; Modul-Gametests simpleinterfaces/simpleriding/simplebeacons; Client-Lauf
`vis24-client-263` (einer zur Zeit) und Modul-Smoke `simplequalityoflife` (Verknüpfte Truhe), Screenshots sichten,
Vorher/Nachher in /root/sb-tools-b/previews/visfix25-preview.html. Generatoren + `./gradlew check -q`.
Nicht getestet: NeoForge/Forge/andere MC-Linien (NUR 26.3 Fabric).
