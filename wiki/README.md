# SimpleBuilding Wiki

Eine leichtgewichtige, offline lauffähige Dokumentation der ganzen Mod: jedes Item, jeder
Block, jedes Rezept, jede Loot-Tabelle, jeder Handel, jede Verzauberung, jeder Tag und
jede Konfigurationsoption – dazu Fließtext, der erklärt, was die Dinge tun.

## Öffnen

Doppelklick auf `wiki/index.html`. Kein Server, kein Build, kein Internet nötig.

Wer lieber über einen Server geht (z. B. für Hash-Links in anderen Browsern):

```bash
cd wiki && python -m http.server 8080
```

und dann `http://localhost:8080/` öffnen.

## Wie die Doku aktuell bleibt

Das ist der Kern des Aufbaus, deshalb ausführlich.

**Nichts in `wiki/data/` ist von Hand geschrieben.** `generate.py` liest die Mod selbst:

| Bereich | Quelle im Repo |
|---|---|
| Items, Blöcke, Namen | `src/main/resources/assets/simplebuilding/lang/*.json` |
| Texturen | `src/main/generated/assets/simplebuilding/models/**` → `textures/**` |
| Rezepte | `src/main/generated/data/simplebuilding/recipe/**` |
| Loot-Tabellen | `src/main/generated/data/simplebuilding/loot_table/**` |
| Handel | `src/main/resources/data/simplebuilding/villager_trade/**` |
| Verzauberungen | `src/main/generated/data/simplebuilding/enchantment/*.json` |
| Tags | `.../tags/**` (generiert und Ressourcen) |
| Konfiguration | `common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java` |
| Welche Items eigenes Verhalten haben | Registrierungen in `ModItems.java` / `ModBlocks.java` gegen die Klassen in `items/custom/` und `blocks/custom/` |

Ändert sich die Mod, ändert sich beim nächsten Lauf die Doku – automatisch.

**Das Einzige, was von Hand gepflegt wird, ist `wiki/manual.json`:** Fließtext, den keine
Datei der Mod enthält – was ein Werkzeug *tut*, wie man es bedient, was sich je Stufe ändert.

### Der Zwang, nicht die Bitte

Ein Wiki, das „bitte aktuell halten" sagt, veraltet. Deshalb ist die Pflicht in den Build
eingebaut:

```bash
gradlew check          # enthält checkWiki
python wiki/generate.py --check
```

`checkWiki` schlägt fehl, wenn

1. die committete `wiki/data/simplebuilding.json` nicht mehr dem entspricht, was die Mod jetzt
   erzeugen würde (also: Mod geändert, Wiki nicht neu erzeugt), **oder**
2. etwas im Spiel existiert, das in `manual.json` keinen Fließtext hat.

Punkt 2 gilt gezielt nur für Dinge mit eigenem Verhalten – Werkzeuge, Maschinen, Verzauberungen.
Ein reiner Baublock ist durch Rezept und Drop ausreichend beschrieben und braucht keine Prosa.
Die Menge ergibt sich aus dem Code (welche Registrierung eine Klasse aus `items/custom/` oder
`blocks/custom/` benutzt), nicht aus einer Liste hier. Ein neues Werkzeug fordert seinen
Fließtext also von selbst ein.

### Ablauf bei einer Änderung an der Mod

```bash
# 1. Mod ändern, Datagen laufen lassen wie gewohnt
gradlew runDatagen

# 2. Wiki neu erzeugen
python wiki/generate.py

# 3. Falls etwas Neues Fließtext braucht, sagt es dir der Lauf:
#      "N entries have no prose in wiki/manual.json"
#    -> Eintrag in manual.json ergänzen, Schritt 2 wiederholen

# 4. wiki/ mit committen
```

Stufenfamilien bekommen **einen** Eintrag mit Glob-Schlüssel, etwa `"*_sledgehammer"` – der
gilt für alle Stufen, und eine neue Stufe (etwa ein Enderit-Hammer) erbt ihn. Eine einzelne
Stufe kann mit ihrem exakten Schlüssel überschrieben werden.

### Aufbau von `manual.json`

```json
{
  "features": [
    {
      "id": "sledgehammer",
      "title": "Vorschlaghammer",
      "summary": "Zwei bis drei Sätze.",
      "details": ["ein Stichpunkt, eine Behauptung"],
      "related": ["simplebuilding:diamond_sledgehammer"],
      "sources": ["common/src/shared/java/com/simplebuilding/items/custom/SledgehammerItem.java"]
    }
  ],
  "notes": {
    "*_sledgehammer": {
      "summary": "Ein Satz.",
      "details": ["..."],
      "controls": ["Rechtsklick: ..."],
      "tiers": ["Kupfer: ...", "Eisen: ..."],
      "caveats": ["..."],
      "sources": ["..."]
    },
    "magnet": { "summary": "...", "details": [], "sources": [] }
  }
}
```

Jede Behauptung in `details`, `controls`, `tiers` und `caveats` soll sich im Code belegen
lassen; `sources` nennt die Dateien. Das ist keine Formalie – die Erstfassung wurde
Behauptung für Behauptung gegen den Code geprüft, und so soll es bleiben.

### Beide Minecraft-Linien

```bash
python wiki/generate.py                 # 26.2 (Standard)
python wiki/generate.py --line 1.21.11  # 1.21.11
```

Die Datengrundlage beider Linien stammt aus denselben Providern; das Wiki zeigt die gewählte
Linie im Kopf an.

## Dateien

| Datei | Zweck | von Hand? |
|---|---|---|
| `generate.py` | erzeugt alles aus der Mod | ja, aber selten |
| `manual.json` | Fließtext | **ja – die einzige Pflegedatei** |
| `index.html` | die App, eine Datei, Vanilla-JS | ja, selten |
| `data/simplebuilding.json` | die Doku als JSON | nein, generiert |
| `data/simplebuilding.js` | dasselbe als `window.WIKI_DATA`, damit `file://` funktioniert | nein, generiert |
