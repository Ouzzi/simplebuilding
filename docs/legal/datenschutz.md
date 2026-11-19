# Datenschutzerklärung – Simple-Mods-Konto, Abo „Simple Mods Pro“ und Simple Livemap (ENTWURF)

> **ENTWURF – nicht ausliefern.** Stand 19.11.2026. Keine Rechtsberatung. Vor Veröffentlichung von einer Anwältin,
> einem Anwalt (IT-/Datenschutzrecht) oder einem geprüften Generator prüfen lassen.
> Absätze mit „**[Anmerkung Entwurf]**“ sind Hinweise an den Besitzer und werden vor der Veröffentlichung entfernt.
> Alles in ⟦PLATZHALTER: …⟧ muss der Besitzer eintragen oder entscheiden.
> Die Beschreibung der Verarbeitung folgt der Spezifikation und dem Plan (`PLAN-A-LIVEMAP34-2026-11-12.md`, D1–D22), für das Abo
> „Simple Mods Pro“ dem Plan `PLAN-B-N108-STRIPE-2026-11-19.md` (E2–E11). Sie ist
> gegen den **gebauten** Dienst abzugleichen, bevor der Text veröffentlicht wird (Liste im README, Abschnitt „Abgleich mit dem Code“).

---

## 1. Verantwortlicher und Kontakt

Verantwortlich für die Verarbeitung personenbezogener Daten auf `account.simplemods.de` und `livemap.simplemods.de` im
Sinne der Datenschutz-Grundverordnung (DSGVO) ist:

⟦PLATZHALTER: Name und ladungsfähige Anschrift des Betreibers – identisch mit dem Impressum⟧
E-Mail: ⟦PLATZHALTER: Datenschutz-Kontakt, z. B. datenschutz@…⟧

Einen Datenschutzbeauftragten haben wir nicht benannt, weil dies nach Art. 37 DSGVO und § 38 BDSG nicht erforderlich ist
(weniger als 20 Personen sind ständig mit der automatisierten Verarbeitung personenbezogener Daten beschäftigt).
⟦PLATZHALTER: bestätigen lassen, ob die Voraussetzungen zutreffen⟧

## 2. Geltungsbereich und Überblick

Diese Erklärung gilt für

- das zentrale Konto (`account.simplemods.de`),
- das Konto-Abo „Simple Mods Pro“ (Bestellung, Abo-Verwaltung, Zahlung, Kündigungsseite) und den anmeldepflichtigen
  Download-Bereich für den Frühzugang, beide auf `account.simplemods.de` (Abschnitt 9),
- den gehosteten Kartendienst „Simple Livemap“ (`livemap.simplemods.de`) einschließlich der Schnittstelle, über die die
  Minecraft-Mod „Simple Livemap“ Karten-Daten hochlädt,
- die Karten-Ansicht (Viewer), mit der Karten betrachtet werden.

**Nicht erfasst:** Wenn Sie die Mod ohne unseren gehosteten Dienst betreiben („Self-Host“), werden dadurch keine Daten an uns
übertragen; die Mod verbindet sich mit dem gehosteten Dienst erst, nachdem ein Spielleiter den Befehl
`/simplelivemap connect <Code>` ausgeführt hat. ⟦PLATZHALTER: bestätigen, dass die Mod ohne `connect` keinerlei Daten an
simplemods.de sendet (auch keine Update-Prüfung)⟧ Für andere Angebote von Simple Mods (z. B. Wiki, Landingpage) gilt, bis ein gemeinsamer Text
besteht, ⟦PLATZHALTER: eigene Datenschutzerklärung der jeweiligen Seite / gemeinsame Erklärung – entscheiden⟧.

Wir verarbeiten nur die Daten, die für Konto, Karten und sicheren Betrieb nötig sind. Die folgende Tabelle gibt einen Überblick;
die Abschnitte 3 bis 11 erklären die Einzelheiten.

