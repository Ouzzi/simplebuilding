# Release-Audit Familie Simple Aesthetics (`b-rel32-aes`, 31.10.2026)

Mods: `simpleaesthetics` (Super-Mod, 0.9.0), `simplevisuals` (0.9.0), `simplemodels` (0.1.0), `simplesounds` (0.1.0). Nur MC 26.3 Fabric.
Basis `b-rel32` 1be1d2873. Je Mod `modules/<mod>/AUDIT.md` (Dateiliste mit Zeilenzahlen, C1-C9, Befunde, Urteil). Nichts veroeffentlicht oder hochgeladen.

## 1. Plan (Kopf)
- Dateiliste je Mod: siehe `AUDIT.md` (zusammen 4 Mods, 172 Dateien, ca. 9,6 k Zeilen; alle `.java`, Mixin-JSON, `fabric.mod.json`, Lang, Daten, Tools). Gelesen: alle Fabric-/Shared-Javadateien vollstaendig; Gametest-Klassen und Forge/NeoForge-Zweige nur ueberflogen bzw. nicht gelesen (nicht Teil dieses Releases).
- Reihenfolge: Super-Mod (558 Z.) -> Sounds (1777; Audioherkunft) -> Models (2704) -> Visuals (4644, 17 Mixins zuletzt). Genutzt: `framework` `SubMods`, `ConfigFiles`, `CosmeticIntensity`; `simplelib` `ConfigCommands`.
- Pruefliste C1-C9 aus `docs/ai/RELEASE-QOL-OPT-AES.md` Abschnitt 2; Mixin-Ziele nur ueber `check_client_mixins.py` (im sbt-Lauf) und Quellsicht gegen die 26.3-Quellen; ein Test-Lauf (Abschnitt 5).

## 2. Ergebnis in Kuerze
| Mod | Urteil | P1 | P2 | P3 |
|---|---|---|---|---|
| simpleaesthetics | shipbar | 0 | 0 | 3 (Hinweise/Rueckfrage) |
| simplesounds | shipbar, keine Audiodateien im Jar | 0 | 1 (Rueckfrage) | 5 (2 gefixt) |
| simplemodels | shipbar | 0 | 1 (gefixt) | 5 |
| simplevisuals | shipbar, Vorbehalt Client-Lauf + Sodium/Iris-Sicht | 0 | 1 (Rueckfrage) | 5 (1 gefixt) |

Fixes (Code): Snapshot-Paket von simplemodels kompakt (P2), Konfig-Quarantaene bei simplevisuals/simplesounds, Tick-Allokation in simplesounds. Siehe Git-Log des Branches.
Kernbefund (P2): `cloth-config` steht bei Visuals und Sounds in `depends`; ein Dedicated Server muss Cloth Config nachinstallieren, obwohl nur Client-Bildschirme es brauchen (Rueckfrage).
Audiofrage beantwortet: **Simple Sounds enthaelt keine Audiodatei**; alle zwoelf Effekte sind Vanilla-Sound-Events (`minecraft:`) die lokal abgespielt werden. Nichts ElevenLabs, nichts Fremdes, keine Mojang-Datei im Jar.

## 3. Abnahmeplan fuer den Besitzer (In-Game)
Voraussetzung: Minecraft 26.3, Fabric, Fabric API, Cloth Config, optional Mod Menu. Entweder das Super-Mod-Jar `simpleaesthetics` (enthaelt Visuals, Sounds, Models) oder die Jars einzeln. Zuweisen von Modellen braucht OP.

