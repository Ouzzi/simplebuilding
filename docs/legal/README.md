# docs/legal – Rechtstext-Entwürfe für Simple Livemap als Dienst (N34)

> **STATUS: ENTWURF – nicht ausliefern. Keine Rechtsberatung.**
> Vor einer Veröffentlichung müssen alle Texte von einer Anwältin / einem Anwalt (IT-Recht, Datenschutzrecht, Verbraucherrecht)
> bzw. einem geprüften Generator (mit Prüfung der Ergebnisse) geprüft und freigegeben werden. Die Dateien liegen bewusst **nur** in
> `docs/legal/` (Plan D21): nicht unter `tools/livemap-web/static`, nicht im Dienst, nicht im Wiki, nicht verlinkt.
> Stand: 12.11.2026 · Branch `a-livemap34` · Auftrag: `/root/b-auftrag-20261109.md` „## N34“, Besitzer-Zusatz 6.

## 1. Zweck

Der Dienst „Simple Livemap“ (`livemap.simplemods.de`, Konto auf `account.simplemods.de`) speichert personenbezogene Daten
(Konto, Karten, Spielernamen/-positionen, IP-Adressen in Logs) und soll später Abos über Stripe verkaufen. Vor einer Veröffentlichung
(öffentliche Free-Stufe) und erst recht vor dem Bezahlstart braucht der Betreiber (Privatperson/Einzelunternehmer in Deutschland,
Hosting zu Hause, Auslieferung über Cloudflare) Impressum, Datenschutzerklärung, AGB samt Widerrufsbelehrung und eine
Auftragsverarbeitungs-Grundlage. Diese Entwürfe sind die **Arbeitsgrundlage für die Prüfung**, kein fertiger Text.

## 2. Dateien

| Datei | Inhalt | Wirksam ab |
|---|---|---|
| [`impressum.md`](impressum.md) | Impressum nach § 5 DDG (ersetzt TMG seit 14.05.2024), ggf. § 18 Abs. 2 MStV, DSA-Kontaktstelle (Art. 11/12), Verbraucherstreitbeilegung, Haftungs-/Marken-Hinweise | Start der öffentlichen Seiten |
| [`datenschutz.md`](datenschutz.md) | Datenschutzerklärung nach Art. 13/14 DSGVO: Logs, Cloudflare, Konto, Sitzungen, Signup-Tag, Ratenlimits, E-Mail, Karten, Spielernamen/Positionen, Cookies (§ 25 Abs. 2 TDDDG), Stripe (später), Empfänger, Drittland, Rechte, Art. 22 | Start der öffentlichen Seiten |
| [`agb.md`](agb.md) | Nutzungsbedingungen/AGB: Stufen und Limits, Verbinden/Puffer/Missbrauchsschutz, Inhalte, DSA-Melde- und Abhilfeverfahren, Verfügbarkeit, Preise/Zahlung, Laufzeit/Kündigung (§ 309 Nr. 9, § 312k BGB), Widerruf (§ 356 Abs. 4 BGB), Haftung; Anlagen: Widerrufsbelehrung, Muster-Widerrufsformular, Texte für Bestellvorgang | Abschnitte 1–8, 13–17 mit Free-Start; 9–12 und Anlagen mit Bezahlstart |
| [`auftragsverarbeitung.md`](auftragsverarbeitung.md) | Teil A: Liste der Dienstleister/Auftragsverarbeiter (Cloudflare, Stripe, SMTP, Hosting, Backup, R2, Registrar) mit Status, Prüf-Checkliste, TOM-Gerüst. Teil B: Gliederung eines AV-Vertrags (Art. 28 DSGVO) für Kartenbesitzer (Spielerpositionen) mit Mustertext zur Spielerinformation | Teil B vor dem Einschalten von „Spieler zeigen“ |

## 3. Konventionen

