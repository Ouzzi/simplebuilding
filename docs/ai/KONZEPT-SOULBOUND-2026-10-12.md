# Konzept Soulbound-Verzauberung (2026-10-12)

Nur Konzept (Queue Nachtrag 42), kein Code. Mit „(Vorschlag)“ markierte Punkte hat Claude festgelegt; der Besitzer kann
jederzeit ändern. Offene Fragen am Ende.

## 1. Idee
Ein Item mit **Soulbound** bleibt beim Tod im Besitz des Spielers und liegt nach dem Respawn wieder im Inventar
(gleicher Slot, wenn frei). Vanilla kennt nur `keepInventory` (alles) und Fluch des Verschwindens (Gegenteil).
Soulbound ist die gezielte, teure, seltene Version für einzelne Lieblingsstücke.

## 2. Modulzugehörigkeit (Vorschlag)
- Heimat: **Simple Respawn** (`simplerespawn`, laut `docs/MODS-UND-FEATURES.md` „wartet auf Besitzer-Konzept“; Tod/Respawn
  ist dort das Thema, siehe `KONZEPT-FARBPINSEL-RESPAWN-2026-10-09.md`). Teilt sich das Modul mit dem Death Vault
  (`KONZEPT-DEATH-VAULT-2026-10-12.md`); beide teilen die Todes-Buchführung (Item-Liste je Tod).
- Falls `simplerespawn` nicht gebaut wird: Simple Tools (Haltbarkeits-/Item-Verzauberungen). Nicht in SimpleBuilding-Kern
  (Prinzip: der Kern bleibt Bau-Mod; die Verzauberung braucht nichts aus ihm).
- **Allein spielbar** (`docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`): nur Vanilla + Cloth Config. Bezugswege (siehe 4) sind
  Vanilla-Material/Level/Loot. Simple Money ist nur optionaler Zusatzweg (Händler gegen Geld, bedingte Datei nach Regel 4).
  Fremde IDs (Money-Scheine) nur mit `fabric:all_mods_loaded` / `neoforge:mod_loaded`. Wiki-Notiz „mit Simple Money“.

## 3. Verhalten
- Registrierung als datengetriebene Verzauberung (`soulbound`, Namespace des Moduls), `max_level` 1 (Stufen siehe 5),
  Slot-Gruppe `any` (alles, was Vanilla verzaubern lässt, plus Buch). Tooltip nach Tooltip-Regel (AGENTS.md 14): nur
  der Verzauberungsname, keine Beschreibung.
- Serverseitig im Todes-Ablauf (Fabric: `ServerPlayerEvents.COPY_FROM` bzw. Drop-Hook; NeoForge/Forge: `PlayerEvent.Clone`
  und `LivingDropsEvent`): vor dem Fallenlassen werden alle Stacks mit Soulbound (Inventar, Rüstung, Nebenhand, Hotbar,
  Curios/Rucksack-Inhalt nicht rekursiv, siehe Frage 6) aus der Drop-Liste genommen und im Spieler-Attachment
  `soulbound_pending` abgelegt; beim Klon (Respawn) in dieselben Slots zurückgeschrieben, freie Slots als Fallback,
  Überschuss am Respawnpunkt als Item-Entity (Besitzer-Eigentum, kein Despawn-Schutz nötig, da sofort).
- **Nur beim Tod des Spielers.** End-Rückkehr (Klon ohne Tod) darf nichts doppelt geben: das Attachment wird nur bei
  `wasDeath = true` gelesen und danach gelöscht.
- Tod ohne Respawn (Server-Neustart, Spieler loggt aus dem Todesbildschirm): `soulbound_pending` wird im Spieler-NBT
  gespeichert (Attachment ist persistent), beim nächsten Respawn eingelöst. Spieler wird gebannt/gelöscht: Daten verfallen.
- Ist `keepInventory` aktiv: Soulbound tut nichts (nichts geht verloren), wirkt aber auch nicht als Verbrauch.
- Gegenstand zerbricht im Tod nicht; Drops aus Totem-Rettung unberührt (Totem wirkt zuerst, kein Tod).
- Verlust über andere Wege (Lava, Void, Cactus, Items einem anderen Spieler geben, Truhe) bleibt: Soulbound ist keine
  Diebstahl-Sicherung, nur Todesschutz.

## 4. Fundort, Seltenheit, Kosten
- **Nicht am Verzauberungstisch** (nicht in `in_enchanting_table`, nicht in `tradeable` des Bibliothekars), damit es selten
  bleibt und nicht per Lapis-Farm erzwungen wird. (Vorschlag)
