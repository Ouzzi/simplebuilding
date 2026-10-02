# Warteschlange SimpleBuilding (Stand 2026-10-01, Hauptlinie 26.3)

Regeln: AGENTS.md; aktueller Bestand/Belege in docs/HANDOFF.md. Worker committen auf
ihrem Branch ohne Push/Merge. Alte Wellen und Run-Details sind in der Git-Historie erhalten.
Ein erledigter Codepunkt ersetzt weder Client-Gate noch Besitzer-Abnahme.

## Integriert / implementiert
- [x] Wellen 1–23: Werkzeuge, Lager/Maschinen, Config/Modpack, Erfolge/Quests, Immersion,
  Balancing-Zentrale und frühere Ports (historische Gate-Zahlen nicht als aktuelles Gate verwenden).
- [x] Welle 24 BB–GG: Detector/Rezepte, Handbücher, platzierte Bündel/Oktanten,
  Pad-Aktivzustände, Spawnzielwahl, Kerne und Neunerreihen/Kreativreiter.
- [x] Welle 24 HH/II/JJ: Materialkern-Padrezepte und Mobköpfe, Shulkerkisten-Stufen,
  Resonanzstab, Attraktor/Range/Touch und Gauge. GG+: nur Eichen-Holzdruckplatte im Modreiter.
- [x] Z: Luftsprung 20/10 s, XP-Leisten-Priorität, Linear/Bridge und Oktant-Rezept.
- [x] Welle 25 KK2/FIX16: zwei isolierte End-Signalkanäle, Schalter/Lampen und Astralgewölbe;
  EndSystemsTests, Rezept-/Sprach-/Config-/Tab-Integrität und Testzentralen-Abdeckung.
- [x] BUGS/MM: Hammer-Zeitmodell 1,5/+0,8/+0,7, Oktant 2 je Block, Eckenschlag,
  Transformationshinweise; Basisbuch-Rezeptfreischaltung und Kapitel per Tab/Itemverbrauch.
- [x] Verstaerktes Bündel: Rezept/Datagen/Abdeckung, kein Gameplay-Fix auf Verdacht.
- [x] Spawn: jede Signalstärkeänderung setzt die Ladung sofort zurück (auch kurze Impulse).
- [x] Multimod-Grundlage, Wiki-Modauswahl, getrennte Balancing-Ablagen, Scaffold/Manifest;
  Launch Hub integriert, Forge 26.3 opt-in (`-Pforge263=true`).
- [x] Simple Money als eigenes Modul; Wiki-Vertrag und sieben Item-Notizen korrigiert.
- [x] Amerikanisches Englisch und Zeilen-Layout umgesetzt; laufende Textpflege bleibt nötig.
- [x] Launch-Zentrale: alle elf Projektmods standardmäßig ausgewählt, einklappbare Abwahl; tatsächliche Ladung und Abwahl auf Fabric/NeoForge/Forge 26.3 geprüft.
- [x] Bett-/Tür-/Doppelpflanzenplatzierung mit zweiter Zellenprüfung repariert; 54/54 WandMode-Tests. Oktant-Füllungen überspringen diese Materialien ohne Verbrauch.
- [x] Claims-Folgeschutz für Crafter, Kupfergolem-Transfers und Blitz-Blockänderungen; 100/100 Modultests. Claims bleiben wegen weiterer offener Pfade AUS.

