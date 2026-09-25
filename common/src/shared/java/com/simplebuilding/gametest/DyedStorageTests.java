package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.BackpackBlock;
import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.component.BackpackContents;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.screen.BackpackMenuProviders;
import com.simplebuilding.screen.BackpackOpenData;
import com.simplebuilding.util.DyedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dyed backpacks and bundles: the four backpacks and the three mod bundles take the vanilla
 * {@code minecraft:dyed_color} component the way leather armour does (see {@link DyedStorage}).
 *
 * <p>Covered here: the dye recipe for all seven items (colour, re-dyeing, and every other
 * component - contents, name, enchantments - untouched), the water cauldron that washes the
 * colour off again and nothing else, and the colour reaching the two places the client tints
 * by it: the backpack menu's open data (worn and placed) and the bundle tooltip. The placed
 * round trip keeps the colour as well.
 *
 * <p>Not covered: the item model tint, the worn backpack layer and the tinted slots themselves.
 * Those are client rendering; this class checks what the client is handed.
 */
public final class DyedStorageTests {
    private DyedStorageTests() {
    }

    /** The seven dyeable items, backpacks first. */
    private static final Item[] DYEABLE = {ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK,
            ModItems.ENDERITE_BACKPACK, ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE, ModItems.ENDERITE_BUNDLE};

    /**
     * Each of the seven items plus red dye, resolved through the live recipe manager the way a
     * crafting table resolves it: the result is the same item, coloured exactly red, and its
     * component patch is the original's plus {@code dyed_color} - contents, custom name and the
     * Funnel enchantment all still there. A second pass with blue dye on the red result mixes
     * the colours (re-dyeing works like leather) and again leaves the rest alone.
     *
     * <p>The negative: a quiver - a {@code ReinforcedBundleItem} as well - plus dye crafts
     * nothing, so the dyeing is bound to the seven items and not to the class.
     *
     * <p>What breaks this test: a missing dye recipe (26.2: {@code <id>_dyed}; 1.21.11: an item
     * missing from {@code minecraft:dyeable}), a recipe that builds a fresh stack instead of
     * carrying the components over, or one that is too wide and takes the quiver.
     */
    public static void dyeingColoursEveryBackpackAndBundleAndKeepsItsComponents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int red = DyeRgb.of(DyeColor.RED);
        int blue = DyeRgb.of(DyeColor.BLUE);
        List<String> problems = new ArrayList<>();

        for (Item item : DYEABLE) {
            ItemStack original = filled(helper, item);
            ItemStack redStack = craft(helper, level, original, DyeColor.RED, problems);
            if (redStack == null) {
                continue;
            }
            if (!redStack.is(item)) {
                problems.add(item + " dyed red turned into " + redStack.getItem());
                continue;
            }
            if (DyedStorage.colour(redStack) != red) {
                problems.add(item + " dyed red carries the colour " + Integer.toHexString(DyedStorage.colour(redStack))
                        + " instead of " + Integer.toHexString(red));
            }
            ItemStack expected = original.copy();
            expected.set(DataComponents.DYED_COLOR, new DyedItemColor(red));
            if (!redStack.getComponentsPatch().equals(expected.getComponentsPatch())) {
                problems.add(item + " lost or changed components in the dye recipe: " + redStack.getComponentsPatch()
                        + " instead of " + expected.getComponentsPatch());
            }

            ItemStack mixed = craft(helper, level, redStack, DyeColor.BLUE, problems);
            if (mixed == null) {
                continue;
            }
            int mixedColour = DyedStorage.colour(mixed);
            if (mixedColour == DyedStorage.UNDYED || mixedColour == red || mixedColour == blue) {
                problems.add(item + " dyed red and then blue carries " + Integer.toHexString(mixedColour)
                        + "; the colours are supposed to mix like leather");
            }
            ItemStack expectedMixed = original.copy();
            expectedMixed.set(DataComponents.DYED_COLOR, new DyedItemColor(mixedColour));
            if (!mixed.getComponentsPatch().equals(expectedMixed.getComponentsPatch())) {
                problems.add(item + " lost components when it was dyed a second time: " + mixed.getComponentsPatch());
            }
        }
        helper.assertTrue(problems.isEmpty(), "dyeing problems: " + problems);

