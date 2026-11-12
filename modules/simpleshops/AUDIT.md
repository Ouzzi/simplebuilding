# Audit simpleshops (a-audsb31e1, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643 (enthaelt die Shop-Musik aus a-shopmusic37), nur MC 26.3 Fabric; Forge/NeoForge nicht Teil
des Releases. Umfang: 44 Java-Dateien / 6890 Zeilen ohne Tests (shared + Fabric), dazu ~3,7k Zeilen Gametests. Risikogeleitet
geprueft (P1-Klassen zuerst: Geld-/Item-Dupes, Diebstahl, Besitzer-Pruefung), Aufbau wie `modules/simplestorage/AUDIT.md`.
Zusatzeingabe BEW39 (`/root/sb-tools-a/bewertung39/simpleshops.json`): einziger roter Punkt "Shop-Musik fehlt" ist in der Basis
gebaut und hier mit auditiert.

## Vollstaendig gelesen
- Menues: `menu/ShopMenu.java` (Reichweite, Leerlauf, ViewSlot), `menu/ShopBuyerMenu.java`, `menu/SalesOwnerMenu.java` (inkl. DiscSlot),
  `menu/StockMenu.java`, `menu/StorageMenu.java`, `menu/TradeTerminalMenu.java` (alle Buttons, Lieferungen, Sperre).
- Logik: `logic/ShopTrade.java` (Kauf vor Ort und fern), `logic/ShopActions.java`, `logic/ShopGuard.java` (Abbau, Protection-Provider),
  `logic/ShopStorage.java` (Lager-Kette, pull), `logic/EnderShops.java` (Weltdaten, Sperre, Codec), `logic/TradeTerminal.java`,
  `logic/PriceRow.java`, `logic/ShopLedger.java`, `logic/ServerShops.java`, `logic/ServerShopCommands.java`, `logic/ShopMusic.java`,
  `logic/ShopMusicPick.java`, `config/ShopsConfig.java`, `SimpleShops.java`, `ShopMapLayer.java`.
- Bloecke/BEs: `block/SalesBlock(Entity).java`, `block/DisplayCaseBlock(Entity).java`, `block/SalesStorageBlock(Entity).java`,
  `block/SalesStorageItem.java`, `block/TradeTerminalBlock.java`, `block/ListContainer.java`, `block/VaultLink.java`, `block/EarningsStore.java`.
- Mixin `mixin/ServerPlayerGameModeMixin.java` + `simpleshops.mixins.json`; Fabric-Einstieg `ShopsFabric(Client).java`; Loot-Tabellen (5),
  Block-Tags (wither/dragon_immune), `fabric.mod.json`; Client-Musik `client/ShopMusicClient.java`, `client/ShopMusicSound.java`.
- Tests: `test/MusicTests.java`, `test/TerminalTests.java` (Tint, Terminal-Sperre), Helfer in `ShopsTests`/`StorageTests`.
- Stichproben: `ShopsRegistry` (Eigenschaften, Menue-Typen), `client/ShopScreens.java` + `client/TradeTerminalScreen.java` (nur: was der
  Client sendet).
