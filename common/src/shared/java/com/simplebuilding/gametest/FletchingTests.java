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
     * Material-Stab-Schaefte: nur der Diamantstab (2026-10-02) ist ein Schaft und durchbohrt ein Ziel mehr. Netherit- und
     * Enderitstab sind seit 2026-10-03 Blitzableiter-Bloecke (Besitzer): sie passen nicht in den Schaft-Slot, der Tisch
     * macht aus ihnen nichts, und kein Fletching-Rezept nimmt sie (9 Spitzen x 5 Schaefte x 2 Befiederungen = 90).
     */
    public static void materialShaftsAreOnlyTheDiamondRod(GameTestHelper helper) {
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
        helper.assertValueEqual(FletchingMenu.partSlotFor(new ItemStack(ModItems.DIAMOND_ROD)), FletchingMenu.SHAFT_SLOT, "the diamond rod fits the shaft slot");
        helper.assertValueEqual(ArrowParts.Shaft.values().length, 5, "shafts: stick, end rod, blaze rod, breeze rod, diamond rod");
        helper.assertValueEqual(ArrowParts.allCombinations().size(), 90, "arrow combinations");

        for (net.minecraft.world.item.Item rod : java.util.List.of(ModItems.NETHERITE_ROD, ModItems.ENDERITE_ROD)) {
            helper.assertTrue(ArrowParts.shaftFor(new ItemStack(rod)) == null, rod + " is still an arrow shaft");
            helper.assertValueEqual(FletchingMenu.partSlotFor(new ItemStack(rod)), -1, rod + " fits a part slot");
            helper.assertTrue(FletchingMenu.resultFor(new ItemStack(Items.FLINT), new ItemStack(rod), new ItemStack(Items.FEATHER)).isEmpty(),
                    "the table makes arrows from " + rod);
        }
        for (String old : java.util.List.of("netherite_rod", "enderite_rod")) {
            Identifier id = Identifier.fromNamespaceAndPath("simplebuilding", "fletching/flint_" + old + "_feather");
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).isEmpty(),
                    "the old fletching recipe " + id + " is still loaded");
        }
        helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value() instanceof FletchingRecipe)
                .map(holder -> (FletchingRecipe) holder.value())
                .forEach(recipe -> helper.assertTrue(recipe.input() != ModItems.NETHERITE_ROD && recipe.input() != ModItems.ENDERITE_ROD,
                        "a fletching recipe takes a netherite or enderite rod: " + recipe.idPath()));
        helper.succeed();
    }

    /**
     * Save-Kompatibilitaet (2026-10-03): Pfeile, die noch mit Netherit- oder Enderitstab-Schaft gespeichert sind, laden
     * mit Stock-Schaft - Spitze, Befiederung und Anzahl bleiben, nichts verschwindet oder bricht ab.
     */
    public static void oldNetheriteAndEnderiteShaftsLoadAsSticks(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        for (String old : java.util.List.of("netherite_rod", "enderite_rod", "no_such_shaft")) {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("tip", "diamond");
            json.addProperty("shaft", old);
            json.addProperty("fletching", "phantom_membrane");
            ArrowParts.Parts loaded = ArrowParts.Parts.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
            helper.assertValueEqual(loaded, parts(ArrowParts.Tip.DIAMOND, ArrowParts.Shaft.STICK, ArrowParts.Fletching.PHANTOM_MEMBRANE),
                    "arrow parts with the shaft " + old);

            com.google.gson.JsonObject stack = new com.google.gson.JsonObject();
            stack.addProperty("id", "simplebuilding:crafted_arrow");
            stack.addProperty("count", 7);
            com.google.gson.JsonObject components = new com.google.gson.JsonObject();
            components.add("simplebuilding:arrow_parts", json);
            stack.add("components", components);
            ItemStack decoded = ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), stack)
                    .getOrThrow();
            helper.assertTrue(decoded.is(ModItems.CRAFTED_ARROW) && decoded.getCount() == 7, "an old arrow stack with the shaft " + old + " loads as " + decoded);
            helper.assertValueEqual(ArrowParts.of(decoded).shaft(), ArrowParts.Shaft.STICK, "shaft of an old arrow stack with " + old);
            helper.assertValueEqual(ArrowParts.of(decoded).tip(), ArrowParts.Tip.DIAMOND, "tip of an old arrow stack with " + old);
        }
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

    private static RecipeHolder<?> fletchingRecipe(GameTestHelper helper, FletchingRecipe.Kind kind, net.minecraft.util.StringRepresentable part) {
        Identifier id = Identifier.fromNamespaceAndPath("simplebuilding", new FletchingRecipe(kind, part.getSerializedName()).idPath());
        return helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id))
                .orElseThrow(() -> helper.assertionException("the fletching recipe " + id + " is not loaded"));
    }

    /**
     * Rezeptbuch (N16): ein Klick auf ein Teil legt es aus dem Inventar in seinen Slot, die anderen Slots bleiben; Shift so
     * viele wie moeglich; fehlt das Teil, wird nur sein Slot geraeumt und das Geisterrezept gemeldet. Shift-Klick sortiert ein.
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
        RecipeHolder<?> amethyst = fletchingRecipe(helper, FletchingRecipe.Kind.TIP, ArrowParts.Tip.AMETHYST);
        RecipeHolder<?> stick = fletchingRecipe(helper, FletchingRecipe.Kind.SHAFT, ArrowParts.Shaft.STICK);
        RecipeHolder<?> membrane = fletchingRecipe(helper, FletchingRecipe.Kind.FLETCHING, ArrowParts.Fletching.PHANTOM_MEMBRANE);

        helper.assertValueEqual(menu.handlePlacement(false, false, amethyst, helper.getLevel(), player.getInventory()),
                RecipeBookMenu.PostPlaceAction.NOTHING, "placing an amethyst tip");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().getCount(), 1, "one tip placed");
        helper.assertTrue(menu.getSlot(FletchingMenu.SHAFT_SLOT).getItem().isEmpty(), "a tip entry leaves the shaft slot alone");
        helper.assertTrue(menu.getSlot(FletchingMenu.RESULT_SLOT).getItem().isEmpty(), "no arrow with only a tip");
        menu.handlePlacement(false, false, stick, helper.getLevel(), player.getInventory());
        menu.handlePlacement(false, false, membrane, helper.getLevel(), player.getInventory());
        helper.assertTrue(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().is(Items.AMETHYST_SHARD), "the tip stayed while shaft and fletching were placed");
        helper.assertTrue(menu.getSlot(FletchingMenu.SHAFT_SLOT).getItem().is(Items.STICK), "the stick went into the shaft slot");
        helper.assertTrue(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().is(Items.PHANTOM_MEMBRANE), "the membrane went into the fletching slot");
        ItemStack result = menu.getSlot(FletchingMenu.RESULT_SLOT).getItem();
        helper.assertValueEqual(ArrowParts.of(result), parts(ArrowParts.Tip.AMETHYST, ArrowParts.Shaft.STICK, ArrowParts.Fletching.PHANTOM_MEMBRANE),
                "the result shows the assembled arrow");
        helper.assertValueEqual(player.getInventory().countItem(Items.AMETHYST_SHARD), 4, "the tip left the inventory");

        menu.handlePlacement(true, false, amethyst, helper.getLevel(), player.getInventory());
        helper.assertValueEqual(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().getCount(), 5, "shift places every amethyst shard");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().getCount(), 1, "shift on a tip leaves the fletching");

        RecipeHolder<?> diamond = fletchingRecipe(helper, FletchingRecipe.Kind.TIP, ArrowParts.Tip.DIAMOND);
        helper.assertValueEqual(menu.handlePlacement(false, false, diamond, helper.getLevel(), player.getInventory()),
                RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE, "without diamond pebbles the book shows the ghost recipe");
        helper.assertTrue(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().isEmpty(), "the tip slot was cleared for the ghost recipe");
        helper.assertTrue(menu.getSlot(FletchingMenu.SHAFT_SLOT).getItem().is(Items.STICK), "the shaft stayed for the ghost tip");
        helper.assertValueEqual(player.getInventory().countItem(Items.AMETHYST_SHARD), 5, "the amethyst went back, nothing was made");

        menu.handlePlacement(false, false, stick, helper.getLevel(), player.getInventory());
        int inventorySlot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.PHANTOM_MEMBRANE));
        int membraneSlot = inventorySlot < 9 ? 4 + 27 + inventorySlot : 4 + inventorySlot - 9;
        menu.quickMoveStack(player, membraneSlot);
        helper.assertValueEqual(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().getCount(), 3, "shift click put the membranes into the fletching slot");
        helper.succeed();
    }

    /**
     * N16: das Rezeptbuch hat drei Kategorien (Spitze, Schaft, Befiederung); je Teil genau ein Eintrag in der Kategorie
     * seiner Art, der das Teil mit seiner Wirkung als Kurz-Tooltip zeigt. Keine ganzen Pfeile mehr im Buch.
     */
    public static void everyCombinationHasItsRecipeBookEntry(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        helper.assertTrue(FletchingRecipes.TIP_CATEGORY != null && FletchingRecipes.SHAFT_CATEGORY != null && FletchingRecipes.FLETCHING_CATEGORY != null,
                "the three book categories are registered");
        helper.assertTrue(FletchingRecipes.TIP_CATEGORY != FletchingRecipes.SHAFT_CATEGORY && FletchingRecipes.SHAFT_CATEGORY != FletchingRecipes.FLETCHING_CATEGORY
                && FletchingRecipes.TIP_CATEGORY != FletchingRecipes.FLETCHING_CATEGORY, "three distinct book categories");
        long loaded = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == FletchingRecipes.TYPE).count();
        int expected = ArrowParts.Tip.values().length + ArrowParts.Shaft.values().length + ArrowParts.Fletching.values().length;
        helper.assertValueEqual((int) loaded, expected, "fletching recipes loaded (one per part)");
        java.util.Map<FletchingRecipe.Kind, net.minecraft.util.StringRepresentable[]> kinds = java.util.Map.of(
                FletchingRecipe.Kind.TIP, ArrowParts.Tip.values(),
                FletchingRecipe.Kind.SHAFT, ArrowParts.Shaft.values(),
                FletchingRecipe.Kind.FLETCHING, ArrowParts.Fletching.values());
        kinds.forEach((kind, parts) -> {
            for (net.minecraft.util.StringRepresentable part : parts) {
                FletchingRecipe recipe = (FletchingRecipe) fletchingRecipe(helper, kind, part).value();
                helper.assertTrue(recipe.recipeBookCategory() == FletchingRecipes.category(kind), "book category of " + recipe.idPath());
                helper.assertValueEqual(FletchingMenu.partSlotFor(new ItemStack(recipe.input())), kind.slot(), "slot of " + recipe.idPath());
                ItemStack shown = recipe.result();
                helper.assertTrue(shown.is(recipe.input()), "the book shows the part itself for " + recipe.idPath());
                net.minecraft.world.item.component.ItemLore lore = shown.get(net.minecraft.core.component.DataComponents.LORE);
                helper.assertTrue(lore != null && lore.lines().size() == 1, "one short effect line for " + recipe.idPath());
            }
        });
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
        helper.assertValueEqual(FletchingRecipes.all(player).size(), FletchingRecipe.all().size(), "recipes to unlock");
        helper.succeed();
    }
}
