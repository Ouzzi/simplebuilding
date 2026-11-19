"""Simple Mods Pro (N108): order flow, Stripe Checkout parameters, signed + idempotent webhooks with Stripe-CLI-like payloads,
entitlements, yearly schedule, up/downgrade, grace/expiry, withdrawal waiver, cancel button (section 312k BGB), early access.
No network: the Stripe API is tests/fake_stripe.FakeStripe."""
from __future__ import annotations

import json
import re
import shutil
import unittest
from pathlib import Path

from helpers import Client, account, make, temp
import fake_stripe as fs

from smweb.app import Request

SECRET = "whsec_unit_test_only"
PRICES = {"basic": {"month": "price_b_m", "year": "price_b_y"}, "pro": {"month": "price_p_m", "year": "price_p_y"},
          "ultra": {"month": "price_u_m", "year": "price_u_y"}}
DAY = 86400


class ProBase(unittest.TestCase):
    billing_enabled = True

    def setUp(self):
        self.dir = temp()
        self.svc, self.app, self.clock = make(self.dir)
        self.svc.cfg.billing = {"enabled": self.billing_enabled, "secret_key": "sk_test_unit", "webhook_secret": SECRET, "prices": PRICES,
                                "vat": {"mode": "small_business", "rate_percent": 19, "stripe_tax": False}, "grace_days": 7,
                                "withdrawal_days": 14, "terms_version": "t1"}
        self.fake = fs.FakeStripe(now=lambda: int(self.clock()))
        self.svc.stripe_api = self.fake
        self.c = Client(self.app)

    def tearDown(self):
        self.svc.db.close()
        shutil.rmtree(self.dir, ignore_errors=True)

    def hook(self, evt: dict, secret: str = SECRET, host: str = "livemap.test", ts: int | None = None, body: bytes | None = None):
        raw, sig = fs.signed(evt, secret, int(self.clock()) if ts is None else ts)
        r = self.app.handle(Request("POST", host, "/api/stripe/webhook", {}, {"stripe-signature": sig, "content-type": "application/json"},
                                    body if body is not None else raw))
        return r.status, json.loads(r.body or b"{}")

    def order(self, plan="basic", interval="month", waiver=True, c: Client | None = None):
        c = c or self.c
        form = {"plan": plan, "interval": interval, "terms": "1"}
        if waiver:
            form["waiver"] = "1"
        return c.post("https://account.test/pro/order", form, page=f"https://account.test/pro/order?plan={plan}&interval={interval}")

    def buy(self, plan="basic", interval="month", waiver=True, sub="sub_test_1", cus="cus_test_1"):
        """Whole flow as Stripe would drive it after the order button: checkout completed, subscription created, invoice paid."""
        r = self.order(plan, interval, waiver)
        self.assertEqual(r.status, 303, r.body[:400])
        session = self.fake.objects[r.header("location").rsplit("/", 1)[1]]
        now = int(self.clock())
        end = now + (365 if interval == "year" else 30) * DAY
        price = PRICES[plan][interval]
        meta = session["params"]["subscription_data"]["metadata"]
        self.assertEqual(self.hook(fs.event("checkout.session.completed", fs.checkout_session(session, cus, sub), now))[0], 200)
        self.assertEqual(self.hook(fs.event("customer.subscription.created", fs.subscription(sub, cus, price, "active", end, meta), now))[0], 200)
        self.assertEqual(self.hook(fs.event("invoice.paid", fs.invoice("in_test_" + sub[-4:], cus, sub, 399, end), now))[0], 200)
        return session, end

    def user(self, email="alex@example.com"):
        return self.svc.db.one("SELECT * FROM users WHERE email = ?", email)


