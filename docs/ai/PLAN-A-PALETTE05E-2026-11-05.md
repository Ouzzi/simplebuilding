# Plan A: IG31-42 Stufe E (05.11.)
Treppe, Stufe, Mauer fuer Block of Astralit/Nihilit (astralit_*/nihilith_stairs|slab|wall) und ender_quartz_wall: 7 Glieder in SimpleBuilding.
- ModBlocks.EndPalette um blockWall erweitert; alle drei Paletten tragen blockStairs/Slab/Wall (14 Bloecke je Palette).
- ModItems, Kreativ-Reihen (hinter dem Grundblock, Familien-Sortierung) + DataIntegrity-Layout, Lang EN/DE (src + overlay), simplemoney-Preise, craftable-Tag.
- Datagen: Modelle (family.wall), Werkbank (wallBuilder 6->6) + Steinmetz; Tags/Beute laufen ueber die Palette.
- Bewusst nicht: Meissel-Kette (Steinmetz deckt es), Forge/NeoForge/1.21.11.
- Verifikation: sbt runDatagen, Compile, data_integrity*, test_centre*, check.
