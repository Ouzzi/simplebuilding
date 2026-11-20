"""Stripe as the second payment provider (N108; behind the PaymentProvider interface since addendum 3/4). Stripe Checkout for
payment data, customer portal, subscription schedules (yearly in advance then monthly; plan change at the period end), promotion
codes, signed webhooks (``Stripe-Signature``). Standard library only; the key is only used in the Authorization header.

Configuration: the top level of the billing file (``secret_key`` or ``key_file`` + ``key_var``, ``webhook_secret``, ``prices``,
``portal_configuration``, ``api_version``, ``vat.stripe_tax``) - unchanged from before the provider interface."""
from __future__ import annotations

import hashlib
import hmac
import json
import time
import urllib.error
import urllib.parse
import urllib.request

from .. import billing_mail
from ..config import PLAN_ALIAS
from ..errors import Problem
from . import base
from .base import Event, PaymentProvider

TOLERANCE = 300
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


def _int(v) -> int | None:
    return int(v) if isinstance(v, str) and v.isdigit() else None


class StripeProvider(PaymentProvider):
    name = "stripe"
    customer_prefix = "cus_"
    csp_forms = "https://checkout.stripe.com https://billing.stripe.com"

    def configured(self) -> bool:
        b = self.svc.cfg.billing
        return bool(b.get("enabled")) and bool(b.get("secret_key")) and bool(b.get("webhook_secret"))

    def own_customer(self, user) -> str | None:
        cus = user["billing_customer"] if user is not None else None
        return cus if cus and not cus.startswith("ctm_") else None    # pre-interface ids had no check; only Paddle ids are foreign

    # ---------------------------------------------------------------- HTTP
    def api(self):
        svc = self.svc
        if svc.stripe_api is None:
            if not self.configured():
                raise Problem(503, "coming_soon")
            svc.stripe_api = StripeHTTP(svc.bcfg("secret_key"), svc.bcfg("api_version"))
        return svc.stripe_api

    def call(self, method: str, path: str, data: dict | None = None, idem: str | None = None) -> dict:
        try:
            return self.api().call(method, path, data, idem)
        except StripeError:
            raise Problem(502, "payment_error")

    # ---------------------------------------------------------------- prices
    def price_id(self, plan: str, interval: str) -> str | None:
        prices = self.svc.bcfg("prices") or {}
        plan = PLAN_ALIAS.get(plan, plan)
        old = next((k for k, v in PLAN_ALIAS.items() if v == plan), None)    # billing files written before the rename
        return (prices.get(plan) or prices.get(old) or {}).get(interval)

    def plan_for_price(self, price: str | None) -> tuple[str | None, str | None]:
        for plan, ivs in (self.svc.bcfg("prices") or {}).items():
            for interval, pid in (ivs or {}).items():
                if price and pid == price:
                    return PLAN_ALIAS.get(plan, plan), interval
        return None, None

    # ---------------------------------------------------------------- checkout
    def check_coupon(self, code: str):
        found = (self.call("GET", "promotion_codes", {"code": code, "active": True, "limit": 1}).get("data") or [])
        if not found:
            raise Problem(400, "coupon_invalid")
        return [{"promotion_code": found[0]["id"]}]

    def _coupon(self, data: dict, discount):
        if discount:
            data["discounts"] = discount           # Stripe: discounts and allow_promotion_codes exclude each other
        elif self.svc.coupons_on():
            data["allow_promotion_codes"] = True

    def checkout_url(self, order_id: int, user, plan: str, interval: str, waiver: bool, lang: str, discount) -> tuple[str | None, str]:
        svc = self.svc
        price = self.price_id(plan, interval)
        if not price:
            raise Problem(400, "bad_request")
        meta = {"order": str(order_id), "plan": plan, "interval": interval, "user": str(user["id"])}
        acc = lambda path: svc.cfg.url("account", path)  # noqa: E731
        data = {"mode": "subscription", "line_items": [{"price": price, "quantity": 1}], "client_reference_id": str(user["id"]),
                "success_url": acc("/pro/done?session_id={CHECKOUT_SESSION_ID}"),
                "cancel_url": acc(f"/pro/order?plan={plan}&interval={interval}"),
                "locale": "de" if lang == "de" else "en", "metadata": meta, "subscription_data": {"metadata": meta},
                "custom_text": {"submit": {"message": billing_mail.submit_note(lang)}}}
        if not waiver:   # legal texts: without the early-start consent the first payment comes when the perks start (trial = withdrawal period)
            data["subscription_data"]["trial_period_days"] = int(svc.bcfg("withdrawal_days", 14))
        self._coupon(data, discount)
        cus = self.own_customer(user)
        if cus:
            data["customer"] = cus
        else:
            data["customer_email"] = user["email"]
        if svc.vat()["stripe_tax"]:
            data["automatic_tax"] = {"enabled": True}
            data["billing_address_collection"] = "required"
            data["tax_id_collection"] = {"enabled": True}
            if cus:
                data["customer_update"] = {"address": "auto", "name": "auto"}
        session = self.call("POST", "checkout/sessions", data, idem=f"order-{order_id}")
        return session.get("id"), session.get("url") or ""

    def gift_checkout_url(self, order_id: int, gift_id: int, user, plan: str, months: int, cents: int, lang: str, discount) -> tuple[str | None, str]:
        svc = self.svc
        p = svc.plan(plan)
        meta = {"kind": "gift", "order": str(order_id), "gift": str(gift_id), "plan": plan, "months": str(months), "user": str(user["id"])}
        acc = lambda path: svc.cfg.url("account", path)  # noqa: E731
        label = f"Simple Mods Pro {p['name']} – {'Geschenk' if lang == 'de' else 'gift'} {months} " + (("Monat" if months == 1 else "Monate") if lang == "de" else ("month" if months == 1 else "months"))
        data = {"mode": "payment", "client_reference_id": str(user["id"]), "locale": "de" if lang == "de" else "en", "metadata": meta,
                "line_items": [{"quantity": 1, "price_data": {"currency": "eur", "unit_amount": cents, "product_data": {"name": label},
                                                              "tax_behavior": "inclusive"}}],
                "payment_intent_data": {"metadata": meta, "statement_descriptor_suffix": "GESCHENK"},
                "success_url": acc("/pro/gifts?done=1"), "cancel_url": acc(f"/pro/gift?plan={plan}"),
                "custom_text": {"submit": {"message": billing_mail.submit_note(lang)}}}
        cus = self.own_customer(user)
        if cus:
            data["customer"] = cus
        else:
            data["customer_email"] = user["email"]
            data["customer_creation"] = "always"
        if svc.vat()["stripe_tax"]:
            data["automatic_tax"] = {"enabled": True}
            data["billing_address_collection"] = "required"
        self._coupon(data, discount)
        session = self.call("POST", "checkout/sessions", data, idem=f"order-{order_id}")
        return session.get("id"), session.get("url") or ""

    # ---------------------------------------------------------------- running subscriptions
    def portal_url(self, user) -> str:
        cus = self.own_customer(user)
        if not cus:
            raise Problem(400, "bad_request")
        data = {"customer": cus, "return_url": self.svc.cfg.url("account", "/pro/manage")}
        if self.svc.bcfg("portal_configuration"):
            data["configuration"] = self.svc.bcfg("portal_configuration")
        return self.call("POST", "billing_portal/sessions", data)["url"]

    def cancel_at_period_end(self, sub) -> None:
        if sub["schedule_id"]:   # yearly plan: a subscription under a schedule cannot be cancelled directly
            self.call("POST", f"subscription_schedules/{sub['schedule_id']}/release", {}, idem=f"release-{sub['id']}")
        self.call("POST", f"subscriptions/{sub['id']}", {"cancel_at_period_end": True}, idem=f"cancel-{sub['id']}-{self.svc.now() // 3600}")

    def year_then_month(self, sub_id: str, plan: str) -> str | None:
        """Schedule from the subscription, phase 1 = current year price for one iteration, phase 2 = month price, release afterwards."""
        month, year = self.price_id(plan, "month"), self.price_id(plan, "year")
        sched = self.call("POST", "subscription_schedules", {"from_subscription": sub_id}, idem=f"sched-{sub_id}")
        phase = (sched.get("phases") or [{}])[0]
        self.call("POST", f"subscription_schedules/{sched['id']}", {
            "end_behavior": "release", "proration_behavior": "none",
            "metadata": {"plan": plan, "kind": "year_then_month"},
            "phases": [{"items": [{"price": year, "quantity": 1}], "start_date": phase.get("start_date"), "end_date": phase.get("end_date")},
                       {"items": [{"price": month, "quantity": 1}], "iterations": 1}]}, idem=f"sched-phases-{sub_id}")
        return sched["id"]

    def change_at_period_end(self, sub, current: str, plan: str) -> str | None:
        """Schedule: current phase until the period end, then the new plan's monthly price (no proration)."""
        new_price = self.price_id(plan, "month")
        cur_price = self.price_id(current, sub["interval"] if sub["interval"] in ("month", "year") else "month")
        if not new_price or not cur_price:
            raise Problem(400, "bad_request")
        sched_id = sub["schedule_id"]
        if sched_id:
            sched = self.call("GET", f"subscription_schedules/{sched_id}")
        else:
            sched = self.call("POST", "subscription_schedules", {"from_subscription": sub["id"]}, idem=f"sched-change-{sub['id']}-{sub['period_end']}")
            sched_id = sched["id"]
        phase = (sched.get("phases") or [{}])[0]
        self.call("POST", f"subscription_schedules/{sched_id}", {
            "end_behavior": "release", "proration_behavior": "none",
            "metadata": {"plan": current, "next": plan, "kind": "change_at_period_end"},
            "phases": [{"items": [{"price": cur_price, "quantity": 1}], "start_date": phase.get("start_date"), "end_date": sub["period_end"] or phase.get("end_date"),
                        "proration_behavior": "none"},
                       {"items": [{"price": new_price, "quantity": 1}], "iterations": 1, "proration_behavior": "none",
                        "metadata": {"plan": plan}}]}, idem=f"change-{sub['id']}-{plan}-{sub['period_end']}")
        return sched_id

    # ---------------------------------------------------------------- webhooks
    def parse_webhook(self, payload: bytes, headers: dict) -> tuple[str, str, int, list[Event]]:
        svc = self.svc
        if not verify_signature(payload, headers.get("stripe-signature"), svc.bcfg("webhook_secret"), now=svc.now()):
            raise Problem(400, "signature")
        try:
            event = json.loads(payload)
        except ValueError:
            raise Problem(400, "bad_request")
        if not isinstance(event, dict):
            raise Problem(400, "bad_request")
        eid, kind = str(event.get("id") or ""), str(event.get("type") or "")
        created = event.get("created") if isinstance(event.get("created"), int) else svc.now()
        obj = (event.get("data") or {}).get("object") or {}
        if not eid or not isinstance(obj, dict):
            raise Problem(400, "bad_request")
        return eid, kind, created, self.events(kind, obj, created)

    def events(self, kind: str, obj: dict, created: int) -> list[Event]:
        meta = obj.get("metadata") or {}
        hint = meta.get("user") or obj.get("client_reference_id")
        if kind == "checkout.session.completed":
            common = dict(created=created, user_hint=hint, customer=obj.get("customer"), order_id=_int(str(meta.get("order", ""))), ref=obj.get("id"))
            if meta.get("kind") == "gift" or obj.get("mode") == "payment":
                return [Event(base.GIFT_PAID, paid=obj.get("payment_status") in (None, "paid", "no_payment_required"), **common)]
            if obj.get("mode") not in (None, "subscription"):
                return []
            return [Event(base.CHECKOUT_COMPLETED, sub_id=obj.get("subscription") if isinstance(obj.get("subscription"), str) else None, **common)]
        if kind.startswith("customer.subscription."):
            items = ((obj.get("items") or {}).get("data") or [])
            price = ((items[0].get("price") or {}).get("id") if items and isinstance(items[0].get("price"), dict) else None)
            plan, interval = self.plan_for_price(price)
            ev_kind = base.SUB_STARTED if kind.endswith(".created") else base.SUB_ENDED if kind.endswith(".deleted") else base.SUB_CHANGED
            period_end = obj.get("current_period_end") or (items[0].get("current_period_end") if items else None)
            return [Event(ev_kind, created=created, user_hint=hint, customer=obj.get("customer") or "", order_id=_int(str(meta.get("order", ""))),
                          sub_id=obj.get("id") if isinstance(obj.get("id"), str) else None, plan=plan, meta_plan=meta.get("plan"), meta_tier=meta.get("tier"),
                          interval=interval or meta.get("interval") or "month",
                          status="canceled" if kind == "customer.subscription.deleted" else str(obj.get("status") or ""),
                          period_end=period_end if isinstance(period_end, int) else None, cancel_at_period_end=bool(obj.get("cancel_at_period_end")))]
        if kind in ("invoice.paid", "invoice.payment_failed"):
            smeta = ((obj.get("subscription_details") or {}).get("metadata") or {})
            ends = [(ln.get("period") or {}).get("end") or 0 for ln in ((obj.get("lines") or {}).get("data") or []) if isinstance(ln, dict)]
            paid = kind == "invoice.paid"
            return [Event(base.PAYMENT_SUCCEEDED if paid else base.PAYMENT_FAILED, created=created, user_hint=smeta.get("user"),
                          customer=obj.get("customer"), sub_id=obj.get("subscription") if isinstance(obj.get("subscription"), str) else None,
                          ref=str(obj.get("id") or ""), period_end=max(ends or [0]) or None,
                          amount=(obj.get("amount_paid") or 0) if paid else (obj.get("amount_due") or 0), number=obj.get("number") or "",
                          link=obj.get("hosted_invoice_url") or "", pdf=obj.get("invoice_pdf") or "", attempt=obj.get("attempt_count") or 0)]
        return []
