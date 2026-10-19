# Plan: Better Item Frames umsetzen (B, b-itemframes, 19.10.2026)

Grundlage: `docs/ai/KONZEPT-BETTER-ITEM-FRAMES-2026-10-19.md`, Empfehlungen übernommen (Besitzer: Konzept + Umsetzung).

## Ist-Zustand (erhoben)
- **Vanilla 26.3** (`ItemFrame`, dekompiliert aus dem Loom-Jar): `Invisible` (Entity-Flag, `setInvisible`) und
  `Fixed` (privates Feld `fixed`) werden gespeichert, aber nur per Befehl/NBT gesetzt. `fixed` heißt in Vanilla:
  `interact` gibt `PASS` (kein Einlegen/Drehen), `hurtServer` nur für Kreativ/`bypasses_invulnerability`
  (Item bleibt drin, Rahmen bricht im Überleben nicht), kein Abfallen ohne Wand, kein Verschieben.
  `GlowItemFrame` erbt `interact` unverändert → Leucht-Rahmen gelten automatisch mit.
  → Wir setzen **genau diese Vanilla-Felder**, kein eigener Zustand (kein Tag, keine Komponente).
- **Simple Maps** `ItemFrameMixin` (HEAD von `interact`): nur ohne Schleichen und nur bei Wegfinder-Karte → öffnet die Karte.
  Unsere Aktionen laufen nur beim Schleichen → disjunkt, der Karten-Klick bleibt unberührt.
- **Abweichung Zielmod:** Das Konzept nennt „Simple Quality of Life“. QoL ist seit der Aufteilung (PLAN-QOL-AUFTEILUNG-2026-10-10)
  ein reiner Super-Mod; `modules/simplequalityoflife/tools/check_data.py` erzwingt „No gameplay mixins in the super-mod“
  und „Super-mod config holds only the sub-mod switches“. Das Feature kommt deshalb in die gebündelte QoL-Sub-Mod
  **Simple Tools** (`simpletools`): Werkzeug-Aktionen in der Welt (wie „Ofen mit Lava füllen“), dort liegen Mixins,
  Config, Befehle, Wiki. Es ist damit in QoL enthalten (und mit „Enable Simple Tools“ abschaltbar) und allein spielbar.
  Tests laufen folglich in `module-simpletools-*` (QoL-Ziele führen die Sub-Mod-Tests nicht).

## Verhalten (alles serverseitig, nur beim Schleichen, nur Rahmen MIT Item)
| Hand (schleichend) | Rahmen nicht gesperrt | Rahmen gesperrt |
|---|---|---|
| Schere (`ShearsItem`) | unsichtbar ↔ sichtbar, Schere −1 (Unbreaking zählt, Kreativ nichts), Ton `SHEARS_SNIP` | Vanilla (PASS) |
| Honigwabe | sperren (`fixed=true`), Wabe −1 (nicht Kreativ), Ton `HONEYCOMB_WAX_ON`, Partikel `WAX_ON` | Vanilla (PASS) |
| Axt (`#minecraft:axes`) | rückwärts drehen | entsperren, Axt −1, Ton `AXE_WAX_OFF`, Partikel `WAX_OFF` |
| alles andere / leer | rückwärts drehen (Rotation −1, Vanilla-Drehton) | Vanilla (PASS) |
- Ohne Schleichen: reines Vanilla (und Simple Maps). Leerer Rahmen: reines Vanilla (Item einlegen).
- Wird das Item aus einem **unsichtbaren** Rahmen geschlagen, wird der Rahmen wieder sichtbar
  (Konzept „unsichtbar nur mit Item“: ein leerer unsichtbarer Rahmen wäre nicht auffindbar). Mixin am Ende von `dropItem`.
- **Wer darf entsperren?** Jeder mit Baurecht – wie gewachstes Kupfer. Eigentümer/OP-Bindung bräuchte eigenen
  Zustand (gegen die Vorgabe „nur Vanilla-Felder“); Schutz übernehmen Claims (Loader-Interaktions-Events laufen vor
  `interact`) und die Vanilla-Prüfungen `InteractionGuard.allow` (Abenteuermodus/`mayBuild`, Spawnschutz, Weltgrenze,
  Reichweite). Abenteuer-Karten mit `Fixed`-Rahmen bleiben so geschützt.
- Durchklicken durch unsichtbare Rahmen: **nicht jetzt** (später, Option erst dann).
- Kein Text auf dem Bildschirm (Regel 3.4): Rückmeldung nur über Vanilla-Töne/Partikel.

## Config (`config/simpletools.json`, Cloth-Reiter „Item Frames“, Befehle `/simpletools …`)
`enableItemFrameInvisibility` (an), `enableItemFrameLock` (an), `enableItemFrameReverseRotate` (an).
Abweichung vom Konzept: kein `shearsDamage`-Regler (Vanilla-Schere nutzt sich immer um 1 ab) und kein `clickThrough` (Stufe 3 später).

## Dateien
- neu `modules/simpletools/shared/java/.../simpletools/ItemFrames.java` (Logik), `.../mixin/ItemFrameInteractMixin.java`,
  `.../mixin/ItemFrameFixedAccessor.java`, `.../FrameTests.java`; `simpletools.mixins.json`.
- `ToolsConfig`, `ToolsScreen`, `ToolsCommands`, `ToolsTests` (ALL), Fabric `ModuleGameTest`, Lang EN/DE, `wiki/manual.json`,
  `tools/discover/discover.py` (Erst-Aufheben-Tipps Schere/Honigwabe in Simple Tools), `docs/MODS-UND-FEATURES.md`, Queue.
- generiert auf sb-test: Guides (simpletools + QoL), Wiki, Discover (Tipp-Mixin, Lang-Tipps).

## Tests (Gametests, `FrameTests`)
`frame_invisible` (Schere+Schleichen → unsichtbar, Schere −1, nochmal → sichtbar; ohne Schleichen → Vanilla-Drehung),
`frame_empty` (ohne Item kein Effekt), `frame_reverse` (Rückwärts 0→7→6, ohne Schleichen vorwärts),
`frame_lock` (Wabe sperrt, Drehen/Entnehmen/Schere gesperrt, Abenteuer-Spieler kann nicht entsperren, Axt entsperrt, Axt −1),
`frame_glow` (Leucht-Rahmen: unsichtbar + sperren), `frame_reveal` (Item rausschlagen → sichtbar),
`frame_config_off` (jede Option aus und Super-Schalter aus → Vanilla).
Ziele: `module-simpletools-fabric-263`, `-neoforge-263`, `-standalone-fabric-263`, `-standalone-neoforge-263`;
Compile Fabric/NeoForge/Forge (`:modules:simpletools:forge:compileJava`, `:modules:simplequalityoflife:forge:compileJava`);
Simple Maps `module-simplemaps-fabric-263` (Karten-Klick bleibt grün); Generatoren + `./gradlew check -q`.
Nicht getestet: Client-Sicht (keine Client-Läufe erlaubt), Forge-Laufzeit.
