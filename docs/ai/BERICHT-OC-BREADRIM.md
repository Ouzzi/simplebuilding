# Bericht oc-breadrim (Nachtrag 61: Sandwich-Brot v8, Rand an der Unterseite)
- Aenderung nur in `tools/textures/sandwiches.py` (`bun_bottom_v8`): 1-px-Kontur in `BUN_WARM[0]` (3a1d08, wie die Oberseite) an allen offenen Seiten/unten der Unterseite.
- Neu erzeugt: `item/sandwich/bottom.png` und `bottom_buttered.png`; alle Stufen (leer, 1-5 Zutaten) setzen sich aus diesen Ebenen zusammen. Brothaelfte war schon rundum umrandet -> Pixel unveraendert. Sonst keine Textur geaendert (2 von 240 Dateien).
- Vorschau 8x vorher/nachher: `docs/ai/previews/oc-breadrim/sandwich-bread-{vorher,nachher}-8x.png` (angesehen; Referenz-PNG /root/previews/... existiert im CT nicht, v8-Zustand aus dem Generator).
- Tests: `sandwiches.py <vanilla> --check` -> "sandwich textures current (240 files)"; `check_missing_textures.py` -> 0 errors (11 bestehende Warnungen).
- Abweichung: Unterseite liegt links schon in Zeile 15 (Bildrand), daher ersetzt die Kontur dort die unterste Krustenzeile statt eine Zeile anzuhaengen; Staerke 1 px wie oben.
- Nicht getestet: Ingame-Ansicht, Gate.