- Nicht gelesen: Renderer und Screens im Detail, `guide/ShopsGuide.java`, `discover/ShopsDiscover.java`, die uebrigen Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigene C2S-Payload. Der Client schickt nur Vanilla-Menue-Buttons (`clickMenuButton`): Kaeufer `ware`/`TRADE`/`TRADE_MAX` mit Revision, Besitzer `DEV_BUYER`, Terminal Tabs/Seiten/Suche (Zeichen einzeln, max. 32, Abfragen 1 je 5 Ticks). Alle Handler pruefen `stillValid` und lesen Mengen/Preise/Items nur aus den Weltdaten. S2C: Block-Update mit Besitzer, Preis-Vorlagen, Waren, Disc + Start-Tick + Reichweite (nie Erloese); siehe P3-4. |
| C2 Server-Autoritaet / Reichweite | Alle Menues: Anker lebt, Chunk geladen, 8 Bloecke, Leerlauf-Timeout; Besitzer-Menues zusaetzlich `mayEdit`/`isOwner`; Terminal `stillValid(access, TRADE_TERMINAL)`. Client-Klassen nur ueber den Client-Einstieg. |
| C3 Dupes / Item-Verlust | Kauf atomar (`ShopTrade.core`: pruefen, simulieren, mit Rueckrollen schreiben); Preise exakt (`isSameItemSameComponents` + Menge), Bezahlplatz nimmt nur das passende Preis-Item; Preis-/Waren-Ansichten sind `ViewSlot` (nie nehmen/legen) bzw. Vorlagen-Klicks aendern nie den Cursor. Voller Inventar-Fall: vor Ort faellt das Stueck vor den Spieler, fern kein Kauf (`NO_SPACE`). Lieferungen per Identitaet (nie doppelt), Abbau wirft Erloese/Waren/Transit genau einmal (`preRemoveSideEffects`), Loot-Tabellen nur der Block. BEs sind keine Container (Trichter/Rohre sehen nichts), `allowsAutomation` false. Disc: Abbau/`servershop remove` werfen sie aus, Explosion trifft den Block nicht (Bedrock-Widerstand + Immun-Tags), Kopien aus Kreativ-Blockdaten leert `initFor`. **P2-1 gefunden und gefixt.** |
| C4 NBT / Komponenten | `EnderShops`: Listen begrenzt (64 Slots, 5 Preise, 16 Lager, 64 Transit, 200k Eintraege), unlesbares Item kostet nur sich (Test `ender_shops_survive_an_unreadable_item`); beschaedigte Preisreihe wird geleert. Ledger/ServerShops mit Listengrenzen. |
| C5 Tick-Kosten | Lager zieht alle 8 Ticks 1 Stapel; Verkaufsblock verlinkt alle 40 Ticks; Terminal-Katalog laeuft ueber alle Eintraege (P3-3); Musik-Start-Suche bei Disc-Wechsel 81 Chunks (selten), Client-Suche 289 Chunks je 10 Ticks (P3-6). |
| C6 Leaks | Keine statischen Spieler-/Level-Maps; Sperre ueber `WeakReference`, verfaellt bei Logout/Tod/Menuewechsel/Abstand. |
| C7 Sicherheit / Rechte | Abbau nur Besitzer/OP (Mixin `destroyBlock`, Prioritaet 100, vor Claims), Fremde brechen nichts (auch kein Abbaufortschritt); Protection-Provider fuer Hammer/Ader/Kolben/Trichter; Server-Shop-Befehle Stufe 2; Config-Befehl Stufe 2 (SimpleLib). Freie Vitrine (nicht auf eigenem Verkaufsblock) ist bewusst ein normaler Block (SP22-7). Sperren-Missbrauch siehe P2-2 (Rueckfrage). |
| C8 Unabhaengigkeit | Pflicht nur SimpleLib; Claims (Simple Tweaks), Money, Maps ueber Framework-Vertraege; Standalone-Ziel `runSimpleshopsStandaloneGameTest` vorhanden. |
| C9 Doku / Pflege | Lang EN=DE (143 Keys), kein TODO/FIXME, keine Kern-Dubletten in `tools/dup_pairs.txt`. `fabric.mod.json`-Beschreibung war die von Simple Beacons (P3-1, gefixt). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simpleshops-P2-1 | P2 (Logikfehler) | Vitrine faerben (Schleichen + Farbstoff, NA24-3/WB24-5) ging im Spiel nie: Vanilla ruft beim Schleichen mit Item in der Hand `useItemOn` des Blocks nicht auf, und der Shop-Mixin gab genau diesen Fall an Vanilla zurueck (`mixin/ServerPlayerGameModeMixin.java:56`, alt `if (suppress) return;`). Der Gametest `display_tint_red` rief `BlockState#useItemOn` direkt und blieb deshalb gruen. | **gefixt**: Faerbe-Regel als `DisplayCaseBlock.tint(...)` (`block/DisplayCaseBlock.java:140`), der Mixin ruft sie im Schleich-Fall auf (`ServerPlayerGameModeMixin.java:57-64`, sonst unveraendert Vanilla); Gametest `display_tint_through_game_mode` (`test/TerminalTests.java:111`, Adapter `displayTintThroughGameMode`) ueber `ServerPlayerGameMode#useItemOn`: Fremder faerbt nicht und oeffnet nichts, Besitzer faerbt mit 1 Farbstoff, gleiche Farbe = nichts. |
| simpleshops-P2-2 | P2 (Griefing) | Ein Shop gehoert immer nur einem Spieler (Sperre vor Ort und vom Terminal). Wer im Handelsterminal einen fremden oeffentlichen Shop auswaehlt und alle < 300 s irgendetwas klickt (`idleTimeoutSeconds`), haelt ihn aus der ganzen Dimension dauerhaft gesperrt (`menu/TradeTerminalMenu.java:288-310`, `:645-649`; `logic/EnderShops.java:240-256`): der Besitzer kann weder vor Ort oeffnen noch nachfuellen ("busy", `logic/ShopActions.java:45,63,87`), nur abbauen. Vor Ort geht dasselbe, braucht aber Anwesenheit. | **Rueckfrage** (Frage 1 unten) |
| simpleshops-P3-1 | P3 (Metadaten) | `fabric.mod.json` (und `neoforge.mods.toml`) trugen die Beschreibung von Simple Beacons ("Strong Beacon ..."); Mod Menu zeigte sie. | **gefixt**: Text wie `modules.json` + Lager, Terminal, Musik (`fabric/src/main/resources/fabric.mod.json:15`, `neoforge/.../neoforge.mods.toml:11`) |
| simpleshops-P3-2 | P3 | Wegfinder-Ebene: in geladenen Chunks erscheinen auch private Shops (`listed=false`), in ungeladenen nur oeffentliche (`ShopMapLayer.java:37-46` vs. `:60`); Zeilen "Shop of/Offer/Price" fest Englisch. | Hinweis (oeffentlich-Pruefung auch im geladenen Zweig) |
| simpleshops-P3-3 | P3 (Last) | Terminal-Katalog (`TradeTerminal.mine/browse`, `logic/TradeTerminal.java:56-143`) laeuft ueber alle Weltdaten-Eintraege (bis 200k), gedrosselt auf 1 Abfrage je 5 Ticks je Spieler plus 1 Server-Refresh je 40 Ticks je offenem Terminal (`TradeTerminalMenu.java:217-228, 595-597`). | Hinweis (Index nach Art/Dimension, falls grosse Server) |
| simpleshops-P3-4 | P3 | Block-Updates enthalten die vollen Waren-/Disc-Stapel samt Komponenten (`DisplayCaseBlockEntity.java:166-171`, `SalesBlockEntity.java:268-278`); sehr grosse Komponenten (volle Shulker, Buecher) vergroessern Chunk-Pakete fuer alle Zuschauer - wie Vanillas Regale. | Hinweis |
| simpleshops-P3-5 | P3 | Waren-Aenderungen im `StockMenu` erhoehen die Revision nicht (nur `quickStock`/Befehle). Folgenlos bei Spieler-Shops (Sperre), bei Server-Shops (ohne Sperre) kann ein Kaeufer im selben Tick die gerade getauschte Ware zum alten Preis bekommen. | Hinweis (`ListContainer`-Callback des Waren-Fensters koennte `bumpRevision` mitrufen) |
| simpleshops-P3-6 | P3 (Last) | Musik: `ShopMusic.startFor` sucht bei jedem Disc-Wechsel in 81 Chunks (`logic/ShopMusic.java:57-79`); der Client sammelt alle 10 Ticks 289 Chunks (`client/ShopMusicClient.java:82-101`). Fuer die Haeufigkeit unkritisch. | Hinweis |
| simpleshops-P3-7 | P3 | Der Abbau-Mixin bricht vor Fabrics `PlayerBlockBreakEvents` ab (`ServerPlayerGameModeMixin.java:37-48`): Log-/Schutz-Mods sehen Shop-Abbauten nicht. Gewollt fuer den Schutz, fuer Protokoll-Mods eine Luecke. | Hinweis |

