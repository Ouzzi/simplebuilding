# PLAN B-TRIMSTATS27 – Messungs-Zeiger der Besätze nach Simple Resonance (MOB27-3 R2, 11.11.2026)

Branch `b-trimstats27`, Basis `hub/b-bundle180-gen` 7def212d6 (181-gen gab es nicht). Punkt #3 aus /root/sb-tools-b/prio27.md.
Vorlauf: KONZEPT-A-WP22 §2.1/§2.3 (R2 = „Umzug Messung aus dem Kern (TrimMultiplierLogic, Tracer, Payloads, Panel) + Datenimport“),
PLAN-A-ZONES31 (Stufe 1: Resonance parallel, Import), PLAN-B-MOB32/TRIMMOVE01 (Fähigkeiten → Simple Mobs, T7 „R2 offen“), b-qw07.

## 1. Zielmodul
Wortlaut T7/MOB32: „Messungs-Zeiger R2 (Tracker/Payloads/Panel → **Simple Resonance**)“, Konzept §2.1: simpleresonance bekommt
Aktiv-Zeit-Tracker, Resonanz-Panel (`TrimStatsPanel` + Tooltip), Netz, Config `trimBenefitBaseMultiplier`. Simple Mobs behält die Fähigkeiten.

## 2. Ist-Stand (erhoben)
- Simple Resonance misst schon alles selbst (`ResonanceService`: Faktoren XP/Überleben/Kampf seit Tod, Aktivzeit, Import des
  Kern-Blocks `SimpleBuildingData` per `LegacyImport`, Config `trimBenefitBaseMultiplier` einmalig aus simplebuilding.json übernommen,
  Vertrag `ResonanceContracts.trimMultiplier`). Es fehlt: Netz zum Client, Panel, Kern-Zeiger.
- Kern 26.3 misst noch selbst: `TrimMultiplierLogic` (Server+Client), `SurvivalTracerMixin` (Zähler, NBT, `TrimDataPayload`,
  `SurvivalSyncPayload` je Sekunde), `TrimStatsPanel` (Knopf + Tooltip im Inventar, über `InventoryScreenMixin`, Rucksack über
  SimpleLib `InventoryExtrasClient` in `LibClientServices`), Befehl `/simplebuilding config setTrimMultiplier`, Option sichtbar.
- **Eigene Stärken-Config Simple Mobs ist schon da** (b-qw07: `MobsConfig.trimStrength`, simplemobs-server.json) → nichts zu tun.
- Gemeinsame Dateien: `common/src/shared` + `src/main` (Fabric) + `neoforge/` + `forge/` kompilieren für 26.2 **und** 26.3.

## 3. Entscheidungen (selbst getroffen)
- **E1 Twins statt Großumbau.** `TrimMultiplierLogic` und `TrimStatsPanel` ziehen mit ihrem alten Code nach `common/src/mc26_2`
  (26.2-Linie unverändert bis zum Port-Run); in `mc26_3/overlay` liegen gleichnamige **Brücken** mit derselben API: Werte per
  Reflection aus `com.simpleresonance.ResonanceBridge` (Kern importiert kein Modul, Regel 10). So bleiben alle gemeinsamen Aufrufer
  (TrimRates, TrimReferenceScreen, TrimHost, Client-Tests, Loader-Empfänger) unverändert und 26.2 baut weiter.
- **E2 Tracker aus, Block bleibt lesbar.** `SurvivalTracerMixin` bleibt gemeinsam (26.2 braucht ihn, die Loader-Netzregistrierung ist
  gemeinsam), zählt/sendet auf 26.3 aber nichts mehr (`McVersion.RESONANCE_IN_SB=false`): kein Tick-Zähler, keine Kills, keine
  Payloads, kein Rebase beim Tod. Lesen/Schreiben von `SimpleBuildingData` bleibt (Konzept §2.3: „der Kern behält den Block 1 Version
  lang lesbar“), damit `LegacyImport` jeden Spieler sicher einmal übernimmt. Die alten Payload-Typen bleiben auf 26.3 registriert,
  werden aber nie gesendet (Löschen mit dem 26.2-Port-Run, sonst Zwillinge von SimplebuildingClient/ModMessages/Neo/Forge-Netz).
- **E3 Netz im Modul-Namensraum.** `simpleresonance:stats` (S2C, je Sekunde aus `ResonanceService.tick`): gesammelte XP, Weg,
  Aktivzeit, Monster-/Tier-Kills, Schaden seit dem Tod + Basis. Client-Speicher `ResonanceClient`, geleert beim Trennen.
