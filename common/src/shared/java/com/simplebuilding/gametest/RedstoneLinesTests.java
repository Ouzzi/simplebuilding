package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.datafix.LegacyItemIds;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Astral/Nihil redstone line and its gametests moved to Simple Ores ({@code com.simpleores.line.test.LineTests});
 * what stays here needs core-only {@link LegacyItemIds}.
 */
public final class RedstoneLinesTests {
    private RedstoneLinesTests() {
    }

    private static boolean active(GameTestHelper helper) {
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) return true;
        helper.succeed();
        return false;
    }

    /**
     * The removed Astral/Nihil switches load as levers; the lamps are back (Nachtrag 44) under their old ids, so old
     * stacks and placed lamps load as the new Astral/Nihilith Lamp - a saved pre-Nachtrag-36 state keeps its power.
     */
    public static void oldSwitchesBecomeLeversLampsReturn(GameTestHelper helper) {
        if (!active(helper)) return;
        if (ModBlocks.ASTRALIT_LAMP == null || ModBlocks.NIHILITH_LAMP == null) { helper.succeed(); return; } // 26.2: no Simple Ores line
        var items = BuiltInRegistries.ITEM;
        var blocks = BuiltInRegistries.BLOCK;
        for (String sw : List.of("astralit_switch", "nihilith_switch")) {
            Identifier old = Identifier.fromNamespaceAndPath("simplebuilding", sw);
            helper.assertTrue(blocks.getValue(old) == Blocks.LEVER && blocks.get(old).orElseThrow().value() == Blocks.LEVER,
                    "old block " + old + " does not load as a lever");
            helper.assertTrue(items.getValue(old) == Items.LEVER && items.containsKey(old), "old item " + old + " does not load as a lever");
            helper.assertTrue(LegacyItemIds.renamedTo(old) != null, "no item alias for " + old);
        }
        for (var entry : java.util.Map.of("astralit_lamp", ModBlocks.ASTRALIT_LAMP, "nihilith_lamp", ModBlocks.NIHILITH_LAMP).entrySet()) {
            Identifier id = Identifier.fromNamespaceAndPath("simplebuilding", entry.getKey());
            // CIRC46: the lamps are Simple Ores blocks now; the old SimpleBuilding id still loads them.
            Identifier canonical = Identifier.fromNamespaceAndPath("simpleores", entry.getKey());
            helper.assertTrue(blocks.getValue(id) == entry.getValue() && BuiltInRegistries.BLOCK.getKey(entry.getValue()).equals(canonical),
                    id + " is not the lamp block again: " + blocks.getValue(id));
            helper.assertTrue(items.getValue(id) == entry.getValue().asItem() && entry.getValue().asItem() != Items.AIR,
                    id + " is not the lamp item again: " + items.getValue(id));
            helper.assertTrue(LegacyItemIds.renamedTo(id) == null, "a stale alias still moves " + id);
            helper.assertTrue(LegacyItemIds.renamedIn(BuiltInRegistries.BLOCK, id) == null, "a stale block alias still moves " + id);
        }
        // An old saved lamp state (only the property power, like before Nachtrag 36) decodes to the new lamp, unlit.
        var tag = (net.minecraft.nbt.CompoundTag) BlockState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,
                ModBlocks.ASTRALIT_LAMP.defaultBlockState().setValue(BlockStateProperties.POWER, 7)).getOrThrow();
        for (String key : List.copyOf(tag.keySet())) {
            if (tag.get(key) instanceof net.minecraft.nbt.CompoundTag props) props.remove("lit");
        }
        BlockState decoded = BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).result().orElse(null);
        helper.assertTrue(decoded != null && decoded.is(ModBlocks.ASTRALIT_LAMP) && decoded.getValue(BlockStateProperties.POWER) == 7
                && !decoded.getValue(BlockStateProperties.LIT), "saved old astralit_lamp state " + tag + " decoded as " + decoded);
        helper.succeed();
    }
}
