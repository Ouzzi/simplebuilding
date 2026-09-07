package com.simplebuilding.clienttest;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.DoubleJumpController;
import com.simplebuilding.client.gui.BuildingWandScreen;
import com.simplebuilding.client.gui.OctantScreen;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.util.EnchantmentHelper;
import com.simplebuilding.util.ISpaceKeyTracker;
import com.simplebuilding.util.TrimBenefitUser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Covers the client side bootstrap of the mod: the key mappings registered in
 * {@code SimplebuildingClient.onInitializeClient}, the two {@code END_CLIENT_TICK} loops hanging
 * off them, the {@code ClientPlayConnectionEvents.JOIN} listener that reports a config value to
 * the server, the {@code MouseHandler} mixin that turns "modifier + wheel" into an octant scroll
 * packet, and the {@code Minecraft.pickBlockOrEntity} mixin behind the pick block key.
 *
 * <p>None of this is reachable from a headless server test: key mappings, screens,
 * {@code MouseHandler.onScroll} and {@code pickBlockOrEntity} only exist on a client, and the
 * server side receivers they talk to are already covered by {@code NetworkHandlerTests}. What is
 * missing there - and what this class adds - is the proof that the client ever sends anything.
 *
 * <p><b>How the claims are made falsifiable.</b> This class takes no pixel measurements; there is
 * nothing to see on screen when a packet leaves. Instead every claim is anchored on a value that
 * is stored somewhere observable and that has a <em>different</em> value when the wiring is
 * missing:
 * <ul>
 *   <li>screens: {@code client.gui.screen()} - and every "no screen opened" step is bracketed by a
 *       step where the same key press does open a screen, so a key press that never arrived shows
 *       up as a red positive step instead of a green negative one;</li>
 *   <li>octant scroll: the {@code Pos1} / {@code Pos2} / {@code Shape} nbt of the server side item
 *       stack, plus the selected hotbar slot, which vanilla only moves when the mixin did
 *       <em>not</em> cancel the scroll event;</li>
 *   <li>the three payloads: the server side state they write
 *       ({@code TrimBenefitUser}, {@code ISpaceKeyTracker}, boot damage from
 *       {@code ModMessageHandlers.handleDoubleJump}), each of which starts out at a value the
 *       payload has to change.</li>
 * </ul>
 * Every renderer style precondition is asserted before the measurement for the same reason as in
 * the screenshot tests: an unchanged value must not be explainable by "the test never set the
 * scene up correctly".
 *
 * <p><b>Vanilla carries these parts, not the mod</b> (stated here so the assertions below are not
 * read as mod claims): that a mouse wheel event moves the selected hotbar slot, that the pick
 * block key calls {@code pickBlockOrEntity} at all, that a survival pick of a block the player
 * does not own is a no-op ({@code ServerGamePacketListenerImpl.tryPickItem} only searches the
 * inventory, never a bundle), and that holding the jump key makes the player leave the ground.
 * The mod claims are the branches layered on top of those.
 *
 * <p><b>Known defect</b> (reported, not written into an assertion):
 * <ul>
 *   <li>{@code key.simplebuilding.toggle_octant_figure} and {@code key.simplebuilding.toggle_highlight}
 *       flip the very same field, {@code ClientState.showHighlights}
 *       (SimplebuildingClient.java:90 and :97), and that field gates only the octant area fill
 *       (BlockHighlightRenderer.java:183). So the key named "toggle highlight" does not touch the
 *       sledgehammer highlight it is named after, and the octant figure key is a second, unbound
 *       copy of it. Only the bound key's documented effect is asserted below; the duplication is
 *       deliberately left unpinned.</li>
 *   <li>{@code ClientToggleKeys} exists to keep both loaders on one implementation and NeoForge
 *       calls it (SimplebuildingNeoForgeClient.java:168, with a comment claiming Fabric shares
 *       it), but Fabric never does - it keeps an inline copy in
 *       {@code SimplebuildingClient.onInitializeClient}. The copies already differ: the shared
 *       class returns early when {@code client.player == null}, the Fabric copy flips
 *       {@code showHighlights} anyway and only skips the actionbar message.</li>
 * </ul>
 *
 * <p><b>Not covered</b>, with the reason:
 * <ul>
 *   <li><b>The NeoForge half of every claim here.</b> This harness boots a Fabric client, so
 *       "both loaders call the same {@code DoubleJumpController.tick} and register the same HUD
 *       layer" is only ever half proven. The NeoForge side stays a code review question.</li>
 *   <li><b>The three HUD layers</b> attached in {@code onInitializeClient}.
 *       {@code HudElementRegistry} has no lookup, and each overlay draws nothing while its
 *       feature is idle, so "the layer is attached" cannot be told apart from "the layer drew
 *       nothing" from the outside. The overlays' own output belongs to the HUD test class.</li>
 *   <li><b>Pressing {@code key.simplebuilding.toggle_octant_figure}.</b> It ships bound to
 *       {@code InputConstants.UNKNOWN} and the client gametest input API refuses to press an
 *       unbound mapping. Only its registration and its unbound state are asserted, as the
 *       tripwire for this note.</li>
 *   <li><b>"{@code SpaceKeyPayload} is sent only when the key state changes."</b> The server keeps
 *       just the last value, so a redundant send is invisible from the outside; proving the
 *       bandwidth guard would need a counter inside the mod.</li>
 *   <li><b>A JOIN that reports the config default.</b> The trim benefit test has to send a value
 *       that differs from the server side default; a client that sends nothing and a client that
 *       sends the default are indistinguishable on the server.</li>
 *   <li><b>The actionbar text of the toggle keys</b> ("Highlights: ON"). It is an overlay message
 *       with a two second lifetime; asserting it belongs to a HUD pixel test, not here.</li>
 * </ul>
 */
public final class ClientBootstrapClientGameTest implements FabricClientGameTest {

    /** First non hotbar inventory slot - where the reinforced bundle goes. */
    private static final int BUNDLE_SLOT = 9;

    /** Hotbar slot the tests keep the item under test in. */
    private static final int HOTBAR_SLOT = 0;

