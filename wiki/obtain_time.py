"""
Time to the k-th piece from loot sources, from per-event piece distributions and scenario rates.

The wiki does not know how fast a player finds chests. Everything that is a rate (openings per
hour of a table) is a scenario INPUT that the reader edits in the wiki; the defaults only copy the
values tools/devserver/sbdev/params.py already marks as assumptions. This module builds the
event part, which is read from the mod's loot pools, and holds the reference maths. index.html
(`timeStats`) computes the same formula in JavaScript; wiki/tests/test_obtain_time.py and
wiki/tests/time_ui.cjs pin both to the same values.

Model: a table is opened as a Poisson process with rate r (per hour). One opening gives Y pieces
of the item with pmf q (q[0] = nothing). Openings that give at least one piece form a Poisson
process with rate lam = r (1 - q0) and jump pmf h[m] = q[m] / (1 - q0). Several tables add up:
lam = sum lam_i, h = sum lam_i h_i / lam. With S(t) pieces by time t:

    P(T_k > t) = P(S(t) < k) = sum_{n<k} Pois(n; lam t) * P(H_1 + .. + H_n < k)
    E[T_k]     = (1 / lam) * sum_{n<k} P(H_1 + .. + H_n < k)

Every jump is at least 1, so n < k and the sums are finite. Median and 90 % quantile solve
P(T_k > t) = 0.5 and 0.1 by bisection. Piece counts above K_MAX are folded into K_MAX (too few,
so the time is slightly too long, never too short); k is limited to K_MAX.
"""

from __future__ import annotations

import math

K_MAX = 24

# Table -> prerequisite tags read from the table id (dimension / structure), not from hours.
REQUIRES = (
    ("nether", ("chests/bastion_", "chests/nether_bridge")),
    ("end", ("chests/end_city",)),
    ("trial", ("chests/trial_chambers/",)),
)
# Prerequisite -> earliest route stage (ids of ERAS in sbdev/params.py; their ORDER is the route).
REQUIRE_STAGE = {"village": "village", "nether": "nether", "end": "endcities", "trial": "trial", "brewing": "brewing"}
ERA_NAMES = {
    "early": ("Early game, first base", "Early Game, erste Basis"),
    "village": ("Village & trading", "Dorf & Handel"),
    "diamond": ("Diamond age, enchanting table", "Diamantzeit, Zaubertisch"),
    "nether": ("Entering the Nether", "Nether-Einstieg"),
    "brewing": ("Brewing stand & potions", "Braustand & Tränke"),
    "trial": ("Diamond armour, trial chambers", "Diamantrüstung, Prüfungskammern"),
    "netherite": ("Late game, netherite", "Late Game, Netherit"),
    "dragon": ("Ender dragon defeated", "Enderdrache besiegt"),
    "endcities": ("End cities & elytra", "End-Städte & Elytren"),
    "wither": ("Wither, beacon, big builds", "Wither, Beacon, Großbauten"),
}


def _conv(a: list[float], b: list[float], size: int) -> list[float]:
    out = [0.0] * size
    for i, x in enumerate(a):
        if x:
            for j, y in enumerate(b):
                if i + j < size:
                    out[i + j] += x * y
    return out


def _fold(pmf: list[float]) -> list[float]:
    """Fold everything above K_MAX into K_MAX; pad to K_MAX + 1 entries."""
    out = (pmf + [0.0] * (K_MAX + 1))[: K_MAX + 1]
    out[K_MAX] += sum(pmf[K_MAX + 1:])
    return out


def _requires(table: str) -> list[str]:
    return [tag for tag, needles in REQUIRES if any(n in table for n in needles)]


