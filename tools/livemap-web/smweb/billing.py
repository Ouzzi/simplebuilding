"""Simple Mods Pro (N108): one account subscription (Basic/Pro/Ultra) that unlocks a Simple Livemap tier and further perks
(early access first). Stripe Checkout for payment data, customer portal, signed + idempotent webhooks, entitlements table
(account -> plan -> perks), downgrade/cancel/expiry, mails. Plan: docs/ai/PLAN-B-N108-STRIPE-2026-11-19.md.

Off while ``billing.json`` has no ``enabled: true`` + keys: every paying path answers "coming soon". The HTTP calls to Stripe
use the standard library only and go through ``self.stripe_api`` (tests and the local harness put a fake there; no real call
happens without a key in the server's billing file).

Order flow (AGB Anlage 3): /pro -> /pro/order (summary, terms box required, early-start box voluntary, button "zahlungspflichtig
bestellen") -> Stripe Checkout (payment data) -> webhooks. Without the early-start consent (section 356 Abs. 4/5 BGB) the
entitlement starts only after the withdrawal period (``withdrawal_days``).

Yearly plan (owner 12.11.2026, AGB 11.3): 12 months paid in advance, then MONTHLY at the monthly price, cancellable to the end of
every paid month = a subscription schedule made right after checkout (phase 1: year price, 1 iteration; phase 2: month price,
``end_behavior: release``).
"""
from __future__ import annotations

import hashlib
import hmac
import json
import time
import urllib.error
import urllib.parse
import urllib.request

from . import billing_mail
from . import security as sec
from .config import LIVEMAP_ALIAS, PLAN_ALIAS
from .errors import Problem

TOLERANCE = 300
ENTITLED = ("active", "trialing")
GRACE = ("past_due",)
RENEWAL_BUFFER = 2 * 86400
API = "https://api.stripe.com/v1/"


def verify_signature(payload: bytes, header: str | None, secret: str, now: float | None = None, tolerance: int = TOLERANCE) -> bool:
    """Stripe's scheme: header ``t=<unix>,v1=<hex>[,v1=<hex>...]``; signed text ``<t>.<raw body>``; any v1 may match."""
    if not header or not secret or not isinstance(payload, (bytes, bytearray)):
        return False
    ts, sigs = None, []
    for part in header.split(","):
        k, _, v = part.strip().partition("=")
        if k == "t" and v.isdigit():
            ts = int(v)
        elif k == "v1" and v:
            sigs.append(v)
    if ts is None or not sigs:
        return False
    if abs((now if now is not None else time.time()) - ts) > tolerance:
        return False
    expected = hmac.new(secret.encode("utf-8"), str(ts).encode("ascii") + b"." + bytes(payload), hashlib.sha256).hexdigest()
    return any(hmac.compare_digest(expected, s) for s in sigs)


def sign(payload: bytes, secret: str, ts: int | None = None) -> str:
    """Header value as Stripe would send it (tests, local webhook replays)."""
    ts = int(time.time()) if ts is None else ts
    sig = hmac.new(secret.encode("utf-8"), str(ts).encode("ascii") + b"." + payload, hashlib.sha256).hexdigest()
    return f"t={ts},v1={sig}"


def flatten(data: dict, prefix: str = "") -> list[tuple[str, str]]:
    """Stripe's form encoding: nested dicts/lists as ``a[b][0][c]=v``; booleans as true/false; None left out."""
    out = []
    for k, v in data.items():
        key = f"{prefix}[{k}]" if prefix else str(k)
        if isinstance(v, dict):
            out += flatten(v, key)
        elif isinstance(v, (list, tuple)):
            for i, item in enumerate(v):
                out += flatten(item, f"{key}[{i}]") if isinstance(item, dict) else [(f"{key}[{i}]", str(item))]
        elif isinstance(v, bool):
            out.append((key, "true" if v else "false"))
        elif v is not None:
            out.append((key, str(v)))
    return out


class StripeError(Exception):
    def __init__(self, status: int, code: str):
        super().__init__(f"stripe {status} {code}")
        self.status, self.code = status, code


