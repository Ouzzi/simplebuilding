# Auftragsverarbeiter-Liste und Auftragsverarbeitungsvertrag für Kartenbesitzer (ENTWURF)

> **ENTWURF – nicht ausliefern.** Stand 12.11.2026. Keine Rechtsberatung. Vor Veröffentlichung von einer Anwältin,
> einem Anwalt (Datenschutzrecht) prüfen lassen.
> Absätze mit „**[Anmerkung Entwurf]**“ sind Hinweise an den Besitzer und werden vor der Veröffentlichung entfernt.
> Alles in ⟦PLATZHALTER: …⟧ muss der Besitzer eintragen oder entscheiden.
>
> **Teil A** ist ein internes Arbeitsdokument des Betreibers (Verzeichnis der Dienstleister, Art. 28/30/44 ff. DSGVO) und eine Grundlage für die
> Empfängerliste der Datenschutzerklärung. **Teil B** ist ein Vorlagen-Gerüst für den Vertrag, den der Dienst Kartenbesitzern anbietet, wenn
> Spielernamen und -positionen angezeigt werden.

---

# Teil A – Eingesetzte Dienstleister und Auftragsverarbeiter

Stand: 12.11.2026, Dienst im Aufbau (Phase 1; Bezahlen „coming soon“). **Status** zeigt, was der Besitzer noch abschließen muss.

## A.1 Übersicht

| Nr. | Dienstleister | Zweck | Daten | Ort / Drittland | Rechtsgrundlage Drittland | Vertrag / DPA | Status |
|---|---|---|---|---|---|---|---|
| 1 | **Cloudflare** ⟦PLATZHALTER: Vertragspartner, i. d. R. Cloudflare, Inc., San Francisco, USA⟧ | CDN, Reverse Proxy, Cloudflare Tunnel (Erreichbarkeit des Heimservers ohne offenen Port), DNS, Schutz vor Angriffen, Cache der Kacheln | IP-Adresse der Besucher, Anfrage-Metadaten, **gesamter Datenverkehr einschließlich Anmeldedaten und Karteninhalte** (TLS endet bei Cloudflare), zwischengespeicherte Kacheln | USA und weltweites Netz | EU-US Data Privacy Framework (Beschluss (EU) 2023/1795), soweit zertifiziert; Standardvertragsklauseln (Beschluss (EU) 2021/914) im Cloudflare-DPA ⟦PLATZHALTER: prüfen⟧ | Cloudflare Data Processing Addendum ⟦PLATZHALTER: abgeschlossen? Datum; ggf. im Dashboard bestätigen⟧ | **offen** |
| 2 | **Stripe** ⟦PLATZHALTER: Stripe Payments Europe, Ltd., Irland; ggf. Stripe, Inc., USA⟧ | Zahlungsabwicklung für das Konto-Abo „Simple Mods Pro“ (Basic/Pro/Ultra): Checkout, Kundenportal, Abo-Zeitplan beim Jahresabo, Rechnungen, Webhooks; optional Steuerberechnung (Stripe Tax ⟦PLATZHALTER: ja/nein⟧) – **erst ab Bezahlstart** | Name, E-Mail, Konto-Kennung, Rechnungsadresse (bei Stripe Tax auch Land/USt-IdNr.), Abo-Stufe/Laufzeit, Abo- und Zahlungsstatus, Rechnungen; Zahlungsmittel-Daten liegen nur bei Stripe | Irland/USA | Stripe-Angaben zu DPF/SCC prüfen | Stripe Data Processing Agreement; Stripe handelt teils als **eigener Verantwortlicher** (z. B. Betrugsprävention, gesetzliche Pflichten) und teils als Auftragsverarbeiter ⟦PLATZHALTER: Rollen anwaltlich bestätigen⟧ | **nicht aktiv; vor Bezahlstart** |
| 3 | **E-Mail-/SMTP-Anbieter** ⟦PLATZHALTER: Anbieter noch zu wählen; EU-Sitz bevorzugt⟧ | Versand von Konto-E-Mails (Bestätigung, Passwort zurücksetzen, später Kündigungs-/Rechnungsmails) | E-Mail-Adresse, Anzeigename, Inhalt der Mail einschließlich Einmal-Link, Versandzeit | ⟦PLATZHALTER: Sitz/Land des Anbieters⟧ | ⟦PLATZHALTER: nur nötig, falls Drittland⟧ | Auftragsverarbeitungsvertrag des Anbieters ⟦PLATZHALTER: abgeschlossen? Datum⟧ | **offen (Anbieter fehlt)** |
| 4 | **Hosting – eigener Server** des Betreibers (Proxmox-Container, zu Hause) | Betrieb von Dienst, Datenbank (SQLite), Kachelspeicher, nginx | alle Daten des Dienstes | ⟦PLATZHALTER: Standort/Land bestätigen⟧ | — | **kein Auftragsverarbeiter** (Betreiber selbst); Internetanbieter transportiert nur Daten (TOMs siehe A.3) | TOM-Dokument offen |
| 5 | **Datensicherung (Backup)** ⟦PLATZHALTER: eigenes Gerät oder externer Anbieter? Falls extern: Name⟧ | Wiederherstellung nach Ausfall | Datenbank, ggf. Kacheln | ⟦PLATZHALTER: Ort des Backups⟧ | ⟦PLATZHALTER: Drittland-Grundlage, falls extern⟧ | ⟦PLATZHALTER: AV-Vertrag, falls extern⟧ | **offen** |
| 6 | **Cloudflare R2** (Objektspeicher; **geplant** vor Bezahlstart, Spezifikation Abschnitt 7) | Speicherung der Kacheln statt auf dem Heimserver | Kartenkacheln (PNG), ggf. Metadaten | ⟦PLATZHALTER: Jurisdiktion „EU“ wählen und prüfen⟧ | wie Nr. 1 | gleiches DPA wie Nr. 1 | **geplant; Eintrag erst bei Einführung gültig** |
| 7 | **Domain-Registrar** ⟦PLATZHALTER: hosting.de laut Betreiberangabe bestätigen⟧ | Registrierung von simplemods.de / simplewiki.de | Inhaberdaten des Betreibers; **keine Daten der Nutzer** | ⟦PLATZHALTER: Sitz des Registrars⟧ | — | kein Zugriff auf Nutzerdaten; kein AV nötig, prüfen | in Ordnung, bestätigen |
| 8 | Weitere (Monitoring, Fehlerberichte, Analyse, E-Mail-Posteingang für Support, Ticket-System) | — | — | — | — | ⟦PLATZHALTER: derzeit keine weiteren eingesetzt – bestätigen; jede spätere Ergänzung hier und in der Datenschutzerklärung nachtragen⟧ | offen |

