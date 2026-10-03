# Plan Trainingspuppe + Pfeil-Station (2026-10-02)

Besitzer-Queue: „Strohpuppe / Trainingspuppe“ und „Testzentrale: Pfeil-Station“. Hauptlinie 26.3, Flag
`McVersion.TRAINING_DUMMY` (26.3 true, 26.2 false). Fabric zuerst, NeoForge/Forge 26.3 gleich mitverdrahtet.

## Recherche (bestehende Konzepte)
- MmmMmmMmmMmm (Target Dummy): wackelt beim Treffer, große Schadenszahlen, Farbe je Schadensart (Krit rot),
  DPS nach kurzer Pause = Schaden / Zeit erster–letzter Treffer, Rüstung anziehbar, Entfernen per Schleich-Linksklick,
  Kürbis als Vogelscheuche.
- Damage Dummys / Training Dummy NPC: Rüstung, Verzauberungen testen, Selbstheilung, Schild/Totem.
Übernommen: Zahlen + Farben, Krit-Kennung, DPS-Summe nach 3 s Pause (automatischer Reset), Schleich-Abbau,
Rüstung/Schutz-Verzauberungen. Nicht übernommen: Vogelscheuche (YAGNI), Herzen-Anzeige, Config (nichts nötig).

## Name
„Training Dummy“ / „Trainingspuppe“: zwei schlichte Wörter wie „Armor Stand“, im Englischen der übliche Begriff,
deutsch die geläufige Übersetzung (u. a. aus MMOs); „Target Dummy“ ist schon der Name der bekanntesten Mod.
Zwischenstufe: „Straw Armor Stand“ / „Stroh-Rüstungsständer“.

## Verhalten
- Rezept (Datagen, formlos): Rüstungsständer + Strohballen → Stroh-Rüstungsständer (Item `straw_armor_stand`).
- Stroh-Rüstungsständer (Entity `straw_armor_stand`, erbt `ArmorStand`): wie ein Rüstungsständer; Spieler-Treffer
  (2× in 5 Ticks, Kreativ sofort) und Explosion zerstören ihn, Drop = Stroh-Rüstungsständer + Ausrüstung.
- Geschnitzten Kürbis aufsetzen (Rechtsklick) → wird zur Trainingspuppe (Entity `training_dummy`, Ausrüstung,
  Name, Drehung übernommen; der Kürbis bleibt als Kopf).
- Trainingspuppe: nimmt nie Schaden, nur Schleich-Schlag eines Spielers (der dort bauen darf) baut sie ab
  (Überleben: Drop Stroh-Rüstungsständer + Ausrüstung; Kreativ: wie Vanilla ohne Drops). `/kill` und Void wirken.
  Kein Rückstoß (Rückstoßresistenz 1), keine Haltbarkeitsverluste an der Rüstung, kein Tod → kein XP/Loot.
- Anzeige: schwebende Zahl (Vanilla-`text_display`, nur Server, wird nie gespeichert/aufgeräumt), Farbe:
  weiß normal, rot fett Krit, gold Feuer, lila Magie, gelb Explosion, grau „Immun“. Höchstens 12 Zahlen je Puppe.
  Nach 3 s ohne Treffer und ≥ 2 Treffern Summe „Σ x in t s (y DPS)“ für 3 s, dann Reset.
- Schadensrechnung wie `LivingEntity.hurtServer` für den simulierten Mob: Unverwundbarkeit des Mobs (Feuerimmun,
  Fall), Enderman gegen Geschosse immun, Frost ×5 für frostempfindliche Typen, Trefferpause (10 Ticks, nur
  Differenz), Rüstung = Puppen-Rüstung + natürliche Rüstung des Mobs (Zombie 2), Schutz-Verzauberungen und
  Resistenz der Puppe.
- Kopf → Mob-Art: `SkullBlock.Type`-Name (`zombie`, `simplebuilding:spider`, `simplefun:pig`, `dragon` →
  `ender_dragon`) → `minecraft:<pfad>`, sonst `<ns>:<pfad>`. Spielerkopf/kein Kopf/Kürbis = keine Art.
- Bann/Schärfe/Nemesis-artige Verzauberungen: Mixin auf `EnchantmentHelper.modifyDamage` und
  `modifyFallBasedDamage` tauscht das Opfer gegen einen nicht gespawnten Stellvertreter-Mob (Cache je Puppe), d. h.
  jede datengetriebene Bedingung über Entity-Type-Tags (auch fremder Mods) wirkt korrekt, inkl. Pfeilen und
  Magie-Krit-Partikeln. Befiederungs-Spitzen (`ArrowParts`) lesen die Art über denselben Helfer.
- Krit: Mixin am Kopf von `Player.attack` merkt die Schlagstärke an der Puppe; Krit = volle Stärke + Vanilla-
  Fallbedingungen. Pfeil-Krit über `isCritArrow`.

## Dateien
- `mc26_3/overlay/.../McVersion.java`, `common/src/mc26_2/.../McVersion.java`: Flag.
- `common/src/shared/java/com/simplebuilding/dummy/`: `TrainingDummy`, `DummyTargets`, `StrawArmorStandItem`,
  `client/TrainingDummyRenderer`.
- `entity/ModEntities`, `items/ModItems`, `ModItemGroupsContent`, `SearchTabPlacement`, `fletching/ArrowParts`.
- Mixins `EnchantmentHelperDummyMixin`, `PlayerAttackDummyMixin` (+ `simplebuilding.mixins.json`).
- Attribute + Renderer: Fabric (`Simplebuilding`, `SimplebuildingClient`), NeoForge (`SimplebuildingNeoForge`,
  `...Client`), Forge 26.3 (`mc26_3/forge/...`).
