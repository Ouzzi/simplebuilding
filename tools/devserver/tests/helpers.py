"""Gemeinsames fuer die Tests der Balancing-Zentrale."""

from __future__ import annotations

import shutil
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
DEVSERVER = HERE.parent
REPO = DEVSERVER.parent.parent
sys.path.insert(0, str(DEVSERVER))

# Was die Zentrale liest - fuer Tests, die schreiben (Anwenden auf Handelsdateien), in einen
# Temp-Ordner kopiert, damit das echte Repo nie veraendert wird.
COPY = [
    "common/src/shared/java/com/simplebuilding/loot/ModLootTableModifications.java",
    "common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java",
    "common/src/shared/java/com/simplebuilding/config/ConfigOptions.java",
    "common/src/shared/java/com/simplebuilding/tweaks/TweaksConfig.java",
    "common/src/shared/java/com/simplebuilding/items/custom/RotatorItem.java",
    "src/main/resources/assets/simplebuilding/lang",
    "src/main/generated/wiki/items.json",
    "src/main/resources/data/simplebuilding/villager_trade",
    "src/main/resources/data/minecraft/tags/villager_trade",
    "src/main/generated/data/simplebuilding/recipe",
    "src/main/generated/data/simplebuilding/enchantment",
    "src/main/generated/data/simplebuilding/worldgen",
    "src/main/generated/data/simplebuilding/loot_table/blocks",
    "tools/devserver/data",
    "wiki/obtain_sources.py",
    "docs",
]


def temp_repo() -> Path:
    root = Path(tempfile.mkdtemp(prefix="bz-repo-"))
    for rel in COPY:
        src = REPO / rel
        dst = root / rel
        if not src.exists():
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        if src.is_dir():
            shutil.copytree(src, dst)
        else:
            shutil.copy2(src, dst)
    return root


def temp_dir(prefix: str = "bz-") -> Path:
    return Path(tempfile.mkdtemp(prefix=prefix))


_shared = {}


def shared_service():
    """Ein Service auf dem ECHTEN Repo (nur lesend) mit einer eigenen Temp-Ablage - einmal je Lauf."""
    from sbdev.service import Service
    if "service" not in _shared:
        _shared["service"] = Service(REPO, temp_dir("bz-store-"))
    return _shared["service"]


def fresh_store(service):
    """Dieselbe (teure) Auslese, aber eine neue leere Ablage."""
    from sbdev.store import Store
    service.store = Store(temp_dir("bz-store-"))
    return service
