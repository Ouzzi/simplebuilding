package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.framework.api.ResonanceContracts;
import com.simplebuilding.framework.api.SubMods;
import com.simplebuilding.framework.api.TrimContracts;
import com.simplebuilding.modules.simplemobs.trim.TrimAbilities;
import com.simplebuilding.modules.simplemobs.trim.TrimAbilityAttributes;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.block.Blocks;

/**
 * MOB27-3: the armour trim abilities in Simple Mobs ({@link TrimAbilities}, moved from the SimpleBuilding core). The rate table
 * (the same numbers the core's TrimBonus/TrimEffect tests pin), damage per pattern and material with cap and floor, the input chain
 * (host, then Simple Resonance, then the fixed value), the switches (player, sub-mod, claim), the attribute bonuses, the player
 * values, and a real hit through {@code hurtServer} that must be reduced exactly once - with or without SimpleBuilding loaded. Every
 * test installs its own host and puts the old one (SimpleBuilding's, when present) back, so the bodies run alone and with the core.
 */
final class TrimAbilityTests {
    private TrimAbilityTests() {}

    private static final float HIT = 10.0f;
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Chainmail: the abilities read the trim material, never the armour item, and "chainmail" names no trim material. */
    private static final Item[] ARMOUR = {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS};

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void near(double actual, double expected, String what) {
        check(Math.abs(actual - expected) <= 1.0e-4, what + ": expected " + expected + " got " + actual);
    }

    /** A host with fixed answers (resonance, strength, switch). */
    private static final class Fixed implements TrimContracts.Host {
        double multiplier;
        float strength = 1.0f;
        boolean on = true;

        Fixed(double multiplier) {
            this.multiplier = multiplier;
        }

        @Override public double multiplier(Object livingEntity) { return multiplier; }
        @Override public float strength(String bonusKey) { return strength; }
        @Override public boolean enabled(Object livingEntity) { return on; }
    }

    /** A Resonance service that only answers the trim multiplier. */
    private static final class Resonance implements ResonanceContracts.Service {
        final double trim;

        Resonance(double trim) {
            this.trim = trim;
        }

        @Override public boolean enabled() { return true; }
        @Override public double playerValue(UUID player) { return 0; }
        @Override public double playerLevel(UUID player) { return 0; }
        @Override public double fraction(UUID player) { return 0; }
        @Override public double trimMultiplier(UUID player) { return trim; }
        @Override public double zoneLevel(String dimension, double x, double z) { return 0; }
        @Override public Optional<UUID> pickOwner(String dimension, double x, double y, double z, double roll) { return Optional.empty(); }
        @Override public ResonanceContracts.Spawn sample(String dimension, double x, double y, double z, double ownerRoll, double noiseRoll) {
            return ResonanceContracts.Spawn.NONE;
        }
        @Override public void award(UUID player, String source, double points) {}
        @Override public boolean isNewcomer(UUID player) { return false; }
    }

    /** Runs {@code body} with {@code host} and {@code resonance} in place and puts the old ones back. */
    private static void with(TrimContracts.Host host, ResonanceContracts.Service resonance, Runnable body) {
        TrimContracts.Host oldHost = TrimContracts.host();
        ResonanceContracts.Service oldResonance = ResonanceContracts.get();
        try {
            TrimContracts.setHost(host);
            ResonanceContracts.set(resonance);
            body.run();
        } finally {
            TrimContracts.setHost(oldHost);
            ResonanceContracts.set(oldResonance);
        }
    }

