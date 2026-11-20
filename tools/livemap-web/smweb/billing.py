"""Simple Mods Pro (N108): one account subscription (Basic/Max/Ultra) that unlocks a Simple Livemap tier and further perks
(early access first). Orders with the consents, entitlements table (account -> plan -> perks), downgrade/cancel/expiry, gifts,
fellow-player seats, mails. Plans: docs/ai/PLAN-B-N108-STRIPE-2026-11-19.md, docs/ai/PLAN-B-N108-PADDLE-2026-11-19.md.

Payment provider (addendum 3/4): this module talks only to the narrow interface in ``smweb/payments`` and handles the unified
events (``payments.base``); ``billing.provider`` picks the provider for new orders (default ``paddle`` = merchant of record,
``stripe`` = second connection). Every provider has its own webhook route (``/api/<provider>/webhook``) checked with its own
secret, so subscriptions of a former provider run out cleanly after a switch.

Off while ``billing.json`` has no ``enabled: true`` + the provider's keys: every paying path answers "coming soon". The HTTP
calls go through ``self.paddle_api`` / ``self.stripe_api`` (tests and the local harness put fakes there; no real call happens
without a key in the server's billing file).

Order flow (AGB/Nutzungsbedingungen): /pro -> /pro/order (summary, terms box required, early-start box voluntary, button
"zahlungspflichtig bestellen") -> provider checkout (payment data) -> webhooks. Without the early-start consent (section 356
Abs. 4/5 BGB) the entitlement starts only after the withdrawal period (``withdrawal_days``).

Yearly plan (owner 12.11.2026, AGB 11.3): 12 months paid in advance, then MONTHLY at the monthly price, cancellable to the end of
every paid month (Stripe: subscription schedule; Paddle: item switched to the month price before the year ends, do_not_bill).
"""
from __future__ import annotations

import json
import re
import secrets
import time

from . import billing_mail
from . import payments
from . import security as sec
from .config import LIVEMAP_ALIAS, PLAN_ALIAS
from .errors import Problem
from .payments import base as ev_kinds
from .payments.stripe import API, TOLERANCE, StripeError, StripeHTTP, flatten, sign, verify_signature  # noqa: F401 (old import path)

ENTITLED = ("active", "trialing")
GRACE = ("past_due",)
RENEWAL_BUFFER = 2 * 86400
MC_NAME_RE = re.compile(r"^[A-Za-z0-9_]{3,16}$")
COUPON_RE = re.compile(r"^[A-Za-z0-9_-]{2,40}$")
GIFT_CODE_RE = re.compile(r"^SMG(-[A-Z2-9]{4}){3}$")
GIFT_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"


def gift_code() -> str:
    return "SMG-" + "-".join("".join(secrets.choice(GIFT_ALPHABET) for _ in range(4)) for _ in range(3))


