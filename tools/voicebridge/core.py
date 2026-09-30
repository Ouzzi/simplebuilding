"""Configuration, owner commands, confirmations, and bounded file proposals."""
from __future__ import annotations

import hashlib
import ipaddress
import json
import os
from pathlib import Path
import re
import secrets
import time
from urllib.parse import urlsplit

PROVIDERS = {"claude", "codex", "ollama", "echo"}
WORDS = ("anker", "birke", "delfin", "fackel", "garten", "insel", "kiesel", "laterne")


def tailscale_ip(value):
    try:
        return ipaddress.ip_address(value) in ipaddress.ip_network("100.64.0.0/10")
    except ValueError:
        return False


def validate_host(value):
    if value in ("127.0.0.1", "localhost") or tailscale_ip(value):
        return value
    raise ValueError("Bind address must be loopback or a Tailscale IPv4 address")


def valid_name(value):
    return isinstance(value, str) and bool(re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_.-]{0,63}", value))


def host_allowed(header, allowed, port):
    if not header or any(c in header for c in "/\\@ ,\r\n"):
        return False
    try:
        parsed = urlsplit("http://" + header)
        return parsed.hostname in allowed and parsed.port in (None, port, 443)
    except ValueError:
        return False


def authenticated(header, token):
    return isinstance(header, str) and header.isascii() and secrets.compare_digest(header, "Bearer " + token)


def safe_path(root, relative):
    """Reject traversal, metadata, symlinks/junction escapes and Windows aliases."""
    if not isinstance(relative, str) or not relative or "\\" in relative or ":" in relative:
        raise ValueError("Use a project-relative path with forward slashes")
    parts = relative.split("/")
    reserved = {"con", "prn", "aux", "nul", *(f"com{i}" for i in range(10)), *(f"lpt{i}" for i in range(10))}
    if any(p in ("", ".", "..") or p.startswith(".") or p.endswith((" ", "."))
           or p.split(".")[0].lower() in reserved for p in parts):
        raise ValueError("Unsafe path")
    base = Path(root).resolve()
    candidate = base.joinpath(*parts)
    if not candidate.resolve().is_relative_to(base):
        raise ValueError("Path escapes project")
    current = base
    for part in parts:
        current /= part
        if current.is_symlink() or (hasattr(current, "is_junction") and current.is_junction()):
            raise ValueError("Linked paths are not writable")
    return candidate


def load_projects(path, repo):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(data, list) or not data or len(data) > 32:
        raise ValueError("Projects must be a nonempty list (maximum 32)")
    projects = {}
    for row in data:
        if not isinstance(row, dict) or not valid_name(row.get("name")):
            raise ValueError("Invalid project name")
        name = row["name"].lower()
        if name in projects or row.get("provider", "claude") not in PROVIDERS:
            raise ValueError("Duplicate project or unknown provider")
        root_value = row.get("root")
        if not isinstance(root_value, str) or not root_value:
            raise ValueError("Missing project root")
        root = (Path(repo) / root_value).resolve()
        if not root.is_dir():
            raise ValueError("Project root does not exist")
        if row.get("default_mode", "read-only") != "read-only":
            raise ValueError("Default mode must be read-only; every action needs confirmation")
        model = row.get("model", "")
        if not isinstance(model, str) or len(model) > 160 or model.startswith("-") or any(ord(c) < 32 for c in model):
            raise ValueError("Invalid model")
        brief = row.get("brief_folder", "docs/ai/briefs")
        safe_path(root, brief)
        projects[name] = dict(row, name=name, root=str(root), provider=row.get("provider", "claude"),
                              model=model, default_mode="read-only", brief_folder=brief)
    return projects


def normalize(text):
    return re.sub(r"\s+", " ", text.casefold()).strip().rstrip(".!?")


def command(text):
    value = normalize(text)
    aliases = {
        "status": "status", "was ist der status": "status",
        "what failed": "failed", "was ist fehlgeschlagen": "failed", "was ging schief": "failed",
        "read last report": "report", "lies den letzten bericht": "report",
        "new conversation": "new", "neues gespräch": "new", "neue unterhaltung": "new",
        "stop": "quiet", "quiet": "quiet", "stopp": "quiet", "ruhe": "quiet",
        "repeat": "repeat", "wiederholen": "repeat", "wiederhole": "repeat",
        "notaus": "emergency", "emergency stop": "emergency", "brücke aus": "emergency",
    }
    if value in aliases:
        return aliases[value], ""
    for prefix in ("switch project ", "projekt wechseln "):
        if value.startswith(prefix):
            return "switch", value[len(prefix):]
    for prefix in ("aktion ", "action "):
        if value.startswith(prefix):
            return "action", text[len(prefix):].strip()
    return "ask", text


