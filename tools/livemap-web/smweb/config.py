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
    "hosts": {"account": "account.simplemods.de", "livemap": "livemap.simplemods.de"},
    "scheme": "https",
    "cookie": {"name": "__Secure-sm_session", "domain": "simplemods.de", "secure": True, "days": 30},
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
    "legal": {"imprint_url": "", "privacy_url": "", "terms_url": ""},
}


def _merge(base: dict, over: dict) -> dict:
    out = copy.deepcopy(base)
    for k, v in (over or {}).items():
        if isinstance(v, dict) and isinstance(out.get(k), dict):
            out[k] = _merge(out[k], v)
        else:
            out[k] = v
    return out


class Config:
    def __init__(self, values: dict):
        self.v = values
        self.data = Path(values["data_dir"])
        self.hosts = values["hosts"]
        self.secret = self._secret()
        self.tiers = load_tiers(Path(values["tiers_file"]), values.get("tiers") or {})
        self.billing = load_json(Path(values["billing_file"])) if values.get("billing_file") else {}

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
        if name in tiers and isinstance(values, dict):
            tiers[name] = {**tiers[name], **values}
    base["tiers"] = tiers
    base.setdefault("order", list(tiers))
    return base


def load(path: str | None = None, **over) -> Config:
    values = _merge(DEFAULTS, load_json(Path(path)) if path else {})
    values = _merge(values, over)
    return Config(values)
