"""
Was die Zentrale von Vanilla braucht: die Handels-Pools (wie viele Vanilla-Angebote konkurrieren
mit einem Mod-Angebot, wie viele werden gezogen) und Anzeigenamen.

* Pools: aus dem Client-Jar (data/minecraft/trade_set + tags/villager_trade + villager_trade),
  zwischengespeichert in tools/devserver/data/vanilla-trade-pools-<linie>.json (versioniert, nur
  Ids, Zahlen und Chancen), damit die Rechner auch ohne Jar gleich rechnen.
* Namen: aus den Minecraft-Sprachdateien im Gradle-Asset-Cache, zwischengespeichert unter
  build/devserver/ (nicht versioniert - Mojang-Texte gehören nicht ins Repo). Ohne Cache: Id
  lesbar gemacht.
"""

from __future__ import annotations

import json
import zipfile
from pathlib import Path

LINE = "26.2"


def client_jar(version: str = LINE) -> Path:
    return Path.home() / ".gradle" / "caches" / "fabric-loom" / version / "minecraft-client.jar"


def pools_cache(repo: Path, version: str = LINE) -> Path:
    return repo / "tools" / "devserver" / "data" / f"vanilla-trade-pools-{version}.json"


def names_cache(repo: Path, version: str = LINE) -> Path:
    return repo / "build" / "devserver" / f"vanilla-names-{version}.json"


def _predicate_chance(trade: dict) -> tuple[float, str | None]:
    predicate = trade.get("merchant_predicate")
    if not predicate:
        return 1.0, None
    if isinstance(predicate, dict) and predicate.get("condition", "").endswith("random_chance"):
        chance = predicate.get("chance")
        if isinstance(chance, (int, float)):
            return float(chance), None
    kind = predicate.get("condition") if isinstance(predicate, dict) else str(predicate)
    return 1.0, str(kind)


def build_pools_from_jar(jar: Path) -> dict:
    out = {"generatedFrom": f"minecraft-client.jar {jar.parent.name}", "tradeSets": {}, "tags": {}}
    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        for name in sorted(names):
            if name.startswith("data/minecraft/trade_set/") and name.endswith(".json"):
                key = name[len("data/minecraft/trade_set/"):-5]
                data = json.loads(archive.read(name))
                out["tradeSets"][key] = {"amount": data.get("amount"), "trades": data.get("trades")}
            elif name.startswith("data/minecraft/tags/villager_trade/") and name.endswith(".json"):
                key = name[len("data/minecraft/tags/villager_trade/"):-5]
                entries = []
                for entry in json.loads(archive.read(name)).get("values", []):
                    ident = entry if isinstance(entry, str) else entry.get("id")
                    ns, path = ident.split(":", 1)
                    trade_file = f"data/{ns}/villager_trade/{path}.json"
                    chance, other = 1.0, None
                    if trade_file in names:
                        chance, other = _predicate_chance(json.loads(archive.read(trade_file)))
                    item = {"id": ident, "chance": chance}
                    if other:
                        item["condition"] = other
                    entries.append(item)
                out["tags"][key] = entries
    return out


def build_names() -> dict:
    """en_us (im Jar nicht enthalten) und de_de aus dem Asset-Cache von Loom."""
    base = Path.home() / ".gradle" / "caches" / "fabric-loom" / "assets"
    indexes = sorted((base / "indexes").glob(f"{LINE}-*.json")) if (base / "indexes").exists() else []
    out: dict[str, dict[str, str]] = {}
    if not indexes:
        return out
    objects = json.loads(indexes[-1].read_text(encoding="utf-8")).get("objects", {})
    for locale in ("en_us", "de_de"):
        entry = objects.get(f"minecraft/lang/{locale}.json")
        table = None
        if entry:
            digest = entry["hash"]
            path = base / "objects" / digest[:2] / digest
            if path.exists():
                table = json.loads(path.read_text(encoding="utf-8"))
        if table is None and locale == "en_us" and client_jar().exists():
            with zipfile.ZipFile(client_jar()) as archive:  # en_us liegt im Jar, nicht im Asset-Index
                try:
                    table = json.loads(archive.read("assets/minecraft/lang/en_us.json"))
                except KeyError:
                    table = None
        if table is None:
            continue
        for key, text in table.items():
            parts = key.split(".")
            if len(parts) == 3 and parts[0] in ("item", "block", "enchantment", "entity") and parts[1] == "minecraft":
                out.setdefault(f"{parts[0]}:{parts[2]}", {})[locale] = text
    return out


def load(repo: Path, refresh: bool = False) -> tuple[dict, dict, list[str]]:
    """-> (Pools, Namen, Hinweise)"""
    notes: list[str] = []
    cache = pools_cache(repo)
    jar = client_jar()
    pools = None
    if cache.exists() and not refresh:
        pools = json.loads(cache.read_text(encoding="utf-8"))
    elif jar.exists():
        pools = build_pools_from_jar(jar)
        cache.parent.mkdir(parents=True, exist_ok=True)
        cache.write_text(json.dumps(pools, indent=1, sort_keys=True) + "\n", encoding="utf-8")
        notes.append(f"Vanilla-Handelspools aus {jar} neu gelesen")
    else:
        pools = {"tradeSets": {}, "tags": {}}
        notes.append("Keine Vanilla-Handelspools: weder Cache noch Client-Jar - Angebotschancen zählen nur Mod-Einträge")

    names: dict = {}
    ncache = names_cache(repo)
    if ncache.exists() and not refresh:
        try:
            names = json.loads(ncache.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            names = {}
    if not names:
        try:
            names = build_names()
        except Exception as err:  # pragma: no cover - Cache kaputt: Ids bleiben lesbar
            notes.append(f"Vanilla-Namen nicht lesbar: {err}")
            names = {}
        if names:
            ncache.parent.mkdir(parents=True, exist_ok=True)
            ncache.write_text(json.dumps(names, ensure_ascii=False), encoding="utf-8")
        else:
            notes.append("Keine Vanilla-Namen (Gradle-Asset-Cache fehlt) - Vanilla-Ids werden lesbar gemacht angezeigt")
    return pools, names, notes
