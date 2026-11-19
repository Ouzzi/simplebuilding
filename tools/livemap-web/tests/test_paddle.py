"""N108 addendum 3/4: payment provider interface, Paddle Billing (v2) as the default provider. Paddle-Signature (valid / forged /
replayed / stale), idempotency, provider switching, and every earlier feature through Paddle - order with and without the
early-start consent, coupons (discounts), yearly then monthly, plan change at the period end (do_not_bill), cancel button,
portal, payment failed/grace/end, refunds, gifts as one-off purchase with code redemption, fellow-player seats, CSP of the
overlay page. No network: the Paddle API is tests/fake_paddle.FakePaddle."""
from __future__ import annotations

import json
import re
import shutil
import unittest

from helpers import Client, account, make, temp
import fake_paddle as fp
import fake_stripe as fs

from smweb import payments
from smweb.app import Request
from smweb.payments import base as evk
from smweb.payments import paddle as pdl
from smweb.payments.base import PaymentProvider

PSECRET = "pdl_ntfset_unit_test_only"
SSECRET = "whsec_unit_test_only"
PRICES = {"basic": {"month": "pri_b_m", "year": "pri_b_y", "month_trial": "pri_b_mt", "year_trial": "pri_b_yt"},
          "max": {"month": "pri_p_m", "year": "pri_p_y", "month_trial": "pri_p_mt", "year_trial": "pri_p_yt"},
          "ultra": {"month": "pri_u_m", "year": "pri_u_y", "month_trial": "pri_u_mt", "year_trial": "pri_u_yt"}}
GIFTS = {"basic": "pri_g_b", "max": "pri_g_p", "ultra": "pri_g_u"}
DAY = 86400


class PaddleBase(unittest.TestCase):
    def setUp(self):
        self.dir = temp()
        self.svc, self.app, self.clock = make(self.dir)
        self.svc.cfg.billing = {"provider": "paddle", "enabled": True, "withdrawal_days": 14, "grace_days": 7, "terms_version": "t1",
                                "vat": {"mode": "small_business", "rate_percent": 19},
                                "paddle": {"environment": "sandbox", "api_key": "pdl_sdbx_apikey_unit", "webhook_secret": PSECRET,
                                           "client_token": "test_client_token_unit", "prices": PRICES, "gift_prices": GIFTS}}
        self.fake = fp.FakePaddle(now=lambda: int(self.clock()))
        self.svc.paddle_api = self.fake
        self.c = Client(self.app)

    def tearDown(self):
        self.svc.db.close()
        shutil.rmtree(self.dir, ignore_errors=True)

    def hook(self, evt: dict, secret: str = PSECRET, host: str = "livemap.test", ts: int | None = None, body: bytes | None = None,
             header: str | None = None):
        raw, sig = fp.signed(evt, secret, int(self.clock()) if ts is None else ts)
        r = self.app.handle(Request("POST", host, "/api/paddle/webhook", {}, {"paddle-signature": header if header is not None else sig,
                                                                                  "content-type": "application/json"},
                                    body if body is not None else raw))
        return r.status, json.loads(r.body or b"{}")

    def order(self, plan="basic", interval="month", waiver=True, coupon="", c: Client | None = None):
        c = c or self.c
        form = {"plan": plan, "interval": interval, "terms": "1"}
        if waiver:
            form["waiver"] = "1"
        if coupon:
            form["coupon"] = coupon
        return c.post("https://account.test/pro/order", form, page=f"https://account.test/pro/order?plan={plan}&interval={interval}")

    def txn_of(self, r):
        loc = r.header("location")
        self.assertTrue(loc.startswith("https://account.test/pro/pay?_ptxn=txn_"), loc)
        return self.fake.objects[loc.split("_ptxn=")[1]]

    def buy(self, plan="basic", interval="month", waiver=True, sub="sub_01test1", cus="ctm_01test1", sub_first=False):
        """Whole flow as Paddle drives it after the overlay: transaction.completed + subscription.created (either order)."""
        r = self.order(plan, interval, waiver)
        self.assertEqual(r.status, 303, r.body[:400])
        txn = self.txn_of(r)
        now = int(self.clock())
        end = now + (365 if interval == "year" else 30) * DAY
        price = txn["items"][0]["price_id"]
        status = "active" if waiver else "trialing"
        evs = [fp.event("transaction.completed", fp.transaction(txn["id"], cus, sub, txn["custom_data"], 0 if not waiver else 399, end), now),
               fp.event("subscription.created", fp.subscription(sub, cus, price, status, end, txn["custom_data"], interval), now)]
        for e in (reversed(evs) if sub_first else evs):
            self.assertEqual(self.hook(e)[0], 200)
        return txn, end

    def user(self, email="alex@example.com"):
        return self.svc.db.one("SELECT * FROM users WHERE email = ?", email)

    def sub_event(self, price, status="active", end=0, at=None, cancel=False, sub="sub_01test1", cus="ctm_01test1", kind="subscription.updated", interval="month"):
        txn = next((o for o in self.fake.objects.values() if o.get("_kind") == "transaction"), {})
        return self.hook(fp.event(kind, fp.subscription(sub, cus, price, status, end, txn.get("custom_data") or {}, interval, cancel),
                                  at if at is not None else int(self.clock())))


