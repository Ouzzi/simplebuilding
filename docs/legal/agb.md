# Nutzungsbedingungen (AGB) für Simple Livemap – gehosteter Dienst (ENTWURF)

> **ENTWURF – nicht ausliefern.** Stand 12.11.2026. Keine Rechtsberatung. Vor Veröffentlichung von einer Anwältin,
> einem Anwalt (IT-Recht, Verbraucherrecht) oder einem geprüften Generator prüfen lassen.
> Absätze mit „**[Anmerkung Entwurf]**“ sind Hinweise an den Besitzer und werden vor der Veröffentlichung entfernt.
> Alles in ⟦PLATZHALTER: …⟧ muss der Besitzer eintragen oder entscheiden. „Variante“ = der Besitzer wählt eine aus.
>
> Grundlage: Spezifikation LIVEMAP34 (alle 8 Abschnitte abgenommen, 12.11.2026), `tools/livemap-web/tiers.json`,
> `docs/ai/PLAN-A-LIVEMAP34-2026-11-12.md`. **Bezahlen ist derzeit „coming soon“.** Die Abschnitte zu Preisen, Laufzeit,
> Kündigung und Widerruf (9 bis 12, Anlagen 1 bis 3) werden erst mit dem Bezahlstart wirksam; die übrigen gelten schon für die
> kostenlose Stufe Free.

---

## 1. Anbieter, Geltungsbereich, Vertragsgegenstand

1.1 Anbieter des Dienstes „Simple Livemap“ (`livemap.simplemods.de`) und des zentralen Kontos (`account.simplemods.de`) ist
⟦PLATZHALTER: Name/Firma, Anschrift und E-Mail laut Impressum⟧ („Betreiber“, „wir“).

1.2 Diese Bedingungen gelten für die Nutzung des gehosteten Dienstes und des Kontos durch Nutzerinnen und Nutzer („Sie“). Sie
gelten gegenüber **Verbrauchern** (§ 13 BGB) und **Unternehmern** (§ 14 BGB). Regelungen, die ausdrücklich nur für Verbraucher
gelten (insbesondere der Kündigungsbutton nach Abschnitt 11.4, der Widerruf nach Abschnitt 12 und die Anlagen 1 bis 3), gelten gegenüber Unternehmern nicht.

1.3 **Gegenstand.** Der Dienst nimmt von der Minecraft-Mod „Simple Livemap“ Kartenkacheln und – wenn Sie das einschalten –
Live-Daten entgegen, speichert sie und zeigt die Karte in einem Web-Viewer an. Die **Mod selbst** und der **Self-Host-Betrieb**
(die Mod startet ihren eigenen Webserver, ohne Konto, ohne unseren Dienst) sind **nicht** Gegenstand dieser Bedingungen;
dafür gilt die Lizenz der Mod ⟦PLATZHALTER: Lizenz der Mod benennen und verlinken⟧.

1.4 Eigene Geschäftsbedingungen von Nutzern gelten nicht, auch wenn wir ihnen nicht ausdrücklich widersprechen.

1.5 **Mindestalter.** Ein Konto können Personen ab ⟦PLATZHALTER: Mindestalter, Vorschlag 16 oder 18 Jahre – Jugendschutz und Geschäftsfähigkeit prüfen lassen⟧ anlegen.
Kostenpflichtige Stufen können nur unbeschränkt Geschäftsfähige oder Minderjährige mit Zustimmung ihrer gesetzlichen Vertreter buchen
(§§ 107 ff. BGB).

1.6 Der Dienst wird in deutscher Sprache angeboten; die Vertragssprache ist Deutsch. ⟦PLATZHALTER: ggf. englische Übersetzung als unverbindliche Lesehilfe⟧

## 2. Konto und Registrierung

2.1 Für die Nutzung ist ein Konto erforderlich. Sie registrieren sich mit E-Mail-Adresse und Passwort und bestätigen die
E-Mail-Adresse über einen Link. Mit der Bestätigung kommt der unentgeltliche Nutzungsvertrag über die Stufe „Free“ zustande. Bei der Registrierung weisen wir ausdrücklich auf diese
Bedingungen hin, Sie können sie vor dem Absenden abrufen und speichern, und Sie bestätigen per Kästchen, dass Sie sie akzeptieren
⟦PLATZHALTER: im Registrierformular umsetzen; Version und Zeitpunkt der Zustimmung speichern – siehe README⟧.

2.2 Sie müssen wahrheitsgemäße Angaben machen und Ihre Zugangsdaten geheim halten. Bei Verdacht auf Missbrauch müssen Sie uns
unverzüglich informieren und das Passwort ändern (Funktion „überall abmelden“).

2.3 **Ein Free-Konto je E-Mail-Adresse.** Mehrfach-Konten zur Umgehung der Limits (insbesondere der Limits der Stufe Free) sind
unzulässig. Auffällige Muster (z. B. viele Konten, dauerndes Umhängen von Karten) können wir prüfen und, soweit nötig,
Konten sperren oder zusammenführen (Abschnitt 6.5).

2.4 Rollen: „Nutzer“ und „Entwickler“. Die Entwickler-Rolle ist dem Betreiber vorbehalten.

## 3. Leistungen und Stufen

3.1 Der Dienst wird in vier Stufen angeboten. Die Stufe bestimmt Zahl und Größe der Karten sowie Aktualisierungstakt und
Priorität. Es gelten folgende Werte (Stand dieses Entwurfs):

