package com.simplemachines.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplemachines.crafter.AutonomousCrafterMenu;
import com.simplemachines.crafter.Crafters;
import com.simplemachines.hopper.HopperTier;
import com.simplemachines.hopper.Hoppers;
import com.simplemachines.hopper.TierHopperMenu;
import com.simplemachines.smither.AutoSmitherBlock;
import com.simplemachines.smither.AutoSmitherMenu;
import com.simplemachines.smither.Smithers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * PLAN40 simplemachines-3: the Auto Smither, Autonomous Crafter and tier hopper windows with the REAL operating-system mouse (pattern of
 * Simple Arrows' FletchingProbe): inert unless the game folder holds {@code simplemachines-machines-probe.json}. Then, on the title
 * screen, it creates a flat survival world and follows commands from {@code simplemachines-machines-command.json}:
 * {@code open} (machine smither | crafter | hopper, tier for the hopper: builds the machine two blocks east of the player with its
 * output container, fills the hotbar with the parts of the check and opens the window through the block's normal use),
 * {@code power} (on: a redstone block above the machine; the smither smiths on the rising edge, the crafter stops while powered),
 * {@code guiScale}, {@code shot}, {@code quit}. Each tick it writes {@code simplemachines-machines-status.json}: window position, the
 * menu, every slot's centre in window pixels with its item (and whether it is a player slot, with its inventory index), the widgets
 * (the filter key), the menu's own state (recipe error, powered, filter mode, switched-off grid slots) and the server side of the
 * output container. The driver ({@code modules/simplemachines/tools/machines_probe.py}) moves and clicks the X11 pointer through XTest.
 * Only this module's classes and Vanilla: no other module is imported.
 */
public final class MachinesProbe {
    private static final Gson GSON = new Gson();
    private static final String REQUEST = "simplemachines-machines-probe.json", STATUS = "simplemachines-machines-status.json",
            COMMAND = "simplemachines-machines-command.json";
    private static Boolean active;
    private static boolean ready, worldRequested;
    private static int ticks;
    /** The machine of the last {@code open} and its output container (server positions). */
    private static volatile BlockPos machine, output;

    private MachinesProbe() {}

    public static void tick(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath();
        if (active == null) active = Files.isRegularFile(dir.resolve(REQUEST));
        if (!active) return;
        ticks++;
        try {
            if (!ready) {
                if (!worldRequested) {
                    if (mc.level != null || mc.gui.overlay() != null || mc.gui.screen() == null || ticks < 60) return;
                    worldRequested = true;
                    String name = "sb-machines-probe-" + System.nanoTime();
                    LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true), true, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0L, false, false),
                            provider -> provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.gui.screen());
                    return;
                }
                var server = mc.getSingleplayerServer();
                if (mc.level == null || mc.player == null || mc.gameMode == null || server == null || !server.isReady() || mc.gui.screen() != null
                        || ticks % 20 != 0) return;
                JsonObject req = GSON.fromJson(Files.readString(dir.resolve(REQUEST)), JsonObject.class);
                mc.getWindow().setWindowed(req.get("width").getAsInt(), req.get("height").getAsInt());
                mc.options.guiScale().set(req.get("guiScale").getAsInt());
                mc.resizeGui();
                ready = true;
                return;
            }
            Path cmd = dir.resolve(COMMAND);
            if (Files.isRegularFile(cmd)) {
                JsonObject c = GSON.fromJson(Files.readString(cmd), JsonObject.class);
                Files.delete(cmd);
                switch (c.get("op").getAsString()) {
                    case "open" -> open(mc, c.get("machine").getAsString(), c.has("tier") ? c.get("tier").getAsString() : "reinforced");
                    case "power" -> power(mc, c.get("on").getAsBoolean());
                    case "close" -> mc.player.closeContainer();
                    case "guiScale" -> {
                        mc.options.guiScale().set(c.get("value").getAsInt());
                        mc.resizeGui();
                    }
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> org.slf4j.LoggerFactory.getLogger("simplemachines").warn("machines probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simplemachines").warn("machines probe: {}", e.toString());
        }
    }

    /** On the server: a fresh hotbar, the machine two blocks east with its output, the window opened by the block's use. */
    private static void open(Minecraft mc, String kind, String tier) {
        var server = mc.getSingleplayerServer();
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) return;
            player.closeContainer();
            Inventory inv = player.getInventory();
            inv.clearContent();
            ServerLevel level = player.level();
            BlockPos at = player.blockPosition().relative(Direction.EAST, 2);
            for (BlockPos q : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 2, 1))) level.setBlock(q, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            // a plain floor under the machine (the crafter's hopper of an earlier round would drain a tier hopper)
            level.setBlock(at.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
            Block block;
            switch (kind) {
                case "smither" -> {
                    inv.setItem(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                    inv.setItem(1, new ItemStack(Items.DIAMOND_SWORD));
                    inv.setItem(2, new ItemStack(Items.NETHERITE_INGOT, 2));
                    inv.setItem(3, new ItemStack(Items.DIRT, 4));
                    inv.setItem(4, new ItemStack(Items.IRON_INGOT, 2));
                    // the smither looks east into a chest: the finished item goes there
                    level.setBlock(at.east(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
                    output = at.east();
                    block = Smithers.BLOCK;
                    level.setBlock(at, block.defaultBlockState().setValue(AutoSmitherBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
                }
                case "crafter" -> {
                    inv.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
                    inv.setItem(1, new ItemStack(Items.OAK_PLANKS, 4));
                    // powered from the start (a powered crafter does not craft), a hopper below it (floor level) takes the results
                    level.setBlock(at.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(at.below(), Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
                    output = at.below();
                    block = Crafters.BLOCK;
                    level.setBlock(at, block.defaultBlockState(), Block.UPDATE_ALL);
                }
                default -> {
                    inv.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
                    inv.setItem(1, new ItemStack(Items.DIRT, 16));
                    block = Hoppers.block(HopperTier.valueOf(tier.toUpperCase(Locale.ROOT)));
                    level.setBlock(at, block.defaultBlockState(), Block.UPDATE_ALL);
                    output = at;
                }
            }
            machine = at.immutable();
            level.getBlockState(at).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(at), Direction.WEST, at, false));
        });
    }

    /** A redstone block above the machine, or air there. */
    private static void power(Minecraft mc, boolean on) {
        var server = mc.getSingleplayerServer();
        BlockPos at = machine;
        if (at == null) return;
        server.execute(() -> server.getPlayerList().getPlayers().getFirst().level()
                .setBlock(at.above(), (on ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState(), Block.UPDATE_ALL));
    }

    private static void writeStatus(Minecraft mc, Path dir) throws Exception {
        JsonObject o = new JsonObject();
        var w = mc.getWindow();
        int scale = w.getGuiScale();
        var screen = mc.gui.screen();
        o.addProperty("tick", ticks);
        o.addProperty("ready", mc.level != null);
        o.addProperty("screen", screen == null ? "none" : screen.getClass().getSimpleName());
        o.addProperty("windowX", w.getX());
        o.addProperty("windowY", w.getY());
        o.addProperty("scale", scale);
        JsonArray slots = new JsonArray();
        JsonArray widgets = new JsonArray();
        if (screen instanceof AbstractContainerScreen<?> s) {
            var menu = s.getMenu();
            o.addProperty("menu", menu.getClass().getSimpleName());
            int left = field(s, "leftPos"), top = field(s, "topPos");
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                JsonObject e = new JsonObject();
                e.addProperty("i", i);
                e.addProperty("inv", slot.container instanceof Inventory);
                e.addProperty("cs", slot.getContainerSlot());
                e.addProperty("x", (left + slot.x + 8) * scale);
                e.addProperty("y", (top + slot.y + 8) * scale);
                describe(e, slot.getItem());
                slots.add(e);
            }
            JsonObject carried = new JsonObject();
            describe(carried, menu.getCarried());
            o.add("carried", carried);
            if (menu instanceof AutoSmitherMenu m) {
                o.addProperty("recipeError", m.hasRecipeError());
                o.addProperty("powered", m.isPowered());
            } else if (menu instanceof AutonomousCrafterMenu m) {
                o.addProperty("filter", m.filterMode().name());
                o.addProperty("powered", m.isPowered());
                JsonArray off = new JsonArray();
                for (int i = 0; i < 9; i++) if (m.isSlotDisabled(i)) off.add(i);
                o.add("disabled", off);
            } else if (menu instanceof TierHopperMenu m) {
                o.addProperty("filter", m.getSyncedFilterMode().name());
                o.addProperty("tier", m.tier().name());
            }
            for (var child : screen.children()) {
                if (!(child instanceof AbstractWidget a)) continue;
                JsonObject j = new JsonObject();
                j.addProperty("class", a.getClass().getSimpleName());
                j.addProperty("x", a.getX() * scale);
                j.addProperty("y", a.getY() * scale);
                j.addProperty("w", a.getWidth() * scale);
                j.addProperty("h", a.getHeight() * scale);
                j.addProperty("visible", a.visible);
                widgets.add(j);
            }
        }
        o.add("slots", slots);
        o.add("widgets", widgets);
        var server = mc.getSingleplayerServer();
        BlockPos out = output;
        if (server != null && out != null) {
            try {
                JsonArray items = server.submit(() -> {
                    JsonArray a = new JsonArray();
                    ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                    BlockState state = level.getBlockState(out);
                    if (level.getBlockEntity(out) instanceof Container c) {
                        for (int i = 0; i < c.getContainerSize(); i++) {
                            ItemStack stack = c.getItem(i);
                            if (stack.isEmpty()) continue;
                            JsonObject e = new JsonObject();
                            describe(e, stack);
                            a.add(e);
                        }
                    } else {
                        JsonObject e = new JsonObject();
                        e.addProperty("item", "no container: " + BuiltInRegistries.BLOCK.getKey(state.getBlock()));
                        a.add(e);
                    }
                    return a;
                }).get(2, TimeUnit.SECONDS);
                o.add("output", items);
            } catch (Exception e) {
                o.addProperty("serverError", e.toString());
            }
        }
        Path tmp = dir.resolve("simplemachines-machines-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve(STATUS), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static void describe(JsonObject e, ItemStack stack) {
        e.addProperty("item", stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        e.addProperty("count", stack.getCount());
    }

    private static int field(AbstractContainerScreen<?> screen, String name) throws ReflectiveOperationException {
        java.lang.reflect.Field f = AbstractContainerScreen.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(screen);
    }
}
