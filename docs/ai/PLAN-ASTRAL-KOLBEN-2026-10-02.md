# Konzept: Astral-Kolben (drückt) und Nihil-Kolben (zieht) – 2026-10-02

Status: **nur Konzept**, nichts implementiert (Besitzer-Queue „erst als Konzept/Plan“). Zielversion 26.3, Fabric zuerst,
hinter `McVersion.END_SYSTEMS` und einem neuen Server-Schalter `features.endPistons`.

## 1. Idee in einem Satz
Ein Block ohne Blickrichtung: Bekommt er ein Signal, bewegt er **gleichzeitig in allen 6 Richtungen** genau einen
Block um genau ein Feld – der Astral-Kolben drückt Nachbarn weg, der Nihil-Kolben holt Blöcke heran. Es gibt
**nie** eine Kette (kein zweiter Block dahinter wird mitbewegt).

## 2. Mechanik
### Signal
- Eingang ist der **eigene Kanal**, nicht Vanilla-Redstone (gleiche Regel wie Pulver/Schalter/Lampe: kein
  Übersprechen). Astral-Kolben hört auf Astral-Redstone/Astralit-Schalter, Nihil-Kolben auf Nihil-Redstone/
  Nihilith-Schalter; angeschlossen werden sie waagerecht wie die Lampe (Pulver verbindet sich sichtbar).
- Auslöser ist die **steigende Flanke** (aus → an). Solange das Signal anliegt, passiert nichts weiter; erst
  nach aus → an wieder. Dazu eine Abklingzeit `machines.endPistonCooldownTicks` (Standard 8, Server-Deckel 4..100),
  damit Taktgeber den Server nicht fluten.
- Blockzustand `active` (für die Textur) und `cooldown` intern über einen geplanten Tick, kein Block-Entity nötig.

### Astral-Kolben (drücken)
Für jede Richtung `d` der 6 Richtungen:
- Quelle `Q = pos + d` (Abstand 1), Ziel `Z = pos + 2d`.
- Bewegt wird nur, wenn `Q` beweglich ist (siehe 4.) **und** `Z` frei ist (Luft oder ersetzbar ohne Flüssigkeits-
  quelle). Ist `Z` belegt, bleibt diese Richtung einfach stehen – **kein Schieben einer Reihe**.

### Nihil-Kolben (ziehen)
Ein Block im Abstand 1 kann nicht „in“ den Kolben gezogen werden. Annahme (im Spiel gut lesbar): gezogen wird der
Block **mit einem Feld Lücke**:
- Quelle `Q = pos + 2d`, Ziel `Z = pos + d` (das Feld direkt am Kolben). Nur wenn `Z` frei und `Q` beweglich.
- Damit bleibt „genau 1 Feld, nie 2 Blöcke“: ein Block rückt um eins heran, dahinter liegende bleiben liegen.
- Offene Besitzerfrage (Liste unten): Soll der Nihil-Kolben stattdessen den direkt anliegenden Block **um eins
  weiter weg nach innen drehen**/tauschen? Standardannahme hier: Lücke-Variante.

### Gleichzeitigkeit
1. **Planen:** alle 6 Züge gegen den aktuellen Weltzustand prüfen und als Liste (Quelle, Ziel, Zustand) merken.
2. **Ausführen:** alle gültigen Züge in einem Tick anwenden. Die 6 Richtungen teilen sich keine Felder
   (Quellen und Ziele liegen auf verschiedenen Achsenstrahlen), Konflikte innerhalb eines Kolbens sind unmöglich.
3. **Zwischen Kolben:** pro Level und Spieltick eine Menge „schon bewegt“ (Quell- und Zielfelder). Ein Feld, das in
   diesem Tick schon Quelle oder Ziel war, ist für alle weiteren Kolben gesperrt. Vor dem Anwenden wird der
   gemerkte Zustand gegen die Welt verglichen; abweichend → Zug verfällt. So kann ein Block pro Tick höchstens
   einmal bewegt werden, auch wenn mehrere Kolben gleichzeitig feuern.

### Bewegung und Darstellung
- Vanilla-Weg: Quelle wird zu `minecraft:moving_piston` mit `PistonMovingBlockEntity` (ausfahrend, nicht Quelle),
  Ziel wird belegt – Animation, Entity-Schieben und Kollision kommen dann von Vanilla. Updates mit
  `UPDATE_ALL | UPDATE_MOVE_BY_PISTON`, Kolbensound (`PISTON_EXTEND` / `PISTON_CONTRACT`) mit leicht erhöhter
  Tonhöhe, wenige End-Rod-/Portal-Partikel. Kein Bildschirmtext.
- Fallende Blöcke (Sand) fallen danach normal; Redstone-Bauteile bekommen ihre Nachbar-Updates.

