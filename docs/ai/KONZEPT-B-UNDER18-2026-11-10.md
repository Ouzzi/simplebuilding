# Konzept B-UNDER18 (10.11.2026) – Ein Bereich unter dem Grundgestein (NUR Konzept, nicht bauen)

Auftrag: /root/b-auftrag-20261109.md „N18 BACKROOMS AUSBAU“ Punkt 2 + „N18-Nachtrag“. Besitzer: „Ursprünglich war das Biom unter
dem Bedrock gedacht – jetzt soll UNTER DEM BEDROCK etwas ANDERES entstehen: ein mit Bedrock abgegrenzter Bereich, wie ein layered
biome nur unter dem Bedrock. Gerät man hinein, muss es einen Weg geben zu entkommen OHNE Bedrock zu zerstören.“ Nachtrag: „aber bitte
anderes Feeling geben, eher wie ein Rätsel“ → ruhig, geheimnisvoll, Entkommen über Rätsel/Mechanismen (kein Backrooms-Horror).
Vorschau mit Wahl: /root/sb-tools-b/previews/b-under18-preview.html, Antwortcode **UNDER18**. Gebaut wird erst nach der Wahl.

## Gemeinsame Leitlinien (alle Varianten)
- **Stimmung:** still, dunkel-bläulich, leuchtende Kristalle und Flechten als einzige Lichtquellen, ferne Tropfen und Kristallklänge
  (Vanilla: Amethyst, Tropfstein, Conduit), keine Monster, kein Schaden, keine Zeitnot. Gegenstück zu den Backrooms: dort Leere und
  Verirren, hier Entdecken und Verstehen.
- **Eintritt:** nie zufällig beim Graben – nur durch eine **seltene natürliche Grundgesteinslücke** (1×1 bis 2×2 Loch durch alle
  Bedrock-Lagen, sichtbar an aufsteigenden Partikeln und einem Ring aus fehlerhaftem Grundgestein, A14). Wer hineinspringt, fällt
  lang und landet weich (Wasserbecken). Config-Schalter + Seltenheit.
- **Ausgang ohne Bedrock-Abbau:** immer ein **Mechanismus**, der einen vorhandenen, versiegelten Weg öffnet (Aufzugsstrom, Schacht,
  Tor). Das Grundgestein bleibt unzerstörbar; ein Notausgang (z. B. Tod/Bett) bleibt Vanilla.
- **Rätsel-Regeln:** lösbar ohne Wiki, Hinweise im Raum (Wandbilder aus Blöcken, Klänge, Licht), sofortiges Feedback je Versuch
  (Aktionsleiste/Klang wie beim Ventil-Rätsel der Backrooms), kein Strafen, beliebig oft versuchbar; Lösung je Welt aus dem Seed.
- **Belohnung:** Kristalle/Deko-Blöcke der Schicht, ein Fund pro gelöstem Rätsel (keine Überbelohnung).

## Variante A – Oberwelt nach unten erweitern (min_y-Erweiterung)
**Prinzip:** `dimension_type minecraft:overworld` bekommt `min_y −128` (statt −64), der Bedrock-Boden bleibt bei −64; darunter ein
eigener Schichtbereich (−128…−65), ringsum und unten Grundgestein.
**Rätsel-Prinzip „Lichtleiter“:** Uralte Lichtleitungen aus Kristall: Spiegelkristalle drehen (Rechtsklick), bis ein Lichtstrahl von
einer Quelle über mehrere Spiegel einen Empfänger trifft; der öffnet eine Siegelplatte über einem vorhandenen Schacht durch den
Bedrock-Boden. Hinweise: Wandbilder zeigen den Strahlengang als Muster.
**Inhalte:** Kristallhöhlen, eine Ruinen-Ebene, stille Seen.
**Risiken (siehe KONZEPT-A-BIOMES09 §2.2 Variante A):** Bestandswelten haben unter −64 keine Sektionen → **Leere-Löcher**, durch die
man fällt (Vanilla brauchte 1.18 eine eigene „below zero“-Nachgenerierung); fest verdrahtete −64 in Erz-, Höhlen- und Leere-Logik
anderer Mods (auch Simple Ores `y −64…−54`), Datapacks mit eigenem Overworld-Typ überschreiben/werden überschrieben; Weltgrenze der
Hauptwelt geändert = nicht abschaltbar ohne Weltschaden.
**A12:** Nutzen hoch (direkt unter der eigenen Welt), Eingriff **sehr stark** (Weltdefinition der Overworld), nicht sauber abschaltbar.
**Aufwand L, Risiko sehr hoch → nicht empfohlen.**