BEW39 "missing" nicht gebaut (Antwort des Besitzers steht aus): Client-Bilder Terminal/Lager, Echt-Maus-Probe, JEI-Infoseiten, Optik-Wahlen BS25-15/TEXRATE20.

## Besitzer-Fragen (Vorschau AUDSB31E1)
1. **Shop-Sperre (simpleshops-P2-2).** Ein Spieler kann einen fremden oeffentlichen Shop vom Handelsterminal aus mit gelegentlichen Klicks dauerhaft
   blockieren; der Besitzer kommt dann selbst nicht mehr an seinen Shop. Optionen: **A** der Besitzer geht immer vor (sein Klick vor Ort oder seine
   Terminal-Auswahl loest die fremde Sperre, das fremde Fenster schliesst); **B** eine Terminal-Auswahl haelt den Shop nur kurz (z. B. 30 s ohne Klick,
   vor Ort bleiben 300 s); **C** so lassen. Empfehlung **A**: der Besitzer behaelt immer die Kontrolle, ehrliche Kaeufer merken nichts, kleiner Eingriff
   an einer Stelle (Sperre in `EnderShops.Vault`).

## Antworten des Besitzers (12.11.2026, E1ANS)
- **Frage 1 (Vorschau F5) = A + Wartungsschild**: der Besitzer geht immer vor (`ownerFirst`, Standard an): sein Klick vor Ort (Verkaufsblock,
  Vitrine, Schnell-Einraeumen) oder sein Shop im Handelskontor („Meine Shops“) nimmt einem Kunden die Sperre ab (`EnderShops.Vault#ownerTakesOver`).
  Das Kunden-Fenster (vor Ort oder Kontor) bleibt offen, Handelszeilen liegen hinter einem Pixelart-Schild „Wartungsarbeiten/Besitzer am Werk“
  (`client/MaintenanceSign`, Textur `textures/gui/maintenance_sign.png` aus `tools/textures/shop_maintenance_2026_11_12.py`, Variante A von 3);
  Handel serverseitig gesperrt (`ShopBuyerMenu` nur mit Sperre, `executeRemote` wie bisher); schliesst der Besitzer, nimmt das wartende Fenster
  die Sperre zurueck. Wer klickt, waehrend der Besitzer arbeitet, sieht dasselbe Schild. Gametests `owner_first_maintenance`, `terminal_owner_first`,
  angepasst `lock_second_player` (ownerFirst aus = alte Regel), Owner-Screen-Test in `StorageTests`.

