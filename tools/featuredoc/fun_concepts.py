"""Zehn neue Fun-Konzepte (W3, Besitzer 09.11.2026). Reiner Text, kein Spielcode."""

CONCEPTS = [
    dict(n=1, de='Konfetti-Knaller', en='Party Popper',
         text='Ein Wegwerf-Item aus Papier, Schnur und einem Farbstoff. Rechtsklick lässt Konfetti in der Farbe des Farbstoffs aus Partikeln aufsteigen und spielt ein kurzes Knallen; das Item ist danach verbraucht. '
              'Es ändert nichts am Spiel, es feiert nur. Die Teilchenzahl ist begrenzt (höchstens 40 je Knall, Abklingzeit 3 s), die Farbe kommt aus dem Farbstoff.',
         a12='Nutzen: ein leises, sozial sichtbares „Geschafft!“ für Mehrspieler (Bosskampf, Bauabnahme) statt Chatnachricht. Ändert nichts am Spielgeschehen. Verbraucht nur Papier, Schnur und Farbstoff.',
         eingriff='keiner (reine Kosmetik, eigenes Item)', aufwand='S',
         abgrenzung='Simple Visuals zeigt Immersionspartikel der Umgebung, nicht auslösbare Feste; Simple Challenges feiert nichts. Neu.'),
    dict(n=2, de='Würfel und Münze', en='Dice and Coin',
         text='Zwei kleine Items: der Würfel (sechsseitig, Rechtsklick würfelt) und die Münze (Kopf oder Zahl). Das Ergebnis erscheint als Zahl über dem Kopf des Werfenden und als Chatzeile für alle im Umkreis von 16 Blöcken; Schleichen würfelt zwei Würfel. '
              'Der Zufall kommt vom Server, daher fälschungssicher; der Würfel kann ein Mal pro Sekunde benutzt werden. Rezept: Würfel aus Knochenmehl und schwarzem Farbstoff, Münze aus einem Kupferbarren.',
         a12='Nutzen: Brettspiele, Verlosungen und Entscheidungen auf dem Server ohne Befehl und ohne fremden Mod. Keine Wirkung auf Spielwerte. Verbraucht ein wenig Knochenmehl oder Kupfer.',
         eingriff='keiner', aufwand='S',
         abgrenzung='Die Verzauberung „Spiegel“ in Simple Mobs würfelt nur intern, ein Spielzeug ist das nicht; Simple Money hat keine Zufallsitems. Neu.'),
    dict(n=3, de='Abklatschen', en='High Five',
         text='Wer mit leerer Hand und Schleichen einen anderen Spieler anklickt, hebt die Hand; klickt der andere innerhalb von drei Sekunden zurück, gibt es einen Abklatsch mit hellem Klatschen und Funkenpartikeln bei beiden. '
              'Ein Zähler pro Spielerpaar ist nur im Chat sichtbar, wenn beide es erlauben. Ohne Gegenklick passiert nichts außer einer erhobenen Hand-Partikelspur.',
         a12='Nutzen: Begrüßung und Lob im Mehrspielerbetrieb, ohne den Chat zu füllen. Ändert nichts am Spielgeschehen (kein Schaden, keine Gabe). Der Klick eines Spielers auf einen Spieler mit leerer Hand bewirkt in Vanilla sonst nichts.',
         eingriff='sehr gering (nur leere Hand + Schleichen)', aufwand='S',
         abgrenzung='Simple Mob Manners und Mobheads behandeln Mobs, nicht Spielergesten; die Kopf-Grußfähigkeiten (Mobheads, „Greeting“) sind Kopf-Effekte. Neu.'),
    dict(n=4, de='Streicheln', en='Pet the Pet',
         text='Rechtsklick mit leerer Hand und Schleichen auf ein gezähmtes Tier (Wolf, Katze, Papagei, Pferd) lässt Herzchen aufsteigen, spielt den passenden Wohlfühlton und lässt den Wolf mit dem Schwanz wedeln. '
              'Je Tier gibt es nur alle zehn Sekunden eine Reaktion, und nur für den Besitzer. Eine Zähler-Statistik („Streicheleinheiten“) wird in den Spielerstatistiken geführt.',
         a12='Nutzen: Rückmeldung für gezähmte Tiere, die Vanilla nur mit Sitzen/Füttern kennt; Bindung an das Tier ohne neue Spielwerte. Ändert nichts am Spielgeschehen, der Besitzer-Klick mit Schleichen löst in Vanilla nichts aus (Sitzen läuft über normalen Rechtsklick).',
         eingriff='gering (nur Besitzer-Klick mit Schleichen)', aufwand='S',
         abgrenzung='Simple Animals bringt wilde Tiere, Simple Mobs und Mob Manners ändern Zucht/Stummschaltung. Streicheln gibt es dort nicht. Neu.'),
    dict(n=5, de='Steinehüpfen', en='Skip Stones',
         text='Flache Steine (Kopfstein, Tuff, Andesit, Schiefer) werden mit Rechtsklick flach über eine Wasserfläche geworfen und hüpfen bis zu fünfmal über die Oberfläche, je Hüpfer mit Platsch, kleinen Ringwellen und Zählung als Titelzeile („3 Hüpfer“). '
              'Das Wurfgeschoss verschwindet im Wasser, der Stein gehört danach nicht mehr dem Spieler. Rekorde pro Spieler werden in einer Statistik geführt; kein Block wird verändert.',
         a12='Nutzen: eine Beschäftigung am Wasser für Wartezeiten (Angeln, Boot) mit Erfolgsgefühl. Ändert nichts am Spielgeschehen. Verbraucht gewöhnliche Steine; der Wurf gegen Mobs richtet keinen Schaden an.',
         eingriff='keiner (eigenes Wurf-Item aus Vanilla-Steinen, abschaltbar)', aufwand='M',
         abgrenzung='Wurfziegel (Simple Fun) werfen Ziegel mit Schaden und Glasbruch; hier gibt es keinen Schaden, nur Hüpfer auf Wasser. Neu.'),
    dict(n=6, de='Wasserbombe', en='Water Balloon',
         text='Ein Wurf-Item aus Lederstück, Wasserflasche und Schnur. Beim Aufprall löscht es Feuer in einem Block Umkreis, macht Spieler im Treffer nass (Wassertropfen-Partikel, kurzer Nässeton, kein Schaden) und füllt einen getroffenen Kessel um eine Stufe. '
              'Es kann nur auf Spieler oder Block treffen, nicht auf Mobs, und hat 5 s Abklingzeit. In der Nether-Dimension verdampft es zu Dampfwolken ohne Wirkung.',
         a12='Nutzen: ein schneller, erreichbarer Feuerlöscher (Brandherde, brennende Mitspieler) und ein Wasserkessel-Füller ohne Eimer. Ändert nur Feuer und Kesselfüllung in einem Block Umkreis. Verbraucht eine Wasserflasche und Leder.',
         eingriff='gering (löscht Feuer, füllt Kessel, nur auf Block/Spieler wirksam)', aufwand='M',
         abgrenzung='Simple Crucibles hat Eimer-/Schmelzflüssigkeiten, keine Wurfsachen; Fun hat keinen Wasserwurf. Brick-Snowball ist ein Schadens-Geschoss. Neu.'),
    dict(n=7, de='Seifenblasen', en='Bubble Wand',
         text='Ein Stab aus Stock, Schnur und Wasserflasche, der bei gehaltenem Rechtsklick große, langsam steigende Blasen erzeugt. Eine Blase steigt bis zu 8 Blöcke und zerplatzt mit Klick-Ton, wenn sie getroffen wird oder eine Decke berührt. '
              'Die Blasen sind reine Partikel und blockieren nichts, tragen niemanden und haben eine Obergrenze von 12 Blasen je Spieler.',
         a12='Nutzen: Spielzeug für Gemeinschaftsplätze und Bauwerke (Brunnen, Spielplatz), ohne den Spielstand zu ändern. Ändert nichts am Spielgeschehen. Verbraucht nur Haltbarkeit des Stabes (64 Nutzungen).',
         eingriff='keiner', aufwand='S',
         abgrenzung='Honigblasen (Simple Fun) sind ein Trinkeffekt, keine Spielzeugaktion; die Wasser-Immersionsblasen in Simple Visuals sind Umgebung. Neu.'),
    dict(n=8, de='Papierflieger', en='Paper Plane',
         text='Aus einem Blatt Papier gefaltet (Rezept: Papier). Beim Werfen segelt es mit sanftem Auftrieb mehrere Dutzend Blöcke weit, ohne Schaden, und landet als Item wieder; wurde vorher ein Buch-und-Feder-Zettel hineingefaltet, wird dessen Text beim Aufheben angezeigt. '
              'Die Flugstrecke hängt vom Blickwinkel ab, höchste Reichweite bei etwa 20 Grad Aufwärtswurf. Über Wasser landet es schwimmend.',
         a12='Nutzen: eine Nachrichtenübergabe auf Distanz (über Schluchten, Burgmauern), die Vanilla nur mit Trichtern oder Loren kennt. Ändert nur das Item-Flugverhalten. Verbraucht ein Papier; Reichweite und Lebensdauer sind begrenzt (60 s).',
         eingriff='gering (neues Wurf-Item, Server begrenzt Reichweite)', aufwand='M',
         abgrenzung='Simple Maps liefert Karten, Simple Music Notenblätter; kein Nachrichten-Wurf. Schleichwurf (Yeet) ist nur ein weiter Wurf beliebiger Items. Neu.'),
    dict(n=9, de='Quietscheente', en='Rubber Duck',
         text='Ein dekoratives Item, das auf der Wasseroberfläche schwimmt und beim Anklicken quietscht, mit kleiner Hüpfanimation. Auf festem Boden quietscht es ebenfalls nur beim Anklicken, nicht bei bloßer Berührung. '
              'Es zählt als Dekoration (kein Inventarfüller), Rezept: Gelber Farbstoff, Slimeball, Gold-Nugget. Eine seltene Variante („Piraten-Ente“) entsteht, wenn man sie auf Seelensand platziert.',
         a12='Nutzen: Bade- und Teichdeko mit Ton, auch als Signal im Mehrspielerbetrieb („Ente ruft“). Ändert nichts am Spielgeschehen. Verbraucht Slimeball und Gold-Nugget.',
         eingriff='keiner', aufwand='M',
         abgrenzung='Es gibt Dekoköpfe in Mobheads, aber keine Wasser-Dekotiere; Quietschen als Klickgeräusch ist neu. Mobheads-Köpfe sind Kopf-Gegenstände, keine Schwimmer. Neu.'),
    dict(n=10, de='Schneeengel', en='Snow Angel',
         text='Wer sich auf einer Schneeschicht oder Schneeblock ablegt (Schleichen, 3 s stillhalten, dann springen), hinterlässt einen Engelsabdruck als kurzlebige Partikelwolke in Engelsform und hört ein weiches Knirschen. '
              'Der Abdruck dauert 30 s und verändert keinen Block; im Schneesturm ist die Wolke größer. Eine Statistik zählt Schneeengel pro Spieler.',
         a12='Nutzen: eine Winter-Beschäftigung in Schneebiomen, die den Rhythmus vom Abbauen und Kämpfen bricht. Ändert nichts am Spielgeschehen. Nur Partikel, keine Blöcke; ohne Schnee passiert nichts.',
         eingriff='keiner (nur Partikel und Ton)', aufwand='S',
         abgrenzung='Simple Movement lässt auf Pulverschnee mit Frostläufer laufen, Simple Blocks hat Schnee-Logging; ein Abdruck oder Schneewesen fehlt. Neu.'),
]
