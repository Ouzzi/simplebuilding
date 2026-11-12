"""Server-rendered pages (EN/DE side by side). Every value from users or the database goes through ``e()``; no inline
script or style (CSP), behaviour lives in static/app.js, looks in static/site.css (Minecraft-like like simplemods.de)."""
from __future__ import annotations

import html
import time

from . import maps as mapsmod


def e(value) -> str:
    return html.escape("" if value is None else str(value), quote=True)


def num(ctx, n) -> str:
    s = f"{int(n):,}"
    return s.replace(",", " ") if ctx.lang == "de" else s


def euro(ctx, cents: int) -> str:
    if cents == 0:
        return "0 €" if ctx.lang == "de" else "€0"
    if ctx.lang == "de":
        return f"{cents // 100},{cents % 100:02d} €"
    return f"€{cents // 100}.{cents % 100:02d}"


def when(ctx, ts) -> str:
    if not ts:
        return "–"
    return time.strftime("%d.%m.%Y %H:%M" if ctx.lang == "de" else "%Y-%m-%d %H:%M", time.gmtime(ts)) + " UTC"


def size(n: int) -> str:
    for unit in ("B", "KB", "MB", "GB"):
        if n < 1024 or unit == "GB":
            return f"{n:.0f} {unit}" if unit == "B" else f"{n:.1f} {unit}"
        n /= 1024
    return str(n)


def csrf_field(ctx) -> str:
    return f'<input type="hidden" name="csrf" value="{e(ctx.csrf())}">'


def form(ctx, action: str, inner: str, cls: str = "", confirm: str = "") -> str:
    c = f' class="{cls}"' if cls else ""
    d = f' data-confirm="{e(confirm)}"' if confirm else ""
    return f'<form method="post" action="{e(action)}"{c}{d}>{csrf_field(ctx)}{inner}</form>'


def alerts(ctx) -> str:
    out = ""
    if ctx.error:
        out += f'<p class="alert bad" role="alert">{e(ctx.error)}</p>'
    if ctx.flash:
        out += f'<p class="alert ok" role="status">{e(ctx.flash)}</p>'
    return out


# -------------------------------------------------------------------- layout
def layout(ctx, title: str, body: str, wide: bool = False, cls: str = "") -> str:
    L = lambda p: ctx.url("livemap", p)  # noqa: E731
    other = "en" if ctx.lang == "de" else "de"
    if ctx.user is not None:
        who = (f'<a href="{L("/app")}">{ctx.t("My maps", "Meine Karten")}</a>'
               f'<a href="{L("/app/account")}" class="who" title="{e(ctx.user["email"])}">{e(ctx.user["name"] or ctx.user["email"])}</a>'
               + form(ctx, ctx.url("account", "/logout"), f'<button class="link">{ctx.t("Log out", "Abmelden")}</button>', "inline"))
        if ctx.user["role"] == "developer":
            who = f'<a href="{L("/admin")}">Admin</a>' + who
    else:
        who = f'<a class="btn small" href="{e(ctx.url("account", "/login"))}">{ctx.t("Log in", "Anmelden")}</a>'
    brand_home = L("/")
    nav = (f'<a href="{L("/pricing")}">{ctx.t("Pricing", "Preise")}</a>'
           f'<a href="{L("/self-host")}">{ctx.t("Self-host", "Selbst hosten")}</a>')
    footer_links = [f'<a href="https://simplemods.de/">simplemods.de</a>', f'<a href="https://wiki.simplemods.de/">Wiki</a>']
    legal = ctx.cfg["legal"]
    for key, en, de in (("imprint_url", "Imprint", "Impressum"), ("privacy_url", "Privacy", "Datenschutz"), ("terms_url", "Terms", "AGB")):
        if legal.get(key):
            footer_links.append(f'<a href="{e(legal[key])}">{ctx.t(en, de)}</a>')
    if not any(legal.get(k) for k in ("imprint_url", "privacy_url", "terms_url")):
        footer_links.append(f'<span class="mut">{ctx.t("Legal pages follow before the launch.", "Rechtstexte folgen vor dem Start.")}</span>')
    return f"""<!doctype html>
<html lang="{ctx.lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>{e(title)} · Simple Livemap</title><link rel="stylesheet" href="/static/site.css"><link rel="icon" href="/static/livemap-icon.png">
<script src="/static/app.js" defer></script></head>
<body class="{cls}"><header class="top"><div class="w bar">
<a class="brand" href="{brand_home}"><img src="/static/livemap-icon.png" alt="" width="32" height="32"><span>Simple Livemap</span></a>
<nav class="nav">{nav}</nav><div class="me">{who}<a class="lang" href="?lang={other}" hreflang="{other}">{other.upper()}</a></div>
</div></header>
<main class="w{' wide' if wide else ''}">{body}</main>
<footer class="w foot">{' · '.join(footer_links)}<br><span class="mut">Simple Livemap · {ctx.t("a Simple Mods service", "ein Dienst von Simple Mods")}</span></footer>
</body></html>"""


def message(ctx, title: str, text: str, link: tuple | None = None, mail: bool = False) -> str:
    extra = f'<p><a class="btn" href="{e(link[0])}">{e(link[1])}</a></p>' if link else ""
    if mail and ctx.svc.mailer.uses_outbox and ctx.cfg["dev_outbox"]:
        extra += f'<p class="mut small">{ctx.t("Development: mails land in the outbox (admin page).", "Entwicklung: Mails liegen im Ausgang (Admin-Seite).")}</p>'
    return layout(ctx, title, f'<section class="card narrow"><h1>{e(title)}</h1><p>{e(text)}</p>{extra}</section>')


# -------------------------------------------------------------------- account pages
def auth_card(ctx, title: str, inner: str, foot: str = "") -> str:
    return layout(ctx, title, f'<section class="card narrow auth"><h1>{e(title)}</h1>{alerts(ctx)}{inner}{foot}</section>', cls="auth-page")


def login(ctx, nxt: str, email: str) -> str:
    done = ""
    if ctx.req.query.get("done") == "reset":
        done = f'<p class="alert ok">{ctx.t("Password set. Please log in.", "Passwort gesetzt. Bitte anmelden.")}</p>'
    inner = done + form(ctx, ctx.url("account", "/login"), f"""
<input type="hidden" name="next" value="{e(nxt)}">
<label>{ctx.t("E-mail", "E-Mail")}<input type="email" name="email" value="{e(email)}" autocomplete="username" required maxlength="254"></label>
<label>{ctx.t("Password", "Passwort")}<input type="password" name="password" autocomplete="current-password" required maxlength="256"></label>
<button class="btn">{ctx.t("Log in", "Anmelden")}</button>""", "stack")
    foot = (f'<p class="links"><a href="{e(ctx.url("account", "/forgot"))}">{ctx.t("Forgot password?", "Passwort vergessen?")}</a> · '
            f'<a href="{e(ctx.url("account", "/register"))}">{ctx.t("Create an account", "Konto anlegen")}</a></p>'
            f'<p class="mut small">{ctx.t("One Simple Mods account for all simplemods.de pages.", "Ein Simple-Mods-Konto für alle Seiten von simplemods.de.")}</p>')
    return auth_card(ctx, ctx.t("Log in", "Anmelden"), inner, foot)


