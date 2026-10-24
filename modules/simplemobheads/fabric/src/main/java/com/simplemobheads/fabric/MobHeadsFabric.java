package com.simplemobheads.fabric;

import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.TestCentre;
import com.simplemobheads.command.MobHeadsCommands;
import com.simplemobheads.drop.HeadDrops;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

/** Fabric entry: registries are open during init, so everything registers here in order. */
public final class MobHeadsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleMobHeads.init(FabricLoader.getInstance().getConfigDir());
        MobHeads.components();
        MobHeads.lootTypes(true, true);
        MobHeads.registerBlocks();
        MobHeads.registerItems();
        MobHeads.registerTab();
        TestCentre.register();
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) {
            com.simplemobheads.guide.MobHeadsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
        }
        LootTableEvents.MODIFY.register((key, table, source, registries) -> HeadDrops.apply(key, table::withPool, registries));
        ServerLivingEntityEvents.AFTER_DEATH.register(HeadDrops::onDeath);
        // a haunting is counted in game ticks of one world: it must not survive the player leaving
        com.simplelib.api.PlayerState.onLeave("simplemobheads:haunt", com.simplemobheads.ritual.HerobrineHaunt::stop);
        com.simplelib.api.PlayerState.onStop("simplemobheads:haunt", com.simplemobheads.ritual.HerobrineHaunt::stopAll);
        com.simplelib.api.PlayerState.onStop("simplemobheads:abilities", com.simplemobheads.ability.HeadAbilities::forgetAll);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> MobHeadsCommands.register(dispatcher));
        // Behind the dragon head (the last Vanilla head) in Functional Blocks, so the search tab lists them beside the Vanilla heads.
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            if (!SimpleMobHeads.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(
                    FabricLoader.getInstance().getConfigDir(), SimpleMobHeads.MOD_ID)) {
                return;
            }
            if (entries.getDisplayStacks().stream().anyMatch(s -> s.is(MobHeads.HEAD_ANCHOR))) {
                entries.insertAfter(MobHeads.HEAD_ANCHOR, MobHeads.baseStacks(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
    }
}
