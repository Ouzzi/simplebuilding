# Plan Soulbound + Death Vault (2026-10-12, Branch `claude-soulbound`)

Grundlage: `KONZEPT-SOULBOUND-2026-10-12.md` und `KONZEPT-DEATH-VAULT-2026-10-12.md`, jeweils Abschnitt
„Besitzer-Antworten 12.10.“ (verbindlich, Vorrang vor den Vorschlägen davor). Modul **Simple Tools** (allein spielbar,
Fabric/NeoForge/Forge 26.3, keine neue harte Abhängigkeit).

## Ist-Zustand
- `modules/simpletools`: nur Config, Thrift-Verzauberung (Daten), Mixins (Amboss, Haltbarkeit), Ofen-Lava; **keine**
  Blöcke/Items/Menüs. `check_data.py` verlangt „no items“ → wird auf den einen Block erweitert.
- Vorlage für Block + Menü + Screen auf drei Loadern: `modules/simplequestionary` (Registry-Klasse, RegisterEvent,
  MenuScreens). Loot-Injektion: `modules/simplemaps` (`MapsLoot`, Forge über `ReloadableServerRegistriesMixin`).
- Vanilla 26.3: `Player#dropEquipment` → `destroyVanishingCursedItems` → `Inventory#dropAll` (Items + Ausrüstung über
  `LivingEntity#createItemStackToDrop`); `ServerPlayer#restoreFrom(old, restoreAll)`; `ItemEntity` verschwindet über
  `Entity#setRemoved` (Despawn im eigenen `tick`, Zerstörung in `hurtServer`, Void in `onBelowWorld`, Aufheben/Trichter/Mobs
  sonst).

## Entscheidungen (offen gelassene Punkte, selbst festgelegt)
- **Soulbound** (`simpletools:soulbound`): Schatz-Verzauberung, Stufe 1, `supported_items` = `#minecraft:enchantable/vanishing`
  (alles, was den Fluch tragen kann), `exclusive_set` = Fluch des Verschwindens. Nicht im Tisch, nicht beim Bibliothekar,
  nicht in `on_random_loot` → nur als Buch. Gewicht 1 (Mending 2).
- Bezug: fahrender Händler mit Chance `soulboundTraderChance` = 0,05 je Händler (Zusatzangebot, 24–32 Diamanten,
  einmal handelbar); End-City-Schatztruhe 1 : 49 (2 %, `soulboundEndCityChance`). Kein Geld-Weg (Owner: Diamanten).
- Tod: Soulbound-Stacks werden in `Inventory#dropAll` (nur während `Player#dropEquipment`) vor dem Drop herausgenommen und
  danach **in denselben Slot des toten Spielers** zurückgelegt. Der tote Spieler ist damit der einzige Speicher (wird beim
  Ausloggen im Todesbildschirm mit gespeichert). `restoreFrom(old, false)` verschiebt sie Slot für Slot in den neuen
  Spieler (alter Slot wird geleert) und setzt die Haltbarkeit auf min(Rest, 50). Bei `keepInventory`/End-Rückkehr nichts.
  Wirkt bei jedem Tod (PvP, Hardcore). Kein Item-Entity entsteht → nichts aufhebbar, kein Dupe.
- Amboss: Liegt rechts ein Gegenstand/Buch mit Soulbound und das Ergebnis trägt es, kostet das Kombinieren immer
  `soulboundAnvilCost` = 35 Level. Schleifstein: Vanilla entfernt es (kein Fluch).
- **Death Vault** (`simpletools:death_vault`): Block (Obsidian, Seelenerde/-sand, Ender-Auge, Truhe), kein Block-Entity.
  Buchführung serverseitig als `SavedData` der Oberwelt (`simpletools_death_ledger`, Vanilla schreibt atomar):
  je Spieler alle Tode (max. `deathVaultMaxDeaths` = 20, älteste fallen), je Tod Zeit, Killer-UUID (PvP) und die beim Tod
  gedroppten Stacks mit Entity-UUID (Erfassung in `createItemStackToDrop` nur während des Tod-Drops).
