package com.simplebuilding.modules.simplechallenges.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.config.ChallengeServerConfig;
import com.simplebuilding.modules.simplechallenges.core.Active;
import com.simplebuilding.modules.simplechallenges.core.ChallengeRuntime;
import com.simplebuilding.modules.simplechallenges.core.ChallengeService;
import com.simplebuilding.modules.simplechallenges.core.PresetRegistry;
import com.simplebuilding.modules.simplechallenges.core.UnlockService;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import com.simplebuilding.modules.simplechallenges.price.Price;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /simplechallenges}: status and unlocking for everyone, the switch-over commands for game masters and only if the server
 * config allows them ({@code allowAdminCommands}, off by default - safety lock, concept R2: test worlds). The config command
 * {@code /simplechallenges config} comes from SimpleLib.
 */
public final class ChallengeCommands {
    private ChallengeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(SimpleChallenges.MOD_ID)
                .executes(c -> status(c.getSource()))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("presets").executes(c -> presets(c.getSource())))
                .then(Commands.literal("unlock").executes(c -> unlockLooked(c.getSource()))
                        .then(Commands.argument("x", IntegerArgumentType.integer(-30_000_000, 30_000_000))
                                .then(Commands.argument("z", IntegerArgumentType.integer(-30_000_000, 30_000_000))
                                        .executes(c -> unlockAt(c.getSource(), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"), false))
                                        .then(Commands.literal("free").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                                .executes(c -> unlockAt(c.getSource(), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"), true))))))
                .then(Commands.literal("activate").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("preset", StringArgumentType.string())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(PresetRegistry.ids(), b))
                                .executes(c -> activate(c.getSource(), StringArgumentType.getString(c, "preset")))))
                .then(Commands.literal("deactivate").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> deactivate(c.getSource())))
                .then(Commands.literal("credits").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer(-100_000, 100_000))
                                .executes(c -> credits(c.getSource(), IntegerArgumentType.getInteger(c, "amount")))))));
    }

    private static boolean adminAllowed(CommandSourceStack s) {
        if (ChallengeServerConfig.allowAdminCommands) return true;
        s.sendFailure(Component.translatable("simplechallenges.cmd.admin_off"));
        return false;
    }

    static Component priceText(Price p) {
        return switch (p.currency()) {
            case ITEMS -> Component.literal(p.count() + " x ").append(new net.minecraft.world.item.ItemStack(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(p.item()))).getHoverName());
            case CREDITS -> Component.translatable("simplechallenges.price.credits", p.count());
            case XP -> Component.translatable("simplechallenges.price.xp", p.count());
        };
    }

    private static int status(CommandSourceStack s) {
        Active a = ChallengeRuntime.server();
        if (a == null || !SimpleChallenges.on()) {
            s.sendSuccess(() -> Component.translatable("simplechallenges.status.none"), false);
            return 1;
        }
        UnlockGrid any = a.grid(a.mode.dimensions().get(0));
        s.sendSuccess(() -> Component.translatable("simplechallenges.status.active", a.preset.name(), any == null ? 0 : any.size(), a.mode.cell(),
                a.state.credits), false);
        if (s.getEntity() instanceof ServerPlayer p) {
            String dim = p.level().dimension().identifier().toString();
            UnlockGrid g = a.grid(dim);
            if (g != null) {
                Vec3 look = p.getLookAngle();
                long k = UnlockService.lookedCell(g, p.getX(), p.getZ(), look.x, look.z, 48);
                if (k != UnlockGrid.NONE) {
                    Price price = UnlockService.priceOf(a, dim, UnlockGrid.keyX(k), UnlockGrid.keyZ(k));
                    boolean can = UnlockService.canPay(a, p, price);
                    s.sendSuccess(() -> Component.translatable(can ? "simplechallenges.status.ahead_yes" : "simplechallenges.status.ahead_no",
                            UnlockGrid.keyX(k) * g.cell(), UnlockGrid.keyZ(k) * g.cell(), priceText(price)), false);
                }
            }
        }
        return 1;
    }

    private static int presets(CommandSourceStack s) {
        for (Preset p : PresetRegistry.all()) {
            s.sendSuccess(() -> Component.literal(p.id() + " - " + p.name() + (p.runnable() ? "" : " (not available yet)")), false);
        }
        for (String problem : PresetRegistry.problems()) s.sendFailure(Component.literal(problem));
        return PresetRegistry.ids().size();
    }

    private static int unlockLooked(CommandSourceStack s) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        Active a = ChallengeRuntime.server();
        UnlockGrid g = a == null ? null : a.grid(p.level().dimension().identifier().toString());
        if (g == null) {
            s.sendFailure(Component.translatable("simplechallenges.status.none"));
            return 0;
        }
        Vec3 look = p.getLookAngle();
        long k = UnlockService.lookedCell(g, p.getX(), p.getZ(), look.x, look.z, 48);
        if (k == UnlockGrid.NONE) {
            s.sendFailure(Component.translatable("simplechallenges.unlock.nothing_ahead"));
            return 0;
        }
        return unlockCell(s, p, a, UnlockGrid.keyX(k), UnlockGrid.keyZ(k), false);
    }

    private static int unlockAt(CommandSourceStack s, int x, int z, boolean free) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Active a = ChallengeRuntime.server();
        ServerPlayer p = s.getPlayerOrException();
        UnlockGrid g = a == null ? null : a.grid(p.level().dimension().identifier().toString());
        if (g == null) {
            s.sendFailure(Component.translatable("simplechallenges.status.none"));
            return 0;
        }
        if (free && !adminAllowed(s)) return 0;
        return unlockCell(s, p, a, g.cellOf(x), g.cellOf(z), free);
    }

    private static int unlockCell(CommandSourceStack s, ServerPlayer p, Active a, int cx, int cz, boolean free) {
        String dim = p.level().dimension().identifier().toString();
        if (free) {
            boolean done = UnlockService.unlock(a, dim, cx, cz);
            s.sendSuccess(() -> Component.translatable(done ? "simplechallenges.unlock.free" : "simplechallenges.unlock.already"), true);
            return done ? 1 : 0;
        }
        UnlockService.Outcome o = UnlockService.tryUnlock(a, p, dim, cx, cz);
        switch (o) {
            case OK -> s.sendSuccess(() -> Component.translatable("simplechallenges.unlock.ok"), false);
            case ALREADY -> s.sendFailure(Component.translatable("simplechallenges.unlock.already"));
            case NOT_ADJACENT -> s.sendFailure(Component.translatable("simplechallenges.unlock.not_adjacent"));
            case CANNOT_PAY -> s.sendFailure(Component.translatable("simplechallenges.unlock.cannot_pay", priceText(UnlockService.priceOf(a, dim, cx, cz))));
            default -> s.sendFailure(Component.translatable("simplechallenges.status.none"));
        }
        return o == UnlockService.Outcome.OK ? 1 : 0;
    }

    private static int activate(CommandSourceStack s, String id) {
        if (!adminAllowed(s)) return 0;
        ChallengeService.Result r = ChallengeService.activate(s.getServer(), id);
        switch (r) {
            case OK -> s.sendSuccess(() -> Component.translatable("simplechallenges.cmd.activated", id), true);
            case UNKNOWN_PRESET -> s.sendFailure(Component.translatable("simplechallenges.cmd.unknown_preset", id));
            default -> s.sendFailure(Component.translatable("simplechallenges.cmd.not_runnable", id));
        }
        return r == ChallengeService.Result.OK ? 1 : 0;
    }

    private static int deactivate(CommandSourceStack s) {
        if (!adminAllowed(s)) return 0;
        ChallengeService.deactivate(s.getServer());
        s.sendSuccess(() -> Component.translatable("simplechallenges.cmd.deactivated"), true);
        return 1;
    }

    private static int credits(CommandSourceStack s, int amount) {
        if (!adminAllowed(s)) return 0;
        Active a = ChallengeRuntime.server();
        if (a == null) {
            s.sendFailure(Component.translatable("simplechallenges.status.none"));
            return 0;
        }
        a.state.credits = Math.max(0, a.state.credits + amount);
        a.state.setDirty();
        s.sendSuccess(() -> Component.translatable("simplechallenges.cmd.credits", a.state.credits), true);
        return 1;
    }
}
