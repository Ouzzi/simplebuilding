"""Pages of Simple Mods Pro on account.simplemods.de (N108): plans, order summary with the consents and the button
"zahlungspflichtig bestellen", thanks, manage (portal), cancel button page (section 312k BGB) and the early-access download area.
Same rules as pages.py: every value through ``e()``, no inline script/style."""
from __future__ import annotations

from .billing_mail import interval_text, vat_note, waiver_text
from .pages import alerts, e, euro, form, layout, when

BRAND = "Simple Mods"


def page(ctx, title: str, body: str, wide: bool = False) -> str:
    return layout(ctx, title, body, wide=wide, brand=BRAND)


PROVIDER_NAMES = {"paddle": "Paddle", "stripe": "Stripe"}


def provider_label(ctx) -> str:
    return PROVIDER_NAMES.get(ctx.svc.provider_name(), ctx.svc.provider_name().title())


def terms_label(ctx) -> tuple[str, str]:
    """Paddle = merchant of record: our terms are terms of use for account and services (addendum 4); Stripe: AGB."""
    return ("terms of use", "Nutzungsbedingungen") if ctx.svc.provider_name() == "paddle" else ("terms", "AGB")


def payment_row(ctx) -> str:
    if ctx.svc.provider_name() == "paddle":
        return ctx.t("in advance for each period, via Paddle: Paddle.com is the seller (merchant of record) and issues the invoice "
                     "(payment data in the next step; Paddle's buyer terms apply to the purchase)",
                     "im Voraus je Zeitraum, über Paddle: Verkäufer ist Paddle.com (Merchant of Record) und stellt die Rechnung aus "
                     "(Zahlungsdaten im nächsten Schritt; für den Kauf gelten die Käuferbedingungen von Paddle)")
    return ctx.t("in advance for each period, via Stripe (payment data in the next step)", "im Voraus je Zeitraum, über Stripe (Zahlungsdaten im nächsten Schritt)")


def legal_link(ctx, key: str, en: str, de: str) -> str:
    url = ctx.cfg["legal"].get(key)
    label = ctx.t(en, de)
    return f'<a href="{e(url)}" target="_blank" rel="noopener">{label}</a>' if url else f'{label} <span class="mut small">({ctx.t("linked before the start", "wird vor dem Start verlinkt")})</span>'


