# Audit simplemachines (b-audsb31-a, 11.11.2026)

Basis `hub/b-bundle182` 9b5764995, nur MC 26.3 Fabric. Aufbau wie `modules/simplemodels/AUDIT.md` (b-rel32).
Umfang: 70 Java-Dateien / 12,1k Zeilen (davon ~45 % Tests). Risikogeleitet geprueft; fruehere Audits im Code vermerkt
(`docs/AUDIT-2026-09-26.md` #47 Brecher-Geisterbloecke, Nach-Audit N16 Kolben-Regeln) wurden nicht wiederholt.

## Vollstaendig gelesen
- Netzwerk: `upgrade/PistonConfigPayload.java` (S2C), Registrierung `PistonsFabric.java`, `MachinesFabricClient.java`.
- Menues: `crafter/AutonomousCrafterMenu.java`, `hopper/TierHopperMenu.java` (Filterknopf, FilterSlot), `smither/AutoSmitherMenu` (stillValid/quickMove).
- BlockEntities: `smither/AutoSmitherBlockEntity.java` (Verbrauch, Ergebnis-Reserve, Slots je Seite), `crafter/AutonomousCrafterBlockEntity` (Slot sperren, canTakeItem),
  `hopper/TierHopperBlockEntity#canTakeItem`, `hopper/TierHopperMinecart` (Ansaugen).
- Kolben: `piston/DoublePistons.java` (pushLine gegen Vanillas `PistonBaseBlock#moveBlocks` im 26.3-Jar verglichen),
  `upgrade/NetheriteBreakerPistonBlock#triggerEvent`, `upgrade/PistonBreach` (Regeln), `PistonGuard`-Aufrufe.
- Nicht gelesen: Screens, Discover/Guide, Mixins nur per Zielliste, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Nur S2C (`PistonConfigPayload`, 2 Booleans). Keine C2S-Pakete; Menue-Knoepfe ueber Vanillas `clickMenuButton`, Server prueft offenes Menue und Id-Bereich. |
| C2 Server-Autoritaet | Brecher bricht nur auf dem Server (Client bekommt die Entfernung vor dem Block-Ereignis). Slot-Sperre im Crafter nur fuer leere Slots. |
| C3 Dupes | Auto-Schmied reserviert den Ergebnisplatz vor dem Verbrauch; Ergebnis nur aus dem Ergebnis-Slot entnehmbar; Crafter gibt Trichtern nichts (`canTakeItem=false`). Doppelkolben: **P2-1**. |
| C4 NBT | BEs laden mit `ContainerHelper` (Vanilla-Grenzen), Minecart-Filter als Enum. |
| C5 Tick-Kosten | Trichterwagen: Entity-Abfrage nur im eigenen Rahmen; Brecher nur bei Block-Ereignis. |
| C6 Leaks | `ADDITION_COUNT` (Klasse -> Methode) waechst nur mit Rezeptklassen. |
| C7 Schutz | `PistonGuard.mayAutomate/mayMove/mayBreak` je Aktion; Admin-Bypass nur LEVEL_OWNERS. Grundgestein-Durchbruch ist Feature (bezahlt, Tags `piston_breach_immune`). |
| C8 Unabhaengigkeit | allein spielbar (Standalone-Target). |
| C9 Doku | 12 Kern-Dubletten (AUDIT-A); Daempfer-Textur Platzhalter (AUDIT-A). |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simplemachines-P2-1 | P2 (Datenverlust) | `DoublePistons.pushLine` liess die Zelle eines zerstoerten Blocks stehen und setzte den bewegten Block mit `UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS` darauf. Ein zerbrechender Behaelter (Verzierter Krug) verlor seinen Inhalt; das `BLOCK_DESTROY`-Spielereignis (Sculk) fehlte. | **gefixt** 056a68ad9: Luft (Flag 18), Partikel, Spielereignis wie Vanilla; Gametest `double_piston_breaks_a_pot_with_its_item` |
| simplemachines-P3-1 | P3 (Sync) | Die Kolben-Regeln des Servers blieben nach dem Trennen im Client; auf einem Server ohne den Mod spielte der Client Verstaerkte Kolben mit fremden Regeln nach (Geisterbloecke moeglich). | **gefixt** (DISCONNECT setzt zurueck); kein Gametest moeglich (Client) |
| simplemachines-P3-2 | P3 | 12 Kern-Dubletten (u. a. `AutoSmitherBlockEntity`, `AutonomousCrafterBlockEntity`, Kolben): Fixes muessen im 26.2-Kern nachgezogen werden. | Liste |

## Urteil
**shipbar** (0 P1, 1 P2 gefixt, 2 P3, davon 1 gefixt).

## Testergebnis
Lokal: javac der geaenderten Dateien (nur bekannte Access-Widener-Meldungen), `check_data.py` gruen. sbt siehe Plan.
Nicht getestet: Client (P3-1), Forge/NeoForge.
