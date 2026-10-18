# Konzept: Hollow Howler gruseliger machen (Nachtrag 55, 2026-10-15)

Nur Recherche + Konzept, kein Code. Bestehende, abgenommene Texturen bleiben unangetastet: Phase 1 und 2 brauchen **keine** Texturänderung; neue Texturen nur als zusätzliche Dateien und nur nach Freigabe (Abschnitt 5).
Aufwand: **S** < 0,5 Tag, **M** ca. 1 Tag, **L** > 1 Tag. Alle Zahlen zu Vanilla stammen aus den 26.3-Quellen (WardenModel/-Animation/-Renderer, Warden.java, AngerLevel, SoundEvents, SoundEngine) und dem Minecraft Wiki; Quellen am Ende.

## 1. Ist-Zustand (per grep/Lesen, `modules/simplemobs`)

**Sound** (`HowlerEntity.java`): ausschließlich Vanilla-Events, kein eigenes `sounds.json` im Modul.
- Ambient = `WARDEN_LISTENING` (das ist Vanillas „bemerkt eine Vibration“-Laut, kein Idle-Laut), in jeder Phase gleich, Vanilla-Intervall.
- Hurt = `SKELETON_HURT` (dünn, klappernd, passt nicht zum Sculk-Körper); Tod = `WARDEN_DEATH`; Stimme `getVoicePitch` 0,7–0,8; Schritt `WARDEN_STEP` Vol 0,8, **Pitch 1,15** (leichter als der Warden).
- Hört es etwas: `WARDEN_TENDRIL_CLICKS` Vol 3. Angriffe: gute Ankündigungen (Sonic-Charge, Shrieker, Ravager-Roar). Phasenwechsel: nur `WARDEN_ROAR`.
- **Kein Herzschlag** am Mob (nur im Ritual `HowlerRitualEntity`, danach Stille), keine Phasen-Ambient, keine Stille-vor-dem-Schlag, Sculk-Spur lautlos.
- Alle Sounds tragen Warden-Untertitel („Warden takes notice“ usw.) – bei Howler irreführend.

**Visuell** (`HowlerModel/Renderer/RenderState`): 2,5 Blöcke, Sculk-Körper, Rippenplatte, Katalysator-Schulterkappen (T-Silhouette), Skelettschädel + Unterkiefer, zwei Antennen.
- Glow-Layer `hollow_howler_glow.png` hat nur **42 sichtbare Pixel**, pulsiert als Sinus (0,75–1,0) ohne Bezug zu irgendeinem Spielereignis; Antennen-Layer = Vanilla-Tendril-Zucken (10 Ticks).
- Animation rein prozedural, symmetrisch: Beine/Arme Gegentakt `cos(pos*0,55)`, Arm-Sway ±0,05 rad, kein Kopf-Idle, Kiefer ab Phase Hunger dauerhaft halboffen (0,35), Biss öffnet linear in 4 Ticks und fällt beim Zuschnappen in einem Frame zurück (kein Nachschwingen). Ladeanimationen (Schrei/Schockwelle/Ansturm) sind Posen, kein Timing-Feintuning; der Ansturm nutzt denselben Gehzyklus (Tempo auf 1,0 geklemmt).
- Emerge: linearer Versatz `root.y += 40*(1-emerge)` in 40 Ticks (Warden: 6,7 s mit Haltepunkten).
- Partikel nur punktuell bei Angriffen (server-gesendet `sendParticles`), keine Umgebungspartikel; Tod = Vanilla-Umkippen.
- Gameplay-Rahmen (nicht ändern ohne Besitzer): Licht ≤ 7 beim Ritual, Darkness nur durch den Biss, Server-Autorität über `MobsConfig`.

## 2. Gruselprinzipien (Recherche, auf den Howler übertragen)

