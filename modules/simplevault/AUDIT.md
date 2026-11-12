# Audit simplevault (a-audsb31e2, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplemobheads/AUDIT.md`.
**Modul PAUSIERT**: nur Fehler-Audit (Abstuerze, Dupes, Rechte, Leaks, Datenverlust), keine neuen Bloecke/Items/Strukturen.
Umfang: 32 Java-Dateien / 3,3k Zeilen ohne Tests (Katalog `test/VaultTests` + Fabric-Adapter 0,4k Zeilen, `clienttest/` 9 Zeilen). Reiner Client-Code: keiner (kein Client-Entrypoint,
keine Client-Mixins). Der Mod hat **keine** Netzwerk-Payloads, **keine** Menues, **keine** Block-Entities, keine Entities und keine Befehle ausser `/simplevault config`: C1 und die
Menue-/BE-Teile von C3 entfallen. Risikogeleitet: Tor/Reise (Einsperren), Vault-gebundener Effekt und Kolben, Raetsel-Zustand, Siegelteile/Sockel/Altar (Dupe), Weltgen-Features.

## Vollstaendig gelesen
- Einstieg/Registry/Config: `SimpleVault.java`, `VaultFabric.java`, `VaultRegistry.java` (Z. 40-119), `VaultConfig.java`, `simplevault.mixins.json`, `fabric.mod.json`, Tag `vault_held`.
- Reise/Tor: `VaultTravel.java`, `VaultEntries.java`, `block/VaultGateBlock.java`, `VaultGapFeature.java`.
- Effekt/Sperren: `VaultEffect.java`, `mixin/VaultBlockStateMixin`, `VaultPistonMixin`, `VaultPistonBreachMixin` (gegen `modules/simplemachines/.../PistonBreach` geprueft).
- Raetsel/Belohnung: `VaultPuzzles.java`, `block/VaultLeverBlock`, `VaultLampBlock` (Kopf), `ChimeCrystalBlock`, `PathTileBlock`, `SealPedestalBlock`, `SealSocketBlock`, `AscentAltarBlock`,
  `AscentStreamBlock`, `SealFragmentItem.java`.
- Weltgen/Ambiente: `VaultChunkGenerator.java`, `VaultCacheFeature.java`, `VaultAmbience.java`, `gen/VaultLayout` (Z. 60-130: Cache, `of`/`at`), Dimension-/Biom-JSON (Hoehe, Mob-Spawns).
- Nicht gelesen: `gen/VaultLayout` (Rest, 780 Zeilen reine Layout-Funktion), `gen/VaultGen`, `VaultPiece`, `VaultHash`, `VaultBlocks`, `guide/VaultGuide` (ausser Quest-Aufruf), Tests/Kataloge,
  `SealPart`, Texturen/Wiki.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | keine eigenen Payloads. |
