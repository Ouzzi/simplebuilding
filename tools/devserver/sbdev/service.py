"""
Die Logik hinter der API: Schnappschuss + Ablage + Prüfung + Anwenden + Rechner + Datagen + checkBalance.

Änderungen kommen als Liste {"id", "value"} oder {"id", "reset": true} (zurück auf den Mod-Wert),
jeweils mit "expected" = der Wert, den die Oberfläche beim Bearbeiten gesehen hat. Ablauf immer:
preview (prüfen, Zusammenfassung) -> Bestätigung in der Oberfläche -> save (dieselbe Prüfung
noch einmal, dann Commit). Rollback baut die Aenderungsliste aus der Zielversion und läuft durch
denselben Weg.
"""

from __future__ import annotations

import json
import math
import re
import threading
import time
from pathlib import Path

from . import apply as applier
from . import check as balance_checker
from . import docs, extract, javaedit, jobs, model, params
from .store import Store, StoreError
from .values import CATEGORIES, Invalid, same, validate

SOURCE_KINDS = {"structure": "Struktur/Truhe", "mob": "Mob-Drop", "trader": "Fahrender Händler", "block": "Block abbauen",
                "custom": "Eigene Quelle"}
DYNAMIC_RATE = re.compile(r"^param:(rate)\.([a-z_]+)\.([a-z_]+)@(.+)$")