| | **Free** | **Pro** | **Max** | **Ultra** |
|---|---|---|---|---|
| Karten gleichzeitig | 2 (davon höchstens 1 öffentlich) | 4 (davon höchstens 3 öffentlich) | 6 (davon höchstens 5 öffentlich) | 13 (davon höchstens 10 öffentlich) |
| Puffer (getrennte Karten gespeichert) | 1 Karte · 30 Tage | 10 Karten · 90 Tage | 20 Karten · 180 Tage | 50 Karten · 1 Jahr |
| Aufrufe pro Tag (alle Karten zusammen) | 300 | 3 000 | 15 000 | fair use (≈ 100 000) |
| Spieler-Live-Takt / Kachel-Aktualisierung | 10 s / 15 min | 5 s / 5 min | 2 s / 2 min | 1 s / 1 min |
| Vorbereitung (Mindestzeit, siehe 4.3) | 6 h, niedrige Priorität | ca. 1 h, Vorrang | ca. 20 min | ca. 5 min, höchste Priorität |
| Karten-Platz neu belegen | 1× pro 7 Tage | frei | frei | frei |
| Teilen mit anderen Konten | 2 | 10 | 50 | unbegrenzt |
| Speicher je Karte | 64 MB | 256 MB | 1 GB | 4 GB |
| Preis pro Monat | 0 € | 3,99 € | 7,99 € | 14,99 € |
| Preis bei Jahreszahlung (12 Monate zum Preis von 10; danach monatlich zum Monatspreis, siehe 11.3) | – | 39,90 € | 79,90 € | 149,90 € |

> **[Anmerkung Entwurf]** Die Zahlen stammen aus der abgenommenen Spezifikation bzw. `tiers.json`; Preise sind dort ausdrücklich „Vorschlag“.
> Der Speicher je Karte (D17 im Plan) steht in der Spezifikation nur als „begrenzt“ – Werte vom Besitzer bestätigen.
> Preise müssen im Shop einschließlich Umsatzsteuer als Endpreise ausgewiesen werden (PAngV); bei Kleinunternehmer-Status (§ 19 UStG) mit entsprechendem Hinweis.
> Tabelle und Zahlen sollten im Dienst aus derselben Quelle (`tiers.json`) erzeugt werden, damit AGB, Preisseite und Technik nicht auseinanderlaufen.

3.2 **Zusatzleistungen** (eigener Kartenname in der Adresse ab Pro; Einbetten per iframe und Verlauf/Schnappschüsse 30 Tage ab Max;
eigene Domain, Betrieb ohne Branding, Programmierschnittstelle und Schnappschüsse 1 Jahr ab Ultra) sind ⟦PLATZHALTER: erst Vertragsbestandteil, wenn sie im Dienst freigeschaltet sind; bis dahin hier streichen oder als „geplant, nicht Teil des Leistungsumfangs“ kennzeichnen⟧.

> **[Anmerkung Entwurf]** Laut Spezifikation (Abschnitt 8, „Später“) gehören eigene Domain, Einbetten, API und Schnappschüsse nicht zur ersten Phase.
> Wer die Stufen verkauft, schuldet, was in der Leistungsbeschreibung steht (§ 327e BGB). Vor dem Bezahlstart: bauen oder aus Beschreibung, Preisseite und Tabelle streichen.

3.3 **Öffentliche Karten.** Karten der Sichtbarkeit „öffentlich“ zählen gegen die Grenze „davon öffentlich“. Karten, die „privat“
oder „per Link“ sind, zählen nicht dagegen. Eine Verzeichnisseite öffentlicher Karten („Explore“) ist derzeit nicht verfügbar; eine
Aufnahme erfolgt nur mit Ihrer Zustimmung.

3.4 **Aufruf-Limit.** Wird das Tageslimit der Aufrufe überschritten, sperren wir die Karte nicht. Sie bleibt sichtbar, zeigt aber
nur zwischengespeicherte Kacheln, aktualisiert Live-Daten langsamer und der Besitzer erhält einen Hinweis, eine höhere Stufe zu erwägen.
Für Ultra gilt eine angemessene Nutzung („fair use“); wir können bei dauerhaft deutlich höherer Last ein Gespräch über eine
Sonderlösung anbieten.

3.5 **Speicher.** Der Speicher je Karte und der Gesamtspeicher des Dienstes sind begrenzt. Ist der Speicher knapp, können
Uploads vorübergehend abgelehnt werden. Daraus entsteht kein Anspruch auf Mehrspeicher.

3.6 **Keine Datensicherung Ihrer Welt.** Kartenkacheln sind aus Ihrer Spielwelt abgeleitet. Der Dienst ist **kein Backup** Ihrer
Welt oder Ihres Servers.

3.7 Wir dürfen die Werte der Stufen ändern, wenn die Änderung zumutbar ist und die Voraussetzungen von Abschnitt 16 vorliegen.

## 4. Karte verbinden, trennen, Puffer, Missbrauchsschutz

4.1 **Verbinden.** Sie wählen Name und Sichtbarkeit der Karte und erhalten einen **Einmal-Code** (15 Minuten gültig). Ein
Spielleiter (Operator) führt im Spiel `/simplelivemap connect <Code>` aus. Die Mod lädt danach alle Kacheln erstmals hoch, anschließend
nur noch Änderungen. Dabei übermittelt die Mod Daten nach unserer Datenschutzerklärung.

4.2 **Zuständigkeit.** Wer den Befehl ausführt, erklärt, dazu berechtigt zu sein (Betreiber des Servers bzw. Besitzer der Welt).