- Wege (Vorschlag, bewusst wenige):
  1. **Beute:** Verzauberungsbuch „Soulbound“ in seltenen Strukturtruhen (Antike Stadt, Festung Bibliothek, Bastion
     Schatzraum, Wüstentempel-Kammer), ca. 3–5 % je Truhe, gewichtetes Loot, nie in normalen Dorftruhen.
  2. **Fahrender Händler:** 1 Angebot mit niedrigem Gewicht (ca. 1 von 15 Wanderungen), Preis 12 Smaragde (+ Buch).
     Laut `AGENTS.md` §9 ist der fahrende Händler „selten“; passt. Hinweis: laut `docs/HANDOFF.md` gibt es keine
     Quellcode-Händlerlisten für Meisterhandel, der Händler-Weg ist neu zu bauen.
  3. **Simple Money (nur wenn geladen):** Bibliothekar-Meister bietet das Buch gegen hohe Geldsumme an (Preisliste
     `links:simplerespawn`, wie die anderen Module).
- **Anwendung am Amboss** (Vanilla-Mechanik: Buch + Item): kostet Ambosslevel; Soulbound-Buch hat Basiswert
  „sehr selten“ (Multiplikator 8 Buch / 4 Item), das entspricht ca. 35 Level (Besitzer: ~35). Level-Kosten über
  Datenwert `anvil_cost` (Server-Config, Grenzen 20–39, Standard 35); über 39 würde Vanilla „Zu teuer!“ zeigen
  (Vanilla-Grenze, Config-Obergrenze bleibt darunter, AGENTS.md Regel 8).
- Tragen-Voraussetzung: keine. Reparatur (Mending/Amboss) unverändert.

## 5. Stufen und Balance
Vorschlag: **Stufe 1 = Standard, keine Stufen im Spiel** (nur 1, wie Silk Touch). Ausgleich statt Stufen:
- **Haltbarkeitsabzug beim Rücktrag:** Das Item kommt mit weniger Haltbarkeit zurück: −25 % der Maximalhaltbarkeit
  (nie unter 1; Item geht dadurch nie kaputt). Config `soulbound_durability_loss_percent` (0–50, Standard 25).
  Nicht-haltbare Items (Bücher, Blöcke) verlieren nichts.
- Optional (Frage 2, Stufen): **Stufe II** „Seelenband“ = Item kommt ohne Abzug zurück; nur Beute-Weg, nochmals
  seltener. Nicht im ersten Wurf.
- **Nutzungsgrenze:** Stapel mit Anzahl > 1 werden nicht gerettet (Verzauberungen liegen am Einzel-Item; Stacks von
  Büchern/Sachen ohnehin nicht verzauberbar). Verzauberte Bücher mit Soulbound als Speicher-Enchant werden nicht
  gerettet (kein Gespeichertes Soulbound im Buch: verhindert Buch-Lager als „Tod-Sicherung“ im Inventar).
- **Pro Tod Obergrenze:** max. 5 Soulbound-Items werden gerettet (Config 1–36); überzählige fallen normal (zufällig
  gewählt, deterministisch nach Slot-Reihenfolge, damit testbar: zuerst Rüstung, Nebenhand, Hotbar, Rest). Verhindert,
  dass ein voller Inventar-Satz bei „Soulbound auf alles“ keepInventory ersetzt.
- **Vorrang vor Levelverlust:** XP/Level-Verlust beim Tod bleibt wie Vanilla (kein Teil von Soulbound).

## 6. Inkompatibilitäten
- **Fluch des Verschwindens:** exklusiv (Tag `#simplerespawn:exclusive_soulbound`). Anvil/Tisch verweigern die
  Kombination (Vanilla-Verhalten bei `exclusive_set`). Vorhandene Kombination (Alt-Welt): Verschwinden gewinnt, Item
  wird gelöscht, Soulbound ignoriert. Begründung: Fluch ist ausdrücklich „Item stirbt mit dir“.
- Kein Konflikt mit Fluch der Bindung (Rüstung bleibt anziehbar): erlaubt, die Kombination ist sogar sinnvoll.
- Kein Konflikt mit Mending/Haltbarkeit/Sparsamkeit (`simplequalityoflife:thrift`).
- Mod-Verzauberungen (Astral-Tisch, `KONZEPT-ASTRAL-VERZAUBERUNG-2026-10-09.md`): Soulbound ist eine **Schatz-Verzauberung**
  (`treasure`/`non_treasure` Tag: Schatz), damit der Astral-Tisch (3 zufällige, „keine Schatz-Verzauberungen“) es nie
  als Wurf anbietet. Auch nicht über „Buch kopieren“ vervielfältigbar (Frage 7).
- Beim Aufspalten am Schleifstein: Soulbound wird wie jede Verzauberung entfernt (Vanilla) und gibt Erfahrung zurück.
  Fluch-Regeln gelten nicht (es ist kein Fluch), also entfernbar. Der Besitzer kann das als Nachteil sehen (Frage 8).

