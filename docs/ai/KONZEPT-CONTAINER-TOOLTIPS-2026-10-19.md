# Konzept: Shulker-Box- und Endertruhen-Tooltip

Stand 19.10.2026, B (b-concepts19). Nur Konzept, kein Code. Freigabe durch den Besitzer offen.

## Ziel
Beim Hovern über eine Shulkerkiste im Inventar den Inhalt als Raster sehen; beim Hovern über eine Endertruhe (Item) den eigenen Endertruhen-Inhalt des Spielers.

## Vorbilder (nur Ideen)
- Shulker Box Tooltip (Fabric/Forge/NeoForge): Vorschau per Shift, Alt+Shift voll mit Slot-Positionen, funktioniert für weitere Container; Server-Teil optional liefert Endertruhen-Vorschau. Aktuell für 26.1 gepflegt.
- Packed Inventory: Vanilla-ähnliche Tooltips für Endertruhe, Shulker, Karte.
- Handy Shulker: Endertruhen-Inhalt wird beim Login und bei jedem Schließen der Endertruhe vom Server gesendet.
- Container Tooltips: Hinweis, dass Client und Server die Mod brauchen, weil der Client Container nicht lokal kennt, wenn sie geschlossen sind.
- Better Shulker Boxes (kostenpflichtig, kein Vorbild für Code): Interaktion aus dem Inventar – nein.

## Ist-Zustand im Repo
- `modules/simpleinterfaces` hat Container-Oberflächen (ContainersScreen, StorageStyles, Portable-Container `linked/`), aber keinen Item-Tooltip. Shulker/Endertruhe kommen dort schon in Portable-Menüs vor (Endertruhe kann tragbar geöffnet werden) -> gleiches Themengebiet.
- Vanilla zeigt für Shulker-Items bereits eine Textliste (die ersten Einträge, dann "+ n weitere") und für Bündel ein Raster. Ein Raster für Shulker und etwas für Endertruhe gibt es in Vanilla 26.x nicht (laut Recherche; vor Umsetzung gegen die 26.3-Quellen prüfen). Rückport-Regel: stellt Vanilla das später bereit, entfällt unser Teil.

## Feature-Tabelle
| Feature | Übernehmen | Grund |
|---|---|---|
| Shulker-Raster im Tooltip (Standard: sichtbar bei Shift, einstellbar immer) | ja | Kern des Wunsches, Daten stehen im Item (`container`-Komponente), rein Client |
| Gefärbte Shulker: Raster in Farbe der Kiste | ja | optisch, billig |
| Endertruhen-Tooltip mit eigenem Inhalt | ja | Wunsch, braucht Sync |
| Tooltip für andere Items mit `container` (Truhe-Item, Rucksäcke, Bundle) | später | gleiche Mechanik; Bündel hat Vanilla-Raster |
| Interaktion aus dem Tooltip (einfügen/entnehmen) | nein | Server-/Konfliktrisiko, nicht gewünscht |
| Verschachtelte Vorschau | nein | Größe/Lesbarkeit |

## Sync-Weg Endertruhe
Inhalt liegt serverseitig (`PlayerEnderChestContainer`). Client kennt ihn nur offen. Vorschlag: Server sendet kompakten Snapshot (27 Stacks, Vanilla-Item-Codec) an den Spieler: beim Login, beim Schließen der Endertruhe (und nach Änderungen über Portable-Menü/Invsee, falls vorhanden) – nie fortlaufend. Client cacht den Snapshot und zeigt ihn im Tooltip; ohne Server-Mod zeigt der Client nur "Inhalt unbekannt" bzw. nichts. Nur eigener Inhalt, kein Mitlesen fremder Truhen. Datenschutz/Anti-Cheat: nur Items, die der Spieler ohnehin besitzt.

## Zielmod
Empfehlung: **Simple Interfaces** (Oberflächen/Anzeige, Container-Themenkreis, QoL-Super-Mod bündelt es bereits). Begründung: Tooltip ist UI; Shulker-Teil rein Client (Fabric-API Tooltip-Komponenten/Mixin), Endertruhen-Teil ein kleines Payload. Allein spielbar: ohne Server-Teil bleibt Shulker-Vorschau nutzbar, Endertruhe still. Gemeinsame Raster-Zeichenfunktion nur nach simplelib, falls SB-Rucksäcke/andere dasselbe Raster nutzen (Stufe 3), sonst in Interfaces lassen. Alternative QoL: abgelehnt, QoL enthält schon Interfaces.

## Stufen
1. Shulker-Raster (Client), Config, Dark/Hell-Kontrast ≥4,5:1, Vanilla-Rahmenstil.
2. Endertruhen-Snapshot (Payload Server->Client, Login + Schließen), Tooltip.
3. Weitere Container-Items, ggf. Raster in simplelib.

## Config-Schalter
`tooltips.shulker` (an), `tooltips.shulkerMode` (shift/always), `tooltips.enderChest` (an, serverseitig abschaltbar), `tooltips.maxRows`, `tooltips.colorByDye` (an).

## Server/Client
Client: Rendern. Server: nur Endertruhen-Snapshot. Fehlt der Server-Teil -> Client zeigt nichts/Hinweis.

## Risiken
Tooltip-API in 26.3 (Komponenten-Tooltips statt Events) und Forge/NeoForge-Pfad; Spielerwechsel Dimension/Respawn (Snapshot neu); Großer Inhalt/Performance (cachen, nicht pro Frame berechnen); Textur-/Lesbarkeitsregel.

## Tests
Client-Smoke: Tooltip-Komponente erzeugt 27 Zellen für gefüllte Shulker; leere -> kein Raster. GameTest: Snapshot-Payload nach Schließen korrekt, nach Login korrekt, kein Fremdinhalt. Config aus -> Vanilla-Tooltip.

## Offene Fragen an den Besitzer
1. Zielmod Simple Interfaces? Empfehlung: ja.
2. Standard: nur bei Shift oder immer sichtbar? Empfehlung: Shift (Vanilla-Textliste bleibt sonst).
3. Endertruhen-Sync nur Login+Schließen (Empfehlung) oder zusätzlich live?
4. Auch Rucksack-/Truhen-Items später (Empfehlung: ja, Stufe 3)?
5. Darf der Server-Teil Pflicht sein (Empfehlung: nein, optional)?
