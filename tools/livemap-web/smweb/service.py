"""The service object: configuration, database, clock, rate limiter, mailer and the domain logic (accounts, maps, upload,
billing). The HTTP layer (app.py) only parses requests and renders answers."""
from __future__ import annotations

import time

from .accounts import AccountsMixin
from .billing import BillingMixin
from .config import Config
from .db import Database
from .early import EarlyMixin
from .mail import Mailer
from .maps import MapsMixin
from .security import Limiter
from .upload import UploadMixin


class Service(AccountsMixin, MapsMixin, UploadMixin, BillingMixin, EarlyMixin):
    def __init__(self, cfg: Config, clock=time.time):
        self.cfg = cfg
        self.clock = clock
        self.db = Database(cfg.data / "smweb.sqlite3")
        self.limiter = Limiter(clock)
        self.mailer = Mailer(cfg["smtp"], cfg.data / "outbox")
        self.live_cache: dict[int, tuple[list, int]] = {}
        self._storage = None

    def now(self) -> int:
        return int(self.clock())
