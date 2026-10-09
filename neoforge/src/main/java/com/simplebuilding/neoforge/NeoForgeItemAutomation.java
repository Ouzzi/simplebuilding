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
        // Mod-Truhen wie NeoForges eigene Truhen-Anbindung (CapabilityHooks): eine Doppeltruhe ist
        // ein Lager aus beiden Haelften. VanillaContainerWrapper fragt getMaxStackSize(stack) der
        // Block-Entity, Rohre sehen also die x2/x4-Plaetze von Netherit und Enderit.
        event.registerBlockEntity(Capabilities.Item.BLOCK, NeoForgeModRegistries.TIERED_CHEST_BE.get(),
                (chest, side) -> com.simplebuilding.neoforge.NeoForgeItemAutomation.chestHandler(chest));
        if (NeoForgeModRegistries.TRAPPED_COPPER_CHEST_BE != null) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, NeoForgeModRegistries.TRAPPED_COPPER_CHEST_BE.get(),
                    (chest, side) -> com.simplebuilding.neoforge.NeoForgeItemAutomation.chestHandler(chest));
        }
        // Gestufte Shulkerkisten wie NeoForges Vanilla-Shulkerkiste: seitenweise ueber WorldlyContainer
        // (nimmt keine Shulkerkisten an), die x2/x4-Plaetze ueber getMaxStackSize(stack).
        event.registerBlockEntity(Capabilities.Item.BLOCK, NeoForgeModRegistries.TIERED_SHULKER_BOX_BE.get(), WorldlyContainerWrapper::new);
    }

    /** Die Truhe, bei einer Doppeltruhe beide Haelften als ein Lager (erst die rechte, wie Vanilla). */
    static ResourceHandler<ItemResource> chestHandler(net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
        net.minecraft.world.level.Level level = chest.getLevel();
        net.minecraft.world.level.block.state.BlockState state = chest.getBlockState();
        if (level == null || !(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock block)) {
            return VanillaContainerWrapper.of(chest);
        }
        return block.combine(state, level, chest.getBlockPos(), true).apply(
                new net.minecraft.world.level.block.DoubleBlockCombiner.Combiner<net.minecraft.world.level.block.entity.ChestBlockEntity, ResourceHandler<ItemResource>>() {
                    @Override
                    public ResourceHandler<ItemResource> acceptDouble(net.minecraft.world.level.block.entity.ChestBlockEntity first,
                                                                      net.minecraft.world.level.block.entity.ChestBlockEntity second) {
                        return new net.neoforged.neoforge.transfer.CombinedResourceHandler<>(
                                VanillaContainerWrapper.of(first), VanillaContainerWrapper.of(second));
                    }

                    @Override
                    public ResourceHandler<ItemResource> acceptSingle(net.minecraft.world.level.block.entity.ChestBlockEntity single) {
                        return VanillaContainerWrapper.of(single);
                    }

                    @Override
                    public ResourceHandler<ItemResource> acceptNone() {
                        return VanillaContainerWrapper.of(chest);
                    }
                });
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
