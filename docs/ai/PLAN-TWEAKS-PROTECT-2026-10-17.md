# Plan: Simple Tweaks – Schutz (Anti-X-Ray, Freecam-Regel, weitere Schutzfunktionen) – 17.10.2026, Sitzung A

Branch `claude-tweaks-anticheat` (Basis `claude-wave1` 23638be3a). Besitzer-Wunsch 17.10. („ja bitte umsetzen“).
Alles Neue im Paket `com.simplebuilding.modules.simpletweaks.protect`, eigene Config `config/simpletweaks-protect.json`,
eigene Mixin-Config `simpletweaks.protect.mixins.json` (Server-Liste + Client-Liste), Lang `simpletweaks.protect.*` (EN/DE),
Tests `ProtectTests` (Fabric `ProtectGameTest`, NeoForge/Forge über die Test-Map, Präfix `protect_game_test_`).
Loader-Einstiege bekommen je eine Zeile (`ProtectFabric`/`ProtectNeo`/`ProtectForge`), das Netz-Paket hängt an den
bestehenden Deed-Kanal (`DeedNet.send`).

## Ist-Zustand (Survey)
- Chunk-Versand 26.3: `PlayerChunkSender.sendChunk(connection, level, chunk)` baut **je Spieler** ein
  `ClientboundLevelChunkWithLightPacket`, dessen `ClientboundLevelChunkPacketData(LevelChunk)` die Abschnitte mit
  `LevelChunkSection.write` in `byte[] buffer` schreibt. Einziger Server-Erzeuger dieser Pakete (Jar-Suche).
- Blockänderungen: `ServerLevel.sendBlockUpdated` → `ServerChunkCache.blockChanged(pos)` → Vanilla schickt am Tick-Ende
  den echten Zustand an alle, die den Chunk sehen. Abbau, Explosion, Kolben, Flüssigkeiten laufen alle darüber.
- Unsere Mods (Survey 17.10.): Reichweite nur über das Vanilla-Attribut `block_interaction_range` (Range-Verzauberung
  `ModEnchantments:99`, Trims `TrimAttributeHandler:58`, beide serverseitig); Kreativ-Baustab/Blaupause zielen per
  Server-Raycast (`CreativeReach`, 1024 Blöcke) aus „Benutzen in die Luft“ – kein weit entferntes useItemOn-Paket.
  Hammer/Vorschlaghammer/Strip-/Vein-Miner brechen Zusatzblöcke serverseitig mit `gameMode.destroyBlock(pos)` aus dem
  Abbau-Hook des einen angeklickten Blocks (keine Zusatz-Pakete). Baustab/Blaupause setzen per `level.setBlock`.
  Abbautempo: `BlockStateBaseMixin` (teilt nur, langsamer), Kopf-Fähigkeiten, Sledgehammer/Meißel-Overrides,
  Simple Tools `FullDurabilityEfficiencyMixin` (Config-Faktor) – alle in gemeinsamem Code, also auch am Server.
  Kein `entity_interaction_range`-Bonus. Simple Movement: Stufenhöhe per Attribut (serverseitig); Simple Riding nur Reittier.
- Erzdetektor (`OreDetectorItem.ping/findTarget(ServerLevel…)`, `PlacedDetectors`) sucht **serverseitig** → unberührt.
  Simple Maps baut Kartendaten serverseitig (`Reveal`, `WayfinderData`) → unberührt. Claims: rein serverseitig → unberührt.
- Client liest Blöcke nur für Vorschauen: Hammer-/Vein-Miner-Vorschau (`MultiBlockBreakingSupport`, `MiningUtils.isOre`)
  sieht verdeckte Erze als Stein – nur die Vorschau weicht ab, das Ergebnis entscheidet der Server (verdeckte Erze sind
  per Definition nicht sichtbar; sobald ein Nachbar fällt, wird das Erz echt nachgeschickt).
- Freecam (hashalite/MinecraftFreecam, **MIT**, nur Client): 1.3.6 (1.21.11) liest `ModConfig.INSTANCE.collision.ignoreAll`
  bzw. `CollisionBehavior.isIgnored(Block)`; ab 1.4/1.5 (einzige Builds für 26.x, 1.5.0+mc26.3) Schnittstelle
  `net.xolt.freecam.config.ModConfig` mit `ignoreAllCollision()` und `ignoreCollisionWith(Block)`, Implementierung
  `net.xolt.freecam.config.model.ModConfigImpl`. **Kein Server-Protokoll**, keine Berechtigung, kein Kanal; einzige
  „Server-Beschränkung“ ist eine clientseitige Liste von Server-Adressen (`isRestrictedOnServer`), die Freecam ganz abschaltet.
