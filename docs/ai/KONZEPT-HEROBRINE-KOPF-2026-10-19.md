# Konzept: Mobheads-Easter-Egg – Herobrine-Kopf durch das Herobrine-Ritual (2026-10-19)

Queue „Besitzer 19.10. abends – Nachtrag“, Zeile Mobheads-Easter-Egg (B, b-konz20). Nur Konzept, kein Code. Entscheidungen: §8 (Code `KONZ20 H…`).

## 1. Ist-Zustand (per grep erhoben)
- Modul **`modules/simplemobheads`** (Pflicht-Sub-Mod von SB): einzige Quelle `data/mobs.json`, Generator `tools/generate_heads.py`
  (Java-Tabelle, Blockstates, Item-Modelle `minecraft:head`, Loot, Tags, Lang EN/DE, Questionary-Geheimnisse, Money-Preise, versteckte
  Sammel-Advancements). Köpfe verweisen auf **Vanilla-Texturen zur Laufzeit** (keine Mojang-Pixel im Repo), `model.custom` für eigene Geometrie,
  `tex` darf einen eigenen Namensraum nennen. Fähigkeiten über `ability`-Schlüssel. `exclude` nennt `minecraft:player` (Vanilla-Kopf).
- Konzept `docs/ai/KONZEPT-SIMPLE-MOBHEADS-2026-10-13.md` (Besitzer 13.10. alles übernommen). Kein Ritual, kein Mob-loser Kopf, kein Easter Egg.
- Vorbilder für Bau-Muster: Vanilla `BlockPattern` (Wither, Eisengolem); Feuer-Auslöser wie das Netherportal (`BaseFireBlock#onPlace`).

## 2. Der Mythos (aus Wissen, keine Quelle zitiert)
Herobrine: Creepypasta ab 2010 (Forenbeitrag + Livestream-Gerücht um Notch’ „verstorbenen Bruder“), Steve-Aussehen mit **weißen, leuchtenden
Augen**; Zeichen: entfernte Fackeln, 2×2-Tunnel, Bäume ohne Blätter, Sandpyramiden im Meer, Gestalt im Nebel, die beim Näherkommen
verschwindet. Mojang „entfernte Herobrine“ als Running Gag in Patchnotes. Ab ca. 2012 zeigten viele YouTube-Videos „How to summon
Herobrine“ mit einem **Schrein**; die verbreitetste Bauweise (auch im bekannten Herobrine-Mod-Totem):
```
Ebene 3:            [Netherrack]  ← mit Feuerzeug anzünden
Ebene 2:  [RF] [Bemooster Bruchstein] [RF]     (RF = Redstone-Fackel an allen vier Seiten, auf dem Gold)
Ebene 1:  3×3 Goldblöcke
```
Varianten der Videos: ohne bemoosten Bruchstein (Netherrack direkt auf Gold), Schild „Herobrine“, Nacht als Bedingung. Kein Kanon.

## 3. Ablauf (Vorschlag)
1. Spieler baut den Schrein wie oben (Gold 3×3, Mitte bemooster Bruchstein, 4 Redstone-Fackeln auf den Kanten-Goldblöcken, Netherrack oben).
2. Netherrack wird **von einem Spieler** angezündet (Feuerzeug oder Feuerkugel; Auslöser `BaseFireBlock#onPlace` + Musterprüfung, Spieler =
   wer im selben Tick ein Feuerzeug/eine Feuerkugel an dieser Stelle benutzt hat).
3. Wirkung (alles kosmetisch, kein Blockschaden außer am Schrein):
   - Himmel: ein **Blitz nur als Effekt** (`setVisualOnly`, kein Feuer, kein Schaden, keine Mob-Umwandlung) neben dem Schrein, Donner.
   - 3 s Stille (Umgebungsmusik aus beim Auslöser), dann Höhlen-Klang; Netherrack-Feuer wird kurz zu **Seelenfeuer**.
   - Redstone-Fackeln erlöschen (werden zu nichts; der Mythos „Fackeln verschwinden“), bemooster Bruchstein wird zu Bruchstein
     (der „Verbrauch“), Gold bleibt.
   - Auf dem Schrein liegt der **Herobrine-Kopf** (Item-Entity, glitzert nicht).
4. Abklingzeit: je Schrein-Position 1 Ingame-Tag; je Spieler höchstens 1 Kopf je Ingame-Tag (Config). Nacht ist **keine** Pflicht, aber nachts
   kommt zusätzlich die Spuk-Phase (Variante B unten).

### 3.1 Umfang (Varianten)
| | Inhalt | Aufwand | Risiko |
|---|---|---|---|
| A · Nur Ritual + Kopf | §3 Schritte 1–4 | klein | keins |
| **B · A + kurze Spuk-Phase (Empfehlung)** | danach 5 min nur beim Auslöser (Client): in 40–60 Blöcken Entfernung kurz eine Gestalt mit weißen Augen (Partikel/Client-Modell, kein Mob), verschwindet beim Hinsehen bzw. < 24 Blöcke; ab und zu Schritte-Klang; leise Fackel-Zisch-Klänge. Keine Weltänderung | mittel | keins (rein Client, abschaltbar) |
| C · B + Herobrine-Mob | echter Mob (flieht, teleportiert, droppt Kopf) | groß (Modell, KI, Spawns) | Balance, Kollision mit Simple Mobs, Griefing-Erwartung |

## 4. Der Kopf (`simplemobheads:herobrine_head`)
- Eintrag in `mobs.json` mit `"from": "ritual"` (kein Mob-Drop, kein `charged_creeper`-Pool), Gruppe `secret`, Spieler-Kopf-Geometrie
  (8×8×8 + Hut-Schicht), Klang am Notenblock: leiser Höhlenklang (`ambient.cave`).
