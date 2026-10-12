# Konzept: Deceiver Stufe 2 (2026-10-11, Konzept, kein Code)

Basis: `KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md` (Besitzer-Entscheidungen dort gelten), Stufe 1 in `modules/simplemobs` (DeceiverEntity/-Logic/-Renderer, Loot: Täuscherstoff 2-4 pro Kill, Looting +0-1; Mirage/Reverse Mirage liegen im SB-Kern `effect/PerceptionEffect`). Offene Queue-Punkte: Verwandlung beliebiger Mobs, Tarnumhang, doppeltes Leben, Hitbox nach Verwandlung. Vorschau: `~/previews/concepts/deceiver2.png`.

Gemeinsam: alles in `simplemobs` (Sub-Mod), Config in `simplemobs-server.json`, Tag-Dateien statt Hartcode, Tests als Gametests (Server) + Client-Smoke (Optik).

## 1. Verwandlung in beliebige Mobs
**Funktion.** Der Deceiver tarnt sich als anderer Mob (Tarnung wählt er je Welle: friedlich oder feindlich, Stufe 1 wählt das schon). Neu: statt der festen Liste `PEACEFUL/BASE/...` jeder Mob aus dem Tag `#simplemobs:deceiver_disguises` (Standard: alle `minecraft:*` Lebewesen außer Spieler, Bosse, Warden, Heuler, Riesen, Kleinstmobs ohne Hitbox-Sinn). Modder-Mobs per Tag ergänzbar, Config `disguiseBlacklist`.
**Technik.**
- Entity-Data `DISGUISE` (Registry-Id als String, leer = normal) + `DISGUISE_SEED` (Variante: Schafsfarbe, Dorfbewohner-Beruf, Katzenart ...; stabil aus Seed).
- Client: `DeceiverRenderer` hält pro Deceiver eine Attrappe (nie in die Welt eingefügt, `EntityType.create(type, clientLevel)`), gleicht je Frame Position, Körper-/Kopfdrehung, Lauf-/Angriffsanimation, Hurt-Zeit, Baby-Flag ab, lässt den passenden Renderer deren Render-State extrahieren und zeichnet damit (Renderer-Delegation; Cache der Attrappe, Aufräumen bei Entfernung/Verwandlung). Eigene Layer (Augen, Brustplatte) werden dabei ausgeblendet. Exakte 26.x-API (`EntityRenderDispatcher.getRenderer`, `extractRenderState`, `submit`) beim Bau prüfen.
- Server: `getDimensions()` liefert Maße des Zielmobs (Hitbox wie Queue-Punkt), `refreshDimensions()` beim Wechsel, Position gegen Wandklemmung prüfen (zu große Maße: Wechsel verschieben/abbrechen). Augenhöhe/Schattenradius vom Ziel. Sound: Hurt/Idle/Schritt des Ziels; Gegenspiel: Deceiver macht keine Schrittgeräusche der Art (siehe Konzept 10-07: Fake-Mobs lautlos), Schatten bleibt klein (Tell bei großen Mobs).
- Bewegung bleibt Deceiver-Pathing (Tempo, Fluchtverhalten); Flug-/Schwimmmobs nur als Optik am Boden (Blacklist: Ghast, Phantom, Fische, Enderdrache).
- Enttarnung: Treffer, Fernrohr-Blick, Spiegel (Teil 4), Tod; Animationen nutzen vorhandene Verwandlungs-Animation.
**Balance.** Größere Hitbox macht ihn leichter zu treffen, kleine (Silberfisch, Huhn) schwerer; deshalb nur Größenklassen aus Stufe 1 (klein/mittel/groß), Wechsel höchstens alle 12 s, Leben bleibt (Queue: doppeltes Leben, 200/240 mit Platte; getrennt umsetzen, Wellen-Einfluss in `DeceiverLogic` prüfen).
**Optik-Idee.** Verwandlung: Stoff-Schleier wirbelt hoch, kurzer Pixel-Wechsel (2 Ticks gemischt), Gläserner Klang. Leichtes Flimmern an den Rändern ca. alle 20 s (fairer Hinweis).
**Modul-Unabhängigkeit.** Nur Vanilla-Registry + Tag, kein SB-Kern nötig. Gerät ein Modmob ohne Renderer an, bleibt Deceiver sichtbar (Fallback), loggt einmal.
**Tests.** Gametest: Wahl nur aus erlaubten Mobs, Hitbox = Zielmaß, Reset beim Treffer; Unit: Tag-/Blacklist-Filter, Größenklassen; Client-Smoke: Rendern von 10 Zielmobs ohne Exception (Schaf, Dorfbewohner, Kuh, Creeper, Spinne ...), Screenshot-Vergleich Deceiver vs. Ziel.

