# Konzept B-BACK22 – Gesamtdokument: 10 Ebenen + Kern (10.11.2026)

NUR KONZEPT – nichts davon ist gebaut. Branch `b-back22-konzept` (Merge aus `b-back22-ebenen-a` c89aa6852, `b-back22-ebenen-b`
ae64d311f, `b-back22-kern-konzept` e9636ba7d). Auftrag „N22-Nachtrag 2“, Gesamtteil (/root/b-auftrag-20261109.md), Besitzer-Wunsch und
-Antworten /root/sb-tools-a/wunsch-back22.md. Nur MC 26.3 Fabric. Vorschau: /root/sb-tools-b/previews/b-back22-preview.html,
Antwortcode **BACK22** (vorige Fassung BACK22b als `b-back22-preview-v2.html`).

## 0. Dokumente und Vorrang

| Dokument | Inhalt | Stand |
|---|---|---|
| `KONZEPT-B-BACK22-2026-11-10.md` | Grundkonzept R1–R9: eine Dimension, Raster 112, Tür-Regeln, Keller-Doppeltür, Stufen S0–S3, Fragen g1–g14 | Grundlage; **§2-Tabelle (Items je Ebene) ist überholt**, es gelten die Ebenen-Dokumente + dieses Dokument |
| `KONZEPT-B-BACK22-KERN-2026-11-10.md` | Kern K-R1–K-R9: Band-API, Schreibpfad, Speicher/Messplan S0, Portal-API in SimpleLib, Datei-Eigentum, Plan K1–K7 | K-R9 ist neu (aus dieser Gesamtprüfung) |
| `KONZEPT-B-BACK22-EBENEN-A-2026-11-10.md` | Ebenen 1–5 (je 3 Runden, G1/G2, Fragen e1–e5) | Detail gilt, Korrekturen §3 hier |
| `KONZEPT-B-BACK22-EBENEN-B-2026-11-10.md` | Ebenen 6–10 (je 3 Runden, G3/G4, Fragen h1–h6) | Detail gilt, Korrekturen §3 hier |
| **dieses Dokument** | Kurzfassung aller 10 Ebenen, Gesamtrunden G-R1–G-R3, konsolidierte Korrekturen, Budget, Teile-Bilanz, Fragen | **Vorrang** bei Widerspruch |

Vorrang-Regel: dieses Dokument → Kern K-R9 → Ebenen-Dokumente → Grundkonzept. Zählung der Revisionen: Grundkonzept 9, Kern 9, je Ebene 3,
gemeinsame Ebenen-Runden G1–G4, Gesamtrunden G-R1–G-R3 → jede Ebene hat ≥ 3 eigene + ≥ 5 übergreifende Runden (Auftrag: gesamt ≥ 5).

## 1. Kurzfassung der 10 Ebenen (Reihenfolge R9, Wahl g1)

Raster: Ebene i offen `y = 208−112(i−1) … 303−112(i−1)`, darunter eine volle Grundgestein-Sektion. Abwärts-Weg = gerechneter Ø-Weg von jeder
Zelle zur nächsten Tür nach unten (Laufzeit bei 4,3 Bl./s, ohne Suchen). Ersatzteil **fett** (nur Beute).