def register(ctx, f: dict) -> str:
    inner = form(ctx, ctx.url("account", "/register"), f"""
<label>{ctx.t("Name (shown to people you share maps with)", "Name (sehen Personen, mit denen du Karten teilst)")}<input name="name" value="{e(f.get("name", ""))}" maxlength="40" autocomplete="nickname"></label>
<label>{ctx.t("E-mail", "E-Mail")}<input type="email" name="email" value="{e(f.get("email", ""))}" autocomplete="email" required maxlength="254"></label>
<label>{ctx.t("Password (at least 10 characters)", "Passwort (mindestens 10 Zeichen)")}<input type="password" name="password" autocomplete="new-password" required minlength="10" maxlength="256"></label>
<label>{ctx.t("Password again", "Passwort wiederholen")}<input type="password" name="password2" autocomplete="new-password" required minlength="10" maxlength="256"></label>
<label class="check"><input type="checkbox" name="terms" value="1" required> <span>{ctx.t(
        "I accept the terms of use and have read the privacy notice.", "Ich akzeptiere die Nutzungsbedingungen und habe die Datenschutzhinweise gelesen.")}</span></label>
<button class="btn">{ctx.t("Create account", "Konto anlegen")}</button>""", "stack")
    foot = (f'<p class="mut small">{ctx.t("One free account per confirmed e-mail address. We keep only what the service needs (e-mail, name, a password hash, your maps, usage numbers); details in the privacy notice.", "Ein Free-Konto je bestätigter E-Mail-Adresse. Gespeichert wird nur, was der Dienst braucht (E-Mail, Name, ein Passwort-Hash, deine Karten, Nutzungszahlen); Einzelheiten in den Datenschutzhinweisen.")}</p>'
            f'<p class="links"><a href="{e(ctx.url("account", "/login"))}">{ctx.t("I already have an account", "Ich habe schon ein Konto")}</a></p>')
    return auth_card(ctx, ctx.t("Create an account", "Konto anlegen"), inner, foot)


def forgot(ctx) -> str:
    inner = form(ctx, ctx.url("account", "/forgot"), f"""
<label>{ctx.t("E-mail", "E-Mail")}<input type="email" name="email" autocomplete="email" required maxlength="254"></label>
<button class="btn">{ctx.t("Send link", "Link schicken")}</button>""", "stack")
    return auth_card(ctx, ctx.t("Forgot password", "Passwort vergessen"), inner,
                     f'<p class="links"><a href="{e(ctx.url("account", "/login"))}">{ctx.t("Back to login", "Zurück zum Anmelden")}</a></p>')


def reset(ctx, token: str) -> str:
    inner = form(ctx, ctx.url("account", "/reset"), f"""
<input type="hidden" name="t" value="{e(token)}">
<label>{ctx.t("New password", "Neues Passwort")}<input type="password" name="password" autocomplete="new-password" required minlength="10" maxlength="256"></label>
<label>{ctx.t("Again", "Wiederholen")}<input type="password" name="password2" autocomplete="new-password" required minlength="10" maxlength="256"></label>
<button class="btn">{ctx.t("Set password", "Passwort setzen")}</button>""", "stack")
    return auth_card(ctx, ctx.t("New password", "Neues Passwort"), inner)


# -------------------------------------------------------------------- public livemap pages
def tier_cells(ctx, tier: dict, key: str) -> str:
    t = tier
    if key == "maps":
        return ctx.t(f"{t['maps']} ({t['public']} public)", f"{t['maps']} (davon {t['public']} öffentlich)")
    if key == "archive":
        days = ctx.t("1 year", "1 Jahr") if t["archive_days"] >= 365 else ctx.t(f"{t['archive_days']} days", f"{t['archive_days']} Tage")
        return f"{t['archive']} · {days}"
    if key == "views":
        return ctx.t("fair use (≈ 100 000)", "fair use (≈ 100 000)") if t["views_day"] >= 100000 else num(ctx, t["views_day"])
    if key == "live":
        return f"{t['live_seconds']} s / {t['tile_seconds'] // 60} min"
    if key == "prep":
        m = t["prep_minutes"]
        base = f"{m // 60} h" if m >= 60 and m % 60 == 0 and m > 60 else ("~1 h" if m == 60 else f"~{m} min")
        extra = {0: ctx.t(", low priority", ", niedrige Priorität"), 1: ctx.t(", priority", ", Vorrang"),
                 3: ctx.t(", highest", ", höchste")}.get(t["priority"], "")
        return base + extra
    if key == "slot":
        return ctx.t("1× per 7 days", "1× pro 7 Tage") if t["slot_lock_days"] else ctx.t("any time", "frei")
    if key == "shares":
        return ctx.t("unlimited", "unbegrenzt") if t["shares"] < 0 else ctx.t(f"{t['shares']} people", f"{t['shares']} Personen")
    if key == "storage":
        return size(t["map_mb"] * 1024 * 1024).replace(".0", "")
    if key == "extra":
        if not t["custom_name"]:
            return "–"
        return ctx.t("own address name", "eigener Adressname")
    return ""


def maps_line(ctx, t: dict) -> str:
    return ctx.t("{0} maps ({1} public)", "{0} Karten ({1} öffentlich)").format(t["maps"], t["public"])


ROWS = [("maps", "Maps at the same time", "Karten gleichzeitig"), ("archive", "Archive (disconnected maps kept)", "Puffer (getrennte Karten gespeichert)"),
        ("views", "Views per day (all maps)", "Aufrufe pro Tag (alle Karten)"), ("live", "Player live rhythm / tile update", "Spieler-Live-Takt / Kachel-Aktualisierung"),
        ("prep", "Preparation (minimum time)", "Vorbereitung (Mindestzeit)"), ("slot", "Take a place again", "Platz neu belegen"),
        ("shares", "Share with accounts", "Teilen mit Konten"), ("storage", "Storage per map", "Speicher je Karte"), ("extra", "Extras", "Zusatz")]


def tier_table(ctx, buy: bool) -> str:
    order = ctx.cfg.tiers["order"]
    tiers = ctx.cfg.tiers["tiers"]
    months = ctx.cfg.tiers.get("yearly_months_paid", 10)
    head = "".join(f'<th scope="col">{e(tiers[k]["name"])}</th>' for k in order)
    body = ""
    for key, en, de in ROWS:
        body += f'<tr><th scope="row">{ctx.t(en, de)}</th>' + "".join(f"<td>{e(tier_cells(ctx, tiers[k], key))}</td>" for k in order) + "</tr>"
    price = ""
    for k in order:
        p = tiers[k]["price_month"]
        if p == 0:
            price += f'<td><b class="price">{euro(ctx, 0)}</b></td>'
        else:
            price += (f'<td><b class="price" data-month="{e(euro(ctx, p))}" data-year="{e(euro(ctx, p * months))}">{euro(ctx, p)}</b>'
                      f'<span class="per" data-month="{ctx.t("per month", "pro Monat")}" data-year="{ctx.t("per year", "pro Jahr")}">{ctx.t("per month", "pro Monat")}</span></td>')
    body += f'<tr class="pricerow"><th scope="row">{ctx.t("Price", "Preis")}</th>{price}</tr>'
    if buy:
        cells = ""
        for k in order:
            if tiers[k]["price_month"] == 0:
                cells += f'<td><a class="btn small" href="{e(ctx.url("livemap", "/app/connect"))}">{ctx.t("Start free", "Kostenlos starten")}</a></td>'
            else:
                cells += f'<td><button class="btn small soon" disabled aria-disabled="true">{ctx.t("Coming soon", "Coming soon")}</button></td>'
        body += f'<tr><th scope="row"></th>{cells}</tr>'
    return f'<div class="tw"><table class="tiers"><thead><tr><th></th>{head}</tr></thead><tbody>{body}</tbody></table></div>'


