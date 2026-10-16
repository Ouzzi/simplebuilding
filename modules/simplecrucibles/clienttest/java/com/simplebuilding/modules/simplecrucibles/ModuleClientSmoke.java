package com.simplebuilding.modules.simplecrucibles;
import com.simplecrucibles.client.CrucibleScreen;
import com.simplecrucibles.crucible.CrucibleMenu;
import com.simplecrucibles.crucible.CrucibleTier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
/** Client smoke: the module boots; the crucible window with an attached barrel (N37: the barrel box beside the crucible on every tier, inventory centred below). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplecrucibles")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplecrucibles-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            // N39: outline (selection box) of the crucible with a docked barrel, looked at from the side and from above.
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var l = p.level();
                var at = p.blockPosition().offset(0, 0, -3);
                l.setBlockAndUpdate(at, com.simplecrucibles.registry.CrucibleBlocks.NETHERITE_CRUCIBLE.defaultBlockState()
                        .setValue(com.simplecrucibles.crucible.CrucibleBlock.FACING, net.minecraft.core.Direction.EAST));
                l.setBlockAndUpdate(at.west(), com.simplecrucibles.registry.CrucibleBlocks.NETHERITE_BARREL.defaultBlockState()
                        .setValue(com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACHED, true)
                        .setValue(com.simplecrucibles.crucible.CrucibleBarrelBlock.FACING, net.minecraft.core.Direction.EAST));
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, -0.2, 0));
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            context.takeScreenshot("simplecrucibles-crucible-hitbox");
            // N51: every stage of the in-world build (16 iron ingots) and of each crucible upgrade (bands per unit).
            // (literal names: the test runner reads them from the source)
            buildScene(world);
            look(context, world, 0, 0);
            context.takeScreenshot("simplecrucibles-crucible-build-stages-1-4");
            look(context, world, 1, 0);
            context.takeScreenshot("simplecrucibles-crucible-build-stages-5-8");
            look(context, world, 2, 0);
            context.takeScreenshot("simplecrucibles-crucible-build-stages-9-12");
            look(context, world, 3, 0);
            context.takeScreenshot("simplecrucibles-crucible-build-stages-13-16");
            look(context, world, -1, 1);
            context.takeScreenshot("simplecrucibles-crucible-upgrade-iron-south-east");
            look(context, world, -2, 1);
            context.takeScreenshot("simplecrucibles-crucible-upgrade-iron-north-west");
            look(context, world, -1, 2);
            context.takeScreenshot("simplecrucibles-crucible-upgrade-reinforced-south-east");
            look(context, world, -2, 2);
            context.takeScreenshot("simplecrucibles-crucible-upgrade-reinforced-north-west");
            look(context, world, -1, 3);
            context.takeScreenshot("simplecrucibles-crucible-upgrade-netherite-south-east");
            look(context, world, -2, 3);
            context.takeScreenshot("simplecrucibles-crucible-upgrade-netherite-north-west");
            crucible(context, CrucibleTier.NETHERITE);
            context.takeScreenshot("simplecrucibles-crucible-netherite-barrel");
            crucible(context, CrucibleTier.ENDERITE);
            context.takeScreenshot("simplecrucibles-crucible-enderite-barrel");
            // Layout comparison: 854x480 (GUI scale 2) and 1920x1080 (auto scale) with the barrel docked.
            // (a real window resize, so the GUI re-lays out at the auto scale)
            context.runOnClient(c -> c.getWindow().setWindowed(1920, 1080));
            context.waitTicks(3);
            context.takeScreenshot("simplecrucibles-crucible-enderite-barrel-1080");
            context.runOnClient(c -> c.getWindow().setWindowed(854, 480));
            context.waitTicks(3);
            // N37: every tier with and without a barrel (7 px margins, compact boxes).
            // (literal names: the test runner reads them from the source)
            crucible(context, CrucibleTier.IRON, false);
            context.takeScreenshot("simplecrucibles-crucible-iron-alone");
            crucible(context, CrucibleTier.IRON, true);
            context.takeScreenshot("simplecrucibles-crucible-iron-with-barrel");
            crucible(context, CrucibleTier.REINFORCED, false);
            context.takeScreenshot("simplecrucibles-crucible-reinforced-alone");
            crucible(context, CrucibleTier.REINFORCED, true);
            context.takeScreenshot("simplecrucibles-crucible-reinforced-with-barrel");
            crucible(context, CrucibleTier.NETHERITE, false);
            context.takeScreenshot("simplecrucibles-crucible-netherite-alone");
            crucible(context, CrucibleTier.NETHERITE, true);
            context.takeScreenshot("simplecrucibles-crucible-netherite-with-barrel");
            crucible(context, CrucibleTier.ENDERITE, false);
            context.takeScreenshot("simplecrucibles-crucible-enderite-alone");
            crucible(context, CrucibleTier.ENDERITE, true);
            context.takeScreenshot("simplecrucibles-crucible-enderite-with-barrel");
            context.runOnClient(c -> c.gui.setScreen(null));
        }
    }

    private static net.minecraft.core.BlockPos sceneBase;

    /**
     * Row 0: the blank stages 1-15 and the finished iron crucible, two blocks apart. Rows 1-3: iron, reinforced and
     * netherite crucibles with 0-3 upgrade units, then the next tier (enderite only with SimpleBuilding: left out).
     */
    private static void buildScene(net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            var l = p.level();
            var base = p.blockPosition().offset(-40, 30, -40);
            sceneBase = base;
            for (int x = -6; x < 40; x++) for (int y = -1; y < 6; y++) for (int z = -32; z < 8; z++)
                l.setBlockAndUpdate(base.offset(x, y, z), y == -1 ? net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            for (int i = 0; i < 16; i++) {
                var state = i < 15 ? com.simplecrucibles.registry.CrucibleBlocks.CRUCIBLE_BLANK.defaultBlockState()
                        .setValue(com.simplecrucibles.crucible.CrucibleBlankBlock.STAGE, i + 1)
                        : com.simplecrucibles.registry.CrucibleBlocks.IRON_CRUCIBLE.defaultBlockState();
                l.setBlockAndUpdate(base.offset(2 * i, 0, 0), state);
            }
            net.minecraft.world.level.block.Block[] tiers = {com.simplecrucibles.registry.CrucibleBlocks.IRON_CRUCIBLE,
                    com.simplecrucibles.registry.CrucibleBlocks.REINFORCED_CRUCIBLE, com.simplecrucibles.registry.CrucibleBlocks.NETHERITE_CRUCIBLE};
            for (int t = 0; t < 3; t++) {
                for (int units = 0; units < 4; units++) {
                    l.setBlockAndUpdate(base.offset(2 * units, 0, -8 * (t + 1)),
                            tiers[t].defaultBlockState().setValue(com.simplecrucibles.crucible.CrucibleBlock.UPGRADE, units));
                }
                if (t < 2) l.setBlockAndUpdate(base.offset(8, 0, -8 * (t + 1)), tiers[t + 1].defaultBlockState());
            }
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
        });
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
    }

    /** Camera on row {@code row}: build shots {@code shot} 0-3 (four stages each); upgrade rows -1 = from south-east, -2 = from north-west. */
    private static void look(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world, int shot, int row) {
        world.getServer().runOnServer(server -> {
            var b = sceneBase;
            double cx, cz = b.getZ() - 8 * row + 0.5, eyeX, eyeZ;
            if (row == 0) {
                cx = b.getX() + 8 * shot + 3.5;
                eyeX = cx;
                eyeZ = cz + 4.5;
            } else {
                cx = b.getX() + 4.5;
                boolean southEast = shot == -1;
                eyeX = cx + (southEast ? 3.5 : -3.5);
                eyeZ = cz + (southEast ? 3.5 : -3.5);
            }
            String cmd = String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f facing %.2f %.2f %.2f",
                    eyeX, (double) b.getY(), eyeZ, cx, b.getY() + 0.35, cz);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
        });
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
        context.waitTicks(10);
    }

    /** A client-side crucible menu of {@code tier} with a barrel attached and a few items, opened and rendered. */
    private static void crucible(ClientGameTestContext context, CrucibleTier tier) {
        crucible(context, tier, true);
    }

    private static void crucible(ClientGameTestContext context, CrucibleTier tier, boolean barrel) {
        context.runOnClient(c -> {
            Inventory inv = c.player.getInventory();
            CrucibleMenu menu = CrucibleMenu.client(tier, 0, inv);
            menu.setData(tier.slots() + 3, barrel ? 1 : 0);
            menu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 12));
            menu.getSlot(menu.wideCrucibleStart()).set(new ItemStack(Items.RAW_IRON, 12));
            menu.getSlot(menu.barrelStart()).set(new ItemStack(Items.IRON_INGOT, 5));
            c.setScreenAndShow(new CrucibleScreen(menu, inv, Component.translatable("container.simplecrucibles." + tier.id() + "_crucible")));
        });
        context.waitTicks(3);
        context.runOnClient(c -> {
            if (!(c.gui.screen() instanceof CrucibleScreen)) throw new AssertionError(tier + " crucible screen closed itself: " + c.gui.screen());
        });
    }
}
