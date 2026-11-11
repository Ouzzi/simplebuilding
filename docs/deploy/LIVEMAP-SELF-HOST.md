# Simple Livemap on your own website

*Deutsch, kurz:* Die Livemap läuft nach der Installation sofort, aber nur auf dem eigenen Rechner (127.0.0.1, privat mit
Schlüssel). Wer sie auf einer eigenen Webseite zeigen will, setzt einen Reverse-Proxy (nginx/Caddy, Abschnitt 2) davor
oder lädt eine statische Kopie hoch (Abschnitt 3). Der Schlüssel bleibt geheim (Abschnitt 4). Kurzfassung auch im Wiki
(Simple Livemap → „Eigene Webseite“).

This guide is for server owners who want the map on their own domain. Nothing here is needed to use the map on the
computer the game runs on: there the chat line **[Open Livemap]** (single player / LAN host) or `/simplelivemap link`
is enough. Files: `tools/deploy/livemap/selfhost/`. (Our own setup on the wiki host is `LIVEMAP-SB-WEB.md`; this is
the general version of it.)

## 1. What the mod gives you

| Setting (`config/simplelivemap.json`, group `web`) | Default | Meaning |
|---|---|---|
| `enabled` | `true` (new installs) | built-in read-only web server. Config files from older versions keep their written `false`. |
| `bind` | `127.0.0.1` | address it listens on; `127.0.0.1` = only this computer |
| `port` | `8125` | busy → next free port up to 8134 for this session (log line). A port outside 8125–8134 never moves. |
| `visibility` | `private` | `private`: tiles and `live.json` only with the key; `public`: for everyone who reaches the port |
| `token` | random, 128 bit | the key; `/simplelivemap config reset web.token` makes a new one |
| `showPlayers` | `false` | online players (name, dimension, x, z only) in `live.json`; each can opt out with `/simplelivemap hide` |
| `requestsPerMinute` | `1200` | budget per client address |
| `chatHint` | `true` | single player / LAN host: one clickable chat line per session |

The server knows exactly three routes, all `GET`/`HEAD`: `/` (viewer page, contains no data), `/live.json`,
`/tiles/<dimension>/<zoom 0-4>/<x>/<y>.png`. The viewer uses relative paths, so it also works under a sub path such
as `https://example.org/map/` (with the trailing slash).

**The key:** a private map wants `Authorization: Bearer <key>`. The link from `/simplelivemap link` carries it after
`#t=`; browsers never send the part after `#`, the viewer reads it and sends it only as that header. So the key never
appears in a server or proxy access log.

## 2. Reverse proxy (live map, players, recommended)

The proxy is the only thing on the internet; the game server stays on `127.0.0.1` (proxy on the same machine) or on its
LAN address with a firewall rule that lets only the proxy in. Keep `visibility` **private** in every case.

**Mod config** (restart the server afterwards, or `/simplelivemap config set ...`):

```json
"web": { "enabled": true, "bind": "127.0.0.1", "port": 8125, "visibility": "private", "requestsPerMinute": 6000 }
```

`requestsPerMinute` goes up because all visitors now arrive from the proxy's address; the per-visitor limit moves to
the proxy. Proxy on another machine: `"bind": "<LAN address of the game server>"` (never `0.0.0.0` on a machine with
a public address) plus a firewall rule, e.g. nftables on the game server:

```
nft add table inet livemap
nft add chain inet livemap input '{ type filter hook input priority 0; policy accept; }'
nft add rule inet livemap input tcp dport 8125 ip saddr <PROXY-ADDRESS> accept
nft add rule inet livemap input tcp dport 8125 drop
```

(Windows: an inbound rule in Windows Defender Firewall for TCP 8125, remote address = the proxy.)

**Choose how visitors get in:**

- **A – link with key** (simplest): visitors open `https://map.example.org/#t=<key>`. Take the link from
  `/simplelivemap link` and replace the part before `#` with your address. The proxy passes the `Authorization` header
  through (both examples do that by default). Everyone with the link sees the map; a new key locks old links out.
- **B – own login**: the proxy asks for a password (basic auth) and adds the key itself; the browser never sees it.
  Visitors open `https://map.example.org/` without `#t=`. Good when the circle of viewers changes.
- **Public**: like B without the login lines. Only if everyone may see your world (and think about `showPlayers`).

### nginx

`tools/deploy/livemap/selfhost/nginx-livemap.conf.example` → `/etc/nginx/conf.d/livemap.conf`, replace
`map.example.org` and the upstream, then `nginx -t && systemctl reload nginx`; TLS e.g. `certbot --nginx -d map.example.org`.
It passes only the three routes and only `GET`/`HEAD`, answers everything else with 404, limits each visitor to
10 requests/s (burst 80, else 429), drops cookies both ways.

Variant B: uncomment the three lines `auth_basic`, `auth_basic_user_file`, `include`, then

