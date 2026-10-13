package com.simplebuilding.modules.simplefun;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

public final class ModuleClientSmoke implements FabricClientGameTest {
  public void runTest(ClientGameTestContext context) {
    context.runOnClient(
        client -> {
          if (!(client.gui.screen() instanceof TitleScreen))
            throw new AssertionError("Client reached title screen");
          if (!FabricLoader.getInstance().isModLoaded("simplefun")
              || !FabricLoader.getInstance().isModLoaded("simplebuilding"))
            throw new AssertionError("Both mods boot");
        });
    context.takeScreenshot("simplefun-title");
    try (var world = context.worldBuilder().create()) {
      world.getConnection().waitForClientboundPackets();
      world.getConnection().waitForChunksRender();
      context.runOnClient(
          client -> {
            if (client.level == null || client.player == null)
              throw new AssertionError("Title-to-world joins");
            for (String id : new String[] {"no_damage"})
              if (client
                      .level
                      .registryAccess()
                      .lookupOrThrow(Registries.ENCHANTMENT)
                      .getValue(Identifier.fromNamespaceAndPath("simplefun", id))
                  == null) throw new AssertionError("Enchantment reaches client");
            if (BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
                    Identifier.fromNamespaceAndPath("simplefun", "fun"))
                == null) throw new AssertionError("Creative tab reaches client");
            int slot = 0;
            for (String name :
                new String[] {
                  "brick_snowball"
                })
              client
                  .player
                  .getInventory()
                  .setItem(
                      slot++,
                      new net.minecraft.world.item.ItemStack(
                          BuiltInRegistries.ITEM.getValue(
                              Identifier.fromNamespaceAndPath("simplefun", name))));
            client.player.setYRot(0);
            client.player.setXRot(15);
          });
      context.waitTicks(20);
      context.takeScreenshot("simplefun-world");
      context.runOnClient(
          client -> {
            try {
              var cls = Class.forName("com.simplefun.client.FunConfigScreen");
              client.setScreenAndShow(
                  (Screen) cls.getMethod("build", Screen.class).invoke(null, client.gui.screen()));
            } catch (Exception e) {
              throw new AssertionError("Module config screen opens", e);
            }
          });
      context.waitTicks(5);
      context.takeScreenshot("simplefun-config");
      context.runOnClient(client -> client.setScreenAndShow(null));
    }
  }
}
