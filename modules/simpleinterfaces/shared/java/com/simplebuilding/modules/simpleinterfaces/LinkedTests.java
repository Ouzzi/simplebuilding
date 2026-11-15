package com.simplebuilding.modules.simpleinterfaces;

import java.util.ArrayList;
import com.simplebuilding.modules.simpleinterfaces.SimpleInterfaces;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedContainers;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedSlot;
import com.simplebuilding.modules.simpleinterfaces.linked.PortableContainers;
import com.simplebuilding.modules.simpleinterfaces.linked.PortableMenu;
import com.simplelib.api.InteractionGuard;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

/** Linked GUIs and Easy Shulkers / Ender Chests (owner queue 2026-10-04). */
public final class LinkedTests {
    /** Linked containers and Easy Shulkers (moved from Simple QoL); registered next to ContainerTests on every loader. */
    public static final java.util.Map<String, java.util.function.Consumer<GameTestHelper>> ALL = new java.util.LinkedHashMap<>();
    static {
        ALL.put("linked_mark", LinkedTests::linkedMark);
        ALL.put("linked_range", LinkedTests::linkedRange);
        ALL.put("linked_transfer", LinkedTests::linkedTransfer);
        ALL.put("linked_inventory", LinkedTests::linkedInventory);
        ALL.put("linked_end", LinkedTests::linkedEnd);
        ALL.put("linked_creative", LinkedTests::linkedCreative);
        ALL.put("portable_shulker", LinkedTests::portableShulker);
        ALL.put("portable_ender_chest", LinkedTests::portableEnderChest);
        ALL.put("linked_qol_migration", LinkedTests::qolMigration);
        ALL.put("linked_sync", LinkedTests::sync);
        ALL.put("linked_super_switch", LinkedTests::superSwitch);
        ALL.put("linked_multi_mark", LinkedTests::multiMark);
        ALL.put("linked_multi_transfer", LinkedTests::multiTransfer);
        ALL.put("linked_layout_modes", LinkedTests::layoutModes);
        ALL.put("linked_layout_margins", LinkedTests::layoutMargins);
    }

    private LinkedTests() {
    }

    /** First start: the old qOL container values are taken over once into simpleinterfaces-server.json, never the style file. */
    public static void qolMigration(GameTestHelper h) {
        try {
            var dir = java.nio.file.Files.createTempDirectory("simpleinterfaces-migration");
            java.nio.file.Files.writeString(dir.resolve("simplequalityoflife.json"), "{\"qOL\":{\"linkedContainerRange\":32,\"enableEasyEnderChests\":false,\"enableAutowalk\":true}}");
            var taken = SimpleInterfaces.read(dir);
            h.assertTrue(taken.linkedContainerRange == 32 && !taken.enableEasyEnderChests && taken.enableEasyShulkers && taken.enableLinkedContainers, "old QoL values taken over, others default");
            h.assertTrue(java.nio.file.Files.isRegularFile(dir.resolve("simpleinterfaces-server.json")) && !java.nio.file.Files.exists(dir.resolve("simpleinterfaces.json")), "own server file written, style file untouched");
            java.nio.file.Files.writeString(dir.resolve("simpleinterfaces-server.json"), "{\"linkedContainerRange\":999}");
            var own = SimpleInterfaces.read(dir);
            h.assertTrue(own.linkedContainerRange == LinkedConfig.MAX_LINKED_RANGE && own.enableEasyEnderChests, "own file wins over QoL, bounded");
            var empty = SimpleInterfaces.read(java.nio.file.Files.createTempDirectory("simpleinterfaces-empty"));
            h.assertTrue(empty.linkedContainerRange == 64 && empty.enableEasyShulkers, "no file: defaults");
        } catch (java.io.IOException e) {
            h.fail("migration io: " + e);
        }
        h.succeed();
    }

    /** The client predicts with the server values: a valid payload is kept, garbage ignored, leaving forgets it. */
    public static void sync(GameTestHelper h) {
        configured(() -> {
            try {
                SimpleInterfaces.config().enableEasyShulkers = false;
                SimpleInterfaces.receiveSync(SimpleInterfaces.syncJson());
                h.assertTrue(SimpleInterfaces.syncedConfig() != null && !SimpleInterfaces.syncedConfig().enableEasyShulkers, "server value arrives");
                SimpleInterfaces.receiveSync("{not json");
                h.assertTrue(!SimpleInterfaces.syncedConfig().enableEasyShulkers, "invalid payload ignored");
                h.assertTrue(SimpleInterfaces.configFor(h.getLevel()) == SimpleInterfaces.config(), "server level reads the server values");
            } finally {
                SimpleInterfaces.clearSync();
            }
        });
        h.assertTrue(SimpleInterfaces.syncedConfig() == null, "cleared");
        h.succeed();
    }

