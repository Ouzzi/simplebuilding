# Konzept Shop-Vitrine + Verkaufsblock (2026-10-14)

Nur Konzept (Queue Nachtrag 56, Job `oc-shop-concept`), kein Code. „(Vorschlag)“ = eigene Festlegung, der Besitzer kann sie ändern.
Die Besitzer-Wünsche aus Nachtrag 56 (Vitrine zeigt Ware, Preis-Slots nach Filter-Prinzip, ein Spieler gleichzeitig, explosionsfest,
nur Besitzer baut ab) sind übernommen. Offene Entscheidungen: Abschnitt 5.

## 0. Ist-Zustand (per grep erhoben)
- **Simple Money** (`modules/simplemoney`): nur Geldschein-Herstellung, Händler-/Dorfbewohner-Angebote, `prices.json`-Preislisten für andere Mods.
  Es gibt **keinen** Spieler-Shop, keinen Verkaufsautomaten, keinen Block mit Inventar. Geld = normales Item (`simplemoney:money_bill`).
  Vorbild für „Geld optional“: Death Vault (`simpletools`) nimmt Scheine über das optionale Tag `simpletools:death_vault_offerings`.
- **Claims** (`modules/simpletweaks/.../claims`, an seit 13.10.): Rechte-Bits `B=1 D=2 U=4 C=8 R=16 A=32 M=64` (`ClaimRights`), Ledger v2 (Phase 1).
  Spieler-Aktionen laufen über `Claims.allowBlock` (`ClaimGameModeMixin` auf `ServerPlayerGameMode.destroyBlock`/`useItemOn`).
  **Wichtig:** `Claims.useAction` ordnet jeden Block mit Menü (`getMenuProvider != null`) dem Recht **C** zu, Blöcke im Tag
  `simpletweaks:claim_use` dem Recht **U**; alles andere ist strikt (B+D). Umwelt (Explosion, Feuer, Fluss, Kolben, Trichter, Mobs,
  Enderman, Blitz, Drache …, ~45 Mixins) prüft „gleicher Landbesitzer“, nicht Vertraute. `ClaimAutomation.transfer` entscheidet
  für Trichter/Werfer/Kupfergolem: unbekannte Behälter = nie erlaubt (fail closed).
- **Protection-API:** `framework/.../Protection` (`Target(actor, dim, x,y,z, admin)`, `Action BUILD/DESTROY/USE/CONTAINER/REDSTONE/ENTITY/ANY`,
  `register(server, id, Check)`; **jeder** registrierte Provider muss zustimmen), SB-Seite `api/WorldPermissions` (`mayAct`, `mayAutomate`).
  Hammer/Vein/Strip/Laser/Baustab/Pads/Attractor/Kolben/Trichter von SimpleBuilding fragen darüber. **Jedes Modul darf einen eigenen
  Provider registrieren** (Simple Tweaks tut es als „simpletweaks“) – ohne Import eines anderen Moduls.
- **Filter-Prinzip** (`docs/ai/PRINZIPIEN-FILTER.md`, `util/ItemFilter`): echtes Item im Slot; Modi exakt (`isSameItemSameComponents`) / gleiche Art;
  Knopf `UiFilterButton` (simplelib). Der Shop-Preis ist dagegen eine **Vorlage** (kein echtes Item, wird nie verbraucht, siehe 2.2/4.2).
- **Vitrinen/Display-Blöcke:** keine Vitrine vorhanden. Vorbilder für Renderer mit Items: `TieredChestRenderer`, `PlacedSmallPartsRenderer`,
  `GoatHornHolderRenderer`, `AstralVaultRenderer` (26.3, `common/src/shared/.../client/render`), abgestellte Bündel (`PlacedBundleClientTest`).
- **Death Vault** (`KONZEPT-DEATH-VAULT-2026-10-12.md`): Invariante „höchstens ein Exemplar“, Fail-closed bei unklarem Zustand, Serverlogik in
  einer Funktion, UI nur Anzeige. Dieses Muster gilt hier für den Tausch.
- Explosionsfeste Mod-Blöcke gibt es schon (`strength(h, 1200F)` bei Netherit-Öfen; `ModBlocks`) und `pushReaction(PUSH_BLOCKED)` (`TweaksBlocks`).

## 1. Blöcke, Rezepte, Optik, Modul

