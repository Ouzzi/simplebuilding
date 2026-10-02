"""Validate tooltip JSON, short lines and bilingual continuation keys (no client)."""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate language key: {key}")
        result[key] = value
    return result


def main():
    tracked = subprocess.check_output(["git", "ls-files"], cwd=ROOT, text=True).splitlines()
    paths = [ROOT / name for name in tracked
             if name.startswith(("src/main/resources/", "mc26_3/overlay/resources/", "modules/"))
             and Path(name).name in ("en_us.json", "de_de.json")]
    files = {p: json.loads(p.read_text(encoding="utf-8"), object_pairs_hook=unique_keys) for p in paths}
    checked = 0
    for path, data in files.items():
        other = files[path.with_name("de_de.json" if path.name == "en_us.json" else "en_us.json")]
        for key, value in data.items():
            if "tooltip" not in key.lower() and key != "gui.simplebuilding.trim_stats.cap":
                continue
            assert key in other, f"Missing translation: {path}: {key}"
            assert max(map(len, value.splitlines()), default=0) <= 48, f"Long tooltip: {path}: {key}"
            if key.startswith("tooltip.") or key.startswith("simplebuilding.blueprint.tooltip."):
                assert "\n" not in value, f"Item tooltip needs separate components: {path}: {key}"
            checked += 1
        if path.is_relative_to(ROOT / "src/main/resources"):
            overlay = ROOT / "mc26_3/overlay/resources" / path.relative_to(ROOT / "src/main/resources")
            for key, value in data.items():
                if "tooltip" in key.lower():
                    assert files[overlay].get(key) == value, f"Tooltip overlay mismatch: {key}"
    print(f"TOOLTIP_TEXT_OK: {len(files)} JSON files, {checked} tooltip entries; locales, width and overlays valid")


if __name__ == "__main__":
    main()
