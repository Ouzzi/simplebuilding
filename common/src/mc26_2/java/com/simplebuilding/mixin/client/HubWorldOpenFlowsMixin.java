package com.simplebuilding.mixin.client;

import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;

/** Hub warning acknowledgment is enabled on the 26.3 main line only. */
@Mixin(WorldOpenFlows.class)
public abstract class HubWorldOpenFlowsMixin {
}
