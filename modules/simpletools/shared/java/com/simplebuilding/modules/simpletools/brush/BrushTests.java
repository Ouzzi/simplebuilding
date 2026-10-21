package com.simplebuilding.modules.simpletools.brush;

import com.simplebuilding.framework.api.Protection;
import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplebuilding.modules.simpletools.ToolsRegistry;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

/** Biome Brush GameTests (moved from Simple Mobs on 21.10.); Fabric wraps them in ModuleGameTest, NeoForge/Forge register them via ToolsTests.ALL. */
public final class BrushTests {
    private BrushTests() {}

    private static ItemStack brush() {
        return new ItemStack(ToolsRegistry.BIOME_BRUSH);
    }

    /** The result of crafting a 3 x 3 grid, or empty. */
    private static ItemStack craft(GameTestHelper h, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static boolean recipe(GameTestHelper h, String path) {
        return h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(SimpleTools.MOD_ID, path))).isPresent();
    }

    private static ResourceKey<Biome> cellBiome(GameTestHelper h, BlockPos rel) {
        BlockPos p = h.absolutePos(rel);
        return h.getLevel().getNoiseBiome(QuartPos.fromBlock(p.getX()), QuartPos.fromBlock(p.getY()), QuartPos.fromBlock(p.getZ()))
                .unwrapKey().orElseThrow();
    }

    /** A fresh survival-like player (mock players are creative; instabuild off means no infinite materials). */
    private static ServerPlayer painter(GameTestHelper h, ItemStack brush, ItemStack material) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getAbilities().instabuild = false;
        p.setItemInHand(InteractionHand.MAIN_HAND, brush);
        p.setItemInHand(InteractionHand.OFF_HAND, material);
        return p;
    }

    /** Test worlds are kept between runs: paint the area back to Plains first (two cells around, no checks). */
    private static void reset(GameTestHelper h, BlockPos rel) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        var plains = BiomeBrush.biome(h.getLevel(), Biomes.PLAINS.identifier());
        BiomeBrush.paint(h.getLevel(), p, h.absolutePos(rel), plains, 2);
        h.assertTrue(cellBiome(h, rel) == Biomes.PLAINS, "reset to plains");
    }

    private static BiomeBrush.Result stroke(GameTestHelper h, ServerPlayer p, BlockPos rel) {
        return BiomeBrushItem.stroke(h.getLevel(), p, h.absolutePos(rel), p.getMainHandItem(), p.getOffhandItem());
    }

    public static void addAll(Map<String, Consumer<GameTestHelper>> ALL) {
        ALL.put("biome_brush_item", h -> {
            ItemStack brush = brush();
            h.assertTrue(brush.getMaxDamage() == 512, "durability 512");
            h.assertTrue(brush.isValidRepairItem(new ItemStack(Items.NETHERITE_INGOT)), "repaired with Netherite Ingots");
            h.assertTrue(brush.get(DataComponents.ENCHANTABLE) != null
                    && brush.is(TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("enchantable/durability"))), "takes Unbreaking and Mending");
            var entries = BiomeBrush.entries(h.getLevel().getServer());
            h.assertTrue(entries.size() >= 20, "biome materials loaded from data: " + entries.size());
            h.assertTrue(BiomeBrush.material(h.getLevel().getServer(), new ItemStack(Items.SAND)).map(e -> e.biome().equals(Biomes.DESERT.identifier())).orElse(false), "sand paints desert");
            h.assertTrue(BiomeBrush.material(h.getLevel().getServer(), new ItemStack(Items.DIRT)).isEmpty(), "dirt paints nothing");
            h.succeed();
        });
        ALL.put("biome_brush_paint", h -> {
            ServerLevel level = h.getLevel();
            BlockPos at = new BlockPos(3, 2, 3);
            level.setBlock(h.absolutePos(at), Blocks.STONE.defaultBlockState(), 3);
            reset(h, at);
            // snow -> snowy plains: the clicked cell and its neighbours change, one snow and one durability point are used
            ServerPlayer p = painter(h, brush(), new ItemStack(Items.SNOW_BLOCK, 4));
            var first = stroke(h, p, at);
            h.assertTrue(first == BiomeBrush.Result.PAINTED, "stroke painted: " + first + " (was " + cellBiome(h, at) + ")");
            h.assertTrue(cellBiome(h, at) == Biomes.SNOWY_PLAINS, "clicked cell is snowy plains: " + cellBiome(h, at));
            h.assertTrue(cellBiome(h, at.offset(4, 0, 0)) == Biomes.SNOWY_PLAINS, "neighbouring cell too (radius 1 cell)");
            h.assertTrue(p.getOffhandItem().getCount() == 3, "one material per stroke: " + p.getOffhandItem().getCount());
            h.assertTrue(p.getMainHandItem().getDamageValue() == 1, "one durability point per stroke");
            ServerPlayer again = painter(h, brush(), new ItemStack(Items.SNOW_BLOCK, 4));
            h.assertTrue(stroke(h, again, at) == BiomeBrush.Result.UNCHANGED && again.getOffhandItem().getCount() == 4, "same biome again: nothing used");
            // a Nether material in the Overworld: refused, nothing used
            ServerPlayer q = painter(h, brush(), new ItemStack(Items.NETHERRACK, 2));
            h.assertTrue(stroke(h, q, at) == BiomeBrush.Result.WRONG_DIMENSION && q.getOffhandItem().getCount() == 2, "Nether biome refused in the Overworld");
            ServerPlayer r = painter(h, brush(), new ItemStack(Items.DIRT, 2));
            h.assertTrue(stroke(h, r, at) == BiomeBrush.Result.NO_MATERIAL && r.getMainHandItem().getDamageValue() == 0, "no biome material: nothing");
            // creative: no material, no durability
            ServerPlayer c = painter(h, brush(), new ItemStack(Items.SAND, 1));
            c.getAbilities().instabuild = true;
            h.assertTrue(stroke(h, c, at) == BiomeBrush.Result.PAINTED && cellBiome(h, at) == Biomes.DESERT, "sand paints desert");
            h.assertTrue(c.getOffhandItem().getCount() == 1 && c.getMainHandItem().getDamageValue() == 0, "creative uses nothing");
            // the last durability point breaks the brush
            ItemStack worn = brush();
            worn.setDamageValue(worn.getMaxDamage() - 1);
            ServerPlayer b = painter(h, worn, new ItemStack(Items.MYCELIUM, 1));
            h.assertTrue(stroke(h, b, at) == BiomeBrush.Result.PAINTED && b.getMainHandItem().isEmpty(), "brush breaks at 0 durability");
            h.succeed();
        });
        ALL.put("biome_brush_sneak", h -> {
            BlockPos at = new BlockPos(3, 2, 3);
            reset(h, at);
            // sneaking: only the clicked cell changes, the neighbouring cell keeps its biome
            ServerPlayer p = painter(h, brush(), new ItemStack(Items.SNOW_BLOCK, 2));
            p.setShiftKeyDown(true);
            h.assertTrue(stroke(h, p, at) == BiomeBrush.Result.PAINTED, "sneaking stroke painted");
            h.assertTrue(cellBiome(h, at) == Biomes.SNOWY_PLAINS, "clicked cell is snowy plains: " + cellBiome(h, at));
            h.assertTrue(cellBiome(h, at.offset(4, 0, 0)) == Biomes.PLAINS, "neighbouring cell unchanged: " + cellBiome(h, at.offset(4, 0, 0)));
            h.assertTrue(p.getOffhandItem().getCount() == 1, "one material per stroke");
            h.succeed();
        });
        ALL.put("biome_brush_claims", h -> {
            ServerLevel level = h.getLevel();
            BlockPos at = new BlockPos(3, 2, 3);
            reset(h, at);
            ServerPlayer p = painter(h, brush(), new ItemStack(Items.SNOW_BLOCK, 4));
            var first = stroke(h, p, at);
            h.assertTrue(first == BiomeBrush.Result.PAINTED && cellBiome(h, at) == Biomes.SNOWY_PLAINS, "unclaimed: painted: " + first + " " + cellBiome(h, at));
            String id = "simpletools_test_claim";
            Object server = level.getServer();
            try {
                Protection.register(server, id, t -> false);
                ServerPlayer q = painter(h, brush(), new ItemStack(Items.MYCELIUM, 4));
                h.assertTrue(stroke(h, q, at) == BiomeBrush.Result.DENIED, "claimed: refused");
                h.assertTrue(cellBiome(h, at) == Biomes.SNOWY_PLAINS, "claimed: biome unchanged");
                h.assertTrue(q.getOffhandItem().getCount() == 4 && q.getMainHandItem().getDamageValue() == 0, "claimed: nothing used");
                // a claim on part of the stroke: only the free cells change
                BlockPos claimed = h.absolutePos(at.offset(4, 0, 0));
                int cx = QuartPos.fromBlock(claimed.getX());
                Protection.unregister(server, id);
                Protection.register(server, id, t -> QuartPos.fromBlock(t.x()) != cx);
                ServerPlayer s = painter(h, brush(), new ItemStack(Items.MYCELIUM, 4));
                h.assertTrue(stroke(h, s, at) == BiomeBrush.Result.PAINTED && cellBiome(h, at) == Biomes.MUSHROOM_FIELDS, "free cells painted");
                h.assertTrue(cellBiome(h, at.offset(4, 0, 0)) == Biomes.SNOWY_PLAINS, "claimed cell keeps its biome");
            } finally {
                Protection.unregister(server, id);
            }
            // adventure mode cannot paint
            ServerPlayer adv = painter(h, brush(), new ItemStack(Items.SAND, 4));
            adv.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            h.assertTrue(stroke(h, adv, at) == BiomeBrush.Result.DENIED, "adventure mode: refused");
            boolean ops = SimpleTools.config().biomeBrushOpsOnly;
            try {
                SimpleTools.config().biomeBrushOpsOnly = true;
                ServerPlayer o = painter(h, brush(), new ItemStack(Items.SNOW_BLOCK, 4));
                var res = stroke(h, o, at);
                h.assertTrue(res == BiomeBrush.Result.DISABLED || o.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER), "ops only: refused for players: " + res);
            } finally {
                SimpleTools.config().biomeBrushOpsOnly = ops;
            }
            h.succeed();
        });
        ALL.put("biome_brush_recipes", h -> {
            // owner 21.10.: 3 Nether Stars + Netherite Nugget + Brush; with Simple Mobs instead its Nether Heart + nugget + brush
            var heart = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("simplemobs", "nether_heart"))
                    .filter(i -> BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("simplemobs"));
            var nugget = BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "nuggets/netherite"))).iterator();
            h.assertTrue(nugget.hasNext(), "a Netherite nugget exists (SimpleLib material)");
            ItemStack n = new ItemStack(nugget.next()), b = new ItemStack(Items.BRUSH), s = new ItemStack(Items.NETHER_STAR), e = ItemStack.EMPTY;
            ItemStack fromStars = craft(h, List.of(s, s, s, e, n, e, e, b, e));
            if (heart.isEmpty()) {
                h.assertTrue(recipe(h, "biome_brush") && !recipe(h, "biome_brush_nether_heart"), "[standalone] only the Nether Star recipe");
                h.assertTrue(fromStars.is(ToolsRegistry.BIOME_BRUSH), "3 Nether Stars + Netherite Nugget + Brush -> Biome Brush");
            } else {
                h.assertTrue(!recipe(h, "biome_brush") && recipe(h, "biome_brush_nether_heart"), "with Simple Mobs: only the Nether Heart recipe");
                h.assertTrue(fromStars.isEmpty(), "with Simple Mobs: no Nether Star recipe");
                ItemStack hs = new ItemStack(heart.get());
                h.assertTrue(craft(h, List.of(e, hs, e, e, n, e, e, b, e)).is(ToolsRegistry.BIOME_BRUSH), "Nether Heart + Netherite Nugget + Brush -> Biome Brush");
            }
            h.assertTrue(craft(h, List.of(e, e, e, e, new ItemStack(Items.NETHERITE_INGOT), e, e, b, e)).isEmpty(), "no ingot-only recipe");
            h.succeed();
        });
        ALL.put("biome_brush_rarity", h -> {
            // owner 21.10.: the Biome Brush is cursed (SimpleLib tier above Epic)
            ItemStack brush = brush();
            h.assertTrue(com.simplelib.api.SimpleRarity.of(brush) == com.simplelib.api.SimpleRarity.CURSED, "cursed tier");
            h.assertTrue(brush.getRarity() == net.minecraft.world.item.Rarity.EPIC, "Epic fallback");
            h.assertTrue(Integer.valueOf(com.simplelib.api.SimpleRarity.CURSED.shadow()).equals(brush.getHoverName().getStyle().getShadowColor()), "black shadow");
            h.succeed();
        });
        ALL.put("biome_brush_moved", h -> {
            // worlds from before 21.10.: the Simple Mobs id finds the Simple Tools brush (SimpleLib IdMoves)
            Item old = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplemobs", "biome_brush"));
            h.assertTrue(old == ToolsRegistry.BIOME_BRUSH, "simplemobs:biome_brush -> simpletools:biome_brush: " + old);
            h.assertTrue(BuiltInRegistries.ITEM.getKey(ToolsRegistry.BIOME_BRUSH).equals(ToolsRegistry.BIOME_BRUSH_ID), "saved under the new id");
            h.succeed();
        });
    }
}