class ProPagesTests(ProBase):
    def test_plans_prices_and_vat_note(self):
        body = self.c.get("https://account.test/pro").body.decode()
        for name, price in (("Basic", "3,99 €"), ("Pro", "7,99 €"), ("Ultra", "14,99 €")):
            self.assertIn(name, body)
        r = self.c.get("https://account.test/pro", headers={"accept-language": "de"}).body.decode()
        self.assertIn("3,99 €", r)
        self.assertIn("39,90 €", r, "yearly placeholder = 10 x monthly")
        self.assertIn("§ 19 UStG", r)
        self.assertIn("Frühzugang", r)
        self.svc.cfg.billing["vat"] = {"mode": "regular", "rate_percent": 19}
        self.assertIn("inkl. 19 % USt.", self.c.get("https://account.test/pro", headers={"accept-language": "de"}).body.decode())

    def test_cancel_link_on_every_page(self):
        for url in ("https://account.test/pro", "https://livemap.test/", "https://livemap.test/pricing", "https://account.test/login"):
            self.assertIn("/kuendigen", self.c.get(url).body.decode(), url)

    def test_order_needs_login_and_shows_summary(self):
        r = self.c.get("https://account.test/pro/order?plan=pro&interval=year")
        self.assertEqual(r.status, 303)
        self.assertIn("/login?next=", r.header("location"))
        account(self.c, self.svc)
        body = self.c.get("https://account.test/pro/order?plan=pro&interval=year&lang=de").body.decode()
        self.assertIn("zahlungspflichtig bestellen", body)
        self.assertIn("79,90 €", body)
        self.assertIn("danach monatlich zum Monatspreis", body)
        self.assertNotIn("checked", body, "no box ticked in advance")
        self.assertIn('name="terms" value="1" required', body)
        self.assertIn("Widerrufsbelehrung", body)

    def test_csp_allows_stripe_form_redirect(self):
        csp = self.c.get("https://account.test/pro").header("content-security-policy")
        self.assertIn("form-action 'self'", csp)
        self.assertIn("https://checkout.stripe.com", csp)
        self.assertIn("https://billing.stripe.com", csp)

    def test_livemap_pricing_links_to_order(self):
        body = self.c.get("https://livemap.test/pricing").body.decode()
        self.assertIn("/pro/order?plan=basic&amp;interval=month", body)
        account(self.c, self.svc)
        r = self.c.post("https://livemap.test/app/plan", {"tier": "max", "interval": "year"})
        self.assertEqual(r.header("location"), "https://account.test/pro/order?plan=pro&interval=year")


class OrderTests(ProBase):
    def test_terms_box_required(self):
        account(self.c, self.svc)
        r = self.c.post("https://account.test/pro/order", {"plan": "basic", "interval": "month"}, page="https://account.test/pro/order?plan=basic")
        self.assertEqual(r.status, 400)
        self.assertEqual(self.fake.calls, [])
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM orders"), 0)

    def test_checkout_parameters(self):
        u = account(self.c, self.svc)
        r = self.order("pro", "month", waiver=True)
        self.assertEqual(r.status, 303)
        self.assertTrue(r.header("location").startswith("https://checkout.stripe.com/"))
        method, path, data, idem = self.fake.calls[-1]
        self.assertEqual((method, path), ("POST", "checkout/sessions"))
        self.assertEqual(data["mode"], "subscription")
        self.assertEqual(data["line_items"][0]["price"], "price_p_m")
        self.assertEqual(data["customer_email"], "alex@example.com")
        self.assertEqual(data["client_reference_id"], str(u["id"]))
        self.assertEqual(data["subscription_data"]["metadata"]["plan"], "pro")
        self.assertIn("{CHECKOUT_SESSION_ID}", data["success_url"])
        self.assertIn("zahlungspflichtig", data["custom_text"]["submit"]["message"])
        self.assertNotIn("automatic_tax", data)
        o = self.svc.db.one("SELECT * FROM orders")
        self.assertEqual(idem, f"order-{o['id']}")
        self.assertEqual((o["plan"], o["interval"], o["price_cents"], o["waiver"], o["terms_version"]), ("pro", "month", 799, 1, "t1"))
        self.assertIn("digital content", o["waiver_text"])

    def test_stripe_tax_switch(self):
        account(self.c, self.svc)
        self.svc.cfg.billing["vat"] = {"mode": "regular", "stripe_tax": True}
        self.order()
        data = self.fake.calls[-1][2]
        self.assertEqual(data["automatic_tax"], {"enabled": True})
        self.assertEqual(data["billing_address_collection"], "required")

    def test_stripe_down_is_friendly(self):
        account(self.c, self.svc)
        self.fake.fail.add("checkout/sessions")
        r = self.order()
        self.assertEqual(r.status, 502)
        self.assertIn("nothing was charged", r.body.decode().lower())

    def test_billing_off_is_coming_soon(self):
        self.svc.cfg.billing["enabled"] = False
        account(self.c, self.svc)
        r = self.order()
        self.assertEqual(r.status, 503)
        self.assertEqual(self.fake.calls, [])
        self.assertEqual(self.hook(fs.event("invoice.paid", {}, 1))[0], 503)

    def test_unverified_cannot_order(self):
        account(self.c, self.svc, verify=False)
        self.assertEqual(self.order().status, 403)


