# Wiki: Stand und offene Befunde

Stand 2026-09-02. Die sechs Punkte aus dem Feedback sind umgesetzt; diese Datei
hält fest, **was gebaut wurde** und **was beim Durchleuchten des Codes
aufgefallen ist**. Der Pflegeprozess steht in `wiki/README.md` – dort nachlesen,
bevor etwas geändert wird.

## Erledigt

| # | Punkt | Wie gelöst |
|---|---|---|
| 1+2 | Haltbarkeit und Co. fehlten, sollen zentral änderbar sein | Datagen-Provider `WikiDataProvider` schreibt die **tatsächlichen** Werte aus der Item-Registry nach `src/main/generated/wiki/items.json`; `generate.py` hängt sie an jedes Item, die App zeigt sie als Abschnitt „Eigenschaften". Eine Konstante ändern + `gradlew runDatagen` genügt. |
| 3 | 3D-Ansicht der Blöcke | Ober-, Seiten- und Vorderseite aus dem Blockmodell, isometrischer Würfel per CSS-Transform. 23 von 29 Blöcken; Trichter und Kolben behalten die flache Textur. |
| 4a | Englisch als Hauptsprache, Deutsch als Auswahl | `STR`-Wörterbuch (en/de) hinter `T()`, Dropdown oben rechts, `localStorage`, Umschalten ohne Neuladen. Auch Namensauflösung, Sortierung, Dezimaltrennzeichen und Mehrzahl hängen an der Sprache. |
| 4b | Prosa zweisprachig | `manual.json` trägt je Eintrag `en` und `de`. Die englische Fassung ist **keine Übersetzung**, sondern aus demselben Code geschrieben; `--check` fordert beide Sprachen. |
| 5 | „ohne Wirkung" an fast allen Verzauberungen | Die App wertete weiter `hasEffect` aus. Jetzt entscheidet `implementedIn` (`data`/`code`/`both`/`none`) mit eigenem Badge, eigener Zeile und erklärendem Kasten. Rot steht nur noch an `cover` und `bridge`. |
| 6 | Vanilla-Texturen fehlten | `generate.py` zieht die referenzierten Vanilla-Ids aus dem Client-Jar im Gradle-Cache nach `wiki/assets/textures/minecraft/` (in `.gitignore`). 96 von 97 Ids bekommen ein Bild. |

## Offene Befunde in der Mod

Nicht Teil des Wikis. Alles hier ist am Code oder am Bytecode nachgeprüft, wenn
nicht ausdrücklich anders vermerkt. Der Nutzer entscheidet, was davon ein Fehler
ist.

### Sichtbar im Spiel

1. **Zwei Items ohne Namen.** `enchanted_netherite_apple` und
   `enchanted_enderite_apple` haben in *keiner* Sprachdatei einen Eintrag,
   liegen aber über `ModLootTableModifications` in End-City-, Ancient-City-,
   Bastion- und Vault-Truhen und haben Item-Modelle. Wer einen findet, sieht
   `item.simplebuilding.enchanted_netherite_apple`.
2. **Tippfehler:** `en_us.json` nennt `netherite_apple` „**Neterite** Apple".
3. **Farbige Oktanten mit 65 536 Haltbarkeit.** `ModItems.java:748` rechnet
   `DURABILITY_NETHERITE * DURABILITY_OCTANT` = 512 × 128; der einfache Oktant
   hat 128. Faktor 512 – vermutlich war eine Summe oder nur eine der beiden
   Konstanten gemeint.
4. **Rotator und Erzdetektor lassen sich nicht am Zaubertisch verzaubern**, weil
   ihre Registrierung kein `enchantable(...)` setzt (`ModItems.java:345`, `:349`)
   – anders als jedes andere Werkzeug der Mod. Der Rotator steht trotzdem im Tag
   `DURABILITY_ENCHANTABLE`. Nur der Amboss mit Buch funktioniert.
5. **Befehlsausgaben sind fest deutsch.** `/simplebuilding config
   setTrimMultiplier` und `getTrimMultiplier` antworten mit hartcodierten
   deutschen Zeichenketten (`ModCommands.java:27,34`) – auffällig, seit Mod und
   Wiki englischsprachig sind.

### Tote oder widersprüchliche Stellen

6. **`cover` und `bridge`** sind registriert, erhältlich (bridge sogar im
   Wanderhändler-Pool) und ohne jede Wirkung. Einzige Ausnahme: die
   Item-Modell-Auswahl liest sie und zeigt eine eigene Textur.
