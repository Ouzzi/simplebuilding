package com.simplebuilding.modules.simpletweaks.table;

import com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The old claim table (SP22-8 §6.3): replaced by the cartography table (deeds are restored and copied there) and the
 * deed's own map. The ids stay registered so worlds keep loading; nothing scans for old tables. Using one turns it into a
 * Vanilla cartography table and throws out an empty deed (its recipe held one); breaking it drops both; placing the old
 * item sets a cartography table and gives the empty deed back.
 */
public final class ClaimTableBlock extends Block {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("simpletweaks", "claim_table");
    private static Block block;
    private static Item item;

    private ClaimTableBlock(Properties properties) { super(properties); }

    public static void registerBlock() {
        var key = ResourceKey.create(Registries.BLOCK, ID);
        block = Registry.register(BuiltInRegistries.BLOCK, key, new ClaimTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                .strength(2.5F).sound(SoundType.WOOD).ignitedByLava().setId(key)));
    }
    public static void registerItem() {
        var key = ResourceKey.create(Registries.ITEM, ID);
        item = Registry.register(BuiltInRegistries.ITEM, key, new LegacyItem(block(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }
    public static Block block() { return block != null ? block : BuiltInRegistries.BLOCK.getValue(ID); }
    public static Item item() { return item != null ? item : BuiltInRegistries.ITEM.getValue(ID); }

    /** Replaces the old table at {@code pos} by a cartography table; the empty deed goes to {@code receiver}, else it drops. */
    public static void convert(Level level, BlockPos pos, Player receiver) {
        if (level.isClientSide()) return;
        level.setBlock(pos, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack deed = new ItemStack(ClaimDeedItem.item());
        if (receiver == null || !receiver.getInventory().add(deed)) Block.popResource(level, pos.above(), deed);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        convert(level, pos, null);
        level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        convert(level, pos, placer instanceof Player player ? player : null);
    }

    /** The old item: still placeable (it becomes a cartography table), marked as outdated. */
    static final class LegacyItem extends BlockItem {
        LegacyItem(Block block, Properties properties) { super(block, properties); }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
            out.accept(Component.translatable("tooltip.simpletweaks.claim_table.legacy"));
        }
    }
}
