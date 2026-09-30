"""3-way merge of a JSON file in a git conflict: python jsonmerge3.py <path>. Writes the merged file."""
import json
import subprocess
import sys

path = sys.argv[1]
conflicts = []


def show(stage):
    out = subprocess.run(["git", "show", f":{stage}:{path}"], capture_output=True, text=True, encoding="utf-8")
    return json.loads(out.stdout) if out.returncode == 0 else None


def merge_list(base, ours, theirs, where):
    if ours == base:
        return theirs
    if theirs == base or ours == theirs:
        return ours
    def keyed(lst):
        return all(isinstance(x, dict) and "id" in x for x in lst)
    if keyed(ours) and keyed(theirs) and keyed(base):
        # Merge by id: an entry both sides edited must stay ONE entry (the old whole-item
        # comparison turned it into two diverged copies).
        bmap = {x["id"]: x for x in base}
        tmap = {x["id"]: x for x in theirs}
        result = []
        for x in ours:
            i = x["id"]
            if i in tmap:
                result.append(merge(bmap.get(i), x, tmap[i], f"{where}[{i}]"))
            elif i in bmap:  # removed by theirs
                if x != bmap[i]:
                    conflicts.append(f"{where}[{i}]: removed by theirs, changed by ours (kept ours)")
                    result.append(x)
            else:
                result.append(x)
        ours_ids = [x["id"] for x in ours]
        for k, x in enumerate(theirs):
            i = x["id"]
            if i in ours_ids or i in bmap:
                continue
            pos = len(result)
            for p in range(k - 1, -1, -1):
                idx = [j for j, y in enumerate(result) if y["id"] == theirs[p]["id"]]
                if idx:
                    pos = idx[0] + 1
                    break
            result.insert(pos, x)
        return result
    result = list(ours)
    base_keys = [json.dumps(x, sort_keys=True) for x in base]
    theirs_keys = [json.dumps(x, sort_keys=True) for x in theirs]
    # removals (and old halves of changes) made by theirs
    for i, k in enumerate(base_keys):
        if k not in theirs_keys:
            for j, x in enumerate(result):
                if json.dumps(x, sort_keys=True) == k:
                    del result[j]
                    break
    # additions made by theirs, placed after their predecessor
    for i, k in enumerate(theirs_keys):
        if k in base_keys:
            continue
        if any(json.dumps(x, sort_keys=True) == k for x in result):
            continue
        pos = 0
        for p in range(i - 1, -1, -1):
            idx = [j for j, x in enumerate(result) if json.dumps(x, sort_keys=True) == theirs_keys[p]]
            if idx:
                pos = idx[0] + 1
                break
        result.insert(pos, theirs[i])
    return result


def merge(base, ours, theirs, where="$"):
    if ours == theirs:
        return ours
    if ours == base:
        return theirs
    if theirs == base:
        return ours
    if isinstance(ours, dict) and isinstance(theirs, dict):
        base = base if isinstance(base, dict) else {}
        out = {}
        for k in list(ours.keys()) + [k for k in theirs.keys() if k not in ours]:
            b = base.get(k)
            if k not in theirs:
                if k in base:  # deleted by theirs
                    if ours.get(k) != b:
                        conflicts.append(f"{where}.{k}: deleted by theirs, changed by ours (kept ours)")
                        out[k] = ours[k]
                    continue
                out[k] = ours[k]
            elif k not in ours:
                if k in base:
                    if theirs[k] != b:
                        conflicts.append(f"{where}.{k}: deleted by ours, changed by theirs (kept theirs)")
                        out[k] = theirs[k]
                    continue
                out[k] = theirs[k]
            else:
                out[k] = merge(b, ours[k], theirs[k], f"{where}.{k}")
        return out
    if isinstance(ours, list) and isinstance(theirs, list):
        return merge_list(base if isinstance(base, list) else [], ours, theirs, where)
    conflicts.append(f"{where}: both changed a scalar (kept theirs): ours={str(ours)[:80]!r} theirs={str(theirs)[:80]!r}")
    return theirs


def near_pairs(node, where="$", acc=None):
    acc = set() if acc is None else acc
    if isinstance(node, dict):
        for k, v in node.items():
            if k != "sources":
                near_pairs(v, f"{where}.{k}", acc)
    elif isinstance(node, list):
        strs = [x for x in node if isinstance(x, str) and len(x) > 60]
        for i in range(len(strs)):
            for j in range(i + 1, len(strs)):
                if strs[i][:45] == strs[j][:45] and strs[i] != strs[j]:
                    acc.add((where, strs[i], strs[j]))
    return acc


b, o, t = show(1), show(2), show(3)
merged = merge(b, o, t)
for where, a, bb in sorted(near_pairs(merged) - near_pairs(o) - near_pairs(t)):
    conflicts.append(f"{where}: merge produced near-duplicates: {a[:90]!r} / {bb[:90]!r}")
with open(path, "w", encoding="utf-8", newline="\n") as f:
    f.write(json.dumps(merged, indent=2, ensure_ascii=False) + "\n")
print(f"merged {path}; {len(conflicts)} scalar/structural conflicts")
for c in conflicts:
    print("  !", c)
