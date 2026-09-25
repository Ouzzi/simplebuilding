package com.simplebuilding.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.blocks.entity.custom.FurnaceTierPerks;
import com.simplebuilding.blocks.entity.custom.ModBlastFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.blueprint.BlueprintJobs;
import com.simplebuilding.component.BackpackContents;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.entity.LevitatingBlockEntity;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OreDetectorItem;
import com.simplebuilding.trim.ModTrimMaterials;
import com.simplebuilding.util.SledgehammerProgress;
import com.simplebuilding.util.SurvivalTracerAccessor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * A world played on Minecraft 26.2 with this mod, opened on 26.3 with the mod's 26.3 build, keeps
 * the mod's data. See docs/UPGRADE-26.2-26.3.md for the audit behind this.
 *
 * <p><b>The fixtures.</b> testing/fixtures/upgrade-26.2/*.snbt hold save data exactly as the 26.2
 * build writes it: a chunk (a vanilla chest full of mod items, a mod hopper, an enderite blast
 * furnace, a placed backpack), an entity chunk (a rising block, a locked item frame), a player,
 * and the mod's two saved-data files. They are produced by {@link #fixturesAreWhatTwentySixTwoWrites}
 * on a 26.2 target with the environment variable {@code SIMPLEBUILDING_WRITE_UPGRADE_FIXTURES=1};
 * without it, that test checks on 26.2 that the committed files still are what 26.2 writes (a mod
 * change to a save format then shows up here, before it reaches players).
 *
 * <p><b>The round trip.</b> Every other test here loads a fixture, sends it through the same
 * vanilla data fixer the server uses when it loads an old chunk/player/file
 * ({@code DataFixTypes#update}, which also runs the mod's {@code ModDataFixer}), and hands the
 * result to the mod's own readers. On 26.2 the fixer has nothing to do (same data version); on
 * 26.3 it does the real 26.2 -> 26.3 upgrade. The fixtures deliberately contain vanilla items 26.3
 * changed - an explorer map (26.2: a filled map with an explorer decoration; 26.3: its own item,
 * map colour removed), a decorated pot with
 * sherds (component turned from a list into a map), a bundle holding the map - inside mod storage.
 *
 * <p><b>The oracle.</b> What an upgraded item must look like is not written down here: the same
 * stacks sit in a vanilla chest in the same chunk, which vanilla upgrades itself. Mod storage has
 * to come out equal to that - on any future Minecraft version too.
 */
public final class WorldUpgradeTests {

    /** Data version of Minecraft 26.2 - the version the fixtures were written with. */
    public static final int DATA_VERSION_26_2 = 4903;

    static final String WRITE_ENV = "SIMPLEBUILDING_WRITE_UPGRADE_FIXTURES";
    static final String FIXTURE_DIR = "testing/fixtures/upgrade-26.2";

    static final String CHUNK = "chunk.snbt";
    static final String ENTITIES = "entities.snbt";
    static final String PLAYER = "player.snbt";
    static final String BLUEPRINT_JOBS = "blueprint_jobs.snbt";
    static final String SLEDGEHAMMER_PROGRESS = "sledgehammer_progress.snbt";
    static final List<String> ALL_FIXTURES = List.of(CHUNK, ENTITIES, PLAYER, BLUEPRINT_JOBS, SLEDGEHAMMER_PROGRESS);

    // Positions in the fixture chunk (chunk 0,0).
    static final BlockPos CHEST_POS = new BlockPos(1, 64, 1);
    static final BlockPos HOPPER_POS = new BlockPos(2, 64, 1);
    static final BlockPos FURNACE_POS = new BlockPos(3, 64, 1);
    static final BlockPos BACKPACK_POS = new BlockPos(4, 64, 1);

    /** The vanilla stacks 26.3 changed, in 26.2 spelling. Index = slot in chest and mod storages. */
    /** 26.2 explorer map = filled map with an explorer decoration (26.3: its own item, map_color gone). */
    static final String EXPLORER_MAP = "{id:\"minecraft:filled_map\",count:1,components:{\"minecraft:map_id\":7,"
            + "\"minecraft:map_color\":3830373,\"minecraft:item_name\":{translate:\"filled_map.monument\"},"
            + "\"minecraft:map_decorations\":{\"+\":{type:\"minecraft:monument\",x:128.0d,z:-256.0d,rotation:180.0f}}}}";
    static final String DECORATED_POT = "{id:\"minecraft:decorated_pot\",count:1,components:{\"minecraft:pot_decorations\":"
            + "[\"minecraft:angler_pottery_sherd\",\"minecraft:brick\",\"minecraft:brick\",\"minecraft:skull_pottery_sherd\"]}}";
    static final String BUNDLE_WITH_MAP = "{id:\"simplebuilding:reinforced_bundle\",count:1,components:{\"minecraft:bundle_contents\":["
            + EXPLORER_MAP + "]}}";
    static final int MAP = 0;
    static final int POT = 1;
    static final int BUNDLE = 2;

    // Mod items in the chest, after the three vanilla oracle stacks.
    static final int OCTANT = 3;
    static final int DETECTOR = 4;
    static final int BLUEPRINT = 5;
    static final int BACKPACK_ITEM = 6;
    static final int CHESTPLATE = 7;
    static final int SLEDGEHAMMER = 8;
    static final int MAGNET = 9;
    static final int WAND = 10;

