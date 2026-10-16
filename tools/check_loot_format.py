"""Find loot tables still in the pre-26 JSON format (26.3 ignores it silently).

Vanilla 26.x: a pool/entry carries ONE "condition" object and a "modifier" (list or object), every predicate/function
is {"type": ...} ("condition" may also name a predicate by id). The old lists "conditions"/"functions" and
{"function": "minecraft:x"} parse without an error but do nothing (found with CrucibleBlankBlock: every pool dropped, set_count was ignored).

  python3 tools/check_loot_format.py            list offenders (exit 0)
  python3 tools/check_loot_format.py --strict   exit 1 unless every offender is in tools/loot_format_baseline.txt (file absent = none allowed)
  python3 tools/check_loot_format.py --write-baseline
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASELINE = ROOT / "tools" / "loot_format_baseline.txt"
OLD_KEYS = ("conditions", "functions")


def old_format(node):
    """True if any pool/entry/predicate in the tree uses the pre-26 shape."""
    if isinstance(node, dict):
        if any(k in node for k in OLD_KEYS) and ("entries" in node or "name" in node or "rolls" in node or "type" in node):
            return True
        if isinstance(node.get("function"), str):
            return True
        return any(old_format(v) for v in node.values())
    if isinstance(node, list):
        return any(old_format(v) for v in node)
    return False


def offenders():
    tracked = subprocess.check_output(["git", "ls-files"], cwd=ROOT, text=True).splitlines()
    # On 26.3 mc26_3/overlay/resources and mc26_3/generated (the 26.3 datagen) hide src/main/generated per path.
    prefixes = ("mc26_3/overlay/resources/", "mc26_3/generated/")
    shadow = {n[len(p):] for n in tracked for p in prefixes if n.startswith(p)}
    removed_file = ROOT / "mc26_3/generated/removed-on-26.3.txt"
    removed = {l.strip() for l in removed_file.read_text(encoding="utf-8").splitlines()} if removed_file.exists() else set()
    out = []
    for name in tracked:
        # 26.3 sources only; the 1.21.11 copy and the shared 26.2 datagen use the old format correctly
        if "/loot_table/" not in name or not name.endswith(".json") or name.startswith("mc1_21_11/"):
            continue
        if name.startswith("src/main/generated/") and name[len("src/main/generated/"):] in shadow | removed:
            continue
        try:
            data = json.loads((ROOT / name).read_text(encoding="utf-8"))
        except ValueError:
            continue
        if old_format(data):
            out.append(name)
    return sorted(out)


def main():
    found = offenders()
    if "--write-baseline" in sys.argv:
        BASELINE.write_text("\n".join(found) + "\n", encoding="utf-8")
    known = set(BASELINE.read_text(encoding="utf-8").split()) if BASELINE.exists() else set()
    new = [n for n in found if n not in known]
    for name in found:
        print(("NEW  " if name in new else "OLD  ") + name)
    print(f"LOOT_FORMAT: {len(found)} tables in the pre-26 format, {len(new)} not in the baseline")
    if "--strict" in sys.argv and new:
        sys.exit(1)


if __name__ == "__main__":
    main()