def interval_toggle(ctx) -> str:
    months = ctx.cfg.tiers.get("yearly_months_paid", 10)
    return (f'<div class="toggle" role="group" aria-label="{ctx.t("Billing period", "Abrechnung")}">'
            f'<button type="button" data-interval="month" aria-pressed="true">{ctx.t("Monthly", "Monatlich")}</button>'
            f'<button type="button" data-interval="year" aria-pressed="false">{ctx.t("Yearly", "Jährlich")}</button></div>'
            f'<p class="mut small">{ctx.t(f"Yearly in advance: 12 months for the price of {months}.", f"Jährlich im Voraus: 12 Monate zum Preis von {months}.")}</p>')


def landing(ctx) -> str:
    L = lambda p: e(ctx.url("livemap", p))  # noqa: E731
    tiers = ctx.cfg.tiers["tiers"]
    cards = ""
    for k in ctx.cfg.tiers["order"]:
        t = tiers[k]
        cards += (f'<div class="c tier"><b>{e(t["name"])}</b><span class="price">{euro(ctx, t["price_month"])}</span>'
                  f'<span class="per">{ctx.t("per month", "pro Monat") if t["price_month"] else ctx.t("forever", "dauerhaft")}</span>'
                  f'<ul><li>{e(maps_line(ctx, t))}</li><li>{e(tier_cells(ctx, t, "views"))} {ctx.t("views/day", "Aufrufe/Tag")}</li>'
                  f'<li>{ctx.t("live", "Live")} {t["live_seconds"]} s</li></ul></div>')
    faq = [
        (ctx.t("Do I need my own web server or an open port?", "Brauche ich einen eigenen Webserver oder eine Portfreigabe?"),
         ctx.t("No. Your Minecraft server uploads the map; we show it. Only an outgoing HTTPS connection is needed.",
               "Nein. Dein Minecraft-Server lädt die Karte hoch, wir zeigen sie. Nötig ist nur eine ausgehende HTTPS-Verbindung.")),
        (ctx.t("Why does a new map say “being prepared”?", "Warum steht bei einer neuen Karte „wird vorbereitet“?"),
         ctx.t("Against abuse every map is prepared for a while: Free at least 6 hours, paid tiers faster. Your first two maps are ready as soon as the upload is done.",
               "Gegen Missbrauch wird jede Karte eine Weile vorbereitet: Free mindestens 6 Stunden, bezahlte Stufen schneller. Deine ersten zwei Karten sind fertig, sobald der Upload durch ist.")),
        (ctx.t("Does the map show players?", "Zeigt die Karte Spieler?"),
         ctx.t("Only when you switch it on for the map. Then name and position of online players appear - only the latest state, no history. Every player can hide with /simplelivemap hide.",
               "Nur wenn du es für die Karte einschaltest. Dann erscheinen Name und Position der Online-Spieler – nur der letzte Stand, kein Verlauf. Jeder Spieler kann sich mit /simplelivemap hide verstecken.")),
        (ctx.t("Does the map give away my seed?", "Verrät die Karte meinen Seed?"),
         ctx.t("No. Only land that players really explored is drawn; pre-generated land stays dark.",
               "Nein. Gezeichnet wird nur, was Spieler wirklich erkundet haben; vorgeneriertes Land bleibt dunkel.")),
        (ctx.t("What happens when I disconnect a map?", "Was passiert beim Trennen einer Karte?"),
         ctx.t("It moves to your archive and stays stored (Free 30 days). Connecting the same world again uploads only what changed.",
               "Sie wandert in deinen Puffer und bleibt gespeichert (Free 30 Tage). Verbindest du dieselbe Welt wieder, wird nur hochgeladen, was sich geändert hat.")),
        (ctx.t("Can I host it myself?", "Kann ich selbst hosten?"),
         ctx.t("Yes, for free: the mod has its own web server for exactly one map per installation. More maps, sharing and no setup: the hosted tiers.",
               "Ja, kostenlos: Die Mod hat einen eigenen Webserver für genau eine Karte je Installation. Mehr Karten, Teilen und keine Einrichtung: die gehosteten Stufen.")),
    ]
    faq_html = "".join(f'<details class="c"><summary>{e(q)}</summary><p>{e(a)}</p></details>' for q, a in faq)
    body = f"""
<section class="hero">
  <div class="hero-text">
    <h1>Simple Livemap</h1>
    <p class="sub">{ctx.t("Your Minecraft world as a live map in the browser - exactly as your players explored it. Connect it with one command, share one link.",
                          "Deine Minecraft-Welt als Live-Karte im Browser – genau so, wie deine Spieler sie erkundet haben. Mit einem Befehl verbinden, einen Link teilen.")}</p>
    <p><a class="btn" href="{L("/app/connect")}">{ctx.t("Connect a map", "Karte verbinden")}</a>
       <a class="btn s" href="{L("/self-host")}">{ctx.t("Host it yourself", "Selbst hosten")}</a></p>
  </div>
  <div class="hero-map"><canvas id="hero-map" width="512" height="384" aria-label="{ctx.t("Animated example map", "Animierte Beispielkarte")}" role="img"></canvas>
  <p class="mut small">{ctx.t("Example drawing, not a real world.", "Beispielzeichnung, keine echte Welt.")}</p></div>
</section>
<h2>{ctx.t("Three steps", "Drei Schritte")}</h2>
<div class="g3">
  <div class="c step"><b class="n">1</b><b>{ctx.t("Install the mod", "Mod installieren")}</b><p>{ctx.t("Simple Livemap on your Fabric server (or single player).", "Simple Livemap auf deinem Fabric-Server (oder im Einzelspieler).")}</p></div>
  <div class="c step"><b class="n">2</b><b>{ctx.t("Enter the code in game", "Code im Spiel eingeben")}</b><p>{ctx.t("Make a code here, then as operator:", "Hier einen Code erzeugen, dann als Operator:")} <code>/simplelivemap connect ABCD-EFGH</code></p></div>
  <div class="c step"><b class="n">3</b><b>{ctx.t("Share the link", "Link teilen")}</b><p>{ctx.t("Private, by link or public - you decide per map.", "Privat, per Link oder öffentlich – du entscheidest je Karte.")}</p></div>
</div>
<h2>{ctx.t("Self-host or hosted?", "Selbst hosten oder gehostet?")}</h2>
<div class="tw"><table class="cmp"><thead><tr><th></th><th>{ctx.t("Self-host", "Selbst hosten")}</th><th>{ctx.t("Hosted here", "Hier gehostet")}</th></tr></thead><tbody>
<tr><th scope="row">{ctx.t("Cost", "Kosten")}</th><td>{ctx.t("free", "kostenlos")}</td><td>{ctx.t("Free tier, paid tiers coming soon", "Free-Stufe, bezahlte Stufen bald")}</td></tr>
<tr><th scope="row">{ctx.t("Maps", "Karten")}</th><td>{ctx.t("exactly one per installation", "genau eine je Installation")}</td><td>{ctx.t("2 to 13 by tier", "2 bis 13 je Stufe")}</td></tr>
<tr><th scope="row">{ctx.t("Setup", "Einrichtung")}</th><td>{ctx.t("port, reverse proxy or own domain", "Port, Reverse-Proxy oder eigene Domain")}</td><td>{ctx.t("one command", "ein Befehl")}</td></tr>
<tr><th scope="row">{ctx.t("Map when the server is off", "Karte bei ausgeschaltetem Server")}</th><td>{ctx.t("gone", "weg")}</td><td>{ctx.t("stays visible", "bleibt sichtbar")}</td></tr>
<tr><th scope="row">{ctx.t("Accounts, sharing, dashboard", "Konten, Teilen, Dashboard")}</th><td>–</td><td>✓</td></tr>
<tr><th scope="row">{ctx.t("Viewer", "Kartenansicht")}</th><td colspan="2">{ctx.t("the same: zoom, dimensions, players, follow, coordinates", "dieselbe: Zoom, Dimensionen, Spieler, folgen, Koordinaten")}</td></tr>
</tbody></table></div>
<h2>{ctx.t("Tiers", "Stufen")}</h2>
<div class="g4">{cards}</div>
<p><a href="{L("/pricing")}">{ctx.t("All limits and prices", "Alle Grenzen und Preise")} →</a></p>
<h2>FAQ</h2>
<div class="faq">{faq_html}</div>"""
    return layout(ctx, ctx.t("Live map for your Minecraft world", "Live-Karte für deine Minecraft-Welt"), body, cls="landing")


