package com.simplecrucibles.compat.jade;

import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.compat.CrucibleInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Sends the crucible status (heat, slots, remaining ticks) the client does not have. */
final class CrucibleJadeServer implements IServerDataProvider<BlockAccessor> {
    static final Identifier UID = SimpleCrucibles.id("crucible");

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be == null) return;
        int[] status = CrucibleInfo.crucibleStatus(be);
        if (status.length == 4) data.putIntArray(CrucibleInfo.CRUCIBLE_DATA, status);
    }
}
