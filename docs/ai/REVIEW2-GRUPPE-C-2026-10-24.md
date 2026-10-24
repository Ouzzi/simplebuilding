# Review 2, Gruppe C (24.10.2026, Branch `b-rev2c24`)

Auftrag Besitzer: Sub-Mods fertig machen, jede Datei einzeln auf Vollständigkeit und Probleme prüfen, dazu
Verbesserungsvorschläge. Gruppe C: simplequestionary, simplerespawn, simpleriding, simplesandwiches, simpleshops, simplesounds,
simplestats, simplestorage, simpletools, simpletrims, simpletweaks, simplevisuals, simplewanderers, simpleweather, wiringexample;
dazu Kern Simple Building auf den 26.3-Fabric-Pfaden (Schwerpunkt Mixins, Netzwerk, Migration/IdMoves). Nur MC 26.3 + Fabric
(NeoForge-/Forge-Quellsätze nicht geprüft). Plan: `docs/ai/PLAN-B-REV2C-2026-10-24.md`.
Nicht doppelt gemeldet: REV24 (`REVIEW-CODE-2026-10-24.md`; für Gruppe C M-1, M-3, M-5, M-8, M-9, M-10, M-12, N-1, N-3, N-4,
N-6, H-1, H-3 laut Queue behoben bzw. mit Probe), Daten-Audit `AUDIT-MODULE-2026-10-24.md` (b-audmods24: Lang, Rezepte, Tags,
Loot, JEI-Lücken, Config-Tooltips), JEI (b-jei24), Config-Befehle (b-cmdcfg24).

Schwere: P1 Absturz/Datenverlust/Exploit, P2 Fehlverhalten, P3 Qualität, P4 Vorschlag.

## Methode
1. Werkzeug-Durchlauf über **jede** Datei (Skript im Scratchpad, nicht im Repo): alle 2302 JSON-/mcmeta-Dateien der Gruppe
   parsebar; jede Java-Datei auf TODO/FIXME/Platzhalter, `@Redirect`/`@Overwrite`/`@ModifyConstant`, leere/breite `catch`,
   `printStackTrace`, Threads/Executor, statische Sammlungen, Netz-Empfänger, Client-Importe außerhalb des Client-Teils,
   Importe interner Klassen anderer Module.
2. Mixin-Configs gegen Klassen (jede gelistete Klasse vorhanden, jede `@Mixin`-Klasse gelistet, Client-Mixins im
   `client`-Abschnitt), `fabric.mod.json` gegen Entrypoints und Mixin-Configs – Gruppe und Kern (168 Kern-Einträge).
3. Lang EN/DE je Modul: Schlüssel-Parität und englische Texte in `de_de.json`.
4. `tools/check_missing_textures.py`, `check_empty_tags.py`, `check_loot_format.py`, `check_load_conditions.py`,
   `audit_data.py all` (Funde mit dem Daten-Audit abgeglichen).
5. Lesen: alle Server-Netz-Empfänger (Kern `ModMessageHandlers`, `TweaksNetwork`; Storage `BundleNet`/`BackpackNet`; Tweaks
   `DeedService` + Payload-Codecs), Persistenz (Block-Entities, SavedData, eigene JSON-Dateien), Mixins mit Spielwirkung, und die
   Module, die REV24 nicht oder nur als Stichprobe gesehen hat (sandwiches, questionary, stats, riding `RidingSecurity`,
   respawn, visuals, sounds, weather, wiringexample, tweaks Play/Admin-Zustand).
6. Migrationstests (IdMoves) im Kern und in der Gruppe auf den 26.3-Paletten-Schlüssel `id` geprüft.

Nicht Zeile für Zeile gelesen (nur Werkzeug-Durchlauf + Stichprobe): Client-Screens/Renderer, generierte Guide-/Discover-Klassen,
die großen Testklassen, simpleshops-Menüs außer `ShopBuyerMenu`, simplestorage außer Netz/Migration/Kiste.

## Migration / IdMoves (Auftrag P1)
- Kern: kein Test setzt `Name` als einzigen Schlüssel. `MigrationWorldTests` (mc26_3) lädt echte Chunk-Daten über
  `SerializableChunkData` und kennt beide Schlüssel; `OreDetectorTests`, `PlacedTemplateTests`, `RedstoneLinesTests`,
  `WorldUpgradeTests`, `StructureStation` lesen schlüsselunabhängig bzw. über `BlockState.CODEC`. Kein Fund.
