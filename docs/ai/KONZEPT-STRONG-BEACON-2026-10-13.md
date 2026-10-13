# Konzept: Strong Upgrade, verstärktes Leuchtfeuer und verstärkter Conduit (2026-10-13)

Branch `claude-concepts50` (Basis `claude-wave1`). Nur Konzept, kein Code, nichts getestet. Queue-Punkt: „Netherstern + neue
Upgrade-Vorlage („Strong Upgrade“) auf Leuchtfeuer … dasselbe für Conduit soweit möglich“ (`.claude/QUEUE.md`, Konzept zuerst).
Grundlagen: `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` (Regeln 1–8, 5a Axt-Prinzip, 6a Libs), `docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md`
(Modulbaum, `simplelib:in_world_upgrades`), `docs/ai/PLAN-INWORLD-2026-10-10.md` (`InWorldStrikes` mit wachsender Vorschau).
Namen aus der Vanilla-Beacon-/Conduit-Logik stammen aus dem Gedächtnis zu 1.21.x und sind **vor dem Bau gegen die 26.3-Quellen zu prüfen**
(markiert mit „[prüfen]“).

## 1. Ist-Zustand

- **Vanilla-Leuchtfeuer:** Pyramide bis 4 Ebenen (3×3, 5×5, 7×7, 9×9 = 164 Blöcke), Basisblöcke aus Tag `minecraft:beacon_base_blocks`.
  Reichweite `Ebenen × 10 + 10` Blöcke (Ebene 4 = 50), Effektdauer `(9 + 2 × Ebenen)` s (Ebene 4 = 17 s), Himmel frei, Wirkung nur auf Spieler,
  senkrecht bis zur Bauhöhe. Effekte: Ebene 1 Tempo/Eile, Ebene 2 Resistenz/Sprungkraft, Ebene 3 Stärke, Ebene 4 sekundär Regeneration oder
  Primär auf Stufe II. Bezahlung: 1 Barren/Smaragd/Diamant aus dem Bezahl-Tag (`beacon_payment_items`) [prüfen]. Alles in
  `BeaconBlockEntity` **statisch** verdrahtet (Ebenen-Schleife bis 4, `BEACON_EFFECTS`, `VALID_EFFECTS`), Menü `BeaconMenu` (3 Datenwerte),
  Client `BeaconScreen`, Paket `ServerboundSetBeaconPacket` prüft die Wahl gegen die feste Liste.
- **Vanilla-Conduit:** 42 mögliche Rahmenblöcke (Prismarin, Prismarinziegel, Dunkelprismarin, Seelaterne) in einer 5×5×5-Schale: drei sich
  schneidende 5×5-Ringe (3 × 16 − 6 Achsenpunkte = 42). Aktiv ab 16 Rahmenblöcken, Reichweite `(Rahmenblöcke / 7) × 16` (max. 96 Blöcke), Wirkung
  Unterwasser-Kraft nur im Wasser/Regen; bei vollem Rahmen (42) greift der Conduit feindliche Mobs im Umkreis von 8 Blöcken an [prüfen].
- **SB heute:** Upgrade-Vorlagen `basic_upgrade_template`, `enderite_upgrade_template` (`ModItems`, laut Modulplan künftig `simplelib`).
  In-World-Aufwertung per Vorschlaghammer (Haupthand), Zusatzmaterial in der Nebenhand, Risse/Funken/steigender Ton/Vorschau über
  `com.simplelib.api.InWorldStrikes`; abgelegte Vorlagen (`PlacedTemplates`) werden mit dem Hammer aufgewertet. Ohne Hammer-Mod gilt die **Axt**
  (Regel 5a). `simpleinterfaces` stylt den Leuchtfeuer-Bildschirm bereits (`BeaconButtonMixin`, `BeaconBackgroundMixin`, `BeaconIconMixin`,
  `WorkScreens.beaconPalette`; Label „II“ am Stufe-II-Knopf).
- Vorhanden ist **kein** Code, der das Leuchtfeuer oder den Conduit verändert.

## 2. Die „Strong Upgrade“-Vorlage

**Name:** EN „Strong Upgrade Smithing Template“ (Id `strong_upgrade_template`), DE „Verstärkungs-Schmiedevorlage“ (siehe Frage 1: „verstärkt“ ist in SB
schon die Eisen-Stufe, „Reinforced“).

