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

## Umsetzung 13.10. (Branch `claude-wither`)
Besitzer: „Wither wie im Konzept, nur halt stärker (mehr Leben/Phasen/Angriffe, fair angekündigt), droppt das Item für den Biom-Pinsel.“ Offene Fragen selbst entschieden (vanillanah, fair, angekündigt):

- **Modul:** beides in **Simple Mobs** (`simplemobs`), nicht Wither in Mobs + Pinsel in SB. Grund: das Witherherz ist Pflichtzutat; läge der Pinsel in SB, wäre SB allein ohne Bezugsweg (Prinzip 1). So bleibt Simple Mobs allein voll spielbar, Claims laufen über `framework` `Protection` (gebündelt).
- **Frage 1 Beschwörung:** Netheritblock unter dem Fuß des T (Empfehlung). Wird mit verbraucht. Ohne Netherit: Vanilla-Wither. Config `witherSummonEnabled`.
- **Frage 2 Werte:** 600 Leben, Rüstung 8, 100 XP, erwacht wie Vanilla (11 s) und heilt dabei voll. Drei Phasen; jeder Spezialangriff hat ≥1 s Warnung (Klang + Partikel genau dort, wo er trifft):
  - Phase 1 (>50 %): Vanilla-Schädel + **Doppelsalve** alle 5 s (Seitenköpfe glühen 1 s mit Seelenfeuer, dann 2 Schädel gleichzeitig).
  - Phase 2 (≤50 %): Vanilla-Schild + alle 20 s **3 Witherskelette** (Seelenflammen markieren 2 s die Plätze), höchstens 6 in der Nähe, ohne Beute (kein Schädel-Farmen).
  - Phase 3 „Welken“ (<15 %): **Nova** alle 12 s (Seelenrauch-Ring 8 Blöcke warnt 2 s, wer drin bleibt: Ausdörrung II 6 s + Rückstoß) und **Welken** alle 30 s (2 s Asche vorher): Gras/Farn/Blumen → toter Busch, natürliches Laub fällt, jeder 5. Grasblock → grobe Erde. Gesetztes Laub, Feldfrüchte, Setzlinge, alles andere bleibt. Braucht `mobGriefing`, Config `witherWilt`.
  - Phasenwechsel kündigt sich mit dem Wither-Schrei an.
- **Drop:** genau 1 Witherherz + Netherstern, Plünderung wirkt nicht; nur der Verdorrte Wither.
- **Frage 3 Radius:** Strich = 3×3×3 Biomzellen (12 Blöcke breit, Config `biomeBrushRadius` 0–2). Verzauberung „Weite“ **nicht** gebaut (Config reicht, keine neue Verzauberung); verzauberbar mit Haltbarkeit/Reparatur (`#enchantable/durability`).
- **Frage 4:** nur über den Wither (kein Truhenloot). **Frage 5:** Namen „Biom-Pinsel“/„Witherherz“, Boss „Verdorrter Wither“ (EN Blighted Wither, Wither Heart, Biome Brush).
- **Pinsel:** Rezept Witherherz / Netheritbarren / Pinsel (eine Spalte), Haltbarkeit 512, Reparatur Netheritbarren. Haupthand Pinsel, Nebenhand Material; 1 Material + 1 Haltbarkeit je Strich, Kreativ gratis; gedrückt halten malt weiter (1 Strich je 5 Ticks). Zuordnung als Daten `data/<ns>/biome_brush/*.json` (Items oder Item-Tags, Partikelfarbe), 23 Einträge (Oberwelt 16, Nether 5, End 2), nach /reload neu gelesen. **Pipette (Schleichen) nicht gebaut** (ohne Text nicht sinnvoll anzeigbar, Regel 4).
- **Grenzen/Sicherheit:** Biome nur in ihrer Dimension (`#is_overworld/#is_nether/#is_end`, sonst BiomeSource), Höhlenbiome (`simplemobs:biome_brush_locked`) und Bauwerke nie. Jede Zelle wie ein Blockeingriff geprüft: `framework` `Protection` (Claims), Spawnschutz, Abenteuermodus; verweigerte Zellen bleiben, verweigerte Klickzelle = kein Strich, nichts verbraucht (Rauch statt Text). Config `biomeBrushEnabled`, `biomeBrushOpsOnly`. Chunks werden per Vanilla-Biome-Paket neu gesendet.
- **Texturen:** Witherherz und Biom-Pinsel eigene Pixel-Art (`tools/textures/wither_textures.py`); Haut des Verdorrten Withers aus der Vanilla-Haut abgeleitet (Asche-Rampe, Seelen-Augen) → nur beim Build (`vanilla_derived.py`), nie eingecheckt.
- **Geld:** Witherherz und Pinsel nicht handelbar (Boss-Beute, wie die übrigen Simple-Mobs-Drops). Simple Mobs bekommt ein Handbuch (`module_guides.py`).