# -------------------------------------------------------------------- signature
class SignatureTests(unittest.TestCase):
    def test_scheme(self):
        body, secret = b'{"event_id":"evt_1"}', "pdl_ntfset_x"
        h = pdl.sign(body, secret, 1000)
        self.assertRegex(h, r"^ts=1000;h1=[0-9a-f]{64}$")
        self.assertTrue(pdl.verify_signature(body, h, secret, now=1100))
        self.assertFalse(pdl.verify_signature(body + b" ", h, secret, now=1100), "body changed")
        self.assertFalse(pdl.verify_signature(body, h, "other", now=1100), "other secret")
        self.assertFalse(pdl.verify_signature(body, h, secret, now=1000 + 301), "stale timestamp")
        self.assertFalse(pdl.verify_signature(body, h, secret, now=1000 - 301), "timestamp from the future")
        self.assertTrue(pdl.verify_signature(body, h.replace("h1=", "h1=" + "a" * 64 + ";h1="), secret, now=1100), "rotation: any h1")
        for bad in (None, "", "ts=abc;h1=00", "h1=" + "0" * 64, "ts=1000", "t=1000,v1=" + h.split("h1=")[1]):
            self.assertFalse(pdl.verify_signature(body, bad, secret, now=1100), bad)
        self.assertFalse(pdl.verify_signature(body, h, "", now=1100), "no secret configured")
        # signed text is "<ts>:<body>" (not Stripe's "<ts>.<body>")
        self.assertFalse(pdl.verify_signature(body, fs.sign(body, secret, 1000).replace("t=", "ts=").replace(",v1=", ";h1="), secret, now=1100))

    def test_time_parsing(self):
        self.assertEqual(pdl.parse_time("2027-01-15T08:00:00.123456Z"), pdl.parse_time("2027-01-15T08:00:00Z"))
        self.assertEqual(pdl.parse_time("2027-01-15T08:00:00+01:00"), pdl.parse_time("2027-01-15T07:00:00Z"))
        self.assertEqual(pdl.parse_time(fp.iso(1_800_000_000)), 1_800_000_000)
        self.assertIsNone(pdl.parse_time("nonsense"))
        self.assertIsNone(pdl.parse_time(None))


class WebhookTests(PaddleBase):
    def test_valid_forged_replayed_stale(self):
        u = account(self.c, self.svc)
        now = int(self.clock())
        evt = fp.event("subscription.created", fp.subscription("sub_x", "ctm_x", "pri_u_m", "active", now + 30 * DAY, {"user": str(u["id"]), "plan": "ultra"}), now)
        self.assertEqual(self.hook(evt, secret="pdl_ntfset_attacker")[0], 400, "forged")
        self.assertEqual(self.hook(evt, ts=now - 301)[0], 400, "stale timestamp")
        self.assertEqual(self.hook(evt, ts=now + 301)[0], 400, "future timestamp")
        raw, sig = fp.signed(evt, PSECRET, now)
        self.assertEqual(self.hook(evt, body=raw.replace(b'"pri_u_m"', b'"pri_u_y"'), header=sig)[0], 400, "tampered body")
        for header in ("", "ts=abc;h1=00", f"ts={now}", "h1=" + "0" * 64):
            self.assertEqual(self.hook(evt, header=header)[0], 400, header)
        self.assertEqual(self.user()["tier"], "free")
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM stripe_events"), 0)
        self.assertEqual(self.hook(evt), (200, {"received": True}), "valid")
        self.assertEqual(self.user()["tier"], "ultra")
        self.assertEqual(self.hook(evt), (200, {"received": True, "duplicate": True}), "replayed with a fresh signature: no effect")
        self.clock.advance(400)
        self.assertEqual(self.app.handle(Request("POST", "livemap.test", "/api/paddle/webhook", {}, {"paddle-signature": sig}, raw)).status, 400,
                         "replayed with the old signature after the tolerance")
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM stripe_events WHERE id = ?", "paddle:" + evt["event_id"]), 1)

    def test_idempotent_effects(self):
        account(self.c, self.svc)
        self.buy("basic")
        n = len(self.svc.mailer.sent)
        self.svc.cfg.billing["mails"] = {"invoice": True}
        tx = fp.transaction("txn_renew1", "ctm_01test1", "sub_01test1", {}, 399, int(self.clock()) + 60 * DAY, origin="subscription_recurring")
        e1 = fp.event("transaction.completed", tx, int(self.clock()))
        e2 = fp.event("transaction.completed", tx, int(self.clock()))       # same payload, new event id (Paddle re-sent)
        self.assertEqual(self.hook(e1)[0], 200)
        self.assertEqual(self.hook(e1)[1], {"received": True, "duplicate": True})
        self.assertEqual(self.hook(e2)[0], 200)
        self.assertEqual(len(self.svc.mailer.sent), n + 1, "one invoice mail per transaction")
        self.assertEqual(self.svc.db.value("SELECT period_end FROM subscriptions"), int(self.clock()) + 60 * DAY)

    def test_failed_handler_lets_paddle_retry(self):
        account(self.c, self.svc)
        r = self.order("max")
        txn = self.txn_of(r)
        evt = fp.event("transaction.completed", fp.transaction(txn["id"], "ctm_y", "sub_y", txn["custom_data"], 799), int(self.clock()))
        orig = self.svc.recompute
        self.svc.recompute = lambda uid: (_ for _ in ()).throw(RuntimeError("db busy"))
        with self.assertRaises(RuntimeError):
            self.app.svc.webhook(fp.signed(evt, PSECRET, int(self.clock()))[0], {"paddle-signature": fp.signed(evt, PSECRET, int(self.clock()))[1]}, "paddle")
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM stripe_events"), 0, "id forgotten -> Paddle retries")
        self.svc.recompute = orig
        self.assertEqual(self.hook(evt)[0], 200)
        self.assertEqual(sum(1 for m in self.svc.mailer.sent if "Max" in m["subject"]), 1, "order mail once")

    def test_out_of_order_and_unknown_events(self):
        account(self.c, self.svc)
        _, end = self.buy("max")
        now = int(self.clock())
        self.sub_event("pri_p_m", "canceled", end, at=now + 10, kind="subscription.canceled")
        self.sub_event("pri_p_m", "active", end + 30 * DAY, at=now + 5)
        self.assertEqual(self.svc.db.value("SELECT status FROM subscriptions"), "canceled")
        self.assertEqual(self.user()["tier"], "free")
        self.assertEqual(self.hook(fp.event("customer.updated", {"id": "ctm_01test1"}, now))[0], 200, "unknown types are accepted, no effect")
        self.assertEqual(self.hook(fp.event("address.created", {"id": "add_1"}, now), host="account.test")[0], 200, "also on the account host")
        bad = fp.event("x", {}, now)
        bad["data"] = []
        self.assertEqual(self.hook(bad)[0], 400)


