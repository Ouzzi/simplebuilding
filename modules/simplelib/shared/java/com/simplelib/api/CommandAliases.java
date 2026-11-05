package com.simplelib.api;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * IG31-44: every command starts with the id of its mod ({@code /simpletweaks freeze}). The pre-rename root names stay as
 * aliases: a Brigadier redirect to the very same node (same requirement, same handler) plus a one-line chat hint that names
 * the new command. Aliases only take a free name - a foreign mod's {@code /claim} is never replaced.
 *
 * <p>{@link #nested} puts the old root under the mod root ({@code /claim ...} becomes {@code /simpletweaks claim ...});
 * {@link #contents} makes the old root the mod root itself ({@code /resonance debug} becomes {@code /simpleresonance debug}).
 * Several calls for one mod merge into one root, like any Brigadier registration.
 */
public final class CommandAliases {
    /** One moved command: the new path ({@code simpletweaks claim}) and the old root literal ({@code claim}). */
    public record Entry(String mod, String oldRoot, String newPath) {}

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private CommandAliases() {}

    /** Registers {@code old} below the root {@code /mod} and the old root as alias for it. */
    public static void nested(CommandDispatcher<CommandSourceStack> d, String mod, LiteralArgumentBuilder<CommandSourceStack> old) {
        nested(d, mod, source -> true, old);
    }

    /** As {@link #nested(CommandDispatcher, String, LiteralArgumentBuilder)}; {@code rootRequirement} applies if this call creates {@code /mod}. */
    public static void nested(CommandDispatcher<CommandSourceStack> d, String mod, Predicate<CommandSourceStack> rootRequirement,
            LiteralArgumentBuilder<CommandSourceStack> old) {
        LiteralCommandNode<CommandSourceStack> built = old.build();
        CommandNode<CommandSourceStack> root = modRoot(d, mod, rootRequirement);
        root.addChild(built);
        CommandNode<CommandSourceStack> target = root.getChild(built.getLiteral());
        alias(d, built, target, mod + " " + built.getLiteral(), mod);
    }

    /** Registers the tree of {@code old} as {@code /mod ...} itself (requirement and handler of the old root move along). */
    public static void contents(CommandDispatcher<CommandSourceStack> d, String mod, LiteralArgumentBuilder<CommandSourceStack> old) {
        LiteralCommandNode<CommandSourceStack> built = old.build();
        LiteralCommandNode<CommandSourceStack> copy = new LiteralCommandNode<>(mod, built.getCommand(), built.getRequirement(),
                built.getRedirect(), built.getRedirectModifier(), built.isFork());
        for (CommandNode<CommandSourceStack> child : new ArrayList<>(built.getChildren())) {
            // The old root's gate moves onto each branch: /mod may already exist without it (config command root).
            if (child instanceof LiteralCommandNode<CommandSourceStack> lit) {
                LiteralCommandNode<CommandSourceStack> gated = new LiteralCommandNode<>(lit.getLiteral(), lit.getCommand(),
                        built.getRequirement().and(lit.getRequirement()), lit.getRedirect(), lit.getRedirectModifier(), lit.isFork());
                for (CommandNode<CommandSourceStack> grand : new ArrayList<>(lit.getChildren())) gated.addChild(grand);
                child = gated;
            }
            copy.addChild(child);
        }
        d.getRoot().addChild(copy);
        alias(d, built, d.getRoot().getChild(mod), mod, mod);
    }

    /** Every move registered so far (for the aliases test of each module). */
    public static List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    /**
     * Problems of the aliases of {@code mod} in {@code d}: new path missing, alias missing or pointing elsewhere. Empty = all fine.
     * An old root taken by a foreign command is not a problem (the alias then does not exist by design).
     */
    public static List<String> verify(CommandDispatcher<CommandSourceStack> d, String mod) {
        List<String> problems = new ArrayList<>();
        int seen = 0;
        for (Entry e : ENTRIES) {
            if (!e.mod().equals(mod)) continue;
            seen++;
            CommandNode<CommandSourceStack> target = d.getRoot();
            for (String part : e.newPath().split(" ")) {
                target = target == null ? null : target.getChild(part);
            }
            if (target == null) {
                problems.add("/" + e.newPath() + " is not registered");
                continue;
            }
            CommandNode<CommandSourceStack> old = d.getRoot().getChild(e.oldRoot());
            if (old == null || old.getRedirect() == null) continue; // foreign command owns the old name
            if (old.getRedirect() != target) problems.add("/" + e.oldRoot() + " does not lead to /" + e.newPath());
        }
        if (seen == 0) problems.add("no moved command registered for " + mod);
        return problems;
    }

    private static CommandNode<CommandSourceStack> modRoot(CommandDispatcher<CommandSourceStack> d, String mod,
            Predicate<CommandSourceStack> requirement) {
        CommandNode<CommandSourceStack> root = d.getRoot().getChild(mod);
        if (root == null) {
            d.getRoot().addChild(Commands.literal(mod).requires(requirement).build());
            root = d.getRoot().getChild(mod);
        }
        return root;
    }

    private static void alias(CommandDispatcher<CommandSourceStack> d, LiteralCommandNode<CommandSourceStack> old,
            CommandNode<CommandSourceStack> target, String newPath, String mod) {
        String name = old.getLiteral();
        if (ENTRIES.stream().noneMatch(e -> e.mod().equals(mod) && e.oldRoot().equals(name) && e.newPath().equals(newPath))) {
            ENTRIES.add(new Entry(mod, name, newPath));
        }
        CommandNode<CommandSourceStack> taken = d.getRoot().getChild(name);
        if (taken != null && taken.getRedirect() == null && taken != target) return; // foreign command: leave it alone
        LiteralArgumentBuilder<CommandSourceStack> alias = Commands.literal(name).requires(old.getRequirement())
                .redirect(target, ctx -> {
                    hint(ctx.getSource(), name, newPath);
                    return ctx.getSource();
                });
        if (target.getCommand() != null) {
            alias.executes(ctx -> {
                hint(ctx.getSource(), name, newPath);
                return target.getCommand().run(ctx);
            });
        }
        d.register(alias);
    }

    private static void hint(CommandSourceStack source, String old, String path) {
        source.sendSuccess(() -> Component.translatable("commands.simplelib.moved", "/" + old, "/" + path)
                .withStyle(ChatFormatting.YELLOW), false);
    }
}
