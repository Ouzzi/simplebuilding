package com.simplebuilding.modules.simpletweaks.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;

public final class ClaimCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        register(dispatcher,Claims.settings());
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ClaimConfig config) {
        if (!config.enabled()) return;
        if (dispatcher.getRoot().getChild("claim")!=null)
            throw new IllegalStateException("Enabled Simple Tweaks claims conflict with an existing /claim command; disable one claim provider.");
        dispatcher.register(Commands.literal("claim").requires(s->Claims.enabled(s.getServer()))
                .executes(c->{
                    var s=c.getSource(); var p=s.getPlayerOrException(); var claims=Claims.get(s.getServer());
                    if (claims.locked()) { s.sendFailure(Component.translatable("command.simpletweaks.claim.locked")); return 0; }
                    var key=new ClaimStore.Key(p.level().dimension().identifier().toString(),p.chunkPosition().pack());
                    var data=claims.view().get(key);
                    s.sendSuccess(()->Component.translatable(data==null?"command.simpletweaks.claim.empty":"command.simpletweaks.claim.owner",data==null?"":data.owner()),false);
                    return 1;
                })
                .then(Commands.literal("trust").then(Commands.argument("player",StringArgumentType.word())
                        .executes(c->trust(c.getSource(),StringArgumentType.getString(c,"player"),true))))
                .then(Commands.literal("untrust").then(Commands.argument("player",StringArgumentType.word())
                        .executes(c->trust(c.getSource(),StringArgumentType.getString(c,"player"),false))))
                .then(Commands.literal("unclaim").executes(c->result(c.getSource(),Claims.get(c.getSource().getServer()).unclaim(c.getSource().getPlayerOrException(),false))))
                .then(Commands.literal("admin").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .executes(c->{c.getSource().sendSuccess(()->Component.translatable("command.simpletweaks.claim.stage","1–3, 5–6"),false);return 1;})
                        .then(Commands.literal("unclaim").executes(c->result(c.getSource(),Claims.get(c.getSource().getServer()).unclaim(c.getSource().getPlayerOrException(),true))))
                        .then(Commands.literal("listall").executes(c->list(c.getSource(),null)))
                        .then(Commands.literal("list").then(Commands.argument("player",StringArgumentType.word())
                                .executes(c->list(c.getSource(),resolve(c.getSource(),StringArgumentType.getString(c,"player"))))))));
    }
    private static int trust(CommandSourceStack source,String input,boolean grant) throws CommandSyntaxException {
        return result(source,Claims.get(source.getServer()).trust(source.getPlayerOrException(),resolve(source,input),grant));
    }
    /** Offline identities require a full UUID: no blocking lookup or guessed offline-mode identity. */
    private static UUID resolve(CommandSourceStack source,String input) throws CommandSyntaxException {
        if (input.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            var id=UUID.fromString(input);if (!id.equals(new UUID(0,0))) return id;
        } else if (input.matches("[A-Za-z0-9_]{1,16}")) {
            var player=source.getServer().getPlayerList().getPlayerByName(input);
            if (player!=null) return player.getUUID();
        }
        throw new SimpleCommandExceptionType(Component.translatable("command.simpletweaks.claim.identity")).create();
    }
    private static int result(CommandSourceStack source,boolean success) {
        if (success) source.sendSuccess(()->Component.translatable("command.simpletweaks.claim.updated"),false);
        else source.sendFailure(Component.translatable(Claims.get(source.getServer()).locked()?"command.simpletweaks.claim.locked":"command.simpletweaks.claim.denied"));
        return success?1:0;
    }
    private static int list(CommandSourceStack source,UUID owner) {
        var claims=Claims.get(source.getServer());if (claims.locked()) return result(source,false);
        var entries=claims.view().entrySet().stream().filter(e->owner==null||owner.equals(e.getValue().owner()))
                .sorted(Comparator.comparing((Map.Entry<ClaimStore.Key,ClaimStore.Claim> e)->e.getKey().dimension()).thenComparingLong(e->e.getKey().chunk()))
                .limit(owner==null?50:10).toList();
        source.sendSuccess(()->Component.translatable("command.simpletweaks.claim.list",entries.size(),owner==null?50:10),false);
        for (var e:entries) source.sendSuccess(()->Component.literal(e.getKey().dimension()+" ["+net.minecraft.world.level.ChunkPos.getX(e.getKey().chunk())+", "+net.minecraft.world.level.ChunkPos.getZ(e.getKey().chunk())+"] "+e.getValue().owner()),false);
        return entries.size();
    }
    private ClaimCommands() {}
}