- Ressourcen (Overlay 26.3): Item-Modell, Texturen (Generator `tools/textures/training_dummy.py`), Lang EN/DE beide Orte.
- Datagen-Rezept; Testzentrale: `TcOp.Dummy`, `TcCanvas.dummy`, Builder, Layout, Kits, Station `archery`.
- Tests `TrainingDummyTests` + `TrainingDummyGameTest` (Fabric) + Katalog; Testzentralen-Test für die Station.

## Pfeil-Station (`archery`, nach `arrows`)
Schusslinie mit Truhen: Bogen, Armbrust (je mit/ohne Verzauberungen), alle 72 Befiederungs-Pfeile, Vanilla-,
Spektral- und alle Trank-Pfeile (aus der Trank-Registry). Ziele in 10 Blöcken: Trainingspuppen mit Kürbis, Zombie-,
Ertrunkenen-, Skelett-, Spinnen- und Endermankopf, eine mit Eisenrüstung. Kit-Knopf gibt Bogen, Armbrust, Pfeile.

## Risiken
- Stellvertreter-Mob ohne Welt-Eintrag: nur Typ-/Attributabfragen, nie `addFreshEntity`. Spielerkopf → kein Proxy.
- `text_display` per NBT erzeugt (Setter privat); Zahlen tragen Tag `simplebuilding_dummy_number`, Puppe räumt
  verwaiste nach dem Laden weg.
- 26.2 kompiliert den gemeinsamen Code (Flag false, Typen null).

## Verifikation
GameTests (Umwandlung, Unzerstörbarkeit, Schleich-Abbau mit Drops, Bann gegen Zombiekopf, Schärfe, Rüstung,
Feuerimmunität Lohenkopf, Trefferpause, Krit, Pfeil-Spitze gegen Zombiekopf, Zahl erscheint und verschwindet, DPS-
Summe), Testzentralen-Abdeckung + Station; `fabric-263`, `neoforge-263`, 26.2-Compile, Forge-26.3-Compile, Datagen,
Wiki-Check, Texturen-Vorschau. Nicht testbar ohne Client: Rendern der Puppe/Zahlen (Client-Abnahme offen).

## Stand nach Umsetzung (2026-10-02)
- Umgesetzt wie geplant. Abweichungen: Testzentralen-Puppen nutzen das bestehende `TcOp.Stand` mit Flag `dummy`
  statt eines neuen Schritts; im Mod-Tab steht nur der Stroh-Rüstungsständer (Vanilla-Items gehören nicht in Mod-Tabs,
  DataIntegrity), in der Suche direkt hinter dem Vanilla-Rüstungsständer. Der Stroh-Rüstungsständer brennt nicht (wie
  geplant vereinfacht: nur Schlag/Explosion).
- Getestet: 6 GameTests `training_dummy_*` (Fabric, NeoForge, Forge 26.3), volle Server-Suite Fabric/NeoForge 26.3, 26.2-Compile,
  Forge-26.3-Compile, Datagen, Wiki-Check, Texturen-Check.
- Nicht getestet: Rendern (Puppe, Zahlen, Summe) im Client; echtes Schießen mit Bogen auf die Puppe nur indirekt
  (Schadensquelle + Verzauberungs-Mixin). Testzentrale in der Besitzerwelt nicht neu gebaut.

## Runde 2 (Besitzer 2026-10-03)
1. Kürbis auf den **Stroh**-Rüstungsständer: Kürbis wird verbraucht (nicht aufgesetzt), es entsteht die Puppe mit eigener
   Optik (Sackkopf mit Gesicht und gestopfter Rumpf als Render-Ebene über dem Ständermodell). Normale Rüstungsständer
   bleiben unverändert: Kürbis auf Rüstungsständern ist ein verbreitetes Vanilla-Deko-Mittel (Vogelscheuchen-Bauten,
   Schneemann-Optik) und darf nicht plötzlich eine neue Entity erzeugen.
2. Bug „nur der erste Treffer zählt“: `cooldownStart = Long.MIN_VALUE` → `now - cooldownStart` läuft über, ist negativ,
   jeder Treffer landete in der Trefferpause und zählte nur, wenn er stärker als der bisher stärkste war. Fix: eigener
   Merker „letzter angenommener Treffer“, Puppe wackelt + klingt bei jedem Treffer, abgewiesene Treffer zeigen graue 0.
3. Schere (Rechtsklick) auf die Puppe: zurück zum Stroh-Rüstungsständer, Kürbis droppt, Ausrüstung bleibt, Schere −1.
4. Neues Item `training_dummy` (stellt die Puppe direkt auf, wie das Ständer-Item); Schleich-Abbau droppt es.
   Zusätzlich formloses Rezept Stroh-Rüstungsständer + geschnitzter Kürbis → Trainingspuppe (auffindbar in JEI).
5. Vogelscheuche: Im Umkreis (Server `server.features.scarecrowRadius`, Standard 8, 0 = aus, max 16) eines
   Stroh-Rüstungsständers oder einer Trainingspuppe zertrampeln Tiere und Monster kein Ackerland (Spieler weiterhin).
   Begründung: echte Vanilla-Plage (Kühe, Schafe, Zombies auf Feldern), Haken existiert schon in allen Loadern
   (`HeadAbilities.tramplesFarmland`), kein Ertrag/kein Item entsteht → kein Exploit; Suche je Trampel-Ereignis mit
   begrenzter Box. Verworfen: Krähen (gibt es nicht), Kaninchen/Fuchs-Ziele (Goal-Klassen je Version anders, kleiner Nutzen).
