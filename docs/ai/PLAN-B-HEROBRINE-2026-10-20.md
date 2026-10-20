# Plan: Herobrine-Kopf + Ritual (b-herobrine20, 2026-10-20)

Grundlage: `docs/ai/KONZEPT-HEROBRINE-KOPF-2026-10-19.md` §7, Besitzer-Entscheidungen KONZ20 (H1 t, H2 b, H3 t1, H4 j, H5 f)
plus Besitzer-Wunsch „herobrine kopf soll eine weitere fähigkeit haben wie die mobheads“.

## Ist-Zustand (erhoben)
- `modules/simplemobheads`: einzige Quelle `data/mobs.json` → `tools/generate_heads.py` (HeadTable.java, Modelle, Loot, Lang, Questionary,
  Preise, Sammel-Advancements). Eigene Geometrie über `model.custom` → `client/CustomHeadModels`.
- Fähigkeiten-System: `ability/HeadAbilities` – je Kopf eine Ability-ID (`mobs.json` → `ability`), Schalter `abilities.<id>` in
  `simplemobheads.json` (automatisch aus `SimpleMobHeads.abilityIds()`), Hauptschalter `enabled`, `active(entity, id)` prüft beides +
  getragen (Helm oder Framework-`WornHeads`). Tick-Fähigkeiten laufen über `ServerPlayerTickMixin` → `HeadAbilities.tick`
  (Vorbild Hühner-Ei, Allay alle 10 Ticks). Texte (Questionary-Geheimnis, Tooltip, Config-Screen) aus `ABILITIES` im Generator.
- Vorbild Ritual: `simplemobs/HowlerRitual` (Ergebnis-Enum, Test-Hook für Licht). Kein Netzwerkkanal im Modul.

## Entscheidungen (selbst getroffen, begründet)
1. **Kopf** `simplemobheads:herobrine_head`, `mob` = `simplemobheads:herobrine` (keine Entität → kein Creeper-Pool, kein Vollständigkeits-
   Konflikt), `drop: ritual`, neue Gruppe `secret` (eigene Testzentralen-Station). **Nicht beim fahrenden Händler** (Preisliste `excluded`):
   sonst umgeht man das Easter Egg.
2. **Textur T1**: eigenes 64×64-Skinblatt (nur Kopf + Hut-Schicht), eigener Generator `tools/textures/herobrine_head_2026_10_20.py`,
   3 Varianten (A Strähnen-Pony + Vollbart, B Mittelscheitel + Kinnbart, C Strubbel + Stoppeln), weiße Augen ohne Pupille, zweite
   Datei `herobrine_eyes.png` (nur Augen) als emissiver Pass (`RenderTypes.eyes` wie Spinne/Enderman). Geometrie `custom: herobrine`
   = Spielerkopf 8×8×8 + Hut. Empfehlung **B** (nach Sichtung: A wirkt mit Haar über fast dem ganzen Gesicht matschig, C unruhig) wird vorläufig eingecheckt, Vorschauen 16-fach im Scratchpad.
3. **Ritual (H1 t, H4 j)**: Muster relativ zum Netherrack N: 3×3 Gold bei N−2, bemooster Bruchstein bei N−1, Redstone-Fackeln auf den
   vier Kanten-Goldblöcken (N−1 ± x/z). Auslöser: Mixin auf `FlintAndSteelItem#useOn` / `FireChargeItem#useOn` (RETURN, nur Server,
   Ergebnis „verbraucht“, Feuer liegt auf Netherrack) – so ist der Spieler immer bekannt, Feuer ohne Spieler (Blitz, Lava, Spender)
   löst nie aus; keine Tick-Suche. **Nur nachts** (`level.isDarkOutside()`; Test-Hook `forcedNight`). Tagsüber: Rauchwölkchen, nichts
   verbraucht.
   Wirkung: Blitz nur visuell (`setVisualOnly`), Fackeln weg, Moos → Bruchstein, Feuer erlischt mit Seelenfeuer-Partikeln, Höhlenklang
   nur beim Auslöser, Kopf als Item auf dem Schrein. Gold + Netherrack bleiben.
   Sicherheit (§6): nur Server-Thread, nur beim Zünden genau an dieser Stelle, `mayBuild` + `mayInteract` (Spawn-Schutz/Claims),
   Abklingzeit je Schrein-Position und je Spieler (`ritualCooldownTicks`, 24000, 1200–168000). Abklingzeiten im Speicher (nach
   Server-Neustart frei – Schrein-Verbrauch bleibt als Kosten; bewusst kein SavedData).