## 2. Tarnumhang (Tarn-Umhang) aus Täuscherstoff
**Funktion.** Getragen (Brustplatz, wie Elytra) halten Mobs **einer gewählten Art** den Träger für ihresgleichen: sie wählen ihn nicht als Ziel, Hostile-Mobs der Art greifen nicht an, Neutrale werden nicht wütend, Tiere der Art fliehen nicht vor ihm. Unter Strafe, nicht wenn der Spieler sie oder Artgenossen angreift (dann 10 s lang aufgedeckt, Umhang -10 Haltbarkeit).
**Art wählen.** Mit dem Umhang in der Hand Rechtsklick auf einen Mob = "Probe nehmen": Art wird im Item gespeichert (Komponente `simplemobs:cloak_species`, EntityType-Id), Tooltip + Item-Name "Tarnumhang (Zombie)" , Textur-Tönung/Emblem nach Art (Stufe 2b). Neue Probe überschreibt (kostet 5 Haltbarkeit); ohne Probe wirkungslos. Alternative ohne GUI und ohne Menü, passt zu "Filter: ein echtes Item" (Spieler muss die Art erst erlegen oder finden).
**Technik.** Ein Mixin auf `TargetingConditions.test` (oder Ziel-Goal-Check), geladen nur wenn simplemobs aktiv: Ziel wird abgelehnt, wenn Angreifer-Typ == Umhang-Art und nicht provoziert (Merker in Spieler-Attachment, `lastAttackedSpeciesTick`). Haltbarkeit: 1 pro 5 s, in denen ein Mob der Art in 24 Blöcken Sichtweite war, nicht pro Tick (keine Dauerabnutzung).
**Grenzen.**
- Bosse (Wither, Enderdrache), Warden, Hollow Howler (Vibration/Schall, nicht Sicht), Wither-Skelette im Nether-Fort? (erlaubt; sonst zu stark), Creeper ja, Spieler nie, Villager-Golems nicht (Ruf, nicht Art).
- Nähe-Enttarnung: Mob der Art im Umkreis 2 Blöcke prüft alle 40 Ticks mit 20 % ("Geruch"), sonst Verdacht für 10 s; Sprinten: Chance 40 %. Zaubert die Täuschung nicht in unendlich.
- Nicht kombinierbar mit Elytra/Brustplatte (selber Platz): Preis der Tarnung, kein Rüstungswert (0), Haltbarkeit 240.
- Probe von Boss-/Spieler-Typen verweigert (Hinweis im Aktionsleiste).
**Rezept.** Geformt: `C C C / P . P / C . C` mit C = Täuscherstoff (5, ca. 2 Deceiver-Kills), P = Phantommembran (2). Repariert mit Täuscherstoff am Amboss/Grindstone-Kombi (1 Stoff = 60). Zusätzlich Drop-Chance 3 % (Looting +2 %) vom Deceiver, wie im Besitzer-Entscheid ("Tarnumhang als Drop").
**Optik-Idee.** Kapuzenumhang, dunkles Violett-Grau mit zerrissener Unterkante, kleiner Augenfleck an der Schulterschließe; Rückenansicht als Cape-Layer (Elytra-Slot). Item im Inventar: gefalteter Stoff mit Schimmer (siehe Vorschau). Aktive Tarnung: Rand des Umhangs flackert leicht in Artfarbe (sehr subtil, nur Ego-Perspektive aus).
**Modul-Unabhängigkeit.** Item/Komponente/Mixin in `simplemobs`; benötigt nur Täuscherstoff (gleiches Modul). Ohne simplemobs existiert nichts davon. Rezept und Item per `simplemobs:cloth`-Bedingung nicht nötig.
**Tests.** Gametest: Zombie mit Umhang (Art Zombie) wählt Spieler nicht; Skelett schon; nach Angriff auf Zombie aufgedeckt; Boss/Warden-Ignorieren-Test per Tag; Haltbarkeitsabnutzung; Probe überschreibt. Unit: Enttarnungs-Logik in `DeceiverLogic`-Stil (reine Funktion). Client: Item-Tooltip/Render.

