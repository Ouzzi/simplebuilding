package com.simplebeacons.test;

import com.google.gson.JsonObject;
import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.SimpleBeacons;
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
        ALL.put("config_defaults", h -> {
            BeaconsConfig.reset();
            h.assertTrue(BeaconsConfig.enabled && BeaconsConfig.strongBeaconMaxLevels == 6 && BeaconsConfig.strongBeaconRangeBase == 10
                    && BeaconsConfig.strongBeaconRangePerLevel == 10 && BeaconsConfig.strongConduitMaxRange == 144
                    && BeaconsConfig.upgradeStrikes == 8, "defaults");
            h.assertTrue(BeaconsConfig.EFFECT_IDS.stream().allMatch(BeaconsConfig::effectOn), "all six effects on by default");
            JsonObject json = new JsonObject();
            json.addProperty("strongBeaconMaxLevels", 9);
            json.addProperty("strongConduitMaxRange", 20);
            json.addProperty("upgradeStrikes", "many");
            JsonObject effects = new JsonObject();
            effects.addProperty("absorption", false);
            json.add("strongBeaconEffects", effects);
            BeaconsConfig.apply(json);
            h.assertTrue(BeaconsConfig.strongBeaconMaxLevels == 6 && BeaconsConfig.strongConduitMaxRange == 96, "values are clamped");
            h.assertTrue(BeaconsConfig.upgradeStrikes == 8, "junk falls back to the default");
            h.assertTrue(!BeaconsConfig.effectOn("absorption") && BeaconsConfig.effectOn("night_vision"), "per effect switch");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("registered", h -> {
            for (String id : List.of("strong_upgrade_template", "strong_beacon", "strong_conduit", "guide_book")) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleBeacons.id(id)), "item missing: " + id);
            }
            h.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleBeacons.id("strong_beacon")) == BeaconsRegistry.STRONG_BEACON, "block");
            h.assertTrue(BuiltInRegistries.MENU.containsKey(SimpleBeacons.id("strong_beacon")), "menu type");
            h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleBeacons.id("strong_conduit")), "conduit block entity type");
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
            BeaconsConfig.reset();
            h.assertTrue(!StrongBeaconLogic.validate(MobEffects.NIGHT_VISION, null, 4) && StrongBeaconLogic.validate(MobEffects.NIGHT_VISION, null, 5),
                    "night vision from level 5");
            h.assertTrue(StrongBeaconLogic.validate(MobEffects.SLOW_FALLING, MobEffects.REGENERATION, 5), "level 5 effect + regeneration");
            h.assertTrue(!StrongBeaconLogic.validate(MobEffects.FIRE_RESISTANCE, null, 5) && StrongBeaconLogic.validate(MobEffects.FIRE_RESISTANCE, null, 6),
                    "fire resistance from level 6");
            h.assertTrue(StrongBeaconLogic.validate(MobEffects.SPEED, MobEffects.SPEED, 6), "Speed II stays");
            h.assertTrue(!StrongBeaconLogic.validate(MobEffects.ABSORPTION, MobEffects.ABSORPTION, 6), "no Absorption II");
            h.assertTrue(!StrongBeaconLogic.validate(MobEffects.REGENERATION, null, 6), "regeneration is no primary");
            h.assertTrue(!StrongBeaconLogic.validate(MobEffects.LUCK, null, 6) && !StrongBeaconLogic.validate(MobEffects.SATURATION, null, 6),
                    "foreign effects refused");
            JsonObject json = new JsonObject();
            json.addProperty("strongBeaconMaxLevels", 5);
            JsonObject effects = new JsonObject();
            effects.addProperty("night_vision", false);
            json.add("strongBeaconEffects", effects);
            BeaconsConfig.apply(json);
            h.assertTrue(!StrongBeaconLogic.validate(MobEffects.NIGHT_VISION, null, 6), "switched off effect refused");
            h.assertTrue(!StrongBeaconLogic.validate(MobEffects.DOLPHINS_GRACE, null, 6), "max levels 5: no level 6 effect");
            BeaconsConfig.reset();
            h.succeed();
        });
        ALL.put("effects_reach_players", h -> {
            BeaconsConfig.reset();
            ServerPlayer near = h.makeMockServerPlayerInLevel();
            ServerPlayer far = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> {
                h.getLevel().getServer().getPlayerList().remove(near);
                h.getLevel().getServer().getPlayerList().remove(far);
            });
            BlockPos beacon = h.absolutePos(CENTER);
            near.snapTo(beacon.getX() + 65.5, beacon.getY(), beacon.getZ() + 0.5);
            far.snapTo(beacon.getX() + 75.5, beacon.getY(), beacon.getZ() + 0.5);
            List<Player> reached = StrongBeaconLogic.applyEffects(h.getLevel(), beacon, 6, MobEffects.FIRE_RESISTANCE, MobEffects.REGENERATION);
            h.assertTrue(reached.contains(near) && !reached.contains(far), "level 6 reaches 70 blocks, not 75");
            MobEffectInstance fire = near.getEffect(MobEffects.FIRE_RESISTANCE);
            h.assertTrue(fire != null && fire.getAmplifier() == 0 && fire.getDuration() == 21 * 20 && fire.isAmbient(), "fire resistance I, 21 s, ambient");
            h.assertTrue(near.hasEffect(MobEffects.REGENERATION) && !far.hasEffect(MobEffects.FIRE_RESISTANCE), "secondary too, nothing far away");
            near.removeAllEffects();
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, 5, MobEffects.FIRE_RESISTANCE, null);
            h.assertTrue(!near.hasEffect(MobEffects.FIRE_RESISTANCE), "a level 6 effect does nothing at level 5");
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, 6, MobEffects.ABSORPTION, null);
            h.assertTrue(near.getAbsorptionAmount() == 4.0F, "absorption I = 2 hearts");
            near.setAbsorptionAmount(1.0F);
            StrongBeaconLogic.applyEffects(h.getLevel(), beacon, 6, MobEffects.ABSORPTION, null);
            h.assertTrue(near.getAbsorptionAmount() == 1.0F, "refreshing the duration does not refill absorption");
            h.succeed();
        });
        ALL.put("menu_checks_the_choice", h -> {
            BeaconsConfig.reset();
            Player player = h.makeMockPlayer(GameType.SURVIVAL);
            SimpleContainerData data = new SimpleContainerData(3);
            data.set(0, 5);
            StrongBeaconMenu menu = new StrongBeaconMenu(0, player.getInventory(), data, ContainerLevelAccess.NULL);
            h.assertTrue(menu.getType() == BeaconsRegistry.STRONG_BEACON_MENU, "own menu type");
            menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
            h.assertTrue(menu.updateEffects(Optional.of(MobEffects.FIRE_RESISTANCE), Optional.empty()), "refused without kicking");
            h.assertTrue(menu.getPrimaryEffect() == null && menu.hasPayment(), "a level 6 effect at level 5: nothing set, payment kept");
            h.assertTrue(menu.updateEffects(Optional.of(MobEffects.NIGHT_VISION), Optional.of(MobEffects.REGENERATION)), "valid choice");
            h.assertTrue(MobEffects.NIGHT_VISION.equals(menu.getPrimaryEffect()) && MobEffects.REGENERATION.equals(menu.getSecondaryEffect())
                    && !menu.hasPayment(), "choice set, payment taken");
            h.succeed();
        });
        ALL.put("block_entity_keeps_choice", h -> {
            h.setBlock(CENTER, BeaconsRegistry.STRONG_BEACON);
            StrongBeaconBlockEntity be = h.getBlockEntity(CENTER, StrongBeaconBlockEntity.class);
            be.dataAccess().set(1, net.minecraft.world.inventory.BeaconMenu.encodeEffect(MobEffects.WATER_BREATHING));
            be.dataAccess().set(2, net.minecraft.world.inventory.BeaconMenu.encodeEffect(MobEffects.LUCK));
            h.assertTrue(MobEffects.WATER_BREATHING.equals(be.primary()) && be.secondary() == null, "unknown effects are filtered");
            var tag = be.saveCustomOnly(h.getLevel().registryAccess());
            h.assertTrue("minecraft:water_breathing".equals(tag.getStringOr("primary_effect", "")), "Vanilla key primary_effect");
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
            ItemStack o = new ItemStack(Items.OBSIDIAN), d = new ItemStack(Items.DIAMOND), n = new ItemStack(Items.NETHER_STAR);
            CraftingInput first = CraftingInput.of(3, 3, List.of(o, d, o, d, n, d, o, d, o));
            h.assertTrue(server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, first, h.getLevel())
                    .map(r -> r.value().assemble(first)).orElse(ItemStack.EMPTY).is(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE), "first template");
            ItemStack t = new ItemStack(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE);
            CraftingInput dup = CraftingInput.of(3, 3, List.of(d, t, d, d, o, d, d, d, d));
            ItemStack two = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, dup, h.getLevel())
                    .map(r -> r.value().assemble(dup)).orElse(ItemStack.EMPTY);
            h.assertTrue(two.is(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE) && two.getCount() == 2, "duplication gives 2");
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
            h.assertTrue(StrongConduitLogic.range(42) == 96 && StrongConduitLogic.killRange(42, 2) == 8, "Vanilla range and attack");
            for (BlockPos p : StrongConduitLogic.shell(center, 3)) h.getLevel().setBlockAndUpdate(p, Blocks.DARK_PRISMARINE.defaultBlockState());
            h.assertTrue(StrongConduitLogic.scan(h.getLevel(), center, found) == 3 && found.size() == 66, "full 7x7x7 frame wins");
            h.assertTrue(StrongConduitLogic.range(66) == 144 && StrongConduitLogic.killRange(66, 3) == 12, "range 144, attack 12");
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
