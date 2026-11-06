package com.simplebuildingtools.test;

import com.simplebuildingtools.SimpleBuildingTools;
import com.simplebuildingtools.config.BtConfig;
import com.simplebuildingtools.items.BtItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Module tests of Simple Building Tools (name -> body; the Fabric adapter {@code ModuleGameTest} calls them): registration, the
 * moved ids, the config takeover, the sub-mod switch, the recipes and the Enderite rule. The tool tests themselves moved with
 * the code from SimpleBuilding ({@link BuildingWandTests}, {@link WandModeTests}, {@link OctantTests}, {@link BlueprintTests}).
 */
public final class BtTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("registered", BtTests::registered);
        ALL.put("old_tool_ids_move", BtTests::oldToolIdsMove);
        ALL.put("config_takeover", BtTests::configTakeover);
        ALL.put("sub_mod_switch", BtTests::subModSwitch);
        ALL.put("tool_recipes_are_there", BtTests::toolRecipesAreThere);
        ALL.put("enderite_wand_needs_simple_ores", BtTests::enderiteWandNeedsSimpleOres);
        ALL.put("guide_book", com.simplebuildingtools.guide.BuildingToolsGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simplebuildingtools.discover.BuildingToolsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplebuildingtools.discover.BuildingToolsDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simplebuildingtools.discover.BuildingToolsDiscover::firstPickupTips);
    }

    private BtTests() {
    }

    /** All 26 tools under simplebuildingtools:, the two components, the tab. */
    public static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleBuildingTools.ITEMS) {
            if (!BuiltInRegistries.ITEM.containsKey(SimpleBuildingTools.id(path))) problems.add("item " + path);
        }
        if (SimpleBuildingTools.ITEMS.size() != 26) problems.add("26 items expected, found " + SimpleBuildingTools.ITEMS.size());
        if (BtItems.all().size() != 26) problems.add("BtItems has " + BtItems.all().size());
        for (String path : SimpleBuildingTools.COMPONENTS) {
            if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleBuildingTools.id(path))) problems.add("component " + path);
        }
        if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimpleBuildingTools.id("tools"))) problems.add("tab simplebuildingtools:tools");
        var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (String path : SimpleBuildingTools.ENCHANTMENTS) {
            if (enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, SimpleBuildingTools.id(path))).isEmpty()) problems.add("enchantment " + path);
        }
        h.assertTrue(problems.isEmpty(), "registration: " + problems);
        h.succeed();
    }

    /** Old worlds: every simplebuilding: id of the tools resolves to the module's object (items, components, enchantments, recipes). */
    public static void oldToolIdsMove(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleBuildingTools.ITEMS) {
            Identifier old = Identifier.fromNamespaceAndPath(SimpleBuildingTools.SB_NAMESPACE, path);
            Item now = BuiltInRegistries.ITEM.getValue(SimpleBuildingTools.id(path));
            if (BuiltInRegistries.ITEM.getValue(old) != now) problems.add("item " + path);
        }
        // A stack saved with the old id loads as the module's item (the codec reads "id").
        var parsed = net.minecraft.world.item.ItemStack.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),
                com.google.gson.JsonParser.parseString("{\"id\":\"simplebuilding:diamond_building_wand\",\"count\":1}"));
        if (parsed.result().map(s -> s.getItem() != BtItems.DIAMOND_BUILDING_WAND).orElse(true)) problems.add("saved stack with id");
        if (BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath(SimpleBuildingTools.SB_NAMESPACE, "blueprint"))
                != com.simplebuildingtools.component.BtComponents.BLUEPRINT) problems.add("component blueprint");
        for (String path : SimpleBuildingTools.ENCHANTMENTS) {
            Identifier target = com.simplelib.api.IdMoves.target(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SimpleBuildingTools.SB_NAMESPACE, path));
            if (!SimpleBuildingTools.id(path).equals(target)) problems.add("enchantment " + path + " -> " + target);
        }
        Identifier recipe = com.simplelib.api.IdMoves.target(Registries.RECIPE, Identifier.fromNamespaceAndPath(SimpleBuildingTools.SB_NAMESPACE, "octant"));
        if (!SimpleBuildingTools.id("octant").equals(recipe)) problems.add("recipe octant -> " + recipe);
        h.assertTrue(problems.isEmpty(), "old ids: " + problems);
        h.succeed();
    }

    /** A first start takes the tool options of an existing simplebuilding.json over. */
    public static void configTakeover(GameTestHelper h) {
        String old = "{\"tools\":{\"buildingWandHungerCost\":false,\"wandHungerMultiplier\":2.5},"
                + "\"server\":{\"features\":{\"blueprint\":false},\"blueprint\":{\"maxBlocksPerTick\":64,\"pictureImport\":false,\"pictureMaxSide\":32}}}";
        BtConfig taken = new com.google.gson.Gson().fromJson(BtConfig.fromLegacyForTests(old), BtConfig.class).validate();
        h.assertFalse(taken.wands.hungerCost, "wands.hungerCost taken over");
        h.assertTrue(Math.abs(taken.wands.hungerMultiplier - 2.5) < 1e-9, "wands.hungerMultiplier taken over");
        h.assertFalse(taken.blueprint.enabled, "features.blueprint -> blueprint.enabled");
        h.assertValueEqual(taken.blueprint.maxBlocksPerTick, 64, "blueprint.maxBlocksPerTick taken over");
        h.assertFalse(taken.blueprint.pictureImport, "blueprint.pictureImport taken over");
        h.assertValueEqual(taken.blueprint.pictureMaxSide, 32, "blueprint.pictureMaxSide taken over");
        h.assertTrue(BtConfig.fromLegacyForTests("{\"tweaks\":{}}") == null, "nothing to take over");
        h.succeed();
    }

    /** "Enable Simple Building Tools" off: the tab is empty. */
    public static void subModSwitch(GameTestHelper h) {
        com.simplebuilding.framework.api.SubMods.set(SimpleBuildingTools.MOD_ID, false);
        try {
            h.assertFalse(SimpleBuildingTools.on(), "switched off");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleBuildingTools.MOD_ID, true);
        }
        h.assertTrue(SimpleBuildingTools.on(), "switched on again");
        h.succeed();
    }

    /** Every unconditional tool recipe is loaded; the blueprint has one recipe with and one without SimpleBuilding. */
    public static void toolRecipesAreThere(GameTestHelper h) {
        var recipes = h.getLevel().getServer().getRecipeManager();
        List<String> problems = new ArrayList<>();
        boolean sb = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplebuilding");
        for (String[] recipe : SimpleBuildingTools.RECIPES) {
            String path = recipe[0];
            if (path.startsWith("enderite_") || path.startsWith("upgrade_") || path.equals("blueprint")) continue; // conditional
            if (recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleBuildingTools.id(path))).isEmpty()) problems.add(path);
        }
        boolean withSb = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleBuildingTools.id("blueprint"))).isPresent();
        boolean alone = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleBuildingTools.id("blueprint_standalone"))).isPresent();
        if (withSb != sb || alone == sb) problems.add("blueprint recipes: with SB " + withSb + ", alone " + alone + ", SB loaded " + sb);
        boolean upgrades = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleBuildingTools.id("upgrade_copper_building_wand_to_iron_building_wand"))).isPresent();
        if (upgrades != sb) problems.add("tier upgrade recipes need SimpleBuilding's count_based_smithing (present " + upgrades + ")");
        h.assertTrue(problems.isEmpty(), "recipes: " + problems);
        h.succeed();
    }

    /** The Enderite wand is smithed with Simple Ores' Enderite: the recipe exists exactly when Simple Ores is loaded. */
    public static void enderiteWandNeedsSimpleOres(GameTestHelper h) {
        var recipes = h.getLevel().getServer().getRecipeManager();
        boolean ores = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleores");
        boolean present = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleBuildingTools.id("enderite_building_wand_smithing"))).isPresent();
        h.assertValueEqual(present, ores, "enderite wand recipe present = Simple Ores loaded");
        h.succeed();
    }
}
