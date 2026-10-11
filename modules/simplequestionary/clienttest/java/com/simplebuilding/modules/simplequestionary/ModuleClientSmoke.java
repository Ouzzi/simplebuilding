package com.simplebuilding.modules.simplequestionary;

import com.simplequestionary.QuestionaryMenu;
import com.simplequestionary.client.QuestionaryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Client smoke: the table in the world, its screen empty, the three pages of a described item, a fallback item. */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplequestionary")) throw new AssertionError("Module did not boot");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set day");
            world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 25");
            world.getServer().runCommand("execute as @p at @s run setblock ~ ~ ~2 simplequestionary:questionary_table[facing=north]");
            world.getServer().runCommand("execute as @p at @s run setblock ~2 ~ ~3 simplequestionary:questionary_table[facing=west]");
            context.waitTicks(30);
            context.takeScreenshot("simplequestionary-block");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = player(server);
                BlockPos pos = player.blockPosition().south(2);
                player.openMenu(player.level().getBlockState(pos).getMenuProvider(player.level(), pos));
            });
            context.waitTicks(10);
            context.runOnClient(c -> {
                if (!(c.gui.screen() instanceof QuestionaryScreen)) throw new AssertionError("table screen not open: " + c.gui.screen());
            });
            context.takeScreenshot("simplequestionary-empty");
            Item described = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("simplebuilding", "enderite_ingot")).orElse(Items.BOOK);
            put(world.getServer(), described);
            context.waitTicks(10);
            String[] names = {"lore", "use", "secret"};
            for (int i = 0; i < 3; i++) {
                int page = i;
                context.runOnClient(c -> ((QuestionaryScreen) c.gui.screen()).setPage(page));
                context.waitTicks(3);
                context.takeScreenshot("simplequestionary-" + names[i]);
            }
            // A Vanilla item without its own text shows a fallback variant.
            put(world.getServer(), Items.ECHO_SHARD);
            context.waitTicks(10);
            context.runOnClient(c -> ((QuestionaryScreen) c.gui.screen()).setPage(0));
            context.waitTicks(3);
            context.takeScreenshot("simplequestionary-fallback");
            context.runOnClient(c -> ((QuestionaryScreen) c.gui.screen()).setPage(1));
            context.waitTicks(3);
            context.takeScreenshot("simplequestionary-fallback-use");
            context.runOnClient(c -> c.player.closeContainer());
            context.waitTicks(5);
        }
    }

    private static void put(TestServerContext onServer, Item item) {
        onServer.runOnServer(server -> {
            ServerPlayer player = player(server);
            if (!(player.containerMenu instanceof QuestionaryMenu menu)) throw new AssertionError("server menu not open");
            menu.getSlot(QuestionaryMenu.ITEM_SLOT).set(new ItemStack(item));
            menu.broadcastChanges();
        });
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }
}
