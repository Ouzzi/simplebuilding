package com.simplesandwiches.item;

import com.simplesandwiches.registry.ModItems;
import com.simplesandwiches.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Iron kitchen knife (owner decision F8 = A, repaired with iron nuggets). Sword-like tool rules
 * (cobweb fast + string, bamboo instantly) at a third of the iron sword's damage. Right click:
 * melon block -> 9 slices (its crafting value, no dupe); cake -> one cake slice per click (see
 * {@code CakeKnifeMixin}, the cake block would otherwise eat first). Board, cheese and butter actions
 * live in their blocks.
 */
public class KnifeItem extends Item {
    /** Iron tier values, repaired with iron nuggets. */
    public static final ToolMaterial MATERIAL = new ToolMaterial(net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL,
            250, 6.0F, 2.0F, 14, ModTags.KNIFE_REPAIR_MATERIALS);
    /** Total attack damage 2 (player 1 + baseline -1 + iron bonus 2), iron sword 6. */
    public static final float ATTACK_DAMAGE_BASELINE = -1.0F;
    /** Attack speed 2.0 (player 4.0 - 2.0). */
    public static final float ATTACK_SPEED_BASELINE = -2.0F;
    public static final int MELON_SLICES = 9;

    public KnifeItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties base) {
        return MATERIAL.applySwordProperties(base, ATTACK_DAMAGE_BASELINE, ATTACK_SPEED_BASELINE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> out,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplesandwiches.knife.recipe")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    /** Spreading butter on the board plays a wiping motion (Vanilla brush animation) while the click is held. */
    public static final int SPREAD_TICKS = 20;

    @Override
    public net.minecraft.world.item.ItemUseAnimation getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.ItemUseAnimation.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity user) {
        return SPREAD_TICKS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).is(Blocks.MELON)) {
            if (!level.isClientSide()) {
                level.destroyBlock(pos, false, context.getPlayer());
                Block.popResource(level, pos, new ItemStack(Items.MELON_SLICE, MELON_SLICES));
                if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand());
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Cuts one slice off a placed cake (candle cakes first drop their candle). A full cake gives
     * {@code CakeBlock.MAX_BITES + 1} = 7 slices, a bitten cake its remaining bites.
     */
    public static InteractionResult cutCake(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack knife) {
        BlockState cake = state;
        if (state.getBlock() instanceof CandleCakeBlock) {
            if (!level.isClientSide()) {
                Item candle = candleOf(state);
                if (candle != Items.AIR) Block.popResource(level, pos, new ItemStack(candle));
            }
            cake = Blocks.CAKE.defaultBlockState();
        }
        if (!(cake.getBlock() instanceof CakeBlock)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            int bites = cake.getValue(CakeBlock.BITES);
            if (bites < CakeBlock.MAX_BITES) {
                level.setBlockAndUpdate(pos, cake.setValue(CakeBlock.BITES, bites + 1));
            } else {
                level.removeBlock(pos, false);
                level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            }
            ItemStack slice = new ItemStack(ModItems.CAKE_SLICE);
            player.getInventory().placeItemBackInInventory(slice, net.minecraft.util.Prediction.SERVER_ONLY);
            knife.hurtAndBreak(1, player, hand);
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.7F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }

    /** {@code minecraft:red_candle_cake} -> {@code minecraft:red_candle}. */
    public static Item candleOf(BlockState candleCake) {
        var key = BuiltInRegistries.BLOCK.getKey(candleCake.getBlock());
        String path = key.getPath();
        if (!path.endsWith("_cake")) return Items.AIR;
        return BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(key.getNamespace(),
                path.substring(0, path.length() - "_cake".length())));
    }
}
