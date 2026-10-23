# Audit Besitzer-Wünsche 17.–23.10.2026 (Stand 23.10.2026, a-audit23)

Frage des Besitzers (23.10.): Sind in der Queue alle Wünsche enthalten, die er generell gegeben hat?
Basis: Branch `a-audit23` = `claude-wave1` @ `d62da08c9`. Gelesen: alle Nutzer-Einträge seit 17.10. aus den Transkripten
beider Sitzungen (A und B; ohne Tool-Ergebnisse, System-/Agenten-/Hook-Texte), dazu `.claude/QUEUE.md`,
`.claude/QUEUE-ARCHIV.md`, `docs/ai/PLAN-*`/`KONZEPT-*`, Memory. Vorgänger: `AUDIT-BESITZER-WUENSCHE-2026-10-14.md`,
Queue-Block „Verwaiste TODOs 20.10.“ (Archiv). Rohauszüge liegen nur außerhalb des Repos.
Geprüft wurde per Lesen (Queue-Zeilen, Pläne, Config-Code); nichts gebaut, nichts im Spiel angesehen.

## Kurzfazit
115 Nutzer-Einträge; davon 12 Weiterleitungen zwischen den Sitzungen und 1 automatische Fortsetzung → **102 Besitzer-Nachrichten**
(etwa 30 davon reine Status-/Betriebsfragen). Zerlegt in **221 Einzelwünsche/Entscheidungen** (Betriebspunkte mitgezählt):

| Status | Bedeutung | Anzahl |
|---|---|---|
| erledigt | in Queue/Archiv abgehakt mit Beleg (Branch/Plan) bzw. Betrieb/Antwort erledigt; Client-Sicht oft noch offen | 152 |
| in Queue | offen oder in Arbeit, mit ID in `.claude/QUEUE.md` | 56 |
| FEHLT | weder offen in der Queue noch erledigt → neu als AUD-1…AUD-7 | 7 |
| überholt | durch spätere Besitzer-Aussage ersetzt (2 eigene Zeilen, 4 als Zusatz an erledigten Zeilen) | 6 |
| nicht auswertbar | nur Bild bzw. unklar, dazu diese Audit-Frage selbst | 4 |

Ergebnis: Die Wünsche vom 22.–23.10. stehen vollständig in der Queue (SP22, TZ22, TX22, PJ23, B23, W23). Lücken sind
Reste älterer Tage und zwei neue Sound-Aufträge vom 23.10. morgens, deren Ergebnisse zwar erzeugt wurden, aber ohne Queue-Zeile
(Klang-Kandidaten und Musikplatten aus ElevenLabs). Dazu ein Widerspruch (TEX21) und eine nie umgesetzte Wahl (Nebelwand).

## Fehlende Punkte (→ Queue „Audit 23.10. – fehlende Besitzer-Wünsche“)
| ID | Datum/Quelle | Was fehlt |
|---|---|---|
| AUD-1 | 23.10. A | Eigene Klänge „zu grell, synthetisch, zu hell, ohne Tiefe“ → mit ElevenLabs neu. Kandidaten für den legendären Klang (s01–s11) sind erzeugt, aber weder Wahl noch Einbau stehen in der Queue; SP22-2-Wahl fehlt im Überblick (1). Andere synthetische Klänge (Redstone-Linien, Verstärker) nicht geprüft. |
| AUD-2 | 23.10. A | Weitere Musikplatten-Vorschläge (episch/„gehen ab“, Vanilla-Stil): m01–m06 erzeugt, Wahl + Einbau (Simple Music, Fundorte) ohne Queue-Zeile. |
| AUD-3 | 17.10. B (ENTSCHEIDUNG17) | End-Wald-Nebel: Wahl „A, B nur falls A nicht reicht“. Nebelwand B (`forestFogWall`) ist weiter Standard AN (SimpleEndConfig), Rückfrage blieb unbeantwortet. |
| AUD-4 | 21.10. B | Besitzer: „TEX21 schon abgearbeitet“; Queue führt `TEX21 S/D/H` weiter als Wahl offen → Wahl ermitteln, Zeile schließen. |
| AUD-5 | 17.10. A | Lichtscheiben + Schwefellichter: Ziel-Sub-Mod „später nachbearbeiten“ – nur als erledigte Entscheidung archiviert. |
| AUD-6 | 18.10. A | Bild-Bauplan (MC-Paint-Idee): Stufe 3 Bild als Karte/Kartenwand (Simple Maps) – im Archiv als offen vermerkt, keine Zeile. |
| AUD-7 | 19.10. A | Notenblatt/Upload-Reste aus b-music20: Raster-Editor E2/E3 und Client-Schalter „Fremde Musik laden“ – im Archiv offen vermerkt, keine Zeile. |