### 1.1 Blöcke (alle Registry-ID-Vorschläge in `simpleshops:`)
| Block | Aufgabe |
|---|---|
| `display_case` (Vitrine) | Block-Entity mit **5 Waren-Fächern** (je ein Stapel). Zeigt die Ware. Muss auf einem Verkaufsblock stehen. |
| `glow_display_case` (Glow-Vitrine) | Wie die Vitrine, Ware voll hell gerendert (wie Glow-Item-Rahmen) + schwaches Licht (Frage 6). |
| `sales_block` (Verkaufsblock) | Block-Entity mit Besitzer-UUID, **Preis-Vorlagen (≤5)** und **Einnahmen-Fach** (9 Slots). Kasse des Shops. |

Ein Verkaufsblock bedient genau **eine** Vitrine direkt über ihm; ein Preis gilt für alle Waren dieser Vitrine (Frage 3).
Wer andere Preise will, baut ein zweites Paar. **Ware lagert in der Vitrine, Preis und Einnahmen im Verkaufsblock** (Vorschlag;
Auftrag sprach von „Lager im Verkaufsblock“ – gemeint ist hier das Einnahmen-Lager; Frage 2).

### 1.2 Rezepte (nur Vanilla-Material, Modul-Unabhängigkeit Regel 1)
```
Vitrine                    Verkaufsblock               Glow-Vitrine (formlos)
 Eisennugget x3            Bretter  Bretter  Bretter    Vitrine + Leuchttintenbeutel
 Glas       x3             Eisenbarren Truhe Eisenbarren
 Bretter    x3             Bretter  Bretter  Bretter
```
→ 1 Vitrine (Bretter = `#minecraft:planks`) · → 1 Verkaufsblock · Glow analog Glow-Item-Rahmen. Kosten gering (3 Nuggets, 3 Glas, 3 Bretter),
bewusst früh erreichbar. Mit Simple Money: eigener Eintrag in `money/simpleshops/prices.json` („nur craftbar“, Händler verkauft nichts).
Rezepte per Datagen (Fabric) für alle Loader, EN+DE-Lang, Tooltip nur Name (AGENTS 3.14).

### 1.3 Optik
- **Verkaufsblock:** Vollblock, dunkles Holz, Eisenkante, Münzschlitz-/Kassen-Motiv auf den Seiten, glatte Oberseite (Vitrine steht darauf).
- **Vitrine:** Block-Modell mit Holzsockel (2 px), Glaskasten 14 px mit feinem Eisenrahmen (Nugget-Ecken), `noOcclusion`, Glas-Textur
  in einer eigenen Schicht (translucent). Glow-Variante: Rahmen hell/gelblich, Sockel-Leuchtstreifen. Eigene Pixelkunst im Vanilla-Stil
  nach `tools/textures/` mit Vorschau (AGENTS 3.7), keine Vanilla-Pixel einchecken.
- **Ware zeigen (Vorschlag, `BlockEntityRenderer` wie `TieredChestRenderer`):** 1 Ware = mittig, 0,55-fach, langsam drehend, leichtes Schweben;
  2–5 Waren = kleine Items auf festen Plätzen (2: nebeneinander, 3: Dreieck, 4: Quadrat, 5: Quadrat + Mitte), drehen versetzt. Leere Fächer nichts.
  Block-Items als kleines 3D-Modell, Flach-Items als Karte. Glow: `FULL_BRIGHT`.
  Leere Vitrine zeigt nichts; **inaktiv** (kein Verkaufsblock darunter oder kein Preis): Items blasser/ohne Drehung (Zustand `active` im Blockstate).
- **Preis am Block (Stufe 3):** kleines Preisschild-Icon auf der Verkaufsblock-Vorderseite (erstes Preis-Item + Anzahl). Nur Anzeige.
- Client-Sync: nur **Waren-Stapel + Besitzer-UUID + Preis-Anzeigeteil** (`getUpdatePacket`); Einnahmen nie. Renderer cacht die Item-Render-Zustände
  bei Änderung, max. 5 Items, Sichtweite 24 Blöcke. Keine Texte auf dem Bildschirm (AGENTS 3.4): Rückmeldung per Klang/Partikel.

### 1.4 Modul (Vorschlag): neues Modul **`simpleshops`** („Simple Shops“)
- **Warum nicht Simple Money?** Der Auftrag verlangt „Geld optional“; Simple Money soll ein Geld-Mod bleiben, der Shop muss ohne Scheine laufen.
  **Nicht SimpleBuilding** (zu groß, 26.3-only reicht, Modul-Aufteilung), **nicht Simple Tweaks** (nur Claims/Kompat).
- Additiv unter `modules/simpleshops/` (`python tools/newmod.py simpleshops "Simple Shops"`), `requires: [simplelib]` (Kastenstil/UI-Bausteine,
  Regel 6a, gebündelt wie Simple Maps), `optional: [simplemoney, simpletweaks, jade, jei, modmenu]`. Allein spielbar mit Vanilla + Cloth Config + simplelib.
