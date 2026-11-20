"""In-memory stand-in for the Paddle Billing API v2 (tests and the local picture harness; never talks to the network) plus webhook
builders in the shape Paddle's notifications have: ``event_id``, ``event_type``, ``occurred_at`` (RFC 3339), ``notification_id``,
``data`` (the entity). Answers are wrapped like Paddle's: ``{"data": ..., "meta": {"request_id": ...}}``."""
from __future__ import annotations

import copy
import itertools
import json
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from smweb.payments.paddle import PaddleError, sign  # noqa: E402


def iso(ts: int | float) -> str:
    return time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(int(ts))) + ".000000Z"


class FakePaddle:
    def __init__(self, now=lambda: 1_800_000_000, pay_base: str | None = None):
        self.calls: list[tuple[str, str, dict]] = []
        self.objects: dict[str, dict] = {}
        self.n = itertools.count(1)
        self.now = now
        self.fail: set[str] = set()
        self.pay_base = pay_base

    def new_id(self, prefix: str) -> str:
        return f"{prefix}_01fake{next(self.n):020d}"

    def call(self, method: str, path: str, data: dict | None = None) -> dict:
        data = copy.deepcopy(data or {})
        json.dumps(data)      # JSON-encodable like the real client
        self.calls.append((method, path, data))
        if path in self.fail or (path.split("/")[0] + "/*") in self.fail:
            raise PaddleError(500, "internal_error")
        return {"data": self.route(method, path, data), "meta": {"request_id": "req_fake"}}

    def by(self, kind: str) -> list[dict]:
        return [o for o in self.objects.values() if o.get("_kind") == kind]

    def sent(self, method: str, prefix: str) -> list[tuple[str, dict]]:
        return [(p, d) for m, p, d in self.calls if m == method and p.startswith(prefix)]

    @staticmethod
    def public(o: dict) -> dict:
        return {k: v for k, v in o.items() if not k.startswith("_")}

    def route(self, method: str, path: str, d: dict):
        parts = path.split("/")
        if parts[0] == "transactions" and method == "POST" and len(parts) == 1:
            tid = self.new_id("txn")
            base = self.pay_base or (d.get("checkout") or {}).get("url") or "https://pay.example.test/pay"
            obj = {"_kind": "transaction", "id": tid, "status": "ready", "items": d.get("items"), "custom_data": d.get("custom_data"),
                   "customer_id": d.get("customer_id"), "discount_id": d.get("discount_id"), "collection_mode": d.get("collection_mode"),
                   "checkout": {"url": f"{base}?_ptxn={tid}"}}
            self.objects[tid] = obj
            return self.public(obj)
        if parts[0] == "discounts" and method == "GET":
            return [self.public(o) for o in self.by("discount") if o.get("code") == d.get("code") and o.get("status") == d.get("status", "active")]
        if parts[0] == "customers" and len(parts) == 3 and parts[2] == "portal-sessions" and method == "POST":
            return {"id": self.new_id("cpls"), "customer_id": parts[1],
                    "urls": {"general": {"overview": f"https://sandbox-customer-portal.paddle.com/cpl_fake?token={next(self.n)}"},
                             "subscriptions": [{"id": s, "cancel_subscription": "https://x", "update_subscription_payment_method": "https://y"}
                                               for s in d.get("subscription_ids") or []]}}
        if parts[0] == "subscriptions" and len(parts) == 3 and parts[2] == "cancel" and method == "POST":
            sub = self.objects.setdefault(parts[1], {"_kind": "subscription", "id": parts[1]})
            sub["scheduled_change"] = {"action": "cancel", "effective_at": iso(self.now() + 30 * 86400)}
            return self.public(sub)
        if parts[0] == "subscriptions" and len(parts) == 2 and method == "PATCH":
            sub = self.objects.setdefault(parts[1], {"_kind": "subscription", "id": parts[1]})
            sub.update(d)
            return self.public(sub)
        # paddle_setup.py
        if parts[0] in ("products", "prices", "notification-settings"):
            kind = {"products": "product", "prices": "price", "notification-settings": "notification_setting"}[parts[0]]
            prefix = {"product": "pro", "price": "pri", "notification_setting": "ntfset"}[kind]
            if method == "GET" and len(parts) == 1:
                out = self.by(kind)
                if d.get("status"):
                    out = [o for o in out if o.get("status", "active") == d["status"]]
                if d.get("product_id"):
                    out = [o for o in out if o.get("product_id") == d["product_id"]]
                return [{k: v for k, v in self.public(o).items() if k != "endpoint_secret_key"} for o in out]
            if method == "POST" and len(parts) == 1:
                obj = {"_kind": kind, "id": self.new_id(prefix), "status": "active", **d}
                if kind == "notification_setting":
                    obj["endpoint_secret_key"] = "pdl_ntfset_" + "f" * 32
                    obj["active"] = True
                self.objects[obj["id"]] = obj
                return self.public(obj)
            if method == "PATCH" and len(parts) == 2:
                if parts[1] not in self.objects:
                    raise PaddleError(404, "not_found")
                self.objects[parts[1]].update(d)
                return {k: v for k, v in self.public(self.objects[parts[1]]).items() if k != "endpoint_secret_key"}
        raise PaddleError(404, "unknown_route")


