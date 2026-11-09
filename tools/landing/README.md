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

## W12 / LAND11 (09.11.): Release-Downloads, Texturen automatisch, anpassbar
- **Paket zusammenstellen** (`paket.html`): Auswahl → Abhängigkeiten automatisch → Modloader → Download. Einzeldateien
  oder eine im Browser gebaute ZIP (eigene ZIP-Funktion in `paket_dl.js`, kein CDN), jede Datei vorher per SHA-256
  geprüft, `LIESMICH-README.txt` mit Prüfsummen und Links der Fremd-Mods in der ZIP, Prüfsummen auch als `sha256sums-<loader>.txt`.
  Was in einem gewählten Jar schon steckt (jar-in-jar, z. B. SimpleLib, Sub-Mods der Super-Mods), wird nicht doppelt geladen.
  Fremd-Mods (Fabric API, Cloth Config, JEI ...) nur als Modrinth-Link, nie mitverteilt.
- Daten: `downloads/index.json` relativ zur Seite = `simplemods.de/downloads/` = `<downloads>/public/` (öffentlich, nur lesen),
  erzeugt von `tools/zentrale-web/public_downloads.py` (von `collect_downloads.py` nach jedem grünen Gate aufgerufen):
  neuester vollständiger grüner Build, nur veröffentlichte Loader (`PUBLIC_LOADERS`, derzeit nur Fabric 26.3).
- Texturen: `concepts.py` nimmt bei jedem Lauf Icons und Texturen frisch aus den Mod-Ressourcen (nur git-verfolgte eigene,
  nie build/, assets/minecraft oder `tools/textures/vanilla_derived.py`); Bilder der Basis tragen `data-ico="<mod>"`.
  `wiki/generate.py --all` ruft es auf (jeder gen-Schritt eines Bündels), `--check` meldet nur „veraltet“, bricht nie ab.
- Anpassen: `overrides.json` (Schlüssel mit `_` = Beispiel, ignoriert): `mods.<id>.icon/de/en`, `textures.<spec>` = andere
  Textur oder `file:bild.png`, `stages.<k>` (Reise), `discover.<mod>` (Kacheln). Danach `python3 tools/landing/concepts.py`.
- Auslieferung: `deploy_dom07.sh domains` (einmalig, mit nginx) bzw. `deploy_dom07.sh landing`; danach kopiert
  `sb-web-sync` die Seiten bei jedem Production-Stand selbst nach `/srv/landing`.