## Offen (inklusive Besitzerpunkte)
- [x] B8: Item-spezifische EN/DE-Zusätze für die 17 Wiki-Familien, einschließlich Alt-IDs; Rezept- und Zahlenkorrekturen gegen 26.3-Quellen. Wiki-Generator und Python-Tests im Worker-Worktree geprüft, kein Port/Push.
- [x] Strahlschalter-Rezeptfilter berücksichtigt amethyst_lens und die alte laser_pointer-ID (RecipeFilter.java).
- [x] Serielle Fabric-/NeoForge-26.3-Clientprüfung und gezielte Nachprüfung der belegten Testfehler; Dimensions/QoL/Sounds/Visuals-Smokes: `docs/ai/CLIENT-ACCEPTANCE-2026-10-01.md`.
- [ ] Weitere Sichtabnahme für Buch-Screen, Truhen, Kern-Animation und neue Gadgets; Octant-Manager-Kontrast verbessern. Bestehende Screenshots sind keine pauschale Abnahme.
- [ ] Testzentrale in der Besitzerwelt neu bauen; automatische GameTest-Welten ersetzen keine Abnahme.
- [ ] Besitzerentscheidung zu Reparatur/Haltbarkeit des Resonanzstabs; Rotator sperrt Mending bereits.
- [ ] Rueckfragen Besitzer: Excavator/Diamond Ingots im Vorlagen-Tooltip, Cover-Buecher im Loot (Code vs HANDOFF), Liste G (58 Punkte) (Rotator hat bereits kein Mending)
- [ ] Rueckfragen neu: 12 Config-Ideen (Run D), Kern-Vorschlaege (Maurer-Diamantkern, 2. Eisenkern-Quelle, Enderit 0,5 %)
- [x] Balancing-Zentrale schreibt unterstützte Java-/JSON-Werte in die Modquellen, einschließlich Vorschau, Konfliktprüfung und Rollback (sbdev/service.py, tests/test_phase2.py). Nicht zugeordnete Werte bleiben ausdrücklich Planwerte; kein automatischer Live-Reload kompilierten Java-Codes.
- [ ] Beschaffungszeit je Item: Zeit bis zum 1. (und k.) Stueck je Quelle, gezielt vs. normales Spiel, mit Zeitalter-Einordnung ("vor Braustand & Traenke") auf Item-Seiten und in der Beschaffungs-Uebersicht (Modell: tools/devserver/sbdev/model.py)
- [ ] Diagramm "Zeit bis k Stueck" mit logarithmischer Zeitachse und Zeitalter-Linien auf Item-Seiten
- [ ] Handel: "im Angebot je Haendler/Dorfbewohner" (aus Poolgroesse, Ziehungen und Angebots-Chance) statt nur der rohen Angebots-Chance
- [ ] Loot: "Ø Stueck je Kiste" neben der Chance; Pools, die im Code fuer mehrere Tabellen gelten (Bastion, Tresore), sichtbar markieren
- [ ] Deep-Link auf eine einzelne Tabellenzeile (?f=<id>) mit Scrollen und kurzer Hervorhebung; "Seite nicht gefunden" nennt den Pfad und fuehrt zurueck
- [x] Filterzustand je Mod/Liste und Rezept-Ansicht einschließlich Suchtext merken; Browserprüfung mit Neuladen und gesperrtem Storage grün.
- [x] Sticky Tabellenköpfe in langen Tabellen; tastaturbedienbare Scrollbereiche, Desktop-/Mobil-Browserprüfung grün.
- [x] Zaehler je Bereich in der Wiki-Seitenleiste vorhanden.
- [ ] Config-Seite: Wertebereich (min ... max aus @BoundedDiscrete/validate()), Client/Server und "wirkt bei /reload" als eigene Spalten
- [x] Leere Listen/Filterergebnisse erklären den nächsten Schritt; Filter zurücksetzen mit Fokuswiederherstellung.
- [ ] Offen beim Besitzer: Kern-Chancen-Vorschlag fuer Eisen/Gold/Diamant/Netherit uebernehmen?
- [ ] Vorlagen teurer machen (mehrere Materialien statt 1 Glowstone/Tintenbeutel/Echoscherbe)
- [ ] Punkte 64-69: Sprachen, Attractor mit Ladung, neue Bloecke, Baustab ueber Planer, Kern-Module, Rucksack-Sortierung/Multi-Mod-Repo
- [ ] Port-Run 26.2/1.21.11/26.4 erst nach Besitzer-Abnahme; Faktenpass und aktuelles 26.3-Gate siehe HANDOFF.
- [ ] Besitzer-Abnahme des bereits implementierten Zeilen-Layouts in allen Kreativreitern.
- [ ] 26.4: Forge einschalten sobald Build da (-Pmc264_forge_version), Cloth-Config-Screen/Dev-Mods sobald 26.4-Builds da, NeoForge-26.4-Linie
- [ ] Baustab V1: normale Flaechen ueber den Blaupausen-Planer (Schutzpruefung pro Position) - ca. 40 Tests pinnen das heutige Verhalten
- [ ] Kerne als Baustab-Module + eigene Funktionen (Vorschlaege in docs/BAUWERKZEUGE-INTERAKTIONEN.md) - Besitzer: erst spaeter
- [ ] Kerne: Netherstern nur ab Diamant, Netherit-/Enderit-Baustab aus Kern, goldener Baustab mehr Haltbarkeit - nicht gewaehlt, spaeter neu besprechen
- [ ] Rucksack-Sortierung (Reihenfolge vorbereitet), sobald eine Sortierfunktion kommt
- [ ] Besitzer-Abnahme: Mega-Handbuecher im Client ansehen und Testzentrale in der Besitzerwelt neu bauen.
- [ ] Besitzer-Abnahme: visuelles Hub-Rendering/echte Clients, lokale Modpack-Kombinationen
- [x] Weitere Ports als Module: riding, qol, visuals, models, dimensions, tweaks, fun, sounds und Money-Links (siehe Welle 25 unten); Abnahmen im Spiel offen.
- [x] Forge 26.3 einschließlich simpledimensions integriert; experimentell/opt-in. Clientabnahme bleibt offen.
- [ ] Besitzer-Abnahme: 26.3-Signalkanaele und Astralgewoelbe im Client; Testzentrale in Besitzerwelt neu bauen.
- [ ] Desktop-/Handy-Sichtprüfung von Wiki und Balancing-Zentrale.
- [ ] Forge: echte Clientdarstellung, Config-Persistenz/optionale Integrationen und spätere Default-Einschaltung abnehmen.
- [ ] Besitzer-Abnahme von Simple Money; NeoForge-Client/alte Spielwelt noch prüfen. Forge 26.3 und andere Linien später im Port-Run.
- [x] TASK FACTS PASS: Texte/Code-Rezepte abgeglichen, DE/EN beider Sprachorte, Generatoren aktuell; check und 1562/1562 Server grün. Kein Gameplay, Push oder Merge; Besitzerabnahmen bleiben oben offen.

