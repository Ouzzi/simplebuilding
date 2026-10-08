# Plan simplecontainers + einheitlicher UI-Stil (2026-10-08)

Besitzer: Roadmap-P3 „UI-Konzepte aller UIs“ findet in **simplecontainers** statt; UI-Elemente parallel per Claude.
Referenzen: `/root/previews/refs-n12/bild3-container-stil.webp` (farbiger Block-Kasten je Container + heller
Inventar-Kasten, eingelassene gerundete Slots, Symbole statt Text) und `bild4-ofen-stil.png` (Ofen: Brennstoff-Slot
zeigt die Flammen in sich, großer Ergebnis-Slot, Pfeil, dezente Motive im Kasten). Maße/Farben: PLAN-CRUCIBLE-N12B
(FRAME 5/7, Ecken-Treppe, Slot 16+2, Palette fill/light/shade/slot/slotTop/label). Inventar: INVENTAR-UIS-2026-10-08.md.

## Entscheidungen (vom Besitzer offen gelassen, von Claude entschieden – Rückmeldung jederzeit möglich)
1. **Umfang:** alle Vanilla-Container-Screens mit Slots (Truhe/Doppeltruhe/Fass/Shulker/Endertruhe, Trichter,
   Spender/Werfer, Crafter, Werkbank, Ofen/Schmelzofen/Räucherofen, Braustand, Amboss, Schleifstein, Steinsäge,
   Webstuhl, Kartografietisch, Schmiedetisch, Verzauberungstisch, Leuchtfeuer, Handel, Reittier-Inventar,
   Spieler-Inventar). **Nicht:** Kreativinventar (eigene Tab-Logik, später), Bücher, Config-Screens (Cloth Config),
   HUD (bleibt Vanilla-Tooltip-Stil).
2. **Technik:** im Code gezeichnet (wie CrucibleScreen), keine PNG-Hintergründe; skaliert, färbt je Block, ohne
   Ressourcenpaket-Konflikte. Je Screen abschaltbar (Config), Hauptschalter.
3. **Farbe je Block:** ja, wie Bild 3 (Holzart der Truhe/Werkbank, Fass braun, Shulker in Shulkerfarbe, Endertruhe
   petrol, Ofen stein-grau, Schmiedetisch dunkel …); Inventar-Kasten immer hell (INVENTORY-Palette).
4. **Mod-UIs:** Stil-Bausteine liegen in **simplelib** (`com.simplelib.client.ui`), damit Mod-UIs (Tiegel, Mod-Truhen,
   Rucksack, Auto-Schmied, Befiederungstisch, Mod-Trichter, Hufeisen-Panel, LinkedPanel) auch **ohne**
   simplecontainers im Stil erscheinen (Modul-Unabhängigkeit). simplecontainers selbst stylt nur Vanilla-Screens
   und ist rein clientseitig (Server braucht es nicht).
5. **Guide-Buch** behält Buch-Optik; Blaupause/Oktant/Bauzauberstab/Trim-Referenz/Modell-Browser bekommen den
   Kasten-Stil in einer späteren Welle (nach Abnahme der Container).
6. **Loader:** Fabric 26.3 zuerst, NeoForge/Forge 26.3 im selben Zug (Client-Mixins sind geteilt), 26.2/1.21.11 erst
   im Port-Run.

## Architektur
- `modules/simplelib/shared/java/com/simplelib/client/ui/`: `UiPalette` (record, aus CrucibleScreen.Palette),
  `UiBoxes` (box(), slot(), bigSlot(), arrow(), progress, Symbole), `UiFlames` (= CrucibleFlames verschoben/geteilt).
  CrucibleScreen nutzt sie (Verhalten/Optik unverändert → Regression = Vorschau-Vergleich).
- `modules/simplecontainers/` (neues Modul wie simplesounds, client-only, `requires` cloth_config +
  `requires` simplelib für die Stil-Bausteine).
  - `ContainerStyles`: Zuordnung Screen-Klasse/MenuType → Stil (Palette, Kasten-Aufteilung, Sonder-Elemente).
  - Mixin auf `AbstractContainerScreen#renderBg`/extractBackground (26.3-Name prüfen): ist ein Stil registriert und
    aktiv → eigener Hintergrund: Kästen aus den Slot-Grenzen (Container-Slots vs. Spieler-Slots), Slots an jeder
    Slot-Position, Sonder-Elemente (Pfeil, Flammen, Blasen, Fortschritt) aus dem Menü-Zustand; sonst Vanilla.
  - Labels: Titel in label-Farbe der Palette; „Inventar“-Label bleibt.
  - Config: Hauptschalter + je Screen-Art an/aus (EN/DE, Tooltips mit Default).
