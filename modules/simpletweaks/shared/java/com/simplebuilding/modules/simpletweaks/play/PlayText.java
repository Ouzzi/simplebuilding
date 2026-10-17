package com.simplebuilding.modules.simpletweaks.play;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.world.level.Level;

/** Shared text helpers: {@code simpletweaks.play.*} keys, coordinates with copy-on-click, durations. */
public final class PlayText {
    public static MutableComponent t(String key, Object... args) { return Component.translatable("simpletweaks.play." + key, args); }

    /** "x y z" in green; click copies it, hover explains. */
    public static MutableComponent coords(int x, int y, int z) {
        String text = x + " " + y + " " + z;
        return Component.literal(text).withStyle(s -> s.withColor(ChatFormatting.GREEN).withUnderlined(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(text))
                .withHoverEvent(new HoverEvent.ShowText(t("copy"))));
    }
    /** The dimension id as readable text (Vanilla names for the three Vanilla dimensions). */
    public static MutableComponent dimension(net.minecraft.resources.ResourceKey<Level> key) {
        var id = key.identifier();
        if (id.getNamespace().equals("minecraft")) return t("dimension." + id.getPath());
        return Component.literal(id.toString());
    }
    /** 1 h 23 min / 4 min / 12 s. */
    public static MutableComponent duration(long millis) {
        long s = Math.max(0, millis / 1000);
        long d = s / 86400, h = s % 86400 / 3600, m = s % 3600 / 60;
        if (d > 0) return t("duration.days", d, h);
        if (h > 0) return t("duration.hours", h, m);
        if (m > 0) return t("duration.minutes", m);
        return t("duration.seconds", s);
    }
    /** A clickable "[label]" that runs a command. */
    public static MutableComponent button(MutableComponent label, String command, ChatFormatting color) {
        return Component.literal("[").append(label).append("]").withStyle(s -> s.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(command)).withHoverEvent(new HoverEvent.ShowText(Component.literal(command))));
    }
    private PlayText() {}
}