4.3 **Vorbereitungszeit.** Nach dem Verbinden zeigt der Dienst „Karte wird vorbereitet“ mit Fortschritt. Die Karte gilt erst als
„fertig“, wenn der Erst-Upload vollständig ist **und** die Mindestzeit der Stufe vergangen ist (Tabelle 3.1). Das dient dem
Missbrauchsschutz und gilt auch für kleine Welten. Bezahlte Stufen haben Vorrang in der Warteschlange und eine höhere
Upload-Rate. **Die Mindestzeit entfällt für die ersten zwei Verbindungen eines Kontos** (Lebenszeit des Kontos, nicht je Platz); es
zählt dann nur die tatsächliche Upload-Dauer. Ab der dritten Verbindung gilt die Mindestzeit der Stufe. Die angegebenen Zeiten sind
Mindestzeiten, keine Garantie für eine Fertigstellung nach genau dieser Zeit; sie hängt von der Größe der Welt, der Auslastung der
Warteschlange und Ihrer Anbindung ab.

4.4 **Trennen und Puffer.** Wenn Sie eine Karte trennen, wandert sie in den **Puffer**: Die Daten bleiben für die in Tabelle 3.1
genannte Zeit gespeichert, danach werden sie gelöscht. Ist die Welt im Puffer (erkannt an einer zufälligen Welt-ID, nicht am Seed), prüft der Server
die gespeicherten Kacheln und lädt nur Abweichungen nach. Auch dann gilt beim Wieder-Verbinden wieder „Karte wird vorbereitet“.
Ist die Zahl der Puffer-Plätze Ihrer Stufe erschöpft, wird die älteste Karte im Puffer gelöscht ⟦PLATZHALTER: Regel gegen Implementierung prüfen (Plan D14: „älteste fällt raus“)⟧.

4.5 **Platz-Wechsel-Sperre (Free).** Ein freier Platz der Stufe Free kann höchstens einmal je 7 Tage neu belegt werden; die erste
Belegung ist frei.

4.6 **Herabstufen.** Wenn Ihre Stufe endet oder sinkt (z. B. durch Kündigung oder Zahlungsausfall), bleiben **überzählige Karten
30 Tage im Puffer**, auch wenn die neue Stufe weniger Puffer-Plätze vorsieht. Überzählige öffentliche Karten werden auf „per Link“ umgestellt.
Welche Karten aktiv bleiben, ergibt sich aus ⟦PLATZHALTER: Regel bestätigen (Plan D15: neueste Karten zuerst in den Puffer) und im Konto anzeigen⟧.
Wir weisen im Konto auf die Folgen hin. Nach Ablauf der 30 Tage werden die überzähligen Karten gelöscht.

4.7 **Missbrauchsschutz.** Wir begrenzen Upload-Größe und -Rate je Stufe. Wir prüfen auffällige Muster in einer Verwaltungsliste und
dürfen angemessene Maßnahmen ergreifen (Abschnitt 6.5).

## 5. Pflichten der Nutzer, Inhalte

5.1 **Verantwortung für Inhalte.** Der Kartenbesitzer ist für alles verantwortlich, was seine Karte zeigt oder ankündigt:
Kartenname, Kartenbilder (Bauwerke, Pixelbilder aus Blöcken, **Beschriftungen auf Schildern**), Spielernamen und sonstige Angaben.
Wir prüfen Inhalte nicht vorab.

5.2 **Verbotene Inhalte.** Unzulässig sind Inhalte, die gegen geltendes Recht oder Rechte Dritter verstoßen, insbesondere
strafbare, volksverhetzende, gewaltverherrlichende, jugendgefährdende oder pornografische Inhalte, Beleidigungen, Bedrohungen,
Inhalte, die Marken-, Urheber- oder Persönlichkeitsrechte verletzen, sowie Schadsoftware oder Täuschungsversuche.

5.3 **Personenbezogene Daten Dritter / Spieler.** Wer „Spieler zeigen“ einschaltet, stellt Name und Position von Spielerinnen und
Spielern dar. Das darf er nur, wenn er dafür eine Rechtsgrundlage hat und die Spieler informiert (Art. 13/14 DSGVO). Für diese Daten
ist er Verantwortlicher; wir sind Auftragsverarbeiter. Vor dem Einschalten ist der Auftragsverarbeitungsvertrag
(abrufbar unter ⟦PLATZHALTER: URL des Auftragsverarbeitungsvertrags⟧) anzunehmen. ⟦PLATZHALTER: Annahme technisch vor dem Einschalten von „Spieler zeigen“ einholen – im Dienst bauen⟧

5.4 **Schnittstelle und Schlüssel.** Der Karten-Schlüssel (in `hosted.json` der Welt) ist geheim zu halten. Nicht zulässig sind
Umgehung von Limits, automatisierte Massen-Abrufe, Überlastung des Dienstes und Zugriffe auf fremde Karten.

5.5 **Minecraft.** Sie müssen die Nutzungsbedingungen von Minecraft (EULA, Mojang/Microsoft) einhalten. ⟦PLATZHALTER: Hinweise zu Mojang-Richtlinien prüfen lassen⟧

5.6 **Freistellung.** Sie stellen uns von Ansprüchen Dritter frei, die auf rechtswidrigen Inhalten Ihrer Karte beruhen, soweit Sie
die Rechtsverletzung zu vertreten haben (§ 276 BGB); gegenüber Verbrauchern nur bei Verschulden.

