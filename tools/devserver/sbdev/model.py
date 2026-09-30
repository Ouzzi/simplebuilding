"""
Die Rechner: wie lange dauert es, 1 ... 6 Stück eines Items zu bekommen - und rückwärts: welche
Chance braucht es für eine Zielzeit.

Alles deterministisch (keine Simulation), in geschlossener Form:

* Jede Quelle ist ein Strom von Ereignissen (Kisten-Öffnungen, Händlerbesuche, Tötungen, Abbau)
  mit Rate r je Stunde. Pro Ereignis faellt eine zufaellige Anzahl Y des Items (auch 0), exakt aus
  den Pools berechnet (Würfe, Gewichte, Mengen).
* Mehrere Ströme zusammen und die Nullen herausgenommen ergeben einen zusammengesetzten
  Poisson-Prozess: Treffer mit Rate L = R x P(Y >= 1), je Treffer C = Y | Y >= 1 Stück.
* Zeit bis zum k-ten Stück T_k:  P(T_k > t) = Summe_{n<k} q_n Poisson(n; L t) mit
  q_n = P(C_1 + ... + C_n <= k - 1). Daraus Mittel E[T_k] = Summe q_n / L (exakt) und Median bzw.
  90 %-Wert per Bisektion. Mit C = 1 ist das die Gamma-Verteilung aus KERNE-SELTENHEIT.md
  (erster Treffer im Mittel 1/L, Median ln 2 / L).

Dorfbewohner sind kein Poisson-Strom (ein Dorfbewohner hat ein Angebot oder nicht): Suche nach einem
passenden Dorfbewohner (Rate = ausgebildete/h x Angebotschance) plus Auffüllungen. Rezepte
rechnen die Zeit für die noetigen Zutaten (die langsamste verfolgte Zutat zählt).
"""

from __future__ import annotations

import math
from functools import lru_cache

from . import params as P

KMAX = 6
INF = float("inf")


# ---------------------------------------------------------------------------
# Verteilungen (Liste p[0..K], p[K] = P(>= K))
# ---------------------------------------------------------------------------

def conv(a: list[float], b: list[float], K: int) -> list[float]:
    out = [0.0] * (K + 1)
    for i, x in enumerate(a):
        if x == 0.0:
            continue
        for j, y in enumerate(b):
            out[min(i + j, K)] += x * y
    return out


def power(a: list[float], n: int, K: int) -> list[float]:
    out = [1.0] + [0.0] * K
    for _ in range(n):
        out = conv(out, a, K)
    return out


def roll_distribution(rolls: dict) -> list[tuple[int, float]]:
    kind = rolls["type"]
    if kind == "exactly":
        return [(int(rolls["n"]), 1.0)]
    if kind == "uniform":
        lo, hi = int(rolls["min"]), int(rolls["max"])
        if hi < lo:
            return [(lo, 1.0)]
        return [(v, 1.0 / (hi - lo + 1)) for v in range(lo, hi + 1)]
    n, p = int(rolls["n"]), min(1.0, max(0.0, float(rolls["p"])))
    return [(k, math.comb(n, k) * p ** k * (1 - p) ** (n - k)) for k in range(n + 1)]


def uniform_count(lo: int, hi: int, K: int) -> list[float]:
    lo, hi = max(0, int(lo)), max(0, int(hi))
    if hi < lo:
        hi = lo
    out = [0.0] * (K + 1)
    for c in range(lo, hi + 1):
        out[min(c, K)] += 1.0 / (hi - lo + 1)
    return out


# ---------------------------------------------------------------------------
# Zusammengesetzter Poisson-Prozess
# ---------------------------------------------------------------------------

def _pois(n: int, x: float) -> float:
    if x <= 0:
        return 1.0 if n == 0 else 0.0
    return math.exp(-x + n * math.log(x) - math.lgamma(n + 1))


