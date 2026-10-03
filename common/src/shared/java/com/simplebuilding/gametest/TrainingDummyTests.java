package com.simplebuilding.gametest;

import com.simplebuilding.dev.testcentre.FeatureStations;
import com.simplebuilding.dev.testcentre.TcOp;
import com.simplebuilding.dev.testcentre.TestCentreLayout;
import com.simplebuilding.dummy.DummyTargets;
import com.simplebuilding.dummy.TrainingDummy;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.fletching.ArrowParts;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Stroh-Ruestungsstaender und Trainingspuppe (docs/ai/PLAN-TRAINING-DUMMY-2026-10-02.md). Loader-neutral; auf Linien
 * ohne {@link McVersion#TRAINING_DUMMY} gelingen die Tests sofort.
 */
public final class TrainingDummyTests {
    private TrainingDummyTests() {
    }

    private static final float EPS = 1.0E-3F;

    private static TrainingDummy spawn(GameTestHelper helper, EntityType<TrainingDummy> type, int x, int z, ItemStack head) {
        TrainingDummy dummy = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 1.0, z + 0.5));
        dummy.setPos(at.x, at.y, at.z);
        dummy.setItemSlot(EquipmentSlot.HEAD, head.copy());
        helper.getLevel().addFreshEntity(dummy);
        return dummy;
    }

    private static TrainingDummy dummy(GameTestHelper helper, int x, int z, ItemStack head) {
        return spawn(helper, ModEntities.TRAINING_DUMMY, x, z, head);
    }

    private static void near(GameTestHelper helper, float actual, float expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < EPS, what + ": expected " + expected + ", got " + actual);
    }

    private static ItemStack enchanted(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), level);
        return stack;
    }

    /**
     * Kuerbis auf den Stroh-Ruestungsstaender: der Kuerbis wird verbraucht (sitzt nicht auf dem Kopf), es entsteht die
     * Puppe mit der Ausruestung; die Schere macht es rueckgaengig (Kuerbis faellt, Ausruestung bleibt, Schere -1).
     */
    public static void pumpkinTurnsTheStrawStandIntoTrainingDummy(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        TrainingDummy stand = spawn(helper, ModEntities.STRAW_ARMOR_STAND, 1, 1, ItemStack.EMPTY);
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        helper.assertFalse(stand.isTrainingDummy(), "a fresh straw stand is already a dummy");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARVED_PUMPKIN));
        stand.interact(player, InteractionHand.MAIN_HAND, new Vec3(0.0, 1.8, 0.0));
        helper.assertTrue(stand.isRemoved(), "the straw stand stays after getting a pumpkin");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the pumpkin was not used up");
        List<TrainingDummy> dummies = helper.getLevel().getEntities(ModEntities.TRAINING_DUMMY, stand.getBoundingBox().inflate(1.0), d -> true);
        helper.assertValueEqual(dummies.size(), 1, "training dummies after the pumpkin");
        TrainingDummy dummy = dummies.getFirst();
        helper.assertTrue(dummy.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the pumpkin sits on the dummy's head");
        helper.assertTrue(dummy.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "the dummy lost the chestplate");
        helper.assertTrue(dummy.getPickResult().is(ModItems.TRAINING_DUMMY), "picking the dummy gives no Training Dummy item");

        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        dummy.interact(player, InteractionHand.MAIN_HAND, new Vec3(0.0, 1.0, 0.0));
        helper.assertTrue(dummy.isRemoved(), "shears do not turn the dummy back");
        List<TrainingDummy> stands = helper.getLevel().getEntities(ModEntities.STRAW_ARMOR_STAND, dummy.getBoundingBox().inflate(1.0), d -> true);
        helper.assertValueEqual(stands.size(), 1, "straw stands after the shears");
        helper.assertTrue(stands.getFirst().getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "the shears took the chestplate");
        helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "shears damage");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, dummy.getBoundingBox().inflate(2.0))
                .stream().anyMatch(e -> e.getItem().is(Items.CARVED_PUMPKIN)), "the pumpkin did not drop");

        java.util.Set<String> recipes = new java.util.HashSet<>();
        helper.getLevel().recipeAccess().getRecipes().forEach(r -> recipes.add(r.id().identifier().getPath()));
        helper.assertTrue(recipes.contains("straw_armor_stand") && recipes.contains("training_dummy"),
                "the straw armor stand or training dummy recipe is missing");
        helper.succeed();
    }

    /**
     * Jeder Treffer zaehlt (Fehler bis 2026-10-03: die Trefferpause begann nie, jeder Treffer nach dem ersten musste
     * staerker sein als der staerkste bisher). Treffer nach der Pause zaehlen voll, Treffer in der Pause zeigen 0.
     */
    public static void everyHitShowsItsNumberAfterTheCooldown(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        DamageSource source = level.damageSources().generic();
        TrainingDummy dummy = dummy(helper, 1, 1, new ItemStack(Items.CARVED_PUMPKIN));
        helper.assertTrue(dummy.hurtServer(level, source, 5.0F), "the first hit was refused");
        helper.runAfterDelay(TrainingDummy.COOLDOWN_TICKS + 2, () -> {
            helper.assertTrue(dummy.hurtServer(level, source, 3.0F), "a weaker hit after the cooldown was refused");
            near(helper, dummy.lastShown(), 3.0F, "a weaker hit after the cooldown");
            helper.assertTrue(dummy.hurtServer(level, source, 2.0F) == false, "a weaker hit inside the new cooldown counted");
            near(helper, dummy.lastShown(), 0.0F, "a refused hit shows 0");
            helper.assertValueEqual(dummy.visibleNumbers(), 3, "numbers after three hits");
            helper.runAfterDelay(TrainingDummy.COOLDOWN_TICKS + 2, () -> {
                helper.assertTrue(dummy.hurtServer(level, source, 1.0F), "a third hit after the cooldown was refused");
                near(helper, dummy.lastShown(), 1.0F, "a small hit after the cooldown");
                near(helper, dummy.sessionTotal(), 9.0F, "total of the counted hits");
                helper.assertValueEqual(dummy.sessionHits(), 3, "counted hits");
                helper.succeed();
            });
        });
    }

    /** Vogelscheuche: im Umkreis eines Stroh-Ruestungsstaenders zertrampelt ein Mob kein Ackerland, ohne schon. */
    public static void scarecrowKeepsMobsFromTramplingFarmland(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos farm = new BlockPos(2, 1, 2);
        helper.setBlock(farm, net.minecraft.world.level.block.Blocks.FARMLAND);
        BlockPos absolute = helper.absolutePos(farm);
        net.minecraft.world.entity.Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, farm.above());
        net.minecraft.world.level.block.Blocks.FARMLAND.fallOn(level, level.getBlockState(absolute), absolute, zombie, 10.0);
        helper.assertTrue(level.getBlockState(absolute).is(net.minecraft.world.level.block.Blocks.DIRT), "the zombie did not trample farmland without a scarecrow");
        helper.setBlock(farm, net.minecraft.world.level.block.Blocks.FARMLAND);
        spawn(helper, ModEntities.STRAW_ARMOR_STAND, 4, 2, ItemStack.EMPTY);
        net.minecraft.world.level.block.Blocks.FARMLAND.fallOn(level, level.getBlockState(absolute), absolute, zombie, 10.0);
        helper.assertTrue(level.getBlockState(absolute).is(net.minecraft.world.level.block.Blocks.FARMLAND), "the zombie trampled farmland next to a scarecrow");
        helper.succeed();
    }

    /** Unzerstoerbar: Explosion und Schlag lassen sie stehen, nur der Schleich-Schlag baut sie ab (mit Drops). */
    public static void onlySneakingHitsPickTheDummyUp(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        TrainingDummy dummy = dummy(helper, 1, 1, new ItemStack(Items.ZOMBIE_HEAD));
        float health = dummy.getHealth();
        dummy.hurtServer(level, level.damageSources().explosion(null, null), 50.0F);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        DamageSource hit = level.damageSources().playerAttack(player);
        dummy.hurtServer(level, hit, 30.0F);
        helper.assertFalse(dummy.isRemoved(), "the dummy broke from an explosion or a plain hit");
        near(helper, dummy.getHealth(), health, "dummy health after the hits");
        player.setShiftKeyDown(true);
        helper.assertTrue(dummy.hurtServer(level, hit, 1.0F), "the sneaking hit was refused");
        helper.assertTrue(dummy.isRemoved(), "a sneaking hit does not pick the dummy up");
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, dummy.getBoundingBox().inflate(2.0));
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.TRAINING_DUMMY)), "no Training Dummy item dropped");
        helper.assertFalse(drops.stream().anyMatch(e -> e.getItem().is(ModItems.STRAW_ARMOR_STAND)), "a straw armor stand dropped instead");
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.ZOMBIE_HEAD)), "the head did not drop");
        helper.succeed();
    }

    /** Kopf -> Mob-Art (Vanilla, SimpleBuilding, Drache) und Verzauberungen sehen den Stellvertreter. */
    public static void headsMakeEnchantmentsSeeTheirMob(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        helper.assertValueEqual(DummyTargets.typeForHead(new ItemStack(Items.ZOMBIE_HEAD)), EntityTypes.ZOMBIE, "zombie head");
        helper.assertValueEqual(DummyTargets.typeForHead(new ItemStack(Items.WITHER_SKELETON_SKULL)), EntityTypes.WITHER_SKELETON, "wither skull");
        helper.assertValueEqual(DummyTargets.typeForHead(new ItemStack(Items.DRAGON_HEAD)), EntityTypes.ENDER_DRAGON, "dragon head");
        helper.assertValueEqual(DummyTargets.typeForHead(new ItemStack(TweaksItems.SPIDER_HEAD)), EntityTypes.SPIDER, "spider head");
        helper.assertValueEqual(DummyTargets.typeForHead(new ItemStack(TweaksItems.CAVE_SPIDER_HEAD)), EntityTypes.CAVE_SPIDER, "cave spider head");
        helper.assertTrue(DummyTargets.typeForHead(new ItemStack(Items.PLAYER_HEAD)) == null, "a player head stands for a mob");
        helper.assertTrue(DummyTargets.typeForHead(new ItemStack(Items.CARVED_PUMPKIN)) == null, "the pumpkin stands for a mob");

        ServerLevel level = helper.getLevel();
        TrainingDummy zombie = dummy(helper, 1, 1, new ItemStack(Items.ZOMBIE_HEAD));
        TrainingDummy spider = dummy(helper, 3, 1, new ItemStack(TweaksItems.SPIDER_HEAD));
        TrainingDummy plain = dummy(helper, 1, 3, new ItemStack(Items.CARVED_PUMPKIN));
        DamageSource source = level.damageSources().generic();
        ItemStack smite = enchanted(helper, new ItemStack(Items.IRON_SWORD), Enchantments.SMITE, 5);
        ItemStack bane = enchanted(helper, new ItemStack(Items.IRON_SWORD), Enchantments.BANE_OF_ARTHROPODS, 5);
        ItemStack sharp = enchanted(helper, new ItemStack(Items.IRON_SWORD), Enchantments.SHARPNESS, 5);
        near(helper, EnchantmentHelper.modifyDamage(level, smite, zombie, source, 5.0F), 17.5F, "smite V against the zombie head");
        near(helper, EnchantmentHelper.modifyDamage(level, smite, plain, source, 5.0F), 5.0F, "smite V against the pumpkin");
        near(helper, EnchantmentHelper.modifyDamage(level, smite, spider, source, 5.0F), 5.0F, "smite V against the spider head");
        near(helper, EnchantmentHelper.modifyDamage(level, bane, spider, source, 5.0F), 17.5F, "bane V against the spider head");
        near(helper, EnchantmentHelper.modifyDamage(level, sharp, plain, source, 5.0F), 8.0F, "sharpness V against the pumpkin");
        helper.assertValueEqual(ArrowParts.Tip.IRON.bonusAgainst(zombie), 2.5, "iron tip against the zombie head");
        helper.assertValueEqual(ArrowParts.Tip.IRON.bonusAgainst(plain), 0.5, "iron tip against the pumpkin");
        helper.assertValueEqual(ArrowParts.Tip.GOLD.bonusAgainst(zombie), 2.0, "gold tip against the zombie head");
        helper.succeed();
    }

    /** Ruestung der Puppe und natuerliche Ruestung des Mobs senken die Zahl; Feuer-/Geschossimmunitaet zeigt 0. */
    public static void armourAndImmunitiesShapeTheNumber(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        // Ein Schlag (generic umgeht Ruestung); der Spieler schleicht nicht, baut also nichts ab.
        DamageSource source = level.damageSources().playerAttack(helper.makeMockServerPlayerInLevel());
        TrainingDummy plain = dummy(helper, 1, 1, new ItemStack(Items.CARVED_PUMPKIN));
        plain.hurtServer(level, source, 10.0F);
        near(helper, plain.lastShown(), 10.0F, "10 against the bare pumpkin dummy");
        TrainingDummy zombie = dummy(helper, 3, 1, new ItemStack(Items.ZOMBIE_HEAD));
        zombie.hurtServer(level, source, 10.0F);
        near(helper, zombie.lastShown(), 9.84F, "10 against the zombie head (natural armor 2)");
        TrainingDummy iron = dummy(helper, 1, 3, new ItemStack(Items.CARVED_PUMPKIN));
        iron.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        iron.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        iron.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        iron.tick();
        helper.assertValueEqual(iron.getArmorValue(), 13, "armor of the dressed dummy");
        iron.hurtServer(level, source, 10.0F);
        near(helper, iron.lastShown(), 10.0F * (1.0F - Math.max(13 - 5.0F, 13 * 0.2F) / 25.0F), "10 against iron armor");
        helper.assertTrue(iron.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 0, "the dummy's armor wears out");

        TrainingDummy blaze = dummy(helper, 3, 3, new ItemStack(TweaksItems.BLAZE_HEAD));
        helper.assertFalse(blaze.hurtServer(level, level.damageSources().inFire(), 1.0F), "fire hurts the blaze head dummy");
        near(helper, blaze.lastShown(), 0.0F, "fire against the blaze head");
        TrainingDummy enderman = dummy(helper, 5, 1, new ItemStack(TweaksItems.ENDERMAN_HEAD));
        net.minecraft.world.entity.projectile.arrow.Arrow arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(level, 0, 0, 0,
                new ItemStack(Items.ARROW), null);
        helper.assertFalse(enderman.hurtServer(level, level.damageSources().arrow(arrow, null), 6.0F), "an arrow hurts the enderman head");
        near(helper, enderman.lastShown(), 0.0F, "arrow against the enderman head");
        helper.succeed();
    }

    /** Trefferpause wie bei Mobs, Krit-Erkennung, Zahl als Anzeige-Entity, Summe nach der Pause. */
    public static void numbersCooldownCritsAndTheSummary(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        DamageSource source = level.damageSources().generic();
        TrainingDummy dummy = dummy(helper, 1, 1, new ItemStack(Items.CARVED_PUMPKIN));
        helper.assertTrue(dummy.hurtServer(level, source, 4.0F), "the first hit was refused");
        helper.assertFalse(dummy.hurtServer(level, source, 3.0F), "a weaker hit inside the cooldown counted");
        helper.assertTrue(dummy.hurtServer(level, source, 6.0F), "a stronger hit inside the cooldown was refused");
        near(helper, dummy.lastShown(), 2.0F, "a stronger hit inside the cooldown shows the difference");
        near(helper, dummy.sessionTotal(), 6.0F, "running total");
        helper.assertValueEqual(dummy.visibleNumbers(), 3, "floating numbers (the refused hit shows 0)");
        List<Display.TextDisplay> numbers = level.getEntitiesOfClass(Display.TextDisplay.class, dummy.getBoundingBox().inflate(2.0, 3.0, 2.0),
                d -> d.entityTags().contains(TrainingDummy.NUMBER_TAG));
        helper.assertValueEqual(numbers.size(), 3, "number entities in the world");

        TrainingDummy critTarget = dummy(helper, 3, 1, new ItemStack(Items.CARVED_PUMPKIN));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.fallDistance = 1.0;
        player.setOnGround(false);
        critTarget.noteAttack(player, 1.0F);
        critTarget.hurtServer(level, level.damageSources().playerAttack(player), 6.0F);
        helper.assertTrue(critTarget.lastCrit(), "a falling full-strength hit is no crit");
        TrainingDummy weak = dummy(helper, 5, 1, new ItemStack(Items.CARVED_PUMPKIN));
        weak.noteAttack(player, 0.3F);
        weak.hurtServer(level, level.damageSources().playerAttack(player), 2.0F);
        helper.assertFalse(weak.lastCrit(), "a weak hit counts as a crit");

        helper.runAfterDelay(TrainingDummy.IDLE_TICKS + 5, () -> {
            helper.assertValueEqual(dummy.visibleNumbers(), 0, "numbers after their second");
            helper.assertTrue(numbers.stream().allMatch(Display.TextDisplay::isRemoved), "a number entity outlived its second");
            helper.assertTrue(dummy.summaryDisplay() != null && !dummy.summaryDisplay().isRemoved(), "no summary after the pause");
            helper.assertValueEqual(dummy.sessionHits(), 0, "hits after the summary");
            dummy.discard();
            helper.assertTrue(dummy.summaryDisplay().isRemoved(), "the summary outlived its dummy");
            helper.succeed();
        });
    }

    /** Pfeil-Station: Puppen als Ziele, alle Befiederungs- und Trank-Pfeile in den Truhen. */
    public static void theArcheryStationHasEveryArrowAndItsDummies(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel().registryAccess(), new BlockPos(0, 64, 0));
        TestCentreLayout.Section archery = plan.section("archery");
        long dummies = archery.ops().stream().filter(op -> op instanceof TcOp.Stand stand && stand.dummy()).count();
        helper.assertValueEqual((int) dummies, 8, "training dummies in the archery station");
        List<ItemStack> stocked = new ArrayList<>();
        archery.ops().forEach(op -> {
            if (op instanceof TcOp.Fill fill) {
                stocked.addAll(fill.contents());
            }
        });
        if (McVersion.FLETCHING) {
            for (ArrowParts.Parts parts : ArrowParts.allCombinations()) {
                helper.assertTrue(stocked.stream().anyMatch(s -> s.is(ModItems.CRAFTED_ARROW) && ArrowParts.of(s).equals(parts)),
                        "the archery station lacks the arrow " + parts);
            }
        }
        helper.assertValueEqual(stocked.stream().filter(s -> s.is(Items.TIPPED_ARROW)).count(), (long) FeatureStations.tippedArrowCount(),
                "tipped arrows in the archery station");
        helper.assertTrue(stocked.stream().anyMatch(s -> s.is(Items.BOW)) && stocked.stream().anyMatch(s -> s.is(Items.CROSSBOW))
                && stocked.stream().anyMatch(s -> s.is(Items.SPECTRAL_ARROW)), "the archery station lacks bows or spectral arrows");
        helper.assertTrue(plan.coveredItems().contains(ModItems.STRAW_ARMOR_STAND), "the straw armor stand has no place in the test centre");
        helper.succeed();
    }
}
