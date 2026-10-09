package com.simplebuilding.util;

import com.simplebuilding.component.ModDataComponentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Shulker state (owner N17): a placed Vanilla shulker box can stand open - its lid stays up like an open eyeblossom.
 * A shulker shell opens a closed box that way, a plain right-click on an open box closes it (no menu, the next
 * right-click opens the menu as usual). The state travels with the item ({@link ModDataComponentTypes#SHULKER_OPEN}),
 * which has its own open item model. Kept in the block entity ({@code ShulkerOpenStateMixin}) and synced to clients.
 */
public final class ShulkerLids {
    private ShulkerLids() {
    }

    /** Implemented on {@link ShulkerBoxBlockEntity} by {@code ShulkerOpenStateMixin}. */
    public interface Kept {
        boolean simplebuilding$keptOpen();

        void simplebuilding$setKeptOpen(boolean open);
    }

    public static boolean keptOpen(BlockEntity entity) {
        return entity instanceof Kept kept && kept.simplebuilding$keptOpen();
    }

    public static boolean keptOpen(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponentTypes.SHULKER_OPEN));
    }

    /** Opens or closes a placed box for good (server side): flag, sound, game event and a sync to the clients. */
    public static void setKeptOpen(Level level, BlockPos pos, ShulkerBoxBlockEntity box, boolean open, Player player) {
        if (!(box instanceof Kept kept) || kept.simplebuilding$keptOpen() == open) return;
        kept.simplebuilding$setKeptOpen(open);
        box.setChanged();
        level.playSound(null, pos, open ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F,
                level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(player, open ? GameEvent.CONTAINER_OPEN : GameEvent.CONTAINER_CLOSE, pos);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }
}
