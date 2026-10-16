"""Generate SimpleBuilding's 26.3 crucible data (plan PLAN-CRUCIBLE-P5-2026-10-05); --check compares only.

Writes into mc26_3/overlay/resources the Simple Crucibles tag additions that name SimpleBuilding items (extreme-heat
recipes, iron rod as crucible handle). The Enderite crucible and barrel (split stage 5) and soul lava with all buckets
(stage 5b) are Simple Crucibles' own: their resources live in modules/simplecrucibles (shared/resources and
tools/gen_resources.py).
"""

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "mc26_3/overlay/resources"
NS = "simplebuilding"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
CRLF, LF = b"\r\n", b"\n"


def files():
    out = {}
    # Simple Crucibles' tags (extreme-heat recipes, iron rod as crucible handle).
    out["data/simplecrucibles/tags/item/needs_extreme_heat.json"] = {"values": [f"{NS}:layered_raw_enderite", f"{NS}:cracked_diamond"]}
    out["data/simplecrucibles/tags/item/crucible_handles.json"] = {"values": [f"{NS}:iron_pole"]}
    return out


def render(data):
    return (json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    wanted = {OUT / path: render(data) for path, data in files().items()}
    if args.check:
        bad = [p for p, b in wanted.items() if not p.exists() or p.read_bytes().replace(CRLF, LF) != b]
        for p in bad:
            print(f"out of date: {p.relative_to(ROOT)}")
        return 1 if bad else 0
    for p, b in wanted.items():
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(b)
    print(f"wrote {len(wanted)} files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
