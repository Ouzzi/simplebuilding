package com.simplebuilding.modules.__MODID__;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
    @GameTest public void tokenRegistered(GameTestHelper helper) {
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("__MODID__", "token"))) throw new AssertionError("Module token missing");
        helper.succeed();
    }
}
