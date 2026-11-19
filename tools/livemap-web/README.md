# tools/livemap-web – Simple Mods account + hosted Simple Livemap (N34, Phase 1)

Plan: `docs/ai/PLAN-A-LIVEMAP34-2026-11-12.md` (decisions D1–D22, API section 3). Spec (owner ok 12.11.):
`/root/sb-tools-a/previews/a-livemap34-preview.html`. **Built, not deployed** (deploy needs the owner's approval).

One Python service (`smweb`, standard library only: `http.server`, `sqlite3`) for two hosts:
- `account.simplemods.de` – central Simple Mods login: `/login`, `/register` (e-mail confirmation), `/forgot`, `/reset`,
  `/verify`, `/logout`; one database for all future Simple Mods pages.
- `livemap.simplemods.de` – landing, `/pricing` (buying = “coming soon”), `/self-host`, `/explore` (later), viewer
  `/m/<name>/` (the mod's own `index.html`, served from the checkout), `/app` (my maps), `/app/connect` (4 steps,
  one-time code 15 min), `/app/m/<id>` (settings, sharing, link, disconnect), `/app/archive`, `/app/plan`, `/app/account`
  (profile, password, sessions, JSON export, delete), `/admin` (developer role: users, tiers, maps, queue, “sofort fertig”,
  storage, noticeable patterns, dev outbox), upload API `/api/v1/*` for the mod, Stripe webhook `/api/stripe/webhook` (off).

## Run locally (tests and pictures only - acceptance happens on the dev stage)
```
cd tools/livemap-web
python3 -m unittest discover -s tests -t tests          # unit tests (also gradle testLivemapWeb)
python3 tests/harness.py --port 8790                     # one host 127.0.0.1:8790 with sample accounts/maps (screenshots)
```
CLI: `python3 -m smweb serve|set-role|set-tier|add-user|maintenance --config <file>` (see `smweb/__main__.py`).

## Security in short
- Passwords PBKDF2-SHA256 600 000 rounds, same format as the wiki login (`tools/zentrale-web/auth.py`, migration later).
- Sessions server side (only SHA-256 of the token stored), cookie `__Secure-sm_session` `Domain=simplemods.de`,
  HttpOnly, Secure, SameSite=Lax, 30 days; password change/reset ends sessions.
- Every form: CSRF token (session-bound, or double-submit cookie `sm_csrf` before login) **and** Origin/Referer check.
- Rate limits in memory (login 5 failures/15 min per address and per e-mail, register, forgot, code redemption, uploads
  per tier) + nginx `limit_req`. Client address = `X-Client-IP` from nginx, trusted only from loopback.
- Strict CSP (`script-src 'self'`, no inline script) on all pages; only the viewer (the mod's page) has its own CSP.
- Upload: only `<dim>/<z>/<x>/<y>.png`, 128 × 128 PNG (signature + IHDR), ≤ 256 KiB each, entries/bytes per tier, zip-bomb
  ratio check, content-addressed blobs; per-map and global storage quota, stop at 90 % disk.
- Live player positions only in memory (latest state), only when the map shows players and the owner accepted the
  processing agreement (legal draft U3); never in the database or a log.
- No secrets in the repo: `config.example.json`, `billing.example.json` are templates; the real files live in `/etc/simplemods/`.

## Tiers
`tiers.json` (spec section 4): Free/Basic/Max/Ultra (owner 19.11.: old id "pro" = basic; P011 dimensions per tier) – places, public share, archive, views/day, live/tile rhythm, minimum
preparation time, place lock (Free 7 days), shares, storage per map, upload rate. First 2 connections of an account
(lifetime) have no minimum time (owner addition 3). Developer role = unlimited. Test accounts: admin page or `set-tier`.

## Deploy (prepared, NOT run – owner approval needed)
`deploy/deploy_livemap_web.sh check|install|nginx|smoke|rollback|developer [--stage dev|prod]` – service user `smweb`,
data `/srv/livemap-web[-dev]`, config `/etc/simplemods/livemap-web-<stage>.json` (root:smweb 640, secret made once),
systemd unit (hardened, 300 MB), nginx server blocks on :8081 with the simplemods allow snippet, rate limits, access log
without query strings (7 days logrotate). Dev stage: `account-dev`/`livemap-dev.simplemods.de` behind nginx basic auth
(`htpasswd -c -B /etc/nginx/smweb-dev.htpasswd <name>`), the mod API stays reachable. Owner steps: CT bigger (N34-7),
tunnel hostnames in Cloudflare, SMTP account, legal texts (`docs/legal/`, drafts), Stripe account – then prod.
Known dev limitation: a “by link” map sends `Authorization: Bearer` for its tiles, which conflicts with basic auth on dev.

## Simple Mods Pro – Abo/Stripe (N108, built, OFF until the owner switches it on)
Plan: `docs/ai/PLAN-B-N108-STRIPE-2026-11-19.md`. One account subscription on `account.simplemods.de` (Basic/Max/Ultra →
livemap tier Basic/Max/Ultra + early access + lite seats for Max/Ultra (P039); catalogue `plans.json`, prices = placeholders from `tiers.json`, server config
`plans` overrides everything). Pages: `/pro`, `/pro/order` (summary, terms box, voluntary early-start box § 356 Abs. 4/5 BGB,
button „zahlungspflichtig bestellen“) → Stripe Checkout → `/pro/done`, `/pro/manage` (portal), `/kuendigen` (§ 312k, footer link
on every page), `/early` (download area, login + perk). Webhook `/api/stripe/webhook` (signed, each event id once; checkout.session.completed,
customer.subscription.*, invoice.paid, invoice.payment_failed). Tables (migration 2): orders, subscriptions, entitlements,
stripe_events, cancellations, notices. Yearly = subscription schedule (year price once, then month price, release).
Tests: `tests/test_billing.py`, `tests/test_stripe_setup.py` (fake Stripe `tests/fake_stripe.py`), `tools/release/test_early_access.py`.
Pictures: `python3 tests/harness.py --port 8797 --billing-mock` (fake Stripe + local checkout imitation; never an acceptance).

### Deploy steps (A, after the owner's go; nothing here was run)
1. Merge `b-stripe108` into the dev line; deploy smweb to the **dev** stage as usual (`deploy_livemap_web.sh install|nginx --stage dev`);
   the DB migrates itself (user_version 2). With billing `enabled: false` everything paying says “coming soon”.
2. `mkdir -p /srv/early-access-dev/files` (owner smweb), set `early_access.dir` = `/srv/early-access-dev`, `accel_prefix` = `/_early/`
   in `/etc/simplemods/livemap-web-dev.json`; `legal.*_url` once the texts are approved.
3. Owner puts the **test** key into the env file on sb-web (`STRIPE_SECRET_KEY=sk_test_…`, mode 600). Then, on sb-web:
   `python3 tools/livemap-web/stripe_setup.py --env-file <file> --webhook-url https://livemap-dev.simplemods.de/api/stripe/webhook --return-url https://account-dev.simplemods.de/pro/manage --billing-file /etc/simplemods/livemap-billing-dev.json`
   (dry run, check the list) → same with `--apply`. It writes prices, portal id, webhook secret and `key_file`/`key_var` (never the key).
4. Billing file: `"enabled": true`, `vat.mode` (small_business | regular), `vat.stripe_tax`, `mails.invoice` (false if Stripe sends
   receipts itself); restart smweb. Stripe dashboard (test): Tax settings if used, customer e-mails, branding.
5. Dev click test (owner/A): `/pro` → order → test card 4242… → `/pro/done` → `/pro/manage` → portal → `/kuendigen`; `stripe trigger`
   or a test clock for renewal/failed payment; check mails in the dev outbox.
6. Prod only after the lawyer-checked texts: same steps with the live key and `--live`, prod URLs, `enabled: true`.

### Owner answer part 2 (19.11.) – what else is in
- Plan change only at the period end, no proration (P070): `/pro/manage` „Stufe wechseln“ → subscription schedule; portal switching off.
- Coupons (P074): Checkout code field (`billing.coupons.enabled`, default on) or a code on the order page; admin page lists used codes;
  `stripe_setup.py` makes **no** codes.
- Gift subscriptions (P068): `/pro/gift`, `/pro/gifts`; one-off payment, bound to Minecraft name(s) (`plans.json` `gifts`), name change
  every 7 days; perks per name: `GET /api/v1/mc/<name>` (livemap host).
- Contact form: `account.simplemods.de/kontakt` (honeypot, 3-s minimum, rate limits); target = server config `contact.form_to` or the
  contact address. Contact addresses: `contact.mode` `operator` (default: `operator_email`) or `addresses` (forwarding addresses).
- Flags P050/P053/P079: `plans.json` `flags`/`free_flags`, `svc.plan_flags(user)`.
- Legal texts: `docs/legal/*.md` complete with `{{betreiber.*}}`/`{{kontakt.*}}`; render at deploy on the target host:
  `python3 tools/livemap-web/fill_legal.py --operator /root/.config/sb-betreiber.json --out /srv/legal --strip-internal [--contact-file <json>]`
  (never prints values; fails on missing keys or leftovers). Billing stays OFF until the owner's review + business registration.