    /** Stone put into the bundle for the Master Builder pick. */
    private static final int BUNDLED_STONE_COUNT = 4;

    /** Octant corners. Only their nbt is read here, but free air keeps them out of the wall. */
    private static final BlockPos OCTANT_POS_1 = new BlockPos(9, 1, RendererTestScene.FRONT_Z);
    private static final BlockPos OCTANT_POS_2 = new BlockPos(11, 1, RendererTestScene.FRONT_Z);

    /**
     * Where the octant lives during the scroll test, and where its nbt is read from.
     *
     * <p>Reading it out of the main hand instead would be wrong for the two steps that expect the
     * scroll <em>not</em> to be cancelled: vanilla then moves the selection off the octant, the main
     * hand goes empty and "the octant has no Pos1" would be reported as the mod's fault. The slot is
     * fixed, the octant never leaves it, so this reads the same stack in every step.
     */
    private static final int OCTANT_SLOT = HOTBAR_SLOT;

    @Override
    public void runTest(ClientGameTestContext context) {
        // The JOIN listener has to be measured on a value that differs from the server side
        // default, so the config is flipped before the world - and with it the JOIN event - exists.
        boolean originalTrimBenefits = readTrimBenefitConfig(context);
        setTrimBenefitConfig(context, false);

        try {
            try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
                try {
                    testTrimBenefitConfigReachesTheServerOnJoin(context, singleplayer);

                    // Survival for the whole class: the pick block mixin bails out in creative,
                    // and the air jump has to damage the boots, which creative would skip.
                    RendererTestScene.build(context, singleplayer, "minecraft:stone", "survival");
                    // No pixel measurement happens here, so the HUD stays on - the screenshots of
                    // the screens read better with a hotbar under them.
                    RendererTestScene.showHudAgain(context);

                    testToggleKeysReachTheClientTickLoop(context);
                    testSettingsKeyOnlyReactsToTheRightItem(context, singleplayer);
                    testSettingsKeyNeedsConstructorsTouchOnAWand(context, singleplayer);
                    testOctantScrollNeedsAModifierAndNoScreen(context, singleplayer);
                    testMasterBuilderPickPullsFromTheBundle(context, singleplayer);
                    testSpaceKeyStateReachesTheServer(context, singleplayer);
                    testAirJumpRunsOnEveryClientTick(context, singleplayer);
                } finally {
                    releaseAllInput(context);
                    RendererTestScene.showHudAgain(context);
                }
            }
        } finally {
            setTrimBenefitConfig(context, originalTrimBenefits);
        }
    }

    /**
     * The client reports its {@code enableArmorTrimBenefits} setting to the server when it joins
     * a world (SimplebuildingClient.java:171).
     *
     * <p>The measurement works because the two sides start out disagreeing: the server side flag
     * lives in {@code PlayerEntityMixin} and defaults to {@code true}, while the config was set to
     * {@code false} before the world was created. So the server can only end up at {@code false}
     * if a {@code TrimBenefitPayload} really was sent and handled.
     *
     * <p><b>What breaks this test:</b> dropping or misplacing the JOIN listener, sending the
     * literal instead of the config value, or the payload losing its receiver - the server player
     * then keeps the default {@code true} and this goes red.
     */
    private void testTrimBenefitConfigReachesTheServerOnJoin(ClientGameTestContext context,
                                                             TestSingleplayerContext singleplayer) {
        if (readTrimBenefitConfig(context)) {
            throw new AssertionError("Setup failed: enableArmorTrimBenefits was supposed to be off before the "
                    + "world was created, otherwise the value the client sends is the same as the server side "
                    + "default and this test could not fail.");
        }

        singleplayer.getConnection().waitForServerboundPackets();

        awaitServerValue(context, singleplayer,
                player -> ((TrimBenefitUser) player).simplebuilding$areTrimBenefitsEnabled(),
                enabled -> !enabled, 100,
                () -> "The client never reported enableArmorTrimBenefits=false to the server on join. The "
                        + "server side flag still holds its default, so no TrimBenefitPayload arrived.");
    }

    /**
     * The highlight toggle key is registered, bound and drained by the client tick loop
     * (SimplebuildingClient.java:69 and :89).
     *
     * <p>Pressing it has to flip {@code ClientState.showHighlights}, and pressing it again has to
     * flip it back - a single press could still be explained by some other code touching the flag,
     * two presses in opposite directions cannot.
     *
     * <p>The octant figure key is only checked for being registered and unbound. That is the
     * tripwire for the "Not covered" note in the class javadoc: the harness cannot press an
     * unbound mapping, so if someone ever gives it a default key, this assertion goes red and the
     * note has to be revisited. Its <em>effect</em> is deliberately not asserted, see "Known
     * defect".
     *
     * <p><b>What breaks this test:</b> losing the {@code KeyMappingHelper} registration, dropping
     * the {@code consumeClick} loop out of {@code END_CLIENT_TICK}, or moving the toggle to a
     * different flag than {@code ClientState.showHighlights}.
     */
    private void testToggleKeysReachTheClientTickLoop(ClientGameTestContext context) {
        String problem = context.computeOnClient(client -> {
            if (ClientState.highlightToggleKey == null) {
                return "the highlight toggle key was never registered";
            }

            if (ClientState.highlightToggleKey.isUnbound()) {
                return "the highlight toggle key is unbound, so it cannot be pressed";
            }

            if (ClientState.octantFigureToggleKey == null) {
                return "the octant figure toggle key was never registered";
            }

            if (!ClientState.octantFigureToggleKey.isUnbound()) {
                return "the octant figure toggle key is bound now. It used to ship unbound, which is why the "
                        + "class javadoc lists it as not coverable - that note needs revisiting";
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Toggle key preconditions not met: " + problem);
        }

        boolean before = context.computeOnClient(client -> ClientState.showHighlights);

        context.getInput().pressKey(ClientState.highlightToggleKey);
        context.waitTicks(5);
        boolean afterFirst = context.computeOnClient(client -> ClientState.showHighlights);

        if (afterFirst == before) {
            throw new AssertionError("The highlight toggle key did not flip ClientState.showHighlights (still "
                    + before + "). Either the key mapping is not registered with the client or the "
                    + "END_CLIENT_TICK loop that drains it is gone.");
        }

        context.getInput().pressKey(ClientState.highlightToggleKey);
        context.waitTicks(5);
        boolean afterSecond = context.computeOnClient(client -> ClientState.showHighlights);

        if (afterSecond != before) {
            throw new AssertionError("The second press of the highlight toggle key did not restore "
                    + "ClientState.showHighlights (" + before + " -> " + afterFirst + " -> " + afterSecond
                    + "). The flag is not driven by the key alone.");
        }
    }

    /**
     * The settings key opens the octant manager for <em>any</em> octant - no enchantment involved -
     * and opens nothing at all for an item that is neither an octant nor a building wand
     * (SimplebuildingClient.java:103).
     *
     * <p>Order matters: the negative step runs first and the positive step right after it, in the
     * same scene and with the same key. That is what makes the negative step worth anything - a key
     * press that never reached the client would take the positive step down with it.
     *
     * <p><b>What breaks this test:</b> dropping the {@code instanceof OctantItem} guard (the stone
     * step then opens a screen), adding an enchantment requirement to the octant branch (the octant
     * step then opens nothing), or breaking the screen itself, which crashes the client.
     */
    private void testSettingsKeyOnlyReactsToTheRightItem(ClientGameTestContext context,
                                                         TestSingleplayerContext singleplayer) {
        giveMainHand(context, singleplayer, "minecraft:stone");

        context.getInput().pressKey(ClientState.settingsKey);
        context.waitTicks(10);
        assertNoScreen(context, "a plain stone block in the main hand");
        context.takeScreenshot("bootstrap-a-settings-key-plain-item");

        giveMainHand(context, singleplayer, "simplebuilding:octant");

        String problem = context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            ItemStack stack = client.player.getMainHandItem();

            if (!(stack.getItem() instanceof OctantItem)) {
                return "the main hand does not hold an OctantItem but " + stack;
            }

            // "for every octant, without enchantment" is the claim - so prove the octant really
            // carries none, otherwise the step below would also pass with a hidden requirement.
            if (!stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()) {
                return "the octant carries enchantments (" + stack.getEnchantments() + "), so this step "
                        + "would not prove that the manager opens without one";
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Octant settings key preconditions not met: " + problem);
        }

        context.getInput().pressKey(ClientState.settingsKey);
        context.waitForScreen(OctantScreen.class);
        context.waitTicks(20);
        assertScreenIs(context, OctantScreen.class, "octant manager");
        context.takeScreenshot("bootstrap-b-octant-manager");
        closeScreen(context);
    }

    /**
     * The settings key opens the building wand screen only for a wand that carries Constructor's
     * Touch (SimplebuildingClient.java:108).
     *
     * <p>{@code ModScreensClientGameTest} constructs {@link BuildingWandScreen} directly and says
     * so; this is the missing half - the real key path including the enchantment gate. Both steps
     * assert the enchantment level the mod reads (through the same
     * {@code EnchantmentHelper.hasEnchantment} with the client level) before pressing the key, so
     * "no screen" can never mean "the enchantment never arrived on the client".
     *
     * <p><b>What breaks this test:</b> dropping the enchantment check (the plain wand then opens
     * the screen), the enchantment component not surviving the trip to the client, or the wand
     * branch losing its {@code instanceof BuildingWandItem} guard.
     */
    private void testSettingsKeyNeedsConstructorsTouchOnAWand(ClientGameTestContext context,
                                                              TestSingleplayerContext singleplayer) {
        giveMainHand(context, singleplayer, "simplebuilding:netherite_building_wand");
        assertWandInHandWithTouchLevel(context, 0);

        context.getInput().pressKey(ClientState.settingsKey);
        context.waitTicks(10);
        assertNoScreen(context, "a building wand without Constructor's Touch");
        context.takeScreenshot("bootstrap-c-settings-key-plain-wand");

        selectHotbarSlot(context, singleplayer, HOTBAR_SLOT);
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runOnServer(server -> {
            ServerPlayer player = serverPlayer(server);
            ItemStack wand = new ItemStack(ModItems.NETHERITE_BUILDING_WAND);
            wand.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(ModEnchantments.CONSTRUCTORS_TOUCH), 1);
            player.getInventory().setItem(HOTBAR_SLOT, wand);
            player.inventoryMenu.broadcastChanges();
        });
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        assertWandInHandWithTouchLevel(context, 1);

        context.getInput().pressKey(ClientState.settingsKey);
        context.waitForScreen(BuildingWandScreen.class);
        context.waitTicks(20);
        assertScreenIs(context, BuildingWandScreen.class, "building wand settings");
        context.takeScreenshot("bootstrap-d-building-wand-settings");
        closeScreen(context);
    }

    /**
     * {@code MouseMixin} turns the mouse wheel into an {@code OctantScrollPayload} only while an
     * octant is in the main hand, no screen is open and Shift, Control or Alt is held - and it
     * cancels the vanilla event exactly in that case (MouseMixin.java:19).
     *
     * <p>Two independent observations are used, because either one alone would be ambiguous:
     * <ul>
     *   <li>the server side {@code Pos1} / {@code Pos2} / {@code Shape} nbt says the payload
     *       arrived, and by how much - the amount is {@code signum(vertical)}, so a wheel event of
     *       four still has to move exactly one block. It is read out of {@link #OCTANT_SLOT}, not
     *       out of the main hand: the two steps that expect no cancellation let vanilla move the
     *       selection off the octant;</li>
     *   <li>the selected hotbar slot says whether the event was cancelled: vanilla moves it on
     *       every uncancelled scroll, so an unchanged slot proves the mixin swallowed the event and
     *       a changed slot proves it did not.</li>
     * </ul>
     * Shift is read through {@code client.options.keyShift.isDown()} while Control and Alt go
     * through {@code InputConstants.isKeyDown}; both paths are exercised so a change to either one
     * shows up. Both reach the mod for different reasons: the harness feeds its synthetic key
     * events through {@code KeyboardHandler}, which is what sets the key mapping's down state, and
     * it replaces {@code InputConstants.isKeyDown} with a lookup into its own set of held keys.
     * Without that second part the Control and Alt steps below could never be driven from a test.
     *
     * <p><b>What breaks this test:</b> dropping the modifier check, the screen check or the
     * {@code Locked} check (the corresponding "unchanged" assertion goes red), removing
     * {@code ci.cancel()} (the hotbar slot moves during the modifier steps), losing the
     * {@code signum} normalisation (the four wheel step moves four blocks), or the payload not
     * reaching its server side handler at all.
     */
    private void testOctantScrollNeedsAModifierAndNoScreen(ClientGameTestContext context,
                                                           TestSingleplayerContext singleplayer) {
        giveOctantWithCorners(context, singleplayer, false);

        // The server resolves the scroll direction from the player's facing, so pin it down first:
        // yaw 0 is SOUTH (+Z), and a pitch beyond +-60 would switch the handler to UP / DOWN.
        String aim = singleplayer.getServer().computeOnServer(server -> {
            ServerPlayer player = serverPlayer(server);

            if (player.getDirection() != Direction.SOUTH) {
                return "the server player faces " + player.getDirection() + " instead of SOUTH";
            }

            if (Math.abs(player.getXRot()) > 60.0f) {
                return "the server player's pitch is " + player.getXRot()
                        + ", which switches the scroll handler to the UP / DOWN branch";
            }

            return null;
        });

        if (aim != null) {
            throw new AssertionError("Octant scroll preconditions not met: " + aim);
        }

        assertOctantPos(singleplayer, "Pos1", OCTANT_POS_1, "before the first scroll");
        assertOctantPos(singleplayer, "Pos2", OCTANT_POS_2, "before the first scroll");

        int slotBefore = selectedSlot(context);

        // --- Control moves Pos1 along the facing direction, one block per wheel notch ---
        context.getInput().holdControl();

        scrollAndSettle(context, 1.0);
        assertOctantPos(singleplayer, "Pos1", OCTANT_POS_1.offset(0, 0, 1), "after one wheel notch with Control");

        // signum: a wheel event of four is still exactly one block.
        scrollAndSettle(context, 4.0);
        assertOctantPos(singleplayer, "Pos1", OCTANT_POS_1.offset(0, 0, 2),
                "after a wheel event of four with Control - the mixin normalises the amount with signum");

        scrollAndSettle(context, -1.0);
        assertOctantPos(singleplayer, "Pos1", OCTANT_POS_1.offset(0, 0, 1), "after scrolling back down with Control");

        context.getInput().releaseControl();
        context.waitTicks(2);

        assertSelectedSlot(context, slotBefore,
                "the mixin did not cancel the vanilla scroll while Control was held");
        context.takeScreenshot("bootstrap-e-octant-scroll");

        // --- Shift moves Pos2. Read from options.keyShift, not from InputConstants ---
        context.getInput().holdShift();
        scrollAndSettle(context, 1.0);
        context.getInput().releaseShift();
        context.waitTicks(2);

        assertOctantPos(singleplayer, "Pos2", OCTANT_POS_2.offset(0, 0, 1), "after one wheel notch with Shift");
        assertOctantPos(singleplayer, "Pos1", OCTANT_POS_1.offset(0, 0, 1), "after the Shift scroll - Pos1 is not "
                + "supposed to move with Shift");
        assertSelectedSlot(context, slotBefore,
                "the mixin did not cancel the vanilla scroll while Shift was held");

        // --- Alt cycles the shape. No shape has been stored yet, so CUBOID is the starting point ---
        if (!readOctantShape(singleplayer).isEmpty()) {
            throw new AssertionError("Setup failed: the octant already carries a Shape ("
                    + readOctantShape(singleplayer) + "), so the step below cannot show that Alt advanced it "
                    + "from the CUBOID default.");
        }

        context.getInput().holdAlt();
        scrollAndSettle(context, 1.0);
        context.getInput().releaseAlt();
        context.waitTicks(2);

        String shape = readOctantShape(singleplayer);
        String expectedShape = OctantItem.SelectionShape.CYLINDER.name();

        if (!expectedShape.equals(shape)) {
            throw new AssertionError("One wheel notch with Alt was supposed to advance the octant shape from "
                    + OctantItem.SelectionShape.CUBOID.name() + " to " + expectedShape + ", but the server side "
                    + "stack holds " + (shape.isEmpty() ? "no shape at all" : shape) + ".");
        }

        assertSelectedSlot(context, slotBefore,
                "the mixin did not cancel the vanilla scroll while Alt was held");

        // --- An open screen switches the mixin off, even with a modifier held ---
        BlockPos posBeforeScreen = OCTANT_POS_1.offset(0, 0, 1);
        context.setScreen(() -> new ChatScreen("", false));
        context.waitForScreen(ChatScreen.class);
        context.waitTicks(5);

        context.getInput().holdControl();
        scrollAndSettle(context, 1.0);
        context.getInput().releaseControl();
        context.waitTicks(2);

        closeScreen(context);
        assertOctantPos(singleplayer, "Pos1", posBeforeScreen,
                "after scrolling with Control while a screen was open - the mixin is supposed to stay out of it");

        // --- Without a modifier the event stays vanilla, which moves the hotbar selection ---
        // Only the slot is asserted here: with all three modifier flags false the server side
        // handler would not move anything either, so an unchanged Pos1 could not fail and would
        // be a decorative assertion.
        scrollAndSettle(context, 1.0);
        int slotAfterPlainScroll = selectedSlot(context);

        if (slotAfterPlainScroll == slotBefore) {
            throw new AssertionError("A plain scroll without a modifier left the selected hotbar slot at "
                    + slotBefore + ". The mixin is only allowed to cancel the event while a modifier is held, "
                    + "so vanilla's hotbar scrolling has to still work here.");
        }

        selectHotbarSlot(context, singleplayer,slotBefore);

        // --- A locked octant is left to vanilla as well ---
        giveOctantWithCorners(context, singleplayer, true);
        int slotBeforeLocked = selectedSlot(context);

        context.getInput().holdControl();
        scrollAndSettle(context, 1.0);
        context.getInput().releaseControl();
        context.waitTicks(2);

        assertOctantPos(singleplayer, "Pos1", OCTANT_POS_1, "after scrolling with Control on a locked octant");

        if (selectedSlot(context) == slotBeforeLocked) {
            throw new AssertionError("Scrolling with Control on a locked octant left the selected hotbar slot at "
                    + slotBeforeLocked + ". A locked octant is supposed to fall through to vanilla instead of "
                    + "cancelling the event.");
        }

        selectHotbarSlot(context, singleplayer,slotBeforeLocked);
    }

    /**
     * The pick block key pulls a targeted block out of a Master Builder bundle when the player does
     * not own it yet and is not in creative (MinecraftClientMixin.java:30).
     *
     * <p>The control step runs the very same press with the very same bundle, only without the
     * enchantment. Vanilla cannot satisfy that press - {@code tryPickItem} searches the inventory
     * and nothing else - so the hand has to stay empty there, and every difference between the two
     * steps is the mixin's.
     *
     * <p><b>What breaks this test:</b> dropping the Master Builder check (the control step fills
     * the hand), dropping the {@code findSlotMatchingItem} or the creative guard, the payload not
     * being sent, or {@code handleMasterBuilderPick} no longer taking the stack out of the bundle.
     */
    private void testMasterBuilderPickPullsFromTheBundle(ClientGameTestContext context,
                                                         TestSingleplayerContext singleplayer) {
        giveBundleWithStone(context, singleplayer, false);

        String problem = context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            if (client.player.isCreative()) {
                return "the player is in creative, where the mixin returns immediately";
            }

            if (!client.player.getMainHandItem().isEmpty()) {
                return "the main hand is not empty but holds " + client.player.getMainHandItem()
                        + ", so the handler would take its free slot branch";
            }

            if (client.player.getInventory().findSlotMatchingItem(new ItemStack(Items.STONE)) != -1) {
                return "the inventory already holds stone, so the mixin returns before it ever looks at a bundle";
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Master Builder pick preconditions not met: " + problem);
        }

        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        if (countStoneInBundle(singleplayer) != BUNDLED_STONE_COUNT) {
            throw new AssertionError("Setup failed: the bundle does not hold the "
                    + BUNDLED_STONE_COUNT + " stone the pick is supposed to find, it holds "
                    + countStoneInBundle(singleplayer) + ".");
        }

        context.getInput().pressKey(options -> options.keyPickItem);
        context.waitTicks(20);

        boolean handFilled = singleplayer.getServer().computeOnServer(server ->
                !serverPlayer(server).getMainHandItem().isEmpty());

        if (handFilled) {
            throw new AssertionError("Control step failed: picking the targeted stone block filled the hand with "
                    + describeServerMainHand(singleplayer) + " although the bundle carries no Master Builder. "
                    + "Neither vanilla nor the mod is allowed to hand out a block the player does not own, so "
                    + "the signal step below would not be attributable to the enchantment.");
        }

        giveBundleWithStone(context, singleplayer, true);
        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        context.getInput().pressKey(options -> options.keyPickItem);

        awaitServerValue(context, singleplayer,
                player -> player.getMainHandItem().is(Items.STONE), Boolean::booleanValue, 100,
                () -> "The pick block key never pulled the targeted stone out of the Master Builder bundle. "
                        + "The hand holds " + describeServerMainHand(singleplayer) + " and the bundle still "
                        + "holds " + countStoneInBundle(singleplayer) + " stone.");

        context.takeScreenshot("bootstrap-f-master-builder-pick");

        int leftInBundle = countStoneInBundle(singleplayer);

        if (leftInBundle != 0) {
            throw new AssertionError("The stone in the hand did not come out of the bundle: the bundle still "
                    + "holds " + leftInBundle + " stone.");
        }
    }

    /**
     * The client sends a {@code SpaceKeyPayload} whenever the jump key changes state
     * (SimplebuildingClient.java:191).
     *
     * <p>Observed on the server side flag from {@code PlayerEntityMixin}, which starts at
     * {@code false}: holding jump has to turn it on, releasing it has to turn it off again. Both
     * directions are asserted, so a flag that is stuck at one value cannot pass.
     *
     * <p><b>What breaks this test:</b> dropping the tick listener, sending only one edge, or the
     * payload losing its server side receiver.
     */
    private void testSpaceKeyStateReachesTheServer(ClientGameTestContext context,
                                                   TestSingleplayerContext singleplayer) {
        if (readSpacePressedOnServer(singleplayer)) {
            throw new AssertionError("Setup failed: the server already thinks the jump key is held before the "
                    + "test pressed it, so 'pressed' below would not prove anything.");
        }

        context.getInput().holdKey(options -> options.keyJump);

        awaitServerValue(context, singleplayer,
                player -> ((ISpaceKeyTracker) player).simplebuilding$isSpacePressed(), Boolean::booleanValue, 60,
                () -> "Holding the jump key never reached the server: the SpaceKeyPayload for the rising edge "
                        + "was not sent or not handled.");

        context.getInput().releaseKey(options -> options.keyJump);

        awaitServerValue(context, singleplayer,
                player -> ((ISpaceKeyTracker) player).simplebuilding$isSpacePressed(), pressed -> !pressed, 60,
                () -> "Releasing the jump key never reached the server: the SpaceKeyPayload for the falling "
                        + "edge was not sent or not handled.");

        waitForGround(context, 100);
    }

    /**
     * {@code DoubleJumpController.tick} runs on every client tick on Fabric
     * (SimplebuildingClient.java:243), and the air jump it performs reaches the server.
     *
     * <p>The player is teleported into the air instead of jumping off the floor: the controller
     * needs a tick in which the player is airborne <em>and</em> the jump key is up before it will
     * accept the next press, and a teleport buys a long, calm fall for that instead of the eleven
     * odd ticks a ground jump leaves.
     *
     * <p>Two observations, one per side of the wire: {@code getCooldownRemaining()} turning
     * positive is the client tick loop, and the boots taking one point of damage is
     * {@code handleDoubleJump} on the server. The damage is only accepted while the player is
     * still airborne, because landing hard damages armour too and would otherwise make this pass
     * for the wrong reason.
     *
     * <p><b>What breaks this test:</b> unregistering {@code DoubleJumpController::tick} from
     * {@code END_CLIENT_TICK}, the air jump branch losing one of its conditions, the enchantment
     * level no longer being read from the equipped boots, or {@code DoubleJumpPayload} not being
     * sent or not being handled.
     */
    private void testAirJumpRunsOnEveryClientTick(ClientGameTestContext context,
                                                  TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runOnServer(server -> {
            ServerPlayer player = serverPlayer(server);
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            boots.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(ModEnchantments.DOUBLE_JUMP), 1);
            player.setItemSlot(EquipmentSlot.FEET, boots);
            player.inventoryMenu.broadcastChanges();
        });
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        String problem = context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            if (!Simplebuilding.getConfig().enableDoubleJump) {
                return "enableDoubleJump is off in the config, so the controller returns immediately";
            }

            if (Simplebuilding.getConfig().airJumpCooldownTicks <= 0) {
                return "airJumpCooldownTicks is " + Simplebuilding.getConfig().airJumpCooldownTicks
                        + ", so a successful air jump would leave no cooldown to observe";
            }

            if (DoubleJumpController.getDoubleJumpLevel(client.player) != 1) {
                return "the client sees Double Jump level "
                        + DoubleJumpController.getDoubleJumpLevel(client.player)
                        + " on the player instead of 1 - the enchanted boots did not arrive";
            }

            if (client.player.getAbilities().flying) {
                return "the player is flying, which the air jump branch skips";
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Air jump preconditions not met: " + problem);
        }

        int bootDamageBefore = singleplayer.getServer().computeOnServer(server ->
                serverPlayer(server).getItemBySlot(EquipmentSlot.FEET).getDamageValue());

        if (bootDamageBefore != 0) {
            throw new AssertionError("Setup failed: the boots are already damaged (" + bootDamageBefore
                    + "), so the damage caused by the air jump could not be told apart from it.");
        }

        // High enough for a long fall, low enough to stay inside the cleared volume of the scene.
        singleplayer.getServer().runCommand("tp @a 10.5 8.0 16.5 0.0 0.0");
        singleplayer.getConnection().waitForClientboundPackets();

        // A few ticks of falling with the jump key up: this is what puts the controller into the
        // state the air jump needs (not on ground, key not pressed, was not on ground last tick).
        waitForClient(context, client -> client.player != null && !client.player.onGround(), 60,
                "the player never left the ground after being teleported into the air");
        context.waitTicks(3);

        if (context.computeOnClient(client -> DoubleJumpController.getCooldownRemaining()) != 0) {
            throw new AssertionError("Setup failed: the air jump is already on cooldown before the test pressed "
                    + "jump, so a positive cooldown afterwards would prove nothing.");
        }

        context.getInput().holdKey(options -> options.keyJump);

        int cooldown = waitForClientValue(context,
                client -> DoubleJumpController.getCooldownRemaining(), remaining -> remaining > 0, 25,
                "The air jump never fired: DoubleJumpController.getCooldownRemaining() stayed at zero while the "
                        + "player was falling with the jump key held. Either the controller is not called on "
                        + "every client tick any more or one of its conditions no longer matches.");

        context.takeScreenshot("bootstrap-g-air-jump");
        context.getInput().releaseKey(options -> options.keyJump);

        System.out.println("[simplebuilding-test] air jump cooldown after the jump: " + cooldown + " ticks");

        // -1 marks "already landed": from that point on armour damage could be fall damage instead
        // of the air jump's, so the poll has to see the damage before that happens.
        int damage = awaitServerValue(context, singleplayer,
                player -> player.onGround() ? -1 : player.getItemBySlot(EquipmentSlot.FEET).getDamageValue(),
                value -> value >= 1, 40,
                () -> "The air jump never reached the server: the boots took no damage while the player was "
                        + "still airborne, so no DoubleJumpPayload was sent or handled.");

        if (damage != 1) {
            throw new AssertionError("The air jump damaged the boots by " + damage
                    + " points instead of the single point handleDoubleJump applies.");
        }

        singleplayer.getServer().runCommand("tp @a 10.5 0.0 16.5 0.0 0.0");
        singleplayer.getConnection().waitForClientboundPackets();
        waitForGround(context, 100);
    }

    // ------------------------------------------------------------------------------------------
    // Scene helpers
    // ------------------------------------------------------------------------------------------

    private static void giveMainHand(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                     String itemArgument) {
        // Always the same slot, and the selection is pinned to it first: "weapon.mainhand" would
        // follow whatever slot happens to be selected, which earlier steps are allowed to move.
        selectHotbarSlot(context, singleplayer, HOTBAR_SLOT);
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runCommand("item replace entity @a hotbar." + HOTBAR_SLOT + " with " + itemArgument);
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);
    }

    /**
     * Puts an octant carrying both corner positions - and optionally the Locked flag - into
     * {@link #OCTANT_SLOT} and selects that slot.
     *
     * <p>Everything the scroll steps depend on is verified here rather than assumed. The command
     * runner swallows a brigadier parse error (see {@link RendererTestScene}), so a renamed
     * component or a changed nbt spelling would otherwise arrive as a plain octant - and "Pos1 did
     * not move" would then be satisfied by an octant that never had a Pos1 to move.
     */
    private static void giveOctantWithCorners(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                              boolean locked) {
        selectHotbarSlot(context, singleplayer, OCTANT_SLOT);
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runCommand("item replace entity @a hotbar." + OCTANT_SLOT + " with "
                + "simplebuilding:octant["
                + "minecraft:custom_data={"
                + "Pos1:[I;" + OCTANT_POS_1.getX() + "," + OCTANT_POS_1.getY() + "," + OCTANT_POS_1.getZ() + "],"
                + "Pos2:[I;" + OCTANT_POS_2.getX() + "," + OCTANT_POS_2.getY() + "," + OCTANT_POS_2.getZ() + "]"
                + (locked ? ",Locked:1b" : "")
                + "}]");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        String serverProblem = singleplayer.getServer().computeOnServer(server -> {
            ItemStack stack = serverPlayer(server).getInventory().getItem(OCTANT_SLOT);

            if (!(stack.getItem() instanceof OctantItem)) {
                return "hotbar slot " + OCTANT_SLOT + " holds " + stack + " instead of an octant";
            }

            CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            if (nbt.getIntArray("Pos1").orElse(new int[0]).length != 3
                    || nbt.getIntArray("Pos2").orElse(new int[0]).length != 3) {
                return "the octant did not arrive with both corner positions, its custom data is " + nbt;
            }

            if (nbt.getBooleanOr("Locked", false) != locked) {
                return "the octant's Locked flag is " + nbt.getBooleanOr("Locked", false)
                        + " instead of " + locked;
            }

            return null;
        });

        if (serverProblem != null) {
            throw new AssertionError("Octant setup failed on the server: " + serverProblem);
        }

        // The mixin reads the client's main hand, so the client has to see the octant too.
        String clientProblem = context.computeOnClient(client -> client.player != null
                && client.player.getMainHandItem().getItem() instanceof OctantItem
                ? null
                : "the main hand does not hold an OctantItem");

        if (clientProblem != null) {
            throw new AssertionError("Octant setup failed on the client: " + clientProblem + ". "
                    + RendererTestScene.describeAim(context));
        }
    }

    /**
     * Puts a reinforced bundle holding stone into the first non hotbar slot and empties the hand.
     * The contents are built in code rather than through the command's component syntax: the
     * component names are the part most likely to move between versions, and a command that fails
     * to parse is silently swallowed by the client gametest command runner.
     */
    private static void giveBundleWithStone(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                            boolean masterBuilder) {
        selectHotbarSlot(context, singleplayer, HOTBAR_SLOT);
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runOnServer(server -> {
            ServerPlayer player = serverPlayer(server);
            ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);

            if (masterBuilder) {
                bundle.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ModEnchantments.MASTER_BUILDER), 1);
            }

            bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(
                    ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.STONE, BUNDLED_STONE_COUNT)))));

            player.getInventory().setItem(BUNDLE_SLOT, bundle);
            player.inventoryMenu.broadcastChanges();
        });
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);
    }

    /** Scrolls the wheel and gives the packet a client tick out and a server tick in. */
    private static void scrollAndSettle(ClientGameTestContext context, double amount) {
        context.getInput().scroll(amount);
        context.waitTicks(10);
    }

    /**
     * Selects a hotbar slot on the client and waits until the server agrees.
     *
     * <p>Driven from the client rather than the server on purpose: {@code LocalPlayer} notices the
     * changed selection on its next tick and sends {@code ServerboundSetCarriedItemPacket}, so both
     * sides end up on the same slot. Setting it on the server alone would leave the client - which
     * is where the mixin reads the main hand - pointing somewhere else. The result is asserted
     * because a selection that silently stayed put would turn the steps below into no-ops.
     */
    private static void selectHotbarSlot(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                         int slot) {
        context.runOnClient(client -> {
            if (client.player != null) {
                client.player.getInventory().setSelectedSlot(slot);
            }
        });
        context.waitTicks(10);
        singleplayer.getConnection().waitForServerboundPackets();

        int onServer = singleplayer.getServer().computeOnServer(server ->
                serverPlayer(server).getInventory().getSelectedSlot());

        if (onServer != slot) {
            throw new AssertionError("Setup failed: the client selected hotbar slot " + slot
                    + " but the server still has slot " + onServer + " selected, so the two sides would "
                    + "look at different stacks.");
        }
    }

    private static void releaseAllInput(ClientGameTestContext context) {
        context.getInput().releaseControl();
        context.getInput().releaseShift();
        context.getInput().releaseAlt();
        context.getInput().releaseKey(options -> options.keyJump);
        context.getInput().releaseMouse(0);
        context.getInput().releaseMouse(1);
    }

    // ------------------------------------------------------------------------------------------
    // Reading and asserting
    // ------------------------------------------------------------------------------------------

    private static boolean readTrimBenefitConfig(ClientGameTestContext context) {
        return context.computeOnClient(client -> Simplebuilding.getConfig().enableArmorTrimBenefits);
    }

    /**
     * {@code SimplebuildingClient}'s JOIN listener reads the config through
     * {@code AutoConfig.getConfigHolder(...).getConfig()}, which hands out the very instance
     * {@code Simplebuilding.getConfig()} returns - both are the holder's single config object. If
     * that ever stops being true the trim benefit test goes red rather than quietly passing.
     */
    private static void setTrimBenefitConfig(ClientGameTestContext context, boolean enabled) {
        context.runOnClient(client -> Simplebuilding.getConfig().enableArmorTrimBenefits = enabled);
    }

    private static boolean readSpacePressedOnServer(TestSingleplayerContext singleplayer) {
        return singleplayer.getServer().computeOnServer(server ->
                ((ISpaceKeyTracker) serverPlayer(server)).simplebuilding$isSpacePressed());
    }

    private static int[] readOctantIntArray(TestSingleplayerContext singleplayer, String key) {
        return singleplayer.getServer().computeOnServer(server -> {
            CompoundTag nbt = serverPlayer(server).getInventory().getItem(OCTANT_SLOT)
                    .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return nbt.getIntArray(key).orElse(new int[0]);
        });
    }

    private static String readOctantShape(TestSingleplayerContext singleplayer) {
        return singleplayer.getServer().computeOnServer(server -> {
            CompoundTag nbt = serverPlayer(server).getInventory().getItem(OCTANT_SLOT)
                    .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return nbt.getString("Shape").orElse("");
        });
    }

    private static void assertOctantPos(TestSingleplayerContext singleplayer, String key, BlockPos expected,
                                        String when) {
        int[] actual = readOctantIntArray(singleplayer, key);

        if (actual.length != 3) {
            throw new AssertionError("The octant in hotbar slot " + OCTANT_SLOT + " has no usable " + key
                    + " " + when + " (stored value has " + actual.length + " entries instead of 3).");
        }

        BlockPos pos = new BlockPos(actual[0], actual[1], actual[2]);

        if (!pos.equals(expected)) {
            throw new AssertionError("The octant's " + key + " is " + pos + " " + when
                    + ", expected " + expected + ".");
        }
    }

    private static int countStoneInBundle(TestSingleplayerContext singleplayer) {
        return singleplayer.getServer().computeOnServer(server -> {
            BundleContents contents = serverPlayer(server).getInventory().getItem(BUNDLE_SLOT)
                    .get(DataComponents.BUNDLE_CONTENTS);

            if (contents == null) {
                return 0;
            }

            int count = 0;

            for (ItemStackTemplate template : contents.items()) {
                ItemStack stack = template.create();

                if (stack.is(Items.STONE)) {
                    count += stack.getCount();
                }
            }

            return count;
        });
    }

    private static String describeServerMainHand(TestSingleplayerContext singleplayer) {
        return singleplayer.getServer().computeOnServer(server ->
                String.valueOf(serverPlayer(server).getMainHandItem()));
    }

    private static int selectedSlot(ClientGameTestContext context) {
        return context.computeOnClient(client ->
                client.player == null ? -1 : client.player.getInventory().getSelectedSlot());
    }

    private static void assertSelectedSlot(ClientGameTestContext context, int expected, String message) {
        int actual = selectedSlot(context);

        if (actual != expected) {
            throw new AssertionError(message + ": the selected hotbar slot moved from " + expected + " to "
                    + actual + ", so vanilla's scroll handling ran.");
        }
    }

    private static void assertWandInHandWithTouchLevel(ClientGameTestContext context, int expectedLevel) {
        String problem = context.computeOnClient(client -> {
            if (client.player == null || client.level == null) {
                return "no client player or level";
            }

            ItemStack stack = client.player.getMainHandItem();

            if (!(stack.getItem() instanceof BuildingWandItem)) {
                return "the main hand does not hold a BuildingWandItem but " + stack;
            }

            // Read exactly the way SimplebuildingClient reads it, including the client level.
            int level = EnchantmentHelper.getEnchantmentLevel(stack, client.level,
                    ModEnchantments.CONSTRUCTORS_TOUCH);

            if (level != expectedLevel) {
                return "the wand in hand carries Constructor's Touch level " + level
                        + " instead of " + expectedLevel;
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Building wand preconditions not met: " + problem);
        }
    }

    private static void assertNoScreen(ClientGameTestContext context, String situation) {
        String actual = context.computeOnClient(client ->
                client.gui.screen() == null ? null : client.gui.screen().getClass().getName());

        if (actual != null) {
            throw new AssertionError("The settings key opened " + actual + " for " + situation
                    + ", although nothing was supposed to open.");
        }
    }

    private static void assertScreenIs(ClientGameTestContext context, Class<? extends Screen> screenClass,
                                       String label) {
        String actual = context.computeOnClient(client ->
                client.gui.screen() == null ? "none" : client.gui.screen().getClass().getName());

        if (!actual.equals(screenClass.getName())) {
            throw new AssertionError("The " + label + " did not stay open: current screen is " + actual);
        }
    }

    private static void closeScreen(ClientGameTestContext context) {
        context.setScreen(() -> null);
        context.waitForScreen(null);
        context.waitTicks(5);
    }

    // ------------------------------------------------------------------------------------------
    // Polling
    // ------------------------------------------------------------------------------------------

    /**
     * Polls a value read off the server player until it is accepted. A fixed wait would have to be
     * either flaky or slow: a payload needs a client tick to leave and a server tick to arrive.
     *
     * <p>The failure message is built lazily so it can quote the state at the moment the poll gave
     * up. Built eagerly it would quote the state from before the poll even started, which is the
     * state every caller here is trying to move away from.
     */
    private static <T> T awaitServerValue(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                          Function<ServerPlayer, T> read, Predicate<T> accept,
                                          int ticks, Supplier<String> message) {
        T last = null;

        for (int tick = 0; tick < ticks; tick++) {
            T current = singleplayer.getServer().computeOnServer(server -> read.apply(serverPlayer(server)));
            last = current;

            if (accept.test(current)) {
                return current;
            }

            context.waitTick();
        }

        throw new AssertionError(message.get() + " (last value seen on the server after " + ticks
                + " ticks: " + last + ")");
    }

    private static <T> T waitForClientValue(ClientGameTestContext context,
                                            Function<net.minecraft.client.Minecraft, T> read,
                                            Predicate<T> accept, int ticks, String message) {
        T last = null;

        for (int tick = 0; tick < ticks; tick++) {
            T current = context.computeOnClient(read::apply);
            last = current;

            if (accept.test(current)) {
                return current;
            }

            context.waitTick();
        }

        throw new AssertionError(message + " (last value seen on the client after " + ticks + " ticks: "
                + last + ")");
    }

    private static void waitForClient(ClientGameTestContext context,
                                      Predicate<net.minecraft.client.Minecraft> condition,
                                      int ticks, String what) {
        for (int tick = 0; tick < ticks; tick++) {
            if (context.computeOnClient(condition::test)) {
                return;
            }

            context.waitTick();
        }

        throw new AssertionError("Scene setup failed: " + what + " within " + ticks + " ticks. "
                + RendererTestScene.describeAim(context));
    }

    private static void waitForGround(ClientGameTestContext context, int ticks) {
        waitForClient(context, client -> client.player != null && client.player.onGround(), ticks,
                "the player never landed again");
    }

    private static ServerPlayer serverPlayer(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        if (players.size() != 1) {
            throw new AssertionError("Expected exactly one player on the test server, found " + players.size()
                    + ". Every assertion in this class reads that single player.");
        }

        return players.get(0);
    }
}