def compound_times(streams: list[tuple[float, list[float]]], ks: list[int]) -> dict:
    """
    streams: [(Rate je Stunde, Verteilung Y je Ereignis mit Länge K+1)], alle mit demselben K >= max(ks).
    -> {"hitRate", "mean": [...], "median": [...], "p90": [...]} (Stunden; inf = nie)
    """
    K = max(ks)
    R = sum(r for r, _ in streams if r > 0)
    nothing = {"hitRate": 0.0, "mean": [INF] * len(ks), "median": [INF] * len(ks), "p90": [INF] * len(ks)}
    if R <= 0:
        return nothing
    mix = [0.0] * (K + 1)
    for r, dist in streams:
        if r <= 0:
            continue
        for j in range(K + 1):
            mix[j] += r / R * (dist[j] if j < len(dist) else 0.0)
    hit = 1.0 - mix[0]
    if hit <= 1e-15:
        return nothing
    lam = R * hit
    C = [0.0] + [mix[j] / hit for j in range(1, K + 1)]
    out = {"hitRate": lam, "mean": [], "median": [], "p90": []}
    for k in ks:
        q = []
        acc = [1.0] + [0.0] * K
        for n in range(k):
            q.append(sum(acc[:k]))
            acc = conv(acc, C, K)

        def survival(t, q=q):
            return sum(qn * _pois(n, lam * t) for n, qn in enumerate(q))

        out["mean"].append(sum(q) / lam)
        out["median"].append(_solve(survival, 0.5, lam, k))
        out["p90"].append(_solve(survival, 0.1, lam, k))
    return out


def _solve(survival, level: float, lam: float, k: int) -> float:
    lo, hi = 0.0, max(1.0, 2.0 * k / lam)
    while survival(hi) > level:
        hi *= 2
        if hi > 1e9:
            return INF
    for _ in range(80):
        mid = (lo + hi) / 2
        if survival(mid) > level:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2


# ---------------------------------------------------------------------------
# Angebotschance im Handels-Pool (26.x: gezogen wird ohne Zurücklegen; faellt die Angebots-Chance
# aus, verschwindet das Angebot und es wird neu gezogen)
# ---------------------------------------------------------------------------

@lru_cache(maxsize=4)
def _gauss_legendre(n: int) -> tuple[tuple[float, ...], tuple[float, ...]]:
    xs, ws = [], []
    for i in range(1, n + 1):
        x = math.cos(math.pi * (i - 0.25) / (n + 0.5))
        for _ in range(100):
            p0, p1 = 1.0, x
            for k in range(2, n + 1):
                p0, p1 = p1, ((2 * k - 1) * x * p1 - (k - 1) * p0) / k
            dp = n * (x * p1 - p0) / (x * x - 1)
            dx = p1 / dp
            x -= dx
            if abs(dx) < 1e-15:
                break
        xs.append(x)
        ws.append(2 / ((1 - x * x) * dp * dp))
    return tuple(xs), tuple(ws)