class Service:
    def __init__(self, repo: Path, store_root: Path, *, refresh_vanilla: bool = False, read_only: bool = False):
        self.repo = Path(repo)
        # read_only: nie in Mod-Dateien schreiben und kein Datagen (Tests auf dem echten Repo)
        self.read_only = read_only
        self.store = Store(store_root)
        self._lock = threading.RLock()
        self.snapshot = extract.build(self.repo, refresh_vanilla)
        self.fingerprint = extract.fingerprint(self.repo)
        self.checked = time.time()
        self.job: jobs.Job | None = None
        self.datagen_steps = None  # Tests setzen hier eigene Schritte statt Gradle

    # ---- Schnappschuss -------------------------------------------------------------------

    def reload(self) -> dict:
        with self._lock:
            self.snapshot = extract.build(self.repo)
            self.fingerprint = extract.fingerprint(self.repo)
            self.checked = time.time()
            return {"builtAt": self.snapshot["builtAt"], "seconds": self.snapshot["buildSeconds"]}

    def maybe_reload(self) -> bool:
        with self._lock:
            if time.time() - self.checked < 5:
                return False
            self.checked = time.time()
            fp = extract.fingerprint(self.repo)
            if fp != self.fingerprint:
                self.reload()
                return True
            return False

    # ---- Wertedefinitionen, auch für dynamische Ids --------------------------------------

    def record_for(self, vid: str) -> dict | None:
        record = self.snapshot["values"].get(vid)
        if record:
            return record
        m = DYNAMIC_RATE.match(vid)
        if m and m.group(2) in params.STRUCTURES and m.group(3) in params.STRUCTURES[m.group(2)]["containers"]:
            base = self.snapshot["values"].get(f"param:rate.{m.group(2)}.{m.group(3)}")
            return dict(base or {}, id=vid, value=None, nullable=True, label=f"{(base or {}).get('label', vid)} nur für {m.group(4)}",
                        category="param", apply="tool", type="float", min=0.0, max=1000.0, group=(base or {}).get("group", ""))
        if vid.startswith("source:"):
            item = vid.split(":", 1)[1].rsplit(":", 1)[0]
            return {"id": vid, "category": "source", "label": "geplante Quelle", "group": item, "type": "json", "value": None,
                    "nullable": True, "apply": "plan", "source": {}, "refs": {"item": item}, "min": None, "max": None}
        if vid.startswith("sourceoff:"):
            rest = vid[len("sourceoff:"):]
            for item, srcs in self.snapshot["sources"].items():
                if rest.startswith(item + ":") and any(s["key"] == rest[len(item) + 1:] for s in srcs):
                    return {"id": vid, "category": "source", "label": "Quelle abgeschaltet (Planung)", "group": item,
                            "type": "bool", "value": False, "apply": "plan", "source": {}, "refs": {"item": item,
                            "source": rest[len(item) + 1:]}, "min": None, "max": None}
        return None

    def validate_value(self, record: dict, raw):
        if record["id"].startswith("source:"):
            return validate_source_spec(raw)
        if record["id"] == "param:eras":
            return validate_eras(raw)
        alias = record.get("alias")
        if alias:
            target = self.record_for(alias["id"]) or {}
            raise Invalid(f"{record['label']}: wird über {target.get('group', '')} {target.get('label', alias['id'])} geändert "
                          f"(Wert = {'Konstante' if alias['id'].startswith('const:') else 'Standard'} × {alias['factor']:g}) - "
                          "bitte dort ändern")
        if record.get("readonly"):
            raise Invalid(f"{record['label']}: nur lesbar" + (f" (berechnet aus {record['derived']} - ändere die Bestandteile)"
                                                                if record.get("derived") else f" ({record.get('note', '')})"))
        value = validate(record, raw)
        if record.get("apply") == "mod":
            for site in applier.sites_of(record):
                if site.get("transform"):
                    try:
                        javaedit.inverse(site, value)
                    except javaedit.JavaEditError as err:
                        raise Invalid(f"{record['label']}: {err}") from None
        return value

    # ---- Effektive Werte -------------------------------------------------------------------

    def effective_fn(self, entries: dict, overrides: dict | None = None):
        values = self.snapshot["values"]
        overrides = overrides or {}

        def eff(vid):
            if vid in overrides:
                return overrides[vid]
            entry = entries.get(vid)
            if entry is not None:
                return entry["value"]
            record = values.get(vid)
            return record["value"] if record else None
        return eff

    def custom_ids(self, entries: dict, overrides: dict | None = None) -> dict:
        out: dict[str, list[str]] = {}
        overrides = overrides or {}
        ids = set(k for k in entries if k.startswith("source:")) | set(k for k in overrides if k.startswith("source:"))
        for vid in sorted(ids):
            value = overrides.get(vid, entries.get(vid, {}).get("value") if vid in entries else None)
            if value is None:
                continue
            item = vid.split(":", 1)[1].rsplit(":", 1)[0]
            out.setdefault(item, []).append(vid)
        return out

    def ctx(self, overrides: dict | None = None, entries: dict | None = None) -> model.Ctx:
        entries = self.store.state()["entries"] if entries is None else entries
        return model.Ctx(self.snapshot, self.effective_fn(entries, overrides), self.custom_ids(entries, overrides))

    def statuses(self, entries: dict) -> dict:
        """Für jeden geplanten Wert: planned | applied | orphan, plus drift."""
        out = {}
        log = balance_checker.applied_log(self.store.root)
        for vid, entry in entries.items():
            record = self.record_for(vid)
            if record is None:
                out[vid] = {"status": "orphan", "drift": False}
                continue
            mod = record.get("value")
            status = "applied" if same(entry["value"], mod) else "planned"
            written = balance_checker.was_applied(entry, log, vid)
            drift = entry.get("mod") is not None and not same(entry.get("mod"), mod) and status != "applied"
            out[vid] = {"status": status, "drift": drift or (written and status == "planned"), "modAtSave": entry.get("mod"),
                        "origin": entry.get("origin"), "written": written}
        return out

    # ---- Zustand für die Oberfläche ------------------------------------------------------

    def state_payload(self) -> dict:
        self.maybe_reload()
        state = self.store.state()
        snap = self.snapshot
        return {
            "snapshot": snap,
            "categories": CATEGORIES,
            "store": {"version": state["version"], "savedAt": state["savedAt"], "message": state["message"],
                      "entries": state["entries"], "status": self.statuses(state["entries"]),
                      "warnings": list(self.store.warnings), "path": _rel(self.store.root, self.repo)},
            "history": self.store.history(),
            "docs": docs.listing(self.repo),
            "sourceKinds": SOURCE_KINDS,
        }

    # ---- Prüfen ----------------------------------------------------------------------------

    def check(self, state: dict, changes: list, apply_to_mod: bool = True, *, rollback: bool = False) -> dict:
        if not isinstance(changes, list):
            raise StoreError(400, "changes muss eine Liste sein.")
        if len(changes) > 5000:
            raise StoreError(400, "Zu viele Änderungen auf einmal (höchstens 5000).")
        entries = state["entries"]
        eff = self.effective_fn(entries)
        summary, errors, conflicts, normalized, skipped = [], [], [], [], []
        seen = set()
        for raw in changes:
            if not isinstance(raw, dict) or not isinstance(raw.get("id"), str):
                errors.append({"id": None, "message": f"Änderung ohne gültige Id: {raw!r}"[:200]})
                continue
            vid = raw["id"]
            if vid in seen:
                errors.append({"id": vid, "message": f"{vid}: doppelt in derselben Speicherung"})
                continue
            seen.add(vid)
            record = self.record_for(vid)
            current = eff(vid)
            reset = bool(raw.get("reset"))
            if record is None:
                if vid in entries and (reset or rollback):
                    normalized.append({"id": vid, "remove": True, "mod": None} if reset or raw.get("value") is None
                                      else {"id": vid, "new": raw.get("value"), "mod": None})
                    summary.append({"id": vid, "label": vid, "group": "", "category": "orphan", "old": current,
                                    "new": None if reset else raw.get("value"), "reset": reset, "apply": "plan", "applyNow": False,
                                    "warnings": ["Diesen Wert gibt es in der Mod nicht mehr (umbenannt/entfernt?)"]})
                else:
                    errors.append({"id": vid, "message": f"Unbekannter Wert {vid} (gibt es in der Mod nicht)"})
                continue
            if "expected" in raw and not same(raw["expected"], current) and not rollback:
                conflicts.append({"id": vid, "label": record["label"], "expected": raw["expected"], "current": current,
                                  "message": f"{record['label']}: steht inzwischen auf {current!r} (du hattest {raw['expected']!r} gesehen)"})
                continue
            mod = record.get("value")
            entry = entries.get(vid)
            if reset:
                if vid not in entries:
                    skipped.append(vid)
                    continue
                if record["apply"] == "mod":
                    # Plan verwerfen = Eintrag weg; steht in der Mod noch der geplante Wert, kommt der
                    # Ursprungswert zurück (mit dem Original-Literal, siehe originText)
                    origin = entry.get("origin", entry.get("mod"))
                    new = origin if origin is not None else mod
                    normalized.append({"id": vid, "remove": True, "mod": mod, "writeBack": new})
                    summary.append(self._line(record, current, new, mod, apply_to_mod, reset=True))
                else:
                    normalized.append({"id": vid, "remove": True, "mod": mod})
                    summary.append(self._line(record, current, mod, mod, apply_to_mod, reset=True))
                continue
            if "value" not in raw:
                errors.append({"id": vid, "message": f"{record['label']}: neuer Wert fehlt"})
                continue
            try:
                new = self.validate_value(record, raw["value"])
            except Invalid as err:
                errors.append({"id": vid, "label": record["label"], "message": str(err)})
                continue
            if vid.startswith("source:") and new is None:
                if vid in entries:
                    normalized.append({"id": vid, "remove": True, "mod": None})
                    summary.append(self._line(record, current, None, None, apply_to_mod))
                continue
            if same(new, current) and not (rollback and vid not in entries):
                skipped.append(vid)
                continue
            normalized.append({"id": vid, "new": new, "mod": mod, "old": current})
            summary.append(self._line(record, current, new, mod, apply_to_mod))
        return {"summary": summary, "errors": errors, "conflicts": conflicts, "changes": normalized, "skipped": skipped}

    def _line(self, record, old, new, mod, apply_to_mod, reset=False):
        warnings = []
        source = record.get("source") or {}
        if record["apply"] == "mod":
            for note in source.get("twinNotes", []):
                warnings.append(note)
            affected = (record.get("refs") or {}).get("usedByItems") or []
            if affected:
                names = sorted({a["item"].split(":")[1] for a in affected})
                warnings.append(f"wirkt auf {len(names)} Items: {', '.join(names[:8])}{' …' if len(names) > 8 else ''}")
            for pin in self.pins(record):
                warnings.append(pin)
        if isinstance(old, (int, float)) and isinstance(new, (int, float)) and not isinstance(old, bool) and old and new:
            factor = max(abs(new / old), abs(old / new)) if old * new > 0 else math.inf
            if factor >= 10:
                warnings.append(f"Faktor {factor:.0f}x gegenüber vorher - Tippfehler?")
        entry_status = None
        where = [{"mc": s.get("mc") or "/".join(source.get("lines") or ["26.2"]), "file": s.get("file"), "line": s.get("line")}
                 for s in applier.sites_of(record)] if record["apply"] == "mod" else []
        return {"id": record["id"], "label": record["label"], "group": record.get("group", ""), "category": record["category"],
                "categoryLabel": CATEGORIES.get(record["category"], record["category"]), "type": record["type"],
                "old": old, "new": new, "mod": mod, "reset": reset, "apply": record["apply"],
                "applyNow": record["apply"] == "mod" and apply_to_mod and not same(new, mod),
                "file": source.get("file"), "line": source.get("line"), "sites": where, "lines": record.get("lines", []),
                "datagen": self.needs_datagen(record),
                "warnings": [w for w in warnings if w], "status": entry_status}

    @staticmethod
    def needs_datagen(record: dict) -> bool:
        """Folgen aus dem Wert erzeugte Dateien (Beute-Tabellen, Verzauberungen, Rezepte, Erze, Item-Export)?"""
        source = record.get("source") or {}
        refs = record.get("refs") or {}
        return bool(isinstance(source.get("generated"), list) or record["category"] == "loot"
                    or refs.get("usedByItems") or refs.get("material"))

    def pins(self, record: dict) -> list[str]:
        """Spieltests, die den heutigen Wert festhalten (Hinweis im Speichern-Dialog)."""
        out = []
        if record["category"] == "config":
            text = self._pin_text()
            path = record["refs"].get("path", "")
            parts = path.rsplit(".", 1)
            owner = parts[0].split(".")[-1] if len(parts) == 2 else "root"
            needle = f"{owner}.{parts[-1]} "
            if needle in text or f"root.{path} " in text:
                out.append("Spieltest ConfigOptionTests hält diesen Standard fest (EXPECTED_OPTIONS) - dort mitändern, "
                           "sonst wird das Gate rot")
        if record["id"].endswith("_CORE_CHANCE") or (record["category"] == "loot" and "Kern" in record.get("group", "")):
            out.append("Spieltest config_option_building_cores_are_very_rare_in_loot_chests prüft Bänder um die Kern-Chancen "
                       "(ConfigOptionTests.CORE_CHANCES) - ein Wert außerhalb macht ihn rot")
        return out

    def _pin_text(self) -> str:
        if not hasattr(self, "_pins_cache"):
            path = self.repo / "common/src/shared/java/com/simplebuilding/gametest/ConfigOptionTests.java"
            try:
                self._pins_cache = path.read_text(encoding="utf-8")
            except OSError:
                self._pins_cache = ""
        return self._pins_cache

    def preview(self, payload: dict) -> dict:
        state = self.store.state()
        base = payload.get("baseVersion")
        result = self.check(state, payload.get("changes"), payload.get("applyToMod", True))
        result.pop("changes")
        result["baseVersion"] = state["version"]
        result["stale"] = base != state["version"]
        result["nextVersion"] = max([h["version"] for h in self.store.history()]) + 1
        return result

    def save(self, payload: dict, *, rollback_of: int | None = None, rollback: bool = False) -> dict:
        with self._lock:
            state = self.store.state()
            apply_to_mod = bool(payload.get("applyToMod", True)) and not self.read_only
            result = self.check(state, payload.get("changes"), apply_to_mod, rollback=rollback)
            if result["errors"]:
                raise StoreError(400, "Ungültige Werte - nichts wurde gespeichert.", result["errors"])
            if result["conflicts"]:
                raise StoreError(409, "Werte wurden inzwischen geändert - nichts wurde gespeichert.", result["conflicts"])
            to_apply = []
            entries = state["entries"]
            for change in result["changes"]:
                record = self.record_for(change["id"])
                if not record or record["apply"] != "mod":
                    continue
                entry = entries.get(change["id"]) or {}
                if not entry.get("originText") and not change.get("remove"):
                    change["originText"] = applier.origin_tokens(record)
                if change.get("remove"):
                    # Plan verworfen: steht ein anderer Wert in der Mod als beim ersten Planen, zurück aufs Original
                    back = change.get("writeBack")
                    if apply_to_mod and back is not None and not same(back, record["value"]):
                        to_apply.append((record, back, entry.get("originText")))
                    continue
                prefer = entry.get("originText") if same(change["new"], entry.get("origin")) else None
                if same(change["new"], record["value"]):
                    change["applied"] = True          # steht schon so in der Mod (z. B. Rücksetzen)
                elif apply_to_mod:
                    to_apply.append((record, change["new"], prefer))
                    change["applied"] = True

            def before_write(_record):
                problems = applier.check_all(self.repo, to_apply)
                if problems:
                    raise StoreError(409, "Mod-Dateien wurden seit dem Einlesen geändert - nichts wurde gespeichert.", problems)

            def after_version(_record):
                try:
                    return applier.write_all(self.repo, to_apply)
                except (applier.ApplyConflict, javaedit.JavaEditError, OSError) as err:
                    raise StoreError(500, f"Schreiben in die Mod-Dateien fehlgeschlagen ({err}) - die Dateien sind "
                                          "unverändert; die Version ist angelegt, die Werte stehen als 'geplant' darin.") from None

            new_state = self.store.commit(payload.get("baseVersion"), result["changes"], payload.get("message", ""),
                                          rollback_of=rollback_of, before_write=before_write, after_version=after_version)
            if to_apply:
                self.reload()
            datagen = any(self.needs_datagen(item[0]) for item in to_apply)
            return {"version": new_state["version"], "applied": len(to_apply), "summary": result["summary"],
                    "datagen": datagen}

    # ---- Rollback --------------------------------------------------------------------------

    def rollback_changes(self, target: int) -> list[dict]:
        state = self.store.state()
        goal = self.store.version(int(target))["entries"]
        changes = []
        for vid in sorted(set(state["entries"]) | set(goal)):
            now = state["entries"].get(vid)
            then = goal.get(vid)
            if then is not None:
                if now is None or not same(now["value"], then["value"]) or now["value"] != then["value"]:
                    changes.append({"id": vid, "value": then["value"]})
            elif now is not None:
                changes.append({"id": vid, "reset": True})
        return changes

    def rollback_preview(self, payload: dict) -> dict:
        target = int(payload.get("target"))
        state = self.store.state()
        changes = self.rollback_changes(target)
        result = self.check(state, changes, payload.get("applyToMod", True), rollback=True)
        result.pop("changes")
        result.update(target=target, baseVersion=state["version"], nextVersion=max(h["version"] for h in self.store.history()) + 1)
        return result

    def rollback(self, payload: dict) -> dict:
        target = int(payload.get("target"))
        changes = self.rollback_changes(target)
        if not changes:
            raise StoreError(400, f"Der aktuelle Stand entspricht schon v{target} - nichts zu tun.")
        message = payload.get("message") or f"Rückgängig: Stand von v{target}"
        return self.save({"baseVersion": payload.get("baseVersion"), "changes": changes, "message": message,
                          "applyToMod": payload.get("applyToMod", True)}, rollback_of=target, rollback=True)

    # ---- Geplante Handelswerte nachträglich anwenden ------------------------------------

    def pending_apply(self) -> list[dict]:
        state = self.store.state()
        out = []
        for vid, entry in state["entries"].items():
            record = self.record_for(vid)
            if record and record["apply"] == "mod" and not same(entry["value"], record["value"]):
                out.append({"id": vid, "label": record["label"], "group": record["group"], "old": record["value"],
                            "new": entry["value"], "file": record["source"].get("file"), "lines": record.get("lines", []),
                            "datagen": self.needs_datagen(record)})
        return out

    def apply_planned(self, payload: dict) -> dict:
        if self.read_only:
            raise StoreError(403, "Diese Zentrale ist schreibgeschützt (read_only) - nichts geschrieben.")
        with self._lock:
            pending = self.pending_apply()
            wanted = set(payload.get("ids") or [p["id"] for p in pending])
            chosen = [p for p in pending if p["id"] in wanted]
            if not chosen:
                raise StoreError(400, "Keine geplanten Werte, die noch nicht in der Mod stehen.")
            entries = self.store.state()["entries"]
            items = [(self.record_for(p["id"]), p["new"],
                      entries.get(p["id"], {}).get("originText") if same(p["new"], entries.get(p["id"], {}).get("origin")) else None)
                     for p in chosen]
            problems = applier.check_all(self.repo, items)
            if problems:
                raise StoreError(409, "Mod-Dateien wurden seit dem Einlesen geändert - nichts geschrieben.", problems)
            done = applier.write_all(self.repo, items)
            log = self.store.root / "applied-log.jsonl"
            self.store.root.mkdir(parents=True, exist_ok=True)
            with open(log, "a", encoding="utf-8") as handle:
                for d in done:
                    handle.write(json.dumps(dict(d, at=time.strftime("%Y-%m-%dT%H:%M:%S"), version=self.store.state()["version"])) + "\n")
            self.reload()
            return {"applied": len(chosen), "sites": len(done),
                    "datagen": any(self.needs_datagen(item[0]) for item in items)}

    # ---- Rechner -----------------------------------------------------------------------------

    def _overrides(self, payload) -> dict:
        overrides = payload.get("overrides") or {}
        if not isinstance(overrides, dict):
            raise StoreError(400, "overrides muss ein Objekt sein.")
        clean = {}
        for vid, raw in overrides.items():
            record = self.record_for(vid)
            if record is None:
                continue
            if isinstance(raw, dict) and raw.get("reset"):
                entry_val = self.snapshot["values"].get(vid, {}).get("value")
                clean[vid] = entry_val
                continue
            try:
                clean[vid] = self.validate_value(record, raw)
            except Invalid:
                continue  # ungueltige Entwuerfe rechnen nicht mit (die Oberfläche zeigt den Fehler am Feld)
        return clean

    def calc(self, payload: dict) -> dict:
        item = payload.get("item")
        if not isinstance(item, str):
            raise StoreError(400, "item fehlt.")
        ctx = self.ctx(self._overrides(payload))
        report = model.item_report(ctx, item, int(payload.get("k", model.KMAX)))
        return _finite(report)

    def reverse(self, payload: dict) -> dict:
        overrides = self._overrides(payload)
        entries = self.store.state()["entries"]
        tunable = payload.get("tunable") or {}
        if not isinstance(tunable, dict) or not self.record_for(tunable.get("id", "")):
            raise StoreError(400, "Stellwert unbekannt.")
        try:
            hours = float(str(payload.get("hours")).replace(",", "."))
            k = int(payload.get("k", 1))
        except (TypeError, ValueError):
            raise StoreError(400, "Zielzeit und Stückzahl müssen Zahlen sein.") from None
        if not (0 < hours < 1e6) or not (1 <= k <= 64):
            raise StoreError(400, "Zielzeit muss > 0 sein, Stückzahl 1 bis 64.")
        stat = payload.get("stat", "mean")
        if stat not in ("mean", "median"):
            raise StoreError(400, "stat muss mean oder median sein.")

        def ctx_with(extra):
            merged = dict(overrides)
            merged.update(extra)
            return model.Ctx(self.snapshot, self.effective_fn(entries, merged), self.custom_ids(entries, merged))
        result = model.reverse(self.snapshot, ctx_with, payload.get("item"), payload.get("row") or "__best__", tunable, k,
                               stat, payload.get("mode", "targeted"), hours)
        return _finite(result)

    def overview(self, payload: dict) -> dict:
        ctx = self.ctx(self._overrides(payload))
        keys = sorted(set(self.snapshot["sources"]) | set(ctx.custom))
        offers = {t["id"]: model.trade_offer_chance(ctx, t)[0] for t in self.snapshot["trades"]}
        return {"rows": _finite(model.overview(ctx, keys)), "offers": _finite(offers)}

    # ---- Übergabe: was geplant ist, aber (noch) nicht in der Mod wirkt ------------------------

    def phase2(self) -> dict:
        """Geplante Werte, die die Zentrale nicht schreiben kann (apply "plan", Quellen) oder die noch nicht
        angewendet sind - mit Grund, Datei und Zeile. (Name aus Phase 1; Route /api/phase2 und /api/handover.)"""
        state = self.store.state()
        groups: dict[str, list] = {}
        for vid, entry in sorted(state["entries"].items()):
            record = self.record_for(vid)
            if record is None:
                groups.setdefault("orphan", []).append({"id": vid, "planned": entry["value"], "note": "Wert gibt es in der Mod nicht mehr"})
                continue
            if record["apply"] == "tool":
                continue
            if record["apply"] == "mod" and same(entry["value"], record["value"]):
                continue  # schon in der Mod
            if record["category"] != "source" and same(entry["value"], record["value"]):
                continue
            groups.setdefault(record["category"], []).append({
                "id": vid, "label": record["label"], "group": record.get("group"), "mod": record.get("value"),
                "planned": entry["value"], "source": record.get("source"), "refs": record.get("refs"),
                "apply": record["apply"], "why": record.get("note") if record["apply"] != "mod" else
                "gespeichert, aber noch nicht in die Mod geschrieben ('Jetzt anwenden' auf der Übersicht)"})
        return {"version": state["version"], "generatedAt": time.strftime("%Y-%m-%dT%H:%M:%S"), "groups": groups,
                "howTo": "docs/BALANCING-ZENTRALE.md"}

    # ---- checkBalance und Datagen -------------------------------------------------------------

    def balance_check(self) -> dict:
        self.maybe_reload()
        return balance_checker.run(self.snapshot, self.store.state(), self.repo, self.store.root, self.record_for)

    def start_datagen(self, payload: dict) -> dict:
        if self.read_only:
            raise StoreError(403, "Diese Zentrale ist schreibgeschützt (read_only) - kein Datagen.")
        with self._lock:
            if self.job and self.job.status == "running":
                raise StoreError(409, "Datagen läuft schon.")
            if not payload.get("force"):
                games = jobs.running_dev_games(self.repo) if self.datagen_steps is None else []
                if games:
                    raise StoreError(409, "Es laufen Dev-Clients/-Server (runClient/runServer). Datagen übersetzt die Mod in "
                                          "diesem Checkout neu - das laufende Spiel stürzt dann mit NoClassDefFoundError ab. "
                                          "Spiel beenden oder 'trotzdem starten'.", [{"message": g} for g in games[:5]])
            steps = self.datagen_steps or jobs.default_steps(self.repo, bool(payload.get("include264")))

            def done(job):
                self.reload()
                return {"check": self.balance_check(), "diff": jobs.git_diff_stat(self.repo)}
            self.job = jobs.Job(steps, self.repo, on_done=done).start()
            return self.job.payload()

    def datagen_status(self) -> dict:
        if not self.job:
            return {"status": "idle"}
        return self.job.payload()

    def cancel_datagen(self, _payload=None) -> dict:
        if not self.job or self.job.status != "running":
            raise StoreError(400, "Es läuft kein Datagen.")
        self.job.cancel()
        return {"status": "cancelling"}

    # ---- Doku --------------------------------------------------------------------------------

    def doc(self, name: str) -> dict:
        return docs.render_file(self.repo, name)


