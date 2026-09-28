package com.simplebuilding.tweaks.block;

import net.minecraft.world.level.block.Block;

/**
 * Ein alter, abgeloester Stufenblock, der nur noch zum Laden alter Welten registriert ist
 * ({@link TweaksBlocks#legacy()}): gesetzt arbeitet er wie seine neue Stufe und wird beim ersten Tick
 * zu ihr, sein Item tauscht sich im Spielerinventar gegen ihr Item ({@code LegacyTierBlockItem}).
 * Kein Rezept, nicht im Kreativ-Tab, in JEI ausgeblendet, Name mit "(Legacy)" / "(alt)".
 */
public interface LegacyTierBlock {
    /** Die neue Stufe, zu der dieser alte Block wird. */
    Block target();
}
