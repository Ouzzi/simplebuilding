"""Paddle Billing (API v2) as the default payment provider (N108 addendum 4). Paddle is the merchant of record: it sells the
subscription/gift to the buyer (Paddle Buyer Terms), collects the payment, calculates and remits VAT and issues the invoices;
we provide the service under our terms of use.

- Checkout: ``POST /transactions`` (catalogue price, custom data with our order) -> our page ``/pro/pay?_ptxn=<txn>`` opens the
  Paddle.js overlay (no inline script; static/paddle-checkout.js).
- Without the early-start consent the price variant with a trial of the withdrawal period is used (``month_trial``/``year_trial``),
  like the Stripe trial; without such a variant only the perks start later (entitlement rule of the billing logic).
- Plan change (P070) and "yearly, then monthly" (AGB 11.3) are noted locally and set shortly before the period end with
  ``PATCH /subscriptions/{id}`` + ``proration_billing_mode: do_not_bill`` (nothing billed or credited now; the next renewal bills
  the new price). The billing logic keeps the paid plan until the period end.
- Cancel: ``POST /subscriptions/{id}/cancel`` with ``effective_from: next_billing_period``. Portal: ``POST /customers/{id}/portal-sessions``.
- Coupons = Paddle discounts (looked up by code, never created here). Gift = one-off price per plan, quantity = months.
- Webhooks: ``Paddle-Signature: ts=<unix>;h1=<hex>[;h1=...]``, HMAC-SHA256 over ``<ts>:<raw body>`` with the notification
  destination's secret; stale timestamps refused; each event id once (billing logic).

Configuration ``billing.paddle``: ``environment`` (sandbox | production), ``api_key`` or ``key_file`` + ``key_var``
(``PADDLE_API_KEY``), ``webhook_secret``, ``client_token`` (public, for Paddle.js), ``prices`` {plan: {month, year, month_trial,
year_trial}}, ``gift_prices`` {plan: price id}, ``apply_before_hours`` (24), ``webhook_tolerance`` (300 s)."""
from __future__ import annotations

import hashlib
import hmac
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime

from ..config import PLAN_ALIAS
from ..errors import Problem
from . import base
from .base import Event, PaymentProvider

API = {"sandbox": "https://sandbox-api.paddle.com/", "production": "https://api.paddle.com/"}
TOLERANCE = 300
ENTITLED = ("active", "trialing")


def verify_signature(payload: bytes, header: str | None, secret: str, now: float | None = None, tolerance: int = TOLERANCE) -> bool:
    """Paddle's scheme: header ``ts=<unix>;h1=<hex>`` (several h1 while a secret is rotated); signed text ``<ts>:<raw body>``."""
    if not header or not secret or not isinstance(payload, (bytes, bytearray)):
        return False
    ts, sigs = None, []
    for part in header.split(";"):
        k, _, v = part.strip().partition("=")
        if k == "ts" and v.isdigit():
            ts = int(v)
        elif k == "h1" and v:
            sigs.append(v)
    if ts is None or not sigs:
        return False
    if abs((now if now is not None else time.time()) - ts) > tolerance:
        return False
    expected = hmac.new(secret.encode("utf-8"), str(ts).encode("ascii") + b":" + bytes(payload), hashlib.sha256).hexdigest()
    return any(hmac.compare_digest(expected, s) for s in sigs)


def sign(payload: bytes, secret: str, ts: int | None = None) -> str:
    """Header value as Paddle would send it (tests, local replays)."""
    ts = int(time.time()) if ts is None else ts
    sig = hmac.new(secret.encode("utf-8"), str(ts).encode("ascii") + b":" + payload, hashlib.sha256).hexdigest()
    return f"ts={ts};h1={sig}"


def parse_time(value) -> int | None:
    """RFC 3339 (``2026-11-19T10:00:00.123456Z``) -> unix seconds."""
    if not isinstance(value, str) or not value:
        return None
    v = re.sub(r"\.\d+", "", value.strip()).replace("Z", "+00:00")
    try:
        return int(datetime.fromisoformat(v).timestamp())
    except ValueError:
        return None


class PaddleError(Exception):
    def __init__(self, status: int, code: str):
        super().__init__(f"paddle {status} {code}")
        self.status, self.code = status, code


