# Bericht oc-headslot (Queue N55: Kopf-Slot wie Helm-Slot)

1. **Vorhanden (N49):** Slot, Rendering, Teile der Effekte (`util/ExtraHead`, `ExtraHeadLayer`, `WornHeads`). Dazu kommen jetzt: **Vorrang** (`AvatarRendererExtraHeadMixin` nimmt den Helm-Kopf aus dem Render-State, `ExtraHead.heads` = Reihenfolge Slot, dann Helm; Effekte beider gelten), **Kopf ersetzt den Spielerkopf** auch im Slot (`HumanoidModelExtraHeadMixin`, nur Spieler-, nicht Rüstungsmodell), **Varianten** (Katze/Dorfbewohner im Slot, `WornHeads.SkullLook` + `MobHeadLook`), **Größe wie am Mob** (Tabelle unten), **Effekte aus dem Slot**: Attribute (Slime-Fallhöhe, Silberfisch-Größe, Locator-Leiste, `ExtraHeadEffects`), Frostschutz Eiswanderer (`LivingEntityExtraHeadFreezeMixin`), Gruß (`HeadAbilities.wornHead`), **Anzieh-Sound/Game-Event** über `onEquipItem` beim Wechsel (`PlayerExtraHeadMixin`; Login leise).
2. Commits: siehe `git log oc-headslot` (ein Arbeits-Commit + Bericht). Framework-API additiv: `WornHeads.registerLook/skullType/skullFactor/skullEdge` (Version bleibt 0.1.11).
3. Tests (neu): SB `extraHeadAttributesAndFreezeWorkLikeTheHelmetSlot`, `helmetHeadAndExtraHeadBothApplyAndTheExtraHeadComesFirst` (inkl. Sichtweite Zombie-/Skelettkopf), `extraHeadPutOnPlaysTheEquipSound`; Modul `extra_head_greeting`, `extra_head_variant_look`; Framework `WornHeadsTest`; Client `head-slot` (Render-State-Vorrang, Kopfkante je Look, 10 Bilder).
4. Läufe: `run.py --targets fabric-263,neoforge-263 --filter 'simplebuilding:mob_head_game_test*'` -> `alles gruen: 44/44 bestanden, 0 rot`; `--targets module-simplemobheads-fabric-263,module-simplemobheads-neoforge-263 --filter 'simplemobheads:*'` -> `alles gruen: 54/54 bestanden, 0 rot`; `SIMPLEBUILDING_CLIENT_ONLY=head-slot xvfb-run ... --targets client-fabric-263` -> `alles gruen: 10/10 bestanden, 0 rot`; `:framework:test` grün; Compile 26.3 Fabric/NeoForge/Forge + Module + 26.2-Hauptquelle (`:compileJava`, `:neoforge:compileJava`) grün. Bilder: `docs/ai/previews/oc-headslot/head-slot-sizes-precedence.png`. Checks: `module_guides --check`, `discover --check` grün; `wiki/generate.py --all` neu erzeugt (Modul-Prosa), `--check --strict` meldet nur `textures.js out of date` (Drift durch Furnace-Minecart-Pfade, nicht aus diesem Branch; `textures.js`/Vanilla-PNGs bewusst NICHT mitcommittet).
5. **Eigene Entscheidungen/Abweichungen:** (a) Größe: getragener Mod-Kopf = Kante des Mob-Kopfes (`min(Mob-Kopf, 1.1875 x Skull-Modell)`, nie größer als vorher, 14.25 px max); gilt für Helm- UND Kopf-Slot (`CustomHeadLayerMixin` im Modul, Skalierung um den Hals-Pivot). Vanilla-Köpfe (Zombie, Skelett, Creeper, Wither-Skelett, Piglin, Drache) bleiben wie in Vanilla (9.5 px über dem Spielerkopf). (b) `Fit.onTop` (Silberfisch obenauf) entfällt: Mod-Köpfe ersetzen den Spielerkopf wie im Helm; Helm-Skalierung für Mod-Köpfe jetzt aus der Kopfkante `(Kante+1)/10`. (c) Gleicher Effekt in beiden Slots zählt einmal (Attribut-Id des Helms gewinnt; Sichtweite nur bei unterschiedlichem Mob) - Konzept §3.5 "nicht stapelnd". (d) Framework-Hook statt direkter Modul-Imports (Unabhängigkeit); `HeadLooks`-Brücke (26.2 = Stub, da dort kein Framework).
6. **Fund/Lehre:** ein zweiter `@Unique`-Feld-Initialisierer (`List.of()`) im Player-Mixin ließ den ersten (`ItemStack.EMPTY`) für Mock-Spieler verschwinden (NPE in `LegacySpatulaMigration`) - Felder ohne Initialisierer, null-sicher lesen.
7. **Offen/nicht getestet:** Client nur Fabric 26.3 geprüft (NeoForge/Forge-Client teilen dieselben Mixin-Dateien, nicht gestartet). 26.2/1.21.11/26.4: nicht angefasst (HeadLooks-Stub, kein MOB_VISIBILITY-Zwilling); `:compileGametestJava` der 26.2-Hauptquelle scheitert schon ohne diese Änderung an `HudAndTooltipClientTest` (simplelib). Köpfe von Simple Mobs/End/Dragonmite (6 Stück) ohne Zahl in der Tabelle (Mod-Modelle nicht im Messlauf); sie nutzen dieselbe Formel zur Laufzeit. Pumpkin-Blur-Overlay aus dem Slot nicht umgesetzt. Prod: **TODO "development -> Production ziehen"** nach Freigabe (Abnahme der Größen im Spiel durch den Besitzer).

