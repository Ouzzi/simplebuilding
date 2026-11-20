# docs/legal – Rechtstexte für Simple Mods (Website, Wiki, Konto, Simple Mods Pro, Simple Livemap)

> **STATUS: vollständig, ungeprüft – vor Zahlungsstart anwaltlich prüfen.**
> Echte Zahlungen erst nach Prüfung + Gewerbeanmeldung (Stripe bleibt bis dahin im Testmodus bzw. `billing.enabled = false`).
> Stand: 19.11.2026 · Branch `b-stripe108` · Grundlage: Plan `docs/ai/PLAN-B-N108-STRIPE-2026-11-19.md` (Abschnitte 2, 6, 7),
> Besitzer-Antworten Teil 1 und Teil 2 (19.11.2026). Keine Rechtsberatung.

## 1. Dateien

| Datei | Inhalt | veröffentlicht |
|---|---|---|
| [`impressum.md`](impressum.md) | § 5 DDG, § 18 Abs. 2 MStV, DSA-Kontaktstelle (Art. 11/12), VSBG (nicht bereit/verpflichtet), Haftung, Urheberrecht, Minecraft-Hinweis; gilt für simplemods.de, wiki., account., livemap. | ja |
| [`datenschutz.md`](datenschutz.md) | Datenschutzerklärung für alle Dienste: Website/Downloads, Wiki (Kommentare nach Freigabe), Konto, Livemap, Abo, Mitspieler-Plätze, Geschenk-Abos inkl. öffentlicher Namensabfrage, Gutscheine, Frühzugang, Kündigungsseite, Stripe, E-Mails, Kontaktformular, Cloudflare, Logs, Cookies, Fristen, Rechte, Aufsichtsbehörden | ja |
| [`agb.md`](agb.md) | AGB inkl. Livemap-Stufen, Simple Mods Pro, Frühzugang, Mitspieler-Plätze, Geschenk-Abos, Gutscheine, Bestellvorgang, Preise, Laufzeit/Kündigung/Stufenwechsel, Widerruf, DSA-Verfahren, §§ 327 ff. BGB, Haftung, Änderungen; Anlage „Auftragsverarbeitung“ für Kartenbesitzer | ja |
| [`widerruf.md`](widerruf.md) | Widerrufsbelehrung (Muster Anlage 1 zu Art. 246a § 1 Abs. 2 EGBGB, Dienstleistungen/digitale Inhalte), besondere Hinweise zum vorzeitigen Erlöschen, Muster-Widerrufsformular (Anlage 2) | ja |
| [`kuendigung.md`](kuendigung.md) | Kündigungsschaltfläche § 312k BGB: Footer-Link, Bestätigungsseite, Felder, „Jetzt kündigen“, Ergebnisseite, Bestätigungsmail, andere Wege | ja |
| [`auftragsverarbeitung.md`](auftragsverarbeitung.md) | intern: Dienstleisterliste, TOMs, Checkliste, Muster-Hinweis für Kartenbesitzer | nein |
| `README.md` | diese Datei | nein |

Jede veröffentlichte Datei beginnt mit der Zeile
`<!-- intern: ungeprüft – vor Zahlungsstart anwaltlich prüfen. Diese Zeile beim Veröffentlichen entfernen. -->`
und trägt sichtbar „Stand: 19.11.2026“. Die Kopfzeile erst nach der anwaltlichen Prüfung entfernen.

**Ansprache:** Die Rechtstexte siezen („Sie“). Die Seiten des Dienstes (Bestellseite, Kündigungsseite, Mails) duzen („du“);
`kuendigung.md` gibt die Seitentexte deshalb sinngemäß in der Sie-Form wieder. Wer die Ansprache vereinheitlichen will, ändert die
Dienst-Texte (`smweb/pro_pages.py`, `smweb/billing_mail.py`) oder diese Datei – Inhalt und Schaltflächen-Beschriftungen bleiben gleich.

## 2. Platzhalter

Nur diese Schlüssel sind erlaubt (anderes bricht `fill_legal.py` ab):

| Schlüssel | Bedeutung |
|---|---|
| `{{betreiber.name}}` | Name des Betreibers (Pflicht) |
| `{{betreiber.anschrift}}` | ladungsfähige Anschrift, mehrzeilig mit `\n` (Pflicht) |
| `{{betreiber.email}}` | E-Mail des Betreibers (Pflicht; Standardwert der drei Kontaktadressen) |
| `{{betreiber.telefon}}` | Telefon (optional) |
| `{{betreiber.ustid}}` | USt-IdNr. (optional) |
| `{{betreiber.wirtschafts_id}}` | Wirtschafts-IdNr. (optional) |
| `betreiber.kleinunternehmer` | true/false, nur für Bedingungen `{{#wenn betreiber.kleinunternehmer}}…{{#sonst}}…{{/wenn}}` (Pflicht) |
| `{{kontakt.allgemein}}` | Kontaktadresse allgemein (Verträge, Widerruf, Kündigung) |
| `{{kontakt.datenschutz}}` | Kontaktadresse Datenschutz |
| `{{kontakt.melden}}` | Kontaktadresse DSA-Kontaktstelle/Meldungen |

