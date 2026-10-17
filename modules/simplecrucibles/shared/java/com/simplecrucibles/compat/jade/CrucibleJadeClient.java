package com.simplecrucibles.compat.jade;

import com.simplecrucibles.compat.CrucibleInfo;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** The crucible lines from the server's status (switchable as {@code config.jade.plugin_simplecrucibles.crucible}). */
final class CrucibleJadeClient implements IBlockComponentProvider {
    @Override
    public Identifier getUid() {
        return CrucibleJadeServer.UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        int[] status = accessor.getServerData().getIntArray(CrucibleInfo.CRUCIBLE_DATA).orElse(new int[0]);
        CrucibleInfo.crucibleLines(status).forEach(tooltip::add);
    }
}
