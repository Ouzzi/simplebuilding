# Gemischte Achtelzellen (Queue N48 „Octet-Blöcke untereinander kombinierbar“, 2026-10-13, Branch claude-octets-mix)

## Ist-Stand
- `OctetCellBlock`: acht Bits `o<x><y><z>` + `waterlogged` im Blockzustand, kein Block-Entity. Ein Block je Material
  (`<key>_octet`, ~170 `MaterialOctetBlock`, `melon_octet`) bzw. `checker_octet` mit `color`. Modelle gebacken
  (Multipart je Bit, Cullfaces außen), Form/Kollision/Licht aus der Bitmaske.
- Setzen (`MaterialOctetItem`, `CheckerOctetItem`): nur in eine Zelle **desselben** Materials, sonst Ablehnung.
- Abbauen: ganze Zelle, Drops aus dem Zustand (Anzahl Bits); Schleichen + leere Hand nimmt ein Achtel heraus.
- Hammer zerlegt nur Vollblock/Treppe/Stufe in eine reine Zelle (Zellen selbst schnitzt er nicht). Steinmetz nur Items.
- Blaupause speichert nur Zustände (Achtel haben kein BlockItem → nur Kreativ); Baustab setzt nur BlockItems.

## Entscheidung: Hybrid „rein = Zustand, gemischt = Block-Entity“
Material je Achtel im Zustand ist unmöglich (170⁸). Darum:
- **Reine Zellen bleiben unverändert** (Zustand, gebackenes Modell, kein BE) – keine Migration, alte Welten laden 1:1,
  große Bauten aus einem Material kosten weiter nichts.
- **Neuer Block `mixed_octet`** (`MixedOctetBlock extends OctetCellBlock implements EntityBlock`) nur für Zellen mit
  ≥ 2 Materialien: Bitmaske + `waterlogged` + `light` (0–15, Maximum der Teile) im Zustand → Form, Kollision, Umriss,
  Licht-Verdeckung und Wasser wie bei reinen Zellen ohne BE-Zugriff. Das BE (`MixedOctetBlockEntity`) hält je Achtel den
  Material-Zustand (Zustand einer reinen Zelle ohne Bits, z. B. `oak_octet` oder `checker_octet[color=lapis]`), als
  Palette + 8 Indizes gespeichert, zum Client per Update-Paket.
- **Zentrale Normalisierung** `OctetCells.set(...)`: 0 Teile → Luft/Wasser, 1 Material → reine Zelle (BE weg),
  ≥ 2 → `mixed_octet` + BE. Wird eine gemischte Zelle wieder einfarbig, wird sie automatisch wieder gebacken.
- **Rendering gemischt**: BER, der je Material die *vorhandenen gebackenen Modelle* der reinen Zelle mit der Teilmaske
  über `submitMovingBlock` (wie der Kolben) zeichnet – mit Weltlicht aus der Licht-Engine (Außenflächen haben
  Cullfaces → Licht vom Nachbarn, keine schwarzen Vollzellen), keine eigenen Modelle/Texturen. Begründung gegen ein
  eigenes Baked-Model: das bräuchte je Loader eigene APIs (Fabric `FabricBlockStateModel`, NeoForge `ModelData`,
  Forge) plus Modell-Hooks; der BER ist loader-neutral, Sodium-fest und betrifft nur gemischte Zellen.
  Kosten: Nachbarflächen werden nicht gecullt (nur Überzeichnung), keine Bruch-Risse auf gemischten Zellen.
- **Abbauen gezielt** (alle Achtelzellen, rein und gemischt, durch Spieler): nur das angeschaute Achtel
  (Blickstrahl gegen die Zellform). Muster wie N37-Kleinteile: `playerWillDestroy` merkt Rest + Drop (ThreadLocal),
  `destroy` legt den Rest zurück, `getDrops` liefert nur das Achtel (loader-fest, auch wenn Forge den Originalzustand
  weiterreicht). Abbaugeschwindigkeit und Werkzeugpflicht nach dem Material des Achtels. Explosion/Kolben/
  `Level#destroyBlock` ohne Spieler: alles fällt wie bisher.
- **Setzen**: Achtel-Items setzen in jede Achtelzelle (rein oder gemischt) mit freiem Bit; anderes Material → Mischung.
  Schleichen + leere Hand nimmt das angeklickte Achtel (Material des Achtels) heraus.
- **Hammer/Steinmetz**: unverändert (Hammer zerlegt weiter Vollblöcke in reine Zellen; gemischte Zellen schnitzt er
  nicht). **Blaupause**: der Scanner nimmt für gemischte Zellen die reine Zelle des häufigsten Materials (mit voller
  Maske) – verlustbehaftet, aber ohne Code-Format-Änderung und nie ein BE-loser `mixed_octet`. **Baustab**: setzt keine
  Achtel (keine BlockItems) – nichts zu tun. **Kolben**: gemischte Zellen sind (BE) unbeweglich wie Truhen.
- **Mehrspieler**: Server autoritativ; BE-Daten per `ClientboundBlockEntityDataPacket`; Client-Vorhersage beim Abbau
  über denselben `destroy`-Pfad.

## Tests
GameTests (`*octet*`): Mischen (Holz + Stein + Schach-Farbe in einer Zelle, BE-Inhalt, Normalisierung zurück zu rein),
gezieltes Abbauen mit echtem `ServerPlayerGameMode#destroyBlock` (rein + gemischt, Drops genau ein Achtel, Rest
bleibt), Explosion/`destroyBlock` droppt alle Teile je Material, Migration (alte reine Zelle unverändert, bekommt ein
fremdes Achtel → gemischt; BE speichern/laden), Blaupausen-Scan. Dazu `data_integrity*`, `blueprint*`.
Client-Screenshot nach `~/previews/octets-mix/`.
