# Konzept: Mob-Animationen verbessern (Nachtrag 55, 2026-10-15)

Nur Recherche + Konzept, kein Code. Keine abgenommene Textur wird geändert; alle Maßnahmen sind Render-/Modell-Code (Posen über `PartPose`/`setupAnim`), keine neuen Cubes oder UV-Änderungen, außer wo ausdrücklich „Besitzerentscheid“ steht. Aufwand: **S** < 0,5 Tag, **M** ca. 1 Tag, **L** > 1 Tag. Hollow-Howler-Details stehen im Schwesterkonzept `KONZEPT-HOLLOW-HOWLER-GRUSEL-2026-10-15.md` (V1–V9) und werden hier nur verknüpft.

## 1. Vanilla-Richtlinien (aus den 26.3-Quellen gelesen)

Java Edition kennt **zwei** Animationswege, beide ohne Molang (Molang gehört zu Bedrock):
1. **Prozedural** in `Model.setupAnim(state)`: Sinus/Kosinus aus `walkAnimationPos/-Speed` und `ageInTicks` (Warden: Gehen, Idle, Tendrils; Zombie, Creeper usw.).
2. **Keyframe** als Java-Daten: `AnimationDefinition.Builder.withLength(..).addAnimation(part, AnimationChannel(...))`, gebacken einmal im Konstruktor (`def.bake(root)`), pro Frame `KeyframeAnimation.apply(AnimationState, ageInTicks)` bzw. `applyWalk(pos, speed, ...)`. Zustände sind `AnimationState` am Entity, gestartet per `start(tickCount)` aus Entity-Events/Synched Data (Warden: Emerge, Roar, Sniff, Attack, Sonic Boom, Dig). Das Projekt nutzt das bereits sauber in `DeceiverAnimation/-Model` (Beispiel für alle).

Konkrete Zahlen aus `WardenModel`/`WardenAnimation`/`Warden` (als Referenzgrößen, nicht kopieren):
- **Idle** nie still: `scaledAge = age·0,1`; Kopf zRot `+0,06·cos`, xRot `+0,06·sin`; Körper `0,025·sin/cos` – Kosinus und Sinus phasenverschoben = Ellipse statt Pendel.
- **Gehen**: Phase `pos·0,8662`; `speedModifier = min(0,5; 3·speed)`; Körper-Z-Roll proportional zur Geschwindigkeit (Gewicht), Kopf nickt gegenläufig, Arme gegenphasig zu den Beinen; Amplituden hängen an der Geschwindigkeit, nicht an der Zeit (kein Rutschen im Stand).
- **Dauern**: Angriff 0,33 s (7 Ticks; schnell, kurz), Roar 4,2 s, Sniff 4,16 s, Dig 5 s, Emerge 6,68 s mit Haltepunkten (lange, gut lesbare Anticipation, kurzer Schlag), Sonic Boom 3 s (1,7 s Aufladen + 1,3 s Abklingen).
- **Sekundärbewegung**: Tendrils `cos(age·2,25)·π·0,1` nur 10 Ticks nach einem Reiz (`tendrilAnimation/10`), Herzschlag-Layer mit eigenem Zähler – Reaktion auf Ereignisse über kurze Abklingzähler in der Entity, nicht über Dauerloops.
- **Layering**: erst Kopf-Look, dann Walk, Idle, Tendrils, danach One-Shots (`apply` der Keyframes zuletzt) – die One-Shots überschreiben bzw. addieren auf die Grundpose.

Die zwölf Animationsprinzipien (Johnston/Thomas) in Minecraft-Java übersetzt:
- **Anticipation / Staging**: jede Aktion bekommt eine Vorbereitungspose, die die Silhouette lesbar macht (Dread, Howler haben das); Pose und Sound zeitgleich.
- **Slow in/out (Easing)**: `CATMULLROM` im Keyframe-Baukasten oder `smoothstep(t)` bei prozeduralen Rampen, statt `min(1, t/n)` linear.
- **Follow-through / Overlapping / Drag**: Teile am Ende der Kette (Arm, Kette, Mantel, Antennen) folgen mit Verzögerung; prozedural per Tiefpass `x += (target−x)·k` mit Zustand im RenderState (Mantel des Deceiver ist ein Positivbeispiel, aber nur an Gehphase gekoppelt).
- **Timing/Gewicht**: schwer = niedrige Frequenz, große Amplitude, kurzes hartes Aufsetzen; leicht = hohe Frequenz. Angriffe kurz, Vorbereitung lang.
- **Squash & Stretch**: sparsam per `ModelPart`-Skalierung (Deceiver-Teleport macht es), bei Pixel-Würfeln nur kurz und 5–25 %.
- **Arcs**: Rotation über Gelenkketten (Schulter → Arm → Waffe), nicht Translation.
- **Secondary action**: Kleinteile (Feeler, Mantel, Kiefer) mit eigenem Takt, unabhängig vom Gehzyklus.
- **Straight-ahead/Pose-to-pose**: Keyframe-Definitionen = Pose-to-pose; prozedurale = Straight-ahead, für Dauerbewegung.

