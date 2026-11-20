"""Payment providers behind one narrow interface (N108 addendum 3/4). ``billing.provider`` picks the provider for new orders
(default ``paddle``); every provider keeps its own webhook route and secret."""
from __future__ import annotations

from .base import Event, PaymentProvider  # noqa: F401
from .paddle import PaddleProvider
from .stripe import StripeProvider

DEFAULT = "paddle"
PROVIDERS = {"paddle": PaddleProvider, "stripe": StripeProvider}


def make(svc, name: str) -> PaymentProvider:
    try:
        return PROVIDERS[name](svc)
    except KeyError:
        raise ValueError(f"unknown payment provider {name!r} (billing.provider: {', '.join(PROVIDERS)})") from None
