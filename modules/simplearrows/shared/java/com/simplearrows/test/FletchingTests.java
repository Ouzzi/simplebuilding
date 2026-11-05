package com.simplearrows.test;

import com.simplearrows.SimpleArrows;
import com.simplearrows.fletching.ArrowParts;
import com.simplearrows.fletching.CraftedArrow;
import com.simplearrows.fletching.FletchingMenu;
import com.simplearrows.fletching.FletchingRecipe;
import com.simplearrows.fletching.FletchingRecipes;
import com.simplearrows.fletching.Fletchings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * Befiederungstisch und Pfeile (B14, docs/ai/PLAN-B14-FLETCHING.md; seit b-arrows03 04.11. in Simple Arrows, vorher
 * SimpleBuildings {@code fletching_game_test_*}). Loader-neutral, laufen mit SimpleBuilding und allein (Teile anderer Mods
 * werden dann uebersprungen). Ids {@code module_game_test_<name>} ueber {@link ArrowsTests#ALL}.
 */
public final class FletchingTests {
    private FletchingTests() {
    }

    /** SimpleBuilding's item by id, air without it (no import, module independence). */
    static net.minecraft.world.item.Item sbItem(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(SimpleArrows.SB_NAMESPACE, path));
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
        check(helper, ArrowParts.Tip.LEAD, cow, 0.5);
        check(helper, ArrowParts.Tip.LEAD, zombie, 0.5);   // no target bonus, unlike iron
        helper.succeed();
    }

    private static void check(GameTestHelper helper, ArrowParts.Tip tip, Mob target, double expected) {
        helper.assertValueEqual(tip.bonusAgainst(target), expected, tip.getSerializedName() + " tip against " + target.getType());
    }

    /** Schaft und Befiederung aendern die Schwerkraft, Netherit durchbohrt, Enderit fliegt anfangs gerade. */
    public static void shaftsAndFletchingsChangeTheFlight(GameTestHelper helper) {
        double vanilla = arrow(helper, ArrowParts.Parts.VANILLA).getGravity();
        helper.assertValueEqual(vanilla, 0.05, "gravity of the plain crafted arrow");
        helper.assertValueEqual(arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.END_ROD, ArrowParts.Fletching.FEATHER)).getGravity(),
                0.025, "gravity with an end rod shaft");
        helper.assertTrue(Math.abs(arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.STICK, ArrowParts.Fletching.PHANTOM_MEMBRANE)).getGravity()
                - 0.035) < 1.0E-9, "gravity with phantom membrane fletching");
        helper.assertValueEqual(arrow(helper, parts(ArrowParts.Tip.ENDERITE, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER)).getGravity(),
                0.0, "gravity of an enderite arrow in its first second");
        helper.assertTrue(Math.abs(arrow(helper, parts(ArrowParts.Tip.LEAD, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER)).getGravity() - 0.075) < 1.0E-9,
                "gravity of a lead arrow: 1.5 x 0.05");
        helper.assertTrue(Math.abs(arrow(helper, parts(ArrowParts.Tip.LEAD, ArrowParts.Shaft.STICK, ArrowParts.Fletching.PHANTOM_MEMBRANE)).getGravity() - 0.0525) < 1.0E-9,
                "lead head with phantom membrane: 1.5 x 0.7 x 0.05");
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
        net.minecraft.world.item.Item diamondRod = sbItem("diamond_rod");
        if (diamondRod == Items.AIR) {
            // Without SimpleBuilding there is no diamond rod: its shaft is not offered and its recipe does not load.
            helper.assertFalse(ArrowParts.available(ArrowParts.Shaft.DIAMOND_ROD), "the diamond rod shaft exists without SimpleBuilding");
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
                    SimpleArrows.id("fletching/shaft/diamond_rod"))).isEmpty(), "the diamond rod shaft recipe loads without SimpleBuilding");
            helper.succeed();
            return;
        }
        Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 1));
        helper.assertValueEqual((int) arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.DIAMOND_ROD, ArrowParts.Fletching.FEATHER)).getPierceLevel(),
                1, "pierce level with a diamond rod shaft");
        helper.assertValueEqual((int) arrow(helper, parts(ArrowParts.Tip.NETHERITE, ArrowParts.Shaft.DIAMOND_ROD, ArrowParts.Fletching.FEATHER)).getPierceLevel(),
                2, "pierce level of a netherite tip on a diamond rod shaft");
        helper.assertValueEqual(parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.DIAMOND_ROD, ArrowParts.Fletching.FEATHER).bonusAgainst(cow), 0.0, "diamond rod damage bonus");
        helper.assertValueEqual(FletchingMenu.partSlotFor(new ItemStack(diamondRod)), FletchingMenu.SHAFT_SLOT, "the diamond rod fits the shaft slot");
        helper.assertValueEqual(ArrowParts.Shaft.values().length, 5, "shafts: stick, end rod, blaze rod, breeze rod, diamond rod");
        int tips = (int) java.util.Arrays.stream(ArrowParts.Tip.values()).filter(ArrowParts::available).count();
        helper.assertValueEqual(ArrowParts.allCombinations().size(), tips * 5 * 2, "arrow combinations (" + tips + " tips x 5 shafts x 2 fletchings)");

        java.util.List<net.minecraft.world.item.Item> poles = java.util.List.of(sbItem("netherite_pole"), sbItem("enderite_pole"));
        for (net.minecraft.world.item.Item rod : poles) {
            helper.assertTrue(ArrowParts.shaftFor(new ItemStack(rod)) == null, rod + " is still an arrow shaft");
            helper.assertValueEqual(FletchingMenu.partSlotFor(new ItemStack(rod)), -1, rod + " fits a part slot");
            helper.assertTrue(FletchingMenu.resultFor(new ItemStack(Items.FLINT), new ItemStack(rod), new ItemStack(Items.FEATHER)).isEmpty(),
                    "the table makes arrows from " + rod);
        }
        for (String old : java.util.List.of("netherite_rod", "enderite_rod")) {
            Identifier id = SimpleArrows.id("fletching/flint_" + old + "_feather");
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).isEmpty(),
                    "the old fletching recipe " + id + " is still loaded");
        }
        helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value() instanceof FletchingRecipe)
                .map(holder -> (FletchingRecipe) holder.value())
                .forEach(recipe -> helper.assertTrue(!poles.contains(recipe.input()),
                        "a fletching recipe takes a netherite or enderite rod: " + recipe.idPath()));
        helper.succeed();
    }

    /**
     * Save-Kompatibilitaet (2026-10-03): Pfeile, die noch mit Netherit- oder Enderitstab-Schaft gespeichert sind, laden
     * mit Stock-Schaft - Spitze, Befiederung und Anzahl bleiben, nichts verschwindet oder bricht ab.
     */
    public static void oldNetheriteAndEnderiteShaftsLoadAsSticks(GameTestHelper helper) {
        for (String old : java.util.List.of("netherite_rod", "enderite_rod", "no_such_shaft")) {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("tip", "diamond");
            json.addProperty("shaft", old);
            json.addProperty("fletching", "phantom_membrane");
            ArrowParts.Parts loaded = ArrowParts.Parts.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
            helper.assertValueEqual(loaded, parts(ArrowParts.Tip.DIAMOND, ArrowParts.Shaft.STICK, ArrowParts.Fletching.PHANTOM_MEMBRANE),
                    "arrow parts with the shaft " + old);

            com.google.gson.JsonObject stack = new com.google.gson.JsonObject();
            // Saved before 04.11.: SimpleBuilding's ids (IdMoves), item and component
            stack.addProperty("id", "simplebuilding:crafted_arrow");
            stack.addProperty("count", 7);
            com.google.gson.JsonObject components = new com.google.gson.JsonObject();
            components.add("simplebuilding:arrow_parts", json);
            stack.add("components", components);
            ItemStack decoded = ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), stack)
                    .getOrThrow();
            helper.assertTrue(decoded.is(Fletchings.CRAFTED_ARROW) && decoded.getCount() == 7, "an old arrow stack with the shaft " + old + " loads as " + decoded);
            helper.assertValueEqual(ArrowParts.of(decoded).shaft(), ArrowParts.Shaft.STICK, "shaft of an old arrow stack with " + old);
            helper.assertValueEqual(ArrowParts.of(decoded).tip(), ArrowParts.Tip.DIAMOND, "tip of an old arrow stack with " + old);
        }
        helper.succeed();
    }

    /** Drei passende Teile ergeben vier Pfeile mit diesen Teilen; Nehmen verbraucht je ein Teil. */
    public static void theTableMakesFourArrowsFromThreeParts(GameTestHelper helper) {
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
        helper.assertTrue(result.is(Fletchings.CRAFTED_ARROW), "the result is a crafted arrow: " + result);
        helper.assertValueEqual(result.getCount(), ArrowParts.ARROWS_PER_CRAFT, "arrows per craft");
        helper.assertValueEqual(ArrowParts.of(result), parts(ArrowParts.Tip.IRON, ArrowParts.Shaft.BLAZE_ROD, ArrowParts.Fletching.FEATHER), "parts of the result");
        menu.quickMoveStack(player, FletchingMenu.RESULT_SLOT);
        helper.assertValueEqual(player.getInventory().countItem(Fletchings.CRAFTED_ARROW), 4, "arrows moved into the inventory");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().getCount(), 1, "tips left");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.SHAFT_SLOT).getItem().getCount(), 1, "shafts left");
        helper.assertTrue(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().isEmpty(), "the only feather is used up");
        helper.assertTrue(menu.getSlot(FletchingMenu.RESULT_SLOT).getItem().isEmpty(), "no result once a part is missing");
        helper.succeed();
    }

    private static RecipeHolder<?> fletchingRecipe(GameTestHelper helper, FletchingRecipe.Kind kind, net.minecraft.util.StringRepresentable part) {
        Identifier id = SimpleArrows.id(new FletchingRecipe(kind, part.getSerializedName()).idPath());
        return helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id))
                .orElseThrow(() -> helper.assertionException("the fletching recipe " + id + " is not loaded"));
    }

    /**
     * Rezeptbuch (N16): ein Klick auf ein Teil legt es aus dem Inventar in seinen Slot, die anderen Slots bleiben; Shift so
     * viele wie moeglich; fehlt das Teil, wird nur sein Slot geraeumt und das Geisterrezept gemeldet. Shift-Klick sortiert ein.
     */
    public static void recipeBookPlacesThePartsFromTheInventory(GameTestHelper helper) {
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
        helper.assertTrue(FletchingRecipes.TIP_CATEGORY != null && FletchingRecipes.SHAFT_CATEGORY != null && FletchingRecipes.FLETCHING_CATEGORY != null,
                "the three book categories are registered");
        helper.assertTrue(FletchingRecipes.TIP_CATEGORY != FletchingRecipes.SHAFT_CATEGORY && FletchingRecipes.SHAFT_CATEGORY != FletchingRecipes.FLETCHING_CATEGORY
                && FletchingRecipes.TIP_CATEGORY != FletchingRecipes.FLETCHING_CATEGORY, "three distinct book categories");
        long loaded = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == FletchingRecipes.TYPE).count();
        int expected = 0;
        for (FletchingRecipe recipe : FletchingRecipe.all()) {
            if (recipe.input() != Items.AIR) expected++;
        }
        helper.assertValueEqual((int) loaded, expected, "fletching recipes loaded (one per part of this installation)");
        java.util.Map<FletchingRecipe.Kind, net.minecraft.util.StringRepresentable[]> kinds = java.util.Map.of(
                FletchingRecipe.Kind.TIP, ArrowParts.Tip.values(),
                FletchingRecipe.Kind.SHAFT, ArrowParts.Shaft.values(),
                FletchingRecipe.Kind.FLETCHING, ArrowParts.Fletching.values());
        kinds.forEach((kind, parts) -> {
            for (net.minecraft.util.StringRepresentable part : parts) {
                if (!ArrowParts.available(part)) continue; // a part of a mod that is not loaded (data-driven tips)
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

    /**
     * WE25-9 Trank-Slot: nur Traenke mit Wirkung (Trank, Wurf-, Verweiltrank) passen; mit Trank tragen die 4 Pfeile ihn mit 1/8 Dauer
     * (Vanilla-Tipped-Regel), Nehmen verbraucht die Flasche, danach wieder normale Pfeile. Ein Treffer gibt die Wirkung.
     */
    public static void potionSlotMakesTippedArrows(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        FletchingMenu menu = new FletchingMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        net.minecraft.world.inventory.Slot potionSlot = menu.getSlot(FletchingMenu.POTION_SLOT);
        ItemStack poison = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.POISON);
        helper.assertTrue(potionSlot.mayPlace(poison), "a potion of poison fits the potion slot");
        helper.assertTrue(potionSlot.mayPlace(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS)), "a splash potion fits");
        helper.assertTrue(potionSlot.mayPlace(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.LINGERING_POTION, net.minecraft.world.item.alchemy.Potions.HEALING)), "a lingering potion fits");
        helper.assertFalse(potionSlot.mayPlace(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER)), "a water bottle does not fit");
        helper.assertFalse(potionSlot.mayPlace(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.AWKWARD)), "an awkward potion does not fit");
        helper.assertFalse(potionSlot.mayPlace(new ItemStack(Items.FLINT)), "flint does not fit the potion slot");
        helper.assertFalse(menu.getSlot(FletchingMenu.TIP_SLOT).mayPlace(poison), "a potion does not fit the tip slot");
        menu.getSlot(FletchingMenu.TIP_SLOT).set(new ItemStack(Items.FLINT, 2));
        menu.getSlot(FletchingMenu.SHAFT_SLOT).set(new ItemStack(Items.STICK, 2));
        menu.getSlot(FletchingMenu.FLETCHING_SLOT).set(new ItemStack(Items.FEATHER, 2));
        helper.assertTrue(menu.getSlot(FletchingMenu.RESULT_SLOT).getItem().get(net.minecraft.core.component.DataComponents.POTION_CONTENTS) == null,
                "without a potion: plain arrows");
        potionSlot.set(poison.copy());
        ItemStack result = menu.getSlot(FletchingMenu.RESULT_SLOT).getItem();
        helper.assertValueEqual(result.getCount(), ArrowParts.ARROWS_PER_CRAFT, "4 tipped arrows");
        net.minecraft.world.item.alchemy.PotionContents contents = result.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        helper.assertTrue(contents != null && contents.is(net.minecraft.world.item.alchemy.Potions.POISON), "the arrows carry the potion: " + contents);
        helper.assertValueEqual(result.get(net.minecraft.core.component.DataComponents.POTION_DURATION_SCALE), FletchingMenu.POTION_DURATION_SCALE, "1/8 duration");
        helper.assertValueEqual(ArrowParts.of(result), ArrowParts.Parts.VANILLA, "the parts stay");
        ItemStack tippedArrow = result.copyWithCount(1); // quickMoveStack empties the slot's stack below
        menu.quickMoveStack(player, FletchingMenu.RESULT_SLOT);
        helper.assertTrue(potionSlot.getItem().isEmpty(), "taking the arrows uses up the bottle");
        helper.assertTrue(menu.getSlot(FletchingMenu.RESULT_SLOT).getItem().isEmpty()
                        || menu.getSlot(FletchingMenu.RESULT_SLOT).getItem().get(net.minecraft.core.component.DataComponents.POTION_CONTENTS) == null,
                "after the bottle: plain arrows again");
        int tipped = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Fletchings.CRAFTED_ARROW) && stack.has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)) tipped += stack.getCount();
        }
        helper.assertValueEqual(tipped, 4, "tipped arrows in the inventory");
        // shift-click moves a potion from the inventory into the potion slot
        player.getInventory().setItem(5, poison.copy());
        menu.quickMoveStack(player, 4 + 27 + 5);
        helper.assertTrue(potionSlot.getItem().is(Items.POTION), "shift-click put the potion into the potion slot");
        // a hit gives the effect at 1/8 duration (poison: 900 ticks -> 112)
        Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 1));
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 3.0, 1.5));
        CraftedArrow arrow = new CraftedArrow(helper.getLevel(), at.x, at.y, at.z, tippedArrow, null);
        arrow.applyHitEffects(cow);
        net.minecraft.world.effect.MobEffectInstance effect = cow.getEffect(net.minecraft.world.effect.MobEffects.POISON);
        helper.assertTrue(effect != null && effect.getDuration() > 100 && effect.getDuration() <= 113, "poison for 1/8 of 900 ticks: " + effect);
        helper.succeed();
    }

    /** Rechtsklick auf den Vanilla-Befiederungstisch oeffnet das Menue. */
    public static void rightClickingTheFletchingTableOpensTheMenu(GameTestHelper helper) {
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
        helper.assertValueEqual(FletchingRecipes.all(player).size(),
                (int) FletchingRecipe.all().stream().filter(r -> r.input() != Items.AIR).count(), "recipes to unlock");
        helper.succeed();
    }

    /**
     * Worlds from before 04.11. (b-arrows03): SimpleBuilding's Fletching Table ids resolve to Simple Arrows' - item and entity
     * {@code crafted_arrow}, the parts component inside a saved stack, menu, recipe type and serializer, the three recipe book
     * categories, every part recipe and the parts advancement.
     */
    public static void oldFletchingIdsMove(GameTestHelper helper) {
        java.util.List<String> problems = new java.util.ArrayList<>();
        Identifier arrow = old("crafted_arrow");
        if (BuiltInRegistries.ITEM.getValue(arrow) != Fletchings.CRAFTED_ARROW) problems.add("item");
        if (BuiltInRegistries.ENTITY_TYPE.getValue(arrow) != Fletchings.CRAFTED_ARROW_ENTITY) problems.add("entity type");
        if (BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(old("arrow_parts")) != Fletchings.ARROW_PARTS) problems.add("component");
        if (BuiltInRegistries.MENU.getValue(old(FletchingRecipes.ID)) != Fletchings.MENU) problems.add("menu");
        if (BuiltInRegistries.RECIPE_SERIALIZER.getValue(old(FletchingRecipes.ID)) != FletchingRecipe.SERIALIZER) problems.add("serializer");
        if (BuiltInRegistries.RECIPE_TYPE.getValue(old(FletchingRecipes.ID)) != FletchingRecipes.TYPE) problems.add("recipe type");
        for (FletchingRecipe.Kind kind : FletchingRecipe.Kind.values()) {
            if (BuiltInRegistries.RECIPE_BOOK_CATEGORY.getValue(old(FletchingRecipes.categoryId(kind))) != FletchingRecipes.category(kind)) {
                problems.add("book category " + kind);
            }
        }
        for (String path : Fletchings.recipePaths()) {
            if (!SimpleArrows.id(path).equals(com.simplelib.api.IdMoves.target(Registries.RECIPE, old(path)))) problems.add("recipe " + path);
        }
        if (!SimpleArrows.id("recipes/combat/fletching_table_parts").equals(com.simplelib.api.IdMoves.advancement(old("recipes/combat/fletching_table_parts")))) {
            problems.add("advancement");
        }
        // A saved stack with the old item and component ids keeps its parts and its potion.
        com.google.gson.JsonObject parts = new com.google.gson.JsonObject();
        parts.addProperty("tip", "iron");
        parts.addProperty("shaft", "blaze_rod");
        parts.addProperty("fletching", "phantom_membrane");
        com.google.gson.JsonObject components = new com.google.gson.JsonObject();
        components.add("simplebuilding:arrow_parts", parts);
        com.google.gson.JsonObject potion = new com.google.gson.JsonObject();
        potion.addProperty("potion", "minecraft:poison");
        components.add("minecraft:potion_contents", potion);
        com.google.gson.JsonObject stack = new com.google.gson.JsonObject();
        stack.addProperty("id", arrow.toString());
        stack.addProperty("count", 12);
        stack.add("components", components);
        ItemStack decoded = ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), stack)
                .result().orElse(ItemStack.EMPTY);
        if (!decoded.is(Fletchings.CRAFTED_ARROW) || decoded.getCount() != 12
                || !ArrowParts.of(decoded).equals(parts(ArrowParts.Tip.IRON, ArrowParts.Shaft.BLAZE_ROD, ArrowParts.Fletching.PHANTOM_MEMBRANE))
                || !decoded.has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)) {
            problems.add("saved stack " + decoded + " " + decoded.getComponentsPatch());
        }
        helper.assertTrue(problems.isEmpty(), "old fletching ids: " + problems);
        helper.succeed();
    }

    private static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(SimpleArrows.SB_NAMESPACE, path);
    }

    /**
     * The table's layout (before 04.11. SimpleBuilding's ModScreenStyleTests): feather, shaft and tip on one row 18 px apart, the
     * container slots and the 24x24 result in the container box, the player's in the inventory box, the recipe book button in the
     * container box over no slot.
     */
    public static void fletchingLayout(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        FletchingMenu menu = new FletchingMenu(0, player.getInventory());
        java.util.List<String> problems = new java.util.ArrayList<>();
        int y = menu.getSlot(FletchingMenu.TIP_SLOT).y, x = menu.getSlot(FletchingMenu.TIP_SLOT).x;
        for (int index : new int[] {FletchingMenu.SHAFT_SLOT, FletchingMenu.FLETCHING_SLOT}) {
            net.minecraft.world.inventory.Slot part = menu.getSlot(index);
            if (part.y != y) problems.add("part slots are not on one row");
            if (x - part.x != 18 * (index - FletchingMenu.TIP_SLOT)) problems.add("part slots are not 18 px apart");
        }
        int[] inv = FletchingMenu.inventoryOrigin(menu);
        // container box (0, 0, 176, inventory y - 5 - 2) and inventory box (x - 8, y - 5, 176, 87); fill areas inside 5 px / 7 px frames
        int[] container = {5, 5, 176 - 5, inv[1] - 7 - 7};
        int[] inventory = {inv[0] - 8 + 5, inv[1], inv[0] - 8 + 176 - 5, inv[1] - 5 + 87 - 7};
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int[] box = slot.container instanceof net.minecraft.world.entity.player.Inventory ? inventory : container;
            int pad = slot.index == FletchingMenu.RESULT_SLOT ? 4 : 0, size = slot.index == FletchingMenu.RESULT_SLOT ? 25 : 17;
            if (slot.x - pad < box[0] || slot.y - pad < box[1] || slot.x - pad + size > box[2] || slot.y - pad + size > box[3]) {
                problems.add("slot " + slot.index + " (" + slot.x + ", " + slot.y + ") leaves its box");
            }
            if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory) && FletchingMenu.BOOK_X < slot.x + 17 && slot.x < FletchingMenu.BOOK_X + FletchingMenu.BOOK_W
                    && FletchingMenu.BOOK_Y < slot.y + 17 && slot.y < FletchingMenu.BOOK_Y + FletchingMenu.BOOK_H) {
                problems.add("the recipe book button overlaps slot " + slot.index);
            }
        }
        if (FletchingMenu.BOOK_X < container[0] || FletchingMenu.BOOK_Y < container[1] || FletchingMenu.BOOK_X + FletchingMenu.BOOK_W > container[2]
                || FletchingMenu.BOOK_Y + FletchingMenu.BOOK_H > container[3]) {
            problems.add("the recipe book button leaves the container box");
        }
        helper.assertTrue(problems.isEmpty(), "fletching layout: " + problems);
        helper.succeed();
    }

    /**
     * Amethyst tips shatter on a hit; SimpleBuilding's arrow recovery (only {@code pickup == ALLOWED} arrows come back from mobs)
     * must not hand them back, so the arrow sets {@code DISALLOWED} before the hit. Other arrows keep {@code ALLOWED}.
     */
    public static void amethystArrowIsNotRecovered(GameTestHelper helper) {
        Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 1));
        CraftedArrow amethyst = arrow(helper, parts(ArrowParts.Tip.AMETHYST, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER));
        CraftedArrow iron = arrow(helper, parts(ArrowParts.Tip.IRON, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER));
        amethyst.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED;
        iron.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED;
        amethyst.hitForTest(new net.minecraft.world.phys.EntityHitResult(cow));
        iron.hitForTest(new net.minecraft.world.phys.EntityHitResult(cow));
        helper.assertValueEqual(amethyst.pickup, net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED, "pickup of a shattered amethyst arrow");
        helper.assertTrue(amethyst.isRemoved(), "the amethyst arrow did not shatter");
        helper.assertValueEqual(iron.pickup, net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED, "pickup of an iron arrow after a hit");
        helper.succeed();
    }

    /** With SimpleBuilding: a Training Dummy counts as its head's mob for the tips (iron +2 against zombies, gold against undead). */
    public static void dummyHeadCountsForTips(GameTestHelper helper) {
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.fromNamespaceAndPath(SimpleArrows.SB_NAMESPACE, "training_dummy"));
        if (type.isEmpty()) {
            helper.succeed(); // standalone: no dummy
            return;
        }
        net.minecraft.world.entity.Entity zombie = type.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        net.minecraft.world.entity.Entity plain = type.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(zombie instanceof net.minecraft.world.entity.LivingEntity && plain instanceof net.minecraft.world.entity.LivingEntity, "the dummy is no living entity");
        ((net.minecraft.world.entity.LivingEntity) zombie).setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.ZOMBIE_HEAD));
        ((net.minecraft.world.entity.LivingEntity) plain).setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        helper.assertValueEqual(ArrowParts.Tip.IRON.bonusAgainst(zombie), 2.5, "iron tip against the zombie head");
        helper.assertValueEqual(ArrowParts.Tip.IRON.bonusAgainst(plain), 0.5, "iron tip against the pumpkin");
        helper.assertValueEqual(ArrowParts.Tip.GOLD.bonusAgainst(zombie), 2.0, "gold tip against the zombie head");
        helper.succeed();
    }

    /**
     * Data-driven parts: every part is its item tag {@code simplearrows:fletching/<kind>/<part>}; the Vanilla parts are always there,
     * the Enderite tip only with Simple Ores and the diamond rod shaft only with SimpleBuilding - tags, tab and recipes agree.
     */
    public static void tipTagsAreData(GameTestHelper helper) {
        java.util.List<String> problems = new java.util.ArrayList<>();
        for (ArrowParts.Tip tip : java.util.List.of(ArrowParts.Tip.FLINT, ArrowParts.Tip.COPPER, ArrowParts.Tip.IRON, ArrowParts.Tip.GOLD,
                ArrowParts.Tip.DIAMOND, ArrowParts.Tip.NETHERITE, ArrowParts.Tip.AMETHYST, ArrowParts.Tip.PRISMARINE)) {
            if (!ArrowParts.available(tip)) problems.add("tip " + tip.getSerializedName() + " missing");
        }
        boolean ores = BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath(SimpleArrows.ORES_NAMESPACE, "enderite_nugget"));
        boolean sb = sbItem("diamond_rod") != Items.AIR;
        if (ArrowParts.available(ArrowParts.Tip.LEAD) != BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath(SimpleArrows.ORES_NAMESPACE, "lead_nugget"))) problems.add("lead tip present mismatch");
        if (ArrowParts.available(ArrowParts.Tip.ENDERITE) != ores) problems.add("enderite tip present=" + !ores);
        if (ArrowParts.available(ArrowParts.Shaft.DIAMOND_ROD) != sb) problems.add("diamond rod shaft present=" + !sb);
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (FletchingRecipe recipe : FletchingRecipe.all()) {
            boolean loaded = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleArrows.id(recipe.idPath()))).isPresent();
            if (loaded != (recipe.input() != Items.AIR)) problems.add("recipe " + recipe.idPath() + " loaded=" + loaded);
        }
        helper.assertValueEqual(FletchingMenu.partSlotFor(new ItemStack(Items.COPPER_NUGGET)), FletchingMenu.TIP_SLOT, "copper nugget = tip");
        long inTab = com.simplearrows.ArrowsRegistry.stacks().stream().filter(s -> s.is(Fletchings.CRAFTED_ARROW)).count();
        if (inTab != ArrowParts.allCombinations().size()) problems.add("tab shows " + inTab + " table arrows");
        helper.assertTrue(problems.isEmpty(), "data-driven parts: " + problems);
        helper.succeed();
    }
}
