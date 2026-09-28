package com.simplebuilding.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blueprint.BlueprintBuilder;
import com.simplebuilding.data.ChiselTables;
import com.simplebuilding.data.ModDataTables;
import com.simplebuilding.data.SledgehammerUpgradeData;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.MagnetItem;
import com.simplebuilding.loot.CoreChanceCondition;
import com.simplebuilding.loot.LootInjection;
import com.simplebuilding.networking.DataTablesSyncPayload;
import com.simplebuilding.platform.ProtectionProbe;
import com.simplebuilding.stats.ModStats;
import com.simplebuilding.tweaks.item.LaserBeam;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.util.AttractorFilter;
import com.simplebuilding.util.MiningUtils;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.util.SledgehammerUsageEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The modpack and server hooks (docs/MODPACK.md): claim protection through the loader's own break
 * and place events, the attractor's hands-off rules, the ore and common tags, the datapack tables
 * (chisel chains, sledgehammer upgrades) with their client sync, the loot injection tables and the
 * player statistics.
 *
 * <p><b>The claim mod</b> is {@link ProtectionProbe}: every loader's guard installer registers an
 * ordinary listener on the real events (Fabric {@code PlayerBlockBreakEvents.BEFORE}, NeoForge and
 * Forge the break event and {@code BlockEvent.EntityPlaceEvent}) that refuses the positions a test
 * names - so these tests go through exactly the path a protection mod sees. Every test hands the
 * refused positions back in a {@code finally}.
 */
public final class ModpackTests {

    private static final int TICK_CAP = 200;

    private ModpackTests() {
    }

    // =====================================================================================
    // CLAIM PROTECTION
    // =====================================================================================

