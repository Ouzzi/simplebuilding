# Auftragsverarbeitung – internes Arbeitsdokument (nicht veröffentlichen)

Stand: 19.11.2026 · ungeprüft – vor Zahlungsstart anwaltlich prüfen.

Dieses Dokument ist die interne Grundlage des Betreibers für das Verzeichnis der Dienstleister (Art. 28, 30, 44 ff. DSGVO), die
Empfängerliste der Datenschutzerklärung (Abschnitt 12) und die technischen und organisatorischen Maßnahmen (Art. 32 DSGVO). Der
Auftragsverarbeitungsvertrag für Kartenbesitzer ist **nicht mehr hier**, sondern als Anlage „Auftragsverarbeitung“ Teil der AGB
(`agb.md`) und wird mit ihnen veröffentlicht.

---

## Teil A – Eingesetzte Dienstleister

| Nr. | Dienstleister | Zweck | Daten | Ort | Drittland-Grundlage | Rolle / Vertrag |
|---|---|---|---|---|---|---|
| 1 | Cloudflare, Inc., San Francisco, USA | CDN, Reverse Proxy, Cloudflare Tunnel, DNS, Schutz vor Angriffen, Cache; optional Email Routing für Kontaktadressen | IP-Adressen, Anfrage-Metadaten, gesamter Datenverkehr (TLS endet bei Cloudflare), zwischengespeicherte Kacheln/Downloads, ggf. weitergeleitete E-Mails | USA, weltweites Netz | EU-US Data Privacy Framework (Durchführungsbeschluss (EU) 2023/1795); Standardvertragsklauseln (Beschluss (EU) 2021/914) im Cloudflare-DPA | Auftragsverarbeiter; Cloudflare Data Processing Addendum (Teil der Self-Serve-Bedingungen) |
| 2 | **Paddle.com Market Limited**, London, Vereinigtes Königreich (Paddle-Gruppe, u. a. USA) – **Standard seit 19.11.2026** (`billing.provider = paddle`) | Verkauf von Abo und Geschenk-Abo als Wiederverkäufer (Merchant of Record): Checkout (Paddle.js-Overlay), Zahlung, Rechnungen, USt-Berechnung und -Abführung, Kundenportal, Erstattungen/Rückbuchungen, Discounts (Gutscheine), Webhooks | Name, E-Mail, Land/PLZ bzw. Rechnungsadresse, ggf. USt-IdNr., Konto- und Bestellkennung (custom_data), Stufe/Laufzeit, Rabatt-Kennung, Abo-/Zahlungsstatus, Rechnungen; Zahlungsmitteldaten nur bei Paddle | UK, USA | Angemessenheitsbeschluss UK; DPF/SCC für Weitergaben der Paddle-Gruppe | **eigener Verantwortlicher** (Verkäufer); kein AV-Vertrag nötig, Paddle-Datenschutzerklärung verlinkt; Paddle Seller Terms/Buyer Terms |
| 2b | Stripe Payments Europe, Limited, Dublin, Irland (mit Stripe, Inc., USA) – nur Variante `billing.provider = stripe`, derzeit nicht aktiv | Zahlungsabwicklung Abo und Geschenk-Abo, Checkout, Kundenportal, Abo-Zeitplan (Jahresabo), Rechnungen, Webhooks, Gutscheine, optional Stripe Tax | Name, E-Mail, Konto-Kennung, Rechnungsadresse, ggf. USt-IdNr., Stufe/Laufzeit, Gutscheincode, Abo-/Zahlungsstatus, Rechnungen; Zahlungsmitteldaten nur bei Stripe | Irland, USA | Data Privacy Framework; Standardvertragsklauseln im Stripe-DPA | eigener Verantwortlicher (Zahlungsabwicklung, Betrugsprävention, gesetzliche Pflichten) und Auftragsverarbeiter (Abo-Verwaltung, Rechnungen, Portal); Stripe Data Processing Agreement (Teil der Stripe Services Agreement) |
| 3 | SMTP-Dienstleister (Versand) | Konto- und Vertrags-E-Mails, Kontaktformular an den Betreiber | E-Mail-Adresse, Anzeigename, Inhalt, Einmal-Links, Zeitpunkt | je nach Anbieter | bei Drittland: DPF oder SCC | Auftragsverarbeiter; AV-Vertrag des Anbieters |
| 4 | E-Mail-Postfach-Anbieter des Betreibers (Empfang) | Empfang von Anfragen, Meldungen, Kündigungen, Widerrufen | Absender, Inhalt, Zeitpunkt | je nach Anbieter | bei Drittland: DPF oder SCC | Auftragsverarbeiter; AV-Vertrag des Anbieters |
| 5 | Eigene Hardware des Betreibers (Proxmox-Container, zu Hause) | Dienst, Datenbank (SQLite), Kachelspeicher, nginx | alle Daten der Dienste | Deutschland | — | kein Auftragsverarbeiter (Betreiber selbst) |
| 6 | Datensicherung auf eigenen Datenträgern des Betreibers | Wiederherstellung | Datenbank, Kacheln | Deutschland | — | kein Auftragsverarbeiter; verschlüsselt, Aufbewahrung höchstens 30 Tage |
| 7 | Domain-Registrar | Registrierung der Domains | nur Inhaberdaten des Betreibers | Deutschland | — | kein Zugriff auf Nutzerdaten, kein AV nötig |
| 8 | Steuerberatung (falls beauftragt) | Buchhaltung, Steuern | Rechnungs- und Buchungsdaten | Deutschland | — | eigener Verantwortlicher mit Schweigepflicht |