Praxisregeln für dieses Projekt:
1. Keyframe-Definitionen **einmal backen** (Konstruktor), keine Allokationen pro Frame.
2. Zeitquelle immer `partialTicks`-interpolierte Werte aus dem RenderState; Zustandsänderungen nie als Sprung (Rampe oder Abklingzähler), sonst „poppt“ die Pose.
3. **Idle-Variation**: (a) Schichten mit inkommensurablen Perioden (Watcher macht 0,05/0,07/0,09), (b) gelegentliche One-Shot-Idles (Umsehen, Kopfneigen, Zucken) mit Zufallsabklingzeit 6–15 s, (c) Phasenversatz je Mob über `entity.getId()` im RenderState, damit mehrere Mobs nicht synchron laufen.
4. **Gangarten**: `walkAnimationSpeed` wird in den Modellen auf 1,0 geklemmt; echte Lauf-/Ansturmphasen brauchen einen eigenen Zustand (z. B. `running`) mit anderem Zyklus, sonst sieht Tempo 0,8 b/Tick wie Spazieren aus.
5. **Tod**: Vanilla kippt um 90° (`deathTime`, `getFlipDegrees`); eigene Mobs können über den Zustand/`setupRotations` eine eigene Todespose bekommen.
6. **Treffer-Reaktion**: `LivingEntity.hurtTime` in `extractRenderState` in den State übernehmen und als 6–8-Tick-Zucken (Körper/Kopf) nutzen.
7. Bei Server-Telegraphen den Wind-up-Zähler und `partialTicks` mischen (Dread/Howler tun das) und **Recovery** definieren: Pose klingt über 5–8 Ticks ab.

## 2. Alle Custom-Mobs und konkrete Verbesserungen

Lebewesen mit eigenem Modell/Renderer (Registrierungen in `MobsRegistry`, `FearMobs`, `EndNetherMobs`, `WitherMobs`, `EndRegistry`, `DragonFightRegistry`):

| Mob | Modul | Modell | Ist-Zustand Animation |
|---|---|---|---|
| Hollow Howler | simplemobs | eigen (`HowlerModel`) | prozedural, symmetrisch, Gehzyklus auch beim Ansturm |
| Dread | simplemobs | eigen (`DreadModel`) | prozedural, Swing/Stomp/Rise per linearer Rampe |
| Hollow Watcher | simplemobs | eigen (`WatcherModel`) | prozedural, Stillstand = sofortiges Nullen der Wackler |
| Deceiver | simplemobs | eigen (`DeceiverModel`) | **Keyframes** (Walk, Idle, Summon, Teleport, Drink, Unmask) |
| Shellker | simplemobs | Vanilla Shulker + eigene Textur | Vanilla-Peek, Schlitz 0,2 |
| Soulfire Blaze | simplemobs | Vanilla Blaze + Textur | Vanilla |
| Blighted / Abyssal Wither | simplemobs | Vanilla Wither + Skins | Vanilla |
| Nibblemite | simpleend | eigen (`NibblemiteModel`) | konstantes Wackeln, unabhängig von Bewegung |
| Dragonmite | simpleenddragonfight | eigen (`DragonmiteModel`) | konstantes Wackeln + fester 7-Tick-Flügelruck |

Nicht-Lebewesen (nur Hinweis): `HowlerRitualEntity`, `DisguiseMarker`, Geschosse (`ShellkerBullet`, `SoulFireball`, `CraftedArrow`, `BrickProjectileEntity`), Boote/Loren, `TrainingDummy`/`SmallArmorStand`, `LevitatingBlockEntity`. Ideen dort: Trainingspuppe könnte ein Treffer-Wackeln (gedämpfte Schwingung) zeigen – Zustand im Rahmen von `TrainingDummyRenderer` prüfen, kein Teil dieser Phase.

