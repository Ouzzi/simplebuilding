"""3-way merge of the module registries (modules/modules.json, integration/enabled-mods.json) by id / by value.

Run from the repository root while a merge is open (MERGE_HEAD exists):
    python tools/ai/merge_helpers/jsonreg3.py
"""
import json, subprocess, sys
sys.path.insert(0, "tools")
from multimod import write

def show(ref, path):
    out = subprocess.run(["git", "show", f"{ref}:{path}"], capture_output=True, text=True, encoding="utf-8", check=True).stdout
    return json.loads(out)

def merge(base, ours, theirs, where=""):
    if ours == theirs:
        return ours
    if ours == base:
        return theirs
    if theirs == base:
        return ours
    if isinstance(ours, dict) and isinstance(theirs, dict):
        base = base if isinstance(base, dict) else {}
        res = {}
        for k in list(ours) + [k for k in theirs if k not in ours]:
            if k in ours and k in theirs:
                res[k] = merge(base.get(k), ours[k], theirs[k], f"{where}/{k}")
            elif k in ours:
                if k in base and base[k] == ours[k]:
                    continue  # removed by theirs
                res[k] = ours[k]
            else:
                if k in base and base[k] == theirs[k]:
                    continue  # removed by ours
                res[k] = theirs[k]
        return res
    if isinstance(ours, list) and isinstance(theirs, list):
        base = base if isinstance(base, list) else []
        if all(isinstance(x, dict) and "id" in x for x in ours + theirs):
            bmap = {x["id"]: x for x in base}
            tmap = {x["id"]: x for x in theirs}
            res = [merge(bmap.get(x["id"]), x, tmap[x["id"]], f"{where}[{x['id']}]") if x["id"] in tmap else x for x in ours]
            res += [x for x in theirs if x["id"] not in {y["id"] for y in ours}]
            return res
        res = list(ours) + [x for x in theirs if x not in ours]
        return [x for x in res if not (x in base and (x not in ours or x not in theirs))]
    raise SystemExit(f"real conflict at {where}: {ours!r} vs {theirs!r}")

base_ref = subprocess.run(["git", "merge-base", "HEAD", "MERGE_HEAD"], capture_output=True, text=True, check=True).stdout.strip()
for path in ("modules/modules.json", "integration/enabled-mods.json"):
    merged = merge(show(base_ref, path), show("HEAD", path), show("MERGE_HEAD", path), path)
    write(__import__("pathlib").Path(path), merged)
    print("merged", path)
