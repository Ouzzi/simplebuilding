# Konzept A-DINO36: die übrigen vier Dinosaurier (DINO-REST)

Stand 12.11.2026, Agent a-dino36, Branch `a-dino36` (Basis `hub/claude-wave1` 240d278d5). **Nur Konzept, kein Code.** Nur MC 26.3 Fabric.
Auftrag: Besitzer 12.11. ~12:15 (N36-4, `/root/sb-tools-a/abnahme-a/antworten.md`): „T. rex passt; vor dem Bau der übrigen Dinos 5 Runden Selbstabnahme
(prüfen, verbessern …), jeder Dino mit eigenem Vorteil und einzigartig, passende weitere Items hinzufügen.“
Vorschau mit Wahl je Dino: `/root/sb-tools-a/previews/a-dino36-preview.html` (Artifact https://claude.ai/artifact/HE2Esf5ZgM9DMGXBtHcySY), Antwortcode **DINO36**. Generator der Vorschau: `/root/sb-tools-a/gen_dino36_preview.py`.

## 0. Ergebnis nach Runde 5 (Kurzfassung)

| Dino | Bereich | Einzigartiger Vorteil | Sprungtaste | Neue Items | Vanilla-Vorbild |
|---|---|---|---|---|---|
| T. rex (gebaut, abgenommen) | Kampf | einziger Dino, der kämpft: beißt Monster, die Besitzer oder ihn angreifen | springen | Großer Sattel, T.-rex-Rüstung (vorhanden) | Wolf, Eisengolem |
| **Triceratops** „Baumschüttler“ | Forst / Ressource | Anlauf mit den Hörnern gegen einen Stamm schüttelt den Baum: Äpfel, Setzlinge, Stöcke (Kakao) fallen, der Baum bleibt stehen | halten = Anlauf aufladen | Rüstung ×3, Eierschale | Kamel-Sprint, Ziegen-Rammstoß, Blatt-Loot |
| **Stegosaurus** „Pflugtier“ | Arbeit / Acker | zieht den Dino-Pflug: pflügt beim Gehen 3 Blöcke breit und sät aus der Nebenhand | Pflug senken/heben | **Dino-Pflug**, Rüstung ×3, Eierschale | Hacke (26.3-Transformer `minecraft:hoe`) |
| **Utahraptor** „Kletterer“ | Reiten / Gelände | klettert mit Reiter senkrechte Wände hoch (Ausdauer ≈ 12 Blöcke), fällt weich | springen | **Raptorfeder**, Rüstung ×3, Eierschale | Spinne (Klettern), Gürteltier (Bürsten) |
| **Parasaurolophus** „Leittier“ | Transport / Tiere | Kamm-Ruf: Nutztiere in 24 Blöcken folgen ihm 2 min, auch durchs Wasser; schwimmt mit Reiter | Ruf | Rüstung ×3, Eierschale | Locken mit Futter, Glocke |

Kein Dino kann, was ein anderer kann; keiner verdoppelt ein Vanilla-Reittier (Matrix §5). Kampf bleibt beim T. rex.

## 1. Ist-Stand und Leitplanken

**Code (`modules/simpledinos`, gelesen):**
- `DinoKind`: 5 Sorten mit Ei-Gewichten R10 (T. rex 10, Triceratops 25, Stegosaurus 25, Utahraptor 20, Parasaurolophus 20). Alle fünf Eier sind registriert, haben Textur und brüten; schlüpfen kann nur der T. rex (`entity()` sonst `null`), Fossilien würfeln nur unter schlüpffähigen Sorten (`FossilBlockEntity.kind()` würfelt lazy aus der Position).
- `Dino extends AbstractHorse`: schlüpft gezähmt (Prägung = längster Sitzer), wächst 25 → 100 % in `growMinutes` (40), kein Züchten, Fell `coat` nach Aufzucht-Biom (`DinoVariants`: olive/sand/rust/moss/ash, ABN30-1), Futter Tag `meat`, Ravager-Töne. **Kampf-Ziele (MeleeAttack + „Monster, die Besitzer/Dino angreifen“) liegen heute in `Dino`, also für alle Sorten.**
- `Trex`: 90 LP, Biss 10 (Config), Tempo 0,22, Sprung 0,45, Stufe 1,5, Hitbox 3,6 × 3,5. Großer Sattel (Sattel + 4 Leder + 4 Eisen), Rüstung Leder/Eisen/Diamant (8 Material um 1 Knochenblock, Pferderüstungswerte 3/5/11), Anzeige als Texturvariante.
- Modelle/Texturen aus `tools/gen_models.py` (Würfel-Spezifikation → Textur + `*Layers.java`), Eier/Fossil aus `tools/gen_blocks.py`; Config `simpledinos-server.json`.
- Nicht gebaut (offen aus Stufe 1): T.-rex-Brüllen und -Ausdauer-Sprint, Dino-Sounds (R14), Vanilla-Fossilstruktur-Anteil (R7a), max. zahme Dinos je Spieler.

**Besitzer-Entscheide, die gelten:** ABN29b-4 (alle fünf Arten), R2/R3 (Utahraptor), R4 (Kern-Hitbox), R5, **R6 (nie wild, nur Schlüpfen)**, R7, R8 (3 Rüstungsstufen), R9 (1 Reiter), R10 (Ei-Gewichte), R14 (ElevenLabs mit Hörprobe), ABN30-1 (Felle je Biom), RELSB31 f4 (Simple Dinos erst mit allen fünf veröffentlichen), DINO28-1 („je Dino eigene Rüstung“, „gute KI, aber leichtgewichtig“).

**Prinzipien:** A1 (allein spielbar, nur Tags/IDs zu Partnern), A6 (zwei Hinweise je Weg), A8c/A8d (Texturen in Runden, Vanilla-Analyse), A9 (Server-Config mit Grenzen), **A12 (Vanilla-Nutzen/Änderung/Eingriff je Teil)**, A14 (Wirkungen sichtbar), Modul-Regel „Dinos schaden Spielern nie, zerstören keine Blöcke“.

Vanilla-26.3-Fakten (in `/root/mcsrc263np` geprüft): Kamel-Sprint (`Camel.DASH_COOLDOWN_TICKS` 55, Sprungtaste), Ziegen-Rammstoß, Gürteltier-Bürsten (`ARMADILLO_BRUSH`-Loot), Spinnen-Klettern (`onClimbable`, `WallClimberNavigation`), Blätter `PERSISTENT`, Hacken-Umwandlung als Datenregister `BlockTransformers.HOE`, Glocke lässt Plünderer leuchten, Tag `dismounts_underwater` (Pferd, Esel, Lama, Kamel … werfen unter Wasser ab), Eisenkette heißt `iron_chain`, Pfeil-Rezept nutzt `minecraft:feather` direkt.

## 2. Bewertungsraster

Fünf Kriterien, je 1–5 (5 = am besten; bei Aufwand 5 = wenig Aufwand), Summe aus 25:
**S** Spielspaß (aktiv, sichtbar, „Aha“) · **B** Balance (stark genug, nie Pflicht, kein Exploit) · **V** Vanilla-Nähe (Vanilla-Bausteine, A12 Nutzen bei geringem Eingriff) · **A** Aufwand (Modell, Logik, Risiko) · **E** Einzigartigkeit (gegen T. rex, untereinander, gegen Vanilla-Reittiere).

## 3. Fünf Runden

### Runde 1 – Ausgangslage (Konzept WP-18 §3.4)

| Dino | Vorteil laut WP-18 | S | B | V | A | E | Σ |
|---|---|---|---|---|---|---|---|
| Triceratops | Kiste 27 Plätze + Ansturm 3 s (5 Schaden, Rückstoß) | 3 | 3 | 3 | 3 | 1 | 13 |
| Stegosaurus | Dornen 2 Schaden + Knochenmehl alle 5 min | 2 | 4 | 3 | 4 | 2 | 15 |
| Utahraptor | Sprint 1,3 × Pferd, Sprung 2,5, jagt Hühner/Kaninchen | 4 | 2 | 2 | 3 | 2 | 13 |
| Parasaurolophus | Ruf: Monster in 32 Blöcken leuchten 6 s; schwimmt | 3 | 4 | 4 | 3 | 3 | 17 |

Befunde:
1. Ansturm (Tri), Dornen (Stego) und Jagd (Raptor) sind **Kampf** → doppelt zum T. rex. Dazu liegen die Kampf-Ziele im Code in `Dino`, also hätte jeder Dino sie.
2. Tri-Kiste = Esel/Maultier/Lama in größer; Raptor-Tempo 1,3 × Pferd entwertet das Pferd (A12: starker Eingriff). Raptor-Ausdauer-Sprint = geplanter T.-rex-Sprint.
3. Stego-Knochenmehl ist passiv und unsichtbar; der Komposter macht dasselbe.
4. Para-Leuchten ähnelt Glocke/Spektralpfeil, ist als Reittier-Fähigkeit aber eigenständig; Schwimmen ist eigenständig.

Verbesserung → jeder Dino bekommt einen **eigenen Vanilla-Bereich** (Kampf, Ressource, Acker, Gelände, Tiere); Schaden nur beim T. rex; kein „mehr vom Vanilla-Gleichen“.

### Runde 2 – eigene Bereiche

| Dino | Vorteil | S | B | V | A | E | Σ |
|---|---|---|---|---|---|---|---|
| Triceratops | Pflug, 3 Blöcke breit | 3 | 4 | 4 | 3 | 4 | 18 |
| Stegosaurus | Äsen: Blätter/Beerensträucher beim Reiten → Äpfel, Setzlinge, Beeren | 2 | 3 | 4 | 3 | 3 | 15 |
| Utahraptor | Klettern wie eine Spinne, unbegrenzt | 5 | 2 | 3 | 2 | 5 | 17 |
| Parasaurolophus | Herdenruf: alle Tiere in 16 Blöcken folgen | 4 | 3 | 4 | 3 | 5 | 19 |

Befunde:
1. Die Tri-Hörner bleiben ungenutzt; ein Pflug passt zu jedem schweren Tier.
2. Stego-Äsen ist passiv und kaum sichtbar; Äpfel sind entweder zu selten (kein Spaß) oder eine Farm-Lücke. Tri + Stego sind beide „Pflanzen“ → Doppel-Gefahr.
3. Raptor ohne Grenze: Mauern und Basen sind wertlos, 500 kg an der Wand ist schwer zu glauben, Technik-Risiko (Bewegung beim Reiten rechnet der Client).
4. Para: Welche Tiere? Im Mehrspieler lockt man die Kühe des Nachbarn weg.

Verbesserung → **Rollen tauschen**: Triceratops = Rammen (Hörner!) gegen Bäume (Forst-Ressource, aktiv, mit Kamel-Anlauf und Ziegen-Stoß als Vorbild), Stegosaurus (langsam, schwer, breit) = Pflug. Raptor: Ausdauer + Block-Tag + Realitäts-Anker (WAIR). Para: Auswahl über das Futter in der Hand (Vanilla-Locken) + Ausschlüsse.

### Runde 3 – Tausch und Grenzen

| Dino | Vorteil | S | B | V | A | E | Σ |
|---|---|---|---|---|---|---|---|
| Triceratops | Rammen → Baum schütteln | 5 | 3 | 4 | 3 | 5 | 20 |
| Stegosaurus | Pflug + Säen aus der Nebenhand | 4 | 4 | 4 | 3 | 5 | 20 |
| Utahraptor | Klettern, 4 s Ausdauer, Tag für nicht kletterbare Blöcke | 5 | 4 | 3 | 2 | 5 | 19 |
| Parasaurolophus | Ruf mit Handfutter-Auswahl, max. 24 Tiere, 120 s | 4 | 4 | 5 | 3 | 5 | 21 |

Befunde:
1. Tri-Anlauf, der Mobs trifft, wäre wieder Kampf. Ertrag und Ruhezeit fehlen; gesetzte Blätter (Blätterwand) wären eine Apfel-Farm.
2. Stego: Rechte (Spawnschutz, Claims), wie wird der Pflug bedient, Haltbarkeit?
3. Raptor: Wo sieht man die Ausdauer, was passiert bei leerer Ausdauer, Fallschaden?
4. Para: Woran erkennt man die Herde (A14), wie endet sie, was ist mit Wasser?

Verbesserung:
- Tri: Mobs nur Rückstoß, kein Schaden, Spieler nie; nur natürliche Blätter (`persistent=false`); 10 min Ruhe je Baum; Ertrag = Vanilla-Blatt-Loot für die Hälfte der Blätter + „Fallobst“-Wurf.
- Stego: Vanilla-Hackenregel (26.3-Datentransformer) + SimpleLib-Rechte-Probe; Sprungtaste senkt/hebt den Pflug (der Stegosaurus springt nicht); Pflug mit Haltbarkeit 750.
- Raptor: Ausdauer im Sprungbalken, bei leerer Ausdauer langsames Abrutschen statt Fallen, Fallschaden halbiert.
- Para: Noten-Partikel über jedem Folger, Doppelruf entlässt die Herde, schwimmt mit Reiter an der Oberfläche.

### Runde 4 – Items, A12, Verhalten

| Dino | Ergänzung | S | B | V | A | E | Σ |
|---|---|---|---|---|---|---|---|
| Triceratops | Rüstung (Stirnschild + Hornkappen), Eierschale | 5 | 4 | 4 | 3 | 5 | 21 |
| Stegosaurus | Dino-Pflug (aus 3 Eisenhacken), Rüstung, Eierschale | 4 | 4 | 5 | 3 | 5 | 21 |
| Utahraptor | Raptorfeder (Bürsten), Rüstung, Eierschale | 5 | 4 | 4 | 2 | 5 | 20 |
| Parasaurolophus | Rüstung (Kammhaube), Eierschale; kein Werkzeug nötig | 4 | 4 | 5 | 3 | 5 | 21 |

Geprüft und **verworfen**: Klettergeschirr (die Fähigkeit ist angeboren, ein Item wäre nur Gängelung), Leitglocke (der Kamm ist die Glocke), Hornkappen als eigenes Item (gehören in die Tri-Rüstung), Satteltaschen (= Esel/Lama), Knochenpfeife zum Herbeirufen (zu nah am Para-Ruf), Bernstein (Vanilla hat Harz).
Befunde: Mit dem T.-rex-Muster (8 Material um 1 Knochenblock) hätten alle Dino-Rüstungen einer Stufe dasselbe Rezept → **Eierschale je Art** als Mitte (Vanilla-Vorbild: Schildkröten-Hornschild → Schildkrötenpanzer; das Rezept erscheint erst nach dem Schlüpfen). Kampf-Ziele aus `Dino` nach `Trex` verschieben, die anderen bekommen nur eine artgerechte Abwehr (§4). Der geplante T.-rex-Ausdauer-Sprint wäre doppelt zum Tri-Anlauf → Vorschlag: streichen (Frage `trex`).

### Runde 5 – Aufwand, Technik, Schluss-Check

| Dino | Vereinfachung | S | B | V | A | E | Σ |
|---|---|---|---|---|---|---|---|
| Triceratops | Kamel-Anlauf-Logik, Vanilla-Blatt-Loot-Tabellen | 5 | 4 | 4 | 4 | 5 | 22 |
| Stegosaurus | 26.3-Hacken-Transformer (Daten), SimpleLib `InteractionGuard` | 4 | 4 | 5 | 4 | 5 | 22 |
| Utahraptor | Spinnen-Klettern (`onClimbable`), Pferde-Sprungbalken | 5 | 4 | 4 | 3 | 5 | 21 |
| Parasaurolophus | Vanilla-`isFood`, eigene Lenkung ohne Mixin | 4 | 4 | 5 | 4 | 5 | 22 |

Befunde: Aufwand sinkt durch Vanilla-Bausteine und ein gemeinsames Vierbeiner-Gerüst im Generator (Tri/Stego/Para). Restrisiko Raptor (Bewegung beim Reiten ist client-seitig) → als letzten bauen. Schluss-Check §5: keine Überschneidung. Beim Besitzer bleiben: Raptor-Klettern (Fantasie-Anteil), Fundort-Gewichtung, T.-rex-Sprint, Item-Liste.

**Verlauf Σ (von 25):** Triceratops 13 → 18 → 20 → 21 → 22 · Stegosaurus 15 → 15 → 20 → 21 → 22 · Utahraptor 13 → 17 → 19 → 20 → 21 · Parasaurolophus 17 → 19 → 21 → 21 → 22.

## 4. Die vier Dinos im Detail

Gemeinsam für alle vier (wie T. rex, abgenommen): nur aus dem Ei, gezähmt und geprägt beim Schlüpfen, Jungtier 25 → 100 % in 40 min (Config), Baby nicht reitbar und folgt dem Besitzer, Großer Sattel, Rüstung im Körperslot, Vanilla-Pferde-Bildschirm, kein Züchten, kein Despawn, greift nie Spieler an, zerstört keine Blöcke. Fähigkeiten nur erwachsen und nur mit Reiter (keine Leerlauf-Last). Werte aus WP-18 §3.4, Hitbox beim Bau am Modell prüfen (T. rex wich ebenfalls ab).

| | T. rex (Ist) | Triceratops | Stegosaurus | Utahraptor | Parasaurolophus |
|---|---|---|---|---|---|
| Länge real | 12 m | 8,5 m | 9 m | 5,5 m | 9,5 m |
| Hitbox B × H | 3,6 × 3,5 | 2,6 × 3,2 | 2,4 × 3,5 | 1,3 × 2,0 | 2,2 × 3,6 |
| Lebenspunkte | 90 | 100 | 90 | 40 | 60 |
| Tempo (Pferd 0,11–0,34) | 0,22 | 0,20 | 0,17 (pflügend 0,12) | 0,30 | 0,24, Wasser 0,6 × |
| Sprung / Stufe | 0,45 / 1,5 | – / 1,0 | – / 1,0 | 0,70 (≈ 3 Blöcke) / 1,5 | – / 1,0 |
| Rückstoßfest | 0,6 | 0,8 | 0,7 | 0,1 | 0,3 |
| Fallschaden-Faktor | 0,4 | 0,6 | 0,6 | 0,5 und +3 sichere Blöcke | 0,5 |
| Abwehr (nur wenn angegriffen, nie Spieler) | verteidigt Besitzer, Biss 10 | stößt Monster weg (Rückstoß, 2) | Stachelschwanz: Nahkampf-Angreifer 2 Schaden | flieht schnell | flieht, Warnruf (nur Ton) |
| Futter (Lieblingsfutter heilt mehr, −10 % Wachstum) | Fleisch | **Apfel**, Weizen, Heuballen, Karotte | **Farn/großer Farn**, Weizen, Heuballen, Beeren | Fleisch, **rohes Huhn/Kaninchen** | **Süßbeeren**, Weizen, Heuballen, Rote Bete |

### 4.1 Triceratops – „Baumschüttler“ (Forst / Ressource)

- **Vorteil:** Sprungtaste halten lädt einen Anlauf (Balken wie beim Kamel), loslassen = Stoß ≈ 6 Blöcke vorwärts, 3 s Abklingzeit. Trifft er den Stamm eines natürlichen Baums, schüttelt der Baum: Blätter rieseln, es fallen Äpfel, Setzlinge, Stöcke; reife Kakaoschoten am Dschungelstamm geben Bohnen (Schote auf Stufe 0, wie Ernten). **Der Baum bleibt stehen.**
- **Ertrag:** Vanilla-Blatt-Loot-Tabelle der getroffenen Blattsorte (wie Abbauen mit der Hand, ohne Glück) für die Hälfte der Blätter des Baums (Suche ab Stamm über Stämme/Blätter, Abstand ≤ 6, max. 40 Würfe) + Fallobst-Wurf (Apfel 35 %) bei Eiche/Schwarzeiche. Richtwert Eiche ≈ ½ Apfel, 1–2 Setzlinge, ~1 Stock. Fallobst als Datapack-Loot-Tabelle `simpledinos:shake/windfall`.
- **Grenzen:** nur Blätter mit `persistent=false` (keine Farm aus gesetzten Blättern); je Baum (unterster Stamm) 10 min Ruhe, der Triceratops merkt sich 32 Bäume; ein ruhender Baum klopft nur dumpf (Lernsignal). Mobs im Anlauf: kräftiger Rückstoß, **kein Schaden**; Spieler werden nie getroffen; kein Block wird zerstört; Bienennester bleiben unberührt.
- **A12:** Nutzen: erneuerbare Äpfel, Setzlinge und Stöcke, ohne den Wald abzuholzen. Änderung: Blatt-Loot ohne Abbau. Eingriff: gering (Ertrag je Baum ≈ halbes Abholzen der Blätter, mit Ruhezeit).
- **A14:** Kopf senkt sich beim Aufladen, Schnauben, beim Treffer Holz-Klopfton + Blatt-Partikel der Baumart.
- **Items:** Triceratops-Rüstung Leder/Eisen/Diamant (Form: Stirnschild über dem Kragen, drei Hornkappen, Brustplatte), Triceratops-Eierschale.
- **Modell (~75 Würfel, Vierbeiner-Gerüst):** Rumpf, kurzer Hals, Kopf mit Hornschnabel, Kragen (zwei Ebenen für den Rand), zwei Stirnhörner, Nasenhorn, vier Beine (je 2 Teile), Schwanz 3 Glieder, Sattel- und Rüstungsteile. Animation: Gang, Kopf senken beim Anlauf, Schwanzpendel.
- **Textur:** 5 Felle (`DinoVariants`) + Artmuster: Kragen mit dunklem Rand und zwei hellen Augenflecken (Imponierfärbung), Hörner und Schnabel hornfarben, Rücken dunkel, Bauch hell; Rüstung je Stufe (5 × 4 = 20 Texturen, generiert).
- **Sounds:** Ruhe-Schnauben, Brummen (verletzt), Tod, Anlauf-Schnauben, Stamm-Treffer. Bis zur Hörabnahme Vanilla: Hoglin/Rind tief gestimmt, Treffer = Ziegen-Rammstoß.
- **Config:** `triceratopsShake` (an), `shakeShare` 0,5 (0–1), `shakeAppleChance` 0,35 (0–1), `shakeTreeCooldownSeconds` 600 (60–3600), `ramCooldownSeconds` 3 (1–30).
- **Tests:** schüttelt nur natürliche Bäume; Ertrag > 0 über n Würfe (fester Seed); Ruhezeit je Baum; Mobs nur Rückstoß, kein Schaden; Spieler unberührt; keine Blockänderung außer reifem Kakao.

### 4.2 Stegosaurus – „Pflugtier“ (Arbeit / Acker)

- **Vorteil:** Mit angelegtem **Dino-Pflug** pflügt er beim Gehen einen 3 Blöcke breiten Streifen hinter den Hüften zu Ackerboden. Hält der Reiter Saatgut in der Nebenhand, sät er auf jedes neue Feld eins (Weizen-, Rote-Bete-, Melonen-, Kürbis-, Fackellilien-Samen, Kannenpflanzen-Kapsel, Karotte, Kartoffel).
- **Regeln:** pflügt genau, was eine Vanilla-Hacke darf (26.3-Datentransformer `minecraft:hoe`: Gras/Erde/Pfad → Ackerboden, grobe Erde → Erde, Wurzelerde → Erde + Hängende Wurzeln), nur mit Luft darüber, nur wo der Reiter bauen darf (SimpleLib `InteractionGuard.permission`, `mayInteract`, Spawnschutz, Claims). Zertrampelt nie Ackerboden (springt nicht).
- **Pflug:** Rechtsklick auf den eigenen erwachsenen Stegosaurus legt ihn an (wie die Truhe am Esel), Schere nimmt ihn ab (wie Sattel/Rüstung in 26.3), fällt beim Tod. Haltbarkeit 750 (−1 je Feld), Amboss-Reparatur mit Eisenbarren, verzauberbar (Haltbarkeit, Reparatur). Sprungtaste senkt/hebt ihn; gesenkt Tempo 0,12.
- **A12:** Nutzen: große Felder schnell anlegen und besäen (Ackerbau ist Vanilla-Kern). Änderung: Hacke in 3er-Breite vom Reittier aus. Eingriff: gering (nur Hackenregel, nur mit Reiter und Rechten).
- **A14:** Pflug sichtbar gesenkt/gehoben, Erd-Partikel und Hacken-Ton je Feld (gedrosselt), Haltbarkeitsbalken am Item.
- **Items:** **Dino-Pflug** (neu, §6), Stegosaurus-Rüstung ×3 (Form: Hals- und Flankenplatten, Rückenplatten bleiben frei), Stegosaurus-Eierschale.
- **Modell (~70 Würfel):** hoch gewölbter Rumpf, kleiner tiefer Kopf, Hinterbeine länger als Vorderbeine, Schwanz 3 Glieder + 4 Stacheln, zwei versetzte Plattenreihen (je 7–9 flache Würfel), Pflug-Teil (Joch an den Hüften, Rahmen, 3 Schare; nur sichtbar mit Pflug, gesenkt/gehoben).
- **Textur:** Platten mit Verlauf zur Spitze (ocker-rot, Wärmeregulation), Rücken dunkel, Flanken mit hellen Längsflecken, Stacheln hornfarben.
- **Sounds:** tiefes Grunzen, Brummen, Tod, Pflug-Ächzen/Schwanzschlag. Bis zur Hörabnahme Vanilla: Schnüffler tief gestimmt.
- **Config:** `stegosaurusPlowWidth` 3 (1–3), `stegosaurusSow` (an).
- **Tests:** pflügt nur Umwandelbares mit Luft darüber; fremder Claim/Spawnschutz → nichts; Breite aus Config; Haltbarkeit sinkt; Säen verbraucht Saat; kein Zertrampeln; Pflug anlegen/abnehmen/Drop beim Tod.

### 4.3 Utahraptor – „Kletterer“ (Reiten / Gelände)

- **Vorteil:** klettert mit Reiter senkrechte Wände hinauf: gegen eine Wand laufen = klettern (wie die Spinne). Ausdauer 4 s (≈ 12 Blöcke), dann rutscht er langsam ab (kein Fallschaden beim Rutschen); lädt am Boden in 5 s auf. Realitäts-Anker: Sichelkrallen und WAIR (flügelunterstütztes Steilwandlaufen, bei heutigen Vögeln beobachtet, für befiederte Dromaeosaurier vermutet).
- **Grenzen:** nicht kletterbar ist Tag `#simpledinos:raptor_unclimbable` (Eis, Packeis, Blaueis, Glas, Glasscheiben, gefärbtes Glas; Server können ergänzen). Klettert nur mit Reiter (keine KI-Kletterwege). Tempo 0,30: schnell wie ein gutes Pferd, die besten Pferde (0,3375) bleiben schneller.
- **Sonst:** Sprung 0,7 (≈ 3 Blöcke), Fallschaden halbiert und 3 Blöcke mehr sicher (für Raptor und Reiter); passt durch 2 Blöcke breite Gänge.
- **A12:** Nutzen: Berge, Schluchten, Höhlen und Steilküsten ohne Leitern oder Blöcke. Änderung: Reittier klettert. Eingriff: **mittel** (Mobilität) → abschaltbar (`raptorClimbSeconds` = 0), begrenzte Höhe, Block-Tag.
- **A14:** Kratz-Partikel (Blockpartikel der Wand) + Kratzton, Ausdauer im Sprungbalken, Rutsch-Ton.
- **Items:** **Raptorfeder** (neu, §6), Utahraptor-Rüstung ×3 (Form: leichte Hals-/Brustschiene + Schienbeinschoner, Krallen frei), Utahraptor-Eierschale.
- **Modell (~55 Würfel, T.-rex-Gerüst kleiner):** schlanker Zweibeiner, langer steifer Schwanz mit Federsaum, Arme mit Federfächern, erhobene Sichelkralle, kleiner Federschopf; Kletter-Pose (Körper ≈ 60° aufgerichtet, Arme vorn).
- **Textur:** Federkleid: dunkler Rücken, gebänderte Arm- und Schwanzfedern, helle Kehle; je Fell.
- **Sounds:** Zirpen/Gurren, Fauchen (verletzt), Schrei (Tod), Kratzen beim Klettern. Bis zur Hörabnahme Vanilla: Papagei/Fuchs.
- **Technik-Risiko:** Die Bewegung eines gerittenen Tiers rechnet der Client des Reiters → Kletterregel und Ausdauer laufen auf beiden Seiten gleich (synchronisierter Wert), Server-Bewegungsprüfung beachten. Darum als letzter Dino.
- **Config:** `raptorClimbSeconds` 4 (0–10, 0 = aus), `raptorFeatherMinutes` 10 (1–60).
- **Tests:** Kletterregel (Unit) und Klettern an 6er-Wand (Gametest), Ausdauer-Ende → Rutschen ohne Schaden, Tag-Blöcke nicht kletterbar, Config 0 = aus, Fallschaden halbiert, Feder-Bürsten mit Abklingzeit.

### 4.4 Parasaurolophus – „Leittier“ (Transport / Tiere)

- **Vorteil:** Sprungtaste = **Kamm-Ruf** (Abklingzeit 20 s). Nutztiere in 24 Blöcken folgen ihm 2 min, auch durchs Wasser. Welche: die, die das Futter in der Haupthand des Reiters mögen (Vanilla-Locken `isFood`: Weizen → Kühe, Schafe, Ziegen, Mooshrooms; Karotte → Schweine, Kaninchen; Samen → Hühner …); leere Hand → alle Tiere aus `#simpledinos:herd_followers` (Kuh, Mooshroom, Schaf, Schwein, Huhn, Ziege, Kaninchen, Pferd, Esel, Maultier, Lama, Kamel, Gürteltier, Schnüffler; optional `simpleanimals:deer`, `zebra`, `zorse`). Höchstens 24 Tiere; ein zweiter Ruf innerhalb von 5 s entlässt die Herde.
- **Ausgenommen:** von anderen gezähmte Tiere, angeleinte, gerittene, Tiere mit Namensschild (Config), Tiere in fremden Claims (Rechte-Probe am Standort des Tiers).
- **Schwimmt:** trägt den Reiter an der Wasseroberfläche, wirft nicht ab (Pferd/Kamel stehen im Tag `dismounts_underwater`), Tempo 0,6 ×. So führt er die Herde über Flüsse.
- **A12:** Nutzen: Tiere umsiedeln, Herden aus fernen Biomen zur Farm holen (in Vanilla mühsam: Leinen, Futter in der Hand, Boote). Änderung: zeitweises Folgen wie beim Locken. Eingriff: gering (Zucht und KI der Tiere bleiben Vanilla).
- **A14:** Ruf weit hörbar (64 Blöcke), Schallringe am Kamm, Noten-Partikel über jedem Folger alle 2 s, Abklingzeit im Sprungbalken.
- **Items:** Parasaurolophus-Rüstung ×3 (Form: Kammhaube + Rückendecke), Parasaurolophus-Eierschale. Kein Werkzeug: der Kamm ist das Werkzeug.
- **Modell (~65 Würfel):** Rumpf, langer Hals, Entenschnabel-Kopf, röhrenförmiger Kamm nach hinten (3 Würfel), schlanke Vorderbeine, kräftige Hinterbeine, tiefer Schwanz; Ruf-Pose (Kopf hoch, Maul offen).
- **Textur:** Kamm mit kontrastierendem Band (dunkel, helle Kante), Flanken mit senkrechten Schattenstreifen, Bauch hell.
- **Sounds:** Ruf (lang, tief, resonant, 1,5–2,5 s, 3 Varianten), Brummen, Tod, Schnauben. Bis zur Hörabnahme Vanilla: Ziegenhorn tief gestimmt.
- **Technik:** kein Eingriff in Vanilla-KI-Ziele (kein Mixin): der Parasaurolophus lenkt seine Folger selbst (Navigation alle 20 Ticks, gestaffelt), Liste ≤ 24 UUIDs, endet beim Entladen.
- **Config:** `callRadius` 24 (8–48), `callSeconds` 120 (30–600), `callMaxAnimals` 24 (1–64), `callCooldownSeconds` 20 (5–300), `callIgnoreNamed` (an).
- **Tests:** Auswahl per Handfutter und leere Hand; Ausschlüsse (fremd gezähmt, Leine, Name, Claim); Höchstzahl; Ende nach Zeit und Doppelruf; Schwimmen mit Reiter ohne Abwurf.

### 4.5 T. rex (abgenommen) – nur Abgrenzung

Bleibt der einzige Kämpfer (verteidigt den Besitzer). Geplantes Brüllen bleibt T.-rex-eigen. **Vorschlag:** den geplanten, nicht gebauten Ausdauer-Sprint streichen (doppelt zum Tri-Anlauf; der echte T. rex war kein Sprinter). Beim Bau: Kampf-Ziele aus `Dino` nach `Trex` verschieben, Verhalten des T. rex bleibt gleich.

## 5. Abgrenzung (Doppel-Check)

| Fähigkeit | T. rex | Tri | Stego | Raptor | Para | Vanilla-Reittier |
|---|---|---|---|---|---|---|
| Kämpft / verteidigt Besitzer | **ja** | – | – | – | – | Wolf, Eisengolem (keine Reittiere) |
| Sprungtaste | springen | Anlauf | Pflug | springen | Ruf | Kamel: Sprint |
| Ressourcen erzeugen | – | **Bäume schütteln** | – | (Feder, klein) | – | – |
| Acker | – | – | **pflügen + säen** | – | – | – |
| Gelände | – | – | – | **Wände klettern** | – | Pferd: hoher Sprung |
| Tiere bewegen | – | – | – | – | **Herdenruf** | Leine, Locken |
| Wasser | – | – | – | – | **schwimmt mit Reiter** | Nautilus (unter Wasser), Boot |
| Höchsttempo | – | – | – | schnell, unter Bestpferd | – | **Pferd** |
| Lasten (Kiste) | – | – | – | – | – | **Esel, Maultier, Lama** |
| Fliegen / Lava / 2+ Reiter | – | – | – | – | – | Glücklicher Ghast, Schreiter, Kamel |

Jede Zeile hat höchstens einen Dino; was Vanilla-Reittiere schon können, bekommt kein Dino (Truhe, Fliegen, Lava, Höchsttempo, mehrere Reiter).

## 6. Neue Items (18)

| Item (ID-Vorschlag) | Herkunft / Rezept | Vanilla-Nutzen |
|---|---|---|
| Dino-Pflug `dino_plow` | `I K I` / `H H H` (I Eisenbarren, K Eisenkette, H Eisenhacke); Haltbarkeit 750, Reparatur Eisenbarren, verzauberbar | Ackerbau |
| Raptorfeder `raptor_feather` | erwachsenen Utahraptor mit dem Vanilla-Pinsel bürsten (1 je 10 min, wie Gürteltier-Hornschild); 1 → 3 Federn (formlos) | Pfeile, Buch und Feder ohne Hühnerfarm |
| Eierschale je Art `triceratops_eggshell`, `stegosaurus_eggshell`, `utahraptor_eggshell`, `parasaurolophus_eggshell` | fallen beim Schlüpfen (3 Stück); 1 → 3 Knochenmehl | Dünger; macht die Rüstung erst nach dem Schlüpfen im Rezeptbuch sichtbar (wie Schildkröten-Hornschild) |
| Rüstung ×12 `<dino>_armor_leather` / `_iron` / `_diamond` | 8 Leder/Eisen/Diamant um 1 Eierschale der Art (Muster wie T. rex) | Schutz des Dinos; Werte wie Pferderüstung 3/5/11, Haltbarkeit wie Pferd |

T. rex bleibt unverändert (Knochenblock in der Mitte). Texturen: 18 Item-Texturen in A8c-Runden; Eierschalen aus den vorhandenen Ei-Paletten abgeleitet. Großer Sattel passt allen (Tag `can_equip_large_saddle` um die vier erweitern).

## 7. Spawn und Fundort

- **R6 bleibt:** kein natürlicher Spawn, kein Spawn-Ei; Dinos nur aus Fossil → Ei → Brüten → Schlüpfen. Ei-Gewichte R10 bleiben die Grundlage; mit dem Bau jedes Dinos wird seine Sorte schlüpffähig (`DinoKind.entity()`), bereits gewürfelte Fossilien behalten ihre Sorte.
- **Vorschlag Fundort-Gewichtung (Frage `fund`):** realitätsnah nach Fundschichten, ohne die Gesamtanteile stark zu ändern: Badlands → Utahraptor × 2, Stegosaurus × 1,5 (Morrison- und Cedar-Mountain-Formation, Utah/Colorado); Wüste → Triceratops × 1,5, T. rex × 1,5 (Hell Creek); Sumpf/Mangrove → Parasaurolophus × 2 (Flussdelta-Ablagerungen). Wer einen bestimmten Dino will, gräbt im passenden Biom. Betrifft nur noch nicht gewürfelte Fossilien.
- **Felle:** alle fünf Felle je Art nach Aufzucht-Biom (ABN30-1), Artmuster §4.

## 8. Gemeinsame Technik beim Bau

- Kampf-Ziele aus `Dino` nach `Trex`; je Art eigene Abwehr (§4 Tabelle). Sprungtaste je Art über Vanilla `PlayerRideableJumping` (wie Kamel).
- IDs ab erstem Release fest: `simpledinos:triceratops`, `stegosaurus`, `utahraptor`, `parasaurolophus` (Ei-IDs existieren).
- Entdeckbarkeit (A6): Handbuch-Seite je Dino + beim ersten Aufsitzen je Art ein einmaliger Hinweis in der Aktionsleiste; Item-Tooltips ≤ 2 Zeilen (z. B. Pflug: „Am Stegosaurus: Sprungtaste senkt den Pflug“).
- Pflichten je Dino: Ausnahme in `simplemobheads/data/mobs.json`, Lang EN/DE, `wiki/manual.json`, Guide, Questionary, Preise (`simplemoney`), Discover, Kreativtab, Testzentrale (Eier, Pflug, Rüstungen), JEI.
- Leistung: Fähigkeiten rechnen nur beim Reiten/auf Tastendruck; Para-Folger ≤ 24 gestaffelt, Tri-Suche ≤ 96 Blätter, Stego 3 Felder je Schritt; Ziel ≤ 0,05 ms je Dino und Tick (WP-18), MSPT-Grenze RELSB31 f5.

## 9. Aufwand und Reihenfolge

| Schritt | Inhalt | Agententage |
|---|---|---|
| 1 | Gemeinsam: Kampf-Ziele nach `Trex`, Sprungtasten-Rahmen, Eierschalen, Fundort-Gewichtung, Config-Schlüssel | 0,5–1 |
| 2 | Triceratops: Modell (Vierbeiner-Gerüst), Felle, Rüstung, Anlauf, Baumschütteln, Tests | 2,5 |
| 3 | Stegosaurus: Modell mit Platten + Pflug-Teil, Dino-Pflug, Pflügen/Säen, Tests | 2,5 |
| 4 | Parasaurolophus: Modell, Ruf/Herde, Schwimmen, Tests | 2,5 |
| 5 | Utahraptor: Modell mit Federn, Klettern (Client + Server), Raptorfeder, Tests | 3 |
| 6 | 18 Item-Texturen in A8c-Runden, Turntable-Vorschau je Dino (wie a-dino30) | 1,5 |
| 7 | Sounds ElevenLabs (≈ 70 Erzeugungen, ≈ 2,5–3 k Credits), Hörprobe | 1 |
| 8 | Integration: Wiki, Guide, Lang, Preise, Testzentrale, Bündel/Gate | 1 |

Summe ≈ 14–15 Agententage. Je Dino ein eigener Branch mit Optik-Vorschau vor dem Merge; Veröffentlichung erst mit allen fünf (RELSB31 f4). Raptor zuletzt (höchstes Technik-Risiko).

## 10. Wahl in der Vorschau (Antwortcode DINO36)

`tri`, `stego`, `raptor`, `para` je ok/Einwand (Alternativen bei Einwand: Tri Pflug, Stego Äsen, Raptor Sprung-/Tempo-Reittier, Para Warnruf wie Glocke) · `trex` Ausdauer-Sprint streichen · `items` Liste §6 · `fund` Fundort-Gewichtung · `bau` Reihenfolge/Aufwand §9.

## 11. Nicht in diesem Konzept

Zweiter Reiter (R9, später), max. zahme Dinos je Spieler (WP-18, Standard 4), Vanilla-Fossilstruktur-Anteil (R7a), Dino-Köpfe (Simple Mob Heads), Netherit-Rüstung (R8 später), NeoForge/Forge (nur 26.3 Fabric).