class WebhookTests(ProBase):
    def test_monthly_flow_entitlement_mails_and_tier(self):
        account(self.c, self.svc)
        self.svc.mailer.sent.clear()
        self.buy("basic")
        u = self.user()
        self.assertEqual(u["tier"], "pro", "Basic unlocks the livemap tier Pro")
        self.assertEqual(u["billing_customer"], "cus_test_1")
        ent = self.svc.entitlement(u)
        self.assertEqual(ent["plan"], "basic")
        self.assertIn("early_access", ent["perks"])
        subjects = [m["subject"] for m in self.svc.mailer.sent]
        self.assertTrue(any("Order confirmation" in s or "Bestellbestätigung" in s for s in subjects), subjects)
        self.assertTrue(any("Invoice" in s or "Rechnung" in s for s in subjects), subjects)
        mail = next(m for m in self.svc.mailer.sent if "Simple Mods Pro Basic" in m["subject"])
        self.assertIn("/kuendigen", mail["text"])
        self.assertIn("§ 356", mail["text"])
        self.assertTrue(self.svc.has_perk(u, "early_access"))
        self.assertIn("Simple Mods Pro Basic", self.c.get("https://account.test/pro/manage").body.decode())

    def test_duplicate_event_has_no_effect(self):
        account(self.c, self.svc)
        self.buy()
        n = len(self.svc.mailer.sent)
        evt = fs.event("invoice.paid", fs.invoice("in_test_9999", "cus_test_1", "sub_test_1", 399, int(self.clock()) + 60 * DAY), int(self.clock()))
        self.assertEqual(self.hook(evt), (200, {"received": True}))
        self.assertEqual(self.hook(evt), (200, {"received": True, "duplicate": True}))
        self.assertEqual(len(self.svc.mailer.sent), n + 1, "one invoice mail only")
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM stripe_events WHERE id = ?", evt["id"]), 1)

    def test_forged_signatures_are_rejected(self):
        u = account(self.c, self.svc)
        evt = fs.event("customer.subscription.created", fs.subscription("sub_x", "cus_x", "price_u_m", "active", int(self.clock()) + 30 * DAY,
                                                                         {"user": str(u["id"]), "plan": "ultra"}), int(self.clock()))
        self.assertEqual(self.hook(evt, secret="whsec_attacker")[0], 400)
        self.assertEqual(self.hook(evt, ts=int(self.clock()) - 301)[0], 400, "replay older than 5 minutes")
        raw, sig = fs.signed(evt, SECRET, int(self.clock()))
        tampered = raw.replace(b'"price_u_m"', b'"price_u_y"')
        r = self.app.handle(Request("POST", "livemap.test", "/api/stripe/webhook", {}, {"stripe-signature": sig}, tampered))
        self.assertEqual(r.status, 400)
        for header in ({}, {"stripe-signature": "t=abc,v1=00"}, {"stripe-signature": f"t={int(self.clock())}"}, {"stripe-signature": "v1=" + "0" * 64}):
            self.assertEqual(self.app.handle(Request("POST", "livemap.test", "/api/stripe/webhook", {}, header, raw)).status, 400, header)
        self.assertEqual(self.user()["tier"], "free")
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM stripe_events"), 0)

    def test_valid_signature_with_secret_rotation(self):
        """Stripe sends several v1 values while a secret is rolled; one match is enough."""
        account(self.c, self.svc)
        evt = fs.event("customer.created", {"id": "cus_q"}, int(self.clock()))
        raw, sig = fs.signed(evt, SECRET, int(self.clock()))
        both = sig.replace("v1=", "v1=" + "a" * 64 + ",v1=")
        self.assertEqual(self.app.handle(Request("POST", "livemap.test", "/api/stripe/webhook", {}, {"stripe-signature": both}, raw)).status, 200)

    def test_webhook_also_on_account_host(self):
        self.assertEqual(self.hook(fs.event("customer.created", {"id": "cus_z"}, int(self.clock())), host="account.test")[0], 200)

    def test_out_of_order_events(self):
        account(self.c, self.svc)
        self.buy("pro")
        now = int(self.clock())
        meta = {"plan": "pro"}
        late = fs.event("customer.subscription.updated", fs.subscription("sub_test_1", "cus_test_1", "price_p_m", "canceled", now, meta), now + 10)
        early = fs.event("customer.subscription.updated", fs.subscription("sub_test_1", "cus_test_1", "price_p_m", "active", now + 30 * DAY, meta), now + 5)
        self.hook(late)
        self.hook(early)
        self.assertEqual(self.svc.db.value("SELECT status FROM subscriptions WHERE id = 'sub_test_1'"), "canceled")
        self.assertEqual(self.user()["tier"], "free")

    def test_failed_handler_lets_stripe_retry(self):
        account(self.c, self.svc)
        self.fake.fail.add("subscription_schedules")
        r = self.order("ultra", "year")
        session = self.fake.objects[r.header("location").rsplit("/", 1)[1]]
        evt = fs.event("checkout.session.completed", fs.checkout_session(session, "cus_y", "sub_y"), int(self.clock()))
        self.assertEqual(self.hook(evt)[0], 502)
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM stripe_events"), 0, "id forgotten -> Stripe retries")
        self.fake.fail.clear()
        self.assertEqual(self.hook(evt)[0], 200)
        self.assertIsNotNone(self.svc.db.value("SELECT schedule_id FROM subscriptions WHERE id = 'sub_y'"))
        self.assertEqual(sum(1 for m in self.svc.mailer.sent if "Ultra" in m["subject"]), 1, "order mail once")