## A.2 Prüf-Checkliste je Dienstleister (Art. 28 Abs. 1 DSGVO)

Für jeden Eintrag mit Zugriff auf personenbezogene Daten abhaken:

- [ ] Vertrag/DPA nach Art. 28 Abs. 3 DSGVO abgeschlossen (Datum, Fundstelle ablegen)
- [ ] Weisungsbindung, Vertraulichkeit, technische und organisatorische Maßnahmen (TOMs) geprüft
- [ ] Unterauftragsverarbeiter bekannt und zulässig
- [ ] Drittlandübermittlung: Grundlage (DPF, SCC, Angemessenheitsbeschluss) festgehalten, Transfer-Folgenabschätzung bei SCC geprüft
- [ ] Eintrag im Verzeichnis von Verarbeitungstätigkeiten (Art. 30 DSGVO)
- [ ] Empfänger in der Datenschutzerklärung genannt (Abschnitt 10)
- [ ] Löschung/Rückgabe bei Vertragsende geregelt

## A.3 Technische und organisatorische Maßnahmen des Betreibers (Gerüst, Art. 32 DSGVO)

Als Einsatz auf eigenem Server beschreibt der Betreiber hier die TOMs (wird für Teil B Anlage 2 wiederverwendet):

| Bereich | Maßnahme (nach Plan/Technik) | Ergänzen |
|---|---|---|
| Transportverschlüsselung | TLS zwischen Browser und Cloudflare; Cloudflare Tunnel zum Server | ⟦PLATZHALTER: Verschlüsselung zwischen Cloudflare und Origin bestätigen⟧ |
| Zugangsdaten | Passwörter als PBKDF2-SHA256-Hash (600 000 Runden, Salz); Sitzungs- und Einmal-Token nur gehasht | — |
| Sitzungen | HttpOnly, Secure, SameSite=Lax; 30 Tage; Widerruf bei Passwortwechsel | — |
| Missbrauchsschutz | Ratenbegrenzungen (nur im Arbeitsspeicher), CSRF-Schutz, Origin-Prüfung | — |
| Schnittstelle der Mod | Bearer-Schlüssel je Karte, Größen- und Formatprüfung, keine Cookies | — |
| Webhook (später) | Signaturprüfung (HMAC-SHA256, Toleranz 300 s) | — |
| Geheimnisse | Nur in Server-Konfiguration außerhalb des Repos (Dateirechte 600) | — |
| Zugriff auf den Server | ⟦PLATZHALTER: Wer hat Zugriff? SSH-Schlüssel, Administratorenrechte, 2FA⟧ | offen |
| Physischer Schutz | ⟦PLATZHALTER: Heimserver – Raum, Zugang, Strom (USV), Festplattenverschlüsselung⟧ | offen |
| Datensicherung | ⟦PLATZHALTER: Intervall, Ort, Aufbewahrung, Wiederherstellungstest⟧ | offen |
| Updates / Patches | ⟦PLATZHALTER: Update-Prozess⟧ | offen |
| Datenpannen | ⟦PLATZHALTER: Meldeprozess nach Art. 33/34 DSGVO (72 Stunden)⟧ | offen |
| Löschkonzept | Sitzungen 30 Tage; Signup Tag 30 Tage; Ratenlimits ≤ 1 h (RAM); Puffer nach Stufe; Logs ⟦PLATZHALTER: Frist, Vorschlag 7 Tage⟧ | Jobs im Dienst prüfen |