def pricing(ctx) -> str:
    body = f"""<h1>{ctx.t("Pricing", "Preise")}</h1>
<p class="sub">{ctx.t("Buying is coming soon. Every limit already works - start free today.", "Kaufen kommt bald. Alle Grenzen gelten schon jetzt – starte heute kostenlos.")}</p>
{interval_toggle(ctx)}
{tier_table(ctx, True)}
<div class="g2">
<div class="c"><b>{ctx.t("Above the view limit", "Über dem Aufruf-Limit")}</b><p>{ctx.t(
        "Nothing is blocked: the map stays visible with cached tiles and a slower live rhythm; you get a note “lots of visitors - higher tier?”.",
        "Nichts wird gesperrt: Die Karte bleibt mit zwischengespeicherten Kacheln und langsamerem Live-Takt sichtbar; du bekommst den Hinweis „viel Besuch – höhere Stufe?“.")}</p></div>
<div class="c"><b>{ctx.t("Cancel", "Kündigen")}</b><p>{ctx.t(
        "Monthly: to the end of the paid month. Yearly: runs to the end of the year and renews only when not cancelled. On a downgrade extra maps wait 30 days in the archive.",
        "Monatlich zum Ende des bezahlten Monats. Jährlich: läuft bis zum Ende des Jahres und verlängert sich nur, wenn nicht gekündigt. Beim Herabstufen bleiben überzählige Karten 30 Tage im Puffer.")}</p></div>
<div class="c"><b>{ctx.t("Later", "Später")}</b><p>{ctx.t(
        "Embedding (iframe), snapshots and time lapse, own domain, API and the public map list come after the start.",
        "Einbetten (iframe), Schnappschüsse und Zeitraffer, eigene Domain, API und die öffentliche Kartenliste kommen nach dem Start.")}</p></div>
</div>"""
    return layout(ctx, ctx.t("Pricing", "Preise"), body, wide=True)


def self_host(ctx) -> str:
    body = f"""<h1>{ctx.t("Host it yourself", "Selbst hosten")}</h1>
<p class="sub">{ctx.t("The mod brings its own small web server: one world, one map, no accounts. It costs nothing - the limits are your own hardware.",
                       "Die Mod bringt ihren eigenen kleinen Webserver mit: eine Welt, eine Karte, keine Konten. Kostet nichts – die Grenzen sind deine eigene Hardware.")}</p>
<div class="c prose">
<h2>{ctx.t("1. Install and open", "1. Installieren und öffnen")}</h2>
<p>{ctx.t("Put Simple Livemap into the mods folder. The map server starts by itself on this computer only (127.0.0.1:8125) and is private.",
          "Simple Livemap in den mods-Ordner legen. Der Kartenserver startet von selbst, nur auf diesem Rechner (127.0.0.1:8125), und ist privat.")}
{ctx.t("In single player you get a clickable [Open Livemap] in chat; on a server an operator types", "Im Einzelspieler kommt ein anklickbares [Livemap öffnen] im Chat; auf einem Server tippt ein Operator")} <code>/simplelivemap link</code>.</p>
<h2>{ctx.t("2. Show it to others", "2. Anderen zeigen")}</h2>
<p>{ctx.t("Put a reverse proxy (nginx, Caddy) or a tunnel in front, or upload a static copy to any web space. The guide with examples is in the wiki (“Simple Livemap → Own website”).",
          "Einen Reverse-Proxy (nginx, Caddy) oder einen Tunnel davorsetzen, oder eine statische Kopie auf beliebigen Webspace laden. Die Anleitung mit Beispielen steht im Wiki („Simple Livemap → Eigene Website“).")}</p>
<h2>{ctx.t("3. Exactly one map", "3. Genau eine Karte")}</h2>
<p>{ctx.t("Self-hosting serves exactly one live map per installation and server: one web server, one world. A second start on the same installation or a second Simple Livemap on the same port is detected and stays off; proxies that merge several worlds are not supported.",
          "Selbst hosten liefert genau eine Live-Karte je Installation und Server: ein Webserver, eine Welt. Ein zweiter Start in derselben Installation oder eine zweite Simple Livemap auf demselben Port wird erkannt und bleibt aus; Proxys, die mehrere Welten zusammenführen, werden nicht unterstützt.")}
{ctx.t("Need more maps, sharing with accounts or a map that stays online when the server is off? Use the hosted tiers.", "Mehr Karten, Teilen mit Konten oder eine Karte, die online bleibt, wenn der Server aus ist? Dafür gibt es die gehosteten Stufen.")}</p>
<p><a class="btn" href="{e(ctx.url("livemap", "/pricing"))}">{ctx.t("Hosted tiers", "Gehostete Stufen")}</a></p>
</div>"""
    return layout(ctx, ctx.t("Self-host", "Selbst hosten"), body)


def explore(ctx) -> str:
    return message(ctx, ctx.t("Explore - coming later", "Entdecken – kommt später"), ctx.t(
        "A list of public maps whose owners agree will come after the start.",
        "Eine Liste öffentlicher Karten, deren Besitzer zustimmen, kommt nach dem Start."))


def preparing(ctx, m, progress: int) -> str:
    own = ctx.user is not None and ctx.user["id"] == m["user_id"]
    link = f'<p><a class="btn s" href="{e(ctx.url("livemap", "/app/m/" + str(m["id"])))}">{ctx.t("Open in my maps", "In meinen Karten öffnen")}</a></p>' if own else ""
    body = f"""<section class="card narrow"><h1>{e(m["name"])}</h1>
<p>{ctx.t("This map is being prepared.", "Diese Karte wird vorbereitet.")}</p>
{bar(progress)}<p class="mut small">{ctx.t("Come back later - the page shows the map as soon as it is ready.", "Schau später wieder vorbei – die Seite zeigt die Karte, sobald sie fertig ist.")}</p>{link}</section>"""
    return layout(ctx, m["name"], body)


def bar(progress: int, label: str = "") -> str:
    p = max(0, min(100, int(progress)))
    return (f'<div class="bar-wrap"><progress max="100" value="{p}">{p} %</progress><span class="pct">{p} %{(" · " + e(label)) if label else ""}</span></div>')


# -------------------------------------------------------------------- dashboard
def state_text(ctx, m, progress: int | None = None) -> str:
    s = m["state"]
    if s == "ready":
        return ctx.t("connected", "verbunden")
    if s == "preparing":
        return ctx.t(f"being prepared {progress} %", f"wird vorbereitet {progress} %") if progress is not None else ctx.t("being prepared", "wird vorbereitet")
    return ctx.t("disconnected", "getrennt")


def vis_text(ctx, v: str) -> str:
    return {"private": ctx.t("private", "privat"), "link": ctx.t("by link", "per Link"), "public": ctx.t("public", "öffentlich")}.get(v, v)