class LifecycleTests(ProBase):
    def test_yearly_schedule_year_then_month(self):
        account(self.c, self.svc)
        self.buy("pro", "year", sub="sub_test_y")
        made = self.fake.posts("subscription_schedules")
        self.assertEqual(made[0], ("subscription_schedules", {"from_subscription": "sub_test_y"}))
        path, upd = made[1]
        self.assertEqual(upd["end_behavior"], "release")
        self.assertEqual(upd["phases"][0]["items"][0]["price"], "price_p_y")
        self.assertEqual(upd["phases"][1]["items"][0]["price"], "price_p_m")
        self.assertEqual(upd["phases"][1]["iterations"], 1)
        self.assertEqual(upd["phases"][0]["end_date"] - upd["phases"][0]["start_date"], 365 * DAY)
        self.assertEqual(self.user()["tier"], "max")
        # a replayed (new id) checkout event does not make a second schedule
        session = next(o for o in self.fake.objects.values() if o.get("object") == "checkout.session")
        self.hook(fs.event("checkout.session.completed", fs.checkout_session(session, "cus_test_1", "sub_test_y"), int(self.clock())))
        self.assertEqual(len(self.fake.posts("subscription_schedules")), 2)

    def test_upgrade_and_downgrade_via_portal(self):
        account(self.c, self.svc)
        _, end = self.buy("basic")
        now = int(self.clock())
        self.hook(fs.event("customer.subscription.updated", fs.subscription("sub_test_1", "cus_test_1", "price_u_m", "active", end), now + 1))
        self.assertEqual(self.user()["tier"], "ultra")
        self.assertEqual(self.svc.entitlement(self.user())["plan"], "ultra")
        self.hook(fs.event("customer.subscription.updated", fs.subscription("sub_test_1", "cus_test_1", "price_b_m", "active", end), now + 2))
        self.assertEqual(self.user()["tier"], "pro")
        actions = [r["detail"] for r in self.svc.db.all("SELECT detail FROM audit WHERE action = 'entitlement' ORDER BY id")]
        self.assertEqual(actions, ["- -> basic", "basic -> ultra", "ultra -> basic"])

    def test_portal_redirect(self):
        account(self.c, self.svc)
        self.buy()
        r = self.c.post("https://account.test/pro/manage", {"action": "portal"})
        self.assertEqual(r.status, 303)
        self.assertTrue(r.header("location").startswith("https://billing.stripe.com/"))
        self.assertEqual(self.fake.calls[-1][2]["customer"], "cus_test_1")
        r = self.c.post("https://account.test/pro/order", {"plan": "pro", "interval": "month", "terms": "1"}, page="https://account.test/pro/manage")
        self.assertEqual(r.header("location"), "https://account.test/pro/manage", "second subscription: manage instead")

    def test_past_due_grace_then_free(self):
        account(self.c, self.svc)
        _, end = self.buy("ultra")
        now = int(self.clock())
        self.hook(fs.event("invoice.payment_failed", fs.invoice("in_f", "cus_test_1", "sub_test_1", 1499, end + 30 * DAY, paid=False), now + 1))
        self.hook(fs.event("customer.subscription.updated", fs.subscription("sub_test_1", "cus_test_1", "price_u_m", "past_due", end), now + 2))
        self.assertTrue(any("fehlgeschlagen" in m["subject"] or "failed" in m["subject"] for m in self.svc.mailer.sent))
        self.clock.t = end + 6 * DAY
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "ultra", "still in the grace days")
        self.clock.t = end + 8 * DAY
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "free")
        self.assertIsNone(self.svc.entitlement(self.user()))
        self.assertTrue(any("beendet" in m["subject"] or "ended" in m["subject"] for m in self.svc.mailer.sent))

    def test_cancel_at_period_end_then_deleted(self):
        account(self.c, self.svc)
        _, end = self.buy("pro")
        now = int(self.clock())
        self.hook(fs.event("customer.subscription.updated", fs.subscription("sub_test_1", "cus_test_1", "price_p_m", "active", end, cancel_at_period_end=True), now + 1))
        self.assertEqual(self.user()["tier"], "max", "stays until the end of the paid month")
        self.clock.t = end
        self.hook(fs.event("customer.subscription.deleted", fs.subscription("sub_test_1", "cus_test_1", "price_p_m", "canceled", end), end))
        self.assertEqual(self.user()["tier"], "free")
        ended = [m for m in self.svc.mailer.sent if "beendet" in m["subject"] or "ended" in m["subject"]]
        self.svc.maintenance()
        self.assertEqual(len(ended), len([m for m in self.svc.mailer.sent if "beendet" in m["subject"] or "ended" in m["subject"]]), "end mail once")

    def test_lost_renewal_webhook_expires_by_maintenance(self):
        account(self.c, self.svc)
        _, end = self.buy("basic")
        self.clock.t = end + 1 * DAY
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "pro", "renewal buffer")
        self.clock.t = end + 3 * DAY
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "free")

    def test_without_waiver_perks_start_after_withdrawal_period(self):
        account(self.c, self.svc)
        self.buy("basic", waiver=False)
        u = self.user()
        self.assertEqual(u["tier"], "free")
        self.assertFalse(self.svc.has_perk(u, "early_access"))
        text = next(m["text"] for m in self.svc.mailer.sent if "Basic" in m["subject"])
        self.assertTrue("after the withdrawal period" in text or "nach Ablauf der Widerrufsfrist" in text, text)
        self.clock.advance(14 * DAY + 1)
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "pro")
        self.assertTrue(self.svc.has_perk(self.user(), "early_access"))

    def test_admin_tier_kept_without_subscription(self):
        account(self.c, self.svc)
        self.svc.set_tier(self.user(), "ultra")
        self.svc.maintenance()
        self.assertEqual(self.user()["tier"], "ultra")

    def test_delete_account_blocked_while_subscribed(self):
        account(self.c, self.svc)
        self.buy()
        r = self.c.post("https://livemap.test/app/account", {"action": "delete", "confirm": "DELETE", "password": "correct horse battery"})
        self.assertIn("cancel it first", r.body.decode().lower())
        self.assertIsNotNone(self.user())

    def test_year_notice_before_transition(self):
        account(self.c, self.svc)
        _, end = self.buy("pro", "year", sub="sub_test_y")
        self.clock.t = end - 40 * DAY
        self.svc.maintenance()
        self.svc.maintenance()
        notes = [m for m in self.svc.mailer.sent if "monatliche Laufzeit" in m["subject"] or "monthly soon" in m["subject"]]
        self.assertEqual(len(notes), 1)


