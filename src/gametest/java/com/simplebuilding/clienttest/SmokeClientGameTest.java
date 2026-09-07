package com.simplebuilding.clienttest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.util.SurvivalTracerAccessor;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
 * Proves the client test harness works - boot a real client, create a world, join it, take a
 * screenshot - and then checks everything the mod contributes to a joined client that is neither a
 * screen nor one of the two in-world renderers.
 *
 * <p><b>Why these live together.</b> The other classes in this package are organised by the thing
 * they measure: {@link BlockHighlightClientGameTest} and {@link BuildingWandPreviewClientGameTest}
 * measure pixels, {@link ModScreensClientGameTest} measures screens,
 * {@link MultiBlockBreakingClientGameTest} measures one renderer's contribution to the level render
 * state. What is left over are claims that need a real client and a joined world but no scene and
 * no screen: language files, baked models, arriving packets, client side interaction results and
 * sounds. Every one of them is invisible to the headless server test suite, and every one of them
 * is a smoke test in the original sense - if it fails, the mod is not correctly installed on a
 * client at all.
 *
 * <p><b>Covered elsewhere.</b> The enchanted book model variants, which belong in the same family
 * of claims, live in {@link ItemRenderingClientGameTest} - it walks all nineteen cases of
 * {@code assets/minecraft/items/enchanted_book.json} instead of only the air jump one, so there is
 * nothing left here to add.
 *
 * <p><b>No pixel measurements here.</b> Two of the checks below could in principle be screenshot
 * differences (the furnace models, the item frame overlay messages) and deliberately are not:
 * <ul>
 *   <li>the lit furnace fronts are <em>animated</em> textures ({@code *_front_on.png.mcmeta}), so
 *       two screenshots of the same lit furnace differ by design and the noise floor assertion the
 *       other classes rely on could never hold;</li>
 *   <li>the item frame messages are drawn on the HUD, and reaching them means sneaking and right
 *       clicking, which moves the camera and swings the first person hand - two changes on screen
 *       that have nothing to do with the message. Reading the message out of the client's own
 *       message event is both cheaper and strictly more precise.</li>
 * </ul>
 * Screenshots are still taken where a human might want to look at the result; they are documentary
 * and nothing is asserted on them.
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
 * construction time out of those properties ({@code Item.Properties#effectiveDescriptionId},
 * defaulting to {@code ITEM_DESCRIPTION_ID}) and {@code BlockItem} overrides neither
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
 * three missing entries, or calling {@code useBlockDescriptionPrefix()} and deleting the duplicated
 * {@code item.} entries - would have to fight the test to land.
 *
 * <p><b>Known defect - {@code tooltip.simplebuilding.netherite_piston} is an orphan.</b> The key
 * exists in both language files (en_us.json:153, de_de.json:141) but no code anywhere in the mod
 * calls {@code appendHoverText} for the piston, so it is never shown. It is deliberately left out
 * of {@link #modLanguageFileReachesTheClient} - asserting that it resolves would turn deleting the
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
 *       the mod contributes nothing to it. Proving it would mean switching the client language,
 *       which in turn needs the vanilla language assets for that locale - available in a dev run,
 *       but the assertion would be about Mojang's code.</li>
 *   <li><b>The chisel sound pitch.</b> {@code ChiselItem} randomises it per use
 *       ({@code 1.0 + random*0.4 - 0.2}), so the test can only assert the 0.8..1.2 band, not a
 *       value.</li>
 *   <li><b>The NeoForge config screen path</b> ({@code IConfigScreenFactory} /
 *       {@code ConfigScreenProvider}). This is a Fabric client; the NeoForge entry point is not on
 *       the classpath. See {@link ModScreensClientGameTest} for the Fabric half.</li>
 * </ul>
 */
public final class SmokeClientGameTest implements FabricClientGameTest {

    /** The six furnace family blocks {@code ModModelProvider} drives through {@code createFurnace}. */
    private static final List<Block> FURNACES = List.of(
            ModBlocks.REINFORCED_FURNACE,
            ModBlocks.NETHERITE_FURNACE,
            ModBlocks.REINFORCED_SMOKER,
            ModBlocks.NETHERITE_SMOKER,
            ModBlocks.REINFORCED_BLAST_FURNACE,
            ModBlocks.NETHERITE_BLAST_FURNACE);

    /** Free air one block in front of the wall, dead centre of the view - the item frame goes here. */
    private static final BlockPos FRAME_POS = new BlockPos(10, 1, RendererTestScene.FRONT_Z);

    private static final BlockPos MOD_PISTON = new BlockPos(11, 1, RendererTestScene.FRONT_Z);
    private static final BlockPos MOD_PISTON_TARGET = new BlockPos(12, 1, RendererTestScene.FRONT_Z);
    private static final BlockPos VANILLA_PISTON = new BlockPos(8, 1, RendererTestScene.FRONT_Z);
    private static final BlockPos VANILLA_PISTON_TARGET = new BlockPos(7, 1, RendererTestScene.FRONT_Z);

    @Override
    public void runTest(ClientGameTestContext context) {
        SoundRecorder.install(context);
        OverlayMessageRecorder.install(context);

        context.takeScreenshot("smoke-main-menu");

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.takeScreenshot("smoke-in-world");
            context.waitTicks(20);

            // The frozen, walled-in scene is reused as a known starting point: a solid block on the
            // crosshair, free air in front of it, no mobs, no random ticks, no weather. Nothing
            // below measures pixels, but everything below needs the aim and the empty working
            // volume that build() guarantees.
            RendererTestScene.build(context, singleplayer, "minecraft:stone", "creative");

            try {
                modLanguageFileReachesTheClient(context);
                furnaceBlockStatesBakeIntoOrientedModels(context, singleplayer);
                survivalCountersArriveOnTheClientEverySecond(context, singleplayer);
                chiselAnswersTheClientSideUseOnItself(context, singleplayer);
                chiselAndSpatulaSoundsReachTheClient(context, singleplayer);
                breakerPistonSoundReachesTheClient(context, singleplayer);
                itemFrameOverlayMessagesReachTheClient(context, singleplayer);
            } finally {
                SoundRecorder.disarm();
                OverlayMessageRecorder.disarm();
                context.getInput().releaseShift();
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    // =================================================================================
    // 1. Language files
    // =================================================================================

    /**
     * The mod's {@code en_us.json} is a client resource: the server never loads it, so no server
     * test can tell a packaged language file from a missing one. This asserts that the client's
     * language manager really resolves the names of the ore generation feature - the two ore
     * blocks, their block items and the four things they smelt into.
     *
     * <p>The check is "the translation differs from the key", which is exactly how vanilla reports
     * a missing entry ({@code Language#getOrDefault} returns the key). A control key that is
     * guaranteed not to exist is looked up first, so the comparison cannot pass by accident - if
     * {@code I18n} ever stopped echoing unknown keys, the control fails instead of the whole test
     * turning into a tautology.
     *
     * <p>The list covers both prefixes the feature needs: {@code block.} for the two ores as they
     * sit in the world, {@code item.} for the things the player carries. Which prefix a block item
     * lands under is not obvious in 26.2, and it is exactly where the mod gets it wrong for three
     * blocks - see the known defect in the class javadoc for why {@code NIHILITH_ORE_ITEM} is
     * absent from this list while its astralit twin is in it.
     *
     * <p>What breaks this test: a language file renamed, moved out of
     * {@code assets/simplebuilding/lang/}, excluded from the jar by {@code processResources}, made
     * unparseable, or one of these five keys renamed on one side only.
     */
    private void modLanguageFileReachesTheClient(ClientGameTestContext context) {
        List<String> keys = List.of(
                ModBlocks.NIHILITH_ORE.getDescriptionId(),
                ModBlocks.ASTRALIT_ORE.getDescriptionId(),
                ModItems.ASTRALIT_ORE_ITEM.getDescriptionId(),
                ModItems.NIHILITH_SHARD.getDescriptionId(),
                ModItems.ASTRALIT_DUST.getDescriptionId());

        String problem = context.computeOnClient(client -> {
            String missingKey = "block.simplebuilding.a_block_that_does_not_exist";

            if (!missingKey.equals(I18n.get(missingKey))) {
                return "control failed: I18n does not echo unknown keys any more (it answered \""
                        + I18n.get(missingKey) + "\"), so \"translated\" cannot be told from "
                        + "\"missing\" the way this test does it";
            }

            List<String> untranslated = new ArrayList<>();

            for (String key : keys) {
                String text = I18n.get(key);

                if (key.equals(text) || text.isBlank()) {
                    untranslated.add(key);
                }
            }

            return untranslated.isEmpty() ? null : "the client language has no entry for " + untranslated;
        });

        if (problem != null) {
            throw new AssertionError("The mod's language file did not reach the client: " + problem);
        }
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
     * <p>What breaks this test: a dropped or renamed {@code createFurnace} call in the model
     * provider, generated blockstate or model json that was not regenerated after a rename, a
     * texture file that is missing from the atlas, or a change to
     * {@code TexturedModel.ORIENTABLE_ONLY_TOP} that stops putting {@code #front} on the north
     * face of {@code minecraft:block/orientable}.
     */
    private void furnaceBlockStatesBakeIntoOrientedModels(ClientGameTestContext context,
                                                          TestSingleplayerContext singleplayer) {
        List<String> problems = context.computeOnClient(client -> {
            BlockStateModelSet models = client.getModelManager().getBlockStateModelSet();
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

                        BlockStateModel model = models.get(state);

                        if (model == models.missingModel()) {
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

            return found;
        });

        if (!problems.isEmpty()) {
            throw new AssertionError("Furnace blockstates or models are wrong on the client: " + problems);
        }

        // Documentary only: the six unlit fronts, side by side, above the line of sight so the
        // crosshair keeps its target. Nothing is asserted on this image.
        for (int i = 0; i < FURNACES.size(); i++) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(FURNACES.get(i));
            singleplayer.getServer().runCommand("setblock " + (8 + i) + " 2 " + RendererTestScene.FRONT_Z
                    + " " + id + "[facing=north,lit=false]");
        }

        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(20);
        context.takeScreenshot("furnace-a-six-fronts");

        clearWorkingVolume(context, singleplayer);
    }

    /** Every sprite the baked model puts on one side of the block. */
    private static Set<Identifier> spritesOn(BlockStateModel model, Direction side) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);

        Set<Identifier> sprites = new LinkedHashSet<>();

        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(side)) {
                sprites.add(quad.materialInfo().sprite().contents().name());
            }
        }

        return sprites;
    }

    // =================================================================================
    // 4. Survival counters over the network
    // =================================================================================

    /**
     * {@code SurvivalTracerMixin} sends a {@code SurvivalSyncPayload} to the player once every
     * twenty ticks; {@code SimplebuildingClient} receives it and writes the numbers into the
     * {@code LocalPlayer} through {@code ClientSurvivalTracerMixin}. A server test can at best
     * count the sends - only a client can say whether anything arrived, whether it kept arriving,
     * and whether the numbers in it are the server's.
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
     * <p>What breaks this test: the {@code tick} injection removed or its {@code % 20} condition
     * broken, the payload dropped by {@code canSendToPlayer}, the client receiver unregistered,
     * {@code ClientSurvivalTracerMixin} no longer applying to {@code LocalPlayer}, or the fields of
     * the payload getting swapped so play time reads some other counter.
     */
    private void survivalCountersArriveOnTheClientEverySecond(ClientGameTestContext context,
                                                              TestSingleplayerContext singleplayer) {
        String setup = context.computeOnClient(client ->
                client.player instanceof SurvivalTracerAccessor
                        ? null
                        : "the LocalPlayer does not implement SurvivalTracerAccessor, so "
                                + "ClientSurvivalTracerMixin was not applied");

        if (setup != null) {
            throw new AssertionError("Survival sync cannot be observed: " + setup);
        }

        int before = clientPlayTime(context);
        context.waitTicks(60);
        int after = clientPlayTime(context);

        int onTheServer = singleplayer.getServer().computeOnServer(server -> {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            return players.isEmpty() ? -1 : players.get(0).getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        });

        if (onTheServer < 0) {
            throw new AssertionError("Survival sync cannot be observed: there is no server player");
        }

        int moved = after - before;

        if (moved <= 0) {
            throw new AssertionError("No SurvivalSyncPayload arrived: the client side play time counter "
                    + "stayed at " + before + " over 60 ticks while the server statistic is at "
                    + onTheServer + ".");
        }

        if (moved < 40 || moved > 80) {
            throw new AssertionError("SurvivalSyncPayload does not arrive once a second: the client side "
                    + "play time counter moved by " + moved + " ticks over a 60 tick window (" + before
                    + " -> " + after + "), expected roughly 60 from three syncs.");
        }

        if (Math.abs(onTheServer - after) > 25) {
            throw new AssertionError("SurvivalSyncPayload carries the wrong number: the client says "
                    + after + " play time ticks, the server statistic says " + onTheServer
                    + ". At most 25 ticks of difference are explainable by the 20 tick sync period.");
        }
    }

    private int clientPlayTime(ClientGameTestContext context) {
        return context.computeOnClient(client ->
                ((SurvivalTracerAccessor) client.player).simplebuilding$getCurrentTime());
    }

    // =================================================================================
    // 5. The chisel's client side branch of useOn
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
     * setup failure instead of as a wrong answer.
     *
     * <p>What breaks this test: the client branch removed or moved behind the server branch, the
     * table lookup reduced to fewer than the four maps, sandstone dropped from the stone tier
     * chain, or the answers swapped.
     */
    private void chiselAnswersTheClientSideUseOnItself(ClientGameTestContext context,
                                                       TestSingleplayerContext singleplayer) {
        String tables = context.computeOnClient(client -> {
            if (inAnyStoneTable(Blocks.GLASS)) {
                return "glass is in one of the stone tier chisel tables, so it is no longer a valid "
                        + "\"nothing to do here\" case";
            }

            return inAnyStoneTable(Blocks.SANDSTONE)
                    ? null
                    : "sandstone is in none of the stone tier chisel tables, so the SUCCESS case "
                            + "below could not fire";
        });

        if (tables != null) {
            throw new AssertionError("Chisel table setup failed: " + tables);
        }

        InteractionResult onChiselable = useChiselOnBlockUnderCrosshair(context, singleplayer, "minecraft:sandstone");

        if (onChiselable != InteractionResult.SUCCESS) {
            throw new AssertionError("ChiselItem#useOn answered " + onChiselable
                    + " on the client for sandstone, which is in the stone tier table; expected SUCCESS. "
                    + RendererTestScene.describeAim(context));
        }

        InteractionResult onPlain = useChiselOnBlockUnderCrosshair(context, singleplayer, "minecraft:glass");

        if (onPlain != InteractionResult.PASS) {
            throw new AssertionError("ChiselItem#useOn answered " + onPlain
                    + " on the client for glass, which is in none of the tables; expected PASS. "
                    + RendererTestScene.describeAim(context));
        }

        singleplayer.getServer().runCommand("setblock " + at(RendererTestScene.TARGET) + " minecraft:stone");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);
    }

    private static boolean inAnyStoneTable(Block block) {
        return ChiselItem.FINAL_STONE_FWD.containsKey(block)
                || ChiselItem.FINAL_STONE_BWD.containsKey(block)
                || ChiselItem.FINAL_STONE_TOUCH_FWD.containsKey(block)
                || ChiselItem.FINAL_STONE_TOUCH_BWD.containsKey(block);
    }

    private InteractionResult useChiselOnBlockUnderCrosshair(ClientGameTestContext context,
                                                             TestSingleplayerContext singleplayer,
                                                             String blockId) {
        singleplayer.getServer().runCommand("setblock " + at(RendererTestScene.TARGET) + " " + blockId);
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:stone_chisel");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        return context.computeOnClient(client -> {
            if (!(client.player.getMainHandItem().getItem() instanceof ChiselItem)) {
                throw new AssertionError("Chisel trigger conditions not met: the main hand holds "
                        + client.player.getMainHandItem());
            }

            if (client.player.getCooldowns().isOnCooldown(client.player.getMainHandItem())) {
                throw new AssertionError("Chisel trigger conditions not met: the chisel is on cooldown, "
                        + "so useOn returns PASS before it ever looks at the block");
            }

            BlockHitResult hit = (BlockHitResult) client.hitResult;
            return client.player.getMainHandItem()
                    .useOn(new UseOnContext(client.player, InteractionHand.MAIN_HAND, hit));
        });
    }

    // =================================================================================
    // 6. Chisel and spatula sounds
    // =================================================================================

    /**
     * A spatula is a {@code ChiselItem} with three things changed at registration time, one of
     * which is {@code setChiselSound(SoundEvents.SAND_FALL)}. The sound itself is played server
     * side with {@code Level#playSound(null, ...)}, so unlike the item frame sounds it does reach
     * the acting player - but only a client can hear it, and nothing in the mod reads the field
     * back.
     *
     * <p>Sounds are observed through vanilla's own {@code SoundManager#addListener} hook, the same
     * one the subtitle overlay uses. {@code SoundEngine#play} notifies listeners before it looks at
     * the volume, so a muted client still reports every sound. The reading is not the packet
     * though - it is the packet times the factor of the {@code sounds.json} variant that was
     * picked, and this sound is precisely where that matters: two of the four
     * {@code ui.stonecutter.take_result} variants declare {@code "pitch": 0.92}. {@link SoundRecorder}
     * divides the factor back out; its javadoc has the details.
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
    private void chiselAndSpatulaSoundsReachTheClient(ClientGameTestContext context,
                                                      TestSingleplayerContext singleplayer) {
        assertTheClientCanHearAnything(context, singleplayer);

        Identifier stonecutter = SoundEvents.UI_STONECUTTER_TAKE_RESULT.location();
        Identifier sandFall = SoundEvents.SAND_FALL.location();

        List<SoundRecorder.Heard> withChisel =
                chiselOnceAndListen(context, singleplayer, "simplebuilding:stone_chisel");
        SoundRecorder.Heard chiselSound = firstOf(withChisel, stonecutter);

        if (chiselSound == null) {
            throw new AssertionError("The chisel played no sound the client could hear: heard "
                    + withChisel + ", expected " + stonecutter + ".");
        }

        assertVolumeAndPitch(chiselSound, "stone chisel");

        List<SoundRecorder.Heard> withSpatula =
                chiselOnceAndListen(context, singleplayer, "simplebuilding:stone_spatula");
        SoundRecorder.Heard spatulaSound = firstOf(withSpatula, sandFall);

        if (spatulaSound == null) {
            throw new AssertionError("The spatula did not use SAND_FALL as its chisel sound: heard "
                    + withSpatula + ", expected " + sandFall + ".");
        }

        if (firstOf(withSpatula, stonecutter) != null) {
            throw new AssertionError("The spatula played the plain chisel sound " + stonecutter
                    + " as well as " + sandFall + " (heard " + withSpatula + "), so setChiselSound "
                    + "is not actually replacing the default.");
        }

        assertVolumeAndPitch(spatulaSound, "stone spatula");

        singleplayer.getServer().runCommand("setblock " + at(RendererTestScene.TARGET) + " minecraft:stone");
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);
    }

    private void assertVolumeAndPitch(SoundRecorder.Heard heard, String label) {
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
    private void assertFactorsAreUsable(SoundRecorder.Heard heard, String label) {
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
     * Puts sandstone under the crosshair, right clicks it once with {@code itemId} and returns
     * everything the client heard while the block changed. Fails if the block did not change, so
     * "no sound" can only ever mean "no sound".
     */
    private List<SoundRecorder.Heard> chiselOnceAndListen(ClientGameTestContext context,
                                                          TestSingleplayerContext singleplayer,
                                                          String itemId) {
        singleplayer.getServer().runCommand("setblock " + at(RendererTestScene.TARGET) + " minecraft:sandstone");
        singleplayer.getServer().runCommand("item replace entity @a weapon.mainhand with " + itemId);
        singleplayer.getConnection().waitForClientboundPackets();
        context.getInput().releaseShift();
        context.waitTicks(15);

        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        SoundRecorder.arm();

        try {
            context.getInput().pressMouse(1);

            for (int tick = 0; tick < 60; tick++) {
                context.waitTick();

                boolean changed = context.computeOnClient(client ->
                        !client.level.getBlockState(RendererTestScene.TARGET).is(Blocks.SANDSTONE));

                if (changed) {
                    // Give the sound packet a few ticks to catch up with the block update.
                    context.waitTicks(5);
                    return SoundRecorder.heard();
                }
            }
        } finally {
            SoundRecorder.disarm();
        }

        throw new AssertionError("The sandstone under the crosshair never changed while right clicking "
                + "with " + itemId + ", so the chisel never ran and no sound could be attributed to it. "
                + RendererTestScene.describeAim(context));
    }

    /**
     * A client whose sound engine did not start reports no sounds at all
     * ({@code SoundEngine#play} returns before it notifies listeners). This makes that a setup
     * failure with its own message instead of a mod defect somewhere further down.
     */
    private void assertTheClientCanHearAnything(ClientGameTestContext context,
                                                TestSingleplayerContext singleplayer) {
        Identifier bell = SoundEvents.NOTE_BLOCK_BELL.value().location();

        SoundRecorder.arm();

        try {
            singleplayer.getServer().runCommand("playsound " + bell + " block @a 10.5 1.0 17.0 1 1");
            singleplayer.getConnection().waitForClientboundPackets();

            for (int tick = 0; tick < 40; tick++) {
                context.waitTick();

                if (firstOf(SoundRecorder.heard(), bell) != null) {
                    return;
                }
            }
        } finally {
            SoundRecorder.disarm();
        }

        throw new AssertionError("This client cannot observe sounds: a /playsound of " + bell
                + " never reached the SoundManager listener. Either the sound engine failed to start "
                + "(no audio device) or SoundManager#addListener no longer fires - in both cases the "
                + "sound assertions below would be meaningless, so they are not attempted.");
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
    // 7. The breaker piston's sound
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
     * too.
     *
     * <p>What breaks this test: the sound removed or its event, volume or pitch changed; the
     * playback moved inside the {@code isClientSide} branch or given the acting player as its
     * "except" argument; the redstone strength check refusing a block it used to break.
     */
    private void breakerPistonSoundReachesTheClient(ClientGameTestContext context,
                                                    TestSingleplayerContext singleplayer) {
        Identifier breakSound = SoundEvents.ZOMBIE_ATTACK_IRON_DOOR.location();

        List<SoundRecorder.Heard> vanilla = firePiston(context, singleplayer, "minecraft:piston", "west",
                VANILLA_PISTON, VANILLA_PISTON_TARGET);

        if (firstOf(vanilla, breakSound) != null) {
            throw new AssertionError("Control failed: a plain vanilla piston already played " + breakSound
                    + " (heard " + vanilla + "), so the measurement below would not be attributable "
                    + "to NetheriteBreakerPistonBlock.");
        }

        boolean vanillaTargetSurvived = context.computeOnClient(client ->
                client.level.getBlockState(VANILLA_PISTON_TARGET.west()).is(Blocks.STONE));

        if (!vanillaTargetSurvived) {
            throw new AssertionError("Control failed: the vanilla piston did not push its target block to "
                    + VANILLA_PISTON_TARGET.west() + ", so the wiring in this step never fired and "
                    + "\"no sound\" says nothing.");
        }

        clearWorkingVolume(context, singleplayer);

        List<SoundRecorder.Heard> mod = firePiston(context, singleplayer, "simplebuilding:netherite_piston",
                "east", MOD_PISTON, MOD_PISTON_TARGET);

        // "Broke it" and not merely "extended past it": a piston that only pushed would have left
        // the stone one block further out, where the extended head is not.
        boolean stoneSurvived = context.computeOnClient(client ->
                client.level.getBlockState(MOD_PISTON_TARGET).is(Blocks.STONE)
                        || client.level.getBlockState(MOD_PISTON_TARGET.east()).is(Blocks.STONE));

        if (stoneSurvived) {
            throw new AssertionError("The netherite breaker piston did not break the block in front of it "
                    + "(the stone is still at " + MOD_PISTON_TARGET + " or was pushed to "
                    + MOD_PISTON_TARGET.east() + "), so the sound branch was never reached. Heard: " + mod);
        }

        SoundRecorder.Heard heard = firstOf(mod, breakSound);

        if (heard == null) {
            throw new AssertionError("The netherite breaker piston broke its target but the client heard "
                    + "no " + breakSound + " (heard " + mod + ").");
        }

        assertFactorsAreUsable(heard, "breaker piston");

        if (Math.abs(heard.sentVolume() - 0.5f) > 0.001f || Math.abs(heard.sentPitch() - 0.8f) > 0.001f) {
            throw new AssertionError("The breaker piston sound was played as " + heard
                    + ", expected volume 0.5 and pitch 0.8.");
        }

        context.takeScreenshot("sound-a-breaker-piston");
        clearWorkingVolume(context, singleplayer);
    }

    /** Builds piston, target and power source, powers it and returns what the client heard. */
    private List<SoundRecorder.Heard> firePiston(ClientGameTestContext context,
                                                 TestSingleplayerContext singleplayer,
                                                 String pistonId, String facing,
                                                 BlockPos piston, BlockPos target) {
        singleplayer.getServer().runCommand("setblock " + at(target) + " minecraft:stone");
        singleplayer.getServer().runCommand(
                "setblock " + at(piston) + " " + pistonId + "[facing=" + facing + ",extended=false]");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);

        SoundRecorder.arm();

        try {
            singleplayer.getServer().runCommand("setblock " + at(piston.above()) + " minecraft:redstone_block");
            singleplayer.getConnection().waitForClientboundPackets();
            context.waitTicks(25);
            return SoundRecorder.heard();
        } finally {
            SoundRecorder.disarm();
        }
    }

    // =================================================================================
    // 8. Item frame overlay messages
    // =================================================================================

    /**
     * {@code ItemFrameEntityMixin} answers four sneak interactions with an action bar message. The
     * messages are sent as system messages with the overlay flag set, so the server can only be
     * asked whether it tried; whether one arrived, and with which text, is a client fact. They are
     * read out of {@code ClientReceiveMessageEvents.GAME}, which fires in the client's packet
     * handler with the same {@code overlay} flag the packet carried - the assertion is therefore on
     * the real message, not on a repaint of the HUD.
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
     * server side state it depends on (frame present, item inside, player sneaking) before the
     * click, so a message that does not turn up cannot be blamed on a click that never landed.
     *
     * <p>The sounds that go with these four messages are <em>not</em> asserted - see the known
     * defect in the class javadoc.
     *
     * <p>What breaks this test: any of the four branches losing its {@code sendOverlayMessage}, the
     * branch order changing so a different message answers the same click, the message text
     * changing, or {@code interact} no longer being injected at all - the last one shows up as the
     * lock step failing first.
     */
    private void itemFrameOverlayMessagesReachTheClient(ClientGameTestContext context,
                                                        TestSingleplayerContext singleplayer) {
        clearWorkingVolume(context, singleplayer);
        placeAndFillItemFrame(context, singleplayer);

        context.getInput().holdShift();
        context.waitTicks(10);

        try {
            assertPlayerIsSneaking(singleplayer);

            equip(context, singleplayer, "minecraft:glass_pane");
            expectOverlayMessage(context, singleplayer, "Item Frame gesperrt (Locked).", "lock with a glass pane");
            expectOverlayMessage(context, singleplayer, "Item Frame entsperrt.", "unlock by sneaking again");

            equip(context, singleplayer, "minecraft:shears");
            expectOverlayMessage(context, singleplayer, "Item Frame unsichtbar gemacht.", "hide with shears");
            context.takeScreenshot("itemframe-a-invisible");

            expectOverlayMessage(context, singleplayer, "Item Frame sichtbar gemacht.", "show by sneaking again");
            context.takeScreenshot("itemframe-b-visible-again");
        } finally {
            context.getInput().releaseShift();
            context.waitTicks(5);
        }

        singleplayer.getServer().runCommand("kill @e[type=minecraft:item_frame]");
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runCommand("tp @a 10.5 0.0 16.5 0.0 0.0");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);
        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);
    }

    /** Right clicks an item frame onto the wall and puts a stone into it, both through real input. */
    private void placeAndFillItemFrame(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        context.getInput().releaseShift();
        equip(context, singleplayer, "minecraft:item_frame");
        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        context.getInput().pressMouse(1);
        context.waitTicks(15);

        if (itemFrameState(singleplayer) == null) {
            throw new AssertionError("Setup failed: right clicking the wall with an item frame placed "
                    + "nothing at " + FRAME_POS + ". " + RendererTestScene.describeAim(context));
        }

        // Step one block closer. Blocks and entities have separate reaches:
        // Attributes.BLOCK_INTERACTION_RANGE is 4.5 and covers the wall from the scene's default
        // standpoint, but ENTITY_INTERACTION_RANGE is 3.0, and LocalPlayer#raycastHitResult turns
        // an entity hit beyond it into a MISS. At the default 3.44 blocks the crosshair would
        // therefore never report the frame, no matter how exactly it is aimed.
        singleplayer.getServer().runCommand("tp @a 10.5 0.0 17.6 0.0 0.0");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        equip(context, singleplayer, "minecraft:stone");
        assertCrosshairIsOnTheItemFrame(context);

        context.getInput().pressMouse(1);
        context.waitTicks(15);

        FrameState state = itemFrameState(singleplayer);

        if (state == null || state.empty()) {
            throw new AssertionError("Setup failed: the item frame is still empty after right clicking it "
                    + "with a stone (" + state + "). Every branch under test needs a filled frame.");
        }
    }

    private void expectOverlayMessage(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                      String expected, String step) {
        assertCrosshairIsOnTheItemFrame(context);
        assertPlayerIsSneaking(singleplayer);

        OverlayMessageRecorder.arm();

        try {
            context.getInput().pressMouse(1);

            for (int tick = 0; tick < 40; tick++) {
                context.waitTick();

                if (OverlayMessageRecorder.seen().contains(expected)) {
                    context.waitTicks(6);
                    return;
                }
            }

            throw new AssertionError("The item frame step \"" + step + "\" sent no action bar message \""
                    + expected + "\" to the client. Seen instead: " + OverlayMessageRecorder.seen()
                    + ". Frame state: " + itemFrameState(singleplayer) + ".");
        } finally {
            OverlayMessageRecorder.disarm();
        }
    }

    private void assertCrosshairIsOnTheItemFrame(ClientGameTestContext context) {
        boolean onFrame = context.computeOnClient(client ->
                client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof ItemFrame);

        if (!onFrame) {
            throw new AssertionError("The crosshair is not on the item frame, so the right click below "
                    + "would go to the wall instead. " + RendererTestScene.describeAim(context));
        }
    }

    private void assertPlayerIsSneaking(TestSingleplayerContext singleplayer) {
        boolean sneaking = singleplayer.getServer().computeOnServer(server -> {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            return !players.isEmpty() && players.get(0).isShiftKeyDown();
        });

        if (!sneaking) {
            throw new AssertionError("The server does not see the player sneaking, so every sneak branch "
                    + "of ItemFrameEntityMixin is skipped and \"no message\" would say nothing.");
        }
    }

    /** What the server thinks of the item frame at {@link #FRAME_POS}, or null if there is none. */
    private FrameState itemFrameState(TestSingleplayerContext singleplayer) {
        return singleplayer.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            AABB around = new AABB(FRAME_POS.getX(), FRAME_POS.getY(), FRAME_POS.getZ(),
                    FRAME_POS.getX() + 1.0, FRAME_POS.getY() + 1.0, FRAME_POS.getZ() + 1.0).inflate(1.0);
            List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, around);

            if (frames.isEmpty()) {
                return null;
            }

            ItemFrame frame = frames.get(0);
            return new FrameState(frame.getItem().isEmpty(), frame.isInvisible());
        });
    }

    private record FrameState(boolean empty, boolean invisible) {
    }

    // =================================================================================
    // Shared helpers
    // =================================================================================

    private void equip(ClientGameTestContext context, TestSingleplayerContext singleplayer, String itemId) {
        singleplayer.getServer().runCommand("item replace entity @a weapon.mainhand with " + itemId);
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(12);
    }

    /** Empties the strip of air in front of the wall and puts the crosshair back on the target. */
    private void clearWorkingVolume(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("fill 5 0 17 15 3 " + RendererTestScene.FRONT_Z + " minecraft:air");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);
        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);
    }

    private static String at(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
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
     * volume sliders, so a muted client reports every sound.
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
     * {@link SmokeClientGameTest#assertFactorsAreUsable} turns that into a loud setup failure
     * instead of a silent wrong number.
     */
    private static final class SoundRecorder {

        /**
         * @param volume        raw reading, packet value times the variant's own volume factor
         * @param pitch         raw reading, packet value times the variant's own pitch factor
         * @param constantFactors whether those two factors are constants, so dividing them out is
         *                      exact rather than a second, unrelated random draw
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

        private static final List<Heard> HEARD = Collections.synchronizedList(new ArrayList<>());
        private static volatile boolean registered;
        private static volatile boolean armed;

        private SoundRecorder() {
        }

        static void install(ClientGameTestContext context) {
            context.runOnClient(client -> {
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

    /**
     * Records the action bar messages the client receives while armed.
     *
     * <p>{@code ClientReceiveMessageEvents.GAME} fires in the client's own packet handler and hands
     * over the {@code overlay} flag of the incoming system message, which is what
     * {@code ServerPlayer#sendOverlayMessage} sets.
     */
    private static final class OverlayMessageRecorder {

        private static final List<String> SEEN = Collections.synchronizedList(new ArrayList<>());
        private static volatile boolean registered;
        private static volatile boolean armed;

        private OverlayMessageRecorder() {
        }

        static void install(ClientGameTestContext context) {
            context.runOnClient(client -> {
                if (registered) {
                    return;
                }

                registered = true;
                ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                    if (armed && overlay) {
                        SEEN.add(message.getString());
                    }
                });
            });
        }

        static void arm() {
            SEEN.clear();
            armed = true;
        }

        static void disarm() {
            armed = false;
        }

        static List<String> seen() {
            synchronized (SEEN) {
                return List.copyOf(SEEN);
            }
        }
    }
}
