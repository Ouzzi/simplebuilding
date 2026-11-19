"""stripe_setup.py (N108): dry run without network, key never shown/written, live key refused without --live, idempotent
second run, changed amount -> new price, billing file mode 600. Stripe is tests/fake_stripe.FakeStripe."""
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

import fake_stripe as fs
from helpers import ROOT, temp

sys.path.insert(0, str(ROOT))
import stripe_setup  # noqa: E402

KEY = "sk_test_UNITSECRETVALUE0123456789"


class StripeSetupTests(unittest.TestCase):
    def setUp(self):
        self.dir = Path(temp())
        self.env = self.dir / "keys.env"
        self.env.write_text(f'export OTHER=1\nexport STRIPE_SECRET_KEY="{KEY}"\n')
        self.billing = self.dir / "billing.json"
        self.fake = fs.FakeStripe()
        self.keys_seen = []

    def tearDown(self):
        shutil.rmtree(self.dir, ignore_errors=True)

    def factory(self, key, version=None):
        self.keys_seen.append(key)
        return self.fake

    def run_setup(self, *args, env=None):
        lines = []
        rc = stripe_setup.main(["--env-file", str(env or self.env), "--billing-file", str(self.billing), *args],
                               api_factory=self.factory, out=lines.append)
        return rc, "\n".join(lines)

    def test_env_parsing(self):
        self.assertEqual(stripe_setup.read_env_var(self.env, "STRIPE_SECRET_KEY"), KEY)
        self.assertIsNone(stripe_setup.read_env_var(self.env, "MISSING"))
        self.assertIsNone(stripe_setup.read_env_var(self.dir / "nope.env", "STRIPE_SECRET_KEY"))

    def test_dry_run_is_default_and_silent_about_the_key(self):
        rc, out = self.run_setup()
        self.assertEqual(rc, 0)
        self.assertIn("dry run", out)
        self.assertIn("present, test mode", out)
        self.assertNotIn(KEY, out)
        self.assertNotIn("UNITSECRET", out)
        self.assertEqual(self.keys_seen, [], "no client made in a dry run")
        self.assertEqual(self.fake.calls, [])
        self.assertFalse(self.billing.exists())
        self.assertIn("smpro_basic_month 399 ct/month", out)
        self.assertIn("smpro_ultra_year 14990 ct/year", out)

    def test_live_key_needs_flag(self):
        live = self.dir / "live.env"
        live.write_text("STRIPE_SECRET_KEY=sk_live_X\n")
        rc, out = self.run_setup("--apply", env=live)
        self.assertEqual(rc, 2)
        self.assertIn("live key refused", out)
        self.assertEqual(self.fake.calls, [])

    def test_missing_key(self):
        rc, out = self.run_setup("--apply", env=self.dir / "none.env")
        self.assertEqual(rc, 2)

    def test_apply_creates_everything_once(self):
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc, out = self.run_setup("--apply")
        self.assertEqual(rc, 0, out)
        self.assertEqual(self.keys_seen, [KEY])
        self.assertNotIn(KEY, out + buf.getvalue())
        self.assertNotIn("whsec_", out)
        posts = [p for m, p, d, i in self.fake.calls if m == "POST"]
        self.assertEqual(posts.count("products"), 3)
        self.assertEqual(posts.count("prices"), 6)
        self.assertEqual(posts.count("webhook_endpoints"), 1)
        self.assertEqual(posts.count("billing_portal/configurations"), 1)
        ep = next(d for m, p, d, i in self.fake.calls if p == "webhook_endpoints" and m == "POST")
        self.assertIn("invoice.payment_failed", ep["enabled_events"])
        self.assertIn("checkout.session.completed", ep["enabled_events"])
        data = json.loads(self.billing.read_text())
        self.assertEqual(stat.S_IMODE(os.stat(self.billing).st_mode), 0o600)
        self.assertNotIn(KEY, self.billing.read_text(), "the secret key is never written")
        self.assertEqual((data["key_file"], data["key_var"], data["mode"]), (str(self.env), "STRIPE_SECRET_KEY", "test"))
        self.assertTrue(data["webhook_secret"].startswith("whsec_"))
        self.assertEqual(set(data["prices"]), {"basic", "pro", "ultra"})
        self.assertFalse(data["enabled"], "switching on stays the owner's step")
        first = dict(data)
        # second run: nothing new
        n = len(self.fake.calls)
        rc, out = self.run_setup("--apply")
        self.assertEqual(rc, 0)
        new_posts = [p for m, p, d, i in self.fake.calls[n:] if m == "POST"]
        self.assertNotIn("products", new_posts)
        self.assertNotIn("prices", new_posts)
        self.assertNotIn("webhook_endpoints", new_posts)
        self.assertNotIn("billing_portal/configurations", new_posts)
        data = json.loads(self.billing.read_text())
        self.assertEqual(data["prices"], first["prices"])
        self.assertEqual(data["webhook_secret"], first["webhook_secret"], "kept")

    def test_changed_amount_makes_new_price(self):
        self.run_setup("--apply")
        old = json.loads(self.billing.read_text())["prices"]["basic"]["month"]
        cfg = self.dir / "server.json"
        cfg.write_text(json.dumps({"plans": {"basic": {"price_month": 499}}}))
        rc, out = self.run_setup("--apply", "--config", str(cfg))
        self.assertEqual(rc, 0, out)
        new = json.loads(self.billing.read_text())["prices"]["basic"]["month"]
        self.assertNotEqual(old, new)
        self.assertFalse(self.fake.objects[old]["active"], "old price archived")
        self.assertEqual(self.fake.objects[new]["unit_amount"], 499)
        self.assertEqual(self.fake.objects[new]["lookup_key"], "smpro_basic_month")

    def test_service_reads_key_from_env_file(self):
        from smweb import config
        self.run_setup("--apply")
        b = config.load_billing(self.billing)
        self.assertEqual(b["secret_key"], KEY)


if __name__ == "__main__":
    unittest.main()
