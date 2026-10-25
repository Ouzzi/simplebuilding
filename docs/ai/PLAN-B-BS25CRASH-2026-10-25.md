# Plan BS25-19: Tod mit Seelenfeuer-Lohenkopf hängt den Server (b-bs25crash, 25.10.)

## Befund (Besitzer 25.10.)
Spieler stirbt mit `simplemobheads:soulfire_blaze_head` auf dem Kopf → Kopf bleibt im Inventar, Spiel hängt
(keine Befehle, kein Moduswechsel, Speichern+Beenden hängt) = Server-Thread steckt im Tod-Pfad.

## Ist-Zustand (Erhebung)
- Kopf-Fähigkeiten: `HeadAbilities` hat für `soulfire_blaze` keine eigene Fähigkeit (nur Gruß/Allgemeines).
- Tod-Pfad-Hooks im SB-Paket 26.3 Fabric: `EasterDeathMixin` (Technoblade, nur Schweine), simplerespawn
  `checkTotemDeathProtection` (nur mit anderen Spielern), `PlayerExtraHeadMixin#dropEquipment` (Zusatz-Kopfplatz),
  `LivingEntityMixin` (Pfeile, Attractor-Markierung), simpletweaks Play-/Admin-Mixins, simplestats `die`,
  `HeadDrops.onDeath` (AFTER_DEATH).
- Statisch keine offensichtliche Schleife → erst reproduzieren.

## Vorgehen
1. Gametest `worn_head_death` (MobHeadTests + Fabric-Adapter): Überlebens-Spieler trägt Kopf (Seelenfeuer-Lohe mit
   generic/inFire/onFire/lava, dann jeder Kopf mit generic), stirbt → tot, Kopfplatz leer, genau ein Kopf-Drop,
   Server tickt weiter. Läufe: module-simplemobheads-fabric-263, -standalone, fabric-263 (volles Paket).
2. Rot? → Stelle per Stack (jstack bei Hänger / Fehlermeldung) finden, kleinste Korrektur.
   Grün? → Pfad weiter eingrenzen (echter Spieler-Tod: `ServerPlayer#die`, Todesnachricht, Respawn) bis rot.
3. Fix, Tests grün, Generatoren + `./gradlew check -q`, Queue BS25-19 [x] mit Beleg, Memory-Abschnitt.

## Risiken
Hänger im Testlauf blockiert sb-test bis Timeout → Lauf mit Watchdog (jstack nach 10 min), nie kill.