## 6. Melde- und Abhilfeverfahren, Sperrung

6.1 **Meldung.** Rechtswidrige Inhalte können über den Melde-Link auf der Karte ⟦PLATZHALTER: Link, muss gebaut werden⟧ oder per E-Mail an
⟦PLATZHALTER: Melde-E-Mail⟧ gemeldet werden (Art. 16 der Verordnung (EU) 2022/2065, DSA). Die Meldung soll enthalten: eine
Begründung, warum der Inhalt rechtswidrig ist; die genaue Fundstelle (Adresse der Karte); Name und E-Mail-Adresse der meldenden
Person (außer bei bestimmten Straftaten); die Erklärung, dass die Angaben nach bestem Wissen richtig und vollständig sind.

6.2 **Bearbeitung.** Wir prüfen Meldungen zeitnah und manuell. Wir bestätigen den Eingang und teilen die Entscheidung und die
Rechtsbehelfe mit. Wir setzen für die Bewertung keine automatisierte Entscheidungsfindung ein ⟦PLATZHALTER: bestätigen⟧.

6.3 **Maßnahmen.** Wir können Inhalte entfernen, Karten unsichtbar schalten oder sperren, Uploads ablehnen und Konten warnen,
einschränken oder sperren, wenn ein Verstoß gegen diese Bedingungen oder das Gesetz vorliegt oder naheliegt. Wir wählen die mildeste
geeignete Maßnahme und berücksichtigen Art und Schwere des Verstoßes.

6.4 **Begründung.** Betroffene Nutzer erhalten eine Begründung mit Angabe der Maßnahme, der Tatsachen, der Rechtsgrundlage,
ob automatisierte Mittel genutzt wurden, und der Rechtsbehelfe (Art. 17 DSA). Sie können innerhalb von ⟦PLATZHALTER: Frist, z. B. 14 Tage⟧ per E-Mail
Widerspruch einlegen; wir prüfen erneut durch eine andere Person, soweit möglich ⟦PLATZHALTER: Verfahren festlegen⟧. Gerichtliche Rechtsbehelfe bleiben unberührt.

6.5 **Konten.** Bei schweren oder wiederholten Verstößen können wir das Konto sperren oder außerordentlich kündigen (Abschnitt 11.6).

> **[Anmerkung Entwurf]** Als Dienst, der fremde Inhalte speichert, gilt der Betreiber als Hosting-Dienst nach dem DSA. Kleinst- und Kleinunternehmen sind von einigen
> Pflichten ausgenommen, nicht aber von den Pflichten für alle Vermittlungsdienste (Kontaktstelle, Bedingungen) und Hosting-Dienste (Meldeverfahren, Begründung). Genauen Umfang anwaltlich prüfen lassen.

## 7. Nutzungsrechte an Inhalten

7.1 Sie behalten alle Rechte an Ihren Inhalten. Sie räumen uns das einfache, auf die Dauer des Vertrags beschränkte Recht ein, die
Inhalte zu speichern, zu vervielfältigen, zu verarbeiten und entsprechend der gewählten Sichtbarkeit öffentlich zugänglich zu machen
sowie über Netzwerke (CDN) zwischenzuspeichern, soweit das zur Erbringung des Dienstes nötig ist.

7.2 Die Kartenbilder enthalten Darstellungen aus Minecraft. ⟦PLATZHALTER: Rechtslage zu Spielinhalten in gehosteten Kartenbildern (Mojang-EULA/Commercial Usage Guidelines) prüfen lassen⟧

## 8. Verfügbarkeit, Wartung

8.1 Wir bemühen uns um einen störungsarmen Betrieb. Der Dienst läuft auf privat betriebener Infrastruktur und wird über Cloudflare
ausgeliefert; Ausfälle durch Wartung, Strom-/Netzprobleme, Angriffe oder Störungen bei Dienstleistern sind möglich.
Zeitweilige Einschränkungen (z. B. Wartungsfenster) kündigen wir, soweit möglich, an.

8.2 **Free:** Es besteht kein Anspruch auf bestimmte Verfügbarkeit, Wiederherstellung oder Support.

8.3 **Bezahlte Stufen:** ⟦PLATZHALTER: Verfügbarkeitszusage festlegen, z. B. 98 % im Monatsmittel, oder nur „angemessene Bemühungen“ – ohne Zahl keine Garantie⟧.
Die gesetzlichen Rechte von Verbrauchern bei Mängeln (§§ 327 ff. BGB) bleiben unberührt.

## 9. Preise und Zahlung (ab Bezahlstart)

9.1 Die Preise ergeben sich aus Abschnitt 3.1 bzw. der Preisseite. ⟦PLATZHALTER: Variante A – Alle Preise sind Endpreise in Euro einschließlich der gesetzlichen Umsatzsteuer.⟧ ⟦PLATZHALTER: Variante B (Kleinunternehmer) – Alle Preise in Euro; gemäß § 19 UStG wird keine Umsatzsteuer berechnet.⟧

9.2 **Abrechnung.** Die Zahlung erfolgt im Voraus für den jeweiligen Abrechnungszeitraum (Monat bzw. Jahr) über den Zahlungsdienst
Stripe. Zahlungsarten: ⟦PLATZHALTER: von Stripe angebotene Zahlungsarten, z. B. Karte, SEPA-Lastschrift⟧. Sie erhalten eine Rechnung in elektronischer Form.
Es gelten ergänzend die Bedingungen von Stripe ⟦PLATZHALTER: Link⟧.

