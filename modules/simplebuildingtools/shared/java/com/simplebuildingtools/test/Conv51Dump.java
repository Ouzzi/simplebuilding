package com.simplebuildingtools.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplebuildingtools.blueprint.BlueprintCode;
import com.simplebuildingtools.blueprint.BlueprintModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * N51 (b-conv51b): data for the wiki converter pages ("Bauwerke"). Every registered block with its default state, its properties
 * and whether it has an item, plus test vectors {@code BlueprintCode.serialize} of fixed models (the wiki's JavaScript serializer
 * must write the same text). Always checks that each vector parses back to its model; writes the JSON only when the environment
 * variable {@code SB_CONV51_DUMP} names a file (tools/conv51/make_data.py and wiki/tests/conv51 read it).
 */
public final class Conv51Dump {
    private Conv51Dump() {
    }

    public static void dump(GameTestHelper helper) {
        JsonObject blocks = new JsonObject();
        List<Block> all = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.BLOCK.keySet().stream().sorted(java.util.Comparator.comparing(Identifier::toString)).toList()) {
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            all.add(block);
            JsonObject entry = new JsonObject();
            entry.addProperty("d", props(block.defaultBlockState()));
            JsonObject values = new JsonObject();
            for (Property<?> property : block.getStateDefinition().getProperties()) {
                JsonArray list = new JsonArray();
                values(property).forEach(list::add);
                values.add(property.getName(), list);
            }
            entry.add("p", values);
            entry.addProperty("item", block.asItem() != Items.AIR);
            blocks.add(id.toString(), entry);
        }

        Map<String, BlueprintModel> models = new LinkedHashMap<>();
        BlueprintCode.ParseResult hut = BlueprintCode.parse(String.join("\n",
                "$treppe = oak_stairs[facing=east,half=top]",
                "minecraft:cobblestone 0..4,0,0..4",
                "oak_planks 0..4,1..2,0..4",
                "air 1..3,1..2,1..3",
                "$treppe 0..4,3,0",
                "oak_fence 0,4,0*3@2,0,0"));
        helper.assertTrue(hut.ok(), "conv51 hut did not parse: " + hut.problems());
        models.put("hut", hut.model());
        Random random = new Random(51);
        List<BlockState> palette = new ArrayList<>();
        for (String id : List.of("stone", "oak_stairs", "oak_slab", "glass", "oak_log", "redstone_wire", "oak_fence", "cobblestone_wall",
                "white_bed", "oak_door", "lantern", "chest", "hopper", "observer", "torch", "wall_torch", "spruce_trapdoor", "oak_leaves",
                "lever", "repeater", "iron_bars", "chain", "candle", "sculk_sensor", "bamboo", "vine")) {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
            palette.add(block.defaultBlockState());
            palette.add(randomState(block, random));
            palette.add(randomState(block, random));
        }
        for (int n = 0; n < 8; n++) {
            BlueprintModel m = new BlueprintModel();
            int sx = 1 + random.nextInt(12), sy = 1 + random.nextInt(8), sz = 1 + random.nextInt(12), kinds = 1 + random.nextInt(6);
            List<BlockState> use = new ArrayList<>();
            for (int k = 0; k < kinds; k++) {
                use.add(palette.get(random.nextInt(palette.size())));
            }
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    for (int x = 0; x < sx; x++) {
                        if (random.nextInt(10) < 7) {
                            m.set(x, y, z, use.get(random.nextInt(use.size())));
                        }
                    }
                }
            }
            models.put("random_" + n, m);
        }
        // long states scattered: line wrap at 100 characters and aliases ($a, $b ...)
        BlueprintModel wrap = new BlueprintModel();
        BlockState stairs = randomState(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("mossy_stone_brick_stairs")), random);
        BlockState wall = randomState(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("polished_blackstone_brick_wall")), random);
        for (int i = 0; i < 400; i++) {
            wrap.set(random.nextInt(40), random.nextInt(6), random.nextInt(40), i % 3 == 0 ? wall : stairs);
        }
        wrap.set(0, 0, 0, BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("stone")).defaultBlockState());
        models.put("wrap_alias", wrap);
        // every block: once in its default state, once in a random state (the default table of the wiki must match the game)
        BlueprintModel defaults = new BlueprintModel(), randoms = new BlueprintModel();
        int i = 0;
        for (Block block : all) {
            if (block.defaultBlockState().isAir()) {
                continue;
            }
            int x = i % 32, z = (i / 32) % 32, y = i / 1024;
            defaults.set(x, y, z, block.defaultBlockState());
            randoms.set(x, y, z, randomState(block, random));
            i++;
        }
        models.put("every_block_default", defaults);
        models.put("every_block_random", randoms);

        JsonArray vectors = new JsonArray();
        for (Map.Entry<String, BlueprintModel> e : models.entrySet()) {
            BlueprintModel m = e.getValue();
            String code = BlueprintCode.serialize(m);
            BlueprintCode.ParseResult back = BlueprintCode.parse(code);
            helper.assertTrue(back.ok() && back.model().equals(m), "conv51 vector " + e.getKey() + " does not parse back: " + back.problems());
            Map<BlockState, Integer> index = new LinkedHashMap<>();
            JsonArray states = new JsonArray(), cells = new JsonArray();
            for (int key : m.sortedKeys()) {
                BlockState state = m.blocks().get(key);
                Integer at = index.get(state);
                if (at == null) {
                    at = index.size();
                    index.put(state, at);
                    states.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()) + "[" + props(state) + "]");
                }
                JsonArray cell = new JsonArray();
                cell.add(BlueprintModel.keyX(key));
                cell.add(BlueprintModel.keyY(key));
                cell.add(BlueprintModel.keyZ(key));
                cell.add(at);
                cells.add(cell);
            }
            JsonObject v = new JsonObject();
            v.addProperty("name", e.getKey());
            v.add("states", states);
            v.add("cells", cells);
            v.addProperty("code", code);
            vectors.add(v);
        }

        String target = System.getenv("SB_CONV51_DUMP");
        if (target != null && !target.isBlank()) {
            JsonObject root = new JsonObject();
            root.addProperty("source", "simplebuildingtools Conv51Dump (gametest module_game_test_converter_data_dump)");
            root.add("blocks", blocks);
            root.add("vectors", vectors);
            try {
                Files.writeString(Path.of(target), new GsonBuilder().disableHtmlEscaping().create().toJson(root));
            } catch (java.io.IOException err) {
                helper.fail("conv51 dump not written: " + err);
            }
        }
        helper.succeed();
    }

    /** All properties as {@code k=v,...} in the order of {@code state.getProperties()} (what {@code BlueprintCode.spec} walks). */
    private static String props(BlockState state) {
        StringBuilder b = new StringBuilder();
        for (Property<?> property : state.getProperties()) {
            if (!b.isEmpty()) {
                b.append(',');
            }
            b.append(property.getName()).append('=').append(name(state, property));
        }
        return b.toString();
    }

    private static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> List<String> values(Property<T> property) {
        return property.getPossibleValues().stream().map(property::getName).toList();
    }

    private static BlockState randomState(Block block, Random random) {
        List<BlockState> states = block.getStateDefinition().getPossibleStates();
        return states.get(random.nextInt(states.size()));
    }
}