---

# Teil B – Auftragsverarbeitungsvertrag (Art. 28 DSGVO) für Kartenbesitzer

## B.1 Wann ein Vertrag nötig ist – Rollen

- Wer **„Spieler zeigen“** einschaltet, lässt den Dienst **Spielername, Dimension und x/z-Position** der Online-Spieler seines
  Minecraft-Servers speichern und anzeigen. Der Kartenbesitzer (Server-Betreiber) bestimmt **Zweck und Mittel** → er ist **Verantwortlicher** (Art. 4 Nr. 7 DSGVO).
- Der Dienst verarbeitet diese Daten **nur nach seiner Weisung** → **Auftragsverarbeiter** (Art. 28 DSGVO). Für eigene Zwecke (Konto,
  Sicherheit, Abrechnung) ist der Betreiber des Dienstes selbst Verantwortlicher; der Vertrag betrifft nur die Spieler- und Karteninhalts-Daten.
- Der Vertrag gilt auch für **Kartenkacheln**, soweit sie personenbezogene Angaben Dritter zeigen (z. B. Namen auf Schildern); Hauptfall ist aber die Live-Position.
- **Privatpersonen:** Betreibt jemand einen reinen Freundes-Server ausschließlich für persönliche oder familiäre Tätigkeiten, kann die
  Haushaltsausnahme (Art. 2 Abs. 2 lit. c DSGVO) greifen; sie entfällt regelmäßig bei öffentlich zugänglichen Karten und Servern.
  ⟦PLATZHALTER: Hinweistext für Kartenbesitzer anwaltlich prüfen lassen⟧
- Standard ist „Spieler zeigen“ **aus**; ohne Einschalten werden keine Spielerangaben übernommen.

> **[Anmerkung Entwurf]** Rollenabgrenzung (Auftragsverarbeitung gegenüber gemeinsamer Verantwortlichkeit nach Art. 26 DSGVO, z. B. bei öffentlichen
> Karten, bei denen wir die Sichtbarkeit technisch bereitstellen) vom Anwalt bestätigen lassen.

## B.2 Vorschlag zur technischen Umsetzung im Dienst

Das ist **nicht Teil der Spezifikation** und muss im Dienst ergänzt werden (siehe README, Umsetzungshinweise):

1. Beim Einschalten von „Spieler zeigen“ im Einstellungsfenster einer Karte: Kästchen „Ich habe den Auftragsverarbeitungsvertrag gelesen und nehme ihn an; ich habe meine Spieler informiert.“
2. Speichern von Annahme-Zeitpunkt, Vertragsversion und Konto; Vertrag als PDF/Text im Konto abrufbar.
3. Ohne Annahme bleibt „Spieler zeigen“ gesperrt (auch serverseitig: Live-Daten ohne Annahme verwerfen).
4. Beim Ausschalten oder Trennen: gespeicherten Live-Stand löschen ⟦PLATZHALTER: gegen Implementierung prüfen⟧.

## B.3 Gliederung des Vertrags