### simpleaesthetics (Super-Mod)
| ID | Aktion | Erwartet |
|---|---|---|
| QOLA-AES-1 | Nur Jar `simpleaesthetics` (+ Fabric API, Cloth Config) in `mods`, Spiel starten | Start ohne Fehler; `config/simpleaesthetics.json` mit `enableSimpleVisuals/Sounds/Models` (alle `true`); mit Mod Menu erscheinen die drei Sub-Mods |
| QOLA-AES-2 | `/simpleaesthetics config` im Spiel | Zeigt die drei "Enable Simple ..."-Schalter, Default true |
| QOLA-AES-3 | `enableSimpleVisuals` auf `false`, Neustart, Einzelspielerwelt | Keine Geschwindigkeitslinien, kein Pickup-Notifier, keine Haltbarkeitszeile am Item, Amboss ohne Farbcodes; Sounds und Models arbeiten weiter |
| QOLA-AES-4 | `enableSimpleSounds` auf `false`, Neustart | Keine der Effekt-Sounds mehr (Schritt in Schnee/Wasser planschen), Visuals/Models unveraendert |
| QOLA-AES-5 | `enableSimpleModels` auf `false`, Neustart | Kein "Models"-Reiter an Inventar/Amboss, Fuehrungsbuch nicht im Kreativ-Tab, normales Umbenennen im Amboss funktioniert |
| QOLA-AES-6 | Datei mit kaputtem Text ueberschreiben, Start | Spiel startet, Kopie `simpleaesthetics.json.broken-<zeit>` liegt daneben, Datei neu mit Standardwerten |

### simplevisuals (Client + etwas Server)
| ID | Aktion | Erwartet |
|---|---|---|
| QOLA-VIS-1 | Spiel + Welt starten, Config-Bildschirm ueber Mod Menu oeffnen | Bildschirm mit Reitern (Allgemein, Partikel, Geschwindigkeit, Aufheben, Schaden, ...) ohne Fehltexte (`text.autoconfig...` darf nirgends als Rohtext stehen); Speichern schreibt `config/simplevisuals.json` |
| QOLA-VIS-2 | Mit Elytra fliegen/sprinten bzw. schnell fallen | Radiale Geschwindigkeitslinien erscheinen mit steigender Geschwindigkeit, hoeren beim Abbremsen auf; ausschaltbar |
| QOLA-VIS-3 | Items aufheben (auch Erfahrung) | Pickup-Liste am rechten Rand (max. 8 Zeilen), Mengen werden zusammengezaehlt, verschwinden nach ca. 6 s |
| QOLA-VIS-4 | Zombie schlagen | Schadenszahl ueber dem Mob, Gold bei grossem Schaden; Partikel (Herzen/Schadensanzeige) nach Intensitaet |
| QOLA-VIS-5 | Verzaubertes/beschaedigtes Werkzeug in die Hand | Haltbarkeit und Verzauberungen (max. 3 + "weitere") ueber der Hotbar |
| QOLA-VIS-6 | Amboss: Namen mit `&c` oder `&lFett` eintippen | Ergebnis farbig/fett; `/simplevisuals config visuals enableAnvilFormatting false` (OP) -> Codes werden entfernt |
| QOLA-VIS-7 | Karte im Inventar mit der Maus ueberfahren | Tooltip zeigt Kartenbild mit Mittelpunkt und Dimension |
| QOLA-VIS-8 | Item-Rahmen mit Druckplatte/Glasscheibe bestuecken; mit SimpleBuilding-Meissel schleichend klicken | Platte liegt flach, Scheibe flach; Meissel kippt in 15-Grad-Schritten bis 90 Grad, dann wieder flach; Tooltip/Rahmen sehen aus allen Winkeln richtig aus |
| QOLA-VIS-9 | Zusaetzlich Sodium installieren, dann Iris + Shaderpack; Rahmen, Chat, HUD ansehen | Start ohne Fehler; Rahmen-Items, HUD, Chat-Koepfe sehen richtig aus (Pflicht-Lauf vor Veroeffentlichung, bisher nie gefahren) |
| QOLA-VIS-10 | Chat: Spieler schreibt; Tod im Spiel | Kopf des Spielers neben der Chat-Zeile; Todesmeldung mit eigenen Koordinaten (Standard: eigene Zeile) |
| QOLA-VIS-11 | Effekt-Leiste: Trank trinken | Duenner Fortschrittsbalken unter dem Effekt-Symbol, schrumpft; im Inventar ebenso |
| QOLA-VIS-12 | `/simplevisuals guide` | Vanilla-Buch mit 43 Seiten in der Spielersprache (DE/EN) |

