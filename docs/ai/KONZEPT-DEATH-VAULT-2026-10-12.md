# Konzept Death Vault (2026-10-12)

Nur Konzept (Queue Nachtrag 42), kein Code. Mit „(Vorschlag)“ markierte Punkte hat Claude festgelegt; der Besitzer kann
sie ändern. Offene Fragen am Ende.

## 1. Idee
Nach einem Tod kann der Spieler **3–5 zufällige Items** aus dem Verlust des **letzten Todes** gegen Bezahlung
zurückholen, aber nur solche, die verloren und bisher **nicht aufgehoben** wurden. Das ist ein Trostpflaster, kein
Ersatz für den Rückweg zum Grab: Der Rest bleibt Risiko. Kein keepInventory durch die Hintertür.

## 2. Modul (Vorschlag)
- **Simple Respawn** (`simplerespawn`, geplant, siehe `KONZEPT-FARBPINSEL-RESPAWN-2026-10-09.md` Teil B und
  `KONZEPT-SOULBOUND-2026-10-12.md`). Alles bleibt dort, was Tod/Respawn betrifft.
- Allein spielbar (`PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`): nur Vanilla + Cloth Config. Bezahlweg ohne Partner: **XP-Level**
  und **Vanilla-Material**. Simple Money ist ein zusätzlicher Bezahlweg (bedingt nach Regel 4: `mod_loaded`, Preisliste
  `links:simplerespawn`, Wiki „mit Simple Money“). Der Vault-Block ist Vanilla-craftbar (Rezept siehe 5).
- Nutzt keine Klasse eines anderen Moduls; Geld ausschließlich über öffentliche Item-IDs/Tags (`#simplemoney:bills`, falls
  vorhanden, sonst Bezahlweg abgeschaltet).

## 3. Todes-Protokoll (Kern der Dupe-Sicherheit)
Pro Spieler genau **ein** Eintrag `last_death` (nur der letzte Tod, ältere verfallen sofort beim neuen Tod):
`death_id` (UUID), Zeit, Dimension, Position, Liste der **verlorenen Stacks** `{ItemStack, entity_uuid}` und Status.
Serverseitig im Spieler-Attachment (persistent), nie vom Client lesbar außer für die Anzeige.

Aufbau beim Tod (Drop-Hook, Fabric/NeoForge/Forge wie Soulbound):
1. Nur Stacks, die die Welt tatsächlich als **Item-Entity** erreichen, werden eingetragen (also nicht Soulbound-gerettete,
   nicht Curse-of-Vanishing-gelöschte, nicht bei keepInventory behaltene). Jeder Eintrag merkt `entity_uuid`.
2. Ein Eintrag ist nur **einlösbar**, solange seine Item-Entity **noch nicht aufgehoben, zerstört oder despawnt** ist
   (Zustand siehe 4). Der Vault bietet Items an, die der Spieler *nicht mehr hat*. Das Wiedereinlösen hat aber **nie ein
   Original-Duplikat** zur Folge, siehe nächster Punkt.
3. **Geister-Regel (kein Dupe):** Beim Einlösen wird die zugehörige **Item-Entity aus der Welt entfernt**, falls sie noch
   existiert (Chunk geladen: sofort `discard`; Chunk nicht geladen: Eintrag als `pending_discard` markiert, beim Laden des
   Chunks oder beim Item-Join-Event dieser UUID verworfen). So existiert zu jedem Zeitpunkt höchstens *ein* Exemplar:
   entweder am Grab oder zurückgeholt. Das ist die zentrale Eigenschaft (Invariante) und wird getestet.
4. **Einmal je Tod:** `redeemed = true` auf dem Eintrag `death_id`; es gibt genau **eine** Rückholung pro Tod (eine Runde
   mit N Items, siehe 6). Danach ist der ganze Eintrag gesperrt, auch für die übrigen Items.
5. **Nur der Eigentümer:** Rückholung nur durch die Spieler-UUID des Eintrags.

## 4. Item-Entity-Tracking (serverseitig)
Eine Item-Entity ist „verloren“ (einlösbar), wenn sie existiert oder per Ereignis als verloren bekannt ist, und nicht „aufgehoben“.
- **Aufheben erkennen:** Pickup-Hook (`EntityItemPickupEvent`, `ItemEntity#playerTouch`/Mixin am Hopper-/Minecart-Pfad und
  Mob-Pickup `Mob#pickUpItem`): stammt der Stack von `entity_uuid` ⇒ Eintrag `picked_up` (egal wer aufhebt, auch ein anderer Spieler
  oder Mob; Hopper zählen wie Aufheben).
