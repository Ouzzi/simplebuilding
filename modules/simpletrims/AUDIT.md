# Audit simpletrims (a-audsb31e1, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplepads/AUDIT.md`.
Umfang: 30 Java-Dateien / 3,1k Zeilen (shared + fabric, ohne Tests; Tests zusaetzlich 2 Dateien / 0,8k Zeilen). Risikogeleitet
(P1-Klassen zuerst: Lichtbloecke, Welteingriffe, Rezepte/Duplikation). Zusatzeingabe BEW39
(`/root/sb-tools-a/bewertung39/simpletrims.json`) wurde gegen den Code geprueft.

## Vollstaendig gelesen
- Licht: `light/TrimLight` (ganz), `light/OwnedLightHolder`, `mixin/LivingEntityTrimsMixin`, `ItemFrameTrimsMixin`,
  `BlockAttachedEntityTrimsMixin`, `EntityRemovedTrimsMixin`; SimpleLib-Gegenstueck `OwnedLights` (Markierung, Bereinigung beim Chunk-Laden).
- Platzierte Vorlagen: `placed/PlacedTemplates` (ganz), `PlacedTrimTemplateBlock`, `PlacedTrimTemplateBlockEntity`, `mixin/ItemUseOnMixin`.
- Rezepte/Aufwertungen: `TrimUpgrades`, `recipe/TrimUpgradeRecipe`, `recipe/ToolShapelessRecipe`, `TemplateTools`, `TrimGlow`, `TrimsRegistry`,
  `SimpleTrims`, `config/TrimsConfig`, `fabric/TrimsFabric`, Rezepte `inverting_armor_upgrade`, `*_duplication`, `fabric.mod.json`.
- Teilweise (Pixelmathematik/Client): `TrimInversion` (rein rechnerisch, kein Weltzugriff), `TrimPulseTextures` (Cache nur Client, mit Reload-Schutz),
  `mixin/client/*` (nur Eintrag im `client`-Abschnitt geprueft).