1. **Präsenz vor Sicht.** Der Warden wurde „sound-first“ entworfen; sein Herzschlag macht Nähe hörbar, und er beschleunigt mit der Wut: `40 - floor(clamp(anger/80,0,1)*30)` Ticks, also 40 → 10 (Warden.java `getHeartBeatDelay`). Der Entwickler wollte erst den Herzschlag des Spielers, verlegte ihn dann in den Mob (Wiki-Geschichte, Zitate Pearce). Übertrag: der Howler braucht einen eigenen, phasenabhängigen Herzschlag, der auch ohne Sichtkontakt Distanz und Phase verrät.
2. **Akusmatik** (Quelle sichtbar/unsichtbar, Chion „Acousmêtre“): was man hört, aber nicht sieht, wirkt bedrohlicher. Minecraft liefert die Technik gratis: Mono-Sounds sind positional (Stereo-Dateien nicht; Wiki sounds.json), Reichweite = `max(volume,1) × attenuation_distance (16)` (Sound.java), Lautstärke selbst wird auf ≤ 1 geklemmt (SoundEngine). Übertrag: Howler ist blind und hört nur → Geräusche gehören an **seine** Position und nicht ins Spieler-Ohr; der Spieler soll *wissen* „es ist links hinter der Wand“.
3. **Stille und Dynamik.** Schreck entsteht aus Kontrast: vor dem Schlag die Ambient-/Herzschlag-Spur aussetzen, dann hart zuschlagen. Vanilla-Warden liefert null Ambient während Roar/Emerge (`getAmbientSound` = null). Übertrag: während `attack() != NONE`, Stagger und Emerge Ambient + Herzschlag pausieren; Pausen ≥ 3–5 s zwischen lauten Ereignissen (Schreck-Budget).
4. **Wrongness statt Ekel.** Uncanny-Valley-Forschung (Wikipedia „Uncanny valley“, Saygin 2012, Tinwell 2010 zu Survival-Horror): Unbehagen entsteht, wenn **Erscheinung und Bewegung nicht zusammenpassen** (Vorhersagefehler) und bei „fast-menschlichen“ Merkmalen, besonders Augen/Gesicht. Der Skelettschädel ist genau so ein Merkmal. Übertrag: kein Realismus, sondern gezielte Inkongruenz: schwerer Körper, aber zuckende, zu schnelle Kleinteile (Kiefer, Antennen); asymmetrische Haltung; Kopf dreht sich zur Quelle, bevor der Körper folgt. Die Warden-Entwicklung bestätigt die Grenze: die „Hollowed“-Variante (Trypophobie) wurde als zu verstörend verworfen → Ziel ist **Unheimlichkeit, nicht Ekel**.
5. **Zurückhaltung.** Schatten und Stillstand wirken mehr als Dauerleuchten: Augen/Glow nur, wenn es etwas „will“ (hört, jagt, lädt auf), sonst dunkel. Wardens „Force of nature“-Rolle (Pearce): man flieht, man kämpft nicht.
6. **Infraschall – ehrlich bewertet.** Tandy & Lawrence (1998, 19 Hz) und das „Infrasonic“-Konzert 2003 (17 Hz, ca. 700 Hörer, 22 % berichteten Unruhe/Frösteln/Furcht; Quelle Wikipedia „Infrasound“) stützen einen Effekt, die Befundlage ist aber schwach und umstritten (Altmann: kein verlässlicher Nachweis für Übelkeit u. Ä.; Dyatlov-These spekulativ). **Technisch** erreicht ein MC-Spieler mit Kopfhörern/Laptop-Lautsprechern < 30–40 Hz praktisch nicht, und Vorbis-OGG ist kein Infraschall-Träger. Realistisch ist nur „Pseudo-Infraschall“: ein tiefes Brummen um 35–50 Hz mit ~17-Hz-Amplitudenpuls (Schwebung zweier Töne, z. B. 40 + 57 Hz) plus Obertöne bei 80–120 Hz, die Hüllkurve auch auf Laptops hörbar macht. Nur sehr leise unterlegt, nie dauerhaft, im Hörschutz-Rahmen; **keine physiologischen Wirkungen behaupten**.

## 3. Maßnahmen, priorisiert

### Phase 1 – nur Vanilla-Sounds und Render-Logik, keine neuen Assets (S, risikoarm)