def dashboard(ctx, maps, shared, usage: dict) -> str:
    svc = ctx.svc
    banner = ""
    if not ctx.user["verified_at"]:
        sent = ctx.req.query.get("sent") == "1"
        banner = (f'<div class="alert warn">{ctx.t("Please confirm your e-mail address - then you can connect maps.", "Bitte bestätige deine E-Mail-Adresse – dann kannst du Karten verbinden.")} '
                  + ("" if sent else form(ctx, ctx.url("account", "/resend"), f'<button class="link">{ctx.t("Send the link again", "Link erneut schicken")}</button>', "inline"))
                  + (f' <b>{ctx.t("Sent.", "Verschickt.")}</b>' if sent else "") + "</div>")
    if usage["over_views"]:
        banner += f'<div class="alert warn">{ctx.t("Lots of visitors today - your maps show cached tiles. Higher tier?", "Viel Besuch heute – deine Karten zeigen zwischengespeicherte Kacheln. Höhere Stufe?")} <a href="{e(ctx.url("livemap", "/app/plan"))}">{ctx.t("Tiers", "Stufen")}</a></div>'
    tier = svc.tier(ctx.user)
    occ = ctx.t(f"{usage['maps']} of {usage['maps_max']} places · {usage['public']} of {usage['public_max']} public",
                f"{usage['maps']} von {usage['maps_max']} Plätzen · {usage['public']} von {usage['public_max']} öffentlich")
    hint = ""
    if usage["maps"] >= usage["maps_max"]:
        hint = f' · <a href="{e(ctx.url("livemap", "/pricing"))}">{ctx.t("more places with a higher tier", "mehr Plätze mit höherer Stufe")}</a>'
    tiles = ""
    for m in maps:
        p = svc.progress(m)
        views = svc.db.value("SELECT SUM(count) FROM views WHERE map_id = ? AND day = ?", m["id"], mapsmod.day(svc.now()), default=0)
        views30 = svc.db.value("SELECT SUM(count) FROM views WHERE map_id = ? AND day > ?", m["id"], mapsmod.day(svc.now() - 30 * 86400), default=0)
        has = svc.db.one("SELECT 1 FROM tiles WHERE map_id = ? LIMIT 1", m["id"])
        prev = (f'<img class="pv" src="/app/m/{m["id"]}/preview.png" alt="" loading="lazy" width="128" height="128">' if has
                else '<span class="pv" aria-hidden="true"></span>')
        tiles += f"""<a class="c mapcard" href="/app/m/{m['id']}">{prev}<div><b>{e(m["name"])}</b>
<span class="state s-{e(m["state"])}">{e(state_text(ctx, m))}</span>{bar(p) if m["state"] == "preparing" else ""}
<span class="mut small">{e(vis_text(ctx, m["visibility"]))} · {ctx.t("views today", "Aufrufe heute")} {num(ctx, views)} · 30 {ctx.t("days", "Tage")} {num(ctx, views30)}<br>
{ctx.t("updated", "aktualisiert")} {e(when(ctx, m["tiles_at"]))}</span></div></a>"""
    for _ in range(max(0, usage["maps_max"] - len(maps)) if usage["maps_max"] < 50 else 0):
        tiles += f'<a class="c mapcard empty" href="/app/connect"><span>+ {ctx.t("Connect a map", "Karte verbinden")}</span></a>'
    sh = ""
    if shared:
        sh = f'<h2>{ctx.t("Shared with me", "Mit mir geteilt")}</h2><ul class="plain">' + "".join(
            f'<li><a href="/m/{e(m["slug"])}/">{e(m["name"])}</a> <span class="mut small">{e(state_text(ctx, m))}</span></li>' for m in shared) + "</ul>"
    body = f"""{banner}<div class="head"><h1>{ctx.t("My maps", "Meine Karten")}</h1>
<a class="btn" href="/app/connect">{ctx.t("Connect a map", "Karte verbinden")}</a></div>
<p class="occ"><b>{e(occ)}</b> · {e(tier["name"])}{hint}</p>
<div class="maps">{tiles}</div>
<p class="links"><a href="/app/archive">{ctx.t("Archive", "Puffer")} ({usage['archived']})</a> · <a href="/app/plan">{ctx.t("Plan and usage", "Stufe und Nutzung")}</a> · <a href="/app/account">{ctx.t("Account", "Konto")}</a></p>
{sh}"""
    return layout(ctx, ctx.t("My maps", "Meine Karten"), body, wide=True)


def visibility_inputs(ctx, current: str, usage: dict | None = None) -> str:
    out = '<fieldset class="vis"><legend>' + ctx.t("Who sees the map?", "Wer sieht die Karte?") + "</legend>"
    for v, en, de in (("private", "Private - only you and accounts you share it with", "Privat – nur du und Konten, mit denen du teilst"),
                      ("link", "By link - everyone with the secret link", "Per Link – alle mit dem geheimen Link"),
                      ("public", "Public - everyone with the address", "Öffentlich – alle mit der Adresse")):
        chk = " checked" if v == current else ""
        out += f'<label class="radio"><input type="radio" name="visibility" value="{v}"{chk}> <span>{ctx.t(en, de)}</span></label>'
    if usage:
        used = ctx.t("Public: {0} of {1} used.", "Öffentlich: {0} von {1} belegt.").format(usage["public"], usage["public_max"])
        out += f'<p class="mut small">{used}</p>'
    return out + "</fieldset>"


def steps(ctx, active: int) -> str:
    names = [ctx.t("Name and visibility", "Name und Sichtbarkeit"), ctx.t("One-time code", "Einmal-Code"),
             ctx.t("Command in game", "Befehl im Spiel"), ctx.t("Being prepared", "Wird vorbereitet")]
    return '<ol class="steps">' + "".join(f'<li class="{"on" if i + 1 == active else ("done" if i + 1 < active else "")}">{e(n)}</li>' for i, n in enumerate(names)) + "</ol>"


def connect_form(ctx, usage: dict, tier: dict, f: dict, archived=None, slot_until: int = 0) -> str:
    lock = ""
    if slot_until:
        lock = f'<p class="alert warn">{ctx.t("All places are in use or locked. Next free place:", "Alle Plätze sind belegt oder gesperrt. Nächster freier Platz:")} {e(when(ctx, slot_until))}</p>'
    elif usage["maps"] >= usage["maps_max"]:
        lock = f'<p class="alert warn">{ctx.t("All places are in use.", "Alle Plätze sind belegt.")} <a href="/pricing">{ctx.t("Higher tier", "Höhere Stufe")}</a></p>'
    verify = "" if ctx.user["verified_at"] else f'<p class="alert warn">{ctx.t("Please confirm your e-mail address first.", "Bitte bestätige zuerst deine E-Mail-Adresse.")}</p>'
    slug = ""
    if tier["custom_name"]:
        slug = (f'<label>{ctx.t("Own address name (optional)", "Eigener Adressname (optional)")}<span class="prefix">{e(ctx.url("livemap", "/m/"))}'
                f'<input name="slug" value="{e(f.get("slug", ""))}" pattern="[a-z0-9][a-z0-9-]{{1,30}}[a-z0-9]" maxlength="32"></span></label>')
    re_note = ""
    hidden = ""
    if archived is not None:
        re_note = f'<p class="alert ok">{ctx.t("Reconnecting from the archive: the stored tiles are checked, only differences upload.", "Wieder verbinden aus dem Puffer: Die gespeicherten Kacheln werden geprüft, nur Abweichungen werden hochgeladen.")}</p>'
        hidden = f'<input type="hidden" name="archive" value="{archived["id"]}">'
    free_rule = ""
    if tier["slot_lock_days"]:
        free_rule = f'<p class="mut small">{ctx.t("Free: a place can be taken again at most once per 7 days - choose the world well.", "Free: Ein Platz lässt sich höchstens einmal pro 7 Tage neu belegen – wähle die Welt mit Bedacht.")}</p>'
    inner = form(ctx, "/app/connect", f"""{hidden}
<label>{ctx.t("Map name", "Kartenname")}<input name="name" value="{e(f.get("name", ""))}" required maxlength="40" placeholder="{ctx.t("e.g. Survival server", "z. B. Survival-Server")}"></label>
{visibility_inputs(ctx, f.get("visibility", "private"), usage)}{slug}
<button class="btn">{ctx.t("Make a code", "Code erzeugen")}</button>""", "stack")
    body = f"""<h1>{ctx.t("Connect a map", "Karte verbinden")}</h1>{steps(ctx, 1)}{verify}{lock}{alerts(ctx)}{re_note}
<section class="card">{inner}{free_rule}</section>"""
    return layout(ctx, ctx.t("Connect a map", "Karte verbinden"), body)