9.3 **Zahlungsverzug.** Bei fehlgeschlagener Zahlung informieren wir Sie und setzen eine angemessene Frist (⟦PLATZHALTER: z. B. 14 Tage⟧). Läuft sie
ergebnislos ab, können wir die Stufe auf Free zurückstufen (Abschnitt 4.6 gilt). Gesetzliche Verzugsfolgen bleiben unberührt.

9.4 **Preisänderungen.** Wir ändern Preise für laufende Verträge nur nach Abschnitt 16.4. Für neue Verträge gilt der angegebene Preis.

## 10. Vertragsschluss bei kostenpflichtigen Stufen (ab Bezahlstart)

10.1 Das Angebot der Stufen im Dienst ist kein bindendes Angebot. Der Bestellvorgang führt über die Auswahl von Stufe und Laufzeit
(Monat/Jahr), eine Zusammenfassung mit Gesamtpreis, Laufzeit und Kündigungsbedingungen zu einer Schaltfläche mit der Aufschrift
„zahlungspflichtig bestellen“ (Anlage 3). Mit Klick geben Sie ein verbindliches Angebot ab. Der Vertrag kommt zustande, wenn wir die
Bestellung durch Freischaltung der Stufe oder per E-Mail annehmen.

10.2 Eingabefehler können Sie vor Absenden in der Zusammenfassung erkennen und über „zurück“ berichtigen.

10.3 **Vertragstext.** Wir speichern den Vertragstext (Bestelldaten, diese AGB) ⟦PLATZHALTER: Speicherung und Zugriff festlegen, z. B. Bestätigungs-E-Mail mit AGB als PDF und Abruf im Konto⟧. Diese AGB können Sie jederzeit unter ⟦PLATZHALTER: URL der AGB⟧ abrufen und speichern.

## 11. Laufzeit und Kündigung

11.1 **Free.** Der Vertrag über die Stufe Free läuft auf unbestimmte Zeit. Sie können ihn jederzeit durch Löschen des Kontos
(`/app/account`) beenden. Wir können ihn mit einer Frist von ⟦PLATZHALTER: z. B. 14 Tagen⟧ ordentlich kündigen; das Recht zur
außerordentlichen Kündigung bleibt unberührt.

11.2 **Monatsabo** (Pro, Max, Ultra). Der Vertrag läuft auf unbestimmte Zeit; der Abrechnungszeitraum beträgt einen Monat. Sie können
**jederzeit zum Ende des bezahlten Monats** kündigen. Bis dahin bleibt die gebuchte Stufe nutzbar. Eine anteilige Erstattung für den
laufenden Monat erfolgt bei ordentlicher Kündigung nicht.

11.3 **Jahresabo** (Zahlung im Voraus, 12 Monate zum Preis von 10). Die erste Laufzeit beträgt 12 Monate. Sie können **bis zum Ende
dieser 12 Monate** kündigen; die Kündigung wirkt zum Ende der bezahlten 12 Monate. Kündigen Sie nicht, läuft der Vertrag nach Ablauf der
12 Monate **auf unbestimmte Zeit mit monatlicher Abrechnung zum Monatspreis** der Stufe (Abschnitt 11.2) weiter; er ist dann **jederzeit
zum Ende des bezahlten Monats kündbar**. Eine Verlängerung um ein weiteres Jahr erfolgt nicht. Wir weisen Sie ⟦PLATZHALTER: 4 bis 6 Wochen⟧
vor Ablauf der 12 Monate per E-Mail auf den Übergang in die monatliche Laufzeit, den dann geltenden Monatspreis und die
Kündigungsmöglichkeit hin.

> **[Anmerkung Entwurf]** Entschieden vom Besitzer am 12.11.2026 (LIVEMAP34b Frage 1 = A): nach Ablauf des Jahres monatlich kündbar –
> vereinbar mit § 309 Nr. 9 BGB (Verlängerung nur auf unbestimmte Zeit mit monatlicher Kündigungsmöglichkeit). Die Varianten B (Ende ohne
> Verlängerung) und C (Verlängerung um ein weiteres Jahr, gegenüber Verbrauchern unwirksam) sind gestrichen. Technik: Stripe-Abo-Zeitplan
> (Jahrespreis für 12 Monate, danach Monatspreis), siehe `tools/livemap-web/smweb/billing.py`; Preisseite sagt dasselbe.

11.4 **Kündigungsbutton.** Verbraucher können kostenpflichtige Verträge, die über diese Website geschlossen wurden, jederzeit über die
dauerhaft sichtbare Schaltfläche „Verträge hier kündigen“ ⟦PLATZHALTER: genaue Beschriftung, § 312k BGB⟧ kündigen. Über die
Schaltfläche gelangen Sie auf eine Bestätigungsseite, auf der Sie Angaben zu Ihrem Konto/Vertrag machen und die Kündigung mit einer Schaltfläche
„Jetzt kündigen“ bestätigen. Wir bestätigen den Eingang unverzüglich mit Datum und Uhrzeit auf einem dauerhaften Datenträger (E-Mail).
Die Schaltfläche ist auf allen Seiten ständig verfügbar und unmittelbar erreichbar ⟦PLATZHALTER: Umsetzung im Dienst – Erreichbarkeit auch ohne Anmeldung, Beschriftung und Ablauf nach § 312k BGB prüfen lassen⟧. Daneben ist Kündigung per E-Mail an die Adresse im Impressum
möglich.

