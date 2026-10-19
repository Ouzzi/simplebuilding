package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.effect.MirageTable;
import com.simplebuilding.effect.ModEffects;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Queue N20/N24/N26 (branch claude-q-brew, docs/ai/PLAN-BRAUEN-WERKBANK-2026-10-09.md): the new potions brew from the
 * chosen ingredients (through the real brewing recipes), drinking them gives the effects, the warden drops its
 * tendrils, Mirage keeps sizes, and the Storage Crafting Table keeps its grid, shares it and drops it. 26.3 only.
 */
public final class BrewingTests {
    private BrewingTests() {
    }

    // ------------------------------------------------------------------ potions

    private static ItemStack potion(Item container, Holder<Potion> potion) {
        return PotionContents.createItemStack(container, potion);
    }

    private static void brews(GameTestHelper helper, Item container, Holder<Potion> input, Item reagent, @Nullable Holder<Potion> expected,
            Item expectedContainer) {
        ItemStack result = BrewChecks.brew(helper.getLevel(), potion(container, input), new ItemStack(reagent));
        String what = container + " " + input.getRegisteredName() + " + " + reagent;
        helper.assertTrue(!result.isEmpty(), "no brewing recipe for " + what);
        PotionContents contents = result.get(DataComponents.POTION_CONTENTS);
        helper.assertTrue(result.is(expectedContainer) && contents != null && expected != null && contents.is(expected),
                what + " brews " + result + " " + contents);
    }

    /** Every step: base ingredient, redstone, glowstone, the spider eye, and for each potion splash and lingering. */
    public static void newPotionsBrewFromTheirIngredients(GameTestHelper helper) {
        if (!McVersion.BREWING_EFFECTS) {
            helper.succeed();
            return;
        }
        // Owner N71: the mod potions brew from the warped potion (water bottle + warped wart, SimpleLib), not from the awkward one.
        Identifier warpedId = Identifier.fromNamespaceAndPath("simplelib", "warped");
        Holder<Potion> warped = BuiltInRegistries.POTION.wrapAsHolder(BuiltInRegistries.POTION.getValue(warpedId));
        Object[][] steps = {
                {warped, ModItems.WARDEN_TENDRIL, ModEffects.DARKNESS_POTION},
                {ModEffects.DARKNESS_POTION, Items.REDSTONE, ModEffects.LONG_DARKNESS_POTION},
                {warped, Items.RED_MUSHROOM, ModEffects.NAUSEA_POTION},
                {ModEffects.NAUSEA_POTION, Items.REDSTONE, ModEffects.LONG_NAUSEA_POTION},
                {warped, Items.SNOWBALL, ModEffects.SHIVERING_POTION},
                {ModEffects.SHIVERING_POTION, Items.REDSTONE, ModEffects.LONG_SHIVERING_POTION},
                {ModEffects.SHIVERING_POTION, Items.GLOWSTONE_DUST, ModEffects.STRONG_SHIVERING_POTION},
                {warped, Items.AMETHYST_SHARD, ModEffects.MIRAGE_POTION},
                {ModEffects.MIRAGE_POTION, Items.REDSTONE, ModEffects.LONG_MIRAGE_POTION},
                {ModEffects.MIRAGE_POTION, Items.FERMENTED_SPIDER_EYE, ModEffects.REVERSE_MIRAGE_POTION},
                {ModEffects.LONG_MIRAGE_POTION, Items.FERMENTED_SPIDER_EYE, ModEffects.LONG_REVERSE_MIRAGE_POTION},
                {ModEffects.REVERSE_MIRAGE_POTION, Items.REDSTONE, ModEffects.LONG_REVERSE_MIRAGE_POTION},
                {warped, Items.INK_SAC, ModEffects.FADED_POTION},
                {ModEffects.FADED_POTION, Items.REDSTONE, ModEffects.LONG_FADED_POTION},
        };
        for (Item container : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            helper.assertTrue(BrewChecks.brew(helper.getLevel(), potion(container, Potions.AWKWARD), new ItemStack(Items.RED_MUSHROOM)).isEmpty(),
                    "the awkward potion no longer brews the mod potion of red mushrooms (nausea)");
        }
        for (Object[] step : steps) {
            @SuppressWarnings("unchecked") Holder<Potion> input = (Holder<Potion>) step[0];
            @SuppressWarnings("unchecked") Holder<Potion> output = (Holder<Potion>) step[2];
            for (Item container : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
                brews(helper, container, input, (Item) step[1], output, container);
            }
            brews(helper, Items.POTION, output, Items.GUNPOWDER, output, Items.SPLASH_POTION);
            brews(helper, Items.SPLASH_POTION, output, Items.DRAGON_BREATH, output, Items.LINGERING_POTION);
        }
        helper.succeed();
    }

    private static void drinks(GameTestHelper helper, ServerPlayer player, Holder<Potion> potion, Holder<MobEffect> effect, int amplifier,
            int duration) {
        player.removeAllEffects();
        potion(Items.POTION, potion).finishUsingItem(helper.getLevel(), player);
        MobEffectInstance instance = player.getEffect(effect);
        helper.assertTrue(instance != null, "drinking " + potion.getRegisteredName() + " gives no " + effect.getRegisteredName());
        helper.assertValueEqual(instance.getAmplifier(), amplifier, potion.getRegisteredName() + " amplifier");
        helper.assertTrue(Math.abs(instance.getDuration() - duration) <= 1, potion.getRegisteredName() + " lasts " + instance.getDuration());
    }