def connect_code(ctx, code: str, until: int, ref: str) -> str:
    cmd = f"/simplelivemap connect {code}"
    body = f"""<h1>{ctx.t("Connect a map", "Karte verbinden")}</h1>{steps(ctx, 2)}
<section class="card center" id="connect" data-ref="{e(ref)}" data-left="{max(0, until - ctx.svc.now())}">
<p>{ctx.t("Your one-time code (valid 15 minutes):", "Dein Einmal-Code (gilt 15 Minuten):")}</p>
<p class="code" aria-label="code">{e(code)}</p>
<p>{ctx.t("In game, as operator (or as owner of the single-player world), type:", "Im Spiel als Operator (oder als Besitzer der Einzelspielerwelt) eingeben:")}</p>
<p><code class="cmd" id="cmd">{e(cmd)}</code> <button class="btn small s" type="button" data-copy="#cmd">{ctx.t("Copy", "Kopieren")}</button></p>
<p class="mut small" id="connect-state" data-waiting="{ctx.t("Waiting for the server … ({left} left)", "Warte auf den Server … (noch {left})")}"
 data-expired="{ctx.t("The code expired. Make a new one.", "Der Code ist abgelaufen. Erzeuge einen neuen.")}"
 data-connected="{ctx.t("Connected! The map is being prepared …", "Verbunden! Die Karte wird vorbereitet …")}">{ctx.t("Waiting for the server …", "Warte auf den Server …")}</p>
<p><a class="btn s hidden" id="connect-open" href="/app">{ctx.t("Open the map", "Karte öffnen")}</a></p>
</section>
<p class="mut small">{ctx.t("Nothing happens? The server needs the Simple Livemap mod and outgoing HTTPS to livemap.simplemods.de.", "Passiert nichts? Der Server braucht die Mod Simple Livemap und ausgehendes HTTPS zu livemap.simplemods.de.")}</p>"""
    return layout(ctx, ctx.t("Connect a map", "Karte verbinden"), body)


def map_page(ctx, m, progress: int, queue_pos: int, shares, tier: dict, stats: dict) -> str:
    view_url = ctx.url("livemap", f"/m/{m['slug']}/")
    link = view_url + ("#t=" + m["view_key"] if m["visibility"] == "link" else "")
    if m["state"] == "ready":
        frame = f'<iframe class="viewer" src="/m/{e(m["slug"])}/{"#t=" + e(m["view_key"]) if m["visibility"] == "link" else ""}" title="{e(m["name"])}" loading="lazy"></iframe>'
    elif m["state"] == "preparing":
        q = f' · {ctx.t("queue place", "Warteschlange Platz")} {queue_pos}' if queue_pos else ""
        frame = (f'<div class="c prep"><b>{ctx.t("Map is being prepared", "Karte wird vorbereitet")}</b>{bar(progress, q.strip(" ·"))}'
                 f'<p class="mut small">{ctx.t("First the server uploads every tile, then only changes. The minimum time of your tier protects against abuse.", "Erst lädt der Server alle Kacheln hoch, danach nur Änderungen. Die Mindestzeit deiner Stufe schützt vor Missbrauch.")}</p></div>')
    else:
        frame = f'<div class="c prep"><b>{ctx.t("Disconnected", "Getrennt")}</b><p>{ctx.t("This map waits in the archive until", "Diese Karte wartet im Puffer bis")} {e(when(ctx, m["archive_until"]))}.</p></div>'
    share_rows = "".join(
        f'<li>{e(s["name"] or s["email"])} <span class="mut small">{e(s["email"])}</span> '
        + form(ctx, f"/app/m/{m['id']}", f'<input type="hidden" name="action" value="unshare"><input type="hidden" name="user" value="{s["id"]}"><button class="link">{ctx.t("remove", "entfernen")}</button>', "inline")
        + "</li>" for s in shares)
    limit = ctx.t("unlimited", "unbegrenzt") if tier["shares"] < 0 else str(tier["shares"])
    slug = ""
    if tier["custom_name"]:
        slug = (f'<label>{ctx.t("Address name", "Adressname")}<span class="prefix">{e(ctx.url("livemap", "/m/"))}'
                f'<input name="slug" value="{e(m["slug"])}" pattern="[a-z0-9][a-z0-9-]{{1,30}}[a-z0-9]" maxlength="32"></span></label>')
    link_box = ""
    if m["visibility"] in ("link", "public"):
        link_box = (f'<label>{ctx.t("Link to share", "Link zum Teilen")}<span class="copyrow"><input readonly id="share-link" value="{e(link)}">'
                    f'<button class="btn small s" type="button" data-copy="#share-link">{ctx.t("Copy", "Kopieren")}</button></span></label>')
        if m["visibility"] == "link":
            link_box += form(ctx, f"/app/m/{m['id']}", f'<input type="hidden" name="action" value="newkey"><button class="link">{ctx.t("Make a new link (the old one stops working)", "Neuen Link erzeugen (der alte geht nicht mehr)")}</button>', "inline")
    dpa = ""
    if not ctx.svc.user(m["user_id"])["dpa_at"]:
        dpa = (f'<label class="check"><input type="checkbox" name="dpa" value="1"> <span class="small">{ctx.t(
            "For players: I accept the processing agreement for my players' names and positions and have informed my players.",
            "Für Spieler: Ich nehme die Vereinbarung zur Verarbeitung der Namen und Positionen meiner Spieler an und habe meine Spieler informiert.")}</span></label>')
    settings = form(ctx, f"/app/m/{m['id']}", f"""<input type="hidden" name="action" value="save">
<label>{ctx.t("Name", "Name")}<input name="name" value="{e(m["name"])}" required maxlength="40"></label>
{visibility_inputs(ctx, m["visibility"])}{slug}
<label class="check"><input type="checkbox" name="players" value="1"{" checked" if m["show_players"] else ""}> <span>{ctx.t(
        "Show players (name and position of online players, latest state only)", "Spieler zeigen (Name und Position der Online-Spieler, nur der letzte Stand)")}</span></label>
{dpa}<button class="btn small">{ctx.t("Save", "Speichern")}</button>""", "stack")
    danger = ""
    if m["state"] != "archived":
        danger = form(ctx, f"/app/m/{m['id']}", f'<input type="hidden" name="action" value="disconnect"><button class="btn small danger">{ctx.t("Disconnect", "Trennen")}</button>',
                      "inline", confirm=ctx.t("Disconnect this map? It moves to the archive; connecting again means uploading again.", "Karte trennen? Sie wandert in den Puffer; wieder verbinden heißt neu hochladen."))
    body = f"""<div class="head"><h1>{e(m["name"])}</h1><span class="state s-{e(m["state"])}">{e(state_text(ctx, m, progress))}</span>
<a class="btn small s" href="{e(view_url)}{"#t=" + e(m["view_key"]) if m["visibility"] == "link" else ""}" target="_blank" rel="noopener">{ctx.t("Open full screen", "Vollbild öffnen")}</a></div>
{alerts(ctx)}
<div class="split"><div class="main">{frame}
<p class="mut small">{ctx.t("Tiles", "Kacheln")} {num(ctx, stats["tiles"])} · {size(m["bytes"])} · {ctx.t("views today", "Aufrufe heute")} {num(ctx, stats["views_today"])} · 30 {ctx.t("days", "Tage")} {num(ctx, stats["views_30"])} · {ctx.t("updated", "aktualisiert")} {e(when(ctx, m["tiles_at"]))}</p></div>
<aside class="c settings"><h2>{ctx.t("Settings", "Einstellungen")}</h2>{settings}{link_box}
<h3>{ctx.t("Share with accounts", "Mit Konten teilen")} <span class="mut small">({len(shares)} / {e(limit)})</span></h3>
<ul class="plain">{share_rows}</ul>
{form(ctx, f"/app/m/{m['id']}", f'<input type="hidden" name="action" value="share"><span class="copyrow"><input type="email" name="email" required maxlength="254" placeholder="{ctx.t("e-mail of the account", "E-Mail des Kontos")}"><button class="btn small s">{ctx.t("Share", "Teilen")}</button></span>')}
<h3>{ctx.t("Disconnect", "Trennen")}</h3><p class="mut small">{ctx.t("The map moves to the archive (data stays stored).", "Die Karte wandert in den Puffer (Daten bleiben gespeichert).")}</p>{danger}
</aside></div>"""
    return layout(ctx, m["name"], body, wide=True)