- Vorschau-Werkzeug `tools/ui/simplecontainers_preview.py`: rendert jede UI aus denselben Zahlen (Python-Port der
  Bausteine) als PNG + Kontaktbogen → `/root/previews/simplecontainers/` (Branch refs-assets, nie master).

## Wellen
- **W0 (parallel):** A) Java-Grundlage (simplelib ui + Modul-Gerüst + generischer Renderer für Truhen-artige Screens)
  – Branch `claude-sc-base`. B) Vorschau-Werkzeug + Vorschauen aller Vanilla- und Mod-Container – Branch
  `claude-sc-preview` (+ PNGs in refs-assets).
- **W1 (parallel nach W0, je Agent ein Branch auf claude-sc-base):** G1 Lager (Truhen, Fass, Shulker, Endertruhe,
  Trichter, Spender/Werfer, Crafter, Reittier), G2 Arbeit I (Werkbank, Öfen ×3, Braustand, Leuchtfeuer,
  Verzauberungstisch), G3 Arbeit II (Amboss, Schleifstein, Steinsäge, Webstuhl, Kartografie, Schmiedetisch,
  Handel, Spieler-Inventar), G4 Mod-UIs (TieredChest, Rucksack, Auto-Schmied, Befiederung, Mod-Trichter,
  Hufeisen-Panel, LinkedPanel) über simplelib-Bausteine.
- **W2:** Nicht-Container-Screens (Blaupause, Oktant, Bauzauberstab, Trim-Referenz, Modell-Browser), Kreativinventar.

## Verifikation
- Kompilieren/Tests je Branch auf sb-test: `/root/sbt <branch> "./gradlew … -q"` bzw. run.py (Job-Sperre).
- GameTests (server-seitig prüfbar): Stil-Registry deckt jede Vanilla-MenuType mit Slots ab; Kasten-Berechnung aus
  Slot-Grenzen (reine Funktionen); Config-Schalter; Modul-Check `tools/check_data.py`; Standalone-Target.
- Client-Sicht: Client-GameTest-Screenshots (xvfb, client-fabric-263) je Screen + Vorschau-Vergleich; Abnahme
  durch Besitzer anhand `/root/previews/simplecontainers/`.
- Gate über `/root/sb-gate.sh`.

## Risiken
- 26.3-Rendering-API (GuiGraphicsExtractor statt GuiGraphics) – Muster aus CrucibleScreen übernehmen.
- Fremde Mods mit eigenen Screens auf Vanilla-Menüs: nur exakte Vanilla-Screen-Klassen stylen.
- Ressourcenpakete: Hauptschalter aus = reines Vanilla.

## W0-A Umsetzung (Branch `claude-sc-base`)
Ist-Zustand (26.3-Quellen, NeoForge-patched 26.3.0.16-beta): `AbstractContainerScreen` **überschreibt
`extractBackground` nicht**; jede Unterklasse (ContainerScreen, ShulkerBoxScreen, HopperScreen, DispenserScreen …)
ruft `super.extractBackground` (= `Screen`: Abdunkeln/Blur) und blittet danach ihr PNG. Labels zeichnet
`AbstractContainerScreen#extractLabels` (Farbe 0xFF404040). Slot-Lücke zwischen Container- und Spieler-Slots ist in
Vanilla nur 13–14 px (inkl. heller Slot-Kante): Truhe 9×N/Trichter/Spender 14, Shulker 13.

Entscheidungen (Claude):
1. **Paket `com.simplelib.api.client.ui`** statt `com.simplelib.client.ui`: Prinzip 6a erlaubt Modulen nur Importe aus
   `com.simplelib.api`. Inhalt: `UiPalette` (record + INVENTORY/BARREL + `derived`), `UiBoxes` (box, slot, bigSlot,
   arrow, progressFill, progressBar, veil, rounded, scale, FRAME 5/7, RIM), `UiFlames` (Flammenstreifen nach Zielhöhe
   und Ruhe, Kern aus CrucibleFlames). CrucibleScreen/CrucibleFlames delegieren – gleicher Code, gleiche Pixel.
2. **Mixin je Screen-Klasse statt auf AbstractContainerScreen** (dort gibt es die Methode nicht): HEAD-Inject in
   `extractBackground` der Ziel-Klassen, bei aktivem Stil `super.extractBackground` (nur Screen-Hintergrund) + eigener
   Hintergrund + cancel. Labels: HEAD-Inject in `AbstractContainerScreen#extractLabels`. Weitere Gruppen legen eine
   eigene Mixin-Klasse für ihre Screen-Klassen an (eine Zeile in `simplecontainers.mixins.json`).