- Schutz über `framework` (`Protection`), vom Modul selbst gebündelt (Regel 3) – **keine** Klassenimporte aus `simpletweaks`/`simplebuilding`.
- Nur **26.3** (Port-Run später). Alle drei Loader im selben Zug; Fabric zuerst.

## 2. Ablauf und GUIs

### 2.1 Besitzer
1. Verkaufsblock setzen (Besitzer-UUID = Platzierer; wird **nie** aus Item/Client gelesen), Vitrine daraufsetzen (ohne Verkaufsblock darunter nicht platzierbar).
2. **Ware:** Rechtsklick Vitrine mit Item in der Hand = ganzer Stapel ins nächste freie Fach (Schnell-Weg, nur Besitzer); mit leerer Hand = Waren-GUI
   (5 echte Slots + Spielerinventar, nur Besitzer) zum Ein-/Herausnehmen. „Item rein → steht zum Verkauf, Item raus → nicht mehr.“
3. **Preis + Einnahmen:** Rechtsklick Verkaufsblock = Besitzer-GUI.
4. Ohne Preis oder ohne Ware: kein Verkauf (Käufer sieht grauen Tausch-Knopf). Besitzer holt Einnahmen jederzeit (auch offline-Zeit lang gesammelt).

### 2.2 Preis-Slots (Filter-Prinzip, **Vorlagen**, nur Besitzer)
- Slot anklicken mit Item im Cursor: **Rechtsklick** = Anzahl um die Stapelgröße des Cursor-Stacks erhöhen, **Linksklick** = +1.
  **Leerer Cursor + Klick** auf belegten Slot = Preis-Item entfernen. Der Cursor-Stack wird **nie verbraucht** (Vorlage); Item-Typ/Komponenten
  kommen vom Cursor. Anderes Item auf belegten Slot = ersetzt Vorlage (Anzahl neu = Klick-Regel). Obergrenze je Slot = Item-Maximalstapel (Werkzeug 1).
- **Layout (5 Positionen −2 −1 0 +1 +2, Mitte zuerst):** leer: nur Mitte sichtbar. Mitte gefüllt → ±1 erscheinen. ±1 gefüllt → ±2 auf dieser Seite.
  Entfernen schiebt die weiter außen liegenden Slots derselben Seite nach innen (keine Lücke); Mitte entfernen → rechter Nachbar rückt in die Mitte.
  Gleiches Item zweimal ist erlaubt (Anzahl addieren geht über Klick im selben Slot).
- Leerer Preis = kein Verkauf.

```
Besitzer-GUI Verkaufsblock                      Käufer-GUI (Vitrine oder Verkaufsblock anklicken)
┌─ Verkaufsblock ────────────────────┐          ┌─ Shop von Ouzzi ──────────────────┐
│ Preis:   [  ][ 8x][  ]             │          │ Ware: [Bretter 42][Äpfel 9][ ][ ]  │  <- wählen (Rahmen)
│           -1   0  +1   (+-2 folgen)│          │ Preis: [ 8x][ 1x][  ]  (nur Vorlage)│
│ Einnahmen (9):                     │          │ Du:    [  ][  ][  ]  <- Preis-Items │
│ [ ][ ][ ][ ][ ][ ][ ][ ][ ]        │          │        legen (gleiche Slot-Zahl)    │
│ ────────────────────────────────── │          │        [ Tauschen ]  (Shift = max.) │
│ Spielerinventar                    │          │ ─────────────────────────────────── │
└────────────────────────────────────┘          │ Spielerinventar                     │
                                                └────────────────────────────────────┘
```
Käufer: **dieselbe Anzahl Slots** wie belegte Preis-Slots, an derselben Position. Slot nimmt nur das passende Preis-Item (Typ/Komponenten laut 4.4);
Überzahlung bleibt im Käufer-Slot liegen und wird beim Schließen zurückgegeben. **Vending-Verhalten:** Käufer wählt eine Ware (Standard: erstes
nicht leeres Fach), ein Tausch gibt **1 Stück** der gewählten Ware (Frage 4); Shift-Klick auf „Tauschen“ wiederholt, solange Preis, Ware und Platz reichen.
Leere Fächer sind nicht wählbar; ausverkauft = Knopf grau. Jede Änderung im Shop erhöht eine `revision`; der Tausch gilt nur für die Revision, die der Käufer sieht.