class BillingMixin:
    stripe_api = None   # set by tests/harness (fake); else made from the billing file on first use
    paddle_api = None

    # ---------------------------------------------------------------- configuration
    def provider_name(self) -> str:
        return str(self.cfg.billing.get("provider") or payments.DEFAULT)

    def provider(self, name: str | None = None):
        """The provider for new orders (``billing.provider``) or a named one (webhook routes)."""
        name = name or self.provider_name()
        cache = self.__dict__.setdefault("_providers", {})
        if name not in cache:
            cache[name] = payments.make(self, name)
        return cache[name]

    def billing_on(self) -> bool:
        try:
            return self.provider().configured()
        except ValueError:
            return False

    def bcfg(self, key: str, default=None):
        return self.cfg.billing.get(key, default)

    def plan(self, name: str) -> dict | None:
        return self.cfg.plans["plans"].get(PLAN_ALIAS.get(name, name))

    def price_id(self, plan: str, interval: str) -> str | None:
        return self.provider().price_id(plan, interval)

    def plan_for_price(self, price: str | None) -> tuple[str | None, str | None]:
        return self.provider().plan_for_price(price)

    def plan_for_livemap_tier(self, tier: str) -> str | None:
        return next((k for k, p in self.cfg.plans["plans"].items() if p.get("livemap_tier") == tier), None)

    def vat(self) -> dict:
        """Price note settings. With Paddle (merchant of record) Paddle is the seller and charges VAT: prices are final prices
        incl. VAT whatever the operator's own VAT status is (mode "merchant_of_record")."""
        v = {"mode": "small_business", "rate_percent": 19, "stripe_tax": False}
        v.update(self.bcfg("vat") or {})
        if self.provider_name() == "paddle":
            v["mode"] = "merchant_of_record"
            v["stripe_tax"] = False
        return v

    def contact_address(self, kind: str = "allgemein") -> str:
        """Public contact address from the server config: the operator's (default) or a forwarding address (mode "addresses")."""
        c = self.cfg.get("contact") or {}
        if c.get("mode") == "addresses" and (c.get("addresses") or {}).get(kind):
            return c["addresses"][kind]
        return c.get("operator_email") or ""

    def plan_flags(self, user) -> dict:
        """P050/P053/P079 flags of the account's plan in effect (plans.json "flags"; without plan "free_flags")."""
        base = dict(self.cfg.plans.get("free_flags") or {})
        ent = self.entitlement(user) if user is not None else None
        p = self.plan(ent["plan"]) if ent else None
        if p is None and ent:     # lite seat: flags of its lite definition
            p = self.cfg.plans.get("lite", {}).get(ent["plan"])
        base.update((p or {}).get("flags") or {})
        return base

    def mail_on(self, kind: str) -> bool:
        mails = self.bcfg("mails") or {}
        if kind == "invoice" and kind not in mails and self.provider_name() == "paddle":
            return False      # Paddle (merchant of record) sends its own invoices/receipts
        return bool(mails.get(kind, True))

    # ---------------------------------------------------------------- order (checkout)
    def active_subscription(self, uid: int):
        return self.db.one("SELECT * FROM subscriptions WHERE user_id = ? AND status IN ('active', 'trialing', 'past_due', 'incomplete') "
                           "ORDER BY updated_at DESC LIMIT 1", uid)

    def start_order(self, user, plan: str, interval: str, terms: bool, waiver: bool, lang: str = "de", coupon: str = "") -> str:
        """Records the order with its consents and returns the provider's checkout URL (subscription)."""
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        prov = self.provider()
        plan = PLAN_ALIAS.get(plan, plan)
        p = self.plan(plan)
        price = prov.price_id(plan, interval)
        if p is None or interval not in ("month", "year") or not price:
            raise Problem(400, "bad_request")
        if not user["verified_at"]:
            raise Problem(403, "verify_first")
        if not terms:
            raise Problem(400, "terms_needed")
        if self.active_subscription(user["id"]):
            raise Problem(409, "sub_exists")
        if not self.limiter.allow(f"order:{user['id']}", 10, 3600):
            raise Problem(429, "too_many")
        now = self.now()
        cents = p["price_month"] if interval == "month" else p["price_year"]
        discount = self.coupon_discount(coupon)      # unknown code: error before anything is stored
        with self.db.tx() as c:
            cur = c.execute("INSERT INTO orders (user_id, plan, interval, price_cents, terms_version, terms_at, waiver, waiver_text, created_at, coupon) "
                            "VALUES (?,?,?,?,?,?,?,?,?,?)", (user["id"], plan, interval, cents, self.bcfg("terms_version", "draft"), now,
                                                             1 if waiver else 0, billing_mail.waiver_text(lang) if waiver else "", now,
                                                             (coupon or "").strip()[:40] or None))
            oid = cur.lastrowid
            self.audit(c, user["id"], "order", f"order {oid} {plan}/{interval} waiver={1 if waiver else 0}")
        ref, url = prov.checkout_url(oid, user, plan, interval, waiver, lang, discount)
        with self.db.tx() as c:
            c.execute("UPDATE orders SET session_id = ? WHERE id = ?", (ref, oid))
        if not url:
            raise Problem(502, "payment_error")
        return url

    def portal(self, user) -> str:
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        return self.provider().portal_url(user)

    # ---------------------------------------------------------------- webhook
    def webhook(self, payload: bytes, header=None, provider: str = "stripe") -> dict:
        """Verified events only, each event id once (providers retry; duplicates answer 200 without effect). ``header`` may be
        the signature header value (old call style, Stripe) or the dict of request headers (lower-case names)."""
        headers = header if isinstance(header, dict) else {"stripe-signature": header}
        try:
            prov = self.provider(provider)
        except ValueError:
            raise Problem(404, "not_found")
        if not prov.configured():
            raise Problem(503, "coming_soon")
        eid, kind, created, events = prov.parse_webhook(payload, headers)
        with self.db.tx() as c:
            if c.execute("SELECT 1 FROM stripe_events WHERE id = ?", (eid,)).fetchone():
                return {"received": True, "duplicate": True}
            c.execute("INSERT INTO stripe_events (id, type, created, received_at) VALUES (?,?,?,?)", (eid[:100], kind[:80], created, self.now()))
        try:
            for ev in events:
                self.handle_event(prov, ev)
        except Exception:
            with self.db.tx() as c:   # not handled: forget the id so the provider's retry runs it again
                c.execute("DELETE FROM stripe_events WHERE id = ?", (eid,))
            raise
        return {"received": True}

    def handle_event(self, prov, ev):
        k = ev.kind
        if k == ev_kinds.CHECKOUT_COMPLETED:
            self.on_checkout_completed(prov, ev)
        elif k == ev_kinds.GIFT_PAID:
            user = self.user_for(ev)
            order = self.db.one("SELECT * FROM orders WHERE id = ? AND kind = 'gift'", ev.order_id) if ev.order_id is not None else None
            if user and order and order["user_id"] == user["id"]:
                self.on_gift_paid(ev, order, user)
        elif k in (ev_kinds.SUB_STARTED, ev_kinds.SUB_CHANGED, ev_kinds.SUB_ENDED):
            self.on_subscription(ev)
        elif k == ev_kinds.PAYMENT_SUCCEEDED:
            self.on_invoice(ev, paid=True)
        elif k == ev_kinds.PAYMENT_FAILED:
            self.on_invoice(ev, paid=False)
        elif k == ev_kinds.REFUND:
            self.on_refund(ev)

    def user_for(self, ev):
        uid = ev.user_hint
        if isinstance(uid, str) and uid.isdigit():
            u = self.user(int(uid))
            if u and (not u["billing_customer"] or not ev.customer or u["billing_customer"] == ev.customer):
                return u
        if ev.customer:
            return self.db.one("SELECT * FROM users WHERE billing_customer = ?", ev.customer)
        return None

    def on_checkout_completed(self, prov, ev):
        user = self.user_for(ev)
        if user is None:
            return
        order = self.db.one("SELECT * FROM orders WHERE id = ? AND user_id = ?", ev.order_id, user["id"]) if ev.order_id is not None else None
        sub_id = ev.sub_id
        if order and sub_id and order["interval"] == "year":
            self.year_schedule(prov, sub_id, PLAN_ALIAS.get(order["plan"], order["plan"]))   # first: when it fails, nothing is marked done and the retry repeats all
        with self.db.tx() as c:
            if ev.customer and not user["billing_customer"]:
                c.execute("UPDATE users SET billing_customer = ? WHERE id = ?", (ev.customer, user["id"]))
            if order and not order["completed_at"]:
                c.execute("UPDATE orders SET completed_at = ?, subscription_id = ?, session_id = COALESCE(session_id, ?) WHERE id = ?",
                          (self.now(), sub_id, ev.ref, order["id"]))
            if sub_id:
                c.execute("UPDATE subscriptions SET order_id = COALESCE(order_id, ?), user_id = COALESCE(user_id, ?) WHERE id = ?",
                          (order["id"] if order else None, user["id"], sub_id))
            self.audit(c, user["id"], "checkout", f"order {order['id'] if order else '-'} sub {sub_id or '-'}")
        if order and not order["completed_at"] and self.mail_on("order"):
            self.billing_mail(user, "order", plan=order["plan"], interval=order["interval"], cents=order["price_cents"],
                              waiver=bool(order["waiver"]), at=self.now(), order_id=order["id"])
        self.recompute(user["id"])

    def year_schedule(self, prov, sub_id: str, plan: str):
        """Yearly in advance, then monthly (AGB 11.3): the provider sets it up (Stripe schedule) or it is noted for later (Paddle)."""
        row = self.db.one("SELECT schedule_id, pending_interval FROM subscriptions WHERE id = ?", sub_id)
        if row and (row["schedule_id"] or row["pending_interval"]):
            return row["schedule_id"]
        out = prov.year_then_month(sub_id, plan)
        cols = out if isinstance(out, dict) else {"schedule_id": out}
        sched, pend = cols.get("schedule_id"), cols.get("pending_interval")
        with self.db.tx() as c:
            c.execute("UPDATE subscriptions SET schedule_id = COALESCE(?, schedule_id), pending_interval = COALESCE(?, pending_interval) WHERE id = ?",
                      (sched, pend, sub_id))
            if not row:   # the subscription event may come later; keep the note in a stub row
                c.execute("INSERT OR IGNORE INTO subscriptions (id, customer, plan, interval, status, schedule_id, pending_interval, updated_at) "
                          "VALUES (?, '', ?, 'year', 'incomplete', ?, ?, ?)", (sub_id, plan, sched, pend, self.now()))
        return sched

    def on_subscription(self, ev):
        sid = ev.sub_id
        if not isinstance(sid, str) or not sid:
            return
        user = self.user_for(ev)
        plan = ev.plan or (PLAN_ALIAS.get(ev.meta_plan, ev.meta_plan) if self.plan(ev.meta_plan or "") else None)
        old_tier = LIVEMAP_ALIAS.get(ev.meta_tier or "", ev.meta_tier or "")
        if plan is None and self.cfg.tiers["tiers"].get(old_tier):   # subscriptions from before N108 (livemap tier)
            plan = self.plan_for_livemap_tier(old_tier)
        interval = ev.interval or "month"
        status = ev.status or ""
        old = self.db.one("SELECT * FROM subscriptions WHERE id = ?", sid)
        if old and old["event_at"] > ev.created:
            return   # older than what we know (providers do not guarantee order)
        if plan is None:
            plan = PLAN_ALIAS.get(old["plan"], old["plan"]) if old else ""
        # P070 / yearly-then-monthly: a change sent to the provider before the period end shows up in the same period -> the paid
        # plan/interval stay until the period really ends (Paddle sets the item early; Stripe changes only with the new period).
        same_period = bool(old and old["period_end"] and status in ENTITLED and (ev.period_end or 0) <= old["period_end"])
        hold_plan = same_period and old["pending_plan"] and plan == PLAN_ALIAS.get(old["pending_plan"], old["pending_plan"]) and plan != old["plan"]
        hold_iv = same_period and old["pending_interval"] and interval == old["pending_interval"] and interval != old["interval"]
        if hold_plan:
            plan = old["plan"]
        if hold_iv:
            interval = old["interval"]
        with self.db.tx() as c:
            c.execute("""INSERT INTO subscriptions (id, user_id, customer, plan, interval, status, period_end, cancel_at_period_end, order_id, event_at, updated_at)
                         VALUES (?,?,?,?,?,?,?,?,?,?,?)
                         ON CONFLICT(id) DO UPDATE SET user_id = COALESCE(excluded.user_id, user_id), customer = excluded.customer,
                           plan = excluded.plan, interval = excluded.interval, status = excluded.status, period_end = excluded.period_end,
                           cancel_at_period_end = excluded.cancel_at_period_end, order_id = COALESCE(order_id, excluded.order_id),
                           event_at = excluded.event_at, updated_at = excluded.updated_at""",
                      (sid, user["id"] if user else None, ev.customer or "", plan, interval, status,
                       ev.period_end if isinstance(ev.period_end, int) else None, 1 if ev.cancel_at_period_end else 0,
                       ev.order_id, ev.created, self.now()))
            if user and ev.customer and not user["billing_customer"]:
                c.execute("UPDATE users SET billing_customer = ? WHERE id = ?", (ev.customer, user["id"]))
            if not hold_plan:
                c.execute("UPDATE subscriptions SET pending_plan = NULL WHERE id = ? AND (pending_plan = ? OR ? = 'canceled')", (sid, plan, status))
            if not hold_iv:
                c.execute("UPDATE subscriptions SET pending_interval = NULL WHERE id = ? AND (pending_interval = ? OR ? = 'canceled')", (sid, interval, status))
            c.execute("UPDATE subscriptions SET pending_applied = 0 WHERE id = ? AND pending_plan IS NULL AND pending_interval IS NULL", (sid,))
        if user:
            self.recompute(user["id"])

    def on_invoice(self, ev, paid: bool):
        user = self.user_for(ev)
        if user is None:
            return
        inv = str(ev.ref or "")
        if paid:
            sub = ev.sub_id
            end = ev.period_end or 0
            if sub and end:
                with self.db.tx() as c:
                    c.execute("UPDATE subscriptions SET period_end = MAX(COALESCE(period_end, 0), ?), updated_at = ? WHERE id = ?", (end, self.now(), sub))
            if self.mail_on("invoice") and self.notice_once(user["id"], "invoice", inv):
                self.billing_mail(user, "invoice", cents=ev.amount or 0, number=ev.number or inv, link=ev.link or "", pdf=ev.pdf or "")
            self.recompute(user["id"])
        elif self.mail_on("payment_failed") and self.notice_once(user["id"], "failed", f"{inv}:{ev.attempt or 0}"):
            self.billing_mail(user, "payment_failed", cents=ev.amount or 0, grace=int(self.bcfg("grace_days", 7)),
                              link=self.cfg.url("account", "/pro/manage"))

    def on_refund(self, ev):
        """Refund or chargeback (approved). A refunded gift ends at once; for subscriptions the provider's subscription events
        carry the state - here only the record."""
        txn = (ev.extra or {}).get("transaction")
        order = self.db.one("SELECT * FROM orders WHERE session_id = ?", txn) if txn else None
        uid = order["user_id"] if order else None
        if uid is None and ev.customer:
            uid = self.db.value("SELECT id FROM users WHERE billing_customer = ?", ev.customer)
        if uid is not None and not self.notice_once(uid, "refund", str(ev.ref or txn or "")):
            return
        now = self.now()
        with self.db.tx() as c:
            if order is not None and order["kind"] == "gift":
                c.execute("UPDATE gifts SET state = 'refunded', ends_at = MIN(COALESCE(ends_at, ?), ?) WHERE order_id = ? AND state IN ('pending', 'active')",
                          (now, now, order["id"]))
            self.audit(c, uid, "refund", f"{ev.action or 'refund'} {ev.ref or '-'} order {order['id'] if order else '-'} sub {ev.sub_id or '-'}")
        if uid is not None:
            self.recompute(uid)

    def notice_once(self, uid: int, kind: str, ref: str) -> bool:
        with self.db.tx() as c:
            if c.execute("SELECT 1 FROM notices WHERE user_id = ? AND kind = ? AND ref = ?", (uid, kind, ref)).fetchone():
                return False
            c.execute("INSERT INTO notices (user_id, kind, ref, at) VALUES (?,?,?,?)", (uid, kind, ref, self.now()))
        return True

    def billing_mail(self, user, kind: str, **params):
        subject, text = billing_mail.render(kind, user["lang"] or "de", self, **params)
        self.mailer.send(user["email"], subject, text)

    # ---------------------------------------------------------------- entitlements
    def sub_until(self, sub) -> int | None:
        """Until when a subscription row entitles (None = not at all)."""
        if sub["status"] in ENTITLED:   # + 2 days: the renewal events arrive around the period end
            return (sub["period_end"] or self.now()) + RENEWAL_BUFFER
        if sub["status"] in GRACE:
            return (sub["period_end"] or self.now()) + int(self.bcfg("grace_days", 7)) * 86400
        return None

    def recompute(self, uid: int):
        """Entitlement of one account from its subscriptions; sets the livemap tier (downgrade rules of maps.set_tier apply).
        A tier set by the developer (no entitlement row from Stripe) stays as long as no Stripe subscription entitles."""
        user = self.user(uid)
        if user is None:
            return
        now = self.now()
        rank = {k: i for i, k in enumerate(self.cfg.plans["order"])}
        best = None
        for sub in self.db.all("SELECT * FROM subscriptions WHERE user_id = ?", uid):
            until = self.sub_until(sub)
            if until is None or until < now or not self.plan(sub["plan"]):
                continue
            if best is None or rank.get(sub["plan"], -1) > rank.get(best[0]["plan"], -1):
                best = (sub, until)
        old = self.db.one("SELECT * FROM entitlements WHERE user_id = ?", uid)
        if best is None:
            if self.lite_entitle(user, old):
                return
            if old and old["source"] in ("stripe", "lite"):
                with self.db.tx() as c:
                    c.execute("DELETE FROM entitlements WHERE user_id = ?", (uid,))
                    self.audit(c, uid, "entitlement", f"end {old['plan']}")
                if LIVEMAP_ALIAS.get(user["tier"], user["tier"]) != "free":
                    self.set_tier(user, "free")
                if old["source"] == "stripe" and self.mail_on("ended") and self.notice_once(uid, "ended", f"{old['subscription_id']}"):
                    self.billing_mail(user, "ended", plan=old["plan"])
            self.lite_sync(uid)
            return
        sub, until = best
        p = self.plan(sub["plan"])
        order = self.db.one("SELECT * FROM orders WHERE id = ?", sub["order_id"]) if sub["order_id"] else None
        if order is None:
            order = self.db.one("SELECT * FROM orders WHERE subscription_id = ?", sub["id"])
        if order is not None and not order["waiver"]:
            starts = order["created_at"] + int(self.bcfg("withdrawal_days", 14)) * 86400
        elif old and old["subscription_id"] == sub["id"]:
            starts = old["starts_at"]
        else:
            starts = order["created_at"] if order is not None else now
        with self.db.tx() as c:
            c.execute("""INSERT INTO entitlements (user_id, plan, perks, livemap_tier, source, subscription_id, starts_at, valid_until, updated_at)
                         VALUES (?,?,?,?, 'stripe', ?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET plan = excluded.plan, perks = excluded.perks,
                         livemap_tier = excluded.livemap_tier, source = 'stripe', subscription_id = excluded.subscription_id,
                         starts_at = excluded.starts_at, valid_until = excluded.valid_until, updated_at = excluded.updated_at""",
                      (uid, sub["plan"], json.dumps(p.get("perks", [])), p.get("livemap_tier") or "free", sub["id"], starts, until, now))
            if not old or old["plan"] != sub["plan"]:
                self.audit(c, uid, "entitlement", f"{old['plan'] if old else '-'} -> {sub['plan']}")
        want = (p.get("livemap_tier") or "free") if starts <= now else "free"
        if want not in self.cfg.tiers["tiers"]:
            want = "free"
        if LIVEMAP_ALIAS.get(user["tier"], user["tier"]) != want:
            self.set_tier(user, want)
        self.lite_sync(uid)

    def entitlement(self, user) -> dict | None:
        """The account's plan and perks in effect now (None = no subscription perks)."""
        row = self.db.one("SELECT * FROM entitlements WHERE user_id = ?", user["id"])
        now = self.now()
        if row is None or row["starts_at"] > now or (row["valid_until"] is not None and row["valid_until"] < now):
            return None
        return {"plan": PLAN_ALIAS.get(row["plan"], row["plan"]), "perks": json.loads(row["perks"]), "livemap_tier": row["livemap_tier"], "until": row["valid_until"],
                "source": row["source"]}

    def has_perk(self, user, perk: str) -> bool:
        if user is None:
            return False
        if user["role"] == "developer":
            return True
        ent = self.entitlement(user)
        return bool(ent and perk in ent["perks"])

    def subscription_view(self, user) -> dict:
        """What /pro/manage shows."""
        sub = self.db.one("SELECT * FROM subscriptions WHERE user_id = ? ORDER BY updated_at DESC LIMIT 1", user["id"])
        row = self.db.one("SELECT * FROM entitlements WHERE user_id = ?", user["id"])
        return {"sub": dict(sub) if sub else None, "ent": dict(row) if row else None, "effective": self.entitlement(user)}

    # ---------------------------------------------------------------- coupons (P074): provider codes (Paddle discounts / Stripe promotion codes), none made by us
    def coupons_on(self) -> bool:
        c = self.bcfg("coupons")
        if isinstance(c, dict):
            return bool(c.get("enabled", True))
        return bool(self.bcfg("allow_promotion_codes", True))

    def coupon_discount(self, code: str):
        """Provider reference for a code typed on our order page; Problem when unknown/inactive; None without a code."""
        code = (code or "").strip()
        if not code:
            return None
        if not self.coupons_on() or not COUPON_RE.match(code):
            raise Problem(400, "coupon_invalid")
        return self.provider().check_coupon(code)

    # ---------------------------------------------------------------- plan change at the period end (P070: no refunds)
    def change_plan(self, user, plan: str) -> dict:
        """Up- or downgrade takes effect at the end of the paid period, without proration (Stripe: subscription schedule;
        Paddle: noted, item set before the period end with do_not_bill). The same call with the current plan undoes a pending change."""
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        plan = PLAN_ALIAS.get(plan, plan)
        sub = self.active_subscription(user["id"])
        if sub is None or self.plan(plan) is None or sub["status"] not in ENTITLED:
            raise Problem(400, "bad_request")
        current = PLAN_ALIAS.get(sub["plan"], sub["plan"])
        if plan == current and not sub["pending_plan"]:
            return {"pending": None}
        prov = self.provider()
        if not prov.price_id(plan, "month"):
            raise Problem(400, "bad_request")
        sched_id = prov.change_at_period_end(sub, current, plan)
        pending = None if plan == current else plan
        with self.db.tx() as c:
            c.execute("UPDATE subscriptions SET schedule_id = COALESCE(?, schedule_id), pending_plan = ? WHERE id = ?", (sched_id, pending, sub["id"]))
            self.audit(c, user["id"], "plan_change", f"{current} -> {plan} at {sub['period_end']}")
        return {"pending": pending, "at": sub["period_end"]}

    # ---------------------------------------------------------------- gift subscriptions (P068), bound to a Minecraft name
    def gift_cfg(self) -> dict:
        g = {"months": [1, 3, 12], "names": {"basic": 1, "max": 1, "ultra": 2}, "change_days": 7, "days_per_month": 30}
        g.update(self.cfg.plans.get("gifts") or {})
        return g

    def clean_names(self, plan: str, names, allow_empty: bool = False) -> list[str]:
        out = []
        for n in names:
            n = (n or "").strip()
            if not n:
                continue
            if not MC_NAME_RE.match(n):
                raise Problem(400, "mc_name_invalid")
            if n.lower() not in [x.lower() for x in out]:
                out.append(n)
        if (not out and not allow_empty) or len(out) > int(self.gift_cfg()["names"].get(plan, 1)):
            raise Problem(400, "mc_name_count", max=int(self.gift_cfg()["names"].get(plan, 1)))
        return out

    def start_gift(self, user, plan: str, months: int, names, terms: bool, waiver: bool, lang: str = "de", coupon: str = "") -> str:
        """One-off purchase. Names are optional (addendum 4: code redemption on a username): without names the paid gift gets a
        gift code that the receiver redeems on /pro/redeem for a Minecraft name."""
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        prov = self.provider()
        plan = PLAN_ALIAS.get(plan, plan)
        p = self.plan(plan)
        if p is None or months not in self.gift_cfg()["months"]:
            raise Problem(400, "bad_request")
        names = self.clean_names(plan, names, allow_empty=True)
        if not user["verified_at"]:
            raise Problem(403, "verify_first")
        if not terms:
            raise Problem(400, "terms_needed")
        if not self.limiter.allow(f"order:{user['id']}", 10, 3600):
            raise Problem(429, "too_many")
        discount = self.coupon_discount(coupon)      # unknown code: error before anything is stored
        now = self.now()
        cents = p["price_month"] * months
        with self.db.tx() as c:
            oid = c.execute("INSERT INTO orders (user_id, plan, interval, price_cents, terms_version, terms_at, waiver, waiver_text, created_at, kind, months, coupon) "
                            "VALUES (?,?,'once',?,?,?,?,?,?,'gift',?,?)", (user["id"], plan, cents, self.bcfg("terms_version", "draft"), now,
                                                                            1 if waiver else 0, billing_mail.waiver_text(lang) if waiver else "", now,
                                                                            months, (coupon or "").strip()[:40] or None)).lastrowid
            gid = c.execute("INSERT INTO gifts (buyer_id, order_id, plan, months, state, created_at) VALUES (?,?,?,?, 'pending', ?)",
                            (user["id"], oid, plan, months, now)).lastrowid
            for n in names:
                c.execute("INSERT INTO gift_names (gift_id, mc_name, mc_lower, set_at) VALUES (?,?,?,?)", (gid, n, n.lower(), now))
            self.audit(c, user["id"], "order", f"gift order {oid} {plan} x{months}")
        ref, url = prov.gift_checkout_url(oid, gid, user, plan, months, cents, lang, discount)
        with self.db.tx() as c:
            c.execute("UPDATE orders SET session_id = ? WHERE id = ?", (ref, oid))
        if not url:
            raise Problem(502, "payment_error")
        return url

    def on_gift_paid(self, ev, order, user):
        if not ev.paid:
            return
        gift = self.db.one("SELECT * FROM gifts WHERE order_id = ?", order["id"])
        if gift is None or gift["state"] != "pending":
            return
        now = self.now()
        starts = now if order["waiver"] else now + int(self.bcfg("withdrawal_days", 14)) * 86400
        ends = starts + gift["months"] * int(self.gift_cfg()["days_per_month"]) * 86400
        code = gift_code()
        with self.db.tx() as c:
            c.execute("UPDATE gifts SET state = 'active', starts_at = ?, ends_at = ?, code = ? WHERE id = ?", (starts, ends, code, gift["id"]))
            c.execute("UPDATE orders SET completed_at = ?, session_id = COALESCE(session_id, ?) WHERE id = ?", (now, ev.ref, order["id"]))
            if ev.customer and not user["billing_customer"]:
                c.execute("UPDATE users SET billing_customer = ? WHERE id = ?", (ev.customer, user["id"]))
            self.audit(c, user["id"], "gift", f"gift {gift['id']} active until {ends}")
        names = [r["mc_name"] for r in self.gift_names(gift["id"])]
        if self.mail_on("order"):
            self.billing_mail(user, "gift", plan=gift["plan"], months=gift["months"], cents=order["price_cents"], names=", ".join(names),
                              starts=starts, ends=ends, waiver=bool(order["waiver"]), at=now, order_id=order["id"], code=code)

    def gift_names(self, gift_id: int):
        return self.db.all("SELECT * FROM gift_names WHERE gift_id = ? AND removed_at IS NULL ORDER BY id", gift_id)

    def gifts_of(self, uid: int) -> list[dict]:
        out = []
        for g in self.db.all("SELECT * FROM gifts WHERE buyer_id = ? AND state != 'pending' ORDER BY id DESC", uid):
            names = self.gift_names(g["id"])
            last = max([n["set_at"] for n in names] or [0])
            out.append({**dict(g), "names": [n["mc_name"] for n in names],
                        "change_from": last + int(self.gift_cfg()["change_days"]) * 86400})
        return out

    def gift_rename(self, user, gift_id: int, names) -> list[str]:
        g = self.db.one("SELECT * FROM gifts WHERE id = ? AND buyer_id = ?", gift_id, user["id"])
        now = self.now()
        if g is None or g["state"] != "active" or (g["ends_at"] or 0) <= now:
            raise Problem(404, "not_found")
        names = self.clean_names(g["plan"], names)
        cur = self.gift_names(g["id"])
        if sorted(n.lower() for n in names) == sorted(n["mc_lower"] for n in cur):
            return names
        last = max([n["set_at"] for n in cur] or [0])
        wait = last + int(self.gift_cfg()["change_days"]) * 86400
        if now < wait:
            raise Problem(429, "mc_name_wait", date=time.strftime("%d.%m.%Y", time.gmtime(wait)))
        with self.db.tx() as c:
            c.execute("UPDATE gift_names SET removed_at = ? WHERE gift_id = ? AND removed_at IS NULL", (now, g["id"]))
            for n in names:
                c.execute("INSERT INTO gift_names (gift_id, mc_name, mc_lower, set_at) VALUES (?,?,?,?)", (g["id"], n, n.lower(), now))
            self.audit(c, user["id"], "gift_rename", f"gift {g['id']}")
        return names

    def redeem_gift(self, code: str, names, ip: str, user=None) -> dict:
        """Addendum 4: redeems a gift code on Minecraft name(s) - once per gift (later changes: the buyer, every 7 days). No account
        needed; rate limited per address against guessing (12 random characters from 32)."""
        if not self.limiter.allow(f"redeem:{ip}", 10, 3600):
            raise Problem(429, "too_many")
        code = (code or "").strip().upper()
        if not GIFT_CODE_RE.match(code):
            raise Problem(400, "gift_code_invalid")
        now = self.now()
        g = self.db.one("SELECT * FROM gifts WHERE code = ?", code)
        if g is None or g["state"] != "active" or (g["ends_at"] or 0) <= now:
            raise Problem(400, "gift_code_invalid")
        if self.gift_names(g["id"]):
            raise Problem(409, "gift_code_used")
        names = self.clean_names(g["plan"], names)
        with self.db.tx() as c:
            for n in names:
                c.execute("INSERT INTO gift_names (gift_id, mc_name, mc_lower, set_at) VALUES (?,?,?,?)", (g["id"], n, n.lower(), now))
            c.execute("UPDATE gifts SET redeemed_at = ?, redeemed_by = ? WHERE id = ?", (now, user["id"] if user else None, g["id"]))
            self.audit(c, g["buyer_id"], "gift_redeem", f"gift {g['id']}")
        return {"plan": g["plan"], "names": names, "starts": g["starts_at"], "ends": g["ends_at"]}

    def mc_entitlement(self, name: str) -> dict:
        """Perks bound to a Minecraft name by active gifts (for mod perks; public, rate limited at the API)."""
        if not MC_NAME_RE.match(name or ""):
            raise Problem(400, "mc_name_invalid")
        now = self.now()
        rank = {k: i for i, k in enumerate(self.cfg.plans["order"])}
        rows = self.db.all("SELECT g.* FROM gift_names n JOIN gifts g ON g.id = n.gift_id WHERE n.mc_lower = ? AND n.removed_at IS NULL "
                           "AND g.state = 'active' AND g.starts_at <= ? AND g.ends_at > ?", name.lower(), now, now)
        if not rows:
            return {"name": name, "plan": None, "perks": [], "flags": dict(self.cfg.plans.get("free_flags") or {})}
        best = max(rows, key=lambda g: (rank.get(g["plan"], -1), g["ends_at"]))
        p = self.plan(best["plan"]) or {}
        flags = dict(self.cfg.plans.get("free_flags") or {})
        flags.update(p.get("flags") or {})
        perks = [x for x in p.get("perks", []) if x not in ("livemap", "lite_seats")]   # account-bound perks stay with accounts
        return {"name": name, "plan": best["plan"], "perks": perks, "flags": flags, "until": max(g["ends_at"] for g in rows if g["plan"] == best["plan"])}

    # ---------------------------------------------------------------- contact form (second contact path, owner part 2)
    def contact_token(self) -> str:
        ts = str(self.now())
        return ts + "." + sec.hmac_hex(self.cfg.secret, "contact", ts)[:24]

    def contact_form(self, f: dict, ip: str) -> bool:
        """Sends the message to the configured address (Reply-To = sender). Honeypot or a too fast form = silently dropped
        (True, so bots learn nothing); never a copy to the sender (no spam relay)."""
        if (f.get("website") or "").strip():
            return True
        ts, _, sig = (f.get("t") or "").partition(".")
        if not ts.isdigit() or not sec.same(sig, sec.hmac_hex(self.cfg.secret, "contact", ts)[:24]):
            raise Problem(403, "csrf")
        age = self.now() - int(ts)
        if age < 3:
            return True
        if age > 7200:
            raise Problem(403, "csrf")
        email = sec.normalize_email(f.get("email", ""))
        if not email:
            raise Problem(400, "email_invalid")
        name = " ".join((f.get("name") or "").split())[:80]
        topic = f.get("topic") if f.get("topic") in ("allgemein", "datenschutz", "melden") else "allgemein"
        message = (f.get("message") or "").strip()
        if not (10 <= len(message) <= 5000):
            raise Problem(400, "contact_message")
        if not self.limiter.allow(f"contact:{ip}", 5, 3600) or not self.limiter.allow(f"contact-mail:{email}", 3, 3600):
            raise Problem(429, "too_many")
        to = (self.cfg.get("contact") or {}).get("form_to") or self.contact_address(topic)
        if not to:
            raise Problem(503, "contact_off")
        clean = "".join(ch for ch in message if ch.isprintable() or ch in "\n\t")
        subject = f"[Kontaktformular/{topic}] {name or email}"[:120]
        self.mailer.send(to, subject, f"Thema: {topic}\nVon: {name} <{email}>\n\n{clean}\n", reply_to=email)
        return True

    # ---------------------------------------------------------------- fellow-player seats "lite" (P039, owner 19.11.)
    def lite_offer(self, user) -> tuple[str, int] | None:
        """(lite kind, seat count) the account may give - only from its OWN paid entitlement (a seat holder gets none: no chaining)."""
        row = self.db.one("SELECT * FROM entitlements WHERE user_id = ? AND source = 'stripe'", user["id"])
        now = self.now()
        if row is None or row["starts_at"] > now or (row["valid_until"] is not None and row["valid_until"] < now):
            return None
        lite = (self.plan(row["plan"]) or {}).get("lite") or {}
        kind, seats = lite.get("kind"), int(lite.get("seats") or 0)
        if not kind or seats <= 0 or kind not in self.cfg.plans["lite"]:
            return None
        return kind, seats

    def lite_seats(self, owner_id: int):
        return self.db.all("SELECT s.*, u.email, u.name FROM lite_seats s JOIN users u ON u.id = s.member_id "
                           "WHERE s.owner_id = ? AND s.removed_at IS NULL ORDER BY s.id", owner_id)

    def lite_used(self, owner_id: int) -> int:
        """Active seats + seats freed within the lock days (a seat cannot be rotated through many accounts)."""
        lock = int(self.cfg.plans["lite_rules"].get("reassign_lock_days", 30)) * 86400
        return self.db.value("SELECT COUNT(*) FROM lite_seats WHERE owner_id = ? AND (removed_at IS NULL OR removed_at > ?)",
                             owner_id, self.now() - lock, default=0)

    def lite_add(self, owner, email: str):
        offer = self.lite_offer(owner)
        if offer is None:
            raise Problem(403, "lite_none")
        if not self.limiter.allow(f"lite:{owner['id']}", 10, 3600):
            raise Problem(429, "too_many")
        member = self.user_by_email(email)
        if (member is None or member["id"] == owner["id"] or not member["verified_at"] or member["disabled"]
                or self.active_subscription(member["id"])
                or self.db.one("SELECT 1 FROM lite_seats WHERE member_id = ? AND removed_at IS NULL", member["id"])):
            raise Problem(400, "lite_target")
        if self.lite_used(owner["id"]) >= offer[1]:
            raise Problem(409, "lite_full", days=int(self.cfg.plans["lite_rules"].get("reassign_lock_days", 30)))
        with self.db.tx() as c:
            c.execute("INSERT INTO lite_seats (owner_id, member_id, created_at) VALUES (?,?,?)", (owner["id"], member["id"], self.now()))
            self.audit(c, owner["id"], "lite_add", f"member {member['id']}")
        self.recompute(member["id"])

    def lite_remove(self, owner_id: int, member_id: int, actor_id: int | None = None):
        with self.db.tx() as c:
            n = c.execute("UPDATE lite_seats SET removed_at = ? WHERE owner_id = ? AND member_id = ? AND removed_at IS NULL",
                          (self.now(), owner_id, member_id)).rowcount
            if n:
                self.audit(c, actor_id or owner_id, "lite_remove", f"owner {owner_id} member {member_id}")
        if n:
            self.recompute(member_id)

    def lite_sync(self, owner_id: int):
        """After the owner's entitlement changed: seats over the count (newest first) end; every member is recomputed."""
        seats = self.lite_seats(owner_id)
        if not seats:
            return
        offer = self.lite_offer(self.user(owner_id))
        keep = offer[1] if offer else 0
        for s in seats[keep:]:
            with self.db.tx() as c:
                c.execute("UPDATE lite_seats SET removed_at = ? WHERE id = ?", (self.now(), s["id"]))
                self.audit(c, owner_id, "lite_end", f"member {s['member_id']}")
        for s in seats:
            self.recompute(s["member_id"])

    def lite_entitle(self, user, old) -> bool:
        """Entitlement of a seat holder without an own subscription (source 'lite'); valid as long as the owner's own is."""
        seat = self.db.one("SELECT * FROM lite_seats WHERE member_id = ? AND removed_at IS NULL", user["id"])
        if seat is None:
            return False
        owner = self.user(seat["owner_id"])
        offer = self.lite_offer(owner) if owner else None
        if offer is None:
            return False
        kind = offer[0]
        lite = self.cfg.plans["lite"][kind]
        until = self.db.value("SELECT valid_until FROM entitlements WHERE user_id = ?", owner["id"])
        now = self.now()
        with self.db.tx() as c:
            c.execute("""INSERT INTO entitlements (user_id, plan, perks, livemap_tier, source, subscription_id, starts_at, valid_until, updated_at)
                         VALUES (?,?,?,?, 'lite', NULL, ?,?,?) ON CONFLICT(user_id) DO UPDATE SET plan = excluded.plan, perks = excluded.perks,
                         livemap_tier = excluded.livemap_tier, source = 'lite', subscription_id = NULL, valid_until = excluded.valid_until,
                         updated_at = excluded.updated_at""",
                      (user["id"], kind, json.dumps(lite.get("perks", [])), lite["livemap_tier"], now, until, now))
            if not old or old["plan"] != kind:
                self.audit(c, user["id"], "entitlement", f"{old['plan'] if old else '-'} -> {kind} (seat of {owner['id']})")
        want = lite["livemap_tier"] if lite["livemap_tier"] in self.cfg.tiers["tiers"] else "free"
        if LIVEMAP_ALIAS.get(user["tier"], user["tier"]) != want:
            self.set_tier(user, want)
        return True

    def lite_holder(self, user):
        """(owner row, kind) when the account holds a seat."""
        seat = self.db.one("SELECT * FROM lite_seats WHERE member_id = ? AND removed_at IS NULL", user["id"])
        return (self.user(seat["owner_id"]), seat) if seat else (None, None)

    # ---------------------------------------------------------------- cancel button (section 312k BGB)
    def cancel_request(self, email: str, kind: str, reason: str, user=None, ip: str = "?") -> dict:
        """Takes the declaration as given, confirms it by mail right away (to the given address and the account's address),
        and - when it belongs to an account with a running subscription - ends that at the end of the paid period. The answer
        tells nothing about whether an account exists."""
        addr = sec.normalize_email(email)
        if not addr:
            raise Problem(400, "email_invalid")
        if kind not in ("ordinary", "extraordinary"):
            raise Problem(400, "bad_request")
        if not self.limiter.allow(f"cancel:{ip}", 10, 3600) or not self.limiter.allow(f"cancel-mail:{addr}", 5, 3600):
            raise Problem(429, "too_many")
        owner = user if user is not None and sec.normalize_email(user["email"]) == addr else self.user_by_email(addr)
        sub = self.active_subscription(owner["id"]) if owner else None
        now = self.now()
        effective, state = None, "received"
        if sub is not None and self.provider_for_sub(sub).configured():
            try:
                self.provider_for_sub(sub).cancel_at_period_end(sub)
                effective, state = sub["period_end"], "scheduled"
                with self.db.tx() as c:   # a pending plan change / yearly-then-monthly note ends with the cancellation
                    c.execute("UPDATE subscriptions SET cancel_at_period_end = 1, schedule_id = NULL, pending_plan = NULL, pending_interval = NULL "
                              "WHERE id = ?", (sub["id"],))
            except Problem:
                state = "manual"     # owner handles it (admin page); the declaration is still received and confirmed
        with self.db.tx() as c:
            c.execute("INSERT INTO cancellations (at, email, user_id, subscription_id, kind, reason, effective_at, state) VALUES (?,?,?,?,?,?,?,?)",
                      (now, addr, owner["id"] if owner else None, sub["id"] if sub else None, kind, (reason or "")[:500], effective, state))
            if owner:
                self.audit(c, owner["id"], "cancel", f"{kind} {state}")
        if self.mail_on("cancel"):
            lang = (owner["lang"] if owner else "") or "de"
            subject, text = billing_mail.render("cancel", lang, self, at=now, how=kind, effective=effective)
            self.mailer.send(addr, subject, text)
            if owner and sec.normalize_email(owner["email"]) != addr:
                self.mailer.send(owner["email"], subject, text)
        return {"at": now, "effective": effective}

    # ---------------------------------------------------------------- housekeeping
    def provider_for_sub(self, sub):
        """The provider a running subscription belongs to (customer id prefix; after a switch old subscriptions stay where they are)."""
        cus = sub["customer"] or ""
        for name in payments.PROVIDERS:
            prov = self.provider(name)
            if cus and cus.startswith(prov.customer_prefix):
                return prov
        return self.provider()

    def billing_maintenance(self):
        """Safety net for lost webhooks: start after the withdrawal period, expiry, end of grace; notice before the year ends;
        provider housekeeping (Paddle: changes due before the period end)."""
        for name in payments.PROVIDERS:
            try:
                self.provider(name).maintenance()
            except Exception:   # never stop the entitlement pass below
                pass
        with self.db.tx() as c:
            c.execute("UPDATE gifts SET state = 'ended' WHERE state = 'active' AND ends_at <= ?", (self.now(),))
        uids = {r[0] for r in self.db.all("SELECT user_id FROM entitlements")} | \
               {r[0] for r in self.db.all("SELECT user_id FROM subscriptions WHERE user_id IS NOT NULL")} | \
               {r[0] for r in self.db.all("SELECT member_id FROM lite_seats WHERE removed_at IS NULL")}
        for uid in sorted(uids):
            self.recompute(uid)
        if not self.mail_on("year_notice"):
            return
        days = int(self.bcfg("year_notice_days", 42))
        now = self.now()
        for sub in self.db.all("SELECT * FROM subscriptions WHERE interval = 'year' AND (schedule_id IS NOT NULL OR pending_interval = 'month') AND status = 'active' "
                               "AND cancel_at_period_end = 0 AND period_end BETWEEN ? AND ?", now, now + days * 86400):
            user = self.user(sub["user_id"]) if sub["user_id"] else None
            if user and self.notice_once(user["id"], "year_notice", f"{sub['id']}:{sub['period_end']}"):
                self.billing_mail(user, "year_notice", plan=sub["plan"], end=sub["period_end"])
