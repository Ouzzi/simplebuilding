package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.client.gui.HudLayout;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.tooltip.InfoTooltips;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.ChunkLoaderBlock;
import com.simplebuilding.tweaks.block.FlypadBlock;
import com.simplebuilding.tweaks.block.LaunchpadBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.FlypadBlockEntity;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Immersion (owner 2026-09-28): visible block states of the pads (launchpad fill level, chunk loader
 * and flypad switched on), the flypad's edge warning, the item info tooltips and the HUD layout.
 * Sounds and particles are not asserted here - they are fire-and-forget calls on the level.
 */
public final class ImmersionTests {

    private ImmersionTests() {
    }

    // =====================================================================================
    // Launchpad fill level
    // =====================================================================================

    /**
     * The fill level follows the charges in thirds, rounded up: 0 only when empty, 3 when full; it
     * is written into the block state without replacing the block entity, and a pad from an older
     * world (charges but level 0) catches up on its next tick.
     *
     * <p><strong>What breaks this test:</strong> a fill level that is not refreshed after loading,
     * that rounds down (1 of 4 would read empty), or a state change that throws away the charges.
     */
    public static void launchpadShowsItsFillLevelInItsBlockState(GameTestHelper helper) {
        helper.assertValueEqual(LaunchpadBlock.chargeLevel(0, 4), 0, "fill level of an empty launchpad");
        helper.assertValueEqual(LaunchpadBlock.chargeLevel(1, 16), 1, "fill level with 1 of 16 charges");
        helper.assertValueEqual(LaunchpadBlock.chargeLevel(6, 16), 2, "fill level with 6 of 16 charges");
        helper.assertValueEqual(LaunchpadBlock.chargeLevel(11, 16), 3, "fill level with 11 of 16 charges");
        helper.assertValueEqual(LaunchpadBlock.chargeLevel(16, 16), 3, "fill level of a full launchpad");

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TweaksBlocks.LAUNCHPAD);
        LaunchpadBlockEntity be = helper.getBlockEntity(pos, LaunchpadBlockEntity.class);
        helper.assertValueEqual(helper.getBlockState(pos).getValue(LaunchpadBlock.CHARGE), 0, "fill level of a fresh launchpad");
        int[] expected = {1, 2, 3, 3};
        for (int i = 0; i < 4; i++) {
            be.addCharges(1, 4);
            helper.assertValueEqual(helper.getBlockState(pos).getValue(LaunchpadBlock.CHARGE), expected[i],
                    "fill level with " + (i + 1) + " of 4 charges");
        }
        helper.assertTrue(helper.getBlockEntity(pos, LaunchpadBlockEntity.class) == be,
                "the fill level change replaced the launchpad's block entity");
        helper.assertValueEqual(be.getCharges(), 4, "charges after the fill level changes");