## Urteil
**shipbar nach Fix** (0 P1; 2 P2: P2-1 gefixt, P2-2 Rueckfrage ohne Dupe-/Verlustrisiko; 7 P3, davon P3-1 gefixt). Die Kauf-, Lager- und
Terminalwege sind gegen Dupes und Diebstahl sauber gebaut (atomarer Kern, Weltdaten statt Container, Identitaets-Lieferungen); die neue
Shop-Musik (Disc-Slot, Abbau, Sync, Schalter) ist ohne Befund.

## Testergebnis
Lokal: javac der geaenderten Dateien (`DisplayCaseBlock`, `ServerPlayerGameModeMixin`, `TerminalTests`, `ModuleGameTest`) gegen 26.3 + Fabric-API:
in ihnen keine Meldung; bekannte Access-Widener-Meldungen nur in nicht geaenderten Dateien (`ShopsRegistry` MenuType/Output, `LibMaterials`
StairBlock, `ShopsTests`/`StorageTests` BlockPlaceContext). `modules/simpleshops/tools/check_data.py` gruen, `tools/check_dup_impl.py` 0 Probleme,
`tools/check_tooltip_text.py` gruen. sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md` (neuer Test `display_tint_through_game_mode`).
Nicht getestet: Gametest-Lauf selbst (kein Gradle in diesem Lauf), Client (Armschwung beim Faerben, Musik-Hoeren), echter Mehrspieler, NeoForge.