## 3. Trank der Unsichtbarkeit ohne Partikel
**Funktion.** Vanilla-Unsichtbarkeit (Effekt `minecraft:invisibility`) mit `showParticles=false`, `ambient=false`, Icon sichtbar. Kein Fremdeffekt, Mobs/Spieler reagieren wie bei Vanilla (reduzierte Sichtweite, Rüstung/Hand-Items weiter sichtbar nur wenn Rüstung getragen, wie Vanilla).
**Brau-Rezept.** Trank der Unsichtbarkeit (Vanilla) + Täuscherstoff -> "Trank der stillen Unsichtbarkeit" (3:00), + Redstone 8:00; Wurf-/Verweiltrank wie üblich (Schießpulver/Drachenatem), Pfeil-Variante via Verweiltrank-Rezept. Alternativ ohne Vanillatrank: Seltsamer Trank + Täuscherstoff + Fermentiertes Spinnenauge.
**Balance.** Gleiche Dauer wie Vanilla, kein Vorteil außer Optik, Preis: Täuscherstoff (ein Kill reicht für 2-4 Tränke). Tell für Gegner: Rüstung bleibt wie Vanilla verborgen, Schritt-Geräusche und Schwebepartikel fallen weg, also zusätzlich Hinweis nur über Fußspuren im Schnee/Sand (Idee, nicht Teil).
**Optik-Idee.** Flaschenfarbe: sehr helles Grau-Transparent mit Schimmer, Tooltip-Name "Stille Unsichtbarkeit".
**Modul-Unabhängigkeit.** Nutzt nur den Vanilla-Effekt, eigener Potion-Eintrag `simplemobs:veiled_invisibility`, Registrierung wie Silence-Trank (siehe MobsRegistry, drei Loader unterschiedliche Brau-Events).
**Tests.** Gametest: Trank gibt Effekt ohne Partikelflag; Brauen mit Täuscherstoff/Redstone ergibt die richtigen Zieltränke; ohne Täuscherstoff kein Ergebnis.

## 4. Trugbild-Spiegel (optional)
**Funktion.** Item mit Nutzdauer 2 s (Halten): in Blickrichtung (Kegel 30°, 24 Blöcke) werden Fakes (Tag `simplemobs_fake`) und der verkleidete Deceiver als Umriss sichtbar (Client-Glow nur für den Nutzer) und der Deceiver wird für 3 s enttarnt (Verwandlung bricht ab). Hält man ihn in Haupt-/Nebenhand, ignoriert man Mirage-Effekte (reine Optik zählt trotzdem als "durchschaut"). Abklingzeit 20 s, 64 Haltbarkeit.
**Balance.** Kein Schaden, nur Information; der Deceiver bleibt eine Skill-Prüfung. Drop: selten (8 %), plus Rezept.
**Rezept.** `G A G / G C G / . S .`: Goldbarren (4), Amethystsplitter (1), Täuscherstoff (1), Stock (1).
**Optik-Idee.** Handspiegel mit goldenem Rahmen, Glasfläche schimmert violett; beim Benutzen kurzer Lichtblitz.
**Modul-Unabhängigkeit.** In `simplemobs`; Mirage-Bezug über Effekt-Id, nur wenn der SB-Kern da ist (sonst ignoriert).
**Tests.** Gametest: Enttarnung von Deceiver und Fakes im Kegel, nicht außerhalb; Abklingzeit; Client: Umriss.

## Reihenfolge und Aufwand
1. Hitbox/Verwandlung (Basis), 2. Trank (klein), 3. Umhang, 4. Spiegel. Je Teil eigener Branch, grünes Gate, dann nächster.