## Überholt (später korrigiert)
| Datum | Wunsch | ersetzt durch |
|---|---|---|
| 17.10. A | Formsäge schneidet Stein UND Holz, ersetzt die Steinsäge | 17.10. 22:05: Octets + Holzvarianten exklusiv in der Formsäge; 21.10. Umbenennung „Shapecutter“ |
| 17.10. B | Enderit „nugget“ überall | 19.10.: wie Vanilla → „Klumpen“ bleibt |
| 18.10. A | Formsäge-Texturen mit Kupfer-Highlights | 21.10.: Metallsockel, Blatt Vanilla poliert (SC21 S2) |
| 21.10. B | Netherherz 3D mit vielen Adern | 22.10.: Form wie Icon, nur zwei angedeutete Adern |
| 22.10. B | SP22-1 Schleich-Abbau macht jeden Block zur Achtel-Zelle | 23.10.: nur mit Hammer (B23-M8) |
| 22.10. B | TZ22-7 Druckplatten im Rahmen schräg wie in der Hand | 23.10.: flach von oben (FR23), dann W23-5 reine Draufsicht |

## Alle Einzelwünsche
Sitzung = wo der Besitzer es schrieb. „erledigt“ heißt Code/Asset laut Queue-Beleg da; Client-Sicht ist oft noch offen (Strom 2).

