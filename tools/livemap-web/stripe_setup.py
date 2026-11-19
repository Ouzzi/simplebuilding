#!/usr/bin/env python3
"""Creates the Stripe objects of Simple Mods Pro (N108) idempotently: one product per plan, a monthly and a yearly price per
plan (lookup keys ``smpro_<plan>_<month|year>``), the webhook endpoint and a customer-portal configuration; writes the price ids,
the portal configuration id and - only when the endpoint is new - the webhook signing secret into the billing file (outside the
repo, mode 600).

Safe by default:
- DRY RUN unless ``--apply``: no network call, shows the plan.
- The secret key is read at runtime from an env file (default ``/root/.config/sb-keys.env``, variable ``STRIPE_SECRET_KEY``);
  it is never printed, logged or written anywhere (the service reads it the same way: billing ``key_file`` + ``key_var``).
- Test mode first: a live key (``sk_live_``/``rk_live_``) is refused without ``--live``.
- Coupons (P074): creates NO coupons or promotion codes; the owner makes them later in the dashboard.
- Run it again any time: existing objects are found (product ids, price lookup keys, endpoint URL, portal metadata) and reused;
  a changed amount makes a new price and moves the lookup key (prices are immutable at Stripe), the old one is archived.

    python3 tools/livemap-web/stripe_setup.py --webhook-url https://livemap.simplemods.de/api/stripe/webhook \\
        --billing-file /etc/simplemods/livemap-billing.json [--apply] [--live]
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from smweb.billing import StripeError, StripeHTTP  # noqa: E402
from smweb.config import load_plans, load_tiers  # noqa: E402

EVENTS = ["checkout.session.completed", "customer.subscription.created", "customer.subscription.updated",
          "customer.subscription.deleted", "customer.subscription.paused", "customer.subscription.resumed",
          "invoice.paid", "invoice.payment_failed"]
PORTAL_KEY = "smpro"


def read_env_var(path: Path, var: str) -> str | None:
    """Value of ``var`` from a shell-style env file (``[export ]VAR=value``, optional quotes); the file is parsed, never run."""
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError:
        return None
    for line in lines:
        line = line.strip()
        if line.startswith("export "):
            line = line[7:].lstrip()
        name, sep, value = line.partition("=")
        if sep and name.strip() == var:
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in "'\"":
                value = value[1:-1]
            return value or None
    return None


def key_mode(key: str) -> str:
    return "live" if key.startswith(("sk_live_", "rk_live_")) else "test" if key.startswith(("sk_test_", "rk_test_")) else "unknown"


def wanted(plans: dict, currency: str = "eur") -> list[dict]:
    """Products and prices the catalogue asks for (amounts in cents from plans.json/tiers.json)."""
    out = []
    for key in plans["order"]:
        p = plans["plans"][key]
        out.append({"plan": key, "product": f"smpro_{key}", "name": f"Simple Mods Pro {p['name']}", "livemap_tier": p.get("livemap_tier") or "",
                    "prices": {"month": {"lookup_key": f"smpro_{key}_month", "amount": int(p["price_month"]), "interval": "month", "currency": currency},
                               "year": {"lookup_key": f"smpro_{key}_year", "amount": int(p["price_year"]), "interval": "year", "currency": currency}}})
    return out


class Setup:
    def __init__(self, api, plans: dict, webhook_url: str, return_url: str, legal: dict, api_version: str | None, out=print,
                 descriptor: str = "SIMPLEMODS"):
        self.api, self.plans, self.webhook_url, self.return_url, self.legal = api, plans, webhook_url, return_url, legal
        self.api_version = api_version
        self.descriptor = descriptor     # owner 19.11.: bank statement shows "SIMPLEMODS" (product-level descriptor for subscriptions)
        self.out = out
        self.created = 0

    def get(self, path, data=None):
        return self.api.call("GET", path, data or {})

    def post(self, path, data, idem=None):
        return self.api.call("POST", path, data, idem)

    def product(self, w: dict) -> str:
        try:
            prod = self.get(f"products/{w['product']}")
            if prod.get("name") != w["name"] or not prod.get("active", True) or prod.get("statement_descriptor") != self.descriptor:
                self.post(f"products/{w['product']}", {"name": w["name"], "active": True, "statement_descriptor": self.descriptor})
                self.out(f"  product {w['product']}: updated")
            else:
                self.out(f"  product {w['product']}: exists")
        except StripeError as ex:
            if ex.status != 404:
                raise
            self.post("products", {"id": w["product"], "name": w["name"], "statement_descriptor": self.descriptor, "metadata": {"sm_key": w["product"], "livemap_tier": w["livemap_tier"]}},
                      idem=f"smpro-product-{w['product']}")
            self.created += 1
            self.out(f"  product {w['product']}: created")
        return w["product"]

    def price(self, product: str, want: dict) -> str:
        found = self.get("prices", {"lookup_keys": [want["lookup_key"]], "active": True}).get("data") or []
        cur = found[0] if found else None
        if cur and cur.get("unit_amount") == want["amount"] and (cur.get("recurring") or {}).get("interval") == want["interval"] \
                and cur.get("currency", want["currency"]) == want["currency"] and cur.get("product") in (None, product):
            self.out(f"  price {want['lookup_key']}: exists ({want['amount']} ct/{want['interval']})")
            return cur["id"]
        new = self.post("prices", {"product": product, "currency": want["currency"], "unit_amount": want["amount"],
                                   "recurring": {"interval": want["interval"]}, "lookup_key": want["lookup_key"], "transfer_lookup_key": True,
                                   "tax_behavior": "inclusive", "metadata": {"sm_key": want["lookup_key"]}},
                        idem=f"smpro-price-{want['lookup_key']}-{want['amount']}")
        self.created += 1
        if cur:
            self.post(f"prices/{cur['id']}", {"active": False})
            self.out(f"  price {want['lookup_key']}: new amount {want['amount']} ct, old price archived")
        else:
            self.out(f"  price {want['lookup_key']}: created ({want['amount']} ct/{want['interval']})")
        return new["id"]

    def webhook(self) -> tuple[str, str | None]:
        """(endpoint id, signing secret only when just created)."""
        for ep in self.get("webhook_endpoints", {"limit": 100}).get("data") or []:
            if ep.get("url") == self.webhook_url:
                if sorted(ep.get("enabled_events") or []) != sorted(EVENTS):
                    self.post(f"webhook_endpoints/{ep['id']}", {"enabled_events": EVENTS})
                    self.out("  webhook endpoint: events updated")
                else:
                    self.out("  webhook endpoint: exists")
                return ep["id"], None
        data = {"url": self.webhook_url, "enabled_events": EVENTS, "description": "Simple Mods Pro (smweb)", "metadata": {"sm_key": PORTAL_KEY}}
        if self.api_version:
            data["api_version"] = self.api_version
        ep = self.post("webhook_endpoints", data, idem=f"smpro-webhook-{self.webhook_url}")
        self.created += 1
        self.out("  webhook endpoint: created (signing secret goes into the billing file, not shown)")
        return ep["id"], ep.get("secret")

    def portal(self, prices: dict) -> str:
        data = {"business_profile": {"headline": "Simple Mods Pro"},
                "default_return_url": self.return_url,
                "features": {"customer_update": {"enabled": True, "allowed_updates": ["email", "address", "name", "tax_id"]},
                             "invoice_history": {"enabled": True}, "payment_method_update": {"enabled": True},
                             "subscription_cancel": {"enabled": True, "mode": "at_period_end",
                                                     "cancellation_reason": {"enabled": True, "options": ["too_expensive", "unused", "other"]}},
                             # P070 (owner 19.11.): plan changes only on our page, at the period end, without proration -> off here
                             "subscription_update": {"enabled": False}},
                "metadata": {"sm_key": PORTAL_KEY}}
        if self.legal.get("privacy_url"):
            data["business_profile"]["privacy_policy_url"] = self.legal["privacy_url"]
        if self.legal.get("terms_url"):
            data["business_profile"]["terms_of_service_url"] = self.legal["terms_url"]
        for cfg in self.get("billing_portal/configurations", {"limit": 100}).get("data") or []:
            if (cfg.get("metadata") or {}).get("sm_key") == PORTAL_KEY:
                self.post(f"billing_portal/configurations/{cfg['id']}", data)
                self.out("  portal configuration: updated")
                return cfg["id"]
        cfg = self.post("billing_portal/configurations", data, idem="smpro-portal")
        self.created += 1
        self.out("  portal configuration: created")
        return cfg["id"]

    def run(self) -> dict:
        prices = {}
        for w in wanted(self.plans):
            pid = self.product(w)
            prices[w["plan"]] = {iv: self.price(pid, want) for iv, want in w["prices"].items()}
        endpoint, secret = self.webhook()
        portal = self.portal(prices)
        return {"prices": prices, "webhook_endpoint": endpoint, "webhook_secret": secret, "portal_configuration": portal}


def write_billing(path: Path, result: dict, mode: str, key_file: str, key_var: str):
    """Merge into the billing file (mode 600); never writes the secret key itself, only where to read it."""
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        data = {"enabled": False}
    data.update({"mode": mode, "prices": result["prices"], "webhook_endpoint": result["webhook_endpoint"],
                 "portal_configuration": result["portal_configuration"], "key_file": key_file, "key_var": key_var})
    data.pop("secret_key", None)
    if result.get("webhook_secret"):
        data["webhook_secret"] = result["webhook_secret"]
    tmp = path.with_name(path.name + ".tmp")
    fd = os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=1)
    os.replace(tmp, path)
    os.chmod(path, 0o600)


def main(argv=None, api_factory=StripeHTTP, out=print) -> int:
    ap = argparse.ArgumentParser(description="Simple Mods Pro: Stripe products/prices/webhook/portal (dry run by default)")
    ap.add_argument("--env-file", default="/root/.config/sb-keys.env")
    ap.add_argument("--key-var", default="STRIPE_SECRET_KEY")
    ap.add_argument("--billing-file", default=None, help="billing JSON to update (outside the repo); none = print only")
    ap.add_argument("--webhook-url", default="https://livemap.simplemods.de/api/stripe/webhook")
    ap.add_argument("--return-url", default="https://account.simplemods.de/pro/manage")
    ap.add_argument("--plans", default=str(HERE / "plans.json"))
    ap.add_argument("--tiers", default=str(HERE / "tiers.json"))
    ap.add_argument("--config", default=None, help="server config (plans/tiers overrides, legal URLs)")
    ap.add_argument("--api-version", default=None)
    ap.add_argument("--statement-descriptor", default="SIMPLEMODS", help="bank statement text (max 22 chars, owner 19.11.: SIMPLEMODS)")
    ap.add_argument("--apply", action="store_true", help="really call Stripe (default: dry run)")
    ap.add_argument("--live", action="store_true", help="allow a live key (only after the test-mode run)")
    a = ap.parse_args(argv)

    server = {}
    if a.config:
        try:
            server = json.loads(Path(a.config).read_text(encoding="utf-8"))
        except (OSError, ValueError):
            out("config not readable")
            return 2
    tiers = load_tiers(Path(a.tiers), server.get("tiers") or {})
    plans = load_plans(Path(a.plans), server.get("plans") or {}, tiers)
    key = read_env_var(Path(a.env_file), a.key_var)
    mode = key_mode(key) if key else "none"
    out(f"Simple Mods Pro - Stripe setup ({'APPLY' if a.apply else 'dry run, nothing is sent'})")
    out(f"key: {a.key_var} {'present, ' + mode + ' mode' if key else 'missing'} (value never shown)")
    out(f"webhook: {a.webhook_url}")
    if not (5 <= len(a.statement_descriptor) <= 22) or any(ch in a.statement_descriptor for ch in "<>\\'\"*"):
        out("statement descriptor must be 5-22 characters without < > \\ ' \" *")
        return 2
    out(f"statement descriptor: {a.statement_descriptor}")
    for w in wanted(plans):
        out(f"  {w['product']}: {w['name']} -> livemap {w['livemap_tier']}; " +
            ", ".join(f"{p['lookup_key']} {p['amount']} ct/{p['interval']}" for p in w["prices"].values()))
    out("  events: " + ", ".join(EVENTS))
    if not a.apply:
        out("dry run done - add --apply to create/update the objects (test key first)")
        return 0
    if not key:
        out(f"no {a.key_var} in {a.env_file}")
        return 2
    if mode == "live" and not a.live:
        out("live key refused: run in test mode first, then add --live")
        return 2
    if mode == "unknown":
        out("key does not look like a Stripe secret/restricted key")
        return 2
    api = api_factory(key, a.api_version)
    del key
    try:
        result = Setup(api, plans, a.webhook_url, a.return_url, server.get("legal") or {}, a.api_version, out,
                       a.statement_descriptor).run()
    except StripeError as ex:
        out(f"Stripe answered {ex.status} {ex.code}; nothing else changed (run again after fixing)")
        return 1
    out(f"done: {sum(1 for _ in result['prices'])} plans, endpoint {result['webhook_endpoint']}, portal {result['portal_configuration']}")
    if a.billing_file:
        write_billing(Path(a.billing_file), result, mode, a.env_file, a.key_var)
        out(f"billing file updated: {a.billing_file} (mode 600; webhook secret {'written' if result.get('webhook_secret') else 'unchanged'})")
        if not result.get("webhook_secret"):
            out("note: Stripe shows a signing secret only when an endpoint is created; roll it in the dashboard if the file has none")
    else:
        out("no --billing-file: price ids above are not stored; the new webhook secret (if any) was NOT saved")
    return 0


if __name__ == "__main__":
    sys.exit(main())
