"""Pages of Simple Mods Pro on account.simplemods.de (N108): plans, order summary with the consents and the button
"zahlungspflichtig bestellen", thanks, manage (portal), cancel button page (section 312k BGB) and the early-access download area.
Same rules as pages.py: every value through ``e()``, no inline script/style."""
from __future__ import annotations

from .billing_mail import interval_text, vat_note, waiver_text
from .pages import alerts, e, euro, form, layout, when

BRAND = "Simple Mods"


def page(ctx, title: str, body: str, wide: bool = False) -> str:
    return layout(ctx, title, body, wide=wide, brand=BRAND)


def legal_link(ctx, key: str, en: str, de: str) -> str:
    url = ctx.cfg["legal"].get(key)
    label = ctx.t(en, de)
    return f'<a href="{e(url)}" target="_blank" rel="noopener">{label}</a>' if url else f'{label} <span class="mut small">({ctx.t("draft, follows before the start", "Entwurf, folgt vor dem Start")})</span>'


def perk_lines(ctx, plan: dict) -> list[str]:
    tiers = ctx.cfg.tiers["tiers"]
    names = ctx.cfg.plans.get("perks", {})
    out = []
    for perk in plan.get("perks", []):
        text = (names.get(perk) or {}).get(ctx.lang) or perk
        lt = tiers.get(plan.get("livemap_tier") or "") or {}
        out.append(text.replace("{livemap}", lt.get("name", "")))
        if perk == "livemap" and lt:
            out.append(ctx.t(f"{lt['maps']} maps, live every {lt['live_seconds']} s", f"{lt['maps']} Karten, live alle {lt['live_seconds']} s"))
    return out


def price_block(ctx, plan: dict) -> str:
    months = ctx.cfg.tiers.get("yearly_months_paid", 10)
    pm, py = plan["price_month"], plan["price_year"]
    return (f'<s class="was" data-show="year" hidden><span class="vh">{ctx.t("instead of", "statt")} </span>{euro(ctx, pm * 12)}</s>'
            f'<span class="price" data-month="{e(euro(ctx, pm))}" data-year="{e(euro(ctx, py))}">{euro(ctx, pm)}</span>'
            f'<span class="per" data-month="{ctx.t("per month", "pro Monat")}" data-year="{ctx.t(f"per year ({months} × monthly)", f"pro Jahr ({months} × Monatspreis)")}">{ctx.t("per month", "pro Monat")}</span>')


def toggle(ctx) -> str:
    return (f'<div class="toggle" role="group" aria-label="{ctx.t("Billing period", "Abrechnung")}">'
            f'<button type="button" data-interval="month" aria-pressed="true">{ctx.t("Monthly", "Monatlich")}</button>'
            f'<button type="button" data-interval="year" aria-pressed="false">{ctx.t("Yearly", "Jährlich")}</button></div>')


