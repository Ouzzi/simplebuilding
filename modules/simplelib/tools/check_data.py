"""Check SimpleLib's generated resources, bilingual names and wiki coverage without writing."""
import json
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = "simplelib"


def main():
    generated = subprocess.run([sys.executable, str(MODULE / "tools/gen_resources.py"), "--check"],
                               capture_output=True, text=True, encoding="utf-8")
    if generated.returncode:
        print(generated.stdout + generated.stderr, end="")
        return 1
    errors = []
    lang = {l: json.loads((MODULE / f"shared/resources/assets/{NS}/lang/{l}.json").read_text(encoding="utf-8"))
            for l in ("en_us", "de_de")}
    for a, b in (("en_us", "de_de"), ("de_de", "en_us")):
        errors += [f"{a}: missing {k}" for k in sorted(lang[b].keys() - lang[a].keys())]
    notes = json.loads((MODULE / "wiki/manual.json").read_text(encoding="utf-8"))["notes"]
    blocks = {p.stem for p in (MODULE / f"generated/resources/assets/{NS}/blockstates").glob("*.json")}
    for block in sorted(blocks):
        if f"block.{NS}.{block}" not in lang["en_us"]:
            errors.append(f"no name for {block}")
        note = notes.get(f"{NS}:{block}")
        if not note or not note.get("en") or not note.get("de"):
            errors.append(f"no bilingual wiki note for {block}")
    for error in errors:
        print(error)
    if not errors:
        print(f"{NS}: resources, bilingual names and wiki notes valid")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
