# Landing Page simplemods.de (IG31-54)

`index.html` ist die einzige Landing Page (Entwurf A unverändert, nur die Icon-Galerie unten nach Super-Mod/Sub-Mods gruppiert, Besitzer 06.11.). `python3 build.py [ausgabeordner]` erzeugt sie
aus den Mod-Icons (`modules/*/shared/resources/assets/*/icon_128.png`); Struktur Super-Mod/Sub-Mods aus `modules/modules.json`
(`tools/modtree/modtree.py`), die Gruppierung folgt automatisch. Keine Vanilla-Texturen. Entwuerfe A/B/C und ihr Bau-Skript: `archiv/`.

## Auslieferung (nur Plan, nichts deployt)
`index.html` nach `/srv/landing` kopieren. nginx:

    server { server_name simplemods.de; root /srv/landing; index index.html; }

`wiki.simplemods.de` bleibt das Wiki; Links der Seite: `/setup/` (Installer) und `https://wiki.simplemods.de`.
Bis dahin zeigt simplemods.de auf das Wiki.
