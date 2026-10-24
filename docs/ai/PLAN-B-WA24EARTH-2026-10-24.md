# Plan B – WA24-6 Simple-Earth-Vorschau V4–V6 (24.10.2026)

Ist: `/root/sb-tools-a/previews/earth-konzept.html` (V1–V3), Konzept `KONZEPT-SIMPLE-EARTH-2026-10-23.md`. Kein Spielcode, kein Gradle.

1. Ursache des Kantensprungs im Code finden und nachmessen (Gier-Ruck der kürzesten Drehung, Umbeschriften der Seitenflächen, Unterseite).
2. Gemeinsame Vorlage (three.js r128 von cdnjs wie das Original), daraus drei eigenständige Seiten V4/V5/V6:
   V4 Lage fortschreiben + Slerp + Seitenmischung 50/50; V5 Superellipsoid, Normale nach oben; V6 Faltnetz (Hereinklappen).
   Je Seite: Flugrichtung (8 + Stopp), Tempo, Pause, Testflüge, Außen/Ich-Ansicht, Vergleich „alt“, hell/dunkel, Handy-Layout.
3. Prüfen: `node --check`; Node-Lauf mit echtem three r128 und Attrappen-DOM: Drehung je Bild, Inhaltswechsel, Netz-Stetigkeit V6, NaN.
4. Konzept §3.7, Queue-Teilstand, Memory, nur eigenen Branch pushen.
Risiko: kein echter Browser auf sb-agent → WebGL-Shader/Optik ungeprüft (im Bericht nennen).
