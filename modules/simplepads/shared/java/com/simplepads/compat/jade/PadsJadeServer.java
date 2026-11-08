package com.simplepads.compat.jade;

import com.simplepads.SimplePads;
import com.simplepads.block.LaunchpadBlock;
import com.simplepads.block.entity.ChunkLoaderBlockEntity;
import com.simplepads.block.entity.LaunchpadBlockEntity;
import com.simplepads.block.entity.PotionPadBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Sends what the client does not have: charges, the stored effects and cooldown, the forced chunks. */
final class PadsJadeServer implements IServerDataProvider<BlockAccessor> {
    static final Identifier UID = SimplePads.id("pad_status");
    static final String KIND = "SimplePadsKind", A = "SimplePadsA", B = "SimplePadsB", EFFECTS = "SimplePadsEffects";

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof LaunchpadBlockEntity launchpad) {
            int max = be.getBlockState().getBlock() instanceof LaunchpadBlock pad && be.getLevel() != null
                    ? pad.capacityAt(be.getLevel(), be.getBlockPos()) : LaunchpadBlock.maxCharges(LaunchpadBlock.ENDERITE_TIER);
            data.putString(KIND, "launchpad");
            data.putInt(A, launchpad.getCharges());
            data.putInt(B, max);
        } else if (be instanceof PotionPadBlockEntity potionPad) {
            List<String> effects = new ArrayList<>();
            PotionContents stored = potionPad.getStored();
            if (stored != null) {
                for (MobEffectInstance effect : stored.getAllEffects()) {
                    effects.add(effect.getEffect().value().getDescriptionId() + "@" + effect.getAmplifier());
                }
            }
            data.putString(KIND, "potion_pad");
            data.putString(EFFECTS, String.join(",", effects));
            data.putInt(A, potionPad.getCooldown());
        } else if (be instanceof ChunkLoaderBlockEntity loader) {
            data.putString(KIND, "chunk_loader");
            data.putInt(A, loader.ownForced().size());
        }
    }
}
