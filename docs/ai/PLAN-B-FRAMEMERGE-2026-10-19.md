# Plan: Rahmen-Aktionen SimpleBuilding -> Simple Tools (B, b-framemerge, 19.10.2026)

## Ist-Zustand
- SB `common/src/shared/.../mixin/ItemFrameEntityMixin.java` (gilt für 26.2 UND 26.3, gleiche `simplebuilding.mixins.json`):
  eigener Zustand `simplebuilding$locked` (NBT `SimpleBuildingLocked`), Schleichen + Glasscheibe sperrt (Scheibe -1),
  Schleichen auf gesperrten Rahmen entsperrt (wirft Scheibe), Schere versteckt, Schleichen zeigt. Dazu Radiance-Licht
  (`OwnedLightHolder`) und Magnet-Filter (Constructor's Touch) im selben Mixin - die bleiben in SB.
- Simple Tools (`ItemFrames`, Mixin Prio 1100): Schere = Invisible, Honigwabe = Vanilla `Fixed`, Axt entsperrt, Rückwärtsdrehen.
- Simple Tools existiert nur auf 26.3; SB läuft auch allein (SB hängt nicht von Simple Tools ab) und auf der 26.2-Linie.

## Entscheidung (Abweichung vom Wortlaut "Mixin entfernen" begründet)
Der SB-Mixin wird nicht gelöscht, sondern **tritt zurück, sobald `simpletools` geladen ist** (`ModEnvironment.isModLoaded`):
dann schreibt/liest/prüft er `SimpleBuildingLocked` nicht mehr und führt die Schleichen-Aktionen nicht aus (Magnet + Licht bleiben).
Grund: Modul-Unabhängigkeit (SB allein und 26.2 haben kein Simple Tools; sonst verlören sie Sperre/Verstecken ersatzlos).
Mit Simple Tools gibt es genau EIN Verhalten, in Simple Tools.

## Umsetzung
1. SB-Mixin: `simplebuilding$toolsOwnFrames()` (gecachtes ModEnvironment-Flag) schaltet Tag-Schreiben, -Lesen, Schutz und die Schleichen-Zweige ab.
2. Simple Tools: Glasscheibe sperrt zusätzlich (wie Honigwabe -> Vanilla `Fixed`, Scheibe -1, Glas-Platzierton, Config `enableItemFrameLock`).
   Entsperren nur per Axt (Scheibe wird nicht mehr zurückgeworfen).
3. Migration: Simple-Tools-Mixin `readAdditionalSaveData` TAIL: `SimpleBuildingLocked=true` -> `fixed=true` (unabhängig von Config, jedes Laden;
   Tag verschwindet, weil SB ihn mit Simple Tools nicht mehr schreibt - einmalig und idempotent). Greift auch ohne SB im Spiel.
4. "Schleichen zeigt" bewusst weggelassen: Sichtbarkeit schaltet die Schere um; Schleichen + leere Hand dreht rückwärts (Konflikt), ein
   Hervorheben bräuchte Client-Code (Nicht trivial). Begründet im Wiki.
5. Lang/Wiki: Simple-Tools-Eintrag `item_frames`, Config-Tooltip, Erst-Aufheben-Tipp (Glasscheibe); SB-Wiki `item_frame_lock` verweist auf Simple Tools.
   Guides/Wiki/Discover werden auf sb-test neu erzeugt.

## Tests
`FrameTests`: `invisible`/`lock`/`configOff` ohne SB-Sonderfälle (Integration mit SB -> genau eine Aktion, Schere verschleißt 1),
neu `frame_glass` (Scheibe sperrt, 2. Scheibe nicht verbraucht, Axt entsperrt, kein Drop, leerer Rahmen Vanilla) und `frame_migrate`
(alter Tag -> Fixed, Resave ohne Tag). Ziele: module-simpletools-fabric-263/-neoforge-263 + standalone, fabric-263 `simplebuilding:*frame*`
(SB allein, Legacy unverändert), Compile Fabric/NeoForge/Forge + 26.2, `check`.
Nicht getestet: Client-Sicht, Forge-Laufzeit, 26.2-Lauf (nur Compile).