| # | Name | y | Stimmung · Nebel (Farbe, Start/Ende) | Strukturen · Großbauwerk | Geschoss · Treppe | Labyrinth · Abwärts Ø | Items | Tür-Regel · Symbol (Ziel) | Rätsel / Gefahr | Licht · Leistung |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Lobby | 208–303 | gelbe Tapete, feuchter Teppich, Neonbrummen · 0xC9B458, 8/40 | Büro-Zellen 8, abgehängte Decken, Lichthöfe, Pausenraum, Sitzecke · **Säulensaal 64×64×24** | 8 · 4+2+4 | niedrig, Schlaufen 50 % · 57 Bl. (13 s), mit e4 ≈ 80 | Tapetenrolle, Teppichrolle (→ Wasser), Neonröhre, Tapetenschaber, Kreide + Kreidebox | zweiwegig · zu E2 Welle (hellblau); EXIT grün | keins; Kreide-Pfeile, EXIT nah | Neon 15 im 8er-Raster, ≤ 60 Licht/Chunk · ≤ 0,3 ms |
| 2 | Poolrooms | 96–191 | weiße Fliesen, stilles Wasser, Hall · 0xBFE4E8, 32/120 | Fliesenhallen, Duschen, Rutschenröhren, Terrassenbecken, Galerie-Halle · **Großes Becken 96×64, Halle 40, Tiefbecken 30** | 10 · 5+2+5 | niedrig–mittel, offen · 101 Bl. (24 s) | Poolfliesen (3), Chlortablette, Poolleiter, **Schwimmerventil** | aktivieren (2↔3) · zu E3 Karteikarte | Schleuse (Wasserstand, optional e1) / nur Ertrinken wie Vanilla | hell 13–15 · Wasser ohne Fließ, ≤ 0,5 ms |
| 3 | Archive | −16–79 | Büros, Aktenregale, warmes Licht, Papierrascheln · 0x5A4630, 10/56 | Regalgassen (Zelle 8), Büros, Lesesäle 3 Geschosse, Karteiräume · **Rotunde Ø 48, 60 hoch, 6 Galerien** | 7 · 4+2+3 | mittel–hoch, perfekt · 142 Bl. (33 s) | Akte → Wegkarte, Stempel, Karteikarte, Aktenregal, **Schrankschloss** | bricht (3↔4) · zu E4 Kiste (grau) | Kartei-Code Galerie.Fach.Nummer / keine | warm 11, Gassen 7 · Rotunde ≈ 25–35 k Flächen |
| 4 | Lagerhalle | −128–−33 | Beton, Hochregale, ruhig · 0x555A62, 16/96 | Regalgassen, Laderampen, Mezzanin, Meisterbox · **Hochregallager 48×32×60 mit Kran + Lastenaufzug** | 12 · 6+2+6 | mittel · 190 Bl. (44 s) | Mandelwasser, Palettenbrett, Gegengewicht, Frachtschein, Hubkette (e2), **Kistenbeschlag** | bricht · zu E5 Ventilrad (kupfer); Doppeltür = Rolltor | Lastenaufzug (Last = Frachtschein) / Sturz mit Geländer | Strahler 13 · Hochregal 40–60 k Flächen, Beute ≤ 24/Chunk |
| 5 | Rohrgänge | −240–−145 | warm, eng, Dampf, Kupfer · 0x7A4A32, 4/28 | Gänge 3×5 (Zelle 4), Kriechgänge **1,5 hoch**, Leiterschächte, Ventilknoten, Kesselräume · **Kesselhalle 32×32×32** | 6 · 3+2+3 | hoch, echte Labyrinthe · 269 Bl. (63 s) | Ventilrad (Signal 0–15), Manometer (Füllstand), Dampfrohr, **Dichtung** | bricht · zu E6 Blitz (blau) | Dampfzonen schieben (kein Schaden), Ventilfolge; Umweg immer da | Kupferbirnen 6–9 · kurze Sicht, ≤ 0,3 ms |
| 6 | Elektrostation | −352–−257 | Maschinenhallen, Summen, Blaulicht · 0x3A5A8C, 10/64 | Maschinenreihen, Schaltwarten, Trafohöfe, Kabelkanäle · **Turbinenhalle 64×64×36** | 10 · 5+2+5 | mittel–hoch · 315 Bl. (73 s) | Phasenprüfer, Kabeltrasse, Schaltschrank-Platte, **Sicherung** | bricht · zu E7 Schlüssel mit Nummernschild (weinrot) | Notstrom: Schaltpult 2×2 (16/16 lösbar, ≤ 4 Züge, optional) | Blau 12, Notlicht 4 · Turbinenhalle 25–35 k Flächen |
| 7 | Hotel | −464–−369 | endlose Flure, roter Läufer · 0x4A2A30, 12/56 | Flure 3 breit, Zimmer (1 von 8 echt, Rest Kulisse), Lobby-Halle, Speisesaal · **Atrium 40×40×96, Glasaufzug** | 8 · 4+2+4 | hoch · 456 Bl. (106 s) | Zimmerschlüssel, Hotelglocke, Teppichläufer, Gästebuch, **Türschließer** | bricht · zu E8 heller Punkt (schwarz) | Aufzug-3er-Folge aus Gästebüchern (optional h4) | Wandlampen 9 · Atrium 18–22 k Flächen |
| 8 | Lichtlos | −576–−481 | fast völlige Dunkelheit, Echo · 0x050507, 2/20 | hohe Gänge (Geschoss 16), leere Hallen, Kalzit-Nischen, Lichtinseln · **Brunnen: Ring Ø 48 + Schacht Ø 16 × 96, Wasserpolster** | 16 · 8+2+8 | sehr hoch (Dunkel) · 481 Bl. (112 s) | Leuchtstab (**Ersatzteil**, häufig), Laterne der Tiefe (episch), Kalzit-Wegstein, Echo-Brunnenwasser, Dunkelglas | bricht · zu E9 Haus mit Giebel (violettblau); Tür leuchtet nur intakt | Vier Lichter am Brunnen (optional) / keine Monster, Sturz gesichert | Licht 0, Brunnen 15 · günstigste Ebene |
| 9 | Vorstadt bei Nacht | −688–−593 | Häuserreihen, Laternen, leere Gärten, Nebel-Himmel · 0x14183A, 24/64 | Straßenraster mit Hang ±4, Häuser (30 % betretbar), Gärten, Bushalte/Kiosk · **Stadtplatz 64×64 + Uhrturm 40** | 5 (Haus) · 3+2+2 | sehr hoch (Weite) · 539 Bl. (125 s) | **Hausschlüssel**, Briefkasten, Samentüte, Schlüsselbund, Straßenlaterne | bricht · zu E10 Ähre (goldorange) | „Ein Haus ist anders“ (weißes Fenster, offene Tür) | Laternen 13 · 70 % Kulissenhäuser, ≤ 0,5 ms |
| 10 | Weizenfeld | −800–−705 | endloses Feld, ewiges Abendrot · 0xD98A3E, **28/64** (G-R1) | Feld Zelle 16 (Hügel ±3), Feldwege, Vogelscheuchen-Wegweiser, Hochsitze, Neben-Gehöfte · **Haupt-Gehöft 64×32 (Scheune, Silo, Mühle 34), 1 je 512²** | 6 · 3+2+3 | keiner (Weite) · Ziel-Weg 385 Bl. (90 s) | **Erntegarn**, Schwellenkern (episch), Vogelscheuche, Abendweizen, Mühlstein-Fragment | bricht (9↔10) · Scheunentor = Ausgang (einwegig, h1) | Vogelscheuchen ausrichten → Kernkammer / keine | Licht ohne Himmel A/B′ (§3) · Abendweizen 50 % |

Durchgehend: keine Gegner, kein Schaden außer Vanilla-Sturz/-Ertrinken mit Sicherung (g8 a); jede Ebene führt genau **eine** neue Mechanik ein
(Orientieren → Aktivieren → Karte/Bruch → Last → Folge → Schaltpult → Ziffernfolge → Dunkel/Ton → Hinweis-Suche → Ausrichten). Σ Abwärts-Wege
≈ 2 935 Bl. ≈ 11,4 min reine Laufzeit, mit Suchen/Rätseln ≈ 35–60 min je Abstieg.