- `⟦PLATZHALTER: …⟧` = Angabe oder Entscheidung des Besitzers (Name, Anschrift, Fristen, Vertragsdaten). Nichts davon wurde erfunden.
- `> **[Anmerkung Entwurf]**` = Hinweis an den Besitzer/Prüfer; **vor Veröffentlichung entfernen**. Dasselbe gilt für den Kopf-Kasten jeder Datei.
- „Variante A/B/C“ = Alternativen, von denen eine gewählt wird.
- Ansprache „Sie“ (förmlich). Wer „du“ will (Community-Ton), muss das durchgängig umstellen.
- Jede Datei endet mit „ENTWURF – keine Rechtsberatung. Vor Veröffentlichung prüfen lassen.“
- Paragrafen-Verweise sind **nach bestem Wissen** gesetzt, nicht gegen den aktuellen Gesetzestext geprüft (kein Rechtsportal im Arbeitsumfeld);
  besonders prüfen lassen: § 357a BGB, § 312k BGB, §§ 327 ff. BGB, DDG, VSBG, DSA-Artikel.

## 4. Grundlage und Annahmen

Quellen: `docs/ai/PLAN-A-LIVEMAP34-2026-11-12.md` (D1–D22, Mod-API), `/root/sb-tools-a/previews/a-livemap34-preview.html` (8 Abschnitte, alle abgenommen),
`tools/livemap-web/tiers.json`, Auftrag N34 Zusatz 6 und die vom Auftraggeber genannten Datenflüsse. **Der Dienst ist noch nicht gebaut**
(`tools/livemap-web/smweb` leer); die Texte beschreiben das **geplante** Verhalten. Annahmen: Betreiber = Privatperson/Einzelunternehmer in
Deutschland; Hosting zu Hause (Proxmox-Container) hinter Cloudflare Tunnel/CDN; Bezahlen über Stripe erst später; E-Mail über einen noch
zu wählenden SMTP-Anbieter; kein Newsletter, keine Werbung, keine Analyse-Tools (jeweils als Platzhalter zur Bestätigung markiert).

## 5. Befunde und Entscheidungen im Entwurf

Wo der Entwurf **von der Spezifikation abweicht** oder sie **ergänzt**, steht es hier (Besitzer entscheidet; Details in den Dateien):

| ID | Befund | Behandlung im Entwurf |
|---|---|---|
| B1 | **Jahresabo-Verlängerung.** Spezifikation: „verlängert sich nur, wenn nicht gekündigt“ (= automatisch um ein weiteres Jahr). Gegenüber Verbrauchern nach § 309 Nr. 9 Buchst. b BGB unwirksam. | AGB 11.3: Variante A (nach 12 Monaten Übergang in monatlich kündbares Abo, empfohlen), B (endet), C (Spezifikation, nur B2B). **Entscheidung nötig.** |
| B2 | **Spezifikation Abschnitt 5 „gespeichert werden nur E-Mail, Name, Passwort-Hash, Karten und Nutzungszahlen“** ist unvollständig: tatsächlich auch Sitzungen, Signup-Tag, Verbindungszähler, Welt-ID, geteilte Konten, Live-Daten, Logs, Cloudflare. | Datenschutz nennt die vollständige Liste. Landing-/Registrierungs-/FAQ-Texte dürfen **nicht** „nur …“ versprechen. |
| B3 | **Zusatzleistungen** von Max/Ultra (Einbetten, Verlauf/Schnappschüsse, eigene Domain, ohne Branding, API) liegen laut Spezifikation Abschnitt 8 in „Später“. | AGB 3.2 als Platzhalter: erst Vertragsbestandteil, wenn gebaut; vor Bezahlstart bauen oder aus Preisseite/Tabelle streichen (§ 327e BGB: Beschaffenheit nach Beschreibung). |
| B4 | **Puffer bei Herabstufung.** Free-Puffer = 1 Karte, Herabstufen = überzählige Karten 30 Tage (kann mehr als 1 sein). | AGB 4.6 stellt klar: 30 Tage unabhängig von der Puffer-Anzahl der neuen Stufe; Regel „welche Karten“ als Platzhalter (Plan D15). |
| B5 | **Spielerpositionen bei Free?** Konzept-Vorlauf: nur bezahlte Stufe. Spezifikation/Plan: alle Stufen, „Spieler zeigen“ Standard aus. | Entwurf folgt Spezifikation/Plan; AV-Vertrag B.5 nennt die Frage. |
| B6 | **Ratenlimits „nur im Speicher“** – aber nginx schreibt Ablehnungen (`limit_req`) mit IP ins Fehler-Log. | Datenschutz 3.1/4.4 nennen das; Logrotation muss auch das Fehler-Log erfassen. |
| B7 | **Aufrufe** zählen laut Plan D16 „je Konto und Tag“, im Dashboard „je Karte“. | Beides genannt; Zählweise im Code festlegen und Text angleichen. |
| B8 | **Kündigungsbutton (§ 312k BGB), Bestell-Button (§ 312j BGB), Zustimmung zum vorzeitigen Leistungsbeginn** sind in der Spezifikation nicht vorgesehen. | AGB 10–12 und Anlage 3 beschreiben Pflichttexte; im Dienst vor dem Bezahlstart zu bauen. |
| B9 | **AV-Annahme vor „Spieler zeigen“** ist nicht Teil der Spezifikation. | AV-Datei B.2 schlägt sie vor (Kästchen, Zeitpunkt, Version, serverseitig). |
| B10 | **Mod sendet Spielerangaben auch bei „Spieler zeigen“ aus?** (Plan: der Dienst übernimmt sie dann nicht.) | Datenschutz 7.1 neutral formuliert + Platzhalter; besser: Mod sendet dann nichts. |
| B11 | **DSA-Pflichten** eines Hosting-Dienstes (Kontaktstelle, Melde- und Abhilfeverfahren, Begründung) sind nicht vorgesehen. | Impressum (Kontaktstelle), AGB 6, Melde-Link auf der Karte (Konzept sah ihn vor). |
| B12 | **Wiki-/Zentrale-Login** zieht später in dieselbe DB um; Cookie gilt für `*.simplemods.de`. | Datenschutz 2 und 4.3: Geltungsbereich/Platzhalter – gemeinsame Erklärung für alle Simple-Mods-Seiten erwägen. |

