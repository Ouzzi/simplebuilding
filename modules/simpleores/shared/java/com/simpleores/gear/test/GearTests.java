package com.simpleores.gear.test;

import com.simpleores.OresRegistry;
import com.simpleores.SimpleOres;
import com.simpleores.block.MetalRodBlock;
import com.simpleores.gear.EnderiteGear;
import com.simpleores.gear.EnderiteSinkDamper;
import com.simpleores.gear.GearIds;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gametests of the Enderite gear, the Netherite/Enderite food and the five lightning poles (N47-M3, b-circ46, 14.11.2026): the numbers are the
 * ones SimpleBuilding's core had before the move (ModToolMaterials/ModArmorMaterials.ENDERITE, ModItems food, MetalRodBlock). Registered in
 * {@link com.simpleores.test.OresTests} ("gear_" prefix; the Fabric adapter methods are the camel case of the names).
 */
public final class GearTests {
    private GearTests() {
    }

    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> maxTicks) {
        all.put("gear_values_equal_the_old_core_values", GearTests::valuesEqualTheOldCoreValues);
        all.put("gear_food_effects_equal_the_old_core_values", GearTests::foodEffectsEqualTheOldCoreValues);
        all.put("gear_poles_pull_lightning_within_their_range", GearTests::polesPullLightningWithinTheirRange);
        maxTicks.put("gear_poles_pull_lightning_within_their_range", 100);
        all.put("gear_old_simplebuilding_ids_load_as_simple_ores_ids", GearTests::oldSimpleBuildingIdsLoadAsSimpleOresIds);
        all.put("gear_enderite_armor_dampens_sneaking_fall", GearTests::enderiteArmorDampensASneakingFall);
    }

    private static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(SimpleOres.id(path)));
    }

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    /** Tool and armor material, every piece's durability, the equipment asset and the fire resistance: as the core had them. */
    private static void valuesEqualTheOldCoreValues(GameTestHelper h) {
        var tools = EnderiteGear.TOOLS;
        check(h, tools.durability() == 2530 && tools.speed() == 10.0F && tools.attackDamageBonus() == 5.0F && tools.enchantmentValue() == 18,
                "tool material 2530 / 10 / 5 / 18");
        var armor = EnderiteGear.ARMOR;
        check(h, armor.durability() == 42 && armor.enchantmentValue() == 18 && armor.toughness() == 4.0F && armor.knockbackResistance() == 0.2F
                && armor.equipSound().equals(SoundEvents.ARMOR_EQUIP_NETHERITE), "armor material 42 / 18 / toughness 4 / knockback 0.2 / netherite sound");
        int[] defense = {4, 9, 7, 4};
        ArmorType[] types = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
        String[] parts = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < 4; i++) {
            check(h, armor.defense().get(types[i]) == defense[i], "protection " + parts[i]);
            check(h, stack("enderite_" + parts[i]).getMaxDamage() == types[i].getDurability(42), parts[i] + " durability");
        }
        check(h, armor.defense().get(ArmorType.BODY) == 22, "horse and nautilus armor protect 22 (Netherite 19)");
        check(h, EnderiteGear.ASSET.identifier().equals(SimpleOres.id("enderite")), "the equipment asset is simpleores:enderite");
        for (String path : new String[] {"sword", "spear", "pickaxe", "axe", "shovel", "hoe"}) {
            check(h, stack("enderite_" + path).getMaxDamage() == 2530, "enderite_" + path + " durability");
        }
        for (String path : GearIds.ITEMS) {
            if (path.endsWith("_pole") || path.endsWith("apple") || path.endsWith("carrot")) continue;
            check(h, stack(path).has(DataComponents.DAMAGE_RESISTANT), path + " is fire resistant");
            check(h, com.simplelib.api.SimpleRarity.of(stack(path)) == com.simplelib.api.SimpleRarity.LEGENDARY, path + " is legendary");
        }
        var pick = stack("enderite_pickaxe");
        check(h, pick.isCorrectToolForDrops(Blocks.ANCIENT_DEBRIS.defaultBlockState()) && !pick.isCorrectToolForDrops(Blocks.BEDROCK.defaultBlockState()),
                "the pickaxe mines like netherite");
        check(h, stack("enderite_horse_armor").has(DataComponents.EQUIPPABLE) && stack("enderite_nautilus_armor").has(DataComponents.EQUIPPABLE),
                "horse and nautilus armor are equippable");
        check(h, EnderiteGear.IRON_POLE == BuiltInRegistries.BLOCK.getValue(SimpleOres.id("iron_pole")), "the poles are registered");
        h.succeed();
    }

    private static void effects(GameTestHelper h, String path, int nutrition, float saturation, Object[][] want) {
        ItemStack stack = stack(path);
        var food = stack.get(DataComponents.FOOD);
        check(h, food != null && food.nutrition() == nutrition && food.saturation() == saturation && food.canAlwaysEat(), path + " food " + food);
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        check(h, consumable != null && consumable.consumeSeconds() == 1.6F, path + " eats in 1.6 s");
        int i = 0;
        for (var effect : consumable.onConsumeEffects()) {
            check(h, effect instanceof ApplyStatusEffectsConsumeEffect, path + " effect type");
            for (var instance : ((ApplyStatusEffectsConsumeEffect) effect).effects()) {
                check(h, i < want.length && instance.getEffect().equals(want[i][0]) && instance.getDuration() == (int) want[i][1]
                        && instance.getAmplifier() == (int) want[i][2], path + " effect " + i + ": " + instance);
                i++;
            }
        }
        check(h, i == want.length, path + " has " + want.length + " effects, found " + i);
    }

    /** Food numbers and effects: (effect, ticks, amplifier) in the order the core's consumables listed them. */
    private static void foodEffectsEqualTheOldCoreValues(GameTestHelper h) {
        effects(h, "netherite_carrot", 6, 1.2F, new Object[][] {{MobEffects.FIRE_RESISTANCE, 2400, 0}, {MobEffects.NIGHT_VISION, 2400, 0}});
        effects(h, "netherite_apple", 8, 1.5F, new Object[][] {{MobEffects.RESISTANCE, 1200, 0}, {MobEffects.ABSORPTION, 600, 1}, {MobEffects.REGENERATION, 200, 1}});
        effects(h, "enderite_carrot", 12, 2.4F, new Object[][] {{MobEffects.NIGHT_VISION, 6000, 0}, {MobEffects.SPEED, 1200, 1}, {MobEffects.JUMP_BOOST, 1200, 1},
                {MobEffects.SATURATION, 1, 0}});
        effects(h, "enderite_apple", 14, 3.0F, new Object[][] {{MobEffects.REGENERATION, 400, 1}, {MobEffects.RESISTANCE, 3000, 0}, {MobEffects.ABSORPTION, 2400, 3},
                {MobEffects.HEALTH_BOOST, 2400, 2}});
        effects(h, "enchanted_netherite_apple", 8, 1.5F, new Object[][] {{MobEffects.REGENERATION, 800, 1}, {MobEffects.RESISTANCE, 6000, 0},
                {MobEffects.FIRE_RESISTANCE, 6000, 0}, {MobEffects.ABSORPTION, 2400, 3}, {MobEffects.STRENGTH, 2400, 1}});
        effects(h, "enchanted_enderite_apple", 14, 3.0F, new Object[][] {{MobEffects.REGENERATION, 1200, 2}, {MobEffects.RESISTANCE, 6000, 1},
                {MobEffects.FIRE_RESISTANCE, 12000, 0}, {MobEffects.ABSORPTION, 3600, 4}, {MobEffects.HEALTH_BOOST, 3600, 4}, {MobEffects.NIGHT_VISION, 6000, 0}});
        check(h, stack("enchanted_enderite_apple").get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == Boolean.TRUE
                && stack("enchanted_netherite_apple").get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == Boolean.TRUE, "the enchanted apples glint");
        check(h, stack("netherite_apple").getRarity() == net.minecraft.world.item.Rarity.UNCOMMON && stack("enchanted_netherite_apple").getRarity() == net.minecraft.world.item.Rarity.RARE
                && stack("enderite_carrot").getRarity() == net.minecraft.world.item.Rarity.EPIC, "rarities as before");
        check(h, stack("netherite_apple").has(DataComponents.DAMAGE_RESISTANT) && stack("enderite_apple").has(DataComponents.DAMAGE_RESISTANT), "the food does not burn");
        h.succeed();
    }

    /** The poles: ranges, nearest rod wins, a strike powers the rod (moved from the core's BlockBehaviourTests). */
    private static void polesPullLightningWithinTheirRange(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.assertTrue(EnderiteGear.IRON_POLE instanceof net.minecraft.world.level.block.LightningRodBlock, "the iron rod is a lightning rod");
        h.assertValueEqual(((MetalRodBlock) EnderiteGear.IRON_POLE).range(), 32, "iron rod range");
        h.assertValueEqual(((MetalRodBlock) EnderiteGear.GOLD_POLE).range(), 64, "gold rod range");
        h.assertTrue(EnderiteGear.DIAMOND_POLE instanceof net.minecraft.world.level.block.LightningRodBlock, "the diamond pole is a lightning rod");
        h.assertValueEqual(((MetalRodBlock) EnderiteGear.DIAMOND_POLE).range(), 80, "diamond pole range");
        h.assertValueEqual(((MetalRodBlock) EnderiteGear.NETHERITE_POLE).range(), 96, "netherite rod range");
        h.assertValueEqual(((MetalRodBlock) EnderiteGear.ENDERITE_POLE).range(), 128, "enderite rod range");
        h.assertValueEqual(MetalRodBlock.MAX_RANGE, 128, "no metal rod reaches further than copper");
        BlockPos iron = onTop(level, h.absolutePos(new BlockPos(1, 1, 1)), EnderiteGear.IRON_POLE);
        java.util.Optional<BlockPos> near = MetalRodBlock.find(level, iron.offset(12, 0, 12));
        h.assertTrue(near.isPresent() && near.get().equals(iron.above()), "an iron rod 17 blocks away attracts the bolt, got " + near);
        java.util.Optional<BlockPos> far = MetalRodBlock.find(level, iron.offset(40, 0, 0));
        h.assertTrue(far.isEmpty() || !far.get().equals(iron.above()), "an iron rod 40 blocks away does not");

        BlockPos gold = onTop(level, iron.offset(2, 0, 0), EnderiteGear.GOLD_POLE);
        java.util.Optional<BlockPos> goldOnly = MetalRodBlock.find(level, iron.offset(50, 0, 0));
        h.assertTrue(goldOnly.isPresent() && goldOnly.get().equals(gold.above()),
                "a gold rod 48 blocks away attracts the bolt the iron rod no longer reaches, got " + goldOnly);
        java.util.Optional<BlockPos> nearest = MetalRodBlock.find(level, iron.offset(-10, 0, 0));
        h.assertTrue(nearest.isPresent() && nearest.get().equals(iron.above()),
                "the nearer iron rod wins over the gold rod with more range, got " + nearest);
        java.util.Optional<BlockPos> beyond = MetalRodBlock.find(level, gold.offset(70, 0, 0));
        h.assertTrue(beyond.isEmpty() || !(beyond.get().equals(gold.above()) || beyond.get().equals(iron.above())),
                "a gold rod 70 blocks away still attracts the bolt, got " + beyond);

        // Netherit rechts vom Eisenstab, Enderit links davon: jeder erreicht, was die kleineren nicht mehr erreichen.
        BlockPos netherite = onTop(level, iron.offset(4, 0, 0), EnderiteGear.NETHERITE_POLE);
        BlockPos enderite = onTop(level, iron.offset(-2, 0, 0), EnderiteGear.ENDERITE_POLE);
        java.util.Optional<BlockPos> netheriteOnly = MetalRodBlock.find(level, netherite.offset(90, 0, 0));
        h.assertTrue(netheriteOnly.isPresent() && netheriteOnly.get().equals(netherite.above()),
                "a netherite rod 90 blocks away wins over the enderite rod 96 blocks away, got " + netheriteOnly);
        java.util.Optional<BlockPos> enderiteOnly = MetalRodBlock.find(level, enderite.offset(-120, 0, 0));
        h.assertTrue(enderiteOnly.isPresent() && enderiteOnly.get().equals(enderite.above()),
                "an enderite rod 120 blocks away attracts the bolt the netherite rod (126) no longer reaches, got " + enderiteOnly);
        java.util.Optional<BlockPos> outOfReach = MetalRodBlock.find(level, enderite.offset(-130, 0, 0));
        h.assertTrue(outOfReach.isEmpty() || !outOfReach.get().equals(enderite.above()), "an enderite rod 130 blocks away still attracts the bolt");

        for (BlockPos rod : List.of(iron, gold, netherite, enderite)) {
            BlockState state = level.getBlockState(rod);
            ((net.minecraft.world.level.block.LightningRodBlock) state.getBlock()).onLightningStrike(state, level, rod);
            h.assertTrue(level.getBlockState(rod).getValue(net.minecraft.world.level.block.LightningRodBlock.POWERED), "a strike powers " + state.getBlock());
            level.setBlock(rod, Blocks.AIR.defaultBlockState(), 3);
        }
        h.succeed();
    }

    /** Stands {@code block} on top of the column at {@code column} (the test structure may have a roof). */
    private static BlockPos onTop(ServerLevel level, BlockPos column, Block block) {
        BlockPos pos = new BlockPos(column.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ()), column.getZ());
        level.setBlock(pos, block.defaultBlockState(), 3);
        return pos;
    }

    /** {@code simplebuilding:<id>} loads as {@code simpleores:<id>} (items, the poles' blocks, saved stacks): worlds made with SimpleBuilding keep their gear. */
    private static void oldSimpleBuildingIdsLoadAsSimpleOresIds(GameTestHelper h) {
        var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        for (String path : GearIds.ITEMS) {
            Identifier old = Identifier.fromNamespaceAndPath("simplebuilding", path);
            Identifier target = com.simplelib.api.IdMoves.target(Registries.ITEM, old);
            check(h, SimpleOres.id(path).equals(target), old + " moves to " + target);
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("id", old.toString());
            tag.putInt("count", 2);
            ItemStack decoded = ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
            check(h, decoded.is(OresRegistry.ITEMS.get(path)) && decoded.getCount() == 2, "an old stack of " + path + " loads as " + decoded);
        }
        for (String path : GearIds.POLES) {
            Identifier old = Identifier.fromNamespaceAndPath("simplebuilding", path);
            check(h, SimpleOres.id(path).equals(com.simplelib.api.IdMoves.target(Registries.BLOCK, old)), old + " (block) moves");
            check(h, BuiltInRegistries.BLOCK.containsKey(SimpleOres.id(path)), path + " is registered");
        }
        h.succeed();
    }

    /** Sneaking in a fall with Enderite armor dampens it (Simple Ores' own damper; SimpleBuilding's does nothing on 26.3). */
    private static void enderiteArmorDampensASneakingFall(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        check(h, EnderiteSinkDamper.pieces(player) == 0 && EnderiteSinkDamper.damping(player) == 0.0, "no armor, no damping");
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        String[] parts = {"boots", "leggings", "chestplate", "helmet"};
        for (int i = 0; i < 4; i++) {
            player.setItemSlot(slots[i], stack("enderite_" + parts[i]));
            check(h, EnderiteSinkDamper.pieces(player) == i + 1, (i + 1) + " pieces");
        }
        check(h, EnderiteGear.isEnderiteArmor(stack("enderite_helmet")) && !EnderiteGear.isEnderiteArmor(new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET)),
                "only Enderite armor counts");
        h.succeed();
    }
}
