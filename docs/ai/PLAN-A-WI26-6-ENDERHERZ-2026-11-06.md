# WI26-6 Enderherz ab dem 4. besiegten Drachen
Ist: Item `simplemobs:ender_heart` existiert (Void-Runner-Drop); Drachen-Zähler nur für Beschwörungen (DragonSummons), kein Kill-Zähler.
Umsetzung (nur simpleenddragonfight, kein Compile-Zwang auf Simple Mobs, Item per ID):
- `DragonKills` (SavedData, Overworld): besiegte Drachen weltweit + Spieler, die die Belohnung hatten.
- `DragonRewards.onDefeated`: ab Drache `rewardFromDragon` (4) bekommt jeder Teilnehmer (FightSession.lastSeen, online) einmal `rewardItem`, als ItemEntity im Ring (r=4) um den Podium-Mittelpunkt, nur vom Besitzer aufhebbar, ohne Despawn.
- Auslöser: FightSession.tick, erster Tick in der DYING-Phase je Drachen-UUID.
- Config: rewardEnabled, rewardFromDragon, rewardItem (+ Lang EN/DE). Gametest `dragon_reward` + Fabric-Adapter.
Offen/Rückfrage: Simple-Mobs-requires (bewusst weich gelassen, Modul-Unabhängigkeit), 4. Drache Chorus/Seelenfarbe, Ender-Juwel ab 10. Drachen, Schwere im skalierten Kampf.

## Rest (06.11.)
- **4. Drache:** gesyncte Entity-Daten-Boolean auf dem EnderDragon (EnderDragonMixin, Duck `DragonTinted`), gesetzt in `FightSession.startFight`, wenn `kills+1 >= tintFromDragon` (4; 0 = aus). Client: `DragonRendererMixin` hängt hinter dem Augen-Durchgang einen zweiten additiven Durchgang (`RenderTypes.eyes` auf der Vanilla-Drachentextur, Chorus-Violett 0x66B070E0, Variante A) an; nicht beim Sterben. Keine Textur im Repo. Vorschau: /root/sb-tools-b/previews/a-wi26-6-preview.html (B = Seelen-Türkis).
- **Ender-Juwel:** `simplemobs:ender_gem` existiert (Ender-Juwel). Config `rewardJewelFromDragon` 10 (0 = aus) und `rewardJewelItem`; je Spieler einmal (`DragonKills.jewel_rewarded`), gleiche Platzierung wie das Herz.
- **Schwierigkeit:** Bestehend: Leben/Schaden/Tempo x (1 + 0,15 je Beschwörung, max 3,0), Tempo max 2,0. Angepasst nur der Schaden:

| Beschwörungen | Faktor Leben/Tempo | Schaden vorher | Schaden jetzt (Deckel 2,0) |
|---|---|---|---|
| 3 (4. Drache) | 1,45 | 1,45 | 1,45 |
| 7 | 2,05 | 2,05 | 2,0 |
| 13+ | 3,0 | 3,0 | 2,0 |

Begründung: Tempo, Leben und Schaden gleichzeitig auf 3x macht späte Kämpfe unfair (ein Treffer 3x); Leben darf weiter wachsen (längere Kämpfe, mehr Spieler zählen), Schaden bleibt tragbar. Config `summonAttackCap` (1,0 bis 10). Bis 6 Beschwörungen unverändert.
- **Rückfrage:** Ender-Juwel als zweites Item je Spieler ab Drache 10 (zusätzlich zum Herz) bestätigen; Vorschau-Wahl A/B.