```
htpasswd -c /etc/nginx/livemap.htpasswd viewer         # apache2-utils; or: printf 'viewer:%s\n' "$(openssl passwd -apr1)"
install -m 600 /dev/null /etc/nginx/livemap-token.conf
# write ONE line into it with an editor (do not echo the key on the command line, it lands in the shell history):
#   proxy_set_header Authorization "Bearer <key>";
nginx -t && systemctl reload nginx
```

Do not run `nginx -T` unfiltered: it prints every included file, the key file too.

Optional cache (the game server then sees at most one request per tile per 30 s, and the map stays visible while the
server sleeps): see `tools/deploy/livemap/nginx-http.conf.in` and `nginx-map.conf.in` (`proxy_cache_path`,
`proxy_cache_valid 200 204 30s`, `proxy_cache_use_stale`). With variant A do **not** cache – a cached tile would be
served without checking the key.

### Caddy

`tools/deploy/livemap/selfhost/Caddyfile.example`: replace `map.example.org` and the upstream, `caddy validate`,
`caddy reload`. Caddy gets the certificate by itself. Variant B: uncomment the `basic_auth` block (Caddy 2.8+,
older: `basicauth`; hash with `caddy hash-password`) and both `header_up Authorization` lines, and start Caddy with
`LIVEMAP_TOKEN=<key>` in its environment (systemd: `EnvironmentFile=/etc/caddy/livemap.env`, mode 600).
A rate limit per visitor needs the plugin `caddy-ratelimit`; without it only the mod's own budget applies.

### Check

```
curl -sI https://map.example.org/                    # 200 (B: 401 without the password)
curl -s -o /dev/null -w '%{http_code}\n' https://map.example.org/live.json   # A: 401 without the key – good
curl -s -o /dev/null -w '%{http_code}\n' https://map.example.org/level.dat   # 404, never reaches the game
curl -s -o /dev/null -w '%{http_code}\n' -X POST https://map.example.org/    # 403/404/405, never 200
```

Then open the map in the browser: tiles load, the dimension list fills, the browser console shows no errors.

## 3. Static copy (any web space, no live players)

For plain web space (no proxy possible) or when the game server should never be reachable. The copy is as fresh as the
last run and shows no players.

```
python3 tools/deploy/livemap/selfhost/livemap_static_sync.py --world <world folder> --dest <site folder> --jar <simplelivemap jar>
```

- `--world`: the server's world folder (single player: `.minecraft/saves/<world>`); the script reads
  `<world>/simplelivemap/tiles`. `--jar` takes the viewer page out of the mod jar (or `--index <index.html>`).
- It copies only valid tile files (`<dimension>/<0-4>/<x>/<y>.png`, at most 1 MiB, no links), writes `live.json`
  without players and `index.html`, removes tiles that are gone, and only touches changed files. `--dry-run` only reports;
  `--every 300` repeats every 5 minutes. Python 3.8+ standard library, runs on Linux, macOS and Windows.
- Upload `<site folder>` with what your host offers: `rsync -a --delete site/ user@host:/var/www/map/`, `scp -r`,
  WinSCP/FileZilla (SFTP/FTP). Regularly: cron/systemd timer (Linux), Task Scheduler (Windows), or `--every` plus an
  upload step.
- Serve it with `X-Content-Type-Options: nosniff` and the same `Content-Security-Policy` as the mod
  (`tools/deploy/livemap/nginx-map-static.conf.in` shows the nginx lines). The copy needs no key: it is as public as the
  web space; put the host's own password protection in front if it should not be.

Our own timer-based variant (rsync with a read-only key, disk guard) is `tools/deploy/livemap/livemap-sync.sh`
(+ `.service`/`.timer`), described in `LIVEMAP-SB-WEB.md` „Variante B“.

## 4. Security checklist

- **Nothing open by accident:** `bind` stays `127.0.0.1` unless you mean it; `0.0.0.0` means every network interface.
  On a dedicated server the start log says who reaches the map.
- **Key:** never in a URL query (the mod ignores `?token=` anyway), never in a public repository, screenshot or chat with
  others. `/simplelivemap link` sends it only to the asking player, never to the log. New key:
  `/simplelivemap config reset web.token`, then update the proxy (variant B) and share new links (variant A).
- **Read only:** the mod and the proxy examples allow only `GET`/`HEAD` and the three routes.
- **Rate limits:** per visitor at the proxy (nginx `limit_req`), per address in the mod (`requestsPerMinute`), plus
  4 worker threads and a short queue in the mod. A whole-map viewer loads ~40 tiles at once and polls `live.json` every
  2 s (`liveSeconds`).
- **Players:** `showPlayers` is off by default. If you turn it on for a public map, tell your players; each can leave
  with `/simplelivemap hide`. `live.json` never contains UUIDs, addresses or health.
- **What the map shows:** only chunks players really explored, so it does not give away the seed or unexplored bases.