**Zugehörigkeit:** nicht nach `simplelib`. Einziger Nutzer ist das neue Modul (§6), also gehört die Vorlage ihm (`simplebeacons:strong_upgrade_template`).
Regel 6: Gemeinsames nur, wenn mehrere es brauchen; kommt ein zweiter Nutzer, zieht sie später per `IdMoves` nach `simplelib` um.

**Bezug (Vorschlag, Vanilla-Material, Regel 1):**
1. *Erste Vorlage:* formlos geformt (Schablone 3×3): Netherstern in der Mitte, 4 Diamanten an den Kanten, 4 Obsidian in den Ecken → 1 Vorlage.
   Wer die Basic-Vorlage von SB kennt, bekommt zusätzlich ein bedingtes Rezept (`basic_upgrade_template` statt der 4 Diamanten, Regel 4).
2. *Duplizieren* wie Vanilla: 7 Diamanten + 1 Obsidian + 1 Vorlage → 2 Vorlagen.
3. Optional (Frage 4): als seltener Fund in der End-Stadt-/Bastion-Truhe; Vorschlag: nein, der Netherstern ist die Hürde.

**Kosten gesamt:** erstes Leuchtfeuer = 2 Nethersterne (Vorlage + Veredlung), jedes weitere = 1 Netherstern + ½ Vorlage (7 Diamanten). Das ist bewusst
teurer als jedes bestehende Upgrade, weil die Pyramide (§3.4) und die Dauerwirkung dazukommen.

**Anwendung (zwei Wege, gleiches Ergebnis):**
- **A. Schmiedetisch (Item):** Basis `minecraft:beacon`, Vorlage, Zusatz `minecraft:nether_star` → Item `strong_beacon`. Vorteil: JEI/REI/Wiki zeigen es von
  selbst, funktioniert ohne Hammer, ohne In-World-Code. Gleiches Rezept für `minecraft:conduit` → `strong_conduit`.
- **B. In-World (Empfehlung als Hauptweg, passt zu allen anderen SB-Aufwertungen):** Vorschlaghammer (Axt ohne Hammer-Mod) in der Haupthand,
  Strong-Vorlage in der Nebenhand, Netherstern irgendwo im Inventar (wie die „Zusatzmaterialien aus dem Inventar“ der abgelegten Vorlage), Rechtsklick auf das
  **aufgebaute** Vanilla-Leuchtfeuer. N Schläge (Vorschlag 8) mit Rissen, Funken, steigendem Ton und der wachsenden Zielvorschau von
  `InWorldStrikes` (Ziel: `strong_beacon`-Block, 1,02 groß, von unten aufgedeckt). Beim letzten Schlag tauscht der Block **an Ort und Stelle**
  und übernimmt Wahl (Primär/Sekundär), Sperre (`Lock`) und eigenen Namen aus der Block-Entity; Vorlage und Netherstern werden verbraucht. Das Leuchtfeuer
  muss dafür nicht aktiv sein. Daten für den Weg kommen als Eintrag in den künftigen Datentyp `simplelib:in_world_upgrades`
  (`from: minecraft:beacon`, `to: simplebeacons:strong_beacon`, `material: strong_upgrade_template`, `extra: nether_star`, `strikes: 8`), kein neuer
  Sonderfall im Code.
- **Abgelegt:** Die Vorlage lässt sich wie jede Vorlage ablegen (Schleichen + Rechtsklick); dafür ist kein Zusatzcode nötig, sie wird nur in der Hand verwendet.

Was **nicht** geht: Rückweg zum Vanilla-Leuchtfeuer (kein Downgrade). Abbau (Spitzhacke/Hammer) gibt das `strong_beacon`-Item.

## 3. Verstärktes Leuchtfeuer („Strong Beacon“)

### 3.1 Regeln

