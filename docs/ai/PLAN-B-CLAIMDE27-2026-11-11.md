# Plan B-CLAIMDE27 (11.11.) - Claim-Code-Kopfzeile Lang EN/DE (prio27 #5)
Basis hub/b-bundle179-gen. Server schreibt weiter die englische Kopfzeile (HEADER in ClaimCode; Tests/Clienttest pruefen "Claim rights"). Der Parser ignoriert Kommentare (# bis Zeilenende, geprueft: parse + whoToken), braucht die Kopfzeile nicht.
Umsetzung: ClaimCode.localizeHeader(code, translate) ersetzt den englischen Block durch 4 Lang-Zeilen (simpletweaks.claim.code.header1-4, EN/DE); DeedScreen nutzt es beim Oeffnen (original und text gleich -> nicht "dirty"). Buchstaben/Format unveraendert. Test in ClaimRightsTests (gleiche Regeln nach Uebersetzung).
Offen: Client-Sicht im DE-Client.