---

## 2. Gesamtrunden

Achsen wie Grundkonzept (SG Spielgefühl · MB Machbarkeit · PF Performance · A12 · KO Konsistenz · BA Balance), 1–5.

### G-R1 – Querprüfung Kern ↔ Ebenen, Gefälle, Bilanz, Balance, Budget, Treppen

**(a) Kern deckt Ebenen-Anforderungen ab?** Abgleich aller Abschnitte „Anforderungen an den Kern“ (E1–E10, Sammellisten A und B) gegen Kern
§1–§8 und K-R8. Regel K-R8: was ≥ 2 Ebenen brauchen → Kern.

| Anforderung | Ebenen | im Kern bis K-R8? | Befund / Korrektur (→ K-R9) |
|---|---|---|---|
| Nebel Farbe/Start/Ende je Biom | alle | ja (Biom-Attribute §0, Skript `gen_eNN.py`) | ok; S0 prüft Wirkung je Biom |
| Helligkeit je Ebene (E8 schwarz, E10 hell **ohne Himmelslicht**) | 8, 10 | nur „Umgebungslicht über Biom“ | **Lücke**: E10-Licht A/B/C fehlte. Neu K-R9.1: Variante A Biom-Umgebungslicht (S0), Rückfall **B′** leuchtende Erde im 4×4-Raster (16 Lichtquellen/Chunk statt 256), C verworfen |
| Decke als Himmel (E9/E10) | 9, 10 | nein | **Lücke** → `BuildRules` „Decke ≥ Nebelende + 8 über jedem begehbaren Freiluft-Augpunkt“ |
| Wendeltreppe | 3, 8, 9, 10 | ja (StairBuilder) | **Fehler**: Ebenen rechnen Kreis-Wendel 1:1 „je Block Bogenlänge“ – auf einem Voxel-Kreis liegen Folgestufen oft nur **über Eck** → nicht begehbar. Korrektur K-R9.2: **eckige Wendel** (gerade Läufe ≤ 8 entlang der Wand, Eckpodest), damit WalkCheck sie als normale `StairRun`s prüft |
| Leiter-Austritt | 4, 5, 6, 8, 10 | Kern „2 frei“ | **Widerspruch**: Ebenen schreiben 3 → einheitlich **3 Luft über der Kopfstufe** |
| Rampen 1:3 | 9, 10 | „Rampen gleich“ | **Fehler**: Vollblock-Rampe 1:3 ist nur mit Springen begehbar. Korrektur: Rampe = Folge aus Platte/Vollblock (Stufenhöhe ½, Auto-Step), Steigung ≤ 1:2, 3 Luft darüber |
| Galerie-Schacht (Lichthof E1, Lesesaal E3, Galerie-Halle E2, Atrium E7) | 1, 2, 3, 7 | nein (nur Raum-Module) | **Lücke** → Kern-Baustein `GalleryWell(b, t, Geschosse, H, Brüstung)` |
| Bewegte Plattform | 4, 7 | ja (K7) | Parameter ergänzt: n Haltestellen, Eingabe-Haken (Last E4, Folge E7) in der Ebene, nur Spieler + Items mitnehmen |
| Zeiger-Block 16 Richtungen | 8 (Wegstein 4), 10 (Vogelscheuche 16), Kreide-Pfeile | nein | **Lücke** → `PointerBlock` (Vanilla-Eigenschaft `rotation` 0–15) + reine Funktion `Pointers.toward(seed, x, z, ziel)` |
| Rätsel-Steuerung (Schaltpult 2×2, Aufzug-Tafel, Ventilfolge, Katalog-Fach, Lichtnischen, Vogelscheuchen) | 3, 5, 6, 7, 8, 10 | „bleiben in der Ebene“ | Teilweise **Lücke**: Logik bleibt Ebene, aber gemeinsames Gerüst fehlte → `PuzzleController` (Zustand aus Seed, `solved()` von der Ebene, gelöst → `PuzzleGate` öffnet Kammer/Tür, Rückstellung) + Test-Haken „jede Startstellung lösbar“ |
| Ton-Emitter (Brunnen 64, Uhrturm 96) | 8, 9 | nein | **Lücke** → `SoundBeacon` (Intervall, Reichweite) + Test „Reichweite < Abstand zur nächsten begehbaren Fläche einer fremden Ebene“ |
| Kulissen-Raum + Nummernschloss + Schlüssel (Zimmer, Häuser, Schlüsselbund hält beide) | 7, 9 | nein | **Lücke** → `FacadeRoom`, `NumberedLock`, Schlüssel-Tag `simplelayers:backrooms/keys` (Schlüsselbund aus E9 greift auf E7-Schlüssel zu → nur über Kern-Tag, nie direkt) |
| Sturzschutz (Abgrund > 3 neben Weg → Brüstung oder Wasserpolster) | 2, 4, 7, 8, 9, 10 | nein | **Lücke** → `BuildRules` |
| Beute „einmal je Spieler“ (Kernkammer E10, Brunnen-Laterne E8) | 8, 10 | nein | **Lücke** → `BandLoot.oncePerPlayer` (SavedData-Flag im Kern) |
| Ausgang einwegig (Scheunentor E10, Lobby-EXIT) | 1, 10 | Nebel-Übergang ja, Regel nein | **Lücke** → `ExitDoor` (kein Link; Ziel nach h1) |
| Fortschritte „Ebene n erreicht“ + „Am Horizont“, Wegbuch-Seite Ersatzteile | alle | nein | Kern liefert Fortschritte aus `Bands.ALL` und die Teile-Liste aus `Band.repairPart()` |
| Pflanzen ohne Himmel (Weizen, Rasen, Hecken) | 9, 10 | nein | **Fehler**: Vanilla-Weizen fällt bei Licht < 8 ab, Ackerboden trocknet, Laub zerfällt. Korrektur: Feld aus **Abendweizen** (nicht tickender Zierblock, ohnehin E10-Item) auf Erde, Laub `persistent`, Rasen = Moos; `BuildRules`: keine tickenden Pflanzen |