| Eigenschaft | Vanilla | Strong |
|---|---|---|
| Ebenen | 1–4 (9, 25, 49, 81 Blöcke = 164) | 1–**6** (+11×11 = 121, +13×13 = 169; gesamt **454** Blöcke) |
| Basisblöcke | `#minecraft:beacon_base_blocks` | dasselbe Tag, plus eigenes Tag `#simplebeacons:strong_base_blocks` (leer, für Modpacks/Enderit; Frage 6) |
| Reichweite | `10 × E + 10` (E4 = 50) | gleiche Formel: E5 = **60**, E6 = **70** Blöcke; E1–E4 wie Vanilla |
| Effektdauer | `9 + 2E` s (E4 = 17) | gleiche Formel: E5 = 19 s, E6 = 21 s |
| Ziel | Spieler | Spieler (nicht Tiere/Mobs, Frage 9) |
| Bezahlung | 1 Barren/Smaragd/Diamant | gleiche Bezahlung (`#minecraft:beacon_payment_items`) |
| Strahl | Vanilla (Glasfärbung) | gleich; Strahlkern etwas breiter oder mit hellem Kern ist **kein** Teil v1 |

Ein Strong Beacon mit 4 Ebenen verhält sich genau wie ein Vanilla-Leuchtfeuer (gleiche Wahl, gleiche Reichweite). Der Mehrwert ist rein die
Pyramidenhöhe 5–6 und die zusätzlichen Effekte. Reichweite linear weiterzuführen (statt zu springen) hält die Formel erklärbar; 70 Blöcke sind
rund 4,4 Chunks Radius, ein Dorf-/Basis-Radius, kein ganzer Server.

Pyramidenprüfung: wie Vanilla alle 80 Ticks, Ebene i liegt `i` Blöcke unter dem Leuchtfeuer und ist `(2i+1)²` groß. Prüfung bricht an der ersten
unvollständigen Ebene ab; Ebenenzahl wird (wie Vanilla) im Block gespeichert und im Bildschirm angezeigt.

### 3.2 Die sechs neuen Effekte

Auswahlkriterien (aus `pixelart-feedback`/Besitzerlinie „nichts Ausnutzbares, Spaß oder kleiner Nutzen“, wie die Kopf-Fähigkeiten): kein neuer Kampfvorteil
über Vanilla-Stärke/Regeneration hinaus, kein Ersatz für Spielmechaniken (Essen, Schlaf), Dauerzustand ohne Missbrauch, in Vanilla als Statuseffekt
vorhanden (keine Eigenbauten).

| Kandidat | Urteil | Begründung / Balance |
|---|---|---|
| **Nachtsicht** | **ja, E5** | Komfort für Höhlen/Ozean; Tränke geben es ohnehin; kein Kampfvorteil. |
| **Wasseratmung** | **ja, E5** | Passt zur Reichweite (Unterwasserbasen); verdrängt den Conduit nicht, weil der nur im Wasser und mit Rahmen wirkt. |
| **Langsamer Fall** | **ja, E5** | Sicherheit für Flug-/Türmebauer; hebt nur Fallschaden auf, kein Elytra-Ersatz (Elytra braucht Starts). |
| **Feuerresistenz** | **ja, E6** | Nether-Basis; löst Lava-/Feuerprobleme, aber keine Explosions- oder Wither-Schäden. Sehr wertvoll → hoch eingestuft. |
| **Delfingunst** | **ja, E6** | Schnelles Schwimmen, Spaß, kein Kampf. |
| **Absorption (nur Stufe I)** | **ja, E6** | 2 Herzen Puffer; wird beim Wiederauffrischen der Dauer nicht neu aufgefüllt, ist also ein einmaliger kleiner Puffer pro Aufenthalt. Keine Stufe II (sonst Kampfvorteil). |
| Glück | Reserve | Wirkt nur auf Angeln/Loot-Tabellen mit Glückswert; schwer erklärbar, Serverbetreiber könnten es einzeln wollen. Als Ersatz für Delfingunst denkbar (Frage 2). |
| Sättigung | **nein** | Hebt Hunger auf; macht Landwirtschaft/Essen überflüssig. |
| Gesundheitsschub | **nein** | +10 HP Dauerzustand, zu stark. |
| Unsichtbarkeit | nein | Reißt PvP/Mob-Balance (Deceiver-Tarnung ist eigenes Thema). |
| Leuchten | nein | Verrät Spieler, kein Nutzen. |
| Held des Dorfes | nein | Ausnutzbare Handelspreise. |
| Wächter-Eigeneffekte (Unterwasser-Kraft) | nein | Gehört dem Conduit (§4). |

