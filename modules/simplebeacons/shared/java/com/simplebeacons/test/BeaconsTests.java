package com.simplebeacons.test;

import com.google.gson.JsonObject;
import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.SimpleBeacons;
import com.simplebeacons.beacon.BeaconKind;
import com.simplebeacons.beacon.EvilGate;
import com.simplebeacons.beacon.RangeFactor;
import com.simplebeacons.beacon.RangeFactorView;
import com.simplebeacons.beacon.StrongBeaconBlockEntity;
import com.simplebeacons.beacon.StrongBeaconLogic;
import com.simplebeacons.beacon.StrongBeaconMenu;
import com.simplebeacons.conduit.StrongConduitBlockEntity;
import com.simplebeacons.conduit.StrongConduitLogic;
import com.simplebeacons.config.BeaconsConfig;
import com.simplebeacons.upgrade.StrongUpgrades;
import com.simplelib.api.InWorldStrikes;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Loader independent module tests (plan docs/ai/PLAN-SIMPLE-BEACONS-2026-10-13.md); the Fabric/NeoForge/Forge
 * adapters register them as module_game_test_<name> on the {@code simplebeacons:arena} structure (15 x 12 x 15).
 * Every test runs standalone (axe mode) and with a hammer mod (hammer mode, read from SimpleLib, never a class).
 */
