# Plan A-DINOREST1: DINO-REST Schritt 1 – Gemeinsames + Triceratops

Stand 12.11.2026, Agent a-dinorest1, Branch `a-dinorest1` (Basis `hub/b-bundle189` 6872a59f6). Nur MC 26.3 Fabric.
Auftrag: `/root/sb-tools-a/agenten-1530.md` §14b. Konzept: `docs/ai/KONZEPT-A-DINO36-2026-11-12.md` (Besitzer DINO36: im Prinzip
angenommen, Empfehlungen der 4 Fragen gelten: T.-rex-Sprint gestrichen, Items §6, Fundort-Gewichtung §7, Reihenfolge §9),
Hitbox-Lösung `docs/ai/PLAN-A-DINO36B-2026-11-12.md` (DINO36b = ok). Vorschau `/root/sb-tools-a/previews/a-dinorest1-preview.html`,
Antwortcode **DINOREST1**.

## 1. Ist-Stand (gelesen)

- `Dino extends AbstractHorse`: Wachstum, Fell je Aufzucht-Biom, Teil-Hitboxen (`rig()`, `updateParts`, Kopf weicht aus, Blätter zählen
  nicht), Biss aus Kopfweite. **Kampf-Ziele (MeleeAttack + Besitzer verteidigen) liegen in `Dino`** → jeder neue Dino hätte sie (und
  ohne `ATTACK_DAMAGE`-Attribut einen Absturz beim ersten Angriff).
- `DinoKind.entity()` liefert nur für TREX einen Typ; `roll()` würfelt nur unter schlüpffähigen Sorten, ohne Biom.
- `Trex`: `RIG` aus `TrexBones` (generiert von `tools/gen_models.py` aus derselben Spezifikation wie `TrexLayers`), Renderer skaliert mit
  `ageScale`, Texturen `entity/trex/<fell>/<stufe>.png` (5 × 4), Rüstung über Tag `can_wear_dino_armor` (= nur trex).
- Items: Großer Sattel (Tag `can_equip_large_saddle`), T.-rex-Rüstung ×3 (`optik32_items` Variante C), Eier (a-dino36b, `eggs.py`, C).
- Handbuch-Kapitel generiert aus `modules/simpledinos/wiki/manual.json` (`tools/guides/module_guides.py`), Discover generiert
  (`tools/discover/discover.py`), Kopf-Ausnahme `simplemobheads/data/mobs.json` (→ `generate_heads.py`), Preise
  `simplemoney/.../simpledinos/prices.json`, Testzentrale-Liste in `SimpleDinos.init`, `check_data.py` prüft Keys/Tags/Texturen.
- Vanilla 26.3 (dekompiliert `/root/mcsrc263np`, Signaturen per `javap` gegen `minecraft-merged.jar`): `PlayerRideableJumping`
  (Client ruft beim Loslassen `onPlayerJump` lokal + schickt `START_RIDING_JUMP` → Server `handleStartJump(scale)`), Kamel-Sprint
  (`executeRidersJump` auf der steuernden Seite, `getJumpCooldown` sperrt den Balken), Bewegung eines gerittenen Tiers rechnet der Client
  des Reiters (`isLocalInstanceAuthoritative`), Ziegen-Rammstoß (`RamTarget`: Schaden + `knockback`), `LeavesBlock.PERSISTENT`,
  `CocoaBlock.AGE` (max 2), `Block.getDrops(state, level, pos, be)` = Abbauen mit leerer Hand, Baby-Füttern = Vanilla `Animal`
  (−10 % Restzeit), `LivingEntity.knockback(power, dx, dz, source, damage)`.

## 2. Umfang

### 2a. Gemeinsames (Konzept §9 Schritt 1)
1. Kampf-Ziele + `defends` von `Dino` nach `Trex` (Verhalten des T. rex unverändert).
2. Sprungtasten-Rahmen in `Dino`: synchronisierte Fähigkeits-Abklingzeit `ABILITY_COOLDOWN` (Server zählt, Client-Sprungbalken zeigt
   sie über `getJumpCooldown`), `usesJumpAbility()` (T. rex: nein = normaler Sprung).
