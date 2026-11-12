package com.simplebuilding.modules.simpleovens;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
/**
 * Client smoke: the module boots; the window of a netherite furnace. PLAN40 simpleovens-4: the furnace boats of every tier
 * (oak, the new poplar ones of simpleovens-1 and the bamboo raft, lit with fuel) on water and the three tier furnace carts
 * on rails, for the owner's look at model, furnace and smoke.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleovens")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simpleovens-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var at = p.blockPosition().offset(0, 0, -3);
                p.level().setBlockAndUpdate(at, com.simpleovens.OvensRegistry.FURNACES.get(com.simpleovens.OvenTier.NETHERITE).defaultBlockState());
                if (p.level().getBlockEntity(at) instanceof net.minecraft.world.MenuProvider mp) p.openMenu(mp);
                else throw new AssertionError("Netherite furnace has no menu provider");
            });
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(10);
            context.takeScreenshot("simpleovens-furnace-window");
            context.runOnClient(c -> c.gui.setScreen(null));
            vehicleScene(world);
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(40);
            context.takeScreenshot("simpleovens-furnace-boats");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().lookAt(
                    net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(sceneBase.offset(4, 0, 6))));
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(20);
            context.takeScreenshot("simpleovens-furnace-carts");
        }
    }

    private static net.minecraft.core.BlockPos sceneBase;

    /** A pool with one lit furnace boat per tier for oak, poplar and the bamboo raft (rows); behind it a rail with the three tier carts. */
    private static void vehicleScene(net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            var l = p.level();
            var base = p.blockPosition().offset(-4, 0, -12);
            sceneBase = base;
            for (int x = -1; x < 11; x++) for (int z = -1; z < 9; z++) for (int y = -1; y < 4; y++) {
                var state = y == -1 ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                        : y == 0 && z < 5 ? net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                l.setBlockAndUpdate(base.offset(x, y, z), state);
            }
            java.util.List<String> woods = java.util.List.of("oak", "poplar", "bamboo");
            java.util.List<com.simpleovens.OvenTier> tiers = com.simpleovens.OvensRegistry.boatTiers();
            for (int row = 0; row < woods.size(); row++) {
                for (int col = 0; col < tiers.size(); col++) {
                    String key = com.simpleovens.OvensRegistry.boatKey(tiers.get(col), woods.get(row));
                    var boat = com.simpleovens.OvensRegistry.boatType(key).create(l, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    if (boat == null) throw new AssertionError("no furnace boat " + key);
                    boat.setPos(base.getX() + 1.5 + col * 2.5, base.getY() + 0.1, base.getZ() + 0.5 + row * 1.6);
                    boat.setYRot(90.0F);
                    boat.addFuel(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL)); // lit: glow and smoke
                    l.addFreshEntity(boat);
                }
            }
            for (int x = 0; x < 10; x++) l.setBlockAndUpdate(base.offset(x, 0, 6), net.minecraft.world.level.block.Blocks.RAIL.defaultBlockState());
            int i = 0;
            for (com.simpleovens.OvenTier tier : com.simpleovens.OvenTier.values()) {
                var cart = com.simpleovens.OvensRegistry.CARTS.get(tier).create(l, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (cart == null) throw new AssertionError("no furnace cart " + tier);
                cart.setPos(base.getX() + 1.5 + i * 3, base.getY(), base.getZ() + 6.5);
                l.addFreshEntity(cart);
                i++;
            }
            p.teleportTo(base.getX() + 5.5, base.getY() + 3, base.getZ() + 12.5);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(base.offset(5, 0, 2)));
        });
    }
}
