package com.simplebuilding.clientgametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.util.SurvivalTracerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Proves the client harness works - a real client boots, a world exists, the scene builds, a
 * screenshot lands - and then checks everything the mod contributes to a joined client that is
 * neither a screen nor one of the in-world renderers.
 *
 * <p>This is the first test written in the shared step form. It runs on all four client targets
 * from one body: Fabric and NeoForge, MC 26.2 and 1.21.11.
 *
 * <p><b>Why these live together.</b> The other classes in this package are organised by the thing
 * they measure: {@link BlockHighlightClientTest} and {@link BuildingWandPreviewClientTest} measure
 * pixels, {@link MultiBlockBreakingClientTest} measures one renderer's contribution to the level
 * render state. What is left over are claims that need a real client and a joined world but no
 * scene of their own and no screen: language files, baked models, arriving packets, client side
 * interaction results, sounds and action bar messages. Every one of them is invisible to the
 * headless server suite, and every one of them is a smoke test in the original sense - if it
 * fails, the mod is not correctly installed on a client at all.
 *
 * <p><b>No pixel measurement happens here, and that is why this class has no noise floor.</b> The
 * renderer tests in this package all follow the same order - measure a noise floor from two
 * screenshots of the unchanged scene, assert every trigger condition, take the measuring shot,
 * then take a control shot back at the baseline - because without it a pixel difference proves
 * nothing. This class measures no pixels, so it inherits none of that; what it does inherit is the
 * <em>reason</em> for it, and the same discipline shows up in a different currency: every
 * measurement below is preceded by a control that would fail loudly if the observation channel
 * itself were dead (the {@code I18n} echo, the {@code /playsound} self check, the action bar
 * sentinel, the vanilla piston, the chisel table lookup). Two of the checks could in principle be
 * screenshot differences and deliberately are not:
 * <ul>
 *   <li>the lit furnace fronts are <em>animated</em> textures ({@code *_front_on.png.mcmeta}), so
 *       two screenshots of the same lit furnace differ by design and the noise floor assertion the
 *       other classes rely on could never hold;</li>
 *   <li>the item frame messages are drawn on the HUD, and reaching them means sneaking and right
 *       clicking, which moves the camera and swings the first person hand - two changes on screen
 *       that have nothing to do with the message. Reading the message out of the client's own
 *       state is both cheaper and strictly more precise.</li>
 * </ul>
 * The screenshots this class takes are documentary: a human may want to look at them, nothing is
 * asserted on them. They are still checkpoints - the runner reads the names out of the source and
 * requires a fresh file per name, so a run that died half way through is visible as the missing
 * names.
 *
 * <p><b>Every case rebuilds the scene it needs.</b> The whole in-world list shares one world (see
 * {@link ClientTests}), so a case that leaves a piston, an item frame or a teleported player
 * behind would silently decide whether the next one passes. Each section therefore starts with
 * {@link TestScene#build}, which fills the working volume with air, rebuilds wall and floor, kills
 * every non-player entity, clears the inventory and teleports the player back. The Fabric-only
 * ancestor of this class built the scene once and relied on the order of its sections; that
 * dependency was real and is gone.
 *
 * <p><b>How this class reaches the server, and why that is not a harness method.</b> Two claims
 * need the server's own view - the play time statistic and whether the server believes the player
 * is sneaking. Fabric offers {@code computeOnServer} for that and NeoForge has nothing like it, so
 * the shared form uses the one route both loaders always have:
 * {@code Minecraft.getSingleplayerServer()} plus {@code MinecraftServer.submit}, whose future is
 * then polled across ticks by an {@code await} step ({@link #askTheServer}). Nothing blocks, which
 * is what makes it work on NeoForge at all, and the question really runs on the server thread
 * rather than reading server state from the client thread.
 *
 * <p><b>Known defect - the four item frame sounds never reach anybody.</b>
 * {@code ItemFrameEntityMixin} plays all four of its sounds with {@code player.playSound(...)}
 * inside its {@code if (!isClient)} branch. {@code Player#playSound} passes {@code this} as the
 * "except" argument of {@code Level#playSound}, and on the server that argument is the player who
 * is skipped when the sound packet is broadcast ({@code PlayerList#broadcast} compares
 * {@code player != except}). The interacting player is therefore the one client that never gets
 * the packet, and in single player there is nobody else. The overlay messages next to them work,
 * because {@code ServerPlayer#sendOverlayMessage} sends straight down that player's connection.
 * The sounds are consequently not covered below: a test for them could only assert the broken
 * behaviour.
 *
 * <p><b>Known defect - three block items have no name at all.</b> {@code ModItems#registerItem}
 * builds every item from a bare {@code new Item.Properties().setId(key)} and never calls
 * {@code Item.Properties#useBlockDescriptionPrefix()}. In 26.2 the description id is fixed at
 * construction time out of those properties, and {@code BlockItem} overrides neither
 * {@code getDescriptionId} nor {@code getName}, so a block item is called
 * {@code item.simplebuilding.<path>} and not {@code block.simplebuilding.<path>}. Most of the
 * mod's block items are covered by a second language entry that repeats the block name under the
 * {@code item.} prefix. Three are not, and show the bare key wherever an item name is drawn -
 * hotbar, inventory, creative tab, death message:
 * {@code item.simplebuilding.nihilith_ore} (while {@code astralit_ore}, its twin from the same
 * feature, does have the duplicate entry), {@code item.simplebuilding.polished_end_stone} and
 * {@code item.simplebuilding.lapis_quartz_checker}.
 * {@link #modLanguageFileReachesTheClient} therefore asserts the <em>astralit</em> block item and
 * leaves the nihilith one out. Listing it would pin the defect in place: both repairs - adding the
 * three missing entries, or calling {@code useBlockDescriptionPrefix()} and deleting the
 * duplicated {@code item.} entries - would have to fight the test to land.
 *
 * <p><b>Known defect - {@code tooltip.simplebuilding.netherite_piston} is an orphan.</b> The key
 * exists in both language files but no code anywhere in the mod calls {@code appendHoverText} for
 * the piston, so it is never shown. It is deliberately left out of
 * {@link #modLanguageFileReachesTheClient} - asserting that it resolves would turn deleting the
 * dead key, which is one of the two correct fixes, into a red test.
 *
 * <p><b>Known defect - the item frame messages are hardcoded literals.</b> All four go through
 * {@code Component.literal} with German text ("Item Frame gesperrt (Locked).") instead of a
 * translation key, so they cannot be translated and they ignore the client language. The test
 * below asserts the literal strings as the mod writes them today; the day they become translatable
 * it has to be updated, and that is the point at which somebody reads this paragraph.
 *
 * <p><b>Not covered</b>
 * <ul>
 *   <li><b>"Other languages fall back to English."</b> That is vanilla, not the mod:
 *       {@code LanguageManager} always stacks the selected language on top of {@code en_us}, and
 *       the mod contributes nothing to it.</li>
 *   <li><b>The chisel sound pitch.</b> {@code ChiselItem} randomises it per use
 *       ({@code 1.0 + random*0.4 - 0.2}), so the test can only assert the 0.8..1.2 band, not a
 *       value.</li>
 *   <li><b>The mod config screen.</b> Both loaders register it through their own entry point
 *       ({@code ModMenu} on Fabric, {@code IConfigScreenFactory} on NeoForge) and neither is on
 *       the other's classpath, so a shared body cannot open it. The Fabric-only
 *       {@code ModScreensClientGameTest} still covers its half.</li>
 *   <li><b>That the item frame branches also change the frame.</b> The four sections below assert
 *       the message, which is what the mixin sends to the client; that the frame really ended up
 *       locked or invisible is a server side fact and is covered by the headless suite.</li>
 * </ul>
 */
public final class SmokeClientTest {

    /** The six furnace family blocks {@code ModModelProvider} drives through {@code createFurnace}. */
    private static final List<Block> FURNACES = List.of(
            ModBlocks.REINFORCED_FURNACE,
            ModBlocks.NETHERITE_FURNACE,
            ModBlocks.REINFORCED_SMOKER,
            ModBlocks.NETHERITE_SMOKER,
            ModBlocks.REINFORCED_BLAST_FURNACE,
            ModBlocks.NETHERITE_BLAST_FURNACE);

    /**
     * The z plane one block in front of the wall - the free air the working volume is built in.
     *
     * <p>Derived from {@link TestScene#WALL_Z} rather than written out, so a scene that ever moves
     * its wall takes this with it instead of leaving the pistons and the item frame buried in it.
     */
    private static final int FRONT_Z = TestScene.WALL_Z - 1;

    /** Free air one block in front of the wall, dead centre of the view - the item frame goes here. */
    private static final BlockPos FRAME_POS = new BlockPos(10, 1, FRONT_Z);

    private static final BlockPos MOD_PISTON = new BlockPos(11, 1, FRONT_Z);
    private static final BlockPos MOD_PISTON_TARGET = new BlockPos(12, 1, FRONT_Z);
    private static final BlockPos VANILLA_PISTON = new BlockPos(8, 1, FRONT_Z);
    private static final BlockPos VANILLA_PISTON_TARGET = new BlockPos(7, 1, FRONT_Z);

    /**
     * The GLFW key code the sneak steps press.
     *
     * <p>The shared {@link Harness} takes a raw GLFW code, where the Fabric-only ancestor handed
     * over the key <em>binding</em> ({@code getInput().holdShift()}) and let the framework resolve
     * it. A raw code is the only thing both loaders can serve, but it silently assumes the binding
     * still sits on this key - {@link #assertSneakKeyIsBound} is the control that closes that hole.
     */
    private static final int SNEAK_KEY = InputConstants.KEY_LSHIFT;

    /**
     * The text the action bar is primed with before every measured click.
     *
     * <p>See {@link #armTheActionBar}: it is both the "the channel works" control and the eraser
     * that makes a message read after the click provably a fresh one.
     *
     * <p><b>Underscores, not hyphens, and that is not a style choice.</b> The test runner reads the
     * expected checkpoints out of these sources with a regular expression over string literals -
     * any literal of the shape {@code lowercase-words-with-hyphens} in a file that takes
     * screenshots counts as a promised screenshot name (it has to be that loose, because the
     * multi block breaking tests hand their names to a helper). A hyphenated sentinel would
     * therefore promise a file that nobody ever writes, and the run would fail on a missing
     * screenshot that never existed.
     */
    private static final String ACTION_BAR_SENTINEL = "simplebuilding_clienttest_armed";

    private SmokeClientTest() {
    }

    /** What can be seen before any world exists: a client that booted with the mod on it. */
    public static void beforeWorld(Script script) {
        script.shot("smoke-main-menu");
    }

    /**
     * Everything that needs a joined world.
     *
     * <p>The order is the one the Fabric-only ancestor used, but nothing depends on it any more:
     * every section past the first rebuilds the scene before it looks at anything.
     */
    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");
        script.shot("smoke-in-world");

        modLanguageFileReachesTheClient(script);
        furnaceBlockStatesBakeIntoOrientedModels(script);
        survivalCountersArriveOnTheClientEverySecond(script);
        chiselAnswersTheClientSideUseOnItself(script);
        chiselAndSpatulaSoundsReachTheClient(script);
        breakerPistonSoundReachesTheClient(script);
        itemFrameOverlayMessagesReachTheClient(script);

        // What a finally block used to do. As steps these run only when everything before them
        // passed: a failing run leaves the sneak key held, the sound recorder armed and the HUD
        // hidden behind it. That is survivable - the next test's TestScene.build rebuilds the
        // world and the rendering options, and the recorder is re-armed before it is next read -
        // but it is the reason these are named steps rather than an afterthought.
        script.harness("release the sneak key", harness -> harness.releaseKey(SNEAK_KEY));
        script.verify("stop recording sounds", SoundRecorder::disarm);
        TestScene.showHudAgain(script);
    }

    // =================================================================================
    // 1. Language files
    // =================================================================================

    /**
     * The mod's language file is on the client and its entries resolve.
     *
     * <p>The mod's {@code en_us.json} is a client resource: the server never loads it, so no server
     * test can tell a packaged language file from a missing one. This asserts that the client's
     * language manager really resolves the names of the ore generation feature - the two ore
     * blocks, one block item and the two things they smelt into.
     *
     * <p>The control comes first and is the reason this test can say anything at all:
     * {@code I18n.get} echoes a key back when it has no entry for it, so "translated" and
     * "missing" are told apart only by that echo. If a future version returned an empty string or
     * a placeholder instead, every assertion below would pass for a client with no language file
     * at all - so the echo is asserted on a key that deliberately does not exist.
     *
     * <p>The list covers both prefixes the feature needs: {@code block.} for the two ores as they
     * sit in the world, {@code item.} for the things the player carries. Which prefix a block item
     * lands under is not obvious in 26.2, and it is exactly where the mod gets it wrong for three
     * blocks - see the known defect in the class javadoc for why {@code NIHILITH_ORE_ITEM} is
     * absent from this list while its astralit twin is in it.
     *
     * <p><b>Not covered:</b> that other languages fall back to English. That is vanilla -
     * {@code LanguageManager} always stacks the selected language on top of {@code en_us} - and
     * the mod contributes nothing to it.
     *
     * <p>What breaks this test: a language file renamed, moved out of
     * {@code assets/simplebuilding/lang/}, excluded from the jar by {@code processResources}, made
     * unparseable, or one of these five keys renamed on one side only.
     */
    private static void modLanguageFileReachesTheClient(Script script) {
        List<String> keys = List.of(
                ModBlocks.NIHILITH_ORE.getDescriptionId(),
                ModBlocks.ASTRALIT_ORE.getDescriptionId(),
                ModItems.ASTRALIT_ORE_ITEM.getDescriptionId(),
                ModItems.NIHILITH_SHARD.getDescriptionId(),
                ModItems.ASTRALIT_DUST.getDescriptionId());

        script.act("the client language resolves the mod's keys", client -> {
            String missingKey = "block.simplebuilding.a_block_that_does_not_exist";

            if (!missingKey.equals(I18n.get(missingKey))) {
                throw new AssertionError("control failed: I18n does not echo unknown keys any more "
                        + "(it answered \"" + I18n.get(missingKey) + "\"), so \"translated\" cannot "
                        + "be told from \"missing\" the way this test does it");
            }

            List<String> untranslated = new ArrayList<>();
            for (String key : keys) {
                String text = I18n.get(key);
                if (key.equals(text) || text.isBlank()) {
                    untranslated.add(key);
                }
            }

            if (!untranslated.isEmpty()) {
                throw new AssertionError("the client language has no entry for " + untranslated);
            }
        });
    }

    // =================================================================================
    // 2. Furnace blockstates and models
    // =================================================================================

    /**
     * {@code ModModelProvider} drives all six furnace family blocks through
     * {@code BlockStateModelGenerator#createFurnace}, which is supposed to produce the vanilla
     * furnace layout: an {@code off} and an {@code on} model, and four y rotations so the front
     * texture always sits on the side the block's {@code facing} property names. Blockstate and
     * model json are client resources, so the whole thing is invisible to a server test.
     *
     * <p>This walks all 6 x 4 x 2 = 48 states of the finished, baked client model set and asserts
     * for each one that
     * <ul>
     *   <li>the state has a model at all - {@code BlockStateModelSet#get} silently answers with
     *       {@code missingModel()} for a state the blockstate json does not mention;</li>
     *   <li>the quads on the {@code facing} side carry exactly the front sprite, named
     *       {@code <block>_front} when unlit and {@code <block>_front_on} when lit - this is the
     *       assertion that fails when a y rotation is wrong, because then the front lands on
     *       another side;</li>
     *   <li>the quads on the opposite side do <em>not</em> carry it, which rules out the
     *       degenerate model that puts the front texture on all six faces and would satisfy the
     *       check above by accident.</li>
     * </ul>
     * A missing texture also fails here rather than passing quietly: an unresolvable sprite bakes
     * as {@code minecraft:missingno}, which is not the expected name.
     *
     * <p>The whole walk is a single {@code act} step. It touches only the baked model set, which is
     * client state, and it is cheap enough to finish inside one tick - splitting it per block would
     * buy six step names and cost the single message that lists every wrong state at once.
     *
     * <p>The screenshot at the end is documentary: the six unlit fronts, side by side, one block
     * above the line of sight so the crosshair keeps its target. Nothing is asserted on it - see
     * the class javadoc for why an animated lit texture rules a pixel comparison out here.
     *
     * <p>What breaks this test: a dropped or renamed {@code createFurnace} call in the model
     * provider, generated blockstate or model json that was not regenerated after a rename, a
     * texture file that is missing from the atlas, or a change to
     * {@code TexturedModel.ORIENTABLE_ONLY_TOP} that stops putting {@code #front} on the north
     * face of {@code minecraft:block/orientable}.
     */
    private static void furnaceBlockStatesBakeIntoOrientedModels(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");

        script.act("all 48 furnace states bake into an oriented model", client -> {
            BlockModelShaper models = client.getModelManager().getBlockModelShaper();
            List<String> found = new ArrayList<>();

            for (Block block : FURNACES) {
                Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);

                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    for (boolean lit : new boolean[]{false, true}) {
                        BlockState state = block.defaultBlockState()
                                .setValue(AbstractFurnaceBlock.FACING, facing)
                                .setValue(AbstractFurnaceBlock.LIT, lit);

                        Identifier expectedFront = Identifier.fromNamespaceAndPath(
                                blockId.getNamespace(),
                                "block/" + blockId.getPath() + "_front" + (lit ? "_on" : ""));

                        BlockStateModel model = models.getBlockModel(state);

                        if (model == client.getModelManager().getMissingBlockStateModel()) {
                            found.add(state + " has no baked model (the client fell back to the "
                                    + "missing model, so the blockstate json does not cover it)");
                            continue;
                        }

                        Set<Identifier> onFront = spritesOn(model, facing);
                        Set<Identifier> onBack = spritesOn(model, facing.getOpposite());

                        if (!onFront.equals(Set.of(expectedFront))) {
                            found.add(state + " shows " + onFront + " on its " + facing
                                    + " side, expected exactly [" + expectedFront + "]");
                        }

                        if (onBack.contains(expectedFront)) {
                            found.add(state + " also shows the front sprite " + expectedFront
                                    + " on its " + facing.getOpposite() + " side, so the model puts "
                                    + "the front texture on more than one face and the check above "
                                    + "proves nothing about the rotation");
                        }
                    }
                }
            }

            if (!found.isEmpty()) {
                throw new AssertionError("Furnace blockstates or models are wrong on the client: " + found);
            }
        });

        for (int i = 0; i < FURNACES.size(); i++) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(FURNACES.get(i));
            script.command("setblock " + (8 + i) + " 2 " + FRONT_Z + " " + id + "[facing=north,lit=false]");
        }

        script.awaitPackets();
        script.idle("let the six furnaces reach the client", 20);
        script.shot("furnace-a-six-fronts");

        clearWorkingVolume(script);
    }

    /** Every sprite the baked model puts on one side of the block. */
    private static Set<Identifier> spritesOn(BlockStateModel model, Direction side) {
        List<BlockModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);

        Set<Identifier> sprites = new LinkedHashSet<>();

        for (BlockModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(side)) {
                sprites.add(quad.sprite().contents().name());
            }
        }

        return sprites;
    }

    // =================================================================================
    // 3. Survival counters over the network
    // =================================================================================

    /**
     * {@code SurvivalTracerMixin} sends a {@code SurvivalSyncPayload} to the player once every
     * twenty ticks; the client receiver writes the numbers into the {@code LocalPlayer} through
     * {@code ClientSurvivalTracerMixin}. A server test can at best count the sends - only a client
     * can say whether anything arrived, whether it kept arriving, and whether the numbers in it are
     * the server's.
     *
     * <p>All three are asserted over a sixty tick window:
     * <ul>
     *   <li>the client side play time counter moved at all, so at least one payload arrived;</li>
     *   <li>it moved by roughly sixty ticks, so payloads kept arriving - a single sync followed by
     *       silence lands far below the lower bound;</li>
     *   <li>it is within twenty five ticks of the server's own {@code PLAY_TIME} statistic, so the
     *       payload carries the real counter and not a constant. Twenty of those twenty five are
     *       the sync period itself: the client is always up to one second stale.</li>
     * </ul>
     *
     * <p>The third one is the reason {@link #askTheServer} exists. It is the only assertion in this
     * class that needs a number the client cannot know, and the Fabric-only ancestor used
     * {@code computeOnServer} for it - a call NeoForge does not have. Dropping it was not an
     * option: the first two assertions are both satisfied by a counter that ticks up locally, so
     * without the comparison a client side stub would pass.
     *
     * <p>The setup check comes first and is not decoration: if {@code ClientSurvivalTracerMixin}
     * were not applied at all, the reads below would throw a {@code ClassCastException} in the
     * middle of a measurement instead of naming the cause.
     *
     * <p>What breaks this test: the {@code tick} injection removed or its {@code % 20} condition
     * broken, the payload dropped by {@code canSendToPlayer}, the client receiver unregistered,
     * {@code ClientSurvivalTracerMixin} no longer applying to {@code LocalPlayer}, or the fields of
     * the payload getting swapped so play time reads some other counter.
     */
    private static void survivalCountersArriveOnTheClientEverySecond(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");

        script.check("the LocalPlayer does not implement SurvivalTracerAccessor, so "
                        + "ClientSurvivalTracerMixin was not applied and the survival sync cannot be "
                        + "observed at all",
                client -> client.player instanceof SurvivalTracerAccessor);

        Later<Integer> before = readClientPlayTime(script, "before the 60 tick window");
        script.idle("let the survival sync run for 60 ticks", 60);
        Later<Integer> after = readClientPlayTime(script, "after the 60 tick window");

        Later<Integer> onTheServer = askTheServer(script, "the server's PLAY_TIME statistic", server -> {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            return players.isEmpty() ? -1 : players.get(0).getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        });

        script.verify("the client's play time counter follows the server's", () -> {
            int server = onTheServer.get();

            if (server < 0) {
                throw new AssertionError("Survival sync cannot be observed: there is no server player");
            }

            int moved = after.get() - before.get();

            if (moved <= 0) {
                throw new AssertionError("No SurvivalSyncPayload arrived: the client side play time "
                        + "counter stayed at " + before.get() + " over 60 ticks while the server "
                        + "statistic is at " + server + ".");
            }

            if (moved < 40 || moved > 80) {
                throw new AssertionError("SurvivalSyncPayload does not arrive once a second: the client "
                        + "side play time counter moved by " + moved + " ticks over a 60 tick window ("
                        + before.get() + " -> " + after.get() + "), expected roughly 60 from three syncs.");
            }

            if (Math.abs(server - after.get()) > 25) {
                throw new AssertionError("SurvivalSyncPayload carries the wrong number: the client says "
                        + after.get() + " play time ticks, the server statistic says " + server
                        + ". At most 25 ticks of difference are explainable by the 20 tick sync period.");
            }

            TestLog.info("survival sync: client " + before.get() + " -> " + after.get()
                    + " over 60 ticks, server statistic " + server);
        });
    }

    private static Later<Integer> readClientPlayTime(Script script, String when) {
        Later<Integer> value = new Later<>("the client side play time counter " + when);

        script.act("read the client side play time counter " + when, client ->
                value.set(((SurvivalTracerAccessor) client.player).simplebuilding$getCurrentTime()));

        return value;
    }

    // =================================================================================
    // 4. The chisel's client side branch of useOn
    // =================================================================================

    /**
     * {@code ChiselItem#useOn} has a branch that only ever runs on the client: it answers
     * {@code SUCCESS} when the block under the crosshair is in one of the item's four
     * transformation tables and {@code PASS} otherwise, and it does that without touching the
     * world. That answer is what {@code Minecraft#startUseItem} turns into the arm swing the player
     * sees before the server has replied, so getting it wrong means either a swing that leads
     * nowhere or a chisel that visibly does nothing until the block changes.
     *
     * <p>The branch is unreachable from a server test ({@code level.isClientSide()} is false there),
     * so it is called here the same way the game calls it: {@code ItemStack#useOn} with a
     * {@code UseOnContext} built from the client player, the main hand and the client's own
     * {@code hitResult} - byte for byte what {@code MultiPlayerGameMode#performUseItemOn} does.
     *
     * <p>Both directions are covered, and the two blocks are checked against
     * {@code ChiselItem.FINAL_STONE_*} before they are used, so a table that changed shows up as a
     * setup failure instead of as a wrong answer. The table lookup reads static maps and needs no
     * game state, which is why it is a {@code verify} step and not an {@code act}.
     *
     * <p>What breaks this test: the client branch removed or moved behind the server branch, the
     * table lookup reduced to fewer than the four maps, sandstone dropped from the stone tier
     * chain, or the answers swapped.
     */
    private static void chiselAnswersTheClientSideUseOnItself(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");

        script.verify("the chisel tables still make this a two sided test", () -> {
            if (inAnyStoneTable(Blocks.GLASS)) {
                throw new AssertionError("Chisel table setup failed: glass is in one of the stone tier "
                        + "chisel tables, so it is no longer a valid \"nothing to do here\" case");
            }

            if (!inAnyStoneTable(Blocks.SANDSTONE)) {
                throw new AssertionError("Chisel table setup failed: sandstone is in none of the stone "
                        + "tier chisel tables, so the SUCCESS case below could not fire");
            }
        });

        chiselAnswersOnTheClient(script, "minecraft:sandstone", InteractionResult.SUCCESS,
                "which is in the stone tier table");
        chiselAnswersOnTheClient(script, "minecraft:glass", InteractionResult.PASS,
                "which is in none of the tables");

        // Put the wall back the way the next section expects it. The section rebuilds the scene
        // anyway; this is here so a reader of the log sees the world restored where it was changed.
        script.command("setblock " + at(TestScene.TARGET) + " minecraft:stone");
        script.awaitPackets();
        script.idle("let the restored target block reach the client", 10);
    }

    private static boolean inAnyStoneTable(Block block) {
        return ChiselItem.FINAL_STONE_FWD.containsKey(block)
                || ChiselItem.FINAL_STONE_BWD.containsKey(block)
                || ChiselItem.FINAL_STONE_TOUCH_FWD.containsKey(block)
                || ChiselItem.FINAL_STONE_TOUCH_BWD.containsKey(block);
    }

    /**
     * Puts {@code blockId} under the crosshair, holds a stone chisel and asserts what the client
     * side branch of {@code useOn} answers for it.
     *
     * <p>The two trigger conditions are asserted in the same step that calls {@code useOn}, right
     * before the call: a chisel that is not in the hand or that is on cooldown makes the method
     * return {@code PASS} before it ever looks at the block, and that would read as a wrong answer.
     */
    private static void chiselAnswersOnTheClient(Script script, String blockId,
                                                 InteractionResult expected, String why) {
        script.command("setblock " + at(TestScene.TARGET) + " " + blockId);
        script.command("item replace entity @a weapon.mainhand with simplebuilding:stone_chisel");
        script.awaitPackets();
        script.idle("let " + blockId + " and the chisel reach the client", 15);

        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.act("ChiselItem#useOn answers " + expected + " for " + blockId, client -> {
            if (!(client.player.getMainHandItem().getItem() instanceof ChiselItem)) {
                throw new AssertionError("Chisel trigger conditions not met: the main hand holds "
                        + client.player.getMainHandItem());
            }

            if (client.player.getCooldowns().isOnCooldown(client.player.getMainHandItem())) {
                throw new AssertionError("Chisel trigger conditions not met: the chisel is on cooldown, "
                        + "so useOn returns PASS before it ever looks at the block");
            }

            BlockHitResult hit = (BlockHitResult) client.hitResult;
            InteractionResult answer = client.player.getMainHandItem()
                    .useOn(new UseOnContext(client.player, InteractionHand.MAIN_HAND, hit));

            if (answer != expected) {
                throw new AssertionError("ChiselItem#useOn answered " + answer + " on the client for "
                        + blockId + ", " + why + "; expected " + expected + ". "
                        + TestScene.describeAim(client));
            }
        });
    }

    // =================================================================================
    // 5. Chisel and spatula sounds
    // =================================================================================

    /**
     * A spatula is a {@code ChiselItem} with three things changed at registration time, one of
     * which is {@code setChiselSound(SoundEvents.SAND_FALL)}. The sound itself is played server
     * side with {@code Level#playSound(null, ...)}, so unlike the item frame sounds it does reach
     * the acting player - but only a client can hear it, and nothing in the mod reads the field
     * back.
     *
     * <p>Sounds are observed through vanilla's own {@code SoundManager#addListener} hook, the same
     * one the subtitle overlay uses. That hook is plain vanilla and identical on both loaders,
     * which is why {@link SoundRecorder} registers itself from a normal step instead of being
     * wired up by each driver the way {@link BreakingStateRecorder} has to be.
     * {@code SoundEngine#play} notifies listeners before it looks at the volume, so a muted client
     * still reports every sound. The reading is not the packet though - it is the packet times the
     * factor of the {@code sounds.json} variant that was picked, and this sound is precisely where
     * that matters: two of the four {@code ui.stonecutter.take_result} variants declare
     * {@code "pitch": 0.92}. {@link SoundRecorder} divides the factor back out; its javadoc has the
     * details.
     *
     * <p>Three steps, in order:
     * <ol>
     *   <li>a self check with {@code /playsound}, because a client whose sound engine failed to
     *       load reports nothing at all and would make every assertion below pass or fail for the
     *       wrong reason;</li>
     *   <li>a stone chisel on sandstone has to produce {@code UI_STONECUTTER_TAKE_RESULT} - the
     *       class default;</li>
     *   <li>a stone spatula on sandstone has to produce {@code SAND_FALL} <em>and not</em> the
     *       stonecutter sound, which is what makes this a statement about the override rather than
     *       about "some sound was played".</li>
     * </ol>
     * Each step also asserts that the block really changed, so a missing sound can never be blamed
     * on a chisel that did not fire.
     *
     * <p>Volume is asserted exactly (0.5); pitch only against the 0.8..1.2 band the item randomises
     * it into. Both are the numbers the mod handed to {@code Level#playSound}, recovered from the
     * reading as described above.
     *
     * <p>What breaks this test: {@code setChiselSound} no longer being called for spatulas, the
     * playback moving to {@code player.playSound} (which would skip the acting player, see the
     * class javadoc), the volume changing, or the spatula registration losing its dedicated flag so
     * it stops being a spatula at all.
     */
    private static void chiselAndSpatulaSoundsReachTheClient(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");

        Identifier stonecutter = SoundEvents.UI_STONECUTTER_TAKE_RESULT.location();
        Identifier sandFall = SoundEvents.SAND_FALL.location();

        assertTheClientCanHearAnything(script);

        Later<List<SoundRecorder.Heard>> withChisel =
                chiselOnceAndListen(script, "simplebuilding:stone_chisel");

        script.verify("the stone chisel played its default sound", () -> {
            SoundRecorder.Heard chiselSound = firstOf(withChisel.get(), stonecutter);

            if (chiselSound == null) {
                throw new AssertionError("The chisel played no sound the client could hear: heard "
                        + withChisel.get() + ", expected " + stonecutter + ".");
            }

            assertVolumeAndPitch(chiselSound, "stone chisel");
        });

        Later<List<SoundRecorder.Heard>> withSpatula =
                chiselOnceAndListen(script, "simplebuilding:stone_spatula");

        script.verify("the stone spatula replaced the default sound", () -> {
            SoundRecorder.Heard spatulaSound = firstOf(withSpatula.get(), sandFall);

            if (spatulaSound == null) {
                throw new AssertionError("The spatula did not use SAND_FALL as its chisel sound: heard "
                        + withSpatula.get() + ", expected " + sandFall + ".");
            }

            if (firstOf(withSpatula.get(), stonecutter) != null) {
                throw new AssertionError("The spatula played the plain chisel sound " + stonecutter
                        + " as well as " + sandFall + " (heard " + withSpatula.get() + "), so "
                        + "setChiselSound is not actually replacing the default.");
            }

            assertVolumeAndPitch(spatulaSound, "stone spatula");
        });

        script.command("setblock " + at(TestScene.TARGET) + " minecraft:stone");
        script.command("clear @a", true);
        script.awaitPackets();
        script.idle("let the restored target block reach the client", 10);
    }

    private static void assertVolumeAndPitch(SoundRecorder.Heard heard, String label) {
        assertFactorsAreUsable(heard, label);

        if (Math.abs(heard.sentVolume() - 0.5f) > 0.001f) {
            throw new AssertionError("The " + label + " sound was played at volume " + heard.sentVolume()
                    + ", expected 0.5 (" + heard + ").");
        }

        if (heard.sentPitch() < 0.8f - 0.001f || heard.sentPitch() > 1.2f + 0.001f) {
            throw new AssertionError("The " + label + " sound was played at pitch " + heard.sentPitch()
                    + ", which is outside the 0.8..1.2 band ChiselItem randomises into (" + heard + ").");
        }
    }

    /**
     * Guards the division {@link SoundRecorder.Heard#sentVolume()} and
     * {@link SoundRecorder.Heard#sentPitch()} do - see the recorder's javadoc for why it is needed.
     * Both cases below are statements about vanilla's {@code sounds.json}, not about the mod, so
     * they are setup failures with their own wording rather than a wrong number blamed on the mod.
     */
    private static void assertFactorsAreUsable(SoundRecorder.Heard heard, String label) {
        if (!heard.constantFactors()) {
            throw new AssertionError("The " + label + " sound cannot be measured through the sound "
                    + "listener any more: the sounds.json entry for " + heard.sound() + " now varies "
                    + "its own volume or pitch per play, so the factor divided out of the reading is "
                    + "a different random draw than the one the sound engine used (" + heard + "). "
                    + "Read the values off the packet instead.");
        }

        if (heard.volumeFactor() <= 0.0f || heard.pitchFactor() <= 0.0f) {
            throw new AssertionError("The " + label + " sound has a zero or negative sounds.json "
                    + "factor (" + heard + "), so the values the mod passed to playSound cannot be "
                    + "recovered from the reading.");
        }
    }

    /**
     * Puts sandstone under the crosshair, right clicks it once with {@code itemId} and hands back
     * everything the client heard while the block changed.
     *
     * <p>The wait is on the block having changed, not on a fixed number of ticks: that is what
     * makes "no sound" mean "no sound" rather than "the chisel had not fired yet". The five idle
     * ticks after it give the sound packet time to catch up with the block update, which arrives
     * over a different path.
     */
    private static Later<List<SoundRecorder.Heard>> chiselOnceAndListen(Script script, String itemId) {
        Later<List<SoundRecorder.Heard>> heard = new Later<>("the sounds heard while using " + itemId);

        script.command("setblock " + at(TestScene.TARGET) + " minecraft:sandstone");
        script.command("item replace entity @a weapon.mainhand with " + itemId);
        script.awaitPackets();
        // Defensive, exactly as in the straight-line version: a sneaking player uses a different
        // interaction path, and this section must not depend on what an earlier one left held.
        script.harness("release the sneak key before using " + itemId,
                harness -> harness.releaseKey(SNEAK_KEY));
        script.idle("let the sandstone and " + itemId + " reach the client", 15);

        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.verify("start recording sounds for " + itemId, SoundRecorder::arm);
        script.harness("right click the sandstone with " + itemId, harness -> harness.pressMouse(1));

        script.await("the sandstone under the crosshair changed with " + itemId, 60,
                client -> !client.level.getBlockState(TestScene.TARGET).is(Blocks.SANDSTONE),
                client -> "the sandstone under the crosshair never changed while right clicking with "
                        + itemId + ", so the chisel never ran and no sound could be attributed to it. "
                        + TestScene.describeAim(client));

        script.idle("let the sound packet catch up with the block update", 5);

        script.verify("stop recording sounds for " + itemId, () -> {
            heard.set(SoundRecorder.heard());
            SoundRecorder.disarm();
        });

        return heard;
    }

    /**
     * A client whose sound engine did not start reports no sounds at all
     * ({@code SoundEngine#play} returns before it notifies listeners). This makes that a setup
     * failure with its own message instead of a mod defect somewhere further down.
     */
    private static void assertTheClientCanHearAnything(Script script) {
        Identifier bell = SoundEvents.NOTE_BLOCK_BELL.value().location();

        script.act("install the sound listener", SoundRecorder::install);
        script.verify("start recording sounds for the hearing self check", SoundRecorder::arm);

        script.command("playsound " + bell + " block @a 10.5 1.0 17.0 1 1");
        script.awaitPackets();

        script.await("the client can observe sounds at all", 40,
                client -> firstOf(SoundRecorder.heard(), bell) != null,
                client -> "a /playsound of " + bell + " never reached the SoundManager listener. Either "
                        + "the sound engine failed to start (no audio device) or SoundManager#addListener "
                        + "no longer fires - in both cases the sound assertions below would be "
                        + "meaningless, so they are not attempted. Heard instead: " + SoundRecorder.heard());

        script.verify("stop recording sounds after the hearing self check", SoundRecorder::disarm);
    }

    private static SoundRecorder.Heard firstOf(List<SoundRecorder.Heard> heard, Identifier sound) {
        for (SoundRecorder.Heard candidate : heard) {
            if (candidate.sound().equals(sound)) {
                return candidate;
            }
        }

        return null;
    }

    // =================================================================================
    // 6. The breaker piston's sound
    // =================================================================================

    /**
     * {@code NetheriteBreakerPistonBlock#triggerEvent} destroys the block in front of the piston
     * and marks that with {@code ZOMBIE_ATTACK_IRON_DOOR} at volume 0.5 and pitch 0.8. The break
     * itself is already covered by the server suite; the sound is not observable there at all.
     *
     * <p>It is played with {@code Level#playSound(null, ...)}, so the packet goes to every player in
     * range including this one - unlike the item frame sounds (see the class javadoc). Unlike the
     * chisel, nothing here is random: the mod passes fixed numbers and
     * {@code entity.zombie.attack_iron_door} declares no volume or pitch of its own in
     * {@code sounds.json}, so both can be asserted exactly.
     *
     * <p>A vanilla piston with the identical wiring is fired first and must <em>not</em> produce the
     * sound. That control rules out the alternative explanation "something in vanilla plays this
     * when a piston extends", and it fires the same code path, so it also proves that the setup
     * itself works: the vanilla piston pushes its target instead of breaking it, which is asserted
     * too. In the shared form the control keeps its place at the front for a second reason - it is
     * the only step that would notice a harness whose {@code setblock} never reached the server.
     *
     * <p>What breaks this test: the sound removed or its event, volume or pitch changed; the
     * playback moved inside the {@code isClientSide} branch or given the acting player as its
     * "except" argument; the redstone strength check refusing a block it used to break.
     */
    private static void breakerPistonSoundReachesTheClient(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");

        Identifier breakSound = SoundEvents.ZOMBIE_ATTACK_IRON_DOOR.location();

        script.act("install the sound listener", SoundRecorder::install);

        Later<List<SoundRecorder.Heard>> vanilla = firePiston(script, "the vanilla piston",
                "minecraft:piston", "west", VANILLA_PISTON, VANILLA_PISTON_TARGET);

        script.verify("the vanilla piston played no breaking sound", () -> {
            if (firstOf(vanilla.get(), breakSound) != null) {
                throw new AssertionError("Control failed: a plain vanilla piston already played "
                        + breakSound + " (heard " + vanilla.get() + "), so the measurement below would "
                        + "not be attributable to NetheriteBreakerPistonBlock.");
            }
        });

        script.act("the vanilla piston pushed its target instead of breaking it", client -> {
            if (!client.level.getBlockState(VANILLA_PISTON_TARGET.west()).is(Blocks.STONE)) {
                throw new AssertionError("Control failed: the vanilla piston did not push its target "
                        + "block to " + VANILLA_PISTON_TARGET.west() + ", so the wiring in this step "
                        + "never fired and \"no sound\" says nothing.");
            }
        });

        clearWorkingVolume(script);

        Later<List<SoundRecorder.Heard>> mod = firePiston(script, "the netherite breaker piston",
                "simplebuilding:netherite_piston", "east", MOD_PISTON, MOD_PISTON_TARGET);

        script.act("the netherite breaker piston broke the block in front of it", client -> {
            // "Broke it" and not merely "extended past it": a piston that only pushed would have
            // left the stone one block further out, where the extended head is not.
            boolean stoneSurvived = client.level.getBlockState(MOD_PISTON_TARGET).is(Blocks.STONE)
                    || client.level.getBlockState(MOD_PISTON_TARGET.east()).is(Blocks.STONE);

            if (stoneSurvived) {
                throw new AssertionError("The netherite breaker piston did not break the block in front "
                        + "of it (the stone is still at " + MOD_PISTON_TARGET + " or was pushed to "
                        + MOD_PISTON_TARGET.east() + "), so the sound branch was never reached.");
            }
        });

        script.verify("the client heard the breaker piston", () -> {
            SoundRecorder.Heard heard = firstOf(mod.get(), breakSound);

            if (heard == null) {
                throw new AssertionError("The netherite breaker piston broke its target but the client "
                        + "heard no " + breakSound + " (heard " + mod.get() + ").");
            }

            assertFactorsAreUsable(heard, "breaker piston");

            if (Math.abs(heard.sentVolume() - 0.5f) > 0.001f
                    || Math.abs(heard.sentPitch() - 0.8f) > 0.001f) {
                throw new AssertionError("The breaker piston sound was played as " + heard
                        + ", expected volume 0.5 and pitch 0.8.");
            }
        });

        script.shot("sound-a-breaker-piston");
        clearWorkingVolume(script);
    }

    /** Builds piston, target and power source, powers it and hands back what the client heard. */
    private static Later<List<SoundRecorder.Heard>> firePiston(Script script, String label,
                                                               String pistonId, String facing,
                                                               BlockPos piston, BlockPos target) {
        Later<List<SoundRecorder.Heard>> heard = new Later<>("the sounds heard while firing " + label);

        script.command("setblock " + at(target) + " minecraft:stone");
        script.command("setblock " + at(piston) + " " + pistonId + "[facing=" + facing + ",extended=false]");
        script.awaitPackets();
        script.idle("let " + label + " reach the client", 10);

        // Armed before the redstone block is placed, so the whole extension is inside the window.
        script.verify("start recording sounds for " + label, SoundRecorder::arm);

        script.command("setblock " + at(piston.above()) + " minecraft:redstone_block");
        script.awaitPackets();
        script.idle("let " + label + " extend and its sound arrive", 25);

        script.verify("stop recording sounds for " + label, () -> {
            heard.set(SoundRecorder.heard());
            SoundRecorder.disarm();
        });

        return heard;
    }

    // =================================================================================
    // 7. Item frame overlay messages
    // =================================================================================

    /**
     * {@code ItemFrameEntityMixin} answers four sneak interactions with an action bar message. The
     * messages are sent as system messages with the overlay flag set, so the server can only be
     * asked whether it tried; whether one arrived, and with which text, is a client fact.
     *
     * <p>All four branches are walked in the one order that reaches them through the real input
     * path, with a single item swap:
     * <ol>
     *   <li>sneak + glass pane on a filled, unlocked frame - locks it,</li>
     *   <li>sneak + the same glass pane again - branch one is now skipped because the frame is
     *       locked, so branch two unlocks it,</li>
     *   <li>sneak + shears on the visible frame - makes it invisible,</li>
     *   <li>sneak + the same shears again - branch three is now skipped because the frame is
     *       invisible, so branch four makes it visible.</li>
     * </ol>
     * The frame is placed and filled through real right clicks as well, and every step asserts the
     * state it depends on (frame present, item inside, crosshair on the frame, the <em>server</em>
     * seeing the player sneak) before the click, so a message that does not turn up cannot be
     * blamed on a click that never landed.
     *
     * <p><b>How the message is read, and why it is read this way.</b> The Fabric-only ancestor
     * registered {@code ClientReceiveMessageEvents.GAME}; that is a Fabric event and NeoForge has
     * no equivalent that a shared body could call. What both loaders do have is the place the
     * message ends up: {@code ChatListener#handleOverlay} calls {@code Gui.hud.setOverlayMessage},
     * which stores the component in {@code Gui.overlayMessageString} - and it does so from the
     * packet handler, not from the renderer, so a hidden HUD (which {@link TestScene} leaves
     * behind) still records it. That field is private and has no getter, so
     * {@link #currentActionBarMessage} reads it reflectively. Reflection can fail for reasons that
     * have nothing to do with the mod, which is why it is never the first thing that runs: see
     * {@link #armTheActionBar}.
     *
     * <p>The sounds that go with these four messages are <em>not</em> asserted - see the known
     * defect in the class javadoc.
     *
     * <p><b>Not covered:</b> that the frame really ends up locked or invisible. That is server
     * state and the headless suite has it; what is under test here is the message reaching the
     * client.
     *
     * <p>What breaks this test: any of the four branches losing its {@code sendOverlayMessage}, the
     * branch order changing so a different message answers the same click, the message text
     * changing, or {@code interact} no longer being injected at all - the last one shows up as the
     * lock step failing first.
     */
    private static void itemFrameOverlayMessagesReachTheClient(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");

        assertSneakKeyIsBound(script);
        placeAndFillItemFrame(script);

        script.harness("hold the sneak key", harness -> harness.holdKey(SNEAK_KEY));
        script.idle("let the sneak state reach the server and come back", 10);
        // Asserted here as well as before every single click. The per-click checks would catch a
        // sneak that never arrived too, but only after the first message has already failed to
        // turn up - and "the message did not arrive" is exactly the wrong first suspicion.
        assertTheServerSeesThePlayerSneaking(script, "after the sneak key was pressed");

        equip(script, "minecraft:glass_pane");
        expectOverlayMessage(script, "Item Frame gesperrt (Locked).", "lock with a glass pane", null);
        expectOverlayMessage(script, "Item Frame entsperrt.", "unlock by sneaking again", null);

        equip(script, "minecraft:shears");
        expectOverlayMessage(script, "Item Frame unsichtbar gemacht.", "hide with shears",
                "itemframe-a-invisible");
        expectOverlayMessage(script, "Item Frame sichtbar gemacht.", "show by sneaking again",
                "itemframe-b-visible-again");

        // The straight-line version put these in a finally block. As steps they run only if
        // everything above passed; a failed run leaves the sneak key held and a frame on the wall,
        // and the next section's TestScene.build is what clears both.
        script.harness("release the sneak key", harness -> harness.releaseKey(SNEAK_KEY));
        script.idle("let the sneak state settle", 5);
        script.command("kill @e[type=minecraft:item_frame]", true);
        script.command("clear @a", true);
        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");
        script.awaitPackets();
        script.idle("let the player arrive back at the scene's standpoint", 15);
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);
    }

    /**
     * Right clicks an item frame onto the wall and puts a stone into it, both through real input.
     *
     * <p>Real clicks and not {@code /summon}: the branches under test are reached through
     * {@code interact}, and a frame that was placed by a command would prove nothing about the
     * click path this test then uses.
     */
    private static void placeAndFillItemFrame(Script script) {
        script.harness("release the sneak key before placing the frame",
                harness -> harness.releaseKey(SNEAK_KEY));

        equip(script, "minecraft:item_frame");
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.harness("right click the wall with the item frame", harness -> harness.pressMouse(1));
        script.idle("let the item frame be placed", 15);

        Later<FrameState> placed = askTheServer(script, "the item frame at " + FRAME_POS,
                SmokeClientTest::frameState);

        script.act("the item frame was placed", client -> {
            if (!placed.get().present()) {
                throw new AssertionError("Setup failed: right clicking the wall with an item frame "
                        + "placed nothing at " + FRAME_POS + ". " + TestScene.describeAim(client));
            }
        });

        // Step one block closer. Blocks and entities have separate reaches:
        // Attributes.BLOCK_INTERACTION_RANGE is 4.5 and covers the wall from the scene's default
        // standpoint, but ENTITY_INTERACTION_RANGE is 3.0, and LocalPlayer#raycastHitResult turns
        // an entity hit beyond it into a MISS. At the default 3.44 blocks the crosshair would
        // therefore never report the frame, no matter how exactly it is aimed.
        script.command("tp @a 10.5 0.0 17.6 0.0 0.0");
        script.awaitPackets();
        script.idle("let the player arrive within entity reach of the frame", 15);

        equip(script, "minecraft:stone");
        assertCrosshairIsOnTheItemFrame(script);

        script.harness("right click the item frame with a stone", harness -> harness.pressMouse(1));
        script.idle("let the stone go into the frame", 15);

        Later<FrameState> filled = askTheServer(script, "the filled item frame at " + FRAME_POS,
                SmokeClientTest::frameState);

        script.verify("the item frame holds an item", () -> {
            if (!filled.get().present() || filled.get().empty()) {
                throw new AssertionError("Setup failed: the item frame is still empty after right "
                        + "clicking it with a stone (" + filled.get() + "). Every branch under test "
                        + "needs a filled frame.");
            }
        });
    }

    /**
     * One branch: prime the action bar, click, and wait for the mod's message to replace the
     * sentinel.
     *
     * @param screenshot a documentary screenshot taken once the message arrived, or null
     */
    private static void expectOverlayMessage(Script script, String expected, String step,
                                             String screenshot) {
        assertCrosshairIsOnTheItemFrame(script);
        assertTheServerSeesThePlayerSneaking(script, "before \"" + step + "\"");

        armTheActionBar(script, step);

        script.harness("right click the item frame to " + step, harness -> harness.pressMouse(1));

        script.await("the item frame step \"" + step + "\" reaches the client", 40,
                client -> expected.equals(currentActionBarMessage(client)),
                client -> "the item frame step \"" + step + "\" sent no action bar message \""
                        + expected + "\" to the client. The action bar currently reads \""
                        + currentActionBarMessage(client) + "\" (it was primed with \""
                        + ACTION_BAR_SENTINEL + "\" right before the click, so anything but the "
                        + "expected text here is what the mod actually sent, and the sentinel itself "
                        + "means nothing arrived at all). " + TestScene.describeAim(client));

        script.idle("let the message settle before the next step", 6);

        if (screenshot != null) {
            script.shot(screenshot);
        }
    }

    /**
     * Puts a known text on the action bar and waits until the client reports reading it back.
     *
     * <p>Two jobs in one step, and both are load bearing:
     * <ul>
     *   <li><b>It is the control for the reading itself.</b> {@link #currentActionBarMessage} gets
     *       at a private vanilla field; if that field is renamed, or the loader forbids the access,
     *       the failure has nothing to do with the mod. Sending a message the test controls and
     *       requiring it to come back makes that a loud, self-describing failure <em>before</em>
     *       any mod claim is measured - the same role {@code /playsound} plays for the sounds and
     *       the unknown-key echo plays for the language file.</li>
     *   <li><b>It erases the previous message.</b> An action bar message stays for 60 ticks, which
     *       is longer than a step takes. Without the sentinel, "the field reads what I expect"
     *       could in principle be satisfied by a message from an earlier step. The four texts are
     *       all different, so that could not actually happen here today - but it would be a silent
     *       false green the day two branches share a wording, and the sentinel costs one command.</li>
     * </ul>
     * {@code /title actionbar} reaches the same field through
     * {@code ClientPacketListener#setActionBarText}, so what is proven is exactly the path the mod
     * message travels.
     */
    private static void armTheActionBar(Script script, String step) {
        script.command("title @a actionbar {\"text\":\"" + ACTION_BAR_SENTINEL + "\"}");
        script.awaitPackets();

        script.await("the action bar is readable and primed for \"" + step + "\"", 60,
                client -> ACTION_BAR_SENTINEL.equals(currentActionBarMessage(client)),
                client -> "a /title actionbar of \"" + ACTION_BAR_SENTINEL + "\" never turned up in "
                        + "Gui.overlayMessageString (it reads \"" + currentActionBarMessage(client)
                        + "\"). Either the message never arrived or this client's action bar cannot "
                        + "be read the way this test reads it - in both cases the four item frame "
                        + "assertions below would be meaningless, so they are not attempted.");
    }

    /**
     * What the client's action bar currently says, or the empty string.
     *
     * <p>Reflective on purpose and with no fallback: {@code Gui.overlayMessageString} is private,
     * has no getter, and is the only place the message exists on the client. The alternatives were
     * weighed and rejected - a loader event is not shared code (that is what this port removes), a
     * pixel comparison of a HUD that has to be shown again would measure the camera and the swung
     * hand as much as the text, and an access widener would have to be written twice.
     *
     * <p>A failure here is never silent: it throws, and {@link #armTheActionBar} makes sure it
     * throws before any mod claim has been measured.
     */
    private static String currentActionBarMessage(Minecraft client) {
        try {
            Field field = actionBarField;

            if (field == null) {
                field = Gui.class.getDeclaredField("overlayMessageString");
                field.setAccessible(true);
                actionBarField = field;
            }

            Component message = (Component) field.get(client.gui);
            return message == null ? "" : message.getString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new AssertionError("The action bar message cannot be read on this client: "
                    + e + ". Gui.overlayMessageString is where ChatListener#handleOverlay stores an "
                    + "overlay system message; if it was renamed or this loader refuses the access, "
                    + "the item frame messages need a recorder that each driver registers - the way "
                    + "BreakingStateRecorder is wired up - instead of this read.", e);
        }
    }

    private static volatile Field actionBarField;

    private static void assertCrosshairIsOnTheItemFrame(Script script) {
        script.await("the crosshair is on the item frame", 40,
                client -> client.hitResult instanceof EntityHitResult hit
                        && hit.getEntity() instanceof ItemFrame,
                client -> "the crosshair is not on the item frame, so the right click below would go "
                        + "to the wall instead. " + TestScene.describeAim(client));
    }

    /**
     * The <em>server</em> has to see the sneak, not the client.
     *
     * <p>Every branch of {@code ItemFrameEntityMixin} is behind {@code player.isShiftKeyDown()} on
     * the server side, and the client's own flag is set a packet earlier. Asking the client would
     * therefore answer yes at a moment when the branches are still being skipped, and "no message"
     * would say nothing.
     */
    private static void assertTheServerSeesThePlayerSneaking(Script script, String when) {
        Later<Boolean> sneaking = askTheServer(script, "whether the player sneaks " + when, server -> {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            return !players.isEmpty() && players.get(0).isShiftKeyDown();
        });

        script.verify("the server sees the player sneaking " + when, () -> {
            if (!sneaking.get()) {
                throw new AssertionError("The server does not see the player sneaking " + when + ", so "
                        + "every sneak branch of ItemFrameEntityMixin is skipped and \"no message\" "
                        + "would say nothing.");
            }
        });
    }

    /**
     * The sneak binding really is the key {@link #SNEAK_KEY} presses.
     *
     * <p>New in the shared form and not optional: the harness presses a raw GLFW code, so a sneak
     * key that moved would leave the player standing while the test believed it sneaks - and every
     * item frame branch would then correctly send nothing and be reported as a broken mixin.
     */
    private static void assertSneakKeyIsBound(Script script) {
        script.act("the sneak binding sits on the key the harness presses", client -> {
            if (!client.options.keyShift.saveString().equals(InputConstants.Type.KEYSYM.getOrCreate(SNEAK_KEY).getName())) {
                throw new AssertionError("The sneak binding is not on GLFW key " + SNEAK_KEY
                        + " any more (it says \"" + client.options.keyShift.saveString() + "\"), so "
                        + "holding that key would not make the player sneak and none of the item "
                        + "frame branches could fire.");
            }
        });
    }

    /** What the server thinks of the item frame at {@link #FRAME_POS}. */
    private static FrameState frameState(MinecraftServer server) {
        ServerLevel level = server.overworld();
        AABB around = new AABB(FRAME_POS.getX(), FRAME_POS.getY(), FRAME_POS.getZ(),
                FRAME_POS.getX() + 1.0, FRAME_POS.getY() + 1.0, FRAME_POS.getZ() + 1.0).inflate(1.0);
        List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, around);

        if (frames.isEmpty()) {
            return new FrameState(false, true, false);
        }

        ItemFrame frame = frames.get(0);
        return new FrameState(true, frame.getItem().isEmpty(), frame.isInvisible());
    }

    /**
     * The server's view of the frame. A record with a {@code present} flag rather than a nullable
     * answer, because {@link Later} treats null as "not filled yet" and a missing frame is a
     * perfectly good answer that has to survive the trip.
     */
    private record FrameState(boolean present, boolean empty, boolean invisible) {
    }

    // =================================================================================
    // Shared helpers
    // =================================================================================

    private static void equip(Script script, String itemId) {
        script.command("item replace entity @a weapon.mainhand with " + itemId);
        script.awaitPackets();
        script.idle("let " + itemId + " reach the client's main hand", 12);
    }

    /** Empties the strip of air in front of the wall and puts the crosshair back on the target. */
    private static void clearWorkingVolume(Script script) {
        // Tolerant: after the first clearing the volume is often already empty, and vanilla reports
        // "no blocks were filled" as a command failure. Marked here one command at a time rather
        // than by switching error reporting off - the same rule TestScene follows, for the same
        // reason: a swallowed brigadier error once left eight game rules in this suite silently dead.
        script.command("fill 5 0 17 15 3 " + FRONT_Z + " minecraft:air", true);
        script.awaitPackets();
        script.idle("let the cleared working volume reach the client", 15);
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);
    }

    private static String at(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    /**
     * Asks the integrated server a question and hands the answer to a later step.
     *
     * <p><b>Why not a harness method.</b> Fabric has {@code computeOnServer} and NeoForge has
     * nothing like it, so a harness method would have to be invented for one loader. This route
     * exists on both without any loader code at all: {@code Minecraft.getSingleplayerServer()} is
     * vanilla, {@code MinecraftServer.submit} is vanilla, and the future it returns is polled once
     * per client tick by an {@code await} step instead of being waited on. Nothing blocks, which is
     * what makes it usable on NeoForge - a client test there runs inside the client tick, and
     * blocking in it stops the very loop that would make the waiting end.
     *
     * <p>The question runs on the server thread, not on the client thread with a stolen reference:
     * reading a player's statistics or a level's entity list from another thread is a data race
     * that shows up as a rare, unexplainable failure rather than as a wrong answer.
     *
     * <p>The answer must not be null - {@link Later} cannot tell a null answer from an unfilled
     * one, so a question that has "nothing there" as a legitimate outcome has to encode it (see
     * {@link FrameState}).
     */
    private static <T> Later<T> askTheServer(Script script, String what,
                                             Function<MinecraftServer, T> question) {
        Later<T> answer = new Later<>(what);
        List<CompletableFuture<T>> pending = new ArrayList<>();

        script.act("ask the integrated server for " + what, client -> {
            MinecraftServer server = client.getSingleplayerServer();

            if (server == null) {
                throw new AssertionError("There is no integrated server, so " + what + " cannot be "
                        + "read. This test only runs in the single player world the driver creates "
                        + "for ClientTests.inWorld().");
            }

            Supplier<T> ask = () -> question.apply(server);
            pending.clear();
            pending.add(server.submit(ask));
        });

        script.await("the server answered with " + what, 200, client -> {
            CompletableFuture<T> future = pending.get(0);

            if (!future.isDone()) {
                return false;
            }

            // join() rethrows whatever the question threw on the server thread, with the cause
            // attached - the alternative is a step that quietly answers null.
            T value = future.join();

            if (value == null) {
                throw new AssertionError("The server answered null for " + what + ", which cannot be "
                        + "carried to a later step - encode \"nothing there\" in the answer instead.");
            }

            answer.set(value);
            return true;
        }, client -> "the integrated server never ran the question about " + what + " within 200 "
                + "client ticks - its task queue is not being drained.");

        return answer;
    }

    // =================================================================================
    // Observers
    // =================================================================================

    /**
     * Records every sound the client's sound engine starts while armed, and undoes the one
     * distortion between the packet and the observation point.
     *
     * <p>{@code SoundManager#addListener} is vanilla's own hook - the subtitle overlay uses it -
     * and {@code SoundEngine#play} notifies listeners before it does anything with the client's
     * volume sliders, so a muted client reports every sound. Because the hook is vanilla, this
     * recorder can register itself from a step; {@link BreakingStateRecorder}, whose event exists
     * only per loader, cannot and is wired up by each driver instead.
     *
     * <p><b>What the listener does not hand over is the packet.</b>
     * {@code AbstractSoundInstance#getVolume()} and {@code #getPitch()} return
     * {@code this.volume * sound.getVolume().sample(random)} and
     * {@code this.pitch * sound.getPitch().sample(random)}: the value from the packet, multiplied
     * by the factor the chosen {@code sounds.json} variant carries. That factor is usually 1, and
     * for one of the sounds under test it is not - {@code ui.stonecutter.take_result} has four
     * variants and two of them declare {@code "pitch": 0.92}, so half of all plays arrive 8 percent
     * flat and a naive band assertion on the raw reading fails at random.
     *
     * <p>So the factor is read off the <em>resolved</em> variant ({@code SoundInstance#getSound()},
     * which {@code SoundEngine#play} has already picked before it notifies us) and divided back
     * out. That is exact as long as the variant's factor is a constant, which is why it is sampled
     * with three unrelated random sources and compared: a variant that starts randomising its own
     * pitch would make the division a different draw than the one the engine used, and
     * {@link SmokeClientTest#assertFactorsAreUsable} turns that into a loud setup failure instead
     * of a silent wrong number.
     *
     * <p><b>Arming exists because the listener outlives the measurement.</b> It is registered once
     * for the whole run and fires on every sound, including those of the tests before and after.
     * Only the window between {@link #arm()} and {@link #disarm()} is recorded.
     */
    private static final class SoundRecorder {

        /**
         * @param volume          raw reading, packet value times the variant's own volume factor
         * @param pitch           raw reading, packet value times the variant's own pitch factor
         * @param constantFactors whether those two factors are constants, so dividing them out is
         *                        exact rather than a second, unrelated random draw
         */
        record Heard(Identifier sound, float volume, float pitch,
                     float volumeFactor, float pitchFactor, boolean constantFactors) {

            /** The volume the mod passed to {@code playSound}. */
            float sentVolume() {
                return volume / volumeFactor;
            }

            /** The pitch the mod passed to {@code playSound}. */
            float sentPitch() {
                return pitch / pitchFactor;
            }

            @Override
            public String toString() {
                String raw = volumeFactor == 1.0f && pitchFactor == 1.0f
                        ? ""
                        : " [raw " + volume + "/" + pitch + ", sounds.json x" + volumeFactor
                                + " / x" + pitchFactor + "]";
                return sound + "@" + sentVolume() + "/" + sentPitch() + raw;
            }
        }

        private static final List<Heard> HEARD = java.util.Collections.synchronizedList(new ArrayList<>());
        private static volatile boolean registered;
        private static volatile boolean armed;

        private SoundRecorder() {
        }

        /**
         * Registers the listener once. Called from a client thread step, and idempotent because the
         * whole in-world list runs in one client: a second registration would report every sound
         * twice and turn "the spatula also played the chisel sound" into a false positive.
         */
        static void install(Minecraft client) {
            if (registered) {
                return;
            }

            registered = true;
            client.getSoundManager().addListener((sound, soundEvent, range) -> {
                if (!armed) {
                    return;
                }

                Sound variant = sound.getSound();
                float volumeFactor = 1.0f;
                float pitchFactor = 1.0f;
                boolean constantFactors = true;

                if (variant != null) {
                    volumeFactor = variant.getVolume().sample(RandomSource.create(1L));
                    pitchFactor = variant.getPitch().sample(RandomSource.create(1L));

                    for (long seed = 2L; seed <= 3L && constantFactors; seed++) {
                        constantFactors =
                                variant.getVolume().sample(RandomSource.create(seed)) == volumeFactor
                                        && variant.getPitch().sample(RandomSource.create(seed)) == pitchFactor;
                    }
                }

                HEARD.add(new Heard(sound.getIdentifier(), sound.getVolume(), sound.getPitch(),
                        volumeFactor, pitchFactor, constantFactors));
            });
        }

        static void arm() {
            HEARD.clear();
            armed = true;
        }

        static void disarm() {
            armed = false;
        }

        static List<Heard> heard() {
            synchronized (HEARD) {
                return List.copyOf(HEARD);
            }
        }
    }
}