def perk_lines(ctx, plan: dict) -> list[str]:
    tiers = ctx.cfg.tiers["tiers"]
    names = ctx.cfg.plans.get("perks", {})
    out = []
    for perk in plan.get("perks", []):
        text = (names.get(perk) or {}).get(ctx.lang) or perk
        lt = tiers.get(plan.get("livemap_tier") or "") or {}
        lite = plan.get("lite") or {}
        lite_name = (ctx.cfg.plans.get("lite", {}).get(lite.get("kind") or "") or {}).get("name", "")
        out.append(text.replace("{livemap}", lt.get("name", "")).replace("{seats}", str(lite.get("seats", 0))).replace("{lite}", lite_name))
        if perk == "livemap" and lt:
            dims = lt.get("dimensions") or ["*"]
            dim_text = (ctx.t("all dimensions incl. mods", "alle Dimensionen inkl. Mods") if "*" in dims else
                        ctx.t("Overworld, Nether, End", "Oberwelt, Nether, End") if len(dims) >= 3 else ctx.t("Overworld, Nether", "Oberwelt, Nether"))
            out.append(ctx.t(f"{lt['maps']} maps, live every {lt['live_seconds']} s, {dim_text}", f"{lt['maps']} Karten, live alle {lt['live_seconds']} s, {dim_text}"))
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
<div class="c"><b>{ctx.t("Give as a gift", "Verschenken")}</b><p>{ctx.t("Simple Mods Pro for a fellow player's Minecraft name, 1, 3 or 12 months, ends by itself.", "Simple Mods Pro für den Minecraft-Namen eines Mitspielers, 1, 3 oder 12 Monate, endet automatisch.")}</p><p><a class="btn small s" href="{e(ctx.url("account", "/pro/gift"))}">{ctx.t("Give", "Verschenken")}</a></p></div>
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
        (ctx.t("Payment", "Zahlung"), payment_row(ctx)),
        (ctx.t("Withdrawal", "Widerruf"), ctx.t(f"14-day right of withdrawal for consumers – ", f"14 Tage Widerrufsrecht für Verbraucher – ") + legal_link(ctx, "withdrawal_url", "withdrawal notice", "Widerrufsbelehrung")),
    ]
    table = "".join(f'<tr><th scope="row">{k}</th><td>{v}</td></tr>' for k, v in rows)
    terms = (ctx.t("I have read and accept the ", "Ich habe die ") + legal_link(ctx, "terms_url", *terms_label(ctx))
             + ctx.t(" and the ", " und die ") + legal_link(ctx, "withdrawal_url", "withdrawal notice", "Widerrufsbelehrung")
             + ctx.t(". I have taken note of the ", " gelesen und akzeptiere sie. Die ") + legal_link(ctx, "privacy_url", "privacy notice", "Datenschutzerklärung")
             + ctx.t(".", " habe ich zur Kenntnis genommen."))
    waiver = waiver_text(ctx.lang)
    chk = lambda name: " checked" if f.get(name) == "1" else ""  # noqa: E731
    inner = f"""<input type="hidden" name="plan" value="{e(plan_key)}"><input type="hidden" name="interval" value="{e(interval)}">
<label class="check"><input type="checkbox" name="terms" value="1" required{chk("terms")}><span>{terms} <b>*</b></span></label>
<label class="check"><input type="checkbox" name="waiver" value="1"{chk("waiver")}><span>{e(waiver)}<br><span class="mut small">{ctx.t(f"Voluntary. Without it the perks start after the withdrawal period ({days} days); the first payment is due anyway.", f"Freiwillig. Ohne dieses Häkchen starten die Vorzüge erst nach Ablauf der Widerrufsfrist ({days} Tage); die erste Zahlung ist trotzdem fällig.")}</span></span></label>
<label>{ctx.t("Coupon code (optional)", "Gutscheincode (optional)")}<input name="coupon" maxlength="40" value="{e(f.get("coupon", ""))}"></label>
<p class="mut small">* {ctx.t("required", "Pflichtfeld")}</p>
<button class="btn order-button">zahlungspflichtig bestellen</button>"""
    body = f"""<h1>{ctx.t("Your order", "Deine Bestellung")}</h1>{alerts(ctx)}
<ol class="steps small"><li>{ctx.t("Choose plan", "Stufe wählen")}</li><li aria-current="step"><b>{ctx.t("Check and order", "Prüfen und bestellen")}</b></li><li>{ctx.t("Pay with " + provider_label(ctx), "Bezahlen mit " + provider_label(ctx))}</li></ol>
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
    body = f"""<h1>{ctx.t("My subscription", "Mein Abo")}</h1>{alerts(ctx)}<section class="c">{inner}</section>{change_block(ctx, view)}{lite_block(ctx)}
