package com.simplelib.test;

import com.simplelib.SimpleLib;
import com.simplelib.api.SimpleLibApi;
import com.simplelib.registry.LibMaterials;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Shared materials (split stage 3d): SimpleLib always registers them; their recipes and the anvil crushing exist only
 * when a loaded mod requested the group. Standalone nothing asks, so nothing reaches the world; SimpleBuilding asks for
 * every group (looked up by id, principle 3).
 */
public final class MaterialRequestTests {
    /** Recipe id -> group, as tools/gen_resources.py writes them. */
    static final Map<String, String> RECIPES = Map.ofEntries(
            Map.entry("cracked_diamond", "cracked_diamond"), Map.entry("diamond_from_blasting_cracked_diamond", "cracked_diamond"),
            Map.entry("cracked_diamond_block", "cracked_diamond"), Map.entry("cracked_diamond_from_cracked_diamond_block", "cracked_diamond"),
            Map.entry("basic_upgrade_template", "upgrade_templates"), Map.entry("enderite_upgrade_template", "upgrade_templates"),
            Map.entry("netherite_nugget", "netherite_nugget"), Map.entry("netherite_ingot", "netherite_nugget"),
            Map.entry("enderite_nugget_from_ingot", "enderite"), Map.entry("enderite_ingot_from_nugget", "enderite"),
            Map.entry("layered_raw_enderite", "enderite"), Map.entry("enderite_scrap_from_blasting_layered_raw_enderite", "enderite"),
            Map.entry("enderite_ingot_from_scrap", "enderite"), Map.entry("enderite_block", "enderite"), Map.entry("enderite_ingot", "enderite"),
            Map.entry("polished_end_stone", "polished_end_stone"), Map.entry("polished_end_stone_from_end_stone_stonecutting", "polished_end_stone"),
            Map.entry("polished_end_stone_from_end_stone_bricks_stonecutting", "polished_end_stone"),
            Map.entry("polished_end_stone_stairs", "polished_end_stone"), Map.entry("polished_end_stone_slab", "polished_end_stone"),
            Map.entry("polished_end_stone_wall", "polished_end_stone"),
            Map.entry("polished_end_stone_stairs_from_polished_end_stone_stonecutting", "polished_end_stone"),
            Map.entry("polished_end_stone_slab_from_polished_end_stone_stonecutting", "polished_end_stone"),
            Map.entry("polished_end_stone_wall_from_polished_end_stone_stonecutting", "polished_end_stone"));

    private MaterialRequestTests() {
    }

    private static boolean simplebuilding() {
        return BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("simplebuilding", "iron_sledgehammer"));
    }

    /** Every material is registered under simplelib, whoever asked; the blocks have their block items; the groups match. */
    public static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, List<String>> group : LibMaterials.GROUPS.entrySet()) {
            for (String id : group.getValue()) {
                Item item = BuiltInRegistries.ITEM.getValue(SimpleLib.id(id));
                if (item == null || item != SimpleLibApi.materialItem(id)) problems.add("item " + id + " is not registered");
            }
        }
        for (String block : List.of("cracked_diamond_block", "enderite_block", "polished_end_stone",
                "polished_end_stone_stairs", "polished_end_stone_slab", "polished_end_stone_wall")) {
            if (!(SimpleLibApi.materialItem(block) instanceof BlockItem item) || item.getBlock() != SimpleLibApi.materialBlock(block)
                    || BuiltInRegistries.BLOCK.getValue(SimpleLib.id(block)) != SimpleLibApi.materialBlock(block)) {
                problems.add("block " + block + " has no matching block item");
            }
        }
        if (!new ItemStack(SimpleLibApi.materialItem("enderite_ingot")).has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT)) {
            problems.add("the enderite ingot is not fire resistant");
        }
        TagKey<Item> ingots = TagKey.create(Registries.ITEM, Identifier.parse("c:ingots/enderite"));
        if (!new ItemStack(SimpleLibApi.materialItem("enderite_ingot")).is(ingots)) problems.add("c:ingots/enderite misses the ingot");
        TagKey<Item> nuggets = TagKey.create(Registries.ITEM, Identifier.parse("c:nuggets/netherite"));
        if (!new ItemStack(SimpleLibApi.materialItem("netherite_nugget")).is(nuggets)) problems.add("c:nuggets/netherite misses the nugget");
        boolean sb = simplebuilding();
        for (String group : LibMaterials.GROUPS.keySet()) {
            if (SimpleLibApi.materialRequested(group) != sb) problems.add("group " + group + " requested=" + SimpleLibApi.materialRequested(group) + " with SimpleBuilding=" + sb);
        }
        h.assertTrue(problems.isEmpty(), "shared materials: " + problems);
        h.succeed();
    }

    /** A material recipe exists exactly when its group was requested (condition simplelib:material_requested). */
    public static void recipesFollowRequests(GameTestHelper h) {
        var recipes = h.getLevel().getServer().getRecipeManager();
        List<String> problems = new ArrayList<>();
        RECIPES.forEach((id, group) -> {
            boolean present = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleLib.id(id))).isPresent();
            if (present != SimpleLibApi.materialRequested(group)) {
                problems.add(id + (present ? " exists although " : " is missing although ") + group + " requested=" + SimpleLibApi.materialRequested(group));
            }
        });
        h.assertTrue(problems.isEmpty(), "material recipes: " + problems);
        h.succeed();
    }

    /** An anvil falling onto a diamond block crushes it into 72 pebbles only while cracked_diamond is requested. */
    public static void anvilCrushingFollowsRequest(GameTestHelper h) {
        boolean on = SimpleLibApi.materialRequested("cracked_diamond");
        BlockPos target = new BlockPos(1, 2, 1);
        h.setBlock(target.below(), Blocks.STONE);
        h.setBlock(target, Blocks.DIAMOND_BLOCK);
        BlockPos start = target.above(3);
        h.setBlock(start, Blocks.ANVIL);
        FallingBlockEntity.fall(h.getLevel(), h.absolutePos(start), Blocks.ANVIL.defaultBlockState());
        Item pebble = SimpleLibApi.materialItem("diamond_pebble");
        h.startSequence().thenExecuteAfter(40, () -> {
            int pebbles = 0;
            for (ItemEntity drop : h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds())) {
                if (drop.getItem().is(pebble)) pebbles += drop.getItem().getCount();
            }
            h.assertValueEqual(pebbles, on ? SimpleLibApi.anvilPebbles() : 0, "pebbles with cracked_diamond requested=" + on);
            h.assertTrue(h.getBlockState(target).is(Blocks.DIAMOND_BLOCK) != on, "the diamond block is consumed only when requested");
        }).thenSucceed();
    }
}
