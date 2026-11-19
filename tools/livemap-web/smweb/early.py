"""Early access (N108 perk 1): files of new versions for Simple Mods Pro subscribers, from ``<early_access.dir>/index.json``
(written by tools/release/early_access.py ``stage``). Only names listed there are served; nginx sends the bytes when
``accel_prefix`` is set (X-Accel-Redirect to an internal location), else the service streams them (dev/tests)."""
from __future__ import annotations

import json
import re
from pathlib import Path

NAME_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._+-]{0,150}\.(jar|zip)$")


class EarlyMixin:
    def early_dir(self) -> Path | None:
        d = (self.cfg.get("early_access") or {}).get("dir")
        return Path(d) if d else None

    def early_releases(self) -> list[dict]:
        d = self.early_dir()
        if d is None:
            return []
        try:
            data = json.loads((d / "index.json").read_text(encoding="utf-8"))
        except (OSError, ValueError):
            return []
        out = []
        for r in data.get("releases", []) if isinstance(data, dict) else []:
            if isinstance(r, dict):
                files = [f for f in r.get("files", []) if isinstance(f, dict) and NAME_RE.match(str(f.get("name", "")))]
                for f in files:
                    n = int(f.get("size") or 0)
                    f["size_text"] = f"{n / 1048576:.1f} MB" if n >= 1048576 else f"{n // 1024} KB"
                out.append({**r, "files": files})
        return out

    def early_file(self, name: str) -> Path | None:
        """Path of a listed early-access file (None when not listed or missing)."""
        d = self.early_dir()
        if d is None or not NAME_RE.match(name or ""):
            return None
        if not any(f["name"] == name for r in self.early_releases() for f in r["files"]):
            return None
        p = d / "files" / name
        return p if p.is_file() else None