def index(ctx) -> str:
    svc = ctx.svc
    plans = ctx.cfg.plans
    on = svc.billing_on()
    vat = vat_note(svc.vat(), ctx.lang)
    cards = ""
    for k in plans["order"]:
        p = plans["plans"][k]
        if on:
            buy = (f'<a class="btn" data-show="month" href="{e(ctx.url("account", f"/pro/order?plan={k}&interval=month"))}">{ctx.t("Choose", "Auswählen")}</a>'
                   f'<a class="btn" data-show="year" hidden href="{e(ctx.url("account", f"/pro/order?plan={k}&interval=year"))}">{ctx.t("Choose", "Auswählen")}</a>')
        else:
            buy = f'<button class="btn soon" disabled aria-disabled="true">Coming soon</button>'
        items = "".join(f"<li>{e(x)}</li>" for x in perk_lines(ctx, p))
        cards += f'<div class="c tier plan" id="plan-{e(k)}"><b>{e(p["name"])}</b>{price_block(ctx, p)}<span class="mut small">{e(vat)}</span><ul>{items}</ul>{buy}</div>'
    months = ctx.cfg.tiers.get("yearly_months_paid", 10)
    body = f"""<h1>Simple Mods Pro</h1>
<p class="sub">{ctx.t("One subscription for your Simple Mods account: hosted Simple Livemap tiers and early access to new versions.",
                      "Ein Abo für dein Simple-Mods-Konto: gehostete Simple-Livemap-Stufen und Frühzugang zu neuen Versionen.")}</p>
{'' if on else f'<p class="alert warn">{ctx.t("Buying is coming soon.", "Kaufen kommt bald.")}</p>'}
{toggle(ctx)}
<p class="mut small">{ctx.t(f"Yearly in advance: 12 months for the price of {months} (struck through: 12 × the monthly price, a comparison, not a former price). After the year it runs on monthly and can be cancelled every month.",
                            f"Jährlich im Voraus: 12 Monate zum Preis von {months} (durchgestrichen: 12 × Monatspreis, ein Vergleich, kein früherer Preis). Nach dem Jahr läuft es monatlich weiter und ist monatlich kündbar.")}</p>
<div class="g3">{cards}</div>
<div class="g2">
<div class="c"><b>{ctx.t("Early access", "Frühzugang")}</b><p>{ctx.t(
        "New versions are available here first. Modrinth and CurseForge get each version with the next update (always one version behind) - starting with the first update after the release. No promise of dates or content.",
        "Neue Versionen gibt es hier zuerst. Modrinth und CurseForge bekommen jede Version mit dem nächsten Update (immer eine Version zurück) – ab dem ersten Update nach dem Release. Keine Zusage zu Terminen oder Inhalten.")}</p></div>
<div class="c"><b>{ctx.t("Cancel", "Kündigen")}</b><p>{ctx.t(
        "Monthly: any time to the end of the paid month. Yearly: to the end of the 12 months; then monthly. Button “Cancel contracts here” at the bottom of every page.",
        "Monatlich: jederzeit zum Ende des bezahlten Monats. Jährlich: zum Ende der 12 Monate, danach monatlich. Schaltfläche „Verträge hier kündigen“ unten auf jeder Seite.")}</p></div>
</div>"""
    return page(ctx, "Simple Mods Pro", body)