## 6. Prüfrunden-Protokoll

### Runde 1 – Vollständigkeit und Formales (mechanisch, per Skript)

Gegenstand: alle vier Dateien. Geprüft: Platzhalter-Format, Endzeile, Querverweise, Stufenwerte gegen `tiers.json`, Datenliste des Auftraggebers.

Gefunden und geändert:
1. **Platzhalter-Format uneinheitlich** (27 Treffer in 3 Dateien): nackte `⟦PLATZHALTER⟧`, Mustervariablen ohne Präfix (`⟦Betreiber⟧`, `⟦Kontakt⟧` …), Bindestrich statt Doppelpunkt (`⟦PLATZHALTER – Variante A …⟧`), ein **verschachtelter** Platzhalter in Datenschutz 11. Alle auf `⟦PLATZHALTER: …⟧` mit Kontext vereinheitlicht; Klammern balanciert.
2. **Querverweise**: alle „Abschnitt x.y“ in AGB und Datenschutz zeigen auf vorhandene Gliederungspunkte (0 fehlend). Korrigiert: AGB 1.2 nannte „Abschnitte 11 und 12“ pauschal als Verbraucherregeln (jetzt 11.4, 12, Anlagen 1–3); Datenschutz 2 sagte „Abschnitte 4 bis 11“ (jetzt 3 bis 11).
3. **Stufenwerte**: Karten, öffentlich, Puffer (Anzahl/Tage), Aufrufe, Takte, Mindestzeiten, Platzsperre, Teilen, Speicher je Karte, Preise und Jahrespreise (39,90 / 79,90 / 149,90 €) stimmen mit `tiers.json` und der Spezifikation überein.
4. **Datenliste des Auftraggebers** gegen Datenschutz abgehakt (42 Punkte, u. a. PBKDF2, Token-Hash, 30 Tage, Signup-Tag/HMAC, RAM ≤ 1 h, Welt-ID ohne Seed, Kacheln mit Schildern, „Spieler zeigen“, nur letzter Stand, Aufrufe ohne IP, geteilte Konten, nginx, Cloudflare/DPF/SCC, Stripe, JSON-Export, Puffer 30/90/180/365): alles vorhanden.
5. **Lücke**: Kontakt per E-Mail und Meldungen rechtswidriger Inhalte als Datenverarbeitung fehlten → Datenschutz 4.5 und Tabellenzeile ergänzt.
6. Endzeile „ENTWURF – keine Rechtsberatung. Vor Veröffentlichung prüfen lassen.“ in allen Dateien vorhanden.

