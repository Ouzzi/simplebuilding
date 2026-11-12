"""Stripe, prepared and switched OFF (spec section 6): products/prices per tier and interval come from ``billing.json``;
checkout and the customer portal answer "coming soon" while ``enabled`` is false; the webhook receiver checks the
``Stripe-Signature`` header (HMAC-SHA256 of ``<t>.<body>``, 5 minutes tolerance) before it looks at anything.

Switching on (owner, after legal texts + Stripe account): fill the real price ids and keys in the server config's
billing file (outside the repo), set ``enabled: true``, restart. The HTTP calls to Stripe use the standard library only.

Yearly plan (owner 12.11.2026, LIVEMAP34b Q1 = A, AGB draft 11.3): 12 months paid in advance, then it runs on MONTHLY at the
monthly price, cancellable to the end of every paid month. With Stripe that is a subscription schedule (phase 1: the "year"
price for one iteration, phase 2: the "month" price, ``end_behavior: release``) - not a plain yearly subscription that renews
for another year. To build with the payment start (TODO a-livemap34c in the hand-over).
"""
from __future__ import annotations

import hashlib
import hmac
import json
import time
import urllib.parse
import urllib.request

from .errors import Problem

TOLERANCE = 300


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


class BillingMixin:
    def billing_on(self) -> bool:
        b = self.cfg.billing
        return bool(b.get("enabled")) and bool(b.get("secret_key")) and bool(b.get("webhook_secret"))

    def price_id(self, tier: str, interval: str) -> str | None:
        return ((self.cfg.billing.get("prices") or {}).get(tier) or {}).get(interval)

    def checkout(self, user, tier: str, interval: str) -> str:
        """URL of a Stripe Checkout session (subscription), or Problem 'coming_soon' while billing is off."""
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        price = self.price_id(tier, interval)
        if tier not in self.cfg.tiers["tiers"] or tier == "free" or interval not in ("month", "year") or not price:
            raise Problem(400, "bad_request")
        data = {"mode": "subscription", "line_items[0][price]": price, "line_items[0][quantity]": "1",
                "client_reference_id": str(user["id"]), "customer_email": user["email"],
                "success_url": self.cfg.url("livemap", "/app/plan?paid=1"), "cancel_url": self.cfg.url("livemap", "/app/plan"),
                "metadata[tier]": tier, "subscription_data[metadata][tier]": tier, "subscription_data[metadata][user]": str(user["id"])}
        if user["billing_customer"]:
            data.pop("customer_email")
            data["customer"] = user["billing_customer"]
        return self.stripe("checkout/sessions", data)["url"]

    def portal(self, user) -> str:
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        if not user["billing_customer"]:
            raise Problem(400, "bad_request")
        return self.stripe("billing_portal/sessions", {"customer": user["billing_customer"],
                                                       "return_url": self.cfg.url("livemap", "/app/plan")})["url"]

    def stripe(self, path: str, data: dict) -> dict:
        req = urllib.request.Request("https://api.stripe.com/v1/" + path, data=urllib.parse.urlencode(data).encode("ascii"),
                                     headers={"Authorization": "Bearer " + self.cfg.billing["secret_key"]}, method="POST")
        with urllib.request.urlopen(req, timeout=15) as r:
            return json.load(r)

    def webhook(self, payload: bytes, header: str | None) -> dict:
        """Verified events only. Subscription created/updated -> tier from its metadata while active; deleted/unpaid -> free
        (downgrade rules of maps.set_tier apply)."""
        if not self.billing_on():
            raise Problem(503, "coming_soon")
        if not verify_signature(payload, header, self.cfg.billing["webhook_secret"], now=self.now()):
            raise Problem(400, "signature")
        try:
            event = json.loads(payload)
        except ValueError:
            raise Problem(400, "bad_request")
        kind = event.get("type", "")
        obj = (event.get("data") or {}).get("object") or {}
        if kind == "checkout.session.completed":
            uid = obj.get("client_reference_id")
            user = self.user(int(uid)) if isinstance(uid, str) and uid.isdigit() else None
            if user and obj.get("customer"):
                with self.db.tx() as c:
                    c.execute("UPDATE users SET billing_customer = ? WHERE id = ?", (obj["customer"], user["id"]))
        elif kind in ("customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted"):
            user = self.db.one("SELECT * FROM users WHERE billing_customer = ?", obj.get("customer") or "")
            meta = obj.get("metadata") or {}
            if user:
                active = kind != "customer.subscription.deleted" and obj.get("status") in ("active", "trialing", "past_due")
                tier = meta.get("tier") if active and meta.get("tier") in self.cfg.tiers["tiers"] else "free"
                self.set_tier(user, tier)
                with self.db.tx() as c:
                    c.execute("UPDATE users SET billing_until = ? WHERE id = ?", (obj.get("current_period_end"), user["id"]))
        return {"received": True}