### simplesounds (Client)
| ID | Aktion | Erwartet |
|---|---|---|
| QOLA-SND-1 | Welt laden, Mod-Menu-Config oeffnen | Bildschirm: "Visuals folgen", Grundstufe, Lautstaerkendeckel (max 0,25), Sounds/Tick, Cooldown, je Effekt Stufe |
| QOLA-SND-2 | Im Schnee/Pulverschnee laufen | Leises Pulverschnee-Tritt-Geraeusch alle Sekunde, nie laut |
| QOLA-SND-3 | In Wasser schwimmen/unter Tropfsteinen stehen | Leises Plantschen bzw. Tropfen |
| QOLA-SND-4 | Verzaubertes Item in die Hand, Leuchtfeuer in Naehe | Leises Amethyst-Klingen bzw. Leuchtfeuer-Rauschen |
| QOLA-SND-5 | Selbst Schaden nehmen und heilen | Kurzer, leiser Schadens-/Heilungs-Ton |
| QOLA-SND-6 | `globalLevel` = OFF oder Effekt `OFF` | Keine Toene dieses Effekts; mit `enableSimpleSounds=false` im Super-Mod gar keine |
| QOLA-SND-7 | Sehr viele Ereignisse zugleich (rennen + Feuer + Wasser) | Nie mehr als 2 gleichzeitige Effekt-Sounds, kein Knattern |
| QOLA-SND-8 | `config/simplesounds.json` mit Muell fuellen, Start | Standardwerte, kaputte Datei als `.broken-<zeit>` daneben |

### simplemodels (Server + Client)
| ID | Aktion | Erwartet |
|---|---|---|
| QOLA-MOD-1 | Server/Einzelspieler starten | `config/simplemodels/config.json` und Ordner `catalog/` entstehen; Beispieldatei aus `examples/catalogue/tomato.json` in `catalog/` kopieren, `/simplemodels reload` |
| QOLA-MOD-2 | Fuehrungsbuch: Buch + Item-Rahmen craften (oder im Kreativ-Tab "Werkzeuge") | Buch mit 20 Seiten, oeffnet sich in der Spielersprache |
| QOLA-MOD-3 | Inventar oeffnen | Lesezeichen "Models" links am Rand (Namensschild); bei offenem Rezeptbuch oben am Rand |
| QOLA-MOD-4 | Browser oeffnen, suchen, Basis-Item-Filter und Sortierung wechseln, Mausrad blaettert | Liste filtert/sortiert, Vorschau-Tooltip zeigt Basis-Item -> Modell |
| QOLA-MOD-5 | Als OP: Basis-Item in den Amboss, Modell waehlen, "Zuweisen" | Ergebnis mit anderem Modell, Name/Haltbarkeit/Verzauberung bleiben, Kosten 1 Stufe, Entnehmen zieht die Stufe |
| QOLA-MOD-6 | Als Nicht-OP dasselbe | Zuweisen-Knopf nicht nutzbar bzw. kein Ergebnis (Standard: nur OP) |
| QOLA-MOD-7 | "Zuruecksetzen" im Browser | Item zeigt wieder das Vanilla-Modell, sonst unveraendert |
| QOLA-MOD-8 | `config.json` kaputt machen, `/simplemodels reload` | Modul bleibt aus (Browser zeigt "deaktiviert"), Spiel laeuft; Datei repariert + reload -> wieder an |
| QOLA-MOD-9 | Auf einem Server ohne Simple Models mit installiertem Client beitreten | Keine Fehler, Browser leer, Amboss normal |

