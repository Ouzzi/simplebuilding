"""Check generated resources, bilingual names and wiki coverage without writing."""

from fnmatch import fnmatchcase
import json
from pathlib import Path
import subprocess
import sys

MODULE = Path(__file__).resolve().parents[1]
NS = "simplesandwiches"


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate JSON key: {key}")
        result[key] = value
    return result


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_keys)


def main():
    generated = subprocess.run([sys.executable, str(MODULE / "tools/gen_resources.py"), "--check"],
                               capture_output=True, text=True, encoding="utf-8")
    if generated.returncode:
        print(generated.stdout + generated.stderr, end="")
        return 1
    errors = []
    languages = {locale: read_json(MODULE / f"shared/resources/assets/{NS}/lang/{locale}.json")
                 for locale in ("en_us", "de_de")}
    for locale, other in (("en_us", "de_de"), ("de_de", "en_us")):
        for key in sorted(languages[other].keys() - languages[locale].keys()):
            errors.append(f"{locale}: missing key {key}")
    notes = read_json(MODULE / "wiki/manual.json")["notes"]
    assets = MODULE / f"generated/resources/assets/{NS}"
    blocks = {p.stem for p in (assets / "blockstates").glob("*.json")}
    items = {p.stem for p in (assets / "items").glob("*.json")}
    for name in sorted(blocks | items):
        identifier = f"{NS}:{name}"
        # ModItems uses useBlockDescriptionPrefix() for every BlockItem.
        key = f"{'block' if name in blocks else 'item'}.{NS}.{name}"
        for locale, lang in languages.items():
            if not isinstance(lang.get(key), str) or not lang[key].strip():
                errors.append(f"{locale}: missing name {key}")
        matching = [note for pattern, note in notes.items() if fnmatchcase(identifier, pattern)]
        for locale in ("en", "de"):
            if not any(isinstance(note.get(locale, {}).get("summary"), str)
                       and note[locale]["summary"].strip() for note in matching):
                errors.append(f"wiki {locale}: missing note {identifier}")
    if errors:
        print("\n".join(errors))
        return 1
    print(f"{NS}: generated resources, {len(items)} items, {len(blocks)} blocks, EN/DE and wiki valid")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, KeyError, TypeError, AttributeError) as error:
        print(f"{NS}: {error}", file=sys.stderr)
        raise SystemExit(1)