### Hollow Howler
Siehe Gruselkonzept V2–V9. Allgemeine Zusätze: Hit-Flinch (Regel 6, S), Lauf-/Ansturmzyklus (Regel 4, M), Todesanimation (Regel 5, M), Emerge mit Haltepunkten (Warden-Muster, M).

### Dread (gepanzert, Kettenmorgenstern) — M
- **Pop am Ende der Aktion**: `state.swing`/`state.stomp` springen beim Aktionsende auf 0 → Pose klappt in einem Frame zurück. Abhilfe: Recovery-Rampe 6 Ticks (Zähler an der Entity, Daten stehen mit `clientActionAge()` schon bereit). S–M.
- **Easing** statt linear `min(1, age/(WINDUP−2))`: Anticipation mit sanftem Anlauf, schneller Schlag (3–4 Ticks), harter Halt. S.
- **Kette/Morgenstern**: heute starre Ruhelage `CHAIN_REST` aus Armwinkel; ergänzen: Drag über Tiefpass (Kette bleibt beim Schwingen zurück, schlägt nach, pendelt in 8–12 Ticks aus), beim Gehen leichtes Nachziehen auf dem Boden. M.
- **Gewicht**: Körper-Y-Dip bei jedem Schritt (zweifache Gehfrequenz, ±0,5 px), Hüft-Roll, Kopf bleibt ruhig (Gegenbewegung); Aufsetz-Sound ist schon vorhanden (`IRON_GOLEM_STEP`). S.
- **Idle**: Atmen 0,03 vorhanden; zusätzlich seltenes Kettenrasseln (Kette schwingt 20 Ticks nach, Zufall 8–20 s). S.
- **Rise**: wie Howler Emerge nichtlinear mit Haltepunkten. S.

### Hollow Watcher (4 Blöcke, lange Glieder) — S–M
- Mechanik „bewegt sich nur unbeobachtet“: der Wechsel zu `still` nullt Wackler sofort (`head.zRot` springt von `sin·0,15` auf 0,12) – gewollt als „Weeping-Angel“-Effekt, aber derzeit immer **dieselbe** Pose. Besser: beim Einfrieren eine von 4–6 *verkrümmten Posen* zufällig (Kopfneigung ±, ein Arm höher) und **zwischen zwei Beobachtungen** wechselnd → der Spieler sieht „es hat sich bewegt“ (Prinzip Inkongruenz/Vorhersagefehler, Uncanny-Forschung). S.
- **Stop-Motion-Gang** (bekannter Horror-Trick): Gehphase quantisieren `floor(pos·k)/k` (ca. 8–10 „Frames“/s), Gliederlänge macht den Rest. S.
- Reach/Claw/Scream: Rampe + Easing statt Sprung `1,0/0,7/0`; Schrei mit Kopf zurück + Kiefer-Zittern (Kopf-Zucken 0,05 rad bei 30 Hz). S.
- Beine 30 px: größere Schrittlänge, Knie-Ausschlag nicht vorhanden → Gangzyklus mit leichtem Z-Schleifen des Nachziehbeins. M.

### Deceiver (Referenz-Mob mit Keyframes) — S
- Idle ist **eine** 4-s-Schleife (`startIfStopped` dauerhaft): zwei bis drei seltene One-Shot-Idles ergänzen (Kapuze zurechtrücken, kurz umsehen, Kopf schief) mit Zufallsabklingzeit 6–15 s, ausgelöst serverseitig wie Summon/Teleport (`ACT_*`). S.
- Idle und Walk werden beide angewendet (siehe `DeceiverModel.setupAnim`): prüfen, dass sich beide nicht addieren (Idle ausblenden, wenn `walkAnimationSpeed > 0,1`), sonst Doppelwackeln. S.
- Mantel reagiert nur auf Gehphase; zusätzlich auf Drehung (Yaw-Delta → Nachschwingen). M.
- Flucht im feindlichen Modus (`AvoidEntityGoal`, 1,35×): eigener Laufzyklus (Regel 4) statt Walk-Skalierung. M.
- Nur Posen, kein neuer Cube.

### Shellker (Shulker + Schlitz) — S
- Anticipation vor dem Öffnen: Deckel zittert 5 Ticks (±0,02 am `peekAmount`) bevor der Schlitz aufgeht; Auge leuchtet gleitend auf (bereits an `openness` gekoppelt). Sichtbar nur im Renderer: `ShellkerRenderer.extractRenderState`. S.