- Handbuch: `wiki/manual.json` (EN+DE).
- Nicht gelesen: `PlacedPlate`/`PlacedSolids` (Rechteck-/Maskenrechnung, Kern-Dubletten), `PlateHint`, `PlacedTrimTemplateRenderer`,
  `TrimsJeiPlugin`, `TrimsGuide`, Lang/Texturen, Forge/NeoForge, Tests (nur Muster).

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigenen Payloads. Vorlagen legen/schmieden laufen ueber Vanilla-Klicks (Reichweite und Spawnschutz prueft der Paket-Handler); Block-Entity-Update enthaelt nur den abgelegten Stapel. |
| C2 Server-Autoritaet | Aufwertung, Platzieren und Licht entscheidet der Server (`isClientSide()` -> nur Vorhersage). Client-Klassen nur in `client/`. |
| C3 Dupes | Kein Dupe gefunden. Abgelegte Vorlage: `consume(1)` beim Legen, `getDrops` gibt den gespeicherten Stapel genau einmal zurueck (kein Beutetabelleneintrag), Aufwertung ersetzt die Vorlage 1:1 und zahlt Material erst im letzten Schlag nach erneuter Pruefung. Schmiedetisch: `copyWithCount(1)`, Obergrenzen (Leuchten 1, Strahlkraft 5, Pulsieren 1, Invertieren 3) -> `ItemStack.EMPTY` = keine Passung. Kopierrezepte 1 Vorlage + 7 Diamanten + Block = 2 (kein Zirkel). `ToolShapelessRecipe` gibt das Werkzeug mit 1 Haltbarkeit weniger zurueck, Unzerstoerbare unveraendert. |
| C4 NBT/Komponenten | Strahlkraft in `custom_data` (`SimpleBuildingEmissionLevel`, beim Lesen nicht auf 5 gedeckelt, aber Licht wird auf 15 gekappt); `GLOW_LEVEL` auf 1 normalisiert, `INVERTED_TRIM` `intRange(1,3)` mit Alt-Boolean. Block-Entity laedt `Template` ueber den Vanilla-Codec. |
| C5 Tick-Kosten | Lichtlogik fuer **jedes** Lebewesen im `LivingEntity#tick` (alle 4 Ticks, Ruestungsstaender alle 10, Spieler alle 2): je Aufruf `wornEmission` (vier Slots, `EquipmentSlot.values()` legt ein Array an, `TrimGlow#getEmissionLevel` kopiert `custom_data`) - klein, siehe P3-3. Platzierte Vorlagen: Block-Entity-Ticker alle 10 Ticks mit Suche nach dem naechsten Spieler im Umkreis 6 nur bei Aufwertbarkeit. Beim Gehen zwei `setBlock` je Block (Altlicht entfernen, Neulicht setzen). |
| C6 Leaks | `PLAYER_LIGHTS` (UUID -> Position+Dimension): Eintrag wird beim Entfernen des Spielers (jeder Grund) und beim Serverstopp (`PlayerState.onStop`) geloescht. `STALE` (hoechstens 1024) wird beim naechsten Spieler-Tick gekehrt, aber von `forgetAll` nicht geleert (P3-4). Absturz: markierte Lichter raeumt `OwnedLights#sweep` beim Chunk-Laden. |
| C7 Sicherheit/Rechte | Platzieren/Aufwerten/Umschalten prueft `mayChange` (Abenteuermodus, Spawnschutz, `Protection`); **Zielblock-Pruefung fehlte (P2-2, gefixt)**. Licht ersetzt nur Luft, Wasserquellen und eigene Lichter - **Wasserquellen-Pruefung traf Seegras, Tang und alle wassergefuellten Bloecke (P2-1, gefixt)**. Fremde `minecraft:light` bleiben unberuehrt (Test `foreignLightUntouched`). Mixins: `ItemUseOnMixin` nur mit Schleichen + Vorlage; `LivingEntityTrimsMixin` fuer alle Lebewesen (Tick, siehe C5). |
| C8 Unabhaengigkeit | Allein spielbar (Axt-Modus; Standalone-Ziel, SimpleLib per Jar-in-Jar, `depends simplelib`). Hammer-Partner nur ueber Tag `#simplelib:hammers` (optional). |
| C9 Doku/Pflege | Keine TODO/FIXME. 7 Kern-Dubletten (`TrimPulseTextures`, `ItemUseOnMixin`, `OwnedLightHolder`, `PlacedPlate`, `PlacedSolids`, `PlacedTemplates`, `TrimUpgrades`): die Fixes P2-1/P2-2 muessen in den 26.2-Kern-Kopien nachgezogen werden (P3-5). Handbuch: Inverting/Herz des Meeres fehlte, Hammer-Text ungenau (P3-6, gefixt). Lang EN=DE (`check_data.py` gruen). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simpletrims-P2-1 | **P2 (Weltschaden)** | `TrimLight#placeOrUpdate` (TrimLight.java:166-183): Die Bedingung "Wasserquelle" war `getFluidState().is(WATER) && isSource()`. Tang, Seegras, hohes Seegras, Blasensaeulen und **jeder wassergefuellte Block** (Leiter, Falltuer, Korallenfaecher, Seegurken, Ketten, Laternen ...) melden Quellwasser als Fluessigkeit. Trug ein Spieler oder Ruestungsstaender Strahlkraft-Ruestung (oder eine Fackel/Laterne in der Hand) unter Wasser und lag der Kopfblock in so einem Block, wurde er durch einen Lichtblock ersetzt und beim Weggehen durch **gewoehnliches Wasser** ersetzt: der Tang-/Seegraswald hinter einem leuchtenden Taucher war weg, ohne Drops. Ebenso wurde ein fremdes wassergefuelltes `minecraft:light` (Kartenbauer) ueberschrieben (Review F-04 galt nur trocken). Gleicher Fehler im 26.2-Kern: `common/.../util/DynamicLightHandler.java:145`. | **gefixt**: nur Luft, der **reine Wasserblock** als Quelle (`current.is(Blocks.WATER)`) oder das eigene stehende Licht; das Wasser-Flag stammt aus dem Wasserblock bzw. dem eigenen Licht. Test `radiance_light_spares_waterlogged_blocks` (neu, ohne Fix rot: Falltuer wird ersetzt). Folge: Im Tang/Seegras bekommt der Kopfblock kein Licht (Licht fehlt dort, statt die Pflanze zu zerstoeren). |
| simpletrims-P2-2 | **P2 (Schutz)** | `PlacedTemplates#tryPlace` (PlacedTemplates.java:68-91): `mayChange` (Spawnschutz, Claims) wurde nur fuer den **angeklickten** Block geprueft (Z. 71), die Vorlage landet aber im Nachbarblock (`place.getClickedPos()`), beim Klick auf eine Claim-Grenze also im fremden Claim (Platzieren ueber die Grenze). Die Pruefung des Zielblocks fehlte. Der 26.2-Kern (`common/.../util/PlacedTemplates.java:218-230`) prueft beim Platzieren nur `mayBuild` (weder Spawnschutz noch Claims noch Zielblock). | **gefixt**: zusaetzliche `mayChange`-Pruefung des Zielblocks (PlacedTemplates.java:79). Test `placing_checks_the_placed_block` (neu, ohne Fix rot: der Testanbieter sperrt nur den Zielblock). |
| simpletrims-P3-1 | P3 (A12/Besitzer) | Lichtblock-Eingriff ohne A12-Steckbrief (BEW39 Q): Radiance **und** Handlicht (N52, Fackel/Laterne/Lavaeimer) setzen alle 2 Ticks einen unsichtbaren Lichtblock in die Welt (nur Luft/Wasser), "Licht aus der Hand" ist thematisch kein Besatz; `TrimsConfig` kennt nur `enabled`, kein Schalter fuer Handlicht; keine MSPT-Messung (L9 nur Opt-Mods). | **Rueckfrage** F1 |
| simpletrims-P3-2 | P3 (Client) | Inverting rendert falsch (WF25-12, BEW39 orange, `EquipmentLayerRendererMixin` / `TrimInversion`); der Client-Smoke zeigt keinen inversen Besatz. Pixelmathematik ist per Test `trimInversion` nur als Daten abgesichert. | nur notiert (Client-Abnahme durch Besitzer) |
| simpletrims-P3-3 | P3 (Tick) | `TrimLight#wornEmission` (TrimLight.java:75-81) ruft `EquipmentSlot.values()` (Array-Kopie) und `TrimGlow#getEmissionLevel` (`copyTag()` des ganzen `custom_data`) fuer jede Ruestungsstueck-Pruefung; `tickLiving` (:109) laeuft fuer alle Lebewesen. Fuer Mobs ohne Ruestung entfaellt das (leere Stapel), mit fremder Ruestung voller `custom_data` entsteht je 4 Ticks eine Kopie. | Liste: statische Slot-Liste, `CustomData#contains`/`getUnsafe` statt `copyTag()` |
| simpletrims-P3-4 | P3 | `TrimLight#forgetAll` (TrimLight.java:222) leert `STALE` nicht: Im Einzelspieler-Weltwechsel kaeme ein altes Licht in die neue Welt, wird dort aber durch die `OwnedLights`-Markierung (`isMarked`) nicht entfernt (kein Schaden). Ueber 1024 Eintraege (:47,191) bleibt die Bereinigung dem `OwnedLights`-Chunk-Sweep ueberlassen. | Hinweis |
| simpletrims-P3-5 | P3 | 7 Kern-Dubletten (`tools/dup_pairs.txt`); `DynamicLightHandler#placeOrUpdate` (Kern, 26.2, Z. 145) hat den Fehler P2-1, `PlacedTemplates#tryPlace` (Kern, Z. 218-230) hat noch weniger Schutz als P2-2; beide auf 26.3 nicht registriert (Ports erst auf Anforderung). | Liste: beim 26.2-Port nachziehen |
| simpletrims-P3-6 | P3 (Doku) | Handbuch (`wiki/manual.json`): Die DE-Zusammenfassung der Besatz-Aufwertungen nannte "Invertierend" nicht, das Herz des Meeres als zweites Inverting-Material (Schmiedetisch `inverting_armor_upgrade`, Schmieden in der Welt mit 2 Prismarinkristallen) stand nirgends, "nur der Vorschlaghammer" war ungenau (Tag `simplelib:hammers`). | **gefixt** (`wiki/manual.json` EN+DE: `trim_upgrades`, `template_tool_rule`, `simpletrims:inverting_trim_template`); `wiki/generate.py` muss der Hauptagent laufen lassen |
| simpletrims-P3-7 | P3 (nicht gebaut) | Invertier-Vorlage ohne Simple-Money-Preis (BEW39 V, `links/simpletrims` nur Glowing/Emitting/Pulsating) und Handelsangebot fehlen - "nicht gebaut (BEW39 missing)". | Besitzer-Entscheid steht aus |