## Variante B – eigene Dimension „Grundgewölbe“ (eine weite Höhlenschicht)
**Prinzip:** Neue Dimension (`min_y 0`, Höhe 128, kein Himmel, Decke), Boden und Decke Grundgestein, seitlich endlos: eine weite,
ruhige Höhlenwelt aus Tiefenschiefer, Kristallgrotten (eigener leuchtender Kristall), Ruinen einer älteren Baukultur, unterirdische
Seen. Eintritt durch die Bedrock-Lücke (gleiche x/z), Ankunft im Wasser.
**Rätsel-Prinzip „Siegelteile sammeln“:** In jeder Region (z. B. 256×256) steht ein **Aufstiegsaltar** mit drei Fassungen; drei
**Siegelfragmente** liegen in nahen Ruinen, jede hinter einem kleinen Rätsel: (1) Hebelmuster nach einem Wandbild, (2) Druckplatten-Pfad,
den Leuchtflechten an der Decke andeuten, (3) Klangfolge: Kristalle in der Reihenfolge anschlagen, die ein Fries zeigt. Alle drei
eingesetzt → ein **Aufstiegsstrom** (Schwebe-Säule) öffnet sich durch eine vorhandene Lücke in der Decke und trägt dich an die
Oberfläche (sichere Stelle über dem Eintritt).
**Inhalte:** Höhlen mit Kristallen (Licht), Ruinen (Truhen mit kleinen Funden, Wandbilder), Seen mit Leuchttintenfischen (Vanilla,
friedlich), keine feindlichen Mobs.
**A12:** Nutzen: Erkundung + Rätsel + Deko-Blöcke; Eingriff: nur eine seltene Lücke im Overworld-Bedrock (abschaltbar), alles andere
in eigener Dimension; Bestandswelten heil. **Aufwand M–L, Risiko niedrig.**

## Variante C – eigene Dimension „Tiefenschichten“ (gestapelte Schichten, je ein Schleusen-Rätsel) – **Empfehlung**
**Prinzip:** wie B eine eigene Dimension (Höhe 192), aber **drei waagrechte Schichten übereinander**, jede ein eigenes „Biom“ und durch
Grundgestein von der nächsten getrennt – wörtlich „wie ein layered biome“. Man fällt durch die Bedrock-Lücke in die **unterste**
Schicht und arbeitet sich nach oben; die oberste Schleuse führt durch eine vorhandene Lücke zurück in die Oberwelt.
- **Schicht 1 „Kristallgrotte“ (unten):** leuchtende Kristalle, Wasser. **Rätsel Klangfolge:** vier Klangkristalle anschlagen in der
  Reihenfolge, die ein Wandfries aus vier Farben zeigt; jeder Anschlag klingt, ein falscher setzt ruhig zurück. Lösung öffnet die
  Schleuse (Leiterschacht durch die Bedrock-Zwischenlage).
- **Schicht 2 „Ruinenhalle“ (Mitte):** stille Säulenhallen, Wandbilder. **Rätsel Schalter-/Lampenmuster:** sechs Hebel steuern über
  sichtbare, alte Redstone-Leitungen sechs Lampen; Ziel ist das Lampenmuster des großen Wandbilds (Hebel schalten teils mehrere Lampen –
  Lichtlogik zum Durchschauen). Die Lampen zeigen jeden Versuch sofort.
- **Schicht 3 „Spiegelsee“ (oben):** dunkler See, Lichtschächte. **Rätsel Licht:** drei Schleusen öffnen/schließen den Wasserstand,
  bis ein Lichtstrahl über die Wasseroberfläche den Empfänger am Ufer trifft; dazu **Schlüsselteile**: zwei Ventilräder liegen in den
  Nebenhöhlen. Gelöst → Aufstiegsstrom in die Oberwelt.
**Inhalte:** je Schicht eigene Blöcke (Kristall, Ruinenstein mit Glyphen, Seegrund), kleine Funde je gelöstem Rätsel, keine Monster.
**Fortschritt:** klar sichtbar (unten → oben), Dauer etwa 20–40 Minuten, jede Schicht für sich ein Erfolgserlebnis; Schichten
deterministisch aus dem Seed (wie die Backrooms-Generatoren, reine Funktionen, testbar).
**A12:** Nutzen: geführtes Rätsel-Abenteuer, Deko-Blöcke, Belohnung je Schicht; Eingriff: wie B nur die seltene Bedrock-Lücke
(abschaltbar), sonst eigene Dimension; kein Schaden, kein Zwang (Eintritt nur durch Hineinspringen). **Aufwand L, Risiko niedrig.**
**Bau in Stufen:** Stufe 1 = Dimension + Lücke + Schicht 1 + Ausgang (direkt nach oben), Stufe 2 = Schicht 2, Stufe 3 = Schicht 3;
jede Stufe einzeln abnehmbar.

## Empfehlung
**Variante C** (eigene Dimension, gestapelte Schichten): trifft „layered biome unter dem Bedrock“ wörtlich, gibt mit drei
verschiedenen Rätselarten (Klang, Schalter-/Lichtlogik, Licht + Schlüsselteile) das gewünschte Rätsel-Gefühl mit klarem Fortschritt,
lässt Bestandswelten und andere Mods unberührt und ist stufenweise baubar. B ist die kleinere Alternative (eine Schicht, ein
Sammel-Rätsel). A nicht wegen des Weltrisikos.

## Offene Besitzer-Fragen (in der Vorschau wählbar)
1. Variante A / B / C.
2. Eintritt: nur Bedrock-Lücke (Standard) oder zusätzlich ein Weg aus den Backrooms (z. B. ein Riss in den Poolrooms)?
3. Name: „Tiefenschichten“ / „Grundgewölbe“ / eigener Vorschlag.
