package com.simplebuilding.blocks.custom;

import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Astralit and Nihilit ore (W24-4, owner 2026-10-23): the ore hides in the End stone (texture C2) until a brush frees it
 * like an archaeology find. Every Vanilla brush stroke (every 10 ticks of brushing, {@code BrushItemMixin}) adds one to
 * {@link #BRUSHED}; two intermediate looks lead to the visible, softly glowing ore (C) after {@link #STROKES} strokes, which
 * also costs the brush one durability like a finished find. The loot table drops the full amount only for the freed ore
 * ({@code brushed = 6}); a hidden or half-brushed ore drops it with a 70 % chance, Fortune applies in both cases
 * (docs/ai/PLAN-A-MECH24-2026-10-23.md). Progress stays where it stopped (no block entity, nothing decays).
 */
public class HiddenOreBlock extends DropExperienceBlock {
    public static final int STROKES = 6;
    public static final IntegerProperty BRUSHED = IntegerProperty.create("brushed", 0, STROKES);

    public HiddenOreBlock(IntProvider experience, Properties properties) {
        super(experience, properties);
        registerDefaultState(defaultBlockState().setValue(BRUSHED, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BRUSHED);
    }

    /** The freed state of a hidden ore (the full drop), any other block's default state - for drop checks. */
    public static BlockState freedOrDefault(Block block) {
        return block instanceof HiddenOreBlock ? block.defaultBlockState().setValue(BRUSHED, STROKES) : block.defaultBlockState();
    }

    /** True for a fully brushed (freed) hidden ore. */
    public static boolean freed(BlockState state) {
        return state.getBlock() instanceof HiddenOreBlock && state.getValue(BRUSHED) == STROKES;
    }

    /**
     * One brush stroke on {@code pos} (server side): one step further, the last one with the completion sound and one
     * durability of the brush. False when there is no hidden ore left to free there.
     */
    public static boolean brush(ServerLevel level, BlockPos pos, LivingEntity user, ItemStack brush) {
        if (!McVersion.BRUSHED_ORES) return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof HiddenOreBlock) || state.getValue(BRUSHED) >= STROKES) return false;
        int next = state.getValue(BRUSHED) + 1;
        level.setBlock(pos, state.setValue(BRUSHED, next), Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(user, state));
        if (next == STROKES) {
            level.playSound(null, pos, SoundEvents.BRUSH_GRAVEL_COMPLETED, SoundSource.BLOCKS, 1.0F, 1.0F);
            brush.hurtAndBreak(1, user, user.getUsedItemHand());
        }
        return true;
    }
}
