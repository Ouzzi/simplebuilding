"""
Zeit -> Stellwerte: der Besitzer tippt eine Beschaffungszeit ein (Quelle, 1. ... 6. Stück, Mittel/Median/90 %,
gezielt oder normales Spiel), und die Zentrale sucht die Werte, die diese Zeit ergeben.

* Jede Quelle kennt ihre **Stellwerte** (`row_drivers`): Truhen - das Gewicht des Items in jedem Pool, der
  es enthält, und die Kern-Chance der eigenen Kern-Pools; Händler/Dorfbewohner - die Angebots-Chance
  (Bücher: das Gewicht des Buchs im Verzauberungs-Pool); Mob/Block - die Annahme Tötungen/Abbau je Stunde
  (die Mod hat dort keine Chance, der Drop ist sicher); geplante Quellen - ihre Chance.
  "eigene" Stellwerte betreffen nur dieses Item; "geteilte" (Würfe eines gemischten Pools, Angebots-Chance
  eines Buch-Angebots) auch andere - sie kommen nur dazu, wenn die eigenen das Ziel nicht erreichen.
* Verteilung: **proportional** (Standard) - alle Stellwerte der betroffenen Quellen mit einem gemeinsamen
  Faktor s (Chancen an 0/100 % gekappt); **eine Quelle** - nur deren Stellwerte; **ein Wert** - genau
  ein Stellwert (Bisektion über den Wert selbst, wie der Rückwärts-Rechner).
* Die Zeit fällt monoton mit s; Bisektion in log s, danach Runden (ganze Zahlen, Chancen/Kommazahlen auf 4
  gültige Stellen) und die erreichte Zeit mit den gerundeten Werten neu gerechnet.

Nichts hier schreibt: das Ergebnis sind Vorschläge, die die Oberfläche als Entwürfe übernimmt; gespeichert
wird nur über preview -> Bestätigung -> save.
"""

from __future__ import annotations

import math

from . import model
from . import params as P

POISSON_KINDS = ("structure", "wandering", "mob", "block", "custom")
S_MIN, S_MAX = 1e-6, 1e6


def _record(ctx: model.Ctx, vid: str) -> dict | None:
    return ctx.snap["values"].get(vid)


def _driver(ctx: model.Ctx, vid: str | None, field: str | None = None) -> dict | None:
    """Ein Stellwert mit Typ, Grenzen und aktuellem Wert - oder None, wenn er nicht einstellbar ist."""
    if not vid:
        return None
    if field:  # geplante Quelle: ein Feld im Objekt
        spec = ctx.eff(vid)
        if not isinstance(spec, dict) or field not in spec:
            return None
        return {"id": vid, "field": field, "type": "prob", "lo": 1e-7, "hi": 1.0, "cur": float(spec[field]), "spec": dict(spec)}
    record = _record(ctx, vid)
    if not record or record.get("readonly") or record.get("alias") or record["type"] not in ("prob", "int", "float"):
        return None
    kind = record["type"]
    if kind == "prob":
        lo, hi = 0.0, 1.0
        if record.get("max") is not None:
            hi = min(hi, float(record["max"]))
    else:
        lo = float(record["min"]) if record.get("min") is not None else 0.0
        hi = float(record["max"]) if record.get("max") is not None else 1e6
    cur = ctx.v(vid, record.get("value"))
    try:
        cur = float(cur)
    except (TypeError, ValueError):
        return None
    return {"id": vid, "field": None, "type": kind, "lo": max(lo, 0.0), "hi": hi, "cur": cur}


