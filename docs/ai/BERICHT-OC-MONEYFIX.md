# Bericht oc-moneyfix (N63/W9)
- Ursache: 238 WT-Links im Tag `wandering_trader/rare`, den kein Vanilla-Trade-Set liest. Keine Texturen angefasst.
- Fix: Trade-Set `simplemoney:wandering_trader/links` (amount 1, Tag `#minecraft:wandering_trader/rare`) + Mixin `WanderingTraderLinksMixin` (TAIL von `updateTrades`, nach den 3 Vanilla-Sets; prueft `enableWanderingTrades`). Vanilla-9 Angebote bleiben, +1 Link-Angebot.
- Rarität: Chance-Praedikate der Links greifen weiter (Draw laeuft bis ein Angebot besteht, d. h. Seltenes relativ seltener, Mobheads ~24 %); Preise/Budget/Stock unveraendert (LinkedTradeMixin).
- Dorf-Wanderer spaeter: eigenes Set/Tag `simplewanderers:wanderer/rare` analog; WT-Set bleibt unberuehrt.
- Test `simplemoney:*link*`, fabric+neoforge: "alles gruen: 16/16" (8 je Loader, neu `linkWanderingTrader`: 400 Haendler, >=9 Vanilla-Angebote + Link). Mutation (Mixin entfernt): NICHT gruen 0/1 (belegt).
- Forge nicht gelaufen (kompiliert nicht separat geprueft; Mixin-Config + Datapack gleich wie bei den anderen Link-Mixins). `wiki/generate.py --all --check --strict`: 3 PROBLEM-Zeilen (textures.js, sandwich/bottom*.png) – nicht von dieser Aenderung. Rest der Checks gruen.
- Offen: Klick-Test auf Dev-Instanz; Balancing (1 garantiertes Zusatzangebot) ggf. mit Besitzer abstimmen.