# -------------------------------------------------------------------- Paddle-like notifications
_evt = itertools.count(1)


def event(kind: str, data: dict, occurred: int, eid: str | None = None) -> dict:
    n = next(_evt)
    return {"event_id": eid or f"evt_01fake{n:020d}", "event_type": kind, "occurred_at": iso(occurred),
            "notification_id": f"ntf_01fake{n:020d}", "data": data}


def subscription(sid: str, customer: str, price: str, status: str = "active", period_end: int = 0, custom: dict | None = None,
                 interval: str = "month", cancel: bool = False, start: int | None = None) -> dict:
    start = start if start is not None else period_end - (365 if interval == "year" else 30) * 86400
    return {"id": sid, "status": status, "customer_id": customer, "address_id": "add_01fake", "currency_code": "EUR",
            "collection_mode": "automatic", "custom_data": custom or {}, "billing_cycle": {"interval": interval, "frequency": 1},
            "current_billing_period": {"starts_at": iso(start), "ends_at": iso(period_end)} if period_end else None,
            "next_billed_at": iso(period_end) if period_end and not cancel else None,
            "scheduled_change": {"action": "cancel", "effective_at": iso(period_end), "resume_at": None} if cancel else None,
            "items": [{"status": "active", "quantity": 1, "recurring": True,
                       "price": {"id": price, "product_id": "pro_01fake", "billing_cycle": {"interval": interval, "frequency": 1},
                                 "unit_price": {"amount": "399", "currency_code": "EUR"}}}]}


def transaction(tid: str, customer: str, sub: str | None, custom: dict | None, amount: int, period_end: int | None = None,
                status: str = "completed", origin: str = "web", failed: int = 0) -> dict:
    return {"id": tid, "status": status, "customer_id": customer, "subscription_id": sub, "origin": origin, "currency_code": "EUR",
            "custom_data": custom or {}, "invoice_number": None if status != "completed" else "325-" + tid[-5:],
            "billing_period": {"starts_at": iso((period_end or 0) - 30 * 86400), "ends_at": iso(period_end)} if period_end else None,
            "details": {"totals": {"subtotal": str(amount), "tax": "0", "total": str(amount), "grand_total": str(amount)}},
            "payments": [{"status": "error", "error_code": "declined"} for _ in range(failed)]}


def adjustment(aid: str, txn: str, customer: str, sub: str | None = None, action: str = "refund", status: str = "approved", amount: int = 0) -> dict:
    return {"id": aid, "action": action, "transaction_id": txn, "subscription_id": sub, "customer_id": customer, "reason": "test",
            "status": status, "totals": {"subtotal": str(amount), "tax": "0", "total": str(amount)}}


def signed(evt: dict, secret: str, ts: int) -> tuple[bytes, str]:
    body = json.dumps(evt, separators=(",", ":")).encode()
    return body, sign(body, secret, ts)