## Welle 25 (2026-09-30): Multi-Mod, Forge 26.3, Haertung - Stand Abend
- [x] Neun Module (Fabric + NeoForge 26.3, Forge experimentell opt-in ausser simpledimensions): simplemoney, simpleriding (+ Nautilus, 18 Serveroptionen), simplemodels, simplefun (8 Mechaniken, 4 Tierkoepfe, 26 Optionen), simplevisuals (12 Partikeleffekte, 5 Stufen, 50 Optionen), simplesounds (12 Klanggegenstuecke), simplequalityoflife, simpletweaks (Kompatibilitaet, Claims inaktiv), simpledimensions (Skyblock, Mining, Travel).
- [x] Simple Money verknuepft die anderen Mods: 243 bedingte reine Kaufangebote, serverseitige Preisgrenzen, dauerhafte Kaufgrenzen, kein Rueckkauf.
- [x] SimpleBuilding-Haertung (Boost-Paketbudget, Geschwindigkeitscap, Config-Obergrenzen, Laser-Rezeptfilter), Faktenpass, Sprach-Bridge (tools/voicebridge), Plugin-Registrierung der Module (tools/newmod.py, aitool merge-module).
- [x] Gate auf 7c540a06: 1600/1600 Server (fabric-263 + neoforge-263), 393/393 Integration + alle Modulsuiten, alles gruen.
- [x] Alle Codex-Laeufe sind gemergt; keine offenen Branches mit ungemergter Arbeit (codex-* lokal, Stand in master).
- [ ] Abnahme im Spiel (Client schliessen, Testzentrale in der Besitzerwelt neu bauen): Pulsating-Trim, Astralgewoelbe + Enderit-Pulver, Mega-Handbuecher, Hammer-Tempo (Formel 2/3 oder 3/4 der Pickaxe-Geschwindigkeit?), Attractor/Echolot, Boost und Pads nach der Haertung, Client-Neustart fuer den Architectury-Mixin-Fix.
- [ ] Abnahme neue Module im Spiel: Simple Models (UI, eigenes Modellpack), Simple Fun (Koepfe, Effekte; Glasbruch nur Einzelspieler), Simple Visuals/Simple Sounds (Stufen, Staerke), Simple Quality of Life, Simple Riding (Nautilus, Fahrgrenzen bei Latenz), Simple Dimensions (Portal bauen, reisen, zurueck), Simple Money (Preise, Haendlerpools der 243 Kaufangebote), Forge-Clients und Mehrspieler.
- [x] Entscheidungen vom 2026-09-30 Abend getroffen (naechster Block): Claims zuerst als ausgeschaltetes Feature, Echo-Kompasse nicht migrieren, Dimensionen (Bogen bleibt Standard, Einzelschalter in den Einstellungen), QoL-Haltbarkeitsbonus Standard 1, Sounds an Visuals koppeln, Forge spaeter. Noch offen: Echolot 3 s halten oder Ein-Klick.
- [ ] Sprach-Bridge einrichten (docs/ai/VOICE-HANDS-FREE.md): Tailscale am Handy, `tailscale serve`, Anbieter und Modell waehlen, Headset testen; offen ob ein gesprochener Morgenbericht gewuenscht ist.
- [x] Forge für simpledimensions und Claim-Adapter implementiert. Native Forge-Dialoge ersetzen die inkompatible Cloth-Version; GUI-Abnahme bleibt offen.
- [ ] Weitere Wiki-UX-Ideen (oben); Port-Run 26.2/1.21.11/26.4 erst nach Release-Ankuendigung des Besitzers.

