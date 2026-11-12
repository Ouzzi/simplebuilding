# KONZEPT B – Gehostete Simple Livemap über simplemods.de (N29 c, 11.11.) – NUR Konzept, kein Bau

Besitzer 11.11.: „… oder geringe Bandbreite über meine Seite haben, und wer mehr haben will, zahlt – das aber später.“
Ziel: Ein Server-Betreiber ohne eigene Webseite bekommt eine Adresse wie `https://map.simplemods.de/<kartenname>/`,
ohne Port-Freigabe, ohne Proxy. Zeitpunkt: **nach Release** (Phase nach Release, vgl. Memory `phasen-nach-release`).
Voraussetzung: a) out of the box und b) Selbst-Hosten (b-livemap29) sind ausgeliefert; die Mod-Seite ändert sich hier
erst beim Bau.

## 1. Rahmen (Ist)

- **Mod:** Kacheln 128 × 128 px PNG, 5 Zoomstufen, nur erkundete Chunks; typische Kachel 2–12 KB. Eine Welt mit
  10 000 erkundeten Chunks ≈ 625 Kacheln auf Zoom 4 + ~210 auf den gröberen Stufen ≈ **5–10 MB**. Ein aktiver Server
  ändert im Spielbetrieb ~5–50 Kacheln pro Minute. `live.json` ≈ 0,2–2 KB.
- **sb-web (CT 109):** 1 GB RAM, **7,8 GB Platte (lief schon dreimal voll)**, nginx 1.26, hinter Cloudflare-Tunnel,
  ohne curl/rsync. Bandbreite über Cloudflare (Free) praktisch unbegrenzt für gecachte statische Dateien; der
  Uplink des Heim-Anschlusses ist der Engpass für alles, was **nicht** aus dem Cloudflare-Cache kommt.
- **Cloudflare:** Domains simplemods.de/simplewiki.de liegen dort (Memory `domains-simplemods-de`). R2 (Objektspeicher)
  hat 10 GB gratis, **kein Egress-Entgelt**; Workers gratis bis 100 000 Anfragen/Tag.

## 2. Varianten

### V1 – Relay-Upload auf sb-web (Push, statisch)
Der Spielserver lädt geänderte Kacheln + `live.json` per HTTPS-`PUT` mit API-Schlüssel an
`https://map.simplemods.de/api/upload/<karte>/…`; ein kleiner Dienst auf sb-web (Python, wie devqueue) prüft Schlüssel,
Kontingent, Pfadmuster und PNG-Kopf und legt die Dateien unter `/srv/hosted-maps/<karte>/` ab; nginx liefert sie
statisch (wie `nginx-map-static.conf.in`), Cloudflare cacht Kacheln 30–60 s.
- + kein offener Port beim Betreiber, Karte bleibt sichtbar, wenn der Spielserver aus ist; Mod braucht nur einen
  HTTP-Client (Java `HttpClient`), Lastspitzen trägt Cloudflare.
- − **Platte von sb-web** (7,8 GB) ist die harte Grenze: bei 10 MB/Karte ~300 Karten, realistisch mit Puffer 100.
  Upload-Verkehr läuft über den Heim-Uplink (klein: wenige KB/s je Server).

### V2 – Relay-Upload nach Cloudflare R2 + Worker (Push, statisch, ausgelagert)
Wie V1, aber der Upload-Endpunkt ist ein Cloudflare-Worker (Schlüssel/Kontingent in Workers KV oder D1), Dateien liegen in
R2, Auslieferung über den Worker bzw. eine R2-Public-Domain mit Cache.
- + Heim-Uplink und sb-web-Platte bleiben unberührt, 10 GB gratis (~1 000 Karten), keine Egress-Kosten, skaliert.
- − neue Technik (Worker-Code, Wrangler-Deploy, Cloudflare-Konto-Rechte), Abhängigkeit von Cloudflare; Kosten ab
  10 GB / 1 Mio. Schreibvorgänge im Monat (Class A: 4,50 $ je Mio.) – bei 50 Kacheln/min je Server ≈ 2,2 Mio./Monat
  für **einen** dauerhaft aktiven Server → Schreibrate muss gebündelt werden (s. 3.).

### V3 – Tunnel/Live-Proxy (Pull)
Der Spielserver hält eine ausgehende Verbindung (WebSocket) zu einem Relay; Besucheranfragen werden live durchgereicht.
- + immer aktuell inkl. Spielern, kein Speicher auf unserer Seite.
- − jede Kachelanfrage belastet den Spielserver **und** den Heim-Uplink; Karte weg, wenn der Server aus ist (genau der
  Fall, den der Besitzer beschreibt); langlebige Verbindungen auf 1-GB-Container; aufwendig (eigenes Protokoll,
  Missbrauch als allgemeiner Tunnel abwehren). **Verworfen.**

