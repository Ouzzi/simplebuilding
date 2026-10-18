# Konzept: Better Item Frames (Ideen übernehmen)

Stand 19.10.2026, B (b-concepts19). Nur Konzept, kein Code. Freigabe durch den Besitzer offen.

## Ziel
Item-Rahmen im Überleben komfortabel machen: unsichtbar schaltbar, Inhalt/Drehung sperrbar, Drehung in Gegenrichtung. Vanilla-nah, ohne neue Items, wenn es auch mit Vanilla-Material geht.

## Vorbilder (öffentliche Beschreibungen, nur Ideen, kein Code; Lizenzen unberücksichtigt, da nichts kopiert wird)
- KawaMood "Better Item Frames" (Datapack): Shift + Rechtsklick mit Schere macht den Rahmen unsichtbar/sichtbar (Schere nimmt Haltbarkeit, Unbreaking zählt), Rotation sperrbar (Rahmen neu setzen löst).
- Fabric/NeoForge-Mod "Better Item Frames": Wurftrank der Unsichtbarkeit macht Leucht-Rahmen unsichtbar, Wasserflasche zurück; unsichtbar erst, wenn ein Item drin ist.
- "Better invisible item frames": Shift+Rechtsklick, durch unsichtbare Rahmen hindurch mit Blöcken interagieren.
- Rahmen-Pack: Shift+Klick dreht das Item rückwärts.

## Ist-Zustand im Repo
- Kein Rahmen-Feature außer `modules/simplemaps` (ItemFrameMixin/Accessor/Renderer: Karte im Rahmen öffnen). Muss kompatibel bleiben (Karte im Rahmen darf nicht kaputtgehen).
- Vanilla 26.x: Rahmen können per Item unsichtbar gemacht werden? Nein (nur per Befehl/Datapack). Nach Rückport-Regel also kein Doppelbau; vor Umsetzung Vanilla-Version prüfen.

## Feature-Tabelle
| Feature | Übernehmen | Grund |
|---|---|---|
| Schere + Shift-Rechtsklick: unsichtbar/sichtbar | ja | Vanilla-Material, kein neues Item |
| Unsichtbar nur mit Item im Rahmen | ja | leerer unsichtbarer Rahmen wäre nicht auffindbar |
| Sperren (Item nicht drehbar/entnehmbar) per Schere + Eisenbarren o. Ä. | ja, Stufe 2 | Schutz vor Fehlklicks; Zeichen im Tooltip |
| Rückwärts drehen mit Shift+Klick | ja | trivial, kein Konflikt |
| Interaktion durch unsichtbare Rahmen auf den Block dahinter | später | Mixin-Risiko, Karten-Klick der Simple Maps darf nicht brechen |
| Wurftrank der Unsichtbarkeit | nein | Verbrauch teuer, Schere reicht |
| Eigenes Rezept "unsichtbarer Rahmen" | nein | Ableitbar aus Vanilla; kein neues Item |

## Zielmod
Empfehlung: **Simple Quality of Life** (QoL, Super-Mod; Server-Verhalten plus kleiner Client-Teil). Begründung: reines Komfortverhalten, kein eigener Block/Item, kein Bezug zu SB-Bau. Als eigenständiges Mixin im QoL (`ItemFrame.interact`), Zustand in Vanilla-Daten (Entity-`Invisible`-Flag, Sperre als Entity-Tag/Custom-Data), damit nichts Mod-spezifisches in der Welt hängt. Alternative: Simple Maps (nur weil dort schon ItemFrame-Mixins liegen) – abgelehnt, da Maps allein spielbar bleiben und kein Rahmen-Feature tragen soll. Nichts in simplelib nötig.

## Stufen
1. Schere+Shift-Rechtsklick: unsichtbar/sichtbar, Haltbarkeit, Aktionsleiste-Hinweis; Shift+Klick rückwärts drehen.
2. Sperren (Item fest, nicht drehbar), Tooltip/Hinweis beim Anvisieren.
3. Optional: Durchklicken durch unsichtbare Rahmen.

## Config-Schalter (Datei-Config + Cloth-Screen wie andere QoL-Features)
`itemFrames.invisibleWithShears` (an), `itemFrames.lock` (an), `itemFrames.reverseRotate` (an), `itemFrames.shearsDamage` (1), `itemFrames.clickThrough` (aus).

## Server/Client
Alles serverseitig (Interaktion, Flag). Client braucht nichts, nur optionaler Hinweistext (Sprachdateien). Dedicated-Server ohne Client-Mod funktioniert.

## Risiken
- Konflikt mit Simple Maps (Rechtsklick auf Karte im Rahmen): Shift-Klick-Belegung abstimmen, Shift = Rahmenaktion, normaler Klick = Karte.
- Claims/Schutz: Rahmenänderung muss Claim-Regeln der Simple Tweaks respektieren (nur, wenn Tweaks da ist; über Vanilla-Events, nicht Import).
- Bedrock/Vanilla-Clients sehen unsichtbare Rahmen korrekt, da Vanilla-Flag.

## Tests
GameTest: Rahmen mit Item + Schere -> invisible=true, Schere -1 Haltbarkeit; ohne Item kein Wechsel; gesperrt -> Rotation bleibt; Karte im Rahmen öffnet weiter die Karte (Maps-Test bleibt grün); Config aus -> Vanilla-Verhalten. Kein Gradle in diesem Konzeptlauf.

## Offene Fragen an den Besitzer
1. Zielmod QoL? Empfehlung: ja.
2. Werkzeug zum Unsichtbarmachen: Schere (Empfehlung) oder Trank?
3. Sperren: braucht es das (Empfehlung: ja, Stufe 2, per Schere + Shift auf bereits unsichtbaren Rahmen)?
4. Durchklick-Funktion in Stufe 3 (Empfehlung: später, nur auf Wunsch)?
5. Gilt es auch für Leucht-Rahmen (Empfehlung: ja, gleiche Klasse)?
