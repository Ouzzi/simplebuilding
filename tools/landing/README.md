# Landing Page simplemods.de (IG31-54)

`index.html` ist die einzige Landing Page (Entwurf A unverändert, nur die Icon-Galerie unten nach Super-Mod/Sub-Mods gruppiert, Besitzer 06.11.). `python3 build.py [ausgabeordner]` erzeugt sie
aus den Mod-Icons (`modules/*/shared/resources/assets/*/icon_128.png`); Struktur Super-Mod/Sub-Mods aus `modules/modules.json`
(`tools/modtree/modtree.py`), die Gruppierung folgt automatisch. Keine Vanilla-Texturen. Entwuerfe A/B/C und ihr Bau-Skript: `archiv/`.

## Auslieferung (nur Plan, nichts deployt)
`index.html` nach `/srv/landing` kopieren. nginx:

    server { server_name simplemods.de; root /srv/landing; index index.html; }

`wiki.simplemods.de` bleibt das Wiki; Links der Seite: `/setup/` (Installer) und `https://wiki.simplemods.de`.
Bis dahin zeigt simplemods.de auf das Wiki.

## LAND09 (06.11.): neue Basis + drei Konzepte
`python3 concepts.py [vorschau-ordner]` schreibt `index.html` (= vorschlag-2.html, nur größere H1/H2 und Abstände)
und `konzept-1.html` (Modpack-Baukasten), `konzept-2.html` (Reise durchs Spiel), `konzept-3.html` (Schaufenster).
Daten: modules.json/modtree.py, Lang-Dateien, git-verfolgte Mod-Texturen (nie build/, wiki/, assets/minecraft).

## LAND10 (08.11.): vier Seiten
`python3 concepts.py [vorschau-ordner]` erzeugt `index.html` (Default + Navigationsleiste), `paket.html` (K1), `reise.html` (K2),
`discover.html` (K3), alle mit Navigation Start | Paket zusammenstellen | Reise | Discover, relative Links, Sprache in localStorage `sm-lang`.
`konzept-*.html` liegen in `archiv/`. Auslieferung: alle vier HTML-Dateien nach `/srv/landing` (`deploy_dom07.sh` kopiert sie).