## Besitzer-Entscheidungen 2026-09-30 Abend (Reihenfolge = Prioritaet; fuer die Laptop-Sitzung, Briefs in docs/ai/briefs/next-*.md)
- Orchestrierung aktiv: `codex-next-claims`, danach `codex-next-dimensions` und `codex-next-small` gestartet. Plan/Verifikation: `docs/ai/CODEX-PLAN.md`; offene Fortsetzung: `docs/ai/CODEX-HANDOVER.md`. Noch keine neue Feature-Abnahme oder Gate-Freigabe.
- Fortsetzung 2026-10-01: Claims-Stufen 1–3 auf `7422a3ab` geprüft (36/36 Claims, 70/70 Hammer-Regressionsfälle). `next-claims` bearbeitet Stufe 4; `next-claims-access` parallel Stufen 5–6. Dimensions (`8657e3d2`) und QoL/Sounds (`7faca095`) geprüft und mergebereit. Noch kein Feature-Merge/Push dieser Welle.
- [ ] 1. CLAIMS (als Allererstes): das Claim-System der Quelle (ClaimState, ClaimProtectionHandler, ClaimDeedItem, /claim-Befehle) jetzt portieren, aber als DEAKTIVIERTES Feature: Hauptschalter standardmaessig aus, Stufe fuer Stufe weiter ausbauen und erst nach Fertigstellung freigeben. Anforderungen, Fallen und Testliste: docs/modules/simpletweaks.md Abschnitt "Sicherheitsbefunde und Entscheidungen" (Caps, atomare Persistenz, Rechte an jedem Ziel, Explosion/Feuer/Kolben/Fluessigkeit, Zwei-Spieler-Tests). Brief: docs/ai/briefs/next-claims.md. Danach ein Claim-Adapter fuer Dimensions.
- [x] 2. Alte Echo-Library-Kompasse: NICHT migrieren (entschieden: die neue Loesung ersetzt sie, die alten braucht niemand). Keine Arbeit.
- [x] 3. DIMENSIONS: (a) Standardform bleibt der Bogen (sechs Glowstone-Boegen mit Zusatzlicht); keine Kupfer/Blaueis-Variante in den Configs anbieten. (b) Die vorinstallierten Dimensionen (Skyblock, Mining, Travel) sind einzeln in den EINSTELLUNGEN (Server-Optionen, eigener Reiter im Config-Bildschirm) ein- und ausschaltbar, nicht ueber die Config-Dateien der Dimensionen. (c) SPAETER, erst nach dem gemeinsamen Durchgehen mit dem Besitzer: beliebige erlaubte Portalformen frei konfigurierbar und ueber waehlbare Mechaniken aktivierbar (nicht jetzt beginnen). Brief fuer (a) und (b): docs/ai/briefs/next-dimensions-settings.md.
- [x] 4. QUALITY OF LIFE Haltbarkeitsbonus: gilt auch fuer Mod-Werkzeuge (SimpleBuilding und andere), aber der Standard ist 1 (kein Bonus); per Config erhoehbar (serverseitig, harte Obergrenze). Brief: docs/ai/briefs/next-small.md.
- [x] 5. SOUNDS: Intensitaet an die Stufe von Simple Visuals koppeln (Off/Subtle/Normal/Strong/Maximum); ohne Visuals gilt eine eigene Einstellung. Brief: docs/ai/briefs/next-small.md.
- [x] 6. FORGE spaeter: Forge 26.3 auch fuer Dimensions, Cloth-Dialog auf Forge und die Frage der Standardaktivierung kommen nach 1 bis 5.
- [ ] Noch offen: Echolot 3 Sekunden halten oder Ein-Klick? Morgenbericht der Sprach-Bridge ja oder nein?

