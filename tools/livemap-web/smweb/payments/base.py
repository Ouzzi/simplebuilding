"""Narrow payment provider interface (N108 addendum 3): the billing logic (orders, entitlements, gifts, seats, legal flow) talks
only to this interface and to the unified ``Event``s, never to a provider's API objects. A provider is chosen per config
(``billing.provider``); its webhook route verifies with that provider's own secret."""
from __future__ import annotations

from dataclasses import dataclass, field

# unified event kinds
CHECKOUT_COMPLETED = "checkout_completed"      # subscription order paid / subscription created from our order
GIFT_PAID = "gift_paid"                        # one-off gift purchase paid
SUB_STARTED = "subscription_started"
SUB_CHANGED = "subscription_changed"
SUB_ENDED = "subscription_ended"
PAYMENT_SUCCEEDED = "payment_succeeded"
PAYMENT_FAILED = "payment_failed"
REFUND = "refund"                              # refund or chargeback (approved)
KINDS = (CHECKOUT_COMPLETED, GIFT_PAID, SUB_STARTED, SUB_CHANGED, SUB_ENDED, PAYMENT_SUCCEEDED, PAYMENT_FAILED, REFUND)


@dataclass
class Event:
    kind: str
    created: int = 0
    user_hint: str | None = None        # our account id as sent with the order (custom data / metadata)
    customer: str | None = None         # provider customer id
    order_id: int | None = None         # our order id from the custom data
    sub_id: str | None = None
    ref: str | None = None              # checkout session / transaction / adjustment id
    plan: str | None = None             # plan from the provider's price id (None = unknown price)
    meta_plan: str | None = None        # plan / livemap tier written into the custom data (fallbacks)
    meta_tier: str | None = None
    interval: str | None = None
    status: str | None = None           # active, trialing, past_due, canceled, paused, ...
    period_end: int | None = None
    cancel_at_period_end: bool = False
    paid: bool = True                   # gift: payment really done
    amount: int = 0                     # cents
    number: str = ""                    # invoice number
    link: str = ""                      # invoice page
    pdf: str = ""
    attempt: int = 0
    action: str = ""                    # refund: "refund" | "chargeback"
    extra: dict = field(default_factory=dict)


class PaymentProvider:
    """What the billing logic needs from a payment provider. Every method that talks to the provider raises
    ``Problem(502, "payment_error")`` on failure and ``Problem(503, "coming_soon")`` when the provider is not configured."""
    name = "?"
    customer_prefix = ""                # customer ids of this provider start with it (other providers' ids are ignored)
    # Content-Security-Policy additions for pages of this provider (checkout overlay / redirect targets)
    csp_forms = ""
    csp_script = ""
    csp_frame = ""

    def __init__(self, svc):
        self.svc = svc

    def configured(self) -> bool:
        raise NotImplementedError

    def own_customer(self, user) -> str | None:
        cus = user["billing_customer"] if user is not None else None
        return cus if cus and cus.startswith(self.customer_prefix) else None

    # ---- checkout
    def checkout_url(self, order_id: int, user, plan: str, interval: str, waiver: bool, lang: str, coupon: str) -> str:
        raise NotImplementedError

    def gift_checkout_url(self, order_id: int, gift_id: int, user, plan: str, months: int, cents: int, lang: str, coupon: str) -> str:
        raise NotImplementedError

    def check_coupon(self, code: str):
        """Provider reference of an active coupon/discount code (Problem 400 coupon_invalid when unknown)."""
        raise NotImplementedError

    # ---- running subscriptions
    def portal_url(self, user) -> str:
        raise NotImplementedError

    def cancel_at_period_end(self, sub) -> None:
        raise NotImplementedError

    def change_at_period_end(self, sub, current: str, plan: str) -> str | None:
        """Plan change at the end of the paid period without proration; returns a schedule reference (or None)."""
        raise NotImplementedError

    def year_then_month(self, sub_id: str, plan: str) -> str | None:
        """Yearly in advance, then monthly (AGB 11.3)."""
        raise NotImplementedError

    def maintenance(self) -> None:
        """Hourly: apply changes the provider needs shortly before a period end."""

    # ---- webhooks
    def parse_webhook(self, payload: bytes, headers: dict) -> tuple[str, str, int, list[Event]]:
        """Verifies the signature (Problem 400 "signature") and returns (event id, provider type, created, unified events)."""
        raise NotImplementedError