- **Teilaufheben:** Teilmengen (Stack von 64, Spieler hebt 20 auf): Eintrag `picked_up` komplett, nicht anbieten (Vorschlag: sicher).
- **Despawn (5 Minuten):** Der Despawn erzeugt ein Ereignis (`ItemEntity#discard` mit Grund `DESPAWN`/ageOut). Eintrag wird `despawned`
  und **bleibt einlösbar** (das ist der Hauptnutzen: Items, die sonst weg sind). Gilt Config `vault_despawn_refund = true`.
- **Lava/Feuer/Kaktus/Explosion/Void:** Zerstörung durch Schaden ⇒ `destroyed`. Vorschlag: **einlösbar** (Spieler hat Lava-Tod, alles ist
  weg; ohne Rückholung wäre der Vault für den häufigsten Tod nutzlos), außer Void (siehe unten). Neuer Stack ist Kopie des Eintrags.
  Unverwundbare Items (Netherit) überleben Lava ohnehin: sie liegen am Grab und gelten als „vorhanden“.
- **Void:** Item fällt unter y = min. Einlösbar (Vorschlag), weil das Grab nicht erreichbar ist.
- **Chunk entladen:** Entity nicht geladen, Status bleibt `exists_unloaded`. Einlösung wählt dann die *Geister-Regel* (3.3). Der
  Spieler darf also *auch dann* zurückholen, wenn er das Grab nicht findet; die originale Entity wird beim nächsten Laden verworfen.
- **Item-Entity zu Entity-Wechsel:** Items, die durch Portale (Nether-Portal-Teleport) die Dimension wechseln, behalten die
  UUID; Tracking hängt an der UUID, nicht am Chunk.
- **Behälter-Wege:** Hopper-Aufnahme = `picked_up`; ein Item, das in ein Fass/Truhe gelegt wurde, ist ebenfalls `picked_up`.
- **Server-Neustart:** Das Protokoll wird mit dem Spieler gespeichert; Item-Entities speichert das Spiel in den Chunks mit ihrer UUID. Nach
  dem Neustart ist die Zuordnung per UUID intakt. Geht die Entity-UUID verloren (Chunk-Reparatur), wird der Eintrag als
  `unknown` behandelt und **nicht angeboten** (Fail-Closed).
- **Fail-Closed generell:** Bei jedem unklaren Zustand (Status unbekannt, Entity existiert, aber Eigentümer-Check schlägt fehl) wird der
  Stack *nicht* angeboten. Lieber ein Item zu wenig zurück als eines doppelt.
- Aufwand pro Tod: höchstens ein paar Dutzend UUID-Einträge (Inventar 41 Slots); kein Dauer-Scan, nur ereignisbasiert + Sicht
  beim Öffnen des Vault (Entity-Lookup per UUID).

## 5. Ort der Rückholung (Vorschlag)
Drei Kandidaten; Empfehlung **Block**, weil er allein spielbar, sichtbar und multiplayer-sicher ist:
- **A (Empfehlung): „Gruft-Schrein“ (Death Vault) als Block.** Rezept (Vanilla-Material): Obsidian + Seelensand + Ender-Auge /
  Schädel im Muster; mit Money-Zusatzrezept-Variante nicht nötig. Rechtsklick öffnet eine kleine UI (3 Reihen wie Händler):
  zeigt den Tod (Zeit, Dimension, Koordinaten nur als „Dimension + Entfernung grob“), die Liste der angebotenen Stacks als
  **verdeckt**, Preis, Knopf „Bezahlen“. Der Spieler wählt nicht, welche Items (zufällig, siehe 6).
  Mehrere Schreine möglich; Eintrag ist am Spieler, nicht am Schrein. Server-Config: Schrein darf nur im Radius/in der Dimension
  des Todes? (Vorschlag: überall).
- B: Befehl `/vault` (OP-Standard aus, serverweit schaltbar): wirkt exakt wie der Block, ohne Bau; für Server, die kein Item wollen.
  Standardmäßig *aus* (würde sonst die Todesstrafe aufweichen).
- C: NPC (Totengräber-Händler als Dorfbewohner-Beruf): schöner, aber mehr Aufwand (Beruf, Markierung, Modell), passt zu
  `KONZEPT-MOBS-2026-10-07.md`-Ideen; später möglich, nutzt dieselbe Server-Logik.
Alle Wege nutzen **dieselbe** serverseitige Funktion `DeathVault.redeem(player)`; UI ist nur Anzeige.

## 6. Ablauf und Anzahl
- „3–5 zufällige Items“ (Vorschlag): `n = clamp(randInt(3,5), 1, Anzahl einlösbarer Stacks)`; nicht mehr einlösbare Stacks zählen nicht.
  Zufall mit `RandomSource` des Servers, gewichtet **ungewichtet** (jeder Stack gleich), damit kein Gaming nach Wert möglich
  ist. Seed ist `death_id`, so ist die Auswahl pro Tod **fest** (kein Reroll durch Neuöffnen; Besitzer-Entscheidung 6).