### 2.3 Nur ein Spieler gleichzeitig
Sperre im Verkaufsblock (`user` UUID, Menü-Referenz): gilt für Besitzer-GUI, Käufer-GUI **und** Waren-GUI der Vitrine (die Vitrine fragt den
Verkaufsblock darunter). Zweiter Spieler: Klick tut nichts (Klang „besetzt“), kein Warten. Siehe 4.3 für Ablauf/Entsperren.

## 3. Grief-Schutz

### 3.1 Besitz und Abbau
- Block-Entity speichert `owner` (UUID, vom Server beim Platzieren). **Abbau nur Besitzer und Ops** (Level ≥ 2, wie Claims-Admin `LEVEL_OWNERS`/Gamemaster – Frage 8).
  Geschützt auf drei Wegen, damit kein Werkzeug vorbeiläuft:
  1. **Mixin `ServerPlayerGameMode.destroyBlock`** (HEAD, wie `ClaimGameModeMixin`): Nicht-Besitzer → abbrechen (alle drei Loader, ein gemeinsamer Mixin;
     deckt Kreativ, Vein, Hammer, Strip, Oktant ab, soweit sie `gameMode.destroyBlock` nutzen).
  2. **Eigener Protection-Provider** `Protection.register(server, "simpleshops", …)`: `allows(Target, DESTROY|BUILD|ANY)` = false für Positionen mit Shop-Block,
     wenn `actor ≠ owner` und nicht `administrator`; `allowsAutomation(source, target)` = false, wenn `target` oder `source` ein Shop-Block ist.
     Damit respektieren auch Laser, Kolben-Breaker, Baustab, Pads, Attractor, Blaupause die Vitrine ohne eigenen Code. Der Provider löst die Position
     nur in **geladenen** Chunks auf (`hasChunkAt`), unbekannt = Shop-Block nicht angenommen.
  3. **`getDestroyProgress`** liefert 0 für Nicht-Besitzer (Client kennt Owner-UUID aus dem Sync) – keine Abbau-Animation, kein Fake-Brechen.
  Ersetzen per Eimer/Block-Setzen: Shop-Blöcke sind nicht ersetzbar (`replaceable` nein, `canBeReplaced` false). `/setblock`/`/fill` bleiben Admin-Befehl.
- **Drops:** Abbau durch den Besitzer gibt Block-Item **ohne Inhalt** (kein Waren-/Einnahmen-NBT im Item, kein Dupe-Pfad über Itemkomponenten). Waren,
  Einnahmen als Item-Entities an der Position, **genau einmal** über `preRemoveSideEffects` (Vitrine: Waren; Verkaufsblock: Einnahmen). Preis-Vorlagen werden **nie** gedroppt.
- **Besitzer-Abbau Verkaufsblock mit Vitrine darüber:** Vitrine bleibt stehen, wird `inactive` (zeigt gedimmt, Käufer sieht „kein Verkauf“); sie bricht nie von selbst.

### 3.2 Explosions-/Umweltfestigkeit
| Gefahr | Maßnahme (Vorschlag, per Test zu belegen) |
|---|---|
| TNT, Creeper, Bett im Nether, Respawn-Anker, Wither-Schädel, Endkristall | `explosionResistance = 3 600 000` (wie Bedrock), Härte moderat (≈2,0, jedes Werkzeug/Hand baut ab – nur der Besitzer darf) |
| Wither-Körper, Ender-Drache (Block-Fressen) | Blöcke in `#minecraft:wither_immune` und `#minecraft:dragon_immune` (Tags ergänzen, ohne Replace) |
| Enderman nimmt Block | Block-Entities sind nicht aufnehmbar; **nicht** in `enderman_holdable` (Test: kein Aufnehmen) |
| Kolben/Slimeblock/Honig | `pushReaction(BLOCK)` für alle drei Blöcke (verhindert auch das Mitziehen ganzer Strukturen) |
| Feuer, Lava (Zündung/Brand) | nicht entflammbar (kein FireBlock-Eintrag), Holzoptik hat **keinen** Brennwert |
| Wasser/Lava-Fluss, Eimer | feste Kollisionsbox (`blocksMotion`) → `FlowingFluid.canHoldFluid` ersetzt sie nie; nicht ersetzbar (Test mit Fluss) |
| Schwerkraft/Stützentzug | Verkaufsblock = Vollblock ohne Schwerkraft/`canSurvive`; er schwebt notfalls. Vitrine ohne `updateShape`-Bruch |
| Blitz, Drachenatem, Feuerball, Ghast | Explosionsresistenz deckt Feuerball/Atem; Blitz setzt nur Brennbares in Brand (nicht brennbar) |
| Sprengung im Claim | Claims schützen die Umwelt ohnehin nur für „gleicher Besitzer“; Shop-Schutz gilt **zusätzlich** und unabhängig davon (Wilderness-Shops sind gleich sicher) |

