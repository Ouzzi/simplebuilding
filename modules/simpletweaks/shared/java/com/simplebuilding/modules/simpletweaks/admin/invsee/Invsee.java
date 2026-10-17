package com.simplebuilding.modules.simpletweaks.admin.invsee;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.simplebuilding.modules.simpletweaks.admin.*;
import com.simplebuilding.modules.simpletweaks.admin.deaths.DeathBackups;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * T13 /invsee and /endersee, T11 death backup restore: one menu family with its own screen. Online targets are edited
 * live (the slots are the target's own containers); offline targets are written back to the player file after every
 * change; a login of the target closes offline views first ({@link #loading}), so no item can exist twice.
 */
public final class Invsee {
    public static MenuType<InvseeMenu> INVSEE, ENDERSEE, DEATHS;

    public static void registerMenus() {
        INVSEE = Registry.register(BuiltInRegistries.MENU, id("invsee"), new MenuType<>((i, inv) -> new InvseeMenu(INVSEE, i, inv, InvseeMenu.Kind.INVENTORY), FeatureFlags.VANILLA_SET));
        ENDERSEE = Registry.register(BuiltInRegistries.MENU, id("endersee"), new MenuType<>((i, inv) -> new InvseeMenu(ENDERSEE, i, inv, InvseeMenu.Kind.ENDER), FeatureFlags.VANILLA_SET));
        DEATHS = Registry.register(BuiltInRegistries.MENU, id("death_backup"), new MenuType<>((i, inv) -> new InvseeMenu(DEATHS, i, inv, InvseeMenu.Kind.DEATHS), FeatureFlags.VANILLA_SET));
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("simpletweaks", path); }

    /** Server side of an open view. */
    public interface Target {
        boolean valid(Player viewer);
        default void changed() {}
        default void closed() {}
        default boolean button(Player viewer, int id) { return false; }
        default UUID offline() { return null; }
    }

    // ---------------------------------------------------------------- opening

    public static boolean openInventory(ServerPlayer viewer, NameAndId who, boolean ender) {
        var cfg = Admin.config().invsee();
        var server = viewer.level().getServer();
        boolean edit = AdminPerms.has(viewer, cfg.editPermission());
        int need = cfg.permission();
        var kind = ender ? InvseeMenu.Kind.ENDER : InvseeMenu.Kind.INVENTORY;
        var type = ender ? ENDERSEE : INVSEE;
        var online = server.getPlayerList().getPlayer(who.id());
        Container container;
        Target target;
        boolean offline = online == null;
        if (online != null) {
            container = ender ? online.getEnderChestInventory() : online.getInventory();
            target = viewerOk(need, v -> server.getPlayerList().getPlayer(who.id()) == online && !online.hasDisconnected());
        } else {
            var data = OfflineData.load(server, who.id());
            if (data == null) return false;
            var box = new Box(ender ? OfflineData.ENDER : OfflineData.INVENTORY);
            var stacks = ender ? data.ender() : data.inventory();
            for (int i = 0; i < stacks.size(); i++) box.setItem(i, stacks.get(i));
            var off = new OfflineTarget(server, data, box, ender, need);
            box.onChange = () -> off.dirty = true;
            target = off;
            container = box;
        }
        Component title = Component.translatable("simpletweaks.admin." + (ender ? "endersee" : "invsee") + ".title" + (offline ? ".offline" : ""), who.name());
        final Container top = container; final Target t = target;
        viewer.openMenu(new SimpleMenuProvider((id, inv, p) -> new InvseeMenu(type, id, inv, kind, top, t, edit), title));
        return true;
    }

    /** Opens death backup {@code index} (0 = newest). False if the player has none. */
    public static boolean openDeaths(ServerPlayer viewer, NameAndId who, int index) {
        var server = viewer.level().getServer();
        var list = DeathBackups.list(server, who.id());
        if (list.isEmpty()) return false;
        index = Math.clamp(index, 0, list.size() - 1);
        var b = list.get(index);
        var box = new Box(OfflineData.INVENTORY);
        for (int i = 0; i < OfflineData.INVENTORY; i++) box.setItem(i, b.slots().get(i).copy());
        var target = new DeathTarget(server, who, index, box, Admin.config().deathBackups().permission());
        box.onChange = () -> target.dirty = true;
        String when = DateTimeFormatter.ofPattern("MM-dd HH:mm").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(b.time()), ZoneId.systemDefault()));
        Component title = Component.translatable("simpletweaks.admin.deaths.title", who.name(), index + 1, list.size(), when, b.cause(), b.xpLevel(),
                b.dimension() + " " + b.x() + " " + b.y() + " " + b.z());
        viewer.openMenu(new SimpleMenuProvider((id, inv, p) -> new InvseeMenu(DEATHS, id, inv, InvseeMenu.Kind.DEATHS, box, target, true), title));
        return true;
    }

    /** A container that reports every change (Vanilla's SimpleContainer has no listeners any more). */
    static final class Box extends SimpleContainer {
        Runnable onChange;
        Box(int size) { super(size); }
        @Override public void setChanged() { super.setChanged(); if (onChange != null) onChange.run(); }
    }

    private static Target viewerOk(int need, java.util.function.Predicate<Player> alsoValid) {
        return v -> v instanceof ServerPlayer sp && Admin.on() && AdminPerms.has(sp, need) && alsoValid.test(v);
    }

    static final class OfflineTarget implements Target {
        final MinecraftServer server; final OfflineData data; final Box box; final boolean ender; final int need;
        boolean dirty, gone;
        OfflineTarget(MinecraftServer server, OfflineData data, Box box, boolean ender, int need) { this.server = server; this.data = data; this.box = box; this.ender = ender; this.need = need; }
        @Override public boolean valid(Player v) {
            return !gone && v instanceof ServerPlayer sp && Admin.on() && AdminPerms.has(sp, need) && server.getPlayerList().getPlayer(data.id()) == null;
        }
        @Override public void changed() { if (dirty && !gone) flush(); }
        @Override public void closed() { if (dirty && !gone) flush(); gone = true; }
        @Override public UUID offline() { return data.id(); }
        void flush() {
            dirty = false;
            var stacks = new ArrayList<ItemStack>();
            for (int i = 0; i < box.getContainerSize(); i++) stacks.add(box.getItem(i).copy());
            if (ender) data.setEnder(stacks); else data.setInventory(stacks);
            data.save();
        }
    }

    static final class DeathTarget implements Target {
        final MinecraftServer server; final NameAndId who; final int index; final Box box; final int need;
        boolean dirty;
        DeathTarget(MinecraftServer server, NameAndId who, int index, Box box, int need) { this.server = server; this.who = who; this.index = index; this.box = box; this.need = need; }
        @Override public boolean valid(Player v) { return v instanceof ServerPlayer sp && Admin.on() && AdminPerms.has(sp, need); }
        @Override public void changed() { if (dirty) flush(); }
        @Override public void closed() { if (dirty) flush(); }
        void flush() {
            dirty = false;
            var stacks = new ArrayList<ItemStack>();
            for (int i = 0; i < box.getContainerSize(); i++) stacks.add(box.getItem(i).copy());
            DeathBackups.update(server, who.id(), index, stacks);
        }
        @Override public boolean button(Player v, int id) {
            if (!(v instanceof ServerPlayer sp) || !valid(v)) return false;
            if (dirty) flush();
            switch (id) {
                case InvseeMenu.PREV -> server.execute(() -> openDeaths(sp, who, index - 1));
                case InvseeMenu.NEXT -> server.execute(() -> openDeaths(sp, who, index + 1));
                case InvseeMenu.RESTORE -> {
                    boolean ok = DeathBackups.restore(server, who.id(), index);
                    sp.sendSystemMessage(Component.translatable(ok ? "simpletweaks.admin.deaths.restored" : "simpletweaks.admin.deaths.restore_failed", who.name()));
                    // the restore saved the old inventory as a new newest backup: the restored one moved one place down
                    if (ok) server.execute(() -> openDeaths(sp, who, index + 1));
                }
                default -> { return false; }
            }
            return true;
        }
    }

    // ---------------------------------------------------------------- safety

    /** Before a player's data is loaded for a login (mixin on {@code PlayerList.loadPlayerData}): close their offline views. */
    public static void loading(MinecraftServer server, UUID id) {
        if (server == null) return;
        for (var viewer : server.getPlayerList().getPlayers()) {
            if (viewer.containerMenu instanceof InvseeMenu m && m.target != null && id.equals(m.target.offline())) {
                m.target.closed();
                viewer.closeContainer();
            }
        }
    }
    public static void closeAll(MinecraftServer server) {
        for (var viewer : server.getPlayerList().getPlayers()) if (viewer.containerMenu instanceof InvseeMenu) viewer.closeContainer();
    }

    // ---------------------------------------------------------------- commands

    public static void register(CommandDispatcher<CommandSourceStack> d, AdminConfig config) {
        if (config.invsee().enabled()) {
            d.register(Commands.literal("invsee").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().invsee().permission()))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Invsee::online).executes(c -> open(c.getSource(), StringArgumentType.getString(c, "player"), false))));
            d.register(Commands.literal("endersee").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().invsee().permission()))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Invsee::online).executes(c -> open(c.getSource(), StringArgumentType.getString(c, "player"), true))));
        }
        if (config.deathBackups().enabled()) {
            d.register(Commands.literal("tweaks").then(Commands.literal("deaths").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().deathBackups().permission()))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Invsee::online)
                            .executes(c -> deaths(c.getSource(), StringArgumentType.getString(c, "player"), 1))
                            .then(Commands.argument("number", IntegerArgumentType.integer(1, 50))
                                    .executes(c -> deaths(c.getSource(), StringArgumentType.getString(c, "player"), IntegerArgumentType.getInteger(c, "number")))))));
        }
    }
    static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> online(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(c.getSource().getOnlinePlayerNames(), b);
    }
    static int open(CommandSourceStack s, String who, boolean ender) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var viewer = s.getPlayerOrException();
        var profile = AdminPlayers.resolve(s.getServer(), who);
        if (profile == null) { s.sendFailure(Component.translatable("simpletweaks.admin.unknown_player", who)); return 0; }
        if (profile.id().equals(viewer.getUUID())) { s.sendFailure(Component.translatable("simpletweaks.admin.invsee.self")); return 0; }
        if (!openInventory(viewer, profile, ender)) { s.sendFailure(Component.translatable("simpletweaks.admin.invsee.nodata", profile.name())); return 0; }
        return 1;
    }
    static int deaths(CommandSourceStack s, String who, int number) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var viewer = s.getPlayerOrException();
        var profile = AdminPlayers.resolve(s.getServer(), who);
        if (profile == null) { s.sendFailure(Component.translatable("simpletweaks.admin.unknown_player", who)); return 0; }
        if (!openDeaths(viewer, profile, number - 1)) { s.sendFailure(Component.translatable("simpletweaks.admin.deaths.none", profile.name())); return 0; }
        return 1;
    }
    private Invsee() {}
}
