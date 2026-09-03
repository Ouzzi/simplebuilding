# Textur-Vorschläge

Zu jeder der 301 Mod-Texturen ein Alternativvorschlag, zum Anschauen.

**Nichts davon ist im Spiel.** Die Dateien werden von keinem Modell und keiner
Ressource referenziert; sie liegen nur hier.

## Ansehen

Doppelklick auf `index.html`. Kein Server, kein Build, kein Internet nötig.
Filter nach Dateiname, Gruppe (block/item/entity/trims/dev) und Vergrößerung.

## Wie die Vorschläge entstanden sind

Form und Schattierung bleiben Pixel für Pixel unangetastet – gerechnet wird in
HSV, verändert wird nur der Farbton (und maßvoll die Sättigung). Helligkeit und
Transparenz bleiben gleich, damit die Pixelgrafik lesbar bleibt. Es sind also
**Farbvorschläge, keine neu gezeichneten Motive**; ein neu gemaltes Bild könnte
ein Skript nicht seriös liefern.

Vier Regeln, je nach Textur:

| Regel | Wann |
|---|---|
| Farbton um ~150° gedreht | Standardfall, alles mit erkennbarer Farbe |
| grau → getönt | mittlere Sättigung unter 0,18; eine Drehung wäre dort unsichtbar. Der Farbton kommt aus dem Dateinamen, damit graue Texturen nicht alle gleich aussehen |
| Zweiklang getauscht | `*_checker`: nur die hellere Hälfte wird umgefärbt, das ergibt eine andere Paarung |
| eigenes Motiv | die vier Schwerkraftblöcke, siehe unten |

## Ein Befund nebenbei

`levitating_sand`, `suspended_sand`, `levitating_gravel` und `suspended_gravel`
sind **byte-identische Kopien der Vanilla-Texturen** (geprüft gegen
`minecraft-client.jar`). Im Spiel lassen sich diese Blöcke damit nicht von
gewöhnlichem Sand und Kies unterscheiden – bei Blöcken, deren ganzer Sinn die
veränderte Schwerkraft ist. Nebenbei liegen so Mojang-Assets im Repository.

Diese vier sind in der Galerie hervorgehoben und haben als Einzige ein eigenes
Motiv bekommen: aufsteigend (kühl getönt, nach oben zeigende Akzente) und
schwebend (ruhiges Punktraster). Auch das ist nur ein Vorschlag – wichtig ist,
dass sie sich überhaupt unterscheiden.

## Neu erzeugen

```bash
python art/alt-textures/generate.py
```

Braucht Pillow. Das Skript liest `src/main/resources/assets/simplebuilding/textures/`
und schreibt `original/`, `alternativ/` und `gallery-data.js` neu.