Schild-Wirkung: ein 3,6-Mio.-Block schluckt Explosionsstrahlen (wie Obsidian/Bedrock) – gewollt, im Wiki erwähnen.
Datenpaket-Spieler können Blast Resistance nicht senken; Server-Config bietet **keinen** Schalter dafür (AGENTS 3.8: nur Obergrenzen/Anzahl).

### 3.3 Trichter, Kolben, Saugen
- Vitrine und Verkaufsblock implementieren **weder `Container` noch `WorldlyContainer`** und registrieren **keine** Item-Transfer-Fähigkeit (Fabric Transfer API,
  NeoForge `Capabilities.Item.BLOCK`, Forge `ForgeCapabilities`). Vanilla-Trichter/Trichter-Loren/Werfer/Kupfergolem/`ClaimAutomation.transfer` erkennen sie
  dann nicht als Behälter („unbekannt = nie erlaubt“). Kein Vergleichsausgang (`hasAnalogOutputSignal` false). Der eigene Provider bleibt Absicherung für SB-Trichter/-Kolben.
- Modbare Rohre/fremde Mods, die eigene Zugriffe bauen: Menü ist die einzige Schnittstelle; Test mit Fabric-Transfer-Lookup auf beiden Blöcken (muss `null` liefern).
- Ware raus nur über GUI-Tausch/Besitzer-GUI; keine Dropper-/Dispenser-Ausgabe.

### 3.4 Claims-Zusammenspiel
- **Platzieren in fremdem Claim** verlangt Recht **B** (Claims greifen vorher über `ClaimBlockItemMixin`). Unser Platzier-Check ergänzt nichts.
- **Käufer in fremdem Claim:** Nach `Claims.useAction` wäre ein Menü-Block Recht **C** (Container) – zu viel für Kunden. Lösung ohne Code in Simple Tweaks:
  `simpleshops` liefert ein Daten-Tag `data/simpletweaks/tags/block/claim_use.json` (nur eigene Blöcke, `replace: false`) → Recht **U**. Dann reicht
  `*: U` bzw. `@guest` im Rechte-Code. Ohne Claim: jeder darf kaufen. Das ist Doku-/Wiki-Pflicht („Shop im Claim: Gästen U geben“).
- Optional Stufe 4: Claims-Manager (M) darf fremde Shops auf seinem Land abbauen → braucht eine neue `Action.MANAGE` im Framework (Änderung in
  `framework`, `WorldPermissions`, `ClaimRights`); **nicht** in Stufe 1 (Frage 7).
- Claim-Wechsel nach dem Bau (Land später beansprucht): Shop bleibt Besitzereigentum; Landbesitzer kann ihn nicht abbauen (nur bei Frage-7-Variante).
- Missbrauch „unzerstörbare Blöcke als Wegsperre“: Config `maxShopsPerPlayer` (Vorschlag 16, Obergrenze 64, serverseitig); pro Chunk max. 8; Zähler im Ledger
  des Moduls (nicht im Block). Befehl `/shop list|info|remove` (OP) zum Aufräumen (Stufe 4).

### 3.5 Besitzer offline/gelöscht, verlassene Shops
- Offline-Besitzer: Shop läuft normal, Einnahmen sammeln sich im Fach; **kein** „Besitzer online“-Zwang (anders als Chunk-Loader), weil nichts lädt.
- Gelöschter Spieler (UUID unbekannt): Shop bleibt, nur Ops dürfen abbauen. Name zeigt „unbekannt“.
- **Verlassene Shops:** **kein automatischer Ablauf** in Stufe 1–3 (Ablauf würde fremdes Gut freigeben, Datenverlust-Risiko). Ops-Weg über `/shop`.
  Optional später Config `abandonedDays` (Standard 0 = aus); abgelaufen = Shop wird für **alle** abbaubar, Inhalt droppt (Frage 9).

## 4. Duping-/Exploit-Schutz

### 4.1 Atomarer Tausch (eine Servermethode `ShopTrade.execute`, nur Server-Thread)
Reihenfolge – **erst alles prüfen, dann alles ändern, nichts dazwischen**:
1. Menü gültig (`stillValid`), Sperre gehört diesem Spieler, `revision` stimmt, Ware-Fach nicht leer, Preis nicht leer.
2. Käufer-Slots enthalten für jede Preis-Vorlage ≥ Anzahl **passender** Items (Abgleich 4.4); Summe je Vorlage separat zählen (gleiches Item in zwei
   Vorlagen: zusammen prüfen, nie doppelt zählen).