## 3. Grenzfälle
| Fall | Verhalten |
|---|---|
| Ziel belegt | diese Richtung entfällt, keine Kette |
| Ziel ist Wasser/Lava-**Quelle** | entfällt (keine Flüssigkeit löschen) |
| Ziel ersetzbar (Gras, Schnee­schicht, fließendes Wasser) | wird wie bei Vanilla zerstört (mit Drops), dann Zug |
| Quelle Luft / Flüssigkeit | nichts |
| `pushReaction` BLOCK, Härte −1, Block-Entity | unbeweglich |
| `pushReaction` DESTROY (Türen, Betten, Pflanzen, Fackeln) | **nicht** zerstören, Richtung entfällt (keine Ernte-Exploits über 6 Seiten) |
| `pushReaction` PUSH_ONLY (glasierte Keramik) | Astral drückt, Nihil zieht nicht (wie klebriger Kolben) |
| Zweiteilige Blöcke (Türen, Betten, hohe Pflanzen) | unbeweglich (würden sonst zerrissen) |
| Schleim/Honig | als Einzelblock, **keine** Klebewirkung, keine Ketten |
| andere Kolben (auch Vanilla-Kolben, ausgefahren/Kopf) | unbeweglich |
| Astral-/Nihil-Kolben selbst | `pushReaction BLOCK` – lässt sich nicht bewegen (keine selbstfahrenden 6-Wege-Maschinen) |
| Weltgrenze, Bauhöhe, Spawnschutz, Claims | Ziel und Quelle müssen erlaubt sein (vorhandene Claims-Adapter/`mayInteract`-Prüfung wie bei den anderen Gadgets) |
| Ungeladener Nachbarchunk | Richtung entfällt (kein Chunk laden) |
| Entities im Ziel | werden von der Moving-Piston-Kollision geschoben (Vanilla) |
| `features.endPistons=false` | Block reagiert nicht, bleibt aber setz-/abbaubar |

## 4. Duplikations- und Exploit-Schutz
- **Keine Block-Entities bewegen** (Truhen, Shulker, Öfen, Spawner): beseitigt alle NBT-Dupes.
- **Kein Zerstören** von DESTROY-Blöcken: kein 6-seitiger Erntehelfer, keine Dupes über zerrissene Doppelblöcke
  (Teppich-/Schienen-Dupes laufen über Vanilla-Kolben-Zerstören, das hier ausgeschlossen ist).
- **Ein Zug pro Block und Tick** (globale Sperrmenge) + Zustandsvergleich vor dem Anwenden: keine
  Doppelbewegung, kein Wettrennen zweier Kolben um denselben Block.
- **Flanke + Abklingzeit** (Server-Deckel): kein Dauertakt, begrenzte Bewegungen pro Sekunde.
- **Unbewegliche Kolben** und keine Ketten: keine selbstfahrenden Maschinen/Endlosschleifen.
- Kein Vanilla-Signal hinein oder hinaus: keine Quasi-Konnektivität, keine BUD-Tricks.
- Tag `simplebuilding:end_piston_immovable` für Sonderfälle (Bedrock, Endportal-Rahmen, Ender-Truhe,
  verstärktes Tiefenschiefer, Astralgewölbe, Mod-„Unzerstörbare“ aus `PistonBreach`).

## 5. Konfiguration (serverseitig, harte Grenzen)
- `features.endPistons` (an/aus, Standard an).
- `machines.endPistonCooldownTicks` 4..100, Standard 8.
- Keine Reichweiten-Option (immer genau 1 Feld, fest).

## 6. Rezept-Idee
- Astral-Kolben: Vanilla-Kolben in der Mitte, oben/unten/links/rechts Astral-Redstone, Ecken polierter Astralit.
- Nihil-Kolben: dasselbe mit Nihil-Redstone und poliertem Nihilith (alternativ klebriger Kolben als Mitte, weil er
  „zieht“).

## 7. Texturideen (Vorschläge für die Besitzerwahl, je aus/an)
- **A „Gemmen-Würfel“:** alle 6 Seiten gleich – Rahmen aus poliertem Astralit/Nihilith, in der Mitte die 4x4-Gemme
  der Schalter (gleiche Sprache wie Schalter/Lampe); aktiv heller.
- **B „Pfeile“:** wie A, dazu in den vier Ecken kleine Chevrons – Astral nach außen (drückt), Nihil nach innen
  (zieht); aktiv leuchten die Pfeile.
- **C „Kolbenplatte“:** Vanilla-Kolbenkopf-Holzplatte umgefärbt auf jeder Seite, in der Mitte ein kleines Gemmenloch;
  aktiv: Platte 1 px heller Rand (wirkt „ausgefahren“).