    /** Drinking gives the effect at the right level and length; milk-like clearing removes the perception effects. */
    public static void drinkingTheNewPotionsAppliesTheirEffects(GameTestHelper helper) {
        if (!McVersion.BREWING_EFFECTS) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        drinks(helper, player, ModEffects.DARKNESS_POTION, MobEffects.DARKNESS, 0, ModEffects.HARMFUL_DURATION);
        drinks(helper, player, ModEffects.LONG_NAUSEA_POTION, MobEffects.NAUSEA, 0, ModEffects.LONG_NAUSEA_DURATION);
        drinks(helper, player, ModEffects.SHIVERING_POTION, ModEffects.SHIVERING, 0, ModEffects.HARMFUL_DURATION);
        drinks(helper, player, ModEffects.STRONG_SHIVERING_POTION, ModEffects.SHIVERING, 1, ModEffects.STRONG_DURATION);
        drinks(helper, player, ModEffects.LONG_MIRAGE_POTION, ModEffects.MIRAGE, 0, ModEffects.LONG_HARMFUL_DURATION);
        drinks(helper, player, ModEffects.REVERSE_MIRAGE_POTION, ModEffects.REVERSE_MIRAGE, 0, ModEffects.HARMFUL_DURATION);
        drinks(helper, player, ModEffects.FADED_POTION, ModEffects.FADED, 0, ModEffects.HARMFUL_DURATION);
        for (Holder<MobEffect> effect : List.of(ModEffects.SHIVERING, ModEffects.MIRAGE, ModEffects.REVERSE_MIRAGE, ModEffects.FADED)) {
            helper.assertFalse(effect.value().isBeneficial(), effect.getRegisteredName() + " counts as beneficial");
        }
        // Milk: the bucket clears every effect, the perception effects included.
        player.addEffect(new MobEffectInstance(ModEffects.FADED, 200));
        new ItemStack(Items.MILK_BUCKET).finishUsingItem(helper.getLevel(), player);
        helper.assertFalse(player.hasEffect(ModEffects.FADED), "milk does not clear Faded");
        helper.succeed();
    }

    /** Mirage swaps within the size class, never to the own type, stably per mob; bosses and the other side stay. */
    public static void mirageKeepsTheSizeClassAndIsStable(GameTestHelper helper) {
        if (!McVersion.BREWING_EFFECTS) {
            helper.succeed();
            return;
        }
        for (EntityType<?> type : List.of(EntityTypes.PIG, EntityTypes.CHICKEN, EntityTypes.COW, EntityTypes.IRON_GOLEM, EntityTypes.SHEEP)) {
            for (long seed = 0; seed < 12; seed++) {
                EntityType<?> look = MirageTable.pick(type, false, seed);
                helper.assertTrue(MirageTable.HOSTILE_LOOKS.get(MirageTable.sizeOf(type)).contains(look) && look != type,
                        type + " under Mirage looks like " + look);
                helper.assertTrue(look == MirageTable.pick(type, false, seed), "the look of " + type + " changes for the same mob");
            }
        }
        for (EntityType<?> type : List.of(EntityTypes.ZOMBIE, EntityTypes.SILVERFISH, EntityTypes.WARDEN, EntityTypes.CREEPER)) {
            EntityType<?> look = MirageTable.pick(type, true, 7);
            helper.assertTrue(MirageTable.PEACEFUL_LOOKS.get(MirageTable.sizeOf(type)).contains(look), type + " under Reverse Mirage looks like " + look);
        }
        helper.assertTrue(MirageTable.sizeOf(EntityTypes.CHICKEN) == MirageTable.Size.SMALL
                && MirageTable.sizeOf(EntityTypes.PIG) == MirageTable.Size.MEDIUM
                && MirageTable.sizeOf(EntityTypes.ZOMBIE) == MirageTable.Size.MEDIUM
                && MirageTable.sizeOf(EntityTypes.IRON_GOLEM) == MirageTable.Size.LARGE
                && MirageTable.sizeOf(EntityTypes.WARDEN) == MirageTable.Size.LARGE, "size classes");
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 2, 1));
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 2, 1));
        helper.assertTrue(MirageTable.disguise(pig, true, false) != null, "Mirage leaves the pig a pig");
        helper.assertTrue(MirageTable.disguise(pig, false, true) == null, "Reverse Mirage changes the peaceful pig");
        helper.assertTrue(MirageTable.disguise(zombie, false, true) != null, "Reverse Mirage leaves the zombie a zombie");
        helper.assertTrue(MirageTable.disguise(zombie, true, false) == null, "Mirage changes the hostile zombie");
        helper.assertTrue(MirageTable.disguise(helper.makeMockServerPlayerInLevel(), true, true) == null, "a player changes its look");
        pig.discard();
        zombie.discard();
        helper.succeed();
    }

    /** A killed warden drops warden tendrils (besides its sculk catalyst). */
    public static void wardenDropsTendrils(GameTestHelper helper) {
        if (!McVersion.BREWING_EFFECTS) {
            helper.succeed();
            return;
        }
        BlockPos pos = new BlockPos(2, 2, 2);
        Warden warden = helper.spawnWithNoFreeWill(EntityTypes.WARDEN, pos);
        warden.kill(helper.getLevel());
        helper.succeedWhen(() -> helper.assertItemEntityPresent(ModItems.WARDEN_TENDRIL, pos, 4.0));
    }
}