Weitere Dienstleister (Monitoring, Fehlerberichte, Analyse, Ticket-System, Objektspeicher wie Cloudflare R2) sind **nicht** im Einsatz.
Jede Ergänzung muss vorher hier, in der Datenschutzerklärung (Abschnitte 4, 9, 12) und – falls Kartendaten betroffen sind – in der
AGB-Anlage „Auftragsverarbeitung“ Nr. 6 eingetragen werden (dort 30 Tage Vorankündigung an Kartenbesitzer).

## Teil B – Technische und organisatorische Maßnahmen (Art. 32 DSGVO)

| Bereich | Maßnahme |
|---|---|
| Transportverschlüsselung | TLS zwischen Browser und Cloudflare; Cloudflare Tunnel (verschlüsselt) zum Server, kein offener Port |
| Zugangsdaten | Passwörter als PBKDF2-SHA256-Hash (600 000 Runden, Salz); Sitzungs- und Einmal-Token nur gehasht |
| Sitzungen | Cookie HttpOnly, Secure, SameSite=Lax; 30 Tage; Löschung bei Abmeldung und Passwortwechsel |
| Missbrauchsschutz | Ratenbegrenzungen nur im Arbeitsspeicher (≤ 1 h), nginx `limit_req`, CSRF-Schutz, Origin-Prüfung, Honeypot und Mindestzeit im Kontaktformular |
| Schnittstelle der Mod | Bearer-Schlüssel je Karte, Größen- und Formatprüfung, keine Cookies |
| Webhooks | Signaturprüfung HMAC-SHA256, Toleranz 300 s, Idempotenz über Ereignis-Kennung |
| Geheimnisse | nur in Server-Konfiguration außerhalb des Repos (Dateirechte 600) |
| Zugriff auf den Server | nur der Betreiber; SSH nur mit Schlüssel, kein Passwort-Login |
| Physischer Schutz | Hardware in der Wohnung des Betreibers, kein Zugang für Dritte; Datenträger verschlüsselt |
| Datensicherung | regelmäßig, verschlüsselt, eigene Datenträger, Aufbewahrung ≤ 30 Tage; Wiederherstellung halbjährlich testen |
| Updates | Sicherheitsupdates von System und Abhängigkeiten zeitnah, spätestens wöchentlich prüfen |
| Datenpannen | Bewertung sofort; Meldung an die Aufsichtsbehörde binnen 72 Stunden (Art. 33), Benachrichtigung Betroffener bei hohem Risiko (Art. 34); Meldung an Kartenbesitzer binnen 48 Stunden (AGB-Anlage Nr. 7); Vorfall dokumentieren |
| Löschkonzept | Logs 7 Tage (Zugriff und Fehler), Sitzungen 30 Tage, Signup-Tag 30 Tage, Audit und Aufrufzahlen 400 Tage, Puffer je Stufe, Herabstufung 30 Tage, Live-Daten nur RAM, Mitspieler-Plätze 30 Tage nach Ende, Geschenk-Namen 30 Tage nach Ablauf, nicht freigegebene Kommentare 30 Tage, Bestellungen/Kündigungen Vertragsende + 3 Jahre, Rechnungen 8/10 Jahre |

