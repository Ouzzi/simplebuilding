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
    public static final int JAW_MAX_TICKS = 140;

    private TrapTests() {
    }

    /**
     * Besitzer N34: eine Sau in der Kiefer wird genau einmal gebissen (1 Herz), bekommt Dunkelheit, und die Kiefer bleibt
     * geschlossen, solange sie drinsteht (kein Auf-und-Zu, kein zweiter Biss). Ist die Sau weg, oeffnet sie sich wieder.
     * Klangtyp lautlos.
     */
    public static void sculkJawBitesAndReopensSilently(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        helper.assertTrue(ModBlocks.SCULK_JAW.defaultBlockState().getSoundType() == SoundType.EMPTY, "sculk jaw is not silent");
        // Besitzer N38: Blindheit waechst exponentiell mit der Verweildauer (2 s -> 3 s, 3 s -> 5 s, Deckel 60 s).
        helper.assertTrue(SculkJawBlock.blindnessTicks(0) == 60 && SculkJawBlock.blindnessTicks(40) == 60
                        && SculkJawBlock.blindnessTicks(60) == 100 && SculkJawBlock.blindnessTicks(80) == 167
                        && SculkJawBlock.blindnessTicks(20 * 30) == 1200,
                "blindness curve: " + SculkJawBlock.blindnessTicks(40) + "/" + SculkJawBlock.blindnessTicks(60) + "/"
                        + SculkJawBlock.blindnessTicks(80) + "/" + SculkJawBlock.blindnessTicks(600));
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SCULK_JAW);
        Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(1, 1, 1));
        helper.assertTrue(pig != null, "no pig");
        float full = pig.getHealth();
        // 0 = noch nie zu, 1 = zu, 2 = waehrend die Sau drin ist wieder aufgegangen (Flackern)
        int[] phase = {0};
        boolean[] dark = {false};
        boolean[] moved = {false};
        helper.onEachTick(() -> {
            boolean snapped = helper.getBlockState(pos).getValue(SculkJawBlock.SNAPPED);
            if (pig.isAlive() && pig.blockPosition().equals(helper.absolutePos(pos)) && phase[0] == 1 && !snapped) phase[0] = 2;
            if (phase[0] == 0 && snapped) phase[0] = 1;
            if (snapped && helper.getBlockState(pos).getValue(SculkJawBlock.MOTION) > 0) moved[0] = true;
            if (pig.isAlive() && pig.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)) dark[0] = true;
        });
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(phase[0] == 1, phase[0] == 0 ? "the jaw never snapped shut" : "the jaw opened again with the pig still inside");
            helper.assertTrue(Math.abs(pig.getHealth() - (full - SculkJawBlock.BITE_DAMAGE)) < 0.01F,
                    "one bite of " + SculkJawBlock.BITE_DAMAGE + " expected: pig health " + pig.getHealth() + " of " + full);
            helper.assertTrue(dark[0], "the pig in the jaw got no darkness");
            var blind = pig.getEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
            helper.assertTrue(blind != null && blind.getDuration() >= SculkJawBlock.blindnessTicks(60),
                    "after 3+ s in the jaw the pig should stay blind for at least 5 s: " + blind);
            helper.assertTrue(moved[0], "the jaw snapped shut without its closing frames");
            helper.assertTrue(helper.getBlockState(pos).getValue(SculkJawBlock.MOTION) == 0, "the shut jaw is still moving: "
                    + helper.getBlockState(pos));
            pig.discard();
            helper.succeedWhen(() -> {
                helper.assertTrue(helper.getBlockState(pos).is(ModBlocks.SCULK_JAW), "jaw gone: " + helper.getBlockState(pos));
                helper.assertFalse(helper.getBlockState(pos).getValue(SculkJawBlock.SNAPPED), "the empty jaw did not reopen");
                helper.assertTrue(helper.getBlockState(pos).getValue(SculkJawBlock.MOTION) == 0, "the jaw is still opening");
            });
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