<section class="c"><h2>{ctx.t("Cancel", "Kündigen")}</h2><p>{ctx.t("You can cancel any time to the end of the paid period.", "Du kannst jederzeit zum Ende des bezahlten Zeitraums kündigen.")}</p>
<p><a class="btn s" href="{e(ctx.url("account", "/kuendigen"))}">{ctx.t("Cancel contracts here", "Verträge hier kündigen")}</a></p></section>"""
    return page(ctx, ctx.t("My subscription", "Mein Abo"), body)


def change_block(ctx, view: dict) -> str:
    """P070: change the plan at the end of the paid period (no refund, no proration)."""
    sub = view["sub"]
    if not sub or sub["status"] not in ("active", "trialing"):
        return ""
    plans = ctx.cfg.plans
    cur = sub["plan"]
    pending = sub.get("pending_plan")
    opts = "".join(f'<option value="{k}"{" selected" if k == (pending or cur) else ""}>{e(plans["plans"][k]["name"])}</option>' for k in plans["order"])
    note = (ctx.t(f"Change to {plans['plans'][pending]['name']} on {when(ctx, sub['period_end'])}.", f"Wechsel zu {plans['plans'][pending]['name']} am {when(ctx, sub['period_end'])}.")
            if pending and pending in plans["plans"] else "")
    inner = (f'<input type="hidden" name="action" value="change"><label>{ctx.t("New plan from the next period", "Neue Stufe ab dem nächsten Zeitraum")}'
             f'<select name="plan">{opts}</select></label><button class="btn small">{ctx.t("Change at the period end", "Zum Periodenende wechseln")}</button>')
    return (f'<section class="c"><h2>{ctx.t("Change plan", "Stufe wechseln")}</h2><p>{ctx.t("Up- and downgrades take effect at the end of the paid period; there is no refund or credit for the running period.", "Up- und Downgrades gelten ab dem Ende des bezahlten Zeitraums; für den laufenden Zeitraum gibt es keine Rückzahlung oder Gutschrift.")}</p>'
            f'{"<p><b>" + e(note) + "</b></p>" if note else ""}{form(ctx, "/pro/manage", inner, "stack")}</section>')


def gift_order(ctx, plan_key: str, f: dict | None = None) -> str:
    svc = ctx.svc
    f = f or {}
    plans = ctx.cfg.plans
    g = svc.gift_cfg()
    p = plans["plans"][plan_key]
    vat = vat_note(svc.vat(), ctx.lang)
    plan_opts = "".join(f'<option value="{k}"{" selected" if k == plan_key else ""}>{e(plans["plans"][k]["name"])} – {euro(ctx, plans["plans"][k]["price_month"])} {ctx.t("per month", "pro Monat")}</option>' for k in plans["order"])
    month_opts = "".join(f'<option value="{m}"{" selected" if str(m) == f.get("months", "") else ""}>{m} {ctx.t("month(s)", "Monat(e)")}</option>' for m in g["months"])
    n = int(g["names"].get(plan_key, 1))
    names = "".join(f'<label>{ctx.t("Minecraft name", "Minecraft-Name")} {i + 1} <span class="mut small">({ctx.t("optional", "optional")})</span><input name="mc{i}" maxlength="16" pattern="[A-Za-z0-9_]{{3,16}}" value="{e(f.get(f"mc{i}", ""))}"></label>' for i in range(n))
    terms = (ctx.t("I have read and accept the ", "Ich habe die ") + legal_link(ctx, "terms_url", *terms_label(ctx))
             + ctx.t(" and the ", " und die ") + legal_link(ctx, "withdrawal_url", "withdrawal notice", "Widerrufsbelehrung") + ctx.t(".", " gelesen und akzeptiere sie."))
    chk = lambda name: " checked" if f.get(name) == "1" else ""  # noqa: E731
    inner = f"""<label>{ctx.t("Plan", "Stufe")}<select name="plan">{plan_opts}</select></label>
<label>{ctx.t("Duration (one-off payment, ends by itself)", "Dauer (Einmalzahlung, endet automatisch)")}<select name="months">{month_opts}</select></label>
{names}<p class="mut small">{ctx.t(f"The gift is bound to the Minecraft name(s); you can change them every {g['change_days']} days. {n} name(s) for {p['name']}. Leave the name empty to get a gift code instead: the receiver redeems it for their Minecraft name.", f"Das Geschenk gilt für den/die Minecraft-Namen; du kannst sie alle {g['change_days']} Tage ändern. {n} Name(n) bei {p['name']}. Lässt du den Namen leer, bekommst du einen Geschenkcode: der Beschenkte löst ihn für seinen Minecraft-Namen ein.")}</p>
<label>{ctx.t("Coupon code (optional)", "Gutscheincode (optional)")}<input name="coupon" maxlength="40" value="{e(f.get("coupon", ""))}"></label>
<p>{ctx.t("Price:", "Preis:")} {ctx.t("monthly price × months", "Monatspreis × Monate")} <span class="mut small">({e(vat)})</span></p>
<label class="check"><input type="checkbox" name="terms" value="1" required{chk("terms")}><span>{terms} <b>*</b></span></label>
<label class="check"><input type="checkbox" name="waiver" value="1"{chk("waiver")}><span>{e(waiver_text(ctx.lang))}</span></label>
<button class="btn order-button">zahlungspflichtig bestellen</button>"""
    body = f"""<h1>{ctx.t("Give Simple Mods Pro", "Simple Mods Pro verschenken")}</h1>{alerts(ctx)}
