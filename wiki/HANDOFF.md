# Wiki: Stand und offene Befunde

Stand 2026-09-03. Das Wiki ist fertig, und die beim Durchleuchten des Codes
gefundenen Fehler sind behoben. Diese Datei hält fest, **was gebaut wurde**,
**was an der Mod korrigiert wurde** und **was bewusst offen bleibt**. Der
Pflegeprozess steht in `wiki/README.md` – dort nachlesen, bevor etwas geändert
wird.

## Das Wiki

| # | Punkt | Wie gelöst |
|---|---|---|
| 1+2 | Haltbarkeit und Co. fehlten, sollen zentral änderbar sein | Datagen-Provider `WikiDataProvider` schreibt die **tatsächlichen** Werte aus der Item-Registry nach `src/main/generated/wiki/items.json`; `generate.py` hängt sie an jedes Item, die App zeigt sie als Abschnitt „Eigenschaften". Eine Konstante ändern + `gradlew runDatagen` genügt. |
| 3 | 3D-Ansicht der Blöcke | Ober-, Seiten- und Vorderseite aus dem Blockmodell, isometrischer Würfel per CSS-Transform. 23 von 29 Blöcken; Trichter und Kolben behalten die flache Textur. |
| 4a | Englisch als Hauptsprache, Deutsch als Auswahl | `STR`-Wörterbuch (en/de) hinter `T()`, Dropdown oben rechts, `localStorage`, Umschalten ohne Neuladen. |
| 4b | Prosa zweisprachig | `manual.json` trägt je Eintrag `en` und `de`. Die englische Fassung ist **keine Übersetzung**, sondern aus demselben Code geschrieben; `--check` fordert beide Sprachen. |
| 5 | „ohne Wirkung" an fast allen Verzauberungen | `implementedIn` statt `hasEffect`, mit eigenem Badge und erklärendem Kasten. Rot steht nur noch an `cover` und `bridge`. |
| 6 | Vanilla-Texturen fehlten | Referenzierte Vanilla-Ids aus dem Client-Jar nach `wiki/assets/textures/minecraft/` (in `.gitignore`). 96 von 97 Ids. |

Dazu kam ein **Registry-Abgleich**: Der Provider exportiert auch die
registrierten Blöcke, und `generate.py` lässt weg, was nur einen Sprachschlüssel
hat, und meldet umgekehrt, was registriert ist, aber keinen Namen trägt. Damit
listet das Wiki nichts mehr, das es im Spiel nicht gibt.

## An der Mod behoben (2026-09-03)

Alles am Code oder Bytecode nachgeprüft, alle vier In-Game-Suiten danach grün
(Fabric 54/54 ×2, NeoForge 53/53 ×2).

**Im Spiel sichtbar**

1. **Zwei Items ohne Namen.** `enchanted_netherite_apple` und
   `enchanted_enderite_apple` standen in *keiner* Sprachdatei, liegen aber über
   `ModLootTableModifications` in End-City-, Ancient-City-, Bastion- und
   Vault-Truhen. Beide haben jetzt Namen in beiden Sprachen.
2. **Zwei Tippfehler:** „Neterite Apple" und „Neterite Carrot" → „Netherite …".
3. **Farbige Oktanten hatten 65 536 Haltbarkeit** (`DURABILITY_NETHERITE *
   DURABILITY_OCTANT`). Der Faktor war ein Tippfehler; sie haben jetzt 128 wie
   der einfache Oktant, aus dem sie hergestellt werden.
4. **Rotator und Erzdetektor** ließen sich am Zaubertisch nicht verzaubern, weil
   ihre Registrierung kein `enchantable(...)` setzte – anders als jedes andere
   Werkzeug, und obwohl der Rotator im Tag `DURABILITY_ENCHANTABLE` steht. Beide
   haben jetzt `ENCHANTABILITY_NETHERITE` (15), wie der Oktant.
5. **`cover` und `bridge` sind aus Loot und Handel raus.** Beide haben
   nachweislich keine Wirkung; `bridge` war in zwei Loot-Pools und im
   Wanderhändler-Buch, `cover` war nie erhältlich. Die Registrierung bleibt,
   damit alte Welten laden. Das Wanderhändler-Buch gibt jetzt nur noch Radius.
6. **Befehlsausgaben waren fest deutsch.** `/simplebuilding config
   setTrimMultiplier` und `getTrimMultiplier` nutzen jetzt Übersetzungsschlüssel
   – in allen fünf `ModCommands.java` (beide Linien, alle Loader).
7. **`de_de.json` war unvollständig:** 68 Schlüssel fehlten gegenüber `en_us.json`
   (die ganze Enderit-Reihe, die Endstein-Blöcke, zwei Konfigurationsoptionen,
   die Schmiedevorlagen-Texte). **Beide Minecraft-Linien** haben jetzt in beiden
   Sprachen dieselben 324 Schlüssel – die 1.21.11-Linie hat eigene Sprachdateien
   unter `mc1_21_11/fabric/src/main/resources/`, was beim ersten Anlauf leicht
   übersehen wird.
