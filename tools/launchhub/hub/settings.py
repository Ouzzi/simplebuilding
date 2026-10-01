"""Hub settings: defaults, validation, persistence in tools/launchhub/settings.json (gitignored)."""

from __future__ import annotations

import json
import os
import tempfile
import threading

from . import paths

SETTINGS_FILE = paths.HUB_DIR / "settings.json"
_LOCK = threading.Lock()

#: Editable command templates. Placeholders (one argv element each, never shell-parsed):
#: {prompt_file} path of the generated prompt, {prompt} the prompt text, {worktree}, {branch}.
#: The flags below follow the public docs of both CLIs but were NOT verified on the owner's machine
#: (neither CLI was installed while this was built) - check `claude --help` / `codex exec --help`.
DEFAULT_PROVIDERS = {
    "claude": {
        "label": "Claude Code",
        "executable": "claude",
        "template": "claude -p --permission-mode acceptEdits",
        "stdin": True,
        "install": "npm install -g @anthropic-ai/claude-code   (or the native installer from the Claude Code docs)",
    },
    "codex": {
        "label": "OpenAI Codex",
        "executable": "codex",
        "template": "codex exec --full-auto -C {worktree} -",
        "stdin": True,
        "install": "npm install -g @openai/codex",
    },
}

DEFAULTS = {
    "minFreeGb": 8,
    "testWorkspace": "gate",        # gate | repo
    "launchWorkspace": "repo",
    "quickPlay": True,              # add --quickPlaySingleplayer SB-Testzentrale when the world exists
    "dryRun": False,
    "historyDepth": 40,             # full sweeps in trends
    "stateDepth": 200,              # records scanned for "latest state per test"
    "includeMutations": False,
    "defaultProvider": "claude",
    "providers": DEFAULT_PROVIDERS,
    "baseRef": "HEAD",
}

_LIMITS = {"minFreeGb": (1, 500), "historyDepth": (5, 200), "stateDepth": (20, 600)}


class SettingsError(ValueError):
    pass


def _clone(value):
    return json.loads(json.dumps(value))


def load() -> dict:
    data = _clone(DEFAULTS)
    if SETTINGS_FILE.is_file():
        try:
            stored = json.loads(SETTINGS_FILE.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            stored = {}
        if isinstance(stored, dict):
            try:
                data = merge(data, stored)
            except SettingsError:
                pass
    if os.environ.get("SB_HUB_DRY_RUN"):
        data["dryRun"] = True
    if production():
        data["dryRun"] = False
    return data


def production() -> bool:
    """Started by the Zentrale (tools/zentrale): always real, never a dry run (owner 2026-10-01)."""
    return os.environ.get("SB_HUB_PRODUCTION") == "1"


def _plain_ref(value) -> bool:
    return isinstance(value, str) and 0 < len(value) < 100 and not value.startswith("-") and all(
        ch.isalnum() or ch in "/-_." for ch in value)


def merge(base: dict, patch: dict) -> dict:
    """Validates `patch` and returns base updated with it. Unknown keys are refused."""
    out = _clone(base)
    for key, value in patch.items():
        if key not in DEFAULTS:
            raise SettingsError(f"unknown setting: {key}")
        if key in _LIMITS:
            low, high = _LIMITS[key]
            if isinstance(value, bool) or not isinstance(value, (int, float)) or not low <= value <= high:
                raise SettingsError(f"{key} must be a number between {low} and {high}")
            out[key] = value
        elif key in ("quickPlay", "dryRun", "includeMutations"):
            if not isinstance(value, bool):
                raise SettingsError(f"{key} must be true or false")
            out[key] = value
        elif key in ("testWorkspace", "launchWorkspace"):
            if value not in ("gate", "repo"):
                raise SettingsError(f"{key} must be 'gate' or 'repo'")
            out[key] = value
        elif key == "baseRef":
            if not _plain_ref(value):
                raise SettingsError("baseRef must be a branch name, tag or HEAD")
            out[key] = value
        elif key == "defaultProvider":
            if value not in DEFAULT_PROVIDERS:
                raise SettingsError("defaultProvider must be claude or codex")
            out[key] = value
        elif key == "providers":
            if not isinstance(value, dict):
                raise SettingsError("providers must be an object")
            for pid, conf in value.items():
                if pid not in DEFAULT_PROVIDERS or not isinstance(conf, dict):
                    raise SettingsError(f"unknown provider: {pid}")
                for field in ("template", "executable"):
                    if field in conf:
                        text = conf[field]
                        if not isinstance(text, str) or not text.strip() or len(text) > 2000 or "\x00" in text or "\n" in text:
                            raise SettingsError(f"{pid}.{field} must be one non-empty line")
                        out["providers"][pid][field] = text.strip()
                if "stdin" in conf:
                    if not isinstance(conf["stdin"], bool):
                        raise SettingsError(f"{pid}.stdin must be true or false")
                    out["providers"][pid]["stdin"] = conf["stdin"]
    return out


def save(patch: dict) -> dict:
    with _LOCK:
        merged = merge(load(), patch)
        stored = _clone(merged)
        if os.environ.get("SB_HUB_DRY_RUN") and "dryRun" not in patch:
            stored["dryRun"] = False
        SETTINGS_FILE.parent.mkdir(parents=True, exist_ok=True)
        fd, tmp = tempfile.mkstemp(dir=str(SETTINGS_FILE.parent), suffix=".tmp")
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(stored, handle, indent=2, ensure_ascii=False)
        os.replace(tmp, SETTINGS_FILE)
        return merged


def dry_run() -> str:
    """'' (real), '1' (echo only) or 'sim' (spawn a harmless simulated process, for UI checks)."""
    if production():
        return ""
    env = (os.environ.get("SB_HUB_DRY_RUN") or "").strip().lower()
    if env:
        return "sim" if env == "sim" else "1"
    return "1" if load()["dryRun"] else ""