**(b) Item-Gefälle 1→10.** Höchste Tooltip-Stufe je Ebene: gew · ungew · selten · ungew (+ Hubkette selten, e2) · selten · selten · selten · **episch** ·
selten · **episch**. Befunde: (1) E4 und E9 fielen heraus – beide schon mit Hubkette/Schlüsselbund behoben (G1/G3). (2) **Redundanz Manometer (E5) ↔
Phasenprüfer (E6)**: beide zeigen Signalstärke → Manometer zeigt nur noch **Füllstand** (Behälter/Kessel), Phasenprüfer Signal + Quellrichtung.
(3) **Widerspruch Leuchtstab**: EBENEN-B §0 „Ersatzteil nicht craftbar“, E8-Tabelle gibt ein Rezept → Rezept gestrichen (Beute häufig genügt).
(4) Item-Zahl 4–6 je Ebene (Σ ≈ 48 Items + ≈ 35 neue Blöcke) → Texturen sind der größte Bauposten (A8c ≥ 3 Runden je Textur) → beim Bau
Platzhalter zuerst, Texturen je Ebene als eigene Runde (Plan §6).

**(c) Fortschritt / Teile-Bilanz Rückweg.** Rechnung EBENEN-B G4 (7 brechende Paare, 7 Wartungskästen = genau die erste Rückreise) stimmt, übersieht aber
zwei Dinge: (1) **Paare sind dicht** (Dichte 0,04 je Chunk) und jedes neue Paar startet OFFEN → Festsitzen ist strukturell unmöglich; Ersatzteile
sparen Zeit („bekannter Rückweg“), sie sind kein harter Zwang – das trifft den Besitzer-Satz „wer sich den Weg merkt, kennt den Rückweg“. Ausnahme 2↔3:
jedes Paar startet RUHEND → Aufsteiger aus E3 brauchen Schrankschloss oder Kasten. (2) **Ausbeutung**: Kästen geben ein Teil heraus; wer Kästen abläuft,
sammelt Sicherungen/Türschließer, die zugleich **Bauteile der Oberwelt-Tür** sind → Seltenheit wertlos. Korrektur: **Wartungskasten repariert bzw.
aktiviert seine Tür direkt** (Rechtsklick, einmal), gibt kein Item heraus (Frage g16).
Aktivierung von der E3-Seite: das Teil der **eigenen** Ebene (Schrankschloss), wie bei der Reparatur – Kern §5.3 entsprechend präzisiert.

**(d) Balance Oberwelt-Türen.** Grenzen bisher: nur Oberwelt, ≤ 1024, nur Spieler, ein Kern = ein Paar, Kern einmal je Spieler (h5). Befunde: (1) „Abbau gibt
die Tür zurück“ macht ein Ende **tragbar**: Ende A zu Hause, Ende B mitnehmen und überall setzen = Heimruf-Gegenstand → genau das „Easy-Travel“, das der Besitzer
ausschließt. Korrektur: Paar **verankert** sich beim ersten Setzen beider Enden; Umsetzen eines Endes macht das Paar RUHEND, Reaktivieren verbraucht je eine
Sicherung + einen Türschließer (Frage g15). (2) **Echo-Splitter aus dem Brunnen** (2 je Brunnen, ein Brunnen je 256²) wären durch Erkunden farmbar und
entwerten die Antike Stätte. Korrektur: Schwellentür verlangt 2 **Vanilla**-Echo-Splitter (Antike Stätte) – verbindet Backrooms mit Vanilla-Tiefe statt sie
zu ersetzen (neue Empfehlung h2 c). (3) Mehrspieler: n Spieler → höchstens n Paare; vertretbar bei 35–60 min Abstieg + Rätsel + Antike Stätte.

**(e) Performance-Budget gesamt.** Summe siehe §4. Befund: **E10 Variante B** (leuchtender Boden auf jedem Feldblock, +20–50 ms/Chunk) sprengt das Kern-Budget
(≤ 1 ms je Ebenen-Chunk) um das 20–50-fache → ersetzt durch B′ (16 Lichtquellen/Chunk, Abstand ≤ 4 → Licht ≥ 11 am Boden). Variante C (Lichtraster in 12 Höhe)
war in EBENEN-B als „teurer“ bezeichnet, ist es nicht (4/Chunk), reicht aber nicht (Licht am Boden ≈ 3) → verworfen. Weizen: Abendweizen hat keine
Zufalls-Ticks (Leistungsgewinn), Dichte 50 % bleibt.

**(f) Treppen-/Kopffreiheit überall.** Alle 10 Treppenhäuser nachgerechnet (H = Lauf1 + Lauf2: 8=4+4, 10=5+5, 7=4+3, 12=6+6, 6=3+3, 10, 8, 16=8+8, 5=3+2, 6=3+3;
Podest ≤ 8 ✓; Kopffreiheit ≥ 3 in Spalte i und i−1 ✓). Befunde: Wendel (s. a), Rampen (s. a), Leiter 2 vs 3 (s. a), außerdem **E5-Kriechgang „2 hoch, Spieler
kriecht“ ist falsch** – 2 Blöcke sind normal begehbar (Spieler 1,8). Korrektur: Kriechgang = **1,5 hoch** (Platte als Decke) → Schleichen nötig; vom
Kopffreiheits-Test ausgenommen, aber im Bauregel-Prüfer als `CRAWL` markiert und am Eingang telegrafiert (Warnband, A14).

