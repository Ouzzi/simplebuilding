"""paddle_setup.py (N108 addendum 4): dry run without network, key never shown/written, sandbox first (production needs --live,
a key of the other environment is refused), idempotent second run, changed amount -> new price + old archived, no discounts,
billing file mode 600 with the paddle section. Paddle is tests/fake_paddle.FakePaddle."""
from __future__ import annotations

import io
import json
import os
import shutil
import stat
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path

import fake_paddle as fp
from helpers import ROOT, temp

sys.path.insert(0, str(ROOT))
import paddle_setup  # noqa: E402

KEY = "pdl_sdbx_apikey_UNITSECRETVALUE0123456789"
URL = "https://livemap-dev.example.test/api/paddle/webhook"


class PaddleSetupTests(unittest.TestCase):
    def setUp(self):
        self.dir = Path(temp())
        self.env = self.dir / "keys.env"
        self.env.write_text(f'export OTHER=1\nexport PADDLE_API_KEY="{KEY}"\n')
        self.billing = self.dir / "billing.json"
        self.fake = fp.FakePaddle()
        self.made = []

    def tearDown(self):
        shutil.rmtree(self.dir, ignore_errors=True)

    def factory(self, key, environment):
        self.made.append((key, environment))
        return self.fake

    def run_setup(self, *args, env=None, url=URL):
        lines = []
        argv = ["--env-file", str(env or self.env), "--billing-file", str(self.billing), *args]
        if url:
            argv += ["--webhook-url", url]
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = paddle_setup.main(argv, api_factory=self.factory, out=lines.append)
        return rc, "\n".join(lines) + buf.getvalue()

    def test_dry_run_is_default_and_silent_about_the_key(self):
        rc, out = self.run_setup()
        self.assertEqual(rc, 0)
        self.assertIn("dry run", out)
        self.assertIn("environment sandbox", out)
        self.assertIn("present (sandbox)", out)
        self.assertNotIn("UNITSECRET", out)
        self.assertEqual(self.made, [])
        self.assertEqual(self.fake.calls, [])
        self.assertFalse(self.billing.exists())
        self.assertIn("smpro_basic_month 399 ct/month", out)
        self.assertIn("smpro_ultra_year_trial 14990 ct/year trial 14 d", out)
        self.assertIn("smpro_max_gift 799 ct/once", out)

    def test_production_needs_live_and_matching_key(self):
        rc, out = self.run_setup("--apply", "--environment", "production")
        self.assertEqual(rc, 2)
        self.assertIn("production refused", out)
        rc, out = self.run_setup("--apply", "--environment", "production", "--live")
        self.assertEqual(rc, 2)
        self.assertIn("key belongs to sandbox", out)
        live = self.dir / "live.env"
        live.write_text("PADDLE_API_KEY=pdl_live_apikey_X\n")
        rc, out = self.run_setup("--apply", env=live)
        self.assertEqual(rc, 2, "a live key against the sandbox")
        self.assertEqual(self.fake.calls, [])
        self.assertEqual(self.made, [])

    def test_missing_key_or_url(self):
        self.assertEqual(self.run_setup("--apply", env=self.dir / "none.env")[0], 2)
        os.environ.pop("SMWEB_LIVEMAP_HOST", None)
        rc, out = self.run_setup("--apply", url=None)
        self.assertEqual(rc, 2)
        self.assertIn("webhook URL unknown", out)
        cfg = self.dir / "server.json"
        cfg.write_text(json.dumps({"hosts": {"account": "account-dev.example.test", "livemap": "livemap-dev.example.test"}}))
        rc, out = self.run_setup("--config", str(cfg), url=None)
        self.assertIn("webhook: https://livemap-dev.example.test/api/paddle/webhook", out, "host from the server config")

    def test_apply_creates_everything_once(self):
        rc, out = self.run_setup("--apply", "--client-token", "test_public_token")
        self.assertEqual(rc, 0, out)
        self.assertEqual(self.made, [(KEY, "sandbox")])
        self.assertNotIn(KEY, out)
        self.assertNotIn("pdl_ntfset_", out)
        posts = [p for m, p, d in self.fake.calls if m == "POST"]
        self.assertEqual(posts.count("products"), 3)
        self.assertEqual(posts.count("prices"), 15, "month, year, 2 trials, gift per plan")
        self.assertEqual(posts.count("notification-settings"), 1)
        self.assertFalse([p for p in posts if p.startswith("discounts")], "P074: no discounts are made")
        prices = [d for m, p, d in self.fake.calls if m == "POST" and p == "prices"]
        self.assertTrue(all(d["tax_mode"] == "internal" for d in prices), "final prices incl. VAT")
        trial = next(d for d in prices if d["custom_data"]["sm_key"] == "smpro_basic_month_trial")
        self.assertEqual(trial["trial_period"], {"interval": "day", "frequency": 14})
        gift = next(d for d in prices if d["custom_data"]["sm_key"] == "smpro_max_gift")
        self.assertNotIn("billing_cycle", gift, "one-off price")
        self.assertEqual((gift["unit_price"]["amount"], gift["quantity"]["maximum"]), ("799", 12))
        ntf = next(d for m, p, d in self.fake.calls if m == "POST" and p == "notification-settings")
        self.assertEqual(ntf["destination"], URL)
        for e in ("subscription.created", "subscription.canceled", "transaction.completed", "transaction.payment_failed", "adjustment.updated"):
            self.assertIn(e, ntf["subscribed_events"])
        data = json.loads(self.billing.read_text())
        self.assertEqual(stat.S_IMODE(os.stat(self.billing).st_mode), 0o600)
        self.assertNotIn(KEY, self.billing.read_text(), "the API key is never written")
        p = data["paddle"]
        self.assertEqual((p["key_file"], p["key_var"], p["environment"], p["client_token"]), (str(self.env), "PADDLE_API_KEY", "sandbox", "test_public_token"))
        self.assertTrue(p["webhook_secret"].startswith("pdl_ntfset_"))
        self.assertEqual(set(p["prices"]), {"basic", "max", "ultra"})
        self.assertEqual(set(p["prices"]["max"]), {"month", "year", "month_trial", "year_trial"})
        self.assertEqual(set(p["gift_prices"]), {"basic", "max", "ultra"})
        self.assertEqual(data["provider"], "paddle")
        self.assertFalse(data["enabled"], "switching on stays the owner's step")
        first = dict(p)
        n = len(self.fake.calls)
        rc, out = self.run_setup("--apply")
        self.assertEqual(rc, 0)
        self.assertEqual([p for m, p, d in self.fake.calls[n:] if m in ("POST", "PATCH")], [], "second run: nothing new")
        p = json.loads(self.billing.read_text())["paddle"]
        self.assertEqual((p["prices"], p["gift_prices"], p["webhook_secret"]), (first["prices"], first["gift_prices"], first["webhook_secret"]))
        # the service reads the key the same way
        from smweb import config
        self.assertEqual(config.load_billing(self.billing)["paddle"]["api_key"], KEY)

    def test_changed_amount_makes_new_price(self):
        self.run_setup("--apply")
        old = json.loads(self.billing.read_text())["paddle"]["prices"]["basic"]["month"]
        cfg = self.dir / "server.json"
        cfg.write_text(json.dumps({"plans": {"basic": {"price_month": 499}}}))
        rc, out = self.run_setup("--apply", "--config", str(cfg))
        self.assertEqual(rc, 0, out)
        new = json.loads(self.billing.read_text())["paddle"]["prices"]["basic"]["month"]
        self.assertNotEqual(old, new)
        self.assertEqual(self.fake.objects[old]["status"], "archived")
        self.assertEqual(self.fake.objects[new]["unit_price"]["amount"], "499")

    def test_paddle_error_is_reported(self):
        self.fake.fail.add("products")
        rc, out = self.run_setup("--apply")
        self.assertEqual(rc, 1)
        self.assertIn("Paddle answered 500", out)
        self.assertFalse(self.billing.exists())


if __name__ == "__main__":
    unittest.main()
