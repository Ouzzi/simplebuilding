# Simple Quality of Life — ideas for owner review (not implemented)

| Title | What it does / benefit | Risk or exploit potential | Effort | Dev-mod overlap |
|---|---|---|---|---|
| Safer auto-walk stop | Stop on approaching unloaded terrain or a cliff; avoids accidental falls | Must not grant flight or trust client collision claims | Medium | Check Client Tweaks auto-movement options |
| Accessible key feedback | Optional distinct sounds for crawl/auto-walk state; easier hands-free use | Cosmetic only, avoid repeated sound spam | Small | Sound Physics changes sound presentation |
| Crop harvest preview | Tooltip explains seed cost and supported crops before interacting | Must not expose hidden claim information or fabricate loot | Small | Jade can display crop maturity |
| Inventory organization | Sort compatible open containers with a server-validated action | Duplication, slot permission bypass, packet spam; atomic rollback needed | Large | ClientSort and Mouse Tweaks already cover much of this; likely skip |
| Vault cooldown hint | Optional Jade tooltip explains remaining cooldown for the viewing player | Privacy leakage; only reveal authorized player's data | Medium | Jade provides tooltip framework |

No idea above is part of this port. No additional gameplay automation has been implemented.