    private static Holder<TrimMaterial> material(GameTestHelper h, ResourceKey<TrimMaterial> key) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).get(key).orElseThrow();
    }

    private static Holder<TrimPattern> pattern(GameTestHelper h, ResourceKey<TrimPattern> key) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).get(key).orElseThrow();
    }

    /** Puts four armour pieces with the trim on (the only armour the wearer has). */
    private static void wear(GameTestHelper h, LivingEntity entity, ResourceKey<TrimMaterial> mat, ResourceKey<TrimPattern> pat) {
        ArmorTrim trim = new ArmorTrim(material(h, mat), pattern(h, pat));
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = new ItemStack(ARMOUR[i]);
            stack.set(DataComponents.TRIM, trim);
            entity.setItemSlot(SLOTS[i], stack);
        }
    }

    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        for (int i = 0; i <= 4; i++) for (int j = 0; j <= 4; j++) h.getLevel().setBlock(h.absolutePos(new BlockPos(i, 1, j)), Blocks.STONE.defaultBlockState(), 3);
        var c = h.spawn(EntityTypes.COW, x, 2, z);
        c.setNoAi(true);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        c.setHealth(100.0f);
        return c;
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("trim_ability_table", h -> {
            // The comparison table: the numbers of the core's TrimEffectUtil (docs/TRIM-BALANCE.md), per piece at resonance 1.0.
            near(TrimAbilities.DAMAGE_FLOOR, 0.2, "damage floor");
            near(TrimAbilities.GENERIC_REDUCTION_CAP, 0.25, "cap of the bonuses against every damage");
            near(TrimAbilities.WARD_ALL, 0.03, "ward");
            near(TrimAbilities.SENTRY_PROJECTILE, 0.05, "sentry");
            near(TrimAbilities.SPIRE_FALL, 0.08, "spire");
            near(TrimAbilities.SILENCE_STEALTH, 0.08, "silence stealth");
            near(TrimAbilities.COAST_AIR_SAVE, 0.10, "coast air");
            near(TrimAbilities.RAISER_XP, 0.10, "raiser");
            near(TrimAbilities.HOST_LUCK, 0.5, "host luck");
            near(TrimAbilities.SHAPER_REACH, 0.25, "shaper reach");
            near(TrimAbilities.DIAMOND_PHYSICAL, 0.03, "diamond");
            near(TrimAbilities.ENDERITE_ALL, 0.05, "enderite");
            near(TrimAbilities.RESIN_KNOCKBACK_RESISTANCE, 0.025, "resin");
            near(TrimAbilities.MAX_LAND_SPEED_BONUS, 0.2, "land speed cap");
            near(TrimAbilities.MAX_XP_BONUS, 0.5, "experience cap");
            near(TrimAbilities.MAX_STEALTH_REDUCTION, 0.5, "stealth cap");
            near(TrimAbilities.MAX_AIR_SAVE_CHANCE, 0.75, "air cap");
            check(TrimAbilities.MAX_WITHER_REDUCTION_TICKS == 100, "wither cap 5 s");
            near(TrimAbilities.NON_PLAYER_MULTIPLIER, 0.2, "resonance of non-players");
            near(TrimAbilities.patternWeight("netherite"), 1.75, "netherite weight");
            near(TrimAbilities.patternWeight("enderite"), 2.0, "enderite weight");
            near(TrimAbilities.patternWeight("gold"), 1.0, "other weight");
            check(TrimAbilities.isOwn(Identifier.withDefaultNamespace("ward"), "ward"), "vanilla pattern counts");
            check(TrimAbilities.isOwn(Identifier.fromNamespaceAndPath("simplebuilding", "enderite"), "enderite"), "Simple-mod material counts");
            check(!TrimAbilities.isOwn(Identifier.fromNamespaceAndPath("othermod", "ward"), "ward"), "a foreign namesake does not count");
            check(TrimAbilities.stasisAmplifier(1.0, 4) == 0 && TrimAbilities.stasisAmplifier(2.0, 4) == 1 && TrimAbilities.stasisAmplifier(0.2, 4) == -1,
                    "stasis resistance steps");
            h.succeed();
        });
        add.accept("trim_ability_damage", h -> {
            Fixed host = new Fixed(1.0);
            with(host, null, () -> {
                LivingEntity cow = cow(h, 2, 2);
                DamageSource wall = h.getLevel().damageSources().flyIntoWall();
                // mob_attack is in no bypass tag (generic and fall bypass armour, so diamond would not count).
                DamageSource physical = h.getLevel().damageSources().mobAttack(cow);
                wear(h, cow, TrimMaterials.COPPER, TrimPatterns.WARD);
                near(TrimAbilities.modifyDamage(cow, HIT, wall), HIT * (1 - 4 * 0.03), "four ward pieces: 12 % off every hit");
                wear(h, cow, TrimMaterials.DIAMOND, TrimPatterns.SHAPER);
                near(TrimAbilities.modifyDamage(cow, HIT, physical), HIT * (1 - 4 * 0.03), "four diamond pieces: 12 % off a physical hit");
                near(TrimAbilities.modifyDamage(cow, HIT, wall), HIT, "diamond does nothing against armour-piercing damage");
                wear(h, cow, TrimMaterials.NETHERITE, TrimPatterns.WARD);
                near(TrimAbilities.modifyDamage(cow, HIT, wall), HIT * (1 - 4 * 1.75 * 0.03), "netherite pieces weigh 1.75 for the pattern");
                wear(h, cow, TrimMaterials.COPPER, TrimPatterns.SPIRE);
                near(TrimAbilities.modifyDamage(cow, HIT, h.getLevel().damageSources().fall()), HIT * (1 - 4 * 0.08), "spire: 8 % per piece against falls");
                host.multiplier = 5.0;
                near(TrimAbilities.modifyDamage(cow, HIT, h.getLevel().damageSources().fall()), HIT * 0.2, "never below the 20 % floor");
                host.multiplier = 4.0;
                wear(h, cow, TrimMaterials.COPPER, TrimPatterns.WARD);
                near(TrimAbilities.modifyDamage(cow, HIT, wall), HIT * 0.75, "the every-damage bonuses stop at 25 %");
                host.multiplier = 1.0;
                host.strength = 2.0f;
                near(TrimAbilities.modifyDamage(cow, HIT, wall), HIT * (1 - 4 * 0.03 * 2), "the host's strength scales the rate");
                host.strength = 1.0f;
                host.on = false;
                near(TrimAbilities.modifyDamage(cow, HIT, wall), HIT, "the wearer's switch takes every bonus away");
                check(TrimAbilities.getTrimCount(cow, "ward") == 0 && TrimAbilities.getMaterialCount(cow, "copper") == 0, "switched off counts nothing");
                cow.discard();
            });
            h.succeed();
        });
        add.accept("trim_ability_inputs", h -> {
            ServerPlayer player = player(h);
            LivingEntity cow = cow(h, 2, 2);
            with(new Fixed(0.7), new Resonance(1.5), () -> {
                near(TrimAbilities.resonance(player), 0.7, "the host comes first");
                near(TrimAbilities.strength("fire_protection"), 1.0, "host strength");
            });
            with(new Fixed(Double.NaN), new Resonance(1.5), () -> {
                near(TrimAbilities.resonance(player), 1.5, "a host that does not know the wearer hands over to Simple Resonance");
                near(TrimAbilities.resonance(cow), 0.2, "non-players keep the fixed value");
            });
            with(null, new Resonance(1.5), () -> {
                near(TrimAbilities.resonance(player), 1.5, "without a host Simple Resonance measures");
                near(TrimAbilities.strength("fire_protection"), 1.0, "designed strength without a host");
                check(TrimAbilities.enabled(player), "on without a host");
            });
            with(null, null, () -> near(TrimAbilities.resonance(player), 0.2, "neither host nor Resonance: the fixed value"));
            cow.discard();
            h.succeed();
        });
        add.accept("trim_ability_switch", h -> {
            if (!TrimAbilities.installed()) {
                // Plan E4: only the Fabric entry point installs the abilities; elsewhere the core keeps them.
                h.succeed();
                return;
            }
            boolean old = SubMods.enabled(MobsRegistry.ID);
            try {
                SubMods.set(MobsRegistry.ID, true);
                check(TrimAbilities.active(), "installed by the Fabric entry point and on");
                check(TrimContracts.claimed(), "the claim reaches the core");
                SubMods.set(MobsRegistry.ID, false);
                check(!TrimAbilities.active() && !TrimContracts.claimed(), "switched off: the core takes the abilities back");
            } finally {
                SubMods.set(MobsRegistry.ID, old);
            }
            h.succeed();
        });
        add.accept("trim_ability_hit", h -> {
            if (!TrimAbilities.installed()) {
                h.succeed();
                return;
            }
            Fixed host = new Fixed(1.0);
            with(host, null, () -> {
                DamageSource wall = h.getLevel().damageSources().flyIntoWall();
                LivingEntity protectedCow = cow(h, 1, 1);
                wear(h, protectedCow, TrimMaterials.COPPER, TrimPatterns.WARD);
                float expected = TrimAbilities.modifyDamage(protectedCow, HIT, wall);
                check(expected < HIT - 0.5f, "setup: four ward pieces must take a measurable part off");
                protectedCow.hurtServer(h.getLevel(), wall, HIT);
                near(100.0f - protectedCow.getHealth(), expected, "the hit goes through the trim reduction exactly once");
                LivingEntity bareCow = cow(h, 3, 3);
                wear(h, bareCow, TrimMaterials.COPPER, TrimPatterns.WARD);
                host.on = false;
                bareCow.hurtServer(h.getLevel(), wall, HIT);
                near(100.0f - bareCow.getHealth(), HIT, "switched off: the full hit");
                protectedCow.discard();
                bareCow.discard();
            });
            h.succeed();
        });
        add.accept("trim_ability_attributes", h -> {
            Fixed host = new Fixed(1.0);
            with(host, null, () -> {
                ServerPlayer player = player(h);
                wear(h, player, TrimMaterials.COPPER, TrimPatterns.BOLT);
                TrimAbilityAttributes.update(player);
                AttributeModifier walking = player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(TrimAbilityAttributes.WALKING_SPEED_ID);
                check(walking != null && walking.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "bolt gives the walking modifier: " + walking);
                near(walking.amount(), 0.2, "four bolt pieces: 4 x 5 %, capped at 20 %");
                check(TrimAbilityAttributes.WALKING_SPEED_ID.equals(Identifier.fromNamespaceAndPath("simplebuilding", "trim_walking_speed")),
                        "the modifier id is the core's (no doubles when the core was in charge before)");
                wear(h, player, TrimMaterials.RESIN, TrimPatterns.SHAPER);
                TrimAbilityAttributes.update(player);
                near(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.1, "resin: 4 x 0.025 knockback resistance");
                near(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) - player.getAttributeBaseValue(Attributes.BLOCK_INTERACTION_RANGE), 1.0,
                        "shaper: 4 x 0.25 blocks of reach");
                check(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(TrimAbilityAttributes.WALKING_SPEED_ID) == null, "bolt taken off: walking modifier gone");
                host.on = false;
                TrimAbilityAttributes.update(player);
                near(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, "switched off: knockback modifier gone");
                host.on = true;
                LivingEntity cow = cow(h, 3, 3);
                wear(h, cow, TrimMaterials.COPPER, TrimPatterns.BOLT);
                TrimAbilityAttributes.update(cow);
                check(cow.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(TrimAbilityAttributes.WALKING_SPEED_ID) == null, "walking speed is a player bonus");
                cow.discard();
            });
            h.succeed();
        });
        add.accept("trim_ability_player", h -> {
            with(new Fixed(1.0), null, () -> {
                ServerPlayer player = player(h);
                wear(h, player, TrimMaterials.COPPER, TrimPatterns.RAISER);
                near(TrimAbilities.getXPMultiplier(player), 1.4, "four raiser pieces: +40 % experience");
                int before = player.totalExperience;
                player.giveExperiencePoints(5);
                check(player.totalExperience - before == 7, "the experience hook pays 5 x 1.4 = 7, got " + (player.totalExperience - before));
                before = player.totalExperience;
                player.giveExperiencePoints(-4);
                check(player.totalExperience - before == -4, "a loss is never multiplied");
                wear(h, player, TrimMaterials.COPPER, TrimPatterns.WAYFINDER);
                near(TrimAbilities.getExhaustionReduction(player), 0.4, "wayfinder: 40 % less sprint hunger");
                wear(h, player, TrimMaterials.COPPER, TrimPatterns.SILENCE);
                near(TrimAbilities.getStealthMultiplier(player), 0.68, "silence: 32 % less visible");
                wear(h, player, TrimMaterials.COPPER, TrimPatterns.COAST);
                near(TrimAbilities.getAirSaveChance(player), 0.4, "coast: 40 % air saving");
                wear(h, player, TrimMaterials.COPPER, TrimPatterns.RIB);
                check(TrimAbilities.getWitherReductionAmount(player) == 100, "rib: 160 ticks capped at 100");
                wear(h, player, TrimMaterials.COPPER, TrimPatterns.HOST);
                near(TrimAbilities.getLuckBonus(player), 2.0, "host: +2 luck");
                wear(h, player, TrimMaterials.COPPER, TrimPatterns.TIDE);
                near(TrimAbilities.getSwimSpeedMultiplier(player), 1.4, "tide: +40 % swimming");
                wear(h, player, TrimMaterials.AMETHYST, TrimPatterns.SHAPER);
                near(TrimAbilities.getAmethystHealChance(player), 1.0, "amethyst: heal chance capped at 100 %");
            });
            h.succeed();
        });
        add.accept("trim_ability_fallback", h -> {
            // Simple Mobs alone without Simple Resonance: every wearer counts with the fixed resonance 0.2.
            with(null, null, () -> {
                LivingEntity cow = cow(h, 2, 2);
                wear(h, cow, TrimMaterials.COPPER, TrimPatterns.WARD);
                near(TrimAbilities.modifyDamage(cow, HIT, h.getLevel().damageSources().flyIntoWall()), HIT * (1 - 4 * 0.03 * 0.2), "ward at resonance 0.2");
                cow.discard();
            });
            h.succeed();
        });
    }
}