class StripeHTTP:
    """Minimal Stripe client (standard library). The key is only used in the Authorization header, never logged."""

    def __init__(self, key: str, version: str | None = None, timeout: int = 20):
        self._key = key
        self.version = version
        self.timeout = timeout

    def __repr__(self):
        return "StripeHTTP(key=***)"

    def call(self, method: str, path: str, data: dict | None = None, idem: str | None = None) -> dict:
        query = urllib.parse.urlencode(flatten(data or {}))
        url, body = API + path, None
        if method == "GET":
            url += ("?" + query) if query else ""
        else:
            body = query.encode("ascii")
        headers = {"Authorization": "Bearer " + self._key}
        if self.version:
            headers["Stripe-Version"] = self.version
        if idem and method == "POST":
            headers["Idempotency-Key"] = idem
        req = urllib.request.Request(url, data=body, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as r:
                return json.load(r)
        except urllib.error.HTTPError as ex:
            try:
                code = (json.load(ex).get("error") or {}).get("code") or "error"
            except (ValueError, AttributeError):
                code = "error"
            raise StripeError(ex.code, str(code)[:60]) from None
        except (urllib.error.URLError, OSError, ValueError):
            raise StripeError(0, "network") from None


class BillingMixin:
    stripe_api = None   # set by tests/harness (fake); else made from the billing file on first use

    # ---------------------------------------------------------------- configuration
    def billing_on(self) -> bool:
        b = self.cfg.billing
        return bool(b.get("enabled")) and bool(b.get("secret_key")) and bool(b.get("webhook_secret"))

    def bcfg(self, key: str, default=None):
        return self.cfg.billing.get(key, default)

    def plan(self, name: str) -> dict | None:
        return self.cfg.plans["plans"].get(PLAN_ALIAS.get(name, name))

    def price_id(self, plan: str, interval: str) -> str | None:
        prices = self.bcfg("prices") or {}
        plan = PLAN_ALIAS.get(plan, plan)
        old = next((k for k, v in PLAN_ALIAS.items() if v == plan), None)    # billing files written before the rename
        return (prices.get(plan) or prices.get(old) or {}).get(interval)

    def plan_for_price(self, price: str | None) -> tuple[str | None, str | None]:
        for plan, ivs in (self.bcfg("prices") or {}).items():
            for interval, pid in (ivs or {}).items():
                if price and pid == price:
                    return PLAN_ALIAS.get(plan, plan), interval
        return None, None

    def plan_for_livemap_tier(self, tier: str) -> str | None:
        return next((k for k, p in self.cfg.plans["plans"].items() if p.get("livemap_tier") == tier), None)

    def vat(self) -> dict:
        v = {"mode": "small_business", "rate_percent": 19, "stripe_tax": False}
        v.update(self.bcfg("vat") or {})
        return v

    def mail_on(self, kind: str) -> bool:
        return bool((self.bcfg("mails") or {}).get(kind, True))

    def api(self):
        if self.stripe_api is None:
            if not self.billing_on():
                raise Problem(503, "coming_soon")
            self.stripe_api = StripeHTTP(self.bcfg("secret_key"), self.bcfg("api_version"))
        return self.stripe_api

    def stripe(self, method: str, path: str, data: dict | None = None, idem: str | None = None) -> dict:
        try:
            return self.api().call(method, path, data, idem)
        except StripeError:
            raise Problem(502, "payment_error")

    # ---------------------------------------------------------------- order (checkout)
    def active_subscription(self, uid: int):
        return self.db.one("SELECT * FROM subscriptions WHERE user_id = ? AND status IN ('active', 'trialing', 'past_due', 'incomplete') "
                           "ORDER BY updated_at DESC LIMIT 1", uid)

    def start_order(self, user, plan: str, interval: str, terms: bool, waiver: bool, lang: str = "de") -> str:
        """Records the order with its consents and returns the Stripe Checkout URL (subscription mode)."""
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        plan = PLAN_ALIAS.get(plan, plan)
        p = self.plan(plan)
        price = self.price_id(plan, interval)
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
        with self.db.tx() as c:
            cur = c.execute("INSERT INTO orders (user_id, plan, interval, price_cents, terms_version, terms_at, waiver, waiver_text, created_at) "
                            "VALUES (?,?,?,?,?,?,?,?,?)", (user["id"], plan, interval, cents, self.bcfg("terms_version", "draft"), now,
                                                           1 if waiver else 0, billing_mail.waiver_text(lang) if waiver else "", now))
            oid = cur.lastrowid
            self.audit(c, user["id"], "order", f"order {oid} {plan}/{interval} waiver={1 if waiver else 0}")
        meta = {"order": str(oid), "plan": plan, "interval": interval, "user": str(user["id"])}
        acc = lambda path: self.cfg.url("account", path)  # noqa: E731
        data = {"mode": "subscription", "line_items": [{"price": price, "quantity": 1}], "client_reference_id": str(user["id"]),
                "success_url": acc("/pro/done?session_id={CHECKOUT_SESSION_ID}"),
                "cancel_url": acc(f"/pro/order?plan={plan}&interval={interval}"),
                "locale": "de" if lang == "de" else "en", "metadata": meta, "subscription_data": {"metadata": meta},
                "custom_text": {"submit": {"message": billing_mail.submit_note(lang)}}}
        if self.bcfg("allow_promotion_codes"):
            data["allow_promotion_codes"] = True
        if user["billing_customer"]:
            data["customer"] = user["billing_customer"]
        else:
            data["customer_email"] = user["email"]
        if self.vat()["stripe_tax"]:
            data["automatic_tax"] = {"enabled": True}
            data["billing_address_collection"] = "required"
            data["tax_id_collection"] = {"enabled": True}
            if user["billing_customer"]:
                data["customer_update"] = {"address": "auto", "name": "auto"}
        session = self.stripe("POST", "checkout/sessions", data, idem=f"order-{oid}")
        with self.db.tx() as c:
            c.execute("UPDATE orders SET session_id = ? WHERE id = ?", (session.get("id"), oid))
        if not session.get("url"):
            raise Problem(502, "payment_error")
        return session["url"]

    def portal(self, user) -> str:
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        if not user["billing_customer"]:
            raise Problem(400, "bad_request")
        data = {"customer": user["billing_customer"], "return_url": self.cfg.url("account", "/pro/manage")}
        if self.bcfg("portal_configuration"):
            data["configuration"] = self.bcfg("portal_configuration")
        return self.stripe("POST", "billing_portal/sessions", data)["url"]

    # ---------------------------------------------------------------- webhook
    def webhook(self, payload: bytes, header: str | None) -> dict:
        """Verified events only, each event id once (Stripe retries; duplicates answer 200 without effect)."""
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        if not verify_signature(payload, header, self.bcfg("webhook_secret"), now=self.now()):
            raise Problem(400, "signature")
        try:
            event = json.loads(payload)
        except ValueError:
            raise Problem(400, "bad_request")
        if not isinstance(event, dict):
            raise Problem(400, "bad_request")
        eid, kind = str(event.get("id") or ""), str(event.get("type") or "")
        created = event.get("created") if isinstance(event.get("created"), int) else self.now()
        obj = (event.get("data") or {}).get("object") or {}
        if not eid or not isinstance(obj, dict):
            raise Problem(400, "bad_request")
        with self.db.tx() as c:
            if c.execute("SELECT 1 FROM stripe_events WHERE id = ?", (eid,)).fetchone():
                return {"received": True, "duplicate": True}
            c.execute("INSERT INTO stripe_events (id, type, created, received_at) VALUES (?,?,?,?)", (eid[:100], kind[:80], created, self.now()))
        try:
            if kind == "checkout.session.completed":
                self.on_checkout_completed(obj)
            elif kind.startswith("customer.subscription."):
                self.on_subscription(obj, kind, created)
            elif kind == "invoice.paid":
                self.on_invoice(obj, paid=True)
            elif kind == "invoice.payment_failed":
                self.on_invoice(obj, paid=False)
        except Exception:
            with self.db.tx() as c:   # not handled: forget the id so Stripe's retry runs it again
                c.execute("DELETE FROM stripe_events WHERE id = ?", (eid,))
            raise
        return {"received": True}

    def user_for(self, obj: dict):
        meta = obj.get("metadata") or {}
        uid = meta.get("user") or obj.get("client_reference_id")
        if isinstance(uid, str) and uid.isdigit():
            u = self.user(int(uid))
            if u and (not u["billing_customer"] or not obj.get("customer") or u["billing_customer"] == obj.get("customer")):
                return u
        if obj.get("customer"):
            return self.db.one("SELECT * FROM users WHERE billing_customer = ?", obj["customer"])
        return None

    def on_checkout_completed(self, obj: dict):
        if obj.get("mode") not in (None, "subscription"):
            return
        user = self.user_for(obj)
        if user is None:
            return
        meta = obj.get("metadata") or {}
        order = self.db.one("SELECT * FROM orders WHERE id = ? AND user_id = ?", int(meta["order"]), user["id"]) if str(meta.get("order", "")).isdigit() else None
        sub_id = obj.get("subscription") if isinstance(obj.get("subscription"), str) else None
        if order and sub_id and order["interval"] == "year":
            self.year_schedule(sub_id, PLAN_ALIAS.get(order["plan"], order["plan"]))   # first: when it fails, nothing is marked done and Stripe's retry repeats all
        with self.db.tx() as c:
            if obj.get("customer") and not user["billing_customer"]:
                c.execute("UPDATE users SET billing_customer = ? WHERE id = ?", (obj["customer"], user["id"]))
            if order and not order["completed_at"]:
                c.execute("UPDATE orders SET completed_at = ?, subscription_id = ?, session_id = COALESCE(session_id, ?) WHERE id = ?",
                          (self.now(), sub_id, obj.get("id"), order["id"]))
            if sub_id:
                c.execute("UPDATE subscriptions SET order_id = COALESCE(order_id, ?), user_id = COALESCE(user_id, ?) WHERE id = ?",
                          (order["id"] if order else None, user["id"], sub_id))
            self.audit(c, user["id"], "checkout", f"order {order['id'] if order else '-'} sub {sub_id or '-'}")
        if order and not order["completed_at"] and self.mail_on("order"):
            self.billing_mail(user, "order", plan=order["plan"], interval=order["interval"], cents=order["price_cents"],
                              waiver=bool(order["waiver"]), at=self.now(), order_id=order["id"])
        self.recompute(user["id"])

    def year_schedule(self, sub_id: str, plan: str):
        """Yearly in advance, then monthly (AGB 11.3): schedule from the subscription, phase 1 = current year price for one
        iteration, phase 2 = month price, release afterwards (it runs on as a normal monthly subscription)."""
        row = self.db.one("SELECT schedule_id FROM subscriptions WHERE id = ?", sub_id)
        if row and row["schedule_id"]:
            return row["schedule_id"]
        month, year = self.price_id(plan, "month"), self.price_id(plan, "year")
        sched = self.stripe("POST", "subscription_schedules", {"from_subscription": sub_id}, idem=f"sched-{sub_id}")
        phase = (sched.get("phases") or [{}])[0]
        self.stripe("POST", f"subscription_schedules/{sched['id']}", {
            "end_behavior": "release", "proration_behavior": "none",
            "metadata": {"plan": plan, "kind": "year_then_month"},
            "phases": [{"items": [{"price": year, "quantity": 1}], "start_date": phase.get("start_date"), "end_date": phase.get("end_date")},
                       {"items": [{"price": month, "quantity": 1}], "iterations": 1}]}, idem=f"sched-phases-{sub_id}")
        with self.db.tx() as c:
            c.execute("UPDATE subscriptions SET schedule_id = ? WHERE id = ?", (sched["id"], sub_id))
            if not row:   # the subscription event may come later; keep the schedule id in a stub row
                c.execute("INSERT OR IGNORE INTO subscriptions (id, customer, plan, interval, status, schedule_id, updated_at) "
                          "VALUES (?, '', ?, 'year', 'incomplete', ?, ?)", (sub_id, plan, sched["id"], self.now()))
        return sched["id"]

    def on_subscription(self, obj: dict, kind: str, created: int):
        sid = obj.get("id")
        if not isinstance(sid, str) or not sid:
            return
        user = self.user_for(obj)
        items = ((obj.get("items") or {}).get("data") or [])
        price = ((items[0].get("price") or {}).get("id") if items and isinstance(items[0].get("price"), dict) else None)
        plan, interval = self.plan_for_price(price)
        meta = obj.get("metadata") or {}
        plan = plan or (PLAN_ALIAS.get(meta.get("plan"), meta.get("plan")) if self.plan(meta.get("plan") or "") else None)
        old_tier = LIVEMAP_ALIAS.get(meta.get("tier") or "", meta.get("tier") or "")
        if plan is None and self.cfg.tiers["tiers"].get(old_tier):   # subscriptions from before N108 (livemap tier)
            plan = self.plan_for_livemap_tier(old_tier)
        interval = interval or meta.get("interval") or "month"
        status = "canceled" if kind == "customer.subscription.deleted" else str(obj.get("status") or "")
        period_end = obj.get("current_period_end") or (items[0].get("current_period_end") if items else None)
        old = self.db.one("SELECT * FROM subscriptions WHERE id = ?", sid)
        if old and old["event_at"] > created:
            return   # older than what we know (Stripe does not guarantee order)
        if plan is None:
            plan = PLAN_ALIAS.get(old["plan"], old["plan"]) if old else ""
        order_id = int(meta["order"]) if str(meta.get("order", "")).isdigit() else None
        with self.db.tx() as c:
            c.execute("""INSERT INTO subscriptions (id, user_id, customer, plan, interval, status, period_end, cancel_at_period_end, order_id, event_at, updated_at)
                         VALUES (?,?,?,?,?,?,?,?,?,?,?)
                         ON CONFLICT(id) DO UPDATE SET user_id = COALESCE(excluded.user_id, user_id), customer = excluded.customer,
                           plan = excluded.plan, interval = excluded.interval, status = excluded.status, period_end = excluded.period_end,
                           cancel_at_period_end = excluded.cancel_at_period_end, order_id = COALESCE(order_id, excluded.order_id),
                           event_at = excluded.event_at, updated_at = excluded.updated_at""",
                      (sid, user["id"] if user else None, obj.get("customer") or "", plan, interval, status,
                       period_end if isinstance(period_end, int) else None, 1 if obj.get("cancel_at_period_end") else 0,
                       order_id, created, self.now()))
            if user and obj.get("customer") and not user["billing_customer"]:
                c.execute("UPDATE users SET billing_customer = ? WHERE id = ?", (obj["customer"], user["id"]))
        if user:
            self.recompute(user["id"])

    def on_invoice(self, obj: dict, paid: bool):
        user = self.user_for({"customer": obj.get("customer"), "metadata": ((obj.get("subscription_details") or {}).get("metadata") or {})})
        if user is None:
            return
        inv = str(obj.get("id") or "")
        if paid:
            sub = obj.get("subscription") if isinstance(obj.get("subscription"), str) else None
            ends = [(ln.get("period") or {}).get("end") or 0 for ln in ((obj.get("lines") or {}).get("data") or []) if isinstance(ln, dict)]
            end = max(ends or [0])
            if sub and end:
                with self.db.tx() as c:
                    c.execute("UPDATE subscriptions SET period_end = MAX(COALESCE(period_end, 0), ?), updated_at = ? WHERE id = ?", (end, self.now(), sub))
            if self.mail_on("invoice") and self.notice_once(user["id"], "invoice", inv):
                self.billing_mail(user, "invoice", cents=obj.get("amount_paid") or 0, number=obj.get("number") or inv,
                                  link=obj.get("hosted_invoice_url") or "", pdf=obj.get("invoice_pdf") or "")
            self.recompute(user["id"])
        elif self.mail_on("payment_failed") and self.notice_once(user["id"], "failed", f"{inv}:{obj.get('attempt_count') or 0}"):
            self.billing_mail(user, "payment_failed", cents=obj.get("amount_due") or 0, grace=int(self.bcfg("grace_days", 7)),
                              link=self.cfg.url("account", "/pro/manage"))

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
        if sub is not None and self.billing_on():
            try:
                if sub["schedule_id"]:   # yearly plan: a subscription under a schedule cannot be cancelled directly
                    self.stripe("POST", f"subscription_schedules/{sub['schedule_id']}/release", {}, idem=f"release-{sub['id']}")
                self.stripe("POST", f"subscriptions/{sub['id']}", {"cancel_at_period_end": True}, idem=f"cancel-{sub['id']}-{now // 3600}")
                effective, state = sub["period_end"], "scheduled"
                with self.db.tx() as c:
                    c.execute("UPDATE subscriptions SET cancel_at_period_end = 1, schedule_id = NULL WHERE id = ?", (sub["id"],))
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
    def billing_maintenance(self):
        """Safety net for lost webhooks: start after the withdrawal period, expiry, end of grace; notice before the year ends."""
        uids = {r[0] for r in self.db.all("SELECT user_id FROM entitlements")} | \
               {r[0] for r in self.db.all("SELECT user_id FROM subscriptions WHERE user_id IS NOT NULL")} | \
               {r[0] for r in self.db.all("SELECT member_id FROM lite_seats WHERE removed_at IS NULL")}
        for uid in sorted(uids):
            self.recompute(uid)
        if not self.mail_on("year_notice"):
            return
        days = int(self.bcfg("year_notice_days", 42))
        now = self.now()
        for sub in self.db.all("SELECT * FROM subscriptions WHERE interval = 'year' AND schedule_id IS NOT NULL AND status = 'active' "
                               "AND cancel_at_period_end = 0 AND period_end BETWEEN ? AND ?", now, now + days * 86400):
            user = self.user(sub["user_id"]) if sub["user_id"] else None
            if user and self.notice_once(user["id"], "year_notice", f"{sub['id']}:{sub['period_end']}"):
                self.billing_mail(user, "year_notice", plan=sub["plan"], end=sub["period_end"])