        CraftingInput quiver = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.QUIVER), dye(DyeColor.RED)));
        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, quiver, level).isEmpty(),
                "a quiver plus red dye crafts something; only the four backpacks and three bundles are dyeable");
        helper.succeed();
    }

    /**
     * A dyed backpack or bundle dipped into a water cauldron comes out as the same item with
     * its colour gone and every other component where it was; the cauldron loses one level and
     * the statistic counts the wash like leather armour. An undyed one is not an interaction at
     * all: the cauldron answers the way it answers an item it does not know, and keeps its water.
     *
     * <p>What breaks this test: one of the seven not reaching the wash (26.2: missing from
     * {@code minecraft:cauldron_can_remove_dye}; 1.21.11: not registered in
     * {@code DyedStorageWashing}), a wash that rebuilds the stack (contents or name gone), a
     * wash that does not lower the water, and one that also takes water for an undyed item.
     */
    public static void waterCauldronWashesOnlyTheDyeOffBackpacksAndBundles(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockPos cauldron = new BlockPos(2, 1, 2);
        int washesBefore = washCount(player);
        List<String> problems = new ArrayList<>();

        for (Item item : DYEABLE) {
            fillCauldron(helper, cauldron, 3);
            ItemStack clean = filled(helper, item);
            ItemStack dyed = clean.copy();
            dyed.set(DataComponents.DYED_COLOR, new DyedItemColor(DyeRgb.of(DyeColor.LIME)));
            player.setItemInHand(InteractionHand.MAIN_HAND, dyed);

            InteractionResult washed = useBlockWithHeldItem(helper, player, cauldron);
            if (washed != InteractionResult.SUCCESS) {
                problems.add(item + " answered " + washed + " instead of SUCCESS, so the cauldron does not wash it");
                continue;
            }
            ItemStack afterwards = player.getMainHandItem();
            if (!afterwards.is(item) || !afterwards.getComponentsPatch().equals(clean.getComponentsPatch())) {
                problems.add(item + " came out of the wash as " + afterwards + " with " + afterwards.getComponentsPatch()
                        + " instead of the undyed original " + clean.getComponentsPatch());
            }
            if (waterLevel(helper, cauldron) != 2) {
                problems.add("washing " + item + " left the cauldron at level " + waterLevel(helper, cauldron) + " instead of 2");
            }
        }
        helper.assertTrue(problems.isEmpty(), "cauldron wash problems: " + problems);
        helper.assertValueEqual(washCount(player) - washesBefore, DYEABLE.length,
                "washes the statistics counted for " + DYEABLE.length + " dyed items");

        fillCauldron(helper, cauldron, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, filled(helper, ModItems.NETHERITE_BACKPACK));
        InteractionResult undyed = useBlockWithHeldItem(helper, player, cauldron);
        helper.assertTrue(undyed != InteractionResult.SUCCESS,
                "an undyed netherite backpack was washed (" + undyed + "); there is no colour to take off");
        helper.assertValueEqual(waterLevel(helper, cauldron), 3, "water level after dipping an undyed backpack");
        helper.succeed();
    }

    /**
     * The colour reaches what the client tints by. The worn backpack's menu data carries the
     * colour (the screen tints the backpack rows with it), an undyed one carries
     * {@link DyedStorage#UNDYED}; a dyed backpack set down as a block keeps the colour in its
     * block entity, hands it to the placed menu, and drops with it again. The bundle's tooltip
     * image carries the colour for the slot tint. The two tints themselves stay subtle: the slot
     * fill is the colour at alpha 0x30, the tooltip slot sprite is multiplied with white pulled
     * 35 % towards the colour, and both leave an undyed item exactly as before.
     *
     * <p>What breaks this test: the open data built without the colour (worn or placed), a
     * placed backpack whose block entity or loot table drops {@code dyed_color}, a dyed backpack set down in
     * the undyed block state (or an undyed one in the dyed state), a client update tag without the colour or with
     * the contents, a tooltip image
     * built without the colour, or a tint that got louder or touches undyed items.
     */
    public static void theDyeColourReachesTheBackpackMenuAndTheBundleTooltip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);
        int purple = DyeRgb.of(DyeColor.PURPLE);

        // --- worn ---
        ItemStack worn = filled(helper, ModItems.NETHERITE_BACKPACK);
        player.setItemSlot(EquipmentSlot.CHEST, worn);
        helper.assertValueEqual(BackpackMenuProviders.worn(player).data().dyeColor(), DyedStorage.UNDYED,
                "colour the menu of an undyed worn backpack hands the client");
        worn.set(DataComponents.DYED_COLOR, new DyedItemColor(purple));
        helper.assertValueEqual(BackpackMenuProviders.worn(player).data().dyeColor(), purple,
                "colour the menu of a purple worn backpack hands the client");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

        // --- an undyed backpack sets down in the undyed state ---
        BlockPos plainSupport = new BlockPos(4, 1, 1);
        helper.setBlock(plainSupport, Blocks.STONE);
        helper.setBlock(plainSupport.above(), Blocks.AIR);
        ItemStack plain = filled(helper, ModItems.BACKPACK);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        player.setShiftKeyDown(true);
        plain.useOn(clickTop(helper, player, plainSupport));
        player.setShiftKeyDown(false);
        helper.assertTrue(helper.getBlockState(plainSupport.above()).is(ModBlocks.BACKPACK)
                        && !helper.getBlockState(plainSupport.above()).getValue(BackpackBlock.DYED),
                "an undyed backpack was set down as " + helper.getBlockState(plainSupport.above()));
        helper.setBlock(plainSupport.above(), Blocks.AIR);

        // --- placed and broken again ---
        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos placed = support.above();
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(placed, Blocks.AIR);
        ItemStack backpack = filled(helper, ModItems.REINFORCED_BACKPACK);
        backpack.set(DataComponents.DYED_COLOR, new DyedItemColor(purple));
        player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
        player.setShiftKeyDown(true);
        backpack.useOn(clickTop(helper, player, support));
        player.setShiftKeyDown(false);
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(placed)) instanceof BackpackBlockEntity,
                "sneak + right click did not set the dyed backpack down, found " + helper.getBlockState(placed));
        BackpackBlockEntity entity = (BackpackBlockEntity) level.getBlockEntity(helper.absolutePos(placed));
        BackpackOpenData placedData = BackpackMenuProviders.placed(entity).data();
        helper.assertValueEqual(placedData.dyeColor(), purple, "colour the menu of a purple placed backpack hands the client");
        helper.assertTrue(helper.getBlockState(placed).getValue(BackpackBlock.DYED),
                "the placed purple backpack is not in its dyed block state, so it would render in its tier's look");
        helper.assertValueEqual(entity.dyeColor(), purple, "colour the placed purple backpack's block entity holds");
        CompoundTag update = entity.getUpdateTag(level.registryAccess());
        helper.assertValueEqual(update.getIntOr("Color", DyedStorage.UNDYED), purple,
                "colour in the update tag the client gets for the placed purple backpack");
        helper.assertTrue(update.keySet().equals(java.util.Set.of("Color")),
                "the client update tag of a placed backpack carries " + update.keySet() + "; it needs the colour and nothing else");

        level.destroyBlock(helper.absolutePos(placed), true);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds());
        helper.assertValueEqual(drops.size(), 1, "item entities after breaking the placed dyed backpack");
        ItemStack dropped = drops.get(0).getItem();
        drops.get(0).discard();
        helper.assertTrue(dropped.is(ModItems.REINFORCED_BACKPACK) && DyedStorage.colour(dropped) == purple,
                "the broken dyed backpack dropped " + dropped + " with colour " + Integer.toHexString(DyedStorage.colour(dropped)));

        // --- bundle tooltip ---
        ItemStack bundle = filled(helper, ModItems.NETHERITE_BUNDLE);
        helper.assertValueEqual(tooltipColour(helper, bundle), DyedStorage.UNDYED, "tooltip colour of an undyed netherite bundle");
        bundle.set(DataComponents.DYED_COLOR, new DyedItemColor(purple));
        helper.assertValueEqual(tooltipColour(helper, bundle), purple, "tooltip colour of a purple netherite bundle");

        // --- the tints stay subtle and leave undyed items alone ---
        helper.assertValueEqual(DyedStorage.slotTint(purple, 0x1CA0602A), 0x30000000 | purple, "slot fill for purple");
        helper.assertValueEqual(DyedStorage.slotTint(DyedStorage.UNDYED, 0x1CA0602A), 0x1CA0602A, "slot fill without dye");
        helper.assertValueEqual(DyedStorage.spriteTint(0x000000), 0xFFA6A6A6, "tooltip slot multiplier for black");
        helper.assertValueEqual(DyedStorage.spriteTint(DyedStorage.UNDYED), -1, "tooltip slot multiplier without dye");
        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /** The item with contents, a custom name and Funnel I - everything dyeing must not touch. */
    private static ItemStack filled(GameTestHelper helper, Item item) {
        ItemStack stack = new ItemStack(item);
        if (item == ModItems.REINFORCED_BUNDLE || item == ModItems.NETHERITE_BUNDLE || item == ModItems.ENDERITE_BUNDLE) {
            stack.set(DataComponents.BUNDLE_CONTENTS, bundleContents(new ItemStack(Items.COBBLESTONE, 16)));
        } else {
            stack.set(ModDataComponentTypes.BACKPACK_CONTENTS, new BackpackContents(List.of(
                    BackpackContents.Entry.of(0, new ItemStack(Items.COBBLESTONE, 64)),
                    BackpackContents.Entry.of(1, new ItemStack(Items.DIAMOND, 3)))));
        }
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Kit"));
        stack.enchant(enchantment(helper, ModEnchantments.FUNNEL), 1);
        return stack;
    }

    /** Bundle contents; the list holds templates on this line. */
    private static BundleContents bundleContents(ItemStack stack) {
        return new BundleContents(List.of(ItemStackTemplate.fromNonEmptyStack(stack)));
    }

    /** A dye of the given colour; the sixteen dyes are one ColorCollection on this line. */
    private static ItemStack dye(DyeColor color) {
        return new ItemStack(Items.DYE.pick(color));
    }

    /** The dye recipe that matches {@code item}: one crafting_dye recipe per item on this line. */
    private static String dyeRecipeId(Item item) {
        return "simplebuilding:" + BuiltInRegistries.ITEM.getKey(item).getPath() + "_dyed";
    }

    /** Item plus one dye through the live recipe manager; null (and a problem) if nothing matches. */
    private static ItemStack craft(GameTestHelper helper, ServerLevel level, ItemStack target, DyeColor color, List<String> problems) {
        CraftingInput grid = CraftingInput.of(2, 1, List.of(target.copy(), dye(color)));
        Optional<RecipeHolder<CraftingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        if (match.isEmpty()) {
            problems.add(target.getItem() + " plus " + color.getName() + " dye crafts nothing");
            return null;
        }
        String id = match.get().id().identifier().toString();
        if (!id.equals(dyeRecipeId(target.getItem()))) {
            problems.add(target.getItem() + " plus " + color.getName() + " dye matched " + id);
        }
        return match.get().value().assemble(grid);
    }

    private static int tooltipColour(GameTestHelper helper, ItemStack bundle) {
        Optional<TooltipComponent> image = bundle.getItem().getTooltipImage(bundle);
        helper.assertTrue(image.isPresent() && image.get() instanceof ReinforcedBundleTooltipData,
                "the tooltip of " + bundle + " is " + image + " instead of a ReinforcedBundleTooltipData");
        return ((ReinforcedBundleTooltipData) image.get()).dyeColor();
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** A click on the middle of the top face of {@code support}. */
    private static UseOnContext clickTop(GameTestHelper helper, ServerPlayer player, BlockPos support) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, topFaceHit(helper, support));
    }

    private static BlockHitResult topFaceHit(GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolute = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        return new BlockHitResult(hit, Direction.UP, absolute, false);
    }

    /** Right click on the block side, where a cauldron interaction sits. */
    private static InteractionResult useBlockWithHeldItem(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
        ServerLevel level = helper.getLevel();
        return level.getBlockState(helper.absolutePos(relativePos)).useItemOn(
                player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, topFaceHit(helper, relativePos));
    }

    private static void fillCauldron(GameTestHelper helper, BlockPos relativePos, int level) {
        helper.setBlock(relativePos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level));
    }

    private static int waterLevel(GameTestHelper helper, BlockPos relativePos) {
        BlockState state = helper.getBlockState(relativePos);
        helper.assertTrue(state.is(Blocks.WATER_CAULDRON), "the cauldron is " + state + " instead of a water cauldron");
        return state.getValue(LayeredCauldronBlock.LEVEL);
    }

    private static int washCount(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.CLEAN_ARMOR));
    }
}
