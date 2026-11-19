"""In-memory stand-in for the Stripe API (tests and the local picture harness; never talks to the network) plus event builders
in the shape the Stripe CLI (``stripe trigger`` / ``stripe listen``) delivers: ``id``, ``object: "event"``, ``api_version``,
``created``, ``livemode: false``, ``pending_webhooks``, ``request``, ``type``, ``data.object``."""
from __future__ import annotations

import itertools
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from smweb.billing import StripeError, flatten, sign  # noqa: E402

API_VERSION = "2025-01-27.acacia"


class FakeStripe:
    def __init__(self, checkout_base: str = "https://checkout.stripe.com/c/pay/", now=lambda: 1_800_000_000):
        self.calls: list[tuple[str, str, dict, str | None]] = []
        self.idem: dict[str, dict] = {}
        self.objects: dict[str, dict] = {}      # id -> object (products, prices, endpoints, sessions, schedules, subscriptions)
        self.n = itertools.count(1)
        self.checkout_base = checkout_base
        self.now = now
        self.fail: set[str] = set()             # paths that answer with an error (tests)

    def new_id(self, prefix: str) -> str:
        return f"{prefix}_test_{next(self.n):04d}"

    def call(self, method: str, path: str, data: dict | None = None, idem: str | None = None) -> dict:
        data = data or {}
        flatten(data)    # same encoding as the real client; raises on unsupported values
        self.calls.append((method, path, data, idem))
        if path in self.fail:
            raise StripeError(500, "api_error")
        if idem and idem in self.idem:
            return self.idem[idem]
        out = self.route(method, path, data)
        if idem:
            self.idem[idem] = out
        return out

    def posts(self, prefix: str) -> list[tuple[str, dict]]:
        return [(p, d) for m, p, d, _ in self.calls if m == "POST" and p.startswith(prefix)]

    # ---------------------------------------------------------------- routes
    def route(self, method: str, path: str, d: dict) -> dict:
        parts = path.split("/")
        if method == "POST" and path == "checkout/sessions":
            sid = self.new_id("cs")
            obj = {"id": sid, "object": "checkout.session", "url": self.checkout_base + sid, "mode": d.get("mode"), "params": d}
            self.objects[sid] = obj
            return obj
        if method == "POST" and path == "billing_portal/sessions":
            return {"id": self.new_id("bps"), "url": "https://billing.stripe.com/p/session/" + self.new_id("x")}
        if method == "POST" and path == "subscription_schedules":
            sid = self.new_id("sub_sched")
            start = self.now()
            obj = {"id": sid, "object": "subscription_schedule", "subscription": d.get("from_subscription"),
                   "phases": [{"start_date": start, "end_date": start + 365 * 86400}], "end_behavior": "release"}
            self.objects[sid] = obj
            return obj
        if method == "POST" and parts[0] == "subscription_schedules" and len(parts) == 3 and parts[2] == "release":
            return {**self.objects.get(parts[1], {"id": parts[1]}), "status": "released"}
        if method == "POST" and parts[0] == "subscription_schedules" and len(parts) == 2:
            obj = self.objects.setdefault(parts[1], {"id": parts[1]})
            obj.update(d)
            return obj
        if method == "POST" and parts[0] == "subscriptions" and len(parts) == 2:
            obj = self.objects.setdefault(parts[1], {"id": parts[1], "object": "subscription"})
            obj.update(d)
            return obj
        # stripe_setup.py
        if parts[0] == "products":
            if method == "GET" and len(parts) == 2:
                if parts[1] not in self.objects:
                    raise StripeError(404, "resource_missing")
                return self.objects[parts[1]]
            if method == "POST" and len(parts) == 1:
                obj = {"object": "product", "active": True, **d}
                obj.setdefault("id", self.new_id("prod"))
                self.objects[obj["id"]] = obj
                return obj
            if method == "POST" and len(parts) == 2:
                self.objects[parts[1]].update(d)
                return self.objects[parts[1]]
        if parts[0] == "prices":
            if method == "GET":
                keys = [v for k, v in d.items() if k.startswith("lookup_keys")]
                keys = keys[0] if keys and isinstance(keys[0], list) else keys
                found = [o for o in self.objects.values() if o.get("object") == "price" and o.get("lookup_key") in keys and o.get("active", True)]
                return {"object": "list", "data": found}
            if method == "POST" and len(parts) == 1:
                if d.get("transfer_lookup_key"):
                    for o in self.objects.values():
                        if o.get("object") == "price" and o.get("lookup_key") == d.get("lookup_key"):
                            o["lookup_key"] = None
                obj = {"id": self.new_id("price"), "object": "price", "active": True, **{k: v for k, v in d.items() if k != "transfer_lookup_key"}}
                self.objects[obj["id"]] = obj
                return obj
            if method == "POST" and len(parts) == 2:
                self.objects[parts[1]].update(d)
                return self.objects[parts[1]]
        if parts[0] == "webhook_endpoints":
            if method == "GET":
                return {"object": "list", "data": [{k: v for k, v in o.items() if k != "secret"} for o in self.objects.values() if o.get("object") == "webhook_endpoint"]}
            if method == "POST" and len(parts) == 1:
                obj = {"id": self.new_id("we"), "object": "webhook_endpoint", **d, "secret": "whsec_" + "f" * 32}
                self.objects[obj["id"]] = obj
                return obj
            if method == "POST" and len(parts) == 2:
                self.objects[parts[1]].update(d)
                return {k: v for k, v in self.objects[parts[1]].items() if k != "secret"}
        if parts[0] == "billing_portal" and parts[1] == "configurations":
            if method == "GET":
                return {"object": "list", "data": [o for o in self.objects.values() if o.get("object") == "billing_portal.configuration"]}
            if method == "POST" and len(parts) == 2:
                obj = {"id": self.new_id("bpc"), "object": "billing_portal.configuration", **d}
                self.objects[obj["id"]] = obj
                return obj
            if method == "POST" and len(parts) == 3:
                self.objects[parts[2]].update(d)
                return self.objects[parts[2]]
        raise StripeError(404, "unknown_route")