| # | Maßnahme | Aufwand | Dateien | Prüfung |
|---|---|---|---|---|
| A1 | **Herzschlag** am Mob, client-seitig in `tick()` (wie Warden): Takt phasenabhängig 40 / 26 / 14 Ticks (Hunger/Wut), `SoundSource.HOSTILE` (Lautstärkeregler „Feindliche Wesen“ greift), Vol ~2,5 (Reichweite 40 Blöcke), Pitch an Stimme gekoppelt (0,6–0,8). „Lub-dub“: zweiter Schlag 6 Ticks später, Pitch −0,15. Pause während Angriff/Emerge. Der Ritual-Herzschlag (`HowlerRitualEntity`) geht nahtlos in den Mob-Herzschlag über. | S | `HowlerEntity.tick` (Client-Zweig), neue pure Funktion `HowlerLogic.heartbeatDelay(phase)` | Unit-Test der Funktion in `HowlerTests`; Gehör auf Dev-Instanz |
| A2 | **Ambient nach Phase**, Intervall länger: ONE `WARDEN_AMBIENT`, HUNGER `WARDEN_AGITATED`, RAGE `WARDEN_ANGRY`, jeweils Pitch ≈ 0,7; `WARDEN_LISTENING` nur noch als Reaktion beim Hören (zusätzlich zu den Tendril-Klicks). `getAmbientSoundInterval` ≈ 200 (ein Laut alle ~10 s statt alle ~4 s); null bei Angriff/Stagger/Emerge. | S | `HowlerEntity` (getAmbientSound, `getAmbientSoundInterval`) | GameTest: `getAmbientSound` je Phase über Testhaken |
| A3 | **Hurt geschichtet**: `WARDEN_HURT` (Pitch 0,7) + leises `SKELETON_HURT` (Vol 0,35, Pitch 0,5) für Knochenknacken; beim Schwachstellen-Treffer (Kiefer offen) zusätzlich kurzer `SCULK_CATALYST_HIT`. Namenprüfung `WARDEN_HURT` für den Snapshot 26.4 (Wiki: Event „until 26.4“). | S | `HowlerEntity` (`getHurtSound`, `hurtServer`) | Compile 26.3; 26.4-Check beim Port-Run |
| A4 | **Schritte schwerer + unregelmäßig**: Pitch 0,75, jeder zweite Schritt zusätzlich `SCULK_BLOCK_STEP` (Pitch 0,6, nass); linker Schritt Vol 1,0, rechter 0,7 (hinkender Gang passt zu V3). | S | `playStepSound` | Gehör |
| A5 | **Sculk-Spur hörbar**: beim Setzen eines Spur-Flecks `SCULK_BLOCK_SPREAD` Vol 0,5, Pitch 0,5; Emerge: `WARDEN_EMERGE` Pitch 0,65 statt 1,2 (schwerer), zweiter Schlag nach 20 Ticks. | S | `tickTrail`, `HowlerRitualEntity` | Gehör |
| A6 | **Suche statt Resignieren**: verliert es das Ziel (200 Ticks), `WARDEN_SNIFF` (Pitch 0,6) und `WARDEN_LISTENING` im Abstand; bei neuer Störung `disturbance` setzt Kopf-Dreh (V2). | S | `customServerAiStep` | GameTest bestehender Jagd-/Vergessen-Test bleibt grün |
| A7 | **Phasenwechsel mit Stille**: 1 s kein Ambient/Herzschlag vor dem Roar, dann Roar (Schreck-Budget). | S | `updatePhase` | Gehör |
| A8 | **Warnung zu Untertiteln**: Vanilla-Sounds zeigen Warden-Untertitel. Akzeptiert bis Phase 3 (eigene Events), im Bericht festhalten. | – | – | – |

### Phase 2 – Animation und Partikel, nur Client-Code (M, keine Texturänderung)