### V4 – nur Anleitung (Status quo nach b)
Betreiber hosten selbst (b). Kostet uns nichts, schließt aber Spieler ohne eigene Webseite aus. Bleibt ohnehin der
Weg für „viel Bandbreite ohne Bezahlung“.

## 3. Bausteine (für V1/V2 gleich)

- **API-Schlüssel:** Registrierung über die Zentrale/Wiki-Login (kein offenes Formular): Betreiber legt eine Karte an,
  bekommt Kartenname + Schlüssel (256 bit, nur einmal angezeigt, gespeichert als Hash). Mod-Config neue Gruppe
  `hosted`: `enabled` (Standard **aus**), `endpoint`, `map`, `key` (wie `web.token` geheim, `***` im Config-Befehl),
  `visibility` (`private` = Betrachter brauchen `#t=` wie heute, `public`). Rotation per Zentrale, alter Schlüssel sofort ungültig.
- **Upload-Protokoll:** gebündelt statt je Kachel: alle 60 s (einstellbar 60–600) ein `POST` mit einem ZIP/Tar der
  geänderten Kacheln (max. 2 MB je Bündel) + `live.json`; Antwort enthält verbleibendes Kontingent. Bei 429/507 setzt die
  Mod aus (exponentiell bis 30 min) und meldet einmal im Log. Nur Kacheln, die die Mod selbst geschrieben hat
  (Pfadmuster wie `LivemapWeb.TILE`), PNG-Signatur + max. 1 MiB je Datei serverseitig erneut geprüft.
- **Kontingente (Gratisstufe, Vorschlag):** 25 MB Speicher je Karte, 1 Karte je Konto, 2 000 Kachel-Uploads/Stunde,
  Auslieferung 2 GB/Monat (zählt nur Cache-Misses am Ursprung), `live.json` höchstens alle 10 s, Spielerpositionen
  standardmäßig aus. Inaktive Karten (90 Tage kein Upload) werden nach Mail-/Zentrale-Hinweis gelöscht.
- **Ratenlimits:** Upload je Schlüssel (Token-Bucket) + je IP; Auslieferung je Besucher-IP (CF-Connecting-IP, wie
  `nginx-http.conf.in`: 10 r/s, Burst 80) und je Karte (Gesamtbudget, darüber 429 + Hinweisseite „Kontingent erreicht“).
- **Speicher-Wächter:** globale Obergrenze (V1: 3 GB von 7,8 GB, Upload-Stopp ab 80 % Plattenfüllung, Alarm in der
  Zentrale); Quoten werden beim Upload geprüft (`du` je Karte gecacht, nicht je Anfrage).
- **Betrachter:** dieselbe `index.html` aus dem Jar (relativer Pfad, läuft unter `/<karte>/`), CSP wie die Mod.

## 4. Datenschutz / Spielernamen

- Kacheln zeigen nur erkundetes Gelände (kein Seed, keine Spieler) – Gelände ist kein personenbezogenes Datum, Bauten
  können aber Namen/Schilder zeigen → Nutzungsbedingungen: Betreiber verantwortet Inhalte.
- `live.json` mit Spielerpositionen = **personenbezogen** (Spielername + Ort + Zeit). Gratisstufe: Spieler **aus** und
  serverseitig verworfen; nur bezahlte Stufe mit ausdrücklicher Betreiber-Bestätigung „Spieler wurden informiert“,
  Felder nur Name/Dimension/x/z (wie heute), `/simplelivemap hide` wirkt weiter, keine Speicherung von Verläufen
  (immer nur der letzte Stand, kein Log mit Positionen).
- Logs: nginx-Zugriffslog ohne Query, IP gekürzt (/24 bzw. /48) oder nach 7 Tagen gelöscht; Upload-Log nur Karte,
  Größe, Zeit. Datenschutzerklärung und Auftragsverarbeitung (Betreiber = Verantwortlicher für seine Spieler, wir =
  Auftragsverarbeiter) **vor** Start rechtlich prüfen lassen (Besitzer). Speicherort EU (sb-web zu Hause; R2 mit
  Jurisdiction „EU“).
- Löschen: Karte löschen in der Zentrale entfernt Dateien sofort, Cloudflare-Cache per Purge.

