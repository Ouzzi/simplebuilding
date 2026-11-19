# PLAN B – N108 Nachtrag 3 + 4: Zahlungsanbieter austauschbar, Paddle zuerst (19.11.2026)

Auftrag: /root/b-auftrag-20261109.md „N108 Nachtrag 3“ und „N108 Nachtrag 4“ (4 ersetzt die Gewichtung von 3: Paddle ist der
Standard, Stripe bleibt zweiter Anschluss). Branch `b-stripe108`, Basis Production (hub master d4fd158ce8, fast-forward, enthält
7b3f66d306). Keine echten Paddle-/Stripe-Aufrufe, keine Keys lesen, `sb-betreiber.json` nie öffnen, kein Deploy.

## 1. Ist-Zustand (erhoben)
- `tools/livemap-web/smweb/billing.py` (BillingMixin): Stripe-HTTP, Signatur, Checkout, Portal, Webhook-Verteilung, Entitlements,
  Jahres-Zeitplan, Stufenwechsel (P070), Gutscheine (P074), Geschenk (P068), Mitspieler-Plätze, Kündigungsbutton, Kontaktformular.
  Anbieter-Teile und Fachlogik sind vermischt (z. B. `on_subscription` liest Stripe-Objekte direkt).
- DB: Migrationen 0–3 (`orders`, `subscriptions`, `entitlements`, `stripe_events`, `gifts`, `gift_names` …).
- Tests: 111 grün (`python3 -I -m unittest discover -s tests`), Stripe-Fake `tests/fake_stripe.py`, Bild-Harness `tests/harness.py --billing-mock`.
- Rechtstexte `docs/legal/*.md` mit `{{betreiber.*}}`, Füllung `fill_legal.py` (Blöcke `{{#wenn …}}…{{#sonst}}…{{/wenn}}`).
- Hostnamen: `config.py` DEFAULTS `account.simplemods.de`/`livemap.simplemods.de` + Cookie-Domain fest; `pages.py` Footer
  (simplemods.de, wiki), Hinweistext livemap.simplemods.de (`pages.py`, `i18n.py`), `stripe_setup.py` Default-URLs fest.

## 2. Ziel-Schnitt
```
smweb/payments/__init__.py   get(svc, name) -> Provider; PROVIDERS = {"paddle", "stripe"}; Standard "paddle"
smweb/payments/base.py       PaymentProvider (schmale Schnittstelle) + Event (einheitliches Ereignis)
smweb/payments/stripe.py     StripeProvider (bisheriger Stripe-Code, Verhalten unverändert)
smweb/payments/paddle.py     PaddleProvider (Paddle Billing API v2)
smweb/billing.py             Fachlogik nur gegen Event + Schnittstelle (Bestellungen, Entitlements, Geschenke, Plätze …)
```
Schnittstelle (Nachtrag 3 wörtlich + was Checkout/Geschenk/Gutschein brauchen):
`configured()`, `checkout_url(order, user, plan, interval, waiver, lang, coupon)`, `gift_checkout_url(...)`, `portal_url(user)`,
`cancel_at_period_end(sub)`, `change_at_period_end(sub, plan)`, `year_then_month(sub_id, plan)`, `parse_webhook(payload, headers)`
→ `(event_id, type, created, [Event])`, `maintenance()`.
Einheitliche Ereignisse: `checkout_completed`, `gift_paid`, `subscription_started`, `subscription_changed`, `subscription_ended`,
`payment_succeeded`, `payment_failed`, `refund`.
- Anbieter für NEUE Bestellungen: `billing.provider` (Standard `paddle`). Webhooks: je Anbieter eigene Route
  (`/api/paddle/webhook`, `/api/stripe/webhook`), jede prüft mit dem Geheimnis ihres Anbieters – so laufen bei einem späteren
  Wechsel alte Abos des anderen Anbieters bis zum Ende weiter.
- Konfiguration: Stripe-Schlüssel bleiben oben im billing-File (Rückwärtskompatibel), Paddle unter `billing.paddle`
  (`environment` sandbox|production, `api_key` bzw. `key_file` + `key_var: PADDLE_API_KEY`, `webhook_secret`, `client_token`,
  `prices{plan:{month,year[,month_trial,year_trial]}}`, `gift_prices{plan}`).
- Idempotenz: Tabelle `stripe_events` bleibt (Stripe-IDs unverändert), Paddle-IDs mit Präfix `paddle:`.

## 3. Paddle-Bau (Nachtrag 4)
- Checkout: `POST /transactions` (items price_id, custom_data order/user/plan/interval/kind, `checkout.url` = eigene Seite
  `/pro/pay`, discount_id bei Gutschein, customer_id falls bekannt) → Weiterleitung auf `/pro/pay?_ptxn=txn_…` → Paddle.js-Overlay
  (`static/paddle-checkout.js`, kein Inline-Skript; CSP um cdn.paddle.com / buy.paddle.com erweitert, nur bei Anbieter Paddle).