## Offene Fragen (max. 4)
- Q1: Umhang auf Brust-Platz (wie Elytra, Empfehlung) oder Kopf als "Kapuze"?
- Q2: Art-Wahl per Probe (Rechtsklick auf Mob, Empfehlung) oder per Crafting mit einem Spawn-Ei/Item?
- Q3: Hostile-Mobs ignorieren die Art nur, bis du sie angreifst (Empfehlung: ja), oder dauerhaft?
- Q4: Trugbild-Spiegel jetzt mitmachen (Empfehlung: ja, klein) oder streichen?

## Antworten Besitzer 2026-10-11 (gehen dem Konzept oben vor)
- **Q1 Platz:** Umhang auf dem Brustplatz (Rezept wie oben). Getragen: Mobs erkennen den Träger erst im **3-Block-Radius** (statt Folgeweite 16-35).
- **Q2 statt Probe/Art-Speicher:** Mit angezogenem Umhang **Rechtsklick (leere Hand) auf einen Mob = Verwandlung** in diesen Mob: Aussehen + Hitbox (Kamera-/Augenhöhe folgt), Mobs dieser Art halten dich für ihresgleichen (wählen dich nie als Ziel). Auch in **andere Spieler**: dann deren Skin und Rüstung/Ausrüstung (Momentaufnahme beim Verwandeln), Hitbox bleibt Spieler. **Schlagen oder Block abbauen = Rückverwandlung**, Umhang ablegen ebenso.
- **Q3 Enttarnung durch Angriff:** der getroffene Mob und alle Mobs im 5x5-Bereich um ihn erkennen dich normal (60 s); alle anderen danach im erhöhten, aber weiter verringerten Radius **8 Blöcke** für 30 s. Der Umhang verliert dabei 10 Haltbarkeit (einmal je Enttarnung).
- Bosse (Wither, Enderdrache, Älterer Wächter), Warden und Hollow Howler sehen durch (Tag `#simplemobs:sees_through_disguise`, `#c:bosses` optional) und sind keine Verwandlungsziele.
- **Q4:** Trank der Unsichtbarkeit ohne Partikel und Trugbild-Spiegel jetzt mitbauen.
- **Zusatz Spezialangriff „Verdoppeln“:** nach jeder Welle wirft der Deceiver mit Chance `p = 5 % + 75 % * t^2`, `t = clamp((1 - Leben/Max) / 0.8)`: volles Leben 5 %, 60 % Leben ~24 %, ab 20 % Leben 80 %. Abklingzeit 30 s. Verdoppelt werden nur seine lebenden Beschwörungen (keine wilden Mobs: Farmtiere bleiben sicher), höchstens 8 Kopien je Wurf und nie über die Obergrenze 24 (`HARD_CAP`). Kopien sind immer Trugbilder (1 Leben, kein Schaden, lautlos) und verschwinden mit seinem Tod – Begründung: 80 % echte Verdopplung im Endkampf wäre unfair/lag-anfällig, so bleibt es eine Wahrnehmungsprüfung.

## Umsetzung (claude-deceiver2)
- Basis: Hitbox/Verwandlung des Deceivers aus `claude-n38` (Cherry-Pick, `disguiseAs`, Stand-in-Render).
- `Disguise` (Spielerzustand serverseitig via Mixin-Interface `DisguiseHolder`; zu den Clients über ein unsichtbares, nie gespeichertes Begleit-Entity `simplemobs:disguise_marker` mit eigenen synchronisierten Daten = Verwandlungs-Id + 6 Ausrüstungs-Slots, weil NeoForge Entity-Daten an fremden Klassen wie `Player` verbietet; Vanilla-Tracking ersetzt so loaderspezifische Pakete), `CloakLogic` (reine Regeln: Radien, Zeiten, Chance-Kurve liegt in `DeceiverLogic`).
- Mixins: `TargetingConditions.test` (Erkennung), `Player` (Daten, Rechtsklick, Schlag, Tick), `Avatar.getDefaultDimensions` (Hitbox), `LivingEntity` (Treffer = Enttarnung, Daten-Update), `ServerPlayerGameMode.destroyBlock` (Abbau = Rückverwandlung), Client `EntityRenderDispatcher.extractEntity` (Stand-in Mob/Spieler mit Skin + Rüstung).
- Items: `deceiver_cloak` (Brust, 240 Haltbarkeit, Reparatur Täuscherstoff, Drop 3 % + 2 %/Looting), `trick_mirror` (2 s halten, Kegel 30° je Seite, 24 Blöcke, 20 s Abklingzeit, 64 Haltbarkeit, Drop 8 %); Tränke `veiled_invisibility` 3:00 / `long_veiled_invisibility` 8:00 (Unsichtbarkeit ohne Partikel). Abweichung: Spiegel lässt Fakes/Verkleidete 5 s für alle leuchten (Glowing) statt nur für den Nutzer; keine Config-Schlüssel (Konstanten in `CloakLogic`); Umhang nutzt sich nur beim Verwandeln (1) und Enttarnen (10) ab.

