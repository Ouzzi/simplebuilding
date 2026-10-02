package com.simplebuilding.gametest;

import com.simplebuilding.fletching.ArrowParts;
import com.simplebuilding.fletching.CraftedArrow;
import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.fletching.FletchingRecipe;
import com.simplebuilding.fletching.FletchingRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Befiederungstisch und Pfeile (B14, docs/ai/PLAN-B14-FLETCHING.md). Loader-neutral; auf Linien ohne
 * {@link McVersion#FLETCHING} gelingen die Tests sofort.
 */
public final class FletchingTests {
    private FletchingTests() {
    }

    private static ArrowParts.Parts parts(ArrowParts.Tip tip, ArrowParts.Shaft shaft, ArrowParts.Fletching fletching) {
        return new ArrowParts.Parts(tip, shaft, fletching);
    }

    private static CraftedArrow arrow(GameTestHelper helper, ArrowParts.Parts parts) {
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 3.0, 1.5));
        return new CraftedArrow(helper.getLevel(), at.x, at.y, at.z, ArrowParts.stack(parts, 1), null);
    }

    /** Spitzen: Grundbonus plus Bonus gegen ihre Zielgruppe, sonst nichts. */
    public static void eachTipAddsItsDamageAgainstItsTargets(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        Mob drowned = helper.spawnWithNoFreeWill(EntityTypes.DROWNED, new BlockPos(1, 2, 1));
        Mob skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new BlockPos(1, 2, 1));
        Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 1));
        check(helper, ArrowParts.Tip.FLINT, zombie, 0.0);
        check(helper, ArrowParts.Tip.COPPER, drowned, 2.0);
        check(helper, ArrowParts.Tip.COPPER, zombie, 0.0);
        check(helper, ArrowParts.Tip.IRON, zombie, 2.5);
        check(helper, ArrowParts.Tip.IRON, cow, 0.5);
        check(helper, ArrowParts.Tip.GOLD, skeleton, 2.0);
        check(helper, ArrowParts.Tip.GOLD, cow, 0.0);
        check(helper, ArrowParts.Tip.DIAMOND, cow, 1.0);
        check(helper, ArrowParts.Tip.NETHERITE, cow, 1.5);
        check(helper, ArrowParts.Tip.ENDERITE, cow, 1.5);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, ArrowParts.Tip tip, Mob target, double expected) {
        helper.assertValueEqual(tip.bonusAgainst(target), expected, tip.getSerializedName() + " tip against " + target.getType());
    }

    /** Schaft und Befiederung aendern die Schwerkraft, Netherit durchbohrt, Enderit fliegt anfangs gerade. */
    public static void shaftsAndFletchingsChangeTheFlight(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        double vanilla = arrow(helper, ArrowParts.Parts.VANILLA).getGravity();
        helper.assertValueEqual(vanilla, 0.05, "gravity of the plain crafted arrow");
        helper.assertValueEqual(arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.END_ROD, ArrowParts.Fletching.FEATHER)).getGravity(),
                0.025, "gravity with an end rod shaft");
        helper.assertTrue(Math.abs(arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.STICK, ArrowParts.Fletching.PHANTOM_MEMBRANE)).getGravity()
                - 0.035) < 1.0E-9, "gravity with phantom membrane fletching");
        helper.assertValueEqual(arrow(helper, parts(ArrowParts.Tip.ENDERITE, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER)).getGravity(),
                0.0, "gravity of an enderite arrow in its first second");
        helper.assertValueEqual((int) arrow(helper, parts(ArrowParts.Tip.NETHERITE, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER)).getPierceLevel(),
                ArrowParts.NETHERITE_EXTRA_PIERCE, "pierce level of a netherite arrow");
        helper.assertValueEqual((int) arrow(helper, ArrowParts.Parts.VANILLA).getPierceLevel(), 0, "pierce level of the plain arrow");
        helper.assertTrue(arrow(helper, parts(ArrowParts.Tip.PRISMARINE, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER)).getWaterInertia()
                > arrow(helper, ArrowParts.Parts.VANILLA).getWaterInertia(), "prismarine arrows are not slowed down by water");
        helper.succeed();
    }

    /**
     * Material-Stab-Schaefte (2026-10-02): Diamant durchbohrt ein Ziel mehr, Netherit +1 Schaden und der Pfeil-Stapel
     * verbrennt als Item nicht, Enderit 30 % weniger Schwerkraft und +1 Schaden; der Tisch nimmt die Staebe als Schaft.
     */
    public static void rodShaftsPierceHitHarderAndResistFire(GameTestHelper helper) {
        if (!McVersion.FLETCHING || ModItems.DIAMOND_ROD == null) {
            helper.succeed();
            return;
        }
        Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 1));
        helper.assertValueEqual((int) arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.DIAMOND_ROD, ArrowParts.Fletching.FEATHER)).getPierceLevel(),
                1, "pierce level with a diamond rod shaft");
        helper.assertValueEqual((int) arrow(helper, parts(ArrowParts.Tip.NETHERITE, ArrowParts.Shaft.DIAMOND_ROD, ArrowParts.Fletching.FEATHER)).getPierceLevel(),
                2, "pierce level of a netherite tip on a diamond rod shaft");
        helper.assertValueEqual(parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.DIAMOND_ROD, ArrowParts.Fletching.FEATHER).bonusAgainst(cow), 0.0, "diamond rod damage bonus");
        helper.assertValueEqual(parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.NETHERITE_ROD, ArrowParts.Fletching.FEATHER).bonusAgainst(cow), 1.0, "netherite rod damage bonus");
        helper.assertValueEqual(parts(ArrowParts.Tip.DIAMOND, ArrowParts.Shaft.ENDERITE_ROD, ArrowParts.Fletching.FEATHER).bonusAgainst(cow), 2.0, "diamond tip on an enderite rod");
        helper.assertTrue(Math.abs(arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.ENDERITE_ROD, ArrowParts.Fletching.FEATHER)).getGravity()
                - 0.035) < 1.0E-9, "gravity with an enderite rod shaft");
        helper.assertValueEqual(arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.NETHERITE_ROD, ArrowParts.Fletching.FEATHER)).getGravity(),
                0.05, "gravity with a netherite rod shaft");

        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        net.minecraft.world.entity.item.ItemEntity fireproof = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), at.x, at.y, at.z,
                ArrowParts.stack(parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.NETHERITE_ROD, ArrowParts.Fletching.FEATHER), 4));
        net.minecraft.world.entity.item.ItemEntity plain = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), at.x, at.y, at.z,
                ArrowParts.stack(ArrowParts.Parts.VANILLA, 4));
        helper.assertTrue(fireproof.fireImmune(), "netherite rod arrows catch fire as an item");
        helper.assertFalse(fireproof.hurtServer(helper.getLevel(), helper.getLevel().damageSources().lava(), 4.0F), "lava hurts netherite rod arrows");
        helper.assertFalse(plain.fireImmune(), "plain crafted arrows are fire immune");

        helper.assertValueEqual(FletchingMenu.partSlotFor(new ItemStack(ModItems.DIAMOND_ROD)), FletchingMenu.SHAFT_SLOT, "the diamond rod fits the shaft slot");
        helper.assertValueEqual(FletchingMenu.partSlotFor(new ItemStack(ModItems.ENDERITE_ROD)), FletchingMenu.SHAFT_SLOT, "the enderite rod fits the shaft slot");
        ItemStack result = FletchingMenu.resultFor(new ItemStack(Items.FLINT), new ItemStack(ModItems.NETHERITE_ROD), new ItemStack(Items.FEATHER));
        helper.assertValueEqual(ArrowParts.of(result).shaft(), ArrowParts.Shaft.NETHERITE_ROD, "shaft of the table result");
        helper.succeed();
    }

    /** Drei passende Teile ergeben vier Pfeile mit diesen Teilen; Nehmen verbraucht je ein Teil. */
    public static void theTableMakesFourArrowsFromThreeParts(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        FletchingMenu menu = new FletchingMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        helper.assertFalse(menu.getSlot(FletchingMenu.TIP_SLOT).mayPlace(new ItemStack(Items.STICK)), "a stick does not fit the tip slot");
        helper.assertFalse(menu.getSlot(FletchingMenu.SHAFT_SLOT).mayPlace(new ItemStack(Items.DIRT)), "dirt does not fit the shaft slot");
        menu.getSlot(FletchingMenu.TIP_SLOT).set(new ItemStack(Items.IRON_NUGGET, 2));
        menu.getSlot(FletchingMenu.SHAFT_SLOT).set(new ItemStack(Items.BLAZE_ROD, 2));
        helper.assertTrue(menu.getSlot(FletchingMenu.RESULT_SLOT).getItem().isEmpty(), "no result without fletching");
        menu.getSlot(FletchingMenu.FLETCHING_SLOT).set(new ItemStack(Items.FEATHER, 1));
        ItemStack result = menu.getSlot(FletchingMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.is(ModItems.CRAFTED_ARROW), "the result is a crafted arrow: " + result);
        helper.assertValueEqual(result.getCount(), ArrowParts.ARROWS_PER_CRAFT, "arrows per craft");
        helper.assertValueEqual(ArrowParts.of(result), parts(ArrowParts.Tip.IRON, ArrowParts.Shaft.BLAZE_ROD, ArrowParts.Fletching.FEATHER), "parts of the result");
        menu.quickMoveStack(player, FletchingMenu.RESULT_SLOT);
        helper.assertValueEqual(player.getInventory().countItem(ModItems.CRAFTED_ARROW), 4, "arrows moved into the inventory");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().getCount(), 1, "tips left");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.SHAFT_SLOT).getItem().getCount(), 1, "shafts left");
        helper.assertTrue(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().isEmpty(), "the only feather is used up");
        helper.assertTrue(menu.getSlot(FletchingMenu.RESULT_SLOT).getItem().isEmpty(), "no result once a part is missing");
        helper.succeed();
    }

    private static RecipeHolder<?> fletchingRecipe(GameTestHelper helper, ArrowParts.Parts parts) {
        Identifier id = Identifier.fromNamespaceAndPath("simplebuilding", new FletchingRecipe(parts).idPath());
        return helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id))
                .orElseThrow(() -> helper.assertionException("the fletching recipe " + id + " is not loaded"));
    }

    /**
     * Rezeptbuch wie an der Werkbank: ein Klick legt Spitze, Schaft und Befiederung aus dem Inventar ein, Shift so viele
     * wie moeglich, fehlt ein Teil, werden die Felder geraeumt und das Geisterrezept gemeldet. Shift-Klick sortiert ein.
     */
    public static void recipeBookPlacesThePartsFromTheInventory(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD, 5));
        player.getInventory().setItem(1, new ItemStack(Items.STICK, 5));
        player.getInventory().setItem(2, new ItemStack(Items.PHANTOM_MEMBRANE, 3));
        FletchingMenu menu = new FletchingMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        ArrowParts.Parts amethyst = parts(ArrowParts.Tip.AMETHYST, ArrowParts.Shaft.STICK, ArrowParts.Fletching.PHANTOM_MEMBRANE);
        RecipeHolder<?> recipe = fletchingRecipe(helper, amethyst);

        helper.assertValueEqual(menu.handlePlacement(false, false, recipe, helper.getLevel(), player.getInventory()),
                RecipeBookMenu.PostPlaceAction.NOTHING, "placing a craftable arrow");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().getCount(), 1, "one tip placed");
        helper.assertTrue(menu.getSlot(FletchingMenu.SHAFT_SLOT).getItem().is(Items.STICK), "the stick went into the shaft slot");
        helper.assertTrue(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().is(Items.PHANTOM_MEMBRANE), "the membrane went into the fletching slot");
        ItemStack result = menu.getSlot(FletchingMenu.RESULT_SLOT).getItem();
        helper.assertValueEqual(ArrowParts.of(result), amethyst, "the result shows the placed arrow");
        helper.assertValueEqual(player.getInventory().countItem(Items.AMETHYST_SHARD), 4, "the tip left the inventory");

        menu.handlePlacement(true, false, recipe, helper.getLevel(), player.getInventory());
        helper.assertValueEqual(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().getCount(), 3, "shift places as many as the scarcest part allows");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().getCount(), 3, "shift places three tips");

        RecipeHolder<?> diamond = fletchingRecipe(helper, parts(ArrowParts.Tip.DIAMOND, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER));
        helper.assertValueEqual(menu.handlePlacement(false, false, diamond, helper.getLevel(), player.getInventory()),
                RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE, "without diamond pebbles the book shows the ghost recipe");
        helper.assertTrue(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().isEmpty(), "the grid was cleared for the ghost recipe");
        helper.assertValueEqual(player.getInventory().countItem(Items.AMETHYST_SHARD), 5, "the amethyst went back, nothing was made");
        helper.assertValueEqual(player.getInventory().countItem(Items.PHANTOM_MEMBRANE), 3, "the membranes went back");

        int inventorySlot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.PHANTOM_MEMBRANE));
        int membraneSlot = inventorySlot < 9 ? 4 + 27 + inventorySlot : 4 + inventorySlot - 9;
        menu.quickMoveStack(player, membraneSlot);
        helper.assertValueEqual(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().getCount(), 3, "shift click put the membranes into the fletching slot");
        helper.succeed();
    }

    /** Fuer jede Teile-Kombination gibt es genau ein Rezept; das Rezeptbuch zeigt das Ergebnis des Tisches. */
    public static void everyCombinationHasItsRecipeBookEntry(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        long loaded = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == FletchingRecipes.TYPE).count();
        helper.assertValueEqual((int) loaded, ArrowParts.allCombinations().size(), "fletching recipes loaded");
        for (ArrowParts.Parts parts : ArrowParts.allCombinations()) {
            FletchingRecipe recipe = (FletchingRecipe) fletchingRecipe(helper, parts).value();
            ItemStack expected = FletchingMenu.resultFor(new ItemStack(parts.tip().input()), new ItemStack(parts.shaft().input()),
                    new ItemStack(parts.fletching().input()));
            helper.assertTrue(ItemStack.matches(recipe.result(), expected), "the book shows what the table makes for " + parts);
            helper.assertTrue(recipe.recipeBookCategory() == FletchingRecipes.CATEGORY, "book category of " + parts);
        }
        helper.succeed();
    }

    /** Rechtsklick auf den Vanilla-Befiederungstisch oeffnet das Menue. */
    public static void rightClickingTheFletchingTableOpensTheMenu(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        BlockPos table = new BlockPos(1, 2, 1);
        helper.setBlock(table, Blocks.FLETCHING_TABLE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos at = helper.absolutePos(table);
        helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        helper.assertTrue(player.containerMenu instanceof FletchingMenu, "the fletching table opened " + player.containerMenu);
        // Wie das fruehere Panel alle Materialien zeigte, kennt das Rezeptbuch nach dem Oeffnen alle Pfeile.
        for (RecipeHolder<?> holder : FletchingRecipes.all(player)) {
            helper.assertTrue(player.getRecipeBook().contains(holder.id()), "opening the table unlocked " + holder.id());
        }
        helper.assertValueEqual(FletchingRecipes.all(player).size(), ArrowParts.allCombinations().size(), "recipes to unlock");
        helper.succeed();
    }
}
