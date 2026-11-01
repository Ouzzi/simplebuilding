package com.simplebuilding.modules.simplemobheads;

import com.mojang.blaze3d.platform.NativeImage;
import com.simplemobheads.MobHeads;
import com.simplemobheads.data.HeadDef;
import com.simplemobheads.data.HeadTable;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractSkullBlock;

/**
 * IG31-39: every head of the "Simple Mob Heads" creative tab, drawn as a GUI item, sits in its own 16 x 16 slot. The heads are
 * drawn by a plain screen on a magenta ground in a grid with 8 px gaps (plus three Vanilla heads as the reference that the check
 * itself works) and the screenshot is read back: per slot enough head pixels inside, at least half the slot wide or high,
 * centred within 3 px, and nothing in the 4 px ring around the slot. Also photographs the creative tab, the hotbar and the head
 * slot for the eye. Client mixins (GuiHeadFitMixin) do not run in server game tests - only this shows the bug.
 */
public final class HeadSlotFitClientTest implements FabricClientGameTest {
    private static final int SLOT = 16;
    private static final int PITCH = 24;
    private static final int RING = 4;
    private static final int GROUND = 0xFFFF00FF;
    /** Allowed distance of the head's pixel box from the slot centre, GUI px (cut-out texture pixels make it not exactly 0). */
    private static final int CENTRE = 3;
    private static final int REFERENCES = 3;

