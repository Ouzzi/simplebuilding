"""Inspect actual Forge distributables and their version-selected framework payload."""
import hashlib
import io
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[2]


def inspect():
    result = []
    for entry in json.loads((ROOT / "modules/modules.json").read_text())["modules"]:
        project = entry["projects"].get("forge")
        if not project:
            continue
        directory = ROOT.joinpath(*project.strip(":").split(":"))
        jars = [p for p in (directory / "build/libs").glob("*.jar") if not p.name.endswith("-sources.jar")]
        assert len(jars) == 1, f"Expected one current distributable for {entry['id']}: {jars}"
        jar = jars[0]
        with zipfile.ZipFile(jar) as archive:
            metadata = json.loads(archive.read("META-INF/jarjar/metadata.json"))
            framework = [item for item in metadata["jars"] if item["identifier"]["group"] == "com.simplebuilding.framework"]
            assert len(framework) == 1, f"Missing/doubled framework in {jar}"
            assert framework[0]["version"]["artifactVersion"] == "0.1.2"
            with zipfile.ZipFile(io.BytesIO(archive.read(framework[0]["path"]))) as library:
                for name in ("Protection", "Protection$Check", "Protection$Target", "CosmeticIntensity", "CosmeticIntensity$Level",
                             "TransformHints", "TransformHints$Query", "TransformHints$Hint"):
                    assert f"com/simplebuilding/framework/api/{name}.class" in library.namelist(), (jar, name)
            assert not any(name.startswith("me/shedaniel/clothconfig2/") for name in archive.namelist()), f"Fake Cloth GUI in {jar}"
            assert "META-INF/mods.toml" in archive.namelist()
            if "forgeNativeConfig" in (directory / "build.gradle").read_text():
                prefix = f"com/simplebuilding/forgenative/{entry['id']}/"
                for name in ("ConfigRegistration.class", "ConfigBuilder.class"):
                    assert prefix + name in archive.namelist(), (jar, name)
                locales = [json.loads(archive.read(f"assets/simplemods/lang/{locale}.json")) for locale in ("en_us", "de_de")]
                assert locales[0].keys() == locales[1].keys(), f"Incomplete native screen translations: {jar}"
            if entry["id"] == "simplebuilding":
                assert "com/simplebuilding/common/SimplebuildingBootstrap.class" in archive.namelist()
                assert "com/simplebuilding/api/FrameworkProtection.class" in archive.namelist()
                assert "META-INF/services/com.simplebuilding.api.WorldPermissions$Bridge" in archive.namelist()
        result.append({"id": entry["id"], "jar": str(jar.relative_to(ROOT)), "sha256": hashlib.sha256(jar.read_bytes()).hexdigest()})
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    inspect()
