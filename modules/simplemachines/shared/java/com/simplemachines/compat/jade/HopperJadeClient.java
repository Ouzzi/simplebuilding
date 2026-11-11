package com.simplemachines.compat.jade;

import com.simplemachines.compat.HopperInfo;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** The filter lines from the server's data (switchable as {@code config.jade.plugin_simplemachines.hopper_filter}). */
final class HopperJadeClient implements IBlockComponentProvider {
    @Override
    public Identifier getUid() {
        return HopperJadeServer.UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        HopperInfo.lines(accessor.getServerData()).forEach(tooltip::add);
    }
}