- Ohne Widerrufs-Zustimmung: Preis-Variante mit Testphase (`month_trial`/`year_trial`, von paddle_setup angelegt) – wie die
  Stripe-Testphase; fehlt sie, startet nur der Vorzug später (Entitlement-Regel wie bisher).
- Abo monatlich/jährlich je Stufe Basic/Max/Ultra; Jahr → danach monatlich (AGB 11.3): vorgemerkt und kurz vor Periodenende per
  `PATCH /subscriptions/{id}` mit `proration_billing_mode: do_not_bill` auf den Monatspreis umgestellt.
- Stufenwechsel P070: Wechsel wird vorgemerkt (`pending_plan`) und vor dem Periodenende mit `do_not_bill` gesetzt (keine
  Gutschrift, keine Nachberechnung); bis zum Periodenende bleibt die bezahlte Stufe (Regel im Kern: gleiche Periode → alte Stufe).
- Kündigung: `POST /subscriptions/{id}/cancel` `effective_from: next_billing_period`.
- Kundenportal: `POST /customers/{ctm}/portal-sessions` → `urls.general.overview`.
- Webhooks: Header `Paddle-Signature: ts=…;h1=…` (HMAC-SHA256 über `ts:rawbody`, mehrere h1 bei Rotation, Zeitfenster
  `webhook_tolerance` Standard 300 s), idempotent; subscription.created/activated/updated/trialing/past_due/paused/resumed/canceled,
  transaction.completed/payment_failed, adjustment.created/updated (refund/chargeback, nur `approved`).
- Gutscheine = Paddle Discounts: Code auf unserer Bestellseite → `GET /discounts?code=…&status=active` → `discount_id`; sonst kann
  der Kunde im Overlay selbst einen Code eingeben. Wir legen keine Discounts an.
- Geschenk = Einmalkauf: einmaliger Preis je Stufe (Monatspreis, Menge = Monate) + **Code-Einlösung auf Username**: jedes bezahlte
  Geschenk bekommt einen Geschenkcode; Namen beim Kauf sind optional, der Beschenkte (oder Käufer) löst den Code auf
  `/pro/redeem` auf seinen Minecraft-Namen ein (danach 7-Tage-Wechselsperre wie bisher). Gilt für beide Anbieter.
- Erstattung/Chargeback eines Geschenks → Geschenk endet; bei Abos nur Protokoll (Status kommt per subscription-Ereignis).
- Entitlements-Zuordnung unverändert (plans.json, recompute).
- `tools/livemap-web/paddle_setup.py`: Produkte/Preise (Monat, Jahr, Testphasen-Varianten, Geschenk einmalig) und
  Notification-Destination idempotent, **Sandbox zuerst** (Live nur mit `--live`), Standard Trockenlauf; Key nur zur Laufzeit aus
  Env-Datei (`PADDLE_API_KEY`), nie ausgegeben; schreibt Preis-IDs + Webhook-Geheimnis ins billing-File (600).

## 4. Rechtstexte
- `fill_legal.py --provider paddle|stripe` (Standard paddle) → Blöcke `{{#wenn zahlung.paddle}}…{{#sonst}}…{{/wenn}}`; eine
  Schalterstelle für den Anbieterwechsel.
- Paddle-Variante: Kaufvertrag, Zahlung, Rechnung, Widerruf/Erstattung mit Paddle als Wiederverkäufer (Merchant of Record, Paddle
  Buyer Terms, Paddle berechnet und führt USt. ab); unsere AGB heißen „Nutzungsbedingungen“ für Konto/Dienste + Hinweis auf Paddle
  als Verkäufer; Datenschutz: Paddle als eigener Verantwortlicher/Empfänger; Auftragsverarbeitung/Checkliste angepasst;
  Preisangaben: Endpreise inkl. USt. (bei Paddle immer, § 19 entfällt für den Verkauf an Endkunden).
- Stripe-Abschnitte bleiben als Variante (`--provider stripe`).

## 5. Hostnamen
- `config.py`: keine festen account/livemap-Hosts mehr; aus Config oder Env (`SMWEB_ACCOUNT_HOST`, `SMWEB_LIVEMAP_HOST`,
  `SMWEB_COOKIE_DOMAIN`); fehlen sie → klarer Startfehler. Footer-Links (Website/Wiki) aus `links` in der Config.
- Texte mit livemap.simplemods.de → aktueller Livemap-Host aus der Config. `stripe_setup.py`/`paddle_setup.py`: URLs nur per
  Argument/Env. Deploy-Skript: Stage-Hosts per Env überschreibbar (Dev bleibt `*-dev.simplemods.de`).