def row_drivers(ctx: model.Ctx, item_key: str, row_key: str) -> tuple[list[dict], list[dict]]:
    """-> (eigene, geteilte) Stellwerte der Quelle `row_key` für das Item."""
    own: list = []
    shared: list = []
    if row_key in ctx.custom.get(item_key, []):
        own.append(_driver(ctx, row_key, "chance"))
        return [d for d in own if d], []
    src = next((s for s in ctx.snap["sources"].get(item_key, []) if s["key"] == row_key), None)
    if src is None:
        return [], []
    kind = src["kind"]
    if kind == "structure":
        for container in P.STRUCTURES[src["structure"]]["containers"].values():
            for table_id in container["tables"]:
                table = ctx.tables.get(table_id)
                if not table:
                    continue
                for pool in table["pools"]:
                    if not any(model.matches(e, item_key) for e in pool["entries"]):
                        continue
                    p_id = (pool["rolls"].get("ids") or {}).get("p")
                    only_me = all(model.matches(e, item_key) for e in pool["entries"] if e.get("item"))
                    if pool.get("rareCore") or (pool["rolls"]["type"] == "binomial" and only_me):
                        own.append(_driver(ctx, p_id))
                    elif pool["rolls"]["type"] == "binomial":
                        shared.append(_driver(ctx, p_id))
                    for entry in pool["entries"]:
                        if model.matches(entry, item_key):
                            own.append(_driver(ctx, (entry.get("ids") or {}).get("weight")))
    elif kind in ("wandering", "villager"):
        trade = ctx.trades.get(src["trade"]) or {}
        ids = trade.get("ids") or {}
        if item_key.startswith("book:"):
            for e in trade.get("enchantPool") or []:
                if model.matches(e, item_key):
                    own.append(_driver(ctx, e["id"]))
            shared.append(_driver(ctx, ids.get("offerChance")))
        else:
            own.append(_driver(ctx, ids.get("offerChance")))
        # Nutzungen wirken erst ab dem Stück nach der ersten Auffüllung
        shared.append(_driver(ctx, ids.get("maxUses")))
    elif kind in ("mob", "block"):
        own.append(_driver(ctx, f"param:{src['param']}"))
    return _unique(own), [d for d in _unique(shared) if d["id"] not in {o["id"] for o in _unique(own)}]


def _unique(drivers):
    out, seen = [], set()
    for d in drivers:
        if d and (d["id"], d["field"]) not in seen:
            seen.add((d["id"], d["field"]))
            out.append(d)
    return out


def active_rows(ctx: model.Ctx, item_key: str) -> list[dict]:
    rows, _ = model.source_rows(ctx, item_key, "targeted", [1], recipes=False)
    return [r for r in rows if not r.get("disabled")]


def _scaled(d: dict, s: float) -> float:
    return min(d["hi"], max(d["lo"], d["cur"] * s))


def _round(d: dict, x: float) -> float:
    if d["type"] == "int":
        r = int(round(x))
        if d["cur"] >= 1 and r < 1:
            r = 1  # ein Gewicht 0 schaltet die Quelle ab - das ist kein Ergebnis einer Zeit
        return int(min(max(r, math.ceil(d["lo"])), math.floor(d["hi"])))
    if x <= 0:
        return 0.0
    return min(d["hi"], max(d["lo"], float(f"{x:.4g}")))


