package com.simplebuilding.forge;

import com.simplebuilding.platform.ItemAutomation;
import com.simplebuilding.platform.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Forge's side of {@link ItemAutomation}: {@code ForgeCapabilities.ITEM_HANDLER}, the capability
 * pipes and other mods ask a block entity for.
 *
 * <p>Unlike NeoForge ({@code NeoForgeItemAutomation}), nothing has to be registered for the mod's
 * machines. Forge 65 still attaches the item handler in the block entity classes themselves: its
 * patch to {@code AbstractFurnaceBlockEntity} answers with a {@code SidedInvWrapper} per face (UP,
 * DOWN, and one for the horizontal sides) that goes through {@code getSlotsForFace} /
 * {@code canPlaceItemThroughFace} / {@code canTakeItemThroughFace}, and its patch to
 * {@code BaseContainerBlockEntity} answers with an unsided {@code InvWrapper}. The mod's furnace,
 * smoker and blast furnace extend {@code AbstractFurnaceBlockEntity} and the mod hopper extends
 * {@code RandomizableContainerBlockEntity}, so they inherit exactly the vanilla furnace's sided and
 * the vanilla hopper's unsided exposure. This class only asks that capability, so the game tests
 * see what a pipe mod sees.
 */
public final class ForgeItemAutomation implements ItemAutomation {

    private ForgeItemAutomation() {
    }

    public static void install() {
        PlatformServices.setItemAutomation(new ForgeItemAutomation());
    }

    private static IItemHandler handler(ServerLevel level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }

    @Override
    public int insert(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        IItemHandler handler = handler(level, pos, side);
        if (handler == null) {
            return NO_HANDLER;
        }
        ItemStack remainder = ItemHandlerHelper.insertItem(handler, stack.copy(), false);
        return stack.getCount() - remainder.getCount();
    }

    @Override
    public int extract(ServerLevel level, BlockPos pos, Direction side, Item item, int amount) {
        IItemHandler handler = handler(level, pos, side);
        if (handler == null) {
            return NO_HANDLER;
        }
        int moved = 0;
        for (int slot = 0; slot < handler.getSlots() && moved < amount; slot++) {
            if (!handler.getStackInSlot(slot).is(item)) {
                continue;
            }
            moved += handler.extractItem(slot, amount - moved, false).getCount();
        }
        return moved;
    }
}
