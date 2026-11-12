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
`tiers.json` (spec section 4): Free/Pro/Max/Ultra – places, public share, archive, views/day, live/tile rhythm, minimum
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