| C2 Server-Autoritaet | Reise, Raetsel, Siegelteile nur unter `instanceof ServerLevel/ServerPlayer`; Zustand liegt in Bloecken (Hebel, Lampen, Kacheln, Sockel), nur die laufende Klangfolge im Speicher. Befund P2-1 (Effekt allein). |
| C3 Dupes/Verlust | **Siegelteile**: das Podest gibt das Teil neu, solange der Sockel leer ist und das Inventar (nur dieses) kein passendes Teil enthaelt. Ein Teil passt nur zum Gewoelbe, aus dem es stammt (`vault_rx/rz`); ein Sockel nimmt genau eines (`FILLED` nie zurueckgesetzt, Bloecke unzerstoerbar). Die **Belohnung** faellt einmal, wenn der dritte Sockel gefuellt wird (`openAscent`); Zweitteile in Truhen sind wertlos: **kein Dupe**. Reise: Vanilla `teleport` (Inventar bleibt, Spieler im Boot/Pferd werden nicht aufgenommen). Zurueck-Tor und Aufstieg schicken ueber `VaultEntries` an die Eintrittsstelle, ohne Eintrag senkrecht ueber die Gewoelbe-Position. |
| C4 NBT/SavedData | `VaultEntries` (Codec mit `optionalFieldOf`, 1 Position je Spieler). Siegelteil: `custom_data` mit `getIntOr` (fehlende Schluessel passen nicht). `VaultLayout.CACHE` pro Seed/Region, bei > 256 geleert. |
| C5 Tick-Kosten | `VaultTravel.tick` nur alle 20 Ticks, nur Spieler im Gewoelbe; `VaultEffect` alle 10 Ticks, `VaultAmbience` mit Zaehler je Spieler. Raetsel nur bei Interaktion; Pfad-Schritt liest je Schritt die Pfadzellen (P3-6). Weltgen rein funktional (1,2 ms/Chunk laut BEW39). |
| C6 Leaks | `VaultAmbience.NEXT` (Logout + Verlassen), `VaultTravel.CLOSE_AT/occupiedLast`, `VaultPuzzles.CHIME` (nur je Region, klein, aber nicht bei Serverstop/Logout geleert: P3-2). Keine Spieler-/Level-Referenzen. |
| C7 Sicherheit/Rechte | `/simplevault config` ueber SimpleLib (Stufe 2), Werte geklemmt (`gapRarity` 0-100000, `closeSeconds` 5-600). Mixins: `VaultBlockStateMixin` (HEAD `getDestroyProgress`, nur `vault_held` + Dimension), `VaultPistonMixin` (HEAD `isPushable`), `VaultPistonBreachMixin` (`@Pseudo`, `require 0`, ohne Simple-Machines-Referenz); keine Client-Mixins. Alle eigenen Bloecke `strength(-1, 3600000)`, `IMMOVEABLE`, ohne Loot. Spawns: Biom ohne jede Spawn-Kategorie. Explosionen: P2-2. |
| C8 Unabhaengigkeit | `fabric.mod.json` depends simplelib >=0.4.0, `modules.json` requires simplelib: kein Widerspruch; Standalone-Ziel vorhanden; der Kolben-Mixin zielt per Klassenname auf Simple Machines und wirkt nur, wenn es da ist. |
| C9 Doku/Pflege | Lang EN=DE 76 = 76, `check_data.py` gruen, keine TODO, keine Dubletten-Paare. BEW39-Luecken (Handbuch-Kapitel, FEATURES-ALLE-MODS, Belohnung) siehe unten. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplevault-P2-1 | P2 (Rechte-Umgehung der Sperre) | `VaultEffect#held` (alt Z. 48) verlangte `player.hasEffect(Vault-bound)`. Der Effekt wird nur alle 10 Ticks (Dauer 40) vergeben; Milch, Totem oder `/effect clear` nehmen ihn weg, bis zum naechsten Vergabe-Tick (<= 9 Ticks) lassen sich Fries-/Markerbloecke (`vault_held`) abbauen. Wer den Fries des Klangraetsels abbaut, kann es nicht mehr lesen und sitzt nach dem Schliessen des Tores fest. | **gefixt**: auf dem Server haelt der Block auch ohne Effekt, wenn `VaultEffect.due` gilt (Mod an, kein Zuschauer, im Gewoelbe); der Client folgt dem synchronisierten Effekt. Reine Regel `VaultEffect#holds`; Gametest `vault_frieze_holds_without_effect` (Adapter `vaultFriezeHoldsWithoutEffect`) |
| simplevault-P2-2 | P2 (Rueckfrage, Einsperren) | Die Fries-/Markerbloecke sind Vanilla-Bloecke (`vault_held`: Amethyst, Prismarin-Ziegel, gewachstes Kupfer, Quarz, polierter Blackstone, 3 Froschlichter) mit Vanilla-Sprengresistenz 0,3-6,0; Kolben und Abbau sind gesperrt, **Explosionen nicht** (kein Treffer fuer "explo" im Modul). Sprengt ein Spieler (eigenes TNT/Endkristall) Fries oder Marker weg, ist das Klang-/Pfad-/Licht-Raetsel nicht mehr lesbar; das Tor ist zu, Wand und Boden sind Bedrock, der einzige Weg hinaus ist der Altar. Notausgang: Tod oder Operator (`/tp`). Die Bedrock-Huelle selbst ist sicher. | **entschieden (Besitzer 12.11., AUDSB31E2 F11=C)**: im Gewoelbe keine Sprengschaeden an Fries/Markern - **nur als TODO bei Wiederaufnahme von Simple Vault** (nicht gebaut; TODO in `/root/sb-memory/uebergabe-A.md`). |
| simplevault-P3-1 | P3 | `VaultTravel#gateContact` (Z. 53) verlangt `VaultRegistry.on()` auch fuer das Verlassen: wird `vault.enabled` (oder der Sub-Mod-Schalter) zur Laufzeit abgeschaltet, kommen Spieler im Gewoelbe ueber das offene Tor nicht mehr zurueck (Raetsel/Altar gehen weiter, sonst Operator-`/tp`). | nur das Betreten an `on()` binden |
| simplevault-P3-2 | P3 | Statischer Speicher wird beim Serverstop nicht geleert: `VaultPuzzles.CHIME` (Z. 46, Schluessel nur Region, nicht Welt/Spieler; `forgetChimes` ist nur Test-Hook), `VaultTravel.CLOSE_AT/occupiedLast` (Z. 33-34, absolute Spielzeit der alten Welt; `reset` nur Test-Hook). Im Einzelspieler-JVM mit Weltwechsel kann ein alter Zeitstempel das Tor sofort schliessen oder offen halten, bis das Gewoelbe leer ist; die Klangfolge zaehlt fremden Fortschritt (harmlos, die Folge ist offen lesbar). | `SERVER_STOPPED`: `VaultTravel.reset()` + `VaultPuzzles.forgetChimes()` |
| simplevault-P3-3 | P3 | `VaultEntries` behaelt den Eintrag nach Tod im Gewoelbe (kein `afterRespawn`-Gegenstueck wie in Simple Layers); er wird beim naechsten Eintritt ueberschrieben. 1 Eintrag je Spieler. | Hinweis |
| simplevault-P3-4 | P3 | `VaultPistonBreachMixin` schuetzt nur `com.simplemachines.upgrade.PistonBreach#isBreachable` (`@Pseudo`, `require 0`: bei Umbenennung stillschweigend wirkungslos, kein Gametest). Die Kern-Kopie `com.simplebuilding.util.PistonBreach` (`common/`) wird nicht erfasst; sind die Kern-Kolben in 26.3 noch registriert, durchbricht ein Reinforced/Netherit-Kolben den Bedrock im Gewoelbe. | Hauptagent: Kern-Kolben in 26.3 pruefen; Gametest mit Simple Machines |
| simplevault-P3-5 | P3 | `VaultTravel#enter` laedt den Ankunfts-Chunk nicht vorab (Gegenstueck: Backrooms FIX172). Das Tor-Chunk wird durch `gateOpen`/`setGate` geladen, der Ankunftspunkt liegt davor; ob er immer im selben Chunk liegt, wurde nicht geprueft. | `vault.getChunk(a[0] >> 4, a[2] >> 4)` vor dem Teleport |
| simplevault-P3-6 | P3 | `PathTileBlock#stepOn` -> `VaultPuzzles#step`: jeder Schritt auf einer unbeleuchteten Kachel liest alle Pfadzellen und sendet bei Fehlschritt/Start je Bewegung eine Aktionsleisten-Meldung. Keine Last, aber Meldungs-Spam. | Hinweis |
| simplevault-BEW39 | Hinweis | Handbuch ohne Config-/Befehls-Kapitel, `docs/FEATURES-ALLE-MODS.md` ohne Simple Vault, keine Questionary-Texte, JEI-Info zu Siegelteilen, "Belohnung schwach" (1 Amethyst + 2-3 Vanilla-Wertsachen), nie im echten Client gesehen. | BEW39: Feature offen, nicht Teil des Audits |
| simplevault-BEW39-2 | P3 | BEW39 "kein configCommand-Gametest" (`SimpleVault.java` Z. 34 registriert `/simplevault config`). | **nachgezogen**: `config_command` (`ConfigCommandTests.module`, Adapter `configCommand`) |