### Runde 2 – Widersprüche zu Spezifikation und Plan, Rechtsaussagen (inhaltlich, Lesen gegen Spezifikation/Plan D1–D22)

Gefunden und geändert (Befunde B1–B12 oben sind die Spezifikationsabweichungen; zusätzlich Textfehler):
1. **B1/B2/B3/B4/B6/B7/B10** wie in Abschnitt 5 beschrieben (Text angepasst bzw. Platzhalter gesetzt).
2. **Zu starke Behauptungen** entfernt oder als Platzhalter markiert: „Ratenzähler nie auf Datenträger“ (nginx-Fehlerlog), „Auffällige Konten werden von einer Person geprüft, bevor wir Maßnahmen ergreifen“ (Spezifikation: nur Admin-Liste), „keine Werbe-/Tracking-Cookies“ und „Mod sendet ohne `connect` nichts“ (jeweils zur Bestätigung beim Start).
3. **Interne Dateinamen** in öffentlichen Texten (`auftragsverarbeitung.md`) durch URL-Platzhalter ersetzt.
4. **Unsichere Gesetzesverweise** entfernt/berichtigt: „§§ 7 ff. DDG“ → allgemein (DDG-Paragrafen nicht sicher); Befreiung von der Teilnahme-Mitteilung nach VSBG: § 36 **Abs. 3** (nicht Abs. 1 Nr. 1); sonstige Verweise im README als „vom Prüfer zu bestätigen“ markiert.
5. **AGB-Einbeziehung** (§ 305 Abs. 2 BGB) fehlte → AGB 2.1: Hinweis, Abruf-/Speichermöglichkeit, Kästchen bei der Registrierung (Umsetzungshinweis U2).
6. **AGB 11.4** (Kündigungsbutton) war sprachlich fehlerhaft („im angemeldeten Bereich ohne Anmeldung“) → neu gefasst.
7. **Cloudflare**: Datenschutz sagt jetzt ausdrücklich, dass TLS dort endet und Anmeldedaten/Karteninhalte durchlaufen; Cloudflare-Cookies/Web Analytics als zu prüfende Punkte.
8. **Annahmen-Check Rollen**: Kartenbesitzer = Verantwortlicher, Dienst = Auftragsverarbeiter nur für Spielerdaten und Karteninhalte; für Konto/Sicherheit/Abrechnung eigener Verantwortlicher – in Datenschutz 7.2 und AV B.1 gleich beschrieben; Rollenabgrenzung (Art. 26) als offener Prüfpunkt.

### Runde 3 – Endkontrolle nach allen Änderungen (mechanisch, per Skript, plus Nachlesen der geänderten Stellen)

Geprüft nach den Korrekturen aus Runde 1 und 2:
1. **Stufentabelle der AGB gegen `tiers.json` programmatisch** (Zelle für Zelle, 4 Stufen × 10 Zeilen einschließlich Jahrespreise): alle Werte stimmen.
2. **Platzhalter**: in allen vier Textdateien ausnahmslos `⟦PLATZHALTER: …⟧`, Klammern balanciert, keine verschachtelten, keine nackten; Zahlen siehe Abschnitt 10.
3. **Endzeile** in allen fünf Dateien vorhanden; README-Verweise (Abschnitte 5–10) vorhanden.
4. **Durchsickern interner Verweise** (Dateinamen, `tiers.json`, Plan-IDs, Branch-Namen) in veröffentlichbarem Text: keine Treffer außerhalb von Kopf-Kästen, `[Anmerkung Entwurf]`-Blöcken und Platzhaltern.
5. **Nachlesen** der in Runde 2 geänderten Stellen (Datenschutz 3.1, 4.4, 4.5, 7.1; AGB 2.1, 11.4): lesbar, keine neuen Widersprüche zu Spezifikation/Plan gefunden.
Offene Befunde nach Runde 3: keine mechanischen; inhaltlich bleiben die Entscheidungen B1–B12 und die Checkliste in Abschnitt 7.