### 17.10.
| Datum | S | Wunsch | Status |
|---|---|---|---|
| 17.10. | B | Queue weiter abarbeiten, mehr parallel | erledigt (Arbeitsweise) |
| 17.10. | B | ENTSCHEIDUNG17: Rotator → Transposer/Transponierer | erledigt (Archiv b-names) |
| 17.10. | B | ENTSCHEIDUNG17: Wald-Nebel A, B nur falls nötig | **FEHLT (AUD-3)** |
| 17.10. | B | ENTSCHEIDUNG17: platzierte Items Schritt 3 einbauen | erledigt (Archiv, A 16.10.) |
| 17.10. | B | ENTSCHEIDUNG17: Enderit „nugget“ | überholt (19.10. Klumpen) |
| 17.10. | B | ENTSCHEIDUNG17: Blazing Obsidian Konzept Textur/Verhalten | erledigt (b-lohen21) |
| 17.10. | B | ENTSCHEIDUNG17: Bild vom 14.10. abhaken | erledigt |
| 17.10. | A | Howler-Klänge prüfen, weniger synthetisch | erledigt (howler5) + in Queue (HOW-2) |
| 17.10. | A | Octet-Regel im Repo als Prinzip; Prinzipien global + je Mod | erledigt (docs/ai/PRINZIPIEN.md Teil 1/2, A4) |
| 17.10. | A | Alle Blöcke mit Treppe/Stufe bekommen Octets (auch Simple Blocks) | erledigt (b-octetrule, claude-octets2) |
| 17.10. | A | Simple Blocks hostet alle Baublöcke ohne Fachort | erledigt (b-blocksmove) |
| 17.10. | A | Ein Kreativtab „Simple Blocks“ statt zwei | erledigt (b-tabt1) |
| 17.10. | A | Mod-Aufteilung als Liste ausgeben | erledigt (Antwort; UEBERSICHT-MODS-2026-10-19) |
| 17.10. | A | Simple Tweaks Server-Tweaks: Ideenliste zum Auswählen | erledigt (T1–T34) |
| 17.10. | A | Simple Optimization als eigene Super-Mod, Vorrang fremder Mods, Schalter je Bereich | erledigt (Konzept, opt1, opt2) / in Queue (OPT-e4) |
| 17.10. | A | Optimization: Messungen fair je Bereich, Clients mit/ohne Fremd-Mods, Bessere bevorzugen | in Queue (END-1, END-2) |
| 17.10. | A | Keine Octets für Naturstufen | erledigt (Archiv) |
| 17.10. | A | Lichtscheiben/Schwefellichter nicht in Simple Blocks | erledigt (Archiv) |
| 17.10. | A | Sub-Mod für Lichtscheiben/Schwefellichter später klären | **FEHLT (AUD-5)** |
| 17.10. | A | Formsäge (fortgeschrittener Cutter), Holz-Schnittrezepte, Rezept/Textur nah an Vanilla | erledigt (claude-formsaege) |
| 17.10. | A | Formsäge schneidet Stein und Holz, ersetzt Steinsäge | überholt (exklusiv, Shapecutter) |
| 17.10. | A | Tweaks-Auswahl ja/vielleicht/nein + Defaults | erledigt (tweaks-admin/-play) |
| 17.10. | A | T23 Tag/Nacht-Abstimmung Standard aus | erledigt (tweaks-play) |
| 17.10. | A | /invsee-artige Fenster sichtbar, Admin kann nehmen (Vorbild invsee/InvRestore) | erledigt (tweaks-admin) |
| 17.10. | A | Alles nur per Befehl Wirkende Standard AN | erledigt (Regel im Archiv) |
| 17.10. | A | Alle Tweaks-Befehle auch unter /simpletweaks | erledigt (tweaks-cmd) |
| 17.10. | A | CT-Platte erweitern, sonst bereinigen | in Queue (OPS-1) |
| 17.10. | A | Formsäge = Steinsäge mit Kupfer-Highlights, Kupfer im Rezept | erledigt (claude-formsaege), später überholt (Shapecutter-Textur) |
| 17.10. | A | Upgrade Steinsäge: ohne SB per Axt, mit SB per Hammer + 4 Eisennuggets | erledigt (claude-formsaege) |
| 17.10. | A | Admin-Fenster abgenommen | erledigt |
| 17.10. | A | Polierter Endstein: Treppe, Stufe, Mauer, Octet | erledigt (b-octetrule) |
| 17.10. | A | Fehlende-Textur-Items selbst per Screenshot-Audit suchen; Color Brush fixen | erledigt (b-colorbrush, b-texfix20) |
| 17.10. | A | Sitzung B prüfen und neu anstoßen | erledigt (Betrieb) |
| 17.10. | A | Octets und Holzvarianten exklusiv in der Formsäge | erledigt (claude-octets2) |
| 17.10. | A | Tweaks Anti-X-Ray (Standard an), Freecam-Durchfliegen-Verbot (Standard aus), weitere Schutzideen | erledigt (tweaks-anticheat) |
| 17.10. | A | Anti-Cheat: schnelles Abbauen/Reichweite der eigenen Mods berücksichtigen | erledigt (PLAN-TWEAKS-PROTECT §actionChecks) |
| 17.10. | A | Modpack-Liste: Konfig-Mods raus, Mods recherchieren | erledigt (MODPACK-BESITZER-2026-10-17) |
| 17.10. | A | Optimization-Konzept: Empfehlungen übernehmen, Tests auf dem Server | erledigt (Archiv) |
| 17.10. | A | Liste: welche Modpack-Mods sind nicht in Simple Mods abgebildet | erledigt (Antwort; Folgewünsche 18.10.) |

