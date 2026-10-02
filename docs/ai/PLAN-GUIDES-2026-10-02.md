# Plan P5/P6 – Guides je Mod, Freischaltung über Advancements (2026-10-02)

Besitzer: Backlog P5, P6, L1 in `docs/ai/BACKLOG-2026-10-01.md`.

## Ist-Zustand (26.3, `McVersion.MEGA_GUIDES`)

- Zwei Hub-Items (`guide_book`, `guide_book_vanilla_start`), Kapitel als Bitmaske `GUIDE_CHAPTERS` (20 Bit, alle belegt) am Stapel.
- Freischalten: Tab anklicken → `GuideUnlockPayload` → `GuideUnlocks.unlock` verbraucht ein Schlüssel-Item und vergibt
  danach `simplebuilding:guides/<topic>` (Kriterium `impossible`). Kein Weg von Advancement → Kapitel.
- Rezept: Buch + Schlüssel-Item des Hubs (Datagen). Kein Start-Geschenk auf 26.3. FTB-Quests: keine Rewards.
- `Book` ist ein geschlossenes Enum, Inhalte über Lang-Keys `book.simplebuilding.*`, Stile in `GuideContent.STYLES`.
  Module haben keine Bücher (nur `wiki/manual.json`).

## Ziel

1. Kapitel schalten sich frei, sobald der Spieler den Stand erreicht hat (Advancement), ohne Item-Verbrauch.
   Gesperrter Tab sagt, was zu tun ist („Erreiche: <Advancement-Titel>“).
2. Freischaltung gilt pro Spieler (nicht pro Stapel): jedes Exemplar zeigt den Stand seines Lesers.
3. Jede Mod bekommt ein eigenes, herstellbares Buch im SimpleBuilding-Layout; nicht beim Start im Inventar;
   mit FTB Quests als kostenlose Belohnung der ersten Quest.

## Entwurf

- **Gemeinsame Buch-Bibliothek** (L1): Bildschirm, Layout, Seitenumbruch, Lesezeichen, Kapitel-Freischaltung als
  Registry-API statt Enum: `GuideRegistry.register(BookDef{id, ownerMod, shelf, icon, langPrefix, chapters, unlock})`.
  Wohnt im SimpleBuilding-Code (`common/.../guide/api`), Module binden sie über den bestehenden `framework`-Weg
  an, wenn SimpleBuilding fehlt? → Entscheidung: Module ohne SimpleBuilding brauchen den Bildschirm selbst, also
  Bibliothek als eigenes kleines Mod-Jar (`simpleguides`), das jede Mod per jarJar/include mitbringt (wie `framework`).
- **Freischaltung**: `ChapterDef.unlock = Advancement-Id` (Vanilla- oder Mod-Advancement). Server prüft beim Öffnen
  und bei jedem `PlayerAdvancements#award` (Mixin, ein Hook für alle Loader) und schickt die freigeschalteten Kapitel
  per Payload; gespeichert als Spieler-Attachment/persistente Daten (Liste von Kapitel-Ids, keine Bitgrenze).
  Alte `GUIDE_CHAPTERS`-Bits werden beim ersten Öffnen in den Spielerstand übernommen (keine Rückschritte).
  Vorschlag Zuordnung: Werkzeuge = „Steinmeißel herstellen“-Advancement usw.; Admin bleibt Operator.
- **Bücher je Mod**: Simple QoL, Simple Fun, Simple Riding, Simple Tweaks, Simple Visuals, Simple Money,
  Simple Sounds, Simple Models, Simple Dimensions. Inhalt aus `wiki/manual.json` (features → Kapitel) automatisch
  als Lang-Keys erzeugt, damit Wiki und Buch nicht auseinanderlaufen; Rezept Buch + typisches Item der Mod.
- **FTB Quests**: Generator bekommt `rewards` (Item-Reward Guide) für die erste Quest jeder Mod; Spieler holt ihn
  kostenlos ab. Ohne FTB Quests: nur Rezept.

## Schritte

1. Spieler-Stand + Advancement-Hook + gesperrter-Tab-Text, SimpleBuilding-Bücher umgestellt; Tests.
2. Registry-API, SimpleBuilding nutzt sie selbst (Enum → Defs), Tests unverändert grün.
3. Bibliothek auslagern (`simpleguides`), Module binden ein; je Modul Buch + Rezept + Inhalt aus manual.json.
4. FTB-Quests-Rewards; Wiki/Doku.

## Risiken

- Bitmasken-Altbestand in Welten (Migration), Mehrspieler: Buch zeigt Stand des Lesers.
- Modul-Isolation (Module dürfen nicht von SimpleBuilding abhängen) → eigene Bibliothek nötig.
- Offene Frage an Besitzer: Advancement-Zuordnung je Kapitel vorschlagen und freigeben lassen.