- **Textur – Varianten:**
  - **T1 Eigene Pixelkunst (Empfehlung):** eigenes 64×64-Skinblatt (nur Kopf-Bereich belegt) unter `tools/textures/herobrine_head_2026_10_19.py`,
    eigene Haar-/Bart-Form im Stil der Besitzer-Texturen (kein Steve-Recolor), erkennbar an **weißen Augen ohne Pupille**; zweite Schicht
    „Augen leuchten“ (emissiv wie Spinnenaugen). 16-fach-Vorschau alt/neu, Besitzer-Abnahme (Regel A8, Pixel-Art-Feedback).
  - T2 Laufzeit-Steve + eigene Augen-Schicht: verweist wie die anderen Köpfe auf `textures/entity/player/wide/steve.png`, nur die Augen sind eigene
    Pixel. Folgt Ressourcenpaketen, aber „Steve mit weißen Augen“ = Recolor-Nähe; Steve-Pfad kann sich je Version ändern.
  - Kein Spieler-Profil „Herobrine“ (würde einen echten Account-Skin von Mojang-Servern laden) – ausgeschlossen.
- **Fähigkeit (Kopf-Slot):** Vorschlag **nur kosmetisch**: Augen leuchten im Dunkeln, sonst keine Wirkung. Alternative: „Fackeln in
  8 Blöcken flackern“ (Client-Partikel). Keine Kampfwerte.
- Questionary-Geheimnis + verstecktes Advancement „Er wurde nie entfernt“ (EN „He was never removed“) wie die anderen Köpfe.
- Hinweise (Regel A6, zwei im Spiel): Questionary-Rätselzeile („Gold trägt Moos, Moos trägt Netherrack, vier rote Lichter wachen“) und ein
  seltenes Buch „Herobrine-Notizen“ in Minenschacht-Truhen (eigener Loot-Inject des Moduls, abschaltbar
  wie alle Mod-Pools); Wiki erst nach erstem Fund aufklappbar (Spoiler).

## 5. Zielmodul und Unabhängigkeit
- **`simplemobheads`** (Kopf + Ritual + Spuk-Client). Nur Vanilla-Blöcke im Muster, keine fremden Klassen. Ritual lebt im gemeinsamen
  `shared/java/com/simplemobheads/ritual/` (alle drei Loader, ein Mixin `BaseFireBlockMixin`).
- Config `simplemobheads-server.json`: `herobrineRitual` (an), `ritualCooldownTicks` (24000, 1200–168000), `hauntPhase` (an);
  Client-Config `hauntEffects` (an). Schalter senken nie Vanilla-Schutz.

## 6. Server-Sicherheit und Balancing
- Kosten: 9 Goldblöcke bleiben (Schrein wiederverwendbar), verbraucht werden 4 Redstone-Fackeln + Moos (→ Bruchstein) + Netherrack-Feuer;
  Abklingzeit verhindert Kopf-Farmen. Kopf ist rein kosmetisch → Money-Preis wie seltene Köpfe (Generator setzt ihn).
- Muster-Prüfung nur beim Feuer-Setzen an genau dieser Stelle (keine Tick-Suche), nur geladene Blöcke, nur Server-Thread.
- Blitz nur visuell; keine Weltänderung außer den 6 Schrein-Blöcken; kein Ritual in Spawn-Schutz/fremden Claims ohne Recht B (Fackeln und
  Netherrack setzen braucht B ohnehin; Feuer zünden über Claims-Regel).
- Spuk nur clientseitig für den Auslöser, kein Paket an andere, keine Entität auf dem Server.

## 7. Stufenplan
1. Kopf-Eintrag + Textur T1 (Vorschau für Besitzer) + Generator-Lauf.
2. Ritual: Muster, Auslöser, Wirkung, Abklingzeit, Config, Advancement, Questionary.
3. Spuk-Phase (B) clientseitig, Hinweis-Buch-Loot.
4. Wiki (Spoiler), Guide, Discover, Client-Abnahme.

## 8. Testplan
- GameTests (Fabric/NeoForge/Forge + standalone): richtiges Muster + Feuerzeug → genau 1 Kopf, Fackeln weg, Moos → Bruchstein, Gold bleibt;
  falsches Muster (eine Fackel fehlt, kein Moos, 2×2 Gold) → nichts; zweites Ritual innerhalb der Abklingzeit → kein Kopf; Feuer ohne Spieler
  (Blitz/Lava) → nichts; Blitz ist visuell (kein Feuer in 5 Blöcken, kein Schaden); Config aus → nichts.
- JUnit/Generator: `generate_heads.py --check`, Kopf hat keinen Mob-Drop-Pool, Textur ohne Vanilla-Pixel (Prüfskript wie andere eigene Texturen).
- Client-Abnahme: Kopf im Inventar/auf Kopf/als Block, Augen leuchten, Spuk-Gestalt.

## 9. Offene Besitzer-Fragen (Vorschlag fett)
- **H1 Schrein-Bauweise:** **Gold 3×3 + bemooster Bruchstein + 4 Redstone-Fackeln + Netherrack (Totem-Version)** / ohne Moos (Netherrack direkt auf Gold).
- **H2 Umfang:** A nur Ritual / **B Ritual + kurze Spuk-Phase (nur Client)** / C mit echtem Herobrine-Mob.
- **H3 Textur:** **T1 eigene Pixelkunst mit weißen Augen** / T2 Laufzeit-Steve + eigene Augen.
- **H4 Nacht nötig:** **nein (nachts nur zusätzlich Spuk)** / ja, nur nachts.
- **H5 Kopf-Fähigkeit:** **nur kosmetisch (Augen leuchten)** / kleine Fähigkeit (Fackeln flackern) / keine.
