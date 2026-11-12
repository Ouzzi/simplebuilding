# Audit simpletweaks (a-audsb31e1, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie
`modules/simplepads/AUDIT.md`. Umfang: 215 Java-Dateien / 13,9k Zeilen ohne Tests (241 / 19,6k mit Tests), davon 89 Mixins
(42 Claims, 20 Admin, 11 Protect inkl. 1 Client-Mixin, 14 Play, 2 Basis). Risikogeleitet geprueft (P1-Klassen zuerst),
Zusatzeingabe BEW39 (`/root/sb-tools-a/bewertung39/simpletweaks.json`, keine roten Eintraege).

## Vollstaendig gelesen
- Netzwerk: `deed/net/*` (5 C2S: Edit, Claim, ChunksRequest, Action, Trade; 3 S2C: Open, Result, Chunks), `protect/ProtectRulesPayload`,
  Registrierung `TweaksFabric`/`TweaksFabricClient`/`ProtectFabric`, Handler `deed/DeedService.java`.
- Claims: `claims/Claims.java`, `ClaimStore`, `ClaimConfig`, `ClaimRights`, `ClaimCommands`, `ClaimAutomation`, `ClaimMaps`,
  `ClaimCode` (Parser bis Zeile 140), alle 42 Mixins unter `mixin/claims/`, `table/ClaimTableBlock`, `TableVisibility`, `ClaimMapLayer`.
- Urkunde: `ClaimDeedItem`, `DeedData`, `DeedCloningRecipe`, `DeedCartography` + `mixin/CartographyDeedMixin`, `ClaimPictures`, `DeedViewer`.
- Admin: `Admin`, `AdminConfig`, `AdminPerms`, `AdminState`, `AdminFiles`, `AdminCodecs`, `OfflineData`, `blocklog/*` (BlockLog, BlockRecorder,
  Rollback, BlockLogCommands, Inspect), `backup/Backups`, `deaths/DeathBackups`, `invsee/Invsee` + `InvseeMenu`, `vanish/Vanish`,
  `freeze/Freeze`, `restart/Restart`, `maintenance/Maintenance`, `lag/LagFinder`, alle 20 Admin-Mixins.
- Protect: `AntiXray`, `Protect`, `ProtectConfig` (bis Zeile 120), `ProtectCommands`, `FreecamRule`, `ActionChecks`, `HideHealth`, `SeedHash`,
  `ProtectPerms`, `client/ProtectClient`, alle 11 Mixins.
- Play: `Play`, `PlayConfig`, `PlayState`, `PlayCommands`, `PlayItems`, `LoginRewardStacks`, `reward/LoginReward`, `keep/KeepInventory`,
  `vote/Votes`, `events/ServerEvents` + `WeatherPlan`, `cleanup/GroundCleanup`, `crowd/EntityLimit`, `roof/NetherRoof`, `pregen/Pregen`,
  `pvp/Pvp`, `shield/NewPlayerShield`, `coords/Coords`, `afk/Afk`, `chat/Mentions`, `dragon/DragonEgg`, `grief/MobGriefing`,
  `border/Borders`, `death/DeathHint`, `phantoms/Phantoms`, `Welcome#vanillaLine`, alle 14 Mixins.
- Rest: `LegacyDeed`, `LegacyAliases`, `mixin/LegacyRegistryMixin`, `TweakCommands`, `TweaksConfigCommand`, Mixin-Konfigs, `fabric.mod.json`.
- Nicht gelesen: Client-Screens (`deed/client/*`, `admin/invsee/client/*`; nur geprueft, dass Logik serverseitig liegt und Client-Klassen
  nur unter `client/` stehen), `DeedLayout`/`DeedMapLayout`/`DeedText`, `ClaimCode` ab Zeile 140 (Generator/Anzeige), `guide/`, `discover/`,
  `play/` Kleinteile (`Playtime`, `Leaderboard`, `StatBoard`, `WeatherSchedule`, `ServerEventsSchedule`, `PlayText`), Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Alle 5 C2S laufen per `server().execute` auf dem Server-Thread, je Spieler 5 Ticks gedrosselt (`DeedService.throttled`, eigene Uhr fuer Kachel-Anfragen). Die Urkunde liest der Server selbst aus der Hand (Hand 0/1 geprueft), Rechte nur aus dem Ledger: Editieren nur Eigentuemer/Manager (`Claims.applyCode:272`), Angebot-Index geprueft (`Claims.trade:402`), Aufgeben nur eigener Chunks (`Claims.unclaim:490`), Karte nur mit Recht im Land (`seesLand`). Texte vom Codec begrenzt (128 bzw. 4096 Zeichen, Parser mit harten Grenzen). S2C begrenzt (64 Kacheln x 4 Seiten x 256 Byte, 128 Namen x 16, Code 4096). |
