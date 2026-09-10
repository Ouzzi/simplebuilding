package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.networking.SetHopperGhostItemPayload;
import com.simplebuilding.platform.HopperSync;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.screen.ModHopperScreenHandler;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The reinforced and the netherite hopper: the redstone lock, the item filter behind the five
 * ghost slots, the menu that configures it, and the registration data the two blocks are built
 * from.
 *
 * <h2>What is already covered elsewhere, and is not repeated here</h2>
 * <ul>
 *   <li><b>The three filter modes as a gate</b> -
 *       {@code HopperAndTrimTests#hopperFilterModesGateWhatMayEnter} drives
 *       {@code canPlaceItem} through Disabled / Exact Match / Type Match. This file only reaches
 *       for {@code canPlaceItem} where it is the <em>proof</em> that something else worked (a
 *       reloaded filter, a filter written through the property delegate, a ghost the hopper
 *       taught itself).</li>
 *   <li><b>The two payload handlers</b> -
 *       {@code HopperAndTrimTests#hopperPayloadsOnlyActOnAnOpenHopperMenu} pins that both refuse
 *       to act unless that hopper's menu is open. What is picked up below is the wire format of
 *       {@code SetHopperGhostItemPayload}, which no test encoded before.</li>
 *   <li><b>The shorter transfer cooldown</b> -
 *       {@code BlockBehaviourTests#reinforcedAndNetheriteHoppersMoveItemsFasterThanVanilla} times
 *       all three tiers against each other. The test below uses the same rig for the opposite
 *       question: a hopper that must move <em>nothing</em>.</li>
 *   <li><b>Registration and loot tables</b> - {@code DataIntegrityTests} proves every mod block
 *       is registered, owns a matching {@code BlockItem} and resolves a loot table that loaded.
 *       Below, the two hoppers are actually broken so the drop is observed rather than the
 *       table's existence.</li>
 * </ul>
 *
 * <h2>Two rules the tests here follow</h2>
 * <ul>
 *   <li><b>The redstone rig checks itself.</b> {@code HopperBlock#onPlace} recomputes
 *       {@code ENABLED} from the neighbour signal, so a hand-set {@code ENABLED=false} is
 *       overwritten in the same call that placed the block. The lock test therefore powers a real
 *       redstone block and reads {@code ENABLED} back before it asserts anything about items. A
 *       failure there says the rig broke, not the mod.</li>
 *   <li><b>Vanilla is the yardstick where vanilla has one.</b> The reinforced hopper's hardness
 *       and blast resistance are compared against a vanilla hopper measured in the same breath,
 *       and its fire resistance against a netherite ingot's, instead of against the numbers in
 *       {@code ModBlocks}.</li>
 * </ul>
 *
 * <h2>Known defects</h2>
 * <ul>
 *   <li><b>{@code NetheriteHopperBlockEntity} is dead code.</b> Nothing constructs it:
 *       {@code ModHopperBlock#newBlockEntity} returns a {@code ModHopperBlockEntity} and
 *       {@code ModBlockEntities} registers {@code ModHopperBlockEntity::new} for both hoppers.
 *       It re-declares {@code ghostItems}, {@code currentFilterMode} and {@code propertyDelegate},
 *       which would shadow the base class's fields if it were ever wired up - the inherited
 *       {@code canPlaceItem}, {@code saveAdditional} and {@code getUpdateTag} would then read the
 *       empty base copies. Every test here uses the class the game actually builds.</li>
 * </ul>
 *
 * <h2>Not pinned, and why</h2>
 * <ul>
 *   <li><b>{@code requiresCorrectToolForDrops}.</b> Both hoppers are built from
 *       {@code Properties.ofFullCopy(Blocks.GLASS)} (ModBlocks.java:99), so unlike vanilla's
 *       hopper they drop themselves to a bare hand; the {@code mineable/pickaxe} entry only makes
 *       a pickaxe faster. That is consistent with the generated tags (neither block is in a
 *       {@code needs_*_tool} tag), so it reads as a decision rather than a defect and an
 *       assertion on it would go red if it were ever tightened. The tag membership itself is
 *       pinned; the tool requirement is not.</li>
 *   <li><b>The {@code TransferCooldown} key of the save round trip.</b>
 *       {@code saveAdditional} writes it and {@code loadAdditional} reads it back
 *       (ModHopperBlockEntity.java:177, 197), but nothing here can see whether the two still
 *       agree. {@code transferCooldown} is private with no getter (line 50) - the only two
 *       methods that touch it, {@code setTransferCooldown} and {@code needsCooldown}
 *       (lines 399-400), are private as well - so a reloaded block entity cannot be asked what
 *       came back. Even with a getter the round trip below would say nothing: nothing in it
 *       ticks the hopper, so the value saved is the untouched {@code -1}, which is exactly what
 *       {@code view.getIntOr("TransferCooldown", -1)} hands back when the key is missing
 *       altogether. A key renamed on one side only would pass, even though every reloaded hopper
 *       would then come back with no cooldown left to run down. Reading it means adding a getter
 *       to the mod, which is not this file's call to make.</li>
 * </ul>
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>{@code level.sendBlockUpdated} inside {@code updateListeners}</b>
 *       (ModHopperBlockEntity.java:138), i.e. the <em>push</em> that hands the update packet to
 *       the tracking clients. All it does on a server level is call
 *       {@code ServerChunkCache#blockChanged}, whose {@code ChunkHolder} is behind a protected
 *       lookup, and both block states it is given are the same one, so nothing else it touches
 *       moves. What the tests below do reach is the two halves around it: {@code setChanged()}
 *       through the chunk's unsaved flag, and the packet itself through
 *       {@code getUpdatePacket()}.</li>
 *   <li><b>Creative tab membership</b> ({@code ModItemGroupsContent}). Same reasoning
 *       {@code BundleWiringTests} records: a test could only assert that two
 *       {@code entries.accept(...)} lines still exist, which restates the source line it
 *       guards.</li>
 *   <li><b>Everything the player sees.</b> {@code NetheriteHopperScreen}, the filter button and
 *       the ghost item overlay are client side. The mode texts and the colour their tooltip is
 *       drawn in live in shared code and are pinned below - as the {@code Style} on the component
 *       the screen hands to {@code setTooltipForNextFrame}, which is the colour that actually
 *       reaches the player - but nothing draws them here.</li>
 *   <li><b>{@code SyncHopperGhostItemPayload} arriving at a client.</b> The mock player's
 *       connection swallows the packet. That the payload is handed out at all is pinned below,
 *       one step earlier, at the {@code HopperSync} the loaders install.</li>
 * </ul>
 */
public final class HopperTests {

    private HopperTests() {
    }

    /** Tick budget for {@link #redstonePowerStopsEveryHopperTransfer}. */
    public static final int REDSTONE_LOCK_MAX_TICKS = 100;

    /** Tick budget for {@link #bothHoppersDropThemselvesWhenBroken}. */
    public static final int HOPPER_DROP_MAX_TICKS = 60;

    /** The single hopper the filter tests configure. */
    private static final BlockPos HOPPER_POS = new BlockPos(2, 1, 2);

    /** Where the mock player stands: clear of every block any test places. */
    private static final Vec3 PLAYER_POS = new Vec3(5.5D, 1.0D, 2.5D);

    // --- the redstone lock rig -------------------------------------------------------------
    private static final BlockPos CONTROL_HOPPER = new BlockPos(1, 2, 1);
    private static final BlockPos LOCKED_HOPPER = new BlockPos(5, 2, 5);
    private static final BlockPos POWER_SOURCE = new BlockPos(4, 2, 5);

    /** How much every source chest starts with, and therefore what a locked one must still hold. */
    private static final int SOURCE_STACK = 64;

    /** Items the control stack has to deliver before the locked stack is judged. */
    private static final int DELIVERED_BEFORE_THE_VERDICT = 5;

    // --- the two hoppers the drop test breaks ----------------------------------------------
    private static final BlockPos REINFORCED_DROP_POS = new BlockPos(1, 1, 1);
    private static final BlockPos NETHERITE_DROP_POS = new BlockPos(5, 1, 5);

    /** Search radius for a dropped block; the two sites above are five blocks apart. */
    private static final double DROP_RADIUS = 1.5D;

    /** First player inventory slot of a {@code HopperMenu}: five hopper slots come before it. */
    private static final int FIRST_PLAYER_SLOT = 5;

    /** The hotbar slot {@link #hopperMenuOpensOnUseAndFilterClicksNeverStoreTheItem} swaps from. */
    private static final int HOTBAR_SWAP_SLOT = 0;

    // --- the grid alignment probe: a hopper of its own, clear of HOPPER_POS and of the player ---
    private static final BlockPos GRID_PROBE_POS = new BlockPos(5, 1, 5);

    /**
     * Where the loose item for the grid alignment probe is dropped: inside the probe hopper's own
     * cell, under the block that caps it, and inside {@code Hopper.SUCK_AABB}, which reaches from
     * 0.6875 above the hopper's floor to two blocks over it.
     */
    private static final Vec3 GRID_PROBE_ITEM = new Vec3(5.5D, 1.8D, 5.5D);

    // =====================================================================================
    // THE REDSTONE LOCK
    // =====================================================================================

    /**
     * A powered hopper moves nothing - it neither pushes into the container below nor pulls from
     * the one above. {@code insertAndExtract} guards both halves with
     * {@code state.getValue(HopperBlock.ENABLED)} (ModHopperBlockEntity.java:308), including the
     * {@code suckInItems} supplier, so a locked hopper's own five slots have to stay empty as
     * well.
     *
     * <p>Two identical chest / hopper / chest stacks are built, one of them next to a redstone
     * block. The verdict is only taken once the unpowered stack has delivered
     * {@value #DELIVERED_BEFORE_THE_VERDICT} items: the two stacks are the same build, so if the
     * lock stopped working the powered one would be at the same count by then, and every
     * assertion in the verdict block would fail at once.
     *
     * <p>The power comes from a real redstone block rather than from a hand set block state.
     * {@code HopperBlock#onPlace} recomputes {@code ENABLED} from {@code hasNeighborSignal}, so a
     * hopper placed with {@code ENABLED=false} and no redstone around it comes back enabled in
     * the very same call - a hand set state would have made this test green for the wrong reason.
     * Both hoppers are therefore placed <em>enabled</em>, and the rig check below reads the
     * property back to prove the redstone, not the placement, turned one of them off.
     *
     * <p>What breaks this test: dropping the {@code ENABLED} check from
     * {@code insertAndExtract}, or moving it so it only guards {@code insert} and leaves the
     * pull from above running.
     */
    public static void redstonePowerStopsEveryHopperTransfer(GameTestHelper helper) {
        buildHopperStack(helper, CONTROL_HOPPER);
        // The power goes down first: the hopper placed on top of it then computes ENABLED itself.
        helper.setBlock(POWER_SOURCE, Blocks.REDSTONE_BLOCK);
        buildHopperStack(helper, LOCKED_HOPPER);

        // --- the rig checks itself before it judges anything ---
        helper.assertTrue(helper.getBlockState(CONTROL_HOPPER).getValue(HopperBlock.ENABLED),
                "the unpowered hopper came up disabled, so the comparison below is meaningless");
        helper.assertFalse(helper.getBlockState(LOCKED_HOPPER).getValue(HopperBlock.ENABLED),
                "the redstone block next to the hopper did not disable it, so this test is not "
                        + "measuring the lock at all");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        countItems(helper, CONTROL_HOPPER.below()) >= DELIVERED_BEFORE_THE_VERDICT,
                        "the unpowered control hopper should have delivered "
                                + DELIVERED_BEFORE_THE_VERDICT + " items, it delivered "
                                + countItems(helper, CONTROL_HOPPER.below())))
                .thenExecute(() -> {
                    helper.assertValueEqual(countItems(helper, LOCKED_HOPPER.below()), 0,
                            "items a powered hopper pushed into the chest below it");
                    helper.assertValueEqual(countItems(helper, LOCKED_HOPPER.above()), SOURCE_STACK,
                            "items left in the chest above a powered hopper");

                    ModHopperBlockEntity locked =
                            helper.getBlockEntity(LOCKED_HOPPER, ModHopperBlockEntity.class);
                    helper.assertTrue(locked.isEmpty(),
                            "a powered hopper pulled items into its own slots, so the ENABLED check "
                                    + "no longer covers the pull from above");

                    // And the lock is still on, so the zeroes above are not a hopper that simply
                    // lost its power halfway through the run.
                    helper.assertFalse(helper.getBlockState(LOCKED_HOPPER).getValue(HopperBlock.ENABLED),
                            "the hopper re-enabled itself during the run");
                })
                .thenSucceed();
    }

    // =====================================================================================
    // THE FIVE FILTER SLOTS
    // =====================================================================================

    /**
     * What a filter slot stores, and what it does with the two inputs it can be given from the
     * network: an empty stack, and a slot index outside {@code 0..4}.
     *
     * <p>Three separate claims. <b>Count one:</b> {@code setGhostItemInternal} stores
     * {@code stack.copy()} with {@code setCount(1)} (ModHopperBlockEntity.java:126-128), so a
     * seventeen item stack leaves a single item behind - what the slot holds is a placeholder,
     * not stock. <b>A copy:</b> later edits to the stack the caller kept must not reach into the
     * hopper, which is what separates {@code copy()} from storing the reference. <b>Empty
     * clears:</b> an empty stack is the delete path (line 123).
     *
     * <p>The delete path is driven through the payload's own stream codec rather than through a
     * plain method call, because that is where it can actually break: {@code ItemStack.CODEC} has
     * two stream variants and only {@code OPTIONAL_STREAM_CODEC} accepts an empty stack -
     * {@code ItemStack.STREAM_CODEC} throws {@code EncoderException("Empty ItemStack not
     * allowed")}. {@code SetHopperGhostItemPayload} picks the optional one on purpose; encoding
     * an empty payload here is what holds that choice in place.
     *
     * <p>The slot index arrives from a client and is never validated before it reaches the block
     * entity, so both ends are driven with 9 and -1 and the whole five slot row is re-read
     * afterwards.
     *
     * <p><b>And the fourth claim: the change leaves the server.</b> Storing the filter is only
     * half of {@code setGhostItem} - the {@code PlatformServices.broadcastHopperGhostItem} call
     * after it (ModHopperBlockEntity.java:408) is what a second player with the same hopper open,
     * or anyone with the block in view, learns the new filter from. Delete it and every field on
     * the server still reads right, so it is observed where it is loader neutral: at the
     * {@code HopperSync} the loaders install, wrapped by a recorder (see
     * {@link #recordGhostBroadcasts}). Both directions are asked for - setting a filter and
     * clearing one - because a broadcast that only fires for non-empty stacks leaves a cleared
     * slot showing its old ghost on every other screen.
     *
     * <p>The out of range slot is asked for on both sides of {@code setGhostItem}: it stores
     * nothing, and it announces nothing. The second half is the one a client controls - the index
     * arrives from {@code SetHopperGhostItemPayload} unclamped - so a broadcast that fires for a
     * slot the hopper refused to write turns one packet into one per tracking player.
     *
     * <p>What breaks this test: dropping the {@code setCount(1)}, storing {@code stack} instead
     * of {@code stack.copy()}, losing the empty-stack branch, swapping the payload to
     * {@code ItemStack.STREAM_CODEC}, narrowing the {@code slot >= 0 && slot < 5} guard on
     * either {@code setGhostItemInternal} or {@code getGhostItem}, dropping the broadcast, or
     * moving it back out from behind that guard.
     */
    public static void filterItemsAreStoredAsSingleCountCopiesAndCanBeCleared(GameTestHelper helper) {
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.NETHERITE_HOPPER);
        List<GhostBroadcast> broadcasts = recordGhostBroadcasts(helper);

        // --- a placeholder, not stock: seventeen diamonds go in, one comes back ---
        ItemStack seventeen = new ItemStack(Items.DIAMOND, 17);
        hopper.setGhostItem(0, seventeen);

        // The half that never touches a field: what the tracking clients are told. Read straight
        // after the call, so the out of range writes further down cannot be mistaken for it.
        GhostBroadcast set = lastBroadcastFor(helper, broadcasts, hopper,
                "setting filter slot 0 to a stack of diamonds");
        helper.assertValueEqual(set.slot(), 0, "the filter slot the hopper broadcast a change for");
        helper.assertTrue(set.stack().is(Items.DIAMOND),
                "the hopper broadcast " + set.stack() + " for slot 0, so every client watching it "
                        + "is told about a filter the hopper does not have");

        helper.assertTrue(hopper.getGhostItem(0).is(Items.DIAMOND),
                "the filter slot did not take the item at all, it holds " + hopper.getGhostItem(0));
        helper.assertValueEqual(hopper.getGhostItem(0).getCount(), 1,
                "count stored in a filter slot that was handed a stack of 17");
        helper.assertValueEqual(seventeen.getCount(), 17,
                "the hopper shrank the caller's own stack, so it moved the items instead of "
                        + "copying a placeholder out of them");

        // --- and a copy: what the caller does with its stack afterwards is none of the hopper's
        //     business. Both an edit that changes the count and one that changes a component are
        //     tried, because sharing the reference would leak either.
        seventeen.setCount(3);
        seventeen.set(DataComponents.CUSTOM_NAME, Component.literal("edited after the fact"));
        helper.assertValueEqual(hopper.getGhostItem(0).getCount(), 1,
                "count in the filter slot after the caller edited its own stack");
        helper.assertTrue(hopper.getGhostItem(0).get(DataComponents.CUSTOM_NAME) == null,
                "a rename applied to the caller's stack showed up in the filter slot, so the slot "
                        + "holds the very stack it was given instead of a copy");

        // --- an out of range slot changes nothing, throws nothing and tells nobody ---
        hopper.setGhostItem(0, new ItemStack(Items.STONE));
        int broadcastsBeforeTheStrayWrites = broadcasts.size();
        hopper.setGhostItem(9, new ItemStack(Items.DIAMOND));
        hopper.setGhostItem(-1, new ItemStack(Items.DIAMOND));
        helper.assertTrue(hopper.getGhostItem(9).isEmpty(),
                "reading filter slot 9 answered with " + hopper.getGhostItem(9));
        helper.assertTrue(hopper.getGhostItem(-1).isEmpty(),
                "reading filter slot -1 answered with " + hopper.getGhostItem(-1));
        helper.assertTrue(hopper.getGhostItem(0).is(Items.STONE),
                "the out of range writes landed in slot 0, which now holds "
                        + hopper.getGhostItem(0));
        for (int slot = 1; slot < 5; slot++) {
            helper.assertTrue(hopper.getGhostItem(slot).isEmpty(),
                    "the out of range writes landed in filter slot " + slot);
        }
        // A slot the hopper refuses to store must not be announced either. The recorder is shared
        // with every other test in the batch, so only this hopper's own entries are judged.
        for (int i = broadcastsBeforeTheStrayWrites; i < broadcasts.size(); i++) {
            helper.assertFalse(broadcasts.get(i).hopper() == hopper,
                    "the hopper announced a change for filter slot " + broadcasts.get(i).slot()
                            + ", which is outside the five it can store, so one packet from a "
                            + "client becomes one to every player tracking the block");
        }

        // --- the delete path, driven through the wire format that has to carry it ---
        SetHopperGhostItemPayload clearing = roundTrip(helper, new SetHopperGhostItemPayload(0, ItemStack.EMPTY));
        helper.assertValueEqual(clearing.slotIndex(), 0, "slot index after the payload round trip");
        helper.assertTrue(clearing.stack().isEmpty(),
                "the payload turned the empty stack into " + clearing.stack()
                        + " on the way through its codec");
        hopper.setGhostItem(clearing.slotIndex(), clearing.stack());
        helper.assertTrue(hopper.getGhostItem(0).isEmpty(),
                "an empty filter item did not clear the slot, it still holds "
                        + hopper.getGhostItem(0));

        // The delete has to travel as well: a client that is never told the slot was emptied keeps
        // drawing the old ghost item over it.
        GhostBroadcast cleared = lastBroadcastFor(helper, broadcasts, hopper,
                "clearing filter slot 0");
        helper.assertValueEqual(cleared.slot(), 0, "the filter slot the clearing broadcast names");
        helper.assertTrue(cleared.stack().isEmpty(),
                "clearing a filter slot broadcast " + cleared.stack() + " instead of an empty "
                        + "stack, so the clients keep the filter that was just deleted");

        // A non-empty payload has to survive the same trip, otherwise the assertion above could
        // be met by a codec that drops every stack it is given.
        SetHopperGhostItemPayload filled =
                roundTrip(helper, new SetHopperGhostItemPayload(3, new ItemStack(Items.DIAMOND, 17)));
        helper.assertTrue(filled.stack().is(Items.DIAMOND) && filled.stack().getCount() == 17,
                "a filled payload came back as " + filled.stack());

        helper.succeed();
    }

    /**
     * The {@code ContainerData} the menu synchronises the mode over: index 0 reads and writes the
     * mode ordinal, everything else is inert (ModHopperBlockEntity.java:64-82). It is the only
     * route the mode takes to the screen, which reads it back out with
     * {@code getSyncedFilterMode} (NetheriteHopperScreen.java:50).
     *
     * <p><b>The write is driven through the menu, because that is the only place it is reachable
     * from.</b> The delegate's {@code set} has no caller in the mod - there is no
     * {@code setData(} anywhere under {@code common/src}, {@code src/main}, {@code forge/src} or
     * {@code neoforge/src}, and the screen pushes a mode change back with a
     * {@code ToggleHopperFilterPayload} instead (NetheriteHopperScreen.java:41-43). The one line
     * that can still reach it is {@code addDataSlots(propertyDelegate)}
     * (NetheriteHopperScreenHandler.java:40) - which is at the same time the live wire the mode
     * travels to the client on, and which no test covered before. Writing through
     * {@code menu.setData(0, ...)} pins it: delete that line and the menu holds no data slot at
     * all, so the call below dies with an {@code IndexOutOfBoundsException}.
     *
     * <p>The write is not judged by what the delegate reports back - that would be one field
     * echoing itself. It is judged by {@code canPlaceItem}: after writing Type Match, a slot
     * without a filter item has to start refusing. {@code getSyncedFilterMode} is asked in the
     * same breath, because a screen that misreads the ordinal shows the wrong mode while every
     * field on the server is right.
     *
     * <p>Index 1 stays on the raw delegate rather than going through the menu: the delegate
     * declares a count of one, so a real menu has exactly one data slot and
     * {@code setData(1, ...)} could only ever throw.
     *
     * <p>Also here, because they are the same enum: the mode texts and their colours. Both are
     * hard coded English literals in {@code HopperFilterMode} rather than translation keys, so
     * this pins the current state deliberately - a port that swaps them for
     * {@code Component.translatable} will go red and should.
     *
     * <p><b>The colour is read off the component, not off {@code getColor()}.</b> The enum's
     * {@code getColor()} has no caller anywhere in the mod - not in {@code common/src},
     * {@code src/main}, {@code forge/src} or {@code neoforge/src} - so an assertion on it says
     * nothing about what the player sees. The colour that reaches the screen is the {@code Style}
     * on the very component {@code NetheriteHopperScreen} hands to
     * {@code setTooltipForNextFrame} (line 66), i.e. the {@code .withStyle(ChatFormatting.*)}
     * call in the enum constant. That is what {@link #assertModeTextColour} asks for; the three
     * {@code getColor()} numbers stay as well, because they are the claim the migration comment
     * above the enum makes about {@code TextColor.RED} and friends, and because the two have to
     * agree.
     *
     * <p>And the block update, in the two steps it really has. {@code toggleFilterMode} calls
     * {@code updateListeners}, which (a) marks the block entity changed and (b) pushes an update
     * to everyone tracking the block. (a) is observed through the chunk's unsaved flag - cleared,
     * then re-read in the same tick, so nothing else in the room can have set it in between. For
     * (b) the packet that carries the mode is asked for directly: {@code setChanged()} alone only
     * makes the chunk save, and a client that already has the hopper in view learns about the
     * switch from {@code getUpdatePacket()} and from nothing else. The {@code sendBlockUpdated}
     * call that hands that packet out is not reachable from here; see the class javadoc.
     *
     * <p><b>Both ends of the wrap are asked for.</b> The delegate reduces the value it is given
     * with {@code Math.floorMod}, and the negative half is the one that used to be wrong: Java's
     * {@code %} keeps the sign of its left operand, so {@code values()[value % length]} indexed
     * the array with a negative number. The positive wrap is pinned right beside it, because a
     * fix that clamped instead of wrapping would change that half.
     *
     * <p>What breaks this test: dropping {@code addDataSlots(propertyDelegate)} from the menu,
     * making the delegate answer at another index, {@code getSyncedFilterMode} losing its ordinal
     * lookup or its bounds check (an ordinal off the wire then indexes past the enum), going back
     * from {@code Math.floorMod} to a plain remainder, dropping the
     * {@code setChanged()} out of {@code updateListeners}, {@code getUpdatePacket} answering
     * {@code null} or with a tag that has lost the mode, renaming a mode text, or a mode constant
     * losing the style its tooltip is drawn in.
     */
    public static void theModeDelegateReadsAndWritesTheFilterMode(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.REINFORCED_HOPPER);
        ContainerData delegate = hopper.getPropertyDelegate();

        helper.assertValueEqual(delegate.getCount(), 1, "values the hopper delegate synchronises");
        helper.assertValueEqual(delegate.get(0), HopperFilterMode.NONE.ordinal(),
                "the mode a fresh hopper reports over its delegate");

        // Built the way the server builds it, so its single data slot is this hopper's own
        // delegate - the write below only arrives if addDataSlots(propertyDelegate) is still there.
        NetheriteHopperScreenHandler menu =
                new NetheriteHopperScreenHandler(1, player.getInventory(), hopper, hopper);
        helper.assertTrue(menu.getSyncedFilterMode() == HopperFilterMode.NONE,
                "a fresh hopper's menu reports " + menu.getSyncedFilterMode()
                        + " to the screen instead of the mode the hopper is in");

        // --- writing index 0 really switches the filter, not just the reported number ---
        menu.setData(0, HopperFilterMode.TYPE.ordinal());
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.TYPE,
                "writing the menu's data slot did not reach the hopper, it is in "
                        + hopper.getFilterMode());
        helper.assertValueEqual(delegate.get(0), HopperFilterMode.TYPE.ordinal(),
                "the mode the delegate reports back after the write");
        helper.assertTrue(menu.getSyncedFilterMode() == HopperFilterMode.TYPE,
                "the menu hands the screen " + menu.getSyncedFilterMode() + " while the hopper is "
                        + "in " + hopper.getFilterMode());
        helper.assertFalse(hopper.canPlaceItem(0, new ItemStack(Items.STONE)),
                "the hopper still accepts anything after Type Match was written over the delegate, "
                        + "so the write only moved a number around");

        // --- every other index is inert in both directions ---
        // Straight on the delegate: a menu carries one data slot, so index 1 has no menu route.
        delegate.set(1, HopperFilterMode.NONE.ordinal());
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.TYPE,
                "a write to delegate index 1 changed the filter mode to " + hopper.getFilterMode());
        helper.assertValueEqual(delegate.get(1), 0, "what the delegate answers for index 1");

        // --- a value outside the modes wraps instead of throwing, in both directions ---
        menu.setData(0, HopperFilterMode.values().length);
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.NONE,
                "the delegate did not wrap a mode ordinal of " + HopperFilterMode.values().length
                        + ", the hopper is in " + hopper.getFilterMode());
        // Below zero as well: Java's % keeps the sign of its left operand, so the plain remainder
        // handed values() a negative index. Math.floorMod wraps onto the last mode instead.
        menu.setData(0, -1);
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.TYPE,
                "the delegate did not wrap the mode ordinal -1 onto the last mode, the hopper is "
                        + "in " + hopper.getFilterMode());
        helper.assertFalse(hopper.canPlaceItem(0, new ItemStack(Items.STONE)),
                "the hopper accepts anything after -1 was written over the delegate, so the wrap "
                        + "only moved a number around instead of reaching the filter");
        // Back where the wrap above left it, so the toggle below still reaches Exact Match.
        menu.setData(0, HopperFilterMode.NONE.ordinal());

        // --- a mode change marks the block entity changed ---
        // Cleared and re-read inside one server tick, so no other block entity in this chunk can
        // have marked it in between.
        LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(HOPPER_POS));
        chunk.tryMarkSaved();
        helper.assertFalse(chunk.isUnsaved(), "the chunk could not be marked saved, so the flag "
                + "below would prove nothing");
        hopper.toggleFilterMode();
        helper.assertTrue(chunk.isUnsaved(),
                "toggling the filter mode did not mark the hopper changed, so a mode switch no "
                        + "longer reaches anyone looking at the block");

        // --- and the packet the switch is carried out on ---
        // The unsaved flag above only says the chunk will be written to disk. A client that
        // already has this hopper in view is told about the new mode by this packet alone.
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the toggle above should have reached Exact Match, the hopper is in "
                        + hopper.getFilterMode());
        Packet<ClientGamePacketListener> update = hopper.getUpdatePacket();
        helper.assertTrue(update instanceof ClientboundBlockEntityDataPacket,
                "the hopper offers "
                        + (update == null ? "no update packet at all" : update.getClass().getSimpleName())
                        + ", so a mode change never reaches a client that is already looking at "
                        + "the block");
        ClientboundBlockEntityDataPacket data = (ClientboundBlockEntityDataPacket) update;
        helper.assertValueEqual(data.getPos(), helper.absolutePos(HOPPER_POS),
                "the block the hopper's update packet names");
        helper.assertValueEqual(data.getTag().getIntOr("FilterMode", -1),
                HopperFilterMode.WHITELIST.ordinal(),
                "the filter mode inside the hopper's update packet");

        // --- the menu texts and colours, pinned as the hard coded English they are ---
        helper.assertValueEqual(HopperFilterMode.NONE.getText().getString(), "Disabled",
                "the text of the disabled filter mode");
        helper.assertValueEqual(HopperFilterMode.WHITELIST.getText().getString(), "Exact Match",
                "the text of the exact match filter mode");
        helper.assertValueEqual(HopperFilterMode.TYPE.getText().getString(), "Type Match",
                "the text of the type match filter mode");
        helper.assertValueEqual(HopperFilterMode.NONE.getColor(), 0xFF5555,
                "the colour of the disabled filter mode (the value ChatFormatting.RED carried "
                        + "before 26.2 removed getColor())");
        helper.assertValueEqual(HopperFilterMode.WHITELIST.getColor(), 0x55FF55,
                "the colour of the exact match filter mode");
        helper.assertValueEqual(HopperFilterMode.TYPE.getColor(), 0xFFFF55,
                "the colour of the type match filter mode");

        // The colour the player is actually shown: the style on the component the screen puts in
        // the tooltip. getColor() above has no reader in the mod, so on its own it would leave a
        // mode text that lost its .withStyle(...) - and is drawn plain white - unnoticed.
        assertModeTextColour(helper, HopperFilterMode.NONE, 0xFF5555, "disabled");
        assertModeTextColour(helper, HopperFilterMode.WHITELIST, 0x55FF55, "exact match");
        assertModeTextColour(helper, HopperFilterMode.TYPE, 0xFFFF55, "type match");

        // --- a number off the wire that names no mode falls back to Disabled ---
        // Everything above goes through the hopper's own delegate, which wraps with floorMod - so
        // through that menu getSyncedFilterMode never sees anything outside the enum and its own
        // bounds check is never exercised. The CLIENT menu is the one that can: it is built from a
        // position and a plain SimpleContainerData, and whatever the server sends lands in there
        // raw. A server running another version of the mod, or a fourth mode added on one side
        // only, sends an ordinal this enum does not have - and the screen asks this method every
        // frame. Without the bounds check that is an ArrayIndexOutOfBoundsException in the render
        // loop; with it, the filter reads as Disabled until the next sync.
        NetheriteHopperScreenHandler clientMenu =
                new NetheriteHopperScreenHandler(2, player.getInventory(), hopper.getBlockPos());
        clientMenu.setData(0, HopperFilterMode.values().length);
        helper.assertTrue(clientMenu.getSyncedFilterMode() == HopperFilterMode.NONE,
                "an ordinal one past the last mode reads as " + clientMenu.getSyncedFilterMode()
                        + " instead of falling back to Disabled");
        clientMenu.setData(0, -1);
        helper.assertTrue(clientMenu.getSyncedFilterMode() == HopperFilterMode.NONE,
                "a negative ordinal reads as " + clientMenu.getSyncedFilterMode()
                        + " instead of falling back to Disabled");
        clientMenu.setData(0, HopperFilterMode.TYPE.ordinal());
        helper.assertTrue(clientMenu.getSyncedFilterMode() == HopperFilterMode.TYPE,
                "the client menu reads " + clientMenu.getSyncedFilterMode() + " for the Type Match "
                        + "ordinal, so the fallback above may be swallowing every value");

        helper.succeed();
    }

    /**
     * With a filter switched on, an item that lands in a slot whose filter is still empty becomes
     * that slot's filter (ModHopperBlockEntity.java:284-293). This is the convenience half of the
     * feature: the player drops one of the item in, and the slot is configured.
     *
     * <p>Three boundaries around it, all of them in the same {@code if}: the filter has to be on,
     * the slot's filter has to be empty (an existing one is never overwritten), and an empty
     * stack teaches nothing. The learned filter is then driven through {@code canPlaceItem}, so
     * the claim is "the slot now filters on that item" rather than "a field was written".
     *
     * <p>What breaks this test: removing the {@code currentFilterMode != NONE} guard (every plain
     * hopper would start filtering the moment something enters it), removing the
     * {@code ghostItems.get(slot).isEmpty()} guard (a configured slot would be re-taught by the
     * first item that slips past it), or dropping the {@code setCount(1)} on the learned copy.
     */
    public static void theFilterLearnsItsGhostFromTheFirstItemThatIsPlaced(GameTestHelper helper) {
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.NETHERITE_HOPPER);

        // --- filter off: putting items in teaches nothing ---
        hopper.setItem(0, new ItemStack(Items.EMERALD, 4));
        helper.assertTrue(hopper.getGhostItem(0).isEmpty(),
                "a hopper with its filter disabled learned the filter item "
                        + hopper.getGhostItem(0));

        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the toggle did not reach Exact Match, it is " + hopper.getFilterMode());

        // --- filter on, slot unconfigured: the item becomes the filter, as a single count copy ---
        hopper.setItem(3, new ItemStack(Items.DIAMOND, 12));
        helper.assertTrue(hopper.getGhostItem(3).is(Items.DIAMOND),
                "slot 3 did not learn its filter item, it holds " + hopper.getGhostItem(3));
        helper.assertValueEqual(hopper.getGhostItem(3).getCount(), 1,
                "count of a filter item the hopper taught itself from a stack of 12");
        helper.assertValueEqual(hopper.getItem(3).getCount(), 12,
                "items left in slot 3 - learning a filter must not eat the stack");
        // Driven, so this is about the gate and not about a written field.
        helper.assertTrue(hopper.canPlaceItem(3, new ItemStack(Items.DIAMOND)),
                "the slot refuses the very item it just learned");
        helper.assertFalse(hopper.canPlaceItem(3, new ItemStack(Items.STONE)),
                "the learned filter lets a different item through, so it is not filtering");

        // --- filter on, slot already configured: the existing filter wins ---
        hopper.setGhostItem(4, new ItemStack(Items.STONE));
        hopper.setItem(4, new ItemStack(Items.DIAMOND));
        helper.assertTrue(hopper.getGhostItem(4).is(Items.STONE),
                "an item that landed in a configured slot overwrote its filter, which now holds "
                        + hopper.getGhostItem(4));

        // --- filter on, empty stack: nothing to learn ---
        hopper.setItem(2, ItemStack.EMPTY);
        helper.assertTrue(hopper.getGhostItem(2).isEmpty(),
                "clearing a slot taught it the filter item " + hopper.getGhostItem(2));

        helper.succeed();
    }

    /**
     * Mode and filter items are written under {@code FilterMode} and {@code GhostItems}
     * (ModHopperBlockEntity.java:187-196) and read back in {@code loadAdditional} - a hopper that
     * is configured and then unloaded has to come back configured. The hopper's own five slots
     * ride along on {@code ContainerHelper}.
     *
     * <p>The third key {@code saveAdditional} writes, {@code TransferCooldown}, is deliberately
     * <em>not</em> claimed here; see the class javadoc for why it cannot be.
     *
     * <p>The round trip is run against a second, detached block entity rather than against the
     * one in the level: loading into the live one would only prove that fields survive being
     * written twice. The reloaded copy is then judged through {@code canPlaceItem}, so the claim
     * is that the <em>filter</em> came back, not that a couple of fields did.
     *
     * <p>The inventory item is deliberately put in before the mode is switched on, because
     * {@code setItem} would otherwise teach slot 1 a filter of its own and the "an empty filter
     * slot stays empty" assertion below would be testing the wrong thing.
     *
     * <p><b>The last two loads feed back a mode the hopper never wrote.</b> {@code FilterMode}
     * is an enum ordinal in a file anyone can edit, and the same number arrives from a server
     * over the update tag; an ordinal outside {@code 0..2} used to index
     * {@code HopperFilterMode.values()} straight out of bounds. {@code BlockEntity#loadStatic}
     * catches that, logs one line and drops the block entity - the hopper stays standing while
     * its contents, its five filter items and its mode are gone. So the claim is not only that
     * nothing throws but that everything else in the tag still came back, judged through
     * {@code canPlaceItem} rather than through the mode field alone.
     *
     * <p>What breaks this test: renaming {@code FilterMode} or {@code GhostItems} on one side
     * only, dropping the {@code GhostItems} child from {@code saveAdditional}, dropping the range
     * check around the saved mode, or the {@code ContainerHelper} format changing under the mod
     * so the two sides no longer agree.
     */
    public static void hopperConfigurationSurvivesTheSaveAndLoadRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.NETHERITE_HOPPER);

        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("a very particular stone"));

        // Stock first, while the filter is still off - see the note above.
        hopper.setItem(1, new ItemStack(Items.COBBLESTONE, 7));
        hopper.toggleFilterMode();
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.TYPE,
                "two toggles should reach Type Match, the hopper is in " + hopper.getFilterMode());
        hopper.setGhostItem(0, new ItemStack(Items.DIAMOND, 9));
        hopper.setGhostItem(4, namedStone);

        CompoundTag saved = hopper.saveCustomOnly(level.registryAccess());

        ModHopperBlockEntity reloaded = new ModHopperBlockEntity(
                helper.absolutePos(HOPPER_POS), ModBlocks.NETHERITE_HOPPER.defaultBlockState());
        reloaded.loadCustomOnly(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), saved));

        helper.assertTrue(reloaded.getFilterMode() == HopperFilterMode.TYPE,
                "the filter mode did not survive the save and load round trip, it came back as "
                        + reloaded.getFilterMode());
        helper.assertTrue(reloaded.getGhostItem(0).is(Items.DIAMOND),
                "filter slot 0 came back as " + reloaded.getGhostItem(0));
        helper.assertValueEqual(reloaded.getGhostItem(0).getCount(), 1,
                "count of filter slot 0 after the round trip");
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(reloaded.getGhostItem(4), hopper.getGhostItem(4)),
                "filter slot 4 lost its components on the way through the save, it came back as "
                        + reloaded.getGhostItem(4));
        helper.assertTrue(reloaded.getGhostItem(1).isEmpty(),
                "an unconfigured filter slot came back holding " + reloaded.getGhostItem(1));
        helper.assertTrue(reloaded.getItem(1).is(Items.COBBLESTONE),
                "the hopper's own contents did not survive, slot 1 holds " + reloaded.getItem(1));
        helper.assertValueEqual(reloaded.getItem(1).getCount(), 7,
                "items in slot 1 after the round trip");

        // The filter is judged by what it does, not by what it stored.
        helper.assertTrue(reloaded.canPlaceItem(0, new ItemStack(Items.DIAMOND)),
                "the reloaded hopper refuses the item its own filter names");
        helper.assertFalse(reloaded.canPlaceItem(0, new ItemStack(Items.STONE)),
                "the reloaded hopper lets anything into slot 0, so its filter mode came back off");
        helper.assertTrue(reloaded.canPlaceItem(4, new ItemStack(Items.STONE)),
                "Type Match did not survive: slot 4 refuses a plain stone even though its filter "
                        + "is a renamed one and only the item type is supposed to matter");
        helper.assertFalse(reloaded.canPlaceItem(1, new ItemStack(Items.COBBLESTONE)),
                "a slot whose filter is empty accepts items again after the round trip");

        // --- and a mode the hopper never wrote: it falls back instead of throwing ---
        // A FilterMode past the last one reaches loadAdditional from a hand edited region file or
        // from a server's update packet. BlockEntity#loadStatic answers a throwing load by
        // dropping the whole block entity, so the hopper would come back with no contents, no
        // filter items and no mode at all - a silent loss with one log line behind it. The very
        // tag the hopper wrote above is reused, so nothing but the mode is out of the ordinary.
        saved.putInt("FilterMode", HopperFilterMode.values().length);
        ModHopperBlockEntity pastTheLastMode = new ModHopperBlockEntity(
                helper.absolutePos(HOPPER_POS), ModBlocks.NETHERITE_HOPPER.defaultBlockState());
        pastTheLastMode.loadCustomOnly(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), saved));

        helper.assertTrue(pastTheLastMode.getFilterMode() == HopperFilterMode.NONE,
                "a hopper loaded with an unknown filter mode came back in "
                        + pastTheLastMode.getFilterMode() + " instead of falling back to Disabled");
        // Driven, so the claim is the mode the hopper is really in: Disabled is the only mode
        // that lets an item into a slot whose filter item is empty.
        helper.assertTrue(pastTheLastMode.canPlaceItem(1, new ItemStack(Items.COBBLESTONE)),
                "the fallback still filters, so the unknown ordinal was never really replaced");
        // And the rest of the block entity came along, which is the whole point of not throwing.
        helper.assertTrue(pastTheLastMode.getGhostItem(0).is(Items.DIAMOND),
                "the filter items went down with the unknown mode, slot 0 holds "
                        + pastTheLastMode.getGhostItem(0));
        helper.assertTrue(pastTheLastMode.getItem(1).is(Items.COBBLESTONE),
                "the hopper's own contents went down with the unknown mode, slot 1 holds "
                        + pastTheLastMode.getItem(1));

        // The other side of the range, which a downgrade or a hand edit reaches just as easily.
        saved.putInt("FilterMode", -1);
        ModHopperBlockEntity belowTheFirstMode = new ModHopperBlockEntity(
                helper.absolutePos(HOPPER_POS), ModBlocks.NETHERITE_HOPPER.defaultBlockState());
        belowTheFirstMode.loadCustomOnly(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), saved));
        helper.assertTrue(belowTheFirstMode.getFilterMode() == HopperFilterMode.NONE,
                "a hopper loaded with a negative filter mode came back in "
                        + belowTheFirstMode.getFilterMode());

        helper.succeed();
    }

    /**
     * {@code getUpdateTag} is what a client gets when the hopper enters its view; it hand-builds
     * the filter list instead of going through {@code ContainerHelper}
     * (ModHopperBlockEntity.java:209-238), which is exactly the kind of code that drifts away
     * from the reader on the other side.
     *
     * <p>So the tag is not only read - it is fed back into a fresh block entity through
     * {@code loadAdditional}, the same reader the client ends up using. That covers the key name
     * ({@code Items} inside {@code GhostItems}), the {@code Slot} byte and the encoded stack in
     * one go: get any of them wrong and the filter arrives empty on the client while every field
     * on the server still looks right.
     *
     * <p>What breaks this test: dropping {@code FilterMode} from the tag, renaming the inner
     * list, writing the slot as anything but the {@code Slot} byte {@code ItemStackWithSlot.CODEC}
     * expects, or listing empty filter slots (a client would then read a filter for a slot that
     * has none).
     */
    public static void theUpdateTagCarriesModeAndFilterItemsToTheClient(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.NETHERITE_HOPPER);

        hopper.toggleFilterMode();
        hopper.setGhostItem(2, new ItemStack(Items.DIAMOND, 5));

        CompoundTag update = hopper.getUpdateTag(level.registryAccess());
        helper.assertValueEqual(update.getIntOr("FilterMode", -1), HopperFilterMode.WHITELIST.ordinal(),
                "the filter mode in the update tag");

        ListTag entries = update.getCompoundOrEmpty("GhostItems").getListOrEmpty("Items");
        helper.assertValueEqual(entries.size(), 1,
                "entries in the update tag's filter list - only the one configured slot belongs "
                        + "in it");
        helper.assertValueEqual((int) entries.getCompoundOrEmpty(0).getByteOr("Slot", (byte) -1), 2,
                "the slot number the update tag names for the configured filter");

        // The client rebuilds its hopper out of exactly this tag, so drive that instead of
        // reading fields.
        ModHopperBlockEntity clientSide = new ModHopperBlockEntity(
                helper.absolutePos(HOPPER_POS), ModBlocks.NETHERITE_HOPPER.defaultBlockState());
        clientSide.loadCustomOnly(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), update));

        helper.assertTrue(clientSide.getFilterMode() == HopperFilterMode.WHITELIST,
                "a hopper rebuilt from the update tag is in " + clientSide.getFilterMode());
        helper.assertTrue(clientSide.getGhostItem(2).is(Items.DIAMOND),
                "the filter item did not survive the update tag, slot 2 holds "
                        + clientSide.getGhostItem(2));
        helper.assertValueEqual(clientSide.getGhostItem(2).getCount(), 1,
                "count of the filter item rebuilt from the update tag");
        helper.assertTrue(clientSide.getGhostItem(0).isEmpty(),
                "the update tag put a filter item into slot 0, which has none");

        helper.succeed();
    }

    /**
     * The menu: right clicking a hopper opens the mod's own one
     * ({@code ModHopperBlock#useWithoutItem}), and while a filter is on, a plain click on one of
     * the five hopper slots configures that slot instead of putting the item in
     * ({@code ModHopperScreenHandler#clicked}, lines 29-48).
     *
     * <p>The interception is only worth anything if the same click still works everywhere else,
     * so both fall-through cases are driven with the very same click: a player inventory slot
     * while the filter is on (the guard is {@code slotIndex < 5}), and a hopper slot while the
     * filter is off. Without those two, deleting the {@code super.clicked(...)} call at the end
     * would go unnoticed.
     *
     * <p><b>And the three click kinds that never pass the screen at all.</b> The filter branch in
     * {@code clicked} only covers {@code PICKUP}; a shift click out of the player inventory
     * ({@code QUICK_MOVE}, which {@code HopperMenu#quickMoveStack} aims straight at slots 0-4), a
     * hotbar swap ({@code SWAP}, which {@code checkHotbarKeyPressed} sends past the screen's
     * {@code mouseClicked}) and a drag across the slots ({@code QUICK_CRAFT}) all go through
     * vanilla, which asks {@code Slot#mayPlace} - a constant {@code true} on the plain
     * {@code Slot} that {@code HopperMenu} adds. All three are driven here against a filter that
     * names diamonds, with a diamond shift click as the positive control: the fix is a filtering
     * {@code Slot} rather than three more branches in {@code clicked}, and a slot that refused
     * <em>everything</em> would meet the negative half on its own.
     *
     * <p><b>The wiring is read off the menu's slots, not off {@code getBlockEntity()}.</b>
     * {@code NetheriteHopperScreenHandler#getBlockEntity} answers with
     * {@code world.getBlockEntity(this.pos)} (NetheriteHopperScreenHandler.java:50-59), so it
     * hands back the live block entity even for a menu built by the <em>client</em> constructor -
     * the one that keeps {@code blockEntity = null} and a detached {@code new SimpleContainer(5)}
     * (lines 21-23). Comparing it against the hopper that was clicked therefore proves nothing:
     * point {@code createScreenMenu} at that constructor and the player would get an empty
     * stranger's inventory while the comparison stayed green. A marker item is put into hopper
     * slot 2 before the click instead, and the opened menu has to show it and to be backed by
     * this very container.
     *
     * <p>For the same reason the clicks are driven on the menu the block actually opened rather
     * than on a hand built one. On a hand built menu the {@code blockEntity != null} guard in
     * {@code ModHopperScreenHandler#clicked} (line 32) is satisfied by the test's own
     * construction, so the filter branch would keep working there no matter what the game builds.
     *
     * <p><b>That guard gets its own click, on a menu built the way the client builds one.</b>
     * The client constructor leaves {@code blockEntity} null (NetheriteHopperScreenHandler.java:
     * 21-23) and the screen normally eats slot clicks in {@code mouseClicked} before the menu
     * sees them - but not all of them: a hotbar swap (keys 1-9) over one of the five slots goes
     * through {@code checkHotbarKeyPressed} straight to {@code slotClicked}, past the screen. So
     * the null case is reachable in game, and without the guard it is a null dereference on the
     * client. Asserted from the other side: with the guard the click falls through to vanilla and
     * the item lands in the detached menu's own container.
     *
     * <p><b>And {@code isGridAligned}</b> (ModHopperBlockEntity.java:254-257), which is here
     * because it has nowhere better to be: it is a one line override with no reader in the mod,
     * and vanilla only consults it in the one branch of {@code suckInItems} that no rig in the
     * suite reaches - the {@code else} that runs when there is <em>no</em> container above the
     * hopper (HopperBlockEntity.java:233-235). Every hopper rig in this file and in
     * {@code BlockBehaviourTests} has a chest up there, so a probe of its own is needed: one mod
     * hopper capped with a solid block and one item lying in its mouth. Answering {@code true}
     * like vanilla's hopper does would make that item unreachable. {@code suckInItems} is called
     * directly instead of through a tick so the item cannot drift out of the suck box first, and
     * so this test still needs no tick budget.
     *
     * <p>What breaks this test: {@code useWithoutItem} falling back to vanilla's hopper menu,
     * {@code createScreenMenu} switching to the client constructor (empty inventory, no filter
     * branch), losing the {@code mode != NONE} guard (the filter would swallow every click even
     * when it is off), widening the slot range past the five hopper slots, dropping the
     * {@code return} that stops the item from being placed, dropping the {@code blockEntity !=
     * null} guard, putting the plain {@code Slot}s back in place of the filtering ones, or
     * {@code isGridAligned} starting to answer {@code true}.
     */
    public static void hopperMenuOpensOnUseAndFilterClicksNeverStoreTheItem(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.NETHERITE_HOPPER);

        // The marker the opened menu has to show. Put in while the filter is still off, so
        // setItem cannot teach slot 2 a filter item of its own on the way in.
        hopper.setItem(2, new ItemStack(Items.BRICK));

        // --- the block hands out the mod's own menu for this very block entity ---
        // Asked through getMenuProvider rather than by right clicking. A real click ends in
        // player.openMenu, and on NeoForge that sends neoforge:advanced_open_screen to the
        // client - which a mock player's connection refuses outright ("may not be sent to the
        // client!"), so the same test passed on Fabric and died on NeoForge. The provider is
        // the part the mod owns; handing its result to the player is vanilla's job.
        BlockPos absolute = helper.absolutePos(HOPPER_POS);
        BlockState state = helper.getBlockState(HOPPER_POS);
        MenuProvider provider = state.getMenuProvider(helper.getLevel(), absolute);
        helper.assertTrue(provider != null,
                "the hopper block offers no menu at all, so a right click cannot open one");

        AbstractContainerMenu opened =
                provider.createMenu(1, player.getInventory(), player);
        helper.assertTrue(opened instanceof NetheriteHopperScreenHandler,
                "the hopper block offers " + (opened == null ? "null" : opened.getClass().getSimpleName())
                        + " instead of the mod's hopper menu");
        player.containerMenu = opened;
        ModHopperScreenHandler menu = (ModHopperScreenHandler) opened;

        // The wiring, read off the slots - see the javadoc on why getBlockEntity() cannot answer
        // this. Both halves are needed: the identity says which container the five slots address,
        // the marker says that container is really being read.
        helper.assertTrue(menu.getSlot(2).container == hopper,
                "the opened menu's hopper slots are backed by "
                        + menu.getSlot(2).container.getClass().getSimpleName()
                        + " instead of the block entity that was clicked, so the player is looking "
                        + "at a different inventory");
        helper.assertTrue(menu.getSlot(2).getItem().is(Items.BRICK),
                "hopper slot 2 holds " + menu.getSlot(2).getItem() + " in the opened menu, but the "
                        + "hopper that was clicked holds " + hopper.getItem(2));
        // The other half of the wiring - the menu's own blockEntity field, which decides whether
        // clicked() takes its filter branch at all - is not readable from here: the netherite
        // subclass overrides getBlockEntity() with the position lookup described above. It is
        // covered by the filter click below, which does nothing at all when that field is null.

        // --- with a filter on, a click on a hopper slot only sets the filter ---
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the toggle did not reach Exact Match, it is " + hopper.getFilterMode());

        menu.setCarried(new ItemStack(Items.DIAMOND, 17));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);

        helper.assertTrue(hopper.getGhostItem(0).is(Items.DIAMOND),
                "the click did not set the filter item, slot 0 holds " + hopper.getGhostItem(0));
        helper.assertValueEqual(hopper.getGhostItem(0).getCount(), 1,
                "count of a filter item set by clicking");
        helper.assertTrue(hopper.getItem(0).isEmpty(),
                "the click put the diamonds into the hopper as well, so the filter click is not "
                        + "stopping the vanilla one behind it");
        helper.assertValueEqual(menu.getCarried().getCount(), 17,
                "items still on the cursor - setting a filter must not consume them");

        // --- the same click on a player inventory slot is none of the filter's business ---
        menu.clicked(FIRST_PLAYER_SLOT, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getSlot(FIRST_PLAYER_SLOT).getItem().is(Items.DIAMOND),
                "the click on a player inventory slot was swallowed by the filter, the slot holds "
                        + menu.getSlot(FIRST_PLAYER_SLOT).getItem());
        helper.assertValueEqual(menu.getSlot(FIRST_PLAYER_SLOT).getItem().getCount(), 17,
                "items that reached the player inventory slot");
        helper.assertTrue(menu.getCarried().isEmpty(),
                "the cursor kept its stack, so nothing was really placed");
        helper.assertTrue(hopper.getItem(0).isEmpty(),
                "the click on a player inventory slot leaked into the hopper's own slot 0");
        helper.assertTrue(hopper.getGhostItem(0).is(Items.DIAMOND),
                "the click on a player inventory slot rewrote filter slot 0, which now holds "
                        + hopper.getGhostItem(0));

        // --- the click kinds the screen never gets to eat have to obey the filter as well ---
        // The screen swallows plain clicks over the five slots, but a shift click out of the
        // player inventory, a hotbar swap (keys 1-9) and a drag across the slots all reach the
        // menu on their own. Vanilla gates those three on Slot#mayPlace, which on the plain Slot
        // HopperMenu adds is a constant true - so filter foreign material went straight into a
        // slot whose filter names something else, and the hopper pushed it on down the line.
        int cobblestoneSlot = FIRST_PLAYER_SLOT + 1;
        menu.getSlot(cobblestoneSlot).set(new ItemStack(Items.COBBLESTONE, 4));

        menu.clicked(cobblestoneSlot, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertTrue(hopper.getItem(0).isEmpty(),
                "a shift click out of the player inventory put " + hopper.getItem(0)
                        + " into hopper slot 0, whose filter names diamonds");
        helper.assertValueEqual(menu.getSlot(cobblestoneSlot).getItem().getCount(), 4,
                "cobblestone still in the player inventory after the shift click - the whole "
                        + "stack has to stay put, not just miss the first slot");

        // The hotbar swap, the one route that reaches clicked() past the screen's mouseClicked.
        player.getInventory().setItem(HOTBAR_SWAP_SLOT, new ItemStack(Items.COBBLESTONE, 4));
        menu.clicked(0, HOTBAR_SWAP_SLOT, ContainerInput.SWAP, player);
        helper.assertTrue(hopper.getItem(0).isEmpty(),
                "a hotbar swap put " + hopper.getItem(0) + " into hopper slot 0, whose filter "
                        + "names diamonds");
        helper.assertValueEqual(player.getInventory().getItem(HOTBAR_SWAP_SLOT).getCount(), 4,
                "cobblestone still in the hotbar after the swap");

        // And the drag distribution, driven the way the client sends it: a start and an end click
        // outside any slot (-999) with one click per slot the pointer crossed in between. Two
        // slots are collected on purpose - with one the menu falls back to a plain PICKUP, which
        // the filter branch above already intercepts.
        int evenSplit = AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE;
        int dragStart = AbstractContainerMenu.getQuickcraftMask(
                AbstractContainerMenu.QUICKCRAFT_HEADER_START, evenSplit);
        int dragOverSlot = AbstractContainerMenu.getQuickcraftMask(
                AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE, evenSplit);
        int dragEnd = AbstractContainerMenu.getQuickcraftMask(
                AbstractContainerMenu.QUICKCRAFT_HEADER_END, evenSplit);
        menu.setCarried(new ItemStack(Items.COBBLESTONE, 4));
        menu.clicked(-999, dragStart, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(0, dragOverSlot, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(1, dragOverSlot, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(-999, dragEnd, ContainerInput.QUICK_CRAFT, player);
        helper.assertTrue(hopper.getItem(0).isEmpty() && hopper.getItem(1).isEmpty(),
                "a drag across the filter slots left " + hopper.getItem(0) + " in slot 0 and "
                        + hopper.getItem(1) + " in slot 1");
        helper.assertValueEqual(menu.getCarried().getCount(), 4,
                "cobblestone still on the cursor after the drag");
        menu.setCarried(ItemStack.EMPTY);

        // None of the three may reconfigure the filter either: an item that lands in a slot with
        // no filter item of its own becomes that slot's filter (setItem), so a leak here does not
        // only slip material through, it rewrites the filter behind the player's back.
        helper.assertTrue(hopper.getGhostItem(0).is(Items.DIAMOND),
                "one of the three clicks rewrote filter slot 0, which now holds "
                        + hopper.getGhostItem(0));
        for (int slot = 1; slot < 5; slot++) {
            helper.assertTrue(hopper.getGhostItem(slot).isEmpty(),
                    "one of the three clicks taught filter slot " + slot + " the filter item "
                            + hopper.getGhostItem(slot));
        }

        // The positive control: what the filter does name still gets in, so the assertions above
        // are about the filter and not about a menu that stopped taking items at all.
        menu.getSlot(cobblestoneSlot).set(new ItemStack(Items.DIAMOND, 4));
        menu.clicked(cobblestoneSlot, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertTrue(hopper.getItem(0).is(Items.DIAMOND),
                "a shift click of the very item filter slot 0 names did not reach it, it holds "
                        + hopper.getItem(0));
        helper.assertValueEqual(hopper.getItem(0).getCount(), 4,
                "diamonds the shift click moved into the filtered slot");
        helper.assertTrue(menu.getSlot(cobblestoneSlot).getItem().isEmpty(),
                "the player inventory slot kept " + menu.getSlot(cobblestoneSlot).getItem()
                        + " after a shift click the filter had room for");

        // Put back the way the sections below expect to find it.
        hopper.setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(HOTBAR_SWAP_SLOT, ItemStack.EMPTY);

        // --- and with the filter off, a hopper slot takes the item like any other slot ---
        menu.clicked(FIRST_PLAYER_SLOT, 0, ContainerInput.PICKUP, player);
        helper.assertValueEqual(menu.getCarried().getCount(), 17,
                "items picked back up off the player inventory slot");
        hopper.toggleFilterMode();
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.NONE,
                "the filter should be back off, it is in " + hopper.getFilterMode());

        menu.clicked(1, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(hopper.getItem(1).is(Items.DIAMOND),
                "with the filter off the click should have filled the slot, it holds "
                        + hopper.getItem(1));
        helper.assertValueEqual(hopper.getItem(1).getCount(), 17,
                "items placed into the hopper slot with the filter off");
        helper.assertTrue(hopper.getGhostItem(1).isEmpty(),
                "a disabled filter learned a filter item anyway: " + hopper.getGhostItem(1));

        // --- a click on a menu with no block entity behind it must not dereference it ---
        // The client builds its menu with blockEntity = null, and a hotbar swap reaches clicked()
        // past the screen's mouseClicked - see the javadoc. What is asserted is the fall through:
        // the item has to land in the detached menu's own container and nothing of the real
        // hopper may move.
        NetheriteHopperScreenHandler clientMenu = new NetheriteHopperScreenHandler(
                2, player.getInventory(), helper.absolutePos(HOPPER_POS));
        player.containerMenu = clientMenu;
        clientMenu.setCarried(new ItemStack(Items.EMERALD, 5));
        clientMenu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(clientMenu.getSlot(0).getItem().is(Items.EMERALD),
                "the click on a menu without a block entity left "
                        + clientMenu.getSlot(0).getItem() + " in its first slot, so it never "
                        + "reached vanilla's own handling");
        helper.assertValueEqual(clientMenu.getSlot(0).getItem().getCount(), 5,
                "items placed into the detached menu's first slot");
        helper.assertTrue(hopper.getGhostItem(0).is(Items.DIAMOND),
                "the click on the detached menu reached the real hopper and rewrote filter slot 0, "
                        + "which now holds " + hopper.getGhostItem(0));
        player.containerMenu = opened;

        // --- not grid aligned: a solid block on top does not stop this hopper ---
        // Its own hopper, because every other rig in the suite has a chest above it and the
        // branch that reads isGridAligned only runs when there is none. Vanilla's hopper answers
        // true here and would leave the emerald lying where it is.
        helper.setBlock(GRID_PROBE_POS, ModBlocks.REINFORCED_HOPPER);
        ModHopperBlockEntity probe =
                helper.getBlockEntity(GRID_PROBE_POS, ModHopperBlockEntity.class);
        helper.setBlock(GRID_PROBE_POS.above(), Blocks.STONE);
        helper.spawnItem(Items.EMERALD, GRID_PROBE_ITEM);
        helper.assertTrue(HopperBlockEntity.suckInItems(helper.getLevel(), probe),
                "a mod hopper capped with a solid block did not take the item lying in its mouth, "
                        + "so it is refusing to pull like a grid aligned vanilla hopper");
        helper.assertTrue(probe.getItem(0).is(Items.EMERALD),
                "the item was reported as taken but slot 0 of the hopper holds "
                        + probe.getItem(0));

        // The menu was really opened, so it is really closed again: the hopper's stopOpen has to
        // run and the player must not be left holding a container into the next test.
        player.closeContainer();
        helper.succeed();
    }

    // =====================================================================================
    // REGISTRY AND RECIPE DATA
    // =====================================================================================

    /**
     * The registration lines behind the two blocks: the strength they are built with, the sound
     * they carry, the mining tag they are listed under and the fire resistance of the netherite
     * hopper's item (ModBlocks.java:53-54, ModItems.java:388,
     * {@code tags/block/mineable/pickaxe.json}). All of it is declared or generated data that a
     * port can drop without a single behaviour test noticing.
     *
     * <p>Vanilla's hopper is measured in the same breath and used as the yardstick for the
     * reinforced tier, which is built to the same 3.0 / 4.8 - so the assertion is a difference
     * between two live blocks rather than a number copied out of {@code ModBlocks}. The netherite
     * tier is then required to be a long way clear of it.
     *
     * <p>The tag half is a real datapack lookup, so it also proves the generated JSON is inside
     * the jar and loaded. A vanilla hopper is the positive control and dirt the negative one: the
     * lookup has to be able to say no. Dirt was picked by reading the merged tag rather than by
     * guessing - {@code data/minecraft/tags/block/mineable/pickaxe.json} in the 26.2 server jar
     * lists {@code minecraft:end_stone} outright, and this mod's generated file carries no
     * {@code "replace"}, so it only adds to that list. An end stone control is therefore red no
     * matter what the mod does; dirt appears neither in the vanilla list nor in any of the
     * thirteen block tags it pulls in.
     *
     * <p>The fire resistance half compares components rather than asserting a shape - the
     * netherite hopper's item has to carry the very same {@code DAMAGE_RESISTANT} value a
     * netherite ingot does - and then drives it against a real damage source.
     *
     * <p>What breaks this test: any edit to the two {@code strength(...)} or {@code sound(...)}
     * calls, removing a hopper from the tag provider or failing to regenerate the data, and
     * dropping {@code fireResistant()} from the netherite hopper item.
     */
    public static void hopperBlocksCarryTheirRegisteredStrengthSoundAndTags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos probe = helper.absolutePos(new BlockPos(1, 1, 1));

        // --- hardness, measured through the state, against a vanilla hopper ---
        float vanillaSpeed = Blocks.HOPPER.defaultBlockState().getDestroySpeed(level, probe);
        float reinforcedSpeed = ModBlocks.REINFORCED_HOPPER.defaultBlockState().getDestroySpeed(level, probe);
        float netheriteSpeed = ModBlocks.NETHERITE_HOPPER.defaultBlockState().getDestroySpeed(level, probe);

        helper.assertValueEqual(reinforcedSpeed, vanillaSpeed,
                "the reinforced hopper's hardness, against a vanilla hopper's - both are 3.0");
        helper.assertValueEqual(netheriteSpeed, 5.0F, "the netherite hopper's hardness");
        helper.assertTrue(netheriteSpeed > reinforcedSpeed,
                "the netherite hopper should be the harder one, " + netheriteSpeed + " against "
                        + reinforcedSpeed);

        // --- blast resistance ---
        helper.assertValueEqual(ModBlocks.REINFORCED_HOPPER.getExplosionResistance(),
                Blocks.HOPPER.getExplosionResistance(),
                "the reinforced hopper's blast resistance, against a vanilla hopper's");
        helper.assertValueEqual(ModBlocks.NETHERITE_HOPPER.getExplosionResistance(), 1200.0F,
                "the netherite hopper's blast resistance");
        helper.assertTrue(ModBlocks.NETHERITE_HOPPER.getExplosionResistance()
                        > Blocks.HOPPER.getExplosionResistance() * 100.0F,
                "the netherite hopper is supposed to be the blast proof one, but its resistance of "
                        + ModBlocks.NETHERITE_HOPPER.getExplosionResistance() + " is no better than "
                        + "vanilla's " + Blocks.HOPPER.getExplosionResistance());

        // --- sound ---
        helper.assertTrue(ModBlocks.REINFORCED_HOPPER.defaultBlockState().getSoundType() == SoundType.METAL,
                "the reinforced hopper lost its metal sound");
        helper.assertTrue(ModBlocks.NETHERITE_HOPPER.defaultBlockState().getSoundType() == SoundType.NETHERITE_BLOCK,
                "the netherite hopper lost its netherite block sound");

        // --- mining tag, with a control on both sides ---
        assertPickaxeMineable(helper, ModBlocks.REINFORCED_HOPPER, true);
        assertPickaxeMineable(helper, ModBlocks.NETHERITE_HOPPER, true);
        assertPickaxeMineable(helper, Blocks.HOPPER, true);
        // Dirt, not end stone: end stone is a plain value in vanilla's own pickaxe tag (see the
        // javadoc), so it can never be the "the lookup says no" side of this pair.
        assertPickaxeMineable(helper, Blocks.DIRT, false);

        // --- and in no tool tier tag: a stone pickaxe has to be enough ---
        for (TagKey<Block> tier : List.of(BlockTags.NEEDS_STONE_TOOL, BlockTags.NEEDS_IRON_TOOL,
                BlockTags.NEEDS_DIAMOND_TOOL)) {
            helper.assertFalse(ModBlocks.REINFORCED_HOPPER.defaultBlockState().is(tier),
                    "the reinforced hopper was put into " + tier.location());
            helper.assertFalse(ModBlocks.NETHERITE_HOPPER.defaultBlockState().is(tier),
                    "the netherite hopper was put into " + tier.location());
        }

        // --- fire resistance of the netherite hopper's item ---
        DamageResistant netheriteHopper = new ItemStack(ModItems.NETHERITE_HOPPER).get(DataComponents.DAMAGE_RESISTANT);
        DamageResistant netheriteIngot = new ItemStack(Items.NETHERITE_INGOT).get(DataComponents.DAMAGE_RESISTANT);
        helper.assertTrue(netheriteIngot != null,
                "a netherite ingot no longer carries a DAMAGE_RESISTANT component, so this test has "
                        + "lost its yardstick");
        helper.assertTrue(netheriteHopper != null,
                "the netherite hopper item carries no DAMAGE_RESISTANT component at all, so "
                        + "fireResistant() is gone from its registration and a dropped one burns");
        // Compared by tag key rather than by component identity: the two HolderSets behind the
        // component are named sets, and those do not implement equals.
        helper.assertValueEqual(netheriteHopper.types().unwrapKey(), netheriteIngot.types().unwrapKey(),
                "the damage types the netherite hopper resists, against a netherite ingot's");
        helper.assertTrue(netheriteHopper.isResistantTo(level.damageSources().lava()),
                "a dropped netherite hopper should survive lava");
        helper.assertFalse(netheriteHopper.isResistantTo(level.damageSources().drown()),
                "the netherite hopper resists drowning as well, so its resistance is not the fire "
                        + "tag any more and the lava assertion above proves nothing");
        helper.assertTrue(
                new ItemStack(ModItems.REINFORCED_HOPPER).get(DataComponents.DAMAGE_RESISTANT) == null,
                "the reinforced hopper item is fire resistant too, which makes the assertions above "
                        + "meaningless - it is the netherite tier that is supposed to survive lava");

        helper.succeed();
    }

    /**
     * The two crafting recipes, driven through the live {@code RecipeManager} with a real
     * crafting grid: pattern, ingredients, result and - the part
     * {@code DataIntegrityTests#modRecipesOnlyReferenceRegisteredItems} never reads - the count.
     * Five reinforced hoppers per craft and two netherite ones are what make the chain worth
     * running at all; a recipe that silently yielded one would pass every other test in the tree.
     *
     * <p>Each recipe is also offered upside down. Vanilla matches a shaped recipe against its own
     * pattern and the mirror of it, never against a vertical flip, so a match on the flipped
     * grids would mean the shape is not being checked at all. The reinforced pattern's rows are
     * palindromes, which is why the flip - and not the mirror - is the control that says
     * something here.
     *
     * <p>What breaks this test: any edit to the two generated recipe JSONs - a different pattern,
     * a swapped ingredient, another count - and either of them failing to load.
     */
    public static void hopperRecipesCraftFromTheirDocumentedPatterns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // "HNH" / "DDD" / "HHH": five vanilla hoppers, a name tag and three cracked diamonds.
        CraftingInput reinforced = CraftingInput.of(3, 3, List.of(
                stack(Items.HOPPER), stack(Items.NAME_TAG), stack(Items.HOPPER),
                stack(ModItems.CRACKED_DIAMOND), stack(ModItems.CRACKED_DIAMOND), stack(ModItems.CRACKED_DIAMOND),
                stack(Items.HOPPER), stack(Items.HOPPER), stack(Items.HOPPER)));
        assertCrafts(helper, level, reinforced, "simplebuilding:reinforced_hopper_from_crafting",
                ModItems.REINFORCED_HOPPER, 5);

        CraftingInput reinforcedFlipped = CraftingInput.of(3, 3, List.of(
                stack(Items.HOPPER), stack(Items.HOPPER), stack(Items.HOPPER),
                stack(ModItems.CRACKED_DIAMOND), stack(ModItems.CRACKED_DIAMOND), stack(ModItems.CRACKED_DIAMOND),
                stack(Items.HOPPER), stack(Items.NAME_TAG), stack(Items.HOPPER)));
        assertCraftsNothing(helper, level, reinforcedFlipped,
                "the reinforced hopper pattern turned upside down");

        // "H" / "N" / "H": two reinforced hoppers around one netherite nugget.
        CraftingInput netherite = CraftingInput.of(1, 3, List.of(
                stack(ModItems.REINFORCED_HOPPER),
                stack(ModItems.NETHERITE_NUGGET),
                stack(ModItems.REINFORCED_HOPPER)));
        assertCrafts(helper, level, netherite, "simplebuilding:netherite_hopper_from_crafting",
                ModItems.NETHERITE_HOPPER, 2);

        CraftingInput netheriteShuffled = CraftingInput.of(1, 3, List.of(
                stack(ModItems.NETHERITE_NUGGET),
                stack(ModItems.REINFORCED_HOPPER),
                stack(ModItems.REINFORCED_HOPPER)));
        assertCraftsNothing(helper, level, netheriteShuffled,
                "the netherite hopper column with the nugget on top");

        helper.succeed();
    }

    /**
     * Both hoppers are actually broken and the drop is picked up off the ground.
     * {@code DataIntegrityTests} proves each of them resolves a loot table that loaded, which is
     * a different claim: a table can load and hand back the wrong item, or nothing at all.
     *
     * <p>{@code GameTestHelper#destroyBlock} deliberately drops nothing, so the break goes
     * through {@code ServerLevel#destroyBlock(pos, true)}. The two hoppers stand five blocks
     * apart and each drop is counted inside {@value #DROP_RADIUS} blocks of its own site, so
     * neither can be satisfied by the other one's item - and the search radius is never widened
     * past the room, which would reach into the neighbouring test's structure.
     *
     * <p>What breaks this test: either loot table naming a different item, or one of the two
     * blocks losing its table and dropping nothing.
     */
    public static void bothHoppersDropThemselvesWhenBroken(GameTestHelper helper) {
        helper.setBlock(REINFORCED_DROP_POS, ModBlocks.REINFORCED_HOPPER);
        helper.setBlock(NETHERITE_DROP_POS, ModBlocks.NETHERITE_HOPPER);
        helper.assertBlockPresent(ModBlocks.REINFORCED_HOPPER, REINFORCED_DROP_POS);
        helper.assertBlockPresent(ModBlocks.NETHERITE_HOPPER, NETHERITE_DROP_POS);

        helper.assertTrue(helper.getLevel().destroyBlock(helper.absolutePos(REINFORCED_DROP_POS), true),
                "could not break the reinforced hopper");
        helper.assertTrue(helper.getLevel().destroyBlock(helper.absolutePos(NETHERITE_DROP_POS), true),
                "could not break the netherite hopper");

        helper.runAfterDelay(3, () -> {
            helper.assertBlockNotPresent(ModBlocks.REINFORCED_HOPPER, REINFORCED_DROP_POS);
            helper.assertBlockNotPresent(ModBlocks.NETHERITE_HOPPER, NETHERITE_DROP_POS);
            helper.assertItemEntityCountIs(ModItems.REINFORCED_HOPPER, REINFORCED_DROP_POS, DROP_RADIUS, 1);
            helper.assertItemEntityCountIs(ModItems.NETHERITE_HOPPER, NETHERITE_DROP_POS, DROP_RADIUS, 1);
            // Negative control: neither table falls back to the vanilla hopper it is crafted from.
            helper.assertItemEntityNotPresent(Items.HOPPER);
            helper.succeed();
        });
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /** Places one hopper at {@link #HOPPER_POS} and hands back its block entity. */
    private static ModHopperBlockEntity placeHopper(GameTestHelper helper, Block hopper) {
        helper.setBlock(HOPPER_POS, hopper);
        return helper.getBlockEntity(HOPPER_POS, ModHopperBlockEntity.class);
    }

    /**
     * Fails unless the mode's text carries the colour its tooltip is drawn in.
     *
     * <p>Asked for on the component rather than on {@code HopperFilterMode#getColor()}: the enum's
     * own accessor has no reader in the mod, so only the {@code Style} says anything about what
     * the player is shown.
     */
    private static void assertModeTextColour(GameTestHelper helper, HopperFilterMode mode,
                                             int expected, String what) {
        TextColor colour = mode.getText().getStyle().getColor();
        helper.assertTrue(colour != null,
                "the " + what + " mode text carries no colour of its own, so the tooltip on the "
                        + "filter button is drawn in plain white");
        helper.assertValueEqual(colour.getValue(), expected,
                "the colour the " + what + " mode text is styled with");
    }

    /** One recorded {@code PlatformServices.broadcastHopperGhostItem} call. */
    private record GhostBroadcast(ModHopperBlockEntity hopper, int slot, ItemStack stack) {
    }

    /**
     * Puts a recorder in front of the {@link HopperSync} the loader installed and hands back the
     * log it fills.
     *
     * <p>The recorder <em>wraps</em> rather than replaces. {@code PlatformServices} has a setter
     * for the sync but no getter, so the implementation is read off the field - and wrapping it
     * means that even a test that dies half way through, or a line whose clean-up hook only fires
     * on the success path, leaves every later hopper broadcasting for real instead of into a dead
     * {@code NOOP}.
     *
     * <p>The whole suite shares one server thread, so a plain list is enough; entries from other
     * tests running in the same batch are told apart by the block entity they name.
     */
    private static List<GhostBroadcast> recordGhostBroadcasts(GameTestHelper helper) {
        HopperSync installed = installedHopperSync();
        List<GhostBroadcast> log = new ArrayList<>();
        PlatformServices.setHopperSync((blockEntity, slot, stack) -> {
            // Copied: the caller keeps its stack and is free to edit it afterwards, which is
            // exactly what the test around this does.
            log.add(new GhostBroadcast(blockEntity, slot, stack.copy()));
            installed.broadcastGhostItem(blockEntity, slot, stack);
        });
        helper.runBeforeTestEnd(() -> PlatformServices.setHopperSync(installed));
        return log;
    }

    private static HopperSync installedHopperSync() {
        try {
            Field field = PlatformServices.class.getDeclaredField("hopperSync");
            field.setAccessible(true);
            return (HopperSync) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            throw new IllegalStateException("PlatformServices.hopperSync could not be read, so a "
                    + "recorder could not be installed in front of it without losing the loader's "
                    + "own sync for the rest of the run", failure);
        }
    }

    /** The most recent broadcast for this hopper; fails if the call sent none at all. */
    private static GhostBroadcast lastBroadcastFor(GameTestHelper helper, List<GhostBroadcast> log,
                                                   ModHopperBlockEntity hopper, String what) {
        GhostBroadcast found = null;
        for (int i = log.size() - 1; i >= 0 && found == null; i--) {
            if (log.get(i).hopper() == hopper) {
                found = log.get(i);
            }
        }
        helper.assertTrue(found != null,
                what + " sent no sync packet to the tracking clients, so the filter changed on "
                        + "the server only");
        return found;
    }

    /**
     * Builds source chest -&gt; netherite hopper (facing down) -&gt; destination chest and fills
     * the source. The hopper is placed <em>enabled</em> on purpose; whether it stays that way is
     * the thing under test.
     */
    private static void buildHopperStack(GameTestHelper helper, BlockPos hopperPos) {
        helper.setBlock(hopperPos.below(), Blocks.CHEST);
        helper.setBlock(hopperPos, ModBlocks.NETHERITE_HOPPER.defaultBlockState()
                .setValue(HopperBlock.FACING, Direction.DOWN)
                .setValue(HopperBlock.ENABLED, Boolean.TRUE));
        helper.setBlock(hopperPos.above(), Blocks.CHEST);

        ChestBlockEntity source = helper.getBlockEntity(hopperPos.above(), ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.COBBLESTONE, SOURCE_STACK));
    }

    private static int countItems(GameTestHelper helper, BlockPos chestPos) {
        ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        int total = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            total += chest.getItem(slot).getCount();
        }
        return total;
    }

    /**
     * Sends the payload through its own stream codec and reads it back, the way the network would.
     * The buffer is released again so a failing assertion does not leave one behind.
     */
    private static SetHopperGhostItemPayload roundTrip(GameTestHelper helper,
                                                       SetHopperGhostItemPayload payload) {
        ByteBuf raw = Unpooled.buffer();
        try {
            RegistryFriendlyByteBuf buffer =
                    new RegistryFriendlyByteBuf(raw, helper.getLevel().registryAccess());
            SetHopperGhostItemPayload.CODEC.encode(buffer, payload);
            return SetHopperGhostItemPayload.CODEC.decode(buffer);
        } finally {
            raw.release();
        }
    }

    private static void assertPickaxeMineable(GameTestHelper helper, Block block, boolean expected) {
        helper.assertValueEqual(block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), expected,
                block.getName().getString() + " in minecraft:mineable/pickaxe");
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    private static void assertCrafts(GameTestHelper helper, ServerLevel level, CraftingInput grid,
                                     String recipeId, Item expected, int count) {
        Optional<RecipeHolder<CraftingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(match.isPresent(),
                "the documented pattern for " + recipeId + " matches no crafting recipe at all, so "
                        + "the block cannot be crafted in game");
        RecipeHolder<CraftingRecipe> holder = match.get();
        helper.assertValueEqual(holder.id().identifier().toString(), recipeId,
                "recipe matched by the documented pattern");

        ItemStack result = holder.value().assemble(grid);
        helper.assertTrue(result.is(expected),
                recipeId + " produced " + result + " instead of the expected item");
        helper.assertValueEqual(result.getCount(), count, recipeId + ": items produced per craft");
    }

    private static void assertCraftsNothing(GameTestHelper helper, ServerLevel level,
                                            CraftingInput grid, String what) {
        helper.assertTrue(
                level.getServer().getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(),
                what + " crafts something as well, so the recipe is not shaped the way the data "
                        + "says it is");
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(PLAYER_POS);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // Hand the player back no matter how the test ends. A leaked mock player keeps the player
        // list non-empty and the gametest server then stalls on shutdown - a failing test would
        // cost minutes of wall clock instead of seconds.
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }
}
