package com.simplebuilding.modules.simpletweaks.admin.blocklog;

import com.simplebuilding.modules.simpletweaks.admin.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;

/** /inspect: a click on a block shows its history (paged) instead of breaking or using it. */
public final class Inspect {
    public static final int PAGE = 7;
    private record Last(BlockPos pos, List<BlockLog.Entry> entries) {}
    private static final Set<UUID> ON = new HashSet<>();
    private static final Map<UUID, Last> LAST = new HashMap<>();

    public static boolean toggle(ServerPlayer p) {
        if (!ON.remove(p.getUUID())) { ON.add(p.getUUID()); return true; }
        LAST.remove(p.getUUID()); return false;
    }
    public static boolean inspecting(ServerPlayer p) {
        return !ON.isEmpty() && ON.contains(p.getUUID()) && BlockLog.active() && AdminPerms.has(p, Admin.config().blockLog().permission());
    }
    /** Mixins: a click while inspecting. Returns true when the click was consumed. */
    public static boolean click(ServerPlayer p, BlockPos pos) {
        if (!inspecting(p)) return false;
        var server = p.level().getServer();
        BlockLog.at(p.level(), pos).thenAccept(list -> server.execute(() -> {
            LAST.put(p.getUUID(), new Last(pos.immutable(), list));
            page(p, 1);
        }));
        return true;
    }
    /** Shows one page of the last inspected block; false if there is none. */
    public static boolean page(ServerPlayer p, int page) {
        var last = LAST.get(p.getUUID());
        if (last == null) return false;
        int pages = Math.max(1, (last.entries.size() + PAGE - 1) / PAGE);
        page = Math.clamp(page, 1, pages);
        var pos = last.pos;
        p.sendSystemMessage(Component.translatable("simpletweaks.admin.blocklog.header", pos.getX(), pos.getY(), pos.getZ(), page, pages).withStyle(ChatFormatting.GOLD));
        if (last.entries.isEmpty()) { p.sendSystemMessage(Component.translatable("simpletweaks.admin.blocklog.none").withStyle(ChatFormatting.GRAY)); return true; }
        long now = BlockLog.clock.millis();
        for (var e : last.entries.subList((page - 1) * PAGE, Math.min(last.entries.size(), page * PAGE))) p.sendSystemMessage(line(e, now));
        if (pages > 1) p.sendSystemMessage(Component.translatable("simpletweaks.admin.blocklog.more", page, pages).withStyle(ChatFormatting.GRAY));
        return true;
    }
    public static Component line(BlockLog.Entry e, long now) {
        Component what = name(e);
        return Component.translatable("simpletweaks.admin.blocklog.line", ago(now - e.time()),
                Component.translatable("simpletweaks.admin.blocklog.action." + e.action(), e.name(), what, e.count()));
    }
    static Component name(BlockLog.Entry e) {
        var id = Identifier.tryParse(e.id());
        if (id == null) return Component.literal(e.id());
        if (e.action() == 'B' || e.action() == 'P') return BuiltInRegistries.BLOCK.getOptional(id).map(b -> (Component) b.getName()).orElse(Component.literal(e.id()));
        return BuiltInRegistries.ITEM.getOptional(id).map(i -> new ItemStack(i).getHoverName()).orElse(Component.literal(e.id()));
    }
    /** 42s, 5m, 3h, 2d. */
    public static String ago(long millis) {
        long s = Math.max(0, millis / 1000);
        if (s < 60) return s + "s";
        if (s < 3600) return s / 60 + "m";
        if (s < 86400) return s / 3600 + "h";
        return s / 86400 + "d";
    }
    /** 30s, 15m, 2h, 1d, 1w and combinations (1h30m); -1 if invalid. */
    public static long parseDuration(String text) {
        var m = java.util.regex.Pattern.compile("(\\d{1,6})([smhdw])").matcher(text.toLowerCase(Locale.ROOT));
        long total = 0; int end = 0;
        while (m.find()) {
            if (m.start() != end) return -1;
            end = m.end();
            long n = Long.parseLong(m.group(1));
            total += n * switch (m.group(2)) { case "s" -> 1000L; case "m" -> 60_000L; case "h" -> 3_600_000L; case "d" -> 86_400_000L; default -> 604_800_000L; };
        }
        return end == text.length() && end > 0 ? total : -1;
    }
    public static void forget(UUID id) { ON.remove(id); LAST.remove(id); }
    private Inspect() {}
}