### 18.–19.10.
| Datum | S | Wunsch | Status |
|---|---|---|---|
| 18.10. | A | Formsäge: Kupferpixel mit Übergang zur Vanilla-Textur | erledigt (feinschliff), später überholt |
| 18.10. | A | Soulbound und Fluch des Verschwindens exklusiv | erledigt (feinschliff) |
| 18.10. | A | Howler-Klang: mehrere ähnliche Varianten, Vanilla-nah, Vorschläge | erledigt (howler5) |
| 18.10. | A | Mit Sitzung B weiter die Queue abarbeiten | erledigt (Betrieb) |
| 18.10. | A | Better Item Frames übernehmen | erledigt (b-itemframes) |
| 18.10. | A | Shulker- und Endertruhen-Tooltip | erledigt (b-tooltips, a-tooltips3) |
| 18.10. | A | MC-Paint-artiger Bild-Bauplan (Code-Import, Bild-Upload mit Umwandlung) | erledigt Stufe 1+2 (b-picture) |
| 18.10. | A | Bild-Bauplan als Karte (Stufe 3) | **FEHLT (AUD-6)** |
| 18.10. | A | Bundles Beyond übernehmen | erledigt (b-bundles) |
| 18.10. | A | Simple Maps Webkarte, wenn möglich 3D | in Queue (WEB: Stufen 1–3 da, 4 offen) |
| 18.10. | A | MineTracer-Vorteile in T10 übernehmen | erledigt (b-t10log) |
| 18.10. | A | Formsäge und Howler-Klänge abgenommen | erledigt |
| 19.10. | A | Sitzung B (Max-Tarif) voll auslasten | erledigt (Betrieb) |
| 19.10. | A | A nur orchestrieren, B implementiert | erledigt (Betrieb) |
| 19.10. | A | Tabelle Super-Mods / mit Sub-Mods, offene Features, Queue-Punkte zugeordnet + Restliste | erledigt (UEBERSICHT-MODS-2026-10-19, Wiki „Alle Mods“) |
| 19.10. | A | Weiterarbeiten während Compile/Gates; API-Guthaben nutzen | erledigt (Betrieb) |
| 19.10. | A | Enderit-Klumpen wie Vanilla | erledigt (Archiv D-05) |
| 19.10. | A | Easter Egg: Herobrine-Kopf per Ritual | erledigt (b-herobrine20) |
| 19.10. | A | Notenblatt wie Bauplan + Jukebox-Variante (Loop, Truhe oben wechselt) | erledigt (b-music20, Orchestrion) |
| 19.10. | A | Notenblatt-Reste Raster-Editor, Client-Schalter fremde Musik | **FEHLT (AUD-7)** |
| 19.10. | A | Eigene Musikplatte per Datei-Upload, Loop mit Truhe oben | erledigt (b-music20 B1–B3) |
| 19.10. | A | Fehlende Texturen (Schachmuster) beheben, Tab-Audit | erledigt (b-texfix20) |
| 19.10. | A | Biome Brush: Schleichen = ein Block | erledigt (b-biomebrush) |
| 19.10. | A | Karte: geladene Chunks nur eigene/Chunkloader | erledigt (b-maps20) |
| 19.10. | A | Claim-Urkunde Textur wie Blaupause | erledigt (b-deedtex, DEED20 C) |
| 19.10. | A | Shops mit Enderperle, Reiter eigene/fremde, Fern-Shop, Lager nur per Handel, Missbrauch ausbalancieren | erledigt (b-shops20) |
| 19.10. | A | Karten anderer Dimensionen: Rauschen dezenter, nur unentdeckt, Entdecktes sichtbar | erledigt (b-maps20) |
| 19.10. | A | Shops auf Karten mit Geldschein | erledigt (b-maps20) |

### 20.10.
| Datum | S | Wunsch | Status |
|---|---|---|---|
| 20.10. | B | KONZ20 (Shops/Musik/Upload/Herobrine), Herobrine-Kopf mit Fähigkeit | erledigt (b-shops20, b-music20, b-herobrine20) |
| 20.10. | B | DEED20 C | erledigt (b-deedtex) |
| 20.10. | A | Gate: nur rote Tests wiederholen statt alles | erledigt (b-gatefast) |
| 20.10. | A | Speedlines-Tempo nach Geschwindigkeit | erledigt (b-speedlines) |
| 20.10. | A | Wiki „Alle Mods“ + jede Super-Mod im Dropdown | erledigt (b-wikiallmods) |
| 20.10. | A | Freigaben automatisch machen | erledigt (Betrieb, Memory) |
| 20.10. | A | Verwaiste TODOs auflisten | erledigt (b-orphtodos) |
| 20.10. | A | Platte vergrößern | in Queue (OPS-1) |
| 20.10. | B | TEX20: Notenblatt wie Vanilla/Blaupause, Platten wie Vanilla, Herobrine wie Vanilla, nichts erfinden | in Queue (TEX21) |
| 20.10. | B | Hopper-UI Rand unten | erledigt (b-ui20) |
| 20.10. | B | Nihil-Gewölbe Hintergrundfarbe | erledigt (b-ui20, UI20 ok) |
| 20.10. | B | Crafter/Smither/Autocrafter: Punktsymbol weg, Zustand im Pfeil | erledigt (b-ui20) |
| 20.10. | A | (nur Bild, ohne Text) | nicht auswertbar |

