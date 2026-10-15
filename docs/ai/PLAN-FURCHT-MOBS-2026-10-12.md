# Plan: Furcht-Mobs F-A „Dread“ und F-C „Hollow Watcher“ (2026-10-12)

Auftrag Besitzer 12.10. (Konzept `KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md`, Abschnitt „Furcht-Mob – Vorschläge“):
F-A und F-C bauen, im Modul `simplemobs`. Branch `claude-fear-mobs` (Basis `claude-wave1`).
Detailfragen hat der Agent selbst entschieden (Leitlinie: fair, angekündigte Angriffe, vanillanah) – unten mit (E) markiert.

## Ist-Zustand
- `simplemobs` hat Deceiver, Hollow Howler (Figur + Ritual), Shellker, Seelenfeuer-Lohe; Muster für Registrierung
  (`EndNetherMobs`: Sinks je Registry), Logik-Klassen (`*Logic`, testbar), Tests (`*Tests.addAll`), Config `MobsConfig`.
- Effekt **Zittern** gibt es bereits in SimpleBuilding (`simplebuilding:shivering`, nur Fadenkreuz-Versatz, Trank).
  Simple Mobs darf SB nicht importieren und muss allein spielbar sein (Prinzipien 1, 3, 4).
- Testzentrale: `claude-testcenter-mobs` (noch nicht in wave1) baut je Mod-Mob eine Zelle aus der Registry.

## Entscheidungen
1. **Zittern (E):** Simple Mobs nutzt `simplebuilding:shivering`, wenn SB geladen ist (öffentliche Registry-ID);
   ohne SB registriert es ein eigenes `simplemobs:shivering` (gleiche Wirkung). Nie beide zugleich.
   Wirkung: SB verschiebt das Fadenkreuz; Simple Mobs lässt zusätzlich **das Zielen wackeln** (Client: Blickrichtung
   pendelt geglättet, Stufe I ±0,6°, II ±1,8°; summiert sich nie auf, Bogen/Armbrust werden ungenau).
2. **Aura (beide Mobs):** alle 20 Ticks, Umkreis 16: Stufe II unter 6 Blöcken, sonst Stufe I bei Sichtkontakt
   (Sichtlinie Mob→Spieler). Dauer 3 s → Milch hilft nur ~1 s, dann legt die Aura nach. Nur Überlebens-/Abenteuerspieler.
3. **Immunität:** Ruhe-Amulett irgendwo im Inventar (E: kein Schmuck-Slot in Vanilla) oder Effekt **Fokus**
   (`simplemobs:focus`). Immun = Aura greift nicht, vorhandenes Zittern wird entfernt (auch Trank-Zittern).
4. **Dread – Beschwörung (E):** Golem-Figur, ausgelöst beim Setzen des Witherskelettschädels (wie Wither):
   ```
   . S .    S = Witherskelettschädel (zuletzt)
   C A C    C = Kette (#minecraft:chains), A = Amboss (#minecraft:anvil, jeder Schaden)
   . I .    I = Eisenblock;  . = Luft
   ```
   ≈ 40 Eisenbarren + Schädel = teuer, aber ohne Netherit erreichbar. Kein Wither-/Golem-Konflikt (anderer Kopf/Block).
   Nicht auf Friedlich. Config `dreadSummonEnabled`. Spawnt nie natürlich. Steigt 3 s lang auf (unverwundbar, Geräusch).
5. **Dread – Kampf:** 2,6 Blöcke, 260 Leben, Rüstung 14, Rückstoßfest, langsam (0,2). Jede Attacke angekündigt:
   - *Kettenkeule:* 14 Ticks Ausholen (Keule hoch, Kettengeräusch), dann 18 Schaden (Normal; Schwer 27) + Rückstoß.
     Ein geblockter Treffer legt den Schild **10 s** lahm (`getSecondsToDisableBlocking`).
   - *Bodenstampfer:* 24 Ticks Ausholen mit Partikelring (Radius 6) und Grollen, dann 10 Schaden + Wurf für alle am
     Boden im Ring; wer springt (nicht am Boden), weicht aus. Abklingzeit 8 s.
   - *Licht-Schwäche:* Licht (Block+Himmel) am Dread ≥ 12 → halber Schaden aller Angriffe, Aura höchstens Stufe I,
     Seelenflammen gedimmt + Rauch.
   - Drop (nur Spielerkill): 2–4 Schreckens-Kettenglieder (+Plünderung). 60 EP.
   - **Ruhe-Amulett** (E-Rezept): ` K ` / `GAG` / ` G ` – K Kette, G Schreckens-Kettenglied ×3, A Amethystscherbe.