11.5 **Folgen der Beendigung.** Mit dem Ende der bezahlten Laufzeit wechselt das Konto auf die Stufe Free. Überzählige Karten werden nach Abschnitt 4.6
behandelt. Das Konto bleibt bestehen.

11.6 **Außerordentliche Kündigung.** Das Recht zur Kündigung aus wichtigem Grund (§ 314 BGB) bleibt unberührt. Ein wichtiger Grund liegt
für uns insbesondere vor bei erheblichen oder wiederholten Verstößen gegen Abschnitt 5, bei Missbrauch der Schnittstelle oder bei Zahlungsverzug nach Fristsetzung.
Im Fall einer berechtigten außerordentlichen Kündigung durch uns erstatten wir den auf die Zeit nach der Kündigung entfallenden Anteil des bezahlten Entgelts ⟦PLATZHALTER: festlegen⟧.

## 12. Widerrufsrecht für Verbraucher (ab Bezahlstart)

12.1 Verbraucher haben bei kostenpflichtigen Verträgen über digitale Dienstleistungen ein gesetzliches Widerrufsrecht nach Maßgabe der
**Widerrufsbelehrung in Anlage 1**. Ein Muster-Widerrufsformular steht in Anlage 2.

12.2 **Beginn der Leistung vor Ablauf der Widerrufsfrist; vorzeitiges Erlöschen.** Wenn Sie wünschen, dass wir mit der Leistung bereits
vor Ablauf der Widerrufsfrist beginnen, holen wir im Bestellvorgang Ihre **ausdrückliche Zustimmung** und die **Bestätigung** ein, dass
Ihr Widerrufsrecht bei vollständiger Vertragserfüllung durch uns erlischt (§ 356 Abs. 4 BGB) und dass Sie bei Widerruf nach
Beginn der Leistung einen angemessenen Betrag für die bis dahin erbrachte Leistung zahlen müssen (§ 357a BGB) – Texte in Anlage 3.
Ohne diese Zustimmung schalten wir die kostenpflichtige Stufe erst nach Ablauf der Widerrufsfrist frei.

12.3 Die kostenlose Stufe Free begründet keine Zahlungspflicht; Anlage 1 und 2 sind dafür nicht erforderlich.
⟦PLATZHALTER: anwaltlich bestätigen lassen, ob für die Free-Stufe ein Widerrufsrecht besteht (§ 312 Abs. 1a BGB, Daten als Gegenleistung)⟧

## 13. Mängel und Gewährleistung

13.1 Für Mängel gelten die gesetzlichen Vorschriften. Gegenüber Verbrauchern gelten für kostenpflichtige Stufen die Vorschriften über Verträge über
digitale Produkte (§§ 327 ff. BGB), insbesondere zur Vertragsmäßigkeit und zu Aktualisierungen.

13.2 Mängel melden Sie bitte unverzüglich an ⟦PLATZHALTER: Support-E-Mail⟧ mit Beschreibung und Zeitpunkt. ⟦PLATZHALTER: Gewährleistungsfristen gegenüber Unternehmern festlegen (Vorschlag: 12 Monate)⟧

## 14. Haftung

> **[Anmerkung Entwurf]** Standardklauseln zur Prüfung. Nicht ohne anwaltliche Prüfung veröffentlichen (§§ 307 ff. BGB).

14.1 Wir haften unbeschränkt bei Vorsatz und grober Fahrlässigkeit, bei der Verletzung von Leben, Körper oder Gesundheit, nach dem
Produkthaftungsgesetz, bei arglistig verschwiegenen Mängeln und bei übernommenen Garantien.

14.2 Bei leichter Fahrlässigkeit haften wir nur bei der Verletzung wesentlicher Vertragspflichten (Pflichten, deren Erfüllung die
ordnungsgemäße Durchführung des Vertrags erst ermöglicht und auf deren Einhaltung Sie regelmäßig vertrauen dürfen) und beschränkt auf den
vorhersehbaren, vertragstypischen Schaden. Im Übrigen ist die Haftung bei leichter Fahrlässigkeit ausgeschlossen.

14.3 Für Datenverlust haften wir bei leichter Fahrlässigkeit nur für den Aufwand, der bei regelmäßiger und gefahrentsprechender
Sicherung Ihrer Daten durch Sie zur Wiederherstellung angefallen wäre (Abschnitt 3.6).

14.4 Für die unentgeltliche Stufe Free haften wir nur für Vorsatz und grobe Fahrlässigkeit sowie in den Fällen von 14.1.
⟦PLATZHALTER: anwaltlich bestätigen⟧

14.5 Die Haftung nach Abschnitt 5.6 bleibt unberührt; die Haftung für Inhalte Dritter richtet sich nach den gesetzlichen Haftungsprivilegien (Art. 4 ff. DSA und die ergänzenden Vorschriften des DDG).

## 15. Datenschutz

Wie wir personenbezogene Daten verarbeiten, steht in der **Datenschutzerklärung** ⟦PLATZHALTER: URL⟧. Für Spielernamen und Positionen,
die ein Kartenbesitzer anzeigen lässt, gilt Abschnitt 5.3 sowie der Auftragsverarbeitungsvertrag.

## 16. Änderungen

16.1 **Änderungen der Leistung.** Wir dürfen den Dienst und seine Funktionen ändern, wenn dafür ein triftiger Grund besteht (z. B.
Sicherheit, rechtliche Vorgaben, technische Weiterentwicklung), die Änderung für Sie keine zusätzlichen Kosten verursacht und wir Sie
**rechtzeitig und klar informieren**. Beeinträchtigen Änderungen Ihre Nutzung nicht nur unerheblich, können Verbraucher nach § 327r BGB
kostenfrei kündigen bzw. die Änderung zurückweisen; hierüber informieren wir bei der Änderung.

