package com.simplehammers.test;

import com.simplehammers.SimpleHammers;
import com.simplehammers.compat.HammersViewer;
import com.simplehammers.item.SledgehammerItem;
import com.simplehammers.registry.HammerRegistry;
import com.simplehammers.HammerTiers;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** JEI24: Simple Hammers' recipe viewer data (no client): the standalone ways and the hammer grades of the upgrade tab. */
public final class HammerJeiTests {
    private HammerJeiTests() {
    }

    /**
     * Alone: quartz and diamond crushing (diamond only iron tier and up), stone carves to stone stairs and every carving
     * entry ends where {@link SledgehammerItem#reshapeTarget} says. With SimpleBuilding (it shows them) the list is empty.
     */
    public static void standaloneWays(GameTestHelper h) {
        List<InWorldUpgrade> entries = HammersViewer.entries();
        if (!SimpleHammers.libLoadsUpgrades()) {
            h.assertTrue(entries.isEmpty(), "with SimpleBuilding its own tabs show the hammer ways: " + entries.size());
            h.succeed();
            return;
        }
        InWorldUpgrade quartz = find(entries, "simplehammers/crush/quartz_block");
        h.assertTrue(quartz != null && quartz.output().is(Items.QUARTZ) && quartz.output().getCount() == SledgehammerItem.QUARTZ_BLOCK_QUARTZ
                && quartz.tools().size() == HammerRegistry.HAMMERS.size(), "quartz crush: " + quartz);
        InWorldUpgrade diamond = find(entries, "simplehammers/crush/diamond_block");
        h.assertTrue(diamond != null && diamond.tools().contains(HammerRegistry.hammer(HammerTiers.IRON))
                && !diamond.tools().contains(HammerRegistry.hammer(HammerTiers.STONE)), "diamond crush, iron tier up: " + diamond);
        InWorldUpgrade stone = find(entries, "simplehammers/reshape/minecraft/stone");
        h.assertTrue(stone != null && stone.output().is(Items.STONE_STAIRS), "stone carves to stairs: " + stone);
        for (InWorldUpgrade entry : entries) {
            if (!entry.id().startsWith("simplehammers/reshape/")) continue;
            var from = net.minecraft.world.level.block.Block.byItem(entry.inputs().get(0).items().get(0));
            var target = SledgehammerItem.reshapeTarget(from, false, true).or(() -> SledgehammerItem.reshapeTarget(from, false, false));
            h.assertTrue(target.isPresent() && target.get().asItem() == entry.output().getItem(), "reshape rule drift: " + entry.id());
        }
        h.succeed();
    }

    /** The upgrade tab's tools follow the hammer ranks: a Netherite step takes no diamond hammer, any step takes every hammer. */
    public static void toolGrades(GameTestHelper h) {
        var netherite = new InWorldUpgrades.Entry(Blocks.CAULDRON, Blocks.BARREL, Items.IRON_INGOT, 1, 1, InWorldUpgrades.UPGRADE_TOOLS, "netherite", 1, "");
        var any = new InWorldUpgrades.Entry(Blocks.CAULDRON, Blocks.BARREL, Items.IRON_INGOT, 1, 1, InWorldUpgrades.UPGRADE_TOOLS, "any", 1, "");
        var high = RecipeViewer.tools(netherite);
        var all = RecipeViewer.tools(any);
        h.assertTrue(high.contains(HammerRegistry.hammer(HammerTiers.NETHERITE)) && !high.contains(HammerRegistry.hammer(HammerTiers.DIAMOND)),
                "netherite grade: " + high);
        h.assertTrue(all.containsAll(HammerRegistry.HAMMERS.values()), "any grade takes every hammer: " + all);
        h.succeed();
    }

    private static InWorldUpgrade find(List<InWorldUpgrade> entries, String id) {
        return entries.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
    }
}