| # | Maßnahme | Aufwand | Dateien |
|---|---|---|---|
| V1 | **Glow pulsiert im Herzschlag** (Lub-dub-Hüllkurve, Mindesthelligkeit 0,45, damit es **nicht blinkt**: WCAG 2.3.1 max. 3 Blitze/s, Rage 2 Hz Doppelschlag = Amplitude statt An/Aus); Antennen-Layer zusätzlich beim Hören hell und langsam abklingend. Vorhandene 42 Glow-Pixel genügen. | S | `HowlerRenderer` (Alpha-Lambda), `HowlerRenderState` (heartPhase) |
| V2 | **Idle nie still**: Kopf mit zwei inkommensurablen Perioden (z. B. 0,07/0,11 rad/Tick, zeitversetzt um `entity.getId()`), Kiefer-„Chatter“ (kurzes 2–3°-Zucken alle ~3 s), Kopf wendet sich zur `disturbance`/zum Ziel **vor** dem Körper (Kopf folgt mit Faktor 1,0 statt 0,7, Körper verzögert). Warden-Vorbild: Idle-Wobble `0,06·cos/sin(0,1·age)`. | M | `HowlerModel.setupAnim`, `HowlerRenderState` (id, disturbanceYaw) |
| V3 | **Asymmetrischer Gang**: Beinamplitude links/rechts 1,0/0,75, Körper kippt 2–3° zur schwächeren Seite, Arme schwingen **nicht** gegengleich, sondern hängen nach (Follow-through): Armwinkel = Tiefpass der Beinphase (Verzögerung ~4–6 Ticks). Ergebnis: schwerer Körper, falsch schwingende Glieder (Inkongruenz). Warden-Gang als Vorlage: Kopf-Z-Roll proportional zur Geschwindigkeit. | M | `HowlerModel` (Zustand per RenderState-Historie) |
| V4 | **Biss mit Anticipation/Nachschwingen**: 3 Ticks Kiefer schließt leicht (−0,1), 9 Ticks auf, beim Zuschnappen Überschwinger (−0,15) + 4 Ticks Ausklingen statt Frame-Sprung; Kosinus-Easing statt linear `min(1,t/4)`. | S | `HowlerRenderer.extractRenderState`, `HowlerModel` |
| V5 | **Ansturm = Laufzyklus**: bei `attack()==CHARGE` Tempo-Klemme anheben, Körper 0,35 rad vor, Kopf unten, Arme nach hinten; Telegraph (Ducken) wie bisher, Impact-Pose beim Wandtreffer mit 4-Tick-Rückfedern. | M | `HowlerModel`, `HowlerRenderer` |
| V6 | **Emerge mit Haltepunkten statt linearem Auftauchen**: 40 Ticks bleiben (Gameplay), aber nichtlinear: 0–12 Ticks langsam (nur Hände/Schädel), 12–22 Stopp + Zucken, 22–40 Ausstieg mit Überschwinger (Warden-Muster: Haltepunkte, Kopfzucken). Client-seitige Dreck-/Sculk-Partikel aus dem Boden. | M | `HowlerModel`, `HowlerEntity.tick` (Client) |
| V7 | **Umgebungspartikel client-seitig** (`addParticle` im Client-Zweig von `tick`, kein Netzwerk, respektiert „Minimale Partikel“): langsam aufsteigende `SCULK_SOUL` vom Schädel (Hunger: ~1 alle 8 Ticks, Wut: ~1 alle 3), gelegentlich `SCULK_CHARGE_POP` an den Füßen; bei Hören: kurzer Ring um die Antennen. | S | `HowlerEntity.tick` |
| V8 | **Todesanimation** statt Umkippen (`deathTime`-Rotation überschreiben): in die Knie, Kopf kippt, Kiefer fällt auf, Glow verlischt über 2 s, Seelen-Partikel steigen. | M | `HowlerRenderer`, `HowlerModel` |
| V9 | **Haltung ohne neue Cubes**: statische Asymmetrie nur über PartPose-Rotation (ein Arm 0,12 rad nach außen, anderer angewinkelt; Schädel 4° geneigt). Cube-Maße/UV bleiben, damit die abgenommene Textur passt. | S | `HowlerModel` |

### Phase 3 – eigene Audio-Assets und Hüllkurven (L, braucht Freigabe)

- **Eigene Sound-Events** (`simplemobs:entity.hollow_howler.{ambient,ambient_hunger,ambient_rage,hurt,death,step,heartbeat,roar}`) mit Untertiteln „Hollow Howler …“ (EN/DE, behebt A8). Mono-OGG, `sounds.json` im Modul, Registrierung nach dem Muster `simplesandwiches/registry/ModSounds` (je Loader beim Sound-Event-Register). Lang-Schlüssel nur additiv.
- **Synthese statt Samples**: Generator `tools/sounds/make_howler_sounds.py` nach Vorbild `make_bore_sound.py` (Rauschen + Oszillatoren, Seeds für Varianten, keine Mojang-Samples einchecken – gilt wie bei Texturen). Rezept: Herzschlag = Sinus 42 Hz mit schnellem Anschlag + 2. Schlag; Ambient = gefiltertes Rauschen (Atem), 40 + 57 Hz Brummen (17-Hz-Schwebung), Knochen-Klick-Trigger; Wut = Subharmonische + Verzerrung. Voraussetzung: numpy, scipy, soundfile, ffmpeg (auf diesem CT **nicht installiert**: scipy und ffmpeg fehlen).
- Pseudo-Infraschall nur als leise Schicht unter dem Herzschlag, ≤ −20 dBFS gegenüber dem Herzschlag, nie als Dauerschleife; Hörkomfort über den Vanilla-Regler „Feindliche Wesen“ (kein eigener Config-Schalter nötig).
- **Pro Mono-Datei Dauer** kurz (< 3 s), Streaming nicht nötig.

