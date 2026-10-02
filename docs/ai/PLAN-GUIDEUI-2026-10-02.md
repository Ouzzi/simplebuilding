# Plan GUIDEUI 2026-10-02 (Branch claude-guideui)

Quelle: `.claude/QUEUE.md`, Abschnitt „Queue-Ende (Besitzer 2026-10-02 abends, mit Screenshots)“.

## Ist-Zustand
- `GuideBookScreen.extractRenderState`: gesperrte Lesezeichen bekommen nach dem Icon ein
  `g.fill(iconX, ty+3, iconX+16, ty+19, 0x88777777)` (26.3) – das ist die graue 16x16-Box über den Icons
  (rechte und linke Leiste). Das gesperrte Lesezeichen-Sprite (u=64) ist schon grau-braun.
- Klick auf gesperrtes Lesezeichen tut nichts (26.3). Freischaltung: `GuideUnlocks` merkt offene Tabs als
  Entity-Tag (`simplebuilding.guide_tab.*`) und vergibt das Tab-Advancement; Client kennt Stand über `GuideStatePayload`.
- Advancement-Tab „The Two Shelves“ = `mc26_3/overlay/resources/data/simplebuilding/advancement/guides/root.json`,
  `background: "minecraft:textures/gui/advancements/backgrounds/stone.png"`. Seit 1.21.5 ist der Hintergrund ein
  ClientAsset (`<ns>:<pfad>` → `assets/<ns>/textures/<pfad>.png`), daraus wird `textures/textures/…png.png` → Fehltextur.
  Alle anderen Tabs (Datagen `ModAdvancementProvider`, Module simplefun/simpleriding) sind gültig.

## Umsetzung
1. Graue Box: das `fill` über gesperrten Icons (26.3-Zweig) entfernen. Gesperrt bleibt erkennbar am grauen Sprite,
   fehlendem Farbstreifen und Tooltip. 26.2-Zweig (Altbuch, braune Tönung) bleibt unverändert, damit die 26.2-Linie
   nicht ohne Port-Run umgestaltet wird.
2. Kreativ-Freischalten:
   - Neues C2S-Payload `GuideUnlockPayload(Identifier tab)` (shared), registriert Fabric (`ModMessages`), NeoForge, Forge.
   - Server `GuideUnlocks.creativeUnlock(player, id)`: nur wenn `MEGA_GUIDES`, Spieler `isCreative()`, Tab existiert und
     `Access.RECIPES` (OPERATOR-Tabs bleiben Operator-Sache, ALWAYS ist eh offen) → Tag setzen, Advancement vergeben,
     `refresh(player, true)`. Rückgabe boolean (Tests). Handler in `ModMessageHandlers.handleGuideUnlock`.
   - Client: Klick auf gesperrtes Lesezeichen im Kreativmodus wählt es aus; unter dem Buch erscheint ein Vanilla-`Button`
     „Unlock anyway: <Titel>“ / „Trotzdem freischalten: <Titel>“. Klick sendet das Payload; der Server schickt den neuen
     Stand, Screen relayoutet über `clientVersion`. Tooltip des gesperrten Tabs nennt im Kreativmodus zusätzlich „Klicken …“.
   - Lang-Keys EN/DE in `src/main/resources` und `mc26_3/overlay/resources`.
3. Advancement-Hintergrund: Pfad auf `minecraft:gui/advancements/backgrounds/stone` korrigieren. Neuer GameTest
   `AdvancementTreeTests.everyTabBackgroundNamesAnExistingTexture`: für alle geladenen Advancements außerhalb `minecraft`
   mit `background`: Format (kein `textures/`-Präfix, keine `.png`-Endung) und, wo der Classpath die Texturen sieht,
   Existenz von `assets/<ns>/textures/<pfad>.png`.
4. Guide-Buch-Texturen: 10 Vorschläge A–J als Generator `tools/textures/guide_books_10_proposals_2026_10_02.py`,
   Vorschau `C:\Users\o_o\code\minecraft-mods\previews\guide-buecher-10-vorschau.png`. NICHT einbauen.

## Tests / Gates
- GameTests: `GuideBookTests.creativePlayersMayUnlockALockedTabOthersMayNot`, neuer Advancement-Test; Katalog + Fabric-Adapter.
- `run.py --targets fabric-263` (+ Filter), dann `neoforge-263`; Compile `:compileJava :neoforge:compileJava`, `-Pforge263=true :mc26_3:forge:compileJava`.
- Nicht testbar ohne Client: Optik der Lesezeichen und des Knopfs (kein Client-Start erlaubt).

## Annahmen
- „Kreativ“ = `ServerPlayer#isCreative()`; Zuschauer nicht. Operator-Tab (Server Admin) wird nicht per Kreativ geöffnet.
- Knopf erscheint erst nach Klick auf den gesperrten Tab (ein Knopf für den gewählten Tab), damit die Tab-Leisten frei bleiben.