def archive(ctx, rows, usage: dict) -> str:
    items = ""
    for m in rows:
        items += (f'<li class="c"><b>{e(m["name"])}</b> <span class="mut small">{ctx.t("disconnected", "getrennt")} {e(when(ctx, m["archived_at"]))} · '
                  f'{ctx.t("kept until", "gespeichert bis")} {e(when(ctx, m["archive_until"]))} · {size(m["bytes"])}</span>'
                  f'<span class="row"><a class="btn small" href="/app/connect?archive={m["id"]}">{ctx.t("Connect again", "Wieder verbinden")}</a>'
                  + form(ctx, "/app/archive", f'<input type="hidden" name="map" value="{m["id"]}"><input type="hidden" name="action" value="delete"><button class="btn small danger">{ctx.t("Delete", "Löschen")}</button>',
                         "inline", confirm=ctx.t("Delete this map and its tiles for good?", "Diese Karte samt Kacheln endgültig löschen?")) + "</span></li>")
    if not items:
        items = f'<li class="mut">{ctx.t("The archive is empty.", "Der Puffer ist leer.")}</li>'
    body = f"""<h1>{ctx.t("Archive", "Puffer")}</h1>
<p class="sub">{ctx.t(f"Disconnected maps stay stored here ({usage['archived']} of {usage['archive_max']}). Connecting the same world again only uploads differences.",
                       f"Getrennte Karten bleiben hier gespeichert ({usage['archived']} von {usage['archive_max']}). Verbindest du dieselbe Welt wieder, werden nur Abweichungen hochgeladen.")}</p>
<ul class="plain stack">{items}</ul>"""
    return layout(ctx, ctx.t("Archive", "Puffer"), body)


def plan(ctx, usage: dict) -> str:
    t = ctx.svc.tier(ctx.user)

    def meter(label, used, mx):
        pct = 0 if mx <= 0 else min(100, int(100 * used / mx))
        return f'<div class="meter"><span>{e(label)}</span><progress max="100" value="{pct}"></progress><b>{num(ctx, used)} / {num(ctx, mx)}</b></div>'
    body = f"""<h1>{ctx.t("Plan and usage", "Stufe und Nutzung")}</h1>{alerts(ctx)}
<section class="c"><p>{ctx.t("Your tier:", "Deine Stufe:")} <b>{e(t["name"])}</b></p>
{meter(ctx.t("Maps", "Karten"), usage["maps"], usage["maps_max"])}{meter(ctx.t("Public", "Öffentlich"), usage["public"], usage["public_max"])}
{meter(ctx.t("Views today", "Aufrufe heute"), usage["views_today"], usage["views_max"])}{meter(ctx.t("Archive", "Puffer"), usage["archived"], usage["archive_max"])}
<p class="mut small">{ctx.t("Views last 30 days:", "Aufrufe der letzten 30 Tage:")} {num(ctx, usage["views_30"])} · {ctx.t("storage", "Speicher")} {size(usage["bytes"])}</p></section>
<h2>{ctx.t("Change tier", "Stufe wechseln")}</h2>
<p class="alert warn">{ctx.t("Buying is coming soon. Test accounts can be set to Pro/Max/Ultra by the developer.", "Kaufen kommt bald. Testkonten setzt der Entwickler manuell auf Pro/Max/Ultra.")}</p>
{interval_toggle(ctx)}{tier_table(ctx, True)}
<h2>{ctx.t("Invoices", "Rechnungen")}</h2><p class="mut">{ctx.t("Coming soon.", "Coming soon.")}</p>"""
    return layout(ctx, ctx.t("Plan", "Stufe"), body, wide=True)


