package com.simplebuilding.gametest;

import com.simplebuilding.config.ConfigOptions;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.network.ElytraBoostPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Server counterexamples, including the actual C2S handler used by both loaders. */
public final class HardenTests {
    private HardenTests() {}
    public static void hardenBalancingEchoSounderAttemptLockTicks(GameTestHelper h) { cap(h, "balancing.echoSounderAttemptLockTicks", TweaksConfig.MAX_ECHO_COOLDOWN_TICKS); }
    public static void hardenBalancingEchoSounderJumpCooldownTicks(GameTestHelper h) { cap(h, "balancing.echoSounderJumpCooldownTicks", TweaksConfig.MAX_ECHO_COOLDOWN_TICKS); }
    public static void hardenLaserPointerRange(GameTestHelper h) { cap(h, "laserPointer.range", TweaksConfig.MAX_LASER_RANGE); }
    public static void hardenSpawnBoostStrength(GameTestHelper h) { cap(h, "spawn.boostStrength", TweaksConfig.MAX_BOOST_STRENGTH); }
    public static void hardenSpawnSpawnElytraRadius(GameTestHelper h) { cap(h, "spawn.spawnElytraRadius", TweaksConfig.MAX_SPAWN_ELYTRA_RADIUS); }
    public static void hardenOptimizationXpClumpRadius(GameTestHelper h) { cap(h, "optimization.xpClumpRadius", TweaksConfig.MAX_XP_CLUMP_RADIUS); }
    public static void hardenCommandsKillCommandRadius(GameTestHelper h) { cap(h, "commands.killCommandRadius", TweaksConfig.MAX_KILL_COMMAND_RADIUS); }
    public static void hardenPadTuningPotionPadCooldownFactor(GameTestHelper h) { cap(h, "padTuning.potionPadCooldownFactor", TweaksConfig.MAX_POTION_COOLDOWN_FACTOR); }
    public static void hardenPadTuningPotionPadChargeStepTicks(GameTestHelper h) { cap(h, "padTuning.potionPadChargeStepTicks", TweaksConfig.MAX_POTION_CHARGE_STEP_TICKS); }
    public static void hardenPadTuningLaunchpadStrengthMultiplier(GameTestHelper h) { cap(h, "padTuning.launchpadStrengthMultiplier", TweaksConfig.MAX_LAUNCHPAD_STRENGTH_MULTIPLIER); }
    public static void hardenPadTuningTeleporterTier3WarmupTicks(GameTestHelper h) { cap(h, "padTuning.teleporterTier3WarmupTicks", TweaksConfig.MAX_TELEPORTER_WARMUP_TICKS); }
    public static void hardenPadTuningTeleporterTier2WarmupTicks(GameTestHelper h) { cap(h, "padTuning.teleporterTier2WarmupTicks", TweaksConfig.MAX_TELEPORTER_WARMUP_TICKS); }
    public static void hardenPadTuningTeleporterTier1WarmupTicks(GameTestHelper h) { cap(h, "padTuning.teleporterTier1WarmupTicks", TweaksConfig.MAX_TELEPORTER_WARMUP_TICKS); }

    public static void cap(GameTestHelper helper, String path, double maximum) {
        var option = ConfigOptions.byPath("tweaks." + path);
        SimplebuildingConfig config = new SimplebuildingConfig();
        Object defaultValue = option.get(config);
        helper.assertTrue(option.parse("NaN") == null && option.parse("Infinity") == null
                && option.parse("-Infinity") == null, path + " accepted nonfinite command input");
        Object excessive = option.parse(option.field().getType() == int.class ? "2147483647" : "1e30");
        helper.assertTrue(excessive != null && option.set(config, excessive), path + " test setup failed");
        config.tweaks.validate();
        helper.assertTrue(((Number) option.get(config)).doubleValue() <= maximum, path + " escaped cap");
        if (option.field().getType() != int.class) {
            for (String value : new String[] {"NaN", "Infinity", "-Infinity"}) {
                Object invalid = option.field().getType() == float.class
                        ? (Object) Float.valueOf(value) : Double.valueOf(value);
                option.set(config, invalid);
                config.tweaks.validate();
                helper.assertTrue(option.get(config).equals(defaultValue), path + " did not restore finite default");
            }
        }
        option.set(config, defaultValue);
        config.tweaks.validate();
        helper.assertTrue(option.get(config).equals(defaultValue), path + " default changed");
        helper.succeed();
    }

