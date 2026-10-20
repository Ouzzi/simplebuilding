package com.simplemusic.disc;

import com.simplemusic.SimpleMusic;
import com.simplemusic.config.MusicConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Blank disc (upload concept §2.1): right-click opens the burn window that lists {@code config/simplemusic/upload/};
 * after a successful upload the server turns it into a {@link CustomDiscItem}.
 */
public class BlankDiscItem extends Item {
    public BlankDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!SimpleMusic.on() || MusicConfig.uploads == MusicConfig.Uploads.OFF) return InteractionResult.PASS;
        if (level.isClientSide()) SimpleMusic.openBurn.accept(hand == InteractionHand.MAIN_HAND ? 0 : 1);
        return InteractionResult.SUCCESS;
    }
}
