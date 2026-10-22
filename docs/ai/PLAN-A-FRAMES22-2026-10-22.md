# Plan a-frames22 (Queue TZ22-7, 22.10.)

Auftrag: Druckplatten (und ähnliche flache Block-Items) im Item-Rahmen so zeigen wie im Inventar
(schräg von oben, Item-Modell-Ansicht) statt flach. Client-Option zum Ausschalten mit Tooltip EN/DE.
Nur MC 26.3 Fabric (Regel 22.10.).

## Ist-Zustand
- Vanilla `ItemFrameRenderer.extractRenderState` löst das Item mit `ItemDisplayContext.FIXED` auf.
  Block-Modelle haben dort keine Drehung (`block/block.json` fixed = [0,0,0]): eine Druckplatte
  (1/16 hoch) erscheint als dünner Strich am unteren Rand, Teppich/Falltür ebenso.
- Vorhandene Rahmen-Mixins: SB `mixin/client/ItemFrameEntityRendererMixin` (leerer Rahmen sichtbar),
  Simple Maps `ItemFrameRendererMixin` (Wegfinder-Karte, setzt `mapId`). Beide `extractRenderState` TAIL.
- Simple Tools (b-itemframes) hat nur Server-Gameplay (Schere/Wabe/Axt/Drehen) mit Server-Config
  (`ToolsConfig`, an Clients gesendet). Keine Client-Config, kein Rahmen-Renderer.
- Simple Visuals: reiner Client-Kosmetik-Mod mit lokaler Config (`config/simplevisuals.json`),
  Optionen werden per Reflexion in Bildschirm, ModMenu, Wiki und Tests übernommen (`ConfigOptions`),
  Super-Mod-Schalter `Visuals.active(option)`.

## Entscheidung Modul
**Simple Visuals**: reine Darstellung, rein clientseitig, braucht eine *Client*-Option. Simple Tools
hat nur Server-Config; dort eine Client-Option einzuführen wäre ein neuer Mechanismus.
Kein Bezug zu Simple-Tools-Code nötig → keine Modul-Kopplung (Prinzip Modul-Unabhängigkeit).

## Auswahl „welches Item wird schräg gezeigt“ (begründet)
Generisch statt Liste, damit auch SB-/Modul-Druckplatten und -Teppiche greifen:
1. Item ist ein `BlockItem`,
2. Grundzustand-Form des Blocks (`getShape(EmptyBlockGetter, 0)`) ist nicht leer, liegt am Boden
   (minY = 0), ist höchstens 6/16 hoch und mindestens 8/16 breit in X und Z,
3. zur Laufzeit zusätzlich: Item-Modell ist ein Block-Modell (`usesBlockLight()`), keine Sprite.

Ergibt: Druckplatten (alle), Teppiche (Wolle, Moos, blasses Moos), Falltüren (3/16),
Tageslichtsensor (Schneeschicht nicht: Grundzustand hat leere Form). Nicht: Knöpfe (Grundzustand an der Wand, maxY 10/16, im Rahmen
schon gut lesbar als Quader), Schienen/Redstone/Seerose/Verstärker (Sprite-Items, Regel 3),
Stufen (8/16), Köpfe (8/16, Spezialmodell). Ergebnis je Item gecacht.

## Umsetzung (Dateien)
- `modules/simplevisuals/shared/java/com/simplevisuals/FrameTilt.java` (neu, ohne Client-Klassen):
  `flat(ItemStack)` mit Cache, Konstanten.
- `.../mixin/ItemFrameRendererMixin.java` (neu, client):
  - `extractRenderState` TAIL: wenn `Visuals.active(enableTiltedFrameItems)`, keine Karte, Item flach
    und Block-Modell → Item erneut mit `ItemDisplayContext.GUI` auflösen, Flag + Wandabstand
    (aus `getModelBoundingBox().minZ`) im Render-State merken.
  - `submit` vor `ItemStackRenderState.submit`: um 180° Y drehen (GUI-Ansicht schaut zum Betrachter,
    im Rahmen liegt der Betrachter auf -Z) und so weit nach vorn schieben, dass nichts in Wand/Rückwand ragt.
- `.../mixin/ItemFrameRenderStateMixin.java` + Duck-Interface `client/TiltedFrameState` (Flag + Versatz).
- `SimplevisualsConfig.Visuals.enableTiltedFrameItems = true` (Tab general), Lang EN/DE Name + Tooltip
  mit „Default: true“/„Standard: true“, `simplevisuals.mixins.json` client-Liste.
- Rahmen-Drehung (8 Stufen) bleibt wie Vanilla erhalten.

## Risiken
- Ohne Client-Lauf nicht sichtbar geprüft: Blickrichtung (180°-Drehung), Abstand zur Wand,
  Licht der GUI-Kontext-Darstellung in der Welt. → Client-Sicht offen (Memory + Queue).
- Mixin-Ziel `submit`/`extractRenderState` Signaturen aus 26.3-Quellen (/root/mcsrc263) übernommen.

## Tests (ohne Client)
- Gametest in vorhandenem `VisualsTests.config` (läuft auf allen Adaptern, keine Adapter-/Katalogänderung
  für andere Loader nötig): Default an, Auswahl-Logik: Druckplatten (Eiche, Stein, leicht/schwer),
  Teppich (Wolle, Moos), Falltür, Tageslichtsensor, SB-Diamant-Druckplatte (falls geladen) → schräg;
  Knopf, Stufe, Stein, Stock, Schädel, Fackel, Luft → Vanilla-Ansicht; Optionen-Anzahl 50 → 51.
- `configAndLanguageCompleteness` deckt Name/Tooltip/Default EN+DE automatisch ab.
- Verifikation: `/root/sbt a-frames22` Compile `:mc26_3:fabric:compileJava` + `:modules:simplevisuals:fabric:compileJava`,
  Targets `module-simplevisuals-fabric-263`, `module-simplevisuals-standalone-fabric-263`, Generatoren, `check`.
- Vorschau: /root/sb-tools-a/previews/frames22-preview.html (Skizze vorher/nachher).