| Daten | Zweck | Rechtsgrundlage | Speicherdauer |
|---|---|---|---|
| Server-Logs (IP-Adresse, Zeit, aufgerufene Adresse, Status, übertragene Bytes) | Betrieb, Sicherheit, Fehleranalyse | Art. 6 Abs. 1 lit. f | ⟦PLATZHALTER: Vorschlag 7 Tage⟧ |
| Kontakt per E-Mail, Meldungen zu Inhalten | Beantwortung von Anfragen, Prüfung von Meldungen | Art. 6 Abs. 1 lit. b, c, f | ⟦PLATZHALTER: Dauer⟧ |
| Konto: E-Mail, Anzeigename, Passwort-Hash, Rolle/Stufe, Erstell- und Bestätigungszeit | Konto, Anmeldung, Vertrag | Art. 6 Abs. 1 lit. b | bis zur Löschung des Kontos |
| Sitzung: Token-Hash, Ablaufzeit (30 Tage), keine IP-Adresse | angemeldet bleiben | Art. 6 Abs. 1 lit. b; § 25 Abs. 2 Nr. 2 TDDDG | bis Abmeldung/Ablauf, spätestens 30 Tage |
| Anmelde-Kennzeichen („Signup Tag“): gekürzter HMAC-Wert der IP-Adresse bei der Registrierung | Erkennung von Mehrfach-Konten und Missbrauch | Art. 6 Abs. 1 lit. f | 30 Tage |
| IP-Adressen und E-Mail-Adressen in Ratenbegrenzungen (nur Arbeitsspeicher) | Schutz vor Brute-Force und Massenanfragen | Art. 6 Abs. 1 lit. f | höchstens 1 Stunde, nur im Arbeitsspeicher |
| E-Mails (Bestätigung, Passwort zurücksetzen) | Konto-Funktionen | Art. 6 Abs. 1 lit. b | Einmal-Links 24 Stunden bzw. 1 Stunde gültig |
| Karten: Name, Sichtbarkeit, zufällige Welt-ID, Kartenkacheln, Verbindungsangaben | Karte bereitstellen | Art. 6 Abs. 1 lit. b | bis Trennen, danach Puffer (siehe 6.4), dann Löschung |
| Live-Daten: Spielername, Dimension, x/z-Position (nur wenn „Spieler zeigen“ aktiv) | Spieler auf der Karte anzeigen | Auftragsverarbeitung für den Kartenbesitzer (Art. 28) | nur letzter Stand, kein Verlauf |
| Geteilte Konten (Zuordnung Karte ↔ Konto) | Karten teilen | Art. 6 Abs. 1 lit. b | bis Aufhebung der Freigabe/Löschung |
| Nutzungszahlen (Aufrufe je Tag, aggregiert, ohne IP) | Stufen-Limits, Hinweise | Art. 6 Abs. 1 lit. b, f | ⟦PLATZHALTER: Dauer, z. B. 13 Monate⟧ |
| Zahlungsdaten (erst nach Bezahlstart, über Stripe) | Abrechnung | Art. 6 Abs. 1 lit. b, c | gesetzliche Fristen (siehe 9) |
| Bestellung und Abo (erst nach Bezahlstart): Stufe, Laufzeit, Zustimmungen mit Zeitpunkt und Textversion, Abo-Status, Stripe-Kunden- und Abo-Kennung, Freischaltungen | Vertragsschluss, Nachweis der Zustimmungen, Freischaltung der Vorzüge | Art. 6 Abs. 1 lit. b, c | ⟦PLATZHALTER: Dauer, z. B. Vertragsdauer plus Verjährungsfrist; Rechnungsbezug gesetzliche Fristen⟧ |
| Stripe-Ereignisse: nur Ereignis-Kennung und -Typ | doppelte Verarbeitung verhindern | Art. 6 Abs. 1 lit. b, f | ⟦PLATZHALTER: Dauer, z. B. 90 Tage⟧ |
| Kündigungen über die Kündigungsseite: E-Mail, Vertrag, Art, ggf. Grund, Zeitpunkt des Eingangs | Kündigung bearbeiten und bestätigen (§ 312k BGB), Nachweis | Art. 6 Abs. 1 lit. b, c, f | ⟦PLATZHALTER: Dauer, z. B. 3 Jahre nach Vertragsende⟧ |
| Frühzugang: Prüfung der Berechtigung beim Abruf | Zugriff nur für Berechtigte | Art. 6 Abs. 1 lit. b | keine eigene Speicherung je Download; Server-Logs siehe 3.1 |

## 3. Server-Logs und Cloudflare

### 3.1 Server-Logs (nginx)

Bei jedem Aufruf unserer Seiten verarbeitet unser Webserver (nginx) technisch notwendige Daten und speichert sie in
Log-Dateien: IP-Adresse des Besucher-Geräts (so, wie sie uns über Cloudflare übermittelt wird), Datum und Uhrzeit, die
aufgerufene Adresse ⟦PLATZHALTER: mit/ohne Query-Teil; Konzept sieht „ohne Query“ vor – prüfen⟧, Statuscode, übertragene
Datenmenge ⟦PLATZHALTER: Referrer und User-Agent ja/nein – gegen die nginx-Konfiguration prüfen⟧. Im Fehler-Log von nginx können zusätzlich Meldungen
stehen (z. B. abgelehnte Anfragen wegen Ratenbegrenzung, Abschnitt 4.4), ebenfalls mit IP-Adresse.

- Zweck: Betrieb, Fehlerbehebung, Abwehr von Angriffen und Missbrauch (Sicherheit der Verarbeitung, Art. 32 DSGVO).
- Rechtsgrundlage: berechtigtes Interesse, Art. 6 Abs. 1 lit. f DSGVO.
- Speicherdauer: ⟦PLATZHALTER: Aufbewahrung der Logs; Vorschlag 7 Tage, danach automatische Löschung/Rotation⟧.
  ⟦PLATZHALTER: ggf. IP-Kürzung (/24 bzw. /48) vor dem Speichern, falls so umgesetzt⟧

### 3.2 Cloudflare (CDN, Schutz, Tunnel)

Unsere Seiten sind über Cloudflare erreichbar. Cloudflare arbeitet als vorgeschalteter Dienst (Reverse Proxy, Content Delivery
Network, Cloudflare Tunnel) und nimmt Ihre Anfragen zuerst entgegen. Dabei verarbeitet Cloudflare technisch bedingt den gesamten
Datenverkehr zwischen Ihrem Gerät und unserem Server, **einschließlich der Anmeldedaten und Karteninhalte**, weil die
TLS-Verschlüsselung dort endet. Cloudflare verarbeitet insbesondere IP-Adresse, Zeitpunkt, angefragte Adresse, Browser-Angaben
und Sicherheitsmerkmale. Cloudflare speichert Karten-Kacheln zwischen (Cache), damit häufig aufgerufene Karten schnell
ausgeliefert werden und unser Server entlastet wird.

- Anbieter: ⟦PLATZHALTER: Vertragspartner laut Cloudflare-Vertrag, i. d. R. Cloudflare, Inc., 101 Townsend St., San Francisco, CA 94107, USA – bestätigen⟧
- Zweck: schnelle und sichere Auslieferung, Schutz vor Angriffen, Erreichbarkeit des Servers ohne offenen Port.
- Rechtsgrundlage: berechtigtes Interesse, Art. 6 Abs. 1 lit. f DSGVO.
- Drittlandübermittlung: Cloudflare kann Daten in den USA und weiteren Ländern verarbeiten. Grundlage ist der
  Angemessenheitsbeschluss der EU-Kommission zum EU-US Data Privacy Framework (Beschluss (EU) 2023/1795), soweit Cloudflare
  zertifiziert ist, ergänzend die Standardvertragsklauseln (Beschluss (EU) 2021/914) im Auftragsverarbeitungsvertrag
  (Data Processing Addendum) von Cloudflare. ⟦PLATZHALTER: Zertifizierung und DPA-Abschluss vom Besitzer prüfen; Stand der Rechtsprechung zum Data Privacy Framework beobachten⟧