# -------------------------------------------------------------------- provider switching / interface
class ProviderTests(PaddleBase):
    def test_interface_is_implemented_by_both(self):
        names = [n for n in vars(PaymentProvider) if not n.startswith("_") and callable(getattr(PaymentProvider, n))]
        for name in ("paddle", "stripe"):
            prov = payments.make(self.svc, name)
            self.assertIsInstance(prov, PaymentProvider)
            for n in names:
                self.assertIn(n, dir(prov), (name, n))
                if n not in ("maintenance", "own_customer"):
                    self.assertIsNot(getattr(type(prov), n), getattr(PaymentProvider, n), f"{name}.{n} implemented")
        with self.assertRaises(ValueError):
            payments.make(self.svc, "lemonsqueezy")
        self.assertEqual(payments.DEFAULT, "paddle")
        for k in ("subscription_started", "subscription_changed", "subscription_ended", "payment_failed", "refund"):
            self.assertIn(k, evk.KINDS, "addendum 3 event names")

    def test_default_provider_is_paddle_and_switch_to_stripe(self):
        del self.svc.cfg.billing["provider"]
        self.assertEqual(self.svc.provider_name(), "paddle")
        account(self.c, self.svc)
        self.assertTrue(self.order().header("location").startswith("https://account.test/pro/pay?_ptxn="))
        # switch: Stripe keys at the top level, provider "stripe" -> Stripe Checkout, Paddle untouched
        self.svc.cfg.billing.update({"provider": "stripe", "secret_key": "sk_test_unit", "webhook_secret": SSECRET,
                                     "prices": {"basic": {"month": "price_b_m", "year": "price_b_y"}}})
        sfake = fs.FakeStripe(now=lambda: int(self.clock()))
        self.svc.stripe_api = sfake
        self.svc.limiter.hits.clear()
        n = len(self.fake.calls)
        r = self.c.post("https://account.test/pro/order", {"plan": "basic", "interval": "month", "terms": "1"}, page="https://account.test/pro/order?plan=basic")
        self.assertTrue(r.header("location").startswith("https://checkout.stripe.com/"), r.header("location"))
        self.assertEqual(len(self.fake.calls), n)
        self.assertIn("https://checkout.stripe.com", self.c.get("https://account.test/pro").header("content-security-policy"))
        self.svc.cfg.billing["provider"] = "nope"
        self.assertFalse(self.svc.billing_on(), "unknown provider = off")
        self.assertEqual(self.order().status, 503)

    def test_each_route_checks_its_own_secret(self):
        account(self.c, self.svc)
        now = int(self.clock())
        evt = fp.event("subscription.created", fp.subscription("sub_q", "ctm_q", "pri_b_m", "active", now + DAY, {"user": "1"}), now)
        raw, sig = fp.signed(evt, PSECRET, now)
        r = self.app.handle(Request("POST", "livemap.test", "/api/stripe/webhook", {}, {"paddle-signature": sig, "stripe-signature": sig}, raw))
        self.assertEqual(r.status, 503, "Stripe not configured -> its route is off")
        self.svc.cfg.billing.update({"secret_key": "sk_test_unit", "webhook_secret": SSECRET})
        r = self.app.handle(Request("POST", "livemap.test", "/api/stripe/webhook", {}, {"paddle-signature": sig, "stripe-signature": sig}, raw))
        self.assertEqual(r.status, 400, "a Paddle signature is no Stripe signature")
        # a former provider's subscription still gets its events after the switch (Stripe route, Stripe secret)
        sevt = fs.event("customer.subscription.updated", fs.subscription("sub_old", "cus_old", "price_x", "active", now + DAY, {"plan": "max", "user": "1"}), now)
        sraw, ssig = fs.signed(sevt, SSECRET, now)
        self.assertEqual(self.app.handle(Request("POST", "livemap.test", "/api/stripe/webhook", {}, {"stripe-signature": ssig}, sraw)).status, 200)
        self.assertEqual(self.user()["tier"], "max")
        self.assertEqual(self.app.handle(Request("POST", "livemap.test", "/api/lemon/webhook", {}, {}, b"{}")).status, 404)

    def test_paddle_off_and_down(self):
        self.svc.cfg.billing["enabled"] = False
        account(self.c, self.svc)
        self.assertEqual(self.order().status, 503)
        self.assertEqual(self.fake.calls, [])
        self.assertEqual(self.hook(fp.event("subscription.created", {}, 1))[0], 503)
        self.svc.cfg.billing["enabled"] = True
        self.fake.fail.add("transactions")
        r = self.order()
        self.assertEqual(r.status, 502)
        self.assertIn("nothing was charged", r.body.decode().lower())