4. **Spuk (H2 b)**: 5 min nur für den Auslöser. **Abweichung vom Konzept**: statt eigener Client-Logik + Netzwerkpaket zeitet der
   Server den Spuk und schickt Partikel/Klänge **nur an diesen einen Spieler** (`sendParticles(ServerPlayer, …)`,
   `ClientboundSoundPacket` an seine Verbindung). Grund: kein eigener Netzwerkkanal in drei Loadern nötig, mit GameTests prüfbar;
   weiterhin keine Entität, keine Weltänderung, nichts an andere Spieler. Gestalt in 40–60 Blöcken (Bodenhöhe, nur geladene Chunks):
   zwei weiße End-Stab-Partikel als Augen + dunkle Staub-Säule, verschwindet unter 24 Blöcken oder beim Hinsehen (≤ 8°), dann Pause;
   ab und zu Schritte hinter dem Spieler oder ein leises Fackel-Zischen. Schalter `hauntPhase`.
5. **Fähigkeit (H5 f + weitere Fähigkeit)**, Ability-ID `herobrine`, ein Schalter `abilities.herobrine` wie bei allen Köpfen:
   - Fackeln (alle `TorchBlock`: Fackel, Seelen-/Kupferfackel, Wandvarianten) in 8 Blöcken flackern: alle 20 Ticks bis zu 6 zufällige
     davon mit Rauch + kleiner Flamme (Partikel, rein kosmetisch).
   - **Weitere Fähigkeit „Herobrines Blick“**: im Dunkeln (Licht am Träger ≤ 7, wie Monster-Spawns) zögert ein feindlicher Mob
     (`Enemy`) in 8 Blöcken, der den Träger als Ziel hat: Pfad stoppt, Langsamkeit IV für 2 s; je Mob höchstens alle 30 s; Wither
     und Wärter ausgenommen. Begründung: passt zum Mythos (die weißen Augen im Dunkeln), Stärke wie Katze (Creeper halten Abstand) /
     Wolf – kleiner Vorteil, kein Kampf-Wert, nicht dauerhaft (Abklingzeit), tagsüber/im Hellen wirkungslos, nicht ausnutzbar zum
     Farmen. Verworfen: Unsichtbarkeits-Puls (zu stark gegen Spieler/PvP, schwer balancierbar).
6. **Hinweise (Regel A6)**: Questionary-Rätselzeile + seltenes Buch „Herobrine-Notizen“ (Minenschacht-Truhen, 5 %, Schalter `hintBook`,
   gilt ab dem nächsten Laden der Loot-Tabellen), verstecktes Advancement „Er wurde nie entfernt“ / „He Was Never Removed“.
7. Modul-Unabhängigkeit: nur Vanilla-Blöcke/-Items, alles in `modules/simplemobheads`, keine fremden Klassen.

## Dateien
`data/mobs.json`, `tools/generate_heads.py` (+ generierte Dateien), `ability/HeadAbilities.java`, neu `ritual/HerobrineRitual.java`,
`ritual/HerobrineHaunt.java`, `mixin/IgniteItemMixin.java`, `mixin/ServerPlayerTickMixin.java`, `simplemobheads.mixins.json`,
`config/MobHeadsConfig.java`, `drop/HeadDrops.java`, `drop/HeadDropCondition.java`, `TestCentre.java`, `client/CustomHeadModels.java`,
Lang EN/DE, Texturen + Generator, `test/MobHeadTests.java` + Fabric-Adapter `ModuleGameTest.java`, `wiki/manual.json`,
`docs/MODS-UND-FEATURES.md`, `.claude/QUEUE.md`.

## Risiken
- `useOn`-Mixin auf zwei Klassen: Forge/NeoForge patchen `FlintAndSteelItem` nicht (nur Events) → RETURN-Inject bleibt gültig.
- Zahlen-Tests: `abilitySwitches` erwartet 28 → 29; Kopfzahl im Wiki-Text (93 → 94).
- Licht/Tageszeit in GameTests nicht steuerbar ohne Nachbartests zu stören → Test-Hooks (`forcedNight`, `dark`-Parameter).

## Verifikation
- GameTests (neu, Fabric-Adapter + NeoForge/Forge über `ALL`): `herobrine_ritual` (richtig/nachts → 1 Kopf, Fackeln weg, Moos →
  Bruchstein, Gold bleibt; Abklingzeit; Tag → nichts; Fackel fehlt / kein Moos / Gold unvollständig → nichts; ohne Spieler → nichts;
  Config aus → nichts), `herobrine_haunt` (Start/Ende, nur Auslöser, Verschwinde-Regel), `herobrine_gaze` (dunkel/hell, Abklingzeit,
  ohne Kopf), `herobrine_torch_flicker`, `herobrine_hint_book`.
- `generate_heads.py --check`, `check_data.py`, Compile Fabric/NeoForge/Forge 26.3, Modul-Targets fabric/neoforge (+ standalone),
  Guides/Wiki/Discover neu, `./gradlew check -q`.
- Nicht prüfbar ohne Client: Aussehen, Leuchten der Augen, Spuk-Gestalt → Client-Abnahme Besitzer.
