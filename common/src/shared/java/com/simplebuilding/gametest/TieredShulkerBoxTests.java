package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.TieredShulkerBoxBlock;
import com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.util.TieredShulkerBoxes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The shulker box tiers: vanilla shulker box (27, any color) -&gt; Reinforced (36) -&gt; Netherite
 * (45, stacks x2) -&gt; Enderite (54, stacks x4), climbed in the world with the sledgehammer like
 * the chests, but dearer: ten blows and two pieces of material per step.
 *
 * <p>The numbers are this file's own constants, not read from the mod (a test that read
 * {@code TieredShulkerBoxes#SHULKER_UPGRADE_MATERIAL_COST} would follow every edit of it). The
 * upgrades drive the real click path ({@code ServerPlayerGameMode#useItemOn}: the box is asked
 * first, a box that opened its menu instead would stop the hammer) and the player tick, like
 * {@code TieredChestTests}.
 */
public final class TieredShulkerBoxTests {

    private TieredShulkerBoxTests() {
    }

    /** Tick budget of the rigs that wait for a hopper or a dispenser. */
    public static final int RIG_MAX_TICKS = 200;

    /** Ten blows, one a second: the shulker upgrade takes twice as long as a chest's. */
    private static final int SHULKER_UPGRADE_TICKS = 200;
    private static final int CHEST_UPGRADE_TICKS = 100;
    private static final int MATERIAL_PER_STEP = 2;
    private static final int VANILLA_SLOTS = 27;
    private static final int REINFORCED_SLOTS = 36;
    private static final int NETHERITE_SLOTS = 45;
    private static final int ENDERITE_SLOTS = 54;

    // =====================================================================================
    // UPGRADES
    // =====================================================================================

    /**
     * A red vanilla shulker box with a name and items in its first and last slot climbs to
     * reinforced (stone hammer, cracked diamonds), netherite (diamond hammer, netherite nuggets) and
     * enderite (netherite hammer, enderite nuggets). Every step: one nugget short and the hammer does
     * not start; after the chest's five seconds the box is still the old tier, after ten it is the
     * next one - same facing, same items in the same slots, same name, still red, exactly two pieces
     * of material gone and the tier's slot count.
     */
    public static void vanillaBoxClimbsToEnderiteInTenBlowsKeepingContentsAndColor(GameTestHelper helper) {
        ServerPlayer player = smith(helper);
        BlockPos pos = new BlockPos(1, 1, 1);
        Block red = vanillaBlock("red_shulker_box");
        helper.setBlock(pos, red.defaultBlockState().setValue(ShulkerBoxBlock.FACING, Direction.EAST));
        BaseContainerBlockEntity start = container(helper, pos);
        start.applyComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Loot")).build(),
                DataComponentPatch.EMPTY);
        start.setItem(0, new ItemStack(Items.DIAMOND, 5));
        start.setItem(VANILLA_SLOTS - 1, new ItemStack(Items.OAK_LOG, 64));

        climb(helper, player, pos, ModItems.STONE_SLEDGEHAMMER, ModItems.CRACKED_DIAMOND, red, ModBlocks.REINFORCED_SHULKER_BOX,
                REINFORCED_SLOTS, "vanilla -> reinforced");
        climb(helper, player, pos, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, ModBlocks.REINFORCED_SHULKER_BOX,
                ModBlocks.NETHERITE_SHULKER_BOX, NETHERITE_SLOTS, "reinforced -> netherite");
        climb(helper, player, pos, ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_NUGGET, ModBlocks.NETHERITE_SHULKER_BOX,
                ModBlocks.ENDERITE_SHULKER_BOX, ENDERITE_SLOTS, "netherite -> enderite");

        TieredShulkerBoxBlockEntity end = box(helper, pos);
        String contents = describe(end.getItem(0)) + "|" + describe(end.getItem(VANILLA_SLOTS - 1));
        helper.assertTrue(contents.equals("5 diamond|64 oak_log"), "contents after three upgrades: " + contents);
        helper.assertTrue(end.getCustomName() != null && end.getCustomName().getString().equals("Loot"),
                "the name was lost on the way: " + end.getCustomName());
        helper.assertTrue(end.getColor() == DyeColor.RED, "the color was lost on the way: " + end.getColor());
        helper.assertTrue(helper.getBlockState(pos).getValue(ShulkerBoxBlock.FACING) == Direction.EAST, "the facing was lost");
        helper.succeed();
    }

    // =====================================================================================
    // CONTENTS
    // =====================================================================================

    /**
     * A blue, named enderite box with a slot of 256 cobblestone breaks into one item that carries all
     * of it: {@code minecraft:container} holds a readable 64, {@code simplebuilding:container_counts}
     * the 256, and the color rides along as {@code minecraft:base_color}. Placed again, the box has
     * the 256, the pearls, the name and the color back. And when that item burns, the real 256 spill
     * out as four stacks of 64, not just the 64 that minecraft:container holds.
     */
    public static void contentsAndOversizedStacksSurviveBreakingPlacingAndBurning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ENDERITE_SHULKER_BOX.defaultBlockState());
        TieredShulkerBoxBlockEntity box = box(helper, pos);
        box.applyComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Quarry"))
                .set(DataComponents.BASE_COLOR, DyeColor.BLUE).build(), DataComponentPatch.EMPTY);
        box.setItem(5, new ItemStack(Items.COBBLESTONE, 256));
        box.setItem(6, new ItemStack(Items.ENDER_PEARL, 40));

        List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), level, helper.absolutePos(pos), box);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.ENDERITE_SHULKER_BOX), "the broken box drops " + drops);
        ItemStack dropped = drops.get(0);
        net.minecraft.world.item.component.ItemContainerContents vanillaView = dropped.get(DataComponents.CONTAINER);
        helper.assertTrue(vanillaView != null, "the dropped box carries no minecraft:container");
        net.minecraft.core.NonNullList<ItemStack> readable = net.minecraft.core.NonNullList.withSize(ENDERITE_SLOTS, ItemStack.EMPTY);
        vanillaView.copyInto(readable);
        String item = describe(readable.get(5)) + "|" + describe(TieredShulkerBoxes.contentsOf(dropped).get(5)) + "|"
                + describe(TieredShulkerBoxes.contentsOf(dropped).get(6)) + "|" + dropped.get(DataComponents.BASE_COLOR)
                + "|" + dropped.getHoverName().getString();
        helper.assertTrue(item.equals("64 cobblestone|256 cobblestone|40 ender_pearl|blue|Quarry"),
                "the dropped item (vanilla view | real | pearls | color | name): " + item);
        helper.assertTrue(dropped.get(ModDataComponentTypes.CONTAINER_COUNTS) != null, "no container_counts on the dropped box");

        // Placed again, the way a player places it.
        helper.setBlock(pos, Blocks.AIR);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        BlockPos other = new BlockPos(3, 1, 1);
        place(helper, player, dropped.copy(), other);
        TieredShulkerBoxBlockEntity placed = box(helper, other);
        String back = describe(placed.getItem(5)) + "|" + describe(placed.getItem(6)) + "|" + placed.getColor()
                + "|" + (placed.getCustomName() == null ? "unnamed" : placed.getCustomName().getString());
        helper.assertTrue(back.equals("256 cobblestone|40 ender_pearl|blue|Quarry"), "the box placed from the item: " + back);

        // The item burns (lava, cactus, explosion): the real counts spill, as normal stacks.
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2.0, 4.5));
        ItemEntity burning = new ItemEntity(level, at.x, at.y, at.z, dropped.copy());
        level.addFreshEntity(burning);
        burning.getItem().getItem().onDestroyed(burning);
        int cobblestone = 0;
        int biggest = 0;
        for (ItemEntity spilled : level.getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (spilled.getItem().is(Items.COBBLESTONE)) {
                cobblestone += spilled.getItem().getCount();
                biggest = Math.max(biggest, spilled.getItem().getCount());
            }
        }
        helper.assertTrue(cobblestone == 256 && biggest == 64,
                "the burnt box spilled " + cobblestone + " cobblestone in stacks of at most " + biggest + ", expected 256 in 64s");
        helper.succeed();
    }

    /**
     * A netherite box saves a slot of 128 (vanilla stores at most a normal stack) and its color and
     * loads both back whole; the vanilla "Items" list alone still holds a readable stack of 64.
     */
    public static void oversizedStacksAndColorSurviveSavingAndLoading(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.NETHERITE_SHULKER_BOX.defaultBlockState());
        TieredShulkerBoxBlockEntity box = box(helper, pos);
        box.setColor(DyeColor.LIME);
        box.setItem(NETHERITE_SLOTS - 1, new ItemStack(Items.COBBLESTONE, 128));
        CompoundTag tag = box.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(tag.toString().contains("\"count\":64") || tag.toString().contains("count:64"),
                "the vanilla item list does not hold the capped stack of 64: " + tag);
        box.clearContent();
        box.setColor(null);
        box.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        String loaded = describe(box.getItem(NETHERITE_SLOTS - 1)) + "|" + box.getColor();
        helper.assertTrue(loaded.equals("128 cobblestone|lime"), "loaded back: " + loaded);
        helper.succeed();
    }

    // =====================================================================================
    // AUTOMATION
    // =====================================================================================

    /**
     * Hoppers fill up to the tier limit and never put a shulker box into a shulker box: vanilla's
     * transfer tops a netherite slot of 127 up to 128 and moves on, refuses a vanilla shulker box and
     * a reinforced one, and a real hopper pushes into an enderite box whose every slot holds 64
     * (vanilla would call it full) while another pulls out of a slot of 120 below. The comparator
     * reads the box against its own limit: every slot at 64 of 128 is 8, at 128 it is 15.
     */
    public static void hoppersAndComparatorsFollowTheTierLimit(GameTestHelper helper) {
        BlockPos netherite = new BlockPos(1, 1, 1);
        helper.setBlock(netherite, ModBlocks.NETHERITE_SHULKER_BOX.defaultBlockState());
        Container single = container(helper, netherite);
        single.setItem(0, new ItemStack(Items.COBBLESTONE, 127));
        ItemStack rest = HopperBlockEntity.addItem(null, single, new ItemStack(Items.COBBLESTONE, 2), Direction.UP);
        String line = single.getItem(0).getCount() + "+" + single.getItem(1).getCount() + " rest " + rest.getCount();
        helper.assertTrue(line.equals("128+1 rest 0"), "netherite slot 0 at 127 plus 2 cobblestone: " + line);
        ItemStack vanillaBox = HopperBlockEntity.addItem(null, single, new ItemStack(Items.SHULKER_BOX), Direction.UP);
        ItemStack tierBox = HopperBlockEntity.addItem(null, single, new ItemStack(ModItems.REINFORCED_SHULKER_BOX), Direction.UP);
        helper.assertTrue(vanillaBox.getCount() == 1 && tierBox.getCount() == 1,
                "a hopper put a shulker box into the netherite box (vanilla rest " + vanillaBox.getCount()
                        + ", reinforced rest " + tierBox.getCount() + ")");

        for (int i = 0; i < NETHERITE_SLOTS; i++) {
            single.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        BlockState state = helper.getBlockState(netherite);
        int half = state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(netherite), Direction.NORTH);
        for (int i = 0; i < NETHERITE_SLOTS; i++) {
            single.setItem(i, new ItemStack(Items.COBBLESTONE, 128));
        }
        int full = state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(netherite), Direction.NORTH);
        helper.assertTrue(half == 8 && full == 15, "comparator signal half/full: " + half + "/" + full + ", expected 8/15");

        BlockPos fed = new BlockPos(4, 1, 1);
        helper.setBlock(fed, ModBlocks.ENDERITE_SHULKER_BOX.defaultBlockState());
        Container fedBox = container(helper, fed);
        for (int i = 0; i < ENDERITE_SLOTS; i++) {
            fedBox.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        helper.setBlock(fed.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((Container) helper.getLevel().getBlockEntity(helper.absolutePos(fed.above()))).setItem(0, new ItemStack(Items.COBBLESTONE, 2));

        BlockPos drained = new BlockPos(1, 2, 4);
        helper.setBlock(drained, ModBlocks.REINFORCED_SHULKER_BOX.defaultBlockState());
        container(helper, drained).setItem(0, new ItemStack(Items.COBBLESTONE, 60));
        helper.setBlock(drained.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));

        helper.succeedWhen(() -> {
            int total = 0;
            for (int i = 0; i < ENDERITE_SLOTS; i++) {
                total += fedBox.getItem(i).getCount();
            }
            helper.assertTrue(total == ENDERITE_SLOTS * 64 + 2 && fedBox.getItem(0).getCount() == 66,
                    "the hopper did not push its 2 cobblestone into the enderite box full of 64s (total " + total
                            + ", slot 0 " + fedBox.getItem(0).getCount() + ")");
            Container hopper = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(drained.below()));
            int pulled = hopper.getItem(0).getCount();
            int remaining = container(helper, drained).getItem(0).getCount();
            helper.assertTrue(pulled >= 2 && pulled + remaining == 60,
                    "the hopper below pulled " + pulled + ", the slot of 60 holds " + remaining);
        });
    }

    /** A powered dispenser sets a green reinforced box down in front of it, color and contents included. */
    public static void dispensersPlaceTheBoxes(GameTestHelper helper) {
        BlockPos dispenser = new BlockPos(1, 1, 1);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));
        ItemStack green = new ItemStack(ModItems.REINFORCED_SHULKER_BOX);
        green.set(DataComponents.BASE_COLOR, DyeColor.GREEN);
        ((Container) helper.getLevel().getBlockEntity(helper.absolutePos(dispenser))).setItem(0, green);
        helper.setBlock(dispenser.east(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            BlockState above = helper.getBlockState(dispenser.above());
            helper.assertTrue(above.is(ModBlocks.REINFORCED_SHULKER_BOX), "the dispenser did not place the box: " + above);
            helper.assertTrue(box(helper, dispenser.above()).getColor() == DyeColor.GREEN, "the placed box lost its color");
        });
    }

    // =====================================================================================
    // NO NESTING, DYEING, WASHING
    // =====================================================================================

    /**
     * No shulker box goes into a tier box (menu slot, hopper face, {@code canPlaceItem}), and no tier
     * box goes into a vanilla shulker box, a bundle or a backpack (tag {@code not_allowed_in_backpack}).
     * Ordinary items do.
     */
    public static void boxesDoNotNest(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.REINFORCED_SHULKER_BOX.defaultBlockState());
        TieredShulkerBoxBlockEntity box = box(helper, pos);
        TieredChestMenu menu = TieredChestMenu.server(0,
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory(), box, box.tier(), false, true);
        ShulkerBoxSlot vanillaSlot = new ShulkerBoxSlot(new net.minecraft.world.SimpleContainer(27), 0, 0, 0);
        List<String> summary = new ArrayList<>();
        for (Item item : List.of(Items.COBBLESTONE, Items.SHULKER_BOX, vanillaBlock("red_shulker_box").asItem(), ModItems.REINFORCED_SHULKER_BOX,
                ModItems.NETHERITE_SHULKER_BOX, ModItems.ENDERITE_SHULKER_BOX)) {
            ItemStack stack = new ItemStack(item);
            summary.add(BuiltInRegistries.ITEM.getKey(item).getPath()
                    + ":" + (menu.getSlot(0).mayPlace(stack) ? "menu" : "-")
                    + (box.canPlaceItemThroughFace(0, stack, Direction.UP) ? "+hopper" : "")
                    + (box.canPlaceItem(0, stack) ? "+place" : "")
                    + (vanillaSlot.mayPlace(stack) ? "+vanilla" : "")
                    + (stack.getItem().canFitInsideContainerItems() ? "+bundle" : "")
                    + (stack.is(ModTags.Items.NOT_ALLOWED_IN_BACKPACK) ? "+nobackpack" : ""));
        }
        String expected = "[cobblestone:menu+hopper+place+vanilla+bundle, shulker_box:-, red_shulker_box:-, "
                + "reinforced_shulker_box:-+nobackpack, netherite_shulker_box:-+nobackpack, enderite_shulker_box:-+nobackpack]";
        helper.assertTrue(summary.toString().equals(expected), "nesting: expected " + expected + " but was " + summary);
        helper.succeed();
    }

    /**
     * Dyeing and the crafting recipe: a reinforced box with items and red dye becomes a red one with
     * the items; a red vanilla shulker box with four cracked diamonds becomes a red reinforced box
     * (three are not enough); a dyed box washed in a water cauldron loses its color and the cauldron
     * one level of water.
     */
    public static void boxesAreDyedCraftedAndWashed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack filled = new ItemStack(ModItems.REINFORCED_SHULKER_BOX);
        filled.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(
                List.of(new ItemStack(Items.DIAMOND, 3))));
        ItemStack dyed = craft(helper, level, CraftingInput.of(2, 1, List.of(filled, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_dye"))))),
                "reinforced box + red dye", "simplebuilding:red_reinforced_shulker_box");
        helper.assertTrue(dyed.is(ModItems.REINFORCED_SHULKER_BOX) && dyed.get(DataComponents.BASE_COLOR) == DyeColor.RED
                        && describe(TieredShulkerBoxes.contentsOf(dyed).get(0)).equals("3 diamond"),
                "dyed: " + dyed + " " + dyed.getComponentsPatch());

        ItemStack diamond = new ItemStack(ModItems.CRACKED_DIAMOND);
        ItemStack redVanilla = new ItemStack(vanillaBlock("red_shulker_box").asItem());
        ItemStack crafted = craft(helper, level, CraftingInput.of(3, 2, List.of(redVanilla, diamond, diamond, diamond, diamond, ItemStack.EMPTY)),
                "red shulker box + 4 cracked diamonds", "simplebuilding:reinforced_shulker_box_from_red_shulker_box");
        helper.assertTrue(crafted.is(ModItems.REINFORCED_SHULKER_BOX) && crafted.get(DataComponents.BASE_COLOR) == DyeColor.RED,
                "crafted: " + crafted + " " + crafted.getComponentsPatch());
        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                        CraftingInput.of(2, 2, List.of(redVanilla, diamond, diamond, diamond)), level).isEmpty(),
                "three cracked diamonds already make a reinforced shulker box");

        BlockPos cauldron = new BlockPos(1, 1, 1);
        // A survival hand: in creative the washed box would go to the inventory and the dyed one stay.
        helper.setBlock(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, dyed);
        net.minecraft.core.cauldron.CauldronInteraction wash = net.minecraft.core.cauldron.CauldronInteractions.WATER.get(dyed);
        wash.interact(helper.getBlockState(cauldron), level, helper.absolutePos(cauldron), player, InteractionHand.MAIN_HAND, dyed);
        ItemStack washed = player.getMainHandItem();
        int water = helper.getBlockState(cauldron).getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL);
        helper.assertTrue(washed.is(ModItems.REINFORCED_SHULKER_BOX) && !washed.has(DataComponents.BASE_COLOR)
                        && describe(TieredShulkerBoxes.contentsOf(washed).get(0)).equals("3 diamond") && water == 2,
                "washed: " + washed.getComponentsPatch() + ", water level " + water);
        helper.succeed();
    }

    // =====================================================================================
    // ITEMS
    // =====================================================================================

    /**
     * Rarity COMMON/UNCOMMON/EPIC (docs/RARITAETEN.md), netherite and enderite fire resistant, stack
     * size 1, the right block, and the enderite box in the void-protected (and double despawn) tag.
     */
    public static void boxItemsFollowTheFamilyScheme(GameTestHelper helper) {
        List<String> summary = new ArrayList<>();
        for (Item item : List.of(ModItems.REINFORCED_SHULKER_BOX, ModItems.NETHERITE_SHULKER_BOX, ModItems.ENDERITE_SHULKER_BOX)) {
            ItemStack stack = new ItemStack(item);
            summary.add(BuiltInRegistries.ITEM.getKey(item).getPath() + ":" + stack.getRarity()
                    + ":" + (stack.has(DataComponents.DAMAGE_RESISTANT) ? "fireproof" : "burns")
                    + ":" + stack.getMaxStackSize()
                    + ":" + (item instanceof net.minecraft.world.item.BlockItem block && block.getBlock() instanceof TieredShulkerBoxBlock)
                    + (stack.is(ModTags.Items.VOID_PROTECTED) ? ":void" : "")
                    + (stack.is(ModTags.Items.DOUBLE_DESPAWN_TIME) ? ":despawn" : ""));
        }
        String expected = "[reinforced_shulker_box:" + Rarity.COMMON + ":burns:1:true, netherite_shulker_box:" + Rarity.UNCOMMON
                + ":fireproof:1:true, enderite_shulker_box:" + Rarity.EPIC + ":fireproof:1:true:void:despawn]";
        helper.assertTrue(summary.toString().equals(expected), "shulker box items: expected " + expected + " but were " + summary);
        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static Block vanillaBlock(String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(path));
    }

    private static BaseContainerBlockEntity container(GameTestHelper helper, BlockPos pos) {
        return (BaseContainerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static TieredShulkerBoxBlockEntity box(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(entity instanceof TieredShulkerBoxBlockEntity, "no tier shulker box at " + pos + ": " + entity);
        return (TieredShulkerBoxBlockEntity) entity;
    }

    private static String describe(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static ItemStack craft(GameTestHelper helper, ServerLevel level, CraftingInput grid, String what, String recipeId) {
        Optional<RecipeHolder<CraftingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(match.isPresent(), what + " crafts nothing at all");
        helper.assertValueEqual(match.get().id().identifier().toString(), recipeId, "recipe matched by " + what);
        return match.get().value().assemble(grid);
    }

    /** Places a box item the way a player does (click on the top of the cell below): facing up. */
    private static void place(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos target = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(target), Direction.UP, target, false);
        net.minecraft.world.InteractionResult result = ((net.minecraft.world.item.BlockItem) stack.getItem()).place(
                new net.minecraft.world.item.context.BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        helper.assertTrue(result.consumesAction() && !helper.getBlockState(pos).isAir(), "placing " + stack + " at " + pos + " failed: " + result);
    }

    private static ServerPlayer smith(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(false);
        helper.runBeforeTestEnd(() -> {
            player.containerMenu = player.inventoryMenu;
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
        return player;
    }

    private static void arm(ServerPlayer player, ItemStack hammer, ItemStack material) {
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setItemInHand(InteractionHand.OFF_HAND, material);
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos box) {
        BlockPos absolute = helper.absolutePos(box);
        Vec3 feet = helper.absoluteVec(new Vec3(box.getX() + 0.5, box.getY() + 1.0, box.getZ() + 0.5));
        player.snapTo(feet.x, feet.y, feet.z, 0.0F, 90.0F);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.375, 0.0), Direction.UP, absolute, false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /**
     * One shulker step: one piece of material short the upgrade cannot begin; with three pieces the hammer starts, the box is still {@code from} after the chest's
     * 100 ticks and {@code to} after 200, and two pieces are gone.
     */
    private static void climb(GameTestHelper helper, ServerPlayer player, BlockPos pos, Item hammerItem, Item material,
                              Block from, Block to, int slots, String what) {
        ItemStack hammer = new ItemStack(hammerItem);
        // One piece short: the hammer would not start (the click would open the box instead - not
        // clicked here, NeoForge refuses to send the menu packet to the mock player).
        arm(player, hammer, new ItemStack(material, MATERIAL_PER_STEP - 1));
        helper.assertFalse(SledgehammerUpgrades.canBegin(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), player),
                what + ": one piece of material short would start the upgrade");
        player.containerMenu = player.inventoryMenu;

        arm(player, hammer, new ItemStack(material, MATERIAL_PER_STEP + 1));
        click(helper, player, pos);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, what + ": the click opened the box instead of the hammer");
        helper.assertTrue(player.isUsingItem() && SledgehammerUpgrades.hasJob(player), what + ": the click did not start the upgrade");
        for (int tick = 1; tick <= CHEST_UPGRADE_TICKS; tick++) {
            player.connection.tick();
        }
        helper.assertTrue(helper.getBlockState(pos).is(from) && SledgehammerUpgrades.hasJob(player),
                what + ": after the chest's five seconds the box is " + helper.getBlockState(pos) + " (job "
                        + SledgehammerUpgrades.hasJob(player) + "), expected still " + from);
        for (int tick = CHEST_UPGRADE_TICKS + 1; tick <= SHULKER_UPGRADE_TICKS; tick++) {
            player.connection.tick();
        }
        helper.assertFalse(SledgehammerUpgrades.hasJob(player), what + ": the job outlived the upgrade");
        helper.assertTrue(helper.getBlockState(pos).is(to), what + ": the box is " + helper.getBlockState(pos) + " after ten blows");
        helper.assertTrue(box(helper, pos).getContainerSize() == slots,
                what + ": " + box(helper, pos).getContainerSize() + " slots, expected " + slots);
        helper.assertTrue(player.getOffhandItem().getCount() == 1,
                what + ": material left " + player.getOffhandItem().getCount() + ", expected " + (MATERIAL_PER_STEP + 1) + " - " + MATERIAL_PER_STEP);
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(hammer));
    }
}