class CancelButtonTests(ProBase):
    def test_page_without_login(self):
        r = self.c.get("https://account.test/kuendigen", headers={"accept-language": "de"})
        self.assertEqual(r.status, 200)
        body = r.body.decode()
        self.assertIn("Verträge hier kündigen", body)
        self.assertIn("Jetzt kündigen", body)
        self.assertIn("ordentlich", body.lower())

    def test_unknown_address_gets_receipt_without_revealing(self):
        r = self.c.post("https://account.test/kuendigen", {"email": "nobody@example.com", "kind": "ordinary", "contract": "pro"})
        self.assertEqual(r.status, 200)
        self.assertIn("Cancellation received", r.body.decode())
        self.assertEqual(self.svc.mailer.sent[-1]["to"], "nobody@example.com")
        self.assertEqual(self.fake.calls, [])
        self.assertEqual(self.svc.db.value("SELECT state FROM cancellations"), "received")

    def test_cancels_subscription_at_period_end_and_mails_both(self):
        account(self.c, self.svc)
        _, end = self.buy("pro")
        other = Client(self.app, ip="192.0.2.50")
        r = other.post("https://account.test/kuendigen", {"email": "ALEX@example.com", "kind": "ordinary"})
        self.assertEqual(r.status, 200)
        path, data = self.fake.posts("subscriptions/")[-1]
        self.assertEqual((path, data), ("subscriptions/sub_test_1", {"cancel_at_period_end": True}))
        row = self.svc.db.one("SELECT * FROM cancellations")
        self.assertEqual((row["state"], row["effective_at"]), ("scheduled", end))
        self.assertEqual(self.svc.mailer.sent[-1]["to"], "alex@example.com")
        self.assertRegex(self.svc.mailer.sent[-1]["text"], r"\d{2}:\d{2}:\d{2} UTC")
        self.assertEqual(self.user()["tier"], "max", "perks until the end of the paid period")

    def test_yearly_schedule_released_first(self):
        account(self.c, self.svc)
        self.buy("basic", "year", sub="sub_test_y")
        self.c.post("https://account.test/kuendigen", {"email": "alex@example.com", "kind": "ordinary"})
        paths = [p for m, p, d, i in self.fake.calls if m == "POST"]
        self.assertLess(paths.index(next(p for p in paths if p.endswith("/release"))), paths.index("subscriptions/sub_test_y"))

    def test_rate_limited(self):
        for _ in range(5):
            self.c.post("https://account.test/kuendigen", {"email": "spam@example.com", "kind": "ordinary"})
        self.assertEqual(self.c.post("https://account.test/kuendigen", {"email": "spam@example.com", "kind": "ordinary"}).status, 429)


