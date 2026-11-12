# Audit simplearrows (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric. Aufbau wie `modules/simplestorage/AUDIT.md` und `modules/simplepads/AUDIT.md`.
Umfang des Mods: 26 Java-Dateien / 2,9k Zeilen (ohne Tests, ohne Forge/NeoForge - nicht Teil des Releases; davon 449 Zeilen erzeugt:
`ArrowsGuide`, `ArrowsDiscover`), Tests 995 Zeilen. Ein Menue (`FletchingMenu`), drei Mixins (`FletchingTableMixin`, `ArrowPierceAccessor`,
`GhostSlotsInvoker` nur Client), drei Projektil-Entities (`CraftedArrow`, `LightArrow`, `SilverArrow`), keine eigenen Netzwerk-Payloads.

## Vollstaendig gelesen
- Menue und Rezepte: `fletching/FletchingMenu.java` (Slots, `quickMoveStack`, Ergebnis-Slot `onTake`, Rezeptbuch-Platzierung `handlePlacement`,
  `removed`, `stillValid`), `fletching/FletchingRecipe.java`, `FletchingRecipes.java` (`unlockAll`), `Fletchings.java` (Registrierung),
  `ArrowParts.java` (Teile, Codecs, Tags), `DummyHeads.java`.
- Entities/Items: `fletching/CraftedArrow.java` + `CraftedArrowItem.java` (Schaden, Trank, Amethyst-Splitter, Windstoss, Aufheben), `entity/LightArrow.java`
  (Licht setzen/entfernen, Speichern), `entity/SilverArrow.java`, `item/LightArrowItem.java`, `item/SilverArrowItem.java`.
- Mixins: `mixin/FletchingTableMixin.java`, `mixin/ArrowPierceAccessor.java`, `mixin/client/GhostSlotsInvoker.java`, `simplearrows.mixins.json`.
- Rahmen: `SimpleArrows.java` (IdMoves), `ArrowsRegistry.java`, `config/ArrowsConfig.java`, `ArrowsFabric.java`, `ArrowsFabricClient.java`,
  `client/FletchingScreen.java`, `client/ThreeSlotRecipeBookComponent.java`, `client/CraftedArrowRenderer.java`; Daten: Rezepte, Item-/Entity-Tags,
  `check_data.py`.
- Gegengelesen in den Minecraft-Quellen 26.3: `AbstractArrow` (Treffer, Aufheben, Speichern, Durchbohrung), `ServerExplosion` (TRIGGER, Blocklike-Entities),
  `ServerGamePacketListenerImpl#handlePlaceRecipe`, `Level#setBlock`-Flags.
- Nicht gelesen: `guide/ArrowsGuide.java` und `discover/ArrowsDiscover.java` (erzeugt), `client/FletchingProbe.java` (nur Kopf und Dateizugriffe;
  Entwicklungswerkzeug, ohne Anforderungsdatei im Spielordner untaetig), Tests ausser den Pfeil-/Spitzen-Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigene C2S- oder S2C-Payload. Rezeptbuch-Klick ist Vanillas `ServerboundPlaceRecipePacket`: Vanilla prueft Menue-ID, `stillValid` und dass der Spieler das Rezept kennt; `handlePlacement` nimmt nur `FletchingRecipe` an, sonst `NOTHING`. Komponente `arrow_parts`: Netz-Codec per Ordinal (Anhaengen-nur-hinten ist vermerkt), Speicher-Codec per Name. |
