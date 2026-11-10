package com.simplemachines.compat.jade;

import com.simplemachines.SimpleMachines;
import com.simplemachines.compat.HopperInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Sends the hopper's filter mode and filter items (the client may not have the slots). */
final class HopperJadeServer implements IServerDataProvider<BlockAccessor> {
    static final Identifier UID = SimpleMachines.id("hopper_filter");

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() != null) HopperInfo.serverData(accessor.getBlockEntity(), data);
    }
}
