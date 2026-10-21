# Konzept: Easter-Egg-Köpfe – Technoblade, Killer-Kaninchen, Riesen-Zombie, Riesen-Kuh (2026-10-21)

Queue „Besitzer 21.10., Simple Mobheads, nur Konzept“ (B, b-easterheads). Kein Code. Vorbild ist der Herobrine-Kopf
(`docs/ai/KONZEPT-HEROBRINE-KOPF-2026-10-19.md`, umgesetzt b-herobrine20). Entscheidungscodes stehen am Ende (§7, `EH21 …`).

## 1. Ist-Zustand (per grep erhoben)
- `modules/simplemobheads/data/mobs.json` ist die einzige Quelle. Daraus erzeugt `tools/generate_heads.py` Blöcke, Modelle, Loot, Lang,
  Questionary-Geheimnisse, Money-Preise und versteckte Advancements. Köpfe verweisen auf Vanilla-Texturen zur Laufzeit (keine Mojang-Pixel
  im Repo). Eigene Geometrie über `model.custom`, Fähigkeiten über `ability`.
- Drop-Regel aller Köpfe: nur geladener Creeper (Besitzer 29.09.). Easter-Egg-Köpfe dürfen davon abweichen (Herobrine: Ritual, `"from": "ritual"`).
- **Killer-Kaninchen gibt es schon als Variante:** Der Kaninchenkopf hat die Variante `evil` (Vanilla-Textur `rabbit/caerbannog`). Das Killer-Kaninchen
  spawnt in Java aber nie natürlich (nur per Befehl), daher ist der Kopf im Überleben heute nicht erreichbar.
- **Riese:** `minecraft:giant` steht in `exclude` („unused in survival (no spawn)“). Vanilla-Riese = Zombie-Modell in 6-facher Größe, ohne KI,
  ohne natürlichen Spawn.
- **Riesen-Kuh:** kein Vanilla-Mob. Machbar über das Vanilla-Attribut `minecraft:scale` (seit 1.20.5, auf 26.3 vorhanden), ohne neuen Mob.
- Gemeinsame Bausteine für alle vier: Gruppe `secret`, verstecktes Advancement, Questionary-Rätselzeile, Wiki-Spoiler (wie Herobrine),
  Abklingzeit/Config im Modul, Rest-Regeln aus PRINZIPIEN (allein spielbar, nur Vanilla-Blöcke in Mustern, nichts Ausnutzbares).

## 2. Technoblade (Tribut)
**Hintergrund und Rücksicht.** Technoblade war ein realer, 2022 verstorbener Minecraft-YouTuber. Sein Erkennungszeichen: ein Schwein mit
goldener Krone; sein Leitsatz „Technoblade never dies“. Mojang hat ihn danach mit einem Startbildschirm-Spruch geehrt (vor Umsetzung im
Spiel nachprüfen, welche Fassung). Weil es um eine reale Person geht, gilt:
- **kein** Gesicht/Skin der Person, **kein** Laden eines Spieler-Profils (würde den echten Account-Skin von Mojang-Servern holen);
- Tribut über sein Symbol: **Schweinekopf mit goldener Krone** (Schwein-Textur zur Laufzeit + eigene Kronen-Ebene, Ebenen-Quelle `.layers.json`);
- respektvoller Ton, keine Witze über den Tod, kein Töten „von ihm“.

**Name (Wahl):**
- **N-A (Empfehlung):** „Gekrönter Schweinekopf“ / „Crowned Pig Head“, Lore-Zeile (grau, kursiv) „Technoblade never dies“. Der Name der Person
  steht nur in der Lore.
- N-B: „Technoblades Krone“ / „Technoblade's Crown“ direkt als Itemname.

**Erhalt (Wahl):**
- **TO-1 „Never dies“ (Empfehlung):** Ein Schwein, das per Namensschild „Technoblade“ heißt, trägt sichtbar die Krone (nur Client-Render).
  Würde es sterben, überlebt es einmal mit Totem-Animation und Halbherz und lässt dabei den gekrönten Kopf fallen. Danach ist die Krone weg
  (Name bleibt), Abklingzeit je Schwein 1 Ingame-Tag; je Spieler höchstens 1 Kopf je Ingame-Tag. Kein Töten nötig – passt zum Leitsatz.