    static final int BACKPACK_COLOUR = 0x3355AA;
    static final int DEEP_POCKET_COUNT = 150;
    static final BlueprintContent BLUEPRINT_CONTENT = new BlueprintContent("fill 3 1 3 minecraft:stone_bricks",
            "Hut", "Tester", true);
    static final BlockState STAIRS = Blocks.OAK_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP);
    static final BlockPos OWNED_LIGHT = new BlockPos(7, 65, 8);
    static final BlueprintJobs.Pending PENDING_JOB = new BlueprintJobs.Pending(new UUID(0x1234L, 0x5678L),
            "c0ffee", "Hut", new BlockPos(10, 64, -20), 2, 17, 12);
    static final SledgehammerProgress.Entry HAMMER_ENTRY = new SledgehammerProgress.Entry(new BlockPos(-3, 70, 5),
            Blocks.DEEPSLATE, 3);

    private WorldUpgradeTests() {
    }

    // =====================================================================================
    // THE WRITER (26.2)
    // =====================================================================================

    /**
     * On 26.2: the committed fixtures are what this build writes (every key and value in the file
     * is present and equal in a fresh write; loaders may add keys of their own). With
     * {@code SIMPLEBUILDING_WRITE_UPGRADE_FIXTURES=1} the files are (re)written instead. On any other
     * version: the fixtures are all there and all carry the 26.2 data version, so the round-trip
     * tests below really start from 26.2 data.
     */
    public static void fixturesAreWhatTwentySixTwoWrites(GameTestHelper helper) {
        Path dir = fixtureDir(helper);
        if (currentDataVersion() != DATA_VERSION_26_2) {
            for (String name : ALL_FIXTURES) {
                CompoundTag fixture = readFixture(helper, name);
                helper.assertValueEqual(fixture.getIntOr("DataVersion", -1), DATA_VERSION_26_2,
                        "DataVersion of fixture " + name);
            }
            helper.succeed();
            return;
        }
        List<CompoundTag> fresh = List.of(writeChunk(helper), writeEntities(helper), writePlayer(helper),
                writeBlueprintJobs(helper), writeSledgehammerProgress(helper));
        boolean write = "1".equals(System.getenv(WRITE_ENV));
        for (int i = 0; i < ALL_FIXTURES.size(); i++) {
            String name = ALL_FIXTURES.get(i);
            if (write) {
                try {
                    Files.createDirectories(dir);
                    Files.writeString(dir.resolve(name), new SnbtPrinterTagVisitor().visit(fresh.get(i)) + "\n",
                            StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw helper.assertionException("could not write fixture " + name + ": " + e);
                }
            } else {
                String mismatch = firstMismatch("", readFixture(helper, name), fresh.get(i));
                helper.assertTrue(mismatch == null, "26.2 no longer writes fixture " + name + " the same way: " + mismatch
                        + ". If the format change is intended, keep the old file as an extra fixture (26.3 must still "
                        + "read what earlier 26.2 builds wrote) and regenerate with " + WRITE_ENV + "=1");
            }
        }
        helper.succeed();
    }

    private static CompoundTag writeChunk(GameTestHelper helper) {
        RegistryOps<Tag> ops = ops(helper);
        List<ItemStack> oracle = List.of(parseStack(helper, EXPLORER_MAP), parseStack(helper, DECORATED_POT),
                parseStack(helper, BUNDLE_WITH_MAP));

        ChestBlockEntity chest = new ChestBlockEntity(CHEST_POS, Blocks.CHEST.defaultBlockState());
        for (int i = 0; i < oracle.size(); i++) {
            chest.setItem(i, oracle.get(i).copy());
        }
        List<ItemStack> modItems = modItemStacks(helper, oracle);
        for (int i = 0; i < modItems.size(); i++) {
            chest.setItem(OCTANT + i, modItems.get(i));
        }

        ModHopperBlockEntity hopper = new ModHopperBlockEntity(HOPPER_POS, ModBlocks.ENDERITE_HOPPER.defaultBlockState());
        CompoundTag hopperSettings = new CompoundTag();
        hopperSettings.putInt("FilterMode", 1);
        hopperSettings.putInt("TransferCooldown", 3);
        CompoundTag ghost = new CompoundTag();
        ListTag ghostItems = new ListTag();
        CompoundTag ghostPot = (CompoundTag) ItemStack.CODEC.encodeStart(ops, oracle.get(POT)).getOrThrow();
        ghostPot.putByte("Slot", (byte) 0);
        ghostItems.add(ghostPot);
        ghost.put("Items", ghostItems);
        hopperSettings.put("GhostItems", ghost);
        hopper.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), hopperSettings));
        for (int i = 0; i < oracle.size(); i++) {
            hopper.setItem(i, oracle.get(i).copy());
        }

        ModBlastFurnaceBlockEntity furnace = new ModBlastFurnaceBlockEntity(FURNACE_POS,
                ModBlocks.ENDERITE_BLAST_FURNACE.defaultBlockState());
        CompoundTag furnaceState = new CompoundTag();
        furnaceState.putInt(FurnaceTierPerks.BONUS_PROGRESS_KEY, 5);
        furnaceState.putInt("cooking_time_spent", 40000);
        furnaceState.putInt("cooking_total_time", 50000);
        furnaceState.putInt("lit_time_remaining", 40001);
        furnaceState.putInt("lit_total_time", 60000);
        furnace.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), furnaceState));
        furnace.setItem(0, oracle.get(MAP).copy());
        furnace.setItem(2, oracle.get(POT).copy());

        BackpackBlockEntity backpack = new BackpackBlockEntity(BACKPACK_POS, ModBlocks.ENDERITE_BACKPACK.defaultBlockState());
        CompoundTag backpackState = new CompoundTag();
        backpackState.put("Contents", BackpackContents.CODEC.encodeStart(ops, backpackContents(helper, oracle)).getOrThrow());
        backpackState.putInt("Color", BACKPACK_COLOUR);
        backpack.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), backpackState));

        ListTag blockEntities = new ListTag();
        for (BlockEntity be : List.of(chest, hopper, furnace, backpack)) {
            blockEntities.add(be.saveWithFullMetadata(helper.getLevel().registryAccess()));
        }
        CompoundTag chunk = new CompoundTag();
        chunk.putInt("DataVersion", currentDataVersion());
        chunk.putInt("xPos", 0);
        chunk.putInt("zPos", 0);
        chunk.putInt("yPos", -4);
        chunk.putString("Status", "minecraft:full");
        chunk.put("sections", new ListTag());
        chunk.put("block_entities", blockEntities);
        return chunk;
    }

    /** The backpack contents used in the placed backpack and in the backpack items. */
    private static BackpackContents backpackContents(GameTestHelper helper, List<ItemStack> oracle) {
        List<BackpackContents.Entry> entries = new ArrayList<>();
        entries.add(BackpackContents.Entry.of(0, oracle.get(MAP)));
        entries.add(BackpackContents.Entry.of(1, oracle.get(POT)));
        entries.add(BackpackContents.Entry.of(2, oracle.get(BUNDLE)));
        ItemStack deepPocket = new ItemStack(Blocks.COBBLESTONE.asItem());
        entries.add(new BackpackContents.Entry(7, new net.minecraft.world.item.ItemStackTemplate(
                deepPocket.typeHolder(), DEEP_POCKET_COUNT, deepPocket.getComponentsPatch())));
        return new BackpackContents(entries);
    }

    /** Mod items, slots OCTANT.. in the chest. Built through the 26.2 item codec (SNBT) where that is simplest. */
    private static List<ItemStack> modItemStacks(GameTestHelper helper, List<ItemStack> oracle) {
        List<ItemStack> items = new ArrayList<>();
        items.add(parseStack(helper, "{id:\"simplebuilding:octant\",count:1,components:{\"minecraft:custom_data\":"
                + "{Pos1:[I;1,2,3],Pos2:[I;-4,5,60],Locked:1b,FillOrder:\"DEFAULT\",Hollow:1b}}}"));

        ItemStack detector = new ItemStack(ModItems.ORE_DETECTOR);
        CompoundTag detectorData = new CompoundTag();
        detectorData.putInt("Mode", 5); // DetectMode.CUSTOM
        detectorData.put("CustomBlock", NbtUtils.writeBlockState(Blocks.OAK_LOG.defaultBlockState()));
        detector.set(DataComponents.CUSTOM_DATA, CustomData.of(detectorData));
        items.add(detector);

        ItemStack blueprint = new ItemStack(ModItems.BLUEPRINT);
        blueprint.set(ModDataComponentTypes.BLUEPRINT, BLUEPRINT_CONTENT);
        blueprint.set(ModDataComponentTypes.BLUEPRINT_ROTATION, 2);
        items.add(blueprint);

        ItemStack backpack = new ItemStack(ModItems.ENDERITE_BACKPACK);
        backpack.set(ModDataComponentTypes.BACKPACK_CONTENTS, backpackContents(helper, oracle));
        backpack.set(DataComponents.DYED_COLOR, new DyedItemColor(BACKPACK_COLOUR));
        items.add(backpack);

        items.add(parseStack(helper, "{id:\"simplebuilding:enderite_chestplate\",count:1,components:{"
                + "\"minecraft:trim\":{material:\"simplebuilding:enderite\",pattern:\"minecraft:sentry\"},"
                + "\"minecraft:enchantments\":{\"simplebuilding:kinetic_protection\":2,\"minecraft:unbreaking\":3},"
                + "\"simplebuilding:glow_level\":3,\"simplebuilding:visual_glow\":1b,\"minecraft:damage\":11}}"));
        items.add(parseStack(helper, "{id:\"simplebuilding:diamond_sledgehammer\",count:1,components:{"
                + "\"minecraft:enchantments\":{\"simplebuilding:range\":2},\"minecraft:damage\":17}}"));
        items.add(parseStack(helper, "{id:\"simplebuilding:magnet\",count:1,components:{\"minecraft:custom_data\":"
                + "{MagnetFilter:\"minecraft:filled_map\"}}}"));
        items.add(parseStack(helper, "{id:\"simplebuilding:enderite_building_wand\",count:1,components:{"
                + "\"minecraft:custom_data\":{SettingsRadius:3,SettingsAxis:1},\"minecraft:enchantments\":"
                + "{\"simplebuilding:master_builder\":1}}}"));
        return items;
    }

    private static CompoundTag writeEntities(GameTestHelper helper) {
        LevitatingBlockEntity rising = new LevitatingBlockEntity(ModEntities.LEVITATING_BLOCK, helper.getLevel());
        CompoundTag risingTag = saveEntity(helper, rising);
        risingTag.put("BlockState", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, STAIRS).getOrThrow());
        risingTag.putInt("Time", 7);
        rising = new LevitatingBlockEntity(ModEntities.LEVITATING_BLOCK, helper.getLevel());
        rising.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), risingTag));

        ItemFrame frame = new ItemFrame(helper.getLevel(), new BlockPos(5, 65, 2), Direction.NORTH);
        frame.setItem(parseStack(helper, EXPLORER_MAP), false);
        CompoundTag frameTag = saveEntity(helper, frame);
        frameTag.putBoolean("SimpleBuildingLocked", true);
        frameTag.put("SimpleBuildingOwnedLight", BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, OWNED_LIGHT).getOrThrow());
        frame = new ItemFrame(helper.getLevel(), new BlockPos(5, 65, 2), Direction.NORTH);
        frame.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), frameTag));

        ListTag entities = new ListTag();
        entities.add(saveEntity(helper, rising));
        entities.add(saveEntity(helper, frame));
        CompoundTag chunk = new CompoundTag();
        chunk.putInt("DataVersion", currentDataVersion());
        chunk.putIntArray("Position", new int[]{0, 0});
        chunk.put("Entities", entities);
        return chunk;
    }

    /** Entity save with its id, a fixed UUID and a fixed position, so every 26.2 write is identical. */
    private static CompoundTag saveEntity(GameTestHelper helper, Entity entity) {
        entity.setUUID(new UUID(0x5B00L, entity instanceof ItemFrame ? 2L : 1L));
        if (!(entity instanceof ItemFrame)) {
            entity.setPos(4.5, 66.0, 1.5);
        }
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        helper.assertTrue(entity.save(output), "entity " + entity + " refused to save");
        return output.buildResult();
    }

    private static final List<String> PLAYER_KEYS = List.of("Inventory", "equipment", "SimpleBuildingData");

    private static CompoundTag writePlayer(GameTestHelper helper) {
        ServerPlayer player = detachedPlayer(helper);
        ((SurvivalTracerAccessor) player).simplebuilding$setBaseValues(11, 22, 33, 44, 55);
        ((SurvivalTracerAccessor) player).simplebuilding$setBaseXp(66);
        ((SurvivalTracerAccessor) player).simplebuilding$setActiveTime(77);
        List<ItemStack> oracle = List.of(parseStack(helper, EXPLORER_MAP), parseStack(helper, DECORATED_POT),
                parseStack(helper, BUNDLE_WITH_MAP));
        player.getInventory().setItem(0, oracle.get(MAP).copy());
        ItemStack backpack = new ItemStack(ModItems.ENDERITE_BACKPACK);
        backpack.set(ModDataComponentTypes.BACKPACK_CONTENTS, backpackContents(helper, oracle));
        player.getInventory().setItem(1, backpack.copy());
        player.setItemSlot(EquipmentSlot.CHEST, backpack.copy());

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        player.saveWithoutId(output);
        CompoundTag full = output.buildResult();
        CompoundTag subset = new CompoundTag();
        subset.putInt("DataVersion", currentDataVersion());
        for (String key : PLAYER_KEYS) {
            Tag value = full.get(key);
            helper.assertTrue(value != null, "a saved player has no " + key);
            subset.put(key, value);
        }
        return subset;
    }

    private static CompoundTag writeBlueprintJobs(GameTestHelper helper) {
        CompoundTag jobs = new CompoundTag();
        ListTag list = new ListTag();
        list.add(BlueprintJobs.Pending.CODEC.encodeStart(NbtOps.INSTANCE, PENDING_JOB).getOrThrow());
        jobs.put("jobs", list);
        BlueprintJobs data = BlueprintJobs.CODEC.parse(NbtOps.INSTANCE, jobs).getOrThrow();
        return savedDataFile(BlueprintJobs.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow());
    }

    private static CompoundTag writeSledgehammerProgress(GameTestHelper helper) {
        CompoundTag progress = new CompoundTag();
        ListTag list = new ListTag();
        list.add(SledgehammerProgress.Entry.CODEC.encodeStart(NbtOps.INSTANCE, HAMMER_ENTRY).getOrThrow());
        progress.put("entries", list);
        SledgehammerProgress data = SledgehammerProgress.CODEC.parse(NbtOps.INSTANCE, progress).getOrThrow();
        return savedDataFile(SledgehammerProgress.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow());
    }

    /** The layout of a data/*.dat file: {DataVersion, data}. */
    private static CompoundTag savedDataFile(Tag data) {
        CompoundTag file = new CompoundTag();
        file.putInt("DataVersion", currentDataVersion());
        file.put("data", data);
        return file;
    }

    // =====================================================================================
    // THE ROUND TRIP (every line; the real upgrade on 26.3)
    // =====================================================================================

    /**
     * Mod hopper, enderite blast furnace and placed backpack from a 26.2 chunk: their items equal
     * what vanilla made of the same stacks in the chest next to them, and their own settings (ghost
     * filter, filter mode, cooldown, int cooking timers, bonus progress, backpack colour and the
     * 150-stack of deep pockets) are all still there.
     *
     * <p>What breaks this: ModDataFixer no longer treating the mod block entities as their vanilla
     * look-alikes (26.3: the explorer map in the hopper and furnace is gone, the pot loses its
     * sherds or vanishes), the hopper's ghost list or the backpack's Contents not being fixed, or
     * a reader of the mod block entities changing its keys.
     */
    public static void modBlockEntitiesSurviveTheUpgrade(GameTestHelper helper) {
        List<CompoundTag> blockEntities = upgradedChunkBlockEntities(helper);
        List<ItemStack> oracle = chestOracle(helper, blockEntities.get(0));

        BlockEntity hopper = load(helper, HOPPER_POS, ModBlocks.ENDERITE_HOPPER.defaultBlockState(), blockEntities.get(1));
        for (int i = 0; i < oracle.size(); i++) {
            assertSameStack(helper, ((Container) hopper).getItem(i), oracle.get(i), "mod hopper slot " + i);
        }
        CompoundTag hopperResaved = hopper.saveCustomOnly(helper.getLevel().registryAccess());
        helper.assertValueEqual(hopperResaved.getIntOr("FilterMode", -1), 1, "mod hopper FilterMode");
        helper.assertValueEqual(hopperResaved.getIntOr("TransferCooldown", -1), 3, "mod hopper TransferCooldown");
        ListTag ghost = hopperResaved.getCompoundOrEmpty("GhostItems").getListOrEmpty("Items");
        // Slot 0 was set as a filter by hand; with a filter mode on, setItem copied slots 1 and 2 in.
        List<ItemStack> expectedGhosts = List.of(oracle.get(POT), oracle.get(POT), oracle.get(BUNDLE));
        helper.assertValueEqual(ghost.size(), expectedGhosts.size(), "ghost filter entries of the mod hopper");
        for (int i = 0; i < expectedGhosts.size(); i++) {
            assertSameStack(helper, ItemStack.CODEC.parse(ops(helper), ghost.getCompoundOrEmpty(i)).result().orElse(ItemStack.EMPTY),
                    expectedGhosts.get(i), "mod hopper ghost filter " + i);
        }

        BlockEntity furnace = load(helper, FURNACE_POS, ModBlocks.ENDERITE_BLAST_FURNACE.defaultBlockState(), blockEntities.get(2));
        assertSameStack(helper, ((Container) furnace).getItem(0), oracle.get(MAP), "blast furnace input");
        assertSameStack(helper, ((Container) furnace).getItem(2), oracle.get(POT), "blast furnace output");
        CompoundTag furnaceResaved = furnace.saveCustomOnly(helper.getLevel().registryAccess());
        helper.assertValueEqual(furnaceResaved.getIntOr(FurnaceTierPerks.BONUS_PROGRESS_KEY, -1), 5, "blast furnace bonus progress");
        helper.assertValueEqual(furnaceResaved.getIntOr("cooking_time_spent", -1), 40000, "blast furnace cooking timer (int, > short)");
        helper.assertValueEqual(furnaceResaved.getIntOr("lit_time_remaining", -1), 40001, "blast furnace fuel left (int, > short)");

        BlockEntity backpack = load(helper, BACKPACK_POS, ModBlocks.ENDERITE_BACKPACK.defaultBlockState(), blockEntities.get(3));
        DyedItemColor colour = backpack.collectComponents().get(DataComponents.DYED_COLOR);
        helper.assertValueEqual(colour == null ? -1 : colour.rgb(), BACKPACK_COLOUR, "placed backpack colour");
        assertBackpackContents(helper, backpack.collectComponents().get(ModDataComponentTypes.BACKPACK_CONTENTS), oracle,
                "placed backpack");
        helper.succeed();
    }

    /**
     * Mod items in a vanilla chest of a 26.2 chunk: octant corners, ore detector target,
     * blueprint code/signature/rotation, backpack contents and colour, trims with the mod's trim
     * material, mod and vanilla enchantments, glow upgrade, damage, the magnet's item filter and the
     * wand's settings all survive.
     *
     * <p>What breaks this: a mod item or component id changing between the lines, the ore
     * detector reading only one block-state spelling, backpack_contents entries not being fixed
     * (26.3: the explorer map entry is dropped).
     */
    public static void modItemsSurviveTheUpgrade(GameTestHelper helper) {
        List<CompoundTag> blockEntities = upgradedChunkBlockEntities(helper);
        List<ItemStack> oracle = chestOracle(helper, blockEntities.get(0));
        Container chest = (Container) load(helper, CHEST_POS, Blocks.CHEST.defaultBlockState(), blockEntities.get(0));

        helper.assertTrue(chest.getItem(BUNDLE).is(ModItems.REINFORCED_BUNDLE), "the reinforced bundle is gone");

        ItemStack octant = chest.getItem(OCTANT);
        helper.assertTrue(octant.is(ModItems.OCTANT), "octant slot holds " + octant);
        CompoundTag octantData = octant.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        helper.assertTrue(java.util.Arrays.equals(octantData.getIntArray("Pos1").orElse(new int[0]), new int[]{1, 2, 3})
                        && java.util.Arrays.equals(octantData.getIntArray("Pos2").orElse(new int[0]), new int[]{-4, 5, 60})
                        && octantData.getBooleanOr("Locked", false) && octantData.getBooleanOr("Hollow", false),
                "octant selection changed: " + octantData);

        ItemStack detector = chest.getItem(DETECTOR);
        helper.assertTrue(detector.is(ModItems.ORE_DETECTOR), "detector slot holds " + detector);
        helper.assertTrue(OreDetectorItem.targetColor(detector) != -1,
                "the ore detector lost its calibrated target: " + detector.get(DataComponents.CUSTOM_DATA));

        ItemStack blueprint = chest.getItem(BLUEPRINT);
        helper.assertValueEqual(blueprint.get(ModDataComponentTypes.BLUEPRINT), BLUEPRINT_CONTENT, "blueprint content");
        helper.assertValueEqual(blueprint.get(ModDataComponentTypes.BLUEPRINT_ROTATION), 2, "blueprint rotation");

        ItemStack backpack = chest.getItem(BACKPACK_ITEM);
        helper.assertTrue(backpack.is(ModItems.ENDERITE_BACKPACK), "backpack slot holds " + backpack);
        DyedItemColor colour = backpack.get(DataComponents.DYED_COLOR);
        helper.assertValueEqual(colour == null ? -1 : colour.rgb(), BACKPACK_COLOUR, "backpack item colour");
        assertBackpackContents(helper, backpack.get(ModDataComponentTypes.BACKPACK_CONTENTS), oracle, "backpack item");

        ItemStack chestplate = chest.getItem(CHESTPLATE);
        var trim = chestplate.get(DataComponents.TRIM);
        helper.assertTrue(trim != null && trim.material().is(ModTrimMaterials.ENDERITE)
                        && trim.pattern().unwrapKey().map(k -> k.identifier().toString()).orElse("").equals("minecraft:sentry"),
                "enderite chestplate trim changed: " + trim);
        helper.assertValueEqual(enchantmentLevel(helper, chestplate, "simplebuilding:kinetic_protection"), 2, "kinetic protection level");
        helper.assertValueEqual(enchantmentLevel(helper, chestplate, "minecraft:unbreaking"), 3, "unbreaking level");
        helper.assertValueEqual(chestplate.get(ModDataComponentTypes.GLOW_LEVEL), 3, "glow level");
        helper.assertValueEqual(chestplate.get(ModDataComponentTypes.VISUAL_GLOW), Boolean.TRUE, "visual glow");
        helper.assertValueEqual(chestplate.getDamageValue(), 11, "chestplate damage");

        ItemStack hammer = chest.getItem(SLEDGEHAMMER);
        helper.assertValueEqual(hammer.getDamageValue(), 17, "sledgehammer damage");
        helper.assertValueEqual(enchantmentLevel(helper, hammer, "simplebuilding:range"), 2, "sledgehammer range level");

        String filter = chest.getItem(MAGNET).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getStringOr("MagnetFilter", "");
        helper.assertValueEqual(filter, "minecraft:filled_map", "magnet filter");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.parse(filter)),
                "the magnet filters for an item that no longer exists: " + filter);

        ItemStack wand = chest.getItem(WAND);
        CompoundTag wandData = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        helper.assertValueEqual(wandData.getIntOr("SettingsRadius", -1), 3, "wand radius");
        helper.assertValueEqual(wandData.getIntOr("SettingsAxis", -1), 1, "wand axis");
        helper.assertValueEqual(enchantmentLevel(helper, wand, "simplebuilding:master_builder"), 1, "master builder level");
        helper.succeed();
    }

    /**
     * The rising block keeps its block (a 26.2 save names it under "Name"/"Properties", 26.3 reads
     * only "id"/"properties" - unfixed it becomes sand), and the locked item frame keeps its lock,
     * its owned light and its (renamed) map.
     */
    public static void modEntitiesSurviveTheUpgrade(GameTestHelper helper) {
        CompoundTag chunk = upgrade(helper, readFixture(helper, ENTITIES), DataFixTypes.ENTITY_CHUNK);
        ListTag entities = chunk.getListOrEmpty("Entities");
        helper.assertValueEqual(entities.size(), 2, "entities in the upgraded entity chunk");

        Entity rising = loadEntity(helper, entities.getCompoundOrEmpty(0));
        helper.assertTrue(rising instanceof LevitatingBlockEntity, "the rising block did not load: " + rising);
        helper.assertValueEqual(((LevitatingBlockEntity) rising).getBlockState(), STAIRS, "block of the rising block");

        Entity frame = loadEntity(helper, entities.getCompoundOrEmpty(1));
        helper.assertTrue(frame instanceof ItemFrame, "the item frame did not load: " + frame);
        CompoundTag frameResaved = saveEntity(helper, frame);
        helper.assertTrue(frameResaved.getBooleanOr("SimpleBuildingLocked", false), "the item frame lost its lock");
        helper.assertValueEqual(frameResaved.read("SimpleBuildingOwnedLight", BlockPos.CODEC).orElse(null), OWNED_LIGHT,
                "owned light of the item frame");
        helper.assertTrue(!((ItemFrame) frame).getItem().isEmpty(), "the item frame lost its map");
        helper.succeed();
    }

    /**
     * A 26.2 player: survival tracker values, the backpack in the inventory and the one worn on the
     * back keep their contents (the explorer map in them equals the one vanilla fixed in the plain
     * inventory slot next to them).
     */
    public static void playerDataSurvivesTheUpgrade(GameTestHelper helper) {
        CompoundTag fixed = upgrade(helper, readFixture(helper, PLAYER), DataFixTypes.PLAYER);
        ServerPlayer player = detachedPlayer(helper);
        player.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), fixed));

        SurvivalTracerAccessor tracker = (SurvivalTracerAccessor) player;
        helper.assertValueEqual(tracker.simplebuilding$getBaseDistance(), 11, "BaseDist");
        helper.assertValueEqual(tracker.simplebuilding$getBaseTime(), 22, "BaseTime");
        helper.assertValueEqual(tracker.simplebuilding$getBaseHostileKills(), 33, "BaseHostile");
        helper.assertValueEqual(tracker.simplebuilding$getBasePassiveKills(), 44, "BasePassive");
        helper.assertValueEqual(tracker.simplebuilding$getBaseDamageTaken(), 55, "BaseDamage");
        helper.assertValueEqual(tracker.simplebuilding$getBaseXp(), 66, "BaseXp");
        helper.assertValueEqual(tracker.simplebuilding$getCurrentTime(), 77, "ActiveTicks");

        ItemStack map = player.getInventory().getItem(0);
        helper.assertTrue(!map.isEmpty(), "vanilla itself lost the explorer map in the inventory - the oracle is broken");
        for (ItemStack backpack : List.of(player.getInventory().getItem(1), player.getItemBySlot(EquipmentSlot.CHEST))) {
            helper.assertTrue(backpack.is(ModItems.ENDERITE_BACKPACK), "backpack slot holds " + backpack);
            BackpackContents contents = backpack.get(ModDataComponentTypes.BACKPACK_CONTENTS);
            helper.assertTrue(contents != null && contents.size() == 4,
                    "the player's backpack has " + (contents == null ? "no" : contents.size()) + " entries instead of 4");
            assertSameStack(helper, contents.stackAt(0), map, "explorer map in the player's backpack");
        }
        helper.succeed();
    }

    /** The mod's saved-data files (blueprint jobs, sledgehammer progress) load unchanged. */
    public static void savedDataSurvivesTheUpgrade(GameTestHelper helper) {
        CompoundTag jobsFile = upgrade(helper, readFixture(helper, BLUEPRINT_JOBS), BlueprintJobs.TYPE.dataFixType());
        BlueprintJobs jobs = BlueprintJobs.CODEC.parse(ops(helper), jobsFile.get("data")).result().orElse(null);
        helper.assertTrue(jobs != null, "the blueprint jobs file does not decode: " + jobsFile);
        helper.assertValueEqual(jobs.pending(), List.of(PENDING_JOB), "pending blueprint jobs");

        CompoundTag hammerFile = upgrade(helper, readFixture(helper, SLEDGEHAMMER_PROGRESS), SledgehammerProgress.TYPE.dataFixType());
        SledgehammerProgress hammer = SledgehammerProgress.CODEC.parse(ops(helper), hammerFile.get("data")).result().orElse(null);
        helper.assertTrue(hammer != null, "the sledgehammer progress file does not decode: " + hammerFile);
        helper.assertValueEqual(hammer.entries(), List.of(HAMMER_ENTRY), "sledgehammer progress entries");
        helper.succeed();
    }

    // =====================================================================================
    // TOLERANT READERS (every line)
    // =====================================================================================

    /**
     * A wand that was mid-build when an older version saved the world stored its block as a
     * numeric registry id; 26.3 shifts those numbers. The wand stops instead of building on with
     * whatever block now has that number.
     *
     * <p>What breaks this: dropping the legacy check in {@code BuildingWandItem#inventoryTick}
     * (the build goes on, from air or a wrong block), or writing numeric ids again.
     */
    public static void wandMidBuildFromAnOlderVersionStopsInsteadOfBuildingAShiftedBlock(GameTestHelper helper) {
        ServerPlayer player = detachedPlayer(helper);
        ItemStack wand = new ItemStack(ModItems.ENDERITE_BUILDING_WAND);
        CompoundTag data = new CompoundTag();
        data.putBoolean("Active", true);
        data.putInt("Timer", 0);
        data.putInt("BuildBlockRawId", 1);
        data.putInt("CoverBlockRawId", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
        wand.getItem().inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);

        CompoundTag after = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        helper.assertTrue(!after.getBooleanOr("Active", true), "a legacy mid-build wand kept building: " + after);
        helper.assertTrue(!after.contains("BuildBlockRawId") && !after.contains("CoverBlockRawId"),
                "the legacy numeric block ids were kept: " + after);
        helper.succeed();
    }

    /**
     * One backpack entry that no longer decodes (an item that does not exist) costs that entry, not
     * the whole backpack - before, the list failed as a whole and the backpack opened empty.
     */
    public static void backpackWithAnUnreadableEntryKeepsTheRest(GameTestHelper helper) {
        CompoundTag good = new CompoundTag();
        good.putInt("slot", 0);
        good.putString("id", "minecraft:cobblestone");
        good.putInt("count", DEEP_POCKET_COUNT);
        CompoundTag bad = good.copy();
        bad.putInt("slot", 1);
        bad.putString("id", "simplebuilding:item_that_never_existed");
        ListTag list = new ListTag();
        list.add(bad);
        list.add(good);
        BackpackContents contents = BackpackContents.CODEC.parse(ops(helper), list).result().orElse(null);
        helper.assertTrue(contents != null && contents.size() == 1, "expected the one readable entry, got " + contents);
        helper.assertValueEqual(contents.stackAt(0).getCount(), DEEP_POCKET_COUNT, "count of the readable entry");
        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    static int currentDataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    private static RegistryOps<Tag> ops(GameTestHelper helper) {
        return helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
    }

    private static ItemStack parseStack(GameTestHelper helper, String snbt) {
        try {
            return ItemStack.CODEC.parse(ops(helper), TagParser.parseCompoundFully(snbt))
                    .getOrThrow(error -> helper.assertionException("stack " + snbt + " does not decode: " + error));
        } catch (CommandSyntaxException e) {
            throw helper.assertionException("bad SNBT " + snbt + ": " + e.getMessage());
        }
    }

    private static ServerPlayer detachedPlayer(GameTestHelper helper) {
        Player raw = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) {
            throw helper.assertionException("makeMockServerPlayer returned " + raw.getClass().getName());
        }
        return player;
    }

    /** Sends a fixture through the vanilla fixer (plus ModDataFixer) from its data version to this one. */
    private static CompoundTag upgrade(GameTestHelper helper, CompoundTag fixture, DataFixTypes type) {
        int from = fixture.getIntOr("DataVersion", -1);
        helper.assertValueEqual(from, DATA_VERSION_26_2, "DataVersion of the fixture");
        DataFixer fixer = DataFixers.getDataFixer();
        CompoundTag copy = fixture.copy();
        copy.remove("DataVersion");
        return type.update(fixer, copy, from, currentDataVersion());
    }

    /** The block entities of the upgraded fixture chunk, in the order chest, hopper, furnace, backpack. */
    private static List<CompoundTag> upgradedChunkBlockEntities(GameTestHelper helper) {
        CompoundTag chunk = upgrade(helper, readFixture(helper, CHUNK), DataFixTypes.CHUNK);
        ListTag list = chunk.getListOrEmpty("block_entities");
        helper.assertValueEqual(list.size(), 4, "block entities in the upgraded chunk");
        List<CompoundTag> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getCompoundOrEmpty(i));
        }
        return result;
    }

    /** The three vanilla stacks as vanilla upgraded them in the chest: the expectation for mod storage. */
    private static List<ItemStack> chestOracle(GameTestHelper helper, CompoundTag chestTag) {
        Container chest = (Container) load(helper, CHEST_POS, Blocks.CHEST.defaultBlockState(), chestTag);
        List<ItemStack> oracle = List.of(chest.getItem(MAP), chest.getItem(POT), chest.getItem(BUNDLE));
        for (int i = 0; i < oracle.size(); i++) {
            helper.assertTrue(!oracle.get(i).isEmpty(), "vanilla itself lost chest slot " + i + " - the oracle is broken");
        }
        return oracle;
    }

    /** Loads an entity the way a chunk does: the type comes from the saved id. */
    private static Entity loadEntity(GameTestHelper helper, CompoundTag tag) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(
                net.minecraft.resources.Identifier.tryParse(tag.getStringOr("id", ""))).orElse(null);
        helper.assertTrue(type != null, "unknown entity id in " + tag);
        return EntityType.create(type, TagValueInput.create(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), tag), helper.getLevel(), EntitySpawnReason.LOAD).orElse(null);
    }

    private static BlockEntity load(GameTestHelper helper, BlockPos pos, BlockState state, CompoundTag tag) {
        BlockEntity be = BlockEntity.loadStatic(pos, state, tag, helper.getLevel().registryAccess());
        helper.assertTrue(be != null, "block entity " + tag.getStringOr("id", "?") + " did not load");
        return be;
    }

    private static void assertSameStack(GameTestHelper helper, ItemStack actual, ItemStack expected, String what) {
        helper.assertTrue(!actual.isEmpty() && ItemStack.matches(actual, expected),
                what + ": expected " + expected + " " + expected.getComponentsPatch() + ", got " + actual + " "
                        + actual.getComponentsPatch());
    }

    private static void assertBackpackContents(GameTestHelper helper, BackpackContents contents, List<ItemStack> oracle,
                                               String what) {
        helper.assertTrue(contents != null && contents.size() == 4,
                what + " has " + (contents == null ? "no contents" : contents.size() + " entries") + " instead of 4");
        for (int i = 0; i < oracle.size(); i++) {
            helper.assertValueEqual(contents.entries().get(i).slot(), i, what + " entry " + i + " slot");
            assertSameStack(helper, contents.stackAt(i), oracle.get(i), what + " entry " + i);
        }
        ItemStack deep = contents.stackAt(3);
        helper.assertTrue(deep.is(Blocks.COBBLESTONE.asItem()) && deep.getCount() == DEEP_POCKET_COUNT,
                what + " deep-pocket stack is " + deep);
        helper.assertValueEqual(contents.entries().get(3).slot(), 7, what + " deep-pocket slot");
    }

    private static int enchantmentLevel(GameTestHelper helper, ItemStack stack, String id) {
        var enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) {
            return 0;
        }
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().unwrapKey().map(k -> k.identifier().toString()).orElse("").equals(id)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    /** Path of the first key/value in {@code expected} that {@code actual} lacks or has different; null if none. */
    static String firstMismatch(String path, Tag expected, Tag actual) {
        if (expected instanceof CompoundTag e) {
            if (!(actual instanceof CompoundTag a)) {
                return path + " is " + actual + ", expected a compound";
            }
            for (String key : e.keySet()) {
                if (!a.contains(key)) {
                    return path + "/" + key + " is missing";
                }
                String inner = firstMismatch(path + "/" + key, e.get(key), a.get(key));
                if (inner != null) {
                    return inner;
                }
            }
            return null;
        }
        if (expected instanceof ListTag e) {
            if (!(actual instanceof ListTag a) || a.size() != e.size()) {
                return path + " is " + actual + ", expected " + expected;
            }
            for (int i = 0; i < e.size(); i++) {
                String inner = firstMismatch(path + "[" + i + "]", e.get(i), a.get(i));
                if (inner != null) {
                    return inner;
                }
            }
            return null;
        }
        return expected.equals(actual) ? null : path + " is " + actual + ", expected " + expected;
    }

    static Path fixtureDir(GameTestHelper helper) {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("testing"))) {
                return dir.resolve(FIXTURE_DIR);
            }
            dir = dir.getParent();
        }
        throw helper.assertionException("no testing/ directory above " + Path.of("").toAbsolutePath());
    }

    private static CompoundTag readFixture(GameTestHelper helper, String name) {
        Path file = fixtureDir(helper).resolve(name);
        try {
            return TagParser.parseCompoundFully(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | CommandSyntaxException e) {
            throw helper.assertionException("cannot read fixture " + file + ": " + e.getMessage());
        }
    }
}