## 4. Release-Punkte
| Mod | Version / Changelog-Entwurf | Kurzbeschreibung (Entwurf) | Langbeschreibung (Entwurf) | `fabric.mod.json` |
|---|---|---|---|---|
| simpleaesthetics | Vorschlag 1.0.0: "Erste Veroeffentlichung: Super-Mod, buendelt Simple Visuals, Sounds und Models mit je einem Schalter." | Buendel kosmetischer Verbesserungen: HUD, Sounds, Item-Modelle. | Simple Aesthetics bringt drei eigenstaendige Mods in einem Jar und je einen Ein-/Ausschalter. Jede Mod funktioniert auch allein. | `depends` simplevisuals/sounds/models `*`, simplelib >=0.3.0; Kontakt/Lizenz CC0 ok; LICENSE-Datei fehlt im Modulordner |
| simplevisuals | 1.0.0: "HUD-Pickup, Geschwindigkeitslinien, Schadenszahlen, Tooltips, Amboss-Farben, flache Rahmen-Items." | Optionale HUD-/Tooltip-/Partikel-Verbesserungen, alles abschaltbar. | Lokale, begrenzte Kosmetik fuer Vanilla (12 Immersions-Partikel, Pickup-Liste, Elytra-Hilfe, Karten-Tooltip, Amboss-Formatierung, Rahmen mit flachen Bloecken) mit Stufen und harten Grenzen. | `depends cloth-config` -> siehe Rueckfrage; Version 0.9.0 im Build |
| simplesounds | 1.0.0: "Leise Vanilla-Sounds passend zu den Simple-Visuals-Effekten." | Dezente lokale Umgebungsgeraeusche, folgt Simple Visuals. | Spielt die zwoelf Immersions-Effekte als leise Vanilla-Sounds (keine eigenen Audiodateien), mit Deckel, Cooldown und Stufen pro Effekt. | `depends cloth-config` (Rueckfrage); Version 0.1.0 |
| simplemodels | 1.0.0: "Server-genehmigte Item-Modelle per Amboss, Browser." | Server-freigegebene eigene Item-Modelle, per Amboss zuweisbar. | Admins legen Modell-Definitionen in einen Ordner; berechtigte Spieler weisen sie am Amboss zu. Streng geprueft, nichts vom Client vertraut, Browser mit Suche. | Version 0.1.0; `provides renamed` |

## 5. Rueckfragen / offen
1. **Cloth Config auf dem Server**: `cloth-config` aus `depends` nehmen (ModMenu-Eintraege per `isModLoaded` absichern), oder so lassen und in die Beschreibung schreiben? (simplevisuals-P2-1, simplesounds-P2-1)
2. **Mixin-Strenge**: simplevisuals (`required:true`, `defaultRequire 1`) wie bei den Opt-Mods auf "Bereich aus statt Absturz" umstellen? (simplevisuals-P3-4)
3. **Versionsschema** 0.9.0/0.1.0 -> alle 1.0.0? (ST-14)
4. Pflicht vor Veroeffentlichung: Sichtlauf mit Sodium und Iris (QOLA-VIS-9), echter Client-Start (`module-*-client-263`, `mixinaudit-client-263`).

## 6. Testergebnis (sbt-Lauf)
Lauf 31.10. auf sb-test (Commit ea41423fb, 1 sbt-Aufruf, Rerun fuer ein Ziel):
- C=0: Compile `:mc26_3:fabric` + vier Modul-Compiles gruen.
- T: Gametests 129/129 gruen (0 rot): Models integration 21/21, standalone 21/21; Visuals 22/22, 22/22; Sounds 20/20, 20/20; Aesthetics standalone 3/3. `Aesthetics integration` brach im ersten Lauf nach 9 s mit einem Gradle-Abbruch ab, ohne Tests zu starten (kein Testfehler, vermutlich Gradle-Konflikt in der geteilten Schlange); der Einzel-Rerun `:integration:runSimpleaestheticsGameTest` lief gruen (3/3, "All 3 required tests passed", BUILD SUCCESSFUL).
- K=0: Client-Smokes unter `xvfb-run -a`: Models 10/10, Visuals 6/6, Sounds 3/3, Aesthetics 1/1 (echter Client, Welt betreten, Screens fotografiert). Hinweis des Laufs: je 1659 "Fehlende Textur/Modell"-Meldungen im Client-Log, in allen vier Logs gleich (stammt vermutlich aus dem gemeinsamen Spiel/Vanilla-Umfeld; nicht ausgewertet).
- M=0: `check_client_mixins.py`: 660 Mixins geprueft, 0 Probleme (inkl. aller 17 Visuals-, 7 Models-, 1 Sounds-Mixins).
- L=0: `check_lang_keys.py`: 52 Module, jeder Key EN und DE.
Nicht getestet: Sodium/Iris, Forge/NeoForge, ein Lauf von `mixinaudit-client-263` (Hauptsitzung nach Merge).