- Zustände je Eintrag: **liegt** (nicht einlösbar) → beim Entfernen der Entity:
  unverändert (gleiches Item, gleiche Anzahl) **und** Despawn (eigener Tick, Alter ≥ 6000) oder Schaden mit
  `#is_fire`/`#is_explosion` oder Void (y < min − 64) → **einlösbar**; alles andere (Aufheben durch irgendwen, Trichter, Mob,
  Teilaufnahme, /kill, Kaktus …) → **gestrichen**. Fail-closed. PvP-Tod: nur Despawn oder Zerstörung mit
  `source.getEntity()` = Killer. Verfolgte Entities verschmelzen nicht (sonst Teilmengen-Dupe).
- GUI (Vanilla-Truhenstil `generic_54`, 4 Reihen): 27 Anzeigefelder (nicht nehmbar), einlösbare zuerst (neueste Tode
  zuerst), liegende abgedunkelt; Opfer-Slot + Knopf „Opfern“. Opfer-Items über Tag `simpletools:death_vault_offerings`
  (Diamant; `simplemoney:money_bill` mit `required:false`) – je Item 1 Punkt. Je `deathVaultCostPerItem` = 4 Punkte eine
  Rückgabe, höchstens `deathVaultMaxPerOffering` = 3 je Opfer. Auswahl gewichtet: Wert (Seltenheit, Verzauberungsstufen,
  Haltbarkeit) × 1/(1 + Rang des Todes, 0 = neuester). Rückgabe = Kopie des Tod-Schnappschusses mit Haltbarkeit
  min(Rest, 50) (`deathDurabilityCap`), Eintrag sofort gestrichen. Ohne Einlösbares: nichts verbraucht.
- Config (`config/simpletools.json`, server, Obergrenzen): `enableSoulbound`, `enableDeathVault`, `deathDurabilityCap`
  (1–2031), `soulboundAnvilCost` (1–39), `soulboundTraderChance` (0–1), `soulboundEndCityChance` (0–1),
  `deathVaultCostPerItem` (1–64), `deathVaultMaxPerOffering` (1–27), `deathVaultMaxDeaths` (1–100).
- Keine neue Abhängigkeit auf simplelib (Container-Stil = Vanilla-Truhe); Money nur über optionalen Tag-Eintrag;
  Money-Preisliste `simpletools/prices.json` nennt den Block als „nur gecraftet“.

## Dateien
- shared/java: `soulbound/Soulbound.java`, `vault/DeathLedger.java`, `vault/DeathVault*.java` (Block, Menü, Screen),
  `ToolsRegistry.java`, `ToolsLoot.java`, Mixins (`InventoryDropMixin`, `PlayerDropMixin`, `ServerPlayerRestoreMixin`,
  `LivingDropMixin`, `ItemEntityLedgerMixin`, `EntityRemovedMixin`, `AnvilSoulboundMixin`, `WanderingTraderMixin`),
  `ToolsConfig`/`ToolsScreen`/`ToolsTests` erweitert; Loader-Einstiege (Registrierung, Tabs, Screen, Loot).
- Daten: `enchantment/soulbound.json`, Tags (`treasure`, `exclusive_set`), Block-Assets/-Loot/Rezept/Advancement,
  Tag `death_vault_offerings`, Texturen (`tools/gen_textures.py`, Vorschau `/root/previews/soulbound/`).
- Doku: Lang EN/DE, `wiki/manual.json` (+ `wiki/generate.py --all`), Guide (`tools/guides/module_guides.py`),
  Fragentisch `assets/simpletools/questionary/*.json`, Testzentrale-Station `deathvault` (SB-Kern, nur über Registry-ID),
  `docs/MODS-UND-FEATURES.md`, `.claude/QUEUE.md`.

## Tests (Gametests in `ToolsTests`, Fabric-Wrapper)
Slot-Erhalt + kein Drop (echter Tod), Haltbarkeit min(.,50), keepInventory/End-Rückkehr unverändert, Amboss 35,
Schleifstein entfernt, Exklusivität Fluch; Vault: Tod erfasst Entity-UUIDs, liegendes Item nicht einlösbar,
Despawn/Lava/Explosion/Void einlösbar, Aufheben/Teilaufheben gestrichen, PvP-Regel, Gewichtung (neu > alt, wertvoll >
billig), Dupe-Versuche (doppelt einlösen, Anzeigefelder nehmen, Verschmelzen, Opfer ohne Einlösbares), Persistenz
(Codec-Rundlauf). Lauf: Compile 3 Loader, `simpletools:*` auf fabric/neoforge/forge + Standalone, Checks.
