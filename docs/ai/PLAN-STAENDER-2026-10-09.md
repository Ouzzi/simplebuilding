# Puppen und Rüstungsständer – Queue N21/N24 (2026-10-09, Branch claude-q-stands)

Aufträge: N21 „Puppen/Ständer“ (Interaktionen), N24 „Trainingspuppe mit Spielernamen → Skin“,
„Rüstungsständer per Redstone wie Item-Displays / Schleich-Rechtsklick tauscht die Rüstung“,
„Rüstungsständer sollen Arme haben“, „weitere Rüstungsständer mittel/klein“.

## Ist-Zustand

- `dummy/TrainingDummy` (eine Klasse, `straw_armor_stand` + `training_dummy`), `StrawArmorStandItem`,
  `client/TrainingDummyRenderer` (Vanilla-`ArmorStandRenderer` + `DummyStuffingLayer`).
- N21 ist großteils erledigt: `docs/ai/PLAN-PUPPE-INTERAKTIONEN.md` (17 Interaktionen, Tests über echte Wege, Fix
  `lastHit`) und `PLAN-PUPPE-SPEER-ANSTURM-2026-10-09.md` (Speer-Ansturm grün). Offen dort: Windladung, Dreizack,
  Streitkolben, Namensschild.
- Vanilla 26.3 (javap): `ArmorStand.interact` → `getClickedSlot`/`isDisabled`/`swapItem` (privat); `disabledSlots`
  (privat, NBT `DisabledSlots`); `brokenByPlayer` wirft fest `Items.ARMOR_STAND`; `ShowArms` aus NBT mit Standard
  `false`; `PlayerSkinRenderCache.lookup(ResolvableProfile)` liefert die aufgelöste Haut asynchron (wie Spielerköpfe).

## Entscheidungen (selbst getroffen, Besitzer kann umstellen)

1. **Interaktionen (N21):** fehlende echte Wege ergänzen: geworfener Dreizack, Windladung, Streitkolben-Schlag,
   Namensschild. Rot → Puppe reparieren, nie den Test aufweichen.
2. **Skin:** heißt die Trainingspuppe wie ein Spieler (Namensschild, Amboss-Item, 3–16 Zeichen `A-Z a-z 0-9 _`),
   zeigt sie dessen Haut als vollen Spielerkörper (Kopf, Rumpf, Arme, Beine samt zweiter Ebene) in der Pose des
   Ständers. Auflösung wie der Vanilla-Spielerkopf über `PlayerSkinRenderCache` (nur Client, nicht blockierend).
   Fallback: solange nichts aufgelöst ist oder der Name keinen Spieler hat, bleibt die normale Puppe (Kürbis/Stroh).
   Ein Mob-Kopf im Kopfslot ersetzt weiter den Kopf. Die Zahlen/Logik bleiben serverseitig unverändert.
3. **Rüstung tauschen** (Vanilla-Ständer, Stroh-Ständer, Puppe, neue Ständer), wie das Vanilla-Regal:
   - Schleich-Rechtsklick mit leerer Haupthand tauscht alle vier Rüstungsteile mit den Rüstungsslots des Spielers.
   - Steht der Ständer auf/an einem Redstone-Signal (`hasNeighborSignal` an seinem Block), tauscht jeder Rechtsklick
     (auch mit Item in der Hand) – wie das bestromte Regal mit der Hotbar.
   - Teile mit Fluch der Bindung bleiben am Spieler (außer Kreativ); gesperrte Slots des Ständers bleiben; ein
     Teil, das in den Spieler-Slot nicht passt, bleibt am Ständer. Config `server.features.armorStandSwap` (an).
4. **Arme:** jeder neu aufgestellte Rüstungsständer (Item, Spender; Vanilla, Stroh, Puppe) hat Arme – gesetzt in
   `EntityType.create(Level, EntitySpawnReason)` nur für `SPAWN_ITEM_USE`/`DISPENSER`. Geladene, gerufene und per Code
   erzeugte Ständer bleiben Vanilla (erste Fassung im Konstruktor brach den Löwenzahn-Test, weil per Code erzeugte
   Ständer plötzlich Items in die Hand nahmen). Config `server.features.armorStandArms` (an).
5. **Neue Ständer** (eigene Entity-Klasse `PartialArmorStand extends ArmorStand`, Vanilla-Modell mit ausgeblendeten
   Teilen, Vanilla-Holztextur, nur Rüstungsslots frei über `DisabledSlots`):
   - **Mittlerer Rüstungsständer** `medium_armor_stand`: Hose + Stiefel (Beine, Hüftstange, Bodenplatte), 0,5×1,0.
     Rezept `/ /`, `/_/` (Stöcke, glatte Steinstufe).
   - **Kleiner Rüstungsständer** `small_armor_stand`: nur Stiefel (Bodenplatte), 0,5×0,5. Rezept `/_/`.
   - Abbau wie Vanilla (zwei Schläge), Drop = eigenes Item (Mixin auf `brokenByPlayer`), Pick-Block = eigenes Item.
   - Pferde-/Nautilus-Rüstung: zurückgestellt (bräuchte Pferde-/Nautilus-Modell), offen für den Besitzer.
   - Item-Icons: eigener Generator `tools/textures/stands_2026_10_09.py`, Vorschau `/root/previews/stands/`.

## Dateien

- `dummy/TrainingDummy.java`, `dummy/StrawArmorStandItem.java` (generisch für `ArmorStand`), neu
  `dummy/PartialArmorStand.java`, `dummy/ArmorStandSwap.java`, `dummy/DummySkins.java`
- `dummy/client/TrainingDummyRenderer.java`, `DummyStuffingLayer.java`, neu `client/DummySkinLayer.java`,
  `client/PartialArmorStandRenderer.java`
- Mixins: neu `ArmorStandStandsMixin` (Tausch in `interact`, Drop in `brokenByPlayer`), `EntityTypeArmorStandArmsMixin`
  (Arme), `ArmorStandAccessor` (`disabledSlots`)
- Registrierung: `ModEntities`, `ModItems`, Attribute/Renderer je Loader, Kreativ-Reihe, Suche, Testzentrale,
  Rezepte (Datagen), Item-Modelle, Lang EN/DE (beide Ablagen), Wiki `manual.json`, Config + `ConfigOptionTests`.
- Tests: `TrainingDummyTests` + neu `ArmorStandTests` (Wrapper + Katalog).

## Verifikation

Compile 3 Loader; `simplebuilding:training_dummy_game_test_*` und `simplebuilding:armor_stand_game_test_*` auf
fabric/neoforge/forge-263; Config-/Daten-Tests (`config_option*`, `data_integrity*`, `test_centre*`); Wiki-/Textur-
Checks; Vorschau ansehen. Nicht abgedeckt: Skin-Darstellung am Client (braucht Profil-Auflösung im Netz).