| C2 Server-Autoritaet | Claim-, Kauf-, Kopier- und Rollback-Entscheidungen nur serverseitig; Kartentisch-Ergebnis nur ueber `ContainerLevelAccess` (Server). `InvseeMenu.stillValid` prueft Recht + Ziel online (gleiches Objekt) bzw. offline nicht eingeloggt. Keine Client-Klasse ausserhalb `client/`; der Freecam-Mixin ist Client-only, `@Pseudo`, `require=0`. |
| C3 Dupes/Verlust | Invsee: ohne Edit-Recht weder Nehmen noch Legen, Selbstansicht verboten, Login schliesst Offline-Ansichten vor dem Laden. Urkunden-Kopie: Rezept gibt das Original als Rest zurueck, Kartentisch nimmt genau 1 Eingabe. Kauf/Tausch: Items erst nach gespeichertem Budget abgezogen. T27: behaltene Slots werden verschoben (Tag), nicht kopiert. Rollback-Grenzen siehe P2-5, Besucher-Itemverlust P2-3. |
| C4 NBT/Daten | Ledger: strenges JSON, 64 MiB, keine Symlinks, atomar, Budget 1..256; Bilder-Datei kaputt = umbenannt; `DeedData`-Codec (Name <= 16, Generation 0..1); Offline-Daten ueber DataFixer, Schreiben wie Vanilla; Status-Dateien kaputt = Quarantaene. |
| C5 Tick-Kosten | Claim-Pruefungen sind Map-Zugriffe; Bilder 4 je 100 Ticks; Block-Log asynchron (Warteschlange max. 200k, Welt-Ereignisse 1000/Tick); Anti-X-Ray-Cache 20 Ticks/256 Chunks; Entity-Limit/Aufraeumen standardmaessig aus. Auto-Backup: `saveEverything(flush)` auf dem Server-Thread alle 6 h (Frage F1). Keine Messung im Release (BEW39). |
| C6 Leaks | Statische Spieler-Maps werden beim Verlassen/Stopp geleert (`TweaksFabric:20-33`, `Play.left`, `Admin.stop`); `BlockRecorder.OPEN` und `ClaimAutomation.WORLDLY` sind WeakHashMaps; Rest P3-11. |
| C7 Rechte/Mixins | Alle Admin-/Event-/Pregen-/Protect-Befehle mit OP-Stufe aus der Config (Standard 2-4), `/claim admin` Stufe 4. Jeder Mixin fragt zuerst den Modul-Schalter (`SubMods`) und den Feature-Schalter; kein `@Overwrite`, kein `@Redirect`; Prioritaeten 1100 (Kolben/Trichter/Spender) und 1300 (Kartentisch). Schutzluecken-Suche (Kolben, Explosionen, Fluesse, Feuer, Blitz, Projektile, Boote/Loren, Begleiter, Endermen, Trichter/Rohre ueber Grenzen, Kupfergolem, Crafter, Knochenmehl, Sculk, Baeume, Frostlaeufer): kein Umgehungsweg gefunden. |
| C8 Unabhaengigkeit | Pflicht nur SimpleLib (`fabric.mod.json`, `modules.json`); Simple Maps/Money/Weather/Dimensions nur per Registry-ID, Mod-ID oder Konfig-Datei. BEW39: 172/172 Gametests standalone. |
| C9 Doku/Pflege | Lang EN=DE (518 Schluessel), kein TODO/FIXME, keine Kern-Dubletten (`tools/dup_pairs.txt` ohne Eintrag, `check_dup_impl` 0 Probleme). Veraltete Modul-Dokumente P3-1. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simpletweaks-P2-1 | P2 (Datenverlust) | Auto-Backup raeumt nach Praefix `<Welt>-` auf (`admin/backup/Backups.java:180` alt): im Einzelspieler liegen alle Welten in `.minecraft/backups`; das Aufraeumen von „Welt" loeschte auch die Sicherungen von „Welt-2". | **gefixt**: nur `<Welt>-yyyyMMdd-HHmmss.zip` zaehlt (`Backups.java:178-182`); Gametest `backup_prune_keeps_other_worlds` (`AdminGameTest#backupPruneKeepsOtherWorlds`). |
| simpletweaks-P2-2 | P2 (Absturz selten, falsche Werte) | `/lag` im Einzelspieler: der Hook auf `Level.tickBlockEntities` (`admin/mixin/AdminLagMixin.java:16`) laeuft auch fuer das Client-Level auf dem Client-Thread und schreibt gleichzeitig mit dem Server in dieselben HashMaps (`LagFinder`): Client-Zeiten im Bericht, `ConcurrentModificationException` moeglich. | **gefixt**: `LagFinder` zaehlt nur auf dem Server-Thread (`admin/lag/LagFinder.java:54-66`); Gametest `lag_finder_ignores_other_threads` (`AdminGameTest#lagFinderIgnoresOtherThreads`). |
| simpletweaks-P2-3 | P2 (Itemverlust, Logik) | Besucher ohne Recht in fremdem Claim: (a) koennen keine Monster schlagen/beschiessen, jeder Mob braucht Recht A (`claims/Claims.java:91`; Handbuch: „A Tiere und Fahrzeuge"), werden aber angegriffen; (b) eigene Todes-Drops brauchen C, XP-Kugeln B+D (`Claims.java:93-94`, `ClaimPickupMixin`) und verfallen nach 5 min; (c) abgewiesene Projektile werden geloescht, auch Dreizacke (`mixin/claims/ClaimProjectileMixin.java:19`). | **gefixt (E1ANS, Besitzer F2=A, 12.11.)**: `Claims.allowAttack/attack/pickup/droppedBy/refuse`, Optionen `claims.visitors.*` (je Teil, Standard an), Gametests `claims_visitor_monsters/pickup/projectiles/config`. |
| simpletweaks-P2-4 | P2 (Balance) | `/vote event double_xp|more_loot|blood_moon` (Standard an, `play/PlayConfig.java:144`): allein Online (Einzelspieler, kleiner Server, andere AFK) reicht die eigene Ja-Stimme (`play/vote/Votes.java:75-81`) - 60 min doppelte XP/mehr Beute, nach Ablauf sofort wieder. | **gefixt (E1ANS, Besitzer F3=B, 12.11.)**: `play.vote.eventMinPlayers` (Standard 2), Event-Abstimmung bei laufendem Event abgelehnt, Abklingzeit ab Event-Ende (`Votes.eventEnded`); Gametest `vote_event_rules`. |
| simpletweaks-P2-5 | P2 (Dupe ueber Admin-Rollback) | (a) Container-Log vergleicht Oeffnen/Schliessen (`admin/blocklog/BlockRecorder.java:104,128`): was Trichter/Ofen in der Zeit bewegen, wird dem Spieler als T/D zugeschrieben, ein Rollback legt es zurueck; (b) Rollback von B/X/F stellt Container mit Inhalt wieder her (`admin/blocklog/Rollback.java:46,119`), obwohl der Inhalt beim Abbau/Explosion als Drops in die Welt fiel. | **Hinweis im Handbuch (E1ANS, Besitzer F4=C, 12.11.)**, kein Code; "Log je Klick" (F4 A) bleibt als Aufgabe nach dem Release. |
| simpletweaks-P3-1 | P3 | `CLAIMS-ACCESS-VERIFICATION.md`, `CLAIMS-ACCESS-PLAN.md`, `CLAIMS-PLAN.md`, `CLAIMS-STAGE4.md`: „Claims remain disabled by default", „Stage 4 unmerged". | **gefixt**: Hinweis „Historical document" oben eingefuegt; Verschieben nach `docs/archive` = Hauptagent. |
| simpletweaks-P3-2 | P3 | `/pregen` nach Neustart: offene und fehlgeschlagene Anfragen gehen verloren (`play/pregen/Pregen.java:104,127`), `done` erreicht `total` nie, Job bleibt bei <100 % stehen (nur `/pregen stop`). | Vorschlag: beim Start `j.next = min(j.next, j.done)`. |
| simpletweaks-P3-3 | P3 | Invsee offline: zwei Admins auf demselben Offline-Spieler (oder invsee + endersee gleichzeitig) arbeiten auf eigenen Kopien, letzter Schreiber gewinnt (`admin/invsee/Invsee.java:65,125`) - Doppelung nur durch OPs mit Edit-Recht (koennen ohnehin `/give`). | Hinweis. |
| simpletweaks-P3-4 | P3 | T27 legt behaltene Stacks zurueck in den toten Spieler (`play/keep/KeepInventory.java:77`); ein Grabstein-Mod, der das Inventar erst nach `dropAll` liest, wuerde sie mitnehmen (nicht pruefbar ohne solchen Mod). BS25-19 (Absturz Seelenfeuer-Lohen-Kopf) bleibt Client-TODO (BEW39). | offen (Client/Kompat). |
| simpletweaks-P3-5 | P3 | Kaputte `simpletweaks-play/admin/protect.json` -> Standardwerte (alles an) bis zur Reparatur (`AdminConfig.java:162` u. a.); kaputte `simpletweaks-claims.json` bricht den Serverstart ab (`ClaimConfig.java:149`, gewollt); gesperrter Ledger sperrt Bauen ueberall (`Claims.java:172`, gewollt). | Hinweis Handbuch. |
| simpletweaks-P3-6 | P3 | Unbekannte Container-Typen (weder BlockEntity noch Entity) sind bei aktiven Claims ueberall fuer Trichter gesperrt (`claims/ClaimAutomation.java:28`); Spender-Sonderverhalten (Eimer, Knochenmehl, ...) im Umkreis von 2 Chunks um jeden Claim, auch im eigenen Land (`ClaimDispenserMixin.java:20`, dokumentiert). | Hinweis (Mod-Kompat). |
| simpletweaks-P3-7 | P3 | Feuer in Claims tickt nie (`ClaimFireMixin.java:12`): brennt nicht ab, geht im Regen nicht aus. | Hinweis. |
| simpletweaks-P3-8 | P3 | Anti-X-Ray: ein veraenderter Client kann verdeckte Bloecke in Reichweite ueber die Block-Antworten auf Abbau/Benutzen abfragen (Vanilla sendet den echten Block); `forget`/`revealAround` laufen bei jedem Block-Update (`ProtectLevelMixin`); keine Kostenmessung im Release (BEW39). | Hinweis/TODO Messung. |
| simpletweaks-P3-9 | P3 | Karten-Marker nur englisch („Claim of", „Your land", `table/ClaimMapLayer.java:61,69`). | Hinweis (Marker-API ohne Lang-Keys). |
| simpletweaks-P3-10 | P3 | `AdminPacketMixin:22` liest `AdminState.vanished` (LinkedHashSet) in `send`, das auch ausserhalb des Server-Threads laufen kann. | Hinweis. |
| simpletweaks-P3-11 | P3 | `EntityLimit.TOLD` und `ActionChecks`-Maps wachsen bis zum Serverstopp (Features standardmaessig aus). | Hinweis. |
| simpletweaks-P3-12 | P3 | Metadaten: Beschreibung in `fabric.mod.json`/`modules.json` nennt nur Altwelt + Claims (Admin/Spieler/Schutz fehlen). | an Hauptagent. |

## Besitzer-Fragen
- **F1 Standardwerte (BEW39, A12):** standardmaessig an sind Block-Log (14 Tage Tagesdateien), Auto-Backups alle 6 h mit Speichern+Flush
  (auch Einzelspieler, 5 ZIPs je Welt), Anti-X-Ray (auch Einzelspieler), Teil-Keep-Inventory `hotbar_armor` und automatische Spieler-Tweaks
  (Todes-Hinweis, Erwaehnungen, AFK zaehlt nicht beim Schlafen, Neulingsschutz 5 min, Drachenei jeder Kampf); A12-Steckbriefe fehlen.
  A) alles so lassen, nur A12-Steckbriefe + Handbuch; B) Server-Werkzeuge (Block-Log, Auto-Backup, Anti-X-Ray) nur auf Dedicated Servern
  standardmaessig an, im Einzelspieler/LAN aus, Gameplay-Tweaks wie entschieden; C) zusaetzlich T27 und Drachenei standardmaessig aus.
  **Empfehlung B**: im Einzelspieler schuetzen diese drei nichts (eigene Welt, Vanilla hat „Sicherung erstellen"), kosten aber Platte, Haenger, CPU.
- **F2 Besucher in fremden Claims (P2-3):** A) Monster (`Enemy`) darf jeder bekaempfen, eigene Items (Werfer = Spieler) und XP-Kugeln darf
  er aufheben, abgewiesene Dreizacke/aufhebbare Pfeile gehen an den Werfer zurueck; B) nur Aufheben + Dreizack wie A, Monster weiter nur mit Recht A;
  C) so lassen, nur Handbuch praezisieren. **Empfehlung A**: Claims schuetzen Eigentum, sollen Besucher aber nicht wehrlos machen oder deren Sachen vernichten.
