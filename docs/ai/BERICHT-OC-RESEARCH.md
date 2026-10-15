# Bericht oc-research (Nachtrag 55: Howler-Grusel + Mob-Animation, nur Konzept)
- Ergebnis: `docs/ai/KONZEPT-HOLLOW-HOWLER-GRUSEL-2026-10-15.md` (Ist-Zustand, 6 Prinzipien, 3 Phasen mit Aufwand S/M/L) und `docs/ai/KONZEPT-MOB-ANIMATIONEN-2026-10-15.md` (Vanilla-Richtlinien, 9 Custom-Mobs einzeln, Reihenfolge).
- Kernbefunde Howler: Ambient = `WARDEN_LISTENING` (Notice-Laut), Hurt = `SKELETON_HURT`, kein Herzschlag am Mob, Glow-Layer nur 42 Pixel, alle Untertitel „Warden …“. Phase 1 (Herzschlag nach Phase, Ambient nach Phase, Stille vor Angriff) braucht nur Vanilla-Events, keine Assets.
- Kernbefunde Animation: Dread/Watcher poppen beim Aktionsende (Sprung auf 0), Nibblemite/Dragonmite wackeln konstant und synchron, Deceiver ist der Keyframe-Referenzmob.
- Texturen: nichts geändert; Phase 1/2 brauchen keine Texturänderung, neue Emissiv-Textur nur als Option mit Freigabe.
- Quellen: Minecraft-26.3-Quellen (lokal), Minecraft Wiki (Warden, sounds.json), Wikipedia (Infrasound, Uncanny valley, Acousmatic sound, 12 Animationsprinzipien); Infraschall bewusst als „schwache Evidenz, Hardwaregrenze“ bewertet.
- Entscheidungen: Phase-3-Sounds nur synthetisiert (kein Mojang-Sample); Darkness-Puls/Kamerawackeln/Textur als Besitzerentscheid markiert.
- Abweichung: kein Code, keine Tests, kein Gate (laut Auftrag); `.claude/QUEUE.md` unverändert.
- Offen: Umsetzung der Phasen; auf diesem CT fehlen scipy/ffmpeg für Phase-3-Synthese. Hörwirkung nicht getestet.
