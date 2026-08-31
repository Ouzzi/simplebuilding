package com.simplebuilding.clienttest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Builds the single, fully deterministic scene that every in-world renderer test uses, and
 * strips the client of everything that could change pixels for reasons other than the renderer
 * under test.
 *
 * <p><b>Why the scene looks the way it does.</b> The renderer tests are difference tests: take a
 * screenshot without the trigger, take one with the trigger, and require the two to differ. That
 * only proves something about the renderer if <em>nothing else</em> on screen reacts to the
 * trigger. Two things normally would:
 * <ul>
 *   <li>the held item model in the bottom right corner, and</li>
 *   <li>the HUD (hotbar, selected-item name, the mod's own HUD overlays).</li>
 * </ul>
 * Both are suppressed by hiding the HUD: in 26.2 {@code GameRenderer.renderItemInHand} returns
 * early when {@code GuiRenderState.isHudHidden} is set, so {@link net.minecraft.client.gui.Hud#toggle()}
 * removes the HUD <em>and</em> the first person hand. What is left on screen is pure world render.
 *
 * <p>The rest of the setup removes every other source of frame-to-frame variance: a walled-in view
 * of a solid block face (so no sky, sun or clouds are ever visible), a frozen time of day, clear
 * weather, no mobs, no random ticks, no view bobbing and no dynamic FOV.
 *
 * <p>Geometry (all values are exact, nothing is rounded at runtime):
 * <pre>
 *   wall   z = 20, x = -12..32, y = -4..24   (fills the whole viewport at this distance)
 *   floor  y = -1, z = 10..19
 *   player x = 10.5, y = 0.0, z = 16.5, yaw 0 (facing +Z / south), pitch 0
 *   eye    y = 1.62  ->  crosshair ray hits block (10, 1, 20) through its NORTH face
 *   reach  3.5 blocks, comfortably inside the 4.5 block interaction range
 * </pre>
 * Because yaw is exactly 0, {@code player.getDirection()} is SOUTH, so the sledgehammer's
 * {@code getHitSideFromPlayer} resolves to NORTH and its 3x3 pattern lies in the X/Y plane of the
 * wall - every one of the eight neighbours of {@link #TARGET} is a solid wall block.
 */
final class RendererTestScene {

    /** Block the crosshair is aimed at. */
    static final BlockPos TARGET = new BlockPos(10, 1, 20);

    /** Face of {@link #TARGET} the crosshair ray enters through. */
    static final Direction TARGET_FACE = Direction.NORTH;

    /** Z plane of the wall. */
    static final int WALL_Z = 20;

    /** Z plane one block in front of the wall - free air, this is where the wand preview lands. */
    static final int FRONT_Z = 19;

    private RendererTestScene() {
    }

    /**
     * Wipes the area, builds wall and floor out of {@code wallBlockId}, puts the player in
     * {@code gameMode} at the fixed position and verifies that the crosshair really is on
     * {@link #TARGET}. Fails loudly if the aim is off - a test that silently missed its target
     * would report "renderer draws nothing" for the wrong reason.
     */
    static void build(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                      String wallBlockId, String gameMode) {
        String[] commands = {
                // Both 1.21.11 and 26.2 use the snake_case game rule ids; the camelCase names
                // that used to stand here do not exist any more and every one of these eight
                // commands was silently a no-op, because the client gametest API's runCommand
                // goes through Commands.performPrefixedCommand, which swallows the brigadier
                // error instead of reporting it.
                "gamerule advance_time false",
                "gamerule advance_weather false",
                "gamerule spawn_mobs false",
                "gamerule spawn_monsters false",
                "gamerule spawn_phantoms false",
                "gamerule spawn_patrols false",
                "gamerule spawn_wandering_traders false",
                "gamerule fire_spread_radius_around_player 0",
                "gamerule mob_griefing false",
                "gamerule random_tick_speed 0",
                "time set noon",
                "weather clear",
                "gamemode " + gameMode + " @a",
                "kill @e[type=!minecraft:player]",
                // Clear the whole working volume first, then build into it.
                "fill -12 -4 10 32 24 20 minecraft:air",
                "fill -12 -4 " + WALL_Z + " 32 24 " + WALL_Z + " " + wallBlockId,
                "fill -12 -1 10 32 -1 19 " + wallBlockId,
                "clear @a",
                "tp @a 10.5 0.0 16.5 0.0 0.0",
        };

        for (String command : commands) {
            singleplayer.getServer().runCommand(command);
        }

        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(40);
        singleplayer.getConnection().waitForChunksRender();
        context.waitTicks(10);

        makeRenderingDeterministic(context);
        context.waitTicks(10);

        assertAimedAt(context, TARGET, TARGET_FACE);
    }

    /**
     * Hides the HUD (and with it the first person hand, see class javadoc) and freezes every
     * client side option that could otherwise move pixels between two screenshots.
     */
    static void makeRenderingDeterministic(ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.fov().set(70);
            client.options.fovEffectScale().set(0.0);
            client.options.screenEffectScale().set(0.0);
            client.options.bobView().set(false);
            client.options.entityShadows().set(false);
            client.options.cloudStatus().set(CloudStatus.OFF);
            client.options.particles().set(ParticleStatus.MINIMAL);
            client.options.setCameraType(CameraType.FIRST_PERSON);

            if (!client.gui.hud.isHidden()) {
                client.gui.hud.toggle();
            }
        });
    }

    /** Undoes {@link #makeRenderingDeterministic}'s HUD toggle; options are reset by the harness. */
    static void showHudAgain(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (client.gui.hud.isHidden()) {
                client.gui.hud.toggle();
            }
        });
    }

    /** Aims the player and waits until the crosshair reports the expected block and face. */
    static void aimAt(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                      String yaw, String pitch, BlockPos expected, Direction expectedFace) {
        singleplayer.getServer().runCommand("tp @a 10.5 0.0 16.5 " + yaw + " " + pitch);
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);
        assertAimedAt(context, expected, expectedFace);
    }

    static void assertAimedAt(ClientGameTestContext context, BlockPos expected, Direction expectedFace) {
        for (int tick = 0; tick < 120; tick++) {
            boolean onTarget = context.computeOnClient(client ->
                    client.hitResult instanceof BlockHitResult hit
                            && hit.getType() == HitResult.Type.BLOCK
                            && hit.getBlockPos().equals(expected)
                            && hit.getDirection() == expectedFace);

            if (onTarget) {
                return;
            }

            context.waitTick();
        }

        throw new AssertionError("Scene setup failed: crosshair never landed on " + expected
                + " (face " + expectedFace + "). " + describeAim(context));
    }

    /** Human readable dump of everything the aim assertion depends on. */
    static String describeAim(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            String hit;

            if (client.hitResult == null) {
                hit = "none";
            } else if (client.hitResult instanceof BlockHitResult blockHit
                    && blockHit.getType() == HitResult.Type.BLOCK) {
                hit = "block " + blockHit.getBlockPos() + " face " + blockHit.getDirection();
            } else {
                hit = String.valueOf(client.hitResult.getType());
            }

            return String.format(
                    "player=(%.3f, %.3f, %.3f) yaw=%.2f pitch=%.2f mainHand=%s hitResult=%s",
                    client.player.getX(), client.player.getY(), client.player.getZ(),
                    client.player.getYRot(), client.player.getXRot(),
                    client.player.getMainHandItem(), hit);
        });
    }
}