public final class BeaconsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    /** Beacon / conduit spot in the arena: a 13 x 13 pyramid fits below (x, z 1..13, y 1..6). */
    static final BlockPos CENTER = new BlockPos(7, 7, 7);

    static {
        // CMD24: /simplebeacons config set/get/reset through the server's dispatcher, clamp, map group of effect switches.
        ALL.put("config_command", h -> {
            com.simplelib.test.ConfigCommandTests.roundTrip(h, SimpleBeacons.MOD_ID, "upgradeStrikes", "12", "12", "abc");
            var spec = com.simplelib.api.config.ConfigCommands.spec(SimpleBeacons.MOD_ID, false);
            String before = com.simplelib.api.config.ConfigCommands.show(spec.value("upgradeStrikes"));
            var r = spec.set("upgradeStrikes", "999");
            spec.set("upgradeStrikes", before);
            h.assertTrue(r.status() == com.simplelib.api.config.ConfigCommands.Status.CLAMPED && r.value().equals(String.valueOf(BeaconsConfig.STRIKES_MAX)), "clamp " + r);
            var effect = spec.option("strongBeaconEffects.night_vision");
            h.assertTrue(effect != null && effect.kind() == com.simplelib.api.config.ConfigCommands.Kind.BOOL, "effect switch " + effect);
            h.succeed();
        });
        ALL.put("config_defaults", h -> {
            BeaconsConfig.reset();
            h.assertTrue(BeaconsConfig.enabled && BeaconsConfig.strongBeaconMaxLevels == 6 && BeaconsConfig.strongBeaconRangeBase == 10
                    && BeaconsConfig.strongBeaconRangePerLevel == 10 && BeaconsConfig.strongConduitMaxRange == 216
                    && BeaconsConfig.strongBeaconMaterialRange && BeaconsConfig.vanillaBeaconMaterialRange
                    && BeaconsConfig.upgradeStrikes == 8 && BeaconsConfig.evilBeaconGraceMinutes == 5, "defaults");
            h.assertTrue(BeaconsConfig.EFFECT_IDS.stream().allMatch(BeaconsConfig::effectOn), "all extra effects on by default");
            JsonObject json = new JsonObject();
            json.addProperty("strongBeaconMaxLevels", 9);
            json.addProperty("strongConduitMaxRange", 20);
            json.addProperty("upgradeStrikes", "many");
            json.addProperty("evilBeaconGraceMinutes", 500);
            JsonObject effects = new JsonObject();
            effects.addProperty("luck", false);
            json.add("strongBeaconEffects", effects);
            BeaconsConfig.apply(json);
            h.assertTrue(BeaconsConfig.strongBeaconMaxLevels == 6 && BeaconsConfig.strongConduitMaxRange == 96 && BeaconsConfig.evilBeaconGraceMinutes == 60, "values are clamped");
            h.assertTrue(BeaconsConfig.upgradeStrikes == 8, "junk falls back to the default");
            h.assertTrue(!BeaconsConfig.effectOn("luck") && BeaconsConfig.effectOn("night_vision"), "per effect switch");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("registered", h -> {
            h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimpleBeacons.id("beacons")), "own creative tab beacons is registered");
            for (String id : List.of("strong_upgrade_template", "evil_upgrade_template", "strong_upgrade_shard", "strong_beacon", "mighty_beacon", "evil_beacon",
                    "mighty_evil_beacon", "strong_conduit", "guide_book")) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleBeacons.id(id)), "item missing: " + id);
            }
            h.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleBeacons.id("strong_beacon")) == BeaconsRegistry.STRONG_BEACON, "block");
            for (String id : List.of("mighty_beacon", "evil_beacon", "mighty_evil_beacon")) {
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleBeacons.id(id)), "block missing: " + id);
            }
            h.assertTrue(BuiltInRegistries.MENU.containsKey(SimpleBeacons.id("strong_beacon")), "menu type");
            h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleBeacons.id("strong_conduit")), "conduit block entity type");
            h.succeed();
        });
        ALL.put("strong_items_rarity", h -> {
            // owner 21.10.: the rarest items get SimpleLib's mythic tier (turquoise-green name, glint)
            for (var item : List.of(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE, BeaconsRegistry.EVIL_UPGRADE_TEMPLATE, BeaconsRegistry.STRONG_BEACON_ITEM,
                    BeaconsRegistry.MIGHTY_BEACON_ITEM, BeaconsRegistry.EVIL_BEACON_ITEM, BeaconsRegistry.MIGHTY_EVIL_BEACON_ITEM, BeaconsRegistry.STRONG_CONDUIT_ITEM)) {
                var stack = new net.minecraft.world.item.ItemStack(item);
                h.assertTrue(com.simplelib.api.SimpleRarity.of(stack) == com.simplelib.api.SimpleRarity.MYTHIC, "mythic: " + item);
                h.assertTrue(stack.getRarity() == net.minecraft.world.item.Rarity.EPIC && stack.hasFoil(), "Epic fallback + glint: " + item);
            }
            h.assertTrue(com.simplelib.api.SimpleRarity.of(new net.minecraft.world.item.ItemStack(BeaconsRegistry.STRONG_UPGRADE_SHARD)) == null, "the shard stays plain");
            h.succeed();
        });
        ALL.put("pyramid_levels", h -> {
            BeaconsConfig.reset();
            pyramid(h, 6, Blocks.IRON_BLOCK);
            BlockPos beacon = h.absolutePos(CENTER);
            h.assertTrue(StrongBeaconLogic.scanPyramid(h.getLevel(), beacon, 6) == 6, "full 6 level pyramid (454 blocks)");
            h.assertTrue(StrongBeaconLogic.scanPyramid(h.getLevel(), beacon, 4) == 4, "a Vanilla-sized scan stops at 4");
            h.setBlock(CENTER.offset(-6, -6, 3), Blocks.STONE);
            h.assertTrue(StrongBeaconLogic.scanPyramid(h.getLevel(), beacon, 6) == 5, "a gap in level 6 gives 5");
            h.setBlock(CENTER.offset(5, -5, -5), Blocks.DIRT);
            h.assertTrue(StrongBeaconLogic.scanPyramid(h.getLevel(), beacon, 6) == 4, "a gap in level 5 stops at 4");
            h.assertTrue(StrongBeaconLogic.pyramidBlocks(5) == 285 && StrongBeaconLogic.pyramidBlocks(6) == 454, "pyramid sizes");
            h.succeed();
        });
        ALL.put("range_and_duration", h -> {
            BeaconsConfig.reset();
            h.assertTrue(StrongBeaconLogic.range(4) == 50 && StrongBeaconLogic.range(5) == 60 && StrongBeaconLogic.range(6) == 70, "range 50/60/70");
            h.assertTrue(StrongBeaconLogic.durationTicks(5) == 19 * 20 && StrongBeaconLogic.durationTicks(6) == 21 * 20, "duration 19/21 s");
            JsonObject json = new JsonObject();
            json.addProperty("strongBeaconRangePerLevel", 15);
            BeaconsConfig.apply(json);
            h.assertTrue(StrongBeaconLogic.range(6) == 100, "range per level is configurable");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("effects_per_level", h -> {
            // BS28b-4 (plan docs/ai/PLAN-A-BEACON29-2026-10-29.md): Strong = primary levels 1-4, the secondary power on level 5
            BeaconsConfig.reset();
            BeaconKind strong = BeaconKind.STRONG;
            h.assertTrue(strong.primaryLevels() == 4 && strong.secondaryLevel() == 5 && StrongBeaconLogic.maxLevels(strong) == 5, "Strong: 4 primary levels, secondary on 5");
            for (int lvl : new int[] {1, 2, 3}) h.assertTrue(strong.powers(lvl).size() == 3, "three powers on level " + lvl);
            h.assertTrue(strong.powers(4).size() == 1, "Luck alone on level 4");
            h.assertTrue(StrongBeaconLogic.validate(strong, MobEffects.SPEED, null, 1) && StrongBeaconLogic.validate(strong, MobEffects.HASTE, null, 1)
                    && StrongBeaconLogic.validate(strong, MobEffects.SATURATION, null, 1), "level 1: Speed, Haste, Saturation");
            h.assertTrue(!StrongBeaconLogic.validate(strong, MobEffects.RESISTANCE, null, 1) && StrongBeaconLogic.validate(strong, MobEffects.RESISTANCE, null, 2)
                    && StrongBeaconLogic.validate(strong, MobEffects.JUMP_BOOST, null, 2) && StrongBeaconLogic.validate(strong, MobEffects.SLOW_FALLING, null, 2), "level 2: Resistance, Jump Boost, Slow Falling");
            h.assertTrue(!StrongBeaconLogic.validate(strong, MobEffects.STRENGTH, null, 2) && StrongBeaconLogic.validate(strong, MobEffects.STRENGTH, null, 3)
                    && StrongBeaconLogic.validate(strong, MobEffects.INVISIBILITY, null, 3) && !StrongBeaconLogic.validate(strong, MobEffects.DOLPHINS_GRACE, null, 2)
                    && StrongBeaconLogic.validate(strong, MobEffects.DOLPHINS_GRACE, null, 3), "level 3: Strength, Invisibility, Dolphin's Grace");
            h.assertTrue(!StrongBeaconLogic.validate(strong, MobEffects.LUCK, null, 3) && StrongBeaconLogic.validate(strong, MobEffects.LUCK, null, 4), "level 4: Luck");
            h.assertTrue(!strong.knows(MobEffects.HEALTH_BOOST) && !strong.knows(MobEffects.HERO_OF_THE_VILLAGE) && !strong.knows(MobEffects.NIGHT_VISION)
                    && !strong.knows(MobEffects.WATER_BREATHING) && !strong.knows(MobEffects.FIRE_RESISTANCE) && !strong.knows(MobEffects.ABSORPTION),
                    "Extra Hearts, Hero, Night Vision, Water Breathing, Fire Resistance, Absorption are not on the Strong Beacon");
            h.assertTrue(!StrongBeaconLogic.validate(strong, MobEffects.SPEED, MobEffects.REGENERATION, 4) && StrongBeaconLogic.validate(strong, MobEffects.SPEED, MobEffects.REGENERATION, 5),
                    "the secondary power needs level 5");
            h.assertTrue(StrongBeaconLogic.validate(strong, MobEffects.SPEED, MobEffects.SPEED, 5) && StrongBeaconLogic.validate(strong, MobEffects.STRENGTH, MobEffects.STRENGTH, 5),
                    "Speed II / Strength II stay");
            h.assertTrue(!StrongBeaconLogic.validate(strong, MobEffects.SATURATION, MobEffects.SATURATION, 5) && !StrongBeaconLogic.validate(strong, MobEffects.LUCK, MobEffects.LUCK, 5),
                    "no II for the new powers");
            h.assertTrue(!StrongBeaconLogic.validate(strong, MobEffects.REGENERATION, null, 5) && !StrongBeaconLogic.validate(strong, MobEffects.INSTANT_HEALTH, null, 5), "foreign effects refused");
            JsonObject json = new JsonObject();
            json.addProperty("strongBeaconMaxLevels", 4);
            JsonObject effects = new JsonObject();
            effects.addProperty("luck", false);
            json.add("strongBeaconEffects", effects);
            BeaconsConfig.apply(json);
            h.assertTrue(!StrongBeaconLogic.validate(strong, MobEffects.LUCK, null, 5), "a switched off power is refused");
            h.assertTrue(StrongBeaconLogic.maxLevels(strong) == 4 && !StrongBeaconLogic.validate(strong, MobEffects.SPEED, MobEffects.REGENERATION, 5),
                    "max levels 4: no secondary power");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("mighty_effects", h -> {
            // BS28b-4: Mighty = Strong + level 5 + secondary on level 6, Night Vision, Extra Hearts, placeholders, no Fire Resistance / Absorption
            BeaconsConfig.reset();
            BeaconKind mighty = BeaconKind.MIGHTY;
            h.assertTrue(mighty.primaryLevels() == 5 && mighty.secondaryLevel() == 6 && StrongBeaconLogic.maxLevels(mighty) == 6, "Mighty: 5 primary levels, secondary on 6");
            h.assertTrue(!StrongBeaconLogic.validate(mighty, MobEffects.NIGHT_VISION, null, 3) && StrongBeaconLogic.validate(mighty, MobEffects.NIGHT_VISION, null, 4), "Night Vision on level 4");
            h.assertTrue(!StrongBeaconLogic.validate(mighty, MobEffects.HEALTH_BOOST, null, 4) && StrongBeaconLogic.validate(mighty, MobEffects.HEALTH_BOOST, null, 5), "Extra Hearts on level 5 (owner 28.10.)");
            h.assertTrue(!mighty.knows(MobEffects.FIRE_RESISTANCE) && !mighty.knows(MobEffects.ABSORPTION) && !mighty.knows(MobEffects.HERO_OF_THE_VILLAGE), "no Fire Resistance, Absorption, Hero");
            h.assertTrue(StrongBeaconLogic.validate(mighty, MobEffects.LUCK, null, 4), "Luck is still there");
            // placeholders: Resistance II (level 4), Speed II and Jump Boost II (level 5)
            h.assertTrue(!StrongBeaconLogic.validate(mighty, MobEffects.RESISTANCE, 1, null, 3) && StrongBeaconLogic.validate(mighty, MobEffects.RESISTANCE, 1, null, 4), "Resistance II on level 4");
            h.assertTrue(!StrongBeaconLogic.validate(mighty, MobEffects.SPEED, 1, null, 4) && StrongBeaconLogic.validate(mighty, MobEffects.SPEED, 1, null, 5)
                    && StrongBeaconLogic.validate(mighty, MobEffects.JUMP_BOOST, 1, null, 5), "Speed II and Jump Boost II on level 5");
            h.assertTrue(!StrongBeaconLogic.validate(mighty, MobEffects.HASTE, 1, null, 6), "no Haste II placeholder");
            h.assertTrue(!StrongBeaconLogic.validate(mighty, MobEffects.SPEED, MobEffects.REGENERATION, 5) && StrongBeaconLogic.validate(mighty, MobEffects.SPEED, MobEffects.REGENERATION, 6), "secondary on level 6");
            h.assertTrue(StrongBeaconLogic.validate(mighty, MobEffects.HEALTH_BOOST, MobEffects.HEALTH_BOOST, 6), "the secondary power doubles the Extra Hearts");
            h.assertTrue(!StrongBeaconLogic.validate(mighty, MobEffects.LUCK, MobEffects.LUCK, 6) && !StrongBeaconLogic.validate(mighty, MobEffects.NIGHT_VISION, MobEffects.NIGHT_VISION, 6), "no II for Luck and Night Vision");
            h.assertTrue(StrongBeaconLogic.amplifier(mighty, MobEffects.SPEED, 0, MobEffects.SPEED, 6) == 1 && StrongBeaconLogic.amplifier(mighty, MobEffects.SPEED, 1, MobEffects.SPEED, 6) == 2
                    && StrongBeaconLogic.amplifier(mighty, MobEffects.SPEED, 1, MobEffects.REGENERATION, 6) == 1 && StrongBeaconLogic.amplifier(mighty, MobEffects.HEALTH_BOOST, 0, MobEffects.HEALTH_BOOST, 6) == 1,
                    "the secondary raises a Mighty primary by one (II placeholders to III)");
            h.assertTrue(StrongBeaconLogic.amplifier(BeaconKind.STRONG, MobEffects.SPEED, 0, MobEffects.SPEED, 5) == 1, "Strong: Speed II");
            JsonObject json = new JsonObject();
            json.addProperty("strongBeaconMaxLevels", 5);
            BeaconsConfig.apply(json);
            h.assertTrue(StrongBeaconLogic.maxLevels(mighty) == 5 && !StrongBeaconLogic.validate(mighty, MobEffects.SPEED, MobEffects.REGENERATION, 6), "config max 5 lowers the Mighty Beacon");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("evil_effects", h -> {
            // BS28b-5: mild negative powers; the list is the plan's (docs/ai/PLAN-A-BEACON29-2026-10-29.md)
            BeaconsConfig.reset();
            BeaconKind evil = BeaconKind.EVIL, mightyEvil = BeaconKind.MIGHTY_EVIL;
            h.assertTrue(evil.primaryLevels() == 4 && evil.secondaryLevel() == 5 && mightyEvil.primaryLevels() == 5 && mightyEvil.secondaryLevel() == 6, "levels 1-5 and 1-6");
            for (BeaconKind kind : new BeaconKind[] {evil, mightyEvil}) {
                for (Holder<MobEffect> effect : kind.allEffects()) {
                    h.assertTrue(!effect.value().isBeneficial(), kind + ": " + StrongBeaconLogic.path(effect) + " must not be beneficial");
                    h.assertTrue(!StrongBeaconLogic.VANILLA.contains(effect), kind + ": no Vanilla beacon power");
                }
            }
            h.assertTrue(StrongBeaconLogic.validate(evil, MobEffects.SLOWNESS, null, 1) && !StrongBeaconLogic.validate(evil, MobEffects.WEAKNESS, null, 1)
                    && StrongBeaconLogic.validate(evil, MobEffects.WEAKNESS, null, 2) && !StrongBeaconLogic.validate(evil, MobEffects.DARKNESS, null, 2)
                    && StrongBeaconLogic.validate(evil, MobEffects.DARKNESS, null, 3) && !StrongBeaconLogic.validate(evil, MobEffects.WEAVING, null, 3)
                    && StrongBeaconLogic.validate(evil, MobEffects.WEAVING, null, 4), "one step per level");
            h.assertTrue(!StrongBeaconLogic.validate(evil, MobEffects.SLOWNESS, MobEffects.NAUSEA, 4) && StrongBeaconLogic.validate(evil, MobEffects.SLOWNESS, MobEffects.NAUSEA, 5), "Nausea as the secondary power on level 5");
            h.assertTrue(!StrongBeaconLogic.validate(evil, MobEffects.SLOWNESS, MobEffects.SLOWNESS, 5), "no level II on the Evil Beacon");
            h.assertTrue(!StrongBeaconLogic.validate(evil, MobEffects.SPEED, null, 5) && !StrongBeaconLogic.validate(evil, MobEffects.REGENERATION, null, 5), "no helpful powers");
            h.assertTrue(!StrongBeaconLogic.validate(mightyEvil, MobEffects.WIND_CHARGED, null, 4) && StrongBeaconLogic.validate(mightyEvil, MobEffects.WIND_CHARGED, null, 5)
                    && StrongBeaconLogic.validate(mightyEvil, MobEffects.HUNGER, 1, null, 5) && StrongBeaconLogic.validate(mightyEvil, MobEffects.MINING_FATIGUE, 1, null, 5)
                    && !StrongBeaconLogic.validate(mightyEvil, MobEffects.WEAKNESS, 1, null, 5), "Mighty Evil level 5: Wind Charged, Hunger II, Mining Fatigue II");
            h.assertTrue(StrongBeaconLogic.validate(mightyEvil, MobEffects.SLOWNESS, MobEffects.NAUSEA, 6) && !StrongBeaconLogic.validate(mightyEvil, MobEffects.SLOWNESS, MobEffects.NAUSEA, 5), "Mighty Evil: secondary on level 6");
            h.assertTrue(StrongBeaconLogic.amplifier(mightyEvil, MobEffects.HUNGER, 1, MobEffects.NAUSEA, 6) == 1 && StrongBeaconLogic.amplifier(evil, MobEffects.SLOWNESS, 0, MobEffects.SLOWNESS, 5) == 0, "no raise on the evil kinds");
            h.succeed();
        });
        ALL.put("effects_reach_players", h -> {
            // Range shrunk to 0 + 1 x 6 = 6 blocks, so both players stay in loaded chunks next to the test.
            JsonObject small = new JsonObject();
            small.addProperty("strongBeaconRangeBase", 0);
            small.addProperty("strongBeaconRangePerLevel", 1);
            BeaconsConfig.apply(small);
            BeaconKind mighty = BeaconKind.MIGHTY;
            ServerPlayer near = h.makeMockServerPlayerInLevel();
            ServerPlayer far = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> {
                h.getLevel().getServer().getPlayerList().remove(near);
                h.getLevel().getServer().getPlayerList().remove(far);
            });
            BlockPos beacon = h.absolutePos(CENTER);
            near.snapTo(beacon.getX() + 5.5, beacon.getY(), beacon.getZ() + 0.5);
            far.snapTo(beacon.getX() + 9.5, beacon.getY(), beacon.getZ() + 0.5);
            List<Player> reached = StrongBeaconLogic.applyEffects(h.getLevel(), beacon, mighty, 6, MobEffects.NIGHT_VISION, 0, MobEffects.REGENERATION, RangeFactor.NONE);
            h.assertTrue(reached.contains(near) && !reached.contains(far), "level 6 reaches its range (6 here), not beyond");
            MobEffectInstance sight = near.getEffect(MobEffects.NIGHT_VISION);
            h.assertTrue(sight != null && sight.getAmplifier() == 0 && sight.getDuration() == 21 * 20 && sight.isAmbient(), "night vision I, 21 s, ambient");
            h.assertTrue(near.hasEffect(MobEffects.REGENERATION) && !far.hasEffect(MobEffects.NIGHT_VISION), "secondary too, nothing far away");
            near.removeAllEffects();
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, mighty, 3, MobEffects.NIGHT_VISION, 0, null, RangeFactor.NONE);
            h.assertTrue(!near.hasEffect(MobEffects.NIGHT_VISION), "a level 4 effect does nothing at level 3");
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, mighty, 5, MobEffects.LUCK, 0, MobEffects.REGENERATION, RangeFactor.NONE);
            h.assertTrue(near.hasEffect(MobEffects.LUCK) && !near.hasEffect(MobEffects.REGENERATION), "the secondary power needs level 6");
            near.removeAllEffects();
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, mighty, 6, MobEffects.SPEED, 1, MobEffects.SPEED, RangeFactor.NONE);
            h.assertTrue(near.getEffect(MobEffects.SPEED) != null && near.getEffect(MobEffects.SPEED).getAmplifier() == 2, "Speed II placeholder + the secondary power = Speed III");
            near.removeAllEffects();
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, BeaconKind.STRONG, 5, MobEffects.SPEED, 0, MobEffects.SPEED, RangeFactor.NONE);
            h.assertTrue(near.getEffect(MobEffects.SPEED) != null && near.getEffect(MobEffects.SPEED).getAmplifier() == 1, "Strong: Speed II");
            near.removeAllEffects();
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, mighty, 6, MobEffects.HEALTH_BOOST, 0, null, RangeFactor.NONE);
            h.assertTrue(near.getMaxHealth() == 24.0F, "health boost I = 2 more hearts: " + near.getMaxHealth());
            near.setHealth(24.0F);
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, mighty, 6, MobEffects.HEALTH_BOOST, 0, null, RangeFactor.NONE);
            h.assertTrue(near.getMaxHealth() == 24.0F && near.getHealth() == 24.0F, "renewing keeps the extra hearts");
            // Saturation: a one-tick pulse every third cycle (12 s), never a lasting effect that feeds every tick
            h.assertTrue(StrongBeaconLogic.saturationDue(0) && !StrongBeaconLogic.saturationDue(80) && !StrongBeaconLogic.saturationDue(160)
                    && StrongBeaconLogic.saturationDue(240), "saturation every third cycle");
            near.removeAllEffects();
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, mighty, 6, MobEffects.SATURATION, 0, null, RangeFactor.NONE);
            MobEffectInstance food = near.getEffect(MobEffects.SATURATION);
            h.assertTrue(StrongBeaconLogic.saturationDue(h.getLevel().getGameTime()) ? food != null && food.getDuration() == 1 : food == null,
                    "saturation only as a one-tick pulse: " + food);
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("evil_beacon_delay_and_protection", h -> {
            // BS28b-5: the Evil beacons reach a player only 5 minutes after the last death and never a protected one
            BeaconsConfig.reset();
            JsonObject small = new JsonObject();
            small.addProperty("strongBeaconRangeBase", 0);
            small.addProperty("strongBeaconRangePerLevel", 1);
            BeaconsConfig.apply(small);
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> {
                h.getLevel().getServer().getPlayerList().remove(player);
                com.simplelib.api.HarmShields.unregister("beacons-test");
            });
            player.setGameMode(GameType.SURVIVAL);
            BlockPos beacon = h.absolutePos(CENTER);
            player.snapTo(beacon.getX() + 3.5, beacon.getY(), beacon.getZ() + 0.5);
            var stat = net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TIME_SINCE_DEATH);
            h.assertTrue(BeaconsConfig.evilBeaconGraceMinutes == 5, "default grace: 5 minutes");
            player.getStats().setValue(player, stat, 5 * 1200 - 1);
            h.assertTrue(!EvilGate.affects(player), "one tick short of 5 minutes: not affected");
            h.assertTrue(StrongBeaconLogic.applyEffects(h.getLevel(), beacon, BeaconKind.EVIL, 5, MobEffects.SLOWNESS, 0, MobEffects.NAUSEA, RangeFactor.NONE).isEmpty()
                    && !player.hasEffect(MobEffects.SLOWNESS), "just respawned: the Evil Beacon does nothing");
            player.getStats().setValue(player, stat, 5 * 1200);
            h.assertTrue(EvilGate.affects(player), "after 5 minutes: affected");
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, BeaconKind.EVIL, 5, MobEffects.SLOWNESS, 0, MobEffects.NAUSEA, RangeFactor.NONE);
            MobEffectInstance slow = player.getEffect(MobEffects.SLOWNESS);
            h.assertTrue(slow != null && slow.getAmplifier() == 0 && player.hasEffect(MobEffects.NAUSEA), "Slowness I and Nausea reach the player");
            player.removeAllEffects();
            com.simplelib.api.HarmShields.register("beacons-test", p -> p == player);
            h.assertTrue(!EvilGate.affects(player), "a protected player (newcomer / PvP off) is not affected");
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, BeaconKind.MIGHTY_EVIL, 6, MobEffects.SLOWNESS, 0, null, RangeFactor.NONE);
            h.assertTrue(!player.hasEffect(MobEffects.SLOWNESS), "protection blocks the Mighty Evil Beacon too");
            com.simplelib.api.HarmShields.unregister("beacons-test");
            JsonObject none = new JsonObject();
            none.addProperty("evilBeaconGraceMinutes", 0);
            BeaconsConfig.apply(none);
            player.getStats().setValue(player, stat, 0);
            h.assertTrue(EvilGate.affects(player), "grace 0: at once");
            player.setGameMode(GameType.CREATIVE);
            h.assertTrue(!EvilGate.affects(player), "creative players are skipped");
            // a Strong Beacon never asks the gate: it helps everybody
            player.setGameMode(GameType.SURVIVAL);
            player.getStats().setValue(player, stat, 0);
            BeaconsConfig.reset();
            BeaconsConfig.apply(small);
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, BeaconKind.STRONG, 5, MobEffects.SPEED, 0, null, RangeFactor.NONE);
            h.assertTrue(player.hasEffect(MobEffects.SPEED), "Strong Beacon: no delay");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("menu_checks_the_choice", h -> {
            BeaconsConfig.reset();
            Player player = h.makeMockPlayer(GameType.SURVIVAL);
            SimpleContainerData data = new SimpleContainerData(3);
            data.set(0, 4);
            StrongBeaconMenu menu = new StrongBeaconMenu(0, player.getInventory(), data, ContainerLevelAccess.NULL);
            h.assertTrue(menu.getType() == BeaconsRegistry.STRONG_BEACON_MENU, "own menu type");
            menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
            h.assertTrue(menu.updateEffects(Optional.of(MobEffects.NIGHT_VISION), Optional.empty()), "refused without kicking");
            h.assertTrue(menu.getPrimaryEffect() == null && menu.hasPayment(), "a Mighty power on a Strong menu: nothing set, payment kept");
            h.assertTrue(menu.updateEffects(Optional.of(MobEffects.LUCK), Optional.of(MobEffects.REGENERATION)), "refused: secondary needs level 5");
            h.assertTrue(menu.getPrimaryEffect() == null && menu.hasPayment(), "still nothing set");
            data.set(0, 5);
            h.assertTrue(menu.updateEffects(Optional.of(MobEffects.LUCK), Optional.of(MobEffects.REGENERATION)), "valid choice");
            h.assertTrue(MobEffects.LUCK.equals(menu.getPrimaryEffect()) && MobEffects.REGENERATION.equals(menu.getSecondaryEffect())
                    && !menu.hasPayment(), "choice set, payment taken");
            // a Mighty menu with the synced data: the placeholder button's amplifier (button id) reaches the beacon
            SimpleContainerData synced = new SimpleContainerData(StrongBeaconMenu.DATA_COUNT);
            synced.set(0, 6);
            synced.set(StrongBeaconMenu.DATA_KIND, BeaconKind.MIGHTY.ordinal());
            synced.set(StrongBeaconMenu.DATA_MAX_LEVELS, 6);
            StrongBeaconMenu mighty = new StrongBeaconMenu(0, player.getInventory(), synced, ContainerLevelAccess.NULL);
            h.assertTrue(mighty.kind() == BeaconKind.MIGHTY && mighty.maxLevels() == 6, "the kind rides the menu data");
            mighty.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
            h.assertTrue(mighty.clickMenuButton(player, StrongBeaconMenu.POWER_BUTTON + 1) && !mighty.clickMenuButton(player, 3), "the power button ids are 10 + amplifier");
            h.assertTrue(mighty.updateEffects(Optional.of(MobEffects.SPEED), Optional.of(MobEffects.SPEED)), "Speed II placeholder + secondary");
            h.assertTrue(synced.get(StrongBeaconMenu.DATA_PRIMARY_AMP) == 1 && MobEffects.SPEED.equals(mighty.getPrimaryEffect()), "amplifier 1 stored");
            mighty.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
            h.assertTrue(mighty.updateEffects(Optional.of(MobEffects.HASTE), Optional.empty()), "Haste has no II placeholder");
            h.assertTrue(synced.get(StrongBeaconMenu.DATA_PRIMARY_AMP) == 0 && MobEffects.HASTE.equals(mighty.getPrimaryEffect()), "the button amplifier is ignored where the power is only offered as I");
            h.succeed();
        });
        ALL.put("menu_carries_server_settings", h -> {
            // N58 (Simple Beacons: the client did not know the server's config): the numbers ride the menu data, so a
            // client whose own config differs still shows the server's limits.
            BeaconsConfig.reset();
            BeaconsConfig.strongBeaconMaxLevels = 5;
            BeaconsConfig.strongBeaconRangeBase = 20;
            BeaconsConfig.strongBeaconRangePerLevel = 3;
            BeaconsConfig.strongBeaconEffects.put("luck", false);
            h.setBlock(CENTER, BeaconsRegistry.STRONG_BEACON);
            net.minecraft.world.inventory.ContainerData server = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class).dataAccess();
            h.assertTrue(server.getCount() == StrongBeaconMenu.DATA_COUNT, "the beacon hands the menu " + StrongBeaconMenu.DATA_COUNT + " values");
            SimpleContainerData wire = new SimpleContainerData(StrongBeaconMenu.DATA_COUNT);
            for (int i = 0; i < server.getCount(); i++) wire.set(i, server.get(i));
            BeaconsConfig.reset(); // the client's own, different, config
            Player player = h.makeMockPlayer(GameType.SURVIVAL);
            StrongBeaconMenu client = new StrongBeaconMenu(0, player.getInventory(), wire, ContainerLevelAccess.NULL);
            h.assertTrue(client.maxLevels() == 5 && client.kind() == BeaconKind.STRONG, "max pyramid from the server: " + client.maxLevels());
            h.assertTrue(client.range(2) == 26, "range from the server's numbers: " + client.range(2));
            h.assertTrue(!client.effectEnabled(MobEffects.LUCK), "a switched-off effect is off for the client");
            h.assertTrue(client.effectEnabled(MobEffects.NIGHT_VISION) && client.effectEnabled(MobEffects.SPEED), "the others stay on");
            StrongBeaconMenu fresh = new StrongBeaconMenu(0, player.getInventory());
            h.assertTrue(fresh.maxLevels() >= 1 && fresh.kind() == BeaconKind.STRONG, "a client menu before the first sync does not break");
            BeaconsConfig.reset();
            h.setBlock(CENTER, BeaconsRegistry.MIGHTY_EVIL_BEACON);
            net.minecraft.world.inventory.ContainerData evil = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class).dataAccess();
            h.assertTrue(evil.get(StrongBeaconMenu.DATA_KIND) == BeaconKind.MIGHTY_EVIL.ordinal() && evil.get(StrongBeaconMenu.DATA_MAX_LEVELS) == 6, "kind and top level of the Mighty Evil Beacon");
            h.succeed();
        });
        ALL.put("beacon_kinds_and_names", h -> {
            Map<Block, BeaconKind> blocks = new LinkedHashMap<>();
            blocks.put(BeaconsRegistry.STRONG_BEACON, BeaconKind.STRONG);
            blocks.put(BeaconsRegistry.MIGHTY_BEACON, BeaconKind.MIGHTY);
            blocks.put(BeaconsRegistry.EVIL_BEACON, BeaconKind.EVIL);
            blocks.put(BeaconsRegistry.MIGHTY_EVIL_BEACON, BeaconKind.MIGHTY_EVIL);
            for (var e : blocks.entrySet()) {
                h.setBlock(CENTER, e.getKey());
                StrongBeaconBlockEntity be = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class);
                h.assertTrue(be.kind() == e.getValue() && BeaconKind.of(e.getKey()) == e.getValue(), "block entity kind " + e.getValue());
                h.assertTrue(be.getName().getString().contains("simplebeacons." + e.getValue().id) || be.getName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                        && tc.getKey().equals("container.simplebeacons." + e.getValue().id), "default name " + e.getValue());
                h.assertTrue(BuiltInRegistries.BLOCK.getKey(e.getKey()).getPath().equals(e.getValue().id), "block id = kind id");
            }
            h.assertTrue(BeaconKind.of(Blocks.STONE) == BeaconKind.STRONG, "foreign blocks count as Strong");
            h.succeed();
        });
        ALL.put("block_entity_keeps_choice", h -> {
            h.setBlock(CENTER, BeaconsRegistry.STRONG_BEACON);
            StrongBeaconBlockEntity be = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class);
            be.dataAccess().set(1, net.minecraft.world.inventory.BeaconMenu.encodeEffect(MobEffects.WATER_BREATHING));
            be.dataAccess().set(2, net.minecraft.world.inventory.BeaconMenu.encodeEffect(MobEffects.NAUSEA));
            h.assertTrue(be.primary() == null && be.secondary() == null, "effects of other kinds are filtered");
            be.dataAccess().set(1, net.minecraft.world.inventory.BeaconMenu.encodeEffect(MobEffects.LUCK));
            var tag = be.saveCustomOnly(h.getLevel().registryAccess());
            h.assertTrue("minecraft:luck".equals(tag.getStringOr("primary_effect", "")), "Vanilla key primary_effect");
            h.assertTrue(!tag.contains("primary_amp"), "no primary_amp for a plain power (old saves stay readable)");
            // Mighty: the placeholder's amplifier is saved and loaded
            h.setBlock(CENTER, BeaconsRegistry.MIGHTY_BEACON);
            StrongBeaconBlockEntity mighty = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class);
            mighty.dataAccess().set(1, net.minecraft.world.inventory.BeaconMenu.encodeEffect(MobEffects.SPEED));
            mighty.dataAccess().set(StrongBeaconMenu.DATA_PRIMARY_AMP, 1);
            var saved = mighty.saveCustomOnly(h.getLevel().registryAccess());
            h.assertTrue(saved.getIntOr("primary_amp", 0) == 1, "primary_amp saved");
            h.setBlock(CENTER, BeaconsRegistry.MIGHTY_BEACON);
            StrongBeaconBlockEntity again = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class);
            again.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
            h.assertTrue(MobEffects.SPEED.equals(again.primary()) && again.primaryAmp() == 1, "primary_amp loaded");
            // A Strong Beacon of an older world: no primary_amp, a 6 level pyramid, powers that no longer exist
            h.setBlock(CENTER, BeaconsRegistry.STRONG_BEACON);
            StrongBeaconBlockEntity old = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class);
            net.minecraft.nbt.CompoundTag legacy = new net.minecraft.nbt.CompoundTag();
            legacy.putString("primary_effect", "minecraft:speed");
            legacy.putString("secondary_effect", "minecraft:regeneration");
            legacy.putInt("Levels", 6);
            old.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), legacy));
            h.assertTrue(MobEffects.SPEED.equals(old.primary()) && MobEffects.REGENERATION.equals(old.secondary()) && old.primaryAmp() == 0, "an old save loads unchanged");
            h.assertTrue(old.levels() == 5, "a saved 6th level counts as 5 now: " + old.levels());
            net.minecraft.nbt.CompoundTag gone = new net.minecraft.nbt.CompoundTag();
            gone.putString("primary_effect", "minecraft:fire_resistance");
            gone.putString("secondary_effect", "minecraft:regeneration");
            old.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), gone));
            h.assertTrue(old.primary() == null && MobEffects.REGENERATION.equals(old.secondary()), "a power that no longer exists is cleared, nothing breaks");
            h.succeed();
        });
        ALL.put("range_cap_340", h -> {
            // REV2-A-1: the range never exceeds 340 blocks, whatever the settings
            BeaconsConfig.reset();
            JsonObject high = new JsonObject();
            high.addProperty("strongBeaconRangeBase", 50);
            high.addProperty("strongBeaconRangePerLevel", 20);
            BeaconsConfig.apply(high);
            h.assertTrue(StrongBeaconLogic.range(6) == 170 && StrongBeaconLogic.range(6, 200) == 340.0, "the highest settings with Netherite give exactly 340");
            BeaconsConfig.strongBeaconRangeBase = 100; // beyond the file's clamp: the cap still holds
            h.assertTrue(StrongBeaconLogic.range(6, 200) == 340.0 && StrongBeaconLogic.RANGE_CAP == 340, "hard cap 340");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("evil_upgrade_in_world", h -> {
            BeaconsConfig.reset();
            ItemStack evilTemplate = new ItemStack(BeaconsRegistry.EVIL_UPGRADE_TEMPLATE), strongTemplate = new ItemStack(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE);
            BlockState beacon = Blocks.BEACON.defaultBlockState(), conduit = Blocks.CONDUIT.defaultBlockState();
            h.assertTrue(StrongUpgrades.targetFor(beacon, evilTemplate) == BeaconsRegistry.EVIL_BEACON && StrongUpgrades.targetFor(beacon, strongTemplate) == BeaconsRegistry.STRONG_BEACON,
                    "Evil Upgrade makes an Evil Beacon, Strong Upgrade a Strong Beacon");
            h.assertTrue(StrongUpgrades.targetFor(conduit, evilTemplate) == null && StrongUpgrades.targetFor(conduit, strongTemplate) == BeaconsRegistry.STRONG_CONDUIT, "the Evil Upgrade does not touch conduits");
            h.setBlock(CENTER, Blocks.BEACON);
            BlockPos pos = h.absolutePos(CENTER);
            ServerPlayer player = upgrader(h, true);
            player.setItemInHand(InteractionHand.OFF_HAND, evilTemplate);
            h.assertTrue(StrongUpgrades.isStance(player), "the Evil Upgrade makes the stance");
            for (int i = 0; i < BeaconsConfig.upgradeStrikes; i++) {
                StrongUpgrades.strike(h.getLevel(), pos, player, InteractionHand.MAIN_HAND);
                InWorldStrikes.allowNextStrike(h.getLevel(), pos);
            }
            h.assertTrue(h.getBlockState(CENTER).is(BeaconsRegistry.EVIL_BEACON), "the last strike makes the Evil Beacon");
            h.succeed();
        });
        ALL.put("upgrade_in_world", h -> {
            BeaconsConfig.reset();
            h.setBlock(CENTER, Blocks.BEACON);
            BlockPos pos = h.absolutePos(CENTER);
            BeaconBlockEntity vanilla = h.getBlockEntity(CENTER, BeaconBlockEntity.class);
            vanilla.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
                    h.getLevel().registryAccess(), tagWith("minecraft:speed", "minecraft:regeneration")));
            vanilla.setCustomName(Component.literal("Home"));
            ServerPlayer player = upgrader(h, true);
            ItemStack tool = player.getMainHandItem();
            int toolDamage = tool.getDamageValue();
            int strikes = BeaconsConfig.upgradeStrikes;
            for (int i = 1; i < strikes; i++) {
                InteractionResult r = StrongUpgrades.strike(h.getLevel(), pos, player, InteractionHand.MAIN_HAND);
                h.assertTrue(r != null && r.consumesAction(), "strike " + i + " consumed");
                h.assertTrue(InWorldStrikes.done(h.getLevel(), pos) == i, "strike " + i + " counted");
                h.assertTrue(h.getBlockState(CENTER).is(Blocks.BEACON), "still Vanilla before the last strike");
                InWorldStrikes.allowNextStrike(h.getLevel(), pos);
            }
            StrongUpgrades.strike(h.getLevel(), pos, player, InteractionHand.MAIN_HAND);
            h.assertTrue(h.getBlockState(CENTER).is(BeaconsRegistry.STRONG_BEACON), "the last strike swaps in place");
            StrongBeaconBlockEntity strong = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class);
            h.assertTrue(MobEffects.SPEED.equals(strong.primary()) && MobEffects.REGENERATION.equals(strong.secondary()), "choice kept");
            h.assertTrue(strong.getCustomName() != null && "Home".equals(strong.getCustomName().getString()), "name kept");
            h.assertTrue(player.getOffhandItem().isEmpty() && StrongUpgrades.starSlot(player) < 0, "template and nether star used up");
            h.assertTrue(tool.isEmpty() || tool.getDamageValue() > toolDamage, "the tool wears");
            h.succeed();
        });
        ALL.put("upgrade_needs_star_and_stance", h -> {
            BeaconsConfig.reset();
            h.setBlock(CENTER, Blocks.CONDUIT);
            BlockPos pos = h.absolutePos(CENTER);
            ServerPlayer player = upgrader(h, false);
            h.assertTrue(StrongUpgrades.strike(h.getLevel(), pos, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "no star: refused");
            h.assertTrue(InWorldStrikes.done(h.getLevel(), pos) == 0, "nothing counted");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            h.assertTrue(StrongUpgrades.strike(h.getLevel(), pos, player, InteractionHand.MAIN_HAND) == null, "without template: Vanilla use");
            h.setBlock(CENTER, Blocks.STONE);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE));
            h.assertTrue(StrongUpgrades.strike(h.getLevel(), pos, player, InteractionHand.MAIN_HAND) == null, "other blocks: Vanilla use");
            h.succeed();
        });
        ALL.put("conduit_upgrade_keeps_water", h -> {
            BeaconsConfig.reset();
            h.setBlock(CENTER, Blocks.CONDUIT.defaultBlockState().setValue(net.minecraft.world.level.block.ConduitBlock.WATERLOGGED, true));
            BlockPos pos = h.absolutePos(CENTER);
            StrongUpgrades.upgrade(h.getLevel(), pos, BeaconsRegistry.STRONG_CONDUIT);
            BlockState state = h.getBlockState(CENTER);
            h.assertTrue(state.is(BeaconsRegistry.STRONG_CONDUIT) && state.getValue(com.simplebeacons.conduit.StrongConduitBlock.WATERLOGGED),
                    "strong conduit, still waterlogged");
            h.succeed();
        });
        ALL.put("recipes", h -> {
            var server = h.getLevel().getServer();
            ItemStack beacon = smith(h, new ItemStack(Items.BEACON));
            h.assertTrue(beacon.is(BeaconsRegistry.STRONG_BEACON_ITEM), "smithing: beacon + template + nether star");
            h.assertTrue(smith(h, new ItemStack(Items.CONDUIT)).is(BeaconsRegistry.STRONG_CONDUIT_ITEM), "smithing: conduit");
            ItemStack o = new ItemStack(Items.CRYING_OBSIDIAN), d = new ItemStack(Items.DIAMOND), n = new ItemStack(Items.NETHER_STAR);
            // Owner 21.10.: not craftable from base materials - the Nether Star way and the Basic Upgrade way are gone
            h.assertTrue(craft(h, List.of(o, d, o, d, n, d, o, d, o)).isEmpty(), "no template from a Nether Star");
            h.assertTrue(server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    SimpleBeacons.id("strong_upgrade_template_from_basic"))).isEmpty(), "no Basic Upgrade way");
            // nine shards (the Vanilla Wither drop) make one template
            ItemStack s = new ItemStack(BeaconsRegistry.STRONG_UPGRADE_SHARD);
            ItemStack fromShards = craft(h, List.of(s, s, s, s, s, s, s, s, s));
            h.assertTrue(fromShards.is(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE) && fromShards.getCount() == 1, "9 shards -> 1 template");
            h.assertTrue(craft(h, List.of(s, s, s, s, s, s, s, s, ItemStack.EMPTY)).isEmpty(), "8 shards are not enough");
            // with Simple Mobs: the Nether Heart takes the Nether Star's place (image 18)
            var heart = BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplemobs", "nether_heart"));
            if (heart.isPresent()) {
                ItemStack hs = new ItemStack(heart.get());
                h.assertTrue(craft(h, List.of(o, d, o, d, hs, d, o, d, o)).is(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE), "Nether Heart in the middle -> template");
            } else {
                h.assertTrue(server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                        SimpleBeacons.id("strong_upgrade_template"))).isEmpty(), "[standalone] no Nether Heart recipe without Simple Mobs");
            }
            ItemStack t = new ItemStack(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE);
            // W23-8: duplication only with a Nether Heart (template + crying obsidian + diamond + heart -> 2); the old diamond ring is gone
            CraftingInput old = CraftingInput.of(3, 3, List.of(d, t, d, d, o, d, d, d, d));
            h.assertTrue(server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, old, h.getLevel()).isEmpty(), "no diamond-ring duplication");
            if (heart.isPresent()) {
                CraftingInput dup = CraftingInput.of(2, 2, List.of(t, o, d, new ItemStack(heart.get())));
                ItemStack two = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, dup, h.getLevel())
                        .map(r -> r.value().assemble(dup)).orElse(ItemStack.EMPTY);
                h.assertTrue(two.is(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE) && two.getCount() == 2, "duplication with Nether Heart gives 2");
            } else {
                h.assertTrue(server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                        SimpleBeacons.id("strong_upgrade_template_duplication"))).isEmpty(), "[standalone] no duplication without Simple Mobs");
            }
            // BS28b-4: Mighty Beacon = Strong Beacon in the middle, the six cores around, a Strong Upgrade in each bottom corner
            boolean cores = BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "copper_core"));
            List<ItemStack> six = new ArrayList<>();
            for (String name : List.of("copper", "iron", "gold", "diamond", "netherite", "enderite")) {
                six.add(cores ? new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", name + "_core")))
                        : new ItemStack(switch (name) {
                            case "copper" -> Items.COPPER_BLOCK;
                            case "iron" -> Items.IRON_BLOCK;
                            case "gold" -> Items.GOLD_BLOCK;
                            case "diamond" -> Items.DIAMOND_BLOCK;
                            case "netherite" -> Items.NETHERITE_BLOCK;
                            default -> Items.EMERALD_BLOCK;
                        }));
            }
            ItemStack strongBeacon = new ItemStack(BeaconsRegistry.STRONG_BEACON_ITEM), evilBeacon = new ItemStack(BeaconsRegistry.EVIL_BEACON_ITEM);
            List<ItemStack> mightyGrid = List.of(six.get(0), six.get(1), six.get(2), six.get(3), strongBeacon, six.get(4), t, six.get(5), t);
            h.assertTrue(craft(h, mightyGrid).is(BeaconsRegistry.MIGHTY_BEACON_ITEM), "Mighty Beacon: Strong Beacon, six cores, two Strong Upgrades (cores: " + cores + ")");
            h.assertTrue(craft(h, List.of(six.get(1), six.get(0), six.get(2), six.get(3), strongBeacon, six.get(4), t, six.get(5), t)).isEmpty(), "the cores sit at fixed places");
            h.assertTrue(craft(h, List.of(six.get(0), six.get(1), six.get(2), six.get(3), new ItemStack(Items.BEACON), six.get(4), t, six.get(5), t)).isEmpty(), "a Vanilla beacon is not enough");
            h.assertTrue(craft(h, List.of(six.get(0), six.get(1), six.get(2), six.get(3), strongBeacon, six.get(4), t, six.get(5), ItemStack.EMPTY)).isEmpty(), "both Strong Upgrades are needed");
            h.assertTrue(craft(h, List.of(six.get(0), six.get(1), six.get(2), six.get(3), evilBeacon, six.get(4), t, six.get(5), t)).is(BeaconsRegistry.MIGHTY_EVIL_BEACON_ITEM),
                    "Mighty Evil Beacon: the same with the Evil Beacon in the middle");
            // BS28b-5: Evil Beacon = beacon + Evil Upgrade + nether star; the template itself needs Simple Mobs
            SmithingRecipeInput evilSmith = new SmithingRecipeInput(new ItemStack(BeaconsRegistry.EVIL_UPGRADE_TEMPLATE), new ItemStack(Items.BEACON), new ItemStack(Items.NETHER_STAR));
            h.assertTrue(server.getRecipeManager().getRecipeFor(RecipeType.SMITHING, evilSmith, h.getLevel()).map(r -> r.value().assemble(evilSmith)).orElse(ItemStack.EMPTY)
                    .is(BeaconsRegistry.EVIL_BEACON_ITEM), "smithing: beacon + Evil Upgrade + nether star");
            var gem = BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplemobs", "nether_gem"));
            if (heart.isPresent() && gem.isPresent()) {
                CraftingInput evil = CraftingInput.of(2, 2, List.of(t, o, new ItemStack(heart.get()), new ItemStack(gem.get())));
                ItemStack made = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, evil, h.getLevel()).map(r -> r.value().assemble(evil)).orElse(ItemStack.EMPTY);
                h.assertTrue(made.is(BeaconsRegistry.EVIL_UPGRADE_TEMPLATE), "Evil Upgrade: Strong Upgrade + crying obsidian + Nether Heart + Red Nether Jewel");
            } else {
                h.assertTrue(server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                        SimpleBeacons.id("evil_upgrade_template"))).isEmpty(), "[standalone] no Evil Upgrade recipe without Simple Mobs");
            }
            h.succeed();
        });
        ALL.put("strong_upgrade_shard", h -> {
            var level = h.getLevel();
            h.getEntities(net.minecraft.world.entity.EntityTypes.ITEM).forEach(e -> e.discard());
            // the Vanilla Wither always drops exactly one shard (next to its Nether Star)
            net.minecraft.world.entity.boss.wither.WitherBoss wither = h.spawn(net.minecraft.world.entity.EntityTypes.WITHER, 7, 2, 7);
            wither.setNoAi(true);
            wither.kill(level);
            var items = h.getEntities(net.minecraft.world.entity.EntityTypes.ITEM);
            int shards = items.stream().filter(i -> i.getItem().is(BeaconsRegistry.STRONG_UPGRADE_SHARD)).mapToInt(i -> i.getItem().getCount()).sum();
            h.assertTrue(shards == 1, "Vanilla Wither: one Strong Upgrade Shard: " + shards);
            h.assertTrue(items.stream().anyMatch(i -> i.getItem().is(Items.NETHER_STAR)), "and its Nether Star");
            items.forEach(e -> e.discard());
            h.getEntities(net.minecraft.world.entity.EntityTypes.EXPERIENCE_ORB).forEach(e -> e.discard());
            // the shard survives an explosion that destroys an ordinary item next to it
            Vec3 at = Vec3.atCenterOf(h.absolutePos(new BlockPos(7, 2, 7)));
            var shard = new net.minecraft.world.entity.item.ItemEntity(level, at.x, at.y, at.z, new ItemStack(BeaconsRegistry.STRONG_UPGRADE_SHARD));
            var dirt = new net.minecraft.world.entity.item.ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.DIRT));
            level.addFreshEntity(shard);
            level.addFreshEntity(dirt);
            level.explode(null, at.x, at.y, at.z, 4.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            h.assertTrue(dirt.isRemoved(), "the explosion destroys an ordinary item");
            h.assertTrue(!shard.isRemoved() && shard.getItem().is(BeaconsRegistry.STRONG_UPGRADE_SHARD), "the shard is explosion proof");
            shard.discard();
            h.succeed();
        });
        ALL.put("conduit_frames", h -> {
            BeaconsConfig.reset();
            water(h);
            h.setBlock(CENTER, BeaconsRegistry.STRONG_CONDUIT);
            BlockPos center = h.absolutePos(CENTER);
            h.assertTrue(StrongConduitLogic.shell(center, 2).size() == 42 && StrongConduitLogic.shell(center, 3).size() == 66, "shell sizes 42/66");
            List<BlockPos> found = new ArrayList<>();
            h.assertTrue(StrongConduitLogic.scan(h.getLevel(), center, found) == 0, "no frame: inactive");
            for (BlockPos p : StrongConduitLogic.shell(center, 2)) h.getLevel().setBlockAndUpdate(p, Blocks.PRISMARINE.defaultBlockState());
            h.assertTrue(StrongConduitLogic.scan(h.getLevel(), center, found) == 2 && found.size() == 42, "full 5x5x5 frame");
            h.assertTrue(StrongConduitLogic.range(42) == 144 && StrongConduitLogic.killRange(42, 2) == 8, "SP22-9: 1.5 x Vanilla range (144), Vanilla attack");
            for (BlockPos p : StrongConduitLogic.shell(center, 3)) h.getLevel().setBlockAndUpdate(p, Blocks.DARK_PRISMARINE.defaultBlockState());
            h.assertTrue(StrongConduitLogic.scan(h.getLevel(), center, found) == 3 && found.size() == 66, "full 7x7x7 frame wins");
            h.assertTrue(StrongConduitLogic.range(66) == 216 && StrongConduitLogic.killRange(66, 3) == 12, "SP22-9: range 216, attack 12");
            h.assertTrue(StrongConduitLogic.range(16) == 48, "16 frame blocks: 2 x 24");
            JsonObject json = new JsonObject();
            json.addProperty("strongConduitMaxRange", 96);
            BeaconsConfig.apply(json);
            h.assertTrue(StrongConduitLogic.range(66) == 96, "server cap");
            BeaconsConfig.reset();
            StrongConduitBlockEntity be = h.getBlockEntity(CENTER, StrongConduitBlockEntity.class);
            be.refresh(h.getLevel(), center);
            h.assertTrue(be.isActive() && be.frameBlocks() == 66 && be.isHunting(), "block entity active and hunting");
            h.succeed();
        });
        ALL.put("conduit_effects", h -> {
            BeaconsConfig.reset();
            water(h);
            BlockPos center = h.absolutePos(CENTER);
            ServerPlayer swimmer = h.makeMockServerPlayerInLevel();
            ServerPlayer dry = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> {
                h.getLevel().getServer().getPlayerList().remove(swimmer);
                h.getLevel().getServer().getPlayerList().remove(dry);
            });
            swimmer.snapTo(center.getX() + 0.5, center.getY() - 1.0, center.getZ() + 0.5);
            dry.snapTo(center.getX() + 20.5, center.getY() + 30.0, center.getZ() + 0.5);
            swimmer.baseTick();
            dry.baseTick();
            List<Player> reached = StrongConduitLogic.applyEffects(h.getLevel(), center, 66);
            h.assertTrue(swimmer.isInWater() && reached.contains(swimmer) && !reached.contains(dry), "only players in the water");
            h.assertTrue(swimmer.hasEffect(MobEffects.CONDUIT_POWER) && swimmer.hasEffect(MobEffects.DOLPHINS_GRACE), "conduit power + dolphin's grace");
            h.assertTrue(!dry.hasEffect(MobEffects.CONDUIT_POWER), "dry player gets nothing");
            h.succeed();
        });
        ALL.put("range_factor_by_material", h -> {
            // SP22-10 (plan docs/ai/PLAN-A-BEACONS22-2026-10-22.md): average block factor of the counted levels, whole percent
            BeaconsConfig.reset();
            h.assertTrue(RangeFactor.percent(Blocks.NETHERITE_BLOCK) == 200 && RangeFactor.percent(Blocks.DIAMOND_BLOCK) == 150
                    && RangeFactor.percent(Blocks.GOLD_BLOCK) == 110 && RangeFactor.percent(Blocks.EMERALD_BLOCK) == 110
                    && RangeFactor.percent(Blocks.IRON_BLOCK) == 100, "block factors 2 / 1.5 / 1.1 / 1.1 / 1");
            BlockPos beacon = h.absolutePos(CENTER);
            pyramid(h, 6, Blocks.IRON_BLOCK);
            h.assertTrue(RangeFactor.percent(h.getLevel(), beacon, 4) == 100, "Iron: x1.00 (Vanilla range)");
            layer(h, 4, Blocks.DIAMOND_BLOCK);
            h.assertTrue(RangeFactor.percent(h.getLevel(), beacon, 4) == 125, "owner's example: bottom of 4 levels Diamond = x1.25");
            h.assertTrue(RangeFactor.percent(h.getLevel(), beacon, 3) == 100, "a level below the counted ones does not count");
            layer(h, 4, Blocks.IRON_BLOCK);
            layer(h, 6, Blocks.DIAMOND_BLOCK);
            h.assertTrue(RangeFactor.percent(h.getLevel(), beacon, 6) == 119, "Strong 6 levels, bottom Diamond: 118.6 -> 119");
            pyramid(h, 6, Blocks.NETHERITE_BLOCK);
            h.assertTrue(RangeFactor.percent(h.getLevel(), beacon, 6) == 200 && StrongBeaconLogic.range(6, 200) == 140.0, "all Netherite: x2, 140 blocks");
            h.assertTrue("1.25".equals(RangeFactor.format(125)) && "2.00".equals(RangeFactor.format(200)) && "1.10".equals(RangeFactor.format(110)), "format");
            JsonObject off = new JsonObject();
            off.addProperty("strongBeaconMaterialRange", false);
            off.addProperty("vanillaBeaconMaterialRange", false);
            BeaconsConfig.apply(off);
            h.assertTrue(RangeFactor.strongPercent(h.getLevel(), beacon, 6) == 100 && RangeFactor.vanillaPercent(h.getLevel(), beacon, 4) == 100,
                    "switches off: x1");
            BeaconsConfig.reset();
            h.assertTrue(RangeFactor.vanillaPercent(h.getLevel(), beacon, 4) == 200 && RangeFactor.percent(h.getLevel(), beacon, 0) == 100, "on again; no pyramid x1");
            h.succeed();
        });
        ALL.put("vanilla_beacon_material_range", h -> {
            // the Vanilla beacon's own applyEffects (BeaconRangeMixin): level 1 range 20, with a Netherite pyramid 40
            BeaconsConfig.reset();
            pyramid(h, 1, Blocks.NETHERITE_BLOCK);
            BlockPos beacon = h.absolutePos(CENTER);
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
            player.snapTo(beacon.getX() + 24.5, beacon.getY(), beacon.getZ() + 0.5);
            com.simplebeacons.mixin.BeaconInvoker.simplebeacons$applyEffects(h.getLevel(), beacon, 1, MobEffects.SPEED, null);
            h.assertTrue(player.hasEffect(MobEffects.SPEED), "24 blocks away: reached with x2 (40 blocks)");
            player.removeAllEffects();
            JsonObject off = new JsonObject();
            off.addProperty("vanillaBeaconMaterialRange", false);
            BeaconsConfig.apply(off);
            com.simplebeacons.mixin.BeaconInvoker.simplebeacons$applyEffects(h.getLevel(), beacon, 1, MobEffects.SPEED, null);
            h.assertTrue(!player.hasEffect(MobEffects.SPEED), "switch off: Vanilla's 20 blocks again");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("range_factor_in_menu", h -> {
            // the factor rides an extra data slot of every beacon menu, read from the open beacon
            BeaconsConfig.reset();
            pyramid(h, 1, Blocks.DIAMOND_BLOCK);
            h.setBlock(CENTER, BeaconsRegistry.STRONG_BEACON);
            StrongBeaconBlockEntity be = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class);
            be.setLevelsForTest(1);
            h.assertTrue(be.rangePercent() == 150, "Strong beacon on Diamond: 150 %");
            Player player = h.makeMockPlayer(GameType.SURVIVAL);
            ContainerLevelAccess access = ContainerLevelAccess.create(h.getLevel(), h.absolutePos(CENTER));
            StrongBeaconMenu strong = new StrongBeaconMenu(0, player.getInventory(), be.dataAccess(), access);
            h.assertTrue(((RangeFactorView) strong).simplebeacons$rangePercent() == 150, "Strong menu carries 150");
            h.assertTrue(((RangeFactorView) new StrongBeaconMenu(0, player.getInventory())).simplebeacons$rangePercent() == 0,
                    "client menu before the sync: 0 = nothing shown");
            h.setBlock(CENTER, Blocks.BEACON);
            SimpleContainerData vanillaData = new SimpleContainerData(3);
            vanillaData.set(0, 1);
            net.minecraft.world.inventory.BeaconMenu vanilla = new net.minecraft.world.inventory.BeaconMenu(0, player.getInventory(), vanillaData, access);
            h.assertTrue(((RangeFactorView) vanilla).simplebeacons$rangePercent() == 150, "Vanilla menu carries 150");
            BeaconsConfig.vanillaBeaconMaterialRange = false;
            net.minecraft.world.inventory.BeaconMenu off = new net.minecraft.world.inventory.BeaconMenu(0, player.getInventory(), vanillaData, access);
            h.assertTrue(((RangeFactorView) off).simplebeacons$rangePercent() == 0, "switch off: 0 = the Vanilla screen shows nothing");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("vanilla_conduit_only_conduit_power", h -> {
            // SP22-9: the normal conduit stays Vanilla - Conduit Power (water breathing) only, no Dolphin's Grace
            BeaconsConfig.reset();
            water(h);
            h.setBlock(CENTER, Blocks.CONDUIT.defaultBlockState().setValue(net.minecraft.world.level.block.ConduitBlock.WATERLOGGED, true));
            BlockPos center = h.absolutePos(CENTER);
            for (BlockPos p : StrongConduitLogic.shell(center, 2)) h.getLevel().setBlockAndUpdate(p, Blocks.PRISMARINE.defaultBlockState());
            ServerPlayer swimmer = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(swimmer));
            swimmer.snapTo(center.getX() + 0.5, center.getY() - 1.0, center.getZ() + 0.5);
            swimmer.baseTick();
            List<BlockPos> frame = new ArrayList<>();
            h.assertTrue(com.simplebeacons.mixin.ConduitInvoker.simplebeacons$updateShape(h.getLevel(), center, frame) && frame.size() == 42,
                    "Vanilla conduit active with the 5x5x5 frame");
            swimmer.removeAllEffects();
            com.simplebeacons.mixin.ConduitInvoker.simplebeacons$applyEffects(h.getLevel(), center, frame);
            h.assertTrue(swimmer.isInWater() && swimmer.hasEffect(MobEffects.CONDUIT_POWER), "Vanilla conduit: Conduit Power");
            h.assertTrue(!swimmer.hasEffect(MobEffects.DOLPHINS_GRACE), "Vanilla conduit: no Dolphin's Grace");
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleBeacons.MOD_ID);
            try {
                com.simplebuilding.framework.api.SubMods.set(SimpleBeacons.MOD_ID, false);
                h.assertTrue(!SimpleBeacons.on() && BeaconsRegistry.tabStacks().isEmpty(), "switched off: inert, no tab entries");
                h.setBlock(CENTER, Blocks.BEACON);
                ServerPlayer player = upgrader(h, true);
                h.assertTrue(StrongUpgrades.strike(h.getLevel(), h.absolutePos(CENTER), player, InteractionHand.MAIN_HAND) == null, "no upgrade");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleBeacons.MOD_ID, before);
            }
            h.assertTrue(SimpleBeacons.on(), "back on");
            h.succeed();
        });
        ALL.put("upgrade_tool_follows_the_partner", h -> {
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            h.assertTrue(SimpleLibApi.isUpgradeTool(axe) == SimpleLibApi.axeWaysEnabled(), "axe only while the axe ways are on");
            h.assertTrue(SimpleLibApi.isUpgradeTool(tool()), "the current upgrade tool works");
            h.succeed();
        });
        ALL.put("guide_book", com.simplebeacons.guide.BeaconsGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simplebeacons.discover.BeaconsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplebeacons.discover.BeaconsDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simplebeacons.discover.BeaconsDiscover::firstPickupTips);
        MAX_TICKS.put("pyramid_levels", 200);
        MAX_TICKS.put("range_factor_by_material", 200);
    }

    /** The result of crafting a 3 x 3 grid, or empty. */
    static ItemStack craft(GameTestHelper h, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    /** Pyramid of {@code levels} levels below {@link #CENTER}. */
    static void pyramid(GameTestHelper h, int levels, Block block) {
        for (int step = 1; step <= levels; step++) {
            for (int dx = -step; dx <= step; dx++) {
                for (int dz = -step; dz <= step; dz++) {
                    h.setBlock(CENTER.offset(dx, -step, dz), block);
                }
            }
        }
    }

    /** Pyramid level {@code step} (one layer) below {@link #CENTER} in {@code block}. */
    static void layer(GameTestHelper h, int step, Block block) {
        for (int dx = -step; dx <= step; dx++) {
            for (int dz = -step; dz <= step; dz++) {
                h.setBlock(CENTER.offset(dx, -step, dz), block);
            }
        }
    }

    /** Water 3x3x3 around {@link #CENTER} (the conduit's own spot included). */
    static void water(GameTestHelper h) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    h.setBlock(CENTER.offset(dx, dy, dz), Blocks.WATER);
                }
            }
        }
    }

    /** The tool of the current mode: an axe standalone, a hammer once a hammer mod switched the axe ways off. */
    static ItemStack tool() {
        if (SimpleLibApi.axeWaysEnabled()) return new ItemStack(Items.IRON_AXE);
        for (Holder<Item> hammer : BuiltInRegistries.ITEM.getTagOrEmpty(InWorldUpgrades.HAMMERS)) return new ItemStack(hammer);
        return new ItemStack(Items.IRON_AXE);
    }

    /** A survival player with the upgrade stance (tool, template in the off hand) and optionally a nether star. */
    static ServerPlayer upgrader(GameTestHelper h, boolean star) {
        Player raw = h.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) throw h.assertionException("makeMockServerPlayer returned " + raw.getClass().getName());
        Vec3 at = h.absoluteVec(new Vec3(7.5, 8.0, 5.5));
        player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool());
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE));
        if (star) player.getInventory().add(new ItemStack(Items.NETHER_STAR));
        return player;
    }

    static net.minecraft.nbt.CompoundTag tagWith(String primary, String secondary) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("primary_effect", primary);
        tag.putString("secondary_effect", secondary);
        return tag;
    }

    static ItemStack smith(GameTestHelper h, ItemStack base) {
        SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE), base, new ItemStack(Items.NETHER_STAR));
        return h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, h.getLevel())
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private BeaconsTests() {}
}
