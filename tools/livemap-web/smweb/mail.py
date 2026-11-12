"""Outgoing e-mail: SMTP from the server config, or - without an SMTP host - files in <data>/outbox (development; the admin
page lists them). Mails are plain text, one link each; nothing else of the account is put into a mail."""
from __future__ import annotations

import json
import os
import smtplib
import ssl
import time
from email.message import EmailMessage
from pathlib import Path


class Mailer:
    def __init__(self, smtp: dict, outbox: Path):
        self.smtp = smtp or {}
        self.outbox = Path(outbox)
        self.sent: list[dict] = []          # last mails of this process (tests)

    @property
    def uses_outbox(self) -> bool:
        return not self.smtp.get("host")

    def send(self, to: str, subject: str, text: str):
        mail = {"to": to, "subject": subject, "text": text, "at": int(time.time())}
        self.sent = (self.sent + [mail])[-50:]
        if self.uses_outbox:
            self.outbox.mkdir(parents=True, exist_ok=True)
            name = f"{time.time_ns()}.json"
            fd = os.open(self.outbox / name, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            with os.fdopen(fd, "w", encoding="utf-8") as f:
                json.dump(mail, f, ensure_ascii=False, indent=1)
            return
        msg = EmailMessage()
        msg["From"] = self.smtp.get("from") or "no-reply@simplemods.de"
        msg["To"] = to
        msg["Subject"] = subject
        msg.set_content(text)
        with smtplib.SMTP(self.smtp["host"], int(self.smtp.get("port") or 587), timeout=15) as s:
            if self.smtp.get("starttls", True):
                s.starttls(context=ssl.create_default_context())
            if self.smtp.get("user"):
                s.login(self.smtp["user"], self.smtp.get("password", ""))
            s.send_message(msg)

    def recent(self, limit: int = 20) -> list[dict]:
        if not self.outbox.is_dir():
            return []
        out = []
        for f in sorted(self.outbox.glob("*.json"), reverse=True)[:limit]:
            try:
                out.append(json.loads(f.read_text(encoding="utf-8")))
            except (OSError, ValueError):
                pass
        return out
