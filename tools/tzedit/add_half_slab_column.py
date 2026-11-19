"""N86-8 (owner 18.11.): the test centre's line areas get the column "half_slab" between "wall" and "slab" - read from the
block outwards: block, stairs, slab, half slab. One-off and idempotent; writes the jar copy
(src/main/resources/data/simplebuilding/testcentre/) and the wiki copy (wiki/data/testcentre/) of layout_lines_{stone,wood,metals}.

Cell = the half slab of the row's slab (simpleslabs:<slab path without _slab>_half_slab, from tools/textures/half_slab_materials.json);
a missing slab (#red) means a missing half slab too, every other cell is "#light" (does not exist).

  python3 tools/tzedit/add_half_slab_column.py [--check]
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
from tzedit_save import check_save, layout_text  # noqa: E402  same writer as the editor's save

DIRS = [ROOT / "src/main/resources/data/simplebuilding/testcentre", ROOT / "wiki/data/testcentre"]
AREAS = ("stone", "wood", "metals")
HALF = {m["slab"]: "simpleslabs:" + m["id"] for m in json.loads((ROOT / "tools/textures/half_slab_materials.json").read_text("utf-8"))}


def half_cell(slab: str) -> str:
    if slab == "#red":
        return "#red"
    return HALF.get(slab, "#light")


def migrate(data: dict) -> dict:
    if "half_slab" in data["columns"]:
        return data
    at = data["columns"].index("wall") + 1
    slab = data["columns"].index("slab")
    data["columns"].insert(at, "half_slab")
    for row in data["rows"]:
        row["cells"].insert(at, half_cell(row["cells"][slab]))
    return data


def main(argv: list[str]) -> int:
    stale = []
    for folder in DIRS:
        for area in AREAS:
            path = folder / f"layout_lines_{area}.json"
            text = layout_text(check_save(migrate(json.loads(path.read_text("utf-8")))))
            if path.read_text("utf-8") != text:
                stale.append(path)
                if "--check" not in argv:
                    path.write_text(text, "utf-8")
    if "--check" in argv and stale:
        print("half_slab column missing: " + ", ".join(str(p.relative_to(ROOT)) for p in stale))
        return 1
    print(("migrated " if stale else "up to date: ") + ", ".join(str(p.relative_to(ROOT)) for p in stale) if stale else "half_slab column: up to date")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
