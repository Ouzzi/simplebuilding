package com.simplecrucibles.compat.jade;

import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.compat.CrucibleInfo;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Content and fill level of the reinforced cauldron, from the block state ({@code config.jade.plugin_simplecrucibles.cauldron}). */
final class CauldronJadeClient implements IBlockComponentProvider {
    private static final Identifier UID = SimpleCrucibles.id("cauldron");

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CrucibleInfo.cauldronLines(accessor.getBlockState()).forEach(tooltip::add);
    }
}