    /**
     * The building wand asks the loader's place event for every cell of a plane (a claim mod
     * refusing one cell keeps that cell empty and unpaid), counts what it placed in the statistic
     * {@code simplebuilding:wand_blocks_placed}, and never places a block from the tag
     * {@code simplebuilding:building_wand_blacklist}.
     *
     * <p>What breaks it: dropping {@code BuildPermissions#mayPlace} or the blacklist check from the
     * wand's placement loop, or the {@code ModStats.award} next to it.
     */
    public static void buildingWandSkipsCellsTheLoaderEventsRefuseAndCountsItsBlocks(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(0.5, 5.0, 7.5));
        clearRoom(helper);
        BlockPos anchor = new BlockPos(3, 1, 3);
        helper.setBlock(anchor, Blocks.STONE);
        BlockPos claimed = anchor.offset(1, 1, 0);
        Runnable release = ProtectionProbe.refuseAt(helper.absolutePos(claimed));
        try {
            ItemStack wand = wand(ModItems.DIAMOND_BUILDING_WAND, 1);
            stock(player, wand, new ItemStack(Items.GLASS, 64));
            int before = stat(player, ModStats.WAND_BLOCKS_PLACED);
            click(helper, player, wand, anchor);
            runUntilIdle(helper, player, wand);

            int glass = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (helper.getBlockState(anchor.offset(dx, 1, dz)).is(Blocks.GLASS)) {
                        glass++;
                    }
                }
            }
            helper.assertTrue(helper.getBlockState(claimed).isAir(),
                    "the wand built into the cell the loader's place event refused: " + helper.getBlockState(claimed));
            helper.assertTrue(glass == 8, "the wand did not build the eight cells it may build on, it built " + glass);
            helper.assertTrue(countIn(player, Items.GLASS) == 64 - 8,
                    "the wand did not pay exactly the eight cells it built: " + countIn(player, Items.GLASS) + " glass left");
            int counted = stat(player, ModStats.WAND_BLOCKS_PLACED) - before;
            helper.assertTrue(counted == 8, "the statistic wand_blocks_placed counted " + counted + " instead of 8");

            // --- the blacklist tag: structure void is in it by default ---
            clearRoom(helper);
            helper.setBlock(anchor, Blocks.STONE);
            stock(player, wand, new ItemStack(Items.STRUCTURE_VOID, 64));
            click(helper, player, wand, anchor);
            runUntilIdle(helper, player, wand);
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(7, 7, 7))) {
                helper.assertTrue(!helper.getBlockState(pos).is(Blocks.STRUCTURE_VOID),
                        "the wand placed a block from simplebuilding:building_wand_blacklist at " + pos.immutable());
            }
            helper.assertTrue(countIn(player, Items.STRUCTURE_VOID) == 64, "the wand charged for blacklisted blocks");
        } finally {
            release.run();
            clearRoom(helper);
        }
        helper.succeed();
    }

    /**
     * An octant fill (the blueprint planner, the same loop a blueprint build runs) leaves a cell the
     * loader's place event refuses empty and builds the rest.
     *
     * <p>What breaks it: dropping {@code BuildPermissions#mayPlace} from {@code Planner#run}.
     */
    public static void octantFillSkipsCellsTheLoaderEventsRefuse(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(0.5, 5.0, 7.5));
        clearRoom(helper);
        BlockPos anchor = new BlockPos(6, 1, 6);
        helper.setBlock(anchor, Blocks.STONE);
        BlockPos claimed = new BlockPos(3, 1, 3);
        Runnable release = ProtectionProbe.refuseAt(helper.absolutePos(claimed));
        try {
            ItemStack wand = wand(ModItems.DIAMOND_BUILDING_WAND, 1);
            stock(player, wand, new ItemStack(Items.GLASS, 64));
            player.setItemInHand(InteractionHand.OFF_HAND, octant(helper, new BlockPos(2, 1, 2), new BlockPos(4, 1, 4)));
            click(helper, player, wand, anchor);
            BlueprintBuilder.completeJob(player);
            int glass = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 1, 2), new BlockPos(4, 1, 4))) {
                if (helper.getBlockState(pos).is(Blocks.GLASS)) {
                    glass++;
                }
            }
            helper.assertTrue(helper.getBlockState(claimed).isAir(),
                    "the octant fill built into the cell the loader's place event refused");
            helper.assertTrue(glass == 8, "the octant fill did not build the eight free cells, it built " + glass);
        } finally {
            release.run();
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            clearRoom(helper);
        }
        helper.succeed();
    }

    /**
     * The amethyst lens beam melts ice only where the loader's break event lets the player change
     * the block; the refusal costs nothing and the same beam melts the ice once the claim is gone.
     *
     * <p>What breaks it: dropping {@code claimAllows} from {@code LaserBeam#beamAt}.
     */
    public static void lensBeamLeavesBlocksTheLoaderEventsProtect(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        BlockPos target = new BlockPos(2, 2, 2);
        helper.setBlock(target.below(), Blocks.STONE);
        helper.setBlock(target, Blocks.ICE);
        Runnable release = ProtectionProbe.refuseAt(helper.absolutePos(target));
        try {
            beam(helper, player, lens, target, LaserBeam.MELT_TICKS);
            helper.assertTrue(helper.getBlockState(target).is(Blocks.ICE),
                    "the beam melted ice the loader's break event protects: " + helper.getBlockState(target));
            helper.assertTrue(lens.getDamageValue() == 0, "a refused beam still used up charge");
        } finally {
            release.run();
        }
        beam(helper, player, lens, target, LaserBeam.MELT_TICKS);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.WATER),
                "without the claim the beam no longer melts the ice (positive control): " + helper.getBlockState(target));
        helper.setBlock(target, Blocks.AIR);
        helper.succeed();
    }

    /**
     * The sledgehammer's area swing breaks its extra blocks through
     * {@code ServerPlayerGameMode#destroyBlock}, which fires the loader's break event for each: a
     * block the claim mod protects stays, the others go.
     *
     * <p>What breaks it: breaking the extra blocks any other way (e.g. {@code Level#destroyBlock}).
     */
    public static void sledgehammerAreaSwingLeavesBlocksTheLoaderEventsProtect(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5));
        player.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, 90.0F);
        BlockPos centre = new BlockPos(3, 1, 3);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(centre.offset(dx, 0, dz), Blocks.STONE);
            }
        }
        BlockPos claimed = centre.offset(1, 0, 0);
        Runnable release = ProtectionProbe.refuseAt(helper.absolutePos(claimed));
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));
            BlockPos origin = helper.absolutePos(centre);
            SledgehammerUsageEvent.handleBeforeBlockBreak(helper.getLevel(), player, origin,
                    helper.getLevel().getBlockState(origin), null);
            helper.assertTrue(helper.getBlockState(claimed).is(Blocks.STONE),
                    "the sledgehammer broke a block the loader's break event protects");
            int broken = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx != 0 || dz != 0) && helper.getBlockState(centre.offset(dx, 0, dz)).isAir()) {
                        broken++;
                    }
                }
            }
            helper.assertTrue(broken == 7, "the swing did not break the seven unprotected neighbours, it broke " + broken);
        } finally {
            release.run();
            helper.killAllEntitiesOfClass(ItemEntity.class);
        }
        helper.succeed();
    }

    // =====================================================================================
    // ATTRACTOR
    // =====================================================================================

    /**
     * The attractor (magnet) pulls loose items, but leaves alone: items that can never be picked
     * up (other mods' display items), items reserved for another player, other players' death drops
     * and the item tag {@code simplebuilding:attractor_ignore}. The owner's own death drop is
     * pulled. A player's death marks the dropped inventory as that player's death drop.
     *
     * <p>What breaks it: dropping {@code AttractorFilter#mayAttract} from the magnet, any rule in
     * it, or the death-drop marking in {@code PlayerEntityMixin}/{@code LivingEntityMixin}.
     */
    public static void attractorLeavesDisplayItemsOwnedItemsAndOtherPlayersDeathDropsAlone(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 1.0, 3.5));
        ItemStack magnet = new ItemStack(ModItems.MAGNET);
        player.setItemInHand(InteractionHand.MAIN_HAND, magnet);
        player.setShiftKeyDown(false);

        ItemEntity loose = drop(helper, Items.STONE, 20);
        ItemEntity display = drop(helper, Items.STONE, 20);
        display.setNeverPickUp();
        ItemEntity reserved = drop(helper, Items.STONE, 20);
        reserved.setTarget(UUID.randomUUID());
        ItemEntity foreignDeath = drop(helper, Items.STONE, 20);
        foreignDeath.addTag(AttractorFilter.DEATH_DROP_TAG_PREFIX + UUID.randomUUID());
        ItemEntity ownDeath = drop(helper, Items.STONE, 20);
        ownDeath.addTag(AttractorFilter.DEATH_DROP_TAG_PREFIX + player.getUUID());
        ItemEntity tagged = drop(helper, Items.STRUCTURE_VOID, 20);
        try {
            ((MagnetItem) magnet.getItem()).inventoryTick(magnet, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            helper.assertTrue(!loose.hasPickUpDelay(), "the attractor no longer pulls an ordinary loose item (control)");
            helper.assertTrue(!ownDeath.hasPickUpDelay(), "the attractor does not pull the player's own death drop");
            helper.assertTrue(display.hasPickUpDelay(), "the attractor made a never-pick-up display item collectable");
            helper.assertTrue(reserved.hasPickUpDelay(), "the attractor pulled an item reserved for another player");
            helper.assertTrue(foreignDeath.hasPickUpDelay(), "the attractor pulled another player's death drop");
            helper.assertTrue(tagged.hasPickUpDelay(), "the attractor pulled an item from simplebuilding:attractor_ignore");
        } finally {
            for (ItemEntity entity : List.of(loose, display, reserved, foreignDeath, ownDeath, tagged)) {
                entity.discard();
            }
        }

        // --- a real death marks the dropped inventory ---
        ServerPlayer victim = mockPlayer(helper, new Vec3(5.5, 1.0, 5.5));
        victim.getInventory().clearContent();
        victim.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        victim.die(victim.damageSources().generic());
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(),
                entity -> entity.getItem().is(Items.DIAMOND));
        try {
            helper.assertTrue(!drops.isEmpty(), "the dying player dropped no diamonds (is keepInventory on?)");
            for (ItemEntity entity : drops) {
                helper.assertTrue(victim.getUUID().equals(AttractorFilter.deathDropOwner(entity)),
                        "a death drop carries no death-drop mark of its owner: " + entity.entityTags());
                helper.assertTrue(!AttractorFilter.mayAttract(entity, player), "another player's attractor may take the death drop");
                helper.assertTrue(AttractorFilter.mayAttract(entity, victim), "the owner's attractor may not take its own death drop");
            }
        } finally {
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    // =====================================================================================
    // TAGS
    // =====================================================================================

    /**
     * Vein Miner's ore list is the block tag {@code simplebuilding:vein_miner_ores}: every block of
     * the loader's {@code #c:ores}, the vanilla ores, nether quartz ore, nether gold ore, ancient
     * debris and the mod's own ores, and nothing that is not an ore. The common tags name every mod
     * material under the {@code c:} conventions, item and block side.
     */
    public static void oreAndCommonTagsCoverEveryModMaterial(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (Block ore : List.of(Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS, Blocks.NETHER_GOLD_ORE,
                Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.COAL_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                com.simplebuilding.blocks.ModBlocks.NIHILITH_ORE, com.simplebuilding.blocks.ModBlocks.ASTRALIT_ORE)) {
            if (!MiningUtils.isOre(ore.defaultBlockState())) {
                problems.add(BuiltInRegistries.BLOCK.getKey(ore) + " is not a Vein Miner ore");
            }
        }
        for (Block notOre : List.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.OAK_LOG, Blocks.DIAMOND_BLOCK)) {
            if (MiningUtils.isOre(notOre.defaultBlockState())) {
                problems.add(BuiltInRegistries.BLOCK.getKey(notOre) + " counts as a Vein Miner ore");
            }
        }
        TagKey<Block> conventionOres = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
        int convention = 0;
        for (var holder : BuiltInRegistries.BLOCK.getTagOrEmpty(conventionOres)) {
            convention++;
            if (!holder.value().defaultBlockState().is(ModTags.Blocks.VEIN_MINER_ORES)) {
                problems.add(holder.getRegisteredName() + " is in #c:ores but not a Vein Miner ore");
            }
        }
        if (convention == 0) {
            problems.add("#c:ores is empty - the loader's convention tags did not load");
        }

        Map<String, Item> itemTags = new java.util.LinkedHashMap<>();
        itemTags.put("ingots", ModItems.ENDERITE_INGOT);
        itemTags.put("ingots/enderite", ModItems.ENDERITE_INGOT);
        itemTags.put("nuggets/enderite", ModItems.ENDERITE_NUGGET);
        itemTags.put("nuggets/netherite", ModItems.NETHERITE_NUGGET);
        itemTags.put("nuggets", ModItems.NETHERITE_NUGGET);
        itemTags.put("raw_materials/enderite", ModItems.RAW_ENDERITE);
        itemTags.put("raw_materials", ModItems.RAW_ENDERITE);
        itemTags.put("gems/ender_quartz", ModItems.ENDER_QUARTZ);
        itemTags.put("gems/nihilith", ModItems.NIHILITH_SHARD);
        itemTags.put("dusts/astralit", ModItems.ASTRALIT_DUST);
        itemTags.put("storage_blocks/enderite", ModItems.ENDERITE_BLOCK_ITEM);
        itemTags.put("storage_blocks/cracked_diamond", ModItems.CRACKED_DIAMOND_BLOCK);
        itemTags.put("storage_blocks", ModItems.ENDER_QUARTZ_BLOCK);
        itemTags.put("ores/nihilith", ModItems.NIHILITH_ORE_ITEM);
        itemTags.put("ores", ModItems.ASTRALIT_ORE_ITEM);
        itemTags.forEach((path, item) -> {
            if (!new ItemStack(item).is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path)))) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " is not in the item tag c:" + path);
            }
        });
        Map<String, Block> blockTags = Map.of(
                "storage_blocks/enderite", com.simplebuilding.blocks.ModBlocks.ENDERITE_BLOCK,
                "storage_blocks", com.simplebuilding.blocks.ModBlocks.CRACKED_DIAMOND_BLOCK,
                "ores/astralit", com.simplebuilding.blocks.ModBlocks.ASTRALIT_ORE,
                "ores_in_ground/end_stone", com.simplebuilding.blocks.ModBlocks.NIHILITH_ORE);
        blockTags.forEach((path, block) -> {
            if (!block.defaultBlockState().is(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", path)))) {
                problems.add(BuiltInRegistries.BLOCK.getKey(block) + " is not in the block tag c:" + path);
            }
        });
        helper.assertTrue(problems.isEmpty(), "tag problems:\n" + String.join("\n", problems));
        helper.succeed();
    }

    /**
     * The backpack refuses items of the tag {@code simplebuilding:not_allowed_in_backpack} on top of
     * its built-in rule (no backpacks, no shulker boxes), and still takes ordinary items.
     */
    public static void backpackRefusesItemsFromTheNotAllowedTag(GameTestHelper helper) {
        helper.assertTrue(!BackpackItem.mayStore(new ItemStack(Items.STRUCTURE_VOID)),
                "the backpack takes an item from simplebuilding:not_allowed_in_backpack");
        helper.assertTrue(!BackpackItem.mayStore(new ItemStack(Items.SHULKER_BOX)), "the backpack takes a shulker box");
        helper.assertTrue(!BackpackItem.mayStore(new ItemStack(ModItems.BACKPACK)), "the backpack takes another backpack");
        helper.assertTrue(BackpackItem.mayStore(new ItemStack(Items.COBBLESTONE)), "the backpack refuses cobblestone (control)");
        helper.succeed();
    }

    // =====================================================================================
    // DATAPACK TABLES
    // =====================================================================================

    /**
     * The chisel chains and the sledgehammer upgrades come from the datapack files
     * ({@code data/simplebuilding/chisel_transformations/}, {@code .../sledgehammer_upgrades/}), and
     * the shipped files say exactly what the built-in tables said: every tier's four maps equal
     * {@code ChiselItem.FINAL_*}, the upgrade table equals {@code SledgehammerUpgrades#builtInTable}.
     *
     * <p>What breaks it: a code change to a built-in table without a new {@code runDatagen}, a
     * chain the recorder in {@code ChiselItem} misses, or a reader that parses a file differently.
     */
    public static void chiselAndUpgradeTablesComeFromTheDatapackAndMatchTheBuiltInTables(GameTestHelper helper) {
        helper.assertTrue(ChiselTables.isLoaded(), "no chisel transformation files were loaded");
        helper.assertTrue(SledgehammerUpgradeData.isLoaded(), "no sledgehammer upgrade files were loaded");
        List<String> problems = new ArrayList<>();
        compareTier(problems, "stone", ChiselItem.FINAL_STONE_FWD, ChiselItem.FINAL_STONE_BWD,
                ChiselItem.FINAL_STONE_TOUCH_FWD, ChiselItem.FINAL_STONE_TOUCH_BWD);
        compareTier(problems, "iron", ChiselItem.FINAL_IRON_FWD, ChiselItem.FINAL_IRON_BWD,
                ChiselItem.FINAL_IRON_TOUCH_FWD, ChiselItem.FINAL_IRON_TOUCH_BWD);
        compareTier(problems, "diamond", ChiselItem.FINAL_DIAMOND_FWD, ChiselItem.FINAL_DIAMOND_BWD,
                ChiselItem.FINAL_DIAMOND_TOUCH_FWD, ChiselItem.FINAL_DIAMOND_TOUCH_BWD);
        compareTier(problems, "netherite", ChiselItem.FINAL_NETHERITE_FWD, ChiselItem.FINAL_NETHERITE_BWD,
                ChiselItem.FINAL_NETHERITE_TOUCH_FWD, ChiselItem.FINAL_NETHERITE_TOUCH_BWD);
        compareTier(problems, "enderite", ChiselItem.FINAL_ENDERITE_FWD, ChiselItem.FINAL_ENDERITE_BWD,
                ChiselItem.FINAL_ENDERITE_TOUCH_FWD, ChiselItem.FINAL_ENDERITE_TOUCH_BWD);
        if (!ChiselTables.builtInTiers().equals(Map.of(
                "stone", ChiselTables.tier("stone"), "iron", ChiselTables.tier("iron"), "diamond", ChiselTables.tier("diamond"),
                "netherite", ChiselTables.tier("netherite"), "enderite", ChiselTables.tier("enderite")))) {
            problems.add("the recorded built-in chains compose to other maps than the loaded files");
        }
        if (!SledgehammerUpgradeData.table().equals(SledgehammerUpgrades.builtInTable())) {
            problems.add("the loaded sledgehammer upgrades differ from the built-in table: "
                    + SledgehammerUpgradeData.table().size() + " against " + SledgehammerUpgrades.builtInTable().size());
        }
        // The stone chisel really works off the loaded maps.
        helper.assertTrue(ModItems.STONE_CHISEL.getForwardMap() == ChiselTables.tier("stone").forward(),
                "the stone chisel does not read the loaded stone tier");
        helper.assertTrue(problems.isEmpty(), "datapack tables differ from the built-in ones:\n" + String.join("\n", problems));
        helper.succeed();
    }

    /**
     * What a datapack can do with the tables, sent to the client the way the sync payload carries
     * it: a file of another namespace adds a chain (inherited by the higher tiers) and removes a
     * block, links to a block that does not exist are skipped, a broken file is skipped, an upgrade
     * is added and another removed. Afterwards the server's own files are applied again.
     */
    public static void datapackFilesExtendAndRemoveChiselAndUpgradeEntries(GameTestHelper helper) {
        Map<String, String> chisel = new HashMap<>(ModDataTables.lastChiselFiles());
        chisel.put("apack:extra", "{\"tier\":\"stone\",\"table\":\"chisel\",\"chains\":[{\"blocks\":[\"minecraft:dirt\",\"minecraft:coarse_dirt\"]}],"
                + "\"remove\":[\"minecraft:stone\"]}");
        chisel.put("apack:unknown", "{\"tier\":\"iron\",\"chains\":[{\"blocks\":[\"minecraft:gravel\",\"nomod:thing\",\"minecraft:sand\"]}]}");
        chisel.put("apack:broken", "{\"tier\":\"wood\"}");
        Map<String, String> upgrades = new HashMap<>(ModDataTables.lastUpgradeFiles());
        upgrades.put("apack:extra", "{\"upgrades\":[{\"from\":\"minecraft:hopper\",\"to\":\"simplebuilding:reinforced_hopper\","
                + "\"material\":\"minecraft:iron_nugget\",\"min_hammer\":\"any\",\"damage_per_hit\":3,\"tier\":\"reinforced\"}],"
                + "\"remove\":[\"simplebuilding:reinforced_piston\"]}");
        ByteBuf raw = Unpooled.buffer();
        try {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(raw, helper.getLevel().registryAccess());
            DataTablesSyncPayload.CODEC.encode(buffer, new DataTablesSyncPayload(chisel, upgrades));
            ModDataTables.receive(DataTablesSyncPayload.CODEC.decode(buffer));

            helper.assertTrue(ChiselTables.tier("stone").forward().get(Blocks.DIRT) == Blocks.COARSE_DIRT,
                    "a chain from another namespace did not reach the stone chisel");
            helper.assertTrue(ChiselTables.tier("netherite").backward().get(Blocks.COARSE_DIRT) == Blocks.DIRT,
                    "a stone chain is not inherited by the higher tiers");
            helper.assertTrue(!ChiselTables.tier("stone").forward().containsKey(Blocks.STONE),
                    "\"remove\" did not take stone out of the stone chisel");
            helper.assertTrue(!ChiselTables.tier("iron").forward().containsKey(Blocks.GRAVEL)
                            && !ChiselTables.tier("iron").backward().containsKey(Blocks.SAND),
                    "links to a block that does not exist were not skipped");
            helper.assertTrue(ChiselTables.tier("diamond").forward().get(Blocks.SMOOTH_QUARTZ) == Blocks.QUARTZ_PILLAR,
                    "a broken file took the default chains down with it");
            SledgehammerUpgrades.Upgrade added = SledgehammerUpgrades.upgradeOf(Blocks.HOPPER);
            helper.assertTrue(added != null && added.nugget() == Items.IRON_NUGGET && added.minHammerRank() == 0
                            && added.damagePerHit() == 3 && added.toReinforced(),
                    "the added upgrade is missing or misread: " + added);
            helper.assertTrue(SledgehammerUpgrades.upgradeOf(com.simplebuilding.blocks.ModBlocks.REINFORCED_PISTON) == null,
                    "\"remove\" did not take the reinforced piston upgrade out");
            helper.assertTrue(SledgehammerUpgrades.isUpgradeNugget(new ItemStack(Items.IRON_NUGGET)),
                    "the added upgrade's material is not an upgrade material");
        } finally {
            raw.release();
            ModDataTables.reapplyLastLoad();
        }
        helper.assertTrue(SledgehammerUpgrades.upgradeOf(Blocks.HOPPER) == null
                        && ChiselTables.tier("stone").forward().containsKey(Blocks.STONE),
                "the server's own tables did not come back");
        helper.succeed();
    }

    // =====================================================================================
    // LOOT INJECTION
    // =====================================================================================

    /**
     * Every vanilla table the mod has loot for rolls the mod's table
     * {@code simplebuilding:inject/<path>}, and each loaded inject table holds exactly the pools
     * {@code ModLootTableModifications#apply} defines. The core chance condition passes with its
     * chance and scales with the config factor.
     *
     * <p>What breaks it: a loot change in the code without a new {@code runDatagen}, or a loader hook
     * that adds the pools directly again (the vanilla table then has no reference).
     */
    public static void lootInjectionTablesMatchTheCodeAndVanillaTablesRollThem(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        Set<ResourceKey<LootTable>> keys = new LinkedHashSet<>(BuiltInLootTables.all());
        keys.add(BuiltInLootTables.CHARGED_CREEPER);
        List<String> problems = new ArrayList<>();
        int injected = 0;
        for (ResourceKey<LootTable> key : keys) {
            List<LootPool> pools = LootInjection.defaultPools(key, registries);
            if (pools.isEmpty()) {
                continue;
            }
            injected++;
            ResourceKey<LootTable> injectKey = LootInjection.injectKey(key);
            LootTable table = server.reloadableRegistries().getLootTable(injectKey);
            if (table == LootTable.EMPTY) {
                problems.add(injectKey.identifier() + " is missing");
                continue;
            }
            JsonArray expected = new JsonArray();
            for (LootPool pool : pools) {
                expected.add(LootPool.CODEC.encodeStart(ops, pool).getOrThrow());
            }
            JsonElement loaded = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject().get("pools");
            if (!expected.equals(loaded)) {
                problems.add(injectKey.identifier() + " differs from the code:\n  file " + loaded + "\n  code " + expected);
            }
            String vanilla = LootTable.DIRECT_CODEC.encodeStart(ops, server.reloadableRegistries().getLootTable(key)).getOrThrow().toString();
            // 26.2 writes the reference as the table id; 26.3 holds it as a bound Holder and the codec
            // writes the loaded inject table inline - either way it is the loaded table that rolls.
            if (!vanilla.contains(injectKey.identifier().toString()) && (loaded == null || !vanilla.contains(loaded.toString()))) {
                problems.add(key.identifier() + " does not roll " + injectKey.identifier() + " (table ends "
                        + vanilla.substring(Math.max(0, vanilla.length() - 300)) + ")");
            }
        }
        helper.assertTrue(injected >= 15, "the mod injects into only " + injected + " vanilla tables");
        helper.assertTrue(problems.isEmpty(), "loot injection problems:\n" + String.join("\n", problems));

        LootContext context = new LootContext.Builder(new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY))
                .withOptionalRandomSeed(7L)
                .create(Optional.empty());
        double original = Simplebuilding.getConfig().worldGen.buildingCoreLootChanceMultiplier;
        try {
            helper.assertTrue(new CoreChanceCondition(1.0f).test(context), "a core chance of 1 did not pass");
            helper.assertTrue(!new CoreChanceCondition(0.0f).test(context), "a core chance of 0 passed");
            Simplebuilding.getConfig().worldGen.buildingCoreLootChanceMultiplier = 0.0;
            helper.assertTrue(!new CoreChanceCondition(1.0f).test(context), "the config factor 0 did not switch the cores off");
        } finally {
            Simplebuilding.getConfig().worldGen.buildingCoreLootChanceMultiplier = original;
        }
        helper.succeed();
    }

    // =====================================================================================
    // STATISTICS
    // =====================================================================================

    /**
     * The three statistics are registered custom stats (so the vanilla statistics screen lists
     * them), a chisel step counts one {@code chisel_uses}, and {@code ModStats#award} counts a
     * teleport. The wand's count is in the wand test above.
     */
    public static void modStatisticsAreRegisteredAndCountChiselUseAndTeleports(GameTestHelper helper) {
        for (Identifier stat : ModStats.ALL) {
            helper.assertTrue(BuiltInRegistries.CUSTOM_STAT.getKey(stat) != null, stat + " is not a registered custom stat");
        }
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 1.5));
        BlockPos stone = new BlockPos(3, 1, 3);
        helper.setBlock(stone, Blocks.STONE);
        ItemStack chisel = new ItemStack(ModItems.STONE_CHISEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, chisel);
        int before = stat(player, ModStats.CHISEL_USES);
        BlockPos pos = helper.absolutePos(stone);
        chisel.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false)));
        helper.assertTrue(helper.getBlockState(stone).is(Blocks.CHISELED_STONE_BRICKS),
                "the stone chisel did not chisel stone (control): " + helper.getBlockState(stone));
        helper.assertTrue(stat(player, ModStats.CHISEL_USES) - before == 1, "a chisel step did not count one chisel_uses");

        int teleports = stat(player, ModStats.TELEPORTS);
        ModStats.award(player, ModStats.TELEPORTS);
        helper.assertTrue(stat(player, ModStats.TELEPORTS) - teleports == 1, "ModStats.award did not count the teleport");
        helper.setBlock(stone, Blocks.AIR);
        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static void compareTier(List<String> problems, String tier, Map<Block, Block> fwd, Map<Block, Block> bwd,
                                    Map<Block, Block> touchFwd, Map<Block, Block> touchBwd) {
        ChiselTables.Tier loaded = ChiselTables.tier(tier);
        if (!loaded.forward().equals(fwd)) problems.add(tier + " forward: " + diff(loaded.forward(), fwd));
        if (!loaded.backward().equals(bwd)) problems.add(tier + " backward: " + diff(loaded.backward(), bwd));
        if (!loaded.touchForward().equals(touchFwd)) problems.add(tier + " touch forward: " + diff(loaded.touchForward(), touchFwd));
        if (!loaded.touchBackward().equals(touchBwd)) problems.add(tier + " touch backward: " + diff(loaded.touchBackward(), touchBwd));
    }

    private static String diff(Map<Block, Block> loaded, Map<Block, Block> builtIn) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<Block, Block> entry : builtIn.entrySet()) {
            if (loaded.get(entry.getKey()) != entry.getValue()) {
                out.add("built-in " + BuiltInRegistries.BLOCK.getKey(entry.getKey()) + "->" + BuiltInRegistries.BLOCK.getKey(entry.getValue()));
            }
        }
        for (Map.Entry<Block, Block> entry : loaded.entrySet()) {
            if (!builtIn.containsKey(entry.getKey())) {
                out.add("extra " + BuiltInRegistries.BLOCK.getKey(entry.getKey()) + "->" + BuiltInRegistries.BLOCK.getKey(entry.getValue()));
            }
        }
        return String.join(", ", out);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static int stat(ServerPlayer player, Identifier stat) {
        return player.getStats().getValue(Stats.CUSTOM.get(stat));
    }

    private static ItemEntity drop(GameTestHelper helper, Item item, int pickupDelay) {
        Vec3 at = helper.absoluteVec(new Vec3(5.5, 1.2, 3.5));
        ItemEntity entity = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(item));
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setPickUpDelay(pickupDelay);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static void beam(GameTestHelper helper, ServerPlayer player, ItemStack lens, BlockPos relative, int ticks) {
        BlockPos pos = helper.absolutePos(relative);
        Vec3 point = Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0);
        BlockHitResult hit = new BlockHitResult(point, Direction.UP, pos, false);
        for (int i = 0; i < ticks; i++) {
            LaserBeam.beamAt(player, lens, hit);
        }
    }

    private static void clearRoom(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(7, 7, 7))) {
            helper.setBlock(pos, Blocks.AIR);
        }
    }

    private static ItemStack wand(BuildingWandItem item, int radius) {
        ItemStack wand = new ItemStack(item);
        CompoundTag settings = new CompoundTag();
        settings.putInt("SettingsRadius", radius);
        settings.putInt("SettingsAxis", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return wand;
    }

    private static ItemStack octant(GameTestHelper helper, BlockPos from, BlockPos to) {
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        CompoundTag nbt = new CompoundTag();
        BlockPos a = helper.absolutePos(from);
        BlockPos b = helper.absolutePos(to);
        nbt.putIntArray("Pos1", new int[]{a.getX(), a.getY(), a.getZ()});
        nbt.putIntArray("Pos2", new int[]{b.getX(), b.getY(), b.getZ()});
        nbt.putString("Shape", "CUBOID");
        nbt.putBoolean("Hollow", false);
        nbt.putBoolean("LayerMode", false);
        nbt.putString("FillOrder", "BOTTOM_UP");
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return octant;
    }

    /** Wand in the selected slot 0, supplies from slot 1 on, empty off hand. */
    private static void stock(ServerPlayer player, ItemStack wand, ItemStack... supplies) {
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        for (int i = 0; i < supplies.length; i++) {
            player.getInventory().setItem(i + 1, supplies[i]);
        }
    }

    private static void click(GameTestHelper helper, ServerPlayer player, ItemStack wand, BlockPos relative) {
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false);
        wand.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void runUntilIdle(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        BuildingWandItem item = (BuildingWandItem) wand.getItem();
        int ticks = 0;
        while (wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr("Active", false) && ticks < TICK_CAP) {
            item.inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            ticks++;
        }
        helper.assertTrue(ticks < TICK_CAP, "the wand never finished within " + TICK_CAP + " ticks");
    }

    private static int countIn(ServerPlayer player, Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }
}