# -------------------------------------------------------------------- order flow
class OrderTests(PaddleBase):
    def test_transaction_parameters_and_pay_page(self):
        u = account(self.c, self.svc)
        r = self.order("max", "month", waiver=True)
        txn = self.txn_of(r)
        method, path, data = self.fake.calls[-1]
        self.assertEqual((method, path), ("POST", "transactions"))
        self.assertEqual(data["items"], [{"price_id": "pri_p_m", "quantity": 1}])
        self.assertEqual(data["custom_data"], {"kind": "subscription", "order": data["custom_data"]["order"], "plan": "max", "interval": "month", "user": str(u["id"])})
        self.assertEqual(data["checkout"], {"url": "https://account.test/pro/pay"})
        self.assertNotIn("customer_id", data)
        self.assertNotIn("discount_id", data)
        o = self.svc.db.one("SELECT * FROM orders")
        self.assertEqual((o["plan"], o["interval"], o["price_cents"], o["waiver"], o["session_id"]), ("max", "month", 799, 1, txn["id"]))
        page = self.c.get(r.header("location"))
        self.assertEqual(page.status, 200)
        body = page.body.decode()
        self.assertIn('src="https://cdn.paddle.com/paddle/v2/paddle.js"', body)
        self.assertIn('src="/static/paddle-checkout.js"', body)
        self.assertIn(f'data-txn="{txn["id"]}"', body)
        self.assertIn('data-env="sandbox"', body)
        self.assertIn('data-success="https://account.test/pro/done"', body)
        self.assertIn('data-token="test_client_token_unit"', body)
        self.assertNotIn("pdl_sdbx_apikey", body, "the API key never reaches a page")
        self.assertNotIn("<script>", body, "no inline script")
        csp = page.header("content-security-policy")
        self.assertIn("script-src 'self' https://cdn.paddle.com", csp)
        self.assertIn("https://sandbox-buy.paddle.com", csp)
        self.assertIn("frame-ancestors 'none'", csp)
        self.assertEqual(self.c.get("https://account.test/pro/pay?_ptxn=<x>").status, 404)
        js = self.c.get("https://account.test/static/paddle-checkout.js")
        self.assertEqual(js.status, 200)
        self.assertIn(b"Paddle.Initialize", js.body)

    def test_order_page_texts_for_paddle(self):
        account(self.c, self.svc)
        body = self.c.get("https://account.test/pro/order?plan=max&interval=year&lang=de").body.decode()
        self.assertIn("zahlungspflichtig bestellen", body)
        self.assertIn("Merchant of Record", body)
        self.assertIn("Bezahlen mit Paddle", body)
        self.assertIn("Nutzungsbedingungen", body)
        self.assertNotIn("Stripe", body)
        self.assertNotIn("§ 19 UStG", body, "Paddle sells: final prices incl. VAT")
        self.assertIn("inkl. gesetzlicher USt.", body)
        csp = self.c.get("https://account.test/pro").header("content-security-policy")
        self.assertIn("customer-portal.paddle.com", csp)
        self.assertNotIn("stripe.com", csp)

    def test_without_waiver_trial_price_and_late_start(self):
        account(self.c, self.svc)
        txn, _ = self.buy("basic", waiver=False)
        self.assertEqual(txn["items"][0]["price_id"], "pri_b_mt", "trial = withdrawal period: no charge before the perks start")
        self.assertEqual(self.user()["tier"], "free")
        self.assertFalse(self.svc.has_perk(self.user(), "early_access"))
        self.clock.advance(14 * DAY + 1)
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "basic")
        # without trial prices configured the normal price is used (perks still start late)
        for p in PRICES.values():
            p.pop("month_trial")
        self.svc.cfg.billing["paddle"]["prices"] = PRICES
        self.assertEqual(self.svc.provider().price_id("max", "month_trial"), None)

    def test_monthly_flow_entitlement_mails_customer(self):
        account(self.c, self.svc)
        self.svc.mailer.sent.clear()
        self.buy("basic", sub_first=True)
        u = self.user()
        self.assertEqual((u["tier"], u["billing_customer"]), ("basic", "ctm_01test1"))
        ent = self.svc.entitlement(u)
        self.assertEqual(ent["plan"], "basic")
        subjects = [m["subject"] for m in self.svc.mailer.sent]
        self.assertTrue(any("Order confirmation" in s or "Bestellbestätigung" in s for s in subjects), subjects)
        self.assertFalse(any("Invoice" in s or "Rechnung" in s for s in subjects), "Paddle sends invoices itself (default)")
        self.assertEqual(self.svc.db.value("SELECT completed_at IS NOT NULL FROM orders"), 1)
        self.assertEqual(self.svc.db.value("SELECT order_id FROM subscriptions"), self.svc.db.value("SELECT id FROM orders"))
        self.assertIn("Simple Mods Pro Basic", self.c.get("https://account.test/pro/manage").body.decode())
        # second order with the known customer passes customer_id
        self.svc.limiter.hits.clear()
        with self.svc.db.tx() as c:
            c.execute("UPDATE subscriptions SET status = 'canceled'")
        self.order("max")
        self.assertEqual(self.fake.calls[-1][2]["customer_id"], "ctm_01test1")

    def test_coupons_are_paddle_discounts(self):
        account(self.c, self.svc)
        r = self.order("basic", coupon="NOPE")
        self.assertEqual(r.status, 400)
        self.assertIn("coupon code is not valid", r.body.decode())
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM orders"), 0, "unknown code: nothing stored")
        self.fake.objects["dsc_1"] = {"_kind": "discount", "id": "dsc_1", "code": "START10", "status": "active", "type": "percentage", "amount": "10"}
        r = self.order("basic", coupon="START10")
        self.assertEqual(r.status, 303)
        self.assertEqual(self.fake.sent("GET", "discounts")[-1][1], {"code": "START10", "status": "active"})
        self.assertEqual(self.fake.calls[-1][2]["discount_id"], "dsc_1")
        self.assertEqual(self.svc.db.value("SELECT coupon FROM orders ORDER BY id DESC"), "START10")
        self.assertFalse(self.fake.sent("POST", "discounts"), "no discounts are made")
        self.svc.cfg.billing["coupons"] = {"enabled": False}
        self.svc.limiter.hits.clear()
        self.assertEqual(self.order("max", coupon="START10").status, 400)