- simpletweaks `CompatibilityTests.blocks`: Palette mit `id` über `BlockState.CODEC` – richtig.
- simpletrims, simpletools: nur Items/Rezepte/Advancements verschoben, keine Blöcke – Paletten-Prüfung entfällt.
- **simplestorage (Testlücke, behoben):** die Migrationstests prüften verschobene Blöcke nur über `BLOCK.getValue(old)`, nie eine
  Paletten-Zeile. Neu in `migration_from_simplebuilding`: für jeden verschobenen Block (Kisten, Truhen, Shulker, Tisch, Rucksäcke,
  abgestelltes Bündel) ein Nicht-Standard-Zustand, über `NbtUtils.writeBlockState` geschrieben, Schlüssel `id` geprüft und auf die
  alte `simplebuilding:`-Id gesetzt, dann über `BlockState.CODEC` (Chunk-Codec) und `NbtUtils.readBlockState` gelesen – muss
  derselbe Zustand sein (Vorbild 9f14ea143).
- Gruppe A (nicht hier geändert): `simplelib IdMovesTests:103`, `simplecrucibles BucketTests:935`, `CrucibleTests:232` setzten
  `Name`; auf b-rev2a24 bereits korrigiert.

## Kern Simple Building (26.3 Fabric)
Geprüft: `ModMessages` (14 C2S, alle über `server.execute`), `ModMessageHandlers` (Reichweiten, Klammern, Enum-Namen, Signier-Budget,
Slot-/Item-Prüfung), Payload-Codecs (Längengrenzen gesetzt), `TweaksNetwork` (Boost-Fenster, Laser-Rate/-Reichweite), 168
Mixin-Einträge (keine fehlende/ungelistete Klasse, kein Client-Import im Common-Abschnitt), verbliebene `@Redirect`
(nur Client-HUD, geparkt auf b-revmixin24-client), `datafix/*`, `IdMovesHook`, `BlockCodecs` (Platzhalter absichtlich, 26.3 kennt
keine Block-Codecs).
- Kein P1/P2.
- P4 `mc26_3/overlay/resources/data/simplebuilding/tags/item/craftable.json` und `not_allowed_in_backpack.json` nennen 33 alte
  `simplebuilding:`-Ids (Rucksäcke, Truhen, Shulker, Hämmer, Enderit …), `astral_vault`/`nihil_vault`-Rezepte
  `simplebuilding:enderite_nugget`, `c:ores/*` die alten Erz-Ids. Lädt nur dank IdMoves-Rückfall (Pflicht-Sub-Mods); sauberer:
  neue Ids mit `required:false`. (REV2-C-1)
- P4 17 Injektoren mit `require = 0` (Kern + Gruppe) fallen bei Ziel-Änderung still aus; Vorschlag: Prüflauf mit
  `-Dmixin.debug.countInjections=true` im Testrunner, Warnung je 0/1. (REV2-C-2)
- Hinweis audit_data: `LANG-BLOCK-NAME-MISSING` für die Astral-/Nihil-Wandfackeln ist Absicht (`overrideDescription` der
  Standfackel), kein Fund.

## simplequestionary (48 Dateien, 16 Java)
Geprüft: Menü (1 Slot, Quickmove, Rückgabe beim Schließen, `stillValid`), Tisch-Block, Fakten (`QuestionaryFacts`), Payload,
Daten/Seiten, Config, Lang.
- P4 `QuestionaryFacts.compute` läuft bei jeder Slot-Änderung über alle Rezepte des Servers (Spam per Shift-Klick möglich, je Klick
  wenige ms). Vorschlag: Ergebnis je Item bis `/reload` cachen. (REV2-C-3)

## simplerespawn (49 Dateien, 26 Java)
Geprüft: `RespawnServer` (Zustand, Bluten, Wiederbelebung, echter Tod mit Original-Quelle, Absturz-Tag, Logout = Tod),
Befehle (Rechte Stufe 2 für down/revive), alle Mixins, Payloads, Config.
- P4 `mixin/ServerGamePacketListenerMixin` bricht Pakete schon am `HEAD` ab (Netz-Thread, vor der Thread-Übergabe); thread-sicher
  (ConcurrentHashMap), aber ein verworfener Container-Klick lässt den Client ohne Neuabgleich. Vorschlag: wie RidingPacketMixin
  nach `ensureRunningOnSameThread` einhängen, bei Container-Klick `sendAllDataToRemote`. (REV2-C-4)

