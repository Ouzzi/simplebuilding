# Plan P5/P6 – Guides je Mod, Tabs freigeschaltet über Rezepte (2026-10-02, überarbeitet)

Besitzer: Backlog P5, P6, L1 in `docs/ai/BACKLOG-2026-10-01.md`; Kriterium vom Besitzer (2026-10-02):
„muss ähnlich wie vorher funktionieren: verschiedene Tabs, jeder Tab gegatet durch ein Crafting-Rezept, so simpel wie
möglich für diese Stufe“ – Werkzeuge = das erste Werkzeug, das man herstellt; Pads = die Druckplatten der Mod (oder
Druckplatten allgemein). Sobald eins dieser Rezepte freigeschaltet ist, ist der Tab frei; auch das Grund-Item des Themas
zählt.

## Ist-Zustand (26.3, `McVersion.MEGA_GUIDES`)

- Zwei Hub-Items (`guide_book`, `guide_book_vanilla_start`), Kapitel = Tabs, Bitmaske `GUIDE_CHAPTERS` (20 Bit) am Stapel.
- Freischalten heute: Tab anklicken → `GuideUnlockPayload` → `GuideUnlocks.unlock` **verbraucht ein Schlüssel-Item**
  (`GuideBooks.keyItem`) und vergibt `simplebuilding:guides/<topic>`.
- Rezept des Hubs: Buch + Schlüssel-Item. Kein Start-Geschenk. FTB-Quests: keine Rewards. Module haben keine Bücher.

## Ziel

1. **Tabs bleiben wie heute** (gleiches Layout, gleiche Reihenfolge, gesperrte Tabs grau).
2. **Freischalten ohne Verbrauch**: ein Tab ist frei, sobald der Spieler **eines der Tor-Rezepte des Tabs im Rezeptbuch
   freigeschaltet hat** (Vanilla `RecipeBook`, serverseitig `ServerRecipeBook#contains`). Tor-Rezepte = das einfachste
   Rezept der Stufe, plus die Rezepte des Grund-Items des Themas. Ein gesperrter Tab nennt im Tooltip das Item
   („Schalte frei: Steinmeißel herstellen“).
3. Freischaltung gilt **pro Spieler** (jedes Exemplar zeigt den Stand seines Lesers); alte Bitmasken werden übernommen.
4. Jede Mod bekommt ein eigenes, herstellbares Buch mit denselben Tabs-Mechanik; nicht beim Start im Inventar; mit
   FTB Quests als kostenlose Belohnung der ersten Quest.

## Tor-Rezepte (Vorschlag, einfachstes Rezept der Stufe)

| Tab | Tor (eines reicht) |
|---|---|
| Werkzeuge | Steinmeißel (erstes Werkzeug der Mod) |
| Verzauberungen | erstes Mod-Buch-Rezept bzw. Bücherregal-/Verzauberungstisch-Rezept |
| Bauen | Steinkiesel/Bruchstein-Rezepte der Mod, erste Baublock-Palette (z. B. Astralit-Ziegel) |
| Lager | Rucksack, Köcher oder Verstärktes Bündel |
| Maschinen | Verstärkter Trichter, Ofen oder Kolben |
| End | Enderit-Barren bzw. erstes End-Rezept (Astralit/Nihilit) |
| Pads | Druckplatten der Mod (Kupfer/Eisen …) oder eine Vanilla-Druckplatte |
| Gadgets | Kupferkern bzw. Messuhr, Erzdetektor, Attraktor |
| Besätze | Leuchtende/Strahlende Besatzvorlage |
| Admin | nur Operator (unverändert) |
| Vanilla-Regal | je Tab das typische Vanilla-Grundrezept (Holzspitzhacke, Fackel, Boot, Feuerzeug, Enderauge, Redstone-Fackel, Steinschwert, Weizen→Brot) |

Endgültige Liste im Code (`GuideBooks.gates(Book)`) mit Test „jedes Tor-Rezept existiert“.

## Entwurf

- **Prüfung**: beim Öffnen und bei jedem `ServerRecipeBook#add` (Mixin, ein Hook für alle Loader) prüft der Server die
  Tore aller Tabs; neu freie Tabs landen im Spielerstand (persistente Spielerdaten, Liste von Tab-Ids) und gehen per
  Payload an den Client. Kein Item-Verbrauch, kein Klick nötig; Seitenblätter-Klang beim ersten Freiwerden.
- **Gemeinsame Buch-Bibliothek** (L1): Bildschirm, Layout, Tabs und Freischaltung als Registry statt Enum
  (`BookDef{id, mod, tabs[TabDef{id, icon, gates, chapters}]}`), als kleines eigenes Jar (`simpleguides`), das jede Mod
  mitbringt, weil Module nicht von SimpleBuilding abhängen dürfen.
- **Bücher je Mod**: Inhalt aus `wiki/manual.json` der Mod (features → Kapitel), als Lang-Keys erzeugt; Rezept Buch +
  typisches Item der Mod; Tabs mit Toren wie oben.
- **FTB Quests**: erste Quest jeder Mod gibt das Buch als Belohnung.

## Schritte

