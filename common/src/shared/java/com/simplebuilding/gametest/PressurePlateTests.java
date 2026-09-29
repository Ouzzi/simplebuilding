package com.simplebuilding.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.tweaks.block.CopperPressurePlateBlock;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Druckplatten der Mod (Besitzer 2026-09-27): gewachste Kupferplatten (Honigwabe wachst, die Axt
 * kratzt erst das Wachs und dann die Oxidation ab, gewachst oxidiert nichts), das Loslassen der
 * Kupferplatte dauert so lange wie das Ausloesen, jede Mod-Platte sinkt gedrueckt sichtbar ein wie eine
 * Vanilla-Platte und heisst wie eine ("X Pressure Plate"/"...Druckplatte").
 *
 * <p>Die Platten werden hier nicht aus einer Liste im Test genommen, sondern aus der Registry: jeder
 * Mod-Block {@code *_pressure_plate} - eine neue Platte faellt so automatisch unter die Pruefungen.
 */
public final class PressurePlateTests {

    /** Ausloesen und Loslassen einer angelaufenen Platte (je 40 Ticks) plus Luft. */
    public static final int RELEASE_MAX_TICKS = 200;

    private PressurePlateTests() {
    }

    /** Jeder Mod-Block, dessen Id auf {@code _pressure_plate} endet. */
    private static List<Block> modPlates() {
        List<Block> plates = new ArrayList<>();
        for (Block block : TweaksBlocks.all()) {
            if (BuiltInRegistries.BLOCK.getKey(block).getPath().endsWith("_pressure_plate")) {
                plates.add(block);
            }
        }
        return plates;
    }

