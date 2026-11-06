# Landing Page simplemods.de (IG31-54)

Drei Entwuerfe als eigenstaendige Seiten (Bilder als data-URI): `A.html` (Pixel), `B.html` (clean), `C.html` (verspielt).
`build.py [ausgabeordner]` erzeugt sie aus den Mod-Icons (`modules/*/shared/resources/assets/*/icon_128.png`) und
eigenen Item-Texturen aus `wiki/assets/textures`. Keine Vanilla-Texturen.

## Auslieferung (nur Plan, nichts deployt)
Nach Besitzer-Wahl wird die gewaehlte Datei als `index.html` nach `/srv/landing` kopiert. nginx:

    server { server_name simplemods.de; root /srv/landing; index index.html; }

`wiki.simplemods.de` bleibt das Wiki; Links der Seite: `/setup/` (Installer) und `https://wiki.simplemods.de`.
Bis dahin zeigt simplemods.de auf das Wiki.