- Generator später als `tools/textures/proposals_end_pistons_<datum>.py`, Vorschau
  `previews/end-kolben-vorschau.png` (A, B, C beschriftet), erst nach Wahl einbauen.

## 8. Umsetzungsskizze (für später)
- `EndPistonBlock(boolean astral)` in `common/src/shared` (26.2 kompiliert, Flag `END_SYSTEMS`), Zustände `active`;
  Signal über `EndSignalBlock#sameChannel` + `receivedFrom`, Pulver verbindet sich wie mit der Lampe.
- Bewegungslogik als reine Funktion `EndPistonMoves.plan(level, pos, astral)` → Liste von Zügen (testbar ohne
  Ausführung), `apply` mit Sperrmenge (`WeakHashMap<ServerLevel, Set<Long>>`, pro Tick geleert).
- Datagen: Modelle (cube_all je Zustand), Beute, Pickaxe-Tag, Rezepte; Lang EN/DE in beiden Bäumen; Tab-Zeile
  „end_signals“; Guide-Kapitel; Testzentrale-Station.

## 9. Tests (GameTests, später)
1. Astral drückt alle 6 Nachbarn je genau 1 Feld, in einem Tick.
2. Belegtes Ziel → keine Kette, Block bleibt.
3. Nihil zieht Blöcke mit Lücke heran, anliegende bleiben.
4. Block-Entity, Bedrock, Tür, Kolben bleiben stehen; Tür wird nicht zerstört.
5. Zwei Kolben auf denselben Block im selben Tick → genau eine Bewegung, keine Duplikation (Itemzählung).
6. Dauersignal → nur eine Auslösung; Abklingzeit-Deckel greift.
7. Vanilla-Redstone und fremder Kanal lösen nichts aus.

## 10. Offene Fragen an den Besitzer
- Nihil-Kolben: Lücke-Variante (Block mit 1 Feld Abstand rückt heran) – passt das?
- Sollen DESTROY-Blöcke (Pflanzen) wie bei Vanilla zerstört werden dürfen, oder bleibt es beim sicheren „nichts tun“?
- Welche Textur (A/B/C) nach Vorschau?

---

## 11. Umsetzungsplan (2026-10-03, Branch `claude-pistons`)

Status ab hier: **wird umgesetzt**. Besitzer „weiter mit der Queue“; offene Fragen mit Standardannahmen entschieden:
- Nihil-Kolben: **Lücken-Variante** (Q = pos+2d → Z = pos+d).
- DESTROY-Blöcke (Pflanzen, Fackeln, Türen …) als Quelle: **nichts tun** (Richtung entfällt, kein Zerstören).
- Textur: Vorschau A/B/C (`previews/astral-nihil-kolben-vorschau.png`), **A eingebaut**, Besitzer kann umwählen
  (Generator `tools/textures/end_pistons_2026_10_03.py --install B|C`).
- Nihil-Rezept mit **klebrigem Kolben** in der Mitte (zieht), Astral mit normalem Kolben.
- Ein ersetzbares **Ziel** (Gras, Schneeschicht, fließendes Wasser) wird wie bei Vanilla mit Drops geräumt (Abschnitt 3).

### Technik (Abweichung von Abschnitt 8 benannt)
- `EndPistonBlock extends EndSignalBlock` mit neuer `Kind.PISTON` statt eigener `active`-Eigenschaft: der Kolben
  verhält sich beim Empfang **genau wie die Lampe** (`POWER` = empfangenes Signal, Pulver verbindet sich sichtbar über
  `sameChannel`, Kolben gibt kein Signal weiter: `receivedFrom` behandelt PISTON wie LAMP). „Aktiv“ = `power>0`.
- Flanke: im 2-Tick-Abfragetakt der Kanalblöcke; `POWER` alt 0 → neu >0 löst aus. Nach dem Auslösen wird der nächste
  Abfrage-Tick erst nach `machines.endPistonCooldownTicks` (4..100, Standard 8, Clamp in `validate` und beim Zugriff)
  geplant – Abklingzeit ohne Block-Entity. `onPlace` plant nur bei neuem Block (sonst würde der Zustandswechsel im Tick
  die Abklingzeit durch einen 2-Tick-Termin ersetzen).
- `features.endPistons` (Standard an) **und** `features.endSignals` müssen an sein; sonst Power 0, keine Bewegung.
  Rezepte fallen bei `endPistons=false` mit dem nächsten Datenpaket-Laden weg (`RecipeFilter`, `RECIPES_ON_RELOAD`).