### 21.10.
| Datum | S | Wunsch | Status |
|---|---|---|---|
| 21.10. | B | Freigaben selbst einrichten | erledigt (Betrieb) |
| 21.10. | B | Taschen-Jukebox (Bedrock-Vorbild), färbbar, Diamant statt Rohling | erledigt (b-pocketjuke) |
| 21.10. | B | (Bild) „was ist damit?“ | nicht auswertbar |
| 21.10. | B | Orchestrion-Kupfermotiv symmetrisch | erledigt (b-music21) |
| 21.10. | B | Noten im Notenblatt 1 px tiefer | erledigt (b-music21) |
| 21.10. | B | Pixeleditor im Wiki-Dev mit Ebenen, Resprite-Analyse | erledigt (b-pixeleditor) |
| 21.10. | B | Prinzip: Texturen immer in Ebenen | erledigt (PRINZIPIEN A8, b-layers1) |
| 21.10. | B | Prinzip: Texturen Vanilla-nah | erledigt (PRINZIPIEN A8) |
| 21.10. | B | Taschen-Jukebox/Rohling an Referenz/Vanilla angleichen | erledigt (b-music21, MU21) |
| 21.10. | B | Plattenpresse: Notenblatt oder Upload auf Rohling, Blatt bleibt | erledigt (b-music21) |
| 21.10. | B | Notenblatt am Kartentisch kopierbar | erledigt (b-music21) |
| 21.10. | B | Blockade (Freigabe) erlauben | erledigt (Betrieb) |
| 21.10. | B | Neue Jukebox-Textur aus 26.4 für alle Versionen | erledigt (b-jukebox264) |
| 21.10. | B | Verzauberungs-Ansicht im Wiki wie im Spiel | erledigt (b-wikiench) |
| 21.10. | B | Alle Mods mit 16×16-Pixel-Icon | erledigt (b-modicons) |
| 21.10. | B | Resonanz-Tooltip einklappen (Shift), gemeinsamer Baustein | erledigt (b-ui21) |
| 21.10. | B | Mob-Kopf-Platz wie die anderen Plätze, Spielerkopf-Motiv | erledigt (b-ui21) |
| 21.10. | B | Strong-Upgrade nur vervielfältigbar, Vorlage von Wither 2 (Netherherz), Wither 3 Nether-Juwel, Drops platziert/explosionsfest, Splitter vom Vanilla-Wither, 9 Splitter = Upgrade | erledigt (b-wither21) |
| 21.10. | B | Biome Brush in Simple Tools, neues Rezept | erledigt (b-wither21) |
| 21.10. | B | IP von sb-test auf freie Adresse ab 200 | in Queue (OPS-2) |
| 21.10. | B | MU21 (R=B, P=A, D=B, runde Platten platziert) | erledigt (b-mu21) |
| 21.10. | B | Pixeleditor-Demo verfeinern, gute Icons | erledigt (b-pixeleditor2) |
| 21.10. | B | Formsäge → Shapecutter, UI wie Steinsäge, Metallsockel, Blatt Vanilla poliert | erledigt (b-shapecutter, SC21 S2) |
| 21.10. | B | Redstone-Lichtscheiben-Items mit Regenbogenmuster, „Pane“ im Namen, 3D-Item | erledigt (b-panes21) |
| 21.10. | B | Tragbare Jukebox 3D-hafter, platzierbar, platziert abspielbar | erledigt (b-pjuke21) |
| 21.10. | B | Seltenheitsstufen über Vanilla (rot/schwarz für verfluchte, besondere mit Glanz) | erledigt (b-rarity21) |
| 21.10. | B | Netherherz animiert (gold→blaue Adern), Juwel analog, „Red Nether Gem“ | erledigt (b-rarity21) |
| 21.10. | B | Queue als Artefakt zeigen; Auswahlen anschaulich | erledigt (Betrieb) |
| 21.10. | B | EH21 Easter-Köpfe inkl. Riesen-Zombie/Killerkaninchen/Riesen-Kuh-Vorgaben | erledigt (b-eh21) |
| 21.10. | B | Verbotene Seltenheit wie Feuer animiert, andere edler | erledigt (b-rarity21b) |
| 21.10. | B | Netherherz 3D herzförmiger | erledigt (b-rarity21b), Form 22.10. präzisiert |
| 21.10. | B | Wayfinder-Karte im Rahmen: Ansicht speichern mit doppelter Bestätigung, Bereich zeigen, Codefeld, Stapelgröße 1 | erledigt (b-framemaps22) |
| 21.10. | B | Shapecutter S2 poliert | erledigt (b-shapecutter) |
| 21.10. | B | SM21 a–f Simple-Models-UX | erledigt (b-models21) |
| 21.10. | B | MU21 M (Simple Music als Sub-Mod) | erledigt (b-musicsub22) |
| 21.10. | B | T12 Neue-Spieler-Schutz, Standard 5 min, einstellbar | erledigt (b-t12pvp) |
| 21.10. | B | WI OK (Wither-Texturen) | erledigt (b-icons21b) |
| 21.10. | B | HV E (Gate-Skript mit 3 Slots) | in Queue (OPS-2) |
| 21.10. | B | PR J (Production = master per grünem Gate) | erledigt (Betrieb) |
| 21.10. | B | Bestätigungen automatisch freigeben | erledigt (Memory „Rechte ohne Rückfrage“) |
| 21.10. | B | Proxmox-Schritte (Betrieb) | erledigt (Betrieb) |
| 21.10. | B | Nachts selbstständig Queue fertig, danach Wiki-Seite überarbeiten | in Queue (TX22-5/a-wiki22, Strom 1) |
| 21.10. | B | „tex21 schon abgearbeitet“ | **FEHLT (AUD-4, Widerspruch zur Queue)** |