16.2 **Änderungen dieser Bedingungen** teilen wir bestehenden Nutzern mindestens ⟦PLATZHALTER: z. B. 6 Wochen⟧ vor Inkrafttreten per
E-Mail mit. Gegenüber **Verbrauchern** gelten Änderungen, die Hauptleistungen oder Preise betreffen, nur mit Ihrer **ausdrücklichen Zustimmung**;
stimmen Sie nicht zu, gilt der Vertrag zu den bisherigen Bedingungen fort, und wir können ihn ordentlich kündigen.
⟦PLATZHALTER: Eine stillschweigende Zustimmung (Zustimmungsfiktion) ist im Verbraucherverkehr oft unwirksam – anwaltlich klären⟧

16.3 Gegenüber **Unternehmern** können wir diese Bedingungen mit ⟦PLATZHALTER: Frist⟧ Ankündigung ändern, wenn Sie nicht
innerhalb der Frist widersprechen und wir Sie auf diese Folge hingewiesen haben.

16.4 **Preisänderungen** für laufende Verträge teilen wir mindestens ⟦PLATZHALTER: z. B. 2 Monate⟧ vorher mit. Sie wirken nur für
künftige Abrechnungszeiträume. Sie können zum Ende des laufenden Abrechnungszeitraums kündigen. ⟦PLATZHALTER: rechtliche Zulässigkeit einseitiger Preisänderungen im Verbrauchervertrag klären; Alternative: nur mit ausdrücklicher Zustimmung⟧

## 17. Schlussbestimmungen

17.1 Es gilt deutsches Recht unter Ausschluss des UN-Kaufrechts. Gegenüber Verbrauchern gilt diese Rechtswahl nur, soweit dadurch
nicht der durch zwingende Bestimmungen des Rechts des Staates ihres gewöhnlichen Aufenthalts gewährte Schutz entzogen wird.

17.2 Gerichtsstand für Kaufleute, juristische Personen des öffentlichen Rechts und öffentlich-rechtliche Sondervermögen ist ⟦PLATZHALTER: Sitz des Betreibers⟧.

17.3 Erklärungen können in Textform (E-Mail) an die Adresse im Impressum abgegeben werden.

17.4 **Streitbeilegung.** Hinweise zur Verbraucherstreitbeilegung stehen im Impressum.

17.5 Sollten einzelne Bestimmungen unwirksam sein, bleibt die Wirksamkeit der übrigen unberührt (§ 306 BGB).

Stand: ⟦PLATZHALTER: Datum der Veröffentlichung⟧

---

## Anlage 1 – Widerrufsbelehrung (ab Bezahlstart, nur für Verbraucher)

> **[Anmerkung Entwurf]** Orientiert sich am amtlichen Muster (Anlage 1 zu Art. 246a § 1 Abs. 2 Satz 2 EGBGB) für Dienstleistungsverträge.
> **Vor Verwendung wortgleich mit der aktuell gültigen amtlichen Fassung abgleichen**; Abweichungen können die Schutzwirkung des Musters beseitigen
> und die Widerrufsfrist auf bis zu zwölf Monate und 14 Tage verlängern (§ 356 Abs. 3 BGB).

**Widerrufsrecht**

Sie haben das Recht, binnen vierzehn Tagen ohne Angabe von Gründen diesen Vertrag zu widerrufen.

Die Widerrufsfrist beträgt vierzehn Tage ab dem Tag des Vertragsabschlusses.

Um Ihr Widerrufsrecht auszuüben, müssen Sie uns (⟦PLATZHALTER: Name, Anschrift, E-Mail-Adresse und ggf. Telefonnummer des Betreibers⟧) mittels
einer eindeutigen Erklärung (z. B. ein mit der Post versandter Brief oder eine E-Mail) über Ihren Entschluss, diesen Vertrag zu widerrufen,
informieren. Sie können dafür das beigefügte Muster-Widerrufsformular verwenden, das jedoch nicht vorgeschrieben ist.

Zur Wahrung der Widerrufsfrist reicht es aus, dass Sie die Mitteilung über die Ausübung des Widerrufsrechts vor Ablauf der Widerrufsfrist absenden.

**Folgen des Widerrufs**

Wenn Sie diesen Vertrag widerrufen, haben wir Ihnen alle Zahlungen, die wir von Ihnen erhalten haben, unverzüglich und spätestens binnen vierzehn Tagen ab
dem Tag zurückzuzahlen, an dem die Mitteilung über Ihren Widerruf dieses Vertrags bei uns eingegangen ist. Für diese Rückzahlung verwenden wir dasselbe
Zahlungsmittel, das Sie bei der ursprünglichen Transaktion eingesetzt haben, es sei denn, mit Ihnen wurde ausdrücklich etwas anderes vereinbart; in keinem
Fall werden Ihnen wegen dieser Rückzahlung Entgelte berechnet.

Haben Sie verlangt, dass die Dienstleistung während der Widerrufsfrist beginnen soll, so haben Sie uns einen angemessenen Betrag zu zahlen, der dem Anteil der
bis zu dem Zeitpunkt, zu dem Sie uns von der Ausübung des Widerrufsrechts hinsichtlich dieses Vertrags unterrichten, bereits erbrachten Dienstleistungen im
Vergleich zum Gesamtumfang der im Vertrag vorgesehenen Dienstleistungen entspricht.

