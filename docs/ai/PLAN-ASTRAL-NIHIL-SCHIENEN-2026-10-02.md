# Plan: Astral- und Nihil-Schienen (Besitzer-Queue Nachtrag 8, umgesetzt 2026-10-04)

Branch `claude-rails` (ab 138b0cc4). Wunsch: „Astral- und Nihil-Schienen, die stoppen oder beschleunigen; dann die
Höchstgeschwindigkeit hochsetzen, dass es sich annähert bzw. wie im realen Leben mit der Reibung verhält – je schneller,
desto mehr Boost braucht man. Die Astral-Schienen sollen mehr Boost geben als Redstone(-Antriebsschienen).“

## Ist-Zustand (erhoben)
- Es gibt **keine** End-Schienen. End-Signal-System (`McVersion.END_SYSTEMS`): `EndSignalBlock` (Pulver/Schalter/Lampe/
  Kolben), zwei private horizontale Kanäle, Polling-Tick alle 2 Ticks, kein Vanilla-Redstone. `EndPistonBlock` ist das
  Muster für einen Empfänger (Kommit a326ebb1 zeigt alle Berührungspunkte: Registrierung, Tab-Zeile, Suchtab, JEI, Guide,
  Testzentrale, Datagen, Lang, Wiki, GameTests, Config + RecipeFilter).
- Vanilla 26.3 (dekompiliert aus `minecraft-merged.jar`):
  - `OldMinecartBehavior` (Standard): Höchstgeschwindigkeit fest 0,4 Bl./Tick (8 Bl./s), im Wasser 0,2; die Bewegung je
    Tick wird pro Achse auf `getMaxSpeed` geklemmt (mit Fahrgast vorher ×0,75), der Schwung (`deltaMovement`) selbst nicht.
    Antriebsschiene: +0,06 je Tick; aus: halbiert, < 0,03 → Stopp. Reibung `applyNaturalSlowdown` ×0,997 (besetzt) bzw.
    ×0,96 (leer).
  - `NewMinecartBehavior` (experimentelles Feature `minecart_improvements`): Höchstgeschwindigkeit = Spielregel
    `max_minecart_speed` (1..1000 Bl./s, Standard 8), klemmt den Schwung im ersten Schritt, fährt in Teilschritten.
  - Antriebsschiene ist hart codiert (`state.is(Blocks.POWERED_RAIL)`, NeoForge/Forge: `instanceof PoweredRailBlock`).
    Eine eigene Schiene, die von `BaseRailBlock` (nicht `PoweredRailBlock`) erbt, bekommt also auf keinem Loader
    Vanilla-Boost/-Halt – die Mod rechnet selbst.
  - Forge 26.3 ruft im alten Verhalten ebenfalls `minecart.getMaxSpeed(level)`; NeoForge patcht `getMaxSpeed` nicht.
  - Dieselben Signaturen gibt es in 26.2 (javap geprüft) → Mixins können in `common/src/shared` liegen.

## Entscheidungen (selbst getroffen, begründet)
1. **Speisung über den eigenen Kanal** (Astral-Schiene ← Astral-Redstone/-Schalter, Nihil-Schiene ← Nihil-…), wie Lampe
   und Kolben: nur waagerechte Nachbarn, kein Vanilla-Redstone, keine Weitergabe von Schiene zu Schiene (eine Pulverspur
   neben dem Gleis versorgt jedes Stück; 15 Segmente je Schalter). Grund: steuerbar (Bahnhof, Abzweig), passt ins
   End-System; „immer aktiv“ wäre der Antriebsschiene in allem überlegen.
2. **Astral-Schiene**: ohne Signal eine normale Schiene mit **angehobener Höchstgeschwindigkeit** (Schwung bleibt erhalten,
   bremst nicht wie die ungespeiste Antriebsschiene); mit Signal Beschleunigung
   `a = boost · (1 − (v / vmax)²)` je Tick. Der Boost wird intern auf höchstens `vmax / 4` begrenzt (Umsetzung: erst `/2`, im Test fiel
   die Gleitkomma-Rundung an der Grenze auf vmax – Abweichung bewusst); damit ist `v ↦ v + a` auf [0, vmax] streng steigend und `v` erreicht vmax nie (asymptotisch). Je schneller, desto weniger
   bringt jedes Stück – „man braucht mehr Boost“. Vanilla-Reibung wirkt weiter (leere Wagen halten ein Gleichgewicht
   unter vmax). Schwung über vmax (etwa aus Vanilla-Antriebsschienen) wird auf der Astral-Schiene auf vmax gedeckelt.
   Stillstand + Signal: Anstoß weg von einem festen Block am Gleisende (wie die Antriebsschiene).
3. **Nihil-Schiene**: ohne Signal normale Schiene; **mit Signal sanftes Bremsen bis Stopp und Halten**:
   `v ↦ 0,8 · v − brake`, unter 0,03 → 0. Grund: ein harter Stopp aus 16 Bl./s wirkt wie ein Teleport-Halt; so hält der
   Wagen aus voller Fahrt nach gut einem Block (≈ 5 Ticks), ähnlich weich wie Vanillas Halbieren. Ein Fahrgast kann wie
   bei Vanilla aus dem Stand anschieben.
