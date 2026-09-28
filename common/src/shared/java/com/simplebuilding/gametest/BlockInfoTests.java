package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.compat.BlockInfo;
import com.simplebuilding.compat.BlockInfo.Line;
import com.simplebuilding.compat.BlockInfo.Topic;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * What the optional Jade plugin shows, read from {@link BlockInfo} - the Jade-independent half of
 * it. Each test builds the block in the world, sets the state through the block entity's own
 * setters and compares the lines as "key [arg, arg]" strings (translation keys stay keys), so a
 * wrong value, a missing line or a line too many all show in the message.
 *
 * <p>The expected numbers are this file's own constants (4 charges for launchpad I, 36/45 slots,
 * 2x/4x/8x furnace speed), not read from the mod.
 */
public final class BlockInfoTests {

    private BlockInfoTests() {
    }

    /** Launchpad I holds 4; after loading 3 the tooltip says "3 / 4", and the owner line is empty without an owner. */
    public static void launchpadShowsItsChargesAgainstItsCapacity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TweaksBlocks.LAUNCHPAD);
        LaunchpadBlockEntity pad = helper.getBlockEntity(pos, LaunchpadBlockEntity.class);
        expect(helper, BlockInfo.serverLines(Topic.PAD_STATUS, pad), List.of("jade.simplebuilding.launchpad.charges [0, 4]"), "empty launchpad I");
        pad.addCharges(3, 4);
        expect(helper, BlockInfo.serverLines(Topic.PAD_STATUS, pad), List.of("jade.simplebuilding.launchpad.charges [3, 4]"), "launchpad I with 3 charges");
        expect(helper, BlockInfo.serverLines(Topic.OWNER, pad), List.of(), "owner line of a launchpad nobody owns");
        helper.setBlock(pos, TweaksBlocks.ENDERITE_LAUNCHPAD);
        LaunchpadBlockEntity enderite = helper.getBlockEntity(pos, LaunchpadBlockEntity.class);
        expect(helper, BlockInfo.serverLines(Topic.PAD_STATUS, enderite), List.of("jade.simplebuilding.launchpad.charges [0, 16]"), "empty enderite launchpad III");
        helper.succeed();
    }

    /**
     * The owner's name: the online player's, else the server's name cache, else the UUID. A pad
     * without owner gets no line.
     */
    @SuppressWarnings("removal")
    public static void padOwnerIsNamedByTheServer(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TweaksBlocks.POTION_PAD);
        PotionPadBlockEntity pad = helper.getBlockEntity(pos, PotionPadBlockEntity.class);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        pad.setOwner(player.getUUID());
        expect(helper, BlockInfo.serverLines(Topic.OWNER, pad), List.of("jade.simplebuilding.owner [" + player.getName().getString() + "]"), "owner line for an online owner");

        UUID cached = UUID.randomUUID();
        helper.getLevel().getServer().services().nameToIdCache().add(new NameAndId(cached, "CachedOwner"));
        pad.setOwner(cached);
        expect(helper, BlockInfo.serverLines(Topic.OWNER, pad), List.of("jade.simplebuilding.owner [CachedOwner]"), "owner line for an offline owner the server knows");

        UUID stranger = UUID.randomUUID();
        pad.setOwner(stranger);
        expect(helper, BlockInfo.serverLines(Topic.OWNER, pad), List.of("jade.simplebuilding.owner [" + stranger + "]"), "owner line for an unknown owner");

        pad.setOwner(null);
        expect(helper, BlockInfo.serverLines(Topic.OWNER, pad), List.of(), "owner line without an owner");
        helper.succeed();
    }

    /** Potion pad: nothing stored, then Swiftness II (speed at potency 1) with 45 ticks = 3 s of cooldown, then ready. */
    public static void potionPadShowsItsPotionAndCooldown(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TweaksBlocks.POTION_PAD);
        PotionPadBlockEntity pad = helper.getBlockEntity(pos, PotionPadBlockEntity.class);
        expect(helper, BlockInfo.serverLines(Topic.PAD_STATUS, pad),
                List.of("jade.simplebuilding.potion_pad.empty []", "jade.simplebuilding.potion_pad.ready []"), "empty potion pad");
        pad.setStored(new PotionContents(Potions.STRONG_SWIFTNESS));
        pad.setCooldown(45);
        expect(helper, BlockInfo.serverLines(Topic.PAD_STATUS, pad),
                List.of("jade.simplebuilding.potion_pad.effect [effect.minecraft.speed potion.potency.1]", "jade.simplebuilding.potion_pad.cooldown [3]"),
                "potion pad with Swiftness II cooling down");
        pad.setCooldown(0);
        expect(helper, BlockInfo.serverLines(Topic.PAD_STATUS, pad),
                List.of("jade.simplebuilding.potion_pad.effect [effect.minecraft.speed potion.potency.1]", "jade.simplebuilding.potion_pad.ready []"),
                "potion pad with Swiftness II, ready");
        helper.succeed();
    }

    /**
     * Chunk loader: before its first update it holds nothing; the enderite loader then forces its
     * 3x3 area, and the tooltip counts exactly the chunks it claims (chunks the test world had
     * forced before stay foreign). Breaking it releases them again.
     */
    public static void chunkLoaderShowsHowManyChunksItHolds(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TweaksBlocks.ENDERITE_CHUNK_LOADER);
        ChunkLoaderBlockEntity loader = helper.getBlockEntity(pos, ChunkLoaderBlockEntity.class);
        if (loader.ownForced().isEmpty()) {
            expect(helper, BlockInfo.serverLines(Topic.PAD_STATUS, loader), List.of("jade.simplebuilding.chunk_loader.idle []"), "chunk loader before its first update");
        }
        loader.update(helper.getLevel());
        int held = loader.ownForced().size();
        helper.assertTrue(held > 0 && held <= 9, "the enderite chunk loader claims " + held + " chunks, expected 1..9");
        expect(helper, BlockInfo.serverLines(Topic.PAD_STATUS, loader), List.of("jade.simplebuilding.chunk_loader.active [" + held + "]"), "chunk loader holding its chunks");
        helper.setBlock(pos, Blocks.AIR);
        helper.succeed();
    }

    /** Netherite piston: full durability new, 10 less after 10 damage; the reinforced piston has none and gets no line. */
    public static void pistonDurabilityFollowsTheBlockState(GameTestHelper helper) {
        BlockState fresh = ModBlocks.NETHERITE_PISTON.defaultBlockState();
        int max = NetheriteBreakerPistonBlock.maxDurabilityOf(fresh);
        helper.assertTrue(max > 10, "the netherite piston has a durability of " + max);
        expect(helper, BlockInfo.stateLines(Topic.PISTON_DURABILITY, fresh),
                List.of("tooltip.simplebuilding.netherite_piston.durability [" + max + ", " + max + "]"), "new netherite piston");
        BlockState worn = NetheriteBreakerPistonBlock.withDamage(fresh, 10);
        expect(helper, BlockInfo.stateLines(Topic.PISTON_DURABILITY, worn),
                List.of("tooltip.simplebuilding.netherite_piston.durability [" + (max - 10) + ", " + max + "]"), "netherite piston after 10 damage");
        expect(helper, BlockInfo.stateLines(Topic.PISTON_DURABILITY, ModBlocks.REINFORCED_PISTON.defaultBlockState()), List.of(), "reinforced piston");
        helper.succeed();
    }

    /** Reinforced chest 36 slots, no stack bonus; netherite 45, doubled 90, stacks x2. */
    public static void chestSlotsFollowTheTierAndDoubleChests(GameTestHelper helper) {
        expect(helper, BlockInfo.stateLines(Topic.CHEST_SLOTS, ModBlocks.REINFORCED_CHEST.defaultBlockState()),
                List.of("jade.simplebuilding.chest.slots [36]"), "single reinforced chest");
        BlockState netherite = ModBlocks.NETHERITE_CHEST.defaultBlockState();
        expect(helper, BlockInfo.stateLines(Topic.CHEST_SLOTS, netherite),
                List.of("jade.simplebuilding.chest.slots [45]", "jade.simplebuilding.chest.stacks [2]"), "single netherite chest");
        expect(helper, BlockInfo.stateLines(Topic.CHEST_SLOTS, netherite.setValue(ChestBlock.TYPE, ChestType.LEFT)),
                List.of("jade.simplebuilding.chest.slots [90]", "jade.simplebuilding.chest.stacks [2]"), "netherite double chest");
        expect(helper, BlockInfo.stateLines(Topic.CHEST_SLOTS, Blocks.CHEST.defaultBlockState()), List.of(), "vanilla chest");
        helper.succeed();
    }

    /** Mod hopper: filter off; exact match without ghost items; then the names of the ghost items in slot order. */
    public static void hopperFilterNamesTheModeAndItems(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.NETHERITE_HOPPER);
        ModHopperBlockEntity hopper = helper.getBlockEntity(pos, ModHopperBlockEntity.class);
        expect(helper, BlockInfo.serverLines(Topic.HOPPER_FILTER, hopper),
                List.of("jade.simplebuilding.hopper.mode [simplebuilding.hopper_filter.none]"), "hopper without filter");
        hopper.toggleFilterMode();
        expect(helper, BlockInfo.serverLines(Topic.HOPPER_FILTER, hopper),
                List.of("jade.simplebuilding.hopper.mode [simplebuilding.hopper_filter.whitelist]", "jade.simplebuilding.hopper.nothing []"),
                "exact-match hopper without ghost items");
        hopper.setGhostItem(0, new ItemStack(Items.STONE));
        hopper.setGhostItem(2, new ItemStack(Items.DIAMOND, 5));
        hopper.toggleFilterMode();
        expect(helper, BlockInfo.serverLines(Topic.HOPPER_FILTER, hopper),
                List.of("jade.simplebuilding.hopper.mode [simplebuilding.hopper_filter.type]",
                        "jade.simplebuilding.hopper.items [block.minecraft.stone, item.minecraft.diamond]"),
                "type-match hopper with two ghost items");
        helper.succeed();
    }

    /** Reinforced 2x, netherite 4x, enderite 8x the vanilla machine, in all three families; vanilla gets no line. */
    public static void furnaceSpeedFollowsTheTier(GameTestHelper helper) {
        expect(helper, BlockInfo.stateLines(Topic.FURNACE_SPEED, ModBlocks.REINFORCED_FURNACE.defaultBlockState()),
                List.of("jade.simplebuilding.furnace.speed [2]"), "reinforced furnace");
        expect(helper, BlockInfo.stateLines(Topic.FURNACE_SPEED, ModBlocks.NETHERITE_SMOKER.defaultBlockState()),
                List.of("jade.simplebuilding.furnace.speed [4]"), "netherite smoker");
        expect(helper, BlockInfo.stateLines(Topic.FURNACE_SPEED, ModBlocks.ENDERITE_BLAST_FURNACE.defaultBlockState()),
                List.of("jade.simplebuilding.furnace.speed [8]"), "enderite blast furnace");
        expect(helper, BlockInfo.stateLines(Topic.FURNACE_SPEED, Blocks.FURNACE.defaultBlockState()), List.of(), "vanilla furnace");
        helper.succeed();
    }

    /** The server data tag carries the lines unchanged (including translated pieces), per topic, and nothing for no lines. */
    public static void linesSurviveTheServerDataTag(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TweaksBlocks.POTION_PAD);
        PotionPadBlockEntity pad = helper.getBlockEntity(pos, PotionPadBlockEntity.class);
        pad.setStored(new PotionContents(Potions.STRONG_SWIFTNESS));
        pad.setCooldown(45);
        List<Line> status = BlockInfo.serverLines(Topic.PAD_STATUS, pad);
        CompoundTag data = new CompoundTag();
        BlockInfo.write(data, Topic.PAD_STATUS, status);
        BlockInfo.write(data, Topic.OWNER, BlockInfo.serverLines(Topic.OWNER, pad));
        helper.assertTrue(BlockInfo.read(data, Topic.PAD_STATUS).equals(status),
                "the pad status came back as " + BlockInfo.read(data, Topic.PAD_STATUS) + " instead of " + status);
        helper.assertTrue(BlockInfo.read(data, Topic.OWNER).isEmpty(), "an empty owner topic came back as " + BlockInfo.read(data, Topic.OWNER));
        helper.assertFalse(data.contains(Topic.OWNER.dataKey()), "an empty topic still wrote its key into the data tag");
        helper.assertTrue(BlockInfo.read(data, Topic.HOPPER_FILTER).isEmpty(), "a topic that was never written came back as " + BlockInfo.read(data, Topic.HOPPER_FILTER));
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, List<Line> lines, List<String> expected, String what) {
        List<String> actual = new ArrayList<>();
        for (Line line : lines) {
            actual.add(line.key() + " " + line.argTexts());
        }
        helper.assertTrue(actual.equals(expected), what + ": expected " + expected + ", was " + actual);
    }
}
