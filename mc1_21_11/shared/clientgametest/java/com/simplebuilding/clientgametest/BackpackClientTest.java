package com.simplebuilding.clientgametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.gui.BackpackScreen;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.BackpackSlot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.render.state.ColoredRectangleRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The backpack as a player meets it: the backpack key, the screen it opens, and the placed block.
 *
 * <p>Everything here is client only code that no server test can reach. The key handler
 * ({@code BackpackKeyHandler}) runs in the client tick and decides between "ask the server for the
 * backpack menu" and "open the vanilla inventory"; the screen ({@code BackpackScreen}) lays out
 * and tints the slots; the loader's menu screen registration turns the server's open packet into
 * that screen. The server side of the same flow - which menu the payload opens, how many slots it
 * has, what the slots accept - is pinned by the server tests; this class asserts what arrives on
 * the client.
 *
 * <p><b>What is pinned, case by case:</b>
 * <ul>
 *   <li>Without a worn backpack the key opens exactly vanilla's {@code InventoryScreen} on the
 *       player's own inventory menu (46 slots, none of them a backpack slot) - even with a
 *       backpack in the main hand, because carrying is not wearing.</li>
 *   <li>With a worn backpack the inventory key E still opens exactly vanilla's
 *       {@code InventoryScreen}, and the worn backpack sits in its chest slot.</li>
 *   <li>For each of the four tiers, the key opens {@code BackpackScreen} on a
 *       {@code BackpackMenu} that is the vanilla inventory (slots 0 to 45, container for
 *       container and index for index the player's own inventory menu) plus exactly 9, 18, 33 or
 *       50 backpack slots. The counts are literals here on purpose, not
 *       {@code BackpackTier#slotCount}: a changed tier table is exactly what they are meant to
 *       catch.</li>
 *   <li>The extra columns stand where the owner asked for them: the netherite backpack's six
 *       column slots right of the 9-wide grid, the enderite backpack's fourteen split seven right
 *       and seven left. "Right" and "left" are measured against the menu's own main inventory
 *       slots, not against {@code BackpackLayout}, so a layout that moved everything together
 *       still has to keep that order.</li>
 *   <li>The screen tints every backpack row slot in {@code TINT_BACKPACK_ROW} and every column
 *       slot in {@code TINT_EXTRA_COLUMN}, the two colours differ, and no vanilla slot carries
 *       either. Read out of the screen's own render state (a CPU only extraction pass, see
 *       {@link #drawnTints}), which states the colour and the 16x16 square exactly; a screenshot
 *       could only say "something is a bit brown there".</li>
 *   <li>A placed backpack opens the same screen on a right click with an empty hand, in placed
 *       mode, with no backpack worn at all.</li>
 * </ul>
 *
 * <p>Every trigger condition is asserted before the thing it triggers is looked at - the
 * backpack really is in the chest slot (or really is not), the binding really sits on the key
 * the harness presses, the placed block really stands where the crosshair points - so a setup
 * that quietly failed is reported as a setup failure and never as a broken backpack.
 *
 * <p><b>The two screenshots are checkpoints, not measurements.</b> Nothing is compared on them;
 * they document the enderite screen (both columns) and the placed netherite screen for a human
 * reading the artefacts.
 *
 * <p><b>What breaks this test:</b>
 * <ul>
 *   <li>the backpack binding not registered or moved off B;</li>
 *   <li>{@code BackpackKeyHandler} losing either branch - no payload for a worn backpack (no
 *       backpack screen opens), or no vanilla inventory without one - or testing the main hand
 *       instead of the chest slot;</li>
 *   <li>anything that makes the inventory key open the backpack instead of the vanilla
 *       inventory;</li>
 *   <li>a tier with a different row or column count ({@code BackpackTier}), a menu that adds its
 *       backpack slots in the middle of the vanilla range or builds the vanilla part differently
 *       ({@code BackpackMenu});</li>
 *   <li>the columns moving to the other side of the grid ({@code BackpackLayout#extraColumnX});</li>
 *   <li>the tint colours becoming equal, a slot getting the other colour (for example
 *       {@code BackpackSlot#isExtraColumn} misclassifying a slot) or a tint square moving off its
 *       slot ({@code BackpackScreen#extractBackground});</li>
 *   <li>the placed block not opening on a right click ({@code BackpackBlock#useWithoutItem}) or
 *       opening in worn mode;</li>
 *   <li>the menu screen registration missing on a loader - then no screen opens at all.</li>
 * </ul>
 *
 * <p><b>Not covered:</b> closing the backpack screen with its own key. The NeoForge drivers press
 * keys through the binding layer ({@code KeyMapping.click}), which never reaches an open screen,
 * so that step would pass on Fabric and fail on NeoForge for a reason in the harness; the screens
 * are closed through {@code Screen#onClose}, the path Escape takes. Also not covered here: moving
 * items (the server tests drive the menu's clicks), the recipe book with a wide backpack screen,
 * and Deep Pockets counts in the slots.
 */
public final class BackpackClientTest {

    /**
     * The key the harness presses for the backpack.
     *
     * <p>Every loader registers {@code key.simplebuilding.open_backpack} on B;
     * {@link #assertBackpackKeyIsBound} keeps that assumption honest at run time.
     */
    private static final int BACKPACK_KEY = InputConstants.KEY_B;

    /** Vanilla's default inventory key; checked against the binding before it is pressed. */
    private static final int INVENTORY_KEY = InputConstants.KEY_E;

    /** Slots 0 to 45 of vanilla's {@code InventoryMenu}: result, 2x2 grid, armor, main, hotbar, offhand. */
    private static final int VANILLA_SLOTS = 46;

    /** The chest armor slot in the vanilla slot order (5 head, 6 chest, 7 legs, 8 feet). */
    private static final int CHEST_SLOT = 6;

    /** Budget for a screen to appear; the backpack screen needs a round trip to the server. */
    private static final int SCREEN_TIMEOUT_TICKS = 100;

    /** How long every opened screen has to survive before it is inspected: init plus twenty renders. */
    private static final int RENDER_TICKS = 20;

    /** Where the placed backpack stands: free air one block in front of the wall, in the crosshair. */
    private static final BlockPos PLACED_POS = new BlockPos(10, 1, TestScene.WALL_Z - 1);

    /** The spot {@link TestScene#build} teleports to, repeated so the aim can be restored. */
    private static final String PLAYER_SPOT = "10.5 0.0 16.5";

    private static final Field LEFT_POS = screenField("leftPos");
    private static final Field TOP_POS = screenField("topPos");

    /**
     * One tier as the player sees it: the item, its backpack slot count, and how many of those
     * slots stand in columns right and left of the grid.
     *
     * @param shot the screenshot taken of this tier's screen, or null for none
     */
    private record Tier(String itemId, int slots, int rightColumnSlots, int leftColumnSlots, String shot) {
    }

    private static final List<Tier> TIERS = List.of(
            new Tier("simplebuilding:backpack", 9, 0, 0, null),
            new Tier("simplebuilding:reinforced_backpack", 18, 0, 0, null),
            new Tier("simplebuilding:netherite_backpack", 33, 6, 0, null),
            new Tier("simplebuilding:enderite_backpack", 50, 7, 7, "backpack-a-enderite-worn"));

    /** The placed case uses the netherite tier: one column, so the placed screen shows a column too. */
    private static final Tier PLACED = new Tier("simplebuilding:netherite_backpack", 33, 6, 0,
            "backpack-b-netherite-placed");

    private BackpackClientTest() {
    }

    /**
     * The whole test, as steps.
     *
     * <p>Survival on purpose: in creative the inventory key and vanilla's {@code InventoryScreen}
     * both hand over to the creative inventory, and "the key opens the vanilla inventory" would
     * be a statement about a different screen.
     */
    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "survival");
        closeWhatTheServerStillHolds(script);

        keyWithoutAWornBackpackOpensTheVanillaInventory(script);
        inventoryKeyStaysVanillaWithAWornBackpack(script);

        for (Tier tier : TIERS) {
            keyOpensTheWornBackpack(script, tier);
        }

        placedBackpackOpensOnRightClick(script);

        putTheWorldBack(script);
    }

    /**
     * Sends the server a close for whatever container it still holds open for the player.
     *
     * <p>The server refuses the backpack payload while any other menu is open, and closing a
     * screen with {@code setScreen(null)} - which is how the scene build and earlier scripts close
     * theirs - leaves the server's menu open. {@code LocalPlayer#closeContainer} is vanilla's own
     * close: the packet first, then the screen. It is hygiene against the previous scripts, not
     * against the mod: every case below closes its own screens the same way.
     */
    private static void closeWhatTheServerStillHolds(Script script) {
        script.act("close any container the server still holds open for the player", client -> {
            if (client.player != null) {
                client.player.closeContainer();
            }
        });
        script.awaitPackets();
        script.idle("let the server process the close", 10);
    }

    /**
     * The key without a worn backpack: vanilla's inventory, nothing else - with a backpack in the
     * main hand, so the case also says the key asks about the chest slot and not about the hand.
     *
     * <p><b>What breaks this case:</b> the vanilla branch of the key handler lost (nothing opens),
     * or the handler testing any slot but the chest (it sends the payload, the server refuses, and
     * nothing opens either).
     */
    private static void keyWithoutAWornBackpackOpensTheVanillaInventory(Script script) {
        script.command("clear @a", true);
        script.command("item replace entity @a weapon.mainhand with simplebuilding:enderite_backpack");
        script.awaitPackets();
        script.idle("let the carried backpack arrive", 10);

        script.act("the backpack is carried in the main hand and nothing is worn", client -> {
            ItemStack hand = client.player.getMainHandItem();
            ItemStack chest = client.player.getItemBySlot(EquipmentSlot.CHEST);

            if (!(hand.getItem() instanceof BackpackItem) || !chest.isEmpty()) {
                throw new AssertionError("Carried backpack setup failed: the main hand holds " + hand
                        + " and the chest slot holds " + chest + ", but the case needs a backpack in the "
                        + "hand and nothing worn.");
            }
        });

        pressBackpackKey(script, "without a worn backpack");
        awaitScreen(script, InventoryScreen.class, "vanilla inventory via the backpack key");
        script.idle("let the vanilla inventory render " + RENDER_TICKS + " frames", RENDER_TICKS);

        assertStillOpen(script, InventoryScreen.class, "vanilla inventory via the backpack key");
        assertVanillaInventoryMenu(script, "the backpack key without a worn backpack");
        closeScreen(script, "vanilla inventory");
    }

    /**
     * E with a worn backpack: still vanilla's inventory, and the backpack visible in its chest slot.
     *
     * <p><b>What breaks this case:</b> anything that turns the inventory key into a second backpack
     * key - an {@code InventoryScreen} hook that forwards to the backpack menu, or a key handler that
     * listens to E.
     */
    private static void inventoryKeyStaysVanillaWithAWornBackpack(Script script) {
        script.command("clear @a", true);
        script.command("item replace entity @a armor.chest with simplebuilding:enderite_backpack");
        script.awaitPackets();
        script.idle("let the worn backpack arrive", 10);
        assertWorn(script, "simplebuilding:enderite_backpack");

        script.act("the inventory binding sits on the key the harness presses", client -> {
            if (!client.options.keyInventory.saveString().equals(InputConstants.Type.KEYSYM.getOrCreate(INVENTORY_KEY).getName())) {
                throw new AssertionError("The inventory binding is not on GLFW key " + INVENTORY_KEY
                        + " any more (it says \"" + client.options.keyInventory.saveString() + "\"), so "
                        + "the key the harness presses would not open the inventory.");
            }
        });
        script.harness("press the inventory key with a worn backpack", harness -> harness.pressKey(INVENTORY_KEY));
        awaitScreen(script, InventoryScreen.class, "vanilla inventory via the inventory key");
        script.idle("let the vanilla inventory render " + RENDER_TICKS + " frames", RENDER_TICKS);

        assertStillOpen(script, InventoryScreen.class, "vanilla inventory via the inventory key");
        assertVanillaInventoryMenu(script, "the inventory key with a worn backpack");

        script.act("the vanilla inventory shows the worn backpack in its chest slot", client -> {
            AbstractContainerMenu menu = client.player.containerMenu;
            ItemStack inChestSlot = menu.slots.get(CHEST_SLOT).getItem();

            if (!(inChestSlot.getItem() instanceof BackpackItem)) {
                throw new AssertionError("The inventory key opened the vanilla inventory, but its chest "
                        + "slot shows " + inChestSlot + " instead of the worn backpack.");
            }
        });
        closeScreen(script, "vanilla inventory");
    }

    /**
     * One tier, worn, opened with the key: the screen, the slot count, the columns, the tints.
     *
     * <p><b>What breaks this case:</b> see the class javadoc - every item from the binding down to
     * the tint squares has its own assertion here and its own message.
     */
    private static void keyOpensTheWornBackpack(Script script, Tier tier) {
        String label = tier.itemId() + " screen";

        script.command("clear @a", true);
        script.command("item replace entity @a armor.chest with " + tier.itemId());
        script.awaitPackets();
        script.idle("let the worn " + tier.itemId() + " arrive", 10);
        assertWorn(script, tier.itemId());

        pressBackpackKey(script, "with a worn " + tier.itemId());
        awaitScreen(script, BackpackScreen.class, label);
        script.idle("let the " + label + " render " + RENDER_TICKS + " frames", RENDER_TICKS);
        assertStillOpen(script, BackpackScreen.class, label);

        script.act("the " + label + " is in worn mode", client -> {
            BackpackMenu menu = backpackMenu(client, label);

            if (menu.openData().placed()) {
                throw new AssertionError("The backpack key opened the " + label + " in placed mode; a "
                        + "worn backpack has to open in worn mode (that is the mode that locks the chest slot).");
            }
        });
        assertSlotsAndColumns(script, tier, label);
        assertTints(script, label);

        if (tier.shot() != null) {
            script.shot(tier.shot());
        }

        closeScreen(script, label);
    }

    /**
     * A placed netherite backpack, right clicked with an empty hand and nothing worn: the backpack
     * screen in placed mode, with the same slots, column and tints as the worn one.
     *
     * <p><b>What breaks this case:</b> {@code BackpackBlock#useWithoutItem} no longer opening the
     * menu, the placed menu opening in worn mode, or the placed screen losing its column or tints.
     */
    private static void placedBackpackOpensOnRightClick(Script script) {
        String label = "placed " + PLACED.itemId() + " screen";

        script.command("clear @a", true);
        script.command("setblock " + PLACED_POS.getX() + " " + PLACED_POS.getY() + " " + PLACED_POS.getZ()
                + " " + PLACED.itemId());
        script.awaitPackets();
        script.idle("let the placed backpack reach the client", 10);

        script.act("the netherite backpack really stands in the world and nothing is worn", client -> {
            if (client.level == null || !client.level.getBlockState(PLACED_POS).is(ModBlocks.NETHERITE_BACKPACK)) {
                throw new AssertionError("Placed backpack setup failed: block " + PLACED_POS + " is "
                        + (client.level == null ? "in no level" : client.level.getBlockState(PLACED_POS))
                        + ", so the right click below would open nothing.");
            }

            if (!client.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    || !client.player.getMainHandItem().isEmpty()) {
                throw new AssertionError("Placed backpack setup failed: the player wears "
                        + client.player.getItemBySlot(EquipmentSlot.CHEST) + " and holds "
                        + client.player.getMainHandItem() + ", but the case needs an empty chest slot (so "
                        + "only the placed block can open a backpack screen) and an empty hand.");
            }
        });

        script.command("tp @a " + PLAYER_SPOT + " 0.0 0.0");
        script.awaitPackets();
        script.idle("let the view angles reach the client", 10);
        TestScene.assertAimedAt(script, PLACED_POS, Direction.NORTH);

        script.harness("right click the placed backpack", harness -> harness.pressMouse(1));
        awaitScreen(script, BackpackScreen.class, label);
        script.idle("let the " + label + " render " + RENDER_TICKS + " frames", RENDER_TICKS);
        assertStillOpen(script, BackpackScreen.class, label);

        script.act("the " + label + " is in placed mode", client -> {
            BackpackMenu menu = backpackMenu(client, label);

            if (!menu.openData().placed() || !menu.openData().pos().equals(PLACED_POS)) {
                throw new AssertionError("The right click opened the " + label + " with placed="
                        + menu.openData().placed() + " at " + menu.openData().pos() + ", expected placed mode "
                        + "at " + PLACED_POS + ".");
            }
        });
        assertSlotsAndColumns(script, PLACED, label);
        assertTints(script, label);
        script.shot(PLACED.shot());
        closeScreen(script, label);

        script.command("setblock " + PLACED_POS.getX() + " " + PLACED_POS.getY() + " " + PLACED_POS.getZ()
                + " minecraft:air", true);
        script.awaitPackets();
        script.idle("let the removed backpack reach the client", 10);
    }

    // =================================================================================
    // Assertions
    // =================================================================================

    /**
     * The open backpack menu is the vanilla inventory plus exactly the tier's backpack slots, and
     * its columns stand on the side the tier promises.
     */
    private static void assertSlotsAndColumns(Script script, Tier tier, String label) {
        script.act("the " + label + " holds the vanilla inventory plus exactly " + tier.slots()
                + " backpack slots", client -> {
            BackpackMenu menu = backpackMenu(client, label);
            List<Slot> slots = menu.slots;

            if (client.player.containerMenu != menu) {
                throw new AssertionError("The " + label + " shows a menu that is not the player's open "
                        + "container menu (" + client.player.containerMenu + "), so clicks would go elsewhere.");
            }

            if (slots.size() != VANILLA_SLOTS + tier.slots()) {
                throw new AssertionError("The " + label + " has " + slots.size() + " slots, expected the "
                        + VANILLA_SLOTS + " of the vanilla inventory plus " + tier.slots() + " backpack slots = "
                        + (VANILLA_SLOTS + tier.slots()) + ".");
            }

            List<String> problems = new ArrayList<>();
            List<Slot> vanilla = client.player.inventoryMenu.slots;
            int backpackSlots = 0;

            for (int i = 0; i < slots.size(); i++) {
                Slot slot = slots.get(i);

                if (slot instanceof BackpackSlot) {
                    backpackSlots++;
                    if (i < VANILLA_SLOTS) {
                        problems.add("slot " + i + " is a backpack slot inside the vanilla range 0.." + (VANILLA_SLOTS - 1));
                    }
                    continue;
                }

                if (i >= VANILLA_SLOTS) {
                    problems.add("slot " + i + " behind the vanilla range is not a backpack slot but " + slot);
                    continue;
                }

                // Result and 2x2 grid (0..4) belong to the menu's own crafting containers, armor,
                // main inventory, hotbar and offhand (5..45) to the player's inventory - as in vanilla.
                Slot own = vanilla.get(i);
                boolean inPlayerInventory = slot.container == client.player.getInventory();

                if (slot.getContainerSlot() != own.getContainerSlot() || inPlayerInventory != (i >= 5)) {
                    problems.add("slot " + i + " is index " + slot.getContainerSlot() + " of "
                            + (inPlayerInventory ? "the player's inventory" : slot.container.getClass().getSimpleName())
                            + " where the vanilla inventory menu has index " + own.getContainerSlot() + " of "
                            + (i >= 5 ? "the player's inventory" : "its crafting grid"));
                }
            }

            if (backpackSlots != tier.slots()) {
                problems.add(backpackSlots + " backpack slots instead of " + tier.slots());
            }

            // Right and left of the grid, measured against the menu's own main inventory slots.
            int gridLeft = Integer.MAX_VALUE;
            int gridRight = Integer.MIN_VALUE;
            int mainTop = Integer.MAX_VALUE;

            for (int i = 9; i < 45; i++) {
                gridLeft = Math.min(gridLeft, slots.get(i).x);
                gridRight = Math.max(gridRight, slots.get(i).x);
                if (i < 36) {
                    mainTop = Math.min(mainTop, slots.get(i).y);
                }
            }

            int right = 0;
            int left = 0;
            int rows = 0;

            for (Slot slot : slots) {
                if (!(slot instanceof BackpackSlot)) {
                    continue;
                }
                if (slot.x > gridRight) {
                    right++;
                } else if (slot.x < gridLeft) {
                    left++;
                } else {
                    rows++;
                    if (slot.y >= mainTop) {
                        problems.add("backpack row slot at " + slot.x + "/" + slot.y + " stands among or below "
                                + "the main inventory rows (they start at y " + mainTop + ")");
                    }
                }
            }

            if (right != tier.rightColumnSlots() || left != tier.leftColumnSlots()) {
                problems.add(right + " column slots right of the grid and " + left + " left of it, expected "
                        + tier.rightColumnSlots() + " right and " + tier.leftColumnSlots() + " left (grid x "
                        + gridLeft + ".." + gridRight + ")");
            }

            if (rows % 9 != 0) {
                problems.add(rows + " backpack slots inside the grid, which is not a whole number of 9-wide rows");
            }

            if (!problems.isEmpty()) {
                throw new AssertionError("The " + label + " is laid out wrong: " + problems);
            }
        });
    }

    /**
     * Every backpack row slot carries a 16x16 square in the row tint, every column slot one in the
     * column tint, and nothing else carries either.
     *
     * <p>Which slot is a row slot and which a column slot is decided by position here (inside or
     * outside the menu's main inventory x range), not by {@code BackpackSlot#isExtraColumn}, so a
     * misclassified slot shows up as a square in the wrong colour.
     */
    private static void assertTints(Script script, String label) {
        script.act("the " + label + " tints its rows and its columns in two different colours", client -> {
            if (BackpackScreen.TINT_BACKPACK_ROW == BackpackScreen.TINT_EXTRA_COLUMN) {
                throw new AssertionError(String.format("The backpack rows and the extra columns share the tint "
                        + "0x%08X, so the screen cannot tell them apart.", BackpackScreen.TINT_BACKPACK_ROW));
            }

            AbstractContainerScreen<?> screen = containerScreen(client);
            List<Slot> slots = backpackMenu(client, label).slots;
            int left = leftPos(screen);
            int top = topPos(screen);

            int gridLeft = Integer.MAX_VALUE;
            int gridRight = Integer.MIN_VALUE;
            for (int i = 9; i < 45; i++) {
                gridLeft = Math.min(gridLeft, slots.get(i).x);
                gridRight = Math.max(gridRight, slots.get(i).x);
            }

            Set<String> expectedRows = new TreeSet<>();
            Set<String> expectedColumns = new TreeSet<>();

            for (Slot slot : slots) {
                if (slot instanceof BackpackSlot) {
                    String square = square(left + slot.x, top + slot.y, 16, 16);
                    boolean column = slot.x > gridRight || slot.x < gridLeft;
                    (column ? expectedColumns : expectedRows).add(square);
                }
            }

            Set<String> drawnRows = drawnTints(client, BackpackScreen.TINT_BACKPACK_ROW);
            Set<String> drawnColumns = drawnTints(client, BackpackScreen.TINT_EXTRA_COLUMN);

            if (!drawnRows.equals(expectedRows) || !drawnColumns.equals(expectedColumns)) {
                throw new AssertionError(String.format("The %s tints the wrong squares. Row tint 0x%08X: "
                                + "missing %s, extra %s. Column tint 0x%08X: missing %s, extra %s.",
                        label, BackpackScreen.TINT_BACKPACK_ROW, minus(expectedRows, drawnRows),
                        minus(drawnRows, expectedRows), BackpackScreen.TINT_EXTRA_COLUMN,
                        minus(expectedColumns, drawnColumns), minus(drawnColumns, expectedColumns)));
            }
        });
    }

    /** Fails unless the chest slot holds exactly that item, as the client sees it. */
    private static void assertWorn(Script script, String itemId) {
        script.act("the client sees " + itemId + " in the chest slot", client -> {
            ItemStack chest = client.player.getItemBySlot(EquipmentSlot.CHEST);
            String worn = chest.isEmpty() ? "nothing" : BuiltInRegistries.ITEM.getKey(chest.getItem()).toString();

            if (!worn.equals(itemId)) {
                throw new AssertionError("Worn backpack setup failed: the chest slot holds " + worn
                        + " instead of " + itemId + ", so the backpack key would be tested on the wrong thing.");
            }
        });
    }

    /** The open screen shows the player's own inventory menu and nothing more. */
    private static void assertVanillaInventoryMenu(Script script, String how) {
        script.act("the screen opened by " + how + " is the player's own inventory menu", client -> {
            AbstractContainerMenu menu = containerScreen(client).getMenu();
            int backpackSlots = 0;

            for (Slot slot : menu.slots) {
                if (slot instanceof BackpackSlot) {
                    backpackSlots++;
                }
            }

            if (menu != client.player.inventoryMenu || menu.slots.size() != VANILLA_SLOTS || backpackSlots != 0) {
                throw new AssertionError("The screen opened by " + how + " shows " + menu + " with "
                        + menu.slots.size() + " slots, " + backpackSlots + " of them backpack slots; expected "
                        + "the player's own inventory menu with its " + VANILLA_SLOTS + " vanilla slots.");
            }
        });
    }

    /** The mod's backpack binding really is the key the harness presses. */
    private static void assertBackpackKeyIsBound(Script script) {
        script.act("the backpack binding sits on the key the harness presses", client -> {
            KeyMapping binding = ClientState.backpackKey;

            if (binding == null) {
                throw new AssertionError("ClientState.backpackKey is null, so the mod never registered its "
                        + "backpack binding and no key press could open the backpack.");
            }

            if (!binding.saveString().equals(InputConstants.Type.KEYSYM.getOrCreate(BACKPACK_KEY).getName())) {
                throw new AssertionError("The backpack binding is not on GLFW key " + BACKPACK_KEY
                        + " any more (it says \"" + binding.saveString() + "\"), so the key the harness presses "
                        + "would open nothing and the backpack would be reported as broken.");
            }
        });
    }

    private static void pressBackpackKey(Script script, String why) {
        assertBackpackKeyIsBound(script);
        script.harness("press the backpack key " + why, harness -> harness.pressKey(BACKPACK_KEY));
    }

    // =================================================================================
    // Screens
    // =================================================================================

    /** Waits until a screen of that class is open, and says what was open instead if not. */
    private static void awaitScreen(Script script, Class<? extends Screen> screenClass, String label) {
        script.await("wait for the " + label, SCREEN_TIMEOUT_TICKS,
                client -> screenClass.isInstance(client.screen),
                client -> "the " + label + " never opened; the current screen is " + describeScreen(client)
                        + ", the chest slot holds " + (client.player == null ? "?"
                        : client.player.getItemBySlot(EquipmentSlot.CHEST)) + ". " + TestScene.describeAim(client));
    }

    /** Fails unless exactly that screen class is open - a subclass would be a different screen. */
    private static void assertStillOpen(Script script, Class<? extends Screen> screenClass, String label) {
        script.act("the " + label + " is still open", client -> {
            String actual = describeScreen(client);

            if (!actual.equals(screenClass.getName())) {
                throw new AssertionError("The " + label + " did not stay open: current screen is " + actual);
            }
        });
    }

    /**
     * Closes the open screen the way Escape does ({@code Screen#onClose}: for a container screen
     * that sends the server its close first) and waits until the server has seen it.
     *
     * <p>Not {@code setScreen(null)}: that closes only the client's screen, the server keeps the
     * menu open and refuses the next backpack payload.
     */
    private static void closeScreen(Script script, String label) {
        script.act("close the " + label, client -> {
            Screen screen = client.screen;

            if (screen != null) {
                screen.onClose();
            }
        });
        script.await("wait until no screen is open any more", SCREEN_TIMEOUT_TICKS,
                client -> client.screen == null,
                client -> "a screen is still open after closing the " + label + ": " + describeScreen(client));
        script.awaitPackets();
        script.idle("let the server process the close", 10);
    }

    /** Puts back what this class changed; also redone by the next scene build. */
    private static void putTheWorldBack(Script script) {
        script.act("make sure no screen is left open", client -> {
            if (client.player != null) {
                client.player.closeContainer();
            }
        });
        script.command("setblock " + PLACED_POS.getX() + " " + PLACED_POS.getY() + " " + PLACED_POS.getZ()
                + " minecraft:air", true);
        script.command("clear @a", true);
        script.command("tp @a " + PLAYER_SPOT + " 0.0 0.0");
        script.awaitPackets();
        script.idle("let the restored world reach the client", 10);
        TestScene.makeRenderingDeterministic(script);
    }

    private static BackpackMenu backpackMenu(Minecraft client, String label) {
        AbstractContainerScreen<?> screen = containerScreen(client);

        if (!(screen.getMenu() instanceof BackpackMenu menu)) {
            throw new AssertionError("The " + label + " does not show a BackpackMenu but " + screen.getMenu());
        }

        return menu;
    }

    private static AbstractContainerScreen<?> containerScreen(Minecraft client) {
        Screen screen = client.screen;

        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            throw new AssertionError("Expected a container screen to be open but found " + describeScreen(client));
        }

        return containerScreen;
    }

    private static String describeScreen(Minecraft client) {
        return client.screen == null ? "none" : client.screen.getClass().getName();
    }

    // =================================================================================
    // Render state
    // =================================================================================

    /**
     * Every filled rectangle of the open screen in exactly {@code colour}, as on-screen squares.
     *
     * <p>A CPU only pass: the screen extracts into a throwaway {@code GuiRenderState} and nothing
     * reaches the GPU. The full pass with the background, because the tints are drawn in the
     * background part ({@code extractBackground} on 26.2, {@code renderBg} on 1.21.11) and the plain
     * render pass does not include it. The mouse is outside the window, so no slot is hovered.
     *
     * <p>Corners go through the rectangle's pose, and min/max rather than named corners:
     * {@code fill} swaps them so that x0 holds the larger value.
     */
    private static Set<String> drawnTints(Minecraft client, int colour) {
        GuiRenderState state = new GuiRenderState();
        GuiGraphics graphics = new GuiGraphics(client, state,
                client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        Screen screen = client.screen;

        if (screen == null) {
            throw new AssertionError("No screen is open, so there is nothing to extract.");
        }

        screen.renderWithTooltipAndSubtitles(graphics, -1, -1, 0.0f);

        Set<String> squares = new TreeSet<>();
        state.forEachElement(element -> {
            if (element instanceof ColoredRectangleRenderState rectangle && rectangle.col1() == colour) {
                org.joml.Vector2f a = rectangle.pose().transformPosition(rectangle.x0(), rectangle.y0(), new org.joml.Vector2f());
                org.joml.Vector2f b = rectangle.pose().transformPosition(rectangle.x1(), rectangle.y1(), new org.joml.Vector2f());
                int x0 = Math.round(Math.min(a.x, b.x));
                int y0 = Math.round(Math.min(a.y, b.y));
                int x1 = Math.round(Math.max(a.x, b.x));
                int y1 = Math.round(Math.max(a.y, b.y));
                squares.add(square(x0, y0, x1 - x0, y1 - y0));
            }
        }, GuiRenderState.TraverseRange.ALL);
        return squares;
    }

    private static String square(int x, int y, int width, int height) {
        return String.format("%04d/%04d %dx%d", x, y, width, height);
    }

    private static Set<String> minus(Set<String> a, Set<String> b) {
        Set<String> rest = new TreeSet<>(a);
        rest.removeAll(b);
        return rest;
    }

    private static int leftPos(AbstractContainerScreen<?> screen) {
        try {
            return LEFT_POS.getInt(screen);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not read AbstractContainerScreen.leftPos", e);
        }
    }

    private static int topPos(AbstractContainerScreen<?> screen) {
        try {
            return TOP_POS.getInt(screen);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not read AbstractContainerScreen.topPos", e);
        }
    }

    private static Field screenField(String name) {
        try {
            Field field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("AbstractContainerScreen has no field '" + name + "' any more. The "
                    + "backpack test reads the GUI origin through it because Minecraft offers no getter - "
                    + "this test needs updating, the mod does not.", e);
        }
    }
}
