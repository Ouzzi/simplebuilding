"""When the next full gate is due (owner 13.11., TESTPLAN41): counter file /root/gate/full-due.json.

    python tools/testrunner/gate_due.py --file F check [--mods a,b]   # prints the reason and exits 0 when due, else 1
    python tools/testrunner/gate_due.py --file F record-delta <ref> --mods a,b
    python tools/testrunner/gate_due.py --file F record-full <ref>
    python tools/testrunner/gate_due.py --file F status               # "Voll fällig in N Bündeln / M Mods"

The bundle gate runs in delta mode; a full gate (all targets + standalone) is due after DELTAS green delta gates
since the last full one, or once MODS different modules changed since then (counted cumulatively). Shared code
and ``--full`` force it anyway (sb-gate.sh). File: {"deltas": n, "mods": m, "mod_ids": [...], "last_full_ref": ref}.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

DELTAS = 7
MODS = 15


def load(path: Path) -> dict:
    try:
        d = json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        d = {}
    ids = sorted(set(d.get("mod_ids", [])))
    return {"deltas": int(d.get("deltas", 0)), "mods": len(ids), "mod_ids": ids, "last_full_ref": d.get("last_full_ref")}


def save(path: Path, state: dict) -> None:
    state = dict(state, mod_ids=sorted(set(state["mod_ids"])))
    state["mods"] = len(state["mod_ids"])
    tmp = Path(str(path) + ".tmp")
    tmp.write_text(json.dumps(state, indent=1) + "\n", encoding="utf-8")
    tmp.replace(path)


def due(state: dict, new_mods: list[str] = ()) -> str | None:
    """Why a full gate is due before this delta (None when it is not): the delta about to run would be number
    deltas+1, so after DELTAS deltas the next gate is full; the mods count includes the ones this change touches."""
    if state["deltas"] >= DELTAS:
        return f"{state['deltas']} delta gates since the last full gate"
    mods = set(state["mod_ids"]) | set(new_mods)
    if len(mods) >= MODS:
        return f"{len(mods)} mods changed since the last full gate"
    return None


def record_delta(state: dict, mods: list[str]) -> dict:
    return dict(state, deltas=state["deltas"] + 1, mod_ids=sorted(set(state["mod_ids"]) | set(mods)))


def record_full(ref: str) -> dict:
    return {"deltas": 0, "mods": 0, "mod_ids": [], "last_full_ref": ref}


def status(state: dict) -> str:
    n = max(0, DELTAS - state["deltas"])
    m = max(0, MODS - len(state["mod_ids"]))
    return "Voll fällig: jetzt" if not n or not m else f"Voll fällig in {n} Bündeln / {m} Mods"


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("--file", type=Path, default=Path("/root/gate/full-due.json"))
    p.add_argument("cmd", choices=["check", "record-delta", "record-full", "status"])
    p.add_argument("ref", nargs="?")
    p.add_argument("--mods", default="")
    a = p.parse_args(argv)
    mods = [m for m in a.mods.split(",") if m and m != "-"]
    state = load(a.file)
    if a.cmd == "check":
        why = due(state, mods)
        print(f"due: {why}" if why else "not due: " + status(state))
        return 0 if why else 1
    if a.cmd == "status":
        print(status(state), f"(deltas {state['deltas']}/{DELTAS}, mods {len(state['mod_ids'])}/{MODS}, "
              f"last full {state['last_full_ref'] or '-'})")
        return 0
    if not a.ref:
        p.error(f"{a.cmd} needs a ref")
    save(a.file, record_delta(state, mods) if a.cmd == "record-delta" else record_full(a.ref))
    print(status(load(a.file)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
