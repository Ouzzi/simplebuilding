---
name: immersion-2026-09-28
description: "Welle 23 R Immersion - wo Pad-Zustaende, Info-Tooltips, HUD-Taste, Jade/REI stecken; EMI nicht verfuegbar"
metadata:
  node_type: memory
  type: project
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-28T22:40:29.257Z
---

Welle 23 R (Branch worktree-agent-ae150af7415be09a4, 2026-09-29):
- Sichtbare Zustaende: LaunchpadBlock.CHARGE 0-3 (Drittel, aufgerundet; LaunchpadBlockEntity#refreshChargeState je Tick),
  ChunkLoaderBlock.ACTIVE (+ Licht 3/Stufe, gesetzt in ChunkLoaderBlockEntity#update -> setActive), FlypadBlock.ACTIVE
  (Spieler im Feld). Texturen aus generate_textures.py pad_state_textures (Spirale = PAD_VEIL '0', nach Weglaenge
  von der Mitte), Modelle in TweaksModelGen launchpad()/activePad(). Vorlage hand/chunk_loader.png.
- Feedback ohne Text: util/Feedback.playTo (Klang nur fuer einen Spieler), Flypad-Randwarnung (1,5 Bloecke, alle 10 Ticks).
- Tooltips: items/tooltip/InfoTooltips.lines (vom client ItemMixin angehaengt, Tests direkt); coreUses wird per Test
  gegen die Rezepte geprueft.
- HUD: client/gui/ModHud (Taste toggle_hud, unbelegt; Config showModHud/hudPositionX/Y/hudScale), Mathe in HudLayout
  (serversicher testbar).
- Jade: compat/BlockInfo + common/src/jade/java; REI: common/src/rei/java (+ neoforge/src/rei/java), nur 26.2 + 1.21.11.
  EMI: kein Build ueber 1.21.1 hinaus (Modrinth + Terraformers-Maven, geprueft 2026-09-28).
- Falle: Namenskollision in generate_textures.py - es gibt dort schon luminance() (0..255); eigene Helfer anders nennen.
Siehe [[besitzer-entscheidungen-2026-09-28]].