class EarlyAccessTests(ProBase):
    def setUp(self):
        super().setUp()
        d = Path(self.dir) / "early"
        (d / "files").mkdir(parents=True)
        (d / "files" / "simplelib-fabric-1.2.0.jar").write_bytes(b"PK\x03\x04jar")
        (d / "index.json").write_text(json.dumps({"releases": [{"version": "1.2.0", "date": "2026-11-19", "public": False,
                                                                 "files": [{"name": "simplelib-fabric-1.2.0.jar", "size": 7}]}]}))
        self.svc.cfg.v["early_access"] = {"dir": str(d), "accel_prefix": ""}

    def test_login_and_perk_required(self):
        self.assertIn("/login?next=", self.c.get("https://account.test/early").header("location"))
        account(self.c, self.svc)
        body = self.c.get("https://account.test/early").body.decode()
        self.assertNotIn("simplelib-fabric-1.2.0.jar", body)
        self.assertIn("/pro", body)
        self.assertEqual(self.c.get("https://account.test/early/f/simplelib-fabric-1.2.0.jar").status, 403)

    def test_subscriber_downloads(self):
        account(self.c, self.svc)
        self.buy()
        self.assertIn("simplelib-fabric-1.2.0.jar", self.c.get("https://account.test/early").body.decode())
        r = self.c.get("https://account.test/early/f/simplelib-fabric-1.2.0.jar")
        self.assertEqual((r.status, r.body), (200, b"PK\x03\x04jar"))
        self.assertIn("attachment", r.header("content-disposition"))
        self.assertEqual(self.c.get("https://account.test/early/f/other-1.0.jar").status, 404, "only listed files")
        self.assertEqual(self.c.get("https://account.test/early/f/..%2Findex.json").status, 404)
        self.svc.cfg.v["early_access"]["accel_prefix"] = "/_early/"
        r = self.c.get("https://account.test/early/f/simplelib-fabric-1.2.0.jar")
        self.assertEqual((r.header("x-accel-redirect"), r.body), ("/_early/simplelib-fabric-1.2.0.jar", b""))

    def test_developer_always(self):
        u = account(self.c, self.svc)
        with self.svc.db.tx() as c:
            c.execute("UPDATE users SET role = 'developer' WHERE id = ?", (u["id"],))
        self.assertEqual(self.c.get("https://account.test/early/f/simplelib-fabric-1.2.0.jar").status, 200)


if __name__ == "__main__":
    unittest.main()
