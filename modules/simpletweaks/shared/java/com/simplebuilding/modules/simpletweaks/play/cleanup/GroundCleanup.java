package com.simplebuilding.modules.simpletweaks.play.cleanup;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Rarity;

/**
 * T32 (off by default): every {@code intervalMinutes} ground items are cleared after a countdown in chat
 * ({@code warnSeconds}). Only items lying longer than {@code minAgeSeconds} go; renamed items, items of
 * {@code exemptRarity} or rarer and the {@code exempt} list stay. /tweaks cleanup now (operators) starts the countdown.
 */
public final class GroundCleanup {
    static long due = -1;
    static final Set<Integer> WARNED = new HashSet<>();

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().cleanup().enabled(); }
    public static long due() { return due; }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.cleanup().enabled()) return;
        int perm = c.cleanup().permission();
        d.register(Commands.literal("tweaks").then(Commands.literal("cleanup").requires(s -> PlayPerms.has(s, perm))
                .then(Commands.literal("now").executes(x -> { schedule(x.getSource().getServer(), firstWarning()); return 1; }))));
    }
    static int firstWarning() { var w = Play.config().cleanup().warnSeconds(); return w.isEmpty() ? 1 : w.get(0); }
    /** Clears in {@code seconds} (server ticks, so a lagging server waits longer, never shorter). */
    public static void schedule(MinecraftServer server, int seconds) { due = server.getTickCount() + seconds * 20L; WARNED.clear(); }

    public static boolean exempt(ItemEntity item) {
        var c = Play.config().cleanup();
        var st = item.getItem();
        if (st.has(DataComponents.CUSTOM_NAME)) return true;
        if (c.exempt().contains(BuiltInRegistries.ITEM.getKey(st.getItem()).toString())) return true;
        if (c.exemptRarity().equals("none")) return false;
        var min = Rarity.valueOf(c.exemptRarity().toUpperCase(Locale.ROOT));
        return st.getRarity().ordinal() >= min.ordinal();
    }
    /** Removes the items that may go now; the number removed. */
    public static int clear(MinecraftServer server) {
        int minAge = Play.config().cleanup().minAgeSeconds() * 20, n = 0;
        for (var level : server.getAllLevels()) {
            var list = new ArrayList<ItemEntity>();
            for (var e : level.getAllEntities()) if (e instanceof ItemEntity item && item.isAlive() && item.tickCount >= minAge && !exempt(item)) list.add(item);
            for (var item : list) { item.discard(); n++; }
        }
        return n;
    }

    public static void tick(MinecraftServer server) {
        if (!active()) { due = -1; return; }
        long now = server.getTickCount();
        if (due < 0) { schedule(server, Play.config().cleanup().intervalMinutes() * 60); return; }
        long left = (due - now + 19) / 20;
        if (now >= due) {
            int n = clear(server);
            server.getPlayerList().broadcastSystemMessage(PlayText.t("cleanup.done", n).withStyle(ChatFormatting.GRAY), false);
            schedule(server, Play.config().cleanup().intervalMinutes() * 60);
            return;
        }
        for (int w : Play.config().cleanup().warnSeconds()) {
            if (left == w && WARNED.add(w)) server.getPlayerList().broadcastSystemMessage(PlayText.t("cleanup.warn", w).withStyle(ChatFormatting.YELLOW), false);
        }
    }
    public static void reset() { due = -1; WARNED.clear(); }
    private GroundCleanup() {}
}
