# PLAN B-N108 – Stripe + Abo „Simple Mods Pro“ (Basic/Pro/Ultra) – 19.11.2026

Auftrag: `/root/b-auftrag-20261109.md` „## N106–N108“ → „### N108“. Branch `b-stripe108` (Basis `claude-wave1` ea9326b793).
Arbeitsweise: Plan → Umsetzung → Test. Keine echten Stripe-Aufrufe (alles gemockt), kein Deploy, keine Secrets.

## 1. Ist-Zustand (erhoben)

- `tools/livemap-web/smweb/billing.py`: Signaturprüfung (HMAC-SHA256, 300 s), `checkout()` (Abo, Preis je Livemap-Stufe),
  `portal()`, `webhook()` (checkout.session.completed → Kunde merken; customer.subscription.* → `users.tier` aus Metadaten).
  AUS, solange `enabled`/Keys fehlen. Kein Idempotenzschutz, kein Jahres-Zeitplan (TODO a-livemap34c), keine Rechnungs-Events,
  keine Entitlements, keine Widerrufs-Zustimmung, keine Kündigungsseite.
- `billing.example.json`: Preis-IDs je **Livemap**-Stufe pro/max/ultra. `tiers.json`: Free/Pro/Max/Ultra, Preise 0/399/799/1499 ct,
  Jahr = 10 Monate.
- `pages.py`: `/pricing`, `/app/plan` mit „Coming soon“. CSP `form-action` erlaubt nur eigene Hosts → ein 303 nach
  `checkout.stripe.com` nach Formular-POST würde vom Browser blockiert (Fehler im Bestand, wird mit behoben).
- `docs/legal/*`: AGB (Livemap-bezogen, Abschnitte 9–12 + Anlagen 1–3 „ab Bezahlstart“), Datenschutz (§ 9 Stripe „nicht aktiv“),
  Impressum, AV-Liste. README mit Entwurfs-Hinweis.
- simplemods.de = statische Landing (nginx `/srv/landing`), öffentliche Downloads `/downloads/`. Kein Login-Bereich.
- Tests: `tools/livemap-web/tests/test_service.py` (43 Tests, grün), `harness.py` (lokaler Server nur für Bilder).

## 2. Entscheidungen (selbst getroffen, Besitzer kann alles umkonfigurieren)