- Cloudflare kann eigene technisch notwendige Cookies setzen (z. B. zur Bot-Erkennung). ⟦PLATZHALTER: tatsächlich gesetzte Cloudflare-Cookies prüfen und in Abschnitt 8 eintragen; Cloudflare Web Analytics/Beacon: nicht aktiviert – bestätigen⟧
- Weitere Informationen: Datenschutzhinweise von Cloudflare ⟦PLATZHALTER: Link⟧.

## 4. Konto (account.simplemods.de)

### 4.1 Registrierung und Konto

Für ein Konto verarbeiten wir: E-Mail-Adresse, Anzeigename, Passwort (nur als Hash, Verfahren PBKDF2-SHA256 mit Salz; wir
können das Passwort nicht im Klartext lesen), Rolle und Stufe (z. B. Nutzer, Free/Pro/Max/Ultra), Zeitpunkt der Erstellung und
der E-Mail-Bestätigung sowie – technisch bedingt – Zähler und Zeitstempel zur Anwendung der Nutzungsbedingungen (Anzahl bisheriger
Karten-Verbindungen, letzte Belegung eines Karten-Platzes).

- Zweck: Bereitstellung des Kontos und der Karten-Funktionen, Durchführung des Nutzungsvertrags.
- Rechtsgrundlage: Vertragserfüllung, Art. 6 Abs. 1 lit. b DSGVO.
- Pflicht zur Bereitstellung: Ohne E-Mail-Adresse, Anzeigename und Passwort können wir kein Konto anlegen.
- Speicherdauer: bis zur Löschung des Kontos (Abschnitt 11). ⟦PLATZHALTER: Löschung inaktiver Konten – Frist festlegen oder streichen; in der Spezifikation nicht geregelt⟧

### 4.2 E-Mail-Bestätigung und Passwort zurücksetzen

Zur Bestätigung der E-Mail-Adresse und zum Zurücksetzen des Passworts senden wir Ihnen eine E-Mail mit einem Einmal-Link. Die
Links sind 24 Stunden (Bestätigung) bzw. 1 Stunde (Zurücksetzen) gültig; auf unserem Server speichern wir die zugehörigen
Token nur als Hash. Für den Versand setzen wir einen E-Mail-Dienstleister ein (Abschnitt 10). Wir versenden derzeit
ausschließlich solche Konto-E-Mails und – ab Bezahlstart – E-Mails zum Abo (Abschnitt 9.4), keine Newsletter und keine Werbung. ⟦PLATZHALTER: ggf. Hinweis auf weitere Transaktions-Mails (Rechnung, Kündigungsbestätigung) ab Bezahlstart⟧

- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO (Vertrag) und lit. f (Sicherheit des Kontos).

### 4.3 Anmeldung und Sitzungen

Nach dem Login legen wir eine Sitzung an. Dazu erzeugen wir ein zufälliges Token, speichern davon **nur den Hash** und setzen
beim Browser ein Cookie (Abschnitt 8). Die Sitzung läuft nach 30 Tagen ab; bei Abmeldung, Passwortwechsel oder „überall
abmelden“ werden Sitzungen gelöscht. **Die Sitzung enthält keine IP-Adresse.** Das Sitzungs-Cookie gilt für alle Seiten unter
`*.simplemods.de` ⟦PLATZHALTER: bestätigen, dass das Cookie auch für das Wiki o. Ä. gilt und dort keine anderen Daten daraus erhoben werden⟧.

- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO; für das Cookie § 25 Abs. 2 Nr. 2 TDDDG.

### 4.4 Schutz vor Missbrauch

- **Anmelde-Kennzeichen (Signup Tag).** Bei der Registrierung speichern wir aus Ihrer IP-Adresse einen **gekürzten
  HMAC-Wert** (mit einem nur uns bekannten Schlüssel gebildet), nicht die IP-Adresse selbst. Er dient dazu, auffällige
  Häufungen (z. B. viele neue Konten vom selben Anschluss) zu erkennen. Aufbewahrung: **30 Tage**, danach wird er gelöscht.
  Da der Wert mit unserem Schlüssel einer IP-Adresse zugeordnet werden kann, behandeln wir ihn als personenbezogenes Datum
  (Pseudonym).
- **Ratenbegrenzungen.** Zum Schutz vor Brute-Force-Angriffen und Massenanfragen (Anmeldung, Registrierung, „Passwort
  vergessen“, Code-Einlösung, Uploads) zählen wir Versuche je IP-Adresse und je E-Mail-Adresse. Diese Zähler liegen **nur im
  Arbeitsspeicher** und werden spätestens nach **1 Stunde** verworfen. Abgelehnte Anfragen können als Zeile im Server-Log (Abschnitt 3.1) erscheinen und folgen dessen Aufbewahrungsfrist.
  ⟦PLATZHALTER: gegen die Implementierung prüfen – Fenster 15 min bzw. 1 h; zusätzlich nginx-Ratenbegrenzung (Zähler nur im Speicher)⟧
- **Manuelle Prüfung.** Auffällige Muster (z. B. viele Konten, dauernder Austausch von Karten-Plätzen) werden in einer
  Verwaltungsliste angezeigt und von uns manuell geprüft.
- Rechtsgrundlage: berechtigtes Interesse an Sicherheit und Missbrauchsabwehr, Art. 6 Abs. 1 lit. f DSGVO; Sie können
  dem nach Abschnitt 12 widersprechen.