def order(ctx, plan_key: str, interval: str, f: dict | None = None) -> str:
    svc = ctx.svc
    p = ctx.cfg.plans["plans"][plan_key]
    f = f or {}
    cents = p["price_month"] if interval == "month" else p["price_year"]
    per = ctx.t("per year", "pro Jahr") if interval == "year" else ctx.t("per month", "pro Monat")
    other = "month" if interval == "year" else "year"
    vat = vat_note(svc.vat(), ctx.lang)
    perks = "".join(f"<li>{e(x)}</li>" for x in perk_lines(ctx, p))
    days = int(svc.bcfg("withdrawal_days", 14))
    rows = [
        (ctx.t("Subscription", "Abo"), f"Simple Mods Pro {e(p['name'])}"),
        (ctx.t("Includes", "Leistungen"), f'<ul class="plain">{perks}</ul>'),
        (ctx.t("Term", "Laufzeit"), e(interval_text(interval, ctx.lang))),
        (ctx.t("Price", "Preis"), f'<b>{euro(ctx, cents)}</b> {per}<br><span class="mut small">{e(vat)}</span>'),
        (ctx.t("Payment", "Zahlung"), ctx.t("in advance for each period, via Stripe (payment data in the next step)", "im Voraus je Zeitraum, über Stripe (Zahlungsdaten im nächsten Schritt)")),
        (ctx.t("Withdrawal", "Widerruf"), ctx.t(f"14-day right of withdrawal for consumers – ", f"14 Tage Widerrufsrecht für Verbraucher – ") + legal_link(ctx, "withdrawal_url", "withdrawal notice", "Widerrufsbelehrung")),
    ]
    table = "".join(f'<tr><th scope="row">{k}</th><td>{v}</td></tr>' for k, v in rows)
    terms = (ctx.t("I have read and accept the ", "Ich habe die ") + legal_link(ctx, "terms_url", "terms", "AGB")
             + ctx.t(" and the ", " und die ") + legal_link(ctx, "withdrawal_url", "withdrawal notice", "Widerrufsbelehrung")
             + ctx.t(". I have taken note of the ", " gelesen und akzeptiere sie. Die ") + legal_link(ctx, "privacy_url", "privacy notice", "Datenschutzerklärung")
             + ctx.t(".", " habe ich zur Kenntnis genommen."))
    waiver = waiver_text(ctx.lang)
    chk = lambda name: " checked" if f.get(name) == "1" else ""  # noqa: E731
    inner = f"""<input type="hidden" name="plan" value="{e(plan_key)}"><input type="hidden" name="interval" value="{e(interval)}">
<label class="check"><input type="checkbox" name="terms" value="1" required{chk("terms")}><span>{terms} <b>*</b></span></label>
<label class="check"><input type="checkbox" name="waiver" value="1"{chk("waiver")}><span>{e(waiver)}<br><span class="mut small">{ctx.t(f"Voluntary. Without it the perks start after the withdrawal period ({days} days); the first payment is due anyway.", f"Freiwillig. Ohne dieses Häkchen starten die Vorzüge erst nach Ablauf der Widerrufsfrist ({days} Tage); die erste Zahlung ist trotzdem fällig.")}</span></span></label>
<p class="mut small">* {ctx.t("required", "Pflichtfeld")}</p>
<button class="btn order-button">zahlungspflichtig bestellen</button>"""
    body = f"""<h1>{ctx.t("Your order", "Deine Bestellung")}</h1>{alerts(ctx)}
<ol class="steps small"><li>{ctx.t("Choose plan", "Stufe wählen")}</li><li aria-current="step"><b>{ctx.t("Check and order", "Prüfen und bestellen")}</b></li><li>{ctx.t("Pay with Stripe", "Bezahlen mit Stripe")}</li></ol>
<section class="c"><div class="tw"><table class="summary">{table}</table></div>
<p class="small"><a href="{e(ctx.url("account", f"/pro/order?plan={plan_key}&interval={other}"))}">{ctx.t("Switch to yearly" if other == "year" else "Switch to monthly", "Zu jährlich wechseln" if other == "year" else "Zu monatlich wechseln")}</a> ·
<a href="{e(ctx.url("account", "/pro"))}">{ctx.t("Back: change plan", "Zurück: Stufe ändern")}</a></p></section>
<section class="c">{form(ctx, "/pro/order", inner, "stack")}</section>"""
    return page(ctx, ctx.t("Order", "Bestellung"), body)


def done(ctx, view: dict) -> str:
    eff = view["effective"]
    ent = view["ent"]
    if eff:
        state = ctx.t(f"Active: Simple Mods Pro {eff['plan'].title()}.", f"Aktiv: Simple Mods Pro {eff['plan'].title()}.")
    elif ent and ent["starts_at"] > ctx.svc.now():
        state = ctx.t(f"Booked. The perks start on {when(ctx, ent['starts_at'])} (after the withdrawal period).",
                      f"Gebucht. Die Vorzüge starten am {when(ctx, ent['starts_at'])} (nach Ablauf der Widerrufsfrist).")
    else:
        state = ctx.t("The payment is being confirmed – this takes a few seconds. Reload the page.", "Die Zahlung wird bestätigt – das dauert ein paar Sekunden. Seite neu laden.")
    body = f"""<section class="card narrow"><h1>{ctx.t("Thank you!", "Danke!")}</h1><p>{e(state)}</p>
<p>{ctx.t("The order confirmation with the contract text is on its way by mail.", "Die Bestellbestätigung mit dem Vertragstext ist per Mail unterwegs.")}</p>
<p><a class="btn" href="{e(ctx.url("account", "/pro/manage"))}">{ctx.t("My subscription", "Mein Abo")}</a>
<a class="btn s" href="{e(ctx.url("account", "/early"))}">{ctx.t("Early access", "Frühzugang")}</a></p></section>"""
    return page(ctx, ctx.t("Thank you", "Danke"), body)