Zuordnung: **Ebene 5**: Nachtsicht, Wasseratmung, Langsamer Fall (Komfort). **Ebene 6**: Feuerresistenz, Delfingunst, Absorption I (die stärkeren, teuersten).
Sekundärwahl (Ebene 4+): Regeneration bleibt; „Stufe II“ ist nur für Tempo, Eile, Resistenz, Sprungkraft, Stärke und Absorption (bei Absorption II = 4 Herzen) zulässig —
Frage 3: Absorption II erlauben? Empfehlung: nein, Knopf ausgegraut.

Alle Effekte wirken Ambient (kein Partikelrauch), wie Vanilla. Server-Konfig: je Effekt ein Schalter (`strongBeaconEffects.<id>`), Reichweiten-Grundwert
und Mehrwert je Ebene (Standard 10), `strongBeaconMaxLevels` (4–6, Standard 6), Gesamtschalter `enableStrongBeacon` (nur Spielstellen aus, Registrierung bleibt, Prinzip aus §2.1 Modulplan).

### 3.3 Oberfläche

Vanilla-Bildschirm (≈ 230 × 219 px): drei Zeilen Primär (Stufen 1–3), Sekundärspalte, Bestätigen/Abbrechen, Bezahl-Slot. Sechs Ebenen passen **nicht** ohne Weiteres in die
Höhe (219 + 2 Zeilen ≈ 270 px > GUI-Höhe 270 bei Maßstab 4 auf 1080p). Vorschlag:

- **Eigener Bildschirm `StrongBeaconScreen`** (Unterklasse des Vanilla-Bildschirms, damit die Mixins von `simpleinterfaces` unverändert greifen [prüfen: die inneren Knopfklassen sind
  privat, ggf. `@Accessor`/`@Invoker`]) mit **zwei Spalten**: links der unveränderte Vanilla-Teil (Ebenen 1–4), rechts ein Zusatzfeld (≈ 90 px breit) mit den **Zeilen
  Ebene 5 (3 Knöpfe)** und **Ebene 6 (3 Knöpfe)**. Gesamtbreite ≈ 320 px, passt bei Maßstab 4 auf 1080p genau (480 px), bei Maßstab 3 bequem.
- Über dem Zusatzfeld ein Pyramiden-Zähler („Pyramide 6/6“, Piktogramm aus Stufen) und darunter eine Textzeile „Reichweite 70 Blöcke, 21 s“. Gesperrte Ebenen (Pyramide
  zu klein) sind ausgegraut mit Tooltip „Braucht Pyramide Ebene 5 (285 Blöcke)“.
- Ebene-Knöpfe nutzen die Vanilla-Effekt-Icons; Tooltip: Name, Kurzzeile („Nachtsicht – sieht im Dunkeln“), kein Fettdruck (Besitzerregel aus dem Resonanz-Tooltip).
- Eigener Menütyp `strong_beacon` mit 5 Datenwerten (Ebenen, Primär, Sekundär, + Platz für Erweiterungen), eigenes Setz-Paket mit Prüfung auf Server (nie dem Client trauen);
  das Vanilla-Paket prüft gegen die feste Liste und lehnt neue Effekte ab.
- Geringster Aufwand als Rückfall (Frage 8): zweizeiliges Blättern („Seite 1/2“) im Vanilla-Maß. Nicht empfohlen, weil man Effekte nicht nebeneinander sieht.

### 3.4 Technische Machbarkeit auf 26.3

**Variante M (Mixins in den Vanilla-Block):** `BeaconBlockEntity.updateBase` (statisch, kennt den Block-Entity nicht), die Ebenen-Konstante, `BEACON_EFFECTS`, `applyEffects`,
`BeaconMenu`, `BeaconScreen`, `ServerboundSetBeaconPacket`. Müsste **jedes** Leuchtfeuer betreffen oder über den Block-Zustand an `(x,y,z)` unterscheiden. Probleme: viele
Zugriffspunkte (7–9 Mixins × 3 Loader), Konflikte mit anderen Leuchtfeuer-Mods und mit den vorhandenen `simpleinterfaces`-Mixins, jede Vanilla-Änderung bricht es,
Vanilla-Leuchtfeuer bekäme versehentlich 6 Ebenen (Prinzip: „Upgrade“ soll etwas **kosten**).

