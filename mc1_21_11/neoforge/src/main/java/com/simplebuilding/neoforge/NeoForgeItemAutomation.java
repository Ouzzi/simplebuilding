package com.simplebuilding.neoforge;

import com.simplebuilding.platform.ItemAutomation;
import com.simplebuilding.platform.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The item handler capability of the mod's machines, and NeoForge's side of {@link ItemAutomation}.
 *
 * <p>NeoForge only attaches {@code Capabilities.Item.BLOCK} to the vanilla block entity types
 * ({@code CapabilityHooks}); a modded type gets nothing unless the mod registers it. Vanilla
 * hoppers still work without it - NeoForge's hopper asks the {@code Container} first - but every
 * pipe and every other mod that goes through the capability sees no inventory at all. Registered the
 * way NeoForge does it for the vanilla furnaces (sided, through {@code getSlotsForFace} /
 * {@code canPlaceItemThroughFace} / {@code canTakeItemThroughFace}) and the vanilla hopper (unsided).
 */
public final class NeoForgeItemAutomation implements ItemAutomation {

    private NeoForgeItemAutomation() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeItemAutomation::registerCapabilities);
        PlatformServices.setItemAutomation(new NeoForgeItemAutomation());
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, NeoForgeModRegistries.MOD_FURNACE_BE.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, NeoForgeModRegistries.MOD_SMOKER_BE.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, NeoForgeModRegistries.MOD_BLAST_FURNACE_BE.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, NeoForgeModRegistries.MOD_HOPPER_BE.get(),
                (hopper, side) -> VanillaContainerWrapper.of(hopper));
    }

    @Override
    public int insert(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) {
            return NO_HANDLER;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = handler.insert(ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return moved;
        }
    }

    @Override
    public int extract(ServerLevel level, BlockPos pos, Direction side, Item item, int amount) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) {
            return NO_HANDLER;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = handler.extract(ItemResource.of(item), amount, transaction);
            transaction.commit();
            return moved;
        }
    }
}