class PaddleHTTP:
    """Minimal Paddle Billing client (standard library, JSON). The key is only used in the Authorization header, never logged."""

    def __init__(self, key: str, environment: str = "sandbox", timeout: int = 20):
        self._key = key
        self.base = API.get(environment, API["sandbox"])
        self.timeout = timeout

    def __repr__(self):
        return f"PaddleHTTP({self.base}, key=***)"

    def call(self, method: str, path: str, data: dict | None = None) -> dict:
        url, body = self.base + path, None
        if method == "GET":
            q = urllib.parse.urlencode({k: (",".join(v) if isinstance(v, (list, tuple)) else v) for k, v in (data or {}).items()})
            url += ("?" + q) if q else ""
        elif data is not None:
            body = json.dumps(data).encode("utf-8")
        headers = {"Authorization": "Bearer " + self._key, "Content-Type": "application/json", "Accept": "application/json"}
        req = urllib.request.Request(url, data=body, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as r:
                return json.load(r)
        except urllib.error.HTTPError as ex:
            try:
                code = (json.load(ex).get("error") or {}).get("code") or "error"
            except (ValueError, AttributeError):
                code = "error"
            raise PaddleError(ex.code, str(code)[:60]) from None
        except (urllib.error.URLError, OSError, ValueError):
            raise PaddleError(0, "network") from None


def _int(v) -> int | None:
    if isinstance(v, int):
        return v
    return int(v) if isinstance(v, str) and v.isdigit() else None


def _cents(v) -> int:
    try:
        return int(v)
    except (TypeError, ValueError):
        return 0


class PaddleProvider(PaymentProvider):
    name = "paddle"
    customer_prefix = "ctm_"
    csp_forms = "https://customer-portal.paddle.com https://sandbox-customer-portal.paddle.com"
    csp_script = "https://cdn.paddle.com"
    csp_frame = "https://buy.paddle.com https://sandbox-buy.paddle.com"

    @property
    def p(self) -> dict:
        return self.svc.cfg.billing.get("paddle") or {}

    def configured(self) -> bool:
        return bool(self.svc.cfg.billing.get("enabled")) and bool(self.p.get("api_key")) and bool(self.p.get("webhook_secret"))

    @property
    def environment(self) -> str:
        return "production" if self.p.get("environment") == "production" else "sandbox"

    # ---------------------------------------------------------------- HTTP
    def api(self):
        svc = self.svc
        if svc.paddle_api is None:
            if not self.configured():
                raise Problem(503, "coming_soon")
            svc.paddle_api = PaddleHTTP(self.p["api_key"], self.environment)
        return svc.paddle_api

    def call(self, method: str, path: str, data: dict | None = None) -> dict:
        try:
            out = self.api().call(method, path, data)
        except PaddleError:
            raise Problem(502, "payment_error")
        return out.get("data") if isinstance(out, dict) and "data" in out else out

    # ---------------------------------------------------------------- prices
    def price_id(self, plan: str, interval: str) -> str | None:
        plan = PLAN_ALIAS.get(plan, plan)
        return ((self.p.get("prices") or {}).get(plan) or {}).get(interval)

    def plan_for_price(self, price: str | None) -> tuple[str | None, str | None]:
        for plan, ivs in (self.p.get("prices") or {}).items():
            for key, pid in (ivs or {}).items():
                if price and pid == price:
                    return PLAN_ALIAS.get(plan, plan), key.removesuffix("_trial")
        return None, None

    # ---------------------------------------------------------------- checkout
    def check_coupon(self, code: str):
        found = self.call("GET", "discounts", {"code": code, "status": "active"}) or []
        hit = next((d for d in found if isinstance(d, dict) and str(d.get("code") or "").lower() == code.lower()), None)
        if hit is None:
            raise Problem(400, "coupon_invalid")
        return hit["id"]

    def pay_url(self, txn: str) -> str:
        return self.svc.cfg.url("account", "/pro/pay?_ptxn=" + urllib.parse.quote(txn, safe=""))

    def _transaction(self, items: list, custom: dict, user, discount) -> tuple[str, str]:
        data = {"items": items, "custom_data": custom, "collection_mode": "automatic",
                "checkout": {"url": self.svc.cfg.url("account", "/pro/pay")}}
        cus = self.own_customer(user)
        if cus:
            data["customer_id"] = cus
        if discount:
            data["discount_id"] = discount
        txn = self.call("POST", "transactions", data) or {}
        if not txn.get("id"):
            raise Problem(502, "payment_error")
        return txn["id"], self.pay_url(txn["id"])

    def checkout_url(self, order_id: int, user, plan: str, interval: str, waiver: bool, lang: str, discount) -> tuple[str | None, str]:
        price = self.price_id(plan, interval)
        if not price:
            raise Problem(400, "bad_request")
        if not waiver:   # trial = withdrawal period: the first payment comes when the perks start (same rule as with Stripe)
            price = self.price_id(plan, f"{interval}_trial") or price
        custom = {"kind": "subscription", "order": str(order_id), "plan": plan, "interval": interval, "user": str(user["id"])}
        return self._transaction([{"price_id": price, "quantity": 1}], custom, user, discount)

    def gift_checkout_url(self, order_id: int, gift_id: int, user, plan: str, months: int, cents: int, lang: str, discount) -> tuple[str | None, str]:
        price = (self.p.get("gift_prices") or {}).get(PLAN_ALIAS.get(plan, plan))
        if not price:
            raise Problem(400, "bad_request")
        custom = {"kind": "gift", "order": str(order_id), "gift": str(gift_id), "plan": plan, "months": str(months), "user": str(user["id"])}
        return self._transaction([{"price_id": price, "quantity": int(months)}], custom, user, discount)

    # ---------------------------------------------------------------- running subscriptions
    def portal_url(self, user) -> str:
        cus = self.own_customer(user)
        if not cus:
            raise Problem(400, "bad_request")
        sub = self.svc.active_subscription(user["id"])
        data = {"subscription_ids": [sub["id"]]} if sub else {}
        out = self.call("POST", f"customers/{cus}/portal-sessions", data) or {}
        url = ((out.get("urls") or {}).get("general") or {}).get("overview")
        if not url:
            raise Problem(502, "payment_error")
        return url

    def cancel_at_period_end(self, sub) -> None:
        self.call("POST", f"subscriptions/{sub['id']}/cancel", {"effective_from": "next_billing_period"})

    def year_then_month(self, sub_id: str, plan: str) -> dict:
        """Noted locally; ``maintenance`` switches the item to the month price shortly before the year ends (do_not_bill)."""
        return {"pending_interval": "month"}

    def change_at_period_end(self, sub, current: str, plan: str) -> str | None:
        """Noted locally by the billing logic (pending_plan). Within the apply window (or when a change was already sent) the
        item is set right away - still without billing anything now."""
        if sub["pending_applied"] or (sub["period_end"] or 0) - self.svc.now() <= self.window():
            self.apply(sub, plan, sub["pending_interval"] if plan == current else None)
        return None

    def window(self) -> int:
        return int(self.p.get("apply_before_hours", 24)) * 3600

    def apply(self, sub, plan: str, pending_interval: str | None):
        """Sets the subscription item to the target price without billing (proration_billing_mode do_not_bill)."""
        current = PLAN_ALIAS.get(sub["plan"], sub["plan"])
        interval = "month" if plan != current else (pending_interval or sub["interval"] or "month")
        price = self.price_id(plan, interval)
        if not price:
            raise Problem(400, "bad_request")
        custom = {"kind": "subscription", "plan": plan, "interval": interval, "user": str(sub["user_id"] or "")}
        if sub["order_id"]:
            custom["order"] = str(sub["order_id"])
        self.call("PATCH", f"subscriptions/{sub['id']}", {"items": [{"price_id": price, "quantity": 1}],
                                                          "proration_billing_mode": "do_not_bill", "custom_data": custom})
        with self.svc.db.tx() as c:
            c.execute("UPDATE subscriptions SET pending_applied = 1 WHERE id = ?", (sub["id"],))

    def maintenance(self) -> None:
        if not self.configured():
            return
        svc = self.svc
        rows = svc.db.all("SELECT * FROM subscriptions WHERE customer LIKE 'ctm\\_%' ESCAPE '\\' AND status IN ('active', 'trialing') "
                          "AND cancel_at_period_end = 0 AND pending_applied = 0 AND (pending_plan IS NOT NULL OR pending_interval IS NOT NULL) "
                          "AND period_end IS NOT NULL AND period_end <= ?", svc.now() + self.window())
        for sub in rows:
            try:
                self.apply(sub, PLAN_ALIAS.get(sub["pending_plan"] or sub["plan"], sub["pending_plan"] or sub["plan"]), sub["pending_interval"])
            except Problem:
                pass      # next hour tries again; the paid plan stays until the period end anyway

    # ---------------------------------------------------------------- webhooks
    def parse_webhook(self, payload: bytes, headers: dict) -> tuple[str, str, int, list[Event]]:
        svc = self.svc
        tol = int(self.p.get("webhook_tolerance", TOLERANCE))
        if not verify_signature(payload, headers.get("paddle-signature"), self.p.get("webhook_secret") or "", now=svc.now(), tolerance=tol):
            raise Problem(400, "signature")
        try:
            event = json.loads(payload)
        except ValueError:
            raise Problem(400, "bad_request")
        if not isinstance(event, dict):
            raise Problem(400, "bad_request")
        eid, kind = str(event.get("event_id") or ""), str(event.get("event_type") or "")
        created = parse_time(event.get("occurred_at")) or svc.now()
        obj = event.get("data")
        if not eid or not isinstance(obj, dict):
            raise Problem(400, "bad_request")
        return "paddle:" + eid[:90], kind, created, self.events(kind, obj, created)

    def events(self, kind: str, obj: dict, created: int) -> list[Event]:
        custom = obj.get("custom_data") if isinstance(obj.get("custom_data"), dict) else {}
        hint = custom.get("user")
        order_id = _int(custom.get("order"))
        if kind.startswith("subscription."):
            items = obj.get("items") or []
            price = (items[0].get("price") or {}).get("id") if items and isinstance(items[0], dict) and isinstance(items[0].get("price"), dict) else None
            plan, interval = self.plan_for_price(price)
            if interval is None and items and isinstance(items[0].get("price"), dict):
                interval = ((items[0]["price"].get("billing_cycle") or {}).get("interval"))
            status = str(obj.get("status") or "")
            ev_kind = base.SUB_STARTED if kind == "subscription.created" else base.SUB_ENDED if (kind == "subscription.canceled" or status == "canceled") else base.SUB_CHANGED
            sched = obj.get("scheduled_change") or {}
            return [Event(ev_kind, created=created, user_hint=hint, customer=obj.get("customer_id") or "", order_id=order_id,
                          sub_id=obj.get("id") if isinstance(obj.get("id"), str) else None, plan=plan, meta_plan=custom.get("plan"),
                          interval=interval or custom.get("interval") or "month", status=status,
                          period_end=parse_time((obj.get("current_billing_period") or {}).get("ends_at")),
                          cancel_at_period_end=isinstance(sched, dict) and sched.get("action") == "cancel")]
        if kind in ("transaction.completed", "transaction.payment_failed"):
            sub_id = obj.get("subscription_id") if isinstance(obj.get("subscription_id"), str) else None
            totals = ((obj.get("details") or {}).get("totals") or {})
            common = dict(created=created, user_hint=hint, customer=obj.get("customer_id"), ref=obj.get("id"))
            if kind == "transaction.payment_failed":
                fails = [p for p in (obj.get("payments") or []) if isinstance(p, dict) and p.get("status") in ("error", "failed", "canceled")]
                return [Event(base.PAYMENT_FAILED, sub_id=sub_id, amount=_cents(totals.get("grand_total")), attempt=len(fails) or 1, **common)]
            out = []
            if custom.get("kind") == "gift":
                return [Event(base.GIFT_PAID, order_id=order_id, paid=True, **common)]
            if sub_id and order_id is not None:
                out.append(Event(base.CHECKOUT_COMPLETED, order_id=order_id, sub_id=sub_id, **common))
            if sub_id:
                out.append(Event(base.PAYMENT_SUCCEEDED, sub_id=sub_id, amount=_cents(totals.get("grand_total")), number=obj.get("invoice_number") or "",
                                 period_end=parse_time((obj.get("billing_period") or {}).get("ends_at")), **common))
            return out
        if kind in ("adjustment.created", "adjustment.updated"):
            if obj.get("status") != "approved" or obj.get("action") not in ("refund", "chargeback"):
                return []
            return [Event(base.REFUND, created=created, customer=obj.get("customer_id"), sub_id=obj.get("subscription_id"), ref=obj.get("id"),
                          action=obj.get("action"), amount=_cents((obj.get("totals") or {}).get("total")),
                          extra={"transaction": obj.get("transaction_id")})]
        return []
