# Plan b-revvault24 (24.10.2026) - Rest von REV24-M-9, F-28, M-11

Basis b-revmid24 (8a8beb027). Review-Datei docs/ai/REVIEW-CODE-2026-10-24.md liegt nicht im Repo; Grundlage sind die Queue-Zeilen REV24.

| Fund | Befund im Code | Aenderung | Pruefung |
|---|---|---|---|
| M-9 Codec | bestaetigt: `Entry.CODEC.listOf()` / `unboundedMap(..., Death.CODEC.listOf())` lassen ein kaputtes Element den ganzen Ledger scheitern (Vanilla ersetzt ihn durch einen leeren) | `lenientList` + `PLAYERS`-Decoder: kaputter Eintrag/Tod/Spielerschluessel wird geloggt und uebersprungen, Rest bleibt; Encode unveraendert (alte Saves laden, Pflichtfelder gleich) | `vault_persistence` erweitert: kaputter Eintrag, leerer Tod, falscher Schluessel, alter Eintrag ohne optionale Felder |
| M-9 reindex | verworfen: `reindex()` laeuft schon ueber alle Spieler (record, clear, Laden); kein Pro-Spieler-Pfad | - | durch obigen Test (lying bleibt tracked) |
| F-28 | bestaetigt (Kosten klein: Listener + isEmpty), aber Listener hing immer am BEFORE-Event | Probes: `activateOnFirstUse`; Fabric registriert den Listener erst beim ersten `refuseAt`/`refuseBreakAt` (nur 26.3-Fabric-Klassen und gemeinsame Probes, Loader-Verhalten sonst gleich) | Gametests BuildingWand/Modpack/PistonBreach |
| M-11 simplefarming | verworfen: b-revmid24 hat die Probe dort wieder entfernt; Veto braucht `com.simplelib.api.HammerActions` = neue harte Abhaengigkeit (PRINZIPIEN-MODUL-UNABHAENGIGKEIT); Reflektion/Soft-Link waere ein Sonderweg | - | - |