**Vorzeitiges Erlöschen des Widerrufsrechts**

Ihr Widerrufsrecht erlischt bei einem Vertrag zur Erbringung von Dienstleistungen, wenn wir die Dienstleistung vollständig erbracht haben und mit der
Ausführung der Dienstleistung erst begonnen haben, nachdem Sie dazu Ihre ausdrückliche Zustimmung gegeben haben und gleichzeitig Ihre Kenntnis davon
bestätigt haben, dass Sie Ihr Widerrufsrecht bei vollständiger Vertragserfüllung durch uns verlieren.

> **[Anmerkung Entwurf]** Bei Abos (Dauerleistung) ist die Dienstleistung innerhalb der ersten 14 Tage in der Regel noch nicht „vollständig erbracht“. Das Erlöschen
> greift daher praktisch erst, wenn ein befristeter Zeitraum (z. B. Jahresabo) vollständig abgelaufen wäre; im Widerrufsfall nach Leistungsbeginn bleibt der Wertersatz.
> Wie das für Monats- und Jahresabos genau zu formulieren ist, vom Anwalt bestätigen lassen.

---

## Anlage 2 – Muster-Widerrufsformular

*(Wenn Sie den Vertrag widerrufen wollen, dann füllen Sie bitte dieses Formular aus und senden Sie es zurück.)*

- An ⟦PLATZHALTER: Name, Anschrift und E-Mail-Adresse des Betreibers⟧:
- Hiermit widerrufe(n) ich/wir (*) den von mir/uns (*) abgeschlossenen Vertrag über die Erbringung der folgenden Dienstleistung (*):
  ⟦PLATZHALTER: Bezeichnung der Leistung, z. B. „Simple Livemap Pro, monatlich“⟧
- Bestellt am (*) / erhalten am (*): ______________
- Name des/der Verbraucher(s): ______________
- Anschrift des/der Verbraucher(s): ______________
- Unterschrift des/der Verbraucher(s) (nur bei Mitteilung auf Papier): ______________
- Datum: ______________

(*) Unzutreffendes streichen.

---

## Anlage 3 – Texte im Bestellvorgang und im Konto (ab Bezahlstart)

> **[Anmerkung Entwurf]** Diese Texte sind Vorgaben für die Umsetzung im Dienst. Sie gehören nicht in die veröffentlichten AGB.
> Wortlaut und Gestaltung (insbesondere Beschriftung der Schaltflächen) anwaltlich prüfen lassen.

**Zusammenfassung unmittelbar über der Bestell-Schaltfläche** (Art. 246a § 1 EGBGB, § 312j Abs. 2 BGB)

- Gewählte Stufe, Leistung (Verweis auf Tabelle 3.1), Laufzeit (Monat/Jahr), Gesamtpreis einschließlich Umsatzsteuer bzw. Hinweis auf § 19 UStG,
  Abrechnungszeitraum, Zahlungsart, Kündigungsbedingungen (Abschnitt 11; beim Jahresabo ausdrücklich: „nach 12 Monaten monatlich zum
  Monatspreis weiter, jederzeit zum Monatsende kündbar“), Hinweis auf Widerrufsrecht und Fundstelle der Widerrufsbelehrung.
- Wird beim Jahresabo der Monatspreis × 12 durchgestrichen gezeigt (Preisseite), steht daneben, dass es der Vergleich mit 12 Monatszahlungen
  ist, kein früherer Preis.

**Zustimmungen** (Kästchen, einzeln, nicht vorangekreuzt)

1. „Ich habe die Nutzungsbedingungen (AGB) und die Widerrufsbelehrung gelesen und akzeptiere sie. Die Datenschutzerklärung habe ich zur Kenntnis genommen.“
2. „Ich verlange ausdrücklich, dass ⟦PLATZHALTER: Name des Betreibers⟧ mit der Ausführung der Dienstleistung vor Ablauf der Widerrufsfrist beginnt. Mir ist bekannt, dass ich bei
   vollständiger Vertragserfüllung durch ⟦PLATZHALTER: Name des Betreibers⟧ mein Widerrufsrecht verliere und dass ich bei einem Widerruf nach Beginn der Leistung einen
   angemessenen Betrag für die bis dahin erbrachte Leistung zahlen muss.“ *(Freiwillig; ohne Zustimmung wird die Stufe erst nach Ablauf der Widerrufsfrist freigeschaltet.)*

**Bestell-Schaltfläche:** „zahlungspflichtig bestellen“ (nichts anderes als diese eindeutige Beschriftung oder eine gleichwertige, § 312j Abs. 3 BGB).

**Kündigung (§ 312k BGB):** Schaltfläche „Verträge hier kündigen“ im Konto und auf den öffentlichen Seiten; Bestätigungsseite mit Angaben zu Kündigendem, Vertrag,
Zeitpunkt der Beendigung und Schaltfläche „Jetzt kündigen“; unmittelbar danach Bestätigungs-E-Mail mit Datum und Uhrzeit.

**Bestätigung des Vertragsschlusses** (§ 312f BGB): E-Mail mit Vertragsinhalt, AGB, Widerrufsbelehrung, Muster-Widerrufsformular und ggf. der Bestätigung der
Zustimmung zum vorzeitigen Leistungsbeginn.

---

ENTWURF – keine Rechtsberatung. Vor Veröffentlichung prüfen lassen.