## simpleriding (132 Dateien, 49 Java)
Geprüft: `RidingSecurity` (Paket-/Distanz-/Höhen-Grenzen, Sprung-Freigabe), `RidingPacketMixin` (nach Thread-Übergabe), Config
(alle Werte begrenzt), Hufeisen, Effekte, Loot, Lang.
- **P3 behoben** `client/RidingConfigScreen`: Speichern schrieb nicht atomar → `ConfigFiles.writeAtomic`.
- P4 `RidingSecurity.acceptMove` lehnt jede Aufwärtsbewegung eines Reittiers außerhalb Wasser/Lava ohne freigegebenen Sprung ab:
  Schwebeeffekt (Shulker-Geschoss), Schleimblock-Sprung, Explosions-Rückstoß führen zu Zurücksetzen. Vorschlag: Aufwärts erlauben,
  solange das Reittier Levitation hat bzw. der Server selbst einen Impuls gesetzt hat. (REV2-C-5)

## simplesandwiches (566 Dateien, 37 Java)
Geprüft (REV24 hatte das Modul nicht gelesen): Schneidebrett (Zustandsmaschine, Drops, Sync, Laden), Milch-Kessel (Reifen per
Block-Tick, Trichter-Ernte mit Protection-API, Altinhalte), Bündel-Essen (Mixin + Logik), Messer (Melone, Kuchen), Sandwich-Item,
Slippery-Mixin (Test `slippery_friction` deckt `require = 0` ab), Discover-Mixin, Lang.
- Kein P1/P2.
- P4 `item/BundleEating.finish` isst, was beim Ende oben liegt – ändert sich der Inhalt während des Essens, wird etwas anderes
  gegessen als begonnen. Vorschlag: Item beim Start merken, bei Wechsel abbrechen. (REV2-C-6)

## simpleshops (263 Dateien, 49 Java)
Geprüft: Käufer-Menü (Button-Ids mit Revision, `stillValid`, Bezahl-Rückgabe), Handel transaktional, H-3-Probe vorhanden
(`ender_shops_survive_an_unreadable_item`). Kein neuer Fund.

## simplesounds (32 Dateien, 16 Java)
Rein clientseitig, keine Pakete. **P3 behoben** `SoundClient.save` → `ConfigFiles.writeAtomic`.

## simplestats (64 Dateien, 42 Java)
Geprüft: `StatsRuntime` (Puffer je Stack, Abschluss am Tick-Ende, Waffen-Zuordnung per Uid), alle Mixins (nur Server-Seite),
Befehle (Stufe 2), Komponente/Budget. Kein Fund.

## simplestorage (1323 Dateien, 138 Java)
Geprüft: Netz (`BundleNet`, `BackpackNet`: Slot-Grenzen, Schleichen/Reichweite/Rechte, Server wählt den Rucksack), IdMoves-Tabelle
(Blöcke, Items, BE-Typen, Komponenten, Verzauberungen, Rezepte, Advancements), Migrationstests, Kisten-Laden.
- **P2 (Testlücke) behoben**: Paletten-Migration aller verschobenen Blöcke unter `id` (s. oben).
- P4 `crate/CrateBlockEntity.loadAdditional`: `Order` über strikten `ItemStack.CODEC.listOf()` – ein unlesbares Element verliert nur
  die Reihenfolge (Inhalt bleibt über `ContainerHelper`). Nur Hinweis.

## simpletools (126 Dateien, 48 Java)
Geprüft: Biom-Pinsel (Rechte je Zelle, Dimension, Strukturen, Chunk-Neuversand), Death Vault (REV24 M-9), Soulbound, Rahmen,
Ofen-Lava, Config.
- **P3 behoben** `brush/BiomeBrush.paint`: die Struktur-Abfrage lief vor der Prüfung, ob der Chunk geladen ist (die Abfrage liest
  Chunks selbst); jetzt erst geladener Chunk, dann Struktur.

## simpletrims (149 Dateien, 33 Java)
Geprüft: Upgrade-Rezept, Licht (REV24 M-5), IdMoves, platzierte Vorlagen.
- **P3 behoben** `recipe/TrimUpgradeRecipe.placementInfo` wurde bei jedem Aufruf neu gebaut; jetzt einmal wie Vanilla.