- `/simpletweaks`-Wurzel: Branch `claude-tweaks-cmd` (f9df76ab9, noch nicht in wave1) hängt jeden `/tweaks`-Zweig per
  `TweakCommands.alias` an. Darum registriert Schutz seine Befehle als Zweig `/tweaks protect …` – nach dem Merge reicht
  im Schutz-Registrierer eine Zeile `TweakCommands.alias(d)` (Notiz in der Übergabe), damit `/simpletweaks protect …` entsteht.

## 1. Anti-X-Ray (Standard AN)
- Mixin `PlayerChunkSender.sendChunk` (HEAD/RETURN) merkt sich den Empfänger in einem ThreadLocal; Mixin am Ende von
  `ClientboundLevelChunkPacketData(LevelChunk)` ersetzt `buffer` durch die verschleierte Fassung, wenn der Empfänger nicht
  ausgenommen ist und die Dimension konfiguriert ist. Kein weiterer Paket-Typ wird angefasst.
- Verschleierung (Modus 1, wie Paper engine-mode 1): Abschnitte oberhalb `maxY`, nur Luft, oder ohne versteckten Block
  (`LevelChunkSection.maybeHas`) werden übersprungen. Sonst wird jeder versteckte Block geprüft: berührt er an keiner der
  6 Seiten einen Block ohne `isSolidRender()` (Luft, Wasser, Lava, Glas, Stufen …), wird er in einer Kopie des Abschnitts
  (`copy()`, copy-on-write) durch den Wirtsblock ersetzt: Überwelt `stone`, bei „deepslate“ im Namen oder y < 0 `deepslate`;
  Nether `netherrack`; End `end_stone` (je Dimension einstellbar). Nachbarn in fremden Chunks über `getChunkNow`; ist der
  Nachbar-Chunk nicht geladen, gilt die Seite als offen (lieber sichtbar lassen als echte Höhlenwände verfälschen).
- Modus 2 (Fake-Erze, Standard aus): zusätzlich werden verdeckte Wirtsblöcke mit `fakeOreChance` (Standard 0,15,
  0,01–1) durch ein zufälliges Erz der Liste ersetzt (Zufall aus der Position → bei erneutem Senden gleich).
- Aufdecken: Mixin am Ende von `ServerLevel.sendBlockUpdated` → für jeden der 6 Nachbarn, der versteckt sein könnte
  (versteckter Block oder in Modus 2 ein Wirtsblock), `blockChanged(nachbar)` → Vanilla sendet den echten Zustand.
  Gilt für Abbau, Setzen, Explosionen, Kolben, Flüssigkeiten. Nur geladene Chunks, keine Rekursion.
- Blocklisten je Dimension (`antiXray.dimensions`): `hidden` (IDs und `#tags`), `replacement`, `deepslateReplacement`,
  `maxY`. Standard: alle Vanilla-Erze inkl. Tiefenschiefer-Varianten, `ancient_debris`, `nether_gold_ore`,
  `nether_quartz_ore`, dazu `#c:ores` (Fremd-Erze). Überwelt maxY 320, Nether 128, End 256. Weitere Dimensionen per
  Eintrag; Schlüssel `*` = alle übrigen Dimensionen. Tags werden beim Serverstart und bei `/tweaks protect reload` aufgelöst.
- Ausnahmen: `exemptCreative`, `exemptSpectator` (Standard an), `exemptPermission` (−1 = aus, sonst OP-Stufe 0–4, Standard −1).
  Gilt für Chunks, die nach dem Wechsel gesendet werden (Relog oder Neuladen der Chunks zeigt echte Erze).
- Kosten: Prüfung nur beim Chunk-Versand + 6 Lookups je Blockänderung. Messung per GameTest (verschleiert vs. Vanilla,
  Zeit je Chunk im Log) und per `/tweaks protect bench`.
- Grenzen: echte Blockänderungen in verdeckten Bereichen (z. B. ein Mod setzt ein Erz tief im Stein) gehen unverändert
  hinaus; Erze, die direkt an Höhlen/Wasser liegen, bleiben sichtbar (wie bei Paper). Wirkt gegen X-Ray-Texturpakete und
  -Mods, weil der Client das Erz nie bekommt – auch Noclip-Blicke (Freecam) sehen nur Stein.

