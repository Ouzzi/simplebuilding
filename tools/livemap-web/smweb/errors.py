"""One error type for the whole service: HTTP status + i18n key (+ values), shown as page text or JSON."""


class Problem(Exception):
    def __init__(self, status: int, key: str, retry_after: int | None = None, **params):
        super().__init__(key)
        self.status = status
        self.key = key
        self.params = params
        self.retry_after = retry_after