| # | Entscheidung |
|---|---|
| E1 | Neuer Katalog `tools/livemap-web/plans.json`: Abo-Stufen **basic/pro/ultra** → Livemap-Stufe **pro/max/ultra**, Vorzüge `["livemap", "early_access"]`. Preise: Platzhalter = Preis der zugeordneten Livemap-Stufe aus `tiers.json` (399/799/1499 ct), Jahr = `yearly_months_paid` × Monat. Überschreibbar in Server-Config `plans`. |
| E2 | Konto-Abo lebt auf **account.simplemods.de**: `/pro` (Stufen), `/pro/order` (Zusammenfassung + Häkchen + „zahlungspflichtig bestellen“), `/pro/done`, `/pro/manage` (Status, Portal, Rechnungen), `/kuendigen` (§ 312k), `/early` (Frühzugang). Livemap `/pricing` und `/app/plan` verweisen auf `/pro`. Alte Route `/app/plan` POST (tier=pro/max/ultra) bleibt kompatibel (Mapping Livemap-Stufe → Abo-Stufe). |
| E3 | Bestell-Button „zahlungspflichtig bestellen“ auf **unserer** Zusammenfassungsseite (§ 312j Abs. 2/3), danach Stripe Checkout nur für Zahlungsdaten; zusätzlich `custom_text[submit]` bei Stripe. Risiko: anwaltlich klären, ob Stripes „Abonnieren“-Knopf zusätzlich reicht/stört (im README §7 vermerkt). |
| E4 | Widerrufs-Checkbox freiwillig (Anlage 3 Nr. 2): mit Zustimmung sofort freigeschaltet (§ 356 Abs. 4 Dienstleistung, Abs. 5 digitale Inhalte = Frühzugang-Downloads); ohne Zustimmung beginnt das Entitlement erst 14 Tage nach Vertragsschluss (`starts_at`). AGB-Häkchen Pflicht. Zustimmungen + Textversion in Tabelle `orders` gespeichert. |
| E5 | Entitlements: Tabellen `subscriptions` (Stripe-Abo-Spiegel), `entitlements` (Konto → Abo-Stufe → Vorzüge, livemap_tier, gültig bis, Quelle stripe/admin), `stripe_events` (Idempotenz: Event-ID einmalig), `orders`. `users.tier` wird aus dem Entitlement abgeleitet (`set_tier` mit Herabstufungsregeln); Admin-gesetzte Stufen bleiben (Quelle admin), solange kein Stripe-Abo existiert. |
| E6 | Status-Regeln: active/trialing → berechtigt; past_due → berechtigt bis Periodenende + Kulanz `grace_days` (Default 7); unpaid/canceled/incomplete/incomplete_expired/deleted → Free. `cancel_at_period_end` → berechtigt bis Periodenende. Out-of-order: Event älter als letzter verarbeiteter Stand des Abos wird ignoriert. Wartung (`maintenance`) setzt abgelaufene Entitlements auf Free (Sicherheitsnetz bei verlorenen Webhooks). |
| E7 | Jahresabo = **Subscription Schedule** nach Checkout (`from_subscription`, dann Phasen: Jahrespreis 1 Iteration → Monatspreis, `end_behavior=release`), idempotent (Schedule-ID gespeichert, Idempotency-Key). Hinweis-Mail 4–6 Wochen vor Ablauf: Hook vorbereitet (`year_notice_days`, Default 42), in Wartung. |
| E8 | Mails (Text, DE/EN): Vertragsbestätigung (§ 312f, mit Zustimmungsbestätigung, Links AGB/Widerruf/Formular), Rechnung bezahlt (Link `hosted_invoice_url`), Zahlung fehlgeschlagen (Portal-Link), Kündigungseingang (§ 312k Abs. 4, Datum/Uhrzeit), Abo beendet, Jahres-Übergangshinweis. Schalter `mails.invoice` (Stripe kann eigene Belege schicken → doppelt vermeiden). |
| E9 | Preisangaben: `vat.mode` = `"small_business"` (Hinweis § 19 UStG) oder `"regular"` („inkl. {rate} % USt.“); `vat.stripe_tax` → `automatic_tax`, Adress-/USt-ID-Erfassung. Default: small_business (Platzhalter, Besitzer entscheidet). |
| E10 | Kündigungsbutton „Verträge hier kündigen“ im Footer jeder Seite → `/kuendigen` (ohne Login erreichbar; eingeloggt vorausgefüllt). Formular: E-Mail, Vertrag, ordentlich/außerordentlich (+Grund), Zeitpunkt „nächstmöglich“ → „Jetzt kündigen“. Wirkung: passt E-Mail zu einem Konto mit Abo → Stripe `cancel_at_period_end=true` (bzw. sofort bei außerordentlich: nur vermerkt, Besitzer prüft). Bestätigungsmail an die angegebene Adresse **und** an die Konto-Adresse; Ratenlimit. Ergebnisseite nennt nichts über Kontoexistenz. |
| E11 | Frühzugang: `/early` auf account.simplemods.de, Login + Vorzug `early_access`; Liste aus `<early_dir>/index.json`; Download `/early/f/<datei>` per `X-Accel-Redirect` (nginx-internal, Prod) oder direkt (Dev/Tests). Regel „zu Release gleichzeitig, danach Plattformen eine Version zurück“: Skript `tools/release/early_access.py` (stage/plan/publish, Default Trockenlauf; Upload nur mit `--upload --owner-approved` + Token-Env, nicht ausgeführt). Link auf der Landing folgt mit Besitzer-Freigabe (nicht in diesem Branch, Landing-Deploy = Prod). |
| E12 | `tools/livemap-web/stripe_setup.py`: liest Key zur Laufzeit aus `/root/.config/sb-keys.env` (`STRIPE_SECRET_KEY` bzw. `--key-var`), nie ausgeben; Default **Trockenlauf** (keine Netzaufrufe, zeigt Plan); `--apply` nötig; `sk_live_` nur mit `--live`; idempotent: Produkte mit fester ID (`smpro_basic` …), Preise über `lookup_key` (bei Betragsänderung neuer Preis + `transfer_lookup_key`), Webhook-Endpunkt per URL-Suche, Portal-Konfiguration per Metadaten; schreibt Preis-IDs + Webhook-Secret in die Billing-Datei (außerhalb Repo, 0600), gibt Secrets nie aus. |
| E13 | Rechtstexte (`docs/legal/*`): AGB auf Gesamt-Abo „Simple Mods Pro“ (alle Dienste) erweitert: Abo-Stufen/Vorzüge, Laufzeit, Kündigung inkl. § 312k, Preisänderung, Frühzugang (keine Zusicherung), digitale Inhalte (§§ 327 ff., § 356 Abs. 5), Downgrade/Ablauf. Datenschutz § 9 Stripe für Abo, Entitlements, Kündigungsseite, Download-Protokoll; Impressum/Widerrufsbelehrung angepasst. ⟦…⟧ bleiben, Entwurfsstatus + README-Hinweis bleiben. (Parallel durch Subagent, gleiche Arbeitsweise.) |