**Variante B (eigener Block, Empfehlung):** `StrongBeaconBlock` (Unterklasse von `BeaconBlock` oder eigener `BaseEntityBlock`) + `StrongBeaconBlockEntity` mit nachgebauter Logik
(Pyramidenscan mit Parameter „max. Ebenen“, Effektliste, Reichweite). Für den Strahl **die Vanilla-Beam-Logik wiederverwenden**: seit 1.21.x teilen Leuchtfeuer und Testblock
das Interface `BeaconBeamOwner` und einen generischen Renderer [prüfen, ob in 26.3 vorhanden]; wenn ja, genügt das Interface plus Renderer-Registrierung für den neuen
Block-Entity-Typ. Sonst Kopie des Renderers (klein). Vorteile: kein Eingriff in Vanilla, nur Spielstellen mit dem Strong-Block ändern sich, vorhandene Welten ohne Strong-Blöcke
unberührt, Modul sauber abschaltbar (Regel: Spielstelle aus, Registrierung bleibt), drei Loader nutzen den gleichen Shared-Code. Nachteil: einige Hundert Zeilen Nachbau
(Scan, Effekt, Menü, Paket, Bildschirm).

**Empfehlung B.** Mixins nur dort, wo sie billig sind: ein Mixin in `BeaconBlockEntity`/`BeaconMenu` ist **nicht** vorgesehen; Kompatibilität zu Vanilla-Beacon-Mods bleibt ohnehin erhalten.

Weitere Punkte:
- **Block-Entity-Daten kompatibel zu Vanilla** (gleiche Schlüssel für Primär/Sekundär/`Lock`/Name), damit das Austauschen an Ort und Stelle (§2 B) nur den Typ wechselt.
- **Texturen:** das Strong-Leuchtfeuer-Modell/Item braucht eine eigene Optik (Vorschlag: Obsidian-Käfig mit Netherstern-Glühen). Jede Textur, die ein Mojang-Sprite umfärbt, wird
  **nicht eingecheckt**, sondern über `tools/textures/vanilla_derived.py` beim Build erzeugt (Besitzerregel 12.10.); eigene Formen (Käfig, Ornamente) sind dagegen eigene Pixel-Art im Vanilla-Stil
  (Besitzerregel: eigene Formen statt Recolors).
- **Strahl-Optik:** Vanilla-Strahl; optional eine zweite hellere Ringschicht bei Ebene 5–6 (Frage 7) — nicht Teil von v1.
- **Server/Spieler-Kontrolle:** Wirkung nur auf Spieler im gleichen Level, nicht in der Reichweite durch Chunk-Grenzen hinaus unbegrenzt (Chunk muss geladen sein, wie Vanilla).
- **26.2 / 1.21.11:** nur 26.3. Port erst im Port-Run (Leuchtfeuer-Internes weicht ab).

### 3.5 Tests (Plan)

Gametests (Fabric-Wrapper + Katalog): Pyramide Ebene 1–6 erkannt, fehlender Block bricht ab; Reichweiten-/Dauerformel je Ebene; Effekte pro Ebene nur ab ihrer Ebene wählbar; Server lehnt
Fremd-Effekt/zu hohe Ebene im Setz-Paket ab; Austausch an Ort: Daten bleiben erhalten, Vorlage + Netherstern verbraucht; Schmiedetisch-Rezept; Axt-Weg ohne Hammer-Mod; Standalone-Target
(`simplebeacons` + Vanilla) grün; JUnit-Layouttest für den neuen Bildschirm (Zusatzfeld überlappt nichts, Maßstab 3/4). Client-Screenshot-Lauf (`client-fabric-263`) für Bildschirm und Strahl.

## 4. Verstärkter Conduit („Strong Conduit“) – soweit möglich

**Machbar, aber als eigener Block** (`ConduitBlockEntity` hat statisch die 42er-Schale und Wirkformel; Mixins wären noch fragiler als beim Leuchtfeuer).

| Eigenschaft | Vanilla | Strong |
|---|---|---|
| Rahmen | 5×5×5-Schale, max. 42 Blöcke | akzeptiert **zusätzlich** eine 7×7×7-Schale (drei 7×7-Ringe: 3 × 24 − 6 = **66** Blöcke) |
| Aktivierung | ab 16 Rahmenblöcken | ab 16 (wie Vanilla) |
| Reichweite | `(Blöcke / 7) × 16`, max. 96 | gleiche Formel: 66 Blöcke → **144** (9 × 16); mit kleinem 42er-Rahmen weiterhin 96 |
| Angriff auf Feinde | volle 42er-Schale: Umkreis 8 Blöcke | volle 66er-Schale: Umkreis **12** Blöcke, gleicher Schaden |
| Zusatzeffekt | Unterwasser-Kraft (Atmung, Eile, Sicht) | Unterwasser-Kraft **plus Delfingunst** (Spieler im Wasser, innerhalb der Reichweite) |
| Mindest-Wasser | Conduit unter Wasser | gleich |

