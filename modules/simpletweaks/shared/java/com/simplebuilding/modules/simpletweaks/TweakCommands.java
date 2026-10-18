package com.simplebuilding.modules.simpletweaks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Every Simple Tweaks command is also reachable as {@code /simpletweaks <command> ...} (one word, no hyphen). The short
 * top-level commands become redirect nodes (same executor, same requirement, same children and suggestions) and the
 * {@code /tweaks} branches are shared, so each branch keeps its own permission check. The {@code /simpletweaks} root has
 * no requirement. {@link #alias} is idempotent; every feature group calls it after its own registration.
 */
public final class TweakCommands {
    public static final String ROOT = "simpletweaks";

    /** Call after registering {@code shorts} (the top-level commands this group owns, if registered) and any /tweaks branches. */
    public static void alias(CommandDispatcher<CommandSourceStack> d, String... shorts) {
        var base = d.getRoot();
        var tweaks = base.getChild("tweaks");
        var found = new ArrayList<CommandNode<CommandSourceStack>>();
        for (var s : shorts) { var n = base.getChild(s); if (n != null) found.add(n); }
        if (found.isEmpty() && (tweaks == null || tweaks.getChildren().isEmpty()) && base.getChild(ROOT) == null) return;
        var root = base.getChild(ROOT);
        if (root == null) {
            root = d.register(Commands.literal(ROOT).then(Commands.literal("help").executes(c -> help(d, c.getSource()))));
        }
        for (var n : found) {
            if (root.getChild(n.getName()) != null) continue;
            root.addChild(new LiteralCommandNode<>(n.getName(), n.getCommand(), n.getRequirement(), n, null, false));
        }
        if (tweaks != null) for (var n : List.copyOf(tweaks.getChildren())) if (root.getChild(n.getName()) != n) root.addChild(n);
    }

    /** Preferred for NEW commands: registers a top-level command and its /simpletweaks twin in one call. */
    public static void add(CommandDispatcher<CommandSourceStack> d, com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> b) {
        d.register(b);
        alias(d, b.getLiteral());
    }

    /** Sub-commands of /simpletweaks the source may use, sorted. */
    public static List<String> allowed(CommandDispatcher<CommandSourceStack> d, CommandSourceStack source) {
        var root = d.getRoot().getChild(ROOT);
        var out = new TreeSet<String>();
        if (root != null) for (var n : root.getChildren()) if (n.canUse(source)) out.add(n.getName());
        return List.copyOf(out);
    }

    static int help(CommandDispatcher<CommandSourceStack> d, CommandSourceStack source) {
        var names = allowed(d, source);
        source.sendSuccess(() -> Component.translatable("command.simpletweaks.help.header"), false);
        for (var n : names) source.sendSuccess(() -> Component.literal("/" + ROOT + " " + n), false);
        return names.size();
    }
    private TweakCommands() {}
}
