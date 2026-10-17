package com.simplebuilding.modules.simpletweaks.play.border;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.play.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;

/**
 * T25: one world border per dimension. Vanilla 26.3 already keeps a border per dimension (level data of each
 * dimension, {@code /execute in <dim> run worldborder ...}), so nothing is duplicated: entries of
 * {@code border.dimensions} ("minecraft:the_nether": size, center) are applied to that Vanilla border at server start,
 * and /tweaks border lists the borders of all dimensions. Without entries this feature changes nothing.
 */
public final class Borders {
    public static boolean active() { return Play.on() && Play.config().border().enabled(); }

    /** Applies the configured entries; the number of dimensions changed. */
    public static int apply(MinecraftServer server) {
        if (!active()) return 0;
        int n = 0;
        for (var e : Play.config().border().dimensions().entrySet()) {
            var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(e.getKey())));
            if (level == null) { org.slf4j.LoggerFactory.getLogger("simpletweaks-play").warn("Border for unknown dimension {} ignored", e.getKey()); continue; }
            var b = level.getWorldBorder();
            var d = e.getValue();
            if (b.getCenterX() != d.centerX() || b.getCenterZ() != d.centerZ()) b.setCenter(d.centerX(), d.centerZ());
            if (b.getSize() != d.size()) b.setSize(d.size());
            n++;
        }
        return n;
    }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.border().enabled()) return;
        d.register(Commands.literal("tweaks").then(Commands.literal("border").requires(s -> PlayPerms.has(s, 2)).executes(x -> {
            var out = PlayText.t("border.title").withStyle(ChatFormatting.GOLD);
            for (var level : x.getSource().getServer().getAllLevels()) {
                var b = level.getWorldBorder();
                boolean configured = Play.config().border().dimensions().containsKey(level.dimension().identifier().toString());
                out.append(Component.literal("\n" + level.dimension().identifier() + ": ").withStyle(ChatFormatting.GRAY))
                        .append(PlayText.t("border.line", Math.round(b.getSize()), Math.round(b.getCenterX()), Math.round(b.getCenterZ())))
                        .append(configured ? PlayText.t("border.configured").withStyle(ChatFormatting.GREEN) : Component.empty());
            }
            x.getSource().sendSuccess(() -> out, false);
            return 1;
        })));
    }
    private Borders() {}
}
