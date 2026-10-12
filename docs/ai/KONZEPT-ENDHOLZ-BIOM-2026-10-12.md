# Konzept: End-Holz, End-Knospe und neues End-Biom (2026-10-12, Nachtrag 44)

Status: Konzept. Offene Punkte stehen am Ende als Liste für den Besitzer (`[?]`).

## 1. End-Holz
- Neue Holzart, Farbe grau, leicht braun, blass; eigenes End-Design (nicht Recolor).
- Vollständiges Holzset wie Vanilla: Stamm, entrindeter Stamm, Holz, entrindetes Holz, Bretter, Treppe, Stufe, Zaun,
  Zauntor, Tür, Falltür, Knopf, Druckplatte, Schild, Hängeschild, Boot, Kistenboot.
- Zusätzlich passend zur Mod: Octets (Achtel), geschnitztes Holz, Holzkessel (normal + entrindet), Regal-Variante.
- Tags: `#logs`, `#planks`, `#logs_that_burn`? (siehe Liste), Holzarten-Tags der Mod.

## 2. End-Knospe (Setzling-Ersatz)
- Neuer Blumenblock „End-Knospe“, platzierbar auf Endstein (und End-Gras-Blöcken des Bioms).
- Wachstum nur per Knochenmehl oder zufällig (siehe Liste); Ergebnis hängt vom Licht ab:
  - **Hell** (Lichtstufe ≥ Schwelle): mächtiger, großer End-Baum (breite Krone, dicker Stamm).
  - **Dunkel** (unter Schwelle): schmales, hohes End-Gestrüpp aus Stöcken (Holz = Stöcke).
  - **Natürlich im End** (Weltgenerierung): viel schmaler, erinnert an Chorus-Pflanzen (verzweigt, knubbelig).

## 3. Neues End-Biom
- Generiert: Chorus-Pflanzen, End-Gras/-Büsche (Nachtrag 42), End-Knospen und die natürlichen, schmalen End-Bäume.
- Biom-Tint für Gras/Blätter: grau-braun.
- Liegt auf den äußeren End-Inseln (nicht auf der Hauptinsel mit dem Drachen).

## 4. Verträge zur Mod
- Modul-Zugehörigkeit (siehe Liste), allein spielbar (Prinzipien Modul-Unabhängigkeit).
- Wiki, Guide-Seite, Lang EN/DE, Fragentisch-Texte, Money-Preise, Testzentrale-Station, Gametests
  (Wachstum hell/dunkel, Biom-Generierung, Rezepte, Holzset vollständig).

## Offene Punkte (Besitzer füllt aus)
1. [x] Holzarten: Aschholz (Ashwood) und Dämmerholz (Duskwood) – zwei Sets (Besitzer 12.10.). Alt: Name der Holzart (Vorschläge: „Ashwood“/Aschholz, „Pale End“, „Voidwood“/Leerholz, „Ender Oak“).
2. [?] Name der Knospe (z. B. „End-Knospe“, „Leerknospe“).
3. [x] Biome: „Ash Valley“ und „Dawnlands“ (Besitzer 12.10.; vermutlich je Holzart ein Biom). Alt: Name des Bioms (z. B. „Ashen Thicket“/Aschdickicht, „Chorus Grove“/Chorushain).
4. [?] Lichtschwelle hell/dunkel (Vorschlag: Licht ≥ 9 = Baum, < 9 = Gestrüpp; Block- oder Himmelslicht?).
5. [?] Wächst die Knospe auch ohne Knochenmehl (zufällige Ticks) und auch außerhalb des Ends (Oberwelt/Nether)?
6. [?] Blätter: hat der große Baum Blätter? Wenn ja: Farbe (Biom-Tint grau-braun) und Drops (Knospe, Stöcke, Frucht?).
7. [?] Gestrüpp: welche Blöcke genau (Stöcke als eigener Block „End-Stöcke“ oder normale Stämme in dünn)?
8. [?] Brennbar wie Oberwelt-Holz oder feuerfest wie Nether-Holz?
9. [?] Biom-Größe und -Häufigkeit (Vorschlag: so häufig wie End-Highlands-Teilstücke, Flecken 3–6 Chunks).
10. [?] Ersetzt das Biom Teile der End Highlands oder kommt es zusätzlich zwischen den Inseln?
11. [?] Chorus-Dichte im Biom (mehr als normal / gleich / weniger)?
12. [?] Eigene Biom-Extras: Partikel, Himmels-/Nebelfarbe, Musik, Mob-Spawns (z. B. niedlicher End-Mob aus Nachtrag 17)?
13. [?] Frucht/Nahrung vom Baum (z. B. „End-Beere“, Chorus-ähnlicher Teleport-Effekt)?
14. [?] Modul: in SimpleBuilding, eigenes Sub-Mod (z. B. „Simple End“), oder zu Simple End Dragon Fight?
15. [?] Holzgröße großer Baum (Vorschlag: 2×2-Stamm, 12–18 hoch) und Gestrüpp (1×1, 8–14 hoch)?
16. [?] Soll das End-Gras (Nachtrag 42) zum Biom gehören und den grau-braunen Tint nutzen statt Endstein-Farbe?

## Besitzer-Antworten 12.10. (verbindlich)
- Holzarten: Aschholz (Ashwood) und Dämmerholz (Duskwood; etwas brauner). Texturen: Runde-1-Sets B1/B3 (docs/ai/previews/n44/).
- Knospen: „Ash Bud“ / „Dusk Bud“ (je Holzart eine). Biome: „Ash Valley“ (Aschholz) und „Dawnlands“ (Dämmerholz).
- Licht: ≥ 9 = großer Baum, < 9 = Gestrüpp.
- Wachstum: von selbst nur mit Licht, sonst nur per Knochenmehl; in den Biomen bereits ausgewachsen generiert.
- Blätter: ja; droppen Knospen, Stöcke und die neue Frucht.
- Gestrüpp: eigener Block „End-Stöcke“ (dicker als platzierbare Stöcke, Form an Tropfstein erinnernd, aus End-Stöcken).
- Holz brennbar; gedroppte Items „nicht voidbar“ (fallen nicht ins Void verloren – Umsetzung: Item treibt über Y-Grenze zurück / schwebt) und doppelte Despawn-Dauer.
- Biom-Größe: Flecken 3–6 Chunks; ersetzt Teile der End Highlands oder zusätzlich – was technisch besser ist.
- Chorus im Biom: weniger als Stöcke-Bäume.
- Extras: Partikel, Himmels-/Nebelfarbe, Musik ja; Mobs erst später.
- Frucht: neue Frucht mit eigenem Zweck „kurzer Schwebeflug“ (Essen: 3–5 s Slow Falling + kleiner Sprung).
- Baum: großer Baum mit 1 Stamm; 2×2 Knospen ergeben einen noch größeren Baum (wie Fichte/Dunkeleiche).
- End-Gras: grau-brauner Biom-Tint.
- Modulstruktur: SimpleBuilding enthält neues Sub-Mod **Simple End**; Simple End enthält **Simple End Dragon Fight** als Sub-Mod (verschachtelt, eigener Schalter je Ebene). End-Holz, Biome, End-Gras (N42), Astral/Nihil-Lampen bleiben in SB? → Simple End bekommt End-Holz, Biome, End-Gras.
