#!/usr/bin/env python3
"""Creates the Paddle Billing objects of Simple Mods Pro (N108 addendum 4) idempotently - SANDBOX first:
one product per plan; per plan a monthly and a yearly price, the same two with a trial of the withdrawal period (used when the
buyer does not ask for the early start, like the Stripe trial) and a one-off gift price (monthly price, quantity = months);
the notification destination (webhook) with the events the service handles. Writes the price ids, the destination id and -
only when the destination is new - its secret into the billing file (``paddle`` section; outside the repo, mode 600).

Safe by default:
- DRY RUN unless ``--apply``: no network call, shows the plan.
- The API key is read at runtime from an env file (default ``/root/.config/sb-keys.env``, variable ``PADDLE_API_KEY``); it is
  never printed, logged or written anywhere (the service reads it the same way: ``paddle.key_file`` + ``paddle.key_var``).
- Sandbox first: ``--environment sandbox`` is the default; production needs ``--environment production --live``; a key made for
  the other environment (``pdl_sdbx_`` / ``pdl_live_``) is refused.
- Discounts (P074): creates NO discounts; the owner makes them later in the dashboard.
- Run it again any time: products/prices are found by ``custom_data.sm_key`` and reused; a changed amount makes a new price and
  archives the old one (prices keep their amount for running subscriptions); the destination is found by its URL.

    python3 tools/livemap-web/paddle_setup.py --webhook-url https://<livemap host>/api/paddle/webhook \\
        --billing-file /etc/simplemods/livemap-billing.json [--client-token test_...] [--apply] [--environment production --live]
No fixed host names: the webhook URL comes from the argument, else the server config (``--config``) or ``SMWEB_LIVEMAP_HOST``.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from smweb.config import load_plans, load_tiers  # noqa: E402
from smweb.payments.paddle import PaddleError, PaddleHTTP  # noqa: E402
from stripe_setup import host_url, read_env_var  # noqa: E402

EVENTS = ["subscription.created", "subscription.activated", "subscription.updated", "subscription.trialing", "subscription.past_due",
          "subscription.paused", "subscription.resumed", "subscription.canceled", "transaction.completed", "transaction.payment_failed",
          "adjustment.created", "adjustment.updated"]


def key_env(key: str) -> str:
    """Environment a Paddle API key belongs to (new key format ``pdl_sdbx_apikey_…`` / ``pdl_live_apikey_…``)."""
    if key.startswith("pdl_sdbx_"):
        return "sandbox"
    if key.startswith("pdl_live_"):
        return "production"
    return "unknown"


def wanted(plans: dict, trial_days: int, currency: str = "EUR") -> list[dict]:
    out = []
    for key in plans["order"]:
        p = plans["plans"][key]
        month, year = int(p["price_month"]), int(p["price_year"])
        name = f"Simple Mods Pro {p['name']}"
        prices = {
            "month": {"sm_key": f"smpro_{key}_month", "amount": month, "cycle": "month", "trial": 0, "description": f"{name} – monatlich"},
            "year": {"sm_key": f"smpro_{key}_year", "amount": year, "cycle": "year", "trial": 0, "description": f"{name} – jährlich (danach monatlich)"},
            "month_trial": {"sm_key": f"smpro_{key}_month_trial", "amount": month, "cycle": "month", "trial": trial_days,
                            "description": f"{name} – monatlich, Start nach {trial_days} Tagen Widerrufsfrist"},
            "year_trial": {"sm_key": f"smpro_{key}_year_trial", "amount": year, "cycle": "year", "trial": trial_days,
                           "description": f"{name} – jährlich, Start nach {trial_days} Tagen Widerrufsfrist"},
            "gift": {"sm_key": f"smpro_{key}_gift", "amount": month, "cycle": None, "trial": 0, "description": f"{name} – Geschenk, je Monat (Einmalzahlung)"},
        }
        for w in prices.values():
            w["currency"] = currency
        out.append({"plan": key, "sm_key": f"smpro_{key}", "name": name, "livemap_tier": p.get("livemap_tier") or "", "prices": prices})
    return out


class Setup:
    def __init__(self, api, plans: dict, webhook_url: str, trial_days: int, tax_category: str, out=print):
        self.api, self.plans, self.webhook_url, self.trial_days, self.tax_category = api, plans, webhook_url, trial_days, tax_category
        self.out = out
        self.created = 0

    def get(self, path, data=None):
        return self.api.call("GET", path, data or {}).get("data") or []

    def post(self, path, data):
        return self.api.call("POST", path, data).get("data") or {}

    def patch(self, path, data):
        return self.api.call("PATCH", path, data).get("data") or {}

    def product(self, w: dict) -> str:
        found = [p for p in self.get("products", {"status": "active", "per_page": 200}) if (p.get("custom_data") or {}).get("sm_key") == w["sm_key"]]
        if found:
            p = found[0]
            if p.get("name") != w["name"] or p.get("tax_category") != self.tax_category:
                self.patch(f"products/{p['id']}", {"name": w["name"], "tax_category": self.tax_category})
                self.out(f"  product {w['sm_key']}: updated")
            else:
                self.out(f"  product {w['sm_key']}: exists")
            return p["id"]
        p = self.post("products", {"name": w["name"], "tax_category": self.tax_category, "type": "standard",
                                   "description": "Simple Mods Pro – Konto-Abo für Simple Livemap und Frühzugang (digitale Dienste)",
                                   "custom_data": {"sm_key": w["sm_key"], "livemap_tier": w["livemap_tier"]}})
        self.created += 1
        self.out(f"  product {w['sm_key']}: created")
        return p["id"]

    @staticmethod
    def same(cur: dict, want: dict) -> bool:
        cycle = cur.get("billing_cycle") or None
        trial = cur.get("trial_period") or None
        return (str((cur.get("unit_price") or {}).get("amount")) == str(want["amount"])
                and (cur.get("unit_price") or {}).get("currency_code") == want["currency"]
                and ((cycle or {}).get("interval") if cycle else None) == want["cycle"]
                and ((trial or {}).get("frequency") if trial else 0) == want["trial"]
                and cur.get("tax_mode") == "internal")

    def price(self, product: str, want: dict, existing: list[dict]) -> str:
        cur = next((p for p in existing if (p.get("custom_data") or {}).get("sm_key") == want["sm_key"]), None)
        if cur and self.same(cur, want):
            self.out(f"  price {want['sm_key']}: exists ({want['amount']} ct)")
            return cur["id"]
        data = {"product_id": product, "description": want["description"], "name": want["description"],
                "unit_price": {"amount": str(want["amount"]), "currency_code": want["currency"]},
                "tax_mode": "internal",                      # final price incl. VAT (Paddle calculates and remits it)
                "custom_data": {"sm_key": want["sm_key"]}}
        if want["cycle"]:
            data["billing_cycle"] = {"interval": want["cycle"], "frequency": 1}
        else:
            data["quantity"] = {"minimum": 1, "maximum": 12}  # gift: quantity = months
        if want["trial"]:
            data["trial_period"] = {"interval": "day", "frequency": want["trial"]}
        new = self.post("prices", data)
        self.created += 1
        if cur:
            self.patch(f"prices/{cur['id']}", {"status": "archived"})
            self.out(f"  price {want['sm_key']}: new amount {want['amount']} ct, old price archived")
        else:
            self.out(f"  price {want['sm_key']}: created ({want['amount']} ct)")
        return new["id"]

    def destination(self) -> tuple[str, str | None]:
        """(notification setting id, secret only when just created)."""
        for d in self.get("notification-settings"):
            if d.get("destination") == self.webhook_url:
                have = sorted(e.get("name") if isinstance(e, dict) else e for e in d.get("subscribed_events") or [])
                if have != sorted(EVENTS) or not d.get("active", True):
                    self.patch(f"notification-settings/{d['id']}", {"subscribed_events": EVENTS, "active": True})
                    self.out("  notification destination: events updated")
                else:
                    self.out("  notification destination: exists")
                return d["id"], None
        d = self.post("notification-settings", {"description": "Simple Mods Pro (smweb)", "type": "url", "destination": self.webhook_url,
                                                "subscribed_events": EVENTS, "api_version": 1, "include_sensitive_fields": False,
                                                "traffic_source": "all"})
        self.created += 1
        self.out("  notification destination: created (secret goes into the billing file, not shown)")
        return d["id"], d.get("endpoint_secret_key")

    def run(self) -> dict:
        prices, gifts = {}, {}
        for w in wanted(self.plans, self.trial_days):
            pid = self.product(w)
            existing = self.get("prices", {"product_id": pid, "status": "active", "per_page": 200})
            ids = {k: self.price(pid, want, existing) for k, want in w["prices"].items()}
            gifts[w["plan"]] = ids.pop("gift")
            prices[w["plan"]] = ids
        ntf, secret = self.destination()
        return {"prices": prices, "gift_prices": gifts, "notification_setting": ntf, "webhook_secret": secret}


def write_billing(path: Path, result: dict, environment: str, key_file: str, key_var: str, client_token: str | None):
    """Merge into the billing file (mode 600), section ``paddle``; never writes the API key itself, only where to read it."""
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        data = {"enabled": False}
    data.setdefault("provider", "paddle")
    p = data.get("paddle") if isinstance(data.get("paddle"), dict) else {}
    p.update({"environment": environment, "prices": result["prices"], "gift_prices": result["gift_prices"],
              "notification_setting": result["notification_setting"], "key_file": key_file, "key_var": key_var})
    p.pop("api_key", None)
    if result.get("webhook_secret"):
        p["webhook_secret"] = result["webhook_secret"]
    if client_token:
        p["client_token"] = client_token
    data["paddle"] = p
    tmp = path.with_name(path.name + ".tmp")
    fd = os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=1)
    os.replace(tmp, path)
    os.chmod(path, 0o600)


def main(argv=None, api_factory=PaddleHTTP, out=print) -> int:
    ap = argparse.ArgumentParser(description="Simple Mods Pro: Paddle products/prices/notification destination (dry run by default)")
    ap.add_argument("--env-file", default="/root/.config/sb-keys.env")
    ap.add_argument("--key-var", default="PADDLE_API_KEY")
    ap.add_argument("--billing-file", default=None, help="billing JSON to update (outside the repo); none = print only")
    ap.add_argument("--webhook-url", default=None, help="default: https://<livemap host>/api/paddle/webhook (config/env)")
    ap.add_argument("--environment", choices=["sandbox", "production"], default="sandbox")
    ap.add_argument("--live", action="store_true", help="allow production (only after the sandbox run)")
    ap.add_argument("--client-token", default=None, help="Paddle client-side token (public, from the dashboard) for Paddle.js")
    ap.add_argument("--trial-days", type=int, default=14, help="trial of the *_trial prices = withdrawal period")
    ap.add_argument("--tax-category", default="standard", help="Paddle tax category of the products (standard | saas | digital-goods ...)")
    ap.add_argument("--plans", default=str(HERE / "plans.json"))
    ap.add_argument("--tiers", default=str(HERE / "tiers.json"))
    ap.add_argument("--config", default=None, help="server config (plans/tiers overrides, hosts)")
    ap.add_argument("--apply", action="store_true", help="really call Paddle (default: dry run)")
    a = ap.parse_args(argv)

    server = {}
    if a.config:
        try:
            server = json.loads(Path(a.config).read_text(encoding="utf-8"))
        except (OSError, ValueError):
            out("config not readable")
            return 2
    a.webhook_url = a.webhook_url or host_url(server, "livemap", "/api/paddle/webhook")
    tiers = load_tiers(Path(a.tiers), server.get("tiers") or {})
    plans = load_plans(Path(a.plans), server.get("plans") or {}, tiers)
    key = read_env_var(Path(a.env_file), a.key_var)
    kenv = key_env(key) if key else "none"
    out(f"Simple Mods Pro - Paddle setup ({'APPLY' if a.apply else 'dry run, nothing is sent'}), environment {a.environment}")
    out(f"key: {a.key_var} {'present (' + kenv + ')' if key else 'missing'} (value never shown)")
    out(f"webhook: {a.webhook_url or '(not set)'}")
    for w in wanted(plans, a.trial_days):
        out(f"  {w['sm_key']}: {w['name']} -> livemap {w['livemap_tier']}; " +
            ", ".join(f"{p['sm_key']} {p['amount']} ct/{p['cycle'] or 'once'}" + (f" trial {p['trial']} d" if p["trial"] else "") for p in w["prices"].values()))
    out("  events: " + ", ".join(EVENTS))
    if not a.apply:
        out("dry run done - add --apply to create/update the objects (sandbox first)")
        return 0
    if not key:
        out(f"no {a.key_var} in {a.env_file}")
        return 2
    if not a.webhook_url:
        out("webhook URL unknown: pass --webhook-url, --config with hosts, or set SMWEB_LIVEMAP_HOST")
        return 2
    if a.environment == "production" and not a.live:
        out("production refused: run in the sandbox first, then add --environment production --live")
        return 2
    if kenv != "unknown" and kenv != a.environment:
        out(f"key belongs to {kenv}, not {a.environment}: refused")
        return 2
    api = api_factory(key, a.environment)
    del key
    try:
        result = Setup(api, plans, a.webhook_url, a.trial_days, a.tax_category, out).run()
    except PaddleError as ex:
        out(f"Paddle answered {ex.status} {ex.code}; nothing else changed (run again after fixing)")
        return 1
    out(f"done: {len(result['prices'])} plans, notification destination {result['notification_setting']}")
    if a.billing_file:
        write_billing(Path(a.billing_file), result, a.environment, a.env_file, a.key_var, a.client_token)
        out(f"billing file updated: {a.billing_file} (mode 600; webhook secret {'written' if result.get('webhook_secret') else 'unchanged'})")
        if not result.get("webhook_secret"):
            out("note: the destination existed; if the billing file has no webhook secret, copy it from the Paddle dashboard into the file (never into the repo)")
    else:
        out("no --billing-file: ids above are not stored; a new webhook secret (if any) was NOT saved")
    return 0


if __name__ == "__main__":
    sys.exit(main())