- TO-2 „Kartoffelbauer“: Verstecktes Advancement nach 1000 geernteten Kartoffeln (Vanilla-Statistik `mined:potatoes`), Belohnung der Kopf.
- TO-3 Seltener Handel beim fahrenden Händler (1 % je Besuch, Smaragde + Goldblock).
- TO-4 Amboss: Schweinekopf + Goldhelm im Amboss, Name „Technoblade“ → gekrönter Kopf (einfach, wenig Geheimnis).

**Fähigkeit (Kopf-Slot), Wahl:**
- **F-A (Empfehlung):** rein kosmetisch – Krone glänzt, Schweine in 8 Blöcken drehen den Kopf zum Träger.
- F-B: zusätzlich einmal je Ingame-Tag Totem-Effekt light (bei tödlichem Schaden 1 Herz, kein Absorption/Regeneration). Klein, aber ein Kampfvorteil.

## 3. Killer-Kaninchen
**Kopf:** vorhandene Variante `rabbit` / `evil` (Vanilla-Textur, rote Augen) – kein neuer Block nötig, nur ein Weg, sie im Überleben zu bekommen.
Optional eigener Name „Killer-Kaninchen-Kopf“ statt „Kaninchenkopf (Böse)“.

**Erhalt (Wahl):**
- **KO-1 „Caerbannog“ (Empfehlung):** Ein Kaninchen per Namensschild „Caerbannog“ (oder „The Killer Bunny“) nennen → es wird nachts zum
  Killer-Kaninchen (Vanilla-Variante `evil`, feindlich, Vanilla-Werte). Getötet durch eine **Explosion** (TNT, Creeper, Feuerwerk) lässt es den
  Kopf fallen – Anspielung auf die „Heilige Handgranate“ aus dem Film, ohne fremde Inhalte zu übernehmen. Normale Tötung: kein Kopf.
- KO-2 Seltener natürlicher Spawn: 1 von 1000 Kaninchen in Blumenwald/Wiese wird als Killer-Kaninchen geboren; Kopf dann normal per
  geladenem Creeper.
- KO-3 Nur geladener Creeper wie alle Köpfe, Killer-Kaninchen aber per KO-1 erzeugbar (Kombination: Name macht es, Creeper holt den Kopf).

**Fähigkeit:** **kosmetisch (Empfehlung)** – Augen leuchten rot im Dunkeln; Kaninchen in 8 Blöcken fliehen vor dem Träger. Alternative: kleiner
Sprung-Bonus (Sprungkraft I nur beim Sprinten); der Kaninchenkopf hat heute keine Fähigkeit, es gäbe also keine Doppelung.

## 4. Riesen-Zombie
**Kopf-Größe (Wahl, gilt auch für die Riesen-Kuh):**
- **G-A (Empfehlung):** „Riesenkopf“ als **ganzer Block** (16×16×16 statt 8×8×8), Zombie-Textur zur Laufzeit hochskaliert (Vanilla-Riese nutzt
  dieselbe Textur). Platzierbar als Deko; im Kopf-Slot getragen wird er in normaler Kopfgröße gezeichnet (sonst verdeckt er die Sicht).
- G-B: normale Kopfgröße mit eigenem Namen; sieht aus wie der Zombiekopf (kaum Mehrwert).

**Erhalt (Wahl):**
- **ZO-1 Blitz bei Vollmond (Empfehlung):** Schlägt bei Vollmond ein natürlicher Blitz in einen Zombie ein (Vanilla: Gewitter), wird daraus ein
  Riese (Vanilla-`minecraft:giant`). Er bekommt eine einfache KI (langsam laufen, Spieler angreifen, Türen nicht) und 100 Herzen wie Vanilla.
  Tod → Riesenkopf (immer), Zombie-Loot ×3. Selten genug durch Vollmond + Gewitter + Treffer. Config `giantFromLightning` (an).