**(g) Reihenfolge R9 vs. A-Liste.** Ebenen-Dokumente folgen R9. Befund: Kern-Pakete/Namespaces heißen `eNN` → die Position ist in Dateinamen, Tests,
Sprach-Namespaces und Beute-Pfaden festgeschrieben; eine spätere Umstellung (g1 b) würde alle Ebenen-Branches umbenennen. Korrektur K-R9.6: Pakete und
Namespaces nach **Thema** (`level.lobby`, `simplelayers_pools`, Tests `pools_*`), Position nur in einer Kern-Liste `Bands.ORDER`. Die Wahl g1 bleibt
dadurch bis kurz vor dem Bau offen. Folgen der Wahl siehe §7.

**(h) Kleinere Unstimmigkeiten.** (1) EBENEN-A nennt im Fließtext die Schleuse „Frage e2“ und die Hubkette „Frage e4“, die Fragenliste zählt anders (e1
Schleuse, e2 Hubkette …) → gültig ist die **Liste**. (2) Antwortcode: Ebenen-Wahl `e1…e10` kollidiert mit den Fragen e1–e5 → Fragen im Code mit
Vorsatz **f** (`fe1`, `fh3`, `fk1`, `fg1`). (3) Wartungskasten an zweiwegigen Türen (1↔2) entfällt (kein Teil nötig). (4) E10-Nebel Ende 80 + Decke
≈ 88 über Boden verletzt die neue Himmel-Regel (Hochsitz-Auge ≈ 14 → Abstand 74) → Nebel 28/64; Mühle damit bis 64 sichtbar (vorher 80), Vogelscheuchen
übernehmen den Rest. E9: Uhrturm-Glockenstube **geschlossen** (offene Stube in 38 Höhe sähe die Decke), Freiluft-Höchstpunkt = Hang +4 → Abstand 82 ≥ 72 ✓.
(5) Kern-Tabelle §2 im Grundkonzept (Beckenleuchte, Schwimmnudel …) überholt.

**Bewertung G-R1:** SG 4,5 · MB 3,5 · PF 3,5 · A12 4 · KO 3 (viele Lücken) · BA 3,5 (Kasten-Ausbeutung, tragbares Tür-Ende).

### G-R2 – Korrekturen prüfen, Besitzer-Wunsch Punkt für Punkt

| Korrektur aus G-R1 | Gegenprüfung | Ergebnis |
|---|---|---|
| E10 Licht B′ | 4×4-Raster, Licht 15: weitester Bodenblock 2+2 = 4 → 11, Weizenhöhe 10; Abendweizen braucht kein Licht → B′ nur Optik; A bleibt erste Wahl (kostenlos) | ✓ |
| eckige Wendel | Brunnen (innen 14×14): Seiten mit Lauf 6 + Podest + Lauf 5, ≈ 48 Stufen/Umlauf → Windung darüber 48 ≥ 3 ✓; Silo (innen 8×8): Lauf 6 + Eckpodest, 24/Umlauf ✓ mit Brüstung zum 4×4-Kern; Uhrturm (innen 12×12) ✓; Rotunde: 8 gerade Läufe ≤ 8 je Galerie-Abschnitt ✓ | ✓, Bilder der Ebenen (Kreisdarstellung) beim Bau anpassen |
| Rampen ½-Stufen | E9-Hang ±4 → Rampe 8 lang (1:2); Straße 5 breit genügt | ✓ |
| Kriechgang 1,5 | Schleichen 1,5 passt; Dampfzonen nur in Gängen 3×5, nicht im Kriechgang → Schub kann niemanden einklemmen | ✓ |
| Kasten repariert direkt | Bilanz unverändert (1 Durchgang je Türende), keine Teile-Ausbeute; Sicherung/Türschließer nur aus Beute (garantiert je Großbauwerk) → selten bleibt selten | ✓ (g16) |
| verankerte Oberwelt-Tür | Umsetzen kostet Sicherung + Türschließer (je aus E6/E7) → möglich, aber teuer; kein Heimruf | ✓ (g15) |
| Vanilla-Echo-Splitter | Antike Stätte ist ohnehin das „tiefste“ Vanilla-Ziel; Bergungskompass bleibt im Gleichgewicht | ✓ (h2 c) |
| Pakete nach Thema | Kern-Tests iterieren `Bands.ALL` unverändert; Gametest-Filter `simplelayers:<thema>_*`; keine Kollision mit vorhandenen Ids (`simplelayers:backrooms_*` bleibt Präfix der Blöcke) | ✓ |
| Manometer = Füllstand | E5 behält 4 Items, keine Doppelung mit E6 | ✓ |

**Besitzer-Wunsch (wunsch-back22.md) gegen den Gesamtstand:**

