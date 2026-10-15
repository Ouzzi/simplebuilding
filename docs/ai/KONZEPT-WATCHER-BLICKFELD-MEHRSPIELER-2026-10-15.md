# Konzept: Hollow Watcher, Blickfeld-Zonen und Mehrspieler (Nachtrag 63)

Umgesetzt auf `claude-watcher2`. Server-Autorität: alle Entscheidungen fallen auf dem Server; der Client meldet nur seinen FOV.

## Blickfeld-Zonen (statt Creaking-Kegel)
Winkel = kleinster Winkel zwischen der Blickrichtung des Spielers und Kopf/Brust/Hüfte des Watchers (Sichtlinie nötig, Blindheit/Spectator zählen nicht). Halbwinkel bei FOV 70, skaliert mit `FOV/70` (Client-FOV, geklemmt 30..110, ohne Meldung 70). Config `simplemobs-server.json`: `watcherGazeStare` 10, `watcherGazeSlow` 25, `watcherGazeEdge` 35 (geklemmt und aufsteigend geordnet).

| Zone | Winkel | Verhalten |
|---|---|---|
| STARE | bis 10° | steht still (friert), startet keinen Angriff |
| SLOW | bis 25° | läuft langsam (0,45) auf einem Bogen um den Spieler Richtung Blickfeldrand, nie der kürzeste Weg |
| EDGE | bis 35° | läuft mit voller Geschwindigkeit aus dem Blickfeld, mit 12° Puffer hinter den Rand |
| FREE | darüber | pirscht: läuft hinter den Spieler (Punkt 2,5 Blöcke hinter ihm), dann Griff/Kralle |

Das Fernglas friert weiter 3 s ein. Teleport hinter das Opfer (Ankündigung) bleibt für Distanz > 8, nicht im Boot.

## Mehrspieler
- Aggro: Zielwahl bleibt der nächste betroffene Spieler; die Aura und der Schrei wirken auf alle in Reichweite.
- Freeze: jeder Spieler wird geprüft, die niedrigste Zone gewinnt. Sieht irgendein Spieler ihn an (STARE), steht er für alle still. In SLOW/EDGE weicht er dem Spieler aus, der ihn am genauesten ansieht (`looker`).
- Treffer: 1 s später Teleport, zufällig entweder hinter ein neues Opfer oder 12 bis 20 Blöcke weg. Opferwahl: zufällig unter den Betroffenen im Umkreis 48, gewichtet `1/(1+d/16)` (Näherer wahrscheinlicher), das aktuelle Ziel wird übersprungen, wenn es andere gibt.

## Boot
Der Watcher kann im Boot sitzen (Spieler setzt ihn per /ride o. ä.); er greift Spieler in Reichweite weiter an. Der Pirsch-Teleport ist im Boot aus; ein Treffer löst ihn aus dem Boot und teleportiert ihn. Das Diktat nannte "Hollow Howler", gemeint ist der Watcher (der Howler hat keine Blickfeld-Logik).

## Client
Hinter dem lokalen Spieler (unter 4,5 Blöcke, außerhalb des Blicks): beide Arme nach vorn über die Schultern (weich über Nähe und Winkel eingeblendet), Hände als skalierte Teilquader (1,7x, vorhandene Arm-Textur, keine Texturänderung). Ruhe-Übergang (Zittern) wird weich ausgeblendet statt auf 0 zu springen. Shivering V: Zielwackeln (AimSway) 4,2° statt 3,0°.

## Nicht getestet
Client-Sicht (Optik der Arme über den Schultern) und echte Mehrspieler-Netzwerkstrecke (FOV-Paket) nur per Compile und Logik-Tests.