def validate_eras(raw) -> list:
    if not isinstance(raw, list) or not raw:
        raise Invalid("Zeitalter: erwartet eine nicht leere Liste.")
    if len(raw) > 40:
        raise Invalid("Zeitalter: höchstens 40 Einträge.")
    out = []
    for i, era in enumerate(raw, 1):
        if not isinstance(era, dict):
            raise Invalid(f"Zeitalter {i}: erwartet ein Objekt mit Name und Stunden.")
        name = str(era.get("name") or "").strip()
        if not name or len(name) > 80:
            raise Invalid(f"Zeitalter {i}: Name fehlt oder ist länger als 80 Zeichen.")
        try:
            hours = float(str(era.get("hours")).replace(",", "."))
        except (TypeError, ValueError):
            raise Invalid(f"Zeitalter {i} ({name}): Stunden sind keine Zahl.") from None
        if not (0 < hours <= 10000) or not math.isfinite(hours):
            raise Invalid(f"Zeitalter {i} ({name}): Stunden müssen zwischen 0 und 10000 liegen.")
        out.append({"id": str(era.get("id") or f"e{i}")[:40], "name": name, "hours": hours})
    return out


def validate_source_spec(raw) -> dict | None:
    if raw is None:
        return None
    if not isinstance(raw, dict):
        raise Invalid("Quelle: erwartet ein Objekt.")
    kind = raw.get("kind")
    if kind not in SOURCE_KINDS:
        raise Invalid(f"Quelle: Art muss eine von {', '.join(SOURCE_KINDS)} sein.")
    out = {"kind": kind}
    try:
        chance = float(str(raw.get("chance")).replace(",", "."))
    except (TypeError, ValueError):
        raise Invalid("Quelle: Chance fehlt oder ist keine Zahl.") from None
    if not (0 < chance <= 1) or not math.isfinite(chance):
        raise Invalid("Quelle: Chance muss zwischen 0 und 100 % liegen (größer als 0).")
    out["chance"] = chance
    try:
        lo = int(raw.get("countMin", 1))
        hi = int(raw.get("countMax", lo))
    except (TypeError, ValueError):
        raise Invalid("Quelle: Anzahl muss eine ganze Zahl sein.") from None
    if not (1 <= lo <= hi <= 64):
        raise Invalid("Quelle: Anzahl min/max muss 1 <= min <= max <= 64 sein.")
    out.update(countMin=lo, countMax=hi)
    label = str(raw.get("label") or "").strip()
    if len(label) > 80:
        raise Invalid("Quelle: Name höchstens 80 Zeichen.")
    out["label"] = label or SOURCE_KINDS[kind]
    note = str(raw.get("note") or "").strip()
    if len(note) > 500:
        raise Invalid("Quelle: Notiz höchstens 500 Zeichen.")
    if note:
        out["note"] = note
    if kind == "structure":
        skey = raw.get("structure")
        if skey not in params.STRUCTURES:
            raise Invalid("Quelle: unbekannte Struktur.")
        out["structure"] = skey
        ckey = raw.get("container") or next(iter(params.STRUCTURES[skey]["containers"]))
        if ckey not in params.STRUCTURES[skey]["containers"]:
            raise Invalid("Quelle: unbekannter Behälter in der Struktur.")
        out["container"] = ckey
        if raw.get("table"):
            out["table"] = str(raw["table"])[:120]
    elif kind != "trader":
        try:
            rate = float(str(raw.get("rate")).replace(",", "."))
        except (TypeError, ValueError):
            raise Invalid("Quelle: Ereignisse je Stunde fehlen oder sind keine Zahl.") from None
        if not (0 <= rate <= 10000) or not math.isfinite(rate):
            raise Invalid("Quelle: Ereignisse je Stunde müssen zwischen 0 und 10000 liegen.")
        out["rate"] = rate
        normal = raw.get("rateNormal")
        if normal not in (None, ""):
            try:
                normal = float(str(normal).replace(",", "."))
            except ValueError:
                raise Invalid("Quelle: normale Rate ist keine Zahl.") from None
            if not (0 <= normal <= 10000):
                raise Invalid("Quelle: normale Rate muss zwischen 0 und 10000 liegen.")
            out["rateNormal"] = normal
        if kind == "mob":
            out["mob"] = str(raw.get("mob") or "")[:80]
        if kind == "block":
            out["block"] = str(raw.get("block") or "")[:80]
    return out


def _finite(data):
    """inf/nan -> None (JSON kennt kein Infinity)."""
    if isinstance(data, float):
        return data if math.isfinite(data) else None
    if isinstance(data, dict):
        return {k: _finite(v) for k, v in data.items()}
    if isinstance(data, list):
        return [_finite(v) for v in data]
    return data


def _rel(path: Path, repo: Path) -> str:
    try:
        return Path(path).resolve().relative_to(repo.resolve()).as_posix()
    except ValueError:
        return str(path)
