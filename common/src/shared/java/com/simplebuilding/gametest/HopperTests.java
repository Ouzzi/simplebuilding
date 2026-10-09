package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.screen.ModHopperScreenHandler;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
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
 * The reinforced and the netherite hopper: the redstone lock, the item filter of the five slots
 * (filter principle since 2026-10-09: the real item in a slot is its filter, one always stays), the menu, and the registration data the two blocks are built
 * from.
 *
 * <h2>What is already covered elsewhere, and is not repeated here</h2>
 * <ul>
 *   <li><b>The three filter modes as a gate</b> -
 *       {@code HopperAndTrimTests#hopperFilterModesGateWhatMayEnter} drives
 *       {@code canPlaceItem} through Disabled / Exact Match / Type Match. This file only reaches
 *       for {@code canPlaceItem} where it is the <em>proof</em> that something else worked (a
 *       reloaded filter, a filter written through the property delegate).</li>
 *   <li><b>The filter key's payload</b> -
 *       {@code HopperAndTrimTests#hopperPayloadsOnlyActOnAnOpenHopperMenu} pins that it refuses
 *       to act unless that hopper's menu is open.</li>
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
 * <p>All three mod hoppers use {@code ModHopperBlockEntity} ({@code ModHopperBlock#newBlockEntity},
 * {@code ModBlockEntities} registers {@code ModHopperBlockEntity::new}). The never constructed
 * {@code NetheriteHopperBlockEntity} was removed on 2026-09-25.
 *
 * <h2>Not pinned, and why</h2>
 * <ul>
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
 *   <li><b>Everything the player sees.</b> {@code NetheriteHopperScreen} and the filter key
 *       are client side. The mode texts and the colour their tooltip is
 *       drawn in live in shared code and are pinned below - as the {@code Style} on the component
 *       the screen hands to {@code setTooltipForNextFrame}, which is the colour that actually
 *       reaches the player - but nothing draws them here.</li>
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

    /** The hotbar slot {@link #hopperMenuOpensOnUseAndFilterSlotsTakeOnlyTheirItem} swaps from. */
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

    /** Tick budget for {@link #aFilteredHopperKeepsOneFilterItemInEverySlot}. */
    public static final int KEEP_ONE_MAX_TICKS = 100;

    /**
     * The owner's filter principle (docs/ai/PRINZIPIEN-FILTER.md): with a filter on, the real item in a slot is the
     * filter and one of it always stays - the hopper pushes only the second and every further one. A netherite hopper
     * over a chest gets five diamonds in slot 0 and a single emerald in slot 1; once four diamonds have arrived below,
     * slot 0 has to hold exactly one, the emerald must not have moved, and both stay put for a few more transfers.
     * Then the filter is switched off as the control: now the last diamond and the emerald leave as well.
     *
     * <p>What breaks this test: {@code insert} pushing the last item of a slot again (no {@code ItemFilter.movable}
     * check), or the check also holding items back with the filter off.
     */
    public static void aFilteredHopperKeepsOneFilterItemInEverySlot(GameTestHelper helper) {
        helper.setBlock(HOPPER_POS.below(), Blocks.CHEST);
        helper.setBlock(HOPPER_POS, ModBlocks.NETHERITE_HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ModHopperBlockEntity hopper = helper.getBlockEntity(HOPPER_POS, ModHopperBlockEntity.class);
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the toggle did not reach Exact Match, it is " + hopper.getFilterMode());
        hopper.setItem(0, new ItemStack(Items.DIAMOND, 5));
        hopper.setItem(1, new ItemStack(Items.EMERALD));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(countItems(helper, HOPPER_POS.below()), 4,
                        "items the filtered hopper pushed into the chest"))
                .thenIdle(10)
                .thenExecute(() -> {
                    helper.assertValueEqual(countItems(helper, HOPPER_POS.below()), 4,
                            "items in the chest a while later - the filter item of a slot must never leave");
                    helper.assertTrue(hopper.getItem(0).is(Items.DIAMOND) && hopper.getItem(0).getCount() == 1,
                            "slot 0 should keep exactly one diamond as its filter, it holds " + hopper.getItem(0));
                    helper.assertTrue(hopper.getItem(1).is(Items.EMERALD) && hopper.getItem(1).getCount() == 1,
                            "the single emerald is slot 1's filter and must stay, the slot holds " + hopper.getItem(1));
                    hopper.toggleFilterMode();
                    hopper.toggleFilterMode();
                    helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.NONE,
                            "two more toggles should switch the filter off, it is " + hopper.getFilterMode());
                })
                .thenWaitUntil(() -> helper.assertTrue(hopper.isEmpty(),
                        "with the filter off the last items have to leave as well, the hopper still holds "
                                + hopper.getItem(0) + " and " + hopper.getItem(1)))
                .thenExecute(() -> helper.assertValueEqual(countItems(helper, HOPPER_POS.below()), 6,
                        "items in the chest once the filter is off"))
                .thenSucceed();
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
     * What may go in and out of a filtered hopper (filter principle): automation only tops up a slot that already holds
     * a match - an empty slot takes nothing while a filter is on - while a player may put anything into an empty slot
     * (that is how a filter is set) and only a match onto a filled one. Pulling (another hopper below, a pipe) may only
     * take what lies above the one filter item. Exact Match compares the components, Same Kind the item only. Then a
     * real pull: a chest above holds stone and diamonds, the hopper's filter is one diamond - only the diamond comes.
     *
     * <p>What breaks this test: {@code canPlaceItem} accepting an empty slot with the filter on, the menu rule
     * ({@code mayPlayerPlace}) refusing an empty slot, {@code canTakeItem}/{@code canTakeItemThroughFace} letting the
     * last item go, or the two modes swapping their comparison.
     */
    public static void automationOnlyTopsUpMatchingSlots(GameTestHelper helper) {
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.NETHERITE_HOPPER);
        ItemStack namedDiamond = new ItemStack(Items.DIAMOND);
        namedDiamond.set(DataComponents.CUSTOM_NAME, Component.literal("a very particular diamond"));

        // --- filter off: like a vanilla hopper ---
        helper.assertTrue(hopper.canPlaceItem(0, new ItemStack(Items.STONE)), "a hopper without filter refuses stone");
        hopper.setItem(0, new ItemStack(Items.DIAMOND));
        helper.assertTrue(hopper.canTakeItemThroughFace(0, hopper.getItem(0), Direction.DOWN),
                "with the filter off even the last item of a slot may be pulled");

        // --- Exact Match ---
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the toggle did not reach Exact Match, it is " + hopper.getFilterMode());
        helper.assertTrue(hopper.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "slot 0 refuses the diamond it holds");
        helper.assertFalse(hopper.canPlaceItem(0, new ItemStack(Items.STONE)), "slot 0 (diamond) accepts stone");
        helper.assertFalse(hopper.canPlaceItem(0, namedDiamond), "Exact Match accepts a renamed diamond");
        helper.assertFalse(hopper.canPlaceItem(1, new ItemStack(Items.DIAMOND)),
                "an empty slot accepts items from automation while a filter is on");
        helper.assertTrue(hopper.mayPlayerPlace(1, new ItemStack(Items.STONE)),
                "a player cannot put an item into an empty slot, so no filter can be set");
        helper.assertFalse(hopper.mayPlayerPlace(0, new ItemStack(Items.STONE)),
                "a player may put stone onto slot 0's diamond");
        helper.assertFalse(hopper.canTakeItemThroughFace(0, hopper.getItem(0), Direction.DOWN),
                "the one filter diamond may be pulled out of the hopper");
        helper.assertFalse(hopper.canTakeItem(hopper, 0, hopper.getItem(0)),
                "canTakeItem lets the one filter diamond go");
        hopper.setItem(0, new ItemStack(Items.DIAMOND, 2));
        helper.assertTrue(hopper.canTakeItemThroughFace(0, hopper.getItem(0), Direction.DOWN),
                "the second diamond above the filter item may not be pulled");

        // --- Same Kind ---
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.TYPE,
                "the toggle did not reach Same Kind, it is " + hopper.getFilterMode());
        helper.assertTrue(hopper.canPlaceItem(0, namedDiamond), "Same Kind refuses a renamed diamond");
        helper.assertFalse(hopper.canPlaceItem(0, new ItemStack(Items.STONE)), "Same Kind accepts stone onto diamonds");

        // --- a real pull from a chest above ---
        hopper.setItem(0, new ItemStack(Items.DIAMOND));
        helper.setBlock(HOPPER_POS.above(), Blocks.CHEST);
        ChestBlockEntity source = helper.getBlockEntity(HOPPER_POS.above(), ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.STONE, 8));
        source.setItem(1, new ItemStack(Items.DIAMOND, 3));
        HopperBlockEntity.suckInItems(helper.getLevel(), hopper);
        helper.assertValueEqual(hopper.getItem(0).getCount(), 2, "diamonds in slot 0 after one pull");
        helper.assertValueEqual(source.getItem(0).getCount(), 8, "stone left in the chest - the filter let stone in");
        for (int slot = 1; slot < 5; slot++) {
            helper.assertTrue(hopper.getItem(slot).isEmpty(), "the pull filled empty slot " + slot + " with "
                    + hopper.getItem(slot));
        }
        helper.succeed();
    }

    /**
     * The mode is written under {@code FilterMode} and read back in {@code loadAdditional}; the filter itself is the
     * hopper's own five slots (filter principle), which ride along on {@code ContainerHelper}. A hopper that is
     * configured and then unloaded has to come back filtering the same way.
     *
     * <p>The round trip is run against a second, detached block entity, and the reloaded copy is judged through
     * {@code canPlaceItem}, so the claim is that the <em>filter</em> came back, not that a couple of fields did.
     *
     * <p><b>Saves from before the filter principle</b> carry a {@code GhostItems} list. It is ignored on purpose (no
     * real item can be made up from a ghost): the slot it named stays empty and, with the filter on, takes nothing.
     *
     * <p><b>The last two loads feed back a mode the hopper never wrote.</b> An ordinal outside {@code 0..2} used to
     * index {@code HopperFilterMode.values()} out of bounds, and {@code BlockEntity#loadStatic} then drops the whole
     * block entity. So the claim is not only that nothing throws but that the contents still came back.
     *
     * <p>What breaks this test: renaming {@code FilterMode} on one side only, dropping the range check around the
     * saved mode, reading the old ghost list back into the slots, or the {@code ContainerHelper} format changing.
     */
    public static void hopperConfigurationSurvivesTheSaveAndLoadRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.NETHERITE_HOPPER);

        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("a very particular stone"));

        hopper.setItem(0, new ItemStack(Items.DIAMOND, 9));
        hopper.setItem(1, new ItemStack(Items.COBBLESTONE, 7));
        hopper.setItem(4, namedStone);
        hopper.toggleFilterMode();
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.TYPE,
                "two toggles should reach Same Kind, the hopper is in " + hopper.getFilterMode());

        CompoundTag saved = hopper.saveCustomOnly(level.registryAccess());
        // A ghost list as saves before 2026-10-09 wrote it: a diamond for slot 2.
        CompoundTag oldGhost = new CompoundTag();
        oldGhost.putByte("Slot", (byte) 2);
        oldGhost.putString("id", "minecraft:diamond");
        oldGhost.putInt("count", 1);
        ListTag oldGhosts = new ListTag();
        oldGhosts.add(oldGhost);
        CompoundTag ghostRoot = new CompoundTag();
        ghostRoot.put("Items", oldGhosts);
        saved.put("GhostItems", ghostRoot);

        ModHopperBlockEntity reloaded = new ModHopperBlockEntity(
                helper.absolutePos(HOPPER_POS), ModBlocks.NETHERITE_HOPPER.defaultBlockState());
        reloaded.loadCustomOnly(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), saved));

        helper.assertTrue(reloaded.getFilterMode() == HopperFilterMode.TYPE,
                "the filter mode did not survive the save and load round trip, it came back as "
                        + reloaded.getFilterMode());
        helper.assertValueEqual(reloaded.getItem(0).getCount(), 9, "diamonds in slot 0 after the round trip");
        helper.assertTrue(ItemStack.isSameItemSameComponents(reloaded.getItem(4), namedStone),
                "slot 4 lost its components on the way through the save, it came back as " + reloaded.getItem(4));
        helper.assertValueEqual(reloaded.getItem(1).getCount(), 7, "cobblestone in slot 1 after the round trip");

        // The filter is judged by what it does, not by what it stored.
        helper.assertTrue(reloaded.canPlaceItem(0, new ItemStack(Items.DIAMOND)),
                "the reloaded hopper refuses the item its slot 0 holds");
        helper.assertFalse(reloaded.canPlaceItem(0, new ItemStack(Items.STONE)),
                "the reloaded hopper lets anything into slot 0, so its filter mode came back off");
        helper.assertTrue(reloaded.canPlaceItem(4, new ItemStack(Items.STONE)),
                "Same Kind did not survive: slot 4 refuses a plain stone although only the item type matters");
        helper.assertTrue(reloaded.getItem(2).isEmpty(),
                "the old ghost list was turned into a real item in slot 2: " + reloaded.getItem(2));
        helper.assertFalse(reloaded.canPlaceItem(2, new ItemStack(Items.DIAMOND)),
                "the slot an old ghost item named still filters diamonds in, so the ghost list was read back");

        // --- and a mode the hopper never wrote: it falls back instead of throwing ---
        saved.putInt("FilterMode", HopperFilterMode.values().length);
        ModHopperBlockEntity pastTheLastMode = new ModHopperBlockEntity(
                helper.absolutePos(HOPPER_POS), ModBlocks.NETHERITE_HOPPER.defaultBlockState());
        pastTheLastMode.loadCustomOnly(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), saved));
        helper.assertTrue(pastTheLastMode.getFilterMode() == HopperFilterMode.NONE,
                "a hopper loaded with an unknown filter mode came back in "
                        + pastTheLastMode.getFilterMode() + " instead of falling back to Disabled");
        // Driven: Disabled is the only mode that lets an item into an empty slot.
        helper.assertTrue(pastTheLastMode.canPlaceItem(2, new ItemStack(Items.COBBLESTONE)),
                "the fallback still filters, so the unknown ordinal was never really replaced");
        helper.assertTrue(pastTheLastMode.getItem(1).is(Items.COBBLESTONE),
                "the hopper's own contents went down with the unknown mode, slot 1 holds "
                        + pastTheLastMode.getItem(1));

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
     * {@code getUpdateTag} is what a client gets when the hopper enters its view: it carries the filter mode (for
     * Jade and the block's own client copy). The filter items are the slots, so no ghost list may travel any more.
     * The tag is fed back into a fresh block entity, the same reader the client ends up using.
     *
     * <p>What breaks this test: dropping {@code FilterMode} from the tag, or a ghost list coming back.
     */
    public static void theUpdateTagCarriesTheFilterModeToTheClient(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.NETHERITE_HOPPER);
        hopper.toggleFilterMode();
        hopper.setItem(2, new ItemStack(Items.DIAMOND, 5));

        CompoundTag update = hopper.getUpdateTag(level.registryAccess());
        helper.assertValueEqual(update.getIntOr("FilterMode", -1), HopperFilterMode.WHITELIST.ordinal(),
                "the filter mode in the update tag");
        helper.assertFalse(update.contains("GhostItems"), "the update tag still carries a ghost item list");

        ModHopperBlockEntity clientSide = new ModHopperBlockEntity(
                helper.absolutePos(HOPPER_POS), ModBlocks.NETHERITE_HOPPER.defaultBlockState());
        clientSide.loadCustomOnly(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), update));
        helper.assertTrue(clientSide.getFilterMode() == HopperFilterMode.WHITELIST,
                "a hopper rebuilt from the update tag is in " + clientSide.getFilterMode());
        helper.succeed();
    }

    /**
     * The menu: right clicking a hopper opens the mod's own one ({@code ModHopperBlock#useWithoutItem}), backed by the
     * clicked block entity (a marker item in slot 2 has to show), and its five slots follow the filter principle: with
     * a filter on, a click puts the real item in (that is the filter), and every click kind vanilla knows - hotbar
     * swap, drag, shift click - only puts a match onto a filled slot; an empty slot takes anything. Taking the stack
     * out is a plain click and clears the filter. A menu built the client's way (no block entity) still works, and
     * {@code isGridAligned} stays {@code false} (a capped mod hopper still takes the item lying in its mouth).
     *
     * <p>What breaks this test: {@code useWithoutItem} falling back to vanilla's hopper menu, {@code createScreenMenu}
     * switching to the client constructor, putting the plain {@code Slot}s back in place of the filtering ones, the
     * filter slot refusing an empty slot, or {@code isGridAligned} starting to answer {@code true}.
     */
    public static void hopperMenuOpensOnUseAndFilterSlotsTakeOnlyTheirItem(GameTestHelper helper) {
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

        // --- with a filter on, a click puts the real item in: it is that slot's filter now ---
        hopper.setItem(2, ItemStack.EMPTY);
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the toggle did not reach Exact Match, it is " + hopper.getFilterMode());

        menu.setCarried(new ItemStack(Items.DIAMOND, 17));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(hopper.getItem(0).is(Items.DIAMOND) && hopper.getItem(0).getCount() == 17,
                "a click with diamonds on an empty filtered slot did not put them in (filter principle: the real "
                        + "item is the filter), slot 0 holds " + hopper.getItem(0));
        helper.assertTrue(menu.getCarried().isEmpty(), "the cursor kept " + menu.getCarried());

        // --- the click kinds the screen never gets to eat obey the filter as well ---
        // A shift click, a hotbar swap and a drag reach the menu on their own; vanilla gates them on Slot#mayPlace.
        int cobblestoneSlot = FIRST_PLAYER_SLOT + 1;
        menu.getSlot(cobblestoneSlot).set(new ItemStack(Items.COBBLESTONE, 4));
        player.getInventory().setItem(HOTBAR_SWAP_SLOT, new ItemStack(Items.COBBLESTONE, 4));
        menu.clicked(0, HOTBAR_SWAP_SLOT, ContainerInput.SWAP, player);
        helper.assertTrue(hopper.getItem(0).is(Items.DIAMOND) && hopper.getItem(0).getCount() == 17,
                "a hotbar swap replaced the diamonds of filter slot 0 with " + hopper.getItem(0));
        helper.assertValueEqual(player.getInventory().getItem(HOTBAR_SWAP_SLOT).getCount(), 4,
                "cobblestone still in the hotbar after the swap");

        // Drag across slot 0 (diamonds) and slot 2 (empty): only the empty one may take the cobblestone.
        int evenSplit = AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE;
        int dragStart = AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_START, evenSplit);
        int dragOverSlot = AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE, evenSplit);
        int dragEnd = AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_END, evenSplit);
        menu.setCarried(new ItemStack(Items.COBBLESTONE, 4));
        menu.clicked(-999, dragStart, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(0, dragOverSlot, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(2, dragOverSlot, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(-999, dragEnd, ContainerInput.QUICK_CRAFT, player);
        helper.assertTrue(hopper.getItem(0).is(Items.DIAMOND) && hopper.getItem(0).getCount() == 17,
                "a drag put cobblestone onto filter slot 0's diamonds: " + hopper.getItem(0));
        helper.assertTrue(hopper.getItem(2).is(Items.COBBLESTONE),
                "the drag did not fill the empty slot 2, it holds " + hopper.getItem(2));
        menu.setCarried(ItemStack.EMPTY);
        hopper.setItem(2, ItemStack.EMPTY);

        // A shift click of diamonds tops up the diamond slot.
        menu.getSlot(cobblestoneSlot).set(new ItemStack(Items.DIAMOND, 4));
        menu.clicked(cobblestoneSlot, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertValueEqual(hopper.getItem(0).getCount(), 21, "diamonds in filter slot 0 after a shift click");
        helper.assertTrue(menu.getSlot(cobblestoneSlot).getItem().isEmpty(),
                "the player inventory slot kept " + menu.getSlot(cobblestoneSlot).getItem());

        // A plain click takes the whole stack out again - the filter is gone with it.
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertValueEqual(menu.getCarried().getCount(), 21, "diamonds picked up off filter slot 0");
        helper.assertTrue(hopper.getItem(0).isEmpty(), "filter slot 0 still holds " + hopper.getItem(0));
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(HOTBAR_SWAP_SLOT, ItemStack.EMPTY);

        // --- a click on a menu with no block entity behind it must not dereference it ---
        // The client builds its menu with blockEntity = null; its slots are filter slots without a hopper.
        NetheriteHopperScreenHandler clientMenu = new NetheriteHopperScreenHandler(
                2, player.getInventory(), helper.absolutePos(HOPPER_POS));
        player.containerMenu = clientMenu;
        clientMenu.setCarried(new ItemStack(Items.EMERALD, 5));
        clientMenu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(clientMenu.getSlot(0).getItem().is(Items.EMERALD),
                "the click on a menu without a block entity left "
                        + clientMenu.getSlot(0).getItem() + " in its first slot");
        helper.assertTrue(hopper.getItem(0).isEmpty(),
                "the click on the detached menu reached the real hopper: " + hopper.getItem(0));
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
     * The registration lines behind the three blocks: the strength they are built with, the sound
     * they carry, the mining tag they are listed under and the fire resistance of the netherite
     * and enderite hoppers' items ({@code ModBlocks}, {@code ModItems},
     * {@code tags/block/mineable/pickaxe.json}). The enderite hopper is made only in the world
     * (sledgehammer and enderite nugget on a netherite hopper) and is built to 6.0 / 1500 like the
     * enderite piston. All of it is declared or generated data that a
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
     * <p>Both hoppers are built from vanilla's hopper (until 2026-09: from glass, so a bare hand
     * dropped them), and the tool requirement that copy brings is pinned against vanilla's hopper.
     *
     * <p>What breaks this test: any edit to the three {@code strength(...)} or {@code sound(...)}
     * calls, removing a hopper from the tag provider or failing to regenerate the data, building a
     * hopper from another base than vanilla's hopper, and dropping {@code fireResistant()} from the
     * netherite or the enderite hopper item.
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
        float enderiteSpeed = ModBlocks.ENDERITE_HOPPER.defaultBlockState().getDestroySpeed(level, probe);
        helper.assertValueEqual(enderiteSpeed, 6.0F, "the enderite hopper's hardness");
        helper.assertValueEqual(ModBlocks.ENDERITE_HOPPER.getExplosionResistance(), 1500.0F,
                "the enderite hopper's blast resistance");

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
        helper.assertTrue(ModBlocks.ENDERITE_HOPPER.defaultBlockState().getSoundType() == SoundType.NETHERITE_BLOCK,
                "the enderite hopper lost its netherite block sound");

        // --- mining tag, with a control on both sides ---
        assertPickaxeMineable(helper, ModBlocks.REINFORCED_HOPPER, true);
        assertPickaxeMineable(helper, ModBlocks.NETHERITE_HOPPER, true);
        assertPickaxeMineable(helper, ModBlocks.ENDERITE_HOPPER, true);
        assertPickaxeMineable(helper, Blocks.HOPPER, true);
        // Dirt, not end stone: end stone is a plain value in vanilla's own pickaxe tag (see the
        // javadoc), so it can never be the "the lookup says no" side of this pair.
        assertPickaxeMineable(helper, Blocks.DIRT, false);

        // --- the tool requirement vanilla's hopper has: a bare hand drops nothing ---
        for (Block hopper : List.of(ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER)) {
            helper.assertTrue(hopper.defaultBlockState().requiresCorrectToolForDrops()
                            && Blocks.HOPPER.defaultBlockState().requiresCorrectToolForDrops(),
                    BuiltInRegistries.BLOCK.getKey(hopper) + " drops to a bare hand "
                            + "(requiresCorrectToolForDrops is false), unlike vanilla's hopper it is built from");
        }

        // --- and in no tool tier tag: a stone pickaxe has to be enough ---
        for (TagKey<Block> tier : List.of(BlockTags.NEEDS_STONE_TOOL, BlockTags.NEEDS_IRON_TOOL,
                BlockTags.NEEDS_DIAMOND_TOOL)) {
            helper.assertFalse(ModBlocks.REINFORCED_HOPPER.defaultBlockState().is(tier),
                    "the reinforced hopper was put into " + tier.location());
            helper.assertFalse(ModBlocks.NETHERITE_HOPPER.defaultBlockState().is(tier),
                    "the netherite hopper was put into " + tier.location());
            helper.assertFalse(ModBlocks.ENDERITE_HOPPER.defaultBlockState().is(tier),
                    "the enderite hopper was put into " + tier.location());
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
        DamageResistant enderiteHopper = new ItemStack(ModItems.ENDERITE_HOPPER).get(DataComponents.DAMAGE_RESISTANT);
        helper.assertTrue(enderiteHopper != null && enderiteHopper.isResistantTo(level.damageSources().lava()),
                "a dropped enderite hopper should survive lava like the netherite one");
        helper.assertTrue(
                new ItemStack(ModItems.REINFORCED_HOPPER).get(DataComponents.DAMAGE_RESISTANT) == null,
                "the reinforced hopper item is fire resistant too, which makes the assertions above "
                        + "meaningless - it is the netherite tier that is supposed to survive lava");

        helper.succeed();
    }

    /**
     * The one hopper crafting recipe, driven through the live {@code RecipeManager} with a real
     * crafting grid: pattern, ingredients, result and - the part
     * {@code DataIntegrityTests#modRecipesOnlyReferenceRegisteredItems} never reads - the count.
     * Five reinforced hoppers per craft are what make the chain worth running at all; a recipe
     * that silently yielded one would pass every other test in the tree.
     *
     * <p>The recipe is also offered upside down. Vanilla matches a shaped recipe against its own
     * pattern and the mirror of it, never against a vertical flip, so a match on the flipped grid
     * would mean the shape is not being checked at all. The pattern's rows are palindromes, which
     * is why the flip - and not the mirror - is the control that says something here.
     *
     * <p>The netherite hopper has had no crafting recipe since 2026-09: it is made in the world
     * with a sledgehammer and a netherite nugget ({@code SledgehammerUpgrades}). The column the old
     * {@code netherite_hopper_from_crafting} recipe used - two reinforced hoppers around a nugget -
     * therefore has to craft nothing, and the recipe id has to be gone; the reinforced grid above
     * is the positive control that the lookup still answers "yes".
     *
     * <p>What breaks this test: any edit to the reinforced recipe JSON - a different pattern, a
     * swapped ingredient, another count - or it failing to load, and a crafting recipe for the
     * netherite hopper coming back.
     */
    public static void hopperRecipesCraftFromTheirDocumentedPatterns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // Shapeless: one vanilla hopper, one name tag and one cracked diamond.
        CraftingInput reinforced = CraftingInput.of(3, 3, List.of(
                stack(Items.HOPPER), stack(Items.NAME_TAG), stack(ModItems.CRACKED_DIAMOND),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        assertCrafts(helper, level, reinforced, "simplebuilding:reinforced_hopper_from_crafting",
                ModItems.REINFORCED_HOPPER, 1);

        // "H" / "N" / "H", the old netherite hopper recipe: two reinforced hoppers around one
        // netherite nugget. The netherite hopper is hammered in the world now, so nothing may craft.
        CraftingInput netherite = CraftingInput.of(1, 3, List.of(
                stack(ModItems.REINFORCED_HOPPER),
                stack(ModItems.NETHERITE_NUGGET),
                stack(ModItems.REINFORCED_HOPPER)));
        assertCraftsNothing(helper, level, netherite,
                "the old netherite hopper column (two reinforced hoppers around a nugget)");
        helper.assertTrue(level.getServer().getRecipeManager().getRecipes().stream()
                        .noneMatch(holder -> holder.id().identifier().getPath().equals("netherite_hopper_from_crafting")),
                "simplebuilding:netherite_hopper_from_crafting is loaded again, but the netherite "
                        + "hopper is meant to be made with the sledgehammer only");

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
    // AUDIT 2026-09-26 #36 / #48, NACH-AUDIT N6
    // =====================================================================================

    /**
     * A filtered hopper slot that holds something is an ordinary slot again (audit #48): a plain
     * click takes its items out - until 2026-09-27 every click on a filtered slot only rewrote the
     * filter item, so the slot could only be emptied with a shift click. On an empty slot the click
     * still sets (with an item on the cursor) or clears (with an empty cursor) the filter item.
     *
     * <p>And the hopper slots ask the vanilla {@code Slot#mayPlace} first (audit N6), where
     * {@code SpawnElytraSlotMixin} keeps the spawn elytra out of every container slot; the mod's
     * filter slot used to answer on its own and took it. Diamonds are the control.
     */
    public static void filteredSlotsHandOutWhatTheyHoldAndRefuseTheSpawnElytra(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.NETHERITE_HOPPER);
        MenuProvider provider = helper.getBlockState(HOPPER_POS).getMenuProvider(helper.getLevel(), helper.absolutePos(HOPPER_POS));
        helper.assertTrue(provider != null, "the hopper block offers no menu at all");
        ModHopperScreenHandler menu = (ModHopperScreenHandler) provider.createMenu(1, player.getInventory(), player);
        player.containerMenu = menu;

        // --- N6: the spawn elytra stays out, the filter is off ---
        helper.assertTrue(!menu.getSlot(1).mayPlace(new ItemStack(com.simplebuilding.tweaks.item.TweaksItems.SPAWN_ELYTRA)),
                "a hopper slot of the mod hopper accepts the spawn elytra - its filter slot skips the vanilla "
                        + "mayPlace where the spawn elytra rule lives");
        helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)),
                "the hopper slot refuses diamonds as well, so the refusal above says nothing");

        // --- #48: a filtered slot with items in it gives them out on a plain click ---
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the toggle did not reach Exact Match, it is " + hopper.getFilterMode());
        hopper.setItem(0, new ItemStack(Items.DIAMOND, 5));
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 5,
                "a plain click on a filtered slot holding 5 diamonds put " + menu.getCarried()
                        + " on the cursor - the items can only be shift-clicked out");
        helper.assertTrue(hopper.getItem(0).isEmpty(), "the filtered slot still holds " + hopper.getItem(0));

        // --- on the now empty slot a click with emeralds makes them the new filter ---
        menu.setCarried(new ItemStack(Items.EMERALD, 3));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(hopper.getItem(0).is(Items.EMERALD) && hopper.getItem(0).getCount() == 3,
                "a click with emeralds on the empty filtered slot did not put them in: " + hopper.getItem(0));
        helper.assertTrue(menu.getCarried().isEmpty(), "emeralds left on the cursor: " + menu.getCarried());

        player.closeContainer();
        helper.succeed();
    }

    /** Tick budget for {@link #modHoppersFallBackToTheLoaderTransferApiWithoutContainer}. */
    public static final int ITEM_AUTOMATION_MAX_TICKS = 60;

    /**
     * No vanilla {@code Container} in front of a mod hopper: it hands its items to the loader's
     * transfer API instead ({@code PlatformServices#itemAutomation} - NeoForge capabilities, Fabric
     * {@code ItemStorage.SIDED}, Forge {@code ITEM_HANDLER}), one per transfer, through the face that
     * looks at the hopper (audit #36; before 2026-09-27 only vanilla containers were fed). A plain
     * stone block below stands in for another mod's machine: the test wraps the installed
     * automation and answers for exactly that position, everything else goes on to the loader.
     */
    public static void modHoppersFallBackToTheLoaderTransferApiWithoutContainer(GameTestHelper helper) {
        ModHopperBlockEntity hopper = placeHopper(helper, ModBlocks.REINFORCED_HOPPER);
        helper.setBlock(HOPPER_POS.below(), Blocks.STONE);
        BlockPos machine = helper.absolutePos(HOPPER_POS.below());
        ServerLevel level = helper.getLevel();
        List<ItemStack> received = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<Direction> faces = new java.util.concurrent.CopyOnWriteArrayList<>();
        com.simplebuilding.platform.ItemAutomation installed = PlatformServices.itemAutomation();
        helper.assertTrue(PlatformServices.hasItemAutomation(), "this loader installed no item automation at all");
        PlatformServices.setItemAutomation(new com.simplebuilding.platform.ItemAutomation() {
            @Override
            public int insert(ServerLevel where, BlockPos pos, Direction side, ItemStack stack) {
                if (where == level && pos.equals(machine)) {
                    received.add(stack.copy());
                    faces.add(side);
                    return stack.getCount();
                }
                return installed.insert(where, pos, side, stack);
            }

            @Override
            public int extract(ServerLevel where, BlockPos pos, Direction side, Item item, int amount) {
                return installed.extract(where, pos, side, item, amount);
            }
        });
        helper.runBeforeTestEnd(() -> PlatformServices.setItemAutomation(installed));

        hopper.setItem(0, new ItemStack(Items.DIAMOND, 3));
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    int total = received.stream().mapToInt(ItemStack::getCount).sum();
                    helper.assertValueEqual(total, 3,
                            "diamonds the mod hopper handed to the loader's transfer API with no container below");
                    helper.assertTrue(received.stream().allMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 1),
                            "the hopper did not hand over one diamond per transfer: " + received);
                    helper.assertTrue(faces.stream().allMatch(face -> face == Direction.UP),
                            "the hopper asked the machine below through " + faces + " instead of its top face");
                    helper.assertTrue(hopper.isEmpty(), "the hopper kept items the transfer API took: " + hopper.getItem(0));
                })
                .thenSucceed();
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
