package com.simplepads.compat.jade;

import com.simplepads.block.ChunkLoaderBlock;
import com.simplepads.block.LaunchpadBlock;
import com.simplepads.block.PotionPadBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade plugin of Simple Pads (a-fix09b): the status lines SimpleBuilding's topic "pad status" showed before the
 * split (launchpad charges, potion pad effects and cooldown, chunk loader chunks), now under {@code jade.simplepads.*}.
 * Only Jade loads it (Fabric entrypoint {@code jade}). Server and client providers stay separate classes (Jade rejects one
 * class doing both).
 */
@WailaPlugin
public final class PadsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new PadsJadeServer(), BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        PadsJadeClient client = new PadsJadeClient();
        registration.registerBlockComponent(client, LaunchpadBlock.class);
        registration.registerBlockComponent(client, PotionPadBlock.class);
        registration.registerBlockComponent(client, ChunkLoaderBlock.class);
    }
}