| C2 Server-Autoritaet | Ergebnis rechnet nur der Server aus den Slots (`slotsChanged`), `stillValid` an den Befiederungstisch gebunden, Teile-Slots `mayPlace` nach Tag. Client-Klassen nur in `client/` und `ArrowsFabricClient`; Client-Mixin nur in der Client-Liste. |
| C3 Dupes/Verlust | `quickMoveStack`: Rueckgabe nur bei echter Bewegung, jede Richtung endet (kein Endlosschleifen-Pfad), Trank-Slot (Index 40) getrennt behandelt. Ergebnis-Slot: `mayPlace` falsch, Teilentnahme nicht moeglich (Vanilla `tryRemove`/`allowModification`), `canTakeItemForPickAll` sperrt Doppelklick-Sammeln, Teile werden erst nach der Entnahme je 1 abgezogen. `removed` gibt alle vier Teil-Slots zurueck (auch bei Trennung). Aufheben: erbt Vanillas `Pickup` (Kreativ/Unendlich = `CREATIVE_ONLY`, Multishot-Zusatzpfeile ebenso), Spieler-Pfeile `ALLOWED`; Amethyst setzt `DISALLOWED` und zerspringt; Trank-Pfeil trocknet nach 600 Ticks im Boden (Vanilla-Regel). Dispenser: `copyWithCount(1)`, kein Mehrfachpfeil. **P2-1 gefunden und gefixt.** |
| C4 NBT/Komponenten | `arrow_parts`: Schaft-Codec tolerant (alte `netherite_rod`/`enderite_rod` werden Stock), Spitze/Befiederung per Namen; fehlen Teile, gilt Vanilla-Pfeil. `LightArrow` speichert `light_pos` als `BlockPos.CODEC` (optional). `PierceLevel` wird von Vanilla mitgespeichert. Keine Listen. |
| C5 Tick-Kosten | `CraftedArrow#tick`: nur Partikel (Client) und ein Zaehlervergleich; `LightArrow#tick` ohne Suche; `releaseLight` sucht nur beim Entfernen in einem 3x3x3-Feld. Amethyst-Splitter: eine AABB mit Radius 1,5 je Treffer. `FletchingRecipes.unlockAll` geht bei jedem Rechtsklick ueber alle Rezepte des Servers (einige Tausend, nur Vergleiche; P3). |
| C6 Leaks | Keine statischen Maps mit Spielern/Entities/Leveln. `FletchingProbe` haelt nur Client-Zustand. |
| C7 Sicherheit/Rechte | Befehl `/simplearrows config` (SimpleLib `ConfigCommands`) nur Stufe 2 (Spielleiter), Werte werden geklemmt. Mixins: `FletchingTableMixin` (Ziel `BlockBehaviour#useWithoutItem`, Kopf, nur fuer den Befiederungstisch, `defaultRequire 1`), `ArrowPierceAccessor` (Invoker auf privates `setPierceLevel`). Keine @Overwrite. Windstoss der Breeze-Rute nutzt `ExplosionInteraction.TRIGGER` wie Vanillas Windkugel: keine Bloecke zerstoert, kein Feuer; ausloesbare Bloecke (Tueren, Knoepfe, Hebel, Glocken) reagieren unabhaengig von `mobGriefing` (Vanilla-Regel fuer Windkugeln von Spielern). Lichtpfeil setzt nur in Luft (`minecraft:light`), ohne `mayInteract` (P3-4). Kein Feuer-Griefing: Lohenrute setzt nur Brandzeit am Ziel. |
| C8 Unabhaengigkeit | Allein spielbar (Standalone-Ziel `runSimplearrowsStandaloneGameTest`), nur `framework` + `simplelib`; SimpleBuilding/Simple Ores nur ueber Item-Tags mit `required: false` und `fabric:load_conditions`. Der Silberpfeil hat ohne Simple Ores kein Rezept (Test vorhanden). |
| C9 Doku/Pflege | Kein TODO/FIXME. Lang EN/DE gleich, amerikanisches Englisch geprueft. Kern-Dubletten (`tools/dup_pairs.txt`): `ThreeSlotRecipeBookComponent`, `GhostSlotsInvoker` (beide Client); `check_dup_impl` 0 Probleme. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplearrows-P2-1 | **P2 (Logikfehler)** | `CraftedArrow#onHitEntity` (CraftedArrow.java:132-142) rief `afterHit` auf, das mit `this.isRemoved()` abbrach (frueher CraftedArrow.java:165). Vanilla verwirft einen nicht durchbohrenden Pfeil schon in `AbstractArrow#onHitEntity` (Quelle 26.3 Z. 498-500, `discard()` nach erfolgreichem Treffer). Folge: bei einem **direkten Treffer** liefen weder der Amethyst-Splitter (1 Schaden an Mobs in der Naehe) noch der Windstoss der Breeze-Rute; sie wirkten nur an Bloecken, bei Durchbohrung oder abgeprallten Pfeilen. Der Hilfetext verspricht beides "beim Aufprall". Der Test `amethyst_arrow_is_not_recovered` pruefte `isRemoved()`, was Vanilla allein erfuellt. | **gefixt** (Arbeitsbaum): "lebt"-Pruefung vor dem Treffer in `onHitEntity` und `onHitBlock`, Wache aus `afterHit` entfernt (`CraftedArrow.java`); Gametest `fletching_impact_effects_happen_on_a_direct_hit` (`FletchingTests#impactEffectsHappenOnADirectHit`, Katalog `ArrowsTests`, Adapter `ModuleGameTest#fletchingImpactEffectsHappenOnADirectHit`); ohne Fix rot |
| simplearrows-P3-1 | P3 | `CraftedArrow#readAdditionalSaveData` stellt `simplearrows$baseDamage` nicht wieder her (CraftedArrow.java:31, 138; `SilverArrow` tut es). Ein nach dem Neuladen eines Chunks fliegender Pfeil mit nicht-Standardschaden (nur durch Mobs/Befehle gesetzt) faellt auf 2,0 zurueck. Spieler-, Dispenser- und Armbrustpfeile haben 2,0. | Hinweis (heute ohne Wirkung) |
| simplearrows-P3-2 | P3 | Windstoss: Quelle der Explosion ist der Pfeil (CraftedArrow.java:170), nicht eine Windkugel; Vanillas `shouldAffectBlocklikeEntities` schliesst nur Windkugeln aus. Sichtbare Ruestungsstaender und Gegenstaende am Boden werden daher mitgeschoben (kein Schaden). | **Besitzer AUDSB31B F5 = A (so lassen)**: keine Code-Aenderung; Wiki EN/DE nennt es, Test `fletching_impact_effects_happen_on_a_direct_hit` prueft zusaetzlich einen liegenden Gegenstand (nur bei `mobGriefing` an, Vanilla-Regel) (a-audsb31b) |
| simplearrows-P3-3 | P3 (Balance) | Lohenrute addiert je Treffer 60 Brandticks auf den Rest (CraftedArrow.java:160), ohne Obergrenze; viele Treffer halten ein Ziel bis zum Loeschen (Wasser) brennend. | **Besitzer AUDSB31B F4 = A, umgesetzt (a-audsb31b)**: `max(Rest, min(200, Rest + 60))` (`ArrowParts.BLAZE_MAX_FIRE_TICKS`, laengeres Feuer bleibt); Gametest `fletching_blaze_shaft_fire_is_capped`, Wiki EN/DE |
| simplearrows-P3-4 | P3 | Lichtpfeil setzt `minecraft:light` ohne `mayInteract`/Claim-Pruefung (LightArrow.java:53-54); nur in Luft, erlischt mit dem Pfeil (Vanilla-Despawn 1200 Ticks) - ueberschreibt aber ein vorhandenes Admin-Licht (`isAir()` ist fuer Lichtbloecke wahr) und entfernt es beim Despawn. | Hinweis |
| simplearrows-P3-5 | P3 | Shift-Klick auf das Ergebnis: `onTake` bekommt den schon leeren Stapel, `onCraftedBy(player, 0)` zaehlt die Statistik "hergestellt" nicht (FletchingMenu.java:101, 258, 286); Vanilla zaehlt ueber `onQuickCraft`. Kein Verlust. | Hinweis |
| simplearrows-P3-6 | P3 | `FletchingTableMixin` ist nicht an den Sub-Mod-Schalter gebunden (nur die Tab-Eintraege sind es): bei "Enable Simple Arrows" aus oeffnet der Tisch trotzdem das Menue. Bei `FletchingRecipe#display` (FletchingRecipe.java:197) wirft `ItemStackTemplate.fromNonEmptyStack` fuer ein von Hand geschriebenes Rezept mit unbekanntem Teil (`IllegalStateException`); die mitgelieferten Rezepte haben `fabric:load_conditions`, daher nur fremde Datapacks. | Hinweis |
| simplearrows-P3-7 | P3 | Trank-Slot: ein trinkbarer Trank (keine Linger-Variante, keine 8 Pfeile) ergibt 4 Pfeile mit 1/8 Dauer; billiger als Vanilla (Verweilttrank + 8 Pfeile -> 8). Besitzer-Entscheid WE25-9, nur als Balance-Hinweis. | Hinweis |
| simplearrows-P3-8 | P3 | `client/FletchingProbe.java` ist ein Entwicklungswerkzeug im Release-Jar (liest `simplearrows-fletching-probe.json` im Spielordner, kann `mc.stop()`); ohne die Datei inaktiv, keine Fernwirkung. | Hinweis |
| simplearrows-P3-9 | P3 | 2 Kern-Dubletten (`ThreeSlotRecipeBookComponent`, `GhostSlotsInvoker`); der Fix P2-1 betrifft nur `CraftedArrow` (ohne Kern-Paar). | Liste |