Bedingte Blöcke (nicht verschachteln): `{{#wenn betreiber.telefon}}…{{/wenn}}`, `{{#wenn betreiber.ustid}}…{{/wenn}}`,
`{{#wenn betreiber.wirtschafts_id}}…{{/wenn}}`, `{{#wenn betreiber.kleinunternehmer}}…{{#sonst}}…{{/wenn}}`.

**Zahlungsanbieter (N108 Nachtrag 3/4).** Ein Schalter für alle Texte: `fill_legal.py --provider paddle` (Standard; Paddle ist
Verkäufer/Merchant of Record, AGB heißen „Nutzungsbedingungen“, Widerruf/Erstattung/Rechnung über Paddle, Datenschutz: Paddle als
eigener Verantwortlicher) oder `--provider stripe` (wir verkaufen, Stripe wickelt ab – bisherige Fassung). Blöcke:
`{{#wenn zahlung.paddle}}…{{#sonst}}…{{/wenn}}`, `{{#wenn zahlung.stripe}}…{{/wenn}}`, `{{#wenn zahlung.stripe_ku}}` (Stripe +
Kleinunternehmer), `{{#wenn zahlung.stripe_ust}}` (Stripe + USt.). Der Anbieter muss zu `billing.provider` des Servers passen.
Der zentrale Abschnitt ist AGB 12.0 „Zahlungsabwicklung“; andere Stellen sprechen neutral vom „Zahlungsanbieter (12.0)“.

`{{betreiber.anschrift}}` steht teils auf eigener Zeile (Impressum, Datenschutz, Widerruf), teils im Fließtext (AGB 1.1, Formular,
Kündigung). Im Fließtext wird die Mehrzeiligkeit von Markdown zu Leerzeichen; wer Kommas möchte, lässt `fill_legal.py` Zeilenumbrüche
dort durch „, “ ersetzen.

## 3. Füllen beim Deploy

```
python3 tools/livemap-web/fill_legal.py --operator <json> --out <dir> [--contact-file <json>] [--provider paddle|stripe]
```

- `--operator`: Betreiberdatei (Schlüssel `name`, `anschrift`, `email`, `telefon`, `ustid`, `wirtschafts_id`, `kleinunternehmer`). Sie
  liegt nur auf dem Zielrechner (Rechte 600) und kommt nie ins Repo, in Logs oder in die Memory.
- `--contact-file` (optional): setzt `kontakt.allgemein/datenschutz/melden` auf eigene Weiterleitungsadressen (Cloudflare Email
  Routing). Ohne diese Option sind alle drei = `betreiber.email`.
- Das Skript ersetzt Platzhalter, wertet die Blöcke aus und bricht ab, wenn ein Pflichtschlüssel fehlt oder ein doppelt geschweifter Platzhalter stehen bleibt.
  Ausgabe nach `<dir>`; die interne Kopfzeile bleibt drin, bis sie nach der Prüfung im Repo entfernt wird.

## 4. Entscheidungen (19.11.2026, durch B getroffen)

