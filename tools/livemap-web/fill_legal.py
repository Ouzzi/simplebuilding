#!/usr/bin/env python3
"""Fills the legal texts (docs/legal/*.md) with the operator data at deploy time (owner answer part 2, 19.11.2026).

The operator data lives only on the target host in a private JSON file (default /root/.config/sb-betreiber.json, mode 600);
it is never in the repository. This script reads it at runtime, writes the rendered texts OUTSIDE the repository and never
prints a value (only file names and counts). It fails when a required key is missing or any {{...}} stays unfilled.

Keys of the operator file (see docs/legal/README.md):
    name (required), anschrift (required, lines separated by \\n), email (required), kleinunternehmer (required, true/false),
    telefon, ustid, wirtschafts_id (optional)
Placeholders in the texts: {{betreiber.<key>}}, {{kontakt.allgemein}}, {{kontakt.datenschutz}}, {{kontakt.melden}};
blocks {{#wenn betreiber.<key>}} ... {{#sonst}} ... {{/wenn}} (not nested).
Payment provider variant (N108 addendum 3/4, one switch for all texts): --provider paddle (default; Paddle = merchant of record) or
stripe -> block keys zahlung.paddle, zahlung.stripe, zahlung.stripe_ku (Stripe + small business), zahlung.stripe_ust (Stripe + VAT). The three contact addresses are the operator
e-mail unless --contact-file names forwarding addresses (owner's option, e.g. Cloudflare Email Routing).

    python3 tools/livemap-web/fill_legal.py --operator /root/.config/sb-betreiber.json --out /srv/legal \\
        [--contact-file /etc/simplemods/contact.json] [--strip-internal] [--provider paddle|stripe]
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
FILES = ("impressum.md", "datenschutz.md", "agb.md", "widerruf.md", "kuendigung.md")
REQUIRED = ("name", "anschrift", "email", "kleinunternehmer")
OPTIONAL = ("telefon", "ustid", "wirtschafts_id")
CONTACT = ("allgemein", "datenschutz", "melden")
BLOCK = re.compile(r"\{\{#wenn ([a-z_.]+)\}\}(.*?)(?:\{\{#sonst\}\}(.*?))?\{\{/wenn\}\}", re.S)
VAR = re.compile(r"\{\{([a-z_]+\.[a-z_]+)\}\}")


class FillError(Exception):
    pass


PROVIDERS = ("paddle", "stripe")


def values(operator: dict, contact: dict | None, provider: str = "paddle") -> dict:
    missing = [k for k in REQUIRED if k not in operator or operator[k] in ("", None)]
    if missing:
        raise FillError("missing required key(s): " + ", ".join(missing))
    if not isinstance(operator["kleinunternehmer"], bool):
        raise FillError("kleinunternehmer must be true or false")
    unknown = [k for k in operator if k not in REQUIRED + OPTIONAL and not k.startswith("_")]
    if unknown:
        raise FillError("unknown key(s): " + ", ".join(sorted(unknown)))
    v = {f"betreiber.{k}": operator.get(k, "") for k in REQUIRED + OPTIONAL}
    for k in CONTACT:
        v[f"kontakt.{k}"] = (contact or {}).get(k) or operator["email"]
    if provider not in PROVIDERS:
        raise FillError("provider must be paddle or stripe")
    ku = operator["kleinunternehmer"] is True
    v.update({"zahlung.paddle": provider == "paddle", "zahlung.stripe": provider == "stripe",
              "zahlung.stripe_ku": provider == "stripe" and ku, "zahlung.stripe_ust": provider == "stripe" and not ku})
    return v


def render(text: str, v: dict, strip_internal: bool = False) -> str:
    def block(m):
        key, yes, no = m.group(1), m.group(2), m.group(3) or ""
        if key not in v:
            raise FillError(f"unknown block key {key}")
        val = v[key]
        return yes if (val is True or (isinstance(val, str) and val.strip())) else no
    out = BLOCK.sub(block, text)

    def var(m):
        key = m.group(1)
        if key not in v or isinstance(v[key], bool):
            raise FillError(f"unknown placeholder {{{{{key}}}}}")
        return str(v[key])
    out = VAR.sub(var, out)
    if "{{" in out or "}}" in out:
        raise FillError("unfilled placeholder left")
    if strip_internal:
        out = "\n".join(line for line in out.splitlines() if not line.startswith("<!-- intern:")) + "\n"
    return out


def main(argv=None, out=print) -> int:
    ap = argparse.ArgumentParser(description="fill docs/legal with the operator data (values are never printed)")
    ap.add_argument("--operator", default="/root/.config/sb-betreiber.json")
    ap.add_argument("--src", default=str(REPO / "docs" / "legal"))
    ap.add_argument("--out", required=True, help="target folder outside the repository")
    ap.add_argument("--contact-file", default=None, help='optional JSON {"allgemein": ..., "datenschutz": ..., "melden": ...}')
    ap.add_argument("--strip-internal", action="store_true", help="drop the internal '<!-- intern: ... -->' header lines")
    ap.add_argument("--provider", default="paddle", choices=PROVIDERS, help="payment provider variant of the texts (billing.provider)")
    a = ap.parse_args(argv)
    dest = Path(a.out).resolve()
    if dest == REPO or REPO in dest.parents:
        out("refused: the output folder must be outside the repository")
        return 2
    try:
        operator = json.loads(Path(a.operator).read_text(encoding="utf-8"))
        contact = json.loads(Path(a.contact_file).read_text(encoding="utf-8")) if a.contact_file else None
        if not isinstance(operator, dict) or (contact is not None and not isinstance(contact, dict)):
            raise FillError("operator/contact file must hold a JSON object")
        v = values(operator, contact, a.provider)
        rendered = {name: render((Path(a.src) / name).read_text(encoding="utf-8"), v, a.strip_internal) for name in FILES}
    except (OSError, ValueError) as ex:
        out(f"failed: {type(ex).__name__} (file missing or not JSON)")
        return 1
    except FillError as ex:
        out(f"failed: {ex}")
        return 1
    dest.mkdir(parents=True, exist_ok=True)
    for name, text in rendered.items():
        tmp = dest / (name + ".tmp")
        fd = os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o644)
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(text)
        os.replace(tmp, dest / name)
    out(f"filled {len(rendered)} texts into {dest} (values not shown; contact addresses: {'forwarding file' if contact else 'operator e-mail'}; "
        f"payment provider variant: {a.provider})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