## Urteil
**shipbar nach Fix** (0 P1, 1 P2 gefixt, 9 P3; Besitzer AUDSB31B: P3-3 Brandgrenze umgesetzt (F4 = A), P3-2 Windstoss bleibt (F5 = A)). Menue, Ergebnis-Slot und Aufheben sind sauber (keine Dupes gefunden); die Pfeil-Entities erben Vanillas
Aufheben/Unendlich/Multishot-Regeln. Der einzige echte Fehler war der nie ausgeloeste Aufprall-Effekt bei direkten Treffern.

## Testergebnis
Lokal: `javac.sh simplearrows` (CraftedArrow, FletchingTests, ArrowsTests, ModuleGameTest) - nur die bekannten Access-Widener-Meldungen in ungeaenderten Dateien
(`Fletchings.java:47`, `ArrowsRegistry.java:54`, `LibMaterials.java:100`); `modules/simplearrows/tools/check_data.py` gruen, `tools/check_dup_impl.py` 0 Probleme,
`tools/check_gametest_adapters.py` OK, `tools/check_tooltip_text.py` OK. sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: der neue Gametest wurde nicht ausgefuehrt (kein Gradle erlaubt); Windstoss-Wirkung haengt an Vanillas Rueckstoss (`pushFromExplosion`), echter
Flug/Mehrspieler, Forge/NeoForge, Client (Screen, Rezeptbuch-Mausbedienung).
a-audsb31b (F4, F5): javac von `ArrowParts`, `CraftedArrow`, `FletchingTests`, `ArrowsTests`, Adapter lokal sauber (nur dieselben Access-Widener-Meldungen);
sbt siehe `docs/ai/PLAN-A-AUDSB31B-2026-11-12.md`.