    public static void boostPacketBudget(GameTestHelper helper) {
        var spawn = SimpleTweaks.config().spawn;
        float previous = spawn.boostStrength;
        var player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        ItemStack chest = new ItemStack(TweaksItems.SPAWN_ELYTRA);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        chest.set(TweaksComponents.BOOST_LEVEL, 1f);
        try {
            spawn.boostStrength = Float.MAX_VALUE;
            TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
            helper.assertTrue(chest.get(TweaksComponents.BOOST_LEVEL) == 1f, "on-foot packet spent charge");
            player.startFallFlying();
            chest.set(TweaksComponents.BOOST_LEVEL, Float.NaN);
            TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
            helper.assertTrue(player.getDeltaMovement().length() == 0, "NaN charge allowed boost");
            for (int i = 0; i < TweaksNetwork.BOOSTS_PER_WINDOW; i++) {
                chest.set(TweaksComponents.BOOST_LEVEL, 1f);
                player.setDeltaMovement(new Vec3(0, 0, 100));
                TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
                helper.assertTrue(player.getDeltaMovement().length() <= TweaksNetwork.MAX_BOOST_VELOCITY + 1e-9,
                        "huge strength escaped absolute velocity cap");
            }
            for (int i = 0; i < 100; i++) {
                // Refill the same component that spawn and pads recharge; budget must survive it.
                chest.set(TweaksComponents.BOOST_LEVEL, 1f);
                player.setDeltaMovement(Vec3.ZERO);
                TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
                helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO)
                        && chest.get(TweaksComponents.BOOST_LEVEL) == 1f, "refill bypassed packet budget");
            }
        } finally { spawn.boostStrength = previous; }
        helper.runAfterDelay(TweaksNetwork.BOOST_WINDOW_TICKS, () -> {
            chest.set(TweaksComponents.BOOST_LEVEL, 1f);
            player.setItemSlot(EquipmentSlot.CHEST, chest);
            player.startFallFlying();
            player.setDeltaMovement(Vec3.ZERO);
            TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
            helper.assertTrue(player.getDeltaMovement().length() > 0, "budget never recovered after window");
            helper.succeed();
        });
    }

    public static void boostNonfiniteAndWrongEquipment(GameTestHelper helper) {
        var spawn = SimpleTweaks.config().spawn;
        float previous = spawn.boostStrength;
        try {
            for (float value : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
                var player = helper.makeMockServerPlayerInLevel();
                helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
                player.startFallFlying();
                player.setDeltaMovement(Vec3.ZERO);
                TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
                helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "wrong equipment boosted");
                var chest = new ItemStack(TweaksItems.SPAWN_ELYTRA);
                player.setItemSlot(EquipmentSlot.CHEST, chest);
                chest.set(TweaksComponents.BOOST_LEVEL, 0f);
                TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
                helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "empty charge boosted");
                chest.set(TweaksComponents.BOOST_LEVEL, Float.POSITIVE_INFINITY);
                TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
                helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "infinite charge boosted");
                chest.set(TweaksComponents.BOOST_LEVEL, 1f);
                spawn.boostStrength = value;
                player.startFallFlying();
                TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
                helper.assertTrue(player.getDeltaMovement().distanceTo(player.getLookAngle().scale(.6)) < 1e-9,
                        "nonfinite strength did not use finite default: " + player.getDeltaMovement()
                                + "; flying=" + player.isFallFlying());
            }
        } finally { spawn.boostStrength = previous; }
        helper.succeed();
    }

    public static void allDefaultsUnchangedByValidation(GameTestHelper helper) {
        var config = new SimplebuildingConfig();
        var before = new java.util.LinkedHashMap<String, Object>();
        ConfigOptions.all().forEach(o -> before.put(o.path(), o.get(config)));
        config.validatePostLoad();
        ConfigOptions.all().forEach(o -> helper.assertTrue(java.util.Objects.equals(before.get(o.path()), o.get(config)),
                "validation changed default " + o.path()));
        helper.succeed();
    }

    public static void boostCommandRefusesInvalidStrength(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        float before = SimpleTweaks.config().spawn.boostStrength;
        var saver = SimpleTweaks.configSaver();
        int[] saves = {0};
        try {
            SimpleTweaks.setConfigSaver(() -> saves[0]++);
            for (String value : new String[] {"NaN", "Infinity", "-Infinity", "1000000", "0.01"}) {
                int result = 0;
                try {
                    result = dispatcher.execute("simplebuilding tweaks spawn elytra boostStrength " + value,
                            server.createCommandSourceStack().withSuppressedOutput());
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
                helper.assertTrue(result == 0 && SimpleTweaks.config().spawn.boostStrength == before,
                        "invalid boost command changed strength: " + value);
            }
            helper.assertTrue(saves[0] == 0, "refused boost command rewrote config");
        } finally { SimpleTweaks.setConfigSaver(saver); }
        helper.succeed();
    }

    public static void xpAndLaunchRuntimeCaps(GameTestHelper helper) {
        var config = SimpleTweaks.config();
        double radius = config.optimization.xpClumpRadius;
        double factor = config.padTuning.launchpadStrengthMultiplier;
        var at = helper.absoluteVec(new Vec3(2, 2, 2));
        var a = new net.minecraft.world.entity.ExperienceOrb(helper.getLevel(), at.x, at.y, at.z, 3);
        var b = new net.minecraft.world.entity.ExperienceOrb(helper.getLevel(), at.x + 10, at.y, at.z, 4);
        helper.getLevel().addFreshEntity(a);
        helper.getLevel().addFreshEntity(b);
        try {
            for (double value : new double[] {Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                config.optimization.xpClumpRadius = value;
                config.padTuning.launchpadStrengthMultiplier = value;
                helper.assertTrue(com.simplebuilding.tweaks.xp.XpClumping.radius() <= TweaksConfig.MAX_XP_CLUMP_RADIUS,
                        "runtime XP scan radius escaped cap");
                com.simplebuilding.tweaks.xp.XpClumping.clump(a);
                helper.assertTrue(b.isAlive(), "runtime XP scan reached orb beyond cap");
                double strength = com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity.strengthFor(16);
                helper.assertTrue(Double.isFinite(strength) && strength <= (1.5 + 16 * .8) * 2,
                        "runtime launch escaped cap");
            }
        } finally {
            a.discard(); b.discard();
            config.optimization.xpClumpRadius = radius;
            config.padTuning.launchpadStrengthMultiplier = factor;
        }
        helper.succeed();
    }

    public static void recipeRename(GameTestHelper helper) {
        var config = SimpleTweaks.config().laserPointer;
        boolean previous = config.enable;
        try {
            config.enable = false;
            helper.assertTrue(com.simplebuilding.recipe.RecipeFilter.removes(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "amethyst_lens")),
                    "disabled rod recipe remains craftable");
            helper.assertFalse(com.simplebuilding.recipe.RecipeFilter.removes(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "amethyst_lens")),
                    "foreign recipe removed");
            config.enable = true;
            helper.assertFalse(com.simplebuilding.recipe.RecipeFilter.removes(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "amethyst_lens")),
                    "enabled rod recipe removed");
        } finally { config.enable = previous; }
        helper.succeed();
    }
}