| Wunsch | erfüllt durch | Rest |
|---|---|---|
| polished, viele Items | ≈ 48 Items + ≈ 35 Blöcke, Bauraster, BuildRules | Texturen = Bau-Runden |
| Treppen verbunden, kein Kopfstoß | StairBuilder + WalkCheck für alle Läufe inkl. eckiger Wendel, Rampen, Leitern | Live-Fix b-stairs22 schon getrennt |
| eine Dimension, Schichten ≈ 100, Bedrock dazwischen | Raster 112 (96 offen), k1 | Besitzer-Wahl k1 |
| viel Höhe, unendlich, ≥ 5 Schichten | 10 Ebenen, Geschosse + Großbauwerke | – |
| nicht alle Schichten rendern | volle Grundgestein-Sektion (26.3-Quellen geprüft), Bandfenster bedingt (k2) | S0 misst Speicher |
| große Bauwerke | 10 Großbauwerke (Säulensaal … Haupt-Gehöft) | Flächenzahlen S0 |
| Durchsicht + sofort drüben | S1 nahtlos, S2 Tiefenfenster (k3), Keller = Kulisse + Nebel | Grenzen ehrlich (§5.6 Kern) |
| Gegenseite geladen, auch zurück | Paar im selben Chunk; Ticket für Keller/S3 | – |
| Rahmen L/R/oben, Doppeltür | FrameRule, Doppeltür = ein Portal | – |
| Symbol je Ziel, Rückweg merken | 10 Symbole + Farbbänder, Rückseite zeigt eigenes Ziel, Wegbuch, Wegkarte | E8-Symbol am schwächsten → Tür leuchtet nur intakt |
| Portal-System in SimpleLib | `com.simplelib.api.portal` | – |
| Regeln je Ebene (1 zweiwegig, 2 aktivieren, ab 3 bricht) | Kern §5.3 + Kasten | g3 (welche Ebene zählt) offen |
| Items je Schicht, oben einfach, tief einzigartig | §6 | – |
| Oberwelt-Türen extrem schwer, kein Easy-Travel | Kern 1×/Spieler, Vanilla-Echo-Splitter, verankert, ≤ 1024, nur Spieler | g9, g10, g15, h2, h5 |
| Grundgewölbe eigene Mod, alte Dimensionen ersetzen | Kern §7 | – |

**Bewertung G-R2:** SG 4,5 · MB 4 · PF 4 · A12 4,5 · KO 4,5 · BA 4,5.

### G-R3 – Schlussprüfung (Fragenliste, Doppelungen, Umsetzbarkeit)
- Fragen dedupliziert: g8 (Gefahr) ist von allen Ebenen als „a“ angenommen und bleibt nur Bestätigung; g6 (Keller-Kulisse) und k3 (Tiefenfenster) betreffen
  verschiedene Türen (Dimensionswechsel vs. Stapel) → beide bleiben; g13 (Shaderpack) ist durch Kern-Rückfallkette (Kulisse statt Schleier) präzisiert →
  Empfehlung jetzt „Kulisse“. Neue Fragen nur, wo eine echte Besitzer-Entscheidung entsteht (g15, g16); alles Technische (Wendel, Rampen, Licht B′,
  Pakete nach Thema) ist begründet entschieden.
- Umsetzbarkeit: neue Kern-Bausteine (K-R9) sind API, gehören vor den API-Freeze in K2 (Kern-Plan §8 angepasst: K2 größer, Wellen unverändert).
- Ehrlich offen bis S0: Speicher, Nebel/Licht je Biom, Flächenzahlen der Großbauwerke, Weizen-Quads, Hörbarkeit.
**Bewertung G-R3 (Endstand):** SG 4,5 · MB 4 · PF 4 (S0) · A12 4,5 · KO 5 · BA 4,5.

---

## 3. Konsolidierte Korrekturen (gelten vor den Einzel-Dokumenten)
1. **Wendeltreppen eckig** (gerade Läufe ≤ 8 + Eckpodest) in E3, E8, E9, E10 – keine Voxel-Kreisstufen.
2. **Leiter-Austritt 3 Luft**, **Rampen** aus ½-Stufen (≤ 1:2), **Kriechgang 1,5 hoch** (E5; E6-Kabelkanal 2×2 bleibt begehbarer Kanal).
3. **Decke-als-Himmel-Regel**: Decke ≥ Nebelende + 8 über jedem begehbaren Freiluft-Augpunkt; E10 Nebel 28/64; E9 Glockenstube geschlossen.
4. **E10 Licht**: A Biom-Umgebungslicht (S0), Rückfall B′ leuchtende Erde im 4×4-Raster; Feld aus **Abendweizen** (nicht tickend), Laub persistent.
5. **Wartungskasten** repariert/aktiviert direkt (kein Item), nur an aktivier-/brechenden Türen; Aktivierung 2↔3 von beiden Seiten mit dem Teil der eigenen Ebene.
6. **Manometer** = Füllstand, **Phasenprüfer** = Signal; **Leuchtstab** ohne Rezept.
7. **Oberwelt-Tür**: Paar verankert, Umsetzen kostet Sicherung + Türschließer (g15); 2 Vanilla-Echo-Splitter statt Brunnen-Fund (h2 c).
8. **Pakete/Namespaces nach Thema**, Position nur in `Bands.ORDER` (g1 spät entscheidbar).
9. Antwortcode-Fragen mit Vorsatz **f**; EBENEN-A-Fragen nach der Liste (e1 Schleuse, e2 Hubkette, e3 Octet-Formen, e4 E1-Abwärtstüren, e5 Wasser-Rücklauf).

## 4. Performance-Budget gesamt (Schätzung, S0 misst)