3. **Simulation:** Einnahmen-Fach kann die Zahlung aufnehmen (Stapel zusammenführen, 9 Slots), Käufer-Inventar kann 1 Stück Ware aufnehmen
   (`Inventory.getSlotWithRemainingSpace`/freier Slot). Sonst: Abbruch ohne Änderung (Klang „voll“). **Kein** Fallen-lassen-vor-die-Füße (Drop-Exploit).
4. **Commit:** Käufer-Slots −Preis → Einnahmen +Preis → Vitrine-Fach −1 → Inventar +1. Alles im selben Tick, keine Netz-/Datei-Wartepunkte dazwischen.
   Danach `setChanged` an beiden Block-Entities (gleicher Chunk, siehe 4.5), `revision++`, Klang + Partikel, Menüaktualisierung.
5. Jeder Fehlerpfad vor Schritt 4 ändert nichts. Ausnahme im Commit (Bug) → Rollback der Kopien aus Schritt 2/3 (Snapshot der betroffenen Stacks).
Client schickt nur „Tauschen(Ware-Index, revision, shift)“. Menge, Preis, Items kennt der Server aus der Block-Entity, nie aus dem Paket.

### 4.2 Preis-Vorlagen sind keine Items
Vorlagen leben nur als `ItemStack`-Kopie in der Block-Entity; Slots sind `mayPickup=false`, `mayPlace=false`, Klick läuft über eigenen Menü-Handler
(nur Besitzer). Shift-Klick, Drag-Verteilen, Doppelklick-Sammeln, Hotbar-Tasten, Offhand-Tausch, `quickMoveStack` und Creative-Klone erreichen sie nie.
Vorlagen werden nie gedroppt (nicht beim Abbau, nicht beim Schließen). Test: Preis setzen + Abbau → keine Preis-Items in der Welt.

### 4.3 Gleichzeitiger Zugriff, Sperre, Disconnect
- Sperre = `user` + Menü-Referenz (`WeakReference`), **nicht** nur eine Zeit. Beim Öffnen: ist `user` gesetzt, wird er geprüft: Spieler offline/tot/anderes
  Menü offen/`containerMenu != gespeichertes Menü`/Dimension anders/Abstand > 8 Blöcke → Sperre ist **verwaist** und wird übernommen.
- `Menu.removed()` (Schließen, Disconnect, Tod, Dimensionswechsel, Kick): Käufer-Slots zurück ins Inventar (Vanilla-Muster `clearContainer`, Rest droppt wie bei
  der Werkbank), Sperre lösen. Beim Server-Stopp laufen dieselben Hooks; **Sperre wird nie gespeichert** (nach Neustart immer frei).
- Zusätzlich Idle-Timeout 5 Minuten ohne Klick (Config, ≤30): Menü wird serverseitig geschlossen (`closeContainer`) – Schutz gegen AFK-Blockade.
- Besitzer und Käufer teilen dieselbe Sperre: Besitzer kann nicht ändern, während ein Käufer offen hat; `revision` fängt trotzdem Befehls-/Op-Änderungen ab.

### 4.4 Vergleich der Preis-Items (Empfehlung: **exakt**)
- **Standard: exakt** (`ItemStack.isSameItemSameComponents`, wie Filter „exakt“): nur Items mit gleichen Komponenten zählen. Warum: Kunde könnte sonst mit
  umbenannten/verzauberten/beschädigten/gefüllten Varianten bezahlen (z. B. Schulterbox mit Inhalt, 1-Haltbarkeit-Hacke statt voller), Besitzer bekommt Müll.
- Ausnahme **Haltbarkeits-Items** (`DAMAGE`-Komponente): Vorlage = Item mit Schaden 0 gilt nur für gleich beschädigte Items – im Zweifel nein (fail closed).
- Schalter „gleiche Art“ (Filter-Knopf, `UiFilterButton`) je Preis-Vorlage als Stufe 4 (Frage 5). Nicht in Stufe 1–3.
- Ware wird **als exakte Kopie des Stapels** übergeben (Komponenten bleiben, wie sie der Besitzer einlegte) – nichts wird neu erzeugt.