## 3. Dateien

- `tools/livemap-web/plans.json` (neu), `billing.example.json` (Schema erweitert), `config.example.json` (`plans`, `early_access`).
- `smweb/config.py` (plans laden), `smweb/db.py` (Migration 2: orders, subscriptions, entitlements, stripe_events, cancellations),
  `smweb/billing.py` (umgebaut: Katalog, Checkout, Schedule, Portal, Webhook idempotent, Entitlements, Kündigung, Mails, Wartung),
  `smweb/billing_mail.py` (neu, Mail-Vorlagen), `smweb/pro_pages.py` (neu, Seiten), `smweb/app.py` (Routen, CSP form-action Stripe,
  API auch auf account host für Webhook), `smweb/pages.py` (Footer-Kündigungslink, Verweise auf /pro), `smweb/maps.py` (Wartung ruft Billing),
  `smweb/i18n.py` (Fehlertexte).
- `tools/livemap-web/stripe_setup.py` (neu), `tools/release/early_access.py` (neu).
- Tests: `tests/test_billing.py` (neu), `tests/test_stripe_setup.py` (neu), `tools/release/tests/test_early_access.py` (neu),
  `tests/harness.py` (`--billing-mock`: Fake-Stripe + Mock-Checkout-Seite für Bilder).
- Deploy: `deploy/nginx-smweb.conf.in` (internal `/_early/`), README Abschnitt „Abo/Stripe“ + Deploy-Schritte.
- Rechtstexte `docs/legal/*`.

## 4. Risiken

- Rechtslage (Button-Beschriftung bei Stripe, § 312k ohne Login, Kleinunternehmer/OSS) → nur Entwurf, Anwalt.
- Stripe-API-Details (Schedule-Phasen, Portal-Konfig) nur gegen Doku/Erfahrung gebaut, nicht gegen echte API → Testmodus-E2E mit
  Besitzer-Key nötig (Skript + Checkliste vorbereitet).
- Doppelte Rechnungsmails (Stripe + wir) → Schalter.
- Kündigung ohne Login missbrauchbar → nur `cancel_at_period_end` (im Portal rücknehmbar), Mail an Konto-Adresse, Ratenlimit.

## 5. Verifikation

- Unit/Integration (`python3 -I -m unittest`): Signatur (gefälscht, alt, falsches Secret, mehrere v1), Stripe-CLI-artige Payloads
  (`stripe trigger`-Form: `id`, `object: event`, `api_version`, `created`, `data.object`), Idempotenz (gleiches Event 2×),
  Reihenfolge, Monat/Jahr inkl. Schedule-Aufrufe (Fake-Transport protokolliert Requests), Up-/Downgrade, past_due/Kulanz, Ablauf per
  Wartung, Widerruf ohne Zustimmung (14 Tage), Kündigungsseite (mit/ohne Login, Mails), Frühzugang (ohne Login → Login, ohne Vorzug
  → 403, mit → Datei), CSP form-action, Preisangabe USt/§ 19.
- `stripe_setup.py`: Trockenlauf ohne Netz, Key nie in Ausgabe, zweiter Lauf legt nichts doppelt an, live-Key ohne `--live` abgewiesen.
- `early_access.py`: Plan-Regel (Launch gleichzeitig, danach eine Version zurück), Trockenlauf, Upload ohne Freigabe abgewiesen.
- Bestehende 43 Tests bleiben grün. Bilder der Bestellstrecke per Playwright vom lokalen Harness (Mock), nur für die Vorschau.
- NICHT getestet: echte Stripe-API, echte Mails, Klick-Test auf Dev (nach Deploy durch A/Besitzer).