class Confirmation:
    def __init__(self, clock=time.monotonic):
        self.clock = clock
        self.pending = None

    def offer(self, project, description, payload):
        word = secrets.choice(WORDS) + secrets.choice(WORDS)
        self.pending = (project, word, self.clock() + 60, payload)
        return f"{description} Sage innerhalb einer Minute genau: {word}."

    def consume(self, project, transcript):
        pending, self.pending = self.pending, None
        if not pending or pending[0] != project or self.clock() > pending[2] or normalize(transcript) != pending[1]:
            raise ValueError("Bestätigung abgelehnt oder abgelaufen. Bitte die Aktion neu anfragen.")
        return pending[3]


def spoken_answer(raw):
    """Keep full provider output separately; never speak paths, code or links."""
    obj = None
    try:
        obj = json.loads(raw.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip())
    except (ValueError, AttributeError):
        pass
    if isinstance(obj, dict) and isinstance(obj.get("spoken"), str):
        text = obj["spoken"]
        details = obj.get("details", raw)
        if not isinstance(details, str):
            details = json.dumps(details, ensure_ascii=False)
    else:
        text, details = raw, raw
    text = re.sub(r"```.*?```", "", text, flags=re.S)
    text = re.sub(r"https?://\S+|\S*[/\\]\S+|[A-Za-z]:\S+", "", text)
    text = re.sub(r"[*#`_<>]", "", text)
    sentences = re.split(r"(?<=[.!?])\s+", text.strip())
    short = " ".join(sentences[:4])[:650].strip()
    return short or "Die Antwort liegt im Verlauf bereit.", details, obj


class Audit:
    def __init__(self, path):
        self.path = Path(path)

    def write(self, event, **fields):
        # Never log Authorization, token, confirmation word or raw request headers.
        clean = {k: v for k, v in fields.items() if k not in {"token", "authorization", "word"}}
        with self.path.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(dict(time=time.time(), event=event, **clean), ensure_ascii=False) + "\n")


class RateLimit:
    def __init__(self, limit=60, clock=time.monotonic):
        self.limit, self.clock, self.clients = limit, clock, {}

    def allow(self, key):
        now = self.clock()
        self.clients = {k: [t for t in v if now-t < 60] for k, v in self.clients.items() if v and now-v[-1] < 60}
        values = self.clients.setdefault(key, [])
        if len(values) >= self.limit:
            return False
        values.append(now)
        return True


def prepare_edits(root, obj):
    """Agents propose content; the bridge alone writes after owner confirmation."""
    edits = obj.get("edits") if isinstance(obj, dict) else None
    if not isinstance(edits, list) or not 1 <= len(edits) <= 8:
        raise ValueError("Keine begrenzten Dateivorschläge vorhanden; nichts geändert.")
    prepared, seen = [], set()
    for edit in edits:
        if not isinstance(edit, dict):
            raise ValueError("Invalid edit")
        relative, content = edit.get("path"), edit.get("content")
        target = safe_path(root, relative)
        if relative.startswith(("tools/voicebridge/", "mc1_21_11/", "mc26_4/", "common/src/mc26_2/", "mc26_2/")):
            raise ValueError("Geschützter Pfad")
        if target in seen or not isinstance(content, str) or not content.strip() or len(content.encode()) > 65536:
            raise ValueError("Empty, duplicate or oversized edit")
        seen.add(target)
        old = target.read_bytes() if target.exists() else None
        if old is not None:
            if len(old) > 65536:
                raise ValueError("Existing file is too large")
            old.decode("utf-8")
        prepared.append(dict(path=relative, content=content, old=old,
                             digest=hashlib.sha256(old).hexdigest() if old is not None else None))
    return prepared


def apply_edits(root, edits, backup_dir):
    # Preflight every file before any write. Backups are immutable and never deleted.
    targets = []
    for edit in edits:
        path = safe_path(root, edit["path"])
        current = path.read_bytes() if path.exists() else None
        digest = hashlib.sha256(current).hexdigest() if current is not None else None
        if digest != edit["digest"]:
            raise ValueError("Datei inzwischen geändert; neue Bestätigung erforderlich.")
        targets.append(path)
    backups = Path(backup_dir) / secrets.token_hex(12)
    backups.mkdir(parents=True)
    for index, (path, edit) in enumerate(zip(targets, edits)):
        if edit["old"] is not None:
            (backups / f"{index}.bak").write_bytes(edit["old"])
        (backups / f"{index}.json").write_text(json.dumps({"path": edit["path"]}), encoding="utf-8")
        path.parent.mkdir(parents=True, exist_ok=True)
        temp = path.with_name(path.name + ".voicebridge-" + secrets.token_hex(8))
        temp.write_text(edit["content"], encoding="utf-8")
        os.replace(temp, path)
    return len(targets)