| Ebene | Erzeugung je Ebenen-Chunk | Lichtquellen/Chunk | Beute-Behälter/Chunk | größte Fläche (Großbauwerk) | Nebelende (Sicht) | Risiko |
|---|---|---|---|---|---|---|
| 1 Lobby | ≤ 0,3 ms | ≤ 60 | ≤ 8 | ≈ 50 k Säulensaal | 40 | gering |
| 2 Poolrooms | ≤ 0,5 ms | ≤ 60 | ≤ 8 | 6 k Wasserfläche + Halle | 120 | Wasser transluzent, weite Sicht |
| 3 Archive | ≤ 0,4 ms | ≤ 80 | ≤ 12 | 25–35 k Rotunde | 56 | Regalflächen |
| 4 Lagerhalle | ≤ 0,4 ms | ≤ 40 | ≤ 24 | 40–60 k Hochregal | 96 | **hoch** (Ausweg 4 Regalebenen) |
| 5 Rohrgänge | ≤ 0,3 ms | ≤ 30 | ≤ 8 | klein | 28 | gering |
| 6 Elektrostation | ≤ 0,3 ms | ≤ 60 | ≤ 8 | 25–35 k Turbinenhalle | 64 | mittel (Ausweg 2 Turbinen) |
| 7 Hotel | ≤ 0,3 ms | ≤ 70 | ≤ 6 | 18–22 k Atrium | 56 | mittel |
| 8 Lichtlos | ≤ 0,3 ms | ≤ 20 (+ ≤ 64 Leuchtstäbe) | ≤ 8 | ≈ 12 k Brunnen | 20 | sehr gering |
| 9 Vorstadt | ≤ 0,5 ms | ≤ 52 | ≤ 24 | Straßen offen, Vollblöcke | 64 | mittel |
| 10 Weizenfeld | ≤ 0,4 ms | 0 (A) / 16 (B′) | ≤ 4 | ≈ 80 k Weizen-Quads (50 %) | 64 | **hoch** (Ausweg Weizen 30 %) |
| **Säule gesamt** | **≤ 3,7 ms** (Kern-Grenze 10 ms) | ≤ 472 (nur Blocklicht, kein Himmelslicht → billiger als Oberwelt) | ≤ 110 (Kern-Test ≤ 120) | gezeichnet wird nur die eigene Ebene | – | Speicher 1,5–2× Oberwelt (S0, k2) |

Regeln: jede Ebene einzeln im Budget-Test `bands_gen_budget`; Sichtbares zählt nur je Ebene (Trennsektion); Wasser nur ohne Fließ; Pflanzen nicht
tickend; Ton-Emitter mit Reichweite kleiner als der Abstand zu fremden Ebenen.

## 5. Teile-Bilanz Rückweg (Endstand)

| Paar | Regel | Teil auf der oberen Seite | Teil auf der unteren Seite | Kasten |
|---|---|---|---|---|
| 1↔2 | zweiwegig | – | – | keiner |
| 2↔3 | aktivieren (einmal, dauerhaft) | Schwimmerventil | Schrankschloss | je Ende 1× Aktivierung |
| 3↔4 … 9↔10 | bricht (je Durchgang) | Teil der oberen Ebene | Teil der unteren Ebene | je Ende 1× Reparatur |

Erste Rückreise E10 → E3: 7 Reparaturen = 7 Kästen. Jede weitere Durchquerung desselben Paars: 1 Teil aus der Beute (Ziel ≤ 3 Bauplätze, Test).
Festsitzen unmöglich: jedes noch nicht benutzte Paar startet OFFEN (ab 3↔4) bzw. hat seinen eigenen Kasten (2↔3); Ausgänge Scheunentor + Lobby-EXIT +
Hintertür-Schlüssel + Vanilla-Tod. Teile sind damit **Zeitersparnis für den bekannten Rückweg**, nicht Pflicht.

## 6. Item-Gefälle 1→10 (Endstand)

| Ebene | Art | Items (Zahl) | höchste Stufe | Ersatzteil | Oberwelt-Wirkung (A12) |
|---|---|---|---|---|---|
| 1 | Bau, Licht, Werkzeug | 5 | gewöhnlich | – | Zierblöcke, Kreide |
| 2 | Verbrauch, Bad | 4 | ungewöhnlich | Schwimmerventil | Chlortablette (entfärben) |
| 3 | Information | 5 | selten | Schrankschloss | Stempel (Bauteil), Aktenregal |
| 4 | Mechanik | 5–6 | selten (Hubkette, e2) | Kistenbeschlag | Gegengewicht, Hubkette |
| 5 | Steuerung | 4 | selten | Dichtung | Ventilrad (Signal 0–15), Manometer |
| 6 | Messung | 4 | selten | Sicherung | Phasenprüfer; Sicherung = Bauteil |
| 7 | Zugang | 5 | selten | Türschließer | Türschließer = Bauteil |
| 8 | Licht, Orientierung | 5 | episch | Leuchtstab | Leuchtstab (Licht 8, 10 min), Laterne nur in Backrooms mit Wegfaden |
| 9 | Alltag, Behälter | 5 | selten | Hausschlüssel | Briefkasten, Schlüsselbund |
| 10 | Ziel | 5 | episch | Erntegarn | **Schwellenkern** (1 Paar) |
Schwellentür-Paar = Schwellenkern (E10) + Sicherung (E6) + Ventilrad (E5) + Stempel (E3) + Türschließer (E7) + 2 Vanilla-Echo-Splitter (h2 c).

## 7. Reihenfolge – Besitzer-Wahl g1

| | (a) R9 – Empfehlung | (b) A-Liste |
|---|---|---|
| Reihenfolge | Lobby · Poolrooms · Archive · Lagerhalle · Rohrgänge · Elektrostation · Hotel · Lichtlos · Vorstadt · Weizenfeld | Lobby · Lagerhalle · Rohrgänge · Elektrostation · Archive · Hotel · Poolrooms · Lichtlos · Vorstadt · Weizenfeld |
| Für | Besitzer-Wortlaut „Lobby, darunter Poolrooms, darunter Archive“; stärkstes Schaubild früh; Wegkarte (Archive) genau ab der ersten brechenden Tür | A-Vorschlag; Poolrooms als tiefes „Belohnungs“-Bild; Industrie-Ebenen als zusammenhängender Block |
| Gegen | Industrie-Block (4–6) kommt erst nach den Archiven | Wegkarte erst ab Ebene 5 (zwei brechende Ebenen ohne Karte); Rohrgänge (eng, echtes Labyrinth) schon als Ebene 3 |
| Folgen bei (b) | – | Labyrinth-Zielwerte gehen mit der Position, nicht mit dem Thema (Rohrgänge Ø 142 statt 269 usw. → Parameter je Ebene neu), Ersatzteil-Rollen wandern (Ebene 2 aktiviert mit Kistenbeschlag), Nebel/Items bleiben; Kern unberührt (Pakete nach Thema, §3.8) |

