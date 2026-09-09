package com.simplebuilding.clientgametest;

import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The scene every client test starts from, as steps.
 *
 * <p>The geometry is the same one the Fabric and the NeoForge suites already used before they
 * shared any code - that was deliberate, so a result means the same thing on every target:
 *
 * <pre>
 *   wall   z = 20, x = -12..32, y = -4..24
 *   floor  y = -1, z = 10..19
 *   player x = 10.5, y = 0.0, z = 16.5, yaw 0 (facing +Z / south), pitch 0
 *   eye    y = 1.62  -&gt;  crosshair ray hits block (10, 1, 20) through its NORTH face
 * </pre>
 *
 * <p><b>The scene is rebuilt, not adjusted.</b> Every run fills the whole working volume with air
 * before building into it, kills every non-player entity, clears the inventory and teleports the
 * player back to the fixed spot. That is what lets the whole suite share one world: whatever the
 * test before it left behind is gone before the next one looks.
 *
 * <p><b>Determinism.</b> Time, weather, mob spawning, random ticks and fire spread are switched
 * off, and the client side options that could move pixels between two screenshots are frozen -
 * field of view, view bob, shadows, clouds, particles, camera type. The HUD is hidden, which in
 * 26.2 also removes the first person hand ({@code GameRenderer.renderItemInHand} returns early
 * when the HUD is hidden), so swapping the held item cannot change pixels by itself.
 */
public final class TestScene {

    private TestScene() {
    }

    /** The block the crosshair points at once the scene is built. */
    public static final BlockPos TARGET = new BlockPos(10, 1, 20);

    /** The face of {@link #TARGET} the ray enters through. */
    public static final Direction TARGET_FACE = Direction.NORTH;

    /** The z plane the wall stands in. */
    public static final int WALL_Z = 20;

    /**
     * Builds the scene and leaves the player aimed at {@link #TARGET}.
     *
     * @param wallBlockId the block id the wall and floor are made of
     * @param gameMode    {@code creative} or {@code survival}
     */
    public static void build(Script script, String wallBlockId, String gameMode) {
        // The snake_case game rule ids are the ones both Minecraft lines use. The camelCase names
        // that used to stand here do not exist any more, and every one of those commands was
        // silently a no-op: the command path swallows the brigadier error instead of reporting it,
        // so a misspelled game rule looks exactly like a working one.
        for (String rule : new String[] {
                "advance_time false", "advance_weather false", "spawn_mobs false",
                "spawn_monsters false", "spawn_phantoms false", "spawn_patrols false",
                "spawn_wandering_traders false", "fire_spread_radius_around_player 0",
                "mob_griefing false", "random_tick_speed 0"}) {
            script.command("gamerule " + rule);
        }
        script.command("time set noon");
        script.command("weather clear");
        script.command("gamemode " + gameMode + " @a");
        // Throws when there is nothing to kill, which is a perfectly good outcome here.
        script.command("kill @e[type=!minecraft:player]", true);
        // These four are the "already in that state is fine" ones. Vanilla treats "no blocks
        // were filled" and "nothing to clear" as command failures, and after the first test in a
        // run the volume is often already what the next one wants. Marked one by one rather than
        // by switching error reporting off, because the commands above them - the game rules -
        // are exactly the ones where a swallowed error cost this suite months of silence.
        script.command("fill -12 -4 10 32 24 20 minecraft:air", true);
        script.command("fill -12 -4 " + WALL_Z + " 32 24 " + WALL_Z + " " + wallBlockId, true);
        script.command("fill -12 -1 10 32 -1 19 " + wallBlockId, true);
        script.command("clear @a", true);
        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");

        script.awaitPackets();
        script.idle("let the world settle", 40);
        script.awaitChunks();
        script.idle("let the chunks settle", 10);

        makeRenderingDeterministic(script);
        script.idle("let the options take effect", 10);

        assertAimedAt(script, TARGET, TARGET_FACE);
    }

    /** Freezes everything client side that could move pixels between two screenshots. */
    public static void makeRenderingDeterministic(Script script) {
        script.act("freeze the rendering options", client -> {
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

    /** Undoes the HUD toggle; the options themselves are reset by the next scene build. */
    public static void showHudAgain(Script script) {
        script.act("show the HUD again", client -> {
            if (client.gui.hud.isHidden()) {
                client.gui.hud.toggle();
            }
        });
    }

    /**
     * Fails unless the crosshair reports that block and that face.
     *
     * <p>Asserted before any screenshot is taken, so a scene that was set up wrong fails as a
     * setup error and can never be reported as "the renderer draws nothing".
     */
    public static void assertAimedAt(Script script, BlockPos expected, Direction expectedFace) {
        script.await("aim at " + expected + " through its " + expectedFace + " face", 120, client -> {
            HitResult hit = client.hitResult;
            return hit instanceof BlockHitResult blockHit
                    && hit.getType() == HitResult.Type.BLOCK
                    && blockHit.getBlockPos().equals(expected)
                    && blockHit.getDirection() == expectedFace;
        }, TestScene::describeAim);
    }

    /**
     * What the client actually looked like, for the message of a failed aim.
     *
     * <p>Every measurement in this suite stands on the crosshair being where the scene says. When
     * that fails, "step timed out" sends the reader looking at the renderer; this line usually
     * names the cause outright - a player who did not arrive, a chunk that was not there yet, a
     * held item that changed the reach.
     */
    public static String describeAim(net.minecraft.client.Minecraft client) {
        if (client.player == null) {
            return "there is no player";
        }
        HitResult hit = client.hitResult;
        String target = switch (hit == null ? HitResult.Type.MISS : hit.getType()) {
            case BLOCK -> hit instanceof BlockHitResult b
                    ? "block " + b.getBlockPos() + " face " + b.getDirection()
                    : "a block hit that is not a BlockHitResult";
            case ENTITY -> "an entity";
            case MISS -> "nothing";
        };
        String world = client.level == null ? "no level"
                : "block " + TARGET + " is " + client.level.getBlockState(TARGET);
        return "the player is at " + client.player.position()
                + ", yaw " + client.player.getYRot() + ", pitch " + client.player.getXRot()
                + ", holding " + client.player.getMainHandItem()
                + ", game mode " + (client.gameMode == null ? "?" : client.gameMode.getPlayerMode())
                + "; the crosshair reports " + target
                + "; " + world
                + "; mouse grabbed " + client.mouseHandler.isMouseGrabbed()
                + ", window active " + client.isWindowActive()
                + ", screen " + (client.gui.screen() == null ? "none"
                        : client.gui.screen().getClass().getSimpleName());
    }
}
