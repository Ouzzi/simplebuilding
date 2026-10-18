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
        if (dispatcher.getRoot().getChild("claim")!=null) {
            // Claims are on by default: another mod's /claim must not stop the server. Protection stays active,
            // only the commands are left out (deeds still claim).
            org.slf4j.LoggerFactory.getLogger("simpletweaks-claims").warn("Another mod already registers /claim; Simple Tweaks claim commands are not registered. Protection stays active.");
            return;
        }
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
                        .executes(c->trust(c.getSource(),StringArgumentType.getString(c,"player"),ClaimRights.LEGACY))
                        .then(Commands.argument("rights",StringArgumentType.greedyString())
                                .executes(c->trust(c.getSource(),StringArgumentType.getString(c,"player"),rights(StringArgumentType.getString(c,"rights")))))))
                .then(Commands.literal("info").executes(c->info(c.getSource())))
                .then(Commands.literal("budget").executes(c->budget(c.getSource())))
                .then(Commands.literal("code").executes(c->code(c.getSource())))
                .then(Commands.literal("untrust").then(Commands.argument("player",StringArgumentType.word())
                        .executes(c->trust(c.getSource(),StringArgumentType.getString(c,"player"),0))))
                .then(Commands.literal("create").executes(c->result(c.getSource(),Claims.get(c.getSource().getServer()).claim(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("unclaim").executes(c->result(c.getSource(),Claims.get(c.getSource().getServer()).unclaim(c.getSource().getPlayerOrException(),false))))
                .then(Commands.literal("admin").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .executes(c->{c.getSource().sendSuccess(()->Component.translatable("command.simpletweaks.claim.stage","1–6"),false);return 1;})
                        .then(Commands.literal("unclaim").executes(c->result(c.getSource(),Claims.get(c.getSource().getServer()).unclaim(c.getSource().getPlayerOrException(),true))))
                        .then(Commands.literal("budget").then(Commands.argument("player",StringArgumentType.word())
                                .then(Commands.argument("chunks",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,ClaimConfig.MAX_CLAIMS))
                                        .executes(c->setBudget(c.getSource(),resolve(c.getSource(),StringArgumentType.getString(c,"player")),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"chunks"))))))
                        .then(Commands.literal("listall").executes(c->list(c.getSource(),null)))
                        .then(Commands.literal("list").then(Commands.argument("player",StringArgumentType.word())
                                .executes(c->list(c.getSource(),resolve(c.getSource(),StringArgumentType.getString(c,"player"))))))));
        com.simplebuilding.modules.simpletweaks.TweakCommands.alias(dispatcher,"claim");
    }
    private static int trust(CommandSourceStack source,String input,int rights) throws CommandSyntaxException {
        if (rights<0) throw new SimpleCommandExceptionType(Component.translatable("command.simpletweaks.claim.rights.invalid")).create();
        return result(source,Claims.get(source.getServer()).trust(source.getPlayerOrException(),resolve(source,input),rights));
    }
    /** Role (@member) or right letters (BDU); -1 if invalid. Managing cannot be granted per chunk. */
    static int rights(String text) {
        String t=text.strip();int bits=0;
        if (t.startsWith("@")) bits=ClaimRights.role(t.substring(1));
        else {
            if (t.isEmpty()) return -1;
            for (int i=0;i<t.length();i++){char ch=t.charAt(i);if(Character.isWhitespace(ch))continue;int b=ClaimRights.bit(ch);if(b<0)return -1;bits|=b;}
        }
        return bits<=0||(bits&ClaimRights.MANAGE)!=0?-1:bits;
    }
    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        var s=source;var p=s.getPlayerOrException();var claims=Claims.get(s.getServer());
        if (claims.locked()) return result(s,false);
        var key=new ClaimStore.Key(p.level().dimension().identifier().toString(),p.chunkPosition().pack());
        if (!claims.view().containsKey(key)) { s.sendSuccess(()->Component.translatable("command.simpletweaks.claim.empty"),false);return 1; }
        int rights=claims.rights(key.dimension(),p.getUUID(),key.chunk());
        s.sendSuccess(()->Component.translatable("command.simpletweaks.claim.info",rights==0?"-":ClaimRights.letters(rights)),false);
        return 1;
    }
    private static int budget(CommandSourceStack source) throws CommandSyntaxException {
        var p=source.getPlayerOrException();var claims=Claims.get(source.getServer());
        if (claims.locked()) return result(source,false);
        var tier=claims.nextTier(p.getUUID());
        Component next=tier==null?Component.translatable("command.simpletweaks.claim.budget.none"):Component.literal(tier.chunks()+" ("+tier.items().stream().map(i->i.count()+"x "+i.id()).collect(java.util.stream.Collectors.joining(", "))+(tier.levels()>0?", "+tier.levels()+" lvl":"")+")");
        source.sendSuccess(()->Component.translatable("command.simpletweaks.claim.budget",claims.chunksOf(p.getUUID()),claims.budget(p.getUUID()),next),false);
        return 1;
    }
    private static int setBudget(CommandSourceStack source,UUID owner,int chunks) {
        var claims=Claims.get(source.getServer());
        if (!claims.setBudget(owner,chunks)) return result(source,false);
        source.sendSuccess(()->Component.translatable("command.simpletweaks.claim.budget.set",chunks),true);
        return 1;
    }
    /** The code of the sender's own land (read-only). */
    private static int code(CommandSourceStack source) throws CommandSyntaxException {
        var p=source.getPlayerOrException();var claims=Claims.get(source.getServer());
        if (claims.locked()) return result(source,false);
        var land=claims.lands().get(p.getUUID());
        String code=land==null?"":land.code();
        source.sendSuccess(()->Component.translatable("command.simpletweaks.claim.code",code.isEmpty()?"-":code),false);
        return 1;
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
