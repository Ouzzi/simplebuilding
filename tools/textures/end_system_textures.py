"""26.3-only end signal sprites and owner-style Astral Vault skin; deterministic --check."""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mc26_3/overlay/resources/assets/simplebuilding"


def images():
    out = {}
    palettes = {"nihilith": [(38, 65, 82), (83, 145, 158), (179, 220, 221)],
                "astralit": [(98, 48, 81), (177, 99, 141), (241, 175, 211)]}
    for channel, colors in palettes.items():
        for active in [False, True]:
            color = colors[2 if active else 1]
            powder = Image.new("RGBA", (16, 16))
            d = ImageDraw.Draw(powder)
            points = [(4, 6), (7, 4), (10, 6), (6, 9), (10, 10), (3, 11), (12, 3)]
            for x, y in points:
                d.rectangle((x, y, x + 1, y + 1), fill=color)
                d.point((x, y + 1), fill=colors[0])
            if channel == "astralit":
                d.line([(6, 7), (8, 5), (10, 7), (8, 9), (6, 7)], fill=color)
            else:
                d.rectangle((6, 5, 9, 8), outline=color)
                d.point((8, 6), fill=colors[2])
            out[f"block/{'astral' if channel == 'astralit' else 'nihil'}_redstone{'_active' if active else ''}.png"] = powder
            switch = Image.new("RGBA", (16, 16))
            d = ImageDraw.Draw(switch)
            d.rectangle((3, 4, 12, 12), fill=(41, 38, 51), outline=(92, 81, 112))
            d.rectangle((5, 6, 10, 10), fill=colors[0])
            d.line([(6, 9), (9, 6)] if active else [(6, 6), (9, 9)], fill=color, width=2)
            out[f"block/{channel}_switch{'_active' if active else ''}.png"] = switch
            lamp = Image.new("RGBA", (16, 16))
            d = ImageDraw.Draw(lamp)
            # New ornamentation is inset; no dark corner filling or painted sprite edge.
            d.rectangle((1, 1, 14, 14), fill=(34, 29, 47))
            d.rectangle((2, 2, 13, 13), outline=(83, 65, 105))
            if channel == "astralit":
                d.polygon([(7, 3), (12, 8), (8, 12), (3, 7)], fill=colors[0], outline=color)
                d.line([(7, 5), (10, 8), (8, 10)], fill=color)
            else:
                d.ellipse((4, 4, 11, 11), fill=colors[0], outline=color)
                d.rectangle((7, 6, 8, 9), fill=color)
            out[f"block/{channel}_lamp{'_active' if active else ''}.png"] = lamp
    owner = Image.open(ROOT / "src/main/resources/assets/simplebuilding/textures/entity/chest/enderite.png").convert("RGBA")
    vault = owner.copy()
    d = ImageDraw.Draw(vault)
    # Chest front UV: keep the owner's metal, rim, and latch, add an inset astral glyph.
    d.line([(18, 38), (21, 35), (24, 38), (21, 41), (18, 38)], fill=(87, 176, 183))
    d.point((21, 38), fill=(192, 241, 225))
    out["entity/chest/astral_vault.png"] = vault
    particle = Image.open(ROOT / "src/main/resources/assets/simplebuilding/textures/block/enderite_block.png").convert("RGBA")
    d = ImageDraw.Draw(particle)
    d.line([(5, 7), (7, 5), (9, 7), (7, 9), (5, 7)], fill=(87, 176, 183))
    out["block/astral_vault.png"] = particle
    return out


def models():
    def write(rel, data):
        p = ASSETS / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    for c in ["nihilith", "astralit"]:
        for kind in ["powder", "switch", "lamp"]:
            name = ("astral" if c == "astralit" else "nihil") + "_redstone" if kind == "powder" else c + "_" + kind
            variants = {}
            for power in range(16):
                variants[f"power={power}"] = {"model": f"simplebuilding:block/{name}" + ("_active" if power else "")}
            write(f"blockstates/{name}.json", {"variants": variants})
            for active in [False, True]:
                suffix = "_active" if active else ""
                tex = f"simplebuilding:block/{name}{suffix}"
                if kind == "lamp":
                    model = {"textures": {"all": tex, "particle": tex}, "elements": [{"from": [1, 0, 1], "to": [15, 14, 15], "faces": {f: {"texture": "#all", "uv": [1, 1, 15, 15]} for f in ["up", "down", "north", "south", "east", "west"]}}]}
                else:
                    height = 0.1 if kind == "powder" else 4
                    model = {"textures": {"all": tex, "particle": tex}, "elements": [{"from": [0, height, 0], "to": [16, height, 16], "faces": {"up": {"texture": "#all"}, "down": {"texture": "#all"}}}]}
                write(f"models/block/{name}{suffix}.json", model)
            # A readable inventory silhouette instead of a nearly invisible flat world plane.
            write(f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"simplebuilding:block/{name}"}})
            write(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"simplebuilding:item/{name}"}})
    write("blockstates/astral_vault.json", {"variants": {"": {"model": "simplebuilding:block/astral_vault"}}})
    write("models/block/astral_vault.json", {"textures": {"particle": "simplebuilding:block/astral_vault"}})
    write("models/item/astral_vault.json", {"parent": "minecraft:item/chest"})
    write("items/astral_vault.json", {"model": {"type": "minecraft:special", "base": "simplebuilding:item/astral_vault", "model": {"type": "minecraft:chest", "texture": "simplebuilding:astral_vault"}}})


def main():
    check = argparse.ArgumentParser()
    check.add_argument("--check", action="store_true")
    args = check.parse_args()
    art = images()
    failures = []
    for rel, image in art.items():
        p = ASSETS / "textures" / rel
        if args.check:
            if not p.exists() or Image.open(p).convert("RGBA").tobytes() != image.tobytes(): failures.append(rel)
        else:
            p.parent.mkdir(parents=True, exist_ok=True)
            image.save(p)
    if args.check:
        if failures: raise SystemExit("Outdated: " + ", ".join(failures))
        print(f"End systems: {len(art)} textures current")
        return
    models()
    preview = ROOT / "docs/previews"
    preview.mkdir(parents=True, exist_ok=True)
    old = [Image.open(ROOT / "src/main/resources/assets/simplebuilding/textures/item" / name).convert("RGBA") for name in ["nihilith_shard.png", "astralit_dust.png"]]
    sheets = old + [art[f"block/{('astral' if c == 'astralit' else 'nihil') + '_redstone' if k == 'powder' else c + '_' + k}.png"] for c in ["nihilith", "astralit"] for k in ["powder", "switch", "lamp"]]
    canvas = Image.new("RGBA", (256 * len(sheets), 256), (53, 53, 53))
    for i, sheet in enumerate(sheets): canvas.alpha_composite(sheet.resize((256, 256), Image.Resampling.NEAREST), (i * 256, 0))
    canvas.save(preview / "end-signals-16x.png")
    owner = Image.open(ROOT / "src/main/resources/assets/simplebuilding/textures/entity/chest/enderite.png").convert("RGBA")
    canvas = Image.new("RGBA", (2048, 1024), (53, 53, 53))
    for i, sheet in enumerate([owner, art["entity/chest/astral_vault.png"]]): canvas.alpha_composite(sheet.resize((1024, 1024), Image.Resampling.NEAREST), (i * 1024, 0))
    canvas.save(preview / "astral-vault-16x.png")
    print(f"End systems: {len(art)} textures, models and 16x old/new previews generated")


if __name__ == "__main__": main()