6. **Hollow Watcher:** 4 Blöcke, dürrer Schatten mit vielen Augen und Endsteinsplittern, 200 Leben, Rüstung 8.
   - *Blick:* sieht ihn ein Spieler an (Sichtlinie, Blickkegel wie beim Enderman, nicht blind), bewegt er sich nicht
     und beginnt keinen Angriff (laufender Griff hält weiter).
   - *Fernrohr:* Blick durchs Fernrohr friert ihn **3 s** komplett ein (auch Schrei), Schneeflocken-Partikel.
   - *Teleport (angekündigt):* nicht angesehen und > 8 Blöcke entfernt: 12 Ticks Portal-Partikel am Ziel 2,5 Blöcke
     hinter dem Spieler + Geräusch, dann Sprung dorthin. Wird er dabei angesehen, bricht er ab. Abklingzeit 6 s.
   - *Griff:* 10 Ticks Ausholen (Arme strecken sich), dann 6 Schaden und **2 s** festgehalten, beim Loslassen 4 Schaden
     und Wurf. Abklingzeit 10 s. *Kralle:* 10 Schaden, 6 Ticks Ausholen.
   - *Schrei:* 20 Ticks Ausholen (Kopf zurück, Endermanschrei tief); danach Zittern II 10 s + Blindheit 3 s für Spieler
     im Umkreis 12. Ausgelöst, wenn ihn sein Ziel 5 s am Stück anstarrt (Konter gegen Dauerstarren) oder alle 20 s.
   - *Spawn:* nur End-Außeninseln (> 1000 Blöcke vom Ursprung), auf Endstein, 4,5 Blöcke Luft, Wurf alle 30 s je
     Spieler mit 4 % (Config `hollowWatcherSpawns`, `hollowWatcherChance`), höchstens einer im Umkreis 128, nicht friedlich.
   - Drop: 1 Wächterauge (+Plünderung bis 1). 50 EP.
   - **Trank „Fokus“:** Seltsamer Trank + Wächterauge = 3:00 Fokus (Redstone 8:00, Wurf/Verweil wie üblich).
7. **Modelle/Texturen:** eigene Java-Modelle (128×128-Texturen), Vanilla-Stil, Generator `tools/textures/fear_textures.py`
   (Referenzen: Vanilla Witherskelett/Eisengolem/Kettenblock, Enderman/Endstein; Paletten daraus, Formen eigen).
   Items: Kettenglied, Amulett, Wächterauge, 2 Spawn-Eier, Effekt-Icons Fokus (+ Zittern ohne SB).
8. **Sounds:** nur Vanilla (Kette, Amboss, Witherskelett, Eisengolem, Enderman, Wärter-Grollen).

## Dateien
- Neu `shared/.../FearMobs.java` (Registrierung, Sinks), `FearLogic.java` (reine Regeln), `FearAura.java`,
  `DreadEntity.java`, `DreadPattern.java`, `HollowWatcherEntity.java`, `FearServer.java`, `FearTests.java`,
  `ShiveringEffect`/`FocusEffect` (einfache MobEffect), Mixins `WitherSkullDreadMixin`, Client `AimSwayMixin`,
  `client/Dread*`, `client/Watcher*`.
- Loader: `FabricExample`, `NeoForgeExample`, `ForgeExample` (+ Client), `ModuleGameTest`, `simplemobs.mixins.json`.
- Daten: Lang EN/DE, Fragentisch EN/DE, Loot, Rezepte (Amulett, Fokus-Brauen), Item-Modelle, `check_data.py`,
  Wiki `modules/simplemobs/wiki/manual.json` + `wiki/generate.py --all`, Money `excluded`, `MobsConfig`.