def offer_probability(chance_self: float, others: list[float], draws: int) -> float:
    """
    P(das Angebot erscheint) = c * Integral_0^1 P(weniger als `draws` andere bestehen vor ihm) du.
    Zufaellige Reihenfolge = unabhaengige Ankunftszeiten u; ein anderes Angebot j liegt mit
    Wahrscheinlichkeit u vorn und besteht mit c_j. Der Integrand ist ein Polynom in u, Gauss-Legendre
    mit genug Stützstellen rechnet also exakt.
    """
    if chance_self <= 0 or draws <= 0:
        return 0.0
    if len(others) < draws:
        return chance_self
    n = max(16, len(others) // 2 + 2)
    xs, ws = _gauss_legendre(n)
    total = 0.0
    for x, w in zip(xs, ws):
        u = (x + 1) / 2
        dp = [1.0] + [0.0] * (draws - 1)  # P(genau m bestanden), m < draws
        for c in others:
            q = u * max(0.0, min(1.0, c))
            for m in range(draws - 1, -1, -1):
                dp[m] = dp[m] * (1 - q) + (dp[m - 1] * q if m > 0 else 0.0)
        total += w / 2 * sum(dp)
    return chance_self * total


# ---------------------------------------------------------------------------
# Kontext: effektive Werte
# ---------------------------------------------------------------------------

class Ctx:
    def __init__(self, snapshot: dict, effective, custom: dict | None = None):
        self.snap = snapshot
        self.custom = custom or {}  # Item -> Ids geplanter Quellen (source:<item>:<uid>)
        self.eff = effective  # id -> Wert (geplanter oder Mod-Wert)
        self.tables = {t["id"]: t for t in snapshot["loot"]["tables"]}
        self.trades = {t["id"]: t for t in snapshot["trades"]}
        self.recipes = snapshot.get("recipes", [])

    def v(self, vid, fallback):
        if not vid:
            return fallback
        got = self.eff(vid)
        return fallback if got is None else got

    def param(self, key, fallback=None):
        return self.v(f"param:{key}", fallback)

    def normal_factor(self):
        return float(self.param("normalFactor", 0.22))

    def rate(self, skey, ckey, item_key, mode):
        base = P.STRUCTURES[skey]["containers"][ckey]["rate"]
        targeted = self.param(f"rate.{skey}.{ckey}@{item_key}", None)
        if targeted is None:
            targeted = self.param(f"rate.{skey}.{ckey}", base)
        if mode == "targeted":
            special = f"param:rate.{skey}.{ckey}@{item_key}"
            return float(targeted), [special if self.eff(special) is not None else f"param:rate.{skey}.{ckey}"]
        normal = self.param(f"rateNormal.{skey}.{ckey}", None)
        if normal is not None:
            return float(normal), [f"param:rateNormal.{skey}.{ckey}"]
        # normal: allgemeine Rate (nicht die Item-Sonderrate) x Faktor
        general = self.param(f"rate.{skey}.{ckey}", base)
        return float(general) * self.normal_factor(), [f"param:rate.{skey}.{ckey}", "param:normalFactor"]


def matches(entry: dict, item_key: str) -> bool:
    if item_key.startswith("book:"):
        ench, level = item_key[5:].rsplit(":", 1)
        return entry.get("enchantment") == ench and str(entry.get("level")) == level
    return entry.get("item") == item_key and not entry.get("enchantment")


def pool_distribution(ctx: Ctx, pool: dict, item_key: str, K: int) -> tuple[list[float], float, list[str]]:
    """Verteilung der Stückzahl des Items aus EINER Pool-Ausführung, Erwartungswert, benutzte Wert-Ids."""
    used: list[str] = []
    rolls = dict(pool["rolls"])
    for field, vid in (rolls.get("ids") or {}).items():
        rolls[field] = ctx.v(vid, rolls.get(field))
        used.append(vid)
    if pool.get("rareCore"):
        mult = ctx.v("config:worldGen.buildingCoreLootChanceMultiplier", 1.0)
        rolls["p"] = min(1.0, float(rolls["p"]) * max(0.0, float(mult)))
        used.append("config:worldGen.buildingCoreLootChanceMultiplier")
    entries = []
    total = 0.0
    for entry in pool["entries"]:
        ids = entry.get("ids") or {}
        w = float(ctx.v(ids.get("weight"), entry.get("weight", 1)))
        lo = ctx.v(ids.get("min"), entry["count"][0])
        hi = ctx.v(ids.get("max"), entry["count"][1])
        total += max(0.0, w)
        entries.append((entry, max(0.0, w), lo, hi))
        if matches(entry, item_key):
            used += [i for i in ids.values() if i]
    per_roll = [0.0] * (K + 1)
    mean_roll = 0.0
    if total > 0:
        for entry, w, lo, hi in entries:
            if not matches(entry, item_key) or w == 0:
                continue
            share = w / total
            count = uniform_count(lo, hi, K)
            for j in range(K + 1):
                per_roll[j] += share * count[j]
            mean_roll += share * (max(0, int(lo)) + max(0, int(hi))) / 2
    per_roll[0] += 1.0 - sum(per_roll)
    per_roll[0] = max(0.0, per_roll[0])
    dist = [0.0] * (K + 1)
    exp_rolls = 0.0
    for n, prob in roll_distribution(rolls):
        exp_rolls += n * prob
        dn = power(per_roll, n, K)
        for j in range(K + 1):
            dist[j] += prob * dn[j]
    return dist, exp_rolls * mean_roll, [u for u in used if u]


def table_distribution(ctx: Ctx, table_id: str, item_key: str, K: int):
    table = ctx.tables.get(table_id)
    dist = [1.0] + [0.0] * K
    mean = 0.0
    used: list[str] = []
    if not table:
        return dist, 0.0, used
    for pool in table["pools"]:
        if not any(matches(e, item_key) for e in pool["entries"]):
            continue
        d, m, u = pool_distribution(ctx, pool, item_key, K)
        dist = conv(dist, d, K)
        mean += m
        used += u
    return dist, mean, used


def structure_streams(ctx: Ctx, skey: str, item_key: str, mode: str, K: int):
    """-> [(Rate, Verteilung, Erwartung je Öffnung, Behälter-Name)], Annahmen, Wert-Ids"""
    streams, assumptions, used = [], [], []
    rare_share = float(ctx.param("vault.rareShare", 0.8))
    for ckey, container in P.STRUCTURES[skey]["containers"].items():
        mix = [0.0] * (K + 1)
        mix_mean = 0.0
        weight_sum = 0.0
        found = False
        for table_id, weight in container["tables"].items():
            if weight == "vault.rareShare":
                weight = rare_share
                assumptions.append("param:vault.rareShare")
            elif weight == "vault.commonShare":
                weight = 1.0 - rare_share
            table = ctx.tables.get(table_id)
            weight_sum += weight
            if not table or not any(matches(e, item_key) for p in table["pools"] for e in p["entries"]):
                mix[0] += weight
                continue
            found = True
            d, m, u = table_distribution(ctx, table_id, item_key, K)
            used += u
            for j in range(K + 1):
                mix[j] += weight * d[j]
            mix_mean += weight * m
        if not found or weight_sum <= 0:
            continue
        mix = [x / weight_sum for x in mix]
        rate, a = ctx.rate(skey, ckey, item_key, mode)
        assumptions += a
        streams.append((rate, mix, mix_mean / weight_sum, container["label"]))
    return streams, assumptions, used


def trade_offer_chance(ctx: Ctx, trade: dict) -> tuple[float, str, list[str]]:
    """Wahrscheinlichkeit, dass ein Händler/Dorfbewohner (dieses Pools) das Angebot hat."""
    if not trade["pools"]:
        return 0.0, "in keinem Pool", []
    pool = trade["pools"][0]
    draws = int(pool.get("amount") or 2)
    used = []
    others = [float(e.get("chance", 1.0)) for e in pool["vanilla"]]
    self_chance = 1.0
    for mod_id in pool["mod"]:
        other = ctx.trades.get(mod_id)
        vid = (other or {}).get("ids", {}).get("offerChance")
        chance = float(ctx.v(vid, (other or {}).get("offerChance") or 1.0)) if other else 1.0
        if mod_id == trade["id"]:
            self_chance = chance
            if vid:
                used.append(vid)
        else:
            others.append(chance)
    p = offer_probability(self_chance, others, draws)
    return p, f"{pool['label']}: {len(pool['vanilla'])} Vanilla + {len(pool['mod'])} Mod, {draws} gezogen", used


def trade_yield(ctx: Ctx, trade: dict, item_key: str) -> tuple[float, int, list[str]]:
    """(Anteil der Angebote, die dieses Item geben, Stück je voller Auffüllung, Wert-Ids)"""
    ids = trade.get("ids", {})
    uses = int(ctx.v(ids.get("maxUses"), trade.get("maxUses") or 1))
    gives = int(ctx.v(ids.get("gives"), (trade.get("gives") or {}).get("count", 1)))
    used = [i for i in (ids.get("maxUses"), ids.get("gives")) if i]
    share = 1.0
    if item_key.startswith("book:"):
        pool = trade.get("enchantPool") or []
        total = sum(float(ctx.v(e["id"], e["weight"])) for e in pool)
        mine = sum(float(ctx.v(e["id"], e["weight"])) for e in pool if matches(e, item_key))
        share = mine / total if total > 0 else 0.0
        used += [e["id"] for e in pool]
    return share, max(0, uses * gives), used


# ---------------------------------------------------------------------------
# Zeilen je Quelle
# ---------------------------------------------------------------------------

def _row(key, kind, label, mode, times, ks, **extra):
    row = {"key": key, "kind": kind, "label": label, "mode": mode, "k": ks,
           "mean": times["mean"], "median": times["median"], "p90": times["p90"],
           "hitRate": times.get("hitRate", 0.0)}
    row.update(extra)
    return row


def source_rows(ctx: Ctx, item_key: str, mode: str, ks: list[int], seen=None, recipes: bool = True) -> tuple[list[dict], list[tuple]]:
    """Alle Zeilen für ein Item und die Poisson-Ströme (für die Summe 'normales Spiel')."""
    K = max(ks)
    rows: list[dict] = []
    all_streams: list[tuple] = []
    disabled = set()
    for src in ctx.snap["sources"].get(item_key, []):
        if ctx.eff(f"sourceoff:{item_key}:{src['key']}"):
            disabled.add(src["key"])
    for src in ctx.snap["sources"].get(item_key, []):
        key, kind = src["key"], src["kind"]
        off = key in disabled
        if kind == "structure":
            streams, assumptions, used = structure_streams(ctx, src["structure"], item_key, mode, K)
            if not streams:
                continue
            times = compound_times([(r, d) for r, d, _, _ in streams], ks)
            per_hour = sum(r * m for r, _, m, _ in streams)
            rows.append(_row(key, kind, P.STRUCTURES[src["structure"]]["label"], mode, times, ks, disabled=off,
                             itemsPerHour=per_hour, assumptions=sorted(set(assumptions)), valueIds=sorted(set(used)),
                             detail=[{"container": name, "rate": r, "perOpening": 1 - d[0], "expected": m} for r, d, m, name in streams],
                             tunables=_tunables(ctx, used)))
            if not off:
                all_streams += [(r, d) for r, d, _, _ in streams]
        elif kind == 'loot':
            dist, mean, used = table_distribution(ctx, src['table'], item_key, K)
            rate_id = 'param:' + src['param']
            rate = float(ctx.v(rate_id, 1.0)) * (ctx.normal_factor() if mode == 'normal' else 1.0)
            times = compound_times([(rate, dist)], ks)
            rows.append(_row(key, kind, src['label'], mode, times, ks, disabled=off,
                             itemsPerHour=rate * mean, assumptions=[rate_id], valueIds=used,
                             tunables=_tunables(ctx, used)))
            if not off:
                all_streams.append((rate, dist))
        elif kind in ("wandering", "villager"):
            trade = ctx.trades.get(src["trade"])
            if not trade:
                continue
            p_offer, pool_text, used_offer = trade_offer_chance(ctx, trade)
            share, per_restock, used_yield = trade_yield(ctx, trade, item_key)
            p_item = p_offer * share
            used = used_offer + used_yield
            label = f"{trade['professionDe']}" + (f" Stufe {trade['level']}" if trade.get("level") else "")
            if kind == "wandering":
                if mode == "targeted":
                    rate = float(ctx.param("trader.visitsPerHour", 1.0)); assumptions = ["param:trader.visitsPerHour"]
                else:
                    rate = float(ctx.param("traderNormal.visitsPerHour", 0.75)); assumptions = ["param:traderNormal.visitsPerHour"]
                dist = [0.0] * (K + 1)
                dist[0] = 1 - p_item
                dist[min(per_restock, K)] += p_item
                times = compound_times([(rate, dist)], ks)
                rows.append(_row(key, kind, label, mode, times, ks, disabled=off, itemsPerHour=rate * p_item * per_restock,
                                 offerChance=p_offer, poolText=pool_text, perVisit=per_restock, assumptions=assumptions,
                                 valueIds=used, tunables=_tunables(ctx, used)))
                if not off:
                    all_streams.append((rate, dist))
            else:
                level = trade.get("level") or 1
                trained = float(ctx.param(f"villager.trainedPerHour.{level}", 1.0))
                restocks = float(ctx.param("villager.restocksPerHour", 2.0))
                if mode == "normal":
                    trained *= ctx.normal_factor()
                    restocks *= ctx.normal_factor()
                find_rate = trained * p_item
                means, medians, p90s = [], [], []
                for k in ks:
                    extra = (math.ceil(k / per_restock) - 1) / restocks if per_restock > 0 and restocks > 0 else INF
                    if find_rate <= 0:
                        means.append(INF); medians.append(INF); p90s.append(INF)
                    else:
                        means.append(1 / find_rate + extra)
                        medians.append(math.log(2) / find_rate + extra)
                        p90s.append(math.log(10) / find_rate + extra)
                rows.append(_row(key, kind, label, mode, {"mean": means, "median": medians, "p90": p90s, "hitRate": find_rate}, ks,
                                 disabled=off, offerChance=p_offer, poolText=pool_text, perVisit=per_restock,
                                 assumptions=[f"param:villager.trainedPerHour.{level}", "param:villager.restocksPerHour"]
                                 + (["param:normalFactor"] if mode == "normal" else []),
                                 valueIds=used, tunables=_tunables(ctx, used),
                                 note="passenden Dorfbewohner finden + Auffüllungen (kein Poisson-Strom, nicht in der Summe)"))
        elif kind in ("mob", "block"):
            pkey = src["param"]
            rate = float(ctx.param(pkey, 0.0))
            assumptions = [f"param:{pkey}"]
            if mode == "normal":
                rate *= ctx.normal_factor()
                assumptions.append("param:normalFactor")
            dist = uniform_count(src.get("count", [1, 1])[0], src.get("count", [1, 1])[1], K)
            times = compound_times([(rate, dist)], ks)
            rows.append(_row(key, kind, src["label"], mode, times, ks, disabled=off,
                             itemsPerHour=rate * sum(src.get("count", [1, 1])) / 2, assumptions=assumptions, valueIds=[],
                             tunables=[], note=src.get("note", "")))
            if not off:
                all_streams.append((rate, dist))
    # geplante eigene Quellen
    for vid in ctx.custom.get(item_key, []):
        spec = ctx.eff(vid)
        if not isinstance(spec, dict):
            continue
        rate, dist, assumptions, label = custom_stream(ctx, spec, item_key, mode, K)
        times = compound_times([(rate, dist)], ks)
        rows.append(_row(vid, "custom", label, mode, times, ks, planned=True, itemsPerHour=rate * (1 - dist[0]),
                         assumptions=assumptions, valueIds=[vid], tunables=[{"id": vid, "field": "chance", "label": "Chance (geplant)",
                                                                              "value": spec.get("chance"), "type": "prob"}]))
        all_streams.append((rate, dist))
    # Rezepte (Ketten)
    seen = set(seen or ()) | {item_key}
    if recipes and len(seen) <= 2 and mode == "targeted":
        rows += recipe_rows(ctx, item_key, ks, seen)
    return rows, all_streams


def custom_stream(ctx: Ctx, spec: dict, item_key: str, mode: str, K: int):
    chance = float(spec.get("chance", 0.0))
    lo, hi = int(spec.get("countMin", 1)), int(spec.get("countMax", spec.get("countMin", 1)))
    count = uniform_count(lo, hi, K)
    dist = [chance * c for c in count]
    dist[0] += 1 - chance
    kind = spec.get("kind")
    if kind == "structure" and spec.get("structure") in P.STRUCTURES:
        skey = spec["structure"]
        ckey = spec.get("container") or next(iter(P.STRUCTURES[skey]["containers"]))
        rate, assumptions = ctx.rate(skey, ckey, item_key, mode)
        label = spec.get("label") or P.STRUCTURES[skey]["label"]
    elif kind == "trader":
        key = "trader.visitsPerHour" if mode == "targeted" else "traderNormal.visitsPerHour"
        rate = float(ctx.param(key, 1.0)); assumptions = [f"param:{key}"]
        label = spec.get("label") or "Fahrender Händler"
    else:
        rate = float(spec.get("rate", 0.0))
        assumptions = []
        if mode == "normal":
            normal = spec.get("rateNormal")
            rate = float(normal) if normal not in (None, "") else rate * ctx.normal_factor()
            assumptions.append("param:normalFactor")
        label = spec.get("label") or kind or "Quelle"
    return max(0.0, rate), dist, assumptions, label


def _tunables(ctx: Ctx, used: list[str]) -> list[dict]:
    """Die Werte einer Zeile, die sich für den Rückwärts-Rechner eignen (Chancen, Gewichte, Nutzungen)."""
    out = []
    seen = set()
    for vid in used:
        if not vid or vid in seen or vid.startswith("config:"):
            continue
        seen.add(vid)
        record = ctx.snap["values"].get(vid)
        if not record or record.get("readonly"):
            continue
        if record["type"] in ("prob",) or vid.endswith(":weight") or vid.endswith(".weight") or vid.endswith(":maxUses"):
            out.append({"id": vid, "label": f"{record['group']} - {record['label']}", "value": ctx.v(vid, record["value"]),
                        "type": record["type"], "apply": record["apply"]})
    return out


def recipe_rows(ctx: Ctx, item_key: str, ks: list[int], seen: set) -> list[dict]:
    rows = []
    if item_key.startswith("book:"):
        return rows
    for recipe in ctx.recipes:
        if recipe.get("easter") or (recipe.get("result") or {}).get("id") != item_key:
            continue
        result_count = int(ctx.v(recipe["ids"].get("count"), recipe["result"].get("count", 1)) or 1)
        tracked = []
        for ing in recipe["ingredients"]:
            ident = ing["id"]
            if " / " in ident or ident.startswith("#") or ident == item_key or ident in seen:
                continue
            if not ctx.snap["sources"].get(ident):
                continue
            tracked.append((ident, ing["count"]))
        if not tracked:
            continue
        needs = [(ident, [math.ceil(k * cnt / result_count) for k in ks]) for ident, cnt in tracked]
        means = [0.0] * len(ks)
        medians = [0.0] * len(ks)
        p90s = [0.0] * len(ks)
        parts = []
        for ident, ns in needs:
            n_ks = sorted(set(ns))
            if max(n_ks) > 64:
                continue
            sub_rows, _ = source_rows(ctx, ident, "targeted", n_ks, seen)
            candidates = [r for r in sub_rows if r["kind"] != "recipe" and not r.get("disabled")]
            if not candidates:
                continue
            best = min(candidates, key=lambda r: r["mean"][-1])
            index = {n: i for i, n in enumerate(n_ks)}
            for i, n in enumerate(ns):
                means[i] = max(means[i], best["mean"][index[n]])
                medians[i] = max(medians[i], best["median"][index[n]])
                p90s[i] = max(p90s[i], best["p90"][index[n]])
            parts.append({"item": ident, "per": [n for n in ns], "via": best["label"]})
        if not parts:
            continue
        display = ctx.snap.get("display", {})
        via = ", ".join((display.get(p["item"], {}).get("name") or {}).get("de", p["item"].split(":")[-1]) for p in parts)
        kind = recipe.get("type", "").split(":")[-1]
        verb = {"smithing_transform": "Schmieden", "crafting_shaped": "Werkbank", "crafting_shapeless": "Werkbank"}.get(kind, kind)
        rows.append(_row(f"recipe:{recipe['id']}", "recipe", f"aus {via} ({verb})", "targeted",
                         {"mean": means, "median": medians, "p90": p90s}, ks, parts=parts, valueIds=[], tunables=[],
                         note="Zeit für die verfolgten Zutaten (die langsamste zählt); andere Zutaten gelten als vorhanden"))
    return rows


def item_report(ctx: Ctx, item_key: str, kmax: int = KMAX) -> dict:
    ks = list(range(1, kmax + 1))
    targeted, targeted_streams = source_rows(ctx, item_key, "targeted", ks)
    normal_rows, normal_streams = source_rows(ctx, item_key, "normal", ks)
    active = [r for r in targeted if not r.get("disabled") and r["kind"] != "recipe"]
    best = min(active, key=lambda r: (r["mean"][0], r["key"])) if active else None
    normal = compound_times(normal_streams, ks) if normal_streams else None
    together = None
    poisson_rows = [r for r in active if r["kind"] not in ("villager", "recipe")]
    if len(poisson_rows) > 1:
        t = compound_times(targeted_streams, ks)
        together = {"mean": t["mean"], "median": t["median"], "p90": t["p90"], "hitRate": t["hitRate"],
                    "note": "alle Truhen-, Händler-, Mob- und Block-Quellen gleichzeitig gezielt (wie KERNE-SELTENHEIT 5.3 beim Eisenkern)"}
    return {
        "item": item_key, "k": ks, "rows": targeted, "normalRows": normal_rows, "together": together,
        "best": best and {"key": best["key"], "label": best["label"], "mean": best["mean"], "median": best["median"], "p90": best["p90"]},
        "normal": normal and {"mean": normal["mean"], "median": normal["median"], "p90": normal["p90"], "hitRate": normal["hitRate"],
                              "note": "alle Truhen-, Händler-, Mob- und Block-Quellen zusammen, mit normalen Raten (ohne Dorfbewohner und Rezepte)"},
    }


def metric(ctx: Ctx, item_key: str, row_key: str | None, mode: str, stat: str, k: int) -> float:
    ks = list(range(1, max(k, 1) + 1))
    never = {"mean": [INF] * len(ks), "median": [INF] * len(ks), "p90": [INF] * len(ks)}
    if row_key in (None, "", "__normal__"):
        _, streams = source_rows(ctx, item_key, "normal", ks, recipes=False)
        times = compound_times(streams, ks) if streams else never
        return times[stat][k - 1]
    if row_key == "__together__":
        _, streams = source_rows(ctx, item_key, "targeted", ks, recipes=False)
        times = compound_times(streams, ks) if streams else never
        return times[stat][k - 1]
    if mode == "normal":
        rows, _ = source_rows(ctx, item_key, "normal", ks, recipes=False)
        for r in rows:
            if r["key"] == row_key:
                return r[stat][k - 1]
        return INF
    if row_key == "__best__":
        rows, _ = source_rows(ctx, item_key, "targeted", ks, recipes=False)
        rows = [r for r in rows if not r.get("disabled") and r["kind"] != "recipe"]
        return min((r[stat][k - 1] for r in rows), default=INF)
    rows, _ = source_rows(ctx, item_key, "targeted", ks, recipes=str(row_key).startswith("recipe:"))
    for r in rows:
        if r["key"] == row_key:
            return r[stat][k - 1]
    return INF


def reverse(snapshot: dict, ctx_with, item_key: str, row_key: str, tunable: dict, k: int, stat: str,
            mode: str, hours: float) -> dict:
    """
    Welcher Wert des Stellwerts `tunable` ({"id", "type", "field"?}) fuehrt zur Zielzeit? Bisektion
    (die Zeit faellt mit steigender Chance/Gewicht/Nutzung). ctx_with(overrides) -> Ctx.
    """
    vid = tunable["id"]
    record = snapshot["values"].get(vid, {})
    kind = tunable.get("type") or record.get("type")
    field = tunable.get("field")
    current_spec = ctx_with({}).eff(vid) if field else None

    def at(x):
        if field:
            spec = dict(current_spec or {})
            spec[field] = x
            ctx = ctx_with({vid: spec})
        else:
            ctx = ctx_with({vid: x})
        return metric(ctx, item_key, row_key, mode, stat, k)

    if kind == "prob" or field == "chance":
        lo, hi = 1e-7, 1.0
    else:
        lo = max(float(record.get("min") or 0), 0.0) or (1.0 if kind == "int" else 1e-6)
        hi = float(record.get("max") or 100000)
    t_hi, t_lo = at(lo), at(hi)
    if t_lo > hours:
        return {"feasible": False, "value": hi, "achieved": t_lo,
                "message": f"Selbst mit dem Höchstwert ({_fmt(hi, kind)}) dauert es {t_lo:.1f} h - Ziel {hours:.1f} h ist mit diesem Wert allein nicht erreichbar."}
    if t_hi < hours:
        return {"feasible": False, "value": lo, "achieved": t_hi,
                "message": f"Schon mit dem Kleinstwert ({_fmt(lo, kind)}) dauert es nur {t_hi:.1f} h - das Ziel ist langsamer als möglich."}
    a, b = math.log(lo), math.log(hi)
    for _ in range(70):
        mid = (a + b) / 2
        if at(math.exp(mid)) > hours:
            a = mid
        else:
            b = mid
    x = math.exp((a + b) / 2)
    if kind == "int":
        candidates = {max(int(lo), math.floor(x)), min(int(hi), math.ceil(x))}
        x = min(candidates, key=lambda c: abs(at(c) - hours))
    elif kind == "prob":
        x = float(f"{x:.4g}")
    else:
        x = float(f"{x:.4g}")
    return {"feasible": True, "value": x, "achieved": at(x), "message": ""}


def _fmt(x, kind):
    return f"{x * 100:.4g} %" if kind == "prob" else f"{x:g}"


def overview(ctx: Ctx, item_keys: list[str]) -> list[dict]:
    out = []
    for key in item_keys:
        rep = item_report(ctx, key, 1)
        if not rep["rows"] and not rep["normal"]:
            continue
        out.append({"item": key,
                    "bestMean": rep["best"]["mean"][0] if rep["best"] else None,
                    "bestMedian": rep["best"]["median"][0] if rep["best"] else None,
                    "bestLabel": rep["best"]["label"] if rep["best"] else None,
                    "bestKey": rep["best"]["key"] if rep["best"] else None,
                    "normalMean": rep["normal"]["mean"][0] if rep["normal"] else None,
                    "normalMedian": rep["normal"]["median"][0] if rep["normal"] else None,
                    "sources": len([r for r in rep["rows"] if not r.get("disabled")])})
    return out
