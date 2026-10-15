package com.simplebuilding.modules.simplestats;

import com.simplebuilding.modules.simplestats.client.ItemStatsScreen;
import com.simplebuilding.modules.simplestats.data.StatData;
import com.simplebuilding.modules.simplestats.data.StatKeys;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Boots the mod, opens the statistics screen for a pickaxe with many counted blocks and takes screenshots (title, screen, scrolled). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplestats")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplestats-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            var delta = new StatData.Delta().record(StatKeys.R_BIGGEST_HIT, 640).add(StatKeys.KILLS, StatKeys.L_KILLS, "minecraft:zombie", 41)
                    .entry(StatKeys.L_KILL_VARIANTS, "minecraft:zombie#baby", 3).entry(StatKeys.L_KILL_VARIANTS, "minecraft:zombie", 38);
            for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                delta.add(StatKeys.BLOCKS_MINED, StatKeys.L_BLOCKS, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString(), 1 + block.hashCode() % 50 + 50);
            }
            pick.set(StatsComponents.ITEM_STATS, StatData.EMPTY.apply(delta, StatData.Budget.DEFAULT, 20741));
            context.runOnClient(c -> c.setScreenAndShow(new ItemStatsScreen(pick, null)));
            context.waitTicks(5);
            context.takeScreenshot("simplestats-screen");
            context.runOnClient(c -> c.gui.screen().mouseScrolled(0, 0, 0, -10));
            context.waitTicks(2);
            context.takeScreenshot("simplestats-scrolled");
            context.runOnClient(c -> c.setScreenAndShow(null));
        }
    }
}