### Optional / Besitzerentscheidung

- **Darkness-Puls** wie beim Warden (13 s Darkness für Spieler im 20-Block-Radius alle 6 s, synchron zum Herzschlag): stärkster Gruseleffekt, aber Balance-Eingriff (bisher nur Biss). Nur mit Freigabe, evtl. nur in Wut.
- **Zusätzliche Emissiv-Textur** `hollow_howler_heart.png` (Rippenplatte pulsiert) als **neue Datei** über `tools/textures/howler_textures.py`, Vorschau mit Gegenüberstellung – ändert keine abgenommenen Texturen, braucht aber Freigabe, weil es Pixelkunst ist.
- **Kamerawackeln** bei Schockwelle/Schrei: Client-Mixin, mit Barrierefreiheits-Schalter („Bewegung reduzieren“); nur wenn A/V-Maßnahmen nicht genügen.

## 4. Reihenfolge und Verifikation

1. Phase 1 (A1–A7) in einem Zug: Compile 3 Loader, `howler_*`-Gametests (Ambient-Phase, `heartbeatDelay`), Wiki: nur falls Soundverhalten beschrieben ist.
2. Phase 2 (V1–V9): Client-Test (`ModuleClientSmoke`/Screenshot-Posen: Biss offen, Schrei, Emerge-Mitte), Dev-Instanz-Klicktest mit Ton.
3. Phase 3 nach Freigabe und Werkzeug-Umgebung.
Abnahmekriterium subjektiv: im Dunkeln ohne Blickkontakt erkennt man an Herzschlag/Ambient, dass er nahe ist und in welcher Phase; vor jedem Schlag gibt es ≥ 0,5 s hörbare Stille.

Nicht getestet: alles hier ist Konzept; Hörwirkung und Akzeptanz müssen auf der Dev-Instanz beurteilt werden.

## 5. Quellen (eigene Formulierung, nichts kopiert)

- Minecraft Wiki „Warden“ (Sounds, Suspense, Herzschlag, Entwicklungsgeschichte mit Pearce-Zitaten): https://minecraft.wiki/w/Warden
- Minecraft Wiki „Sounds.json“ (Mono = positional, `attenuation_distance`, `stream`): https://minecraft.wiki/w/Sounds.json
- Wikipedia „Infrasound“ (Tandy/Lawrence 1998, Infrasonic-Konzert 2003, Kritik): https://en.wikipedia.org/wiki/Infrasound
- Wikipedia „Uncanny valley“ (Hypothesen, Designprinzipien, Tinwell/Saygin): https://en.wikipedia.org/wiki/Uncanny_valley
- Wikipedia „Acousmatic sound“ (Chion): https://en.wikipedia.org/wiki/Acousmatic_sound
- Minecraft 26.3 Quellen (lokal gelesen): `WardenModel`, `WardenAnimation`, `WardenRenderer`, `Warden`, `AngerLevel`, `SoundEvents`, `SoundEngine`, `Sound`.
- WCAG 2.3.1 „Three Flashes or Below Threshold“ (Standardwissen, nicht neu abgerufen).

## 6. Antworten Besitzer (2026-10-15, verbindlich)
H1 Phase 1 und 2 umsetzen. H2 danach Phase 3 (eigene synthetische Sounds + Untertitel). H3 Darkness-Puls nur in der Wut-Phase. H4 leuchtendes Herz als neue Emissiv-Textur, Vorschau zur Freigabe vor dem Einbau. H5 Kamerawackeln mit Schalter „Bewegung reduzieren“. Mob-Animationen (A1): in der vorgeschlagenen Reihenfolge.

