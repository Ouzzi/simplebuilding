# Plan b-claimreview (17.10.2026)

Ziel: Sicherheits-Review Claim-Urkunde Phase 1 (Rechte B/D/U/C/R/A/M, Parser, Ledger v2, Hooks) + rote Tests belegen.

## Ist-Zustand
- Rechte: `claims/ClaimRights`, Parser `ClaimCode`, Ledger `ClaimStore`, Autoritaet `Claims`; ~37 Mixins `mixin/claims/*`.
- Server-Autoritaet: `Claims.applyCode` parst serverseitig, Namen nur online/Land-Cache (kein Netz), `pin` speichert UUIDs.

## Review-Schwerpunkte
1. Entity-Rechte (`Claims.entityNeed`): Behaelter-Fahrzeuge (Truhenlore/Truhenboot) nur mit A oeffenbar?
2. Wachstum ueber Claimgrenzen (Baum/Knochenmehl), Frost Walker, Sculk, Angel.
3. Parser-Grenzfaelle (Namen/UUID/Rollen/Sektionen/Limits), Namens-Cache bei Umbenennung.
4. Fehlende Hooks je Lücke -> Fix + GameTest.

## Tests
- simplebuilding Filter *crucible*, *auto_smither*, *tiered*: forge-263 (+neoforge/fabric).
- Standalone simplemobs double_cap, simpletweaks claims_grief; every_mod_mob NeoForge.
- Claims-Module: module-simpletweaks-{fabric,neoforge,forge}-263 + standalone.

## Verifikation
Compile aller Loader, Filter `simpletweaks:*claim*`. Berichte ehrlich zu Nicht-Getestetem.