### 4.5 Chunk entladen, Kreativ, Rücknahme, sonstige Pfade
- Vitrine und Verkaufsblock liegen **vertikal benachbart = immer im selben Chunk**: sind beide geladen oder beide nicht; kein Tausch über Chunkgrenzen.
- Chunk entlädt sich, während die GUI offen ist: `stillValid` prüft `level.isLoaded(pos)`, dieselbe Block-Entity-Instanz (`!isRemoved`) und Abstand → Menü schließt,
  `removed()` gibt zurück. Kein Tausch gegen eine „tote“ Block-Entity.
- Kreativ-Mittelklick auf Shop-Block gibt nur das leere Block-Item (kein NBT, da keine Daten im Item; Kreativ-Klon der Block-Entity-Daten wird per `getCloneItemStack`
  ohne Komponenten umgangen).
- **Rücknahme:** kein Rückkauf, keine Stornofunktion (Tausch ist endgültig, Vorschau vor dem Tausch ist die GUI). Besitzer holt Ware/Einnahmen selbst.
- Fehlschlag-Rückmeldung nur per Klang/Partikel (kein Chat/Aktionsleiste).
- Welten-Reparatur: Shop-Block ohne Block-Entity (z. B. Datenverlust) → Block inaktiv, nicht abbaubar außer Op; keine Ware wird erfunden.
- Besitzer baut „sein“ Item: darf nie als Preis **und** Ware zugleich 1:1 zum Verdoppeln dienen: kein Dupe, nur Tausch gegen sich selbst, erlaubt.
- Race „Ware wird vom Besitzer gerade entnommen“: durch Sperre (4.3) ausgeschlossen.

## 5. Offene Besitzer-Fragen (jeweils mit Empfehlung)
1. **Modul:** neues Modul `simpleshops` – oder lieber in Simple Money? *Empfehlung:* neues Modul (Geld bleibt optional, Modul-Unabhängigkeit).
2. **Lager:** Ware in der Vitrine (so beschrieben) und Einnahmen im Verkaufsblock – oder alles im Verkaufsblock? *Empfehlung:* Ware in der Vitrine (sichtbar, ein Besitzer-Wunsch), Einnahmen im Verkaufsblock.
3. **Preis je Ware?** Ein Preis für alle 5 Fächer – oder je Fach ein Preis? *Empfehlung:* ein Preis je Paar; unterschiedliche Preise = mehrere Paare.
4. **Menge je Tausch:** 1 Stück pro Kauf (Preis gilt je Stück) – oder feste Packung (z. B. 16)? *Empfehlung:* 1 Stück, Shift-Klick wiederholt; Packungen als spätere Stufe.
5. **Preis-Vergleich:** exakt (Komponenten) oder gleiche Art? *Empfehlung:* exakt, „gleiche Art“ später je Vorlage mit Filter-Knopf.
6. **Glow-Vitrine:** nur Hell-Rendering (wie Glow-Rahmen) oder echtes Licht? *Empfehlung:* Hell-Rendering + Licht 6 (schwach, nur Vitrine).
7. **Claims-Manager/Landbesitzer darf fremde Shops abbauen?** *Empfehlung:* Stufe 1 nein (nur Besitzer + Ops); optional Stufe 4 mit neuer Action `MANAGE`.
8. **Ops-Stufe zum Abbau:** Gamemaster (2) oder Owner (4, wie Claims-Admin)? *Empfehlung:* Level 2.
9. **Verlassene Shops:** automatischer Ablauf? *Empfehlung:* nein (Config `abandonedDays` aus); Ops-Befehle genügen.
10. **Shop-Anzahl-Limit:** 16 je Spieler, 8 je Chunk als Anti-Wegsperre? *Empfehlung:* ja, beide einstellbar (Obergrenzen 64/32).
11. **Kauf im Claim ohne U:** Tag `claim_use` setzen (Käufer brauchen U) – oder Käufer dürfen immer? *Empfehlung:* Tag setzen; Besitzer entscheidet über `*: U`.
12. **Inventar voll:** Tausch abbrechen (Empfehlung) oder Ware fallen lassen? *Empfehlung:* abbrechen.
13. **Simple Money:** nur Preisliste + Wiki-Hinweis („Scheine als Preis“) oder Zusatzfunktion (Schnell-Preis aus Scheinen)? *Empfehlung:* nur Preisliste/Wiki.

## 6. Umsetzungs-Stufen mit Tests
Alle Stufen: nur 26.3, Fabric zuerst, NeoForge/Forge im selben Zug; Gate im Worktree (`check`, Wiki, Texturen, Tooltip-Check, Handbücher);
Fabric-Tests brauchen `@GameTest`-Wrapper; Standalone-Target `module-simpleshops-*` (nur Modul + Vanilla + simplelib).