# -------------------------------------------------------------------- Stripe-CLI-like events
_evt = itertools.count(1)


def event(kind: str, obj: dict, created: int, eid: str | None = None) -> dict:
    return {"id": eid or f"evt_test_{next(_evt):06d}", "object": "event", "api_version": API_VERSION, "created": created,
            "livemode": False, "pending_webhooks": 1, "request": {"id": f"req_test_{next(_evt):06d}", "idempotency_key": None},
            "type": kind, "data": {"object": obj}}


def checkout_session(session: dict, customer: str, sub: str) -> dict:
    p = session["params"]
    return {"id": session["id"], "object": "checkout.session", "mode": "subscription", "status": "complete", "payment_status": "paid",
            "client_reference_id": p.get("client_reference_id"), "customer": customer, "subscription": sub,
            "customer_details": {"email": p.get("customer_email")}, "metadata": p.get("metadata", {}), "livemode": False}


def subscription(sid: str, customer: str, price: str, status: str = "active", period_end: int = 0, meta: dict | None = None,
                 cancel_at_period_end: bool = False, interval: str = "month") -> dict:
    return {"id": sid, "object": "subscription", "customer": customer, "status": status, "cancel_at_period_end": cancel_at_period_end,
            "current_period_end": period_end, "metadata": meta or {}, "livemode": False,
            "items": {"object": "list", "data": [{"id": "si_" + sid, "object": "subscription_item", "current_period_end": period_end,
                                                    "price": {"id": price, "object": "price", "recurring": {"interval": interval}}}]}}


def invoice(iid: str, customer: str, sub: str, amount: int, period_end: int, paid: bool = True, attempt: int = 1) -> dict:
    return {"id": iid, "object": "invoice", "customer": customer, "subscription": sub, "number": "SM-" + iid[-4:],
            "amount_paid": amount if paid else 0, "amount_due": amount, "attempt_count": attempt, "status": "paid" if paid else "open",
            "hosted_invoice_url": f"https://invoice.stripe.com/i/{iid}", "invoice_pdf": f"https://pay.stripe.com/invoice/{iid}/pdf",
            "lines": {"object": "list", "data": [{"period": {"start": period_end - 30 * 86400, "end": period_end}}]}}


def signed(evt: dict, secret: str, ts: int) -> tuple[bytes, str]:
    body = json.dumps(evt, separators=(",", ":")).encode()
    return body, sign(body, secret, ts)