- **F3 Event-Abstimmung (P2-4):** A) `vote.events` standardmaessig aus; B) Event-Abstimmung erst ab 2 aktiven Spielern, Abklingzeit ab Event-Ende;
  C) so lassen. **Empfehlung B**: bleibt fuer Gruppen nutzbar, der Solo-Dauer-Bonus faellt weg.
- **F4 Rollback und Container (P2-5):** A) Container-Log je Klick statt Oeffnen/Schliessen (Aufwand M); B) Container-Inhalt nur mit Zusatz
  `contents:yes` wiederherstellen; C) so lassen, Hinweis in Handbuch und `--preview`. **Empfehlung C jetzt, A nach Release**: Admin-Werkzeug,
  der Admin prueft vorher Drops und Taeter-Inventar (Verhalten wie verbreitete Log-Plugins).

## Antworten des Besitzers (12.11.2026, E1ANS)
- **F1 = B**: Block-Log, Auto-Backup und Anti-X-Ray laufen standardmaessig nur auf Dedicated Servern (`blockLog.onlyDedicated`, `backups.onlyDedicated`,
  `antiXray.onlyDedicated`, Standard true; `Admin.dedicated/runs` ueber `MinecraftServer#isDedicatedServer`, der Gametest-Server gilt als dediziert).
  Bestehende Dateien ohne den Schluessel zaehlen als true: ein Dedicated Server aendert sich nicht, der integrierte Server (Einzelspieler, LAN) schaltet die drei ab.
  Gametests `server_tools_only_on_dedicated`, `antixray_only_on_dedicated`.
