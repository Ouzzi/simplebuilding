package com.simplebuilding.modules.simplelayers.backrooms.level.lobby;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.LegacyBand;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;

/**
 * Level "lobby" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-lobby): everything in this package. Start content:
 * the BACK18/20 generator through {@link LegacyBand} (core §7); replace with the level concept's own generator, pieces and palette.
 */
public final class LobbyBand extends LegacyBand {
    public LobbyBand() {
        super("lobby", BackroomsLevel.LOBBY, DoorRule.TWO_WAY, 0xC9B458, () -> com.simplebuilding.modules.simplelayers.LayersRegistry.WALLPAPER.defaultBlockState(), false);
    }
}
