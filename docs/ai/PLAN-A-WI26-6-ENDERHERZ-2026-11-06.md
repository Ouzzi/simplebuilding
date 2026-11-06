# WI26-6 Enderherz ab dem 4. besiegten Drachen
Ist: Item `simplemobs:ender_heart` existiert (Void-Runner-Drop); Drachen-Zähler nur für Beschwörungen (DragonSummons), kein Kill-Zähler.
Umsetzung (nur simpleenddragonfight, kein Compile-Zwang auf Simple Mobs, Item per ID):
- `DragonKills` (SavedData, Overworld): besiegte Drachen weltweit + Spieler, die die Belohnung hatten.
- `DragonRewards.onDefeated`: ab Drache `rewardFromDragon` (4) bekommt jeder Teilnehmer (FightSession.lastSeen, online) einmal `rewardItem`, als ItemEntity im Ring (r=4) um den Podium-Mittelpunkt, nur vom Besitzer aufhebbar, ohne Despawn.
- Auslöser: FightSession.tick, erster Tick in der DYING-Phase je Drachen-UUID.
- Config: rewardEnabled, rewardFromDragon, rewardItem (+ Lang EN/DE). Gametest `dragon_reward` + Fabric-Adapter.
Offen/Rückfrage: Simple-Mobs-requires (bewusst weich gelassen, Modul-Unabhängigkeit), 4. Drache Chorus/Seelenfarbe, Ender-Juwel ab 10. Drachen, Schwere im skalierten Kampf.
