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
        // Besitzer N38/N41: Dunkelheit (statt Blindheit) waechst exponentiell mit der Verweildauer (2 s -> 3 s, 3 s -> 5 s, Deckel 60 s).
        helper.assertTrue(SculkJawBlock.darknessTicks(0) == 60 && SculkJawBlock.darknessTicks(40) == 60
                        && SculkJawBlock.darknessTicks(60) == 100 && SculkJawBlock.darknessTicks(80) == 167
                        && SculkJawBlock.darknessTicks(20 * 30) == 1200,
                "darkness curve: " + SculkJawBlock.darknessTicks(40) + "/" + SculkJawBlock.darknessTicks(60) + "/"
                        + SculkJawBlock.darknessTicks(80) + "/" + SculkJawBlock.darknessTicks(600));
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
            helper.assertFalse(pig.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "the jaw still blinds (N41: darkness only)");
            var darkness = pig.getEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
            helper.assertTrue(darkness != null && darkness.getDuration() >= SculkJawBlock.darknessTicks(60),
                    "after 3+ s in the jaw the pig should stay in darkness for at least 5 s: " + darkness);
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

    /**
     * N65: das Zuschnappen klingt (Sculk-Sensor-Klick + leiser Shrieker-Schrei, genau einmal je Biss), als Paket an einen
     * Spieler in Hoerweite; die geschlossene Kiefer mit der Sau darin und das Wieder-Oeffnen bleiben still.
     */
    public static void sculkJawClosingMakesSculkSounds(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        java.util.List<net.minecraft.network.protocol.Packet<?>> sink = new java.util.ArrayList<>();
        net.minecraft.server.level.ServerPlayer listener = MusicDiscTests.capturingPlayer(helper, sink);
        net.minecraft.world.phys.Vec3 ear = helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 2.0, 3.5));
        listener.snapTo(ear.x, ear.y, ear.z, 0.0F, 0.0F);
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SCULK_JAW);
        java.util.function.Function<net.minecraft.sounds.SoundEvent, java.util.List<net.minecraft.network.protocol.game.ClientboundSoundPacket>> heard =
                event -> MusicDiscTests.captured(sink, net.minecraft.network.protocol.game.ClientboundSoundPacket.class).stream()
                        .filter(p -> p.getSound().value().equals(event)).toList();
        helper.assertTrue(heard.apply(net.minecraft.sounds.SoundEvents.SCULK_CLICKING).isEmpty(), "sound before any bite");
        Pig pig = helper.spawn(EntityTypes.PIG, pos);
        helper.assertTrue(pig != null, "no pig");
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(helper.getBlockState(pos).getValue(SculkJawBlock.SNAPPED), "the jaw never snapped shut");
            var clicks = heard.apply(net.minecraft.sounds.SoundEvents.SCULK_CLICKING);
            var shrieks = heard.apply(net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK);
            helper.assertValueEqual(clicks.size(), 1, "sculk sensor clicks for one bite");
            helper.assertValueEqual(shrieks.size(), 1, "shrieker shrieks for one bite");
            helper.assertTrue(Math.abs(clicks.get(0).getPitch() - SculkJawBlock.CLOSE_CLICK_PITCH) < 1.0e-4F
                    && Math.abs(clicks.get(0).getVolume() - SculkJawBlock.CLOSE_CLICK_VOLUME) < 1.0e-4F, "click pitch/volume");
            helper.assertTrue(shrieks.get(0).getVolume() < clicks.get(0).getVolume(), "the shriek should be quieter than the click");
            pig.discard();
            helper.succeedWhen(() -> {
                helper.assertFalse(helper.getBlockState(pos).getValue(SculkJawBlock.SNAPPED), "the empty jaw did not reopen");
                helper.assertValueEqual(MusicDiscTests.captured(sink, net.minecraft.network.protocol.game.ClientboundSoundPacket.class).size(), 2,
                        "sound packets after reopening (the reopening stays silent)");
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