def solve(ctx_with, item_key: str, row_key: str, mode: str, stat: str, k: int, hours: float,
          strategy: str = "proportional") -> dict:
    """
    ctx_with(overrides) -> Ctx mit den Entwürfen plus overrides. row_key: Quelle, "__together__" (alle
    gezielt) oder "__normal__" (normales Spiel). strategy: "proportional" | "source:<Quelle>" | "value:<Id>".
    -> {"feasible", "message", "current", "target", "achieved", "factor", "changes": [{id, field, old, new,
        clamped}], "rows", "sharedUsed", "ignored"}
    """
    if stat not in ("mean", "median", "p90"):
        raise ValueError("stat muss mean, median oder p90 sein")
    if not (1 <= k <= 64) or not (0 < hours < 1e7) or not math.isfinite(hours):
        raise ValueError("Zielzeit > 0 und Stückzahl 1 bis 64")
    if row_key == "__normal__":
        mode = "normal"
    base = ctx_with({})

    def at(overrides):
        return model.metric(ctx_with(overrides), item_key, row_key, mode, stat, k)

    current = at({})
    out = {"current": current, "target": hours, "row": row_key, "mode": mode, "stat": stat, "k": k,
           "strategy": strategy, "changes": [], "sharedUsed": False, "ignored": []}
    if str(row_key).startswith("recipe:"):
        return dict(out, feasible=False, achieved=current, factor=1.0,
                    message="Rezept-Zeilen rechnen aus den Zutaten - ändere die Zeit auf der Seite der Zutat.")

    # --- ein einzelner Wert: Bisektion über den Wert selbst -------------------------------------------
    if strategy.startswith("value:"):
        vid = strategy[len("value:"):]
        field = "chance" if vid.startswith("source:") else None
        d = _driver(base, vid, field)
        if d is None:
            return dict(out, feasible=False, achieved=current, factor=1.0, message=f"{vid}: kein einstellbarer Wert.")
        return _finish(out, ctx_with, at, [d], *_solve_single(d, at, hours, base))

    # --- Quellen und ihre Stellwerte --------------------------------------------------------------------
    if row_key in ("__together__", "__normal__"):
        if strategy.startswith("source:"):
            keys = [strategy[len("source:"):]]
        else:
            keys = [r["key"] for r in active_rows(base, item_key) if r["kind"] in POISSON_KINDS]
    else:
        keys = [row_key]
    out["rows"] = keys
    own, shared = [], []
    for key in keys:
        o, s = row_drivers(base, item_key, key)
        own += o
        shared += s
    own, shared = _unique(own), [d for d in _unique(shared) if d["id"] not in {o["id"] for o in own}]
    own, ignored = _effective(own, at, current)
    out["ignored"] += ignored
    drivers = own
    result = _solve_scale(drivers, at, hours) if drivers else None
    if (result is None or not result[0]) and shared:
        more, ignored = _effective(shared, at, current)
        out["ignored"] += ignored
        if more:
            candidate = _solve_scale(drivers + more, at, hours)
            if result is None or candidate[0] or abs(math.log(max(candidate[2], 1e-12) / hours)) < abs(math.log(max(result[2], 1e-12) / hours)):
                drivers, result = drivers + more, candidate
                out["sharedUsed"] = True
    if not drivers:
        return dict(out, feasible=False, achieved=current, factor=1.0,
                    message="Diese Quelle hat keinen einstellbaren Wert, der die Zeit verändert (z. B. schon 100 % oder "
                            "nur Rezept/Annahme) - wähle eine andere Verteilung oder einen einzelnen Wert.")
    return _finish(out, ctx_with, at, drivers, *result)


def _effective(drivers: list[dict], at, current: float) -> tuple[list[dict], list[str]]:
    """Stellwerte, die die Zeit überhaupt bewegen (ein Gewicht im Pool ohne andere Einträge tut es nicht)."""
    keep, ignored = [], []
    for d in drivers:
        moved = False
        for s in (0.5, 2.0):
            x = _scaled(d, s)
            if abs(x - d["cur"]) < 1e-15:
                continue
            t = at(_overrides_single(d, x))
            if not _close(t, current):
                moved = True
                break
        if moved and d["cur"] > 0:
            keep.append(d)
        else:
            ignored.append(d["id"])
    return keep, ignored


def _overrides_single(d, x):
    return _overrides_from([d], [x])


def _close(a: float, b: float) -> bool:
    if math.isinf(a) or math.isinf(b):
        return math.isinf(a) and math.isinf(b)
    return abs(a - b) <= 1e-9 * max(1.0, abs(b))


def _solve_scale(drivers: list[dict], at, hours: float):
    """-> (erreichbar, Faktor s, Zeit bei s, Werte bei s) - Werte noch ungerundet."""
    def values(s):
        return [_scaled(d, s) for d in drivers]

    def t(s):
        return at(_overrides_from(drivers, values(s)))
    t_fast, t_slow = t(S_MAX), t(S_MIN)
    if t_fast > hours:
        return False, S_MAX, t_fast, values(S_MAX)
    if t_slow < hours:
        return False, S_MIN, t_slow, values(S_MIN)
    a, b = math.log(S_MIN), math.log(S_MAX)
    for _ in range(80):
        mid = (a + b) / 2
        if t(math.exp(mid)) > hours:
            a = mid
        else:
            b = mid
    s = math.exp((a + b) / 2)
    return True, s, t(s), values(s)


def _solve_single(d: dict, at, hours: float, base):
    lo = d["lo"] if d["lo"] > 0 else (1.0 if d["type"] == "int" else 1e-7)
    hi = d["hi"]

    def t(x):
        return at(_overrides_from([d], [x]))
    t_fast, t_slow = t(hi), t(lo)
    if t_fast > hours:
        return False, hi / d["cur"] if d["cur"] else math.inf, t_fast, [hi]
    if t_slow < hours:
        return False, lo / d["cur"] if d["cur"] else 0.0, t_slow, [lo]
    a, b = math.log(lo), math.log(hi)
    for _ in range(80):
        mid = (a + b) / 2
        if t(math.exp(mid)) > hours:
            a = mid
        else:
            b = mid
    x = math.exp((a + b) / 2)
    return True, (x / d["cur"]) if d["cur"] else math.inf, t(x), [x]


