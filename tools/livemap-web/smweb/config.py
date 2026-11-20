"""Service configuration: one JSON file outside the repo (default /etc/simplemods/livemap-web.json, mode 600).

Secrets (session/CSRF key, SMTP password, Stripe keys) live only there or in the data folder, never in the repository.
Every value has a safe default, so ``python3 -m smweb serve --data <dir>`` runs for local tests without a file.
"""
from __future__ import annotations

import copy
import json
import os
import secrets
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent                      # tools/livemap-web
REPO = ROOT.parents[1]
VIEWER = REPO / "modules/simplelivemap/shared/resources/assets/simplelivemap/web/index.html"

DEFAULTS = {
    "data_dir": "/srv/livemap-web",
    # No fixed host names in the code (owner 19.11.: dev = *-dev.simplemods.de, the livemap domain may move): the server config
    # or the environment (SMWEB_ACCOUNT_HOST, SMWEB_LIVEMAP_HOST, SMWEB_COOKIE_DOMAIN) names them; missing = start error.
    "hosts": {"account": "", "livemap": ""},
    "scheme": "https",
    "cookie": {"name": "__Secure-sm_session", "domain": None, "secure": True, "days": 30},
    "links": {"site": "", "wiki": ""},          # footer links (main website, wiki); empty = not shown
    "client_ip_header": "X-Client-IP",
    "viewer_path": str(VIEWER),
    "storage_quota_gb": 20,
    "disk_stop_percent": 90,
    "prep_slots": 4,
    "registration": True,
    "dev_outbox": False,
    "smtp": {"host": "", "port": 587, "user": "", "password": "", "from": "Simple Mods <no-reply@simplemods.de>", "starttls": True},
    "billing_file": str(ROOT / "billing.example.json"),
    "tiers_file": str(ROOT / "tiers.json"),
    "tiers": {},
    "plans_file": str(ROOT / "plans.json"),
    "plans": {},
    "early_access": {"dir": "", "accel_prefix": ""},
    # Owner answer part 2 (19.11.): binding contact = the operator's address (server config only, never in the repo); separate
    # forwarding addresses (e.g. Cloudflare Email Routing) are an option: mode "addresses". form_to = target of the contact form.
    "contact": {"mode": "operator", "operator_email": "", "addresses": {"allgemein": "", "datenschutz": "", "melden": ""}, "form_to": ""},
    "legal": {"imprint_url": "", "privacy_url": "", "terms_url": "", "withdrawal_url": "", "withdrawal_form_url": ""},
}


# Owner 19.11.2026 (N109 P001): "Pro" is called "Max"; livemap tiers Free/Basic/Max/Ultra. Old stored ids stay readable.
LIVEMAP_ALIAS = {"pro": "basic"}      # livemap tier (users.tier, entitlements.livemap_tier, admin input)
PLAN_ALIAS = {"pro": "max"}           # Simple Mods Pro plan (subscriptions/orders/entitlements.plan, billing prices, metadata)


def dim_folder(value: str) -> str:
    """Dimension id or tile folder -> folder name ("minecraft:the_end" -> "minecraft_the_end"; height folder ".h" -> base)."""
    v = (value or "").lower()
    if v.endswith(".h"):
        v = v[:-2]
    return v.replace(":", "_").replace("/", "_")


def dim_allowed(tier: dict, value: str) -> bool:
    """P011: Free = overworld + nether, Basic + end, Max/Ultra all mod dimensions ("*"). No list = all (old configs)."""
    allowed = tier.get("dimensions")
    if not allowed or "*" in allowed:
        return True
    return dim_folder(value) in {dim_folder(a) for a in allowed}


def _merge(base: dict, over: dict) -> dict:
    out = copy.deepcopy(base)
    for k, v in (over or {}).items():
        if isinstance(v, dict) and isinstance(out.get(k), dict):
            out[k] = _merge(out[k], v)
        else:
            out[k] = v
    return out


class ConfigError(ValueError):
    pass


def env_defaults(environ=None) -> dict:
    """Host names from the environment (used where the config file has none)."""
    env = os.environ if environ is None else environ
    out = {"hosts": {}, "cookie": {}}
    if env.get("SMWEB_ACCOUNT_HOST"):
        out["hosts"]["account"] = env["SMWEB_ACCOUNT_HOST"].strip()
    if env.get("SMWEB_LIVEMAP_HOST"):
        out["hosts"]["livemap"] = env["SMWEB_LIVEMAP_HOST"].strip()
    if env.get("SMWEB_COOKIE_DOMAIN"):
        out["cookie"]["domain"] = env["SMWEB_COOKIE_DOMAIN"].strip()
    return out


def parent_domain(host: str) -> str:
    """Cookie domain shared by account and livemap host: their common suffix ("account-dev.simplemods.de" +
    "livemap-dev.simplemods.de" -> "simplemods.de"); "" for IPs/localhost (host-only cookie)."""
    h = host.split(":")[0]
    if not h or h.replace(".", "").isdigit() or "." not in h:
        return ""
    return h.split(".", 1)[1] if h.count(".") >= 2 else h


