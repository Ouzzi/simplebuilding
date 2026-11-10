package com.simplemachines.compat.jade;

import com.simplemachines.hopper.TierHopperBlock;
import com.simplemachines.hopper.TierHopperBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade plugin of Simple Machines (b-jadehop26): the filter of the reinforced, netherite and enderite hopper
 * (SimpleBuilding's plugin only registers its 26.2 hopper). Only Jade loads it (Fabric entrypoint {@code jade}), the
 * module runs without Jade. Server and client providers stay separate classes (Jade rejects one class doing both).
 */
@WailaPlugin
public final class MachinesJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new HopperJadeServer(), TierHopperBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new HopperJadeClient(), TierHopperBlock.class);
    }
}
