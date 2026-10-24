package com.simplebuilding.modules.simpletrims;

import com.simpletrims.SimpleTrims;
import com.simpletrims.TrimsRegistry;
import com.simpletrims.placed.PlacedTrimTemplateBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class TrimsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleTrims.init(FabricLoader.getInstance().getConfigDir());
        TrimsRegistry.registerComponents();
        TrimsRegistry.registerBlocks();
        TrimsRegistry.registerItems();
        TrimsRegistry.registerBlockEntity(new BlockEntityType<>(PlacedTrimTemplateBlockEntity::new, java.util.Set.of(TrimsRegistry.PLACED_TEMPLATE)));
        TrimsRegistry.registerRecipeSerializers();
        TrimsRegistry.registerTab();
        // a player's light is removed with the player (EntityRemovedTrimsMixin); the table must not outlive the world
        com.simplelib.api.PlayerState.onStop("simpletrims:light", com.simpletrims.light.TrimLight::forgetAll);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> {
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleTrims.MOD_ID)) return;
            var stacks = TrimsRegistry.tabStacks();
            // Behind the last Vanilla armor trim template (where SimpleBuilding had them); a tail append clashes with
            // other mods' insertAfter at the tail.
            var lastTrim = out.getDisplayStacks().stream().filter(com.simpletrims.TemplateTools::isTrimTemplate)
                    .filter(s -> "minecraft".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace()))
                    .reduce((first, second) -> second);
            if (lastTrim.isPresent()) {
                out.insertAfter(lastTrim.get().getItem(), stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleTrims.on()) return;
            var book = java.util.List.of(com.simpletrims.guide.TrimsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpletrims.guide.TrimsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