## Teil C – Checkliste für den Besitzer (vor Zahlungsstart abhaken)

- [ ] Cloudflare-DPA im Dashboard bestätigt/heruntergeladen, Datum notiert; DPF-Zertifizierung von Cloudflare geprüft
- [ ] Paddle-Konto (Standard): Verkäuferprüfung bestanden (Checkliste in docs/ai/PLAN-B-N108-PADDLE-2026-11-19.md Abschnitt 6), Seller Terms akzeptiert, Datum notiert; Checkout-Domain freigegeben; Rechtstexte mit `fill_legal.py --provider paddle` erzeugt
- [ ] Nur falls Stripe-Variante aktiv: Stripe-Konto: Vertragsgesellschaft (Stripe Payments Europe) und DPA bestätigt, Datum notiert
- [ ] SMTP-Anbieter gewählt, AV-Vertrag abgeschlossen, Sitz/Drittland notiert, SPF/DKIM/DMARC gesetzt
- [ ] Postfach-Anbieter: AV-Vertrag bzw. Auftragsverarbeitungsbedingungen abgeschlossen
- [ ] Falls Cloudflare Email Routing aktiviert wird: in Teil A Nr. 1 vermerkt (Datenschutz 9 deckt es ab)
- [ ] Datensicherung wie in Teil B eingerichtet (verschlüsselt, ≤ 30 Tage) – sonst Datenschutz 14/16 anpassen
- [ ] Festplattenverschlüsselung und SSH nur mit Schlüssel bestätigt
- [ ] Verzeichnis von Verarbeitungstätigkeiten (Art. 30) aus Datenschutz Abschnitt 3 und Teil A erstellt
- [ ] Datenpannen-Ablauf (72 Stunden) als Notiz griffbereit
- [ ] Rollenabgrenzung Kartenbesitzer (Auftragsverarbeitung statt gemeinsamer Verantwortlichkeit) anwaltlich bestätigt

## Teil D – Muster-Hinweis für Kartenbesitzer an ihre Spieler

Kartenbesitzer, die „Spieler zeigen“ einschalten, müssen ihre Spieler informieren (Art. 13 DSGVO). Wir bieten diesen Baustein zur
Erleichterung an (zum Beispiel für Hilfeseite oder Wiki); er ersetzt keine Rechtsberatung. Die Stellen in ‹…› füllt der Kartenbesitzer aus.

> Auf unserer Server-Karte unter ‹Adresse der Karte› werden – solange Sie online und im Spiel sichtbar sind – Ihr Minecraft-Name, die
> Dimension und Ihre x/z-Position angezeigt. Gespeichert wird nur der aktuelle Stand im Arbeitsspeicher, kein Verlauf. Die Karte ist
> ‹privat / per Link / öffentlich› sichtbar. Verantwortlich: ‹Name und Kontakt des Server-Betreibers›. Rechtsgrundlage: ‹berechtigtes
> Interesse, Art. 6 Abs. 1 lit. f DSGVO, oder Einwilligung, Art. 6 Abs. 1 lit. a DSGVO›. Die Karte wird bei Simple Livemap
> (livemap.simplemods.de) als Auftragsverarbeiter gehostet; dabei wird Cloudflare (USA) eingesetzt. Sie haben Rechte auf Auskunft,
> Löschung, Widerspruch und weitere Rechte; wenden Sie sich an ‹Kontakt des Server-Betreibers›. Mit dem Befehl der Mod können Sie sich
> auf der Karte ausblenden.
