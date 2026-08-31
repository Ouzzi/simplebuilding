package com.simplebuilding.clienttest;

import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.gui.BuildingWandScreen;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.client.gui.OctantScreen;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Opens the mod's three screens in a running client and checks that they appear, survive a few
 * frames and close again. A screen that throws while initialising or rendering crashes the client,
 * which fails the whole client game test run - so "still on screen after 20 ticks" is a real
 * statement about init and render, not a tautology.
 *
 * <p>Two of the three go through the real trigger path:
 * <ul>
 *   <li>the octant screen via the mod's settings key binding with an octant in the main hand,</li>
 *   <li>the netherite hopper menu via an actual right click on a placed netherite hopper, which
 *       also exercises the server side menu opening and the {@code MenuScreens} registration.</li>
 * </ul>
 * The building wand screen is opened directly, because its key binding path additionally requires
 * a Constructor's Touch enchantment on the wand and the enchantment component syntax is the part
 * of this that is most likely to change between Minecraft versions.
 *
 * <p>1.21.11 difference: the current screen is the public field {@code Minecraft.screen}, not the
 * {@code Minecraft.gui.screen()} accessor of 26.2.
 */
public final class ModScreensClientGameTest implements FabricClientGameTest {

    private static final BlockPos HOPPER_POS = new BlockPos(10, 1, RendererTestScene.FRONT_Z);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            RendererTestScene.build(context, singleplayer, "minecraft:stone", "creative");
            // Screens are unaffected by the hidden HUD, but the screenshots read better with it.
            RendererTestScene.showHudAgain(context);

            testOctantScreenViaSettingsKey(context, singleplayer);
            testBuildingWandScreen(context);
            testNetheriteHopperMenu(context, singleplayer);
        }
    }

    private void testOctantScreenViaSettingsKey(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("item replace entity @a weapon.mainhand with simplebuilding:octant");
        RendererTestScene.awaitOnClient(context,
                client -> client.player != null
                        && client.player.getMainHandItem().getItem() instanceof OctantItem,
                "the octant arriving in the main hand");

        context.getInput().pressKey(ClientState.settingsKey);
        context.waitForScreen(OctantScreen.class);
        context.waitTicks(20);

        assertStillOpen(context, OctantScreen.class, "octant screen");
        context.takeScreenshot("screen-a-octant");
        closeScreen(context);
    }

    private void testBuildingWandScreen(ClientGameTestContext context) {
        context.setScreen(() -> new BuildingWandScreen(new ItemStack(ModItems.NETHERITE_BUILDING_WAND)));
        context.waitForScreen(BuildingWandScreen.class);
        context.waitTicks(20);

        assertStillOpen(context, BuildingWandScreen.class, "building wand screen");
        context.takeScreenshot("screen-b-building-wand");
        closeScreen(context);
    }

    private void testNetheriteHopperMenu(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runCommand(
                "setblock " + HOPPER_POS.getX() + " " + HOPPER_POS.getY() + " " + HOPPER_POS.getZ()
                        + " simplebuilding:netherite_hopper");
        RendererTestScene.awaitOnClient(context,
                client -> client.level != null
                        && !client.level.getBlockState(HOPPER_POS).isAir(),
                "the netherite hopper arriving in the client world");

        // Slightly upwards so the ray enters the hopper's top plate (y 0.625..1.0 of the block)
        // head on instead of grazing the funnel below it.
        RendererTestScene.aimAt(context, singleplayer, "0.0", "-4.0", HOPPER_POS, Direction.NORTH);

        context.getInput().pressMouse(1);
        context.waitForScreen(NetheriteHopperScreen.class);
        context.waitTicks(20);

        assertStillOpen(context, NetheriteHopperScreen.class, "netherite hopper menu");
        context.takeScreenshot("screen-c-netherite-hopper");
        closeScreen(context);
    }

    private void assertStillOpen(ClientGameTestContext context, Class<? extends Screen> screenClass, String label) {
        String actual = context.computeOnClient(client ->
                client.screen == null ? "none" : client.screen.getClass().getName());

        if (!actual.equals(screenClass.getName())) {
            throw new AssertionError("The " + label + " did not stay open: current screen is " + actual);
        }
    }

    private void closeScreen(ClientGameTestContext context) {
        context.setScreen(() -> null);
        context.waitForScreen(null);
        context.waitTicks(5);
    }
}