## Besitzer-Fragen (fuer die Vorschau AUDSB31E1)
**F1 Lichtblock-Eingriff und Handlicht (simpletrims-P3-1, A12).** Kontext: Radiance-Ruestung und Handlicht (N52) setzen dauernd einen unsichtbaren Lichtblock in die Welt; ohne Schalter fuers Handlicht und ohne Messung der Tick-Zeit.
- A: lassen, A12-Satz in PRINZIPIEN/Store-Text (Eingriff mittel, nur Luft/Wasser, wird sauber entfernt - mit den Fixes).
- B: Handlicht (N52) getrennt abschaltbar machen (Config `heldLight`, Standard an) oder in ein anderes Modul verlagern.
- C: vor der Freigabe MSPT messen (Server mit 20 leuchtenden Spielern und 200 Mobs) und erst dann entscheiden.
- Empfehlung: **A + C** - die Lichtbloecke sind durch Markierung (`OwnedLights`) und die Fixes jetzt unschaedlich; ein Messlauf zeigt, ob B noetig ist.

## Urteil
**shipbar nach Fix** (0 P1, 2 P2 gefixt, 7 P3). Besonders sorgfaeltig: Obergrenzen der Aufwertungen, Ein-Mal-Rueckgabe der Vorlage, Absturzsicherung der Lichter ueber `OwnedLights`.

## Testergebnis
Lokal: javac ueber `/root/sb-tools-a/audsb31e1/javac.sh simpletrims` fuer `TrimLight`, `PlacedTemplates`, `TrimsTests`, `ModuleGameTest` - keine Fehler in geaenderten Dateien (nur die bekannten Access-Widener-Meldungen in `TrimsRegistry` und `LibMaterials`); `modules/simpletrims/tools/check_data.py` gruen (prueft auch die Test-Struktur); `tools/check_dup_impl.py` 0 Probleme (253 Paare). sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: die zwei neuen Gametests wurden nicht ausgefuehrt (kein Gradle); "ohne Fix rot" ist aus dem Code abgeleitet. Nicht geprueft: echter Claim-Anbieter, Client (Inverting, Lichtwirkung), Forge/NeoForge, Lichtlast unter echter Last.