| Stufe | Inhalt | Tests (GameTest, Namensraum `simpleshops:`) |
|---|---|---|
| **1 Fundament + Schutz** | Modul-Gerüst, 3 Blöcke, Block-Entities (Owner, Waren, Preis, Einnahmen), Platzieren nur auf Verkaufsblock, Rezepte, Lang EN+DE, Schutz 3.1–3.3 (Mixin, Provider, Resistenz, Tags, Push, nicht-Container) | `place_needs_sales_block`, `owner_set_from_placer`, `stranger_cannot_break` (Spieler, Kreativ, Hammer/Vein via Protection), `owner_break_drops_once_no_templates`, `tnt_creeper_wither_dragon_resistance`, `piston_blocked`, `fluid_does_not_replace`, `hopper_dispenser_golem_cannot_access`, `transfer_lookup_null`, `limits_per_player_chunk` |
| **2 GUIs + Tausch** | Preis-Slots (Layout 5er, Klickregeln), Käufer-GUI, Waren-GUI, Tausch atomar, Sperre, Idle-Timeout | `price_slot_clicks` (rechts/links/entfernen/Layout/Kompaktierung), `template_not_consumed`, `trade_ok`, `trade_atomic_failures` (voll Einnahmen, voll Inventar, falsches Item, falsche Menge), `trade_exact_vs_components`, `lock_second_player`, `lock_disconnect_release`, `lock_stale_takeover`, `chunk_unload_closes`, `buyer_slots_returned_on_close`, `revision_mismatch_rejected`, `dupe_invariant` (Items vorher = nachher über zufällige Klickfolgen, Fuzz) |
| **3 Optik** | Modelle/Texturen (Generator + Vorschau), Renderer, Glow, Preisschild, Klänge/Partikel, Guide/Wiki/Jade, JEI-Info | Client: `ShopRenderClientTest` (1–5 Waren, Glow, inaktiv), `check_missing_textures`, `generate_textures --check`, Wiki `generate.py --all --check`, Guide 0 Probleme; Jade-Provider-Split-Test |
| **4 Claims/Admin/Kompat** | `claim_use`-Tag + Doku, optional `Action.MANAGE`, `/shop list|info|remove`, `abandonedDays`, Preis „gleiche Art“, Simple-Money-Preisliste, ConfigOptionTests | `claim_guest_can_buy_with_U`, `claim_member_without_U_denied`, `claim_owner_vs_foreign_shop`, `shop_command_op_only`, `abandoned_expiry`, `money_prices_json_valid`, Modul-Standalone, Integration `integration-263` |

**Nach jeder Stufe:** Abnahme auf der Development-Instanz (Klick-Test, Konsole), erst dann Frage nach Production. Stufe 1 und 2 sollten **nicht**
getrennt ausgeliefert werden (Stufe 1 allein = unzerstörbare Blöcke ohne Funktion).

## 7. Antworten Besitzer (2026-10-15, verbindlich)
1. Modul: neues Sub-Mod **Simple Shops** (`simpleshops`), gehört zu SimpleBuilding (Super-Mod requires Sub-Mod, Config „Enable Simple Shops“).
2. Lager: Ware lagert in der **Vitrine**, das Geld/die Preis-Items (Einnahmen) im **Verkaufsblock**.
3. Preis: **ein Preis je Verkaufsblock** (Preisblock).
4. Menge je Kauf: **1 Stück**.
5. Preis-Vergleich: **exakt gleich inklusive Name und Verzauberung** (alle Komponenten).
6. Glow-Vitrine: **hell gerendert + schwaches Licht**.
7. Claims: Claim-Verwalter darf fremde Vitrinen **nicht** abbauen. Der Vitrinen-Besitzer darf nur dort **platzieren**, wo er im Claim bauen (B) darf; **abbauen** darf der Besitzer seine Vitrine/seinen Verkaufsblock danach immer, auch wenn sich die Claim-Rechte geändert haben.
8. Ops: ab **Stufe 2** abbaubar.
9. Verlassene Shops: kein automatischer Ablauf (Empfehlung übernommen, Besitzer hat nicht widersprochen).
10. Limit: **16 je Spieler**, **kein** Chunk-Limit (mehrere in einem Chunk erlaubt).
11. Kaufen im fremden Claim: **braucht kein Recht** (kein claim_use-Tag für Verkaufsblock/Vitrine-Kauf).
12. Käufer-Inventar voll: Ware wird **gedroppt** (vor dem Käufer, wie Villager-Überlauf).
13. Simple Money: **nur Preisliste bzw. Wiki-Hinweis**, keine Zusatzfunktion.
