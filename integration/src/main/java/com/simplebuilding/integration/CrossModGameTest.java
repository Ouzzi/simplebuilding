package com.simplebuilding.integration;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Only public registry ids and vanilla interfaces cross the mod boundary. */
public final class CrossModGameTest {
    @GameTest(maxTicks = 100)
    public void exampleItemFitsSimpleBuildingStorage(GameTestHelper helper) {
        helper.assertTrue(FabricLoader.getInstance().isModLoaded("simplebuilding")
                && FabricLoader.getInstance().isModLoaded("wiringexample"), "Both mods must be loaded");
        var item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("wiringexample", "token"));
        var block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "reinforced_hopper"));
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, block);
        var entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(entity instanceof Container, "SimpleBuilding hopper exposes vanilla Container");
        var container = (Container) entity;
        container.setItem(0, new ItemStack(item, 3));
        helper.assertTrue(container.getItem(0).is(item) && container.getItem(0).getCount() == 3,
                "SimpleBuilding storage must accept the example mod's item");
        helper.assertTrue(container.removeItem(0, 2).getCount() == 2 && container.getItem(0).getCount() == 1,
                "The foreign item can be retrieved without loss");
        helper.succeed();
    }
}