### 4.5 Kontakt und Meldungen

Wenn Sie uns per E-Mail kontaktieren oder rechtswidrige Inhalte melden (Impressum, AGB Abschnitt 6), verarbeiten wir Ihre E-Mail-Adresse, Ihren Namen (falls angegeben), Ihre Nachricht und die
zur Bearbeitung nötigen Angaben (z. B. betroffene Karte).

- Zweck: Beantwortung, Prüfung und Dokumentation von Meldungen und Entscheidungen.
- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO (Vertrag), lit. c (gesetzliche Pflichten, u. a. Verordnung (EU) 2022/2065) und lit. f (Rechtsverteidigung).
- Speicherdauer: ⟦PLATZHALTER: z. B. bis zur Erledigung plus Verjährungsfrist (3 Jahre) – festlegen⟧.

## 5. Betrachter von Karten (ohne Konto)

Wer eine öffentliche Karte oder eine per Link geteilte Karte ansieht, braucht kein Konto. Dabei fallen die Server-Logs
(Abschnitt 3) an. Außerdem zählen wir **Seitenaufrufe je Karte bzw. Konto des Kartenbesitzers und Tag** (nur Anzahl, **ohne IP-Adresse** und ohne Wiedererkennung
einzelner Besucher). Der Link-Schlüssel einer Link-Karte steht in der Adresse hinter dem „#“; wer den Link kennt, kann die Karte ansehen. ⟦PLATZHALTER: Übertragungsweg des Ansichtsschlüssels (bleibt er im Browser oder wird er für Kachel-Anfragen als Header/Parameter gesendet und damit ggf. geloggt?) gegen den gebauten Viewer prüfen⟧
Ihre Anzeigeeinstellungen im Viewer (z. B. Spieler anzeigen, Ansicht) werden in Ihrem Browser gespeichert (Abschnitt 8).

- Rechtsgrundlage: Art. 6 Abs. 1 lit. f DSGVO (Bereitstellung der Karten-Ansicht, Einhaltung der Stufen-Limits).

## 6. Karten (livemap.simplemods.de)

### 6.1 Karte verbinden

Zum Verbinden erzeugen Sie im Dashboard einen Einmal-Code (15 Minuten gültig). Ein Spielleiter führt auf dem Minecraft-Server
oder in der Einzelspieler-Welt `/simplelivemap connect <Code>` aus. Dabei übermittelt die Mod an uns: den Code, eine **zufällige
Welt-ID** (kein Welten-Seed), die Mod- und die Minecraft-Version. Wir geben der Mod einen Karten-Schlüssel zurück, mit dem sie danach Daten hochlädt. Der Schlüssel liegt auf Ihrem Server in der Datei `hosted.json` der Welt, nicht in der allgemeinen Konfiguration.
⟦PLATZHALTER: Speicherform des Karten-Schlüssels beim Dienst (Hash?) gegen Implementierung prüfen⟧

Zusätzlich verarbeitet unser Server die IP-Adresse, von der die Anfragen der Mod kommen (Server-Logs, Abschnitt 3.1).

### 6.2 Kartenkacheln

Die Mod lädt Kartenkacheln hoch: Bilder (128 × 128 Pixel, PNG) des Geländes, das Spielerinnen und Spieler erkundet haben. Die Kacheln
enthalten **keinen Seed und keine Spielerfiguren**, **können aber Bauwerke, Schilder und Beschriftungen zeigen**, die auch Namen
oder andere personenbezogene Angaben enthalten können, die Sie selbst im Spiel eingebaut haben. Für den Inhalt der Karte ist
der Kartenbesitzer verantwortlich (AGB, Abschnitt 5).

Zur Entlastung des Servers werden Kacheln dedupliziert gespeichert und über Cloudflare ausgeliefert/zwischengespeichert.
⟦PLATZHALTER: ab Bezahlstart ggf. Ablage in Cloudflare R2 (Objektspeicher; EU-Jurisdiktion?) – hier und in der AV-Liste ergänzen⟧

- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO (Vertrag mit dem Kartenbesitzer). Für personenbezogene Daten Dritter im Karteninhalt
  gilt Abschnitt 7.

### 6.3 Sichtbarkeit und geteilte Konten

Jede Karte ist **privat** (nur Besitzer und freigegebene Konten), **per Link** (jeder, der den geheimen Link kennt) oder
**öffentlich** (jeder). Der Kartenbesitzer legt das fest. Bei der Freigabe für andere Konten speichern wir die Zuordnung
Karte ↔ Konto. ⟦PLATZHALTER: welche Angaben des eingeladenen Kontos (E-Mail? Anzeigename?) dem Kartenbesitzer angezeigt werden – gegen die Implementierung prüfen⟧
Der Kartenname kann – je nach Stufe – Teil der öffentlichen Adresse (`/m/<name>`) sein. Eine Verzeichnisseite öffentlicher Karten
(„Explore“) gibt es erst, wenn sie eingeführt ist; Aufnahme nur mit Zustimmung ⟦PLATZHALTER: Einwilligungstext bei Einführung ergänzen⟧.

### 6.4 Trennen, Puffer, Löschung von Karten

Wird eine Karte getrennt oder durch Herabstufen überzählig, bleiben die Daten zunächst gespeichert („Puffer“), damit ein
Wieder-Verbinden schneller geht. Der Puffer läuft je nach Stufe nach **30 Tagen (Free), 90 Tagen (Pro), 180 Tagen (Max) oder
einem Jahr (Ultra)** ab; bei Herabstufen bleiben überzählige Karten **30 Tage** im Puffer. Danach löschen wir die Karte und ihre
Kacheln. Sie können Karten auch vorher selbst löschen. Die Karte wird bei Konto-Löschung mitgelöscht (Abschnitt 11).

- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO.

### 6.5 Nutzungszahlen

Für die Anwendung der Stufen-Limits zählen wir die **Aufrufe der Karten-Ansicht je Konto des Kartenbesitzers (und je Karte) und Tag** (Summe, keine IP-Adresse, keine
Geräte-Kennung). Der Kartenbesitzer sieht die Zahlen „heute“ und „30 Tage“ im Dashboard.

- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO; Speicherdauer ⟦PLATZHALTER: Dauer festlegen⟧.

## 7. Spielernamen und Positionen (Live-Daten)

### 7.1 Was verarbeitet wird

Wenn der Kartenbesitzer „**Spieler zeigen**“ einschaltet (Standard: aus), übermittelt die Mod regelmäßig (je nach Stufe alle
1 bis 10 Sekunden) für die gerade online befindlichen Spielerinnen und Spieler: **Minecraft-Spielername, Dimension, x- und
z-Position**. Wir speichern **nur den jeweils letzten Stand**; es gibt keinen Verlauf und kein Protokoll der Positionen.
Ist „Spieler zeigen“ aus, speichern wir keine Spielerangaben. ⟦PLATZHALTER: bestätigen, ob die Mod in diesem Fall Spielerangaben gar nicht erst sendet oder ob der Dienst sie beim Empfang verwirft – Text ggf. anpassen⟧ Je nach Sichtbarkeit der Karte sehen diese Angaben der
Kartenbesitzer, freigegebene Konten, alle Personen mit dem Link oder – bei öffentlichen Karten – jeder im Internet.
⟦PLATZHALTER: Zeitpunkt der Löschung des Live-Stands beim Trennen/Ausschalten von „Spieler zeigen“ gegen die Implementierung prüfen⟧

### 7.2 Rollen

Für diese Daten ist der **Kartenbesitzer** (Betreiber des Minecraft-Servers) der **Verantwortliche**. Er entscheidet, ob und für
wen Spieler angezeigt werden, und braucht dafür eine eigene Rechtsgrundlage (z. B. berechtigtes Interesse bei einem Server
mit Karten-Funktion oder Einwilligung der Spieler) und muss seine Spieler informieren. Wir verarbeiten die Live-Daten **in seinem
Auftrag und nach seinen Weisungen als Auftragsverarbeiter** (Art. 28 DSGVO). Dazu bieten wir Kartenbesitzern einen
Auftragsverarbeitungsvertrag an (abrufbar unter ⟦PLATZHALTER: URL des Auftragsverarbeitungsvertrags⟧). Für unsere eigenen Zwecke (Konto, Sicherheit,
Abrechnung) sind wir selbst Verantwortliche.

### 7.3 Information für Spielerinnen und Spieler (Art. 14 DSGVO)

Wenn Ihr Spielername und Ihre Position auf einer Karte erscheinen, haben wir diese Daten **nicht von Ihnen**, sondern vom
Server bzw. der Mod des Kartenbesitzers erhalten.

- **Kategorien:** Spielername, Dimension, x/z-Position, Zeitpunkt des letzten Standes; (nur als Bild) ggf. Beschriftungen in Kacheln.
- **Verantwortlicher:** der Kartenbesitzer (der Betreiber des Servers). Wenden Sie sich für Auskunft, Löschung und Widerspruch
  zuerst an ihn. Wir leiten Anfragen, die uns erreichen, an den Kartenbesitzer weiter bzw. unterstützen ihn bei der Erfüllung.
  ⟦PLATZHALTER: Funktion/Befehl der Mod, mit der einzelne Spieler ausgeblendet werden können, gegen den Ist-Stand der Mod benennen⟧
- **Unser Kontakt:** siehe Abschnitt 1.
- **Speicherdauer:** nur der letzte Stand, überschrieben bei der nächsten Übermittlung.
- **Weitere Informationen** (Zwecke, Rechtsgrundlage, Empfänger) ergeben sich aus der Datenschutzinformation des
  Kartenbesitzers.

## 8. Cookies und lokale Speicherung

Wir setzen **ausschließlich technisch notwendige** Cookies und vergleichbare Technik ein. Sie sind nach § 25 Abs. 2 Nr. 2
TDDDG ohne Einwilligung zulässig, weil sie erforderlich sind, damit wir einen von Ihnen ausdrücklich gewünschten Dienst
bereitstellen können. Es gibt keine Werbe- oder Tracking-Cookies und keine Analyse-Werkzeuge von Dritten.
⟦PLATZHALTER: Aussage zum Zeitpunkt der Veröffentlichung bestätigen⟧

| Name | Zweck | Laufzeit | Besonderheiten |
|---|---|---|---|
| `__Secure-sm_session` | Anmeldung (Sitzungs-Token) | 30 Tage bzw. bis Abmeldung | HttpOnly, Secure, SameSite=Lax; gilt für `*.simplemods.de` |
| `sm_csrf` | Schutz vor CSRF-Angriffen bei Formularen vor der Anmeldung | ⟦PLATZHALTER: Laufzeit, z. B. Sitzung/1 Stunde⟧ | ⟦PLATZHALTER: Flags/Domain gegen Implementierung prüfen⟧ |
| ⟦PLATZHALTER: Name des Sprach-Cookies⟧ | gewählte Sprache | ⟦PLATZHALTER: Laufzeit⟧ | nur Spracheinstellung |
| lokaler Speicher des Browsers (Viewer-Einstellungen) | gemerkte Ansicht (z. B. Spieler anzeigen/folgen) | bis Sie die Browser-Daten löschen | wird nicht an uns übertragen ⟦PLATZHALTER: bestätigen⟧ |
| ⟦PLATZHALTER: Cloudflare-Cookies, falls gesetzt (z. B. Bot-Schutz)⟧ | Sicherheit | ⟦PLATZHALTER: Laufzeit laut Cloudflare⟧ | siehe 3.2 |