## 8. Konsolidierte offene Fragen (Antwortcode `f…`, Empfehlung zuerst)

| Code | Frage | Optionen (Empfehlung **fett**) | Quelle |
|---|---|---|---|
| fk1 | Raster je Ebene | **a 112 = 96 offen + 16 Grundgestein** · b 128 (112 offen) · c exakt 100 (Nachbarebenen schimmern durch) | Kern |
| fk2 | Bandfenster bei Sichtweite > 16 automatisch, falls Messung es verlangt | **a ja** · b nie | Kern |
| fk3 | Tiefenfenster (S2) Standard an, mit Shaderpack Kulisse | **a ja** · b nur Kulisse | Kern |
| fe1 | Poolrooms-Schleuse | **a optional (Beute)** · b Pflicht (Tür am Beckenboden) | Ebenen A |
| fe2 | Hubkette (E4) | **a aufnehmen, 1 Block Hub** · b weglassen | Ebenen A |
| fe3 | Pool-Formen Platte/Stufe | **a über Simple Octets** · b eigene Formen | Ebenen A |
| fe4 | E1 Abwärts-Türen seltener (Ø ≈ 80 Bl.) | **a ja** · b jede Kreuzung (Ø 57) | Ebenen A |
| fe5 | Schleusen-Wasser läuft nach 5 min zurück | **a ja** · b bleibt leer | Ebenen A |
| fh1 | Ziel von Scheunentor/Lobby-EXIT | **a Respawn-Punkt** · b Eingangshaus-Keller | Ebenen B |
| fh2 | Echo-Splitter für die Schwellentür | a 2 je Brunnen (E8) · b Amethyst · **c Vanilla-Echo-Splitter (Antike Stätte)** (G-R1 neu) | Ebenen B |
| fh3 | Erntegarn als E10-Ersatzteil | **a ja** · b anderes | Ebenen B |
| fh4 | Hotel-Aufzug-Folge | **a optionale Abkürzung** · b Pflicht | Ebenen B |
| fh5 | Schwellenkern | **a einmal je Spieler und Welt** · b 1 von 4 Gehöften, unbegrenzt | Ebenen B |
| fh6 | E8-Dunkelheit | **a Nebel Ende 20 fest** · b Config `layers.darkness` | Ebenen B |
| fg1 | Reihenfolge | **a R9** · b A-Liste (§7) | Grundkonzept |
| fg2 | Raster gemischt bei Speicher-Problem | **a 112 überall, gemischt nur falls S0 es verlangt** · b gleich gemischt (Höhe 944) | Grundkonzept |
| fg3 | welche Ebene bestimmt die Tür-Regel eines Paars | **a obere** (2↔3 aktiviert) · b untere (2↔3 bricht) | Grundkonzept |
| fg4 | Aktivierung | **a Ersatzteil einmal** · b dauerhaft offen nach erstem Durchgang | Grundkonzept |
| fg5 | Reparatur | **a Teil der eigenen Ebene + Kasten** · b immer Teil der oberen Ebene | Grundkonzept |
| fg6 | Keller-Doppeltür | **a Kulisse + Nebel-Übergang** · b normaler Ladebildschirm | Grundkonzept |
| fg7 | Ausgänge | **a Lobby-EXIT + Scheune + Schlüssel** · b nur Scheune | Grundkonzept |
| fg8 | Gefahr | **a keine Gegner/kein Schaden** · b seltene harmlose Erscheinungen · c echte Gegner | Grundkonzept |
| fg9 | Oberwelt-Tür Reichweite | **a ≤ 1024** · b ≤ 256 · c unbegrenzt | Grundkonzept |
| fg10 | Durchlass Oberwelt-Tür | **a nur Spieler** · b auch Tiere/Items | Grundkonzept |
| fg11 | Riss / fehlerhaftes Grundgestein | **a ganz entfernen** · b als Deko-Block behalten | Grundkonzept |
| fg12 | Stufenplan | **a S0 + K1–K6 + erste Ebenen-Welle, S2 nach Abnahme** · b alles gleichzeitig | Grundkonzept |
| fg13 | Durchsicht bei Shaderpack | **a Kulisse** · b Schleier | Grundkonzept (präzisiert) |
| fg14 | Tür-Kennzeichnung | **a Farbband + Symbol (10 Entwürfe)** · b nur Farbe | Grundkonzept |
| fg15 | Oberwelt-Tür umsetzen | **a verankert, Umsetzen kostet Sicherung + Türschließer** · b frei umsetzen · c nie umsetzbar | G-R1 neu |
| fg16 | Wartungskasten | **a repariert/aktiviert direkt, kein Item** · b gibt ein Teil heraus | G-R1 neu |

## 9. Nächste Schritte (nach Besitzer-OK)
Kern-Plan §8 (S0 → K1/K2 inkl. K-R9-Bausteine → API-Freeze → K3–K7) und danach je Ebene ein Branch `b-back22-<thema>` auf `b-back22-kern`, Wellen ≤ 4
Agenten (Welle 1 Lobby/Poolrooms/Archive/Rohrgänge, Welle 2 Lagerhalle/Elektrostation/Hotel nach K7, Welle 3 Lichtlos/Vorstadt/Weizenfeld); je Ebene ≥ 5
Runden nach dem Bau (Besitzer-Vorgabe), Texturen je Ebene in A8c-Runden. Bis zur Antwort **STOPP**.
