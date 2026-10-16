"""Fail on a tag JSON with no values unless it is a documented, intentionally empty extension point (audit D-10)."""
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# tag id (namespace:type/path) -> who fills it / why it ships empty
EMPTY_BY_DESIGN = {
    "simplebuilding:block/dyeable_families": "opt-in list for datapacks/mods: extra families the color brush may recolor",
    "simplebuilding:mob_effect/potion_pad/public": "datapack opt-in: harmful effects that potion pads may still give to everyone",
    "simplecrucibles:block/heat_source/extreme": "filled by consumer mods; Simple Crucibles alone has none",
    "simplelib:item/hammers": "filled by a hammer mod; SimpleBuilding's sledgehammer is deliberately not in it (InWorldUpgradeTests)",
    "simplebeacons:block/strong_base_blocks": "modpack opt-in for extra Strong Beacon pyramid blocks (check_data.py asserts it is empty)",
}
TAG = re.compile(r"(?:^|/)data/([^/]+)/tags/(.+)\.json$")


def main():
    tracked = subprocess.check_output(["git", "ls-files"], cwd=ROOT, text=True).splitlines()
    empty = set()
    for name in tracked:
        m = TAG.search(name)
        if not m:
            continue
        if not json.loads((ROOT / name).read_text(encoding="utf-8")).get("values"):
            empty.add(f"{m.group(1)}:{m.group(2)}")
    unknown = sorted(empty - EMPTY_BY_DESIGN.keys())
    stale = sorted(EMPTY_BY_DESIGN.keys() - empty)
    for tag in unknown:
        print(f"ERROR empty tag not documented in tools/check_empty_tags.py: {tag}")
    for tag in stale:
        print(f"ERROR documented as empty but filled or gone: {tag}")
    if unknown or stale:
        sys.exit(1)
    print(f"EMPTY_TAGS_OK: {len(empty)} intentionally empty tags")


if __name__ == "__main__":
    main()
