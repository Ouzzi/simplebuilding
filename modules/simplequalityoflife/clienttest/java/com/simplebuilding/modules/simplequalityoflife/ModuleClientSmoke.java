package com.simplebuilding.modules.simplequalityoflife;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(c->{
            if (!(c.gui.screen() instanceof net.minecraft.client.gui.screens.TitleScreen)) throw new AssertionError("Title screen missing");
            if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplequalityoflife") || !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplebuilding")) throw new AssertionError("Both mods must boot");
            try {
                var type=Class.forName("com.simplequalityoflife.client.SimplequalityoflifeClient");
                if(type.getField("autoWalkKey").get(null)==null || type.getField("crawlKey").get(null)==null) throw new AssertionError("Both key mappings registered");
            } catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
        context.takeScreenshot("simplequalityoflife-title");
        try(var world=context.worldBuilder().create()){
            world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();
            context.runOnClient(c->{
                if(c.player==null||c.level==null)throw new AssertionError("Title-to-world failed");
                try {
                    var config=Class.forName("com.simplequalityoflife.client.ClientNetworking").getMethod("getSyncedConfig").invoke(null);
                    if(config==null)throw new AssertionError("Server config was not synchronized");
                }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            });
            context.takeScreenshot("simplequalityoflife-world");
            context.runOnClient(c->c.player.connection.sendCommand("crawl"));
            context.waitFor(ModuleClientSmoke::crawling);
            context.waitTicks(20);
            context.runOnClient(c->{if(c.player.getPose()!=net.minecraft.world.entity.Pose.SWIMMING)throw new AssertionError("Server-authorized crawl survives client pose updates");});
            context.takeScreenshot("simplequalityoflife-crawl");
            context.runOnClient(c->c.player.connection.sendCommand("crawl"));
            context.waitFor(c->!crawling(c));
            // Linked containers: mark a chest (server), HUD slot, inventory with it, another GUI with it below.
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().get(0);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                var level=(net.minecraft.server.level.ServerLevel)player.level();
                var base=player.blockPosition();
                var marked=base.offset(2,0,0);var other=base.offset(-2,0,0);
                level.setBlockAndUpdate(marked,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(other,net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState());
                var box=(net.minecraft.world.Container)level.getBlockEntity(marked);
                box.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,12));
                box.setItem(4,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,5));
                box.setItem(11,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT,30));
                ((net.minecraft.world.Container)level.getBlockEntity(other)).setItem(2,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,9));
                player.setShiftKeyDown(true);
                try{Class.forName("com.simplequalityoflife.container.LinkedContainers").getMethod("onRightClickBlock",net.minecraft.world.entity.player.Player.class,net.minecraft.world.InteractionHand.class,net.minecraft.core.BlockPos.class).invoke(null,player,net.minecraft.world.InteractionHand.MAIN_HAND,marked);}
                catch(ReflectiveOperationException e){throw new AssertionError(e);}
                player.setShiftKeyDown(false);
            });
            context.waitFor(c->{try{return (Boolean)Class.forName("com.simplequalityoflife.client.LinkedHud").getMethod("active").invoke(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
            context.waitTicks(5);
            context.takeScreenshot("linkhud-hud");
            context.runOnClient(c->c.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.InventoryScreen(c.player)));
            context.waitTicks(5);
            context.takeScreenshot("linkhud-inventory");
            context.runOnClient(c->c.setScreenAndShow(null));
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().get(0);
                var level=(net.minecraft.server.level.ServerLevel)player.level();
                player.openMenu((net.minecraft.world.MenuProvider)level.getBlockEntity(player.blockPosition().offset(-2,0,0)));
            });
            context.waitFor(c->c.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>);
            context.waitTicks(5);
            context.takeScreenshot("linkhud-second-gui");
            context.runOnClient(c->c.setScreenAndShow(null));
            context.runOnClient(c->{try{
                var type=Class.forName("com.simplequalityoflife.client.QolConfigScreen");
                c.setScreenAndShow((net.minecraft.client.gui.screens.Screen)type.getMethod("create",net.minecraft.client.gui.screens.Screen.class).invoke(null,c.gui.screen()));
            }catch(ReflectiveOperationException e){throw new AssertionError("Config screen opens",e);}});
            context.waitTicks(5);context.takeScreenshot("simplequalityoflife-config");
            context.runOnClient(c->c.setScreenAndShow(null));
            var reload=context.computeOnClient(c->{c.getLanguageManager().setSelected("de_de");c.options.languageCode="de_de";return c.reloadResourcePacks();});
            context.waitFor(c->reload.isDone());reload.join();
            context.waitFor(c->c.gui.overlay()==null);
            context.runOnClient(c->{try{
                var type=Class.forName("com.simplequalityoflife.client.QolConfigScreen");
                c.setScreenAndShow((net.minecraft.client.gui.screens.Screen)type.getMethod("create",net.minecraft.client.gui.screens.Screen.class).invoke(null,c.gui.screen()));
            }catch(ReflectiveOperationException e){throw new AssertionError("German config screen opens",e);}});
            context.waitTicks(5);
            context.runOnClient(c->{if(c.gui.screen()==null||!c.gui.screen().getTitle().getString().contains("Servereinstellungen"))throw new AssertionError("German config title is translated and visible");});
            context.takeScreenshot("simplequalityoflife-config-de");
            context.runOnClient(c->c.setScreenAndShow(null));
        }
    }
    private static boolean crawling(net.minecraft.client.Minecraft client){
        if(client.player==null)return false;
        try{return (Boolean)Class.forName("com.simplequalityoflife.util.CrawlAccessor").getMethod("simpleQualityOfLife$isCrawling").invoke(client.player);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
}