3. Futter je Art: `Dino#isFood` bleibt Fleisch (T. rex); Arten überschreiben; Lieblingsfutter heilt 8, anderes 4; Jungtier-Füttern
   = Vanilla (−10 % Restzeit).
4. Eierschalen ×4 (`triceratops_eggshell`, `stegosaurus_eggshell`, `utahraptor_eggshell`, `parasaurolophus_eggshell`): 3 Stück beim
   Schlüpfen der Art (`DinoKind#eggshell`), 1 → 3 Knochenmehl. Texturen aus den Ei-Paletten (`eggs.py`).
5. Fundort-Gewichtung (§7): Badlands Raptor ×2, Stego ×1,5; Wüste Tri ×1,5, T. rex ×1,5; Sumpf/Mangrove Para ×2 – nur für noch nicht
   gewürfelte Fossilien (`FossilBlockEntity.kind()` nimmt das Biom der Position).
6. Config-Schlüssel (Tri): `triceratopsShake`, `shakeShare`, `shakeAppleChance`, `shakeTreeCooldownSeconds`, `ramCooldownSeconds`.
7. Rüstungs-Tag je Art: `can_wear_dino_armor` bleibt T.-rex-Rüstung (keine Umbenennung kurz vor Release), neu
   `can_wear_triceratops_armor`; Großer Sattel: Tag um `triceratops` erweitert.

### 2b. Triceratops (Konzept §4.1)
- Entity `simpledinos:triceratops` (`Triceratops extends Dino`), 100 LP, Tempo 0,20, kein Sprung, Stufe 1,0, Rückstoßfest 0,8,
  Fallschaden 0,6; Kern-Hitbox am Modell bestimmt (Ziel: Rumpf + Beine, Kopf/Kragen/Schwanz als Teil-Hitboxen).
- **Anlauf (Sprungtaste):** halten lädt (Vanilla-Balken), loslassen = Stoß ≈ 4,5–7 Blöcke je Ladung (Tempo × (1 + 1,5 × Ladung),
  Vorwärts erzwungen, 12 Ticks), Abklingzeit `ramCooldownSeconds` (3 s). Bewegung auf der Reiter-Seite (wie Kamel), Wirkung (Baum,
  Rückstoß, Klang) nur auf dem Server, Server prüft die eigene Abklingzeit.
- **Baumschütteln:** trifft der gestreckte Kopf (Teil-Hitbox) während des Anlaufs einen Stamm (`#minecraft:logs`): Suche ab Treffer über
  Stämme und Blätter (≤ 7 waagrecht, ≤ 64 Stämme, ≤ 96 Blätter); Baum = unterster Stamm der Treffersäule; nur Blätter mit
  `persistent=false` zählen. Ertrag: Vanilla-Blatt-Loot (`Block.getDrops`, leere Hand) für `shakeShare` der Blätter (≤ 40 Würfe),
  Fallobst `simpledinos:shake/windfall` mit `shakeAppleChance` bei Blättern aus `#simpledinos:windfall_leaves` (Eiche, Schwarzeiche),
  reifer Kakao am Stamm → Bohnen, Schote auf Alter 0. Je Baum `shakeTreeCooldownSeconds` (600) Ruhe, 32 Bäume gemerkt (gespeichert);
  ruhender/ künstlicher Baum = dumpfes Klopfen ohne Ertrag. Nichts wird zerstört.
- **Mobs im Anlauf:** kräftiger Rückstoß, kein Schaden, Spieler nie, eigene Tiere des Reiters nie.
- **Abwehr:** greift ein Monster den erwachsenen Tri an, stößt er es weg (Rückstoß 2, kein Schaden). Keine Angriffsziele.
- Futter: Apfel (Liebling), Weizen, Heuballen, Karotte (`#simpledinos:triceratops_food`).
- Teil-Hitboxen: `TriceratopsBones` (generiert), `Triceratops.RIG` (Hals, Kopf, Kragen, Hörner; Schwanz 3 Glieder).
- Modell (gen_models.py): Vierbeiner – Rumpf + Hüftbuckel, kurzer Hals, Schädel, Schnauze, Hornschnabel, Nasenhorn, 2 Stirnhörner
  (2 Glieder), Kragen in 2 Ebenen, 4 Beine (2 Glieder), Schwanz 3 Glieder, Sattel, Rüstung (Kragenschild, 3 Hornkappen, Brustplatte).
  Animation: Kreuzgang, Kopf folgt Blick, Kopf senken beim Laden/Anlauf, Schwanzpendel.