3. **Kasten-Grenzen aus Slots** (`BoxLayout`, rein, serverseitig testbar): waagerecht über alle Slots ±8 (Truhe: 0..176),
   Container-Kasten oben bis Bildrand (Titel liegt im Kasten), Rahmen 5/7. Fuge nach der W0-B-Regel („Kasten-Fuge“):
   freie Zeilen ≥ 12 → zwei Kästen, Fuge min(2, frei−12), Rest polstert den Container-Kasten; < 12 → vorerst Vanilla
   (schattenloser Kasten 10–11 und Ein-Kasten-Variante < 10 kommen mit den Screens, die sie brauchen, W1). Das
   „Inventar“-Label entfällt im Stil (kein Platz, Bild 3 zeigt auch keins); der Titel steht in der Label-Farbe.
4. **Registry** (`ContainerStyles`, serverseitig ladbar): Schlüssel = `MenuType` + exakter Vanilla-Screen-Klassenname
   (fremde Unterklassen bleiben Vanilla). Je Gruppe eine Klasse (`StorageStyles` = W0), zentrale Liste
   `ContainerStyles.GROUPS`. Palette aus `StyleContext` (Menü, Titel-Schlüssel, angeschauter Block): Titel bestimmt die
   Familie (Truhe/Fass/Endertruhe/Shulker/Trichter/Spender), der Block verfeinert (Kupfertruhen-Stufen,
   Shulkerfarbe); sonst Standard je Menü (Eiche-Truhe). Füllfarben = W0-B-Palettentabelle, Rest über
   `UiPalette.derived` (gleiche Regel wie `derive` im Vorschau-Skript; Test prüft Eiche/Trichter gegen die Tabelle).
5. **Config** `config/simplecontainers.json` (nur Client): Hauptschalter `enabled` + `screens.<stil-id>` (Standard an);
   Cloth-Screen aus der Registry erzeugt (Forge: native-config wie simplesounds).
6. **SimpleLib gebündelt** (Prinzip 6a, wie Sandwiches: Fabric `include`+`implementation`, NeoForge `jarJar`, Forge
   `forgeBundleSimplelib`); `requires` nennt `cloth_config` + `simplelib`. Folge: SimpleLib bringt Blöcke mit – ein
   reiner Client ohne SimpleLib auf dem Server ist damit nicht garantiert (Registry-Abgleich). Offener Punkt für
   den Besitzer: UI-Bausteine später ggf. in eine inhaltsfreie Client-Bibliothek.
7. Tests: GameTests (Fabric @GameTest + NeoForge-Katalog) für BoxLayout (alle W0-Geometrien, zu enge Lücke → null),
   Registry-Abdeckung (alle W0-Menüs, exakte Klassen, eindeutige Ids), Palettenwahl, Config-Default; Standalone-Target
   Fabric/NeoForge; Client-Smoke mit Truhen-Screenshot.

### W0-A Verifikation (2026-10-08, sb-test)
- Compile: `:modules:simplecontainers:{fabric,neoforge}:compileJava`, `:modules:simplelib:{fabric,neoforge}:compileJava`,
  `:integration:compileGametestJava`, Forge (`-Pforge263=true`) simplecontainers + simplelib: grün.
- `run.py --targets module-simplecontainers-{fabric,neoforge,standalone-fabric,standalone-neoforge}-263`:
  „alles gruen: 20/20 bestanden, 0 rot“. `module-simplelib-fabric-263` (Tiegel-Regression): 25/25 grün.
- Client-Smoke `module-simplecontainers-client-263` (unter `xvfb-run`, ohne Display stürzt der Client bei SDL ab):
  „alles gruen: 7/7 bestanden, 0 rot“; Screenshots `/root/previews/simplecontainers/w0a/` (Truhe, Doppeltruhe, Fass,
  Endertruhe, Shulker, Trichter, Werfer).
- Nicht getestet: NeoForge/Forge-Client-Sicht, Tiegel-Bildschirm im Client (Code nur verschoben), reiner Client gegen
  Vanilla-Server, echte Blöcke angeschaut (Smoke öffnet Screens clientseitig ohne Block).
- Abweichungen: Paket `com.simplelib.api.client.ui` (statt `com.simplelib.client.ui`), Mixin je Screen-Klasse statt
  auf AbstractContainerScreen (Methode existiert dort nicht), Fugenregel/Paletten aus W0-B übernommen.