**Vertrag über die Verarbeitung personenbezogener Daten im Auftrag (Art. 28 DSGVO)**
zwischen ⟦PLATZHALTER: Name und Anschrift des Kartenbesitzers (aus dem Konto übernommen)⟧ („Verantwortlicher“, „Kartenbesitzer“) und
⟦PLATZHALTER: Name und Anschrift des Betreibers⟧ („Auftragsverarbeiter“, „Betreiber“).

### § 1 Gegenstand und Dauer
Der Betreiber speichert und zeigt für den Kartenbesitzer Spielername, Dimension und x/z-Position der Spieler des Servers des Kartenbesitzers auf der von ihm
verbundenen Karte (nur wenn „Spieler zeigen“ aktiv ist) und verarbeitet ggf. personenbezogene Angaben in Kartenkacheln. Dauer: bis zur Beendigung des
Nutzungsvertrags bzw. bis zum Ausschalten von „Spieler zeigen“ oder dem Trennen der Karte.

### § 2 Art und Zweck der Verarbeitung; Art der Daten; Betroffene
- Art: Entgegennahme von der Mod, Speichern des **letzten Standes** (kein Verlauf), Auslieferung an Betrachter entsprechend der Sichtbarkeit der Karte (privat/Link/öffentlich), Zwischenspeicherung (CDN).
- Zweck: Anzeige der Online-Spieler auf der Karte im Auftrag und nach Weisung des Kartenbesitzers.
- Datenarten: Minecraft-Spielername, Dimension, x/z-Position, Zeitpunkt; ggf. in Kachelbildern erkennbare Beschriftungen.
- Betroffene: Spielerinnen und Spieler und Mitwirkende auf dem Server des Kartenbesitzers.
- Besondere Kategorien (Art. 9 DSGVO): keine vorgesehen. ⟦PLATZHALTER: bestätigen⟧

### § 3 Weisungen
Verarbeitung nur auf dokumentierte Weisung des Kartenbesitzers. Weisungen sind die Einstellungen im Dashboard (Sichtbarkeit, „Spieler zeigen“, Löschen/Trennen) und
Texte in Textform an ⟦PLATZHALTER: Kontakt⟧. Der Betreiber weist auf Weisungen hin, die er für rechtswidrig hält (Art. 28 Abs. 3 Satz 3).

### § 4 Vertraulichkeit
Personen, die Zugriff haben, sind zur Vertraulichkeit verpflichtet. ⟦PLATZHALTER: wer außer dem Betreiber Zugriff hat⟧

### § 5 Technische und organisatorische Maßnahmen
Maßnahmen nach Anlage 2 (Art. 32 DSGVO).

### § 6 Unterauftragsverarbeiter
Genehmigt sind die in Anlage 3 genannten (derzeit: Cloudflare als CDN/Tunnel ⟦PLATZHALTER: weitere ergänzen⟧). Änderungen kündigt der Betreiber mindestens
⟦PLATZHALTER: z. B. 30 Tage⟧ vorher an; der Kartenbesitzer kann widersprechen oder den Dienst kündigen. Der Betreiber legt Unterauftragsverarbeitern gleichwertige Pflichten auf.

### § 7 Unterstützung des Verantwortlichen
Der Betreiber unterstützt bei Anfragen Betroffener (Art. 12 ff.), bei der Sicherheit (Art. 32), Datenpannen (Art. 33/34), Datenschutz-Folgenabschätzung (Art. 35/36) in
angemessenem Umfang. Anfragen Betroffener leitet er unverzüglich an den Kartenbesitzer weiter. Eine Datenpanne meldet er unverzüglich, ⟦PLATZHALTER: spätestens binnen 48 Stunden⟧.

### § 8 Löschung und Rückgabe
Nach Beendigung oder auf Weisung löscht der Betreiber die Live-Daten und – soweit sie personenbezogen sind – die Kachelinhalte; dabei gelten die **Puffer-Fristen** der Stufe
für getrennte Karten (Free 30 Tage, Pro 90, Max 180, Ultra 1 Jahr) ⟦PLATZHALTER: ob Live-Stand sofort gelöscht wird, mit Implementierung abgleichen⟧. Der Kartenbesitzer kann Daten vorher selbst löschen
und über den JSON-Export abrufen. Gesetzliche Aufbewahrungspflichten bleiben unberührt.

### § 9 Nachweise und Kontrollen
Der Betreiber stellt auf Anforderung die nötigen Informationen zum Nachweis bereit und ermöglicht Überprüfungen in angemessenem Rahmen ⟦PLATZHALTER: z. B. schriftliche Auskunft statt Vor-Ort-Audit⟧.