Sie können Cookies in den Browser-Einstellungen löschen oder blockieren; ohne das Sitzungs-Cookie ist keine Anmeldung möglich.

## 9. Abo „Simple Mods Pro“, Zahlung (Stripe), Kündigung und Frühzugang – derzeit nicht aktiv

*Variante bis zum Bezahlstart:* Bezahlen ist derzeit **nicht möglich** („coming soon“); es werden keine Zahlungsdaten erhoben. Sobald das Abo
„Simple Mods Pro“ eingeschaltet ist, wickeln wir Zahlungen über **Stripe** ab. Dann gilt zusätzlich: ⟦PLATZHALTER: diesen Abschnitt erst mit dem Bezahlstart scharf schalten und mit dem tatsächlichen Stripe-Vertrag abgleichen lassen⟧

*Variante ab Bezahlstart:* Die Überschrift lautet dann „9. Abo „Simple Mods Pro“, Zahlung (Stripe), Kündigung und Frühzugang“; der vorstehende Absatz entfällt.

### 9.1 Bestellung und Abo (auf unserem Server)

Wenn Sie das Abo bestellen, speichern wir in unserer Datenbank:

- **Bestellungen:** Konto, gewählte Stufe und Laufzeit, Zeitpunkt, Ihre **Zustimmungen** (AGB; freiwillig: vorzeitiger Beginn) jeweils mit
  **Zeitpunkt und Version des zugestimmten Textes**, Bezug zur Bezahlsitzung bei Stripe.
- **Abos:** Spiegel des Abos bei Stripe – Stripe-Kundenkennung, Stripe-Abo-Kennung, Stufe, Laufzeit, Status (z. B. aktiv, Zahlung offen, gekündigt),
  Ende des bezahlten Zeitraums, Kündigung zum Periodenende ja/nein, beim Jahresabo die Kennung des Zahlungsplans.
- **Freischaltungen (Entitlements):** welche Stufe und welche Vorzüge Ihr Konto hat, ab wann und bis wann, und ob sie aus dem Abo oder von uns
  manuell gesetzt sind.
- **Stripe-Ereignisse:** nur die **Kennung und den Typ** jeder von Stripe gemeldeten Nachricht, damit wir sie nicht doppelt verarbeiten; den Inhalt der
  Nachricht speichern wir dort nicht.

**Zahlungsmittel-Daten** (z. B. Kartennummer, IBAN) speichern wir **nicht**; Sie geben sie direkt bei Stripe ein.

- Zweck: Vertragsschluss und -durchführung, Freischaltung und Herabstufung der Vorzüge, Nachweis der Zustimmungen (§ 356 Abs. 4/5 BGB, § 312j BGB).
- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO; für den Nachweis zusätzlich lit. c und f.
- Speicherdauer: ⟦PLATZHALTER: Bestellungen/Zustimmungen, Abos und Freischaltungen – z. B. bis zum Ende des Vertrags plus Verjährungsfrist (3 Jahre); Stripe-Ereignisse z. B. 90 Tage; gegen das Löschkonzept prüfen⟧.
  Die Freischaltung von Vorzügen und die Herabstufung nach dem Zahlungs- bzw. Abo-Status erfolgen automatisch; das dient der Vertragserfüllung (siehe Abschnitt 13).

### 9.2 Zahlungsabwicklung durch Stripe

Für die Eingabe der Zahlungsdaten leiten wir Sie auf die Bezahlseite von Stripe („Stripe Checkout“) weiter; Rechnungen, Zahlungsdaten und das Abo können
Sie im Kundenportal von Stripe verwalten. Auf diesen Seiten gelten zusätzlich die Datenschutzhinweise von Stripe; Stripe kann dort eigene Cookies setzen
⟦PLATZHALTER: Link auf die Datenschutzhinweise von Stripe⟧.

- **Daten:** Name, E-Mail, Rechnungsadresse, ausgewählte Stufe und Laufzeit, Abo- und Zahlungsstatus, Rechnungsdaten. Zahlungsmittel-Daten (z. B. Kartennummer) gibt der Nutzer direkt bei Stripe ein; wir erhalten sie nicht, sondern nur eine Referenz und den Status.
  Wir übermitteln an Stripe Ihre E-Mail-Adresse, eine Kennung Ihres Kontos sowie Stufe und Laufzeit.
- **Steuerberechnung (optional):** Falls wir Stripe Tax einsetzen ⟦PLATZHALTER: Stripe Tax ja/nein⟧, verarbeitet Stripe zusätzlich Ihre Rechnungsadresse (Land) und ggf. Ihre
  USt-IdNr., um die Umsatzsteuer zu berechnen.
- **Anbieter:** ⟦PLATZHALTER: Stripe Payments Europe, Ltd., Irland (und Stripe, Inc., USA) – Vertragspartner laut Stripe-Konto⟧. Stripe verarbeitet Zahlungsdaten teils als eigener Verantwortlicher (z. B. Betrugsprävention, gesetzliche Pflichten), teils in unserem Auftrag. ⟦PLATZHALTER: Rollenverteilung anwaltlich bestätigen lassen⟧
- **Rechtsgrundlage:** Art. 6 Abs. 1 lit. b DSGVO (Vertrag), lit. c (steuer- und handelsrechtliche Aufbewahrung).
- **Aufbewahrung** von Rechnungen und Buchungsbelegen: nach Handels- und Steuerrecht (§ 257 HGB, § 147 AO) ⟦PLATZHALTER: Fristen (6/8/10 Jahre je Unterlage) bestätigen lassen⟧.
- **Drittland:** Übermittlungen in die USA auf Basis des Data Privacy Framework bzw. Standardvertragsklauseln ⟦PLATZHALTER: prüfen⟧.
- Die Webhook-Meldungen von Stripe an unseren Server werden auf ihre Signatur geprüft; wir speichern daraus nur den Abo-Status, die Zuordnung zum Konto und die in 9.1 genannten Angaben.