## Orchestrator-Abschluss 2026-10-01

- [x] Claims-Stufen 1-6 integriert, Hauptschalter AUS. Stage 4 bleibt teilweise
  offen; keine Freigabe. Grenzen: modules/simpletweaks/CLAIMS-STAGE4.md.
- [x] Dimensions: sechs Glowstone-Boegen, drei persistente Serverschalter im
  eigenen Reiter; Kupfer/Blaueis-Preset entfernt. Freie Formen spaeter.
- [x] QoL: Standard 1, Cap 1.5, auch Mod-Werkzeuge; bestehende Werte erhalten.
- [x] Sounds folgt Visuals optional, eigene Stufe ohne Anbieter.
- [x] Forge integriert mit 39fd85b1 (Worker 6786b545): Dimensions/Claims-Adapter,
  native Dialoge, validierte Persistenz und korrigierte Laufzeitverpackung.
  Forge bleibt experimentell/opt-in; kein passendes Cloth-Forge-Artefakt.
- [x] Default-/Forge-Worker-check gruen; 1039 verschiedene Forge-Faelle belegt,
  finale Drei-Loader-Regression 264/264, Wiederholung 88/88, Settings bytegleich.
  Testzentralen in isolierten Welten neu gebaut, SimpleBuilding-Abdeckung komplett.
- Abschlussgate der zusammengefuehrten SHA: .ai-runs/final-full-gate.log.
  Push ausschliesslich fuer deren GREEN-SHA; Remote-Bestaetigung im Abschlussbericht.
- [ ] Claims-Schutzluecken, Client-/Audio-/GUI-Abnahmen und Besitzerwelt bleiben
  offen. Besitzer-Client laeuft; keine Clients gestartet oder gestoppt.
  Konkrete Fortsetzung: docs/ai/CODEX-HANDOVER.md.
- Pruefbelege: docs/ai/CODEX-VERIFICATION-2026-10-01.md und
  docs/FORGE-FOLLOWUP-RESULTS.md. Erstes rotes Gesamtgate und Korrekturen erhalten.

## B7: End redstone rename (26.3)
- [x] Renamed both End signal powders to Astral Redstone / Nihil Redstone, yield two per recipe, and preserve item and block aliases. Verified on claude-b7: compile targets, 26.3 datagen, wiki, full check, End systems 6/6, migration 1/1, and test centre rebuild/coverage 4/4. No port or push.

## Queue-Ende (Besitzer 2026-10-02): Rezeptbuch in Befiederungs- und Schmiedetisch
- [ ] Befiederungstisch-GUI wie der Werkbank-Bildschirm: das Rezeptbuch-Symbol an exakt derselben Stelle wie bei der Werkbank, dieselbe Bedienung (Buch öffnet die Rezeptliste links, Klick legt die Teile ein), nur dass man Spitze/Schaft/Befiederung wählt; Layout wie die Werkbank minus der fehlenden Felder.
- [ ] In den drei Teil-Slots Hintergrund-Silhouetten wie bei den Rüstungsslots (zeigen, was hineingehört: Spitze, Schaft, Befiederung).
- [ ] Titel über den drei Slots: „Fletching“ / „Befiederung“ statt „Fletching Table“.
- [ ] Schmiedetisch: statt des Knopfs für die Besatz-Resonanz-Vorschau ein anklickbares Rezeptbuch (über dem dritten Slot gerendert), das wie bei der Werkbank die möglichen Ergebnisse zeigt und einlegt.