7. **`speedMultiplier`** ist in allen drei Ofenklassen deklariert und wird nie
   gelesen; die Beschleunigung kommt aus `extraTicks`.
8. **Sechs Sprachschlüssel ohne Item:** `netherite_chest`, `reinforced_chest`,
   `nithilith_ore` (Tippfehler für `nihilith_ore`?), `polished_endstone` (vs.
   `polished_end_stone`), `purpur_lapis_checker`, `sledgehammer`.
9. **68 Schlüssel fehlen in `de_de.json`** gegenüber `en_us.json` (Enderit-Reihe,
   Konfigurationstexte). Kein Fehler – Minecraft fällt auf Englisch zurück –,
   aber eine Übersetzungslücke.
10. **Zwei Mixins für denselben Zweck:** `ItemEntityMixin.floatInVoid` arbeitet
    mit einer hartcodierten Enderit-Liste samt TODO, während `EnderiteItemMixin`
    dasselbe tagbasiert löst. Beide sind registriert.
11. **`TrimReferenceScreen.java:86`** und `SmithingScreenMixin.java:56` schlagen
    `simplebuilding:enderite_armor_trim_smithing_template` nach – dieses Item ist
    nirgends registriert.
12. **`TrimEffectUtil`** vergleicht Besatzmuster und -materialien mit
    `String.contains`. Ein Fremdmod-Material, dessen Pfad „iron" oder „gold"
    enthält, bekäme die Vanilla-Boni.

### Nur teilweise geprüft

13. **Luftsprung auf Forge:** `ForgeClientGameEvents.java:87` merkt sich
    `wasOnGround` allein aus `onGround()`, während der geteilte
    `DoubleJumpController` auch Klettern und Wasser mitzählt. Das ist ein
    Verhaltensunterschied zwischen den Loadern, kein Wiki-Fehler.
14. **`NetheriteHopperScreen.mouseClicked:104`** prüft
    `hoveredSlot.getContainerSlot() < 5` – ein Index innerhalb des Behälters.
    Ob das für die größere Netherit-Variante stimmt, konnte ich nicht sicher
    entscheiden; deshalb steht dazu nichts im Wiki.

## Zwei Baustellen im Wiki-Werkzeug

15. **Bündel-Kapazität fehlt im Datagen-Export.** Sie hängt an
    `getTierCapacityMultiplier(ItemStack)`, und im Datagen lässt sich ab MC 26.2
    kein `ItemStack` bauen – dessen Konstruktor liest Komponenten, die dort noch
    nicht gebunden sind. Sauber wäre eine verhaltensgleiche Umstellung auf eine
    item- statt stackbasierte Stufenabfrage in `ReinforcedBundleItem` und
    `QuiverItem` (vier Dateien, beide Bäume). Die Werte 96/192/288 stehen bis
    dahin im Fließtext.
16. **`implementedIn` erkennt „wirkt im Code" über eine Verbotsliste.**
    `generate.py:353` scannt nur `common/src/shared` und schließt sechs
    Dateinamen aus. Heute stimmt jedes Urteil – alle 19 Verzauberungen wurden
    einzeln gegen den Java-Baum geprüft. Aber eine neue Datei, die alle
    Verzauberungen in einer Schleife erwähnt, kippt schlagartig *alle* auf
    „code", auch `cover` und `bridge`; und eine Verzauberung, die nur in einem
    Loader-Modul implementiert wäre, gälte als nicht implementiert. Eine
    Erlaubnisliste plus Scan der Loader-Bäume wäre robuster.

## Arbeitsweise, die der Nutzer erwartet

- Jede Behauptung im Wiki muss im Code stehen. Nicht raten. Gegenprüfen.
- Keine Fehlalarme: bevor etwas als Mod-Fehler gemeldet wird, Testumgebung
  ausschließen (Mock-Spieler ist immer Kreativmodus; Fabric registriert
  Gametests nur über den Entrypoint).
- Bei langen Läufen nicht blind warten: Prozesse und Protokolle prüfen. Achtung
  auf Windows: `gradlew … | tail` verschluckt den Rückgabewert von Gradle – ohne
  `set -o pipefail` sieht ein fehlgeschlagener Build wie ein Erfolg aus.
- Merge direkt auf `master`, kein PR.
