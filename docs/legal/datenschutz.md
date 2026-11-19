<!-- intern: Zahlungsanbieter-Varianten Paddle (Standard) / Stripe; Schalter fill_legal.py --provider. -->
<!-- intern: ungeprüft – vor Zahlungsstart anwaltlich prüfen. Diese Zeile beim Veröffentlichen entfernen. -->
# Datenschutzerklärung

Stand: 19.11.2026

## 1. Verantwortlicher und Kontakt

Verantwortlich für die Verarbeitung personenbezogener Daten im Sinne der Datenschutz-Grundverordnung (DSGVO) auf allen Seiten von
Simple Mods ist:

{{betreiber.name}}

{{betreiber.anschrift}}

Deutschland

E-Mail für Datenschutzanfragen: {{kontakt.datenschutz}}

{{#wenn betreiber.telefon}}Telefon: {{betreiber.telefon}}{{/wenn}}

Weitere Angaben stehen im Impressum. Einen Datenschutzbeauftragten haben wir nicht benannt, weil die gesetzlichen Voraussetzungen
dafür (Art. 37 DSGVO, § 38 BDSG) nicht vorliegen.

## 2. Geltungsbereich

Diese Erklärung gilt für

- die Website `simplemods.de` (Startseite, öffentliche Downloads),
- das Wiki `wiki.simplemods.de` (einschließlich Anmeldung, Kommentaren, Designs, Einstellungen und Entwicklerseiten),
- das Simple-Mods-Konto auf `account.simplemods.de` (Registrierung, Anmeldung, Abo „Simple Mods Pro“, Mitspieler-Plätze,
  Geschenk-Abos, Gutscheine, Frühzugang, Kontaktformular, Kündigungsseite),
- den Kartendienst „Simple Livemap“ auf `livemap.simplemods.de` einschließlich der Schnittstelle, über die die Mod „Simple Livemap“
  Kartendaten hochlädt,
- E-Mails, die wir im Zusammenhang mit diesen Diensten senden und empfangen.

Wenn Sie unsere Mods ohne unsere Online-Dienste nutzen (zum Beispiel Simple Livemap im Self-Host-Betrieb), übertragen die Mods dadurch
keine Daten an uns. Simple Livemap verbindet sich mit unserem Dienst erst, nachdem ein Spielleiter `/simplelivemap connect <Code>`
ausgeführt hat.

## 3. Überblick

| Daten | Zweck | Rechtsgrundlage (DSGVO) | Speicherdauer |
|---|---|---|---|
| Server-Logs: IP-Adresse, Zeit, Methode, Pfad ohne Query-Teil, Status, Bytes | Betrieb, Sicherheit, Fehlersuche | Art. 6 Abs. 1 lit. f | 7 Tage |
| Ratenbegrenzung: IP-Adresse, E-Mail-Adresse, Zähler | Schutz vor Missbrauch | Art. 6 Abs. 1 lit. f | nur Arbeitsspeicher, höchstens 1 Stunde |
| Konto: E-Mail, Anzeigename, Passwort-Hash, Rolle, Stufe, Sprache, Zeitpunkte | Konto, Anmeldung, Verträge | Art. 6 Abs. 1 lit. b | bis zur Löschung des Kontos |
| Sitzung: Token-Hash, Ablaufzeit | angemeldet bleiben | Art. 6 Abs. 1 lit. b; § 25 Abs. 2 Nr. 2 TDDDG | höchstens 30 Tage |
| Anmelde-Kennzeichen (gekürzter HMAC der IP bei Registrierung) | Erkennen von Mehrfach-Konten | Art. 6 Abs. 1 lit. f | 30 Tage |
| Protokoll wichtiger Konto-Ereignisse (Audit) | Nachweis, Sicherheit, Missbrauchsabwehr | Art. 6 Abs. 1 lit. b, c, f | 400 Tage |
| Wiki-Kommentare | Kommentarfunktion, Moderation | Art. 6 Abs. 1 lit. b, f | bis zur Löschung |
| Karten, Kacheln, Freigaben | Simple Livemap | Art. 6 Abs. 1 lit. b | bis zur Löschung, Puffer je Stufe |
| Live-Daten (Spielername, Dimension, Position) | Anzeige auf der Karte | Auftragsverarbeitung (Art. 28) | nur letzter Stand im Arbeitsspeicher |
| Aufrufzahlen je Karte und Tag (ohne IP) | Stufen-Grenzen, Statistik für Kartenbesitzer | Art. 6 Abs. 1 lit. b | 400 Tage |
| Bestellungen, Zustimmungen, Abos, Freischaltungen, Gutscheincode | Vertrag, Nachweis | Art. 6 Abs. 1 lit. b, c, f | Vertragsende + 3 Jahre; steuerlich relevante Teile 8 bzw. 10 Jahre |
| Mitspieler-Plätze | Vergabe, Missbrauchsschutz | Art. 6 Abs. 1 lit. b, f | bis 30 Tage nach Ende des Platzes |
| Geschenk-Abos, Minecraft-Namen | Vertrag, Vorzüge im Spiel | Art. 6 Abs. 1 lit. b, f | Namen bis 30 Tage nach Ablauf; Bestellung wie oben |
| Kündigungen | Bearbeitung, Nachweis (§ 312k BGB) | Art. 6 Abs. 1 lit. b, c, f | 3 Jahre nach Ende des Jahres der Vertragsbeendigung |
| Zahlungsdaten (beim Zahlungsanbieter, 8.2) | Zahlungsabwicklung | Art. 6 Abs. 1 lit. b, c | nach den gesetzlichen Fristen |
| Kontaktformular und E-Mails | Beantwortung | Art. 6 Abs. 1 lit. b, f | bis Erledigung, höchstens 3 Jahre; Geschäftsbriefe 6 Jahre |
| Meldungen rechtswidriger Inhalte | Bearbeitung (DSA) | Art. 6 Abs. 1 lit. c, f | 3 Jahre nach Abschluss |
| Rechnungen und Buchungsbelege | gesetzliche Aufbewahrung | Art. 6 Abs. 1 lit. c | 8 bzw. 10 Jahre |

Die Einzelheiten stehen in den folgenden Abschnitten.

## 4. Bereitstellung der Seiten, Server-Logs, Cloudflare

### 4.1 Hosting

Unsere Dienste laufen auf eigener Hardware des Betreibers in Deutschland. Einen externen Hosting-Anbieter setzen wir nicht ein. Der
Internetanbieter des Betreibers transportiert die Daten nur.

### 4.2 Server-Logs

Bei jedem Aufruf unserer Seiten speichert unser Webserver (nginx) in Log-Dateien: die IP-Adresse des aufrufenden Geräts (so, wie
Cloudflare sie uns übermittelt), Datum und Uhrzeit, die Anfragemethode, den aufgerufenen Pfad **ohne** den Query-Teil (also ohne die
Angaben nach einem „?“), den Statuscode und die übertragene Datenmenge. Referrer und Browserkennung (User-Agent) speichern wir nicht.
Im Fehler-Log können Meldungen mit IP-Adresse stehen, zum Beispiel über Anfragen, die wegen einer Ratenbegrenzung abgelehnt wurden.

- Zweck: Betrieb, Fehlersuche, Abwehr von Angriffen und Missbrauch.
- Rechtsgrundlage: berechtigtes Interesse, Art. 6 Abs. 1 lit. f DSGVO.
- Speicherdauer: 7 Tage, danach werden Zugriffs- und Fehler-Logs automatisch gelöscht. Benötigen wir einen Eintrag zur Aufklärung eines
  konkreten Angriffs, bewahren wir ihn bis zum Abschluss der Aufklärung auf.

### 4.3 Cloudflare

Unsere Seiten werden über Cloudflare ausgeliefert. Cloudflare ist als Reverse Proxy, Content Delivery Network (CDN), DNS-Anbieter und
über Cloudflare Tunnel vorgeschaltet und nimmt alle Anfragen zuerst entgegen. Weil die TLS-Verschlüsselung bei Cloudflare endet, verarbeitet
Cloudflare technisch bedingt den gesamten Datenverkehr zwischen Ihrem Gerät und unserem Server, einschließlich Anmeldedaten,
Formulareingaben und Karteninhalten, außerdem IP-Adresse, Zeitpunkt, angefragte Adresse, Browserangaben und Sicherheitsmerkmale.
Cloudflare speichert öffentliche Inhalte wie Kartenkacheln und Downloads zwischen (Cache). Cloudflare kann sicherheitsbezogene Cookies
setzen (zum Beispiel `__cf_bm` zur Erkennung von Bots, Laufzeit 30 Minuten, oder `cf_clearance` nach einer Sicherheitsprüfung). Cloudflare
Web Analytics nutzen wir nicht.

- Anbieter: Cloudflare, Inc., 101 Townsend St., San Francisco, CA 94107, USA.
- Zweck: schnelle und sichere Auslieferung, Schutz vor Angriffen, Erreichbarkeit des Servers ohne offenen Port.
- Rechtsgrundlage: berechtigtes Interesse, Art. 6 Abs. 1 lit. f DSGVO. Cloudflare ist unser Auftragsverarbeiter (Art. 28 DSGVO).
- Drittland: Cloudflare kann Daten in den USA und anderen Ländern verarbeiten. Grundlage ist der Angemessenheitsbeschluss der
  EU-Kommission zum EU-US Data Privacy Framework (Durchführungsbeschluss (EU) 2023/1795), da Cloudflare danach zertifiziert ist,
  ergänzend die Standardvertragsklauseln der EU-Kommission im Auftragsverarbeitungsvertrag von Cloudflare.
- Weitere Informationen: https://www.cloudflare.com/privacypolicy/

### 4.4 Schriftarten und externe Inhalte

Schriftarten, Skripte und Bilder laden unsere Seiten von unseren eigenen Servern. Verbindungen zu Schriftarten-Diensten Dritter (zum
Beispiel Google Fonts), Werbe- oder Analyse-Anbietern bestehen nicht. Links zu anderen Websites (zum Beispiel Modrinth, CurseForge)
führen Sie erst beim Anklicken zu diesen Anbietern; dort gelten deren Datenschutzhinweise.

### 4.5 Website und öffentliche Downloads

Die Startseite `simplemods.de` ist eine statische Seite. Außer den Server-Logs (4.2) und der Auslieferung über Cloudflare (4.3) verarbeiten
wir beim Besuch und bei öffentlichen Downloads keine personenbezogenen Daten. Ihre Auswahl von Design oder Sprache speichert die Seite
nur im lokalen Speicher Ihres Browsers (Abschnitt 13).

## 5. Simple-Mods-Konto

### 5.1 Registrierung und Konto

Für ein Konto verarbeiten wir: E-Mail-Adresse, Anzeigename, Passwort (nur als Hash nach PBKDF2-SHA256 mit Salz; wir kennen Ihr
Passwort nicht), Rolle (Nutzer oder Entwickler), Stufe, Sprache, Zeitpunkt der Erstellung und der E-Mail-Bestätigung, Einstellungen
(zum Beispiel Wiki-Einstellungen) sowie Zähler und Zeitstempel zur Anwendung der Stufen-Grenzen (zum Beispiel Anzahl bisheriger
Karten-Verbindungen).

- Zweck: Bereitstellung des Kontos und der damit verbundenen Dienste.
- Rechtsgrundlage: Vertragserfüllung, Art. 6 Abs. 1 lit. b DSGVO.
- Pflicht: Ohne E-Mail-Adresse, Anzeigename und Passwort können wir kein Konto anlegen.
- Speicherdauer: bis zur Löschung des Kontos (Abschnitt 16). Konten, mit denen sich drei Jahre lang niemand angemeldet hat, können wir nach
  Ankündigung per E-Mail mit 30 Tagen Frist löschen.

### 5.2 E-Mail-Bestätigung und Passwort zurücksetzen

Zur Bestätigung der E-Mail-Adresse und zum Zurücksetzen des Passworts senden wir Ihnen einen Einmal-Link. Er gilt 24 Stunden
(Bestätigung) bzw. 1 Stunde (Zurücksetzen); auf dem Server speichern wir nur einen Hash des Tokens. Rechtsgrundlage: Art. 6 Abs. 1
lit. b und f DSGVO.

### 5.3 Anmeldung und Sitzungen

Nach der Anmeldung legen wir eine Sitzung an: Wir erzeugen ein zufälliges Token, speichern davon nur einen Hash und die Ablaufzeit und
setzen ein Sitzungs-Cookie (Abschnitt 13). Die Sitzung enthält keine IP-Adresse. Sie endet nach 30 Tagen, bei Abmeldung oder beim
Ändern des Passworts. Das Sitzungs-Cookie gilt für alle Seiten unter `simplemods.de`, damit Sie im Wiki, im Konto und in Simple
Livemap angemeldet sind. Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO; für das Cookie § 25 Abs. 2 Nr. 2 TDDDG.

### 5.4 Schutz vor Missbrauch

- **Anmelde-Kennzeichen.** Bei der Registrierung speichern wir aus Ihrer IP-Adresse einen gekürzten HMAC-Wert (mit einem nur uns
  bekannten Schlüssel gebildet), nicht die IP-Adresse selbst. Damit erkennen wir Häufungen neuer Konten vom selben Anschluss. Wir
  löschen den Wert nach 30 Tagen.
- **Ratenbegrenzungen.** Bei Anmeldung, Registrierung, „Passwort vergessen“, Code-Einlösung, Uploads, Kontaktformular,
  Kündigungsseite und der öffentlichen Abfrage von Minecraft-Namen zählen wir Versuche je IP-Adresse bzw. je E-Mail-Adresse. Diese
  Zähler liegen nur im Arbeitsspeicher und werden spätestens nach einer Stunde verworfen.
- **Audit-Protokoll.** Wichtige Ereignisse eines Kontos (zum Beispiel Änderungen der Stufe, Freischaltungen, vergebene Plätze,
  Kündigungen, Sperren) protokollieren wir mit Zeitpunkt und Konto, ohne IP-Adresse. Wir löschen die Einträge nach 400 Tagen.
- **Manuelle Prüfung.** Auffällige Muster werden in einer Verwaltungsansicht angezeigt und von uns von Hand geprüft.
- Rechtsgrundlage: berechtigtes Interesse an Sicherheit und Missbrauchsabwehr, Art. 6 Abs. 1 lit. f DSGVO, für das Audit-Protokoll
  außerdem lit. b und c.

## 6. Wiki

- **Lesen** ist ohne Konto möglich; es fallen nur Server-Logs und die Auslieferung über Cloudflare an.
- **Anmeldung** erfolgt mit dem Simple-Mods-Konto (Abschnitt 5). Seiten für Entwickler sind nur für Konten mit Entwickler-Rolle
  zugänglich; dabei verarbeiten wir keine zusätzlichen Daten.
- **Kommentare.** Wir speichern den Kommentartext, das Konto, den angezeigten Anzeigenamen, die Wiki-Seite, den Zeitpunkt und den
  Freigabestatus. Kommentare sind erst sichtbar, nachdem wir sie geprüft und freigegeben haben; danach sieht jeder den Kommentar mit
  Ihrem Anzeigenamen und dem Zeitpunkt. Nicht freigegebene Kommentare löschen wir spätestens 30 Tage nach der Entscheidung. Freigegebene
  Kommentare speichern wir, bis Sie oder wir sie löschen oder Ihr Konto gelöscht wird. Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO
  (Kommentarfunktion) und lit. f (Moderation, Schutz vor rechtswidrigen Inhalten).
- **Designs und Einstellungen.** Das gewählte Design speichert das Wiki im lokalen Speicher Ihres Browsers. Weitere Wiki-Einstellungen,
  die zu Ihrem Abo gehören, speichern wir in Ihrem Konto, damit sie auf allen Geräten gelten. Rechtsgrundlage: Art. 6 Abs. 1 lit. b
  DSGVO; § 25 Abs. 2 Nr. 2 TDDDG.

## 7. Simple Livemap

### 7.1 Karte verbinden

Zum Verbinden erzeugen Sie im Dashboard einen Einmal-Code (15 Minuten gültig). Ein Spielleiter führt auf dem Minecraft-Server
`/simplelivemap connect <Code>` aus. Die Mod übermittelt dabei den Code, eine zufällige Welt-ID (keinen Seed), die Mod- und die
Minecraft-Version. Wir geben einen Karten-Schlüssel zurück, mit dem die Mod danach Daten hochlädt; er liegt auf dem Server in der Datei
`hosted.json` der Welt. Die IP-Adresse, von der die Mod Anfragen sendet, erscheint in den Server-Logs (4.2).

### 7.2 Kartenkacheln

Die Mod lädt Bilder (Kacheln) des erkundeten Geländes hoch. Sie enthalten keinen Seed und keine Spielerfiguren, können aber Bauwerke,
Schilder und Beschriftungen zeigen, die Namen oder andere Angaben enthalten. Wir speichern Kacheln je Karte und liefern sie über
Cloudflare aus. Kacheln aus Dimensionen, die die Stufe des Kartenbesitzers nicht umfasst, speichern wir beim Upload nicht bzw. liefern sie
nicht aus. Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO (Vertrag mit dem Kartenbesitzer); für Angaben Dritter in Kacheln gilt Abschnitt 7.6.

### 7.3 Sichtbarkeit, Teilen, Aufrufzahlen

Jede Karte ist privat (nur Besitzer und freigegebene Konten), per Link (wer den geheimen Link kennt) oder öffentlich. Beim Teilen mit
anderen Konten speichern wir die Zuordnung Karte–Konto. Wir zählen die Aufrufe der Karten-Ansicht je Karte und Tag (nur die Anzahl,
ohne IP-Adresse oder Gerätekennung); der Kartenbesitzer sieht diese Zahlen. Aufrufzahlen löschen wir nach 400 Tagen. Wer eine Karte nur
ansieht, braucht kein Konto; seine Anzeigeeinstellungen speichert der Browser lokal (Abschnitt 13). Rechtsgrundlage: Art. 6 Abs. 1
lit. b DSGVO, für die Aufrufzahlen auch lit. f.

### 7.4 Trennen, Puffer, Löschung

Getrennte Karten bleiben je nach Stufe 30 Tage (Free), 90 Tage (Basic), 180 Tage (Max) oder ein Jahr (Ultra) im Puffer; überzählige
Karten nach einer Herabstufung 30 Tage. Danach löschen wir Karte und Kacheln. Sie können Karten jederzeit selbst löschen.

### 7.5 Spielernamen und Positionen (Live-Daten)

Schaltet der Kartenbesitzer „Spieler zeigen“ ein (Standard: aus), übermittelt die Mod regelmäßig (je nach Stufe alle 1 bis 10
Sekunden) für die Spielerinnen und Spieler, die gerade online und im Spiel sichtbar sind, den **Minecraft-Namen, die Dimension und die
x/z-Position**. Zuschauer und Spieler, die sich mit dem Befehl der Mod ausgeblendet haben, werden nicht übermittelt. Wir halten **nur den
jeweils letzten Stand im Arbeitsspeicher**; wir schreiben Positionen weder in die Datenbank noch in Logs, und es gibt keinen Verlauf. Der
Stand wird verworfen, wenn „Spieler zeigen“ ausgeschaltet, die Karte getrennt oder der Dienst neu gestartet wird. Je nach Sichtbarkeit der
Karte sehen die Angaben der Kartenbesitzer, freigegebene Konten, Personen mit dem Link oder – bei öffentlichen Karten – alle.

### 7.6 Rollen und Hinweise für Spielerinnen und Spieler (Art. 14 DSGVO)

Für Live-Daten und personenbezogene Angaben in Kacheln ist der **Kartenbesitzer** (Betreiber des Minecraft-Servers) Verantwortlicher; er
entscheidet, ob und für wen Spieler angezeigt werden. Wir verarbeiten diese Daten in seinem Auftrag (Art. 28 DSGVO; Anlage
„Auftragsverarbeitung“ unserer AGB). Wenn Ihr Name auf einer Karte erscheint, haben wir die Daten nicht von Ihnen, sondern von der Mod
auf dem Server des Kartenbesitzers erhalten (Kategorien: Name, Dimension, Position, Zeitpunkt des letzten Standes). Wenden Sie sich für
Auskunft, Löschung oder Widerspruch zuerst an den Betreiber des Servers; auf dem Server können Sie sich mit dem Befehl der Mod
ausblenden. Anfragen, die uns erreichen, leiten wir an den Kartenbesitzer weiter.

## 8. Abo „Simple Mods Pro“

### 8.1 Bestellung, Abo, Freischaltungen

Bei einer Bestellung speichern wir in unserer Datenbank:

- **Bestellung:** Konto, Stufe, Laufzeit, Zeitpunkt, Ihre Zustimmungen (AGB und Widerrufsbelehrung; Vereinbarung zu Vorabversionen;
  freiwillig: Beginn vor Ablauf der Widerrufsfrist) jeweils mit Zeitpunkt und Version des Textes, ein eingelöster Gutscheincode und die
  Kennung der Bezahlsitzung bzw. Transaktion beim Zahlungsanbieter (8.2).
- **Abo:** Kunden- und Abo-Kennung beim Zahlungsanbieter, Stufe, Laufzeit, Status (zum Beispiel aktiv, Zahlung offen, gekündigt), Ende des bezahlten
  Zeitraums, vorgemerkte Kündigung oder vorgemerkter Stufenwechsel, beim Jahresabo die Kennung des Zahlungsplans.
- **Freischaltungen:** Stufe und Vorzüge Ihres Kontos, Beginn und Ende sowie die Quelle (Abo, Mitspieler-Platz oder manuell).
- **Ereignisse des Zahlungsanbieters:** nur Kennung und Typ jeder Nachricht, damit wir sie nicht doppelt verarbeiten.

Zahlungsmitteldaten (zum Beispiel Kartennummer, IBAN) speichern wir nicht.

- Zweck: Vertragsschluss und -durchführung, Freischaltung und Herabstufung, Nachweis der Zustimmungen (§§ 312j, 356 BGB).
- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO; für den Nachweis lit. c und f.
- Speicherdauer: bis zum Ende des Vertrags und danach drei Jahre ab Ende des Jahres der Vertragsbeendigung (regelmäßige
  Verjährungsfrist); Angaben, die Teil von Rechnungen oder Buchungsbelegen sind, nach Abschnitt 8.3.

{{#wenn zahlung.paddle}}### 8.2 Zahlungsabwicklung (Paddle als Verkäufer)

Die kostenpflichtigen Produkte verkauft Paddle als Wiederverkäufer (Merchant of Record). Zum Bezahlen öffnet sich auf unserer Seite
die Bezahlseite von Paddle (Paddle.js, eingebettet von `cdn.paddle.com` bzw. `buy.paddle.com`); Zahlungsart, Rechnungen und Kündigung
können Sie im Kundenportal von Paddle verwalten.

- **Daten:** Name, E-Mail-Adresse, Land und Postleitzahl bzw. Rechnungsadresse, ggf. Umsatzsteuer-Identifikationsnummer, Stufe,
  Laufzeit, Betrag, Abo- und Zahlungsstatus, Rechnungsdaten, Zahlungsmitteldaten, Geräte- und Verbindungsdaten zur Betrugsprävention.
  Wir übermitteln an Paddle eine Kennung Ihres Kontos und der Bestellung, Stufe und Laufzeit sowie einen eingegebenen Gutscheincode
  (als Rabatt-Kennung); Ihre E-Mail-Adresse geben Sie im Bezahlschritt selbst ein. Von Paddle erhalten wir Kunden-, Abo- und
  Transaktionskennungen, Abo- und Zahlungsstatus, Beträge und Rechnungsnummern sowie Erstattungen und Rückbuchungen, aber keine
  Zahlungsmitteldaten.
- **Anbieter und Rolle:** Paddle.com Market Limited, Judd House, 18–29 Mora Street, London EC1V 8BT, Vereinigtes Königreich. Paddle ist
  für den Verkauf, die Zahlungsabwicklung, die Rechnungsstellung, die Steuerberechnung, die Betrugsprävention und seine gesetzlichen
  Pflichten **eigener Verantwortlicher** und damit Empfänger Ihrer Daten. Für die Verarbeitung durch Paddle gilt die
  Datenschutzerklärung von Paddle: https://www.paddle.com/legal/privacy
- **Rechtsgrundlage:** Art. 6 Abs. 1 lit. b DSGVO (Vertrag), lit. c (gesetzliche Pflichten), lit. f (Abgleich von Zahlung und
  Freischaltung).
- **Drittland:** Für das Vereinigte Königreich besteht ein Angemessenheitsbeschluss der EU-Kommission. Paddle kann Daten an
  Unternehmen der Paddle-Gruppe und Dienstleister außerhalb der EU, insbesondere in den USA, übermitteln; Grundlage sind dort das
  EU-US Data Privacy Framework bzw. Standardvertragsklauseln.
- Nachrichten von Paddle an unseren Server (Webhooks) prüfen wir anhand ihrer Signatur.{{#sonst}}### 8.2 Zahlungsabwicklung (Stripe)

Zur Eingabe der Zahlungsdaten leiten wir Sie auf die Bezahlseite von Stripe („Stripe Checkout“) weiter; Zahlungsart, Rechnungen und
Kündigung können Sie im Kundenportal von Stripe verwalten.

- **Daten:** Name, E-Mail-Adresse, Rechnungsadresse (soweit abgefragt), Stufe, Laufzeit, Betrag, Abo- und Zahlungsstatus,
  Rechnungsdaten, Zahlungsmitteldaten, Gerätedaten zur Betrugsprävention. Wir übermitteln an Stripe Ihre E-Mail-Adresse, eine Kennung
  Ihres Kontos, Stufe und Laufzeit sowie einen eingegebenen Gutscheincode. Von Stripe erhalten wir Abo- und Zahlungsstatus sowie
  Rechnungsangaben, aber keine vollständigen Zahlungsmitteldaten.
- **Steuerberechnung:** Setzen wir Stripe Tax ein, verarbeitet Stripe zusätzlich das Land Ihrer Rechnungsadresse und ggf. Ihre
  Umsatzsteuer-Identifikationsnummer, um die Umsatzsteuer zu berechnen.
- **Anbieter und Rollen:** Stripe Payments Europe, Limited, 1 Grand Canal Street Lower, Grand Canal Dock, Dublin, D02 H210, Irland.
  Stripe verarbeitet Zahlungsdaten für die Zahlungsabwicklung, Betrugsprävention und eigene gesetzliche Pflichten als eigener
  Verantwortlicher; für Abo-Verwaltung, Rechnungen und Kundenportal handelt Stripe als unser Auftragsverarbeiter.
- **Rechtsgrundlage:** Art. 6 Abs. 1 lit. b DSGVO (Vertrag), lit. c (gesetzliche Pflichten).
- **Drittland:** Stripe kann Daten an Stripe, Inc. in den USA übermitteln. Grundlage ist das EU-US Data Privacy Framework, ergänzend
  Standardvertragsklauseln.
- **Weitere Informationen:** https://stripe.com/de/privacy
- Nachrichten von Stripe an unseren Server (Webhooks) prüfen wir anhand ihrer Signatur.{{/wenn}}

### 8.3 Rechnungen und Aufbewahrung

Rechnungen und Buchungsbelege bewahren wir nach § 147 AO und § 257 HGB acht Jahre auf, Bücher und Jahresabschlüsse zehn Jahre,
empfangene und versandte Geschäftsbriefe sechs Jahre, jeweils ab Ende des Kalenderjahres. Während dieser Zeit sind die Daten für andere
Zwecke gesperrt. Rechtsgrundlage: Art. 6 Abs. 1 lit. c DSGVO.

### 8.4 Mitspieler-Plätze

Vergeben Sie einen Mitspieler-Platz, geben Sie die E-Mail-Adresse eines anderen Simple-Mods-Kontos ein. Wir prüfen, ob dazu ein
bestätigtes Konto ohne eigenes Abo und ohne Platz besteht, und speichern die Zuordnung: Konto des Abo-Inhabers, Konto des Mitspielers,
Art des Platzes, Zeitpunkt der Vergabe und des Endes. Der Mitspieler sieht den Anzeigenamen des Abo-Inhabers, der Abo-Inhaber die
E-Mail-Adresse, die er eingegeben hat. Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO, für die Sperrfrist gegen Weitergabe lit. f. Wir
löschen die Zuordnung 30 Tage nach dem Ende des Platzes (Ende der Sperrfrist für die Neuvergabe); das Ereignis bleibt im
Audit-Protokoll (5.4).

### 8.5 Geschenk-Abos und öffentliche Namensabfrage

Beim Geschenk-Abo speichern wir zusätzlich zu den Bestelldaten (8.1) die Stufe, die Laufzeit, den bzw. die angegebenen
**Minecraft-Namen**, den Zeitpunkt jeder Namensänderung und das Ende der Laufzeit. Die Zahlung erfolgt einmalig über den
Zahlungsanbieter (8.2). Bleibt der Name bei der Bestellung offen, erzeugen wir einen zufälligen **Geschenkcode**, den wir dem Käufer
anzeigen und per E-Mail senden; bei der Einlösung speichern wir den Namen, den Zeitpunkt und – falls die einlösende Person angemeldet
ist – ihr Konto.

Damit unsere Mods die Vorzüge im Spiel erkennen, gibt es eine **öffentliche Abfrage** `https://account.simplemods.de/api/v1/mc/<Name>`.
Sie antwortet für einen Minecraft-Namen nur, ob und welche Vorzüge (Stufe, Vorzugs-Schlüssel) bis wann gelten; den Käufer, sein Konto oder
Zahlungsangaben gibt sie nicht preis. Die Abfrage ist ratenbegrenzt. Wer einen Namen kennt, kann damit also sehen, dass für ihn
Vorzüge bestehen.

- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO gegenüber dem Käufer; gegenüber der beschenkten Person berechtigtes Interesse an der
  Erbringung des Geschenks im Spiel, Art. 6 Abs. 1 lit. f DSGVO. Die beschenkte Person kann widersprechen (Abschnitt 17); dann entfernen
  wir ihren Namen, und der Käufer kann einen anderen Namen angeben.
- Herkunft: Den Minecraft-Namen hat uns der Käufer genannt (Art. 14 DSGVO).
- Speicherdauer: Namen bis 30 Tage nach Ablauf des Geschenk-Abos; Bestell- und Rechnungsdaten nach 8.1 und 8.3.

### 8.6 Gutscheine

Lösen Sie einen Gutscheincode ein, speichern wir den Code an Ihrer Bestellung und übermitteln ihn bzw. die zugehörige Rabatt-Kennung an
den Zahlungsanbieter (8.2). In der Verwaltung sehen wir,
welche Codes wie oft genutzt wurden. Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO, gegen Missbrauch lit. f.

### 8.7 Frühzugang

Der Download-Bereich ist nur nach Anmeldung erreichbar. Bei jedem Aufruf prüfen wir anhand Ihrer Sitzung und Ihrer Freischaltungen, ob Ihr
Konto den Vorzug „Frühzugang“ hat. Einzelne Downloads protokollieren wir nicht je Konto; der Webserver erfasst wie bei jedem Aufruf den
Pfad einschließlich Dateiname im Server-Log (4.2). Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO.

### 8.8 Kündigungsseite

Über „Verträge hier kündigen“ gelangen Sie – auch ohne Anmeldung – zur Kündigungsseite. Wir verarbeiten die angegebene E-Mail-Adresse,
den Vertrag, die Art der Kündigung, ggf. den Grund, Datum und Uhrzeit des Eingangs, das Ergebnis (zum Beispiel Vertragsende) und die
Zuordnung zu Konto und Abo. Wir gleichen die E-Mail-Adresse mit unseren Konten ab, setzen die Kündigung beim Zahlungsanbieter um und senden die
Eingangsbestätigung an die angegebene Adresse und an die Adresse des zugehörigen Kontos. Die Ergebnisseite verrät nicht, ob ein Konto
besteht.

- Rechtsgrundlage: Art. 6 Abs. 1 lit. c DSGVO (§ 312k BGB), lit. b (Vertragsbeendigung), lit. f (Nachweis, Missbrauchsschutz).
- Speicherdauer: drei Jahre ab Ende des Jahres, in dem der Vertrag endet bzw. die Kündigung einging.

## 9. E-Mails

Wir senden nur E-Mails, die für Konto und Verträge nötig sind: Bestätigung der E-Mail-Adresse, Passwort zurücksetzen,
Vertragsbestätigung, Rechnung bzw. Zahlungseingang, fehlgeschlagene Zahlung, Kündigungsbestätigung, Ende des Abos, Hinweis vor Ende der
12 Monate eines Jahresabos, Mitteilungen über Änderungen sowie Antworten auf Ihre Anfragen. Newsletter und Werbung senden wir nicht.

Für den Versand nutzen wir einen SMTP-Dienstleister, für den Empfang ein E-Mail-Postfach bei einem E-Mail-Anbieter. Diese Anbieter
verarbeiten Absender, Empfänger, Inhalt und Zeitpunkt der E-Mails in unserem Auftrag (Art. 28 DSGVO). Soweit ein Anbieter Daten in den
USA oder einem anderen Drittland verarbeitet, erfolgt das auf Grundlage des EU-US Data Privacy Framework oder von
Standardvertragsklauseln. Nutzen wir für unsere Kontaktadressen eine Weiterleitung über Cloudflare Email Routing, laufen eingehende
E-Mails über Cloudflare (4.3) zu unserem Postfach; Cloudflare speichert dabei keine Inhalte dauerhaft.

Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO (Vertrag), lit. c (gesetzliche Pflichten, zum Beispiel § 312f, § 312k BGB) und lit. f.

## 10. Kontaktformular und Anfragen

Unter `https://account.simplemods.de/kontakt` können Sie uns schreiben. Wir verarbeiten:

- **Pflichtangaben:** Name, E-Mail-Adresse, Thema (allgemein, Datenschutz, Meldung), Nachricht (höchstens 5000 Zeichen),
- **technische Angaben:** ein leeres Prüffeld gegen Spam-Programme (Honeypot), einen signierten Zeitstempel (Mindest-Ausfüllzeit
  von 3 Sekunden), ein Sicherheits-Token gegen Fremdzugriffe (CSRF) und eine Ratenbegrenzung je IP-Adresse und E-Mail-Adresse
  (nur Arbeitsspeicher, 5.4).

Die Nachricht wird nicht in unserer Datenbank gespeichert, sondern nur als E-Mail an uns gesendet; Ihre E-Mail-Adresse wird als
Antwortadresse eingetragen. Eine Kopie an Sie senden wir nicht, damit das Formular nicht zum Versand unerwünschter E-Mails an fremde
Adressen missbraucht werden kann. Dasselbe gilt, wenn Sie uns direkt per E-Mail schreiben.

- Zweck: Bearbeitung und Beantwortung Ihrer Anfrage.
- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO, wenn die Anfrage einen Vertrag betrifft, sonst lit. f (Beantwortung von Anfragen);
  bei Datenschutzanfragen lit. c.
- Speicherdauer: Wir löschen Anfragen, wenn sie erledigt sind, spätestens drei Jahre nach dem letzten Kontakt. E-Mails, die
  Geschäftsbriefe sind (zum Beispiel zu einem Vertrag), bewahren wir sechs Jahre auf (8.3).

## 11. Meldungen rechtswidriger Inhalte

Melden Sie Inhalte (per E-Mail an {{kontakt.melden}} oder über das Kontaktformular), verarbeiten wir Ihren Namen, Ihre E-Mail-Adresse, die
Meldung, den gemeldeten Inhalt, unsere Entscheidung und die Mitteilungen an Sie und an den betroffenen Nutzer. Den Namen der meldenden
Person geben wir an den betroffenen Nutzer nur weiter, wenn das für die Bearbeitung erforderlich ist und Sie zugestimmt haben.

- Rechtsgrundlage: Art. 6 Abs. 1 lit. c DSGVO (Art. 16, 17 der Verordnung (EU) 2022/2065) und lit. f (Schutz der Nutzer, Rechtsverteidigung).
- Speicherdauer: drei Jahre ab Ende des Jahres, in dem die Bearbeitung abgeschlossen wurde.

## 12. Empfänger und Drittländer

Personenbezogene Daten erhalten außer uns nur:

| Empfänger | Zweck | Ort | Grundlage |
|---|---|---|---|
| Cloudflare, Inc. | CDN, Reverse Proxy, Tunnel, DNS, Schutz vor Angriffen, ggf. E-Mail-Weiterleitung | USA, weltweites Netz | Auftragsverarbeitung; Data Privacy Framework, Standardvertragsklauseln |
{{#wenn zahlung.paddle}}| Paddle.com Market Limited (Paddle-Gruppe) | Verkauf als Wiederverkäufer (Merchant of Record), Zahlungsabwicklung, Abo-Verwaltung, Rechnungen, Steuerberechnung | Vereinigtes Königreich, USA | eigener Verantwortlicher; Angemessenheitsbeschluss (UK), Data Privacy Framework, Standardvertragsklauseln |{{#sonst}}| Stripe Payments Europe, Limited (mit Stripe, Inc.) | Zahlungsabwicklung, Abo-Verwaltung, Rechnungen, ggf. Steuerberechnung | Irland, USA | eigener Verantwortlicher bzw. Auftragsverarbeitung; Data Privacy Framework, Standardvertragsklauseln |{{/wenn}}
| SMTP-Dienstleister und E-Mail-Anbieter | Versand und Empfang von E-Mails | EU oder Drittland | Auftragsverarbeitung; bei Drittland Data Privacy Framework oder Standardvertragsklauseln |
| Steuerberatung (falls beauftragt) | Buchhaltung, Steuererklärungen | Deutschland | Art. 6 Abs. 1 lit. c DSGVO; berufliche Schweigepflicht |
| Kartenbetrachter, Wiki-Leser | Anzeige von Karten (nach Sichtbarkeit) und freigegebenen Kommentaren | weltweit | nach Ihren Einstellungen bzw. Ihrer Veröffentlichung |

Darüber hinaus geben wir Daten nur weiter, wenn wir gesetzlich dazu verpflichtet sind (zum Beispiel auf behördliche oder gerichtliche
Anordnung, Art. 6 Abs. 1 lit. c DSGVO) oder Sie eingewilligt haben. Wir verkaufen keine Daten.

## 13. Cookies und lokale Speicherung

Wir verwenden nur technisch notwendige Cookies und vergleichbare Techniken. Sie sind nach § 25 Abs. 2 Nr. 2 TDDDG ohne Einwilligung
zulässig, weil sie für einen von Ihnen ausdrücklich gewünschten Dienst erforderlich sind. Werbe-, Tracking- und Analyse-Cookies setzen wir
nicht.

| Name | Zweck | Laufzeit | Eigenschaften |
|---|---|---|---|
| `__Secure-sm_session` | Anmeldung (Sitzungs-Token) | 30 Tage bzw. bis zur Abmeldung | HttpOnly, Secure, SameSite=Lax; gilt für alle Seiten unter `simplemods.de` |
| `sm_csrf` | Schutz von Formularen vor Fremdzugriffen (CSRF), solange Sie nicht angemeldet sind | 2 Stunden | nur für die jeweilige Seite (Host) |
| `sm_lang` | gewählte Sprache | 1 Jahr | enthält nur den Sprachcode |
| `__cf_bm`, `cf_clearance` (Cloudflare) | Bot-Erkennung, Sicherheitsprüfung | 30 Minuten bzw. bis zu 1 Jahr | nur bei Bedarf, siehe 4.3 |
| lokaler Speicher des Browsers | Design, Sprache, Anzeigeeinstellungen der Karte | bis Sie die Browserdaten löschen | wird nicht an uns übertragen |

Auf der Bezahlseite und im Kundenportal des Zahlungsanbieters setzt dieser eigene Cookies bzw. lokale Speicherung ein; dafür gelten
seine Hinweise (8.2). Sie können
Cookies in Ihrem Browser löschen oder blockieren; ohne das Sitzungs-Cookie ist keine Anmeldung möglich.

## 14. Datensicherheit

Die Verbindung zu unseren Seiten ist per TLS verschlüsselt; zwischen Cloudflare und unserem Server läuft der Verkehr über einen
verschlüsselten Cloudflare Tunnel, ohne offenen Port. Passwörter speichern wir nur als Hash (PBKDF2-SHA256, 600 000 Runden, mit Salz),
Sitzungs- und Einmal-Token nur als Hash. Formulare sind gegen CSRF geschützt, Anmeldeversuche begrenzt. Die Schnittstelle der Mod
akzeptiert nur einen Karten-Schlüssel. Zugangsdaten und Schlüssel liegen außerhalb des Programmcodes mit eingeschränkten Dateirechten. Auf
den Server hat nur der Betreiber Zugriff (Anmeldung nur mit Schlüssel). Wir spielen Sicherheitsupdates zeitnah ein und sichern die Daten
regelmäßig verschlüsselt auf eigenen Datenträgern des Betreibers in Deutschland; Datensicherungen bewahren wir höchstens 30 Tage auf.

## 15. Automatisierte Entscheidungen

Eine ausschließlich automatisierte Entscheidung einschließlich Profiling im Sinne von Art. 22 DSGVO findet nicht statt. Technische
Grenzen (Ratenbegrenzung, Stufen-Grenzen) gelten für alle gleich. Die Freischaltung und Herabstufung von Vorzügen nach dem Zahlungs-
und Abo-Status erfolgt automatisch, setzt aber nur die vereinbarten Vertragsbedingungen um. Auffällige Muster und Meldungen prüfen wir von
Hand.

## 16. Speicherdauer, Export und Löschung

Wir speichern Daten nur, solange es für den jeweiligen Zweck nötig ist oder das Gesetz es verlangt (Übersicht in Abschnitt 3).

- **Export:** Unter „Konto“ in Simple Livemap (`/app/account`) können Sie Ihre Konto- und Kartendaten als JSON-Datei herunterladen.
  Bestell- und Abo-Daten erhalten Sie auf Anfrage.
- **Konto löschen:** Unter „Konto“ können Sie Ihr Konto löschen. Wir löschen dann sofort Konto, Sitzungen, Karten (auch im Puffer),
  Freigaben, Mitspieler-Plätze und Ihre Wiki-Kommentare. Daten, die wir aufbewahren müssen (zum Beispiel Rechnungen, Bestellungen und
  Kündigungen für die genannten Fristen), sperren wir und löschen sie nach Ablauf der Frist. In Datensicherungen verschwinden gelöschte Daten
  spätestens nach 30 Tagen.

## 17. Ihre Rechte

Sie haben nach der DSGVO das Recht auf

- **Auskunft** (Art. 15), **Berichtigung** (Art. 16), **Löschung** (Art. 17), **Einschränkung der Verarbeitung** (Art. 18),
- **Datenübertragbarkeit** (Art. 20),
- **Widerspruch** (Art. 21): Verarbeiten wir Daten auf Grundlage berechtigter Interessen (Art. 6 Abs. 1 lit. f DSGVO), können Sie aus
  Gründen, die sich aus Ihrer besonderen Situation ergeben, jederzeit widersprechen. Wir verarbeiten die Daten dann nicht mehr, es sei denn,
  wir weisen zwingende schutzwürdige Gründe nach, die Ihre Interessen überwiegen, oder die Verarbeitung dient der Geltendmachung, Ausübung
  oder Verteidigung von Rechtsansprüchen,
- **Widerruf einer Einwilligung** (Art. 7 Abs. 3) mit Wirkung für die Zukunft, soweit wir eine Einwilligung eingeholt haben.

Eine Nachricht an {{kontakt.datenschutz}} genügt. Zur Identifizierung können wir Sie bitten, die Anfrage von der E-Mail-Adresse Ihres Kontos
zu senden.

**Beschwerderecht.** Sie können sich bei einer Datenschutz-Aufsichtsbehörde beschweren (Art. 77 DSGVO), insbesondere in dem
Mitgliedstaat Ihres Aufenthaltsorts oder Arbeitsplatzes oder des Orts des mutmaßlichen Verstoßes. Für uns zuständig ist die für den
Wohnsitz des Betreibers zuständige Landesdatenschutzbehörde. Eine Liste der deutschen Aufsichtsbehörden mit Anschriften führt der
Bundesbeauftragte für den Datenschutz und die Informationsfreiheit:
https://www.bfdi.bund.de/DE/Service/Anschriften/Laender/Laender-node.html

Die Landesdatenschutzbehörden im Einzelnen:

| Land | Website |
|---|---|
| Baden-Württemberg | https://www.baden-wuerttemberg.datenschutz.de |
| Bayern (nicht-öffentlicher Bereich) | https://www.lda.bayern.de |
| Berlin | https://www.datenschutz-berlin.de |
| Brandenburg | https://www.lda.brandenburg.de |
| Bremen | https://www.datenschutz.bremen.de |
| Hamburg | https://datenschutz-hamburg.de |
| Hessen | https://datenschutz.hessen.de |
| Mecklenburg-Vorpommern | https://www.datenschutz-mv.de |
| Niedersachsen | https://www.lfd.niedersachsen.de |
| Nordrhein-Westfalen | https://www.ldi.nrw.de |
| Rheinland-Pfalz | https://www.datenschutz.rlp.de |
| Saarland | https://www.datenschutz.saarland.de |
| Sachsen | https://www.datenschutz.sachsen.de |
| Sachsen-Anhalt | https://datenschutz.sachsen-anhalt.de |
| Schleswig-Holstein | https://www.datenschutzzentrum.de |
| Thüringen | https://www.tlfdi.de |

## 18. Änderungen dieser Erklärung

Wir passen diese Erklärung an, wenn sich unsere Dienste oder die Rechtslage ändern. Es gilt die jeweils auf unseren Seiten
veröffentlichte Fassung. Über wesentliche Änderungen, die Ihr Konto betreffen, informieren wir Sie zusätzlich per E-Mail.