## Umsetzung 13.10. Teil 2 (Branch `claude-wither2`, Besitzer N50)
Besitzer: Beschwörung per Netherit-Helm auf dem mittleren Schädel, zweite Stufe mit Enderit-Helm, gerüstete Witherskelette bei wenig Leben, starker Nova-Rückstoß, neue Drops. Offene Details selbst entschieden:

- **Beschwörung:** Die alte Netheritblock-Figur ist entfernt. Vanilla-T aus Seelensand/-erde, der **mittlere** Witherskelett-Schädel steht; Rechtsklick mit dem Helm setzt ihn auf (Helm wird verbraucht, Kreativ behält ihn). Danach die übrigen Schädel wie Vanilla; der dritte ruft den Boss. Der Helm geht nur auf den mittleren Schädel einer noch **unvollständigen** Figur (sonst Rauch, Helm bleibt in der Hand); ohne Helm kommt der Vanilla-Wither. Ist die Figur schon komplett, war der Vanilla-Wither schon da – damit ist „Helm vor dem dritten Schädel“ von selbst erzwungen.
- **Sichtbarkeit/Speicher:** Der Helm sitzt als Item-Display (Vanilla-Entity, Tag `simplemobs.wither_crown`) auf dem Schädel, wird mit der Welt gespeichert. Wird der Schädel abgebaut, fällt der Helm (mit Verzauberungen) als Item heraus (Prüfung alle 10 Ticks, Mixin auf `Display.tick`).
- **Stufe 2 „Abgrund-Wither“ (EN Abyssal Wither, `simplemobs:abyssal_wither`):** Helm aus dem Tag `simplemobs:abyssal_wither_crowns` (optional `simplebuilding:enderite_helmet`; ohne SimpleBuilding leer → Stufe 2 nicht beschwörbar, Prinzip 1). Werte: 1000 Leben (Stufe 1: 600), Rüstung 14 (8), 200 XP (100); Salve aus allen **drei** Köpfen, Angriffspausen ¾ so lang, 4 Skelette je Ruf (Obergrenze 8); **Warnzeiten unverändert** (fair). Eigene Haut (End-Lila-Rampe, violette Augen) aus der Vanilla-Haut, nur beim Build.
- **Gerüstete Skelette:** ab ≤ 30 % Leben tragen die gerufenen Witherskelette volle Rüstung + Schwert: Stufe 1 Eisen, Stufe 2 Diamant; weiterhin ohne Beute (Drop-Chance 0).
- **Nova-Rückstoß:** Nova gibt es nur in Phase 3 (< 15 %); Stoß jetzt 2,8 waagerecht + 0,6 hoch (Stufe 2: 3,4 + 0,7), vorher 1,2 + 0,4; ignoriert Rückstoßresistenz.
- **Drops:** 1 Witherherz + 1–2 Nethersterne (Vanilla-Stern + 50 % ein zweiter) + 3–5 Nuggets: Stufe 1 erstes Item aus `c:nuggets/netherite` (SimpleBuilding), ohne Nugget-Mod ersatzweise 1 Netheritplatte; Stufe 2 aus `c:nuggets/enderite`, ohne Mod keine. Plünderung wirkt nicht.
- **Testzentrale:** beide Boss-Zellen bekommen eine halbe Wither-Figur (Mitte + ein Schädel), Rahmen mit Helm (Netherit bzw. Enderit, falls vorhanden) und Witherskelett-Schädel.
- **Tests:** `wither_summon` (Netherit, Enderit/Stufe 2, Seitenschädel abgelehnt, ohne Helm Vanilla, Helm fällt bei abgebautem Schädel, Config aus), `wither_order` (Figur komplett → Helm abgelehnt), `wither_drop` (beide Stufen), `wither_minion_armor`, `wither_nova_push`, `wither_values` (Stufe 2).