1. SimpleBuilding: Tor-Liste, Spielerstand, Rezeptbuch-Hook, Tooltip „Schalte frei: …“, Migration der Bitmaske; Tests.
2. Registry-API, SimpleBuilding nutzt sie selbst; Tests unverändert grün.
3. Bibliothek auslagern, Module binden ein (Buch, Rezept, Inhalt aus manual.json).
4. FTB-Quests-Rewards; Wiki/Doku.

## Risiken

- Rezepte, die Vanilla nicht per Rezeptbuch freischaltet (Spezialrezepte) als Tor meiden.
- Bitmasken-Altbestand (Migration), Mehrspieler: Buch zeigt Stand des Lesers.

## Stand 2026-10-02 (Branch `claude-guides`)

**Schritt 1 erledigt.** Tor-Liste im Code (`GuideBooks.gates/hint`), Spielerstand als Entity-Tag
`simplebuilding.guide_tab.<ns>.<pfad>` (übersteht Tod/Neuanmeldung, ohne eigene Persistenz je Loader), Hook
`GuideRecipeUnlockMixin` auf `ServerPlayer#awardRecipes` (RETURN, nur bei neu hinzugefügten Rezepten; ein Mixin für
Fabric/NeoForge/Forge), Prüfung zusätzlich beim Betreten (`GuideBooks.onPlayerJoin` → `GuideUnlocks.onJoin`) und beim
Benutzen. Stand geht per `GuideStatePayload` (S2C, alle drei Loader) an den Client; der alte C2S-`GuideUnlockPayload`,
die Bestätigungsseite und der Item-Verbrauch sind entfernt. Gesperrter Reiter: Tooltip „Gesperrt. Schalte frei: %s
herstellen“, Klick tut nichts. Migration: `guide_chapters`-Maske des benutzten/getragenen Buchs → Spieler-Tags, Maske
wird entfernt; erledigte `guides/*`-Erfolge zählen ebenfalls als offen. Kapitel-Erfolge werden beim Öffnen weiter
vergeben (Quests lesen sie). Admin bleibt live an OP-Stufe 2 gebunden.

Abweichungen: (a) Spielerstand als Entity-Tag statt eigener Spielerdaten-Datei – vanilla, loaderneutral. (b)
Steinkiesel-Rezepte (`stone_pebble`, `cobblestone_from_stone_pebbles`) haben keinen Rezept-Erfolg, kommen also nie ins
Rezeptbuch und taugen nicht als Tor; Bauen nutzt Ziegelsteine/Steinziegel/Kupfer-Baustab. Falls Kiesel gewünscht:
Rezept-Erfolge für die handgeschriebenen 26.3-Rezepte ergänzen. (c) Besätze: Pulsierende Vorlage (Werkbank) zuerst,
dazu die Schmiede-Rezepte der Leuchtenden/Strahlenden Vorlage (`*_armor_upgrade_dummy`).

**Schritt 2 teilweise.** `GuideTabs` ist die Registry für Tabs und Freischaltung (`Tab{id, access ALWAYS|RECIPES|OPERATOR,
gates, hint, advancement}`); SimpleBuilding registriert seine Bücher darüber, Server-Stand und Payload arbeiten nur mit
Tab-Ids. Bildschirm und Inhalt (`GuideBookScreen`, `GuideContent`) hängen weiter am Enum `GuideBooks.Book` – das ist der
Teil, der mit Schritt 3 in die Bibliothek wandert.

**Schritt 4 teilweise.** FTB Quests: die erste SimpleBuilding-Quest (`stage_1.welcome`) gibt auf 26.3 das Handbuch
(`Q.reward`, `tools/quests/generate_quests.py`). Bestehende Installationen bekommen die geänderte Kapiteldatei nicht
(FtbQuestsDefaults überschreibt nie).

### TODO Schritt 3/4 (offen, zu groß für diesen Lauf)

1. Gemeinsame Bibliothek `simpleguides` als eigenes Gradle-Projekt mit MC-Abhängigkeit (nicht `framework`, das ist reines
   Java): `GuideTabs`, `GuideUnlocks`, `GuideStatePayload`, `GuideRecipeUnlockMixin` und ein von `Book`-Enum gelöster
   Bildschirm (`BookDef{id, mod, colour, tabs[TabDef{id, icon, gates, chapters}]}`) wandern dorthin; jede Mod bündelt
   sie (Jar-in-Jar bzw. Shadow mit Relocation je Mod, damit zwei Mods nicht kollidieren) – Entscheidung Relocation vs.
   gemeinsames Mod-Jar steht aus. Payload-/Mixin-Registrierung je Loader in die Bibliothek.
2. Je Modul (`modules/*`, Liste in `modules/modules.json`): Buch-Item (Fabric/NeoForge/Forge), Rezept „Buch + typisches
   Item der Mod“, Tabs mit Toren wie oben, Inhalt aus `modules/<mod>/wiki/manual.json` (features → Kapitel, als
   Lang-Keys EN/DE generiert), Cover-Textur. Nicht beim Start im Inventar.
3. FTB Quests: erste Quest jeder Mod gibt deren Buch (Modul-Questdaten analog `Q.reward`).
4. Tests: je Modul „jedes Tor-Rezept existiert“ (wie `GuideBookTests#everyGuideTabGateIsAnUnlockableRecipe`).