- Vorschau zeigt vor Bezahlung nur **Anzahl und Preis**, nicht die Items (verdeckt); optional Config `vault_preview`: zeigt Art
  (Rüstung/Werkzeug/Block) ohne Details.
- Bezahlung → Auswahl → Items ins Inventar (Überschuss als Item-Entity am Spieler, besitzergebunden `thrower = player`) →
  Einlösung der Geister-Regel (3.3) → `redeemed = true`. Alles im selben Tick, atomar: Zahlung abziehen, dann geben; scheitert das
  Geben (Inventar voll), wird nichts abgebucht (vorher Platz prüfen, sonst am Boden ablegen und abbuchen nach Platzprüfung).
- Zeitfenster: Eintrag verfällt nach X Tagen (Vorschlag 7 Tage Echtzeit / `vault_expiry_hours`, 0 = nie) oder beim nächsten Tod.
- Ausgeschlossene Items (Tag `#simplerespawn:vault_blacklist`): z. B. Stacks aus Creative-Items, Befehlsblöcke, Mod-Items, die der
  Server sperrt, **Soulbound-Items** (kommen eh zurück), Items mit NBT-Flag `vault_ignore`.
- Gibt es 0 einlösbare Stacks: Schrein zeigt „Nichts zu holen“, keine Kosten.

## 7. Bezahlung (Vorschlag: gestaffelt, Config)
- **Weg 1, XP (Vanilla, Standard):** Kosten = 5 Level + 2 Level je zurückgeholtes Item, Deckel 15 Level (Config). Aus dem
  aktuellen Level; nicht genug Level ⇒ ablehnen (kein Schuldenmodell). Der Tod hat das Level schon fast auf null gesetzt
  (Vanilla), also **Level aus der Zeit nach dem Respawn**: faktisch zahlt man mit neuem Fortschritt. Alternative (Frage 2): Level
  *vor* dem Tod als Schuld speichern.
- **Weg 2, Material (Vanilla, immer verfügbar):** z. B. Seelenfragment-Gegenwert: 1 Diamant + 4 Obsidian + 1 Ender-Perle je 2 Items. Wirkt
  als Fallback ohne Level.
- **Weg 3, Simple Money (nur wenn geladen):** Geldscheine, Preis pro Item aus der Preisliste, ca. 30 % des Verkaufswerts, mindestens ein
  Schein; Preisliste `links:simplerespawn` bedingt. Ohne Money nicht vorhanden.
- Server wählt aktive Wege (Config `vault_payment` = `xp|material|money|any`), wie bei QueueEinträgen mit Obergrenzen (AGENTS.md 8).
- Mehrfacherhalt verhindert: ein Tod, eine Zahlung (siehe 3.4).

## 8. Multiplayer und Anti-Dupe (Zusammenfassung)
- Gesamte Logik serverseitig; Client schickt nur „Öffnen“ und „Bezahlen“, Server prüft Eigentümer, `redeemed`, Zahlung, Inventarplatz.
- Invariante „höchstens ein Exemplar je verlorenem Stack“ durch Geister-Regel (Abschnitt 3.3) und Fail-Closed (Abschnitt 4).
- Rückholung kopiert nur den Stack **aus dem Protokoll** (Snapshot beim Tod), nicht aus der Welt; NBT/Komponenten bleiben erhalten
  (Verzauberungen, Namen, Haltbarkeit zum Todeszeitpunkt). Beschädigte Haltbarkeit −10 % beim Zurückholen (Vorschlag, Frage 5).
- Spieler holt zurück, **dann** erreicht jemand das Grab: Entity wurde verworfen, nichts da. Spieler erreicht **erst** das Grab, dann Schrein:
  Eintrag war `picked_up`, nichts anzubieten.
- Mehrere Tode kurz hintereinander: nur der *letzte* Tod zählt; der Eintrag des vorigen verfällt (auch wenn nicht eingelöst).
  Ausnahme „Tod vor dem Respawn“ gibt es nicht.
- Tod durch anderen Spieler (PvP): Gegenstände, die der Killer aufhebt, sind `picked_up` (nicht zurückholbar). Config
  `vault_pvp = false` schaltet den Vault im PvP-Tod aus (Vorschlag: aus, sonst PvP-Beute wertlos).
- Admin: `/vault reset <spieler>` löscht den Eintrag (OP), `/vault info` zeigt Status (nur OP, für Support).
- Keine Chat-/Aktionsleiste-Meldungen (AGENTS.md 3.4): Rückmeldung über Klang, Partikel (Seelenflammen) und UI.

