# Bericht oc-shop-concept (2026-10-14)
- Nur Konzept, kein Code: `docs/ai/KONZEPT-SHOP-VITRINE-2026-10-14.md` (Queue Nachtrag 56 unverändert).
- Ist-Zustand per grep: Simple Money hat keinen Shop; Claims-Rechte B/D/U/C/R/A/M; Protection-API erlaubt eigenen Provider je Modul; Filter-Prinzip; keine Vitrine vorhanden; ClaimAutomation (fail closed); Death-Vault-Muster.
- Fund: `Claims.useAction` wertet Menü-Blöcke als Recht C. Lösung ohne Code in Simple Tweaks: Modul liefert Daten-Tag `simpletweaks:claim_use` (dann reicht `*: U`).
- Entscheidungen/Abweichungen: neues Modul `simpleshops` (Geld optional); Ware lagert in der Vitrine, Preis + Einnahmen im Verkaufsblock (Auftrag sagte „Lager im Verkaufsblock“, Frage 2); Preis = Vorlage, wird nie verbraucht.
- Schutz: Mixin auf `destroyBlock` + eigener Protection-Provider + Resistenz 3,6 Mio, Wither/Drache-Tags, Push BLOCK, kein Container/keine Transfer-Capability.
- Tausch atomar (prüfen, simulieren, committen), Sperre über Menü-Referenz statt nur Zeit, Preisvergleich exakt.
- 13 offene Besitzer-Fragen mit Empfehlung (Abschnitt 5), 4 Stufen mit GameTest-Namen (Abschnitt 6).
- Nicht getestet/verifiziert: Fluss-, Enderman-, Wither-Verhalten sind Annahmen aus Vanilla-Wissen und im Konzept als „per Test zu belegen“ markiert.