- **E4 Panel im Modul.** `client/ResonancePanel` (alter Code, Lang `gui.simpleresonance.stats.*`, Sprites `simpleresonance:resonance_*`
  1:1 kopiert, keine neue Optik) + `ResonanceLayout` (Herz-Stufen). Vanilla-Inventar: eigener Client-Mixin `InventoryScreenResonanceMixin`
  (gleicher Platz 76/34 – in Vanilla frei, also auch ohne SB); Rucksack (Simple Storage): `InventoryExtrasClient.register`. Der Kern
  zeichnet auf 26.3 keinen Knopf mehr (`TrimStatsPanel.render` der Brücke ist leer), der Rahmen des Extra-Kopf-Platzes bleibt Kern.
  `TrimStatsLayout` behält nur die Geometrie; Herz-Stufen ziehen in den 26.2-`TrimStatsPanel` bzw. `ResonanceLayout`.
- **E5 Config.** Wert lebt in `simpleresonance.json` (Übernahme gab es schon). SB-Option `trimBenefitBaseMultiplier` auf 26.3
  ausgeblendet (`ConfigOptions.MOVED_TO_RESONANCE`), `/simplebuilding config set/getTrimMultiplier` auf 26.3 nicht registriert
  (Ersatz `/simpleresonance config`); Admin-Buch 26.3 nennt den neuen Ort.
- **E6 Tests.** Kern-Tests der Messung/des Panels/des Befehls (`TrimWiringTests` 7 Stück, `HopperAndTrimTests` XP-Kurve) testen Code,
  der auf 26.3 nicht mehr läuft → gelöscht (wie T6 in TRIMMOVE01; 26.2 verliert sie bis zum Port-Run). Neu in Simple Resonance:
  `stats_payload_roundtrip`, `stats_measure` (Messung liefert die Werte), `bridge_values` (Brücke Server = Vertrag), `panel_layout`
  (Herz-Stufen, Platz neben dem Rezeptbuch), `panel_tooltip_keys` (Lang EN/DE). Kern neu: `trim_wiring_game_test_core_reads_resonance_from_the_module`
  (Kern-Brücke liefert die Modul-Werte; ohne Modul Rückfall ohne Absturz per Reflection-Fehlerpfad). Client-Smoke-Schritte der alten
  Synchronisation laufen nur noch auf 26.2 (`ClientTestVersion.CORE_RESONANCE_TRACKER`).
- **E7 Kein IdMove** (keine Items/Blöcke/Komponenten ziehen um; Sprites/Lang bekommen neue Namen im Modul).
- **E8 Nur Fabric verdrahtet.** Payload/Client-Hooks auf NeoForge/Forge offen (Regel „nur 26.3 Fabric“); dort zeigt das Panel frische Werte.

## 4. Dateien
Modul: `ResonanceBridge`, `net/ResonanceStatsPayload`, `client/{ResonanceClient,ResonancePanel,ResonanceLayout}`,
`mixin/client/InventoryScreenResonanceMixin`, `ResonanceService` (stats/send), Fabric `ResonanceFabric`(+Payload), `ResonanceFabricClient`,
fabric.mod.json, mixins.json, Lang EN/DE, Sprites, Tests + Adapter, wiki/manual.json (+ Guide-Generator).
Kern: mc26_2 ← `TrimMultiplierLogic`, `TrimStatsPanel`; overlay-Brücken; `TrimStatsLayout`; `SurvivalTracerMixin`; `McVersion` (beide);
`ConfigOptions`, `ConfigOptionTests`, `SimplebuildingCommand`, `LibClientServices`, Tests/Katalog/Adapter, `ClientTestVersion` (beide), SmokeClientTest,
Admin-Buch-Lang 26.3, Doku (PRINZIPIEN-MODUL-UNABHAENGIGKEIT Stand), Queue, Memory.

## 5. Risiken
Kein lokales Gradle: javac-Harness gegen MC 26.3 + ein sbt. Client-Teile (Mixin, Panel) ohne Client-Lauf (Regel 16) → Client-Sicht im
Bündel. Reflection-Signaturen: Test `trim_wiring_game_test_core_reads_resonance_from_the_module` fängt Tippfehler.

## 6. Verifikation
Lokal: javac, check_dup_impl, multimod, check_lang_keys, check_tooltip_text, check_client_mixins, precheck --fast.
Ein sbt: Compile Kern (:compileJava = 26.2) + :mc26_3:fabric + simpleresonance fabric; `module-simpleresonance-fabric-263`,
`module-simpleresonance-standalone-fabric-263`, `fabric-263 --filter 'simplebuilding:data_integrity*,simplebuilding:*trim*'` (+ config_option*).