    @Override
    public void runTest(ClientGameTestContext context) {
        List<String> faults = new ArrayList<>();
        int checked = 0;
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            // hotbar and head slot (survival inventory)
            world.getServer().runOnServer(server -> {
                // stacks only once a world has bound the item components
                List<ItemStack> hotbar = new ArrayList<>();
                for (HeadDef head : HeadTable.HEADS.subList(0, 9)) {
                    hotbar.add(MobHeads.stack(head, null));
                }
                var player = server.getPlayerList().getPlayers().getFirst();
                for (int slot = 0; slot < 9; slot++) {
                    player.getInventory().setItem(slot, hotbar.get(slot).copy());
                }
                player.setItemSlot(EquipmentSlot.HEAD, hotbar.get(0).copy());
                player.inventoryMenu.broadcastChanges();
            });
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(5);
            context.takeScreenshot("simplemobheads-hotbar");
            context.runOnClient(client -> client.gui.setScreen(new InventoryScreen(client.player)));
            context.waitTicks(5);
            context.takeScreenshot("simplemobheads-head-slot");
            context.runOnClient(client -> client.gui.setScreen(null));
            // the creative tab as the owner saw it
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE));
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(client -> client.setScreenAndShow(new CreativeModeInventoryScreen(
                    client.player, client.player.connection.enabledFeatures(), false)));
            context.waitTicks(3);
            context.runOnClient(client -> {
                var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(MobHeads.TAB);
                if (!((FabricCreativeModeInventoryScreen) client.gui.screen()).setSelectedTab(tab)) {
                    throw new AssertionError("Simple Mob Heads tab cannot be selected");
                }
            });
            context.waitTicks(5);
            context.takeScreenshot("simplemobheads-creative-tab");
            List<ItemStack> stacks = context.computeOnClient(client -> {
                List<ItemStack> all = new ArrayList<>(List.of(new ItemStack(Items.SKELETON_SKULL), new ItemStack(Items.ZOMBIE_HEAD),
                        new ItemStack(Items.CREEPER_HEAD)));
                for (ItemStack stack : BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(MobHeads.TAB).getDisplayItems()) {
                    if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AbstractSkullBlock) {
                        all.add(stack.copy());
                    }
                }
                return all;
            });
            if (stacks.size() < 20) {
                faults.add("only " + stacks.size() + " heads in the tab");
            }
            // the pixel check, page by page
            int[] grid = context.computeOnClient(client -> new int[] {
                    client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight()});
            int columns = (grid[0] - RING * 2) / PITCH;
            int rows = (grid[1] - RING * 2) / PITCH;
            int perPage = columns * rows;
            for (int page = 0; page * perPage < stacks.size(); page++) {
                List<ItemStack> slice = stacks.subList(page * perPage, Math.min(stacks.size(), (page + 1) * perPage));
                context.runOnClient(client -> client.gui.setScreen(new Grid(slice, columns)));
                context.waitTicks(3);
                Path shot = context.takeScreenshot(String.format("simplemobheads-slotfit-%02d", page));
                try (InputStream in = Files.newInputStream(shot); NativeImage image = NativeImage.read(in)) {
                    double scale = image.getWidth() / (double) grid[0];
                    for (int i = 0; i < slice.size(); i++) {
                        // the Vanilla references (first three) prove the check works; their place is Vanilla's, so no centre check
                        String fault = check(image, scale, Grid.x(i, columns), Grid.y(i, columns), page > 0 || i >= REFERENCES);
                        checked++;
                        if (fault != null) {
                            faults.add(slice.get(i).getItem() + " " + slice.get(i).getComponents().get(MobHeads.VARIANT) + ": " + fault);
                        }
                    }
                } catch (java.io.IOException e) {
                    throw new AssertionError("Screenshot unreadable: " + shot, e);
                }
            }
            context.runOnClient(client -> client.gui.setScreen(null));
        }
        System.out.println("[headslotfit] heads checked: " + checked + ", faults: " + faults.size());
        faults.forEach(f -> System.out.println("[headslotfit] FAULT " + f));
        if (!faults.isEmpty()) {
            throw new AssertionError(faults.size() + " heads do not fit their slot, first: " + faults.getFirst());
        }
    }

    /** Null when the slot at GUI (x, y) holds a head that fills it, centred, and the ring around it is clean. */
    private static String check(NativeImage image, double scale, int x, int y, boolean centred) {
        int x0 = (int) Math.round(x * scale), y0 = (int) Math.round(y * scale), size = (int) Math.round(SLOT * scale);
        int ring = (int) Math.round(RING * scale);
        int inside = 0, outside = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int py = y0 - ring; py < y0 + size + ring; py++) {
            for (int px = x0 - ring; px < x0 + size + ring; px++) {
                if (px < 0 || py < 0 || px >= image.getWidth() || py >= image.getHeight() || ground(image.getPixel(px, py))) {
                    continue;
                }
                if (px >= x0 && px < x0 + size && py >= y0 && py < y0 + size) {
                    inside++;
                    minX = Math.min(minX, px); maxX = Math.max(maxX, px); minY = Math.min(minY, py); maxY = Math.max(maxY, py);
                } else {
                    outside++;
                }
            }
        }
        if (outside > 0) {
            return outside + " px outside the slot";
        }
        if (inside < size * size / 12) {
            return "slot nearly empty (" + inside + " px)";
        }
        if (Math.max(maxX - minX + 1, maxY - minY + 1) < size / 2) {
            return "head too small (" + (maxX - minX + 1) + "x" + (maxY - minY + 1) + " px of " + size + ")";
        }
        double offX = (minX + maxX + 1) / 2.0 - (x0 + size / 2.0), offY = (minY + maxY + 1) / 2.0 - (y0 + size / 2.0);
        if (centred && (Math.abs(offX) > CENTRE * scale || Math.abs(offY) > CENTRE * scale)) {
            return String.format("off centre by %.1f/%.1f px", offX / scale, offY / scale);
        }
        return null;
    }

    private static boolean ground(int argb) {
        return Math.abs(((argb >> 16) & 0xFF) - 0xFF) < 12 && ((argb >> 8) & 0xFF) < 12 && Math.abs((argb & 0xFF) - 0xFF) < 12;
    }

    /** A plain screen: magenta ground, the stacks in a grid with gaps, nothing else. */
    private static final class Grid extends Screen {
        private final List<ItemStack> stacks;
        private final int columns;

        Grid(List<ItemStack> stacks, int columns) {
            super(Component.empty());
            this.stacks = stacks;
            this.columns = columns;
        }

        static int x(int i, int columns) {
            return RING * 2 + (i % columns) * PITCH;
        }

        static int y(int i, int columns) {
            return RING * 2 + (i / columns) * PITCH;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            graphics.fill(0, 0, width, height, GROUND);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractRenderState(graphics, mouseX, mouseY, a);
            for (int i = 0; i < stacks.size(); i++) {
                graphics.item(stacks.get(i), x(i, columns), y(i, columns));
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