## 9. Tests (später)
- Tod mit 10 Stacks: Protokoll enthält genau die gedroppten Stacks mit Entity-UUID.
- Aufgehobene Stacks (Spieler, Mob, Hopper) werden nicht angeboten.
- Despawn (Alter hochsetzen): Eintrag bleibt einlösbar.
- Lava-Zerstörung: einlösbar; unzerstörbares Netherit-Item am Boden zählt nicht als verloren.
- Geister-Regel: nach Einlösung existiert die Original-Entity nicht mehr; Entity in entladenem Chunk wird beim Laden verworfen.
- Einmal je Tod: zweiter Versuch abgelehnt; zweiter Tod ersetzt Eintrag.
- Zahlung: zu wenig Level ⇒ nichts; Inventar voll ⇒ kein Abzug; Zahlung genau einmal.
- Soulbound-Stack erscheint nie im Protokoll.
- Neustart: Protokoll und UUID-Zuordnung bleiben bestehen.
- Standalone-Target `simplerespawn` (Vanilla + Cloth Config): XP/Material-Weg; Money-Weg im Integrationskatalog.

## 10. Doku-Nachzug bei Umsetzung
`docs/MODS-UND-FEATURES.md`, Modul-Wiki EN/DE, Lang, Guide-Buch, Preisliste `links:simplerespawn`, Testzentrale, Balancing-Zentrale (Kostenwerte).

## Offene Besitzer-Fragen
1. Ort: Block (Gruft-Schrein, Empfehlung), Befehl `/vault` oder NPC (Totengräber)? Mehrere zugleich?
2. Bezahlung: XP-Level (nach dem Respawn gezahlt oder Schuld aus dem Level vor dem Tod?), Material, Simple Money, oder alle drei?
3. Preishöhe: 5 Level + 2 je Item (Deckel 15) und Material (1 Diamant + 4 Obsidian + 1 Ender-Perle je 2 Items) passend? Geld 30 % Verkaufswert?
4. Anzahl: immer 3–5 zufällig, oder fester Wert, oder steigt mit Bezahlung? Auswahl verdeckt und fest je Tod (kein Reroll) in Ordnung?
5. Zustand der Items: unverändert zurück oder mit Haltbarkeitsabzug (Vorschlag −10 %)?
6. Despawn und Lava/Void: einlösbar (Vorschlag ja, nur dort lohnt sich der Vault)? Oder nur Items, die noch am Grab liegen?
7. PvP-Tod: Vault aus (Vorschlag) oder an?
8. Verfallszeit des Eintrags (7 Tage) und „nur der letzte Tod“: so gewollt?
9. Darf der Schrein Items zurückgeben, wenn das Grab noch existiert (Original wird dann entfernt, Geister-Regel), oder erst nach Despawn/Zerstörung?
10. Soll `simplerespawn` als eigenes Modul starten (zusammen mit Niederschlagen/Wiederbeleben und Soulbound) oder die Features in ein bestehendes Modul?
11. Tote Mehrspieler-Server: soll es eine Config geben, die Vault ganz abschaltet (Hardcore-Server)?

## Besitzer-Antworten 12.10. (verbindlich)
- Craftbarer Block mit GUI. Passiv zeigt die GUI, was man zuletzt (bei Toden) verloren hat.
- Opfer-Slot: Geld oder Diamanten hineinlegen → zufällige Items aus verlorenen Toden zurück. Wahrscheinlichkeit höher, je **neuer** der Tod und je **wertvoller** das Item; nur eine begrenzte Zahl je Opfer, mit sehr viel Einsatz theoretisch alles. Zurückgeholte Items werden aus der Liste gestrichen.
- Gilt für **alle Tode** (neuere wahrscheinlicher), nicht nur den letzten.
- Nur Items, die beim Tod gedroppt wurden und danach **despawnt** oder durch **Lava/Explosion** zerstört sind. Liegen sie noch (Grab/Boden), nicht einlösbar. Vor dem Tod gedroppte Items zählen nie.
- Tod durch Spieler: nur Items, die direkt despawnt sind oder die der andere Spieler direkt zerstört hat; hebt er sie auf (und droppt erneut), zählen sie nicht mehr.
- Rückgabe mit Haltbarkeit min(aktuell, 50) wie Soulbound.
- Config-Schalter zum Abschalten.
- Notiz: Falls je eine eigene Grabstein-Mod im Modpack geplant wird, wäre Rückgabe einfacher (Items aus dem Grab direkt ins Inventar). Derzeit keine geplant.
- Umsetzung jetzt in **Simple Tools** (Besitzer 12.10.), gemeinsame Todes-Buchführung mit Soulbound.
