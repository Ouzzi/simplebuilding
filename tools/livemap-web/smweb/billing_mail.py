"""Texts of Simple Mods Pro (N108): price lines with the VAT note, the note under Stripe's pay button, and the mails
(contract confirmation section 312f BGB, invoice, payment failed, cancellation receipt section 312k BGB, end, yearly notice).
Plain text, German and English; addresses of legal pages come from the server config (``legal``)."""
from __future__ import annotations

import time


def money(cents: int, lang: str) -> str:
    cents = int(cents or 0)
    if lang == "de":
        return f"{cents // 100},{cents % 100:02d} €"
    return f"€{cents // 100}.{cents % 100:02d}"


def day(ts, lang: str) -> str:
    if not ts:
        return "–"
    return time.strftime("%d.%m.%Y" if lang == "de" else "%Y-%m-%d", time.gmtime(ts))


def stamp(ts, lang: str) -> str:
    return time.strftime("%d.%m.%Y, %H:%M:%S UTC" if lang == "de" else "%Y-%m-%d %H:%M:%S UTC", time.gmtime(ts))


def vat_note(vat: dict, lang: str) -> str:
    """Price note next to every price (PAngV): small business (section 19 UStG) or VAT included; with Stripe Tax the rate
    depends on the buyer's country and is shown in the checkout."""
    if vat.get("mode") == "small_business":
        return ("Gemäß § 19 UStG wird keine Umsatzsteuer berechnet." if lang == "de"
                else "No VAT is charged (small business, § 19 UStG).")
    if vat.get("stripe_tax"):
        return ("Endpreis inkl. gesetzlicher USt.; der Satz richtet sich nach deinem Land und wird im Bezahlschritt ausgewiesen." if lang == "de"
                else "Final price incl. VAT; the rate depends on your country and is shown in the payment step.")
    rate = vat.get("rate_percent", 19)
    return f"inkl. {rate} % USt." if lang == "de" else f"incl. {rate}% VAT"


WAIVER = {
    "de": "Ich verlange ausdrücklich, dass mit der Ausführung vor Ablauf der Widerrufsfrist begonnen wird. Mir ist bekannt, dass ich bei "
          "vollständiger Vertragserfüllung mein Widerrufsrecht verliere, dass es mit Beginn der Bereitstellung digitaler Inhalte "
          "(Frühzugang-Downloads) erlischt und dass ich bei einem Widerruf nach Beginn einen angemessenen Betrag für die bis dahin "
          "erbrachte Leistung zahle.",
    "en": "I expressly ask that the service starts before the withdrawal period ends. I know that I lose my right of withdrawal when the "
          "contract is completely fulfilled, that it expires when the provision of digital content (early-access downloads) starts, "
          "and that on withdrawal after the start I pay a fair amount for what was provided.",
}


def waiver_text(lang: str) -> str:
    """The exact consent text shown in the order form; the same text is stored with the order as proof."""
    return WAIVER["de" if lang == "de" else "en"]


def submit_note(lang: str) -> str:
    return ("Mit „Abonnieren“ zahlst du die oben bestätigte Bestellung (zahlungspflichtig bestellt auf simplemods.de)."
            if lang == "de" else "“Subscribe” pays the order you placed on simplemods.de with “zahlungspflichtig bestellen” (order with obligation to pay).")


def interval_text(interval: str, lang: str) -> str:
    if interval == "year":
        return ("jährlich im Voraus (12 Monate), danach monatlich zum Monatspreis, jederzeit zum Ende des bezahlten Monats kündbar"
                if lang == "de" else "yearly in advance (12 months), then monthly at the monthly price, cancellable to the end of every paid month")
    return ("monatlich, jederzeit zum Ende des bezahlten Monats kündbar" if lang == "de"
            else "monthly, cancellable any time to the end of the paid month")


def legal_links(svc, lang: str) -> str:
    legal = svc.cfg["legal"]
    acc = svc.cfg.url("account", "/pro")
    rows = [("terms_url", "AGB", "Terms"), ("withdrawal_url", "Widerrufsbelehrung", "Withdrawal notice"),
            ("withdrawal_form_url", "Muster-Widerrufsformular", "Model withdrawal form"), ("privacy_url", "Datenschutz", "Privacy")]
    return "\n".join(f"- {de if lang == 'de' else en}: {legal.get(k) or acc}" for k, de, en in rows)