- **F2 = A**: siehe P2-3. **F3 = B**: siehe P2-4. **F4 = C**: Handbuch-Hinweis (Eintrag `admin_block_log`), "Log je Klick" nach dem Release.

## Urteil
**shipbar** nach den zwei Fixes (P2-1, P2-2) - vorbehaltlich F1-F4 (3 P2 als Rueckfrage, keine P1). Netzwerk und Claim-Schutz sind
sorgfaeltig serverseitig gebaut; die offenen Punkte sind Mechanik-/Standardwert-Entscheidungen.

## Testergebnis
Lokal: javac der geaenderten Dateien (`Backups`, `LagFinder`, `AdminTests`, `AdminGameTest`) sauber - nur die bekannten Access-Widener-Meldungen
in der unveraenderten `Invsee.java` (`MenuType` private access); `tools/check_data.py` gruen, `check_dup_impl` 0 Probleme.
sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md` (Gametests `backup_prune_keeps_other_worlds`, `lag_finder_ignores_other_threads`).
Nicht getestet: echter Einzelspieler-`/lag` mit Client-Thread (der Gametest ruft den Hook von einem zweiten Thread), Backups zweier
echter Welten, alles Client-seitige (Screens, BS25-19), Grabstein-Mods mit T27, Forge/NeoForge.