def build_streams(sources: list[dict], roll_distribution) -> list[dict]:
    """One stream per (table, item): pieces per opening, all pools of the table convolved."""
    groups: dict[tuple, list[dict]] = {}
    for s in sources:
        groups.setdefault((s["table"], s["item"]), []).append(s)
    streams = []
    for (table, item), group in groups.items():
        if any(s["type"] not in ("chest", "vault", "fishing") or s.get("enchantment") or s.get("randomEnchant")
               or s.get("how") or s.get("victim") for s in group):
            continue
        pools: dict[int, list[dict]] = {}
        for s in group:
            pools.setdefault(s["pool"], []).append(s)
        total = [1.0]
        for entries in pools.values():
            roll: list[float] = [0.0] * (K_MAX + 1)
            roll[0] = 1.0 - sum(e["weight"] / e["totalWeight"] for e in entries)
            for e in entries:
                lo, hi = e["count"]
                for c in range(lo, hi + 1):
                    roll[min(c, K_MAX)] += e["weight"] / e["totalWeight"] / (hi - lo + 1)
            pool = [0.0] * (K_MAX + 1)
            power = [1.0] + [0.0] * K_MAX
            done = 0
            for n, prob in sorted(roll_distribution(entries[0]["rolls"])):
                while done < n:
                    power = _conv(power, roll, K_MAX + 1)
                    done += 1
                pool = [x + prob * y for x, y in zip(pool, power)]
            total = _conv(total, pool, K_MAX + 1)
        pmf = _fold(total)
        if pmf[0] >= 1.0:
            continue
        first = group[0]
        streams.append({"item": item, "table": table, "type": first["type"], "label": first["label"],
                        "gated": bool(first.get("configFlag")), "requires": _requires(table),
                        "pmf": [round(x, 12) for x in pmf]})
    streams.sort(key=lambda s: (s["item"], s["table"]))
    return streams


def scenario_defaults(params) -> dict:
    """Annahmen aus sbdev/params.py (dort als Annahme/Schätzung markiert), sonst nichts."""
    shares = {"vault.rareShare": 0.8, "vault.commonShare": 0.2}
    rates: dict[str, dict] = {}
    for skey, struct in params.STRUCTURES.items():
        for ckey, container in struct["containers"].items():
            for table, share in container["tables"].items():
                weight = shares[share] if isinstance(share, str) else share
                row = rates.setdefault(table, {"targeted": 0.0, "why": []})
                row["targeted"] += container["rate"] * weight
                row["why"].append(f"{struct['label']}: {container['why']}")
    factor = 0.22
    for row in rates.values():
        row["targeted"] = round(row["targeted"], 6)
        row["normal"] = round(row["targeted"] * factor, 6)
        row["why"] = "; ".join(row["why"])
    return {"origin": "tools/devserver/sbdev/params.py", "normalFactor": factor, "rates": rates,
            "eras": [{"id": e["id"], "en": ERA_NAMES[e["id"]][0], "de": ERA_NAMES[e["id"]][1]} for e in params.ERAS],
            "requireStage": REQUIRE_STAGE, "kMax": K_MAX}


# --- reference maths (the same formula as timeStats in index.html) ------------------------------

def combine(streams: list[tuple[float, list[float]]]) -> tuple[float, list[float]] | None:
    """[(openings per hour, pmf per opening)] -> (hit rate lam, jump pmf h[0..K_MAX]); None without hits."""
    lam, jump = 0.0, [0.0] * (K_MAX + 1)
    for rate, pmf in streams:
        hit = rate * (1.0 - pmf[0])
        if rate > 0 and hit > 0:
            lam += hit
            for m in range(1, K_MAX + 1):
                jump[m] += hit * pmf[m] / (1.0 - pmf[0])
    if lam <= 0:
        return None
    return lam, [x / lam for x in jump]


def _cum_jumps(h: list[float], k: int) -> list[float]:
    """G[n] = P(H_1 + .. + H_n < k) for n = 0..k-1."""
    g, power = [], [1.0] + [0.0] * (k - 1)
    for n in range(k):
        g.append(sum(power))
        power = _conv(power, h[:k], k)
    return g


def survival(lam: float, h: list[float], k: int, t: float) -> float:
    g, x = _cum_jumps(h, k), lam * t
    p, total = math.exp(-x), 0.0
    for n in range(k):
        total += p * g[n]
        p *= x / (n + 1)
    return total


def time_stats(lam: float, h: list[float], k: int) -> dict:
    g = _cum_jumps(h, k)
    out = {"mean": sum(g) / lam}
    for name, target in (("median", 0.5), ("q90", 0.1)):
        hi = 1.0 / lam
        while survival(lam, h, k, hi) > target:
            hi *= 2
        lo = 0.0
        for _ in range(100):
            mid = (lo + hi) / 2
            lo, hi = (mid, hi) if survival(lam, h, k, mid) > target else (lo, mid)
        out[name] = (lo + hi) / 2
    return out