Begründung: „größerer Rahmen, mehr Reichweite“ ist die direkte Übertragung der Leuchtfeuer-Idee. Die Schale ist leicht verallgemeinerbar (die Regel „eine Koordinate 0, eine mit Betrag r“ gilt für beliebiges r;
r = 3). 144 Blöcke Reichweite sind groß, wirken aber nur unter Wasser und für Spieler im Wasser; Server können in der Konfig auf 96 deckeln (Frage 5). **Delfingunst** ist der einzige Zusatzeffekt, weil der Conduit
Atmung/Eile/Sicht schon bündelt; keine Kampfwerte. **Nicht möglich/ausgelassen:** Variante mit mehr als einer Schale (verschachtelt), Mob-Angriff mit höherem Schaden (Balance), Land-Wirkung.
Rendern: Vanilla-Conduit-Renderer ist an `ConduitBlockEntity` gebunden [prüfen] → Kopie mit Farbtönung (Käfig golden/netheritfarben, abgeleitet aus der Vanilla-Textur beim Build).
Anwendung: Schmiedetisch- und In-World-Weg wie §2, Basis `minecraft:conduit`, Zusatz `minecraft:nether_star` (Frage 1: alternativ Herz des Meeres als Zusatz, thematisch näher).
Mindestumfang wenn der Aufwand zu groß ist: nur Rahmen 7×7×7 und Reichweite, ohne neuen Renderer (Vanilla-Optik) — Frage 5.

## 5. Zusammenspiel und Modulgrenzen

- **simpleinterfaces:** stylt `BeaconScreen` per Palette; `StrongBeaconScreen` als Unterklasse erbt das, solange `WorkScreens.beaconPalette(screen)` `instanceof BeaconScreen` prüft (tut es). Keine Pflicht,
  das neue Modul steht allein (Vanilla-Optik).
- **simplehammers:** nur über Tag `#simplehammers:sledgehammers` (bedingt, Regel 4); ohne Hammer die Axt (Regel 5a). Kein Klassenimport.
- **simplemoney:** Preise/Handelslinks bedingt (`links:simplebeacons`).
- **Wiki/Guide:** Kapitel im Modul-Wiki „Strong Beacon“: Bezug, Pyramide (mit Bild 6-Ebenen-Pyramide), Effekttabelle, Reichweiten; Guide-Buch-Seite; `Obtain`-Quellen für Vorlage.
- **Migration:** Vanilla-Leuchtfeuer/-Conduits in Welten bleiben unangetastet; neue Ids `simplebeacons:strong_beacon`, `simplebeacons:strong_conduit`, `simplebeacons:strong_upgrade_template`.
  Block-Entity-Typ neu (`simplebeacons:strong_beacon`). `IdMoves` nur nötig, falls das Modul später den Namensraum wechselt. Entfernt jemand das Modul, gehen Strong-Blöcke verloren (unbekannte Blöcke);
  deshalb im Bündel gebündelt und Abschalten nur über Spielstellen-Schalter.

## 6. Modul-Zugehörigkeit (Vorschlag)

Neues Sub-Mod **Simple Beacons** (`simplebeacons`, Namespace `simplebeacons:`), enthält Strong-Vorlage, Strong-Leuchtfeuer, Strong-Conduit, Bildschirm, Konfig. Hart nur `simplelib` (InWorldStrikes, gebündelt) und
`framework` (gebündelt), Cloth Config. In **SimpleBuilding optional gebündelt** (Schalter „Enable Simple Beacons“ per `SubMods`, Vorbild QoL/Weather). Begründung: Der Kern schrumpft (Modulplan), ein Leuchtfeuer-Upgrade passt in keines der
fünf Pflicht-Sub-Mods (Octets, Hammers, Storage, Ovens, Crucibles), und es soll allein spielbar sein. Alternative: in `simpletweaks` (Frage 10) — dort liegen schon Pads/Teleporter, aber Tweaks hängt noch am Kern.

