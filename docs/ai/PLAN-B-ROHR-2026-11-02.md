# Plan B-ROHR (ROHR33, 02.11.2026) - Rohr-Fenster: Kompass und drei Zustaende

Besitzer-Wortlaut: oben Norden, links/rechts Westen/Osten, unten Sueden, daneben Oben/Unten; Klick = hellrot = Ausgang; naechster Klick = aus (weder Ein- noch Ausgang); weiterer Klick = wieder Eingang; dann wieder Ausgang. Auslegung A: Standard-Kompass (W links, O rechts), Zyklus Eingang -> Ausgang -> Aus -> Eingang, mehrere Ausgaenge erlaubt.

## Ist-Zustand
- `ItemPipeBlock.FACING` = der EINE Ausgang (Blockstate, Modell zeichnet Pfeil), sechs Arm-Properties. `ItemPipeBlockEntity.push/pull`: push nur nach FACING, pull von jeder anderen Seite. `PipeScreen`: Sechseck mit 6 Knoepfen, Klick = "genau diese Seite ist Ausgang". Menu: Daten FILTER_MODE/OUTPUT/TIER.

## Entwurf
- BE speichert `sides[6]` (IN=0, OUT=1, OFF=2), NBT-Key `Sides` (2 Bit je Seite in einem Int), plus `PushTo` (Round-Robin-Zeiger).
- FACING (Blockstate) bleibt: zeigt auf den ersten Ausgang (Pfeil im Modell), Arme folgen den Zustaenden: Ausgang immer Arm, Eingang nur mit Behaelter, AUS nie. Kein Modell-/Blockstate-Umbau noetig.
- Transport: push reihum ueber alle OUT-Seiten (Round-Robin ab `PushTo`, voller/fehlender Ausgang wird uebersprungen); pull nur von IN-Seiten; AUS: `canPlaceItemThroughFace`/`canTakeItemThroughFace` false (auch Trichter von aussen), nie Ziel, nie Quelle.
- Klick-Zyklus je Seite: IN -> OUT -> OFF -> IN. Seite, auf die die Stufe nicht zeigen darf (oben im Basis-Rohr): OUT wird uebersprungen (IN <-> OFF).
- Altes "Ausgang drehen" (Schleichen, Menuetaste 1) bleibt: setzt genau diese Seite als einzigen Ausgang (andere Ausgaenge -> Eingang, AUS bleibt).
- Menu: neues Datenfeld SIDES (4 Felder). Buttons 2..7 = Zyklus der Seite.

## Round-Robin: Begruendung
Reihum verteilt gleichmaessig, ist deterministisch und erzeugt keinen Rueckstau am ersten Ziel; "alle Ziele gleichzeitig fuellen" waere bei 1 Item/Schritt unmoeglich, "immer erstes Ziel" wuerde die zweite rote Seite wirkungslos machen. Gleiches Muster wie der bestehende Pull (`pullFrom`). Voller Ausgang: naechster wird versucht, nichts blockiert.

## Migration
Alte Welten haben kein `Sides`. Beim Laden: `sides = null` -> beim ersten Zugriff aus FACING abgeleitet (FACING = OUT, alle anderen IN) und beim naechsten Speichern geschrieben. Das ist exakt das alte Verhalten. In-Welt-Upgrade (`Pipes.upgradeInPlace`) uebertraegt die Zustaende.

## UI
Kompass-Kreuz (N oben, W links, O rechts, S unten; Buchstaben N/O/S/W bzw. N/E/S/W) bei x=126, daneben Spalte Oben/Unten (Pfeile) bei x=158. Eingang: erhabener Knopf in Palettenfarbe; Ausgang: eingedrueckt, hellrot 0xFF9A8F mit Tinte 0x3A0804 (Kontrast ca. 9:1); Aus: eingedrueckt, grau 0xA0A0A0 mit Tinte 0x262626 (ca. 5,5:1). Tooltip je Seite 3-4 Zeilen (Zustand, Wirkung, was ein Klick tut), EN/DE, je Zeile <= 48 Zeichen.

## Risiken
Mit 0 Ausgaengen bleibt FACING (und damit die Pfeil-Modellvariante) auf der zuletzt gueltigen Seite; Arme folgen den Zustaenden, ein Pfeil an einer Eingangs-/AUS-Seite waere nur Modell-Optik (im Klick-Test pruefen, ggf. Rueckfrage). AUS-Seite blockiert auch Trichter-Einwurf (gewollt).

## Verifikation
Gametests (Zyklus, Round-Robin, AUS nimmt nichts, Migration + Persistenz + Upgrade), Compile, Modul-Gametests + standalone, check_client_mixins, echte-Maus-Test im Fenster mit Screenshots (vorher/nachher), Vorschau-HTML.