STATUS = {"active": ("active", "aktiv"), "trialing": ("trial", "Testphase"), "past_due": ("payment open", "Zahlung offen"),
          "canceled": ("ended", "beendet"), "unpaid": ("unpaid", "unbezahlt"), "incomplete": ("not completed", "nicht abgeschlossen"),
          "incomplete_expired": ("expired", "abgelaufen")}


def manage(ctx, view: dict) -> str:
    sub, ent, eff = view["sub"], view["ent"], view["effective"]
    plans = ctx.cfg.plans["plans"]
    if not sub:
        inner = f"""<p>{ctx.t("You have no Simple Mods Pro subscription.", "Du hast kein Simple-Mods-Pro-Abo.")}</p>
<p><a class="btn" href="{e(ctx.url("account", "/pro"))}">{ctx.t("See the plans", "Stufen ansehen")}</a></p>"""
    else:
        st = STATUS.get(sub["status"], (sub["status"], sub["status"]))
        name = (plans.get(sub["plan"]) or {}).get("name", sub["plan"])
        lines = [(ctx.t("Plan", "Stufe"), f"Simple Mods Pro {e(name)}"), (ctx.t("Status", "Status"), e(ctx.t(*st))),
                 (ctx.t("Billing", "Abrechnung"), e(interval_text(sub["interval"], ctx.lang))),
                 (ctx.t("Paid until", "Bezahlt bis") if not sub["cancel_at_period_end"] else ctx.t("Ends on", "Endet am"), e(when(ctx, sub["period_end"])))]
        if ent and ent["starts_at"] > ctx.svc.now():
            lines.append((ctx.t("Perks from", "Vorzüge ab"), e(when(ctx, ent["starts_at"]))))
        if eff:
            lines.append((ctx.t("Perks", "Vorzüge"), e(", ".join(perk_lines(ctx, plans.get(eff["plan"]) or {})))))
        table = "".join(f'<tr><th scope="row">{k}</th><td>{v}</td></tr>' for k, v in lines)
        portal = form(ctx, "/pro/manage", f'<input type="hidden" name="action" value="portal"><button class="btn">{ctx.t("Customer portal (payment method, invoices, change plan)", "Kundenportal (Zahlungsart, Rechnungen, Stufe wechseln)")}</button>', "inline")
        inner = f'<div class="tw"><table class="summary">{table}</table></div>{portal}'
    body = f"""<h1>{ctx.t("My subscription", "Mein Abo")}</h1>{alerts(ctx)}<section class="c">{inner}</section>
<section class="c"><h2>{ctx.t("Cancel", "Kündigen")}</h2><p>{ctx.t("You can cancel any time to the end of the paid period.", "Du kannst jederzeit zum Ende des bezahlten Zeitraums kündigen.")}</p>
<p><a class="btn s" href="{e(ctx.url("account", "/kuendigen"))}">{ctx.t("Cancel contracts here", "Verträge hier kündigen")}</a></p></section>"""
    return page(ctx, ctx.t("My subscription", "Mein Abo"), body)