## 7. Was der Besitzer noch eintragen bzw. prüfen lassen muss

**Vor Start der öffentlichen Seiten / Free-Stufe (Impressum, Datenschutz, AGB 1–8, 13–17, AV Teil B):**

1. **Identität**: Name/Firma, **ladungsfähige Anschrift** (wird öffentlich), E-Mail und zweiter Kommunikationsweg (Telefon oder Kontaktformular), Datenschutz-Kontakt.
2. **Unternehmerstatus**: Gewerbeanmeldung, Finanzamt (Betriebseröffnung), Handelsregister ja/nein.
3. **Umsatzsteuer**: Kleinunternehmer (§ 19 UStG) ja/nein; USt-IdNr./Wirtschafts-IdNr.; **Preise inkl. USt** (PAngV); steuerliche Behandlung digitaler Dienste an Verbraucher in anderen EU-Staaten (OSS) – Steuerberater.
4. **Verbraucherstreitbeilegung** (§ 36 VSBG): bereit/nicht bereit, ggf. Schlichtungsstelle; § 36 Abs. 3 VSBG (≤ 10 Beschäftigte) prüfen.
5. **§ 18 Abs. 2 MStV**: ob ein Verantwortlicher für redaktionelle Inhalte zu nennen ist.
6. **DSA/Melde-System**: Kontaktstelle (Art. 11/12 DSA), Melde-E-Mail und Melde-Link (Art. 16), Beschwerde-Frist und -Verfahren (Art. 17), Umfang der Pflichten für Kleinstunternehmen.
7. **Datenschutzbeauftragter**: voraussichtlich nicht nötig (§ 38 BDSG, < 20 Personen) – bestätigen. **Zuständige Aufsichtsbehörde** (Landesdatenschutzbehörde des Betreibersitzes) eintragen.
8. **SMTP-Anbieter** wählen (Standort, AV-Vertrag, Versandgrenzen); Absenderdomain/SPF/DKIM.
9. **Log-Aufbewahrung** (Vorschlag 7 Tage, Zugriffs- **und** Fehler-Log), Log-Format (Query/Referrer/User-Agent?), ggf. IP-Kürzung.
10. **Cloudflare**: Vertragsgesellschaft, DPA/Standardvertragsklauseln abschließen/bestätigen, DPF-Zertifizierung und Rechtsprechung beobachten, Web Analytics aus, gesetzte Cookies prüfen, Tunnel-/Origin-Verschlüsselung, Aufbewahrung der Cloudflare-Logs.
11. **Hosting zu Hause**: Standort/Land, wer hat Zugriff, Festplattenverschlüsselung, Strom/USV, Backups (Ort, Intervall, Aufbewahrung), Update-Prozess → TOM-Dokument (Art. 32 DSGVO).
12. **Verzeichnis von Verarbeitungstätigkeiten** (Art. 30), **Löschkonzept**, **Datenpannen-Prozess** (Art. 33/34, 72 Stunden).
13. **Löschfristen** festlegen: Nutzungszahlen, Meldungen/Schriftverkehr, inaktive Konten (in der Spezifikation nicht geregelt), Backups.
14. **Cookies**: Name und Laufzeit des Sprach-Cookies und von `sm_csrf`; bestätigen, dass nur technisch notwendige Technik verwendet wird (§ 25 Abs. 2 TDDDG); Domain-Cookie für `*.simplemods.de`.
15. **Jugendschutz / Mindestalter**: Minecraft-Zielgruppe ist jung; Mindestalter für Konten, Zustimmung der Eltern bei bezahlten Stufen (§§ 107 ff. BGB).
16. **Mojang/Microsoft**: Marken-, EULA- und Commercial-Usage-Richtlinien für einen kostenpflichtigen Dienst mit Minecraft-Bezug; Kartenbilder enthalten Spielinhalte; Disclaimer „kein offizielles Minecraft-Produkt“.
17. **Lizenz der Mod** und Credits-Seite (Link auf `CREDITS.md`/Credits-Seite) benennen.
18. **Spielerdaten / AV-Vertrag** (Teil B): Inhalt anwaltlich prüfen, Rollenabgrenzung (Auftragsverarbeitung gegenüber Art. 26), Hinweis zur Haushaltsausnahme, Funktion zum Ausblenden einzelner Spieler benennen, Unterauftragsverarbeiter, Frist für Datenpannen-Meldung.
19. **Haftung/AGB-Standardklauseln**: Haftung (§§ 307 ff. BGB), Free-Haftung, Gerichtsstand, Freistellung, Kündigungsfrist des Betreibers für Free, Verfügbarkeitszusage für bezahlte Stufen, Datenverlust-Klausel.
20. **Einwilligungstext** für /explore, falls das später gebaut wird.
21. **Freigabe-Termin**: Texte in `static`/Routen (`/impressum`, `/datenschutz`, `/agb`, `/widerruf`) erst **nach** Anwaltsprüfung einspielen (Plan D21); Links in der Fußzeile **aller** Seiten beider Hosts.