def render(kind: str, lang: str, svc, **p) -> tuple[str, str]:
    de = lang == "de"
    addr = svc.contact_address("allgemein")
    sign = ("\n\nSimple Mods" + ((f"\nKontakt: {addr}" if de else f"\nContact: {addr}") if addr else "")
            + ("\nVerträge hier kündigen: " if de else "\nCancel contracts here: ") + svc.cfg.url("account", "/kuendigen"))
    plans = svc.cfg.plans["plans"]
    name = lambda k: (plans.get(k) or {}).get("name", k)  # noqa: E731
    vat = vat_note(svc.vat(), lang)
    if kind == "order":
        waiver = (("Du hast ausdrücklich verlangt, dass wir vor Ablauf der Widerrufsfrist beginnen, und bestätigt, dass dein Widerrufsrecht "
                   "damit nach § 356 Abs. 4/5 BGB erlischt bzw. du bei Widerruf einen angemessenen Betrag zahlst. Die Vorzüge gelten ab sofort.")
                  if p["waiver"] else
                  (f"Du hast keinen vorzeitigen Beginn verlangt: Die Vorzüge werden nach Ablauf der Widerrufsfrist "
                   f"({int(svc.bcfg('withdrawal_days', 14))} Tage) freigeschaltet.")) if de else \
                 (("You expressly asked us to start before the withdrawal period ends and confirmed that your right of withdrawal expires "
                   "(§ 356 (4)/(5) BGB) or that you pay a fair amount on withdrawal. The perks apply right away.")
                  if p["waiver"] else
                  (f"You did not ask for an early start: the perks are unlocked after the withdrawal period "
                   f"({int(svc.bcfg('withdrawal_days', 14))} days)."))
        if de:
            return (f"Bestellbestätigung Simple Mods Pro {name(p['plan'])}",
                    f"Hallo,\n\ndanke für deine Bestellung. Vertragsdaten:\n\n- Abo: Simple Mods Pro {name(p['plan'])}\n"
                    f"- Laufzeit: {interval_text(p['interval'], lang)}\n- Preis: {money(p['cents'], lang)} pro "
                    f"{'Jahr' if p['interval'] == 'year' else 'Monat'} ({vat})\n- Bestellnummer: {p['order_id']}\n"
                    f"- Vertragsschluss: {stamp(p['at'], lang)}\n\n{waiver}\n\nVertragstext und Belehrungen:\n{legal_links(svc, lang)}\n\n"
                    f"Abo verwalten (Zahlungsart, Rechnungen, Stufe): {svc.cfg.url('account', '/pro/manage')}{sign}")
        return (f"Order confirmation Simple Mods Pro {name(p['plan'])}",
                f"Hello,\n\nthank you for your order. Contract data:\n\n- Subscription: Simple Mods Pro {name(p['plan'])}\n"
                f"- Term: {interval_text(p['interval'], lang)}\n- Price: {money(p['cents'], lang)} per {'year' if p['interval'] == 'year' else 'month'} ({vat})\n"
                f"- Order number: {p['order_id']}\n- Contract made: {stamp(p['at'], lang)}\n\n{waiver}\n\nContract text and notices:\n"
                f"{legal_links(svc, lang)}\n\nManage the subscription: {svc.cfg.url('account', '/pro/manage')}{sign}")
    if kind == "gift":
        if de:
            return (f"Bestellbestätigung Geschenk-Abo Simple Mods Pro {name(p['plan'])}",
                    f"Hallo,\n\ndanke für dein Geschenk. Vertragsdaten:\n\n- Geschenk: Simple Mods Pro {name(p['plan'])} für {p['months']} "
                    f"{'Monat' if p['months'] == 1 else 'Monate'} (Einmalzahlung, endet automatisch)\n- Minecraft-Name(n): {p['names']}\n"
                    f"- Preis: {money(p['cents'], lang)} ({vat})\n- Gültig: {day(p['starts'], lang)} bis {day(p['ends'], lang)}\n"
                    f"- Bestellnummer: {p['order_id']}\n- Vertragsschluss: {stamp(p['at'], lang)}\n\n"
                    f"Den Namen kannst du alle {int(svc.gift_cfg()['change_days'])} Tage ändern: {svc.cfg.url('account', '/pro/gifts')}\n\n"
                    f"{'Du hast den sofortigen Beginn verlangt; dein Widerrufsrecht erlischt nach § 356 Abs. 4/5 BGB.' if p['waiver'] else 'Ohne vorzeitigen Beginn startet das Geschenk nach Ablauf der Widerrufsfrist.'}\n\n"
                    f"Vertragstext und Belehrungen:\n{legal_links(svc, lang)}{sign}")
        return (f"Order confirmation gift Simple Mods Pro {name(p['plan'])}",
                f"Hello,\n\nthank you for your gift. Contract data:\n\n- Gift: Simple Mods Pro {name(p['plan'])} for {p['months']} month(s) "
                f"(one-off payment, ends by itself)\n- Minecraft name(s): {p['names']}\n- Price: {money(p['cents'], lang)} ({vat})\n"
                f"- Valid: {day(p['starts'], lang)} to {day(p['ends'], lang)}\n- Order number: {p['order_id']}\n- Contract made: {stamp(p['at'], lang)}\n\n"
                f"Change the name every {int(svc.gift_cfg()['change_days'])} days: {svc.cfg.url('account', '/pro/gifts')}\n\n"
                f"Contract text and notices:\n{legal_links(svc, lang)}{sign}")
    if kind == "invoice":
        links = "\n".join(x for x in (p.get("link"), p.get("pdf")) if x) or svc.cfg.url("account", "/pro/manage")
        if de:
            return (f"Rechnung {p['number']} – Simple Mods Pro",
                    f"Hallo,\n\ndeine Zahlung über {money(p['cents'], lang)} ist eingegangen. Rechnung {p['number']}:\n\n{links}\n\n"
                    f"Alle Rechnungen findest du unter {svc.cfg.url('account', '/pro/manage')}.{sign}")
        return (f"Invoice {p['number']} – Simple Mods Pro",
                f"Hello,\n\nyour payment of {money(p['cents'], lang)} arrived. Invoice {p['number']}:\n\n{links}\n\n"
                f"All invoices: {svc.cfg.url('account', '/pro/manage')}.{sign}")
    if kind == "payment_failed":
        if de:
            return ("Zahlung fehlgeschlagen – Simple Mods Pro",
                    f"Hallo,\n\ndie Zahlung über {money(p['cents'], lang)} hat nicht geklappt. Bitte aktualisiere deine Zahlungsart:\n\n{p['link']}\n\n"
                    f"Deine Vorzüge bleiben noch {p['grace']} Tage nach Ende des bezahlten Zeitraums aktiv; danach wechselt das Konto auf Free.{sign}")
        return ("Payment failed – Simple Mods Pro",
                f"Hello,\n\nthe payment of {money(p['cents'], lang)} did not work. Please update your payment method:\n\n{p['link']}\n\n"
                f"Your perks stay active for {p['grace']} days after the paid period; then the account changes to Free.{sign}")
    if kind == "cancel":
        kind_text = ({"ordinary": "ordentliche Kündigung zum nächstmöglichen Zeitpunkt", "extraordinary": "außerordentliche Kündigung"} if de
                     else {"ordinary": "ordinary cancellation at the earliest possible date", "extraordinary": "extraordinary cancellation"})[p["how"]]
        eff = day(p["effective"], lang) if p.get("effective") else ("wird geprüft und dir mitgeteilt" if de else "is checked and sent to you")
        if de:
            return ("Eingangsbestätigung deiner Kündigung – Simple Mods",
                    f"Hallo,\n\nwir haben deine Kündigung erhalten.\n\n- Eingang: {stamp(p['at'], lang)}\n- Art: {kind_text}\n"
                    f"- Vertrag: Simple Mods Pro\n- Vertragsende: {eff}\n\nBis zum Vertragsende bleiben deine Vorzüge aktiv. "
                    f"Warst du das nicht? Antworte auf diese Mail oder nimm die Kündigung im Kundenportal zurück: "
                    f"{svc.cfg.url('account', '/pro/manage')}{sign}")
        return ("Your cancellation was received – Simple Mods",
                f"Hello,\n\nwe received your cancellation.\n\n- Received: {stamp(p['at'], lang)}\n- Kind: {kind_text}\n"
                f"- Contract: Simple Mods Pro\n- Contract ends: {eff}\n\nYour perks stay active until then. Not you? Reply to this mail "
                f"or undo it in the customer portal: {svc.cfg.url('account', '/pro/manage')}{sign}")
    if kind == "ended":
        if de:
            return ("Dein Simple Mods Pro ist beendet",
                    f"Hallo,\n\ndein Abo Simple Mods Pro {name(p['plan'])} ist beendet; dein Konto nutzt jetzt die Stufe Free. "
                    f"Überzählige Karten liegen 30 Tage im Puffer. Wieder abonnieren: {svc.cfg.url('account', '/pro')}{sign}")
        return ("Your Simple Mods Pro has ended",
                f"Hello,\n\nyour subscription Simple Mods Pro {name(p['plan'])} has ended; your account uses the Free tier now. "
                f"Extra maps wait 30 days in the archive. Subscribe again: {svc.cfg.url('account', '/pro')}{sign}")
    if kind == "year_notice":
        month = money((plans.get(p["plan"]) or {}).get("price_month", 0), lang)
        if de:
            return ("Dein Jahresabo geht bald in die monatliche Laufzeit über",
                    f"Hallo,\n\ndie bezahlten 12 Monate deines Simple Mods Pro {name(p['plan'])} enden am {day(p['end'], lang)}. "
                    f"Danach läuft das Abo monatlich zum Monatspreis von {month} ({vat}) weiter und ist jederzeit zum Ende des bezahlten "
                    f"Monats kündbar. Kündigen: {svc.cfg.url('account', '/kuendigen')}{sign}")
        return ("Your yearly plan changes to monthly soon",
                f"Hello,\n\nthe 12 paid months of your Simple Mods Pro {name(p['plan'])} end on {day(p['end'], lang)}. Then it runs on "
                f"monthly at {month} ({vat}) and can be cancelled any time to the end of the paid month. Cancel: "
                f"{svc.cfg.url('account', '/kuendigen')}{sign}")
    raise ValueError(kind)