- `EndPistonMoves` (shared): `plan(level, pos, astral)` → Züge (reine Prüfung), `fire(level, pos, astral)` wendet an.
  Beweglichkeit: `PistonBaseBlock.isPushable(..., allowDestroy=false, facing)` (Bauhöhe, Weltgrenze, Obsidian/
  Ankerblock/verstärkter Tiefenschiefer, Härte −1, BLOCK/DESTROY, PUSH_ONLY nur in Kolbenrichtung, Block-Entities) plus
  eigene Sperren: Härte <0, Tag `simplebuilding:end_piston_immovable`, alle Kolben/Kolbenköpfe/bewegte Kolben,
  **alle End-Signalblöcke** (Pulver, Schalter, Lampe, Kolben – Ergänzung beim Bau: sonst stößt der Astral-Kolben
  sein eigenes Pulver/seinen Schalter weg),
  zweiteilige Blöcke (`double_block_half`, `bed_part`), Flüssigkeitsblöcke. Ziel: geladen, in Bauhöhe/Weltgrenze, Luft
  oder ersetzbar ohne Flüssigkeitsquelle. Claims: `WorldPermissions.mayAutomate(level, kolben, Q|Z)`.
- Sperrmenge je Level und Spieltick (`WeakHashMap<Level, Lock>`), Zustandsvergleich vor jedem Zug.
- Bewegung über `minecraft:moving_piston` + `MovingPistonBlock.newMovingBlockEntity` (Astral: extending, Nihil:
  retracting mit Blickrichtung d → Bewegung −d), Quelle wird Luft (Flags 82 wie Vanilla), danach Nachbar-Updates;
  Kolbensound mit Tonhöhe 1,2–1,3, wenige `REVERSE_PORTAL`-Partikel, kein Text.
- Registrierung hinter `McVersion.END_SYSTEMS` (26.3 an, 26.2 aus → `null`, wie Lampe/Schalter); Kein neues Flag nötig.

### Dateien
- Code: `blocks/custom/EndPistonBlock.java`, `blocks/custom/EndPistonMoves.java`, `EndSignalBlock` (Kind.PISTON),
  `ModBlocks`, `ModItems`, `ModItemGroupsContent` (Zeile `end_signals`: Nihil-Kolben hinter Nihil-Lampe, Astral-Kolben
  hinter Astral-Lampe = 9 Plätze), `SearchTabPlacement` (hinter Vanilla-Klebekolben), `ServerTuningConfig`,
  `ConfigOptions`, `RecipeFilter`, `RecipelessJeiInfo` (Hinweisseite), `GuideContent` (Kapitel End-Signale),
  `ModTags` (`END_PISTON_IMMOVABLE`), `TestCentreSections.devices` (zwei Vorführreihen).
- Datagen: Rezepte, Beute, Pickaxe-Tag, Immovable-Tag (alle `END_SYSTEMS`-geschützt) → `mc26_3/generated`.
- Ressourcen von Hand (wie Lampe): Blockstates/Modelle/Item-Modelle in `mc26_3/overlay/resources`, Texturen vom Generator.
- Lang EN/DE in `src/main/resources` und `mc26_3/overlay/resources` (Blöcke, Items, Config, JEI-Hinweis).
- Tests: `EndSystemsTests` + Fabric-Adapter + Katalog; Erwartungen in `DataIntegrityTests`, `ConfigOptionTests`,
  `GuideBookTests` nachziehen.
- Wiki: `wiki/manual.json` (End-Signale-Seite: Kolben-Absatz, belegbar), `wiki/generate.py --all`.

### Tests (Abschnitt 9 konkret)
1. `pistonPushesAllSixAtOnce` – Astral drückt 6 Nachbarn je 1 Feld im selben Aufruf.
2. `pistonNeverChains` – belegtes Ziel → Block bleibt, dahinterliegender bleibt.
3. `nihilPullsAcrossTheGap` – Nihil zieht 6 Blöcke mit Lücke heran, direkt anliegende bleiben.
4. `pistonLeavesImmovablesAlone` – Obsidian, Bedrock, Truhe (Block-Entity), Tür, Kolben, Fackel/Pflanze bleiben;
   nichts zerstört; PUSH_ONLY (glasierte Keramik) Astral ja / Nihil nein.
5. `twoPistonsMoveABlockOnce` – zwei Astral-Kolben auf denselben Block im selben Tick: genau eine Bewegung, Blockzahl gleich.
6. `pistonFiresOnRisingEdgeOnly` – Dauersignal löst einmal aus; Abklingzeit-Clamp 4..100; Feature aus → nichts.
7. `pistonIgnoresVanillaAndOtherChannel` – Redstoneblock und Nihil-Schalter lösen den Astral-Kolben nicht aus.

### Risiken
- Moving-Piston-Block-Entity ohne Vanilla-Kolben: Vanilla prüft beim Abschluss keinen Kolben → unkritisch
  (dasselbe nutzt `PistonBreachTests.placeMovingBlock`).
- Tab-/Integritätstests mit festen Zeilen müssen nachgezogen werden.