## Urteil
**shipbar im Fehlersinn** (0 P1, 1 P2 gefixt, 1 P2 Rueckfrage, 6 P3 plus 2 BEW39-Eintraege). Der Dupe-Verdacht aus BEW39 ("verlorene Teile neu holbar") bestaetigt sich nicht: die Belohnung haengt am
unumkehrbaren Fuellen des dritten Sockels. Das echte Risiko ist das Einsperren durch zerstoerte Raetsel-Hinweise (P2-1 behoben, P2-2 offen).

## Testergebnis
Lokal: `javac.sh simplevault` auf `VaultEffect.java`, `VaultTests.java`, `ModuleGameTest.java`: sauber (nur bekannte Access-Widener-Meldungen in den unveraenderten `VaultRegistry.java`
(`displayItems`) und `simplelib/.../LibMaterials.java`); `tools/check_gametest_adapters.py`: nur die bekannte Kern-Luecke; `modules/simplevault/tools/check_data.py` gruen; Lang EN=DE per Skript.
Neue Tests: `vault_frieze_holds_without_effect` (Wahrheitstafel der reinen Regel, laeuft auch standalone; die Dimension gibt es dort nicht) und `config_command`. sbt-Lauf: siehe
`docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: Gametest-Lauf (kein Gradle), echter Milch-/Totem-Fall im gebauten Gewoelbe (nur Integrationswelt), Explosionen, Mehrspieler (zwei Spieler im selben Gewoelbe, Tor schliesst), Client.