    /**
     * Honigwabe wachst jede Oxidationsstufe zu ihrer gewachsten Stufe, verbraucht eine Wabe und laesst
     * den Besitzer stehen; gewachste Platten oxidieren nicht mehr (kein Zufallstick, keine naechste
     * Stufe), und eine zweite Wabe tut nichts.
     */
    public static void honeycombWaxesEveryStageAndWaxedPlatesStopOxidizing(GameTestHelper helper) {
        List<Block> stages = CopperPressurePlateBlock.stages();
        List<Block> waxed = CopperPressurePlateBlock.waxedStages();
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(1.5, 1.0, 1.5));
        for (int i = 0; i < stages.size(); i++) {
            BlockPos plate = new BlockPos(1 + i, 1, 3);
            helper.setBlock(plate, stages.get(i));
            UUID owner = UUID.randomUUID();
            helper.getBlockEntity(plate, OwnedBlockEntity.class).setOwner(owner);
            ItemStack honeycomb = new ItemStack(Items.HONEYCOMB, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, honeycomb);
            InteractionResult result = use(helper, player, plate, honeycomb);
            helper.assertTrue(result.consumesAction(), "honeycomb did nothing on " + stages.get(i) + ": " + result);
            BlockState now = helper.getBlockState(plate);
            helper.assertTrue(now.is(waxed.get(i)), "honeycomb turned " + stages.get(i) + " into " + now + " instead of " + waxed.get(i));
            helper.assertTrue(honeycomb.getCount() == 2, "waxing used " + (3 - honeycomb.getCount()) + " honeycombs instead of one");
            helper.assertTrue(owner.equals(helper.getBlockEntity(plate, OwnedBlockEntity.class).getOwner()), "waxing " + stages.get(i) + " lost its owner");
            CopperPressurePlateBlock block = (CopperPressurePlateBlock) now.getBlock();
            helper.assertTrue(block.isWaxed() && block.getAge() == ((CopperPressurePlateBlock) stages.get(i)).getAge(),
                    waxed.get(i) + " is not the waxed twin of " + stages.get(i));
            helper.assertFalse(now.isRandomlyTicking(), waxed.get(i) + " still ticks randomly and so still oxidizes");
            helper.assertTrue(block.getNext(now).isEmpty(), waxed.get(i) + " still has a next oxidation stage");
            InteractionResult again = use(helper, player, plate, honeycomb);
            helper.assertFalse(again.consumesAction(), "honeycomb on the already waxed " + waxed.get(i) + " did something: " + again);
            helper.assertTrue(honeycomb.getCount() == 2 && helper.getBlockState(plate).is(waxed.get(i)),
                    "a second honeycomb changed the waxed plate or was used up");
        }
        helper.assertTrue(TweaksBlocks.COPPER_PRESSURE_PLATE.defaultBlockState().isRandomlyTicking(),
                "the unwaxed copper plate no longer ticks randomly, so it would never oxidize");
        helper.succeed();
    }

    /**
     * Die Axt kratzt von einer gewachsten Platte zuerst das Wachs ab (gleiche Oxidationsstufe, der
     * Besitzer bleibt), erst danach wie bisher eine Oxidationsstufe; jedes Kratzen kostet 1 Haltbarkeit.
     */
    public static void theAxeScrapesTheWaxOffBeforeTheOxidation(GameTestHelper helper) {
        BlockPos plate = new BlockPos(3, 1, 3);
        helper.setBlock(plate, TweaksBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE);
        UUID owner = UUID.randomUUID();
        helper.getBlockEntity(plate, OwnedBlockEntity.class).setOwner(owner);
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        InteractionResult first = use(helper, player, plate, axe);
        helper.assertTrue(first.consumesAction(), "the axe did nothing on a waxed weathered plate: " + first);
        helper.assertTrue(helper.getBlockState(plate).is(TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE),
                "the axe turned the waxed weathered plate into " + helper.getBlockState(plate) + " instead of the weathered plate");
        helper.assertTrue(owner.equals(helper.getBlockEntity(plate, OwnedBlockEntity.class).getOwner()), "scraping the wax off lost the owner");
        helper.assertTrue(axe.getDamageValue() == 1, "scraping the wax off cost " + axe.getDamageValue() + " durability instead of 1");
        use(helper, player, plate, axe);
        helper.assertTrue(helper.getBlockState(plate).is(TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE),
                "the second axe use turned the weathered plate into " + helper.getBlockState(plate) + " instead of the exposed plate");
        // Die gewachste unoxidierte Platte hat keine Oxidation mehr, aber noch Wachs.
        helper.setBlock(plate, TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE);
        use(helper, player, plate, axe);
        helper.assertTrue(helper.getBlockState(plate).is(TweaksBlocks.COPPER_PRESSURE_PLATE),
                "the axe turned the waxed copper plate into " + helper.getBlockState(plate));
        InteractionResult bare = use(helper, player, plate, axe);
        helper.assertFalse(bare.consumesAction(), "the axe still scraped something off a bare copper plate: " + bare);
        helper.succeed();
    }

    /** Wie Vanilla-Kupfer: Platte + Honigwabe an der Werkbank ergibt die gewachste Platte, fuer jede Stufe. */
    public static void waxedCopperPlatesAreCraftedFromThePlateAndOneHoneycomb(GameTestHelper helper) {
        List<Block> stages = CopperPressurePlateBlock.stages();
        List<Block> waxed = CopperPressurePlateBlock.waxedStages();
        for (int i = 0; i < stages.size(); i++) {
            CraftingInput grid = CraftingInput.of(2, 1, List.of(new ItemStack(stages.get(i)), new ItemStack(Items.HONEYCOMB)));
            Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
            helper.assertTrue(match.isPresent(), stages.get(i) + " + honeycomb matches no recipe");
            ItemStack result = match.get().value().assemble(grid);
            helper.assertTrue(result.is(waxed.get(i).asItem()), stages.get(i) + " + honeycomb makes " + result + " instead of " + waxed.get(i));
        }
        helper.succeed();
    }

    /**
     * Loslassen dauert so lange wie Ausloesen: eine angelaufene Platte (40 Ticks) bleibt nach dem
     * Heruntergehen noch 40 Ticks gedrueckt, nicht einen Tick wie frueher.
     */
    public static void copperPlatesReleaseAsLateAsTheyPress(GameTestHelper helper) {
        BlockPos plate = new BlockPos(4, 1, 4);
        // Wax fixes the oxidation tier while this test measures its 40-tick delay.
        helper.setBlock(plate, TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE);
        int required = CopperPressurePlateBlock.requiredTicks(((CopperPressurePlateBlock) TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE).getAge());
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 1.05, 4.5));
        long[] ticks = new long[3];
        ticks[0] = helper.getTick();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(plate).getValue(CopperPressurePlateBlock.POWERED),
                        "the exposed copper plate never powered"))
                .thenExecute(() -> {
                    ticks[1] = helper.getTick();
                    Vec3 away = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
                    player.snapTo(away.x, away.y, away.z, 0.0F, 0.0F);
                })
                .thenExecuteAfter(required - 10, () -> helper.assertTrue(helper.getBlockState(plate).getValue(CopperPressurePlateBlock.POWERED),
                        "the copper plate let go " + (required - 10) + " ticks after the player stepped off, before its " + required + " ticks were up"))
                .thenWaitUntil(() -> helper.assertFalse(helper.getBlockState(plate).getValue(CopperPressurePlateBlock.POWERED),
                        "the copper plate never let go"))
                .thenExecute(() -> {
                    ticks[2] = helper.getTick();
                    long press = ticks[1] - ticks[0];
                    long release = ticks[2] - ticks[1];
                    helper.assertTrue(Math.abs(press - release) <= 2 && Math.abs(release - required) <= 2,
                            "pressing took " + press + " ticks but letting go " + release + " (both should be about " + required + ")");
                })
                .thenSucceed();
    }

    /**
     * Jede Mod-Druckplatte sinkt gedrueckt ein wie eine Vanilla-Platte: sie hat {@code powered}, ihre
     * Form ist gedrueckt so flach wie die der Steindruckplatte, und ihr Blockstate zeigt fuer
     * {@code powered=true} ein Modell aus {@code pressure_plate_down}, sonst {@code pressure_plate_up}.
     */
    public static void everyModPressurePlateVisiblySinksWhenPressed(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockState stone = Blocks.STONE_PRESSURE_PLATE.defaultBlockState();
        double vanillaUp = stone.getShape(helper.getLevel(), origin).max(Direction.Axis.Y);
        double vanillaDown = stone.setValue(PressurePlateBlock.POWERED, true).getShape(helper.getLevel(), origin).max(Direction.Axis.Y);
        List<Block> plates = modPlates();
        if (plates.size() != 11) {
            problems.add("expected 11 mod pressure plates (diamond, netherite, enderite, 4 copper, 4 waxed copper) but found " + plates.size());
        }
        for (Block block : plates) {
            String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
            BlockState state = block.defaultBlockState();
            if (!state.hasProperty(BlockStateProperties.POWERED)) {
                problems.add(id + " has no powered property");
                continue;
            }
            double up = state.setValue(BlockStateProperties.POWERED, false).getShape(helper.getLevel(), origin).max(Direction.Axis.Y);
            double down = state.setValue(BlockStateProperties.POWERED, true).getShape(helper.getLevel(), origin).max(Direction.Axis.Y);
            if (up != vanillaUp || down != vanillaDown) {
                problems.add(id + " is " + up + " high and " + down + " pressed instead of " + vanillaUp + " and " + vanillaDown);
            }
            JsonObject blockstate = json("/assets/simplebuilding/blockstates/" + id + ".json", problems);
            if (blockstate == null) {
                continue;
            }
            JsonObject variants = blockstate.getAsJsonObject("variants");
            String pressed = variants == null ? null : model(variants, "powered=true");
            String released = variants == null ? null : model(variants, "powered=false");
            if (pressed == null || released == null || pressed.equals(released)) {
                problems.add(id + " blockstate has no separate powered=true/false models: " + blockstate);
                continue;
            }
            expectParent(pressed, "minecraft:block/pressure_plate_down", id, problems);
            expectParent(released, "minecraft:block/pressure_plate_up", id, problems);
        }
        helper.assertTrue(problems.isEmpty(), "pressure plates: " + problems);
        helper.succeed();
    }

    /** Alle Mod-Druckplatten heissen wie Vanilla-Platten: "X Pressure Plate" und "...Druckplatte". */
    public static void everyModPressurePlateIsNamedLikeTheVanillaOnes(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        JsonObject en = json("/assets/simplebuilding/lang/en_us.json", problems);
        JsonObject de = json("/assets/simplebuilding/lang/de_de.json", problems);
        if (en != null && de != null) {
            for (Block block : modPlates()) {
                String key = block.getDescriptionId();
                String enName = en.has(key) ? en.get(key).getAsString() : null;
                String deName = de.has(key) ? de.get(key).getAsString() : null;
                if (enName == null || !enName.endsWith(" Pressure Plate")) {
                    problems.add(key + " is \"" + enName + "\" in English, not \"<Material> Pressure Plate\"");
                }
                if (deName == null || !deName.endsWith("Druckplatte")) {
                    problems.add(key + " is \"" + deName + "\" in German, not \"...Druckplatte\"");
                }
            }
            String waxed = en.has(TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE.getDescriptionId())
                    ? en.get(TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE.getDescriptionId()).getAsString() : null;
            if (!"Waxed Exposed Copper Pressure Plate".equals(waxed)) {
                problems.add("the waxed exposed copper plate is \"" + waxed + "\" instead of \"Waxed Exposed Copper Pressure Plate\" (vanilla pattern)");
            }
        }
        helper.assertTrue(problems.isEmpty(), "pressure plate names: " + problems);
        helper.succeed();
    }

    private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos plate, ItemStack stack) {
        BlockPos abs = helper.absolutePos(plate);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        return helper.getBlockState(plate).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    private static String model(JsonObject variants, String key) {
        if (!variants.has(key)) {
            return null;
        }
        return variants.get(key).isJsonArray()
                ? variants.getAsJsonArray(key).get(0).getAsJsonObject().get("model").getAsString()
                : variants.getAsJsonObject(key).get("model").getAsString();
    }

    private static void expectParent(String model, String parent, String id, List<String> problems) {
        int colon = model.indexOf(':');
        JsonObject json = json("/assets/" + model.substring(0, colon) + "/models/" + model.substring(colon + 1) + ".json", problems);
        if (json != null && (!json.has("parent") || !parent.equals(json.get("parent").getAsString()))) {
            problems.add(id + " uses " + model + " with parent " + json.get("parent") + " instead of " + parent);
        }
    }

    private static JsonObject json(String path, List<String> problems) {
        try (InputStream in = TweaksBlocks.class.getResourceAsStream(path)) {
            if (in == null) {
                problems.add("missing resource " + path);
                return null;
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            problems.add("unreadable resource " + path + ": " + e);
            return null;
        }
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** Ohne instabuild, damit die Honigwabe verbraucht wird (siehe TweaksTests#survivalLikePlayer). */
    private static ServerPlayer survivalLikePlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = mockPlayer(helper, relative);
        player.getAbilities().instabuild = false;
        return player;
    }
}