## Queue-Ende (Besitzer 2026-10-02): seltene verstärkte Shulker in End-Städten
- [ ] Spawn: In End-Städten wird ein Shulker sehr selten verstärkt (ca. 2 %) oder zum Enderit-Shulker (ca. 0,5 %); serverseitig konfigurierbar mit Obergrenze.
- [ ] Leben: verstärkt 1,5×, Enderit 3× (oder mehr, wenn es spielerisch nötig ist). Die Hülle zeigt die Stufe.
- [ ] Drops: 0–2 Schalen ihres eigenen Typs.
- [ ] Neue Items: Verstärkte, Netherit- und Enderit-Shulkerschale.
- [ ] Schalen in der Welt aufwerten (wie die anderen In-World-Transformationen): Schale auf den Boden legen und mit einem Nugget rechtsklicken. Eisen/verstärkt → verstärkte Schale, Netherit → Netherit-Schale, Enderit → Enderit-Schale; genau ein Nugget pro Schale.
- [ ] Rezept der Shulkerkisten-Stufen: Kupfertruhe + eine aufgewertete Schale + eine normale Shulkerschale.
- [x] Geklärt (Besitzer 2026-10-02): Auch die Netherit-Schale entsteht in der Welt mit einem Netherit-Nugget. Das neue Rezept kommt zu den bestehenden Stufenrezepten dazu.

## Queue-Ende (Besitzer 2026-10-02 abends, mit Screenshots)
- [ ] Guide-Buch: Die Tabs haben eine hässliche graue Box als Overlay (Screenshot: linke und rechte Tab-Leiste) → entfernen bzw. sauber zeichnen.
- [ ] Guide-Buch im Kreativmodus: gesperrter Tab zeigt einen Knopf „Trotzdem freischalten“.
- [x] Astral/Nihil-Redstone soll sich wie Vanilla-Redstone verhalten und dieselben Texturarten haben (Punkt, Linie, Verbindungen – Multipart wie Redstone-Draht). Die Pulver-Textur sieht im Spiel falsch aus (Screenshot: großes, verpixeltes violettes Muster).
  Erledigt auf claude-astral: Ursache war das Modell (eine 16x16-Ebene mit dem Kreuzbild statt Multipart). Jetzt `EndSignalPowderBlock` mit Seiten none/side/up, Punkt/Linie/Kreuz, Wand hoch, Signal über Stufen, nur eigener Kanal; Item = umgefärbter Redstone-Haufen. Plan/Details: docs/ai/PLAN-ASTRAL-NIHIL-REDSTONE-2026-10-02.md. Abnahme im Client offen.
- [x] Texturen von Nihil-/Astral-Schalter und -Lampe sind kaputt → reparieren.
  Erledigt auf claude-astral: Schalter = flache Platte (statt schwebender Ebene), Lampe = voller Würfel wie die Redstone-Lampe, Items zeigen das Blockmodell. Abnahme im Client offen.
- [ ] Neue Blöcke: Astral-Kolben (drückt) und Nihil-Kolben (zieht). Mit Signal wird jeder Block im Abstand 1 in alle 6 Richtungen gleichzeitig um genau 1 Block gedrückt bzw. gezogen. Nie 2 Blöcke hintereinander in derselben Richtung. Erst als Konzept/Plan.
  Konzept fertig: docs/ai/PLAN-ASTRAL-KOLBEN-2026-10-02.md (offene Besitzerfragen am Ende). Umsetzung wartet auf Freigabe.
- [ ] Bessere Truhen statt normaler Loot-Truhen, je 1 % Chance:
  - Verstärkte Truhe in der Festung (Stronghold), Netherit-Truhe in der Bastion oder der Netherfestung, Enderit-Truhe in der End-Stadt oder auf dem End-Schiff.
  - Inhalt: doppelter oder höherstufiger Loot.
  - Doppeltruhen: Würfelt die erste Hälfte die bessere Truhe, wird die zweite Hälfte mit 1 % neu gewürfelt. Klappt das, werden beide besser, sonst bleiben beide normale Truhen.