# -------------------------------------------------------------------- lifecycle
class LifecycleTests(PaddleBase):
    def test_yearly_then_monthly_switch_before_year_end(self):
        account(self.c, self.svc)
        _, end = self.buy("max", "year", sub="sub_01year")
        row = self.svc.db.one("SELECT * FROM subscriptions WHERE id = 'sub_01year'")
        self.assertEqual((row["interval"], row["pending_interval"], row["pending_applied"]), ("year", "month", 0))
        self.assertEqual(self.user()["tier"], "max")
        self.assertFalse(self.fake.sent("PATCH", "subscriptions/"))
        self.clock.t = end - 40 * DAY
        self.svc.maintenance()
        self.svc.maintenance()
        notes = [m for m in self.svc.mailer.sent if "monatliche Laufzeit" in m["subject"] or "monthly soon" in m["subject"]]
        self.assertEqual(len(notes), 1, "year notice once")
        self.assertFalse(self.fake.sent("PATCH", "subscriptions/"), "not yet")
        self.clock.t = end - 12 * 3600
        self.svc.maintenance()
        path, data = self.fake.sent("PATCH", "subscriptions/")[-1]
        self.assertEqual(path, "subscriptions/sub_01year")
        self.assertEqual(data["items"], [{"price_id": "pri_p_m", "quantity": 1}])
        self.assertEqual(data["proration_billing_mode"], "do_not_bill")
        self.svc.maintenance()
        self.assertEqual(len(self.fake.sent("PATCH", "subscriptions/")), 1, "sent once")
        # Paddle confirms the item change in the same period -> still the yearly row until the renewal
        self.sub_event("pri_p_m", "active", end, sub="sub_01year", interval="month")
        self.assertEqual(self.svc.db.value("SELECT interval FROM subscriptions"), "year")
        self.clock.t = end + 60
        self.sub_event("pri_p_m", "active", end + 30 * DAY, sub="sub_01year", interval="month")
        row = self.svc.db.one("SELECT * FROM subscriptions")
        self.assertEqual((row["interval"], row["pending_interval"], row["pending_applied"], row["plan"]), ("month", None, 0, "max"))

    def test_plan_change_at_period_end_do_not_bill(self):
        account(self.c, self.svc)
        _, end = self.buy("basic")
        r = self.c.post("https://account.test/pro/manage", {"action": "change", "plan": "ultra"})
        self.assertIn("Change planned", r.body.decode())
        self.assertEqual(self.svc.db.value("SELECT pending_plan FROM subscriptions"), "ultra")
        self.assertFalse(self.fake.sent("PATCH", "subscriptions/"), "nothing sent early")
        self.clock.t = end - 3600
        self.svc.maintenance()
        path, data = self.fake.sent("PATCH", "subscriptions/")[-1]
        self.assertEqual((data["items"][0]["price_id"], data["proration_billing_mode"]), ("pri_u_m", "do_not_bill"))
        self.assertEqual(data["custom_data"]["plan"], "ultra")
        self.sub_event("pri_u_m", "active", end)          # Paddle: item changed, same period
        self.assertEqual(self.user()["tier"], "basic", "paid plan until the period end, no proration")
        self.assertEqual(self.svc.db.value("SELECT plan FROM subscriptions"), "basic")
        self.clock.t = end + 10
        self.sub_event("pri_u_m", "active", end + 30 * DAY)
        self.assertEqual(self.user()["tier"], "ultra")
        self.assertIsNone(self.svc.db.value("SELECT pending_plan FROM subscriptions"))

    def test_plan_change_undo_and_late_change(self):
        account(self.c, self.svc)
        _, end = self.buy("max")
        self.svc.change_plan(self.user(), "basic")
        self.assertEqual(self.svc.change_plan(self.user(), "max"), {"pending": None, "at": end})
        self.assertFalse(self.fake.sent("PATCH", "subscriptions/"), "undo before sending: no call")
        self.clock.t = end - 600                               # inside the apply window: sent right away
        self.svc.change_plan(self.user(), "ultra")
        self.assertEqual(self.fake.sent("PATCH", "subscriptions/")[-1][1]["items"][0]["price_id"], "pri_u_m")
        self.svc.change_plan(self.user(), "max")               # undo after sending: back to the current price, still do_not_bill
        path, data = self.fake.sent("PATCH", "subscriptions/")[-1]
        self.assertEqual((data["items"][0]["price_id"], data["proration_billing_mode"]), ("pri_p_m", "do_not_bill"))
        self.assertIsNone(self.svc.db.value("SELECT pending_plan FROM subscriptions"))

    def test_cancel_button_cancels_at_period_end(self):
        account(self.c, self.svc)
        _, end = self.buy("max")
        self.svc.change_plan(self.user(), "ultra")
        other = Client(self.app, ip="192.0.2.50")
        r = other.post("https://account.test/kuendigen", {"email": "ALEX@example.com", "kind": "ordinary"})
        self.assertEqual(r.status, 200)
        path, data = self.fake.sent("POST", "subscriptions/")[-1]
        self.assertEqual((path, data), ("subscriptions/sub_01test1/cancel", {"effective_from": "next_billing_period"}))
        row = self.svc.db.one("SELECT * FROM cancellations")
        self.assertEqual((row["state"], row["effective_at"]), ("scheduled", end))
        sub = self.svc.db.one("SELECT * FROM subscriptions")
        self.assertEqual((sub["cancel_at_period_end"], sub["pending_plan"]), (1, None), "a pending change ends with the cancellation")
        self.assertEqual(self.user()["tier"], "max")
        self.sub_event("pri_p_m", "active", end, cancel=True)
        self.assertEqual(self.svc.db.value("SELECT cancel_at_period_end FROM subscriptions"), 1)
        self.clock.t = end
        self.sub_event("pri_p_m", "canceled", end, kind="subscription.canceled")
        self.assertEqual(self.user()["tier"], "free")
        self.assertTrue(any("beendet" in m["subject"] or "ended" in m["subject"] for m in self.svc.mailer.sent))

    def test_cancel_falls_back_to_manual_when_paddle_fails(self):
        account(self.c, self.svc)
        self.buy("max")
        self.fake.fail.add("subscriptions/sub_01test1/cancel")
        self.c.post("https://account.test/kuendigen", {"email": "alex@example.com", "kind": "ordinary"})
        self.assertEqual(self.svc.db.value("SELECT state FROM cancellations"), "manual")
        self.assertTrue(self.svc.mailer.sent[-1]["subject"])

    def test_portal_link(self):
        account(self.c, self.svc)
        self.buy()
        r = self.c.post("https://account.test/pro/manage", {"action": "portal"})
        self.assertEqual(r.status, 303)
        self.assertTrue(r.header("location").startswith("https://sandbox-customer-portal.paddle.com/"))
        path, data = self.fake.calls[-1][1:]
        self.assertEqual((path, data), ("customers/ctm_01test1/portal-sessions", {"subscription_ids": ["sub_01test1"]}))

    def test_payment_failed_grace_then_free(self):
        account(self.c, self.svc)
        _, end = self.buy("ultra")
        now = int(self.clock())
        self.hook(fp.event("transaction.payment_failed", fp.transaction("txn_f1", "ctm_01test1", "sub_01test1", {}, 1499, status="past_due", failed=1), now + 1))
        self.hook(fp.event("transaction.payment_failed", fp.transaction("txn_f1", "ctm_01test1", "sub_01test1", {}, 1499, status="past_due", failed=1), now + 2))
        self.sub_event("pri_u_m", "past_due", end, at=now + 3, kind="subscription.past_due")
        fails = [m for m in self.svc.mailer.sent if "fehlgeschlagen" in m["subject"] or "failed" in m["subject"]]
        self.assertEqual(len(fails), 1, "one mail per attempt")
        self.clock.t = end + 6 * DAY
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "ultra", "grace days")
        self.clock.t = end + 8 * DAY
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "free")

    def test_paused_is_not_entitled(self):
        account(self.c, self.svc)
        _, end = self.buy("max")
        self.sub_event("pri_p_m", "paused", end, kind="subscription.paused")
        self.assertEqual(self.user()["tier"], "free")
        self.sub_event("pri_p_m", "active", end, at=int(self.clock()) + 1, kind="subscription.resumed")
        self.assertEqual(self.user()["tier"], "max")

    def test_refund_and_chargeback_records(self):
        account(self.c, self.svc)
        txn, end = self.buy("max")
        now = int(self.clock())
        self.hook(fp.event("adjustment.created", fp.adjustment("adj_1", txn["id"], "ctm_01test1", "sub_01test1", status="pending_approval", amount=799), now))
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM audit WHERE action = 'refund'"), 0, "only approved adjustments count")
        self.hook(fp.event("adjustment.updated", fp.adjustment("adj_1", txn["id"], "ctm_01test1", "sub_01test1", amount=799), now + 1))
        self.hook(fp.event("adjustment.updated", fp.adjustment("adj_1", txn["id"], "ctm_01test1", "sub_01test1", amount=799), now + 2))
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM audit WHERE action = 'refund'"), 1, "once per adjustment")
        self.assertEqual(self.user()["tier"], "max", "subscription state comes from the subscription events")
        self.hook(fp.event("adjustment.created", fp.adjustment("adj_2", txn["id"], "ctm_01test1", "sub_01test1", action="chargeback", amount=799), now + 3))
        self.assertIn("chargeback", self.svc.db.value("SELECT detail FROM audit WHERE action = 'refund' ORDER BY id DESC"))
        self.hook(fp.event("adjustment.created", fp.adjustment("adj_3", txn["id"], "ctm_01test1", action="credit"), now + 4))
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM audit WHERE action = 'refund'"), 2, "credits are not refunds")

    def test_lost_renewal_expires_and_delete_blocked(self):
        account(self.c, self.svc)
        _, end = self.buy("basic")
        r = self.c.post("https://livemap.test/app/account", {"action": "delete", "confirm": "DELETE", "password": "correct horse battery"})
        self.assertIn("cancel it first", r.body.decode().lower())
        self.clock.t = end + 3 * DAY
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "free")