| # | Entscheidung |
|---|---|
| D1 | Eine gemeinsame Datenschutzerklärung und ein Impressum für alle vier Hosts; § 18 Abs. 2 MStV = Betreiber. |
| D2 | VSBG: „nicht bereit und nicht verpflichtet“. Zweiter Kontaktweg: Kontaktformular, Telefon optional per Block. |
| D3 | Konto ab 16 Jahren; Minderjährige zahlen nur mit Zustimmung der Eltern. Free-Kündigung durch uns 4 Wochen; inaktive Konten nach 3 Jahren mit 30 Tagen Ankündigung löschbar. |
| D4 | Ohne Zustimmung zum vorzeitigen Beginn: Freischaltung **und Abrechnungsbeginn** erst nach 14 Tagen (Abo); Geschenk-Abo: Laufzeit ab Freischaltung. Mit Zustimmung: sofort; Dienstleistung anteiliger Wertersatz, Frühzugang erlischt mit Freischaltung (§ 356 Abs. 5), kein Wertersatz für digitale Inhalte. |
| D5 | § 327h BGB: eigenes Kästchen „Vorabversionen weichen von der üblichen Beschaffenheit ab“ im Bestellvorgang bei Stufen mit Frühzugang. |
| D6 | Stufenwechsel (auf- und abwärts) zum Ende des Abrechnungszeitraums, Jahresabo nach den 12 Monaten; keine Erstattung/Gutschrift (P070). |
| D7 | Preiserhöhung bei laufenden Abos nur mit aktiver Zustimmung, ≥ 6 Wochen Ankündigung; ohne Zustimmung alter Preis, Betreiber darf mit 1 Monat zum Periodenende ordentlich kündigen (Jahresabo nicht vor Ablauf der 12 Monate). AGB-Änderungen: Verbraucher nur mit Zustimmung, Unternehmer Widerspruchslösung 6 Wochen. |
| D8 | Kulanz bei Zahlungsausfall 7 Tage, dann Free. Jahres-Hinweismail ca. 6 Wochen vorher (System 42 Tage). |
| D9 | Frühzugang: persönliche Nutzung inkl. eigener Server und deren Mitspieler; keine öffentliche Weitergabe bis zur öffentlichen Version. |
| D10 | Fristen: Logs 7 Tage (Zugriff + Fehler, ohne Query/Referrer/UA), Sitzungen 30 Tage, Signup-Tag 30 Tage, Audit und Aufrufzahlen 400 Tage (wie Code), Bestellungen/Kündigungen Vertragsende + 3 Jahre, Rechnungen 8 Jahre, Bücher 10, Geschäftsbriefe 6, Anfragen ≤ 3 Jahre, DSA-Meldungen 3 Jahre, Lite-Zuordnung 30 Tage nach Ende, Geschenk-Namen 30 Tage nach Ablauf, abgelehnte Kommentare 30 Tage, Backups ≤ 30 Tage. |
| D11 | Stripe = eigener Verantwortlicher (Zahlung) + Auftragsverarbeiter (Abo/Rechnung/Portal); Vertragspartner Stripe Payments Europe, Dublin. Cloudflare, Inc. als Auftragsverarbeiter, DPF + SCC. |
| D12 | Aufsichtsbehörde ohne Platzhalter: „für den Wohnsitz des Betreibers zuständige Landesdatenschutzbehörde“ + BfDI-Liste + Tabelle der 16 Länder. |
| D13 | AV-Vertrag für Kartenbesitzer als AGB-Anlage (gilt mit „Spieler zeigen“), Datenpanne an Kartenbesitzer ≤ 48 h, neue Unterauftragsverarbeiter 30 Tage vorher. |
| D14 | DSA: Beschwerde gegen Entscheidungen 6 Monate, menschliche Prüfung, keine automatisierten Entscheidungen. |
| D15 | Geschenk-Abo umfasst nur namensgebundene Vorzüge (keine Livemap, kein Frühzugang, keine Plätze); Erstattung nur, wo das Gesetz es verlangt. |
| D16 | Gutscheine: kein Barwert, einer je Bestellung, nicht kombinierbar, befristbar; Missbrauch → Rabatt entfällt ab nächstem Zeitraum. |
| D17 | Haftung: Standardmodell (unbeschränkt Vorsatz/grobe Fahrlässigkeit/Leben/ProdHaftG/Garantie; leichte Fahrlässigkeit nur Kardinalpflichten, vertragstypisch); unentgeltlich nur Vorsatz/grobe Fahrlässigkeit; Unternehmer-Gewährleistung 1 Jahr. Keine Verfügbarkeitszusage. |
| D18 | Konto-Löschung während Abo = Kündigung zum Periodenende, Vorzüge enden mit der Löschung (Warnhinweis vorher). |

## 5. Abweichungen vom gebauten Code – vor Zahlungsstart angleichen

Die Texte legen Verhalten fest, das der Code (Stand Branch) noch nicht oder anders zeigt. **Vor echten Zahlungen bauen oder Text ändern:**

1. **D4 Abrechnungsbeginn ohne Widerrufs-Häkchen:** Code lässt Stripe sofort abrechnen und schaltet nach 14 Tagen frei („die erste
   Zahlung ist trotzdem fällig“). Text verlangt: ohne Häkchen erster Abrechnungszeitraum erst ab Freischaltung (z. B. Checkout
   `subscription_data[trial_period_days]=14` ohne `waiver`). Hinweistext auf `/pro/order` anpassen.
2. **D5 Kästchen § 327h** (Vorabversionen) auf `/pro/order` fehlt; als Pflicht-Kästchen bei Stufen mit `early_access` ergänzen, Text +
   Version in `orders` speichern.
