# Konzept: Simple Mobheads – alle Mob-Köpfe in einem Sub-Mod (2026-10-13)

Branch `claude-concepts50` (Basis `claude-wave1`). Nur Konzept, kein Code, nichts getestet. Queue-Punkt: „Mob-Köpfe in neues Sub-Mod ‚Simple Mobheads‘, alle Mob-Köpfe
implementieren; SimpleBuilding requires es; aus Simple Fun entfernen“. Grundlagen: `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, `PLAN-MODULAUFTEILUNG-2026-10-11.md` (Sub-Mods, `IdMoves`,
SB als Super-Mod), `KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md`. Mojang-Namen/-Klassen wie `SkullBlock.Type`, `ModelLayers` stammen aus dem Gedächtnis → vor dem Bau gegen 26.3 prüfen (markiert „[prüfen]“).

## 1. Ist-Zustand (wichtig: die Köpfe liegen heute an **zwei** Stellen)

| Ort | Köpfe | Namespace | Code |
|---|---|---|---|
| **Simple Fun** (`modules/simplefun`) | 4 Tierköpfe: Schwein, Kuh, Huhn, Schaf | `simplefun:pig_head`, `…_wall_head`, usw. | `heads/AnimalHead` (Enum als `SkullBlock.Type`), `AnimalHeads` (Block/Wandblock/Item/Tab), `HeadAbilities` (Kuh melken, Schaf scheren, Huhn Ei, Schwein Trüffelnase), `FunLoot` (Pool in `charged_creeper`), `client/AnimalSkullModels`, `mixin/AnimalSkullValidityMixin`, Konfig `enableAnimalHeads` + 8 weitere Schalter, `event/PlayerHeadDrop` (PvP-Kopf des Opfers), Tag `minecraft:skulls`/`mineable/axe`, Advancements `simplefun:content/*_head`, Wiki-Kapitel, Money-Preise (`MoneyDiscover`: 4 Köpfe) |
| **SimpleBuilding-Kern** (`tweaks/heads`, `tweaks/block/BlazeHeadType`, `tweaks/item/TweaksItems`) | 12 Köpfe: Lohe, Enderman, Wüstenzombie, Spinne, Höhlenspinne, Eiswanderer, Sumpfskelett, Schleim, Silberfischchen, Breeze, Shulker, Ertrunkener | `simplebuilding:blaze_head`, `…_skull` (Stray/Bogged) usw. | `BlazeHeadType` (Enum), `ModHeads` (Mob, Spawn-Ei, Notenblock-Sound, Tag `trial_chamber_heads`), `HeadAbilities` (12 geheime Fähigkeiten, Mixins `tweaks.mixin.HeadAbility*`), `ModLootTableModifications.headPool` (Pools in `charged_creeper`), `tweaks/client/ModSkullModels` + `SkullModelMixin`, `compat/MobDropCatalog` (JEI „Mob drops“), Tests `MobHeadTests`, Rezepte Chunk-Loader I / Launchpad I / Flypad I / Trank-Pad / Spawn-Teleporter nutzen Köpfe |
| **Vanilla** | Creeper, Zombie, Skelett, Wither-Skelett, Piglin, Enderdrache, Spielerkopf | `minecraft:` | bleibt Vanilla, nicht anfassen |
| **Kopf-Slot (SB)** | `util/ExtraHead`: nimmt `#minecraft:skulls` + `#simplebuilding:extra_head_slot` + geschnitzten Kürbis; Kopf wirkt wie getragen (Endermen-Ruhe, Sichtweite, Fähigkeiten) | – | `PlayerExtraHeadMixin`, `BackpackMenu` |

Das Fun-Modul beschreibt sich bisher als „Bounded playful mechanics, throwable bricks, cosmetic transformations and farm-animal heads“ (`modules.json`).
**Der Auftrag nennt nur den Umzug aus Simple Fun; die 12 SB-Köpfe müssen mit** (Vorschlag in Frage 1), sonst gäbe es zwei Köpfe-Systeme.

Gemeinsame Technik beider Systeme (wird übernommen): eigener `SkullBlock.Type` je Kopf (trägt sich in `SkullBlock.Type.TYPES` ein), `SkullBlock` + `WallSkullBlock` + `StandingAndWallBlockItem`,
Instrument `CUSTOM_HEAD` mit `note_block_sound`-Komponente, `equippableUnswappable(HEAD)`, Textur = die echte Vanilla-Mob-Textur **per Verweis** (keine kopierten Pixel; Ressourcenpakete
färben den Kopf mit um), Geometrie = Kopf des Vanilla-Entity-Modells auf die Grundlinie geschoben, Drop **nur** durch geladenen Creeper (Besitzer 2026-09-29: Pool in `minecraft:charged_creeper`).

## 2. Zielbild

**Sub-Mod `simplemobheads`** (Namespace `simplemobheads:`, Anzeige „Simple Mobheads“), Pflicht-Sub-Mod von SimpleBuilding (Modulplan §2.1 „required, gebündelt“, wie Octets/Hammers/…).

- Alle Köpfe (Tier-, Monster-, Variantenköpfe) in einem Modul, eine gemeinsame Technik, **eine** Quelle der Wahrheit (`modules/simplemobheads/data/mobs.json` + Generator, §3.6).
- Hart nur: Vanilla, Cloth Config, `framework` und `simplelib` (beide gebündelt; `IdMoves`, UI-Bausteine, Konfig-Migration). Regel 1: allein voll spielbar — jeder Kopf hat den Vanilla-Bezug (geladener Creeper
  erledigt den Mob), Fähigkeiten brauchen nur Vanilla-Mechaniken.
- **Simple Fun ohne Köpfe:** Fun behält Ziegelschneeball, Piggy-Effekt/Schweinekopf-Kosmetik (das ist ein Effekt, kein Kopf-Item), Fun-Guide, Trades, Konfig-Rest. Fun hängt nicht an Mobheads.
- **SB requires Mobheads:** `modules.json` (`requires`), `fabric.mod.json` `depends`, `neoforge.mods.toml`/`mods.toml` `type="required"`/`mandatory=true` und Wiki-Hinweis (Regel 2). Kern-Rezepte, die Köpfe brauchen
  (Chunk-Loader I, Launchpad I, Flypad I, Trank-Pad I, Spawn-Teleporter I), verweisen auf **Tags** von Mobheads, nicht auf Ids.
- **Kopf-Slot bleibt SB:** Mobheads wirkt auch ohne SB im normalen Helm-Slot. Mit SB gilt zusätzlich der Extra-Kopf-Slot. Dafür ein reiner Java-Vertrag in `framework.api` (z. B. `WornHeads`):
  SB meldet beim Start „liefere mir die Zusatzköpfe dieser Entität“, `HeadAbilities.wears` fragt Helm + Vertrag. Kein Klassenimport über die Grenze (Regel 3). Das Tag `minecraft:skulls` nimmt jeden Mobheads-Kopf
  auf (heute schon bei Fun/SB), der Kopf-Slot-Filter braucht keine Änderung.

## 3. Umfang

### 3.1 Welche Mobs

Quelle der Prüfung ist die Registry, nicht diese Liste: ein Gametest läuft über **alle lebenden `EntityType`s** (`MobCategory` ≠ MISC, nicht-technisch) und verlangt entweder einen Kopf oder einen Eintrag in der
Ausschlussliste mit Grund. So fällt jeder neue Mojang-Mob (und jeder Mob der Simple-Mods) beim nächsten Port-Run sofort auf. Tabelle der ersten Fassung (Gruppen, „V“ = Varianten, Zahl = Anzahl Aussehen):

| Gruppe | Köpfe (neu in Mobheads, ohne die 23 vorhandenen) |
|---|---|
| Monster Oberwelt | Zombiedorfbewohner, Hexe, Plünderer, Vindicator, Magier (Evoker), Verwüster, Phantom, Wächter, Großer Wächter, Endermite, Vex, Warden, Creaking, Parched, Kamel-Husk, Zombie-Nautilus, Illusionist (nur `/summon`, niedrige Prio) |
| Nether | Ghast, Magmawürfel, Hoglin, Zoglin, Piglin-Barbar, Zombifizierter Piglin, Strider, Wither (Boss) |
| Passiv/Zahm | Allay, Gürteltier, Axolotl (V 5), Fledermaus, Biene, Kamel, Katze (V 11), Kupfergolem (V 4 Oxidation), Delfin, Esel, Fuchs (V 2), Frosch (V 3), Leuchttintenfisch, Ziege (V normal/Schrei), Pferd (V 7 Fell), Eisengolem, Lama (V 4), Händlerlama, Pilzkuh (V 2), Maultier, Nautilus, Ozelot, Panda (V Gene), Papagei (V 5), Eisbär, Schneegolem, Kaninchen (V 7), Sniffer, Tintenfisch, Schildkröte, Dorfbewohner (V Biom 7), Fahrender Händler, Wolf (V 9), Skelettpferd, Zombiepferd, Glücklicher Ghast |
| Wasser | Kabeljau, Lachs, Kugelfisch, Tropenfisch (V Form × Muster), Kaulquappe |
| Tier-Varianten von Fun | Schwein/Kuh/Huhn (V warm/kalt), Schaf (V 16 Wollfarben), Shulker (V 17 Farben) |
| Simple-Mods | Mobs aus `simplemobs` (Deceiver u. a.), Furcht-Mobs, Sculk-Mob, End-Drachenkampf-Zusätze: eigene Köpfe **sobald der Mob existiert** (§3.7) |
| Ausschluss | Giant (unspielbar), Armorstand, Marker, Item-/Projektil-Entitäten, Spieler (Vanilla-Spielerkopf bleibt), Enderdrache/Wither-Skelett/Creeper/Zombie/Skelett/Piglin (Vanilla-Köpfe) |

Zählung: ≈ 75 neue Mobs + 16 vorhandene Mod-Köpfe ≈ 90 Kopf-Blöcke (je Stand- und Wandblock) plus Varianten.

### 3.2 Varianten – zwei Wege

- **A: je Variante ein eigener Block/Item** (wie heute je Mob). Einfach, aber ≈ 200 Köpfe (≈ 400 Blöcke): riesiger Creative-Tab, doppelte Lang-/Modell-/Loot-Dateien.
- **B (Empfehlung): ein Kopf je Mob, Variante als Daten.** Item-Komponente `simplemobheads:variant` (Id oder Farbe) und eine eigene Block-Entity `simplemobheads:mob_head`, die sie speichert und an den Renderer gibt
  (Vorbild Spielerkopf: Profil in der Block-Entity). Der Drop kopiert die Variante des getöteten Mobs (Katze „Siamesisch“ lässt ihren Kopf fallen), der Creative-Tab zeigt alle Varianten, JEI zeigt sie als Gruppe.
  Risiko: `SkullBlock` erzeugt eine Vanilla-`SkullBlockEntity`; eigene Block-Entity heißt eigener Block-Entity-Typ + eigene Block-Klasse (`MobHeadBlock extends AbstractSkullBlock` [prüfen]) — keine große Sache, aber bei Heads mit
  Vanilla-Verhalten (Piston, Waterlogging) prüfen. Rückfall: A nur für die wenigen Mobs, bei denen B scheitert (z. B. Schaf mit Wollschicht).
- **Zusammengesetzte Aussehen** (Schaf-Wolle, Dorfbewohner Biom + Beruf, Pilzkuh, Tropenfisch Muster, Kupfergolem Oxidation) werden zur Laufzeit in **mehreren Renderdurchgängen** übereinander gezeichnet (wie die Vanilla-Layer),
  nicht in Texturen gebacken.

### 3.3 Drop-Regeln

Grundregel bleibt die Besitzerlinie 2026-09-29: **nur die Explosion eines geladenen Creepers lässt den Kopf fallen**, wie Vanilla bei Creeper/Zombie/Skelett/Piglin/Wither-Skelett (Pool in `minecraft:charged_creeper`, je Mob ein
Pool mit Bedingung „Opfer = Mob“, höchstens ein Kopf je Explosion, Vanilla-Zähler). Daraus folgt:

| Regel | Vorschlag |
|---|---|
| Quelle | `minecraft:charged_creeper` (Vanilla-Mechanik, 100 % bei Treffer; mehrere Mobs in einer Explosion: Vanilla-Grenze „ein Kopf“). Heute für 16 Köpfe so; wird auf alle Köpfe verallgemeinert (ein generischer Pool je Mob aus `mobs.json`) |
| Looting | **wirkt nicht** auf die Creeper-Quelle (Vanilla-Verhalten, kein Dropchance-Wurf). Optional (Standard **aus**): `playerKillChance` — Spielertötung mit einem Kopf-Pool, Basis 0 %, +1 % je Looting-Stufe; nur für Serverbetreiber, die es wollen |
| Geladener Creeper | Wie Vanilla, ein Kopf je Explosion; der Zähler gilt gemeinsam mit den Vanilla-Köpfen, damit die Explosion nicht mehrere Köpfe gibt |
| Babys | Baby-Tiere lassen den **normalen** Kopf fallen (kein Baby-Kopf, Skalierung am Item, Frage 5) oder keinen. Empfehlung: normaler Kopf, die Chance ist ohnehin gering |
| Bosse | Wither: Kopf nur auf Konfig-Schalter (Standard aus); Warden: Standard an (Creeper-Sprengung ist schwer genug); Enderdrache: Vanilla-Kopf, nicht anfassen |
| Spieler | `playerHeadDrops` (PvP-Kopf des Opfers) zieht von Fun nach Mobheads um (Frage 3); Server-Befehl `/simplefun pvp headDrops` → `/simplemobheads pvp headDrops` (alter Befehl bleibt als Alias, Verweis im Chat) |
| Variante | Dropt die Variante des Opfers (§3.2 B) |
| Nicht-Drop-Wege | Handel (Money-Links bleiben), Creative, Truhen-Fund: kein Fund. Nur Creeper-Weg + Handel |

### 3.4 Platzierung, Wandkopf, Notenblock

- **Platzieren:** `StandingAndWallBlockItem` wie heute: Boden → 16-teilig drehbarer Standkopf, Wand → Wandkopf (zwei Blöcke je Kopf, Wandblock ohne Item, nutzt die Beschreibung/den Loot des Standkopfes).
  Rotation, Redstone-Signal (`powered` für die Mundanimation nur bei Mobs mit Kiefer: Phantom, Ghast, Strider — optional, nicht v1), Abbau mit Axt/Hand (`mineable/axe`, Stärke 1 wie Vanilla-Köpfe).
- **Notenblock:** Instrument `CUSTOM_HEAD`, Klang aus der Item-Komponente `note_block_sound` = das Standardgeräusch des Mobs. Quelle: Ambient-Klang, sonst Hurt-Klang, sonst Idle-/Step-Klang (Fische: Zappeln,
  Tintenfisch: Ambient, Schneegolem: Hurt …). Tabelle in `mobs.json` mit Rückfallkette; Varianten, die eigene Klänge haben (Schwein/Kuh/Huhn „klassisch“ vs. moderne Sets, Katze, Wolf), bekommen den Klang der Variante, sonst den
  der Grundform. Tonhöhe regelt der Notenblock (wie Vanilla-Kopf). Test: jeder Kopf hat einen Klang, der nicht `null` ist.
- **Tragen:** `equippableUnswappable(HEAD)`, Helm-Slot und Extra-Kopf-Slot. Rendering getragen: Kopf-Layer des Spielers nutzt dasselbe Modell wie der Block (der Skull-Modell-Mixin, der heute `ModSkullModels`/`AnimalSkullModels` bedient).

### 3.5 Kopf-Slot-Effekte (Fähigkeiten)

Grundsatz des Besitzers (2026-09-29/10-02): nichts, was das Spiel stark verändert, sich ausnutzen lässt oder Dinge überflüssig macht — Spaß oder ein kleiner Nutzen. Jede Fähigkeit hat einen Server-Schalter.

1. **Allgemein für jeden Kopf (neu, datengetrieben):** Wer den Kopf eines Mobs trägt, wird von **diesem** Mob nur halb so weit gesehen (Vanilla-Verhalten für Zombie/Skelett/Creeper …, in SB als „MobHeads 0,5“ im `LivingEntityMixin`).
   Umgesetzt als Datenwert `detection_factor` je Eintrag in `mobs.json`/Datenpaket `simplemobheads:head_traits`; ein einziger Mixin-Punkt statt 90.
2. **Vorhanden (16, ziehen um, unverändert):** Lohe, Enderman, Wüstenzombie, Spinne, Höhlenspinne, Eiswanderer, Sumpfskelett, Schleim, Silberfischchen, Breeze, Shulker, Ertrunkener, Kuh, Schaf, Huhn, Schwein (siehe `tweaks/heads/HeadAbilities`, `simplefun/heads/HeadAbilities`).
3. **Neue Vorschläge (klein, ungefährlich):**

| Kopf | Wirkung | Balance |
|---|---|---|
| Fledermaus | sieht Fledermäuse/Höhlenwände nicht; nur Kosmetik + Ambient-Piepen auf Wunsch | keine Spielwirkung |
| Biene | Bienenstöcke/Nester stechen nicht (Trägerschutz beim Honigernten) | kein Vorteil beim Kampf |
| Fuchs | Süßbeerensträucher stechen nicht | gering |
| Axolotl | Unter Wasser kein Zahnen; kein Effekt außerhalb | Konfig |
| Frosch | springt höher aus dem Wasser (Sprungkraft nur im Wasser) | gering |
| Kamel | Sprint aus Stehen: kurzer Anlauf-Boost, Abklingzeit | wie Kamel, nicht stapelbar |
| Gürteltier | Fußtritte erschrecken keine Gürteltiere/Zombies erschrecken sich nicht — (offen) | gering |
| Schildkröte | Eier zertrampeln nicht | gering |
| Ziege | Rammstoß gibt Mini-Rückstoß statt Schaden | Spaß |
| Eisbär | Pulverschnee/Eis rutscht weniger (Eisläufer-Effekt) | gering |
| Wolf | Wölfe sind nicht feindselig | wie Gewohnheit |
| Katze | Creeper meiden den Träger (wie Vanilla-Katze), Reichweite klein | wie Vanilla-Katze, dort nicht doppelt |
| Papagei | Kein Gefallen an Keksen (Papagei-Tod)/Nachahmer-Geräusch | Spaß |
| Pilzkuh | Pilzsuppe aus Schüssel (Rechtsklick wie die Kuh melken) | wie Kuh |
| Kaninchen | Fußschutz auf Beeren; leichter Sprungbonus | gering |
| Sniffer | Schnüffeln: Samen auf Moos/Erde finden (wie Trüffelnase) | Abklingzeit |
| Fahrender Händler/Dorfbewohner | Dorfbewohner reagieren freundlicher (leichtere Preise ausdrücklich **nicht**) | nur Gestik |
| Allay | Items in der Nähe schweben leicht (kosmetisch) | keine |
| Warden | Sculk-Sensoren hören weniger (nicht lautlos) | gering, Konfig |
| Wither | **keine** Wirkung (nur Kosmetik, Boss-Respekt) | – |
| Ghast | Feuerbälle gehen am Träger vorbei? **nein** | zu stark |
| Phantom | Schlafen nicht nötig? **nein** | schlägt Spielmechanik |
| Kupfergolem | Knopf-Klick (Sound), sonst Kosmetik | keine |

   Die übrigen Köpfe haben nur Tarnung (1.) + Notenblock + Optik. Erst nach Besitzerauswahl (Frage 6) umsetzen; „(offen)“/„nein“-Zeilen sind bewusst Rückfragen bzw. Absagen.
4. **Kopf-Slot:** gleicher Wirkungsweg; der Helm gewinnt bei zwei Köpfen (beide gelten, nicht stapelnd; ein Effekt je Mob). Test: Kopf im Extra-Slot ohne Helm wirkt, Kopf entfernt = keine Wirkung (Vorbild `MobHeadTests`).

### 3.6 Texturen, Modelle, Daten (Build-Pipeline)

- **Keine Mojang-Pixel im Repo** (Besitzerregel 2026-10-12). Köpfe verweisen auf die **Vanilla-Texturen zur Laufzeit** (wie heute: `ResourceLocation`, z. B. `textures/entity/pig/pig_temperate.png`), das braucht keinen Build-Schritt
  und folgt Ressourcenpaketen. Hier gibt es nur einen Haken: Texturen, die Vanilla in Varianten **tauscht** (z. B. Katzenfell, Wolfsvariante), sind als Namensliste in `mobs.json` hinterlegt.
- **Geometrie:** statt 90 handgeschriebener Würfel-Listen (heute `ModSkullModels` je Typ) wird der **Kopf-Teil des Vanilla-Entity-Modells zur Laufzeit gebaut**: `ModelLayers.<MOB>` → `LayerDefinition` → Teil `head`
  auf die Grundlinie (Unterkante y = 0) verschoben, UV/Texturgröße aus dem Modell [prüfen: Zugriff auf `LayerDefinition`/Teile ist in 26.3 anders organisiert; Rückfall: Würfel-Liste aus `mobs.json`]. Ausnahmen ohne Kopf-Teil
  (Schleim, Ghast, Tintenfisch, Fische, Wächter, Magmawürfel, Kaulquappe) tragen eine Tabelle „Teil + Verschiebung“.
- **Abgeleitete Pixel** (sind nur nötig für 2D-Bilder: Wiki-Renderings, Guide-Icons, Leer-Slot-Icon, ggf. kolorierte Variantenbilder): werden beim Build von `tools/textures/vanilla_derived.py` aus dem Client-Jar erzeugt
  (`generateVanillaDerived263`, nie eingecheckt). Eintrag `mobheads_icons` plus Platzhalter ohne Jar. Eigene 2D-Elemente (Slot-Silhouette „Kopf“, ggf. Herz-Marker) bleiben eigene Pixel-Art unter `tools/textures/`.
- **Generator:** `modules/simplemobheads/tools/generate_heads.py` liest `mobs.json` (Mob-Id, Gruppe, Namen EN/DE, Klang-Rückfallkette, Varianten, Detection-Faktor, Fähigkeiten-Id, Trial-Chamber-Flag, Tab-Reihenfolge) und erzeugt:
  Blockstates/Item-Modelle (Spezialmodell `minecraft:head` mit `kind`), Lang EN/DE, Loot-Tabellen des Blocks, `charged_creeper`-Pool, Tags (`minecraft:skulls`, `mineable/axe`, `trial_chamber_heads`), Money-Preise/-Links, Wiki-Daten,
  JEI-Kataloge. `--check`-Modus wie die anderen Generatoren. Neue Mobs = Eintrag in `mobs.json`, kein Java.
- **Registrierung bleibt Java** (Block-Registry friert beim Start ein): eine Schleife über die generierte Mob-Liste, **immer registriert**, auch wenn die Mod-Mobs fehlen (keine verschwindenden Blöcke).

### 3.7 Mod-Mobs („bedingt“)

- **Simple-Familie** (`simplemobs`: Deceiver u. a.; Furcht-Mobs; Sculk-Mob; End-Drachenkampf): Köpfe liegen in Mobheads, werden **immer** registriert, Drop und Effekt sind inhaltlich abhängig vom Mob (ohne Mob unbeschaffbar, aber im Creative und per Handel).
  Der Mob selbst bleibt im eigenen Modul; Mobheads kennt nur die Mob-**Id** (String), nie die Klasse. Regel 4: Rezepte/Loot/Tags nennen die Mob-Id nur mit `mod_loaded`-Condition bzw. `required:false`.
- **Fremd-Mods** (Alex' Mobs usw.): nicht im ersten Satz. Stattdessen eine **kleine API** `SimpleMobHeadsApi.register(mobId, ModelHint, soundId, textureId, …)` in `simplelib`/`framework`? Empfehlung: `com.simplemobheads.api`,
  nur als Datenobjekt, wie `SimpleLibApi`; Nachteil: der Block muss vor dem Registry-Freeze angelegt werden, also muss die Fremd-Mod ihn in ihrer eigenen Initialisierung eintragen (Frage 7).
- Datengetriebener Rückfall „generischer Kopf für jede Entität“ (Textur der Entität, Würfelkopf 8×8×8) ist denkbar, aber Registry-Blöcke lassen sich nicht nachträglich anlegen → nicht empfohlen.

## 4. Umzug aus Simple Fun (inkl. Migration)

Alte Ids → neue Ids über `com.simplelib.api.IdMoves` (Registry-Mixins beantworten **fehlgeschlagene** Lookups mit dem neuen Objekt; die nächste Speicherung schreibt die neue Id). Regeln des Modulplans: alte Registrierung
**im selben Commit** entfernen; Moves im Mod-Konstruktor vor dem Laden jeder Welt (Forge: vor den Registry-Events); keine Ketten.

| Was | Alt | Neu |
|---|---|---|
| Items (Standkopf) | `simplefun:pig_head`, `cow_head`, `chicken_head`, `sheep_head` | `simplemobheads:pig_head`, … |
| Blöcke (Stand + Wand) | `simplefun:pig_head`, `simplefun:pig_wall_head`, … | `simplemobheads:pig_head`, `…_wall_head` |
| Block-Entity | Vanilla `minecraft:skull` (kein Move nötig) bzw. neuer Typ ab §3.2 B | – |
| Rezepte/Rezeptbuch, Advancements | `simplefun:content/pig_head` … (Advancements), kein Rezept | `IdMoves` für `Registries.ADVANCEMENT` (Fortschritte wandern) |
| Kopf-Typ-Name (`SkullBlock.Type`) | `simplefun:pig` | `simplemobheads:pig`; Item-Modell-Feld `kind` neu; alte Modell-Dateien entfallen |
| Tags, Loot, Datenpakete | `simplefun:*` Loot der Blöcke, Eintrag in `minecraft:skulls` | werden neu erzeugt; Datenpaket-Autoren mit `simplefun:pig_head` in eigenen Tags: Move wirkt über Tags/Rezepte (Registry-Lookups) |
| Konfig | Fun: `enableAnimalHeads`, `pig/cow/chicken/sheepHeadGreeting`, `cowHeadMilking`, `sheepHeadShearing`, `chickenHeadEggs`, `pigHeadTruffles`, `playerHeadDrops` | neue Datei `simplemobheads.json`; **einmalige Übernahme** beim ersten Start aus der Fun-Konfig (`framework.api.ConfigFiles` lesen, Werte kopieren, Fun-Schlüssel entfernen). Kaputte Konfig → `*.broken-<Zeit>` wie überall |
| Befehl | `/simplefun pvp headDrops` | `/simplemobheads pvp headDrops`, alter Befehl bleibt als Verweis |
| Money | `simplefun:chicken_head` … in `MoneyDiscover`, Links `links:simplefun` | Links `links:simplemobheads`, Preise unter `money/simplemobheads/prices.json`, `generate_links.py` aus neuen Ids; alte Fun-Links entfallen |
| Wiki/Guide/Testzentrale | Fun-Kapitel „Animal heads“, `FunGuide` (registriert heute in `AnimalHeads.items()`!) | neues Kapitel in Mobheads; Fun-Guide wird von der Kopf-Registrierung gelöst (Hinweis: heute hängt `FunGuide.register()` im Kopf-Code) |

**SB-Köpfe (12):** `simplebuilding:blaze_head`, `…_skull` usw. → `simplemobheads:*` mit **demselben** Mechanismus; Tag `simplebuilding:trial_chamber_heads` wird zu `simplemobheads:trial_chamber_heads`, SB behält das alte Tag
als Alias (leitet auf das neue), damit Datenpakete weiterlaufen. SB-Rezepte (Chunk-Loader I, Launchpad I, Flypad I, Trank-Pad I, Spawn-Teleporter I) wechseln auf `simplemobheads:`-Ids/Tags **im selben Commit**.
Das passt in die geplante Entkopplung von Simple Tweaks (Modulplan §2.2, Tweaks nicht mehr hart an SB).

**Reihenfolge (jede Stufe gate-grün, Fabric zuerst, Standalone- und Migrationstests):**
1. Modulgerüst + 4 Fun-Köpfe + `IdMoves` + Konfig-/Befehls-Übernahme + Fun-Rückbau (Fun ohne Köpfe grün, Standalone).
2. SB-Köpfe (12) + Fähigkeiten + `charged_creeper`-Pools + `MobDropCatalog`-Anbindung + `framework`-Vertrag für den Extra-Kopf-Slot + SB-Rezepte; `SimpleBuilding requires Simple Mobheads`.
3. Generator `mobs.json` + generische Geometrie + Notenblock-Rückfallkette + Detection-Faktor; erste Charge neuer Köpfe (Monster).
4. Weitere Chargen (Nether, Passiv, Wasser), Varianten (§3.2 B), Mod-Mobs.
5. Fähigkeiten-Charge nach Besitzerauswahl.

## 5. Tests (Plan)

- **Migration:** Welt-Fixture mit `simplefun:*_head` als Block (Boden/Wand), Item in Truhe, Item im Spielerinventar, Advancement-Fortschritt; Laden → neue Ids, nach Speichern keine alten (Vorbild `IdMovesTests`); Konfig-Übernahme;
  gleiches für die 12 SB-Köpfe. Jeweils Fabric, NeoForge, Forge.
- **Vollständigkeit:** jeder lebende `EntityType` hat Kopf oder Ausschlussgrund; jeder Kopf hat Klang, Lang EN/DE, Loot-Tabelle, Modell, `charged_creeper`-Pool.
- **Drop:** geladener Creeper sprengt Mob → genau ein passender Kopf (inkl. Variante); normaler Kill → keiner; `playerKillChance` Standard 0.
- **Fähigkeiten:** wie `MobHeadTests` (mit Kopf wirkt, ohne nicht); Extra-Kopf-Slot.
- **Standalone:** `simplemobheads` + Vanilla grün (Regel 8); `simplefun` ohne Köpfe grün; SB mit Mobheads (Pflicht) grün; Jar-in-Jar-Bündelung auf allen Loadern.
- **Client:** Screenshot-Lauf der Köpfe (Stand, Wand, getragen), Katze/Wolf Varianten, Wiki-Rendering.
- JUnit-Layouttest nur falls GUI geändert wird (Kopf-Slot ändert sich nicht).

## 6. Risiken

- Unterschied der Loader: Forge-Registry-Aliase nur bei offener Registry; Kopf-Typen müssen **vor** dem Lesen von Chunks bekannt sein (`SkullBlock.Type.TYPES`).
- Variantenweg B braucht eigene Block-Entity und Renderer-Umweg (Skull-Renderer ist auf `SkullBlockEntity` festgelegt).
- Datenmenge: ≈ 90 Köpfe × Lang/Loot/Modell/Wiki; deshalb Generator, nie von Hand.
- 26.2/1.21.11: die Köpfe der Fun-Module sind nur 26.3 (Mod-Mobs/Modelle). Port erst im Port-Run.
- Texturrecht: ausschließlich Verweis auf Laufzeit-Texturen + Build-Ableitung; keine eingecheckten Mojang-Pixel.

## 7. Offene Besitzer-Fragen (mit Empfehlung)

1. **Auch die 12 SB-Köpfe (Lohe, Enderman, …) nach Mobheads?** Der Auftrag nennt nur Simple Fun. Empfehlung: **ja**, ein System; Ids `simplebuilding:*` → `simplemobheads:*` mit `IdMoves`, SB behält ein Alias-Tag.
2. **Varianten:** Weg B (ein Kopf je Mob + Variante als Daten)? Empfehlung: B, mit A als Rückfall für wenige Mobs.
3. **Spielerkopf-Drop (PvP, `playerHeadDrops`)** von Fun nach Mobheads? Empfehlung: ja (es ist ein Kopf-Drop), Konfig und Befehl mit Alias übernehmen.
4. **Drop-Quelle:** nur geladener Creeper (wie bisher, Vanilla-Linie)? Optional Spieler-Kill-Chance (Standard 0 %, +1 % je Looting, nur per Konfig)? Empfehlung: nur Creeper, die Spieler-Kill-Option als Server-Schalter (Standard aus).
5. **Babys und Bosse:** Babys lassen den normalen Kopf fallen; Wither-Kopf Standard aus, Warden an? Empfehlung: ja zu beiden.
6. **Fähigkeiten für die ca. 75 neuen Köpfe:** nur Tarnung/Notenblock/Optik (Empfehlung) plus die ca. 12 Vorschläge aus §3.5 (Biene, Fuchs, Frosch, Ziege, Katze, Pilzkuh, Sniffer, Allay …) nach Ihrer Auswahl — oder alle ohne Fähigkeit starten?
7. **Fremd-Mod-Mobs:** nur eine kleine API (Fremd-Mod registriert selbst) oder gar nichts im ersten Satz? Empfehlung: API ohne Fremd-Mod-Köpfe, erst Simple-Mods.
8. **Pflicht-Sub-Mod:** Mobheads wie Octets/Hammers/Storage/Ovens/Crucibles als `required` in SB. Folge: SB lädt nie ohne Mobheads; Mobheads läuft allein. Empfehlung: ja (so beauftragt).
9. **Simple Fun** behält Ziegelschneeball, Piggy-Effekt (Kosmetik), Guide; Beschreibung/Version → 1.4.0 „ohne Köpfe“. Empfehlung: ja; Fun nennt keine Köpfe mehr.
10. **Namens- und Anzeigenformat:** „Pig Head“ / „Schweinekopf“ (Vanilla-Stil) auch für Varianten („Siamese Cat Head“)? Empfehlung: ja, Variante als Namenszusatz im Item, Grundname im Creative-Tab.
11. **Tab:** eigener Kreativ-Tab „Mob Heads“ (Empfehlung) oder hinter den Drachenkopf im „Functional Blocks“-Tab wie heute (Fun)? Empfehlung: eigener Tab plus Verankerung hinter dem Drachenkopf (Suche).
12. **Trial-Chamber-Köpfe:** das Tag (für Chunk-Loader I/Launchpad I) bleibt auf den heutigen Mobs? Empfehlung: ja, nur die dort genannten Mobs; neue Köpfe erweitern das Tag nicht.
