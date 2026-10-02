# Plan: Kampf-Welle (Besitzer 2026-10-02, Branch claude-combat)

Vier Queue-Punkte aus `.claude/QUEUE.md`. Nur 26.3 (Fabric zuerst, dann NeoForge/Forge); gemeinsamer Code kompiliert auf 26.2.

## Ist-Zustand (erhoben)

- **Pfeile:** Vanilla `AbstractArrow#onHitEntity` zaehlt den Pfeil im Ziel (`setArrowCount`) und verwirft ihn
  (`discard`) - er ist weg. Mod-Pfeile (`CraftedArrow`, Teile im Aufhebe-Stapel) erben das. Aufsammeln
  fuehrt `AbstractArrowPickupMixin` (Koecher-Trichter). Unendlichkeit/Kreativ/Mehrfachschuss setzen
  `pickup = CREATIVE_ONLY`, Spieler-Pfeile sonst `ALLOWED`; Skelett/Werfer haben keinen Spieler als Besitzer.
- **Shulkerkiste:** Die Mod-Stufen (`TieredShulkerBoxItem`) haben weder `enchantable` noch einen Tag. Die
  Vanilla-Shulkerkiste steht aber in `constructors_touch_enchantable` (ModItemTagProvider) - Amboss + Buch
  verzaubert sie; eine Funktion hat das nicht (kein Code liest Constructor's Touch an Shulkerkisten).
  `DevEnchantedTab` bietet sie deshalb verzaubert an (Test in DataIntegrityTests erwartet das).
- **Resonanzstab (`amethyst_lens`):** Der Server trifft Lebewesen schon (`LaserPointerItem#aim`, `LaserBeam#beamAtEntity`:
  anzuenden nach doppelter Verweildauer, PvP-/Kreativ-/Nass-Schutz, Schalter `server.laser.igniteEntities`) und
  speichert mit Beruehrung des Konstrukteurs auch Entity-Messungen. **Luecke:** der Client (`LaserRenderer`,
  `TweaksClient#laserHit`, HUD `hudLines`) nimmt nur `player.pick` = Bloecke: der Punkt sitzt auf der Wand hinter
  dem Mob, das HUD misst den Block dahinter - "scannen" eines Mobs gibt es fuer den Spieler nicht.
- **Traenke:** Die Mod hat keine eigenen Traenke/Effekte (nur simplefun `piggy_effect`, ohne Trank). 26.3 braut
  datengetrieben (`data/minecraft/recipe/brewing/*.json`, Typ `minecraft:brewing`, Datagen `BrewingProvider`);
  26.2 hat das nicht. `MobEffect#onMobHurt` gibt es auf 26.2 und 26.3. Shulkerkopf: `TweaksItems.SHULKER_HEAD`
  (simplebuilding, droppt von Shulkern) - simplefun hat keinen Shulkerkopf.

## Umsetzung

1. **Pfeile zurueck (`fletching/ArrowRecovery`, Mixins):**
   - `AbstractArrowRecoveryMixin` am Aufruf von `doPostHurtEffects` in `onHitEntity` (nur erfolgreicher Treffer auf
     ein Lebewesen): Server, `pickup == ALLOWED`, Besitzer ist Spieler, Durchschlag 0 (sonst fliegt der Pfeil weiter),
     Ziel kein Spieler, Schalter an, Amethyst-Spitze zerspringt (bleibt weg). Ist das Ziel schon tot, faellt der
     Pfeil sofort dort; sonst merkt sich das Lebewesen den Stapel (1 Stueck, alle Komponenten = Teile, Trank).
   - `LivingEntityMixin`: Liste speichern/laden (`SimpleBuildingStuckArrows`), beim Tod in `dropAllDeathLoot` fallen
     lassen (Spielregel `mob_drops`). Obergrenze je Lebewesen: Config, hart 64.
   - Config `server.arrows.recoverFromMobs` (an), `server.arrows.maxPerMob` (16, 1..64).
2. **Shulkerkiste nicht verzauberbar:** `Items.SHULKER_BOX` aus `constructors_touch_enchantable`; Test: keine
   Shulkerkiste (17 Vanilla + 3 Stufen) hat `enchantable` oder wird von einer Verzauberung unterstuetzt. Dev-Tab-Test anpassen.
3. **Resonanzstab auf Lebewesen:** gemeinsames Zielen `LaserPointerItem.aim(Player, range, partialTick)` (Block +
   Lebewesen bis `ENTITY_RANGE`) fuer Server, Punkt, Netzwerk-Punkt und HUD. HUD mit Beruehrung: Entfernung, Name,
   Leben, Hoehe des Lebewesens. Neu "Scannen": nach der halben Anzuende-Verweildauer leuchtet das Ziel
   (`Glowing`, 5 s) - kein Text; brennbare Ziele brennen danach wie bisher. Schutz: `mayAffectEntity`,
   Spieler nur mit Schalter `server.laser.scanPlayers` (aus) und `canHarmPlayer`; Schalter `server.laser.scanEntities` (an).
4. **Trank des listigen Shulkers (`McVersion.CRAFTY_SHULKER`, 26.3 an / 26.2 aus):**
   - `effect/ModEffects`: Effekt `simplebuilding:crafty_shulker` (nuetzlich, Farbe Shulker-Lila), `onMobHurt`:
     nur bei Schaden durch ein Wesen (nicht Leere/Fall/Feuer), Server, Abklingzeit (Config 60 Ticks, 20..1200),
     16 Versuche wie Chorusfrucht im Radius (Config 8, 2..16): Zielspalte nach unten absuchen, fester Boden
     (`isFaceSturdy` oben), Fuesse/Kopf frei, keine Fluessigkeit, Boden nicht in `simplebuilding:crafty_shulker_unsafe`
     (Feuer, Magma, Kaktus, Pulverschnee ...), Weltgrenze; Teleport ueber `McVersion.randomTeleport` mit
     Chorus-Klang/Partikeln. Reittiere absteigen.
   - Traenke `crafty_shulker` (3600 Ticks) und `long_crafty_shulker` (9600); Registrierung Fabric-Init,
     NeoForge/Forge im RegisterEvent (MOB_EFFECT, POTION).
   - Brauen (26.3-Datagen `ModBrewingProvider` in mc26_3/fabric): Seltsamer Trank + Shulkerkopf -> Trank;
     + Redstone -> lang; Schwarzpulver -> Wurf, Drachenatem -> Verweil, fuer alle drei Behaelter (Vanilla-Schema).
     Getraenkte Pfeile kommen von Vanilla. Config `server.craftyShulker.*`.
   - Effekt-Icon 18x18 im Vanilla-Stil, Generator `tools/textures/crafty_shulker_effect.py`, Vorschau
     `previews/crafty-shulker-vorschau.png` (A = Vanilla-Levitation, B = neues Icon, C = Trankfarbe).
   - Sprache EN/DE in beiden Sprachorten (Effekt, 4 Trankformen, Config).
5. Tests (GameTests, Katalog + Fabric-Adapter): Pfeil faellt beim Tod mit Teilen; Unendlichkeit/Skelett/Durchschlag
   kein Pfeil; sofortiger Tod; Speichern/Laden; Shulkerkisten-Verzauberung; Scannen leuchtet + PvP-Schutz;
   Effekt teleportiert sicher, nicht in Lava/Leere, Abklingzeit, nur Wesen-Schaden; Brau-Rezepte geladen.

## Risiken

- Mixin-Ziel `doPostHurtEffects`-Aufruf in `onHitEntity`: auf 26.2 gleich (gleiche Signatur), Forge/NeoForge
  nutzen dieselbe Mixin-Datei.
- NeoForge/Forge: Effekt/Trank muessen in ihrem RegisterEvent registriert werden, sonst eingefroren.
- Brauen nur 26.3 (Datenrezepte); 26.2 Flag aus -> nichts registriert.

## Verifikation

Compile 26.2 (`:compileJava :neoforge:compileJava`), Forge 26.3, Datagen 26.3, `fabric-263` + `neoforge-263`
GameTests, Wiki-Check, Textur-Check, `check`.

## Annahmen (selbst entschieden)

- "Mob" = jedes Lebewesen ausser Spielern; Pfeile fallen beim Tod (nicht beim Despawn/Entladen).
- Teleport nur bei Schaden durch ein Wesen (Nahkampf, Geschoss, Explosion eines Wesens) - sonst wuerde Fall-/
  Feuerschaden dauernd teleportieren. Keine Staerke-II-Stufe (Besitzer nannte nur lang/verlaengert).
- Shulkerkopf der Mod (simplebuilding), weil simplefun keinen hat.
- Scannen = Leuchten statt Text (Regel: keine Bildschirmtexte); Zahlen nur im bestehenden HUD-Kasten mit Beruehrung.

## Ergebnis (Agent, 2026-10-02)

- Umgesetzt wie geplant. Abweichung: Brau-Datagen braucht einen leeren 26.2-Zwilling (`src/mc26_2/.../ModBrewingProvider.java`, checkOverlays).
- Gates: `fabric-263` + `neoforge-263` 1684/1684 "alles gruen" (Lauf 2026-10-02T20-05-56Z-27d7); `check` gruen;
  26.2 `:compileJava :neoforge:compileJava`, `-Pforge263=true :mc26_3:forge:compileJava` gruen; Wiki `--all --check` und Texturen `--check` gruen.
- Nicht getestet: Client (Punkt/HUD auf Lebewesen, Effekt-Icon im Spiel, Trankfarbe), Forge-26.3-GameTests, Mehrspieler/PvP real.
- Offen: Icon-Abnahme `previews/crafty-shulker-vorschau.png`; 26.2/1.21.11-Port (Flag aus, 26.2 braut nicht datengetrieben); `src/main/generated` (26.2-Datagen) enthaelt die Vanilla-Shulkerkiste noch im Constructor's-Touch-Tag bis zum Port-Run.