3. **Vertragsbestätigung (§ 312f):** Mail enthält nur Links auf AGB/Widerruf. Text sagt: AGB + Widerrufsbelehrung + Formular **mit**
   der Mail (Anhang oder Volltext) – Links allein sind kein dauerhafter Datenträger.
4. **Google Fonts:** `tools/landing/index.html` lädt `fonts.googleapis.com`/`fonts.gstatic.com`. Datenschutz 4.4 sagt „keine
   Schriftarten-Dienste Dritter“ → Schrift (Silkscreen) selbst hosten, **bevor** die Datenschutzerklärung live geht. Wiki prüfen.
5. **Preiserhöhung mit Zustimmung (19.2)** und **Mitteilung von Leistungs-/AGB-Änderungen**: kein Zustimmungs-Knopf unter „Mein Abo“
   vorhanden; erst bauen, wenn eine Erhöhung ansteht.
6. **Löschjobs:** abgelehnte Wiki-Kommentare 30 Tage, Lite-Zuordnungen 30 Tage nach Ende, Geschenk-Namen 30 Tage nach Ablauf,
   Bestellungen/Kündigungen nach 3 Jahren – im Wartungslauf ergänzen bzw. prüfen. Logrotation 7 Tage für Zugriffs- **und** Fehler-Log.
7. **Konto-Löschung:** Warnhinweis bei laufendem Abo und Kündigung bei Stripe (D18) prüfen/bauen; Löschung der Wiki-Kommentare mit dem Konto.
8. **Kündigungsbutton-Footer** auf allen Seiten von account. und livemap. (gebaut laut Plan E10) – auf Dev klicktesten.
9. **Geschenk-Abo nur verkaufen, wenn mindestens ein namensgebundener Vorzug verfügbar ist** (§ 327e BGB) und die Bestellseite die Vorzüge nennt.
10. **Live-Daten:** Mod blendet Zuschauer und `/simplelivemap hide` aus; schleichende/unsichtbare Spieler (P041) noch nicht – Texte sagen
    „im Spiel sichtbar“ ohne Schleichen zu erwähnen; nach Mod-Änderung ggf. ergänzen.
11. **Widerrufsbutton (EU-Richtlinie 2023/2673, ab 19.06.2026):** Für online geschlossene Verträge ist voraussichtlich eine elektronische
    Widerrufsfunktion („Vertrag widerrufen“) Pflicht. Nicht gebaut und nicht in den Texten → anwaltlich klären, ggf. analog `/kuendigen`
    bauen und in widerruf.md/AGB 14 ergänzen.

## 6. Checkliste für den Besitzer

- [ ] Gewerbe angemeldet, Finanzamt (Fragebogen steuerliche Erfassung); Kleinunternehmer ja/nein mit Steuerberatung → `kleinunternehmer` in der Betreiberdatei und `vat.mode` in der Server-Config **gleich** setzen
- [ ] Betreiberdatei vollständig (ladungsfähige Anschrift, keine Postfachadresse); Kontaktadressen entscheiden (`--contact-file` für Weiterleitungen)
- [ ] Anwaltliche Prüfung aller fünf veröffentlichten Texte, insbesondere: Button-Lösung mit Stripes zweiter Schaltfläche (§ 312j), Kündigung ohne Login (§ 312k), Erlöschen beim gemischten Abo (§ 356 Abs. 4/5), § 327h-Kästchen, Preis-/AGB-Änderungsmodell, Weitergabeverbot Frühzugang vs. Mod-Lizenz, Widerrufsbutton (Punkt 5.11), Mindestalter, Mojang-Richtlinien für einen kostenpflichtigen Dienst
- [ ] Widerrufsbelehrung und Muster-Formular wortgleich mit der aktuell gültigen amtlichen Fassung abgleichen
- [ ] Abweichungen aus Abschnitt 5 gebaut bzw. Texte angepasst; danach `terms_version` erhöhen
- [ ] Dienstleister-Checkliste in `auftragsverarbeitung.md` Teil C abgehakt
- [ ] Links der Aufsichtsbehörden (Datenschutz 17) einmal aufrufen
- [ ] Preise auf der Preisseite = Beispiele in AGB 6.1/12.1 (sonst Beispiele anpassen)
- [ ] Footer auf allen Seiten: Impressum, Datenschutz, AGB, Widerruf, „Verträge hier kündigen“
- [ ] Nach der Prüfung: interne Kopfzeile entfernen, Stand-Datum setzen, veröffentlichen; erst dann `billing.enabled` mit Live-Schlüssel