### 9.3 Kündigungsseite

Über den Link „Verträge hier kündigen“ in der Fußzeile gelangen Sie – auch ohne Anmeldung – zur Kündigungsseite. Dort verarbeiten wir die von Ihnen
angegebene **E-Mail-Adresse**, den **Vertrag**, die **Art der Kündigung** (ordentlich/außerordentlich), ggf. den **Grund**, den gewünschten Zeitpunkt sowie
**Datum und Uhrzeit des Eingangs**. Wir gleichen die E-Mail-Adresse mit unseren Konten ab, setzen bei einem zugeordneten Abo die Kündigung bei Stripe um
und senden die Eingangsbestätigung an die angegebene Adresse **und** an die Adresse des zugehörigen Kontos (damit eine Kündigung durch Unbefugte auffällt).
Zum Schutz vor Missbrauch gilt eine Ratenbegrenzung wie in Abschnitt 4.4.

- Rechtsgrundlage: Art. 6 Abs. 1 lit. c DSGVO (§ 312k BGB), lit. b (Vertragsbeendigung), lit. f (Nachweis, Missbrauchsschutz).
- Speicherdauer: ⟦PLATZHALTER: Dauer der Aufbewahrung von Kündigungen festlegen, z. B. 3 Jahre nach Vertragsende (Nachweis)⟧.

### 9.4 E-Mails zum Abo

Zum Abo senden wir Ihnen Transaktions-E-Mails: Vertragsbestätigung, Rechnung bzw. Zahlungseingang ⟦PLATZHALTER: ob wir selbst oder Stripe die Rechnungs-E-Mails versenden⟧,
Hinweis auf eine fehlgeschlagene Zahlung, Eingangsbestätigung einer Kündigung, Ende des Abos und beim Jahresabo den Hinweis vor Ablauf der 12 Monate.
Versand über unseren E-Mail-Dienstleister (Abschnitt 10). Rechtsgrundlage: Art. 6 Abs. 1 lit. b und c DSGVO.

### 9.5 Frühzugang (Download-Bereich)

Der Download-Bereich auf `account.simplemods.de` ist nur nach Anmeldung erreichbar. Bei jedem Aufruf prüfen wir anhand Ihrer Sitzung (Abschnitt 4.3) und
Ihrer Freischaltungen (9.1), ob Ihr Konto den Vorzug „Frühzugang“ hat. Die Anwendung protokolliert die einzelnen Downloads derzeit **nicht** gesondert
⟦PLATZHALTER: bestätigen, dass weiterhin keine Download-Protokollierung je Konto erfolgt; falls eingeführt (z. B. zur Missbrauchserkennung), Zweck, Daten und Speicherdauer hier ergänzen⟧.
Wie bei jedem Aufruf erfasst der Webserver die aufgerufene Adresse (einschließlich Dateiname) im Server-Log (Abschnitt 3.1).

- Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO.

## 10. Empfänger, Auftragsverarbeiter und Drittländer

Empfänger oder Auftragsverarbeiter personenbezogener Daten sind – soweit eingesetzt – nur:

| Empfänger | Zweck | Ort/Drittland | Grundlage |
|---|---|---|---|
| Cloudflare ⟦PLATZHALTER: Vertragsgesellschaft⟧ | CDN, Schutz, Tunnel, DNS; ab Bezahlstart ggf. Objektspeicher (R2) | USA/weltweit | Auftragsverarbeitungsvertrag, DPF/SCC |
| E-Mail-Dienstleister ⟦PLATZHALTER: SMTP-Anbieter, Sitz, Land⟧ | Versand der Konto-E-Mails | ⟦PLATZHALTER: Land des Anbieters⟧ | Auftragsverarbeitungsvertrag ⟦PLATZHALTER: abgeschlossen?⟧ |
| Stripe ⟦PLATZHALTER: Vertragsgesellschaft⟧ (erst ab Bezahlstart) | Zahlungsabwicklung für das Abo „Simple Mods Pro“ (Checkout, Kundenportal, Rechnungen), optional Steuerberechnung (Stripe Tax) | Irland/USA | siehe Abschnitt 9.2 |
| Hosting-Anbieter | Der Dienst läuft auf **eigenem Server des Betreibers** in Deutschland ⟦PLATZHALTER: Standort bestätigen⟧; kein externer Hoster | Deutschland | — |
| Backup-Ziel ⟦PLATZHALTER: eigenes Gerät oder externer Anbieter?⟧ | Datensicherung | ⟦PLATZHALTER: Ort des Backups⟧ | ⟦PLATZHALTER: Grundlage, falls extern⟧ |

Eine Weitergabe an sonstige Dritte erfolgt nur, wenn wir dazu gesetzlich verpflichtet sind (z. B. behördliche Anordnung, Art. 6 Abs. 1
lit. c DSGVO) oder Sie eingewilligt haben. Eine Liste der eingesetzten Dienstleister mit Einzelheiten führen wir intern ⟦PLATZHALTER: ggf. veröffentlichen, z. B. unter …/dienstleister⟧.

## 11. Speicherdauer, Export und Löschung

Wir speichern Daten nur so lange, wie es für den Zweck nötig ist (siehe Tabelle in Abschnitt 2 und die Abschnitte 3 bis 9). Zusammengefasst:

- **Konto und Karten:** bis zur Löschung des Kontos. Unter `/app/account` können Sie Ihre Daten als **JSON exportieren** und Ihr
  **Konto löschen**. Bei der Löschung werden Konto, Sitzungen, Karten (einschließlich der Karten im Puffer) und Freigaben gelöscht
  ⟦PLATZHALTER: Löschung sofort oder mit Frist; Löschung in Backups nach Ablauf der Backup-Aufbewahrung – gegen die Implementierung prüfen⟧.