    /** "Enable Simple Interfaces" off in the super-mod: no marks, boxes stay closed. */
    public static void superSwitch(GameTestHelper h) {
        configured(() -> {
            var p = player(h, new BlockPos(1, 2, 3));
            p.getInventory().setItem(3, new ItemStack(Items.SHULKER_BOX));
            com.simplebuilding.framework.api.SubMods.set(SimpleInterfaces.MOD_ID, false);
            h.assertTrue(!SimpleInterfaces.active() && !ContainersConfig.on(), "one switch for style and linked containers");
            h.assertTrue(!PortableContainers.open(p, 3), "switched-off sub-mod: boxes stay closed");
            var pos = new BlockPos(1, 2, 1);
            h.setBlock(pos, Blocks.CHEST);
            p.setShiftKeyDown(true);
            h.assertTrue(LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, h.absolutePos(pos)) == net.minecraft.world.InteractionResult.PASS, "switched-off sub-mod: no mark");
        });
        h.succeed();
    }

    private static void configured(Runnable body) {
        var old = SimpleInterfaces.CONFIG;
        var guard = InteractionGuard.permission;
        var sync = LinkedContainers.openSync;
        try {
            SimpleInterfaces.CONFIG = new LinkedConfig();
            LinkedContainers.openSync = (player, payload) -> true;
            body.run();
        } finally {
            SimpleInterfaces.CONFIG = old;
            com.simplebuilding.framework.api.SubMods.set(SimpleInterfaces.MOD_ID, true);
            InteractionGuard.permission = guard;
            LinkedContainers.openSync = sync;
        }
    }

    private static ServerPlayer player(GameTestHelper h, BlockPos at) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.setPos(Vec3.atBottomCenterOf(h.absolutePos(at)));
        return p;
    }

    private static int count(Container container, Item item) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) if (container.getItem(i).is(item)) n += container.getItem(i).getCount();
        return n;
    }

    private static int contents(ItemStack box, Item item) {
        return box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static int firstLinked(AbstractContainerMenu menu) {
        for (int i = 0; i < menu.slots.size(); i++) if (menu.slots.get(i) instanceof LinkedSlot) return i;
        return -1;
    }

    /** Sneak + empty hand marks, again unmarks; double chest halves count as one; vetoes and switches hold. */
    public static void linkedMark(GameTestHelper h) {
        configured(() -> {
            var pos = new BlockPos(1, 2, 1);
            h.setBlock(pos, Blocks.BARREL);
            var abs = h.absolutePos(pos);
            var p = player(h, new BlockPos(1, 2, 3));
            p.setShiftKeyDown(true);
            h.assertTrue(LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs) == InteractionResult.SUCCESS
                    && LinkedContainers.mark(p) != null && LinkedContainers.mark(p).pos().equals(abs), "Sneak + empty hand marks the barrel");
            h.assertTrue(LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs) == InteractionResult.SUCCESS
                    && LinkedContainers.mark(p) == null, "Sneak + right-click again unmarks");
            p.setShiftKeyDown(false);
            h.assertTrue(LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs) == InteractionResult.PASS && LinkedContainers.mark(p) == null, "Without sneaking the barrel opens normally");
            p.setShiftKeyDown(true);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            h.assertTrue(LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs) == InteractionResult.PASS, "Only with an empty main hand");
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            h.assertTrue(LinkedContainers.onRightClickBlock(p, InteractionHand.OFF_HAND, abs) == InteractionResult.PASS, "Offhand event does nothing");
            h.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
            h.assertTrue(LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, h.absolutePos(new BlockPos(2, 2, 1))) == InteractionResult.PASS, "No container, no mark");
            InteractionGuard.permission = (a, b) -> false;
            h.assertTrue(LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs) == InteractionResult.PASS && LinkedContainers.mark(p) == null, "Claim veto refuses the mark");
            InteractionGuard.permission = (a, b) -> true;
            LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs);
            h.assertTrue(LinkedContainers.mark(p) != null, "Marked again for the revoke check");
            InteractionGuard.permission = (a, b) -> false;
            p.tickCount = 40;
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) == null, "A right revoked after marking ends the mark within a second");
            InteractionGuard.permission = (a, b) -> true;
            SimpleInterfaces.config().enableLinkedContainers = false;
            h.assertTrue(LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs) == InteractionResult.PASS && LinkedContainers.mark(p) == null, "Switch off refuses the mark");
            SimpleInterfaces.config().enableLinkedContainers = true;
            // A double chest: marking one half and clicking the other half unmarks the same container.
            var left = new BlockPos(3, 2, 1);
            var right = left.relative(Direction.NORTH.getClockWise());
            h.setBlock(left, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
            h.setBlock(right, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
            var container = LinkedContainers.container(h.getLevel(), h.absolutePos(left));
            h.assertTrue(container != null && container.getContainerSize() == 54, "Double chest marks the whole 54 slots: " + (container == null ? -1 : container.getContainerSize()));
            p.setPos(Vec3.atBottomCenterOf(h.absolutePos(left.south(2))));
            LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, h.absolutePos(left));
            h.assertTrue(LinkedContainers.mark(p) != null, "Double chest marked");
            LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, h.absolutePos(right));
            h.assertTrue(LinkedContainers.mark(p) == null, "Other half of the same double chest unmarks");
            LinkedContainers.clear(p);
        });
        h.succeed();
    }

    /** Range (with its config bounds), a broken block and the tick check end the mark. */
    public static void linkedRange(GameTestHelper h) {
        configured(() -> {
            var c = new LinkedConfig();
            c.linkedContainerRange = 999;
            c.normalize();
            h.assertTrue(c.linkedContainerRange == LinkedConfig.MAX_LINKED_RANGE, "Range hard cap 128");
            c.linkedContainerRange = 0;
            c.normalize();
            h.assertTrue(c.linkedContainerRange == LinkedConfig.MIN_LINKED_RANGE, "Range lower bound 8");
            h.assertTrue(new LinkedConfig().linkedContainerRange == 64, "Default range 64");
            var pos = new BlockPos(1, 2, 1);
            h.setBlock(pos, Blocks.CHEST);
            var abs = h.absolutePos(pos);
            var p = player(h, new BlockPos(1, 2, 3));
            p.setShiftKeyDown(true);
            LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs);
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) != null, "Mark survives a tick in range");
            SimpleInterfaces.config().linkedContainerRange = 8;
            p.setPos(Vec3.atCenterOf(abs).add(7, 0, 0));
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) != null, "Inside the configured range");
            p.setPos(Vec3.atCenterOf(abs).add(9, 0, 0));
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) == null, "Beyond the configured range the mark ends");
            p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 2, 3))));
            LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs);
            h.setBlock(pos, Blocks.AIR);
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) == null, "Broken container ends the mark");
            LinkedContainers.clear(p);
        });
        h.succeed();
    }

    /** The second menu gets the marked slots; shift-click routes without creating or losing items; a broken box hands out nothing. */
    public static void linkedTransfer(GameTestHelper h) {
        configured(() -> {
            var markedPos = new BlockPos(1, 2, 1);
            var otherPos = new BlockPos(3, 2, 1);
            h.setBlock(markedPos, Blocks.SHULKER_BOX);
            h.setBlock(otherPos, Blocks.CHEST);
            var marked = (Container) h.getLevel().getBlockEntity(h.absolutePos(markedPos));
            var other = (Container) h.getLevel().getBlockEntity(h.absolutePos(otherPos));
            marked.setItem(0, new ItemStack(Items.DIAMOND, 10));
            marked.setItem(2, new ItemStack(Items.EMERALD, 7));
            var p = player(h, new BlockPos(2, 2, 3));
            p.setShiftKeyDown(true);
            LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, h.absolutePos(markedPos));
            p.setShiftKeyDown(false);
            p.openMenu((MenuProvider) other);
            var menu = p.containerMenu;
            int start = firstLinked(menu);
            h.assertTrue(start == 27 + 36 && menu.slots.size() == 27 + 36 + 27, "Marked slots appended behind the chest menu: " + menu.slots.size());
            // Marked -> second container first.
            menu.clicked(start, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(other, Items.DIAMOND) == 10 && count(marked, Items.DIAMOND) == 0, "Shift-click moves marked -> second chest");
            // Second container -> the marked box first (it is shown where the inventory is), then the inventory.
            int diamondSlot = -1;
            for (int i = 0; i < 27; i++) if (menu.slots.get(i).getItem().is(Items.DIAMOND)) diamondSlot = i;
            menu.clicked(diamondSlot, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(marked, Items.DIAMOND) == 10 && count(p.getInventory(), Items.DIAMOND) == 0 && count(other, Items.DIAMOND) == 0, "Second chest -> marked box first");
            // Inventory -> second chest; when it is full, into the marked box.
            p.getInventory().add(new ItemStack(Items.DIAMOND, 10));
            for (int i = 0; i < 27; i++) other.setItem(i, new ItemStack(Items.STONE, 64));
            int inv = -1;
            for (int i = 27; i < 63; i++) if (menu.slots.get(i).getItem().is(Items.DIAMOND)) inv = i;
            menu.clicked(inv, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(marked, Items.DIAMOND) == 20 && count(p.getInventory(), Items.DIAMOND) == 0, "Full second chest: inventory -> marked box");
            h.assertTrue(count(other, Items.STONE) == 27 * 64, "Nothing lost in the full chest");
            // Break the marked box: its block entity keeps the items for the dropped box, the panel hands out nothing.
            h.setBlock(markedPos, Blocks.AIR);
            menu.clicked(start + 2, 0, ContainerInput.PICKUP, p);
            h.assertTrue(menu.getCarried().isEmpty(), "Removed box: no pickup from its slot");
            menu.clicked(start + 2, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(p.getInventory(), Items.EMERALD) == 0, "Removed box: no shift-click out of it");
            LinkedContainers.tick(p);
            h.assertTrue(p.containerMenu == p.inventoryMenu && LinkedContainers.mark(p) == null, "Broken link closes the menu and ends the mark");
            // Opening the marked container itself never doubles it.
            h.setBlock(markedPos, Blocks.BARREL);
            p.setShiftKeyDown(true);
            LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, h.absolutePos(markedPos));
            p.openMenu((MenuProvider) h.getLevel().getBlockEntity(h.absolutePos(markedPos)));
            h.assertTrue(firstLinked(p.containerMenu) == -1, "The marked container itself is not shown twice");
            p.closeContainer();
            // A client without the payload channel never gets extra slots.
            LinkedContainers.openSync = (a, b) -> false;
            p.openMenu((MenuProvider) other);
            h.assertTrue(firstLinked(p.containerMenu) == -1, "No channel, no extra slots");
            p.closeContainer();
            LinkedContainers.clear(p);
        });
        h.succeed();
    }

    /** The inventory menu carries the marked slots while the mark lasts (HUD payload with the block's item), and loses them again. */
    public static void linkedInventory(GameTestHelper h) {
        configured(() -> {
            var boxPos = new BlockPos(1, 2, 1);
            h.setBlock(boxPos, Blocks.SHULKER_BOX);
            var box = (Container) h.getLevel().getBlockEntity(h.absolutePos(boxPos));
            box.setItem(0, new ItemStack(Items.DIAMOND, 5));
            var sent = new ArrayList<com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload>();
            LinkedContainers.openSync = (player, payload) -> sent.add(payload);
            var p = player(h, new BlockPos(2, 2, 3));
            var menu = p.inventoryMenu;
            int base = menu.slots.size();
            p.setShiftKeyDown(true);
            LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, h.absolutePos(boxPos));
            p.setShiftKeyDown(false);
            h.assertTrue(menu.slots.size() == base + 27 && firstLinked(menu) == base, "Mark appends its slots to the inventory menu: " + menu.slots.size());
            h.assertTrue(!sent.isEmpty() && sent.get(sent.size() - 1).containerId() == menu.containerId
                    && sent.get(sent.size() - 1).size() == 27 && sent.get(sent.size() - 1).entries().get(0).icon().is(Items.SHULKER_BOX), "Payload carries size and the block's item");
            // Inventory screen: main/hotbar slots fill the marked box, the box's slots empty into the inventory.
            p.getInventory().add(new ItemStack(Items.DIAMOND, 3));
            int diamonds = -1;
            for (int i = 9; i < 45; i++) if (menu.slots.get(i).getItem().is(Items.DIAMOND)) diamonds = i;
            menu.clicked(diamonds, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(box, Items.DIAMOND) == 8 && count(p.getInventory(), Items.DIAMOND) == 0, "Inventory shift-click fills the marked box");
            menu.clicked(base, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(box, Items.DIAMOND) == 0 && count(p.getInventory(), Items.DIAMOND) == 8, "Marked slot shift-click -> inventory");
            // Unmark: slots gone, payload with size 0.
            p.setShiftKeyDown(true);
            LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, h.absolutePos(boxPos));
            p.setShiftKeyDown(false);
            h.assertTrue(menu.slots.size() == base && sent.get(sent.size() - 1).size() == 0, "Unmark removes the slots again");
            LinkedContainers.clear(p);
        });
        h.succeed();
    }

    private static void mark(ServerPlayer p, BlockPos abs) {
        p.setShiftKeyDown(true);
        LinkedContainers.onRightClickBlock(p, InteractionHand.MAIN_HAND, abs);
        p.setShiftKeyDown(false);
    }

    /** The HUD banner payloads (container id -1) among {@code sent}, in order. */
    private static List<com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload> hud(List<com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload> sent) {
        return sent.stream().filter(x -> x.containerId() == com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload.HUD).toList();
    }

    /** HUD banner payload, Escape cancel, and the automatic end after the marked container was opened and left again. */
    public static void linkedEnd(GameTestHelper h) {
        configured(() -> {
            var markedPos = new BlockPos(1, 2, 1);
            var otherPos = new BlockPos(3, 2, 1);
            h.setBlock(markedPos, Blocks.BARREL);
            h.setBlock(otherPos, Blocks.CHEST);
            var markedAbs = h.absolutePos(markedPos);
            var marked = h.getLevel().getBlockEntity(markedAbs);
            var other = h.getLevel().getBlockEntity(h.absolutePos(otherPos));
            var sent = new ArrayList<com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload>();
            LinkedContainers.openSync = (player, payload) -> sent.add(payload);
            var p = player(h, new BlockPos(2, 2, 3));
            // Banner: one HUD payload on marking (kind = block item, name = container name), one empty one on ending.
            mark(p, markedAbs);
            var shown = hud(sent);
            h.assertTrue(shown.size() == 1 && shown.get(0).size() == 1 && shown.get(0).entries().get(0).icon().is(Items.BARREL)
                    && shown.get(0).entries().get(0).title().getString().equals(((net.minecraft.world.level.block.entity.BaseContainerBlockEntity) marked).getDisplayName().getString()), "Marking sends the banner: " + shown);
            LinkedContainers.tick(p);
            h.assertTrue(hud(sent).size() == 1, "The banner is sent once, not every tick");
            // Escape (cancel payload): mark and banner gone; a second cancel is harmless.
            LinkedContainers.onCancel(p);
            h.assertTrue(LinkedContainers.mark(p) == null && hud(sent).size() == 2 && hud(sent).get(1).size() == 0, "Cancel ends the mark and hides the banner");
            LinkedContainers.onCancel(p);
            h.assertTrue(hud(sent).size() == 2, "Cancel without a mark sends nothing");
            // Not opening it: another container (with the linked panel) opened and closed keeps the mark.
            mark(p, markedAbs);
            p.openMenu((MenuProvider) other);
            LinkedContainers.tick(p);
            p.closeContainer();
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) != null, "Visiting only a second container (linked panel) keeps the mark");
            // Open the marked container: the mark holds while it is open, ends once it is closed.
            p.openMenu((MenuProvider) marked);
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) != null, "The mark lasts while its container is open");
            p.closeContainer();
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) == null && hud(sent).get(hud(sent).size() - 1).size() == 0, "Open + close of the marked container ends the mark and the banner");
            // Marking again starts fresh: no visit remembered from before.
            mark(p, markedAbs);
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) != null, "A new mark is not ended by the old visit");
            // Switching straight to another menu also leaves the marked container.
            p.openMenu((MenuProvider) marked);
            LinkedContainers.tick(p);
            p.openMenu((MenuProvider) other);
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.mark(p) == null, "Opening something else counts as leaving");
            p.closeContainer();
            // Double chest: one half marked, the other half opened is the same container.
            h.setBlock(new BlockPos(1, 2, 2), Blocks.CHEST);
            h.setBlock(new BlockPos(2, 2, 2), Blocks.CHEST);
            LinkedContainers.clear(p);
            var halfA = h.absolutePos(new BlockPos(1, 2, 2));
            var halfB = h.absolutePos(new BlockPos(2, 2, 2));
            var combined = LinkedContainers.container(h.getLevel(), halfA);
            if (combined != null && combined.getContainerSize() == 54) {
                mark(p, halfA);
                p.openMenu((MenuProvider) h.getLevel().getBlockEntity(halfB));
                LinkedContainers.tick(p);
                p.closeContainer();
                LinkedContainers.tick(p);
                h.assertTrue(LinkedContainers.mark(p) == null, "Opening the other half of a marked double chest counts as visiting it");
            }
            LinkedContainers.clear(p);
        });
        h.succeed();
    }

    /**
     * Owner bug: in creative the mark must never take part in the inventory / creative tabs. No slots on the inventory
     * menu, shift-clicks never fill the marked container, nothing moves for a menu that is not the open one.
     */
    public static void linkedCreative(GameTestHelper h) {
        configured(() -> {
            var boxPos = new BlockPos(1, 2, 1);
            var otherPos = new BlockPos(3, 2, 1);
            h.setBlock(boxPos, Blocks.SHULKER_BOX);
            h.setBlock(otherPos, Blocks.CHEST);
            var box = (Container) h.getLevel().getBlockEntity(h.absolutePos(boxPos));
            var other = h.getLevel().getBlockEntity(h.absolutePos(otherPos));
            var sent = new ArrayList<com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload>();
            LinkedContainers.openSync = (player, payload) -> sent.add(payload);
            var p = player(h, new BlockPos(2, 2, 3));
            var menu = p.inventoryMenu;
            int base = menu.slots.size();
            mark(p, h.absolutePos(boxPos));
            h.assertTrue(menu.slots.size() == base + 27, "Survival: the inventory menu carries the mark");
            // Race: switched to creative, the server has not ticked yet - clicks still must not move anything.
            p.setGameMode(GameType.CREATIVE);
            p.getInventory().add(new ItemStack(Items.DIAMOND, 4));
            box.setItem(0, new ItemStack(Items.EMERALD, 3));
            int diamonds = -1;
            for (int i = 9; i < 45; i++) if (menu.slots.get(i).getItem().is(Items.DIAMOND)) diamonds = i;
            menu.clicked(diamonds, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(box, Items.DIAMOND) == 0 && count(p.getInventory(), Items.DIAMOND) == 4, "Creative: shift-click in the inventory does not fill the marked container");
            menu.clicked(base, 0, ContainerInput.QUICK_MOVE, p);
            menu.clicked(base, 0, ContainerInput.PICKUP, p);
            h.assertTrue(count(box, Items.EMERALD) == 3 && count(p.getInventory(), Items.EMERALD) == 0 && menu.getCarried().isEmpty(), "Creative: the marked slots cannot be taken from");
            // After the next tick the slots are gone from the creative inventory, the mark itself stays (chests, banner).
            LinkedContainers.tick(p);
            h.assertTrue(menu.slots.size() == base && firstLinked(menu) == -1 && LinkedContainers.mark(p) != null, "Creative: slots removed from the inventory menu, mark kept");
            h.assertTrue(sent.stream().anyMatch(x -> x.containerId() == menu.containerId && x.size() == 0), "Creative: the client is told to drop the slots");
            diamonds = -1;
            for (int i = 9; i < 45; i++) if (menu.slots.get(i).getItem().is(Items.DIAMOND)) diamonds = i;
            menu.clicked(diamonds, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(box, Items.DIAMOND) == 0, "Creative: shift-click stays vanilla, nothing reaches the marked container");
            LinkedContainers.tick(p);
            h.assertTrue(firstLinked(menu) == -1, "Creative: slots do not come back");
            // A real container GUI in creative still shows the marked container (not the creative inventory).
            p.openMenu((MenuProvider) other);
            h.assertTrue(firstLinked(p.containerMenu) != -1, "Creative: a chest GUI still gets the marked panel");
            p.closeContainer();
            // Back to survival: the inventory shows it again and works.
            p.setGameMode(GameType.SURVIVAL);
            LinkedContainers.tick(p);
            h.assertTrue(menu.slots.size() == base + 27, "Survival again: slots return");
            diamonds = -1;
            for (int i = 9; i < 45; i++) if (menu.slots.get(i).getItem().is(Items.DIAMOND)) diamonds = i;
            menu.clicked(diamonds, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(box, Items.DIAMOND) == 4, "Survival again: shift-click fills the marked container");
            // Server rule: a click on the inventory menu's marked slots while another menu is open moves nothing.
            p.openMenu((MenuProvider) other);
            int emerald = base;
            menu.clicked(emerald, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(box, Items.EMERALD) == 3 && count(p.getInventory(), Items.EMERALD) == 0, "No transfer for a menu that is not the open one");
            p.closeContainer();
            LinkedContainers.clear(p);
        });
        h.succeed();
    }

    /** Shulker box from the inventory: contents written back, slot locked, no box in a box, switch honored. */
    public static void portableShulker(GameTestHelper h) {
        configured(() -> {
            var p = player(h, new BlockPos(1, 2, 1));
            var box = new ItemStack(Items.SHULKER_BOX);
            box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 5))));
            p.getInventory().setItem(3, box);
            p.getInventory().setItem(5, new ItemStack(Items.SHULKER_BOX));
            // Right-click on the box's own slot in the inventory screen.
            h.assertTrue(PortableContainers.interceptClick(p.inventoryMenu, 36 + 3, 1, ContainerInput.PICKUP, p), "Right-click on the box slot is taken");
            PortableContainers.tick(p);
            h.assertTrue(p.containerMenu instanceof PortableMenu && p.containerMenu.getType() == MenuType.SHULKER_BOX, "Opens with the Vanilla shulker screen type");
            var menu = p.containerMenu;
            h.assertTrue(menu.slots.get(0).getItem().is(Items.DIAMOND) && menu.slots.get(0).getItem().getCount() == 5, "Contents shown");
            menu.clicked(0, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(contents(box, Items.DIAMOND) == 0 && count(p.getInventory(), Items.DIAMOND) == 5, "Taking out writes back at once");
            int diamonds = -1;
            for (int i = 27; i < menu.slots.size(); i++) if (menu.slots.get(i).getItem().is(Items.DIAMOND)) diamonds = i;
            menu.clicked(diamonds, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(contents(box, Items.DIAMOND) == 5 && count(p.getInventory(), Items.DIAMOND) == 0, "Putting back writes back; total stays 5");
            int locked = 27 + 27 + 3;
            menu.clicked(locked, 0, ContainerInput.PICKUP, p);
            h.assertTrue(menu.getCarried().isEmpty() && p.getInventory().getItem(3) == box, "Locked slot: no pickup");
            menu.clicked(locked, 0, ContainerInput.THROW, p);
            h.assertTrue(p.getInventory().getItem(3) == box, "Locked slot: no throw");
            menu.clicked(locked, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(p.getInventory().getItem(3) == box, "Locked slot: no shift-click");
            menu.clicked(1, 3, ContainerInput.SWAP, p);
            h.assertTrue(p.getInventory().getItem(3) == box && menu.slots.get(1).getItem().isEmpty(), "Locked slot: no number-key swap");
            menu.clicked(27 + 27 + 5, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(p.getInventory().getItem(5).is(Items.SHULKER_BOX) && contents(box, Items.SHULKER_BOX) == 0, "No shulker box inside a shulker box");
            h.assertTrue(!menu.slots.get(1).mayPlace(new ItemStack(Items.SHULKER_BOX)), "Content slots refuse boxes");
            p.getInventory().setItem(3, ItemStack.EMPTY);
            h.assertTrue(!menu.stillValid(p) && !menu.slots.get(0).mayPickup(p), "Box gone from its slot: menu invalid, nothing to take");
            p.closeContainer();
            p.getInventory().setItem(3, box);
            // Right-click in the air.
            p.getInventory().setSelectedSlot(3);
            h.assertTrue(PortableContainers.onUse(h.getLevel(), p, InteractionHand.MAIN_HAND), "Use in the air is taken");
            PortableContainers.tick(p);
            h.assertTrue(p.containerMenu instanceof PortableMenu, "Use in the air opens the box");
            p.closeContainer();
            h.assertTrue(contents(box, Items.DIAMOND) == 5, "Contents kept after closing");
            SimpleInterfaces.config().enableEasyShulkers = false;
            h.assertTrue(!PortableContainers.open(p, 3) && !PortableContainers.interceptClick(p.inventoryMenu, 36 + 3, 1, ContainerInput.PICKUP, p), "Switch off: boxes stay closed");
            SimpleInterfaces.config().enableEasyShulkers = true;
            // A Simple Storage tier box (public registry ID; SimpleBuilding's before split stage 8b): 45 slots, its own components kept.
            var tier = BuiltInRegistries.ITEM.getValue(Identifier.parse("simplestorage:netherite_shulker_box"));
            if (tier != Items.AIR) {
                var tierBox = new ItemStack(tier);
                p.getInventory().setItem(6, tierBox);
                h.assertTrue(PortableContainers.open(p, 6) && p.containerMenu.getType() == MenuType.GENERIC_9x5, "Tier box opens with 45 slots: " + p.containerMenu.getType());
                p.containerMenu.slots.get(44).set(new ItemStack(Items.GOLD_INGOT, 3));
                p.closeContainer();
                var opened = PortableContainers.open(p, 6);
                h.assertTrue(opened && p.containerMenu.slots.get(44).getItem().is(Items.GOLD_INGOT) && p.containerMenu.slots.get(44).getItem().getCount() == 3, "Tier box keeps its contents");
                p.closeContainer();
            }
        });
        h.succeed();
    }

    /** Ender chest from the inventory opens the player's own ender inventory. */
    public static void portableEnderChest(GameTestHelper h) {
        configured(() -> {
            var p = player(h, new BlockPos(1, 2, 1));
            p.getEnderChestInventory().setItem(0, new ItemStack(Items.EMERALD, 4));
            var chest = new ItemStack(Items.ENDER_CHEST, 2);
            p.getInventory().setItem(4, chest);
            h.assertTrue(PortableContainers.open(p, 4) && p.containerMenu.getType() == MenuType.GENERIC_9x3, "Ender chest opens as 9x3");
            var menu = p.containerMenu;
            h.assertTrue(menu.slots.get(0).getItem().is(Items.EMERALD), "Shows the player's ender inventory");
            menu.clicked(0, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(p.getInventory(), Items.EMERALD) == 4 && p.getEnderChestInventory().getItem(0).isEmpty(), "Moves out of the ender inventory");
            menu.clicked(27 + 27 + 4, 0, ContainerInput.PICKUP, p);
            h.assertTrue(menu.getCarried().isEmpty() && p.getInventory().getItem(4) == chest, "Ender chest item locked too");
            var snapshots = new ArrayList<com.simplebuilding.modules.simpleinterfaces.network.EnderChestSnapshotPayload>();
            var oldSender = com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips.sender;
            com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips.sender = (to, payload) -> to == p && snapshots.add(payload);
            try {
                p.closeContainer();
            } finally {
                com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips.sender = oldSender;
            }
            h.assertTrue(snapshots.size() == 1, "Closing the Easy Ender Chest refreshes the tooltip snapshot (INT-P2-1): " + snapshots.size());
            SimpleInterfaces.config().enableEasyEnderChests = false;
            h.assertTrue(!PortableContainers.open(p, 4), "Switch off: ender chest stays closed");
            p.getEnderChestInventory().clearContent();
        });
        h.succeed();
    }

    // --- N65: several marks ---

    private static BlockPos doubleChest(GameTestHelper h, BlockPos left) {
        var right = left.relative(Direction.NORTH.getClockWise());
        h.setBlock(left, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        h.setBlock(right, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        return h.absolutePos(left);
    }

    /** N65: marks add up to linkedContainerMax (oldest drops), sneak + right-click removes one, Escape all; slot sum <= MAX_SLOTS. */
    public static void multiMark(GameTestHelper h) {
        configured(() -> {
            var c = new LinkedConfig();
            h.assertTrue(c.linkedContainerMax == 3, "Default: 3 marks");
            c.linkedContainerMax = 0;
            c.normalize();
            h.assertTrue(c.linkedContainerMax == LinkedConfig.MIN_LINKED_MAX, "Lower bound 1");
            c.linkedContainerMax = 9;
            c.normalize();
            h.assertTrue(c.linkedContainerMax == LinkedConfig.MAX_LINKED_MAX, "Upper bound 5");
            var sent = new ArrayList<com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload>();
            LinkedContainers.openSync = (player, payload) -> sent.add(payload);
            BlockPos[] at = new BlockPos[4];
            for (int i = 0; i < 4; i++) {
                h.setBlock(new BlockPos(1 + i, 2, 1), Blocks.BARREL);
                at[i] = h.absolutePos(new BlockPos(1 + i, 2, 1));
            }
            var p = player(h, new BlockPos(2, 2, 3));
            mark(p, at[0]);
            mark(p, at[1]);
            h.assertTrue(LinkedContainers.marks(p).size() == 2 && LinkedContainers.marks(p).get(0).pos().equals(at[0])
                    && LinkedContainers.mark(p).pos().equals(at[1]), "Two marks, oldest first: " + LinkedContainers.marks(p));
            var tiles = hud(sent);
            h.assertTrue(tiles.get(tiles.size() - 1).entries().size() == 2, "HUD payload: one tile per mark");
            mark(p, at[2]);
            mark(p, at[3]);
            var marks = LinkedContainers.marks(p);
            h.assertTrue(marks.size() == 3 && marks.get(0).pos().equals(at[1]) && marks.get(2).pos().equals(at[3]), "Fourth mark drops the oldest: " + marks);
            mark(p, at[2]);
            marks = LinkedContainers.marks(p);
            h.assertTrue(marks.size() == 2 && marks.get(0).pos().equals(at[1]) && marks.get(1).pos().equals(at[3]), "Sneak + right-click on a marked one removes only it: " + marks);
            int base = p.inventoryMenu.slots.size() - 54;
            h.assertTrue(firstLinked(p.inventoryMenu) == base, "Inventory menu carries both marked containers: " + p.inventoryMenu.slots.size());
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.marks(p).size() == 2, "Marks survive a tick in range");
            LinkedContainers.onCancel(p);
            h.assertTrue(LinkedContainers.marks(p).isEmpty() && hud(sent).get(hud(sent).size() - 1).entries().isEmpty() && firstLinked(p.inventoryMenu) == -1,
                    "Escape ends every mark, tiles and inventory slots go");
            // One far mark ends alone.
            mark(p, at[0]);
            mark(p, at[3]);
            SimpleInterfaces.config().linkedContainerRange = 8;
            p.setPos(Vec3.atCenterOf(at[0]).add(-7.5, 0, 0));
            LinkedContainers.tick(p);
            h.assertTrue(LinkedContainers.marks(p).size() == 1 && LinkedContainers.mark(p).pos().equals(at[0]), "Out of range ends only that mark: " + LinkedContainers.marks(p));
            SimpleInterfaces.config().linkedContainerRange = 64;
            p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 2, 3))));
            LinkedContainers.clear(p);
            // Server config 1: a new mark replaces the old one (the former behaviour).
            SimpleInterfaces.config().linkedContainerMax = 1;
            mark(p, at[0]);
            mark(p, at[1]);
            h.assertTrue(LinkedContainers.marks(p).size() == 1 && LinkedContainers.mark(p).pos().equals(at[1]), "Max 1: the new mark replaces the old");
            SimpleInterfaces.config().linkedContainerMax = 3;
            LinkedContainers.clear(p);
            // Slot sum: two double chests (108) fill MAX_SLOTS; a barrel more drops the oldest double chest.
            var dc1 = doubleChest(h, new BlockPos(1, 2, 5));
            var dc2 = doubleChest(h, new BlockPos(4, 2, 5));
            mark(p, dc1);
            mark(p, dc2);
            h.assertTrue(LinkedContainers.marks(p).size() == 2, "Two double chests = 108 slots fit");
            mark(p, at[0]);
            marks = LinkedContainers.marks(p);
            h.assertTrue(marks.size() == 2 && marks.get(0).pos().equals(dc2) && marks.get(1).pos().equals(at[0]), "Beyond MAX_SLOTS the oldest drops: " + marks);
            int linked = p.inventoryMenu.slots.size() - firstLinked(p.inventoryMenu);
            h.assertTrue(linked == 54 + 27 && linked <= LinkedContainers.MAX_SLOTS, "Appended slots stay within MAX_SLOTS: " + linked);
            LinkedContainers.clear(p);
        });
        h.succeed();
    }

    private static int total(Item item, Container... containers) {
        int n = 0;
        for (Container c : containers) n += count(c, item);
        return n;
    }

    /** N65: a second menu gets all marked containers in order; shift-clicks fill them in order and never create or lose items. */
    public static void multiTransfer(GameTestHelper h) {
        configured(() -> {
            h.setBlock(new BlockPos(1, 2, 1), Blocks.BARREL);
            h.setBlock(new BlockPos(2, 2, 1), Blocks.SHULKER_BOX);
            h.setBlock(new BlockPos(3, 2, 1), Blocks.CHEST);
            var a = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1, 2, 1)));
            var b = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 1)));
            var other = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3, 2, 1)));
            var sent = new ArrayList<com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload>();
            LinkedContainers.openSync = (player, payload) -> sent.add(payload);
            var p = player(h, new BlockPos(2, 2, 3));
            mark(p, h.absolutePos(new BlockPos(1, 2, 1)));
            mark(p, h.absolutePos(new BlockPos(2, 2, 1)));
            p.openMenu((MenuProvider) other);
            var menu = p.containerMenu;
            int start = firstLinked(menu);
            h.assertTrue(start == 63 && menu.slots.size() == 63 + 54, "Both marked containers appended behind the chest menu: " + menu.slots.size());
            var open = sent.get(sent.size() - 1);
            h.assertTrue(open.containerId() == menu.containerId && open.entries().size() == 2 && open.entries().get(0).icon().is(Items.BARREL)
                    && open.entries().get(1).icon().is(Items.SHULKER_BOX), "One payload, an entry per container in marking order");
            for (int i = 0; i < 26; i++) a.setItem(i, new ItemStack(Items.STONE, 64));
            a.setItem(26, new ItemStack(Items.DIAMOND, 60));
            other.setItem(0, new ItemStack(Items.DIAMOND, 64));
            int diamonds = total(Items.DIAMOND, a, b, other, p.getInventory());
            // Second chest -> marked: fills the first marked container (4 into its diamond stack), the rest into the second.
            menu.clicked(0, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(a, Items.DIAMOND) == 64 && count(b, Items.DIAMOND) == 60 && count(other, Items.DIAMOND) == 0,
                    "Chest -> marked containers in order: a=" + count(a, Items.DIAMOND) + " b=" + count(b, Items.DIAMOND));
            h.assertTrue(total(Items.DIAMOND, a, b, other, p.getInventory()) == diamonds, "No diamond created or lost");
            // Second marked container -> the open chest.
            menu.clicked(start + 27, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(b, Items.DIAMOND) == 0 && count(other, Items.DIAMOND) == 60, "Marked -> second GUI");
            // Inventory -> chest full -> marked containers (the first is full: the second takes it).
            for (int i = 0; i < 27; i++) other.setItem(i, new ItemStack(Items.STONE, 64));
            p.getInventory().add(new ItemStack(Items.EMERALD, 30));
            int inv = -1;
            for (int i = 27; i < 63; i++) if (menu.slots.get(i).getItem().is(Items.EMERALD)) inv = i;
            menu.clicked(inv, 0, ContainerInput.QUICK_MOVE, p);
            h.assertTrue(count(b, Items.EMERALD) == 30 && count(p.getInventory(), Items.EMERALD) == 0 && count(a, Items.EMERALD) == 0,
                    "Full chest: inventory -> the next marked container with room");
            h.assertTrue(total(Items.STONE, a, b, other) == 53 * 64, "No stone moved or lost");
            p.closeContainer();
            // Opening a marked container itself: only the other one is appended.
            p.openMenu((MenuProvider) a);
            h.assertTrue(firstLinked(p.containerMenu) == 27 + 36 && p.containerMenu.slots.size() == 63 + 27, "The open marked container is not shown twice: " + p.containerMenu.slots.size());
            p.closeContainer();
            LinkedContainers.clear(p);
        });
        h.succeed();
    }

    // --- N65: LinkedLayout (pure) ---

    /** Frames of KONZEPT-N65 §2 (GUI px): 1080p scale 2/3/4, 720p scale 2/3. */
    static final int[][] FRAMES = {{960, 540}, {640, 360}, {480, 270}, {640, 360}, {426, 240}};
    static final String[] FRAME_NAMES = {"1080p*2", "1080p*3", "1080p*4", "720p*2", "720p*3"};

    /** A screen of the test table: image size, first inventory row, own storage rows, inventory screen, clean. */
    record Screen(String name, int width, int height, int inventoryTop, int ownRows, boolean inventory, boolean clean) {
        com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Input input(int[] frame, List<Integer> marked, int bookShift) {
            return new com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Input(frame[0], frame[1], width, height,
                    (frame[0] - width) / 2 + bookShift, (frame[1] - height) / 2, inventoryTop, ownRows, inventory, clean, marked, 0);
        }
    }

    static final Screen E = new Screen("E", 176, 166, 84, 0, true, true);
    static final Screen FURNACE = new Screen("furnace", 176, 166, 84, 0, false, true);
    static final Screen CRAFTING = new Screen("crafting", 176, 166, 84, 0, false, true);
    static final Screen ANVIL = new Screen("anvil", 176, 166, 84, 0, false, false);
    static final Screen BEACON = new Screen("beacon", 230, 219, 137, 0, false, false);
    /** N62-styled chest / double chest (player slots 6 px lower), and Vanilla's. */
    static final Screen CHEST = new Screen("chest", 176, 172, 91, 3, false, true);
    static final Screen DOUBLE = new Screen("double chest", 176, 228, 145, 6, false, true);
    static final Screen DOUBLE_VANILLA = new Screen("vanilla double chest", 176, 222, 139, 6, false, false);

    /** Marked chest/shulker 27, double chest 54. */
    record Case(Screen screen, List<Integer> marked, String expected) {}

    static final List<Case> CASES = List.of(
            new Case(E, List.of(27), "BELOW BELOW BELOW BELOW BESIDE"),
            new Case(FURNACE, List.of(27), "BELOW BELOW BELOW BELOW BELOW"),
            new Case(CRAFTING, List.of(27), "BELOW BELOW BELOW BELOW BELOW"),
            new Case(CHEST, List.of(54), "BELOW BELOW BELOW BELOW BESIDE"),
            new Case(ANVIL, List.of(27), "BELOW BELOW BELOW BELOW BELOW"),
            new Case(BEACON, List.of(27), "BELOW BELOW BELOW_SCROLL BELOW BELOW_SCROLL"),
            new Case(E, List.of(54), "BELOW BELOW BESIDE BELOW BESIDE"),
            new Case(DOUBLE, List.of(54), "BESIDE BESIDE BESIDE BESIDE BESIDE"),
            new Case(DOUBLE_VANILLA, List.of(54), "TABS TABS TABS TABS TABS"),
            new Case(DOUBLE, List.of(27, 27), "TABS TABS TABS TABS TABS"),
            new Case(FURNACE, List.of(27, 54), "BELOW BELOW BELOW BELOW BESIDE"),
            new Case(FURNACE, List.of(5), "BELOW BELOW BELOW BELOW BELOW"),
            new Case(CHEST, List.of(27, 27, 27), "BELOW BELOW BELOW BELOW BELOW"));

    /** Every layout of {@code in} over all its tabs: inside the window, same shift on every tab. Returns the first tab's. */
    private static com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Layout checkTabs(GameTestHelper h,
            com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Input in, String what) {
        var first = com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.compute(in);
        for (int t = 0; t < Math.max(1, first.tabs().size()); t++) {
            var l = com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.compute(in.withTab(t));
            int[] b = com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.bounds(l, in);
            h.assertTrue(com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.fits(l, in),
                    what + " tab " + t + ": inside the window " + java.util.Arrays.toString(b) + " " + l);
            h.assertTrue(l.mode() == first.mode() && l.dx() == first.dx() && l.dy() == first.dy(), what + " tab " + t + ": same mode and shift");
            h.assertTrue(l.tab() == t || first.tabs().isEmpty(), what + " tab " + t + " is the open one");
        }
        return first;
    }

    /** KONZEPT-N65 §4/§5 x §2: the expected mode for every combination and frame; everything inside the window on every tab. */
    public static void layoutModes(GameTestHelper h) {
        for (Case c : CASES) {
            String[] expected = c.expected().split(" ");
            for (int f = 0; f < FRAMES.length; f++) {
                String what = c.screen().name() + " + " + c.marked() + " @ " + FRAME_NAMES[f];
                var in = c.screen().input(FRAMES[f], c.marked(), 0);
                var l = checkTabs(h, in, what);
                h.assertTrue(l.mode().name().equals(expected[f]), what + ": mode " + l.mode() + ", expected " + expected[f]);
                if (l.mode() == com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Mode.BESIDE) {
                    h.assertTrue(c.screen().clean() && l.dy() == 0 && l.box().right() + com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.GAP == 0,
                            what + ": C only for clean screens, box left of the image");
                }
                if (!c.screen().clean()) h.assertTrue(l.dx() == 0 && l.dy() == 0, what + ": a screen that is not clean never moves");
                if (l.hotbar() != null) h.assertTrue(l.box().y() == c.screen().inventoryTop() - 8 && l.bgBottom() == l.box().y()
                        && l.hotbar().y() == l.box().bottom() + 2, what + ": A = station, box, 2 px, hotbar strip; background cut at the box");
            }
        }
        // Two double chests on a GUI narrower than 360: side tabs (B).
        var narrow = DOUBLE.input(new int[] {350, 270}, List.of(54), 0);
        h.assertTrue(checkTabs(h, narrow, "double chests @ 350 wide").mode() == com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Mode.TABS, "GUI < 360: tabs");
        var tabs = com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.compute(narrow.withTab(1));
        h.assertTrue(tabs.box() != null && tabs.part() == 0 && tabs.box().y() == 0 && tabs.bgTop() == tabs.box().bottom() && tabs.rows() == 6
                && tabs.box().bottom() <= DOUBLE.inventoryTop() - 2, "B marked tab: the marked double chest over the storage area, all 6 rows: " + tabs);
        h.assertTrue(com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.compute(narrow).box() == null, "B opened tab: the chest itself");
        // Recipe book open (screen moved right): A, tabs on the right, no sideways shift; E falls back to scrolling instead of C.
        var book = CRAFTING.input(FRAMES[4], List.of(27), 77);
        var withBook = checkTabs(h, book, "crafting + book @ 720p*3");
        h.assertTrue(withBook.mode() == com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Mode.BELOW && withBook.dx() == 0
                && withBook.tabs().get(0).x() > withBook.box().right(), "Recipe book open: A with the tabs on the right: " + withBook);
        var eBook = checkTabs(h, E.input(FRAMES[4], List.of(54), 77), "E + double chest + book @ 720p*3");
        h.assertTrue(eBook.mode() == com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Mode.BELOW_SCROLL && eBook.dx() == 0, "E + book: below, scrolling, never beside");
        // Whitelist of screens that may move.
        h.assertTrue(com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.clean("net.minecraft.client.gui.screens.inventory.CraftingScreen", false)
                && !com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.clean("net.minecraft.client.gui.screens.inventory.ContainerScreen", false)
                && com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.clean("net.minecraft.client.gui.screens.inventory.ContainerScreen", true)
                && !com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.clean("net.minecraft.client.gui.screens.inventory.BeaconScreen", true)
                && !com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.clean("com.example.ModChestScreen", true), "Clean screens: Vanilla leftPos screens, storage only styled");
        // SimpleLib HUD reservation: a gadget box under the tiles moves below them, one beside them stays.
        h.assertTrue(com.simplelib.api.client.HudReserve.clearTop(10, 10, 80, 76, 25) == 27 && com.simplelib.api.client.HudReserve.clearTop(10, 40, 80, 76, 25) == 40
                && com.simplelib.api.client.HudReserve.clearTop(90, 10, 80, 76, 25) == 10 && com.simplelib.api.client.HudReserve.clearTop(10, 10, 80, 0, 0) == 10,
                "HudReserve keeps gadget boxes clear of the tiles");
        h.succeed();
    }

    /** A7 margins: every box and hotbar strip of every case, frame and tab keeps 3 px fill; 177 wide, 182 with the scroll bar. */
    public static void layoutMargins(GameTestHelper h) {
        int checked = 0;
        for (Case c : CASES) {
            for (int f = 0; f < FRAMES.length; f++) {
                var in = c.screen().input(FRAMES[f], c.marked(), 0);
                var first = com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.compute(in);
                for (int t = 0; t < Math.max(1, first.tabs().size()); t++) {
                    var l = com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.compute(in.withTab(t));
                    String what = c.screen().name() + " + " + c.marked() + " @ " + FRAME_NAMES[f] + " tab " + t;
                    int[] m = com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.margins(l);
                    for (int i = 0; i < m.length; i++) {
                        h.assertTrue(m[i] == -1 || m[i] == com.simplelib.api.client.ui.UiBoxes.MARGIN, what + ": margin " + i + " = " + m[i] + " " + java.util.Arrays.toString(m));
                    }
                    if (l.box() != null) {
                        h.assertTrue(l.box().width() == (l.scroll() ? 182 : 177) && l.box().height() == 27 + 18 * l.rows(), what + ": box " + l.box());
                        checked++;
                    }
                    if (l.hotbar() != null) h.assertTrue(l.hotbar().width() == 177 && l.hotbar().height() == 35, what + ": hotbar strip " + l.hotbar());
                }
            }
        }
        h.assertTrue(checked > 100, "Boxes checked: " + checked);
        h.succeed();
    }
}
