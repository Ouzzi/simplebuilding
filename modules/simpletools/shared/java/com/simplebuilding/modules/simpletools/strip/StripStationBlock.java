package com.simplebuilding.modules.simpletools.strip;

import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplelib.api.InteractionGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Strip station: a cage that holds a whole stack of blocks. Right click with a stack puts it in, with an axe works the whole
 * stack in one click (strip, scrape, wax off; 1 durability per block), with an empty hand takes the result (else the input)
 * out. A hopper above fills it, a hopper below takes only what the axe made.
 */
public class StripStationBlock extends Block implements EntityBlock {
    public StripStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StripStationBlockEntity(pos, state);
    }

    private static boolean on(Level level) {
        return SimpleTools.active(level) && SimpleTools.configFor(level).enableStripStation;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!on(level) || hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof StripStationBlockEntity cage)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        boolean axe = StripWork.isAxe(stack);
        if (!axe && !StripStationBlockEntity.workable(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return axe ? (cage.canWork(stack) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND) : InteractionResult.SUCCESS;
        if (!InteractionGuard.mayChange(player, pos)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (axe) return work(cage, (ServerLevel) level, player, hand);
        if (cage.insert(stack) > 0) level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        else tell(player, "full");
        return InteractionResult.SUCCESS;
    }

    private static void tell(Player player, String key) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) sp.sendOverlayMessage(Component.translatable("message.simpletools.strip_station." + key));
    }

    private static InteractionResult work(StripStationBlockEntity cage, ServerLevel level, Player player, InteractionHand hand) {
        switch (cage.work(level, player, hand)) {
            case PARTIAL -> tell(player, "partial");
            case WORN -> tell(player, "worn");
            case BLOCKED -> tell(player, "blocked");
            case NOTHING -> {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            default -> {}
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!on(level) || !player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof StripStationBlockEntity cage)) return InteractionResult.PASS;
        if (cage.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!InteractionGuard.mayChange(player, pos)) return InteractionResult.PASS;
        ItemStack out = cage.take();
        player.getInventory().placeItemBackInInventory(out, net.minecraft.util.Prediction.SERVER_ONLY);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