- **Puffer:** getrennte Karten bleiben Free 30 Tage, Pro 90 Tage, Max 180 Tage, Ultra 1 Jahr gespeichert, danach Löschung.
- **Sitzung:** höchstens 30 Tage. **Signup Tag:** 30 Tage. **Ratenbegrenzung:** höchstens 1 Stunde (nur Arbeitsspeicher).
- **Server-Logs:** ⟦PLATZHALTER: Aufbewahrung, Vorschlag 7 Tage⟧.
- **Meldungen zu rechtswidrigen Inhalten und Schriftverkehr:** ⟦PLATZHALTER: Dauer, z. B. bis zur Erledigung plus Verjährungsfrist⟧.
- **Rechnungsdaten (nach Bezahlstart):** gesetzliche Aufbewahrungsfristen.
- **Bestellungen, Zustimmungen, Abos, Freischaltungen, Stripe-Ereignisse, Kündigungen (nach Bezahlstart):** siehe Abschnitt 9.1 und 9.3.
- **Backups:** ⟦PLATZHALTER: Intervall und Aufbewahrung⟧.

## 12. Ihre Rechte

Sie haben nach der DSGVO folgende Rechte:

- **Auskunft** (Art. 15), **Berichtigung** (Art. 16), **Löschung** (Art. 17), **Einschränkung der Verarbeitung** (Art. 18),
- **Datenübertragbarkeit** (Art. 20) – den JSON-Export finden Sie in `/app/account` ⟦PLATZHALTER: prüfen, ob der Export auch Bestellungen, Abo und Freischaltungen enthält⟧,
- **Widerspruch** (Art. 21): Soweit wir Daten auf Grundlage berechtigter Interessen (Art. 6 Abs. 1 lit. f) verarbeiten, können Sie
  aus Gründen, die sich aus Ihrer besonderen Situation ergeben, jederzeit widersprechen. Wir verarbeiten die Daten dann nicht
  mehr, es sei denn, wir weisen zwingende schutzwürdige Gründe nach oder die Verarbeitung dient der Geltendmachung,
  Ausübung oder Verteidigung von Rechtsansprüchen.
- **Widerruf einer Einwilligung** (Art. 7 Abs. 3) mit Wirkung für die Zukunft, soweit die Verarbeitung auf einer Einwilligung beruht
  (derzeit holen wir keine Einwilligungen ein ⟦PLATZHALTER: bei Einführung von „Explore“ ergänzen⟧).

Zur Ausübung genügt eine Nachricht an die Adresse in Abschnitt 1. Wir können Sie zur Identifizierung um einen Nachweis bitten.

**Beschwerderecht:** Sie haben das Recht, sich bei einer Datenschutz-Aufsichtsbehörde zu beschweren (Art. 77 DSGVO). Für uns
zuständig ist ⟦PLATZHALTER: Landesbeauftragte/r für Datenschutz des Bundeslandes, in dem der Betreiber seinen Sitz hat, mit Anschrift⟧.
Sie können sich auch an die Aufsichtsbehörde an Ihrem Wohnort wenden.

## 13. Automatisierte Entscheidungen

Eine ausschließlich automatisierte Entscheidungsfindung einschließlich Profiling im Sinne des Art. 22 DSGVO findet nicht statt.
Technische Begrenzungen (Ratenbegrenzung, Stufen-Limits, Mindestzeit der Karten-Vorbereitung) gelten für alle Nutzer gleich;
auffällige Muster werden in einer Verwaltungsliste gesammelt und von uns manuell geprüft.
Die Freischaltung und Herabstufung von Abo-Vorzügen nach dem Zahlungs- bzw. Abo-Status (Abschnitt 9.1) erfolgt automatisch. Sie setzt nur
die vereinbarten Vertragsbedingungen um; soweit sie überhaupt unter Art. 22 DSGVO fällt, ist sie für die Erfüllung des Abo-Vertrags erforderlich
(Art. 22 Abs. 2 lit. a DSGVO, nach bestem Wissen). Bei Fragen oder Einwänden können Sie sich jederzeit an uns wenden.
⟦PLATZHALTER: rechtlich prüfen lassen, ob die automatisierten Sperren bei fehlgeschlagenen Anmeldungen unter Art. 22 fallen (nach unserer Einschätzung nein)⟧

## 14. Datensicherheit

Die Verbindung zu unseren Seiten ist per TLS verschlüsselt. Passwörter speichern wir nur als Hash (PBKDF2-SHA256, 600 000
Runden, mit Salz), Sitzungs- und Einmal-Token nur als Hash; Sitzungs-Cookies sind als HttpOnly und Secure gesetzt; Formulare sind
gegen CSRF-Angriffe gesichert; Anmeldeversuche werden begrenzt. Die Schnittstelle für die Mod akzeptiert nur einen Karten-Schlüssel (keine Cookies). Weitere technische und organisatorische Maßnahmen: ⟦PLATZHALTER: Zugriffsschutz auf den Heimserver, Festplattenverschlüsselung, Backups, Update-Prozess – TOM-Dokument (Art. 32 DSGVO) erstellen⟧.

## 15. Änderungen dieser Erklärung

Wir passen diese Erklärung an, wenn sich der Dienst oder die Rechtslage ändert. Es gilt die jeweils aktuelle Fassung unter
⟦PLATZHALTER: URL der Datenschutzerklärung, z. B. https://livemap.simplemods.de/datenschutz⟧.
Stand: ⟦PLATZHALTER: Datum der Veröffentlichung⟧.

---

ENTWURF – keine Rechtsberatung. Vor Veröffentlichung prüfen lassen.
