package com.simplebuilding.modules.simplegadgets;

import com.simplegadgets.GadgetProtection;
import com.simplegadgets.GadgetsRegistry;
import com.simplegadgets.network.LaserPayload;
import com.simplegadgets.network.RodNetwork;
import com.simplegadgets.SimpleGadgets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class GadgetsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleGadgets.init(FabricLoader.getInstance().getConfigDir());
        GadgetsRegistry.registerItems();
        GadgetsRegistry.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleGadgets.on()) return;
            // With SimpleBuilding its search tab chain puts them into its gadget row (by id); alone behind the recovery compass.
            if (!FabricLoader.getInstance().isModLoaded(SimpleGadgets.OLD_NAMESPACE)
                    && com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleGadgets.MOD_ID)) {
                var stacks = GadgetsRegistry.gadgetStacks().stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
                if (!stacks.isEmpty()) {
                    if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.RECOVERY_COMPASS))) out.insertAfter(Items.RECOVERY_COMPASS, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
                }
            }
            var book = java.util.List.of(com.simplegadgets.guide.GadgetsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        // The Resonance Rod's dot: both ways, relayed by the server (SimpleBuilding's payload moved here as simplegadgets:laser_pos).
        PayloadTypeRegistry.serverboundPlay().register(LaserPayload.ID, LaserPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LaserPayload.ID, LaserPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(LaserPayload.ID,
                (payload, context) -> context.server().execute(() -> RodNetwork.handleLaser(payload, context.player())));
        RodNetwork.setPlayerSender((player, payload) -> {
            if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        });
        // Claim and protection mods guard blocks through the loader's break event: fire it with the real player (Fabric has no place event).
        GadgetProtection.setBuildGuard(new GadgetProtection.BuildGuard() {
            @Override
            public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                boolean allowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity);
                if (!allowed) PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, pos, state, blockEntity);
                return allowed;
            }

            @Override
            public boolean mayPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
                BlockState current = level.getBlockState(pos);
                return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, current, level.getBlockEntity(pos));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> com.simplegadgets.rotator.RotatorItem.onDisconnect(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.simplegadgets.rotator.RotatorItem.clearAll());
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplegadgets.guide.GadgetsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