### Soulfire Blaze / Blighted & Abyssal Wither — S (niedrige Priorität)
- Vanilla-Animation reicht. Optional Blaze: bei `DATA_SOUL_CHARGED` schnellere Stab-Rotation (`state.ageInTicks` im `extractRenderState` skalieren) + Seelenfeuer-Partikel client-seitig. Wither: keine Änderung (Vanilla gut lesbar, Skin-Pipeline unberührt).

### Nibblemite (Passiv/Tier) — S
- Wackeln läuft **konstant** (`age·0,6`, Amplituden fix), auch im Stand → wirkt aufgezogen. Abhilfe: Frequenz/Amplitude an `walkAnimationSpeed` koppeln (Stand: sehr langsames Atmen, Gehen: schnelles Schlängeln), Phasenversatz je Mob (`entity.getId()`). S.
- `NibbleGoal` (Fressen): sichtbarer Kauzyklus (Kopfsegment nickt 4×, Feeler legen sich an), Zustand per Entity-Event. S–M.
- Baby: Feeler schneller und größer proportional (Vanilla-Babyskalierung greift schon).

### Dragonmite (Monster) — S
- Flügelruck `(int) age % 7 == 0` ist für **alle** gleich getaktet; Phasenversatz per Entity-ID. S.
- Wackeln und Buzz an Zustand koppeln: Ruhe = Flügel gefaltet/langsam, Jagd/Lauf = Buzz hoch, **Angriff** (`MeleeAttackGoal`, `attackTime`) = kurzer Z-Stretch der Vorderkörper-Segmente (Lunge, 6 Ticks) + Flügel anlegen. S–M.

## 3. Reihenfolge

1. **Quick Wins (S)**, gebündelt in einem Lauf, nur Client-Code: Easing/Recovery-Rampen (Dread, Watcher), Watcher-Posen + Stop-Motion, Nibblemite/Dragonmite Kopplung an Geschwindigkeit + ID-Phase, Shellker-Zittern, Deceiver-Idle-Check.
2. **Howler Phase 2** (V2–V9) mit Gruselkonzept.
3. **M-Aufgaben**: Dread-Kette mit Drag, Watcher-Gang, Deceiver-Mantel/Flucht, Lauf-/Todeszustände.
4. L: nur falls Besitzer neue Animationssätze als Keyframes (Blockbench-Export) wünscht.

Technik-Rahmen: gemeinsamer Hilfstyp `AnimUtil` im Modul `simplemobs` (smoothstep, Tiefpass, Phasenversatz) – nur anlegen, wenn ≥ 3 Mobs ihn nutzen; Nibblemite/Dragonmite liegen in anderen Modulen und dürfen **nicht** von `simplemobs` abhängen (Prinzip Modul-Unabhängigkeit, `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`) → dort kleine lokale Kopie oder Verschieben nach `simplelib`.

## 4. Verifikation (Plan)

- Compile 3 Loader; bestehende Client-Smokes (`ModuleClientSmoke`, `FearClientSmoke`) bleiben grün.
- Je Mob Screenshot-Pose-Test (Pre/Post-Vergleich der Posen: Wind-up, Impact, Recovery); Bewegung ist nur auf der Dev-Instanz im Spiel beurteilbar (Klicktest, Video/Frame-Serie).
- Keine Server-/Verhaltensänderung → bestehende Server-Gametests unverändert; neue pure Hilfsfunktionen (Easing, Tiefpass) mit Unit-Test.
- Nicht getestet (Konzeptphase): alles.

## 5. Quellen (eigene Formulierung, nichts kopiert)

- Minecraft 26.3 Quellen (lokal gelesen): `WardenModel`, `WardenAnimation`, `WardenRenderer`, `Warden`; Projektbeispiel `DeceiverAnimation`.
- Wikipedia „Twelve basic principles of animation“ (Johnston/Thomas, „The Illusion of Life“, 1981): https://en.wikipedia.org/wiki/Twelve_basic_principles_of_animation
- Wikipedia „Uncanny valley“ (Erscheinung/Bewegung-Inkongruenz, Saygin 2012; Tinwell zu Survival-Horror): https://en.wikipedia.org/wiki/Uncanny_valley
- Minecraft Wiki „Warden“ (Animationsgalerie, Entwicklungsgeschichte): https://minecraft.wiki/w/Warden
- Richard Williams, „The Animator's Survival Kit“ (Timing/Spacing, Standardliteratur, nicht neu abgerufen).