4. **Höchstgeschwindigkeit**: nur auf **flachen** Astral-Schienen (nicht auf Steigungen, nicht im Wasser) gilt
   `max(Vanilla-Grenze, vmax)`; überall sonst bleibt Vanilla unverändert. Astral-/Nihil-Schienen sind wie die
   Antriebsschiene nur gerade (keine Kurven). Damit kann ein schneller Wagen nie mit erhöhter Grenze durch eine Kurve:
   auf der normalen Kurvenschiene gilt wieder 0,4. vmax ≤ 20 Bl./s = 1 Bl./Tick: das alte Verhalten überspringt dabei
   keine Zelle (höchstens 1 Block je Achse und Tick); 20 Bl./s liegt unter Elytra-Tempo, Chunks kommen mit.
   Experimentelles Verhalten: die Spielregel bleibt maßgeblich, wenn sie höher ist (wir senken nichts ab).
5. **Server-Config** (hart begrenzt, auch beim Zugriff): `features.endRails` (an; braucht `endSignals`; aus: Schienen
   wirken wie normale Schienen, Rezepte weg), `machines.astralRailMaxSpeed` (Bl./s, 8..20, Standard 16),
   `machines.astralRailBoost` (0,07..0,25 je Tick, Standard 0,12 – Untergrenze über Vanillas 0,06, also immer mehr
   Anstoß als die Antriebsschiene), `machines.nihilRailBrake` (0,02..0,4, Standard 0,08).
6. **Rezept** analog Antriebsschiene: `G G / GSG / GRG` → 6, G Goldbarren, S Stock, R Astral- bzw. Nihil-Redstone
   (der Astralit/Nihilit-Anteil steckt im Kanal-Redstone).
7. **Flag** `McVersion.END_RAILS` (26.3 true, 26.2 false); Registrierung nur mit Flag.

## Umsetzung (Dateien)
- Neu `blocks/custom/EndRailBlock.java` (BaseRailBlock, `shape` gerade + `powered` + `waterlogged`, Polling-Tick nur
  solange ein Kanal-Block daneben liegt, neu angestoßen über Nachbar-Updates), `blocks/custom/EndRailPhysics.java`
  (Formeln, Config-Zugriff mit Klemmen, Anwendung je Tick).
- `EndSignalBlock`: statische `receiverPower(astral, level, pos)`/`hasChannelNeighbour`; `sameChannel` zählt Schienen des
  Kanals mit (Pulver zeichnet die Verbindung zur Schiene).
- Mixins (shared, `simplebuilding.mixins.json`): `MinecartBehaviorEndRailMixin` (Old+New, HEAD von `moveAlongTrack`),
  `AbstractMinecartEndRailMixin` (RETURN von `getMaxSpeed`).
- `ModBlocks`/`ModItems` (Eigenschaften von `Blocks.POWERED_RAIL` kopiert), `ModItemGroupsContent` (Zeile `end_rails`
  nach `end_signals`), `SearchTabPlacement` (Redstone-Tab nach Antriebsschiene, Zweitplatz Werkzeuge & Hilfsmittel nach
  Antriebsschiene), `RecipelessJeiInfo` (Infoseite `end_rails`), `GuideContent` (End-Kapitel), `RecipeFilter`,
  `ConfigOptions.RECIPES_ON_RELOAD`, `ServerTuningConfig` (+validate), `TestCentreSections` (Teststrecke im Abschnitt
  „Weitere Geräte“).
- Datagen: Rezepte, Beute, Tags `minecraft:rails` (Block + Item), `mineable/pickaxe`.
- Assets (overlay 26.3, von Hand wie Kolben): Blockstates, Modelle (`rail_flat`, `template_rail_raised_ne/sw`), Items,
  Texturen aus `tools/textures/end_rails_2026_10_04.py` (Antriebsschiene als Basis, Gold → Kanal-Edelsteinrampe, rote
  Leitung → Kanal-Farbe; Vorschläge A/B/C, eingebaut A) + Vorschau `previews/astral-nihil-schienen-vorschau.png`.
- Lang EN/DE in `src/main/resources` und `mc26_3/overlay/resources`; Wiki-Eintrag `end_rails` in `wiki/manual.json`.

## Risiken
- Mixin auf private/neu geordnete Methoden: nur `moveAlongTrack` (public) und `getMaxSpeed` (protected) – auf allen drei
  Loadern vorhanden (Quellen geprüft).
- Polling: nur Schienen mit Kanal-Nachbar ticken (alle 2 Ticks, wie das Pulver).
- Bestehende Tests (Tab-Layout, Suchtab, Config-Liste, Testzentrale, Guide-Liste) müssen angepasst werden.

## Verifikation
- GameTests: Formel (steigt mit Boost, nähert sich vmax ohne Überschreiten, Gewinn sinkt mit v, Astral > 0,06),
  Config-Grenzen (999/−1 → geklemmt), Kanal (Astral-Pulver speist Astral-Schiene, nicht Nihil, Vanilla-Redstone nie),
  Fahrt: Wagen startet aus dem Stand und fährt schneller als 0,4 Bl./Tick (Mixin wirkt im echten Spiel), Nihil bremst
  bis Stopp, ohne Signal rollt er durch, schneller Wagen nimmt danach eine normale Kurve ohne Entgleisen.
- Gates: `run.py --targets fabric-263`, `neoforge-263`, Compile 26.2 + Forge 26.3, `gradlew check -q`, Wiki-Check,
  Textur-Generator `--check`.
- NICHT prüfbar: Sichtprüfung im Client, experimentelles Minecart-Verhalten (Feature-Flag im GameTest nicht schaltbar).