# -------------------------------------------------------------------- gifts, seats
class GiftTests(PaddleBase):
    def gift(self, plan="max", months=3, names=("Steve_01",), waiver=True):
        form = {"plan": plan, "months": str(months), "terms": "1"}
        for i, n in enumerate(names):
            form[f"mc{i}"] = n
        if waiver:
            form["waiver"] = "1"
        r = self.c.post("https://account.test/pro/gift", form, page=f"https://account.test/pro/gift?plan={plan}")
        self.assertEqual(r.status, 303, r.body[:300])
        txn = self.txn_of(r)
        self.assertEqual(self.hook(fp.event("transaction.completed", fp.transaction(txn["id"], "ctm_g", None, txn["custom_data"], 799 * months), int(self.clock())))[0], 200)
        return txn

    def test_gift_is_one_off_purchase(self):
        account(self.c, self.svc)
        txn = self.gift("max", 3, ("Steve_01",))
        self.assertEqual(txn["items"], [{"price_id": "pri_g_p", "quantity": 3}], "one-off price x months")
        self.assertEqual(txn["custom_data"]["kind"], "gift")
        self.assertEqual(self.user()["tier"], "free", "a gift does not change the buyer's account")
        data = json.loads(self.app.handle(Request("GET", "livemap.test", "/api/v1/mc/steve_01", {}, {}, b"")).body)
        self.assertEqual(data["plan"], "max")
        self.assertEqual(self.svc.db.value("SELECT state FROM gifts"), "active")
        page = self.c.get(f"https://account.test/pro/pay?_ptxn={txn['id']}").body.decode()
        self.assertIn('data-success="https://account.test/pro/gifts?done=1"', page)
        self.clock.advance(90 * DAY + 1)
        self.svc.maintenance()
        self.assertIsNone(self.svc.mc_entitlement("Steve_01")["plan"])

    def test_gift_code_redemption_on_username(self):
        account(self.c, self.svc)
        self.gift("ultra", 1, ())
        g = self.svc.db.one("SELECT * FROM gifts")
        self.assertRegex(g["code"], r"^SMG(-[A-Z2-9]{4}){3}$")
        mail = next(m for m in self.svc.mailer.sent if "gift" in m["subject"].lower() or "Geschenk" in m["subject"])
        self.assertIn(g["code"], mail["text"])
        self.assertIn("/pro/redeem?code=", mail["text"])
        self.assertIn(g["code"], self.c.get("https://account.test/pro/gifts").body.decode())
        friend = Client(self.app, ip="192.0.2.90")          # receiver without an account
        page = friend.get(f"https://account.test/pro/redeem?code={g['code']}")
        self.assertEqual(page.status, 200)
        r = friend.post("https://account.test/pro/redeem", {"code": "SMG-AAAA-BBBB-CCCC", "mc0": "Alex_2"})
        self.assertEqual(r.status, 400)
        r = friend.post("https://account.test/pro/redeem", {"code": g["code"].lower(), "mc0": "Alex_2", "mc1": "Bea_3"})
        self.assertEqual(r.status, 200, r.body[:300])
        self.assertIn("Gift redeemed", r.body.decode())
        self.assertEqual(self.svc.mc_entitlement("alex_2")["plan"], "ultra")
        self.assertEqual(self.svc.mc_entitlement("Bea_3")["plan"], "ultra", "Ultra: 2 names")
        r = friend.post("https://account.test/pro/redeem", {"code": g["code"], "mc0": "Other"})
        self.assertEqual(r.status, 409, "once")
        self.assertNotIn(g["code"], self.c.get("https://account.test/pro/gifts").body.decode(), "code hidden after redemption")
        r = self.c.post("https://account.test/pro/gifts", {"gift": str(g["id"]), "mc0": "Notch"})
        self.assertIn("every 7 days", r.body.decode(), "7-day rule applies after redemption")

    def test_redeem_rate_limit_and_refunded_gift(self):
        account(self.c, self.svc)
        txn = self.gift("basic", 1, ())
        code = self.svc.db.value("SELECT code FROM gifts")
        self.hook(fp.event("adjustment.updated", fp.adjustment("adj_g", txn["id"], "ctm_g", amount=399), int(self.clock()) + 1))
        self.assertEqual(self.svc.db.value("SELECT state FROM gifts"), "refunded", "refunded gift ends")
        guess = Client(self.app, ip="192.0.2.91")
        self.assertEqual(guess.post("https://account.test/pro/redeem", {"code": code, "mc0": "Alex_2"}).status, 400)
        for _ in range(9):
            guess.post("https://account.test/pro/redeem", {"code": "SMG-AAAA-BBBB-CCCC", "mc0": "Alex_2"})
        self.assertEqual(guess.post("https://account.test/pro/redeem", {"code": "SMG-AAAA-BBBB-CCCC", "mc0": "Alex_2"}).status, 429)

    def test_gift_with_names_and_without_waiver(self):
        account(self.c, self.svc)
        self.gift("ultra", 1, ("Herobrine",), waiver=False)
        self.assertIsNone(self.svc.mc_entitlement("Herobrine")["plan"])
        self.clock.advance(14 * DAY + 1)
        self.assertEqual(self.svc.mc_entitlement("Herobrine")["plan"], "ultra")
        mail = next(m for m in self.svc.mailer.sent if "Geschenk" in m["subject"] or "gift" in m["subject"].lower())
        self.assertNotIn("/pro/redeem", mail["text"], "names given: no code paragraph")