def cancel(ctx, f: dict, result: dict | None = None) -> str:
    """Section 312k BGB: confirmation page behind the button "Verträge hier kündigen" with the button "Jetzt kündigen"."""
    if result is not None:
        body = f"""<section class="card narrow"><h1>{ctx.t("Cancellation received", "Kündigung eingegangen")}</h1>
<p>{ctx.t("Received on", "Eingang am")} <b>{e(when(ctx, result["at"]))}</b>. {ctx.t("The end of the contract is in the confirmation mail.", "Das Vertragsende steht in der Bestätigungsmail.")}</p>
<p>{ctx.t("A confirmation with date and time is on its way to the e-mail address you gave. If an account with a subscription belongs to it, it ends at the end of the paid period.",
          "Eine Bestätigung mit Datum und Uhrzeit ist an die angegebene E-Mail-Adresse unterwegs. Gehört dazu ein Konto mit Abo, endet es zum Ende des bezahlten Zeitraums.")}</p></section>"""
        return page(ctx, ctx.t("Cancellation received", "Kündigung eingegangen"), body)
    user = ctx.user
    email = f.get("email") or (user["email"] if user else "")
    kind = f.get("kind") or "ordinary"
    inner = f"""<label>{ctx.t("Your e-mail address (of the account)", "Deine E-Mail-Adresse (des Kontos)")}<input type="email" name="email" required maxlength="254" value="{e(email)}" autocomplete="email"></label>
<label>{ctx.t("Contract", "Vertrag")}<select name="contract"><option value="pro">Simple Mods Pro</option></select></label>
<fieldset><legend>{ctx.t("Kind of cancellation", "Art der Kündigung")}</legend>
<label class="radio"><input type="radio" name="kind" value="ordinary"{" checked" if kind == "ordinary" else ""}> {ctx.t("Ordinary, at the earliest possible date", "Ordentlich, zum nächstmöglichen Zeitpunkt")}</label>
<label class="radio"><input type="radio" name="kind" value="extraordinary"{" checked" if kind == "extraordinary" else ""}> {ctx.t("Extraordinary (for good cause)", "Außerordentlich (aus wichtigem Grund)")}</label></fieldset>
<label>{ctx.t("Reason (only for extraordinary cancellation)", "Grund (nur bei außerordentlicher Kündigung)")}<textarea name="reason" maxlength="500" rows="3">{e(f.get("reason", ""))}</textarea></label>
<button class="btn danger">{ctx.t("Cancel now", "Jetzt kündigen")}</button>"""
    body = f"""<section class="card narrow"><h1>{ctx.t("Cancel contracts here", "Verträge hier kündigen")}</h1>{alerts(ctx)}
<p>{ctx.t("Cancel your Simple Mods Pro subscription here. You do not need to log in. The cancellation takes effect at the end of the paid period; you get a confirmation by mail right away.",
          "Hier kündigst du dein Simple-Mods-Pro-Abo. Eine Anmeldung ist nicht nötig. Die Kündigung wirkt zum Ende des bezahlten Zeitraums; du bekommst sofort eine Bestätigung per Mail.")}</p>
{form(ctx, "/kuendigen", inner, "stack")}</section>"""
    return page(ctx, ctx.t("Cancel contracts here", "Verträge hier kündigen"), body)


def early(ctx, releases: list[dict], allowed: bool) -> str:
    if not allowed:
        body = f"""<section class="card narrow"><h1>{ctx.t("Early access", "Frühzugang")}</h1>
<p>{ctx.t("New versions of the Simple mods are available here first - for Simple Mods Pro subscribers. Modrinth and CurseForge get them with the next update.",
          "Neue Versionen der Simple-Mods gibt es hier zuerst – für Abonnenten von Simple Mods Pro. Modrinth und CurseForge bekommen sie mit dem nächsten Update.")}</p>
<p><a class="btn" href="{e(ctx.url("account", "/pro"))}">Simple Mods Pro</a></p></section>"""
        return page(ctx, ctx.t("Early access", "Frühzugang"), body)
    rows = ""
    for r in releases:
        files = "".join(f'<li><a href="{e(ctx.url("account", "/early/f/" + x["name"]))}" download>{e(x["name"])}</a> <span class="mut small">{e(x.get("size_text", ""))}</span></li>'
                        for x in r.get("files", []))
        public = ctx.t("also public", "auch öffentlich") if r.get("public") else ctx.t("only here so far", "bisher nur hier")
        rows += f'<li class="c"><b>{e(r.get("version", ""))}</b> <span class="mut small">{e(r.get("date", ""))} · {public}</span><ul>{files}</ul></li>'
    body = f"""<h1>{ctx.t("Early access", "Frühzugang")}</h1>
<p class="sub">{ctx.t("Early versions without a promise of dates, content or stability; for your own use, please do not pass the files on.",
                      "Frühe Versionen ohne Zusage zu Terminen, Inhalten oder Stabilität; für deinen eigenen Gebrauch, bitte die Dateien nicht weitergeben.")}</p>
<ul class="plain stack">{rows or f'<li class="c">{ctx.t("No early version at the moment.", "Gerade gibt es keine frühe Version.")}</li>'}</ul>"""
    return page(ctx, ctx.t("Early access", "Frühzugang"), body)