<p class="sub">{ctx.t("For a fellow player's Minecraft name – perks of the plan that belong to the name (e.g. future in-game perks), early access; no account needed for the receiver.", "Für den Minecraft-Namen eines Mitspielers – die Vorzüge der Stufe, die am Namen hängen (z. B. künftige Vorzüge im Spiel), Frühzugang; der Beschenkte braucht kein Konto.")}</p>
<section class="c">{form(ctx, "/pro/gift", inner, "stack")}</section>"""
    return page(ctx, ctx.t("Gift", "Geschenk"), body)


def gifts(ctx, items: list[dict]) -> str:
    rows = ""
    for g in items:
        plan = ctx.cfg.plans["plans"].get(g["plan"], {}).get("name", g["plan"])
        n = int(ctx.svc.gift_cfg()["names"].get(g["plan"], 1))
        fields = "".join(f'<input name="mc{i}" maxlength="16" aria-label="Minecraft {i + 1}" value="{e(g["names"][i] if i < len(g["names"]) else "")}">' for i in range(n))
        can = g["state"] == "active" and ctx.svc.now() >= g["change_from"]
        change = form(ctx, "/pro/gifts", f'<input type="hidden" name="gift" value="{g["id"]}">{fields}<button class="btn small s"{"" if can else " disabled"}>{ctx.t("Change name", "Namen ändern")}</button>', "inline") if g["state"] == "active" else ""
        wait = "" if can or g["state"] != "active" else f'<span class="mut small">{ctx.t("changeable from", "änderbar ab")} {e(when(ctx, g["change_from"]))}</span>'
        code = ""
        if g["state"] == "active" and g.get("code") and not g["names"]:
            code = (f'<br>{ctx.t("Gift code (not redeemed yet):", "Geschenkcode (noch nicht eingelöst):")} <code>{e(g["code"])}</code> '
                    f'<span class="mut small">{ctx.t("redeem at", "einlösen unter")} {e(ctx.url("account", "/pro/redeem"))}</span>')
        rows += (f'<li class="c"><b>Simple Mods Pro {e(plan)}</b> · {g["months"]} {ctx.t("month(s)", "Monat(e)")} · {e(", ".join(g["names"]))}{code}'
                 f'<br><span class="mut small">{e(when(ctx, g["starts_at"]))} – {e(when(ctx, g["ends_at"]))} · {e(g["state"])}</span><br>{change} {wait}</li>')
    body = f"""<h1>{ctx.t("My gifts", "Meine Geschenke")}</h1>{alerts(ctx)}
<ul class="plain stack">{rows or f'<li class="c">{ctx.t("No gifts yet.", "Noch keine Geschenke.")}</li>'}</ul>
<p><a class="btn" href="{e(ctx.url("account", "/pro/gift"))}">{ctx.t("Give Simple Mods Pro", "Simple Mods Pro verschenken")}</a></p>"""
    return page(ctx, ctx.t("My gifts", "Meine Geschenke"), body)


def contact(ctx, f: dict | None = None, sent: bool = False) -> str:
    if sent:
        return page(ctx, ctx.t("Message sent", "Nachricht gesendet"), f'<section class="card narrow"><h1>{ctx.t("Thank you", "Danke")}</h1><p>{ctx.t("Your message is on its way. We answer by e-mail.", "Deine Nachricht ist unterwegs. Wir antworten per E-Mail.")}</p></section>')
    f = f or {}
    topics = (("allgemein", ctx.t("General", "Allgemein")), ("datenschutz", ctx.t("Privacy", "Datenschutz")), ("melden", ctx.t("Report content", "Inhalt melden")))
    opts = "".join(f'<option value="{k}"{" selected" if f.get("topic") == k else ""}>{v}</option>' for k, v in topics)
    inner = f"""<input type="hidden" name="t" value="{e(ctx.svc.contact_token())}">