## 7. Ablauf (Stufen, jede gate-grün, Fabric zuerst + NeoForge/Forge im selben Zug)

1. Modulgerüst `simplebeacons` (modules.json, Loader-Metadaten, Standalone-Target, leeres Wiki-Kapitel, Konfig).
2. Vorlage + Strong-Leuchtfeuer-Block mit Pyramide 1–6, Effekte, Menü, Paket (ohne neue UI, Vanilla-Maß + Ebenen 5–6 als Textliste) + Schmiedetisch-Rezept + Tests.
3. Bildschirm (Zusatzfeld), Texturen (eigene Pixel-Art, Vorschau selbst ansehen, Besitzer-Abnahme), JUnit-Layout, Client-Screenshot.
4. In-World-Weg (Datentyp-Eintrag oder bis dahin Hilfsklasse), Axt-Weg, Tests.
5. Strong Conduit (Rahmen, Reichweite, Delfingunst, Renderer), Tests.
6. Wiki, Guide, Money-Preise, Testzentrale-Station.

## 8. Offene Besitzer-Fragen (mit Empfehlung)

1. **Name und Zusatz:** EN „Strong“ ist gesetzt; DE „verstärkt“ ist in SB die Eisen-Stufe („Reinforced“). Empfehlung: DE „Starkes Leuchtfeuer“ / „Starke Aufwertungsvorlage“, EN „Strong Beacon“. Zusatz beim Conduit:
   Netherstern (konsistent) oder Herz des Meeres (thematisch)? Empfehlung: Netherstern bei beiden, damit die Vorlage ein einheitliches Rezept hat.
2. **Die sechs Effekte** wie in §3.2 (E5: Nachtsicht, Wasseratmung, Langsamer Fall; E6: Feuerresistenz, Delfingunst, Absorption I)? Empfehlung: ja; falls Delfingunst zu spielerisch, durch Glück ersetzen.
3. **Absorption Stufe II / „Stufe II“ nur für bisherige Effekte?** Empfehlung: Absorption nur Stufe I; II-Knopf nur bei Tempo/Eile/Resistenz/Sprung/Stärke.
4. **Bezug der Vorlage:** Netherstern-Rezept wie in §2 oder zusätzlich als seltener Truhen-Fund? Empfehlung: nur Rezept (Netherstern ist die Hürde), Duplikation 7 Diamanten + Obsidian.
5. **Conduit:** Rahmen 7×7×7 (66 Blöcke), Reichweite 144, Angriff 12 Blöcke, Delfingunst — Reichweite deckeln auf 96? Empfehlung: 144 zulassen (nur unter Wasser), Konfig-Deckel; wenn der Aufwand zu groß ist zuerst nur Rahmen + Reichweite mit Vanilla-Optik.
6. **Basisblöcke Ebene 5–6:** wie Vanilla (Eisen, Gold, Smaragd, Diamant, Netherit) oder Kupfer/Enderit/Lapis zusätzlich? Empfehlung: Vanilla-Tag plus leeres `strong_base_blocks`-Tag (Enderit-Block per Datapack/Modpack einhängbar).
7. **Strahl** bei Ebene 5–6 optisch hervorheben (Zusatz-Ring/helleres Zentrum)? Empfehlung: nicht in v1; Texturen zuerst.
8. **Oberfläche:** Zusatzfeld rechts (Empfehlung) oder Blättern auf Seite 2?
9. **Wirkt das Strong-Leuchtfeuer auch auf zahme Tiere/Pferde** (Reichweite 70)? Empfehlung: nein, nur Spieler, wie Vanilla.
10. **Modulplatz:** neues Modul `simplebeacons` (Empfehlung, optional im SB-Bündel mit Schalter) oder in `simpletweaks`/Kern?
11. **Anzahl Hammer-Schläge** und ob die Vorlage beim Schmieden verbraucht wird (Vanilla: ja). Empfehlung: 8 Schläge, verbraucht.
12. **Reichweiten-Formel** linear (E5 60, E6 70) wie vorgeschlagen oder stärker (z. B. 80/100)? Empfehlung: linear, um den Server nicht zu überfahren; Wert je Ebene konfigurierbar.

## Besitzer-Entscheidung 13.10.
Alle Empfehlungen der offenen Fragen übernommen (Besitzer 13.10.). Umsetzung freigegeben.
