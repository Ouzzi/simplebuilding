package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

/** MOB27-1 effects Volatile, Deflection and Mirror (shared by the Fabric wrappers). Standalone-safe: only vanilla mobs. */
final class CombatEffectTests {
    private CombatEffectTests() {}

    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        for (int i = -1; i <= 10; i++) for (int j = 0; j <= 10; j++) h.getLevel().setBlock(h.absolutePos(new BlockPos(i, 1, j)), Blocks.STONE.defaultBlockState(), 3);
        var c = h.spawn(EntityTypes.COW, x, 2, z);
        c.setNoAi(true);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        c.setHealth(100.0f);
        return c;
    }

    private static boolean hit(GameTestHelper h, LivingEntity victim, LivingEntity attacker, float dmg) {
        var level = h.getLevel();
        return victim.hurtServer(level, level.damageSources().mobAttack(attacker), dmg);
    }

    private static void close(GameTestHelper h, double a, double b, String what) {
        h.assertTrue(Math.abs(a - b) < 0.05, what + ": expected " + b + " got " + a);
    }

    /** Runs the body with every random roll fixed, then restores randomness. */
    private static void rolled(double roll, Runnable body) {
        CombatEffects.testRoll = roll;
        try { body.run(); } finally { CombatEffects.testRoll = null; }
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("combat_registered", h -> {
            for (var key : java.util.List.of(CombatEffects.VOLATILE_KEY, CombatEffects.DEFLECTION_KEY, CombatEffects.MIRROR_KEY))
                h.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(key.identifier()), "effect " + key.identifier());
            h.succeed();
        });
        add.accept("combat_rules", h -> {
            close(h, CombatEffects.deflectChance(0), 0.1, "deflection I");
            close(h, CombatEffects.deflectChance(4), 0.5, "deflection V");
            close(h, CombatEffects.deflectChance(9), 0.5, "deflection capped at 50 %");
            close(h, CombatEffects.bearerFactor(0), 1.2, "mirror bearer I");
            close(h, CombatEffects.bearerFactor(4), 2.0, "mirror bearer V");
            close(h, CombatEffects.reflectFactor(0), 0.2, "mirror attacker I");
            close(h, CombatEffects.reflectFactor(4), 1.0, "mirror attacker V");
            close(h, CombatEffects.safeReflect(10f, 3f), 3.0, "reflect below health");
            close(h, CombatEffects.safeReflect(2f, 30f), 1.0, "reflect leaves half a heart");
            close(h, CombatEffects.safeReflect(1f, 30f), 0.0, "reflect at half a heart does nothing");
            h.assertTrue(CombatEffects.volatileDamage(0, 0.5) > CombatEffects.volatileDamage(0, 3.0), "volatile falls off with distance");
            close(h, CombatEffects.volatileDamage(0, 1.5), 6.25, "volatile I: ~3 hearts at melee distance");
            close(h, CombatEffects.volatileDamage(0, CombatEffects.volatileRadius(0)), 0.0, "nothing at the radius");
            h.assertTrue(CombatEffects.volatileDamage(4, 1.5) > CombatEffects.volatileDamage(0, 1.5), "higher level hits harder");
            h.succeed();
        });
        add.accept("deflection_roll", h -> {
            var attacker = cow(h, 8, 8);
            var a = cow(h, 1, 1);
            a.addEffect(new MobEffectInstance(CombatEffects.deflection(), 600, 4));
            rolled(0.0, () -> h.assertFalse(hit(h, a, attacker, 5f), "roll below the chance: the hit is negated"));
            close(h, a.getHealth(), 100.0, "no damage when deflected");
            rolled(0.0, () -> a.hurtServer(h.getLevel(), h.getLevel().damageSources().fall(), 3f));
            h.assertTrue(a.getHealth() < 100.0f, "fall damage has no attacker and is never deflected");
            var b = cow(h, 3, 1);
            b.addEffect(new MobEffectInstance(CombatEffects.deflection(), 600, 4));
            rolled(0.99, () -> h.assertTrue(hit(h, b, attacker, 5f), "roll above the chance: the hit lands"));
            close(h, b.getHealth(), 95.0, "full damage");
            h.succeed();
        });
        add.accept("mirror_reflect", h -> {
            var attacker = cow(h, 8, 8);
            var bearer = cow(h, 1, 1);
            bearer.addEffect(new MobEffectInstance(CombatEffects.mirror(), 600, 0));
            rolled(0.0, () -> hit(h, bearer, attacker, 10f));
            close(h, bearer.getHealth(), 90.0, "bearer takes the plain damage when the coin reflects");
            close(h, attacker.getHealth(), 98.0, "attacker takes 20 % at level I");
            var bearer5 = cow(h, 3, 1);
            var attacker5 = cow(h, 6, 6);
            bearer5.addEffect(new MobEffectInstance(CombatEffects.mirror(), 600, 4));
            rolled(0.0, () -> hit(h, bearer5, attacker5, 10f));
            close(h, attacker5.getHealth(), 90.0, "attacker takes 100 % at level V");
            h.succeed();
        });
        add.accept("mirror_bearer", h -> {
            var attacker = cow(h, 8, 8);
            var bearer = cow(h, 1, 1);
            bearer.addEffect(new MobEffectInstance(CombatEffects.mirror(), 600, 0));
            rolled(0.99, () -> hit(h, bearer, attacker, 10f));
            close(h, bearer.getHealth(), 88.0, "bearer takes 120 % at level I");
            close(h, attacker.getHealth(), 100.0, "attacker is untouched when the coin favours the hit");
            var bearer5 = cow(h, 3, 1);
            bearer5.addEffect(new MobEffectInstance(CombatEffects.mirror(), 600, 4));
            rolled(0.99, () -> hit(h, bearer5, attacker, 10f));
            close(h, bearer5.getHealth(), 80.0, "bearer takes 200 % at level V");
            h.succeed();
        });
        add.accept("mirror_lethal", h -> {
            var attacker = cow(h, 8, 8);
            attacker.setHealth(1.5f);
            var bearer = cow(h, 1, 1);
            bearer.addEffect(new MobEffectInstance(CombatEffects.mirror(), 600, 4));
            rolled(0.0, () -> hit(h, bearer, attacker, 10f));
            h.assertTrue(attacker.isAlive(), "reflected damage never kills");
            close(h, attacker.getHealth(), 1.0, "the attacker is left at half a heart");
            h.succeed();
        });
        add.accept("volatile_explosion", h -> {
            var bearer = cow(h, 5, 5);
            var attacker = cow(h, 5, 7);   // 2 blocks away
            var bystander = cow(h, 5, 8);  // 3 blocks away
            var far = cow(h, 5, 10);       // 5 blocks away
            BlockPos next = h.absolutePos(new BlockPos(4, 2, 5));
            h.getLevel().setBlock(next, Blocks.DIRT.defaultBlockState(), 3);
            bearer.addEffect(new MobEffectInstance(CombatEffects.volatileEffect(), 600, 0));
            double dAttacker = attacker.position().distanceTo(bearer.position()), dBystander = bystander.position().distanceTo(bearer.position());
            hit(h, bearer, attacker, 4f);
            close(h, bearer.getHealth(), 96.0, "the bearer is immune to its own explosion (only the hit counts)");
            close(h, 100.0 - attacker.getHealth(), CombatEffects.volatileDamage(0, dAttacker), "attacker takes the falloff damage");
            close(h, 100.0 - bystander.getHealth(), CombatEffects.volatileDamage(0, dBystander), "bystander takes less, by distance");
            h.assertTrue(bystander.getHealth() > attacker.getHealth(), "damage falls off with distance");
            close(h, far.getHealth(), 100.0, "outside the radius nothing happens");
            h.assertTrue(h.getLevel().getBlockState(next).is(Blocks.DIRT), "no block damage");
            h.succeed();
        });
    }
}