<label>{ctx.t("Name (optional)", "Name (optional)")}<input name="name" maxlength="80" value="{e(f.get("name", ""))}" autocomplete="name"></label>
<label>{ctx.t("Your e-mail address (for the answer)", "Deine E-Mail-Adresse (für die Antwort)")}<input type="email" name="email" required maxlength="254" value="{e(f.get("email", ""))}" autocomplete="email"></label>
<label>{ctx.t("Topic", "Thema")}<select name="topic">{opts}</select></label>
<label>{ctx.t("Message", "Nachricht")}<textarea name="message" required minlength="10" maxlength="5000" rows="7">{e(f.get("message", ""))}</textarea></label>
<div class="hp" aria-hidden="true"><label>Website<input name="website" tabindex="-1" autocomplete="off"></label></div>
<p class="mut small">{ctx.t("We use your data only to answer (privacy notice). No copy is sent to you.", "Wir nutzen deine Angaben nur zur Antwort (Datenschutzerklärung). Eine Kopie an dich wird nicht verschickt.")}</p>
<button class="btn">{ctx.t("Send", "Senden")}</button>"""
    body = f'<section class="card narrow"><h1>{ctx.t("Contact", "Kontakt")}</h1>{alerts(ctx)}{form(ctx, "/kontakt", inner, "stack")}</section>'
    return page(ctx, ctx.t("Contact", "Kontakt"), body)


def lite_block(ctx) -> str:
    """P039: fellow-player seats of the owner, or the seat this account holds."""
    svc, user = ctx.svc, ctx.user
    owner, seat = svc.lite_holder(user)
    if owner is not None:
        name = owner["name"] or ctx.t("a subscriber", "ein Abonnent")
        leave = form(ctx, "/pro/manage", f'<input type="hidden" name="action" value="lite_leave"><button class="btn small s">{ctx.t("Give the seat back", "Platz zurückgeben")}</button>', "inline")
        return (f'<section class="c"><h2>{ctx.t("Fellow-player seat", "Mitspieler-Platz")}</h2><p>{ctx.t("You use a seat of", "Du nutzt einen Platz von")} '
                f'<b>{e(name)}</b> ({ctx.t("since", "seit")} {e(when(ctx, seat["created_at"]))}). {ctx.t("It ends with their subscription.", "Er endet mit dessen Abo.")}</p>{leave}</section>')
    offer = svc.lite_offer(user)
    if offer is None:
        return ""
    kind, count = offer
    lite_name = ctx.cfg.plans["lite"][kind]["name"]
    days = int(ctx.cfg.plans["lite_rules"].get("reassign_lock_days", 30))
    rows = ""
    for s in svc.lite_seats(user["id"]):
        remove = form(ctx, "/pro/manage", f'<input type="hidden" name="action" value="lite_remove"><input type="hidden" name="member" value="{s["member_id"]}">'
                                          f'<button class="btn small s">{ctx.t("Remove", "Entfernen")}</button>', "inline")
        rows += f'<li>{e(s["name"] or s["email"])} <span class="mut small">{e(s["email"])}</span> {remove}</li>'
    free = max(0, count - svc.lite_used(user["id"]))
    label = ctx.t("E-mail of the fellow player's Simple Mods account", "E-Mail des Simple-Mods-Kontos des Mitspielers")
    add = form(ctx, "/pro/manage", f'<input type="hidden" name="action" value="lite_add"><label>{label}<input type="email" name="email" required maxlength="254"></label>'
                                   f'<button class="btn small">{ctx.t("Give seat", "Platz vergeben")}</button>', "stack") if free else ""
    info = ctx.t(f"{count} seat(s) for other accounts; free now: {free}. A freed seat can be given again after {days} days; seats end with your subscription and cannot be passed on.",
                 f"{count} Platz/Plätze für andere Konten; jetzt frei: {free}. Ein freigewordener Platz ist erst nach {days} Tagen wieder vergebbar; Plätze enden mit deinem Abo und sind nicht weitergebbar.")
    return (f'<section class="c"><h2>{ctx.t("Fellow-player seats", "Mitspieler-Plätze")} „{e(lite_name)}“</h2><p>{info}</p>'
            f'<ul class="plain">{rows}</ul>{add}</section>')


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


def pay(ctx, txn: str, success: str, prov) -> str:
    """Paddle checkout page: Paddle.js (cdn.paddle.com) + static/paddle-checkout.js open the overlay for the transaction; the
    values are data attributes (no inline script). client_token is Paddle's public client-side token."""
    token = prov.p.get("client_token") or ""
    data = (f'id="paddle-pay" data-token="{e(token)}" data-env="{e(prov.environment)}" data-txn="{e(txn)}" '
            f'data-locale="{"de" if ctx.lang == "de" else "en"}" data-success="{e(success)}"')
    body = f"""<section class="card narrow" {data}><h1>{ctx.t("Payment with Paddle", "Bezahlen mit Paddle")}</h1>
<p>{ctx.t("The secure Paddle checkout opens over this page. Paddle.com is the seller (merchant of record) of this order and issues the invoice.",
          "Die sichere Bezahlseite von Paddle öffnet sich über dieser Seite. Verkäufer dieser Bestellung ist Paddle.com (Merchant of Record); Paddle stellt die Rechnung aus.")}</p>
<p><button type="button" class="btn" id="paddle-open">{ctx.t("Open the payment again", "Bezahlung erneut öffnen")}</button>
<a class="btn s" href="{e(ctx.url("account", "/pro"))}">{ctx.t("Cancel", "Abbrechen")}</a></p>
<noscript><p class="alert">{ctx.t("The Paddle checkout needs JavaScript.", "Die Paddle-Bezahlseite braucht JavaScript.")}</p></noscript></section>
<script src="https://cdn.paddle.com/paddle/v2/paddle.js" defer></script><script src="/static/paddle-checkout.js" defer></script>"""
    return page(ctx, ctx.t("Payment", "Bezahlung"), body)


