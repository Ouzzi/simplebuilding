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