### 22.10.
| Datum | S | Wunsch | Status |
|---|---|---|---|
| 22.10. | B | Mod-Icons: Hintergründe später abstimmen, 8 neu | in Queue (END-3) |
| 22.10. | B | SP21 B | erledigt (b-icons21b) |
| 22.10. | B | Köpfe ok, Technoblade wie sein Skin | erledigt (b-eh21) |
| 22.10. | B | Pane-Namen überall | erledigt (a-docs22) |
| 22.10. | B | Seltenheit/Netherherz wie empfohlen, Herz wie Icon in 3D, weniger Adern | erledigt (b-rarity21b) |
| 22.10. | B | Sitzung A anstoßen, Kontingent nutzen | erledigt (Betrieb) |
| 22.10. | B | TZ22-1 Testzentrale vollständig | erledigt (b-tz22a) |
| 22.10. | B | TZ22-2 weniger Item-Rahmen | erledigt (b-tz22a) |
| 22.10. | B | TZ22-3 alle Strukturen inkl. Dorfhäuser/End-Strukturen, gruppiert | erledigt (a-tz22b) |
| 22.10. | B | TZ22-4 Teilbau per Befehl | erledigt (a-tz22b) |
| 22.10. | B | TZ22-5 kompaktes Layout, niedrigere Wände | erledigt (b-tz22a) |
| 22.10. | B | TZ22-6 Astral-/Nihil-Schienen richtig testen | erledigt (a-tz22c) |
| 22.10. | B | TZ22-7 Druckplatten im Rahmen wie in der Hand, Client-Schalter | erledigt, Optik überholt (FR23/W23-5) |
| 22.10. | B | TZ22-8 veraltete Umwandlungs-Schilder | erledigt (a-tz22c) |
| 22.10. | B | Testzentrale neu bauen/Rundgang | in Queue (TZ22-9) |
| 22.10. | B | Queue sortieren | erledigt (a-queue23) |
| 22.10. | B | SP22-1 Achtel ↔ Vollblock | erledigt (b-octfull22), Teil überholt (B23-M8) |
| 22.10. | B | SP22-2 legendärer Klang + Effekt in simplelib | erledigt (a-legend22); Wahl neuer Klang in Queue (SP22-2) |
| 22.10. | B | SP22-3 Death-Vault-UI | erledigt (a-sp22ui) |
| 22.10. | B | SP22-4 Items auf Crafting-Storage mit Höhe | erledigt (a-sp22ui) |
| 22.10. | B | SP22-5 eigene Klänge Redstone/Lampe/Kolben | erledigt (b-redsound22) |
| 22.10. | B | SP22-6 Tiegel-UI mehr Platz unten, Trapez | erledigt (a-crucui22) |
| 22.10. | B | SP22-7 Verkaufslager nur am Verkaufsblock, gerichtet; Vitrine frei, Glasdeckel | erledigt (b-shops22) |
| 22.10. | B | SP22-8 Claims-Umbau (Karte, Urkunde, Kartentisch, Reiter, Credits) | erledigt (Bündel 49), Rest in Queue (SP22-8) |
| 22.10. | B | SP22-9 Strong Conduit größerer Bereich | erledigt (a-beacons22) |
| 22.10. | B | SP22-10 Leuchtfeuer 3 Kräfte je Stufe, Material-Reichweite, Anzeige | erledigt (a-beacons22) |
| 22.10. | B | SP22-11 Mob-Kopf wie Helm tauschen | erledigt (b-octfull22) |
| 22.10. | B | Sitzung B nur Token-sparend, Übergabe an A planen | erledigt (Betrieb) |
| 22.10. | A | Rest-Kontingent nutzen, parallele Sitzungen | erledigt (Betrieb) |
| 22.10. | A | Neue Sitzung A, Handoff, B pausiert bis 24.10. 01:00 | erledigt (Memory zwei-sessions) |
| 22.10. | A | Nur 26.3 Fabric bis alles fertig; dann Texturen, Fixes, erst dann andere Loader | erledigt (Regel Queue/Memory) |
| 22.10. | A | Queue-Stand als Liste | erledigt (Antwort) |
| 22.10. | A | Artefakte nicht sichtbar | erledigt (Betrieb) |
| 22.10. | A | UI-Änderungen erst exemplarisch | erledigt (Regel Queue 2) |
| 22.10. | A | Tiegel-Höhe ½ und ¼ Slot zeigen | erledigt (a-crucui22) |
| 22.10. | A | Leuchtfeuer: kein Glück | erledigt (a-beacons22) |
| 22.10. | A | Death-Vault-Legende verbessern | erledigt (a-sp22ui, a-ui23) |
| 22.10. | A | SP22-4 auf 1 px | erledigt (a-sp22ui) |
| 22.10. | A | „die abgenommen später verbessert“ | unklar, kein eigener Auftrag |
| 22.10. | A | Tiegel ¼ | erledigt (a-fix22b) |
| 22.10. | A | Beacon ok | erledigt |
| 22.10. | A | Legendäre Momente mystischer | in Queue (SP22-2, Wahl offen) + AUD-1 |
| 22.10. | A | Ungestoppt weiterarbeiten | erledigt (Betrieb) |

