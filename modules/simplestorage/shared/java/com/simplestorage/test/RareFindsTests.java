package com.simplestorage.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplelib.api.IdMoves;
import com.simplestorage.SimpleStorage;
import com.simplestorage.chest.ChestTier;
import com.simplestorage.config.StorageConfig;
import com.simplestorage.rare.BetterChests;
import com.simplestorage.rare.RareShulkers;
import com.simplestorage.rare.ShulkerShells;
import com.simplestorage.registry.StorageItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rare structure finds (owner 2026-10-02; split stage 8b-3, bodies from SimpleBuilding's {@code RareStructureFindsTests}): better
 * loot chests in stronghold, bastion/nether fortress and end city/end ship, rare reinforced/Netherite/Enderite shulkers with
 * their escort, the tier shells and their recipes, the config limits, the migration of old {@code simplebuilding:} worlds.
 *
 * <p>The numbers are this file's own constants (1 %, 1.5x/3x health, 0-2 shells, one nugget per step), not read from the module.
 * Not here: a real stronghold or end city generation (the mixins on {@code StructurePiece#createChest} and
 * {@code EndCityPiece#handleDataMarker} call {@link BetterChests#upgradePlaced} and {@link RareShulkers#onEndCitySpawn}, which run
 * directly here); the bastion's template route runs for real through {@link StructureTemplate#processBlockInfos}. The in-world
 * upgrade of a placed shell is SimpleBuilding's pile and stays its test. Every test runs standalone and with SimpleBuilding.
 */
final class RareFindsTests {
    private static final double CHANCE = 0.01;
    private static final double REINFORCED_HEALTH = 45.0;
    private static final double ENDERITE_HEALTH = 90.0;

    private RareFindsTests() {
    }

    /** Table -&gt; tier, the 1 % rate and the double chest rule (both halves roll, same result). */
    static void betterChestTablesRateAndDoubleChestRule(GameTestHelper helper) {
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.STRONGHOLD_LIBRARY), ChestTier.REINFORCED, "stronghold library");
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.STRONGHOLD_CORRIDOR), ChestTier.REINFORCED, "stronghold corridor");
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.BASTION_TREASURE), ChestTier.NETHERITE, "bastion treasure");
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.NETHER_BRIDGE), ChestTier.NETHERITE, "nether fortress");
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.END_CITY_TREASURE), ChestTier.ENDERITE, "end city / end ship");
        helper.assertTrue(BetterChests.tierFor(BuiltInLootTables.SIMPLE_DUNGEON) == null, "a dungeon chest must stay vanilla");
        helper.assertTrue(Math.abs(StorageConfig.betterChestChance() - CHANCE) < 1e-9, "default chance " + StorageConfig.betterChestChance());

        long seed = 1234567L;
        int singles = 0;
        int doubles = 0;
        int samples = 40000;
        BlockState left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
        BlockState right = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT);
        for (int i = 0; i < samples; i++) {
            BlockPos pos = new BlockPos(i * 3, 64, (i * 7) % 1000);
            if (BetterChests.decides(seed, pos, Blocks.CHEST.defaultBlockState(), CHANCE)) {
                singles++;
            }
            // A north-facing LEFT half connects east; its partner is the RIGHT half there.
            BlockPos partner = ChestBlock.getConnectedBlockPos(pos, left);
            boolean a = BetterChests.decides(seed, pos, left, CHANCE);
            boolean b = BetterChests.decides(seed, partner, right, CHANCE);
            helper.assertTrue(a == b, "the halves of a double chest at " + pos + " disagree");
            if (a) {
                doubles++;
                helper.assertTrue(BetterChests.rolls(seed, pos, CHANCE) && BetterChests.rolls(seed, partner, CHANCE),
                        "a double chest upgraded although a half missed");
            }
        }
        helper.assertTrue(singles > samples * 0.007 && singles < samples * 0.013, "single chest rate " + singles + "/" + samples);
        helper.assertTrue(doubles <= 10, "double chest rate far above 0.01 %: " + doubles + "/" + samples);
        helper.assertTrue(!BetterChests.rolls(seed, BlockPos.ZERO, 0.0) && BetterChests.rolls(seed, BlockPos.ZERO, 1.0), "chance 0 / 1");
        helper.succeed();
    }

    /** Bastion: a template chest with a bastion table becomes the Netherite Chest while the template is processed. */
    static void bastionTemplateChestBecomesNetheriteChest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long seed = level.getSeed();
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
        BlockPos hit = null;
        BlockPos miss = null;
        for (int i = 0; i < 100000 && (hit == null || miss == null); i++) {
            BlockPos pos = new BlockPos(i, 70, 3);
            boolean rolls = BetterChests.decides(seed, pos, chest, CHANCE);
            if (rolls && hit == null) hit = pos;
            if (!rolls && miss == null) miss = pos;
        }
        helper.assertTrue(hit != null && miss != null, "no hitting position found");
        List<StructureTemplate.StructureBlockInfo> infos = new ArrayList<>();
        infos.add(new StructureTemplate.StructureBlockInfo(hit, chest, lootNbt("minecraft:chests/bastion_other")));
        infos.add(new StructureTemplate.StructureBlockInfo(miss, chest, lootNbt("minecraft:chests/bastion_other")));
        infos.add(new StructureTemplate.StructureBlockInfo(hit.above(), chest, lootNbt("minecraft:chests/simple_dungeon")));
        List<StructureTemplate.StructureBlockInfo> out = StructureTemplate.processBlockInfos(level, BlockPos.ZERO, BlockPos.ZERO,
                new StructurePlaceSettings(), infos);
        Map<BlockPos, StructureTemplate.StructureBlockInfo> byPos = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : out) {
            byPos.put(info.pos(), info);
        }
        helper.assertTrue(byPos.get(hit).state().is(Refs.chestBlock("netherite")), "rolled bastion chest is " + byPos.get(hit).state());
        helper.assertValueEqual(byPos.get(hit).state().getValue(ChestBlock.FACING), Direction.SOUTH, "facing kept");
        helper.assertValueEqual(byPos.get(hit).nbt().getString("LootTable").orElse(""), "minecraft:chests/bastion_other", "loot table kept");
        helper.assertTrue(byPos.get(miss).state().is(Blocks.CHEST), "missed bastion chest changed: " + byPos.get(miss).state());
        helper.assertTrue(byPos.get(hit.above()).state().is(Blocks.CHEST), "a dungeon table must not upgrade");
        helper.succeed();
    }

    private static CompoundTag lootNbt(String table) {
        CompoundTag tag = new CompoundTag();
        tag.putString("LootTable", table);
        tag.putLong("LootTableSeed", 42L);
        return tag;
    }

    /** A placed end city chest becomes the Enderite Chest, keeps table and seed and rolls its loot twice. */
    static void placedEndCityChestBecomesEnderiteChestWithDoubleLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos vanillaPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos betterPos = helper.absolutePos(new BlockPos(3, 1, 1));
        long lootSeed = 987654321L;
        for (BlockPos pos : List.of(vanillaPos, betterPos)) {
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
            ((RandomizableContainer) level.getBlockEntity(pos)).setLootTable(BuiltInLootTables.END_CITY_TREASURE, lootSeed);
        }
        helper.assertTrue(BetterChests.upgradePlaced(level, betterPos, 1.0), "chance 1 did not upgrade");
        helper.assertTrue(level.getBlockState(betterPos).is(Refs.chestBlock("enderite")), "not an enderite chest: " + level.getBlockState(betterPos));
        RandomizableContainer better = (RandomizableContainer) level.getBlockEntity(betterPos);
        helper.assertValueEqual(better.getLootTable(), BuiltInLootTables.END_CITY_TREASURE, "loot table");
        helper.assertValueEqual(better.getLootTableSeed(), lootSeed, "loot seed");
        helper.assertTrue(!BetterChests.upgradePlaced(level, vanillaPos, 0.0), "chance 0 upgraded");

        ((RandomizableContainer) level.getBlockEntity(vanillaPos)).unpackLootTable(null);
        better.unpackLootTable(null);
        Map<Item, Integer> vanilla = counts((Container) level.getBlockEntity(vanillaPos));
        Map<Item, Integer> doubled = counts((Container) better);
        int vanillaTotal = vanilla.values().stream().mapToInt(Integer::intValue).sum();
        int doubledTotal = doubled.values().stream().mapToInt(Integer::intValue).sum();
        helper.assertTrue(vanillaTotal > 0, "the vanilla end city chest is empty");
        for (Map.Entry<Item, Integer> entry : vanilla.entrySet()) {
            helper.assertTrue(doubled.getOrDefault(entry.getKey(), 0) >= entry.getValue(),
                    "the first roll is missing " + entry.getKey() + ": " + doubled);
        }
        helper.assertTrue(doubledTotal > vanillaTotal, "no second roll: " + doubledTotal + " vs " + vanillaTotal);
        helper.succeed();
    }

    private static Map<Item, Integer> counts(Container container) {
        Map<Item, Integer> out = new HashMap<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                out.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return out;
    }

    /** Roll, health, saving of the tier and 0-2 shells on death (through the real death path). */
    static void rareShulkersHaveTieredHealthAndDropTheirShells(GameTestHelper helper) {
        // Bands from 0: enderite 0.5 %, netherite 1 %, reinforced 2 % (together 3.5 %).
        helper.assertValueEqual(RareShulkers.tierFor(0.004, 0.005, 0.01, 0.02), ChestTier.ENDERITE, "roll below enderite");
        helper.assertValueEqual(RareShulkers.tierFor(0.010, 0.005, 0.01, 0.02), ChestTier.NETHERITE, "roll in the netherite band");
        helper.assertValueEqual(RareShulkers.tierFor(0.020, 0.005, 0.01, 0.02), ChestTier.REINFORCED, "roll in the reinforced band");
        helper.assertTrue(RareShulkers.tierFor(0.036, 0.005, 0.01, 0.02) == null, "roll above all bands");
        helper.assertTrue(Math.abs(StorageConfig.netheriteShulkerChance() - 0.01) < 1e-9, "default netherite chance");
        helper.assertTrue(Math.abs(StorageConfig.reinforcedShulkerChance() - 0.02) < 1e-9
                && Math.abs(StorageConfig.enderiteShulkerChance() - 0.005) < 1e-9, "default shulker chances");

        ServerLevel level = helper.getLevel();
        Shulker plain = helper.spawn(EntityTypes.SHULKER, new BlockPos(1, 1, 1));
        Shulker reinforced = helper.spawn(EntityTypes.SHULKER, new BlockPos(3, 1, 1));
        Shulker enderite = helper.spawn(EntityTypes.SHULKER, new BlockPos(5, 1, 1));
        RareShulkers.apply(reinforced, ChestTier.REINFORCED);
        RareShulkers.apply(enderite, ChestTier.ENDERITE);
        helper.assertTrue(RareShulkers.tierOf(plain) == null, "a plain shulker has a tier");
        helper.assertValueEqual(RareShulkers.tierOf(reinforced), ChestTier.REINFORCED, "reinforced tier");
        helper.assertValueEqual(RareShulkers.tierOf(enderite), ChestTier.ENDERITE, "enderite tier");
        helper.assertTrue(Math.abs(reinforced.getMaxHealth() - REINFORCED_HEALTH) < 1e-3 && Math.abs(reinforced.getHealth() - REINFORCED_HEALTH) < 1e-3,
                "reinforced health " + reinforced.getHealth() + "/" + reinforced.getMaxHealth());
        helper.assertTrue(Math.abs(enderite.getMaxHealth() - ENDERITE_HEALTH) < 1e-3, "enderite health " + enderite.getMaxHealth());
        helper.assertTrue(Math.abs(plain.getMaxHealth() - 30.0) < 1e-3, "plain health " + plain.getMaxHealth());
        helper.assertValueEqual(reinforced.getAttribute(Attributes.MAX_HEALTH).getModifier(RareShulkers.REINFORCED_HEALTH_ID) != null,
                true, "tier modifier present (saved and synced with the attribute)");

        // Ten reinforced shulkers die: every one drops 0-2 Reinforced Shulker Shells and no other tier shell.
        int shells = 0;
        for (int i = 0; i < 10; i++) {
            Shulker victim = helper.spawn(EntityTypes.SHULKER, new BlockPos(1 + (i % 5), 1, 3 + i / 5));
            RareShulkers.apply(victim, ChestTier.REINFORCED);
            victim.kill(level);
        }
        plain.kill(level);
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16.0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = item.getItem();
            helper.assertTrue(!stack.is(StorageItems.NETHERITE_SHULKER_SHELL) && !stack.is(StorageItems.ENDERITE_SHULKER_SHELL),
                    "a reinforced shulker dropped " + stack);
            if (stack.is(StorageItems.REINFORCED_SHULKER_SHELL)) {
                helper.assertTrue(stack.getCount() <= 2, "more than two shells at once: " + stack);
                shells += stack.getCount();
            }
        }
        helper.assertTrue(shells > 0 && shells <= 20, "ten reinforced shulkers dropped " + shells + " shells");
        helper.succeed();
    }

    /**
     * Netherite tier (2x health, Netherite shells) and the Easter egg: living shulkers take the nugget of their next tier (iron,
     * Netherite, Enderite), one each; wrong nuggets do nothing; upgraded shulkers drop no tier shells (else upgrading + killing =
     * cheap shells).
     */
    static void livingShulkersClimbTheTiersButUpgradedOnesDropNoShells(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Shulker netherite = helper.spawn(EntityTypes.SHULKER, new BlockPos(1, 1, 1));
        RareShulkers.apply(netherite, ChestTier.NETHERITE);
        helper.assertValueEqual(RareShulkers.tierOf(netherite), ChestTier.NETHERITE, "netherite tier");
        helper.assertTrue(Math.abs(netherite.getMaxHealth() - 60.0) < 1e-3, "netherite health " + netherite.getMaxHealth());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        Shulker shulker = helper.spawn(EntityTypes.SHULKER, new BlockPos(4, 1, 4));
        Item netheriteNugget = Refs.material("netherite_nugget");
        Item enderiteNugget = Refs.material("enderite_nugget");
        ItemStack wrong = new ItemStack(netheriteNugget, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrong);
        helper.assertTrue(!RareShulkers.upgradeLiving(shulker, player, InteractionHand.MAIN_HAND), "a netherite nugget skipped a tier");
        helper.assertValueEqual(wrong.getCount(), 2, "wrong nugget used");
        Item[] nuggets = {Items.IRON_NUGGET, netheriteNugget, enderiteNugget};
        ChestTier[] tiers = {ChestTier.REINFORCED, ChestTier.NETHERITE, ChestTier.ENDERITE};
        double[] health = {45.0, 60.0, 90.0};
        for (int i = 0; i < 3; i++) {
            ItemStack nugget = new ItemStack(nuggets[i], 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, nugget);
            // The real click path: Player#interactOn -> Mob#interact -> Mob#mobInteract (mixin).
            player.interactOn(shulker, InteractionHand.MAIN_HAND, shulker.position());
            helper.assertValueEqual(RareShulkers.tierOf(shulker), tiers[i], "tier after nugget " + i);
            helper.assertValueEqual(nugget.getCount(), 1, "one nugget per step " + i);
            helper.assertTrue(Math.abs(shulker.getMaxHealth() - health[i]) < 1e-3, "health after step " + i + ": " + shulker.getMaxHealth());
        }
        ItemStack more = new ItemStack(enderiteNugget, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, more);
        helper.assertTrue(!RareShulkers.upgradeLiving(shulker, player, InteractionHand.MAIN_HAND), "upgraded past enderite");
        helper.assertTrue(shulker.entityTags().contains(RareShulkers.UPGRADED_TAG), "upgrade not marked");

        // Exploit guard: ten player-upgraded shulkers die and drop no tier shell; ten natural netherite ones drop 0-2 each.
        for (int i = 0; i < 10; i++) {
            Shulker upgraded = helper.spawn(EntityTypes.SHULKER, new BlockPos(1 + (i % 5), 1, 6));
            ItemStack iron = new ItemStack(Items.IRON_NUGGET, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, iron);
            helper.assertTrue(RareShulkers.upgradeLiving(upgraded, player, InteractionHand.MAIN_HAND), "upgrade " + i);
            upgraded.kill(level);
        }
        shulker.kill(level);
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16.0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            helper.assertTrue(!ShulkerShells.tierShells().contains(item.getItem().getItem()), "an upgraded shulker dropped " + item.getItem());
        }
        int shells = 0;
        for (int i = 0; i < 10; i++) {
            Shulker natural = helper.spawn(EntityTypes.SHULKER, new BlockPos(1 + (i % 5), 1, 3));
            RareShulkers.apply(natural, ChestTier.NETHERITE);
            natural.kill(level);
        }
        netherite.kill(level);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = item.getItem();
            if (stack.is(StorageItems.NETHERITE_SHULKER_SHELL)) {
                helper.assertTrue(stack.getCount() <= 2, "more than two shells at once: " + stack);
                shells += stack.getCount();
            }
            helper.assertTrue(!stack.is(StorageItems.REINFORCED_SHULKER_SHELL) && !stack.is(StorageItems.ENDERITE_SHULKER_SHELL), "foreign shell " + stack);
        }
        helper.assertTrue(shells > 0 && shells <= 22, "eleven netherite shulkers dropped " + shells + " shells");
        helper.succeed();
    }

    /**
     * Endermite escort: a rare shulker with a due escort calls 4 endermites once (default), but only when a player comes near;
     * they stand on safe spots near the shulker and are not persistent.
     */
    static void rareShulkerCallsFourEndermitesOnceWhenPlayersComeNear(GameTestHelper helper) {
        helper.assertValueEqual(StorageConfig.endermitesPerRareShulker, 4, "default endermites per rare shulker");
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        Shulker shulker = helper.spawn(EntityTypes.SHULKER, new BlockPos(4, 1, 4));
        RareShulkers.apply(shulker, ChestTier.ENDERITE);
        shulker.addTag(RareShulkers.ESCORT_TAG);
        // The escort samples only twelve positions per mite, including two unsupported Y
        // layers. An unlucky random stream can exhaust those attempts on an empty floor.
        shulker.getRandom().setSeed(0L);
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8.0, 8.0, 8.0).inflate(4.0);

        // No player near: the escort waits (the tick only looks once a second). Skipped if a survival player of a
        // neighbouring test happens to stand within range.
        if (level.getNearestPlayer(shulker.getX(), shulker.getY(), shulker.getZ(), RareShulkers.ESCORT_TRIGGER_RANGE,
                net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR) == null) {
            for (int i = 0; i < 40; i++) {
                shulker.tickCount = i;
                RareShulkers.tickEscort(shulker);
            }
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.monster.Endermite.class, area).isEmpty(), "endermites without a player");
            helper.assertTrue(shulker.entityTags().contains(RareShulkers.ESCORT_TAG), "escort tag gone without a player");
        }

        // A creative player (the mock player is one) right next to it does not call the escort either.
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        // Outside the seeded spawn cells, but still inside the escort's trigger range.
        Vec3 near = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
        player.snapTo(near.x, near.y, near.z, 0.0F, 0.0F);
        if (player.isCreative()) {
            shulker.tickCount = 40;
            RareShulkers.tickEscort(shulker);
            helper.assertTrue(shulker.entityTags().contains(RareShulkers.ESCORT_TAG), "a creative player called the escort");
        }
        // A survival player in range triggers it (tickEscort -> triggerEscort; the mock player cannot be a survival
        // player, so the trigger runs directly). Vanilla refuses endermites on Peaceful (EntityType#create), so does the
        // escort, which then stays pending. The difficulty is raised only for these synchronous calls of this tick.
        net.minecraft.world.Difficulty before = level.getDifficulty();
        if (before == net.minecraft.world.Difficulty.PEACEFUL) {
            helper.assertValueEqual(RareShulkers.triggerEscort(level, shulker), 0, "endermites on Peaceful");
            helper.assertTrue(shulker.entityTags().contains(RareShulkers.ESCORT_TAG), "Peaceful used up the escort");
        }
        int spawned;
        int again;
        // Seed 0 selects these four distinct floor cells before exhausting its attempt budget.
        for (BlockPos spot : List.of(new BlockPos(6, 1, 5), new BlockPos(1, 1, 5),
                new BlockPos(1, 1, 1), new BlockPos(3, 1, 7))) {
            helper.assertTrue(RareShulkers.isSafeSpot(level, helper.absolutePos(spot)),
                    "escort fixture is obstructed at " + spot);
        }
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        try {
            spawned = RareShulkers.triggerEscort(level, shulker);
            again = RareShulkers.triggerEscort(level, shulker);
        } finally {
            level.getServer().setDifficulty(before, true);
        }
        helper.assertValueEqual(spawned, 4, "endermites called");
        helper.assertValueEqual(again, 0, "a second call");
        List<net.minecraft.world.entity.monster.Endermite> mites = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Endermite.class, area);
        helper.assertValueEqual(mites.size(), 4, "endermites around the rare shulker");
        helper.assertTrue(!shulker.entityTags().contains(RareShulkers.ESCORT_TAG), "escort tag still there");
        for (net.minecraft.world.entity.monster.Endermite mite : mites) {
            helper.assertTrue(!mite.isPersistenceRequired(), "a persistent endermite");
            helper.assertTrue(mite.distanceTo(shulker) <= RareShulkers.ESCORT_RADIUS * 1.8 + 1.0, "endermite too far: " + mite.distanceTo(shulker));
            helper.assertTrue(level.getBlockState(mite.blockPosition().below()).isSolid(), "endermite without ground at " + mite.blockPosition());
        }
        helper.assertTrue(!RareShulkers.isSafeSpot(level, helper.absolutePos(new BlockPos(4, 1, 4))), "the shulker's own block is a safe spot");
        mites.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    /** Copper chest + tier shell + shulker shell craft the tier's shulker box; the config has its limits. */
    static void shellRecipesAndConfigCaps(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item[][] cases = {
                {StorageItems.REINFORCED_SHULKER_SHELL, Refs.shulkerBox("reinforced")},
                {StorageItems.NETHERITE_SHULKER_SHELL, Refs.shulkerBox("netherite")},
                {StorageItems.ENDERITE_SHULKER_SHELL, Refs.shulkerBox("enderite")}};
        Item copperChest = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("exposed_copper_chest"));
        for (Item[] c : cases) {
            CraftingInput grid = CraftingInput.of(3, 1, List.of(new ItemStack(Items.SHULKER_SHELL), new ItemStack(copperChest), new ItemStack(c[0])));
            Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
            helper.assertTrue(match.isPresent(), "no recipe for " + c[0]);
            helper.assertTrue(match.get().value().assemble(grid).is(c[1]), c[0] + " crafts " + match.get().value().assemble(grid));
        }
        // Two vanilla shells and a copper chest stay the vanilla recipe's business (no tier box).
        CraftingInput plain = CraftingInput.of(3, 1, List.of(new ItemStack(Items.SHULKER_SHELL), new ItemStack(copperChest), new ItemStack(Items.SHULKER_SHELL)));
        Optional<RecipeHolder<CraftingRecipe>> none = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, plain, level);
        helper.assertTrue(none.isEmpty() || SimpleStorage.TIERS.stream().map(Refs::shulkerBox)
                .noneMatch(box -> none.get().value().assemble(plain).is(box)), "two vanilla shells made a tier box");

        JsonObject saved = StorageConfig.toJson();
        try {
            JsonObject json = new JsonObject();
            json.addProperty("betterChestPercent", 99.0);
            json.addProperty("reinforcedShulkerPercent", 99.0);
            json.addProperty("enderiteShulkerPercent", Double.NaN);
            json.addProperty("endermitesPerRareShulker", 99);
            StorageConfig.apply(json);
            helper.assertValueEqual(StorageConfig.betterChestPercent, 5.0, "better chest cap");
            helper.assertValueEqual(StorageConfig.reinforcedShulkerPercent, 10.0, "reinforced shulker cap");
            helper.assertValueEqual(StorageConfig.enderiteShulkerPercent, 0.5, "NaN falls back to the default");
            helper.assertValueEqual(StorageConfig.netheriteShulkerPercent, 1.0, "missing key falls back to the default");
            helper.assertValueEqual(StorageConfig.endermitesPerRareShulker, 8, "endermite cap");
        } finally {
            StorageConfig.apply(saved);
        }
        helper.assertValueEqual(ShulkerShells.steps().size(), 3, "upgrade chain length");
        helper.succeed();
    }

    /** The shells are registered (rarities, fire-proof as in SimpleBuilding), in the tab, their recipes loaded. */
    static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleStorage.MOVED_SHELLS) {
            if (!BuiltInRegistries.ITEM.containsKey(SimpleStorage.id(path))) problems.add("item " + path);
        }
        if (new ItemStack(StorageItems.REINFORCED_SHULKER_SHELL).has(DataComponents.DAMAGE_RESISTANT)) problems.add("reinforced shell fire-proof");
        for (Item item : List.of(StorageItems.NETHERITE_SHULKER_SHELL, StorageItems.ENDERITE_SHULKER_SHELL)) {
            if (!new ItemStack(item).has(DataComponents.DAMAGE_RESISTANT)) problems.add(item + " not fire-proof");
        }
        if (new ItemStack(StorageItems.NETHERITE_SHULKER_SHELL).get(DataComponents.RARITY) != Rarity.UNCOMMON) problems.add("netherite shell rarity");
        if (new ItemStack(StorageItems.ENDERITE_SHULKER_SHELL).get(DataComponents.RARITY) != Rarity.EPIC) problems.add("enderite shell rarity");
        var manager = h.getLevel().getServer().getRecipeManager();
        for (String path : SimpleStorage.MOVED_SHELL_RECIPES) {
            if (manager.byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, SimpleStorage.id(path))).isEmpty()) problems.add("recipe " + path);
        }
        var tab = com.simplestorage.registry.StorageRegistry.tabStacks();
        for (Item shell : ShulkerShells.tierShells()) {
            if (tab.stream().noneMatch(s -> s.is(shell))) problems.add("tab lacks " + shell);
        }
        h.assertTrue(problems.isEmpty(), "rare finds registered: " + problems);
        h.succeed();
    }

    /**
     * Old worlds: the shells and their recipes resolve from {@code simplebuilding:}; a rare shulker of an old world (SimpleBuilding's
     * modifier and tags) keeps its tier, its escort and its upgraded mark; the old config values are taken over.
     */
    static void migrationFromSimplebuilding(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleStorage.MOVED_SHELLS) {
            if (BuiltInRegistries.ITEM.getValue(SimpleStorage.old(path)) != BuiltInRegistries.ITEM.getValue(SimpleStorage.id(path))) problems.add("item " + path);
        }
        for (String path : SimpleStorage.MOVED_SHELL_RECIPES) {
            if (!SimpleStorage.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleStorage.old(path)))) problems.add("recipe " + path);
            Identifier advancement = SimpleStorage.id("recipes/decorations/" + path);
            if (!advancement.equals(IdMoves.advancement(SimpleStorage.old("recipes/decorations/" + path)))) problems.add("advancement " + path);
        }
        // An Enderite shulker of an old world: SimpleBuilding's modifier, a due escort and (another one) the upgraded mark.
        Shulker old = h.spawn(EntityTypes.SHULKER, new BlockPos(2, 1, 2));
        old.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(SimpleStorage.old("enderite_shulker"),
                RareShulkers.ENDERITE_HEALTH_FACTOR - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        old.addTag(RareShulkers.LEGACY_ESCORT_TAG);
        if (RareShulkers.tierOf(old) != ChestTier.ENDERITE) problems.add("old enderite shulker reads as " + RareShulkers.tierOf(old));
        if (Math.abs(old.getMaxHealth() - ENDERITE_HEALTH) > 1e-3) problems.add("old enderite health " + old.getMaxHealth());
        RareShulkers.apply(old, ChestTier.REINFORCED);
        if (old.getAttribute(Attributes.MAX_HEALTH).getModifier(SimpleStorage.old("enderite_shulker")) != null) problems.add("re-tiering kept the old modifier");
        if (RareShulkers.tierOf(old) != ChestTier.REINFORCED) problems.add("re-tiered shulker reads as " + RareShulkers.tierOf(old));
        net.minecraft.world.Difficulty before = h.getLevel().getDifficulty();
        int mites;
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        try {
            mites = RareShulkers.triggerEscort(h.getLevel(), old);
        } finally {
            h.getLevel().getServer().setDifficulty(before, true);
        }
        if (old.entityTags().contains(RareShulkers.LEGACY_ESCORT_TAG)) problems.add("old escort tag left after the call (" + mites + " mites)");
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Endermite.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(8.0))
                .forEach(net.minecraft.world.entity.Entity::discard);
        Shulker upgraded = h.spawn(EntityTypes.SHULKER, new BlockPos(4, 1, 2));
        upgraded.addTag(RareShulkers.LEGACY_UPGRADED_TAG);
        if (!RareShulkers.upgradedByPlayer(upgraded)) problems.add("old upgraded mark not read");
        // SimpleBuilding's config values (server.loot.*) and its bundle option come over on the first start.
        String taken = StorageConfig.fromSimpleBuilding("{\"tools\":{\"invertBundleInteractions\":true},\"server\":{\"loot\":{\"betterChestPercent\":3.0,"
                + "\"endermitesPerRareShulker\":2,\"globalLootMultiplier\":2.0}}}");
        JsonObject json = taken == null ? new JsonObject() : JsonParser.parseString(taken).getAsJsonObject();
        if (!json.has("betterChestPercent") || json.get("betterChestPercent").getAsDouble() != 3.0) problems.add("betterChestPercent not taken: " + taken);
        if (!json.has("endermitesPerRareShulker") || json.get("endermitesPerRareShulker").getAsInt() != 2) problems.add("endermites not taken: " + taken);
        if (!json.has("invertBundleInteractions") || json.has("globalLootMultiplier")) problems.add("config takeover " + taken);
        h.assertTrue(problems.isEmpty(), "rare finds migration: " + problems);
        h.succeed();
    }
}
