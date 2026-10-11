# Konzept: Stärkerer Wither + Biom-Pinsel (N26, 2026-10-10)

Queue: „Konzept stärkerer Wither: droppt ein Item, das später für ein Biom-Werkzeug dient (Biom-Pinsel: Pinsel in der Haupthand, biomspezifisches Material in der Nebenhand; Haltbarkeit, verzauberbar).“ Nur Konzept, nichts umgesetzt. Vorschau: `<preview-dir>/concepts/biom_pinsel.png`, `witherherz.png`. Verwandt: `KONZEPT-FARBPINSEL-RESPAWN-2026-10-09.md` (gleiche Bedienung wie Farbpinsel).

## A. Stärkerer Wither („Verdorrter Wither“)
- **Beschwörung:** wie Vanilla (T aus Seelensand/Seelenerde + 3 Schädel), aber der Block **unter dem T** (Fußpunkt) ist ein Netherit-Block. Ohne ihn erscheint der normale Wither. Kein neues Item zum Starten, nur Rohstoffdruck. Alternative siehe Frage 1.
- **Werte (Vorschlag):** 600 Leben (Vanilla 300, Schwierigkeit Normal), Panzerung 8. Drei Phasen: >50 %: Schädelsalven wie Vanilla, aber 2 Schädel gleichzeitig. <50 %: Schildphase wie Vanilla; zusätzlich ruft er alle 20 s 3 Wither-Skelette. <15 %: „Welken“: verwelkt Pflanzen und Laub im 12-Block-Radius (Blöcke werden nach Vanilla-Verhalten zu totem Busch/Seelenerde-Flecken; nur Config-fähig, kein Blockabbau von Gebäuden).
- **Arena-Schutz:** Welken zerstört nie Bauwerke (Config `witherWilt=true`, Standard an nur für Pflanzen/Laub/Gras); Wither-Schädel behalten Vanilla-Zerstörungsregel (`mobGriefing`).
- **Drop:** zusätzlich zum Netherstern **ein Witherherz** (Kern der Biom-Pinsel; 1 pro Kill, nicht per Looting). Keine Item-Farm: Nur der verstärkte Wither droppt es.
- **Modul:** Simple Mobs (Wither) + SimpleBuilding (Biom-Pinsel). Der Biom-Pinsel liegt in SimpleBuilding, der Wither in Simple Mobs; Witherherz ist Pflichtzutat (Modul-Unabhängigkeit: Biom-Pinsel bleibt im Creative-Tab erreichbar).

## B. Biom-Pinsel
- **Item:** Pinsel (Pinselkopf aus Witherherz + Stab). Rezept: Witherherz + Pinsel (Vanilla-Bürste oder Farbpinsel) + Netherit-Barren. Haltbarkeit 512, reparierbar mit Netherit-Barren, **verzauberbar** (Haltbarkeit, Reparatur, Haltbarkeit-Zuwachs).
- **Bedienung:** Pinsel in der Haupthand, **biomspezifisches Material in der Nebenhand** (1 Item pro Strich). Rechtsklick auf Block: setzt das Biom der 4×4×4-Biomzelle am Ziel (Vanilla speichert Biome in 4×4×4-Zellen) auf das zum Material gehörende. Ziehen bei gehaltener Taste malt fortlaufend (max. 1 Strich/5 Ticks). Sneak-Rechtsklick: Material-Biom aufnehmen (Pipette, kostet kein Material).
- **Zuordnung (Datenpaket `simplebuilding:biome_brush`, Item oder Tag → Biom):** Grasblock → Ebene; Sand → Wüste; Schnee → Verschneite Ebene; Myzel → Pilzinsel; Seelensand → Seelensandtal; Kirschlaub → Kirschhain; Dschungelholz → Dschungel; Rotsand → Ödland; Podsol → Taiga; Mangrovenwurzel → Mangrovensumpf; Chorusblume → Außenends (Endenmoore). Modpacks erweitern per Datenpaket.
- **Grenzen:** Biome nur innerhalb der eigenen Dimension (Oberwelt-Biome nur Oberwelt, Nether-Biome nur Nether). Strukturen und Höhlenbiome nicht änderbar (Tiefschiefer-Höhlen/Deep Dark ausgeschlossen). Config: Radius (1 Zelle Standard), Kosten pro Strich, Creative unbegrenzt (Besitzer wie Farbpinsel: Kreativ unbegrenzt, Pinselstrich-Partikel/Klang ja).
- **Effekte:** Biomfarbe (Gras/Laub/Wasser) ändert sich nach Chunk-Neusendung (Client sieht es sofort per Biome-Update-Paket); Wetter (Schnee/Regen), Mob-Spawns und Pflanzenwachstum folgen dem neuen Biom. Strichpartikel in Biomfarbe.
- **Verzauberung (Vorschlag):** neue Verzauberung „Weite“ I–III: Radius 1→3 Zellen, Kosten steigen.
- **Technik/Risiko:** Biom-Änderung ist Chunk-Daten + Netzwerkpaket; Neustart-fest, aber Chunks müssen neu gesendet werden; Mehrloader-Pfad testen (Fabric/NeoForge/Forge Chunk-Resend). Bestehende Welt-Biome werden dauerhaft verändert, daher Config `biomeBrush=true` und Server-OP-Schalter. Performance: Strich = 1–27 Zellen, vernachlässigbar.

## Offene Besitzer-Fragen (Empfehlung zuerst)
1. Wither-Beschwörung: Netherit-Block als Fußpunkt (Empfehlung) oder anderes Zusatzitem/Struktur (z. B. Leuchtfeuer-Pyramide)?
2. Werte: 600 Leben + Phase 3 „Welken“ (Empfehlung) oder nur mehr Leben und Schaden?
3. Biom-Pinsel Radius 1 Zelle + Verzauberung „Weite“ (Empfehlung) oder festes Raster?
4. Biom-Pinsel auch im Überleben ohne Wither (z. B. Truhen-Beute der Bastion)? (Empfehlung: nein, nur Wither.)
5. Name: „Biom-Pinsel“ (Empfehlung) und „Witherherz“ ok?
