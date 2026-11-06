# Plan A DRAGONXP06 (06.11.) - Drachenkampf schwerer + mehr XP
Basis a-rarity06. Simple Mobs bleibt optional: Items nur per ID (DragonRewards), fabric.mod.json fuehrt es als `suggests`, nicht `depends`.

## Schwerer (Kurve je weiterem Drachen s = Beschwoerungen)
Vorhanden und geprueft: Leben/Schaden/Tempo linear +15 % je s, ohne Spruenge (Schritt <= 0.15). Deckel: Leben x3.0 (s=14), Tempo 2.0 (summonSpeedCap), Schaden 2.0 (summonAttackCap, ab s=7).
Neu: `miteExtraPerSummons` (3): +1 Dragonmite je Welle je 3 Beschwoerungen (Obergrenze bleibt maxAliveMites).
| s | 0 | 3 | 7 | 10 | 14+ |
|---|---|---|---|----|-----|
| Leben x | 1.0 | 1.45 | 2.05 | 2.5 | 3.0 |
| Schaden x | 1.0 | 1.45 | 2.0 | 2.0 | 2.0 |
| Tempo (Basis 1.2) | 1.38 | 1.74 | 2.0 | 2.0 | 2.0 |
| Milben/Welle (1 Spieler) | 2 | 3 | 4 | 5 | 6 |

## Mehr XP
Vanilla: 1. Drache 12000, danach 500. Neu ab Kampf n>=2 (Kampfzaehler DragonKills): Gesamt = min(xpMax, 500 + xpPerFight*(n-1)); Bonus = Gesamt - 500 als ExperienceOrb.award am Drachen im DYING-Tick (einmalig ueber defeatedId). Kampf 4: 1250, Kampf 10: 2750, Deckel 5000 ab Kampf 19.
Begruendung linear: spuerbar je Kampf (+250 = ca. 2 Level), vorhersehbar, kein Explodieren; Deckel in Config. Kampf 1 bleibt Vanilla (12000 ist schon hoch).
Configs: xpPerFight 250 (0 = Vanilla), xpMax 5000, miteExtraPerSummons 3; Tooltip + Lang EN/DE + Wiki-Manual.

## Test
Gametest `curve_and_xp` (Katalog + @GameTest-Adapter curveAndXp): Monotonie, keine Spruenge, Deckel, XP Kampf 1/4/10/Deckel. Compile + `simpleenddragonfight:*` fabric + standalone.
Offen: Rueckfrage keine; Forge/NeoForge nicht (nur 26.3 Fabric).