- Testzentrale: `claude-testcenter-mobs` mergen; Lebensräume: Dread → Figur ohne Schädel (Schädel-Knopf), Watcher → End-Insel.
- Client-Test `FearClientSmoke` (Screenshots → `~/previews/fear-mobs/`).

## Tests (GameTests, alle drei Loader)
`fear_registered`, `fear_dread_summon` (Figur → Dread, Blöcke weg; ohne Amboss nichts), `fear_aura_levels`
(Logik + in der Welt Stufe I/II), `fear_dread_light` (Licht ≥ 12 halbiert, Aura I), `fear_dread_shield` (10 s Sperre),
`fear_dread_stomp` (Springen weicht aus), `fear_watcher_gaze` (angesehen → steht), `fear_watcher_spyglass` (3 s),
`fear_watcher_scream`, `fear_watcher_spawn_rules`, `fear_drops`, `fear_immunity` (Amulett, Fokus, Trank-Zittern weg).

## Risiken
- Bedingte Registrierung des Zitter-Effekts: Registry-Sync identisch, weil Client und Server dieselben Mods haben.
- Mock-Spieler sind kreativ → Tests schalten auf Überleben.
- Testzentralen-Merge bringt fremde Commits mit (`claude-testcenter-mobs`, ohnehin für wave1 vorgesehen).

## Änderung Besitzer 12.10. abends (umgesetzt)
Figur ohne Ketten: Eisengolem-T (unten 1 Eisenblock, darüber 3 quer, der mittlere ist ein Amboss), obendrauf ein normaler
Skelettschädel. Ausgelöst durch Rechtsklick mit Eisenhelm auf den Schädel (E: intuitiver als ein eigenes Schädel-Item –
man sieht das Ergebnis sofort; falsche Figur: Rauch, Helm bleibt). Mixin `ItemHelmetDreadMixin` (Item#useOn) statt
`WitherSkullDreadMixin`. Kosten ≈ 63 Eisenbarren + Schädel.

## Umsetzung/Abweichungen
- Umgesetzt wie geplant. Abweichungen: kein eigener Fadenkreuz-Versatz in Simple Mobs (das Zielen selbst pendelt; mit SB
  kommt dessen Fadenkreuz-Zittern dazu). Test-Haken `FearAura.TEST_TAG` gilt auch für Mock-Spieler (die sind immer kreativ).
- Mitgebracht durch den Merge von `claude-testcenter-mobs`: drei 26.3-Fixes dort (Shellker-Test `damageCooldownTime`,
  Katalogname `..._has_astation_...`). Offen von dort: Client-Shot `testcenter-mobs-*` im Modul-Clientlauf (ohne SB keine Testzentrale).
- Belege: simplemobs Fabric/NeoForge/Forge + Standalone Fabric/NeoForge 285/285 grün; Testzentrale 3 Loader 27/27 grün;
  Client-Smoke: alle 10 Furcht-Screenshots (Figur → Beschwörung, Posen, Watcher, Items), `check_data.py`, Texturen- und Wiki-Check grün.
- Nicht getestet: Pfad mit geladenem SimpleBuilding (`simplebuilding:shivering`), Aim-Sway im echten Spiel, natürlicher Spawn im End.

## Nachtrag N66 (15.10.): Wald-Regel für den Hollow Watcher
Wer mehr als `hollowWatcherForestSeconds` (120) ohne Unterbrechung in einem `simpleend:watcher_core`-Biom ist, bekommt je 30-s-Wurf die Chance `hollowWatcherForestChance` (0,25) auf einen Spawn (mehrere Versuche je Wurf); zusätzlich zu den bisherigen Regeln, senkt sie nie. Verlassen/Tod/anderes Level setzt die Zeit zurück. Code: `FearServer.trackForest`, `FearLogic.mayWatcherSpawn(..., dwell, forestChance)`.