## 7. Multiplayer und Anti-Dupe
- Rein **serverseitig**, Client bekommt nur die normale Item-Synchronisation; Config nur serverseitig (AGENTS.md Regel 8).
- Einziger Quell der Wahrheit: `soulbound_pending` im Spieler-Attachment. Rückgabe = `remove` aus dem Attachment und
  `give` im selben Tick; Absturz dazwischen: Attachment wird erst nach erfolgreichem `give` geschrieben/gelöscht (Reihenfolge:
  Item einsetzen → Pending löschen → speichern; bei Absturz vor dem Speichern liegt der Zustand vor dem Tod vor, kein
  Duplikat, höchstens Rücksetzung, wie bei jedem Welt-Rollback).
- Das Item verlässt die Welt als Entity **nie** (keine Item-Entity mit Soulbound-Stack beim Tod), also nichts, was ein
  anderer Spieler aufheben oder ein Hopper abgreifen könnte.
- **Zusammenspiel Death Vault:** ein von Soulbound geretteter Stack wird nicht in die Todesliste des Vault eingetragen
  (er wurde ja nicht verloren). Gemeinsame Regel: nur tatsächlich gedroppte Stacks stehen im Vault-Protokoll.
- **Fremder Tod / Kill durch Spieler:** Soulbound gilt auch im PvP (Besitzer-Frage 5), Standard: ja (kein Beute-Verlust
  für den Killer; passt zur seltenen Verzauberung).
- Sonderfälle: Tod in Fremddimension, Respawn in anderer Dimension: Daten hängen am Spieler, nicht am Level.
  Hardcore: Soulbound wirkt nicht (Spieler wird zum Zuschauer/Welt gelöscht); Config `soulbound_in_hardcore` aus.
  Tod durch `/kill`: wirkt (wie normaler Tod). Creative: nichts zu retten.
- Container, Enderkiste, Rucksack-Inhalt: bleiben unberührt (Rucksack wird nur als Gesamt-Stack betrachtet; Soulbound
  gilt nicht für Items im Rucksack, außer der Rucksack selbst trägt es; Frage 6).

## 8. Tests (später, Gametests und Standalone)
- Verzauberung ist registriert, nicht im Tisch, nicht im Tag `in_enchanting_table`, im Loot-Pool der Beispieltruhen.
- Tod simulieren (`player.kill`): Soulbound-Schwert im Hotbar-Slot 3 kehrt in Slot 3 zurück; ohne Soulbound fällt es.
- Haltbarkeit: −25 % und min. 1; nicht haltbare Items unverändert.
- Fluch des Verschwindens + Soulbound: Anvil verweigert; Alt-Welt-Item verschwindet.
- Obergrenze 5: sechstes Item fällt, Reihenfolge deterministisch.
- Zweimal sterben ohne Respawn-Zwischenschritt (Mehrfach-Tod): nichts doppelt, Pending leer nach Einlösung.
- Absturz-Simulation: Pending lesen/schreiben/Reload mit `PlayerList.save`.
- keepInventory aktiv: keine Doppelung, kein Haltbarkeitsabzug.
- Standalone-Target `simplerespawn` (nur Vanilla + Cloth Config): Item, Verzauberung, Truhen-Loot, Amboss laufen; Geld-Händler-Datei
  nur mit Money (`isModLoaded`). Integration: Money-Weg.
- Loader: Fabric zuerst, NeoForge/Forge gebündelt danach (AGENTS.md 3.1). Datei-Check `check_data.py` des Moduls für
  Tags/Loot-Bedingungen.

## 9. Doku-Nachzug bei Umsetzung
`docs/MODS-UND-FEATURES.md`, Modul-Wiki (EN/DE, Hinweis „mit Simple Money“), Lang (US-Englisch + Deutsch), Guide-Buch, Testzentrale.

## Offene Besitzer-Fragen
1. Heimat: **Simple Respawn** (neues Modul, noch nicht freigegeben) oder vorerst Simple Tools? Soll das Modul erst mit
   Soulbound + Death Vault starten?
2. Stufen: Reicht eine Stufe mit −25 % Haltbarkeit, oder Stufe II ohne Abzug (nur Beute) zusätzlich?
3. Preis: ~35 Level am Amboss in Ordnung (Config 20–39)? Soll stattdessen ein Material (z. B. Seelenstaub, Nether-Stern)
   zusätzlich nötig sein?
4. Fundort: Beute in seltenen Strukturen + fahrender Händler (12 Smaragde) + optional Geld-Bibliothekar. Welche davon
   streichen/ergänzen? Darf der Händler es verkaufen?
5. PvP: Soulbound auch, wenn ein Spieler dich tötet (Standard ja)? Oder Item fällt, nur bei Umwelt-/Mob-Tod?
6. Rucksack/Behälter: Soulbound-Items im Rucksack mitretten, oder nur der Rucksack als Ganzes?
7. Buch-Kopie (Bibliothek/Schleifstein): erlaubt oder gesperrt?
8. Darf Soulbound am Schleifstein entfernt werden (Standard ja) oder wie ein Fluch nicht?
9. Obergrenze 5 Soulbound-Items je Tod und 25 % Haltbarkeitsabzug: passende Standardwerte?
10. Hardcore: wirkt Soulbound dort (Standard: nein)?