**Zusätzlich vor dem Bezahlstart (AGB 9–12, Anlagen, Datenschutz 9, AV Teil A Nr. 2/6):**

22. **Stripe-Konto**: Vertragsgesellschaft (Irland/USA), DPA, Rollen (Verantwortlicher/Auftragsverarbeiter), Zahlungsarten, Steuer-Einstellungen, Rechnungsdaten.
23. **Endgültige Preise und Limits** bestätigen (Spezifikation: „Vorschlag“), Speicher je Karte (64 MB / 256 MB / 1 GB / 4 GB), Zusatzleistungen nur wenn gebaut (B3).
24. **Jahresabo-Variante** wählen (B1); Preisänderungs- und AGB-Änderungs-Klauseln (Zustimmungsfiktion) prüfen lassen.
25. **Widerrufsbelehrung und Muster-Widerrufsformular** wortgleich mit dem aktuellen amtlichen Muster (Anlage 1/2 zu Art. 246a § 1 Abs. 2 Satz 2 EGBGB) abgleichen; Formulierung für Abos (Wertersatz, Erlöschen nach § 356 Abs. 4 BGB); Frage, ob Free ein Widerrufsrecht begründet (§ 312 Abs. 1a BGB).
26. **Button-Lösung**: „zahlungspflichtig bestellen“ (§ 312j Abs. 3 BGB), Zusammenfassung vor Absenden (§ 312j Abs. 2, Art. 246a § 1 EGBGB), Bestätigung auf dauerhaftem Datenträger (§ 312f BGB), Kästchen für vorzeitigen Leistungsbeginn.
27. **Kündigungsbutton** (§ 312k BGB): Beschriftung, Erreichbarkeit auch ohne Login, Bestätigungsseite, Eingangsbestätigung.
28. **BFSG**: ob die Kleinstunternehmen-Ausnahme für Dienstleistungen greift; ggf. Erklärung.
29. **Aufbewahrung** von Rechnungen (§ 257 HGB, § 147 AO), Buchhaltung, Umsatzsteuer-Voranmeldung.
30. **Objektspeicher (Cloudflare R2)**: Jurisdiktion „EU“, Eintrag in AV-Liste und Datenschutz.
31. Optional: Haftpflicht-/Cyber-Versicherung für den Dienst klären.

## 8. Abgleich mit dem Code / Umsetzungshinweise

Die Texte beschreiben den **geplanten** Dienst. Beim Bau (oder vor der Veröffentlichung) sind folgende Punkte zu prüfen bzw. zu bauen:

- **U1** Fußzeile mit Impressum/Datenschutz/AGB auf allen Seiten beider Hosts; Routen für die Texte (erst nach Freigabe, Quelle `docs/legal`, nicht jetzt).
- **U2** Registrierung: Kästchen „AGB akzeptiert, Datenschutz zur Kenntnis genommen“, Mindestalter; AGB-Version und Zeitpunkt am Konto speichern.
- **U3** „Spieler zeigen“: Annahme des AV-Vertrags (Zeitpunkt, Version) **serverseitig** erzwingen; Mod sendet Spieler nur bei aktivem Schalter; Live-Stand beim Ausschalten/Trennen löschen; **kein** Positionsverlauf, kein Log mit Positionen.
- **U4** Löschjobs: Signup-Tag 30 Tage, abgelaufene Sitzungen, Puffer nach Stufe, Herabstufung 30 Tage; nginx-Zugriffs- **und** Fehler-Log rotieren (Vorschlag 7 Tage); Ratenzähler nur im Speicher.
- **U5** Melde-Link auf jeder gehosteten Karte, Admin-Ansicht für Meldungen, Begründung/Widerspruch nach DSA Art. 16/17.
- **U6** Export (JSON) mit allen Konto- und Karten-Daten (Konto, Karten, Freigaben, Nutzungszahlen, Verbindungen, AV-Annahmen); Konto-Löschung einschließlich Puffer; Löschung in Backups nach definiertem Zyklus.
- **U7** Landing/Registrierung/FAQ: nicht „wir speichern nur E-Mail, Name, Passwort-Hash …“ (Befund B2), sondern „nur das Nötigste“ mit Link zur Datenschutzerklärung.
- **U8** AGB-Tabelle (Stufen, Preise) aus `tiers.json` erzeugen oder per Test gegen die Preisseite prüfen.
- **U9** Cookies: Namen, Flags, Laufzeiten festlegen (`__Secure-sm_session`, `sm_csrf`, Sprach-Cookie); kein weiteres Cookie ohne Eintrag in der Datenschutzerklärung; Cloudflare Web Analytics aus.
- **U10** Viewer: Ansichtsschlüssel bleibt im Fragment `#t=`; wenn er für Kachel-Anfragen gesendet wird, als Header, **nicht** als Query (sonst im Log).
- **U11** Admin-Liste „auffällige Muster“: nur Anzeige und manuelle Prüfung, keine automatische Sperre allein auf Grund eines Musters.
- **U12** Bezahlstart: Bestell-Button, Zusammenfassung, Zustimmungs-Kästchen, Kündigungsbutton (ohne Login erreichbar), Bestätigungs-E-Mails, Rechnung (siehe AGB Anlage 3).
- **U13** Welche Angaben des eingeladenen Kontos dem Kartenbesitzer beim Teilen angezeigt werden (E-Mail? Name?), und Speicherform des Karten-Schlüssels (Hash?) festlegen und im Text nachtragen.

## 9. Was NICHT getestet/geprüft wurde

- Kein Abgleich mit einem gebauten Dienst (noch nicht vorhanden); keine technische Prüfung von Cookies, Logs, Headern.
- Keine Rechtsprüfung durch Fachleute; Gesetzesverweise nicht gegen aktuellen Wortlaut verifiziert (kein Rechtsportal angebunden).
- Wortlaut von Widerrufsbelehrung und Muster-Widerrufsformular nicht gegen die amtliche Fassung abgeglichen.
- Keine Prüfung der aktuellen Verträge/DPAs von Cloudflare und Stripe, der DPF-Zertifizierung oder der Mojang-Richtlinien.

## 10. Platzhalter-Statistik (Runde 3)

Stand nach Runde 3 (12.11.2026): **182 Besitzer-Platzhalter** in den vier Textdateien –
`impressum.md` 18 · `datenschutz.md` 65 · `agb.md` 48 · `auftragsverarbeitung.md` 51.
(Die Beispiele in diesem README zählen nicht mit.) Vor Veröffentlichung muss jeder einzelne beantwortet oder gestrichen sein; Suche: `grep -n "⟦PLATZHALTER" docs/legal/*.md`.

---

ENTWURF – keine Rechtsberatung. Vor Veröffentlichung prüfen lassen.