## Anhang: Kopfgrößen (Modellpixel, Kantenlänge; gemessen am Mob-Modell, vorher = 1.1875 x Skull-Modell)

Vanilla unverändert: zombie/skeleton/creeper/wither_skeleton 8 -> 9.5, piglin 10 breit, dragon 16.

| Kopf | Mob-Kopf (px) | getragen vorher | getragen nachher |
|---|---|---|---|
| blaze | 8.0 | 9.5 | 8.0 |
| enderman | 8.0 | 9.5 | 8.0 |
| husk | 8.0 | 9.5 | 8.0 |
| spider | 8.0 | 9.5 | 8.0 |
| cave_spider | 8.0 | 9.5 | 8.0 |
| stray | 8.0 | 9.5 | 8.0 |
| bogged | 13.1 | 15.6 | 13.1 |
| slime | 6.5 | 7.7 | 6.5 |
| silverfish | 4.0 | 4.8 | 4.0 |
| breeze | 10.0 | 11.9 | 10.0 |
| shulker | 12.0 | 14.2 | 12.0 |
| drowned | 8.0 | 9.5 | 8.0 |
| pig | 10.0 | 11.9 | 10.0 |
| cow | 10.0 | 11.9 | 10.0 |
| chicken | 6.0 | 7.1 | 6.0 |
| sheep | 8.0 | 9.5 | 8.0 |
| zombie_villager | 16.0 | 14.2 | 14.2 |
| witch | 20.4 | 14.2 | 14.2 |
| pillager | 11.0 | 13.1 | 11.0 |
| vindicator | 11.0 | 13.1 | 11.0 |
| evoker | 11.0 | 13.1 | 11.0 |
| illusioner | 12.0 | 14.2 | 12.0 |
| ravager | 24.1 | 14.2 | 14.2 |
| vex | 5.0 | 5.9 | 5.0 |
| phantom | 7.0 | 8.3 | 7.0 |
| guardian | 43.9 | 14.2 | 14.2 |
| elder_guardian | 43.9 | 14.2 | 14.2 |
| endermite | 9.0 | 10.7 | 9.0 |
| warden | 48.0 | 14.2 | 14.2 |
| creaking | 24.0 | 14.2 | 14.2 |
| parched | 8.0 | 9.5 | 8.0 |
| camel_husk | 25.0 | 14.2 | 14.2 |
| zombie_nautilus | 25.3 | 14.2 | 14.2 |
| sulfur_cube | 16.0 | 14.2 | 14.2 |
| ghast | 28.0 | 14.3 | 14.3 |
| magma_cube | 8.0 | 9.5 | 8.0 |
| hoglin | 22.5 | 14.3 | 14.3 |
| zoglin | 22.5 | 14.3 | 14.3 |
| piglin_brute | 15.7 | 14.3 | 14.3 |
| zombified_piglin | 15.7 | 14.3 | 14.3 |
| strider | 31.4 | 14.3 | 14.3 |
| wither | 8.0 | 9.5 | 8.0 |
| allay | 5.0 | 5.9 | 5.0 |
| armadillo | 8.0 | 9.5 | 8.0 |
| axolotl | 14.0 | 14.2 | 14.0 |
| bat | 8.0 | 9.5 | 8.0 |
| bee | 15.0 | 14.2 | 14.2 |
| camel | 25.0 | 14.2 | 14.2 |
| cat | 6.0 | 7.1 | 6.0 |
| ocelot | 6.0 | 7.1 | 6.0 |
| copper_golem | 14.0 | 14.2 | 14.0 |
| dolphin | 10.0 | 11.9 | 10.0 |
| donkey | 22.5 | 14.3 | 14.3 |
| mule | 22.5 | 14.3 | 14.3 |
| horse | 20.1 | 14.2 | 14.2 |
| skeleton_horse | 20.1 | 14.2 | 14.2 |
| zombie_horse | 20.1 | 14.2 | 14.2 |
| fox | 9.0 | 10.7 | 9.0 |
| frog | 9.0 | 10.7 | 9.0 |
| goat | 20.0 | 14.2 | 14.2 |
| iron_golem | 11.0 | 13.1 | 11.0 |
| snow_golem | 8.0 | 9.5 | 8.0 |
| llama | 21.0 | 14.2 | 14.2 |
| trader_llama | 21.0 | 14.2 | 14.2 |
| mooshroom | 10.0 | 11.9 | 10.0 |
| nautilus | 25.3 | 14.2 | 14.2 |
| panda | 17.0 | 14.3 | 14.3 |
| parrot | 8.0 | 9.5 | 8.0 |
| polar_bear | 10.0 | 11.9 | 10.0 |
| rabbit | 10.8 | 12.8 | 10.8 |
| sniffer | 20.0 | 14.2 | 14.2 |
| turtle | 6.0 | 7.1 | 6.0 |
| villager | 16.0 | 14.2 | 14.2 |
| wandering_trader | 16.0 | 14.2 | 14.2 |
| wolf | 8.0 | 9.5 | 8.0 |
| happy_ghast | 23.0 | 14.3 | 14.3 |
| cod | 15.0 | 14.2 | 14.2 |
| salmon | 25.0 | 14.2 | 14.2 |
| pufferfish | 12.0 | 14.2 | 12.0 |
| tropical_fish | 12.0 | 14.2 | 12.0 |
| squid | 33.0 | 14.3 | 14.3 |
| glow_squid | 33.0 | 14.3 | 14.3 |
| tadpole | 10.0 | 11.9 | 10.0 |
| shellker | 12.0 | 14.2 | 12.0 |
| soulfire_blaze | 8.0 | 9.5 | 8.0 |
| blighted_wither | 8.0 | 9.5 | 8.0 |
| abyssal_wither | 8.0 | 9.5 | 8.0 |
