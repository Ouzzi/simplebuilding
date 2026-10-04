package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.BetterChests;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.RareShulkers;
import com.simplebuilding.util.ShulkerShells;
import com.simplebuilding.util.TransformTargets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Seltene Strukturfunde (Besitzer 2026-10-02): bessere Loot-Truhen in Festung, Bastion/Netherfestung und
 * End-Stadt/End-Schiff, seltene verstaerkte und Enderit-Shulker, die Stufen-Schalen und ihre Aufwertung in der Welt.
 *
 * <p>Die Zahlen sind eigene Konstanten dieser Datei (1 %, 1,5-/3-faches Leben, 0-2 Schalen, ein Klumpen je Schale),
 * nicht aus der Mod gelesen. Nicht hier: echte Strukturgenerierung einer Festung oder End-Stadt (die Mixins auf
 * {@code StructurePiece#createChest} und {@code EndCityPiece#handleDataMarker} rufen {@link BetterChests#upgradePlaced}
 * und {@link RareShulkers#onEndCitySpawn}, die hier direkt laufen); die Vorlagen-Route der Bastion laeuft echt
 * ueber {@link StructureTemplate#processBlockInfos}.
 */
public final class RareStructureFindsTests {
    private static final double CHANCE = 0.01;
    private static final double REINFORCED_HEALTH = 45.0;
    private static final double ENDERITE_HEALTH = 90.0;

    private RareStructureFindsTests() {
    }

    /** Tabelle -&gt; Stufe, die 1-%-Rate und die Doppeltruhen-Regel (beide Haelften wuerfeln, gleiches Ergebnis). */
    public static void betterChestTablesRateAndDoubleChestRule(GameTestHelper helper) {
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.STRONGHOLD_LIBRARY), ChestTier.REINFORCED, "stronghold library");
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.STRONGHOLD_CORRIDOR), ChestTier.REINFORCED, "stronghold corridor");
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.BASTION_TREASURE), ChestTier.NETHERITE, "bastion treasure");
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.NETHER_BRIDGE), ChestTier.NETHERITE, "nether fortress");
        helper.assertValueEqual(BetterChests.tierFor(BuiltInLootTables.END_CITY_TREASURE), ChestTier.ENDERITE, "end city / end ship");
        helper.assertTrue(BetterChests.tierFor(BuiltInLootTables.SIMPLE_DUNGEON) == null, "a dungeon chest must stay vanilla");
        helper.assertTrue(Math.abs(ServerTuning.betterChestChance() - CHANCE) < 1e-9, "default chance " + ServerTuning.betterChestChance());

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

    /** Bastion: eine Vorlagen-Truhe mit Bastion-Tabelle wird beim Verarbeiten der Vorlage zur Netherit-Truhe. */
    public static void bastionTemplateChestBecomesNetheriteChest(GameTestHelper helper) {
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
        helper.assertTrue(byPos.get(hit).state().is(ModBlocks.NETHERITE_CHEST), "rolled bastion chest is " + byPos.get(hit).state());
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

    /** Eine gesetzte End-Stadt-Truhe wird zur Enderit-Truhe, behaelt Tabelle und Seed und wuerfelt ihre Beute doppelt. */
    public static void placedEndCityChestBecomesEnderiteChestWithDoubleLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos vanillaPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos betterPos = helper.absolutePos(new BlockPos(3, 1, 1));
        long lootSeed = 987654321L;
        for (BlockPos pos : List.of(vanillaPos, betterPos)) {
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
            ((RandomizableContainer) level.getBlockEntity(pos)).setLootTable(BuiltInLootTables.END_CITY_TREASURE, lootSeed);
        }
        helper.assertTrue(BetterChests.upgradePlaced(level, betterPos, 1.0), "chance 1 did not upgrade");
        helper.assertTrue(level.getBlockState(betterPos).is(ModBlocks.ENDERITE_CHEST), "not an enderite chest: " + level.getBlockState(betterPos));
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

    /** Wurf, Leben, Speichern der Stufe und 0-2 Schalen beim Tod (ueber den echten Todespfad). */
    public static void rareShulkersHaveTieredHealthAndDropTheirShells(GameTestHelper helper) {
        // Bands from 0: enderite 0.5 %, netherite 1 %, reinforced 2 % (together 3.5 %).
        helper.assertValueEqual(RareShulkers.tierFor(0.004, 0.005, 0.01, 0.02), ChestTier.ENDERITE, "roll below enderite");
        helper.assertValueEqual(RareShulkers.tierFor(0.010, 0.005, 0.01, 0.02), ChestTier.NETHERITE, "roll in the netherite band");
        helper.assertValueEqual(RareShulkers.tierFor(0.020, 0.005, 0.01, 0.02), ChestTier.REINFORCED, "roll in the reinforced band");
        helper.assertTrue(RareShulkers.tierFor(0.036, 0.005, 0.01, 0.02) == null, "roll above all bands");
        helper.assertTrue(Math.abs(ServerTuning.netheriteShulkerChance() - 0.01) < 1e-9, "default netherite chance");
        helper.assertTrue(Math.abs(ServerTuning.reinforcedShulkerChance() - 0.02) < 1e-9
                && Math.abs(ServerTuning.enderiteShulkerChance() - 0.005) < 1e-9, "default shulker chances");

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
            helper.assertTrue(!stack.is(ModItems.NETHERITE_SHULKER_SHELL) && !stack.is(ModItems.ENDERITE_SHULKER_SHELL),
                    "a reinforced shulker dropped " + stack);
            if (stack.is(ModItems.REINFORCED_SHULKER_SHELL)) {
                helper.assertTrue(stack.getCount() <= 2, "more than two shells at once: " + stack);
                shells += stack.getCount();
            }
        }
        helper.assertTrue(shells > 0 && shells <= 20, "ten reinforced shulkers dropped " + shells + " shells");
        helper.succeed();
    }

    /**
     * Netherit-Stufe (2-faches Leben, Netherit-Schalen) und das Easter Egg: lebende Shulker nehmen den Klumpen ihrer
     * naechsten Stufe (Eisen, Netherit, Enderit), je einen; falsche Klumpen tun nichts; aufgewertete Shulker lassen keine
     * Stufen-Schalen fallen (sonst Aufwerten + Toeten = billige Schalen).
     */
    public static void livingShulkersClimbTheTiersButUpgradedOnesDropNoShells(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Shulker netherite = helper.spawn(EntityTypes.SHULKER, new BlockPos(1, 1, 1));
        RareShulkers.apply(netherite, ChestTier.NETHERITE);
        helper.assertValueEqual(RareShulkers.tierOf(netherite), ChestTier.NETHERITE, "netherite tier");
        helper.assertTrue(Math.abs(netherite.getMaxHealth() - 60.0) < 1e-3, "netherite health " + netherite.getMaxHealth());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        Shulker shulker = helper.spawn(EntityTypes.SHULKER, new BlockPos(4, 1, 4));
        ItemStack wrong = new ItemStack(ModItems.NETHERITE_NUGGET, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrong);
        helper.assertTrue(!RareShulkers.upgradeLiving(shulker, player, InteractionHand.MAIN_HAND), "a netherite nugget skipped a tier");
        helper.assertValueEqual(wrong.getCount(), 2, "wrong nugget used");
        Item[] nuggets = {Items.IRON_NUGGET, ModItems.NETHERITE_NUGGET, ModItems.ENDERITE_NUGGET};
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
        ItemStack more = new ItemStack(ModItems.ENDERITE_NUGGET, 1);
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
            if (stack.is(ModItems.NETHERITE_SHULKER_SHELL)) {
                helper.assertTrue(stack.getCount() <= 2, "more than two shells at once: " + stack);
                shells += stack.getCount();
            }
            helper.assertTrue(!stack.is(ModItems.REINFORCED_SHULKER_SHELL) && !stack.is(ModItems.ENDERITE_SHULKER_SHELL), "foreign shell " + stack);
        }
        helper.assertTrue(shells > 0 && shells <= 22, "eleven netherite shulkers dropped " + shells + " shells");
        helper.succeed();
    }

    /**
     * Endermiten-Begleitung: ein seltener Shulker mit ausstehender Begleitung ruft einmalig 4 Endermiten (Standard), aber
     * erst, wenn ein Spieler naht; sie stehen auf sicheren Plaetzen nahe dem Shulker und sind nicht dauerhaft.
     */
    public static void rareShulkerCallsFourEndermitesOnceWhenPlayersComeNear(GameTestHelper helper) {
        helper.assertValueEqual(ServerTuning.endermitesPerRareShulker(), 4, "default endermites per rare shulker");
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

    /** Schale ablegen, mit Klumpen rechtsklicken: genau eine Stufe, genau ein Klumpen; falscher Klumpen nichts; Hinweis gleich. */
    public static void placedShellsUpgradeOneTierPerNugget(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(Items.SHULKER_SHELL).is(ModTags.Items.PLACEABLE_SMALL)
                && new ItemStack(ModItems.NETHERITE_SHULKER_SHELL).is(ModTags.Items.PLACEABLE_SMALL), "shells are not placeable");
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState());
        PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) level.getBlockEntity(helper.absolutePos(rel));
        pile.setParts(List.of(new ItemStack(Items.SHULKER_SHELL), new ItemStack(Items.SHULKER_SHELL)));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(false);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));

        // Wrong nugget first: a netherite nugget does nothing to a vanilla shell, and the hand does not tilt.
        ItemStack netherite = new ItemStack(ModItems.NETHERITE_NUGGET, 3);
        click(helper, player, rel, netherite);
        helper.assertValueEqual(netherite.getCount(), 3, "a wrong nugget was used");
        helper.assertTrue(pile.parts().get(0).is(Items.SHULKER_SHELL) && pile.parts().get(1).is(Items.SHULKER_SHELL), "a wrong nugget changed a shell");

        ItemStack iron = new ItemStack(Items.IRON_NUGGET, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, iron);
        helper.assertTrue(TransformTargets.canTransformTarget(level, hit(helper, rel), player, InteractionHand.MAIN_HAND), "no hint for the iron nugget");
        click(helper, player, rel, iron);
        helper.assertValueEqual(iron.getCount(), 2, "iron nuggets after one upgrade");
        helper.assertTrue(pile.parts().get(0).is(Items.SHULKER_SHELL) && pile.parts().get(1).is(ModItems.REINFORCED_SHULKER_SHELL),
                "the last placed shell should be reinforced: " + pile.parts());

        click(helper, player, rel, netherite);
        helper.assertValueEqual(netherite.getCount(), 2, "netherite nuggets after one upgrade");
        helper.assertTrue(pile.parts().get(1).is(ModItems.NETHERITE_SHULKER_SHELL), "reinforced -> netherite: " + pile.parts());

        ItemStack enderite = new ItemStack(ModItems.ENDERITE_NUGGET, 1);
        click(helper, player, rel, enderite);
        helper.assertTrue(enderite.isEmpty(), "enderite nugget not used");
        helper.assertTrue(pile.parts().get(1).is(ModItems.ENDERITE_SHULKER_SHELL), "netherite -> enderite: " + pile.parts());

        // An enderite shell is the top; another enderite nugget does nothing.
        ItemStack more = new ItemStack(ModItems.ENDERITE_NUGGET, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, more);
        helper.assertTrue(!TransformTargets.canTransformTarget(level, hit(helper, rel), player, InteractionHand.MAIN_HAND), "hint at the top tier");
        click(helper, player, rel, more);
        helper.assertValueEqual(more.getCount(), 1, "enderite nugget used on the top tier");
        helper.assertValueEqual(pile.parts().size(), 2, "pile size");
        helper.succeed();
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos rel) {
        BlockPos absolute = helper.absolutePos(rel);
        return new BlockHitResult(Vec3.atBottomCenterOf(absolute).add(0.0, 0.05, 0.0), Direction.UP, absolute, false);
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos rel, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 eye = helper.absoluteVec(new Vec3(rel.getX() + 0.5, rel.getY() + 1.0, rel.getZ() + 0.5));
        player.snapTo(eye.x, eye.y, eye.z, 0.0F, 90.0F);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit(helper, rel));
    }

    /** Kupfertruhe + Stufen-Schale + Shulkerschale ergibt die Shulkerkiste der Stufe; die Config hat Obergrenzen. */
    public static void shellRecipesAndConfigCaps(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item[][] cases = {
                {ModItems.REINFORCED_SHULKER_SHELL, ModItems.REINFORCED_SHULKER_BOX},
                {ModItems.NETHERITE_SHULKER_SHELL, ModItems.NETHERITE_SHULKER_BOX},
                {ModItems.ENDERITE_SHULKER_SHELL, ModItems.ENDERITE_SHULKER_BOX}};
        Item copperChest = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("exposed_copper_chest"));
        for (Item[] c : cases) {
            CraftingInput grid = CraftingInput.of(3, 1, List.of(new ItemStack(Items.SHULKER_SHELL), new ItemStack(copperChest), new ItemStack(c[0])));
            Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
            helper.assertTrue(match.isPresent(), "no recipe for " + c[0]);
            helper.assertTrue(match.get().value().assemble(grid).is(c[1]), c[0] + " crafts " + match.get().value().assemble(grid));
        }
        // Two vanilla shells and a copper chest stay the vanilla recipe's business (no tier box).
        CraftingInput plain = CraftingInput.of(3, 1, List.of(new ItemStack(Items.SHULKER_SHELL), new ItemStack(copperChest), new ItemStack(Items.SHULKER_SHELL)));
        Optional<RecipeHolder<CraftingRecipe>> none = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, plain, level);
        helper.assertTrue(none.isEmpty() || com.simplebuilding.util.TieredShulkerBoxes.items().stream()
                .noneMatch(box -> none.get().value().assemble(plain).is(box)), "two vanilla shells made a tier box");

        ServerTuningConfig config = new ServerTuningConfig();
        config.loot.betterChestPercent = 99.0;
        config.loot.reinforcedShulkerPercent = 99.0;
        config.loot.enderiteShulkerPercent = Double.NaN;
        config.validate();
        helper.assertValueEqual(config.loot.betterChestPercent, 5.0, "better chest cap");
        helper.assertValueEqual(config.loot.reinforcedShulkerPercent, 10.0, "reinforced shulker cap");
        helper.assertValueEqual(config.loot.enderiteShulkerPercent, 0.5, "NaN falls back to the default");
        helper.assertValueEqual(ShulkerShells.steps().size(), 3, "upgrade chain length");
        helper.succeed();
    }
}