## 7. Umsetzungsplan Lauf claude-howler-n63 (2026-10-15)
Umfang: Phase 1 (A1-A7), Phase 2 (V1-V9), H3 Darkness-Puls nur Wut, H5 Kamerawackeln mit Client-Schalter, H4 Herz nur als Vorschau. Phase 3 (eigene Sounds) nicht in diesem Lauf.
- `HowlerLogic`: reine Funktionen (Herzschlag-Takt/Zyklus, Stille-Regel, Puls-Takt auf Herzschlag gerastet, Glow-Hüllkurve, Biss-/Emerge-Kurven, Wackel-Amplitude).
- `HowlerEntity`: Client-Herzschlag (lub-dub) in `tick`, Ambient je Phase (Intervall 200), Hurt/Step/Trail/Emerge-Sounds, Phasen-Stille (synched `DATA_HUSH`, 1 s vor dem Phasenwechsel), Suche nach Zielverlust, Darkness-Puls (`MobsConfig`: howlerRagePulse/-Interval/-Ticks/-Radius), Stagger gespiegelt (`DATA_STAGGER`), Tod 2 s, Client-Partikel.
- `HowlerModel/Renderer/RenderState`: Glow im Herzschlag (min. 0,45), Idle/Kiefer-Chatter, asymmetrischer Gang, Biss-Kurve mit Nachschwingen, Ansturm-Pose, Emerge mit Haltepunkten, Todesanimation, Haltung per PartPose. Cubes/UV unverändert.
- `HowlerShake` (Client) + `simplemobs-client.json` (`reduceMotion`, Standard false = Wackeln an). Kein Mixin: die Blickrichtung wird um einen kleinen Offset versetzt, den dieselbe Entität beim nächsten Tick zurücknimmt.
- Herz-Textur: nur Generator `howler_textures.py --heart-preview` + Vorschau in `docs/ai/previews/claude-howler-n63/`, nichts in `resources`.
- Verifikation: Compile 3 Loader, `simplemobs:*howler*` auf Fabric+NeoForge, neue Tests `howler_heartbeat`, `howler_rage_pulse_rules`, `howler_phase_sounds`, `howler_rage_pulse`, `howler_no_pulse_before_rage`; Wiki/Guide/Tooltip-Checks.
Abweichungen: Kopf-Dreh zur Störung nutzt `lookControl` auf dem Server (Kopf folgt mit Faktor 1,0) statt eigenem `disturbanceYaw`; kein Untertitel-Fix (Phase 3).

## 8. Umsetzung H2 – Phase 3 eigene Sounds (claude-howler3, 2026-10-16)
Ist-Zustand nach claude-howler-n63: Phase 1/2, H3, H5 sind drin (Vanilla-Sounds geschichtet, Herzschlag am Mob mit `HowlerLogic.heartbeatCycle`, Stille vor Phasenwechsel `quiet()`, Miniboss-Größe/Bossbar/Sculk-Jaws in Phase 2). Kein Eingriff in diese Logik: nur die Sound-Events werden getauscht, Takt/Stille/Pulse bleiben.
- Generator `tools/sounds/make_howler_sounds.py` (numpy/scipy/soundfile, feste Seeds, nichts kopiert): 18 Mono-OGG (44,1 kHz) nach `assets/simplemobs/sounds/entity/hollow_howler/`. Spitze ca. -2 dBFS, Vanilla-nah.
- Ereignisse (`MobsSounds`, `sounds.json`, Untertitel EN/DE `subtitles.simplemobs.entity.hollow_howler.*`): `heartbeat` (ein Schlag, Code spielt lub-dub; 45-Hz-Sub + 40/57-Hz-Schwebung), `ambient`/`ambient_hunger`/`ambient_rage` (je 2 Varianten: trockenes Knochenatmen -> nasses Rasseln mit Kieferklappern -> verzerrtes Knurren mit Sub-Oktave und Tremolo), `hurt` (3), `death`, `step` (3), `notice` (statt WARDEN_LISTENING), `sniff` (Suche), `roar` (Hunger-Wechsel, Ansturm, Biss-Ankündigung), `roar_rage` (Wut-Wechsel, längster Laut, leise Pseudo-Infraschall-Schicht).
- Eingehängt in `HowlerEntity` (Ambient je Phase, Hurt ohne Skelett-Schicht, Death, Step, Herzschlag, Hören, Suche, Phasenwechsel-Roar). Angriffs-Ankündigungen (Sonic-Charge, Shrieker, Impact, Emerge) bleiben Vanilla, damit Spieler sie wiedererkennen; Untertitel dazu bleiben Warden/Vanilla.
- Registrierung: Fabric über `MobsRegistry.registerAll`, NeoForge/Forge über `RegisterEvent` (`Registries.SOUND_EVENT`).
- Prüfung: `tools/check_data.py` (Events = sounds.json, Dateien vorhanden, Untertitel in beiden Sprachen), Compile, `simplemobs:*howler*`.
- Nicht prüfbar ohne Ohr: Klangwirkung (Besitzer hört `/tmp/claude-0/ash/howler3/`). Nachjustieren nur im Generator (Konstanten je Funktion), dann neu erzeugen.