- ZO-2 Ritual wie Herobrine: Eisenblock-Säule 2 hoch, oben Zombiekopf, 4 Zombieköpfe ringsum, bei Vollmond mit Feuerzeug zünden → Riese spawnt.
- ZO-3 Bauen statt Kampf: 8 Zombieköpfe + 1 Netherstern im Kreuz → Riesenkopf (Werkbank). Einfach, kein Geheimnis.
- Abhängigkeit: KI-Riese gehört inhaltlich zu Simple Mobs → nur mit Simple Mobs; ohne Simple Mobs gilt ZO-3 als Rückfall (Modul allein spielbar).

**Fähigkeit (Empfehlung):** Tarnung wie der Zombiekopf (Vanilla: verringerte Erkennungsweite), gleicher Radius, keine Verstärkung.

## 5. Riesen-Kuh
**Wie entsteht sie:** kein neuer Mob, eine normale Kuh mit Attribut `minecraft:scale` 3,0 (Vanilla-Mechanik), Name „Riesen-Kuh“, gleiche
Drops wie eine Kuh (kein Leder-/Fleisch-Exploit), Milch normal.

**Erhalt (Wahl):**
- **CO-1 Mästen (Empfehlung):** Eine Kuh frisst 64 Weizen (je Fütterung +1, sichtbar über Herz-Partikel; ab 16/32/48 wächst sie schrittweise
  auf 1,5/2/2,5, bei 64 auf 3). Getötet durch geladenen Creeper → Riesen-Kuhkopf (Blockgröße wie G-A). Je Kuh nur einmal, Zähler in den
  Entity-Daten.
- CO-2 Seltener Spawn: 1 von 500 Kühen in Wiesen-Biomen spawnt als Riesen-Kuh; Kopf per geladenem Creeper.
- CO-3 Mondkuh-Spaß: bei Vollmond auf einem Berg (y > 200) gefüttert → Riesen-Kuh. Witzig, aber zufällig.

**Fähigkeit:** kosmetisch; getragen: Kühe folgen dem Träger wie bei Weizen in der Hand (es gibt schon eine `cow`-Fähigkeit – dann nur Optik).

## 6. Gemeinsame Umsetzung (Stufenplan, nach Wahl)
1. `mobs.json`: vier Einträge (`technoblade_crown`, Variante `evil` freischalten, `giant_head`, `giant_cow_head`), Gruppe `secret`, Lang EN/DE,
   Advancements („Never dies“, „Run away!“, „Big trouble“, „Moo-ve over“), Questionary-Rätsel, Money-Preise wie seltene Köpfe.
2. Geometrie Riesenkopf (G-A) im Generator (`model.custom: "giant"`), Kronen-Ebene (Technoblade), Ebenen-Quellen, Vorschau-Seite für den Besitzer.
3. Erhalt-Logik je Kopf (gewählte Varianten), Config-Schalter je Easter Egg (Standard an), Abklingzeiten persistent (wie b-shopsherob21 bei Herobrine).
4. Tests: Drop-Wege, Abklingzeit, Config aus → kein Kopf, Modul allein spielbar (ZO-3-Rückfall), data_integrity; Wiki-Spoiler, Guide, Discover.

## 7. Entscheidungen (Code `EH21`)
| Kopf | Optionen | Empfehlung |
|---|---|---|
| Technoblade Name | N-A / N-B | N-A |
| Technoblade Erhalt | TO-1 / TO-2 / TO-3 / TO-4 | TO-1 |
| Technoblade Fähigkeit | F-A / F-B | F-A |
| Killer-Kaninchen Erhalt | KO-1 / KO-2 / KO-3 | KO-1 |
| Riesenkopf-Größe | G-A / G-B | G-A |
| Riesen-Zombie Erhalt | ZO-1 / ZO-2 / ZO-3 | ZO-1 (+ZO-3 als Rückfall ohne Simple Mobs) |
| Riesen-Kuh Erhalt | CO-1 / CO-2 / CO-3 | CO-1 |

Antwortbeispiel: `EH21 N-A TO-1 F-A KO-1 G-A ZO-1 CO-1`.