## 5. Missbrauch

- **Fremdinhalte/Dateiablage:** nur PNG mit gültigem Kopf, feste Größe 128 × 128 (Dekodieren der Kopfzeile reicht),
  nur das Kachelmuster, keine anderen Dateitypen → kein allgemeiner Dateispeicher. Gesamtgröße je Karte gedeckelt.
- **Anstößige Bilder** (man kann mit Blöcken „malen“): Melde-Link auf jeder gehosteten Karte, Sperren per Zentrale,
  Nutzungsbedingungen; öffentliche Karten erst nach Konto-Alter X Tage.
- **Schlüssel-Diebstahl:** Schlüssel nur in der Server-Config (wie Token), Rotation, Upload nur von einer IP je Stunde
  wechselbar (Hinweis statt Sperre), Anomalie (Speicher springt) → Karte pausieren.
- **DoS/Kosten:** Cloudflare vorne, Kontingente, Gesamtbudget je Monat (Notbremse: Gratisstufe pausiert, bezahlte
  laufen weiter), kein Proxy zu Spielservern (V3 verworfen).
- **Scraping/Seed-Rückschluss:** Karten zeigen nur Erkundetes; private Karten brauchen den Schlüssel.

## 6. Kosten (grobe Schätzung, monatlich)

| | V1 sb-web | V2 R2 + Worker |
|---|---|---|
| Speicher 100 Karten × 10 MB = 1 GB | 0 € (vorhandene Platte, aber knapp) | 0 € (≤ 10 GB) |
| Auslieferung | Cloudflare gratis; Ursprung = Heim-Uplink (Cache-Misses) | 0 € Egress |
| Uploads 100 Server, nur geänderte Kacheln | Strom/Uplink vernachlässigbar | R2 speichert je Kachel ein Objekt (ein Schreibvorgang); 100 Server × ~10 geänderte Kacheln/min ≈ 43 Mio./Monat → ~190 $ (!). Mit 5-min-Takt und Kachel-Entprellung (eine Kachel höchstens 1×/10 min) ≈ 2–4 Mio. → ~5–15 $ |
| Betrieb | eigener Python-Dienst + Wächter | Worker-Code + Konto |
| Risiko | Platte voll (3× passiert) | Kostenfalle bei Fehlkonfiguration → Budget-Alarm |

Bezahlstufen (später, nicht Teil dieses Konzepts zu bauen): z. B. „Plus“ 250 MB, 3 Karten, Spieler an, 20 GB/Monat;
„Server“ 2 GB, 10 Karten. Abrechnung über einen Zahlungsanbieter mit Abo (Stripe/Paddle, Merchant of Record für
USt.), Kontingente aus der Stufe in der Zentrale gepflegt. Preise erst nach Messung echter Nutzung festlegen.

## 7. Empfehlung

1. **Jetzt (vor Release): nichts bauen**, a) + b) reichen für „funktioniert immer“ und „eigene Seite“.
2. **Nach Release, Stufe 1: V1** (Relay-Upload auf sb-web, statisch), Gratisstufe nur **privat, ohne Spieler**,
   max. 25 MB je Karte, Gesamtgrenze 3 GB, Einladung (Wiki-Konto) statt offener Registrierung, ~20 Test-Server. Grund:
   nutzt vorhandene Bausteine (nginx-Statik-Vorlage, devqueue-Login, Wächter-Muster), keine neuen Konten/Kosten; die
   Messwerte (Kacheln/min, MB je Karte) bestimmen die Bezahlstufen.
3. **Stufe 2, sobald > 50 Karten oder Platte > 60 %: V2** (R2 + Worker) mit demselben Upload-Protokoll – die Mod
   ändert sich dabei nicht (nur `endpoint`). Erst dann Bezahlstufen + Spielerpositionen (mit Rechtsprüfung).
4. V3 (Live-Tunnel) nicht bauen.

## 8. Offene Besitzer-Entscheidungen (beim Bau, nicht jetzt)

- Adresse: `map.simplemods.de/<karte>/` (empfohlen, eigene Subdomain wegen Cookie-Trennung vom Wiki) oder `wiki.simplemods.de/maps/`.
- Registrierung: nur Wiki-Konten (empfohlen) oder offen mit Mail-Bestätigung.
- Ob die Gratisstufe öffentliche Karten erlaubt (Missbrauchsaufwand) – Empfehlung: nein, nur privat mit Link.
- Rechtsprüfung Datenschutz/AGB vor Start der Gratisstufe.