- [ ] Enderit-Nugget-Textur passend zur Barren-Textur und zu Vanilla überarbeiten (10 Vorschläge).
- [ ] Weisheitserz-Textur etwas kleiner und langsamer animieren.
- [ ] Die Advancement-Seite sieht falsch aus (Screenshot: Pink-Schwarz-Fehltextur als Hintergrund im Tab „The Two Shelves“) → Hintergrund-Textur reparieren.
- [ ] 10 Alternativ-Vorschläge für die eigenen Besatzvorlagen (Glowing, Pulsating, Emitting).
- [ ] 10 Vorschläge für Netherit-Apfel, Enderit-Apfel und Netherit-/Enderit-Karotte.
- [ ] Pfeile, die einen Mob getroffen haben, sollen wieder aufsammelbar sein, am besten wenn er gestorben ist (teure Pfeile lohnen sich dann).
- [ ] Das Rezept des Spawn-Elytra-Pads wird nicht angezeigt (JEI zeigt nur die Info).
- [ ] Rezepte der Trank-Pads:
  - Verstärktes Trank-Pad: Netherit-Aufwertung + Netherit-Druckplatte.
  - Infundiertes Trank-Pad 3: Enderit-Aufwertung + Enderit-Druckplatte.
- [ ] Das Rezept des Flypads wird auch nicht angezeigt. Höhe je Stufe = Breite der Grundfläche × 2.
- [ ] Shulkerkopf: Das 3D-Modell ist nur im Inventar zu groß und wird abgeschnitten (Screenshot).
- [ ] Silberfischkopf viel kleiner: im Inventar, auf dem Kopf und abgestellt.
- [ ] Guide-Buch-Texturen überarbeiten (10 Vorschläge).
- [ ] Simple Money: 10 Textur-Vorschläge für Special Fiber, 10 für Resin Fiber.
- [ ] Simple Money: Schmelzzeiten der Geld-Teile erhöhen, damit es in SimpleBuilding-Welten balanciert ist.
- [ ] Zusätzlicher Weg zu Diamant-Kieseln (In-World): Fällt ein Amboss auf einen Diamantblock, entstehen Diamant-Kiesel.
- [ ] Auto-Schmied analog zum Autocrafter: automatisiert den Schmiedetisch.
- [ ] Wiki: Auf jeder Item-Seite beim Rezept die benötigten Grundmaterialien insgesamt auflisten (ab Barren, Holzstämmen, Zuckerrohr, Wachs, Bruchstein …), damit klar ist, wie viel Rohmaterial ein Item kostet. Beim Geldschein sein Wert.
- [ ] Strohpuppe / Trainingspuppe:
  - Rezept: Rüstungsständer + Strohballen ergibt einen Stroh-Rüstungsständer.
  - Mit aufgesetztem geschnitzten Kürbis wird daraus eine Trainingspuppe mit gutem Minecraft-Namen. Sie ist unzerstörbar, außer beim Abbauen im Schleichen.
  - Sie zeigt allen Schaden an, auch kritische Treffer.
  - Je nach aufgesetztem Kopf zeigt sie den Schaden gegen diese Mob-Art (Gliederfüßer, Untote, Endermen …).
  - Man kann ihr Rüstung anziehen.
  - Bestehende Konzepte im Internet recherchieren und vervollständigen.
- [ ] Testzentrale: eine Pfeil-Station, an der jeder Pfeil getestet werden kann.
- [x] Schachbrett-Blöcke zusätzlich aus poliertem Astralit, poliertem Nihilit und Enderquarz (mit der Textur der polierten Variante; Screenshot der Schachbrett-Zeile).
  Erledigt auf claude-astral: polished_astralit_checker, polished_nihilith_checker, polished_ender_quartz_checker (Rezept 2 polierter Block + 2 Quarzblock → 4). Vorschau previews/polierte-schachbretter-vorschau.png.

## Besitzer 2026-10-02 (Nachtrag)

- [ ] Geschwindigkeitsmesser: läuft im Autowalk auch im Inventar und in nicht pausierenden UIs (Chat usw.) weiter.
- [ ] Shulkerkiste vorerst nicht verzauberbar machen.
- [ ] Erzdetektor: Kompassnadel wirkt nicht zentriert – Animation/Nadel-Frames prüfen und zentrieren.
- [ ] Resonanzstab: soll auch Entities anzünden bzw. scannen können.
- [ ] Mod-Items zusätzlich an den richtigen Stellen in die Vanilla-Kreativtabs einsortieren; Simple-Building-Tabs weiter aufteilen (Werkzeuge, Waffen, Rüstung usw. wie in Vanilla).
- [ ] Trank „Crafty Shulker“: Effekt – bei Treffer an eine sichere Stelle in der Nähe teleportieren; braubar mit Shulkerkopf, analog zu den anderen Tränken/Effekten.