### § 10 Haftung
Nach Art. 82 DSGVO und ⟦PLATZHALTER: Haftungsbegrenzung aus den AGB, Abschnitt 14, prüfen⟧.

### § 11 Schlussbestimmungen
Laufzeit parallel zum Nutzungsvertrag; Kündigung nur mit diesem; Änderungen nur in Textform; Vorrang dieses Vertrags bei Widersprüchen zu den AGB in Datenschutzfragen; deutsches Recht; Salvatorische Klausel.

### Anlage 1 – Beschreibung der Verarbeitung
Wie § 2, ergänzt um Speicherort (⟦PLATZHALTER: Deutschland / Cloudflare⟧), Löschzeitpunkte, Zugriffsberechtigte.

### Anlage 2 – Technische und organisatorische Maßnahmen
Inhalt aus Teil A, Abschnitt A.3 (nach Ausfüllen der offenen Zeilen).

### Anlage 3 – Unterauftragsverarbeiter
| Name | Sitz | Leistung | Daten | Drittland-Grundlage |
|---|---|---|---|---|
| Cloudflare ⟦PLATZHALTER: Vertragsgesellschaft⟧ | USA/weltweit | CDN, Tunnel | siehe A.1 Nr. 1 | DPF/SCC ⟦PLATZHALTER: Grundlage aus A.1 Nr. 1 übernehmen⟧ |
| ⟦PLATZHALTER: weitere⟧ | | | | |

### Anlage 4 – Ansprechpartner
Datenschutz-Kontakt des Betreibers: ⟦PLATZHALTER: E-Mail/Anschrift Datenschutz-Kontakt⟧; Ansprechpartner des Kartenbesitzers: im Konto hinterlegte E-Mail.

## B.4 Mustertext für Kartenbesitzer: Information der Spieler (Art. 13 DSGVO)

> **[Anmerkung Entwurf]** Dieser Baustein ist nur ein Muster für Kartenbesitzer, die ihre Spieler informieren müssen. Er ersetzt keine Rechtsberatung.
> Der Kartenbesitzer ist dafür verantwortlich; wir bieten ihn nur zur Erleichterung an.

„Auf unserer Server-Karte unter ⟦PLATZHALTER: Adresse der Karte⟧ werden – solange Sie online sind – Ihr **Minecraft-Spielername**, die **Dimension** und Ihre
**x/z-Position** angezeigt. Nur der aktuelle Stand wird gespeichert, kein Verlauf. Die Karte ist ⟦PLATZHALTER: privat / per Link / öffentlich⟧ sichtbar. Verantwortlich:
⟦PLATZHALTER: Name und Kontakt des Server-Betreibers⟧. Rechtsgrundlage: ⟦PLATZHALTER: berechtigtes Interesse, Art. 6 Abs. 1 lit. f DSGVO / Einwilligung, Art. 6 Abs. 1 lit. a DSGVO – Kartenbesitzer wählt⟧.
Die Karte wird bei Simple Livemap (⟦PLATZHALTER: Name des Betreibers⟧, livemap.simplemods.de) als Auftragsverarbeiter gehostet; dazu wird Cloudflare als CDN eingesetzt (USA).
Sie haben Rechte auf Auskunft, Löschung, Widerspruch u. a.; wenden Sie sich an ⟦PLATZHALTER: Kontakt des Server-Betreibers⟧. Auf Wunsch blenden wir Sie aus: ⟦PLATZHALTER: Funktion der Mod zum Ausblenden einzelner Spieler⟧.“

## B.5 Offene Punkte für den Besitzer

- Rollenabgrenzung und Vertragsinhalt anwaltlich prüfen lassen (auch Haushaltsausnahme-Hinweis).
- Entscheiden, ob der Vertrag online per Klick angenommen wird (Empfehlung, siehe B.2) oder zusätzlich in Textform/Unterschrift.
- Unterauftragsverarbeiter (Anlage 3) mit Teil A abgleichen; TOMs vervollständigen.
- Ob die Funktion „Spieler zeigen“ zum Start der Free-Stufe freigeschaltet wird (Konzept-Vorlauf sah „nur bezahlte Stufe“ vor; die Spezifikation und der Plan: für alle Stufen, Standard aus).

---

ENTWURF – keine Rechtsberatung. Vor Veröffentlichung prüfen lassen.