## Nachtrag Besitzer 2026-10-11 (2): Brustplatte, Drops, Trugbild-Tränke
- **Deceiver mit Brustplatte:** 280 Leben (statt 240) und 6 Rüstung (Eisen-Brustplatte). Droppt den **anderen** Drop doppelt: neues Item **Trugsplitter** (`simplemobs:illusion_shard`, 1-2 + Plünderung 0-1; mit Platte wird die Splitter-Tabelle `entities/deceiver_armor_bonus` ein zweites Mal gewürfelt).
- **Täuscherstoff:** nie doppelt, 50 % Chance (2-4 + Plünderung) für beide Varianten.
- **Trugbild-Tränke aus dem Trugsplitter (mit SimpleBuilding, Effekte liegen im SB-Kern):** Seltsamer Trank + Trugsplitter = Trank des Trugbilds („feindlich“: friedliche Mobs wirken feindlich), + Fermentiertes Spinnenauge = umgekehrtes Trugbild („friedlich“: feindliche wirken friedlich), Redstone = 8:00, Wurf/Verweil wie Vanilla (vorhandene SB-Rezepte). Rezept mit `mod_loaded`-Bedingung (Regel 4). Keine „verstärkte“ Stufe: Mirage wertet die Stärke nicht aus, eine Stufe II wäre wirkungslos. Ohne SimpleBuilding ist der Splitter reines Sammel-/Handelsgut (Abweichung von Regel 1 bewusst: der Effekt selbst gehört dem Kern).

## Nachtrag 48 (Besitzer 2026-10-12), Plan + Umsetzung auf claude-deceiver3
1. **Umhang am Kopf:** `Equippable` HEAD statt CHEST (Brustplatz frei für Rüstung); Haltbarkeit/Abfragen auf den Kopf-Slot. Getragen: Kapuze (eigene Pixel-Art im Kopf-/Hut-Bereich der Rüstungs-UV, `deceiver2_textures.py`).
   Verkleidung überall sichtbar: Welt/Third Person (vorhanden), **Inventar-Spielermodell** (`InventoryScreen.extractRenderState` zeichnet den Stellvertreter), **Ego-Hände** (`FirstPersonHandsAndItemsRenderer.renderPlayerHand`: Spieler-Gestalt = Arm mit dessen Skin; Mob mit Humanoid-Modell = dessen Arm mit dessen Textur; Gestalt ohne Arme = keine Hand).
   **Schlag in die Luft** (jeder Angriffsversuch, `ServerboundPunchPacket` → `handlePunch`) beendet die Verwandlung (ohne Enttarnung/Haltbarkeit, die bleibt Treffern vorbehalten).
2. **Trick-Spiegel ausbalanciert** (bleibt in Rezept/Loot): 3 s hochhalten (vorher 2), 60 s Abklingzeit (20), Kegel 20° (30) bis 12 Blöcke (24), Leuchten 2 s (5), höchstens 3 Täuschungen je Nutzung (nächste zuerst). Wirkt damit etwa einmal pro fünf Deceiver-Zyklen und nur auf einen kleinen Ausschnitt der Welle: leichte Hilfe statt Lösung.
3. **Shellker-Schale:** Vanilla `item/shulker_shell.png` mit Grundgestein-Rampe + violettem Innenrand (Shellker-Farben), Mojang-abgeleitet → `vanilla_derived.py` (neu: Modul-Ausgaben `MODULE_OUTPUTS`, `--module`), Gradle `gradle/module-vanilla-derived.gradle` für die simplemobs-Loader, nicht im Git (auch nicht die Wiki-Kopie).