def redeem(ctx, f: dict, result: dict | None = None) -> str:
    """Addendum 4: redeem a gift code on a Minecraft name (no account needed)."""
    if result is not None:
        plan = ctx.cfg.plans["plans"].get(result["plan"], {}).get("name", result["plan"])
        body = f"""<section class="card narrow"><h1>{ctx.t("Gift redeemed", "Geschenk eingelöst")}</h1>
<p>{ctx.t("Simple Mods Pro", "Simple Mods Pro")} <b>{e(plan)}</b> {ctx.t("now belongs to", "gilt jetzt für")} <b>{e(", ".join(result["names"]))}</b>
({e(when(ctx, result["starts"]))} – {e(when(ctx, result["ends"]))}).</p></section>"""
        return page(ctx, ctx.t("Gift redeemed", "Geschenk eingelöst"), body)
    inner = f"""<label>{ctx.t("Gift code", "Geschenkcode")}<input name="code" required maxlength="20" pattern="[A-Za-z0-9-]{{18,20}}" placeholder="SMG-XXXX-XXXX-XXXX" value="{e(f.get("code", ""))}" autocomplete="off"></label>
<label>{ctx.t("Your Minecraft name", "Dein Minecraft-Name")}<input name="mc0" required maxlength="16" pattern="[A-Za-z0-9_]{{3,16}}" value="{e(f.get("mc0", ""))}"></label>
<label>{ctx.t("Second Minecraft name (only Ultra, optional)", "Zweiter Minecraft-Name (nur Ultra, optional)")}<input name="mc1" maxlength="16" pattern="[A-Za-z0-9_]{{3,16}}" value="{e(f.get("mc1", ""))}"></label>
<p class="mut small">{ctx.t("A code can be redeemed once. Later name changes: the giver, every 7 days.", "Ein Code lässt sich einmal einlösen. Spätere Namensänderungen: der Schenkende, alle 7 Tage.")}</p>
<button class="btn">{ctx.t("Redeem", "Einlösen")}</button>"""
    body = f'<section class="card narrow"><h1>{ctx.t("Redeem a gift", "Geschenk einlösen")}</h1>{alerts(ctx)}{form(ctx, "/pro/redeem", inner, "stack")}</section>'
    return page(ctx, ctx.t("Redeem a gift", "Geschenk einlösen"), body)