- Texturen: 5 Felle × 4 Stufen (generiert); Artmuster Kragen: **Varianten A „Augenflecken“ (eingebaut), B „Randbänder“,
  C „Verlauf“** (UI erst exemplarisch), Hörner/Schnabel hornfarben. Items: Tri-Rüstung ×3 (Kragenschild mit Hornkappen),
  Eierschalen ×4 – A8c-Runden, `texrate` grün.
- Klänge (Vanilla bis Hörabnahme R14): Hoglin tief (Ruhe/verletzt/Tod), Anlauf = Hoglin-Schnauben, Treffer = Ziegen-Rammstoß,
  ruhender Baum = Holz-Klopfen tief, Blätter-Rascheln.
- Lang EN/DE, Wiki (`manual.json` Feature „triceratops“, Notizen je neuer ID, Config-Keys), Handbuch neu generiert, Discover,
  Kopf-Ausnahme, Preise (ausgenommen), Testzentrale, Kreativtab.

## 3. Dateien

`modules/simpledinos/`: `shared/java/com/simpledinos/{DinoKind,DinosRegistry,SimpleDinos}.java`, `entity/{Dino,Trex,Triceratops}.java`,
`entity/part/TriceratopsBones.java` (gen.), `block/{FossilBlockEntity,DinoEggBlockEntity}.java`, `config/DinosConfig.java`,
`client/{DinosClient,TriceratopsLayers(gen.),TriceratopsModel,TriceratopsRenderer}.java`, `test/DinosTests.java`,
`fabric/.../{DinosFabric,DinosFabricClient,ModuleGameTest}.java`, `neoforge/...` (Registrierung analog Trex), `tools/{gen_models,gen_blocks,check_data}.py`,
neue Item-Zeichnung `tools/gen_items.py`, Ressourcen (Texturen, Modelle, Rezepte, Advancements, Tags, Loot), `wiki/manual.json`, Lang;
außerhalb: `simplemobheads/data/mobs.json` (+ generiert), `simplemoney/.../simpledinos/prices.json`, generierte Guide-/Discover-Dateien.

## 4. Risiken

- Bewegung beim Reiten ist client-seitig: Anlauf-Tempo rechnet der Client, Wirkung der Server (Treffer mit ≤ 6 Ticks Nachlauf, eigene
  Abklingzeit). Kein Mixin.
- Fossil-Anteile ändern sich: mit dem Tri wird statt nur T. rex jetzt T. rex 10 : Tri 25 gewürfelt (Konzept R10, gewollt).
- Gametests nur als sbt-Befehl (sb-test nicht erreichbar); Client-Sicht (Modell, Animation) nur per Software-Render in der Vorschau.

## 5. Prüfung

Lokal: `precheck.py --fast`, alle `modules/*/tools/check_data.py`, `check_dup_impl.py`, `check_tooltip_text.py`, `wiki/generate.py --all --check`,
`check_missing_textures.py`, `layers.py check`, `texrate` je neuer Textur, `gen_models.py --check`, `gen_blocks.py --check`, javac gegen 26.3.
Gametests (neu): Tri registriert/Werte/Hitbox, Teil-Hitboxen (Kopf an Wand eingezogen, Blätter frei), Zähmung (Schlüpfen → zahm,
Eierschalen), Futter/Heilen, Sattel/Rüstung/Reiten (T.-rex-Rüstung passt nicht), Anlauf-Abklingzeit (Server), Baumschütteln (natürlicher
Baum gibt Ertrag, Blätter bleiben, gesetzte Blätter nichts, Ruhezeit), Kakao, Rückstoß ohne Schaden, Spieler unberührt, Abwehr,
Fundort-Gewichtung, Kampf-Ziele nur T. rex. sbt-Befehl `/root/sb-tools-a/a-dinorest1-sbt.cmd`.

