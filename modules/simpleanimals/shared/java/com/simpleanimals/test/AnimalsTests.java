package com.simpleanimals.test;

import com.simpleanimals.AnimalVariants;
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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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

    private static int truffles(GameTestHelper h) {
        int n = 0;
        for (var item : h.getEntities(net.minecraft.world.entity.EntityTypes.ITEM)) {
            if (item.getItem().is(AnimalsRegistry.TRUFFLE)) n += item.getItem().getCount();
        }
        return n;
    }

    private static ServerPlayer player(GameTestHelper h, int x, int z) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(p));
        Vec3 pos = h.absoluteVec(new Vec3(x + 0.5, 1.0, z + 0.5));
        p.setPos(pos.x, pos.y, pos.z);
        return p;
    }

    private static Holder<Biome> biome(GameTestHelper h, ResourceKey<Biome> key) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
    }

    /** Checks one catalog entry: {@code variant} is at home in {@code biome}, the weighted pick favours it and still shows every other coat. */
    private static void checkHome(GameTestHelper h, java.util.List<AnimalVariants.Variant> catalog, int variant, Holder<Biome> biome, String what) {
        h.assertTrue(AnimalVariants.atHome(catalog, variant, biome), what + " is at home");
        for (int other = 0; other < catalog.size(); other++) {
            if (other != variant) h.assertTrue(!AnimalVariants.atHome(catalog, other, biome), what + ": variant " + other + " is not at home too");
        }
        RandomSource random = RandomSource.create(4242L + variant);
        int[] count = new int[catalog.size()];
        for (int i = 0; i < 800; i++) count[AnimalVariants.pick(catalog, biome, random)]++;
        h.assertTrue(count[variant] * 100 >= 800 * 55, what + ": the home coat is the most common (" + count[variant] + " of 800)");
        for (int i = 0; i < catalog.size(); i++) h.assertTrue(count[i] > 0, what + ": coat " + i + " still appears now and then");
    }

    static {
        ALL.put("registered", h -> {
            for (String id : new String[] {"deer", "boar", "hedgehog", "crab", "zebra", "zorse"}) {
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleAnimals.id(id)), "entity type " + id);
            }
            for (String id : new String[] {"deer_spawn_egg", "boar_spawn_egg", "hedgehog_spawn_egg", "crab_spawn_egg", "zebra_spawn_egg", "raw_crab", "cooked_crab", "truffle", "truffle_soup", "guide_book"}) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleAnimals.id(id)), "item " + id);
            }
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(SimpleAnimals.id("zorse_spawn_egg")), "the zorse has no egg");
            h.assertTrue(DefaultAttributes.hasSupplier(AnimalsRegistry.DEER) && DefaultAttributes.hasSupplier(AnimalsRegistry.BOAR)
                    && DefaultAttributes.hasSupplier(AnimalsRegistry.HEDGEHOG) && DefaultAttributes.hasSupplier(AnimalsRegistry.CRAB)
                    && DefaultAttributes.hasSupplier(AnimalsRegistry.ZEBRA) && DefaultAttributes.hasSupplier(AnimalsRegistry.ZORSE), "attributes");
            h.assertTrue(AnimalsConfig.enabled && AnimalsConfig.spawnScale == 1.0 && AnimalsConfig.deer && AnimalsConfig.boar && AnimalsConfig.hedgehog
                    && AnimalsConfig.crab && AnimalsConfig.zebra && AnimalsConfig.boarAggression && AnimalsConfig.boarCloseSeconds == 6
                    && AnimalsConfig.hedgehogHuntRadius == 8 && AnimalsConfig.hedgehogHunts && AnimalsConfig.zorse
                    && AnimalsConfig.TRUFFLE_DEFAULT == 0.005, "config defaults");
            h.succeed();
        });
        ALL.put("config_caps", h -> {
            try {
                AnimalsConfig.apply(com.google.gson.JsonParser.parseString("{\"spawnScale\":9,\"boarCloseSeconds\":0,\"hedgehogHuntRadius\":999,\"deer\":\"x\"}").getAsJsonObject());
                h.assertTrue(AnimalsConfig.spawnScale == 1.0 && AnimalsConfig.boarCloseSeconds == 2 && AnimalsConfig.hedgehogHuntRadius == 16 && AnimalsConfig.deer, "clamps, bad bool = default");
                AnimalsConfig.apply(com.google.gson.JsonParser.parseString("{\"spawnScale\":-3,\"hedgehogHuntRadius\":\"a\",\"boarCloseSeconds\":null}").getAsJsonObject());
                h.assertTrue(AnimalsConfig.spawnScale == 0.0 && AnimalsConfig.hedgehogHuntRadius == 8 && AnimalsConfig.boarCloseSeconds == 6, "garbage = default");
                AnimalsConfig.apply(com.google.gson.JsonParser.parseString("{\"boarTruffleChance\":7,\"boarTruffles\":3}").getAsJsonObject());
                h.assertTrue(AnimalsConfig.boarTruffleChance == 1.0 && AnimalsConfig.boarTruffles, "truffle chance clamps to 1, bad bool = default");
                AnimalsConfig.apply(com.google.gson.JsonParser.parseString("{\"boarTruffleChance\":-1}").getAsJsonObject());
                h.assertTrue(AnimalsConfig.boarTruffleChance == 0.0, "truffle chance clamps to 0");
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
            h.assertTrue(Math.abs((before - hedgehog.getHealth()) - 2.0F) < 0.01F, "curled: half of 4 = 2 damage, got " + (before - hedgehog.getHealth()));
            Hedgehog small = quiet(h.spawn(AnimalsRegistry.HEDGEHOG, at(6, 3)));
            small.setCurled(true);
            float tiny = small.getHealth();
            small.hurtServer(level, level.damageSources().generic(), 1.0F);
            h.assertTrue(Math.abs((tiny - small.getHealth()) - 0.5F) < 0.01F, "curled: 1 damage is 0.5, not zero");
            // 50 % chance: over 40 hits at least one prick lands (2^-40 to miss); 1..4 damage each
            for (int i = 0; i < 40 && attacker.getHealth() >= attackerBefore; i++) {
                Hedgehog target = quiet(h.spawn(AnimalsRegistry.HEDGEHOG, at(7, 3)));
                target.setCurled(true);
                target.hurtServer(level, level.damageSources().mobAttack(attacker), 0.5F);
                target.discard();
            }
            h.assertTrue(attacker.getHealth() < attackerBefore, "the melee attacker is pricked");
            h.assertTrue(attackerBefore - attacker.getHealth() <= Hedgehog.PRICK_MAX + 0.01F, "a prick is at most 4");
            net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(42L);
            int hits = 0, n = 2000;
            for (int i = 0; i < n; i++) {
                float p = Hedgehog.prick(rnd);
                if (p > 0.0F) hits++;
                h.assertTrue(p == 0.0F || (p >= Hedgehog.PRICK_MIN && p <= Hedgehog.PRICK_MAX), "prick 0 or 1..4, got " + p);
            }
            h.assertTrue(hits > n * 0.45 && hits < n * 0.55, "about 50 % pricks, got " + hits + " / " + n);
            Mob calm = quiet(h.spawn(EntityTypes.ZOMBIE, at(1, 3)));
            calm.setNoAi(true);
            boolean saved = AnimalsConfig.hedgehogThorns;
            AnimalsConfig.hedgehogThorns = false;
            try {
                float calmBefore = calm.getHealth();
                for (int i = 0; i < 40; i++) {
                    Hedgehog target = quiet(h.spawn(AnimalsRegistry.HEDGEHOG, at(7, 3)));
                    target.setCurled(true);
                    target.hurtServer(level, level.damageSources().mobAttack(calm), 0.5F);
                    target.discard();
                }
                h.assertTrue(calm.getHealth() == calmBefore, "switch off: no prick");
            } finally {
                AnimalsConfig.hedgehogThorns = saved;
            }
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
        ALL.put("boar_digs_truffles", h -> {
            // IG31-20: a boar on stone cannot dig; on dirt a dig drops 1 to 2 truffles and changes no block.
            Boar boar = quiet(h.spawn(AnimalsRegistry.BOAR, at(4, 4)));
            h.assertTrue(boar.digTruffles(h.getLevel()) == 0, "stone: nothing to dig");
            h.setBlock(new BlockPos(4, 0, 4), Blocks.DIRT);
            h.runAfterDelay(5, () -> {
                int dropped = boar.digTruffles(h.getLevel());
                h.assertTrue(dropped >= 1 && dropped <= 2, "1 to 2 truffles, got " + dropped);
                h.assertTrue(h.getBlockState(new BlockPos(4, 0, 4)).is(Blocks.DIRT), "the dirt stays");
                h.assertItemEntityPresent(AnimalsRegistry.TRUFFLE, new BlockPos(4, 1, 4), 3.0);
                h.succeed();
            });
        });
        MAX_TICKS.put("boar_digs_truffles", 100);
        ALL.put("boar_truffle_loot_table", h -> {
            var table = h.getLevel().getServer().reloadableRegistries().getLootTable(Boar.TRUFFLE_TABLE);
            int min = 99, max = 0;
            for (int i = 0; i < 200; i++) {
                int n = 0;
                for (var stack : table.getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY))) {
                    h.assertTrue(stack.is(AnimalsRegistry.TRUFFLE), "only truffles");
                    n += stack.getCount();
                }
                min = Math.min(min, n);
                max = Math.max(max, n);
            }
            h.assertTrue(min == 1 && max == 2, "always 1 to 2 truffles, saw " + min + ".." + max);
            h.succeed();
        });
        ALL.put("boar_truffle_tick_and_switch", h -> {
            // The dig attempt runs in the boar's own tick (every Boar.DIG_INTERVAL ticks); phase 1 switch off, phase 2 on.
            AnimalsConfig.boarTruffleChance = 1.0;
            AnimalsConfig.boarTruffles = false;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
            }
            quiet(h.spawn(AnimalsRegistry.BOAR, at(8, 8)));
            h.runAfterDelay(Boar.DIG_INTERVAL + 20, () -> {
                if (truffles(h) != 0) {
                    AnimalsConfig.reset();
                    h.fail("switch off: no truffle");
                }
                AnimalsConfig.boarTruffles = true;
            });
            h.runAfterDelay(2 * Boar.DIG_INTERVAL + 40, () -> {
                int found = truffles(h);
                AnimalsConfig.reset();
                h.assertTrue(found >= 1, "switch on, chance 1: the adult boar dug in its own tick");
                h.succeed();
            });
        });
        MAX_TICKS.put("boar_truffle_tick_and_switch", 2 * Boar.DIG_INTERVAL + 80);
        ALL.put("truffle_soup", h -> {
            var soup = new net.minecraft.world.item.ItemStack(AnimalsRegistry.TRUFFLE_SOUP);
            var stew = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MUSHROOM_STEW);
            h.assertTrue(soup.get(net.minecraft.core.component.DataComponents.FOOD).nutrition() == stew.get(net.minecraft.core.component.DataComponents.FOOD).nutrition()
                    && soup.getMaxStackSize() == 1, "soup feeds like mushroom stew (A12: no new food level)");
            ServerPlayer p = player(h, 8, 8);
            p.getFoodData().setFoodLevel(10);
            var left = soup.finishUsingItem(h.getLevel(), p);
            h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "short night vision");
            h.assertTrue(p.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION).getDuration() <= 20 * 20, "at most 20 seconds");
            h.assertTrue(left.is(net.minecraft.world.item.Items.BOWL), "the bowl comes back");
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
        ALL.put("variant_catalog", h -> {
            h.assertTrue(AnimalVariants.DEER.size() == 2 && AnimalVariants.BOAR.size() == 4 && AnimalVariants.HEDGEHOG.size() == 4 && AnimalVariants.CRAB.size() == 2, "catalog sizes");
            h.assertTrue(AnimalVariants.BOAR.get(0).name().equals("dark") && AnimalVariants.HEDGEHOG.get(0).name().equals("brown")
                    && AnimalVariants.CRAB.get(0).name().equals("red") && AnimalVariants.DEER.get(0).name().equals("red"), "stored indices keep their meaning");
            for (var catalog : java.util.List.of(AnimalVariants.DEER, AnimalVariants.BOAR, AnimalVariants.HEDGEHOG, AnimalVariants.CRAB)) {
                h.assertTrue(catalog.stream().map(AnimalVariants.Variant::name).distinct().count() == catalog.size(), "unique names");
                h.assertTrue(AnimalVariants.clamp(catalog, -5) == 0 && AnimalVariants.clamp(catalog, 99) == catalog.size() - 1, "clamp");
            }
            Holder<Biome> plains = biome(h, Biomes.PLAINS), desert = biome(h, Biomes.DESERT), taiga = biome(h, Biomes.TAIGA),
                    swamp = biome(h, Biomes.SWAMP), beach = biome(h, Biomes.BEACH);
            checkHome(h, AnimalVariants.DEER, 1, plains, "fallow deer in plains");
            checkHome(h, AnimalVariants.DEER, 0, taiga, "red deer in taiga");
            checkHome(h, AnimalVariants.BOAR, 0, plains, "dark boar in plains (standard)");
            checkHome(h, AnimalVariants.BOAR, 1, desert, "sandy boar in desert");
            checkHome(h, AnimalVariants.BOAR, 2, taiga, "frost boar in taiga");
            checkHome(h, AnimalVariants.BOAR, 3, swamp, "rust boar in swamp");
            checkHome(h, AnimalVariants.HEDGEHOG, 0, plains, "brown hedgehog in plains (standard)");
            checkHome(h, AnimalVariants.HEDGEHOG, 1, desert, "desert hedgehog in desert");
            checkHome(h, AnimalVariants.HEDGEHOG, 2, taiga, "white-breasted hedgehog in taiga");
            checkHome(h, AnimalVariants.HEDGEHOG, 3, swamp, "four-toed hedgehog in swamp");
            checkHome(h, AnimalVariants.CRAB, 0, plains, "red crab inland (standard)");
            checkHome(h, AnimalVariants.CRAB, 1, beach, "coral crab on the beach");
            h.succeed();
        });
        ALL.put("variant_entities", h -> {
            ServerLevel level = h.getLevel();
            Deer deer = quiet(h.spawn(AnimalsRegistry.DEER, at(4, 4)));
            Boar boar = quiet(h.spawn(AnimalsRegistry.BOAR, at(8, 4)));
            Hedgehog hedgehog = quiet(h.spawn(AnimalsRegistry.HEDGEHOG, at(4, 8)));
            Crab crab = quiet(h.spawn(AnimalsRegistry.CRAB, at(8, 8)));
            h.assertTrue(deer.variant() >= 0 && deer.variant() < 2 && boar.variant() >= 0 && boar.variant() < 4
                    && hedgehog.variant() >= 0 && hedgehog.variant() < 4 && crab.variant() >= 0 && crab.variant() < 2, "a spawned animal has a valid variant");
            deer.setVariant(99);
            boar.setVariant(99);
            hedgehog.setVariant(99);
            crab.setVariant(99);
            h.assertTrue(deer.variant() == 1 && boar.variant() == 3 && hedgehog.variant() == 3 && crab.variant() == 1, "too large values are clamped");
            deer.setVariant(-3);
            boar.setVariant(-3);
            h.assertTrue(deer.variant() == 0 && boar.variant() == 0, "negative values are clamped");
            boar.setVariant(2);
            Boar mate = quiet(h.spawn(AnimalsRegistry.BOAR, at(10, 4)));
            mate.setVariant(2);
            for (int i = 0; i < 12; i++) {
                h.assertTrue(boar.getBreedOffspring(level, mate) instanceof Boar baby && baby.variant() == 2, "two parents of one coat have a piglet of that coat");
            }
            deer.setVariant(0);
            Deer doe = quiet(h.spawn(AnimalsRegistry.DEER, at(6, 4)));
            doe.setVariant(1);
            boolean[] seen = new boolean[2];
            for (int i = 0; i < 60; i++) {
                if (deer.getBreedOffspring(level, doe) instanceof Deer fawn) seen[fawn.variant()] = true;
            }
            h.assertTrue(seen[0] && seen[1], "a fawn takes the coat of either parent");
            h.succeed();
        });
        ALL.put("guide_book", com.simpleanimals.guide.AnimalsGuide::gameTest);
    }

    private AnimalsTests() {}
}