def account(ctx, sessions: int, usage: dict) -> str:
    u = ctx.user
    lang_opts = "".join(f'<option value="{v}"{" selected" if u["lang"] == v else ""}>{n}</option>' for v, n in (("", ctx.t("automatic", "automatisch")), ("de", "Deutsch"), ("en", "English")))
    body = f"""<h1>{ctx.t("Account", "Konto")}</h1>{alerts(ctx)}
<div class="g2">
<section class="c"><h2>{ctx.t("Profile", "Profil")}</h2>
<p class="mut small">{e(u["email"])} · {ctx.t("confirmed", "bestätigt") if u["verified_at"] else ctx.t("not confirmed", "nicht bestätigt")} · {ctx.t("since", "seit")} {e(when(ctx, u["created_at"]))}</p>
{form(ctx, "/app/account", f'''<input type="hidden" name="action" value="profile">
<label>{ctx.t("Name", "Name")}<input name="name" value="{e(u["name"])}" required maxlength="40"></label>
<label>{ctx.t("Language", "Sprache")}<select name="lang">{lang_opts}</select></label>
<button class="btn small">{ctx.t("Save", "Speichern")}</button>''', "stack")}</section>
<section class="c"><h2>{ctx.t("Security", "Sicherheit")}</h2>
{form(ctx, "/app/account", f'''<input type="hidden" name="action" value="password">
<label>{ctx.t("Current password", "Aktuelles Passwort")}<input type="password" name="old" autocomplete="current-password" required maxlength="256"></label>
<label>{ctx.t("New password", "Neues Passwort")}<input type="password" name="new" autocomplete="new-password" required minlength="10" maxlength="256"></label>
<label>{ctx.t("Again", "Wiederholen")}<input type="password" name="new2" autocomplete="new-password" required minlength="10" maxlength="256"></label>
<button class="btn small">{ctx.t("Change password", "Passwort ändern")}</button>''', "stack")}
<p class="mut small">{ctx.t(f"Active sessions: {sessions}", f"Aktive Sitzungen: {sessions}")} · {ctx.t("Two-factor login comes later.", "Zwei-Faktor-Anmeldung kommt später.")}</p>
{form(ctx, "/app/account", f'<input type="hidden" name="action" value="others"><button class="btn small s">{ctx.t("Log out everywhere else", "Überall sonst abmelden")}</button>', "inline")}</section>
<section class="c"><h2>{ctx.t("Your data", "Deine Daten")}</h2>
<p>{ctx.t("The export holds everything stored about your account and maps (the tiles are pictures of your world and stay out). Player positions are kept only in memory, latest state. Details in the privacy notice.",
          "Der Export enthält alles, was zu deinem Konto und deinen Karten gespeichert ist (die Kacheln sind Bilder deiner Welt und bleiben draußen). Spielerpositionen nur im Arbeitsspeicher, letzter Stand. Einzelheiten in den Datenschutzhinweisen.")}</p>
<p><a class="btn small s" href="/app/account/export">{ctx.t("Download data export (JSON)", "Daten-Export herunterladen (JSON)")}</a></p></section>
<section class="c"><h2>{ctx.t("Delete account", "Konto löschen")}</h2>
<p class="mut small">{ctx.t("Deletes the account, all maps and tiles at once. This cannot be undone.", "Löscht Konto, alle Karten und Kacheln sofort. Das lässt sich nicht rückgängig machen.")}</p>
{form(ctx, "/app/account", f'''<input type="hidden" name="action" value="delete">
<label>{ctx.t("Password", "Passwort")}<input type="password" name="password" autocomplete="current-password" required maxlength="256"></label>
<label>{ctx.t("Type DELETE", "DELETE eintippen")}<input name="confirm" required pattern="DELETE" maxlength="6"></label>
<button class="btn small danger">{ctx.t("Delete account for good", "Konto endgültig löschen")}</button>''', "stack", confirm=ctx.t("Really delete the account and all maps?", "Konto und alle Karten wirklich löschen?"))}</section>
</div>"""
    return layout(ctx, ctx.t("Account", "Konto"), body, wide=True)


# -------------------------------------------------------------------- admin
def admin(ctx, users, maps, owners: dict, queue, storage: dict, flags, outbox) -> str:
    order = ctx.cfg.tiers["order"]
    urows = ""
    for u in users:
        opts = "".join(f'<option value="{k}"{" selected" if u["tier"] == k else ""}>{k}</option>' for k in order)
        tier_form = form(ctx, "/admin", f'<input type="hidden" name="action" value="tier"><input type="hidden" name="user" value="{u["id"]}"><select name="tier" aria-label="tier">{opts}</select><button class="btn small s">{ctx.t("set", "setzen")}</button>', "inline")
        toggle = form(ctx, "/admin", f'<input type="hidden" name="action" value="{"enable" if u["disabled"] else "disable"}"><input type="hidden" name="user" value="{u["id"]}"><button class="link">{ctx.t("enable", "freigeben") if u["disabled"] else ctx.t("block", "sperren")}</button>', "inline")
        urows += (f'<tr><td>{u["id"]}</td><td>{e(u["email"])}{" ✓" if u["verified_at"] else ""}</td><td>{e(u["role"])}</td><td>{tier_form}</td>'
                  f'<td>{u["active"]}</td><td>{u["connections_total"]}</td><td>{e(when(ctx, u["created_at"]))}</td><td>{toggle if u["id"] != ctx.user["id"] else ""}</td></tr>')
    mrows = ""
    for m in maps:
        act = ""
        if m["state"] == "preparing":
            act += form(ctx, "/admin", f'<input type="hidden" name="action" value="ready"><input type="hidden" name="map" value="{m["id"]}"><button class="btn small">{ctx.t("ready now", "sofort fertig")}</button>', "inline")
        if m["state"] != "archived":
            act += form(ctx, "/admin", f'<input type="hidden" name="action" value="disconnect"><input type="hidden" name="map" value="{m["id"]}"><button class="link">{ctx.t("disconnect", "trennen")}</button>', "inline")
        mrows += (f'<tr><td>{m["id"]}</td><td><a href="/app/m/{m["id"]}">{e(m["name"])}</a><br><span class="mut small">/m/{e(m["slug"])}</span></td><td>{e(owners.get(m["user_id"], "?"))}</td>'
                  f'<td>{e(m["state"])} {ctx.svc.progress(m)} %</td><td>{e(m["visibility"])}</td><td>{size(m["bytes"])}</td><td>{act}</td></tr>')
    qrows = "".join(f'<li>#{q["id"]} {e(q["name"])} <span class="mut small">{e(owners.get(q["user_id"], "?"))} · {e(q["user_tier"])} · {e(when(ctx, q["connected_at"]))}</span></li>' for q in queue) or f'<li class="mut">{ctx.t("empty", "leer")}</li>'
    frows = "".join(f'<li><b>{e(f["kind"])}</b> ({f["count"]}): {e(f["detail"])}</li>' for f in flags) or f'<li class="mut">{ctx.t("nothing noticeable", "nichts auffällig")}</li>'
    orows = ""
    if outbox:
        orows = f'<h2>{ctx.t("Outbox (development)", "Ausgang (Entwicklung)")}</h2><ul class="plain">' + "".join(
            f'<li><b>{e(o["to"])}</b> – {e(o["subject"])}<pre class="mail">{e(o["text"])}</pre></li>' for o in outbox) + "</ul>"
    pct = 0 if not storage["quota"] else int(100 * storage["used"] / storage["quota"])
    body = f"""<h1>Admin</h1>{alerts(ctx)}
<div class="g3"><div class="c"><b>{ctx.t("Storage", "Speicher")}</b><progress max="100" value="{pct}"></progress><p class="small">{size(storage["used"])} / {size(storage["quota"])} · {ctx.t("disk", "Platte")} {storage["disk"]} %</p>
{form(ctx, "/admin", f'<input type="hidden" name="action" value="maintenance"><button class="btn small s">{ctx.t("Run housekeeping", "Aufräumen")}</button>', "inline")}</div>
<div class="c"><b>{ctx.t("Queue (preparing)", "Warteschlange (Vorbereitung)")}</b><ol class="plain small">{qrows}</ol><p class="mut small">{ctx.t("upload places", "Upload-Plätze")}: {ctx.cfg["prep_slots"]}</p></div>
<div class="c"><b>{ctx.t("Noticeable patterns", "Auffällige Muster")}</b><ul class="plain small">{frows}</ul></div></div>
<h2>{ctx.t("Users", "Nutzer")}</h2><div class="tw"><table class="admin"><thead><tr><th>#</th><th>E-Mail</th><th>{ctx.t("Role", "Rolle")}</th><th>{ctx.t("Tier", "Stufe")}</th><th>{ctx.t("Maps", "Karten")}</th><th>{ctx.t("Connects", "Verbindungen")}</th><th>{ctx.t("Created", "Erstellt")}</th><th></th></tr></thead><tbody>{urows}</tbody></table></div>
<h2>{ctx.t("Maps", "Karten")}</h2><div class="tw"><table class="admin"><thead><tr><th>#</th><th>{ctx.t("Map", "Karte")}</th><th>{ctx.t("Owner", "Besitzer")}</th><th>{ctx.t("State", "Zustand")}</th><th>{ctx.t("Visibility", "Sichtbarkeit")}</th><th>{ctx.t("Size", "Größe")}</th><th></th></tr></thead><tbody>{mrows}</tbody></table></div>
{orows}"""
    return layout(ctx, "Admin", body, wide=True)