## simpletweaks (298 Dateien, 237 Java)
Geprüft: Urkunden-Netz (`DeedService`: Hand/Item vom Server, Drossel, Rechte aus dem Ledger, Längengrenzen der Payloads),
Ledger (`ClaimStore`: Größengrenze, Symlink-Schutz, atomar, Sicherungen v1/v2), Admin-/Play-Zustand, Freeze, Restart, Block-Log-Seiten,
statische Spielerlisten.
- **P2 behoben** `admin/AdminState.load`, `play/PlayState.load`: eine unlesbare Datei (z. B. von Hand kaputt editiert) gab einen
  leeren Zustand, und das nächste Speichern überschrieb sie – Vanish/Freeze/Wartung bzw. Spielzeiten, „zuletzt gesehen“, PvP-/
  Phantom-Wahl, Serien waren still weg. Jetzt wird die Datei vorher als `<name>.broken-<Zeit>` beiseitegelegt (wie
  `ClaimPictures`). Tests `unreadable_state_file_kept` (Admin + Play).
- P4 kleine statische Spielerlisten ohne Abmelde-Aufräumen (`Freeze.AT`, `BlockLogCommands.PAGES`, `DeedService.LAST*`,
  `Pvp.TOGGLED`, `Coords.LAST`, `Inspect`): klein und begrenzt; beim nächsten Anfassen auf `PlayerState.onLeave` (simplelib)
  umstellen. (REV2-C-7)

## simplevisuals (67 Dateien, 45 Java)
- **P3 behoben** `lang/de_de.json`: 31 Config-Bezeichnungen standen auf Englisch (Schadensanzeige, Biom-Info,
  Geschwindigkeitslinien, Item-Tooltips, Aufsammel-Anzeige) – übersetzt.
- **P3 behoben** `Visuals.save` → `ConfigFiles.writeAtomic`.
- P4 `Visuals.save` wirft bei Schreibfehler eine `IllegalStateException` (aus dem Config-Bildschirm = Absturz); loggen statt werfen.
  (REV2-C-8)

## simplewanderers, simpleweather, wiringexample
Gelesen bzw. per Durchlauf: Spawner (REV24 M-12), Entity-Speichern, Wetter-Config mit QoL-Übernahme, Wiring-Token. Kein Fund.

## Querschnitt
- Modul-Unabhängigkeit: keine Importe interner Kern-/Fremdmodul-Klassen außer per Reflexion in Tests (`ToolClaimTests`) und Javadoc.
- Lang-Parität EN/DE in allen 15 Modulen vollständig; **P3 behoben** britisches Englisch in 2 Guide-Seiten (simpletools 9
  „behaviour“, simpletweaks 51 „grey“; `wiki/manual.json` + Lang). (REV2-C-9, erledigt)

- **P2 behoben** `fabric.mod.json` von simplerespawn, simplesounds, simplevisuals: nutzen Fabric API (Netzwerk, Befehle,
  Client-Ereignisse, Kreativtab), erklärten sie aber nicht als Abhängigkeit – ohne Fabric API Absturz beim Start statt der
  Meldung „fehlende Abhängigkeit“. Jetzt `"fabric-api": "*"` wie die anderen Module.
- **P3 behoben** simpletweaks `fabric.mod.json` `suggests` nannte `simpledimension` (Modul heißt `simpledimensions`). In
  `neoforge.mods.toml`/`mods.toml` steht derselbe Tippfehler – beim Port (REV2-C-10).

## Geänderte Dateien
`modules/simplevisuals/.../lang/de_de.json`, `simplevisuals/Visuals.java`, `simplesounds/SoundClient.java`,
`simpleriding/client/RidingConfigScreen.java`, `simplestorage/test/StorageTests.java`, `simpletools/brush/BiomeBrush.java`,
`simpletrims/recipe/TrimUpgradeRecipe.java`, `simpletweaks/admin/AdminState.java`, `simpletweaks/play/PlayState.java`,
`simpletweaks/admin/AdminTests.java`, `simpletweaks/play/PlayTests.java`, Fabric-Adapter `AdminGameTest`, `PlayGameTest`,
`fabric.mod.json` von simplerespawn/simplesounds/simplevisuals/simpletweaks, Guide-Texte simpletools/simpletweaks (manual.json + Lang).