## 6. Paddle-Kontoprüfung – Checkliste (auch in Vorschau N108)
1. Öffentliche Website auf der Domain des Kontos (simplemods.de / account.simplemods.de), erreichbar ohne Login.
2. Produktbeschreibung: was „Simple Mods Pro“ ist (digitale Dienste, keine physischen Waren), Stufen und Vorzüge (/pro).
3. Preisseite mit Endpreisen (Monat/Jahr, inkl. USt.) – /pro und /pricing.
4. Nutzungsbedingungen (AGB) mit Hinweis auf Paddle als Verkäufer, Datenschutzerklärung, Impressum.
5. Erstattungsregel (Refund Policy) öffentlich verlinkt (Widerruf 14 Tage über Paddle; Hinweis „keine Rückzahlung beim Stufenwechsel“).
6. Kontakt (Kontaktformular /kontakt + Adresse im Impressum).
7. Firmenname/Betreiber stimmt mit den Angaben im Paddle-Konto überein; Domain im Paddle-Dashboard freigeben (Checkout-Domain).
8. Keine verbotenen Inhalte (Paddle AUP): Mods sind eigene Software, Minecraft-Hinweis „nicht offiziell“ vorhanden.

## 7. Tests (Verifikation)
- Paddle-Signatur: gültig / gefälscht / wiederholt (Replay = Duplikat ohne Wirkung) / veralteter Zeitstempel / Rotation (2× h1).
- Idempotenz, Reihenfolge, Fehler → Retry.
- Anbieterwechsel: `billing.provider` paddle/stripe, Routen prüfen jeweils eigenes Geheimnis, unbekannter Anbieter → Fehler.
- Alle bisherigen Funktionen über Paddle mit Fake: Bestellung (mit/ohne Zustimmung, Gutschein), Jahr→Monat, Stufenwechsel zum
  Periodenende (do_not_bill), Kündigung, Portal, Zahlung fehlgeschlagen/Karenz/Ende, Erstattung, Geschenk (Einmalkauf, Code-Einlösung,
  Wechselsperre), Mitspieler-Plätze, CSP/Overlay-Seite.
- `paddle_setup.py`: Trockenlauf Standard, Key nie ausgegeben, Sandbox-URL, Live nur mit Flag, idempotent.
- `fill_legal.py --provider` beide Varianten vollständig.
- Bestehende 111 Tests grün (einzige Änderung: Stripe-Testbasis setzt `"provider": "stripe"`, weil der Standard jetzt Paddle ist).
- Vorschau: Harness `--billing-mock paddle` (Fake-Paddle + Overlay-Nachbildung) → Bilder für n108.html.

## 8. Risiken / bewusst offen
- Paddle-Details, die nur die echte Sandbox zeigt (do_not_bill bei Intervallwechsel Jahr→Monat, Portal-URLs, Overlay mit
  `_ptxn` auf eigener Domain) → Sandbox-E2E erst mit Besitzer-Key.
- Rechtstexte bleiben Entwurf bis zur anwaltlichen Prüfung.
- DB-Migration 4 (neu) kann beim Bündeln mit b-livemap106 kollidieren (Liste nur anhängen).

## 9. Umsetzung – Abweichungen vom Plan (bewusst)
1. Stripe-Tests: einzige Änderungen `"provider": "stripe"` in der Testbasis (Standard ist jetzt Paddle) und explizite
   `--webhook-url/--return-url` im stripe_setup-Test (keine festen Hostnamen mehr als Standard).
2. Paddle-Stufenwechsel/Jahr→Monat: nicht sofort an Paddle, sondern vorgemerkt und ~24 h vor Periodenende (stündliche Wartung,
   `apply_before_hours`) mit `do_not_bill` gesetzt – die Variante „Änderung geplant“ des Auftrags; vermeidet sofortige
   Intervallwechsel, deren Paddle-Verhalten nur die Sandbox zeigt.
3. Gutschein beim Geschenk wird jetzt (beide Anbieter) vor dem Speichern geprüft; eine Kündigung löscht vorgemerkte Wechsel.
4. Stripe liefert weiter kein `refund`-Ereignis (charge.refunded war nie angebunden; Verhalten unverändert).
5. Button „zahlungspflichtig bestellen“ bleibt auch bei Paddle (konservativ; anwaltlich prüfen, ob „weiter zur Zahlung“ reicht).
6. Rechnungs-Mails bei Paddle standardmäßig aus (Paddle stellt Rechnung/Beleg aus; `mails.invoice: true` schaltet sie ein).
7. Paddle-Client-Token legt das Skript nicht an (Dashboard → `--client-token`).
8. Hostnamen: Code frei von festen account/livemap-Hosts; Docstrings, Rechtstexte und Deploy-Stage-Standards nennen sie weiter
   (Deploy per `SMWEB_*_HOST` überschreibbar). Mod (Java) nicht angefasst.