## 2. Freecam-Regel (Standard AUS)
- (a) Freecam hat keinen Server-Mechanismus → entfällt.
- (b) Client-Teil: Paket `simpletweaks:protect_rules` (Server → Client) beim Beitritt mit `freecamCollision`. Client-Mixin
  (`@Pseudo`, nur wenn Freecam geladen, `required:false`, `require=0`) auf `ModConfigImpl.ignoreAllCollision()` und
  `ignoreCollisionWith(Block)` → `false`, solange die Verbindung, für die die Regel kam, besteht. Ohne Freecam: nichts.
  Freecam 1.3.6 (nur 1.21.11) hat andere Ziele (`CollisionBehavior.isIgnored`, Feld `collision.ignoreAll`) – betrifft
  erst den 1.21.11-Port (Notiz im Port-Run).
- (c) `freecam.requireClient` (Standard aus): ist die Regel an und kann der Client das Paket nicht empfangen (kein Simple
  Tweaks), wird er mit `kickMessage` getrennt (übersetzbar mit englischem Fallback für Clients ohne Mod).
- Grenze: Das ist eine Maßnahme für ehrliche Clients; ein veränderter Client kann den Kanal melden und die Kollision
  trotzdem abschalten. Den Inhalt (Erze) schützt Anti-X-Ray serverseitig.

## 3. Weitere Schutzfunktionen (je Standard AUS, eigener Schalter)
- **seedHash:** Der Server schickt statt `BiomeManager.obfuscateSeed(seed)` einen zufälligen Wert (je Serverstart neu,
  Redirect in `ServerPlayer.createCommonSpawnInfo`; gilt für Login und Respawn/Dimensionswechsel). Der Client nutzt den
  Wert nur für das Biom-Rauschen an Grenzen (Farbübergänge minimal anders) – Seed-Cracker verlieren eine Quelle.
- **hideHealth:** Gesundheit anderer Spieler in Entity-Daten-Paketen wird als Maximum gesendet (0 bleibt 0 für die
  Todesanimation); Health-ESP/Anzeige-Mods sehen keine Werte mehr. Eigene Gesundheit, Reittiere, Mobs unverändert.
  Anzeige-Ziele der Anzeigetafel (`health`-Kriterium) bleiben, weil der Server sie bewusst sendet.
- **actionChecks (Reichweite/Schnellabbau, nur Protokoll, kein Auto-Bann):** misst nur, was der Client per Paket
  anfordert – Abbau-Aktionen (`handleBlockBreakAction`), Benutzen an einem Block (`handleUseItemOn`), Angriff und
  Interaktion mit Entities. Grenze je Spieler aus den **echten Server-Werten**: `isWithinBlockInteractionRange(pos, 1 + reachSlack)`
  bzw. `isWithinEntityInteractionRange(box, 3 + reachSlack)` (Vanilla-Toleranz + `reachSlack`, Standard 1,0) – enthält
  Range-Verzauberung, Trims, jedes Attribut fremder Mods. Schnellabbau: bei STOP_DESTROY wird Vanillas eigene
  Berechnung `state.getDestroyProgress(player, level, pos) * (Ticks+1)` gelesen (Werkzeug, Verzauberungen, Effekte,
  Attribute, unsere Mining-Mixins, NeoForge-BreakSpeed) und gemeldet, wenn sie unter `fastBreakRatio` (Standard 0,5;
  Vanilla akzeptiert ab 0,7) liegt. Hammer-/Vein-/Strip-Zusatzblöcke, Baustab, Blaupause, Meißel, Shaping Saw laufen
  serverseitig ohne diese Pakete → nie gemeldet. Kreativ (Sofortabbau) wird nie geprüft.
  Treffer: Zähler je Spieler, Zeile in `<world>/simpletweaks/protect-checks.log`, Hinweis an Admins ab `notifyPermission`
  (Standard 2) höchstens alle `notifyCooldownSeconds` (Standard 30). `/tweaks protect checks [player]` zeigt die Zähler.
  Bekanntes Risiko: Simple Tools `fullDurabilityBonusMultiplier` ist Server-Config; weicht der Client-Wert stark ab, kann
  der Client früher STOP senden → höchstens Protokollzeile, Vanilla bricht trotzdem erst zur echten Zeit ab.

### Verworfen (mit Grund)
- Entity-Verdeckung hinter Wänden (Anti-ESP): braucht Strahlen je Spieler-Paar und Tick – teuer; bricht Namensschilder,
  Simple-Maps-Spielermarker und Vanish-Logik. Nicht billig genug.
