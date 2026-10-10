# In-World-Umwandlung vereinheitlichen (2026-10-10, Branch claude-q-inworld)

Ausgangslage: `docs/ai/INWORLD-UMWANDLUNGEN-2026-10-06.md` (Inventar je Umwandlung). Queue N15 "vereinheitlichen",
N15 "Schrittweiser Umbau je Schlag (universell)", N11 "Hammer + Besatz".

## Entscheidungen
- Eine gemeinsame Umsetzung: `com.simplelib.api.InWorldStrikes` (Zaehlung, Risse, Partikel, Schlagklang, schwebende
  Teil-Ergebnisse, **neu** aufwachsende Vorschau, Tick/Aufraeumen). SimpleLib registriert den Tick selbst (3 Loader).
- SimpleBuilding 26.3: `mc26_3/overlay/.../util/InWorldStrikes` ist eine duenne Huelle (gleiche Signaturen, damit
  Hammer-Code/Tests unveraendert bleiben). 26.2 hat kein SimpleLib: dort bleibt die volle Fassung in
  `common/src/mc26_2/.../util/InWorldStrikes` (Vorschau = No-op).
- Modul-Unabhaengigkeit (Prinzip 6a): nur `com.simplelib.api` wird von SB importiert; SimpleLib kennt keine Nutzer.
  Sandwiches buendelt SimpleLib und kann dieselbe API nutzen.
- Zwischenmodelle ("schrittweiser Umbau"): `preview(level,pos,target,done,total)` zeigt den Zielblock als Vanilla-
  `BlockDisplay` (1.02 gross, von unten nach oben aufgedeckt, Hoehe = done/total) ueber dem alten Block; beim letzten
  Schlag weg, Block wird getauscht. Verfall nach 100 Ticks ohne Schlag, Waisen werden aufgeraeumt.
- Einheitliche Riss-Kennung (`crackId`/`crackStage`): SimpleLib-Fass delegiert an die gemeinsame Funktion.

## Inventar und Luecken

| Umwandlung | Rechtsklick | Risse | Partikel | Klang | Teil-Ergebnis | Vorschau (neu) |
|---|---|---|---|---|---|---|
| Maschinen/Truhen/Shulker aufwerten (SledgehammerUpgrades) | ja (halten) | ja (eigene Persistenz) | ja (eigene Schlag-Effekte) | ja | n/a (Block) | **ja (neu)** |
| Tiegel/Fass/Kessel aufwerten (Hammer, Axt) | ja | ja | **jetzt gemeinsam** | **jetzt gemeinsam, steigend** | n/a | **ja (neu, Lib)** |
| Eisen-Tiegel bauen (Rohling) | ja | ja | **jetzt gemeinsam** | gemeinsam steigend | n/a | Rohling-Stufen sind die Zwischenmodelle |
| Fass an Tiegel | ja | ja | **jetzt gemeinsam** | **gemeinsam, steigend (vorher fest)** | n/a | **ja (neu)** |
| Diamantblock/Quarz/Splitter | ja | ja | ja | ja | ja | n/a (Items) |
| Besatz-Vorlage + Leuchttinte | ja | ja | eigene | eigene (steigend) | n/a | Luecke: Item-Tausch, keine Block-Vorschau |
| Sandwiches (Brett, Messer, Milchkessel) | ja | n/a (ein Klick je Schritt) | ja | ja | n/a | n/a, bewusst Einzelschritte |
| Schallplatte wenden, Kern-Erz, Schere, Meissel ... | ja | n/a (1 Klick) | ja | ja | n/a | n/a |

Bewusst offen: Zwischenmodelle fuer Umwandlungen ohne Blockziel (Besatz-Vorlage) und echte Mehrstufen-Keyframes
(z. B. eigene Teilmodelle pro Schlag) - die Aufdeck-Vorschau ist der universelle Ersatz.

## Verifikation
Compile Fabric/NeoForge/Forge 26.3; `simplelib:*` (u. a. barrel_attach_and_results_first mit Vorschau-Pruefung);
`simplebuilding:*sledgehammer*`; Standalone-Targets der Module (Lib-Klasse nur in simplelib).