### 23.10.
| Datum | S | Wunsch | Status |
|---|---|---|---|
| 23.10. | A | Vault-Legende A+C, abgerundete Rahmen, „Verloren“ rot | erledigt (a-ui23) |
| 23.10. | A | Klänge zu grell/synthetisch → ElevenLabs-Kreationen | **FEHLT (AUD-1)** |
| 23.10. | A | Weitere Musikplatten (episch, Vanilla-Stil) | **FEHLT (AUD-2)** |
| 23.10. | A | Spawn-Eier: Shellker-Animation A→C→B, Dorf-Wanderer B überarbeitet | erledigt (a-tex23); übrige Eier in Queue (TX22-2) |
| 23.10. | A | Käse B/B, Milch B, Tiegelgriff C | erledigt (a-tex23) |
| 23.10. | A | Endholz-Laub lockerer (Alternative) | in Queue (TX22-3, W23-4: ~30 %) |
| 23.10. | A | Upload-UI C | erledigt (a-ui23) |
| 23.10. | A | Claims-UI an UI-Stil anpassen | in Queue (SP22-8, Strom 1) |
| 23.10. | A | PJ23-1…5 Taschen-Jukebox wie Bündel, Geräusche, Animation, Redstone, Weiterspielen | in Queue (a-pjuke23) |
| 23.10. | A | Abgestellte Mod-Bündel ohne Einlege-Geräusch | in Queue (PJ23-2) |
| 23.10. | A | Plattenpresse Plätze nebeneinander | in Queue (DP23) |
| 23.10. | A | Fragentisch-UI zentriert, Buchseiten außen, Hierarchie, Bild-/Rezept-Seiten | erledigt (a-qtable23), Feinschliff in Queue (W23-6) |
| 23.10. | A | Druckplatten/Scheiben im Rahmen flach von oben | erledigt (a-frames23), Folge in Queue (W23-5) |
| 23.10. | A | Salbei-Erz Stein-Variante verbessern | in Queue (SAGE23) |
| 23.10. | A | Astralit-/Nihilit-Redstone-Fackel | in Queue (B23-M1) |
| 23.10. | A | Bemooste Varianten Vanilla-näher | in Queue (B23-T1, W23-3) |
| 23.10. | A | Sculk-Tiefenschiefer neu | in Queue (B23-T2, W23-3) |
| 23.10. | A | Tiefenschiefer-Fliesen/gemeißelte Varianten wie Endholz-Prozess | in Queue (B23-T3) |
| 23.10. | A | Rissige Varianten wie Vanilla (Overlay je Block) | in Queue (B23-T4, W23-3) |
| 23.10. | A | Rissiges Eis nicht nebeneinander setzbar | in Queue (B23-M2) |
| 23.10. | A | Rissiges Eis Textur später | in Queue (Funde 23.10.) |
| 23.10. | A | Gras auf Stufen ausbreiten | in Queue (B23-M3) |
| 23.10. | A | Köcher und Farbkästen platzierbar | in Queue (B23-M11) |
| 23.10. | A | Bündel/Rucksäcke: Boden Schwerkraft, Wand wie Rahmen, nicht Decke | in Queue (B23-M12) |
| 23.10. | A | Rucksack-Itemtextur wie Bündel | in Queue (B23-T13, W23-4) |
| 23.10. | A | Redstone-Blöcke Brücke/Astral/Nihil neu | in Queue (B23-T5, W23-1) |
| 23.10. | A | Astralit-/Nihilit-Erz Vanilla-näher (Endstein + Overlay) | in Queue (B23-T6, W23-1) |
| 23.10. | A | Astral-/Nihil-Kolben neue Textur | in Queue (B23-T7, W23-1) |
| 23.10. | A | Baulicht verbessern | in Queue (B23-T8, W23-1) |
| 23.10. | A | Notenblock/Verstärker an neue Jukebox-Textur | in Queue (B23-T10, W23-4) |
| 23.10. | A | Astral-Gewölbe Motiv mittig, Enderit-Leuchten | in Queue (B23-T9, W23-1) |
| 23.10. | A | Mod-Kisten: nur Deckel öffnet sich | in Queue (B23-M13) |
| 23.10. | A | Warden-Ranke selten, Kerne wie Netherstern | in Queue (B23-M5) |
| 23.10. | A | Splitter werfbar, wenig Schaden | in Queue (B23-M4) |
| 23.10. | A | Simple-Pads-Tab aufräumen | in Queue (B23-M6) |
| 23.10. | A | Drifting-Effekt „betrunken“ | in Queue (B23-M14) |
| 23.10. | A | Animationen: Dimensional Scraps, Salbei-Erz, Lohenholz-Bretter/-Regale | in Queue (B23-A1, W23-4) |
| 23.10. | A | Nibblemites zähmbar | in Queue (B23-M15) |
| 23.10. | A | End-Gras/-Büsche auf mehr Blöcken | in Queue (B23-M7) |
| 23.10. | A | Mega-Wayfinder-Karte, Dimension-Knöpfe, Amboss-Erweiterung, Blättern | in Queue (B23-K1, K2) |
| 23.10. | A | Rezepte End-/Nether-Wayfinder geändert | in Queue (B23-K3) |
| 23.10. | A | Simple Money Texturvorschläge | in Queue (B23-T11), dann überholt (W23-4: verworfen) |
| 23.10. | A | Orchestrion: Relevanz prüfen | in Queue (B23-M9) |
| 23.10. | A | Schleich-Abbau → Achtel nur mit Hammer | in Queue (B23-M8) |
| 23.10. | A | Verkaufslager/Lager-Werkbank Design | in Queue (B23-T12, W23-4) |
| 23.10. | A | Claim-Tisch entfernen (Wiederholung) | in Queue (SP22-8) |
| 23.10. | A | Models-Knopf hinter der UI | in Queue (B23-M10) |
| 23.10. | A | W23-1 Wahlen T5–T9, Erze versteckter | in Queue (W23-1) |
| 23.10. | A | Falsche Erze (~20 %, größere Adern), Drop-Vorschläge | in Queue (W23-2) |
| 23.10. | A | Moos/Risse tiefer/Sculk/gehüllte Fliesen B + neuer Block C/Packeis B | in Queue (W23-3) |
| 23.10. | A | Notenblock-Rahmen 90°-symmetrisch, Money verworfen, Lager weicher, Rucksack drehen, Animationen ok, Butter/Piggy/Laub | in Queue (W23-4) |
| 23.10. | A | Rahmen Draufsicht + Meißel dreht in Stufen | in Queue (W23-5) |
| 23.10. | A | Fragentisch Buch größer, Falz, Schatten | in Queue (W23-6) |
| 23.10. | A | Beantwortetes von der Abnahme-Seite auf die Abgenommen-Seite | erledigt (Queue-Regel 3, zwei Artefakte) |
| 23.10. | A | Sind alle Wünsche in der Queue? | dieses Audit |

## Grenzen
- Nachrichten mit nur einem Bild (20.10., 21.10.) sind ohne Bild nicht zuzuordnen.
- Wahlen auf den Abnahme-Seiten (Kommentare) sind nicht Teil der Transkripte; deshalb der TEX21-Widerspruch (AUD-4).
- Status „erledigt“ folgt den Queue-Belegen; nicht erneut im Code oder im Spiel geprüft (außer AUD-3: `SimpleEndConfig.forestFogWall = true`).
