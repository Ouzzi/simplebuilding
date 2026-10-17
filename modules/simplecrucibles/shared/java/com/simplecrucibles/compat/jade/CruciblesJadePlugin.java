package com.simplecrucibles.compat.jade;

import com.simplecrucibles.cauldron.ReinforcedCauldronBlock;
import com.simplecrucibles.crucible.CrucibleBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade plugin of Simple Crucibles (split stage 5b, was SimpleBuilding's topic "crucible" and the reinforced
 * part of "cauldron"): heat, occupied slots and the shortest remaining time of a crucible (server data), the content
 * and fill level of the reinforced cauldron (block state). Only Jade loads it (Fabric: entrypoint {@code jade};
 * NeoForge: the annotation). Server and client providers stay separate classes (Jade rejects one class doing both).
 */
@WailaPlugin
public final class CruciblesJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new CrucibleJadeServer(), BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new CrucibleJadeClient(), CrucibleBlock.class);
        registration.registerBlockComponent(new CauldronJadeClient(), ReinforcedCauldronBlock.class);
        // Jade's universal fluid line ("Empty 1B") is wrong for the reinforced cauldron: its content is a block state.
        registration.addTooltipCollectedCallback((root, accessor) -> {
            if (accessor instanceof BlockAccessor block && block.getBlock() instanceof ReinforcedCauldronBlock) {
                root.getTooltip().remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
            }
        });
    }
}