def _overrides_from(drivers, values):
    out: dict = {}
    for d, x in zip(drivers, values):
        if d["field"]:
            spec = dict(out.get(d["id"]) or d.get("spec") or {})
            spec[d["field"]] = x
            out[d["id"]] = spec
        else:
            out[d["id"]] = x
    return out


def _finish(out: dict, ctx_with, at, drivers: list[dict], feasible: bool, factor: float, t_raw: float, raw: list[float]):
    rounded = [_round(d, x) for d, x in zip(drivers, raw)]
    # eine einzelne ganze Zahl: die bessere der beiden Nachbarn
    if len(drivers) == 1 and drivers[0]["type"] == "int" and feasible:
        d = drivers[0]
        candidates = {_round(d, math.floor(raw[0])), _round(d, math.ceil(raw[0]))}
        rounded = [min(candidates, key=lambda c: abs(at(_overrides_from([d], [c])) - out["target"]))]
    elif feasible and any(d["type"] == "int" for d in drivers):
        rounded = _refine_ints(drivers, rounded, at, out["target"])
    achieved = at(_overrides_from(drivers, rounded))
    changes = []
    for d, x, r in zip(drivers, raw, rounded):
        clamped = "max" if x >= d["hi"] - 1e-15 else ("min" if x <= d["lo"] + 1e-15 else None)
        if _close(r, d["cur"]):
            continue
        changes.append({"id": d["id"], "field": d["field"], "type": d["type"], "old": d["cur"], "new": r,
                        "clamped": clamped})
    message = ""
    if not feasible:
        faster = t_raw > out["target"]
        message = (f"Nicht ganz erreichbar: {'selbst mit den Höchstwerten' if faster else 'selbst mit den Kleinstwerten'} "
                   f"dauert es {_h(t_raw)} (Ziel {_h(out['target'])}). Übernommen wird der Grenzwert.")
    elif changes and any(c["clamped"] for c in changes):
        message = "Einige Werte stehen an ihrer Grenze (0 bzw. 100 %) - die anderen haben den Rest übernommen."
    if feasible and any(d["type"] == "int" for d in drivers) and not _near(achieved, out["target"]):
        message = (message + " " if message else "") + (
            f"Ganze Zahlen: genauer geht es nicht - mit den gerundeten Werten {_h(achieved)} statt {_h(out['target'])}.")
    return dict(out, feasible=feasible, factor=factor, achieved=achieved, changes=changes, message=message,
                specs={d["id"]: _overrides_from([d], [r])[d["id"]] for d, r in zip(drivers, rounded) if d["field"]})


def _refine_ints(drivers, rounded, at, target, rounds: int = 12):
    """Mehrere ganze Zahlen: gerundet trifft es oft schlecht (Gewicht 2 statt 2,4) - je Schritt die eine
    +-1-Änderung nehmen, die dem Ziel am nächsten kommt, solange es besser wird."""
    values = list(rounded)
    best = abs(at(_overrides_from(drivers, values)) - target)
    for _ in range(rounds):
        step = None
        for i, d in enumerate(drivers):
            if d["type"] != "int":
                continue
            for delta in (-1, 1):
                x = values[i] + delta
                if x < max(d["lo"], 1 if d["cur"] >= 1 else 0) or x > d["hi"]:
                    continue
                trial = values[:i] + [x] + values[i + 1:]
                err = abs(at(_overrides_from(drivers, trial)) - target)
                if err < best - 1e-12 and (step is None or err < step[0]):
                    step = (err, trial)
        if step is None:
            break
        best, values = step
    return values


def _near(a: float, b: float) -> bool:
    return abs(a - b) <= 0.005 * max(abs(b), 1e-9)


def _h(t: float) -> str:
    if math.isinf(t):
        return "nie"
    return f"{t:.2f} h" if t < 100 else f"{t:.0f} h"