## 6. Runden (A8c)

| Runde | Was | Bewertung |
|---|---|---|
| Modell R1 | Rumpf-Box, flacher Kragen, Augenflecken, langer Schwanz | erkennbar, aber Kragen wie eine Tafel, Augenflecken wirken wie Cartoon-Augen, Rumpf ein Rohr, Schwanz wie beim Krokodil |
| Modell R2 | Tonnenrumpf + Rückenbogen (2 Kästen), Kragen als gestufter Bogen (3 Platten + 2 Randplatten mit Höckern), Hörner steiler, Schwanz kürzer, Flecken weich ocker | liest sich als Triceratops; Kopf zu klein |
| Modell R3 (eingebaut) | Schädel/Schnauze größer, Vorderbeine dicker | Felle hart 4,6–6,1 % (A8b ~7 %), 22 Töne |
| Items R1 | Rüstung (Kragenschild + Hornkappen), Schale (halbe Schale + Splitter) | Rüstung ohne Kragen-Merkmal; Schalen-Muster = zwei Punkte → Gesicht |
| Items R2 | Höckerrand, Mittelgrat, Naht/Nieten/Facetten je Stufe | Rüstung gut; Schalen-Muster noch symmetrisch |
| Items R3 (eingebaut) | Muster unregelmäßig in 2–3-px-Gruppen, Innenhaut zur Schalenfarbe getönt | texrate alle grün (big 5–11, ge6 0–34 %) |

Kragen-Muster A/B/C in der Vorschau (UI erst exemplarisch), A eingebaut (`gen_models.FRILL_CHOSEN`).

## 7. Ergebnis und Abweichungen

- Umgesetzt wie §2. **Abweichungen:** (1) **Fester Kopf** (`PartRig.solidHead`, `Dino#move`/`clipHead`, `Dino#pushEntities`) zusätzlich zur
  DINO36b-Lösung: Kragen und Hörner des Triceratops sitzen vor den Schultern, Einziehen verkürzt die Reichweite kaum (2,85 → 2,82); der Kopf
  hält deshalb den Lauf an Wänden an (Blätter und Schritthöhe ausgenommen) und schiebt Lebewesen beiseite; „eingezogen“ heißt beim Triceratops
  „Kopf heben“. Der T. rex bleibt unverändert (zieht ein). (2) Kern-Hitbox 2,6 × 2,6 statt 2,6 × 3,2 (Modell in 75 % wie der T. rex; der Kragen
  ist Kopf-Teilbox). (3) Rüstungs-Tag je Art: alter Tag `can_wear_dino_armor` bleibt der T.-rex-Tag. (4) Eierschalen aller vier Arten jetzt
  (Konzept §9 „Gemeinsames“), Rüstungen der anderen drei kommen mit ihrem Dino. (5) Fundort-Gewichtung wirkt erst sichtbar, wenn mehr Arten
  schlüpfen (T. rex und Triceratops haben in der Wüste denselben Faktor).
- Geprüft: javac gegen 26.3 (nur die bekannten Access-Widener-/Client-Gametest-Meldungen roher javac in unveränderten Stellen),
  `precheck.py --fast` grün, alle `modules/*/tools/check_data.py`, `check_dup_impl`, `check_tooltip_text`, `check_missing_textures`, `layers.py check`,
  `gen_models/gen_blocks --check`, `discover --check`, `module_guides --check`, `wiki/generate.py --all --check`, questionary-Check, texrate Items grün;
  Skelett-Rechnung (Python-Spiegel von `Skeleton`) für Reichweite/Hitbox/Schwanzbiegung.
- **Nicht geprüft:** Gametests (sb-test nicht erreichbar, Befehl `/root/sb-tools-a/a-dinorest1-sbt.cmd`), Client-Sicht (Modell, Animation,
  Anlauf-Gefühl beim Reiten, Sprungbalken), NeoForge/Forge (nur Registrierung analog T. rex ergänzt, nicht kompiliert).