class Config:
    def __init__(self, values: dict):
        self.v = values
        self.data = Path(values["data_dir"])
        self.hosts = values["hosts"]
        if not self.hosts.get("account") or not self.hosts.get("livemap"):
            raise ConfigError("hosts.account and hosts.livemap must be set (server config or SMWEB_ACCOUNT_HOST / SMWEB_LIVEMAP_HOST)")
        if values["cookie"].get("domain") is None:
            a, b = parent_domain(self.hosts["account"]), parent_domain(self.hosts["livemap"])
            values["cookie"]["domain"] = a if a == b else ""
        self.secret = self._secret()
        self.tiers = load_tiers(Path(values["tiers_file"]), values.get("tiers") or {})
        self.billing = load_billing(Path(values["billing_file"])) if values.get("billing_file") else {}
        self.plans = load_plans(Path(values["plans_file"]), values.get("plans") or {}, self.tiers)

    def __getitem__(self, key):
        return self.v[key]

    def get(self, key, default=None):
        return self.v.get(key, default)

    def url(self, which: str, path: str = "/") -> str:
        return f"{self.v['scheme']}://{self.hosts[which]}{path}"

    def _secret(self) -> bytes:
        """``secret`` from the file, else ``<data>/secret`` (made once, mode 600)."""
        s = self.v.get("secret")
        if isinstance(s, str) and len(s) >= 64:
            return bytes.fromhex(s)
        self.data.mkdir(parents=True, exist_ok=True)
        f = self.data / "secret"
        if not f.exists():
            fd = os.open(f, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            with os.fdopen(fd, "w") as out:
                out.write(secrets.token_hex(32))
        return bytes.fromhex(f.read_text().strip())


def load_json(path: Path) -> dict:
    try:
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        return data if isinstance(data, dict) else {}
    except (OSError, ValueError):
        return {}


def load_tiers(path: Path, overrides: dict) -> dict:
    base = load_json(path)
    tiers = base.get("tiers", {})
    for name, values in (overrides or {}).items():
        name = LIVEMAP_ALIAS.get(name, name)
        if name in tiers and isinstance(values, dict):
            tiers[name] = {**tiers[name], **values}
    base["tiers"] = tiers
    base.setdefault("order", list(tiers))
    return base


def env_value(path: str, var: str) -> str | None:
    try:
        for line in Path(path).read_text(encoding="utf-8").splitlines():
            line = line.strip().removeprefix("export ").strip()
            name, sep, value = line.partition("=")
            if sep and name.strip() == var:
                return value.strip().strip("'\"") or None
    except OSError:
        pass
    return None


def load_billing(path: Path) -> dict:
    """Billing file; a provider's secret key may instead live in an env file (``key_file`` + ``key_var``; Stripe at the top
    level -> ``secret_key``, Paddle in ``paddle`` -> ``paddle.api_key``), read at start, never logged."""
    b = load_json(path)
    if not b.get("secret_key") and b.get("key_file") and b.get("key_var"):
        v = env_value(b["key_file"], b["key_var"])
        if v:
            b["secret_key"] = v
    p = b.get("paddle")
    if isinstance(p, dict) and not p.get("api_key") and p.get("key_file") and p.get("key_var"):
        v = env_value(p["key_file"], p["key_var"])
        if v:
            p["api_key"] = v
    return b


def load_plans(path: Path, overrides: dict, tiers: dict) -> dict:
    """Simple Mods Pro catalogue (plans.json + server overrides). ``price_month`` null = price of the mapped livemap tier;
    ``price_year`` = ``yearly_months_paid`` x monthly unless set."""
    base = load_json(path)
    plans = base.get("plans", {})
    for name, values in (overrides or {}).items():
        name = PLAN_ALIAS.get(name, name)
        if isinstance(values, dict):
            plans[name] = {**plans.get(name, {}), **values}
    months = tiers.get("yearly_months_paid", 10)
    for name, p in plans.items():
        p.setdefault("name", name.title())
        p.setdefault("perks", [])
        p["livemap_tier"] = LIVEMAP_ALIAS.get(p.get("livemap_tier") or "", p.get("livemap_tier"))
        lt = tiers["tiers"].get(p.get("livemap_tier") or "") or {}
        if p.get("price_month") is None:
            p["price_month"] = int(lt.get("price_month", 0))
        if p.get("price_year") is None:
            p["price_year"] = p["price_month"] * months
    base["plans"] = plans
    base.setdefault("order", list(plans))
    base["order"] = [k for k in base["order"] if k in plans]
    base.setdefault("perks", {})
    base.setdefault("lite", {})
    for lite in base["lite"].values():
        lite["livemap_tier"] = LIVEMAP_ALIAS.get(lite.get("livemap_tier") or "", lite.get("livemap_tier") or "free")
        lite.setdefault("perks", ["livemap"])
    base.setdefault("lite_rules", {"reassign_lock_days": 30})
    return base


def load(path: str | None = None, **over) -> Config:
    values = _merge(_merge(DEFAULTS, env_defaults()), load_json(Path(path)) if path else {})
    values = _merge(values, over)
    return Config(values)