        // An old world: charges stored, the block state still says empty.
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pos);
        level.setBlock(abs, helper.getBlockState(pos).setValue(LaunchpadBlock.CHARGE, 0), 2);
        LaunchpadBlockEntity.tick(level, abs, level.getBlockState(abs), be);
        helper.assertValueEqual(helper.getBlockState(pos).getValue(LaunchpadBlock.CHARGE), 3,
                "fill level of an old full launchpad after one tick");
        helper.succeed();
    }

    // =====================================================================================
    // Chunk loader and flypad switched on
    // =====================================================================================

    /**
     * A chunk loader that keeps its chunks is {@code active=true} and glows at its tier's light
     * level; switched off by the config it releases, turns {@code active=false} and only glimmers.
     *
     * <p><strong>What breaks this test:</strong> a state that is not set from {@code update}, or a
     * light level that does not follow it.
     */
    public static void chunkLoaderShowsWhetherItKeepsChunksLoaded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TweaksBlocks.NETHERITE_CHUNK_LOADER);
        ChunkLoaderBlockEntity be = helper.getBlockEntity(pos, ChunkLoaderBlockEntity.class);
        helper.assertFalse(helper.getBlockState(pos).getValue(ChunkLoaderBlock.ACTIVE), "a chunk loader starts switched on");
        helper.assertValueEqual(helper.getBlockState(pos).getLightEmission(), 3, "light of a switched off chunk loader");
        boolean enabled = SimpleTweaks.config().pads.enableChunkLoaders;
        try {
            SimpleTweaks.config().pads.enableChunkLoaders = true;
            be.update(level);
            helper.assertTrue(helper.getBlockState(pos).getValue(ChunkLoaderBlock.ACTIVE), "a working chunk loader is not shown as active");
            helper.assertTrue(be.isActive(), "isActive() disagrees with the block state");
            helper.assertValueEqual(helper.getBlockState(pos).getLightEmission(), 8, "light of a working netherite chunk loader");
            helper.assertTrue(helper.getBlockEntity(pos, ChunkLoaderBlockEntity.class) == be,
                    "switching the state replaced the chunk loader's block entity");

            SimpleTweaks.config().pads.enableChunkLoaders = false;
            be.update(level);
            helper.assertFalse(helper.getBlockState(pos).getValue(ChunkLoaderBlock.ACTIVE), "a switched off chunk loader still shows active");
            helper.assertTrue(be.ownForced().isEmpty(), "a switched off chunk loader keeps chunks");
        } finally {
            SimpleTweaks.config().pads.enableChunkLoaders = enabled;
        }
        helper.setBlock(pos, Blocks.AIR);
        helper.succeed();
    }

    /**
     * A flypad is {@code active=true} while a player stands in its field and turns back when nobody
     * is left; the item model shows the switched off pad.
     *
     * <p><strong>What breaks this test:</strong> an active state that ignores the players in the
     * field or never switches back.
     */
    public static void flypadShowsActiveWhileSomeoneIsInItsField(GameTestHelper helper) {
        BlockPos pad = new BlockPos(3, 1, 3);
        helper.setBlock(pad, TweaksBlocks.FLYPAD);
        FlypadBlockEntity be = helper.getBlockEntity(pad, FlypadBlockEntity.class);
        helper.assertFalse(helper.getBlockState(pad).getValue(FlypadBlock.ACTIVE), "a fresh flypad is shown as active");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 inside = helper.absoluteVec(new Vec3(3.5, 3.0, 3.5));
        player.snapTo(inside.x, inside.y, inside.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        helper.assertTrue(helper.getBlockState(pad).getValue(FlypadBlock.ACTIVE), "a flypad with a player in its field is not shown as active");

        player.snapTo(inside.x, inside.y + PadTiers.flyHeight(1) + 5, inside.z, 0.0F, 0.0F);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        helper.assertFalse(helper.getBlockState(pad).getValue(FlypadBlock.ACTIVE), "an empty flypad is still shown as active");
        helper.succeed();
    }

    /**
     * The edge warning: the margin is the distance to the nearest side or the ceiling of the field,
     * the warning band is 1.5 blocks and the pitch rises from 0.6 at its start to 1.2 at the edge.
     *
     * <p><strong>What breaks this test:</strong> a margin that forgets the ceiling or a side, or a
     * pitch that falls instead of rising as the edge comes closer.
     */
    public static void flypadWarningRisesTowardsTheEdgeOfItsField(GameTestHelper helper) {
        AABB area = PadTiers.flyArea(BlockPos.ZERO, 1);
        helper.assertTrue(Math.abs(FlypadBlockEntity.edgeMargin(area, 0.5, 2.0, 0.5) - 2.0) < 1e-9,
                "margin in the middle of flypad I's field (4 wide, 6 high): " + FlypadBlockEntity.edgeMargin(area, 0.5, 2.0, 0.5));
        helper.assertTrue(Math.abs(FlypadBlockEntity.edgeMargin(area, 0.5, 5.5, 0.5) - 0.5) < 1e-9,
                "margin half a block under the ceiling");
        helper.assertTrue(Math.abs(FlypadBlockEntity.edgeMargin(area, 2.2, 2.0, 0.5) - 0.3) < 1e-9,
                "margin 0.3 blocks from the east side");
        helper.assertTrue(FlypadBlockEntity.edgeMargin(area, 3.0, 2.0, 0.5) < 0, "a point outside has a negative margin");
        helper.assertTrue(Math.abs(FlypadBlockEntity.warningPitch(FlypadBlockEntity.WARNING_MARGIN) - 0.6f) < 1e-6,
                "pitch at the start of the warning band");
        helper.assertTrue(Math.abs(FlypadBlockEntity.warningPitch(0.0) - 1.2f) < 1e-6, "pitch at the edge");
        helper.assertTrue(FlypadBlockEntity.warningPitch(0.5) > FlypadBlockEntity.warningPitch(1.0),
                "the pitch does not rise towards the edge");
        helper.succeed();
    }

    // =====================================================================================
    // Item info tooltips
    // =====================================================================================

    /**
     * Pads, machines, pistons and floating sand name their numbers in the tooltip: tier, charges,
     * area, wait time, speed. The legacy flypad (turns into its new tier) promises nothing.
     *
     * <p><strong>What breaks this test:</strong> a tooltip that shows a different number than the
     * tier really has, or a missing line.
     */
    public static void padAndMachineTooltipsNameTheirNumbers(GameTestHelper helper) {
        expectLines(helper, TweaksBlocks.ENDERITE_LAUNCHPAD, "Tier III of III", "Holds 16 wind charges", "No fall damage until you land");
        expectLines(helper, TweaksBlocks.LAUNCHPAD, "Tier I of III", "Holds 4 wind charges");
        expectLines(helper, TweaksBlocks.NETHERITE_CHUNK_LOADER, "Tier II of III", "Keeps 5 chunks loaded: its own and the 4 beside it");
        expectLines(helper, TweaksBlocks.FINE_ELYTRA_PAD, "Tier V of V", "Area: 128 x 128 blocks, 127 high", "Recharges boosts in the whole area");
        expectLines(helper, TweaksBlocks.ELYTRA_PAD, "Tier I of V", "Area: 1 x 1 blocks, 15 high");
        expectLines(helper, TweaksBlocks.REINFORCED_FLYPAD, "Tier II of III", "Flight area: 8 x 8 blocks, 12 high", "Flying out gives you Slow Falling");
        expectLines(helper, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER, "Tier III of III", "Stand still for 5 s to travel to your bed or respawn anchor (otherwise the spawn)");
        expectLines(helper, TweaksBlocks.POTION_PAD, "Tier I of III", "Effects last up to 30 s (never longer than the potion), then about 60 s of cooldown");
        expectLines(helper, ModBlocks.ENDERITE_FURNACE, "Works 8× as fast", "Double experience");
        expectLines(helper, ModBlocks.REINFORCED_SMOKER, "Works 2× as fast");
        expectLines(helper, ModBlocks.NETHERITE_BLAST_FURNACE, "Works 4× as fast", "+25% output from raw metals");
        expectLines(helper, ModBlocks.REINFORCED_HOPPER, "Moves an item every 4 ticks", "Filter in its menu: exact or type match");
        expectLines(helper, ModBlocks.ENDERITE_HOPPER, "Moves an item every tick");
        expectLines(helper, ModBlocks.REINFORCED_STICKY_PISTON, "Pushes up to 18 blocks");
        expectLines(helper, ModBlocks.LEVITATING_SAND, "Falls upward instead of down");
        expectLines(helper, ModBlocks.SUSPENDED_GRAVEL, "Hangs in mid-air and never falls");
        helper.assertTrue(InfoTooltips.lines(new ItemStack(TweaksBlocks.ENDERITE_FLYPAD)).isEmpty(),
                "the legacy flypad has info lines: " + text(new ItemStack(TweaksBlocks.ENDERITE_FLYPAD)));
        helper.assertFalse(text(new ItemStack(ModBlocks.REINFORCED_FURNACE)).contains("Double experience"),
                "the reinforced furnace claims double experience");
        helper.assertTrue(InfoTooltips.lines(new ItemStack(Blocks.FURNACE)).isEmpty(), "the vanilla furnace got info lines");
        helper.succeed();
    }

    /**
     * Enderite armor explains its void protection, and the mod's apples and carrots list what eating
     * them gives, read from their consumable component like a potion's effects.
     *
     * <p><strong>What breaks this test:</strong> food lines that drift from the real consume effects.
     */
    public static void armorAndFoodTooltipsExplainWhatTheyDo(GameTestHelper helper) {
        expectLines(helper, ModItems.ENDERITE_BOOTS, "Void damage hits less often, more so with each piece",
                "2+ pieces: hold Jump while falling to glide down");
        expectLines(helper, ModItems.NETHERITE_APPLE, "When eaten:", " Fire Resistance (4:00)", " Absorption II (0:30)",
                " Regeneration II (0:10)");
        expectLines(helper, ModItems.ENDERITE_CARROT, "When eaten:", " Night Vision (5:00)", " Speed II (1:00)");
        helper.assertValueEqual(InfoTooltips.clock(20 * 75), "1:15", "clock format of 75 s");
        helper.succeed();
    }

    /**
     * Each core's "Used in" list is exactly the set of results of the recipes (shaped and shapeless crafting
     * and smithing - any slot, the pad tiers I take the core as their template - easter recipes aside) that take that core - checked against the recipes the server
     * loaded, so a new recipe or a moved core shows up here.
     *
     * <p><strong>What breaks this test:</strong> a recipe that takes a core but is missing from the
     * tooltip, or a tooltip entry without a recipe behind it.
     */
    public static void coreTooltipsListExactlyTheRecipesThatTakeTheCore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Map<Item, Set<Item>> found = new LinkedHashMap<>();
        for (Item core : InfoTooltips.coreUses().keySet()) {
            found.put(core, new HashSet<>());
        }
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            if (holder.id().identifier().toString().contains("easter/")) {
                continue;
            }
            Recipe<?> recipe = holder.value();
            for (Item core : found.keySet()) {
                ItemStack result = resultWithCore(level, recipe, core);
                if (result != null && !result.isEmpty()) {
                    found.get(core).add(result.getItem());
                }
            }
        }
        for (Map.Entry<Item, List<Item>> entry : InfoTooltips.coreUses().entrySet()) {
            Set<Item> listed = new HashSet<>(entry.getValue());
            helper.assertValueEqual(listed, found.get(entry.getKey()),
                    "items the tooltip of " + entry.getKey() + " lists vs. the recipes that take it");
        }
        List<String> lines = text(new ItemStack(ModItems.GOLD_CORE));
        helper.assertTrue(lines.contains("Used in:") && lines.contains("  Gold Building Wand, Detector, Octant"),
                "the gold core tooltip reads " + lines);
        helper.succeed();
    }

    // =====================================================================================
    // HUD layout
    // =====================================================================================

    /**
     * The HUD boxes: 0 % sits at the 10 px margin, 100 % at the far margin, 50 % centred; the scale
     * shrinks the room the box needs, and a box larger than the screen stays at the margin.
     *
     * <p><strong>What breaks this test:</strong> percentages applied to the whole screen instead of
     * the free room (the box would leave the screen at 100 %), or a scale that is ignored.
     */
    public static void hudBoxesFollowTheConfiguredPositionAndScale(GameTestHelper helper) {
        int[] leftMiddle = HudLayout.origin(400, 300, 100, 50, 1.0f, 0, 50);
        helper.assertValueEqual(leftMiddle[0], 10, "x of a box at 0 %");
        helper.assertValueEqual(leftMiddle[1], 125, "y of a 50 px box at 50 % of 300 px (the old centred place)");
        int[] bottomRight = HudLayout.origin(400, 300, 100, 50, 1.0f, 100, 100);
        helper.assertValueEqual(bottomRight[0], 290, "x of a box at 100 %: right edge on the margin");
        helper.assertValueEqual(bottomRight[1], 240, "y of a box at 100 %");
        int[] doubled = HudLayout.origin(400, 300, 100, 50, 2.0f, 100, 0);
        helper.assertValueEqual(doubled[0], 190, "x of a doubled box at 100 %");
        helper.assertValueEqual(doubled[1], 10, "y of a box at 0 %");
        int[] huge = HudLayout.origin(400, 300, 1000, 50, 1.0f, 100, 50);
        helper.assertValueEqual(huge[0], 10, "x of a box wider than the screen");
        helper.succeed();
    }

    // =====================================================================================

    private static void expectLines(GameTestHelper helper, ItemLike item, String... expected) {
        List<String> lines = text(new ItemStack(item));
        for (String line : expected) {
            helper.assertTrue(lines.contains(line), item.asItem() + " tooltip lacks \"" + line + "\": " + lines);
        }
    }

    private static List<String> text(ItemStack stack) {
        List<String> out = new ArrayList<>();
        for (Component line : InfoTooltips.lines(stack)) {
            out.add(line.getString());
        }
        return out;
    }

    /** The result of {@code recipe} if it takes {@code core} (one item per slot), else null. */
    private static ItemStack resultWithCore(ServerLevel level, Recipe<?> recipe, Item core) {
        if (recipe instanceof ShapedRecipe shaped) {
            List<Optional<Ingredient>> ingredients = shaped.getIngredients();
            if (ingredients.stream().noneMatch(i -> i.isPresent() && takes(i.get(), core))) {
                return null;
            }
            List<ItemStack> grid = new ArrayList<>();
            for (Optional<Ingredient> ingredient : ingredients) {
                grid.add(ingredient.map(i -> one(i, core)).orElse(ItemStack.EMPTY));
            }
            return shaped.assemble(CraftingInput.of(shaped.getWidth(), shaped.getHeight(), grid));
        }
        if (recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe shapeless) {
            // Formlos (Flypad I seit 2026-09-29: Kern + Platte + Shulkerkopf + Elytra mit Reparatur)
            List<Ingredient> ingredients = shapeless.placementInfo().ingredients();
            if (ingredients.stream().noneMatch(i -> takes(i, core))) {
                return null;
            }
            List<ItemStack> row = new ArrayList<>();
            for (Ingredient ingredient : ingredients) {
                row.add(one(ingredient, core));
            }
            return shapeless.assemble(CraftingInput.of(row.size(), 1, row));
        }
        if (recipe instanceof SmithingRecipe smithing) {
            boolean base = takes(smithing.baseIngredient(), core);
            boolean addition = smithing.additionIngredient().map(i -> takes(i, core)).orElse(false);
            // Stufe I der Pads (seit 2026-09-29): der Kern liegt im Vorlagen-Feld
            boolean template = smithing.templateIngredient().map(i -> takes(i, core)).orElse(false);
            if (!base && !addition && !template) {
                return null;
            }
            SmithingRecipeInput input = new SmithingRecipeInput(
                    smithing.templateIngredient().map(i -> one(i, core)).orElse(ItemStack.EMPTY),
                    one(smithing.baseIngredient(), core),
                    smithing.additionIngredient().map(i -> one(i, core)).orElse(ItemStack.EMPTY));
            return smithing.matches(input, level) ? smithing.assemble(input) : null;
        }
        return null;
    }

    private static boolean takes(Ingredient ingredient, Item core) {
        return ingredient.items().anyMatch(h -> h.value() == core);
    }

    private static ItemStack one(Ingredient ingredient, Item core) {
        Optional<Holder<Item>> match = ingredient.items().filter(h -> h.value() == core).findFirst();
        return new ItemStack(match.orElseGet(() -> ingredient.items().findFirst().orElseThrow()));
    }
}
