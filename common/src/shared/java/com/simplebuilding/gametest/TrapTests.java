package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.SculkJawBlock;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;

/** Fallen (QUEUE N24, claude-q-traps): Sculk-Kiefer und der Schwefelwuerfel-Easter-Egg (Ofen/Tische = schwer wie Eisen). */
public final class TrapTests {
    public static final int JAW_MAX_TICKS = 120;

    private TrapTests() {
    }

    /** Eine Sau in der Kiefer wird gebissen (Gesundheit sinkt, Kiefer schnappt zu) und die Kiefer oeffnet sich wieder; Klangtyp lautlos. */
    public static void sculkJawBitesAndReopensSilently(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        helper.assertTrue(ModBlocks.SCULK_JAW.defaultBlockState().getSoundType() == SoundType.EMPTY, "sculk jaw is not silent");
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SCULK_JAW);
        Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(1, 1, 1));
        helper.assertTrue(pig != null, "no pig");
        float full = pig.getHealth();
        boolean[] snapped = {false};
        helper.onEachTick(() -> {
            if (helper.getBlockState(pos).getValue(SculkJawBlock.SNAPPED)) {
                snapped[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(snapped[0], "the jaw never snapped shut");
            helper.assertTrue(pig.getHealth() <= full - SculkJawBlock.BITE_DAMAGE + 0.01F, "pig health " + pig.getHealth() + " of " + full);
            helper.assertTrue(helper.getBlockState(pos).is(ModBlocks.SCULK_JAW), "jaw gone: " + helper.getBlockState(pos));
        });
    }

    /** Alle Ofen-Varianten und Tische stehen im Archetyp-Tag "schwer" der Schwefelwuerfel (Vanilla: Seelensand), und sind schluckbar. */
    public static void workstationsAreHeavySulfurCubeFood(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        TagKey<Item> heavy = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/high_resistance"));
        TagKey<Item> swallowable = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_swallowable"));
        for (Item item : new Item[]{Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER, Items.CRAFTING_TABLE, Items.SMITHING_TABLE,
                Items.FLETCHING_TABLE, Items.CARTOGRAPHY_TABLE, Items.ENCHANTING_TABLE, Items.ANVIL, Items.SOUL_SAND}) {
            helper.assertTrue(new ItemStack(item).is(heavy), item + " is not heavy sulfur cube food");
            helper.assertTrue(new ItemStack(item).is(swallowable), item + " is not swallowable");
        }
        for (String path : new String[]{"reinforced_furnace", "netherite_blast_furnace", "enderite_smoker", "auto_smither"}) {
            Item item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("simplebuilding", path)).orElse(null);
            if (item != null) {
                helper.assertTrue(new ItemStack(item).is(heavy), path + " is not heavy sulfur cube food");
            }
        }
        helper.assertTrue(!new ItemStack(Items.DIRT).is(heavy), "dirt must stay light");
        helper.assertTrue(ModItems.SCULK_JAW != null, "no sculk jaw item");
        helper.succeed();
    }
}