8. **Zwei Blöcke ohne Namen:** `enderite_block` und `netherite_piston_head`
   hatten keinen `block.*`-Schlüssel (Vanilla hat für den Kolbenkopf einen).

**Toter Code**

9. **`speedMultiplier`** war in allen vier Blockklassen deklariert und wurde nie
   gelesen – samt Konstruktorparameter entfernt. Verhalten unverändert: die
   Beschleunigung kommt aus `extraTicks` bzw. der Block-Entity.
10. **`durability(512)`** an der Erzdetektor-Registrierung war tot:
    `OreDetectorItem` erzwingt im Konstruktor `durability(1024)`. Die
    irreführende Zeile ist weg, der Wert war und ist 1024.
11. **Vier verwaiste Sprachschlüssel** ohne Registrierung entfernt:
    `nithilith_ore`, `polished_endstone`, `purpur_lapis_checker`, `sledgehammer`.
12. **Zwei Mixins für denselben Leere-Schutz.** `ItemEntityMixin.floatInVoid`
    arbeitete mit einer hartcodierten Vier-Item-Liste samt TODO und injizierte in
    dieselbe Methode wie das tagbasierte `EnderiteItemMixin`, das alle 24 Einträge
    des Tags `void_protected` abdeckt. Die ältere Fassung ist raus; die übrigen
    Injektionen von `ItemEntityMixin` (Explosionsschutz, Aufsammeln) bleiben.
13. **Toter Verweis entfernt.** `TrimReferenceScreen` und `SmithingScreenMixin`
    schlugen `simplebuilding:enderite_armor_trim_smithing_template` nach, das
    nirgends registriert ist. Die Aufrufe waren null-sicher, also folgenlos.
14. **Veraltete Kommentare** in `ModLootTableModifications`, die BRIDGE noch als
    Pool-Inhalt führten.

## Bewusst offen

- **`cover` und `bridge` bleiben registriert und wirkungslos.** Was sie tun
  sollen, ist eine Design-Entscheidung. Der Gametest
  `coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown` hält den Zustand fest.
- **`netherite_chest` und `reinforced_chest`** sind auskommentierte TODOs in
  `ModItems.java`. Ihre Sprachschlüssel bleiben stehen; der Generator lässt sie
  weg, statt Blöcke zu zeigen, die es nicht gibt.
- **Die sechs Spachteln bleiben ohne Namen** – Altlasten für
  `LegacySpatulaMigration`, nie im Spielerbesitz. Sie stehen als Ausnahme in
  `generate.py`, damit die Warnung nicht zu Rauschen wird.
- **`TrimEffectUtil`** vergleicht Besatzmuster und -materialien mit
  `String.contains`. **Bewusst nicht geändert:** `assetId().getPath()` liefert ein
  Format, das sich ohne Laufzeitprobe nicht sicher bestimmen lässt; ein falsches
  `equals` würde die Besatz-Boni still abschalten – ein schlechterer Zustand als
  jetzt. Betrifft ohnehin nur Fremdmods, deren Pfad „iron" oder „gold" enthält.

## Nur teilweise geprüft – nicht angefasst

- **Luftsprung auf Forge:** `ForgeClientGameEvents.java:87` merkt sich
  `wasOnGround` allein aus `onGround()`, der geteilte `DoubleJumpController`
  zählt auch Klettern und Wasser mit. Verhaltensunterschied zwischen den Loadern;
  Forge ist ohnehin zurückgestellt.
- **`NetheriteHopperScreen.mouseClicked:104`** prüft
  `hoveredSlot.getContainerSlot() < 5`. Ob das für die größere Netherit-Variante
  stimmt, konnte ich nicht entscheiden – deshalb steht dazu nichts im Wiki.

## Arbeitsweise, die der Nutzer erwartet

- Jede Behauptung im Wiki muss im Code stehen. Nicht raten. Gegenprüfen.
- Keine Fehlalarme: bevor etwas als Mod-Fehler gemeldet wird, Testumgebung
  ausschließen (Mock-Spieler ist immer Kreativmodus; Fabric registriert
  Gametests nur über den Entrypoint).
- Bei langen Läufen nicht blind warten: Prozesse und Protokolle prüfen. Achtung
  auf Windows: `gradlew … | tail` verschluckt den Rückgabewert von Gradle – ohne
  `set -o pipefail` sieht ein fehlgeschlagener Build wie ein Erfolg aus. Und
  „BUILD SUCCESSFUL" ist kein Testnachweis: immer die JUnit-Berichte lesen.
- Merge direkt auf `master`, kein PR.
