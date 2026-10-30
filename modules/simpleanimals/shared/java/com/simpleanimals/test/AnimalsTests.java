package com.simpleanimals.test;

import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.AnimalsSpawn;
import com.simpleanimals.SimpleAnimals;
import com.simpleanimals.config.AnimalsConfig;
import com.simpleanimals.entity.Boar;
import com.simpleanimals.entity.Crab;
import com.simpleanimals.entity.Deer;
import com.simpleanimals.entity.Hedgehog;
import com.simpleanimals.entity.Zebra;
import com.simpleanimals.entity.Zorse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Module GameTests (concept WP-18, owner choice ABN29b-3). All on the 16 x 8 x 16 stone arena ({@code simpleanimals:arena}).
 * Behaviour is tested through the entities' own rules: who provokes a boar, when a hedgehog curls, what a zebra and a
 * horse produce. Natural spawning itself (biome lists, light) is covered by the rules' unit-level checks here and by the
 * client/dev-server smoke, not by waiting for the spawner.
 */
public final class AnimalsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();

    private static BlockPos at(int x, int z) {
        return new BlockPos(x, 1, z);
    }

    private static <T extends Mob> T quiet(T mob) {
        mob.setPersistenceRequired();
        return mob;
    }

    private static ServerPlayer player(GameTestHelper h, int x, int z) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(p));
        Vec3 pos = h.absoluteVec(new Vec3(x + 0.5, 1.0, z + 0.5));
        p.setPos(pos.x, pos.y, pos.z);
        return p;
    }

    static {
        ALL.put("registered", h -> {
            for (String id : new String[] {"deer", "boar", "hedgehog", "crab", "zebra", "zorse"}) {
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleAnimals.id(id)), "entity type " + id);
            }
            for (String id : new String[] {"deer_spawn_egg", "boar_spawn_egg", "hedgehog_spawn_egg", "crab_spawn_egg", "zebra_spawn_egg", "raw_crab", "cooked_crab", "guide_book"}) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleAnimals.id(id)), "item " + id);
            }
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(SimpleAnimals.id("zorse_spawn_egg")), "the zorse has no egg");
            h.assertTrue(DefaultAttributes.hasSupplier(AnimalsRegistry.DEER) && DefaultAttributes.hasSupplier(AnimalsRegistry.BOAR)
                    && DefaultAttributes.hasSupplier(AnimalsRegistry.HEDGEHOG) && DefaultAttributes.hasSupplier(AnimalsRegistry.CRAB)
                    && DefaultAttributes.hasSupplier(AnimalsRegistry.ZEBRA) && DefaultAttributes.hasSupplier(AnimalsRegistry.ZORSE), "attributes");
            h.assertTrue(AnimalsConfig.enabled && AnimalsConfig.spawnScale == 1.0 && AnimalsConfig.deer && AnimalsConfig.boar && AnimalsConfig.hedgehog
                    && AnimalsConfig.crab && AnimalsConfig.zebra && AnimalsConfig.boarAggression && AnimalsConfig.boarCloseSeconds == 6
                    && AnimalsConfig.hedgehogHuntRadius == 8 && AnimalsConfig.hedgehogHunts && AnimalsConfig.zorse, "config defaults");
            h.succeed();
        });
        ALL.put("config_caps", h -> {
            try {
                AnimalsConfig.apply(com.google.gson.JsonParser.parseString("{\"spawnScale\":9,\"boarCloseSeconds\":0,\"hedgehogHuntRadius\":999,\"deer\":\"x\"}").getAsJsonObject());
                h.assertTrue(AnimalsConfig.spawnScale == 1.0 && AnimalsConfig.boarCloseSeconds == 2 && AnimalsConfig.hedgehogHuntRadius == 16 && AnimalsConfig.deer, "clamps, bad bool = default");
                AnimalsConfig.apply(com.google.gson.JsonParser.parseString("{\"spawnScale\":-3,\"hedgehogHuntRadius\":\"a\",\"boarCloseSeconds\":null}").getAsJsonObject());
                h.assertTrue(AnimalsConfig.spawnScale == 0.0 && AnimalsConfig.hedgehogHuntRadius == 8 && AnimalsConfig.boarCloseSeconds == 6, "garbage = default");
                h.assertTrue(AnimalsConfig.KEYS.equals(AnimalsConfig.toJson().keySet().stream().toList()), "KEYS lists the file's keys in order");
            } finally {
                AnimalsConfig.reset();
            }
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleAnimals.MOD_ID);
            try {
                var random = h.getLevel().getRandom();
                h.assertTrue(AnimalsSpawn.allowed(true, random) && !AnimalsSpawn.allowed(false, random), "on: allowed per animal switch");
                AnimalsConfig.spawnScale = 0.0;
                h.assertTrue(!AnimalsSpawn.allowed(true, random), "spawnScale 0: nothing spawns");
                AnimalsConfig.reset();
                com.simplebuilding.framework.api.SubMods.set(SimpleAnimals.MOD_ID, false);
                h.assertTrue(!SimpleAnimals.on() && !AnimalsSpawn.allowed(true, random), "super-mod switch off: no natural spawn");
                Deer deer = quiet(h.spawn(AnimalsRegistry.DEER, at(2, 2)));
                h.assertTrue(!deer.checkSpawnRules(h.getLevel(), EntitySpawnReason.NATURAL) && deer.checkSpawnRules(h.getLevel(), EntitySpawnReason.SPAWN_ITEM_USE),
                        "off: natural refused, a spawn egg still works");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleAnimals.MOD_ID, before);
                AnimalsConfig.reset();
            }
            h.succeed();
        });
        ALL.put("spawn_ground", h -> {
            ServerLevel level = h.getLevel();
            Deer deer = quiet(h.spawn(AnimalsRegistry.DEER, at(2, 2)));
            Crab crab = quiet(h.spawn(AnimalsRegistry.CRAB, at(4, 2)));
            h.assertTrue(!deer.checkSpawnRules(level, EntitySpawnReason.NATURAL), "a deer does not spawn on stone");
            h.assertTrue(!crab.checkSpawnRules(level, EntitySpawnReason.NATURAL), "a crab does not spawn on stone");
            h.assertTrue(deer.checkSpawnRules(level, EntitySpawnReason.SPAWN_ITEM_USE) && crab.checkSpawnRules(level, EntitySpawnReason.BREEDING), "other reasons are not limited");
            h.assertTrue(deer.getMaxSpawnClusterSize() == 3 && crab.getMaxSpawnClusterSize() == 1, "cluster sizes");
            h.succeed();
        });

        // --- wild boar -------------------------------------------------------------------------------------------------------
        ALL.put("boar_piglet_aggro", h -> {
            Boar adult = quiet(h.spawn(AnimalsRegistry.BOAR, at(3, 3)));
            Boar piglet = quiet(h.spawn(AnimalsRegistry.BOAR, at(6, 3)));
            piglet.setBaby(true);
            ServerPlayer near = player(h, 9, 3);   // 3 blocks from the piglet, 6 from the adult
            ServerPlayer far = player(h, 14, 14);  // far from the piglet
            h.runAfterDelay(2, () -> {
                h.assertTrue(piglet.isBaby() && !adult.isBaby(), "one piglet, one adult");
                h.assertTrue(adult.pigletNear(near) && adult.provokedBy(near), "a player close to the piglet provokes the adult (like the polar bear's cub)");
                h.assertTrue(!adult.provokedBy(far), "a player far from the piglet does not");
                h.assertTrue(!piglet.provokedBy(near), "the piglet itself never attacks");
                h.succeed();
            });
        });
        ALL.put("boar_no_piglet_provocation", h -> {
            Boar alone = quiet(h.spawn(AnimalsRegistry.BOAR, at(3, 3)));
            ServerPlayer p = player(h, 5, 3);
            h.runAfterDelay(2, () -> {
                h.assertTrue(!alone.provokedBy(p), "an adult without piglets leaves a nearby player alone");
                alone.shove();
                h.assertTrue(alone.provokedBy(p), "shoved aside: it rams");
                Boar waiting = quiet(h.spawn(AnimalsRegistry.BOAR, at(10, 10)));
                waiting.addCloseTicks(AnimalsConfig.boarCloseSeconds * 20 - 1);
                h.assertTrue(!waiting.provokedBy(p), "just under the time: still calm");
                waiting.addCloseTicks(1);
                h.assertTrue(waiting.provokedBy(p), "too long too near: it rams");
                h.succeed();
            });
        });
        ALL.put("boar_close_time_decays", h -> {
            Boar boar = quiet(h.spawn(AnimalsRegistry.BOAR, at(3, 3)));
            boar.addCloseTicks(100);
            h.runAfterDelay(30, () -> {
                h.assertTrue(boar.closeTicks() < 60, "with nobody near, the counter runs down by 2 per tick: " + boar.closeTicks());
                h.succeed();
            });
        });
        ALL.put("boar_ram_damage", h -> {
            Boar boar = quiet(h.spawn(AnimalsRegistry.BOAR, at(3, 3)));
            Mob target = quiet(h.spawn(EntityTypes.COW, at(5, 3)));
            boar.setTarget(target);
            var difficulty = h.getLevel().getDifficulty();
            double expected = difficulty == net.minecraft.world.Difficulty.HARD ? 8.0 : difficulty == net.minecraft.world.Difficulty.EASY ? 4.0 : 6.0;
            h.assertTrue(boar.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == expected, "ram damage 4 / 6 / 8 by difficulty: " + expected);
            h.succeed();
        });

        // --- hedgehog --------------------------------------------------------------------------------------------------------
        ALL.put("hedgehog_curls_faster_than_armadillo", h -> {
            h.assertTrue(Hedgehog.CURL_TICKS < Armadillo.ArmadilloState.ROLLING.animationDuration(), "the roll is quicker than the armadillo's");
            Hedgehog hedgehog = quiet(h.spawn(AnimalsRegistry.HEDGEHOG, at(3, 3)));
            Mob threat = quiet(h.spawn(EntityTypes.CREEPER, at(5, 3)));
            threat.setNoAi(true);
            h.runAfterDelay(12, () -> {
                h.assertTrue(hedgehog.isCurled(), "a monster within 4 blocks: curled");
                threat.discard();
                h.runAfterDelay(Hedgehog.UNROLL_DELAY + 20, () -> {
                    h.assertTrue(!hedgehog.isCurled(), "no threat for 2 seconds: unrolled");
                    h.succeed();
                });
            });
        });
        MAX_TICKS.put("hedgehog_curls_faster_than_armadillo", 120);
        ALL.put("hedgehog_curled_takes_less_and_pricks", h -> {
            ServerLevel level = h.getLevel();
            Hedgehog hedgehog = quiet(h.spawn(AnimalsRegistry.HEDGEHOG, at(3, 3)));
            Mob attacker = quiet(h.spawn(EntityTypes.ZOMBIE, at(5, 3)));
            attacker.setNoAi(true);
            hedgehog.setCurled(true);
            float before = hedgehog.getHealth(), attackerBefore = attacker.getHealth();
            hedgehog.hurtServer(level, level.damageSources().mobAttack(attacker), 4.0F);
            h.assertTrue(Math.abs((before - hedgehog.getHealth()) - 1.5F) < 0.01F, "curled: (4 - 1) / 2 = 1.5 damage, got " + (before - hedgehog.getHealth()));
            h.assertTrue(attacker.getHealth() < attackerBefore, "the melee attacker is pricked");
            Hedgehog open = quiet(h.spawn(AnimalsRegistry.HEDGEHOG, at(8, 3)));
            open.setCurled(false);
            float openBefore = open.getHealth();
            open.hurtServer(level, level.damageSources().generic(), 3.0F);
            h.assertTrue(Math.abs((openBefore - open.getHealth()) - 3.0F) < 0.01F, "open: full damage");
            h.succeed();
        });
        ALL.put("hedgehog_hunts_silverfish", h -> {
            Hedgehog hedgehog = quiet(h.spawn(AnimalsRegistry.HEDGEHOG, at(3, 3)));
            Silverfish near = quiet(h.spawn(EntityTypes.SILVERFISH, at(9, 3)));   // 6 blocks
            near.setNoAi(true);
            Silverfish far = quiet(h.spawn(EntityTypes.SILVERFISH, at(3, 14)));  // 11 blocks, outside the 8-block radius
            far.setNoAi(true);
            h.runAfterDelay(120, () -> {
                h.assertTrue(!near.isAlive(), "the silverfish within 8 blocks was hunted down");
                h.assertTrue(far.isAlive(), "the one beyond 8 blocks was left alone");
                h.assertTrue(hedgehog.isAlive(), "unhurt");
                h.succeed();
            });
        });
        MAX_TICKS.put("hedgehog_hunts_silverfish", 160);

        // --- zebra and zorse ---------------------------------------------------------------------------------------------------
        ALL.put("zebra_crosses_with_horse_to_zorse", h -> {
            ServerLevel level = h.getLevel();
            Zebra zebra = quiet(h.spawn(AnimalsRegistry.ZEBRA, at(3, 3)));
            Zebra other = quiet(h.spawn(AnimalsRegistry.ZEBRA, at(5, 3)));
            AbstractHorse horse = quiet(h.spawn(EntityTypes.HORSE, at(8, 3)));
            for (AbstractHorse a : new AbstractHorse[] {zebra, other, horse}) {
                a.setTamed(true);
                a.setHealth(a.getMaxHealth());
                a.setInLove(null);
            }
            h.assertTrue(zebra.canMate(other), "zebra x zebra");
            h.assertTrue(zebra.canMate(horse), "zebra x horse (the zebra is the parent that looks)");
            h.assertTrue(zebra.getBreedOffspring(level, other) instanceof Zebra, "zebra x zebra gives a zebra");
            var cross = zebra.getBreedOffspring(level, horse);
            h.assertTrue(cross instanceof Zorse, "zebra x horse gives a zorse");
            AnimalsConfig.zorse = false;
            try {
                h.assertTrue(!zebra.canMate(horse) && zebra.canMate(other), "zorse switch off: the cross is off, zebras still breed");
            } finally {
                AnimalsConfig.reset();
            }
            h.assertTrue(!((net.minecraft.world.entity.animal.Animal) horse).canMate(zebra), "the Vanilla horse is unchanged: it does not look for zebras");
            h.succeed();
        });
        ALL.put("zebra_untamed_does_not_breed", h -> {
            Zebra a = quiet(h.spawn(AnimalsRegistry.ZEBRA, at(3, 3)));
            Zebra b = quiet(h.spawn(AnimalsRegistry.ZEBRA, at(5, 3)));
            a.setInLove(null);
            b.setInLove(null);
            h.assertTrue(!a.canMate(b), "wild zebras do not breed (like Vanilla horses)");
            h.succeed();
        });
        ALL.put("zorse_is_infertile", h -> {
            ServerLevel level = h.getLevel();
            Zorse zorse = quiet(h.spawn(AnimalsRegistry.ZORSE, at(3, 3)));
            Zorse second = quiet(h.spawn(AnimalsRegistry.ZORSE, at(5, 3)));
            AbstractHorse horse = quiet(h.spawn(EntityTypes.HORSE, at(8, 3)));
            for (AbstractHorse a : new AbstractHorse[] {zorse, second, horse}) {
                a.setTamed(true);
                a.setHealth(a.getMaxHealth());
                a.setInLove(null);
            }
            h.assertTrue(!zorse.canMate(second) && !zorse.canMate(horse), "a zorse mates with nobody");
            h.assertTrue(zorse.getBreedOffspring(level, second) == null, "and has no offspring");
            h.assertTrue(!(horse.getType() == AnimalsRegistry.ZORSE), "the horse is a Vanilla horse");
            h.succeed();
        });
        ALL.put("zebra_is_horse", h -> {
            Zebra zebra = quiet(h.spawn(AnimalsRegistry.ZEBRA, at(3, 3)));
            h.assertTrue(zebra instanceof AbstractHorse && zebra.getBbWidth() == EntityTypes.HORSE.getWidth() && zebra.getBbHeight() == EntityTypes.HORSE.getHeight(), "horse size");
            h.assertTrue(zebra.isFood(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_CARROT)), "horse food");
            h.assertTrue(zebra.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) >= 15.0, "horse health range");
            h.succeed();
        });

        // --- deer, crab, food ---------------------------------------------------------------------------------------------------
        ALL.put("deer_food_and_offspring", h -> {
            ServerLevel level = h.getLevel();
            Deer deer = quiet(h.spawn(AnimalsRegistry.DEER, at(3, 3)));
            h.assertTrue(deer.isFood(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT))
                    && deer.isFood(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CARROT))
                    && !deer.isFood(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF)), "wheat and carrots breed it");
            h.assertTrue(deer.getBreedOffspring(level, deer) instanceof Deer, "a fawn");
            h.succeed();
        });
        ALL.put("crab_food_cooks_like_cod", h -> {
            var raw = new net.minecraft.world.item.ItemStack(AnimalsRegistry.RAW_CRAB);
            var cooked = new net.minecraft.world.item.ItemStack(AnimalsRegistry.COOKED_CRAB);
            var cod = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COD);
            var cookedCod = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_COD);
            h.assertTrue(raw.get(net.minecraft.core.component.DataComponents.FOOD).nutrition() == cod.get(net.minecraft.core.component.DataComponents.FOOD).nutrition()
                    && cooked.get(net.minecraft.core.component.DataComponents.FOOD).nutrition() == cookedCod.get(net.minecraft.core.component.DataComponents.FOOD).nutrition(),
                    "crab meat has the food value of cod (A12: no new food level)");
            h.succeed();
        });
        ALL.put("natural_spawn_never_touches_players_or_blocks", h -> {
            // The module changes no blocks: a deer next to a flower does not eat it, a boar next to crops does not trample on its own.
            BlockPos flower = at(5, 5);
            h.setBlock(flower, Blocks.POPPY);
            quiet(h.spawn(AnimalsRegistry.DEER, at(5, 6)));
            quiet(h.spawn(AnimalsRegistry.BOAR, at(6, 5)));
            h.runAfterDelay(60, () -> {
                h.assertTrue(h.getBlockState(flower).is(Blocks.POPPY), "the flower is still there");
                h.succeed();
            });
        });
        MAX_TICKS.put("natural_spawn_never_touches_players_or_blocks", 100);
        ALL.put("guide_book", com.simpleanimals.guide.AnimalsGuide::gameTest);
    }

    private AnimalsTests() {}
}