class SeatTests(PaddleBase):
    def test_seats_and_flags_through_paddle(self):
        friend_c = Client(self.app, ip="192.0.2.77")
        account(friend_c, self.svc, "friend@example.com")
        account(self.c, self.svc)
        _, end = self.buy("max")
        self.assertEqual(self.svc.plan_flags(self.user())["vote_weight"], 2)
        self.svc.lite_add(self.user(), "friend@example.com")
        self.assertEqual(self.user("friend@example.com")["tier"], "basic")
        self.clock.t = end
        self.sub_event("pri_p_m", "canceled", end, kind="subscription.canceled")
        self.assertEqual(self.user("friend@example.com")["tier"], "free", "seat ends with the owner's subscription")

    def test_early_access_perk(self):
        account(self.c, self.svc)
        self.assertFalse(self.svc.has_perk(self.user(), "early_access"))
        self.buy("basic")
        self.assertTrue(self.svc.has_perk(self.user(), "early_access"))


class HostTests(unittest.TestCase):
    """Owner 19.11.: no fixed host names in the web code; account/livemap hosts only from the config or the environment."""

    def test_hosts_from_config_or_env_only(self):
        import os
        from smweb import config
        d = temp()
        try:
            saved = {k: os.environ.pop(k, None) for k in ("SMWEB_ACCOUNT_HOST", "SMWEB_LIVEMAP_HOST", "SMWEB_COOKIE_DOMAIN")}
            with self.assertRaises(config.ConfigError):
                config.load(None, data_dir=d)
            os.environ.update({"SMWEB_ACCOUNT_HOST": "account-dev.simplemods.de", "SMWEB_LIVEMAP_HOST": "livemap-dev.simplemods.de"})
            cfg = config.load(None, data_dir=d)
            self.assertEqual(cfg.url("account", "/pro"), "https://account-dev.simplemods.de/pro")
            self.assertEqual(cfg["cookie"]["domain"], "simplemods.de", "common parent domain")
            cfg = config.load(None, data_dir=d, hosts={"account": "account.simplelivemaps.test", "livemap": "livemap.simplelivemaps.test"})
            self.assertEqual(cfg.hosts["livemap"], "livemap.simplelivemaps.test", "config wins over env")
            self.assertEqual(config.load(None, data_dir=d, hosts={"account": "127.0.0.1:8790", "livemap": "127.0.0.1:8790"})["cookie"]["domain"], "")
        finally:
            for k, v in saved.items():
                os.environ.pop(k, None)
                if v is not None:
                    os.environ[k] = v
            shutil.rmtree(d, ignore_errors=True)

    def test_pages_use_configured_hosts(self):
        d = temp()
        try:
            svc, app, _ = make(d, hosts={"account": "account.x.test", "livemap": "maps.x.test"}, links={"site": "https://site.x.test/", "wiki": ""})
            body = Client(app).get("https://maps.x.test/").body.decode()
            self.assertIn('href="https://site.x.test/"', body)
            self.assertNotIn("simplemods.de/\"", body)
            self.assertNotIn("wiki.simplemods.de", body)
            body = json.dumps({"code": "AAAA-BBBB", "world": "0f8a7c1e-2b3d-4c5e-8f90-a1b2c3d4e5f6", "mod": "0.2.0"}).encode()
            r = app.handle(Request("POST", "maps.x.test", "/api/v1/connect", {}, {"content-type": "application/json"}, body))
            self.assertEqual(r.status, 404, r.body)
            self.assertIn("maps.x.test", json.loads(r.body)["message"], "unknown-code text names the configured livemap host")
            svc.db.close()
        finally:
            shutil.rmtree(d, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