- Strukturen-Orten verbergen: `/locate` ist schon OP-only; Enderaugen/Entdeckerkarten sind Spielmechanik.
- Gamma/Fullbright (z. B. Gamma Utils): reine Client-Darstellung, der Server kann das nicht verhindern. Anti-X-Ray hilft
  nur gegen versteckte Erze, nicht gegen Helligkeit.
- Auto-Bann/Auto-Kick bei Prüfungstreffern: Besitzer wünscht nur Protokoll.

## Überschneidungen mit dem Modpack des Besitzers (Queue-Punkt, nicht jetzt umsetzen)
- Ledger ↔ T10 Block-Protokoll, InvRestore/Inv View ↔ T11/T13, Chunky ↔ T33 Pregen, Freecam ↔ Freecam-Regel,
  Litematica/Axiom (Client-Bau-Tools; Axiom setzt Blöcke per eigenem Paket serverseitig, Litematica Easy Place nutzt
  normale Pakete in Reichweite) ↔ actionChecks/Anti-X-Ray.
- Vorschlag: je Feature „automatisch aus, wenn der Partner-Mod installiert ist“ (Schalter `autoOffWith: [...]`, Prüfung
  über die Modliste beim Start, Hinweis im Log). Als Queue-Punkt eintragen.

## Befehle (nur Befehl = an)
`/simpletweaks protect status | reload | bench | checks [player]` (= `/tweaks protect …`, gespiegelt per `TweakCommands.alias`; Stufe `permission`, Standard 3).

## Tests (GameTests, alle drei Loader)
- `config_defaults_and_bounds`; `obfuscated_chunk_hides_enclosed_ore` (Paket dekodiert: verdecktes Erz = Stein,
  Tiefenschiefer-Erz = Tiefenschiefer); `exposed_ore_stays`; `mining_reveals_neighbours` (Abbau und Explosion melden
  die Nachbarn); `exempt_admin_sees_real_blocks` (Kreativ und OP-Stufe); `fake_ores_mode_two`; `bench_cost_logged`;
  `freecam_rule_payload_and_kick` (Regel aus: kein Kick; an + requireClient: Kick); `seed_hash_randomised`;
  `hide_health_of_others`; `checks_flag_far_break_not_server_breaks` (weiter Abbau-Paket wird gemeldet,
  `gameMode.destroyBlock` weit weg = Hammer-Zusatzblock und `level.setBlock` = Baustab/Blaupause nicht);
  `fast_break_uses_real_speed` (Effizienz-Werkzeug + Haste: früher STOP im erlaubten Rahmen nicht gemeldet);
  `commands_follow_switches`.
- Mit SimpleBuilding geladen (Integration) zusätzlich: echter Vorschlaghammer-Flächenabbau wird nicht gemeldet.

## Prüfungen
26.3 Fabric/NeoForge/Forge kompilieren + 26.2 `:compileJava`; `run.py module-simpletweaks-fabric-263`/`-neoforge-263`
+ Standalone; Regeneration nur auf sb-test (`discover.py`, `module_guides.py`, `generate_links.py`, `wiki/generate.py --all`),
dann `checkModuleGuides checkModuleData checkWiki checkDiscover checkMultimod`.

## Ergebnis (18.10., Stand 4228f3d41)
- Kompiliert: 26.3 Fabric/NeoForge/Forge + 26.2 `:compileJava`. GameTests: module-simpletweaks-fabric/-neoforge-263 und
  beide Standalone-Ziele je 137/137 (548/548; 13 neue `protect_game_test_*`). Fabric-Integration lief wegen fremder Last
  auf sb-test (Load ~57) über die 2400-s-Grenze, Bericht vollständig.
- Kosten (GameTest `bench_cost_logged`, synthetischer Chunk, 8 volle Stein-Abschnitte mit 3 % Erz und 4 % Luft – etwa das
  Dreifache echter Erzdichte): Vanilla-Serialisierung 0,01–0,04 ms, mit Anti-X-Ray 0,6–1,6 ms je Chunk-Paket. Abschnitte
  ohne versteckten Block (Himmel, Oberfläche) kosten nur `maybeHas`. Live messen: `/simpletweaks protect bench [radius]`.
- Nicht getestet: echter Client mit Freecam 1.5.0 (nur Mixin-Ziele per javap geprüft), Forge-GameTests, Chunk-Laden unter
  vielen Spielern. Mögliche Verbesserung bei Bedarf: Verschleierung je Chunk zwischenspeichern (gleiche Daten für alle
  nicht ausgenommenen Spieler) oder außerhalb des Server-Threads rechnen.
