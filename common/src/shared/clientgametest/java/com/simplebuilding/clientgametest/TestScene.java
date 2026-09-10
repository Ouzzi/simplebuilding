package com.simplebuilding.clientgametest;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CloudStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
        // Explicit, because the two drivers create their worlds differently: NeoForge's opens the
        // world in Peaceful, Fabric's does not, and /summon refuses a monster in Peaceful with
        // "commands.summon.failed.peaceful" - which is how the smoke test's creeper existed on one
        // loader and not on the other. Natural spawning is off through the game rules above
        // either way, so Easy changes nothing else in this scene. Tolerated, because "already
        // easy" is a command failure in vanilla, exactly like a fill that fills nothing.
        script.command("difficulty easy", true);
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
        rebuildEveryChunk(script);
        script.awaitChunks();
        script.idle("let the chunks settle", 10);

        makeRenderingDeterministic(script);
        script.idle("let the options take effect", 10);

        assertAimedAt(script, TARGET, TARGET_FACE);
        assertTheClientSeesAnEmptyScene(script);
        // Last, because it is the only one of these that looks at the picture itself. Twenty ticks
        // is the gap the shortest noise floor in the suite uses; four attempts with a rebuild
        // between them is more than any run has needed and still under the timeout of one step.
        script.awaitStableFrame("scene", 20, 4, client -> client.levelExtractor.allChanged());
    }

    /**
     * Fails unless the <em>client</em> agrees that the working volume in front of the camera is
     * empty.
     *
     * <p>The three fills above are server commands, and the server reporting "1831 blocks filled"
     * says nothing about what this client is drawing. That gap is not hypothetical: a run on
     * 26.2 had the ladder column that {@code AirJumpClientTest} builds still on screen seven
     * seconds after the fill that removed it, and it was still changing between two screenshots
     * taken three seconds apart. The test that paid for it was the next one in the list, and what
     * it reported was "Scene is not deterministic - 4950 changed pixels while nothing changed on
     * screen" - a true statement that names neither the block nor the test that left it behind.
     *
     * <p>Read from {@code client.level}, so it is the client's own copy that is checked and not
     * the server's. The box is the part of the volume the camera can actually see: from the floor
     * up to head height, in front of the player, out to the wall. The floor (y = -1) and the wall
     * (z = {@link #WALL_Z}) are excluded, because those are the scene.
     *
     * <p>A wait rather than a plain check, because the block data legitimately takes a few ticks
     * to arrive - but a short one, since everything before it has already waited for the packets
     * and the chunks.
     */
    public static void assertTheClientSeesAnEmptyScene(Script script) {
        script.await("the client sees the working volume as empty", 100,
                client -> firstLeftoverBlock(client) == null,
                client -> {
                    String leftover = firstLeftoverBlock(client);
                    return "The client still has " + leftover + " in the working volume, which the "
                            + "scene build filled with air. Whatever ran before this test left it "
                            + "behind, or the client never rebuilt that chunk section - either way "
                            + "every screenshot below would compare a scene that is still changing.";
                });
    }

    /** The first block the client still has inside the volume, described, or null if it is clean. */
    private static String firstLeftoverBlock(Minecraft client) {
        if (client.level == null) {
            return "no client level at all";
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int z = 11; z < WALL_Z; z++) {
            for (int y = 0; y <= 6; y++) {
                for (int x = 2; x <= 20; x++) {
                    cursor.set(x, y, z);

                    if (!client.level.getBlockState(cursor).isAir()) {
                        return BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(cursor).getBlock())
                                + " at " + cursor.getX() + "/" + cursor.getY() + "/" + cursor.getZ();
                    }
                }
            }
        }

        return null;
    }


    /**
     * Throws every built chunk section away, so the next frame is drawn from the block data that
     * is actually there.
     *
     * <p>What this is for: the fills above are server commands, and the client gets the block
     * updates long before the geometry catches up. Waiting is not reliably enough - a run had the
     * ladder column that {@code AirJumpClientTest} builds still on screen seven seconds after the
     * fill that removed it, with the client's own block data already clean, and it was still
     * fading between two screenshots taken three seconds apart. The test that paid for that was
     * the next one in the list, and what it reported was "Scene is not deterministic - 4950
     * changed pixels while nothing changed on screen".
     *
     * <p>This is what F3+A does, so it is a supported thing to ask of the renderer rather than a
     * poke at its internals. It costs a rebuild of a handful of sections in a flat, mostly empty
     * test world.
     *
     * <p>Line difference: on 26.2 the extraction pass owns this
     * ({@code Minecraft.levelExtractor}); on 1.21.11 it is still {@code Minecraft.levelRenderer}.
     */
    public static void rebuildEveryChunk(Script script) {
        script.act("throw the built chunks away so they are rebuilt from the new blocks",
                client -> client.levelExtractor.allChanged());
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