## 9. Sounds überarbeitet: Variante A und B (claude-howler4, 2026-10-17)
Besitzer 17.10.: die 18 Howler-Sounds aus §8 klingen synthetischer als Enderman/Warden → zwei Varianten, im Spiel A.

**Messung (Vanilla 26.3, Asset-Index der Loom-Caches auf sb-test; Spitze/aktive RMS in dBFS, Schwerpunkt):**
Warden-Ambient −21…−19 dB, Schwerpunkt 145–220 Hz (62–72 % unter 150 Hz); Warden-Roar −16 dB, 270–540 Hz;
Warden-Hurt −14 dB, 600 Hz; Warden-Step −27 dB, 640 Hz; Warden-Sniff −22 dB, 1,1 kHz; Ravager/Zoglin −12…−8 dB, 130–260 Hz;
Enderman 530–590 Hz. Vanilla-Stimmen: 7–12 % Tonhöhen-Zittern, langsames Anschwellen, kurzer Raumanteil.
howler3 dagegen: Atmen/Hunger/Notice/Sniff Schwerpunkt 2,1–3,9 kHz (Zischen), Roar/Steps/Wut 96–99 % reiner Sinus-Bass,
alle auf −2 dB Spitze normalisiert (Schritte so laut wie Brüllen).
A und B liegen jetzt in diesen Bereichen (A: Ambient 220–240 Hz/−21 dB, Roar 295–340 Hz/−15 dB, Hurt 380–450 Hz/−15,5 dB,
Step −24,5 dB, Sniff 1,3 kHz/−22 dB; B ähnlich).

**Variante A „besser synthetisiert“ (Standard, eingecheckt):** `tools/sounds/make_howler_sounds.py` + gemeinsame DSP
`tools/sounds/howler_dsp.py` (numpy/scipy/soundfile, feste Seeds, kein fremdes Audio). Stimmband-Impulse einzeln mit
Jitter/Shimmer/Knarren (Unteroktave), Atemrauschen im Stimmtakt, 4-Formanten-Kaskade eines langen Rachens (driftend),
nasse Körner/Blasen, Aufschläge als angeschlagene Resonanzkörper (Modal-Synthese), erzeugter kurzer Höhlenhall (IR),
Tiefpass wie eine Aufnahme, Pegel per aktiver RMS auf Warden-Lautheit. Neu erzeugen: `python3.12 tools/sounds/make_howler_sounds.py`.

**Variante B „aus Vanilla abgeleitet“ (nie im Repo, AGENTS.md Regel 7 analog zu Texturen):**
`tools/sounds/vanilla_derived_sounds.py` (`MODULE_OUTPUTS`, `NAMES` = dieselben 18 Dateinamen, `sounds.json` unverändert)
schichtet Warden/Ravager/Hoglin/Zoglin/Enderman/Skelett/Schleim tiefer gestimmt, gedehnt, teils rückwärts, gefiltert.
Quelle: Sounds liegen nicht im Client-Jar, sondern als gehashte Objekte im Asset-Store (`--assets`, `SB_MC_ASSETS`,
`GRADLE_USER_HOME/caches/fabric-loom/assets`, `…/neoformruntime/assets`, `~/.minecraft/assets`).
Gradle: `gradle/module-vanilla-derived-sounds.gradle` (in `modules/simplemobs/{fabric,neoforge,forge}/build.gradle`).
Nur mit `-PsbHowlerSounds=vanilla` (oder `SB_HOWLER_SOUNDS=vanilla`) läuft `generateVanillaDerivedSounds` nach
`build/generated-resources/vanillaDerivedSounds` (gitignored über `build/`); `processResources`/`sourcesJar` lassen dann
die eingecheckte A-Datei gleichen Namens weg. Ohne Python-Pakete oder Asset-Store: Warnung und A (`-PsbVanillaDerivedStrict`
bricht ab). Erlaubte Werte: `synth` (Standard) und `vanilla`, sonst Abbruch.
Vorschau (privat, Vanilla-Referenzen nur dort): `/tmp/claude-0/ash/howler4.html`. Nachjustieren nur in den Generatoren.
